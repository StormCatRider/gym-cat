# Gym Cat 🐱🏋️ — v1.0.5

Android workout tracker and companion to **Gym & Ñam**. Kotlin + Jetpack Compose, with the same tech stack and versions as Gym & Ñam. English and Spanish (it follows the phone's language).

## Open and build
1. Unzip, then in Android Studio choose **File → Open** and select the `GymCat` folder.
2. If Android Studio asks to create the Gradle wrapper, accept (Gradle 8.11.1). The zip doesn't include `gradle-wrapper.jar` because it couldn't be downloaded here.
3. Wait for sync, then **Run ▶** on a phone with Android 10 or newer.
4. If it shows errors, copy the first red error lines and send them to me.

> ⚠️ **Not compiled yet.** Claude's workspace can't reach Gradle/Maven. It passed static checks (`tools/check_project.py`) and 60 formula mirror tests (`tools/tests/mirror_tests.py`), but the first real build is in your Android Studio.

## What's inside
- **Workouts:** free logging plus routines and programs; big set cards with − / +; prefill from last time; RPE suggestion; set types; RPE; notes; supersets; swap, skip and reorder exercises; automatic and manual rest timer; plate calculator (lb, or kg in Settings); Epley and Brzycki 1RM; automatic warm-ups; deload suggestion; missed-day prompt.
- **Library:** 304 exercises (176 strength, 72 calisthenics, 18 cardio, 30 stretches), each with muscles, equipment, an original how-to and common mistakes. You can add your own or copy and edit a built-in one. Favorites, search and filters.
- **Programs:** beginner full body, dumbbell full body (home), calisthenics ladders, and cardio + stretch. Plus 7 skill ladders (push-up, pull-up, squat → pistol, core, handstand, dip, muscle-up) where the app suggests the next step and you confirm.
- **Cardio:** manual entry, timer, interval timer with beeps and voice cues, and GPS runs (offline route line and mile splits). Also guided stretching, and classes and sports.
- **Progress:** calendar, workouts per week, per-exercise 1RM chart, sets per muscle, weekly volume, body weight from Health Connect, records, 30 badges, and the full history.
- **Body:** front and back muscle map, measurements with Navy body-fat %, and progress photos kept private in the app with side-by-side compare.
- **Gamification:** weekly streak with earned freezes (up to 2), badges, and a share image card with the cat.
- **Sharing:** routine as a `.gymcat` file or a QR code, with no accounts and no servers.
- **Settings:** 4 palettes, dark mode, metric display, profile, rest defaults, sounds, vibration, meows, voice cues, Health Connect, PIN or fingerprint lock, reminders, backup and restore, and weekly auto-backup.
- **Widgets:** small (streak) and medium (next workout with a Start button).

## Privacy
No internet permission, no accounts, no analytics, no ads, and `allowBackup=false`. Health Connect, the lock, reminders and auto-backup are all **off** until you turn them on. Location is asked for only when you start a GPS run, and the camera only for QR scanning and photos.

## Docs
- `docs/CODE_GUIDE.md`: layers, folder map, Kotlin cheat sheet, a "how do I…" table, and where each formula lives.
- `docs/ASSETS_GUIDE.md`: exact file names for your cat images and meows.
- `THIRD_PARTY_NOTICES.md`: sources and licenses.
- `CHANGELOG.md`.
