## Cursor Cloud specific instructions

This is a native Android (Kotlin) app called **CallMind** — an AI-powered call recording analyzer using Google Gemini. There is no backend or Docker dependency; all cloud processing goes through the Gemini API at runtime on the device.

### Prerequisites (already installed in snapshot)

- JDK 21 (`/usr/lib/jvm/java-21-openjdk-amd64`)
- Android SDK at `~/android-sdk` (platforms android-36, android-36.1; build-tools 36.0.0, 36.1.0)
- `JAVA_HOME` and `ANDROID_HOME` are set in `~/.bashrc`
- `local.properties` points `sdk.dir` to `~/android-sdk` — this file is gitignored and recreated by the update script

### Common commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Run lint | `./gradlew lint` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Full check | `./gradlew check` |

### Gotchas

- The project uses AGP 9.1.0 with `compileSdk { version = release(36) { minorApiLevel = 1 } }`, which requires `platforms;android-36.1` in the SDK.
- Gradle auto-provisions JDK 21 via the Foojay toolchain resolver if the system JDK doesn't match, but having JDK 21 pre-installed avoids the download.
- No Android Emulator is available in this environment, so instrumented/UI tests (`connectedDebugAndroidTest`) cannot run. Only local unit tests (`testDebugUnitTest`) and lint are executable.
- Kotlin compiler warnings about `@Inject` annotation targets (KT-73255) are expected and not errors.
