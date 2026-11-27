# Differences from TalkBack

SandFly is built from Backtalk, which is built from the TalkBack source releases from Google. This document lists how SandFly differs from TalkBack: features that TalkBack doesn't have, defaults that SandFly changes, and problems in TalkBack that SandFly fixes. To install SandFly, see the [README](README.md).

Settings paths start at the main SandFly settings screen, such as **Touch** > **Focus delay**. Where a screen has groups, the path can include the group, such as **Speech** > **Stopping speech** > **Interrupt speech when typing**. Each section links to the chapter of the [SandFly User Guide](docs/user-guide/en) that describes the feature in full. The user guide also opens from **Help & feedback** > **User guide**.

## Main differences

- **Speed**: SandFly speaks sooner after swipes, double taps and screen changes, prepares the next swipe in advance, scrolls lists ahead of focus, and plays speech and sounds through a low-latency audio path. See [Speed](#speed).
- **Gestures**: SandFly adds a two-finger rotor, edge gestures and edge menus, a two-finger scrub, corner gestures, swipe and hold, three-part and two-stroke swipes, tap then swipe, pinch and spread, back tap, shake, VoiceOver and BlackBerry gesture layouts, gestures for single apps and gesture profiles. See [Gestures](#gestures) and [Defaults that differ from TalkBack](#defaults-that-differ-from-talkback).
- **Navigation**: wrap around can be turned off, element navigation works in apps as on web pages, and SandFly adds sentences, vertical navigation, and addresses and numbers in text. See [Navigation](#navigation) and [Web pages](#web-pages).
- **Commands and menus**: every command can be put in the SandFly menu, in custom menus, edge menus and custom reading controls. The SandFly menu can show as a circle. See [Commands and menus](#commands-and-menus).
- **Speech**: SandFly has its own speech engine setting, voice profiles, voices by language, voice effects, a speech dictionary, a speech history, and settings for emoji, phonetic letters and apps that speak by themselves. See [Speech](#speech) and [Verbosity](#verbosity).
- **Sound and vibration**: every sound has a vibration of its own, sound themes replace the sounds, and control sounds play in 3D audio. Speech and sounds can play on a chosen output device and at another volume. See [Sound and vibration](#sound-and-vibration) and [Audio output and calls](#audio-output-and-calls).
- **Notifications**: SandFly has settings for which notifications are read, what is read and in which voice, its own notification list, and its own quick settings panel and overview of recent apps. See [Notifications](#notifications) and [Display, overview and quick settings](#display-overview-and-quick-settings).
- **Typing and keyboards**: SandFly adds typing methods, a clipboard history, granular editing, dictation with several speech recognizers, a handwriting keyboard, keymaps from VoiceOver, NVDA, JAWS and Orca, a BlackBerry mode, support for keyboard phones and an input method for physical keyboards. See [Typing](#typing) and [Physical keyboards](#physical-keyboards).
- **Braille**: braille keyboard dots follow how you hold the device, braille commands run commands from braille cells, and braille displays connect over HID, from other apps, with keys that you choose. See [Braille keyboard](#braille-keyboard) and [Braille displays](#braille-displays).
- **Automation**: voice commands with chains and AI, macros, Lua scripts and a script builder, and an API for other apps to speak through SandFly. See [Voice commands](#voice-commands), [Macros](#macros) and [Scripts and other apps](#scripts-and-other-apps).
- **Direct touch**: audio games and other apps that you choose get touches directly while SandFly speaks. See [Direct touch](#direct-touch).
- **AI**: image and screen descriptions from many AI services or a model on the phone, Live recognition, translation, and the SandFly Pro add-on. See [Image and screen descriptions](#image-and-screen-descriptions), [SandFly Pro](#sandfly-pro) and [Translation](#translation).
- **Setup**: an interactive tutorial, settings search, settings profiles, backups, built-in updates, Pause SandFly and permissions through Shizuku or root. See [Tutorial](#tutorial), [Settings](#settings), [Pause and turn off](#pause-and-turn-off) and [Updates and installation](#updates-and-installation).

## Defaults that differ from TalkBack

| Setting | SandFly | TalkBack |
|---|---|---|
| **Touch** > **Focus delay** | 0.15 seconds | 0.3 seconds |
| **Display** > **Turn off animations** | On, and turns animations off on Android 13 and later | **Reduce window announcement delay** on, which doesn't turn animations off |
| **Verbosity** > **Window names** | **Always, cut off by the first item** | **Speak window names** on, which is **Always** |
| **Verbosity** > **Speak text formatting** | Off | On |
| **Speech** > **Stopping speech** > **Cover proximity sensor to stop speech** | Off | On |
| SandFly menu items | **Actions**, **Screen search**, **Add or edit labels**, **Describe text formatting**, **Copy last spoken phrase**, **Spoken language**, **Voice commands**, **Keyboard shortcuts** and **Braille display settings** off; **Pause SandFly**, **Turn off SandFly**, **Send feedback**, **Split screen**, **Gesture profile** and **Gestures for this app** added | The nine items on; the added items not available |
| Gestures | See [Default gestures](#default-gestures) | |

## Speed

For the settings in this section, see the user guide chapters [Explore and read the screen](docs/user-guide/en/02-reading.md), [Touch](docs/user-guide/en/04-touch.md) and [Sound, vibration and audio output](docs/user-guide/en/08-sound-and-vibration.md).

### Faster speech after a swipe

When you swipe to an item, TalkBack waits for the app to confirm the new focus before it builds the text to speak. SandFly speaks the item as soon as it sets focus, and does less other work around each swipe. On a Galaxy Z Fold7, the median time from lifting your finger to the start of speech went from about 68 ms to about 39 ms.

When SandFly has the text of the item before it moves focus, it speaks the item first and then sends the focus request to the app. The focus request waits for the app, which can take long in a busy app, and speech plays during that wait. If the app doesn't move focus, the speech stops, and for 2 seconds that item is spoken only after focus has moved.

This doesn't apply when focus moves to another window, when the screen changes, or during continuous reading. Keys on the on-screen keyboard, sliders, progress bars and pages in a pager still wait for the app. Items on web pages are spoken first when SandFly finds them among the items of the page; items that the page finds itself wait for the page. On a test page in Chrome on an Android 17 emulator, the median time from the swipe to the start of speech went from 64 ms to 10 ms, and to the start of audio from 126 ms to 55 ms.

### Next swipe prepared in advance

While an item is spoken, SandFly builds what a swipe forward or back would speak. When the swipe lands on the prepared item, speech starts immediately: on average 33 ms after the swipe, compared with 60 ms without preparation. In testing, about four in five swipes used prepared speech.

SandFly prepares nothing for the first swipe on a screen or after the screen changes, and discards prepared speech after 5 seconds.

### Faster feedback after a double tap

TalkBack plays the click sound and vibration, and speaks a new state such as "On", from the click event of the app, which it handles 150 ms after the event arrives. The state is also queued behind other speech, such as the description of the item or a summary that the click changed.

When SandFly activates an item itself, with a double tap, the activation key or a braille display, it plays the click sound and vibration as it sends the click. It reads the item again as soon as the app has carried out the click, and speaks the new state at once, cutting off the speech about the item. The events that the app sends for the same click and state add nothing more. On the Date & time switch **Use 24-hour format**, on an Android 17 emulator, the time from lifting your finger to hearing "On" or "Off" went from about 1.2 seconds to about 40 ms.

Apps that update an item after the click has been carried out still have its state spoken from their own events. Keys on the on-screen keyboard keep the usual click feedback.

### Items that a swipe scrolls into view

When a swipe moves to a list item that is only partly on screen, the list scrolls it into view. By default, SandFly then waits for the app to confirm the new focus, as TalkBack does, so these swipes don't get the faster speech above. Speech before the scroll would leave out text of the item that is still off screen, and speak only its place in the list, such as "20 of 80".

With **Navigation** > **Speak items before they scroll into view** on, SandFly speaks these items at once, including the parts still past the edge of the list. Parts that the app hides are still left out. The setting is off by default. Turn it on only if your apps follow the Android conventions: in an app that keeps a part out of sight by moving it outside the item instead of hiding it, that part is read although it isn't shown, and so is a part that stays off screen when the list can't scroll the whole item into view.

### No items skipped when a swipe scrolls a list

When a swipe at the end of the screen scrolls a list a page, SandFly continues as soon as the list starts to move. TalkBack can then find rows at their old places, or a reused row with its old text, and skip items or jump back. On Android 13 and later, SandFly reads the list again from the app at that moment, on several threads at once. When the next item still isn't on screen, SandFly checks again on each frame until the list stops, for at most about 300 ms, instead of scrolling a second page while the first is still moving. In walks through the Android Settings list on an emulator, the time from lifting your finger to speech on swipes that scroll was the same as before.

### Reading order kept between swipes

On each swipe, TalkBack requests every item on the screen from the app and waits for each answer. SandFly keeps the reading order between swipes, and builds it again only when the screen changes, or when SandFly scrolls or clicks. Changes to text or state, such as a clock that ticks or a progress bar that moves, don't discard the order.

To find the next item in a list, TalkBack also builds the reading order of the whole screen three times for one swipe: to check whether focus is at the end of the list, to find the item, and to check whether the item needs to be scrolled into view. SandFly builds it once and reuses it.

In a test on a Realme phone, three in four swipes reused the reading order, and the longest pause during swiping went from about 450 ms to about 150 ms.

### Next item found near the focused item

A list that scrolls discards the saved reading order, so the next swipe has to build the order of the whole screen again. SandFly first looks for the next or previous item near the focused item: in its children, then in the items after or before it and after or before each item that contains it, in the same order as for the whole screen, including items that an app places before or after an item beside them, such as an action bar before the content. SandFly uses the result only when it is certain to be the same as with the order of the whole screen, and keeps what it read for the next swipes until the screen changes.

SandFly builds the whole order as before at the start or end of the screen, when an app places items elsewhere in the order, in web content, in a container with more than 50 items, and when the search reads more than 150 items. The search near the focused item applies only on screens where the last whole order placed items only beside the items next to them. SandFly also prepares the next swipe this way when no reading order is saved, so prepared speech is used after a list scrolls too.

### Swiping on web pages

On a web page, TalkBack requests the next item from the page. Chromium, the engine of Chrome and of web views in apps, sometimes skips items on a page that has just opened, such as a paragraph or a quote, or reports no previous item when there is one, and TalkBack then moves focus out of the web view. SandFly takes the next or previous item from the items of the page, in page order, and requests it from the page only when an item that contains other items can be acted on, such as a clickable section, or when the page has no further item.

On every swipe, TalkBack also builds the reading order of the whole screen before it requests the item from the page, and after focus moves, it reads the items beside each item that contains the new focus. In apps with web pages inside other web pages, such as a restaurant menu in Uber Eats, focus and speech stopped during fast swiping. SandFly builds the reading order only when the swipe leaves the page. It reads the items beside the new focus when focus moves, and the items beside each item that contains it only when it needs them to put focus back after a screen change.

TalkBack also speaks an item on a web page only after it has handled every change that the page reported before it, and during fast swiping it drops all but the last two items. SandFly speaks each item as soon as the page moves focus. Window changes, announcements and changes to assertive live regions, such as form errors, that came before it are still spoken first.

### Putting focus back after a page changes

When the focused item disappears, such as when a page opens or reloads, TalkBack searches the new page for the item, or for the item now at its place. Each step of the search waits for the app. In Uber Eats, opening a restaurant menu stopped gestures and speech for over 2 seconds, as TalkBack searched twice in a row for the same item: once to put focus back, and again when that failed. SandFly doesn't repeat a search that found nothing during the same change, requests each item that contained the focus only once, and, after a content change, stops a search at 0.3 seconds. A stopped search places focus as when the item isn't found. Searches after a window change or window navigation have no time limit, as in TalkBack.

### Low-latency audio

The usual Android audio path adds about 60 to 70 ms before a sound or speech reaches the speaker. With **Audio output** > **Low-latency audio** on, SandFly plays its sounds and speech through a single fast audio track at the sample rate of the device. On a Galaxy Z Fold7, this path takes about 21 ms. The sounds of a sound theme are decoded for it as soon as the theme is chosen or SandFly starts, and over a minute of sounds stays decoded, so that no sound waits for its first play or plays the slower usual way.

Low-latency audio also changes how pausing speech works. A two-finger tap stops speech mid-word, and speech resumes from exactly where it stopped. SandFly holds up to 10 seconds of paused speech.

With low-latency audio on, SandFly also detects a speech engine that stops responding: when the engine reports nothing about new speech for 3 seconds, SandFly switches to the next installed engine.

Low-latency audio is on by default on phones and tablets, and off by default on watches. If speech or sounds break up, turn it off. Low-latency speech requires Android 11 or later; on earlier versions, only the sounds use the fast path. Speech plays the usual way for engines that play their own audio or that report errors, while an app records with the microphone, during calls, and for 10 seconds after a call starts or ends. Each sound plays the usual way the first time that SandFly uses it, and 3D sounds play through the 3D player.

### Less lag while scrolling

After each scroll event, TalkBack searches the list for a new item to focus. The search blocks touch exploration and can make gestures fail. SandFly searches once, after scrolling stops.

### Less stalling in busy apps

In busy apps, a backlog of accessibility events could block gestures in TalkBack for almost 3 seconds. SandFly handles events in slices of about 16 ms, so that gestures get through between them. It also skips lookups of view classes that can never succeed, which stalled screens with many custom views.

### Shorter focus delay

When you touch the screen, SandFly waits to distinguish a gesture from a touch before it focuses the item under your finger. **Touch** > **Focus delay** sets this wait, from 0.15 to 2 seconds, on Android 13 and later. TalkBack waits 0.3 seconds by default, which is most of the time between touching an item and hearing it. SandFly waits 0.15 seconds. If SandFly focuses items when you meant to swipe, choose a longer delay.

**Touch** > **Reset all delay settings** sets the delay settings of **Touch**, **Typing**, **Display** and **Navigation** back to their defaults. See [Touch](docs/user-guide/en/04-touch.md).

### Faster response on empty space

When you touch an empty part of the screen, TalkBack waits 100 ms after the focus delay before it plays the sound. SandFly waits 40 ms.

### Faster screen changes

After a window changes, TalkBack waits for the window to settle before it speaks the title. TalkBack counts that wait in a way that makes 550 ms last about 900 ms. SandFly counts the real time.

**Display** > **Turn off animations** turns off animations for the whole phone, so that screens change immediately and the wait is only 200 ms. TalkBack calls this setting **Reduce window announcement delay**, and has the code that turns animations off disabled, so the setting does nothing. SandFly turns animations off on Android 13 and later, and sets them back as they were when you turn the setting off or turn SandFly off. On Android 12 and earlier, the setting only shortens the wait. The setting is on by default.

When you activate an item, or press a key, while the item is spoken or just after, TalkBack still speaks its usage hint, such as "Double-tap to activate", and the name of the new window waits for the hint. SandFly drops the hint.

TalkBack speaks the window name only after the speech before it, and the first item in the new window only after the window name. **Verbosity** > **Window names** sets when window names are spoken:

- **Always**, as TalkBack does.
- **Always, cut off by the first item**, the default: the window name interrupts the speech before it, and the first item interrupts the window name as soon as it is focused, so that usually only the window sound plays before the first item.
- **Only when a window opens**: no window name when focus moves into another window, such as the keyboard.
- **Never**: a screen change plays only the window sound.

### Swiping into the next part of a list

When the next item is only partly on screen, TalkBack scrolls it into view, waits for the scroll before it speaks, and then waits another 110 ms for more scroll events. SandFly speaks the item immediately and lets the scroll finish while it speaks.

When the next item is off screen, SandFly checks on each frame whether the list has moved, instead of waiting for the scroll event, which Android sends at most every 100 ms. On a Samsung phone, swipes that scroll went from about 310 ms to about 60 ms when the item was partly on screen, and from about 320 ms to about 140 ms when the list had to scroll a page.

SandFly also finds the end of a list that is laid out in reverse, such as a chat.

### Scroll views that show the next screen immediately

Some screens are one long page instead of a list, and keep all of their content in the accessibility tree. On these screens, when you swipe past the last item on screen, SandFly scrolls straight to the next screen without the animation, so that the next item is at the top, and searches again immediately. An item that was cut off at the edge of the screen is read instead of scrolled past, as the page scroll of TalkBack does.

This requires Android 14 or later. Lists, grids, pagers and Compose screens still scroll a page at a time, because they create only the items on screen or animate their scrolling. For the exception in lists, see [Swiping past list items taller than the screen](#swiping-past-list-items-taller-than-the-screen).

### Swiping past list items taller than the screen

When a swipe moves past a list item that reaches the edge of the list, such as a long post, TalkBack scrolls the list a page at a time and waits about a second after each page. When you swipe forward in a list that supports it, SandFly shows the next item immediately, so that an item of any height takes one step. On a Galaxy S24, swipes past a post ten screens tall in Eltenger went from about 7 seconds to about 0.16 seconds.

This applies to lists with one column whose indexes run down the screen, and whose item has no other controls. SandFly must have had at least two items of the list on screen before it jumps. Swipes back and other lists still scroll a page at a time. After each page, SandFly reads the position of the focused item every 30 ms and searches as soon as the list slows down, at most 300 ms after the scroll, instead of waiting for the list to stop. For an item that fills the list, SandFly reads a small part of the item instead, since the bounds of the item don't change while the list moves. If the next item still isn't on screen then, SandFly scrolls again at once. When the focused item is the last row of the list on screen and has no other part that takes focus, SandFly scrolls before it reads the order of the items in the whole window.

### Speaking the next item while a list scrolls

When the next item is only partly on screen, SandFly speaks it first and then scrolls the rest of it into view, instead of scrolling before it speaks. An item with text that is off screen, such as a row whose title is cut off by the edge of the list, is scrolled into view first and then spoken, so that its text is included.

When a swipe or a key has to scroll a list to reach the next item, SandFly focuses and speaks the next item as soon as it comes on screen, while the list is still moving. If the scroll ends without showing it, SandFly scrolls again once the list has stopped. This applies to the previous item too. Once the list stops, an item that is still cut off by the edge of the list is scrolled fully into view. To wait until the list slows down instead, turn off **Navigation** > **Speak the next item while scrolling**, which is on by default.

### Scrolling ahead to the next item

When the focused item is the last item of a list on screen, SandFly scrolls the list forward while the item is spoken, until the next item is on screen, so that the next swipe doesn't wait for a scroll. Once the next item is on screen, SandFly reads the order of the items in the window and prepares what the next swipe speaks. Focus stays on the item, a part of the item stays on screen, and the scroll plays no sound. After a move to the previous item, SandFly scrolls back instead, until the previous item is on screen.

SandFly scrolls ahead only in lists whose page scroll it measured during an earlier scroll and found shorter than the list and at least half its height, and at most 10 pages. If a page still moves the focused item off screen, the next swipe goes on to the item next to it in the list, and SandFly stops measuring the page of that list. SandFly stops at the first page that shows the item, doesn't scroll ahead in text fields, in web content or during continuous reading, and stops when you touch the screen, press a key, or a window changes. To turn it off, turn off **Navigation** > **Scroll ahead to the next item**, which is on by default.

### Scroll sound when moving between items

TalkBack plays the scroll sound when a swipe or a key scrolls a list to reach the next item, and when a web page scrolls to show the item that a swipe or a key focused. SandFly plays it only for scrolls that you make or request, such as with **Scroll forward**.

### Focus that stays where a swipe scroll put it

A list can keep scrolling for a moment after a swipe has moved focus to the next item. TalkBack treats the rest of that scroll as a scroll that you made, and moves focus on to the next item. SandFly counts it as part of its own scroll. As a result, if you scroll the same list yourself within a second of the scroll of a swipe, focus doesn't move.

### Swiping away from long paragraphs

Some speech engines, such as Gryphon, stop only between the blocks of text that they synthesize, and treat a whole item as one block. When you swipe away from a long paragraph on a web page, the next item can wait up to 850 ms for the engine to stop.

With **Speech** > **Pronunciation** > **Send long text a sentence at a time** on, SandFly sends text longer than 150 characters about a sentence at a time, so that the engine stops immediately. In Chrome on a Samsung phone with Gryphon, web swipes went from as much as 900 ms to at most about 110 ms.

The setting is off by default. Many engines, such as RHVoice, already stop immediately, and pause longer between sentences that are sent separately.

### Time between one-finger taps

TalkBack always waits 0.25 seconds for another tap before it ends a tap gesture. **Gestures** > **Time between one-finger taps** sets this wait for one-finger taps, from 0.1 to 2 seconds. A shorter time makes one-finger multi-tap gestures respond faster, but a slow double tap can then count as two single taps. With a longer time, two slow taps on an item still activate it, and corner taps and multi-finger single taps wait longer. Taps with two or more fingers always allow at least 0.25 seconds between taps.

The setting needs gesture recognition by SandFly. See [Gestures recognized by SandFly](#gestures-recognized-by-sandfly).

### Gesture start time

In TalkBack, a one-finger swipe has to move 1 cm within 0.15 seconds; a slower start becomes touch exploration. **Gestures** > **Gesture start time** sets this time from 0.15 to 2 seconds, 0.15 seconds by default. It needs gesture recognition by SandFly, and isn't on watches.

## Gestures

For every gesture and its settings, see the user guide chapter [Gestures](docs/user-guide/en/03-gestures.md).

### Gestures recognized by SandFly

On Android 13 and later, SandFly recognizes gestures itself by default, instead of leaving this to Android, as the TalkBack source release does. Gesture recognition by SandFly is needed for the rotor, edge gestures and edge menus, corner gestures, the scrub, pinch and spread, swipe and hold, three-part and two-stroke swipes, tap then swipe, two-finger angle gestures, the one-finger triple-tap and quadruple-tap, the quadruple-tap with three fingers, the four-finger taps on half of the screen, the timing settings on the **Gestures** screen and touch lock.

To have Android recognize gestures again, turn off **Developer settings** > **Handle gestures in SandFly**, then turn SandFly off and on again. The gestures and settings above are then unavailable.

### Default gestures

SandFly changes these default gestures:

| Gesture | SandFly | TalkBack |
|---|---|---|
| Tap with 4 fingers | Back | Practice gestures |
| Double-tap with 4 fingers | Home | Tutorial |
| Triple-tap with 4 fingers | Overview | Unassigned |
| Double-tap and hold with 4 fingers | Notifications | Pass through the next gesture |
| Tap and hold with 3 fingers | Pass through the next gesture | Screen search |
| Double-tap with 3 fingers | Turn speech on or off | Copy |
| Triple-tap with 3 fingers | Copy last spoken phrase | Paste |
| Triple-tap and hold with 3 fingers | Paste | Unassigned |
| Quadruple-tap with 3 fingers | Hide or show screen | Not available |
| Double-tap with 2 fingers | Dictate, play or pause media | Play or pause media, start or end voice input |
| Double-tap and hold with 2 fingers | Switch to braille keyboard | Start or end selection mode |
| Triple-tap with 2 fingers | Speak status | Read from focused item |
| Triple-tap and hold with 2 fingers | Unassigned | Turn speech on or off |
| Rotate clockwise with 2 fingers | Next reading control | Not available |
| Rotate counterclockwise with 2 fingers | Previous reading control | Not available |
| Scrub with 2 fingers | Back | Not available |
| Swipe up from the bottom edge | Home | Not available |
| Long swipe up from the bottom edge | Overview | Not available |
| Swipe down from the top left | Notifications | Not available |
| Swipe down from the top right | Quick settings | Not available |

The gestures that TalkBack doesn't have need gesture recognition by SandFly. You can assign any gesture to another command on the **Gestures** screen. **Practice gestures** is at the top of the **Gestures** screen and in **Help & feedback**. Watches have defaults of their own; see [Wear OS watches](#wear-os-watches).

### Gesture layouts

**Gestures** > **Gesture layout** chooses the gestures that the **Gestures** screen starts from:

- **SandFly gestures**, the default, with the gestures in [Default gestures](#default-gestures).
- **VoiceOver gestures**, the gestures of VoiceOver on iPhone, such as a two-finger tap to pause speech, three-finger swipes to scroll and a three-finger tap for **Speak position**. Gestures that VoiceOver doesn't have are **Unassigned**.
- **BlackBerry gestures**, similar to the gestures of the BlackBerry 10 Screen Reader. Choosing them opens **Use BlackBerry gestures?** first. See [Settings profiles](#settings-profiles) for BlackBerry mode.

The first time SandFly starts, the **Choose gestures** dialog offers **SandFly gestures** and **VoiceOver gestures**. Each layout keeps its own changes. Gestures in apps and gesture profiles apply to every layout.

### Two-finger swipes handled by SandFly

In TalkBack, two-finger swipes go to the app, which scrolls with them. With **Gestures** > **Handle two-finger swipes in SandFly** on, two-finger swipes run the commands assigned to them, by default **Scroll up**, **Scroll down**, **Scroll left** and **Scroll right**, and the **2 finger angle** gestures are available. Where nothing on the screen scrolls sideways, **Scroll left** and **Scroll right** send a one-finger swipe to the pager around the focused item, such as the folder tabs in Solid Explorer. Choosing a gesture layout turns the setting on for **VoiceOver gestures** and **BlackBerry gestures** and off for **SandFly gestures**. It needs Android 11 or later.

### Rotor

Turn two fingers on the screen like a dial to choose a reading control: clockwise for the next one, and counterclockwise for the previous one. Then swipe up or down to change it.

Each step of 30 degrees, a twelfth of a turn, moves one reading control, so that you can keep turning to move further. A quick flick moves exactly one step. **Gestures** > **2 fingers** > **Rotor turn per step** sets the step from 15 to 60 degrees. Scrolling with two fingers doesn't turn the rotor. You can assign other commands to **Rotate clockwise with 2 fingers** and **Rotate counterclockwise with 2 fingers**.

### Edge gestures and scrub

As in VoiceOver on iPhone, you can slide one finger in from an edge of the screen. Start at the very edge, such as on the gesture navigation bar or the status bar, and slide in:

- Up from the bottom edge: a click like a home button plays after about a centimeter. Lift there for the home screen. Slide about four more centimeters to a second click, and lift there for Overview.
- Down from the left half of the top edge for notifications, or from the right half for quick settings, after the same first click. A long swipe down from either half, to a second click, does nothing by default.
- Right from the left edge, or left from the right edge. The top half and bottom half of each side edge are separate gestures, each with a first and second click. They do nothing by default.

Only slides that have a command click. A slide without a command at the first click stays an ordinary swipe until the second click, and a long swipe without a command has no second click. Lifting before the first click is an ordinary swipe. In a corner, the direction of the slide chooses the gesture; a slide that fits two gestures uses the top or bottom edge, and a slide in comes before a slide along an edge.

Sliding back from the second click to the first plays one short tap; sliding back to the edge plays two, and lifting there cancels the gesture. Touching the edge, the first click and the second click each play a sound that rises with each stage. A slide that starts away from the edge, or that takes longer than about half a second to reach the first click, is an ordinary swipe or touch exploration. A finger resting at the edge for 0.2 seconds starts touch exploration. To turn off the clicks and taps, turn off **Edge gesture middle**, **Edge gesture top** and **Edge gesture slide back** under **Vibrations** in **Sound and vibration** > **Individual sounds and vibrations**; see [Navigation sounds](#navigation-sounds).

You can also slide one finger along an edge: up or down the left or right edge, or left or right along the top or bottom edge, such as **Swipe up along the left edge**. Each of these runs its command when you lift after sliding the distance to the first click, does nothing by default, and plays no edge sounds.

Under **Gestures** > **Edges**:

- **Scroll with one finger along the left edge**, **Scroll with one finger along the right edge**, **Scroll with one finger along the top edge** and **Scroll with one finger along the bottom edge**: a slide along that edge moves the content of the app with the finger, as a two-finger slide does. The gestures that slide along that edge are then not used. Off by default.
- **Left edge menu** and **Right edge menu**: with **Use the left edge as a menu** or **Use the right edge as a menu** on, the edge is a menu of up to 20 commands, added with **Add command**. Slide up or down along the edge: the commands divide the length of the edge equally from the top, SandFly speaks each command with a tick as the finger reaches it, and lifting runs the command under the finger. Moving in from the edge more than three times the edge width speaks "Cancel", and lifting there runs nothing. A slide that starts on the keyboard is not a menu. With **Slide in from the edge** on, the menu goes across the screen instead: slide in from the edge, the commands divide the width beyond the distance to the first click equally, from that edge, and moving up or down more than three times the edge width speaks "Cancel". It replaces the slides in from that edge, and slides and scrolling along the edge work again; from the top and bottom corners, top and bottom scrolling and the slides in from the top and bottom still take the touch. The gesture trainer practises it that way.
- **Edge width**: how far from the edge a touch can start, from 0.5 to 2 cm, 0.5 cm by default.
- **Distance to first click**: from 0.5 to 2 cm, 1 cm by default.
- **Distance from first to second click**: from 1 to 5 cm, 4 cm by default.
- **Speak command at each click**: at each click of a slide in from an edge, SandFly speaks the command that lifting there runs, such as "Home" or "Overview". Off by default.

To scrub, move two fingers quickly one way across the screen and back, each stroke about a centimeter long. By default, the scrub goes back. Two fingers moved one way only still scroll. While a scrub is still possible, a sideways scroll with two fingers can wait up to about half a second before it starts.

Edge gestures and the scrub aren't on watches.

### Corner gestures

Under **Gestures** > **Corners**, each corner of the screen has five one-finger gestures: tap, double-tap, triple-tap, double-tap and hold, and triple-tap and hold, such as **Tap top left corner**. They do nothing by default.

A corner with no command works as usual. In a corner with a command, taps that SandFly doesn't use are ordinary taps: a tap still waits for a second tap and then explores, and a double tap still activates. A double tap waits for a third tap only while the corner has a triple-tap command. A touch that moves, adds a finger or rests without a hold gesture is an ordinary touch, so that swipes and edge gestures from the corner still work. Corner gestures don't start on the keyboard.

- **Corner size**: the size of the square at each corner, from 0.5 to 2 cm. The default is **Same as edge width**.
- **Corner tap time limit**: how long a tap in a corner can stay down before it counts as a hold, from 0.15 to 0.5 seconds. The default is **System default**.

Not on watches.

### Tap, then swipe

Under **Gestures** > **Tap, then swipe**, there are gestures that tap once, twice or three times with two, three or four fingers and then swipe up, down, left or right with as many fingers, such as **Double-tap, then swipe up with 2 fingers**, and gestures that tap with two fingers and then swipe with one finger. They do nothing by default. The swipe is the next touch after the last tap and must start within 0.25 seconds of it. While one of these gestures has a command, the taps it starts with wait for the swipe. Not on watches.

### Two-finger angle gestures

Under **Gestures** > **2 finger angle**, there are eight gestures that swipe two fingers one way and then turn at a right angle, such as **Swipe up then right with 2 fingers**. They do nothing by default. Two fingers moved one way and back are the scrub, so there are no two-finger back-and-forth gestures. The angle gestures work only while **Handle two-finger swipes in SandFly** is on, and are hidden while it is off. Not on watches.

### Three-part swipes

Under **Gestures** > **Three-part swipes**, there are 36 gestures that move one finger in three directions without lifting it, each part turning from the one before, such as **Swipe right, up, then right**. They do nothing by default, and a three-part swipe without a command isn't recognized. **Recognize swipes before lifting** turns them off and hides them. Not on watches.

### Two-stroke swipes

Under **Gestures** > **Two-stroke swipes**, there are 16 gestures of two one-finger swipes with the finger lifted between them, such as **Swipe right, lift, then swipe up**. They do nothing by default and work only while **Gestures** > **Time between two-stroke swipes** is set, from 0.25 to 2 seconds; it's **Off** by default, and their categories are hidden while it's **Off**. The three-part and two-stroke swipe categories are at the end of the **Gestures** screen. After a swipe, SandFly waits for the second swipe only when a two-stroke swipe that starts with that swipe has a command; other swipes run at once. When the time passes, or the next touch is something else, the first swipe runs as an ordinary swipe.

Either swipe can also be a back-and-forth or angle swipe, such as **Swipe up then left, lift, then swipe right** or **Swipe up, lift, then swipe right then down**: 240 more gestures, in **Two-stroke swipes ending with back-and-forth or angle** and a **Two-stroke swipes starting with** category for each back-and-forth and angle swipe. A two-stroke swipe that starts with a back-and-forth or angle swipe makes no one-direction swipe wait. **Recognize swipes before lifting** turns these off and hides them. Not on watches.

### Swipe and hold

Each one-finger swipe, back-and-forth swipe and angle swipe has a version that ends with the finger resting on the screen, such as **Swipe right and hold** or **Swipe left then up and hold**. Swipe, then keep the finger still: the command runs when the hold time ends, before you lift the finger. Lifting sooner makes the ordinary swipe. A pause shorter than the hold time where an angle or back-and-forth swipe turns stays part of that swipe; a longer pause is the swipe and hold of the first direction, when that has a command.

The 16 gestures are at the end of the **1 finger**, **1 finger back-and-forth** and **1 finger angle** categories, and do nothing by default. A swipe and hold without a command isn't recognized, so that swipes work as before until you assign one. **Gestures** > **Hold time after a swipe** sets the rest, from 0.1 to 0.5 seconds, 0.25 seconds by default. Not on watches.

### Pinch and spread

**Pinch with 2 fingers**, **Spread with 2 fingers** and the same gestures with 3 and 4 fingers do nothing by default. A pinch or spread counts once the fingers have moved about half a centimeter toward or away from their middle, and by a third of the distance, without moving together or turning much. While a two-finger pinch or spread has a command, a two-finger movement that pinches more than it moves waits before it reaches the app, so that apps that zoom with two fingers may not zoom until both are **Unassigned**. Not on watches.

### Four-finger taps on half of the screen

**Tap with 4 fingers on top half** and **Tap with 4 fingers on bottom half**, in **Gestures** > **4 fingers**, are separate gestures. While one of them has a command, a four-finger tap on that half runs it instead of **Tap with 4 fingers**. Both are unassigned by default.

### Two-finger double-tap at the top and bottom

**Double-tap with 2 fingers, top and bottom**, in **Gestures** > **2 fingers**, is a separate gesture. While it has a command, a two-finger double-tap with one finger in the top third of the screen and the other in the bottom third, measured between the ends of the device held upright, runs it instead of **Double-tap with 2 fingers**. It is unassigned by default, requires Android 13 or later and **Handle gestures in SandFly** turned on, and isn't on watches.

### One-finger triple-tap and quadruple-tap

**Triple-tap** and **Quadruple-tap**, in **Gestures** > **1 finger**, are one-finger taps anywhere on the screen. TalkBack has no one-finger tap gestures beyond the double-tap. Both are unassigned by default. A one-finger double-tap runs at once while neither has a command; while one has a command, a double-tap waits for a further tap for **Time between one-finger taps**. A triple-tap waits only while **Quadruple-tap** has a command. They require Android 13 or later and **Handle gestures in SandFly** turned on, and aren't on watches.

### Gestures on the cover screen of a flip phone

On a flip phone, such as a Galaxy Z Flip, SandFly sets up gesture recognition on each display as it appears or turns on, so that touch exploration and gestures work on the cover screen after you close the phone, also when SandFly started while the phone was open. Needs Android 13 or later.

### Multi-finger taps without the wait

A tap with two, three or four fingers, or several such taps, runs its command as the fingers lift when no longer gesture that starts with the same taps has a command, such as a further tap, a tap and hold, or a tap then swipe. TalkBack waits about 0.3 seconds for a further tap. A quadruple-tap with three fingers always runs at once. In the tutorial, these taps still wait.

### Recognize swipes before lifting

With **Gestures** > **Recognize swipes before lifting** on, a one-finger swipe runs its command once the finger has moved 1 cm, before you lift it. The setting turns off and hides the one-finger back-and-forth and angle gestures, the three-part swipes and the two-finger angle gestures. A swipe still waits for the lift when the swipe and hold of its direction has a command, or when an edge gesture is still possible. It's off by default.

With the setting off, a swipe also runs before the lift when no gesture that starts in its direction has a command. With **SandFly gestures**, the back-and-forth gestures have commands; with **VoiceOver gestures**, they don't. Each gesture profile can turn the setting on or off for itself. Not on watches.

### Back tap

Under **Gestures** > **Back tap**, **Double tap on back**, **Triple tap on back** and **Quadruple tap on back** run a command when you tap the back of the phone two, three or four times. SandFly detects the taps with the accelerometer, only while the screen is on, SandFly isn't paused and a back tap has a command. They do nothing by default. Taps wait for a further tap only while a back tap with more taps has a command. **Back tap sensitivity** is **Low**, **Medium** or **High**, **Medium** by default. Back tap isn't on watches or TVs.

### Shake

Under **Gestures** > **Shake**, **Shake** runs a command when you shake the phone quickly back and forth, at least two swings each way within about two thirds of a second. Shaking that goes on runs it once. SandFly detects shakes with the accelerometer, only while the screen is on, SandFly isn't paused and **Shake** has a command. It does nothing by default. **Shake sensitivity** is **Low**, **Medium** or **High**, **Medium** by default. Shake works while touch is locked, and isn't on watches or TVs.

### Gestures in apps

**Gestures** > **Gestures in apps** gives a gesture or keyboard shortcut a different command while one app is in front. In the notification shade, quick settings and the lock screen, the gestures of the System UI app apply. **Unassigned** makes the gesture do nothing in the app. The **Gestures for this app** command, in the SandFly menu by default, opens the screen of the app in front.

### Gesture profiles

A gesture profile is a set of gestures that run other commands while the profile is in use. To add one, go to **Gestures** > **Gesture profiles** and choose **Add gesture profile**. Each gesture in a profile is **Default** at first, and runs what it runs without the profile: its command on the **Gestures** screen, in **Gestures in apps**, or in a script. A gesture set to a command runs that command, before gestures in apps and scripts. While the reading control is **Rows and columns**, swipe left and swipe right still move by column. Profiles also have **Rename**, **Move up**, **Move down**, **Export to file** and **Delete gesture profile**, and a **Recognize swipes before lifting** switch of their own.

**Default** uses no profile. To change the profile in use:

- **Gestures** > **Gesture profiles** > **Gesture profile in use**.
- The **Gesture profile** reading control and the **Gesture profile** item in the SandFly menu, shown once a profile exists.
- The **Choose gesture profile**, **Next gesture profile** and **Previous gesture profile** commands, in the **SandFly** category, and **Use gesture profile** for **Default** and each profile, in the **Gesture profiles** category. You can assign them to gestures, including gestures in a profile, and to keyboard shortcuts.

SandFly speaks the name of the profile when it changes. Profiles don't change fingerprint gestures or keyboard shortcuts, and spoken hints that name a gesture name the gesture on the **Gestures** screen.

## Touch

For these settings, see the user guide chapter [Touch](docs/user-guide/en/04-touch.md).

### Lift to activate

SandFly can activate the item under your finger when you lift the finger after exploring by touch, as TalkBack does for keys of the on-screen keyboard. **Touch** > **Lift to activate** has **Disabled**, the default, **Only on navigation bar**, for the Back, Home and Overview buttons, and **Entire screen**. **Lift to activate** is also a reading control.

While lift to activate is on, you can lift without activating:

- Slide the finger off the bottom of the screen. Lifting in the bottom 3.5 mm of the screen activates nothing, also for keys that type when you lift, and a sound and a vibration play when the finger reaches the bottom edge.
- Hold the finger on the item. **Touch** > **Hold to cancel** sets how long: **Off**, the default, **1 second**, **2 seconds**, **3 seconds** or **5 seconds**. After that time on one item, lifting activates nothing.

### Tap to activate

With **Touch** > **Tap to activate** on, a quick tap that lifts before the item under the finger is focused activates that item. While focus has moved in the last 3 seconds, a tap waits for a second tap first, so that a double tap still activates the focused item. It's off by default.

### Tap to activate navigation bar buttons

With **Touch** > **Tap to activate navigation bar buttons** on, touches on the navigation bar go straight to Android, so that a single tap presses a button and holding Home holds it, as without a screen reader. It's off by default and needs Android 11 or later. SandFly doesn't read the buttons as you touch them, but speaks the button that a tap pressed, such as "Back" or "Home". To turn this off, turn off **Touch** > **Speak navigation bar buttons**. While **Tap to activate navigation bar buttons** is on, **Lift to activate** doesn't apply to the navigation bar.

### Activate with a tap

Some controls, such as some in TikTok, ignore the accessibility click that activating sends, and react only to a finger on the screen. The **Activate with a tap** command, in the **Activation** category and unassigned by default, taps the middle of the part of the focused item on the screen instead, with touch passed through to the app for the tap. A script rule can do the same for chosen items whenever they are activated. For apps where activating an item does nothing, turn the app on in **Touch** > **Double tap taps the screen**, or run the **Double tap taps the screen in this app** command, in **Activation** and unassigned by default, in the app; activating an item in that app then taps it. All of these need Android 11 or later; otherwise the item is activated as usual.

### Lock touch

While touch is locked, touches and gestures on the screen do nothing, and nothing reaches the app. Keys, volume key commands, fingerprint gestures, back tap and shake still work. The **Lock or unlock touch** command, in the **SandFly** category, locks and unlocks touch and announces "Touch locked" or "Touch unlocked". Pressing both volume keys together always unlocks touch.

**Touch** > **Lock touch automatically** is **Never**, the default, **When the screen turns on**, **On the lock screen** or **While the proximity sensor is covered**. **Touch** > **Touches while touch is locked** sets what a touch on the locked screen plays: **Nothing**, the default, **Sound** or **Vibration**. Touch lock needs Android 13 or later and gesture recognition by SandFly.

### Speech kept while you touch the screen

TalkBack stops speech when a finger touches the screen. To keep speech going until touch focuses an item, turn off **Speech** > **Stopping speech** > **Stop speech when touching the screen**, which is on by default. When touch focuses an item, all speech stops, including notifications, window titles and other speech that new speech doesn't cut off; gestures that speak still interrupt. Activating an item, with a double tap, a key or a braille display, stops speech whether the setting is on or off.

### Stuck touch exploration

On some phones, after a touch with several fingers, Android can stop touch exploration for every screen reader until the phone restarts. Swipes and taps still work, but dragging a finger reads nothing.

**Touch** > **Fix stuck touch exploration**, on by default, detects exploring touches that find no item. To fix the problem, SandFly needs the DUMP permission. With [Shizuku](https://shizuku.rikka.app) or root, SandFly grants it when it starts. Without them, grant it once with adb:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
```

With the permission, SandFly reads which touches Android left down, announces "Fixing touch exploration", turns explore by touch off while it ends those touches with a gesture outside the screen, usually for less than half a second, and turns explore by touch back on. It then announces "Touch exploration fixed". A touch during the fix goes straight to the app, and SandFly tries again after your next exploring touch. Without the permission, after three such touches in a row, SandFly announces that touch exploration may be stuck, and you need to restart the phone.

The fix needs a phone whose `dumpsys input` lists the touches that Android holds for accessibility, as Samsung phones do. On other phones, SandFly can only announce that touch exploration may be stuck. Exploring an area with nothing to read, such as blank space or a game, also finds no item, so without the permission the warning can be wrong, at most once every 10 minutes.

When the screen turns off during a touch, Android can also keep the fingers of that touch as down, so that every later touch ends in touch exploration: only exploring works, and no gesture or double tap is detected. SandFly turns explore by touch off and on again when the screen turns off during a touch, and when two touches in a row end this way, which sets up the touch exploration of Android again. This needs no permission.

## Navigation

For these settings, see the user guide chapter [Explore and read the screen](docs/user-guide/en/02-reading.md).

### Wrap around

When you swipe past the last item on the screen, TalkBack stops once and then goes to the first item. To stop at the end instead, turn off **Navigation** > **Wrap around**. Each swipe or key press past the first or last item then reads the item again with the end sound. Moving by heading, link or control in apps stops at the ends too. Web pages can still wrap by themselves.

To go to the first item on the first swipe past the last one, turn off **Navigation** > **Stop at the last item before wrapping**. It is on by default and has no effect while **Wrap around** is off.

With **Navigation** > **Wrap around in reading controls** off, reading controls that go through a list of values, such as **Speech engine**, **Audio output device**, **AI actions**, **Live recognition**, **Actions**, **Addresses and numbers**, **Speak punctuation and symbols** and **Sound mode**, stop at the first and last value: swiping past it reads the value again with the end sound. **Wrap around in reading controls** follows **Wrap around** until you change it, so that items and values can wrap differently.

**Wrap around** is also a reading control. Wrapping is on by default.

When focus is in the status bar or the navigation bar, swipes past the first or last item of the bar go around the bar. To move into the app instead, turn on **Navigation** > **Move from the status bar and navigation bar to other windows**, which is off by default.

### Focus in new windows

When a window opens, or you go back, TalkBack moves focus into it: back to the item you were on, to the text field with the cursor, or to the first item. To leave focus where it is, turn off **Navigation** > **Move focus to new windows**. SandFly then reads only the window name, and the next swipe moves to the first item. This applies to the menus and dialogs of SandFly too, when they open and when they close. Focus follows input focus only when you move input focus with a key, such as an arrow key or Tab, not when an app moves it. When the focused item disappears and the windows haven't changed, focus is placed again. The setting isn't on TVs, where focus always follows input focus.

### Reading place kept when an item disappears

When an app removes the focused item, such as an ad that collapses, TalkBack jumps back to the top of the page. SandFly focuses the item now at its place and reading goes on from there.

### Focus back on the item you opened

After you activate an item in a list, such as a folder in a file manager or a page in Settings, and go back, focus returns to that item when the list shows it again within 2 seconds. This also works in apps that change the list without opening a new window.

### Text inside items focused separately

With **Navigation** > **Focus text inside items separately** on, each piece of text and each state inside an item gets focus of its own, so that you can reach it by swiping and by touch. The item itself keeps focus only when it has text, a state or actions of its own; activating a piece of text inside it activates the item. It's off by default. The **Focus text inside items separately** command, in the **Navigation** category, turns it on or off and announces "Focus text inside items separately on" or "Focus text inside items separately off".

### Moving by character and word in drawn text

Some apps draw their text themselves and give it to screen readers without movement through it, as Telegram does for messages. In such text, TalkBack moves by character, word or paragraph only through a content description, so it can't move through these messages, only through reply previews. SandFly moves through the text itself when the app doesn't.

### Sentences reading control

With the **Sentences** reading control, swiping down moves to and speaks the next sentence of the focused item, and swiping up the previous one. Moving past the first or last sentence moves to the item beside it. Android has no sentence granularity, so SandFly splits the text itself: a sentence ends at sentence punctuation, including Chinese 。！？, and at the end of a line. In a focused text field, the cursor moves to the end of the sentence when you swipe down and to the start when you swipe up, and swiping stays in the field. On web pages, a sentence that continues into another item, such as a link inside it, is read in parts. **Sentences** isn't in the reading controls by default, and isn't available in selection mode. The commands **Next sentence** and **Previous sentence**, in the **Navigate by text** category, move by sentence with gestures and keyboard shortcuts without the reading control.

### Vertical navigation

The **Vertical navigation** reading control moves to the item below when you swipe down and to the item above when you swipe up. It passes over the other items of the same row, such as the channel and options buttons beside a video in YouTube, or the buttons beside the name of a file. It stays in the column of the focused item: from a key on a keypad, it moves to the key below or above, and from the first item of a row, or an item as wide as the row, to the first item of the next row. In a list, it stays in the list: when there is no row below or above on screen, it scrolls the list a page and moves to the next row. At the end of the list, or outside one, it moves to the item below or above on screen, and plays the end sound when there is none.

**Move to item below** and **Move to item above**, in the **Navigation** category, make the same moves from a gesture or keyboard shortcut, without the reading control. **Move to item on the left** and **Move to item on the right** move to the nearest item on screen in that direction. None of them has a gesture by default.

### Container navigation

**Next container** and **Previous container**, a four-finger swipe right or left by default, and the **Containers** reading control also stop at toolbars, tab bars, navigation bars, search bars and panes, besides lists, grids and scroll views. A bar inside a bar, such as a search bar in a toolbar, is one stop. **Previous container** moves to the start of the container, as **Next container** does, not to its last item, except in web content.

### List items

In an app, moving by list item with focus in a list, such as the posts of a channel, moves to the next or previous item of that list and passes over the buttons and other parts inside each item, such as the Share button of a post. The list scrolls when the next item is off screen. The list is the nearest list, grid, scroll view, collection or other view that scrolls, other than a pager, that contains focus. An item that can't be focused itself is entered at its first part. After the last item, moving by list item goes on to the next list item after the list. Outside a list, it moves to the next list item on screen. The **List items** reading control and the list item gestures and keyboard shortcuts all move this way.

### Tables in Google Sheets

In Google Sheets, SandFly treats the grid of cells as a table, so that the table reading controls and commands move between cells, and the actions of a cell include **Edit cell** and **Insert formula**.

### Addresses and numbers in text

The **Addresses and numbers** reading control and SandFly menu item list the web addresses, email addresses, phone numbers and codes of 4 to 8 digits written in the text of the focused item, also when the app doesn't make them links, such as in a chat message or the notification of a text message with a one-time code. Choosing a web address opens it, an email address opens a new email, a phone number opens in the phone app, and a code is copied. In the reading control, swipe up or down to choose one, then double-tap. Both are off by default; add them in **Reading controls** or **SandFly menu**. **Addresses and numbers to include**, after **Addresses and numbers** on both screens, turns each kind on or off. **Addresses and numbers** is also a command in the **Activation** category.

Phone numbers come from the Android text classifier, which also finds dates, times, street addresses and flight numbers. Digits in any of these, and digits joined to a word, a user name, a time or an amount, aren't listed as codes. Other numbers of 4 to 8 digits, such as a year in a headline, are listed as codes. Nothing is classified as focus moves: the text is classified when the reading control is swiped or the SandFly menu opens, with a wait of at most a second. On Android 8, and when the text classifier doesn't answer within a second, only web and email addresses are listed.

**Copy text from screen** has an **Addresses and numbers** option beside **Characters**, **Words** and **Paragraphs**. It lists every web address, email address, phone number and code on the screen as written, so that a phone number can be copied instead of called. The option is shown only when the screen has any, and lists every kind, whatever **Addresses and numbers to include** is set to. The window can take up to a second longer to open.

### Reading control kept when focus moves

When focus moves onto an item with actions, a slider, a table or a spelling mistake, TalkBack changes the reading control to the one for that item, such as **Actions**, **Adjust slider** or **Proofread**, and changes it back afterwards. On web pages, where many items have actions, this replaces a reading control you chose, such as **Headings**. To keep the reading control you chose, turn off **Reading controls** > **Change reading control automatically**, which is on by default. With it off, **Actions**, when you choose it, also stays selected on items without actions. For the change on web pages, see [Actions on web pages](#actions-on-web-pages).

### Double tap with the actions reading control on Android 11

On Android 11, when you choose an action with the **Actions** reading control, a double tap in TalkBack only plays a sound. In SandFly, a double tap performs the action, and a double-tap and hold performs a long press.

### Sliders that report themselves as unfocusable

realme UI reports some volume sliders as not focusable and unlabeled, so TalkBack skips them. SandFly focuses them.

### realme image descriptions

On realme and OPPO phones, the accessibility service com.oplus.viewtalk sends announcements such as "Image may contain…", which TalkBack speaks. SandFly doesn't speak the announcements, live region changes, window changes, toasts or notifications of com.oplus.viewtalk.

### Telegram reply quotes

In Telegram and apps built from it, such as Nekogram, the quote of the message that a message replies to is a separate item in TalkBack, often cut off. SandFly reads it with its message, after the time, as "replying to" the sender and text, with the whole quoted text when the quoted message is loaded. The quote isn't focused on its own; **Go to quoted message**, in the actions of the message, shows the quoted message. To have the quote as an item of its own, turn off **Navigation** > **Apps** > **Read Telegram reply quotes with the message**; **Go to quoted message** isn't offered then.

### Telegram share buttons

In Telegram and apps built from it, the Share button beside each message in a channel is a separate item, so that each message takes two swipes. SandFly skips it when you swipe and when you touch the screen. It is in the actions of the message, under the name that Telegram gives it, such as **Share**.

### Touching the home screen

Touching between the icons of the home screen doesn't read the page, such as "Home screen 1 of 2". Swiping still reaches it. On Android 13 and later, SandFly clears the cached copy of the home screen when it opens again, so that its icons can be reached after you leave the app list of a launcher, as in Hyperion.

## Web pages

For these settings, see the user guide chapter [Web pages and elements](docs/user-guide/en/13-web.md). For faster swipes on web pages, see [Swiping on web pages](#swiping-on-web-pages).

### Element navigation in apps

You can assign a gesture to move to the next or previous character, word, line, paragraph, heading, link, control, landmark, button, checkbox, radio button, edit field, combo box, focusable item, graphic, list, list item, table, visited link, unvisited link, or heading of a given level. Moving with one of these gestures doesn't change the reading control. In the list of commands for a gesture, these commands are under **Navigate by text** and **Navigate by element**.

Every element reading control, gesture and keyboard shortcut also works in apps:

| Element | In apps |
|---|---|
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
| List items | Rows of the list that has focus. See [List items](#list-items). |
| Tables | Tables and grids with rows and columns. |
| Visited links | None. Apps don't report visited links. |
| Unvisited links | All links. |

When the next element is off screen, SandFly scrolls the list it is in, up to 10 times. With **Reading controls** > **Links and Controls only in web content** on, the **Links** and **Controls** reading controls are left out of the list outside web content. Gestures and keyboard shortcuts for the next or previous link or control still work everywhere. It's off by default.

### Actions on web pages

With **Reading controls** > **Change reading control automatically** on, SandFly selects the **Actions** reading control on web pages only on items that have actions of their own, not on text or on links inside text, and selects the earlier reading control again on the next item without actions. When **Actions** was selected automatically and has nothing for the focused item, swiping up or down uses the reading control that it replaced.

### Posts reached with the keyboard shortcuts of a site

When a web page moves focus to an element such as a post, as Mastodon does with J and K, TalkBack reads only the text of the element that isn't in a link, so that a post is read without its author, the person who boosted it, its time and its hashtags. SandFly reads the text of the links in it as well, but not its buttons. Spaces between links aren't read as "Space".

### Browse mode in each app

With **Navigation** > **Remember browse mode in each app** on, SandFly keeps browse mode as you last turned it on or off in each app. In an app where you haven't turned it on or off, browse mode is off, unless **Navigation** > **Smart browse mode** turns it on in web content. It's off by default. The **Turn browse mode on or off** command, in the **Web** category, can be assigned to a gesture.

## Commands and menus

For the commands and menus, see the user guide chapter [Commands and menus](docs/user-guide/en/05-commands-and-menus.md).

### Commands screen

**Commands**, in the main settings, lists every command in categories, such as **Navigation**, **Reading**, **Text editing** and **System actions**. Each command lists the gestures, the arrow keys of arrow key navigation, the braille cells and the keyboard shortcut that run it, and you assign them from there. A gesture that already runs another command moves to the new command after you choose **Replace**. The same categories are used in the list of commands for a gesture, in the SandFly menu settings, in custom menus, edge menus, macros, voice commands and braille commands. The list holds only the commands that the device supports.

**Search commands**, at the top of the screen, lists the commands of every category whose names have the words typed. The screen of each command also has **Perform gesture**, which assigns the next gesture made, **Run command**, which closes SandFly settings and runs the command in the app shown before, and **Add to SandFly menu** or **Remove from SandFly menu**.

### SandFly menu made of commands

In TalkBack, the menu has a fixed list of items that you turn on or off. In SandFly, the SandFly menu is a list of commands and submenus. In **SandFly menu**, **Add command** lists every command by category, and **Add submenu** adds a submenu, which can hold commands and more submenus, nested up to 12 levels. Under **Current**, each item has **Move up**, **Move down**, **Move to** and **Remove**, and submenus have **Open**, **Rename** and **Delete**. **Move to** moves an item to the end of another menu or submenu. **Reset to default** restores the default items and order. In TalkBack, the menu settings are in **Edit menus** > **Customize TalkBack menu**.

Some items appear only when they apply, such as **Actions** on an item with actions, **Addresses and numbers** when the text has any, and **Split screen** while the divider of split screen is on screen. Options of an item, such as **Addresses and numbers to include**, follow the item in **Current** and are hidden while the item isn't in the menu.

### Shorter SandFly menu by default

These items are off by default in the SandFly menu, and on in TalkBack: **Actions**, **Screen search**, **Add or edit labels**, **Describe text formatting**, **Copy last spoken phrase**, **Spoken language**, **Voice commands**, **Keyboard shortcuts** and **Braille display settings**. Actions are still available with the **Actions** reading control. **Text-to-speech** stays in the menu, so that you can reach the speech settings if your speech engine crashes.

SandFly adds these items, on by default: **Pause SandFly**, **Turn off SandFly**, **Send feedback**, **Split screen**, **Gesture profile** and **Gestures for this app**. In builds with SandFly Pro, the menu also has the screen recognition items.

### Circle menu

The SandFly menu can show as a circle in the middle of the screen, as the menu of TalkBack 8.1 and earlier did. Each item is a slice of the screen around the middle. Slide to an item and lift your finger to choose it, or lift in the middle of the circle to close the menu. Items that open more items show them in a new circle. When the menu opens, a rising note plays and a tick vibrates for each item.

If you open the menu with a gesture that ends with your fingers on the screen, such as a double-tap and hold, slide those fingers out to an item and lift them. The place where your fingers were is the middle of the circle.

To turn on the circle menu, turn on **SandFly menu** > **Circle menu**. It's off by default.

### Custom menus

**Custom menus** makes other menus of commands and submenus, on a screen that works as the **SandFly menu** screen does. Each custom menu is a command in the **Custom menus** category, so that a gesture, a keyboard shortcut, an arrow key, a menu item, or a gesture or shortcut in one app can open it. A custom menu added to another menu appears there as a submenu with its current items.

### Custom reading controls and reading control lists

**Reading controls** has these additions:

- **Custom reading controls**: reading controls made of commands. Swipe up or down to choose a command, and double-tap to run it. A custom reading control can be moved in the list and removed from it like any other. Needs Android 11 or later.
- **Reading control lists**: named lists of reading controls apart from the main list, each with its own order. Each list adds **Next item in** and **Previous item in** commands to the **Reading control lists** category. **Next reading control list** and **Previous reading control list**, in the **Reading controls** category, choose the list that next and previous reading control move through.
- **Not in reading controls**: the reading controls that aren't in the list, in collapsed groups. Double-tap a control to add it at the end of **Current**.

The **Reading controls** category of commands has a **Set reading control to** command for each reading control in the list, in its order, including reading controls from scripts, and **First reading control**, which selects the first reading control of the list in use. The **Setting values** category has next and previous value commands for reading controls that change a setting, such as **Speak punctuation and symbols** and **Speech engine**. When you change a reading control or its value with a gesture, SandFly also shows the change on the screen for about 3 seconds.

### Write the letter of a reading control

The **Write the letter of a reading control** command, in the **Reading controls** category, opens a writing surface on the whole screen. Write a letter with one finger, and SandFly selects the reading control with that letter, or else the next one whose name starts with it. The letters are mostly the quick navigation keys of NVDA: H for **Headings**, K for **Links**, L for **Lines**, A for **Landmarks**, W for **Words**, C for **Characters**, P for **Paragraphs**, O for **Controls**, S for **Speech rate**, and B, X, E, G, I, T, R, V and U for buttons, checkboxes, edit fields, graphics, list items, tables, radio buttons, visited and unvisited links. **Reading controls** > **Handwriting letters** changes or clears each letter. A tap closes the surface, and so do 10 seconds without writing.

Handwriting is recognized on the phone by Google ML Kit, in the language of the phone. The first time, SandFly shows a dialog to download the recognition model for the language, about 20 MB, from Google, and a message shows when it has downloaded. Not on watches.

### Speak status

**Speak status**, in the **Reading** category and a two-finger triple-tap by default, reads what the status bar shows: the time, the battery, the Wi-Fi network and its signal, and the mobile carrier, network type and signal. SandFly also reads the ringer, Do Not Disturb and airplane mode when they aren't in their usual state, and "Screen hidden" while the screen is hidden. To read the name of the Wi-Fi network, allow location access when SandFly requests it the first time.

**Verbosity** > **Status readout** sets what **Speak status** reads and in what order; each item has **Move up** and **Move down** actions. **Read from focused item**, the action of this gesture in TalkBack, is in the SandFly menu by default.

### More commands

SandFly adds these commands, which you can assign to gestures, keyboard shortcuts and menus. None has a gesture by default. Commands for typing are in [Editing commands](#editing-commands).

- **Open app**: a category with one command for each installed app, named **Open** and the name of the app. The command opens the app as its home screen icon does.
- **App shortcuts**: a category that lists the shortcuts that touching and holding an app icon on the home screen shows, such as the cart of a shopping app or a new message, named "App: Shortcut". The list has the static shortcuts of each home screen app. Shortcuts that an app adds while it runs, such as recent conversations, and shortcuts that only the home screen app may open are listed from Android 12 with Shizuku started through adb or wireless debugging, or with root. A command list reads them each time it opens. **Gestures** > **App shortcuts** > **Add app shortcut** lists the create shortcut activities of apps, which launchers use to add shortcuts such as a MacroDroid macro or a contact to the home screen; the shortcut made there is added to the list, and can be renamed or deleted on that screen. SandFly opens only shortcuts that start an exported activity of another app, not one protected by a permission that the app of the shortcut doesn't have. When a shortcut can't be opened, SandFly announces "Could not open app shortcut".
- **Accessibility shortcuts**: a category that lists the features that Android accessibility shortcuts can run, such as Reading mode or the Guided Vision of Gemini, without the feature taking one of those shortcuts. A feature that is an accessibility service needs Shizuku or root. A service that only turns on and off is turned on or off. One that opens, such as Reading mode, is turned on if it's off and opened through one of your accessibility shortcuts that has it; when none has it, SandFly puts it in the volume keys shortcut for the moment it takes, and sets that shortcut back as it was, also after SandFly is stopped in that moment.
- **App tiles** and **System tiles**: categories that activate quick settings tiles. **App tiles** lists the tiles of installed apps. SandFly clicks such a tile in the system quick settings, which it opens and closes; with Shizuku or root, it first adds the tile to the system quick settings when it isn't there, and clicks it through Shizuku when it isn't found there. **System tiles** lists the tiles that **Add system tile** in the quick settings panel settings found last. With Shizuku or root, **Add system tile** gives SandFly DUMP and usage access and reads the tiles from the system; without them, or when the system gives no tiles, it looks through the system quick settings. While SandFly has DUMP and usage access, it reads the tiles again when it starts. A system tile that SandFly has a control for, such as Wi-Fi, Hotspot or the sound mode, is that control, named as in SandFly's quick settings panel, and changes it through Android or Shizuku. Without Shizuku, a tile command that needs it announces "Shizuku not running". Other system tiles, such as screen recording, are activated in the system quick settings, which SandFly opens and closes.
- **Lock screen**, **Power menu** and **Take screenshot**, in **System actions**.
- **Restart phone** and **Power off phone**, in **System actions**. They ask to confirm in a dialog, then restart or turn off the phone through Shizuku or root, the same on every phone, with no power menu to use. On the lock screen, where the phone may ask to unlock before it turns off, the power menu opens instead. Without Shizuku or root the power menu opens too, and when Shizuku isn't running or doesn't allow SandFly, SandFly says so first.
- **Turn magnification on or off**, in **System actions**, at the scale set in the Android magnification settings.
- **Turn color inversion on or off**, in **System actions**. It needs Shizuku or root; without them, the Android accessibility settings open.
- **Answer call**, in **System actions**, answers the ringing call, and announces "No incoming call" when none rings. **End call**, in **System actions**, declines the ringing call or ends the call in progress, and announces "No call" when there is none. Phone calls go through Android; the first time, SandFly requests permission to answer calls. The calls of apps such as WhatsApp are answered, declined and ended with the buttons of their call notification; ending a call in progress needs notification access. While a phone call rings, only a volume key with **Answer call** or **End call** assigned to one of its presses runs commands; Android keeps a volume key press that silences the ringer from SandFly.
- **Previous app**, in **System actions**, switches to the app before the one on screen. With Shizuku or root, SandFly reads the recent apps and opens that app, leaving out the launcher, and announces "No previous app" when there is none. Without them, it performs the Recents action of the system twice. **Overview**, performed again while the SandFly overview is on screen, opens the previous app the same way.
- **Older app** and **Newer app**, in **System actions**, step through the recent apps. **Older app** opens the next older app and speaks its name; repeated, it goes further back. **Newer app** goes back toward the app where the steps started. The order of the apps is kept until 5 seconds pass without a step or an app that the steps didn't open comes to the front. SandFly announces "No older app" or "No newer app" at either end. They need Shizuku or root. Without them, **Older app** does what **Previous app** does, and **Newer app** announces "Recent apps unavailable".
- **Write an app's name to open it**, in **System actions**, opens a writing surface on the whole screen. Write the start of the name of an app with one finger, a letter or several at a time, and SandFly speaks the first app whose name, or a later word of it, starts with what you wrote, and how many apps do: "WhatsApp, 1 of 3" for W H A. Case, spaces and accents don't count. Letters that leave no app are dropped, and SandFly announces "No apps". An app opens only when you open it, also when it is the only match. Swipe up with two fingers for the next app, down with two fingers for the previous app, left with two fingers to delete the last letter, and tap with two fingers to hear the app again. Swipe right with two fingers to switch between the language of the device and English, for app names in Latin letters. Double-tap with one finger to open the app, or swipe down with three fingers to open it in split screen. A tap closes the surface, and so do 10 seconds without a touch. Not on watches.
- **Type an app's name in braille to open it**, in **System actions**: see [Braille commands](#braille-commands).
- **Increase media volume** and **Decrease media volume**, in **Media and volume**, change the media volume a step and speak the new volume.
- **Scroll to top** and **Scroll to bottom**, in **Scrolling**, scroll the list that focus is in to its start or end a page at a time, then focus its first or last item. In a list that doesn't scroll, such as a short settings screen, they focus its first or last item. Outside a list, they use the first list on screen, or the first or last item on screen when there is none. A touch, gesture or key stops the scroll. Not on watches.
- **Refresh**, in **Scrolling**, scrolls the list that focus is in to its top and pulls it down, which refreshes it in most apps. **Scroll back** and **Scroll down** pull a list down too when it is already at its top, as scrolling with two fingers does. SandFly sends the pull as a swipe that goes to the app as it is, while your own touches are still explored, so it needs Android 13 or later. The same kind of swipe is sent for Home on the lock screen in the VoiceOver gesture layout, a two-finger swipe up that unlocks the phone or shows the PIN, pattern or password, as Home does on an iPhone, and, while SandFly handles two-finger swipes, for the gesture for **Scroll up**, a two-finger swipe up by default: on the lock screen it sends a two-finger swipe up, and on the home screen a one-finger swipe up, which opens the apps list. A two-finger swipe up assigned another command runs that command there.
- **System quick settings**, **System notification shade** and **System overview**, in **System actions**, open the quick settings, notification shade and overview of the system while **Use SandFly's quick settings**, **Use SandFly's notification list** or **Use SandFly's overview** is on.
- **Read from focused item by reading control**, in **Reading**, reads from the focused item, and then only the items of the selected reading control, such as each heading or each link.
- **Speak position** and **Speak coordinates**, in **Reading**. **Speak position** says the position in a list or table, the cursor in text, such as "Character 120 of 800, paragraph 2 of 5, 15%", and how far the view around the focused item is scrolled, such as "Scroll position 40%".
- **Report AI response**, in **Image and screen descriptions**, opens a problem report with the last text that an AI model gave. See [Feedback](#feedback).
- **Share image of focused item** and **Share screenshot**, in **Image and screen descriptions**, take a screenshot of the focused item or of the whole screen and open the Android share sheet with it. They need Android 11 or later.
- **Activate confirm button** and **Activate cancel button**, in **Activation**, activate a button on the screen by its label without moving focus, such as **OK**, **Send** or **Allow**, or **Cancel**, **Close** or **Not now**. SandFly looks in the active window first, and activates the matching button nearest the focused item. **Button labels**, on the screen of each command in **Commands**, replaces the built-in labels. When no button matches, SandFly announces "No confirm button" or "No cancel button".

### Collapsed categories

When you choose a command for a gesture, the categories of commands are collapsed, except the category with the current command of the gesture, and the list starts with **Search**. The categories on the **Gestures**, **Reading controls** and **SandFly menu** screens are collapsed too, and expand when you activate them.

## Speech

For the speech settings, see the user guide chapter [Speech](docs/user-guide/en/06-speech.md).

### Speech settings

In TalkBack, **Text-to-speech** opens the system speech settings of Android. SandFly has its own **Speech** screen. On it, **Text-to-speech engine** chooses an engine for SandFly apart from the default engine of Android, and sliders set **Speech rate**, from 10% to 600%, **Speech pitch** and **Speech volume**. **Speech rate step** sets how much the rate changes at a time with the **Speech rate** sliders, the **Speech rate** reading control and the speech rate commands: **10% of the rate**, the default, or **1%**, **2%**, **5%**, **10%** or **20%**. **System text-to-speech settings** opens the system settings. The screen also holds the settings in the following sections, in the groups **Voice**, **Languages**, **Pronunciation**, **Stopping speech** and **Other apps**.

Some engines play their audio themselves, such as engines that pass text on to other engines. SandFly detects them and plays their speech the usual way instead of through low-latency audio.

### Speech engine reading control

The **Speech engine** reading control switches between your installed speech engines when you swipe up or down. It changes the engine of **SandFly default**, so it's unavailable while a voice profile is in use. It isn't in the reading controls by default.

### Voice profiles

A voice profile is a set of speech settings that you can switch to at once: a speech engine, language, voice, volume, rate, pitch, voice effects and **Send long text a sentence at a time**. For example, one profile can use another engine for long text while **SandFly default** stays fast. To add one, go to **Speech** > **Voice profiles** and choose **Add voice profile**. A new profile starts with the volume, rate, pitch, voice effects and **Send long text a sentence at a time** of **SandFly default**. When you choose a language, the voice list shows only the voices of the engine in that language. **Keep the engine started**, on the screen of a profile, keeps its engine running while another engine is in use and the screen is on, so that switching to the profile doesn't wait for the engine to start.

To switch profiles:

- **Speech** > **Voice profiles** > **Voice profile in use**.
- The **Voice profile** reading control, which isn't in the reading controls by default.
- The **Voice profile** item of the SandFly menu, which lists the profiles and announces which is in use.
- The **Choose voice profile**, **Next voice profile** and **Previous voice profile** commands, in the **Speech** category, and **Use voice profile** for **SandFly default** and each profile, in the **Voice profiles** category.

The reading control goes through the profiles in the order of the **Voice profiles** screen, after **SandFly default**. To change the order, touch and hold a profile and drag it, or use the **Move up** and **Move down** actions; SandFly announces which profile it moved above or below. Profiles also have **Rename** and **Delete** actions, which a long press shows too. **Keep the engine of the previous voice profile started**, on the **Voice profiles** screen, keeps the engine of the profile used before the current one running while the screen is on.

**SandFly default** is always in the list, and uses the settings of the **Speech** screen. While a profile is in use, changing the speech rate or pitch with commands or reading controls changes that profile only. All speech uses the engine and voice of the profile, so SandFly doesn't switch language or dialect, except for languages that have a voice of their own in **Voices by language**, and the **Spoken language** reading control has no value.

Voice profiles are also used for notifications, emoji, languages and braille keyboard languages. See [Reading notifications](#reading-notifications), [Emoji voice and rate](#emoji-voice-and-rate), [Voices by language](#voices-by-language) and [Next braille table](#next-braille-table).

### Voices by language

**Speech** > **Voices by language** lists the languages you read, each spoken with **SandFly default**, using the voice of its engine for that language, or with a voice profile. **Add language** lists the languages of all installed engines. When two or more of the listed languages share a script with each other or with the language of the voice, such as English and French in Latin script, the screen shows a list for that script, such as **Latin script**, which sets the language of text in that script when nothing else identifies it.

### Automatic language and dialect changes

When text is marked as another language, TalkBack switches the voice to that language. SandFly lets you turn this off. In **Speech** > **Languages**, **Switch language automatically** controls changes to another language, and **Switch dialect automatically** controls changes to another regional form of the language of the voice, such as British English text with a US English voice. Both are on by default.

With a change turned off, SandFly doesn't split the text or pause around it. Text marked as the language and country of the voice, such as US English for a US English voice, isn't split either, whatever the settings, since the voice doesn't change. SandFly compares languages by their codes only, so Serbian in Latin and in Cyrillic script counts as two dialects, and Norwegian marked as `no` and as `nb` counts as two languages.

With **Switch language automatically** on, SandFly also finds the language of text that apps don't mark from its script, such as Arabic or Greek letters in English text, and from letters that only one of your languages uses. **Speech** > **Languages** > **Identify languages from words**, off by default, also distinguishes languages written in the same script by their words, with on-device language identification from Google Play services, for pieces of text of at least 12 letters.

The **Switch language automatically** and **Identify languages from words** commands, in the **Speech** category, turn those settings on or off, and announce "Switch language automatically on" or "off", and "Identify languages from words on" or "off".

### Voice effects

**Speech** > **Voice effects** sets an 11-band equalizer and compression effects for **SandFly default**, and **Voice effects** on the screen of a voice profile sets them for that profile. **Preset** chooses **Flat**, **Clarity**, **Bass boost**, **Bass cut**, **Treble boost**, **Treble cut**, **Warm**, **Reduce harshness**, **Reduce sibilance**, **Small speaker** or **Deep bass**, then your saved presets; a preset sets the bands and the effects. **Bass compression**, **Voice compression** and **Treble compression**, each **Off**, **Low**, **Medium** or **High**, compress speech below 200 Hz, from 200 Hz to 4 kHz, or above 4 kHz and raise its level. Each equalizer band, from 64 Hz to 16 kHz, has a slider from -12 to +12 dB. Changing a slider or an effect sets **Preset** to **Custom**. **Save as preset** saves the bands and effects as a named preset, up to 50, which **Preset** lists for **SandFly default** and every voice profile. **Saved presets** renames or deletes them; deleting a preset doesn't change voice effects set to it. **Export settings** and settings profiles include saved presets. **Use voice effects** turns voice effects off without changing them. When a band boosts or an effect is on, a limiter keeps speech from clipping.

The **Voice effects** reading control turns the voice effects of the voice in use on or off, and each equalizer band has a reading control, such as **Equalizer 1 kHz**: swipe up or down to change its gain by 1 dB. They aren't in the reading controls by default. The **Use voice effects** command, in the **Speech** category, turns voice effects on or off.

Voice effects apply to speech that SandFly plays through **Audio output** > **Low-latency audio**. With **Low-latency audio** off, they apply as an audio effect on Android 9 and later where the device supports it. They don't apply during calls or while the microphone records, when the engine plays speech itself. Voice effects aren't on watches.

### Emoji voice and rate

**Speech** > **Emoji voice** speaks emoji with a voice profile, or with **Same as speech**, the default. **Speech** > **Emoji speech rate** sets the rate of emoji as a percentage of the speech rate, from 50% to 300%. Both apply whether the speech engine reads the emoji or SandFly replaces them with their names. See [How emoji are spoken](#how-emoji-are-spoken).

### Speak letters phonetically

The **Speak letters with examples** switch of TalkBack is two settings in SandFly, in **Speech** > **Pronunciation**: **Speak letters phonetically when reviewing**, for letters you move over by character and spelled text, and **Speak letters phonetically when typing**, for keyboard keys and letters you type or delete. Each has four choices:

- **Off** speaks only the letter.
- **After the letter, delayed**, the default, is what the TalkBack switch does when it is on. Keyboard keys speak the letter, then its phonetic word, as in "h, hotel". Moving by character, the word comes a quarter of a second after the letter, and moving on before then skips it.
- **After the letter, no delay** speaks the letter and its word together, as in "h hotel".
- **Instead of the letter** speaks only the word, as in "hotel".

With **After the letter, no delay** and **Instead of the letter**, SandFly speaks the word in all of these places. **After the letter, delayed** still leaves it out for **Spell last spoken phrase**, deleting a letter, and inserting a letter with a touch keyboard such as Gboard, as TalkBack does with **Speak letters with examples** on.

### Speech dictionary and symbol dictionary

**Speech** > **Pronunciation** > **Speech dictionary** changes how text is spoken in all speech, such as a word said as another word, or a symbol said as a space. Each entry has **Text**, **Say as**, **Whole words only**, on by default, and **Regular expression**, and can be limited to one app with **Choose an app**. Entries apply in order. SandFly settings screens are read as written.

**Speech** > **Pronunciation** > **Symbol dictionary** replaces symbols and sequences of symbols, in speech and on a braille display, such as a period replaced by a space. An entry matches its exact text anywhere, including directly after a word, and applies after the speech dictionary. An empty replacement removes the symbol. A symbol read alone, such as during navigation by character, is still spoken by its name. Up to 500 entries.

Both dictionaries have **Import from file** and **Export to file**. See the [item file reference](docs/reference/item-files.md).

### Line breaks read as spaces

With **Speech** > **Pronunciation** > **Read line breaks as spaces** on, SandFly reads text without the pauses at its line breaks. It's off by default.

### Resume speech where you paused it

With speech engines that don't report word positions, such as RHVoice and Gryphon, TalkBack starts the text over when you resume speech. SandFly measures how fast the engine speaks from the items that it finishes, estimates how far the engine got when you paused, and resumes from the start of that sentence or phrase. It can repeat a few words, but it never skips any. Engines that report word positions resume from the word, and with [low-latency audio](#low-latency-audio) on, speech resumes mid-word.

When speech resumes, SandFly doesn't play the sound and vibration of the item again.

### Two-finger tap right after a swipe

TalkBack doesn't recognize a two-finger tap made right after a swipe, or pauses older speech instead of the current item. SandFly recognizes the tap and pauses the speech for the item that you swiped to.

### Keys that stop speech

In TalkBack, only Ctrl on a physical keyboard stops speech, so that a window name or an announcement holds up the item that a key moved to. In SandFly, every key on a physical keyboard stops speech, except modifier keys, lock keys and the volume keys. Shortcuts with Ctrl, Alt or Meta always stop it, and Space outside an edit field always stops it. In **Speech** > **Stopping speech**:

- **Interrupt speech when typing** sets when typed characters stop speech: **Always**, the default, **In edit fields**, or **On keyboard echo**, only when the keyboard echo speaks the character or the word it ends.
- **Interrupt speech on Enter**, on by default.

Neither is on watches. A window name that a key stopped isn't spoken later. Pressing Ctrl alone pauses speech, and SandFly keeps the key from the app, so that an app such as YouTube doesn't show its keyboard shortcuts. A shortcut with Ctrl still reaches the app. An app that needs Ctrl alone, such as a remote desktop app, doesn't receive it.

### Speech while an app records

While another app records from the microphone without headphones, and during calls, TalkBack drops announcements that you request with a gesture or key. SandFly speaks them: the time and date, the battery, the status, the window title, phonetic letters, **Copy item details**, and turning scripts on or off.

With **Speech** > **Stopping speech** > **Quiet while recording** on, SandFly is quiet while an app records without headphones, except for speech within 2 seconds of a touch, gesture, key or braille input, so that notifications and other speech that you didn't request aren't recorded. It's off by default.

### Speech history

The **Speech history** command, in the **Reading** category, opens a list of the last 100 texts that SandFly spoke, newest first. Double-tap a text for **Copy**, and for **Show all** when the list shortens the text. **Clear history** removes every text. The history is kept in memory only, is never saved or logged, and is cleared when SandFly turns off. It leaves out speech while a password field has focus, speech while the list is open, and the hints and messages that **Repeat last spoken phrase** also leaves out. Each text is kept to 2,000 characters.

### Repeat last spoken phrase slower

The **Repeat last spoken phrase slower** command, in the **Reading** category, speaks the last spoken phrase again once, slower, in the same voice, including a voice profile from **Voices by language**. The speech rate doesn't change, so the next item is spoken at the usual rate. **Speech** > **Repeat slower by** sets how much slower, from 5% to 50%; the default is 15%. Emoji spoken with their own voice profile keep the emoji speech rate.

### "SandFly off" at the right volume

When you turn SandFly off, it speaks "SandFly off" at the accessibility volume, using the real volume levels of the device. TalkBack only approximates that volume.

## Verbosity

For these settings, see the user guide chapter [Verbosity](docs/user-guide/en/07-verbosity.md).

### No verbosity presets

TalkBack has **Choose a preset**, with **High**, **Low** and **Custom** verbosity. SandFly has no presets: you set each setting on the **Verbosity** screen, in the groups **Items**, **Text and symbols**, **Tables**, **Announcements** and **Apps with their own speech**. Keyboard echo is on the **Typing** screen, and phonetic letters and **Use pitch changes** are on the **Speech** screen.

### Item type without a pause

With **Verbosity** > **Speak item type without a pause** on, SandFly joins the name and the type of an item with a space instead of a pause. It's off by default.

### Actions available

TalkBack says that an item has actions only in its usage hint. SandFly also has:

- **Verbosity** > **Speak Actions available**: says "Actions available" after an item with actions of its app, with usage hints on or off. With usage hints on, the hint then gives only the gesture that shows the actions, such as "Use tap with 2 fingers to view actions". Off by default.
- The **Actions available** sound, two short rising tones. **Sound and vibration** > **Actions available sound** turns it **Off** or plays it **When the item gets focus**, after the focus sound, which is the default, or **After the item is spoken**. It has a switch in **Individual sounds and vibrations**, a sound theme can replace it, and a theme can give it a vibration; it has none of its own.

Both are for items with custom actions of their app, such as Reply and Forward on a chat message. Standard Android actions, such as activating, long pressing, scrolling and expanding, don't count, so an ordinary button has neither. Keys of on-screen keyboards don't count either.

### Order of item details

**Verbosity** > **Order of item details** sets the order in which SandFly speaks the name, type and state of an item. TalkBack has this setting in **Advanced settings**, as **Element description order**, with three orders to choose from. SandFly lists **State**, **Name** and **Type**, and you put them in any of the six orders: touch and hold a detail and drag it up or down, or use its **Move up** and **Move down** actions. With SandFly on, double-tap and hold a detail, then drag it. SandFly announces the position of the detail as you pick it up, move it and drop it. The default is **State, name, type**, as in TalkBack.

TalkBack ignores the order for a row whose checkbox or switch has no text of its own, such as the rows of the **Reading controls** screen, and always reads the type and state of the checkbox before the text of the row. SandFly reads such a row as one control, with the text of the row as its name, in the order that you chose. A button or tab whose label is a separate view inside it, as in the power menu of a Pixel, also follows the order, with the label as its name.

### Text formatting

TalkBack speaks text formatting, such as bold, underline and text color, by default. In SandFly, **Verbosity** > **Speak text formatting** is off by default.

### Punctuation left out

**Verbosity** > **Speak punctuation and symbols** has **None** besides **All**, **Most** and **Some**. **None** leaves punctuation and symbols out of speech, except a character read on its own, such as when moving by character. Apostrophes and hyphens inside words, separators inside numbers, and punctuation that ends a word are left to the speech engine. **Say symbols that change meaning**, shown while the setting is **None** and off by default, also leaves to the speech engine a plus or minus sign directly before a number, degree, percent, per mille and currency signs, and + − × ÷ = < > between numbers.

### How emoji are spoken

Some speech engines read emoji badly, use names of their own, or don't read them at all. **Verbosity** > **Emoji** sets how they're spoken:

- **Read by speech engine**, the default, gives the engine the emoji, as TalkBack does.
- **Read by SandFly** replaces each emoji with its name from the [Unicode CLDR](https://cldr.unicode.org/), in the language that the text is spoken in, for 138 languages and regional variants. A skin tone, a family, a flag or a keycap is one emoji with one name.
- **None** leaves emoji out, except in text that is only emoji, such as a reaction button or a character, which is still named.

With **Read by SandFly**, **Verbosity** > **Count repeated emoji** speaks an emoji repeated in a row once with a count, such as "3 grinning face", from **2 or more** to **6 or more**; it's **Off** by default. Moving by character names a whole emoji once, and moving by word stops on emoji. Copying or spelling the last spoken phrase gives the emoji, not their names. **Verbosity** > **After emoji names** sets what follows each emoji: **Nothing**, the default, **Say "emoji"**, or **Play a sound**. **Emoji** is also a reading control, a SandFly menu item and a command.

With **None**, moving by word skips emoji, while moving by character still names them. Emoji newer than Emoji 18.0, which the names of SandFly cover, are left to the speech engine. Emoji without a CLDR name in a language are spoken in English.

### Table column headers

**Verbosity** > **Tables** > **Read column headers** sets whether SandFly reads table column headers before or after the data of the cell, or leaves them out:

- **After cell data**, the default, keeps the order of TalkBack: the contents of the cell, then the row and column headers or numbers.
- **Before cell data** speaks the row and column headers or numbers before the contents of the cell, with "Column heading" or "Row heading" for a header cell. This applies to grids as well as tables, so that an item in a photo grid speaks its row and column first. On a TV, SandFly already speaks them first, so this choice changes nothing there.
- **Do not read** leaves column headers out, and reads column numbers only while **Speak row and column numbers** is on.

**Verbosity** > **Tables** > **Speak row and column numbers**, on by default, can be turned off to hear only named headers without row and column numbers.

Both settings apply only while **Verbosity** > **Speak grid and table information** is on. That setting covers grids, staggered grids, and pagers laid out in rows and columns, such as a folder on a home screen.

### Number of list items while scrolling

With **Verbosity** > **Always speak number of list items** off, the default, SandFly speaks no list position when a list scrolls. TalkBack still speaks it after its scroll commands and after the page of a pager changes. With the setting on, the position isn't spoken while audio or video plays.

### Apps with their own speech

Some apps and keyboards have their own text-to-speech output, such as an assistant that plays its answer as speech while it moves accessibility focus to each new piece of text, or a keyboard that speaks its keys. TalkBack speaks the same content at the same time. The settings in **Verbosity** > **Apps with their own speech** leave this speech out. Only speech is left out: focus still moves, and sounds and vibrations still play. SandFly screens are always spoken.

- **Speak only focus moved by the user**: **Off**, the default, **In all apps**, or **In chosen apps**, with the apps listed in **Chosen apps**. While it applies, SandFly speaks focus that you move by touch, gestures, keys, a braille display or commands, and doesn't speak focus that the app moves, announcements, live region changes, window names or window change announcements. Focus that SandFly moves after a window changes, after a scroll, or to follow input focus counts as moved by the app, including input focus that an app moves when you press Tab or another key that the app handles. The setting is also a reading control, and a command that turns it off, or back on with **In chosen apps** when there are chosen apps and **In all apps** otherwise.
- **Speak only focus moved by the user while an assistant or call plays audio**: does the same in all apps while another app plays audio as an assistant or a call, and while an assistant window, such as Gemini over an app or the lock screen, is shown. The screen-on and unlock readouts are left out then too, and speech stops when an assistant window opens. Focus that you move is still spoken during calls. When SandFly speech uses the call volume, its own audio can't be told apart from the call audio of an app, so an app call counts only while Android is in call mode. Off by default.
- **Keyboards with their own speech**: while a keyboard in this list is shown, SandFly doesn't speak its keys, the text typed or deleted, or the cursor moving in the edit field. Gestures and commands still work, and moving through text with reading controls is still spoken.

### Screen on and off announcements

**Verbosity** > **Screen on and off** sets what SandFly speaks when the screen turns off, when it turns on, and when you unlock the phone. **When the screen turns off**, **When the screen turns on** and **When the phone is unlocked** each have **Time**, **Number of notifications**, **Battery** (**Off**, **Always** or **Only while charging**), **Low battery** (**Off**, or below 10% to 50%), **Hidden screen**, **Wi-Fi**, **Mobile network**, **Ringer and Do Not Disturb** and **Airplane mode**, spoken in that order in one announcement. **When the screen turns off** also has **Say "Screen off"** and **Ringer mode**, which speaks "Ringer vibrate" or "Ringer silent"; with **Ringer and Do Not Disturb** on there, **Ringer mode** isn't spoken. **When the phone is unlocked** also has **Say "Device unlocked"**.

By default, SandFly speaks "Screen off" and the ringer mode when the screen turns off, the time and the number of notifications when it turns on, and "Device unlocked" when you unlock the phone. The number of notifications, as in "6 notifications", needs notification access and counts notifications that aren't ongoing. On a flip phone, waking the cover screen counts as the screen turning on. With face or fingerprint unlock, the phone is unlocked right after the screen turns on; items that the screen-on announcement spoke less than 5 seconds before aren't spoken again on unlock.

These items are separate from **Status readout**, so that **Speak status** can read other things. During calls, SandFly speaks only "Screen off". The lock screen doesn't cut off the screen-off announcement. Media playback silences the screen-off and unlock announcements unless **Speak during media playback** is on; it's off by default. The screen-on announcement is always spoken during media playback.

### Battery, call and time announcements

In **Verbosity** > **Announcements**:

- **Announce full charge**: SandFly speaks "Battery fully charged" and plays the **Full charge** sound once when the battery is full while the charger is connected, also with notification reading off. Off by default.
- **Battery level**: SandFly speaks the battery level each time it reaches a multiple of 5% or 10%, or every 15 minutes, 30 minutes or hour, charging or not, and not while the screen is off. **Off** by default. **Battery level voice** is **Notification voice**, **SandFly's voice**, the default, or a voice profile.
- **Announce keyboard shown or hidden**: SandFly speaks when an on-screen keyboard is shown or hidden. On by default.
- **Announce call length**: when a phone call ends, SandFly speaks how long it lasted, from when the call was answered or dialed. Off by default.
- **Include seconds in the time**: the time that SandFly speaks includes the seconds. Off by default. **Include seconds when the screen turns on**, on by default, applies it to the time spoken when the screen turns on.
- **Time format**, which TalkBack has in **Advanced settings**: **Use device time format**, **12 hour** or **24 hour**.

## Sound and vibration

For these settings, see the user guide chapter [Sound, vibration and audio output](docs/user-guide/en/08-sound-and-vibration.md).

### A vibration for every sound

As in VoiceOver, SandFly can convey every action by vibration alone. Every sound has a vibration of its own, which plays also with **Sound feedback** off, so that you can turn all sounds off and still feel each action. No two actions share a vibration.

Frequent actions are a single hit: a soft tap when focus lands on an item, a firm click on an item that you can activate, and a softer tap over an empty area. Others have a rhythm of their own. The end of a list, or a completed action, is two firm knocks. Entering a list is two light taps with the second one stronger, and leaving a list is two light taps with the second one weaker. A window change is three soft taps with the middle one strongest. When SandFly scrolls a list, three quick ticks play. When the circle menu opens, one tick plays for each item. The loading tone, which repeats while an image or screen description loads, has a short heartbeat that is firm enough to feel through a case.

Progress bar tones, the sound for capital letters, and the braille display and braille keyboard sounds don't vibrate. The braille keyboard has vibrations of its own, which **Vibration feedback** turns on and off with the rest.

In **Sound and vibration**:

- **Sound feedback volume** is a slider from 5 to 100 percent in steps of 5, 50 percent by default, instead of a list of 25, 50, 75 and 100 percent.
- **Vibration strength** sets how strong vibrations are, from 5 to 200 percent, 100 percent by default, on devices whose vibrator can vary its strength.
- **Vibration type** sets how vibrations play: **Haptic effects**, composed effects such as clicks and ticks, on Android 11 and later with a vibrator that supports them; **Pulses**, vibrations of varying strength; or **On and off**, the vibrator at full strength. The list shows only the types that the vibrator can play, and the setting is hidden when only one is available. If vibrations feel weak with **Haptic effects**, choose **Pulses** and raise **Vibration strength**.

### Ticks while you scroll with two fingers

TalkBack plays its scroll sound and vibration at most every 250 ms while you scroll. SandFly plays one light tick for each item that scrolls past, so that a slow drag ticks slowly and a fast drag ticks quickly, with the scroll sound at most every 250 ms. The pitch of the scroll sound rises from the top of the list to the bottom. Lists that don't report their items tick once for every 48 dp of scrolling. The first tick plays as soon as the drag reaches the app, and SandFly passes the drag to the app sooner than TalkBack does. Scrolls that SandFly makes itself play the scroll sound only.

### Individual sounds and vibrations

To turn off one sound or one vibration and keep the rest, go to **Sound and vibration** > **Individual sounds and vibrations**. There's a switch for each sound and each vibration, including the braille display and braille keyboard sounds, the braille keyboard vibrations, the announcement vibration and the screen-off sound. Turning off a sound keeps its vibration, and turning off a vibration keeps its sound. To play one, open the actions menu on its switch and choose **Preview**. **Sound feedback** and **Vibration feedback** still turn all of them off at once.

### Sound themes

A sound theme is a set of sounds and vibrations that replace the built-in ones, with control sound and 3D audio settings of its own. You can install as many themes as you like and switch between them in **Sound and vibration** > **Sound themes**. **Install a theme** has **From a file**, **From a link**, including a link to a GitHub repository that contains a theme, and **From a source**, a list of repositories on GitHub, GitLab, Codeberg, Gitea or Bitbucket that starts with the SandFly themes repository. You can also open or share a theme file, or a link to one, with SandFly from another app. On watches, themes install only from a link.

To replace single sounds with your own, open **Sounds** under the settings of the theme in use. **Choose from folder**, in the dialog of each sound, lists the sound files and subfolders of a folder and plays the file in focus. **Create new theme** makes a theme, and **Save as a theme file** saves a theme to share. For the theme file format, see [Sound themes](themes.md).

### Control sounds and vibrations

As the [Unspoken](https://github.com/ahicks92/Unspoken) add-on for NVDA does, SandFly can play a sound for each kind of element in place of the focus sound, and leave out words such as "button" and "checkbox". There are sounds and vibrations for buttons, checkboxes (also switches and toggle buttons), radio buttons, edit fields, drop-down lists, sliders, images, clocks, tabs, menu items and list items, and on web pages, links and tree items. To turn them on, go to **Sound and vibration** > **Sound themes** and turn on **Control sounds and vibrations** under the settings of the theme in use. It's off by default.

Each kind of element has a short sound and a vibration, which a sound theme can replace. A kind of element whose sound is turned off, or set to no sound in the theme, plays the focus sound, and SandFly speaks the kind of element. A control vibration plays also with **Sound feedback** off, so that you can tell controls apart by vibration.

A row that isn't a control itself, such as a row in Settings, plays the sound of the control in it, such as its switch, and otherwise the sound for its kind of item, such as a list item. Plain text and keys of the on-screen keyboard keep the usual focus sound. Moving into or out of a list plays the list sound and the control sound together, with the control vibration.

Each theme has its own **Still say the kind of control** setting, on by default only for the **SandFly** theme, which speaks the kind of control after its sound. Each control sound and vibration also has its own switch in **Individual sounds and vibrations**.

### 3D audio

Each control sound comes from where the element is on the screen: from the left for elements on the left, and from higher up for elements near the top. With headphones, the sounds play in 3D, using measurements of how a real head receives sound from each direction (the MIT Media Lab KEMAR measurements, by Bill Gardner and Keith Martin). On the phone speaker, the sounds move only between left and right.

**3D audio**, under the settings of each theme, is **With headphones**, the default, **Always**, **Never, only left and right**, or **With headphones, centred otherwise**, which plays 3D audio with headphones and plays the sounds from the centre on other outputs. With **With headphones**, on Android 13 and later, SandFly uses 3D when its audio plays on wired or USB headphones, a hearing aid, a Bluetooth media device or a Bluetooth LE headset. Bluetooth LE speakers, broadcasts and call audio don't count. On earlier versions, any connected headphones count.

### Navigation sounds

TalkBack plays a sound and vibration when a gesture starts and another when it ends. SandFly plays neither. In their place, it has these sounds, each with a vibration, which a sound theme can replace:

- **Next reading control** and **Previous reading control**: another reading control is selected, with a gesture, the rotor, the circle menu, a keyboard shortcut or the braille keyboard.
- **Edge gesture bottom**, **Edge gesture middle** and **Edge gesture top**: a finger touches an edge where an edge gesture starts, the gesture reaches its first click, and the gesture reaches its second click. The edge plays a low tick, the first click a higher tone, and the second click two rising tones. Sliding back plays the sound of the stage that the finger returns to. Lifting plays nothing.
- **Insertion point moved**: moving by character, word, line or paragraph moves the cursor in a text field that you edit.
- **Column wrap** and **Column wrap back**: a swipe forward moves to an item on a row below the focused item, or a swipe back to an item on a row above it. Each item of a list is a row of its own; items side by side, such as icons on a home screen, cells of a table, or a title with buttons beside it, share a row. The sound plays with the focus sound, and its vibration replaces the focus vibration. An item is on another row when its middle is below the bottom of the focused item, or above its top. Column wrap doesn't play for swipes that move from one window to another.

Each navigation sound has its own sound and vibration switch in **Individual sounds and vibrations**.

### Sound mode reading control

The **Sound mode** reading control sets the ringer to sound, vibrate or mute when you swipe up or down. Vibrate is left out on devices without a vibrator. Turning mute on or off needs **Do Not Disturb access**; without it, SandFly opens that setting. It isn't in the reading controls by default.

### Volume reading controls

**Media volume**, **Call volume**, **Ring volume**, **Notification volume** and **Alarm volume** are reading controls, like the **Accessibility volume** reading control of TalkBack. Swipe up to raise the volume one step and down to lower it; SandFly speaks the new volume as a percentage. Selecting one speaks its volume. They don't need Shizuku, and aren't in the reading controls by default. Changing the ring or notification volume to or from silent needs **Do Not Disturb access**; without it, SandFly announces that the access is missing. The **Setting values** category has next and previous value commands for each of them except **Media volume**, which has its own commands in **Media and volume**.

## Audio output and calls

TalkBack has no settings for where speech plays. For the **Audio output** screen, see the user guide chapter [Sound, vibration and audio output](docs/user-guide/en/08-sound-and-vibration.md).

### Audio output device

**Audio output** > **Audio output device** sets where SandFly speech and sounds play. **System default**, the default, follows Android. **Phone speaker** plays on the phone speaker also while Bluetooth or wired headphones are connected. You can also choose a connected Bluetooth, wired, USB or hearing aid device by name.

A Bluetooth device that you choose receives audio through Android routing, as with **System default**, so that the device doesn't switch to call mode; if Android routes audio to another device while it is connected, the audio stays on the other device. Wired, USB and hearing aid devices receive audio directly. The choice stays when devices connect or disconnect and when other apps start playing. While the chosen device isn't connected, the setting shows its name with "(disconnected)", and audio follows Android routing. During phone calls, and while another app uses the microphone for a call or a voice assistant, Android chooses the output. The **Audio output device** reading control changes the setting with a swipe up or down.

### Phone speaker with some devices

**Audio output** > **Phone speaker with these devices** lists the Bluetooth, wired and USB outputs connected now and those connected before while SandFly was on. While a device that you check is connected, SandFly plays on the phone speaker, whatever **Audio output device** is set to: for example, on the speaker while music plays on a Bluetooth speaker, and in your ears with earbuds that you leave unchecked. Devices are listed by name, so two devices with the same name are one choice.

### Speech kept on the phone while casting

**Audio output** > **Keep speech on the phone while casting** stops screen casting and mirroring from sending SandFly speech and sounds to the TV or speaker. Screen recordings then have no SandFly audio either. It's off by default and requires Android 10 or later.

### Volume control for speech

TalkBack always plays speech at the accessibility volume. **Audio output** > **Volume control for speech** sets the volume that speech and SandFly sounds play at:

- **Accessibility volume**, the default.
- **Media volume**.
- **Ring volume**: speech plays as a ringtone does, so that it stays on the phone while media is cast to another device. Like a ringtone, it can also play on the phone speaker while headphones are connected, and it is silent while the ringer is silent.
- **Call volume, earpiece**: speech plays as a call does, on the earpiece when no headset is connected.

While **Audio output device** sends audio to the phone speaker or to a wired, USB or hearing aid device, speech uses the accessibility volume. While speech doesn't use the accessibility volume, Android plays the accessibility sounds of other apps, such as the tones of voice typing, at the media volume. The **Speech volume** reading control switches between these volumes.

### Speaker when away from your ear

As VoiceOver on iPhone does, SandFly can move a call to the speaker when you take the phone away from your ear, and back to the earpiece when you hold it up again, with the proximity sensor. A call that starts with the phone away from your ear goes straight to the speaker. Bluetooth and wired headsets stay in use, and if you turn the speaker on or off in the Phone app, your choice stays until you move the phone again. It works only while SandFly is on and not paused.

Android lets only certain apps, such as smartwatch companions, change where call audio plays, so SandFly needs a permission for it. With [Shizuku](https://shizuku.rikka.app) or root, turn on **Audio output** > **Speaker when away from your ear**, and SandFly grants the permission itself; the first time, Shizuku shows a request to allow SandFly. If the permission can't be granted, the setting turns back off. Without Shizuku or root, grant the permission once with adb, then turn on the setting:

```
adb shell appops set io.github.aaron_gh.sandfly MANAGE_ONGOING_CALLS allow
```

Until you grant the permission, the setting is unavailable and shows the command, and its **Copy command** action copies it. The setting is off by default and requires Android 12 or later.

### Reset accessibility audio after calls

During a call, Android sends accessibility audio to the audio device of the call. Some phones keep that device after the call ends: on a Galaxy Z Fold, after a WhatsApp call, speech played on the earpiece, and music on Bluetooth headphones stopped each time SandFly spoke.

With **Audio output** > **Reset accessibility audio after calls** on, SandFly resets the accessibility audio output when Android sends it to the earpiece outside a call. The reset needs the shell user, so it runs through [Shizuku](https://shizuku.rikka.app) or root; turning the setting on shows the Shizuku request to allow SandFly. Without them, the setting shows an adb command that resets the output once, and its **Copy command** action copies it. The command names the installed APK, so copy it again after SandFly updates. The setting is off by default and requires Android 13 or later.

## Notifications

For these settings, see the user guide chapter [Notifications](docs/user-guide/en/14-notifications.md).

### Reading notifications

TalkBack reads each new notification in a fixed format and has no settings for it. In SandFly, **Notifications** is a screen of the main settings. **Notifications** > **Speak notifications**, on by default, turns reading new notifications on or off. Incoming calls are always read. The other screens are:

- **Reading notifications**: **Speech format**, with the placeholders `{app}`, `{title}`, `{text}`, `{content}`, `{bigtext}`, `{subtext}`, `{ticker}`, `{category}` and `{time}`, and **Private notifications say**. **Voice** reads notifications with one of your [voice profiles](#voice-profiles), with all of its settings, instead of the SandFly voice. **Detect language** reads each notification in the language it is written in. Also a sound before notifications, emoji, capitals, long numbers, links, length limits in words, sentences, characters or seconds, a wait before reading, what happens when a notification arrives while another is read, and reading again until the screen turns on.
- **Apps**: for each app, **As other apps**, **Read**, **App name only**, **Read first** or **Don't read**, and a spoken name, a minimum time between notifications, a speech format and a **Voice** of its own.
- **Words and replacements**: words that a notification must have to be read, words that keep it from being read, words that leave only the app name, and text to say differently.
- **Kinds of notifications**: skip media players, ongoing notifications, running app notifications, progress notifications, group summaries, and low priority and system notifications.
- **When to read**: with the screen on, off or locked, with or without headphones, while media plays, in Do Not Disturb, while the screen is touched, during calls, in silent, vibrate or car mode, and quiet hours.
- **Rules**: conditions, such as a Bluetooth device or Wi-Fi network, the battery, charging, the time, the app in front, the app or text of the notification, and actions, such as not reading it, reading only the app name, another speech format, a voice profile, or reading it first. The voice of a rule takes the place of the voice of the app. **Add a rule from a template** starts from rules such as **Silent at night**.
- **Repeats**: skip the same notification for a while, a minimum time between the notifications of an app, and leave out a start that repeats, such as the name of a group in a chat.
- **Stop reading**: shake, wave over the proximity sensor, press both volume keys, dismiss the notification, or the app removes it.
- **Summary and time**: read the notifications in the shade now or every day at a set time, and speak the time at a set interval, in the notification voice, the SandFly voice or a voice profile.
- **History**: the last 100 notifications, each with whether it was read and why not, and actions such as **Don't read notifications like this**.
- **Send a test notification**, **Allow automation apps** and **Automation**, which let apps such as Tasker turn notification speech on or off, stop it, or read the notifications.

When notifications for different voices arrive together, each waits for the one before it, so that two voices don't speak at once. A notification in the voice profile that SandFly speaks with is read after the current speech, in the same speech queue. One in a different voice is also read after the current speech, or at once over it with **Read over SandFly's speech in a different voice**. That setting doesn't apply while **Speak only focus moved by the user** applies to the app in front or while an assistant or call plays audio, and no notification is read while **Quiet while recording** applies.

The **Notification speech** quick settings tile turns reading on or off. The **Notifications** category of commands has **Read notifications**, **Read the last notification again**, **Stop reading the notification**, **Notification speech on or off** and **Time announcements on or off**. **Time announcements on or off** sets **Say the time every**, in **Summary and time**, to **Off**, or back to the interval it had before, or to 1 hour when no interval was set. Several settings need notification access, which **Turn on notification access** on those screens opens.

The speech of an app, such as a screen change, doesn't cut off a notification being read. A gesture, keyboard shortcut or key that you use after a notification starts does: its speech plays at once, and the notification stops, also in the speech engine of a voice profile. With **Stop speech when touching the screen** off, a swipe made 0.5 seconds into a 12-second notification was spoken after 0.9 seconds, instead of after the whole notification.

### SandFly notification list

With **Notifications** > **Use SandFly's notification list** on, the **Notifications** command opens a SandFly list instead of the notification shade of the system, which some phones, such as Samsung phones with many notifications, redraw so often that swiping through it is slow. The list shows each notification with its actions and reply field, groups as one item that shows its notifications when you double-tap it, and **Playing** or **Paused** for media. A notification without text is read with the text of its custom layout, or else its sub text, info text or summary text. Text longer than about 160 characters is cut, and the **Show all** action reads the whole text. **Dismiss** and **Clear all** work as in the shade, and **Quick settings** opens the quick settings.

- **List notifications newest first, without groups**: each notification is an item of its own, newest first. Off by default.
- **Leave media players out of the notification list**: also leaves them out of the notification count spoken when the screen turns on. Off by default.
- **Notification details**: which parts of a notification are read and in what order: title, text, app name, time and playback.

The list needs notification access, and isn't used while the phone is locked. The **System notification shade** command opens the shade of the system.

### Notifications that Android doesn't announce

Android announces a new notification to screen readers only when it alerts. A notification that Do Not Disturb holds back, or an update that an app posts without alerting again, such as a doorbell app replacing its last motion alert, never reaches TalkBack. With notification access, SandFly also receives these notifications, and reads them as the notification settings set, so that **Notifications** > **When to read** > **Do Not Disturb** applies to them too. Progress and ongoing notifications are left out. Incoming calls are listed in the notification history as announced.

While the phone is locked, Android gives screen readers only the public version of a private notification, which many apps don't have, so TalkBack reads nothing. With notification access, SandFly reads the whole notification, as the notification settings set, such as **Private notifications say**. SandFly connects to notifications only while SandFly is on, so that turning SandFly off lets its process end.

### Toasts in Do Not Disturb

While Do Not Disturb is on, TalkBack doesn't speak toasts, the short messages that apps show at the bottom of the screen. SandFly speaks toasts, and applies Do Not Disturb only to notifications.

### No "collapsed" on each notification

SandFly doesn't speak "collapsed" for each notification on the lock screen and in the notification shade. It still speaks "expanded" when you expand a notification, and "collapsed" in other apps.

### No shade name for a heads-up notification

Some phones, such as Vivo phones, show the pane of the notification shade when a heads-up notification arrives, without the shade being opened, and TalkBack speaks the name of the shade, such as "Notification Center". SandFly doesn't speak the name of a pane that appears in a system window within 3 seconds after a notification arrives, unless you touched the screen, pressed a key, used a braille display or gave a voice command after the notification.

### Notices that you can dismiss

TalkBack marks its notices about an update, or about changed gestures, as ongoing, so that you can't swipe them away or dismiss them. In SandFly, you can dismiss these notices, also on the lock screen. The notifications for pausing SandFly and for downloading an on-device model stay while those continue.

### Samsung watch notification content

On Samsung watches, SandFly reads a notification card as app and title, then the message, then the time. The label of the card can leave out the message although it is in the accessibility tree; SandFly includes that text, and doesn't announce the font formatting of the card.

## Display, overview and quick settings

For these settings, see the user guide chapter [Display, overview and quick settings](docs/user-guide/en/21-display.md).

### Hide screen

For 3 minutes after you hide the screen, while the instructions for showing it again are displayed, TalkBack sets the screen to full brightness. SandFly keeps your brightness during that time. With the **Modify system settings** permission, SandFly then sets the screen brightness to its minimum while the screen is hidden, and sets it back when the screen is shown; the first time you hide the screen without the permission, SandFly opens the Android screen to allow it. The screen also turns fully black, including around the camera cutout and over the navigation bar. On some Pixel phones with Android 17, the curtain of TalkBack only dims the screen.

If you turn off **Always show this warning** in the **Hide screen?** dialog, SandFly announces only "Screen hidden", without the instructions for showing the screen. When SandFly starts with the screen hidden, it announces "Screen hidden" after "SandFly on".

Hiding and showing the screen each play a sound and a vibration of their own, **Screen hidden** and **Screen shown**, which can be turned off in **Individual sounds and vibrations** and replaced by a [sound theme](themes.md). **Speak status** reads "Screen hidden" while the screen is hidden; to leave it out, turn off **Hidden screen** in **Verbosity** > **Status readout**. To have SandFly speak "Screen hidden" when the screen turns on, turn on **Hidden screen** in **Verbosity** > **Screen on and off** > **When the screen turns on**. The **Hide screen** reading control hides and shows the screen with a swipe up or down.

### Brightness reading control

The **Brightness** reading control, in the reading controls by default, changes the screen brightness in steps of 10 percent of the system brightness slider when you swipe up or down, and speaks the new level. It works while the screen is hidden, so that you can set the brightness before you give the phone to someone. The first time, SandFly opens the Android screen for the **Modify system settings** permission. While adaptive brightness is on, SandFly announces "Turn off adaptive brightness to change brightness."

### SandFly as the wallpaper

Some phones, such as Vivo phones, stop SandFly in the background even when it is exempt from battery optimization. **Display** > **Use SandFly as the wallpaper** opens the Android screen for setting the live wallpaper of SandFly. Android keeps the current wallpaper running and restarts it if it stops, and the wallpaper runs in SandFly itself, so that these phones leave SandFly running too. The wallpaper is black, or shows the image chosen with **Display** > **Wallpaper image**, and is drawn only when Android requests it, so that it uses no battery while it is shown. The setting shows "SandFly is the wallpaper" while it is. Phones and tablets only.

### SandFly overview of recent apps

With **Overview** > **Use SandFly's overview** on, the **Overview** command opens a list of the recent apps in place of the overview of the system, and also opens over the system overview when you open it with the navigation gesture or the Recents button. Double-tap an app to switch to it, or open its actions for **Close**, **Split screen** and **App info**. **Close all** closes all recent apps, and **System overview** opens the overview of the system. On a physical keyboard, Delete closes the focused app.

**Most recent app at the bottom** lists the most recent app last, and **Columns** shows a list or a grid of 2, 3 or 4 columns. The overview needs Shizuku or root to read and open the recent apps; without them, the overview of the system opens. On the lock screen, the overview of the system opens. Not on TVs.

With **Use SandFly's overview** on and Shizuku or root, Alt+Tab on a physical keyboard switches apps without opening a window: hold Alt and press Tab to hear the app before the current one, press Tab again for older apps or Shift+Tab to move back, and release Alt to switch. Escape before you release Alt stays in the current app. Alt+Tab isn't available while Alt is the SandFly key.

### Split screen

**Split screen**, in the actions of an app in the SandFly overview, shows the app in split screen beside the app that the overview was opened from. While split screen is shown, the SandFly menu has a **Split screen** item, with **Go to divider** and the actions of the divider, such as its sizes and swapping the apps. The **Split screen** command, in **System actions**, opens the same menu, and announces "Not in split screen" when no split screen is shown. In split screen, the Back key and typed keys go to the app that you last moved focus in.

### SandFly quick settings panel

With **Quick settings** > **Use SandFly's quick settings** on, the **Quick Settings** command opens a SandFly grid instead of the quick settings of the system: switches, sliders and buttons in the order you choose, in as many columns as you choose. SandFly speaks the row and column of each item. Each control is one item with its name, its state and, where there is one, what it's connected to. Swipe up or down on a slider to move it one step. Double-tap and hold an item, or use its **Open settings** action, to open its settings. The **System quick settings** command and the **System quick settings** button of the panel open the quick settings of the system. On the lock screen, the system quick settings open.

**Quick settings** > **Quick settings panel**, or **Edit** in the panel, adds, removes and moves items, and sets **Columns**, from 1 to 4, 2 by default. Each item has its row and column; **Move** lists the cells, and the item takes the cell you choose while the items between move one cell to make room. SandFly announces how many items moved.

- **Add control** lists the SandFly controls: Wi-Fi, Bluetooth, mobile data, hotspot, airplane mode, data saver, Do Not Disturb, sound mode, flashlight, flashlight brightness, location, auto-rotate, battery saver, dark theme, Night Light, Extra dim, color inversion, color correction, NFC, microphone access, camera access, brightness, adaptive brightness, screen timeout, font size, display size, media, media output, the media, call, ring, notification, alarm, accessibility and system volumes, next alarm, battery, Settings and the power menu. Controls that the phone doesn't have aren't listed.
- **Add app tile** lists the tiles that installed apps offer.
- **Add system tile** lists the tiles of the system. With Shizuku or root, SandFly gives itself DUMP and usage access and reads the list from the system; otherwise it looks through the quick settings of the system. A system tile that SandFly has a control for, such as Hotspot, is added as that control.

Android lets only the system and the shell change some settings, and only the system run tiles. With Shizuku or root, SandFly turns Wi-Fi, Bluetooth, mobile data, the hotspot, airplane mode, location, NFC, battery saver, dark theme, data saver, and microphone and camera access on and off itself, runs app tiles, and grants itself the permissions it needs for the other controls. Without them, those switches open the settings of the system. SandFly activates system tiles and app tiles in the quick settings of the system: it opens the quick settings, activates the tile and closes them without reading them, and then speaks the new state of the tile. Without Shizuku or root, a tile works only while it is in the quick settings of the system. With them, SandFly first adds an app tile there when it isn't there, and clicks it through Shizuku when it isn't found there.

## Typing

For these settings, see the user guide chapter [Type and edit text](docs/user-guide/en/09-typing.md).

### Keyboard echo

**Typing** > **Keyboard echo** sets the echo for each way of typing: **On-screen keyboard**, **Physical keyboard**, **Braille keyboard**, **Braille display** and **Handwriting keyboard**, each with **None**, **Characters**, **Words** or **Characters and words**. In TalkBack, keyboard echo is in **Verbosity**, and the braille keyboard follows the setting of the on-screen keyboard. Each echo setting is also a reading control and a SandFly menu item, such as **On-screen keyboard echo**, off by default, and a command in the **Speech** category.

Word echo speaks the whole word, back to the last space, as it is in the field. TalkBack speaks only the part typed since the last punctuation, such as "normal" for "non-normal", and only the changed letters of a corrected word. Words with apostrophes or hyphens, such as "don't", are one word, and a web or email address is one word. When a keyboard adds a word, its punctuation and a space at once, as the braille keyboard does in contracted braille, SandFly echoes the word instead of only the space.

### Lift to type, except to send

In **Typing** > **Typing method**, **Hold finger to select any key, then lift. Double-tap for Enter, Done, or Send.** types every key when you lift your finger, as **Hold finger to select any key, then lift** does, but the key that sends or submits, such as Enter, Done, Send, Search or Go, still needs a double-tap, so that lifting your finger on the wrong key doesn't send a message. SandFly recognizes this key in Gboard. In other keyboards, every key types when you lift your finger.

### Typing by tapping

On Android 13 and later, **Typing** > **Typing method** has **Tap any key to type it, or hold finger to select any key, then lift**. A quick tap on a key types it at once, as direct touch typing does in VoiceOver, so that quick double letters type twice. Holding a finger on the keyboard explores the keys, and lifting types the key under the finger.

### Keys held down after the touch and hold delay

With **Typing** > **Hold keys down after the touch and hold delay** on, SandFly presses the key down at the end of **Touch and hold delay when typing** and passes the rest of the touch to the keyboard, as a double-tap and hold does. The keyboard then handles the touch as a long press of its own, such as a Delete key that repeats while held. This applies with the typing methods that type on lift, on all keyboards except Gboard and Pixelboard versions before 18.8. It's off by default.

### Alternative characters in Gboard

When you hold a key in Gboard, SandFly long-presses it after **Typing** > **Touch and hold delay when typing**, 3 seconds by default, and Gboard opens a popup of alternative characters. TalkBack announces the popup as a new keyboard, "Showing Available alternatives", and that announcement cuts off the name of the character that Gboard selected, so that lifting your finger types a character that wasn't spoken. When the popup closes, TalkBack announces the keyboard again. SandFly speaks the selected character, such as "Comma", as soon as the popup opens. Slide your finger to the other characters to hear them, and lift to type the one you're on. After you lift, SandFly speaks nothing more.

Gboard versions before 18.8, and Pixelboard versions before 18.8, which are built from Gboard, don't report the character they select in the popup to a screen reader with touch exploration, so that TalkBack speaks nothing while the finger slides over it. On Android 11 and later, SandFly explores a touch on these versions by itself instead of through touch exploration: it finds the key under the finger, and types it when the finger lifts. While the popup is open, the finger selects one of its characters wherever it is, the nearest one when it is outside the popup, as Gboard does, and lifting types it and closes the popup. When the finger leaves the keyboard with no popup open, touch exploration takes over for the rest of the screen, and lifting the finger there types nothing. This doesn't apply with the typing method **Hold finger to select any key, then double-tap**. Gboard 18.8 and later report the selected character, and get touch exploration as other keyboards do, with the key feedback of Gboard.

### Gboard key vibration

Gboard plays its key feedback, such as a vibration, only when a key receives hover events, which a touch that SandFly explores by itself, on Gboard and Pixelboard versions before 18.8, doesn't send. With Shizuku or root, SandFly sends these hover events to these keyboards, so that Gboard plays the feedback set in its own settings, such as **Vibrate on keypress**. There is no setting for this.

### Swipes and the on-screen keyboard

Swipes from an app, the status bar or the navigation bar don't move focus into the on-screen keyboard. With any typing method except **Hold finger to select any key, then double-tap**, a swipe while focus is on a key goes on in the app, from the text field that you are typing in. With the double-tap method, swipes move through the keys. With **Typing** > **Swipe from key to key with every typing method** on, swipes move through the keys with every typing method. Swipes from inside the keyboard can still leave it.

### Double-tap to move the cursor to the start or end

As in VoiceOver, with **Typing** > **Double-tap moves cursor to start or end** on, a double tap on the text field that you are typing in moves the cursor. When the cursor isn't at the start, a double tap moves it to the start of the text, and SandFly announces "Beginning of field". When the cursor is at the start, a double tap moves it to the end, and SandFly announces "End of field". The **Activate** command does the same when it runs from another gesture, a voice command, a braille command or a script. In any other control, or in a field that you aren't typing in, a double tap activates as usual. The setting is off by default and requires Android 13 or later and gesture recognition by SandFly.

### Editing commands

These commands are in the **Text editing** category:

- **Submit text** sends the text in the focused text field: it presses the send button of the app beside the field, as chat apps need, or else performs the action of the keyboard. In a custom keymap, it can have keys without the SandFly key, such as Ctrl + Enter.
- **New line** does what Enter does: in a text field of several lines it types a new line and speaks "New line"; in a field of one line it performs the action of the field, such as Search, or else does what Submit text does. Voice commands and voice editing also run it with "new paragraph" and "press enter".
- **Clear text** removes all text from the focused text field.
- **Character count** speaks how long the text in the focused text field is, such as "120 characters, 24 words". An emoji or a letter with an accent is one character. A hint shown in an empty field counts as no text. In a password field, it speaks only the characters.
- **Read text field** speaks the whole text of the text field with input focus, or else of the focused text field. A text field without text, or showing its hint, is "Empty", followed by the hint. With no text field, it speaks "Not editable". The text goes into the speech history, except in a password field.
- **Switch keyboard** switches to the next on-screen keyboard turned on in the Android keyboard settings, after the last one to the first, and speaks its name. It announces "No other keyboard turned on" when there is only one. It needs Android 11 or later.
- **Add to clipboard** adds the selected text, or the text of the focused item, to the end of the text on the clipboard, on a new line.
- **Editing menu** opens a menu of **Clipboard history**, **Copy**, **Add to clipboard**, **Cut**, **Paste**, **Clear text**, **Select all** and **Start or end selection mode**.
- **Paste**, **Cut**, **Clear text**, **Move cursor to beginning** and **Move cursor to end** also work while focus is on a key of the on-screen keyboard, on the text field that the keyboard types into. TalkBack announces "Not editable" unless the text field has focus.

The actions of a text field also include **Granular editing**, except in password fields.

### Selection in text that isn't editable

Selection mode also works in text that isn't in a text field, such as a message or a web page, and across items. SandFly keeps this selection itself: only the text that the cursor passes is selected, and moving to another item starts another range there. **Copy** then copies all the ranges, one range to a line.

### Clipboard history

SandFly keeps a history of up to 50 copied texts, newest first: the texts that SandFly copies, and the texts that other apps copy while a SandFly window has focus. With Shizuku or root, **Add copies from other apps**, at the top of the history, adds every text that other apps copy. The **Clipboard history** command, in **Text editing** and the **Editing menu**, opens the history over the current screen. Double-tap a text to paste it into the text field that the history was opened from, or double-tap and hold it for **Paste**, **Copy**, **Add to clipboard**, **Edit**, **Export to file** and **Remove**. **Select** checks texts to remove or export together. **Export to file** saves the history as a text file, and **Clear history** removes all texts. The history isn't included in a settings backup.

The **Clipboard history** reading control, off by default, goes through the texts in a text field with a swipe up or down, and a double tap pastes the text that SandFly read last.

### Copying text from the screen

- **Select last spoken phrase word by word**, in **Text editing**, opens a list of the words of the last spoken phrase, to check the words to copy.
- **Copy text from screen**, in **Text editing**, opens the text on the screen as a list of check boxes, divided into **Addresses and numbers**, **Characters**, **Words** or **Paragraphs**, at most 1,000 pieces. When the screen has a list or page that scrolls, **Scroll up** and **Scroll down** scroll it a page past the text listed and add the text of that page, and the list scrolls back when the window closes. To check a range, double-tap and hold the first piece and then the last, or use the **Start selection** and **End selection** actions.
- **Continuous copy**, in **Text editing**, moves focus from the focused item through every item after it, as **Read from focused item** does, scrolling lists on the way, without speech, and collects the text of each item. A touch, a key press, another command or the command again ends it, and so do the last item, 2,000 items, 200,000 characters and 10 minutes. A window then lists the texts, one check box each, all checked. **Save** puts the checked texts, one per line, on the clipboard and in the clipboard history. The same text twice in a row, as after a scroll, is listed once. Pausing SandFly or turning the screen off drops the texts.

### Granular editing

**Granular editing** opens the text of the focused text field, or of any focused item with text, in a screen of its own, split into pieces: characters, words, sentences, lines or paragraphs, chosen with **Split by**, **Words** by default. It is in the actions of a text field, and is a command in **Text editing**.

- Double-tap a piece to edit it. Leaving it empty deletes it.
- The actions of a piece are **Select**, **Insert before**, **Insert after** and **Delete**, also in a dialog with a double-tap and hold. **Start selection** and **End selection** select every piece between two pieces.
- Spaces, tabs and line breaks between pieces are pieces of their own, such as "2 spaces" or "New line", and a blank line is read as "Blank line".
- **Copy** copies the selected pieces, or all of the text when none is selected.
- **More** has **Undo**, **Redo**, **Find**, **Find next**, **Find previous**, **Replace** (all matches, ignoring case), **Add text at end**, **Select all**, **Clear selection**, **Cut selected** and **Delete selected**.
- **Save** writes the text back to the text field and closes the screen. If the field is gone by then, the text is copied instead. Back opens a dialog to save or discard the changes.

Text that isn't in a text field, such as a message, can be edited and copied but not saved. Password fields can't be opened.

### Dictation

The **Dictate** command, in **Text editing**, types what you say into the focused text field, at the cursor. A chime plays when recording starts; recording goes on through pauses until you run **Dictate** again, which plays a second chime. Speech is silenced while SandFly records, so that it isn't recorded. Recording stops after 15 minutes, or after the silence set in **Typing** > **Dictation** > **Auto stop after silence**, **Off** by default.

**Dictate, play or pause media**, in **Media and volume** and the default for a two-finger double tap, starts **Dictate** when a text field has input focus, a keyboard is shown or connected, and no call is active; it stops a dictation that is recording, and otherwise plays or pauses media. It replaces the **Play or pause media, start or end voice input** command of TalkBack, which pressed the voice typing key of Gboard. Gestures and keys assigned to the removed **Start or end voice input** command run **Dictate**.

**Typing** > **Dictation** has:

- **Speech recognition**: **Automatic**, **Google on-device speech recognition**, **Rambler (Gemini API)**, **Whisper** or **Android speech recognition**.
  - **Google on-device speech recognition** is the recognizer of the Speech Recognition & Synthesis app, with its offline language packs, in the language of the phone, with punctuation and capital letters on Android 13 and later. The voice typing of Gboard runs on it. When the language of the phone has no language pack, **Download** (language) **for on-device recognition** opens the download screen of Speech Recognition & Synthesis. The cleanup that Rambler does in Gboard, such as filler words and self-corrections, isn't available to other apps. **Automatic punctuation** and **Block offensive words**, both on by default and shown on Android 13 and later with Google speech recognition installed, add punctuation and capital letters and mask offensive words.
  - **Rambler (Gemini API)** uses Gemini 3.5 Transcribe, the model behind the cleanup of Rambler, with the Gemini API key from **AI services**: filler words are removed, self-corrections resolved, and punctuation, numbers and lists formatted. With **Rambler live transcription** on, the default, shown when there is a Gemini API key, the recording is streamed while you speak to Gemini 3.5 Transcribe Live, which returns the text of each phrase when you pause, so that only the last phrase is left to transcribe after you stop. If the stream can't connect, ends early, or the last phrase doesn't arrive within 5 seconds, the whole recording is sent to the model that isn't live. The recording is sent with `store: false`, so it isn't kept as an interaction. With a free-tier key, Google may use the audio and text to improve its products; with a paid-tier key, about $0.005 a minute, it doesn't.
  - **Whisper** runs whisper.cpp on the phone, with a model downloaded under **Whisper models**.
  - **Android speech recognition** uses the service chosen in the Android settings.
  - Google on-device and Android speech recognition start again after each pause until you stop dictation.
  - **Automatic**, the default, uses Google on-device speech recognition when it is installed and has the language of the phone, else Rambler when there is a Gemini API key and Rambler supports the language, else Whisper when a model is downloaded, else Android speech recognition. A chosen recognizer that can't be used falls back in the same order. If Google on-device recognition reports a missing language during recording, the next one takes over without stopping. When Rambler doesn't respond, the recording is transcribed with Whisper if a model is downloaded.
- **Whisper models**: Whisper Tiny (32 MB), Base (60 MB), Small (190 MB) and Large v3 Turbo (574 MB), all multilingual and quantized, and the FUTO Tiny, Base and Small models with English-only versions, from Hugging Face, checked against their SHA-256. **Model file** adds a whisper.cpp model of your own, up to 4 GB. **Processor**, only on phones with an Adreno GPU and an OpenCL driver, runs Whisper on the **CPU** or the **GPU**; if the GPU build can't load, Whisper runs on the CPU.
- **Correct dictation with AI**, on by default, and **AI service for dictation**: **Same as AI services**, the default, any AI service that is set up, or **On-device AI** when its model is downloaded. The text of Google on-device, Whisper and Android speech recognition goes to that service first, with up to 300 characters of the text before the cursor, for the cleanup that Rambler does in Gboard: filler words, stutters and false starts removed, self-corrections resolved ("two, no, three" becomes "three"), punctuation and capital letters added, misheard words corrected, and numbers, dates and spoken lists formatted. With **On-device AI**, all of dictation stays on the phone and works with any keyboard. The text of Rambler isn't sent again. If the service doesn't respond within 20 seconds (60 for on-device AI), or returns text much longer than the speech, the text is typed as recognized; **Dictate** run while it waits does the same at once. Dictation into a password field isn't sent to the service.
- **Correction prompt**: the instructions for correcting dictation, such as another language or style. Saving it unchanged or empty keeps the built-in prompt.
- **Speak dictated text**, on by default, **Keep recognition models loaded** and **Model download server**.

Typing into text fields that accept only plain text, such as in Google Contacts, failed in TalkBack, also with the voice command "type". SandFly sends plain text.

### Handwriting keyboard

**SandFly handwriting** is a keyboard that types what you write on the screen with one finger, on phones and tablets with Android 11 or later. To turn it on, choose **Typing** > **Handwriting keyboard**, which opens the Android keyboard settings, then choose it as the keyboard in a text field. It fills the screen, and while SandFly is on, touches on it go straight to it instead of through touch exploration.

Write a letter, several letters or a whole word anywhere on the screen. Strokes that follow each other within 0.6 seconds are one piece of writing, which is then typed and spoken. **Typing** > **Wait after writing** sets this wait, from 0.2 to 1 second, for the keyboard, **Write the letter of a reading control** and **Write an app's name to open it**. A letter with several strokes, such as H, needs each stroke to start within the wait. Dots, such as on an i, count. Two- and three-finger gestures do the rest:

| Gesture | Action |
|---|---|
| Swipe right with two fingers | Space |
| Swipe left with two fingers | Delete the character before the cursor |
| Swipe down with two fingers | New line, or the action of the field, such as Send, Search or Done |
| Swipe up with two fingers | Replace the last writing with its next reading of the same length, such as "h" for "H" |
| Tap with two fingers | Read the text before the cursor |
| Swipe down with three fingers | Hide the keyboard |
| Swipe right or left with three fingers | Switch to the next or previous keyboard |
| Tap with three fingers | Open the tutorial |

The first time the keyboard opens, a tutorial goes through writing a letter and a word and each gesture, and continues when you do each step. Nothing is typed in the field during the tutorial. To end it early, swipe down with three fingers.

**Typing** > **Keyboard echo** > **Handwriting keyboard** sets what the keyboard speaks as you type: each piece of writing, each word when a space or new line ends it, both, or nothing. The keyboard speaks deleted characters and the readings that replace the last writing whatever the echo. Handwriting is recognized on the phone by Google ML Kit, in the language of the phone; the first time, SandFly shows a dialog to download the model, about 20 MB. In SandFly text fields, the text before the cursor isn't read, and a deletion is read only as "Deleted". While the keyboard is shown, only commands that open no window run.

## Physical keyboards

TalkBack has a default keymap and a classic keymap. For the keymaps and keyboard settings of SandFly, see the user guide chapter [Physical keyboards](docs/user-guide/en/11-keyboards.md).

### Keymaps

**Keyboard shortcuts** > **Choose a keymap** has these keymaps:

- **SandFly keymap**, the default.
- **VoiceOver keymap**: the keyboard commands of VoiceOver on iPhone, with the SandFly key as the VO key. In browse mode, single keys move as single-key Quick Nav does in VoiceOver.
- **NVDA keymap**: the laptop layout of NVDA, which needs no number pad.
- **JAWS keymap**: the desktop keystrokes of JAWS, with its laptop keys for clicks and cursors.
- **Orca keymap**: the laptop layout of Orca.
- **Keyboard phone keymap** and **Keyboard phone single-key keymap**, for phones with a built-in keyboard. See [Keyboard phones](#keyboard-phones).
- **BlackBerry keymap**: keys similar to those of the BlackBerry 10 Screen Reader, for keyboard phones. BlackBerry mode chooses it on keyboard phones.

Each keymap keeps its own shortcuts, its own SandFly key and its own arrow key navigation settings. **Reset the keymap** gives every shortcut of the keymap in use its default keys again.

### The SandFly key

Most shortcuts use the SandFly key, which **Keyboard shortcuts** > **Choose SandFly key** sets for the keymap in use: Meta, Alt, Ctrl + Alt, Ctrl + Meta, Ctrl, Insert, Numpad Insert, and on keyboard phones Sym, Right Alt, Right Shift and Fn, as far as the keymap allows. Insert, Numpad Insert, Sym, Right Alt, Right Shift and Fn aren't modifier keys for Android; SandFly takes them while they are held, and they don't reach apps. With Meta as the SandFly key, Meta pressed alone once opens the list of all apps, as in Android. The SandFly key pressed twice quickly can be the shortcut for **Turn browse mode on or off**.

### Custom keymaps and keymap files

**Keyboard shortcuts** > **New keymap** makes a copy of the keymap in use, with its shortcuts, its SandFly key and the way its keys work. In a keymap that you make, shortcuts can use any keys, with or without the SandFly key; shortcuts without the SandFly key work anywhere except in a text field being edited. **Rename keymap** and **Delete keymap** are shown for keymaps that you made. **Export to file** and **Import from file** save and read the keymap in use. Not on watches.

### Shortcuts in a row and while editing

In the SandFly and VoiceOver keymaps, and keymaps copied from them, a shortcut can be the SandFly key + R or the SandFly key + O followed by a third key within 3 seconds, such as SandFly key + O, then H. **Only while editing**, in the shortcut dialog, makes a shortcut without the SandFly key that works only while a text field is being edited, such as "Alt + Space while editing".

### Keyboard shortcuts in apps

**Keyboard shortcuts** > **Keyboard shortcuts in apps** sets shortcuts for one app, which come before the shortcuts of the keymap while the app is shown. A shortcut set to **Unassigned** in an app leaves its keys to the app.

### Arrow key navigation

While arrow key navigation is on, the arrow keys run commands without the SandFly key: alone, two pressed together, or with Ctrl or Alt. The default commands are those of the arrow keys in VoiceOver on iPhone:

| Keys | Command |
|---|---|
| Right Arrow, Left Arrow | Next item, previous item |
| Down Arrow, Up Arrow | Adjust reading control down, up |
| Left Arrow + Right Arrow together | Turn arrow key navigation on or off |
| Up Arrow + Down Arrow together | Activate the item |
| Up Arrow + Right Arrow, Up Arrow + Left Arrow together | Select next reading control, previous reading control |
| Ctrl + Up Arrow, Ctrl + Down Arrow | First item, last item on screen |
| Alt + Up Arrow, Alt + Down Arrow | Scroll back, scroll forward |
| Alt + Left Arrow, Alt + Right Arrow | Scroll right, scroll left |

Arrow key navigation is off by default, except in the keyboard phone keymaps, and stays as you last left it. To turn it on or off, press Left Arrow and Right Arrow together, or use **Keyboard shortcuts** > **Arrow key navigation** > **Arrow key navigation**. Each arrow key, or two keys pressed together, can be given any command in **Keyboard shortcuts** > **Arrow key navigation**, from the same list as for gestures. **Turn arrow key navigation on or off** is also a command in the **SandFly** category.

A key that is part of two keys pressed together runs its command when it is released, so that the two keys together don't also run the command of the first key. While a text field is being edited, single arrow keys move the cursor; keys pressed together still run their commands. The NVDA, JAWS and Orca keymaps don't have arrow key navigation, as their arrow keys move SandFly focus.

### Desktop navigation and the review cursor

In the NVDA, JAWS and Orca keymaps, and keymaps copied from them, Down Arrow and Up Arrow move to the next and previous item, Left Arrow and Right Arrow move by character, and Ctrl with them by word. Tab and Shift + Tab move input focus and SandFly focus together, Enter clicks the focused item, and Escape is Back. In browse mode, single letters move by element.

The **Object navigation** category moves SandFly focus through the items as the app arranges them, including containers that SandFly focus otherwise skips. The **Screen review** category has a review cursor that reads the text on screen by line, word and character as it is laid out, without moving SandFly focus, and clicks or long clicks at its position. **Recognize text on screen** recognizes the text in a screenshot of the screen, including text that apps don't report, and shows it to review; it needs Android 11 or later. These commands have shortcuts in the NVDA, JAWS and Orca keymaps.

In the NVDA keymap, P and Shift + P move by paragraph in browse mode, besides Ctrl + Down Arrow and Ctrl + Up Arrow, and SandFly key + F9 starts or ends selection mode.

### Fast search

In browse mode, **Keyboard shortcuts** > **Fast search** > **Fast search key**, Off by default, with a letter or digit moves to the next item that starts with it; with Shift too, it moves to the previous one. **Wrap around**, on by default, continues from the other end of the screen.

### Keys left to apps

These commands, in the **Activation** category, leave keys to the app:

- **Pass through to system**: the next key or keys go to the app, and SandFly announces "Ignoring next key press".
- **Pass keys without the SandFly key to this app**: the app shown receives every key pressed without the SandFly key, such as the arrow keys and the browse mode letters, until you run the command again in that app. SandFly keeps the choice for each app.
- **Turn SandFly keyboard input on or off**: every key goes to Android, shortcuts with the SandFly key too, until you press the shortcut of this command again. Keyboard input is on again when SandFly restarts.

### Keyboard practice

**Turn keyboard practice on or off**, in the **SandFly** category, **Keyboard shortcuts** > **Keyboard practice** and **Help & feedback** > **Keyboard practice** turn on keyboard practice, like input help in NVDA: SandFly says each key pressed, with the modifier keys held, and the command its keys run in the keymap in use, and runs nothing; no key reaches the app. The SandFly key pressed alone is also said as the SandFly key. Keys pressed twice, keys held down, two keys in a row and arrow key navigation are said with their commands. The shortcut again, Back or Escape turns it off. Its default shortcut is the keyboard help key of each computer screen reader keymap: SandFly key + K in the VoiceOver keymap, SandFly key + 1 in the NVDA and JAWS keymaps and SandFly key + H in the Orca keymap; in the keyboard phone keymaps it is SandFly key + Shift + K.

### Hold a touch on the focused item

**Hold a touch on the focused item**, in the **Activation** category, puts a touch down on the middle of the part of the focused item on the screen, with touch passed through to the app, for as long as you choose: for buttons that act while a finger stays on them, such as the voice message button of Telegram, which records while the touch is down and sends the message when it lifts. While its keys are held, the touch stays down; releasing them lifts it. Keys released within half a second latch the touch, and SandFly says "Touch held"; the shortcut again, Back or Escape lifts it, and SandFly says "Touch lifted". Run from a gesture or the SandFly menu, the touch latches too. Other keys work as usual while the touch is held. The touch lifts after 10 minutes, when the item is no longer on the screen and when SandFly is suspended. Its default shortcut is SandFly key + V in the keyboard phone keymaps. It needs Android 11 or later.

### Keyboard phones

On phones with a built-in keyboard that has no Ctrl, Meta or arrow keys, such as the Unihertz Titan phones, the BlackBerry KEYone, KEY2 and KEY2 LE, and Zinwa phones such as the Q25, SandFly uses the **Keyboard phone keymap**. Its most used shortcuts are presses of keys that typing doesn't use, such as the SandFly key pressed alone for the next item and Alt pressed twice to play or pause media and answer calls. The **Keyboard phone single-key keymap** runs commands with single keys outside text fields, such as Space for the next item and H for the next heading. Shortcuts can also be a modifier key pressed alone, keys pressed twice, or keys held down. In the keyboard phone single-key keymap, the SandFly key pressed twice turns browse mode on or off, and letters then move by element as in the SandFly keymap. In the keyboard phone keymaps, Enter after SandFly focus moved activates the item, and arrow key navigation is on for the arrow keys that a trackpad sends. SandFly chooses a SandFly key that typing on the phone doesn't need, such as Right Shift on the Zinwa Q25.

While a physical keyboard is connected, **Typing** > **Physical keyboard** has:

- **Letter keys press keypad buttons**: on keyboards whose letters carry numbers, letter keys press the buttons of the lock screen PIN pad, the call keypad and calculators. On the lock screen, digit keys also press the PIN pad buttons, and a letter or digit pressed before the PIN pad shows opens it, or unlocks where no PIN is needed. On by default, shown only for such keyboards.
- **Enter sends messages**: in the message field of a messaging app, such as Messages, WhatsApp, Telegram, Signal or Slack, with text in it, Enter presses the Send button of the app. Shift + Enter and Alt + Enter type a new line. Off by default. The Send key of a keyboard phone, such as the green call key of the Zinwa Q25, presses the Send button in the same apps whether the setting is on or off, also outside the message field.
- **SandFly keyboard**, **Held keys type**, **Double-press characters**, **Double space types a period**, **Speak password characters**, the settings in [Auto-correct and word suggestions](#auto-correct-and-word-suggestions) and **Android physical keyboard settings**.

### SandFly keyboard

The SandFly keyboard is an input method for physical keyboards that shows no keys on the screen. Keys type as they do without it. It adds exact information about whether a text field takes what you type, so that single-key shortcuts and Enter in the keyboard phone keymaps work in the right places; held keys that type the Alt character or the capital letter of the key, set with **Typing** > **Physical keyboard** > **Held keys type**; **Search emoji** and **Choose a symbol**, which list characters by name while you type; Shift pressed and released before a key, which types that key with Shift, as on BlackBerry keyboards; double presses, set in **Typing** > **Physical keyboard** > **Double-press characters**, such as G pressed twice for ğ, and with **Double space types a period** on, a period for Space pressed twice after a word; and **Turn editing mode on or off**, in which single keys move the cursor and select, copy, cut and paste. To turn it on, go to **Typing** > **Physical keyboard** > **SandFly keyboard**. Not on watches and TVs.

### Auto-correct and word suggestions

The SandFly keyboard can correct and complete the word being typed, with word lists for English (US and UK), German, Spanish, French, Portuguese (Brazil and Portugal), Hungarian, Turkish, Russian, Ukrainian and Arabic. Both are off by default, in **Typing** > **Physical keyboard**:

- **Auto-correct**: Space, punctuation or Enter after a misspelled word put its correction in its place, taking neighbouring keys of the keyboard layout, doubled, missing and swapped letters, apostrophes and accents into account. Backspace straight after a correction undoes it, and the word isn't corrected again.
- **Word suggestions**: while a word is typed, Up Arrow and Down Arrow, which the Zinwa Q25's trackpad sends, go through corrections and completions; Enter or a trackpad click types one with a space, and Space or punctuation types it with that character. Without a word being typed, the arrows move in the text.
- **Speak suggestion**: the correction Space would type ("receive, correction"), or else the first suggestion, said at a lower pitch while a word is typed: **Immediately**, right after the keyboard echo of each key, or after typing pauses for 0.25 to 3 seconds (1 second by default), or **Off**. The next key stops it.
- **Enter types the spoken suggestion**, off by default: Enter, or a Zinwa Q25 trackpad click, which sends Enter, types the correction or suggestion just spoken; Down Arrow starts the list after it. In the list, a trackpad swipe moves once.
- **Word list language**: the system language by default. **Delete learned words** deletes the words learned from typing, which rank higher in suggestions.
- **Next word suggestion**, **Previous word suggestion** and **Type word suggestion**, in the **Text editing** category, and the **Word suggestions** reading control, off by default, for keyboards without arrow keys and for touch.

### Password characters

TalkBack's **Speak passwords** setting was removed, and Android gives screen readers only bullets for password text. The SandFly keyboard reads the text of the field it types into, so SandFly can say the characters of password fields:

- **Typing** > **Physical keyboard** > **Speak password characters**, off by default: in a password field being edited with the SandFly keyboard, each character typed, such as "capital P", "at" or "1", each character deleted with Backspace or Delete, such as "a deleted", and the character the cursor passes over, named as in other text fields. Pasted text, a cleared field and longer moves are spoken with bullets as before.
- **Spell the password field**, in the **Text editing** category: spells the whole password field being edited with the position of each character, such as "1 capital P, 2 a, 3 at", whether or not the setting is on. Outside a password field it says "Not a password field". Its default shortcut is SandFly key + Y in the keyboard phone keymaps.

The characters are kept out of the speech history, **Repeat last spoken phrase** and **Copy last spoken phrase**, and only their number is logged. In editing mode, R and C no longer say the text of a password field.

## Braille keyboard

For these settings, see the user guide chapter [Braille](docs/user-guide/en/10-braille.md). Settings paths in this section start at **Braille** > **Braille keyboard**.

### Diagonal swipes

A swipe on the braille keyboard counts when it goes up to about 35 degrees off the horizontal or vertical. TalkBack allows about 27 degrees, and ignores swipes that go a little diagonally.

### Keyboard echo for the braille keyboard

In TalkBack, the braille keyboard follows the echo setting of the on-screen keyboard. In SandFly, the braille keyboard has its own setting, **Typing** > **Keyboard echo** > **Braille keyboard**; see [Keyboard echo](#keyboard-echo). The capital and number signs follow it too.

### Next braille table

Hold dots 4, 5 and 6 and swipe down to switch to the next language of the braille keyboard. Contracted braille is turned on or off as it was when you last used that language, and SandFly announces the language and whether it's contracted. Speech changes to your first voice profile in that language, if you have one, and back to **SandFly default** when the new language has none. Unicode braille keeps the voice in use. You can change the gesture in **Braille** > **Braille keyboard** > **Gestures**.

### Emoji search

Hold dots 4, 5 and 6 and swipe right to search for an emoji. Type words of its name in uncontracted braille of the keyboard language, and SandFly speaks each character and the first emoji whose name contains every word, with the number of matches. A character that leaves no emoji is dropped. Swipe down or up with one finger for the next or previous emoji, right with one finger for a space between words, and left to delete. Swipe right with two fingers to insert the emoji; the search stays open for another. Any other gesture closes the search. The names are the Unicode CLDR names that SandFly speaks emoji with, in the language of the braille table, or in English where CLDR has none. Skin tone variants aren't offered, and a search finds at most 100 emoji.

### Mongolian and Romanian braille

The braille languages include Mongolian, uncontracted and contracted, from the liblouis tables, and **Romanian**, 6-dot uncontracted braille, besides **Romanian 8-dot Computer Braille**. Both are on the braille keyboard and on braille displays.

### Indonesian braille

**Indonesian**, uncontracted and Tusing contracted, is on the braille keyboard and on braille displays.

### Unicode braille

**Unicode Braille** and **Unicode 8-dot Braille** type each cell as its Unicode braille character, from U+2800 to U+28FF, such as ⠞⠡, on the braille keyboard and on a braille display. A space is the blank braille cell U+2800, which SandFly speaks as "blank braille cell" wherever it is read. They are in **Add a language** > **Unicode Braille**, at the end of the list, and are for typing only: they aren't reading languages of braille displays, and switching to the next reading language skips them.

### Custom braille characters

**Custom braille characters** lists characters or text typed with braille cells of your choice, each for one braille table. Choose **Add character** and enter the character or text, the dots of each cell with cells separated by hyphens, such as `2356` or `456-2356`, and the braille table. An entry takes precedence over the rules of the table on the braille keyboard and on a braille display, both for typing and for the braille shown. Entering a character again for the same table replaces the earlier entry. Rules of the table that cover more cells, such as contractions, and rules for characters within numbers, can still take precedence. **Export to file** and **Import from file** save and read the list as an [item file](docs/reference/item-files.md).

### Typing sounds

**Typing sounds** plays a sound for each character, and a sound each for space, delete and new line, at the sound feedback volume. A [sound theme](themes.md) can replace them. It's off by default.

### Say "Submitting text"

**Say "Submitting text"** can be turned off to send text without the announcement. It's on by default.

### Braille keyboard stays open after sending in chats

TalkBack hides the braille keyboard each time you submit text. In SandFly, with **Braille** > **Braille keyboard** > **Keep the keyboard open after sending** on, the braille keyboard stays open for the next message when Submit text presses the send button next to the field, as in Telegram, WhatsApp and Messages. The setting is off by default. When Submit text carries out the action of the field, such as search or next, or presses Enter, it still hides the braille keyboard so that focus can move on.

### Return to the previous keyboard

**Return to the previous keyboard** changes back to the keyboard used before the braille keyboard when you hide the braille keyboard with **Hide keyboard** or leave its text field. It doesn't apply while a braille display is connected. It's off by default.

### Capitalize sentences

**Capitalize sentences** changes the first letter typed at the start of a sentence to a capital: at the start of a field, after a new line, and after a full stop, question mark or exclamation mark followed by a space. It doesn't change password, email address, web address or number fields. It's off by default.

### Update dot positions with one hand, then the other

To update the dot positions without holding six fingers on the screen for 2 seconds, hold three fingers of one hand on the screen for at least 0.4 seconds, then put down the three fingers of the other hand and keep all six still for 0.65 seconds. Either hand can go first. With eight-dot braille, use four fingers of each hand. Six fingers that land together, or that lift sooner, still type a full cell. This works only while **Include all pressed dots** is off; that setting is on by default. **Update dot positions with one hand, then the other** turns this off. It's on by default.

### Update dot positions while typing

**Update dot positions while typing** moves the dots toward where the fingers land, so that the dots follow hands that shift during a typing session. Each typed cell moves all dots of a hand a little toward the touches of that hand, and moves each touched dot further toward its finger. The positions are saved and used in the next session, for each layout as with dot calibration. It's on by default.

- A touch moves the dots only when it is clearly closer to its own dot than to any other. A finger that has shifted far enough to type the wrong dot doesn't move the dots; update the dot positions instead.
- The dots don't move off the screen, closer together than one dot width, or into an order that changes their dot numbers.
- Only typed cells move the dots. Gestures, holds and the tutorial don't.

### Swap top and bottom dots

**Swap top and bottom dots** makes dot 1 trade places with dot 3, and dot 4 with dot 6. The **Reverse dots** setting of TalkBack is **Swap left and right dots** in SandFly. You can turn on both.

### Skip the tutorial

The first page of the braille keyboard tutorial has **Skip tutorial**, which opens the keyboard immediately.

### Haptics

The braille keyboard uses the same vibration effects as the rest of SandFly. Submitting text vibrates as closing or switching the keyboard does. Deleting in an empty field plays a soft vibration that fades out.

### Navigation that stays in the text field

If you swiped to another control while the braille keyboard opened, TalkBack moves by character, word or line in that control instead of in the text field. SandFly moves focus back to the text field before each command.

### No windows over the braille keyboard

While the braille keyboard is shown, gestures run only commands that open no window, such as navigation and reading controls, so that no window opens over the keyboard. Braille commands still open over it.

### Dots that follow how you hold the device

The braille keyboard keeps the dots under your fingers whichever way round you hold the device, with auto-rotate on or off. TalkBack expects the charging port on one side when auto-rotate is off, so that holding the phone the other way round types dot 4 for dot 1.

SandFly records how you last held the device up, the whole time that the screen is on, as auto-rotate does. In screen-away mode, the dots follow how you hold the device. In tabletop mode, laying the device flat keeps the side that you held it up with, even from before the keyboard opened, and turning it on the table turns the dots with it. The keyboard announces where the charging port is when the mode changes and after each turn, such as "Tabletop mode, charging port on the left". On a phone, the dots turn when the port moves to your other side, because you type with the port on your left or right; a quarter turn in between announces "Charging port toward you" or "Charging port away from you". On a tablet or an unfolded foldable, they turn by quarter turns, including upside down, which auto-rotate doesn't do.

To keep the dots facing the way they are, hold dots 4, 5 and 6 and swipe up. To have them follow the device again, do the same again. There's a lock for each screen size and mode, so that you can lock a foldable one way when it's folded and another way when it's unfolded.

Gravity can't show whether a device that's held up faces you or faces away. When you lay a phone flat after holding it up, SandFly takes it to have faced you, as auto-rotate does, unless you typed on it screen away. The side you typed with screen away is kept, and turns with the phone on the table, until you type on the table. Typing counts as screen away only if you held the phone upright, so that tilting a phone that you hold nearly flat in your hands doesn't count. A phone last held in portrait announces "Tabletop mode, charging port toward you", and turning it to your left or right on the table puts the port on that side. A tablet or an unfolded foldable held in portrait is taken to have faced you, and one held sideways to have faced away, as when typing screen away.

If you hold or stand your tablet facing you, turn off **Tablet held up faces away**. A tablet held up any way round then uses the tabletop layout facing you, with your thumbs holding the bottom edge, and the keyboard announces, for example, "Screen toward you, charging port on the left". The dots stay the same way round when you lay the tablet flat. **Tablet held up faces away** is also a reading control: with the braille keyboard open, swipe right or left with three fingers to reach it, then swipe down or up with one finger.

Limitations:

- A phone opened flat that hasn't been held up since the screen came on follows the screen rotation, and with auto-rotate off expects the port on the left; turning it on the table, or locking the orientation, corrects this.
- A phone held up screen away and laid flat without typing, or typed on screen away without ever being held within 40 degrees of upright, is taken to have faced you, so that it expects the port on the wrong side; locking the orientation corrects this.
- Turns on the table require the game rotation vector sensor; devices without it still follow how they're held up.
- A tablet announces where the port is only if it's a portrait tablet with the port at the bottom.
- Recording how the device is held keeps the accelerometer running while the screen is on, as auto-rotate does.

### Braille commands

Braille commands run SandFly commands from braille cells typed on the screen, laid out as on the braille keyboard. To show them, run the **Braille commands** command, in the **Braille** category, or hold dots 4, 5 and 6 on the braille keyboard and swipe left. Type a cell to run its command, such as dot 4 for the next item and dots 1 2 5 for Home. Braille commands stay open after each command, until you swipe down with two fingers or the screen turns off. They need Android 11 or later, and aren't on watches.

Hold dots 4, 5 and 6 and swipe left to switch from braille commands to app search, and then to the braille keyboard while a text field is being edited. An app opened from app search entered this way leaves braille commands open. **Find text**, **Find next text** and **Find previous text** find text typed in uncontracted braille on the screen.

**Braille** > **Braille commands** lists each cell with its command, adds and changes cells, and has **Braille commands in apps** and **Braille command profiles**. A cell runs the command of the braille command profile in use, otherwise the command that the app in front gives it, otherwise the default command. You can also give a command a cell from its screen in **Commands**.

**Type an app's name in braille to open it**, in **System actions**, opens app search directly, on phones and tablets with Android 11 or later. Type the start of the name of an app in uncontracted braille of the current language of the braille keyboard; SandFly speaks each character and the first app whose name, or a later word of it, starts with what you typed, and how many apps do. Case, spaces, punctuation and accents don't count. A capital or number sign is kept and spoken; a cell that leaves no app is dropped, and SandFly announces "No apps". An app opens only when you open it, also when it is the only match. Swipe up and down with one finger for the previous and next app, left with one finger to delete the last character, left with two fingers to delete everything, up with two fingers to open the app, up with three fingers to open it in split screen, and down with two fingers to close. 15 seconds without input also close it. App names in scripts that braille writes in another script, such as Chinese characters, don't match. Not on watches.

## Braille displays

For these settings, see the user guide chapter [Braille](docs/user-guide/en/10-braille.md).

### EuroBraille b.note and b.book

The EuroBraille driver of SandFly is from a newer brltty than the driver of TalkBack, so that it also supports the b.note, b.note 2, b.book and b.book 2, with their keypads and navigation keys. Over USB, they are found by their USB IDs.

On EuroBraille displays, Space chords and Dot 8 run the same commands as on other displays, and can be assigned in **Braille display commands**. Space with dot 2, 3, 5 or 6 is the exception: these chords run nothing until assigned, so that they can have commands different from the left joystick. This includes the chords that EuroBraille uses for Tab, Shift+Tab, Page Up, Page Down, Insert, Delete and Shift (Space with dots 2 5 6, 2 3 6, 1 3, 4 6, 1 3 5, 3 6 and 7). Backspace with dots runs nothing until assigned. The cursor routing keys move the cursor and activate items. Pressing the right joystick activates, like Dot 8, and left and right on it pan; on the left joystick, left and right move by character and up and down by word. Right joystick left and right, and Backspace, which deletes as Dot 7 does, can be assigned apart from the other keys of their commands.

### Any braille display that uses HID

On Android 15 and later, TalkBack connects to braille displays that use the HID braille standard over Bluetooth, but lists only displays whose names it has, so that newer displays and braille terminals are left out. SandFly lists any Bluetooth device that offers HID, whatever its name, and connecting checks that it's a braille display, so that other HID devices, such as keyboards, can appear in the list too. TalkBack also refuses displays that send their keys and receive their cells in separate HID reports, as the standard allows; SandFly connects to those.

### Displays from other apps

Apps can give SandFly a braille display that SandFly can't connect to on its own, such as the built-in keys and cells of a braille notetaker. SandFly uses it as a display connected through Bluetooth or USB: its keys navigate and type with the same commands, and its cells show what SandFly shows. A display with keys and no cells works too.

SandFly uses a display from an app only after you allow it, after a warning that the app will be able to read the screen and control the device. SandFly shows the dialog when it finds such an app, and **Braille** > **Braille display** > **Displays from other apps** lists these apps, to allow or stop allowing each one. A notification shows which app is in use. A display connected through Bluetooth or USB comes first, and one from an app waits while it's connected. [Providing a braille display from another app](docs/braille-display-api.md) describes the API for app developers.

### Braille dictionary

**Braille** > **Braille dictionary** changes text on a braille display, such as a symbol into a word, or text left off the display. Its entries are separate from the entries of the speech dictionary, which are for how text sounds. Entries apply in order, to all apps or one app, with whole words and regular expressions as in the speech dictionary. Text being typed and SandFly screens are shown as they are.

### Symbol dictionary

**Braille** > **Symbol dictionary** opens the same list as **Speech** > **Symbol dictionary**: symbols or sequences of symbols to replace in speech and on a braille display. On a braille display, entries apply after the braille dictionary, and text being typed and SandFly screens are left as they are. See [Speech dictionary and symbol dictionary](#speech-dictionary-and-symbol-dictionary).

### Hide the on-screen keyboard

With **Braille** > **Braille display** > **Hide the on-screen keyboard** on, other on-screen keyboards stay hidden while a braille display is connected. The SandFly braille keyboard still shows its strip, since typing on the display goes into text fields through it. Keyboards show again when the display disconnects or the setting is turned off. It's off by default.

### Braille display keys you choose

In TalkBack, **Braille display commands** only lists the keys of each command. In SandFly, choose a command under **Braille** > **Braille display** > **Braille display commands**, then **Change keys**, and press the new keys on the display: one of its keys, Space with dots, or on EuroBraille displays, Backspace with dots. Keys that already run another command move to the new one after you confirm, and the other command then has no keys. **Reset** gives a command its default keys back, and **Reset all keys** does so for every command. **SandFly commands** lists the commands that gestures run, such as **Open SandFly menu**, **Notifications** or **Read from top**, by category, to give each one display keys. Commands without keys on the connected display are listed with "No keys". Panning and routing keys can't be changed, except right joystick left and right on EuroBraille displays, and keys without Space still type their dots in text fields.

## Volume keys and sensors

TalkBack has no **Volume keys** settings screen. For the **Volume keys** screen, see the user guide chapter [Volume keys and sensors](docs/user-guide/en/12-volume-keys-and-sensors.md).

### Volume keys that adjust sliders and move the cursor

With **Volume keys** > **Volume keys adjust sliders** on, the volume keys change the value of the focused slider, and SandFly speaks the new value, as with the **Adjust slider** reading control. Hold a key to keep changing the value. With **Volume keys** > **Volume keys move the cursor** on, the volume keys move the cursor while a text field has focus: volume up moves it back a character and volume down moves it on, and SandFly speaks the character. Both are off by default. When focus is on anything else, or the screen is off, the volume keys change the volume as usual. With a finger on the screen or during continuous reading, they still change the speech volume.

### Commands on volume key presses

**Volume keys** > **Volume up double press**, **Volume up triple press**, **Volume up long press**, and the same three for volume down, each run a command from the list of commands for gestures. All are unassigned by default. A key with no assigned press changes the volume at once, as usual. When a key has a double or triple press assigned, a single press changes the volume 0.3 seconds after the key is released, once no further press follows. When a key has a long press assigned, holding it runs the command instead of changing the volume repeatedly. **Volume keys** > **Volume key long press duration** sets how long a key is held for its long press: **System default**, or from 0.3 to 1 second.

The presses don't run commands, and the keys change the volume as usual, while the screen is off, except on an always-on display that shows the lock screen; during a phone call or while an app plays call audio; while a finger is on the screen; and during continuous reading. When a key that adjusts sliders or moves the cursor has a command for one of its presses, its presses run their commands, and a press without a command adjusts the slider or moves the cursor. Pressing both volume keys together runs no command, so that the accessibility shortcut still works. While a phone call rings, only a volume key with **Answer call** or **End call** assigned to one of its presses runs commands. A press kept from Android changes the volume that Android chooses for the key: that of the media session playing, including the volume of a cast session, when SandFly has notification access, or else that of the media playing or the ringer.

### Every volume key press handled by SandFly

- **Volume keys** > **SandFly handles every volume key press** keeps every volume key press from Android while the screen is on: presses with a command run it, and other presses, including holding a key, change no volume. While it's on, **Volume up press** and **Volume down press** run a command for a single press. Off by default.
- **Volume keys** > **Hide volume panel** keeps every volume key press from Android while the screen is on, and presses with no command change the volume without the volume panel of Android. Off by default.

With both on, presses with no command change no volume. Both leave the keys to Android while the screen is off, during a phone call or while an app plays call audio, while a call rings, while SandFly is paused, and for the second key when both volume keys are held. Slider adjustment, cursor movement and speech volume with a finger on the screen still work.

### Volume key shortcuts with the screen off

Android doesn't pass the volume keys to screen readers while the display is off. With **Volume keys** > **Volume key shortcuts with the screen off** on, the volume key presses that have commands run them while the display is off, through Shizuku. Presses without a command change the volume. It's off by default, needs Shizuku, and isn't shown when SandFly uses root instead of Shizuku, or on watches.

### Volume change feedback

**Volume keys** > **Volume change feedback** sets the feedback for volume changes:

- **Off**.
- **Tone** plays a 50 ms tone at 1 kHz on each volume step, at the volume being changed. A step that comes while a tone still plays gets no tone, so that tones never queue.
- **Speech**, the default, speaks which volume changed and the new level, such as "Media volume set to 60 percent", about 0.6 seconds after the last step.
- **Tone and speech** does both.

**Silence tone during media playback** and **Silence speech during media playback**, off by default, turn the tone or the announcement off while an app plays media. **Interrupt speech to announce the volume**, on by default, stops the current speech to announce the volume. **Change speech volume silently while speaking**, off by default, leaves out the announcement when the volume keys change the speech volume while SandFly speaks or has speech queued, so that speech goes on at the new volume; presses within a second of each other count as one series, which stays silent after speech ends. Volume changes that SandFly makes, such as with a volume command or a script, get no feedback; volume changes that other apps make do. There's no tone during a phone call, during a call in an app, or while SandFly is paused. A press at the highest or lowest volume changes nothing and gets no feedback.

### Speech volume on Samsung phones

On Samsung phones, One UI changes the accessibility volume with the volume keys while speech or a sound plays and for 1 second after. SandFly changes the media or ring volume instead, as One UI does when no speech plays. With **Volume keys** > **Volume keys change speech volume while speaking** on, presses while speech plays change the accessibility volume; presses in the 1.5 seconds after it stops still change the media or ring volume. The setting is shown only on Samsung phones, and is off by default.

### Mute key

On phones with a mute key, such as the Zinwa Q25, **Volume keys** > **Hold mute key to dictate in text fields**, on by default, runs **Dictate** while the mute key is held in a text field. **Mute key press**, unassigned by default, runs a command for a press elsewhere.

### Proximity sensor

TalkBack stops speech when something covers the proximity sensor. SandFly doesn't, by default. To turn this on, turn on **Speech** > **Stopping speech** > **Cover proximity sensor to stop speech**. Other features that use the sensors are back tap, the **Shake** gesture, touch lock while the proximity sensor is covered, shake and wave to stop a notification, and **Speaker when away from your ear**.

## Voice commands

In TalkBack, voice commands are a fixed list of phrases. For SandFly voice commands, see the user guide chapter [Voice commands](docs/user-guide/en/15-voice-commands.md) and the phrase list in [Appendix C](docs/user-guide/en/appendix-c-voice-commands.md).

### What you can say

SandFly reads what you say in this order: a custom voice command, the name of any command as the **Commands** screen shows it, including the names of macros, custom menus and script commands, a phrase pattern, one of the voice commands of TalkBack, and, with AI on, the AI service. The phrase patterns are:

- Open an app: "open", "launch", "start" or "switch to", and the name of the app.
- Activate an item on the screen: "tap", "double tap", "click", "press", "activate" or "select", and the text of the item.
- Type text: "type", "write" or "enter", and the text.
- Run a command: "run" or "do" and the name of a command, or "run the" and the name of a macro and "macro".
- Wait: "wait", or "wait" and a number of seconds, up to 60.
- Call: "call", "phone", "ring" or "dial", and a contact name or phone number.
- Send a message: such as "message Sam saying I'm on my way" or "send a message to Sam on WhatsApp saying hello". With SMS or WhatsApp, SandFly opens the conversation with the text, reads it back, and presses **Send** after you answer "yes".
- Describe: "describe the screen", "describe this image", "copy it", and "send it to Sam", which sends the description from the command before.
- Text in a text field: such as "next 3 words", "end of line", "delete next five words", "copy previous sentence" or "select crocodile", which move the cursor, or select, copy, cut or delete, by a number of characters, words, sentences, lines or paragraphs, to the start or end of one, or a text in the field.

Words such as "please" and "can you" are left out.

### Chains of commands

A voice command can hold several commands, joined by "then", "and then", "after that" or "afterwards", such as "open Messages then call Sam". "And" joins commands too, when each part is a command. SandFly runs the commands one after another, with a short pause after each command that changes the screen, and stops at the first command that fails. To stop a chain, start another voice command and say "stop", "cancel", "quit" or "never mind".

### Voice editing

**Start voice editing**, a **Text editing** command without a default gesture, records voice commands for the focused text field one after another until it runs again, a stop phrase such as "stop editing" is said, focus leaves the field, or **Voice commands** > **Voice editing time limit** passes: 5, 10, 20 or 30 minutes, 10 by default. It runs only **Text editing** and **Navigate by text** commands, "type" and a text, the phrases for text in a text field, the text editing voice commands of TalkBack, and custom voice commands for these commands or for macros. Speech is silenced while SandFly records, and recording starts again after SandFly finishes speaking the result. TalkBack records one voice command per gesture.

### Voice commands understood with AI

**Voice commands** > **Understand voice commands with AI**, off by default, sends a voice command that SandFly doesn't recognize, or a chain that SandFly recognizes only in part, to the AI service in use, which turns it into commands. SandFly sends what you said, the names of the commands and the texts of the items on the screen. The switch needs an AI service set up in **AI services**: on-device AI, SandFly Pro, or another service with a key.

### Custom voice commands

**Voice commands** > **Custom voice commands** > **Add voice command** gives a phrase of up to 200 characters to any command, including a macro or a custom menu. Custom voice commands come before every other voice command. **Export to file** and **Import from file** save and read them.

## Macros

For macros, see the user guide chapter [Macros](docs/user-guide/en/16-macros.md). Macros aren't on watches.

### Lists of steps

**Macros** makes lists of steps that run one after another, without a script. **Add step** offers:

- **Run command**: any command except a macro.
- **Tap item**: activates the first item on screen whose text or description contains a text, ignoring case, waiting for it to appear up to **Seconds to wait for the item**, 3 seconds by default. An item whose whole text is the text comes first.
- **Tap at position**: taps the screen at an X and a Y in pixels. Needs Android 11 or later.
- **Touch and hold item**: touches the middle of an item for a time, then lifts in place or slides before it lifts, for buttons that act while held, such as the voice message button of a chat app.
- **Focus item**: finds an item as **Tap item** does and moves accessibility focus to it, which reads it.
- **Speak item**: finds an item the same way and reads its name without moving focus.
- **Long-press or scroll an item**: long-presses, scrolls forward or scrolls backward an item a number of times, or the largest list on screen when there is no item text.
- **Speak text**, **Type text**, at the cursor or in place of the text of the field, and **Open app**.
- **Wait**, up to 600 seconds, and **Wait until**, which waits until a condition is met, 10 seconds by default; the macro stops after that.
- **If**, **Else if**, **Else** and **End if**; **Repeat**, **Repeat while**, **End repeat** and **Exit loop**, up to 1,000 times; and **Stop macro**.
- **More steps**: **Play sound**, **Vibrate**, **Play tone**, **Copy text**, **Open link**, **Open settings page**, **Share text**, **Set volume**, **Set ringer mode**, **Device setting** (any control of the quick settings panel: on, off, toggle, a value or the next option), **Media key**, **Set timer**, **Set alarm**, **Call**, **Send message**, **Open camera**, **Press keys** (needs Shizuku), **Show message**, and three that keep a value: **Set value**, **Fetch link** (the text at an https address) and **Translate text** (into the language of the device).

Values are kept between runs, for all your macros together and for each script. The text of **Speak text**, **Type text** and the **More steps** takes a value as `{name}`, and `{item}` (the text of the focused item), `{clipboard}`, `{time}`, `{date}`, `{battery}` and `{device.wifi}` and the like (the state of a device setting).

Each step has **Edit**, **Add step after**, **Move up**, **Move down** and **Delete**. Steps in blocks are indented. When the blocks don't match, such as an **If** without **End if**, the screen of the macro shows the step and the problem, and the macro doesn't run. A macro has at most 200 steps.

### Conditions

A condition is a list of tests: **Item on screen**, **Focused item**, **Focus is inside an item**, **App in front**, **Keyboard shown**, **Screen locked** and **Value**. The item tests can match in **Any text**, **Text**, **Description**, **Hint**, **State**, **Tooltip** or **Resource ID**, as a regular expression, and require an item type: **Clickable**, **Long-clickable**, **Editable**, **Scrollable**, **Adjustable**, **Checkable**, **Checked**, **Selected**, **Heading** or **Enabled**. Each test has **Change to**, which reverses it, such as **Keyboard hidden** or **No item on screen**, and each test after the first has **Join with and** or **Join with or**. "And" goes before "or": the condition is met when all the tests of one of the groups that "or" separates are met. A condition has at most 20 tests.

### Where macros run

- **Run only when**, on the screen of the macro: a condition that the macro needs to run. When it isn't met, the command does nothing, and **Run now** announces "Macro not run. Run only when: not met".
- **App**: limits the macro to an app, which must be in front. The **Macros** screen lists the macros of **All apps**, and under **Apps**, each app with its macros.
- **Show in Actions**: lists the macro in the **Actions** menu and the **Actions** reading control of every item of its app, or of every app for **All apps**, after the actions of the app. **Show in Actions only when** limits it to items that meet a condition; on those items, **Change reading control automatically** selects the **Actions** reading control.
- **Add to home screen**: adds a home screen shortcut that runs the macro on the screen it is opened from. Renaming the macro renames the shortcut, and deleting the macro disables it.

Each macro is a command in the **Macros** category, so that a gesture, a keyboard shortcut, an arrow key, a braille cell, a menu or a voice command can run it. Starting a macro stops the one running, and so does **Stop macro**. When a step fails, such as an item that isn't found, SandFly announces "Macro stopped. Failed:" and the step. Deleting a macro removes it from the gestures, keys and menus that run it. Macros are included in exported settings, and in item files.

### Record a macro

**Record a macro**, in the **Macros** category, makes a macro named after the date and time and records its steps as you do them in apps: presses and long presses, scrolls, typed text, except in password fields, apps opened from the launcher, and the **Back**, **Home**, **Overview**, **Notifications**, **Quick Settings**, **All apps**, **Select all**, **Copy**, **Cut** and **Paste** commands. Running it again, running **Stop macro** or opening the **Recording macro steps** notification ends the recording and opens the macro with **Save**, **Record again** and **Cancel**. **Record steps**, on the screen of a macro, records steps into that macro. SandFly screens aren't recorded.

The recorded steps hold what the actions imply: scrolls in a list followed by a press on an item in it become a **Repeat while** that scrolls until the item is on screen; actions done several times in a row become a **Repeat**, or a **Repeat while** the first item pressed is on screen when it was gone after the last time; a press that turns a switch or check box on or off runs only when it is off or on; and when the first action is in the app in front at the start, the macro opens that app first. Items are found by their whole text, as a regular expression.

### Inspect items

**Inspect focused item**, in the **Developer settings** category, opens a window with the details of the focused item under headings: **Texts** (text, description, hint and state, named as **Match in** names them), **Item types**, **Items inside**, **Script details** (resource ID, role, class and rules) and **App** (app, activity and window). **Copy all** copies them as text, and activating a line copies its value. **Copy item details** copies the same details without the window, and **Copy screen tree** copies every item on screen as JSON. All three can be assigned to gestures, and added to the SandFly menu, where they are off by default.

## Direct touch

Audio games need raw touch, but explore by touch captures taps and swipes before the game receives them. With direct touch, SandFly passes your touches straight to the apps that you choose, so that you don't have to pause SandFly. For direct touch, see the user guide chapter [Touch](docs/user-guide/en/04-touch.md). Direct touch needs Android 11 or later, and isn't on watches or TVs.

### Built in from NVGT Bridge

Direct touch is all of [NVGT Bridge](https://github.com/trypsynth/nvgt-bridge), the accessibility service that gives audio games direct touch, built into SandFly. There's nothing extra to install or turn on in the accessibility settings, and no second service that can undo the touch setting of SandFly. Direct touch shares that setting with the pass-through gesture and the braille keyboard, so that none of them cancels another. NVGT Bridge searches the views of each window for dialogs and text fields, only five levels deep. SandFly already follows the windows, the keyboard and the screen state, so that it returns touch to you as soon as they change, with no depth limit. SandFly announces "Direct touch on" and "Direct touch off" in its own speech, also while the game plays audio.

### Choose your apps

Go to **Touch** > **Direct touch**, and turn on each app in the **Apps** list. When an app that you chose comes to the front, SandFly announces "Direct touch on", and when touch returns to SandFly, "Direct touch off". To turn the announcements off, turn off **Speak when direct touch changes**. For a short vibration instead or as well, turn on **Vibrate when direct touch changes**. The **Direct touch** switch at the top of the screen turns the whole feature on or off, and is on by default.

The **Turn direct touch on or off for this app** command, in **Activation**, adds the app on screen to the **Apps** list or removes it, and announces "Direct touch on" or "Direct touch off". Turning it on also turns on the **Direct touch** switch. Assign it to a volume key press, so that a sighted helper can use an app that doesn't work with SandFly while you still hear speech, and you can turn direct touch off again without touching the screen. With SandFly or the notification shade in front, SandFly announces "No app for direct touch".

### When SandFly takes touch back

Touch returns to SandFly when a dialog appears, another app opens on top, the notification shade or quick settings cover half of the screen or more, a text field takes focus, or the screen turns off. Touch goes back to the app when they're gone. The on-screen keyboard always stays with SandFly, so that you can explore it while the rest of the screen has direct touch.

### Direct typing

By default, the keyboard area keeps working with SandFly. For a game that draws its own keyboard, open the actions of the game in the **Apps** list and choose **Turn on direct typing**.

### Keyboards that speak by themselves

A keyboard that speaks by itself, such as a braille keyboard app, needs your touches in every app. Turn it on in the **Apps** list. While it shows, touches on it go straight to it in any app, and the rest of the screen stays with SandFly.

### Quick settings tile

To turn direct touch off and on again without leaving your game, add the **Direct touch** tile to quick settings.

### Back up and restore direct touch settings

**Back up settings**, on the **Direct touch** screen, saves your direct touch choices to a file, and **Restore settings** loads them on another device.

### Direct touch requested by a game

If you develop a game, add this `<meta-data>` element inside your `<application>` element or your main `<activity>` element, and SandFly turns your game on the first time it comes to the front. Players can still turn it off.

```xml
<meta-data
    android:name="dev.nvgt.capability.DIRECT_TOUCH"
    android:value="true" />
```

A game can also take direct touch only on parts of the screen, such as its board, while explore by touch works on the rest. It can declare the areas in its manifest for each activity and for states that it sets, request areas or the whole screen while it runs, and stop direct touch for itself. [Choosing where your app gets direct touch](docs/direct-touch-api.md) describes the API.

## Image and screen descriptions

For these settings, see the user guide chapter [Image and screen descriptions](docs/user-guide/en/17-descriptions.md). Descriptions aren't available on watches and TVs.

### AI services

The source release of TalkBack doesn't include working Gemini support, so that **Describe image** only reads text in images and **Describe screen** doesn't work. SandFly adds its own **Describe screen** with follow-up questions, and sends both to an AI service with your own API key, to SandFly Pro, or to a model on the phone. **AI services**, in the main settings, has a settings screen for each service: **Gemini settings**, **Claude settings**, **OpenAI (ChatGPT) settings**, **Mistral settings**, **Groq settings**, **OpenRouter settings**, **Ollama Cloud settings**, **Qwen settings**, **Zhipu GLM settings**, **Baichuan settings**, **DeepSeek settings**, **Kimi settings**, **Hunyuan settings**, **SiliconFlow settings** and **Other service settings**, for any service with an OpenAI-compatible API, and in builds with SandFly Pro, **SandFly Pro settings**. Each has its API key, with **Get a key** where the service offers one, and its model, chosen from the list that the service gives for your key.

**AI services** > **AI service** chooses among the services that are set up, or **None**. A service that has just been set up is used when no other is, and when the service in use loses its key or model, the next one that is set up takes over. Questions about an image or screen, spoken or typed, Live recognition, voice commands with AI, dictation correction and translation with the **AI service** option use the same service. API keys stay on the phone and out of exported settings.

To turn on descriptions, turn on **Image and screen descriptions** > **Detailed image descriptions**. **Image and screen descriptions** > **Automatic descriptions for text in images** reads the text in images when focus moves to them: **All images**, **Unlabelled images only**, the default, or **Don't describe images**.

ML Kit, which TalkBack recognizes text with, has no Cyrillic recognizer. SandFly reads Cyrillic text with Tesseract OCR once the model of the language is downloaded in **Image and screen descriptions** > **Cyrillic text recognition**, for the speech language or the first phone language written in Cyrillic. Not on watches.

With Gemini, SandFly requests as little thinking as the model allows, and requests Gemini Flash-Lite when the chosen model is busy or takes more than 15 seconds. **Gemini model** is **Automatic** by default, which uses Gemini Flash.

### Spoken descriptions and questions

**Speak screen description** and **Speak image description** describe the screen or the focused image as **Describe screen** and **Describe image** do, and speak the description instead of showing it in a window. **Ask about screen** and **Ask about image** take a screenshot of the screen or the focused image, then record a question with the speech recognizer chosen in **Typing** > **Dictation**. Recording stops after a pause in speech, or when you run the command again; the question is sent and the answer is spoken. When recording can't start, a text field for the question opens instead. Before you agree to send screenshots to the AI service, these commands run **Describe screen** or **Describe image**, which show the dialog for that agreement. The commands are in the **Image and screen descriptions** category.

### Take a picture and describe it

**Take picture and describe**, in **Image and screen descriptions**, takes one picture with the rear camera and describes it as **Describe image** does, with the same window for questions. **Take another picture** takes another.

### AI actions reading control

The **AI actions** reading control, in the reading controls by default, runs **Describe screen**, **Speak screen description**, **Ask about screen**, **Describe image**, **Speak image description**, **Ask about image**, **Share image of focused item** and **Share screenshot**, and with SandFly Pro, **Start or stop AI agent task** and **Type AI agent task**. Swipe up or down to choose an action, then double-tap to run it. Double-tap runs the action that the reading control announced until focus moves. **AI actions to include**, after **AI actions** in **Reading controls**, chooses which actions the reading control offers; all are included by default. Actions that aren't available, such as **Describe screen** in a phone language that it doesn't support, are left out, and the reading control is hidden when none is available. It needs Android 11 or later.

### AI service reading control

The **AI service** reading control, off by default, switches **AI services** > **AI service** between the services that are set up with a swipe up or down, and says the name of the service. **None** and SandFly Pro without a licence aren't in it. With fewer than two services set up, it says so and changes nothing. **AI service: next value** and **AI service: previous value** in **Commands** do the same.

### On-device AI

SandFly can describe images and screens with a vision model that runs on the phone, with nothing sent to Google or anyone else after the download. Go to **AI services** > **On-device AI**, choose **Model**, then **Download model**, and turn on **Use on-device AI**. **Gemma 4 E2B** is the recommended model, and the default on phones with enough memory for it. The list shows only the models that fit the memory of the phone; **Developer settings** > **Ignore on-device AI memory limits** lists every model. **Use a model file from storage** installs a Gemma 4 `.litertlm` file. On phones that have one, **Processor** runs the model on the GPU, or on the NPU of Google Tensor G5 and G6 (Gemma 4 E2B) and Snapdragon 8 Elite and 8 Elite Gen 5 (FastVLM 0.5B). On-device AI needs a 64-bit ARM processor, and isn't on watches.

**AI services** > **Keep recognition models loaded** sets how long models stay in memory after use, and **AI services** > **Model download server** is **Hugging Face** or **hf-mirror.com**. Both settings are also in **Typing** > **Dictation**.

While the on-device model loads, a sound of its own repeats every 2 seconds; once the model is loaded and works on the image, the loading sound plays instead. Both can be turned off in **Individual sounds and vibrations**, and a [sound theme](themes.md) can replace them.

### Live recognition

**Image and screen descriptions** > **Live recognition** opens a camera screen like Live Recognition in VoiceOver, with **Scenes**, **Text**, **Point and Speak**, **People detection**, **Door detection**, **Color detection**, which names the colors in the centre of the camera view, **Light level**, which reads the light level from the light sensor, and **Light level tone**, a tone whose pitch follows the brightness of the camera view. The **Live recognition** reading control, in the reading controls by default, turns each mode on and off, and the **Live recognition** category has a command for each mode. Text is recognized on the phone. **Scenes**, **Point and Speak**, **People detection** and **Door detection** use what **Describe image** uses: the on-device model, the AI service in use, or, with Gemini, the Gemini Live API. Results are spoken in the SandFly voice, also while other audio plays.

With the on-device model or the AI service in use, the loading sound plays until the first description, and the model loading sound plays whenever the on-device model loads.

### Clear error messages

When Gemini can't describe something, TalkBack speaks "Something went wrong". SandFly speaks what failed: the key wasn't accepted, the usage limit was reached, the service is busy, it took too long, or there's no internet connection. For other errors, SandFly speaks the error code and the first sentence of the message of the service, in English. **Describe screen** speaks its errors instead of showing them in a toast.

## SandFly Pro

SandFly Pro is a paid add-on that adds AI features without an API key of your own. For SandFly Pro, see the user guide chapter [SandFly Pro](docs/user-guide/en/18-sandfly-pro.md). Its settings are in **SandFly Pro**, in builds with SandFly Pro, and **AI services** > **SandFly Pro settings** opens the same screen.

- **AI agent**: **Start or stop AI agent task** and **Type AI agent task** carry out a task that you say or type in the app in front, such as "start a new game", through taps, swipes, typing and the items on the screen. Actions such as payments and sending messages need your permission, and actions such as entering passwords are blocked.
- **Screen recognition**: **Recognize screen** finds the buttons and other elements that an app draws without making them accessible, and adds them as items that you can read, touch and activate. **SandFly Pro** > **Automatic screen recognition** recognizes every screen of the apps you choose.
- **Script agent**: **Build a script with SandFly Pro**, on the **Scripts** screen, writes a script from a description of what it should do, and opens it in the script builder for review.
- **AI service**: **SandFly Pro** as the AI service in **AI services** describes images and screens, answers questions, describes the camera view in Live recognition and translates.

SandFly Pro needs an internet connection. The agent, screen recognition and the script agent need Android 11 or later. Each code has a monthly usage limit.

## Translation

TalkBack has no translation commands. For these settings, see the user guide chapter [Translation](docs/user-guide/en/19-translation.md).

### Translate the last spoken phrase

**Translate last spoken phrase**, in the **Reading** category, translates what SandFly spoke last and speaks the translation. In a text field, it translates the text of the field instead, except in password fields. Run again right after, it translates the same phrase again, not the translation, so that a language changed in between takes effect. The translation stays in the speech history, so that repeat, spell and copy last spoken phrase work on it. Messages such as "Nothing to translate" don't. Text over 5,000 characters is cut. A translation is kept until the phrase or the settings change, so that running the command again doesn't send the text to the service again.

**Translation** has:

- **Translation service**:
  - **On-device**, the default on phones: ML Kit translation, in 59 languages. Each language model, about 30 MB, downloads from Google the first time it is needed, with English, which translation between two other languages goes through. **On-device languages** lists the languages, and downloads or deletes one. **Download all languages** downloads every language that isn't downloaded, one at a time, after a dialog with their number and size, about 1.6 GB for all; it announces each quarter while **Translation** is open, continues after you leave the screen, and stops when you choose it again. On an Android 17 emulator, 35 languages took under 3 minutes. **Download languages on Wi-Fi only** holds downloads until Wi-Fi. The language is detected with ML Kit language identification, which Google Play services downloads the first time; until then, SandFly announces "Language detection is downloading". Not on watches.
  - **AI service**: the on-device AI model when **Use on-device AI** is on, otherwise the AI service in use in **AI services**, in one request that also returns the detected language. The default on watches.
  - **Google Cloud Translation**, **DeepL** and **Microsoft Translator**, with your API key, and for Microsoft, the Azure region of your resource, empty for a global one. A DeepL key that ends in ":fx" uses the free API of DeepL. Choosing one without a key opens the field for it.
  - **LibreTranslate**, at `https://libretranslate.com` or the address of your own server, https only, with an API key if the server needs one.
- **Translate from**: **Detect language**, the default, or a language.
- **Translate into**: **Device language**, the default, or a language.
- **Translate text in the target language into**, shown only while the language is detected: a second language for text that is already in the target language, such as German for an English speaker who reads both, or **Don't translate**, the default, which announces "Already in English".
- **Swap languages**, shown only while **Translate from** is a language.
- **Use a voice for the target language**, on by default: the translation is marked with its language, so that the voice changes as for other text in that language, following **Switch language automatically**.
- **Say the original language**, off by default: "From German:" before the translation.
- **Copy translations to the clipboard**, off by default.

API keys stay out of exported settings.

### Automatic translation

With **Translation** > **Automatic translation** on, speech in another language than **Translate into** is translated before it is spoken, notifications included, also those read in the voice chosen for their app. The **Automatic translation** reading control, off by default, turns it on and off.

**Translate notifications** translates only notifications, with **Automatic translation** off. A notification is translated before it is queued, so that it keeps its place. One read with a voice profile, chosen for all notifications, an app or a rule, is read in the voice of that profile, without a voice for the target language. Private notifications aren't translated, so that their text is never sent to a service.

The language of each utterance is identified on the phone with ML Kit language identification, at a confidence of at least 70%. Speech already in the target language, in one of **Languages not translated**, or, when **Translate from** is a language, in any other language, is spoken as it is and never sent to a service. Text with fewer than three letters isn't checked. Recent languages and translations are kept, so that going back to an item doesn't translate it again.

**Automatic translation service** is **On-device**, **Google Cloud Translation**, **DeepL**, **Microsoft Translator** or **LibreTranslate**; **AI service** isn't offered, as it takes too long for every utterance. It is the translation service unless that is the AI service, and then **On-device**.

The sounds and vibrations of an utterance play at once; its words follow when the translation is ready. Speech after it waits for it, so that the order stays the same, and newer speech that interrupts drops it, except notifications. A translation that takes longer than 3 seconds, or fails, is replaced by the original text. An on-device language that isn't downloaded starts downloading with the first text identified as it with at least 90% confidence and at least 12 letters, or with the second text identified less surely, so that one phrase taken for another language doesn't download a model; SandFly announces this once. Speech that comes while the language downloads is spoken untranslated, and once it has downloaded, the item you are on is spoken again translated, unless you moved on in the meantime. A notification waits for the download, up to 30 seconds, and is then read translated. On an Android 17 emulator, moving between items took as long with **Automatic translation** on as off.

## Scripts and other apps

For scripts, see the user guide chapter [Scripts and custom labels](docs/user-guide/en/20-scripts.md) and [Writing SandFly scripts](docs/scripting.md).

### Lua scripts and plugins

As the app modules and global plugins of NVDA do, scripts change how SandFly works in one app or in every app. Scripts are written in Lua 5.4. A script can:

- Give unlabeled buttons names, hide clutter, group items, and change or silence what SandFly speaks for an item.
- Read the screen, press buttons, speak, play sounds and vibrate.
- Add gestures, keyboard shortcuts, SandFly menu items, reading controls, and items in the Actions menu of an item.
- Change everything that SandFly speaks, and react to notifications, typing and screen changes.
- Use the internet and the clipboard, show dialogs, and keep its own data and settings.
- With the permission for Java code, call any Java code through LuaJava.

To install a script, go to **Scripts** > **Import script**. You can also share a script file, or an `https` link to one, with SandFly, or open a script file with it, and choose **Install SandFly script**. **Scripts** > **Script sources** lists the scripts in repositories on GitHub, GitLab, Codeberg, Gitea or Bitbucket, to install them; the SandFly scripts repository is listed by default. Before it installs or updates a script, SandFly shows where it came from, whether SandFly signed it, the permissions it needs and the commands it adds. **Add the example script** adds a small script to start from. **Scripts** > **New script from code** installs a script entered as code. **View code** and **Edit code**, on the page of a script, show and change its `main.lua`; **Edit code** isn't shown for scripts made in the script builder.

Each script has a page with its switch, a switch for each permission, its commands with their gestures and keys, its settings, its log, **Run earlier** and **Run later**, **Clear stored data** and **Remove**. While a script runs, its gestures and keys come before the gestures and keys of SandFly.

Scripts never change SandFly screens, so that the **Scripts** screen always works. **Scripts** > **Turn off all scripts** stops every script at once, and so does the **Turn all scripts on or off** command, in the **SandFly** category, which you can assign to a gesture that scripts can't take over. Scripts don't run while SandFly is paused, or while the screen is off unless you turn on **Keep running with the screen off** for a script that offers it, and SandFly starts the script engine only once a script is turned on. A script with repeated errors is turned off.

Speech requested with `sf.speak` is spoken while media plays, during calls, while another app records from the microphone and during dictation, including queued and interrupting requests. The script still needs the speech permission, and muted speech stays muted. Hooks that change or suppress speech take priority over queued script work and share a 30-millisecond deadline; if the deadline expires or a long task is already running, SandFly uses its normal speech.

Once a day, SandFly checks installed scripts for a newer version in the source they came from, and **Scripts** > **Check for updates** checks now. With **Update scripts automatically** on, the default, a newer version installs without a dialog when it has the same author, needs no new permissions, is made for this version of SandFly and comes from the source of the script; from the SandFly repository, only if SandFly signed it. Other updates are listed under **Updates** on the **Scripts** screen. Scripts run on phones and tablets only. [docs/scripting/examples](docs/scripting/examples) has a script to start from.

### Script builder

**Scripts** > **New script** makes a script without code. Its parts are **Apps**, **Rules**, which change items with labels, hiding, reading order, actions and what is read, **Reading controls**, **Commands**, made of steps as in a macro, **Triggers**, such as when focus moves to an item or every day at a time, **Speech changes**, **Settings**, **About** and **Code**, for Lua code added to the end of the script. Each change is saved and applies at once. **Add a rule for this item**, in the window of **Inspect focused item**, adds a rule for the focused item. **Export script** saves a built script as a `.zip` file that can still be changed in the script builder.

### Speech from other apps

Apps can send text to SandFly to speak in your SandFly voice and settings, for example a game, an automation app such as Tasker, or a remote-access tool. To allow this, turn on **Speech** > **Other apps** > **Let other apps speak**. It's off by default.

The first time an app tries to speak, SandFly posts a notification with **Allow** and **Don't allow**, and nothing from the app is spoken until you choose **Allow**. You can allow or block each app later in **Let other apps speak**. To hear a short sound before each text from another app, turn on **Play a sound before other apps speak**. Nothing is spoken while the device is locked unless the app declares that it speaks while locked and you allow that separately, with the "while the device is locked" check box of the app or the first notification. An app can speak only a few times in a row, and apps can never stop or cut off SandFly speech. The keyboard in use can speak without being in the list. [Speaking through SandFly from another app](docs/speech-api.md) describes the API for app developers.

## Tutorial

TalkBack has a tutorial of pages that describe gestures, with a few practice areas. SandFly has an interactive tutorial, modeled on the VoiceOver tutorial on the Mac. To open it, go to **Help & feedback** > **SandFly tutorial**, or run the **Open tutorial** command. It also opens the first time SandFly is turned on, after the **Choose gestures** dialog. For the tutorial, see the user guide chapter [Get started](docs/user-guide/en/01-getting-started.md).

The tutorial has 16 lessons in five chapters: Basics, Reading, Controls and text, Getting around, and Practice. Each lesson has an instruction and items to practice on. Each step waits until you have done what it describes with your own gestures, then gives the next step. For example, the step that teaches swiping waits until a swipe right reaches a particular item, the step that teaches exploring by touch waits until you have touched four squares, and the step that teaches headings waits until the **Headings** reading control has moved to a particular heading. The tutorial teaches the gestures of the gesture layout you chose.

The tutorial speaks each instruction itself, also while other audio plays, and speaks it again after 25 seconds without a gesture or touch, up to three times. When a lesson is done, SandFly announces which gesture starts the next lesson and moves focus to **Next lesson**. The instructions name the gesture that each action has in your gesture settings, one-finger gestures first. When an action has no gesture, the step states this, and **Skip step** goes on without it.

In the lessons on Back, Home, recent apps, notifications, screen search and turning speech off, the tutorial takes those gestures and announces what they do, instead of leaving the tutorial or turning speech off. In **Gesture practice**, every gesture announces its name and command without running it, except moving to the next and previous item and activating, so that you can reach **End practice**. **Help & feedback** > **Practice gestures** and **Gestures** > **Practice gestures** open this lesson.

**Contents** lists the lessons, shows which are done, and opens any of them. The tutorial reopens at the lesson you were on, or at the next one if that lesson was done. In a lesson, gestures don't leave it: the tutorial takes Back, Home, recent apps, notifications, quick settings, all apps, the lock screen, the power menu, the accessibility button, opening an app, and opening the tutorial or gesture practice again, and announces what the gesture would have done. It takes turning speech off and pausing SandFly the same way. Back still closes the keyboard and menus. From **Contents**, all of these work, and **Close tutorial** closes the tutorial.

On a device with a physical keyboard and no touch screen, the **SandFly keyboard tutorial** opens the first time instead. It teaches the keyboard shortcuts of your keymap in 15 lessons, including **Keyboard shortcut list**, **Browse mode** and **Keyboard practice**. On a device with a touch screen and a physical keyboard, **SandFly tutorial** offers **Touch tutorial** and **Keyboard tutorial**.

**Help & feedback** > **Gesture trainer** measures gestures against the limits SandFly uses with your settings, and lists the settings that change those limits. It needs Android 13 or later, and isn't on watches or TVs.

Limitations:

- The tutorial takes only gestures that SandFly recognizes. With **Developer settings** > **Handle gestures in SandFly** off, the navigation gestures of Android aren't taken, so that they can leave the tutorial, and gesture practice can't name them.
- The slider and actions lessons need **Reading controls** > **Change reading control automatically** on, which it is by default.
- Wear OS watches and TVs keep tutorials of their own.

## Pause and turn off

For these settings, see the user guide chapter [Pause and turn off SandFly](docs/user-guide/en/23-pause-and-turn-off.md).

### Pause SandFly

Pausing turns off SandFly speech, sounds, vibration, gestures and explore by touch, so that the phone works as if no screen reader were on. SandFly stays on, so that it resumes immediately. TalkBack 8.1 and earlier had this feature as suspend.

To pause, choose **Pause SandFly** in the SandFly menu, where it is by default, assign the **Pause SandFly** command to a gesture, or assign a key to the **Pause or resume SandFly** keyboard shortcut. The first time, the **Pause SandFly?** dialog shows how to resume.

To resume, tap the **SandFly is paused** notification, press volume down three times quickly, or press the keyboard shortcut again. **Pause and turn off** > **Resume SandFly** sets when SandFly also resumes by itself: **When the lock screen shows**, the default, **When the screen turns on**, or **Only from the notification or a shortcut**.

The volume keys still change the volume while SandFly is paused, and the keys of a physical keyboard go to the app. If SandFly restarts while it's paused, it starts unpaused. Pause SandFly isn't available on watches and TVs.

### Turn off SandFly

To turn SandFly off before you turn on another screen reader, such as TalkBack, choose **Turn off SandFly** in the SandFly menu, where it is by default, or assign the **Turn off SandFly** command to a gesture, and then confirm.

SandFly recognizes gestures itself, which sets a flag on the touch screen. Android keeps the flag after SandFly is turned off unless SandFly clears it, whenever another accessibility feature still uses input, and a screen reader turned on next that leaves gesture recognition to Android then receives no touches. SandFly clears the flag when it turns off in any way: with **Turn off SandFly**, the accessibility shortcut, a Quick Settings tile or the Android settings. After SandFly is off, its process ends within a few seconds.

## Settings

For the settings screens, see the user guide chapters [Get started](docs/user-guide/en/01-getting-started.md), [Settings profiles and backups](docs/user-guide/en/22-profiles-and-backup.md) and [Help and feedback](docs/user-guide/en/25-help.md).

### Layout and wording

TalkBack groups its main settings under **Audio**, **Visual**, **Controls**, **Typing**, **Braille**, **Advanced** and **Tutorial & help**, with many settings in **Advanced settings** and **Reduce delay**. The main settings screen of SandFly has no groups, and lists these screens: **Speech**, **Verbosity**, **Sound and vibration**, **Audio output**, **Touch**, **Gestures**, **Keyboard shortcuts**, **Volume keys**, **Commands**, **Reading controls**, **SandFly menu**, **Custom menus**, **Macros**, **Voice commands**, **Navigation**, **Typing**, **Braille**, **Display**, **Notifications**, **Translation**, **Image and screen descriptions**, **AI services**, **Scripts**, **Custom labels**, **Quick settings**, **Overview**, **Pause and turn off**, **Settings profiles**, **Back up and restore**, **Updates and installation**, **Shizuku and root**, **Help & feedback** and **Developer settings**. Each setting is on the screen named after what it changes.

These TalkBack settings are on other screens in SandFly:

| TalkBack | SandFly |
|---|---|
| **Advanced settings** > **Reduce delay** > **Focus delay** | **Touch** > **Focus delay** |
| **Advanced settings** > **Reduce delay** > **Reset all delay settings** | **Touch** > **Reset all delay settings** |
| **Advanced settings** > **Reduce delay** > **Typing preference**, **Typing focus delay**, **Touch and hold delay when typing** and **Announce keyboard keys faster** | **Typing** > **On-screen keyboard**, with **Typing preference** named **Typing method** |
| **Advanced settings** > **Single-tap to activate** | **Touch** > **Single-tap to activate** |
| **Advanced settings** > **Cover proximity sensor to stop speech** | **Speech** > **Stopping speech** > **Cover proximity sensor to stop speech** |
| **Advanced settings** > **Element description order** | **Verbosity** > **Order of item details** |
| **Advanced settings** > **Time format** | **Verbosity** > **Time format** |
| **Advanced settings** > **Reduce window announcement delay** | **Display** > **Turn off animations** |
| **Advanced settings** > **Automatically switch to table navigation reading control when entering a table** | **Navigation** > **Switch to table navigation in tables** |
| **Advanced settings** > **Smart browse mode** | **Navigation** > **Smart browse mode** |
| **Advanced settings** > **Show instructions for turning off TalkBack** | **Pause and turn off** > **Show instructions for turning off SandFly** |
| **Advanced settings** > **Custom labels** | **Custom labels** |
| **Advanced settings** > **Developer settings** | **Developer settings** |
| **Change focus indicator** | **Display** > **Focus indicator** |
| **Display speech output** | **Developer settings** > **Display speech output** |
| **Gemini settings** | **AI services** > **Gemini settings** |
| **Automatic descriptions** | **Image and screen descriptions** |
| **Edit menus** > **Customize TalkBack menu** | **SandFly menu** |
| **Edit menus** > **Customize reading controls** | **Reading controls** |
| **On-screen keyboard** and **Physical keyboard** | **Typing** |
| **Verbosity** > **Keyboard echo: On-screen keyboard** and **Keyboard echo: Physical keyboard** | **Typing** > **Keyboard echo** |
| **Verbosity** > **Speak letters with examples** and **Use pitch changes** | **Speech** > **Pronunciation** |
| **Sound and vibration** > **Audio ducking** | **Audio output** > **Audio ducking** |
| **Tutorial & help** > **TalkBack tutorial** | **Help & feedback** > **SandFly tutorial** |

Many settings have clearer names, such as **Speak item type** and **Order of item details**. SandFly removes the links to the Play Store, the privacy policy and terms of service of Google, Disability Support and the TalkBack help, and the pages about new features in TalkBack. SandFly has its own **New features in SandFly** pages after an update, and **Help & feedback** > **Privacy policy** opens the SandFly privacy policy.

### Search

**Search**, at the top of the main settings screen, finds settings and commands on every settings screen. Type one or more words; each result has every word in its title or description, also inside a word, ignoring case and accents. Each result shows its title and the screens and groups that lead to it, such as **Speech** > **Stopping speech**. Choosing a setting opens its screen with the setting focused and its group expanded. Choosing a command opens it in **Commands**. Screens made of lists, such as apps, profiles, rules, scripts, **Individual sounds and vibrations** and **Status readout**, are found by their entries only.

**Search** is also at the top of **Gestures**, which finds a gesture by name or by the command it runs, and of **Keyboard shortcuts**, which finds a command or arrow key and opens its shortcut. On phones and tablets, the list of commands for a gesture starts with **Search**, which finds a command by name or category and assigns it.

### User guide

**Help & feedback** > **User guide** opens the SandFly User Guide, in the language of SandFly where it is translated, with a table of contents and links between sections. Its source files are in [docs/user-guide](docs/user-guide). **Help**, in the app bar of most settings screens, opens the section of the guide about that screen.

### App icon that opens the settings

SandFly has an app icon in the launcher, labeled SandFly, that opens its settings. The settings of TalkBack open only from the accessibility settings. The settings of SandFly open from the icon whether SandFly is on or off, so that you can set it up, or change a setting, without turning it on first.

### Dark mode

On Android 12 and later, SandFly settings follow the dark mode of the system.

### Slider titles

SandFly doesn't stop on the title of a slider as a separate item when you swipe through settings.

### Feedback

**Help & feedback** > **Send feedback**, also in the SandFly menu, sends a problem report or suggestion to the SandFly developer without a GitHub account. It has three parts:

- **New feedback**: the form, with **Type of feedback**, **Area**, **Title**, **App where it happens**, **Description**, **Steps to reproduce**, **Email for replies**, **Include device information** and **Include SandFly log**. What you type is kept as a draft until you choose **Send** or **Delete draft**.
- **Inbox**: the feedback you sent, each with its status (**Open**, or **Closed**: completed, not planned or duplicate) and the number of replies you haven't read. Open one to read the replies and send your own, with **Your reply** and **Send reply**.
- **Drafts**: feedback that you started and haven't sent.

While SandFly is on, it checks for replies and status changes every hour, and when you open **Send feedback**. A reply or a new status posts a notification, with sound, that opens the feedback. Closed feedback is still checked for 30 days. **Remove from inbox** stops the checks and deletes the feedback from the phone; the developer keeps it. **Report AI response**, in **Image and screen descriptions**, opens a new report with the last text that an AI model gave.

### Export and import settings

**Back up and restore** > **Export settings** saves the SandFly settings to a `.zip` file, with custom labels, settings profiles, installed scripts, sound themes and the wallpaper image. **Back up and restore** > **Import settings** replaces the settings with those in a file, such as one from another phone, and adds the labels, profiles, scripts and sound themes in it, each replacing the one with the same item, name or ID. API keys, the SandFly Pro license, downloaded models, logs and sound theme drafts stay out of the file. **Import settings** also reads `.json` settings files from earlier versions of SandFly, and settings files exported from Backtalk: the settings in a Backtalk file replace those of SandFly, and settings that only SandFly has are kept. Not on watches.

### Item files

An item file holds SandFly items of one or more kinds, without other settings, to share them or move them to another phone. **Export to file** and **Import from file** are on the screens of macros, gesture profiles, gestures in apps, keyboard shortcuts in apps, keymaps, settings profiles, the speech, symbol and braille dictionaries, custom braille characters and custom voice commands. An imported item replaces the item of the same kind with the same name. **Back up and restore** > **Export file format reference** saves a document that describes item files, with the gestures, commands and settings of the phone, so that an AI chat can write items. See the [item file reference](docs/reference/item-files.md). Not on watches.

### Settings profiles

A settings profile is a named set of settings that replace the SandFly settings while the profile is in use. A profile holds only the settings changed in it; every other setting stays as it is without profiles. **Settings profiles** lists the profiles, with those in use marked **In use**. **New settings profile** adds a profile that sets nothing yet. Each profile has a screen of its own:

- **Use now** uses the profile everywhere, until **Stop using**. One profile at a time is used this way.
- **Change settings** opens the settings with the profile applied, and every setting changed there goes to the profile until the screen of the profile is shown again or another app opens; SandFly announces "Changes go to" and "Changes saved to" with the name of the profile.
- **Use automatically** has **Web content**, **Continuous reading** and **Text editing**: the profile is used while focus is in web content, during continuous reading such as **Read from top**, and while focus is in an edit field or on the keyboard.
- **Apps** and **Keyboards**: the profile is used while one of the apps or keyboards listed there is shown.
- **Settings in this profile** lists the settings that the profile sets, by their titles; choosing one removes it from the profile.
- **Rename**, **Move up**, **Move down**, **Export to file** and **Delete**.

When profiles in use set the same setting, the profile used with **Use now** wins, then the profile higher in the list. Profiles used automatically aren't used in the SandFly settings, which show the settings without them, and aren't used while SandFly is off. A setting changed while a profile in use sets it, such as the speech rate changed with the reading controls, is saved to that profile; other changed settings apply without profiles.

The **Choose settings profile** command opens a menu with **No settings profile** and the profiles, and **Next settings profile** and **Previous settings profile** go through the profiles and **No settings profile**. They choose the profile used everywhere, announce the name of the profile or "No settings profile", and are in the **SandFly** category, unassigned.

A profile can hold the main, braille keyboard and braille display settings, including gestures, keyboard shortcuts, gesture profiles and the gesture profile in use. API keys, the SandFly Pro license, the time of the last update check, installed scripts and their settings, sound theme drafts, and the state of the phone, such as which tutorials and dialogs were shown, aren't in profiles. **Export settings** saves the settings without profiles, and the profiles beside them. **Import settings** replaces the settings, adds the profiles in the file, and applies the profiles in use again. Profiles made before profiles held only their changes become profiles of the settings in which they differ from the profile that was in use.

**Settings profiles** > **BlackBerry mode** makes SandFly work similarly to the BlackBerry 10 Screen Reader. Turning it on opens **Turn on BlackBerry mode?** first. It makes and uses a **BlackBerry mode** profile that sets **BlackBerry gestures**, the **BlackBerry keymap** on keyboard phones, the **BlackBerry** sound theme, and fewer announcements. **Turn BlackBerry mode on or off** and **Quick Tasks** are commands in the **SandFly** category.

## Updates and installation

For these settings, see the user guide chapter [Updates, Shizuku and root](docs/user-guide/en/24-updates.md).

### Separate app

SandFly installs as `io.github.aaron_gh.sandfly`, next to Google TalkBack, with settings of its own. See [Install SandFly](README.md#install-sandfly).

### Built-in updates

SandFly checks the SandFly releases on GitHub 30 seconds after it starts and then once a day, and shows the **SandFly update available** notification with the changes; tap it to download and install the update. **Updates and installation** > **Check for updates** checks now and shows the version and build number. With **Updates and installation** > **Development builds** on, SandFly also offers development builds, whichever build is newer, and checks about once an hour. When the connection drops, SandFly continues the download where it stopped, and the download notification shows the time left. SandFly installs only builds signed with the SandFly key. See [Updates](README.md#updates).

### Appear installed from Google Play

Some apps, such as some banking apps, turn off accessibility services that Android records as installed from anywhere other than Google Play, which stops SandFly from working with those apps when you installed it from a release. **Updates and installation** > **Appear installed from Google Play**, off by default, makes Android record Google Play as the installer of SandFly, so that those apps keep SandFly on.

Android records the installer only when an app is installed, and only the shell or the system can choose it, so SandFly installs its own APK again through Shizuku or root, naming Google Play. It is the same app, with the same settings; only what apps are told about the installer changes. The install restarts SandFly, so that speech stops for a moment. While the setting is on, the built-in updater installs updates the same way, so that an update keeps the recorded installer.

Without Shizuku or root, the setting can't change the installer; its **Copy command** action copies the adb command to run from a computer. Turning the setting off records the installer that SandFly had before. An app may still detect a sideloaded install in other ways. If something records another installer, such as an update installed while Shizuku isn't running, SandFly records Google Play again the next time Shizuku allows SandFly. While the setting is on but Google Play isn't recorded yet, the summary of the setting states this.

### Shizuku and root

Some features need permissions that Android grants only through adb. With [Shizuku](https://shizuku.rikka.app) running and allowing SandFly, SandFly grants itself these permissions and runs commands that only adb can run. On a rooted device, **Shizuku and root** > **Use root instead of Shizuku**, off by default, runs these commands with `su` as the shell user. The features that use Shizuku or root include **Fix stuck touch exploration**, **Speaker when away from your ear**, **Reset accessibility audio after calls**, **Volume key shortcuts with the screen off** (Shizuku only), the SandFly overview and Alt+Tab, the switches of the SandFly quick settings panel, copies from other apps in the clipboard history, Gboard key vibration, accessibility shortcuts of other services, key presses from scripts and macros, the **App tiles** and **System tiles** commands and **Add system tile**, app shortcuts that apps add while they run, and **Appear installed from Google Play**. Without them, a setting that needs an adb permission shows the adb command, and its **Copy command** action copies it.

## Android TV

### Settings on the TV home screen

Android TV has no other way to open the settings of an accessibility service, so SandFly adds **SandFly settings** to the apps on the TV home screen.

### Phone-only features hidden

Settings for features that need a touch screen or a phone are hidden on TVs, such as **Gestures**, **Direct touch**, **Status readout**, **Use SandFly's overview**, **Use SandFly's notification list**, **Speaker when away from your ear** and **Image and screen descriptions**, and the **SandFly menu** and **Reading controls** screens. Pause SandFly also isn't available on TVs, as the volume keys often go to the TV or a soundbar, and there's no notification shade to resume from. TVs often don't show notifications, so that the update notification may not appear; use **Updates and installation** > **Check for updates**. See the user guide chapter [Watches and TVs](docs/user-guide/en/26-watches-and-tv.md).

## Wear OS watches

For watches, see the user guide chapter [Watches and TVs](docs/user-guide/en/26-watches-and-tv.md).

### Same settings as phones

Watches have a build of their own, `sandfly-wear.apk`, with the same SandFly settings as phones, including **Speech**, all of **Verbosity**, **Individual sounds and vibrations**, **Lift to activate**, **Turn off animations**, **Status readout**, the rotor, the reading controls, the delay settings, update checks, and the circle menu, which is scaled to fit the screen. Settings for features that watches don't support are hidden: scripts, macros, custom menus, custom reading controls, voice commands, direct touch, back tap, edge and corner gesture settings, the braille keyboard, the handwriting keyboard, the SandFly keyboard, on-device AI, **Use SandFly as the wallpaper**, **Speaker when away from your ear**, **Reset accessibility audio after calls**, **Volume key shortcuts with the screen off**, **Resume SandFly** and **Show instructions for turning off SandFly**. On watches, the reading controls include **Scroll** and **Accessibility volume** by default, and **Audio output** > **Low-latency audio** is off by default.

### Gestures on watches

Watch screens track at most two fingers, so that gestures with three or four fingers don't work, and two-finger swipes scroll the app. Watches use the default gestures of phones except for these:

| Gesture | Command on watches |
|---|---|
| Double-tap and hold with 2 fingers | Open SandFly menu |
| Triple-tap and hold with 2 fingers | Turn speech on or off |
| Swipe left then up | Repeat last spoken phrase |
| Swipe right then up | Increase volume |
| Swipe right then down | Decrease volume |
| Swipe left then down | Unassigned |

On Samsung watches, turn on **Gestures** > **Reserve gestures for Vibration Watch** to let Samsung Vibration Watch use two-finger single and double taps. The setting is off by default, and Vibration Watch must also be turned on in the accessibility settings of the watch. While it's on, these gestures don't run their SandFly commands: by default, a two-finger tap pauses or resumes speech, and a two-finger double tap starts **Dictate** in a text field or plays or pauses media. Saved gesture assignments are kept and apply again when you turn the setting off.

### SandFly menu on watches

The SandFly menu scrolls on watches, so that you can reach every item. **Describe screen** is off in the watch menu by default, the item that turns off SandFly is named **Turn off**, and **Pause SandFly** isn't available on watches.

### Updates on watches

A watch updates to new builds of `sandfly-wear.apk`. The watch downloads the update over its own Wi-Fi when Wi-Fi is available, not through the phone, and shows the progress in a notification.
