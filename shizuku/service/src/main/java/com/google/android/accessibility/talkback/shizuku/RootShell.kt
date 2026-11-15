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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.RemoteException
import android.util.Log
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Runs [ShizukuGrantService] through su, in place of Shizuku: in a process of its own started with
 * app_process ([RootShellMain]), as the shell user where su can switch to it, as Shizuku started
 * with adb would, and otherwise as root. Works with the su of Magisk, KernelSU and APatch.
 *
 * The process sends its binder back in a broadcast with a token that only it was given, and exits
 * when [unbind] closes its standard input or the app stops.
 *
 * Everything here runs on the main thread, except the su processes, which have threads of their own.
 */
object RootShell {
  private const val TAG = "RootShell"
  internal const val EXTRA_TOKEN = "token"
  internal const val EXTRA_SERVICE = "service"
  private const val CHECK_TIMEOUT_SECONDS = 120L
  private const val SHELL_UID = "2000"

  private val mainHandler = Handler(Looper.getMainLooper())
  private val random = SecureRandom()
  private val checkCallbacks = ArrayList<(Boolean) -> Unit>()

  /**
   * Whether su can run commands as the shell user: null until a start tells. Without it, the
   * service runs as root, where the clipboard and the hotspot can't be used as the shell's.
   */
  @Volatile private var shellUserWorks: Boolean? = null

  /**
   * Checks whether su gives SandFly root, which may show the root manager's request. [onResult]
   * gets the answer on the main thread. Checks that overlap share one su.
   */
  fun check(onResult: (Boolean) -> Unit) {
    checkCallbacks += onResult
    if (checkCallbacks.size > 1) return
    Thread({
        val granted =
          try {
            runCheck()
          } catch (e: Throwable) {
            Log.w(TAG, "su check failed: $e")
            false
          }
        mainHandler.post {
          val callbacks = ArrayList(checkCallbacks)
          checkCallbacks.clear()
          callbacks.forEach { it(granted) }
        }
      }, "$TAG-check")
      .start()
  }

