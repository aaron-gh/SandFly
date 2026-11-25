## Physical keyboards {#keyboards}

You can use SandFly with a Bluetooth or USB keyboard, or with the built-in keyboard of a keyboard phone. SandFly runs commands from keyboard shortcuts, moves with the arrow keys and single letters where your keymap uses them, and can leave keys to the app you use. This chapter describes **Keyboard shortcuts** in SandFly settings and the physical keyboard settings in **Typing**. For the default shortcut of each command, see [](#appendix-keyboard-shortcuts).

To learn the keyboard shortcuts step by step, use the keyboard tutorial. See [](#getting-started).

### The SandFly key {#keyboards-sandfly-key}

Most keyboard shortcuts use the SandFly key with other keys, such as SandFly key + Right Arrow to move to the next item. The SandFly key is a key, or two keys held together, that you choose for each keymap. It can be one of these keys, depending on the keymap:

- **Meta**: the Windows key, next to the left Alt key. On Apple keyboards, the Command key.
- **Alt**
- **Ctrl + Alt**: both keys held together.
- **Ctrl + Meta**: both keys held together.
- **Ctrl**
- **Insert**: on most keyboards, in the group of six keys above the arrow keys.
- **Numpad Insert**: the 0 key of the number pad, with Num Lock off.
- **Sym**, **Right Alt**, **Right Shift** and **Fn**: keys of keyboard phones. See [](#keyboards-phones).

Insert, Numpad Insert, Sym, Right Alt, Right Shift and Fn are keys that Android doesn't treat as modifier keys. SandFly takes them while they are held, and they don't reach apps.

With Meta as the SandFly key, pressing Meta alone once opens the list of all apps, as Android does. Pressing the SandFly key twice quickly turns browse mode on or off in keymaps where that is the shortcut for it. See [](#keyboards-browse-mode).

To choose the SandFly key, see [](#keyboards-choose-sandfly-key).

### Keymaps {#keyboards-keymaps}

A keymap is a set of keyboard shortcuts. SandFly has these keymaps:

- **SandFly keymap**: the default keymap. Its SandFly key is Meta, Insert or Numpad Insert, Meta by default.
- **VoiceOver keymap**: the keyboard commands of VoiceOver on iPhone, with the SandFly key as the VO key. Its SandFly key is Ctrl + Alt, Insert or Numpad Insert, Ctrl + Alt by default. Meta takes the place of Command. In browse mode, single keys move as single-key Quick Nav does in VoiceOver.
- **NVDA keymap**: the laptop layout of NVDA, which needs no number pad, with the SandFly key as the NVDA key. Its SandFly key is Insert, Numpad Insert or Meta, Insert by default.
- **JAWS keymap**: the desktop keystrokes of JAWS, with its laptop keys for clicks and cursors, with the SandFly key as the JAWS key. Its SandFly key is Insert, Numpad Insert or Meta, Insert by default.
- **Orca keymap**: the laptop layout of Orca, with the SandFly key as the Orca modifier. Its SandFly key is Insert, Numpad Insert, Meta or Alt, Insert by default.
- **Keyboard phone keymap** and **Keyboard phone single-key keymap**: for phones with a built-in keyboard that has no Ctrl, Meta or arrow keys. See [](#keyboards-phones).
- **BlackBerry keymap**: keys similar to those of the BlackBerry 10 Screen Reader, for keyboard phones. See [](#keyboards-phones).
- Keymaps that you make. See [](#keyboards-custom-keymaps).

In the NVDA, JAWS and Orca keymaps, the arrow keys and Tab move without the SandFly key, as on a computer. See [](#keyboards-desktop).

On a keyboard phone, the keyboard phone keymap is the default keymap. SandFly switches a keyboard phone from the SandFly keymap to the keyboard phone keymap once, the first time it starts on it.

#### Choose a keymap {#keyboards-choose-keymap}

1. In SandFly settings, open **Keyboard shortcuts**.
2. Select **Choose a keymap**.
3. Choose a keymap.

SandFly announces the keymap that is now active. Each keymap keeps its own shortcuts, its own SandFly key and its own arrow key navigation settings, so changing keymaps doesn't change another keymap.

#### Choose the SandFly key {#keyboards-choose-sandfly-key}

1. In SandFly settings, open **Keyboard shortcuts**.
2. Select **Choose SandFly key**.
3. Choose a key.

**Choose SandFly key** sets the SandFly key of the keymap in use. It lists only the keys that keep every default shortcut of the keymap; a keymap that you make offers every SandFly key. Each default shortcut keeps its keys, with the new SandFly key in place of the old one. Shortcuts that you changed are kept separately for each SandFly key of a keymap.

#### Custom keymaps {#keyboards-custom-keymaps}

You can make keymaps of your own. A new keymap starts as a copy of the keymap in use, with its shortcuts, its SandFly key and the way its keys work, such as arrow keys that move as in the NVDA keymap.

To make a keymap:

1. Choose the keymap to copy. See [](#keyboards-choose-keymap).
2. In **Keyboard shortcuts**, select **New keymap**.
3. Enter a name, or leave the field empty to use the name shown, such as "Copy of NVDA keymap".
4. Select **OK**.

The new keymap is then in use. In a keymap that you make, shortcuts can use any keys, with or without the SandFly key. Shortcuts without the SandFly key work anywhere except in a text field being edited, where the keys go to the text field. In keymaps copied from the SandFly or VoiceOver keymap, such shortcuts with Ctrl or Alt work in text fields too. Modifier keys pressed on their own always go to the app.

While a keymap that you made is in use, **Keyboard shortcuts** also has:

- **Rename keymap**: changes its name.
- **Delete keymap**: deletes it, after you confirm. The default keymap is then in use.

#### Export and import keymaps {#keyboards-keymap-files}

- **Export to file**: saves the keymap in use, with its shortcuts and SandFly key, to a file that you choose.
- **Import from file**: adds a keymap from a file.

These aren't available on watches. For the file format, see [](#api-item-files).

#### Reset a keymap {#keyboards-reset-keymap}

**Reset the keymap** gives every shortcut of the keymap in use its default keys again, and every arrow key its default command. SandFly asks you to confirm first. You can't undo a reset.

### The Keyboard shortcuts screen {#keyboards-settings}

**Keyboard shortcuts** in SandFly settings has:

- **Search**: finds a command or an arrow key by its name or its keys, and opens its shortcut.
- **Keymap**: **Choose a keymap**, **Choose SandFly key**, **Reset the keymap**, **View system keyboard shortcuts**, **New keymap**, **Export to file**, **Import from file**, and for keymaps that you made, **Rename keymap** and **Delete keymap**. **View system keyboard shortcuts** opens the list of the keyboard shortcuts of Android and the app in use.
- **Keyboard shortcuts in apps**: shortcuts that run other commands in one app. See [](#keyboards-app-shortcuts).
- **Keyboard practice**: turns keyboard practice on. See [](#keyboards-practice).
- **Arrow key navigation**: in keymaps that have it. See [](#keyboards-arrow-keys).
- **Fast search**: **Fast search key** and **Wrap around**. See [](#keyboards-fast-search).
- The commands, by category, each with its shortcut. Each category opens and closes. A command whose shortcut works only in browse mode has "in browse mode" after its name, such as "Navigate to next heading in browse mode".

You can change shortcuts and reset the keymap only while SandFly is on.

You can also see and change the keyboard shortcut of a command on its screen in **Commands**. See [](#commands).

### Change a keyboard shortcut {#keyboards-change-shortcut}

1. In SandFly settings, open **Keyboard shortcuts**.
2. Open the category of the command, or select **Search** and type its name.
3. Select the command.
4. Press the new keys.
5. Press Enter, or select **Save**.

In the dialog:

- Backspace clears the shortcut, so the command has none. **Clear** does the same.
- Ctrl + Backspace sets the default shortcut again, in the SandFly and VoiceOver keymaps and in keymaps copied from them.
- Escape, or **Cancel**, closes the dialog without a change.

If another command has the keys, SandFly shows "Currently assigned to" and the name of that command. When you save, the **Key already in use** dialog asks whether to take the keys from that command; select **OK** to assign them to the command you are changing.

The shortcut for **Turn browse mode on or off** can also be the SandFly key pressed twice: press it twice within half a second. Ctrl + Alt and Ctrl + Meta can't be pressed twice.

#### Shortcuts of two keys in a row {#keyboards-sequences}

In the SandFly keymap and the VoiceOver keymap, and in keymaps copied from them, a shortcut can be the SandFly key + R or the SandFly key + O followed by a third key, such as SandFly key + O, then H. Press the third key within 3 seconds, with or without the SandFly key. The third key can't be Escape, Backspace, Enter or a modifier key.

To record such a shortcut, press SandFly key + R or SandFly key + O, then the third key.

#### Shortcuts that work only while editing {#keyboards-while-editing}

Select **Only while editing** in the shortcut dialog for a shortcut that works only while a text field is being edited. Such a shortcut is pressed without the SandFly key, and SandFly takes its keys from the text field. Outside text fields, the keys work as they would without the shortcut. The shortcut is shown with "while editing" after its keys, such as "Alt + Space while editing".

#### Shortcuts that work only in browse mode {#keyboards-in-browse-mode}

In the keyboard phone keymaps, select **Only in browse mode** in the shortcut dialog for a shortcut that works only in browse mode. Such a shortcut is pressed without the SandFly key. In browse mode, it comes before a shortcut of the same keys without **Only in browse mode**, which the SandFly key with the keys then runs. Outside browse mode, the keys work as they would without the shortcut. The shortcut is shown with "in browse mode" after its keys, such as "B in browse mode".

#### Presses in keyboard phone keymaps {#keyboards-presses}

In the keyboard phone keymaps, a shortcut can also be:

- A modifier key pressed on its own, such as Sym alone or Shift + Sym. Such a press reaches the app unless it has a shortcut.
- A key, or modifier keys, pressed twice quickly, such as Alt twice. The first press goes to the app as usual; the second runs the shortcut.
- Modifier keys held down on their own for half a second, such as Alt + Shift held.
- Another key held down, such as K held. While you type, held keys type as usual.

To record these, press the keys the same way in the shortcut dialog. In these keymaps, Enter pressed once saves the shortcut a moment later; Enter pressed twice records Enter twice. Backspace clears the shortcut.

### Keyboard shortcuts in apps {#keyboards-app-shortcuts}

**Keyboard shortcuts** > **Keyboard shortcuts in apps** sets keyboard shortcuts for one app. They come before the shortcuts of the keymap while the app is shown. A shortcut set to **Unassigned** in an app leaves its keys to the app.

1. In **Keyboard shortcuts**, select **Keyboard shortcuts in apps**.
2. Select **Add app**, and choose the app.
3. Under **Keyboard shortcuts**, select **Add keyboard shortcut**, press the keys, and select **Save**.
4. Choose a category, then a command, or choose **Unassigned**.

**Add keyboard shortcut** is available only while SandFly is on. Select a shortcut for **Choose command** or **Remove**. **Remove app** removes the gestures and shortcuts of the app.

The same screen has the gestures of each app. For gestures in apps, see [](#gestures).

### Find a keyboard shortcut {#keyboards-shortcut-list}

**Show keyboard shortcuts list** opens a list of every command and its shortcut, which you can search. Its default shortcut is SandFly key + Shift + / in the SandFly, VoiceOver and Orca keymaps, SandFly key + Q in the keyboard phone keymap, and Q in the keyboard phone single-key keymap.

**Keyboard shortcuts** > **Search** finds a command by its name or keys in settings. For the default shortcuts of every keymap, see [](#appendix-keyboard-shortcuts).

### Keyboard practice {#keyboards-practice}

While keyboard practice is on, SandFly says each key that you press and the command that it runs, and runs nothing. Keys don't reach apps. Use it to learn the keys of a keyboard and the shortcuts of your keymap.

- Keys are said as the keyboard shortcuts list names them, followed by their command, such as "Right Shift + W, Speak window title". Keys without a command are said alone.
- A modifier key is said when you press it, with the command of the modifier keys pressed alone. The SandFly key pressed alone is also said as the SandFly key, such as "Right Shift, SandFly key, Next item".
- Keys pressed twice, keys held down and the second key of a shortcut of two keys in a row are said with their command, such as "Alt twice" or "Alt + Shift held".
- While arrow key navigation is on, the arrow keys are said with their arrow key navigation commands.
- A key held down is said once.

To turn keyboard practice on, select **Keyboard shortcuts** > **Keyboard practice**, or press the shortcut of **Turn keyboard practice on or off**: SandFly key + Shift + K in the keyboard phone keymaps. Other keymaps have no default shortcut. SandFly says "Keyboard practice on".

To turn it off, press the shortcut again, Back or Escape. SandFly says "Keyboard practice off". Keyboard practice is also off when SandFly restarts.

**Turn keyboard practice on or off** is in the **SandFly** category, so you can also assign it to a gesture or add it to the SandFly menu. Volume keys pressed without the SandFly key change the volume as usual. The **Keyboard practice** lesson of the keyboard tutorial is different: it says only keyboard shortcuts, on the tutorial screen.

### Browse mode {#keyboards-browse-mode}

In browse mode, single keys without the SandFly key move SandFly focus, as on a computer screen reader: in the SandFly keymap, H moves to the next heading, K to the next link, B to the next button, D to the next landmark and F to the next control, and Shift with the key moves back. The arrow keys move by line and character, and Ctrl with the Left or Right Arrow by word. Browse mode works in web content and in apps.

The default shortcut of **Turn browse mode on or off** is:

| Keymap | Shortcut |
| --- | --- |
| SandFly keymap | SandFly key pressed twice |
| VoiceOver keymap | SandFly key + Q |
| NVDA keymap | SandFly key + Space |
| JAWS keymap | SandFly key + Z |
| Orca keymap | SandFly key + A |
| Keyboard phone single-key keymap | SandFly key pressed twice; SandFly key + Q with Alt as the SandFly key |

SandFly plays a sound or says "Browse mode on" or "Browse mode off".

In the keyboard phone keymaps, the commands that need browse mode in other keymaps work without it. In the keyboard phone single-key keymap, browse mode changes what letters do: B, C, D, I, K, L, R, T, U, V, X and the number keys 1 to 6 move as in the SandFly keymap's browse mode, and H, F, E and G move by heading, control, edit field and graphic in and outside browse mode. The SandFly key with one of these letters runs its shortcut outside browse mode, such as SandFly key + B for the bottom of the screen and SandFly key + K to move by the reading control. See [](#keyboards-phones).

Two settings in **Navigation** change when browse mode is on:

- **Navigation** > **Smart browse mode**: turns browse mode off when focus moves to a text field in web content, so that you can type, and on again when focus moves to an item that isn't a text field. It's on by default. While arrow key navigation is on, and in the keyboard phone keymaps, moving to a text field doesn't give it input focus, so browse mode stays on until you activate the field.
- **Navigation** > **Remember browse mode in each app**: keeps browse mode as you last turned it on or off in each app. In an app where you haven't turned it on or off, browse mode is off, unless **Smart browse mode** turns it on in web content. It's off by default.

For navigation by web elements, see [](#web).

#### Fast search {#keyboards-fast-search}

In browse mode, the fast search key with a letter or digit moves SandFly focus to the next item that starts with that letter or digit. Shift with both keys moves to the previous one. Case and accents don't matter: E finds "Email" and "Écran".

To choose the key, go to **Keyboard shortcuts** > **Fast search** > **Fast search key**. It's Off by default. The list has Alt, Ctrl, Meta, each of them left or right, Sym and Fn, except keys that are part of the keymap's SandFly key. If you change the SandFly key to one that includes the fast search key, fast search is off for that keymap.

Fast search doesn't work in a text field you are typing in. While the fast search key is held, its letters and digits don't run other shortcuts.

**Keyboard shortcuts** > **Fast search** > **Wrap around**: when no item after SandFly focus starts with the letter, the search continues from the top of the screen, or from the bottom when moving back. It's on by default. When no item is found, SandFly says "Not found".

### Arrow key navigation {#keyboards-arrow-keys}

While arrow key navigation is on, the arrow keys run commands without the SandFly key: alone, two pressed together, or with Ctrl or Alt. The default commands are those of the arrow keys in VoiceOver on iPhone:

| Keys | Command |
| --- | --- |
| Right Arrow, Left Arrow | Next item, previous item |
| Down Arrow, Up Arrow | Adjust reading control down, up |
| Left Arrow + Right Arrow together | **Turn arrow key navigation on or off** |
| Up Arrow + Down Arrow together | Activate the item |
| Up Arrow + Right Arrow, Up Arrow + Left Arrow together | Select next reading control, previous reading control |
| Ctrl + Up Arrow, Ctrl + Down Arrow | First item, last item on screen |
| Alt + Up Arrow, Alt + Down Arrow | Scroll back, scroll forward |
| Alt + Left Arrow, Alt + Right Arrow | Scroll right, scroll left |

Down Arrow + Right Arrow, Down Arrow + Left Arrow, Ctrl + Left Arrow and Ctrl + Right Arrow have no command by default.

To turn arrow key navigation on or off, press Left Arrow and Right Arrow together, or turn on or off **Keyboard shortcuts** > **Arrow key navigation** > **Arrow key navigation**. **Turn arrow key navigation on or off** is also a command that you can assign to a gesture. Arrow key navigation is off by default, except in the keyboard phone keymaps, where it is on for the arrow keys that a trackpad sends, such as the trackpad of the Zinwa Q25.

To change the command of an arrow key:

1. In **Keyboard shortcuts**, open **Arrow key navigation**.
2. Select the arrow keys, such as "Arrow Up + Arrow Down".
3. Choose a command from the list, the same list as for a gesture.

A key that is part of two keys pressed together runs its command when you release it, unless you press the second key first. While a text field is being edited, single arrow keys move the cursor; keys pressed together still run their commands, and Left Arrow + Right Arrow turns arrow key navigation off once both are held for 0.4 seconds.

While arrow key navigation is on, moving to a text field reads it without giving it input focus, and the arrow keys move on past it, also where the app already gave the field input focus. The field is being edited once you activate it, such as with Up Arrow + Down Arrow, or type into it. In the keyboard phone keymaps, Enter activates it too. A new screen that opens with SandFly focus on a text field with input focus starts with the field being edited. In the keyboard phone keymaps, on a new screen where another item has SandFly focus, letters type into the text field with input focus, and the arrow keys go to it once you have typed.

Each keymap has its own arrow key commands and its own switch. The NVDA, JAWS and Orca keymaps don't have arrow key navigation, as their arrow keys move SandFly focus; there, the command says "No arrow key navigation in this keymap".

### Desktop navigation {#keyboards-desktop}

In the NVDA, JAWS and Orca keymaps, and in keymaps copied from them:

- Down Arrow and Up Arrow move to the next and previous item. Left Arrow and Right Arrow move by character, and Ctrl with them by word. Ctrl + Home and Ctrl + End move to the first and last item.
- Tab and Shift + Tab move input focus and SandFly focus to the next and previous item that can take input focus.
- Enter clicks the item with SandFly focus, unless it has input focus.
- Escape is Back.
- In a text field being edited, these keys go to the text field.

In browse mode, single letters move by element, as single letter navigation, quick navigation keys and structural navigation do in NVDA, JAWS and Orca.

Ctrl + Down Arrow and Ctrl + Up Arrow move to the next and previous paragraph. In the NVDA keymap, P and Shift + P also do, in browse mode. They are **Navigate to next paragraph, second key** and **Navigate to previous paragraph, second key** in **Keyboard shortcuts**, where you can change them or assign them in other keymaps.

#### Object navigation {#keyboards-object-navigation}

The **Object navigation** commands move SandFly focus through the items as the app arranges them, including containers that SandFly focus otherwise skips:

- **Move to containing object**
- **Move to first contained object**
- **Move to next object**
- **Move to previous object**

A container without a label is read as "Group" with its number of items.

#### Review cursor {#keyboards-review-cursor}

The review cursor reads the text on screen by line, word and character as it is laid out, top to bottom and left to right, without moving SandFly focus. Items side by side on screen are read as one line. The **Screen review** commands are:

- **Review previous line**, **Review current line**, **Review next line**
- **Review previous word**, **Review current word**, **Review next word**
- **Review previous character**, **Review current character**, **Review next character**
- **Review top line**, **Review bottom line**, **Review start of line**, **Review end of line**
- **Click at review cursor**, **Long click at review cursor**: click the item at the review cursor, or tap the screen there.
- **Move review cursor to SandFly focus**, **Move SandFly focus to review cursor**
- **Turn on review cursor**, **Turn off review cursor**
- **Recognize text on screen**: recognizes the text in a screenshot of the screen and shows it to review, including text that apps don't report. It needs Android 11 or later.

Review commands start where SandFly focus is, unless the review cursor is on. While the review cursor is on, and while recognized text is shown, these keys move and click the review cursor instead of reaching the app:

- Up Arrow and Down Arrow: previous and next line.
- Left Arrow and Right Arrow: previous and next character. With Ctrl: previous and next word.
- Home and End: start and end of the line. With Ctrl: top and bottom line.
- Page Up and Page Down: top and bottom line.
- Enter: click. Shift + Enter: long click.
- Escape: closes recognized text.

In the JAWS keymap, the review cursor works as the JAWS cursor; in the Orca keymap, as flat review. These commands have shortcuts in the NVDA, JAWS and Orca keymaps. In other keymaps, you can assign them in **Keyboard shortcuts**.

### Leave keys to apps {#keyboards-pass-keys}

Some apps use keys that SandFly also uses. These commands leave keys to the app:

- **Pass through to system**: the next key, or keys pressed together, that you press after the shortcut go to the app instead of SandFly. SandFly says "Ignoring next key press".
- **Pass keys without the SandFly key to this app**: the app shown gets every key pressed without the SandFly key, such as the arrow keys and the browse mode letters, until you run the command again in that app. SandFly says "Passing keys to" or "Not passing keys to" and the name of the app, and keeps the choice for each app. While the review cursor is on, it still takes its keys.
- **Turn SandFly keyboard input on or off**: every key goes to Android, shortcuts with the SandFly key too, until you press the shortcut of this command again. SandFly says "SandFly keyboard input off" or "SandFly keyboard input on". Keyboard input is on again when SandFly restarts.

The default shortcuts are:

| Keymap | Pass through to system | Pass keys without the SandFly key to this app | Turn SandFly keyboard input on or off |
| --- | --- | --- | --- |
| SandFly keymap | SandFly key + Shift + Escape | None | SandFly key + Shift + K |
| VoiceOver keymap | None | None | SandFly key + Shift + ; |
| NVDA keymap | SandFly key + F2 | SandFly key + Shift + Space | SandFly key + Shift + K |
| JAWS keymap | SandFly key + 3 | SandFly key + Shift + Z | SandFly key + Shift + K |
| Orca keymap | SandFly key + Backspace | SandFly key + Shift + A | SandFly key + Shift + ; |
| Keyboard phone keymap | SandFly key + P | SandFly key + Shift + Q | SandFly key + Shift + P |
| Keyboard phone single-key keymap | SandFly key + P | Shift + Q | SandFly key + Shift + P |

These commands are in the **Activation** category. A script can also have SandFly pass every key to its app; turning SandFly keyboard input on again ends that. See [](#scripts).

### Hold a touch {#keyboards-touch-hold}

**Hold a touch on the focused item** puts a touch down on the middle of the focused item, as a finger would, and holds it down for as long as you choose. Use it for buttons that act while a finger stays on them, such as the voice message button of Telegram: recording runs while the touch is down, and the message is sent when the touch lifts. **Touch and hold** doesn't do this.

- Hold the keys down: the touch goes down and stays down while you hold them. Releasing the last key of the shortcut, such as V, lifts it; the SandFly key can be released first. SandFly says nothing.
- Press and release the keys within half a second: the touch stays down without the keys. SandFly says "Touch held". To lift it, press the shortcut again, Back or Escape. SandFly says "Touch lifted".

The default shortcut is SandFly key + V in the keyboard phone keymaps; other keymaps have none. While a touch is held, other keys work as usual, and touch is passed through to the app over the whole screen. The touch lifts after 10 minutes, when the item is no longer on the screen, and when SandFly is suspended. Another touch sent to the screen, such as **Activate with a tap**, cancels it.

The command is in the **Activation** category, so you can also assign it to a gesture or add it to the SandFly menu; run that way, the touch stays down until you run the command again or press Back or Escape. It needs Android 11 or later; before that, and when the item isn't on the screen, SandFly says "Not available". When no item has focus, SandFly says "No item has focus".

### Keyboard phones {#keyboards-phones}

Keyboard phones are phones with a built-in keyboard that has letters, Shift, Alt and often Sym, but no Ctrl, Meta or arrow keys: the Unihertz Titan phones, the BlackBerry KEYone, KEY2 and KEY2 LE, and Zinwa phones such as the Q25. SandFly recognizes these phones and uses the **Keyboard phone keymap** on them. You can choose either keymap for keyboard phones on any phone.

- **Keyboard phone keymap**: the most used shortcuts are presses of keys that typing doesn't use, and work anywhere, text fields too. The SandFly key pressed alone moves to the next item, and Shift + the SandFly key to the previous item. Enter activates the item that SandFly focus moved to. Alt pressed twice plays or pauses media and answers calls. Alt + Shift held selects the next reading control. The other shortcuts are the SandFly key with a letter, such as SandFly key + J, I and K for reading controls, and T and B for the top and bottom of the screen.
- **BlackBerry keymap**: the keyboard phone keymap with keys similar to those of the BlackBerry 10 Screen Reader for an external keyboard, with the SandFly key in place of Ctrl. SandFly key + A reads from the top, and SandFly key + Shift + A from the focused item. SandFly key + Enter speaks the focused item. SandFly key + S pauses speech, and SandFly key + Shift + S hides or shows the screen. SandFly key + . says the title of the screen. SandFly key + Shift + T starts Quick Tasks; see [](#gestures-quick-tasks). The arrow keys, such as those of the Zinwa Q25 trackpad, move by the navigation setting with Right and Left, and choose the next and previous navigation setting with Up and Down. Shortcuts of the keyboard phone keymap on those keys move: long press to SandFly key + Shift + Enter, **Search on screen** to SandFly key + Shift + Space, **Open SandFly settings** to SandFly key + Shift + O, and **Describe image** to SandFly key + Z.
- **Keyboard phone single-key keymap**: single keys run commands, such as Space for the next item and Shift + Space for the previous item, H for the next heading, and J, L, I and K for reading controls. Single keys work outside text fields being edited. The SandFly key with any of these keys does the same anywhere, text fields too, so SandFly key + Space moves out of a text field. Shift + Z locks or unlocks touch. The SandFly key pressed twice turns browse mode on or off. In browse mode, letters move by element as in the SandFly keymap, such as B to the next button and K to the next link, and the SandFly key with a letter runs its shortcut outside browse mode, such as SandFly key + T for the top of the screen. See [](#keyboards-browse-mode).

In all three keymaps:

- Enter, pressed after SandFly focus moved to an item, activates that item. After you type, Enter goes to the app, so that it sends a message. Where SandFly focus is on the text field being edited, Enter goes to the app. On a text field that SandFly focus moved to, Enter activates the field, even where it has input focus.
- The commands that need browse mode in other keymaps work without it.
- Arrow key navigation is on by default. A press of a trackpad that sends Enter, such as on the Zinwa Q25, activates the item.
- With Alt as the SandFly key, Alt with a letter types its digit or symbol in a text field being edited.
- Shortcuts with Sym, Fn or Ctrl work in text fields too.
- SandFly key + L locks or unlocks touch in the keyboard phone keymap. See [](#touch).
- SandFly key + V holds a touch on the focused item, such as the voice message button of a messaging app. See [](#keyboards-touch-hold).
- SandFly key + Y spells the password field being edited. See [](#keyboards-passwords).

For the full list of shortcuts, see [](#appendix-keyboard-shortcuts).

#### The SandFly key on keyboard phones {#keyboards-phone-sandfly-key}

The keyboard phone keymaps and the BlackBerry keymap offer Sym, Ctrl, Right Shift, Fn, Right Alt, Alt and Meta as the SandFly key. By default, SandFly chooses a key that typing doesn't need on the phone:

- Ctrl, on Unihertz phones whose keyboard settings set the Fn key to work as Ctrl, except the Titan 2 Elite.
- Right Shift, on the Unihertz Titan 2 Elite, the Zinwa Q25 and BlackBerry phones with a right Shift key.
- Sym, on other phones with a Sym key.
- Alt, on phones without a Sym key, such as the first Unihertz Titan.

With Sym as the SandFly key, the symbol layer of the keyboard isn't available from Sym. On the Zinwa Q25, the key labelled Sym sends Right Alt. On the BlackBerry KEY2, Fn is the Speed key.

#### Physical keyboard settings in Typing {#keyboards-typing-settings}

**Typing** > **Physical keyboard** is shown while a physical keyboard is connected:

- **Letter keys press keypad buttons**: on keyboards whose letters carry numbers, as on BlackBerry, Unihertz and Zinwa phones where W, E and R are 1, 2 and 3, letter keys press the buttons of the lock screen PIN pad, the call keypad, where they play the keypad tones, and calculators, where the Alt characters of letters, such as + and -, press the operator buttons. A letter on the lock screen before the PIN pad shows opens it. Keys with a modifier, digit keys and keys typed into a text field aren't taken. It's on by default, and shown only for such keyboards.
- **Enter sends messages**: in the message field of a messaging app, such as Messages, WhatsApp, Telegram, Signal or Slack, with text in it, Enter presses the Send button of the app. Shift + Enter and Alt + Enter go to the app, which types a new line. While arrow key navigation is on, and in the keyboard phone keymaps, Enter on a message field that SandFly focus moved to doesn't send until the field is activated or typed into. It works with any physical keyboard, and is off by default. The Send key of a keyboard phone, such as the green call key of the Zinwa Q25, presses the Send button in the same message fields whether this setting is on or off. In those apps it also presses a Send button shown outside the field, such as the one for a voice message being recorded; elsewhere it goes to the phone as usual.
- **SandFly keyboard**: opens the list of keyboards in Android settings, where you turn on the SandFly keyboard. See [](#keyboards-sandfly-keyboard).
- **Held keys type**: what a key held down types with the SandFly keyboard.
- **Double-press characters**: what a key pressed twice types with the SandFly keyboard. See [](#keyboards-double-press).
- **Double space types a period**: with the SandFly keyboard, Space pressed twice after a word types a period and a space. Off by default.
- **Speak password characters**: with the SandFly keyboard, in password fields, SandFly says each character typed, deleted or passed over instead of "bullet". Off by default. See [](#keyboards-passwords).
- **Android physical keyboard settings**: opens the physical keyboard settings of Android, such as the keyboard layout.

### The SandFly keyboard {#keyboards-sandfly-keyboard}

The SandFly keyboard is an input method for physical keyboards. It shows no keys on the screen. Keys type as they do without it; the SandFly keyboard adds:

- Exact information about whether a text field takes what you type, so that single-key shortcuts and Enter in the keyboard phone keymaps work in the right places.
- Held keys that type the Alt character of the key or its capital letter.
- Shift pressed and released before a key, which types that key with Shift, as on BlackBerry keyboards. Shift pressed and released again before the key cancels it.
- Double presses that type other characters, such as G G for ğ, and a period for Space pressed twice.
- Emoji search and a symbol list.
- Editing mode, in which single keys move the cursor and select, copy, cut and paste.
- The characters of password fields, which Android gives screen readers only as bullets.

The SandFly keyboard isn't available on watches and TVs.

#### Turn on the SandFly keyboard {#keyboards-turn-on-sandfly-keyboard}

1. Connect the keyboard, or use the built-in keyboard of a keyboard phone.
2. In SandFly settings, open **Typing** > **Physical keyboard** > **SandFly keyboard**.
3. Turn on **SandFly keyboard** in the list of keyboards.
4. In a text field, switch to the SandFly keyboard with the keyboard switcher of Android, or with the **Switch keyboard** command, which switches to the next keyboard that is turned on. **Switch keyboard** needs Android 11 or later.

While the SandFly keyboard is in use, no on-screen keyboard shows.

#### Held keys {#keyboards-held-keys}

**Typing** > **Physical keyboard** > **Held keys type** sets what a printing key held down types with the SandFly keyboard:

- **Alt character**: the Alt character of the key, as on BlackBerry keyboards, or its capital letter where it has none. This is the default.
- **Capital letter**: its capital letter.
- **Repeated character**: the character, repeated, as without the SandFly keyboard.

The key types its own character first; when Android starts to repeat it, the SandFly keyboard puts the held character in its place.

#### Double presses {#keyboards-double-press}

**Typing** > **Physical keyboard** > **Double-press characters** lists keys and what each types when pressed twice with the SandFly keyboard, such as ğ for G, ö for O or ä for E. **Add a key** adds one: enter the character of the key in **Key**, and 1 to 16 characters in **Double press types**. Select a key for **Edit** or **Remove**. The list is empty by default.

The first press types the character of the key. A second press of the same key within 350 milliseconds, with no other key or cursor move between, puts the double-press characters in its place. After a capital, such as with Shift, the double press types the capital, such as Ğ. Keys not in the list type as usual.

With **Double space types a period** on, Space pressed twice after a letter or digit puts a period and a space in place of the first space.

Double presses don't apply in password fields.

#### Search emoji and choose symbols {#keyboards-emoji}

**Search emoji** and **Choose a symbol** open a list in the text field being edited:

1. Run **Search emoji** or **Choose a symbol**. SandFly says "Emoji search" or "Symbols". The characters you chose most recently come first.
2. Type words of the name of the character, such as "thumbs up". Each typed word must start a word of the name. Emoji names are in the language of your phone.
3. Press the arrow keys, or Tab and Shift + Tab, to move through the matches. SandFly reads each match with its position, such as "1 of 5".
4. Press Enter to type the character.

Backspace deletes the last letter typed, and closes the list when nothing is typed. Back or Escape closes the list. If the SandFly keyboard isn't in use in a text field, the commands say "Needs the SandFly keyboard in a text field".

In the keyboard phone keymaps, **Search emoji** is SandFly key + O, and **Choose a symbol** is Alt + Space while editing. On the Zinwa Q25, Alt + Space opens the symbol list too. In other keymaps, you can assign them in **Keyboard shortcuts**, in the **Text editing** category.

#### Editing mode {#keyboards-editing-mode}

**Turn editing mode on or off** turns on editing mode in the text field being edited. SandFly says "Editing mode on". In editing mode, single keys move the cursor and edit instead of typing, and SandFly reads the moves:

- J and L, or Left Arrow and Right Arrow: previous and next character.
- I and K, or Up Arrow and Down Arrow: previous and next line.
- U and O: previous and next word.
- Alt + J and Alt + L: start and end of the line. Alt + I and Alt + K: start and end of the text.
- T and B: start and end of the text.
- Home and End: start and end of the line. With Ctrl: start and end of the text.
- Shift with a move selects. S starts selecting, and SandFly says "Selecting"; press S again to stop.
- A selects all, C copies, X cuts, V pastes.
- Z undoes, Shift + Z redoes.
- R reads the selection, or the line if nothing is selected.
- Delete keys delete.
- Enter, Escape or Back turns editing mode off.

Other keys that type do nothing in editing mode. In a password field, R says "Not read in password fields", and C copies without saying the text. Editing mode needs the SandFly keyboard. In the keyboard phone keymaps, its shortcut is SandFly key + C.

#### Password fields {#keyboards-passwords}

Android gives screen readers only bullets for the text of a password field, so SandFly says "bullet" for each character. The SandFly keyboard reads the text of the field it types into, and SandFly can say the characters in two ways.

**Typing** > **Physical keyboard** > **Speak password characters**, off by default: in a password field being edited with the SandFly keyboard, SandFly says:

- Each character typed, such as "capital P", "at" or "1". Characters are named as in other text fields, and capital letters follow **Verbosity** > **Capital letters**.
- Each character deleted with Backspace or Delete, such as "a deleted".
- The character the cursor passes over with Left Arrow or Right Arrow, the trackpad or editing mode.
- For a key held down for its Alt character, the character and the one it replaced, such as "at replaced a".

Text pasted or filled in, a cleared field, a selection and moves over more than one character are spoken as before, with bullets.

**Spell the password field**, in the **Text editing** category of commands, spells the whole text of the password field being edited, each character with its position, such as "1 capital P, 2 a, 3 at, 4 1". Capital letters are always said as "capital". It works whether **Speak password characters** is on or off. In a text field that isn't a password field, it says "Not a password field"; without the SandFly keyboard in a text field, "Needs the SandFly keyboard in a text field". In the keyboard phone keymaps, its shortcut is SandFly key + Y. In other keymaps, assign it in **Keyboard shortcuts**. You can also assign it to a gesture or add it to the SandFly menu.

Password characters are spoken only in these two ways. They aren't kept for **Repeat last spoken phrase**, **Spell last spoken phrase**, **Copy last spoken phrase** or **Speech history**, and SandFly logs only how many characters a field has.

Android's own text fields give the SandFly keyboard their text, and so do most apps. Where an app gives none, SandFly says "bullet" as before, and **Spell the password field** says "Password field text not available".

### Other keyboard settings {#keyboards-other-settings}

- **Typing** > **Keyboard echo** > **Physical keyboard** sets what SandFly says as you type on a physical keyboard. See [](#typing).
- Every key on a physical keyboard stops speech, except modifier keys such as Shift, Caps Lock, Num Lock, Scroll Lock and the volume keys. **Speech** > **Interrupt speech when typing** sets when typed characters stop speech, and **Speech** > **Interrupt speech on Enter** whether Enter does. Ctrl pressed alone pauses or resumes speech. See [](#speech-stop).
- With a physical keyboard connected the first time SandFly starts, the keyboard tutorial opens instead of the touch tutorial. When you open the tutorial later with a keyboard connected to a device with a touch screen, SandFly offers **Touch tutorial** and **Keyboard tutorial**. See [](#getting-started).
