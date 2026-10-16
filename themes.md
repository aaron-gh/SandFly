# Sound themes

A sound theme is a set of sounds and vibrations that replace Backtalk's own, with its own settings
for control sounds and 3D audio. Install as many as you like and switch between them. Backtalk's
own sounds are the **Backtalk** theme, which is always there.

Sound themes are in Backtalk settings, under **Sound and vibration** > **Sound themes**.

## Installing a theme

A theme comes as a theme file, which is a ZIP file. There are several ways to install one:

*   **Open it with Backtalk.** Open a theme file from your file manager or your browser's
    downloads, and choose **Install sound theme**.
*   **Share it with Backtalk.** Share a theme file, or a link to one, from another app, and choose
    **Install sound theme**. Sharing a page from your browser works too, as long as it is a link
    to a theme file or to a GitHub repository with a theme in it.
*   **From a file.** In **Sound themes**, under **Install a theme**, choose **From a file**.
*   **From a link.** In **Sound themes**, under **Install a theme**, choose **From a link**, and
    enter or paste a link. It can be a link to a theme file, or to a GitHub repository, such as
    `https://github.com/someone/my-theme`. Links must start with https.

Before installing, Backtalk says who made the theme, what it has, and anything in it that it could
not use. Choose **Install and use** to switch to it at once, or **Install** to keep using the theme
you have. Installing a theme with the same name as one you have replaces it.

## Using themes

Each installed theme is listed in **Sound themes**, and the one in use says so. Select a theme to
use it, to read about it, to save it as a theme file, or to remove it.

Under **Settings of** the theme in use:

