# Differences from TalkBack

Backtalk is built from Google's TalkBack source releases. This document describes how Backtalk differs from TalkBack: features that TalkBack doesn't have, defaults that Backtalk changes, and problems in TalkBack that Backtalk fixes. To learn how to install Backtalk, see the [README](README.md).

Each section names the settings involved as they appear in Backtalk. Unless a section says otherwise, each setting is in Backtalk settings.

## Speed

### Faster speech after a swipe

When you swiped to an item, TalkBack waited for the app to confirm the new focus before it put together what to say. Backtalk speaks the item as soon as it has set focus, and does less other work around each swipe. On a Galaxy Z Fold7, the median time from lifting your finger to hearing speech went from about 68 ms to about 39 ms.

This doesn't apply when focus moves to another window, when the screen changes, or during continuous reading. Keys on the keyboard, sliders, progress bars, pages in a pager, and web content still wait for the app.

### Next swipe prepared in advance

While you listen to an item, Backtalk works out what a swipe forward or back would say. When your swipe lands on the item that Backtalk prepared, speech starts immediately: on average 33 ms after the swipe, compared with 60 ms without preparation. In testing, about four in five swipes used prepared speech.

Backtalk doesn't prepare anything for the first swipe on a screen or after the screen changes, and it discards prepared speech after 5 seconds.

### Items that a swipe scrolls into view

When a swipe moves to a list item that is only partly on screen, the list scrolls it into view. By default, Backtalk then waits for the app to confirm the new focus, as TalkBack does, so these swipes don't get the faster speech above. Speaking straight away would come before the list had scrolled, when the item's text can still be off screen, and only its place in the list, such as "20 of 80", would be said.

When you turn on **Advanced settings** > **Reduce delay** > **Speak items before they scroll into view**, Backtalk speaks these items straight away too, including the parts that are still past the edge of the list, since they will be on screen once it has scrolled. Parts the app has hidden are still left out. Apps that follow Android's conventions work correctly with this setting on. It is off by default to protect you from apps that don't: in those, a part that the app keeps out of sight by moving it outside the item, instead of hiding it, is read even though it isn't shown, and so is a part that stays off screen when the list can't scroll the whole item into view.

### Reading order kept between swipes

TalkBack asked the app for every item on the screen on each swipe, and waited for each answer. Backtalk keeps the reading order between swipes, and builds it again only when the screen changes, or when Backtalk scrolls or clicks. Changes to text or state, such as a clock that ticks or a progress bar that moves, don't discard the order.

To find the next item in a list, TalkBack also built the reading order of the whole screen three times for one swipe: once to check whether focus was at the end of the list, once to find the item, and once to check whether the item needed to be scrolled into view. Backtalk builds it once and reuses it.

In a test on a Realme phone, three in four swipes reused the reading order, and the longest pause during swiping went from about 450 ms to about 150 ms.

### Time between one-finger taps

TalkBack always waits 0.25 seconds for another tap before it decides that a tap gesture is finished. In Backtalk, you can set this wait for one-finger taps in **Advanced settings** > **Reduce delay** > **Time between one-finger taps**, down to 0.1 seconds. A shorter time makes one-finger multi-tap gestures respond faster, but a slow double tap can then count as two single taps.

Taps with two or more fingers always allow at least 0.25 seconds between taps, as lifting and placing several fingers takes longer.

