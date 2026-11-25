## Display, overview and quick settings {#display}

**Display** in SandFly settings sets the focus indicator, screen animations and the SandFly wallpaper. This chapter also covers hiding the screen, the screen brightness, the SandFly overview of recent apps (**Overview**), split screen, and the SandFly quick settings panel (**Quick settings**).

### Change the focus indicator {#display-focus-indicator}

The focus indicator is the border that SandFly draws around the item with accessibility focus. Its settings are in **Display** > **Focus indicator**, on Android 12 and later:

- **Show the focus indicator**: draws the border. On by default.
- **Thick borders**: draws a wider border.
- **Color**: the color of the border: **Default**, **Red**, **Orange**, **Yellow**, **Green**, **Blue** or **Gray**.

The **Show the focus indicator** command, in the **SandFly** group of commands, turns the focus indicator on or off. See [](#commands).

### Turn off animations {#display-animations}

**Display** > **Turn off animations** turns off window animations for the whole phone, so that SandFly reads new screens sooner. It is on by default. SandFly turns animations off on Android 13 and later, and sets them back as they were when you turn the setting off or turn SandFly off.

### Hide the screen {#display-hide-screen}

Hide screen turns the display black, including the navigation bar, so that people near you can't see what is on it. SandFly and the apps keep working as usual.

To hide the screen:

1. Open the SandFly menu. See [](#commands).
2. Select **Hide or show screen**.
3. In the **Hide screen?** dialog, select **OK**.

SandFly says "Screen hidden" and plays a sound. To show the screen again, select **Hide or show screen** in the SandFly menu again. SandFly says "Showing screen".

You can also hide and show the screen with:

- The **Hide or show screen** command, in the **SandFly** group of commands, which you can assign to a gesture or keyboard shortcut. See [](#commands).
- The **Hide screen** reading control, off by default. Turn it on in **Reading controls**, then swipe up or down to hide or show the screen.

To stop the dialog from opening each time, turn off **Always show this warning** in the dialog. SandFly then says only "Screen hidden", without the instructions for showing the screen.

With the **Modify system settings** permission, SandFly also sets the screen brightness to its minimum while the screen is hidden, and sets it back when the screen is shown. The first time you hide the screen without that permission, SandFly opens the Android screen to allow it.

Note: The screen stays hidden after SandFly restarts. Hide screen isn't available while the phone is locked. Before Android 14, image descriptions aren't available while the screen is hidden.

To change the sounds for hiding and showing the screen, see [](#sound). To have SandFly say "Screen hidden" when the screen turns on, see [](#verbosity).

### Change the screen brightness {#display-brightness}

The **Brightness** reading control changes the system screen brightness, even while the screen is hidden. It is on by default in **Reading controls**.

1. Swipe up or down to choose the reading control, until SandFly says "Brightness". See [](#reading).
2. Swipe up to make the screen brighter, or swipe down to make it dimmer.

Each step changes the brightness by 10 percent of the system brightness slider, and SandFly says the new level, such as "Brightness 40 percent." SandFly needs the **Modify system settings** permission to change the brightness, and opens the Android screen to allow it the first time. While adaptive brightness is on, SandFly says "Turn off adaptive brightness to change brightness."

### Use SandFly as the wallpaper {#display-wallpaper}

Some phones, such as Vivo phones, stop SandFly in the background even when it is exempt from battery optimization. Android keeps the current wallpaper running, and restarts it if it stops. SandFly has a live wallpaper that runs in SandFly itself, so on these phones SandFly keeps running while it is the wallpaper.

1. Go to **Display** > **Use SandFly as the wallpaper**.
2. On the Android wallpaper screen that opens, set the wallpaper.

While SandFly is the wallpaper, the setting says "SandFly is the wallpaper". The wallpaper is black. To show an image instead, go to **Display** > **Wallpaper image** and choose an image. SandFly says "Wallpaper image set", or "Can't use this image".

These settings are on phones and tablets only.

### The SandFly overview {#display-overview}

The SandFly overview lists the recent apps in a column or grid that SandFly reads in order. It opens in place of the system overview when you turn on **Overview** > **Use SandFly's overview**. The setting is off by default.

The SandFly overview needs Shizuku, or root, to read and open the recent apps. See [](#updates). Without Shizuku, the system overview opens, and SandFly says "Shizuku not running" or "Shizuku doesn't allow SandFly". On the lock screen, the system overview opens.

The other settings in **Overview** are:

- **Most recent app at the bottom**: lists the most recent app last, and opens the list scrolled to the end. Off by default.
- **Columns**: 1 shows a list. 2, 3 or 4 show a grid with that number of columns. The default is 1.

These settings aren't on TVs.

#### Use the overview {#display-overview-use}

With **Use SandFly's overview** on, the **Overview** command, in **System actions**, opens the SandFly overview instead of the system overview. The SandFly overview also opens over the system overview when you open the system overview with the navigation gesture or the Recents button.

In the overview:

- Double-tap an app to switch to it.
- Open the Actions menu on an app for **Close**, **Split screen** and **App info**. **Split screen** shows the app in split screen beside the app the overview was opened from. **App info** opens the Android App info page of the app.
- On a physical keyboard, press Delete to close the focused app.
- **Close all**, at the bottom left, closes all recent apps.
- **System overview**, at the bottom right, opens the system overview.

Run the **Overview** command again while the overview is open to switch to the previous app. The **System overview** command opens the system overview even when **Use SandFly's overview** is on.

#### Switch between recent apps {#display-switch-apps}

These commands are in **System actions**, unassigned by default. See [](#commands).

- **Previous app**: switches to the app used before the current one. SandFly says "No previous app" when there is none. Without Shizuku, SandFly opens the system overview and switches to the previous app there.
- **Older app**: opens the next older app in the list of recent apps. Run it again to go further back. SandFly says the name of each app, and "No older app" at the end of the list. Without Shizuku, Older app does the same as Previous app.
- **Newer app**: goes back toward the most recent app, after **Older app**. SandFly says "No newer app" at the most recent app. Newer app needs Shizuku.

With **Use SandFly's overview** on and Shizuku running, Alt+Tab on a physical keyboard switches apps without opening a window:

1. Hold Alt and press Tab. SandFly says the name of the app before the current one.
2. Press Tab again to move to older apps, or Shift+Tab to move back.
3. Release Alt to switch to the app SandFly said. To stay in the current app, press Escape before you release Alt.

Alt+Tab isn't available when Alt is the SandFly key. See [](#keyboards).

#### Split screen {#display-split-screen}

To open two apps in split screen, use **Split screen** in the Actions menu of an app in the SandFly overview. See [](#display-overview-use).

While split screen is shown, the SandFly menu has a **Split screen** item, with **Go to divider** and the actions of the divider, such as its sizes and swapping the apps. The **Split screen** command, in **System actions**, opens the same menu, and says "Not in split screen" when no split screen is shown. In split screen, the Back key and typed keys go to the app that you last moved focus in.

### The SandFly quick settings panel {#display-quick-settings}

The SandFly quick settings panel is a grid of switches, sliders and buttons that you choose and arrange. It opens in place of the system quick settings when you turn on **Quick settings** > **Use SandFly's quick settings**. The setting is off by default.

#### Use the quick settings panel {#display-quick-settings-use}

The **Quick Settings** command, in **System actions**, opens the panel. The **Quick settings** button in the SandFly notification list also opens it. See [](#notifications). On the lock screen, the system quick settings open instead.

In the panel:

- SandFly says the row and column of each item, its name and its state, such as what Wi-Fi is connected to.
- Double-tap a switch to turn it on or off. Double-tap an item with a few values, such as **Sound mode** or **Screen timeout**, to move to the next value.
- Swipe up or down on a slider, such as **Brightness** or **Media volume**, to move it one step.
- Double-tap and hold an item, or use its **Open settings** action, to open the Android settings for it.
- **Edit** opens the screen that chooses the items. See [](#display-quick-settings-edit).
- **System quick settings** opens the system quick settings.

The **System quick settings** command opens the system quick settings even when **Use SandFly's quick settings** is on.

Android lets only the system and the shell change some settings. With Shizuku running, SandFly turns Wi-Fi, Bluetooth, mobile data, the hotspot, airplane mode, location, NFC, battery saver, dark theme, data saver, and microphone and camera access on and off itself, and gives itself the permissions it needs for the other controls. Without Shizuku, these switches open the Android settings for them. See [](#updates).

Tiles of the system, and tiles of apps without Shizuku, work only if they are in the system quick settings. SandFly opens the system quick settings, activates the tile, closes them without reading them, and then says the new state of the tile. SandFly says "<name> isn't in the system's quick settings" when it can't find the tile.

#### Choose what the panel shows {#display-quick-settings-edit}

Go to **Quick settings** > **Quick settings panel**, or select **Edit** in the panel. The screen lists the items in the panel, each with its row and column.

- **Add control**: lists the controls of SandFly that aren't in the panel: Wi-Fi, Bluetooth, mobile data, hotspot, airplane mode, data saver, Do Not Disturb, sound mode, flashlight, flashlight brightness, location, auto-rotate, battery saver, dark theme, Night Light, Extra dim, color inversion, color correction, NFC, microphone access, camera access, brightness, adaptive brightness, screen timeout, font size, display size, media, media output, the media, call, ring, notification, alarm, accessibility and system volumes, next alarm, battery, Settings and power menu. Controls that the phone doesn't have aren't listed.
- **Add app tile**: lists the quick settings tiles that installed apps offer.
- **Add system tile**: lists the tiles of the system quick settings. A system tile that SandFly has a control for, such as Hotspot, is added as that control. With Shizuku running, SandFly reads the list from the system; otherwise it looks through the system quick settings, which takes a few seconds.
- **Columns**: the number of columns in the grid, from 1 to 4. The default is 2.

To move or remove an item, double-tap it, or use its actions:

- **Move**: lists the cells of the grid. The item takes the cell you choose, and the items between move one cell to make room. SandFly says how many items moved.
- **Remove**: removes the item from the panel.

By default, the panel has Wi-Fi, Bluetooth, mobile data, airplane mode, Do Not Disturb, sound mode, flashlight, location, auto-rotate, battery saver, brightness, media volume, ring volume, accessibility volume, media and battery.
