# SandFly

SandFly is a screen reader for Android, for blind and visually-impaired users. It is a fork of [Backtalk](https://github.com/trypsynth/backtalk), which is a fork of [Google's TalkBack](https://github.com/google/talkback). It is not affiliated with Google or with Backtalk.

## History and license

SandFly began as Aaron's fork of Backtalk, Backtalk Aaron, and became a separate project when it had many changes that are not in Backtalk. The git history keeps every change from TalkBack, Backtalk and SandFly. Files from TalkBack and Backtalk are changed in SandFly.

SandFly is under the Apache License 2.0, as TalkBack and Backtalk are. See [LICENSE](LICENSE).

## Goals

*   Make SandFly faster and more responsive for people who use their phone quickly.
*   Bring back useful behavior from older TalkBack and other screen readers.
*   Keep the build working on Windows, Linux, and macOS.
*   Move the code to Kotlin over time. New code is written in Kotlin.
*   Take fixes from Backtalk and from Google's releases where they apply.

## Differences from TalkBack

SandFly speaks sooner after each swipe and screen change, and can play its sounds and speech through a low-latency audio path. It adds a two-finger rotor, edge gestures and a two-finger scrub as in VoiceOver, a circle menu, Pause SandFly, direct touch for audio games, sound themes with control sounds in 3D, a vibration for every sound, a status gesture, and braille keyboard dots that follow how you hold the device. It also changes some defaults and fixes problems in TalkBack. For the full list, see [Differences from TalkBack](differences.md).

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

Library and plugin versions are in `gradle/libs.versions.toml`. GitHub Actions builds each pull request.

### Image descriptions with Gemini

Google's source release does not include the Gemini settings, so **Describe image** only reads text in images, and **Describe screen** does not work, unless you add your own Gemini API key. SandFly adds its own support for Describe screen, including follow-up questions. To add a key:

1.  Open SandFly settings, then **Automatic descriptions**, then **Gemini API key**.
2.  Choose **Get a key** to open [Google AI Studio](https://aistudio.google.com/apikey) and create a key.
3.  Paste the key into the field, and choose **Save**. It works at once.

To remove the key, clear the field and save.

If you build SandFly yourself, you can also build a key into the APK instead. Add `gemini.api.key=YOUR_KEY` to `local.properties` in the project folder, then build and install SandFly again. A key entered in settings takes the place of the built-in key. Git ignores `local.properties`, so your key is not committed, but do not share an APK that contains your key. To use a different model, add `gemini.model=MODEL_NAME`. The default is `gemini-flash-latest`.

Images and screenshots that you describe are sent to Google. On the free tier, Google can use this data to improve its products.

### On-device AI

If you do not want to use an API key, or you have hit the free tier limit, SandFly can describe images and screens with a Gemma 4 model that runs on your phone. Nothing is sent to Google or anyone else when you use it.

1.  Open SandFly settings, then **Automatic descriptions**, then **On-device AI**.
2.  Choose a model. **Gemma 4 E2B** is the one to start with: 2.6 GB, and it needs a phone with about 6 GB of memory. **Gemma 4 E4B** is 3.7 GB, gives better answers, and needs about 8 GB. The list also has other small vision models from the [LiteRT community](https://huggingface.co/litert-community), from 0.4 GB up, so that you can try them. Those are marked experimental: they are community conversions that the SandFly developers have not tried, and some may not work or may follow the screen description format badly. The list only shows models that your phone has enough memory for, plus any you already have. If your phone does not have enough memory for Gemma 4 E2B, SandFly picks the largest model that fits.
3.  Choose **Download model**. It downloads once from [Hugging Face](https://huggingface.co/litert-community) and carries on where it stopped if the connection drops. A notification shows the progress. SandFly checks the file against a known SHA-256 hash and deletes it if it does not match. Or choose **Use a model file from storage** to use a `.litertlm` file that you downloaded yourself, such as `gemma-4-E2B-it.litertlm` from `litert-community/gemma-4-E2B-it-litert-lm`. SandFly works out which model it is.
4.  Turn on **Use on-device AI**.

You still need to turn on Gemini support in the Gemini settings, which switches on Describe image and Describe screen. After that, they use the model on your phone. Turn **Use on-device AI** off to go back to the Gemini API.

Answers take several seconds and use battery, more than the cloud on a mid-range phone. The model loads on the first request and unloads after two idle minutes to free memory. The model runs in its own process, so if the phone runs out of memory, Android stops the model and not SandFly, and SandFly says so. Before it loads a model, SandFly checks that the phone has at least as much free memory as the model's size, and if not, it says there is not enough free memory and does not load it. To test with a model that is too big for your phone, turn on **Ignore on-device AI memory limits** in **Advanced settings** > **Developer settings**. It lists every model, lets you choose any downloaded one, and skips the free memory check. If answers fail or the phone slows down, try the other model, or turn **Use the GPU** on or off. On-device AI needs a 64-bit ARM phone and adds about 22 MB to the app.

## Install

Download `sandfly.apk` from the [latest release](https://github.com/aaron-gh/SandFly/releases/latest) and open it on the phone. For a Wear OS watch, install `sandfly-wear.apk` with adb.

On Windows, `.\deploy.ps1` builds the APK and installs it with adb. This script needs PowerShell 7. To install the last build without building again, use `-SkipBuild`.

SandFly installs as `io.github.aaron_gh.sandfly`, so it does not replace Google's TalkBack or Backtalk. Each app has its own settings.

### Moving from Backtalk Aaron

The last build of Backtalk Aaron offers SandFly as an update. To move:

1.  Install SandFly from the notification, or from **Check for updates**. It installs next to Backtalk Aaron, and the accessibility settings open.
2.  Turn on SandFly. The settings and custom labels of Backtalk Aaron are copied to it, and **Development builds** is on.
3.  Backtalk Aaron turns itself off, and a notification offers to uninstall it.

On-device AI models are not copied, so download them again in SandFly. If you used the braille keyboard or an accessibility shortcut, turn them on again for SandFly.

To move the accessibility shortcut and the braille keyboard as well, grant SandFly permission to change secure settings before you turn it on: `adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS`.

## Updates

SandFly has stable releases and development builds, both built on GitHub:

*   Stable releases are on the [releases](https://github.com/aaron-gh/SandFly/releases) page, named by version, such as SandFly 0.1.0.
*   Development builds are on the [dev release](https://github.com/aaron-gh/SandFly/releases/tag/dev) page, which is replaced by each change.

`sandfly.apk` is for phones and `sandfly-wear.apk` is for Wear OS watches, and each checks for updates of its own kind. SandFly checks for a new stable release when it starts and about once a day. With **Development builds** on, it also checks for development builds, about once an hour, and offers whichever is newer, so a development build is offered the next stable release. When there is an update, SandFly shows a notification with the list of changes. Tap the notification to download and install it. The first time, Android asks you to allow SandFly to install apps.

To check now, go to **Check for updates** in SandFly settings, which also shows the version and build number. To stop automatic checks, turn off **Automatically check for updates**.

Android only installs an update that is signed with the same key as the installed app. If you build SandFly yourself without SandFly's key, your build is signed with your own debug key, so it cannot be updated by the builds from GitHub. To use them, uninstall your build first.

### Making a release

1.  Set `sandflyVersion` in `version.gradle` to the new version, such as `0.2.0`, and push to master.
2.  Tag the commit `v0.2.0` and push the tag. GitHub Actions builds it and creates the release, with the changes since the last release.

## Run

After you install SandFly, go to **Settings > Accessibility**. SandFly is listed as **SandFly** and is off by default. Turn off Google's TalkBack first, then turn on SandFly.

## Android TV

SandFly includes the Android TV support from Google's TalkBack, and the same `sandfly.apk` installs on Android TV and Google TV. It has been tried on an Android 10 TV emulator, but not yet on a real TV.

1.  Turn on developer options and network debugging on the TV, then connect to it with `adb connect` and the TV's IP address.
2.  Install the APK with `adb install sandfly.apk`.
3.  Turn on SandFly in the TV's accessibility settings. On most Android TVs, this is **Settings** > **Device Preferences** > **Accessibility**.

To open SandFly settings, choose **SandFly settings** in the apps on the TV home screen. TVs often don't show notifications, so to update, go to **Check for updates** in SandFly settings. Pause SandFly isn't available on TVs, because a TV remote often has no way to resume it.

## Debug tools

Debug builds include tools to find lag:

*   The `BacktalkStall` logcat tag logs each time the main thread is blocked for more than 100 ms, with the code that was running.
*   The `BacktalkGesture` logcat tag logs touch state changes and each gesture that SandFly detects.
*   Event processing has trace sections, which show in [Perfetto](https://perfetto.dev) system traces.
