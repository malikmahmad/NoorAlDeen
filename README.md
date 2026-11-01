# NoorAlDeen

NoorAlDeen is a focused Android app for reading the Qur'an, exploring tafseer, and
studying the major collections of hadith in one clean, offline-first experience.

## Features

- Qur'an reader with surah browsing and search
- English and Urdu tafseer content
- Hadith collections from the Kutub al-Sittah
- Daily hadith support
- Fast local content access with bundled JSON datasets
- Light and dark themes
- Arabic-friendly reading layout
- Location access for prayer-related features

## Project structure

```text
app/                         Android application
NoorAlDeen/                  Source datasets and content pipeline
  01_Complete_Quran/
  02_Tafseers/
  Kuttub As Sittah/
  content_pipeline/
```

## Requirements

- Android Studio Hedgehog or newer
- Android SDK 34
- JDK 8 or newer

## Build

Open the project in Android Studio, allow Gradle to sync, and run the `app`
configuration on an Android 7.0 (API 24) or newer device or emulator.

To build from the command line:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release APK

The latest APK is published in the repository's GitHub Releases:

https://github.com/malikmahmad/NoorAlDeen/releases

## Content pipeline

The `NoorAlDeen/content_pipeline` directory contains source configuration,
validation schemas, processing scripts, generated datasets, and validation
reports used to prepare the app's Islamic content.

## Permissions

NoorAlDeen requests internet access for supported online content workflows and
coarse/fine location access for location-aware features. The app does not
require an account.

## License

Content and code licensing should be reviewed and documented before distributing
production builds. Please preserve the attribution and licensing information
associated with each included dataset.
