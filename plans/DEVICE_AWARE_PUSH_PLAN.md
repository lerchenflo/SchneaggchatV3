# Device-aware push fallback (plan, not decided)

Status: plan only, decide later (user, 2026-10-21). Needs server changes, which are not wanted right now.

## Problem

A new message is written to every socket session of the recipient, and the push is held in
`PendingPushRegistry` until one session acks it (`SchneaggchatV3server` `notifications/websocket/SocketConnectionHandler.kt:150-177`).
One ack from **any** session cancels the push for **all** of the user's devices, because push
tokens (`FirebaseToken`, APNs token) only know the user, not the device.

So:
- Desktop app open (it holds its socket while minimized) -> it acks -> the phone never buzzes, even
  when the user is away from the computer.
- Two phones, one in the foreground -> the other phone gets nothing.

## Goal

A device that acks only cancels **its own** push. Every other device of the user still gets its
push (or its own socket delivery).

## Idea

The device identity already exists on the server: one `RefreshToken` row per logged-in device
(`authentication/model/RefreshToken.kt`, deduped on userId + deviceName + deviceType).

1. **Server: device id in the access token.** Add the `RefreshToken` row id as a claim (e.g. `sid`)
   when issuing access tokens (`core/security/JwtService.kt:48`, login and refresh in `AuthService`).
2. **Server: token per device.** `/setnotificationtoken` stores the `sid` of the calling access
   token on `FirebaseToken` / the APNs token row. Old rows without it stay "device unknown".
3. **Server: socket per device.** On connect, read `sid` from the socket's access token and keep it
   on `SocketConnection`.
4. **Server: push per device.** `sendNewMessage` no longer holds one push per user. For each push
   token of the user:
   - token's device has an acking socket session -> hold this token's push until that device acks
     (current 3 s / session-closed rules, now per device);
   - otherwise -> push to this token right away.
   Tokens without a device id keep today's behaviour (held until any ack), so old app versions
   don't get double notifications.
5. **Client:** nothing new, as long as it re-uploads its token after login (it does, in
   `ChatSelectorViewModel` init), so the token gets its `sid`. Logout already deletes the token.

## Open questions

- Should a desktop ack cancel the phone push at all? Telegram, for example, keeps the phone silent
  while the desktop is active. Options: always push to the other devices, or push to them only if
  the acking device is a desktop that has been idle for some minutes.
- A message read on the desktop leaves the phone notification standing. A "read elsewhere" cancel
  (silent push or socket event that removes the chat's notification) would pair well with this.

## Size

M/L, mostly server (JWT claim, token model + migration, `SocketConnection`, `PendingPushRegistry`
keyed by device). Client side no change or a small one.
