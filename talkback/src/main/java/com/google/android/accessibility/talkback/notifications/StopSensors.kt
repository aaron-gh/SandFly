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

package com.google.android.accessibility.talkback.notifications

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Listens for a shake or a hand waved over the proximity sensor while a notification is read, and
 * calls [onStop]. The sensors are on only while [start] and [stop] are around a readout, so they
 * use no battery the rest of the time.
 */
class StopSensors(context: Context, private val onStop: () -> Unit) {
  private val sensors = context.getSystemService(SensorManager::class.java)
  private var shakeListener: SensorEventListener? = null
  private var waveListener: SensorEventListener? = null

  /** Starts listening for what [settings] turn on. */
  fun start(settings: NotificationSettings) {
    stop()
    val sensors = sensors ?: return
    if (settings.shakeToStop) {
      sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
        val listener = ShakeListener(shakeThreshold(settings.shakeStrength)) { onStop() }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        shakeListener = listener
      }
    }
    if (settings.waveToStop) {
      sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { sensor ->
        val listener = WaveListener(sensor.maximumRange, settings.pocketMode) { onStop() }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        waveListener = listener
      }
    }
  }

  fun stop() {
    shakeListener?.let { sensors?.unregisterListener(it) }
    waveListener?.let { sensors?.unregisterListener(it) }
    shakeListener = null
    waveListener = null
  }

  /** Two shakes of [threshold] m/s² beyond gravity within a second and a half stop reading. */
  private class ShakeListener(private val threshold: Float, private val onShake: () -> Unit) :
    SensorEventListener {
    private var firstShakeAt = 0L
    private var lastShakeAt = 0L

    override fun onSensorChanged(event: SensorEvent) {
      val (x, y, z) = event.values
      val force = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
      if (force < threshold) return
      val now = SystemClock.uptimeMillis()
      // One movement gives several readings over the threshold.
      if (now - lastShakeAt < SAME_SHAKE_MS) return
      lastShakeAt = now
      if (firstShakeAt != 0L && now - firstShakeAt < SHAKES_WITHIN_MS) {
        firstShakeAt = 0L
        onShake()
      } else {
        firstShakeAt = now
      }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
  }

  /**
   * A hand moved over the sensor stops reading. With [pocketMode], a sensor that is covered when
   * reading starts, as in a pocket, is ignored until it is uncovered.
   */
  private class WaveListener(
    private val maximumRange: Float,
    private val pocketMode: Boolean,
    private val onWave: () -> Unit,
  ) : SensorEventListener {
    private var first = true
    private var armed = true

    override fun onSensorChanged(event: SensorEvent) {
      val near = event.values[0] < minOf(maximumRange, NEAR_CM)
      if (first) {
        first = false
        if (near && pocketMode) {
          armed = false
          return
        }
      }
      if (!near) {
        armed = true
      } else if (armed) {
        onWave()
      }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
  }

  private companion object {
    const val SAME_SHAKE_MS = 250L
    const val SHAKES_WITHIN_MS = 1500L
    const val NEAR_CM = 3f

    /** Gentle, medium and hard shakes. */
    fun shakeThreshold(strength: Int): Float =
      when (strength) {
        1 -> 7f
        3 -> 18f
        else -> 12f
      }
  }
}
