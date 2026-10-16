# Backtalk

Backtalk is a fork of [Google's TalkBack](https://github.com/google/talkback), the screen reader for blind and visually-impaired users of Android. It adds fixes and features on top of Google's source releases. It is not affiliated with Google.

## This fork

This is Aaron's fork of [Backtalk](https://github.com/trypsynth/backtalk). It has Backtalk's changes, plus Aaron's that are not merged into Backtalk yet. Each of those is also a pull request to Backtalk.

The fork installs as **Backtalk Aaron**, with the app ID `io.github.aaron_gh.backtalk`, so it installs next to Backtalk instead of replacing it. The two apps have separate settings. Its development builds are on the fork's [dev release](https://github.com/aaron-gh/backtalk/releases/tag/dev) page. Its updater checks there about once an hour instead of once a day, so that each push reaches the phone soon. The rest of this readme describes Backtalk.

## Goals

*   Make Backtalk faster and more responsive for people who use their phone quickly.
*   Bring back useful behavior from older TalkBack and other screen readers.
*   Keep the build working on Windows, Linux, and macOS.
*   Move the code to Kotlin over time. New code is written in Kotlin. Google's Java files are converted only when they already need large changes, so that new Google releases stay easy to merge.
*   Stay close to Google's releases, and merge each new one.

## Differences from TalkBack

Backtalk speaks sooner after each swipe and screen change, and can play its sounds and speech through a low-latency audio path. It adds a two-finger rotor, a circle menu, Pause Backtalk, direct touch for audio games, sound themes with control sounds in 3D, a vibration for every sound, a status gesture, and braille keyboard dots that follow how you hold the device. It also changes some defaults and fixes problems in TalkBack. For the full list, see [Differences from TalkBack](differences.md).

## Third-party data

The emoji names are from the [Unicode CLDR](https://cldr.unicode.org/) and the Unicode emoji data, copyright Unicode, Inc., used under the Unicode License v3, which is in `utils/src/main/assets/emoji_names/LICENSE`.

Scripts run in [QuickJS-NG](https://github.com/quickjs-ng/quickjs), copyright Fabrice Bellard, Charlie Gordon, Ben Noordhuis and Saúl Ibarra Corretgé, used under the MIT License, which is in `scripting/quickjs/src/main/assets/quickjs/LICENSE`. The build downloads its source, pinned by version and checksum in `scripting/quickjs/src/main/cpp/CMakeLists.txt`.

## Build

You need JDK 17 or newer, the Android SDK with platform 37, and NDK 27.3.13750724. The Gradle wrapper downloads the correct Gradle version, so you do not need to install Gradle.

### Linux or macOS

Set `ANDROID_SDK` to your SDK path, then run `./build.sh`. This builds the phone and watch APKs, and keeps anything else that's in `local.properties`, such as a Gemini API key.

### Windows

Set `ANDROID_HOME` to your SDK path, then run:

```
.\gradlew.bat assemblePhoneDebug
```

### Dependencies

Library and plugin versions are in `gradle/libs.versions.toml`. Renovate opens pull requests to update them each week, and GitHub Actions builds each pull request.

### Image descriptions with Gemini

Google's source release does not include the Gemini settings, so **Describe image** only reads text in images, and **Describe screen** does not work, unless you add your own Gemini API key. Backtalk adds its own support for Describe screen, including follow-up questions. To add a key:

1.  Open Backtalk settings, then **Automatic descriptions**, then **Gemini API key**.
2.  Choose **Get a key** to open [Google AI Studio](https://aistudio.google.com/apikey) and create a key.
3.  Paste the key into the field, and choose **Save**. It works at once.

To remove the key, clear the field and save.

If you build Backtalk yourself, you can also build a key into the APK instead. Add `gemini.api.key=YOUR_KEY` to `local.properties` in the project folder, then build and install Backtalk again. A key entered in settings takes the place of the built-in key. Git ignores `local.properties`, so your key is not committed, but do not share an APK that contains your key. To use a different model, add `gemini.model=MODEL_NAME`. The default is `gemini-flash-latest`.

Images and screenshots that you describe are sent to Google. On the free tier, Google can use this data to improve its products.

### On-device AI

If you do not want to use an API key, or you have hit the free tier limit, Backtalk can describe images and screens with a Gemma 4 model that runs on your phone. Nothing is sent to Google or anyone else when you use it.

1.  Open Backtalk settings, then **Automatic descriptions**, then **On-device AI**.
2.  Choose a model. **Gemma 4 E2B** is the one to start with: 2.6 GB, and it needs a phone with about 6 GB of memory. **Gemma 4 E4B** is 3.7 GB, gives better answers, and needs about 8 GB. The list also has other small vision models from the [LiteRT community](https://huggingface.co/litert-community), from 0.4 GB up, so that you can try them. Those are marked experimental: they are community conversions that the Backtalk developers have not tried, and some may not work or may follow the screen description format badly. The list only shows models that your phone has enough memory for, plus any you already have. If your phone does not have enough memory for Gemma 4 E2B, Backtalk picks the largest model that fits.
3.  Choose **Download model**. It downloads once from [Hugging Face](https://huggingface.co/litert-community) and carries on where it stopped if the connection drops. A notification shows the progress. Backtalk checks the file against a known SHA-256 hash and deletes it if it does not match. Or choose **Use a model file from storage** to use a `.litertlm` file that you downloaded yourself, such as `gemma-4-E2B-it.litertlm` from `litert-community/gemma-4-E2B-it-litert-lm`. Backtalk works out which model it is.
4.  Turn on **Use on-device AI**.

You still need to turn on Gemini support in the Gemini settings, which switches on Describe image and Describe screen. After that, they use the model on your phone. Turn **Use on-device AI** off to go back to the Gemini API.

Answers take several seconds and use battery, more than the cloud on a mid-range phone. The model loads on the first request and unloads after two idle minutes to free memory. The model runs in its own process, so if the phone runs out of memory, Android stops the model and not Backtalk, and Backtalk says so. Before it loads a model, Backtalk checks that the phone has at least as much free memory as the model's size, and if not, it says there is not enough free memory and does not load it. To test with a model that is too big for your phone, turn on **Ignore on-device AI memory limits** in **Advanced settings** > **Developer settings**. It lists every model, lets you choose any downloaded one, and skips the free memory check. If answers fail or the phone slows down, try the other model, or turn **Use the GPU** on or off. On-device AI needs a 64-bit ARM phone and adds about 22 MB to the app.

## Install

Install the APK on your device with adb.

On Windows, `.\deploy.ps1` builds the APK and installs it with adb. This script needs PowerShell 7. To install the last build without building again, use `-SkipBuild`.

Backtalk installs as `fyi.quin.backtalk`, so it does not replace Google's TalkBack. The two apps have separate settings. Because its app ID is its own, Backtalk also installs on GrapheneOS and other ROMs that ship the AOSP TalkBack as a system app named `com.android.talkback`.

### Moving from the old app ID

Earlier builds of Backtalk installed as `com.android.talkback`. To move to the new app ID:

1.  Update as usual. The update installs the new Backtalk next to the old one, and the accessibility settings open.
2.  Turn on the new Backtalk when asked. Your settings and custom labels come along.
3.  The old Backtalk turns itself off, and a notification asks to remove it. Tap it to uninstall the old app.

On-device AI models are not carried over, so download them again in the new app. If you used the braille keyboard or an accessibility shortcut, turn them on again for the new Backtalk.

To make the switch fully automatic, grant the new app permission to change secure settings before you turn it on: `adb shell pm grant fyi.quin.backtalk android.permission.WRITE_SECURE_SETTINGS`. Then the new Backtalk turns the old one off, and moves the accessibility shortcut and the braille keyboard over, by itself.

## Updates

Each change to Backtalk is built on GitHub as a development build, on the [dev release](https://github.com/trypsynth/backtalk/releases/tag/dev) page. `backtalk.apk` is for phones and `backtalk-wear.apk` is for Wear OS watches, and each checks for updates of its own kind. The [latest release](https://github.com/trypsynth/backtalk/releases/tag/latest) holds the last build with the old app ID, which moves old installs to the new one. Backtalk checks for a new build when it starts and about once a day. When there is one, it shows a notification with the list of changes. Tap the notification to download and install the new build. The first time, Android asks you to allow Backtalk to install apps.

To check now, go to **Check for updates** in Backtalk settings. To stop the daily checks, turn off **Automatically check for updates**.

Android only installs an update that is signed with the same key as the installed app. If you build Backtalk yourself, your build is signed with your own debug key, so it cannot be updated by the builds from GitHub. To use them, uninstall your build first.

## Run

After you install Backtalk, go to **Settings > Accessibility**. Backtalk is listed as **Backtalk** and is off by default. Turn off Google's TalkBack first, then turn on Backtalk.

## Android TV

Backtalk includes the Android TV support from Google's TalkBack, and the same `backtalk.apk` installs on Android TV and Google TV. It has been tried on an Android 10 TV emulator, but not yet on a real TV.

1.  Turn on developer options and network debugging on the TV, then connect to it with `adb connect` and the TV's IP address.
2.  Install the APK with `adb install backtalk.apk`.
3.  Turn on Backtalk in the TV's accessibility settings. On most Android TVs, this is **Settings** > **Device Preferences** > **Accessibility**.

To open Backtalk settings, choose **Backtalk settings** in the apps on the TV home screen. TVs often don't show notifications, so to update, go to **Check for updates** in Backtalk settings. Pause Backtalk isn't available on TVs, because a TV remote often has no way to resume it.

## Debug tools

Debug builds include tools to find lag:

*   The `BacktalkStall` logcat tag logs each time the main thread is blocked for more than 100 ms, with the code that was running.
*   The `BacktalkGesture` logcat tag logs touch state changes and each gesture that Backtalk detects.
*   Event processing has trace sections, which show in [Perfetto](https://perfetto.dev) system traces.
