## Explore and read the screen {#reading}

SandFly reads the item that has SandFly focus. You move focus by touching the screen, by swiping, with a keyboard or a braille display, and SandFly reads each item it moves to. This chapter describes how to move through the screen, read text in pieces, read continuously and scroll, and lists the settings on the **Navigation** screen.

The gestures in this chapter are those of the default gesture layout, **SandFly gestures**. With **Gestures** > **Gesture layout** set to **VoiceOver gestures**, some gestures differ. For every default gesture, see [](#appendix-gestures). To change gestures, see [](#gestures).

### SandFly focus {#reading-focus}

SandFly focus is the item that SandFly reads and acts on. A rectangle on the screen shows where it is; to change how it looks, see [](#display).

SandFly focus is separate from input focus, the item that receives typing from a keyboard. Text fields, and some web content, can have input focus while SandFly focus is elsewhere.

An item can be a button, a line of text, a list row, a picture or anything else an app shows. Apps decide how their screens are divided into items. When an item contains other text, SandFly reads it all as one item, unless **Navigation** > **Focus text inside items separately** is on. See [](#reading-navigation-settings).

### Explore by touch {#reading-explore}

Touch the screen and move your finger. SandFly focuses and reads the item under your finger. When your finger moves over an empty part of the screen, SandFly plays a sound.

SandFly waits a short time after you touch the screen, to tell a gesture from a touch, before it focuses the item under your finger. To change this wait, go to **Touch** > **Focus delay**. The default is 0.15 seconds. Touch settings are described in [](#touch).

- Touching between the icons of the home screen doesn't read the page, such as "Home screen 1 of 2". Swiping still reaches it.
- A picture-in-picture window, such as a video that plays over the home screen, is one item, named after the app and "picture in picture". The controls in the window are actions of that item. Touch still reaches each control.

### Move between items {#reading-move}

- To move to the next item, swipe right.
- To move to the previous item, swipe left.

Items are read in the order the app gives them, which is usually from the top left to the bottom right. When the next item is off screen in a list, SandFly scrolls the list. See [](#reading-scroll).

Swipes don't move into the status bar or the navigation bar. You reach them by touch, or with the **Windows** reading control. Swipes from the app don't move into the on-screen keyboard; see [](#typing).

At the end of the screen, a swipe past the last item stops there and plays a sound. The next swipe goes to the first item. To change this, see [](#reading-wrap).

These commands move to the ends of the screen. They have no gesture by default:

- **Focus on first item on screen**
- **Focus on last item on screen**

To assign a gesture or keyboard shortcut to a command, see [](#commands).

#### Wrap around {#reading-wrap}

With **Navigation** > **Wrap around** on, which is the default, a swipe past the last item goes to the first item, and a swipe past the first item goes to the last.

- **Navigation** > **Stop at the last item before wrapping**: The first swipe past the last or first item stops there with the end sound, and only the next swipe wraps. On by default. It has no effect while **Wrap around** is off.
- With **Wrap around** off, each swipe or key press past the first or last item reads the item again with the end sound. Moving by heading, link or control in apps stops at the ends too. Web pages can still wrap by themselves.

**Wrap around** is also a reading control, which you can add on the **Reading controls** screen to turn wrapping on or off with a swipe up or down. See [](#reading-controls).

**Navigation** > **Wrap around in reading controls** sets whether reading controls that go through a list of values, such as **Speech engine**, **Audio output device**, **Actions**, **Addresses and numbers** or **Sound mode**, go around from the last value to the first. When it is off, swiping past the first or last value reads the value again with the end sound. Until you change it, it follows **Wrap around**, so that items and values can wrap differently once you set it.

### Activate an item {#reading-activate}

- To activate the focused item, double-tap anywhere on the screen. This is the **Activate** command.
- To touch and hold the focused item, as a long press does, double-tap and hold. This is the **Touch and hold** command.

When the **Actions** reading control is selected, a double-tap performs the action chosen in it. See [](#reading-controls).

**Activate with a tap**, a command with no gesture by default, taps the middle of the focused item on the screen, for items that respond only to a finger on the screen. It needs Android 11 or later; otherwise the item is activated as usual.

To activate items by lifting your finger or with a single tap, see [](#touch).

### Reading controls {#reading-controls}

A reading control sets what a swipe up or swipe down does. Most reading controls move by a kind of item or piece of text, such as **Headings** or **Words**. Others change a setting, such as **Speech rate**.

- To select the next reading control, swipe down then up, swipe right or down with three fingers, or rotate clockwise with two fingers.
- To select the previous reading control, swipe up then down, swipe left or up with three fingers, or rotate counterclockwise with two fingers.
- To select the first reading control of the list in use, assign a gesture or keyboard shortcut to **First reading control**. It has none by default.
- To move by the selected reading control, swipe down to go forward and swipe up to go back. These are the **Move reading control down or forward** and **Move reading control up or backward** commands.
- On reading controls that change a value, such as **Adjust slider**, **Adjust value** and **Speech rate**, the direction of the swipe sets the change, whichever of the two commands the swipe runs: a swipe up or right increases the value, a swipe down or left decreases it. With a right-to-left language, a swipe left increases the value and a swipe right decreases it.

SandFly reads the name of each reading control as you select it. Swiping right and left moves by item, whichever reading control is selected, except with **Rows and columns** in a table.

Reading controls that go only with some items, such as **Actions** on an item with actions or **Rows and columns** in a table, appear in the list only where they apply. **Reading controls** > **Change reading control automatically**, on by default, selects such a reading control when focus moves onto an item that uses it, such as **Actions**, **Adjust slider**, **Adjust value** or **Proofread**, and selects the earlier reading control again when focus moves away. Turn it off to keep the reading control you selected. In tables, **Rows and columns** is selected this way only with **Navigation** > **Switch to table navigation in tables** on.

The **Reading controls** screen sets which reading controls are in the list and in what order. It also has reading control lists, custom reading controls and the letters for **Write the letter of a reading control**, a command that selects a reading control by the letter you write on the screen, such as H for **Headings**. These are described in [](#commands).

By default, the list has these navigation reading controls: **Characters**, **Words**, **Lines**, **Paragraphs**, **Headings**, **Links**, **Controls**, **Screen search** and, in tables, **Rows and columns**. You can add the others on the **Reading controls** screen.

#### Read text in pieces {#reading-text}

These reading controls move through the text of the focused item, and then on to the next or previous item:

- **Characters**: one character at a time.
- **Words**: one word at a time.
- **Lines**: one line at a time.
- **Sentences**: one sentence at a time. A sentence ends at sentence punctuation, including Chinese 。！？, and at the end of a line. On web pages, a sentence that continues into another item, such as a link inside it, is read in parts. Not in the list by default, and not available in selection mode.
- **Paragraphs**: one paragraph at a time.

In a text field that is being edited, these reading controls move the cursor, and swipes stay in the field. For editing text, see [](#typing).

SandFly also moves through text that an app draws itself and doesn't let screen readers move through, such as messages in Telegram.

#### Move by kind of item {#reading-kinds}

These reading controls move to the next or previous item of one kind. Swipe down for the next one and swipe up for the previous one.

- **Default navigation**: one item at a time, as swiping right and left does. Not in the list by default.
- **Headings**, **Links** and **Controls**: headings, links, and items that you can use, such as buttons and text fields. These work in apps and on web pages.
- **Windows**: other windows on the screen, such as the volume controls or the navigation bar. Not in the list by default.
- **Containers**: out of the current container to the next or previous one. Containers are lists, grids, scroll views, toolbars, tab bars, navigation bars, search bars and panes. A bar inside a bar, such as a search bar in a toolbar, counts as one. Going back moves to the start of the previous container, except on web pages. Not in the list by default.
- **Vertical navigation**: the item below or above. See [](#reading-vertical).
- **Screen search**: the results of the last screen search. See [](#reading-search).
- **Scroll**: scrolls the list forward when you swipe down and back when you swipe up. When a list doesn't respond to scrolling, SandFly sends a swipe to the list instead. Not in the list by default.

The reading controls under **Element navigation** and **Heading levels** on the **Reading controls** screen, such as **Landmarks**, **Buttons**, **Edit fields**, **Lists** and **Tables**, move by the elements of web pages, and also work in apps. See [](#web).

**Reading controls** > **Links and Controls only in web content**, off by default, leaves **Links** and **Controls** out of the list outside web content. Gestures and keyboard shortcuts for the next or previous link or control still work everywhere.

#### Move by text or element without a reading control {#reading-navigation-commands}

You can assign a gesture or keyboard shortcut to move to the next or previous character, word, line, paragraph, heading, link, control, landmark, button, checkbox, radio button, edit field, combo box, focusable item, graphic, list, list item, table, visited link, unvisited link, or heading of a given level. These moves don't change the selected reading control. In the list of commands for a gesture, they are under **Navigate by text** and **Navigate by element**. See [](#gestures).

### Read continuously {#reading-continuous}

SandFly can read every item, one after another, and scroll lists on the way:

- **Read from top**: reads from the first item on the screen.
- **Read from focused item**: reads from the focused item. It is in the SandFly menu by default.
- **Read from focused item by reading control**: reads from the focused item, and then only the items of the selected reading control, such as each heading or each link. A reading control that doesn't move between items of one kind reads every item.

These commands are in the **Reading** category of commands and have no gesture by default. You can run them from the SandFly menu, which a tap with three fingers opens, or assign them to a gesture or a keyboard shortcut. See [](#commands).

While SandFly reads continuously:

- Swipe right or left to skip to the next item or go back to the previous one. Reading goes on from there.
- Press the volume keys to change the speech volume.
- To stop reading, touch the screen or use another gesture.

The first time you start continuous reading, SandFly shows **How continuous reading works**. To show it each time, select **Always show this message**.

SandFly keeps the screen on while it reads.

### Scroll {#reading-scroll}

Swiping right and left scrolls lists by itself when the next item is off screen. You can also scroll yourself:

- To scroll forward, swipe right then left. This is **Scroll forward**.
- To scroll back, swipe left then right. This is **Scroll back**. At the top of a list, **Scroll back** pulls the list down, which refreshes it in most apps.
- To scroll the app directly, drag two fingers on the screen, as you would drag one finger without SandFly.

In the default gesture layout, two-finger swipes go to the app and scroll it. With **Gestures** > **Handle two-finger swipes in SandFly** on, as in the VoiceOver gesture layout, two-finger swipes run commands instead: by default **Scroll up**, **Scroll down**, **Scroll left** and **Scroll right**, which move the content in the direction of the swipe. Where no item on the screen scrolls sideways, **Scroll left** and **Scroll right** send a one-finger swipe to the pager around the focused item, such as the folder tabs in Solid Explorer. These four commands need Android 11 or later, and aren't on watches.

These commands, in the **Scrolling** category, have no gesture by default:

- **Scroll to top** and **Scroll to bottom** scroll the list that has focus to its start or end, a page at a time, and then focus its first or last item. A touch, gesture or key stops them. Not on watches.
- **Refresh** scrolls the list to its top and pulls it down. It needs Android 11 or later.

While you scroll with two fingers, SandFly plays the scroll sound and a light vibration for each item that scrolls past. The pitch of the scroll sound rises from the top of the list to the bottom. Scrolls that SandFly makes to reach the next item make no sound. For sound and vibration settings, see [](#sound).

#### Scrolling during swipes {#reading-scroll-swipes}

These settings on the **Navigation** screen change how swipes scroll lists:

- **Scroll ahead to the next item**: When the focused item is the last item of a list on the screen, SandFly scrolls the list forward while it reads the item, until the next item is on the screen, so that the next swipe doesn't wait for a scroll. Focus stays on the item, and the scroll makes no sound. After a move back, SandFly scrolls back instead. It doesn't scroll ahead in text fields, in web content or during continuous reading, and stops when you touch the screen, press a key or a window changes. On by default.
- **Speak the next item while scrolling**: When a swipe has to scroll a list to reach the next item, SandFly focuses and reads the item as soon as it comes on the screen, while the list is still moving. Turn it off to wait until the list slows down. On by default.
- **Speak items before they scroll into view**: When a swipe moves to an item that is only partly on the screen, SandFly reads it at once, including the parts still past the edge of the list, instead of waiting for the list to scroll. Off by default, because some apps keep hidden text outside an item instead of hiding it, and that text is then read.

### Lists and tables {#reading-lists-tables}

When focus enters or leaves a list, SandFly reads list information, such as the position of the item. **Verbosity** > **Speak list information**, **Speak grid and table information** and **Always speak number of list items** set what is read. See [](#verbosity).

The **List items** reading control and the list item gestures and keyboard shortcuts move to the next or previous item of the list that has focus. They pass over the buttons and other parts inside each item, such as the Share button of a post, and scroll the list when the next item is off screen. After the last item, they go on to the next list. Outside a list, they move to the next list item on the screen.

#### Navigate tables {#reading-tables}

In a table, the **Rows and columns** reading control moves by cell:

- Swipe down or up to move to the next or previous row.
- Swipe right or left to move to the next or previous column.

While **Rows and columns** is selected, swipe right and swipe left always move by column, whatever a gesture profile, an app gesture or a script assigns to them. When focus leaves the table, SandFly selects the earlier reading control again.

The **Row** and **Column** reading controls move only by row or only by column with swipes down and up. They aren't in the list by default. **Next row**, **Previous row**, **Next column** and **Previous column** are also commands, in the **Tables** category, that you can assign to gestures.

**Navigation** > **Switch to table navigation in tables**, off by default, selects **Rows and columns** when focus enters a table.

The table reading controls appear in the list only in tables. In Google Sheets, SandFly treats the grid of cells as a table, so the table reading controls and commands move between cells, and the actions of a cell include **Edit cell** and **Insert formula**.

Whether SandFly reads row and column numbers and column headers with each cell is set by **Verbosity** > **Speak row and column numbers** and **Read column headers**. See [](#verbosity). Keyboard commands that read the row and column headers are described in [](#keyboards).

### Windows, containers and items below {#reading-windows}

- To move to the next window, swipe down with four fingers. To move to the previous window, swipe up with four fingers. These are **Next window** and **Previous window**.
- To move to the next container, swipe right with four fingers. To move to the previous container, swipe left with four fingers. These are **Next container** and **Previous container**, which stop at the same containers as the **Containers** reading control.

**Navigation** > **Move from the status bar and navigation bar to other windows**, off by default: When focus is in the status bar or the navigation bar, such as after you touch it, swiping past the first or last item of the bar moves into the app. When off, swipes go around the bar.

#### Vertical navigation {#reading-vertical}

The **Vertical navigation** reading control moves to the item below when you swipe down and the item above when you swipe up. It passes over the other items of the same row, such as the buttons beside the title of a video or the name of a file, and stays in the column of the focused item: from a key on a keypad, it moves to the key below or above. In a list, it stays in the list, and scrolls the list a page when there is no row below or above on the screen. At the end of the list, or outside a list, it moves to the item below or above on the screen, and plays the end sound when there is none.

The **Move to item below** and **Move to item above** commands make the same moves from a gesture or keyboard shortcut, without the reading control. **Move to item on the left** and **Move to item on the right** move to the nearest item on the screen in that direction. None of them has a gesture by default.

### Focus when the screen changes {#reading-screen-changes}

When a window opens, or you go back to a screen, SandFly moves focus into it: to the item you were on, to the text field with the cursor, or to the first item. This also applies to SandFly menus and dialogs.

After you activate an item in a list, such as a folder in a file manager or a page in Settings, and go back, focus returns to that item when the list shows it again within two seconds. This also works in apps that change the list without opening a new window.

To leave focus where it is, turn off **Navigation** > **Move focus to new windows**. SandFly then reads only the name of the window, and the next swipe moves to the first item. Focus follows input focus only when you move input focus with a key, such as an arrow key or Tab. On TVs, focus always moves to new windows.

When the focused item disappears, such as an ad that collapses or a list row that is removed, SandFly focuses the item now at its place and reading goes on from there.

### Search the screen {#reading-search}

Screen search finds items on the screen by their text.

1. Swipe left then down. This is the **Screen search** command, in the **SandFly** category.
2. Type a word in the search field. SandFly lists the matching items and reads the number of matches.
3. Double-tap a result to move focus to it.

The search window also has **Next screen** and **Previous screen**, which scroll the screen behind it to search more of a list, **Clear input** and **Close search**.

After a search, the **Screen search** reading control moves between the results: swipe down for the next result and swipe up for the previous one. Screen search isn't on watches.

### Addresses and numbers {#reading-addresses}

The **Addresses and numbers** reading control lists the web addresses, email addresses, phone numbers and codes of 4 to 8 digits in the text of the focused item, also when the app doesn't make them links, such as in a chat message or in a notification with a one-time code.

1. On the **Reading controls** screen, add **Addresses and numbers**. It is off by default.
2. Focus the item, and select **Addresses and numbers**.
3. Swipe up or down until SandFly reads the address or number you want.
4. Double-tap. A web address opens, an email address opens a new email, a phone number opens in the phone app (**Call** and the number), and a code is copied (**Copy** and the code).

**Addresses and numbers** is also an item of the SandFly menu and a command, in the **Activation** category. **Reading controls** > **Addresses and numbers to include** turns each kind on or off: **Web addresses**, **Email addresses**, **Phone numbers** and **Codes**.

Phone numbers are found by the text classifier of Android, which also finds dates, times, street addresses and flight numbers. Digits in any of these, and digits joined to a word, a user name, a time or an amount, aren't listed as codes. Other numbers of 4 to 8 digits, such as a year in a headline, are listed as codes. The text is read when you swipe the reading control or open the SandFly menu, which can take up to a second. On Android 8, and when the text classifier doesn't answer within a second, only web and email addresses are listed.

### Copy text from screen {#reading-copy-text}

**Copy text from screen**, a command in the **Text editing** category with no gesture by default, opens a window with the text on the screen as a list of check boxes.

The window lists the text on the screen. When the screen has a list or page that scrolls, **Scroll up** above the list and **Scroll down** below it scroll that list a page past the text listed so far and add that page's text. SandFly announces "Added" and the number of pieces added. Checked pieces stay checked. When the list is at its start or end, SandFly says "No more text". The list scrolls back to where it was when the window closes.

1. Run **Copy text from screen**.
2. Under **Granularity**, choose how the text is divided: **Addresses and numbers**, **Characters**, **Words** or **Paragraphs**. **Words** is selected first.
3. Check the pieces you want. To check a range, double-tap and hold the first piece and then the last, or use the **Start selection** and **End selection** actions. **Select all** checks every piece.
4. Select **Copy**. The checked pieces are copied in order: words with a space between them, characters with nothing between them, paragraphs with a blank line between them, and addresses and numbers one per line.

**Addresses and numbers** lists every web address, email address, phone number and code on the screen as written, so that you can copy a phone number instead of calling it. It appears only when the screen has any, and lists every kind, whatever **Addresses and numbers to include** is set to. Finding them can make the window take up to a second longer to open.

The window holds at most 1,000 pieces; when the text is longer, it reads "List cut off at" and the number of pieces. For other ways to copy text, see [](#typing).

### Navigation settings {#reading-navigation-settings}

The **Navigation** screen in SandFly settings has these settings:

- **Wrap around**: Swiping past the last item goes to the first, and past the first goes to the last. On by default. See [](#reading-wrap).
- **Stop at the last item before wrapping**: The first swipe past the end stops there; the next one wraps. On by default.
- **Wrap around in reading controls**: Values of reading controls go around from the last to the first. Follows **Wrap around** until you change it.
- **Move from the status bar and navigation bar to other windows**: Swipes past the end of a system bar move into the app. Off by default. See [](#reading-windows).
- **Move focus to new windows**: Focus moves into windows that open. On by default. See [](#reading-screen-changes).
- **Speak items before they scroll into view**: Items partly off screen are read before the list scrolls. Off by default. See [](#reading-scroll-swipes).
- **Scroll ahead to the next item**: Lists scroll ahead while the last item on the screen is read. On by default.
- **Speak the next item while scrolling**: The next item is read as soon as it comes on the screen. On by default.
- **Focus text inside items separately**: Each piece of text and each state inside an item gets focus of its own, so that you can reach it by swiping and by touch. The item itself keeps focus only when it has text, a state or actions of its own; activating a piece of text inside it activates the item. Off by default. The command **Focus text inside items separately**, in the **Navigation** category, turns it on or off and reads "Focus text inside items separately on" or "Focus text inside items separately off".
- **Switch to table navigation in tables**: **Rows and columns** is selected when focus enters a table. Off by default. See [](#reading-tables).
- **Smart browse mode** and **Remember browse mode in each app**: See [](#web).
- **Read Telegram reply quotes with the message**, under **Apps**: See [](#reading-telegram).

#### Telegram {#reading-telegram}

In Telegram and apps built from it, such as Nekogram:

- With **Navigation** > **Read Telegram reply quotes with the message** on, which is the default, SandFly reads the quote of the message that a message replies to with the message, after the time, as "replying to" the sender and the text. The quote isn't focused on its own; **Go to quoted message**, in the actions of the message, shows the quoted message. Turn the setting off to have the quote as an item of its own; **Go to quoted message** isn't offered then.
- The Share button beside each message in a channel isn't focused when you swipe or touch the screen. It is in the actions of the message, under the name Telegram gives it, such as **Share**.
