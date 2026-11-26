## Image and screen descriptions {#descriptions}

SandFly can describe the focused image, the whole screen and the view of the rear camera, and answer questions about them. Descriptions come from one of these:

- An AI service, such as Gemini, Claude or OpenAI, with your own API key. See [](#descriptions-ai-services).
- SandFly Pro, without an API key. See [](#pro).
- A model that runs on the phone, with nothing sent over the network. See [](#descriptions-on-device).

Text in images is recognized on the phone and needs none of these.

Settings for descriptions are on two screens: **Image and screen descriptions** turns descriptions on and opens Live recognition; **AI services** chooses what makes the descriptions. Descriptions need a phone or tablet: they aren't available on watches and TVs.

### Set up image and screen descriptions {#descriptions-set-up}

1. Set up an AI service, SandFly Pro or on-device AI. See [](#descriptions-set-up-service), [](#pro) or [](#descriptions-on-device-download).
2. Go to **Image and screen descriptions** and turn on **Detailed image descriptions**.
3. Read the dialog about what is sent and to whom, then choose **Turn on**.

If you run **Describe image** while **Detailed image descriptions** is off, SandFly shows the dialog **Get detailed image descriptions?**. **Turn on** turns the setting on and describes the image; **No thanks** leaves it off, and SandFly reads only the text in the image.

The first time you run **Describe screen**, SandFly shows the **Screen description** dialog about the screenshot that is sent. Choose **Turn on** to describe the screen.

### Image and screen descriptions settings {#descriptions-settings}

**Image and screen descriptions** has these settings:

- **Detailed image descriptions** (off by default): **Describe image** sends the focused image to the AI service in use, or to the on-device model, for a full description.
- **Automatic descriptions for text in images**: SandFly reads the text in an image when focus moves to it. Choose **All images**, **Unlabelled images only** (the default) or **Don't describe images**. Text is recognized on the phone.
- **Cyrillic text recognition**: downloads and deletes the text recognition models for languages written in Cyrillic, such as Russian, Ukrainian and Serbian. See [](#descriptions-cyrillic-text). Not on watches.
- **Live recognition**: opens the Live recognition screen. See [](#descriptions-live-recognition). Only on devices with a camera.

### Cyrillic text recognition {#descriptions-cyrillic-text}

Text recognition reads Cyrillic text with a model for the language, which you download first. SandFly uses the model of the speech language, or of the first phone language, that is written in Cyrillic. Without the model, only Latin letters are read, and SandFly says once that the model isn't downloaded.

1. Go to **Image and screen descriptions** > **Cyrillic text recognition**.
2. Choose the language. Its model, a few megabytes, is downloaded, with the model of Latin letters the first time, so that text in both is read. On mobile data, a confirmation dialog opens first.

To stop a download, choose the language again. To delete a model, choose a downloaded language, then **Delete**. The model of Latin letters is deleted with the last language.

The models come from the Tesseract OCR project and are read by Tesseract on the phone. Recognition is slower than for other scripts.

### Describe an image {#descriptions-describe-image}

1. Move focus to an image.
2. Run **Describe image**. It is in the SandFly menu, and you can assign it to a gesture or a key. See [](#descriptions-commands).

SandFly opens the **Image description** window with the description, and the text found in the image. In the window you can copy the description, rate it, and ask questions about the image. See [](#descriptions-questions).

When no detailed description can be made, SandFly reads the text in the image and says why, for example "Can't get a more detailed image description" with the reason, or "Connect to the internet for a more detailed image description."

To have the description read aloud without the window, run **Speak image description** instead.

### Describe the screen {#descriptions-describe-screen}

Run **Describe screen**. SandFly takes a screenshot, sends it with the items on the screen, and opens the **Screen description** window with a description of the screen. You can ask questions about the screen in the window. See [](#descriptions-questions).

To have the description read aloud without the window, run **Speak screen description** instead.

**Describe screen** is available only in phone languages that it supports. Screenshots of secure windows, such as some banking apps, can't be taken.

### Ask questions about an image or the screen {#descriptions-questions}

Questions go to the same AI service or on-device model as the description.

In the **Image description** or **Screen description** window:

- **Ask by voice** records a question with the speech recognizer chosen in **Typing** > **Dictation**, sends it and reads the answer. **Stop listening** ends the recording. See [](#typing).
- **Type a question** opens a text field. Type the question and choose **Ask**.

Each answer stays in the window, so you can ask follow-up questions about the same image or screen.

To ask without opening a description first, run **Ask about image** or **Ask about screen**:

1. Move focus to an image, for **Ask about image**.
2. Run the command. SandFly takes a screenshot of the image or the screen and starts recording.
3. Say the question. Recording stops after a pause in speech, or when you run the command again.

SandFly sends the question and reads the answer. When recording can't start, a text field opens instead: type the question and choose **Ask**. Until you have agreed to send screenshots to the AI service, these commands run **Describe image** or **Describe screen** instead, which show the dialog for that agreement.

### Commands for descriptions {#descriptions-commands}

These commands are in the **Image and screen descriptions** category in **Commands**, where you can assign them to gestures and keys and add them to the SandFly menu. See [](#commands).

- **Take picture and describe**: takes a picture with the rear camera and describes it. See [](#descriptions-take-picture).
- **Describe image**, **Speak image description** and **Ask about image**: describe the focused image in a window, describe it aloud, or answer a spoken question about it.
- **Describe screen**, **Speak screen description** and **Ask about screen**: the same for the whole screen.
- **Share image of focused item** and **Share screenshot**: take a screenshot of the focused item or the whole screen and open the Android share sheet with it, to send it to another app. Needs Android 11 or later. Secure windows are black in the screenshot.
- **Report AI response**: opens a problem report with the last text an AI model gave, such as a description or an answer. See [](#help).

With SandFly Pro, the category also has the screen recognition commands. See [](#pro-screen-recognition).

The **Live recognition** category has a command for each Live recognition mode, such as **Live recognition: Scenes**, and **Stop Live recognition**. See [](#descriptions-live-recognition).

For default gestures and keyboard shortcuts, see [](#appendix-gestures) and [](#appendix-keyboard-shortcuts).

#### AI actions reading control {#descriptions-ai-actions}

The **AI actions** reading control runs **Describe screen**, **Speak screen description**, **Ask about screen**, **Describe image**, **Speak image description**, **Ask about image**, **Share image of focused item** and **Share screenshot**, and with SandFly Pro, **Start or stop AI agent task** and **Type AI agent task**. It needs Android 11 or later.

1. Choose the **AI actions** reading control.
2. Swipe up or down to choose an action.
3. Double-tap to run it.

Double-tap runs the action that the reading control announced until focus moves. **AI actions to include**, in **Reading controls**, chooses which actions the reading control offers; all are included by default. Actions that aren't available are left out. For reading controls, see [](#commands).

### Take a picture and describe it {#descriptions-take-picture}

**Take picture and describe** opens a screen that takes one picture with the rear camera and describes it as **Describe image** does, with the same window for questions. It is available only on devices with a camera.

1. Run **Take picture and describe**. SandFly says "Hold the phone steady" and takes the picture.
2. Read the description, and ask questions about the picture if you need to.
3. To take another, choose **Take another picture**.

SandFly asks for the camera permission the first time.

### AI services {#descriptions-ai-services}

**AI services**, in the main SandFly settings, chooses what describes images and screens and answers questions about them. The same service is used by Live recognition, by translation with the **AI service** option and by other features that use AI.

The screen has these items:

- **AI service**: the service in use. It lists only the services that are set up, and **None**.
- A settings item for each service: **SandFly Pro settings**, **Gemini settings**, **Claude settings**, **OpenAI (ChatGPT) settings**, **Mistral settings**, **Groq settings**, **OpenRouter settings**, **Ollama Cloud settings**, **Qwen settings**, **Zhipu GLM settings**, **Baichuan settings**, **DeepSeek settings**, **Kimi settings**, **Hunyuan settings**, **SiliconFlow settings** and **Other service settings**. The item of the service in use says **In use**; the item of a service that isn't set up says **Not set up**. **SandFly Pro settings** is in builds with SandFly Pro and opens the SandFly Pro screen. See [](#pro).
- **On-device AI**: see [](#descriptions-on-device).
- **Keep recognition models loaded** and **Model download server**: see [](#descriptions-model-settings).

#### Set up a service {#descriptions-set-up-service}

1. Go to **AI services** and open the settings of a service, such as **Claude settings**.
2. For **Other service**, choose **Address** and enter the address of a service with an OpenAI-compatible chat API and image input. The address must start with `https://`.
3. Choose the API key item, such as **Claude API key**. Choose **Get a key** to open the website of the service and create a key, where the service offers one. Paste the key and choose **Save**.
4. Choose the model item, such as **Claude model**. SandFly gets the list of models for your key. Choose a model, or choose **Enter a model name** and type it. When the list can't be fetched, SandFly shows why and opens a field for the model name.

A service is set up once it has an address, an API key and a model. If no other service is in use, SandFly uses a service as soon as it is set up. To remove a key, clear the field and choose **Save**. When the service in use loses its key or model, SandFly uses the next service that is set up, or **None**.

API keys stay on the phone: exported settings don't include them. See [](#profiles).

#### Choose the AI service {#descriptions-choose-service}

1. Go to **AI services** > **AI service**.
2. Choose a service, or **None** to turn AI descriptions off.

When no service is set up, SandFly says "No AI service is set up. Set one up in its settings below."

The **AI service** reading control, off by default, changes the same setting. Swipe up or down to switch between the services that are set up; SandFly says the name of the service. **None** and SandFly Pro without a licence aren't in it. With fewer than two services set up, SandFly says "Only one AI service set up" or "No AI service set up" and the setting stays. Add it in **Reading controls**; see [](#commands). The **AI service: next value** and **AI service: previous value** commands, in **Setting values**, do the same without selecting the reading control.

#### Gemini settings {#descriptions-gemini}

**Gemini settings** has two items:

- **Gemini API key**: the key from Google AI Studio. **Get a key** opens the page that creates one.
- **Gemini model**: **Automatic** (the default) uses Gemini Flash, and Gemini Flash-Lite when Flash is busy. You can choose another model from the list Google gives for your key; SandFly asks Flash-Lite when that model is busy or takes too long.

On the free tier of the Gemini API, Google can keep what it receives and use it to improve its products.

#### What is sent {#descriptions-privacy}

Images, screenshots and questions that you send are sent to the service in use. Before the first image goes to a service, SandFly shows a dialog with the address of the service, such as api.anthropic.com; its own terms and privacy policy apply. With **Use on-device AI** on, nothing is sent.

### On-device AI {#descriptions-on-device}

On-device AI describes images and screens with a vision model on the phone, with no API key and nothing sent over the network after the download. It is slower than an AI service and uses more battery. It needs a phone with a 64-bit ARM processor and enough memory for the model, and isn't available on watches.

While **Use on-device AI** is on, **Describe image**, **Describe screen**, questions, Live recognition and translation with the **AI service** option use the model on the phone instead of the AI service. **Detailed image descriptions** must also be on.

#### Download a model {#descriptions-on-device-download}

1. Go to **AI services** > **On-device AI**.
2. Choose **Model**, then choose a model. The list shows the size of each model, the memory it needs and a short note, and marks models that are **downloaded** or **experimental**. It lists only the models that the phone has enough memory for, and the ones already downloaded.
3. Choose **Download model**, which shows the size of the download. On mobile data, SandFly asks first.
4. When the download finishes, turn on **Use on-device AI**.

**Gemma 4 E2B** is the recommended model, and the default on phones with enough memory for it. On other phones the default is the largest model that fits. Experimental models may not work well.

While the model downloads, the item shows how far it is and the time left, and SandFly announces each quarter of the download and "Model downloaded" at the end. A notification shows the progress. Tap the item to stop; the download continues where it stopped when you start it again. SandFly checks the downloaded file and deletes it if it doesn't match the model.

To use a file that you downloaded yourself, choose **Use a model file from storage** and pick a Gemma 4 `.litertlm` file. SandFly says "Model installed" or that the file isn't a supported model.

#### On-device AI settings {#descriptions-on-device-settings}

- **Use on-device AI** (off by default): describe with the model on the phone. Available once a model is downloaded.
- **Model**: the model to use.
- **Download model**: downloads the chosen model. Shown until it is downloaded.
- **Manage models**: lists downloaded models and unfinished downloads, with the storage they use. Choose one and **Delete** to free its storage.
- **Use a model file from storage**: installs a model file from storage.
- **Time limit for answers**: how long SandFly waits for an answer, from 1 to 10 minutes. The default is 3 minutes. **Describe screen** takes longer than **Describe image**.
- **Shorter screen descriptions** (off by default): **Describe screen** asks for fewer details, so the answer comes sooner.
- **Processor**: **CPU**, **GPU** or **NPU**. Shown only on phones where the model can run on the GPU or the NPU. With **NPU**, SandFly uses the model built for that NPU, and **Model** can't be changed.

While the model loads and works, SandFly plays a sound that repeats. You can turn these sounds off in **Sound and vibration** > **Individual sounds and vibrations**. See [](#sound).

#### Memory {#descriptions-on-device-memory}

The model loads on the first request and runs in its own process. Before it loads a model, SandFly checks that the phone has enough free memory and, if not, says "Not enough free memory" with the name of the model. If Android stops the model for lack of memory, SandFly says so. In both cases, close some apps or choose a smaller model.

**Ignore on-device AI memory limits**, in **Developer settings**, lists every model, lets you choose any downloaded one and loads it without checking for free memory.

### Model settings {#descriptions-model-settings}

These two settings are in **AI services** and in **Typing** > **Dictation**, and apply to on-device AI models and to the speech recognition models of dictation:

- **Keep recognition models loaded**: how long models stay in memory after use: **Unload after use** (the default), **5 minutes**, **15 minutes**, **30 minutes** or **Until SandFly stops**. The on-device AI model always stays loaded for at least 2 minutes after a request. With **Until SandFly stops**, the on-device AI model loads when SandFly starts.
- **Model download server**: **Hugging Face** (the default) or **hf-mirror.com**, for places where Hugging Face can't be reached.

### Live recognition {#descriptions-live-recognition}

The **Live recognition** screen describes what the rear camera sees. Open it from **Image and screen descriptions** > **Live recognition**, or with the **Live recognition** reading control or commands. It isn't available on devices without a camera, on watches or on TVs.

Each mode turns on and off by itself, with a check box on the screen:

- **Scenes**: describes what the camera sees.
- **Text**: reads text in the camera view. Text is recognized on the phone.
- **Point and Speak**: reads the text at your fingertip in the camera view. Choose **Read above the fingertip** (the default) or **Read below the fingertip**.
- **People detection**: describes people in the camera view.
- **Door detection**: describes doors in the camera view.
- **Color detection**: names the colors in the centre of the camera view, such as "Dark blue", "Light gray" or "Red and white", or says "Mixed colors" when no color covers enough of it. A color is spoken when it changes and has held for about half a second. Light and dark depend on the light and on the automatic exposure of the camera. Colors are recognized on the phone.
- **Light level**: reads the light level from the light sensor: **Dark**, **Dim light**, **Normal light**, **Bright light** or **Very bright light**. It doesn't use the camera.
- **Light level tone**: plays a continuous tone whose pitch rises with the brightness of the rear camera view, from 200 Hz in the dark to 1600 Hz in sunlight. The tone plays at the volume of SandFly sounds, and stops when the mode is turned off or the screen is left.

The status line says **Camera on** or **Camera off**. SandFly asks for the camera permission the first time a mode needs it.

Descriptions can be wrong. Don't rely on them to move around safely. People and doors are described from the image only, without distances.

#### Use Live recognition {#descriptions-live-recognition-use}

1. Choose the **Live recognition** reading control. It needs Android 11 or later.
2. Swipe up or down to choose a mode. SandFly says the mode and whether it is on.
3. Double-tap to turn the mode on or off. The Live recognition screen opens if it isn't open.

While the screen is open:

- Two-finger tap: stops speech and reads the next result.
- Two-finger scrub: turns off every mode and closes the screen.

Results are read with the SandFly voice, also while other audio plays. The microphone isn't used. Leaving the screen, locking the phone, pausing SandFly or turning off every mode turns off the camera.

#### What describes the camera view {#descriptions-live-recognition-ai}

**Scenes**, **Point and Speak**, **People detection** and **Door detection** use the same as **Describe image**:

- With **Use on-device AI** on, the model on the phone. Nothing leaves the phone.
- Otherwise the AI service in use, such as SandFly Pro or Claude, which gets a camera image every few seconds.
- With Gemini as the AI service, the Gemini Live API, which gets a camera image every few seconds. SandFly reads the text of the answer.

Before the first camera image goes to a service, SandFly shows the dialog **Send camera images to Gemini?**, or for other services **Send camera images to** and the address of the service, such as api.anthropic.com. Choose **Send images**. A rejected API key, a used-up usage limit or repeated failures turn off the modes that use AI, and SandFly says so.

### Error messages {#descriptions-errors}

When a description or answer fails, SandFly says what failed, for example:

- The service didn't accept the API key. Check the key in **AI services**.
- The usage limit of the service or the API key has been reached.
- The service is busy, or took too long to answer.
- There is no internet connection.
- "No AI service is in use. Set one up or choose one in AI services settings."

For other errors, SandFly says the error code and the first sentence of the message from the service, in English.
