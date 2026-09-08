# Real Weather 365

Fresh native Android weather application built from zero for a cinematic, real-time weather experience.

## Locked visual reference
The master visual specification is the approved two-page `weather.pdf` concept supplied for the project. The app is being rebuilt from scratch; no code or visual system from the old `Live_Weather` project is reused.

## Stage 1 — Sunny / Clear Morning Reference Match

Implemented foundation:
- Kotlin + Jetpack Compose native Android project
- Min Android 8 (API 26)
- Edge-to-edge immersive weather canvas
- Animated clear-day scene foundation: moving cloud, sunlight, mountains, lake shimmer and pine layers
- Chandauli location header
- Large temperature / condition treatment
- Compact hourly glass panel
- AQI / Wind / Humidity / Pressure metrics panel
- Bottom weather navigation matching the reference hierarchy
- MVVM state foundation ready for real weather data
- GitHub Android CI build workflow

## Visual acceptance rule
Stage 1 is not considered visually final until an actual device/emulator screenshot is compared against the approved Sunny/Clear Morning reference. Background realism, typography, spacing, glass opacity and card proportions will be refined before the remaining environments are started.

## Planned environment sequence
1. Sunny / Clear Morning
2. Golden Sunrise
3. Monsoon Rain
4. Thunderstorm
5. Winter Snow
6. Moonlit Night

Only after the six primary live worlds are locked will Radar, Forecast, AQI, Alerts, Cities, Widgets, Animated Backgrounds, Notifications and Settings move into implementation.
