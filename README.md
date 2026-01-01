# Minimalist Launcher

A minimalist Android home launcher application designed to help users reduce phone usage through intentional design and friction.

## Features

### 🏠 Minimal Home Screen
- Clean, distraction-free interface
- Large time and date display
- Daily screen time summary
- No widgets, no notifications preview
- Monochrome/grayscale color scheme

### 📱 Intentional App Access
- Text-based alphabetical app list
- Real-time search functionality
- Launch counter for each app (daily usage count)
- Optional "Do you really need this?" confirmation dialogs
- Support for showing/hiding app icons

### 📊 Usage Awareness
- Daily screen time tracking
- App launch frequency monitoring
- Configurable launch limit warnings
- Visual reminders for frequently-used apps

### 🎯 Focus Modes
- "Deep Work" mode with essential apps only (calls, messages, calendar)
- Create custom focus modes with app allowlists
- Activate/deactivate focus modes on demand
- Focus mode indicator on home screen

## Technical Details

- **Language:** Java
- **Minimum SDK:** Android 10 (API 29)
- **Target SDK:** Android 14 (API 34)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** Room Persistence Library
- **Async Operations:** RxJava3
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

Open the project in Android Studio and sync Gradle files. Build the APK:

```bash
./gradlew assembleDebug
```

### 2. Install on Device

```bash
./gradlew installDebug
```

Or use Android Studio's "Run" button.

### 3. Grant Permissions

After installation:

1. Open the launcher app
2. Tap the settings icon (top right)
3. Grant **Usage Stats Permission** (required for screen time tracking)
4. Set as **Default Launcher** (makes this your home screen)

### 4. Set as Default Launcher

1. Press the Home button on your device
2. Select "Minimalist Launcher" from the launcher chooser
3. Optionally select "Always" to make it the default

## Usage

### Home Screen
- Tap anywhere on the screen to view your app list
- Screen time and focus mode status are displayed centrally
- Tap the settings icon to access configuration

### App List
- Use the search bar to quickly find apps
- Launch counters appear next to frequently-used apps
- If warnings are enabled, you'll see a confirmation dialog for apps opened more than 10 times per day (configurable)

### Settings
- **Show App Icons:** Toggle between text-only and icon+text display
- **Enable Launch Warnings:** Turn friction dialogs on/off
- **Focus Modes:** Manage custom focus modes

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
Edit `SharedPreferences` key `"warning_threshold"` (default: 10 launches)

### Modify Deep Work Allowlist
See `AppFilterHelper.getDeepWorkAllowlist()` to add/remove essential apps

### Change Color Scheme
Edit `res/values/colors.xml` to customize the grayscale palette

## Known Limitations

- Usage stats permission must be manually granted in system settings
- Some system launchers may interfere with launcher selection
- Screen time tracking requires Android 10+ (API 29)
- Focus mode scheduling is not yet implemented

## Future Enhancements

- [ ] Scheduled focus modes (auto-activate during specific hours)
- [ ] Weekly/monthly usage statistics
- [ ] App blocking (completely hide apps during focus mode)
- [ ] Type-to-launch friction mode
- [ ] Export usage data
- [ ] Widget support for quick focus mode toggle

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
