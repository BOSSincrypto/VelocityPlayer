# VelocityPlayer

A small, privacy-first Android video player built for responsive local playback.

[![Android CI](https://github.com/BOSSincrypto/VelocityPlayer/actions/workflows/ci.yml/badge.svg)](https://github.com/BOSSincrypto/VelocityPlayer/actions/workflows/ci.yml)

## Capabilities

- Local video selection through Android's Storage Access Framework
- Opens `video/*` links shared by other apps
- One service-owned Media3/ExoPlayer instance and system media session
- Global playback speed from 0.25× to 4×, persisted between sessions
- Player seek bar plus ±10-second controls
- Fit/fill scaling, fullscreen and manual/automatic Picture-in-Picture
- English and Russian resources
- No network permission, analytics, ads, account or background library scan

## Performance design

VelocityPlayer uses Media3 1.11.0, the platform hardware-decoder path and a `SurfaceView`. The Activity connects to the single player in `PlaybackService` through `MediaController`; rotation, fullscreen and PiP do not create a second player. Fast seek uses `CLOSEST_SYNC`, so the selected frame can differ from the exact requested timestamp depending on keyframes and the container index.

The project intentionally keeps Media3's measured default buffering policy. It does not claim zero latency: startup, seek time, dropped frames, memory and battery depend on the device, codec, GOP, storage and source file. Real performance claims require Macrobenchmark/Perfetto measurements on physical low-, mid- and high-end devices.

## Requirements and build

- Android 8.0 / API 26 or newer
- JDK 17
- Android SDK 36

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

On Windows use `gradlew.bat`. The installable test APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

```text
MainActivity + PlayerView (SurfaceView)
             │ MediaController
             ▼
PlaybackService → MediaSession → ExoPlayer
```

The service is not exported. Local files are accessed through content URIs; persistable read permission is requested when the source supports it.

## CI and releases

- CI runs unit tests, Android Lint and debug assembly on pushes and pull requests.
- Every trusted push/merge to `main` rebuilds and creates a GitHub prerelease.
- The prerelease debug APK is installable for testing and debug-signed.
- The release APK is R8/resource-shrunk but **unsigned** and clearly named accordingly. It is not a production or Google Play release.

## Verification boundaries

Unit tests cover speed normalization, seek clamping and automatic-PiP policy. CI/build success does not prove playback, PiP transitions, codec support or visual quality on a phone. Those require emulator or physical-device instrumentation and media fixtures.

## License

[MIT](LICENSE)
