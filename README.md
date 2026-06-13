# Send Reduced (dev)

An Android app that sits in the system **Share** sheet and shrinks / converts
photos before they go out. Share an image to **Send Reduced**, it re-encodes
the photo according to a **profile**, then hands the result to the app you
actually want to send it with.

This is a hard fork of the original **Send Reduced** by Omega Centauri Software
(package `mobi.omegacentauri.SendReduced`), which is no longer maintained. The
fork modernizes the build, adds user-defined profiles, and follows the system
light/dark theme.

- **Canonical source (GitLab):** https://gitlab.com/Nowaker/send-reduced-advanced-android
- **GitHub mirror:** https://github.com/Nowaker/sendreduced-android
- **Package:** `net.nowaker.sendreduced`

## Why this fork

The original always strips metadata and downscales to a fixed size. The driving
use case here is the opposite: convert **HEIC → JPG** at full resolution with
**no stripping and no downscaling**. Profiles make both behaviors (and anything
in between) first-class.

## Features

- **Profiles** — named presets of reduction settings:
  - Output format: **JPEG** or **PNG**
  - JPEG quality (10–100)
  - Max resolution (longest side), or **keep original resolution**
  - **Preserve metadata** (EXIF / GPS / capture date) or strip it for privacy
- **Profile picker on share** — when two or more profiles exist, sharing a photo
  first asks which profile to use, then continues to the normal share target.
  With a single profile, it is applied automatically and the picker is skipped.
- **Light / dark mode** — follows the system theme automatically (Material 3
  DayNight).
- **Single or multiple images** — works with `Share` and `Share to multiple`.
- **Get reduced photo** — other apps can pull a reduced image via
  `GET_CONTENT` / `PICK`.

## How it works

1. Share one or more photos and choose **Send Reduced (dev)**.
2. If you have multiple profiles, pick one.
3. The app re-encodes each image into its private cache and opens a share sheet
   with the reduced copies. Pick the destination app as usual.

Manage profiles by opening the app from the launcher: tap a profile to edit it,
or use the **+** button to add one.

## Build

Requirements:

- JDK 17 or newer (the project targets Java 17; built and tested with Temurin 21)
- Android SDK with platform 34 and build-tools 34.0.0
- A `local.properties` pointing at your SDK, e.g. `sdk.dir=/opt/android-sdk`

Build a debug APK:

```sh
./gradlew :app:assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

Install on a connected device:

```sh
./gradlew :app:installDebug
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Toolchain

- Gradle 8.7, Android Gradle Plugin 8.5.2
- `compileSdk` / `targetSdk` 34, `minSdk` 21
- AndroidX: AppCompat, Material 3, ConstraintLayout, ExifInterface

## License

Original code Copyright (C) 2011–2016 Omega Centauri Software, plus
`FileProvider` material Copyright (C) The Android Open Source Project / Google,
licensed under the Apache License 2.0. Fork changes are released under the same
license. See [`license.txt`](license.txt).
