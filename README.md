# Minimal Home - Android Launcher

A minimal, elegant Android home launcher designed for simplicity and efficiency. Built specifically for Samsung Galaxy S24+ and modern Android devices.

## Features

- **Clean Clock Display**: Large, easy-to-read clock centered on the home screen with date
- **Swipe Up for Apps**: Intuitive swipe-up gesture to access your app drawer
- **Smart Search**: Search bar at the bottom to quickly find apps and files
- **Minimalist Design**: Black background with clean, modern UI
- **Edge-to-Edge Display**: Full immersive experience with hidden system bars
- **4-Column App Grid**: Organized app drawer with 4 apps per row

## Requirements

- Android 8.0 (API 26) or higher
- Recommended: Samsung Galaxy S24+ or similar flagship device
- Permissions:
  - QUERY_ALL_PACKAGES - To display installed apps
  - READ_EXTERNAL_STORAGE / READ_MEDIA - For file search functionality

## Installation

### Building from Source

1. Clone this repository:
   ```bash
   git clone <repository-url>
   cd minimal_home_app
   ```

2. Open the project in Android Studio (Hedgehog 2023.1.1 or later)

3. Build the project:
   - Click **Build** > **Make Project**
   - Or run: `./gradlew build`

4. Install on your device:
   - Connect your Android device via USB
   - Click **Run** > **Run 'app'**
   - Or run: `./gradlew installDebug`

### Setting as Default Launcher

1. After installation, press the Home button
2. Android will prompt you to choose a launcher
3. Select "Minimal Home" and tap "Always" or "Set as default"

To change back to your original launcher:
1. Go to **Settings** > **Apps** > **Default apps** > **Home app**
2. Select your preferred launcher

## Usage

### Home Screen
- The clock is displayed in the center of the screen
- Search bar is located at the bottom
- Swipe up from anywhere to open the app drawer

### App Drawer
- Swipe up to open
- Swipe down or press back to close
- Apps are displayed in a 4-column grid
- Apps are sorted alphabetically

### Search
- Tap the search bar to automatically open the app drawer
- Start typing to filter apps by name
- Results update in real-time as you type

## Architecture

```
com.minimal.home/
├── MainActivity.kt       # Main launcher activity with gesture handling
├── AppInfo.kt           # Data class for app information
└── AppsAdapter.kt       # RecyclerView adapter for app grid
```

### Key Components

- **MainActivity**:
  - Handles gesture detection for swipe up/down
  - Manages app drawer visibility
  - Loads and displays installed apps
  - Implements search functionality

- **AppsAdapter**:
  - RecyclerView adapter for displaying apps in a grid
  - Handles app filtering for search
  - Launches apps when tapped

## Customization

### Changing Grid Columns
In `MainActivity.kt`, modify the GridLayoutManager:
```kotlin
appsRecyclerView.layoutManager = GridLayoutManager(this, 4) // Change 4 to desired columns
```

### Clock Format
The clock automatically uses 12-hour or 24-hour format based on device settings.
Modify in `activity_main.xml`:
```xml
android:format12Hour="h:mm"
android:format24Hour="HH:mm"
```

### Colors and Theme
Edit `res/values/colors.xml` and `res/values/themes.xml` to customize colors.

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build Tool**: Gradle 8.2.0
- **Libraries**:
  - AndroidX Core KTX
  - Material Design Components
  - ConstraintLayout
  - RecyclerView
  - Lifecycle Runtime KTX

## Permissions

The app requests the following permissions:

- `QUERY_ALL_PACKAGES`: Required to list all installed apps
- `READ_EXTERNAL_STORAGE`: For file search (Android 12 and below)
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`: For file search (Android 13+)

## Known Limitations

- File search is currently implemented for apps only
- No widget support (intentionally minimal)
- No app uninstall from drawer (use Settings)
- No wallpaper customization

## Future Enhancements

- Add file search integration
- Implement app shortcuts
- Add gesture customization
- Support for favorite apps
- Notification dots
- App usage statistics

## License

This project is open source and available under the MIT License.

## Support

For issues, questions, or contributions, please open an issue on the repository.

## Compatibility

Tested and optimized for:
- Samsung Galaxy S24+
- Android 14 (API 34)
- Should work on all modern Android devices (Android 8.0+)

## Performance

- Lightweight: Less than 5MB installed
- Fast app loading with coroutines
- Smooth animations at 60fps
- Minimal battery impact
- No background services
