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

package com.google.android.accessibility.talkback.trainer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.BackTapDetector
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Feels taps on the back of the device for the gesture trainer, with the user's Back tap
 * sensitivity, whether or not back taps run commands. It reads the accelerometer itself, as Back
 * tap does only while a back tap has a command. Calls [listener] on the main thread.
 */
class BackTapPractice(private val context: Context, private val listener: Listener) {
  interface Listener {
    fun onJolt(jolt: BackTapDetector.Jolt)

    fun onRunEnded(count: Int)
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  private var thread: HandlerThread? = null
  private var handler: Handler? = null
  private var detector: BackTapDetector? = null
  private var sensorListener: SensorEventListener? = null

  /**
   * Counts starts and stops: a jolt posted by the detector of an earlier start, such as before
   * Repeat, reaches no try of a later one.
   */
  private var generation = 0

  /** Starts feeling taps. Returns false when the device has no accelerometer. */
  fun start(): Boolean {
    if (sensorListener != null) return true
    val sensors = context.getSystemService(SensorManager::class.java) ?: return false
    val sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
    val started = ++generation
    val thread = HandlerThread("BackTapPractice").apply { start() }
    val handler = Handler(thread.looper)
    val detector =
      BackTapDetector(
        object : BackTapDetector.Listener {
          override fun onTaps(count: Int) {}

          override fun onJoltJudged(jolt: BackTapDetector.Jolt) {
            mainHandler.post { if (generation == started) listener.onJolt(jolt) }
          }

          override fun onRunEnded(count: Int) {
            mainHandler.post { if (generation == started) listener.onRunEnded(count) }
          }
        }
      )
    detector.threshold = sensitivity().threshold
    // Every run is judged, so a double tap waits to see whether more taps follow.
    detector.mostTaps = Int.MAX_VALUE
    val sensorListener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          val values = event.values
          if (values.size >= 3) detector.onSample(event.timestamp, values[0], values[1], values[2])
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
      }
    if (!sensors.registerListener(sensorListener, sensor, SAMPLING_PERIOD_US, handler)) {
      thread.quitSafely()
      return false
    }
    this.thread = thread
    this.handler = handler
    this.detector = detector
    this.sensorListener = sensorListener
    return true
  }

  fun stop() {
    generation++
    sensorListener?.let { context.getSystemService(SensorManager::class.java)?.unregisterListener(it) }
    sensorListener = null
    thread?.quitSafely()
    thread = null
    handler = null
    detector = null
  }

  /** The screen was touched or the touch ended, at [eventTimeMs] in uptime: jolts then are no taps. */
  fun onScreenTouch(start: Boolean, eventTimeMs: Long) {
    val handler = handler ?: return
    val detector = detector ?: return
    // The touch's time in the clock of sensor timestamps.
    val ageMs = (SystemClock.uptimeMillis() - eventTimeMs).coerceAtLeast(0)
    val timeNs = SystemClock.elapsedRealtimeNanos() - ageMs * 1_000_000L
    handler.post { if (start) detector.onTouchStart(timeNs) else detector.onTouchEnd(timeNs) }
  }

  private fun sensitivity(): BackTapDetector.Sensitivity {
    val value =
      SharedPreferencesUtils.getSharedPreferences(context)
        .getString(
          context.getString(R.string.pref_back_tap_sensitivity_key),
          context.getString(R.string.pref_back_tap_sensitivity_default),
        )
    return BackTapDetector.Sensitivity.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
      ?: BackTapDetector.Sensitivity.MEDIUM
  }

  private companion object {
    /** As Back tap reads it: every 2.5 ms, or as fast as Android allows. */
    const val SAMPLING_PERIOD_US = 2_500
  }
}
