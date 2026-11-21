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
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.RemoteException
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyPresses
import java.io.File
import java.io.IOException

/** Owns the volume-only hardware devices for one screen-off interval. */
internal class ScreenOffVolumeKeyCapture(
  private val context: Context,
  private val listener: IVolumeKeyCommandListener,
  private val up: VolumeKeyPresses.KeyCommands,
  private val down: VolumeKeyPresses.KeyCommands,
  private val timeouts: VolumeKeyPresses.Timeouts,
  private val speechUsage: Int,
) : AutoCloseable {
  private val power = context.getSystemService(PowerManager::class.java)
  private val displays = context.getSystemService(DisplayManager::class.java)
  private val audio = context.getSystemService(AudioManager::class.java)
  private val stopPipe = ParcelFileDescriptor.createPipe()
  private val death = IBinder.DeathRecipient { stop(byClient = true) }
  private val worker = Thread({ capture() }, TAG)
  @Volatile private var stopped = false
  /** Whether SandFly asked to stop or died, so that it isn't told the keys went back. */
  @Volatile private var stoppedByClient = false
  /** Keeps the CPU awake while presses are counted, which a display that is off doesn't. */
  private var wakeLock: PowerManager.WakeLock? =
    power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)?.apply {
      setReferenceCounted(false)
    }
  /** Uptime until which the CPU stays awake after a key edge or a command. */
  private var awakeUntil = 0L

  fun isWatching(callback: IVolumeKeyCommandListener): Boolean =
    listener.asBinder() == callback.asBinder()

  fun start() {
    try {
      listener.asBinder().linkToDeath(death, 0)
      worker.start()
    } catch (failure: Exception) {
      stop(byClient = true)
      stopPipe[0].close()
      throw failure
    }
  }

  @Synchronized
  private fun stop(byClient: Boolean) {
    if (byClient) stoppedByClient = true
    if (!stopped) {
      stopped = true
      stopPipe[1].close()
    }
  }

  override fun close() {
    stop(byClient = true)
    worker.join(1000)
  }

  private fun screenOff(): Boolean =
    !stopped &&
      power?.isInteractive == false &&
      displays?.getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_OFF

  private fun capture() {
    val devices = mutableListOf<ParcelFileDescriptor>()
    // What went wrong, for SandFly; null when the display turned on.
    var reason: String? = null
    val displayThread = HandlerThread(TAG).apply { start() }
    val displayListener =
      object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
          if (displayId == Display.DEFAULT_DISPLAY && !screenOff()) stop(byClient = false)
        }
      }
    try {
      displays?.registerDisplayListener(displayListener, Handler(displayThread.looper))
      if (!screenOff()) return
      var foundKeys = 0
      for (file in File("/dev/input").listFiles().orEmpty()) {
        if (!EVENT_NODE.matches(file.name)) continue
        val device = try {
          ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (_: IOException) {
          continue
        }
        val keys =
          try {
            VolumeKeyDevice.supportedKeys(device.fd)
          } catch (failure: Throwable) {
            device.close()
            throw failure
          }
        if (keys == 0) {
          device.close()
          continue
        }
        devices += device
        check(foundKeys and keys == 0) { "Ambiguous built-in volume keys" }
        foundKeys = foundKeys or keys
      }
      check(foundKeys == (UP or DOWN)) { "No dedicated built-in volume key devices" }
      ParcelFileDescriptor.open(File("/dev/uinput"), ParcelFileDescriptor.MODE_WRITE_ONLY).use { output ->
        check(VolumeKeyDevice.createOutput(output.fd)) { "Cannot create Android volume fallback" }
        if (!waitForOutput()) return
        // Do not take a key in the middle of an Android press.
        val grabbedAt = LongArray(devices.size)
        try {
          for ((index, device) in devices.withIndex()) {
            if (!screenOff()) return
            checkNoKeysDown(device)
            grabbedAt[index] = SystemClock.uptimeMillis()
            check(VolumeKeyDevice.grab(device.fd)) { "Cannot take a volume key device" }
            checkNoKeysDown(device)
          }
          Log.i(TAG, "Watching built-in volume keys with the display off")
          readKeys(devices, grabbedAt, output.fd)
        } finally {
          // Closing the physical fds returns the buttons to Android before the helper stops.
          devices.forEach { it.close() }
          devices.clear()
          Log.i(TAG, "Returned built-in volume keys to Android")
        }
      }
    } catch (failure: Exception) {
      if (!stopped) {
        Log.w(TAG, "Leaving volume keys to Android", failure)
        reason = failure.message ?: failure.toString()
      }
    } catch (failure: LinkageError) {
      if (!stopped) {
        Log.w(TAG, "Cannot load volume key support", failure)
        reason = "Cannot load volume key support: $failure"
      }
    } finally {
      devices.forEach { runCatching { it.close() } }
      releaseWakeLock()
      runCatching { displays?.unregisterDisplayListener(displayListener) }
      displayThread.quitSafely()
      stop(byClient = false)
      stopPipe[0].close()
      runCatching { listener.asBinder().unlinkToDeath(death, 0) }
      if (!stoppedByClient) {
        try {
          listener.stopped(reason)
        } catch (_: RemoteException) {
          // SandFly stopped.
        }
      }
    }
  }

  private fun checkNoKeysDown(device: ParcelFileDescriptor) {
    val down = VolumeKeyDevice.keysDown(device.fd)
    check(down >= 0) { "Cannot read the volume key state" }
    check(down == 0) { "Volume key held when the display turned off" }
  }

  /** Waits for Android to recognize the fallback device before taking any physical key. */
  private fun waitForOutput(): Boolean {
    val input = context.getSystemService(InputManager::class.java) ?: return false
    val stopFd = pollFd(stopPipe[0])
    val until = SystemClock.uptimeMillis() + OUTPUT_START_TIMEOUT_MS
    while (screenOff()) {
      if (input.inputDeviceIds.any { input.getInputDevice(it)?.name == OUTPUT_NAME }) return true
      check(SystemClock.uptimeMillis() < until) { "Android did not register the volume fallback" }
      Os.poll(arrayOf(stopFd), 25)
    }
    return false
  }

  private fun readKeys(devices: List<ParcelFileDescriptor>, grabbedAt: LongArray, output: Int) {
    val sources = (listOf(stopPipe[0]) + devices).map(::pollFd).toTypedArray()
    val timer = KeyTimer()
    fun emit(key: Int, pressed: Boolean) {
      check(VolumeKeyDevice.emit(output, key == KeyEvent.KEYCODE_VOLUME_UP, pressed)) {
        "Volume fallback disconnected"
      }
    }
    val presses = VolumeKeyPresses(
      timer,
      object : VolumeKeyPresses.Callbacks {
        override fun perform(command: String) {
          if (screenOff()) {
            stayAwakeBriefly()
            listener.perform(command)
          }
        }

        override fun adjustVolume(keyCode: Int, repeated: Boolean) {
          emit(keyCode, true)
          emit(keyCode, false)
        }
      },
    ).apply { setCommands(up, down, timeouts) }

    var held = 0
    var passed = 0
    try {
      while (!stopped) {
        val ready = Os.poll(sources, pollTimeout(timer))
        if (sources[0].revents.toInt() != 0 || !screenOff()) return
        if (ready == 0) {
          timer.advance(SystemClock.uptimeMillis())
          keepAwake(timer)
          continue
        }
        for (index in devices.indices) {
          val flags = sources[index + 1].revents.toInt()
          if (flags == 0) continue
          check(flags and INVALID_FD == 0) { "Volume key device disconnected" }
          if (flags and OsConstants.POLLIN == 0) continue
          val edge = VolumeKeyDevice.readEdge(devices[index].fd)
          check(edge >= 0) { "Volume key input overruns or disconnected" }
          val eventTime = edge shr 2
          if (edge == 0L || eventTime < grabbedAt[index]) continue
          stayAwakeBriefly()
          timer.advance(eventTime)
          val isUp = edge and 2L != 0L
          val isDown = edge and 1L != 0L
          val bit = if (isUp) UP else DOWN
          val key = if (isUp) KeyEvent.KEYCODE_VOLUME_UP else KeyEvent.KEYCODE_VOLUME_DOWN
          presses.onKeyState(key, isDown)
          if (isDown) {
            held = held or bit
            if (held == (UP or DOWN)) {
              // Keep the system's two-volume-key shortcut available.
              presses.cancel()
              if (passed and UP == 0) emit(KeyEvent.KEYCODE_VOLUME_UP, true)
              if (passed and DOWN == 0) emit(KeyEvent.KEYCODE_VOLUME_DOWN, true)
              passed = UP or DOWN
            } else if (!presses.onKeyDown(key, repeated = false, canUse = ::canUseCommands)) {
              emit(key, true)
              passed = passed or bit
            }
          } else {
            held = held and bit.inv()
            if (!presses.onKeyUp(key) && passed and bit != 0) {
              emit(key, false)
              passed = passed and bit.inv()
            }
          }
        }
        keepAwake(timer)
      }
    } finally {
      if (passed and UP != 0) runCatching { emit(KeyEvent.KEYCODE_VOLUME_UP, false) }
      if (passed and DOWN != 0) runCatching { emit(KeyEvent.KEYCODE_VOLUME_DOWN, false) }
    }
  }

  private fun canUseCommands(): Boolean {
    val audio = audio ?: return true
    val mode = audio.mode
    // Leave the Android handling of call volume and silencing an incoming call available.
    if (mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL) return false
    return !VolumeKeyPresses.isAppCall(
      audio.activePlaybackConfigurations.map { it.audioAttributes.usage },
      speechUsage,
      mode,
    )
  }

  private fun stayAwakeBriefly() {
    awakeUntil = SystemClock.uptimeMillis() + AWAKE_AFTER_EDGE_MS
  }

  /** Holds the wake lock while [timer] runs or shortly after a key edge or a command. */
  private fun keepAwake(timer: KeyTimer) {
    val lock = wakeLock ?: return
    val needed = timer.isPending || SystemClock.uptimeMillis() < awakeUntil
    try {
      if (needed && !lock.isHeld) {
        lock.acquire(WAKE_LOCK_LIMIT_MS)
      } else if (!needed && lock.isHeld) {
        lock.release()
      }
    } catch (failure: RuntimeException) {
      Log.w(TAG, "Cannot keep the CPU awake while counting presses", failure)
      wakeLock = null
    }
  }

  private fun releaseWakeLock() {
    val lock = wakeLock ?: return
    try {
      if (lock.isHeld) lock.release()
    } catch (_: RuntimeException) {
      // Already released.
    }
  }

  /** Waits until [timer] is due, or until the wake lock may be released. */
  private fun pollTimeout(timer: KeyTimer): Int {
    val timerWait = timer.waitMs()
    val now = SystemClock.uptimeMillis()
    if (wakeLock?.isHeld != true || awakeUntil <= now) return timerWait
    val awakeWait = (awakeUntil - now).toInt()
    return if (timerWait < 0) awakeWait else minOf(timerWait, awakeWait)
  }

  private fun pollFd(descriptor: ParcelFileDescriptor): StructPollfd =
    StructPollfd().apply {
      fd = descriptor.fileDescriptor
      events = OsConstants.POLLIN.toShort()
    }

  /** Uses input timestamps so an already-queued release isn't mistaken for a long press. */
  private class KeyTimer : VolumeKeyPresses.Timer {
    private var latestTime = 0L
    private var due = 0L
    private var task: Runnable? = null

    val isPending: Boolean
      get() = task != null

    override fun schedule(delayMs: Long, task: Runnable) {
      due = latestTime + delayMs
      this.task = task
    }

    override fun cancel() {
      task = null
    }

    fun waitMs(): Int =
      if (task == null) -1 else (due - SystemClock.uptimeMillis()).coerceAtLeast(0).toInt()

    fun advance(time: Long) {
      latestTime = maxOf(latestTime, time)
      if (latestTime < due) return
      val ready = task ?: return
      task = null
      ready.run()
    }
  }

  companion object {
    private const val TAG = "ScreenOffVolumeKeys"
    private const val OUTPUT_NAME = "SandFly Volume Keys"
    private const val OUTPUT_START_TIMEOUT_MS = 750L
    private const val WAKE_LOCK_TAG = "SandFly:ScreenOffVolumeKeys"
    /** How long the CPU stays awake after a key edge or a command, for SandFly to respond. */
    private const val AWAKE_AFTER_EDGE_MS = 1000L
    /** Release in case the capture thread stops responding; renewed while presses continue. */
    private const val WAKE_LOCK_LIMIT_MS = 10_000L
    private const val UP = 1
    private const val DOWN = 2
    private val EVENT_NODE = Regex("event[0-9]+")
    private val INVALID_FD = OsConstants.POLLERR or OsConstants.POLLHUP or OsConstants.POLLNVAL
  }
}

private object VolumeKeyDevice {
  init {
    System.loadLibrary("sandfly_volume_keys")
  }

  external fun supportedKeys(fd: Int): Int
  external fun keysDown(fd: Int): Int
  external fun grab(fd: Int): Boolean
  external fun readEdge(fd: Int): Long
  external fun createOutput(fd: Int): Boolean
  external fun emit(fd: Int, upKey: Boolean, pressed: Boolean): Boolean
}
