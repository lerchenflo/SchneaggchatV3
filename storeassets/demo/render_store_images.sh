#!/usr/bin/env bash
# Render all store screenshot sizes from the raw captures in storeassets/shots/ (see HOW-TO-RECREATE.md).
# Needs the store-image-generator skill (compose_shot.sh) and ImageMagick 7.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE="${COMPOSE:-$HOME/.claude/skills/store-image-generator/scripts/compose_shot.sh}"
FONT="${FONT:-/usr/share/fonts/noto/NotoSans-ExtraBold.ttf}"
LOCALE=de

# Colours from app/theme/Color.kt: primary -> primaryContainer, tertiary as the bezel
STYLE=(--bg1 '#006E2C' --bg2 '#1ED660' --fg '#FFFFFF' --frame '#0D0D0D' --font "$FONT" --device-width 76 --caption-size 6.6)

# name | caption | callout args (source-screenshot pixels; empty = none)
SHOTS=(
  "01-chats|Dein Chat.\nMade in Vorarlberg.|--callout 30,672,1020,150 --callout-scale 1.3"
  "02-chat|Gruppen, Reaktionen,\nUmfragen und mehr.|--callout 20,1806,700,346 --callout-scale 1.5"
  "03-karte|Live-Standort teilen –\nnur mit Freunden.|"
  "04-events|Vom Chat\nzum Treffen.|--callout 20,780,1040,380 --callout-scale 1.3"
  "05-umfrage|Abstimmen statt\nendlos diskutieren.|"
  "06-kompass|Finde deine Leute –\nauch im Gedränge.|--callout 20,1680,1040,150 --callout-scale 1.3"
  "07-spiel|Tägliche Rätsel, Spiele\nund Highscores.|--callout 30,666,1020,378 --callout-scale 1.3 --callout-anchor top"
)

# folder | canvas size (see the skill's references/store-sizes.md)
TARGETS=(
  "ios/iphone|1206x2622"
  "ios/iphone-duo|2007x2853"
  "ios/ipad|2064x2752"
  "play/phone|1080x2400"
)

for target in "${TARGETS[@]}"; do
  IFS='|' read -r folder size <<< "$target"
  for shot in "${SHOTS[@]}"; do
    IFS='|' read -r name caption callout <<< "$shot"
    # shellcheck disable=SC2086 # callout holds several flags on purpose
    "$COMPOSE" --shot "$ROOT/storeassets/shots/$name.png" --out "$ROOT/storeassets/$folder/$LOCALE/$name.png" \
      --caption "$caption" --size "$size" "${STYLE[@]}" $callout
  done
done

# App icons (1024 App Store, 512 Play) and the 1024x500 Play feature graphic
SCRIPTS="$(dirname "$COMPOSE")"
"$SCRIPTS/make_icons.sh" --src "$ROOT/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/schneaggchat_logo_playstore_v3.png" \
  --bg '#FFFFFF' --out-dir "$ROOT/storeassets/icon"
"$SCRIPTS/feature_graphic.sh" --layout phones --out "$ROOT/storeassets/play/feature-graphic/$LOCALE/phones.png" \
  --bg1 '#006E2C' --bg2 '#1ED660' --frame '#0D0D0D' --font "$FONT" --tagline 'Dein Chat. Made in Vorarlberg.' \
  --shot "$ROOT/storeassets/shots/03-karte.png" --shot "$ROOT/storeassets/shots/01-chats.png" --shot "$ROOT/storeassets/shots/04-events.png"
