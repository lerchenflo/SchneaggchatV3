#!/usr/bin/env bash
# Fill the debug app on a connected emulator with fictional German demo data for store screenshots
# (friends with locations around Vorarlberg, three groups with a poll, direct chats, events).
# Requires adb, sqlite3, python3 and ImageMagick 7.
#
# Recreate the store set:
#   1. Install the debug build on the emulator, device language German.
#   2. Log in with the test-server account "luca" and let it sync once.
#   3. Developer settings -> turn on "Demo-Modus" (hides the offline and test-server bars).
#   4. Turn on airplane mode and keep it on - any sync overwrites the demo data.
#   5. Run this script, then take the screenshots with the store-image-generator skill.
#
# Content lives in seed_demo_data.py (written for Room schema v87), faces in avatars/ (AI-generated,
# not real people). The untouched DB is backed up to .last-backup-database.db next to this script.
set -euo pipefail

PKG="${PKG:-org.lerchenflo.schneaggchatv3mp.debug}"
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT

# Online is only safe while the app points at an unreachable server (needed for map tiles):
# set ONLINE_OK=1 after changing the server URL in the developer settings to e.g. http://10.0.2.2:1
if [[ "${ONLINE_OK:-0}" != "1" && "$(adb shell settings get global airplane_mode_on | tr -d '\r')" != "1" ]]; then
  echo "Turn on airplane mode first - a sync with the server would overwrite the demo data." >&2
  echo "(Or point the app at a dead server URL and run with ONLINE_OK=1.)" >&2
  exit 1
fi

echo "== stopping app and pulling the database"
adb shell am force-stop "$PKG"
for f in database.db database.db-wal database.db-shm; do
  adb exec-out run-as "$PKG" cat "databases/$f" > "$WORK/$f" 2>/dev/null || rm -f "$WORK/$f"
done
cp "$WORK/database.db" "$HERE/.last-backup-database.db"   # untouched copy, in case you want it back
[[ -f "$WORK/database.db-wal" ]] && cp "$WORK/database.db-wal" "$HERE/.last-backup-database.db-wal"
sqlite3 "$WORK/database.db" "PRAGMA wal_checkpoint(TRUNCATE);" >/dev/null
rm -f "$WORK/database.db-wal" "$WORK/database.db-shm"

echo "== seeding"
python3 "$HERE/seed_demo_data.py" "$WORK/database.db"

echo "== profile pictures"
# avatars/<name>.jpeg -> <userId>.upb (PNG), user ids come from seed_demo_data.py
declare -A IDS=(
  [jonas]=de0000000000000000000001 [sophie]=de0000000000000000000002 [max]=de0000000000000000000003
  [mia]=de0000000000000000000004 [paul]=de0000000000000000000005 [lena]=de0000000000000000000006
  [tobias]=de0000000000000000000007 [alex]=de0000000000000000000008
)
# Round with a transparent outside, so map markers and lists never show square corners
circle() { magick "$1" -resize 512x512^ -gravity center -extent 512x512 \
  \( -size 512x512 xc:black -fill white -draw "circle 256,256 256,0" \) -alpha off -compose CopyOpacity -composite "PNG32:$2"; }
for name in "${!IDS[@]}"; do
  circle "$HERE/avatars/$name.jpeg" "$WORK/${IDS[$name]}.upb"
done
# Group pictures: 2x2 collage of member faces
collage() { magick "$HERE/avatars/$2.jpeg" "$HERE/avatars/$3.jpeg" "$HERE/avatars/$4.jpeg" "$HERE/avatars/$5.jpeg" \
  -resize 256x256^ -gravity center -extent 256x256 miff:- | magick montage - -tile 2x2 -geometry +0+0 "PNG:$WORK/$1.sq.png"
  circle "$WORK/$1.sq.png" "$WORK/$1.gpb"; rm "$WORK/$1.sq.png"; }
collage de00000000000000000000a1 jonas sophie max lena
collage de00000000000000000000a2 mia paul tobias alex
collage de00000000000000000000a3 max paul jonas tobias

echo "== pushing back"
for f in "$WORK"/*.upb "$WORK"/*.gpb; do
  adb push "$f" "/data/local/tmp/$(basename "$f")" >/dev/null
  adb shell run-as "$PKG" cp "/data/local/tmp/$(basename "$f")" "files/$(basename "$f")"
  adb shell rm "/data/local/tmp/$(basename "$f")"
done
adb push "$WORK/database.db" /data/local/tmp/database.db >/dev/null
adb shell run-as "$PKG" sh -c "'rm -f databases/database.db-wal databases/database.db-shm && cp /data/local/tmp/database.db databases/database.db'"
adb shell rm /data/local/tmp/database.db

echo "== starting app (German app locale - an unset device locale falls back to English)"
adb shell cmd locale set-app-locales "$PKG" --locales de-DE
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
echo "done - keep airplane mode on while taking screenshots"
