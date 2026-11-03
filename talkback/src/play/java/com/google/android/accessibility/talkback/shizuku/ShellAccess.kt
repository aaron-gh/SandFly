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

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PatternMatcher
import android.os.RemoteException
import androidx.core.content.ContextCompat
import com.google.android.libraries.accessibility.utils.log.LogUtils
import io.github.aaron_gh.sandfly.addon.AddonContract
import io.github.aaron_gh.sandfly.addon.IAddon
import io.github.aaron_gh.sandfly.addon.IAddonListener
import io.github.aaron_gh.sandfly.addon.IShellConnection

/**
 * Shizuku, through the SandFly add-on, which the Google Play build uses instead of Shizuku itself:
 * Shizuku allows the add-on, and runs the add-on's shell service, whose binder the add-on gives to
 * SandFly. Without the add-on, Shizuku is treated as not running.
 *
 * SandFly binds to the add-on while the service runs, if the add-on is signed with SandFly's key,
 * and binds again when the add-on is installed or updated.
 *
 * Everything here runs on the main thread.
 */
internal object ShellAccess {
  private const val TAG = "ShellAccess"

  /** Gets the user's answer to [requestPermission]. */
  var permissionListener: ((Boolean) -> Unit)? = null

  /** Called when Shizuku or the add-on stops. */
  var deadListener: (() -> Unit)? = null

  private val mainHandler = Handler(Looper.getMainLooper())
  private val readyListeners = ArrayList<() -> Unit>()
  private var context: Context? = null
  private var connection: ServiceConnection? = null
  private var addon: IAddon? = null
  private var packageReceiver: BroadcastReceiver? = null

  private val addonListener =
    object : IAddonListener.Stub() {
      override fun onShizukuReady() {
        mainHandler.post { if (addon != null) tellReady() }
      }

      override fun onShizukuDead() {
        mainHandler.post { deadListener?.invoke() }
      }

      override fun onPermissionResult(granted: Boolean) {
        mainHandler.post { permissionListener?.invoke(granted) }
      }
    }

  /** Whether the add-on is connected, and Shizuku is running and new enough. */
  fun isReady(): Boolean = ask(false) { it.isShizukuReady() }

  /** Whether Shizuku allows the add-on. */
  fun hasPermission(): Boolean = ask(false) { it.hasShizukuPermission() }

  /** Whether the user chose "Deny and don't ask again" for the add-on in Shizuku. */
  fun isPermissionDenied(): Boolean = call { it.isShizukuPermissionDenied() }

  /** Shows Shizuku's request to allow the add-on. Throws without the add-on. */
  fun requestPermission() {
    call { it.requestShizukuPermission() }
  }

  /**
   * Connects to the add-on, and calls [listener] each time Shizuku starts, while it is added, and
   * at once if Shizuku runs.
   */
  fun addReadyListener(context: Context, listener: () -> Unit) {
    readyListeners += listener
    start(context.applicationContext)
    if (isReady()) listener()
  }

  /** Removes [listener], and disconnects from the add-on after the last one. */
  fun removeReadyListener(listener: () -> Unit) {
    readyListeners -= listener
    if (readyListeners.isEmpty()) stop()
  }

  /**
   * Starts the add-on's shell service, in a process named after [tag], for [connection]. Returns
   * what [unbind] takes. Throws without the add-on.
   */
  fun bind(context: Context, tag: String, connection: ServiceConnection): Any {
    val shellConnection =
      object : IShellConnection.Stub() {
        private var connected = false

        override fun onConnected(shell: IBinder?) {
          mainHandler.post {
            val alive =
              try {
                shell?.linkToDeath({ mainHandler.post { disconnected() } }, 0)
                shell != null
              } catch (e: RemoteException) {
                false
              }
            connected = alive
            connection.onServiceConnected(null, shell.takeIf { alive })
          }
        }

        override fun onDisconnected() {
          mainHandler.post { disconnected() }
        }

        override fun onFailed(error: String?) {
          LogUtils.w(TAG, "The add-on can't start the shell service: %s", error)
          mainHandler.post { connection.onServiceConnected(null, null) }
        }

        /** Tells [connection] once, whether the add-on said so or the shell process died. */
        private fun disconnected() {
          if (!connected) return
          connected = false
          connection.onServiceDisconnected(null)
        }
      }
    call { it.bindShell(tag, shellConnection) }
    return shellConnection
  }

