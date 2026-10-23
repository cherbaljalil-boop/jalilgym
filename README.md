# Jalilgym

An offline-first Android training and nutrition coach. It offers structured programs (PPL, Full Body, custom),
a fast workout logger with auto-progression, and goal-based calorie and macro plans with halal-aware food tagging.

- **Spec:** [Product & Architecture Blueprint](docs/BLUEPRINT.md)
- **Status:** Phases 0–3 are complete: foundation, onboarding, the exercise library, programs and workout logging. CI is green. Phase 4 (progress and gamification) is next.

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
core/database         Room schema v1, DAOs, seed importer (assets/seed)
core/datastore        Typed DataStore for user settings
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
```

CI runs the same steps on every push. See `.github/workflows/ci.yml`.

## Licenses & attributions
- Fonts: Bebas Neue and Inter, under the SIL Open Font License 1.1 (`core/designsystem/src/main/assets/licenses`)
- Icons: Material Symbols, under the Apache License 2.0

_Created by CH.Djalil_
