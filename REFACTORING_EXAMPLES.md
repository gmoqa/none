# Ejemplos de Refactorización TeaBoard

## Archivo 1: ButtonConstants.kt (NUEVO)

**Ubicación:** `shared/src/commonMain/kotlin/com/example/teaboard/constants/ButtonConstants.kt`

```kotlin
package com.example.teaboard.constants

/**
 * Centralized button configuration constants used across all platforms
 */
object ButtonConstants {
    const val MAX_BUTTONS = 12
    const val TOTAL_COLORS = 12
    
    val BUTTON_COLORS = listOf(
        "#4A90E2", // Soft Blue
        "#72C604", // Sage Green
        "#B4A7D6", // Muted Lavender
        "#BCD19E", // Mint Green
        "#F4A582", // Soft Coral
        "#8AB4D6", // Light Blue
        "#A8D8EA", // Light Cyan
        "#FFD3B6", // Light Peach
        "#D4A5A5", // Warm Taupe
        "#9FD8CB", // Seafoam
        "#C5A3E0", // Light Purple
        "#F4C87A"  // Warm Yellow
    )
    
    /**
     * Get button color by ID using modulo operation
     * This ensures consistent color cycling across all platforms
     */
    fun getButtonColor(buttonId: Int): String {
        val colorIndex = (buttonId - 1) % BUTTON_COLORS.size
        return BUTTON_COLORS[colorIndex]
    }
}
```

---

## Archivo 2: ValidationConstants.kt (NUEVO)

**Ubicación:** `shared/src/commonMain/kotlin/com/example/teaboard/constants/ValidationConstants.kt`

```kotlin
package com.example.teaboard.constants

/**
 * Validation limits and constraints used across platforms
 * These values are shared between Android and iOS to ensure consistency
 */
object ValidationConstants {
    
    object Label {
        const val MAX_LENGTH = 50
        const val MIN_LENGTH = 1
    }
    
    object Audio {
        const val MAX_FILE_SIZE_MB = 10
        const val MIN_SIZE_BYTES = 100L
    }
    
    object Image {
        const val MAX_FILE_SIZE_MB = 10
    }
    
    // File size in bytes
    fun toBytes(sizeMb: Int): Long = sizeMb.toLong() * 1024 * 1024
}
```

---

## Archivo 3: PreferencesKeys.kt (NUEVO)

**Ubicación:** `shared/src/commonMain/kotlin/com/example/teaboard/constants/PreferencesKeys.kt`

```kotlin
package com.example.teaboard.constants

/**
 * Centralized preferences/settings keys
 * Used by both Android SharedPreferences and iOS UserDefaults
 */
object PreferencesKeys {
    
    // Preferences storage name
    const val PREFS_NAME = "TeaBoardPrefs"
    
    // Sync & Google Drive
    const val SYNC_ENABLED = "sync_enabled"
    const val IS_LOGGED_IN = "is_logged_in"
    const val USER_EMAIL = "user_email"
    const val USER_NAME = "user_name"
    const val TEABOARD_FOLDER_ID = "teaboard_folder_id"
    const val CONFIG_FILE_ID = "config_file_id"
    
    // Application State
    const val DEFAULT_BUTTONS_CREATED = "default_buttons_created"
    const val DEFAULT_BUTTONS_LANGUAGE = "default_buttons_language"
    const val CURRENT_LANGUAGE = "current_language"
}
```

---

## Archivo 4: SettingsConstants.kt (NUEVO)

**Ubicación:** `shared/src/commonMain/kotlin/com/example/teaboard/constants/SettingsConstants.kt`

```kotlin
package com.example.teaboard.constants

/**
 * Settings-related constants
 */
object SettingsConstants {
    
    // Hidden settings access (triple tap)
    const val SETTINGS_TAP_COUNT_REQUIRED = 3
    const val SETTINGS_TAP_TIMEOUT_MS = 3000L
    
    // Grid layout
    const val TABLET_LANDSCAPE_COLUMNS = 3
    const val TABLET_PORTRAIT_COLUMNS = 2
    const val MIN_TABLET_WIDTH_DP = 600
}
```

---

## Archivo 5: TimeFormatter.kt (NUEVO)

**Ubicación:** `shared/src/commonMain/kotlin/com/example/teaboard/utils/TimeFormatter.kt`

```kotlin
package com.example.teaboard.utils

/**
 * Formatting utilities for time display
 * Currently used for recording time display (MM:SS.m format)
 */
object TimeFormatter {
    
    /**
     * Format recording time from seconds to MM:SS.m format
     * 
     * Examples:
     * - 5 seconds -> "00:05.0"
     * - 65 seconds -> "01:05.0"
     * - 125.5 seconds -> "02:05.5"
     */
    fun formatRecordingTime(timeInSeconds: Double): String {
        val minutes = timeInSeconds.toInt() / 60
        val seconds = timeInSeconds.toInt() % 60
        val milliseconds = ((timeInSeconds % 1) * 10).toInt()
        return "%02d:%02d.%01d".format(minutes, seconds, milliseconds)
    }
}
```

