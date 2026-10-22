# Writing SandFly scripts

A script is a Lua file that changes how SandFly works in one app, or in every app. This document describes the whole script API, version 1.

Scripts run on phones and tablets. Watches and TVs can't run them.

## What a script is

A script is one of these:

*   A `.lua` file, up to 1 MB.
*   A `.zip` file with a `main.lua` in it, plus anything `main.lua` uses: other Lua files, sounds, and translations. The zip can be up to 5 MB, up to 5 MB unpacked, and hold up to 200 files.

To install a script, go to **Apps** > **Scripts** in SandFly settings and choose **Import script**. SandFly lists what the script asks to do before it installs it. Importing a file with the id of a script that's already installed updates that script and keeps its data and settings; SandFly always asks first, and says so if the author changed.

To find scripts, choose **Script sources** on the Scripts screen and **Add source**. A source is the address of a repository, or of a branch or folder in one, on GitHub, GitLab, Codeberg, Gitea (gitea.com) or Bitbucket:

*   `github.com/owner/repository` or `github.com/owner/repository/tree/main/scripts`
*   `gitlab.com/group/repository` or `gitlab.com/group/subgroup/repository/-/tree/main/scripts`
*   `codeberg.org/owner/repository` or `codeberg.org/owner/repository/src/branch/main/scripts`
*   `gitea.com/owner/repository`, written as for Codeberg
*   `bitbucket.org/workspace/repository` or `bitbucket.org/workspace/repository/src/main/scripts`

Only public repositories on these services work. The source's `.lua` and `.zip` files are listed. Choosing one downloads it and shows the same install dialog as **Import script**. **Remove source** on a source's screen removes it from the list; scripts already installed from it stay installed.

SandFly checks installed scripts for a newer `version` in the source they came from once a day, and **Check for updates** on the Scripts screen checks now. With **Update scripts automatically** on, a newer version is installed without asking when it is by the same author, asks for no new permissions, is made for this SandFly, and comes from the script's own source; scripts installed before SandFly kept their source are updated this way only from SandFly's own scripts. Any other newer version is listed under **Updates** on the Scripts screen, and installs after the usual dialog. A script is found in a source by the file it came from, or, if SandFly doesn't know it, by a file named after its `id`.

