# DrivePlay V0.2 — USB Media Library

## Included
- Kotlin + Jetpack Compose landscape interface
- USB folder selection through Android Storage Access Framework
- Persisted read permission for the selected folder
- Recursive scan of supported video formats (MP4, MKV, AVI, MOV, WEBM, 3GP, M4V)
- Video grid with generated frame thumbnails
- Media3 / ExoPlayer playback
- Local playlists stored on device
- YouTube and Instagram official-app/web handoff buttons

## Open and run
1. Extract the ZIP.
2. Open the folder in Android Studio.
3. Install Android SDK Platform 36 and allow Gradle sync.
4. Run on an Android device/emulator.
5. Connect a USB drive using a USB-C OTG adapter/hub if needed.
6. In DrivePlay, select the USB folder and grant access.

## USB caveats
- USB host/OTG support, filesystem support and power delivery depend on the phone and adapter.
- A USB-C hub with external power may be needed for some drives.
- Android's folder picker controls which folder DrivePlay can read.
- If the USB drive is unplugged, files become unavailable until it is reconnected and the library is refreshed.
- Playlist entries store document URIs. If the USB is reformatted or the folder permissions are revoked, those entries may need to be recreated.

## Online integration
The Online tab opens official YouTube and Instagram experiences. Embedded playback is not claimed in this version: provider policies, login and playback restrictions must be respected. No scraping or unofficial downloading is implemented.

## Safety / vehicle integration
This is a standalone Android app, not an Android Auto video app. Use video playback only while safely parked. Do not bypass vehicle safety interlocks. An Android AI box purchase remains on hold until the Slavia's exact head-unit wired USB/CarPlay compatibility is confirmed.

## Version
0.2.0 — USB library, thumbnails, playlists and official online-service handoff.
