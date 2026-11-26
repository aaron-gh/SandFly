## Web pages and elements {#web}

Web content is a page in a browser, such as Chrome, or a web view inside an app. SandFly reads web content with the same gestures as other screens, and adds ways to move by element: by heading, link, landmark, form control, list or table. These element moves also work in apps.

For moving by item, reading controls and continuous reading, see [](#reading). Keyboard shortcuts for web pages, including the single-key shortcuts of browse mode, are described in [](#keyboards).

### Read a web page {#web-read}

- Swipe right or left to move to the next or previous item of the page. A swipe past the last item of the page goes on to the items after the web content, such as the toolbar of the browser.
- Touch the screen to read the item under your finger.
- To read the whole page, use **Read from top** or **Read from focused item**. See [](#reading-continuous).

SandFly takes the next and previous items from the items of the page, in page order, and requests the next item from the page only where needed, so that items of a page that has just opened aren't skipped.

When you scroll a page yourself, with two fingers or with a scroll command, and the focused item scrolls out of view, SandFly moves focus to the first item in view, or to the last item in view when you scroll back. When a page changes or reloads and the focused item disappears, SandFly focuses the item now at its place.

How some elements are read:

- A landmark, such as navigation or main, is read by its type and name, not with all of its contents.
- Check boxes and radio buttons are read with their labels.
- When a page moves focus to an element such as a post, as Mastodon does with the J and K keys, SandFly reads the text of the links in it, such as the author, the time and the hashtags, but not its buttons. Spaces between links aren't read.

Note: Some speech engines, such as Gryphon, stop only between the pieces of text they receive. If speech of the next item starts late after you swipe away from a long paragraph, turn on **Speech** > **Send long text a sentence at a time**. See [](#speech-pronunciation).

### Move by element {#web-elements}

To move by one kind of element, select its reading control, then swipe down for the next one and swipe up for the previous one. When there is no further element of that kind, SandFly reads, for example, "No next heading".

These reading controls are on by default:

- **Headings**: headings of any level.
- **Links**: links.
- **Controls**: items that you can use, such as buttons, check boxes and text fields.

You can add these reading controls on the **Reading controls** screen, under **Element navigation** and **Heading levels**:

- **Landmarks**: landmarks, such as "main" or "navigation".
- **Buttons**
- **Checkboxes**
- **Radio buttons**
- **Edit fields**
- **Combo boxes**: combo boxes, such as drop-down menus.
- **Focusable items**: items that can take input focus.
- **Images and videos**
- **Lists**
- **List items**
- **Tables**
- **Visited links**: links to pages you have opened.
- **Unvisited links**: links to pages you haven't opened.
- **Heading 1** to **Heading 6**: headings of one level.

To add a reading control, see [](#commands). To select reading controls, see [](#reading-controls).

You can also move by element without changing the reading control: assign gestures to the commands in the **Navigate by element** category, such as **Next heading**, **Previous heading** and **Next heading 1**. See [](#gestures). On a physical keyboard, each element has a keyboard shortcut; see [](#keyboards).

The command **Navigation**, which you can add to the SandFly menu, lists kinds of navigation to choose from, such as characters, headings or landmarks. When it is in the SandFly menu, **Edit navigation options**, below it on the **SandFly menu** screen, chooses which kinds it lists. See [](#commands).

#### Elements in apps {#web-elements-in-apps}

Every element reading control, gesture and keyboard shortcut also works in apps, so that one set of commands moves through web pages and apps. In apps, the elements are:

| Element | In apps |
| --- | --- |
| Headings, and Heading 1 to Heading 6 | Items that the app marks as headings. Apps don't report heading levels, so each level moves through all headings. |
| Links | Links in text. |
| Controls | Buttons, check boxes, switches, text fields, sliders and other items that you can use. |
| Landmarks | Toolbars, tab bars, navigation bars and search bars, panes with a title, and containers with a title. |
| Buttons | Buttons, image buttons and floating action buttons. |
| Checkboxes | Check boxes, switches and toggle buttons. |
| Radio buttons | Radio buttons. |
| Edit fields | Text fields. |
| Combo boxes | Drop-down lists. |
| Focusable items | Items that can take input focus. |
| Images and videos | Images, including images that you can activate. |
| Lists | Lists and collections that aren't tables. |
| List items | Rows of the list that has focus. See [](#reading-lists-tables). |
| Tables | Tables and grids with rows and columns. |
| Visited links | None. Apps don't report visited links, so SandFly reads "No next visited link". |
| Unvisited links | All links. |

When the next element is off screen, SandFly scrolls the list it is in, up to 10 times.

**Reading controls** > **Links and Controls only in web content**, off by default, leaves the **Links** and **Controls** reading controls out of the list outside web content. The gestures and keyboard shortcuts still work everywhere.

When SandFly Pro screen recognition adds the elements of an app that doesn't report them, those elements are read, found by touch and navigated like the items of the app. See [](#pro).

### Tables on web pages {#web-tables}

On a web page, the **Tables** reading control moves from table to table. Inside a table, the **Rows and columns**, **Row** and **Column** reading controls move between cells, as in apps. See [](#reading-tables).

A physical keyboard has more table commands, in the **Tables** category, such as moving to the start or end of a row, a column or a table, and reading the row or column header. See [](#keyboards).

### Actions on web pages {#web-actions}

With **Reading controls** > **Change reading control automatically** on, SandFly selects the **Actions** reading control when focus moves onto an item that has actions. On web pages, SandFly selects it only on items that have actions of their own, not on text or on links inside text, so that a reading control you selected, such as **Headings**, stays selected while you read. On the next item without actions, SandFly selects the earlier reading control again. When **Actions** was selected automatically and has nothing for the focused item, swiping up or down uses the reading control it replaced.

### Browse mode {#web-browse-mode}

Browse mode changes what the keys of a physical keyboard and the commands of a braille display do. With browse mode on, single keys move by element, such as H for the next heading, instead of going to the page. With browse mode off, keys go to the page, for typing in text fields and for controls that use the arrow keys. SandFly reads "Browse mode on" or "Browse mode off" and plays a sound when it changes.

To turn browse mode on or off, use the command **Turn browse mode on or off**, in the **Web** category, which you can assign to a gesture. Each keymap also has a keyboard shortcut for it. See [](#keyboards).

In browse mode, the fast search key with a letter or digit moves to the next item that starts with that letter or digit. **Keyboard shortcuts** > **Fast search** > **Fast search key** chooses the key; it is off by default. See [](#keyboards-fast-search).

These settings are on the **Navigation** screen:

- **Smart browse mode**: turns browse mode on or off for the focused item. On by default. SandFly turns browse mode on when focus moves onto an item in web content, and off on text fields and on controls that use the arrow keys, such as sliders, list boxes, menu bars, menus, tree views, grids, combo boxes and spin buttons. When a keyboard command moves SandFly focus to one of these items, input focus moves there too. When you turn browse mode off yourself in web content, moving focus within the web content doesn't turn it back on. Browse mode is also turned off when input focus moves to a text field or slider in an app.
- **Remember browse mode in each app**: SandFly keeps browse mode as you last turned it on or off in each app. In an app where you haven't turned it on or off, browse mode is off, unless **Smart browse mode** turns it on in web content. Off by default.

### Lists of elements and addresses {#web-lists}

With a physical keyboard, these commands, in the **Web** category, list the elements of the page:

- **Show headings list**
- **Show landmarks list**
- **Show links list**
- **Show controls list**
- **Show tables list**

Each opens screen search with the list of one kind of element. Select an element to move focus to it. With a physical keyboard connected, screen search also has the buttons **All**, **Headings**, **Landmarks**, **Links**, **Controls** and **Tables**, which show one kind of element. See [](#reading-search).

**Speak URL** reads the address of the focused link, and **Speak page URL** reads the address of the page. Both are keyboard commands. See [](#keyboards).

To find the web addresses, email addresses and phone numbers written in the text of an item, use the **Addresses and numbers** reading control. See [](#reading-addresses).

### Settings for web content {#web-settings}

- **Navigation** > **Smart browse mode** and **Remember browse mode in each app**: See [](#web-browse-mode).
- **Reading controls** > **Links and Controls only in web content**: See [](#web-elements-in-apps).
- **Reading controls** > **Change reading control automatically**: See [](#web-actions).
- A settings profile can be used automatically while focus is in web content, for example to set a different speech rate for web pages. See [](#profiles).
