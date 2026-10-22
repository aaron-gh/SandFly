# Differences from TalkBack

SandFly is built from Backtalk, which is built from Google's TalkBack source releases. This document describes how SandFly differs from TalkBack: features that TalkBack doesn't have, defaults that SandFly changes, and problems in TalkBack that SandFly fixes. To learn how to install SandFly, see the [README](README.md).

Each section names the settings involved as they appear in SandFly. Unless a section says otherwise, each setting is in SandFly settings.

## Speed

### Faster speech after a swipe

When you swiped to an item, TalkBack waited for the app to confirm the new focus before it put together what to say. SandFly speaks the item as soon as it has set focus, and does less other work around each swipe. On a Galaxy Z Fold7, the median time from lifting your finger to hearing speech went from about 68 ms to about 39 ms.

This doesn't apply when focus moves to another window, when the screen changes, or during continuous reading. Keys on the keyboard, sliders, progress bars, pages in a pager, and web content still wait for the app.

### Next swipe prepared in advance

While you listen to an item, SandFly works out what a swipe forward or back would say. When your swipe lands on the item that SandFly prepared, speech starts immediately: on average 33 ms after the swipe, compared with 60 ms without preparation. In testing, about four in five swipes used prepared speech.

SandFly doesn't prepare anything for the first swipe on a screen or after the screen changes, and it discards prepared speech after 5 seconds.

### Items that a swipe scrolls into view

When a swipe moves to a list item that is only partly on screen, the list scrolls it into view. By default, SandFly then waits for the app to confirm the new focus, as TalkBack does, so these swipes don't get the faster speech above. Speaking straight away would come before the list had scrolled, when the item's text can still be off screen, and only its place in the list, such as "20 of 80", would be said.

When you turn on **Controls** > **Touch and activation** > **Reduce delay** > **Speak items before they scroll into view**, SandFly speaks these items straight away too, including the parts that are still past the edge of the list, since they will be on screen once it has scrolled. Parts the app has hidden are still left out. Apps that follow Android's conventions work correctly with this setting on. It is off by default to protect you from apps that don't: in those, a part that the app keeps out of sight by moving it outside the item, instead of hiding it, is read even though it isn't shown, and so is a part that stays off screen when the list can't scroll the whole item into view.

### Reading order kept between swipes

TalkBack asked the app for every item on the screen on each swipe, and waited for each answer. SandFly keeps the reading order between swipes, and builds it again only when the screen changes, or when SandFly scrolls or clicks. Changes to text or state, such as a clock that ticks or a progress bar that moves, don't discard the order.

To find the next item in a list, TalkBack also built the reading order of the whole screen three times for one swipe: once to check whether focus was at the end of the list, once to find the item, and once to check whether the item needed to be scrolled into view. SandFly builds it once and reuses it.

In a test on a Realme phone, three in four swipes reused the reading order, and the longest pause during swiping went from about 450 ms to about 150 ms.

### Time between one-finger taps

TalkBack always waits 0.25 seconds for another tap before it decides that a tap gesture is finished. In SandFly, you can set this wait for one-finger taps in **Controls** > **Touch and activation** > **Reduce delay** > **Time between one-finger taps**, down to 0.1 seconds. A shorter time makes one-finger multi-tap gestures respond faster, but a slow double tap can then count as two single taps.

Taps with two or more fingers always allow at least 0.25 seconds between taps, as lifting and placing several fingers takes longer.

