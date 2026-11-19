---
name: pre-release-check
description: Schneaggchat pre-release check - before an app release, checks that every text is translated in every language (EN/DE/IT/Vori strings and plurals, the iOS notification tables, the server website texts), that the version matches in every spot and is not released yet, that every open or partly built to-do (app and server Todo.md) can ship as it is or must be finished first (release order, older clients, server on beta), the git state of both repos and a compile check. Reports a table of blockers and warnings, then asks what to fix. Use whenever the user asks for a pre-release check, release check or "are we ready to release", invokes /pre-release-check, and always before creating or pushing a GitHub release tag.
---

# Pre-release check

Run it before every release, and always before a GitHub release tag (`v*`) is created or pushed:
a blocker stops the tag until the user decides. It only reads and reports; fixes happen after the
user picks them in step 7.

## 1. Run the script

From the app repo root:

```bash
python3 -I .claude/skills/pre-release-check/scripts/release_check.py all
```

It prints `BLOCKER`, `WARN`, `OK` and `INFO` lines in four sections. `--server <path>` if the server
repo is not next to the app repo (`../SchneaggchatV3server`). Single sections: `translations`,
`versions`, `todos`, `git`.

## 2. Translations

The script checks:

- `values-de`, `values-it`, `values-de-rAT`: missing strings and plurals, placeholders that differ
  from EN (a crash or wrong text), keys that are not in EN any more. In `values-de-rAT` the
  feature quiz question texts stay German on purpose (`schneaggchat-vori` skill) and are skipped.
- EN texts changed since the last `v*` tag while a translation kept its old text (stale meaning).
- The four tables of `iosApp/SchneaggchatNotificationServiceExtension/NotificationStrings.swift`
  against the `NotificationStringKey` enum, with placeholders against `englishStrings`.
- The server website texts (`strings-en` / `-de` / `-de-at` in the server's `static/i18n/`).

Then read the strings added or changed since the last tag (`git diff <tag> -- composeApp/src/commonMain/composeResources iosApp/SchneaggchatNotificationServiceExtension`)
and check the meaning: does DE / IT / Vori say what EN says now? A wrong meaning is a WARN, a wrong
fact (limit, setting path, what gets deleted) is a BLOCKER.

## 3. Versions

Every spot must hold the same version (Android `versionName`, `desktopVersion`, iOS
`MARKETING_VERSION`, server `version`, README changelog top heading), the iOS build number is 1,
and no `v<version>` tag exists yet (else the version is released already: `/bump-version` first).
An empty changelog section for the version is a WARN: the GitHub release notes come from it.

## 4. To-dos: can each one ship as it is?

The script lists candidates from `plans/Todo.md` (open items that mention built / partly /
release / beta / older clients / migration ..., and done items with "device test open") and from
the server's `Todo.md`. Read every candidate (and the plan section it points to when the item
alone doesn't answer it) and sort it into one of:

| Verdict | Meaning | Severity |
|---|---|---|
| Must be done before the release | Half-built code is reachable by users, a migration or forced update is missing, a release-order item says this client or the server must go first and that is not done | BLOCKER |
| Needs the server first / at the same time | The client uses server code that is only on `beta` (the script shows how many `beta` commits are not on `main`) | BLOCKER until the user says which server goes live with this release |
| Older clients break | A server change makes released app versions fail or show wrong things (check "older clients" / "released client" notes) | BLOCKER or WARN, by impact |
| Device test open | Built but never tested on a device | WARN, list them as a test list |
| Ships as is | Not user-reachable yet, or independent of the release | not listed |

If it is unclear which server version will be live after the release, ask the user in step 7
instead of guessing.

## 5. Git state

Uncommitted, unpushed or not pulled changes in either repo are a WARN (a BLOCKER before tagging:
the tag must point to the pushed, final commit). Report the server branch.

## 6. Compile check

Only when the machine is not a laptop (`pmset -g batt | grep -q InternalBattery` matches = laptop,
then skip and say so):

```bash
./gradlew :composeApp:compileKotlinJvm :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64 -q
```

A failure only in `build/generated/compose/resourceGenerator/.../ActualResourceCollectors.kt`
("Unresolved reference '_collectCommonMain...Resources'") is stale generated code: delete
`composeApp/build/generated/compose/resourceGenerator` and compile again. Any other error is a
BLOCKER.

## 7. Report and ask

End with one table, blockers first:

| Area | Severity | Finding | Where | Fix |
|---|---|---|---|---|

Then ask with AskUserQuestion (multiSelect) which findings to fix now. Fix routes:

- DE / IT strings and the iOS German / Italian / English tables: translate directly (project string rule).
- Vori (`values-de-rAT`, iOS `austrianGermanStrings`): run the `vori-translator` skill with the
  `schneaggchat-vori` add-on; it always fills the iOS table in the same run.
- Website texts and every other server change: hand over to the server (live session via
  `SendMessage`, else `SchneaggchatV3server/Todo.md`); never edit server files.
- Versions: `/bump-version`.
- To-dos: interview the user about each picked item before building.
- Device tests: give the user the test list; nothing to build.

Never fix anything the user did not pick, and never push a release tag while a blocker is open
unless the user says so explicitly.
