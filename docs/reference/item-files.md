# SandFly item files

An item file holds SandFly items: macros, gesture profiles, keymaps, gestures and keyboard shortcuts in apps, settings profiles, dictionary entries, custom braille characters and custom voice commands. Item files can be exported, shared, edited, written by hand or written by an AI chat, and imported on any phone with SandFly.

An item file is not a settings file. Back up and restore > Export settings writes every setting; an item file holds only the items in it.

## Export and import

Each screen of items has Export to file and Import from file. Export to file on a list screen writes every item in the list; Export to file on an item's screen writes that item alone.

| Items | Screen |
| --- | --- |
| Macros | Settings > Macros |
| Gesture profiles | Settings > Gestures > Gesture profiles |
| Gestures in apps | Settings > Gestures > Gestures in apps |
| Keyboard shortcuts in apps | Settings > Keyboard shortcuts > Keyboard shortcuts in apps |
| Keymaps | Settings > Keyboard shortcuts (exports the keymap in use) |
| Settings profiles | Settings > Settings profiles |
| Speech dictionary | Settings > Speech > Speech dictionary |
| Symbol dictionary | Settings > Speech > Symbol dictionary |
| Braille dictionary | Settings > Braille > Braille dictionary |
| Custom braille characters | Settings > Braille > Braille keyboard > Custom braille characters |
| Custom voice commands | Settings > Voice commands |

Import from file on any of these screens imports everything in the file. An imported item replaces the item of the same kind with the same name, ignoring case; other items stay. Dictionary entries replace entries with the same text to find in the same app. Custom braille characters replace those with the same text in the same braille table. Custom voice commands replace those with the same phrase.

After an import, SandFly lists what it left out: unknown commands, gestures, keys and settings, entries without a name, and regular expressions it refuses.

Back up and restore > Export file format reference writes this document followed by the gestures, commands and settings of the phone, with the value each setting has now. Give that file to an AI chat together with a description of the items to write.

## The file

An item file is a JSON object. `format` is `"sandfly-items"` and `version` is `1`. Each kind of item is a list under its own name. A file can hold any of the lists, in any order.

```json
{
  "format": "sandfly-items",
  "version": 1,
  "macros": [],
  "gestureProfiles": [],
  "keymaps": [],
  "apps": [],
  "settingsProfiles": [],
  "speechDictionary": [],
  "symbolDictionary": [],
  "brailleDictionary": [],
  "brailleCharacters": [],
  "voiceCommands": []
}
```

Text before the first `{` and after the last `}` is ignored, so a reply copied from an AI chat with its code fence can be saved and imported as it is. Comments starting with `//` or `#` are allowed. A file can be at most 2 MB, with at most 1,000 items in each list.

## Commands

Gestures, keyboard shortcuts, macro steps and voice commands run commands. A command is written as its value, such as `"READ_FROM_TOP"` or `"NEXT"`. The file format reference lists the value and name of every command on the phone. `"UNASSIGNED"` runs nothing, so that a gesture or keys do nothing in an app or profile.

An Open app command is `"OPEN_APP:"` followed by the app's package name, such as `"OPEN_APP:com.google.android.gm"`, or by the component the file format reference lists. The app must be installed when the file is imported.

A macro's command is `"MACRO:"` followed by the macro's `id` in the file, or by its name: `"MACRO:Open mail"`. A gesture profile's command is `"GESTURE_PROFILE:"` followed by its `id` or name; `"GESTURE_PROFILE:"` alone changes to Default. Exported files use IDs and include the macros that their items run.

## Gestures

A gesture is written as its key, such as `"pref_shortcut_up"` for Swipe up. The file format reference lists the key and name of every gesture.

## Keyboard shortcuts

A keyboard shortcut is written as its keys joined with `+`, such as `"SandFly+Shift+Right"` or `"Ctrl+Alt+H"`. `SandFly` is the keymap's SandFly key. Modifiers are `Shift`, `Ctrl`, `Alt`, `Meta`, `Sym` and `Fn`. A key is a letter, a digit, or a name: `Left`, `Right`, `Up`, `Down`, `Enter`, `Space`, `Backspace`, `Delete`, `Escape`, `Tab`, `PageUp`, `PageDown`, `Home`, `End`, `Insert`, `Menu`, `Grave`, `Minus`, `Equals`, `LeftBracket`, `RightBracket`, `Backslash`, `Semicolon`, `Apostrophe`, `Comma`, `Period`, `Slash`, or Android's name for the key without `KEYCODE_`, such as `F5` or `Numpad_5`. Names are not case-sensitive.

A sequence, as in the SandFly keymap, has its first keys, a comma and the second key: `"SandFly+O, H"`. The SandFly key pressed twice is `"SandFly twice"`.

