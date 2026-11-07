# Changelog

## 1.0.4 — Nov 7 2026 (first-build fix)
- Android Studio stopped with "connect-client 1.1.0 requires compileSdk 36 and AGP 8.9.1". Updated to **AGP 8.9.1** and **compileSdk 36**, matching Gym & Ñam. minSdk (29) and targetSdk (35) are unchanged, so the same phones can install it and it behaves the same.

## 1.0.3 — Nov 7 2026 (cat images added)
- **39 cat images** added to `res/drawable-nodpi/` (512 × 512, transparent, 1.42 MB total). Every name the app uses is found.
- New `tools/images/prepare_cats.py`: trims, squares (6% margin), shrinks and compresses images. It keeps full quality when 256 colors would change the look (4 images: freeze, empty, leg press, lat pulldown).
- **Dark mode:** the navy outlines disappeared on dark cards. Cats now sit on a light rounded tile in dark mode, in the app and on the share card.
- **Badges:** the badge frame image covered the badge emoji. New `BadgeIcon` draws the emoji inside the frame (Progress screen).
- Checks: static checks pass, deep check 0 failures, 60 mirror tests pass, assets 39/39.

## 1.0.2 — Nov 6 2026 (3 extra check passes + confirmation pass)
- **Pass 1 (automated):** new `tools/deep_check.py` checks call parameters against function definitions, database queries against tables, crash patterns, placeholders, drawables and manifest classes. Result: 0 failures. All 40 empty-list warnings were reviewed and are already guarded.
- **Pass 2 (manual read), fixes:**
  - "Skip it" on a missed program day never hid the card. Programs now remember the skip day (also in backups).
  - The deload suggestion had no way to dismiss it. Added "Not now", which hides it for 7 days.
  - GPS: Stop before the first GPS fix left the background service running with no way to close it.
  - Backup and restore ran on the screen thread and stopped if you left Settings. They now run in the background and show an error if saving fails.
  - Saving progress photos froze the screen for about 1 s. They now save in the background.
  - The routine editor briefly showed the previous routine.
  - Button rows could be cut off on small phones. They now scroll sideways.
- **Pass 3 (copyright + edge cases):**
  - Scanned all 2,598 texts for brands, ©/®/™ symbols and URLs: none found (14 false alarms such as "espinilla" and "spine"). All 304 exercises have their own Spanish text.
  - Rewrote vague how-to text for 9 core exercises (mountain climber, flutter kicks, heel taps, side plank dip, stir the pot, Russian twist, Pallof press ×2, woodchop).
  - Spanish decimal comma ("62,5") was rejected in every number field. Fixed with `Fmt.cleanDecimal`.
  - Rotating the phone re-locked the app. The lock state now lives in the ViewModel.
  - With the lock off, turning it on in Settings locked you out immediately.
  - Deleting a routine during its workout and then choosing "save changes" saved invisible orphan items. It now creates a new routine instead.
- **Confirmation pass:** every changed line was re-read against v1.0.1. Static checks pass, deep check has 0 failures, and 60 mirror tests pass.

## 1.0.1 — Nov 6 2026 (copyright + code review)
- Copyright: renamed "Spin bike / Spin class" to "Indoor cycling" (Spinning® trademark). Notices now cover exercise names, AI images, emoji and Material Icons.
- Fix: "Save changes to routine" ran at the same time as Finish and could save sets you didn't do. Now it runs after Finish.
- Fix: a workout with no ticked sets is no longer saved (it inflated the streak). A message explains why.
- Fix: the app could flash open for a moment before the lock screen while settings loaded.
- Fix: after re-locking, unlocking now returns you to the same screen (for example, your workout) instead of Home.
- Fix: the timer notification restarts if the app was closed mid-workout.
- Fix: in kg mode the − / + buttons snap to 2.5 kg steps (no more 63.7 kg).
- Fix: "Save as routine" decided hold/cardio by distance; it now uses the exercise type.
- Fix: plate calculator menu used an unsafe null check.
- New: Library button on the Routines tab. System Back from "choose exercise" now clears choose mode.
- Mirror tests: 55 (added weight-step snapping).

## 1.0.0 — Nov 6 2026 (built in one session, all features)
- First complete version: every feature from the 4 question rounds.
- Audit:
  - Push-up body-weight shares corrected to Ebben et al. 2011, Table 1: regular 0.64, knees 0.49, hands elevated 12 in 0.55, feet elevated 12 in 0.70.
  - Martial arts/boxing class MET set to 7.8 (boxing sparring).
  - All other MET values double-checked against the 2011 Compendium.
  - Navy formula coefficients double-checked.
  - Health Connect `Metadata` factory signatures checked against the Android docs.
- Not yet compiled; waiting for the first Android Studio build.
