# Gym Cat — Adding your cat images and meows

The app already works without them: every spot shows an emoji until the matching file exists. Once you add a file with the **exact name**, it appears automatically. No code changes are needed.

## 1. Cat images (from your ChatGPT prompts)

**Where:** `app/src/main/res/drawable-nodpi/` (create the folder if it's missing).
**Format:** PNG with a transparent background, 1024 × 1024 is fine.
**Names:** lowercase letters, numbers and `_` only (Android rule). Use these exact names:

| Prompt # | File name | Where it shows |
|---|---|---|
| 4 | `cat_04_welcome.png` | Onboarding first page |
| 5 | `cat_05_thinking.png` | Onboarding questions, lock screen |
| 6 | `cat_06_home.png` | Home header, medium widget |
| 7 | `cat_07_pr.png` | Finish screen when you set a record |
| 8 | `cat_08_streak.png` | Home "This week", small widget |
| 9 | `cat_09_freeze.png` | Home when a freeze saved last week |
| 10 | `cat_10_streak_lost.png` | Home when there's no streak |
| 11 | `cat_11_rest.png` | Rest timer banner |
| 12 | `cat_12_rest_day.png` | Deload suggestion |
| 13 | `cat_13_badge.png` | New badge, latest win |
| 14 | `cat_14_empty.png` | Empty screens |
| 15 | `cat_15_finished.png` | Finish screen, share card, stretch done |
| 16 | `cat_16_badge_frame.png` | Behind every badge in Progress |
| 17 | `cat_17_squat.png` | Back squat |
| 18 | `cat_18_deadlift.png` | Deadlift |
| 19 | `cat_19_bench.png` | Bench press |
| 20 | `cat_20_ohp.png` | Overhead press |
| 21 | `cat_21_row.png` | Bent-over row |
| 22 | `cat_22_rdl.png` | Romanian deadlift |
| 23 | `cat_23_hip_thrust.png` | Hip thrust |
| 24 | `cat_24_leg_press.png` | Leg press |
| 25 | `cat_25_lat_pulldown.png` | Lat pulldown |
| 26 | `cat_26_curl.png` | Dumbbell curl |
| 27 | `cat_27_pushdown.png` | Triceps pushdown |
| 28 | `cat_28_lateral_raise.png` | Lateral raise |
| 29 | `cat_29_lunge.png` | Walking lunge |
| 30 | `cat_30_pushup.png` | Push-up |
| 31 | `cat_31_pullup.png` | Pull-up |
| 32 | `cat_32_dip.png` | Parallel bar dip |
| 33 | `cat_33_plank.png` | Plank |
| 34 | `cat_34_crunch.png` | Crunch |
| 35 | `cat_35_pistol.png` | Pistol squat |
| 36 | `cat_36_handstand.png` | Wall handstand |
| 37 | `cat_37_treadmill.png` | Treadmill run |
| 38 | `cat_38_bike.png` | Stationary bike |
| 39 | `cat_39_rower.png` | Rowing machine |
| 40 | `cat_40_jump_rope.png` | Jump rope |
| 41 | `cat_41_hamstring.png` | Standing forward fold |
| 42 | `cat_42_down_dog.png` | Downward dog |

Prompts 1–3 (reference sheet, icon, monochrome icon) aren't loaded by the app. The app icon is already drawn as a vector (option A). If you want your ChatGPT icon instead, send it and I'll convert it.

**Status (v1.0.3):** all 39 images are in the app (1.42 MB in total).

**Adding or replacing an image (easy way)**
1. Put the new PNGs in any folder, already named like `cat_19_bench.png`.
2. Run `python3 tools/images/prepare_cats.py <that folder>`.
   It trims the empty border, centers the cat on a square with a 6% margin, shrinks it to 512 × 512,
   and saves a small 256-color version when it looks identical (otherwise full quality).
3. Run `python3 tools/check_assets.py` to confirm all names are found.

**Dark mode:** the cats have dark navy outlines, so in dark mode the app shows each cat on a light
rounded tile (`CatArt` in `ui/components/Components.kt`; the share card does the same).
Badges draw their emoji on top of `cat_16_badge_frame` (`BadgeIcon`).

## 2. Meows

**Where:** `app/src/main/res/raw/` (create the folder).
**Names:** `meow_1.ogg`, `meow_2.ogg` … up to `meow_9.ogg`. `.mp3` and `.wav` also work, but names must stay lowercase.
**Length:** short (under 2 s).
**When they play** (if Settings → Meows is on): randomly after about 1 in 7 finished sets, and on new records and badges.
**Copyright:** use your own recordings (your cat!) or sounds you made. Don't use downloaded sound effects unless their license allows it.
