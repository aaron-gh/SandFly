## Touch {#touch}

**Touch** in SandFly settings sets how touching the screen focuses and activates items, when touch is locked, and which apps get touches directly. Gestures are described in [](#gestures), and reading the screen by touch in [](#reading).

### Focus and activate items {#touch-activation}

By default, touching an item focuses it and SandFly reads it, and a double-tap anywhere on the screen activates the focused item. The settings in this section change that.

#### Focus delay {#touch-focus-delay}

**Touch** > **Focus delay** sets how long the finger rests on an item before SandFly focuses it and reads it, from 0.15 to 2 seconds. The default is 0.15 seconds. A shorter delay makes SandFly speak sooner after you touch the screen. Needs Android 13 or later.

**Focus delay** is also a reading control, off by default; see [](#commands).

#### Single-tap to activate {#touch-single-tap}

With **Touch** > **Single-tap to activate** on, a single tap on the focused item activates it, without a double-tap. It's off by default. It needs explore by touch, and isn't on TVs.

#### Lift to activate {#touch-lift-to-activate}

With lift to activate, lifting the finger after touching an item activates it, as lifting the finger types a key of the on-screen keyboard. **Touch** > **Lift to activate** has three choices:

- **Disabled**: the default.
- **Only on navigation bar**: only the Back, Home and Overview buttons.
- **Entire screen**: every item.

**Lift to activate** is also a reading control, off by default, so you can change it with a swipe up or down; see [](#commands).

#### Cancel a lift activation {#touch-lift-cancel}

While **Lift to activate** is on, there are two ways to lift the finger without activating the item:

- Slide the finger off the bottom of the screen. Lifting in the bottom 3.5 mm of the screen activates nothing, also for keys of the keyboard that type when you lift. SandFly plays a sound and a vibration when the finger reaches the bottom edge.
- Hold the finger on the item. **Touch** > **Hold to cancel** sets how long: **1 second**, **2 seconds**, **3 seconds** or **5 seconds**. After that time on one item, lifting the finger activates nothing, and SandFly plays the same sound and vibration. The default is **Off**. Keys of the keyboard that type when you lift keep their own touch and hold.

#### Tap to activate {#touch-tap-to-activate}

With **Touch** > **Tap to activate** on, a quick tap that lifts before the item under the finger is focused activates that item. While focus has moved in the last 3 seconds, a tap waits for a second tap first, so a double-tap still activates the focused item. It's off by default.

#### Navigation bar buttons {#touch-navigation-bar}

With **Touch** > **Tap to activate navigation bar buttons** on, touches on the navigation bar go straight to Android: a single tap presses a button, and holding Home holds it. It's off by default and needs Android 11 or later. While it's on, **Lift to activate** doesn't apply to the navigation bar.

SandFly doesn't read the buttons as you touch them, but speaks the button that a tap pressed, such as "Back" or "Home". To turn this off, turn off **Touch** > **Speak navigation bar buttons**, which is on by default.

#### Activate with a tap {#touch-activate-with-tap}

Some controls ignore the activation that SandFly sends and react only to a finger on the screen. The **Activate with a tap** command, unassigned by default, taps the middle of the focused item instead. Assign it to a gesture or keyboard shortcut; see [](#gestures). A script can do the same for chosen items whenever they are activated; see [](#scripts). Both need Android 11 or later; otherwise the item is activated as usual.

For apps where activating an item does nothing, turn the app on under **Touch** > **Double tap taps the screen**, or run the **Double tap taps the screen in this app** command, unassigned by default, in the app. Activating an item in that app then taps it. The command says "Double tap taps the screen on" or "Double tap taps the screen off", or "No app in front" when no app is in front. This also needs Android 11 or later, and isn't on watches or TVs.

#### Speech while you touch the screen {#touch-speech}

By default, touching the screen stops speech. To keep speech going until touch focuses an item, turn off **Speech** > **Stop speech when touching the screen**. See [](#speech).

### Lock touch {#touch-lock}

While touch is locked, touches and gestures on the screen do nothing, and nothing reaches the app. Keys, volume key commands, fingerprint gestures, back tap and shake still work. Commands that touch the screen, such as **Activate with a tap**, **Hold a touch on the focused item** and touches in macros, still reach the app. Touch lock needs Android 13 or later and **Developer settings** > **Handle gestures in SandFly**.

To lock or unlock touch, use the **Lock or unlock touch** command. SandFly says "Touch locked" or "Touch unlocked". Assign the command to a volume key press, a keyboard shortcut or a back tap, so that you can unlock touch without the screen; see [](#buttons) and [](#gestures-back-tap). Pressing both volume keys together always unlocks touch.

**Touch** > **Lock touch automatically** locks touch without the command:

- **Never**: the default.
- **When the screen turns on**: each time the screen turns on. Unlock with the command or both volume keys.
- **On the lock screen**: until you unlock the phone.
- **While the proximity sensor is covered**: while the screen is on and something covers the proximity sensor, such as an ear or a pocket.

SandFly speaks nothing when touch locks automatically.

**Touch** > **Touches while touch is locked** sets what a touch on the locked screen gives: **Nothing**, the default, **Sound** or **Vibration**.

Scripts can also lock touch for up to 10 seconds; see [](#scripts).

### Direct touch {#touch-direct-touch}

Some apps, such as audio games, need touches as they are made, but explore by touch takes the taps and swipes first. With direct touch, SandFly passes touches straight to the apps you choose, while SandFly stays on and keeps speaking. Direct touch needs Android 11 or later and isn't on watches or TVs.

#### Turn on direct touch for an app {#touch-direct-touch-apps}

1. Go to **Touch** > **Direct touch**.
2. Under **Apps**, turn on each app that gets direct touch.

When an app you chose comes to the front, SandFly says "Direct touch on", and when touch returns to SandFly, "Direct touch off".

The **Direct touch** screen has:

- **Direct touch**: turns the feature on or off for all apps. On by default.
- **Speak when direct touch changes**: on by default.
- **Vibrate when direct touch changes**: off by default.
- **Back up settings** and **Restore settings**: save the direct touch settings to a file and load them, such as on another device.
- **Apps**: the apps and on-screen keyboards, each with a check box.

The **Turn direct touch on or off for this app** command adds the app in front to the list or removes it, and turns on **Direct touch** when it turns an app on. Assign it to a volume key press, so that a sighted helper can use an app that doesn't work with SandFly while SandFly keeps speaking, and you can turn direct touch off without touching the screen. With SandFly or the notification shade in front, SandFly says "No app for direct touch".

To turn direct touch off and on again without leaving the app, add the **Direct touch** tile to quick settings.

#### When SandFly takes touch back {#touch-direct-touch-return}

Touch returns to SandFly when a dialog opens, another app opens on top, the notification shade or quick settings cover half of the screen or more, a text field is focused, or the screen turns off. Touch goes back to the app when they're gone. The on-screen keyboard always stays with SandFly, so you can explore it while the rest of the screen has direct touch.

#### Direct typing {#touch-direct-typing}

For an app that draws its own keyboard, turn on direct typing: in the **Apps** list, open the actions of the app and choose **Turn on direct typing**. The keyboard area then gets direct touch too, and the app in the list shows "Direct typing on". **Turn off direct typing** turns it off.

#### Keyboards that speak {#touch-direct-touch-keyboards}

A keyboard that speaks by itself, such as a braille keyboard app, needs touches in every app. Turn it on in the **Apps** list. While it shows, touches on it go straight to it in any app, and the rest of the screen stays with SandFly. A keyboard that draws its keys in an accessibility overlay and has an accessibility service of its own turned on keeps the touch area it sets, without being in the list.

#### Apps that choose where they get direct touch {#touch-direct-touch-areas}

An app can declare that it needs direct touch. SandFly turns on such an app the first time it comes to the front; you can turn it off in the **Apps** list. An app can also take direct touch only on parts of its screen, such as a game board, while explore by touch works on the rest, and can stop direct touch for itself. These apps work only while they are turned on in the **Apps** list. For developers, the interface is described in [](#api-direct-touch).

### Fix stuck touch exploration {#touch-stuck}

On some phones, after a touch with several fingers, Android can stop touch exploration for every screen reader until the phone restarts. Swipes and taps still work, but moving a finger over the screen reads nothing.

**Touch** > **Fix stuck touch exploration**, on by default, detects this from exploring touches that find no item. To fix it, SandFly needs the DUMP permission:

- With Shizuku running, SandFly grants the permission when it starts, and when you turn the setting on; the first time, Shizuku shows a request to allow SandFly. See [](#updates).
- Without Shizuku, grant it once with adb. While the permission is missing, the setting shows the command, and its **Copy command** action copies it:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
```

With the permission, SandFly says "Fixing touch exploration", ends the touches that Android left down, and says "Touch exploration fixed". A touch during the fix goes straight to the app. This works on phones that list these touches in `dumpsys input`, such as Samsung phones.

Without the permission, or on other phones, SandFly can only say that touch exploration may be stuck, and you need to restart the phone. Exploring an area with nothing to read, such as a game, also finds no item, so the warning can be wrong, at most once every 10 minutes.

When the screen turns off during a touch, Android can keep the lifted fingers as down, and then only touch exploration works: gestures and double-taps do nothing. SandFly detects this when the screen turns off during a touch, or when two touches in a row end in touch exploration, and turns explore by touch off and on again. A touch that ends because the next finger goes down, as in fast typing, doesn't count. This doesn't depend on **Fix stuck touch exploration** or the DUMP permission.

### Reset all delay settings {#touch-reset-delays}

**Touch** > **Reset all delay settings** sets these settings back to their defaults, after the **Reset delay settings?** dialog:

- **Touch** > **Single-tap to activate** and **Focus delay**.
- **Display** > **Turn off animations**.
- **Typing** > **Typing method**, **Touch and hold delay when typing**, **Typing focus delay** and **Announce keyboard keys faster**.
- **Navigation** > **Speak items before they scroll into view**.
- Whether the **Focus delay** and **Typing focus delay** reading controls are shown.

See [](#typing) for the typing settings.
