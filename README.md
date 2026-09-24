# SwimmingManager

SwimmingManager is an Android management and simulation game built around competitive swimming. Take control of a club, build and train its roster, plan entries for the season, and guide your swimmers through a simulated world of meets, rankings, records, medals, and national selections.

The project is currently developed as a native Android application. It uses Kotlin and Jetpack Compose for the user interface, with a local Room database powering the game world and its long-term progression.

## Features

### Club management

- Select a club from a searchable list of generated clubs.
- Manage the club's active roster and inspect individual swimmer profiles.
- Configure training regime and stroke focus for the entire team.
- Recruit new generations of swimmers as the season progresses.
- Release swimmers from the club when rebuilding the roster.

### Season and competition management

- Play through a week-based season with a generated annual competition calendar.
- Enter swimmers in individual events when they meet the applicable qualification standards.
- Build and save relay line-ups for freestyle and medley relays.
- Receive national call-ups for major international competitions.
- Advance the timeline and simulate competitions involving the player's club and the wider swimming world.

### Race simulation and progression

- Follow live meet simulations round by round, including finals and relay events.
- Simulate race times using swimmer attributes, event characteristics, training, fatigue, and progression.
- Track personal bests, season bests, meet results, medals, and relay results.
- Apply end-of-season development, decline, retirement, and recruitment systems.

### Performance overview

- Browse national and world rankings.
- Review world, national, regional, and meet records.
- Inspect qualification time standards for long-course and short-course events.
- Read in-game messages about roster changes, selections, and season events.

## Technology stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 |
| Architecture | Android `ViewModel` with coroutine-based state updates |
| Persistence | Android Room |
| Build system | Gradle 9.1.0 with Kotlin DSL |
| Android configuration | Compile/target SDK 36, minimum SDK 28 |
| Java compatibility | Java 11 |

## Requirements

- Android Studio with Android SDK 36 support
- JDK 11
- An Android device or emulator running Android 9 (API 28) or newer

The Gradle wrapper downloads the configured Gradle distribution automatically when it is not already available locally.

## Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/maurovagge/SwimmingManager.git
   cd SwimmingManager
   ```

2. Open the project in Android Studio.

3. Allow Android Studio to sync the Gradle project and install any required SDK components.

4. Select the `app` configuration and run it on a connected device or emulator.

The first launch initializes the local game world. Select a club to create a new career and generate the initial roster, competition calendar, qualification standards, and historical data used by the simulation.

## Building from the command line

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## Testing

Run the local unit tests with:

```bash
./gradlew test
```

On Windows, use `.\gradlew.bat test` instead.

Instrumented tests require an available Android device or emulator:

```bash
./gradlew connectedAndroidTest
```

## How a season works

1. **Choose a club.** Search the available clubs and sign a contract with the one you want to manage.
2. **Review the roster.** Inspect swimmer profiles, attributes, personal and season bests, and current development.
3. **Set training.** Apply a team-wide training regime and stroke focus.
4. **Plan the calendar.** Open upcoming competitions and register eligible swimmers for individual and relay events.
5. **Advance the week.** The simulation processes registrations, call-ups, races, relays, medals, training effects, and the wider world.
6. **Manage the results.** Review live meets, competition results, rankings, records, messages, and the impact on the rest of the season.

Major international competitions use national-selection logic, while regional and club competitions use the relevant club registration flow. Both long-course and short-course events are represented in the event dictionary and qualification system.

## Project structure

```text
.
├── app/
│   └── src/
│       ├── main/java/com/example/swimmingmanager/
│       │   ├── data/       # Room entities and DAOs
│       │   ├── engine/     # Simulation, progression, calendar, and selection logic
│       │   └── ui/         # Navigation and Compose state
│       ├── main/java/ui/   # Compose screens
│       ├── main/assets/    # Seed data for clubs and generated names
│       └── test/           # Unit tests
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew[.bat]
```

The application stores game data locally in a Room database named `swimming_manager_db`. Preferences such as the selected club, current year, and current week are stored separately using Android `SharedPreferences`.

## Development notes

- The application is a local, single-device experience; no backend service or account system is required.
- The initial world is generated from the bundled CSV assets and seeded simulation data.
- Starting a new game clears the local game database and resets the saved game preferences.
- Room is configured with destructive fallback migrations for the current development database, so changing the schema may reset local game data when a migration is unavailable.

## Repository status

This repository contains the Android application source and build configuration. No pre-built release APK or published release process is defined in the project configuration.
