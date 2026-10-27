# Game Ideas Collection

Collection of game ideas for the games feature. Each entry holds the basic idea only, not an implementation plan.

## Target
Multiplayer games for **friend groups sitting together in one room**, each player on their own phone. Shouting, watching the other team and physical chaos are part of the fun.

What this means for all ideas:
- **Sound**: many phones playing the same sound out of sync becomes an echo mess. Play shared audio (beat, chant) on one phone only (host / cox), or start it on all phones from a synced clock.
- **Mic**: room noise and other players shouting reach every phone's mic. Mic mechanics need a loud threshold or must be limited to one player at a time.
- **Safety**: players sit close together, so swinging motions can hit people. Gestures should stay small enough for a sofa.
- **Peeking**: players can see the other team's screens. That's fine and part of the fun.
- **Same place**: GPS and heading are the same for everyone, so "face the same wall and tap" works for calibrating a relative heading.

---

## 1. Dirt War (working title) — team digging with phone motion

### Idea
Teams dig holes by moving the phone like a shovel (Wii-style motion control). Every shovel of dirt has to land somewhere: **dirt thrown over the shoulder lands in the enemy team's hole**. Digging is attack and defence at once.

### Phone = tool, each layer needs its own motion
| Layer       | Motion                                      |
|-------------|---------------------------------------------|
| Soil        | Scoop: tilt down, flick up and back         |
| Clay        | Twist like a corkscrew                      |
| Rock        | Pickaxe: sharp downward hit (haptic thud)   |
| Roots       | Saw: fast back and forth                    |
| Groundwater | Bail: tilt sideways and pour                |
| Permafrost  | Rub fast to warm it                         |

The next layer is unknown, so the team has to switch motions fast.

### Team roles
- **Digger**: shovels.
- **Bucket chain**: swipe-throws dirt to a teammate, who "catches" by holding the phone flat at the right moment.
- **Bracer**: holds the phone still to place support beams. Digging too fast without bracing makes the walls cave in.
- **Sifter**: shakes the phone like a sieve to find treasure in the dirt.
- **Rhythm bonus**: the team digs on the beat of a work chant for double dirt.
- **Desktop player** (no motion sensors): plays the Earth or a mole. Places rocks, water veins and roots with the mouse, and tunnels under holes to steal treasure.

### Twists / modes
- **Dig through the Earth**: first team through wins and comes out at the real GPS antipode (from Vorarlberg: Pacific south-east of New Zealand, so the hole floods). Controls flip upside down.
- **Holes on Schneaggmap**: craters stay at the real GPS spot and other users can see them.
- **Bury things**: bury a chat message, photo or voice note; someone digs it up later (time capsule).
- **Buried friend**: one player is buried with a black screen and has to shout into the mic or tap SOS. Diggers find them through hot/cold vibration before the air runs out.
- **Tunnel meet**: two teams dig from both sides of a mountain (Arlberg tunnel) and must meet in the middle. Direction comes from the real heading. Tunnels that cross start a mole fight.
- **Drain the Bodensee**: teams AT / CH / DE dig channels to steal lake water into their country (the upper lake has no agreed border).
- **Earthquake**: the whole team shakes together to collapse the enemy's walls.
- **Blind mode**: dark screen, only haptics and sound.
- **Absurd finds**: old Nokia, Käsknöpfle pot, the Funken witch, a giant sleeping Schneagg, the Bodensee drain plug.

### Feasibility
- Use fused sensors (Android `ROTATION_VECTOR` / `LINEAR_ACCELERATION`, iOS `CMDeviceMotion`), not raw gyro. KMP has no shared sensor API, so it needs a small `expect/actual` layer.
- **Not possible**: tracking where the phone is in space (no IR bar like the Wii; integrating accel drifts within a second). Gestures must be recognized from rotation + acceleration patterns only.
- Easy: tilt/pour, hold flat, twist, pickaxe hit, shake, upside-down detection, GPS, mic.
- Medium: scoop, over-shoulder throw, hold still (a phone lying on a table has no hand tremor, so it can be detected as cheating), haptic strength.
- Hard: rhythm sync (Bluetooth audio delay of 150–300 ms needs calibration; judge timing locally), compass heading (±10–30° off indoors; use a relative heading with "everyone faces the same wall, tap" instead).

