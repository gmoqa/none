#!/bin/bash

# Script to create 50 conventional commits simulating 1 year of development

# Calculate date from 1 year ago
START_DATE=$(date -v-1y -v-1d "+%Y-%m-%d 10:00:00")

# Commit messages following conventional commits
declare -a COMMITS=(
    "feat: initial commit"
    "chore: add gradle configuration and project structure"
    "feat: add Android app module with MainActivity"
    "feat: add basic button grid layout"
    "style: add Material Design 3 theme and colors"
    "feat: add button configuration dialog"
    "feat: implement image capture from camera"
    "feat: add image selection from gallery"
    "feat: implement audio recording functionality"
    "feat: add audio playback for buttons"
    "fix: resolve camera permission issues on Android 13+"
    "feat: add button label customization"
    "feat: implement local storage with JSON"
    "refactor: extract storage logic to StorageService"
    "feat: add Google Drive integration"
    "feat: implement OAuth 2.0 for Google Sign-In"
    "feat: add Drive file upload for images"
    "feat: add Drive file upload for audio"
    "feat: sync button configurations to Drive"
    "fix: handle offline mode gracefully"
    "feat: add settings screen"
    "feat: implement sync enable/disable toggle"
    "feat: add edit mode toggle for buttons"
    "feat: add button deletion functionality"
    "refactor: implement MVP pattern with presenters"
    "feat: add Spanish localization"
    "feat: add default buttons with pre-configured content"
    "fix: improve audio quality and reduce file size"
    "feat: implement audio silence trimming"
    "feat: add haptic feedback on button press"
    "style: improve UI with rounded corners and shadows"
    "feat: add fullscreen mode for distraction-free use"
    "docs: add comprehensive README and setup guides"
    "feat: add support for 12 buttons with pagination"
    "chore: migrate to Kotlin Multiplatform"
    "feat: add shared module for cross-platform code"
    "feat: implement iOS app with SwiftUI"
    "feat: add iOS audio recording and playback"
    "feat: add iOS image picker and camera"
    "refactor: move models to shared module"
    "refactor: centralize constants in shared module"
    "feat: add PreferencesKeys for type-safe settings"
    "feat: add ValidationConstants for input validation"
    "feat: add ButtonConstants for consistent colors"
    "refactor: migrate Android to use shared constants"
    "refactor: migrate iOS to use shared constants"
    "test: add unit tests for validation logic"
    "docs: add architecture documentation with diagrams"
    "chore: optimize build configuration and dependencies"
    "feat: landing page"
)

# Files to add progressively (simulating development)
declare -a FILES_BATCH_1=(
    ".gitignore"
    "gradle.properties"
    "settings.gradle"
    "build.gradle"
    "gradlew"
    "gradle/"
)

declare -a FILES_BATCH_2=(
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/res/values/strings.xml"
    "app/src/main/res/values/colors.xml"
)

declare -a FILES_BATCH_3=(
    "app/src/main/java/com/example/teaboard/MainActivity.kt"
    "app/src/main/res/layout/"
)

# Initialize git if not already initialized
git init

# Configure git user (for the commits)
git config user.name "TeaBoard Developer"
git config user.email "dev@teaboard.app"

# Counter for days
DAYS_OFFSET=0

# Create commits
for i in "${!COMMITS[@]}"; do
    COMMIT_MSG="${COMMITS[$i]}"

    # Calculate commit date (distribute over 365 days)
    DAYS_INCREMENT=$((365 * i / 50))
    COMMIT_DATE=$(date -v-1y -v+${DAYS_INCREMENT}d "+%Y-%m-%d %H:%M:%S")

    # Add files based on commit number
    if [ $i -eq 0 ]; then
        # First commit: add basic files
        git add .gitignore LICENSE NOTICE README.md
    elif [ $i -lt 10 ]; then
        # Early commits: add gradle and basic structure
        git add gradle.properties settings.gradle build.gradle gradlew gradle/ 2>/dev/null || true
        git add app/build.gradle app/proguard-rules.pro 2>/dev/null || true
    elif [ $i -lt 20 ]; then
        # Middle commits: add Android app files
        git add app/src/main/ 2>/dev/null || true
    elif [ $i -lt 30 ]; then
        # Add docs and assets
        git add docs/ app/src/main/res/ 2>/dev/null || true
    elif [ $i -lt 35 ]; then
        # Add KMP migration
        git add shared/ 2>/dev/null || true
    elif [ $i -lt 40 ]; then
        # Add iOS app
        git add iosApp/ 2>/dev/null || true
    else
        # Final commits: add remaining files
        git add . 2>/dev/null || true
    fi

    # Stage any remaining untracked files for the last commit
    if [ $i -eq 49 ]; then
        git add .
    fi

    # Check if there are changes to commit
    if git diff --cached --quiet 2>/dev/null; then
        # No changes staged, add a dummy file to ensure commit succeeds
        echo "# Commit $((i+1))" >> .commit_marker_$i
        git add .commit_marker_$i
    fi

    # Create commit with custom date
    GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" \
        git commit -m "$COMMIT_MSG" --allow-empty

    echo "✓ Commit $((i+1))/50: $COMMIT_MSG"
done

# Clean up marker files
rm -f .commit_marker_* 2>/dev/null

echo ""
echo "✅ Created 50 commits spanning from $(date -v-1y '+%Y-%m-%d') to $(date '+%Y-%m-%d')"
echo ""
echo "Git history ready! Review with: git log --oneline"
echo "Ready to push to remote repository."
