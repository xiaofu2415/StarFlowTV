# StarFlowTV v1.4.0 Release Notes

## Official live category

- Adds the official live category, with 21 CCTV official pages available from the catalog.
- Official entries prioritize their supported pages while preserving the familiar live-TV navigation flow.

## Playback and source behavior

- All 21 entries in this release use official WEB pages. A failed page is retried once;
  if that retry fails, playback is exhausted and the viewer is notified. DIRECT
  playback is a reserved extension point, not a shipped fallback source.
- WebView fallback is limited by the page itself: login, regional availability, DRM, unsupported browser features, or a provider-side change can prevent playback.
- Adds manual source refresh so viewers can request the latest source catalog without waiting for the next automatic refresh.
- Failed/offline configuration loading retains the built-in official category; remote
  IPTV groups cannot claim official status merely by using the same category name.
- Remembers official channels by stable ID, including when an IPTV channel has the same name.
- Refresh results received in the background wait until the player returns to the foreground.

## Remote-control reliability

- Fixes repeated handling of D-pad up and down key-up events, preventing an extra navigation movement after a key press.
- Left/right on a single-source official channel shows a no-alternate-source notice.

## Release safety

- This document contains no private source addresses, tokens, credentials, or signing material.
