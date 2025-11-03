# Developer Documentation

Technical documentation for building and contributing to None.

## Tech Stack

- **Kotlin Multiplatform** - Shared business logic across platforms
- **Android SDK 34** (min API 21 - Android 5.0+)
- **SwiftUI** (iOS)
- **Google Drive API** (optional cloud sync)
- **Material Design 3** (Android)
- **Local JSON storage** with cloud sync

## Architecture

### Kotlin Multiplatform Structure

```
shared/
├── commonMain/      # Shared code (constants, models)
├── androidMain/     # Android-specific implementations
└── iosMain/         # iOS-specific implementations
```

### Key Components

- **Shared Constants**: ButtonConstants, PreferencesKeys, ValidationConstants
- **Storage Pattern**: Automatic fallback from Drive to local storage
- **MVP Pattern**: Clean separation of concerns
- **Services**: AudioRecorderService, AudioPlayerService, StorageService

### Storage Strategy

The app uses a delegator pattern:
- `StorageService` - Routes to local or cloud storage
- `LocalStorageService` - JSON files in app's private storage
- `DriveStorageService` - Optional Google Drive sync

All operations attempt Drive sync first (if enabled), then fall back to local storage automatically.

## Building from Source

### Prerequisites

- Android Studio (Arctic Fox or newer)
- JDK 11 or higher
- Xcode (for iOS development)
- Git

### Android

1. Clone the repository
```bash
git clone https://github.com/gmoqa/none.git
cd none
```

2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### iOS

1. Open `iosApp/TeaBoard.xcodeproj` in Xcode
2. Select target device or simulator
3. Build and run (Cmd+R)

### Shared Module

Build the shared Kotlin Multiplatform module:
```bash
./gradlew :shared:build
```

## Optional: Google Drive Setup

Google Drive sync is optional. The app works in offline mode by default.

To enable Drive sync during development:

1. Create a project in Google Cloud Console
2. Enable Google Drive API
3. Configure OAuth 2.0 credentials
4. Get SHA-1 fingerprint:
```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android
```

See `GOOGLE_DRIVE_SETUP.md` for detailed instructions.

## Project Structure

```
none/
├── app/                    # Android app
│   ├── src/main/java/      # Kotlin source code
│   └── src/main/res/       # Android resources
├── shared/                 # Kotlin Multiplatform shared code
│   ├── src/commonMain/     # Shared constants and models
│   ├── src/androidMain/    # Android implementations
│   └── src/iosMain/        # iOS implementations
├── iosApp/                 # iOS app (SwiftUI)
│   └── TeaBoard/
├── docs/                   # Landing page (GitHub Pages)
└── documentation/          # Project documentation
```

## Common Gradle Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Check for dependency updates
./gradlew dependencyUpdates

# Lint checks
./gradlew lint

# Generate signing report (SHA-1)
./gradlew signingReport
```

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly on both platforms
5. Submit a pull request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

### Testing

- Test on real Android devices (not just emulators)
- Test on iOS simulators and physical devices
- Test offline mode
- Test with and without Google Drive sync

## Release Process

See `DEPLOYMENT_CHECKLIST.md` (Android) and `IOS_DEPLOYMENT_CHECKLIST.md` (iOS) for deployment guides.

## License

GNU GPL v3 - See LICENSE file for details.

## Contact

- Email: gu.quinteros@gmail.com
- GitHub: [@gmoqa](https://github.com/gmoqa)
