## Notifications {#notifications}

SandFly reads new notifications as they arrive, and can read the notifications in the shade on request or at a set time. **Notifications** in SandFly settings sets which notifications SandFly reads, what it says, in which voice, and when. It also turns on the SandFly notification list, which can take the place of the notification shade.

Incoming calls are read whatever the notification settings are. **Verbosity** > **Announce caller** sets whether the caller is said once, repeated while the call rings, or not said. See [](#verbosity-announcements).

### Notification access {#notifications-access}

Some features need notification access for SandFly, which Android settings give:

- Reading notifications that Android doesn't announce to screen readers. See [](#notifications-not-announced).
- Reading private notifications in full while the phone is locked.
- **Read what Do Not Disturb allows**. Without notification access, SandFly reads no notifications while Do Not Disturb is on with this choice.
- Stopping a notification when it is dismissed or removed.
- Summaries of the notifications in the shade.
- The SandFly notification list.
- Ending the call in progress of an app, such as WhatsApp, with **End call**. See [](#commands-calls).
- Repeating the caller of an app's call with **Verbosity** > **Announce caller**. See [](#verbosity-announcements).

To give notification access, select **Turn on notification access** on the **When to read**, **Stop reading** or **Summary and time** screen, and turn on SandFly in the list. The item is shown only while SandFly has no notification access. SandFly connects to notifications only while SandFly is on.

### Turn notification speech on or off {#notifications-speech-on-off}

**Notifications** > **Speak notifications** turns reading new notifications on or off. It's on by default. Incoming calls are read while it is off, as **Verbosity** > **Announce caller** sets.

You can also turn notification speech on or off with:

- The **Notification speech** quick settings tile.
- The **Notification speech on or off** command, which you can assign to a gesture or keyboard shortcut. SandFly says "Notification speech on" or "Notification speech off".
- An automation app. See [](#notifications-automation).

### Commands for notifications {#notifications-commands}

The **Notifications** category in **Commands** has:

- **Read notifications**: reads the notifications in the shade, as **Summary and time** sets. See [](#notifications-summary).
- **Read the last notification again**: says again the last notification that SandFly read. SandFly says "No notification was read" when there is none.
- **Stop reading the notification**: stops the notification being read, and those waiting to be read.
- **Notification speech on or off**
- **Time announcements on or off**: sets **Say the time every** to **Off**, or back to the interval it had before, or to 1 hour when none was set. SandFly says "Time announcements on" or "Time announcements off".

You can assign these to gestures, keyboard shortcuts and other inputs. See [](#commands).

A gesture, keyboard shortcut or key that you use while a notification is read stops it, and SandFly says what the gesture or key does.

### Change what is said {#notifications-reading}

**Notifications** > **Reading notifications** sets what SandFly says for a notification and how. Under **What is said**:

- **Speech format**: the text SandFly says for each notification, made of placeholders: `{app}`, `{title}`, `{text}`, `{content}` (the whole text), `{bigtext}`, `{subtext}`, `{ticker}`, `{category}` and `{time}`. The default is `{category}, {title}, {text}`.
- **Private notifications say**: what SandFly says in place of the content of a private notification, with the placeholders `{app}`, `{category}` and `{time}`. The default is `{app}`.

Under **Text**:

- **Leave out emoji**: emoji in notifications aren't read. Off by default.
- **Read capitals as words**: words in capital letters are read as words. Some speech engines spell them letter by letter. Off by default.
- **Read long numbers digit by digit**: numbers such as codes and phone numbers are read one digit at a time. Off by default. **Long numbers have at least** sets how many digits a number has to be read this way, from 3 to 10; the default is 5.
- **Links**: **Read the whole link**, the default, **Read the site only**, **Say the text below**, or **Leave out**. **Said in place of a link** is the text for **Say the text below**.
- **Skip notifications with no text**: notifications without text aren't read. Off by default.

Under **Length**, each is **Off** by default:

- **Most words**: reads up to 5, 10, 15, 20, 30, 50 or 100 words.
- **Most sentences**: reads up to 1, 2, 3 or 5 sentences.
- **Most characters**: reads up to 50, 100, 200, 300, 500 or 1000 characters.
- **Stop reading after**: stops a notification after 5 to 60 seconds.

Under **Timing**:

- **Wait before reading**: waits up to 10 seconds before reading a notification. **Off** by default.
- **When another notification is read**: what happens when a notification arrives while another is read: **Read after it**, the default, **Interrupt it**, or **Don't read the new one**.
- **Read again until the screen turns on**: says again, every 1 to 30 minutes, the notifications that arrived while the screen was off, until the screen turns on. **Off** by default.

For voice settings on this screen, see [](#notifications-voice).

### Notification voice {#notifications-voice}

Under **Notifications** > **Reading notifications** > **Voice**:

- **Voice**: reads notifications with one of your voice profiles, with all of its settings, such as another speech engine, rate and pitch. The default is **SandFly's voice**.
- **Voice profiles**: opens the voice profiles, where you make and change them. See [](#speech-voice-profiles).
- **Read over SandFly's speech in a different voice**: see below. Off by default.
- **Detect language**: says each notification in the language it is written in. It needs **Speech** > **Switch language automatically**. See [](#speech-languages). Off by default.
- **Sound before notifications**: plays a sound before each notification. Off by default.

Apps and rules can have a voice of their own. See [](#notifications-apps) and [](#notifications-rules).

How notifications in different voices are read:

- A notification in the voice profile that SandFly speaks with is read after the current speech, in the same speech queue.
- A notification in a different voice is also read after the current speech, so that two voices don't speak at once. It waits up to 45 seconds. With **Read over SandFly's speech in a different voice** on, it is read at once, over the current speech.
- **Read over SandFly's speech in a different voice** doesn't apply while **Speak only focus moved by the user** applies to the app in front, or while an assistant or a call plays audio. See [](#verbosity-own-speech).
- While **Quiet while recording**, in **Speech**, applies, no notification is read. See [](#speech).
- When notifications for different voices arrive together, each waits for the one before it.

If the engine of a voice profile fails, SandFly reads the notification in its own voice.

### Apps {#notifications-apps}

**Notifications** > **Apps** sets how the notifications of each app are read.

- **Other apps**: how SandFly reads the notifications of apps without settings of their own: **Read**, the default, **App name only**, or **Don't read**.
- **Apps**: the apps in the launcher, the apps with settings, and the apps in the history, by name. Each shows its settings.

For one app:

- **Notifications**: **As other apps**, the default, **Read**, **App name only**, **Read first** or **Don't read**. **App name only** never says what the notification says. **Read first** notifications interrupt a notification being read.
- **Spoken name**: the name SandFly says for the app.
- **Time between an app's notifications**: the shortest time between two notifications of the app that are read. **As other apps** uses the setting in **Repeats**.
- **Speech format**: a speech format for the app. See [](#notifications-reading).
- **Voice**: **As other apps**, **SandFly's voice**, or a voice profile, such as one with another speech engine.
- **Reset**: removes the settings of the app. Shown only for an app with settings.

### Words and replacements {#notifications-words}

**Notifications** > **Words and replacements** has four lists. Each item can be a regular expression.

- **Only read notifications with**: when the list has words, only notifications that contain one of them are read.
- **Don't read notifications with**: notifications that contain one of these words aren't read.
- **Read only the app name for**: for notifications that contain one of these words, only the name of the app is said.
- **Say differently**: text that SandFly says differently, such as "lol" as "laughing". Each replacement has **Text**, **Say as**, **Whole words only**, on by default, and **Regular expression**.

To add a word, select **Add a word** under its list, enter the text, and select **OK**. To add a replacement, select **Add a replacement**. Select an item to **Edit** or **Remove** it.

### Kinds of notifications {#notifications-kinds}

**Notifications** > **Kinds of notifications** leaves out kinds of notifications:

- **Skip media players**: on by default.
- **Skip ongoing notifications**: such as navigation and timers.
- **Skip running app notifications**: such as a VPN or a recorder.
- **Skip progress notifications**
- **Skip group summaries**: such as "3 new messages".
- **Skip low priority notifications**
- **Skip system notifications**

All except **Skip media players** are off by default.

### When to read {#notifications-when}

**Notifications** > **When to read** sets the conditions in which notifications are read:

- **While the screen is on**: on by default.
- **While the screen is off**: off by default.
- **While the phone is locked**: on by default.
- **Headphones**: **Read with or without**, the default, **Only with headphones**, or **Only without headphones**.
- **While media plays**: **Read**, the default, **Pause the media while reading**, or **Don't read**.
- **Do Not Disturb**: **Read none**, **Read what Do Not Disturb allows**, the default, or **Read all**. **Read what Do Not Disturb allows** needs notification access.
- **While the screen is touched**: **Read right away**, **Read when the touch ends**, the default, or **Don't read**. With **Read when the touch ends**, notifications that arrive while you touch the screen are read half a second after the touch ends, and at most 10 seconds after the first of them arrived.
- **During calls**: reads notifications during calls. Off by default.
- **Not in silent mode**: off by default.
- **Not in vibrate mode**: off by default.
- **Not in car mode**: such as with Android Auto, which reads notifications itself. Off by default.
- **Quiet hours**: no notifications are read between **Start** and **End**. Off by default; the default times are 22:00 and 07:00.

### Rules {#notifications-rules}

**Notifications** > **Rules** changes how notifications are read in some conditions. A rule has conditions and actions: when its conditions are met, its actions apply to the notification. A rule with no conditions applies to every notification. The voice of a rule takes the place of the voice of the app.

To add a rule:

1. Select **Add a rule**, enter a name, and select **OK**. Or select **Add a rule from a template** and choose a template.
2. Under **When**, select **Add a condition** and choose a condition.
3. Under **Then**, select **Add an action** and choose an action.

A rule has:

- **Name**
- **Turned on**
- **Conditions**: **All must be met**, or **Any one must be met**.
- **When**: the conditions. Select a condition to **Edit** it, **Turn around** to have it met when it isn't, such as "The screen is off" in place of "The screen is on", or **Remove** it.
- **Then**: the actions. Select an action to remove it.
- **Delete the rule**

The conditions are **Bluetooth device**, a device you choose or **Any device**, **Wired headphones**, **Wi-Fi network**, by name, **Time of day**, with **From**, **Until** and **Days**, **Screen on**, **Phone locked**, **Notification from an app**, **App in front**, **Notification contains**, a text or regular expression, **Battery below**, a percentage, **Charging**, **Landscape**, **Silent or vibrate mode**, and **Media playing**. **Bluetooth device** needs the Nearby devices permission on Android 12 and later, and **Wi-Fi network** needs the location permission; SandFly asks for them when you add the condition.

The actions are: don't read, read only the app name, read in full, even if private, use a speech format, read up to a number of words, read without length limits, keep emoji, keep numbers together, read first, read even in quiet hours and other times notifications are not read, and read with a voice profile.

The templates are **Only with headphones**, **Silent at night** (22:00 to 07:00), **Only with the screen off**, **App name only when locked** and **Short when not charging** (up to 15 words).

### Repeats {#notifications-repeats}

**Notifications** > **Repeats** handles notifications that come again and apps that post many:

- **Skip the same notification for**: a notification with the same text as one read within this time isn't read. The choices are **Off**, the default, from 10 seconds to 1 hour, and **Until SandFly restarts**.
- **Numbers count as the same**: notifications that differ only in numbers, such as the percentage of a download, count as the same. Off by default.
- **Time between an app's notifications**: the shortest time between two notifications of an app that are read, from 5 seconds to 15 minutes. **Off** by default. Each app can have its own. See [](#notifications-apps).
- **Leave out a repeated start**: leaves out a start that repeats in the notifications of an app, such as the name of a group in a chat. Off by default. **Remember the start for** sets how long SandFly keeps the start: 30 seconds to 15 minutes, 1 minute by default.

SandFly also reads a notification once when an app posts it again with the same title and text within 5 seconds, or posts the same text as a group summary.

### Stop reading {#notifications-stop}

**Notifications** > **Stop reading** sets what stops a notification being read:

- **Shake to stop**: shake the phone. **Shake strength**: **Gentle**, **Medium**, the default, or **Hard**.
- **Wave to stop**: move a hand over the proximity sensor. **Pocket mode**, on by default, keeps a sensor that is covered when reading starts, as in a pocket, from stopping it.
- **Press both volume keys**
- **Stop when dismissed**: stops the notification when you dismiss it. **Also when dismissed on a watch**, on by default, also stops it when you dismiss it on a connected watch.
- **Stop when the app removes it**

All except **Pocket mode** and **Also when dismissed on a watch** are off by default. **Stop when dismissed** and **Stop when the app removes it** need notification access. These settings also apply while a summary is read.

### Summary and time {#notifications-summary}

**Notifications** > **Summary and time** reads the notifications in the shade, and says the time at an interval. Under **Summary**:

- **Turn on notification access**: summaries need it. Shown only while SandFly has no notification access.
- **Read notifications now**: reads the notifications in the shade. It needs SandFly to be on.
- **Read notifications every day**: reads them each day at **Time**, 08:00 by default.
- **Newest first**: reads the newest notification first. Off by default.
- **Your name**: a name for the greeting at the start, such as "Good morning. Sam."
- **Pause between notifications**: from **Off**, the default, to 5 seconds.

A summary starts with a greeting and the number of notifications, such as "You have 5 notifications.", and reads each notification as app, title and text. Group summaries and ongoing notifications are left out. **Apps**, **Words and replacements**, **Kinds of notifications** and **Rules** apply, and the notifications that they leave out aren't counted.

Under **Time announcements**:

- **Say the time every**: **Off**, the default, 15 minutes, 30 minutes, 1 hour or 3 hours.
- **Turn on Alarms and reminders**: opens the Android setting that lets SandFly say the time exactly on time. Without it, Android can delay the announcements. Shown only while **Say the time every** isn't **Off** and the setting is off. SandFly also opens this Android setting when you choose an interval for **Say the time every** or turn on **Read notifications every day**, unless it is already on.
- **Time format**: what SandFly says, with the placeholders `{time}`, `{date}` and `{battery}`, the battery percentage. The default is "It's {time}".
- **Voice**: **Notification voice**, the default, **SandFly's voice**, or a voice profile.

Summaries are read in the notification voice.

To have the battery level said at an interval, see **Verbosity** > **Battery level** in [](#verbosity-announcements).

### History {#notifications-history}

**Notifications** > **History** lists the last 100 notifications that arrived since SandFly started, with the time each arrived, whether it was read, why not, such as "Not read: Do Not Disturb", and what SandFly said. The history is kept in memory only.

- **Keep a history**: on by default.
- **Clear the history**

Select a notification for these actions, shown with the name of its app, such as Chat:

- **Don't read Chat**: sets the app to **Don't read**.
- **Read only the name of Chat**: sets the app to **App name only**.
- **Read Chat first**: sets the app to **Read first**.
- **Don't read notifications like this**: adds a regular expression that matches notifications like it to **Don't read notifications with**.
- **Copy**: copies its title and text.

### Test the settings {#notifications-test}

**Notifications** > **Send a test notification** posts a SandFly test notification after 5 seconds, so that you can turn off the screen or open another app first. SandFly reads it with your notification settings. SandFly needs permission to show notifications.

### Automation {#notifications-automation}

Automation apps, such as Tasker or MacroDroid, can control notification speech with broadcasts to SandFly. Turn on **Notifications** > **Allow automation apps** first; it's off by default, as any app can send them. **Notifications** > **Automation** lists the actions, each with its broadcast action: the package name of SandFly followed by an action such as `.action.READ_NOTIFICATIONS`. Select an action to copy its broadcast action:

- **Turn notification speech on**
- **Turn notification speech off**
- **Turn notification speech on or off**
- **Stop the notification being read**
- **Read notifications**

### SandFly notification list {#notifications-list}

With **Notifications** > **Use SandFly's notification list** on, the **Notifications** command opens a SandFly list instead of the system notification shade. On some phones, such as Samsung phones with many notifications, the system shade is redrawn so often that moving through it is slow. It's off by default, and needs notification access.

In the list:

- Double-tap a notification to open it.
- A group is one item. Double-tap it to show its notifications.
- Each notification has its actions and, where the app offers it, a reply field with **Send**.
- Media notifications show **Playing** or **Paused**, with **Previous**, **Play** or **Pause**, and **Next**.
- A notification without text, such as a weather notification with a layout of its own, is read with the text of its layout, or else with its sub text, info text or summary text.
- Text longer than about 160 characters is cut. The **Show all** action reads the whole text, and **Show less** cuts it again.
- **Dismiss** removes a notification, and **Clear all** removes all that can be removed.
- **Quick settings** opens the quick settings.

While the phone is locked, the **Notifications** command opens the system shade, which hides what you chose to hide on the lock screen. The **System notification shade** command always opens the system shade. The SandFly list opens as a screen of its own, so the app in front pauses. Some apps end what they are doing when they pause: wireless debugging pairing in Settings fails when the pairing code is entered from the SandFly list. Use **System notification shade** for those.

Settings for the list:

- **List notifications newest first, without groups**: each notification is an item of its own, newest first, read as app, time, title and text. Off by default.
- **Leave media players out of the notification list**: leaves out media player notifications. The notification count read when the screen turns on leaves them out too. Off by default.
- **Notification details**: which parts of a notification the list reads, and in what order: **Title**, **Text**, **App name**, **Time** and **Playing or paused**. Each part is a check box, all checked by default, and has the actions **Move up** and **Move down**.

### Notifications Android doesn't announce {#notifications-not-announced}

Android tells screen readers about a new notification only when it alerts. A notification that Do Not Disturb holds back, or an update that an app posts without alerting again, such as a doorbell app replacing its last motion alert, isn't announced. When SandFly has notification access, it also receives these notifications, and reads them as the notification settings say, so **When to read** > **Do Not Disturb** applies to them too. Progress and ongoing notifications are left out.

While the phone is locked, Android gives screen readers only the public version of a private notification, which many apps don't have. With notification access, SandFly reads the whole notification instead, as the notification settings say, such as **Private notifications say** and **App name only when locked**.

### Other notification behavior {#notifications-other}

- Toasts, the short messages that apps show at the bottom of the screen, are read while Do Not Disturb is on. Do Not Disturb settings apply only to notifications.
- On the lock screen and in the notification shade, SandFly doesn't say "collapsed" for each notification. It says "expanded" when you expand a notification.
- On phones that show the pane of the notification shade when a heads-up notification arrives, such as Vivo phones, SandFly doesn't say the name of the pane within 3 seconds after a notification arrives, unless you touched the screen, pressed a key, used a braille display or gave a voice command after it.
- You can dismiss SandFly notices, such as those about an update or changed gestures, also on the lock screen. The notifications for pausing SandFly and for downloading an on-device model stay while those continue.
- On Samsung watches, SandFly reads a notification card as app and title, then the message, then the time, also where the label of the card leaves out the message. See [](#devices).
- To have notifications translated, see [](#translation).
