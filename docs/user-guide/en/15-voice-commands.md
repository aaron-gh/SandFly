## Voice commands {#voice-commands}

Voice commands run SandFly commands from speech: moving focus, reading, editing text, opening apps, activating items on the screen, calling a contact, sending a message, or describing the screen. One voice command can hold several commands one after another. **Voice commands** in SandFly settings turns on AI for commands that SandFly doesn't understand, sets the time limit of voice editing, holds the voice commands that you make, and lists the phrases that SandFly doesn't understand. The **Voice commands** screen isn't shown on watches.

For the full list of phrases, see [](#appendix-voice-commands).

### Start a voice command {#voice-commands-start}

1. Use the **Start voice command** command. In the SandFly gesture layout, swipe right then up. See [](#appendix-gestures).
2. Say the command.

SandFly shows **Use voice commands to control SandFly** with examples of what to say. Choose **Use voice commands** to start recording. To stop the dialog from showing again, uncheck **Always show this message**.

Voice commands need the microphone permission. SandFly records with the speech recognizer chosen in **Typing** > **Dictation**. See [](#typing).

Voice commands aren't available while the screen is locked or during device setup. While the screen is locked, SandFly says so and names the gesture that opens the SandFly menu.

To list the commands, say "Help" or "What can I say". SandFly opens **Voice command overview**.

The **Start voice command** command is also in the **System actions** category, so a keyboard shortcut or a menu item can run it. To add it to the SandFly menu, see [](#commands-edit-menu).

### What you can say {#voice-commands-phrases}

SandFly reads what you say in this order:

1. A custom voice command, by its phrase. See [](#voice-commands-custom).
2. The name of any command, as SandFly shows it on the **Commands** screen, such as "Read from top", "Describe image" or "Next heading". The names of your macros, custom menus and script commands count too. The name of an app, such as "Gmail", opens the app, unless a command has that name. See [](#commands-list).
3. A phrase pattern, such as "open Chrome", "tap Settings", "call Sam" or "type hello". See [](#voice-commands-sandfly).
4. One of the original voice commands, such as "Find save", "Words navigation", "Speak faster" or "Copy". See [](#appendix-voice-commands).
5. With **Understand voice commands with AI** on, the AI service. See [](#voice-commands-ai).

Case and the punctuation that the speech recognizer adds don't matter. Words such as "please", "can you", "could you", "I want to" and "now" at the start, and "please", "for me" and "now" at the end, are left out.

When SandFly doesn't understand a command, it says "Can't recognize this command" and how to get the list of commands. The third time it doesn't understand the same phrase, it also says "Added to Phrases not understood". See [](#voice-commands-not-understood).

#### Apps, items and text {#voice-commands-sandfly}

- **Open an app**: "open", "launch", "start" or "switch to", and the name of the app. SandFly picks the app with that whole name, else one whose name starts with it, else one with a word of it in its name. When no app matches, or several match equally, SandFly runs the command with that name, such as "open notifications"; without one, the voice command isn't understood.
- **Activate an item on the screen**: "tap", "double tap", "click", "press", "activate" or "select", and the text of the item. SandFly activates the first item whose whole text is that text, else the first item whose text contains it. When there is none, SandFly says "Not found" and the text. "Press enter" runs **New line** instead. "Select" and a text that the focused text field has selects that text instead. See [](#voice-commands-text).
- **Type text**: "type", "write" or "enter", and the text. SandFly types it in the focused edit field and says the typed text, except in a password field.
- **Run a command**: "run" or "do" and the name of a command, or "run the" and the name of a macro and "macro", such as "run the morning macro".
- **Wait**: "wait", "wait a second", or "wait" and a number of seconds, up to 60. Use it between commands in a chain.

#### Text in a text field {#voice-commands-text}

These phrases act on the text of the text field with input focus.

- **Move the cursor**: "next", "previous", "right", "left", "forward" or "back", a number, and "characters", "words", "sentences", "lines" or "paragraphs", such as "next 3 words", "move left five words" or "go back two lines". "Move", "move cursor" or "go" can come before. SandFly moves the cursor and says the text that it passed over.
- **Start and end**: "start of", "beginning of" or "end of", and "word", "sentence", "line", "paragraph" or "text", such as "end of line" or "go to the start of the sentence". "To start" and "to end" are the start and end of the text. SandFly moves the cursor there and says the text that it passed over. When the cursor is at the start or end already, it moves to the start of the one before or the end of the one after.
- **Select, copy, cut or delete**: "select", "copy", "cut" or "delete", and one of the phrases above, such as "delete next five words", "copy previous 2 sentences", "select the next line", "delete last word" or "delete to end of line". SandFly says "Selected text" and the selected text, "copied" and the copied text, "cut" and the cut text, or the deleted text and "deleted". Copying leaves the cursor where it is.
- **Select, copy, cut or delete a text**: "select", "copy", "cut" or "delete", and a text in the field, such as "select crocodile". SandFly finds the text nearest to the cursor, ignoring case and the punctuation between its words, and acts on it. When the field doesn't have the text, SandFly says "Not found" and the text.

The number is 1 to 20 in words, or digits; without one, the phrase acts on one, as in "select previous word". The word, sentence, line or paragraph that the cursor is in counts as the first one, from the cursor. Words are counted without punctuation. A line ends at a line break. Deleting whole words, sentences, lines or paragraphs also deletes the space or line break that they leave. With nothing left to move over, SandFly says "Beginning of field" or "End of field".

The names of commands come first: "Next word" and "Previous character" run those commands. See [](#voice-commands-phrases).

When no text field has input focus, "select" and a text activates the item with that text, and the other phrases are read as the original voice commands. "Select" and a text also activates the item when the focused text field is a password field or doesn't have the text. For the other phrases, in a password field, SandFly says "Not available in password fields".

#### Calls and messages {#voice-commands-calls}

- **Call**: "call", "phone", "ring" or "dial", and a contact name or a phone number. SandFly says "Calling" and the name and starts the call. The first time, SandFly asks for the permission to make phone calls; say the command again after you allow it. Without the permission, the phone app opens with the number.
- **Send a message**: such as "message Sam saying I'm on my way", "text Sam I'm on my way", "send a message to Sam on WhatsApp saying hello" or "tell Sam on Signal hello". Without the text, SandFly says "Message to" and the name, and records the text.

For contact names, SandFly needs the permission to read contacts, and asks for it the first time. When several contacts have the name, SandFly says "More than one contact named" and their full names; say the full name. When no contact has it, SandFly says "No contact named" and the name.

Messages go to the default SMS app unless you name an app. With SMS or WhatsApp, SandFly opens the conversation with the text, says "Send to", the name and the text, and records the answer. Say "yes", "send", "OK" or "sure", and SandFly presses **Send** and says "Send pressed". Any other answer, or none, leaves the message unsent in the app, and SandFly says "Message not sent". Other apps open their share screen with the text, where you choose the conversation.

#### Descriptions {#voice-commands-describe}

- **Describe the screen**: "describe the screen" or "what's on the screen".
- **Describe the focused image**: "describe this image", "describe the photo" or "what's in this picture".
- **Copy**: "copy it", "copy that" or "copy the description" copies the description from the command before, or else the last phrase that SandFly spoke.
- **Send**: "send it to Sam", "share it with Sam", or "send it to Sam on WhatsApp" sends the description from the command before as a message, or else the last phrase that SandFly spoke. "Send the image description to Sam" and "send the screen description to Sam" describe and send in one command; SandFly says "Send to" and the name before it presses **Send**.

These commands use the AI services or on-device descriptions that **Image and screen descriptions** and **AI services** set up. See [](#descriptions).

### Chain commands {#voice-commands-chains}

A voice command can hold several commands, joined by "then", "and then", "after that" or "afterwards", such as "open Messages then call Sam". "And" joins commands too, when each part is a command: "go home and open Chrome" is two commands, while "tap Salt and pepper" is one. SandFly runs the commands one after another, with a short pause after each command that changes the screen.

When a command fails, such as an item that isn't found, SandFly says why and stops. A part that isn't a command is read as one of the original voice commands; when it isn't one either, SandFly says "Not understood" and the part.

To stop a voice command while it runs, start another voice command and say "stop", "cancel", "quit" or "never mind". Starting another voice command also stops the one running.

### Voice editing {#voice-commands-editing}

**Start voice editing**, in the **Text editing** category, records voice commands for the focused text field one after another, without a gesture for each command. It has no gesture by default. See [](#commands-list).

1. Move focus to a text field.
2. Run **Start voice editing**. SandFly says "Voice editing on", and a chime plays when recording starts.
3. Say a command. SandFly runs it, speaks the result, and records again once its speech ends. The chime plays again when recording starts after a command.

Voice editing runs only these commands:

- The commands of the **Text editing** and **Navigate by text** categories, by name, such as "Select all", "Copy", "Paste", "Clear text", "Move cursor to end", "Next word" or "Previous character". **Dictate** and **Start voice editing** don't run. See [](#typing-editing-commands).
- "Type", "write" or "enter", and a text, which SandFly types at the cursor and says.
- "New line", "new paragraph" or "press enter", which run **New line**.
- The phrases for text in a text field, such as "delete next five words", "end of line" or "select crocodile". Here, "select" and a text always acts on the text of the field. See [](#voice-commands-text).
- The text editing phrases of the original voice commands, such as "Select", "Finish selection", "Delete", "Cut" or "Spell TEXT". Here they are matched as the whole command. See [](#appendix-voice-commands-text-editing).
- Custom voice commands that run one of these commands or a macro. See [](#voice-commands-custom).
- Chains of these commands, such as "select all then cut". See [](#voice-commands-chains).

Anything else isn't run: SandFly says "Not understood" and what you said, and records again. Text that isn't a command isn't typed; say "type" before it.

Voice editing ends, and SandFly says "Voice editing off", when:

- You run **Start voice editing** again.
- You say "stop editing", "stop voice editing", "end editing", "end voice editing", "voice editing off", "start voice editing", "stop", "cancel", "quit" or "never mind".
- Focus leaves the text field.
- **Voice commands** > **Voice editing time limit** passes since voice editing started: 5, 10, 20 or 30 minutes, 10 by default.
- Speech recognition fails three times in a row, or the microphone permission is missing.

Speech is silenced while SandFly records, so that it isn't recorded. When nothing is said, recording starts again without a chime. Voice editing records with the speech recognizer chosen in **Typing** > **Dictation**, without AI correction. **Dictate**, **Dictate, play or pause media** and **Start voice command** end voice editing without "Voice editing off".

### Understand voice commands with AI {#voice-commands-ai}

**Voice commands** > **Understand voice commands with AI** sends a voice command that SandFly doesn't understand, or a chain that isn't completely understood, to the AI service in use, which turns it into commands. SandFly sends what you said, the names of the commands and the texts of the items on the screen. Off by default.

The switch needs an AI service set up in **AI services**: on-device AI, SandFly Pro, or another service with a key. See [](#descriptions) and [](#pro). The AI answer can run commands, activate items, open apps, type text, wait, make calls, send messages, describe the screen or an image, copy, or speak a short answer.

### Custom voice commands {#voice-commands-custom}

A custom voice command is a phrase that runs a command. Custom voice commands come before every other voice command.

1. In SandFly settings, choose **Voice commands**.
2. Under **Custom voice commands**, choose **Add voice command**.
3. Enter the phrase, up to 200 characters.
4. Choose a category, then the command.

Choose a custom voice command for **Change phrase**, **Change command** and **Delete**. Two custom voice commands can't have the same phrase.

**Export to file** saves the custom voice commands to a file, and **Import from file** adds them from one. See [](#api-item-files).

A custom voice command can run any command, including a macro or a custom menu. To run several steps from one phrase, make a macro and give the macro a phrase. See [](#macros).

### Phrases not understood {#voice-commands-not-understood}

SandFly counts the voice commands that it doesn't understand. A phrase that isn't understood three times is listed under **Voice commands** > **Phrases not understood**, and SandFly says "Added to Phrases not understood".

To make a listed phrase a custom voice command:

1. In SandFly settings, choose **Voice commands**.
2. Under **Phrases not understood**, choose the phrase.
3. Choose **Add voice command**.
4. Choose a category, then the command.

The phrase leaves the list. **Delete** removes a phrase from the list without a voice command.

Phrases are counted as custom voice commands are matched: case, punctuation, and words such as "please" at the start or end don't matter. Only a whole voice command counts, not a part of a chain. Not counted:

- Voice commands said while a password field has input focus.
- Phrases that start with "type", "write" or "enter".
- Phrases longer than 200 characters.
- Commands said during voice editing.

A count is forgotten 7 days after the phrase was last not understood, and a listed phrase after 30 days. SandFly keeps up to 50 phrases. The phrases stay on the device and aren't in settings files or settings profiles.

**Save phrases not understood** turns the counting on and off. On by default. Turning it off deletes the counts and the list.

### Languages {#voice-commands-languages}

Voice commands use the language of the device. The phrases of the original voice commands, the phrase patterns and the joining words are translated with SandFly, and command names are read in the language that SandFly shows them in. The phrases in [](#appendix-voice-commands) are the English ones.
