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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.ShakeDetector
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Feels shakes of the device for the gesture trainer, with the user's Shake sensitivity, whether or
 * not the shake runs a command. It reads the accelerometer itself, as Shake does only while the
 * shake has a command. Calls [listener] on the main thread.
 */
class ShakePractice(private val context: Context, private val listener: Listener) {
  interface Listener {
    /** The device moves, at most [strongest] m/s² beyond gravity since the last call. */
    fun onMovement(strongest: Float)

    /** A swing ended, and [swings] swings, each the other way, count toward a shake. */
    fun onSwing(swings: Int)

    fun onShake()
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  private var thread: HandlerThread? = null
  private var sensorListener: SensorEventListener? = null

  /**
   * Counts starts and stops: a movement posted by the detector of an earlier start, such as before
   * Repeat, reaches no try of a later one.
   */
  private var generation = 0

  /** How strong each swing must be, in m/s² beyond gravity, with the user's sensitivity. */
  val threshold: Float
    get() = sensitivity().threshold

  /** Starts feeling shakes. Returns false when the device has no accelerometer. */
  fun start(): Boolean {
    if (sensorListener != null) return true
    val sensors = context.getSystemService(SensorManager::class.java) ?: return false
    val sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
    val started = ++generation
    val thread = HandlerThread("ShakePractice").apply { start() }
    fun post(call: () -> Unit) {
      mainHandler.post { if (generation == started) call() }
    }
    val detector = ShakeDetector { post { listener.onShake() } }
    detector.threshold = threshold
    detector.observer =
      object : ShakeDetector.Observer {
        // Movement is told at most every MOVEMENT_REPORT_NS, with the strongest reading since.
        private var strongest = 0f
        private var reportedNs = 0L

        override fun onMovement(timeNs: Long, strength: Float) {
          strongest = maxOf(strongest, strength)
          if (timeNs - reportedNs < MOVEMENT_REPORT_NS) return
          val told = strongest
          strongest = 0f
          reportedNs = timeNs
          post { listener.onMovement(told) }
        }

        override fun onSwing(timeNs: Long, swings: Int) {
          post { listener.onSwing(swings) }
        }
      }
    val sensorListener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          val values = event.values
          if (values.size >= 3) detector.onSample(event.timestamp, values[0], values[1], values[2])
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
      }
    if (!sensors.registerListener(sensorListener, sensor, SAMPLING_PERIOD_US, Handler(thread.looper))) {
      thread.quitSafely()
      return false
    }
    this.thread = thread
    this.sensorListener = sensorListener
    return true
  }

  fun stop() {
    generation++
    sensorListener?.let { context.getSystemService(SensorManager::class.java)?.unregisterListener(it) }
    sensorListener = null
    thread?.quitSafely()
    thread = null
  }

  private fun sensitivity(): ShakeDetector.Sensitivity {
    val value =
      SharedPreferencesUtils.getSharedPreferences(context)
        .getString(
          context.getString(R.string.pref_shake_sensitivity_key),
          context.getString(R.string.pref_shake_sensitivity_default),
        )
    return ShakeDetector.Sensitivity.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
      ?: ShakeDetector.Sensitivity.MEDIUM
  }

  private companion object {
    /** As Shake reads it alone: every 20 ms. */
    const val SAMPLING_PERIOD_US = 20_000
    const val MOVEMENT_REPORT_NS = 100_000_000L
  }
}
