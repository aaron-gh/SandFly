## Macros {#macros}

A macro is a named list of steps that SandFly runs one after another: commands, taps on items, typed text, waits, and steps that check the screen and repeat or skip other steps. Each macro is a command, so a gesture, a keyboard shortcut, a menu item, a voice command or a home screen shortcut can run it.

You make macros in **Macros** in SandFly settings, by adding steps or by recording what you do. Macros aren't available on watches. For scripts, which can do more than macros, see [](#scripts).

### Make a macro {#macros-add}

1. In SandFly settings, choose **Macros**.
2. Choose **Add macro** and enter a name. SandFly opens the screen of the new macro.
3. Choose **Add step**, then the kind of step, and fill in what it does. See [](#macros-steps).
4. Repeat step 3 for each step.
5. To try the macro, choose **Run now**.
6. To run it with a gesture or keyboard shortcut, choose **Gestures and keyboard shortcut**. See [](#commands-assign).

To make the steps by doing them instead, see [](#macros-record).

### The Macros screen {#macros-screen}

The **Macros** screen has:

- **Add macro**: makes a macro for all apps.
- **Add app**: chooses an app and opens the list of its macros, where **Add macro** makes a macro that runs only in that app.
- **Import from file** and **Export to file**: read macros from a file, or save all macros to one. On the list of an app, **Export to file** saves the macros of that app. See [](#api-item-files).
- **All apps**: the macros that run in every app.
- **Apps**: each app that has macros, with the number of its macros. Choose an app to open its list.

Macros are also saved in settings backups. See [](#profiles).

### The macro screen {#macros-macro-screen}

The screen of a macro has:

- **Run now**: runs the macro. When SandFly is off, it shows "SandFly is off".
- **Add step**: adds a step. Under it, SandFly shows where the step goes: **At the end**, **At the start**, or after a step. A new step goes after the step that you added last, so that steps added after **If** or a repeat go inside its block.
- **Record steps**: records steps where **Add step** adds them. See [](#macros-record).
- **Run only when**: a condition that has to be met for the macro to run. **Always** by default. When the condition isn't met, the macro does nothing, and **Run now** says "Macro not run. Run only when: not met". See [](#macros-conditions).
- **App**: the app the macro runs in, or **All apps**, the default. A macro with an app runs only while that app is in front.
- **Show in Actions**: lists the macro in the **Actions** menu and the **Actions** reading control of every item in its app, or in every app for **All apps**, after the actions of the app. Choosing it there runs the macro. Off by default.
- **Show in Actions only when**: shown with **Show in Actions** on. A condition that has to be met for the focused item to list the macro. With a condition, the macro counts as an action of the item for **Change reading control automatically**, so the **Actions** reading control is selected on it; without one it doesn't.
- **Steps**: the steps in order. Steps inside an **If** or a repeat are indented.
- **Gestures and keyboard shortcut**: the gestures, arrow keys, braille cells and keyboard shortcut that run the macro.
- **Add to home screen**: adds a home screen shortcut that runs the macro on the screen that you open it from. Renaming the macro renames the shortcut; deleting the macro disables it. Not every launcher supports shortcuts.
- **Rename**, **Export to file** and **Delete macro**.

Choose a step for these actions, which are also accessibility actions on the step: **Edit**, **Add step after**, **Move up**, **Move down** and **Delete**. Deleting an **If** or a repeat also deletes its **Else if**, **Else** and end steps, and keeps the steps inside it. A macro has at most 200 steps.

When the blocks of a macro don't match, such as an **If** without **End if**, the screen shows the step and the problem under **Steps**, and the macro doesn't run.

### Steps {#macros-steps}

**Add step** offers these steps:

- **Run command**: runs any command except a macro. Choose a category, then the command.
- **Tap item**: activates the first item on the screen whose text or description contains a text, ignoring case. An item whose whole text is the text comes first. SandFly waits for the item to appear, 3 seconds by default, set by **Seconds to wait for the item**.
- **Tap at position**: taps the screen at an X and a Y in pixels from the top left. Needs Android 11 or later.
- **Touch and hold item**: touches the centre of an item for **Seconds to hold**, 1 second by default, then lifts in place or slides up, down, left or right by a distance in dp before it lifts. For buttons that act while they are held, such as the voice message button of a chat app.
- **Focus item**: finds an item as **Tap item** does and moves accessibility focus to it, which reads it.
- **Speak item**: finds an item as **Tap item** does and reads its name without moving focus.
- **Long-press or scroll an item**: long-presses, scrolls forward or scrolls backward an item a number of times. For an item that can't do it, SandFly uses the nearest item around it that can, such as a seek bar. A scroll with no item text scrolls the largest list on the screen.
- **Speak text**: reads a text.
- **Type text**: types a text in the focused edit field, at the cursor. With **Replace text in field**, the text replaces the text of the field.
- **Open app**: opens an app.
- **More steps**: steps that act outside the screen. See [](#macros-more-steps).
- **Wait**: waits a number of seconds, up to 600.
- **Wait until**: waits until a condition is met. **Seconds to wait** is 10 by default; the macro stops when the time runs out.
- **If**, **Else if**, **Else** and **End if**: run the steps of the first branch whose condition is met. See [](#macros-if).
- **Repeat**, **Repeat while**, **End repeat** and **Exit loop**: run steps more than once. See [](#macros-repeat).
- **Stop macro**: stops the macro.

When a step fails, such as an item that isn't found, SandFly says "Macro stopped. Failed:" and the step, and the macro stops.

#### Find items {#macros-find-items}

The steps and tests that find an item have these fields:

- **Item text**: the text to look for.
- **Regular expression**: reads **Item text** as a regular expression, matched anywhere in the text and ignoring case. Repeated groups can't contain repetition or `|`, such as `(a+)+`.
- **Match in**: where to look. **Any text**, the default, or only **Text**, **Description**, **Hint**, **State**, **Tooltip** or **Resource ID**. A resource ID matches with or without its package, so `send` matches `com.example:id/send`.

The tests also have **Item type**: **Any type**, or **Clickable**, **Long-clickable**, **Editable**, **Scrollable**, **Adjustable**, **Checkable**, **Checked**, **Selected**, **Heading** or **Enabled**.

To see what an item has in each field and which item types it is, use **Inspect focused item**. See [](#macros-inspect).

#### More steps {#macros-more-steps}

- **Play sound**: plays one of the SandFly sounds.
- **Vibrate** and **Play tone**.
- **Copy text**: copies a text to the clipboard.
- **Open link**: opens an address.
- **Open settings page**: opens a page of Android settings, such as Wi-Fi, Bluetooth or Battery.
- **Share text**: opens the share screen with a text.
- **Set volume**: sets the volume of a stream in percent, such as media, ring or accessibility.
- **Set ringer mode**: normal, vibrate or silent.
- **Device setting**: changes a control of the SandFly quick settings panel, such as Wi-Fi, the flashlight, Do Not Disturb or brightness: **Turn on**, **Turn off** or **Turn on or off** for a switch; a number, a percent such as 50%, or +1 or -1 for a slider; an option, or next, for a choice. See [](#display).
- **Media key**: **Play or pause**, **Play**, **Pause**, **Next**, **Previous**, **Stop**, **Rewind** or **Fast forward**.
- **Set timer**: a number of seconds, up to 86,400, with an optional label.
- **Set alarm**: a time such as 08:30, with an optional label.
- **Call**: calls a phone number.
- **Send message**: writes a message to a phone number.
- **Open camera**: for a photo or a video.
- **Press keys**: presses keys, such as `tab enter`. Needs Shizuku. See [](#updates).
- **Show message**: shows a dialog with a text, and waits until you close it.
- **Set value**: keeps a text as a value with a name.
- **Fetch link**: keeps the text at an https address as a value.
- **Translate text**: translates a text into the language of the device and keeps it as a value. See [](#translation).

Value names use letters, digits, dots, dashes and underscores. Values are kept between runs, for all your macros together.

The texts of **Speak text**, **Type text** and the steps above can hold values as `{name}`, and these values that every run has: `{item}`, the text of the focused item; `{clipboard}`; `{time}`; `{date}`; `{battery}`, the battery level; and `{device.wifi}` and the like, the state of a device setting.

### Conditions {#macros-conditions}

A condition is a list of tests. **If**, **Else if**, **Repeat while**, **Wait until**, **Run only when** and **Show in Actions only when** each have one. Choose the step, or the setting, to open its condition.

The condition screen has **Add test** and **Tests**, and for **Wait until**, **Seconds to wait**. A condition without tests is always met.

The tests are:

- **Item on screen**: an item on the screen with a text, and of an item type if you choose one. See [](#macros-find-items).
- **Focused item**: the item with accessibility focus, or the items inside it, have a text, an item type, or both. The text is optional.
- **Focus is inside an item**: the focused item is inside an item with a text, or is one, such as a video player found by its resource ID.
- **App in front**: an app is in front.
- **Keyboard shown**: the on-screen keyboard is shown.
- **Screen locked**: the lock screen is shown.
- **Value**: a value contains a text, or, with **Contains** empty, is set.

When a condition has tests, **Add test** first asks for **Join with and** or **Join with or**. Choose a test for these actions:

- **Edit**.
- **Change to**: reverses the test, such as **Keyboard hidden**, **Screen unlocked**, **No item on screen** or **App not in front**.
- **Join with and** or **Join with or**, for tests after the first.
- **Move up**, **Move down** and **Delete**.

"And" goes before "or": the condition is met when all the tests of one of the groups that "or" separates are met. A condition has at most 20 tests.

#### If {#macros-if}

**Add step** > **If** asks for a test and adds **If** with its **End if**. The steps that you add next go between them. **Else if** adds another branch with a condition, and **Else** a branch for when no condition before it is met. **Else if** or **Else** added right after **End if** goes before it.

#### Repeat {#macros-repeat}

- **Repeat**: runs the steps up to **End repeat** a number of times, from 1 to 1,000.
- **Repeat while**: runs the steps up to **End repeat** while a condition is met, up to 1,000 times in one run of the macro.
- **Exit loop**: goes on after the **End repeat** of the repeat that it is in.

**Add step** adds a repeat with its **End repeat**, and the steps that you add next go inside it.

To scroll a list until an item appears and then open it:

1. Add **Repeat while** with the test **Item on screen**, the text of the item, and **Change to** so that it reads **No item on screen**.
2. Add **Long-press or scroll an item**, **Scroll forward**, with no item text.
3. After **End repeat**, add **Tap item** with the text of the item.

### Record a macro {#macros-record}

SandFly can record the steps of a macro as you do them in apps.

To record a new macro:

1. Go to the screen where the macro starts.
2. Use the **Record a macro** command, in the **Macros** category. Assign it to a gesture first, or add it to the SandFly menu. See [](#commands-assign) and [](#commands-edit-menu).
3. SandFly makes a macro named "Recorded" and the date and time, and shows the notification **Recording macro steps**.
4. Do the steps.
5. To stop, use **Record a macro** again, use **Stop macro**, or open the notification. SandFly opens the macro and lists the recorded steps.
6. Choose **Save** to add the steps, **Record again** to start over, or **Cancel**. **Cancel** deletes the macro.

To record steps into an existing macro, choose **Record steps** on its screen. SandFly goes back to the app that was open before settings, or opens the app of the macro, and records. The steps go where **Add step** adds a step.

SandFly records presses and long presses on items, by touch or with SandFly, scrolls, typed text, apps opened from the launcher, and the **Back**, **Home**, **Overview**, **Notifications**, **Quick Settings**, **All apps**, **Select all**, **Copy**, **Cut** and **Paste** commands. A notifications, Quick Settings, overview or all apps command used to stop the recording at the end is left out. Text typed in password fields and the screens of SandFly aren't recorded.

The recorded steps also hold what the actions imply:

- Scrolls in a list followed by a press on an item in it become a **Repeat while** that scrolls until the item is on the screen, then the press.
- An action, or a run of actions, done several times in a row becomes a **Repeat**, or a **Repeat while** the first item pressed is on the screen when it was gone after the last time.
- A press that turns a switch or check box on or off runs only when it is off or on.
- A press in the launcher that opened an app becomes **Open app**.
- When the first action is in the app that was in front at the start, the macro opens that app first when it isn't in front.

Recorded steps find items by their whole text, as a regular expression. Edit the steps afterwards to make them find items by part of the text.

### Run a macro {#macros-run}

- A gesture, keyboard shortcut, arrow key or braille cell assigned to it. See [](#commands-assign).
- A menu item: add the macro to the SandFly menu, a custom menu or a custom reading control from the **Macros** category. See [](#commands).
- The **Actions** menu or reading control, with **Show in Actions** on.
- A home screen shortcut, with **Add to home screen**.
- A voice command: say the name of the macro, or "run the" name "macro". A custom voice command can run it too. See [](#voice-commands).
- **Run now** on the macro screen.

Starting a macro stops the macro that is running. The **Stop macro** command stops the macro running. Deleting a macro removes it from the gestures, keys and menus that run it.

### Inspect items for macros {#macros-inspect}

**Inspect focused item**, a command in the **Developer settings** category, opens **Item details** for the focused item. The window has these headings:

- **Texts**: the text, description, hint and state of the item, named as **Match in** names them.
- **Item types**: the item types that the item is, such as **Clickable** or **Heading**.
- **Items inside**: the texts of the items inside it.
- **Script details**: the resource ID, role, class and other details that scripts use.
- **App**: the app, activity and window.

Activate a line to copy its value. **Copy all** copies all the details as text. In builds with scripts, **Add a rule for this item** opens the script builder with the item. See [](#scripts).

**Copy item details** copies the same details without opening the window, and says "Copied item details". **Copy screen tree** copies all items on the screen as text, one line for each item with its class, ID, role, states and texts, for writing scripts.

**Inspect focused item** and **Copy item details** can be added to the SandFly menu; they are off by default. To inspect an item, move focus to it, then open the SandFly menu and choose **Inspect focused item**: the window shows the item that had focus before the menu.
