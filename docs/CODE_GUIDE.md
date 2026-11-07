# Gym Cat — Code Guide (for a non-coder)

This guide explains how the app is put together, where everything lives, and how to change common things. Every Kotlin file also starts with a **PLAIN-ENGLISH GUIDE** box.

---

## 1. The layers (top = what you see, bottom = where data lives)

```
┌───────────────────────────────────────────────┐
│  SCREENS  (ui/screens/*.kt)                    │  what you tap
│  Home · Routines · Workout · Progress · Body … │
├───────────────────────────────────────────────┤
│  VIEWMODEL  (ui/MainViewModel.kt)              │  live lists + derived numbers
│  streak, records, muscle sets, deload …        │  (survives screen rotation)
├───────────────────────────────────────────────┤
│  REPOSITORY  (data/Repository.kt)              │  "the manager": start/finish
│  + Health Connect, Backup, Share codes         │  workouts, calories, badges …
├───────────────────────────────────────────────┤
│  CORE  (core/…)  ← pure Kotlin, no Android     │  ALL formulas + programs
│  StrengthMath · PlateMath · CalorieMath …      │  (ready for iOS / KMP later)
├───────────────────────────────────────────────┤
│  DATABASE  (data/db/*.kt, Room)  + Settings    │  tables on the phone
└───────────────────────────────────────────────┘
   SERVICE (service/*.kt): timer notification, rest beeps, GPS, reminders, auto-backup
   WIDGETS (widget/Widgets.kt): home-screen streak + next workout
```

## 2. Folder map

