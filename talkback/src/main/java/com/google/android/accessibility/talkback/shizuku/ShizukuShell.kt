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
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import android.os.ParcelFileDescriptor
import java.util.concurrent.TimeUnit
import rikka.shizuku.Shizuku

/** A command that SandFly runs as the shell user, through Shizuku. */
enum class ShizukuCommand {
  WIFI,
  BLUETOOTH,
  MOBILE_DATA,
  AIRPLANE_MODE,
  LOCATION,
  BATTERY_SAVER,
  DARK_THEME,
  NFC,
  DATA_SAVER,
  /**
   * Turns the Wi-Fi hotspot on or off. There is no shell command for it, so the service calls
   * Android's TetheringManager as the shell user, which may tether.
   */
  HOTSPOT,
  /** Blocks the microphone for a user, as the system's Microphone access switch does. */
  MICROPHONE_BLOCKED,
  CAMERA_BLOCKED,
  /** Adds an app's tile to the system's quick settings, which SystemUI needs to run it. */
  ADD_TILE,
  /** Clicks an app's tile in the system's quick settings, without opening them. */
  CLICK_TILE,
  /** Types text as key events, into apps that have no text field to set text in. */
  INPUT_TEXT,
  /** Sends key events: up to 100 key code names, separated by spaces. */
  INPUT_KEYEVENT;

  /**
   * The command, as adb would run it after `adb shell`, or null if [argument] isn't one it takes.
   * Switches take [ON] or [OFF], the sensor switches with `@` and a user ID after it, and tiles a
   * flattened component name.
   */
  fun shellCommand(argument: String): List<String>? {
    val match = SWITCH.matchEntire(argument)
    val on = match?.let { it.groupValues[1] == ON }
    val user = match?.groupValues?.get(2)?.ifEmpty { null }
    val component = argument.takeIf { COMPONENT.matches(it) }
    return when (this) {
      WIFI -> on?.let { listOf("cmd", "wifi", "set-wifi-enabled", if (it) "enabled" else "disabled") }
      BLUETOOTH -> on?.let { listOf("cmd", "bluetooth_manager", if (it) "enable" else "disable") }
      MOBILE_DATA -> on?.let { listOf("svc", "data", if (it) "enable" else "disable") }
      AIRPLANE_MODE ->
        on?.let { listOf("cmd", "connectivity", "airplane-mode", if (it) "enable" else "disable") }
      LOCATION -> on?.let { listOf("cmd", "location", "set-location-enabled", it.toString()) }
      BATTERY_SAVER -> on?.let { listOf("cmd", "power", "set-mode", if (it) "1" else "0") }
      DARK_THEME -> on?.let { listOf("cmd", "uimode", "night", if (it) "yes" else "no") }
      NFC -> on?.let { listOf("svc", "nfc", if (it) "enable" else "disable") }
      DATA_SAVER -> on?.let { listOf("cmd", "netpolicy", "set", "restrict-background", it.toString()) }
      HOTSPOT -> null
      MICROPHONE_BLOCKED -> sensorCommand(on, user, "microphone")
      CAMERA_BLOCKED -> sensorCommand(on, user, "camera")
      ADD_TILE -> component?.let { listOf("cmd", "statusbar", "add-tile", it) }
      CLICK_TILE -> component?.let { listOf("cmd", "statusbar", "click-tile", it) }
      // input text reads %s as a space.
      INPUT_TEXT ->
        argument
          .takeIf { it.isNotEmpty() && it.length <= MAX_TEXT && it.none { c -> c.isISOControl() } }
          ?.let { listOf("input", "text", it.replace(" ", "%s")) }
      INPUT_KEYEVENT -> KEY_CODES.matchEntire(argument)?.let { listOf("input", "keyevent") + argument.split(' ') }
    }
  }

  private fun sensorCommand(on: Boolean?, user: String?, sensor: String): List<String>? {
    if (on == null || user == null) return null
    return listOf("cmd", "sensor_privacy", if (on) "enable" else "disable", user, sensor)
  }

