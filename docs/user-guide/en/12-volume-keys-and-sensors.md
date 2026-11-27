## Volume keys and sensors {#buttons}

**Volume keys** in SandFly settings sets what the volume keys do while SandFly is on: adjust sliders, move the cursor, run commands, and the feedback for volume changes. The last section of this chapter lists the features that use the sensors of the phone.

### Volume keys and speech volume {#buttons-speech-volume}

To change the speech volume from any screen, hold one finger on the screen and press volume up or volume down. The volume keys also change the speech volume during continuous reading. Speech volume and the other volume settings are described in [](#sound).

Pressing both volume keys together runs no SandFly command, so the accessibility shortcut of Android still works. It also unlocks touch; see [](#touch-lock).

### Adjust sliders and move the cursor {#buttons-sliders-cursor}

- **Volume keys adjust sliders**: when a slider has focus, volume up and volume down change its value instead of the volume, and SandFly speaks the new value. Hold a key to keep changing the value. Off by default.
- **Volume keys move the cursor**: when a text field has focus, volume up moves the cursor back a character and volume down moves it on, and SandFly speaks the character. Off by default.

When a key also has a command for one of its presses (see [](#buttons-commands)), its presses run their commands, and a press that runs no command adjusts the slider or moves the cursor once it ends, in place of changing the volume.

When focus is on anything else, or the screen is off, the volume keys change the volume as usual. With a finger on the screen or during continuous reading, they still change the speech volume.

### Run commands with the volume keys {#buttons-commands}

Each volume key can run a command for a double press, a triple press and a long press: **Volume up double press**, **Volume up triple press**, **Volume up long press**, **Volume down double press**, **Volume down triple press** and **Volume down long press**. All are unassigned by default.

To assign a command to a press:

1. Go to **Volume keys**.
2. Choose the press, such as **Volume down double press**.
3. Choose a command from the list, which is the same as for gestures. See [](#commands).

A key with no command for any press changes the volume at once, as usual. When a key has a double or triple press command, a single press changes the volume 0.3 seconds after you release the key, once no further press follows. When a key has a long press command, holding it runs the command instead of changing the volume repeatedly.

**Volume key long press duration** sets how long you hold a key for its long press: **System default**, or from 0.3 to 1 second.

The presses don't run commands, and the keys change the volume as usual, in these cases:

- While the screen is off, unless the lock screen shows on an always-on display, or **Volume key shortcuts with the screen off** is on. See [](#buttons-screen-off).
- During a phone call, or while an app plays call audio.
- While a finger is on the screen, or during continuous reading.
- While the Shortcut Menu app is shown.

While a phone call rings, only a volume key with the **Answer call**, **End call** or **Say caller** command assigned to one of its presses runs commands.

### Keep volume key presses from Android {#buttons-take-all}

- **SandFly handles every volume key press**: while the screen is on, every volume key press goes to SandFly. Presses with a command run it, and other presses, including holding a key, change no volume. While this setting is on, **Volume up press** and **Volume down press** appear, and run a command for a single press. Off by default.
- **Hide volume panel**: while the screen is on, presses with no command change the volume without showing the volume panel of Android. Off by default. With both settings on, presses with no command change no volume.

Both settings leave the keys to Android while the screen is off, during a phone call or while an app plays call audio, while a call rings, while SandFly is paused, and when both volume keys are held. Slider adjustment, cursor movement and speech volume with a finger on the screen still work.

### Volume key shortcuts with the screen off {#buttons-screen-off}

With **Volume key shortcuts with the screen off** on, the volume key presses that have commands run them while the display is off. Presses without a command change the volume. The setting needs Shizuku, and isn't shown when SandFly uses root instead of Shizuku or on watches; see [](#updates-shizuku). The first time you turn it on, Shizuku shows a request to allow SandFly. It's off by default.

### Mute key {#buttons-mute-key}

On phones with a mute key, such as the Zinwa Q25, **Volume keys** has two more settings:

- **Hold mute key to dictate in text fields**: when a text field has input focus, a keyboard is shown or connected, and no call is active, holding the mute key runs **Dictate**: recording starts when you press the key, and stops when you release it, and SandFly types the text. Pressing the key during a dictation into a text field stops that dictation. On by default. See [](#typing-dictate).
- **Mute key press**: the command that a press of the mute key runs everywhere else. The list is the same as for gestures. Unassigned by default.

When neither setting takes a press, it goes to the keymap, where the mute key can be a keyboard shortcut of its own in the keyboard phone keymaps and in custom keymaps (see [](#keyboards-keymaps)), and then to Android.

### Speech volume on Samsung phones {#buttons-samsung}

On Samsung phones, One UI changes the speech volume when you press a volume key while SandFly speaks, and for a second after. SandFly keeps these presses from One UI and changes the volume that the keys change when SandFly is silent, such as the media volume.

To have the volume keys change the speech volume while SandFly speaks, turn on **Volume keys change speech volume while speaking**. This setting is shown only on Samsung phones, and is off by default.

### Volume change feedback {#buttons-feedback}

**Volume change feedback** sets what SandFly plays when a volume changes:

- **Off**.
- **Tone**: a short tone on each volume step, at the volume being changed.
- **Speech**: the volume that changed and the new level, such as "Media volume set to 60 percent", after the last step. This is the default.
- **Tone and speech**.

With a tone, **Silence tone during media playback** turns off the tone while an app plays media. With speech, **Silence speech during media playback** turns off the announcement while an app plays media, and **Interrupt speech to announce the volume** stops the speech that is playing to announce the volume. The two media switches are off by default, and **Interrupt speech to announce the volume** is on.

With speech, **Change speech volume silently while speaking** leaves out the announcement when the volume keys change the speech volume while SandFly speaks or has speech queued, so speech continues at the new volume. Presses that follow each other within a second count as one series, which stays silent after speech ends. The tone still plays if chosen. Presses while a finger is on the screen, presses while SandFly is silent, and changes from the volume panel or apps are announced. On Samsung phones, outside continuous reading, the keys change the speech volume while SandFly speaks only with **Volume keys change speech volume while speaking** on. Off by default.

Volume changes that SandFly makes, such as with a volume command, get no feedback. Volume changes that other apps make do. There's no tone during a call or while SandFly is paused.

### Sensors {#buttons-sensors}

These features use the sensors of the phone. Each is set on the screen named here.

- **Back tap**: two, three or four taps on the back of the phone run commands. **Gestures** > **Back tap**; see [](#gestures-back-tap).
- **Shake**: shaking the phone runs a command. **Gestures** > **Shake**; see [](#gestures-shake).
- **Cover proximity sensor to stop speech**: covering the proximity sensor stops speech. Off by default. **Speech** > **Cover proximity sensor to stop speech**; see [](#speech-stop).
- **Lock touch automatically** > **While the proximity sensor is covered**: touch is locked while something covers the proximity sensor. See [](#touch-lock).
- **Shake to stop** and **Wave to stop**: shaking the phone, or moving a hand over the proximity sensor, stops the reading of a notification. **Notifications** > **Stop reading**; see [](#notifications-stop).
- **Speaker when away from your ear**: a call moves to the speaker when you take the phone away from your ear, and back to the earpiece when you hold it up. **Audio output** > **Speaker when away from your ear**. It needs Android 12 or later. See [](#sound-calls).
