# Local agent notes (not for upstream)

## Build + install loop

The development device is reachable over Tailscale at
`oneplus.ts.nowaker.net:5555` (adb over TCP).

Once the app is built, **always attempt to install it via adb on that device**:

```sh
adb connect oneplus.ts.nowaker.net:5555
./gradlew :app:installDebug
# or, after a manual assemble:
adb -s oneplus.ts.nowaker.net:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

Verify the installed build:

```sh
adb -s oneplus.ts.nowaker.net:5555 shell dumpsys package net.nowaker.sendreduced | grep versionName
```

## Toolchain notes

- The system default JDK is too new for Gradle 8.7. Build with a JDK 17–21,
  e.g. a portable Temurin 21 in `~/.local/java/`:
  `JAVA_HOME=~/.local/java/jdk-21.* ./gradlew :app:assembleDebug`
- Android SDK lives at `/opt/android-sdk` (set in `local.properties`).
