## Braille {#braille}

SandFly has an on-screen braille keyboard for typing in six-dot or eight-dot braille, braille commands that run SandFly commands from braille cells typed on the screen, and support for refreshable braille displays connected through Bluetooth or USB, or provided by another app.

To open the braille settings, go to **Braille** in SandFly settings. The screen has:

- **Braille display**: see [](#braille-displays).
- **Braille keyboard**: see [](#braille-keyboard). If the device can't track five or more fingers at once, the item is unavailable, with the summary "Your device doesn’t support braille keyboard".
- **Braille commands**: see [](#braille-commands).
- **Braille dictionary** and **Symbol dictionary**: see [](#braille-dictionaries).

Keyboard echo for the braille keyboard and for braille displays is set in **Typing** > **Keyboard echo**. See [](#typing).

### Braille keyboard {#braille-keyboard}

The braille keyboard is an input method, **SandFly braille keyboard**, that covers the screen with six or eight dots. You type a braille cell by touching its dots at the same time, and you use swipes to add spaces, delete, move the cursor and more.

#### Set up the braille keyboard {#braille-keyboard-set-up}

1. Go to **Braille** > **Braille keyboard** > **Set up braille keyboard**.
2. On Android 13 and later, choose **Turn on**. On earlier versions, choose **Settings**, and turn on **SandFly braille keyboard** in the Android keyboard settings.
3. Open a text field. In the keyboard, choose the button that switches keyboards, usually in the bottom-right corner, and choose **SandFly braille keyboard**.

Once the keyboard is turned on, the first item is **How to use braille keyboard**, which shows step 3.

The **Switch to braille keyboard** command, in the **Braille** group of **Commands**, changes to the braille keyboard from any keyboard. By default, a two-finger double-tap and hold runs it. It requires Android 11 or later. See [](#commands).

The first time the keyboard opens, a tutorial starts. To open the keyboard without it, choose **Skip tutorial** on the first page.

#### Hold the device {#braille-keyboard-hold}

You can type in three ways, set in **Braille** > **Braille keyboard** > **Layout**:

- **Adapt to how you hold the device**: SandFly uses screen-away mode while you hold the device up with the screen facing away from you, and tabletop mode while it lies flat. This is the default on phones.
- **Screen-away mode**: hold the device with the screen away from you, and type with the fingers of both hands on the back edges.
- **Tabletop mode**: lay the device flat on a table in front of you. This is the default on tablets.

SandFly keeps the dots under your fingers whichever way you hold the device, with auto-rotate on or off. See [](#braille-keyboard-orientation).

#### Type braille {#braille-keyboard-type}

Touch the dots of a cell at the same time, then lift your fingers. SandFly types the character and reads it, as set in **Typing** > **Keyboard echo** > **Braille keyboard**.

With **Braille** > **Braille keyboard** > **Include all pressed dots** on, each dot that you touch is part of the cell, even if you lift it before you touch the other dots. It's on by default.

The braille keyboard has its own gestures. The most common, with the default gestures:

| Gesture | Action |
| --- | --- |
| Swipe right | Add a space |
| Swipe left | Delete |
| Swipe up | Move the cursor backward |
| Swipe down | Move the cursor forward |
| Two-finger swipe right | Add a new line |
| Two-finger swipe left | Delete a word |
| Two-finger swipe up | Submit text |
| Two-finger swipe down | Hide keyboard |
| Three-finger swipe up | Help and other options |
| Three-finger swipe down | Switch to next keyboard |
| Three-finger swipe right or left | Switch to next or previous granularity |

Gestures that start with held dots, such as holding dot 4, 5 or 6 and swiping up or down, move by line, word or character and select text. Holding dot 4 and swiping with three fingers cuts, copies, pastes or selects all. Holding dots 4, 5 and 6 and swiping runs these actions:

| Gesture | Action |
| --- | --- |
| Hold dots 4, 5 and 6, swipe up | Lock or unlock orientation |
| Hold dots 4, 5 and 6, swipe down | Switch to next braille table |
| Hold dots 4, 5 and 6, swipe right | Search emoji |
| Hold dots 4, 5 and 6, swipe left | Next braille mode |

Each gesture with held dots also works with the matching dots of the other hand, such as dots 1, 2 and 3 for dots 4, 5 and 6. A swipe counts when it goes up to about 35 degrees off the horizontal or vertical.

The three-finger swipe up opens **Braille keyboard options**: **Switch to next language**, **Switch to** contracted or uncontracted **braille**, **Update dot positions**, **Review gestures**, **Open tutorial** and **Settings**.

In fields with no action of their own or a send action, such as the message fields of Telegram, WhatsApp and Messages, Submit text presses the send button next to the field, and the braille keyboard stays open for the next message. In other fields, or when there is no send button, Submit text carries out the action of the field, such as search or next, or presses Enter, and hides the braille keyboard, so that focus can move on.

When you swipe to another control before the braille keyboard opens, SandFly moves focus back to the text field before each command, so cursor movement and editing apply to the text field.

#### Change the braille keyboard gestures {#braille-keyboard-gestures}

1. Go to **Braille** > **Braille keyboard** > **Gestures**.
2. Choose a group: **Basic controls**, **Cursor movement**, **Text selection and editing** or **Proofreading**.
3. Choose an action, then choose **Change gesture**, **Reset to default** or **Remove gesture**.
4. For **Change gesture**, the braille keyboard opens. Perform the new gesture, then perform it again to confirm. To cancel, swipe down with two fingers.
5. A dialog offers the mirrored gesture for the action too. Choose **Use mirrored gesture** or **Skip**.

If the gesture runs another action, SandFly announces the action; perform the gesture again to replace it. **Reset all gestures** gives every action its default gesture back.

#### Update dot positions {#braille-keyboard-dot-positions}

SandFly places the dots where your fingers land when you update the dot positions. The positions are saved for each layout.

- Hold six fingers on the screen (eight with eight-dot braille) for 2 seconds.
- Or choose **Update dot positions** in **Braille keyboard options**, and follow the steps for each hand.
- With **Braille** > **Braille keyboard** > **Update dot positions with one hand, then the other** on, hold three fingers of one hand on the screen for at least 0.4 seconds, then put down the three fingers of the other hand and keep all six still for 0.65 seconds. Either hand can go first. With eight-dot braille, use four fingers of each hand. Six fingers that land together, or that lift sooner, type a cell. This works only while **Include all pressed dots** is off, which isn't the default. The setting is on by default.

With **Braille** > **Braille keyboard** > **Update dot positions while typing** on, each typed cell moves the dots a little toward where your fingers land, so the dots follow hands that shift while you type. The positions are saved and used the next time. The setting is on by default.

- A touch moves the dots only when it is clearly closer to its own dot than to any other. When a finger has shifted far enough to type the wrong dot, update the dot positions instead.
- The dots don't move off the screen, closer together than one dot width, or into an order that changes their dot numbers.
- Only typed cells move the dots. Gestures, holds and the tutorial don't.

#### Dots that follow how you hold the device {#braille-keyboard-orientation}

SandFly records how you last held the device up, the whole time the screen is on, as auto-rotate does.

- In screen-away mode, the dots follow how you hold the device.
- In tabletop mode, laying the device flat keeps the side that you held it up with, even from before the keyboard opened, and turning it on the table turns the dots with it.
- On a phone, the dots turn when the charging port moves to your other side. After a quarter turn in between, SandFly announces "Charging port toward you" or "Charging port away from you".
- On a tablet or an unfolded foldable, the dots turn by quarter turns, including upside down.

SandFly announces where the charging port is when the mode changes and after each turn, such as "Tabletop mode, charging port on the left".

To keep the dots facing the way they are, hold dots 4, 5 and 6 and swipe up. SandFly announces "Orientation locked". Do the same again to unlock. There is a lock for each screen size and mode, so you can lock a foldable one way when it's folded and another way when it's unfolded.

When you lay a phone flat after holding it up, SandFly assumes that it faced you, unless you typed on it in screen-away mode. A tablet or an unfolded foldable held in portrait is assumed to face you, and one held sideways is assumed to face away, as when typing screen away. If you hold or stand a tablet facing you, turn off **Braille** > **Braille keyboard** > **Tablet held up faces away**. A tablet held up any way round then uses the tabletop layout facing you, with your thumbs on the bottom edge. The setting is on by default, and is also a reading control, in the **Display** group of **Reading controls**.

Limitations:

- A phone opened flat, which hasn't been held up since the screen came on, follows the screen rotation, and with auto-rotate off expects the charging port on the left. Turning it on the table, or locking the orientation, corrects this.
- A phone held up screen away and laid flat without typing is assumed to face you. Locking the orientation corrects this.
- Turns on the table require a game rotation vector sensor. Devices without one still follow how they are held up.
- Recording how the device is held keeps the accelerometer running while the screen is on.

#### Braille languages {#braille-keyboard-languages}

- **Languages**: the braille tables that the keyboard and braille displays can use. Choose **Add a language** to add one. Japanese braille needs a dictionary of about 15 MB, which SandFly downloads when you add the language. **Romanian** is 6-dot uncontracted braille: dots 46 capital letter, dots 56 capital word, dots 3456 number sign, dots 2356 parentheses, and UEB signs for operators; **Romanian 8-dot Computer Braille** uses one 8-dot cell per character. **Add a language** > **Unicode Braille**, at the end of the list, has **Unicode Braille** and **Unicode 8-dot Braille**, which type each cell as its Unicode braille character, such as ⠞ for dots 2, 3, 4 and 5; a space is the blank braille cell U+2800, which SandFly speaks as "blank braille cell". They are for typing only and aren't choices for **Preferred reading language** on braille displays.
- **Preferred typing language**: the table that the keyboard starts with.
- **Preferred braille grade**: **Contracted braille** or **Uncontracted braille**. The default is uncontracted.

To change to the next language while you type, hold dots 4, 5 and 6 and swipe down. SandFly turns contracted braille on or off as it was when you last used that language, and announces the language and the grade. Speech changes to your first voice profile in that language, if you have one, and back to **SandFly default** when the new language has none. Unicode braille keeps the voice in use. See [](#speech).

#### Custom braille characters {#braille-keyboard-custom-characters}

**Braille** > **Braille keyboard** > **Custom braille characters** lists characters or texts typed with braille cells that you choose, each for one braille table. An entry takes precedence over the rules of the table, on the braille keyboard and on a braille display, for typing and for the braille shown.

1. Choose **Add character**.
2. Enter the **Character or text**.
3. Enter the **Dots** of each cell, with cells separated by hyphens, such as `2356` or `456-2356`.
4. Choose the **Braille table**.

Entering a character again for the same table replaces the earlier entry. Rules of the table that cover more cells, such as contractions, and rules for characters within numbers, can still take precedence. **Export to file** and **Import from file** save and read the list as an item file; see [](#api-item-files).

#### Search emoji {#braille-keyboard-emoji}

1. Hold dots 4, 5 and 6 and swipe right.
2. Type words of the emoji name in uncontracted braille of the keyboard language. SandFly reads each character and the first emoji whose name contains every word, with the number of matches. A character that leaves no emoji is dropped.
3. Swipe down or up for the next or previous emoji. Swipe right for a space between words, and left to delete.
4. Swipe right with two fingers to insert the emoji. The search stays open for another.

Any other gesture closes the search. The names are the Unicode CLDR names in the language of the braille table, or in English where CLDR has none. Skin tone variants aren't offered, and a search finds at most 100 emoji.

#### Other braille keyboard settings {#braille-keyboard-settings}

In **Braille** > **Braille keyboard**:

- **Swap left and right dots**: dots 1, 2 and 3 trade places with dots 4, 5 and 6. Off by default.
- **Swap top and bottom dots**: dot 1 trades places with dot 3, and dot 4 with dot 6. Off by default. You can turn on both swaps.
- **Typing sounds**: plays a sound for each character, and a sound each for space, delete and new line, at the SandFly sound volume. A sound theme can replace them. Off by default. See [](#sound).
- **Say "Submitting text"**: announces "Submitting text" when you submit text with a two-finger swipe up. On by default.
- **Return to the previous keyboard**: when you hide the braille keyboard with **Hide keyboard**, or leave its text field, SandFly changes back to the keyboard you used before it. Open the braille keyboard again with **Switch to braille keyboard**. It doesn't apply while a braille display is connected. Off by default.
- **Capitalize sentences**: types the first letter of a sentence as a capital, at the start of a field, after a new line, and after a full stop, question mark or exclamation mark followed by a space. It doesn't change password, email address, web address or number fields. Off by default.

The braille keyboard uses the same vibration effects as the rest of SandFly. Submitting text vibrates as closing or switching the keyboard does. Deleting in an empty field plays a soft vibration that fades out.

While the braille keyboard is shown, gestures run only commands that open no window, such as navigation and reading controls, so that no window opens over the keyboard. Braille commands still open over it. See [](#braille-commands).

### Braille commands {#braille-commands}

Braille commands let you run SandFly commands by typing braille cells on the screen, as on the braille keyboard. From braille commands you can also search for an app by name, find text on the screen, and change to the braille keyboard. Braille commands require Android 11 or later, and aren't available on watches.

#### Use braille commands {#braille-commands-use}

To show braille commands, do one of the following:

- Run the **Braille commands** command, in the **Braille** group of **Commands**. It has no gesture by default. See [](#commands).
- On the braille keyboard, hold dots 4, 5 and 6 and swipe left.

A chime plays, and SandFly announces "Braille commands", with the name of the braille command profile in use. The dots cover the screen, laid out as on the braille keyboard. Type a cell to run its command. For a cell without a command, SandFly announces "No command for" and the dots.

Braille commands stay open after each command, until you swipe down with two fingers or the screen turns off. A command that opens a menu or dialog of SandFly over them closes them. They don't show over the lock screen.

Hold dots 4, 5 and 6 and swipe left to change to the next mode: from braille commands to app search, then to the braille keyboard when a text field is being edited, otherwise back to braille commands. When you open a text field from braille commands, the braille keyboard opens, and braille commands show again when it closes.

The default cells are:

| Cell | Command |
| --- | --- |
| Dot 4 | **Next item** |
| Dot 1 | **Previous item** |
| Dots 5 6 | **Next reading control** |
| Dots 2 3 | **Previous reading control** |
| Dot 5 | **Move reading control down or forward** |
| Dot 2 | **Move reading control up or backward** |
| Dots 3 6 | **Activate** |
| Dots 1 2 | **Back** |
| Dots 1 2 5 | **Home** |
| Dots 1 2 4 | **Find text** |
| Dots 1 2 4 6 | **Find next text** |
| Dots 1 3 4 5 | **Find previous text** |
| Dots 1 2 3 5 | **Overview** |
| Dots 1 2 3 4 5 | **Quick Settings** |
| Dots 2 3 4 5 | **Focus on first item on screen** |
| Dots 1 5 | **Focus on last item on screen** |
| Dots 1 3 4 | **Open SandFly menu** |

#### Search for an app {#braille-commands-apps}

In app search, type the name of an app in uncontracted braille of the braille keyboard language. SandFly announces the first app whose name, or a word of it, starts with the letters, such as "Calendar, 1 of 3". A cell that matches no app is dropped, and SandFly announces "No apps".

- Swipe down or up for the next or previous app.
- Swipe left to delete the last cell, and swipe left with two fingers to clear.
- Swipe up with two fingers to open the app, or swipe up with three fingers to open it in split screen. When you entered app search from braille commands, braille commands show again over the app.
- Swipe down with two fingers to close.

App search closes after 15 seconds without a touch. To open app search directly, run **Type an app's name in braille to open it**, in the **System actions** group of **Commands**.

#### Find text on the screen {#braille-commands-find}

**Find text**, **Find next text** and **Find previous text** are in the **Braille** group of **Commands**, and have braille cells by default.

1. Run **Find text**. SandFly announces "Type text to find".
2. Type the text in uncontracted braille. Swipe right for a space, swipe left to delete, and swipe left with two fingers to clear.
3. Swipe up or right with two fingers. Focus moves to the next item that contains the text.

**Find text** goes on from the top of the screen when it reaches the bottom, and SandFly announces "Wrapped". **Find next text** and **Find previous text** look for the same text again, after or before the focused item, without wrapping. When no text has been found yet, they start as **Find text** does. Swipe down with two fingers to leave without finding. SandFly keeps the text until braille commands close.

#### Change the cells {#braille-commands-cells}

Go to **Braille** > **Braille commands**. The screen lists each cell with its command, such as "Dots 1 2 5: Home".

- To add a cell, choose **Add braille command**, choose a group and a command, then type the cell. SandFly shows the dots to type it on.
- To change a cell, choose it, then **Choose command**, **Change braille cell** or **Remove**.
- If the cell already runs another command, a dialog offers to use it for the new command instead. Choose **Replace**.
- **Reset to default**, shown after you change the cells, gives the cells their defaults back.

You can also give a command a cell from its own screen in **Commands**, in its **Braille cells** section. See [](#commands).

#### Braille commands in apps and profiles {#braille-commands-profiles}

A cell runs the command of the braille command profile in use, otherwise the command that the app in front gives it, otherwise the default command.

- **Braille commands in apps**: choose **Add app**, choose an app, then add cells as for the defaults. In an app, **Unassigned** stops a cell running its default command. **Remove app** removes the app and its cells.
- **Braille command profiles**: choose **Add braille command profile**, enter a name, then add cells. In a profile, choose **Rename** or **Delete braille command profile**.
- **Braille command profile in use**: shown when there is a profile. Choose **Default** or a profile.

The commands **Choose braille command profile**, **Next braille command profile** and **Previous braille command profile** are in the **Braille** group of **Commands**, and each profile has a **Use braille command profile** command in the **Braille command profiles** group. You can give them gestures, keys or braille cells. See [](#commands).

### Braille displays {#braille-displays}

SandFly shows text on refreshable braille displays and takes commands and typing from their keys. It supports displays connected through Bluetooth or USB, and displays provided by other apps.

#### Connect a braille display {#braille-displays-connect}

1. Go to **Braille** > **Braille display**.
2. Turn on **Use braille display**. If SandFly needs a permission, such as Nearby devices, or Bluetooth is off, a banner at the top shows what to do.
3. Turn on Bluetooth on the display, or connect it with a USB cable.
4. Under **Devices**, choose the display, then choose **Connect**. If the display isn't listed, choose **Look for new device**.

A display in the list is **Active** when connected, **Saved, available** or **Available** when it can connect, or **Saved** when it is out of range. Choose a saved display to **Disconnect** or **Forget** it. With **Auto-connect** on, saved displays connect when nearby. It's on by default.

On Android 15 and later, SandFly also connects to displays that use the HID braille standard over Bluetooth. The list shows every Bluetooth device that offers HID, so other HID devices, such as keyboards, can appear in it; connecting checks that the device is a braille display.

EuroBraille displays are supported, including the b.note, b.note 2, b.book and b.book 2, with their keypads and navigation keys.

#### Braille display settings {#braille-displays-settings}

In **Braille** > **Braille display**, the **Languages and commands** group has:

- **Languages**: the braille tables in use, shared with the braille keyboard. See [](#braille-keyboard-languages).
- **Preferred reading language** and **Preferred typing language**: the tables for text shown on the display and for typing on it. While you type in Unicode braille, the display shows the text of the field in the reading language.
- **Preferred braille grade**: **Contracted braille** or **Uncontracted braille**.
- **Braille display commands**: the keys of each command. See [](#braille-displays-keys).
- **Braille elements**: the abbreviations that the display shows for elements on the screen, such as "btn" for a button or "lnk" for a link.
- **Practice braille display commands**: with a display connected, press a key to hear its command.

The **General** group has:

- **Auto-connect**: see [](#braille-displays-connect).
- **Hide the on-screen keyboard**: keeps other on-screen keyboards hidden while a braille display is connected. The braille keyboard still shows its strip, since typing on the display goes into text fields through it. Off by default.
- **Auto-scroll**: advances the display at a set speed. Press Space with dots 1, 2, 4, 5 and 6 to turn it on or off, and dot 4 or dot 1 to make the duration longer or shorter. **Automatically adjust display duration** changes the duration with the length of the text, and **Default display duration** sets it.
- **Show captions on braille display**: shows captions of videos and other media.
- **Displays from other apps**: see [](#braille-displays-other-apps).
- **Advanced settings**: **Reverse pan up and pan down buttons**, **Show braille output on screen**, **Message duration** (**Short**, **Medium** or **Long**), **Cursor blinks every** (from **Never** to 2 seconds; the default is 0.75 seconds), **Show cursor on non-editable items** (on by default), **Show device messages**, which shows messages such as notifications and incoming calls on the display, and **Save device messages**, which keeps a list of them to open with Space and dots 1, 3, 4, 5, 7 and 8.

The **Navigate to braille display settings** command, in the **Braille** group of **Commands**, opens these settings. The keyboard shortcuts **Switch between contracted (grade 2) and uncontracted (grade 1)** and **Show or hide on-screen braille for connected braille displays** change the braille grade and **Show braille output on screen** from a physical keyboard. See [](#appendix-keyboard-shortcuts).

#### Choose the keys of a command {#braille-displays-keys}

1. Connect the display.
2. Go to **Braille** > **Braille display** > **Braille display commands**.
3. Choose a group, then a command.
4. Choose **Change keys**, and press the new keys on the display: one of its keys, Space with dots, or on EuroBraille displays, Backspace with dots.

If the keys already run another command, a dialog offers to move them to the new command; the other command then has no keys. **Reset** gives a command its default keys back, and **Reset all keys** does so for every command.

**SandFly commands** lists the commands that gestures run, such as **Open SandFly menu**, **Notifications** or **Read from top**, by group, so that you can give each one display keys. Commands without keys on the connected display are listed with "No keys".

Panning and routing keys can't be changed, except right joystick left and right on EuroBraille displays. Keys without Space still type their dots in text fields.

On EuroBraille displays, Space with dot 2, 3, 5 or 6, and Backspace with dots, run nothing until you assign them a command. The cursor routing keys move the cursor and activate items. Pressing the right joystick activates, and left and right on it pan. On the left joystick, left and right move by character, and up and down by word.

#### Displays from other apps {#braille-displays-other-apps}

An app can provide SandFly with a braille display that SandFly can't connect to on its own, such as the built-in keys and cells of a braille notetaker. SandFly uses it as a connected display. A display with keys and no cells works too.

SandFly uses a display from an app only after you allow it. When SandFly finds such an app, it shows the dialog "Allow" (app) "to be a braille display for SandFly?", with a warning that the app will be able to read everything shown in braille and control the device. Choose **Allow** or **Don't allow**. **Braille** > **Braille display** > **Displays from other apps** lists these apps, to allow or stop allowing each one. While SandFly uses one, a notification shows which app it is.

A display connected through Bluetooth or USB comes first. SandFly uses a display from an app only while no other display is connected.

For developers, the API is described in [](#api-braille-display).

### Braille dictionary and symbol dictionary {#braille-dictionaries}

**Braille** > **Braille dictionary** changes text on a braille display, such as a symbol into a word, or text left off the display. Its entries are separate from the speech dictionary, which changes how text is spoken.

1. Choose **Add a replacement**.
2. Enter the **Text** and what to **Replace with**. An empty replacement leaves the text off the display.
3. Choose **Whole words only** or **Regular expression** if needed.
4. Choose **All apps** or **Choose an app**.

Entries apply in order. Text being typed and the screens of SandFly are shown as they are. **Export to file** and **Import from file** save and read the list.

**Braille** > **Symbol dictionary** opens the same list as **Speech** > **Symbol dictionary**: symbols or sequences of symbols to replace in speech and on a braille display, such as a period replaced by a space. An entry matches its exact text anywhere, including directly after a word, and applies after the braille dictionary. See [](#speech).
