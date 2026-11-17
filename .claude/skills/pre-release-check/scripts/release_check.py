#!/usr/bin/env python3
"""Mechanical checks before a Schneaggchat release. Stdlib only, read-only.

Run from the app repo root:
    python3 .claude/skills/pre-release-check/scripts/release_check.py [translations|versions|todos|git|all]
        [--server ../SchneaggchatV3server]

Every finding line starts with BLOCKER, WARN or OK. The to-do section only lists candidates;
judging them is the skill's job.
"""
import argparse
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

RES = "composeApp/src/commonMain/composeResources"
LOCALES = {"de": "values-de", "it": "values-it", "de-AT (Vori)": "values-de-rAT"}
SWIFT_STRINGS = "iosApp/SchneaggchatNotificationServiceExtension/NotificationStrings.swift"
PBXPROJ = "iosApp/iosApp.xcodeproj/project.pbxproj"
WEBSITE_I18N = "src/main/resources/static/i18n"
# Feature quiz texts stay German on purpose in values-de-rAT (schneaggchat-vori skill)
GERMAN_ONLY_IN_VORI = re.compile(r"^featurequiz_.+_(question|answer_\d+|none|explanation)$")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf@]|%(?:\d+\$)?l[du]")
SHOWN = 15  # findings listed per group before "... and N more"


def say(level, text):
    print(f"{level:7} {text}")


def listed(keys):
    keys = sorted(keys)
    more = f" ... and {len(keys) - SHOWN} more" if len(keys) > SHOWN else ""
    return ", ".join(keys[:SHOWN]) + more


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def run(cmd, cwd):
    result = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    return result.stdout.rstrip() if result.returncode == 0 else ""


def latest_tag():
    tags = run(["git", "tag", "--list", "v*", "--sort=-v:refname"], ".").splitlines()
    return tags[0] if tags else None


# ─── Translations ────────────────────────────────────────────────────────────

def load_resources(path, text=None):
    """Returns ({string key: text}, {plural key: {quantity: text}}), from [text] when given."""
    root = ET.fromstring(text) if text is not None else ET.parse(path).getroot()
    strings, plurals = {}, {}
    for el in root:
        name = el.get("name")
        if el.tag == "string":
            strings[name] = "".join(el.itertext())
        elif el.tag == "plurals":
            plurals[name] = {item.get("quantity"): "".join(item.itertext()) for item in el}
    return strings, plurals


def placeholders(text):
    return sorted(p.replace("l", "") for p in PLACEHOLDER.findall(text))


def check_locale(label, en, other, skip=lambda key: False):
    en_strings, en_plurals = en
    strings, plurals = other
    missing = [k for k in en_strings if k not in strings and not skip(k)]
    missing_plurals = [k for k in en_plurals if k not in plurals]
    orphans = [k for k in list(strings) + list(plurals) if k not in en_strings and k not in en_plurals]
    broken = [k for k, text in strings.items() if k in en_strings and placeholders(text) != placeholders(en_strings[k])]
    if missing:
        say("BLOCKER", f"{label}: {len(missing)} strings missing: {listed(missing)}")
    if missing_plurals:
        say("BLOCKER", f"{label}: {len(missing_plurals)} plurals missing: {listed(missing_plurals)}")
    if broken:
        say("BLOCKER", f"{label}: placeholders differ from EN (crash or wrong text): {listed(broken)}")
    if orphans:
        say("WARN", f"{label}: {len(orphans)} keys not in EN any more (delete them): {listed(orphans)}")
    if not (missing or missing_plurals or broken or orphans):
        say("OK", f"{label}: complete ({len(strings)} strings, {len(plurals)} plurals)")


