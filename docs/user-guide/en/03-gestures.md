## Gestures {#gestures}

SandFly runs a command for each gesture you make on the touch screen: swipes, taps, slides from the edges of the screen, two-finger turns and more. Most gestures can run any command, and you can change them in **Gestures**. This chapter describes the kinds of gestures, the settings on the **Gestures** screen, gestures in single apps and gesture profiles.

The commands that each gesture runs by default are listed in [](#appendix-gestures). The commands themselves are described in [](#commands).

### How SandFly recognizes gestures {#gestures-recognition}

A touch that rests on the screen or moves slowly is touch exploration: SandFly reads the item under the finger. A touch that moves quickly, or taps, is a gesture. See [](#reading) for touch exploration.

On Android 13 and later, SandFly recognizes gestures itself. Many gestures in this chapter need this: the rotor, edge gestures, corner gestures, the scrub, pinch and spread, swipe and hold, three-part swipes, two-stroke swipes, tap then swipe, two-finger angle gestures, the one-finger triple-tap and quadruple-tap, the quadruple-tap with three fingers, the two-finger double-tap at the top and bottom of the screen and the four-finger taps on the top or bottom half of the screen. The timing settings on the **Gestures** screen need it too. Gestures with two or more fingers need Android 11 or later.

Recognition by SandFly is controlled by **Developer settings** > **Handle gestures in SandFly**, which is on by default. When you turn it off, Android recognizes gestures, and the gestures and settings above are not available. Turn SandFly off and on again after you change this setting.

### Taps {#gestures-taps}

- **One finger**: double-tap activates the focused item, and double-tap and hold touches and holds it. These gestures can't be changed. **Triple-tap** and **Quadruple-tap**, in the **1 finger** category, do nothing by default and work anywhere on the screen.
- **Two, three and four fingers**: a tap, double-tap and triple-tap with each number of fingers, and double-tap and hold. Two and three fingers also have triple-tap and hold, and three fingers have tap and hold and a quadruple-tap.
- **Two fingers at the top and bottom**: **Double-tap with 2 fingers, top and bottom** is a separate gesture. While it has a command, a two-finger double-tap with one finger in the top third of the screen and the other in the bottom third runs it instead of **Double-tap with 2 fingers**. Top and bottom follow the screen as displayed, also in landscape.
- **Four fingers on half of the screen**: **Tap with 4 fingers on top half** and **Tap with 4 fingers on bottom half** are separate gestures. While one of them has a command, a four-finger tap on that half runs it instead of **Tap with 4 fingers**.

A tap with two, three or four fingers runs its command as the fingers lift, unless a longer gesture that starts with the same taps has a command, such as a further tap, a tap and hold, or a tap then swipe. Then SandFly waits for the next tap. When any finger moves during a multi-finger tap, the touch is no longer a tap. A quadruple-tap with three fingers always runs at once.

A one-finger double-tap runs as the finger lifts, unless **Triple-tap** or **Quadruple-tap** has a command. Then SandFly waits for a further tap, and a double-tap activates after that wait. A triple-tap waits only while **Quadruple-tap** has a command. In a corner with a corner gesture, taps are corner taps; see [](#gestures-corners).

**Time between one-finger taps** sets how long SandFly waits for a further one-finger tap. See [](#gestures-timing).

### Swipes {#gestures-swipes}

A swipe moves one or more fingers quickly across the screen in one direction. One-finger swipes left and right move to the previous and next item, and swipes up and down change the value of the current reading control. See [](#reading).

Two-finger swipes either scroll the app or run SandFly commands; see [](#gestures-two-finger-swipes). Three- and four-finger swipes run SandFly commands.

#### Back-and-forth and angle gestures {#gestures-two-direction}

A back-and-forth gesture swipes one way and back without lifting the finger, such as **Swipe up then down**. An angle gesture swipes one way and then turns at a right angle, such as **Swipe down then right**. They are in the **1 finger back-and-forth** and **1 finger angle** categories of the **Gestures** screen.

**2 finger angle** has eight angle gestures with two fingers, such as **Swipe up then right with 2 fingers**. They do nothing by default, work only while **Handle two-finger swipes in SandFly** is on, and are hidden while it is off. Two fingers moved one way and back are the scrub, so there are no two-finger back-and-forth gestures.

**Recognize swipes before lifting** turns off and hides the one-finger back-and-forth and angle gestures and the two-finger angle gestures. See [](#gestures-swipes-before-lift).

#### Three-part swipes {#gestures-three-part}

A three-part swipe moves one finger in three directions without lifting it, each part turning from the one before, such as **Swipe right, up, then right** or **Swipe up, down, then up**. The 36 gestures are in the **Three-part swipes** category of the **Gestures** screen. They do nothing by default, and a three-part swipe without a command isn't recognized. Each part needs the length of a part of an angle gesture. **Recognize swipes before lifting** turns off and hides them. Not on watches.

#### Swipe and hold {#gestures-swipe-and-hold}

Each one-finger swipe, back-and-forth swipe and angle swipe has a version that ends with the finger resting on the screen, such as **Swipe right and hold** or **Swipe left then up and hold**. Swipe, then keep the finger still: the command runs when the hold time ends, before you lift the finger. Lifting sooner makes the ordinary swipe.

A pause shorter than the hold time where an angle or back-and-forth swipe turns stays part of that swipe. A longer pause is the swipe and hold of the first direction, when that has a command.

These 16 gestures are at the end of the **1 finger**, **1 finger back-and-forth** and **1 finger angle** categories and do nothing by default. A swipe and hold without a command isn't recognized, so swipes work as before until you assign one. **Hold time after a swipe** sets the rest; see [](#gestures-timing). Not on watches.

#### Two-stroke swipes {#gestures-two-stroke}

A two-stroke swipe is two one-finger swipes with the finger lifted between them, such as **Swipe right, lift, then swipe up**. Either swipe can be a one-direction, back-and-forth or angle swipe, such as **Swipe up then left, lift, then swipe right** or **Swipe up, lift, then swipe right then down**, which makes 256 gestures, including the same swipe twice. They do nothing by default. On the **Gestures** screen, the 16 made of one-direction swipes are in the **Two-stroke swipes** category, those that start with a one-direction swipe and end with a back-and-forth or angle swipe are in **Two-stroke swipes ending with back-and-forth or angle**, and the others are in a category for each first swipe, such as **Two-stroke swipes starting with Swipe up then left**.

They work only while **Time between two-stroke swipes** is set; see [](#gestures-timing). While it's **Off**, the two-stroke swipe categories are hidden. The second swipe must start within that time after the first lifts. After a swipe, SandFly waits for a second swipe only when a two-stroke swipe that starts with that swipe has a command, in the gesture layout, the gesture profile in use or the gestures of the app; other swipes run at once. When the time passes, or the next touch is a tap, touch exploration or a gesture with more fingers, the first swipe runs as an ordinary swipe.

A two-stroke swipe that starts with a back-and-forth or angle swipe makes no one-direction swipe wait for a second swipe, so commands can go on two-stroke swipes while one-direction swipes run at once. A back-and-forth or angle swipe in a two-stroke swipe with a command is recognized even when it has no command of its own. **Recognize swipes before lifting** turns off and hides the two-stroke swipes with a back-and-forth or angle swipe. Not on watches.

### Rotor {#gestures-rotor}

Turn two fingers on the screen like a dial to choose a reading control: clockwise for the next reading control and counterclockwise for the previous one. Then swipe up or down to change its value. Reading controls are described in [](#commands).

Each step of the turn moves one reading control, so you can keep turning to move further. **Gestures** > **Rotor turn per step**, under **2 fingers**, sets the step from 15 to 60 degrees. The default is 30 degrees, a twelfth of a turn. Choose a larger step if you often move past the reading control you want. Scrolling with two fingers doesn't turn the rotor.

You can assign other commands to **Rotate clockwise with 2 fingers** and **Rotate counterclockwise with 2 fingers**.

### Scrub, pinch and spread {#gestures-scrub-pinch-spread}

To scrub, move two fingers quickly one way across the screen and back, each stroke about a centimeter long. By default, the scrub goes back. Two fingers moved one way only still scroll. While a scrub is still possible, a sideways scroll with two fingers can wait up to about half a second before it starts.

**Pinch with 2 fingers**, **Spread with 2 fingers** and the same gestures with 3 and 4 fingers do nothing by default. A pinch or spread counts once the fingers have moved about half a centimeter toward or away from their middle, without moving together or turning much. While a two-finger pinch or spread has a command, a two-finger movement that pinches more than it moves waits before it reaches the app, so apps that zoom with two fingers may not zoom until both are set to **Unassigned**.

The scrub, pinch and spread aren't on watches.

### Edge gestures {#gestures-edges}

Edge gestures start at the very edge of the screen, such as on the gesture navigation bar or the status bar. They are in the **Edges** category of the **Gestures** screen and aren't on watches.

#### Slide in from an edge {#gestures-edges-slide-in}

Slide one finger in from an edge:

- **Swipe up from the bottom edge**: after about 1 cm, SandFly plays a click like a home button. Lift there to go to the home screen. Keep sliding about 4 cm more to a second click, and lift there for **Long swipe up from the bottom edge**, which opens the recent apps.
- **Swipe down from the top left** opens notifications and **Swipe down from the top right** opens quick settings, after the same first click. **Long swipe down from the top left** and **Long swipe down from the top right**, at the second click, do nothing by default.
- Right from the left edge and left from the right edge. The top half and bottom half of each side edge are separate gestures, each with a short and a long swipe, such as **Swipe right from the left edge, top half**. They do nothing by default.

Only slides that have a command click. A slide without a command at the first click stays an ordinary swipe until the second click; a long swipe without a command has no second click. Lifting before the first click makes an ordinary swipe.

Sliding back from the second click to the first plays one short tap, and sliding back to the edge plays two; lifting at the edge cancels the gesture. Touching the edge, the first click and the second click each play a sound that rises with each stage. The sounds and vibrations can be turned off in **Individual sounds and vibrations**; see [](#sound).

A slide that starts away from the edge, or that takes longer than about half a second to reach the first click, is an ordinary swipe or touch exploration. A finger that rests at the edge for 0.2 seconds starts touch exploration.

In a corner, the direction of the slide chooses the gesture. A slide that fits two gestures uses the top or bottom edge, and a slide in comes before a slide along an edge.

#### Slide along an edge {#gestures-edges-slide-along}

You can also slide one finger along an edge: up or down the left or right edge, or left or right along the top or bottom edge, such as **Swipe up along the left edge**. Start at the very edge and keep the finger near it. The command runs when you lift after sliding the distance to the first click. These gestures do nothing by default and play no edge sounds or vibrations.

#### Scroll along an edge {#gestures-edges-scroll}

To scroll apps with one finger along an edge, turn on **Scroll with one finger along the left edge**, **Scroll with one finger along the right edge**, **Scroll with one finger along the top edge** or **Scroll with one finger along the bottom edge** under **Edges**. All are off by default. A slide along that edge then moves the content of the app with the finger, as a two-finger slide does: up and down along the left and right edges, left and right along the top and bottom edges.

While an edge scrolls, the gestures that slide along it are not used. Slides in from the edge stay gestures, and so do slides up and down from the corners.

#### Edge menus {#gestures-edge-menus}

The left and right edges can each be a menu of commands. To set up an edge menu:

1. Go to **Gestures** > **Edges** > **Left edge menu** or **Right edge menu**.
2. Turn on **Use the left edge as a menu** or **Use the right edge as a menu**.
3. Choose **Add command**, and choose a command from the same list as for gestures. A menu has up to 20 commands.

Each command has **Change command**, **Move up**, **Move down** and **Remove**.

To use the menu, start at the very edge and slide up or down along it. The commands divide the length of the edge equally, from the top, so each command is always at the same place. SandFly speaks the name of each command with a tick when the finger reaches it, and lifting runs the command under the finger. Moving in from the edge more than three times the edge width speaks "Cancel", and lifting there runs nothing; moving back to the edge returns to the commands.

While an edge is a menu, the gestures that slide along it and scrolling along it are not used. Slides in from that edge stay gestures. A slide that starts on the keyboard is not a menu, so the keys at the edge type as usual.

#### Edge settings {#gestures-edge-settings}

Under **Edges** on the **Gestures** screen:

- **Edge width**: how far from the edge a touch can start, from 0.5 to 2 cm. The default is 0.5 cm.
- **Distance to first click**: how far you slide in from the edge, or along it, to the first click, from 0.5 to 2 cm. The default is 1 cm.
- **Distance from first to second click**: how far you slide on to the second click, from 1 to 5 cm. The default is 4 cm.
- **Speak command at each click**: at each click of a slide in from an edge, SandFly speaks the command that lifting there runs, such as "Home". Off by default.

### Corner gestures {#gestures-corners}

Each corner of the screen has five one-finger gestures, under **Corners**: tap, double-tap, triple-tap, double-tap and hold, and triple-tap and hold, such as **Double-tap top left corner**. They do nothing by default.

A corner with no command assigned works as usual. In a corner with a command assigned, taps that SandFly doesn't use are ordinary taps: a double-tap still activates. A double-tap waits for a third tap only while the corner has a triple-tap command. A touch that moves, adds a finger, or rests without a hold gesture is an ordinary touch, so swipes and edge gestures from the corner still work. Corner gestures don't start on the keyboard.

- **Corner size**: the size of the square at each corner, from 0.5 to 2 cm. The default is **Same as edge width**.
- **Corner tap time limit**: how long a tap in a corner can stay down before it counts as a hold, from 0.15 to 0.5 seconds. The default is **System default**.

Not on watches.

### Tap, then swipe {#gestures-tap-then-swipe}

The **Tap, then swipe** category has gestures that tap once, twice or three times with two, three or four fingers and then swipe with as many fingers, such as **Double-tap, then swipe up with 2 fingers**. It also has gestures that tap with two fingers and then swipe with one finger, such as **Tap with 2 fingers, then swipe up**. They do nothing by default.

The swipe is the next touch after the last tap and must start within 0.25 seconds of it. While one of these gestures has a command, the taps it starts with wait for the swipe. Not on watches.

### Back tap {#gestures-back-tap}

Tap the back of the phone two, three or four times with a finger to run **Double tap on back**, **Triple tap on back** or **Quadruple tap on back**, under **Back tap** on the **Gestures** screen. They do nothing by default. SandFly detects the taps with the accelerometer, only while the screen is on, SandFly isn't paused and a back tap has a command. Taps while you touch the screen don't count.

**Back tap sensitivity** sets how hard a tap must be: **Low** needs the hardest taps, **High** the lightest. The default is **Medium**. Raise it if taps are missed, and lower it if back taps run when you put the phone down or carry it.

Taps wait for a following tap only while a gesture with more taps has a command: a double tap runs sooner when **Triple tap on back** and **Quadruple tap on back** have no command. Back tap isn't on watches or TVs. Back taps still work while touch is locked; see [](#touch).

### Shake {#gestures-shake}

Shake the phone quickly back and forth to run **Shake**, under **Shake** on the **Gestures** screen. It does nothing by default. A shake is at least two swings each way within about two thirds of a second. Shaking that goes on runs **Shake** once; the next shake counts after the phone has been still for a moment. SandFly detects shakes with the accelerometer, only while the screen is on, SandFly isn't paused and **Shake** has a command.

**Shake sensitivity** sets how hard the phone must be shaken: **Low** needs the hardest shakes, **High** the lightest. The default is **Medium**.

Shake isn't on watches or TVs, and works while touch is locked.

### Fingerprint gestures {#gestures-fingerprint}

On phones whose fingerprint sensor reports swipes, the **Fingerprint gestures** category has **Swipe up on fingerprint sensor**, **Swipe down on fingerprint sensor**, **Swipe left on fingerprint sensor** and **Swipe right on fingerprint sensor**. The category is hidden on other phones. Gesture profiles don't change fingerprint gestures.

### The Gestures screen {#gestures-screen}

**Gestures** in SandFly settings has, from the top:

- **Search**: finds a gesture by its name or by the command it runs, and opens its list of commands.
- **Practice gestures**: opens a page where SandFly says what each gesture you make does. See [](#gestures-practice).
- **Reset gesture settings**: see [](#gestures-reset).
- **Gesture layout** and **Handle two-finger swipes in SandFly**: see [](#gestures-layout) and [](#gestures-two-finger-swipes).
- The timing settings: see [](#gestures-timing).
- **Recognize swipes before lifting**: see [](#gestures-swipes-before-lift).
- **Gestures in apps**, **Gesture profiles** and **App shortcuts**: see [](#gestures-in-apps), [](#gestures-profiles) and [](#gestures-app-shortcuts).
- The gesture categories: **1 finger**, **1 finger back-and-forth**, **1 finger angle**, **Edges**, **Corners**, **2 fingers**, **2 finger angle**, **3 fingers**, **4 fingers**, **Tap, then swipe**, **Fingerprint gestures**, **Back tap**, **Shake**, **Three-part swipes**, **Two-stroke swipes**, **Two-stroke swipes ending with back-and-forth or angle** and a **Two-stroke swipes starting with** category for each back-and-forth and angle swipe.

The categories are collapsed. Activate a category to expand it. Each gesture in a category shows the command it runs.

On a watch, the screen also has **Reserve gestures for Vibration Watch**; see [](#devices).

#### Gesture layout {#gestures-layout}

**Gesture layout** chooses the gestures that the **Gestures** screen starts from:

- **SandFly gestures**: the default gestures of SandFly.
- **VoiceOver gestures**: the gestures of VoiceOver on iPhone, such as a two-finger tap to pause speech, a two-finger double-tap for **Dictate, play or pause media**, three-finger swipes to scroll and a three-finger tap for **Speak position**. Gestures that VoiceOver doesn't have are **Unassigned**, and you can assign them.
- **BlackBerry gestures**: gestures similar to those of the BlackBerry 10 Screen Reader (BlackBerry 10 OS 10.3). Not every gesture is the same. Swipe right and left move by the navigation setting, and swipe up and down choose the next and previous navigation setting, such as characters, words or lines. Swipe down then left pauses speech, swipe down then right starts Quick Tasks, swipe down then up says the time and date, and swipe up then down says the status. A two-finger tap says the title of the screen, a two-finger double-tap is **Dictate, play or pause media**, which answers and ends calls, and a two-finger double-tap and hold ends a call. Two-finger swipe up and down scroll back and forward. A three-finger swipe down reads from the top. Gestures that the BlackBerry 10 Screen Reader doesn't have are **Unassigned**, and you can assign them. See [](#gestures-quick-tasks). These gestures are likely to be unfamiliar if you haven't used BlackBerry 10, so choosing them opens the **Use BlackBerry gestures?** dialog; select **Use** to confirm. They aren't in the **Choose gestures** dialog.

The first time SandFly starts, it opens the **Choose gestures** dialog, where you choose **SandFly gestures** or **VoiceOver gestures**. Each layout keeps its own changes, so switching back restores the gestures as you set them. Choosing a layout turns **Handle two-finger swipes in SandFly** on for **VoiceOver gestures** and **BlackBerry gestures**, and off for **SandFly gestures**. Gestures in apps and gesture profiles apply to every layout.

#### Quick Tasks {#gestures-quick-tasks}

**Quick Tasks** is similar to Quick Tasks mode of the BlackBerry 10 Screen Reader. SandFly says "Quick Tasks", and for 3 seconds these gestures run these commands:

| Gesture | Command |
| --- | --- |
| Swipe up, swipe down | Increase volume, decrease volume |
| Swipe right, swipe left | Increase speech rate, decrease speech rate |
| Two-finger swipe up | Repeat last spoken phrase |
| Two-finger swipe down | Spell last spoken phrase |
| Two-finger tap | Hide or show screen |

Each of these gestures gives another 3 seconds. Any other gesture ends Quick Tasks and runs its own command. When the time runs out, the phone vibrates. In the BlackBerry layout, Quick Tasks is swipe down then right. In other layouts, assign it on the **Gestures** screen; it's under **SandFly** on the **Commands** screen.

#### Handle two-finger swipes in SandFly {#gestures-two-finger-swipes}

With **Handle two-finger swipes in SandFly** off, two-finger swipes go to the app, which scrolls with them, and the four two-finger swipes on the **Gestures** screen show "not customizable". With it on, two-finger swipes run the commands assigned to them, and the **2 finger angle** gestures are available. This setting needs Android 11 or later.

#### Timing settings {#gestures-timing}

- **Time between one-finger taps**: how long SandFly waits for a further tap before it decides that a one-finger tap gesture is finished, from 0.1 to 2 seconds. The default is 0.25 seconds. A shorter time makes one-finger double-taps respond faster, but a slow double-tap can then count as two taps. With a longer time, two taps on an item within that time activate it, and corner taps and multi-finger single taps wait longer. Taps with two or more fingers always allow at least 0.25 seconds between taps.
- **Hold time after a swipe**: how long the finger rests after a swipe for a swipe and hold gesture, from 0.1 to 0.5 seconds. The default is 0.25 seconds. See [](#gestures-swipe-and-hold).
- **Gesture start time**: how long a one-finger swipe can take to move its first centimeter before the touch becomes touch exploration, from 0.15 to 0.3 seconds. The default is 0.15 seconds. Choose a longer time if slow swipes start touch exploration.
- **Time between two-stroke swipes**: how long after a swipe the second swipe of a two-stroke swipe can start: **Off**, 0.25, 0.5, 1, 1.5 or 2 seconds. The default is **Off**, which turns two-stroke swipes off and hides their categories. See [](#gestures-two-stroke).

These settings need gesture recognition by SandFly; see [](#gestures-recognition). **Hold time after a swipe**, **Gesture start time** and **Time between two-stroke swipes** aren't on watches.

#### Recognize swipes before lifting {#gestures-swipes-before-lift}

With **Recognize swipes before lifting** on, a one-finger swipe runs its command once the finger has moved 1 cm, before you lift it. Back-and-forth and angle gestures start like a single swipe, so this setting turns off and hides the **1 finger back-and-forth**, **1 finger angle**, **Three-part swipes** and **2 finger angle** categories and the two-stroke swipe categories with back-and-forth and angle swipes. A swipe still waits for the lift when the swipe and hold of its direction has a command, or when an edge gesture is still possible. It's off by default.

With it off, a swipe also runs before the lift when no gesture that starts in its direction has a command: no back-and-forth or angle gesture, no swipe and hold of either, and no three-part swipe. With **SandFly gestures**, the back-and-forth gestures have commands; with **VoiceOver gestures**, none of these gestures do.

Each gesture profile can turn this setting on or off for itself; see [](#gestures-profiles). Not on watches.

### Change what a gesture does {#gestures-change}

1. Go to **Gestures**.
2. Activate the category of the gesture, such as **3 fingers**, to expand it.
3. Activate the gesture. A list of commands opens.
4. Choose a command. **Unassigned** makes the gesture do nothing.

The list starts with **Search** and **Unassigned**, followed by the categories of commands. Each category is collapsed, except the one with the command that the gesture runs now. To find a gesture by the command it runs, use **Search** at the top of the **Gestures** screen.

You can also give a command a gesture from the **Commands** screen; see [](#commands).

### Reset gestures {#gestures-reset}

**Gestures** > **Reset gesture settings** sets every gesture of the gesture layout in use back to its default command, after the **Reset gestures?** dialog. Choose **Reset** to confirm. Gestures in apps, gesture profiles and the other settings on the screen don't change.

### Gestures in apps {#gestures-in-apps}

A gesture or keyboard shortcut can run a different command in one app, while that app is in front. In the notification shade, quick settings and the lock screen, the gestures of the System UI app apply.

To add gestures for an app:

1. Go to **Gestures** > **Gestures in apps**.
2. Choose **Add app**, and choose the app.
3. Under **Gestures**, choose **Add gesture**. The list shows each gesture with its command.
4. Choose a gesture, then a category of commands, then a command. **Unassigned** makes the gesture do nothing in the app.

Each gesture on the screen of the app shows its command in the app. Activate it for **Choose command** and **Remove**. Under **Keyboard shortcuts**, **Add keyboard shortcut** records keys for the app in the same way; see [](#keyboards). **Remove app** removes all the gestures and keyboard shortcuts of the app.

The **Gestures for this app** command opens the screen of the app in front, so you can add a gesture while you use the app.

**Export to file** and **Import from file** save the gestures of apps to a file and read them back; see [](#api-item-files). A gesture profile comes before gestures in apps; see [](#gestures-profiles).

### Gesture profiles {#gestures-profiles}

A gesture profile is a set of gestures that run other commands while the profile is in use. **Default** is no profile.

To add a gesture profile:

1. Go to **Gestures** > **Gesture profiles**.
2. Choose **Add gesture profile**, and type a name, or leave the suggested name.
3. Open the profile, and change the gestures you want, as on the **Gestures** screen.

Each gesture in a profile is **Default** at first, and runs what it runs without the profile: its command on the **Gestures** screen, in **Gestures in apps** or in a script. A gesture set to a command runs that command, before gestures in apps and scripts. The list of commands has **Default** first.

The screen of a profile also has **Rename**, **Move up**, **Move down**, **Export to file** and **Delete gesture profile**, and its own **Recognize swipes before lifting** switch, which follows the **Gestures** screen until you change it.

To change the profile in use:

- **Gestures** > **Gesture profiles** > **Gesture profile in use**.
- The **Gesture profile** reading control and the **Gesture profile** item in the SandFly menu, shown once a profile exists.
- The **Choose gesture profile**, **Next gesture profile** and **Previous gesture profile** commands, and **Use gesture profile** for each profile and **Default**. You can assign them to gestures, including gestures in a profile, and to keyboard shortcuts.

SandFly speaks the name of the profile when it changes. Next and previous go through **Default** and then the profiles in the order of the **Gesture profiles** screen. Profiles don't change fingerprint gestures or keyboard shortcuts. Settings profiles, which change other settings, are described in [](#profiles).

### App shortcuts {#gestures-app-shortcuts}

The **App shortcuts** category of commands lists the shortcuts that touching and holding an app icon on the home screen shows, such as a new message, and the shortcuts you add on the **App shortcuts** screen. Shortcuts that an app adds while it runs, such as recent conversations, and shortcuts that only the home screen app may open are listed only through Shizuku, from Android 11. The list gets them each time it opens, so a new shortcut appears the next time. You can assign them to gestures, edge menus and keyboard shortcuts.

To add a shortcut that an app makes for the home screen, such as a contact or a macro of another app:

1. Go to **Gestures** > **App shortcuts**.
2. Choose **Add app shortcut**, and choose the kind of shortcut.
3. Complete the screen that the app opens.

The shortcut is added to the list, where you can rename or delete it. See [](#commands) for the commands that open apps and shortcuts.

### Practice gestures {#gestures-practice}

**Gestures** > **Practice gestures** opens a page where you can try any gesture. SandFly says what the gesture does instead of running it. The **Practice gestures** command opens the same page.
