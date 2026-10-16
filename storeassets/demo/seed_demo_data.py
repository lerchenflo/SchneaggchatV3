"""Seed a pulled SchneaggchatV3 Room DB (schema v87) with German demo data for store screenshots.

Usage: python3 seed_demo_data.py database.db   (WAL-checkpointed copy; apply_demo_data.sh does all of it)

Built for the test-server account "luca" whose friends vanessa/emma/noah already exist on the
device; the logged-in user is detected automatically, missing old friends are simply skipped.
All other people, groups, messages, the poll and the events are fictional and created here.
Times are relative to "now", so re-running later gives fresh "today" timestamps."""
import datetime, json, sqlite3, sys, time

PKG = "org.lerchenflo.schneaggchatv3mp.debug"
FILES = f"/data/user/0/{PKG}/files"
ME = None  # logged-in user, detected below (the only user row without a friendship status)
VANESSA, EMMA, NOAH = "6a93f644c99a8eb72043ae00", "6a93f50ec99a8eb72043adcc", "6a93f46ac99a8eb72043adbf"
NEW = {  # id: (name, birthDate, lat, long, battery, speed)
    "de0000000000000000000001": ("Jonas", "2001-11-12", 47.418, 9.733, 64, 0.0),
    "de0000000000000000000002": ("Sophie", "2002-04-03", 47.4245, 9.752, 88, 0.0),
    "de0000000000000000000003": ("Max", "2000-07-21", 47.401, 9.745, 41, 13.0),
    "de0000000000000000000004": ("Mia", "2003-01-30", 47.41, 9.728, 92, 0.0),
    "de0000000000000000000005": ("Paul", "1999-10-15", 47.398, 9.76, 23, 0.0),
    "de0000000000000000000006": ("Lena", "2001-06-08", 47.43, 9.738, 70, 0.0),
    "de0000000000000000000007": ("Tobias", "1998-02-17", 47.406, 9.77, 35, 0.0),
    "de0000000000000000000008": ("Alex", "2002-12-01", 47.42, 9.764, 59, 0.0),
}
JONAS, SOPHIE, MAX, MIA, PAUL, LENA, TOBIAS, ALEX = NEW
EXISTING_LOC = {VANESSA: (47.4140, 9.7400, 77), EMMA: (47.4150, 9.7510, 55), NOAH: (47.4030, 9.7330, 81)}
# All around Dornbirn, so one map zoom level shows every face without clustering
G_SKI, G_WG, G_FB = "de00000000000000000000a1", "de00000000000000000000a2", "de00000000000000000000a3"

now = int(time.time() * 1000)
MIN, HOUR, DAY = 60_000, 3_600_000, 86_400_000
db = sqlite3.connect(sys.argv[1])
c = db.cursor()
own = c.execute("select id, name from users where frienshipStatus is null or frienshipStatus = ''").fetchall()
if len(own) != 1:
    sys.exit(f"cannot tell the logged-in user apart, candidates: {own}")
ME, ME_NAME = own[0]
# Re-runnable: drop the rows of a previous run first (all demo ids start with "de0000")
c.execute("delete from message_readers where messageId like 'de0000%'")
c.execute("delete from messages where id like 'de0000%'")
c.execute("delete from group_members where groupId like 'de0000%'")
present = {row[0] for row in c.execute("select id from users")}
# Old friends that are not on this device are left out of groups and events
EXISTING_LOC = {k: v for k, v in EXISTING_LOC.items() if k in present}

# --- users ---------------------------------------------------------------
for uid, (name, bday, lat, lng, bat, speed) in NEW.items():
    c.execute("""insert or replace into users (id, updatedAt, profilePicUpdatedAt, name, nickName, description, status,
        profilePictureUrl, locationLat, locationLong, locationDate, locationSpeed, locationHeading, locationAltitude,
        locationBattery, locationDistance24h, frienshipStatus, requesterId, locationShared, shareSpeedHeading, snailTrail,
        wakeupEnabled, notisMuted, lastSeen, birthDate, phoneNumber, email, emailVerifiedAt, createdAt)
        values (?,?,?,?,'','','',?,?,?,?,?,?,?,?,?, 'ACCEPTED', ?, 1, 1, 0, 0, 0, ?, ?, null, null, null, ?)""",
              (uid, now, now, name, f"{FILES}/{uid}.upb", lat, lng, now - 4 * MIN, speed, 90.0, 430.0, bat, 12_000.0,
               ME, now - 10 * MIN, bday, now - 300 * DAY))
