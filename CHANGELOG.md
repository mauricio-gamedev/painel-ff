# Changelog

## 2.0 — Android integration baseline

- Added native detection for the known Free Fire and Free Fire MAX package IDs.
- Added launch buttons that use the Android package launch intent; this does not access the game process.
- Added runtime device model, Android version, and approximate total RAM reporting.
- Added Android document picker support for profile JSON import and export.
- Added persistent sensitivity profiles and calibration/target practice.
- Added local performance preparation checklist.
- Added CI validation for inline JavaScript syntax, duplicate HTML IDs, and Android XML resources before Gradle build.
- Direct internal game modification remains out of scope because no supported game settings API is exposed to this app.
