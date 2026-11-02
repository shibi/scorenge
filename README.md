# Scorenge

Score tracker for group games. Native Android (Kotlin, Jetpack Compose, Material 3), offline-first with Room, Firebase Auth and Firestore backup.

## Modules

- `domain`: pure Kotlin business rules (scoring engine, ranking, round timer). No Android dependencies.
- `app`: the Android app (package `com.bzcode.scorenge`): Compose UI, Hilt, Navigation Compose, Room.

## App structure

- `ui/theme`: colour, type (Plus Jakarta Sans, bundled) and shape tokens. Filled controls with white text use `#C93D18`; the brand `#F7502D` is for headings and accents.
- `ui/navigation`: nav graph and the floating pill bar (Home, All games, + New match, Players, Stats). The bar only shows on those four tabs; match, round and final screens are full screen.
- `data/auth`: Firebase Auth repository, Google ID token via Credential Manager, local users row.
- `data/local`: Room database (schema exported to `app/schemas`), entities and DAOs.
- `di`: Hilt modules.

## Firebase setup

Sign-in uses Firebase Auth with Google (Credential Manager). Two files are needed and are not in git:

- `app/google-services.json`: from the Firebase console (project settings, Android app `com.bzcode.scorenge`). Google must be enabled under Authentication, sign-in method, so the file contains a Web OAuth client (`client_type` 3).
- `app/debug.keystore` (optional): the project's shared debug key, so debug builds match the SHA-1 registered in Firebase. Without it, add your machine's debug SHA-1 (`./gradlew signingReport`) in the Firebase console.

CI writes both from the repository secrets `GOOGLE_SERVICES_JSON` (the file's contents) and `DEBUG_KEYSTORE_BASE64` (the keystore, base64). Without them it builds with `ci/google-services.placeholder.json`, and sign-in won't work in that APK.

## Build and test

Requires JDK 17+ and the Android SDK (compileSdk 35).

```
./gradlew :domain:test :app:testDebugUnitTest :app:assembleDebug
```

CI runs the same on every pull request, plus lint, and uploads the debug APK as a build artifact.
Room schema JSON in `app/schemas` must be committed whenever the database changes.

## Licences

Plus Jakarta Sans and Fredoka are under the SIL Open Font License 1.1, see `licenses/PlusJakartaSans-OFL.txt` and `licenses/Fredoka-OFL.txt`.
