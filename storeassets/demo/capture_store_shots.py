#!/usr/bin/env python3
"""Take all raw store screenshots automatically - no manual steps, no checking in between.

    python3 storeassets/demo/capture_store_shots.py [--web-out DIR] [--restore]

What it does, in order (see HOW-TO-RECREATE.md for the background):
  1. Developer settings: turns on Demo-Modus and points the app at a dead server URL, so the map
     tiles still load (internet stays on) but no sync can overwrite the demo data.
  2. Sets the emulator's own GPS position to Dornbirn (the compass and the map's "me" dot).
  3. Runs apply_demo_data.sh (fictional friends, groups, poll, events) and pins vanessa's chat.
  4. Captures the 7 screens into storeassets/shots/ with a clean status bar (09:41, full battery).
  5. Optional: --web-out writes 600 px WebP copies for the website (server repo
     src/main/resources/static/web_images/screens), --restore switches back to the test server.

Needs: a running emulator (developed on the Pixel 10 Pro AVD, 1280x2856) with the debug build
installed and the test account "luca" logged in; adb, sqlite3, python3 and ImageMagick 7 (magick),
plus cwebp for --web-out. The map taps are screen coordinates for that AVD: on another device size
the map view (03) and the compass (06) need new values in MAP_TAPS.
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "org.lerchenflo.schneaggchatv3mp.debug"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SHOTS = os.path.join(ROOT, "storeassets", "shots")
DEAD_URL = "http://10.0.2.2:1"
OWN_POSITION = ("9.7435", "47.4118", "430")  # lon, lat, altitude: luca stands in Dornbirn
# Map (Pixel 10 Pro AVD): tap the friends cluster in the default view, tap the cluster again, then
# double-tap to zoom one more level - that is the view with two clusters and single faces around.
MAP_TAPS = [("tap", 540, 1265), ("tap", 630, 1335), ("double", 640, 1290)]
POLL_TITLE = "Wann starten wir"
POLL_Y = {"02-chat": 475, "05-umfrage": 980}  # where the poll title sits in each capture

SDK_ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
ADB = shutil.which("adb") or SDK_ADB


def adb(*args, check=False):
    return subprocess.run([ADB, *args], capture_output=True, text=True, check=check).stdout


def sh(cmd):
    adb("shell", cmd)


def pause(seconds):
    time.sleep(seconds)


# ─── UI tree ─────────────────────────────────────────────────────────────────

def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = subprocess.run([ADB, "exec-out", "cat", "/sdcard/ui.xml"], capture_output=True).stdout
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return []
    out = []
    for n in root.iter("node"):
        nums = list(map(int, re.findall(r"\d+", n.get("bounds", ""))))
        if len(nums) != 4:
            continue
        out.append({
            "text": n.get("text") or "",
            "desc": n.get("content-desc") or "",
            "checkable": n.get("checkable") == "true",
            "checked": n.get("checked") == "true",
            "bounds": nums,
            "cx": (nums[0] + nums[2]) // 2,
            "cy": (nums[1] + nums[3]) // 2,
        })
    return out


def find(text, exact=False, below=None, above=None, tree=None):
    hits = []
    for n in tree if tree is not None else nodes():
        label = n["text"] or n["desc"]
        ok = label == text if exact else text.lower() in label.lower()
        if ok and (below is None or n["cy"] > below) and (above is None or n["cy"] < above):
            hits.append(n)
    return hits


def wait_for(text, timeout=15, **kw):
    end = time.time() + timeout
    while time.time() < end:
        hits = find(text, **kw)
        if hits:
            return hits[0]
        pause(0.5)
    sys.exit(f"timed out waiting for '{text}' on screen")


def tap(x, y, wait=1.0):
    sh(f"input tap {x} {y}")
    pause(wait)


def tap_text(text, wait=1.5, **kw):
    node = wait_for(text, **kw)
    tap(node["cx"], node["cy"], wait)


def double_tap(x, y, wait=3.0):
    sh(f"input tap {x} {y} & sleep 0.08; input tap {x} {y}")
    pause(wait)


def swipe(x0, y0, x1, y1, ms):
    sh(f"input swipe {x0} {y0} {x1} {y1} {ms}")


def back(wait=1.2):
    sh("input keyevent 4")
    pause(wait)


def screen_height():
    size = re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    return int(size[-1][1])


def nav(tab):
    """Tap a bottom navigation tab (the tab row is the bottom ~8% of the screen)."""
    tap_text(tab, exact=True, below=int(screen_height() * 0.9), wait=2.5)


# ─── Steps ───────────────────────────────────────────────────────────────────

def launch():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
    wait_for("Chats", exact=True, timeout=30)
    dismiss_popups()


def dismiss_popups():
    """The changelog ("Neuigkeiten") shows after every app update and after a fresh start."""
    for _ in range(3):
        if not find("Neuigkeiten"):
            return
        back()


def developer_settings():
    nav("Einstellungen")
    tap_text("Entwickler", exact=True, wait=2)

    tree = nodes()
    row = find("Demo-Modus", exact=True, tree=tree)[0]
    switch = min((n for n in tree if n["checkable"]), key=lambda n: abs(n["cy"] - row["cy"]))
    if not switch["checked"]:
        tap(switch["cx"], switch["cy"])

    tap_text("Server URL ändern", wait=1.5)
    field = wait_for("http")
    if field["text"] != DEAD_URL:
        tap(field["cx"], field["cy"], 0.6)
        sh("input keycombination 113 29")  # Ctrl+A
        sh("input keyevent 67")            # Del
        sh(f"input text '{DEAD_URL}'")
        pause(0.6)
    tap_text("Speichern", exact=True)
    back()


def restore_server():
    nav("Einstellungen")
    tap_text("Entwickler", exact=True, wait=2)
    tap_text("Server URL ändern", wait=1.5)
    tap_text("Testserver verwenden")
    tap_text("Speichern", exact=True)
    back()


def seed_demo_data():
    env = dict(os.environ, ONLINE_OK="1", PATH=os.path.dirname(ADB) + os.pathsep + os.environ["PATH"])
    subprocess.run(["bash", os.path.join(HERE, "apply_demo_data.sh")], env=env, check=True,
                   stdout=subprocess.DEVNULL)
    pause(4)
    wait_for("Chats", exact=True, timeout=30)
    dismiss_popups()


def pin_vanessa():
    for _ in range(6):
        hits = find("vanessa", exact=True)
        if hits:
            break
        swipe(640, 2100, 640, 900, 500)
        pause(1)
    else:
        sys.exit("vanessa's chat not found")
    row = hits[0]
    swipe(row["cx"] + 300, row["cy"], row["cx"] + 300, row["cy"], 900)  # long press
    pause(1.2)
    if find("Chat anpinnen"):
        tap_text("Chat anpinnen")
    else:
        back()  # already pinned


def scroll_to_top():
    """Back up to the pinned vanessa row. Stops there instead of pulling past the top edge, which
    would start a pull-to-refresh whose spinner ends up in the screenshot."""
    for _ in range(10):
        hits = find("vanessa", exact=True)
        if hits and hits[0]["cy"] < 800:
            return
        drag(-900)
    sys.exit("could not scroll the chat list back to the top")


def scroll_so(text, target_y):
    """Scroll slowly (no fling) until the node with `text` is centred at target_y."""
    for _ in range(8):
        hits = find(text)
        if not hits:  # still above the visible part: reveal older content
            swipe(640, 1200, 640, 1700, 1800)
            pause(1)
            continue
        node = hits[0]
        delta = node["cy"] - target_y
        if abs(delta) <= 12:
            return
        if abs(delta) < 150:
            # A short slow swipe would count as a long press (message menu). Overshoot, come back.
            sign = 1 if delta > 0 else -1
            drag(delta + 200 * sign)
            drag(-200 * sign)
        else:
            drag(delta)


def drag(distance):
    """Move the content up by `distance` px (negative = down), slow enough not to fling."""
    start = 1500
    ms = min(max(abs(distance) * 5, 500), 1500)
    swipe(640, start, 640, start - distance, ms)
    pause(1)


def demo_status_bar(on):
    def demo(*args):
        adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", *args)
    if on:
        adb("shell", "settings", "put", "global", "sysui_demo_allowed", "1")
        demo("enter")
        demo("clock", "-e", "hhmm", "0941")
        demo("battery", "-e", "level", "100", "-e", "plugged", "false")
        demo("network", "-e", "wifi", "show", "-e", "level", "4")
        demo("network", "-e", "mobile", "show", "-e", "datatype", "none", "-e", "level", "4")
        demo("notifications", "-e", "visible", "false")
        pause(1)
    else:
        demo("exit")


def capture(name):
    pause(1)
    png = subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True).stdout
    path = os.path.join(SHOTS, f"{name}.png")
    with open(path, "wb") as f:
        f.write(png)
    print("captured", path)


def take_shots():
    demo_status_bar(True)

    nav("Chats")
    scroll_to_top()
    capture("01-chats")

    tap_text("Skitour Bödele", wait=2.5)
    scroll_so(POLL_TITLE, POLL_Y["02-chat"])
    capture("02-chat")
    scroll_so(POLL_TITLE, POLL_Y["05-umfrage"])
    capture("05-umfrage")
    back()

    nav("Events")
    capture("04-events")

    # The app was just restarted by the seeding, so the map opens in its default view
    nav("Karte")
    pause(2)
    for kind, x, y in MAP_TAPS:
        if kind == "tap":
            tap(x, y, 3)
        else:
            double_tap(x, y, 3.5)
    capture("03-karte")

    # Compass towards Mia: her chip centres the map on her, her marker opens the sheet
    tap_text("Mia", exact=True, below=int(screen_height() * 0.75), wait=2.5)
    tap(633, 1288, 2)
    tap_text("Kompass", exact=True, wait=3)
    capture("06-kompass")
    back()  # once only: a second back on the map tab would leave the app

    nav("Spiele")
    tap_text("Ein tägliches C-Programmierrätsel", wait=2.5)
    if find("Tippe irgendwo"):
        tap(640, int(screen_height() * 0.85), 1.5)
    capture("07-spiel")
    back()

    demo_status_bar(False)


def export_web(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for f in sorted(os.listdir(SHOTS)):
        if not f.endswith(".png"):
            continue
        tmp = os.path.join(out_dir, f".{f}")
        subprocess.run(["magick", os.path.join(SHOTS, f), "-resize", "600x", "-strip", tmp], check=True)
        subprocess.run(["cwebp", "-quiet", "-q", "82", "-m", "6", tmp, "-o",
                        os.path.join(out_dir, f.replace(".png", ".webp"))], check=True)
        os.remove(tmp)
    print("website copies in", out_dir)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--web-out", help="also write 600 px WebP copies for the website here")
    parser.add_argument("--restore", action="store_true", help="switch the app back to the test server at the end")
    args = parser.parse_args()

    if "device" not in adb("get-state"):
        sys.exit("no emulator/device attached")
    os.makedirs(SHOTS, exist_ok=True)

    launch()
    developer_settings()
    adb("emu", "geo", "fix", *OWN_POSITION)
    seed_demo_data()
    nav("Chats")
    pin_vanessa()
    take_shots()

    if args.web_out:
        export_web(args.web_out)
    if args.restore:
        restore_server()
        print("server switched back - the next sync replaces the demo data")
    else:
        print(f"app still points at {DEAD_URL}; run with --restore (or switch it in the developer settings) when done")


if __name__ == "__main__":
    main()