### Edge cases to keep in mind
- **Safety**: phones fly out of hands (no strap), teammates stand close, pickaxe hits can hit real tables. Idea: thumb on screen = holding the shovel; no thumb, no digging. This also cuts false positives. Gestures should peak in deceleration, not in a release motion.
- **Hardware**: some cheap Android phones have no gyro (needs an accelerometer-only fallback); Android/iOS axis units and signs differ; lock screen orientation (newer iPhones have no upside-down portrait, so rotate the canvas instead); iPads mostly have no haptics.
- **Humans**: different grips, left-handed players, very different motion strength → calibration round ("do 3 scoops"). Lazy wrist flicks will be discovered. The screen is not visible mid-gesture, so feedback must be haptics + sound. Palm touches during swings must be ignored.
- **False positives / cheating**: walking, bus or car noise; "shake for everything" → recognize the gesture shape, not just strength; give diminishing returns for the wrong motion; server sanity-checks gesture rates.
- **Compose**: never push raw sensor samples into Compose state (100 recompositions/s); classify in a Flow and emit gesture events only. Register/unregister sensors with the lifecycle (backgrounding, incoming calls).
- **Network**: dirt throws tolerate 50–300 ms latency. Catch timing is judged on the catcher's own clock with a generous window. Disconnects get a bot.

### Prototype tip
Record real gestures with the **phyphox** app (Android + iOS, exports CSV) to see which motions are clearly different before writing any code.

---

## 2. Rowing (working title) — team boat race with phone motion

Shares the motion engine with Dirt War (see "Shared motion engine" below).

### Idea
Players hold the phone with both hands like an oar handle and row: pull back, recover. **Boat speed depends on how in sync the team is**, not on raw strength. Out of sync, the boat wobbles and loses speed.

Steering works like real rowing: players are split into left and right side. If the left side rows harder, the boat turns right, so the team has to talk.

Fits sitting together well: players can sit on the floor in a row like a real boat (living-room regatta).

### Roles
- **Rower**: rows.
- **Cox / drummer** (like a dragon boat): doesn't row, gives the beat by tapping or calling it out. Their beat is the reference for everyone. Their phone plays the beat for the whole team. A desktop player fits this role.
- **Bailer**: the boat leaks, so one player bails water with the pour motion from Dirt War. Fewer rowers means a slower boat.
- **Saboteur**: one player is secretly a traitor who rows off-beat or backwards. The team only sees the boat wobbling and has to find out who it is, while sitting next to them.

### Twists / modes
- **Bodensee race**: teams AT / CH / DE. Fog mode: no picture, only the drummer's sound. Swans attack.
- **Weird boats**: bathtub, Käsknöpfle pot, giant Schneagg shell.
- **Against the current**: row up the Ill or the Rhine; stop rowing and you drift back.
- **Ramming**: two boats side by side ram each other and throw stuff across.
- **Linked to Dirt War**: a team that digs through the Earth floods its hole in the Pacific, then has to row home. Phase 1 dig, phase 2 row.

### Feasibility
- Easier to recognize than digging: one repeating motion, so stroke rate and timing come straight from the sensor curve.
- **Judge sync by timestamps, not by when data arrives.** Sync the phone clocks once at the start (NTP-style offset); each phone timestamps its own strokes and the server compares them. Network latency then doesn't matter.

### Edge cases to keep in mind
- **Fatigue**: holding a phone up gets tiring, so rounds of 1–2 min max.
- **Safety**: pulling back can put an elbow in the face of the teammate behind. The phone flies away less often than with a shovel because it's held with both hands the whole time.
- **Audio delay**: Bluetooth headphones delay the drummer's beat, so calibration is still needed.
- **Lazy rowing**: small wrist flicks instead of real strokes will be discovered, same as in Dirt War.

---

## Shared motion engine
Dirt War and Rowing (and future motion games) can share one base:
- Sensor `expect/actual` layer (Android `SensorManager`, iOS `CMMotionManager`) with normalized axes and units.
- Gesture recognizer that emits gesture events (scoop, twist, hit, stroke, pour, shake, hold still), with a per-player calibration round.
- Thumb-on-screen grip as the "holding the tool" gate.
- Team sync: clock sync between phones plus timestamped motion events.
- Haptics + sound feedback, since the screen isn't visible mid-motion.
