## Speech {#speech}

The **Speech** screen in SandFly settings sets the speech engine, voice, rate, pitch and volume, voice profiles, languages, pronunciation, and what stops speech. It has the groups **Voice**, **Languages**, **Pronunciation**, **Stopping speech** and **Other apps**.

Where speech plays, the volume it plays at, audio ducking and **Low-latency audio** are on the **Audio output** screen. See [](#sound). What SandFly says about each item, such as usage hints, punctuation and emoji, is on the **Verbosity** screen. See [](#verbosity).

### Choose a speech engine {#speech-engine}

SandFly uses its own speech engine setting, separate from the Android default engine.

1. Go to **Speech** > **Text-to-speech engine**.
2. Choose an installed engine, or **System default** to use the engine set in Android.

**Speech** > **System text-to-speech settings** opens the Android text-to-speech settings, where you can install voices and set the system default engine. It isn't shown on TVs.

The **Speech engine** reading control switches between the installed engines when you swipe up or down. It changes the engine of **SandFly default**, so it isn't available while a voice profile is in use. It isn't in the reading controls by default. To add it, go to **Reading controls**. See [](#commands).

Some engines play their audio themselves, such as engines that pass text on to other engines. SandFly detects them and plays their speech the usual way instead of through **Low-latency audio**.

### Change the speech rate, pitch and volume {#speech-rate}

The sliders in the **Voice** group set the speech of **SandFly default**. They're available only while SandFly is on.

- **Speech rate**: from 10% to 600%. The default is 100%.
- **Speech pitch**: from 20% to 200%. The default is 100%.
- **Speech volume**: from 25 to 100. The default is 100.

**Speech rate step**, below **Speech rate**, sets how much the rate changes at a time with the **Speech rate** sliders of this screen and of voice profiles, the **Speech rate** reading control and the speech rate commands. **10% of the rate**, the default, changes the rate in use by a tenth of it. **1%**, **2%**, **5%**, **10%** and **20%** move to the next multiple of that step, such as from 133% to 135% with **5%**.

While a voice profile is in use, the **Voice profiles** item shows "In use:" and the name of the profile, and these sliders don't change how it sounds. Changing the rate or pitch with commands or reading controls then changes the profile in use. See [](#speech-voice-profiles).

You can also change speech without opening settings:

- The **Speech rate** reading control, which is in the reading controls by default: swipe up or down to change the rate.
- The **Increase speech rate**, **Decrease speech rate**, **Increase speech pitch** and **Decrease speech pitch** commands, for gestures, keyboard shortcuts and menus.
- The **Increase speech volume**, **Decrease speech volume** and **Reset speech rate and pitch** keyboard shortcuts. See [](#appendix-keyboard-shortcuts).

To choose whether speech plays at the accessibility, media, ring or call volume, see [](#sound-speech-volume). To change the speech volume with the volume keys, see [](#buttons-speech-volume).

### Repeat the last phrase slower {#speech-repeat-slower}

The **Repeat last spoken phrase slower** command, in the **Reading** group of commands, says the last spoken phrase again once, slower, in the same voice. The speech rate doesn't change, so the next item is spoken at the usual rate.

**Speech** > **Repeat slower by** sets how much slower: 5%, 10%, 15%, 20%, 25%, 30%, 40% or 50%. The default is 15%.

To use the command, assign it to a gesture or keyboard shortcut, or add it to a menu. See [](#commands).

### Voice profiles {#speech-voice-profiles}

A voice profile is a set of speech settings that you can switch to at once: a speech engine, language, voice, volume, rate, pitch, voice effects, and **Send long text a sentence at a time**. For example, one profile can use another engine for reading long text while **SandFly default** stays fast.

**SandFly default** is always in the list. It uses the settings on the **Speech** screen. While a profile is in use, all speech uses the engine and voice of the profile, so SandFly doesn't switch language or dialect, except for languages that have a voice of their own in **Voices by language**. See [](#speech-voices-by-language).

Voice profiles are also used for emoji and for languages: see [](#speech-emoji) and [](#speech-voices-by-language).

#### Add a voice profile {#speech-add-voice-profile}

1. Go to **Speech** > **Voice profiles**.
2. Choose **Add voice profile**.
3. Enter a name, or leave the field empty to use the suggested name, such as "Voice profile 1". Choose **OK**.

SandFly opens the settings of the new profile. A new profile starts with the speech volume, rate, pitch, voice effects and **Send long text a sentence at a time** of **SandFly default**.

#### Voice profile settings {#speech-voice-profile-settings}

To change a profile, go to **Speech** > **Voice profiles** and choose the profile.

- **Name**: renames the profile.
- **Text-to-speech engine**: the engine of the profile. An engine that is no longer installed is listed with "not installed".
- **Language**: **Default**, or one of the languages of the engine.
- **Voice**: **Default**, or one of the voices of the engine in the chosen language.
- **Speech volume**, **Speech rate** and **Speech pitch**: as on the **Speech** screen, for this profile.
- **Play sample**: speaks a sample sentence with the engine, voice, volume, rate, pitch and voice effects set here, in the language of the profile where SandFly has the sentence in it. "Sample not played" means that the engine didn't start or returned an error.
- **Voice effects**: the voice effects of this profile. See [](#voice-effects). Not on watches.
- **Send long text a sentence at a time**: as on the **Speech** screen, for this profile. See [](#speech-pronunciation).
- **Keep the engine started**: keeps the engine of this profile running while another engine is in use and the screen is on, so that switching to the profile doesn't wait for the engine to start. Each engine kept started runs as a process of its own. Off by default.
- **Move up** and **Move down**: on watches only. On phones, you move profiles in the list.
- **Delete voice profile**: deletes the profile after you confirm.

#### Switch voice profiles {#speech-switch-voice-profile}

- **Speech** > **Voice profiles** > **Voice profile in use**: choose **SandFly default** or a profile.
- The **Voice profile** reading control: swipe up or down to go through **SandFly default** and the profiles, in the order of the **Voice profiles** screen. It isn't in the reading controls by default. To add it, go to **Reading controls**.
- The **Choose voice profile**, **Next voice profile** and **Previous voice profile** commands, in the **Speech** group of commands.
- Once you have a profile, the **Voice profiles** group of commands has a **Use voice profile** command for **SandFly default** and for each profile, for gestures, keyboard shortcuts and menus.

You can also add a **Voice profile** item to the SandFly menu. See [](#commands).

#### Order, rename and delete voice profiles {#speech-manage-voice-profiles}

On the **Voice profiles** screen:

- To move a profile, touch and hold it and drag it up or down, or use its **Move up** and **Move down** actions. SandFly announces which profile it moved above or below.
- To rename or delete a profile, use its **Rename** and **Delete** actions, or touch and hold the profile to open a menu with them.

**Keep the engine of the previous voice profile started**, on the **Voice profiles** screen, keeps the engine of the profile used before the current one running while the screen is on, so that switching back doesn't wait for the engine to start. It's off by default.

### Emoji voice and rate {#speech-emoji}

Two settings in the **Voice** group change how emoji sound:

- **Emoji voice**: **Same as speech**, the default, or a voice profile to speak emoji with.
- **Emoji speech rate**: the rate of emoji as a percentage of the speech rate, from 50% to 300%. The default is 100%.

They apply whether the speech engine reads the emoji or SandFly replaces them with their names. Whether emoji are read, and how, is set in **Verbosity** > **Emoji**. See [](#verbosity-emoji).

### Command feedback and text editing voice {#speech-command-feedback}

**Speech** > **Voice for command feedback and text editing** sets the voice that says what a command or a text edit did. It covers the answers of commands, such as the time and date, the battery level, the status and the title of the window, and the result of copy, cut and paste. It doesn't cover voice commands, the questions and answers of the AI, or the speech of macros. **SandFly's voice**, the default, says them as always. A voice profile says them in that profile's voice instead. See [](#speech-voice-profiles).

Speech that is off or muted, the speech filter and script rewrites, the pronunciation dictionary, and the speech history apply as to any speech, so **Repeat last spoken phrase** repeats these texts too. A touch, a gesture or a key that stops SandFly's speech stops this voice too, and only what it says. While the profile is reading a notification, SandFly's own voice says the text, so that the notification isn't cut off.

**Read command feedback over SandFly's speech** is available while a voice profile is chosen. With it off, the feedback waits in SandFly's speech queue. With it on, it is said at once, over what SandFly is saying. It's off by default.

If the engine of the voice profile doesn't start within 2 seconds, SandFly's own voice says the text. With **Read command feedback over SandFly's speech** on, the text is dropped instead.

### Voice effects {#voice-effects}

**Speech** > **Voice effects** sets an 11-band equalizer and compression effects for **SandFly default**. **Voice effects** on the screen of a voice profile sets them for that profile. A new profile starts with the voice effects of **SandFly default**. Voice effects aren't available on watches.

The **Voice effects** screen has:

- **Use voice effects**: turns the equalizer and effects on or off without changing them. On by default.
- **Preset**: **Flat**, **Clarity**, **Bass boost**, **Bass cut**, **Treble boost**, **Treble cut**, **Warm**, **Reduce harshness**, **Reduce sibilance**, **Small speaker** or **Deep bass**, then your saved presets. A preset sets the bands and the effects. The bands start flat and the effects off. Changing a band or an effect sets **Preset** to **Custom**.
- **Save as preset**: saves the bands and effects as a preset with a name. Saved presets are listed for **SandFly default** and for every voice profile. You can save up to 50 presets.
- **Saved presets**: renames or deletes a saved preset. Deleting a preset doesn't change voice effects set to it.
- **Bass compression**, **Voice compression** and **Treble compression**: **Off**, **Low**, **Medium** or **High**. Each compresses speech in its range, below 200 Hz, 200 Hz to 4 kHz, or above 4 kHz, and raises its level, so that quiet parts of the range are louder. **Deep bass** uses **Bass compression** at **Medium**.
- **Equalizer**: a slider for each band: 64 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 3 kHz, 4 kHz, 6 kHz, 8 kHz and 16 kHz, each from -12 dB to +12 dB.

When a band boosts or an effect is on, a limiter keeps speech from clipping.

Voice effects apply to speech that SandFly plays through **Audio output** > **Low-latency audio**. With **Low-latency audio** off, they apply as an audio effect on Android 9 and later where the device supports it. Otherwise the **Voice effects** item reads "Needs Audio output > Low-latency audio". Engines that pass text on to other engines apply the effect only if they pass the audio session on as well. See [](#sound).

Reading controls: **Voice effects** turns the voice effects of the voice in use on or off, and each equalizer band has a reading control, such as **Equalizer 1 kHz**. Swipe up or down to change the band by 1 dB. They aren't in the reading controls by default. To add them, go to **Reading controls**. The **Use voice effects** command, in the **Speech** group of commands, turns voice effects on or off.

### Languages {#speech-languages}

When an app marks text as another language, SandFly switches the voice to that language. The **Languages** group sets how:

- **Switch language automatically**: switches to the voice of another language for text in that language. On by default.
- **Switch dialect automatically**: switches to another regional form of the language of the voice, such as British English for text marked as British English while the voice speaks US English. On by default.
- **Voices by language**: the languages you read, each with a voice. See [](#speech-voices-by-language).
- **Identify languages from words**: tells apart languages that are written in the same script by their words, for text that apps don't mark. Off by default.

With a change turned off, SandFly reads the text in the voice in use, without pauses around it.

With **Switch language automatically** on, SandFly also finds the language of text that apps don't mark from its script, such as Arabic or Greek letters in English text, and from letters that only one of your languages uses. **Identify languages from words** uses on-device language identification from Google Play services, for pieces of text of at least 12 letters.

The **Switch language automatically** and **Identify languages from words** commands, in the **Speech** group of commands, turn these settings on or off.

#### Voices by language {#speech-voices-by-language}

**Speech** > **Voices by language** lists the languages you add. Each language is spoken with **SandFly default**, using the voice of the engine for that language, or with a voice profile. Use a profile when the default engine has a voice for the language that you don't want to use.

To add a language:

1. Go to **Speech** > **Voices by language**.
2. Choose **Add language**. SandFly lists the languages of all installed engines.
3. Choose the language. SandFly adds it with **SandFly default**.
4. To use a voice profile, choose the language in the list, then choose the profile.

To remove a language, choose it and choose **Remove language**.

When two or more of the listed languages, or a listed language and the language of the voice, share a script, such as English and French in Latin script, the screen shows a list for that script, such as **Latin script**. It sets the language of text in that script when nothing else identifies it.

#### Change the spoken language {#speech-spoken-language}

The **Spoken language** reading control and the **Change spoken language** command change the language of SandFly default speech. While a voice profile is in use, the profile sets the language, and the reading control has no value. The reading control isn't in the reading controls by default.

### Pronunciation {#speech-pronunciation}

The **Pronunciation** group has:

- **Use pitch changes**: speaks deleted letters in a higher-pitched voice. On by default.
- **Speak letters phonetically when reviewing**: for letters you move over by character and spelled text.
- **Speak letters phonetically when typing**: for keyboard keys and letters you type or delete.
- **Send long text a sentence at a time**: for speech engines that are slow to stop, such as Gryphon, so that a swipe stops long text at once. Many engines pause longer between sentences sent separately, so leave it off if your engine stops quickly. Off by default.
- **Read line breaks as spaces**: reads text without the pauses at its line breaks. Off by default.
- **Speech dictionary** and **Symbol dictionary**: see [](#speech-dictionary).

The two phonetic letter settings each have four choices:

- **Off**: says only the letter.
- **After the letter, delayed, like "h, hotel"**: the default. When you move by character, the word follows after a short delay, and moving on before then skips it.
- **After the letter, no delay, like "h hotel"**.
- **Instead of the letter, like "hotel"**.

### Speech dictionary {#speech-dictionary}

**Speech** > **Speech dictionary** changes how text is spoken in all speech, such as a word said as another word, or a symbol said as a space. Entries apply in order, each to the text that the one before it left. SandFly settings screens are read as written.

To add an entry:

1. Go to **Speech** > **Speech dictionary**.
2. Choose **Add a replacement**.
3. In **Text**, enter the text to find. In **Say as**, enter what to say instead. Leave **Say as** empty to leave the text out.
4. Choose **Whole words only**, on by default, to match only whole words, and **Regular expression** to match a regular expression.
5. Choose **OK**.

Choose an entry for **Edit**, **Choose an app** and **Remove**. **Choose an app** limits the entry to one app, or **All apps**. An entry for one app applies while a window of that app is the last window shown.

**Import from file** and **Export to file** load and save the entries as a file. See [](#api-item-files).

The braille dictionary, for braille displays, is in braille settings. See [](#braille).

#### Symbol dictionary {#speech-symbol-dictionary}

**Speech** > **Symbol dictionary** replaces symbols and sequences of symbols, in speech and on a braille display. An entry matches its exact text anywhere, including a period directly after a word, and has no whole-word or regular expression option. Entries apply in order, after the speech dictionary or braille dictionary. You can add up to 500 entries.

Choose **Add a replacement**, enter the **Symbol** and what to **Replace with**, and choose **OK**. Entries have the same **Edit**, **Choose an app**, **Remove**, **Import from file** and **Export to file** options as the speech dictionary.

In speech, entries for punctuation that speech engines don't read aloud, such as a period or comma, don't apply while **Verbosity** > **Speak punctuation and symbols** leaves that punctuation out. With **None**, no entry for a single symbol applies in text, except where **Say symbols that change meaning** keeps the symbol. See [](#verbosity-text).

### Pause and resume speech {#speech-pause}

The **Pause or resume speech** command pauses speech and resumes it. By default, it's a two-finger tap. See [](#appendix-gestures).

When speech resumes:

- With **Audio output** > **Low-latency audio** on, speech stops mid-word and resumes from the same place. SandFly keeps up to 10 seconds of paused speech.
- Otherwise, with engines that report word positions, speech resumes from the word where it stopped.
- With engines that don't report word positions, such as RHVoice and Gryphon, SandFly estimates how far the engine got from how fast it spoke earlier items, and resumes from the start of that sentence or phrase. It can repeat a few words, but doesn't skip any.

SandFly doesn't play the sound and vibration of the item again when speech resumes.

The **Turn speech on or off** command turns all speech off and on.

On a physical keyboard, pressing Ctrl alone pauses speech. See [](#keyboards).

### Stop speech {#speech-stop}

The **Stopping speech** group sets what stops speech:

- **Stop speech when touching the screen**: on by default.
- **Interrupt speech when typing**: when a character typed on a physical keyboard stops speech. **Always**, the default, **In edit fields**, or **On keyboard echo**, only when the keyboard echo speaks the character or the word it ends. Not on watches.
- **Interrupt speech on Enter**: Enter on a physical keyboard stops speech. On by default. Not on watches.
- **Cover proximity sensor to stop speech**: covering the proximity sensor stops speech. Off by default. Not shown on TVs or on devices without a proximity sensor.
- **Quiet while recording**: while an app records without headphones connected, SandFly speaks only within 2 seconds of a touch, gesture, key or braille input, and is quiet for other speech, such as notifications, so that it isn't recorded. Off by default.

On a physical keyboard, every key except modifier keys, lock keys and the volume keys stops speech, and so do all shortcuts with Ctrl, Alt or Meta. Typed characters and Enter follow the settings above. Space outside an edit field always stops speech.

While another app records from the microphone and during calls, SandFly still speaks what you ask for with a gesture or key, such as the time, the battery level or the window title.

### Speech history {#speech-history}

The **Speech history** command, in the **Reading** group of commands, opens a list of the last 100 texts SandFly spoke, newest first.

- Choose a text for **Copy**, and for **Show all** when the list shortens the text.
- **Clear history**, at the end of the list, removes every text.

The history is kept in memory only, is never saved or logged, and is cleared when SandFly turns off. It doesn't include speech while a password field has focus, or speech while the list is open. Each text is kept to 2,000 characters.

To use the command, assign it to a gesture or keyboard shortcut, or add it to a menu. See [](#commands).

### Let other apps speak {#speech-other-apps}

Apps and scripts can send text to SandFly to speak in your SandFly voice and settings. **Speech** > **Let other apps speak** sets which apps can:

- **Let other apps speak**: turns speech from other apps on or off. Off by default.
- **Play a sound before other apps speak**: plays a sound before each text from another app, so that it can't be mistaken for SandFly speech. Off by default.
- **Apps**: each app that has connected, with a check box to allow it. An app that can speak while the device is locked also has a check box with its name and "while the device is locked". "No app has connected yet." means the list is empty.

The first time an app tries to speak, SandFly shows a notification, "Allow" and the name of the app "to speak through SandFly?", with **Allow** and **Don't allow**. Nothing from the app is spoken until you allow it. Nothing from other apps is spoken while SandFly is off or paused, and apps can't stop or replace SandFly speech.

The keyboard in use can speak without being in the list, and can turn off the SandFly keyboard echo of its own typing.

For developers, see [](#api-speech).

### Speech commands and reading controls {#speech-commands}

The **Speech** group of commands has, among others: **Pause or resume speech**, **Turn speech on or off**, **Increase speech rate**, **Decrease speech rate**, **Increase speech pitch**, **Decrease speech pitch**, **Change spoken language**, **Switch language automatically**, **Identify languages from words**, **Choose voice profile**, **Next voice profile**, **Previous voice profile** and **Use voice effects**. Keyboard shortcuts add **Increase speech volume**, **Decrease speech volume**, **Reset speech rate and pitch** and **Open text-to-speech settings**.

The **Reading** group has **Repeat last spoken phrase**, **Repeat last spoken phrase slower**, **Spell last spoken phrase** and **Speech history**.

The speech reading controls are **Speech rate**, **Spoken language**, **Speech engine**, **Voice profile**, **Voice effects** and the equalizer bands. Only **Speech rate** is in the reading controls by default. The **Speech volume** and **Audio output device** reading controls are described in [](#sound-reading-controls). The **Speech** group of **Reading controls** also has **Speak punctuation and symbols**, **Emoji** and **Speak text formatting** (see [](#verbosity-text)), **Automatic translation** (see [](#translation)) and **Speak only focus moved by the user** (see [](#verbosity-own-speech)).

To assign commands to gestures and keyboard shortcuts, and to add reading controls, see [](#commands).
