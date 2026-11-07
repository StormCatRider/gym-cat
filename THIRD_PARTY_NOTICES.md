# Third-party notices — Gym Cat 1.0.3

## Content
- **Exercise names, how-to text, common mistakes, programs, ladders, badges:** written originally for Gym Cat (English and Spanish). Generic exercise names, like "bench press", are common terms. Programs are generic templates and don't copy branded plans.
- **Images:** the app icon (cat on a weight plate) and the muscle map are original vector drawings. The 39 cat illustrations in `res/drawable-nodpi/` were made by the owner with ChatGPT from original prompts ("Gym Cat" original character), checked by eye for resemblance to existing characters, logos and text. No photos, videos or third-party artwork are bundled.
- **Sounds:** beeps come from Android's built-in `ToneGenerator`. Meows are owner-supplied. No bundled audio.
- **Fonts:** system fonts only.

## Reference data (facts, cited)
- MET values: Ainsworth BE et al., *2011 Compendium of Physical Activities* (same edition Gym & Ñam uses). Values not in the Compendium are marked as estimates in `tools/exercises/build_exercises.py`.
- Push-up share of body weight: Ebben WP et al. (2011), "Kinetic analysis of several variations of push-ups", ISBS Conference Proceedings.
- U.S. Navy circumference body-fat equations: Hodgdon JA & Beckett MB (1984), Naval Health Research Center; DoD Instruction 1308.3.
- 1RM equations: Epley (1985); Brzycki (1993).

## Names and trademarks
- **Exercise names** are standard gym terms, including names that honor people, such as Arnold press, Pendlay row, Zottman curl, Zercher squat, JM press, Pallof press and Copenhagen plank. Names of exercises aren't copyrightable, and none of these is a product brand.
- **Renamed to avoid trademarks:** "Spin/Spinning" is a registered mark of Mad Dogg Athletics, so the app says "indoor cycling". "Air bike" is used instead of brand names. There are no other gym-equipment brands (Smith machine is a generic term).
- "Health Connect" is Google's. It's named only to describe the feature.
- **App name:** before a public release, search the name "Gym Cat" in the USPTO trademark database and on Google Play.

## AI-generated cat images
The cat drawings come from ChatGPT prompts that describe an original character. Under OpenAI's terms the outputs belong to you. Purely AI-made images may not get copyright protection, so others could copy them; that doesn't put you at risk. Before adding each image, check that it doesn't look like an existing character (Garfield, Pusheen, Hello Kitty, Grumpy Cat and so on) and has no logos or text.

## Libraries (Apache License 2.0)
AndroidX (Core, Activity, Lifecycle, Fragment, Compose, Material 3, Material Icons Extended, Navigation, Room, DataStore, CameraX, Glance, WorkManager, Biometric, Health Connect client), Kotlin and kotlinx.coroutines (JetBrains), and ZXing core 3.5.3 (QR code create/read).

## Emoji
Emoji are drawn by the phone's own system font (on Pixel, Noto Color Emoji, OFL 1.1). None are bundled. The share-card image draws emoji with that same system font.

## Network
Gym Cat declares **no INTERNET permission**, so it can't send or receive anything online.
