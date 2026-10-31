# Scorenge

Score tracker for group games. Native Android (Kotlin, Jetpack Compose, Material 3), offline-first with Room, Firebase Auth and Firestore backup.

## Modules

- `domain`: pure Kotlin business rules (scoring engine, ranking, round timer). No Android dependencies.
- `app`: the Android app (package `com.bzcode.scorenge`): Compose UI, Hilt, Navigation Compose, Room.

## App structure

- `ui/theme`: colour, type (Plus Jakarta Sans, bundled) and shape tokens. Filled controls with white text use `#C93D18`; the brand `#F7502D` is for headings and accents.
- `ui/navigation`: nav graph and the floating pill bar (Home, All games, + New match, Players, Stats). The bar only shows on those four tabs; match, round and final screens are full screen.
- `data/local`: Room database (schema exported to `app/schemas`), entities and DAOs.
- `di`: Hilt modules.

## Build and test

Requires JDK 17+ and the Android SDK (compileSdk 35).

```
./gradlew :domain:test :app:testDebugUnitTest :app:assembleDebug
```

CI runs the same on every pull request, plus lint, and uploads the debug APK as a build artifact.
Room schema JSON in `app/schemas` must be committed whenever the database changes.

## Licences

Plus Jakarta Sans is under the SIL Open Font License 1.1, see `licenses/PlusJakartaSans-OFL.txt`.