for uid, (lat, lng, bat) in EXISTING_LOC.items():
    c.execute("""update users set locationLat=?, locationLong=?, locationDate=?, locationSpeed=0, locationHeading=0,
        locationBattery=?, locationShared=1, shareSpeedHeading=1, lastSeen=? where id=?""",
              (lat, lng, now - 7 * MIN, bat, now - 20 * MIN, uid))

# --- groups --------------------------------------------------------------
groups = {
    G_SKI: ("Skitour Bödele ⛷️", "Wer kommt mit aufs Bödele?", [ME, JONAS, SOPHIE, MAX, EMMA]),
    G_WG: ("WG Dornbirn 🏠", "Putzplan, Einkauf und Pizza", [ME, MIA, PAUL, NOAH]),
    G_FB: ("Fußball Freitag ⚽", "Jeden Freitag 18:30 im Birkenwiese", [ME, JONAS, MAX, PAUL, VANESSA, NOAH]),
}
known = present | set(NEW)
groups = {g: (n, d, [u for u in m if u in known]) for g, (n, d, m) in groups.items()}
colors = [0xFF1ED660, 0xFF005EB2, 0xFFE57373, 0xFFFFB74D, 0xFF9575CD, 0xFF4DD0E1]
for gid, (name, desc, members) in groups.items():
    c.execute("insert or replace into groups values (?,?,?,?,?,?,?,null,0)",
              (gid, name, f"{FILES}/{gid}.gpb", desc, now - 120 * DAY, now, now))
    for i, uid in enumerate(members):
        mname = c.execute("select name from users where id=?", (uid,)).fetchone()[0]
        c.execute("insert or replace into group_members (groupId, userId, joinDate, admin, color, memberName) values (?,?,?,?,?,?)",
                  (gid, uid, str(now - 120 * DAY), 1 if i == 0 else 0, colors[i % len(colors)] - (1 << 32), mname))

# --- messages ------------------------------------------------------------
msg_no = 0
def msg(sender, receiver, text, ago, group=False, read=True, reactions=(), msg_type="TEXT", poll=None, answer=None):
    global msg_no
    if sender not in known or (not group and receiver not in known):
        return None
    reactions = [(u, e) for u, e in reactions if u in known]
    msg_no += 1
    mid = f"de0000000000000000{msg_no:06d}"
    send = now - ago
    react = [{"userId": u, "content": e, "reactedAt": send + MIN} for u, e in reactions]
    c.execute("""insert into messages (id, msgType, content, poll, systemEvent, pictureUrl, audioPath, senderId, receiverId,
        sendDate, updatedAt, deleted, edited, myMessage, readByMe, groupMessage, answerId, sent, reactions, version, clientMessageId)
        values (?,?,?,?,null,null,null,?,?,?,?,0,0,?,?,?,?,1,?,1,null)""",
              (mid, msg_type, text, json.dumps(poll, ensure_ascii=False) if poll else None, sender, receiver, send, str(send),
               int(sender == ME), int(read or sender == ME), int(group), answer, json.dumps(react, ensure_ascii=False)))
    if sender == ME:  # read ticks
        readers = groups[receiver][2] if group else [receiver]
        for r in readers:
            if r != ME:
                c.execute("insert into message_readers (messageId, readerID, readDate) values (?,?,?)", (mid, r, str(send + 2 * MIN)))
    return mid

