# OMNIServers for Android

Remote control for OMNIServers (Mac today; Windows/Linux when their Remote API lands), written in Kotlin + Jetpack Compose with a custom OMNI design system (no Material components).

- Pair with the pairing code from the desktop app (Settings → Remote API), or try the built-in demo.
- Servers: start/stop, live status. Detail: overview ring, console with quick commands, players (op/kick).
- Build: `./gradlew :app:assembleDebug` (JDK 21; Android SDK path in `local.properties`).
- Tests: `./gradlew :app:testDebugUnitTest`.
- Space Grotesk font: SIL OFL, see `licenses/SpaceGrotesk-OFL.txt`.
- Not yet: QR pairing, host-a-server-on-the-phone mode, release signing.
