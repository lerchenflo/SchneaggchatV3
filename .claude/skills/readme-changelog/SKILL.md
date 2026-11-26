---
name: readme-changelog
description: Adds a short German README changelog bullet after a user-visible fix or feature. Run inline after every fix/feature, never as a subagent.
---

Inline only (a subagent costs ~50k tokens). Version: `grep -n versionName androidApp/build.gradle.kts`; read only that `### <version>` section of README.md (`grep -n "^##" README.md`).

**Skip** (say why in one line): not visible in the app (refactor, cleanup, tests, docs); tweak to a feature new in this unreleased version; dev-only settings; already covered.

**Write**
- Under `### <versionName>` (never bump; create at top if missing), `#### Features` or `#### Bugfixes`. Never touch Highlights or other entries.
- German, one line, `- <Area>: <2-5 words>` (e.g. `- Schneaggmap: UI verbessert`). Summary, never a detail list. Reuse existing area spelling.
- One bullet per area per subsection: covered → nothing; else append `, <summary>` to the topmost bullet of that area. New area: new bullet at the end of the subsection. No other subsections (the README parser only knows Highlights, Features, Bugfixes).

**Features only**: new feature or changed fact (limit, settings path) → `references/feature-catalog.md`.

Report one line: bullet (quoted) or skip reason, plus to-dos added.
