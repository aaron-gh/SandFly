## Watches and TVs {#devices}

SandFly runs on Wear OS watches and on Android TV and Google TV. This chapter describes how SandFly differs on them from phones. Everything else in this guide applies to them as well, except features that need a touch screen, a phone, or hardware that the device lacks.

### Wear OS watches {#devices-watch}

Watches have a SandFly build of their own, `sandfly-wear.apk`. It has the same settings as phones, except settings for features that watches don't support.

#### Install SandFly on a watch {#devices-watch-install}

You install `sandfly-wear.apk` from a computer with adb.

1. Download `sandfly-wear.apk` from the [latest release](https://github.com/aaron-gh/SandFly/releases/latest) to the computer.
2. On the watch, turn on the developer options, then turn on **ADB debugging** and wireless debugging. The steps depend on the watch. On most watches, tap the build number in the **About** settings seven times to show the developer options.
3. On the computer, connect to the watch with `adb connect` and the address that the wireless debugging setting of the watch shows. Some watches first need to be paired with `adb pair`.
4. Run:

   ```
   adb install sandfly-wear.apk
   ```

5. On the watch, turn on SandFly in the accessibility settings.

To open the SandFly settings on a watch, open **SandFly** in the apps list of the watch. The first time SandFly is turned on, it opens the watch tutorial.

#### Gestures on watches {#devices-watch-gestures}

Watch screens track at most two fingers, so gestures with three or four fingers aren't available. Two-finger swipes go to the app, where they scroll.

Watches use the default gestures of phones, except:

| Gesture | Command on watches |
| --- | --- |
| Double-tap and hold with 2 fingers | Open SandFly menu |
| Triple-tap and hold with 2 fingers | Turn speech on or off |
| Swipe left then up | Repeat last spoken phrase |
| Swipe right then up | Increase volume |
| Swipe right then down | Decrease volume |
| Swipe left then down | Unassigned |

For the default gestures of phones, see [](#appendix-gestures). To change a gesture, go to **Gestures**. See [](#gestures).

#### Settings and features on watches {#devices-watch-settings}

On watches:

- The reading controls include **Scroll** and **Accessibility volume** by default. See [](#reading).
- The SandFly menu scrolls, so that every item can be reached. **Describe screen** isn't in the menu by default. The item that turns off SandFly is named **Turn off**. See [](#commands).
- **New features in SandFly** is named **What's new**.
- Pause SandFly isn't available, and **Resume SandFly** isn't shown. See [](#pause).
- **Practice gestures** isn't in **Help & feedback**. Use **Practice gestures** at the top of **Gestures**, which opens the gesture practice of the watch tutorial.
- **Gesture trainer** and **Keyboard practice** aren't in **Help & feedback**.
- **Version history** and **Privacy policy** show the address of the page when the watch has no browser.

These features aren't available on watches, and their settings aren't shown: scripts, macros, custom menus, custom reading controls, voice commands, direct touch, back tap, shake, edge and corner gesture settings, the braille keyboard, the handwriting keyboard, the SandFly keyboard, on-device AI, **Use SandFly as the wallpaper**, **Speaker when away from your ear**, **Reset accessibility audio after calls**, **Volume key shortcuts with the screen off**, and **Show instructions for turning off SandFly**.

For notifications on Samsung watches, see [](#notifications).

#### Updates on watches {#devices-watch-updates}

A watch updates to new builds of `sandfly-wear.apk`, as described in [](#updates). The watch downloads the update over its own Wi-Fi when Wi-Fi is available, not through the phone, and shows the progress in a notification. If the install prompt doesn't open, such as when the screen turned off during the download, tap the notification **SandFly update downloaded**.

### Android TV {#devices-tv}

The same `sandfly.apk` as for phones installs on Android TV and Google TV.

#### Install SandFly on a TV {#devices-tv-install}

1. On the TV, turn on the developer options and network debugging. The steps depend on the TV.
2. On a computer on the same network, connect to the TV with `adb connect` and the IP address of the TV.
3. Download `sandfly.apk` from the [latest release](https://github.com/aaron-gh/SandFly/releases/latest) and run:

   ```
   adb install sandfly.apk
   ```

4. On the TV, turn on SandFly in the accessibility settings. On most TVs, they are in **Settings** > **Device Preferences** > **Accessibility**.

On many TVs, pressing and holding Back and Down on the remote together for 3 seconds turns SandFly on or off. This is the accessibility shortcut of the TV, which you can also set in the settings of the TV.

To open the SandFly settings, choose **SandFly settings** in the apps on the TV home screen.

#### Use the remote {#devices-tv-remote}

On a TV, the remote moves focus:

- The arrow buttons move focus to the next item up, down, left or right.
- Select activates the focused item. When the focused item can't be activated, select goes to the app.
- Press and hold select to touch and hold the focused item.
- On a slider, select starts slider mode, and SandFly says "Slider mode started". The arrow buttons then change the value. Press select again to end slider mode.
- With **Developer settings** > **Enable node tree debugging** on, the search button writes the tree of items on the screen to the system log. See [](#help-developer).

SandFly doesn't handle the arrow and select buttons while the on-screen keyboard is shown. For physical keyboards, see [](#keyboards).

#### Settings and features on TVs {#devices-tv-settings}

On TVs:

- Settings for features that need a touch screen or a phone aren't shown, such as **Gestures**, **Gesture trainer**, **Direct touch**, **Status readout**, **Use SandFly's overview**, **Use SandFly's notification list**, **Speaker when away from your ear** and **Image and screen descriptions**. The **SandFly menu** and **Reading controls** settings screens aren't shown either.
- Pause SandFly isn't available, and **Resume SandFly** isn't shown. See [](#pause).
- The first item in **Help & feedback** opens the TV tutorial where the TV has one. Otherwise it is named **Help**.
- TVs often don't show notifications, so the update notification may not appear. To update, go to **Updates and installation** > **Check for updates**. See [](#updates-check).