# Skitour group: lively chat + poll, newest -> top of the chat list
msg(JONAS, G_SKI, "Leute, am Wochenende soll's 40 cm Neuschnee geben ❄️", 5 * HOUR, True, reactions=[(SOPHIE, "🔥"), (MAX, "🔥"), (ME, "😍")])
msg(SOPHIE, G_SKI, "Bin dabei! Felle sind schon gewachst 😄", 4 * HOUR + 50 * MIN, True)
msg(ME, G_SKI, "Ich auch. Treffpunkt beim Parkplatz Bödele?", 4 * HOUR + 40 * MIN, True, reactions=[(JONAS, "👍"), (EMMA, "👍")])
msg(MAX, G_SKI, "Passt, ich nehm die Lawinenausrüstung mit", 4 * HOUR + 30 * MIN, True)
poll = {
    "creatorId": JONAS, "title": "Wann starten wir am Samstag?", "description": "Früh = besserer Schnee 😉",
    "maxAnswers": 1, "customAnswersEnabled": True, "maxAllowedCustomAnswers": None, "visibility": "PUBLIC",
    "expiresAt": now + 2 * DAY, "allowDeleteOptions": False, "showCheckboxes": True, "visibleToAll": True,
    "voteOptions": [
        {"id": "de00000000000000000p0001", "text": "7:00 – Sonnenaufgang 🌅", "custom": False, "creatorId": JONAS,
         "voters": [{"userId": JONAS, "votedAt": now - 3 * HOUR}, {"userId": SOPHIE, "votedAt": now - 3 * HOUR},
                    {"userId": ME, "votedAt": now - 2 * HOUR}], "maxVoters": None, "createdByMe": False,
         # Follow-up poll for everyone who picked the early start
         "subPoll": {
             "creatorId": JONAS, "title": "Wer fährt? 🚗", "description": None, "maxAnswers": 1,
             "customAnswersEnabled": False, "maxAllowedCustomAnswers": None, "visibility": "PUBLIC",
             "expiresAt": now + 2 * DAY, "allowDeleteOptions": False, "showCheckboxes": True, "visibleToAll": True,
             "voteOptions": [
                 {"id": "de00000000000000000p0011", "text": "Ich fahre (4 Plätze)", "custom": False, "creatorId": JONAS,
                  "voters": [{"userId": ME, "votedAt": now - 2 * HOUR}], "maxVoters": 1, "createdByMe": False, "subPoll": None},
                 {"id": "de00000000000000000p0012", "text": "Ich fahre mit", "custom": False, "creatorId": JONAS,
                  "voters": [{"userId": JONAS, "votedAt": now - 3 * HOUR}, {"userId": SOPHIE, "votedAt": now - 3 * HOUR}],
                  "maxVoters": None, "createdByMe": False, "subPoll": None},
             ],
         }},
        {"id": "de00000000000000000p0002", "text": "9:00", "custom": False, "creatorId": JONAS,
         "voters": [{"userId": MAX, "votedAt": now - 2 * HOUR}], "maxVoters": None, "createdByMe": False, "subPoll": None},
        {"id": "de00000000000000000p0003", "text": "11:00 – ausschlafen 😴", "custom": True, "creatorId": EMMA,
         "voters": [{"userId": EMMA, "votedAt": now - HOUR}], "maxVoters": None, "createdByMe": False, "subPoll": None},
    ],
}
def keep_known_voters(p):
    for option in p["voteOptions"]:
        option["voters"] = [v for v in option["voters"] if v["userId"] in known]
        if option["subPoll"]:
            keep_known_voters(option["subPoll"])
keep_known_voters(poll)
msg(JONAS, G_SKI, poll["title"], 3 * HOUR, True, msg_type="POLL", poll=poll)
msg(EMMA, G_SKI, "11 Uhr gewinnt sicher nicht 😂", 50 * MIN, True, read=False, reactions=[(SOPHIE, "😂"), (MAX, "Wetten? 😏")])
msg(SOPHIE, G_SKI, "Ich bring Tee und Kuchen mit 🍰", 12 * MIN, True, read=False, reactions=[(MAX, "❤️"), (JONAS, "❤️")])

# WG group
msg(MIA, G_WG, "Wer hat meinen Joghurt gegessen?? 😤", 9 * HOUR, True, reactions=[(PAUL, "😂"), (NOAH, "😇")])
msg(PAUL, G_WG, "Ich wars nicht 🙈", 8 * HOUR + 55 * MIN, True)
msg(ME, G_WG, "Heute Abend Pizza? Ich bestell 🍕", 2 * HOUR, True, reactions=[(MIA, "❤️"), (PAUL, "🙌"), (NOAH, "🙌")])
msg(NOAH, G_WG, "Für mich eine Diavola bitte!", 40 * MIN, True, read=False)

# Football group
msg(MAX, G_FB, "Freitag 18:30 wie immer? Wer ist dabei?", DAY + 3 * HOUR, True)
msg(VANESSA, G_FB, "Bin dabei ⚽", DAY + 2 * HOUR, True, reactions=[(MAX, "💪")])
msg(ME, G_FB, "Ich auch, bring den Ball mit", DAY + HOUR, True)

