---
name: Schneaggchat V3 Project Structure
description: Key locations for version info and changelog format
type: reference
---

## Version Location
- **gradle/libs.versions.toml** does NOT contain app version (version tracked only in README)
- **README.md** contains the changelog section starting at line 10
- Current version: **3.0.11** (added 2026-05-19)

## Changelog Format
- Section: `# Changelog`
- Version headers: `### X.Y.Z` (no date needed)
- Subsections: `#### Features` and `#### Bugfixes`
- One bullet per entry, present tense
- Most recent version at the top

## Project Language
- English for code/variables/functions
- German mixed in documentation and changelog entries (user's preference)
- Changelog entries can be either language depending on context