  /** Stops the service that [bind] started, and its process. */
  fun unbind(binding: Any) {
    ask(Unit) { it.unbindShell(binding as IShellConnection) }
  }

  private fun start(appContext: Context) {
    if (context != null) return
    context = appContext
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          if (intent.data?.schemeSpecificPart != AddonContract.PACKAGE) return
          if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
            if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) disconnect()
          } else {
            bindAddon()
          }
        }
      }
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addDataScheme("package")
        addDataSchemeSpecificPart(AddonContract.PACKAGE, PatternMatcher.PATTERN_LITERAL)
      }
    ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    packageReceiver = receiver
    bindAddon()
  }

  private fun stop() {
    val appContext = context ?: return
    packageReceiver?.let { appContext.unregisterReceiver(it) }
    packageReceiver = null
    disconnect()
    context = null
  }

  /** Binds to the add-on's service, if the add-on is installed and signed with SandFly's key. */
  private fun bindAddon() {
    val appContext = context ?: return
    if (connection != null) return
    if (!isSignedLikeSandFly(appContext)) return
    val serviceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
          if (connection !== this) return
          val connected = binder?.let { IAddon.Stub.asInterface(it) } ?: return
          try {
            connected.setListener(addonListener)
          } catch (e: RemoteException) {
            return
          }
          addon = connected
          if (isReady()) tellReady()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
          if (connection !== this) return
          // Android connects again when the add-on's service starts again.
          addon = null
          deadListener?.invoke()
        }

        override fun onBindingDied(name: ComponentName?) {
          if (connection !== this) return
          disconnect()
          bindAddon()
        }

        override fun onNullBinding(name: ComponentName?) {
          if (connection !== this) return
          disconnect()
        }
      }
    connection = serviceConnection
    val intent = Intent(AddonContract.ACTION_BIND).setPackage(AddonContract.PACKAGE)
    val bound =
      try {
        appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
      } catch (e: SecurityException) {
        LogUtils.w(TAG, "Can't bind to the add-on: %s", e)
        false
      }
    if (!bound) {
      disconnect()
    }
  }

  /** Unbinds from the add-on, which stops its shell services. */
  private fun disconnect() {
    val serviceConnection = connection ?: return
    val wasConnected = addon != null
    try {
      addon?.setListener(null)
    } catch (e: RemoteException) {
      // The add-on has stopped.
    }
    connection = null
    addon = null
    try {
      context?.unbindService(serviceConnection)
    } catch (e: IllegalArgumentException) {
      // It wasn't bound.
    }
    if (wasConnected) deadListener?.invoke()
  }

  private fun isSignedLikeSandFly(context: Context): Boolean =
    context.packageManager.checkSignatures(context.packageName, AddonContract.PACKAGE) ==
      PackageManager.SIGNATURE_MATCH

  private fun tellReady() {
    ArrayList(readyListeners).forEach { it() }
  }

  /** Calls the add-on, or gives [default] if it isn't connected or the call fails. */
  private inline fun <T> ask(default: T, work: (IAddon) -> T): T {
    val connected = addon ?: return default
    return try {
      work(connected)
    } catch (e: RemoteException) {
      default
    } catch (e: RuntimeException) {
      default
    }
  }

  /** Calls the add-on. Throws if it isn't connected or the call fails. */
  private inline fun <T> call(work: (IAddon) -> T): T {
    val connected = addon ?: throw IllegalStateException("The SandFly add-on isn't connected")
    return try {
      work(connected)
    } catch (e: RemoteException) {
      throw IllegalStateException(e)
    }
  }
}
