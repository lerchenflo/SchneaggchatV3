# Schneagg Rodeo UX Plan

**Status: partly done** (2026-10-22, branch `mapuiredo`, uncommitted, not compiled - laptop). Done: 1, 2, 3, 4, 5, 6, 7 (plane, train, U-boat), 8, 9, 11, 12, 15. Open: 10 (only the lasso part: it moves under the track in portrait), 13, 14, 16, 17.
- 8: the drill keeps "hold = drill down" (flipped, a passive player would drill through to the fossil layer); its key hint adds ↓. S / ↓ is the down input (RodeoVehicleKind.downKey) on plane, rocket, bull, U-boat, balloon, drill, train; L is always the lasso now. No-input rides all say "No input – <goal>".
- 9: candy bus: instead of feedback, a Capture press with nobody in the picture (or during a capture) is kept for the next Stanislaus; range is the visible track (was 75 u). Snail priority: a vehicle / wild horse only wins over a snail in reach when it is closer than the snail.
- 6: the buffered press fires on landing even if already let go (tap-friendly), not only while held. No coyote time.
- 11 (user, 2026-10-22): lasso button bottom right (right thumb, like Rayman Jungle Run's punch button), 56 dp; never over the rider, out of the left thumb's jump area. Portrait with >= 80 dp free under the track: it sits there, off the picture.
- 15: kept, with a warning: the horse gets restless now and then (banner "small jumps!"), only a big jump landed >= 1.2 s into it throws him. Replaces FALL_CHANCE.

Makes Schneagg Rodeo friendlier to play: input (touch + keyboard), the view on portrait phones, and a few unfair moments.
Paths: `R/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/games/presentation/schneaggrodeo/`. Findings verified 2026-10-21 (engine lines spot-checked).

**Order with `plans/GAMES_ARCHITECTURE_PLAN.md`:** that plan's WP-L touches `R/SchneaggRodeoViewModel.kt` and `R/SchneaggRodeoScreen.kt` (wiring only). Run this plan before its Phase 1 or after its Phase 2, never at the same time as WP-L.

Rules: CLAUDE.md applies (theme colors only, new strings en/de/it, never `values-de-rAT`; findings outside scope go to `plans/Todo.md`). Compile `:composeApp:compileKotlinJvm` after each part (desktop: allowed). Device testing is up to the user.

---

## Agents
- **Agent E (engine):** items 1, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17. Owns `R/engine/`, `R/vehicles/`, `R/SchneaggRodeoViewModel.kt`, the vehicle hint strings.
- **Agent U (UI):** items 2, 10, 11, 12, 13, 14. Owns `R/ui/`, `R/SchneaggRodeoScreen.kt`, `R/SchneaggRodeoState.kt` (UI fields only).
- They can run in parallel. Shared: `SchneaggRodeoAction` (U adds new actions, E handles them in the ViewModel; agree on the names first) and the strings files (small, distinct hunks only).

**Recommended set:** 1-7, 10-12, 15. The rest is optional.

---

## Bugs
1. **A held finger stops working on boarding.** `onBoarded` sets `horse.jumpHeld = false` (`R/engine/SchneaggRodeoEngine.kt:701`). Plane, bull, balloon, helicopter, U-boat and drill only take input in RIDING (`RodeoPlane.kt:166`, `RodeoBull.kt:119/123`, `RodeoBalloon.kt:98`, `RodeoHelicopter.kt:110`, `RodeoUBoat.kt:106`, `RodeoDrill.kt:134`), so a finger held through boarding does nothing until it is lifted and pressed again. Rocket and train keep it. Fix: keep raw `jumpDown` / `diveDown` in the engine and call `onJump(held)` again when a vehicle enters RIDING. Effort S.
2. **Steering follows only one finger.** `detectTapGestures` (`R/ui/RodeoInput.kt:54`) tracks one pointer, and the half is chosen at press time. Sliding from the left to the right half does not switch; a second finger is ignored. Fix: `awaitEachGesture` over all pointers, recompute up/down from every pressed pointer on each move. The engine already treats up + down as "sink" (`R/vehicles/RodeoSteering.kt:34-38`). Effort S-M.
3. **Keyboard and touch share one `jumpHeld`.** Releasing one releases the jump while the other is still down. Fix: count held sources (key, pointers) and release only when none is held. Effort S.
4. **Crashes during the map fade.** `crashIntoFences()` runs during `mapSwitch.stepFade` (`SchneaggRodeoEngine.kt:500, :636`), with the screen up to 80% black. Fix: no fence/runner collisions while the fade is active. Effort S.
5. **Super-jump button on the rainbow map.** It stays visible (`R/ui/RodeoControls.kt:146`) but the engine ignores it there (`SchneaggRodeoEngine.kt:441`). Fix: hide it on that map. Effort S.

## Input
6. **Jump buffer (~120 ms) on the horse.** A press in the air is thrown away (`SchneaggRodeoEngine.kt:421`, only `isOnGround` jumps). A full jump takes ~0.84 s, `FENCE_GAP_MIN_SECONDS 0.9` (`R/engine/RodeoDifficulty.kt:49`) leaves ~50 ms on the ground. On foot there is already `PRESS_BUFFER 0.12` (`R/engine/RodeoFall.kt:53`). Fix: store the press time, jump on landing if it is within the buffer and still held. Optional coyote time ~80 ms. Effort S.
7. **Grace at the start of a ride.** The plane takes off at y=20 and sinks with `sinkAccel 45` (`R/vehicles/plane/RodeoPlane.kt:105`): ground after ~0.94 s without a press. Fix: no sink for the first ~1 s; first train bridge and first U-boat mine only after the boarding banner (`RodeoTrain` `BRIDGE_FIRST_SECONDS`, `RodeoUBoat` `thingIn`). Effort S.
8. **Consistent vehicle controls.** Effort M (incl. en/de/it strings).
   - "Hold" means up everywhere; the drill goes down on hold (`RodeoDrill.kt:134`). Either flip it or make its hint say so clearly.
   - Desktop S/↓ descends or ducks in U-boat, drill, balloon; today it sends an ignored lasso press. Plane key hint adds ↑.
   - Tractor, Ford Escort, mine cart (and the other no-input rides) say "Hold on (tight)!" (`values/strings.xml` ~1648/1651/1682), which reads like "hold the screen". Reword to "No controls – enjoy the ride" style.
   - Key hints for milk truck, rowboat, candy bus. Clearer bull hint (`RodeoBull.kt:118-124`).
9. **More forgiving lasso.** Effort S-M.
   - Buffer a press in the last 150 ms of `COOLDOWN 0.52` (`R/engine/RodeoLasso.kt:12-17`); today it is dropped (`SchneaggRodeoEngine.kt:466`).
   - Target order (`SchneaggRodeoEngine.kt:478-482`) puts vehicles and wild horses before snails; prefer a snail in range over a vehicle/horse at the far edge of reach, so a throw does not start an unwanted ride or a horse switch (which drops the riding friend).
   - Candy bus "Capture" eats the press with no target (`RodeoCandyBus.kt:150-158`): show feedback.

## View
10. **Bigger track on portrait phones.** Track height = `min(maxHeight, maxWidth / MIN_TRACK_ASPECT)` with `MIN_TRACK_ASPECT 1.8` (`R/SchneaggRodeoScreen.kt:74`): ~220 dp in the middle of a 720 dp screen, ~200 dp empty above and below. Fix (portrait only): track at the top, controls in the free space below (big lasso button bottom-left, power buttons bottom-right, the rest is a jump area). Landscape stays. Effort M.
11. **Clearer lasso button.** Today alpha 0.35, 32 dp tall, mid-left over the rider (`R/ui/RodeoControls.kt`, `LASSO_BUTTON_*`). Fix: ~56 dp, more opaque, in thumb reach (part of 10 in portrait). Effort S.
12. **Smaller map title.** Covers 85% of the track (`MAP_TITLE_SIZE`, `R/ui/RodeoHud.kt:120`) for `TITLE_SECONDS 2.4` (`R/engine/RodeoMapSwitch.kt:44`) while riding goes on. Fix: top banner like `RodeoAnnouncementBanner`. Effort S.
13. **Hints that come back** (optional). Ride hints fade after `HINT_VISIBLE_MILLIS 3_000` and never return. Fix: longer the first time per vehicle (remember seen vehicles in DataStore); faint ▲/▼ markers on the screen halves while `steers`. Effort M.
14. **Haptics** (optional). None today. Catch, crash, boarding. Effort S.

## Fairness (design decisions)
15. **Random buck-off.** A full-hold jump (peak ≥ `FALL_MIN_JUMP_PEAK 14`) can throw the cowboy without warning, `FALL_CHANCE 0.05` (`R/engine/RodeoFall.kt:22-26`). Fix: ~0.8 s warning (snort / rearing) before it, or remove it. Effort S.
16. **Remount clearance** (optional). No fences are cleared when the cowboy gets back on (`SchneaggRodeoEngine.kt:754-760`); a fence may come right away (not verified). Fix: clear a short zone ahead. Effort S.
17. **Friend joins mid-jump** (optional). The auto-pause releases the jump (`R/SchneaggRodeoViewModel.kt:172-174`). Fix: wait for landing; short countdown on resume. Effort S-M.

Also seen: crash penalty on HIGH late in a run (`FENCE_CRASH_PENALTY 12` × pressure ≤ 2 × level 1.8 ≈ 43 of `CHASE_GAP_MAX 45`): one missed fence is almost a peck. Balance question, not in scope.
