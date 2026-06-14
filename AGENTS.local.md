# Local agent notes (not for upstream)

## Build + install loop

The development device is reachable over Tailscale at
`oneplus.ts.nowaker.net:5555` (adb over TCP).

**Every build must automatically do BOTH of the following:**

1. **Install on the phone over adb:**

   ```sh
   adb connect oneplus.ts.nowaker.net:5555
   adb -s oneplus.ts.nowaker.net:5555 install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Archive the APK to the synced ownCloud apks folder**, named by
   `versionName` (mirrors the convention of the other projects there):

   ```sh
   cp app/build/outputs/apk/debug/app-debug.apk \
      ~/sync/owncloud/documents/apks/send-reduced/send-reduced-debug-v<versionName>.apk
   ```

   `<versionName>` comes from `app/build.gradle`; bump it to keep distinct
   archived builds. That directory syncs to ownCloud automatically.

Verify the installed build:

```sh
adb -s oneplus.ts.nowaker.net:5555 shell dumpsys package net.nowaker.sendreduced | grep versionName
```

## Toolchain notes

- The system default JDK is too new for Gradle 8.7. Build with a JDK 17–21,
  e.g. a portable Temurin 21 in `~/.local/java/`:
  `JAVA_HOME=~/.local/java/jdk-21.* ./gradlew :app:assembleDebug`
- Android SDK lives at `/opt/android-sdk` (set in `local.properties`).
