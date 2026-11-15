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

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import rikka.shizuku.Shizuku

/**
 * Shizuku or root, which the full build uses itself: Shizuku allows SandFly, and runs SandFly's own
 * [ShizukuGrantService], or, with Use root instead of Shizuku on, su runs it through [RootShell].
 * The Google Play build has its own ShellAccess, which goes through the SandFly add-on.
 *
 * In root mode, "ready" means the switch is on, and "permission" means su gave SandFly root the
 * last time it was asked.
 *
 * Everything here runs on the main thread.
 */
internal object ShellAccess {
  private const val REQUEST_CODE = 0x5F17

  /** Whether SandFly has asked Shizuku for access by itself at start, which it does only once. */
  const val PREF_ASKED_AT_START = "shizuku_asked_at_start"

  /** Gets the user's answer to [requestPermission]. */
  var permissionListener: ((Boolean) -> Unit)? = null

  /** Called when Shizuku stops, or SandFly switches between Shizuku and root. */
  var deadListener: (() -> Unit)? = null

  private val readyListeners = LinkedHashMap<() -> Unit, Shizuku.OnBinderReceivedListener>()

  private var prefs: SharedPreferences? = null
  private var rootKey: String? = null
  /** Whether su gave SandFly root when last asked, in root mode. */
  private var rootGranted = false

  private val prefListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == rootKey) onModeChanged()
    }

  init {
    Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
      if (requestCode == REQUEST_CODE && !usesRoot()) {
        permissionListener?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
      }
    }
    Shizuku.addBinderDeadListener { if (!usesRoot()) deadListener?.invoke() }
  }

  /** Whether this build can use Shizuku or root, which the full build always can. */
  fun isAvailable(context: Context): Boolean {
    remember(context)
    return true
  }

  /** Whether SandFly uses root instead of Shizuku. */
  fun usesRoot(): Boolean {
    val key = rootKey ?: return false
    return prefs?.getBoolean(key, false) == true
  }

  /** Reads the root switch from [context]'s settings, and follows it from then on. */
  private fun remember(context: Context) {
    if (prefs != null) return
    val appContext = context.applicationContext
    rootKey = appContext.getString(R.string.pref_use_root_key)
    prefs = SharedPreferencesUtils.getSharedPreferences(appContext).also {
      it.registerOnSharedPreferenceChangeListener(prefListener)
    }
  }

  /** Whether Shizuku is running and new enough, or root mode is on. */
  fun isReady(): Boolean =
    usesRoot() ||
      try {
        Shizuku.pingBinder() && !Shizuku.isPreV11()
      } catch (e: RuntimeException) {
        false
      }

  /** Whether Shizuku allows SandFly, or su gave it root. */
  fun hasPermission(): Boolean =
    if (usesRoot()) {
      rootGranted
    } else {
      try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
      } catch (e: RuntimeException) {
        false
      }
    }

  /**
   * Whether the user chose "Deny and don't ask again" in Shizuku. Throws without Shizuku. Root
   * managers keep their own answers, so su is always asked.
   */
  fun isPermissionDenied(): Boolean = !usesRoot() && Shizuku.shouldShowRequestPermissionRationale()

  /** Shows Shizuku's request to allow SandFly, or asks su for root. Throws without Shizuku. */
  fun requestPermission() {
    if (usesRoot()) {
      RootShell.check { granted ->
        if (!usesRoot()) return@check
        rootGranted = granted
        permissionListener?.invoke(granted)
      }
      return
    }
    Shizuku.requestPermission(REQUEST_CODE)
  }

  /**
   * Calls [listener] each time Shizuku starts, while it is added, and at once if Shizuku runs. In
   * root mode, calls it once su gives root, without asking for it again. Throws if it can't listen.
   */
  fun addReadyListener(context: Context, listener: () -> Unit) {
    remember(context)
    val received = Shizuku.OnBinderReceivedListener { if (!usesRoot()) listener() }
    readyListeners[listener] = received
    Shizuku.addBinderReceivedListenerSticky(received)
    if (usesRoot()) checkRoot()
  }

  fun removeReadyListener(listener: () -> Unit) {
    readyListeners.remove(listener)?.let { Shizuku.removeBinderReceivedListener(it) }
  }

  /** Asks su for root, and tells the ready listeners if it gives it. */
  private fun checkRoot() {
    RootShell.check { granted ->
      if (!usesRoot()) return@check
      rootGranted = granted
      if (granted) tellReady()
    }
  }

  /**
   * Root mode was turned on or off: what Shizuku or su started is treated as stopped, and the
   * other is used from now on.
   */
  private fun onModeChanged() {
    rootGranted = false
    deadListener?.invoke()
    if (usesRoot()) {
      checkRoot()
    } else if (isReady()) {
      tellReady()
    }
  }

  private fun tellReady() {
    ArrayList(readyListeners.keys).forEach { it() }
  }

  /**
   * Starts SandFly's shell service, in a process named after [tag], for [connection], through
   * Shizuku or su. Returns what [unbind] takes. Throws if Shizuku can't start it.
   */
  fun bind(context: Context, tag: String, connection: ServiceConnection): Any {
    remember(context)
    if (usesRoot()) return RootShell.bind(context, tag, connection)
    val args =
      Shizuku.UserServiceArgs(ComponentName(context.packageName, ShizukuGrantService::class.java.name))
        .daemon(false)
        .processNameSuffix(tag)
        .tag(tag)
        .version(BuildConfig.COMMIT_COUNT)
    Shizuku.bindUserService(args, connection)
    return args to connection
  }

  /** Stops the service that [bind] started, and its process. Throws if Shizuku can't. */
  fun unbind(binding: Any) {
    if (RootShell.owns(binding)) {
      RootShell.unbind(binding)
      return
    }
    @Suppress("UNCHECKED_CAST") val (args, connection) = binding as Pair<Shizuku.UserServiceArgs, ServiceConnection>
    Shizuku.unbindUserService(args, connection, true)
  }
}
