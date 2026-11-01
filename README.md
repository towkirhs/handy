# Handy for Android

Talk instead of type, in any app. Handy listens, transcribes **entirely on your phone**, cleans the text up, and drops it into whatever field you're typing in.

Inspired by the [Handy](https://github.com/cjpais/Handy) desktop app (Whisper-based dictation). Handy itself is a Tauri desktop app with no Android build, so this is a native Kotlin app that follows the same idea.

## How it works

- **Floating bar.** A small bubble appears over any app when you tap a text field. Tap it to start and stop, or hold it to talk and release to finish. While listening it expands into a pill with a live waveform, a cancel button and a done button. Drag it anywhere.
- **On-device speech to text.** [whisper.cpp](https://github.com/ggml-org/whisper.cpp) runs the Whisper `base.en` model, which is bundled inside the APK. Dictation needs no internet.
- **Clean-up before it's inserted.** Removes filler words (um, uh), stutters ("I I think"), tags like `[BLANK_AUDIO]`, handles "scratch that" and "new line", and fixes spacing and capitals.
- **Optional AI polish.** In Settings you can point Handy at an OpenAI-compatible endpoint. Only the transcribed text is sent, never audio. Off by default; if it fails, you get the offline result.
- **Voice keyboard.** Handy also ships a small keyboard with the same dictation bar, for places where the floating bar can't appear.

## Install

Every push to `main` builds the APK in GitHub Actions and publishes it as a GitHub Release (named "Handy build N"). Download `handy.apk` from the latest entry under **Releases**. Tagging a commit `v*` publishes a release with that tag name instead.

Releases are only published when these repository secrets are set (Settings, Secrets and variables, Actions), so that every build is signed with the same key and installs over the previous one: `KEYSTORE_BASE64` (the keystore file, base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. You can also start a build by hand from the Actions tab with **Run workflow**.

The APK is signed with a debug key, so Android will ask you to allow installs from your browser or file manager.

## First run

Handy walks you through three permissions:

1. **Microphone**.
2. **Show over other apps**, so the bar can float.
3. **Accessibility**, so Handy knows when you're in a text field and can insert text. On Android 13+, if the switch is greyed out, open App info, then the three-dot menu, then **Allow restricted settings**.

Then turn on **Floating bar** on the home screen and try it in the "Try it" box.

## Build it yourself

Requires JDK 17 and the Android SDK.

```bash
./scripts/setup.sh                      # fetches whisper.cpp and the model (about 60 MB)
./gradlew :app:testDebugUnitTest        # clean-up rules
./gradlew :app:assembleRelease          # app/build/outputs/apk/release/app-release.apk
```

## Known limits

- English only for now (`base.en` model). A multilingual model would be a larger APK and slower.
- The first dictation after the app starts loads the model, which takes a second or two.
- Text is inserted by pasting, so your clipboard will hold the last dictation.
- The floating bar can't appear over password fields, by design.
- Some devices stop background apps aggressively. If the bar disappears, reopen Handy and switch it back on.
- Android may refuse to restart the bar after a reboot until you open the app once.

## Credits

The JNI wrapper in `whisper/` is adapted from the Android example in [whisper.cpp](https://github.com/ggml-org/whisper.cpp) (MIT, Georgi Gerganov and contributors). Speech models are OpenAI Whisper converted by the whisper.cpp project.

## Project layout

- `app/`: the app (Compose UI, overlay service, accessibility service, keyboard, dictation pipeline)
- `whisper/`: JNI wrapper that builds whisper.cpp with the NDK
- `scripts/setup.sh`: fetches whisper.cpp and the model
- `.github/workflows/build.yml`: CI that tests and builds the APK
