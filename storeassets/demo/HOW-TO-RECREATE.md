# Store screenshots: recreate the demo data

Fills the debug app on an emulator with fictional German demo content (friends with locations
around Vorarlberg, three groups with a poll, direct chats, events), so store screenshots never show
real people or an empty app.

## Automatic (recommended)

1. Start the emulator (developed on the **Pixel 10 Pro** AVD, 1280x2856), install the **debug**
   build and log in with the test-server account `luca`.
2. Run

   ```bash
   python3 storeassets/demo/capture_store_shots.py \
     --web-out ../SchneaggchatV3server/src/main/resources/static/web_images/screens
   ```

   It runs every step below by itself, without any check in between, and writes the 7 raw
   captures to `storeassets/shots/` (clean status bar: 09:41, full battery). `--web-out` also writes
   the 600 px WebP copies the website shows; leave it out if you only need the raw shots.
3. When done: run it once more with `--restore`, or switch the server URL back to the test server
   in the developer settings. The next sync then replaces the demo data.

Takes about 3 minutes. Needs `adb`, `sqlite3`, `python3`, ImageMagick 7 (`magick`) and, for
`--web-out`, `cwebp` (`brew install imagemagick webp`).

Limits:
- The map taps (`MAP_TAPS`) and the tap on Mia's marker are screen coordinates for the Pixel 10 Pro
  AVD. On another device size the map (03) and compass (06) shots need new values.
- The C-Challenge (07) shows that day's puzzle. If today's puzzle was already solved on this
  account, the screen shows the result instead.

## What the script does (manual steps, if you need them)

1. Developer settings → turn on **Demo-Modus**. This hides the offline and test-server bars.
2. Developer settings → change the server URL to the unreachable `http://10.0.2.2:1`. Internet stays
   on, so the map tiles load, but no sync can overwrite the demo data. (Alternative: airplane mode,
   then run `apply_demo_data.sh` without `ONLINE_OK=1`.)
3. Set the emulator's own position to Dornbirn: `adb emu geo fix 9.7435 47.4118 430`. The compass
   distances and the map's own-location dot come from it.
4. Run `ONLINE_OK=1 storeassets/demo/apply_demo_data.sh`. It can be run again; it replaces the previous
   demo rows and restarts the app.
5. Pin vanessa's chat (long-press → "Chat anpinnen").
6. Capture, with the status bar in demo mode:
   - `01-chats`: chat list, scrolled to the top
   - `02-chat`: group "Skitour Bödele", poll title near the top, emma's message and reactions below
   - `05-umfrage`: same chat scrolled up, the whole poll with the message above it
   - `04-events`: Events tab
   - `03-karte`: Karte tab from its default view: tap the friends cluster, tap it again, double-tap
     once more (two clusters, single faces around them)
   - `06-kompass`: tap the "Mia" chip, tap her marker, "Kompass"
   - `07-spiel`: Spiele → C-Challenge, intro dismissed

The seeding backs up the untouched database to `.last-backup-database.db` (gitignored).
Logging out or syncing with the real server removes the demo data again.

The store images (captions, phone frame, zoomed boxes) are rendered from these raw shots by
`render_store_images.sh`. Its callout regions are still measured for the older 1080x2400 captures.

## Files

| File | What it is |
|---|---|
| `capture_store_shots.py` | Runs all steps above unattended and captures the raw shots (optionally website copies) |
| `apply_demo_data.sh` | Pulls the DB, seeds it, pushes the DB and the profile/group pictures back, then restarts the app |
| `seed_demo_data.py` | All demo content: names, positions, messages, poll, events. Times are relative to "now". Written for Room schema v87; if the schema changed, update the inserts |
| `render_store_images.sh` | Renders the store images from `storeassets/shots/` |
| `avatars/` | AI-generated faces (not real people), one per demo user, named like the user |
| `../shots/` | The raw captures (1280x2856), kept in git for reuse |
