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
import android.content.pm.PackageManager
import com.google.android.accessibility.talkback.BuildConfig
import rikka.shizuku.Shizuku

/**
 * Shizuku, which the full build uses itself: Shizuku allows SandFly, and runs SandFly's own
 * [ShizukuGrantService]. The Google Play build has its own ShellAccess, which goes through the
 * SandFly add-on.
 *
 * Everything here runs on the main thread.
 */
internal object ShellAccess {
  private const val REQUEST_CODE = 0x5F17

  /** Whether SandFly has asked Shizuku for access by itself at start, which it does only once. */
  const val PREF_ASKED_AT_START = "shizuku_asked_at_start"

  /** Gets the user's answer to [requestPermission]. */
  var permissionListener: ((Boolean) -> Unit)? = null

  /** Called when Shizuku stops. */
  var deadListener: (() -> Unit)? = null

  private val readyListeners = HashMap<() -> Unit, Shizuku.OnBinderReceivedListener>()

  init {
    Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
      if (requestCode == REQUEST_CODE) {
        permissionListener?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
      }
    }
    Shizuku.addBinderDeadListener { deadListener?.invoke() }
  }

  /** Whether this build can use Shizuku, which the full build always can. */
  fun isAvailable(context: Context): Boolean = true

  /** Whether Shizuku is running and new enough. */
  fun isReady(): Boolean =
    try {
      Shizuku.pingBinder() && !Shizuku.isPreV11()
    } catch (e: RuntimeException) {
      false
    }

  /** Whether Shizuku allows SandFly. */
  fun hasPermission(): Boolean =
    try {
      Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: RuntimeException) {
      false
    }

  /** Whether the user chose "Deny and don't ask again" in Shizuku. Throws without Shizuku. */
  fun isPermissionDenied(): Boolean = Shizuku.shouldShowRequestPermissionRationale()

  /** Shows Shizuku's request to allow SandFly. Throws without Shizuku. */
  fun requestPermission() {
    Shizuku.requestPermission(REQUEST_CODE)
  }

  /**
   * Calls [listener] each time Shizuku starts, while it is added, and at once if Shizuku runs.
   * Throws if it can't listen.
   */
  fun addReadyListener(context: Context, listener: () -> Unit) {
    val received = Shizuku.OnBinderReceivedListener { listener() }
    readyListeners[listener] = received
    Shizuku.addBinderReceivedListenerSticky(received)
  }

  fun removeReadyListener(listener: () -> Unit) {
    readyListeners.remove(listener)?.let { Shizuku.removeBinderReceivedListener(it) }
  }

  /**
   * Starts SandFly's shell service, in a process named after [tag], for [connection]. Returns what
   * [unbind] takes. Throws if Shizuku can't start it.
   */
  fun bind(context: Context, tag: String, connection: ServiceConnection): Any {
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
    @Suppress("UNCHECKED_CAST") val (args, connection) = binding as Pair<Shizuku.UserServiceArgs, ServiceConnection>
    Shizuku.unbindUserService(args, connection, true)
  }
}
