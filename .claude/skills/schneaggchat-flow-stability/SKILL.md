---
name: schneaggchat-flow-stability
description: Schneaggchat add-on for the global flow-stability skill — real class names, locks, ready-made hop maps and known gaps with file:line anchors across the KMP client (SchneaggchatV3) and the Spring Boot server (SchneaggchatV3server). Use it together with flow-stability whenever a Schneaggchat flow is checked for stability or traced, or when asked what happens under a bad network, a dropped socket, a backgrounded app, two users acting at once, or an offline-to-online transition. Trigger on phrases like "is this stable", "will this break if...", "race condition", "trace the flow", "stuck on loading/spinner", "messages not arriving", "duplicate message", "hangs", "sync issue", "out of sync", or any bug report shaped like "sometimes X doesn't happen". For endpoint auth/authorization only, prefer spring-endpoint-security-check instead.
---

# Schneaggchat flow stability (project add-on)

Run the global **`flow-stability`** skill for the method, the five lenses, the severity rubric and
the output format. This skill only adds what is specific to Schneaggchat. The client is this repo
(SchneaggchatV3); the server is the sibling repo `../SchneaggchatV3server` — trace across both.

## References

| Topic | File |
|---|---|
| Real classes, locks, reference shapes and known gaps per lens, with `file:line` anchors — read before tracing any hop | `references/known-gaps.md` |
| Ready-made hop maps for the five most common flows (send message, data sync, login/token refresh, socket reconnect, push fallback) — check here before re-deriving a flow | `references/known-flows.md` |

## The hop skeleton with Schneaggchat names

```
Compose UI (button / gesture)
  → ViewModel Action handler
  → AppRepository method (often behind a Mutex - see lens 2)
  → NetworkUtils.safeCall → Ktor HttpClient (authenticated, TokenManager-backed)
  → Controller.requireAuth() → Service → Mongo repository
  → NotificationService.notifyX() → SocketConnectionHandler.sendMessage()
      → connected? push via WebSocket : fall back to FirebaseService + ApnsService
  → client SocketConnectionManager receives frame → handleSocketConnectionMessage()
  → Room DAO upsert/delete → Flow emission → Compose recomposition
```

Server-initiated flows (push-only, cron, birthday notifications) start partway down this chain.
Pure sync flows (`AppRepository.dataSync`) skip the socket/push section and loop a `/x/sync`
endpoint until `moreEntries` is exhausted. Map, events and poll votes branch differently.

## Related skills

- **`schneaggchatv3server`** (in the server repo) — architecture map: packages, routes, persistence.
- **`spring-endpoint-security-check`** — auth/authorization (can user A touch user B's data).
