---
name: bump-version
description: Bumps the Schneaggchat version for a new release - asks first whether the current desktop version is already released on GitHub (stops if not), proposes the next patch version for the user to confirm, then adds a new changelog heading in README.md and sets the new version in androidApp/build.gradle.kts (versionName + versionCode), the desktopVersion in composeApp/build.gradle.kts, the iOS Version/Build in iosApp.xcodeproj/project.pbxproj, and the server version in SchneaggchatV3server/build.gradle.kts. Use whenever the user wants to bump, raise, update or increase the version, start a new version, cut or prepare a release, or invokes /bump-version.
---

# Bump version

One release bump for every platform. Do the steps in order. Never commit, push, tag or build;
the user does that.

## Server repo exception

This skill is the **only** case where an agent of this repo may edit code in the server repo
`SchneaggchatV3server` (usually `~/Desktop/SchneaggchatV3server`). The user allowed exactly
one change there: the `version = "..."` line in its `build.gradle.kts`. Touch nothing else in
the server repo (no README, no changelog, no other file).

## Version spots

| File | Spot | New value |
|---|---|---|
| `README.md` | heading `### <version>` under `# Changelog` | new heading above the current one |
| `androidApp/build.gradle.kts` | `versionName = "<version>"` | new version |
| `androidApp/build.gradle.kts` | `versionCode = <n>` | current `n` + 1 |
| `composeApp/build.gradle.kts` | `val desktopVersion = "<version>"` | new version |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | every `MARKETING_VERSION = <version>;` (Xcode "Version", 6 spots: app, share extension, notification extension, Debug + Release each) | new version |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | every `CURRENT_PROJECT_VERSION = <n>;` (Xcode "Build") | always `1` |
| `~/Desktop/SchneaggchatV3server/build.gradle.kts` | `version = "<version>"` (top level, next to `group`) | new version |

Do not touch `iosApp/Configuration/Config.xcconfig`: its `MARKETING_VERSION=1.0` and
`CURRENT_PROJECT_VERSION=1` are overridden by the target settings in `project.pbxproj`.
Do not touch the `3.0.19_beta` example in the comment above `desktopVersion`.

## Steps

### 1. Read the current state

The current version is `versionName` in `androidApp/build.gradle.kts`. Read every spot in the
table and list what each one holds. A spot may already hold the new version (the user sometimes
bumps one platform by hand first, e.g. iOS in Xcode); that is fine, step 4 leaves it as it is.
If a spot holds a value that is neither the current version nor a newer one, show the user the
list and ask with AskUserQuestion how to go on before you change anything.

If the server repo or its `build.gradle.kts` is missing, say so; bump the rest and report the
server spot as skipped.

### 2. Ask the user (one AskUserQuestion call, two questions in this order)

1. **GitHub desktop release**: "Did you already release desktop version `<current>` on GitHub
   (tag `v<current>`)?" Options: "Yes, released" / "No, not yet".
2. **New version**: propose the next patch version (`3.0.19` gives `3.0.20`) as the first
   option, marked "(Recommended)"; add the next minor version (`3.1.0`) as a second option. The
   user can type another number with "Other".

If the answer to question 1 is **"No, not yet"**: stop. Change nothing. Tell the user to release
desktop `<current>` on GitHub first (steps in `.github/workflows/workflow_release.md`) and then
run `/bump-version` again. Ignore the version answer.

Check the new version: it must be three numbers `X.Y.Z` and greater than the current version.
If not, ask again.

### 3. Changelog

In `README.md`, insert this block directly under `# Changelog` (above the current topmost
version heading), with one blank line between the parts, like the older entries:

```markdown
### <new>

##### Highlights

#### Features

#### Bugfixes

```

The heading levels are deliberate (`#####` for Highlights, `####` for the rest) and match the
older entries. If a `### <new>` heading already exists, add nothing.

### 4. Version spots

Set every spot in the table to its new value. Use Edit for the single lines. For
`project.pbxproj` replace every `MARKETING_VERSION = ...;` and every
`CURRENT_PROJECT_VERSION = ...;` line, keeping the tabs, e.g.:

```bash
sed -i '' -E 's/(MARKETING_VERSION = )[0-9.]+;/\1<new>;/; s/(CURRENT_PROJECT_VERSION = )[0-9]+;/\11;/' iosApp/iosApp.xcodeproj/project.pbxproj
```

### 5. Check and report

Grep every spot again and confirm each holds its new value (`grep -n "MARKETING_VERSION\|CURRENT_PROJECT_VERSION" iosApp/iosApp.xcodeproj/project.pbxproj`, and so on). Then end with a small table: file, spot, old value, new value (one row per spot, the
server spot marked as the server repo). Do not run Gradle, do not run the
`readme-changelog-updater` agent, do not commit.
