# Inkstride

Narrative-driven Android walking app that turns real-world steps into story progression across a 168-mile journey. Built with Kotlin, Jetpack Compose, Room, Health Connect, and WorkManager.

---


## Demo

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/e82d6c9c-6c0a-468f-9440-db1d6667316b" width="250" alt="Journey screen"/></td>
    <td><img src="https://github.com/user-attachments/assets/ca5ce0c6-9c4e-40d4-923b-1b3c59ff11bb" width="250" alt="Story unlock screen"/></td>
    <td><img src="https://github.com/user-attachments/assets/bf025ffd-97dc-4361-a93d-1219ee880e06" width="250" alt="Storybook screen"/></td>
  </tr>
</table>

---

## Why I Built This

Most fitness apps are built around challenges and competition. Inkstride is built around something quieter: the idea that movement itself is worth rewarding and that a story is a better reward than a streak. Walk more, reach the next milestone, unlock part of the 10,000-word story.

---

## Core Features

- Health Connect step sync (foreground + background)
- Journey dashboard (day, today distance, total distance, next milestone)
- Story unlock pager for newly unlocked segments
- Storybook archive for previously read segments
- Local-first persistence with Room
- Startup routing with permission-aware flow

---

## Tech Stack

- Kotlin
- Jetpack Compose (Material3)
- AndroidX Lifecycle / Activity Compose
- Room (SQLite)
- Health Connect
- WorkManager
- JUnit / AndroidX Test

---

## Architecture

MVVM + Repository pattern

| Layer | Contents |
|---|---|
| UI | Compose screens, components, router |
| ViewModel | State, routing, sync orchestration |
| Service | Progress calculations, milestone engine, validation |
| Data | Room entities, DAOs, repositories |
| Integration | Health Connect, WorkManager |

---

## App Flow

1. App startup initializes router and local defaults.
2. App requests Health Connect permissions.
3. App requests background permissions (routing then continues).
4. App routes to intro (if unread) or Journey screen.
5. Foreground sync runs every ~5 min and on pull-to-refresh.
6. Background sync runs every ~15 minutes.
7. New unlocks trigger Storybook tab badge.
8. Data persists in Room across launches.

---

## Project Structure

```
app/src/main/java/com/inkstride/app/
    ui/          Screens, components, theme, viewmodels, router
    data/        Database, entities, DAOs, repositories
    health/      Health Connect manager, sync coordinator, worker, scheduler
    services/    Progress, milestone, validation, error handling

app/src/main/assets/
    story_seed_data.json
```

---

## Getting Started

**Prerequisites**
- Android Studio (stable)
- Android SDK API 33+
- Android 13+ device or emulator recommended

**Build**
```bash
./gradlew assembleDebug
```

**Test**
```bash
./gradlew test
./gradlew lint
```

---

## Documentation

Full project documentation: `docs/project-documentation.docx`

---

## Author

LV Marlowe
