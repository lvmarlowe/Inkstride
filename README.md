![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![SQLite](https://img.shields.io/badge/sqlite-%2307405e.svg?style=for-the-badge&logo=sqlite&logoColor=white)

# Inkstride

Narrative-driven Android walking app that turns real-world steps into story progression. Built with Kotlin, Jetpack Compose, Room, Health Connect, and WorkManager.

---

## Demo

<table width="100%">
  <tr>
    <td width="33%" align="center"><img src="https://github.com/user-attachments/assets/fde5c836-0ac0-419f-b520-f57fb6567e10" width="100%" alt="Journey screen"/></td>
    <td width="33%" align="center"><img src="https://github.com/user-attachments/assets/9d1a4dd5-f50f-4a82-bb04-dce0d78cf964" width="100%" alt="Story unlock screen"/></td>
    <td width="33%" align="center"><img src="https://github.com/user-attachments/assets/26acfe24-d340-4aaa-b1f1-f60ab0ec472e" width="100%" alt="Storybook screen"/></td>
  </tr>
</table>
<table width="100%">
  <tr>
    <td width="50%" align="center">
      <a href="https://youtu.be/Oal0huWeeeI"><img src="https://img.youtube.com/vi/Oal0huWeeeI/maxresdefault.jpg" width="100%" alt="Demo video"/></a><br/>
      ▶ Watch the demo
    </td>
    <td width="50%" align="center">
      <a href="https://youtu.be/mmoHv9AYurg"><img src="https://img.youtube.com/vi/mmoHv9AYurg/maxresdefault.jpg" width="100%" alt="Architecture walkthrough"/></a><br/>
      ▶ Watch the architecture walkthrough
    </td>
  </tr>
</table>

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material3)
- **Database:** Room (SQLite)
- **Async / Sync:** WorkManager + Health Connect API
- **Architecture:** MVVM + Repository Pattern
  
---

## Core Features

- Health Connect step sync (foreground + background)
- Journey dashboard (day, today distance, total distance, next milestone)
- Story unlock pager for newly unlocked segments
- Storybook archive for previously read segments
- Storybook scroll position memory and bookmark for newly unlocked segments
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

Beyond the mechanics, this project is a personal exploration of found family and the necessity of supportive connections. I began writing this narrative while navigating the grief of my father’s death and the complexities of his decade-long battle with dementia. As a result, the story follows characters as they navigate memory loss and the long process of healing from the past. By centering the world on emotional resilience and mutual support, Inkstride is designed to be a quiet space for restoration for anyone walking through their own experiences with loss or trauma.

---

## Story

**Act 1: The Unmaking**  
*168 miles | 13,200+ words*

Something has drained the color from the forest and left the landscape devastated. You play the Inker, a traveler whose past is as mysterious as the world you traverse. After waking at the edge of the forest with no memory, you start down a path leading deeper into the woods. Accompanied by woodland creatures you encounter along the way, you set out in search of answers about the disaster they call the Unmaking.

**Act 2: The Awakening**  
*TBD*

**Act 3: The Reckoning**  
*TBD*

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

```text
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
- Storybook table of contents
- Settings screen with customizable character name and distance unit (km/mi) selection
- Daily stats and activity reports
- Illustrated map tab showing journey progress
- Hand-drawn illustrations throughout the story
- Audio narration
- Scrapbook tab collecting unlocked characters, places, and trinkets with unlock dates

---

## Inspiration

- *Glow* by Bombyx
- *Zombies, Run!* by Zombies Run! Ltd
- *Fitbit Adventures* by Chris Burkard
- *Fantasy Hike* by Forge7 AB
- *Sky: Children of the Light* by thatgamecompany inc
- *Peek a Phone* by FaintLines, Inc.

## Author

LV Marlowe
