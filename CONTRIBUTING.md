# Contributing to SandFly

## Scope

SandFly accepts features that are within the scope of a screen reader. Features outside that scope are not accepted.

## Features need an agreed issue

1.  Open an issue that describes the feature: what it does, who needs it, and how it is used.
2.  Wait until the feature is agreed in the issue.
3.  Implement it, and link the issue in the pull request.

A pull request for a feature with no agreed issue is closed without comment.

Bug fixes do not need an issue first. The pull request describes the problem and how it was reproduced.

The issue must also name, before implementation:

*   Any new Android permission, and the feature that needs it.
*   Any new use of Shizuku or root.
*   Any new exported component, intent action or API for other apps.
*   Any new network connection, and what is sent.
*   Any new library.

A pull request that adds one of these without agreement in the issue is rejected.

## Security vulnerabilities

Do not report a security vulnerability in a public issue. Use **Help & feedback** > **Send feedback** in SandFly; reports go to a private repository.

## Security and privacy

SandFly is an accessibility service. It reads every screen, acts in every app, and holds permissions for calls, contacts, the microphone, the camera, location and secure settings. With Shizuku or root it gets adb permissions. A change must not give any of this to another app, a script, a file or a server without the user's choice.

### Other apps

*   Every exported component is protected by a permission, or does nothing an app could not do itself.
*   An app gets nothing from SandFly until the user allows that app in SandFly. Follow the existing APIs: speech, braille displays and direct touch. Each lists the app in SandFly settings, where the user can allow or block it at any time.
*   Allowing an app needs the device unlocked. An app does not act or receive screen content while the device is locked unless the user allowed that separately.

### Scripts

*   Every new script call that reads screen content, acts in an app, or reaches the network, clipboard, notifications or system belongs to a script permission. Add it to an existing permission where one fits; a new permission is listed in the install dialog.
*   Password field text stays `nil` without the `passwords` permission.

### Files and input from outside SandFly

Scripts, themes, item files, sounds, speech engine audio, intents and server responses are untrusted. Check sizes, formats and values before allocating or dividing, reuse the existing checks, and fail with an error the user can read instead of a crash.

### Network

*   No analytics, tracking, advertising or crash reporting SDKs.
*   Screen content, images, audio and text go to a server only after the user chose that service and the action that sends them.
*   API keys and codes are stored as the existing ones are, and never in code, logs or reports.

### Logs

The SandFly log is sent with problem reports. Never log password text, typed text from password fields, API keys, codes or account details.

### Shizuku and root

*   Use Shizuku or root only for the feature that needs it, and only while that feature is on.
*   Never pass text from a file, script, app or server to a shell command.
*   Without Shizuku or root, show the adb command, as existing settings do.

## Reliability

A crash or a hang leaves the user without speech.

*   New behaviour runs only for users who turn it on. A change to default behaviour for everyone needs its own pull request and its own issue.
*   A switch that trades correctness for speed is off by default. Gesture recognition never moves focus before the gesture is known.
*   Code that runs off the main thread and decodes or allocates catches `Throwable`.
*   Every cache, map, queue and buffer has a size limit.
*   No polling or `sleep` loops. Timers and threads run only while there is work, and stop when the feature is turned off and when the service stops.
*   No R8 or minification.
*   Code in `talkback/src/main` also builds for watches. Build the wear flavor as well as phone.

## Settings and text

*   A setting goes on the screen named after what it changes. No "Advanced" or other general screens.
*   A setting has a title only, unless the title cannot say what it does.
*   New English strings go in `talkback/src/main/res/values/strings.xml` with a translator comment, and a `MissingTranslation` entry in `lint-baseline.xml`. Do not add translations.
*   Update the user guide in `docs/user-guide/en` for each change users can see.
*   Write plainly. Controls and messages say what happens, in as few words as possible.

## AI-assisted contributions

AI-assisted contributions are permitted. The contributor is responsible for every line, and must know what it does as though they had written it themselves.

## Licence

SandFly is under the Apache License 2.0. Contributions are under the same licence. Do not copy code from sources with an incompatible licence, and credit the source of any code that is copied.

## Testing

Every change must be tested on a device running a build with the change. Passing unit tests is not enough.

The pull request states:

*   The device and Android version.
*   What was tested and the result.

Unit tests and lint must pass. See [Tests and lint](README.md#tests-and-lint).

## Touch, gestures and scrolling

A change that affects touch exploration, gestures or scrolling must include timing measurements:

*   One set measured on a build of a clean `master`.
*   One set measured on a build of `master` with the change.

Both sets use the same device, the same settings and the same actions. Repeat each action at least 20 times, and report the median and the maximum for each build.

A pull request that changes touch exploration, gestures or scrolling without these measurements is rejected.

Debug builds log touch state changes and each detected gesture under the `BacktalkGesture` logcat tag. To record them with millisecond timestamps:

```
adb logcat -v epoch -s BacktalkGesture
```

Event processing also has trace sections, which show in [Perfetto](https://perfetto.dev) system traces.

## Pull requests

*   One change per pull request.
*   Base the branch on the current `master`.
*   Fill in the pull request template.