This setting appears only when SandFly recognizes gestures itself, which requires Android 13 or later and **Handle gestures in SandFly** turned on. For more information, see [Gestures recognized by SandFly](#gestures-recognized-by-sandfly).

### Low-latency audio

Android's usual audio path adds about 60 to 70 ms before a sound or speech reaches the speaker. When you turn on **Controls** > **Touch and activation** > **Reduce delay** > **Low-latency audio**, SandFly plays its sounds and speech through a single fast audio track at your phone's own sample rate. On a Galaxy Z Fold7, this path takes about 21 ms.

Low-latency audio also changes how pausing speech works. When you tap with two fingers, speech stops mid-word, and when you resume, it carries on from exactly where it stopped. SandFly holds up to 10 seconds of paused speech.

With low-latency audio on, SandFly also watches for a speech engine that stops responding. If the engine says nothing about new speech for 3 seconds, SandFly switches to the next installed engine.

Low-latency audio is on by default, except on Wear OS watches. If you used an earlier version of SandFly with the setting off, SandFly turns it on once when you update. If speech or sounds break up, turn the setting off. Low-latency speech requires Android 11 or later; on older versions, only SandFly's sounds use the fast path. Speech falls back to the usual path for engines that play their own audio or that report errors. During calls, and for 10 seconds after a call starts or ends, speech also plays the usual way. Each sound plays the usual way the first time that SandFly uses it, and 3D sounds still play through the 3D player.

### Less lag while scrolling

After each scroll event, TalkBack searched the list for a new item to focus. The search blocked touch exploration and could make gestures fail. SandFly searches once, after scrolling stops.

### Less stalling in busy apps

In busy apps, a backlog of accessibility events could block gestures for almost 3 seconds. SandFly handles events in slices of about 16 ms, so gestures get through between them. It also skips lookups of view classes that can never succeed, which stalled screens that have many custom views.

### Shorter focus delay

When you touch the screen, SandFly waits to see whether you're starting a gesture before it focuses the item under your finger. **Controls** > **Touch and activation** > **Reduce delay** > **Focus delay** sets this wait. TalkBack waits 300 ms by default, which is most of the time between touching an item and hearing it. SandFly waits 200 ms.

If you used an earlier version of SandFly with the delay at 300 ms, SandFly changes it to 200 ms once when you update. If you chose another delay, SandFly keeps it. If SandFly focuses items when you meant to swipe, choose a longer delay.

### Faster response on empty space

When you touched an empty part of the screen, TalkBack waited 100 ms after the focus delay before it played the sound. SandFly waits 40 ms.

### Faster screen changes

After a window changes, TalkBack waits for the window to settle before it says the title. TalkBack counted that wait in a way that made 550 ms last about 900 ms. SandFly counts the real time.

The **Turn off animations** setting in **Controls** > **Touch and activation** > **Reduce delay** turns off animations for the whole phone, so screens change immediately and the wait is only 200 ms. TalkBack called this setting **Reduce window announcement delay**, and had disabled the code that turns animations off, so the setting did nothing. SandFly makes the setting work, and turns animations back on when you turn SandFly off. On Android 12 and earlier, which don't let SandFly turn animations off, the setting only shortens the wait. The setting is on by default.

When you activated an item, or pressed a key, while the item was being spoken or just after, TalkBack still spoke its usage hint, such as "Double-tap to activate", and the name of the new window waited for the hint. SandFly drops the hint.

TalkBack speaks the window name only after the speech before it, and the first item in the new window only after the window name. **Verbosity** > **Window names** chooses when window names are spoken:

*   **Always**, the default, as TalkBack does.
*   **Always, cut off by the first item**: the window name interrupts the speech before it, and the first item interrupts the window name as soon as it is focused, so usually only the window sound is heard before the first item.
*   **Only when a window opens**: no window name when focus moves into another window, such as the keyboard.
*   **Never**: a screen change plays only the window sound.

### Swiping into the next part of a list

When the next item was only partly on screen, TalkBack scrolled it into view, waited for the scroll before it spoke, and then waited another 110 ms in case more scroll events arrived. SandFly speaks the item immediately and lets the scroll finish while it speaks.

When the next item is off screen, SandFly checks once per frame whether the list has moved, rather than wait for the scroll event, which Android sends at most every 100 ms. On a Samsung phone, swipes that scroll went from about 310 ms to about 60 ms when the item was partly on screen, and from about 320 ms to about 140 ms when the list had to scroll a page.

SandFly also finds the end of a list that's laid out in reverse, such as a chat, correctly.

### Scroll views that show the next screen immediately

Some screens are one long page rather than a list, and keep all of their content in the accessibility tree. On these screens, when you swipe past the last item on screen, SandFly scrolls straight to the next screen without the animation, so that the next item is at the top, and searches again immediately. An item that was cut off at the edge of the screen is read rather than scrolled past, as TalkBack's page scroll did.

This requires Android 14 or later. Lists, grids, pagers, and Compose screens still scroll a page at a time, because they create only the items on screen or they animate their scrolling.

### Focus that stays where a swipe's scroll put it

A list can keep scrolling for a moment after a swipe has moved focus to the next item. TalkBack treated the rest of that scroll as a scroll that you made, and moved focus on to the next item by itself. SandFly counts it as part of its own scroll. As a result, if you scroll the same list yourself within a second of a swipe's scroll, focus doesn't move.

### Swiping away from long paragraphs

Some speech engines, such as Gryphon, stop only between the blocks of text that they synthesize, and treat a whole item as one block. When you swiped away from a long paragraph on a web page, the next item could wait up to 850 ms for the engine to stop.

To fix this, turn on **Text-to-speech** > **Send long text a sentence at a time**. SandFly then sends text that's longer than 150 characters about a sentence at a time, so the engine stops immediately. In Chrome on a Samsung phone with Gryphon, web swipes went from as much as 900 ms to at most about 110 ms.

The setting is off by default, because many engines, such as RHVoice, already stop immediately, and they pause longer between sentences that are sent separately.

## Speech

### Text-to-speech settings

In TalkBack, **Text-to-speech** opens Android's system speech settings. SandFly has its own **Text-to-speech** screen, under **Feedback** in SandFly settings. On this screen, you can choose a speech engine for SandFly that's separate from Android's default engine, and set the speech volume, rate, and pitch with sliders. It also links to Android's system settings, and holds the speech settings that are described in the following sections.

### Speech engine reading control

The **Speech engine** reading control switches between your installed speech engines when you swipe up or down. It changes the engine of **SandFly default**, so it's unavailable while a voice profile is in use. It isn't in the reading controls by default. To add it, go to **Reading controls**.

### Voice profiles

A voice profile is a set of speech settings that you can switch to at once: a speech engine, language, voice, volume, rate, pitch, and **Send long text a sentence at a time**. For example, one profile can use a slower rate for long text while **SandFly default** stays fast. To add one, go to **Text-to-speech** > **Voice profiles** and choose **Add voice profile**. When you choose a language, the voice list shows only the engine's voices in that language.

To switch profiles, swipe up or down with the **Voice profile** reading control, or choose **Voice profile in use** on the **Voice profiles** screen. The **Voice profile** reading control is separate from the **Speech engine** reading control. It isn't in the reading controls by default. To add it, go to **Reading controls**. You can also add a **Voice profile** item to the SandFly menu in **SandFly menu** settings, which lists the profiles and says which is in use. The **Choose voice profile**, **Previous voice profile** and **Next voice profile** actions can be assigned to gestures and keyboard shortcuts.

The reading control goes through the profiles in the order of the **Voice profiles** screen, after **SandFly default**. To change the order, hold a profile and drag it, or use the **Move up** and **Move down** actions. SandFly says which profile it moved above or below. Profiles also have **Rename** and **Delete** actions, which a long press shows too.

**SandFly default** is always there, and uses the rest of the text-to-speech settings. While a profile is in use, changing the speech rate or pitch with gestures or the reading controls changes that profile only. All speech uses the profile's engine and voice, so SandFly doesn't switch language or dialect, and the **Spoken language** reading control and menu item are hidden.

### Speech volume

TalkBack always plays speech at the accessibility volume. In SandFly, **Text-to-speech** > **Speech volume** chooses the volume that speech and SandFly's sounds play at:

- **Accessibility volume**, the default.
- **Media volume**.
- **Ring volume**: speech plays as a ringtone does, so it stays on the phone when media is cast to another device. Like a ringtone, it can also play on the phone's speaker while headphones are connected, and it is silent when the ringer is.
- **Call volume, earpiece**: speech plays as a call does, on the earpiece when no headset is connected.

The **Speech volume** reading control, which you can turn on in **Reading controls**, switches between them. While **Audio output device** sends speech to a device other than the system default, speech uses the accessibility volume.

### Automatic language and dialect changes

When text is marked as another language, TalkBack switches the voice to that language. SandFly lets you turn this off. In **Text-to-speech**, **Switch language automatically** controls changes to another language, and **Switch dialect automatically** controls changes to another country's form of the voice's language, such as US English text with a British English voice. Both are on by default.

When you turn a change off, SandFly no longer splits the text or pauses around it. Text marked as the voice's own language and country, such as US English for a US English voice, isn't split either, whatever the settings, since the voice doesn't change. SandFly compares languages by their codes only, so Serbian in Latin and in Cyrillic script counts as two dialects, and Norwegian marked as `no` and as `nb` counts as two languages.

### How emoji are spoken

Some speech engines read emoji badly, use names of their own, or don't read them at all. **Verbosity** > **Emoji** chooses how they're spoken:

*   **Read by speech engine**, the default, gives the engine the emoji as TalkBack did.
*   **Read by SandFly** replaces each emoji with its name from the [Unicode CLDR](https://cldr.unicode.org/), in the language the text is spoken in, for 138 languages and regional variants. A skin tone, a family, a flag or a keycap is one emoji with one name.
*   **None** leaves emoji out, except in text that is only emoji, such as a reaction button or a character, which is still named.

With **Read by SandFly**, **Count repeated emoji** says an emoji repeated in a row once with a count, such as "3 grinning face", from 2 to 6 repeats. Moving by character names a whole emoji once, and moving by word stops on emoji. Copying or spelling the last spoken phrase gives the emoji, not their names. You can also add **Emoji** to the reading controls and the SandFly menu.

With **None**, moving by word skips emoji, as moving by character still names them. Emoji newer than SandFly's names, Emoji 18.0, are left to the speech engine. Some languages lack CLDR names for some emoji, which are then spoken in English.

### Resume speech where you paused it

With speech engines that don't report word positions, such as RHVoice and Gryphon, TalkBack started the text over when you resumed speech. SandFly learns how fast your engine speaks from the items that it finishes, works out how far the engine got when you paused, and resumes from the start of that sentence or phrase. It can repeat a few words, but it never skips any. Engines that report word positions still resume from the word, and with [low-latency audio](#low-latency-audio) on, speech resumes mid-word.

When speech resumes, SandFly doesn't play the item's sound and vibration again.

### Two-finger tap right after a swipe

TalkBack didn't recognize a two-finger tap made right after a swipe, or it paused older speech instead of the current item. SandFly recognizes the tap and pauses the speech for the item that you swiped to.

### Keys that stop speech

In TalkBack, only Ctrl on a physical keyboard stops speech, so a window name or an announcement held up the item that a key moved to. In SandFly, every key on a physical keyboard stops speech, except modifier keys such as Shift. Ctrl, Alt and Meta shortcuts always stop it. In **Verbosity**:

*   **Interrupt speech when typing** chooses when typed characters stop speech: **Always**, the default, **In edit fields**, or **On keyboard echo**, only when the keyboard echo says what you typed.
*   **Interrupt speech on Enter**, on by default.

A window name that a key stopped isn't spoken later. Pressing Ctrl alone pauses speech, and SandFly keeps the key from the app, so that an app such as YouTube doesn't show its keyboard shortcuts. A shortcut with Ctrl still reaches the app. An app that needs Ctrl alone, such as a remote desktop app, doesn't get it.

### Speech while an app records

While another app records from the microphone without headphones, and during calls, TalkBack dropped announcements that you asked for with a gesture or key. SandFly speaks them: the time and date, the battery, the status, the window title, phonetic letters, **Inspect focused item**, and turning scripts on or off.

With **Text-to-speech** > **Quiet while recording** on, SandFly stays quiet while an app records without headphones, except for speech within two seconds of a touch, gesture, key or braille input, so that notifications and other speech you didn't ask for aren't recorded. It's off by default.

### Speak volume changes

To stop SandFly from saying the new volume after you press the volume keys, turn off **Verbosity** > **Speak volume changes**. It's on by default.

### Keyboard echo

**Keyboard echo**, first under **Typing and braille**, sets the echo for each way of typing: **On-screen keyboard**, **Physical keyboard**, **Braille keyboard** and **Braille display**, each with **None**, **Characters**, **Words**, or **Characters and words**. In TalkBack, keyboard echo is in **Verbosity**, and the braille keyboard follows the on-screen keyboard's setting. Each echo setting is also a reading control and a SandFly menu item, which you can turn on in **Reading controls** and **SandFly menu**. Keyboard echo isn't part of the verbosity presets. When you update, SandFly copies your earlier echo settings once.

Word echo says the whole word, back to the last space, as it is in the field. TalkBack said only the part typed since the last punctuation, such as "normal" for "non-normal", and only the changed letters of a corrected word. Words with apostrophes or hyphens, such as "don't", are one word, and a web or email address is one word. When a keyboard adds a word, its punctuation, and a space all at once, as the braille keyboard does in contracted braille, SandFly echoes the word rather than just the space.

### Order of item details

**Verbosity** > **Order of item details** sets the order in which SandFly says an item's name, type, and state. TalkBack has this setting in **Advanced settings** and offers three orders to choose from. SandFly lists the name, type, and state, and you put them in any of the six orders: touch and hold a detail and drag it up or down, or use its **Move up** and **Move down** actions. With SandFly on, double-tap and hold a detail, then drag it. SandFly says the detail's position as you pick it up, move it, and drop it.

TalkBack ignored the order for a row whose checkbox or switch has no text of its own, such as the rows in **Reading controls**, and always read the checkbox's type and state before the row's text. SandFly reads such a row as one control, with the row's text as its name, in the order that you chose. A button or tab whose label is a separate view inside it, as in the Pixel power menu, also follows the order, with the label as its name.

### "SandFly off" at the right volume

When you turn SandFly off, it says "SandFly off" at the accessibility volume, using your device's real volume levels. TalkBack only approximated that volume.

### Table column headers

In **Verbosity**, you can choose whether SandFly reads table column headers before or after the cell's data, or leaves them out:

*   **After cell data**, the default, keeps TalkBack's order: the cell's contents, then the row and column headers or numbers.
*   **Before cell data** speaks the row and column headers or numbers before the cell's contents, along with "Column heading" or "Row heading" for a header cell. This applies to grids as well as tables, so an item in a photo grid says its row and column first. On a TV, SandFly already speaks them first, so this option changes nothing there.
*   **Do not read** leaves column headers out, and reads column numbers only when row and column numbers are on.

Under preset settings in **Verbosity**, you can also turn off **Speak row and column numbers** to hear only named headers without row and column coordinates. The setting is on by default.

Both settings only apply while **Speak container info** is on, because rows and columns aren't spoken at all when it's off.

### Speak letters phonetically

TalkBack's **Speak letters with examples** switch is called **Speak letters phonetically** in SandFly, in **Verbosity** under preset settings, and in on-screen keyboard settings. It has four choices:

*   **Off** says only the letter.
*   **After the letter, delayed**, the default, is what the switch did when it was on. Keyboard keys say the letter, then its phonetic word, as in "h, hotel". Moving by character, the word comes a quarter of a second after the letter, and moving on before then skips it.
*   **After the letter, no delay** says the letter and its word together, as in "h hotel".
*   **Instead of the letter** says only the word, as in "hotel".

With **After the letter, no delay** and **Instead of the letter**, SandFly says it in all of these places. **After the letter, delayed** still ignores **Spell last spoken phrase**, deleting a letter, and inserting a letter with a touch keyboard like Gboard, just as TalkBack did when **Speak letters with examples** was turned on.

The **High** preset uses **After the letter, delayed** and the **Low** preset uses **Off**. Your choice from the switch carries over.

## Notifications

### Samsung watch notification content

On Samsung watches, SandFly reads the app and title, then the message content, then the time when you focus a notification card. Samsung's card label can omit the message even though it is available in the accessibility tree. SandFly includes that text automatically, without an extra gesture or setting, and does not announce the card's display font formatting.

### Reading notifications

TalkBack reads each new notification with a fixed format and has no settings for it. In SandFly, **Notifications** is a category of its own in SandFly settings. **Speak notifications** turns reading new notifications on or off, and is on by default. Incoming calls are always read. The other screens are:

*   **Reading notifications**: **Speech format**, with the placeholders `{app}`, `{title}`, `{text}`, `{content}`, `{bigtext}`, `{subtext}`, `{ticker}`, `{category}` and `{time}`, and what private notifications say. **Voice** reads notifications with one of your [voice profiles](#voice-profiles), with all of its settings, instead of SandFly's voice. **Detect language** reads each notification in the language it's written in. When notifications for different voices arrive together, each waits for the one before it, so that two voices don't speak at once. Also a sound before notifications, emoji, capitals, long numbers, links, length limits in words, sentences, characters or seconds, a wait before reading, and what happens when a notification arrives while another is read.
*   **Apps**: for each app, **As other apps**, **Read**, **App name only**, **Read first** or **Don't read**, and a name, minimum time between notifications, speech format and **Voice** of its own. **Voice** reads the app's notifications with one of your voice profiles, such as one with another speech engine, or with SandFly's voice.
*   **Words and replacements**: words that a notification must have to be read, words that keep it from being read, and text to say differently.
*   **Kinds of notifications**: skip media players, ongoing notifications, running app notifications, progress notifications, group summaries, and low priority and system notifications.
*   **When to read**: with the screen on, off or locked, with or without headphones, while media plays, in Do Not Disturb, while the screen is touched, during calls, in silent, vibrate or car mode, and quiet hours.
*   **Rules**: conditions, such as a Bluetooth device or Wi-Fi network being connected, the battery, charging, the time, the app in front, the app or text of the notification, and actions, such as not reading it, reading only the app name, another speech format, reading it with a voice profile, or reading it first. A rule's voice takes the place of the app's. **Add a rule from a template** starts from rules such as **Silent at night**.
*   **Repeats**: skip the same notification for a while, a minimum time between an app's notifications, and leave out a start that repeats, such as a group name in a chat.
*   **Stop reading**: shake, wave over the proximity sensor, press both volume keys, dismiss the notification, or the app removes it.
*   **Summary and time**: read the notifications in the shade now or every day at a set time, and say the time at a set interval.
*   **History**: the last 100 notifications, each with whether it was read and why not, and actions such as **Don't read notifications like this**.
*   **Send a test notification**, **Allow automation apps** and **Automation**, which let apps such as Tasker turn notification speech on or off, stop it, or read the notifications.

The **Notification speech** quick settings tile turns reading on or off. The gesture actions **Read notifications**, **Notification speech on or off**, **Read the last notification again** and **Stop reading the notification** can be assigned in gesture settings. Several of these settings need notification access, which **Turn on notification access** on those screens opens.

### SandFly's notification list

With **Notifications** > **Use SandFly's notification list** on, the **Notifications** action opens a list of SandFly's own instead of the system's notification shade, which some phones, such as Samsung phones with many notifications, redraw so often that swiping through it is slow. The list shows each notification with its actions and reply field, groups as one item that shows its notifications when you double-tap it, and Playing or Paused for media. Text longer than about 160 characters is cut, and the **Show all** action reads the whole text. **Dismiss** and **Clear all** work as in the shade. **Notification details** sets which parts of a notification are read and in what order: title, text, app, time and playback. The list needs notification access, and isn't used while the phone is locked. The **System notification shade** gesture action opens the system's shade.

### Notifications that Android doesn't announce

Android tells screen readers about a new notification only when it alerts. A notification that Do Not Disturb holds back, or an update that an app posts without alerting again, such as a doorbell app replacing its last motion alert, never reaches TalkBack. When SandFly has notification access, it also receives these from its notification listener, and reads them as its notification settings say, so **Do Not Disturb** in the notification settings applies to them too. While the phone is locked, Android gives screen readers only the public version of a private notification, which many apps don't have, so TalkBack reads nothing; with notification access, SandFly reads the whole notification from its listener instead. Progress and ongoing notifications are left out. Incoming calls are listed in the notification history as announced.

### Toasts in Do Not Disturb

While Do Not Disturb was on, TalkBack didn't speak toasts, the short messages that apps show at the bottom of the screen. SandFly speaks toasts, and stays silent only for notifications.

### No "collapsed" on each notification

SandFly doesn't say "collapsed" for each notification on the lock screen and in the notification shade. It still says "expanded" when you open a notification, and it still says "collapsed" in other apps.

### Notices that you can dismiss

TalkBack marked its notices that it was updated, or that gestures changed, as ongoing, so you couldn't swipe them away or dismiss them. In SandFly, you can dismiss these notices, including on the lock screen. The notifications for pausing SandFly and for downloading an on-device model stay ongoing while those continue.

### SandFly's quick settings panel

With **Advanced settings** > **Use SandFly's quick settings** on, the Quick settings action opens a grid of SandFly's own instead of the system's quick settings: switches, sliders and buttons in the order you choose, in as many columns as you choose. SandFly says each tile's row and column. Each control is one tile with its name, its state and, where there is one, what it's connected to. Swipe up or down on a volume slider to move it one step. Double tap and hold an item to open its settings. The **System quick settings** gesture action and the panel's button open the system's quick settings.

**Advanced settings** > **Quick settings panel** adds, removes and moves items, and sets the number of columns. Each item says its row and column; **Move** lists the cells, and the item you move takes the cell you pick while the items between move one cell to make room. SandFly says how many moved. **Add control** lists SandFly's controls: Wi-Fi, Bluetooth, mobile data, hotspot, airplane mode, data saver, Do Not Disturb, sound mode, flashlight and its brightness, location, auto-rotate, battery saver, dark theme, Night Light, Extra dim, color inversion, color correction, NFC, microphone and camera access, brightness, adaptive brightness, screen timeout, font size, the volumes, media, media output, next alarm, battery, Settings and the power menu. **Add app tile** lists the tiles that installed apps offer, and **Add system tile** lists the system's own tiles. A system tile that SandFly has a control for, such as Hotspot, is added as that control.

Android only lets the system and the shell change some settings, and only lets the system run tiles. With Shizuku running, SandFly changes the radios, the hotspot, data saver, battery saver, dark theme and the microphone and camera switches with shell commands, runs app tiles with `cmd statusbar click-tile`, and gives itself the permissions it needs for the rest. Without Shizuku, those switches open the system's settings for them. System tiles, and app tiles without Shizuku, are used in the system's quick settings: SandFly opens them, activates the tile and closes them without speaking them, and then says the tile's new state.

## Sound and vibration

### Audio output device

**Sound and vibration** > **Audio output device** chooses where SandFly's speech and sounds play. **System default** follows Android. **Phone speaker** plays on the phone's speaker even when Bluetooth is connected, and you can also choose a connected Bluetooth, wired, USB, or hearing aid device by name.

SandFly keeps your choice when devices connect or disconnect, or when other apps start playing. If the device that you chose isn't connected, SandFly uses another external device, and the setting shows "(disconnected)" next to the device name. During phone calls, Android decides where audio plays.

**Sound and vibration** > **Phone speaker with these devices** lists the Bluetooth, wired and USB outputs that have been connected while SandFly runs. While one that you check is connected, SandFly plays on the phone speaker, whatever **Audio output device** is set to: for example, on the speaker while music plays on a Bluetooth speaker, and in your ears with earbuds. Outputs are known by name, so two devices with the same name are one choice.

### A vibration for every sound

Like VoiceOver, SandFly can tell you what happened by touch alone. Every sound has its own vibration, which plays even when **Sound feedback** is off, so you can turn all sounds off and still feel each action. No two actions share a vibration.

Frequent actions are a single hit: a soft tap when focus lands on an item, a firm click on an item that you can activate, and a softer tap over an empty area. Others have a rhythm of their own. The end of a list, or a completed action, is two firm knocks. Entering a list is two light taps with the second one stronger, and leaving a list is two light taps with the second one weaker. A window change is three soft taps with the middle one strongest. When SandFly scrolls a list, you feel three quick ticks. When the circle menu opens, you feel one tick for each item. The loading tone, which repeats while you wait for an image or screen description, has a short heartbeat that's firm enough to feel through a case.

Progress bar tones, the sound for capital letters, and braille display and braille keyboard sounds don't vibrate. The braille keyboard has vibrations of its own, which **Vibration feedback** turns on and off with the rest. Phones that support rich haptics play the vibrations as composed effects. Phones without them play softer and shorter forms of the same patterns.

**Sound feedback volume** is a slider from 5 to 100 percent in steps of 5, rather than a list of 25, 50, 75 and 100 percent. **Vibration strength**, below **Vibration feedback**, sets how strong vibrations are, from 5 to 100 percent, on phones whose vibrator can vary its strength.

### Ticks while you scroll with two fingers

TalkBack played its scroll sound and vibration at most every 250 ms while you scrolled. SandFly plays the scroll sound and one light tick for each item that scrolls past, so a slow drag ticks slowly and a fast drag ticks quickly. Lists that don't number their items tick once for every 48 dp of scrolling. You feel the first tick as soon as the drag reaches the app, and SandFly hands the drag to the app sooner than TalkBack did. Scrolls that SandFly makes itself keep the usual sound.

### Individual sounds and vibrations

To turn off one sound or one vibration and keep the rest, go to **Sound and vibration** > **Individual sounds and vibrations**. There's a switch for each sound and each vibration, including the braille display and braille keyboard sounds, the braille keyboard's vibrations, and the screen-off sound. Turning off a sound keeps its vibration, and turning off a vibration keeps its sound. To hear or feel one, open the actions menu on its switch and choose **Preview**. **Sound feedback** and **Vibration feedback** still turn all of them off at once.

### Sound themes

A sound theme is a set of sounds and vibrations that replace SandFly's own, with its own control sounds and 3D audio settings. You can install as many themes as you like, from a file or a link, and switch between them in **Sound and vibration** > **Sound themes**. A link to a GitHub repository that contains a theme also works, and you can open or share a theme file, or a link to one, with SandFly from another app. On Wear OS watches, you can install themes only from a link.

To replace single sounds with your own, open **Sounds** under the theme that you're using. To share your sounds, save the theme as a file. For more information about installing, using, and making themes, see [Sound themes](themes.md).

### Control sounds and vibrations

Like the [Unspoken](https://github.com/ahicks92/Unspoken) add-on for NVDA, SandFly can play a sound for each kind of element in place of the focus sound, and leave out words such as "button" and "checkbox". There are sounds and vibrations for buttons, checkboxes (which also cover switches and toggle buttons), radio buttons, edit fields, drop-down lists, sliders, images, clocks, tabs, menu items, and list items, and on web pages, for links and tree items. To turn them on, go to **Sound and vibration** > **Sound themes** and turn on **Control sounds and vibrations**.

SandFly has no control sounds or vibrations of its own: they come from the sound theme that you're using. Until a kind of element has a sound or a vibration, it plays the focus sound and SandFly says what kind of element it is. An element that has a vibration but no sound plays the focus sound with its own vibration. A control's vibration plays even with sound feedback off, so you can learn the kinds of controls by touch alone.

A row that isn't a control itself, such as a row in Settings, plays the sound of the control in it, such as its switch, and otherwise the sound for its kind of item, such as a list item. Plain text and keys on the keyboard keep the usual focus sound. Moving into or out of a list plays the list sound and the control's sound together, with the control's vibration.

To keep hearing the kind of control as well, turn on **Still say the kind of control** in **Sound and vibration**. Each control sound and vibration also has its own switch in **Individual sounds and vibrations**.

### Navigation sounds

TalkBack plays a sound and vibration when a gesture starts and another when it ends. SandFly plays neither. In their place, a sound theme can give a sound and a vibration to each of these:

- **Next reading control** and **Previous reading control**: selecting another reading control, with a gesture, the rotor, a keyboard shortcut or the braille keyboard.
- **Edge gesture bottom**, **Edge gesture middle** and **Edge gesture top**: a finger touching an edge where an edge gesture starts, the gesture reaching its first click, and the gesture reaching its second click. Sliding back plays the sound of the place the finger is back at. Lifting plays nothing.
- **Insertion point moved**: moving by character, word, line or paragraph moves the cursor in a text field you are editing.
- **Column wrap** and **Column wrap back**: swiping forward to an item below the row you are in, and back to an item above it. Each item of a list is a row of its own; items side by side, such as icons on a home screen, cells of a table, or a title with buttons beside it, share a row. The sound plays with the focus sound, and its vibration plays in place of the focus vibration.

SandFly has no sounds of its own for these. The edge gesture middle and top have SandFly's edge gesture vibrations; the others vibrate only with a theme's vibration. Each has its own sound and vibration switch in **Individual sounds and vibrations**.

Rows are found by where items are on the screen: an item is on another row when its middle is below the bottom of the focused item, or above its top. Column wrap doesn't play for swipes that move from one window to another.

### 3D audio

Each control sound comes from where the element is on the screen: from the left for elements on the left, and from higher up for elements near the top. With headphones, the sounds play in 3D, using measurements of how a real head hears sound from each direction (the MIT Media Lab KEMAR measurements, by Bill Gardner and Keith Martin). On the phone speaker, the sounds only move between left and right.

On Android 13 and later, SandFly uses 3D when its audio plays on wired or USB headphones, a hearing aid, a Bluetooth media device, or a Bluetooth LE headset. Bluetooth LE speakers, broadcasts, and call audio don't count. On earlier versions, any connected audio device counts. To play the sounds in 3D on the speaker as well, or never, change **3D audio** for the theme. Each theme has its own **Control sounds and vibrations** and **3D audio** settings.

## Gestures

### Default gestures

SandFly changes these default gestures:

| Gesture | SandFly | TalkBack |
|---|---|---|
| Tap with four fingers | Back | Practice gestures |
| Double-tap with four fingers | Home | Tutorial |
| Triple-tap with four fingers | Overview (recent apps) | None |
| Double-tap and hold with four fingers | Notifications | Pass through the next gesture |
| Tap and hold with three fingers | Pass through the next gesture | Screen search |
| Double-tap with three fingers | Turn speech on or off | Copy |
| Triple-tap with three fingers | Copy last spoken phrase | Paste |
| Triple-tap and hold with three fingers | Paste | None |
| Quadruple-tap with three fingers | Hide or show screen | None |
| Double-tap and hold with two fingers | Braille keyboard | Selection mode |
| Triple-tap with two fingers | Speak status | Read from focused item |
| Triple-tap and hold with two fingers | None | Turn speech on or off |
| Rotate clockwise with two fingers | Next reading control | None |
| Rotate counterclockwise with two fingers | Previous reading control | None |
| Scrub with two fingers | Back | None |
| Swipe up from the bottom edge | Home | None |
| Long swipe up from the bottom edge | Overview (recent apps) | None |
| Swipe down from the top left | Notifications | None |
| Swipe down from the top right | Quick settings | None |

SandFly recognizes the quadruple-tap with three fingers, the rotation gestures, the scrub and the edge gestures itself, so they require Android 13 or later and **Handle gestures in SandFly** turned on. You can assign any of these gestures to another action in gesture settings. Wear OS watches have defaults of their own; see [Wear OS watches](#wear-os-watches).

### Navigation gestures

You can assign a gesture to move to the next or previous character, word, line, paragraph, heading, link, control, landmark, button, checkbox, radio button, edit field, combo box, focusable item, graphic, list, list item, table, visited link, unvisited link, or heading of a given level. Moving with one of these gestures doesn't change the reading control. When you choose an action for a gesture, these actions are under **Navigate by text**, **Navigate by element**, and **Navigate by heading level**.

Headings, links, and controls work in apps and on web pages. The other elements work only on web pages.

### More gesture actions

SandFly adds these actions, which you can assign to gestures in gesture settings. None is assigned by default.

*   **Open app**, at the end of the list of actions, lists the apps on the home screen. A gesture set to an app opens it, as its home screen icon does.
*   **Lock screen** and **Power menu**, in **System actions**.
*   **Increase media volume** and **Decrease media volume**, in **Special features**, change the media volume a step and say the new volume.
*   **Scroll to top** and **Scroll to bottom**, in **Basic navigation**, scroll the list that focus is in to its start or end a page at a time, then focus its first or last item. In a list that doesn't scroll, such as a short settings screen, they focus its first or last item. Outside a list, they use the first list on screen, or the first or last item on screen when there is none. A touch, gesture or key while it scrolls stops it. These aren't on watches.
*   **Refresh**, in **Basic navigation** and as a keyboard shortcut, scrolls the list that focus is in to its top and pulls it down, which refreshes it in most apps. **Scroll back** and **Scroll up** pull a list down too when it is already at its top, as scrolling with two fingers does. SandFly sends the pull as a swipe, so it needs Android 11 or later.
*   **System quick settings** and **System notification shade** open the system's own quick settings and notification shade, when **Use SandFly's quick settings** or **Use SandFly's notification list** is on.
*   **Write the letter of a reading control**, in **Menu and controls**, opens a writing surface on the whole screen. Write a letter with one finger, and SandFly chooses the next reading control whose name starts with it: H for Headings, L for Lines, and L again for Links. A tap closes the surface, and so do 10 seconds without writing. Handwriting is recognized on the phone by Google's ML Kit, in the phone's language. The first time, SandFly asks whether to download its model for the language, about 20 MB, from Google, and a message shows when it has downloaded. Not on watches.

When you choose an action for a gesture, the categories of actions are collapsed, except the one with the gesture's current action. The categories in gesture settings, **Reading controls** and **SandFly menu** are collapsed too, and expand when you activate them.

### Status gesture

Triple-tap with two fingers to hear what the status bar shows: the time, the battery, the Wi-Fi network and its signal, and the mobile carrier, network type, and signal. SandFly also says the ringer, Do Not Disturb, and airplane mode when they aren't in their usual state. To hear the name of the Wi-Fi network, allow location access when SandFly asks the first time.

To choose what the gesture says and in what order, go to **Status readout**. Each item has **Move up** and **Move down** actions. In gesture settings, this action is called **Speak status**. To read from the current item, which was the gesture's action in TalkBack, choose **Read from focused item** in the SandFly menu.

### Rotor

Turn two fingers on the screen like a dial to choose a reading control: clockwise for the next one, and counterclockwise for the previous one. Then swipe up or down to change it.

Each step of 30 degrees, a twelfth of a turn, moves one reading control, so you can keep turning to move further. A quick flick moves exactly one step. If you often move past the reading control that you want, choose a larger step, from 15 to 60 degrees, in **Rotor turn per step** in gesture settings. Scrolling with two fingers doesn't turn the rotor.

The rotor requires Android 13 or later and **Handle gestures in SandFly** turned on. You can assign other actions to **Rotate clockwise with 2 fingers** and **Rotate counterclockwise with 2 fingers** in gesture settings.

### Edge gestures and scrub

As in VoiceOver on iOS, you can slide one finger in from an edge of the screen. Start at the very edge, such as on the gesture navigation bar or the status bar, and slide in:

- Up from the bottom edge: you feel a click like a home button after about a centimeter. Lift there to go home. Keep sliding for about four more centimeters to feel a second click, and lift there for Overview (recent apps).
- Down from the left half of the top edge for notifications, or from the right half for quick settings, after the same click. A long swipe down from either half, to a second click, does nothing by default.
- Right from the left edge, or left from the right edge. The top half and bottom half of each side edge are separate gestures, each with a first and second click. They do nothing by default.

Only gestures that have an action click. A swipe that does nothing has no first click, and the slide stays an ordinary swipe until the second click. A long swipe that does nothing has no second click, so sliding further is still the swipe. Lifting before the first click there is an ordinary swipe. In a corner, the top or bottom edge comes first, unless its gestures do nothing.

Sliding back from the second click to the first plays one short tap; sliding back to the edge plays two, and lifting there cancels the gesture. To turn off the clicks, turn off **Edge gesture middle** and **Edge gesture top** under vibrations in **Individual sounds and vibrations**; to turn off the taps, turn off **Edge gesture slide back**. A sound theme can give each stage a sound; see [Navigation sounds](#navigation-sounds). A slide that starts away from the edge, or that takes longer than about half a second to reach the first click, is an ordinary swipe or touch exploration. A finger resting at the edge for 0.2 seconds is touch exploration.

To scrub, move two fingers quickly one way across the screen and back, each stroke about a centimeter long. By default, this goes back. Two fingers moved one way only still scroll.

You can assign other actions to all of these in gesture settings, under **Edges** and **2 fingers**. Under **Edges**, **Distance to first click** sets how far you slide in from the edge to the first click, from 0.5 to 2 cm (1 cm by default), and **Distance from first to second click** how far you slide on to the second, from 1 to 5 cm (4 cm by default).

### Pinch and spread

You can assign actions to pinching and spreading with two, three or four fingers in gesture settings. They do nothing by default. A pinch or spread counts once the fingers have moved about half a centimeter toward or away from their middle, and by a third of the distance, without moving together or turning much. While a two-finger pinch or spread has an action, a two-finger movement that pinches more than it moves waits before it reaches the app, so apps that zoom with two fingers may not zoom until you set both to nothing. A scrub set to nothing is left to ordinary two-finger movements. While a scrub is still possible, a sideways scroll with two fingers waits for up to about half a second before it starts. Pinches and spreads require Android 13 or later and **Handle gestures in SandFly** turned on, and aren't on watches.

### Lift to activate

SandFly can activate the item under your finger when you lift your finger after exploring by touch, as TalkBack already does for keys on the keyboard. In **Controls** > **Touch and activation** > **Lift to activate**, choose **Only on navigation bar** to use it for the Back, Home, and Overview buttons only, or **Entire screen** to use it everywhere. It's **Disabled** by default. With **Only on navigation bar**, a single tap presses the button, and SandFly says which one, such as "Back" or "Home". To stop this, turn off **Controls** > **Touch and activation** > **Speak navigation bar buttons**.

With **Only on navigation bar** on Android 11 and later, touches on the navigation bar go straight to Android, so a single tap presses a button and holding Home holds it, as without a screen reader. SandFly doesn't say the buttons as you touch them.

**Lift to activate** is also a reading control, so you can change it with a swipe up or down.

### Speech kept while you touch the screen

TalkBack stops speech when a finger touches the screen. To keep speech going until touch focuses an item, turn off **Controls** > **Touch and activation** > **Stop speech when touching the screen**. Speech of the item that touch focuses still interrupts, as do gestures that speak. It's on by default.

### Wrap around

When you swipe past the last item on the screen, TalkBack stops once and then goes back to the first item. To stop at the end instead, turn off **Controls** > **Navigation** > **Wrap around**. Each swipe past the end then plays the end sound. This also applies to moving by heading, link, or control in apps, and to moving with a keyboard. Web pages can still wrap by themselves.

**Wrap around** is also a reading control, so you can turn it on or off with a swipe up or down. It's on by default.

### Stuck touch exploration

On some phones, after a touch with several fingers, Android can stop touch exploration for every screen reader until the phone restarts. Swipes and taps still work, but dragging a finger says nothing. The bug is in Android, not in the screen reader.

SandFly notices exploring touches that hover over nothing. To let it fix the problem, it needs the DUMP permission. With [Shizuku](https://shizuku.rikka.app) running, SandFly grants it when it starts: Shizuku asks once whether to allow SandFly. Without Shizuku, grant it once with adb:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
```

With the permission, SandFly reads which touches Android left down, says "Fixing touch exploration", turns explore by touch off while it ends those touches with a gesture outside the screen, usually for less than half a second, and turns explore by touch back on. It then says "Touch exploration fixed". A touch during the fix goes straight to the app, and SandFly tries again after your next exploring touch. Without the permission, after three such touches in a row SandFly says that touch exploration may be stuck, and you need to restart the phone.

This needs a phone whose `dumpsys input` lists the touches that Android holds for accessibility, which Samsung phones do. On other phones, SandFly can only say that touch exploration may be stuck. Exploring something that has nothing for SandFly to read, such as blank space or a game, also hovers over nothing, so without the permission the warning can be wrong, at most once every 10 minutes. To turn this off, turn off **Touch and activation** > **Fix stuck touch exploration**.

### Focus in new windows

When a window opens, or you go back, TalkBack moves focus into it: back to the item you were on, to the text field with the cursor, or to the first item. To leave focus alone, turn off **Controls** > **Navigation** > **Move focus to new windows**. SandFly then speaks only the window name, and the next swipe moves to the first item. This applies to SandFly's own menus and dialogs too, when they open and when they close. Focus follows input focus only when you move it with a key, such as an arrow key or Tab, not when an app moves it by itself. When the item you focused disappears and the windows haven't changed, focus is placed again. The switch isn't on TVs, which need focus to follow input focus.

### Gestures recognized by SandFly

By default, SandFly recognizes gestures itself instead of leaving this to Android, which makes the rotor, the edge gestures, the scrub and the quadruple-tap with three fingers possible. TalkBack leaves this off.

To let Android recognize gestures again, turn off **Advanced settings** > **Developer settings** > **Handle gestures in SandFly**, and then turn SandFly off and on again. When this setting is off, the rotor, the edge gestures, the scrub and the quadruple-tap with three fingers don't work, and **Time between one-finger taps** is hidden.

## Navigation

### Reading place kept when an item disappears

When an app removed the item that had focus, such as an ad that collapses, TalkBack jumped back to the top of the page. SandFly carries on from where you were.

### Moving by character and word in drawn text

Some apps draw their text themselves and give it to screen readers without letting them move through it, as Telegram does for messages. In such text, TalkBack could move by character, word, or paragraph only through a content description, so it couldn't move through these messages, only through reply previews. SandFly moves through the text itself when the app doesn't.

### Double tap with the actions reading control on Android 11

On Android 11, when you picked an action with the **Actions** reading control, a double tap only played a sound. In SandFly, a double tap performs the action, and a double-tap and hold performs a long press.

### Sliders that report themselves as unfocusable

realme UI reports some volume sliders as not focusable and unlabeled, so TalkBack skipped them. SandFly can focus them.

### Vertical navigation

The **Vertical navigation** reading control, which you can turn on in **Reading controls**, moves to the item below when you swipe down and to the item above when you swipe up. It skips the other items of the same row, such as the channel and options buttons beside a video in YouTube, or the buttons beside a file's name. Going up lands on the first item of the row above. In a list, it stays in the list: when there is no row below or above on screen, it scrolls the list a page and moves to the next row. At the end of the list, or outside one, it moves to the item below or above on screen, and plays the end sound when there is none.

### Reading control kept when focus moves

When focus moves onto an item with actions, a slider, a table or a spelling mistake, TalkBack changes the reading control to the one for that item, such as Actions, Adjust slider or Proofread, and changes it back afterwards. On web pages, where many items have actions, this replaces a reading control you chose, such as Headings. To keep the reading control you chose, turn off **Reading controls** > **Change reading control automatically**.

With **Reading controls** > **Links and Controls only in web content** on, the **Links** and **Controls** reading controls are left out of the list outside web content, as the web navigation reading controls are. Gestures and keyboard shortcuts for the next or previous link or control still work everywhere. It's off by default.

### Volume keys that adjust sliders

When you turn on **Controls** > **Touch and activation** > **Volume keys adjust sliders**, the volume keys change the value of the slider that has focus, and SandFly speaks the new value, as it does when you adjust the slider with the **Adjustable** reading control. Hold a key to keep changing the value. When focus is on anything else, or the screen is off, the volume keys change the volume as usual. They also still change speech volume while a finger is on the screen, during continuous reading, and with the keyboard shortcut modifier. The setting is off by default.

**Controls** > **Touch and activation** > **Volume keys move the cursor**, off by default, makes the volume keys move the cursor while a text field has focus: volume up moves it back a character and volume down moves it on, saying the character.

### Telegram reply quotes

In Telegram and its forks, such as Nekogram, the quote of the message that a message replies to was a separate item, often cut off. SandFly reads it with its message, after the time, as "replying to" the sender and text, with the whole quoted text when the quoted message is loaded. The quote isn't focused on its own, so the quoted message can't be opened from it.

### Touching the home screen

Touching between the icons of the home screen no longer says the page, such as "Home screen 1 of 2". Swiping still reaches it. On Android 13 and later, SandFly clears Android's cached copy of the home screen when it opens again, so that its icons can be reached after you leave a launcher's app list, as in Hyperion.

## SandFly menu

### Shorter menu by default

These items are off by default in the SandFly menu: **Actions**, **Screen search**, **Add or edit labels**, **Describe text formatting**, **Copy last spoken phrase**, **Spoken language**, **Voice commands**, **Keyboard shortcuts**, and **Braille display settings**. You can still use actions with the **Actions** reading control. **Text-to-speech** stays on, so that you can reach speech settings if your speech engine crashes, and SandFly adds a **Pause SandFly** item.

To change the items, go to **SandFly menu**, under **Controls** in SandFly settings. In TalkBack, this screen is under **Customize menus**.

### Circle menu

The SandFly menu can show as a circle in the middle of the screen, as it did in TalkBack 8.1 and earlier. Each item is a slice of the screen around the middle. Slide to an item and lift your finger to select it, or lift in the middle of the circle to close the menu. Items that open more items show them in a new circle. When the menu opens, you hear a rising note and feel a tick for each item.

If you open the menu with a gesture that ends with your fingers still on the screen, such as a double-tap and hold, you can slide those fingers out to an item and lift them to select it. The place where your fingers were counts as the middle of the circle.

To turn the circle menu on, go to **SandFly menu** and turn on **Circle menu**. It's off by default.

## Screen and brightness

### Brightness reading control

Use the **Brightness** reading control to change the screen brightness in steps of about 10% when you swipe up or down. It works while the screen is hidden, so you can set the brightness before you give the phone to someone. The first time that you use it, SandFly asks for the **Modify system settings** permission.

### Hide screen

For 3 minutes after you hid the screen, TalkBack set the screen to full brightness. SandFly keeps your brightness. The screen also turns fully black, including around the camera cutout. On some Pixel phones with Android 17, TalkBack's curtain only dimmed the screen.

If you turn off **Always show this** in the hide screen dialog, SandFly says only "Screen hidden" and skips the instructions for showing the screen again. When SandFly starts with the screen hidden, it says "Screen hidden" after "SandFly on".

### Proximity sensor

SandFly doesn't stop speech when something covers the proximity sensor. To turn this back on, go to **Sound and vibration** > **Cover proximity sensor to stop speech**.

### Screen on and off announcements

In **Verbosity** > **Screen on and off**, you can choose what SandFly says when the screen turns off, when it turns on, and when you unlock the phone. When the screen turns off, SandFly says "Screen off" and the ringer mode. When the screen turns on, it says the time, and it can also say the battery, the Wi-Fi network, the mobile network, the ringer and Do Not Disturb, and airplane mode, after the time in the same announcement. When you unlock the phone, it says "Device unlocked". Each of these has its own switch. By default, everything except the extra screen-on items is on.

These switches are separate from **Status readout**, so the status gesture can say different things. SandFly doesn't make the screen-on announcement during calls, and the lock screen no longer cuts off "Screen off".

## Pausing SandFly

### Pause SandFly

Pausing turns off SandFly's speech, sounds, vibration, gestures, and explore by touch, so the phone works as if no screen reader were on. SandFly stays on, so it resumes immediately. TalkBack 8.1 and earlier had this feature as suspend.

To pause, choose **Pause SandFly** in the SandFly menu, assign the **Pause SandFly** action to a gesture, or assign a key to the **Pause or resume SandFly** keyboard shortcut. The first time, SandFly asks you to confirm and says how to resume.

To resume, tap the **SandFly is paused** notification, press volume down three times quickly, or press the keyboard shortcut again. By default, SandFly also resumes when the lock screen shows. To change this, go to **Advanced settings** > **Resume SandFly** and choose **When the screen turns on** or **Only from the notification or a shortcut**.

The volume keys still change the volume while SandFly is paused. If SandFly restarts while it's paused, it starts unpaused.

## Calls

### Speaker when away from your ear

Like VoiceOver on iPhone, SandFly can move a call to the speaker when you take the phone away from your ear, and back to the earpiece when you hold it up again. A call that starts with the phone away from your ear goes straight to the speaker. SandFly leaves Bluetooth and wired headsets alone, and if you turn the speaker on or off in the Phone app, your choice stays until you move the phone again.

Android lets only certain apps, such as smartwatch companions, change where call audio goes, so SandFly needs a permission for it. With [Shizuku](https://shizuku.rikka.app) running, turn on **Sound and vibration** > **Speaker when away from your ear**, and SandFly grants the permission itself; the first time, Shizuku asks whether to allow SandFly. If the permission can't be granted, the setting turns back off. Without Shizuku, grant the permission once with adb, then turn on the setting:

```
adb shell appops set io.github.aaron_gh.sandfly MANAGE_ONGOING_CALLS allow
```

Without Shizuku, until you grant the permission, the setting is unavailable and shows the command, and its **Copy command** action copies the command to the clipboard. This feature requires Android 12 or later.

## Direct touch

Audio games need raw touch, but explore by touch captures taps and swipes before the game sees them. With direct touch, SandFly passes your touches straight to the games that you choose, so you don't have to pause SandFly.

### Built in from NVGT Bridge

Direct touch is all of [NVGT Bridge](https://github.com/trypsynth/nvgt-bridge), the accessibility service that gives audio games direct touch, built into SandFly. Because SandFly is the screen reader and already knows what's on the screen, direct touch works better than running NVGT Bridge alongside SandFly.

There's nothing extra to install or turn on in Accessibility settings, and no second service that can undo SandFly's touch setting. Direct touch shares that setting with the pass-through gesture and the braille keyboard, so none of them cancels another. NVGT Bridge searches each window's views for dialogs and text fields, and looks only five levels deep. SandFly already follows the windows, the keyboard, and the screen state, so it hands touch back to you as soon as they change, with no depth limit. SandFly's own speech says "Direct touch on" and "Direct touch off", even while the game plays audio.

### Choose your games

Go to **Direct touch**, under **Controls** in SandFly settings, and turn on each game in the **Apps** list. When a game that you chose comes to the front, SandFly says "Direct touch on", and when the game gives touch back, it says "Direct touch off". To turn the announcements off, turn off **Speak when direct touch changes**. To feel a short vibration instead or as well, turn on **Vibrate when direct touch changes**. The **Direct touch** switch at the top of the screen turns the whole feature on or off, and is on by default.

### When SandFly takes touch back

Touch returns to SandFly when a dialog appears, another app opens on top of the game, you open the notification shade or quick settings, a text field takes focus, or the screen turns off. Touch goes back to the game when they're gone. The keyboard always stays with SandFly, so you can explore it while the rest of the screen stays in direct touch.

### Direct typing

By default, the keyboard area keeps working with SandFly. For a game that draws its own keyboard, open the actions menu on the game in the **Apps** list and choose **Turn on direct typing**.

### Self-voicing keyboards

A keyboard that speaks for itself, such as Advanced Braille Keyboard, needs your touches in every app, not only in games. Turn it on in the **Apps** list, and while it shows, touches on it go straight to it in any app, and the rest of the screen stays with SandFly.

### Quick settings tile

To pause and resume direct touch without leaving your game, add the **Direct touch** tile to quick settings.

### Back up and restore direct touch settings

**Back up settings** saves your direct touch choices to a file, and **Restore settings** loads them on another device.

### Turn on direct touch from your game

If you develop a game, add this `<meta-data>` element inside your `<application>` element or your main `<activity>` element, and SandFly turns your game on the first time that it sees it. Players can still turn it off.

```xml
<meta-data
    android:name="dev.nvgt.capability.DIRECT_TOUCH"
    android:value="true" />
```

## Granular editing

**Granular editing** opens the text of the focused text field, or of any focused item with text, in a screen of its own, split into pieces: characters, words, sentences, lines or paragraphs, chosen with **Split by**. It is in the **Editing** actions of a text field, and is a gesture action under **Text editing**.

- Double tap a piece to edit it. Leaving it empty deletes it.
- A piece's actions are **Select**, **Insert before**, **Insert after** and **Delete**, also in a dialog with a double tap and hold.
- **Copy** copies the selected pieces, or all of the text when none is selected.
- **More** has **Undo**, **Redo**, **Find**, **Find next**, **Replace** (all matches, ignoring case), **Add text at end**, **Select all**, **Clear selection**, **Cut selected** and **Delete selected**.
- **Save** writes the text back to the text field and closes the screen. If the field is gone by then, the text is copied instead. Back asks whether to save or discard changes.

Text that isn't in a text field, such as a message, can be edited and copied but not saved. Password fields can't be opened.

## On-screen keyboard

### Lift to type, except to send

In **On-screen keyboard** > **Typing method**, **Hold finger to select any key, then lift. Double-tap for Enter, Done, or Send.** types every key when you lift your finger, like **Hold finger to select any key, then lift**, but the key that sends or submits, such as Enter, Done, Send, Search, or Go, still needs a double-tap. This keeps you from sending a message by lifting your finger on the wrong key. SandFly recognizes this key in Gboard. In other keyboards, every key types when you lift your finger.

### Typing by tapping

On Android 13 and later, **On-screen keyboard** > **Typing method** has **Tap any key to type it, or hold finger to select any key, then lift**. A quick tap on a key types it at once, as when typing directly in VoiceOver, so quick double letters type twice. Holding a finger on the keyboard explores the keys, and lifting types the key under the finger.

### Handwriting keyboard

**SandFly handwriting** is a keyboard that types what you write on the screen with one finger, on phones and tablets with Android 11 or later. To turn it on, choose **Typing and braille** > **Handwriting keyboard**, which opens Android's keyboard settings, and then choose it as the keyboard in a text field. It fills the screen, and while SandFly is on, touches on it go straight to it instead of through touch exploration.

Write a letter, several letters or a whole word anywhere on the screen. Strokes that follow each other within 0.6 seconds are one piece of writing; it is then typed and spoken. **Typing and braille** > **Wait after writing** sets this wait, from 0.2 to 1 second, for the keyboard and for **Write the letter of a reading control**. A shorter wait answers sooner, but a letter with several strokes, such as H, needs each stroke to start within it. Dots, such as on an i, count. Two- and three-finger gestures do the rest:

| Gesture | Action |
|---|---|
| Swipe right with two fingers | Space |
| Swipe left with two fingers | Delete the character before the cursor |
| Swipe down with two fingers | New line, or the field's action, such as Send, Search or Done |
| Swipe up with two fingers | Replace the last writing with its next reading of the same length, such as "h" for "H", saying which of how many it is |
| Tap with two fingers | Read the text before the cursor |
| Swipe down with three fingers | Hide the keyboard |
| Swipe right or left with three fingers | Switch to the next or previous keyboard |
| Tap with three fingers | Open the tutorial |

The first time the keyboard opens, a tutorial goes through writing a letter and a word and each gesture, says each step, and waits for you to do it. Nothing is typed in the field during the tutorial. To end it early, swipe down with three fingers.

**Keyboard echo** > **Handwriting keyboard** sets what the keyboard says as you type: each piece of writing, each word when a space or new line ends it, both, or nothing. It is also a reading control and a SandFly menu item, **Handwriting keyboard echo**. The keyboard says deleted characters and the readings that replace the last writing whatever the echo. The app's field is behind the keyboard, so the keyboard does this itself, as the braille keyboard does.

Handwriting is recognized on the phone by Google's ML Kit, in the phone's language, as in [Write the letter of a reading control](#more-gesture-actions). The first time, the keyboard hides and SandFly asks whether to download the model, about 20 MB, from Google; a message shows when it has downloaded. In SandFly's own text fields, the text before the cursor isn't read, and a deletion is said only as "Deleted".

## Braille keyboard

### Keyboard echo for the braille keyboard

In TalkBack, the braille keyboard follows the on-screen keyboard's echo setting. In SandFly, the braille keyboard has its own setting in **Keyboard echo**; see [Keyboard echo](#keyboard-echo). The capital and number signs follow it too.

### Next braille table

Hold dots 4, 5 and 6 and swipe down to switch to the next of the braille keyboard's languages. Contracted braille is turned on or off as it was when you last used that language, and SandFly says the language and whether it's contracted. Speech changes to your first voice profile in that language, if you have one, and back to **SandFly default** when the new language has none. You can change the gesture in the braille keyboard's gesture settings.

### Mongolian braille

The braille languages include Mongolian, uncontracted and contracted, from the liblouis tables.

### Typing sounds

In braille keyboard settings, **Typing sounds** plays Android's keyboard clicks as you type: a click for each character, and the space, delete, and return sounds for those actions. It's off by default, and it works even when touch sounds are off in Android's settings. A [sound theme](themes.md) can replace these sounds with its own.

### Swap top and bottom dots

In braille keyboard settings, **Swap top and bottom dots** makes dot 1 trade places with dot 3, and dot 4 with dot 6. TalkBack's **Reverse dots** is called **Swap left and right dots** in SandFly. You can turn on both.

### Skip the tutorial

The first page of the braille keyboard tutorial has a **Skip tutorial** button, which opens the keyboard immediately.

### Haptics

The braille keyboard uses the same crisp vibration effects as the rest of SandFly. Submitting text feels the same as closing or switching the keyboard. Deleting in an empty field gives a soft vibration that fades out, so that you know that there was nothing to delete.

### Navigation that stays in the text field

If you swiped to another control while the braille keyboard opened, TalkBack moved by character, word, or line in that control instead of in the text field. SandFly moves focus back to the text field before each command.

### Dots that follow how you hold the device

The braille keyboard keeps the dots under your fingers whichever way round you hold the device, with auto-rotate on or off. TalkBack expected the charging port on one side when auto-rotate was off, so holding the phone the other way round typed dot 4 for dot 1.

SandFly remembers how you last held the device up, the whole time that the screen is on, as auto-rotate does. In screen-away mode, the dots follow how you hold the device. In tabletop mode, laying the device flat keeps the side that you held it up with, even from before the keyboard opened, and turning it on the table turns the dots with it. The keyboard says where the charging port is when the mode changes and after each turn, such as "Tabletop mode, charging port on the left". On a phone, the dots turn when the port moves to your other side, because you type with the port on your left or right; a quarter turn in between says "Charging port toward you" or "Charging port away from you". On a tablet or an unfolded foldable, they turn by quarter turns, including upside down, which auto-rotate doesn't do.

#### Lock the orientation

To keep the dots facing the way they are, hold dots 4, 5, and 6 and swipe up. To let them follow the device again, do the same again. There's a lock for each screen size and mode, so you can lock a foldable one way when it's folded and another way when it's unfolded.

#### Tablets held up facing you

Gravity can't tell whether a device that's held up faces you or faces away. When you lay a phone flat after holding it up, SandFly assumes that it faced you, as auto-rotate does, unless you typed on it screen away. The side you typed with screen away is kept, and turns with the phone on the table, until you type on the table. Typing only counts as screen away if you held the phone upright, so tilting a phone that you hold nearly flat in your hands doesn't count. A phone last held in portrait says "Tabletop mode, charging port toward you", and turning it to your left or right on the table puts the port on that side. A tablet or an unfolded foldable that's held in portrait is assumed to have faced you, and one that's held sideways is assumed to have faced away, as when typing screen away.

If you hold or stand your tablet facing you instead, turn off **Tablet held up faces away** in braille keyboard settings. A tablet held up any way round then uses the tabletop layout facing you, with your thumbs holding the bottom edge, and the keyboard says, for example, "Screen toward you, charging port on the left". The dots stay the same way round when you lay the tablet flat. You can also add **Tablet held up faces away** to the reading controls. With the braille keyboard open, swipe right or left with three fingers to reach it, as you would for words or lines, and then swipe down or up with one finger.

#### Limitations

A phone that's opened flat, and that hasn't been held up since the screen came on, follows the screen rotation, and with auto-rotate off expects the port on the left; turning it around on the table, or locking the orientation, corrects this. A phone held up screen away and laid flat without typing, or typed on screen away without ever being held within 40 degrees of upright, is taken to have faced you, so it expects the port on the wrong side; locking the orientation corrects this. Turns on the table require the game rotation vector sensor; devices without it still follow how they're held up. A tablet says where the port is only if it's a portrait tablet with the port at the bottom. Remembering how the device is held keeps the accelerometer running while the screen is on, as auto-rotate does.

## Braille displays

### EuroBraille b.note and b.book

SandFly's EuroBraille driver is from a newer brltty than TalkBack's, so it also supports the b.note, b.note 2, b.book and b.book 2, with their keypads and navigation keys. Over USB they are found by their USB IDs.

### Any braille display that uses HID

On Android 15 and later, TalkBack connects to braille displays that use the HID braille standard over Bluetooth, but only lists displays whose names it knows, so newer displays and braille terminals were left out. SandFly lists any Bluetooth device that offers HID, whatever its name, and connecting checks that it's a braille display, so other HID devices, such as keyboards, can appear in the list too. TalkBack also refused displays that send their keys and receive their cells in separate HID reports, as the standard allows; SandFly connects to those.

### Displays from other apps

Apps can give SandFly a braille display that it can't connect to on its own, such as the built-in keys and cells of a braille notetaker. SandFly uses it like a display connected through Bluetooth or USB: its keys navigate and type with the same commands, and its cells show what SandFly shows. A display with keys and no cells works too.

SandFly only uses a display from an app once you allow it, after a warning that the app will be able to read the screen and control the device. SandFly asks when it finds such an app, and **Displays from other apps** in braille display settings lists them, to allow or stop allowing each one. A notification says which app is in use. A display connected through Bluetooth or USB comes first, and one from an app waits while it's connected. [Providing a braille display from another app](docs/braille-display-api.md) describes the API for app developers.

## Image and screen descriptions

### AI services

Google's source release doesn't include working Gemini support, so **Describe image** only reads text in images and **Describe screen** doesn't work. SandFly adds its own Describe screen with follow-up questions, and sends both to an AI service with your own API key. **Automatic descriptions** > **AI service** chooses Gemini, Claude, OpenAI (ChatGPT), Mistral, Groq, OpenRouter, Ollama Cloud, another service with an OpenAI-compatible API, or **None**. Each service has its own settings screen with its API key and model. For more information, see [AI services](README.md#ai-services).

With Gemini, SandFly asks the model to think as little as it can, and asks Gemini Flash-Lite when the chosen model is busy or takes more than 15 seconds.

### On-device AI

SandFly can describe images and screens with a model that runs on your phone, so nothing is sent to Google or anyone else. For more information, see [On-device AI](README.md#on-device-ai).

### Clear error messages

When Gemini can't describe something, TalkBack says "Something went wrong". SandFly says what failed: the key wasn't accepted, the usage limit was reached, the service is busy, it took too long, or there's no internet connection. For other errors, SandFly says the error code and the first sentence of the service's message, in English. Describe screen speaks its errors instead of showing them in a toast.

## Scripts

### Lua scripts and plugins

Like NVDA's app modules and global plugins, scripts change how SandFly works in one app or in every app. Scripts are written in Lua 5.4. A script can:

*   Give unlabeled buttons names, hide clutter, group items, and change or silence what SandFly says for an item.
*   Read the screen, press buttons, speak, play sounds and vibrate.
*   Add gestures, keyboard shortcuts, SandFly menu items, reading controls, and items in an item's Actions menu.
*   Change everything SandFly says, and react to notifications, typing and screen changes.
*   Use the internet and the clipboard, show dialogs, and keep its own data and settings.
*   With the `java` permission, call any Java code through LuaJava.

To install a script, go to **Apps** > **Scripts** and choose **Import script**. You can also share a script file, or a link to one such as a script's page on GitHub, to SandFly, or open a script file with it, and choose **Install SandFly script**. SandFly lists what the script asks to do before it installs or updates it. **Add the example script** adds a small script to start from.

Each script has a page with an on and off switch, a switch for each permission and for each gesture, key, menu item and reading control it adds, its settings on their own screen, its log, and **Remove**. While a script runs, its gestures and keys come before SandFly's.

Scripts never change SandFly's own screens, so the Scripts screen always works. **Turn off all scripts** on that screen stops every script at once, and so does the **Turn all scripts on or off** action, which you can assign to a gesture that scripts can't take over. Scripts don't run while the screen is off or SandFly is paused, and SandFly only starts the script engine once a script is turned on.

Speech requested with `sf.speak` can be heard while media is playing, including queued and interrupting requests. The script still needs the Speech permission, and muting speech, recording, voice recognition and phone calls retain their existing speech policy.

Hooks that change or suppress speech take priority over queued script work and share a 30-millisecond deadline. If the deadline expires or a long task is already running, SandFly uses its normal speech, so scripts should keep these hooks fast.

**Script sources** lists the scripts in repositories on GitHub, GitLab, Codeberg, Gitea or Bitbucket, to install them. SandFly's own scripts are listed until you remove that source. Once a day, SandFly checks installed scripts for a newer version in the source they came from, and **Check for updates** on the Scripts screen checks now. With **Update scripts automatically** on, the default, a newer version is installed without asking when it's by the same author, asks for no new permissions, and comes from the script's own source. Other updates are listed under **Updates** on the Scripts screen.

Scripts run on phones and tablets only. [Writing SandFly scripts](docs/scripting.md) describes the whole API, and [docs/scripting/examples](docs/scripting/examples) has a script to start from.

### Speech from other apps

Apps can ask SandFly to say text in your own SandFly voice, for example a game, an automation app such as Tasker, or a remote-access tool. To allow this, turn on **Apps** > **Let other apps speak**. It's off by default.

The first time an app asks, SandFly posts a notification that asks whether to allow it, and nothing the app sends is said until you choose **Allow**. You can allow or block each app later in **Let other apps speak**. To hear a short sound before each thing another app says, so that you can tell it apart from SandFly, turn on **Play a sound before other apps speak**. Nothing is said while the device is locked, an app can speak only a few times in a row, and apps can never stop or cut off SandFly's own speech. [Speaking through SandFly from another app](docs/speech-api.md) describes the API for app developers.

### Inspect focused item and Copy screen tree

**Inspect focused item** says the focused item's id, class, role, app, activity and window, and copies them, so that scripts can name the item. **Copy screen tree** copies every item on screen as JSON. Assign either to a gesture in gesture settings, or add it to the SandFly menu in **SandFly menu**, under **Controls**.

## Settings

### Layout and wording

SandFly groups its main settings under **Feedback**, **Notifications**, **Controls**, **Typing and braille**, **Apps**, and **More**, and puts **SandFly menu** and **Reading controls** in the main settings. Many settings have clearer names, such as **Speak item type** and **Order of item details**. SandFly removes the links to the Play Store, the privacy policy, the terms of service, Disability Support, and Google's TalkBack help, and the pages about new features in TalkBack. **Touch and activation** and **Navigation**, in **Controls**, and **Apps** hold settings that TalkBack keeps in **Advanced settings**. **Display speech output** is only in **Developer settings**. **Time format** is in **Verbosity**, **Turn off animations** is in **Reduce delay**, and **Cover proximity sensor to stop speech** is in **Sound and vibration**, instead of in **Advanced settings**.

### Dark mode

On Android 12 and later, SandFly's settings follow the system dark mode.

### Slider titles

SandFly doesn't stop on a slider's title as a separate item when you swipe through settings.

### Feedback

**Send feedback**, in Settings and in the SandFly menu, sends a problem or suggestion to SandFly's developer without a GitHub account. It has three parts:

- **Send feedback**: the form. What you type is kept as a draft until you choose **Send** or **Delete draft**.
- **Inbox**: the feedback you sent, each with its status (Open, or Closed: completed, not planned or duplicate) and how many replies you haven't read. Open one to read the replies and send your own, with **Your reply** and **Send reply**.
- **Drafts**: feedback you started and haven't sent.

While SandFly is on, it checks for replies and status changes every hour, and when you open **Send feedback**. A reply or a new status posts a notification, with sound, that opens the feedback. Closed feedback is still checked for 30 days. **Remove from inbox** stops the checks and deletes the feedback from the phone; the developer keeps it.

### Export and import settings

**Export settings** saves SandFly's settings to a file, and **Import settings** replaces them with a file's, such as one from another phone. API keys stay on the phone. Import settings also reads a settings file exported from Backtalk: the settings in it replace SandFly's, and SandFly's own settings that Backtalk doesn't have are kept.

### App icon that opens the settings

SandFly has an app icon in the launcher, labelled SandFly, that opens its settings. TalkBack's settings open only from the accessibility settings, which need TalkBack to be on. SandFly's settings open from the icon whether SandFly is on or off, so you can set it up, or change a setting, without turning it on first.

## Updates and installing

### Separate app

SandFly installs as `io.github.aaron_gh.sandfly`, so it installs next to Google's TalkBack, with settings of its own. For more information, see [Install](README.md#install).

### Built-in updates

SandFly checks GitHub for new releases, and for development builds if you turn them on, and installs them when you tap its notification. For more information, see [Updates](README.md#updates).

### Appear installed from Google Play

Some apps, such as some banking apps, turn off accessibility services that Android records as installed from anywhere but Google Play. That stops a blind user who installed SandFly from a release from using it with those apps. **Advanced settings** > **Appear installed from Google Play**, off by default, makes Android record Google Play as SandFly's installer, so those apps keep working.

Android only records the installer when an app is installed, and only lets the shell or the system choose it, so SandFly reinstalls its own APK through Shizuku, naming Google Play. It is the same app, with the same settings; only what apps are told about where it came from changes. Reinstalling restarts SandFly, so speech stops for a moment. While the setting is on, the built-in updater installs updates the same way, so an update keeps the recorded installer.

Without Shizuku, the switch can't change the installer; its "Copy command" action gives the adb command to run from a computer. Turning the setting off records the installer SandFly had before. A banking app may still detect a sideloaded install in other ways.

If something records a different installer anyway, such as an update installed while Shizuku isn't running, SandFly puts Google Play back the next time Shizuku is available. While the setting is on but Google Play isn't recorded yet, the setting says so.

## Android TV

### Settings on the TV home screen

Android TV has no other way to open an accessibility service's settings, so SandFly adds **SandFly settings** to the apps on the TV home screen.

### Phone-only features hidden

Settings for features that need a touchscreen or a phone are hidden on TVs, such as **Direct touch**, **Status readout**, and **Speaker when away from your ear**. Pause SandFly also isn't available on TVs, because the volume keys often go to the TV or a soundbar, and there's no notification shade to resume from.

## Wear OS watches

### Same settings as phones

Watches show the same SandFly settings as phones, including **Text-to-speech**, all of **Verbosity**, **Individual sounds and vibrations**, **Lift to activate**, **Turn off animations**, **Status readout**, the rotor, the reading controls, the delay settings, update checks, and the circle menu, which is scaled to fit the screen. Settings for features that watches don't support are hidden, such as the braille keyboard, keyboard shortcuts, voice commands, direct touch, on-device AI, **Speaker when away from your ear**, and **Resume SandFly**.

### Gestures on watches

Watch screens track only two fingers, so gestures with three or four fingers don't work, and two-finger swipes scroll the app. Watches use the phone's default gestures except for the following:

| Gesture | Action on watches |
|---|---|
| Double-tap and hold with two fingers | Open the SandFly menu |
| Triple-tap and hold with two fingers | Turn speech on or off |
| Swipe left then up | Repeat last spoken phrase |

On Samsung watches, turn on **Controls** > **Touch and activation** > **Reserve gestures for Vibration Watch** to let Samsung's Vibration Watch use two-finger single and double taps. The switch is off by default, and Vibration Watch must also be enabled in the watch's accessibility settings. While the switch is on, these gestures no longer perform their SandFly actions: by default, a two-finger tap pauses or resumes speech, and a two-finger double tap controls media or starts voice input. Saved gesture assignments are kept and become available again when you turn the switch off.

### SandFly menu on watches

The SandFly menu scrolls on watches, so you can reach every item. **Describe screen** is off in the watch menu by default, and **Pause SandFly** isn't available on watches.
