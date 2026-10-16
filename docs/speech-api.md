# Speaking through Backtalk from another app

An app can ask Backtalk to say text in the user's own Backtalk voice, with their speech engine, rate, pitch and volume. This suits games, automation apps such as Tasker, and remote-access tools that want to talk to a blind user without a speech engine of their own.

## What the user controls

Nothing your app sends is said until the user allows it:

1.  The user turns on **Advanced settings** > **Let other apps speak** in Backtalk. It's off by default.
2.  The first time your app asks to speak, Backtalk posts a notification that asks the user whether to allow it. Until they choose **Allow**, nothing your app sends is said, and requests aren't saved for later.
3.  The user can allow or block your app at any time in **Advanced settings** > **Let other apps speak**.

Even for an allowed app:

*   If the user turns on **Play a sound before other apps speak**, Backtalk plays a short sound before each thing your app says, so they can tell it apart from Backtalk's own speech.
*   Nothing is said while the device is locked, or while Backtalk is off or paused.
*   Each app can speak a few times at once, then about once a second. Requests beyond that are dropped.
*   Text is said as plain text, without styling.
*   Interrupting and stopping only affect speech from other apps. Your app can never cut off Backtalk's own speech.

Backtalk doesn't tell your app whether the user allowed it, or whether the text was said.

## Permission

Declare Backtalk's permission in your manifest:

```xml
<uses-permission android:name="fyi.quin.backtalk.permission.SPEAK" />
```

On Android 11 and later, also make Backtalk visible to your app:

```xml
<queries>
    <package android:name="fyi.quin.backtalk" />
</queries>
```

## Binding to the service

The service works on every Android version, and is the most reliable way. Copy `talkback/src/main/aidl/fyi/quin/backtalk/speech/ISpeechControl.aidl` into your app, keeping the package `fyi.quin.backtalk.speech`, then bind to it:

```kotlin
val intent =
  Intent("fyi.quin.backtalk.action.SPEECH_CONTROL").setPackage("fyi.quin.backtalk")
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

Once connected:

```kotlin
val speech = ISpeechControl.Stub.asInterface(binder)
// Text, interrupt, rate, pitch, volume.
speech.speak("Level 3", false, 1f, 1f, 1f)
speech.stop()
```

| Method | What it does |
| --- | --- |
| `speak(text, interrupt, rate, pitch, volume)` | Says `text`. `rate` and `pitch` are multiples of the user's own, from 0.25 to 4. `volume` is from 0 to 1. With `interrupt`, it replaces anything other apps were saying instead of waiting for it. |
| `stop()` | Stops anything other apps are saying. |

Both calls are one-way, so they return at once.

## Sending a broadcast

A broadcast is simpler to send, for example from an automation app, but Backtalk has to know which app sent it. So broadcasts work only on Android 14 and later, and only when the sender shares its identity:

```kotlin
val intent =
  Intent("fyi.quin.backtalk.action.SPEAK")
    .setPackage("fyi.quin.backtalk")
    .putExtra("text", "Download finished")
    .putExtra("interrupt", false)
val options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true)
context.sendBroadcast(intent, null, options.toBundle())
```

| Action | Extras |
| --- | --- |
| `fyi.quin.backtalk.action.SPEAK` | `text` (string, required), `interrupt` (boolean), `rate`, `pitch`, `volume` (numbers, as for `speak`) |
| `fyi.quin.backtalk.action.STOP_SPEECH` | None |

Backtalk ignores broadcasts that don't share their identity.

## Scripts

Backtalk scripts don't need any of this. They speak with `backtalk.speak`, as described in [Writing Backtalk scripts](scripting.md).
