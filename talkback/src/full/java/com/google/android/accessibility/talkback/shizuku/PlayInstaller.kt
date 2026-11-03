/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.shizuku

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.content.edit
import androidx.core.util.Consumer
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File

/**
 * Changes the package Android records as having installed SandFly. Some apps, such as some banking
 * apps, turn off accessibility services that Android records as installed from anywhere but Google
 * Play, which stops a blind user who installed SandFly from a release using it. At the user's
 * choice, and only for SandFly itself, this records Google Play as the installer so those apps keep
 * working.
 *
 * Android only lets the installer be recorded when an app is installed, and only by the shell or
 * the system, so SandFly reinstalls its own APK through Shizuku, naming Google Play. The app is the
 * same one the user installed; only what apps are told about where it came from changes. Reinstall-
 * ing restarts SandFly, so speech stops for a moment. The in-app updater does the same install
 * through Shizuku while this is on, so an update keeps the recorded installer.
 */
object PlayInstaller {
  /** Google Play's package, which apps look for when they check the installer. */
  const val PLAY = "com.android.vending"

  /** The real installer, kept so that turning the setting off puts it back. */
  private const val PREF_REAL_INSTALLER = "real_installer"

  /** Whether the user wants Google Play recorded, kept so SandFly can restore it after a reset. */
  private const val PREF_WANTED = "play_installer_wanted"

  /** Whether SandFly has tried to restore the installer this run, so it does so at most once. */
  @Volatile private var reapplied = false

  /** Whether Android records Google Play as SandFly's installer now. */
  fun isPlay(context: Context): Boolean = installer(context) == PLAY

  /** Whether the user has turned the setting on, whatever the installer reads now. */
  fun isWanted(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context).getBoolean(PREF_WANTED, false)

  @Suppress("DEPRECATION") // getInstallerPackageName is the only way before Android 11.
  private fun installer(context: Context): String? =
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
      } else {
        context.packageManager.getInstallerPackageName(context.packageName)
      }
    } catch (e: RuntimeException) {
      null
    }

  /** The adb command that a user without Shizuku can run themselves to do the same thing. */
  fun command(context: Context, play: Boolean): String =
    if (play) "adb install -r -i $PLAY sandfly.apk" else "adb install -r sandfly.apk"

  /**
   * Reinstalls SandFly through Shizuku so that Android records Google Play as the installer, or
   * the real one again. [onDone] gets whether it worked, and false at once without Shizuku. The
   * real installer is kept the first time, so that turning the setting off restores it.
   *
   * SandFly restarts during the reinstall, so [onDone] may not run; the setting reads the result
   * the next time it is shown.
   */
  fun set(context: Context, play: Boolean, onDone: Consumer<Boolean>) {
    val appContext = context.applicationContext
    val prefs = SharedPreferencesUtils.getSharedPreferences(appContext)
    if (play && !isPlay(appContext)) {
      prefs.edit { putString(PREF_REAL_INSTALLER, installer(appContext).orEmpty()) }
    }
    // Kept before the reinstall, which restarts SandFly, so the choice survives and can be
    // restored after an update or a reset.
    prefs.edit { putBoolean(PREF_WANTED, play) }
    install(appContext, target(appContext, play)) { worked -> onDone.accept(worked) }
  }

  /**
   * Restores Google Play as the installer if the user wants it and it isn't recorded now, which
   * happens after an update that Shizuku couldn't do. Does nothing, and never prompts, unless
   * Shizuku already allows SandFly. Runs at most once per process, since the reinstall restarts
   * SandFly and this runs again then.
   */
  fun reapplyIfNeeded(context: Context) {
    val appContext = context.applicationContext
    if (reapplied || !isWanted(appContext) || isPlay(appContext) || !ShizukuShell.isAllowed()) {
      return
    }
    reapplied = true
    install(appContext, PLAY) { worked ->
      LogUtils.i("PlayInstaller", "Restored Google Play as the installer: %s", worked)
    }
  }

  /**
   * Installs a downloaded [apk] through Shizuku, recording Google Play as the installer, so that
   * an in-app update keeps the recorded installer while the setting is on. [onDone] gets whether
   * it worked. Used only when [isEnabled] and Shizuku allows SandFly.
   */
  fun installUpdateAsPlay(context: Context, apk: File, onDone: Consumer<Boolean>) {
    val appContext = context.applicationContext
    val descriptor =
      try {
        ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY)
      } catch (e: Exception) {
        onDone.accept(false)
        return
      }
    ShizukuShell.install(
      appContext,
      listOf(descriptor),
      listOf(apk.length()),
      appContext.packageName,
      PLAY,
    ) { worked -> onDone.accept(worked) }
  }

  /**
   * Whether the setting is on, so that the in-app updater installs updates through Shizuku. It
   * follows the user's choice, not the installer now, so an update restores Google Play even if a
   * reset dropped it.
   */
  fun isEnabled(context: Context): Boolean = isWanted(context)

  /** Reinstalls SandFly's own APKs through Shizuku, recording [installer]. */
  private fun install(context: Context, installer: String, onDone: (Boolean) -> Unit) {
    val apks = apkFiles(context)
    if (apks.isEmpty()) {
      onDone(false)
      return
    }
    val descriptors =
      try {
        apks.map { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
      } catch (e: Exception) {
        onDone(false)
        return
      }
    ShizukuShell.install(context, descriptors, apks.map { it.length() }, context.packageName, installer, onDone)
  }

  /** SandFly's installed APKs: the base, and the splits if the release has any. */
  private fun apkFiles(context: Context): List<File> {
    val info = context.applicationInfo
    val paths = mutableListOf(info.sourceDir)
    info.splitSourceDirs?.let { paths.addAll(it) }
    return paths.filterNotNull().map { File(it) }.filter { it.exists() }
  }

  /** The installer to record: Google Play, or the real one kept before, or SandFly itself. */
  private fun target(context: Context, play: Boolean): String {
    if (play) {
      return PLAY
    }
    val real =
      SharedPreferencesUtils.getSharedPreferences(context).getString(PREF_REAL_INSTALLER, null)
    // The install needs an installer name, so without a real one SandFly names itself, which is
    // clearly not Google Play.
    return real?.takeIf { it.isNotEmpty() } ?: context.packageName
  }
}