def check_swift_tables():
    src = read(SWIFT_STRINGS)
    enum = re.search(r"enum NotificationStringKey: String \{(.*?)\n\}", src, re.S)
    keys = set(re.findall(r"^\s*case (\w+)", enum.group(1), re.M)) if enum else set()
    tables = {
        m.group(1): dict(re.findall(r'^\s+\.(\w+):\s*"(.*)",\s*$', m.group(2), re.M))
        for m in re.finditer(r"private static let (\w+): \[NotificationStringKey: String\] = \[(.*?)\n    \]", src, re.S)
    }
    english = tables.get("englishStrings", {})
    for name, table in tables.items():
        missing = keys - set(table)
        broken = [k for k, text in table.items() if k in english and placeholders(text) != placeholders(english[k])]
        if missing:
            say("BLOCKER", f"iOS {name}: {len(missing)} keys missing: {listed(missing)}")
        if broken:
            say("BLOCKER", f"iOS {name}: placeholders differ from englishStrings: {listed(broken)}")
        if not (missing or broken):
            say("OK", f"iOS {name}: complete ({len(table)} keys)")


def check_website(server):
    folder = os.path.join(server, WEBSITE_I18N)
    if not os.path.isdir(folder):
        say("WARN", f"website texts not found at {folder}")
        return
    en = load_resources(os.path.join(folder, "strings-en.xml"))
    for label, file in (("website de", "strings-de.xml"), ("website de-AT (Vori)", "strings-de-at.xml")):
        check_locale(label, en, load_resources(os.path.join(folder, file)))


def check_stale(en_now, tag):
    """Keys whose EN text changed since [tag] while a translation kept its old text."""
    path = os.path.join(RES, "values/strings.xml")
    old_text = run(["git", "show", f"{tag}:{path}"], ".")
    if not old_text:
        say("WARN", f"stale check skipped: {path} not readable at {tag}")
        return
    en_old = load_resources(path, old_text)[0]
    changed = [k for k, text in en_now[0].items() if k in en_old and en_old[k] != text]
    for label, folder in LOCALES.items():
        locale_path = os.path.join(RES, folder, "strings.xml")
        old = run(["git", "show", f"{tag}:{locale_path}"], ".")
        if not old:
            continue
        before = load_resources(locale_path, old)[0]
        now = load_resources(locale_path)[0]
        stale = [k for k in changed if k in now and now[k] == before.get(k)]
        if stale:
            say("WARN", f"app {label}: EN text changed since {tag}, translation still the old one: {listed(stale)}")
    if changed:
        say("INFO", f"{len(changed)} EN texts changed since {tag}")


def translations(args):
    print("## Translations")
    en = load_resources(os.path.join(RES, "values/strings.xml"))
    for label, folder in LOCALES.items():
        skip = (lambda key: bool(GERMAN_ONLY_IN_VORI.match(key))) if folder == "values-de-rAT" else (lambda key: False)
        check_locale(f"app {label}", en, load_resources(os.path.join(RES, folder, "strings.xml")), skip)
    tag = latest_tag()
    if tag:
        check_stale(en, tag)
    check_swift_tables()
    check_website(args.server)


# ─── Versions ────────────────────────────────────────────────────────────────

def first(pattern, text, default="?"):
    m = re.search(pattern, text, re.M)
    return m.group(1) if m else default


