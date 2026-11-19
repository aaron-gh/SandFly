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
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.RemoteException
import android.util.Log
import android.view.Display
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyPresses
import com.google.android.accessibility.utils.FormFactorUtils

/** Lets the regular volume key recognizer receive presses when Android doesn't deliver key events. */
internal class ScreenOffVolumeKeys(
  private val context: Context,
  private val perform: (String) -> Unit,
) : DisplayManager.DisplayListener {
  private val main = Handler(Looper.getMainLooper())
  private val displays = context.getSystemService(DisplayManager::class.java)
  private val power = context.getSystemService(PowerManager::class.java)
  private var options: Options? = null
  private var active = false
  private var listening = false

  @Volatile private var session: Session? = null
  private val connected: (IShizukuGrants) -> Unit = { session?.register(it) }
  private val ready: () -> Unit = { main.post { refresh() } }

  fun configure(
    enabled: Boolean,
    up: VolumeKeyPresses.KeyCommands,
    down: VolumeKeyPresses.KeyCommands,
    timeouts: VolumeKeyPresses.Timeouts,
  ) {
    options = if (enabled && (!up.isEmpty || !down.isEmpty)) Options(up, down, timeouts) else null
    updateListening()
  }

  fun setActive(value: Boolean) {
    active = value
    updateListening()
  }

  private fun updateListening() {
    val shouldListen = active && options != null &&
      !FormFactorUtils.isAndroidWear() && ShizukuShell.isAvailable(context)
    if (listening != shouldListen) {
      listening = shouldListen
      if (shouldListen) {
        displays?.registerDisplayListener(this, main)
        ShizukuShell.addConnectListener(connected)
        ShizukuShell.hold(context)
        try {
          ShellAccess.addReadyListener(context, ready)
        } catch (failure: RuntimeException) {
          Log.w(TAG, "Cannot listen for Shizuku", failure)
        }
      } else {
        displays?.unregisterDisplayListener(this)
        ShellAccess.removeReadyListener(ready)
        ShizukuShell.removeConnectListener(connected)
        // Stop watching before releasing the service connection.
        refresh()
        ShizukuShell.release()
        return
      }
    }
    refresh()
  }

  fun refresh() {
    val next = options?.takeIf {
      listening && screenOff() && !ShizukuShell.usesRoot() && ShizukuShell.isAllowed()
    }
    val previous = session
    if (previous?.options == next) return
    session = next?.let(::Session)
    previous?.stop()
    if (next != null) {
      ShizukuShell.call(context, { session?.register(it) }) {}
    }
  }

  private fun screenOff(): Boolean =
    power?.isInteractive == false &&
      displays?.getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_OFF

  override fun onDisplayAdded(displayId: Int) = Unit
  override fun onDisplayRemoved(displayId: Int) = Unit

  override fun onDisplayChanged(displayId: Int) {
    if (displayId == Display.DEFAULT_DISPLAY) refresh()
  }

  private data class Options(
    val up: VolumeKeyPresses.KeyCommands,
    val down: VolumeKeyPresses.KeyCommands,
    val timeouts: VolumeKeyPresses.Timeouts,
  )

  private inner class Session(val options: Options) : IVolumeKeyCommandListener.Stub() {
    @Volatile private var service: IShizukuGrants? = null

    /** Called on ShizukuShell's commands thread. */
    fun register(connected: IShizukuGrants): Boolean {
      if (session !== this || service === connected) return true
      val error = connected.watchScreenOffVolumeKeys(commands(), timings(), this)
      if (error != null) {
        Log.w(TAG, "Cannot use volume keys with the screen off: $error")
        return false
      }
      service = connected
      if (session !== this) stop()
      return true
    }

    /** A one-way call so waking up doesn't wait behind another shell command. */
    fun stop() {
      try {
        service?.stopScreenOffVolumeKeys(this)
      } catch (_: RemoteException) {
        // The service already stopped.
      }
    }

    private fun commands(): Array<String?> =
      with(options) {
        arrayOf(up.single, up.double, up.triple, up.long, down.single, down.double, down.triple, down.long)
      }

    private fun timings(): LongArray =
      with(options.timeouts) { longArrayOf(multiPress, longPress, repeat, repeatStart) }

    override fun perform(command: String?) {
      if (command == null) return
      main.post {
        if (session === this && screenOff()) {
          Log.i(TAG, "Running assigned volume key command")
          this@ScreenOffVolumeKeys.perform(command)
        }
      }
    }
  }

  private companion object {
    const val TAG = "ScreenOffVolumeKeys"
  }
}
