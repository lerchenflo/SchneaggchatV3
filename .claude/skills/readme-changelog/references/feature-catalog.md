Add one to-do each (skip if one for the feature is open); never edit the targets.

a. **Server seed**: grep `~/Desktop/SchneaggchatV3server/src/main/resources/seed/feedback_features.json` (read-only) by `seedKey`/`title`. Add to server `Todo.md` under `## Feedback seed (from the client changelog)` (create above `## GitHub issues`): `- [ ] Seed <new | update "<seedKey>">: <what it does for the user, where to find it; for update: what is outdated> *(client changelog <version>, <date>)*`. `Todo.md` is the only server file you may write.

b. **Feature quiz**: check `games/domain/featurequiz/FeatureQuizQuestions.kt` + `featurequiz_*` strings for answers now wrong. Add at top of `## P4` in `plans/Todo.md`: `- [ ] **S** Feature quiz: <add a question about X | fix <key>: what is wrong> *(client changelog <version>, <date>)*`. If (a) changes a seed `title`, note the question's `seedTitle` must follow.