  companion object {
    const val ON = "on"
    const val OFF = "off"

    fun argument(on: Boolean): String = if (on) ON else OFF

    fun argument(on: Boolean, userId: Int): String = argument(on) + "@" + userId

    private val SWITCH = Regex("""(on|off)(?:@(\d{1,6}))?""")

    private const val MAX_TEXT = 1000

    private val KEY_CODES = Regex("""KEYCODE_[A-Z0-9_]{1,30}( KEYCODE_[A-Z0-9_]{1,30}){0,99}""")

    private val COMPONENT =
      Regex(
        """[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+/\.?[A-Za-z_$][A-Za-z0-9_$]*(\.[A-Za-z_$][A-Za-z0-9_$]*)*"""
      )
  }
}

/**
 * Runs [ShizukuCommand]s through Shizuku's user service. The service stays bound while something
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
  private var args: Shizuku.UserServiceArgs? = null
  private var connection: ServiceConnection? = null
  private var holders = 0
  private var running = 0

  private val timeout = Runnable {
    LogUtils.w(TAG, "Shizuku did not start the command service")
    unbind()
    failPending()
  }

  /** Whether Shizuku runs and allows SandFly, so that commands run without asking the user. */
  fun isAllowed(): Boolean = ShizukuGrants.isReady() && ShizukuGrants.hasShizukuPermission()

  /** Starts the service if Shizuku already allows SandFly, and keeps it until [release]. */
  fun hold(context: Context) {
    holders++
    if (isAllowed()) {
      bind(context.applicationContext)
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
   * Installs [apks], of the given [sizes], as an update to [packageName], recording [installer] as
   * the installer. [onDone] gets whether it worked, and false at once without Shizuku. Closes the
   * file descriptors afterwards.
   */
  fun install(
    context: Context,
    apks: List<ParcelFileDescriptor>,
    sizes: List<Long>,
    packageName: String,
    installer: String,
    onDone: (Boolean) -> Unit,
  ) {
    if (!ShizukuGrants.isReady()) {
      apks.forEach { runCatching { it.close() } }
      onDone(false)
      return
    }
    val appContext = context.applicationContext
    ShizukuGrants.withShizukuPermission { allowed ->
      if (!allowed) {
        apks.forEach { runCatching { it.close() } }
        onDone(false)
        return@withShizukuPermission
      }
      call(
        appContext,
        { connected ->
          val error = connected.install(apks.toTypedArray(), sizes.toLongArray(), packageName, installer)
          if (error != null) {
            LogUtils.w(TAG, "Install as %s failed: %s", installer, error)
          }
          error == null
        },
      ) { worked ->
        apks.forEach { runCatching { it.close() } }
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
  private fun <T> call(context: Context, work: (IShizukuGrants) -> T?, onResult: (T?) -> Unit) {
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

  private fun bind(context: Context) {
    if (connection != null) {
      return
    }
    val serviceArgs =
      Shizuku.UserServiceArgs(ComponentName(context.packageName, ShizukuGrantService::class.java.name))
        .daemon(false)
        .processNameSuffix("commands")
        .tag("commands")
        .version(BuildConfig.COMMIT_COUNT)
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
          val requests = ArrayList(pending)
          pending.clear()
          requests.forEach { execute(connected, it) }
          unbindIfIdle()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
          if (connection === this) {
            service = null
          }
        }
      }
    args = serviceArgs
    connection = serviceConnection
    try {
      Shizuku.bindUserService(serviceArgs, serviceConnection)
      mainHandler.postDelayed(timeout, CONNECT_TIMEOUT_MS)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot start the command service: %s", e)
      args = null
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
    val serviceArgs = args
    val serviceConnection = connection
    args = null
    connection = null
    service = null
    if (serviceArgs != null && serviceConnection != null) {
      try {
        // Removing it also stops its process.
        Shizuku.unbindUserService(serviceArgs, serviceConnection, true)
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
