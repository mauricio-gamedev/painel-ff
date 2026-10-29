# Changelog

## 2.2 — External manual assistant

- Rebuilt the floating overlay as a scrollable external assistant with interactive sliders for all sensitivity fields.
- Added persistence for user-adjusted values, synchronised preset controls, profile copy, and launch shortcuts for Free Fire and Free Fire MAX.
- Kept the panel independent of protected game files and documented manual application of values.

## 2.1.1 — Wallpaper integration

- Added an actual remote anime character wallpaper as a full-screen app backdrop, with dark overlays to keep text readable.
- Added the wallpaper source credit to the app and README.
- Added Android INTERNET permission only to load the remote background; profile data stays local.


## 2.1 — Yoru Anime Dark + Floating Panel

- Reworked the interface with an anime-dark palette, sakura neon accents and mobile-friendly layout.
- Added Android overlay permission flow and a draggable floating quick panel that can be minimized or stopped.
- Added floating profile shortcuts and local clipboard export; values are presented for manual application.
- Added a conservative calibration assistant for heavy-feeling drag and overshooting, with optional local application of suggested profile values.
- Added a touch-path test canvas that measures gestures inside the app only.
- Added Android runtime status for available RAM, battery, thermal state and free internal storage when supported.
- Added shortcuts to official Android battery, display, notifications and installed game app settings.
- Updated app version and web-app cache identifier to 2.1.
- The overlay does not intercept game input, synthesize touches, modify game files, read game memory or claim to stabilize aim inside gameplay.