The keyboard phone keymap also takes presses. Modifier keys pressed on their own end with `alone`: `"SandFly alone"`, `"SandFly+Shift alone"`. A key or modifier keys pressed twice in a row end with `twice`: `"Enter twice"`, `"Alt alone twice"`. Keys held down end with `held`: modifier keys on their own, `"Alt+Shift held"`, or a key, `"K held"`, which outside typing runs its shortcut when held and the key's own shortcut when pressed briefly.

## Macros

```json
{
  "id": "open-mail",
  "name": "Open mail",
  "condition": [{ "test": "keyboard", "not": true }],
  "steps": [
    { "type": "open_app", "app": "OPEN_APP:com.google.android.gm" },
    { "type": "tap", "label": "Compose", "timeout": 5000 },
    { "type": "wait", "ms": 500 },
    { "type": "command", "command": "READ_FROM_TOP" }
  ]
}
```

`id` is optional; other items in the file can name the macro by it. `condition` is optional; without it the macro always runs. `app` is optional: the package name of the app the macro runs in, such as `"com.whatsapp"`; without it the macro runs in all apps. A macro without `app` whose `condition` has an `app` test that isn't reversed, and no `"or"`, gets that test's package as its `app`. `"actions": true` lists the macro in the Actions menu of the items of its app, or of all apps. `actionsCondition` is optional, a condition like `condition`: with it, the macro is listed in the Actions menu only while the condition is met, such as `[{ "test": "focused", "label": "Send", "not": true }]`.

Steps, by `type`:

| Type | Fields | What it does |
| --- | --- | --- |
| `command` | `command` | Runs a command |
| `wait` | `ms` | Waits, up to 600000 ms |
| `tap` | `label`, `timeout`, `regex`, `field` | Taps the item whose text contains `label` |
| `tap_position` | `x`, `y` | Taps the screen at `x`, `y` in pixels from the top left |
| `touch_hold` | `label`, `hold`, `slide`, `distance`, `timeout`, `regex`, `field` | Touches the centre of the item and holds for `hold` ms, then slides `distance` dp towards `slide` if set, and lifts |
| `focus_item` | `label`, `timeout`, `regex`, `field` | Moves accessibility focus to the item |
| `speak_item` | `label`, `timeout`, `regex`, `field` | Speaks the item |
| `act_item` | `act`, `label`, `times`, `timeout`, `regex`, `field` | Long-presses or scrolls the item `times` times: `act` is `"long_press"`, `"scroll_forward"` or `"scroll_backward"`. A scroll without `label` scrolls the largest list on screen |
| `speak_text` | `text` | Speaks the text |
| `open_app` | `app` | Opens an app: its Open app command |
| `type_text` | `text`, `replace` | Types the text in the focused field; `replace` replaces its text |
| `if` | `condition` | Runs the steps up to the next `else_if`, `else` or `end_if` when the condition is met |
| `else_if` | `condition` | |
| `else` | | |
| `end_if` | | |
| `repeat` | `count` | Repeats the steps up to `end_repeat`, up to 1000 times |
| `repeat_while` | `condition` | Repeats the steps up to `end_repeat` while the condition is met |
| `end_repeat` | | |
| `exit_loop` | | Leaves the repeat |
| `stop` | | Stops the macro |
| `wait_until` | `condition`, `timeout` | Waits until the condition is met; the macro stops after `timeout` ms |

`timeout` of `tap`, `touch_hold`, `focus_item`, `speak_item` and `act_item` is how long to wait for the item, from 500 to 60000 ms; 3000 by default. `label` is found anywhere in the item's text, description, hint, state or tooltip, ignoring case. With `"regex": true` it is a regular expression. `field` limits the search to `"text"`, `"description"`, `"hint"`, `"state"`, `"tooltip"` or `"id"` (the resource ID). `x` and `y` of `tap_position` are from 0 to 10000; the step fails when the position is outside the screen. `hold` of `touch_hold` is from 100 to 600000 ms; 1000 by default. `slide` is `"up"`, `"down"`, `"left"` or `"right"`; without it the touch lifts where it is. `distance` is from 1 to 1000 dp; 100 by default; the end of the slide is kept on the screen. The Speak coordinates command speaks and copies the position of the centre of the focused item. A macro has at most 200 steps.

A condition is a list of tests. Tests are joined with "and"; a test with `"or": true` starts a new group joined with "or". `"not": true` reverses a test. A condition has at most 20 tests.

| Test | Fields | Met when |
| --- | --- | --- |
| `item` | `label`, `kind`, `regex`, `field` | An item on screen contains `label` |
| `focused` | `label`, `kind`, `regex`, `field` | The focused item contains `label`; an empty `label` matches any |
| `app` | `package` | The app of the active window is `package` |
| `keyboard` | | The on-screen keyboard is shown |
| `locked` | | The lock screen is shown |

