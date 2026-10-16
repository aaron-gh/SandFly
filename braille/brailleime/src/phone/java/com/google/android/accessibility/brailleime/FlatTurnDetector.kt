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

package com.google.android.accessibility.brailleime

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Notices when a device lying flat is turned on the table, which gravity cannot show. It follows
 * the game rotation vector, from the gyroscope and accelerometer, and counts the turns with a
 * [FlatTurnCounter]. It keeps following while the device is lifted for a moment, so a turn that
 * tilts it still counts. Devices without the sensor never report a turn.
 */
class FlatTurnDetector(context: Context, private val onTurned: OnTurnedListener) {
  /** Hears about turns of the device lying flat. */
  fun interface OnTurnedListener {
    /** The device turned this many quarter turns clockwise, seen from above. */
    fun onTurned(quarters: Int)
  }

  private val sensorManager = context.getSystemService(SensorManager::class.java)
  private val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
  private val rotationMatrix = FloatArray(9)
  private val orientation = FloatArray(3)
  private val counter = FlatTurnCounter()
  private var listening = false

  private val listener =
    object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val quarters = counter.onHeading(Math.toDegrees(orientation[0].toDouble()).toFloat())
        if (quarters != 0) {
          onTurned.onTurned(quarters)
        }
      }

      override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

  /** Starts watching, taking where the device points now as the starting direction. */
  fun start() {
    counter.reset()
    val sensor = sensor ?: return
    if (listening) return
    listening = true
    sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
  }

  /** Takes where the device points now as the starting direction for the next turn. */
  fun reset() {
    counter.reset()
  }

  fun stop() {
    if (!listening) return
    listening = false
    sensorManager?.unregisterListener(listener)
    counter.reset()
  }
}

/**
 * Counts quarter turns of a device lying flat from its compass heading. A turn counts once the
 * device has turned two thirds of the way to the next quarter, so small nudges do not.
 */
class FlatTurnCounter {
  private var reference: Float? = null

  /**
   * Takes the device's heading in degrees, clockwise seen from above, and returns how many quarter
   * turns clockwise it has turned since the last turn, or 0.
   */
  fun onHeading(heading: Float): Int {
    val from = reference
    if (from == null) {
      reference = heading
      return 0
    }
    val turned = angleBetween(heading, from)
    if (abs(turned) < QUARTER_TURN * 2 / 3) {
      return 0
    }
    val step = if (turned > 0) 1 else -1
    // Measure the next turn from a whole quarter on, not from where this turn was noticed, so
    // turning back counts as soon as this one did.
    reference = angleBetween(from + step * QUARTER_TURN, 0f)
    return step
  }

  /** Takes the next heading as the starting direction. */
  fun reset() {
    reference = null
  }

  companion object {
    const val QUARTER_TURN = 90f

    /** The signed difference from [from] to [to], in degrees, between -180 and 180. */
    fun angleBetween(to: Float, from: Float): Float = ((to - from) % 360f + 540f) % 360f - 180f
  }
}
