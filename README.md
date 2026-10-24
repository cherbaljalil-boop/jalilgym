# Jalilgym

An offline-first Android training and nutrition coach. It offers structured programs (PPL, Full Body, custom),
a fast workout logger with auto-progression, and goal-based calorie and macro plans with halal-aware food tagging.

- **Spec:** [Product & Architecture Blueprint](docs/BLUEPRINT.md)
- **Status:** v1.0 is feature-complete (Phases 0–6): training, progress and achievements, nutrition with Open Food Facts and barcode scanning, plus release hardening (signed R8 build in CI, ktlint, migration and accessibility tests, Baseline Profiles, licenses). To publish, follow [docs/RELEASING.md](docs/RELEASING.md); store materials are in [docs/play-store/](docs/play-store/).

## Tech stack
Kotlin · Jetpack Compose (Material 3) · Hilt · Room · DataStore · Navigation Compose (type-safe) ·
Gradle convention plugins + version catalog · minSdk 26 / targetSdk 37

## Project layout
```
app/                  Application, MainActivity, bottom navigation, splash
build-logic/          Gradle convention plugins (jalilgym.android.*, jalilgym.jvm.library, ...)
core/model            Pure Kotlin domain models
core/common           Dispatchers, units
core/domain           Repository interfaces (and, later, use cases & calculators)
core/data             Offline-first repository implementations
core/database         Room schema, DAOs, seed importer (assets/seed: exercises, foods, recipes)
core/datastore        Typed DataStore for user settings
core/network          Open Food Facts client
core/health           Optional Health Connect sync (workouts, weight, meals, steps)
core/designsystem     Theme, fonts, icons, Jg* components
core/ui               Shared composables
core/testing          Fakes and test rules
feature/*             onboarding · today · programs (Train tab) · exercises · workout · nutrition · progress · profile
```

## Building
Requirements: JDK 17+ and the Android SDK (Android Studio's bundled JBR works).

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # unit, Room and Compose (Robolectric) tests
./gradlew lintDebug            # Android lint
java -jar ktlint.jar --format  # style (ktlint 1.8.0, rules in .editorconfig)
./gradlew :app:bundleRelease   # signed release bundle (see docs/RELEASING.md)
```

CI runs the same steps on every push, plus a signed, minified release build. See `.github/workflows/ci.yml`.

## Licenses & attributions
The app's Profile → About → Open-source licenses screen lists everything, with full texts in `feature/profile/src/main/assets/licenses`.
- Exercise data and photos: free-exercise-db (Unlicense)
- Food data: Open Food Facts (ODbL 1.0) and USDA FoodData Central (public domain)
- Fonts: Bebas Neue and Inter (SIL Open Font License 1.1)
- Icons: Material Symbols (Apache License 2.0); libraries: AndroidX, Dagger/Hilt, Kotlin, OkHttp, Coil (Apache License 2.0), ML Kit (ML Kit Terms of Service)

_Created by CH.Djalil_
