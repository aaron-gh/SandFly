# Writing Backtalk scripts

A script is a JavaScript file that changes how Backtalk works in one app, or in every app. This document describes the whole script API, version 1.

Scripts run on phones and tablets. Watches and TVs can't run them.

## What a script is

A script is one of these:

*   A `.js` file.
*   A `.zip` file with a `main.js` in it, plus anything `main.js` uses: other JavaScript modules, sounds, and translations. The zip can be up to 5 MB unpacked and hold up to 200 files.

To install a script, go to **Advanced settings** > **Scripts** in Backtalk settings and choose **Import script**. Backtalk lists what the script asks to do before it installs it. Importing a file with the id of a script that's already installed updates that script and keeps its data and settings; Backtalk always asks first, and says so if the author changed.

Scripts run in [QuickJS](https://github.com/quickjs-ng/quickjs), so you can use modern JavaScript, including modules, classes, `async` and `await`. A script can't reach Android directly. It has no file system, no `require`, and no Java classes; everything goes through the `backtalk` object described below.

## A first script

```js
export const manifest = {
  id: 'hello',
  name: 'Hello',
  version: '1.0',
  author: 'You',
  minApiVersion: 1,
  testedApiVersion: 1,
  apps: ['com.android.settings'],
  permissions: ['speech'],
};

backtalk.onAppEnter(() => backtalk.speak('Hello from a script'));
```

[examples/example-plugin.js](scripting/examples/example-plugin.js) is a longer one. It also comes with Backtalk: choose **Add the example script** on the Scripts screen.

## The manifest

`main.js` must contain `export const manifest = { ... }`. Backtalk reads it without running the rest of the script, so it can only hold plain values: no variables, function calls or template strings with expressions.

| Field | Meaning |
|---|---|
| `id` | Required. Lowercase letters, digits, dots, dashes and underscores, starting with a letter or digit, up to 64 characters. A script with the same id replaces the installed one. |
| `name` | Required. Shown in the Scripts list, and at the start of the title of every dialog the script shows. |
| `author` | Required. |
| `minApiVersion` | Required. The lowest script API version the script works with. This document describes version 1. |
| `testedApiVersion` | Required. The highest script API version you tested with. Backtalk warns before installing a script tested only with an older version. |
| `version` | The script's own version, as text. The default is `'1'`. |
| `description` | Shown on the script's page and before it's installed. |
| `homepage` | An `http` or `https` address. The script's page has a button that opens it. |
| `apps` | Where the script runs. Leave it out for a script that runs in every app. See [Where a script runs](#where-a-script-runs). |
| `permissions` | A list of the [permissions](#permissions) the script needs. |
| `settings` | [Settings](#settings) the user can change on the script's page. |
| `rules` | [Rules](#rules) that change items without running any code. |
| `commands` | [Commands](#commands) the user can run with a gesture, keys, the Backtalk menu or reading controls. |
| `navigation` | [Reading controls](#navigation) that move between items of a kind. |

### Where a script runs

`apps` is a package name, or a list. Each entry is a package name or an object:

```js
apps: [
  'com.example.mail',
  { package: 'com.example.chat', activity: '.ConversationActivity' },
  { package: 'com.example.chat', window: 'Settings' },
],
```

*   `activity` limits the script to one screen. It's the activity's full class name, or the end of it.
*   `window` limits it to windows whose title contains the text.

A script with `apps` is loaded when one of its apps comes to the front and unloaded when it leaves, so anything it keeps in variables is lost then. Use [`backtalk.storage`](#storage) for what should last.

A script without `apps` runs in every app and is loaded as long as it's turned on.

Scripts never change Backtalk's own screens. Rules, speech changes, gestures, keys, menu items and reading controls from scripts don't apply there, and a script can't name Backtalk in `apps`. While a Backtalk screen is in front, calls that need the `screen`, `actions`, `system` or `dialogs` permission throw an error, `backtalk.speak` doesn't interrupt, and scripts get no events from Backtalk itself. This is so that **Advanced settings** > **Scripts** always works, whatever a script does.

Scripts are also unloaded while the screen is off, while Backtalk is paused, and while **Turn off all scripts** is on. Timers and requests end when a script is unloaded.

### Permissions

A script can only use what its manifest asks for. The user sees the list before installing, and can turn each permission off on the script's page. A call that needs a permission the script doesn't have throws an error; a hook or rule that needs one is skipped, with a warning in the script's log.

| Permission | What it allows |
|---|---|
| `screen` | Reading the screen: `backtalk.screen`, the items given to hooks, commands and actions, and the `onFocus`, `onAnnouncement`, `onTextChange` and `onContentChange` hooks. A script for all apps also needs it to learn which app is in front: `backtalk.app`, `onAppEnter`, `onAppLeave` and `onWindowChange`. |
| `actions` | Clicking, scrolling, typing and moving focus: `node.click()`, `node.perform()`, `node.focus()`, `node.setText()`, and rule actions other than `speak`. |
| `speech` | Speaking with `backtalk.speak`, rules that change an item, `backtalk.rules`, changing what a hook was given to say, and the `onSpeech` hook. |
| `passwords` | The text of password fields. Without it, that text is left out. |
| `input` | Commands, `backtalk.bind`, `navigation`, the `onActions` hook, and actions in rules. |
| `network` | `fetch`. |
| `clipboard` | `backtalk.clipboard`. |
| `system` | `backtalk.system` and `backtalk.resume`. |
| `notifications` | The `onNotification` hook. |
| `events` | The `onEvent` hook, which sees every accessibility event in every app. |
| `dialogs` | `backtalk.ui`. |

Backtalk marks `passwords`, `network`, `clipboard`, `system`, `notifications`, `events` and `dialogs` as ones to allow only for trusted scripts.

## Rules

A rule changes an item without running code. Rules are the cheapest way to fix an app, and they keep working while the script is busy. Rules that change an item need the `speech` permission.

```js
rules: [
  { id: 'send_button', label: 'Send' },
  { match: { className: 'ImageView', parentId: 'toolbar' }, hide: true },
  { id: 'row', group: true, hint: 'Double-tap to open' },
],
```

A rule names its item with `id`, with `match`, or with both:

*   `id` is the item's view id, either the short form (`send_button`) or the full one (`com.example:id/send_button`).
*   `match` is a [query](#queries).

`window` and `activity` limit a rule to windows whose title contains the text, or to an activity whose class name ends with it.

A rule can set any of these:

| Field | Effect |
|---|---|
| `label` | The item's name, used where the app gave none or a poor one. |
| `speak` | Everything Backtalk says for the item, in place of its name, role and state. |
| `hide` | `true` hides the item: swiping skips it. `'all'` hides what's inside it too. A hidden item is still there, so a script can click it. |
| `role` | The item's role, such as `'button'`. An unknown role is an error that lists the roles. |
| `state` | The item's state, such as `'Selected'`. |
| `hint` | The usage hint. |
| `heading` | `true` or `false`. |
| `group` | `true` reads the item and everything in it as one item. |
| `readBefore`, `readAfter` | Moves the item in the reading order to just before or after another item, named by id or query. Use one, not both. |
| `actions` | Items to add to the item's Actions menu. See below. |

When several rules match an item, the first one that sets a field wins for that field. Rules from scripts for the app come before rules from scripts for all apps, and `backtalk.rules.add` comes before the manifest.

To find an item's id, class and role, assign **Inspect focused item** to a gesture, or add it to the Backtalk menu in **Customize menus**. **Copy screen tree** copies every item on screen as JSON.

### Actions in rules

```js
{
  id: 'message_row',
  actions: [
    { title: 'Archive', click: 'archive_button' },
    { title: 'Read time', speak: { textOf: 'timestamp' } },
  ],
}
```

Each action has a `title` and one of `click`, `longClick`, `focus`, `scrollForward`, `scrollBackward` or `speak`. All but `speak` take the item to act on, as an id or a query, and need the `actions` permission. `speak` takes text, or `{ textOf: item }`, which needs `screen`. Actions in rules need the `input` permission.

Backtalk looks for the item inside the focused item first, and then on the whole screen. In a list where every row has the same button, the second search finds the first row's button, so use the [`onActions` hook](#hooks) there and search from the row yourself.

## Queries

A query finds items. It's an id as text, or an object with any of these; an item must match them all.

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
| `inside` | A query that one of the items around it must match: its parent, the parent's parent, and so on. `{ className: 'Button', inside: { textContains: 'play video' } }` matches every button in a video's row, and not the row itself. `inside` can't hold another `inside`. |

## Items

Hooks, commands and `backtalk.screen` give you items. An item is a snapshot: its fields don't change when the screen does. Call `refresh()` for a new one.

Fields: `text`, `contentDescription`, `hint`, `stateDescription`, `id`, `className`, `role`, `packageName`, `windowId`, `bounds` (`left`, `top`, `right`, `bottom`), `childCount`, `selectionStart`, `selectionEnd`, `checkable`, `checked`, `clickable`, `longClickable`, `focusable`, `focused`, `accessibilityFocused`, `selected`, `enabled`, `editable`, `password`, `scrollable`, `visible`, `heading`, and `actions`, a list of `{ id, label }`.

| Method | Does |
|---|---|
| `parent`, `children`, `child(index)` | The items around it. |
| `find(query)`, `findAll(query, limit = 50)` | Items inside it. `find` gives the first, or `null`. |
| `refresh()` | A new snapshot, or `null` if the item is gone. |
| `equals(other)` | Whether two snapshots are the same item. |
| `click()`, `longClick()`, `scrollForward()`, `scrollBackward()` | Performs the action. Returns a promise of whether it worked. |
| `perform(action, args)` | Performs `click`, `longClick`, `scrollForward`, `scrollBackward`, `expand`, `collapse`, `dismiss`, `copy`, `cut`, `paste`, `select`, a custom action by its label, or any action by its number. |
| `setText(text)` | Replaces the text of an edit field. |
| `focus()` | Moves Backtalk's focus to the item. |

Backtalk keeps the last 500 items a script was given. Using an older one throws "That item is no longer available".

## The backtalk object

### Hooks

Each of these takes a function and returns a function that removes it again.

| Hook | Called | Arguments |
|---|---|---|
| `onFocus` | Before Backtalk speaks a focused item. | The item, and the text Backtalk will say. |
| `onSpeech` | Before Backtalk says anything. | The text. |
| `onNotification` | Before Backtalk speaks a notification or toast. | `{ package, toast, title, text, category }`, and the text. |
| `onAnnouncement` | Before Backtalk speaks an app's announcement. | `{ package, text }`, and the text. |
| `onAppEnter`, `onAppLeave` | When the app in front changes, or the script loads or unloads. | `{ package, activity, window }`. |
| `onWindowChange` | When a window opens or changes. | `{ package, activity, window }`. |
| `onTextChange` | When text in an edit field changes. | `{ package, node, text, before, from, added, removed, password }`. |
| `onContentChange` | Shortly after part of the screen changes. | `{ package, node, changeTypes }`. |
| `onEvent` | For every accessibility event. | `{ type, package, className, text, contentDescription, beforeText, password, windowId, time }`. |
| `onSettingChange` | When the user changes one of the script's settings. | The key and the new value. |
| `onButton` | When the user presses a button setting. | The key. |
| `onActions` | When Backtalk builds the Actions menu for an item. | The item. Return a list of `{ title, run }`; `run` is called with the item. |

The first four can change what's said. Return:

*   Text, or `{ speak: text }`, to say that instead.
*   `{ before: text, after: text }` to add to what's said.
*   `false` or `{ silent: true }` to say nothing.
*   Nothing, to leave it alone.

Changing speech needs the `speech` permission. Backtalk waits 30 milliseconds for these hooks, then speaks without them, so keep them fast and don't `await` in them. Speech hooks take priority over queued background work, and all listeners share that deadline, including queue time. A hook cannot interrupt JavaScript already running. If the script is in the middle of something that takes longer, such as a command, Backtalk doesn't wait at all.

### Commands

A command is something the user runs. Declare it in the manifest, and give it at least one way to run:

```js
commands: [
  { id: 'unread', title: 'Next unread', gesture: 'swipeUpThenRight', keys: 'backtalk+u' },
  { id: 'summary', title: 'Read summary', menu: true },
  { id: 'messages', title: 'Messages', control: true },
],
```

*   `gesture` is a gesture name or a list of them. The names are in [Gestures](#gestures).
*   `keys` is keys or a list. Keys start with `backtalk`, Backtalk's modifier key, then any of `shift`, `ctrl`, `alt` and `meta`, then a key: `'backtalk+shift+n'`. A second key can follow a comma: `'backtalk+t, s'`.
*   `menu: true` adds the command to the Backtalk menu.
*   `control: true` adds it to the reading controls. Swiping up or down with the control selected runs the command with `direction` set to `'previous'` or `'next'`.

Commands need the `input` permission. The user can turn each gesture and key off on the script's page, or assign others. While a script runs, its gestures and keys come before Backtalk's own, except for the gesture assigned to **Turn all scripts on or off**.

```js
backtalk.onCommand('unread', (event) => {
  // event.id, event.via ('gesture', 'keys', 'menu' or 'control'), event.direction, event.node
});
```

`event.node` is the focused item. Return `false` to have Backtalk do what the gesture or keys normally do.

`backtalk.bind(command, { gesture })` or `backtalk.bind(command, { keys })` adds a gesture or keys to a declared command, and `backtalk.unbind` removes one.

### Navigation

```js
navigation: [{ title: 'Unread messages', match: { className: 'UnreadRow' } }],
```

Each entry becomes a reading control that moves to the next or previous item matching `id` or `match`. It needs the `input` permission.

### Speech, sound and vibration

*   `backtalk.speak(text, { interrupt, rate, pitch, language })`. `rate` and `pitch` are from 0.25 to 4, where 1 is the user's own setting. `language` is a tag such as `'de'`.
*   `backtalk.playSound(name)` plays one of Backtalk's sounds (`focus`, `actionable`, `click`, `longClick`, `scroll`, `listEnter`, `listExit`, `end`, `enter`, `typo`, `beep`) or a sound file in the script's zip, such as `'sounds/chime.wav'`.
*   `backtalk.playTone(frequency, ms = 100, { volume })` plays a tone.
*   `backtalk.playAudio(samples, { sampleRate, volume })` plays samples from -1 to 1, up to 10 seconds.
*   `backtalk.vibrate(pattern)` vibrates for a number of milliseconds, or a list of up to 20 that alternates vibration and pause, up to 5 seconds.

Sounds and vibration follow the user's **Sound feedback** and **Vibration feedback** settings. A script can play 4 sounds at once; more are skipped.

### Screen

*   `backtalk.screen.focused()` is the item with Backtalk's focus.
*   `backtalk.screen.root()` is the top item of the active window.
*   `backtalk.screen.find(query)` and `backtalk.screen.findAll(query, limit = 50)` search the active window. A search looks at up to 3,000 items, returns up to 200, and stops when the script runs out of [time](#limits).
*   `backtalk.app` is `{ package, activity, window }` for the app in front.

### Rules from code

`backtalk.rules.add(rule)` adds a rule and returns a number for `backtalk.rules.remove(number)`. `backtalk.rules.clear()` removes all added rules. A script can add up to 200.

### Storage

`backtalk.storage` keeps data between loads: `get(key, fallback)`, `set(key, value)`, `remove(key)`, `keys()` and `clear()`. Values are anything JSON can hold. A script can store about 512,000 characters. The user can clear it with **Clear stored data**.

### Settings

```js
settings: [
  { key: 'sayApp', type: 'switch', title: 'Say the app', default: true },
  { key: 'voice', type: 'list', title: 'Voice', options: ['low', { value: 'hi', label: 'High' }] },
  { key: 'name', type: 'text', title: 'Your name', summary: 'Used in greetings' },
  { key: 'count', type: 'number', title: 'How many' },
  { key: 'reset', type: 'button', title: 'Reset counts' },
],
```

Settings appear on their own screen on the script's page. `backtalk.settings.get(key)` reads one, `backtalk.settings.all()` reads them all, and `backtalk.settings.set(key, value)` changes one. `backtalk.settings.setOptions(key, options)` replaces the options of a list. A setting's value can be up to 16,384 characters.

### Dialogs

All of these need the `dialogs` permission. Every dialog's title starts with the script's name, so that it can't be taken for the app's own or the system's.

*   `await backtalk.ui.alert(message, { title })`
*   `await backtalk.ui.confirm(message, { title, ok, cancel })` gives `true` or `false`.
*   `await backtalk.ui.prompt(title, { text, hint })` gives the text, or `null` if cancelled.
*   `await backtalk.ui.choose(title, options, { selected })` gives the chosen value, or `null`.
*   `backtalk.ui.dialog({ title, items, onClose })` shows a form with up to 50 items.

Each form item has a `type`, an `id`, and usually a `label`:

| Type | Other fields |
|---|---|
| `label` | |
| `edit` | `value`, `hint`, `number`, `password`, `multiline`, `onChange` |
| `switch` | `value`, `onChange` |
| `list` | `items`, `value`, `onChange` |
| `button` | `close: true` closes the form; `onClick` |

`onChange` is called with the new value and the form, and `onClick` with the form. The form has `get(id)`, `values`, `set(id, { value, label, items, enabled, visible })`, `close()`, and `closed`, a promise of the final values.

### Clipboard, system and network

*   `backtalk.clipboard.get()` and `backtalk.clipboard.set(text)`.
*   `backtalk.system.back()`, `home()`, `recents()`, `notifications()`, `quickSettings()`, `powerDialog()`, `lockScreen()`, `takeScreenshot()` and `allApps()`.
*   `backtalk.system.openApp(packageName)`.
*   `backtalk.resume()` resumes a paused Backtalk. It needs the `system` permission.
*   `fetch(url, { method, headers, body, timeout })` works like the web's, for `https` addresses only. `method` is `GET`, `POST`, `PUT`, `DELETE`, `HEAD` or `OPTIONS`. A `body` that isn't text is sent as JSON. The response has `status`, `statusText`, `ok`, `url`, `headers`, `text()` and `json()`. A request body can be 1 MB and a response 5 MB, the timeout is at most 60 seconds, and a script can have 8 requests going at once.

### Translations

Put a file for each language in the zip's `locales` folder, named by language tag: `locales/de.json`, `locales/pt-BR.json`. Each maps the English text to the translation:

```json
{
  "Next unread": "Nächste ungelesene",
  "{count} message": { "one": "{count} Nachricht", "other": "{count} Nachrichten" }
}
```

Backtalk translates the manifest's names, titles, labels and hints with it. In code:

*   `backtalk._(text, params)` translates text and fills in `{name}` placeholders.
*   `backtalk.ngettext(singular, plural, count, params)` picks the plural form for the language.
*   `backtalk.locale` is the device's language tag, and `backtalk.language` the one in use.

### Everything else

*   `backtalk.apiVersion` is the script API version of this Backtalk.
*   `backtalk.manifest` is the script's manifest.
*   `console.log`, `info`, `debug`, `warn` and `error` write to the script's **Log**. They reach Android's log only at the level set in Backtalk's developer settings.
*   `setTimeout`, `setInterval`, `clearTimeout` and `clearInterval` work as on the web. A script can have 100 timers at once.

## Gestures

`swipeUp`, `swipeDown`, `swipeLeft`, `swipeRight`, `swipeUpThenDown`, `swipeDownThenUp`, `swipeLeftThenRight`, `swipeRightThenLeft`, `swipeUpThenLeft`, `swipeUpThenRight`, `swipeDownThenLeft`, `swipeDownThenRight`, `swipeLeftThenUp`, `swipeLeftThenDown`, `swipeRightThenUp`, `swipeRightThenDown`, `doubleTap`, `doubleTapAndHold`, `twoFingerTap`, `twoFingerDoubleTap`, `twoFingerTripleTap`, `twoFingerDoubleTapAndHold`, `twoFingerTripleTapAndHold`, `twoFingerSwipeUp`, `twoFingerSwipeDown`, `twoFingerSwipeLeft`, `twoFingerSwipeRight`, `threeFingerTap`, `threeFingerDoubleTap`, `threeFingerTripleTap`, `threeFingerQuadrupleTap`, `threeFingerTapAndHold`, `threeFingerDoubleTapAndHold`, `threeFingerTripleTapAndHold`, `threeFingerSwipeUp`, `threeFingerSwipeDown`, `threeFingerSwipeLeft`, `threeFingerSwipeRight`, `fourFingerTap`, `fourFingerDoubleTap`, `fourFingerTripleTap`, `fourFingerDoubleTapAndHold`, `fourFingerSwipeUp`, `fourFingerSwipeDown`, `fourFingerSwipeLeft`, `fourFingerSwipeRight`, `fingerprintSwipeUp`, `fingerprintSwipeDown`, `fingerprintSwipeLeft` and `fingerprintSwipeRight`.

## Limits

*   A script has 2 seconds to load, and 2 seconds for each call into it. Hooks that change speech have 250 milliseconds. Searching the screen counts toward that time.
*   A script can use 32 MB of memory.
*   Backtalk turns a script off after 5 errors in a minute, or after it runs out of time 3 times in 5 minutes, and says so.

## If a script goes wrong

*   The script's page has its **Log**, with what it wrote and every error.
*   **Turn off all scripts** on the Scripts screen stops every script without changing which ones are on.
*   **Turn all scripts on or off** does the same from a gesture. Assign it in gesture settings before trying a script you don't trust yet. Scripts can't take over the gesture it's assigned to.
*   Scripts change nothing in Backtalk's own settings, so you can always get to the Scripts screen.