Scripts run in [Lua 5.4](https://www.lua.org/manual/5.4/) through [LuaJava](https://github.com/gudzpoz/luajava). A script has the `string`, `table`, `math`, `utf8` and `coroutine` libraries, `os.time`, `os.clock`, `os.date` and `os.difftime`, `json` for JSON text, and the `sf` table described below. It has no `io`, `debug` or `package`, no access to files, and loads only Lua text, not compiled Lua. Unless it has the [`java` permission](#java), it can't reach Android or Java; everything goes through `sf`.

## A first script

```lua
local manifest = {
  id = 'hello',
  name = 'Hello',
  version = '1.0',
  author = 'You',
  minApiVersion = 1,
  testedApiVersion = 1,
  apps = { 'com.android.settings' },
  permissions = { 'speech' },
}

sf.onAppEnter(function()
  sf.speak('Hello from a script')
end)
```

[examples/example-plugin.lua](scripting/examples/example-plugin.lua) is a longer one. It also comes with SandFly: choose **Add the example script** on the Scripts screen.

## The manifest

`main.lua` must contain `local manifest = { ... }` before its code. SandFly reads it without running the script, so it can only hold plain values: text, numbers, `true`, `false` and tables. No variables, function calls or `..`.

A table with only listed values, such as `{ 'speech', 'screen' }`, is a list. A table with only named fields, such as `{ id = 'x' }`, is a record. A table can't have both.

| Field | Meaning |
|---|---|
| `id` | Required. Lowercase letters, digits, dots, dashes and underscores, starting with a letter or digit, up to 64 characters. A script with the same id replaces the installed one. |
| `name` | Required. Shown in the Scripts list, and at the start of the title of every dialog the script shows. |
| `author` | Required. |
| `minApiVersion` | Required. The lowest script API version the script works with. This document describes version 1. |
| `testedApiVersion` | Required. The highest script API version you tested with. SandFly warns before installing a script tested only with an older version. |
| `version` | The script's own version, as text, such as `'1.2'`. The default is `'1'`. SandFly updates installed scripts from their source when this is higher, compared number by number, so raise it with each change. |
| `description` | Shown on the script's page and before it's installed. |
| `homepage` | An `http` or `https` address. The script's page has a button that opens it. |
| `apps` | Where the script runs. Leave it out for a script that runs in every app. See [Where a script runs](#where-a-script-runs). |
| `permissions` | A list of the [permissions](#permissions) the script needs. |
| `settings` | [Settings](#settings) the user can change on the script's page. |
| `rules` | [Rules](#rules) that change items without running any code. |
| `commands` | [Commands](#commands) the user can run with a gesture, keys, the SandFly menu or reading controls. |
| `navigation` | [Reading controls](#navigation) that move between items of a kind. |

### Where a script runs

`apps` is a package name, or a list. Each entry is a package name or a record:

```lua
apps = {
  'com.example.mail',
  { package = 'com.example.chat', activity = '.ConversationActivity' },
  { package = 'com.example.chat', window = 'Settings' },
},
```

*   `activity` limits the script to one screen. It's the activity's full class name, or the end of it. SandFly learns the activity from the app's window changes. Until it has seen one, such as just after SandFly is updated or the phone restarts with the app open, the script runs in all of the app's screens.
*   `window` limits it to windows whose title contains the text.

A script with `apps` is loaded when one of its apps comes to the front and unloaded when it leaves, so anything it keeps in variables is lost then. Use [`sf.storage`](#storage) for what should last.

A script without `apps` runs in every app and is loaded as long as it's turned on.

Scripts never change SandFly's own screens. Rules, speech changes, gestures, keys, menu items and reading controls from scripts don't apply there, and a script can't name SandFly in `apps`. While a SandFly screen is in front, calls that need the `screen`, `actions`, `system` or `dialogs` permission raise an error, `sf.speak` doesn't interrupt, and scripts get no events from SandFly itself. This is so that **Apps** > **Scripts** always works, whatever a script does. A script with the `java` permission can get past all of this.

Scripts are also unloaded while the screen is off, while SandFly is paused, and while **Turn off all scripts** is on. Timers and requests end when a script is unloaded.

### Permissions

A script can only use what its manifest asks for. The user sees the list before installing, and can turn each permission off on the script's page. A call that needs a permission the script doesn't have raises an error; a hook or rule that needs one is skipped, with a warning in the script's log.

| Permission | What it allows |
|---|---|
| `screen` | Reading the screen: `sf.screen`, the items given to hooks, commands and actions, and the `onFocus`, `onAnnouncement`, `onTextChange` and `onContentChange` hooks. A script for all apps also needs it to learn which app is in front: `sf.app()`, `onAppEnter`, `onAppLeave` and `onWindowChange`. |
| `actions` | Clicking, scrolling, typing and moving focus: `item:click()`, `item:perform()`, `item:focus()`, `item:setText()`, and rule actions other than `speak`. |
| `speech` | Speaking with `sf.speak`, rules that change an item, `sf.rules`, changing what a hook was given to say, and the `onSpeech` hook. |
| `passwords` | The text of password fields. Without it, that text is left out. |
| `input` | Commands, `sf.bind`, `navigation`, the `onActions` hook, and actions in rules. |
| `network` | `sf.fetch`. |
| `clipboard` | `sf.clipboard`. |
| `system` | `sf.system` and `sf.resume`. |
| `notifications` | The `onNotification` hook. |
| `events` | The `onEvent` hook, which sees every accessibility event in every app. |
| `dialogs` | `sf.ui`. |
| `java` | LuaJava's `java` module. See [Java](#java). |

SandFly marks `passwords`, `network`, `clipboard`, `system`, `notifications`, `events`, `dialogs` and `java` as ones to allow only for trusted scripts.

### Java

With the `java` permission, a script gets LuaJava's [`java` module](https://luajava.iroiro.party/api.html): `java.import`, `java.new`, `java.proxy` and the rest. It can call any Java class in SandFly, with all of SandFly's access, in every app, and no other permission limits it. Install such a script only from someone you trust.

```lua
local Build = java.import('android.os.Build')
sf.speak('This is a ' .. Build.MODEL)
```

Calls into the script hold the script's Lua state, so a Java object made with `java.proxy` and called from another thread waits until the script's current call ends. Don't wait in the script for something that needs SandFly's main thread.

## Rules

A rule changes an item without running code. Rules are the cheapest way to fix an app, and they keep working while the script is busy. Rules that change an item need the `speech` permission.

```lua
rules = {
  { id = 'send_button', label = 'Send' },
  { match = { className = 'ImageView', parentId = 'toolbar' }, hide = true },
  { id = 'row', group = true, hint = 'Double-tap to open' },
},
```

A rule names its item with `id`, with `match`, or with both:

*   `id` is the item's view id, either the short form (`send_button`) or the full one (`com.example:id/send_button`).
*   `match` is a [query](#queries).

`window` and `activity` limit a rule to windows whose title contains the text, or to an activity whose class name ends with it. A rule with `activity` applies while the activity isn't known yet, as described for `apps`.

A rule can set any of these:

| Field | Effect |
|---|---|
| `label` | The item's name, used where the app gave none or a poor one. |
| `speak` | Everything SandFly says for the item, in place of its name, role and state. |
| `hide` | `true` hides the item: swiping skips it. `'all'` hides what's inside it too. A hidden item is still there, so a script can click it. |
| `silence` | `true` stops SandFly speaking the item's changes, selections and announcements, such as a progress bar's updates. `'all'` silences what's inside it too, such as the seek bar in a grouped row. Focusing the item still speaks it. |
| `role` | The item's role, such as `'button'`. An unknown role is an error that lists the roles. |
| `state` | The item's state, such as `'Selected'`. |
| `hint` | The usage hint. |
| `heading` | `true` or `false`. |
| `group` | `true` reads the item and everything in it as one item. |
| `readBefore`, `readAfter` | Moves the item in the reading order to just before or after another item, named by id or query. Use one, not both. |
| `actions` | Items to add to the item's Actions menu. See below. |
| `activate` | A [query](#queries) for the item inside it that a double-tap presses, such as the part of a grouped post that opens it. |
| `liftToActivate` | `true` activates the item when the finger lifts from it after touching it, as **Lift to activate** does, whatever that setting is. |
| `longPressAfter` | For an item with `liftToActivate`, a number of milliseconds from 100 to 10,000. Holding the finger on the item for that time long-presses it, and lifting the finger then does nothing more. Items that can't be long-pressed are left alone. |

When several rules match an item, the first one that sets a field wins for that field. Rules from scripts for the app come before rules from scripts for all apps, and `sf.rules.add` comes before the manifest.

To find an item's id, class and role, assign **Inspect focused item** to a gesture, or add it to the SandFly menu in **SandFly menu**, under **Controls**. **Copy screen tree** copies every item on screen as JSON.

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

Each action has a `title` and one of `click`, `longClick`, `focus`, `scrollForward`, `scrollBackward` or `speak`. All but `speak` take the item to act on, as an id or a query, and need the `actions` permission. `speak` takes text, or `{ textOf = item }`, which needs `screen`. Actions in rules need the `input` permission.

SandFly looks for the item inside the focused item first, and then on the whole screen. In a list where every row has the same button, the second search finds the first row's button, so use the [`onActions` hook](#hooks) there and search from the row yourself.

## Queries

A query finds items. It's an id as text, or a record with any of these; an item must match them all.

| Field | Matches |
|---|---|
| `id` | The view id, short or full. |
| `text` | The item's text, exactly. |
| `textContains` | Part of the item's text or content description, ignoring case. |
| `contentDescription` | The content description, exactly. |
| `className` | The end of the class name, so `'Button'` matches `android.widget.Button`. |
| `role` | The role the app gave the item. |
| `clickable` | `true` or `false`. |
| `parentId` | The view id of the item's parent. |
| `inside` | A query that one of the items around it must match: its parent, the parent's parent, and so on. `{ className = 'Button', inside = { textContains = 'play video' } }` matches every button in a video's row, and not the row itself. `inside` can't hold another `inside`. |
| `has` | A query that one of the items inside it must match, up to 6 levels down and 60 items. `{ parentId = 'results', has = { textContains = 'Subscribe to' } }` matches a row in a list that holds a Subscribe button. `has` can't hold `has` or `inside`. |

## Items

Hooks, commands and `sf.screen` give you items. An item is a snapshot: its fields don't change when the screen does. Call `item:refresh()` for a new one.

Fields: `text`, `contentDescription`, `hint`, `stateDescription`, `id`, `className`, `role`, `packageName`, `windowId`, `bounds` (`left`, `top`, `right`, `bottom`), `childCount`, `selectionStart`, `selectionEnd`, `checkable`, `checked`, `clickable`, `longClickable`, `focusable`, `focused`, `accessibilityFocused`, `selected`, `enabled`, `editable`, `password`, `scrollable`, `visible`, `heading`, and `actions`, a list of `{ id, label }`. A field the item doesn't have is `nil`.

| Method | Does |
|---|---|
| `item:parent()`, `item:children()`, `item:child(index)` | The items around it. `child(1)` is the first. |
| `item:find(query)`, `item:findAll(query, limit)` | Items inside it. `find` gives the first, or `nil`. `limit` is 50 if left out. |
| `item:refresh()` | A new snapshot, or `nil` if the item is gone. |
| `item:click()`, `item:longClick()`, `item:scrollForward()`, `item:scrollBackward()` | Performs the action, and gives whether it worked. |
| `item:perform(action, args)` | Performs `click`, `longClick`, `scrollForward`, `scrollBackward`, `expand`, `collapse`, `dismiss`, `copy`, `cut`, `paste`, `select`, a custom action by its label, or any action by its number. |
| `item:setText(text)` | Replaces the text of an edit field. |
| `item:focus()` | Moves SandFly's focus to the item. |

`a == b` is true when two snapshots are the same item, and `tostring(item)` gives its text, description, id or class.

The actions and `focus()` [wait](#waiting) for the result. Where a call can't wait, such as in a speech hook, they still act, and give `nil`.

SandFly keeps the last 500 items a script was given. Using an older one raises "That item is no longer available".

## Waiting

Some calls wait for an answer: dialogs, `sf.fetch`, `sf.sleep`, and an item's actions. A script waits only where SandFly runs it as a task: the main file, hooks other than the four speech hooks and `onActions`, commands, actions, timers, dialog callbacks, and functions given to `sf.run`. While one task waits, others run. The script's own coroutines can wait too, inside a task.

```lua
sf.onCommand('delete', function()
  if sf.ui.confirm('Delete the message?') then
    sf.screen.find('delete_button'):click()
  end
end)
```

A call that waits raises an error where it can't, such as in `onFocus`, because those hooks must answer at once. Start a task with `sf.run(function() ... end)` to do it later.

## The sf table

### Hooks

Each of these takes a function and returns a function that removes it again.

| Hook | Called | Arguments |
|---|---|---|
| `sf.onFocus` | Before SandFly speaks a focused item. | The item, and the text SandFly will say. |
| `sf.onSpeech` | Before SandFly says anything. | The text. |
| `sf.onNotification` | Before SandFly speaks a notification or toast. | `{ package, toast, title, text, category }`, and the text. |
| `sf.onAnnouncement` | Before SandFly speaks an app's announcement. | `{ package, text }`, and the text. |
| `sf.onAppEnter`, `sf.onAppLeave` | When the app in front changes, or the script loads or unloads. | `{ package, activity, window }`. |
| `sf.onWindowChange` | When a window opens or changes. | `{ package, activity, window }`. |
| `sf.onTextChange` | When text in an edit field changes. | `{ package, node, text, before, from, added, removed, password }`. |
| `sf.onContentChange` | Shortly after part of the screen changes. | `{ package, node, changeTypes }`. |
| `sf.onEvent` | For every accessibility event. | `{ type, package, className, text, contentDescription, beforeText, password, windowId, time }`. |
| `sf.onSettingChange` | When the user changes one of the script's settings. | The key and the new value. |
| `sf.onButton` | When the user presses a button setting. | The key. |
| `sf.onActions` | When SandFly builds the Actions menu for an item. | The item. Return a list of `{ title, run }`; `run` is called with the item, and can wait. |

The first four can change what's said. Return:

*   Text, or `{ speak = text }`, to say that instead.
*   `{ before = text, after = text }` to add to what's said.
*   `false` or `{ silent = true }` to say nothing.
*   Nothing, to leave it alone.

Changing speech needs the `speech` permission. SandFly waits 30 milliseconds for these hooks, then speaks without them, so keep them fast. They can't [wait](#waiting). Speech hooks take priority over queued background work, and all listeners share that deadline, including queue time. A hook cannot interrupt Lua already running. If the script is in the middle of something that takes longer, such as a command, SandFly doesn't wait at all.

### Commands

A command is something the user runs. Declare it in the manifest, and give it at least one way to run:

```lua
commands = {
  { id = 'unread', title = 'Next unread', gesture = 'swipeUpThenRight', keys = 'sf+u' },
  { id = 'summary', title = 'Read summary', menu = true },
  { id = 'messages', title = 'Messages', control = true },
},
```

*   `gesture` is a gesture name or a list of them. The names are in [Gestures](#gestures).
*   `keys` is keys or a list. Keys start with `sf`, SandFly's modifier key, then any of `shift`, `ctrl`, `alt` and `meta`, then a key: `'sf+shift+n'`. A second key can follow a comma: `'sf+t, s'`.
*   `menu = true` adds the command to the SandFly menu.
*   `control = true` adds it to the reading controls. Swiping up or down with the control selected runs the command with `direction` set to `'previous'` or `'next'`.

Commands need the `input` permission. The user can turn each gesture and key off on the script's page, or assign others. While a script runs, its gestures and keys come before SandFly's own, except for the gesture assigned to **Turn all scripts on or off**.

```lua
sf.onCommand('unread', function(event)
  -- event.id, event.via ('gesture', 'keys', 'menu' or 'control'), event.direction, event.node
end)
```

`event.node` is the focused item. Return `false`, before waiting for anything, to have SandFly do what the gesture or keys normally do.

`sf.bind(command, { gesture = name })` or `sf.bind(command, { keys = keys })` adds a gesture or keys to a declared command, and `sf.unbind` removes one.

### Navigation

```lua
navigation = { { title = 'Unread messages', match = { className = 'UnreadRow' } } },
```

Each entry becomes a reading control that moves to the next or previous item matching `id` or `match`. It needs the `input` permission.

### Speech, sound and vibration

*   `sf.speak(text, { interrupt, rate, pitch, language })`. `rate` and `pitch` are from 0.25 to 4, where 1 is the user's own setting. `language` is a tag such as `'de'`.
*   `sf.playSound(name)` plays one of SandFly's sounds (`focus`, `actionable`, `click`, `longClick`, `scroll`, `listEnter`, `listExit`, `end`, `enter`, `typo`, `beep`) or a sound file in the script's zip, such as `'sounds/chime.wav'`.
*   `sf.playTone(frequency, ms, { volume })` plays a tone. `ms` is 100 if left out.
*   `sf.playAudio(samples, { sampleRate, volume })` plays a list of samples from -1 to 1, up to 10 seconds.
*   `sf.vibrate(pattern)` vibrates for a number of milliseconds, or a list of up to 20 that alternates vibration and pause, up to 5 seconds.

Sounds and vibration follow the user's **Sound feedback** and **Vibration feedback** settings. A script can play 4 sounds at once; more are skipped.

### Screen

*   `sf.screen.focused()` is the item with SandFly's focus.
*   `sf.screen.root()` is the top item of the active window.
*   `sf.screen.find(query)` and `sf.screen.findAll(query, limit)` search the active window. A search looks at up to 3,000 items, returns up to 200, and stops when the script runs out of [time](#limits).
*   `sf.app()` is `{ package, activity, window, meta }` for the app in front. `activity` is `nil` until SandFly has seen the app change window. `meta` holds the meta-data in the app's manifest that has a true or false, number or text value, such as `sf.app().meta['com.example.flag']`.

### Rules from code

`sf.rules.add(rule)` adds a rule and returns a number for `sf.rules.remove(number)`. `sf.rules.clear()` removes all added rules. A script can add up to 200.

### Storage

`sf.storage` keeps data between loads: `get(key, fallback)`, `set(key, value)`, `remove(key)`, `keys()` and `clear()`. Values are text, numbers, `true`, `false` and tables of them. A script can store about 512,000 characters. The user can clear it with **Clear stored data**.

### Settings

```lua
settings = {
  { key = 'sayApp', type = 'switch', title = 'Say the app', default = true },
  { key = 'voice', type = 'list', title = 'Voice', options = { 'low', { value = 'hi', label = 'High' } } },
  { key = 'name', type = 'text', title = 'Your name', summary = 'Used in greetings' },
  { key = 'count', type = 'number', title = 'How many' },
  { key = 'reset', type = 'button', title = 'Reset counts' },
},
```

Settings appear on their own screen on the script's page. `sf.settings.get(key)` reads one, `sf.settings.all()` reads them all, and `sf.settings.set(key, value)` changes one. `sf.settings.setOptions(key, options)` replaces the options of a list. A setting's value can be up to 16,384 characters.

### Dialogs

All of these need the `dialogs` permission, and all but `dialog` [wait](#waiting). Every dialog's title starts with the script's name, so that it can't be taken for the app's own or the system's.

*   `sf.ui.alert(message, { title })`
*   `sf.ui.confirm(message, { title, ok, cancel })` gives `true` or `false`.
*   `sf.ui.prompt(title, { text, hint })` gives the text, or `nil` if cancelled.
*   `sf.ui.choose(title, options, { selected })` gives the chosen value, or `nil`.
*   `sf.ui.dialog({ title, items, onClose })` shows a form with up to 50 items, and gives the form at once.

Each form item has a `type`, an `id`, and usually a `label`:

| Type | Other fields |
|---|---|
| `label` | |
| `edit` | `value`, `hint`, `number`, `password`, `multiline`, `onChange` |
| `switch` | `value`, `onChange` |
| `list` | `items`, `value`, `onChange` |
| `button` | `close = true` closes the form; `onClick` |

`onChange` is called with the new value and the form, and `onClick` with the form. The form has `form:get(id)`, `form:values()`, `form:set(id, { value, label, items, enabled, visible })`, `form:close()`, and `form:wait()`, which waits until the form closes and gives the final values.

### Clipboard, system and network

*   `sf.clipboard.get()` and `sf.clipboard.set(text)`.
*   `sf.system.back()`, `home()`, `recents()`, `notifications()`, `quickSettings()`, `powerDialog()`, `lockScreen()`, `takeScreenshot()` and `allApps()`.
*   `sf.system.openApp(packageName)`.
*   `sf.resume()` resumes a paused SandFly. It needs the `system` permission.
*   `sf.fetch(url, { method, headers, body, timeout })` [waits](#waiting) for an `https` address and gives the response. `method` is `GET`, `POST`, `PUT`, `DELETE`, `HEAD` or `OPTIONS`. A `body` that isn't text is sent as JSON. The response has `status`, `statusText`, `ok`, `url`, `headers` (with lowercase names), `body`, and `response:json()`. A request body can be 1 MB and a response 5 MB, the timeout is at most 60 seconds, and a script can have 8 requests going at once.

### Translations

Put a file for each language in the zip's `locales` folder, named by language tag: `locales/de.json`, `locales/pt-BR.json`. Each maps the English text to the translation:

```json
{
  "Next unread": "Nächste ungelesene",
  "{count} message": { "one": "{count} Nachricht", "other": "{count} Nachrichten" }
}
```

SandFly translates the manifest's names, titles, labels and hints with it. In code:

*   `sf._(text, params)` translates text and fills in `{name}` placeholders.
*   `sf.ngettext(singular, plural, count, params)` picks the plural form for the language.
*   `sf.locale()` is the device's language tag, and `sf.language()` the one in use.

### Everything else

*   `sf.apiVersion` is the script API version of this SandFly.
*   `sf.manifest()` is the script's manifest.
*   `print(...)` and `sf.log(...)` write to the script's **Log**, as do `sf.log.debug`, `sf.log.info`, `sf.log.warn` and `sf.log.error`. They reach Android's log only at the level set in SandFly's developer settings.
*   `sf.setTimeout(fn, ms, ...)` and `sf.setInterval(fn, ms, ...)` run a function later, or again and again, and give a number for `sf.clearTimer`. `sf.sleep(ms)` [waits](#waiting). A script can have 100 timers at once.
*   `sf.run(fn, ...)` runs a function as a task, so that it can wait.
*   `json.encode(value)` and `json.decode(text)`, also as `sf.json`. An empty table is encoded as `[]`, and `json.null` stands for `null`.
*   `require(name)` loads another Lua file in the script's zip, once: `require('lib.words')` loads `lib/words.lua`.

## Gestures

`swipeUp`, `swipeDown`, `swipeLeft`, `swipeRight`, `swipeUpThenDown`, `swipeDownThenUp`, `swipeLeftThenRight`, `swipeRightThenLeft`, `swipeUpThenLeft`, `swipeUpThenRight`, `swipeDownThenLeft`, `swipeDownThenRight`, `swipeLeftThenUp`, `swipeLeftThenDown`, `swipeRightThenUp`, `swipeRightThenDown`, `doubleTap`, `doubleTapAndHold`, `twoFingerTap`, `twoFingerDoubleTap`, `twoFingerTripleTap`, `twoFingerDoubleTapAndHold`, `twoFingerTripleTapAndHold`, `twoFingerSwipeUp`, `twoFingerSwipeDown`, `twoFingerSwipeLeft`, `twoFingerSwipeRight`, `threeFingerTap`, `threeFingerDoubleTap`, `threeFingerTripleTap`, `threeFingerQuadrupleTap`, `threeFingerTapAndHold`, `threeFingerDoubleTapAndHold`, `threeFingerTripleTapAndHold`, `threeFingerSwipeUp`, `threeFingerSwipeDown`, `threeFingerSwipeLeft`, `threeFingerSwipeRight`, `fourFingerTap`, `fourFingerDoubleTap`, `fourFingerTripleTap`, `fourFingerDoubleTapAndHold`, `fourFingerSwipeUp`, `fourFingerSwipeDown`, `fourFingerSwipeLeft`, `fourFingerSwipeRight`, `fingerprintSwipeUp`, `fingerprintSwipeDown`, `fingerprintSwipeLeft` and `fingerprintSwipeRight`.

## Limits

*   A script has 2 seconds to load, and 2 seconds for each call into it. Hooks that change speech have 250 milliseconds. Searching the screen counts toward that time.
*   A script can use 32 MB of memory. `string.rep` can make text up to 8 MB.
*   SandFly checks the time and memory every 10,000 Lua instructions, and stops the call with an error that `pcall` can't catch. A single library call, such as a pattern match on long text with several `.-` or `.*`, runs to its end, so keep patterns simple on long text.
*   SandFly turns a script off after 5 errors in a minute, or after it runs out of time 3 times in 5 minutes, and says so.

## If a script goes wrong

*   The script's page has its **Log**, with what it wrote and every error.
*   **Turn off all scripts** on the Scripts screen stops every script without changing which ones are on.
*   **Turn all scripts on or off** does the same from a gesture. Assign it in gesture settings before trying a script you don't trust yet. Scripts can't take over the gesture it's assigned to.
*   Scripts change nothing in SandFly's own settings, so you can always get to the Scripts screen. A script with the `java` permission can.
