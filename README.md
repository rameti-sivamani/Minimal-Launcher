# Minimalist Launcher

A minimalist Android home launcher application designed to help users reduce phone usage through intentional design and friction.

## Features

### 🏠 Minimal Home Screen
- Clean, distraction-free interface
- Large time and date display (follows the system 12/24-hour setting)
- Today's screen time, calculated on-device from Android usage access
- Up to 4 favorite apps, reorderable (long-press a favorite)
- Gestures: swipe up or right (or tap the clock) for all apps, swipe left for the camera.
  Start horizontal swipes a little away from the screen edge; the edges belong to Android's Back gesture.
- No widgets, no notifications preview
- Monochrome/grayscale color scheme

### 🎨 Styles
- **Aura** (default): dark, big type, one bright accent (lime, tangerine, sky or orchid)
- **Mono**: pure black, serif clock, lowercase typewriter names
- **Paper**: warm off-white, serif, deep accents
- **Auto**: Aura at night, Paper by day

### 🌱 Wellbeing
- Daily screen time goal with a progress ring on the home screen
- Streaks: consecutive days under your goal
- Daily intention: one line for today, shown on the home screen
- Mindful pause: a short breathing screen before distracting apps (long-press › Pause before opening),
  also shown when an app is over its time limit or launch reminder

### 📱 Intentional App Access
- Text-based alphabetical app list
- Real-time search; press Enter to open the top result
- Sort alphabetically, by most used today, or by recently installed
- Launch counter for each app (daily usage count)
- Optional "Do you really need this?" confirmation dialogs
- Support for showing/hiding app icons

### 📊 Usage Awareness
- Daily screen time tracking, with a Usage Report (today by app + last 7 days); tap the screen time on the home screen
- Daily time limits per app (long-press an app › Daily time limit); opening an app past its limit asks you to take a break
- The app list shows today's time, limit and launches next to each app
- App launch frequency monitoring
- Configurable launch limit warnings
- Visual reminders for frequently-used apps

### 🎯 Focus Modes
- "Deep Work" mode with essential apps only (calls, messages, calendar)
- Create and edit custom focus modes with an app picker (Settings › Focus Modes)
- Activate/deactivate focus modes on demand, or schedule them (e.g. 09:00–17:00, Mon–Fri; overnight ranges work too)
- Focus mode indicator on home screen

## Technical Details

- **Language:** Java
- **Minimum SDK:** Android 10 (API 29)
- **Target SDK:** Android 16 (API 36)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** Room Persistence Library
- **Async Operations:** single-thread executors + LiveData
- **Background Tasks:** WorkManager

## Project Structure

```
app/src/main/java/com/minimalist/launcher/
├── data/
│   ├── database/
│   │   ├── entities/      # Room entities (AppUsage, AppLaunchCounter, FocusMode)
│   │   ├── dao/           # Data Access Objects
│   │   └── AppDatabase.java
│   ├── model/             # Data models (AppInfo, UsageStats)
│   └── repository/        # Repository layer (AppRepository, UsageRepository)
├── ui/
│   ├── launcher/          # Home screen
│   ├── applist/           # App list screen
│   ├── settings/          # Settings screen
│   └── focus/             # Focus mode management
├── utils/                 # Helper classes
├── workers/               # Background tasks
└── LauncherApplication.java
```

## Setup Instructions

### 1. Build the Project

Open the project in Android Studio and sync Gradle files, or use the wrapper (JDK 17+):

```bash
./gradlew assembleDebug        # debug build (installs alongside release as .debug)
./gradlew testDebugUnitTest    # unit tests
```

For a Play Store release build, see [PLAY_STORE_RELEASE.md](PLAY_STORE_RELEASE.md).

### 2. Install on Device

```bash
./gradlew installDebug
```

Or use Android Studio's "Run" button.

### 3. Grant Permissions

On first launch a short onboarding explains and requests:

1. **Usage access** (optional): only for on-device screen time. Can be changed later in Settings › Usage Access.
2. **Default home app**: uses the system "default home app" dialog.

### 4. Set as Default Launcher

1. Press the Home button on your device
2. Select "Minimalist Launcher" from the launcher chooser
3. Optionally select "Always" to make it the default

## Usage

### Home Screen
- Swipe right or up (or tap the clock) to view your app list; swipe left for the camera
- Screen time and focus mode status are displayed centrally
- Long-press a favorite to reorder or remove it
- Tap the settings icon to access configuration

### App List
- Use the search bar to quickly find apps
- Launch counters appear next to frequently-used apps
- If warnings are enabled, you'll see a confirmation dialog for apps opened more than 10 times per day (configurable)

### Settings
- **Appearance:** theme (OLED black, dark gray, light, auto), font size, app icons, day of week, quick info
- **Usage awareness:** screen time, usage report, usage access, app time limits, launch warnings and their threshold, focus modes
- **Apps:** sort order, hidden apps, reset favorites
- **System:** set as default launcher, privacy policy, version

### Focus Modes
- Create a "Deep Work" mode with one tap (essential apps only)
- Create custom modes by selecting specific apps
- Activate a mode to filter the app list
- Active mode is shown on the home screen

## Database Schema

### AppUsage Table
- Tracks individual app launch events
- Fields: packageName, appName, launchTime, duration, date

### AppLaunchCounter Table
- Daily counter for each app
- Fields: packageName, appName, launchCount, date, warningThreshold

### FocusMode Table
- Focus mode configurations
- Fields: name, allowedApps, isActive, hasSchedule, startTime, endTime

## Customization

### Change Warning Threshold
Settings › Launch Warning Threshold (default: 10 launches a day)

### Modify Deep Work Allowlist
See `AppFilterHelper.getDeepWorkAllowlist()` to add/remove essential apps

### Change Color Scheme
Edit `res/values/colors.xml` to customize the grayscale palette

## Known Limitations

- Usage access must be granted by the user in system settings (Android requirement)
- Some system launchers may interfere with launcher selection
- Screen time tracking requires Android 10+ (API 29)
- Time limits are checked when an app is opened from the launcher, not while it is already open

## Future Enhancements

- [x] Scheduled focus modes (auto-activate during specific hours)
- [x] Weekly usage statistics
- [x] Per-app daily time limits
- [ ] App blocking (completely hide apps during focus mode)
- [ ] Type-to-launch friction mode
- [ ] Export usage data
- [ ] Widget support for quick focus mode toggle

## Privacy

All data stays on the device; the app has no internet permission. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## License

This project is open source and available for personal use.

## Credits

Built with Android Jetpack components:
- Room Database
- LiveData & ViewModel
- WorkManager
- Material Design 3

---

**Note:** This is a minimalist launcher focused on reducing distractions. It intentionally omits features like widgets, notification previews, and app shortcuts to encourage mindful phone usage.
