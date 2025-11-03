# None - AAC App for Autism

A Kotlin Multiplatform AAC (Augmentative and Alternative Communication) tool designed for people with autism.

**Website**: [noneapp.org](https://noneapp.org)

## Features

- **Customizable buttons** with images and audio
- **Built-in audio recording** (M4A format) for personalized sounds
- **Camera & gallery support** for button images
- **Works offline** - local storage by default
- **Optional Google Drive sync** across devices with automatic fallback
- **Edit/Use modes** - separate modes for configuration and daily use
- **Clean, distraction-free interface** optimized for accessibility
- **Multi-language support** (ES, EN, FR, PT, DE, AR, IT, JA, ZH)
- **Cross-platform** - Android and iOS (Kotlin Multiplatform)

## Platforms

- **Android**: minSdk 21 (Android 5.0+), targetSdk 34
- **iOS**: Coming soon

## Quick Start

### Android

1. Clone this repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on device or emulator

### iOS

1. Open `iosApp/TeaBoard.xcodeproj` in Xcode
2. Build and run on simulator or device

The app works immediately in offline mode. Google Drive sync is optional and can be enabled in Settings.

## Architecture

- **Kotlin Multiplatform** - Shared business logic across platforms
- **Shared constants** - Centralized configuration (ButtonConstants, PreferencesKeys, ValidationConstants)
- **Storage strategy pattern** - Automatic fallback from Drive to local storage
- **MVP pattern** - Clean separation of concerns

## Tech Stack

- Kotlin Multiplatform
- Android SDK 34 (min API 21)
- SwiftUI (iOS)
- Google Drive API (optional)
- Material Design 3 (Android)
- Local JSON storage with cloud sync

## License

GNU GPL v3 - Free and open source forever.

**Copyright © 2023-2025 Guillermo Quinteros**

Named in honor of Salvador "None" Quinteros.

## Contact

- Email: gu.quinteros@gmail.com
- Website: [noneapp.org](https://noneapp.org)

---

**Note**: This is an assistive tool and does not replace professional therapy. Always consult with autism specialists for comprehensive care.