This setting appears only when Backtalk recognizes gestures itself, which requires Android 13 or later and **Handle gestures in Backtalk** turned on. For more information, see [Gestures recognized by Backtalk](#gestures-recognized-by-backtalk).

### Low-latency audio

Android's usual audio path adds about 60 to 70 ms before a sound or speech reaches the speaker. When you turn on **Advanced settings** > **Reduce delay** > **Low-latency audio**, Backtalk plays its sounds and speech through a single fast audio track at your phone's own sample rate. On a Galaxy Z Fold7, this path takes about 21 ms.

Low-latency audio also changes how pausing speech works. When you tap with two fingers, speech stops mid-word, and when you resume, it carries on from exactly where it stopped. Backtalk holds up to 10 seconds of paused speech.

With low-latency audio on, Backtalk also watches for a speech engine that stops responding. If the engine says nothing about new speech for 3 seconds, Backtalk switches to the next installed engine.

Low-latency audio is on by default, except on Wear OS watches. If you used an earlier version of Backtalk with the setting off, Backtalk turns it on once when you update. If speech or sounds break up, turn the setting off. Low-latency speech requires Android 11 or later; on older versions, only Backtalk's sounds use the fast path. Speech falls back to the usual path for engines that play their own audio or that report errors. Each sound plays the usual way the first time that Backtalk uses it, and 3D sounds still play through the 3D player.

### Less lag while scrolling

After each scroll event, TalkBack searched the list for a new item to focus. The search blocked touch exploration and could make gestures fail. Backtalk searches once, after scrolling stops.

### Less stalling in busy apps

In busy apps, a backlog of accessibility events could block gestures for almost 3 seconds. Backtalk handles events in slices of about 16 ms, so gestures get through between them. It also skips lookups of view classes that can never succeed, which stalled screens that have many custom views.

### Shorter focus delay

When you touch the screen, Backtalk waits to see whether you're starting a gesture before it focuses the item under your finger. **Advanced settings** > **Reduce delay** > **Focus delay** sets this wait. TalkBack waits 300 ms by default, which is most of the time between touching an item and hearing it. Backtalk waits 200 ms.

If you used an earlier version of Backtalk with the delay at 300 ms, Backtalk changes it to 200 ms once when you update. If you chose another delay, Backtalk keeps it. If Backtalk focuses items when you meant to swipe, choose a longer delay.

### Faster response on empty space

When you touched an empty part of the screen, TalkBack waited 100 ms after the focus delay before it played the sound. Backtalk waits 40 ms.

### Faster screen changes

After a window changes, TalkBack waits for the window to settle before it says the title. TalkBack counted that wait in a way that made 550 ms last about 900 ms. Backtalk counts the real time.

The **Turn off animations** setting in **Advanced settings** > **Reduce delay** turns off animations for the whole phone, so screens change immediately and the wait is only 200 ms. TalkBack called this setting **Reduce window announcement delay**, and had disabled the code that turns animations off, so the setting did nothing. Backtalk makes the setting work, and turns animations back on when you turn Backtalk off. On Android 12 and earlier, which don't let Backtalk turn animations off, the setting only shortens the wait. The setting is on by default.

When you activated an item, or pressed a key, while the item was being spoken or just after, TalkBack still spoke its usage hint, such as "Double-tap to activate", and the name of the new window waited for the hint. Backtalk drops the hint.

TalkBack speaks the window name only after the speech before it, and the first item in the new window only after the window name. To hear the first item sooner, turn on **Speak first item without waiting for window name** in **Advanced settings** > **Reduce delay**. The window name then interrupts the speech before it, and the first item interrupts the window name as soon as it is focused, so usually only the window sound is heard before the first item.

### Swiping into the next part of a list

When the next item was only partly on screen, TalkBack scrolled it into view, waited for the scroll before it spoke, and then waited another 110 ms in case more scroll events arrived. Backtalk speaks the item immediately and lets the scroll finish while it speaks.

When the next item is off screen, Backtalk checks once per frame whether the list has moved, rather than wait for the scroll event, which Android sends at most every 100 ms. On a Samsung phone, swipes that scroll went from about 310 ms to about 60 ms when the item was partly on screen, and from about 320 ms to about 140 ms when the list had to scroll a page.

Backtalk also finds the end of a list that's laid out in reverse, such as a chat, correctly.

### Scroll views that show the next screen immediately

Some screens are one long page rather than a list, and keep all of their content in the accessibility tree. On these screens, when you swipe past the last item on screen, Backtalk scrolls straight to the next screen without the animation, so that the next item is at the top, and searches again immediately. An item that was cut off at the edge of the screen is read rather than scrolled past, as TalkBack's page scroll did.

This requires Android 14 or later. Lists, grids, pagers, and Compose screens still scroll a page at a time, because they create only the items on screen or they animate their scrolling.

### Focus that stays where a swipe's scroll put it

A list can keep scrolling for a moment after a swipe has moved focus to the next item. TalkBack treated the rest of that scroll as a scroll that you made, and moved focus on to the next item by itself. Backtalk counts it as part of its own scroll. As a result, if you scroll the same list yourself within a second of a swipe's scroll, focus doesn't move.

### Swiping away from long paragraphs

Some speech engines, such as Gryphon, stop only between the blocks of text that they synthesize, and treat a whole item as one block. When you swiped away from a long paragraph on a web page, the next item could wait up to 850 ms for the engine to stop.

To fix this, turn on **Text-to-speech** > **Send long text a sentence at a time**. Backtalk then sends text that's longer than 150 characters about a sentence at a time, so the engine stops immediately. In Chrome on a Samsung phone with Gryphon, web swipes went from as much as 900 ms to at most about 110 ms.

The setting is off by default, because many engines, such as RHVoice, already stop immediately, and they pause longer between sentences that are sent separately.

## Speech

### Text-to-speech settings

In TalkBack, **Text-to-speech** opens Android's system speech settings. Backtalk has its own **Text-to-speech** screen, under **Feedback** in Backtalk settings. On this screen, you can choose a speech engine for Backtalk that's separate from Android's default engine, and set the speech volume, rate, and pitch with sliders. It also links to Android's system settings, and holds the speech settings that are described in the following sections.

### Speech engine reading control

The **Speech engine** reading control switches between your installed speech engines when you swipe up or down. It changes the engine of **Backtalk default**, so it's unavailable while a voice profile is in use. It isn't in the reading controls by default. To add it, go to **Reading controls**.

### Voice profiles

A voice profile is a set of speech settings that you can switch to at once: a speech engine, language, voice, volume, rate, pitch, and **Send long text a sentence at a time**. For example, one profile can use a slower rate for long text while **Backtalk default** stays fast. To add one, go to **Text-to-speech** > **Voice profiles** and choose **Add voice profile**. When you choose a language, the voice list shows only the engine's voices in that language.

To switch profiles, swipe up or down with the **Voice profile** reading control, or choose **Voice profile in use** on the **Voice profiles** screen. The **Voice profile** reading control is separate from the **Speech engine** reading control. It isn't in the reading controls by default. To add it, go to **Reading controls**. You can also add a **Voice profile** item to the Backtalk menu in **Backtalk menu** settings, which lists the profiles and says which is in use. The **Choose voice profile**, **Previous voice profile** and **Next voice profile** actions can be assigned to gestures and keyboard shortcuts.

The reading control goes through the profiles in the order of the **Voice profiles** screen, after **Backtalk default**. To change the order, hold a profile and drag it, or use the **Move up** and **Move down** actions. Backtalk says which profile it moved above or below. Profiles also have **Rename** and **Delete** actions, which a long press shows too.

**Backtalk default** is always there, and uses the rest of the text-to-speech settings. While a profile is in use, changing the speech rate or pitch with gestures or the reading controls changes that profile only. All speech uses the profile's engine and voice, so Backtalk doesn't switch language or dialect, and the **Spoken language** reading control and menu item are hidden.

### Accessibility volume or media volume

TalkBack always plays speech at the accessibility volume. In Backtalk, you can turn off **Text-to-speech** > **Use accessibility volume**, so that speech and Backtalk's sounds use the media volume instead. The setting is on by default.

### Automatic language and dialect changes

When text is marked as another language, TalkBack switches the voice to that language. Backtalk lets you turn this off. In **Text-to-speech**, **Switch language automatically** controls changes to another language, and **Switch dialect automatically** controls changes to another country's form of the voice's language, such as US English text with a British English voice. Both are on by default.

When you turn a change off, Backtalk no longer splits the text or pauses around it. Text marked as the voice's own language and country, such as US English for a US English voice, isn't split either, whatever the settings, since the voice doesn't change. Backtalk compares languages by their codes only, so Serbian in Latin and in Cyrillic script counts as two dialects, and Norwegian marked as `no` and as `nb` counts as two languages.

### How emoji are spoken

Some speech engines read emoji badly, use names of their own, or don't read them at all. **Verbosity** > **Emoji** chooses how they're spoken:

*   **Read by speech engine**, the default, gives the engine the emoji as TalkBack did.
*   **Read by Backtalk** replaces each emoji with its name from the [Unicode CLDR](https://cldr.unicode.org/), in the language the text is spoken in, for 138 languages and regional variants. A skin tone, a family, a flag or a keycap is one emoji with one name.
*   **None** leaves emoji out, except in text that is only emoji, such as a reaction button or a character, which is still named.

With **Read by Backtalk**, **Count repeated emoji** says an emoji repeated in a row once with a count, such as "3 grinning face", from 2 to 6 repeats. Moving by character names a whole emoji once, and moving by word stops on emoji. Copying or spelling the last spoken phrase gives the emoji, not their names. You can also add **Emoji** to the reading controls and the Backtalk menu.

With **None**, moving by word skips emoji, as moving by character still names them. Emoji newer than Backtalk's names, Emoji 18.0, are left to the speech engine. Some languages lack CLDR names for some emoji, which are then spoken in English.

### Resume speech where you paused it

With speech engines that don't report word positions, such as RHVoice and Gryphon, TalkBack started the text over when you resumed speech. Backtalk learns how fast your engine speaks from the items that it finishes, works out how far the engine got when you paused, and resumes from the start of that sentence or phrase. It can repeat a few words, but it never skips any. Engines that report word positions still resume from the word, and with [low-latency audio](#low-latency-audio) on, speech resumes mid-word.

When speech resumes, Backtalk doesn't play the item's sound and vibration again.

### Two-finger tap right after a swipe

TalkBack didn't recognize a two-finger tap made right after a swipe, or it paused older speech instead of the current item. Backtalk recognizes the tap and pauses the speech for the item that you swiped to.

### Typing echo for words with apostrophes

TalkBack echoed a word such as "don't" as two words. Backtalk echoes it as one word. When a keyboard adds a word, its punctuation, and a space all at once, as the braille keyboard does in contracted braille, Backtalk echoes the word rather than just the space.

### Order of item details

**Verbosity** > **Order of item details** sets the order in which Backtalk says an item's name, type, and state. TalkBack has this setting in **Advanced settings** and offers three orders to choose from. Backtalk lists the name, type, and state, and you put them in any of the six orders: touch and hold a detail and drag it up or down, or use its **Move up** and **Move down** actions. With Backtalk on, double-tap and hold a detail, then drag it. Backtalk says the detail's position as you pick it up, move it, and drop it.

TalkBack ignored the order for a row whose checkbox or switch has no text of its own, such as the rows in **Reading controls**, and always read the checkbox's type and state before the row's text. Backtalk reads such a row as one control, with the row's text as its name, in the order that you chose.

### "Backtalk off" at the right volume

When you turn Backtalk off, it says "Backtalk off" at the accessibility volume, using your device's real volume levels. TalkBack only approximated that volume.

### Table column headers

In **Verbosity**, you can choose whether Backtalk reads table column headers before or after the cell's data, or leaves them out:

*   **After cell data**, the default, keeps TalkBack's order: the cell's contents, then the row and column headers or numbers.
*   **Before cell data** speaks the row and column headers or numbers before the cell's contents, along with "Column heading" or "Row heading" for a header cell. This applies to grids as well as tables, so an item in a photo grid says its row and column first. On a TV, Backtalk already speaks them first, so this option changes nothing there.
*   **Do not read** leaves column headers out, and reads column numbers only when row and column numbers are on.

Under preset settings in **Verbosity**, you can also turn off **Speak row and column numbers** to hear only named headers without row and column coordinates. The setting is on by default.

Both settings only apply while **Speak container info** is on, because rows and columns aren't spoken at all when it's off.

### Speak letters phonetically

TalkBack's **Speak letters with examples** switch is called **Speak letters phonetically** in Backtalk, in **Verbosity** under preset settings, and in on-screen keyboard settings. It has four choices:

*   **Off** says only the letter.
*   **After the letter, delayed**, the default, is what the switch did when it was on. Keyboard keys say the letter, then its phonetic word, as in "h, hotel". Moving by character, the word comes a quarter of a second after the letter, and moving on before then skips it.
*   **After the letter, no delay** says the letter and its word together, as in "h hotel".
*   **Instead of the letter** says only the word, as in "hotel".

With **After the letter, no delay** and **Instead of the letter**, Backtalk says it in all of these places. **After the letter, delayed** still ignores **Spell last spoken phrase**, deleting a letter, and inserting a letter with a touch keyboard like Gboard, just as TalkBack did when **Speak letters with examples** was turned on.

The **High** preset uses **After the letter, delayed** and the **Low** preset uses **Off**. Your choice from the switch carries over.

## Notifications

### Samsung watch notification content

On Samsung watches, Backtalk reads the app and title, then the message content, then the time when you focus a notification card. Samsung's card label can omit the message even though it is available in the accessibility tree. Backtalk includes that text automatically, without an extra gesture or setting, and does not announce the card's display font formatting.

### Speak notifications setting

To stop Backtalk from reading new notifications when they arrive, turn off **Verbosity** > **Speak notifications**. Backtalk still reads incoming calls, and you can still read notifications in the notification shade. The setting is on by default.

### Toasts in Do Not Disturb

While Do Not Disturb was on, TalkBack didn't speak toasts, the short messages that apps show at the bottom of the screen. Backtalk speaks toasts, and stays silent only for notifications.

### No "collapsed" on each notification

Backtalk doesn't say "collapsed" for each notification on the lock screen and in the notification shade. It still says "expanded" when you open a notification, and it still says "collapsed" in other apps.

### Notices that you can dismiss

TalkBack marked its notices that it was updated, or that gestures changed, as ongoing, so you couldn't swipe them away or dismiss them. In Backtalk, you can dismiss these notices, including on the lock screen. The notifications for pausing Backtalk and for downloading an on-device model stay ongoing while those continue.

## Sound and vibration

### Audio output device

**Sound and vibration** > **Audio output device** chooses where Backtalk's speech and sounds play. **System default** follows Android. **Phone speaker** plays on the phone's speaker even when Bluetooth is connected, and you can also choose a connected Bluetooth, wired, USB, or hearing aid device by name.

Backtalk keeps your choice when devices connect or disconnect, or when other apps start playing. If the device that you chose isn't connected, Backtalk uses another external device, and the setting shows "(disconnected)" next to the device name. During phone calls, Android decides where audio plays.

### A vibration for every sound

Like VoiceOver, Backtalk can tell you what happened by touch alone. Every sound has its own vibration, which plays even when **Sound feedback** is off, so you can turn all sounds off and still feel each action. No two actions share a vibration.

Frequent actions are a single hit: a soft tap when focus lands on an item, a firm click on an item that you can activate, and a softer tap over an empty area. Others have a rhythm of their own. The end of a list, or a completed action, is two firm knocks. Entering a list is two light taps with the second one stronger, and leaving a list is two light taps with the second one weaker. A window change is three soft taps with the middle one strongest. When Backtalk scrolls a list, you feel three quick ticks. When the circle menu opens, you feel one tick for each item. The loading tone, which repeats while you wait for an image or screen description, has a short heartbeat that's firm enough to feel through a case.

Progress bar tones, the sound for capital letters, and braille display and braille keyboard sounds don't vibrate. The braille keyboard has vibrations of its own, which **Vibration feedback** turns on and off with the rest. Phones that support rich haptics play the vibrations as composed effects. Phones without them play softer and shorter forms of the same patterns.

### Ticks while you scroll with two fingers

TalkBack played its scroll sound and vibration at most every 250 ms while you scrolled. Backtalk plays the scroll sound and one light tick for each item that scrolls past, so a slow drag ticks slowly and a fast drag ticks quickly. Lists that don't number their items tick once for every 48 dp of scrolling. You feel the first tick as soon as the drag reaches the app, and Backtalk hands the drag to the app sooner than TalkBack did. Scrolls that Backtalk makes itself keep the usual sound.

### Individual sounds and vibrations

To turn off one sound or one vibration and keep the rest, go to **Sound and vibration** > **Individual sounds and vibrations**. There's a switch for each sound and each vibration, including the braille display and braille keyboard sounds, the braille keyboard's vibrations, and the screen-off sound. Turning off a sound keeps its vibration, and turning off a vibration keeps its sound. To hear or feel one, open the actions menu on its switch and choose **Preview**. **Sound feedback** and **Vibration feedback** still turn all of them off at once.

### Sound themes

A sound theme is a set of sounds and vibrations that replace Backtalk's own, with its own control sounds and 3D audio settings. You can install as many themes as you like, from a file or a link, and switch between them in **Sound and vibration** > **Sound themes**. A link to a GitHub repository that contains a theme also works, and you can open or share a theme file, or a link to one, with Backtalk from another app. On Wear OS watches, you can install themes only from a link.

To replace single sounds with your own, open **Sounds** under the theme that you're using. To share your sounds, save the theme as a file. For more information about installing, using, and making themes, see [Sound themes](themes.md).

### Control sounds and vibrations

Like the [Unspoken](https://github.com/ahicks92/Unspoken) add-on for NVDA, Backtalk can play a sound for each kind of element in place of the focus sound, and leave out words such as "button" and "checkbox". There are sounds and vibrations for buttons, checkboxes (which also cover switches and toggle buttons), radio buttons, edit fields, drop-down lists, sliders, images, clocks, tabs, menu items, and list items, and on web pages, for links and tree items. To turn them on, go to **Sound and vibration** > **Sound themes** and turn on **Control sounds and vibrations**.

Backtalk has no control sounds or vibrations of its own: they come from the sound theme that you're using. Until a kind of element has a sound or a vibration, it plays the focus sound and Backtalk says what kind of element it is. An element that has a vibration but no sound plays the focus sound with its own vibration. A control's vibration plays even with sound feedback off, so you can learn the kinds of controls by touch alone.

A row that isn't a control itself, such as a row in Settings, plays the sound of the control in it, such as its switch, and otherwise the sound for its kind of item, such as a list item. Plain text and keys on the keyboard keep the usual focus sound. Moving into or out of a list plays the list sound and the control's sound together, with the control's vibration.

To keep hearing the kind of control as well, turn on **Still say the kind of control** in **Sound and vibration**. Each control sound and vibration also has its own switch in **Individual sounds and vibrations**.

### 3D audio

Each control sound comes from where the element is on the screen: from the left for elements on the left, and from higher up for elements near the top. With headphones, the sounds play in 3D, using measurements of how a real head hears sound from each direction (the MIT Media Lab KEMAR measurements, by Bill Gardner and Keith Martin). On the phone speaker, the sounds only move between left and right.

On Android 13 and later, Backtalk uses 3D when its audio plays on wired or USB headphones, a hearing aid, a Bluetooth media device, or a Bluetooth LE headset. Bluetooth LE speakers, broadcasts, and call audio don't count. On earlier versions, any connected audio device counts. To play the sounds in 3D on the speaker as well, or never, change **3D audio** for the theme. Each theme has its own **Control sounds and vibrations** and **3D audio** settings.

## Gestures

### Default gestures

Backtalk changes these default gestures:

| Gesture | Backtalk | TalkBack |
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

Backtalk recognizes the quadruple-tap with three fingers and the rotation gestures itself, so they require Android 13 or later and **Handle gestures in Backtalk** turned on. You can assign any of these gestures to another action in gesture settings. Wear OS watches have defaults of their own; see [Wear OS watches](#wear-os-watches).

### Navigation gestures

You can assign a gesture to move to the next or previous character, word, line, paragraph, heading, link, control, landmark, button, checkbox, radio button, edit field, combo box, focusable item, graphic, list, list item, table, visited link, unvisited link, or heading of a given level. Moving with one of these gestures doesn't change the reading control. When you choose an action for a gesture, these actions are under **Navigate by text**, **Navigate by element**, and **Navigate by heading level**.

Headings, links, and controls work in apps and on web pages. The other elements work only on web pages.

### Status gesture

Triple-tap with two fingers to hear what the status bar shows: the time, the battery, the Wi-Fi network and its signal, and the mobile carrier, network type, and signal. Backtalk also says the ringer, Do Not Disturb, and airplane mode when they aren't in their usual state. To hear the name of the Wi-Fi network, allow location access when Backtalk asks the first time.

To choose what the gesture says and in what order, go to **Status readout**. Each item has **Move up** and **Move down** actions. In gesture settings, this action is called **Speak status**. To read from the current item, which was the gesture's action in TalkBack, choose **Read from focused item** in the Backtalk menu.

### Rotor

Turn two fingers on the screen like a dial to choose a reading control: clockwise for the next one, and counterclockwise for the previous one. Then swipe up or down to change it.

Each step of 30 degrees, a twelfth of a turn, moves one reading control, so you can keep turning to move further. A quick flick moves exactly one step. If you often move past the reading control that you want, choose a larger step, from 15 to 60 degrees, in **Rotor turn per step** in gesture settings. Scrolling with two fingers doesn't turn the rotor.

The rotor requires Android 13 or later and **Handle gestures in Backtalk** turned on. You can assign other actions to **Rotate clockwise with 2 fingers** and **Rotate counterclockwise with 2 fingers** in gesture settings.

### Lift to activate

Backtalk can activate the item under your finger when you lift your finger after exploring by touch, as TalkBack already does for keys on the keyboard. In **Advanced settings** > **Lift to activate**, choose **Only on navigation bar** to use it for the Back, Home, and Overview buttons only, or **Entire screen** to use it everywhere. It's **Disabled** by default. With **Only on navigation bar**, a single tap presses the button, and Backtalk says which one, such as "Back" or "Home". To stop this, turn off **Advanced settings** > **Speak navigation bar buttons**.

With **Only on navigation bar** on Android 11 and later, touches on the navigation bar go straight to Android, so a single tap presses a button and holding Home holds it, as without a screen reader. Backtalk doesn't say the buttons as you touch them.

**Lift to activate** is also a reading control, so you can change it with a swipe up or down.

### Wrap around

When you swipe past the last item on the screen, TalkBack stops once and then goes back to the first item. To stop at the end instead, turn off **Advanced settings** > **Wrap around**. Each swipe past the end then plays the end sound. This also applies to moving by heading, link, or control in apps, and to moving with a keyboard. Web pages can still wrap by themselves.

**Wrap around** is also a reading control, so you can turn it on or off with a swipe up or down. It's on by default.

### Focus in new windows

When a window opens, or you go back, TalkBack moves focus into it: back to the item you were on, to the text field with the cursor, or to the first item. To leave focus alone, turn off **Advanced settings** > **Move focus to new windows**. Backtalk then speaks only the window name, and the next swipe moves to the first item. This applies to Backtalk's own menus and dialogs too, when they open and when they close. Focus follows input focus only when you move it with a key, such as an arrow key or Tab, not when an app moves it by itself. When the item you focused disappears and the windows haven't changed, focus is placed again. The switch isn't on TVs, which need focus to follow input focus.

### Gestures recognized by Backtalk

By default, Backtalk recognizes gestures itself instead of leaving this to Android, which makes the rotor and the quadruple-tap with three fingers possible. TalkBack leaves this off.

To let Android recognize gestures again, turn off **Advanced settings** > **Developer settings** > **Handle gestures in Backtalk**, and then turn Backtalk off and on again. When this setting is off, the rotor and the quadruple-tap with three fingers don't work, and **Time between one-finger taps** is hidden.

## Navigation

### Reading place kept when an item disappears

When an app removed the item that had focus, such as an ad that collapses, TalkBack jumped back to the top of the page. Backtalk carries on from where you were.

### Double tap with the actions reading control on Android 11

On Android 11, when you picked an action with the **Actions** reading control, a double tap only played a sound. In Backtalk, a double tap performs the action, and a double-tap and hold performs a long press.

### Sliders that report themselves as unfocusable

realme UI reports some volume sliders as not focusable and unlabeled, so TalkBack skipped them. Backtalk can focus them.

## Backtalk menu

### Shorter menu by default

These items are off by default in the Backtalk menu: **Actions**, **Screen search**, **Add or edit labels**, **Describe text formatting**, **Copy last spoken phrase**, **Spoken language**, **Voice commands**, **Keyboard shortcuts**, and **Braille display settings**. You can still use actions with the **Actions** reading control. **Text-to-speech** stays on, so that you can reach speech settings if your speech engine crashes, and Backtalk adds a **Pause Backtalk** item.

To change the items, go to **Backtalk menu**, under **Controls** in Backtalk settings. In TalkBack, this screen is under **Customize menus**.

### Circle menu

The Backtalk menu can show as a circle in the middle of the screen, as it did in TalkBack 8.1 and earlier. Each item is a slice of the screen around the middle. Slide to an item and lift your finger to select it, or lift in the middle of the circle to close the menu. Items that open more items show them in a new circle. When the menu opens, you hear a rising note and feel a tick for each item.

If you open the menu with a gesture that ends with your fingers still on the screen, such as a double-tap and hold, you can slide those fingers out to an item and lift them to select it. The place where your fingers were counts as the middle of the circle.

To turn the circle menu on, go to **Backtalk menu** and turn on **Circle menu**. It's off by default.

## Screen and brightness

### Brightness reading control

Use the **Brightness** reading control to change the screen brightness in steps of about 10% when you swipe up or down. It works while the screen is hidden, so you can set the brightness before you give the phone to someone. The first time that you use it, Backtalk asks for the **Modify system settings** permission.

### Hide screen

For 3 minutes after you hid the screen, TalkBack set the screen to full brightness. Backtalk keeps your brightness. The screen also turns fully black, including around the camera cutout. On some Pixel phones with Android 17, TalkBack's curtain only dimmed the screen.

If you turn off **Always show this** in the hide screen dialog, Backtalk says only "Screen hidden" and skips the instructions for showing the screen again. When Backtalk starts with the screen hidden, it says "Screen hidden" after "Backtalk on".

### Proximity sensor

Backtalk doesn't stop speech when something covers the proximity sensor. To turn this back on, go to **Sound and vibration** > **Cover proximity sensor to stop speech**.

### Screen on and off announcements

In **Verbosity** > **Screen on and off**, you can choose what Backtalk says when the screen turns off, when it turns on, and when you unlock the phone. When the screen turns off, Backtalk says "Screen off" and the ringer mode. When the screen turns on, it says the time, and it can also say the battery, the Wi-Fi network, the mobile network, the ringer and Do Not Disturb, and airplane mode, after the time in the same announcement. When you unlock the phone, it says "Device unlocked". Each of these has its own switch. By default, everything except the extra screen-on items is on.

These switches are separate from **Status readout**, so the status gesture can say different things. Backtalk doesn't make the screen-on announcement during calls, and the lock screen no longer cuts off "Screen off".

## Pausing Backtalk

### Pause Backtalk

Pausing turns off Backtalk's speech, sounds, vibration, gestures, and explore by touch, so the phone works as if no screen reader were on. Backtalk stays on, so it resumes immediately. TalkBack 8.1 and earlier had this feature as suspend.

To pause, choose **Pause Backtalk** in the Backtalk menu, assign the **Pause Backtalk** action to a gesture, or assign a key to the **Pause or resume Backtalk** keyboard shortcut. The first time, Backtalk asks you to confirm and says how to resume.

To resume, tap the **Backtalk is paused** notification, press volume down three times quickly, or press the keyboard shortcut again. By default, Backtalk also resumes when the lock screen shows. To change this, go to **Advanced settings** > **Resume Backtalk** and choose **When the screen turns on** or **Only from the notification or a shortcut**.

The volume keys still change the volume while Backtalk is paused. If Backtalk restarts while it's paused, it starts unpaused.

## Calls

### Speaker when away from your ear

Like VoiceOver on iPhone, Backtalk can move a call to the speaker when you take the phone away from your ear, and back to the earpiece when you hold it up again. A call that starts with the phone away from your ear goes straight to the speaker. Backtalk leaves Bluetooth and wired headsets alone, and if you turn the speaker on or off in the Phone app, your choice stays until you move the phone again.

Android lets only certain apps, such as smartwatch companions, change where call audio goes, so you need to grant Backtalk permission once, with adb or [Shizuku](https://shizuku.rikka.app):

```
adb shell appops set fyi.quin.backtalk MANAGE_ONGOING_CALLS allow
```

Then turn on **Sound and vibration** > **Speaker when away from your ear**. Until you grant the permission, the setting is unavailable and shows the command, and its **Copy command** action copies the command to the clipboard. This feature requires Android 12 or later.

## Direct touch

Audio games need raw touch, but explore by touch captures taps and swipes before the game sees them. With direct touch, Backtalk passes your touches straight to the games that you choose, so you don't have to pause Backtalk.

### Built in from NVGT Bridge

Direct touch is all of [NVGT Bridge](https://github.com/trypsynth/nvgt-bridge), the accessibility service that gives audio games direct touch, built into Backtalk. Because Backtalk is the screen reader and already knows what's on the screen, direct touch works better than running NVGT Bridge alongside Backtalk.

There's nothing extra to install or turn on in Accessibility settings, and no second service that can undo Backtalk's touch setting. Direct touch shares that setting with the pass-through gesture and the braille keyboard, so none of them cancels another. NVGT Bridge searches each window's views for dialogs and text fields, and looks only five levels deep. Backtalk already follows the windows, the keyboard, and the screen state, so it hands touch back to you as soon as they change, with no depth limit. Backtalk's own speech says "Direct touch on" and "Direct touch off", even while the game plays audio.

### Choose your games

Go to **Direct touch**, under **Controls** in Backtalk settings, and turn on each game in the **Apps** list. When a game that you chose comes to the front, Backtalk says "Direct touch on", and when the game gives touch back, it says "Direct touch off". To turn the announcements off, turn off **Speak when direct touch changes**. To feel a short vibration instead or as well, turn on **Vibrate when direct touch changes**. The **Direct touch** switch at the top of the screen turns the whole feature on or off, and is on by default.

### When Backtalk takes touch back

Touch returns to Backtalk when a dialog appears, another app opens on top of the game, you open the notification shade or quick settings, a text field takes focus, or the screen turns off. Touch goes back to the game when they're gone. The keyboard always stays with Backtalk, so you can explore it while the rest of the screen stays in direct touch.

### Direct typing

By default, the keyboard area keeps working with Backtalk. For a game that draws its own keyboard, open the actions menu on the game in the **Apps** list and choose **Turn on direct typing**.

### Self-voicing keyboards

A keyboard that speaks for itself, such as Advanced Braille Keyboard, needs your touches in every app, not only in games. Turn it on in the **Apps** list, and while it shows, touches on it go straight to it in any app, and the rest of the screen stays with Backtalk.

### Quick settings tile

To pause and resume direct touch without leaving your game, add the **Direct touch** tile to quick settings.

### Back up and restore direct touch settings

**Back up settings** saves your direct touch choices to a file, and **Restore settings** loads them on another device.

### Turn on direct touch from your game

If you develop a game, add this `<meta-data>` element inside your `<application>` element or your main `<activity>` element, and Backtalk turns your game on the first time that it sees it. Players can still turn it off.

```xml
<meta-data
    android:name="dev.nvgt.capability.DIRECT_TOUCH"
    android:value="true" />
```

## On-screen keyboard

### Lift to type, except to send

In **On-screen keyboard** > **Typing method**, **Hold finger to select any key, then lift. Double-tap for Enter, Done, or Send.** types every key when you lift your finger, like **Hold finger to select any key, then lift**, but the key that sends or submits, such as Enter, Done, Send, Search, or Go, still needs a double-tap. This keeps you from sending a message by lifting your finger on the wrong key. Backtalk recognizes this key in Gboard. In other keyboards, every key types when you lift your finger.

## Braille keyboard

### Keyboard echo for the braille keyboard

In TalkBack, the braille keyboard follows the on-screen keyboard's echo setting. Backtalk gives the braille keyboard its own **Keyboard echo** setting in braille keyboard settings, with **None**, **Characters**, **Words**, and **Characters and words**. The capital and number signs follow it too. The first time that Backtalk starts, it copies your on-screen keyboard echo setting, and after that the two are separate. The setting doesn't apply to braille displays.

### Typing sounds

In braille keyboard settings, **Typing sounds** plays Android's keyboard clicks as you type: a click for each character, and the space, delete, and return sounds for those actions. It's off by default, and it works even when touch sounds are off in Android's settings. A [sound theme](themes.md) can replace these sounds with its own.

### Swap top and bottom dots

In braille keyboard settings, **Swap top and bottom dots** makes dot 1 trade places with dot 3, and dot 4 with dot 6. TalkBack's **Reverse dots** is called **Swap left and right dots** in Backtalk. You can turn on both.

### Skip the tutorial

The first page of the braille keyboard tutorial has a **Skip tutorial** button, which opens the keyboard immediately.

### Haptics

The braille keyboard uses the same crisp vibration effects as the rest of Backtalk. Submitting text feels the same as closing or switching the keyboard. Deleting in an empty field gives a soft vibration that fades out, so that you know that there was nothing to delete.

### Navigation that stays in the text field

If you swiped to another control while the braille keyboard opened, TalkBack moved by character, word, or line in that control instead of in the text field. Backtalk moves focus back to the text field before each command.

### Dots that follow how you hold the device

The braille keyboard keeps the dots under your fingers whichever way round you hold the device, with auto-rotate on or off. TalkBack expected the charging port on one side when auto-rotate was off, so holding the phone the other way round typed dot 4 for dot 1.

Backtalk remembers how you last held the device up, the whole time that the screen is on, as auto-rotate does. In screen-away mode, the dots follow how you hold the device. In tabletop mode, laying the device flat keeps the side that you held it up with, even from before the keyboard opened, and turning it on the table turns the dots with it. The keyboard says where the charging port is when the mode changes and after each turn, such as "Tabletop mode, charging port on the left". On a phone, the dots turn when the port moves to your other side, because you type with the port on your left or right; a quarter turn in between says "Charging port toward you" or "Charging port away from you". On a tablet or an unfolded foldable, they turn by quarter turns, including upside down, which auto-rotate doesn't do.

#### Lock the orientation

To keep the dots facing the way they are, hold dots 4, 5, and 6 and swipe up. To let them follow the device again, do the same again. There's a lock for each screen size and mode, so you can lock a foldable one way when it's folded and another way when it's unfolded.

#### Tablets held up facing you

Gravity can't tell whether a device that's held up faces you or faces away. When you lay a phone flat after holding it up, Backtalk assumes that it faced you, as auto-rotate does, unless you typed on it screen away. The side you typed with screen away is kept, and turns with the phone on the table, until you type on the table. Typing only counts as screen away if you held the phone upright, so tilting a phone that you hold nearly flat in your hands doesn't count. A phone last held in portrait says "Tabletop mode, charging port toward you", and turning it to your left or right on the table puts the port on that side. A tablet or an unfolded foldable that's held in portrait is assumed to have faced you, and one that's held sideways is assumed to have faced away, as when typing screen away.

If you hold or stand your tablet facing you instead, turn off **Tablet held up faces away** in braille keyboard settings. A tablet held up any way round then uses the tabletop layout facing you, with your thumbs holding the bottom edge, and the keyboard says, for example, "Screen toward you, charging port on the left". The dots stay the same way round when you lay the tablet flat. You can also add **Tablet held up faces away** to the reading controls. With the braille keyboard open, swipe right or left with three fingers to reach it, as you would for words or lines, and then swipe down or up with one finger.

#### Limitations

A phone that's opened flat, and that hasn't been held up since the screen came on, follows the screen rotation, and with auto-rotate off expects the port on the left; turning it around on the table, or locking the orientation, corrects this. A phone held up screen away and laid flat without typing, or typed on screen away without ever being held within 40 degrees of upright, is taken to have faced you, so it expects the port on the wrong side; locking the orientation corrects this. Turns on the table require the game rotation vector sensor; devices without it still follow how they're held up. A tablet says where the port is only if it's a portrait tablet with the port at the bottom. Remembering how the device is held keeps the accelerometer running while the screen is on, as auto-rotate does.

## Braille displays

### Any braille display that uses HID

On Android 15 and later, TalkBack connects to braille displays that use the HID braille standard over Bluetooth, but only lists displays whose names it knows, so newer displays and braille terminals were left out. Backtalk lists any Bluetooth device that offers HID, whatever its name, and connecting checks that it's a braille display, so other HID devices, such as keyboards, can appear in the list too. TalkBack also refused displays that send their keys and receive their cells in separate HID reports, as the standard allows; Backtalk connects to those.

### Displays from other apps

Apps can give Backtalk a braille display that it can't connect to on its own, such as the built-in keys and cells of a braille notetaker. Backtalk uses it like a display connected through Bluetooth or USB: its keys navigate and type with the same commands, and its cells show what Backtalk shows. A display with keys and no cells works too.

Backtalk only uses a display from an app once you allow it, after a warning that the app will be able to read the screen and control the device. Backtalk asks when it finds such an app, and **Displays from other apps** in braille display settings lists them, to allow or stop allowing each one. A notification says which app is in use. A display connected through Bluetooth or USB comes first, and one from an app waits while it's connected. [Providing a braille display from another app](docs/braille-display-api.md) describes the API for app developers.

## Image and screen descriptions

### Gemini API key

Google's source release doesn't include working Gemini support, so without an API key, **Describe image** only reads text in images and **Describe screen** doesn't work. Backtalk adds **Automatic descriptions** > **Gemini API key**, where you can add your own key, and its own Describe screen with follow-up questions. For more information, see [Image descriptions with Gemini](README.md#image-descriptions-with-gemini).

### On-device AI

Backtalk can describe images and screens with a model that runs on your phone, so nothing is sent to Google or anyone else. For more information, see [On-device AI](README.md#on-device-ai).

### Clear error messages

When Gemini can't describe something, TalkBack says "Something went wrong". Backtalk says what failed: the key wasn't accepted, the usage limit was reached, Gemini is busy, Gemini took too long, or there's no internet connection. For other errors, Backtalk says the error code and the first sentence of Google's message, in English. Describe screen speaks its errors instead of showing them in a toast.

## Scripts

### JavaScript scripts and plugins

Like NVDA's app modules and global plugins, scripts change how Backtalk works in one app or in every app. A script can:

*   Give unlabeled buttons names, hide clutter, group items, and change or silence what Backtalk says for an item.
*   Read the screen, press buttons, speak, play sounds and vibrate.
*   Add gestures, keyboard shortcuts, Backtalk menu items, reading controls, and items in an item's Actions menu.
*   Change everything Backtalk says, and react to notifications, typing and screen changes.
*   Use the internet and the clipboard, show dialogs, and keep its own data and settings.

To install a script, go to **Advanced settings** > **Scripts** and choose **Import script**. You can also share a script file, or a link to one such as a script's page on GitHub, to Backtalk, or open a script file with it, and choose **Install Backtalk script**. Backtalk lists what the script asks to do before it installs or updates it. **Add the example script** adds a small script to start from.

Each script has a page with an on and off switch, a switch for each permission and for each gesture, key, menu item and reading control it adds, its settings on their own screen, its log, and **Remove**. While a script runs, its gestures and keys come before Backtalk's.

Scripts never change Backtalk's own screens, so the Scripts screen always works. **Turn off all scripts** on that screen stops every script at once, and so does the **Turn all scripts on or off** action, which you can assign to a gesture that scripts can't take over. Scripts don't run while the screen is off or Backtalk is paused, and Backtalk only starts the script engine once a script is turned on.

Speech requested with `backtalk.speak` can be heard while media is playing, including queued and interrupting requests. The script still needs the Speech permission, and muting speech, recording, voice recognition and phone calls retain their existing speech policy.

Hooks that change or suppress speech take priority over queued script work and share a 30-millisecond deadline. If the deadline expires or a long task is already running, Backtalk uses its normal speech, so scripts should keep these hooks fast.

Scripts run on phones and tablets only. [Writing Backtalk scripts](docs/scripting.md) describes the whole API, and [docs/scripting/examples](docs/scripting/examples) has a script to start from.

### Speech from other apps

Apps can ask Backtalk to say text in your own Backtalk voice, for example a game, an automation app such as Tasker, or a remote-access tool. To allow this, turn on **Advanced settings** > **Let other apps speak**. It's off by default.

The first time an app asks, Backtalk posts a notification that asks whether to allow it, and nothing the app sends is said until you choose **Allow**. You can allow or block each app later in **Let other apps speak**. To hear a short sound before each thing another app says, so that you can tell it apart from Backtalk, turn on **Play a sound before other apps speak**. Nothing is said while the device is locked, an app can speak only a few times in a row, and apps can never stop or cut off Backtalk's own speech. [Speaking through Backtalk from another app](docs/speech-api.md) describes the API for app developers.

### Inspect focused item and Copy screen tree

**Inspect focused item** says the focused item's id, class, role, app, activity and window, and copies them, so that scripts can name the item. **Copy screen tree** copies every item on screen as JSON. Assign either to a gesture in gesture settings, or add it to the Backtalk menu in **Customize menus** > **Backtalk menu**.

## Settings

### Layout and wording

Backtalk groups its main settings under **Feedback**, **Controls**, **Typing and braille**, and **More**, and puts **Backtalk menu** and **Reading controls** in the main settings. Many settings have clearer names, such as **Speak item type** and **Order of item details**. Backtalk removes the links to the Play Store, the privacy policy, the terms of service, Disability Support, and Google's TalkBack help, and the pages about new features in TalkBack. **Display speech output** is only in **Developer settings**. **Time format** is in **Verbosity**, **Turn off animations** is in **Reduce delay**, and **Cover proximity sensor to stop speech** is in **Sound and vibration**, instead of in **Advanced settings**.

### Dark mode

On Android 12 and later, Backtalk's settings follow the system dark mode.

### Slider titles

Backtalk doesn't stop on a slider's title as a separate item when you swipe through settings.

## Updates and installing

### Separate app

Backtalk installs as `fyi.quin.backtalk`, so it installs next to Google's TalkBack, with settings of its own. For more information, see [Install](README.md#install).

### Built-in updates

Backtalk checks GitHub for new development builds and installs them when you tap its notification. For more information, see [Updates](README.md#updates).

## Android TV

### Settings on the TV home screen

Android TV has no other way to open an accessibility service's settings, so Backtalk adds **Backtalk settings** to the apps on the TV home screen.

### Phone-only features hidden

Settings for features that need a touchscreen or a phone are hidden on TVs, such as **Direct touch**, **Status readout**, and **Speaker when away from your ear**. Pause Backtalk also isn't available on TVs, because the volume keys often go to the TV or a soundbar, and there's no notification shade to resume from.

## Wear OS watches

### Same settings as phones

Watches show the same Backtalk settings as phones, including **Text-to-speech**, all of **Verbosity**, **Individual sounds and vibrations**, **Lift to activate**, **Turn off animations**, **Status readout**, the rotor, the reading controls, the delay settings, update checks, and the circle menu, which is scaled to fit the screen. Settings for features that watches don't support are hidden, such as the braille keyboard, keyboard shortcuts, voice commands, direct touch, on-device AI, **Speaker when away from your ear**, and **Resume Backtalk**.

### Gestures on watches

Watch screens track only two fingers, so gestures with three or four fingers don't work, and two-finger swipes scroll the app. Watches use the phone's default gestures except for the following:

| Gesture | Action on watches |
|---|---|
| Double-tap and hold with two fingers | Open the Backtalk menu |
| Triple-tap and hold with two fingers | Turn speech on or off |
| Swipe left then up | Repeat last spoken phrase |

On Samsung watches, turn on **Advanced settings** > **Reserve gestures for Vibration Watch** to let Samsung's Vibration Watch use two-finger single and double taps. The switch is off by default, and Vibration Watch must also be enabled in the watch's accessibility settings. While the switch is on, these gestures no longer perform their Backtalk actions: by default, a two-finger tap pauses or resumes speech, and a two-finger double tap controls media or starts voice input. Saved gesture assignments are kept and become available again when you turn the switch off.

### Backtalk menu on watches

The Backtalk menu scrolls on watches, so you can reach every item. **Describe screen** is off in the watch menu by default, and **Pause Backtalk** isn't available on watches.
