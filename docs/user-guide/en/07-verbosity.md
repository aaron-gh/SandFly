## Verbosity {#verbosity}

The **Verbosity** screen in SandFly settings sets what SandFly says about items, text and symbols, tables, and changes on the screen, and what it leaves out for apps that speak by themselves. It has the groups **Items**, **Text and symbols**, **Tables**, **Announcements** and **Apps with their own speech**.

How text is pronounced, such as phonetic letters and the speech dictionary, is on the **Speech** screen. See [](#speech). Keyboard echo is on the **Typing** screen. See [](#typing).

The **Verbosity** command opens this screen. You can also add a **Verbosity** item to the SandFly menu. See [](#commands).

### Items {#verbosity-items}

The **Items** group sets what SandFly says about each item:

- **Speak usage hints**: after an item is described, says how to use it, such as "Double-tap to activate". On by default.
- **Speak item type**: says the type of an item, such as "Button" or "Checkbox". On by default.
- **Speak item type without a pause**: joins the name and the type of an item with a space instead of a pause. Off by default.
- **Speak Actions available**: says "Actions available" after an item with actions of its app, such as Reply on a chat message. Works with **Speak usage hints** on or off; with usage hints on, the hint then gives only the gesture that shows the actions, such as "Use tap with 2 fingers to view actions". Off by default. For the items it applies to and the **Actions available** sound, see [](#sound-actions-available).
- **Speak list information**: says list positions and list details. On by default.
- **Speak grid and table information**: says rows and columns in grids, tables, and pagers laid out in rows and columns, such as a folder on a home screen. On by default. The **Tables** settings apply only while it's on. See [](#verbosity-tables).
- **Order of item details**: the order of the name, type and state of an item. See [](#verbosity-item-order).
- **Speak element IDs**: says the ID of buttons that have no label. Off by default.

#### Change the order of item details {#verbosity-item-order}

**Verbosity** > **Order of item details** lists **State**, **Name** and **Type** in the order SandFly says them. The default is **State, name, type**.

To move a detail, do one of the following:

- Touch and hold the detail, then drag it up or down. With SandFly on, double-tap and hold the detail, then drag it.
- Use the **Move up** and **Move down** actions of the detail.

SandFly announces the position of the detail as you pick it up, move it, and drop it.

The order also applies to rows whose check box or switch has no text of its own, which SandFly reads as one control with the text of the row as its name, and to buttons and tabs whose label is a separate item inside them.

### Text and symbols {#verbosity-text}

The **Text and symbols** group sets how text, punctuation, capital letters and emoji are spoken:

- **Speak punctuation and symbols**: **All** says every punctuation mark and symbol. **Most** says most symbols, such as & and @, but leaves common punctuation, such as commas and apostrophes, to the speech engine. **Some**, the default, leaves punctuation and symbols to the speech engine. **None** leaves punctuation and symbols out of speech, except a character read on its own, such as when moving by character. Apostrophes and hyphens inside words, separators inside numbers, and punctuation that ends a word are left to the speech engine. **Learn more**, in the list, opens a web page about punctuation levels.
- **Say symbols that change meaning**, shown while **Speak punctuation and symbols** is **None**: leaves these to the speech engine: a plus or minus sign directly before a number, degree, percent, per mille and currency signs, and + − × ÷ = < > between numbers. Off by default.
- **Capital letters**: what SandFly does for a capital letter: **Do nothing**, **Say “capital”**, the default, **Change pitch**, or **Play sound**. With any other choice than **Say “capital”**, SandFly sends a capital letter on its own to the speech engine in lower case.
- **Emoji**, **Count repeated emoji** and **After emoji names**: see [](#verbosity-emoji).
- **Count repeated symbols**: says how many times a symbol repeats when it repeats 4 or more times. On by default.
- **Speak text formatting** and **Text formatting options**: see [](#verbosity-text-formatting).

The **Speak punctuation and symbols** reading control and the **Cycle punctuation level** keyboard shortcut change the punctuation level. To replace symbols with your own words, use **Speech** > **Symbol dictionary**. See [](#speech-symbol-dictionary).

#### Emoji {#verbosity-emoji}

Some speech engines read emoji badly, use names of their own, or don't read them. **Verbosity** > **Emoji** chooses how SandFly speaks them:

- **Read by speech engine**: the default. The speech engine gets the emoji as they are.
- **Read by SandFly**: replaces each emoji with its name from the Unicode CLDR, in the language the text is spoken in. A skin tone, a family, a flag or a keycap is one emoji with one name. Emoji that SandFly has no name for are left to the speech engine, and emoji without a name in the language are spoken in English.
- **None**: leaves emoji out, except in text that is only emoji, such as a reaction button, which is still named.

Two more settings depend on **Emoji**:

- **Count repeated emoji**: with **Read by SandFly**, says an emoji repeated in a row once with a count, such as "3 grinning face". Choose **Off**, the default, or from **2 or more** to **6 or more**.
- **After emoji names**: what follows each emoji. Not available with **None**.
  - **Nothing**: the default.
  - **Say "emoji"**: says "emoji" after each emoji, such as "grinning face emoji".
  - **Play a sound**: plays the **Emoji** sound after each emoji, or once after emoji next to each other. Emoji are then spoken as separate utterances from the text around them. A sound theme can replace the sound, and **Individual sounds and vibrations** can turn it off. See [](#sound-events).

With **Read by SandFly**, moving by character names a whole emoji once, and moving by word stops on emoji. With **None**, moving by word skips emoji. Copying or spelling the last spoken phrase gives the emoji, not their names.

The **Emoji** reading control and the **Emoji** command change this setting. The voice and rate of emoji are set in **Speech**. See [](#speech-emoji).

#### Text formatting {#verbosity-text-formatting}

**Verbosity** > **Speak text formatting** announces changes in text formatting, such as bold and italic, while SandFly reads text. It's off by default.

**Verbosity** > **Text formatting options** is available while **Speak text formatting** is on. It has:

- **Announcement style**: **Speech and sound**, the default, **Speech only**, or **Sound only**. With speech, SandFly says announcements such as "start bold, headline, end bold".
- **Text formatting announcements**: the formatting to announce. **Bold**, **Italic**, **Underline**, **Strikethrough**, **Text color name**, **Text size** and **Font family** are on by default. **Text color hex value** is off by default.

The **Speak text formatting** reading control and command turn the setting on or off. The **Describe text formatting** command, in the **Reading** group of commands, describes the formatting of the focused text.

### Tables {#verbosity-tables}

The **Tables** group applies only while **Speak grid and table information** is on:

- **Speak row and column numbers**: says row and column numbers for table cells. On by default. Turn it off to hear only named headers.
- **Read column headers**: **After cell data**, the default, says the contents of a cell, then its row and column headers or numbers. **Before cell data** says the headers or numbers first, with "Column heading" or "Row heading" for a header cell. It applies to grids as well as tables, so an item in a photo grid says its row and column first. **Do not read** leaves column headers out, and says column numbers only while **Speak row and column numbers** is on.

### Announcements {#verbosity-announcements}

The **Announcements** group sets what SandFly says when something changes:

- **Window names**: when SandFly says the name of a window. **Always** says it when a window opens and when focus moves into it from another window, before the first item. **Always, cut off by the first item**, the default, does the same, but the first item focused in a new window stops the name. **Only when a window opens** doesn't say it when focus moves into a window. **Never** says no window names.
- **Always speak number of list items**: says which items are showing after every scroll. Off by default. With it off, SandFly says no list position when a list scrolls.
- **Announce full charge**: says "Battery fully charged" and plays the **Full charge** sound once when the battery is full while the charger is connected. Off by default.
- **Battery level**: says the battery level, charging or not, each time it reaches a multiple of 5% or 10%, or every 15 minutes, 30 minutes or hour. Nothing is said while the screen is off: a level reached or an interval ended while it was off is said when the screen turns on. Off by default.
- **Battery level voice**: the voice for **Battery level**: **Notification voice**, **SandFly's voice**, the default, or a voice profile. See [](#notifications-voice).
- **Announce caller**: how often SandFly says the caller when a phone call or an app's call, such as a WhatsApp call, starts ringing. **Once**, the default, says it when the call notification arrives. **Repeat while ringing** says it again every 5 seconds, after any speech in progress, until the call is answered, declined or ends. For the calls of apps, it needs notification access for SandFly. See [](#notifications-access). **Off** leaves the call notification unread, and the **Say caller** command says the caller on request. See [](#commands-calls).
- **Announce call length**: when a phone call ends, says how long it lasted, from when the call was answered or dialed. Calls that weren't answered aren't announced. Off by default.
- **Announce keyboard shown or hidden**: on by default.
- **Screen on and off**: what SandFly says when the screen turns off or on and when the phone is unlocked. See [](#verbosity-screen-on-off).
- **Time format**: **Use device time format**, the default, **12 hour** or **24 hour**, for the time that SandFly says.
- **Include seconds in the time**: off by default.
- **Include seconds when the screen turns on**: with **Include seconds in the time** on, includes the seconds in the time said when the screen turns on. On by default.
- **Status readout**: the items of the **Speak status** command. See [](#verbosity-status-readout).

#### Screen on and off {#verbosity-screen-on-off}

**Verbosity** > **Screen on and off** has three groups: **When the screen turns off**, **When the screen turns on**, and **When the phone is unlocked**. On a flip phone, waking the cover screen counts as the screen turning on, and opening the phone right after doesn't say it again. Each group has the same items, except the first ones:

- **Say "Screen off"**: in **When the screen turns off** only. On by default.
- **Ringer mode**: in **When the screen turns off** only. Says when the ringer is set to vibrate or silent. On by default.
- **Say "Device unlocked"**: in **When the phone is unlocked** only. On by default.
- **Time**: on by default when the screen turns on, off for the other two.
- **Number of notifications**: on by default when the screen turns on, off for the other two.
- **Battery**: **Off**, the default, **Always**, or **Only while charging**.
- **Low battery**: says the battery level when it's below a level: **Off**, the default, or **Below 10%**, **Below 15%**, **Below 20%**, **Below 30%** or **Below 50%**.
- **Hidden screen**: only when the screen is hidden. Off by default. See [](#display).
- **Wi-Fi**: network name and signal strength. Off by default.
- **Mobile network**: carrier, network type, and signal strength. Off by default.
- **Ringer and Do Not Disturb**: only when the ringer is not set to ring, or Do Not Disturb is on. Off by default.
- **Airplane mode**: only when airplane mode is on. Off by default.

**Speak during media playback**, above the groups, speaks the screen-off and unlock announcements while media plays. Off by default. The screen-on announcement is always spoken during media playback.

#### Status readout {#verbosity-status-readout}

**Verbosity** > **Status readout** lists the items that the **Speak status** command says, in order: **Time**, **Battery**, **Wi-Fi**, **Mobile network**, **Ringer and Do Not Disturb**, **Airplane mode** and **Hidden screen**. All are on by default. Items in their usual state, such as the ringer when it rings, are left out.

- To turn an item off or on, choose it.
- To change the order, use the **Move up** and **Move down** actions of an item.

**Status readout** isn't shown on TVs.

### Apps with their own speech {#verbosity-own-speech}

Some apps and keyboards speak by themselves, such as an assistant that speaks its answer while it moves accessibility focus to each new piece of text, or a keyboard that speaks its keys. The **Apps with their own speech** group leaves SandFly speech out for them, so that the same text isn't spoken twice. Only speech is left out: focus still moves, and sounds and vibrations still play. SandFly screens are always spoken.

- **Speak only focus moved by the user**: **Off**, the default, **In all apps**, or **In chosen apps**. While it applies, SandFly speaks focus that you move by touch, gestures, keys, a braille display or commands. It doesn't speak focus that the app moves, announcements, live region changes, window names or window change announcements. Focus that SandFly moves after a window changes, after a scroll, or to follow input focus counts as moved by the app.
- **Chosen apps**: the apps for **In chosen apps**. Available only while **Speak only focus moved by the user** is **In chosen apps**. Choose **Add app** to add one, and choose an app to remove it.
- **Speak only focus moved by the user while an assistant or call plays audio**: does the same in all apps while another app plays audio as an assistant or a call, and while an assistant window, such as Gemini over an app or the lock screen, is shown. The screen-on and unlock readouts are left out then too, and speech stops when an assistant window opens. Focus that you move is still spoken. Off by default.
- **Keyboards with their own speech**: while a keyboard in this list is shown, SandFly doesn't speak its keys, the text typed or deleted, or the cursor moving in the edit field. Gestures and commands still work, and moving through text with reading controls is still spoken. Choose **Add keyboard** to add one.

SandFly never speaks the announcements, live region changes, window changes, toasts or notifications of com.oplus.viewtalk, an accessibility service on realme and OPPO phones that sends image descriptions such as "Image may contain…".

The **Speak only focus moved by the user** reading control changes the first setting. Its command turns the setting off, or back on with **In chosen apps** when there are chosen apps and **In all apps** otherwise. The reading control isn't in the reading controls by default. See [](#commands).
