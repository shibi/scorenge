# Scorenge

Score tracker for group games. Native Android (Kotlin, Jetpack Compose, Material 3), offline-first with Room, Firebase Auth and Firestore backup.

## Modules

- `domain`: pure Kotlin business rules (scoring engine, ranking, round timer). No Android dependencies.

## Run the tests

```
./gradlew :domain:test
```
