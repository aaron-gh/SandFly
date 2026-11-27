# Writing SandFly scripts

This document is the reference for the SandFly script API, version 7. A script is a Lua program that changes how SandFly works in one app or in every app, or adds commands to SandFly. To install and manage scripts, or to make one without code in the script builder, see the [Scripts and custom labels](user-guide/en/20-scripts.md) chapter of the SandFly User Guide.

## Overview

Scripts run on phones and tablets. Watches and TVs don't run scripts.

### What a script can do

*   Change items without code, with [rules](#rules): relabel or hide an item, read a row as one item, change the reading order, read only some parts of an item, add entries to the Actions menu of an item.
*   Change what SandFly says for an item, an announcement, a notification or all speech, with [speech hooks](#speech-hooks).
*   Run code when something happens: an app opens, focus moves, text changes, a list scrolls, a notification arrives. See [Hooks](#hooks).
*   Add [commands](#commands) that run from a gesture, keyboard shortcut, the SandFly menu or a reading control, and [reading controls](#navigation) that move between items of a kind.
*   Read the screen and act on it: find items, click, scroll, type, touch the screen, press keys, recognize text in a part of the screen.
*   Speak, play sounds and tones, vibrate, show text on a braille display.
*   Show dialogs and full-screen windows, keep data and settings, fetch from the internet, translate text, run macros and SandFly commands, open apps, links and settings pages, and change device settings such as Wi-Fi or the volume.

### What a script can't do

*   Use anything its manifest doesn't ask for. The user sees the [permissions](#permissions) before installing, and can turn each one off.
*   Change SandFly screens, such as SandFly settings. See [SandFly screens](#sandfly-screens).
*   Read or write files, other than reading the files in its own package. It has no `io`, `os.execute`, `debug` or `package`.
*   Call Android or Java, unless it has the [`java` permission](#java).
*   Run for long: each call into a script has 2 seconds. See [Limits](#limits).
*   Run while SandFly is paused, while **Turn off all scripts** is on, or while the screen is off, unless the user allows it. See [With the screen off](#with-the-screen-off).

### API versions

This document describes script API version 7. `sf.apiVersion` gives the version of the SandFly a script runs in. Items added after version 1 are marked with the version that added them, such as *Version 2*. Set `minApiVersion` in the manifest to the highest version marked on anything the script uses. Versions 2 to 7 only add to version 1; nothing a version 1 script uses has changed.

## Write your first script

This script says "Hello from a script" when the Android Settings app opens, and adds a command that says how it was run.

```lua
local manifest = {
  id = 'example.hello',
  name = 'Hello',
  author = 'Your name',
  version = '1',
  minApiVersion = 6,
  testedApiVersion = 7,
  apps = { 'com.android.settings' },
  permissions = { 'speech' },
  commands = {
    { id = 'hello', title = 'Say hello' },
  },
}

sf.onAppEnter(function(app)
  sf.speak('Hello from a script')
  print('Started in', app.package)
end)

sf.onCommand('hello', function(event)
  sf.speak('Hello, run from ' .. event.via)
end)
```

The `manifest` table says what the script is, where it runs and what it may do. SandFly reads it without running the script. The code after it runs each time the script loads: it adds a function to the `onAppEnter` hook and a handler for the `hello` command.

1.  Save the script as a text file named `hello.lua`, and put it on the phone, such as in Downloads.
2.  In SandFly settings, go to **Scripts** > **Import script**, and choose `hello.lua`.
3.  Review the install dialog, then select **Install**. The script is on after it is installed.
4.  Open the Settings app. SandFly says "Hello from a script".
5.  In SandFly settings, go to **Scripts** > **Hello** > **Log**. The log has the line that `print` wrote, `Started in com.android.settings`.
6.  In SandFly settings, go to **Commands** > **Scripts**, and assign **Hello: Say hello** to a gesture. Open the Settings app and perform the gesture. SandFly says "Hello, run from command".

To change the script, edit the file, raise `version`, and import it again. A script with the same `id` replaces the installed one, and keeps its stored data, settings and permission choices.

The rest of this document is the reference. [Testing and debugging](#testing-and-debugging) describes the log, errors and reloading. [Example scripts](#example-scripts) lists longer scripts to start from.

## Script files

A script is one of these:

*   A `.lua` file, up to 1 MB. It is the `main.lua` of the script.
*   A `.zip` file with a `main.lua` file, plus anything `main.lua` uses: other Lua files, sounds, macros, data files and translations. The zip can be up to 5 MB, up to 5 MB unpacked, and hold up to 200 files. `main.lua` can be at the top of the zip or in a single folder that holds everything else. Files whose names start with `.`, and the `__MACOSX` folder, are left out.

The name of the file must end in `.lua` or `.zip`. In a zip, code reaches the other files with [`require`](#lua-and-files), [`sf.readFile`](#lua-and-files), [`sf.playSound`](#speech-sound-and-vibration) and [`sf.macros.run`](#macros), and SandFly reads [translations](#translations) from the `locales` folder.

To install a script, go to **Scripts** > **Import script**, or open or share a `.lua` or `.zip` file, or an `https` link to one, with **Install SandFly script**. The install dialog shows where the script came from, whether SandFly signed it, its permissions and its commands. **Scripts** > **New script from code** installs a `main.lua` entered in a text field, checked in the same way.

### The Lua environment

Scripts run in [Lua 5.4](https://www.lua.org/manual/5.4/) through [LuaJava](https://github.com/gudzpoz/luajava). SandFly loads only Lua text, not compiled Lua. A script has:

*   The basic functions: `assert`, `error`, `ipairs`, `next`, `pairs`, `pcall`, `xpcall`, `print`, `rawequal`, `rawget`, `rawlen`, `rawset`, `select`, `tonumber`, `tostring`, `type`, `getmetatable`, `setmetatable`, `load`, `require`, `collectgarbage`, `_G` and `_VERSION`.
*   The `string`, `table`, `math`, `utf8` and `coroutine` libraries.
*   `os.time`, `os.clock`, `os.date` and `os.difftime`.
*   `json`, for JSON text, and `sf`, described in the rest of this document.

Some of these work differently:

*   `load` takes text or a function, never compiled Lua, and runs the chunk in the environment of the script unless you give it another.
*   `setmetatable` refuses metatables with `__gc`.
*   `collectgarbage` takes only `'collect'`, `'count'` and `'step'`.
*   `string.dump` doesn't exist, and `string.rep` refuses to make text longer than 8 MB.
*   `pcall` and `xpcall` can't catch the errors that stop a script that runs out of time or memory.
*   Coroutines that the script makes can call functions that [wait](#waiting), as long as the code that resumes them runs in a task.

There is no `io`, `debug`, `package`, `dofile` or `loadfile`, and no other `os` function.

## The manifest

`main.lua` must contain `local manifest = { ... }` before its code. SandFly reads the table without running the script, so it can hold only plain values: text, numbers, `true`, `false`, `nil` and tables. Variables, function calls and `..` aren't allowed. Comments are.

A table with only listed values, such as `{ 'speech', 'screen' }`, is a list. A table with only named fields, such as `{ id = 'x' }`, is a record. A table can't have both. An empty table `{}` is an empty list.

`sf.manifest()` gives the manifest to the code of the script.

| Field | Type | Meaning |
|---|---|---|
| `id` | text | Required. Lowercase letters, digits, `.`, `-` and `_`, starting with a letter or digit, up to 64 characters. A script with the same id replaces the installed one. |
| `name` | text | Required. Shown in the Scripts list, and at the start of the title of every dialog the script shows. |
| `author` | text | Required. |
| `minApiVersion` | number | Required. The lowest [API version](#api-versions) the script works with. |
| `testedApiVersion` | number | Required. The highest API version you tested with. It can't be lower than `minApiVersion`. SandFly warns about a script tested only with a version older than the last version that changed existing calls; no version since 1 has, so no script gets this warning yet. |
| `version` | text | The version of the script, such as `'1.2'`. The default is `'1'`. Versions are compared number by number, so `'1.10'` is newer than `'1.9'`. Raise it with each change, so that [updates](#publish-a-script) reach users. |
| `description` | text | Shown on the page of the script and in the install dialog. |
| `homepage` | text | An `http` or `https` address. The page of the script has **Open homepage**. |
| `apps` | text or list | Where the script runs. Leave it out for a script that runs in every app. See [Where a script runs](#where-a-script-runs). |
| `permissions` | list | The [permissions](#permissions) the script needs. |
| `settings` | list | [Settings](#settings) on the page of the script. |
| `rules` | list | [Rules](#rules) that change items without code. |
| `commands` | list | [Commands](#commands) the user can run. |
| `navigation` | list | [Reading controls](#navigation) that move between items of a kind. |
| `onDemand` | `true` or `false` | *Version 4.* Loads the script only to run one of its commands. See [Scripts loaded on demand](#scripts-loaded-on-demand). |
| `keepLoaded` | `true` or `false` | *Version 5.* Adds **Keep running with the screen off** to the page of the script. See [With the screen off](#with-the-screen-off). |

SandFly ignores other fields. It refuses a manifest with unknown fields inside rules, queries, actions or navigation entries, and says which field is wrong.

If `minApiVersion` is higher than the API version of this SandFly, the install dialog, the page of the script and the log say that parts may not work, and the script can still be installed.

### Where a script runs

`apps` is a package name, or a list. Each entry is a package name or a record:

```lua
apps = {
  'com.example.mail',
  { package = 'com.example.chat', activity = '.ConversationActivity' },
  { package = 'com.example.chat', window = 'Settings' },
},
```

*   `activity` limits the script to one screen of the app: the full class name of the activity, or the end of it. SandFly gets the activity from the window changes of the app. Until it has seen one, such as right after SandFly starts with the app open, the script runs in all screens of the app.
*   `window` limits the script to windows whose title contains the text, ignoring case.

A script with `apps` is loaded when one of its apps comes to the front, and unloaded when the app leaves. Each load starts the script again, so anything kept in variables is lost. Use [`sf.storage`](#storage) for what should last. An entry without `activity` or `window` also loads the script while SandFly focus is in a window of the app that isn't the active window, such as the window of a keyboard app.

A script without `apps` runs in every app, and stays loaded while it is on, unless it is [loaded on demand](#scripts-loaded-on-demand).

All scripts are unloaded while SandFly is paused, while **Turn off all scripts** is on, and while the screen is off, except scripts kept running with the screen off. Timers, requests, dialogs and windows end when a script unloads.

### SandFly screens

Scripts never change SandFly screens, so that **Scripts** in SandFly settings always works, whatever a script does. While a SandFly screen is in front:

*   Rules, speech hooks, actions, gestures, keys, menu items and reading controls of scripts don't apply, and scripts get no events from SandFly.
*   Calls that need the `screen`, `actions`, `system` or `dialogs` permission raise an error, as do `sf.commands.run`, `sf.macros.run`, `sf.feedback.mute` and `sf.touch.lock`. These errors don't count toward turning the script off.
*   `sf.speak` doesn't interrupt, and `sf.speech.stop` does nothing.

A script can't name SandFly in `apps`. Scripts for all apps stay loaded, so their settings still work. A script with the `java` permission can get past all of this.

### With the screen off

*Version 5.* A script with `keepLoaded = true`, such as a talking clock or reminders, can stay loaded while the screen is off. The user turns this on with **Keep running with the screen off** on the page of the script; it is off when the script is installed, and a script can't turn it on. Only scripts without `apps` and without `onDemand` can have `keepLoaded`.

While the screen is off, timers, requests and `sf.speak` work, and hooks for the screen, such as `onFocus`, don't run. Timers don't wake the phone, so they can run late while the phone sleeps.

### Scripts loaded on demand

*Version 4.* With `onDemand = true`, a script uses no memory until one of its commands runs. SandFly loads the script, runs the command, and unloads the script about 2 seconds after it has no dialog or window open, no timer set, nothing it waits for, and no [`java.proxy` object](#java) still to be called.

Variables are lost when the script unloads, so use [`sf.storage`](#storage) for what should last. Hooks run only while the script is loaded. A script loaded on demand:

*   Needs at least one command, and each command has only `id` and `title`. The user assigns them in **Commands**.
*   Can't have `apps`, `rules`, `navigation` or button settings.
*   Can't call `sf.bind` or `sf.unbind`.

[examples/wikipedia-search.lua](scripting/examples/wikipedia-search.lua) is a script loaded on demand.

### Permissions

A script can use only the permissions in its manifest. The install dialog lists them, and the user can turn each one off on the page of the script. Installing grants every permission in the manifest; an update keeps the permissions the user turned off, and grants new ones after the install dialog lists them.

A call that needs a permission the script doesn't have raises the error "This script needs the *name* permission". A hook, rule, action or command assignment that needs one is skipped, and the log gets one warning for it.

| Permission | What it allows |
|---|---|
| `screen` | Reading the screen: `sf.screen`, the methods of [items](#items) that read the screen, `sf.speech.last()`, recognizing text, and the items given to hooks, commands and actions (without it they are `nil`). The hooks `onFocus`, `onAnnouncement`, `onTextChange`, `onContentChange`, `onScroll`, `onSelectionChange`, `onClick`, `onLongClick` and `onInputFocus`. A script for all apps also needs it to learn which app is in front: `sf.app()`, `onAppEnter`, `onAppLeave` and `onWindowChange`. Rule actions that speak the text of an item. |
| `actions` | Acting on the screen: the actions of items, `item:focus()`, `item:setText()`, `item:tap()` and the other touches, `sf.screen.click`, `sf.touch`, `sf.keys.type`, `sf.keys.press`, and rule actions other than speaking, and `buttonActions`. |
| `speech` | `sf.speak`, `sf.message`, `sf.speech.stop`, `sf.braille.show`, `sf.feedback.mute`, rules that change an item, `sf.rules`, changing speech from a speech hook, and the `onSpeech` hook. |
| `passwords` | The text of password fields, in items and events. Without it, that text is `nil`. |
| `input` | Commands with their own `gesture`, `keys`, `menu` or `control`, `sf.bind`, `sf.unbind`, `navigation`, `sf.commands.run`, `sf.keys.passAll`, the `onActions` hook, and actions in rules. *Version 6:* commands without their own assignments need no permission; before, every command needed `input`. |
| `network` | `sf.fetch` and `sf.translate`. |
| `clipboard` | `sf.clipboard`. |
| `system` | `sf.system`, `sf.resume`, `sf.device.setVolume`, `sf.device.setRingerMode`, `sf.device.set` and `sf.device.media`. |
| `notifications` | The `onNotification` hook, which gets the notifications of every app. |
| `events` | The `onEvent` hook, which gets every accessibility event in every app, including typing. |
| `dialogs` | `sf.ui`. |
| `java` | The `java` module of LuaJava. See [Java](#java). |

These need no permission: sounds, tones and vibration, `sf.speech.isSpeaking`, reading device information, storage, settings, timers, `sf.text`, `sf.readFile`, `require`, `sf.commands.list`, logging, JSON and translations, and `sf.app()` in a script with `apps`.

SandFly marks `passwords`, `network`, `clipboard`, `system`, `notifications`, `events`, `dialogs` and `java` as permissions to allow only for trusted scripts. SandFly refuses a manifest that has `navigation`, or commands with their own `gesture`, `keys`, `menu` or `control`, without `input`.

### Java

With the `java` permission, a script gets the [`java` module](https://luajava.iroiro.party/api.html) of LuaJava: `java.import`, `java.new`, `java.proxy` and the rest. It can call any Java class in SandFly, with all the access SandFly has, in every app, and no other permission limits it. Anything `sf` doesn't offer needs this permission, such as saving a screenshot to a file or sharing a file with another app.

```lua
local Build = java.import('android.os.Build')
sf.speak('This is a ' .. Build.MODEL)
```

Calls into the script hold its Lua state, so a Java object made with `java.proxy` and called from another thread waits until the current call of the script ends. Don't wait in the script for something that needs the main thread of SandFly. After the script unloads, calling a method of an object made with `java.proxy` does nothing and gives `nil`, 0 or `false`, so remove anything the script registered with Android before it unloads, such as in `onAppLeave`.

In a script [loaded on demand](#scripts-loaded-on-demand), each object made with `java.proxy` keeps the script loaded until the first call to one of its methods has finished, or for 30 seconds, for up to 100 objects at once. A listener that stays registered needs a script without `onDemand`. A table or function passed straight to a Java method doesn't count, so use `java.proxy` for those.

## Rules

A rule changes an item without running code. Rules are the cheapest way to fix an app, and they keep working while the code of the script is busy. Rules that change an item need the `speech` permission; rules with actions need `input`.

```lua
rules = {
  { id = 'send_button', label = 'Send' },
  { match = { className = 'ImageView', parentId = 'toolbar' }, hide = true },
  { id = 'row', group = true, hint = 'Double-tap to open' },
},
```

A rule names its item with `id`, with `match`, or with both:

*   `id` is the view id of the item, in the short form (`send_button`) or the full one (`com.example:id/send_button`).
*   `match` is a [query](#queries).

`window` limits a rule to windows whose title contains the text, ignoring case, and `activity` to an activity whose class name ends with the text. A rule with `activity` applies while the activity isn't known yet, as described in [Where a script runs](#where-a-script-runs).

A rule must set at least one of these fields:

| Field | Value | Effect |
|---|---|---|
| `label` | text | The name SandFly reads for the item, in place of the name from the app. |
| `speak` | text | Everything SandFly says for the item, in place of its name, role and state. |
| `hide` | `true` or `'all'` | `true` hides the item: swiping skips it. `'all'` hides the items inside it too. A hidden item is still on screen, so a script can click it. |
| `silence` | `true` or `'all'` | `true` stops SandFly from speaking changes, selections and announcements of the item, such as the updates of a progress bar. `'all'` silences the items inside it too, such as the seek bar in a grouped row. Focusing the item still speaks it. |
| `role` | text | The role of the item, such as `'button'`. An unknown role is an error that lists the roles. |
| `state` | text | The state of the item, such as `'Selected'`. |
| `hint` | text | The usage hint. |
| `heading` | `true` or `false` | Whether the item is a heading. |
| `group` | `true` | Reads the item and everything in it as one item. An item with its own description or text is read by it, without the texts of the items in it. |
| `readBefore`, `readAfter` | id or query | Moves the item in the reading order to the place right before or after another item. Use one, not both. |
| `activate` | query | The item inside it that a double-tap presses, such as the part of a grouped post that opens it. |
| `liftToActivate` | `true` | Activates the item when the finger lifts from it after touching it, as **Lift to activate** does, whatever that setting is. |
| `longPressAfter` | number | For an item with `liftToActivate`, a number of milliseconds from 100 to 10,000. Holding the finger on the item for that time long-presses it, and lifting the finger then does nothing more. Items that can't be long-pressed are left alone. |
| `touchToActivate` | `true` | Activates the item by tapping it on the screen instead of sending it an accessibility click, for controls that ignore that click. Needs Android 11 or later; elsewhere the item is clicked as usual. |
| `read` | list | *Version 6.* What SandFly reads for the item instead of what it says, as up to 12 parts joined by commas. See below. |
| `actions` | list | Up to 10 entries to add to the Actions menu of the item. See [Actions in rules](#actions-in-rules). |
| `buttonActions` | `true` | *Version 6.* Adds each button in the item to its Actions menu when the menu opens, named by its description, its text or the first text in it: up to 20 buttons, up to 8 levels down. A button inside a button is left to the outer one. Needs `actions` and `input`. |
| `skipButtons` | list of text | *Version 6.* Up to 20 texts of buttons that `buttonActions` leaves out: a button whose name contains one, ignoring case. |

Each part of `read` is one of these, with optional `before` and `fromAbove`:

*   `{ path = { 2, 1 } }`: the item at those child numbers, counted from 1, down from the item of the rule, counting only shown items in reading order, as **Items inside** in **Inspect focused item** lists them. Up to 12 numbers.
*   `{ from = query }`: the first item the query finds in the item. `{ from = {} }` is the own text of the item.
*   `before = 'in reply to'` is said before the part.
*   `fromAbove = true` takes an empty part from the nearest of the 4 items above in the same list that has it, such as the sender of a chat message.

```lua
{ id = 'message_row', read = {
  { from = 'sender', fromAbove = true },
  { from = 'body' },
  { from = 'time', before = 'sent' },
} }
```

When several rules match an item, the first rule that sets a field wins for that field; `group`, `liftToActivate` and `touchToActivate` apply if any rule sets them. Rules of app scripts come before rules of scripts for all apps, then scripts in the order set with **Run earlier** and **Run later** on their pages. In a script, rules from `sf.rules.add` come before the rules of the manifest.

A manifest can have up to 500 rules, 100 of them with `match`.

### Actions in rules

```lua
{
  id = 'message_row',
  actions = {
    { title = 'Archive', click = 'archive_button' },
    { title = 'Read time', speak = { textOf = 'timestamp' } },
  },
}
```

Each action has a `title` and one of these:

| Field | Value | Does |
|---|---|---|
| `click`, `longClick` | id or query | Clicks or long-clicks the item. |
| `touchHold` | id or query | *Version 6.* Holds a touch on the item for a second, passed through to the app as a double-tap and hold is, for buttons that act only on a held touch, such as a Like button that shows reactions. |
| `focus` | id or query | Moves SandFly focus to the item. |
| `scrollForward`, `scrollBackward` | id or query | Scrolls the item. |
| `speak` | text, or `{ textOf = item }` | Says the text, or the text of an item named by id or query. `textOf` needs `screen`. |
| `steps` | list | *Version 6.* Up to 10 steps done in order. See below. |

All but `speak` need the `actions` permission. *Version 5:* `first = true` puts the action before the actions of the app: `{ title = 'Archive', click = 'archive_button', first = true }`. Otherwise the actions of the script come after them.

SandFly looks for the item inside the item the Actions menu is for, and then in the whole window. When the item can't do the action, such as the text of a button, the nearest item around it, up to 6 levels up, that can does it. When the item isn't found, SandFly plays a sound and the log gets a warning. In a list where every row has the same button, the search in the whole window finds the button of the first row, so use the [`onActions` hook](#hooks) there and search from the row.

*Version 6.* Each step of `steps` is `{ click = item, where = 'item' }`, or `longClick`, `touchHold`, `focus`, `scrollForward` or `scrollBackward` in place of `click`. `item` is a query, a list of up to 6 queries tried in turn, or `{}` for the item of the action itself. `where = 'item'`, the default, finds the item inside the item of the action; `where = 'screen'` finds it in any app window, such as an entry of the menu the step before opened. SandFly closes its Actions menu first, and each step waits up to 2 seconds for its item. When one isn't found, SandFly presses Back if an earlier step ran, and plays a sound.

```lua
{ title = 'Add to queue', steps = {
  { longClick = {} },
  { click = { text = 'Add to queue' }, where = 'screen' },
} }
```

### Find the details of an item

To write a rule or query, you need the id, class, role and texts of the item. These commands, in the **Developer settings** group of **Commands**, show them; assign them to a gesture or add them to the SandFly menu:

*   **Inspect focused item** opens the **Item details** window for the focused item. Its **Items inside** lists the items in the item in reading order, each with its level (1 for the items directly in it), class, Clickable and texts, so that a text can be told apart from a button with the same text.
*   **Copy item details** copies the same details as text.
*   **Copy screen tree** copies every item of the window, after lines with the app, activity and window title: one line for each item, in order, each item before the items in it. A line has the level (0 for the top item of the window), class, id, role, states, texts, named actions and the script rules that apply, such as `level 5: Button; id: send; clickable, focusable; description: Send`. An item belongs to the nearest line above it with a lower level.

## Queries

A query finds items, in rules, navigation entries and code. It is an id as text, or a record with any of these fields; an item must match all of them.

| Field | Matches |
|---|---|
| `id` | The view id. A short id (`send`) matches the end of the full id after `:id/`; an id with `:` must match exactly. |
| `text` | The text of the item, exactly. |
| `textContains` | Part of the text or content description, ignoring case. |
| `contentDescription` | The content description, exactly. |
| `className` | The end of the class name, so `'Button'` matches `android.widget.Button`. |
| `role` | The role from the class of the item, such as `'button'` or `'check_box'`, as the `role` field of an item gives it. |
| `clickable` | `true` or `false`. |
| `parentId` | The view id of the parent, short or full. |
| `inside` | A query that one of the items around the item must match: its parent, the parent of the parent, and so on. `{ className = 'Button', inside = { textContains = 'play video' } }` matches every button in the row of a video, and not the row itself. `inside` can't hold `inside` or `has`. *Version 2:* a list of up to 4 queries, each of which one of the items around it must match. `{ clickable = true, inside = { { clickable = true }, { id = 'results' } } }` matches every item that can be clicked inside another one in the list `results`, and not the rows of the list. |
| `has` | A query that one of the items inside the item must match, up to 6 levels down and 60 items, nearest first. `{ parentId = 'results', has = { textContains = 'Subscribe to' } }` matches a row of a list that holds a Subscribe button. `has` can't hold `has` or `inside`. *Version 6:* `hasDepth`, 1 to 6, limits how many levels down it looks: with 1, only the items directly in it, so that a row of a list matches and the card that holds the list doesn't. |
| `checkable`, `checked`, `longClickable`, `focusable`, `focused`, `selected`, `enabled`, `editable`, `scrollable`, `visible`, `heading`, `password` | *Version 2.* `true` or `false`. `{ className = 'CheckBox', checked = false }` matches unchecked check boxes. |
| `hint`, `stateDescription` | *Version 6.* The hint or the state, exactly. |
| `contains` | *Version 6.* Part of the text, content description, hint, state or tooltip, ignoring case, as **Any text** in a macro test matches. With `field`, part of that one text. |
| `matches` | *Version 6.* A regular expression found in the text, content description, hint, state or tooltip, ignoring case, as the **Regular expression** check box in a macro test does. With `field`, in that one text. `{ matches = '^new post$', field = 'contentDescription' }` matches an item described as exactly "New post". Repeated groups can't contain repetition or `\|`, and only the first 1,000 characters of a text are looked at. |
| `field` | *Version 6.* The one text that `contains` and `matches` look in, as **Match in** in a macro test: `'text'`, `'contentDescription'` (or `'description'`), `'hint'`, `'stateDescription'` (or `'state'`), `'tooltip'` or `'id'`. An id is matched with and without its package, so `'^send$'` matches `com.example:id/send`. |
| `kind` | *Version 6.* A macro item type, or a list of them that the item must all be: `'clickable'`, `'longClickable'`, `'editable'`, `'scrollable'`, `'adjustable'`, `'checkable'`, `'checked'`, `'selected'`, `'heading'` or `'enabled'`. They match as in a macro test: `'clickable'` and `'longClickable'` count an item up to 6 levels above that can be clicked, as the text of a button is often an item in it; `'scrollable'` counts an item with a scroll action, and `'adjustable'` a slider or progress bar. `{ contains = 'new post', field = 'description', kind = 'clickable' }` is the macro test Item on screen: new post (Description), Clickable. |

Text fields match the text of the item without trimming, except `contains` and `matches`, which trim it. A query in a manifest needs at least one field. Queries in code are checked when they are used; an unknown `kind` or `field`, or a regular expression SandFly can't use, raises an error.

## Items

Hooks, commands, actions and `sf.screen` give items. An item is a snapshot: its fields don't change when the screen does. Call `item:refresh()` for a new one. A field the item doesn't have is `nil`.

| Field | Value |
|---|---|
| `text`, `contentDescription`, `hint`, `stateDescription` | Text. The text of a password field is `nil` without the `passwords` permission. |
| `id` | The full view id, such as `com.example:id/send`. |
| `className`, `packageName` | Text. |
| `role` | The role from the class of the item, such as `'button'`, `'edit_text'` or `'none'`. |
| `windowId`, `childCount` | Numbers. |
| `bounds` | `{ left, top, right, bottom }`, in screen pixels. |
| `selectionStart`, `selectionEnd` | The selection in an edit field, counted from 0, or -1. |
| `checkable`, `checked`, `clickable`, `longClickable`, `focusable`, `focused`, `accessibilityFocused`, `selected`, `enabled`, `editable`, `password`, `scrollable`, `visible`, `heading` | `true` or `false`. `focused` is input focus; `accessibilityFocused` is SandFly focus. |
| `actions` | A list of `{ id, label }`: the actions of the item that have a label or a name in `item:perform`. |
| `roleDescription` | *Version 2.* The role the app names itself, such as `"tab"` on a web page. |
| `error` | *Version 2.* The error text of a field. |
| `tooltip`, `paneTitle`, `containerTitle` | *Version 2.* Text. |
| `uniqueId` | *Version 2.* An id that stays the same while the item exists, on Android 13 and later. |
| `multiLine`, `dismissable`, `contentInvalid`, `textSelectable`, `importantForAccessibility`, `screenReaderFocusable` | *Version 2.* `true` or `false`. |
| `maxTextLength`, `inputType`, `drawingOrder` | *Version 2.* Numbers. |
| `liveRegion` | *Version 2.* `'polite'`, `'assertive'` or `nil`. |
| `range` | *Version 2.* For sliders and progress bars: `{ min, max, current, type }`, where `type` is `'integer'`, `'float'`, `'percent'` or `'indeterminate'`. |
| `collection` | *Version 2.* For lists, grids and tables: `{ rows, columns, hierarchical }`. |
| `collectionItem` | *Version 2.* For rows and cells: `{ row, column, rowSpan, columnSpan, heading, rowTitle, columnTitle }`. `row` and `column` count from 1. |

`a == b` is true when two snapshots are the same item. `tostring(item)` gives its text, description, id or class, the first it has.

### Reading around an item

These need the `screen` permission.

*   `item:parent()` gives the parent item, or `nil`.
*   `item:children()` gives a list of the items directly in it. `item:child(index)` gives one, from 1.
*   `item:find(query)` gives the first item that matches the [query](#queries), searching the item itself and everything in it, or `nil`. `item:findAll(query, limit)` gives a list of up to `limit` items, 50 if left out, at most 200.
*   `item:refresh()` gives a new snapshot, or `nil` if the item is gone.
*   `item:labeledBy()` and `item:labelFor()` *Version 2* give the item that labels this one, or that this one labels, or `nil`.
*   `item:recognizeText()` *Version 2* [recognizes text](#recognizing-text) in the part of the screen the item covers.

### Acting on an item

These need the `actions` permission. They [wait](#waiting) for the result and give `true` or `false`; where the script can't wait, such as in a speech hook, they still act and give `nil`.

*   `item:click()`, `item:longClick()`, `item:scrollForward()` and `item:scrollBackward()` perform the action.
*   `item:showOnScreen()`, `item:expand()`, `item:collapse()` and `item:dismiss()` *Version 2* perform the action.
*   `item:focus()` moves SandFly focus to the item.
*   `item:setText(text)` replaces the text of an edit field.
*   `item:setSelection(start, end)` *Version 2* selects text in an edit field, or moves the cursor when `end` is left out. Positions count from 0.
*   `item:moveCursor(unit, direction, extend)` *Version 2* moves the cursor by `'character'` (the default), `'word'`, `'line'`, `'paragraph'` or `'page'`. `direction` is `'next'` (the default) or `'previous'`; `extend = true` selects the text passed over.
*   `item:setProgress(value)` *Version 2* sets a slider to a number between `range.min` and `range.max`.
*   `item:scrollTo(row, column)` *Version 2* scrolls a list or grid to a row and column, counted from 1.
*   `item:nextHtmlElement(element, direction)` *Version 2* moves in web content to the next or previous element of a kind, such as `'HEADING'`, `'LINK'`, `'BUTTON'`, `'LANDMARK'` or `'TABLE'`.
*   `item:tap()`, `item:doubleTap()` and `item:longPress(ms)` *Version 2* touch the middle of the item on the screen, as a finger would, for items with no click action. `ms` is 1,000 if left out. They work as [`sf.touch`](#touch-and-keys) does, and raise an error for an item with no place on screen.
*   `item:perform(action, args)` performs an action by name, by the label of a custom action (ignoring case), or by its number:
    *   Without `args`: `click`, `longClick`, `scrollForward`, `scrollBackward`, `expand`, `collapse`, `dismiss`, `copy`, `cut`, `paste` and `select`. *Version 2* adds `clearSelection`, `focusInput`, `clearFocus`, `showOnScreen`, `scrollUp`, `scrollDown`, `scrollLeft`, `scrollRight`, `pageUp`, `pageDown`, `pageLeft`, `pageRight`, `contextClick`, `imeEnter`, `pressAndHold`, `showTooltip`, `hideTooltip` and `showTextSuggestions`.
    *   *Version 2*, with `args`: `setText` (`text`), `setSelection` (`start`, `end`), `setProgress` (`value`), `scrollToPosition` (`row`, `column`, from 1), `nextAtGranularity` and `previousAtGranularity` (`unit`, `extend`), and `nextHtmlElement` and `previousHtmlElement` (`element`).

```lua
local field = sf.screen.find({ className = 'EditText', editable = true })
if field then
  field:setText('Hello')
  field:perform('imeEnter')
end
```

SandFly keeps the 500 items a script used last. Using an older one raises the error "That item is no longer available".

## Waiting

Some calls wait for an answer: dialogs, `sf.fetch`, `sf.sleep`, the actions of items, touches and keys, `sf.commands.run`, `sf.screen.waitFor`, recognizing text, `sf.translate`, `sf.speech.last`, `sf.device.set`, `sf.system.openApp`, the system actions, `sf.playSound` and `sf.speak` with `wait`.

A script waits only where SandFly runs it as a task: the main file, hooks other than the four speech hooks, `onActions` itself, commands, actions, timers, dialog callbacks, and functions given to `sf.run`. While one task waits, other tasks of the script run. Coroutines the script makes can wait too, inside a task.

```lua
sf.onCommand('delete', function()
  if sf.ui.confirm('Delete the message?') then
    sf.screen.click('delete_button')
  end
end)
```

Where the script can't wait, such as in `onFocus`, which must answer at once:

*   Actions of items, touches, keys, `sf.commands.run`, `sf.device.set`, `sf.system.openApp`, the system actions and `sf.playSound` still act, and give `nil`.
*   The other calls that wait raise the error "This waits for an answer, so call it from an event handler, a command, a timer or sf.run".

Start a task with `sf.run(function() ... end)` to do something that waits.

## Hooks

Each hook takes a function and returns a function that removes it again. A script can add several functions to one hook.

```lua
local stop = sf.onAppEnter(function(app) print(app.package) end)
stop() -- removes it
```

| Hook | Called | Arguments | Permission |
|---|---|---|---|
| `sf.onFocus` | Before SandFly speaks a focused item. | The item, and the text SandFly will say. | `screen` |
| `sf.onSpeech` | Before SandFly says anything. | The text. | `speech` |
| `sf.onNotification` | Before SandFly speaks a notification or toast of any app. | `{ package, toast, title, text, category }`, and the text. `toast` is `true` for a toast. | `notifications` |
| `sf.onAnnouncement` | Before SandFly speaks an announcement of an app the script runs in. | `{ package, text }`, and the text. | `screen` |
| `sf.onAppEnter`, `sf.onAppLeave` | When the script loads or unloads, and, in a script for all apps, when the app in front changes. | `{ package, activity, window, meta }`, as `sf.app()` gives it without `label`. | `screen` for scripts for all apps |
| `sf.onWindowChange` | When a window opens or changes. | `{ package, activity, window, meta }`. | `screen` for scripts for all apps |
| `sf.onTextChange` | When text in an edit field changes. | `{ package, node, text, before, from, added, removed, password }`. `before` is the text before the change; `from` is where it changed, from 0. | `screen` |
| `sf.onContentChange` | 200 milliseconds after part of the screen changes, once for each app. | `{ package, node, changeTypes }`. | `screen` |
| `sf.onScroll` | *Version 2.* 150 milliseconds after a list or page scrolls, for the last scroll. | `{ package, node, from, to, count, scrollX, scrollY, deltaX, deltaY }`. `from` and `to` are the first and last items shown, counted from 1, and `count` the number of items, where the app gives them. | `screen` |
| `sf.onSelectionChange` | *Version 2.* When the cursor moves or the selection changes in a text. | `{ package, node, text, start, end, length, password }`. | `screen` |
| `sf.onClick`, `sf.onLongClick` | *Version 2.* When an item is clicked or long-clicked, by anyone. | `{ package, node }`. | `screen` |
| `sf.onInputFocus` | *Version 2.* When an item gets input focus, such as an edit field the keyboard types into. | `{ package, node }`. | `screen` |
| `sf.onEvent` | For every accessibility event in every app. | `{ type, package, className, text, contentDescription, beforeText, password, windowId, time }`, with `changeTypes` for content and window changes, and `notification` (as given to `onNotification`) for notifications. `type` is the event type in lower camel case, such as `'viewClicked'` or `'windowStateChanged'`. | `events` |
| `sf.onSettingChange` | When one of the settings of the script changes, by the user or by `sf.settings.set`. | The key and the new value. | none |
| `sf.onButton` | When the user presses a button setting of the script. | The key. | none |
| `sf.onActions` | When SandFly builds the Actions menu for an item. | The item. Return a list of `{ title, run }`. | `input` |

`node` is the item the event came from, or `nil`. The text of a password field is `nil` without the `passwords` permission. Hooks with `node` get events only from the apps the script runs in. Events from SandFly itself never reach scripts. SandFly drops `onEvent` events while 100 are waiting for the script.

`onAppEnter` also runs when a script loads, with the app in front, and `onAppLeave` when a script with `apps` unloads. `onAppLeave` then has 250 milliseconds.

`sf.onActions` answers at once and can't wait. Each `run` function is called with the item, as a task, so it can wait. The actions come after the actions of the app; *Version 5:* `first = true` puts one before them. The answer must come within 50 milliseconds, or the menu opens without the actions of the script.

```lua
-- Needs screen, actions and input.
sf.onActions(function(row)
  local reply = row:find('reply_button')
  if reply then
    return { { title = 'Reply', first = true, run = function() reply:click() end } }
  end
end)
```

### Speech hooks

`onFocus`, `onSpeech`, `onNotification` and `onAnnouncement` can change what SandFly says. Return:

*   Text, or `{ speak = text }`, to say that instead.
*   `{ before = text, after = text }` to add to what is said, joined with spaces.
*   `false` or `{ silent = true }` to say nothing.
*   Nothing, to leave it alone.

```lua
sf.onFocus(function(item, text)
  if item.id and item.id:find(':id/timestamp$') then
    return { before = 'Sent' }
  end
end)
```

Changing speech needs the `speech` permission. When several functions or scripts change the same speech, each gets the text the one before made: app scripts first, then in the order set with **Run earlier** and **Run later**.

SandFly waits 30 milliseconds for all speech hooks together, then speaks without them, so keep them fast. They can't [wait](#waiting). They run ahead of other work queued for scripts, but can't interrupt Lua already running: if a script is in the middle of something longer, such as a command, SandFly doesn't wait at all. [`sf.text.edit`](#text) applies the edits of the speech changes of the script builder.

## Commands

A command is something the user runs: with a gesture, keys, the SandFly menu, a reading control, or an assignment in **Commands**. Declare each command in the manifest, then handle it in code:

```lua
commands = {
  { id = 'unread', title = 'Next unread', gesture = 'swipeUpThenRight', keys = 'sf+u' },
  { id = 'summary', title = 'Read summary', menu = true },
  { id = 'messages', title = 'Messages', control = true },
  { id = 'search', title = 'Search' },
},
```

| Field | Meaning |
|---|---|
| `id` | Required. Letters, digits, `.`, `-` and `_`, up to 64 characters, unique in the script. |
| `title` | Required. The name the user sees, in **Commands**, the SandFly menu and the reading controls. |
| `gesture` | A gesture name, or a list of them. The names are in [Gestures](#gestures). |
| `keys` | Keys, or a list of them. See [Keys](#keys). |
| `menu` | `true` adds the command to the SandFly menu. |
| `control` | `true` adds it to the reading controls. Swiping up or down with the control selected runs the command with `direction` set to `'previous'` or `'next'`. |

A command with `gesture`, `keys`, `menu` or `control` needs the `input` permission. *Version 6:* a command with only `id` and `title` needs no permission; before, every command needed `input`. A script that does a task when its command runs, such as turning on the flashlight, lists only the permissions of the task. A manifest can have up to 50 commands.

`sf.onCommand(id, fn)` handles a command, and returns a function that removes the handler. It raises an error for an id the manifest doesn't declare. A command does nothing until it has a handler.

```lua
sf.onCommand('unread', function(event)
  -- event.id, event.via, event.direction, event.node
end)
```

The function gets a record:

*   `id`: the id of the command.
*   `via`: `'gesture'`, `'keys'`, `'menu'`, `'control'`, or `'command'` for an assignment in **Commands**.
*   `direction`: `'next'` or `'previous'` for a reading control, otherwise `nil`.
*   `node`: the focused item, or for the SandFly menu the item it opened on. `nil` without the `screen` permission.

Return `false`, before waiting for anything, to have SandFly do what the gesture or keys do without the script.

While a script runs, its gestures and keys come before those of SandFly, except the gesture assigned to **Turn all scripts on or off**. When two scripts take the same gesture or keys, app scripts come first, then the order set with **Run earlier** and **Run later** on the pages of the scripts. The user can turn off each gesture and key on the page of the script, or assign others there.

*Version 4.* Every command of a script that is on is listed in **Commands** > **Scripts**, as the name of the script and the title of the command, such as **Hello: Say hello**. From there the user assigns it to a gesture, a keyboard shortcut, the SandFly menu, a custom menu, a custom reading control or the gestures of an app, as with the commands of SandFly; `event.via` is then `'command'`. These assignments aren't shown on the page of the script.

If a command runs while its script is off, in an app the script doesn't run in, or on a SandFly screen, SandFly says "Script not running on this screen".

### Keys

Keys are one combination, or two after each other separated by a comma. A combination is modifiers and a key joined by `+`:

*   `sf` is the SandFly modifier. *Version 2:* keys can leave it out.
*   The modifiers are `shift`, `ctrl` (or `control`), `alt` and `meta` (or `search`).
*   The key is a letter, digit or symbol (`.` `/` `;` `'` `-` `=` `[` `]` `\` `` ` ``), an Android key name without `KEYCODE_`, such as `space`, `enter`, `tab` or `f5`, or one of `up`, `down`, `left`, `right`, `backspace`, `delete`, `esc`, `home`, `end`, `pageup`, `pagedown` and `return`.

Examples: `'sf+shift+n'`, `'sf+t, s'`, `'k'`, `'shift+n'`, `'space'`, `'t, s'`. Only the first combination can have `sf`. After the first keys of two, SandFly plays a sound and waits 3 seconds for the second; a second key that no command has plays another sound.

*Version 2.* Keys without `sf` don't apply while an edit field has input focus. For them, SandFly waits up to 30 milliseconds for the command. If it returns `false` in that time, before waiting for anything, the keys go to the app. Otherwise they don't, and returning `false` later does nothing. Holding the keys down runs the command again for each key repeat. Arrow key navigation, while on, takes the arrow keys before scripts.

### Assign gestures and keys from code

`sf.bind(command, assignment)` adds a gesture or keys to a declared command, and `sf.unbind(command, assignment)` removes one. `assignment` is `{ gesture = name }` or `{ keys = keys }`. The change is kept, and shown on the page of the script. They need the `input` permission, and raise an error in a script loaded on demand, for an undeclared command and for an unknown gesture or keys.

```lua
sf.bind('unread', { gesture = 'twoFingerSwipeUpThenRight' })
```

## Navigation

```lua
navigation = {
  { title = 'Unread messages', match = { className = 'UnreadRow' } },
  { title = 'Headlines', id = 'headline' },
},
```

Each entry becomes a reading control with its `title`. Swiping up or down with it selected moves SandFly focus to the next or previous shown item in the window that matches `id`, `match`, or both, and plays a sound when there is none. Navigation needs the `input` permission. A manifest can have up to 20 entries.

## Speech, sound and vibration

**`sf.speak(text, options)`** says `text`. `options` is a record with any of:

*   `interrupt` (boolean, default `false`): stops what SandFly is saying first. Otherwise the text is queued.
*   `rate` and `pitch` (numbers from 0.25 to 4, default 1): multiply the speech rate and pitch the user set.
*   `language` (text): a language tag such as `'de'`, for the text.
*   `wait` (boolean): [waits](#waiting) until the text is said, and gives `true`, or `false` if it was interrupted, muted or not said within 2 minutes.

The text is said while media plays, during calls, while another app records from the microphone and during dictation, and not while SandFly speech is muted. Needs `speech`.

```lua
-- In a command: wait for Done to be said, then play a sound.
sf.speak('Downloading', { interrupt = true })
if sf.speak('Done', { wait = true }) then sf.playSound('end') end
```

**`sf.message(text, options)`** *Version 2* says the text and shows it on a connected braille display. `interrupt` is `true` if left out. Needs `speech`.

**`sf.speech.stop()`** *Version 2* stops what SandFly is saying. Needs `speech`.

**`sf.speech.isSpeaking()`** *Version 2* gives whether SandFly is speaking.

**`sf.speech.last()`** *Version 2* [waits](#waiting), and gives the text of the last thing SandFly said, or `nil`. Needs `screen`.

**`sf.braille.show(text)`** *Version 2* shows text on a connected braille display for a while, as SandFly shows announcements, up to 1,000 characters. Needs `speech`.

**`sf.playSound(name)`** plays a sound of SandFly or a sound file in the package of the script, such as `'sounds/chime.wav'`. The sounds of SandFly are `focus`, `actionable`, `click`, `longClick`, `scroll`, `listEnter`, `listExit`, `end`, `enter`, `typo` and `beep`. In a task, it gives whether the sound played; for a file, it waits until the file has played.

**`sf.playTone(frequency, ms, options)`** plays a sine tone. `frequency` is in hertz, 440 if left out; `ms` is 100 if left out; `options.volume` is from 0 to 1, 0.5 if left out.

**`sf.playAudio(samples, options)`** plays a list of samples from -1 to 1, up to 10 seconds. `options.sampleRate` is from 8,000 to 48,000, 22,050 if left out; `options.volume` is from 0 to 1, 1 if left out.

**`sf.vibrate(pattern)`** vibrates for a number of milliseconds, 50 if left out, or for a list of up to 20 numbers that alternate vibration and pause, starting with vibration. Each is at most 2,000 milliseconds, and all together at most 5,000.

Sounds, tones and vibration need no permission. They follow **Sound feedback** and **Vibration feedback** in SandFly settings, and don't play while SandFly is paused or muted. A script can play 4 sound files, tones and samples at once; more are skipped.

**`sf.feedback.mute(what, ms)`** *Version 6* mutes SandFly speech, sounds or vibration for `ms` milliseconds, at most 10,000. `what` is a record with `speech`, `sounds` and `vibration`, each `true` to mute it; at least one must be `true`. Speech under way stops, and what SandFly would say, play or vibrate while muted is dropped, not queued, including `sf.speak` and sounds of the script. It gives a handle whose `release()` ends the mute. Needs `speech`.

**`sf.feedback.unmute()`** *Version 6* ends a mute the script started.

A mute also ends when the script unloads or raises an error, when the app it started in leaves the screen, and when the user touches the screen, makes a gesture or presses a key; touches and keys of the script don't end it. A new mute, of this script or another, replaces the one before. Scripts can't mute SandFly on SandFly screens.

```lua
-- In a command: silence SandFly while the script pages through a list.
local list = sf.screen.find({ scrollable = true })
if list then
  local mute = sf.feedback.mute({ speech = true, sounds = true }, 3000)
  for _ = 1, 5 do list:scrollForward() end
  mute.release()
end
```

## Screen

These need the `screen` permission. They search the active window, and raise an error while a SandFly screen is in front.

**`sf.screen.focused()`** gives the item with SandFly focus, or `nil`.

**`sf.screen.inputFocused()`** *Version 2* gives the item with input focus, such as the edit field the keyboard types into, or `nil`.

**`sf.screen.root()`** gives the top item of the active window, or `nil`.

**`sf.screen.find(query)`** gives the first item in the active window that matches the [query](#queries), or `nil`. **`sf.screen.findAll(query, limit)`** gives a list of up to `limit` items, 50 if left out, at most 200. A search goes through the items in order, each before the items in it, looks at up to 3,000 items, and stops when the call runs out of [time](#limits).

**`sf.screen.at(x, y)`** *Version 2* gives the innermost shown item at a point on the screen, in pixels, in the top window there.

**`sf.screen.windows()`** *Version 2* gives a list of the windows on screen, top first. Each is `{ id, type, title, layer, active, focused, bounds, root }`. `type` is `'application'`, `'inputMethod'`, `'system'`, `'accessibilityOverlay'`, `'splitScreenDivider'` or `'magnificationOverlay'`, and `root` is the top item of the window. `window.root:find(query)` searches a window that isn't active, such as the keyboard. SandFly windows, such as the SandFly menu, aren't listed.

**`sf.screen.waitFor(query, timeout, interval)`** *Version 2* [waits](#waiting) until an item that matches the query is in the active window, and gives it, or gives `nil` after `timeout` milliseconds: 5,000 if left out, at most 60,000. It looks every `interval` milliseconds: 250 if left out, at least 100.

**`sf.screen.click(query)`** *Version 2* finds an item, or takes an item, and clicks it, or taps it if it has no click action. It gives whether that worked, and `false` if no item matches. Needs `actions` too.

**`sf.screen.recognizeText()`** *Version 2* [recognizes text](#recognizing-text) on the whole screen.

**`sf.app()`** gives `{ package, label, activity, window, meta }` for the app in front. `label` is the name of the app. `activity` is `nil` until SandFly has seen the app change window. `window` is the title of the active window. `meta` holds the meta-data in the manifest of the app with a true or false, number or text value, such as `sf.app().meta['com.example.flag']`. A script for all apps needs `screen` for it.

```lua
sf.run(function()
  local button = sf.screen.waitFor({ text = 'Continue' }, 10000)
  if button then button:click() else sf.speak('No Continue button') end
end)
```

### Recognizing text

*Version 2.* `sf.screen.recognizeText()` and `item:recognizeText()` take a screenshot and recognize the text in it, on the device. SandFly uses the recognizer for the script of the speech language and the device languages: Latin, Chinese, Japanese, Korean, Devanagari, or Cyrillic once its model is downloaded in **Image and screen descriptions** > **Cyrillic text recognition**. They [wait](#waiting), need `screen` and Android 11 or later, and give `{ text, blocks }`. Each block is `{ text, bounds, lines }`, and each line `{ text, bounds }`, in screen pixels, so a script can tap a line with `sf.touch.tap`.

A script can recognize text twice at once. Android allows only a few screenshots a second, so SandFly tries again twice when Android refuses one, and raises an error after that. Recognition also raises an error when the item isn't on screen.

```lua
-- In a command: tap the first line that says Skip.
local result = sf.screen.recognizeText()
for _, block in ipairs(result.blocks) do
  for _, line in ipairs(block.lines) do
    if line.text:find('Skip') then
      local b = line.bounds
      sf.touch.tap((b.left + b.right) // 2, (b.top + b.bottom) // 2)
      return
    end
  end
end
```

## Touch and keys

*Version 2.* These need the `actions` permission. They [wait](#waiting) for the result where they can, and give whether it worked.

### Touch the screen

Touching the screen needs Android 11 or later. Points are in screen pixels, 0 or more. While a touch of a script is under way, touches on the screen pass through to apps, so that SandFly doesn't take the touches of the script as gestures.

*   `sf.touch.tap(x, y)` taps a point.
*   `sf.touch.doubleTap(x, y)` taps a point twice.
*   `sf.touch.longPress(x, y, ms)` holds a touch on a point for `ms` milliseconds, 1,000 if left out.
*   `sf.touch.swipe(x1, y1, x2, y2, ms)` moves a finger from one point to the other in `ms` milliseconds, 300 if left out.
*   `sf.touch.gesture(strokes)` performs up to 10 strokes at once, for gestures with more fingers. Each stroke is `{ points, start, duration }`: a list of 1 to 100 `{ x, y }` points, followed in `duration` milliseconds (1 to 10,000, 50 if left out), starting `start` milliseconds after the gesture begins (0 to 10,000).

```lua
-- A two-finger swipe up in the middle of a 1080-pixel-wide screen.
sf.touch.gesture({
  { points = { { x = 440, y = 1600 }, { x = 440, y = 800 } }, duration = 300 },
  { points = { { x = 640, y = 1600 }, { x = 640, y = 800 } }, duration = 300 },
})
```

*Version 6.* `sf.touch.lock(ms)` locks touch for `ms` milliseconds, at most 10,000: touches and gestures of the user on the screen do nothing and don't reach the app, while `sf.touch` gestures of the script still go to the app. It gives a handle whose `release()` ends the lock, and `sf.touch.unlock()` ends a lock the script started. The lock also ends when the script unloads or raises an error, when the app it started in leaves the screen, and when the user presses both volume keys together or runs **Lock or unlock touch**. A new script lock, of this script or another, replaces the one before. Scripts can't lock touch on SandFly screens. Touch lock needs Android 13 or later and **Handle gestures in SandFly**; elsewhere, nothing is locked.

### Type and press keys

*   `sf.keys.type(text)` types up to 2,000 characters where the cursor is in the edit field with input focus, replacing any selection. Into a password field, or with no edit field, it types through Shizuku: line breaks and tabs become Enter and Tab presses, and other control characters are left out.
*   `sf.keys.press(keys)` presses keys through Shizuku, one after another. `keys` is a list, or names separated by spaces: 1 to 100 Android key names with or without `KEYCODE_`, in any case, such as `'enter'`, `'tab'` or `'KEYCODE_MEDIA_PLAY_PAUSE'`, and `up`, `down`, `left`, `right`, `center`, `backspace`, `delete`, `esc`, `return`, `home`, `end`, `pageup`, `pagedown`, `playpause`, `next`, `previous`, `volumeup` and `volumedown`. `home` and `end` move the cursor to the start or end of the line; for the Home button, use `sf.system.home()`.

Keys and typing through Shizuku need Shizuku running with SandFly allowed, set up in **Shizuku and root**. Otherwise they raise an error.

```lua
sf.keys.type('Meeting at 3')
sf.keys.press('enter')
```

### Pass every key to the app

*Version 3.* `sf.keys.passAll(on)` sends every key to Android and the app in front, as **Turn SandFly keyboard input on or off** does: the SandFly key and its shortcuts, browse mode keys, arrow key navigation, the review cursor, and the keys of scripts, this one too. Only the keys of **Turn SandFly keyboard input on or off** still work. `sf.keys.passAll()` or `sf.keys.passAll(true)` starts it, and `sf.keys.passAll(false)` stops it. Needs `input`.

It lasts while the script is loaded: a script with `apps` stops passing keys when its app leaves, and a script for all apps passes them in every app until it stops. Keys are never passed on SandFly screens or the lock screen. The keys of **Turn SandFly keyboard input on or off** stop it until the script calls `sf.keys.passAll` again. SandFly says nothing when a script starts or stops passing keys, so tell the user.

```lua
-- A game that uses every key.
local manifest = {
  id = 'example.game-keys', name = 'Game keys', author = 'Example',
  minApiVersion = 3, testedApiVersion = 6,
  apps = 'com.example.game',
  permissions = { 'input', 'speech' },
}

sf.keys.passAll()
sf.message('Keys go to the game')
```

## Macros

*Version 6.* **`sf.macros.run(file, values)`** runs the macro in a file of the script, such as `'macros/reply.json'`, as SandFly runs the macros of the user. The file is up to 1 MB, in the format of the macros in **Export to file**, and SandFly runs the first macro in it. See Macros in the [item files reference](reference/item-files.md#macros).

*   It stops the macro that is running, if any, and gives `true` at once. The steps run after the script carries on, so it can be called from any hook.
*   `values` is a record of texts the steps use as `{name}`, such as `{ text = item.text }`: up to 200 names of letters, digits, `.`, `-` and `_`, up to 64 characters each, with values up to 10,000 characters. The macro also has the values that macros of the script kept with **Set value**, **Fetch link** and **Translate text**.
*   It raises an error on SandFly screens, for a file that holds no macro, and for a macro that runs **Turn all scripts on or off** or **Turn SandFly keyboard input on or off**.

The script needs the permissions of the steps: `screen` for steps and tests that read the screen or use `{item}`; `screen` and `actions` for steps that press, hold, focus or act on items; `actions` to tap a position, type text or press keys; `speech` to speak; `input` to run commands, with the permissions those commands need as for [`sf.commands.run`](#run-sandfly-commands); `system` to open apps, links, settings pages and the share sheet, set the volume, ringer mode or device controls, press media keys, set timers and alarms, call, message and open the camera; `clipboard` to copy text or use `{clipboard}`; `dialogs` to show a message; and `network` to fetch a link or translate text.

**`sf.macros.stop()`** stops a macro this script started.

The script builder keeps the steps of each command and trigger in a file in `macros/`, and runs it with `sf.macros.run`.

## Run SandFly commands

*Version 2.* A script can run any command that a gesture or keys can be assigned to in SandFly, such as moving to the next heading, reading from the top or changing the reading control.

**`sf.commands.list()`** gives a list of the commands, `{ id, title, category }`, in the order of **Commands** in SandFly settings. It leaves out **Turn all scripts on or off**, **Turn SandFly keyboard input on or off**, **Open app** and app shortcut commands, and the commands of scripts.

**`sf.commands.run(id)`** runs a command from the list, as its gesture would, and gives whether it ran. It raises an error for an id not in the list, and on SandFly screens. Needs `input`. These commands also need:

*   `system`: commands that pause or silence SandFly, turn spoken notifications on or off, act on the system (Back, Home, recent apps, previous app, notifications, quick settings, screenshots, lock screen, power menu, restart and power off, all apps, accessibility button), answer or end a call, change the volume, switch the keyboard, use the microphone (dictation, voice commands), turn magnification, the braille display, pass-through or direct touch on or off, and the SandFly Pro commands.
*   `clipboard`: commands that copy, cut, paste or add to the clipboard, copy text from the screen, the last utterance, the screen tree or item details, select the last spoken phrase, and open the clipboard or speech history.

```lua
sf.commands.run('NEXT')          -- Next item
sf.commands.run('READ_FROM_TOP') -- Read from top
```

## Rules from code

These need the `speech` permission.

*   **`sf.rules.add(rule)`** adds a [rule](#rules), checked as a rule in the manifest, and gives a number for `sf.rules.remove`. A script can add up to 200.
*   **`sf.rules.remove(number)`** removes a rule it added.
*   **`sf.rules.clear()`** removes all rules it added.

Added rules last while the script is loaded, and come before the rules of the manifest.

```lua
local rule = sf.rules.add({ id = 'promo_banner', hide = 'all' })
```

## Storage

`sf.storage` keeps data while the script is installed, between loads and updates. Values are text, numbers, `true`, `false` and tables of them. Keys are 1 to 200 characters.

*   **`sf.storage.get(key, fallback)`** gives the stored value, or `fallback` if there is none.
*   **`sf.storage.set(key, value)`** stores a value. `nil` removes it. It raises "Storage is full" when the keys and values of the script, as JSON text, would be more than 524,288 characters.
*   **`sf.storage.remove(key)`** removes a value.
*   **`sf.storage.keys()`** gives a list of the keys.
*   **`sf.storage.clear()`** removes everything.

The user can delete it with **Clear stored data** on the page of the script.

```lua
local count = sf.storage.get('count', 0) + 1
sf.storage.set('count', count)
```

## Settings

Settings are on the **Settings** screen of the page of the script.

```lua
settings = {
  { key = 'sayApp', type = 'switch', title = 'Say the app', default = true },
  { key = 'voice', type = 'list', title = 'Voice', options = { 'low', { value = 'hi', label = 'High' } } },
  { key = 'name', type = 'text', title = 'Your name', summary = 'Used in greetings' },
  { key = 'count', type = 'number', title = 'How many' },
  { key = 'reset', type = 'button', title = 'Reset counts' },
  { key = 'onUnlock', type = 'command', title = 'When unlocked', default = 'TOGGLE_TOUCH_LOCK',
    options = { { value = 'vibrate', label = 'Vibrate the time' } } },
},
```

| Field | Meaning |
|---|---|
| `key` | Required. Letters, digits, `.`, `-` and `_`, up to 64 characters, unique in the script. |
| `type` | `'switch'` (the default), `'list'`, `'text'`, `'number'`, `'button'` or `'command'` (*Version 7*). |
| `title` | Required. |
| `summary` | Text under the title. A list shows its chosen option instead, and a command setting its chosen command. |
| `default` | The value before the user changes it. If left out: `false` for a switch, the first option for a list, `''` for text, 0 for a number, `''` (no command) for a command setting. |
| `options` | For a list, required: values, or `{ value, label }` records. For a command setting, the script's own choices, in the same form. |

*Version 7.* A command setting lets the user choose a SandFly command in the list used to assign a command to a gesture or a volume key press, with its categories and its search. The list has the setting's own `options` first, then **Unassigned**, then the commands of [`sf.commands.list()`](#run-sandfly-commands). The value is the command's id, which [`sf.commands.run`](#run-sandfly-commands) takes, an option's value, or `''` for **Unassigned**, so an option's value can't be empty. `sf.commands.run` needs the `input` permission, and the permissions some commands need, such as `system`. A script handles its own options itself, as in this example:

```lua
local command = sf.settings.get('onUnlock')
if command == 'vibrate' then
  vibrateTime()
elseif command ~= '' then
  sf.commands.run(command)
end
```

A button has no value. Pressing it calls [`sf.onButton`](#hooks) with its key; while the script is off, SandFly shows "Turn the script on first".

*   **`sf.settings.get(key)`** gives the value of a setting. It raises an error for a key the manifest doesn't declare, or a button.
*   **`sf.settings.all()`** gives a record of all values, by key.
*   **`sf.settings.set(key, value)`** changes a value, up to 16,384 characters as JSON, and calls `onSettingChange`.
*   **`sf.settings.setOptions(key, options)`** replaces the options of a list: 1 to 500 options, up to 16,384 characters together.
*   **`sf.settings.setTitle(key, text)`** and **`sf.settings.setSummary(key, text)`** *Version 3* change the title or summary that a setting shows, such as "Slot 1: Rewind, YouTube" on a button, up to 500 characters. `nil` goes back to the text of the manifest.

## Dialogs and windows

These need the `dialogs` permission. The title of every dialog and window starts with the name of the script, as "*name*: *title*", so that it can't be taken for a dialog of the app or the system. Texts are cut at 20,000 characters.

*   **`sf.ui.alert(message, options)`** shows a message with an OK button, and waits until it closes. `options.title` is the title.
*   **`sf.ui.confirm(message, options)`** shows a message with OK and Cancel buttons, and gives `true` for OK, otherwise `false`. `options` has `title`, and `ok` and `cancel`, the texts of the buttons.
*   **`sf.ui.prompt(title, options)`** shows a text field, and gives the text entered, or `nil` if cancelled. `options` has `text`, the text to start with, and `hint`.
*   **`sf.ui.choose(title, choices, options)`** shows a list of 1 to 500 choices, values or `{ value, label }` records, and gives the chosen value, or `nil`. `options.selected` is the value chosen at first. A list of more than 10 options has a field that filters it.
*   **`sf.ui.text(title, text)`** *Version 2* shows text that can be read by line, word or character and selected, and waits until it closes. Use it for results too long to say.
*   **`sf.ui.dialog(form)`** shows a form, and gives it at once, without waiting.
*   **`sf.ui.window(form)`** *Version 2* does the same in a window that fills the screen, for a script that works as an app of its own, such as a notepad. Back closes it.

All but `dialog` and `window` [wait](#waiting). *Version 2:* dialogs and windows open over the ones already open, so a script can call `sf.ui.confirm` from a window and the window stays open under it. Up to 8 are open at once; opening another closes the oldest. They close when the script unloads, such as when its app leaves the screen, so keep what the user typed in [`sf.storage`](#storage).

### Forms

A form is `{ title, items, onClose }`. `items` is a list of 1 to 50 items; `onClose` is called with the values when the form closes. Each item has a `type`, an `id` (unique in the form; `item1`, `item2` and so on if left out), and usually a `label`. Any item can have `enabled = false` or `visible = false`.

| Type | Other fields |
|---|---|
| `label` | `label` is the text. *Version 2:* `selectable = true` makes it readable by character, word and line, and selectable. |
| `heading` | *Version 2.* A label that is a heading, for moving between parts of a window. |
| `edit` | `value`, `hint`, `number`, `password`, `multiline`, `onChange`. *Version 2:* `readOnly = true` holds text to read and select, not to change; `onSubmit` is called with the text when Done or Enter on the keyboard is pressed in a field of one line. |
| `switch` | `value`, `onChange`. |
| `list` | `items`, values or `{ value, label }`; `value`, the chosen value; `onChange`. One item can be chosen. |
| `items` | *Version 2.* Rows to open, such as the notes of a notepad: `items`, up to 500 values or `{ value, label }`; `empty`, the text shown when there are none; and `actions`, names of actions on each row, in the SandFly **Actions** menu. `onSelect` is called with the value of a row when it is double-tapped, `onLongPress` when it is long-pressed, and `onAction` with the action and the value of the row. |
| `slider` | *Version 2.* `min`, `max` and `step`, 0, 100 and 1 if left out, up to 10,000 steps; `value`, `min` if left out; `onChange`. |
| `button` | `onClick`; `close = true` closes the form after it. |

`onChange` is called with the new value and the form, `onClick` with the form, `onSelect`, `onLongPress` and `onSubmit` with the value and the form, and `onAction` with the action, the value and the form. All of them run as tasks and can [wait](#waiting).

A form has these methods:

*   `form:get(id)` gives the value of an item. `form:values()` gives all values, by id.
*   `form:set(id, changes)` changes an item: any of `value`, `label`, `hint`, `items`, `actions`, `empty`, `min`, `max`, `step`, `enabled` and `visible`.
*   `form:close()` closes the form. `form.closed` is `true` once it has closed.
*   `form:wait()` [waits](#waiting) until the form closes, and gives the values.
*   `form:add(item, options)` *Version 2* adds an item at the end, or after the item with the id `options.after`, and gives its id.
*   `form:remove(id)` *Version 2* removes an item.
*   `form:focus(id)` *Version 2* moves SandFly focus to an item, or to the first row of an `items` list.
*   `form:setTitle(title)` *Version 2* changes the title.

```lua
-- In a command: ask for a number of minutes.
local form = sf.ui.dialog({
  title = 'Timer',
  items = {
    { type = 'slider', id = 'minutes', label = 'Minutes', min = 1, max = 60, value = 5 },
    { type = 'button', id = 'start', label = 'Start', close = true },
  },
})
local values = form:wait()
```

[examples/notepad.lua](scripting/examples/notepad.lua) is a notepad: a window lists the notes, double-tapping one opens it in a window of its own, and the Actions menu copies or deletes one.

## Clipboard

These need the `clipboard` permission.

*   **`sf.clipboard.get()`** gives the text on the clipboard, or `nil`.
*   **`sf.clipboard.set(text)`** puts text on the clipboard.

## System

These need the `system` permission.

**`sf.system.back()`**, **`home()`**, **`recents()`**, **`notifications()`**, **`quickSettings()`**, **`powerDialog()`**, **`lockScreen()`**, **`takeScreenshot()`** and **`allApps()`** perform the system action and, in a task, give whether it worked. `takeScreenshot()` takes the Android screenshot; the image doesn't reach the script.

**`sf.system.openApp(packageName)`** opens an app and, in a task, gives whether it opened.

**`sf.system.apps()`** *Version 2* gives a list of the apps with a launcher icon, `{ package, label }`, by name.

**`sf.system.shizuku()`** *Version 7* gives whether SandFly can run commands through Shizuku, without asking the user anything: `'ready'`, `'not running'` or `'not allowed'`. With **Use root instead of Shizuku** on, `'ready'` means that su gave SandFly root, and `'not allowed'` that it hasn't yet. The Google Play build without the SandFly add-on gives `'not running'`. A script can use what needs Shizuku, such as `sf.keys.press`, when it gives `'ready'`, and do the work another way otherwise.

```lua
if sf.system.shizuku() == 'ready' then
  sf.keys.press('enter')
else
  -- Press the app's Send button instead.
end
```

**`sf.system.openUrl(url)`** *Version 2* opens an `http`, `https`, `mailto`, `tel`, `sms`, `smsto`, `geo` or `market` address in the app for it, and gives whether an app opened it.

**`sf.system.share(text, options)`** *Version 2* opens the Android share sheet with the text. `options` has `title`, the title of the share sheet, and `subject`.

**`sf.system.openSettings(page, packageName)`** *Version 2* opens a page of Android settings: `settings` (the default), `wifi`, `bluetooth`, `mobileData`, `airplaneMode`, `nfc`, `location`, `sound`, `display`, `battery`, `batterySaver`, `accessibility`, `apps`, `defaultApps`, `notifications`, `doNotDisturb`, `dateTime`, `language`, `keyboard`, `storage`, `security`, `privacy`, `developer`, `about`, `castScreen` or `hotspot`, or, with a package name, `app` or `appNotifications` for one app. It gives whether the page opened.

**`sf.system.startActivity(intent)`** and **`sf.system.sendBroadcast(intent)`** *Version 2* send an intent: `{ action, data, type, package, className, categories, extras }`. An intent needs `action` or `className`, and `className` needs `package`. `extras` holds text, numbers, `true` and `false`. A broadcast needs `package`. Neither can reach SandFly. `startActivity` gives whether an app opened it.

```lua
sf.system.startActivity({
  action = 'android.intent.action.VIEW',
  data = 'https://example.com',
  package = 'com.android.chrome',
})
```

*Version 6.* These go through the app that handles them, and give whether an app took the request:

*   **`sf.system.setTimer(seconds, label)`** starts a timer of 1 to 86,400 seconds in the clock app, without opening it where the app allows.
*   **`sf.system.setAlarm(hour, minute, label)`** sets an alarm in the clock app, without opening it where the app allows.
*   **`sf.system.call(number)`** calls a number, or opens the dialer with it when SandFly isn't allowed to make calls.
*   **`sf.system.message(number, text)`** opens the messaging app with a message to send.
*   **`sf.system.camera(video)`** opens the camera, for a video when `video` is `true`.

**`sf.resume()`** resumes SandFly when it is paused.

## Network

These need the `network` permission.

**`sf.fetch(url, options)`** [waits](#waiting) for a request to an `https` address, and gives the response. `options` has any of:

*   `method`: `GET` (the default), `POST`, `PUT`, `DELETE`, `HEAD` or `OPTIONS`.
*   `headers`: a record of header names and values.
*   `body`: text, or a table, which is sent as JSON with `Content-Type: application/json` unless `headers` set another type. Up to 1 MB.
*   `timeout`: milliseconds to connect and to read, 30,000 if left out, at most 60,000.

The response has `status`, `statusText`, `ok` (`true` for a status from 200 to 299), `url`, `headers` (with lowercase names; several values of one header are joined with `, `), `body` (text), `response:json()`, which decodes the body, and `response:text()`. A response can be 5 MB. A script can have 8 requests going at once. A status such as 404 gives a response; a failed connection raises an error that starts with "Network error".

```lua
-- In a command, a hook or sf.run.
local ok, response = pcall(sf.fetch, 'https://api.example.com/items', {
  method = 'POST',
  body = { name = 'Test' },
})
if ok and response.ok then
  local data = response:json()
end
```

### Translating text

*Version 2.* **`sf.translate(text, options)`** [waits](#waiting), and gives `{ text, from, to }`, translated with the service chosen in **Translation** in SandFly settings. `options.from` and `options.to` are language tags such as `'fr'`. Without `from`, the service finds the language; without `to`, the text is translated into the language chosen there. It raises an error if the service isn't set up or fails. With on-device translation, it waits for the languages to download. Needs `network`.

## Device

*Version 2.* Reading these needs no permission.

*   **`sf.device.info()`** gives `{ manufacturer, model, androidVersion, sdk, sandflyVersion, screen }`, where `screen` is `{ width, height, density, orientation }` and `orientation` is `'portrait'` or `'landscape'`.
*   **`sf.device.battery()`** gives `{ level, charging, full, plugged }`. `level` is a percentage, and `plugged` is `'ac'`, `'usb'`, `'wireless'`, `'dock'` or `nil`.
*   **`sf.device.network()`** gives `{ connected, wifi, cellular, ethernet, vpn, metered }`.
*   **`sf.device.volume(stream)`** gives `{ level, min, max, muted }` for `'media'` (the default), `'ring'`, `'notification'`, `'alarm'`, `'call'`, `'system'` or `'accessibility'`.
*   **`sf.device.ringerMode()`** gives `'normal'`, `'vibrate'` or `'silent'`.

These need the `system` permission:

*   **`sf.device.setVolume(stream, level)`** sets the volume of a stream, kept between `min` and `max`.
*   **`sf.device.setRingerMode(mode)`** sets the ringer mode. Android refuses some changes while Do Not Disturb is on, and the call raises an error then.
*   **`sf.device.media(key)`** *Version 6* presses a media key for the app that plays media: `'playPause'`, `'play'`, `'pause'`, `'next'`, `'previous'`, `'stop'`, `'rewind'` or `'fastForward'`.

### Device controls

*Version 6.* These reach the controls of the SandFly quick settings panel by name: `wifi`, `bluetooth`, `mobileData`, `hotspot`, `airplaneMode`, `dataSaver`, `doNotDisturb`, `soundMode`, `flashlight`, `flashlightBrightness`, `location`, `autoRotate`, `batterySaver`, `darkTheme`, `nightLight`, `extraDim`, `colorInversion`, `colorCorrection`, `nfc`, `microphoneAccess`, `cameraAccess`, `brightness`, `adaptiveBrightness`, `screenTimeout`, `fontSize`, `displaySize`, and the volumes `mediaVolume`, `callVolume`, `ringVolume`, `notificationVolume`, `alarmVolume`, `accessibilityVolume` and `systemVolume`.

*   **`sf.device.controls()`** gives a list of the names of the controls this device has.
*   **`sf.device.get(name)`** reads one: `true` or `false` for a switch, `{ value, min, max }` for a slider, `{ value, label, options }` for a choice, with `min` and `max` for a choice that also takes any number between them, or `nil` when the device doesn't have it or it can't be read.
*   **`sf.device.set(name, value)`** [waits](#waiting) and changes one: a switch to `true`, `false`, `'on'`, `'off'` or `'toggle'`; a slider to a number, a percent of its range such as `'50%'`, or `'+1'` or `'-1'` steps; a choice to the value or label of an option, or `'next'`. It gives `true`, and raises an error with the reason when the control can't be changed, such as Wi-Fi without Shizuku, or a setting whose permission SandFly doesn't have, when the Android page for it opens. Needs `system`.

`fontSize` also takes any percent from 25 to 200, or to 130 before Android 14, such as `50` or `'50%'`, including sizes below the 85% that Android settings offer. `displaySize` is the display size as a percent of the default density, as the Android Display size setting changes it. It takes any percent from `min`, which keeps the smaller side of the screen under 600 dp, where apps change to their tablet layouts, to `max`, which keeps it at least 320 dp. Setting it needs Shizuku. Some apps change their layout at a smaller size, so rules that match an activity or the items of a list may stop matching.

```lua
sf.run(function()
  local ok, why = pcall(sf.device.set, 'flashlight', 'toggle')
  if not ok then sf.speak(why) end
end)
```

## Translations

Put a JSON file for each language in the `locales` folder of the zip, named by language tag: `locales/de.json`, `locales/pt-BR.json`. Each maps the English text to the translation. A text with plural forms maps to a record of forms: `zero`, `one`, `two`, `few`, `many` and `other`, as the language uses them.

```json
{
  "Next unread": "Nächste ungelesene",
  "{count} message": { "one": "{count} Nachricht", "other": "{count} Nachrichten" }
}
```

SandFly reads the file of the device language, and of the language without its region, with the more specific file winning. It translates the name and description of the script, the titles, summaries and option labels of settings, the labels, speech, states, hints and action titles of rules, and the titles of commands and navigation entries. In code:

*   **`sf._(text, params)`** gives the translation of `text`, or `text`, with `{name}` placeholders filled from `params`.
*   **`sf.ngettext(singular, plural, count, params)`** gives the plural form for `count` in the language, with `{count}` and the other placeholders filled. The translation is looked up by `singular`.
*   **`sf.locale()`** gives the language tag of the device, such as `'de-DE'`. **`sf.language()`** gives the tag that translations are read for.

```lua
local n = 3
sf.speak(sf.ngettext('{count} message', '{count} messages', n)) -- '3 messages'
```

## Text

*Version 6.* These need no permission. Regular expressions are Java regular expressions, found ignoring case, as in macros. Repeated groups can't contain repetition or `|`. Texts are cut at 10,000 characters.

*   **`sf.text.replace(text, pattern, with)`** replaces every match of `pattern`. `with` can use `$1` for the first group.
*   **`sf.text.find(text, pattern)`** gives whether `pattern` is found in the text.
*   **`sf.text.edit(text, edits)`** applies the edits of the speech changes of the script builder, in order, and gives `{ text = ... }` with the new text, `{ silent = true }` when nothing is to be said, or `nil` when nothing changed. A speech hook returns `result.text`, or `false` for `{ silent = true }`. Each edit is a record with `op`:
    *   `remove` and `replace`: remove or replace `find`; with `regex = true`, `find` is a regular expression and `with` can use `$1`.
    *   `removeRepeats`: removes repeated parts. Parts are the pieces between commas.
    *   `removeParts`: removes the parts that contain `find` (or match it, with `regex = true`).
    *   `keepParts`: keeps the first `count` parts.
    *   `say`, `before` and `after`: say `text` instead, before or after; `{text}` in it is what SandFly would say.
    *   `silence`: say nothing.

```lua
sf.onNotification(function(n, said)
  local result = sf.text.edit(said, {
    { op = 'remove', find = 'new message from' },
    { op = 'keepParts', count = 2 },
  })
  if result == nil then return nil end -- unchanged
  if result.silent then return false end
  return result.text
end)
```

## Logs, timers, files and JSON

*   **`print(...)`** and **`sf.log(...)`** write their arguments, joined by spaces, to the **Log** of the script at the info level. **`sf.log.debug`**, **`sf.log.info`**, **`sf.log.warn`** and **`sf.log.error`** write at a level. Tables without a metatable are written as JSON. See [Testing and debugging](#testing-and-debugging).
*   **`sf.setTimeout(fn, ms, ...)`** runs `fn` once after `ms` milliseconds, with the other arguments, and **`sf.setInterval(fn, ms, ...)`** runs it every `ms` milliseconds, at least 10. Both give a number for **`sf.clearTimer(number)`**. A script can have 100 timers at once. Timers run as tasks.
*   **`sf.sleep(ms)`** [waits](#waiting) for `ms` milliseconds.
*   **`sf.run(fn, ...)`** runs a function as a task, so that it can wait.
*   **`sf.clock()`** *Version 2* gives a count of milliseconds that only goes up, for timing. `os.time()` and `os.date()` give the date and time.
*   **`sf.apiVersion`** is the script API version of this SandFly.
*   **`sf.manifest()`** gives the manifest of the script.

### Lua and files

*   **`require(name)`** loads another Lua file of the package, once, and gives what it returns: `require('lib.words')` loads `lib/words.lua`. Names are letters, digits, `_`, `-` and dots.
*   **`sf.readFile(path)`** *Version 2* gives the text of a file of the package, such as `'data/words.txt'`, up to 1 MB, or `nil` if there is none.
*   **`json.encode(value)`** and **`json.decode(text)`**, also as `sf.json`, convert between Lua values and JSON text. An empty table is encoded as `[]`, a table with only listed values as a list, and `json.null` stands for `null`. `json.decode` gives `nil` for `null`.

## Gestures

These are the names for `gesture` in commands and `sf.bind`.

`swipeUp`, `swipeDown`, `swipeLeft`, `swipeRight`, `swipeUpThenDown`, `swipeDownThenUp`, `swipeLeftThenRight`, `swipeRightThenLeft`, `swipeUpThenLeft`, `swipeUpThenRight`, `swipeDownThenLeft`, `swipeDownThenRight`, `swipeLeftThenUp`, `swipeLeftThenDown`, `swipeRightThenUp`, `swipeRightThenDown`, `doubleTap`, `doubleTapAndHold`, `twoFingerTap`, `twoFingerDoubleTap`, `twoFingerTripleTap`, `twoFingerTapAndHold`, `twoFingerDoubleTapAndHold`, `twoFingerTripleTapAndHold`, `twoFingerSwipeUp`, `twoFingerSwipeDown`, `twoFingerSwipeLeft`, `twoFingerSwipeRight`, `twoFingerSwipeUpThenLeft`, `twoFingerSwipeUpThenRight`, `twoFingerSwipeDownThenLeft`, `twoFingerSwipeDownThenRight`, `twoFingerSwipeLeftThenUp`, `twoFingerSwipeLeftThenDown`, `twoFingerSwipeRightThenUp`, `twoFingerSwipeRightThenDown`, `twoFingerRotateClockwise`, `twoFingerRotateCounterclockwise`, `threeFingerTap`, `threeFingerDoubleTap`, `threeFingerTripleTap`, `threeFingerQuadrupleTap`, `threeFingerTapAndHold`, `threeFingerDoubleTapAndHold`, `threeFingerTripleTapAndHold`, `threeFingerSwipeUp`, `threeFingerSwipeDown`, `threeFingerSwipeLeft`, `threeFingerSwipeRight`, `fourFingerTap`, `fourFingerDoubleTap`, `fourFingerTripleTap`, `fourFingerDoubleTapAndHold`, `fourFingerSwipeUp`, `fourFingerSwipeDown`, `fourFingerSwipeLeft`, `fourFingerSwipeRight`, `fingerprintSwipeUp`, `fingerprintSwipeDown`, `fingerprintSwipeLeft` and `fingerprintSwipeRight`.

SandFly gestures, which SandFly recognizes only while a command or a script uses them: `tripleTap`, `quadrupleTap`, `edgeSwipeUpFromBottom`, `edgeLongSwipeUpFromBottom`, `edgeSwipeDownFromTopLeft`, `edgeLongSwipeDownFromTopLeft`, `edgeSwipeDownFromTopRight`, `edgeLongSwipeDownFromTopRight`, `edgeSwipeRightFromLeftTop`, `edgeLongSwipeRightFromLeftTop`, `edgeSwipeRightFromLeftBottom`, `edgeLongSwipeRightFromLeftBottom`, `edgeSwipeLeftFromRightTop`, `edgeLongSwipeLeftFromRightTop`, `edgeSwipeLeftFromRightBottom`, `edgeLongSwipeLeftFromRightBottom`, `edgeSwipeUpAlongLeft`, `edgeSwipeDownAlongLeft`, `edgeSwipeUpAlongRight`, `edgeSwipeDownAlongRight`, `edgeSwipeLeftAlongTop`, `edgeSwipeRightAlongTop`, `edgeSwipeLeftAlongBottom`, `edgeSwipeRightAlongBottom`, `cornerTapTopLeft`, `cornerDoubleTapTopLeft`, `cornerTripleTapTopLeft`, `cornerDoubleTapAndHoldTopLeft`, `cornerTripleTapAndHoldTopLeft`, `cornerTapTopRight`, `cornerDoubleTapTopRight`, `cornerTripleTapTopRight`, `cornerDoubleTapAndHoldTopRight`, `cornerTripleTapAndHoldTopRight`, `cornerTapBottomLeft`, `cornerDoubleTapBottomLeft`, `cornerTripleTapBottomLeft`, `cornerDoubleTapAndHoldBottomLeft`, `cornerTripleTapAndHoldBottomLeft`, `cornerTapBottomRight`, `cornerDoubleTapBottomRight`, `cornerTripleTapBottomRight`, `cornerDoubleTapAndHoldBottomRight`, `cornerTripleTapAndHoldBottomRight`, `twoFingerTapThenSwipeUp`, `twoFingerTapThenSwipeDown`, `twoFingerTapThenSwipeLeft`, `twoFingerTapThenSwipeRight`, `twoFingerDoubleTapThenSwipeUp`, `twoFingerDoubleTapThenSwipeDown`, `twoFingerDoubleTapThenSwipeLeft`, `twoFingerDoubleTapThenSwipeRight`, `twoFingerTripleTapThenSwipeUp`, `twoFingerTripleTapThenSwipeDown`, `twoFingerTripleTapThenSwipeLeft`, `twoFingerTripleTapThenSwipeRight`, `threeFingerTapThenSwipeUp`, `threeFingerTapThenSwipeDown`, `threeFingerTapThenSwipeLeft`, `threeFingerTapThenSwipeRight`, `threeFingerDoubleTapThenSwipeUp`, `threeFingerDoubleTapThenSwipeDown`, `threeFingerDoubleTapThenSwipeLeft`, `threeFingerDoubleTapThenSwipeRight`, `threeFingerTripleTapThenSwipeUp`, `threeFingerTripleTapThenSwipeDown`, `threeFingerTripleTapThenSwipeLeft`, `threeFingerTripleTapThenSwipeRight`, `fourFingerTapThenSwipeUp`, `fourFingerTapThenSwipeDown`, `fourFingerTapThenSwipeLeft`, `fourFingerTapThenSwipeRight`, `fourFingerDoubleTapThenSwipeUp`, `fourFingerDoubleTapThenSwipeDown`, `fourFingerDoubleTapThenSwipeLeft`, `fourFingerDoubleTapThenSwipeRight`, `fourFingerTripleTapThenSwipeUp`, `fourFingerTripleTapThenSwipeDown`, `fourFingerTripleTapThenSwipeLeft`, `fourFingerTripleTapThenSwipeRight`, `twoFingerTapThenOneFingerSwipeUp`, `twoFingerTapThenOneFingerSwipeDown`, `twoFingerTapThenOneFingerSwipeLeft`, `twoFingerTapThenOneFingerSwipeRight`, `twoFingerDoubleTapThenOneFingerSwipeUp`, `twoFingerDoubleTapThenOneFingerSwipeDown`, `twoFingerDoubleTapThenOneFingerSwipeLeft`, `twoFingerDoubleTapThenOneFingerSwipeRight`, `twoFingerTripleTapThenOneFingerSwipeUp`, `twoFingerTripleTapThenOneFingerSwipeDown`, `twoFingerTripleTapThenOneFingerSwipeLeft`, `twoFingerTripleTapThenOneFingerSwipeRight`, `twoFingerPinch`, `twoFingerSpread`, `threeFingerPinch`, `threeFingerSpread`, `fourFingerPinch`, `fourFingerSpread`, `twoFingerScrub`, and the swipe-and-hold gestures `swipeUpAndHold`, `swipeDownAndHold`, `swipeLeftAndHold`, `swipeRightAndHold`, `swipeUpThenDownAndHold`, `swipeDownThenUpAndHold`, `swipeLeftThenRightAndHold`, `swipeRightThenLeftAndHold`, `swipeUpThenLeftAndHold`, `swipeUpThenRightAndHold`, `swipeDownThenLeftAndHold`, `swipeDownThenRightAndHold`, `swipeLeftThenUpAndHold`, `swipeLeftThenDownAndHold`, `swipeRightThenUpAndHold` and `swipeRightThenDownAndHold`, and the three-part swipes, named in the same way, such as `swipeRightThenUpThenRight` and `swipeUpThenDownThenUp`, and the two-stroke swipes, such as `swipeRightLiftSwipeUp`, `swipeDownLiftSwipeDown`, `swipeUpThenLeftLiftSwipeRight` and `swipeUpLiftSwipeRightThenDown`, which also need **Time between two-stroke swipes** set. Slides along an edge that is an edge menu go to the menu instead, and so do slides in from that edge while its menu goes across the screen, with **Slide in from the edge** on.

## Testing and debugging

### The log

**Log**, on the page of the script, lists what the script wrote with `print` and `sf.log`, every error, and warnings from SandFly, newest first, with the time and level of each. It keeps the last 300 entries of each script, up to 4,000 characters each, from the time SandFly started. **Copy log** copies it, oldest first; **Clear log** empties it.

SandFly also writes to the log:

*   "Loaded for *package*" each time the script loads, at the debug level.
*   A warning, once, for each hook, rule, action or assignment skipped for a missing permission, such as "onFocus needs the screen permission, so it is not used".
*   A warning for each rule action whose item isn't found, and, once, for each speech hook that takes longer than the speech deadline.
*   A warning when the API versions of the manifest don't fit this SandFly.

Log entries reach the Android system log, with the tag `BacktalkScript`, only at the level set in **Developer settings** > **Log output level**. They can hold what is on screen, so the log of the script always has them and the system log doesn't by default.

### Errors

An error in a hook, command, timer or callback goes to the log with where it happened, such as `onFocus:` or `command unread:`, the message and a traceback. The script stays loaded, and other hooks keep running. An error while `main.lua` runs as the script loads starts with `Loading:`, and the script isn't loaded.

The import refuses a script whose manifest SandFly can't read, and shows why, such as "The manifest needs author" or "Unknown rule fields: lable".

SandFly turns a script off after 5 errors in a minute, or after it runs out of time 3 times in 5 minutes, says "*name* turned off after repeated errors. See its log in Scripts.", and writes "Turned off after repeated errors" to the log. It also turns off a script when one call holds up all scripts for more than 20 seconds, and starts the other scripts again. Turn the script on again on its page after fixing it.

### Reload a script

*   Import the file again with **Import script**. The new code replaces the old, and stored data, settings and permission choices stay.
*   Change the code with **Edit code** on the page of the script, and select **Save**. The same parts stay as with **Import script**.
*   Turn the script off and on with **Turn on** on its page. The script loads again, and its variables start again.
*   A script with `apps` loads again each time its app comes to the front.

### Tips

*   Before you try a script you don't trust yet, assign **Turn all scripts on or off** to a gesture. Scripts can't take the gesture it is assigned to. **Scripts** > **Turn off all scripts** does the same.
*   Use **Inspect focused item**, **Copy item details** and **Copy screen tree** to find ids and texts for queries. See [Find the details of an item](#find-the-details-of-an-item).
*   Use `print` with tables to see what a hook gets: `sf.onScroll(function(e) print(e) end)` writes the event as JSON.
*   Wrap calls that can fail, such as `sf.fetch` and `sf.device.set`, in `pcall`.
*   Keep speech hooks short. Do the slow parts in `onAppEnter` or a timer, and keep the results in variables.

## Limits

*   A script has 2 seconds to load its main file, and 2 seconds for each call into it, such as a hook or a command. Speech hooks share 30 milliseconds. `onActions` has 250 milliseconds, and its actions reach the menu only if it answers within 50. Searching the screen counts toward that time, and a search stops when the time is up.
*   A task that waits doesn't use its time while it waits; each part between waits has 2 seconds.
*   A script can use 32 MB of memory. `string.rep` can make text up to 8 MB.
*   SandFly checks the time and memory every 10,000 Lua instructions, and stops the call with an error that `pcall` can't catch. A single library call, such as a pattern match on long text with several `.-` or `.*`, runs to its end, so keep patterns simple on long text.
*   A manifest can have up to 500 rules, 100 of them with `match`, 50 commands and 20 navigation entries. A rule can have up to 10 actions, an action up to 10 steps, and `read` up to 12 parts.
*   A script can add 200 rules, have 100 timers, 8 requests, 8 dialogs and windows, and 2 text recognitions at once, and keep 524,288 characters in storage.
*   Text for speech and dialogs is cut at 20,000 characters, braille text at 1,000, log messages at 4,000, and texts for `sf.text` at 10,000. Keys in `sf.storage` are 1 to 200 characters; keys of settings and ids of commands up to 64.
*   A `.lua` file is up to 1 MB; a `.zip` file up to 5 MB, 5 MB unpacked, and 200 files. `sf.readFile` and macro files read up to 1 MB.

## Publish a script

To share a script, put the `.lua` or `.zip` file in a public repository on GitHub, GitLab, Codeberg, Gitea (gitea.com) or Bitbucket. Users add the repository, or a branch or folder in it, as a script source in **Scripts** > **Script sources** > **Add source**:

*   `github.com/owner/repository` or `github.com/owner/repository/tree/main/scripts`
*   `gitlab.com/group/repository` or `gitlab.com/group/subgroup/repository/-/tree/main/scripts`
*   `codeberg.org/owner/repository` or `codeberg.org/owner/repository/src/branch/main/scripts`
*   `gitea.com/owner/repository`, written as for Codeberg
*   `bitbucket.org/workspace/repository` or `bitbucket.org/workspace/repository/src/main/scripts`

A source lists up to 200 `.lua` and `.zip` files directly in that folder. A user can also install a script from an `https` link to the file.

SandFly checks installed scripts for a newer `version` once a day, in the source the script was installed from and by the same file name. A script installed without a known source is found by a file named after its `id`, such as `my-script.lua`. With **Update scripts automatically** on, SandFly installs a newer version without asking when it has the same `author`, needs no new permissions, fits the API version of this SandFly, and comes from the source of the script. Other newer versions are listed under **Updates** on the **Scripts** screen, and install after the install dialog, which lists new permissions and warns when the author changed.

SandFly signs the scripts in its own repository, [aaron-gh/sandfly-scripts](https://github.com/aaron-gh/sandfly-scripts). The signature is the last line of `main.lua`, starting with `-- SandFly signature:`, and covers every file of the script. The install dialog shows **Signed by SandFly.** for a signed script, that the signature doesn't match for a signed script changed after signing, and **Not signed by SandFly.** for other scripts. A newer version from the SandFly repository installs without asking only if SandFly signed it.

## The script builder

**Scripts** > **New script** makes a script without code, with apps, rules, reading controls, commands, triggers, speech changes, settings and Lua code. The [Scripts and custom labels](user-guide/en/20-scripts.md) chapter of the SandFly User Guide describes it, in **Build a script**. For script writers:

*   A built script is an ordinary script. The builder writes `main.lua` with the manifest, a line of Lua for each command, trigger and speech change, and the text of **Code** > **Lua code** at the end. It keeps the steps of each command and trigger in a file in `macros/`, run with [`sf.macros.run`](#macros).
*   The builder adds the permissions its parts need to the manifest. **Code** > **Extra permissions** adds others, such as `java`. Lua code is checked for syntax errors before it is saved.
*   A change that makes a manifest SandFly refuses, such as rules in a script loaded on demand, isn't saved, and SandFly shows why.
*   **Copy to script builder**, on the page of a script not made in the builder, makes a built copy, with its manifest as parts of the builder, its code as Lua code, and its other files.
*   **Export script** saves a built script as a `.zip` file, which **Import script** installs, still editable in the builder.
*   **Build a script with SandFly Pro** and **Change with SandFly Pro** write a built script from a description. Until the user selects **Accept**, the script runs without the `network` and `java` permissions.

## Example scripts

The examples are in [docs/scripting/examples](scripting/examples):

*   [example-plugin.lua](scripting/examples/example-plugin.lua): says the name of each app you switch to, and counts how often each app opens. It shows `onAppEnter`, `sf.storage` and switch settings. It comes with SandFly: **Scripts** > **Add the example script** installs it.
*   [notepad.lua](scripting/examples/notepad.lua): a notepad in SandFly windows, opened from the SandFly menu or with the SandFly modifier and N. It shows `sf.ui.window`, `items` lists with actions, `sf.ui.confirm` over a window, and storage.
*   [wikipedia-search.lua](scripting/examples/wikipedia-search.lua): searches Wikipedia and opens the chosen article. It shows a script loaded on demand, `sf.ui.prompt`, `sf.fetch` with JSON, `sf.ui.choose` and `sf.system.openUrl`.
*   [talking-clock.lua](scripting/examples/talking-clock.lua): says the time every hour or half hour, and on a command. It shows `keepLoaded`, list settings, `onSettingChange` and timers.
