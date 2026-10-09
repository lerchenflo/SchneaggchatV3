# Store screenshots: recreate the demo data

Fills the debug app on an emulator with fictional German demo content (friends with locations
around Vorarlberg, three groups with a poll, direct chats, events), so store screenshots never show
real people or an empty app.

## Steps

1. Install the **debug** build on the emulator. The apply script switches the app itself to German.
2. Log in with the test-server account `luca` and let it sync once.
3. Developer settings → turn on **Demo-Modus**. This hides the offline and test-server bars.
4. Turn on **airplane mode** and keep it on. Any sync overwrites the demo data.
   For the map screenshot the tiles need internet: Developer settings → change the server URL to the
   unreachable `http://10.0.2.2:1`, then airplane mode can go off (sync fails, demo data stays).
   Run the script with `ONLINE_OK=1` in that case. Switch the URL back to the test server afterwards.
5. Run `storeassets/demo/apply_demo_data.sh`. It can be run again; it replaces the previous demo rows.
6. Pin the chats you want on top (vanessa was pinned for the 2026-10 set).
7. Take the screenshots with the `store-image-generator` Claude skill. The raw captures go to
   `storeassets/shots/` (same file names). Then `storeassets/demo/render_store_images.sh` renders all
   store sizes, the icons and the feature graphic with the captions and callouts of the 2026-10 set.

The script backs up the untouched database to `.last-backup-database.db` (gitignored).
Logging out or going online removes the demo data again.

## Files

| File | What it is |
|---|---|
| `apply_demo_data.sh` | Pulls the DB, seeds it, pushes the DB and the profile/group pictures back, then restarts the app |
| `seed_demo_data.py` | All demo content: names, messages, poll, events. Times are relative to "now". Written for Room schema v87; if the schema changed, update the inserts |
| `avatars/` | AI-generated faces (not real people), one per demo user, named like the user |
