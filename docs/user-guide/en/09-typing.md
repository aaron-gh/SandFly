## Type and edit text {#typing}

SandFly reads the on-screen keyboard as you explore it, types the key under your finger, and reads what you type. You can also dictate text, write it by hand on the screen, or type in braille. This chapter describes the **Typing** settings, typing on the on-screen keyboard, moving the cursor, selecting and copying text, the clipboard history, granular editing, dictation and the handwriting keyboard.

The braille keyboard is described in [](#braille), and physical keyboards and the SandFly keyboard input method in [](#keyboards).

### Typing settings {#typing-settings}

To open the typing settings, go to **Typing** in SandFly settings. The screen has:

- **Keyboard echo**: what SandFly reads as you type with each kind of keyboard. See [](#typing-keyboard-echo).
- **Double-tap moves cursor to start or end**. See [](#typing-double-tap-cursor).
- **On-screen keyboard**: **Typing method**, **Swipe from key to key with every typing method**, **Touch and hold delay when typing**, **Hold keys down after the touch and hold delay**, **Typing focus delay** and **Announce keyboard keys faster**. See [](#typing-on-screen-keyboard).
- **Physical keyboard**: shown only while a physical keyboard is connected. See [](#typing-physical-keyboard).
- **Other ways to type**: **Dictation**, **Handwriting keyboard** and **Wait after writing**. See [](#typing-dictation) and [](#typing-handwriting).

### Type on the on-screen keyboard {#typing-on-screen-keyboard}

Touch the keyboard and slide your finger over the keys. SandFly reads each key under your finger. How a key is typed depends on the typing method.

#### Choose a typing method {#typing-method}

1. Go to **Typing** > **Typing method**.
2. Choose one of the methods:
   - **Hold finger to select any key, then double-tap**: lift your finger, then double-tap anywhere to type the key that SandFly read last.
   - **Hold finger to select letter key, then lift. Double-tap for other keys.**: lifting your finger types a letter key. Other keys, such as Delete or Enter, need a double-tap. This is the default.
   - **Hold finger to select any key, then lift**: lifting your finger types any key. This method has the lowest delay.
   - **Hold finger to select any key, then lift. Double-tap for Enter, Done, or Send.**: lifting your finger types any key except the key that sends or submits, such as Enter, Done, Send, Search or Go, which needs a double-tap. SandFly recognizes this key in Gboard. In other keyboards, every key types when you lift your finger.
   - **Tap any key to type it, or hold finger to select any key, then lift**: a quick tap on a key types it at once, so quick double letters type twice. Holding a finger on the keyboard explores the keys, and lifting types the key under the finger. This method is available on Android 13 and later.

With any method, you can also use split-tap: hold one finger on a key, and tap the screen with another finger to type it.

On Android 13 and later, choosing one of the last three methods sets **Typing focus delay** to 0.15 seconds. See [](#typing-focus-delay).

#### Touch and hold a key {#typing-touch-and-hold}

**Typing** > **Touch and hold delay when typing** sets how long you keep your finger on a key before SandFly long-presses it, for example to open the alternative characters of a key. The choices are 0.5, 1, 2, 3 and 5 seconds; the default is 3 seconds. The setting isn't available with the typing method **Hold finger to select any key, then double-tap**.

With **Typing** > **Hold keys down after the touch and hold delay** on, SandFly presses the key down at the end of the delay and passes the rest of the touch to the keyboard, as a double-tap and hold does. The keyboard then handles the touch as a long press of its own, such as a Delete key that repeats while held. This applies with the lift-to-type methods, on all keyboards except Gboard and Pixelboard: their keys are long-pressed, which opens the popup of alternative characters. It's off by default.

#### Alternative characters in Gboard {#typing-gboard-alternatives}

When you hold a key in Gboard, SandFly long-presses it after the touch and hold delay, and Gboard opens a popup of alternative characters. SandFly reads the selected character, such as "Comma", as soon as the popup opens. Slide your finger to the other characters to hear them, and lift your finger to type the one you're on.

Gboard 18.8 and later receive touches through Android touch exploration, as other keyboards do. With earlier versions of Gboard, and of Pixelboard, which is built from Gboard, SandFly explores touches on the keyboard by itself on Android 11 and later. While the popup of these versions is open, your finger selects one of its characters wherever it is, the nearest one when your finger is outside the popup, and lifting types it and closes the popup. When your finger leaves the keyboard with no popup open, touch exploration takes over for the rest of the screen, and lifting your finger there types nothing. This doesn't apply with the typing method **Hold finger to select any key, then double-tap**.

#### Gboard key vibration {#typing-gboard-vibration}

Gboard 18.8 and later play the key feedback set in Gboard settings, such as **Vibrate on keypress**.

Gboard and Pixelboard versions before 18.8 play key feedback only when a key receives hover events, which a touch that SandFly explores by itself doesn't send. When Shizuku or root is set up for SandFly, SandFly sends these hover events to the keyboard, so that it plays the feedback set in its own settings. Without Shizuku or root, these versions play no key feedback while SandFly explores the keyboard. There is no setting for this. To set up Shizuku or root, see [](#updates).

#### Change the typing focus delay {#typing-focus-delay}

**Typing** > **Typing focus delay** sets how soon SandFly reads a key after you touch the keyboard: 0.15, 0.2, 0.25 or 0.3 seconds. The default is 0.3 seconds. A shorter delay reads keys sooner. The setting is available on Android 13 and later.

You can also change the delay while you type with the **Typing focus delay** reading control, in the **Touch** group of **Reading controls**. See [](#commands).

#### Announce keyboard keys faster {#typing-faster-keys}

**Typing** > **Announce keyboard keys faster** prepares the speech of the keyboard keys in advance, to reduce the delay before a key is read. It's off by default, and is available only when Google Text-to-Speech is the speech engine.

#### Swipes and the keyboard {#typing-swipes-keyboard}

Swipes from an app, the status bar or the navigation bar never move focus into the keyboard. With any typing method except **Hold finger to select any key, then double-tap**, a swipe while focus is on a key goes on in the app, from the text field that you are typing in. With the double-tap method, swipes move through the keys. With **Typing** > **Swipe from key to key with every typing method** on, swipes move through the keys with every typing method. It is off by default. Swipes from inside the keyboard can still leave it.

### Keyboard echo {#typing-keyboard-echo}

Keyboard echo sets what SandFly reads as you type. Each kind of keyboard has its own setting.

1. Go to **Typing** > **Keyboard echo**.
2. Choose a keyboard: **On-screen keyboard**, **Physical keyboard**, **Braille keyboard**, **Braille display** or **Handwriting keyboard**.
3. Choose **None**, **Characters**, **Words** or **Characters and words**. The default for each is **Characters and words**.

Word echo reads the whole word as it is in the field, back to the last space. Words with apostrophes or hyphens, such as "don't", are one word, and a web or email address is one word. When a keyboard adds a word, its punctuation and a space at once, as the braille keyboard does in contracted braille, SandFly reads the word.

Each echo setting is also a reading control, a SandFly menu item and a command in the **Speech** group of commands: **On-screen keyboard echo**, **Physical keyboard echo**, **Braille keyboard echo**, **Braille display echo** and **Handwriting keyboard echo**. The reading controls and menu items are off by default. To add them, go to **Reading controls** or **SandFly menu** and turn them on in the **Keyboard echo** group. See [](#commands).

The **Cycle typing echo** keyboard shortcut changes **Physical keyboard** to the next choice. See [](#appendix-keyboard-shortcuts).

### Move the cursor {#typing-move-cursor}

When focus is on a text field, the reading controls **Characters**, **Words**, **Lines**, **Sentences** and **Paragraphs** move the cursor through the text, and SandFly reads the text it passes. With the default gestures:

1. Swipe up then down, or down then up, to choose a reading control. You can also swipe up or down with three fingers.
2. Swipe down to move the cursor forward, or swipe up to move it back.

The commands **Move cursor to beginning** and **Move cursor to end** in the **Text editing** group of **Commands** move the cursor to the start or end of the text. They are also in the actions of a text field. See [](#typing-editing-actions).

For the default gestures, see [](#appendix-gestures). For reading controls, see [](#commands).

#### Double-tap to move the cursor to the start or end {#typing-double-tap-cursor}

With **Typing** > **Double-tap moves cursor to start or end** on, a double-tap on the text field that you are typing in moves the cursor:

- When the cursor isn't at the start, a double-tap moves it to the start of the text, and SandFly announces "Beginning of field".
- When the cursor is at the start, a double-tap moves it to the end, and SandFly announces "End of field".

The **Activate** command does the same when it runs from another gesture, a voice command, a braille command or a script.

In any other control, or in a text field without input focus and a shown keyboard, a double-tap activates the item as usual. The setting is off by default, and is available on Android 13 and later.

### Select, copy, cut and paste {#typing-select-copy-paste}

#### Select text {#typing-select-text}

1. Move the cursor to where the selection starts.
2. Run **Start or end selection mode**. SandFly announces "Selection mode on".
3. Move the cursor with a reading control, such as **Characters** or **Words**. The text that the cursor passes is selected.
4. Run **Start or end selection mode** again to end selection mode.

In the NVDA keymap, **Start or end selection mode** is SandFly key + F9. In the SandFly keymap, it is SandFly key + Shift + S.

**Select all** selects all the text in the field, and **Clear selection** removes the selection.

Selection mode also works in text that isn't in a text field, such as a message or a web page, and across items. SandFly keeps this selection itself: only the text that the cursor passes is selected, and moving to another item starts another range there. **Copy** then copies all the ranges, one range to a line.

#### Copy, cut and paste {#typing-copy-cut-paste}

- **Copy** copies the selected text, or the text of the focused item.
- **Add to clipboard** adds the selected text, or the text of the focused item, to the end of the text on the clipboard, on a new line.
- **Cut** cuts the selected text in a text field.
- **Paste** pastes the clipboard into the text field at the cursor.

By default, a three-finger double-tap and hold runs **Cut**, and a three-finger triple-tap and hold runs **Paste**. You can give any of these commands a gesture. See [](#appendix-gestures) and [](#commands).

#### Editing actions and the editing menu {#typing-editing-actions}

When focus is on a text field, the **Actions** reading control and the **Actions** item of the SandFly menu include editing actions: **Move cursor to beginning**, **Move cursor to end**, **Start or end selection mode**, **Cut**, **Copy**, **Paste**, **Select all** and **Granular editing**. **Granular editing** isn't offered in password fields.

The **Editing menu** command opens a menu of **Clipboard history**, **Copy**, **Add to clipboard**, **Cut**, **Paste**, **Clear text**, **Select all** and **Start or end selection mode**.

#### Other text editing commands {#typing-editing-commands}

The **Text editing** group of **Commands** also has:

- **Submit text**: presses the key that sends or submits the text in the keyboard, such as Send, Search or Done.
- **New line**: does what Enter does. In a text field of several lines, it types a new line at the cursor and SandFly says "New line". In a text field of one line, it performs the action of the field, such as Search or Next, or else does what **Submit text** does. By voice, also "new paragraph" or "press enter".
- **Clear text**: deletes all the text in the text field.
- **Character count**: reads the length of the text, such as "12 characters, 3 words". In a password field, only the characters are counted.
- **Read text field**: reads the whole text of the text field, or "Empty" and the hint when it has no text.
- **Spelling / grammar suggestions**: opens the suggestions for the typo at the cursor. The **Proofread** reading control moves between typos.
- **Dictate**: see [](#typing-dictate).
- **Start voice editing**: records voice commands for the text field one after another. See [](#voice-commands-editing).
- **Switch keyboard**: changes to the next keyboard. Available on Android 11 and later.
- **Search emoji**, **Choose a symbol**, **Turn editing mode on or off**, **Spell the password field**, **Next word suggestion**, **Previous word suggestion** and **Type word suggestion**: commands of the SandFly keyboard. See [](#keyboards).

None of these commands has a gesture by default. To give one a gesture, see [](#commands).

### Clipboard history {#typing-clipboard-history}

SandFly keeps a history of up to 50 texts copied, newest first. Each text is kept up to 200,000 characters, and the history up to 1,000,000 characters in all: older texts past that are removed. **Add to clipboard** also stops at 200,000 characters. The history holds the texts that SandFly copies, and the texts that other apps copy while a window of SandFly has focus. Android doesn't let apps read the clipboard in the background, so with Shizuku or root, the switch **Add copies from other apps** at the top of the history adds every text that other apps copy. The history is kept apart from the settings and isn't included in a settings backup.

To open the history, run **Clipboard history**, from the **Text editing** commands or the **Editing menu**. The history opens over the current screen.

- Double-tap a text to paste it into the text field that the history was opened from. When the history was opened without a text field, double-tap opens the actions of the text.
- Double-tap and hold a text to open its actions: **Paste**, **Copy** it again, **Add to clipboard**, **Edit** its text, **Export to file**, or **Remove** it. The actions are also in the **Actions** of each text.
- **Select** shows the texts as check boxes. Double-tap texts to check them, then choose **Remove** or **Export to file** for the checked texts.
- **Export to file** saves the whole history as a text file. Several texts are separated by a line of three hyphens between blank lines.
- **Clear history** removes all the texts.

To paste without opening the history, add the **Clipboard history** reading control, in the **Actions for current item** group of **Reading controls**. In a text field, swipe up or down to go through the texts in the history, and double-tap to paste the text that SandFly read last. The reading control is off by default. See [](#commands).

### Copy text from the screen {#typing-copy-from-screen}

These commands are in the **Text editing** group of **Commands**:

- **Copy last spoken phrase**: copies the last text that SandFly read. By default, a three-finger triple-tap runs it.
- **Select last spoken phrase word by word**: opens a list of the words of the last phrase, to check the words to copy.
- **Copy text from screen**: opens a list of the text on the screen, split into addresses and numbers, characters, words or paragraphs, to check the parts to copy. See [](#reading-copy-text).
- **Continuous copy**: moves focus from the focused item through every item after it, without speech, scrolling lists and pages on the way, and collects the text of each item. Touch the screen, press a key or run another command to stop. At the end, the texts are shown with a check box each; check the ones to copy and choose **Save**. Continuous copy stops after 2,000 items or 10 minutes.

In these lists, the actions **Start selection** and **End selection** of an item check every item between two items. Double-tapping and holding an item does the same.

### Granular editing {#typing-granular-editing}

Granular editing opens the text of the focused text field, or of any focused item with text, in a screen of its own, split into pieces. To open it, choose **Granular editing** in the actions of a text field, or run the **Granular editing** command from the **Text editing** group of **Commands**. Password fields can't be opened.

- **Split by** chooses the pieces: **Characters**, **Words**, **Sentences**, **Lines** or **Paragraphs**. The default is **Words**, and SandFly keeps your last choice.
- Double-tap a piece to edit it. Leaving it empty deletes it.
- The actions of a piece are **Select**, **Insert before**, **Insert after** and **Delete**. Double-tap and hold a piece to open them in a dialog. **Start selection** and **End selection** select every piece between two pieces.
- Spaces, tabs and line breaks between pieces are pieces of their own, such as "2 spaces" or "New line", and a blank line is read as "Blank line".
- **Copy** copies the selected pieces, or all the text when no piece is selected.
- **More** has **Undo**, **Redo**, **Find**, **Find next**, **Find previous**, **Replace**, **Add text at end**, **Select all**, **Clear selection**, **Cut selected** and **Delete selected**. **Replace** replaces all matches, ignoring case.
- **Save** writes the text back to the text field and closes the screen. If the text field is gone, the text is copied instead.
- Back opens a dialog to save or discard the changes.

Text that isn't in a text field, such as a message, can be edited and copied but not saved.

### Dictation {#typing-dictation}

#### Dictate into a text field {#typing-dictate}

The **Dictate** command types what you say into the focused text field, at the cursor.

1. Move focus to a text field.
2. Run **Dictate**. A chime plays, and recording starts. Speech is silenced while SandFly records.
3. Speak. Recording goes on through pauses.
4. Run **Dictate** again. A second chime plays, and SandFly types the text.

Recording stops by itself after 15 minutes, or after a time of silence that you set. See [](#typing-dictation-settings).

By default, a two-finger double-tap runs **Dictate, play or pause media**. It starts **Dictate** when a text field has input focus, a keyboard is shown or connected, and no call is active; it stops a dictation that is recording; otherwise it plays or pauses media. For the default gestures, see [](#appendix-gestures).

On phones with a mute key, such as the Zinwa Q25, hold the mute key in a text field to dictate: recording starts when you press the key and stops when you release it. The setting is **Volume keys** > **Hold mute key to dictate in text fields**, on by default. See [](#buttons-mute-key).

#### Choose how speech is recognized {#typing-speech-recognition}

Go to **Typing** > **Dictation** > **Speech recognition**, and choose one of the following:

- **Automatic**: the default. SandFly uses Google on-device speech recognition when it is installed and has the language of the phone; otherwise Rambler, when there is a Gemini API key and Rambler supports the language; otherwise Whisper, when a model is downloaded; otherwise Android speech recognition. The screen shows which one is in use, such as "Automatic: Rambler".
- **Google on-device speech recognition**: the recognizer of the Speech Recognition & Synthesis app, with its offline language packs, in the language of the phone. On Android 13 and later, it adds punctuation and capital letters while **Automatic punctuation** is on. When the language of the phone has no language pack, the row **Download** (language) **for on-device recognition** opens the download screen of Speech Recognition & Synthesis.
- **Rambler (Gemini API)**: the Gemini 3.5 Transcribe model, through the Gemini API key set in AI services. It removes filler words, resolves self-corrections, and formats punctuation, numbers and lists. See [](#descriptions) for the Gemini API key.
- **Whisper**: recognition on the phone, with a downloaded Whisper model. See [](#typing-whisper-models).
- **Android speech recognition**: the speech recognition service chosen in Android settings.

A choice that can't be used falls back to the same order as **Automatic**. Google on-device and Android speech recognition start again after each pause until you stop dictation. If Rambler doesn't respond, the recording is transcribed with Whisper when a model is downloaded.

**Dictate** recognizes the language of the keyboard, such as the language chosen in Gboard, when it differs from the language of the phone. Google on-device speech recognition then needs a language pack for the language of the keyboard; without one, the next way of recognizing speech in the **Automatic** order is used. Voice commands and other voice input use the language of the phone.

With **Rambler live transcription** on, SandFly streams the recording while you speak, and receives the text of each phrase when you pause, so the text is typed sooner after you stop. If the stream fails, SandFly sends the whole recording when you stop. It's on by default, and shown only when there is a Gemini API key.

Note: The recording is sent to Google with Rambler. With a free-tier Gemini API key, Google may use the audio and text to improve its products; with a paid-tier key, it doesn't.

#### Whisper models {#typing-whisper-models}

Whisper recognizes speech on the phone, so the recording stays on the phone. **Typing** > **Dictation** > **Whisper models** lists the models to download:

- **Whisper Tiny** (32 MB), **Whisper Base** (60 MB), **Whisper Small** (190 MB) and **Whisper Large v3 Turbo** (574 MB): multilingual models. A larger model is more accurate and slower.
- **FUTO Tiny**, **FUTO Base** and **FUTO Small**, and English-only versions of each: models from FUTO.

To use a model:

1. Choose a model. SandFly downloads it, after a confirmation on mobile data.
2. To change to another downloaded model, choose it, then choose **Use**. To remove a model, choose it, then choose **Delete**.

The first model that you download is used. Whisper recognizes the language described in [](#typing-speech-recognition).

**Model file** adds a Whisper model of your own in the whisper.cpp format, such as one trained for a language, from the files on the phone, up to 4 GB. Once added, you can use it, replace it with **Choose another file**, or delete it. A file that isn't a whisper.cpp model is refused.

**Processor**, shown only on phones with an Adreno GPU and an OpenCL driver, chooses whether Whisper runs on the **CPU** or the **GPU**. If Whisper can't run on the GPU, it runs on the CPU.

#### Correct dictation with AI {#typing-dictation-correction}

With **Typing** > **Dictation** > **Correct dictation with AI** on, SandFly sends the text from Google on-device, Whisper or Android speech recognition to an AI service before typing it, with up to 300 characters of the text before the cursor. The service removes filler words, stutters and false starts, resolves self-corrections ("two, no, three" becomes "three"), adds punctuation and capital letters, corrects misheard words, and formats numbers, dates and lists. It's on by default. Text from Rambler isn't sent again, and dictation into a password field isn't sent.

- **AI service for dictation**: **Same as AI services** (the default), any AI service that is set up, or **On-device AI** when its model is downloaded. With On-device AI, all of dictation stays on the phone. See [](#descriptions).
- **Correction prompt**: the instructions that the AI service receives, such as a language or style. The field starts with the built-in prompt. Saving it unchanged or empty keeps the built-in prompt.

If the service doesn't respond within 20 seconds (60 seconds for On-device AI), or returns text much longer than the speech, SandFly types the text as recognized. Running **Dictate** before the service responds does the same at once.

#### Other dictation settings {#typing-dictation-settings}

In **Typing** > **Dictation**:

- **Automatic punctuation**: Google speech recognition adds punctuation and capital letters. On by default. Shown on Android 13 or later with Google speech recognition installed.
- **Block offensive words**: Google speech recognition masks offensive words. On by default. Shown with **Automatic punctuation**.
- **Auto stop after silence**: stops recording after 1, 2, 3, 5, 10, 15 or 30 seconds, or 1 minute, without speech. The default is **Off**.
- **Speak dictated text**: reads the text after SandFly types it, except in password fields. On by default.
- **Keep recognition models loaded**: how long a Whisper model and the On-device AI model stay in memory after use: **Unload after use** (the default), 5, 15 or 30 minutes, or **Until SandFly stops**. A loaded model starts sooner and uses memory.
- **Model download server**: where models are downloaded from, **Hugging Face** (the default) or **hf-mirror.com**.

### Handwriting keyboard {#typing-handwriting}

SandFly handwriting is a keyboard that types what you write on the screen with one finger. It is available on phones and tablets with Android 11 or later. The keyboard fills the screen, and while SandFly is on, touches on it go straight to it instead of through touch exploration.

#### Turn on the handwriting keyboard {#typing-handwriting-turn-on}

1. Go to **Typing** > **Handwriting keyboard**. Android keyboard settings open.
2. Turn on **SandFly handwriting**.
3. In a text field, choose **SandFly handwriting** as the keyboard, such as with the **Switch keyboard** command or the keyboard switcher of Android.

The first time the keyboard opens, a tutorial goes through writing a letter, a word and each gesture, and goes on when you do each step. Nothing is typed into the field during the tutorial. To end it early, swipe down with three fingers. To open it again, tap with three fingers.

Handwriting is recognized on the phone by Google ML Kit, in the language of the phone. The first time, SandFly shows a dialog to download the recognition model, about 20 MB, and announces when it has downloaded.

#### Write with the handwriting keyboard {#typing-handwriting-write}

Write a letter, several letters or a whole word anywhere on the screen. Strokes that follow each other within the wait time are one piece of writing, which is then typed and read. Dots, such as on an i, count as strokes.

**Typing** > **Wait after writing** sets the wait, from 0.2 to 1 second; the default is 0.6 seconds. A shorter wait types sooner, but each stroke of a letter with several strokes, such as H, must start within it. The setting also applies to the commands **Write the letter of a reading control** and **Write an app's name to open it**. See [](#commands).

| Gesture | Action |
| --- | --- |
| Two-finger swipe right | Type a space |
| Two-finger swipe left | Delete the character before the cursor |
| Two-finger swipe down | New line, or the action of the field, such as Send, Search or Done |
| Two-finger swipe up | Replace the last writing with the next reading of the same length, such as "h" for "H" |
| Two-finger tap | Read the text before the cursor |
| Three-finger swipe down | Hide the keyboard |
| Three-finger swipe right or left | Change to the next or previous keyboard (Android 9 and later) |
| Three-finger tap | Open the tutorial |

**Typing** > **Keyboard echo** > **Handwriting keyboard** sets what the keyboard reads as you type: each piece of writing, each word when a space or new line ends it, both, or nothing. Deleted characters and the readings that replace the last writing are read whatever the echo. In text fields of SandFly, the text before the cursor isn't read, and a deletion is read only as "Deleted".

While the handwriting keyboard is shown, only commands that open no window run, such as navigation and reading controls.

### Physical keyboards {#typing-physical-keyboard}

While a physical keyboard is connected, the **Physical keyboard** group of **Typing** has **Letter keys press keypad buttons**, **Enter sends messages**, **SandFly keyboard**, **Held keys type**, **Double-press characters**, **Double space types a period**, **Auto-correct**, **Word suggestions**, **Speak suggestion**, **Enter types the spoken suggestion**, **Word list language**, **Delete learned words**, **Speak password characters** and **Android physical keyboard settings**. These settings are described in [](#keyboards).
