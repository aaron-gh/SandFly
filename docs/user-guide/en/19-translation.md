## Translation {#translation}

SandFly can translate what it said last, the text of a text field, and, with automatic translation, all speech and notifications in other languages. Translations are made on the phone or by a translation service or AI service that you choose. Settings are in **Translation**.

### Translate the last spoken phrase {#translation-last-phrase}

**Translate last spoken phrase** translates what SandFly said last and reads the translation. With focus in a text field, it translates the text of the field instead, except in password fields.

The command is in the **Reading** category in **Commands** and has no gesture by default. Assign it to a gesture or a key there. See [](#commands).

1. Move focus to an item, or let SandFly read something.
2. Run **Translate last spoken phrase**.

SandFly reads the translation. When the text is already in the target language, SandFly says "Already in" and the language. When there is nothing to translate, SandFly says "Nothing to translate".

If you run the command again right after, SandFly translates the same original text again, not the translation, so a language you changed in between takes effect. A translation is kept until the text or the settings change, so running the command again doesn't send the text to the service again.

The translation is added to the speech history, so the commands that repeat, spell and copy the last spoken phrase work on it. Text over 5,000 characters is cut.

### Choose the translation service {#translation-service}

1. Go to **Translation** > **Translation service**.
2. Choose a service.

The services are:

- **On-device**: translates on the phone with downloaded languages. The default on phones. Not on watches. See [](#translation-on-device).
- **AI service**: the on-device AI model when **Use on-device AI** is on, otherwise the AI service in use in **AI services**, such as SandFly Pro, Gemini or Claude. The default on watches. See [](#descriptions-ai-services).
- **Google Cloud Translation**, **DeepL** and **Microsoft Translator**: need your API key from the service.
- **LibreTranslate**: needs the address of a LibreTranslate server, such as `https://libretranslate.com` or your own server, and an API key if the server needs one.

When you choose a service that needs an API key or an address, SandFly asks for it at once. The settings of the service are then in a category of their own on the **Translation** screen, such as **DeepL settings**:

- The API key item, such as **DeepL API key**: paste the key and choose **Save**. To remove it, clear the field and choose **Save**.
- **Address**, for LibreTranslate: the address of the server. It must start with `https://`.
- **Region**, for Microsoft Translator: the Azure region of your Translator resource, such as `westeurope`. Leave it empty for a global resource (**Global**).

A service that isn't ready shows "not set up" after its name. Text that you translate is sent to the service, except with **On-device**, or with **AI service** while on-device AI is on. API keys stay on the phone: exported settings don't include them.

### Choose the languages {#translation-languages}

- **Translate from**: **Detect language** (the default) or a language.
- **Translate into**: **Device language** (the default) or a language.
- **Translate text in the target language into**: a second language for text that is already in the target language, or **Don't translate** (the default). For example, an English speaker who reads German and English can choose German here. Shown only while **Translate from** is **Detect language**.
- **Swap languages**: swaps the languages of **Translate from** and **Translate into**, and SandFly says the new pair. Shown only while **Translate from** is a language.

With **On-device**, the lists show the languages that on-device translation supports. Language detection runs on the phone. The first time, SandFly may say "Language detection is downloading. Try again in a moment."

### On-device languages {#translation-on-device}

With **On-device** as the translation service or the automatic translation service, the **On-device settings** category has these items:

- **On-device languages**: lists the languages and which are downloaded. Choose a language that isn't downloaded to download it, about 30 MB, or a downloaded one to delete it.
- **Download all languages**: downloads every language that isn't downloaded, one at a time, after a dialog with their number and size. While they download, the item shows how many are done, and SandFly announces each quarter while the **Translation** screen is open. The downloads continue after you leave the screen. Choose the item again to stop them.
- **Download languages on Wi-Fi only** (off by default): languages download only on Wi-Fi.

A language that is needed and isn't downloaded is downloaded the first time it is needed, with English, through which other language pairs are translated. SandFly says which languages are downloading.

### Automatic translation {#translation-automatic}

The **Automatic translation** category has these settings:

- **Automatic translation** (off by default): speech in another language than **Translate into** is translated before SandFly reads it, notifications included.
- **Translate notifications** (off by default): translates only notifications, while **Automatic translation** is off.
- **Automatic translation service**: **On-device**, **Google Cloud Translation**, **DeepL**, **Microsoft Translator** or **LibreTranslate**. **AI service** isn't offered. The default is the translation service, or **On-device** when the translation service is **AI service**.
- **Languages not translated**: languages that SandFly reads as they are, such as languages you understand. None by default.

The last two settings are available while **Automatic translation** or **Translate notifications** is on.

To turn automatic translation on and off without opening settings, use the **Automatic translation** reading control. It is off by default; turn it on in **Reading controls**. See [](#commands).

#### How automatic translation works {#translation-automatic-details}

SandFly identifies the language of each utterance on the phone. Text that is in the target language, in one of **Languages not translated**, or, when **Translate from** is a language, in any other language, is read as it is and isn't sent to a service. Text with fewer than three letters isn't checked.

The sounds and vibrations of an utterance play at once, and the words follow when the translation is ready. A translation that takes longer than 3 seconds, or fails, is replaced by the original text. SandFly keeps recent translations, so moving back to an item doesn't translate it again.

With **On-device**, a language that isn't downloaded starts to download once SandFly is sure of it. While it downloads, speech is read untranslated, and when it has downloaded, SandFly reads the item you are on again, translated, unless you moved on. A notification waits for the download for up to 30 seconds.

Notifications are translated before they are queued, so they keep their place. A notification read with a voice profile keeps the voice of that profile. Private notifications aren't translated, so their text is never sent to a service. See [](#notifications).

### How translations are read {#translation-speech}

The **Speech** category has these settings:

- **Use a voice for the target language** (on by default): the translation is marked with its language, so SandFly changes the voice as for other text in that language, following the language settings in **Speech**. See [](#speech).
- **Say the original language** (off by default): SandFly says the language of the original text before the translation, such as "From German:".
- **Copy translations to the clipboard** (off by default): each translation is copied to the clipboard.

### Translation error messages {#translation-errors}

When a translation fails, SandFly says why, for example:

- The API key or the LibreTranslate address is missing: "Enter the DeepL API key in Translation settings."
- The service didn't accept the API key, or its usage limit is reached.
- The service can't be reached.
- The service doesn't translate the language.
- "Language not detected".
- Languages for on-device translation are still downloading, or couldn't be downloaded.

For other failures, SandFly says that the service couldn't translate the text, with the message of the service.
