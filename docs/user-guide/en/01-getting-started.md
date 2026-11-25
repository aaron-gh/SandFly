## Get started {#getting-started}

SandFly is a screen reader for Android. It reads what is on the screen with text-to-speech, plays sounds and vibrations, and changes how touch gestures work, so that you can use the device without seeing the screen. SandFly can also show the screen on a braille display and take input from a braille keyboard on the screen or a physical keyboard.

### Devices and Android versions {#getting-started-devices}

SandFly runs on:

- Phones and tablets with Android 8 or later.
- Wear OS watches. See [](#devices-watch).
- Android TV and Google TV. See [](#devices-tv).

Some features need a later version of Android, and some need Shizuku or root, which give SandFly permissions that Android grants only through adb. The sections that describe those features say so. See [](#updates-shizuku). SandFly Pro is a subscription that adds AI features. See [](#pro).

### Install SandFly {#getting-started-install}

SandFly is published as an APK file on the [SandFly releases page](https://github.com/aaron-gh/SandFly/releases/latest). `sandfly.apk` is for phones, tablets and TVs, and `sandfly-wear.apk` is for watches.

To install SandFly on a phone or tablet:

1. On the phone, open the [latest release](https://github.com/aaron-gh/SandFly/releases/latest) in a browser.
2. Download `sandfly.apk`.
3. Open the downloaded file.
4. If Android asks, allow the browser or file manager to install apps, then go back and open the file again.
5. Choose **Install**.

SandFly installs as a separate app, with settings of its own, next to any other screen reader on the device. After that, SandFly installs its own updates. See [](#updates).

Note: On Android 13 and later, Android can block accessibility services that were installed from a browser or file manager. If SandFly can't be turned on in the accessibility settings, open the app info for SandFly in the Android settings, open the **More** menu, and choose **Allow restricted settings**. The names of these items can be different on some phones.

### Turn on SandFly {#getting-started-turn-on}

Turn off any other screen reader before you turn on SandFly. Two screen readers that are on at the same time both read the screen and both handle gestures.

1. Open the Android **Settings** app.
2. Go to **Accessibility**. On some phones, SandFly is in a subsection such as **Installed apps** or **Downloaded apps**.
3. Choose **SandFly**.
4. Turn on the switch, then confirm.

SandFly says "SandFly on". The first time, SandFly opens the gesture layout dialog and the tutorial. See [](#getting-started-first-start).

#### Turn SandFly on and off with the accessibility shortcut {#getting-started-shortcut}

Android can turn SandFly on and off with an accessibility shortcut, such as holding both volume keys for a few seconds, or the accessibility button. To set one up, go to **Settings** > **Accessibility** > **SandFly** in the Android settings and turn on the shortcut for SandFly. The shortcuts that are available depend on the version of Android and the phone.

### What happens the first time {#getting-started-first-start}

The first time SandFly is turned on, it opens these screens in order:

1. The **Choose gestures** dialog, on a device with a touch screen. See [](#getting-started-gesture-layout).
2. The SandFly tutorial. See [](#getting-started-tutorial).

If you used an earlier version of SandFly, SandFly opens the pages about the new features of the update instead of the tutorial. See [](#getting-started-new-features).

If Backtalk Aaron is installed, SandFly first copies its settings and custom labels. See [](#getting-started-backtalk).

#### Choose a gesture layout {#getting-started-gesture-layout}

The **Choose gestures** dialog has two buttons:

- **SandFly gestures**: the default SandFly gestures. Two-finger swipes go to the app, where they scroll.
- **VoiceOver gestures**: gestures that match VoiceOver on iPhone. Two-finger swipes run SandFly commands, such as reading from the top of the screen.

Closing the dialog without a choice selects SandFly gestures. If you leave the dialog with Home, it opens again the next time SandFly starts. The tutorial opens after the choice and teaches the gestures of the layout you chose.

To change the layout later, go to **Gestures** > **Gesture layout**. See [](#gestures).

#### The SandFly tutorial {#getting-started-tutorial}

The SandFly tutorial teaches the basic gestures with lessons that you practice on the screen. It opens the first time SandFly is turned on. To open it later, go to **Help & feedback** > **SandFly tutorial**, or use the **Open tutorial** command.

The tutorial has 16 lessons in five chapters:

- **Basics**: Welcome, Exploring by touch, Moving between items, Activating items, Pausing speech.
- **Reading**: Reading controls, Headings, Reading continuously.
- **Controls and text**: Scrolling lists, Sliders, Actions, Typing.
- **Getting around**: Back, Home and notifications, SandFly menu, Useful commands.
- **Practice**: Gesture practice.

Each lesson gives an instruction, waits until you have done it with your own gestures, then gives the next one. The instruction is the first item after the name of the lesson. SandFly speaks each instruction, also while other audio plays, and repeats it after 25 seconds without a gesture or touch, up to three times. The instructions name the gestures that are assigned to each action in your gesture settings. If an action has no gesture, the step says so, and **Skip step** goes on without it.

When a lesson is complete, SandFly says which gesture starts the next lesson and moves focus to **Next lesson**, followed by the name of the lesson. The last lesson has **Finish tutorial**.

In a lesson, gestures that would leave the tutorial, such as Back, Home, recent apps and notifications, don't leave it. SandFly says what the gesture would have done. Turning speech off and pausing SandFly work the same way. Back still closes the keyboard and menus. To choose another lesson or leave the tutorial, use **Contents**, which lists the lessons, says which are done, and opens any of them. **Close tutorial** in **Contents** closes the tutorial.

The tutorial opens again at the lesson you were on, or at the next lesson if that one is done.

In **Gesture practice**, each gesture says its name and the command assigned to it, without running the command. Moving to the next and previous item and activating still work, so that you can reach **End practice**. To open this lesson directly, use **Help & feedback** > **Practice gestures**, or **Practice gestures** at the top of the **Gestures** screen.

Note: The tutorial only takes gestures that SandFly recognizes. If **Handle gestures in SandFly** is off in **Developer settings**, Android recognizes the gestures, and the navigation gestures of Android can leave the tutorial. The lessons on sliders and actions need **Reading controls** > **Change reading control automatically**, which is on by default.

#### Keyboard tutorial {#getting-started-keyboard-tutorial}

If a physical keyboard is connected when SandFly starts for the first time, SandFly opens the **SandFly keyboard tutorial** instead. It teaches the keyboard shortcuts of your keymap in 15 lessons, including **Keyboard shortcut list**, **Browse mode** for web pages, and **Keyboard practice**. When a device has both a touch screen and a physical keyboard, **SandFly tutorial** opens a dialog with **Touch tutorial** and **Keyboard tutorial**. See [](#keyboards).

Watches and TVs have tutorials of their own. See [](#devices).

#### New features pages {#getting-started-new-features}

After an update that adds features, SandFly opens pages that describe them the next time it starts. To read them again, choose **New features in SandFly** at the top of the main SandFly settings screen.

### Basic gestures {#getting-started-basics}

These are the gestures of the SandFly layout. The VoiceOver layout assigns some of them differently. For every default gesture, see [](#appendix-gestures). To change a gesture, see [](#gestures).

- **Explore by touch**: Move one finger on the screen. SandFly reads the item under your finger.
- **Move to the next or previous item**: Swipe right or swipe left.
- **Activate the item**: Double-tap anywhere on the screen. Double-tap and hold to touch and hold the item.
- **Scroll**: Swipe up or down with two fingers. SandFly also scrolls a list when you swipe past the last item on the screen.
- **Stop speech**: Tap with two fingers. Tap with two fingers again to continue. Double-tap with three fingers turns speech off and on.
- **Reading controls**: Swipe down with three fingers, or turn two fingers clockwise on the screen, to choose the next reading control, such as **Words**, **Headings** or **Speech rate**. Swipe up with three fingers, or turn two fingers counterclockwise, for the previous one. Then swipe down or up to move by the chosen item or change the chosen setting. See [](#reading).
- **SandFly menu**: Tap with three fingers, swipe up then right, or swipe down then right. See [](#commands).
- **Back**: Swipe down then left, or scrub back and forth with two fingers.
- **Home**: Swipe up then left, or swipe up from the bottom edge.
- **Recent apps**: Swipe left then up.
- **Notifications**: Swipe right then down.
- **Search the screen**: Swipe left then down.

For how SandFly reads and how to move through text, lists and web pages, see [](#reading) and [](#web).

### Open SandFly settings {#getting-started-settings}

To open the SandFly settings, do one of the following:

- Open **SandFly** in the apps list of the device. The app icon opens the settings whether SandFly is on or off, so you can change settings before you turn SandFly on.
- Choose **SandFly settings** in the SandFly menu.
- In the Android settings, go to **Accessibility** > **SandFly** > **Settings**.

The main settings screen lists the settings screens: Speech, Verbosity, Sound and vibration, Audio output, Touch, Gestures, Keyboard shortcuts, Volume keys, Commands, Reading controls, SandFly menu, Custom menus, Macros, Voice commands, Navigation, Typing, Braille, Display, Notifications, Translation, Image and screen descriptions, AI services, SandFly Pro, Scripts, Custom labels, Quick settings, Overview, Pause and turn off, Settings profiles, Back up and restore, Updates and installation, Shizuku and root, Help & feedback and Developer settings. Some screens are not shown on watches and TVs, or on devices that lack the hardware a feature needs.

#### Search the settings {#getting-started-search}

**Search**, at the top of the main settings screen, finds settings and commands on every settings screen.

1. Choose **Search**.
2. Type one or more words, then search.
3. Choose a result.

A result has every word you typed in its title or description, also inside a word. Case and accents are ignored. Each result shows its title and the screens and categories that lead to it, such as **Speech** > **Stopping speech**. Choosing a setting opens its screen with the setting focused. Choosing a command opens it in **Commands**.

The **Gestures** and **Keyboard shortcuts** screens have a **Search** of their own, which finds a gesture or shortcut by its name or by the command assigned to it.

### The user guide {#getting-started-user-guide}

This guide opens from **Help & feedback** > **User guide** in the SandFly settings. It starts with a table of contents, and links in the text go to the sections they name. The guide is shown in the language of SandFly. Chapters that aren't translated into that language are shown in English.

### Move from Backtalk Aaron {#getting-started-backtalk}

SandFly replaces Backtalk Aaron, which gets no more updates. The last build of Backtalk Aaron offers SandFly as an update, with the notification or dialog **Backtalk Aaron is now SandFly**.

1. In Backtalk Aaron, tap the update notification, or choose **Check for updates**, then **Install**.
2. SandFly installs next to Backtalk Aaron, and the Android accessibility settings open.
3. Turn on SandFly. SandFly copies the settings and custom labels of Backtalk Aaron, and Backtalk Aaron turns itself off.
4. Tap the notification **Settings copied from Backtalk Aaron** to uninstall Backtalk Aaron.

SandFly gets stable releases. To get development builds as well, turn on **Updates and installation** > **Development builds**. See [](#updates-channels).

On-device AI models aren't copied. Download them again in SandFly. See [](#descriptions). If you used the braille keyboard, turn it on again for SandFly. See [](#braille).

If you use an accessibility shortcut, grant SandFly permission to change secure settings before you turn it on. SandFly then moves the accessibility shortcuts, and the braille keyboard, from Backtalk Aaron to itself. From a computer, run:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
```

Without this permission, set the shortcut again for SandFly in the Android accessibility settings.

To use a settings file exported from Backtalk, see [](#profiles).
