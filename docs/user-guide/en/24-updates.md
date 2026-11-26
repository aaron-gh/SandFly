## Updates, Shizuku and root {#updates}

SandFly updates itself from the SandFly releases on GitHub. The settings for updates are in **Updates and installation**.

Some features need permissions that Android grants only through adb, the Android Debug Bridge. SandFly can get them through Shizuku, or through root on a rooted device. The setting for root is in **Shizuku and root**.

### Stable releases and development builds {#updates-channels}

SandFly is published in two kinds of builds:

- Stable releases, named by version, such as SandFly 0.6.0. They are on the [SandFly releases page](https://github.com/aaron-gh/SandFly/releases).
- Development builds, built from each change to SandFly. They get changes sooner than stable releases, and are tested less.

Each build has a build number, which goes up with each change. **Updates and installation** > **Check for updates** shows the version and build number of the installed SandFly, such as "Version 0.6.1, build 1234".

By default, SandFly offers only stable releases. To get development builds too, turn on **Updates and installation** > **Development builds**. SandFly then offers whichever build is newer, a development build or a stable release, and checks for updates about once an hour instead of once a day.

### Check for updates {#updates-check}

1. Go to **Updates and installation**.
2. Choose **Check for updates**.

SandFly shows "Checking for updates", then one of these:

- "SandFly is up to date".
- A dialog **SandFly build** *number* **is available**, with the changes in the new build. For a stable release, the changes are the release notes. For a development build, they are the changes since the installed build. Choose **Install** to install it, or **Cancel**.
- "Could not check for updates", when GitHub can't be reached. Try again later.

While a new development build is being published, the check waits for it and tries again, for up to a minute.

### Automatic update checks {#updates-automatic}

**Updates and installation** > **Automatically check for updates** is on by default. While SandFly is on, it checks for a new build 30 seconds after it starts and then once a day, or once an hour with **Development builds** on. When a new build is available, SandFly shows the notification **SandFly update available**, with the changes. Each build is announced once.

The notification needs permission for SandFly to show notifications. If notifications are off for SandFly, the build is announced when they are turned on.

Turn off **Automatically check for updates** to check only with **Check for updates**.

### Install an update {#updates-install}

1. Tap the **SandFly update available** notification, or choose **Install** in the dialog of **Check for updates**.
2. The first time, Android opens the setting that allows SandFly to install apps, and SandFly shows "Allow SandFly to install apps, then try the update again". Turn on the setting, go back, and repeat step 1.
3. SandFly shows "Downloading update" and the progress of the download in a notification: the percentage, and after a few seconds the time left, such as "40%, 1 minute, 20 seconds left".
4. When the download is complete, the Android install prompt opens. Choose **Update** or **Install**.

SandFly restarts with the new build, with the same settings. If the install prompt doesn't open, such as when the screen turned off during the download, tap the notification **SandFly update downloaded**. The notification goes away after 10 minutes.

SandFly installs only builds of SandFly signed with the SandFly key. If the download is something else, SandFly shows "The download is not a build of this app, so it was not installed". When the connection drops, SandFly retries up to five times, and continues the download where it stopped. If the download still fails, SandFly shows "Could not download the update". Installing the same build again, before SandFly restarts, continues the download.

A build of SandFly that you build yourself is signed with another key. Android doesn't install a SandFly release over it, or the other way around. Uninstall one before you install the other.

On a watch, see [](#devices-watch-updates).

### Appear installed from Google Play {#updates-play-installer}

Some apps, such as some banking apps, turn off accessibility services that Android records as installed from anywhere other than Google Play. **Updates and installation** > **Appear installed from Google Play** makes Android record Google Play as the installer of SandFly, so that those apps keep SandFly on. The setting is off by default and needs Shizuku or root.

When you turn on the setting, SandFly installs its own APK again through Shizuku, naming Google Play as the installer. SandFly restarts during the install, so speech stops for a moment. The app and its settings stay the same. Only what other apps are told about the installer changes. An app can still find out in other ways that SandFly wasn't installed from Google Play.

While the setting is on:

- SandFly installs updates through Shizuku, so that Google Play stays the recorded installer. If that fails, SandFly installs the update with the Android install prompt.
- If something records another installer, such as an update installed while Shizuku wasn't running, SandFly records Google Play again the next time Shizuku allows SandFly. Until then, the setting shows "On, but Google Play isn't recorded as the installer yet. SandFly sets it when Shizuku is available."

Turning the setting off records the installer that SandFly had before.

Without Shizuku, the setting can't change the installer. Its **Copy command** action copies an adb command that does the same from a computer, in the folder that has `sandfly.apk`:

```
adb install -r -i com.android.vending sandfly.apk
```

To record the installer as a normal install again, run `adb install -r sandfly.apk`.

### Shizuku {#updates-shizuku}

[Shizuku](https://shizuku.rikka.app) is an app that runs commands for other apps with the permissions of adb. With Shizuku running and allowing SandFly, SandFly grants itself permissions that Android gives only through adb, and runs commands that only adb can run. SandFly needs Shizuku version 11 or later.

#### Set up Shizuku {#updates-shizuku-setup}

1. Install Shizuku and start it, as the Shizuku app describes. On Android 11 and later, Shizuku can start with wireless debugging, without a computer. On a rooted device, Shizuku can also start with root.
2. Turn on SandFly, or turn it off and on again if it is on.
3. When SandFly starts with Shizuku running, Shizuku shows a dialog to allow SandFly. Choose to allow it.

SandFly requests Shizuku access only once by itself. After that, the Shizuku dialog opens when you turn on a feature that needs Shizuku. If you chose to deny SandFly and not ask again, allow SandFly in the Shizuku app.

Shizuku started with wireless debugging or adb stops when the device restarts. Start it again in the Shizuku app.

#### Features that use Shizuku or root {#updates-shizuku-features}

Without Shizuku or root, these features are unavailable or work in a limited way, as described in their chapters:

- **Touch** > **Fix stuck touch exploration**, on by default, needs the DUMP permission to fix touch exploration when Android leaves a touch down. Without it, SandFly says only that touch exploration may be stuck. See [](#touch).
- **Audio output** > **Speaker when away from your ear** needs permission to manage calls. Requires Android 12 or later. See [](#sound).
- **Audio output** > **Reset accessibility audio after calls** works only through Shizuku or root. Requires Android 13 or later. See [](#sound).
- **Volume keys** > **Volume key shortcuts with the screen off** works only through Shizuku, not root. See [](#buttons).
- **Overview** > **Use SandFly's overview** reads and opens the recent apps through Shizuku or root. Without them, the overview of the system opens instead. Alt+Tab, opening apps in split screen, and the **Older app** and **Newer app** commands also need Shizuku or root. See [](#display).
- The switches of the SandFly quick settings panel, such as Wi-Fi, Bluetooth, mobile data and airplane mode, and its brightness, auto-rotate, night light, extra dim, color and display size controls. Without Shizuku or root, some switches open the settings of the system instead. See [](#display).
- The clipboard history adds text copied in other apps only through Shizuku or root, with **Add copies from other apps** in the history. See [](#typing).
- Gboard plays its own key feedback, such as **Vibrate on keypress**, while you explore its keys only through Shizuku or root. See [](#typing).
- Commands that run the accessibility shortcut of another accessibility service. See [](#commands).
- The **App tiles** and **System tiles** commands, and **Add system tile** in the quick settings panel settings. Without Shizuku or root, a tile works only if it is in the system quick settings, and **Add system tile** looks through the system quick settings to find the tiles. See [](#commands-categories) and [](#display-quick-settings).
- App shortcuts that an app adds while it runs, and shortcuts that only the home screen app may open, are listed only through Shizuku or root. See [](#gestures-app-shortcuts).
- Scripts and macros that press keys, or type text where there is no text field, and macro steps that change the quick settings controls listed above. See [](#scripts) and [](#macros).
- **Updates and installation** > **Appear installed from Google Play**. See [](#updates-play-installer).

When SandFly starts with Shizuku or root allowed, it grants itself the permissions that the features you have turned on need.

#### Grant permissions with adb {#updates-adb}

Without Shizuku, a setting that needs an adb permission shows the adb command in its description, and its **Copy command** action copies the command to the clipboard. Run the command from a computer with adb, with the device connected and USB debugging or wireless debugging on, then go back to the setting.

The commands are:

- For **Fix stuck touch exploration**:

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
  ```

- For **Speaker when away from your ear**:

  ```
  adb shell appops set io.github.aaron_gh.sandfly MANAGE_ONGOING_CALLS allow
  ```

- For moving accessibility shortcuts from Backtalk Aaron. See [](#getting-started-backtalk).

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
  ```

**Reset accessibility audio after calls** has no permission that adb can grant. Without Shizuku, the setting shows an adb command that resets the accessibility audio output once, for when speech plays on the earpiece after a call. The command names the installed APK, so copy it again after SandFly updates.

### Use root instead of Shizuku {#updates-root}

On a rooted device, SandFly can run its commands with root instead of Shizuku. Turn on **Shizuku and root** > **Use root instead of Shizuku**. The setting is off by default.

SandFly then runs the commands through `su`, as the shell user, the user that adb and Shizuku use. The root manager of the device, such as Magisk or KernelSU, shows a dialog to grant root to SandFly. Features that need Shizuku use root while the setting is on, except **Volume key shortcuts with the screen off**, which isn't shown in root mode. When `su` doesn't grant root, the overview and split screen say "No root access".

Note: If `su` on the device can't run commands as the shell user, SandFly runs them as root. Adding copies from other apps to the clipboard history and the hotspot switch don't work then.
