---
name: Schneaggchat V3 changelog patterns
description: Version format, language, structure, and location of changelog in this project
type: reference
---

**README.md location**: `/home/flo/Desktop/SchneaggchatV3/README.md`

**Changelog section**: Located under `# Changelog` heading (line 10+)

**Version format**: `### X.Y.Z` (semantic versioning without date)
- Current version: 3.0.10
- No date appended to version heading

**Language**: German for older entries, mixed German/English recently
- Subsection headings in German: `#### Features` and `#### Bugfixes`
- Entry text: German ("Entwicklerstatus direkt übernehmen") and English ("Direct APNs (iOS) + Direct FCM (Android)...")
- Note: CLAUDE.md says "all new code must be in English" — changelog entries should follow code language (English for new features)

**Structure**: 
- Version heading → Features subsection → Bugfixes subsection
- Each entry is a single bullet point, no detailed descriptions
- Empty bullet points should be removed or filled
- Most recent version at top

**Recent entry example** (v3.0.10):
- Features describe what was added, in plain language (e.g., "Direct APNs + FCM with shared content builder")
- Bugfixes are typically one-liners ("Token sync fix")

**Next update**: Append to v3.0.10 Features or create v3.0.11 section as needed
