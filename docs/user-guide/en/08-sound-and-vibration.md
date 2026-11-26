## Sound, vibration and audio output {#sound}

SandFly plays a sound and a vibration for actions and events, such as moving focus, activating an item, scrolling and opening a window. **Sound and vibration** in SandFly settings turns sounds and vibrations on and off, sets their volume and strength, and holds sound themes and control sounds. **Audio output** sets where speech and sounds play, at which volume, and what happens to other audio and to calls while SandFly is on.

For the speech engine, voices, speech rate, speech volume and voice effects, see [](#speech). For the volume keys and **Volume change feedback**, see [](#buttons).

### Turn sounds on or off {#sound-feedback}

The **Sounds** group of **Sound and vibration** has these settings:

- **Sound feedback**: plays the SandFly sounds. On by default. With it off, vibrations still play.
- **Sound feedback volume**: the volume of the sounds, from 5 to 100 percent in steps of 5. The default is 50 percent.
- **Sound themes**: opens the installed sound themes, control sounds and 3D audio. See [](#sound-themes).
- **Actions available sound**: when the **Actions available** sound plays: **Off**, **When the item gets focus**, the default, or **After the item is spoken**. See [](#sound-actions-available).

The **Turn sound feedback (earcons) on or off** keyboard shortcut turns **Sound feedback** on or off. See [](#appendix-keyboard-shortcuts).

To turn off a single sound and keep the rest, see [](#sound-individual).

### Change vibration {#sound-vibration}

The **Vibration** group of **Sound and vibration** has these settings. On devices without a vibrator, the group isn't shown.

- **Vibration feedback**: plays the SandFly vibrations. On by default. With it off, sounds still play.
- **Vibration strength**: how strong vibrations are, from 5 to 200 percent. The default is 100 percent. The setting is shown only on devices whose vibrator can vary its strength.
- **Vibration type**: how vibrations are played. The list shows only the types the vibrator can play, and the setting is hidden when only one type is available. Until you choose a type, SandFly uses the first one in the list.
  - **Haptic effects**: composed effects such as clicks and ticks, on Android 11 and later with a vibrator that supports them.
  - **Pulses**: vibrations of varying strength, on vibrators that can vary their strength.
  - **On and off**: the vibrator turns on and off at full strength. Every vibrator can play this type.

If vibrations feel weak with **Haptic effects**, choose **Pulses** and raise **Vibration strength**.

The **Vibration feedback** command, in the **Speech** group of commands, turns **Vibration feedback** on or off. See [](#commands).

### Sounds and vibrations {#sound-events}

Every sound has a vibration of its own, which plays with the sound and also plays with **Sound feedback** off. You can turn off all sounds and still tell actions apart by vibration. A few vibrations, such as the announcement vibration and the braille keyboard vibrations, play without a sound.

The sounds, by the names shown in **Individual sounds and vibrations** and in the sound list of a theme:

| Sound | Plays for |
| --- | --- |
| **Focus on an item** | Focus moves to an item. |
| **Focus on an actionable item** | Focus moves to an item that you can activate. |
| **Actions available** | Focus moves to an item with actions of its app, such as Reply on a chat message. See [](#sound-actions-available). |
| **Touching an empty area** | Your finger touches a part of the screen with no item. |
| **Click** | An item is activated. |
| **Long press** | An item is touched and held. |
| **Scroll** | A list scrolls. |
| **Entering a list** and **Leaving a list** | Focus moves into or out of a list. |
| **Action done or end reached** | An action completes, or focus reaches the end of a list. |
| **Window change** | A new window or screen opens. |
| **Next reading control** and **Previous reading control** | Another reading control is selected. |
| **Edge gesture bottom**, **Edge gesture middle** and **Edge gesture top** | The stages of an edge gesture. Not on watches. |
| **Insertion point moved** | Moving by character, word, line or paragraph moves the cursor in a text field that you edit. |
| **Column wrap** and **Column wrap back** | A swipe moves focus to an item on a row below or above. |
| **Agent task started**, **Agent task complete**, **Agent question**, **Agent stopped at a sensitive action** and **Agent task failed** | The SandFly Pro agent. Only in builds with SandFly Pro. See [](#pro). |
| **Spelling error** | A misspelled word. |
| **Link** | A link in text. |
| **Formatted text** | Formatted text, such as bold. |
| **Emoji** | After an emoji, with **Verbosity** > **After emoji names** set to **Play a sound**. No built-in vibration; a sound theme can give it one. See [](#verbosity-emoji). |
| **Screen off** and **Screen on** | The screen turns off or on. |
| **Screen hidden** and **Screen shown** | The screen is hidden, or shown again. See [](#display). |
| **Loading** | A description or other result is loading. |
| **On-device AI model loading** | The on-device AI model is loading. |
| **Browse mode on** and **Browse mode off** | Browse mode turns on or off. See [](#web). |
| **Keyboard opened** and **Keyboard closed** | An on-screen keyboard opens or closes. |
| **SandFly notification** | SandFly posts a notification on a notification channel without a sound. |
| **Full charge** | The battery is fully charged, with **Announce full charge** on. See [](#verbosity-announcements). |
| **Circle menu** | The items of the circle menu, eight notes. |
| **Braille display connected**, **Braille display disconnected**, **Braille command failed**, **Braille auto scroll on**, **Braille auto scroll off** and **Braille keyboard calibrated** | Braille displays and the braille keyboard. See [](#braille). |
| **Control sound: button** to **Control sound: tree item** | Control sounds, one for each kind of control. See [](#sound-control-sounds). |

#### Navigation sounds {#sound-navigation-sounds}

SandFly plays no sound when a gesture starts or ends. These sounds give feedback during navigation instead:

- **Next reading control** and **Previous reading control** play when you select another reading control with a gesture, the circle menu, a keyboard shortcut or the braille keyboard.
- The edge gesture sounds play when a finger touches an edge where an edge gesture starts (**Edge gesture bottom**, a low tick), when the gesture reaches its first stage (**Edge gesture middle**), and when it reaches its second stage (**Edge gesture top**). Sliding back plays the sound of the stage the finger returns to. Lifting the finger plays nothing. For edge gestures, see [](#gestures).
- **Column wrap** plays when a swipe forward moves focus to an item on a row below the focused item, and **Column wrap back** when a swipe back moves focus to an item on a row above. Each list item is a row of its own. Items side by side, such as icons on a home screen or cells of a table, share a row. The sound plays with the focus sound, and its vibration replaces the focus vibration. It doesn't play when focus moves to another window.

#### Actions available sound {#sound-actions-available}

The **Actions available** sound plays for an item with actions of its app beyond the standard ones, such as Reply and Forward on a chat message in a messaging app. Activating, long pressing, scrolling, expanding and other standard actions don't count, so an ordinary button plays no **Actions available** sound. Keys of on-screen keyboards don't count either. The actions are in the **Actions** reading control and in the SandFly menu.

The sound has no built-in vibration; a sound theme can give it one.

**Sound and vibration** > **Actions available sound** sets when it plays:

- **Off**: the sound doesn't play.
- **When the item gets focus**: after the focus sound. The default. A vibration that the theme gives it plays in place of the focus vibration.
- **After the item is spoken**: once the description of the item has been spoken to the end. If you move on before then, it doesn't play.

To have SandFly say "Actions available" after such items, see [](#verbosity-items).

#### Ticks while scrolling {#sound-scroll-ticks}

While you scroll a list with two fingers, SandFly plays a light tick for each item that scrolls past, so a slow drag ticks slowly and a fast drag ticks quickly. In lists that don't report their items, a tick plays for every 48 dp of scrolling. The first tick plays as soon as the drag reaches the app. The scroll sound plays with the ticks, at most every 250 ms, and its pitch rises from the top of the list to the bottom. Scrolls that SandFly makes itself play the scroll sound only.

### Turn off individual sounds and vibrations {#sound-individual}

**Sound and vibration** > **Individual sounds and vibrations** has a switch for each sound, under **Sounds**, and for each vibration, under **Vibrations**. All switches are on by default. Turning off a sound keeps its vibration, and turning off a vibration keeps its sound. **Sound feedback** and **Vibration feedback** still turn off all sounds or all vibrations at once.

To play a sound or vibration before you change its switch:

1. Move focus to the switch.
2. Open the actions menu.
3. Choose **Preview**.

Changing a switch plays nothing.

The **Vibrations** list also has switches for vibrations without a sound: **Announcement**, **Edge gesture slide back**, and the braille keyboard vibrations, such as **Braille keyboard: typing a character** and **Braille keyboard: nothing to delete**. The switch of a vibration also turns off the vibration that a sound theme puts in its place.

### Sound themes {#sound-themes}

A sound theme is a set of sounds and vibrations that replace the built-in ones. Each theme also has its own **Control sounds and vibrations**, **3D audio** and **Still say the kind of control** settings. The built-in sounds are the **SandFly** theme, which can't be removed.

**Sound and vibration** > **Sound themes** has these parts:

- **Create new theme**: makes a new theme. See [](#sound-themes-create).
- **Installed themes**: each installed theme. The theme in use shows "In use" in its summary.
- **Settings of** and the name of the theme in use: the control sound settings and the sound list of the theme. See [](#sound-control-sounds) and [](#sound-themes-replace).
- **Install a theme**: **From a file**, **From a link** and **From a source**. See [](#sound-themes-install).

#### Install a sound theme {#sound-themes-install}

A theme file is a ZIP file. To install one, use one of these:

- **From a file**: choose a theme file with the system file picker.
- **From a link**: enter or paste a link to a theme file, or to a GitHub repository with a theme in it, and choose **Install**. Links must start with `https`.
- **From a source**: a list of repositories on GitHub, GitLab, Codeberg, Gitea or Bitbucket. Choose **Add source** and enter the address of a repository, such as `github.com/owner/repository`. Choose a source to list its theme files, and choose a theme file to install it. The list starts with the SandFly themes repository. **Remove source** removes a source from the list; themes installed from it stay installed.
- Open a theme file with SandFly from a file manager or the downloads of a browser, or share a theme file or a link with SandFly from another app, and choose **Install sound theme**.

Before installing, SandFly shows the author, the description, the number of sounds and vibrations, the license and any files it left out. Choose **Install and use** to install the theme and use it, or **Install** to install it and keep the theme in use. A theme with the same name as an installed theme replaces it.

Note: Watches have no file picker. On a watch, install themes from a link. **From a source** is available on phones only.

#### Choose and manage themes {#sound-themes-manage}

1. Go to **Sound and vibration** > **Sound themes**.
2. Under **Installed themes**, choose a theme.
3. Choose one of these:
   - **Use this theme**: switches to the theme, with its own control sound settings.
   - **About this theme**: shows the author, the description, the number of sounds and vibrations, the license and the website.
   - **Save as a theme file**: saves the theme as a ZIP file that you can share.
   - **Remove**: removes the theme. Not available for the **SandFly** theme.

#### Replace single sounds {#sound-themes-replace}

You can replace any sound of the theme in use with a sound file. To replace sounds without installing a theme, use the **SandFly** theme.

1. Go to **Sound and vibration** > **Sound themes**.
2. Under the settings of the theme in use, choose **Sounds**.
3. Choose a sound. The dialog title shows the sound and its file name, such as "Click (tick)".
4. Choose one of these:
   - **Choose a sound file**: opens the file picker. A sound can be up to 5 MB, in WAV, OGG, MP3 or FLAC format.
   - **Choose from folder**: lists the sound files and subfolders of a folder. The first time, the folder picker opens to choose the folder. See [](#sound-themes-folder).
   - **Change folder**: opens the folder picker to choose another folder for **Choose from folder**. Shown after a folder is chosen.
   - **Preview**: plays the sound.
   - **No sound**: plays no sound for the event.
   - **Use SandFly's sound**: goes back to the built-in sound.

The summary of each sound shows **SandFly's sound**, **Theme sound** or **No sound**. The same actions are in the actions menu of each sound, as **Preview**, **Set to no sound**, **Use SandFly's sound** and **Choose a sound file**. **Use SandFly's sounds for all** removes every sound of the theme and uses the built-in sounds.

The sound list also has sounds that **Individual sounds and vibrations** doesn't: **Volume change**, which plays for each volume step when **Volume change feedback** is set to a tone (see [](#buttons)), and the braille keyboard typing sounds, which play with **Typing sounds** on in the braille keyboard settings (see [](#braille)).

#### Choose sounds from a folder {#sound-themes-folder}

**Choose from folder** lists one folder on the phone. Choose a sound, then **Choose from folder**. The first time, choose the folder in the folder picker.

The list shows subfolders first, then sound files, each sorted by name. Up to 2,000 entries are listed per folder.

- The file in accessibility focus plays. Moving focus off the file stops it.
- Choose a file to use it for the sound.
- Choose a subfolder to list it. **Parent folder** or Back returns to the folder above.

If the folder is removed or SandFly can no longer read it, the folder picker opens again.

#### Create a sound theme {#sound-themes-create}

1. Go to **Sound and vibration** > **Sound themes**.
2. Choose **Create new theme**.
3. Enter a **Name**. A name must be different from the names of installed themes. **Description**, **Author** and **Website** are optional.
4. Choose **OK**. The theme opens in a list of its sounds.
5. Set **Control sounds and vibrations**, **3D audio** and **Still say the kind of control**, and choose a sound file for each sound you want to replace.
6. Choose **Save** to save the theme and use it, or **Save as a theme file** to save it as a ZIP file without using it.

To leave without saving, go back and choose **Discard**. **Keep editing** returns to the theme. If you leave **Control sounds and vibrations** unchanged, it is turned on when the saved theme has control sounds.

#### Theme files {#sound-themes-files}

A theme file is a ZIP file with a `theme.json` file and the sound files, in the same folder. The folder can also be inside one folder of the ZIP file, as in a ZIP file that GitHub makes of a repository, so a GitHub repository with `theme.json` and the sounds at the top is a theme. A ZIP file of sounds without `theme.json` also installs, with the name of the file.

- Name each sound file after the sound it replaces, with the extension of its format, such as `focus.wav` or `tick.ogg`. The dialog of each sound in **Sounds** shows its file name. A theme needs only the sounds it changes.
- WAV, OGG, MP3, FLAC, M4A, AAC and Opus files work, up to 5 MB each and 50 MB for the theme. A file Android can't play is left out at installation.
- `theme.json` can set the name, author, description, license and website of the theme, its control sound settings, sounds set to no sound, and vibrations. A vibration can have up to three forms, for the three vibration types.
- License and readme files, such as `LICENSE.txt` or `README.md`, are kept with the theme. Other files are left out.

For the complete file format, with every sound and vibration name, see [Sound themes](https://github.com/aaron-gh/SandFly/blob/master/themes.md).

### Control sounds and vibrations {#sound-control-sounds}

With control sounds on, focus on a control plays a sound and a vibration for its kind of control in place of the focus sound and vibration, and SandFly doesn't say the kind of control, such as "button". There are sounds for buttons, checkboxes (also switches and toggle buttons), radio buttons, edit fields, drop-down lists, sliders, images, clocks, tabs, menu items and list items, and on web pages, links and tree items.

To turn on control sounds:

1. Go to **Sound and vibration** > **Sound themes**.
2. Under the settings of the theme in use, turn on **Control sounds and vibrations**. It's off by default.

The settings under the theme in use:

- **Control sounds and vibrations**: plays control sounds and vibrations.
- **3D audio**: when control sounds play from the position of the control. See [](#sound-3d-audio).
- **Still say the kind of control**: says the kind of control after its sound. On by default for the **SandFly** theme, off for other themes unless the theme sets it.

An item that isn't a control itself, such as a row in Settings, plays the sound of the first control in it, such as its switch, and otherwise the sound of its kind of item, such as a list item. Keys of the on-screen keyboard keep the focus sound. A control vibration plays with **Sound feedback** off, so you can tell controls apart by vibration. A kind of control whose sound is turned off in **Individual sounds and vibrations**, or set to no sound by the theme, plays the focus sound, and SandFly says the kind of control.

Each theme keeps its own control sound settings. Switching themes switches them.

#### 3D audio {#sound-3d-audio}

Control sounds play from the position of the control on the screen: from the left for a control on the left, and from higher up for a control near the top. With 3D audio, the sounds use measurements of how a head receives sound from each direction. Without it, they move only between left and right.

**3D audio** has these options:

- **With headphones**: the default. 3D audio plays when SandFly audio goes to wired or USB headphones, a Bluetooth headset or headphones, or hearing aids. On Android 13 and later, SandFly checks where its audio plays, so a connected Bluetooth speaker or car that doesn't play SandFly audio doesn't count. On earlier versions, any connected headphones count.
- **Always**: 3D audio also on the phone speaker.
- **Never, only left and right**: the sounds move only between left and right.
- **With headphones, centred otherwise**: 3D audio with headphones, as **With headphones**. On the phone speaker and other outputs, the sounds play from the centre, without moving between left and right.

### Audio output settings {#sound-audio-output}

**Audio output** in SandFly settings has these settings:

- **Audio output device**: where speech and sounds play. See [](#sound-output-device).
- **Phone speaker with these devices**: outputs that move SandFly audio to the phone speaker while connected. See [](#sound-speaker-devices).
- **Speaker when away from your ear**: switches calls between the earpiece and the speaker. See [](#sound-calls).
- **Keep speech on the phone while casting**: see [](#sound-casting).
- **Reset accessibility audio after calls**: see [](#sound-calls).
- **Volume control for speech**: the volume that speech and sounds use. See [](#sound-speech-volume).
- **Audio ducking**: see [](#sound-ducking).
- **Low-latency audio**: see [](#sound-low-latency).

### Choose the audio output device {#sound-output-device}

**Audio output** > **Audio output device** sets where SandFly speech and sounds play:

- **System default**: the default. Android chooses the output.
- **Phone speaker**: the phone speaker, also while Bluetooth or wired headphones are connected.
- A connected device by name, such as a Bluetooth, wired, USB or hearing aid device. The list shows the devices connected now.

A Bluetooth device that you choose receives audio through Android routing, as with **System default**, so the device doesn't switch to call mode. If Android routes audio to another device while the chosen Bluetooth device is connected, the audio stays on the other device. Wired, USB and hearing aid devices receive audio directly.

The choice stays when devices connect or disconnect and when other apps start playing. While the chosen device isn't connected, the setting shows its name with "(disconnected)", and audio follows Android routing until the device connects again. During phone calls, and while another app uses the microphone for a call or a voice assistant, Android chooses the output.

The **Audio output device** reading control changes the setting with a swipe up or down. See [](#sound-reading-controls).

### Play on the phone speaker with some devices {#sound-speaker-devices}

**Audio output** > **Phone speaker with these devices** lists the Bluetooth, wired and USB outputs connected now and those connected before while SandFly was on. While a device that you check is connected, SandFly plays speech and sounds on the phone speaker, whatever **Audio output device** is set to. For example, check a Bluetooth speaker to have speech on the phone while music plays on the speaker, and leave your earbuds unchecked to have speech in them.

Devices are listed by name, so two devices with the same name are one choice. Until a device has been connected, the setting shows "No Bluetooth or wired devices connected yet".

### Choose the volume for speech {#sound-speech-volume}

**Audio output** > **Volume control for speech** sets the volume that speech and SandFly sounds play at:

- **Accessibility volume**: the default.
- **Media volume**.
- **Ring volume**: speech plays as a ringtone. It stays on the phone while media is cast to another device, can play on the phone speaker while headphones are connected, and is silent while the ringer is silent.
- **Call volume, earpiece**: speech plays as a call, on the earpiece when no headset is connected.

While **Audio output device** sends audio to the phone speaker or to a wired, USB or hearing aid device, speech uses the accessibility volume.

While speech doesn't use the accessibility volume, Android plays the accessibility sounds of other apps, such as the tones of voice typing, at the media volume.

The **Speech volume** reading control switches between these volumes, and the **Accessibility volume** reading control changes the accessibility volume. See [](#sound-reading-controls).

### Lower other audio while SandFly speaks {#sound-ducking}

**Audio output** > **Audio ducking** lowers the volume of other audio, such as music, while SandFly speaks. It's on by default. The **Audio ducking** reading control and the **Audio ducking** command, in the **Speech** group of commands, turn it on and off.

### Low-latency audio {#sound-low-latency}

**Audio output** > **Low-latency audio** plays SandFly sounds and speech through the fastest audio path of the device, so they start sooner. It's on by default on phones and tablets and off by default on watches. If speech or sounds break up or sound wrong, turn it off.

- Speech uses the fast path on Android 11 and later. On earlier versions, only sounds use it.
- Speech plays the usual way during calls, shortly after a call starts or ends, while an app records with the microphone, and with speech engines that play their own audio.
- Resuming speech mid-word after a pause needs **Low-latency audio**. See [](#speech-pause). With it off, voice effects work only where the device supports them as an audio effect. See [](#voice-effects).

### Keep speech on the phone while casting {#sound-casting}

**Audio output** > **Keep speech on the phone while casting** stops screen casting and screen mirroring from sending SandFly speech and sounds to the TV or speaker. Screen recordings then have no SandFly audio either. It's off by default. Requires Android 10 or later.

### Calls {#sound-calls}

**Audio output** has two settings for calls.

**Speaker when away from your ear** switches a call to the speaker when you take the phone away from your ear, and back to the earpiece when you hold it up again. It's off by default and requires Android 12 or later. It works only while SandFly is on and not paused, and only moves the call between the earpiece and the speaker: Bluetooth and wired headsets stay in use. A call that starts with the phone away from your ear goes to the speaker.

The setting needs a permission that Android doesn't give to screen readers. Until the permission is granted, the summary shows the command that grants it. To grant it, do one of these:

- With Shizuku running, turn on the setting. SandFly grants the permission through Shizuku. See [](#updates).
- Run the command with adb. To copy the command, open the actions menu on the setting and choose **Copy command**. Then return to **Audio output**. Without Shizuku, the switch is unavailable until the permission is granted.

SandFly uses the proximity sensor to detect the phone at your ear.

**Reset accessibility audio after calls** moves SandFly audio back from the earpiece when Android leaves accessibility audio on the earpiece after a call, such as a call in a messaging app. It's off by default, requires Android 13 or later, and runs through Shizuku or root. Turning it on asks Shizuku to allow SandFly. Without Shizuku, the summary shows an adb command that resets the audio once; open the actions menu on the setting and choose **Copy command** to copy it.

### Reading controls for sound and volume {#sound-reading-controls}

These reading controls change sound and volume settings with a swipe up or down. Add them in **Reading controls**. See [](#commands).

- **Audio output device**: switches between the choices of **Audio output device**.
- **Speech volume**: switches between the volumes of **Volume control for speech**.
- **Audio ducking**: turns **Audio ducking** on or off.
- **Accessibility volume**: raises or lowers the accessibility volume.
- **Media volume**, **Call volume**, **Ring volume**, **Notification volume** and **Alarm volume**: raise or lower each volume one step and say the new volume. Changing the ring or notification volume to or from silent needs **Do Not Disturb access**.
- **Sound mode**: sets the ringer to sound, vibrate or mute. Turning mute on or off needs **Do Not Disturb access**.
