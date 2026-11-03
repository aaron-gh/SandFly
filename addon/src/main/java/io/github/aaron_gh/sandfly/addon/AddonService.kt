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

package io.github.aaron_gh.sandfly.addon

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import java.io.FileDescriptor
import java.io.PrintWriter
import com.google.android.accessibility.talkback.shizuku.ShizukuGrantService
import rikka.shizuku.Shizuku

/**
 * The service that SandFly's Google Play build binds to. It tells SandFly Shizuku's state, asks the
 * user to allow the add-on in Shizuku, and starts the add-on's shell service through Shizuku,
 * giving its binder to SandFly. SandFly then calls the shell service itself.
 *
 * Only apps signed with SandFly's key may bind to it (see the manifest). Calls arrive on binder
 * threads; Shizuku is called on the main thread.
 */
class AddonService : Service() {

  private val mainHandler = Handler(Looper.getMainLooper())
  @Volatile private var listener: IAddonListener? = null

  /** The shell services started for SandFly, by the binder of the connection SandFly gave. */
  private val shells = HashMap<IBinder, Shell>()

  private class Shell(
    val args: Shizuku.UserServiceArgs,
    val connection: ServiceConnection,
    val death: IBinder.DeathRecipient,
  )

  private val binderReceived = Shizuku.OnBinderReceivedListener { tell { it.onShizukuReady() } }

  private val binderDead = Shizuku.OnBinderDeadListener { tell { it.onShizukuDead() } }

  private val permissionResult =
    Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
      if (requestCode == AddonShizuku.REQUEST_CODE) {
        tell { it.onPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED) }
      }
    }

  private val binder =
    object : IAddon.Stub() {
      override fun interfaceVersion(): Int = AddonContract.INTERFACE_VERSION

      override fun isShizukuReady(): Boolean = AddonShizuku.isReady()

      override fun hasShizukuPermission(): Boolean = AddonShizuku.hasPermission()

      override fun isShizukuPermissionDenied(): Boolean = AddonShizuku.isPermissionDenied()

      override fun requestShizukuPermission() {
        mainHandler.post { requestPermission() }
      }

      override fun setListener(listener: IAddonListener?) {
        this@AddonService.listener = listener
      }

      override fun bindShell(tag: String?, connection: IShellConnection?) {
        if (connection == null) return
        mainHandler.post { startShell(tag, connection) }
      }

      override fun unbindShell(connection: IShellConnection?) {
        if (connection == null) return
        mainHandler.post { stopShell(connection.asBinder()) }
      }
    }

  override fun onCreate() {
    super.onCreate()
    Shizuku.addBinderReceivedListener(binderReceived)
    Shizuku.addBinderDeadListener(binderDead)
    Shizuku.addRequestPermissionResultListener(permissionResult)
    AddonUpdater.checkIfDue(this)
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onUnbind(intent: Intent?): Boolean {
    listener = null
    ArrayList(shells.keys).forEach { stopShell(it) }
    return false
  }

  override fun onDestroy() {
    Shizuku.removeBinderReceivedListener(binderReceived)
    Shizuku.removeBinderDeadListener(binderDead)
    Shizuku.removeRequestPermissionResultListener(permissionResult)
    ArrayList(shells.keys).forEach { stopShell(it) }
    super.onDestroy()
  }

  /** For adb shell dumpsys activity service io.github.aaron_gh.sandfly.addon. */
  override fun dump(fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
    writer.println("Shizuku ready: ${AddonShizuku.isReady()}")
    writer.println("Shizuku allowed: ${AddonShizuku.hasPermission()}")
    writer.println("Shizuku denied: ${AddonShizuku.isPermissionDenied()}")
    writer.println("Listener: ${listener != null}")
    writer.println("Shell services: ${shells.size}")
  }

  private fun requestPermission() {
    when {
      !AddonShizuku.isReady() -> tell { it.onPermissionResult(false) }
      AddonShizuku.hasPermission() -> tell { it.onPermissionResult(true) }
      AddonShizuku.isPermissionDenied() -> tell { it.onPermissionResult(false) }
      else ->
        try {
          Shizuku.requestPermission(AddonShizuku.REQUEST_CODE)
        } catch (e: RuntimeException) {
          Log.w(TAG, "Can't ask Shizuku: $e")
          tell { it.onPermissionResult(false) }
        }
    }
  }

  /** Starts the shell service in a process named after [tag], and gives it to [connection]. */
  private fun startShell(tag: String?, connection: IShellConnection) {
    val key = connection.asBinder()
    if (shells.containsKey(key)) return
    if (shells.size >= MAX_SHELLS) {
      fail(connection, "Too many shell services")
      return
    }
    if (tag == null || !TAG_PATTERN.matches(tag)) {
      fail(connection, "Bad tag $tag")
      return
    }
    if (!AddonShizuku.hasPermission()) {
      fail(connection, "Shizuku doesn't allow the add-on")
      return
    }
    val args =
      Shizuku.UserServiceArgs(ComponentName(packageName, ShizukuGrantService::class.java.name))
        .daemon(false)
        .processNameSuffix(tag)
        .tag(tag)
        .version(BuildConfig.VERSION_CODE)
    val serviceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
          try {
            connection.onConnected(service)
          } catch (e: RemoteException) {
            mainHandler.post { stopShell(key) }
          }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
          try {
            connection.onDisconnected()
          } catch (e: RemoteException) {
            // SandFly has stopped.
          }
        }
      }
    // SandFly stopped: stop its shell service too.
    val death = IBinder.DeathRecipient { mainHandler.post { stopShell(key) } }
    try {
      key.linkToDeath(death, 0)
    } catch (e: RemoteException) {
      return
    }
    shells[key] = Shell(args, serviceConnection, death)
    try {
      Shizuku.bindUserService(args, serviceConnection)
    } catch (e: RuntimeException) {
      shells.remove(key)
      key.unlinkToDeath(death, 0)
      fail(connection, e.toString())
    }
  }

  private fun stopShell(key: IBinder) {
    val shell = shells.remove(key) ?: return
    key.unlinkToDeath(shell.death, 0)
    try {
      // Removing it also stops its process.
      Shizuku.unbindUserService(shell.args, shell.connection, true)
    } catch (e: RuntimeException) {
      Log.w(TAG, "Can't stop the shell service: $e")
    }
  }

  private fun fail(connection: IShellConnection, error: String) {
    try {
      connection.onFailed(error)
    } catch (e: RemoteException) {
      // SandFly has stopped.
    }
  }

  private fun tell(call: (IAddonListener) -> Unit) {
    val current = listener ?: return
    try {
      call(current)
    } catch (e: RemoteException) {
      // SandFly has stopped.
    }
  }

  private companion object {
    const val TAG = "SandFlyAddon"
    /** Shizuku names the process after the tag. */
    val TAG_PATTERN = Regex("[a-z]{1,20}")
    /** SandFly uses two: one for commands and one for grants. */
    const val MAX_SHELLS = 8
  }
}