# Direct chats
msg(EMMA, ME, "Hast du heute schon etwas vor?", 3 * HOUR, read=True)
msg(ME, EMMA, "Noch nichts, warum? 😄", 2 * HOUR + 50 * MIN)
msg(EMMA, ME, "Kaffee in der Stadt? ☕", 25 * MIN, read=False)
msg(SOPHIE, ME, "Danke fürs Mitnehmen gestern! 🙏", DAY + 5 * HOUR)
msg(ME, SOPHIE, "Gern, jederzeit!", DAY + 4 * HOUR, reactions=[(SOPHIE, "❤️")])
msg(JONAS, ME, "Schickst du mir die Route vom Samstag?", 6 * HOUR)
msg(ME, JONAS, "Klar, schau auf die Karte, hab sie eingezeichnet 🗺️", 5 * HOUR + 40 * MIN, reactions=[(JONAS, "👍")])
msg(MIA, ME, "Alles Gute zum Geburtstag 🎉🎂", 2 * DAY)
msg(ME, MIA, "Danke dir!! 😊", 2 * DAY - HOUR)
msg(LENA, ME, "Kommst du am Samstag auch zum Grillen? 🔥", 26 * HOUR)
msg(ME, LENA, "Fix! Ich bring Würstel mit 🌭", 25 * HOUR, reactions=[(LENA, "😂")])
msg(TOBIAS, ME, "Danke für den Tipp mit dem Radweg 🚴", 3 * DAY)
msg(ALEX, ME, "Wo bist du gerade? Ich seh dich auf der Karte 😄", 9 * HOUR)
msg(ME, ALEX, "Beim Bahnhof, komm vorbei!", 8 * HOUR + 50 * MIN)
msg(PAUL, ME, "Bist du schon zu Hause?", 7 * HOUR)
msg(ME, PAUL, "In 10 Minuten 🚲", 6 * HOUR + 55 * MIN)

# --- events --------------------------------------------------------------
def event(eid, etype, title, desc, lat, lng, start, invited, accepted, group_id=None, max_users=None):
    invited = [u for u in invited if u in known]
    parts = [{"userId": u, "status": "ACCEPTED", "updatedAt": now - HOUR} for u in accepted if u in known]
    c.execute("insert or replace into events values (?,?,?,?,?,?,?,?,null,?,?,?,?,?,?,?,?,?)",
              (eid, ME, etype, title, desc, group_id, json.dumps({"lat": lat, "long": lng}), start,
               json.dumps(invited), json.dumps(parts), "FRIENDS_ONLY", max_users, "ONE_DAY", now - DAY, now - HOUR, ME, ME_NAME))

def at(days, hour, minute=0):
    """Local wall-clock time `days` from today - events start at round times, not 'now + x hours'."""
    day = datetime.datetime.now().astimezone() + datetime.timedelta(days=days)
    return int(day.replace(hour=hour, minute=minute, second=0, microsecond=0).timestamp() * 1000)

everyone = [JONAS, SOPHIE, MAX, MIA, PAUL, LENA, TOBIAS, ALEX, VANESSA, EMMA, NOAH]
event("de00000000000000000e0001", "FOOD", "Grillabend am See 🔥", "Bring was zum Grillen mit, Getränke sind organisiert!",
      47.5070, 9.7390, at(2, 18, 30), everyone, [ME, SOPHIE, MIA, PAUL, LENA, VANESSA, NOAH])
event("de00000000000000000e0002", "SPORT", "Skitour aufs Bödele ⛷️", "Treffpunkt Parkplatz Bödele, Lawinenausrüstung nicht vergessen.",
      47.4269, 9.8158, at(3, 7, 0), [JONAS, SOPHIE, MAX, EMMA], [ME, JONAS, SOPHIE, MAX], max_users=8)
event("de00000000000000000e0003", "GAMING", "Spieleabend 🎲", "Catan, Uno und Pizza in der WG.",
      47.4136, 9.7432, at(5, 19, 30), everyone, [ME, MIA, PAUL, EMMA])

db.commit()
print("users", c.execute("select count(*) from users").fetchone()[0],
      "groups", c.execute("select count(*) from groups").fetchone()[0],
      "messages", c.execute("select count(*) from messages").fetchone()[0],
      "events", c.execute("select count(*) from events").fetchone()[0])