`kind` is optional: `clickable`, `long_clickable`, `editable`, `scrollable`, `adjustable`, `checkable`, `checked`, `selected`, `heading` or `enabled`. `clickable`, `long_clickable` and `checked` also count an item a few levels around the one with the text; `checked` also counts a checked switch or check box in the row the text is in.

## Gesture profiles

```json
{
  "name": "Reading",
  "swipesBeforeLift": false,
  "gestures": {
    "pref_shortcut_up": "READ_FROM_TOP",
    "pref_shortcut_down": "READ_FROM_CURRENT",
    "pref_shortcut_right_and_down": "MACRO:Open mail"
  }
}
```

A gesture left out of `gestures` runs what it runs without the profile. `swipesBeforeLift` is optional: Recognize swipes before lifting in this profile. The name Default is taken.

## Keymaps

```json
{
  "name": "My keymap",
  "sandFlyKey": "Insert",
  "sequences": false,
  "arrowsNavigate": false,
  "plainKeys": false,
  "shortcuts": {
    "NEXT": "SandFly+Right",
    "PREVIOUS": "SandFly+Left",
    "READ_FROM_TOP": "SandFly+A"
  }
}
```

An imported keymap is a custom keymap; choose it in Settings > Keyboard shortcuts > Choose a keymap. Commands left out of `shortcuts` have no keyboard shortcut in it, so start from an exported keymap to change a few shortcuts. Each command has one shortcut, and two commands can't have the same keys.

`sandFlyKey` is `Meta`, `Alt`, `Ctrl+Alt`, `Ctrl+Meta`, `Insert`, `Numpad Insert`, `Sym`, `Right Alt`, `Ctrl`, `Right Shift` or `Fn`; `Meta` by default. `sequences` is whether shortcuts can be sequences. `arrowsNavigate` is whether arrow keys and other shortcuts without the SandFly key work outside browse mode, as in NVDA. `plainKeys` is optional, `false` by default: whether the SandFly key with a shortcut without it runs that shortcut anywhere, text fields too, as in the keyboard phone keymap.

## Gestures and keyboard shortcuts in apps

```json
{
  "package": "org.telegram.messenger",
  "name": "Telegram",
  "gestures": { "pref_shortcut_up": "MACRO:Open mail" },
  "keys": { "Ctrl+Enter": "UNASSIGNED", "SandFly+M": "READ_FROM_TOP" }
}
```

Each item is one app, by its package name, and replaces what that app had. In `keys`, keys without `SandFly` work without the SandFly key.

## Settings profiles

```json
{
  "name": "Reading",
  "apps": ["com.amazon.kindle"],
  "web": false,
  "reading": true,
  "editing": false,
  "keyboards": ["com.example.keyboard/.KeyboardService"],
  "settings": {
    "pref_vibration": false,
    "pref_soundback": true
  }
}
```

`apps`, `web`, `reading`, `editing` and `keyboards` are when the profile is used without being chosen: in those apps, in web content, during continuous reading, while text is edited and while one of those keyboards is shown. Keyboards are input method IDs, as the list of keyboards of this phone shows them.

`settings` holds each setting the profile sets, by its key, with a value of the type the file format reference shows for it now: `true` or `false`, a number, text in quotes, or a list of texts. A setting saved as text, such as a choice in a list, takes text: `"1.5"`, not `1.5`. `null` sets a setting back to its default while the profile is used. Settings of the braille keyboard and braille display have their file before the key, as the reference shows them: `"braille_keyboard/key"`.

API keys, SandFly Pro and the state of the phone, such as which tutorials were shown, can't be in a profile.

## Dictionaries

`speechDictionary`, `symbolDictionary` and `brailleDictionary` are lists of entries:

```json
{ "find": "btw", "replace": "by the way", "regex": false, "wholeWord": true, "app": "", "appName": "" }
```

`replace` can be empty, which removes the text. `regex` and `wholeWord` are optional, and the symbol dictionary has neither. `app` is a package name, for an entry used in one app; empty for every app. Regular expressions that repeat a group with repetition or alternatives in it, such as `(a+)+`, are refused. The symbol dictionary holds at most 500 entries, the speech and braille dictionaries at most 5000 after an import.

## Custom braille characters

`brailleCharacters` is a list of text typed with braille cells of your choice, each in one braille table:

```json
{ "text": ")", "cells": "56-2356", "table": "POLISH" }
```

`text` is 1 to 50 characters. `cells` is the dot numbers of each cell, 1 to 8, with cells separated by hyphens; at most 20 cells. `table` is the braille table's code name, such as `"POLISH"`, `"POLISH_COMP8"`, `"UEB"` or `"GERMAN"`; a file exported from Custom braille characters shows the code names of the tables in use. At most 500 custom braille characters are kept.

## Custom voice commands

```json
{ "phrase": "open mail", "command": "MACRO:Open mail" }
```

At most 500 custom voice commands are kept.
