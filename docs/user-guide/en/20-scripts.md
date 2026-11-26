## Scripts and custom labels {#scripts}

A script changes how SandFly works in one app or in every app, or adds commands to SandFly. A script can relabel or hide items, change what SandFly says, add actions, gestures and keyboard shortcuts, and run steps when something happens on screen. You can install scripts that other people write, or make your own in the script builder without writing code.

A custom label is a name that you give to an item that has no name, or a wrong one, such as an unlabeled button.

Scripts run on phones and tablets only. On watches and TVs, SandFly says "Scripts are not available on this device".

To write scripts in Lua, see [](#api-scripting).

### Scripts {#scripts-overview}

**Scripts** in SandFly settings lists the installed scripts in two groups:

- **App scripts**: scripts that run in the apps they name.
- **Global plugins**: scripts that run in every app.

Each script shows whether it is on, where it runs and its version. Select a script to open its page. See [](#scripts-manage).

The **Scripts** screen also has:

- **Turn off all scripts**: stops all scripts. Scripts stay installed and keep their own switches.
- **New script**: makes a script in the script builder. See [](#scripts-builder).
- **New script from code**: installs a script that you enter as code. See [](#scripts-code).
- **Build a script with SandFly Pro**: SandFly Pro writes a script from your description. See [](#scripts-builder-pro).
- **Import script**: installs a script file. See [](#scripts-install).
- **Script sources**: lists the places to find scripts. See [](#scripts-sources).
- **Update scripts automatically** and **Check for updates**. See [](#scripts-updates).
- **Add the example script**: installs a script that says the name of each app you switch to, as a script to start from. It is shown until you install it.

Scripts don't change the screens of SandFly, such as SandFly settings. Scripts stop while the screen is off, except scripts that you allow to keep running, and while SandFly is paused.

### Install a script {#scripts-install}

A script is a `.lua` file of up to 1 MB, or a `.zip` file of up to 5 MB with a `main.lua` file in it.

To install a script file:

1. Go to **Scripts** > **Import script**.
2. Choose the `.lua` or `.zip` file.
3. Review the install dialog, then select **Install**. See [](#scripts-install-dialog).

You can also open or share a script file, or an `https` link to one, with **Install SandFly script**, such as from a messaging app, a file manager or the downloads of a browser. For a link that another app sends, SandFly shows the address and downloads the script only after you select **Download**. A link to a whole repository doesn't work: open the script in the repository, then share its link.

#### Find scripts in script sources {#scripts-sources}

A script source is a public repository of scripts on GitHub, GitLab, Codeberg, Gitea or Bitbucket. **Scripts** > **Script sources** lists the sources. The SandFly scripts repository is in the list by default.

To add a source:

1. Go to **Scripts** > **Script sources** > **Add source**.
2. Enter the address of the repository, or of a branch or folder in it, such as `github.com/owner/repository`.
3. Select **Add**.

Select a source to list its scripts. Select a script to download it and open the install dialog. If the list can't be loaded, select **Try again**. **Remove source** removes the source from the list; scripts installed from it stay installed.

#### Review a script before installing {#scripts-install-dialog}

Before SandFly installs a script, the install dialog shows:

- Where the script came from: a file, a link, a script source, or **Included with SandFly.**
- Whether SandFly signed the script. SandFly signs the scripts in its own repository. A signed script shows **Signed by SandFly.** wherever it came from. Other scripts show **Not signed by SandFly.** A signed script that was changed after signing shows that the signature doesn't match. The author named in a script is whatever its writer put there; only the signature shows that a script comes from SandFly.
- **It can:** the permissions that the script needs. Permissions marked "Allow only for trusted scripts" give the script access to sensitive data or to all of SandFly. See [](#scripts-permissions).
- **It adds:** the commands the script adds, with their gestures, keys, SandFly menu items and reading controls.

If the script was made for a different version of SandFly, the dialog says that parts may not work, and the button is **Install anyway**.

Importing a script that is already installed updates it, and keeps its data and settings. The dialog says which version it replaces, lists new permissions, and warns when the new version has a different author.

### Update scripts {#scripts-updates}

SandFly checks the installed scripts for newer versions in their sources once a day. To check now, go to **Scripts** > **Check for updates**.

With **Update scripts automatically** on, SandFly installs a newer version without asking when it has the same author, needs no new permissions, is made for this version of SandFly, and comes from the source of the script. A newer version from the SandFly repository installs without asking only if SandFly signed it. **Update scripts automatically** is on by default.

Other newer versions are listed under **Updates** on the **Scripts** screen. Select one to open the install dialog.

### Manage an installed script {#scripts-manage}

Go to **Scripts** and select the script. Its page has:

- **Turn on**: turns the script on or off.
- **Keep running with the screen off**: keeps the script running while the screen is off, such as for a talking clock. It is shown only for scripts that offer it, and is off when you install the script.
- The name, description, version and author of the script, and where it runs.
- **View code**: shows the `main.lua` file of the script, with its manifest. See [](#scripts-code).
- **Edit code**: changes the code of the script. It is not shown for scripts made in the script builder. See [](#scripts-code).
- **Edit in script builder**, for a script made in the script builder, or **Copy to script builder** for other scripts. See [](#scripts-builder-manage).
- **Change with SandFly Pro**: shown for a script made in the script builder, with SandFly Pro. See [](#scripts-builder-pro).
- **Compatibility**: shown when the script was made for a different version of SandFly.
- **Open homepage**: opens the web page of the script, after you confirm the address.
- **Settings**: the settings of the script, if it has any. A command setting opens the list of commands used for gestures, with its search, and with the script's own choices first.
- **Permissions**: a switch for each permission that the script needs. See [](#scripts-permissions).
- **Commands**: the commands of the script and their gestures and keys. See [](#scripts-commands).
- **Run earlier** and **Run later**: the order of scripts that change the same speech. App scripts run first.
- **Log**: the output and errors of the script since SandFly started. See [](#scripts-log).
- **Clear stored data**: deletes the data that the script saved.
- **Remove**: deletes the script, with its code, data and settings, after you confirm.

#### View and edit the code of a script {#scripts-code}

**View code**, on the page of a script, shows its `main.lua` file, with the manifest at the start, as text that you can read by line, word and character, and select.

**Edit code**, on the page of a script that wasn't made in the script builder, opens the same file in a text field. **Save** checks the code as **Import script** checks a script file and replaces the code of the installed script. The script keeps its switch, permissions, gestures, keys, settings and stored data, and runs again with the new code. The `id` in the manifest must stay the same. If the code requests permissions that the script didn't, the install dialog lists them first. See [](#scripts-install-dialog). The other files of a script from a `.zip` file stay as they are.

**New script from code**, on the **Scripts** screen, opens an empty text field. Enter a whole `main.lua` file, with `local manifest = { ... }` before the code, and select **Save**. SandFly checks the code as it checks a script file and shows the install dialog. A manifest with the `id` of an installed script updates that script.

If the code can't be installed, **Script not saved** shows the reason, and the code stays open to change. If you go back with changes that aren't saved, **Discard changes?** opens: choose **Discard** or **Keep editing**.

With **Update scripts automatically** on, a newer version from the source of the script replaces code changed with **Edit code**. See [](#scripts-updates).

To write scripts, see [](#api-scripting).

#### Script permissions {#scripts-permissions}

A script can use only the permissions listed in it. On the page of a script, under **Permissions**, you can turn off each permission. Turning one off stops the parts of the script that need it.

The permissions are:

- **Read the screen**
- **Click, scroll, type and move focus**
- **Hear and change SandFly's speech**
- **Read password fields**
- **Add gestures, keys, menu items and reading controls, and run SandFly's commands**
- **Use the internet**
- **Read and change the clipboard**
- **Go back or home, open notifications, list and open apps, settings and links, send intents to apps, change the volume, ringer mode and device settings such as Wi-Fi and the flashlight, control media, set timers and alarms, and start calls and messages**
- **Read and change spoken notifications**
- **See all activity in every app, including typing**
- **Show dialogs over any app**
- **Run Java code with all of SandFly's access, in every app**: no other permission limits a script with this permission.

Allow the permissions for the internet, the clipboard, passwords, system actions, notifications, all activity, dialogs and Java code only for scripts from people you trust.

#### Script commands {#scripts-commands}

A script can add commands, which you can run with a gesture, keyboard shortcut, the SandFly menu or reading controls. The commands of installed scripts are also in the **Scripts** group of **Commands**, named "Script name: command", where you can assign them like other commands. See [](#commands).

To change the gestures and keys of a script command:

1. Go to **Scripts**, select the script, then select the command under **Commands**.
2. Choose one of these:
   - A gesture or key in the list: **Turn off** keeps the SandFly command of that gesture or key. **Remove** removes it from the command.
   - **Perform gesture or keys**: assigns the next gesture you make, or the keys you press with the SandFly modifier.
   - **Choose gesture**: lists the gestures.
   - **Type keys**: enter keys such as `sf+shift+n`, `sf+t`, `s` or `shift+n`.
   - **Restore defaults**: brings back the gestures and keys of the script. Shown after you change them.

While a script runs, its gestures and keys come before those of SandFly. Some scripts load only to run a command; their commands are in **Commands** only. If you run a command of a script that isn't running on the current screen, SandFly says "Script not running on this screen".

#### Script log and errors {#scripts-log}

**Log**, on the page of a script, lists the output and errors of the script since SandFly started, with the time of each entry. **Copy log** copies it, such as to send it to the author of the script. **Clear log** empties it.

A script that has repeated errors is turned off, and SandFly says "<name> turned off after repeated errors. See its log in Scripts."

### Turn scripts off {#scripts-turn-off}

- To turn off one script, turn off **Turn on** on its page.
- To turn off all scripts, turn on **Scripts** > **Turn off all scripts**. Each script keeps its own switch for when you turn scripts on again.

The **Turn all scripts on or off** command, in the **SandFly** group of commands, does the same as **Turn off all scripts**, and says "Scripts off" or "Scripts on". Use it if a script stops you from using an app. See [](#commands).

### Build a script {#scripts-builder}

The script builder makes a script without code. To start one:

1. Go to **Scripts** > **New script**.
2. Enter a name, then select **OK**.

The script builder opens. Each change is saved and applies at once. The parts of the script are in groups on its screen:

- **Name**: the name of the script.
- **Apps**: the apps the script runs in. With none, it runs in **All apps**. **Add app** adds an app; each app can be limited with **Only in activity** or **Only in window**.
- **Rules**: change items. See [](#scripts-builder-rules).
- **Reading controls**: reading controls that move between items of a kind. **Add reading control** adds one, with the item it moves to.
- **Commands**: steps that run from a gesture, keys, the SandFly menu or a reading control. See [](#scripts-builder-commands).
- **Triggers**: steps that run by themselves. See [](#scripts-builder-triggers).
- **Speech changes**: change what SandFly says. See [](#scripts-builder-speech).
- **Settings**, **About** and **Code**. See [](#scripts-builder-settings).
- **Script**: **Export script** and **Duplicate script**. See [](#scripts-builder-manage).

**Script page** opens the page of the script, with its switch, permissions and log. A part that isn't finished says what it needs, such as "Not used: set the item", and is left out of the script until it is finished.

#### Rules {#scripts-builder-rules}

A rule names an item and changes it. Select **Add rule** to add one.

**Item** names the items the rule applies to, with the same tests as a macro: text with **Match in** and **Regular expression**, item types, **Exact text**, **Resource ID**, **Parent resource ID**, **Class name**, **Role**, **States**, **Inside an item** and **Has an item inside**. See [](#macros). **Use the inspected item** fills these in from the item last shown with **Inspect focused item** or **Copy item details**. See [](#scripts-inspect).

**Changes** sets what the rule does to the item:

- **Label**, **Speak instead**, **Usage hint**, **State**, **Role** and **Heading**.
- **Hide**: **Hide the item**, or **Hide the item and the items inside**.
- **Silence changes**: stops SandFly from reading changes of the item, or of the items inside it too.
- **Read as one item**: reads the item and the items inside it as one item.
- **Reading order**: **Read before an item** or **Read after an item**.
- **Double-tap activates** another item, **Lift to activate**, **Long press after (ms)** and **Tap on screen to activate**.
- **Change speech**: removes, replaces or reorders what SandFly says for the item. See [](#scripts-builder-speech).
- **Arrange what is read**: chooses the parts SandFly reads for the item, in order. **Add a part** adds a text of the item, its own text, or an item inside it.

**Actions** adds entries to the Actions menu of the item. **Add action** chooses **What it does**: **Click an item**, **Long-click an item**, **Move focus to an item**, **Scroll an item forward**, **Scroll an item backward**, **Speak text** or **Speak the text of an item**, and whether it comes before or after the actions of the app. **Buttons inside as actions** makes the buttons inside the item its actions, with **Buttons to leave out**.

To record an action:

1. Under **Actions**, select **Record an action**. The app of the script opens.
2. Press or long-press the items, with SandFly or by touch.
3. Open the **Recording an action** notification.
4. Select **Save**, or **Record again**.

When the rule names the item you inspected, the **Inspected item** group has **Read as one item, with its buttons as actions** and **Choose what is read**, which lists the parts that SandFly reads for the item: unchecked parts are removed from every item like it. **Rule for** chooses whether the rule names **Each row like the one it is in** or **Each item like the inspected one**.

**Duplicate rule** copies the rule in the same script, **Copy rule to script** copies it to another built script, and **Delete rule** removes it.

#### Commands {#scripts-builder-commands}

**Add command** adds a command with a title. On the screen of a command:

- **Steps**: the steps the command runs, made in the same editor as the steps of a macro. See [](#macros).
- **Gestures and keyboard shortcut**: assigns the command on this phone. See [](#scripts-commands).
- **Defaults when shared**: the gesture, keys, **In the SandFly menu** and **As a reading control** that the command has where the script is installed.
- **Rename** and **Delete**.

#### Triggers {#scripts-builder-triggers}

**Add trigger** adds steps that run by themselves:

- **When focus moves to an item**
- **When the app opens** and **When the app closes**
- **When a window changes**
- **When an announcement has a text** and **When a notification has a text**
- **When text changes**, **When the screen changes**, **When a list scrolls**, **When the cursor moves**, **When an item is clicked**, **When an item is long-clicked** and **When an edit field gets input focus**, for any item or one item
- **Every number of minutes** and **Every day at a time**
- **When a button setting is pressed**

Timed triggers run only while the script runs.

#### Speech changes {#scripts-builder-speech}

**Add speech change** changes what SandFly says. **Speech it changes** chooses **Speech for an item**, **Notifications**, optionally of one **App**, **Announcements** or **All speech**, with **Has text (empty: any)**.

**Edits** applies these changes in order: **Remove text**, **Replace text**, **Remove repeated parts**, **Remove parts that have a text**, **Keep the first parts**, **Say instead**, **Say before**, **Say after** and **Say nothing**. Parts are the pieces between commas. In **Say instead**, **Say before** and **Say after**, `{text}` is what SandFly would say. **Preview on the inspected item** shows the result for the item you inspected last.

#### Settings, About and Code {#scripts-builder-settings}

- **Settings** > **Add setting**: adds a **Switch**, **List**, **Text**, **Number**, **Button** or **Command** to the page of the script. Steps use the value of a setting, and a button runs a trigger. A command lets the user choose a SandFly command as for a gesture, with the setting's own options first; **Lua code** can run it with `sf.commands.run`.
- **About**: **Description**, **Version**, **Homepage**, **Load only to run a command** and **Keep running with the screen off**.
- **Code**: **Lua code** added to the end of the script, which SandFly checks for errors before it saves it, and **Extra permissions** that the code needs.

#### Export, duplicate and copy scripts {#scripts-builder-manage}

- **Export script**, in the script builder, saves the script as a `.zip` file. **Import script** installs it, and it can still be changed in the script builder.
- **Duplicate script** makes a copy of the script.
- **Copy to script builder**, on the page of a script that wasn't made in the script builder, makes a copy that you can change in the script builder.
- **Edit in script builder**, on the page of a built script, opens it in the script builder.

#### Build a script with SandFly Pro {#scripts-builder-pro}

With SandFly Pro, **Build a script with SandFly Pro**, on the **Scripts** screen, writes a script from your description of what it should do. **Change with SandFly Pro**, on the page of a built script and in the script builder, changes a script. SandFly Pro reads the screen in front and tests the script. In its notification, SandFly Pro shows requests to open other screens or to try parts of the script; select **Done** or **Reply** there when you have done so.

The script then opens in the script builder under **Written by SandFly Pro**, with a summary, **Accept**, **Ask for changes** and **Discard**. Until you accept it, the script runs without the internet and Java code permissions. Building a script uses much more of the monthly usage limit of SandFly Pro than other tasks. See [](#pro-scripts).

### Inspect items {#scripts-inspect}

These commands show the details that scripts and macros use to name items. They are in the **Developer settings** group of commands, unassigned by default, and can be added to the SandFly menu. See [](#commands).

- **Inspect focused item**: opens the **Item details** window for the item with focus, with **Texts**, **Item types**, **Items inside**, **Script details** and **App**. Double-tap a line to copy its value. **Copy all** copies all details. **Add a rule for this item** adds a rule for the item to a built script, or to a new script for its app, and opens it.
- **Copy item details**: copies the same details as text, and says "Copied item details".
- **Copy screen tree**: copies all items on screen as text, one line for each item, and says how many items it copied.

SandFly says "No item has focus" when there is no item to inspect.

### Custom labels {#scripts-labels}

A custom label replaces the name of an item, such as an unlabeled button in an app. SandFly reads the label each time it reads the item. You can label items that have a resource ID in the app.

#### Add or edit a label {#scripts-labels-add}

1. Move focus to the item.
2. Run the **Add or edit labels** command, in the **Activation** group of commands. See [](#commands).
3. In the **Add label** or **Edit label** dialog, enter the label, then select **OK**.

To remove a label, run **Add or edit labels** on the item, then select **Remove label**. SandFly says "Cannot label" when the item can't be labeled.

To label items from the SandFly menu, turn on **Add or edit labels** in **SandFly menu**. It is off by default. The SandFly menu then has **Labels** for items that can be labeled. When focus is on an unlabeled item, SandFly says that labels are available.

#### Manage custom labels {#scripts-labels-manage}

**Custom labels**, in SandFly settings, lists the apps that have custom labels. Select an app to list its labels, with the time each was last changed. Select a label to edit or remove it.

#### Export and import custom labels {#scripts-labels-export}

**Back up and restore** > **Export settings** includes custom labels. See [](#profiles-export). To share only the labels, export them to a file.

- **Export custom labels**: saves all custom labels to a file and opens the Android share sheet, where you choose where to send or save it. SandFly says "Nothing to export. You have not added any labels" when there are none.
- **Import custom labels**: reads a labels file. In the **Import custom labels to SandFly** dialog, choose **Override** to replace your labels with those in the file when both label the same item, or **Skip** to keep yours.
- **Revert imported labels**: deletes the imported labels and brings back the labels that the import replaced. It is shown after an import.