  private fun runCheck(): Boolean {
    val process =
      try {
        ProcessBuilder("su", "-c", "id -u").redirectErrorStream(true).start()
      } catch (e: Exception) {
        // No su, or KernelSU and APatch, which hide it from apps they don't give root.
        Log.i(TAG, "No su: $e")
        return false
      }
    return try {
      process.outputStream.close()
      val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
      if (!process.waitFor(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        Log.w(TAG, "su did not answer")
        false
      } else {
        val granted = process.exitValue() == 0 && output.lines().lastOrNull()?.trim() == "0"
        if (!granted) Log.i(TAG, "su refused: ${process.exitValue()} ${output.take(200)}")
        granted
      }
    } catch (e: Exception) {
      Log.w(TAG, "su failed: $e")
      false
    } finally {
      process.destroy()
    }
  }

  /** Whether [binding] is one that [bind] returned. */
  fun owns(binding: Any?): Boolean = binding is Binding

  /**
   * Starts the service, in a process named after [tag], and gives its binder to [connection] on the
   * main thread: null if it can't start. [ServiceConnection.onServiceDisconnected] follows when the
   * process ends. Returns what [unbind] takes.
   */
  fun bind(context: Context, tag: String, connection: ServiceConnection): Any {
    val binding = Binding(context.applicationContext, tag, connection)
    binding.start(if (shellUserWorks == false) null else SHELL_UID)
    return binding
  }

  /** Stops the service that [bind] started, and its process. */
  fun unbind(binding: Any) {
    (binding as? Binding)?.stop()
  }

  /** One service started for one [connection], through one su at a time. */
  private class Binding(
    val context: Context,
    val tag: String,
    val connection: ServiceConnection,
  ) {
    private val action = context.packageName + ".action.ROOT_SHELL"
    /** Set on the main thread, read by the su thread too. */
    @Volatile private var stopped = false
    private var connected = false
    private var receiver: BroadcastReceiver? = null
    private var service: IBinder? = null
    private val death = IBinder.DeathRecipient { mainHandler.post { disconnected() } }
    /** The su of the current attempt, touched by its own thread too. */
    @Volatile private var process: java.lang.Process? = null

    /** Starts su, as [user] if given, and otherwise as root. */
    fun start(user: String?) {
      val token = ByteArray(16).also { random.nextBytes(it) }.joinToString("") { "%02x".format(it) }
      listen(token, user)
      Thread({
          try {
            runSu(token, user)
          } catch (e: Throwable) {
            Log.w(TAG, "su failed: $e")
            mainHandler.post { ended(user) }
          }
        }, "$TAG-$tag")
        .start()
    }

    private fun listen(token: String, user: String?) {
      unregister()
      val received =
        object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra(EXTRA_TOKEN) != token) return
            val binder = intent.getBundleExtra(EXTRA_SERVICE)?.getBinder(EXTRA_SERVICE)
            unregister()
            if (stopped) return
            if (shellUserWorks == null) shellUserWorks = user != null
            onConnected(binder)
          }
        }
      val filter = IntentFilter(action)
      // Exported, since su's process sends it as the shell user or root; the token checks it.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(received, filter, null, mainHandler, Context.RECEIVER_EXPORTED)
      } else {
        context.registerReceiver(received, filter, null, mainHandler)
      }
      receiver = received
    }

    private fun unregister() {
      val current = receiver ?: return
      receiver = null
      try {
        context.unregisterReceiver(current)
      } catch (e: IllegalArgumentException) {
        // Not registered.
      }
    }

    private fun onConnected(binder: IBinder?) {
      val alive =
        try {
          binder?.linkToDeath(death, 0)
          binder != null
        } catch (e: RemoteException) {
          false
        }
      if (!alive) {
        stop()
        connection.onServiceConnected(null, null)
        return
      }
      service = binder
      connected = true
      connection.onServiceConnected(ComponentName(context, ShizukuGrantService::class.java), binder)
    }

    /** Runs on the attempt's own thread. */
    private fun runSu(token: String, user: String?) {
      val su =
        try {
          // Options before the user, as Magisk's su documents them.
          val command = listOf("su", "-c", launchCommand()) + listOfNotNull(user)
          ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (e: Exception) {
          Log.w(TAG, "Cannot run su: $e")
          mainHandler.post { ended(user) }
          return
        }
      process = su
      if (stopped) {
        end(su)
        return
      }
      try {
        su.outputStream.write("$token\n".toByteArray())
        su.outputStream.flush()
        // Logs what su and the process print, until the process ends.
        su.inputStream.bufferedReader().useLines { lines ->
          lines.forEach { Log.i(TAG, "$tag: ${it.take(500)}") }
        }
      } catch (e: IOException) {
        // Stopped.
      }
      try {
        su.waitFor(1, TimeUnit.SECONDS)
      } catch (e: InterruptedException) {
        // Ends anyway.
      }
      mainHandler.post { ended(user) }
    }

    private fun launchCommand(): String {
      val apk = context.applicationInfo.sourceDir
      val userId = Process.myUid() / PER_USER_RANGE
      return "export CLASSPATH=${quote(apk)}; exec /system/bin/app_process /system/bin " +
        "--nice-name=${quote(context.packageName + ":" + tag)} " +
        "${RootShellMain::class.java.name} ${quote(context.packageName)} $userId ${quote(action)}"
    }

    /** su's process ended: before connecting, it couldn't start. */
    private fun ended(user: String?) {
      if (stopped) return
      if (connected) {
        disconnected()
        return
      }
      unregister()
      if (user != null && shellUserWorks != true) {
        Log.i(TAG, "su can't run $tag as the shell user; running it as root")
        shellUserWorks = false
        start(null)
        return
      }
      stopped = true
      connection.onServiceConnected(null, null)
    }

    private fun disconnected() {
      if (!connected) return
      connected = false
      service?.let {
        try {
          it.unlinkToDeath(death, 0)
        } catch (e: RuntimeException) {
          // Already dead.
        }
      }
      service = null
      stop()
      connection.onServiceDisconnected(null)
    }

    fun stop() {
      stopped = true
      unregister()
      val su = process ?: return
      process = null
      end(su)
    }
  }

  /**
   * Ends [su] and the process it started. Closing its standard input ends that process, which may
   * not be su's child: Magisk's su is a client of a root daemon.
   */
  private fun end(su: java.lang.Process) {
    try {
      su.outputStream.close()
    } catch (e: IOException) {
      // Closed.
    }
    su.destroy()
  }

  private fun quote(text: String): String = "'" + text.replace("'", "'\\''") + "'"

  private const val PER_USER_RANGE = 100_000
}