---

## Archivo 6: ButtonValidator.kt (NUEVO)

**Ubicación:** `shared/src/commonMain/kotlin/com/example/teaboard/validators/ButtonValidator.kt`

```kotlin
package com.example.teaboard.validators

import com.example.teaboard.constants.ValidationConstants
import com.example.teaboard.storage.PlatformFile

/**
 * Validation rules for button configuration
 * Centralized validation shared between Android and iOS
 */
class ButtonValidator {
    
    fun validateLabel(label: String): ValidationResult {
        return when {
            label.isBlank() -> 
                ValidationResult.Error("Label cannot be empty")
            label.length > ValidationConstants.Label.MAX_LENGTH -> 
                ValidationResult.Error("Label is too long (max ${ValidationConstants.Label.MAX_LENGTH} characters)")
            label.length < ValidationConstants.Label.MIN_LENGTH -> 
                ValidationResult.Error("Label is too short")
            else -> 
                ValidationResult.Success
        }
    }
    
    fun validateAudioFile(file: PlatformFile?): ValidationResult {
        return when {
            file == null -> 
                ValidationResult.Error("Audio file is required")
            !file.exists() -> 
                ValidationResult.Error("Audio file does not exist")
            file.length() < ValidationConstants.Audio.MIN_SIZE_BYTES -> 
                ValidationResult.Error("Audio file is too short (minimum ${ValidationConstants.Audio.MIN_SIZE_BYTES} bytes)")
            file.length() > ValidationConstants.toBytes(ValidationConstants.Audio.MAX_FILE_SIZE_MB) -> 
                ValidationResult.Error("Audio file is too large (max ${ValidationConstants.Audio.MAX_FILE_SIZE_MB}MB)")
            else -> 
                ValidationResult.Success
        }
    }
    
    fun validateImageFile(file: PlatformFile?): ValidationResult {
        return when {
            file == null -> 
                ValidationResult.Error("Image file is required")
            !file.exists() -> 
                ValidationResult.Error("Image file does not exist")
            file.length() == 0L -> 
                ValidationResult.Error("Image file is empty")
            file.length() > ValidationConstants.toBytes(ValidationConstants.Image.MAX_FILE_SIZE_MB) -> 
                ValidationResult.Error("Image file is too large (max ${ValidationConstants.Image.MAX_FILE_SIZE_MB}MB)")
            else -> 
                ValidationResult.Success
        }
    }
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

---

## Archivo 7: ButtonIdService.kt (NUEVO)

**Ubicación:** `shared/src/commonMain/kotlin/com/example/teaboard/services/ButtonIdService.kt`

```kotlin
package com.example.teaboard.services

import com.example.teaboard.constants.ButtonConstants

/**
 * Service for managing button IDs and availability
 */
class ButtonIdService {
    
    /**
     * Get the next available button ID
     * @param usedIds Set of currently used button IDs
     * @return The next available ID, or -1 if all slots are filled
     */
    fun getNextAvailableId(usedIds: Set<Int>): Int {
        for (id in 1..ButtonConstants.MAX_BUTTONS) {
            if (id !in usedIds) {
                return id
            }
        }
        return -1 // No available ID
    }
    
    /**
     * Check if maximum number of buttons has been reached
     */
    fun isMaxButtonsReached(currentCount: Int): Boolean {
        return currentCount >= ButtonConstants.MAX_BUTTONS
    }
    
    /**
     * Get remaining slots for buttons
     */
    fun getRemainingSlots(currentCount: Int): Int {
        return maxOf(0, ButtonConstants.MAX_BUTTONS - currentCount)
    }
}
```

---

## BEFORE/AFTER: Android MainActivity.kt Changes

### BEFORE (líneas 85-88):
```kotlin
// Button color cycle
private val BUTTON_COLORS = listOf(
    "#4A90E2", "#72C604", "#B4A7D6", "#BCD19E", "#F4A582", "#8AB4D6",
    "#A8D8EA", "#FFD3B6", "#D4A5A5", "#9FD8CB", "#C5A3E0", "#F4C87A"
)
```

### AFTER:
```kotlin
// Import at top
import com.example.teaboard.constants.ButtonConstants

// Usage in methods:
private fun getButtonColor(buttonId: Int): String {
    return ButtonConstants.getButtonColor(buttonId)
}
```

---

## BEFORE/AFTER: ConfigureButtonActivity.kt Changes

### BEFORE (líneas 68-71):
```kotlin
companion object {
    const val MAX_LABEL_LENGTH = 50
    const val MIN_LABEL_LENGTH = 1
    const val MAX_FILE_SIZE_MB = 10
    const val MIN_AUDIO_SIZE_BYTES = 100L
}
```

### AFTER:
```kotlin
// Import at top
import com.example.teaboard.constants.ValidationConstants
import com.example.teaboard.validators.ButtonValidator

companion object {
    const val REQUEST_CONFIGURE_BUTTON = 101
    const val REQUEST_SETTINGS = 102
}