| Folder / file | What's inside |
|---|---|
| `app/build.gradle.kts` | Library list and Android versions (same as Gym & Ñam) |
| `app/src/main/AndroidManifest.xml` | Permissions (no internet!), screens, service, widgets |
| `app/src/main/assets/exercises.json` | 304 built-in exercises (generated — don't edit by hand) |
| `app/src/main/res/values/strings.xml` | English text (generated) |
| `app/src/main/res/values-es/strings.xml` | Spanish text (generated) |
| `app/src/main/res/drawable/` | App icon (vector, option A) |
| `app/src/main/res/drawable-nodpi/` | **Put your cat PNGs here** (see `ASSETS_GUIDE.md`) |
| `app/src/main/res/raw/` | **Put your meow sounds here** (create the folder) |
| `java/com/gymcat/app/GymCatApp.kt` | Builds the shared tools once (manual dependency injection) |
| `java/com/gymcat/app/MainActivity.kt` | Theme, lock screen, navigation map, bottom bar |
| `core/model/Models.kt` | Lists of choices (muscles, equipment…) and the Exercise shape |
| `core/calc/StrengthMath.kt` | 1RM (Epley, Brzycki), bodyweight load, volume, RPE suggestion, warm-ups, deload |
| `core/calc/PlateMath.kt` | Plate calculator |
| `core/calc/CalorieMath.kt` | MET calories, set + rest split |
| `core/calc/StreakMath.kt` | Weekly streak with earned freezes |
| `core/calc/BodyMath.kt` | Navy body fat; lb↔kg, mi↔km, in↔cm |
| `core/calc/GeoMath.kt` | GPS distance, pace, mile splits, route drawing |
| `core/calc/Badges.kt` | Badge list and rules |
| `core/programs/Programs.kt` | 4 built-in programs + 7 skill ladders |
| `core/csv/Csv.kt` | CSV read/write + date conversion |
| `data/db/` | Tables (`Entities.kt`), questions (`GymDao.kt`), database file (`GymDatabase.kt`) |
| `data/Repository.kt` | Start/finish workouts, records, badges, ladders, cardio, classes |
| `data/SettingsStore.kt` | All settings with their defaults |
| `data/HealthConnectManager.kt` | Write calories; read weight + protein |
| `data/BackupManager.kt` | Backup zip (CSV + photos), restore, weekly auto-backup |
| `data/ShareCodec.kt` | Routine ⇄ share code (file or QR) |
| `data/ExerciseLibrary.kt` | Loads exercises.json + your own exercises |
| `service/WorkoutService.kt` | Notification, rest-timer beeps, GPS tracking |
| `service/Feedback.kt` | Beeps, vibration, voice, meows |
| `service/Workers.kt` | Daily reminders, weekly auto-backup |
| `ui/theme/Theme.kt` | The 4 palettes, dark mode, 28 dp cards |
| `ui/Format.kt` | How numbers are shown (lb/kg, mi/km, 1:23, pace) |
| `ui/components/` | Cards, steppers, charts, cat pictures, muscle map |
| `ui/screens/` | One file per screen (Home, Workout, Finish, Routines, Library, Cardio, Stretch, Progress, Body, Settings, Onboarding, QR, Lock) |
| `widget/Widgets.kt` | Two home-screen widgets |
| `tools/exercises/build_exercises.py` | Writes exercises.json |
| `tools/strings/build_strings.py` | Writes both strings.xml files |
| `tools/images/prepare_cats.py` | Trims, squares, shrinks and compresses cat PNGs into `drawable-nodpi/` |
| `tools/tests/mirror_tests.py` | Checks every formula against its worked example |
| `tools/check_project.py` | Static checks (strings, imports, brackets, ids) |
| `tools/deep_check.py` | Deeper checks (parameters, SQL vs tables, crash patterns, placeholders, manifest) |

## 3. Kotlin cheat sheet

| You see | It means |
|---|---|
| `val x = 5` | a value that never changes |
| `var x = 5` | a value that can change |
| `fun add(a: Int): Int` | a function named add that takes a number and gives a number |
| `Double?` | a decimal number **or nothing** (`null`) |
| `x ?: 0.0` | "use x, or 0.0 if x is nothing" |
| `x?.let { … }` | "only if x isn't nothing, do …" |
| `data class` | a bundle of named values (one row) |
| `enum class` | a fixed list of options |
| `object` | a single shared helper |
| `listOf(a, b)` / `mapOf(a to b)` | a list / a lookup table |
| `when (x) { 1 -> … else -> … }` | like a multiple-choice "if" |
| `@Composable fun` | draws part of a screen |
| `remember { mutableStateOf(…) }` | a value a screen remembers between redraws |
| `collectAsState()` | "redraw when this live list changes" |
| `suspend fun` / `launch { }` | runs in the background so the screen doesn't freeze |
| `Flow` / `StateFlow` | a live value that updates by itself |
| `R.string.xxx` | text from strings.xml (English or Spanish automatically) |

## 4. "How do I…?"

| I want to… | Do this |
|---|---|
| Change a word or translation | Edit `tools/strings/build_strings.py`, run `python3 tools/strings/build_strings.py` |
| Add or fix a built-in exercise | Edit `tools/exercises/build_exercises.py`, run it, then `python3 tools/tests/mirror_tests.py` |
| Add or replace a cat drawing | Name it (e.g. `cat_19_bench.png`), run `python3 tools/images/prepare_cats.py <folder>` (see `docs/ASSETS_GUIDE.md`) |
| Add meows | Put `meow_1.ogg` … `meow_9.ogg` in `app/src/main/res/raw/` |
| Change default rest times | `data/SettingsStore.kt` → `restCompound`, `restIsolation`, `restCalisthenics` (or in the app's Settings) |
| Change a palette color | `ui/theme/Theme.kt` → `Palettes` |
| Change the RPE suggestion | `core/calc/StrengthMath.kt` → `suggestNext` (then update the mirror test) |
| Change warm-up percentages | `StrengthMath.warmups` (50/70/85 %) |
| Change when a deload is suggested | `StrengthMath.shouldDeload` (6 weeks or 2 stalled lifts) |
| Change badge thresholds | `core/calc/Badges.kt` |
| Change a program | `core/programs/Programs.kt` |
| Change a ladder target | `Programs.ladders` (sets × reps or seconds per step) |
| Change how many freezes you can save | `StreakMath.MAX_FREEZES` |
| Check everything after edits | `python3 tools/check_project.py && python3 tools/deep_check.py && python3 tools/tests/mirror_tests.py` |

## 5. Where each formula lives

| Formula | File → function | Worked example |
|---|---|---|
| Epley 1RM = w × (1 + r/30) | `StrengthMath.epley` | 185 × 5 → 215.8 lb |
| Brzycki 1RM = w × 36/(37 − r) | `StrengthMath.brzycki` | 185 × 5 → 208.1 lb |
| Bodyweight load = BW × pct + added − assist | `StrengthMath.effectiveLoad` | 180 lb pull-up +25 → 205 lb |
| Volume = load × reps | `StrengthMath.volume` | 3 × 135 × 10 → 4,050 lb |
| RPE suggestion: e1RM = w(1 + (r + RIR)/30), next = e1RM / (1 + (r* + RIR*)/30) | `StrengthMath.suggestNext` | 185×5 @7 → 190 lb for 5 @8 |
| Warm-ups 50 %×8, 70 %×5, 85 %×3 | `StrengthMath.warmups` | 225 → 115, 160, 190 |
| Deload = 90 % weight, ½ sets | `StrengthMath.deloadWeight/Sets` | 4×5 @200 → 2×5 @180 |
| Plates per side = (target − bar) ÷ 2 | `PlateMath.perSide` | 190 lb → 45 + 25 + 2.5 |
| kcal = MET × kg × hours | `CalorieMath.met` | run 30 min @180 lb → 400 kcal |
| Lifting kcal = (5.0 × active h + 1.5 × rest h) × kg | `CalorieMath.strength` | 60 min, 20×10 → 170 kcal |
| Weekly streak + freezes | `StreakMath.compute` | miss 1 week with a freeze → streak lives |
| Navy body fat (men / women) | `BodyMath.navyBodyFat` | man 70/15/34 in → 17.5 % |
| Haversine distance | `GeoMath.haversineMi` | 0.01° latitude ≈ 0.691 mi |
| Pace = time ÷ distance | `GeoMath.paceMinPerMile` | 30 min / 3.1 mi → 9:41 /mi |
| Weekly muscle sets (primary +1, secondary +0.5) | `MainViewModel.muscleSetsThisWeek` | 3 bench sets → chest 3, triceps 1.5 |

## 6. Data the app shares with Gym & Ñam

- **Health Connect** (only if you turn it on): Gym Cat **writes** `ActiveCaloriesBurnedRecord` per workout (manual entry; GPS runs are "actively recorded" by the phone), and **reads** `WeightRecord` and today's `NutritionRecord` protein. It deletes only records it wrote (it saves their ids).
- **Backups**: `workouts.csv` starts with Gym & Ñam's `exercise.csv` columns `id,date,name,emoji,minutes,kcal,source,created` (source = `gymcat`), then Gym Cat's extra columns. Also `sets.csv`, `exercises.csv`, `exercise_prefs.csv`, `routines.csv`, `routine_items.csv`, `body_measurements.csv`, `photos.csv` + `photos/`, `programs.csv`, `badges.csv`, `ladders.csv`. UTF-8 + BOM, `\r\n`, dates `YYYY-MM-DD`.
- Storage units are always **lb / miles / inches**; the kg/km switch only changes the display.
