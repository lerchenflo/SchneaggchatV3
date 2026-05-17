---
name: Schneaggchat V3 Project Structure
description: Key locations for version info and changelog format
metadata:
  type: reference
---

## Version Location
- **androidApp/build.gradle.kts** (lines 29-30): `versionCode` and `versionName = "3.0.11"`
- **README.md** contains the changelog section starting at line 16
- Current version: **3.0.11**

## Changelog Format
- Section: `## Changelog` (line 16 in README)
- Version headers: `### X.Y.Z` (no date needed)
- Subsections: `#### Features` and `#### Bugfixes`
- One bullet per entry, present tense
- Most recent version at the top
- **All changelog text is in German** (e.g., "Email Provider Warnung", "Bugfixes")

## Project Language
- English for code/variables/functions and new code
- German in changelog entries and existing German code
- CLAUDE.md: New code must be in English, do not rename existing German code unless modifying that file