*   **Control sounds and vibrations** gives each kind of control, such as buttons, checkboxes
    and edit fields, its own sound and its own vibration, in place of the focus sound and
    vibration, as the [Unspoken](https://github.com/ahicks92/Unspoken) add-on does for NVDA with
    sounds. Backtalk has no control sounds or vibrations of its own, so they come from the theme.
    When a control's sound or vibration plays, Backtalk leaves out saying what kind of control it
    is. A control's vibration plays even with **Sound feedback** off, so you can learn the kinds of
    control by touch alone. A kind of control that the theme gives a vibration but no sound plays
    the focus sound with its own vibration. One the theme gives neither plays the focus sound and
    vibration, and Backtalk says what kind of control it is.
*   **3D audio** sets when control sounds play in 3D: with headphones, always, or never.
*   **Sounds** lists every sound, where you can replace any of them with a sound file of your own,
    preview it, or go back to Backtalk's sound.

Each theme remembers its own **Control sounds and vibrations** and **3D audio** settings, so
switching themes switches them too. A theme starts with the settings in its `theme.json`. **Still
say the kind of control**, in **Sound and vibration**, is the same for every theme.

To replace a few sounds without installing anything, use **Sounds** with the Backtalk theme in use.
To share your sounds, select the theme and choose **Save as a theme file**.

## Making a theme

A theme file is a ZIP file with a `theme.json` and the sound files, all in the same folder. Name
each sound after the sound it replaces, such as `focus.wav`. A theme only needs the sounds it
changes. Every other sound stays Backtalk's own.

    my-theme.zip
        theme.json
        focus.wav
        tick.ogg
        control_button.wav
        LICENSE.txt

The folder can also be inside one folder of the ZIP file, as when GitHub makes a ZIP file of a
repository. So a GitHub repository with `theme.json` and the sounds at the top is a theme too, and
its link installs it.

Files called `LICENSE`, `LICENCE`, `COPYING`, `README`, `NOTICE`, `AUTHORS` or `CREDITS`, with
`.txt`, `.md` or no extension, are kept with the theme, and saved with it when someone shares it
again. Put the license of your sounds there. Backtalk leaves out any other files.

A ZIP file of sounds alone, without a `theme.json`, also installs, named after the file.

### theme.json

```json
{
  "format": 1,
  "name": "Soft",
  "author": "Your name",
  "description": "Quiet, rounded sounds for every action.",
  "license": "CC-BY-4.0",
  "website": "https://github.com/someone/soft",
  "settings": {
    "control_sounds": true,
    "3d_audio": "headphones"
  },
  "vibrations": {
    "focus": { "pattern": [0, 12], "strength": [[12, 90]], "effects": [["tick", 100, 0]] },
    "control_button": { "pattern": [0, 20, 30, 20] },
    "scroll_tone": "none"
  }
}
```

Every field is optional.

| Field | What it is |
| --- | --- |
| `format` | The version of this format, 1. A theme for a newer Backtalk still installs, with a warning. |
| `name` | The theme's name. Without one, the theme is named after its file. A theme with the same name as an installed one replaces it. |
| `author` | Who made it. |
| `description` | A sentence or two about it. |
| `license` | The license of the sounds, such as `GPL-2.0` or `CC0-1.0`. Include its text in the theme too. |
| `website` | Where to find out more, or get updates. |
| `settings.control_sounds` | `true` or `false`: whether control sounds and vibrations are on with this theme. Without it, they are on when the theme has a control sound or a control vibration. |
| `settings.3d_audio` | `"headphones"`, `"always"` or `"never"`: when control sounds play in 3D. Without it, `"headphones"`. |
| `vibrations` | The theme's own vibrations, by the name of the sound they play with. See below. |

### Sounds

Name each sound file after one of these, with the extension of its format. WAV, OGG, MP3, FLAC,
M4A, AAC and Opus all work, up to 5 MB each and 50 MB for the whole theme. A file Android can't
play is left out when the theme is installed, and Backtalk says so.

| Name | Plays for |
| --- | --- |
| `focus` | Focus on an item |
| `focus_actionable` | Focus on an actionable item |
| `view_entered` | Touching an empty area |
| `tick` | Click |
| `long_clicked` | Long press |
| `scroll_tone` | Scroll |
| `chime_up` | Entering a list |
| `chime_down` | Leaving a list |
| `complete` | Action done or end reached |
| `window_state` | Window change |
| `gesture_begin` | Gesture start |
| `gesture_end` | Gesture end |
| `typo` | Spelling error |
| `hyperlink` | Link |
| `formatting` | Formatted text |
| `screen_off` | Screen off |
| `loading` | Loading |
| `browse_mode_on_v4_2` | Browse mode on |
| `browse_mode_off_v4_2` | Browse mode off |
| `radial_menu` | Circle menu (all eight notes play this one sound) |
| `display_connected` | Braille display connected |
| `display_disconnected` | Braille display disconnected |
| `double_beep` | Braille command failed |
| `turn_on` | Braille auto scroll on |
| `turn_off` | Braille auto scroll off |
| `calibration_done` | Braille keyboard calibrated |
| `control_button` | Control sound: button |
| `control_checkbox` | Control sound: checkbox, also switches and toggle buttons |
| `control_radio_button` | Control sound: radio button |
| `control_edit_text` | Control sound: edit field |
| `control_combo_box` | Control sound: drop-down list |
| `control_slider` | Control sound: slider |
| `control_link` | Control sound: link on a web page |
| `control_image` | Control sound: image |
| `control_clock` | Control sound: clock |
| `control_tab` | Control sound: tab |
| `control_menu_item` | Control sound: menu item |
| `control_list_item` | Control sound: list item |
| `control_tree_item` | Control sound: tree item on a web page |
| `braille_keyboard_character` | Braille keyboard: typing a character |
| `braille_keyboard_space` | Braille keyboard: space |
| `braille_keyboard_delete` | Braille keyboard: deleting a character or a word |
| `braille_keyboard_new_line` | Braille keyboard: new line |

The braille keyboard sounds replace Android's keyboard sounds, and play only with **Typing sounds**
on in the braille keyboard's settings.

The names are also shown in Backtalk: in **Sounds**, selecting a sound shows its name after its
title, such as "Click (tick)".

Tips:

*   Keep sounds short. Focus and control sounds play on every swipe, and a new control sound cuts
    off the one before it.
*   Control sounds, and the focus and list sounds while control sounds are on, come from where the
    item is on the screen. They are mixed down to mono for that, so make them mono to hear them as
    you made them.
*   Leave some headroom. Backtalk plays sounds at the volume of its **Sound feedback volume**
    setting, which can't make a quiet sound louder than it is.

### Vibrations

A theme can replace every vibration Backtalk plays, and define each pattern itself. A vibration
the theme leaves out keeps Backtalk's.

Each sound has a vibration, which plays with it even when sound feedback is off. A theme gives a
sound its own vibration by the name of the sound, as in the table above. It then replaces that
vibration everywhere it plays: with the sound, and wherever Backtalk plays the same vibration
without the sound, such as the click vibration of the circle menu and the focus vibration of
selection. `radial_menu` replaces the vibrations of all eight circle menu notes. The braille display
sounds (`display_connected`, `display_disconnected`, `double_beep`, `turn_on`, `turn_off` and
`calibration_done`) have no vibration, so they can't have one in a theme either.

These vibrations have no sound, and a theme replaces them by these names:

| Name | Plays for |
| --- | --- |
| `announcement` | An app's announcement |
| `braille_keyboard_character` | Typing a character on the braille keyboard |
| `braille_keyboard_space` | Space, delete, moving the cursor or changing the reading unit on the braille keyboard |
| `braille_keyboard_new_line` | A new line or deleting a word on the braille keyboard |
| `braille_keyboard_hold` | Holding the fingers down on the braille keyboard |
| `braille_keyboard_gesture` | Other gestures on the braille keyboard |
| `braille_keyboard_nothing_to_delete` | Deleting with nothing left to delete on the braille keyboard |
| `direct_touch_on` | Direct touch turning on |
| `direct_touch_off` | Direct touch turning off |

The braille keyboard vibrations play only with **Vibration feedback** on, and each has its own
switch in **Individual sounds and vibrations**. The direct touch vibrations play only with direct
touch's vibration setting on.

#### Control vibrations

A theme can give each kind of control its own vibration, by the names of the control sounds, such
as `control_button`, so that people can tell what a control is by touch alone. Backtalk has none
of its own. A control vibration:

*   plays in place of the focus vibration, with **Control sounds and vibrations** on;
*   plays even with **Sound feedback** off;
*   leaves the kind of control out of the speech, as a control sound does;
*   works without a control sound: the control then plays the focus sound with its own vibration;
*   has its own switch in **Individual sounds and vibrations**, under **Vibrations**, except for
    links on web pages, which share the switch of the link vibration.

Keep control vibrations short and easy to tell apart, because controls are focused often. Build
them from the same taps, clicks, rises and falls, and give each kind its own rhythm, since some
phones can only play the `pattern` form. For example:

```json
"vibrations": {
  "control_button": {
    "pattern": [0, 15, 40, 15],
    "strength": [[15, 255], [40, 0], [15, 255]],
    "effects": [["click", 255, 0], ["click", 255, 40]]
  },
  "control_checkbox": {
    "pattern": [0, 15, 30, 40],
    "strength": [[15, 255], [30, 0], [40, 160]],
    "effects": [["click", 255, 0], ["quick_rise", 160, 30]]
  },
  "control_list_item": {
    "pattern": [0, 10, 50, 10],
    "effects": [["click", 140, 0], ["click", 140, 50]]
  }
}
```

A vibration is `"none"`, for no vibration, or an object with up to three forms of the same
vibration. Phones play the best form they can.

| Form | What it is |
| --- | --- |
| `pattern` | Off and on times in milliseconds, starting with off: `[0, 20, 40, 20]` waits 0 ms, vibrates 20 ms, waits 40 ms and vibrates 20 ms. Every vibrator can play this. |
| `strength` | Steps of `[milliseconds, strength]`, with strength from 0 (off) to 255 (strongest): `[[20, 255], [40, 0], [20, 100]]`. For vibrators that can vary their strength. |
| `effects` | Steps of `["effect", strength, delay in ms]`, with strength from 0 to 255: `[["click", 255, 0], ["tick", 150, 40]]`. Crisp effects for phones with a good vibration motor. |

A vibration needs `pattern` or `strength`, because every phone can play those. Without `pattern`,
the steps of `strength` with any strength make the pattern.

The effects are `click`, `tick`, `low_tick`, `thud`, `spin`, `quick_rise`, `slow_rise` and
`quick_fall`. A phone that lacks one effect of a vibration plays the `strength` or `pattern` form
instead, and many phones lack `thud` and `spin`, so `click`, `tick`, `quick_rise`, `slow_rise` and
`quick_fall` are the safest.

Times can be up to 5000 ms, and each form up to 64 steps. A vibration that is not well formed is
left out when the theme is installed, and Backtalk says why.

The **Vibrations** switches in **Individual sounds and vibrations** turn off a theme's vibrations
too: the switch of the vibration a theme's vibration replaces turns it off, and plays the theme's
vibration when you preview it.

### Sharing a theme

*   Put the theme in a GitHub repository, with `theme.json` and the sounds at the top, and share
    the repository's link. Anyone can paste it into **From a link**, or share it to Backtalk from
    their browser.
*   Or share the theme file itself, for anyone to open with Backtalk.
*   To make a theme file from sounds on your phone, choose them in **Sounds** and use **Save as a
    theme file**. Its `theme.json` has the theme's settings.

## Limitations

*   Watches have no file picker, so themes can only be installed from a link there, and single
    sounds can't be chosen.
*   The circle menu's eight notes share one theme sound.
*   Links on web pages and links in text share one vibration switch.