private fun validateLabel(label: String): ValidationResult {
    return ButtonValidator().validateLabel(label)
}
```

---

## BEFORE/AFTER: iOS MainView.swift Changes

### BEFORE (líneas 138-147):
```swift
private var buttonColor: Color {
    switch buttonId {
    case 1: return Color(red: 1.0, green: 0.8, blue: 0.8)
    case 2: return Color(red: 0.8, green: 0.9, blue: 1.0)
    case 3: return Color(red: 1.0, green: 0.95, blue: 0.8)
    case 4: return Color(red: 0.85, green: 1.0, blue: 0.85)
    case 5: return Color(red: 0.95, green: 0.85, blue: 1.0)
    case 6: return Color(red: 1.0, green: 0.9, blue: 0.8)
    default: return Color.gray.opacity(0.3)
    }
}
```

### AFTER:
```swift
private var buttonColor: Color {
    let hexColor = ButtonConstants.shared.getButtonColor(Int(buttonId))
    return Color(hex: hexColor)
}

// Helper extension
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let scanner = Scanner(string: hex)
        var hexNumber: UInt64 = 0
        scanner.scanHexInt64(&hexNumber)
        
        let red = CGFloat((hexNumber >> 16) & 0xFF) / 255.0
        let green = CGFloat((hexNumber >> 8) & 0xFF) / 255.0
        let blue = CGFloat(hexNumber & 0xFF) / 255.0
        
        self.init(red: red, green: green, blue: blue)
    }
}
```

---

## BEFORE/AFTER: iOS SettingsView.swift Changes

### BEFORE (líneas 165-168):
```swift
private func loadSettings() {
    syncEnabled = appState.preferencesProvider.getBoolean(key: "sync_enabled", defaultValue: false)
    isLoggedIn = appState.preferencesProvider.getBoolean(key: "is_logged_in", defaultValue: false)
    userEmail = appState.preferencesProvider.getString(key: "user_email", defaultValue: "")
    userName = appState.preferencesProvider.getString(key: "user_name", defaultValue: "")
}
```

### AFTER:
```swift
import shared

private func loadSettings() {
    syncEnabled = appState.preferencesProvider.getBoolean(
        key: PreferencesKeys.SYNC_ENABLED, 
        defaultValue: false
    )
    isLoggedIn = appState.preferencesProvider.getBoolean(
        key: PreferencesKeys.IS_LOGGED_IN, 
        defaultValue: false
    )
    userEmail = appState.preferencesProvider.getString(
        key: PreferencesKeys.USER_EMAIL, 
        defaultValue: ""
    )
    userName = appState.preferencesProvider.getString(
        key: PreferencesKeys.USER_NAME, 
        defaultValue: ""
    )
}
```

---

## BEFORE/AFTER: iOS ConfigureButtonView.swift Changes

### BEFORE (líneas 274-279):
```swift
private func formatTime(_ time: TimeInterval) -> String {
    let minutes = Int(time) / 60
    let seconds = Int(time) % 60
    let milliseconds = Int((time.truncatingRemainder(dividingBy: 1)) * 10)
    return String(format: "%02d:%02d.%01d", minutes, seconds, milliseconds)
}
```

### AFTER:
```swift
import shared

private func formatTime(_ time: TimeInterval) -> String {
    return TimeFormatter.shared.formatRecordingTime(timeInSeconds: time)
}
```

---

## Integration Strategy

### Phase 1: Create Constants & Utils (Week 1)
1. Create ButtonConstants.kt
2. Create ValidationConstants.kt
3. Create PreferencesKeys.kt
4. Create SettingsConstants.kt
5. Create TimeFormatter.kt

**Testing:** Unit tests for each constant object

### Phase 2: Create Services & Validators (Week 2)
1. Create ButtonValidator.kt
2. Create ButtonIdService.kt
3. Add comprehensive tests

**Testing:** Validator tests with edge cases

### Phase 3: Refactor Android (Week 2-3)
1. Update MainActivity.kt to remove duplicated constants
2. Update ConfigureButtonActivity.kt to use validators
3. Update MainPresenter.kt to use ButtonConstants
4. Update ConfigureButtonPresenter.kt to use ButtonValidator
5. Update DefaultButtonsHelper.kt to use PreferencesKeys

**Testing:** Regression testing on all Android flows

### Phase 4: Refactor iOS (Week 3-4)
1. Update MainView.swift to use ButtonConstants
2. Update ConfigureButtonView.swift to use TimeFormatter
3. Update SettingsView.swift to use PreferencesKeys
4. Add validation to iOS ConfigureButtonView

**Testing:** Regression testing on all iOS flows

---

## Estimated Effort

| Task | Hours | Notes |
|------|-------|-------|
| Create 5 constant objects | 8 | Straightforward |
| Create validators/services | 12 | Include tests |
| Refactor Android code | 12 | Multiple files |
| Refactor iOS code | 10 | Simpler changes |
| Testing & QA | 8 | End-to-end |
| **Total** | **50** | ~1.5 weeks full-time |

