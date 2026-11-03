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

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.Settings
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.distribution.Distribution
import com.google.android.accessibility.talkback.quicksettings.QuickSettingsItems
import com.google.android.accessibility.talkback.quicksettings.SystemTiles
import com.google.android.accessibility.talkback.speakerphone.SpeakerphoneSettings
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.Executors

/**
 * Grants the permissions that some settings need, which Android only gives through adb, with
 * Shizuku when it is installed and running. The first time, Shizuku asks the user to allow
 * SandFly, or in the Google Play build the SandFly add-on, through which that build uses Shizuku
 * (see [ShellAccess]). Without Shizuku, the settings keep their adb commands.
 *
 * Everything here runs on the main thread, except the grant itself.
 */
object ShizukuGrants {
  private const val TAG = "ShizukuGrants"
  private const val CONNECT_TIMEOUT_MS = 15_000L
  /** Whether SandFly has asked Shizuku for access by itself at start, which it does only once. */
  private const val PREF_ASKED_AT_START = "shizuku_asked_at_start"

  /** The permissions that SandFly can grant itself through Shizuku. */
  enum class Grant {
    /** Lets "Fix stuck touch exploration" find and cancel the stuck touch. */
    DUMP,
    /** Lets "Speakerphone away from ear" move calls to the speaker. */
    MANAGE_ONGOING_CALLS,
    /** Lets the quick settings panel change brightness and auto-rotate. */
    WRITE_SETTINGS,
    /** With DUMP, lets the quick settings panel read the state of apps' tiles. */
    USAGE_STATS,
    /** Lets the quick settings panel change night light, extra dim and the color settings. */
    WRITE_SECURE_SETTINGS;

    /** The shell service's grant. */
    val shellGrant: ShellGrant
      get() = ShellGrant.valueOf(name)

    /** The command that grants it, as adb would run it after `adb shell`. */
    fun shellCommand(packageName: String): List<String> = shellGrant.shellCommand(packageName)

    fun isGranted(context: Context): Boolean =
      when (this) {
        DUMP ->
          context.checkSelfPermission(Manifest.permission.DUMP) ==
            PackageManager.PERMISSION_GRANTED
        MANAGE_ONGOING_CALLS -> SpeakerphoneSettings.canControlCalls(context)
        WRITE_SETTINGS -> Settings.System.canWrite(context)
        USAGE_STATS -> hasUsageAccess(context)
        WRITE_SECURE_SETTINGS ->
          context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED
      }
  }

  /**
   * Whether SandFly may dump system services that show app usage, as `dumpsys activity` does:
   * Android checks the permission, and then that the user hasn't turned usage access off.
   */
  @Suppress("DEPRECATION") // unsafeCheckOpNoThrow needs Android 10.
  private fun hasUsageAccess(context: Context): Boolean {
    if (context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }
    val appOps = context.getSystemService(AppOpsManager::class.java)
    val mode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
      } else {
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
      }
    return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_DEFAULT
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  private val executor = Executors.newSingleThreadExecutor()
  private val waiting = HashMap<Grant, MutableList<(Boolean) -> Unit>>()
  private var permissionCallbacks = mutableListOf<(Boolean) -> Unit>()
  private var serviceContext: Context? = null

  private val binderReceived: () -> Unit = {
    failPendingRequests()
    onShizukuReady()
  }

  init {
    ShellAccess.permissionListener = { granted ->
      val callbacks = permissionCallbacks
      permissionCallbacks = mutableListOf()
      callbacks.forEach { it(granted) }
      if (granted) {
        serviceContext?.let(ShizukuClipboard::start)
      }
    }
    ShellAccess.deadListener = {
      failPendingRequests()
      ShizukuClipboard.stop()
    }
  }

  /**
   * Answers no to the requests waiting for the user's answer in Shizuku, when the Shizuku that was
   * asked has stopped or been replaced and the answer can't come. Otherwise they would wait for
   * good, and no request after them would be sent.
   */
  private fun failPendingRequests() {
    if (permissionCallbacks.isEmpty()) return
    val callbacks = permissionCallbacks
    permissionCallbacks = mutableListOf()
    callbacks.forEach { it(false) }
  }

  /** Whether Shizuku is running and new enough, and in the Google Play build the add-on too. */
  fun isReady(): Boolean = ShellAccess.isReady()

  /**
   * Grants [grant] through Shizuku, asking the user to allow SandFly in Shizuku first if needed.
   * [onDone] gets whether SandFly has the permission afterwards, and false at once without
   * Shizuku, or when the user told Shizuku not to ask again.
   */
  fun request(context: Context, grant: Grant, onDone: (Boolean) -> Unit) {
    val appContext = context.applicationContext
    if (grant.isGranted(appContext)) {
      onDone(true)
      return
    }
    if (!isReady()) {
      onDone(false)
      return
    }
    waiting[grant]?.let {
      it += onDone
      return
    }
    waiting[grant] = mutableListOf(onDone)
    withShizukuPermission { allowed ->
      if (allowed) {
        runGrant(appContext, grant)
      } else {
        finish(appContext, grant)
      }
    }
  }

