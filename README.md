![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![SQLite](https://img.shields.io/badge/sqlite-%2307405e.svg?style=for-the-badge&logo=sqlite&logoColor=white)

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

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose (Material3)
- Database: Room (SQLite)
- Async / Sync: WorkManager + Health Connect API
- Architecture: MVVM + Repository Pattern
  
---

## Core Features

- Health Connect step sync (foreground + background)
- Journey dashboard (day, today distance, total distance, next milestone)
- Story unlock pager for newly unlocked segments
- Storybook archive for previously read segments
- Local-first persistence with Room
- Startup routing with permission-aware flow

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

## Why I Built This

I built Inkstride to capture the compelling "what happens next" curiosity that makes story-driven games so engaging. The goal was to take the best elements of narrative progression and discovery from my favorite apps and games, but strip away the common stressors like competition, streaks, and timers. By focusing on a developing world and unfolding mystery, Inkstride turns physical movement into a low-pressure journey where the story is the primary reward for every step taken.

Beyond the mechanics, this project is a personal exploration of found family and the necessity of supportive connections. I began writing this narrative while navigating the grief of my father’s passing and the complexities of his decade-long battle with dementia. As a result, the story follows characters as they navigate memory loss and the long process of healing from the past. By centering the world on emotional resilience and mutual support, Inkstride is designed to be a quiet space for restoration for anyone walking through their own experiences with loss or trauma.

---

## Story

**Act 1: The Unmaking** (168 miles | 13,200+ words) — Something has drained the color from the forest and left the landscape devastated. You play the Inker, a traveler whose past is as mysterious as the world you traverse. After waking at the edge of the forest with no memory, you start down a path leading deeper into the woods. Accompanied by woodland creatures you encounter along the way, you set out in search of answers about the disaster they call the Unmaking.

**Act 2: The Awakening** (TBD) — Coming soon.

**Act 3: The Reckoning** (TBD) — Coming soon.

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

## Documentation

[Full project documentation](docs/project-documentation.docx)

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

## Privacy

Inkstride’s use of information received from Health Connect adheres to the Health Connect Developer Policy, including the Limited Use requirements, ensuring that health data is accessed only to drive in-app narrative progression and is never shared with third parties. All data is stored locally on the device. Inkstride does not collect, transmit, or share any user data.

---

## Roadmap

- Push notifications for milestone unlocks
- Local backup and restore for progress across devices and reinstalls
- Settings screen with customizable character name and distance unit (km/mi) selection
- Daily stats and activity reports
- Illustrated map tab showing journey progress
- Hand-drawn illustrations throughout the story
- Scrapbook tab collecting unlocked characters, places, and trinkets with unlock dates

---

## Inspiration

- *Glow* by Bombyx
- *Fantasy Hike* by Forge7 AB
- *Fitbit Adventures* by Chris Burkard
- *Peek a Phone* by FaintLines, Inc.
- *An Elmwood Trail* by Techyonic

## Author

LV Marlowe
