/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.monitor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Whether a foldable phone is folded, from the angle of its hinge in degrees: 0 is closed and 180
 * is flat. Angles between [CLOSED_AT_MOST] and [OPEN_AT_LEAST] keep the last state, so that a phone
 * held nearly closed, or jostled while closed, doesn't fold and unfold over and over. Some phones,
 * such as the Galaxy Z Flip and Fold, report only 0, 90 and 180; others report every angle.
 */
class HingeFoldTracker {
  /** Whether the phone is folded, or null before the first reading. */
  var folded: Boolean? = null
    private set

  /**
   * Takes a reading and returns whether the phone folded or unfolded. The first reading only sets
   * the state, so that nothing is said when SandFly starts.
   */
  fun onAngle(degrees: Float): Boolean {
    val current = folded
    if (current == null) {
      folded = degrees <= CLOSED_AT_MOST
      return false
    }
    val next =
      when {
        degrees <= CLOSED_AT_MOST -> true
        degrees >= OPEN_AT_LEAST -> false
        else -> current
      }
    folded = next
    return next != current
  }

  companion object {
    /** The largest angle that is closed. */
    const val CLOSED_AT_MOST = 20f

    /** The smallest angle that is open, as in Flex mode. */
    const val OPEN_AT_LEAST = 45f
  }
}

/**
 * Reads the hinge angle sensor of a foldable phone and says when it folds or unfolds. It does
 * nothing on phones without the sensor, which Android has since Android 11, and never guesses from
 * screens. It prefers the wake-up sensor, which reports while the screen is off.
 */
class HingeFoldMonitor(private val context: Context) {

  /** Called on the main thread when the phone folds or unfolds, not at the first reading. */
  fun interface Listener {
    fun onFoldChanged(folded: Boolean)
  }

  private val tracker = HingeFoldTracker()
  private val handler = Handler(Looper.getMainLooper())
  private var listener: Listener? = null
  private var sensorListener: SensorEventListener? = null

  /** When the phone last folded or unfolded, in [SystemClock.uptimeMillis], or 0. */
  private var changedAt = 0L

  /** Whether the phone is folded, or null without a hinge sensor or before its first reading. */
  val folded: Boolean?
    get() = tracker.folded

  fun start(listener: Listener) {
    if (sensorListener != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      return
    }
    val sensorManager = context.getSystemService(SensorManager::class.java) ?: return
    val sensor =
      sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE, /* wakeUp= */ true)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
        ?: return
    this.listener = listener
    val events =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          val degrees = event.values.firstOrNull() ?: return
          if (tracker.onAngle(degrees)) {
            changedAt = SystemClock.uptimeMillis()
            LogUtils.i(TAG, "Hinge at %s degrees: folded=%s", degrees, tracker.folded)
            this@HingeFoldMonitor.listener?.onFoldChanged(tracker.folded == true)
          }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
      }
    if (
      sensorManager.registerListener(events, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
    ) {
      sensorListener = events
      LogUtils.i(TAG, "Watching the hinge with %s, wake-up %s", sensor.name, sensor.isWakeUpSensor)
    } else {
      this.listener = null
    }
  }

  fun stop() {
    sensorListener?.let {
      context.getSystemService(SensorManager::class.java)?.unregisterListener(it)
    }
    sensorListener = null
    listener = null
  }

  /** Whether the phone unfolded, and is still unfolded, less than [millis] ago. */
  fun unfoldedWithin(millis: Long): Boolean = folded == false && isRecent(millis)

  private fun isRecent(millis: Long): Boolean =
    changedAt != 0L && SystemClock.uptimeMillis() - changedAt < millis

  companion object {
    private const val TAG = "HingeFoldMonitor"

    /** Whether this device has a hinge angle sensor, for showing the fold settings. */
    @JvmStatic
    fun hasHingeSensor(context: Context): Boolean =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        // Not getDefaultSensor, which leaves out wake-up sensors, the only kind some phones have.
        context.getSystemService(SensorManager::class.java)
          ?.getSensorList(Sensor.TYPE_HINGE_ANGLE)
          ?.isNotEmpty() == true
  }
}
