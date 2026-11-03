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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Runs [ShizukuCommand]s through Shizuku's user service, which [ShellAccess] starts. The service stays bound while something
 * holds it, such as the open quick settings panel, so that commands after the first don't wait for
 * Shizuku to start a process.
 *
 * Everything here runs on the main thread, except the commands themselves.
 */
object ShizukuShell {
  private const val TAG = "ShizukuShell"
  private const val CONNECT_TIMEOUT_MS = 15_000L
  private const val IDLE_SECONDS = 10L

  /** A call to the service, which runs on [executor] and reports back on the main thread itself. */
  private class Pending(val call: (IShizukuGrants) -> Unit, val onFailed: () -> Unit)

  private val mainHandler = Handler(Looper.getMainLooper())
  /** Runs one command at a time, and ends its thread when idle. */
  private val executor =
    ThreadPoolExecutor(1, 1, IDLE_SECONDS, TimeUnit.SECONDS, LinkedBlockingQueue()) { Thread(it, TAG) }
      .apply { allowCoreThreadTimeOut(true) }
  private val pending = ArrayList<Pending>()
  private var service: IShizukuGrants? = null
  private var binding: Any? = null
  private var connection: ServiceConnection? = null
  private var holders = 0
  private val connectListeners = ArrayList<(IShizukuGrants) -> Unit>()
  private var running = 0

  private val timeout = Runnable {
    LogUtils.w(TAG, "Shizuku did not start the command service")
    unbind()
    failPending()
  }

  /**
   * Whether this build can run commands through Shizuku at all: the full build always can, and the
   * Google Play build only with the SandFly add-on installed. Features that work only through
   * Shizuku are left out without it.
   */
  @JvmStatic fun isAvailable(context: Context): Boolean = ShellAccess.isAvailable(context)

  /** Whether Shizuku runs and allows SandFly, so that commands run without asking the user. */
  fun isAllowed(): Boolean = ShizukuGrants.isReady() && ShizukuGrants.hasShizukuPermission()

  /** Starts the service if Shizuku already allows SandFly, and keeps it until [release]. */
  fun hold(context: Context) {
    holders++
    if (isAllowed()) {
      bind(context.applicationContext)
    }
  }

  /**
   * Calls [listener] on the commands thread each time the service connects, while it is added, and
   * at once if it is connected. [hold] keeps the service connected.
   */
  fun addConnectListener(listener: (IShizukuGrants) -> Unit) {
    connectListeners += listener
    service?.let { connected -> executor.execute { tell(listener, connected) } }
  }

  fun removeConnectListener(listener: (IShizukuGrants) -> Unit) {
    connectListeners -= listener
  }

  private fun tell(listener: (IShizukuGrants) -> Unit, connected: IShizukuGrants) {
    try {
      listener(connected)
    } catch (e: Throwable) {
      LogUtils.w(TAG, "Shizuku call failed: %s", e)
    }
  }

  fun release() {
    holders = maxOf(0, holders - 1)
    unbindIfIdle()
  }

  /**
   * Runs [command] with [argument], asking the user to allow SandFly in Shizuku first if needed.
   * [onDone] gets whether it worked, and false at once without Shizuku.
   */
  fun run(context: Context, command: ShizukuCommand, argument: String, onDone: (Boolean) -> Unit) {
    if (!ShizukuGrants.isReady()) {
      onDone(false)
      return
    }
    val appContext = context.applicationContext
    ShizukuGrants.withShizukuPermission { allowed ->
      if (!allowed) {
        onDone(false)
        return@withShizukuPermission
      }
      call(
        appContext,
        { connected ->
          val error = connected.run(command.ordinal, argument)
          if (error != null) {
            LogUtils.w(TAG, "%s %s failed: %s", command, argument, error)
          }
          error == null
        },
      ) { worked ->
        onDone(worked == true)
      }
    }
  }

  /**
   * Reads whether [command]'s switch is on, without asking the user anything: [onResult] gets
   * null at once unless Shizuku already allows SandFly, and when it can't be read.
   */
  fun state(context: Context, command: ShizukuCommand, onResult: (Boolean?) -> Unit) {
    if (!isAllowed()) {
      onResult(null)
      return
    }
    call(
      context.applicationContext,
      { connected ->
        when (connected.state(command.ordinal)) {
          1 -> true
          0 -> false
          else -> null
        }
      },
      onResult,
    )
  }

  /**
   * Calls [work] on the service on [executor], connecting first if needed, and gives its result to
   * [onResult] on the main thread: null if the call failed or the service didn't start.
   */
  internal fun <T> call(context: Context, work: (IShizukuGrants) -> T?, onResult: (T?) -> Unit) {
    // The connection's state is the main thread's; scripts and the updater call from their own.
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post { call(context, work, onResult) }
      return
    }
    val request =
      Pending(
        call = { connected ->
          val result =
            try {
              work(connected)
            } catch (e: Throwable) {
              LogUtils.w(TAG, "Shizuku call failed: %s", e)
              null
            }
          mainHandler.post {
            running--
            onResult(result)
            unbindIfIdle()
          }
        },
        onFailed = { onResult(null) },
      )
    val connected = service
    if (connected != null) {
      execute(connected, request)
    } else {
      pending += request
      bind(context)
    }
  }

  private fun execute(service: IShizukuGrants, request: Pending) {
    running++
    executor.execute { request.call(service) }
  }

  private fun bind(anyContext: Context) {
    val context = anyContext.applicationContext
    if (connection != null) {
      return
    }
    val serviceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
          if (connection !== this) {
            return
          }
          mainHandler.removeCallbacks(timeout)
          val connected = binder?.takeIf { it.pingBinder() }?.let { IShizukuGrants.Stub.asInterface(it) }
          if (connected == null) {
            unbind()
            failPending()
            return
          }
          service = connected
          for (listener in ArrayList(connectListeners)) {
            executor.execute { tell(listener, connected) }
          }
          val requests = ArrayList(pending)
          pending.clear()
          requests.forEach { execute(connected, it) }
          unbindIfIdle()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
          if (connection !== this) {
            return
          }
          // Shizuku forgets a connection whose service died, so only a new bind brings it back,
          // and calls waiting for this one would wait forever.
          unbind()
          failPending()
          if (holders > 0 && isAllowed()) {
            bind(context)
          }
        }
      }
    connection = serviceConnection
    try {
      binding = ShellAccess.bind(context, "commands", serviceConnection)
      mainHandler.postDelayed(timeout, CONNECT_TIMEOUT_MS)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot start the command service: %s", e)
      binding = null
      connection = null
      failPending()
    }
  }

  private fun unbindIfIdle() {
    if (holders == 0 && running == 0 && pending.isEmpty()) {
      unbind()
    }
  }

  private fun unbind() {
    mainHandler.removeCallbacks(timeout)
    val serviceBinding = binding
    binding = null
    connection = null
    service = null
    if (serviceBinding != null) {
      try {
        // Removing it also stops its process.
        ShellAccess.unbind(serviceBinding)
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Cannot stop the command service: %s", e)
      }
    }
  }

  private fun failPending() {
    val requests = ArrayList(pending)
    pending.clear()
    requests.forEach { it.onFailed() }
  }
}
