## Settings profiles and backups {#profiles}

A settings profile is a named set of settings that SandFly uses in place of your settings while the profile is in use, everywhere or only in some apps and situations. **Back up and restore** saves all SandFly settings to a file and restores them from one. Item files hold single kinds of items, such as macros or gesture profiles, to share them or move them to another phone.

For gesture profiles, which change only gestures, see [](#gestures). For voice profiles, see [](#speech).

### Settings profiles {#profiles-settings-profiles}

A settings profile holds only the settings changed in it. Every other setting stays as it is without profiles. **Settings profiles**, in SandFly settings, lists the profiles, and marks the profiles in use with **In use**.

A profile can be used in two ways:

- **Use now** uses the profile everywhere, until you stop it. One profile at a time is used this way. See [](#profiles-use).
- **Use automatically** and **Apps** use the profile in web content, during continuous reading, while editing text, in the apps you choose, or while a keyboard you choose is shown. See [](#profiles-automatic).

A profile can hold the main SandFly settings, the braille keyboard settings and the braille display settings, including gestures, keyboard shortcuts, gesture profiles and the gesture profile in use. A profile doesn't hold API keys, the SandFly Pro license, the time of the last update check, installed scripts and their settings, sound theme drafts, or the state of this phone, such as which tutorials and dialogs were shown.

### BlackBerry mode {#profiles-blackberry-mode}

**BlackBerry mode**, at the bottom of **Settings profiles**, makes SandFly work similarly to the BlackBerry 10 Screen Reader. Its gestures, keys and sounds are likely to be unfamiliar if you haven't used BlackBerry 10. Turning it on opens the **Turn on BlackBerry mode?** dialog; select **Turn on** to confirm. The first time, SandFly makes a settings profile, **BlackBerry mode**, and uses it now. The profile sets:

- **Gestures** > **Gesture layout** to **BlackBerry gestures**, with **Handle two-finger swipes in SandFly** on. See [](#gestures-layout).
- On keyboard phones, the **BlackBerry keymap**. See [](#keyboards-phones).
- The **BlackBerry** sound theme, which SandFly installs. The theme has SandFly sounds, not BlackBerry 10 sounds: no sound when focus moves, and sounds for screen changes, clicks, scrolling, the end of a list, errors and the keyboard showing or hiding. **Control sounds and vibrations** are off. See [](#sound).
- No usage hints, list and grid positions, actions available or keyboard shown and hidden announcements. Kinds of control are still said, and pitch changes are on.

Every other setting stays as it is. To change what BlackBerry mode sets, change the settings of the **BlackBerry mode** profile; see [](#profiles-change-settings). Turning BlackBerry mode off stops using the profile, and turning it on again uses the profile with your changes. To start again from the defaults, delete the profile. **Turn BlackBerry mode on or off**, under **SandFly** on the **Commands** screen, does the same from a gesture or a key, and also opens the dialog before turning it on.

BlackBerry mode is used everywhere, as **Use now** uses a profile, so it replaces another profile used that way.

### Add a settings profile {#profiles-add}

1. Go to **Settings profiles** > **New settings profile**.
2. Enter a name, or leave the field empty to use the suggested name, such as "Profile 2".
3. Select **OK**.

The new profile sets nothing yet. To add settings to it, see [](#profiles-change-settings).

### Change the settings of a profile {#profiles-change-settings}

1. Go to **Settings profiles** and select the profile.
2. Select **Change settings**.

SandFly says "Changes go to <name>" and opens SandFly settings with the profile applied. Every setting you change goes to the profile. When you go back to the screen of the profile or to **Settings profiles**, or open another app, SandFly says "Changes saved to <name>".

A setting changed while a profile in use sets it, such as the speech rate changed with the reading controls, is also saved to that profile. Other settings that you change apply without profiles.

To see the settings of a profile, go to the screen of the profile. **Settings in this profile** lists each setting by its title and its place in SandFly settings. To remove a setting from the profile, select it, then select **Remove**. The setting then has its value without the profile.

### Use a profile now {#profiles-use}

1. Go to **Settings profiles** and select the profile.
2. Select **Use now**.

The profile is used everywhere, including SandFly settings. To stop using it, select **Stop using** on the same screen.

These commands, in the **SandFly** group of commands, choose the profile used everywhere. They are unassigned by default. See [](#commands).

- **Choose settings profile**: opens a menu with **No settings profile** and the profiles.
- **Next settings profile** and **Previous settings profile**: move through the profiles and **No settings profile**.

SandFly says the name of the profile, such as "Reading profile", or "No settings profile".

### Use a profile automatically {#profiles-automatic}

On the screen of a profile:

- **Use automatically** > **Web content**: uses the profile while focus is in web content.
- **Use automatically** > **Continuous reading**: uses the profile during continuous reading, such as Read from top.
- **Use automatically** > **Text editing**: uses the profile while focus is in an edit field, or on the keyboard that types in it.
- **Apps** > **Add app**: uses the profile while the app you choose is shown. To remove an app, select it, then select **Remove**.
- **Keyboards** > **Add keyboard**: uses the profile while the keyboard you choose is shown. The list has the keyboards turned on in Android settings. To remove a keyboard, select it, then select **Remove**.

Profiles used automatically aren't used in SandFly settings, so that SandFly settings show your settings without them. They also aren't used while SandFly is off.

#### When profiles set the same setting {#profiles-order}

Several profiles can be in use at once. When they set the same setting, the profile used with **Use now** wins, then the profile higher in the list in **Settings profiles**. To change the order, use **Move up** and **Move down** on the screen of a profile.

### Rename, move or delete a profile {#profiles-manage}

The screen of a profile also has:

- **Rename**: changes the name of the profile.
- **Move up** and **Move down**: move the profile in the list.
- **Export to file**: saves the profile to an item file. See [](#profiles-item-files).
- **Delete**: deletes the profile, after you confirm.

### Back up and restore settings {#profiles-backup}

**Back up and restore** saves all SandFly settings and your own content to a file and replaces them with those in a file, such as to set up another phone the same way. These settings aren't on watches.

#### Export settings {#profiles-export}

1. Go to **Back up and restore** > **Export settings**.
2. Choose where to save the file. The suggested name is `sandfly-settings.zip`.

SandFly says "Settings exported. API keys are not included".

The .zip file holds every SandFly setting, and:

- Custom labels.
- Settings profiles. The settings are saved as they are without profiles, and the profiles separately.
- Installed scripts.
- Sound themes, and the sounds you changed in the Backtalk theme.
- The wallpaper image.

It leaves out:

- API keys, so that you can share or sync the file.
- The SandFly Pro license. On another phone, activate SandFly Pro as a device of its own. See [](#pro).
- The time of the last update check.
- Downloaded AI and speech recognition models, logs and problem reports.
- Sound theme drafts.

#### Import settings {#profiles-import}

1. Go to **Back up and restore** > **Import settings**.
2. Choose the settings file.

The settings in the file replace your settings, and SandFly says "Settings imported". Settings that the file doesn't hold, and the API keys on this phone, stay as they are. The settings profiles in use are applied again over the imported settings.

Custom labels, settings profiles, scripts and sound themes in the file are added. Each replaces the one on this phone for the same item, name or ID; others stay. The wallpaper image in the file replaces this phone's.

**Import settings** also reads a settings file with the .json extension, exported by an earlier version of SandFly, and a settings file exported from Backtalk. A Backtalk file's settings replace those of SandFly, and settings that only SandFly has stay as they are.

If SandFly says "This file is not a SandFly or Backtalk settings file, or could not be read", the file isn't a settings file or is damaged, a .json file is larger than 5 MB, or a .zip file is larger than 300 MB unpacked. Nothing is changed.

### Item files {#profiles-item-files}

An item file holds SandFly items of one or more kinds, without other settings. You can export items, share them, edit them, write them by hand or with an AI chat, and import them on any phone with SandFly. An item file isn't a settings file: **Import settings** doesn't read it, and **Import from file** doesn't read a settings file.

Each of these screens has **Export to file** and **Import from file**:

| Items | Screen |
| --- | --- |
| Macros | **Macros** |
| Gesture profiles | **Gestures** > **Gesture profiles** |
| Gestures in apps | **Gestures** > **Gestures in apps** |
| Keyboard shortcuts in apps | **Keyboard shortcuts** > **Keyboard shortcuts in apps** |
| Keymaps | **Keyboard shortcuts** (exports the keymap in use) |
| Settings profiles | **Settings profiles** |
| Speech dictionary | **Speech** > **Speech dictionary** |
| Symbol dictionary | **Speech** > **Symbol dictionary** |
| Braille dictionary | **Braille** > **Braille dictionary** |
| Custom braille characters | **Braille** > **Braille keyboard** > **Custom braille characters** |
| Custom voice commands | **Voice commands** |

Item files aren't available on watches.

#### Export items to a file {#profiles-item-export}

1. Go to the screen of the items.
2. Select **Export to file**.
3. Choose where to save the file.

**Export to file** on a list screen saves every item in the list. On the screen of a single item, such as a settings profile, it saves that item. SandFly says "Exported", or "Could not export".

#### Import items from a file {#profiles-item-import}

1. Go to any of the screens in the table in [](#profiles-item-files).
2. Select **Import from file**.
3. Choose the item file.

SandFly imports every item in the file, whichever screen you import it from, and says what it imported, such as "Imported 2 macros, 1 gesture profile". An imported item replaces the item of the same kind with the same name, ignoring case. Other items stay. Dictionary entries replace entries with the same text to find in the same app. Custom braille characters replace those with the same text in the same braille table. Custom voice commands replace those with the same phrase. Imported keymaps are always custom keymaps.

After an import, SandFly lists what it left out, such as unknown commands, gestures, keys and settings, and regular expressions it refuses. If the file is a settings file, SandFly says "This is a settings file. Import it with Back up and restore > Import settings".

#### Write an item file {#profiles-item-reference}

**Back up and restore** > **Export file format reference** saves a document, `sandfly-file-format.md`, that describes item files, followed by the gestures, commands and settings of this phone, with the value each setting has now. To have an AI chat write items for you, give it this file with a description of the items, save the reply as a file, and import it with **Import from file**.

The format is described in [](#api-item-files).
