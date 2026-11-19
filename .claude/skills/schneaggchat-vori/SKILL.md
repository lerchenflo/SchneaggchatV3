---
name: schneaggchat-vori
description: Schneaggchat add-on for the global vori-translator skill - where the Vori (values-de-rAT / strings-de-at) files of the app (SchneaggchatV3) and the website (SchneaggchatV3server) live, brand and feature names that stay unchanged, strings with deliberate jokes, in-game names and tone per feature. Use it together with vori-translator whenever Schneaggchat's Austrian / Vorarlbergerisch strings are translated, filled in or linted, or the user mentions "Schneaggchat translations", de-rAT, strings-de-at or Vori in this project.
---

# Schneaggchat Vori (project add-on)

Run the global **`vori-translator`** skill for the method, the script, the learning loop and the
dialect rules. This file holds only what is specific to Schneaggchat; read it before you start.

## Files

| | App (`SchneaggchatV3/composeApp/src/commonMain/composeResources/`) | Website (`SchneaggchatV3server/src/main/resources/static/i18n/`) |
|---|---|---|
| EN - which keys exist | `values/strings.xml` | `strings-en.xml` |
| DE - wording | `values-de/strings.xml` | `strings-de.xml` (formal "Sie" - Vori still uses "du") |
| AT - Vori, in scope | `values-de-rAT/strings.xml` | `strings-de-at.xml` |

The app's CLAUDE.md forbids Claude to add or change strings in `values-de-rAT` (it is translated by
hand); the only exception is this skill pair, invoked by the user. Orphaned or changed keys are
deleted there by the normal string rule, so they show up as missing for `vori-translator`.

**iOS notification texts, always in the same run** (user, 2026-11-17: "this should never happen"):
the iOS notification service extension has its own string tables in
`iosApp/SchneaggchatNotificationServiceExtension/NotificationStrings.swift`. A key missing in
`austrianGermanStrings` shows **English** on iOS (the app falls back to German). Every Vori run
also fills `austrianGermanStrings` until it has every key of `englishStrings`:

- Take the text from the matching `values-de-rAT` key when there is one (e.g.
  `map_sighting_noti_police` → `.mapSightingPoliceTitle`, `system_event_cancelled` →
  `.eventCancelledBody`); translate only what has no app counterpart.
- Swift placeholders: `%1$s` → `%1$@`, `%1$d` → `%1$ld`; keep the index.
- `vori_strings.py missing` does not see this file, and it skips `<plurals>`: check both by hand
  (compare each `plurals name` of `values/strings.xml` with `values-de-rAT`, and the keys of the
  four Swift tables).

## Names that stay unchanged

Schneaggchat, Schneaggmap, Schneagg / Schneagga, Wordle, Yahtzee, Tetris, Undercover, Mr. White.

## Deliberate jokes (typo fixes only, lint warnings expected)

- Undercover rules texts (`undercover_rules_*_text`), e.g. "gang ham" ("ham" is otherwise an avoid
  form of "huam").
- Heißluftballon / Schneagg-Rakete: "Rakete" (button: "Raketa") is on purpose, don't fix.

## Keys that stay German (never translate)

- Feature quiz question texts: `featurequiz_*_question`, `featurequiz_*_answer_1..3`,
  `featurequiz_*_none`, `featurequiz_*_explanation` (user, 2026-11-16: "de-rat is not used here").
  They are left out of `values-de-rAT` on purpose so Vori users get the German text; `missing` lists
  them, skip them. The quiz's other strings (`featurequiz_title`, `featurequiz_next`, ...) are Vori.

## Tone per feature

- Humour in games, Recap, empty states and the rodeo game ("Abhaua" for "Gruppe verlassen").
- Rodeo instructions keep controls and goal, nothing else: "wenn se di erwischand, isch s End vom
  Ritt".
- In-game names are localised freely, e.g. Milchlaster → Milchlastwaga, Free Candy Bus → Ghettobus.
- Review a feature in bulk with `vori_strings.py dump --prefix games_` (or another key prefix).
