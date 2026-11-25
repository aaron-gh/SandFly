## Commands and menus {#commands}

Everything SandFly does on request is a command: moving focus, reading from the top, describing an image, opening the SandFly menu, running a macro. A command runs from a gesture, a keyboard shortcut, an arrow key, a braille cell, a menu item, a reading control or a voice command. This chapter describes the list of commands and the menus built from them: the SandFly menu, custom menus and the list of reading controls.

For gestures and gesture profiles, see [](#gestures). For keyboard shortcuts and keymaps, see [](#keyboards). For how to use reading controls while you read, see [](#reading). For voice commands and macros, see [](#voice-commands) and [](#macros).

### Commands {#commands-list}

**Commands** in SandFly settings lists every command, in categories. Each category opens the list of its commands. Under the name of a command, SandFly lists the gestures and the keyboard shortcut that run it.

**Search commands**, at the top of the screen, stays in place while the list scrolls. As you type, the list shows the commands of every category that have each word you typed in their names, also inside a word, with the category under each name. Names that start with what you typed come first. Case and accents are ignored. SandFly says how many commands it found. Clear the field to see the categories again.

The list holds only the commands that the device supports. For example, commands that need Android 11 or later, a vibrator or a braille display aren't listed on devices without them.

#### Command categories {#commands-categories}

- **Navigation**: next and previous item, the item above, below, left and right, first and last item on the screen, containers, windows, and **Focus text inside items separately**.
- **Navigation setting**: the **Navigation** menu, **Page navigation**, and next and previous navigation setting.
- **Object navigation** and **Screen review**: commands for desktop-style object navigation and the review cursor, mostly for keyboards. See [](#keyboards).
- **Activation**: **Activate**, **Touch and hold**, **Activate with a tap**, **Hold a touch on the focused item**, **Activate confirm button**, **Activate cancel button**, **Show custom actions**, **Addresses and numbers**, **Add or edit labels**, **Open SandFly menu**, **Pass through system gesture**, **Turn direct touch on or off for this app**, and **Double tap taps the screen in this app**.
- **Scrolling**: scroll forward, back, up, down, left and right, to the top and to the bottom, and **Refresh**.
- **Navigate by text** and **Navigate by element**: next and previous character, word, line, paragraph, heading, link, control, button, list, table, landmark and other kinds of item.
- **Tables**: next and previous row and column, the start and end of rows, columns and tables, and row and column headers.
- **Web**: **Turn browse mode on or off**, lists of headings, landmarks, links, controls and tables, and the address of a link or page. See [](#web).
- **Reading**: **Read from top**, **Read from focused item**, **Read from focused item by reading control**, repeat, spell and translate the last spoken phrase, **Speech history**, **Speak status**, **Speak position**, **Speak coordinates**, the date and time, the battery level and the window title.
- **Speech**: **Pause or resume speech**, **Turn speech on or off**, speech rate, pitch and volume, spoken language, voice profiles, punctuation, keyboard echo, **Use voice effects**, **Audio ducking** and **Vibration feedback**.
- **Reading controls**: next and previous reading control, **First reading control**, **Move reading control down or forward** and **Move reading control up or backward**, **Write the letter of a reading control**, next and previous reading control list, and a **Set reading control to** command for each reading control in the list. See [](#commands-reading-controls).
- **Text editing**: selection mode, cursor movement, select all, copy, cut, paste, **Clipboard history**, **Editing menu**, **Submit text**, **Clear text**, **Copy last spoken phrase**, **Copy text from screen**, **Continuous copy**, **Granular editing**, **Spelling / grammar suggestions**, **Search emoji**, **Choose a symbol**, **Turn editing mode on or off**, **Spell the password field**, **Dictate**, **Start voice editing** and **Switch keyboard**. See [](#typing).
- **System actions**: **Home**, **Back**, **Overview**, previous, older and newer app, **Take screenshot**, **Answer call**, **End call**, **Split screen**, notifications, Quick Settings, **Lock screen**, **Power menu**, **All apps**, the accessibility shortcut, magnification, color inversion, **Write an app's name to open it**, **Type an app's name in braille to open it**, and **Start voice command**.
- **Media and volume**: play or pause, next and previous track, rewind, fast forward, and volume.
- **Notifications**: read notifications, read the last notification again, stop reading, and turn notification speech and time announcements on or off. See [](#notifications).
- **Image and screen descriptions**: **Take picture and describe**, **Describe image**, **Describe screen**, their **Speak** and **Ask about** versions, **Share image of focused item**, **Share screenshot** and **Report AI response**. With SandFly Pro, also the screen recognition commands. See [](#descriptions).
- **Live recognition**: a command for each Live recognition mode, and **Stop Live recognition**.
- **Braille**: **Switch to braille keyboard**, **Braille commands**, **Find text**, braille command profiles, and braille display commands. See [](#braille).
- **SandFly**: **Pause SandFly**, **Hide or show screen**, **Show the focus indicator**, **Screen search**, gesture and settings profiles, **Gestures for this app**, **Turn arrow key navigation on or off**, **Lock or unlock touch**, **Turn all scripts on or off**, **Script commands**, **Verbosity**, **Turn off SandFly**, **Open tutorial**, **Practice gestures** and **Turn keyboard practice on or off**. With SandFly Pro, also **Start or stop AI agent task**.
- **Developer settings**: **Inspect focused item**, **Copy item details** and **Copy screen tree**. See [](#macros-inspect).
- **Setting values**: a next value and a previous value command for each reading control that changes a setting, such as **Punctuation: next value**, and commands that turn settings such as **Wrap around** on or off.

These categories list what you add yourself, and are shown only when they have commands:

- **Scripts**: the commands that installed scripts add. See [](#scripts).
- **Custom menus**: one command for each custom menu, which opens it. See [](#commands-custom-menus).
- **Macros**: one command for each macro, **Stop macro**, and **Record a macro**. See [](#macros).
- **Gesture profiles**, **Braille command profiles** and **Voice profiles**: one command for each profile, which puts it in use.
- **Reading control lists**: **Next item in** and **Previous item in** a list, for each list. See [](#commands-reading-control-lists).
- **Open app**: one command for each installed app.
- **App shortcuts** and **Accessibility shortcuts**: the shortcuts of apps and of other accessibility features. See [](#gestures).
- **App tiles**: the quick settings tiles of installed apps. SandFly clicks the tile in the system quick settings, which it opens and closes. With Shizuku running, SandFly first adds the tile to the system quick settings when it isn't there, and clicks it through Shizuku when it isn't found there. A tile is named as the system quick settings showed it when SandFly last read them, such as the name of the device that a Bluetooth tile connects.
- **System tiles**: the tiles of the system quick settings that **Add system tile** in the quick settings panel settings found last. With Shizuku running, **Add system tile** gives SandFly DUMP and usage access and reads the tiles from the system; otherwise it looks through the system quick settings. While SandFly has DUMP and usage access, it reads the tiles again when it starts. A system tile that SandFly has a control for, such as Wi-Fi, Hotspot or the sound mode, is that control, named as in SandFly's quick settings panel; the Internet tile is Wi-Fi. When a control needs Shizuku and Shizuku isn't running, SandFly announces "Shizuku not running". Other system tiles, such as screen recording, are named as the system names them, and are activated in the system quick settings, which SandFly opens and closes; SandFly then announces the new state. See [](#display-quick-settings-edit).

#### Speak status {#commands-status}

**Speak status**, in the **Reading** category, reads what the status bar shows, such as the time, the battery level and the network, and whether the screen is hidden. By default, a two-finger triple-tap runs it. To choose what it reads and in what order, go to **Verbosity** > **Status readout**. See [](#verbosity).

#### Activate confirm button and Activate cancel button {#commands-button-commands}

**Activate confirm button** and **Activate cancel button**, in the **Activation** category and unassigned by default, activate a button on the screen by its label, without moving focus. **Activate confirm button** looks for labels such as **OK**, **Yes**, **Send**, **Done**, **Next**, **Save** and **Allow**; **Activate cancel button** for labels such as **Cancel**, **No**, **Close**, **Back**, **Not now** and **Skip**. A label matches when it is the same word, ignoring case and punctuation at the end.

SandFly looks in the active window first, such as a dialog, then in the other app windows from the top. When several buttons match, the one nearest to the focused item is activated; with no focused item, the lowest one on the screen. When no button matches, SandFly says "No confirm button" or "No cancel button".

To change the labels, choose the command in **Commands**, then **Button labels**, and enter one label per line. Your list replaces the built-in one. To use the built-in list again, delete every label.

#### Answer call and End call {#commands-calls}

**Answer call** and **End call**, in the **System actions** category and unassigned by default, act on phone calls and on the calls of apps such as WhatsApp.

- **Answer call** answers the ringing call. SandFly says "No incoming call" when none rings.
- **End call** declines the ringing call, or else ends the call in progress. SandFly says "No call" when there is none.

Phone calls are answered and ended through Android. The first time, SandFly requests permission to answer calls. The calls of apps are answered, declined and ended with the buttons of their call notification. Ending a call in progress needs notification access for SandFly. See [](#notifications-access).

### Assign a gesture or keyboard shortcut to a command {#commands-assign}

1. In SandFly settings, choose **Commands**.
2. Choose a category, then a command.
3. Under **Gestures**, choose **Add gesture**, then a gesture. A gesture that runs another command shows that command after its name. Or choose **Perform gesture**, make the gesture, then choose **Assign**. **Perform gesture** works only while SandFly is on.
4. If the gesture already runs another command, SandFly asks whether to use it for this command instead. Choose **Replace**.

While **Perform gesture** waits, the next gesture is taken for the command and doesn't run its own command. Some gestures still run their own command, such as those that the gesture profile in use gives a command. A gesture that can't be assigned on this screen isn't assigned; SandFly says "Can't assign" and the name of the gesture.

The command screen has these items and groups:

- **Run command**: closes SandFly settings, which brings back the app shown before, and runs the command there after one second. Available only while SandFly is on.
- **Add to SandFly menu**: adds the command at the end of the SandFly menu. While the command is in the SandFly menu or one of its submenus, the item is **Remove from SandFly menu**, which takes it out of the SandFly menu and its submenus. Custom menus aren't changed. See [](#commands-edit-menu).
- **Button labels**: only for **Activate confirm button** and **Activate cancel button**, the labels that the command looks for. See [](#commands-button-commands).
- **Gestures**: the gestures that run the command. To remove one, choose it, then **Remove gesture**.
- **Arrow key navigation**: the arrow keys of arrow key navigation that run the command, with **Add arrow keys** and **Remove arrow keys**. The group is shown only with a keymap that has arrow key navigation. See [](#keyboards).
- **Braille cells**: the braille cells that run the command in the **Default** braille commands. **Add braille cell** records a cell that you type on the SandFly braille surface. To remove a cell, choose it, then **Remove braille cell**. See [](#braille).
- **Keyboard shortcut**: the keyboard shortcut of the command. Choose it and press the new keys. Recording a shortcut works only while SandFly is on.

A gesture or arrow key runs one command. A command can have several gestures. To see every gesture at once, use the **Gestures** screen. See [](#gestures).

The menus, macros and voice commands that this chapter describes have a **Gestures and keyboard shortcut** item on their own screens, which opens this screen for the command that runs them.

### SandFly menu {#commands-sandfly-menu}

The SandFly menu lists commands and settings for the focused item and for SandFly. To open it, use the **Open SandFly menu** command. In the SandFly gesture layout, swipe up then right, swipe down then right, or tap with three fingers. See [](#appendix-gestures).

The menu opens as a list. Swipe right or left to move through the items, and double-tap to choose one. An item that opens more items opens a submenu. To close the menu without choosing an item, use **Back**.

Some items appear only when they apply to the focused item or the screen:

- **Spelling / grammar suggestions**: when the cursor is on a typo.
- **Actions**: when the focused item has actions, such as links, copy, paste and select.
- **Page navigation**: when the content has several pages.
- **Add or edit labels**: on an unlabeled item.
- **Addresses and numbers**: when the text of the focused item has web addresses, email addresses, phone numbers or codes.
- **Split screen**: when the split screen divider is on the screen.
- Script items: when an installed script adds items to the menu.

#### Circle menu {#commands-circle-menu}

**SandFly menu** > **Circle menu** shows the SandFly menu as a circle in the middle of the screen. Each item is a slice of the screen around the middle. Slide your finger to an item and lift it to choose the item, or lift in the middle of the circle to close the menu. An item that opens more items shows them in a new circle. Off by default.

If you open the menu with a gesture that ends with your fingers on the screen, such as a double-tap and hold, slide those fingers to an item and lift them. The place where your fingers were is the middle of the circle.

### Change the SandFly menu {#commands-edit-menu}

The SandFly menu is a list of commands and submenus. Any command can be in it.

1. In SandFly settings, choose **SandFly menu**.
2. Choose **Add command**. The commands are listed by category. Categories are collapsed; double-tap a category to expand it.
3. Double-tap a command. SandFly adds it at the end of the menu and announces "added". The command leaves the list, and focus stays in the category.

You can also add a command from its screen in **Commands**, with **Add to SandFly menu**. See [](#commands-assign).

To add a submenu, choose **Add submenu** and enter its name. Choose the submenu in the list and then **Open** to add commands and more submenus to it, with **Add command** and **Add submenu** on its own screen. Submenus can be nested up to 12 levels.

**Current** lists the items of the menu in order. Choose an item for these actions, which are also accessibility actions on the item:

- **Open**: opens a submenu.
- **Move up** and **Move down**: move the item one place.
- **Move to**: moves the item to the end of another menu or submenu, named by its path, such as "SandFly menu > Reading". A command already there isn't added twice.
- **Rename** and **Delete**: rename or delete a submenu. Deleting a submenu deletes everything in it.
- **Remove**: removes a command from the menu.

Some items have options, which follow the item in **Current** and are hidden while the item isn't in the menu:

- **Addresses and numbers to include**, after **Addresses and numbers**: which of **Web addresses**, **Email addresses**, **Phone numbers** and **Codes** the item lists. All by default.
- **Edit navigation options**, after **Navigation**: which navigation settings the **Navigation** submenu offers.

**Circle menu** follows the list. See [](#commands-circle-menu).

**Reset to default** restores the default items and order of the SandFly menu, after you confirm.

#### Default items {#commands-menu-defaults}

By default, the SandFly menu has **Spelling / grammar suggestions**, script items, **Describe image**, **Describe screen**, **Read from focused item**, **Gesture profile**, **Gestures for this app**, **Hide or show screen**, **Pause SandFly**, **Turn off SandFly**, **SandFly settings**, **Send feedback**, **Text-to-speech** and **Split screen**. With SandFly Pro, it also has the screen recognition items. Items such as **Actions**, **Screen search**, **Navigation**, **Read from top**, **Copy last spoken phrase**, **Voice commands**, **Verbosity**, **Spoken language**, **Inspect focused item** and **Copy item details** are off by default; add them with **Add command**.

On watches, the SandFly menu settings are a list of switches in a fixed order. See [](#devices).

### Custom menus {#commands-custom-menus}

A custom menu is a menu of commands and submenus that you make, apart from the SandFly menu. Each custom menu is a command in the **Custom menus** category, so a gesture, a keyboard shortcut, an arrow key or a menu item can open it.

To make a custom menu:

1. In SandFly settings, choose **Custom menus**.
2. Choose **Add menu** and enter a name.
3. Choose the new menu, then **Add command** or **Add submenu**. The menu screen works as the SandFly menu screen does. See [](#commands-edit-menu).
4. To open the menu with a gesture or keyboard shortcut, choose **Gestures and keyboard shortcut**. See [](#commands-assign).

The menu screen also has **Rename** and **Delete**. Deleting a menu deletes everything in it.

A custom menu added to another menu, or to the SandFly menu, appears there as a submenu with its current items. A menu inside itself is left out.

### Reading controls settings {#commands-reading-controls}

Reading controls change what swipe up and swipe down do, such as moving by words or headings, or changing the speech rate. **Reading controls** in SandFly settings sets which reading controls are in the list that you move through, and in what order. For how to use them, see [](#reading).

The screen has these groups:

- **Current**: the reading controls in the list, in order. Choose a control for **Move up**, **Move down** and **Remove**, which are also accessibility actions on the control.
- **Options**: the settings of the screen. See [](#commands-reading-control-options).
- **Not in reading controls**: the reading controls that aren't in the list, in groups. Double-tap a control to add it at the end of **Current**. Groups are collapsed until you double-tap them.

When you change the reading control or its value with a gesture, SandFly also shows the change on the screen for about 3 seconds.

#### Reading controls you can add {#commands-reading-control-groups}

- **Actions for current item**: **Actions**, **Addresses and numbers**, **Clipboard history** and **Proofread**, which moves between spelling and grammar suggestions.
- **Basic navigation**: **Default navigation**, **Windows**, **Containers**, **Screen search**, **Vertical navigation**, **Scroll** and **Wrap around**.
- **Navigate by text**: **Characters**, **Words**, **Lines**, **Sentences** and **Paragraphs**.
- **Element navigation**: **Headings**, **Links**, **Controls**, **Landmarks**, **Buttons**, **Checkboxes**, **Radio buttons**, **Edit fields**, **Combo boxes**, **Focusable items**, **Images and videos**, **Lists**, **List items**, **Tables**, **Visited links** and **Unvisited links**.
- **Heading levels**: **Heading 1** to **Heading 6**.
- **Tables**: **Rows and columns**, **Row** and **Column**.
- **Speech**: **Speech rate**, **Speak punctuation and symbols**, **Emoji**, **Speak text formatting**, **Spoken language**, **Speech engine**, **Speech volume**, **Audio output device**, **Voice profile**, **Automatic translation** and **Speak only focus moved by the user**.
- **Keyboard echo**: the echo of the on-screen keyboard, physical keyboard, braille keyboard, braille display and handwriting keyboard.
- **Voice effects**: **Voice effects**, and one control for each equalizer band from 64 Hz to 16 kHz. See [](#voice-effects).
- **Image and screen descriptions**: **Live recognition**, **AI actions** and **AI service**. See [](#commands-ai-actions) and [](#descriptions-choose-service).
- **Sound and vibration**: **Audio ducking**, **Accessibility volume**, **Media volume**, **Call volume**, **Ring volume**, **Notification volume**, **Alarm volume** and **Sound mode**.
- **Display**: **Hide screen**, **Brightness**, and **Tablet held up faces away**, for the braille keyboard on a tablet or an unfolded phone.
- **Touch**: **Focus delay**, **Typing focus delay**, **Lift to activate** and **Gesture profile**.
- **Custom reading controls**: the reading controls that you make. See [](#commands-custom-reading-controls).

By default, the list has **Actions**, **Proofread**, **Screen search**, **Characters**, **Words**, **Lines**, **Paragraphs**, **Headings**, **Links**, **Controls**, **Rows and columns**, **Speech rate**, **Live recognition**, **AI actions**, **Brightness** and **Gesture profile**. Scripts can add reading controls of their own, which follow the list.

**Actions**, **Addresses and numbers**, **Clipboard history**, **Live recognition**, **AI actions** and custom reading controls need Android 11 or later.

Some controls have options, which follow the control in **Current** and are hidden while it isn't in the list:

- **Addresses and numbers to include**, after **Addresses and numbers**: which kinds of address and number it lists.
- **Links and Controls only in web content**, after **Links** and **Controls**: the **Links** and **Controls** reading controls are offered only on web pages. Off by default.
- **AI actions to include**, after **AI actions**: which AI commands the control offers.

#### Options {#commands-reading-control-options}

- **Change reading control automatically**: when focus moves to an item with a reading control of its own, such as **Actions** for an item with actions, SandFly selects that control, and returns to the one before when focus leaves the item. On by default. With it off, the reading control changes only when you change it.
- **Handwriting letters**: the letter of each reading control. See [](#commands-handwriting-letters).
- **Custom reading controls**: see [](#commands-custom-reading-controls).
- **Reading control lists**: see [](#commands-reading-control-lists).

#### Handwriting letters {#commands-handwriting-letters}

The **Write the letter of a reading control** command opens a surface on the whole screen. Write a letter on it with one finger, and SandFly selects the next reading control that has that letter, or else the next one whose name starts with it. A tap closes the surface. Handwriting recognition uses a model that SandFly downloads the first time, after you confirm.

**Reading controls** > **Handwriting letters** lists the reading controls with their letters. Choose a control to give it a letter from A to Z, or **None**. Several controls can share a letter; writing it again moves to the next one. Default letters include H for **Headings**, K for **Links**, L for **Lines**, A for **Landmarks**, W for **Words**, C for **Characters**, P for **Paragraphs**, O for **Controls**, B for **Buttons**, E for **Edit fields**, T for **Tables** and S for **Speech rate**.

#### Custom reading controls {#commands-custom-reading-controls}

A custom reading control is a reading control made of commands. With it selected, swipe up or down to choose a command, and double-tap to run it. Needs Android 11 or later.

1. Go to **Reading controls** > **Custom reading controls**.
2. Choose **Add reading control** and enter a name.
3. Choose the new control, then **Add command**, and double-tap the commands to add.

The control screen lists its commands under **Current**, with the same actions as the SandFly menu, and has **Gestures and keyboard shortcut**, **Rename** and **Delete**. A custom reading control can't hold submenus. A new custom reading control is added at the end of the reading controls list; move or remove it there like any other. A control without commands isn't in the list.

#### Reading control lists {#commands-reading-control-lists}

A reading control list is a named list of reading controls, apart from the main list, with its own order. For example, a list with only the speech controls.

1. Go to **Reading controls** > **Reading control lists**.
2. Choose **Add list** and enter a name, or keep the suggested one, such as "List 1".
3. Choose the list. Under **Not in**, double-tap the reading controls to add. Under **Current**, choose a control to move or remove it.

The list screen also has **Rename** and **Delete list**.

Each list adds two commands to the **Reading control lists** category: **Next item in** and **Previous item in** the list, which select the next or previous reading control of that list. The **Next reading control list** and **Previous reading control list** commands, in the **Reading controls** category, choose the list that next and previous reading control move through, the main list or one of yours. **First reading control** selects the first reading control of that list.

#### AI actions {#commands-ai-actions}

The **AI actions** reading control holds the AI commands: **Describe screen**, **Speak screen description**, **Ask about screen**, **Describe image**, **Speak image description**, **Ask about image**, **Share image of focused item** and **Share screenshot**, and with SandFly Pro, **Start or stop AI agent task** and **Type AI agent task**. Swipe up or down to choose a command, then double-tap to run it. The control offers only the commands that the device and the AI services settings support, and isn't shown when there are none.

**AI actions to include**, after **AI actions** in **Current**, chooses which commands the control offers. All are included until you change it. For the commands, see [](#descriptions) and [](#pro).
