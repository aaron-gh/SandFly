# SandFly

SandFly is a screen reader for Android. It reads the screen with text-to-speech, plays sounds and vibrations, and changes how touch gestures work, so that you can use an Android device without seeing the screen. It also works with braille displays, an on-screen braille keyboard and physical keyboards.

SandFly runs on phones and tablets with Android 8 or later, on Wear OS watches, and on Android TV and Google TV. It installs as a separate app, `io.github.aaron_gh.sandfly`, with its own settings, next to any other screen reader.

SandFly started as a fork of [Backtalk](https://github.com/trypsynth/backtalk), which is a fork of [Google TalkBack](https://github.com/google/talkback). It is not affiliated with Google or with Backtalk. For what SandFly does differently from TalkBack, see [Differences from TalkBack](differences.md).

- [Features](#features)
- [Install SandFly](#install-sandfly)
- [Turn on SandFly](#turn-on-sandfly)
- [Learn SandFly](#learn-sandfly)
- [Updates](#updates)
- [SandFly Pro](#sandfly-pro)
- [Image descriptions with your own AI service](#image-descriptions-with-your-own-ai-service)
- [Permissions through Shizuku, root or adb](#permissions-through-shizuku-root-or-adb)
- [Report a problem](#report-a-problem)
- [Languages](#languages)
- [For developers](#for-developers)
- [Move from Backtalk Aaron](#move-from-backtalk-aaron)
- [Build SandFly](#build-sandfly)
- [History, license and third-party components](#history-license-and-third-party-components)

## Features

Each item links to the chapter of the user guide that describes it.

- **Faster speech.** SandFly reads the next item sooner after a swipe, a double-tap or a screen change, prepares the next swipe in advance, and can play speech and sounds through a low-latency audio path. See [Differences from TalkBack](differences.md).
- **Gestures.** One- to four-finger swipes and taps, back-and-forth and angle gestures, three-part and two-stroke swipes, swipe and hold, a two-finger rotor, edge gestures and a two-finger scrub, corner gestures, pinch and spread, back tap and shake. Choose the SandFly gesture layout or one that matches VoiceOver on iPhone, and set gestures for each app or in gesture profiles. See [Gestures](docs/user-guide/en/03-gestures.md).
- **Reading and navigation.** Reading controls for characters, words, lines, headings, links, controls and more; continuous reading; screen search; tables; containers and windows; addresses and numbers in text. See [Explore and read the screen](docs/user-guide/en/02-reading.md) and [Web pages and elements](docs/user-guide/en/13-web.md).
- **Commands and menus.** Every command can have gestures and keyboard shortcuts. The SandFly menu, a circle menu, custom menus, macros that record and repeat steps, and voice commands. See [Commands and menus](docs/user-guide/en/05-commands-and-menus.md), [Macros](docs/user-guide/en/16-macros.md) and [Voice commands](docs/user-guide/en/15-voice-commands.md).
- **Speech.** Voice profiles, voices by language, voice effects with an equalizer and compression, a speech dictionary and a symbol dictionary, emoji settings, and speech history. See [Speech](docs/user-guide/en/06-speech.md) and [Verbosity](docs/user-guide/en/07-verbosity.md).
- **Sound and vibration.** Sound themes, control sounds in 3D audio, a vibration for every sound, and a choice of audio output device. See [Sound, vibration and audio output](docs/user-guide/en/08-sound-and-vibration.md).
- **Typing.** Typing methods for the on-screen keyboard, granular editing, clipboard history, dictation on the phone with Whisper, and a handwriting keyboard. See [Type and edit text](docs/user-guide/en/09-typing.md).
- **Braille.** An on-screen braille keyboard with dots that follow how you hold the device, braille commands and an app launcher, and braille displays over Bluetooth, USB and HID, or from another app. See [Braille](docs/user-guide/en/10-braille.md).
- **Physical keyboards.** SandFly, VoiceOver, NVDA, JAWS and Orca keymaps, custom keymaps, browse mode, object navigation and a review cursor, keymaps for keyboard phones, and the SandFly keyboard input method. See [Physical keyboards](docs/user-guide/en/11-keyboards.md).
- **Notifications.** Rules for which notifications are read and how, by app, kind, time and words; repeats; schedules; and the SandFly notification list. See [Notifications](docs/user-guide/en/14-notifications.md).
- **Image and screen descriptions.** Describe images, the screen and the camera view, and ask questions about them, with SandFly Pro, an AI service with your own key, or a model on the phone. See [Image and screen descriptions](docs/user-guide/en/17-descriptions.md).
- **Translation.** Translate the last phrase, or translate speech and notifications automatically. See [Translation](docs/user-guide/en/19-translation.md).
- **Scripts.** Lua scripts and a script builder change how SandFly reads and works in one app or in every app. See [Scripts and custom labels](docs/user-guide/en/20-scripts.md).
- **Settings profiles and backups.** Sets of changed settings that apply in chosen apps, on web pages, while reading or while editing, and backups of all settings with custom labels, profiles, scripts and sound themes in one file. See [Settings profiles and backups](docs/user-guide/en/22-profiles-and-backup.md).
- **Other tools.** Direct touch for games, touch lock, Pause SandFly, the SandFly overview of recent apps, the SandFly quick settings panel, hide screen, and SandFly as the wallpaper. See [Touch](docs/user-guide/en/04-touch.md), [Display, overview and quick settings](docs/user-guide/en/21-display.md) and [Pause and turn off SandFly](docs/user-guide/en/23-pause-and-turn-off.md).

## Install SandFly

SandFly is published on the [releases page](https://github.com/aaron-gh/SandFly/releases/latest). Each release has two APK files:

- `sandfly.apk` for phones, tablets and TVs.
- `sandfly-wear.apk` for Wear OS watches.

To install SandFly on a phone or tablet:

1. On the phone, open the [latest release](https://github.com/aaron-gh/SandFly/releases/latest) in a browser and download `sandfly.apk`.
2. Open the downloaded file.
3. If Android asks, allow the browser or file manager to install apps, then open the file again.
4. Choose **Install**.

On Android 13 and later, Android can block accessibility services installed from a browser or file manager. If SandFly can't be turned on, open the app info for SandFly in the Android settings, open the **More** menu, and choose **Allow restricted settings**. The names of these items differ on some phones.

To install SandFly on a watch or TV, use adb. See [Watches and TVs](docs/user-guide/en/26-watches-and-tv.md).

## Turn on SandFly

1. Turn off any other screen reader.
2. Open the Android **Settings** app and go to **Accessibility**. On some phones, SandFly is in a section such as **Installed apps** or **Downloaded apps**.
3. Choose **SandFly**, turn on the switch, and confirm.

The first time, SandFly opens the **Choose gestures** dialog, where you choose the SandFly or VoiceOver gesture layout, then the SandFly tutorial. With a physical keyboard connected, it opens the keyboard tutorial instead.

To turn SandFly on and off quickly, set up the accessibility shortcut for SandFly in **Settings** > **Accessibility** > **SandFly**, such as holding both volume keys.

## Learn SandFly

- **SandFly tutorial**: interactive lessons for the basic gestures. It opens the first time, and later from **Help & feedback** > **SandFly tutorial** in SandFly settings.
- **SandFly User Guide**: describes every feature and setting, with appendices that list every default gesture and keyboard shortcut and the developer APIs. It opens from **Help & feedback** > **User guide** in SandFly settings. Its source is in [docs/user-guide](docs/user-guide/en/01-getting-started.md).

To open SandFly settings, open **SandFly** in the apps list, or choose **SandFly settings** in the SandFly menu. **Search** at the top of the settings screen finds any setting or command.

The basic gestures of the SandFly layout:

| Gesture | What it does |
| --- | --- |
| Touch and move one finger | Read the item under the finger |
| Swipe right or left | Next or previous item |
| Double-tap | Activate the item |
| Double-tap and hold | Touch and hold the item |
| Two-finger swipe up or down | Scroll |
| Two-finger tap | Pause or resume speech |
| Three-finger swipe down or up | Next or previous reading control |
| Swipe down or up | Move by the reading control, or change its setting |
| Three-finger tap | SandFly menu |
| Swipe down then left | Back |
| Swipe up then left | Home |
| Swipe right then down | Notifications |

The user guide lists every default gesture, from the settings of SandFly itself.

## Updates

SandFly updates itself from the releases on GitHub. It checks for a new release when it starts and about once a day, and shows a notification with the changes. Choose the notification to download and install the update. The first time, Android asks you to allow SandFly to install apps.

The update settings are in **Updates and installation**:

- **Check for updates** checks now, and shows the version and build number of the installed SandFly.
- **Automatically check for updates** turns the automatic checks on or off.
- **Development builds** also offers development builds, which are built from each change and tested less. SandFly then checks about once an hour and offers whichever build is newer. Development builds are on the [dev release](https://github.com/aaron-gh/SandFly/releases/tag/dev) page.

`sandfly.apk` and `sandfly-wear.apk` each update to builds of their own kind. On a watch, updates download over Wi-Fi when it is available. See [Updates, Shizuku and root](docs/user-guide/en/24-updates.md).

## SandFly Pro

SandFly Pro is a paid add-on with AI features:

- The AI agent carries out a task that you say or type in the app in front.
- Screen recognition finds buttons and other elements that an app draws without making them accessible, and adds them as items that you can read and activate.
- The script agent writes a SandFly script from a description, for you to review in the script builder.
- Image and screen descriptions, questions, Live recognition and translation work without an API key of your own.

To buy SandFly Pro or enter a code, go to **SandFly Pro** in SandFly settings. Each code has a monthly usage limit. Automatic renewal is opt-in. See [SandFly Pro](docs/user-guide/en/18-sandfly-pro.md).

## Image descriptions with your own AI service

Without SandFly Pro, **Describe image**, **Describe screen** and Live recognition can use an AI service with your own API key, or a model that runs on the phone. Without either, **Describe image** reads only the text in images.

To use an AI service:

1. Go to **Image and screen descriptions** and turn on **Detailed image descriptions**.
2. Go to **AI services** and open the settings of a service: Gemini, Claude, OpenAI (ChatGPT), Mistral, Groq, OpenRouter, Ollama Cloud, Qwen, Zhipu GLM, Baichuan, DeepSeek, Kimi, Hunyuan, SiliconFlow, or **Other service** for a service with an OpenAI-compatible API.
3. Choose the API key item. **Get a key** opens the website of the service, where the service offers one. Paste the key and choose **Save**.
4. Choose the model item, then choose a model from the list for your key, or enter its name.

SandFly uses a service as soon as it is set up, if no other service is in use. **AI services** > **AI service** switches between the services that are set up. Images and screenshots that you describe are sent to the service. On the free tier of Gemini, Google can use this data to improve its products.

To describe images on the phone, with nothing sent over the network, go to **AI services** > **On-device AI**, download a model, such as Gemma 4 E2B (2.6 GB, for phones with about 6 GB of memory), and turn on **Use on-device AI**. On-device AI needs a 64-bit ARM phone. On some phones, **Processor** runs the model on the GPU or the NPU. See [Image and screen descriptions](docs/user-guide/en/17-descriptions.md).

## Permissions through Shizuku, root or adb

Some features need permissions that Android grants only through adb. With [Shizuku](https://shizuku.rikka.app) installed and running, SandFly grants them itself; the first time, Shizuku asks whether to allow SandFly. On a rooted device, turn on **Shizuku and root** > **Use root instead of Shizuku** instead.

Features that need Shizuku, root or adb:

- **Touch** > **Fix stuck touch exploration**, on by default, needs the DUMP permission.
- **Audio output** > **Speaker when away from your ear** needs permission to manage calls.
- **Audio output** > **Reset accessibility audio after calls** works only through Shizuku or root.
- **Volume keys** > **Volume key shortcuts with the screen off** works only through Shizuku.
- The SandFly overview of recent apps, Alt+Tab and split screen.
- The switches and sliders of the SandFly quick settings panel.
- App shortcuts that apps add while they run, and those that only the home screen app may open, from Android 12, with Shizuku started through adb or wireless debugging, or with root.
- Clipboard history of text copied in other apps, and the key vibration of Gboard versions before 18.8 while you explore the keyboard.
- **Updates and installation** > **Appear installed from Google Play**.

Without Shizuku, a setting that needs an adb permission shows the adb command, and its **Copy command** action copies it. See [Updates, Shizuku and root](docs/user-guide/en/24-updates.md).

## Report a problem

To report a problem or suggest a feature, go to **Help & feedback** > **Send feedback** in SandFly settings, or choose **Send feedback** in the SandFly menu. No account is needed. **Include device information** and **Include SandFly log** add the Android version and device model, and the recent SandFly log. Replies from the developer arrive in SandFly. See [Help and feedback](docs/user-guide/en/25-help.md).

You can also open an [issue on GitHub](https://github.com/aaron-gh/SandFly/issues).

## Languages

SandFly is translated into Afrikaans, Albanian, Amharic, Arabic, Armenian, Assamese, Azerbaijani, Bangla, Basque, Belarusian, Bosnian, Bulgarian, Burmese, Catalan, Chinese (Simplified, Traditional and Hong Kong), Croatian, Czech, Danish, Dutch, English (United Kingdom, Australia and India), Estonian, Filipino, Finnish, French (France and Canada), Galician, Georgian, German, Greek, Gujarati, Hebrew, Hindi, Hungarian, Icelandic, Indonesian, Italian, Japanese, Kannada, Kazakh, Khmer, Korean, Kyrgyz, Lao, Latvian, Lithuanian, Macedonian, Malay, Malayalam, Marathi, Mongolian, Nepali, Norwegian, Odia, Persian, Polish, Portuguese (Brazil and Portugal), Punjabi, Romanian, Russian, Serbian (Cyrillic and Latin), Sinhala, Slovak, Slovenian, Spanish (Spain and Latin America), Swahili, Swedish, Tamil, Telugu, Thai, Turkish, Ukrainian, Urdu, Uzbek, Vietnamese and Zulu.

The user guide is in English, Dutch, Indonesian and Polish, and shows in English in other languages. Its lists of default gestures and keyboard shortcuts are shown in the language of SandFly. To translate the guide, see [docs/user-guide/README.md](docs/user-guide/README.md).

## For developers

- [Contributing](CONTRIBUTING.md): what SandFly accepts, and what a pull request must include.
- [Writing SandFly scripts](docs/scripting.md): the Lua script API, with examples in [docs/scripting/examples](docs/scripting/examples).
- [Speaking through SandFly from another app](docs/speech-api.md): other apps can speak with the SandFly voice, once the user allows each app.
- [Providing a braille display from another app](docs/braille-display-api.md): an app, such as a notetaker app, can give SandFly a braille display that SandFly can't reach on its own.
- [Choosing where your app gets direct touch](docs/direct-touch-api.md): games can take direct touch on the whole screen or only in some areas.
- [SandFly item files](docs/reference/item-files.md): the file format of exported macros, gesture profiles, keymaps, settings profiles and dictionaries, for writing them by hand or with an AI chat.

## Move from Backtalk Aaron

The last build of Backtalk Aaron offers SandFly as an update.

1. Install SandFly from the notification, or from **Check for updates** in Backtalk Aaron. SandFly installs next to Backtalk Aaron, and the accessibility settings open.
2. Turn on SandFly. SandFly copies the settings and custom labels of Backtalk Aaron.
3. Backtalk Aaron turns itself off, and a notification offers to uninstall it.

On-device AI models aren't copied: download them again in SandFly. Turn on the braille keyboard again for SandFly if you used it.

If you use an accessibility shortcut, grant SandFly permission to change secure settings before you turn it on, and SandFly moves the shortcut to itself:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
```

Otherwise, set the shortcut again for SandFly.

## Build SandFly

You need JDK 17 or later, the Android SDK with platform 37, and NDK 27.3.13750724. The Gradle wrapper downloads the right version of Gradle.

SandFly Pro is in a private repository, `sandfly-ai`, which a build includes only when it is checked out into `sandfly-ai/`. Builds without it have no SandFly Pro features.

### Linux or macOS

Set `ANDROID_SDK` to the path of your SDK, then run `./build.sh`. It builds the phone and watch APKs, the Google Play build and the SandFly add-on, and keeps anything else in `local.properties`, such as a Gemini API key.

To build only the phone APK:

```
./gradlew assemblePhoneFullDebug
```

### Windows

Set `ANDROID_HOME` to the path of your SDK, then run:

```
.\gradlew.bat assemblePhoneFullDebug
```

`.\deploy.ps1` builds the APK and installs it with adb. It needs PowerShell 7. To install the last build without building again, use `-SkipBuild`.

### Tests and lint

```
./gradlew :talkback:testPhoneFullDebugUnitTest
./gradlew :lintPhoneFullDebug
```

Unit tests are in the `talkback` module. Lint fails only on issues that aren't in `lint-baseline.xml`; a new English string needs a `MissingTranslation` entry there until it is translated. GitHub Actions builds the APKs, runs the unit tests and runs lint for each change.

### Signing

Android installs an update only when it is signed with the same key as the installed app. Builds from GitHub are signed with the SandFly key. A build you make yourself is signed with your own debug key, so it can't update a build from GitHub or be updated by one. Uninstall one before you install the other.

### A built-in Gemini key

A build of your own can have a Gemini key built in. Add `gemini.api.key=YOUR_KEY` to `local.properties`, then build and install SandFly again. A key entered in settings replaces the built-in key. Git ignores `local.properties`, but don't share an APK that contains your key. To use another model, add `gemini.model=MODEL_NAME`. The default is `gemini-flash-latest`.

### Debug tools

Debug builds include tools to find lag:

- The `BacktalkStall` logcat tag logs each time the main thread is blocked for more than 100 ms, with the code that was running.
- The `BacktalkGesture` logcat tag logs touch state changes and each gesture that SandFly detects.
- Event processing has trace sections, which show in [Perfetto](https://perfetto.dev) system traces.

### Make a release

1. Set `sandflyVersion` in `version.gradle` to the new version, such as `0.7.0`, write the release notes in `release-notes/0.7.0.md`, and push to master. List items in the notes start with `* `: the in-app updater reads lines that start with `- ` as commits.
2. Tag the commit `v0.7.0` and push the tag. GitHub Actions builds the release and publishes it with the release notes.

The build number is the number of commits since `buildNumberBase` in `version.gradle`. The version code is 100000000 plus the build number: Gboard leaves typing to the screen reader only when the version code of the screen reader is at least 60105832.

Library and plugin versions are in `gradle/libs.versions.toml`. Renovate opens pull requests for updates.

## History, license and third-party components

SandFly began as Backtalk Aaron, a fork of Backtalk, and became a separate project when it had many changes that are not in Backtalk. The git history keeps every change from TalkBack, Backtalk and SandFly. Files from TalkBack and Backtalk are changed in SandFly.

SandFly is under the Apache License 2.0, as TalkBack and Backtalk are. See [LICENSE](LICENSE).

- **Emoji names** are from the [Unicode CLDR](https://cldr.unicode.org/) and the Unicode emoji data, copyright Unicode, Inc., under the Unicode License v3, in `utils/src/main/assets/emoji_names/LICENSE`.
- **Scripts** run in [Lua 5.4](https://www.lua.org/), copyright Lua.org, PUC-Rio, through [LuaJava](https://github.com/gudzpoz/luajava), copyright gudzpoz, both under the MIT License. The license texts are in `talkback/src/phone/assets/scripting/LICENSE-luajava`, which is in the APK.
- **Japanese braille** follows [libkuraji](https://github.com/nishimotz/libkuraji), copyright Takuya Nishimoto, ported to Kotlin, under the BSD 3-Clause License, in `braille/translate/src/phone/assets/japanese-braille/LICENSE-libkuraji`, in the APK. Its dictionary, the JTalk extended dictionary from [libkuraji-jtalk-dic](https://github.com/nishimotz/libkuraji-jtalk-dic) (NAIST-JDIC with the additions of NVDA Japanese, BSD 3-Clause), is downloaded when Japanese braille is first used, checked against its SHA-256, and read by a reader of the MeCab dictionary format in SandFly.
- **Dictation** uses [whisper.cpp](https://github.com/ggml-org/whisper.cpp) 1.9.5, copyright the ggml authors, under the MIT License, in `whisper/src/main/cpp/whisper.cpp/LICENSE`, with only the CPU backend. The Whisper models of OpenAI, under the MIT License, are downloaded from [Hugging Face](https://huggingface.co/ggerganov/whisper.cpp) when you choose one.
- **Whisper on Adreno GPUs** uses the OpenCL backend of whisper.cpp, built as a separate library, `libsandfly_whisper_opencl.so` (module `whisper-opencl`, 64-bit ARM only), loaded only when **Typing** > **Dictation** > **Processor** is GPU. It is built against the [Khronos OpenCL headers](https://github.com/KhronosGroup/OpenCL-Headers), Apache License 2.0, in `whisper-opencl/src/main/cpp/opencl`, and links against a `libOpenCL.so` made only for linking, which isn't packaged. If the library can't load, as with an OpenCL driver older than 3.0, Whisper runs on the CPU.
- **On-device AI models** are downloaded from the [LiteRT community](https://huggingface.co/litert-community) on Hugging Face and checked against their SHA-256. Gemma 4 E2B and E4B are the recommended models; the other small vision models are community conversions marked experimental.
- **NPU libraries** are downloaded only on phones with a supported NPU, together with the model built for it, and checked against their SHA-256. None of them are in the APK.
  - Google Tensor G5 and G6 (Pixel 10 and 11), with Gemma 4 E2B: `libLiteRtDispatch_GoogleTensor.so` from `com.google.ai.edge.litert:litert-npu-runtime-google-tensor` 2.3.0, from the Google Maven repository.
  - Snapdragon 8 Elite (SM8750) and 8 Elite Gen 5 (SM8850), with FastVLM 0.5B (Apple research license): `libLiteRtDispatch_Qualcomm.so` from `litert-npu-runtime-qualcomm` 2.3.0, and `libQnnHtp.so`, `libQnnSystem.so` and the Hexagon v79 or v81 stub and skel from `com.qualcomm.qti:qnn-runtime` 2.50.0, from Maven Central, under the Qualcomm AI Hub Model License, which allows them in apps.
