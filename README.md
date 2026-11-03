# OMNIServers for Android

Remote control for OMNIServers (Mac today; Windows/Linux when their Remote API lands), written in Kotlin + Jetpack Compose with a custom OMNI design system (no Material components).

- Pair with the pairing code from the desktop app (Settings → Remote API), or try the built-in demo.
- Servers: start/stop, live status. Detail: overview ring, console with quick commands, players (op/kick).
- Build: `./gradlew :app:assembleDebug` (JDK 21; Android SDK path in `local.properties`).
- Tests: `./gradlew :app:testDebugUnitTest`.
- Space Grotesk font: SIL OFL, see `licenses/SpaceGrotesk-OFL.txt`.
- Not yet: QR pairing, host-a-server-on-the-phone mode, release signing.

## Android hosting spike

The first runtime check was performed on the arm64 emulator on 2026-11-03. The
bundled OpenJDK 21 runtime starts from an ADB shell when
`LD_LIBRARY_PATH=<runtime>/lib` is set, but the application UID cannot execute
`<runtime>/bin/java` from `/data/local/tmp`, either directly or through
`/system/bin/sh` (`Permission denied`). This confirms Java works on the device,
but does not yet prove that an Android app can launch it.

The next implementation step is an Android-native launcher that loads the Java
runtime through the app's native-library mechanism; copying an executable into
app storage or importing the Fortinet interception certificate is not an
acceptable workaround.