def versions(args):
    print("## Versions")
    android = read("androidApp/build.gradle.kts")
    pbx = read(PBXPROJ)
    readme = read("README.md")
    changelog = readme.split("# Changelog", 1)[-1]
    server_gradle = os.path.join(args.server, "build.gradle.kts")
    spots = {
        "Android versionName": first(r'versionName\s*=\s*"([^"]+)"', android),
        "Desktop desktopVersion": first(r'val desktopVersion\s*=\s*"([^"]+)"', read("composeApp/build.gradle.kts")),
        "iOS MARKETING_VERSION": ",".join(sorted(set(re.findall(r"MARKETING_VERSION = ([^;]+);", pbx)))),
        "Server version": first(r'^version\s*=\s*"([^"]+)"', read(server_gradle)) if os.path.isfile(server_gradle) else "missing",
        "README changelog top": first(r"^###\s+(\S+)", changelog),
    }
    for spot, value in spots.items():
        say("INFO", f"{spot}: {value}")
    version_code = first(r"versionCode\s*=\s*(\d+)", android)
    say("INFO", f"Android versionCode: {version_code}")
    builds = sorted(set(re.findall(r"CURRENT_PROJECT_VERSION = ([^;]+);", pbx)))
    say("OK" if builds == ["1"] else "WARN", f"iOS build numbers: {', '.join(builds)} (expected 1)")

    values = set(spots.values())
    version = spots["Android versionName"]
    if len(values) == 1:
        say("OK", f"every spot holds {version}")
    else:
        say("BLOCKER", "versions differ between the spots above (run /bump-version)")

    tags = run(["git", "tag", "--list", "v*", "--sort=-v:refname"], ".").splitlines()
    say("INFO", f"latest tags: {', '.join(tags[:3]) or 'none'}")
    if f"v{version}" in tags:
        say("BLOCKER", f"tag v{version} exists already: this version is released, bump before releasing again")
    else:
        say("OK", f"tag v{version} not used yet")

    section = re.split(r"^###\s", changelog, flags=re.M)[1] if "###" in changelog else ""
    bullets = len(re.findall(r"^- ", section, re.M))
    say("OK" if bullets else "WARN", f"README changelog {version}: {bullets} bullets")


# ─── To-dos (candidates only) ────────────────────────────────────────────────

RELEASE_WORDS = re.compile(
    r"built|partly|device test|release|ship|beta|older (app|client)|old (app|client)|released client|"
    r"compat|before the update|server first|client first|migration|force", re.I)


def todo_candidates(path, label, open_only):
    if not os.path.isfile(path):
        say("WARN", f"{label}: {path} not found")
        return
    hits = []
    for number, line in enumerate(read(path).splitlines(), 1):
        is_open = line.startswith("- [ ]")
        built_untested = line.startswith("- [x]") and re.search(r"device test open", line, re.I)
        if (is_open and RELEASE_WORDS.search(line)) or (not open_only and built_untested):
            hits.append(f"{path}:{number}: {line[:220]}")
    say("INFO", f"{label}: {len(hits)} candidates to judge")
    for hit in hits:
        print(f"        {hit}")


def todos(args):
    print("## To-dos")
    todo_candidates("plans/Todo.md", "app plans/Todo.md", open_only=False)
    todo_candidates(os.path.join(args.server, "Todo.md"), "server Todo.md", open_only=True)


# ─── Git state ───────────────────────────────────────────────────────────────

def repo_state(path, label):
    if not os.path.isdir(os.path.join(path, ".git")):
        say("WARN", f"{label}: no git repo at {path}")
        return
    run(["git", "fetch", "-q"], path)
    branch = run(["git", "branch", "--show-current"], path)
    dirty = [l for l in run(["git", "status", "--porcelain"], path).splitlines() if l]
    counts = run(["git", "rev-list", "--left-right", "--count", "HEAD...@{upstream}"], path).split()
    ahead, behind = (counts + ["?", "?"])[:2]
    level = "OK" if not dirty and ahead == "0" and behind == "0" else "WARN"
    say(level, f"{label}: branch {branch}, {len(dirty)} uncommitted files, {ahead} unpushed, {behind} not pulled")
    for line in dirty[:SHOWN]:
        print(f"        {line}")


def git(args):
    print("## Git")
    repo_state(".", "app")
    repo_state(args.server, "server")
    beta_only = run(["git", "rev-list", "--count", "origin/main..origin/beta"], args.server)
    if beta_only and beta_only != "0":
        say("WARN", f"server: {beta_only} commits on beta not on main (does this client need them live?)")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("check", nargs="?", default="all", choices=["translations", "versions", "todos", "git", "all"])
    parser.add_argument("--server", default=os.path.join("..", "SchneaggchatV3server"))
    args = parser.parse_args()
    if not os.path.isdir(RES):
        sys.exit("run from the app repo root (SchneaggchatV3)")
    checks = {"translations": translations, "versions": versions, "todos": todos, "git": git}
    for name, check in checks.items():
        if args.check in (name, "all"):
            check(args)
            print()


if __name__ == "__main__":
    main()