  /**
   * Called when the service starts. When Shizuku is or becomes ready, SandFly asks the user to
   * allow it in Shizuku, once, if Shizuku doesn't already, and then grants itself what the
   * features that are on need and it doesn't have.
   */
  @JvmStatic
  fun onServiceConnected(context: Context) {
    serviceContext = context.applicationContext
    try {
      ShellAccess.addReadyListener(context.applicationContext, binderReceived)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot listen for Shizuku: %s", e)
    }
  }

  @JvmStatic
  fun onServiceDestroyed() {
    serviceContext = null
    ShizukuClipboard.stop()
    ShellAccess.removeReadyListener(binderReceived)
  }

  private fun onShizukuReady() {
    val context = serviceContext ?: return
    if (!isReady()) return
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    if (!hasShizukuPermission() && !askAtStart(prefs)) return
    withShizukuPermission { allowed ->
      if (allowed) {
        ShizukuClipboard.start(context)
        requestAll(context, wantedAtStart(context, prefs)) { granted ->
          LogUtils.i(TAG, "Granted at start through Shizuku: %s", granted)
        }
        Distribution.onShizukuAllowed(context)
      }
    }
  }

  /**
   * The permissions that the features the user has on need and SandFly doesn't have: DUMP for
   * touch exploration fixes, and DUMP and usage access for the quick settings panel's tiles.
   */
  private fun wantedAtStart(context: Context, prefs: SharedPreferences): List<Grant> {
    val fixStuckTouch =
      SharedPreferencesUtils.getBooleanPref(
        prefs,
        context.resources,
        R.string.pref_fix_stuck_touch_exploration_key,
        R.bool.pref_fix_stuck_touch_exploration_default,
      )
    val wanted = ArrayList<Grant>()
    if (fixStuckTouch) wanted += Grant.DUMP
    if (QuickSettingsItems.isEnabled(context)) wanted += SystemTiles.GRANTS
    return wanted.distinct().filterNot { it.isGranted(context) }
  }

  /**
   * Grants [grants] one after another, since each grant starts and stops Shizuku's service.
   * [onDone] gets the ones that were granted.
   */
  fun requestAll(context: Context, grants: List<Grant>, onDone: (List<Grant>) -> Unit) {
    val granted = ArrayList<Grant>()
    fun next(index: Int) {
      val grant = grants.getOrNull(index)
      if (grant == null) {
        onDone(granted)
        return
      }
      request(context, grant) { ok ->
        if (ok) granted += grant
        next(index + 1)
      }
    }
    next(0)
  }

  private fun askAtStart(prefs: SharedPreferences): Boolean {
    if (prefs.getBoolean(PREF_ASKED_AT_START, false)) return false
    prefs.edit().putBoolean(PREF_ASKED_AT_START, true).apply()
    return true
  }

  fun hasShizukuPermission(): Boolean = ShellAccess.hasPermission()

  /** Calls [onResult] with whether Shizuku allows SandFly, asking the user first if needed. */
  fun withShizukuPermission(onResult: (Boolean) -> Unit) {
    try {
      when {
        hasShizukuPermission() -> onResult(true)
        // The user chose "Deny and don't ask again" in Shizuku.
        ShellAccess.isPermissionDenied() -> onResult(false)
        else -> {
          permissionCallbacks += onResult
          if (permissionCallbacks.size == 1) ShellAccess.requestPermission()
        }
      }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot ask Shizuku for access: %s", e)
      permissionCallbacks.remove(onResult)
      onResult(false)
    }
  }

  private fun runGrant(context: Context, grant: Grant) {
    var binding: Any? = null
    var done = false
    lateinit var connection: ServiceConnection
    val timeout = Runnable {
      if (!done) {
        done = true
        LogUtils.w(TAG, "Shizuku did not start the grant service")
        unbind(binding)
        finish(context, grant)
      }
    }
    connection =
      object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
          if (done) {
            // It started after the timeout, so nothing waits for it.
            unbind(binding)
            return
          }
          done = true
          mainHandler.removeCallbacks(timeout)
          val service = binder?.takeIf { it.pingBinder() }?.let { IShizukuGrants.Stub.asInterface(it) }
          executor.execute {
            val error =
              try {
                // The service gives null when the grant worked.
                if (service == null) "No grant service"
                else service.grant(grant.shellGrant.ordinal, context.packageName)
              } catch (e: Exception) {
                e.toString()
              }
            mainHandler.post {
              if (error != null) LogUtils.w(TAG, "Cannot grant %s: %s", grant, error)
              unbind(binding)
              finish(context, grant)
            }
          }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
      }
    try {
      binding = ShellAccess.bind(context, "grants", connection)
      mainHandler.postDelayed(timeout, CONNECT_TIMEOUT_MS)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot start the grant service: %s", e)
      done = true
      finish(context, grant)
    }
  }

  private fun unbind(binding: Any?) {
    if (binding == null) return
    try {
      // Removing it also stops its process.
      ShellAccess.unbind(binding)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot stop the grant service: %s", e)
    }
  }

  private fun finish(context: Context, grant: Grant) {
    val granted = grant.isGranted(context)
    waiting.remove(grant)?.forEach { it(granted) }
  }
}
