# Inkstride

Narrative-driven Android walking app that turns real-world steps into story progression. Built with Kotlin, Jetpack Compose, Room, Health Connect, and WorkManager.

---

## Demo

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/3ea27c95-e6a0-4c67-88b1-9f0e21e81509" width="250" alt="Journey screen"/></td>
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

## Privacy

All data is stored locally on the device. Inkstride does not collect, transmit, or share any user data.

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

## Story

**Act 1: The Unmaking** (168 miles) — Something has drained the color from the forest and silenced everything in it. You play the Inker, a traveler with a past as mysterious as the world you're walking through. Accompanied by the woodland creatures who survived the damage, you set out searching for answers.

**Act 2: The Awakening** (TBD) — Coming soon.

**Act 3: The Reckoning** (TBD) — Coming soon.

---

## Documentation

[Full project documentation](docs/project-documentation.docx)

---

## Roadmap

- Settings screen with character name and distance unit (km/mi)
- Push notifications for milestone unlocks
- Daily stats and activity reports
- Illustrated map tab showing journey progress
- Hand-drawn illustrations throughout the story
- Scrapbook tab collecting unlocked characters, places, and trinkets with unlock dates
- Local backup and restore for progress across devices and reinstalls

---

## Author

LV Marlowe
