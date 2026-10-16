/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.update

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.IntentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.migration.AppIdMove
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.PreferenceSettingsUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Checks for new development builds, shows a notification when one is available, and installs it.
 *
 * Automatic checks run when the service starts, and then about once an hour while it runs, since
 * this fork's builds are tried as soon as they are pushed.
 *
 * While this app has the old app ID, a build under the app ID fyi.quin.backtalk installs next to
 * it. The accessibility settings for the new app then open, so the user can turn it on.
 */
object Updater {
  private const val TAG = "Updater"
  private const val NOTIFICATION_ID = 0x0BAC7A1C
  internal const val PREF_LAST_CHECK_TIME = "update_last_check_time"
  internal const val PREF_LAST_NOTIFIED_BUILD = "update_last_notified_build"
  private const val APK_FILE_NAME = "backtalk.apk"
  private val CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(1)
  private val POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15)
  // Gives the network time to come up when the service starts at boot.
  private val STARTUP_DELAY_MS = TimeUnit.SECONDS.toMillis(30)
  // Lets the screen that opens be announced before the toast.
  private const val TOAST_AFTER_SCREEN_DELAY_MS = 1500L
  // Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS and its permission, which the SDK hides.
  private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
    "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
  private const val PERMISSION_OPEN_DETAILS =
    "android.permission.OPEN_ACCESSIBILITY_DETAILS_SETTINGS"

  private val mainHandler = Handler(Looper.getMainLooper())
  private val executor = Executors.newSingleThreadExecutor()
  private var serviceContext: Context? = null

  private val startupCheck = Runnable { serviceContext?.let { checkAutomatically(it, true) } }

  private val periodicCheck =
    object : Runnable {
      override fun run() {
        val context = serviceContext ?: return
        checkAutomatically(context, false)
        mainHandler.postDelayed(this, POLL_INTERVAL_MS)
      }
    }

  /** Starts automatic checks. Called when the service starts. */
  @JvmStatic
  fun startAutomaticChecks(context: Context) {
    serviceContext = context.applicationContext
    mainHandler.removeCallbacks(startupCheck)
    mainHandler.removeCallbacks(periodicCheck)
    mainHandler.postDelayed(startupCheck, STARTUP_DELAY_MS)
    mainHandler.postDelayed(periodicCheck, POLL_INTERVAL_MS)
  }

  /** Stops automatic checks. Called when the service stops. */
  @JvmStatic
  fun stopAutomaticChecks() {
    mainHandler.removeCallbacks(startupCheck)
    mainHandler.removeCallbacks(periodicCheck)
    serviceContext = null
  }

  /** Sets up the "Check for updates" preference in the main settings screen. */
  @JvmStatic
  fun setUpPreferences(fragment: PreferenceFragmentCompat) {
    val preference =
      fragment.findPreference<Preference>(fragment.getString(R.string.pref_check_for_updates_key))
        ?: return
    preference.summary =
      fragment.getString(R.string.summary_pref_check_for_updates, BuildConfig.COMMIT_COUNT)
    preference.setOnPreferenceClickListener {
      fragment.activity?.let { checkManually(it) }
      true
    }
  }

  private fun isAutomaticCheckEnabled(context: Context): Boolean =
    SharedPreferencesUtils.getBooleanPref(
      SharedPreferencesUtils.getSharedPreferences(context),
      context.resources,
      R.string.pref_auto_update_check_key,
      R.bool.pref_auto_update_check_default,
    )

  private fun checkAutomatically(context: Context, ignoreInterval: Boolean) {
    if (!isAutomaticCheckEnabled(context)) {
      return
    }
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val now = System.currentTimeMillis()
    val lastCheck = prefs.getLong(PREF_LAST_CHECK_TIME, 0)
    // A clock that moved back also makes a check due.
    if (!ignoreInterval && now >= lastCheck && now - lastCheck < CHECK_INTERVAL_MS) {
      return
    }
    executor.execute {
      val update =
        try {
          UpdateChecker.check(context)
        } catch (e: Exception) {
          LogUtils.w(TAG, "Automatic update check failed: %s", e)
          return@execute
        }
      prefs.edit().putLong(PREF_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
      if (update != null && update.build > prefs.getInt(PREF_LAST_NOTIFIED_BUILD, 0)) {
        // A notification that Android won't show doesn't count, so the build is still announced
        // once notifications are allowed.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
          LogUtils.w(TAG, "Build %d is available, but notifications are off", update.build)
          return@execute
        }
        prefs.edit().putInt(PREF_LAST_NOTIFIED_BUILD, update.build).apply()
        showNotification(context, update)
      }
    }
  }

  /** Checks now, and offers to install a newer build. */
  @JvmStatic
  fun checkManually(activity: Activity) {
    val context = activity.applicationContext
    toast(context, R.string.update_checking)
    executor.execute {
      val update =
        try {
          UpdateChecker.check(context)
        } catch (e: Exception) {
          LogUtils.w(TAG, "Update check failed: %s", e)
          mainHandler.post { toast(context, R.string.update_check_failed) }
          return@execute
        }
      SharedPreferencesUtils.getSharedPreferences(context)
        .edit()
        .putLong(PREF_LAST_CHECK_TIME, System.currentTimeMillis())
        .apply()
      mainHandler.post {
        when {
          update == null -> toast(context, R.string.update_up_to_date)
          activity.isFinishing || activity.isDestroyed -> showNotification(context, update)
          else -> showUpdateDialog(activity, update)
        }
      }
    }
  }

  private fun notificationText(context: Context, update: UpdateInfo): String =
    if (update.move) {
      context.getString(R.string.update_move_notification_text)
    } else {
      context.getString(R.string.update_notification_text, update.build)
    }

  private fun describe(context: Context, update: UpdateInfo): String =
    (listOf(notificationText(context, update)) + update.changes).joinToString("\n")

  private fun showUpdateDialog(activity: Activity, update: UpdateInfo) {
    val title: String
    val message: String
    if (update.move) {
      title = activity.getString(R.string.update_move_title)
      message = activity.getString(R.string.update_move_dialog_message)
    } else {
      title = activity.getString(R.string.update_dialog_title, update.build)
      message =
        update.changes
          .joinToString("\n")
          .ifEmpty { activity.getString(R.string.update_notification_text, update.build) }
    }
    A11yAlertDialogWrapper.materialDialogBuilder(activity)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton(R.string.update_dialog_install) { _, _ ->
        install(activity, update.downloadUrl)
      }
      .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
      .setCancelable(true)
      .create()
      .show()
  }

  private fun showNotification(context: Context, update: UpdateInfo) {
    val intent =
      Intent(context, UpdateReceiver::class.java)
        .setAction(UpdateReceiver.ACTION_INSTALL)
        .putExtra(UpdateReceiver.EXTRA_DOWNLOAD_URL, update.downloadUrl)
    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val title =
      context.getString(
        if (update.move) R.string.update_move_title else R.string.update_notification_title
      )
    val text = notificationText(context, update)
    val notification =
      NotificationUtils.createDefaultNotificationBuilder(context)
        .setTicker(title)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(describe(context, update)))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
  }

  /**
   * Downloads the APK and opens the system install prompt. A build of this app updates it, and
   * Backtalk under its new app ID installs as a new app. Anything else is refused.
   */
  @JvmStatic
  fun install(context: Context, downloadUrl: String) {
    val appContext = context.applicationContext
    if (!appContext.packageManager.canRequestPackageInstalls()) {
      val intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:" + appContext.packageName),
          )
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      try {
        appContext.startActivity(intent)
      } catch (e: ActivityNotFoundException) {
        LogUtils.w(TAG, "Cannot open the install apps setting: %s", e)
      }
      mainHandler.postDelayed(
        { toast(appContext, R.string.update_allow_install, Toast.LENGTH_LONG) },
        TOAST_AFTER_SCREEN_DELAY_MS,
      )
      return
    }
    toast(appContext, R.string.update_downloading)
    executor.execute {
      try {
        val apk = download(appContext, downloadUrl)
        try {
          installApk(appContext, apk)
        } finally {
          apk.delete()
        }
      } catch (e: WrongPackageException) {
        LogUtils.e(TAG, "Refused the update: %s", e.message)
        mainHandler.post { toast(appContext, R.string.update_wrong_package, Toast.LENGTH_LONG) }
      } catch (e: Exception) {
        LogUtils.e(TAG, "Cannot install the update: %s", e)
        mainHandler.post { toast(appContext, R.string.update_install_failed) }
      }
    }
  }

  private fun download(context: Context, url: String): File {
    val directory = File(context.cacheDir, "update")
    directory.mkdirs()
    val file = File(directory, APK_FILE_NAME)
    val connection = UpdateChecker.openConnection(context, url)
    try {
      val code = connection.responseCode
      if (code != HttpURLConnection.HTTP_OK) {
        throw IOException("Download returned HTTP $code")
      }
      connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
    } finally {
      connection.disconnect()
    }
    return file
  }

  private class WrongPackageException(message: String) : Exception(message)

  private fun installApk(context: Context, apk: File) {
    val info = readArchive(context, apk)
    val apkPackage = info?.packageName
    val kind = AppIdMove.installKind(apkPackage, context.packageName, isSameSigner(context, info))
    when (kind) {
      AppIdMove.InstallKind.REFUSE ->
        throw WrongPackageException("The APK is $apkPackage, and this app is ${context.packageName}")
      AppIdMove.InstallKind.MOVE -> {
        LogUtils.i(TAG, "Installing Backtalk under its new app ID %s", apkPackage)
        mainHandler.post { toast(context, R.string.update_move_installing, Toast.LENGTH_LONG) }
      }
      AppIdMove.InstallKind.UPDATE -> {}
    }
    val installer = context.packageManager.packageInstaller
    // The session names the APK's app ID: this app's for an update, and the new one for a move,
    // which installs as a new app next to this one.
    val params =
      PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
        setAppPackageName(apkPackage)
      }
    val sessionId = installer.createSession(params)
    try {
      installer.openSession(sessionId).use { session ->
        session.openWrite(APK_FILE_NAME, 0, apk.length()).use { output ->
          apk.inputStream().use { it.copyTo(output) }
          session.fsync(output)
        }
        val intent =
          Intent(context, UpdateReceiver::class.java)
            .setAction(UpdateReceiver.ACTION_INSTALL_STATUS)
            .putExtra(UpdateReceiver.EXTRA_INSTALLED_PACKAGE, apkPackage)
        // The installer fills in the status, so the intent must be mutable.
        val flags =
          PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val statusIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
        session.commit(statusIntent.intentSender)
      }
    } catch (e: Exception) {
      installer.abandonSession(sessionId)
      throw e
    }
  }

  /** Reads the app ID and, where Android can, the signing certificates of a downloaded APK. */
  private fun readArchive(context: Context, apk: File): PackageInfo? {
    val flags =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
      } else {
        0
      }
    return context.packageManager.getPackageArchiveInfo(apk.path, flags)
  }

  /**
   * Returns false only if the APK is known to be signed with a different key than this app. When
   * Android cannot read the APK's certificates, the system still checks the key of an update, and
   * the new app checks the old one's key before it takes any settings.
   */
  private fun isSameSigner(context: Context, info: PackageInfo?): Boolean {
    if (info == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return true
    }
    val signers = info.signingInfo?.apkContentsSigners
    if (signers.isNullOrEmpty()) {
      return true
    }
    return signers.all {
      context.packageManager.hasSigningCertificate(
        context.packageName,
        it.toByteArray(),
        PackageManager.CERT_INPUT_RAW_X509,
      )
    }
  }

  /** Handles a status update from the install session. */
  fun onInstallStatus(context: Context, intent: Intent) {
    val appContext = context.applicationContext
    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
    when (status) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        val confirmIntent =
          IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
        if (confirmIntent == null) {
          toast(appContext, R.string.update_install_failed)
          return
        }
        try {
          appContext.startActivity(confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
          LogUtils.e(TAG, "Cannot open the install prompt: %s", e)
          toast(appContext, R.string.update_install_failed)
        }
      }
      // For an update, the system stops Backtalk to replace it, and restarts the service if it
      // was on. A move installs a new app, and this one keeps running until that one is on.
      PackageInstaller.STATUS_SUCCESS -> {
        val installed = intent.getStringExtra(UpdateReceiver.EXTRA_INSTALLED_PACKAGE)
        if (installed != null && installed != appContext.packageName) {
          onMoved(appContext, installed)
        }
      }
      PackageInstaller.STATUS_FAILURE_ABORTED -> toast(appContext, R.string.update_install_canceled)
      else -> {
        LogUtils.e(
          TAG,
          "Install failed with status %d: %s",
          status,
          intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
        )
        toast(appContext, R.string.update_install_failed)
      }
    }
  }

  /** Opens the accessibility settings for the newly installed Backtalk, so the user turns it on. */
  private fun onMoved(context: Context, packageName: String) {
    LogUtils.i(TAG, "Installed Backtalk under its new app ID %s", packageName)
    val component = ComponentName(packageName, AppIdMove.SERVICE_CLASS)
    val intents = buildList {
      // The screen for a single service is on Android 12 and later, but Android lets only the
      // system installer open it. Some phones may grant it, so try it when it is granted.
      if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
          context.checkSelfPermission(PERMISSION_OPEN_DETAILS) == PackageManager.PERMISSION_GRANTED
      ) {
        add(
          Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS)
            .putExtra(Intent.EXTRA_COMPONENT_NAME, component.flattenToString())
        )
      }
      // Otherwise the accessibility settings open, and Pixel phones highlight the new app.
      add(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).also {
          PreferenceSettingsUtils.attachSettingsHighlightBundle(it, component)
        }
      )
    }
    for (intent in intents) {
      try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        break
      } catch (e: ActivityNotFoundException) {
        LogUtils.w(TAG, "Cannot open %s: %s", intent.action, e)
      } catch (e: SecurityException) {
        LogUtils.w(TAG, "Cannot open %s: %s", intent.action, e)
      }
    }
    mainHandler.postDelayed(
      { toast(context, R.string.update_move_turn_on, Toast.LENGTH_LONG) },
      TOAST_AFTER_SCREEN_DELAY_MS,
    )
  }

  private fun toast(context: Context, @StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, message, duration).show()
  }
}
