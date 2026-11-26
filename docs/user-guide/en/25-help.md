## Help and feedback {#help}

**Help & feedback** in the SandFly settings has:

- **User guide**: this guide. See [](#getting-started-user-guide).
- **SandFly tutorial**: the interactive tutorial. See [](#help-tutorial).
- **Practice gestures**: the gesture practice lesson of the tutorial. See [](#help-tutorial).
- **Keyboard practice**: turns on keyboard practice. SandFly says each key pressed and its command, and runs nothing. Available while SandFly is on; not shown on watches. See [](#keyboards-practice).
- **Gesture trainer**: exercises that measure gestures against the limits SandFly uses. Not shown on watches and TVs. See [](#gestures-trainer).
- **Send feedback**: problem reports and suggestions to the SandFly developer, and the replies. See [](#help-feedback).
- **Version history**: the release notes of every SandFly release. See [](#help-version-history).
- **Privacy policy**: the SandFly privacy policy. See [](#help-version-history).

The settings for developers and for finding problems are in **Developer settings**. See [](#help-developer).

### Tutorial and gesture practice {#help-tutorial}

**Help & feedback** > **SandFly tutorial** opens the SandFly tutorial at the lesson you were on. On a device with a touch screen and a physical keyboard, it first offers **Touch tutorial** and **Keyboard tutorial**. For the lessons and how the tutorial works, see [](#getting-started-tutorial).

**Help & feedback** > **Practice gestures** opens **Gesture practice**, where each gesture says its name and the command assigned to it without running the command. To leave it, move to **End practice** and activate it.

The **Open tutorial** and **Practice gestures** commands open the same lessons. **Practice gestures** isn't shown on watches and TVs, which have tutorials of their own. On a TV without a tutorial, **SandFly tutorial** is named **Help**. See [](#devices).

### Send feedback {#help-feedback}

**Send feedback** sends a problem report or a suggestion to the SandFly developer, without an account. The developer can reply, and the replies come to SandFly. To open it, go to **Help & feedback** > **Send feedback**, or choose **Send feedback** in the SandFly menu.

The **Send feedback** screen has:

- **New feedback**: a new report.
- **Inbox**: the feedback you have sent, with the status of each report and the replies.
- **Drafts**: reports that you started and haven't sent.

#### Send a report {#help-feedback-send}

1. Go to **Help & feedback** > **Send feedback**.
2. Choose **New feedback**.
3. Fill in the form:
   - **Type of feedback**: **Incorrect behavior**, **Crash**, **Slow or unresponsive**, or **Suggestion**. The default is **Incorrect behavior**.
   - **Area**: the part of SandFly, such as **Speech**, **Gestures**, **Braille display** or **Updates**.
   - **Title**: a short summary. Required.
   - **App where it happens**: the name of the app, if the problem happens in one app.
   - **Description**: what happens and what you expected. Required.
   - **Steps to reproduce**: what to do to make the problem happen. Not shown for suggestions.
   - **Email for replies**: optional. Replies come to SandFly whether or not you enter an address.
   - **Include device information**: on by default. See [](#help-feedback-contents).
   - **Include SandFly log**: off by default. See [](#help-feedback-contents).
4. Choose **Send**.

SandFly says "Feedback sent." and the report moves to **Inbox**. If the report can't be sent, SandFly says why, such as "Feedback not sent. Check the internet connection.", and the report stays a draft.

What you type is kept as a draft until you send it. **Delete draft** deletes it. The email address and the two **Include** switches are kept for the next report.

#### What a report includes {#help-feedback-contents}

A report always includes the fields you filled in and the version and build number of SandFly.

**Include device information** adds the Android version and the manufacturer and model of the device. The switch shows the information that it adds.

**Include SandFly log** adds the recent log of SandFly, which helps the developer find the cause of a problem. The log can include text from the screen. It includes:

- The log of SandFly from the time you opened **Send feedback**, so that the problem you want to report is still in it. In split screen, the log is read when you choose **Send**, so that you can make the problem happen in the other app first.
- The last gestures that SandFly recognized.
- The last crash and the last time SandFly turned off, and times that SandFly stopped responding for a moment.
- The accessibility services that are on, and the last volume key and Enter key presses.
- The last announcements, notifications, toasts and window names that apps sent, with the app that sent each and the start of its text.
- Where focus moved after Back, the time taken to check the **Show in Actions only when** conditions of macros, what plays and records audio, and typing from the Gboard popup of alternative characters.
- The logs of the other SandFly processes, such as on-device AI.

To report a problem soon after it happens, open **Send feedback** before you do anything else, so that the log still has it.

#### Replies and status {#help-feedback-inbox}

**Inbox** lists the reports you sent, with the status of each and the number of replies you haven't read. The status is one of:

- **Open**.
- **Closed, completed**.
- **Closed, not planned**.
- **Closed, duplicate**.
- **Removed**: the report is no longer on the report server.

Choose a report to open it. It shows **Status**, **Your feedback** and **Messages**, the replies from the developer and your own, each with who sent it and when. To reply:

1. Choose **Your reply** and type the reply.
2. Choose **Send reply**.

While SandFly is on, it checks for replies and status changes every hour, and each time you open **Send feedback**. A reply or a status change shows a notification with sound, which opens the report. Closed reports are still checked for 30 days. Notifications for replies are in the **Feedback replies** notification category of SandFly.

**Remove from inbox** removes a report from the device and stops the checks. The developer keeps the report.

#### Report an AI response {#help-feedback-ai-response}

The **Report AI response** command opens a new report with the last text that an AI model gave, such as an image or screen description, a live recognition result or the result of an agent task. Use it to report a response that is wrong or offensive. SandFly keeps only the last response, until SandFly stops. If there has been no AI response, SandFly says so. Assign the command to a gesture or key in **Commands**. See [](#commands) and [](#descriptions).

### Version history and privacy policy {#help-version-history}

**Version history** opens the [SandFly releases page](https://github.com/aaron-gh/SandFly/releases) in the browser. It has the release notes of each release. **Privacy policy** opens the [SandFly privacy policy](https://sandfly.dev/privacy). On a watch without a browser, SandFly shows the address instead.

### Developer settings {#help-developer}

**Developer settings** has settings for finding problems and for development. Some of them write the content of the screen to the system log, which other apps with log access can read. Leave them off unless you need them.

- **Version code**: the version of SandFly and its version code. On Android 11 and earlier, the version is shown under the title of the screen instead.
- **Diagnosis mode**: off by default. A four-finger tap writes the screen contents to the system log. While the mode is on, **Display speech output**, **Echo recognized speech**, **Enable node tree debugging** and **Enable debug log overlay** work as if they are on, and they and **Log output level** can't be changed. SandFly shows a confirmation dialog.
- **Display speech output**: off by default. Shows the text that SandFly speaks on the screen.
- **Handle gestures in SandFly**: on by default. SandFly recognizes gestures itself instead of leaving it to Android. Many SandFly gesture features need it. Turn SandFly off and on again after you change it. Not shown on TVs.
- **Ignore on-device AI memory limits**: off by default. Lists every on-device AI model, also models that need more memory than the device has, and loads a model without checking for free memory. See [](#descriptions).
- **Echo recognized speech**: off by default. SandFly speaks the text that it recognized from a voice command. See [](#voice-commands).
- **Enable performance statistics**: off by default. SandFly collects timing statistics, which the **Print performance statistics** command writes to the system log. Uses more memory and power. SandFly shows a confirmation dialog.
- **Explore by touch**: on by default. Turning it off makes SandFly stop reading items you touch and stop handling gestures. SandFly shows a confirmation dialog. Not shown on TVs.
- **Enable node tree debugging**: off by default. The **Print node tree** command writes the tree of items on the screen to the system log. When you turn it on, a dialog offers to assign **Print node tree** to a four-finger triple-tap. On a TV, the search button of the remote prints the tree.
- **Enable debug log overlay**: off by default. Shows the log of SandFly on the screen.
- **Log output level**: how much SandFly writes to the system log, from **NONE** to **VERBOSE**. The default is **ERROR**. Verbose logging, which can put personal information in the log, needs confirmation.
- **Filter accessibility event logging**: the types of accessibility events that SandFly writes to the system log.

To inspect a single item, and to copy the details of an item or the tree of the screen, use the commands described in [](#scripts).
