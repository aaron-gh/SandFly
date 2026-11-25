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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.trainer.GestureTrainerHooks
import com.google.android.accessibility.utils.AccelerometerShare
import com.google.android.accessibility.utils.AccessibilityEventListener
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Gestures > Back tap and Gestures > Shake: runs the commands of taps on the back of the device and
 * of shakes.
 *
 * The accelerometer is read only while the screen is on, the screen reader is running and not
 * paused, and a back tap or the shake runs a command on the Gestures screen, in a gesture profile
 * or in an app: fast while a back tap runs one, slower for the shake alone. Readings are taken on a
 * thread of their own, in batches, and passed on through [AccelerometerShare].
 */
class AccelerometerGestures(
  private val context: Context,
  /** Runs the command of the gesture with the given preference key, on the main thread. */
  private val perform: (String) -> Unit,
) : AccessibilityEventListener {
  private val sensors = context.getSystemService(SensorManager::class.java)
  private val mainHandler = Handler(Looper.getMainLooper())
  /** The back tap keys by tap count, from 2. */
  private val tapKeys =
    listOf(
      context.getString(R.string.pref_shortcut_back_tap_double_key),
      context.getString(R.string.pref_shortcut_back_tap_triple_key),
      context.getString(R.string.pref_shortcut_back_tap_quadruple_key),
    )
  private val shakeKey = context.getString(R.string.pref_shortcut_shake_key)

  // The most back taps that run a command, or 0 for none.
  private var mostTaps = 0
  private var shakeUsed = false
  private var sensitivity = BackTapDetector.Sensitivity.MEDIUM
  private var shakeSensitivity = ShakeDetector.Sensitivity.MEDIUM
  private var screenOn = context.getSystemService(PowerManager::class.java)?.isInteractive ?: true
  private var allowed = false

  // While the accelerometer is read: the thread readings come on, and what reads them.
  private var thread: HandlerThread? = null
  private var handler: Handler? = null
  private var detector: BackTapDetector? = null
  private var shakeDetector: ShakeDetector? = null
  private var sensorListener: SensorEventListener? = null
  // Whether the readings come fast enough for back taps.
  private var readingFast = false
  // Whether readings go to the shake detector: used on the thread readings come on.
  private var readingShake = false

  /** Reads which back taps and shakes run commands, and the sensitivities, from [prefs]. */
  fun reload(prefs: SharedPreferences) {
    val gestureSet = gestureSet(prefs)
    mostTaps = 0
    tapKeys.forEachIndexed { index, key ->
      if (isUsed(prefs, key, gestureSet)) {
        mostTaps = index + 2
      }
    }
    shakeUsed = isUsed(prefs, shakeKey, gestureSet)
    val value =
      prefs.getString(
        context.getString(R.string.pref_back_tap_sensitivity_key),
        context.getString(R.string.pref_back_tap_sensitivity_default),
      )
    sensitivity =
      BackTapDetector.Sensitivity.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: BackTapDetector.Sensitivity.MEDIUM
    val shakeValue =
      prefs.getString(
        context.getString(R.string.pref_shake_sensitivity_key),
        context.getString(R.string.pref_shake_sensitivity_default),
      )
    shakeSensitivity =
      ShakeDetector.Sensitivity.entries.firstOrNull { it.name.equals(shakeValue, ignoreCase = true) }
        ?: ShakeDetector.Sensitivity.MEDIUM
    update()
  }

  /** Sets whether the gestures may run commands: while the screen reader runs and isn't paused. */
  fun setAllowed(allowed: Boolean) {
    this.allowed = allowed
    update()
  }

  /** The screen turned on or off. */
  fun onScreenChanged(on: Boolean) {
    screenOn = on
    update()
  }

  /** Stops reading the accelerometer for good, as the service stops. */
  fun shutdown() {
    allowed = false
    stop()
  }

  override fun getEventTypes(): Int =
    AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or AccessibilityEvent.TYPE_TOUCH_INTERACTION_END

  override fun onAccessibilityEvent(event: AccessibilityEvent, eventId: EventId?) {
    val handler = handler ?: return
    val detector = detector ?: return
    val start = event.eventType == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START
    // The touch's time in the clock of sensor timestamps, as readings come in batches after it.
    val ageMs = (SystemClock.uptimeMillis() - event.eventTime).coerceAtLeast(0)
    val timeNs = SystemClock.elapsedRealtimeNanos() - ageMs * 1_000_000L
    handler.post { if (start) detector.onTouchStart(timeNs) else detector.onTouchEnd(timeNs) }
  }

  private fun update() {
    if ((mostTaps > 0 || shakeUsed) && screenOn && allowed) start() else stop()
  }

  private fun start() {
    val fast = mostTaps > 0
    val running = detector
    if (running != null && fast == readingFast) {
      configure(running, shakeDetector)
      return
    }
    if (running != null) {
      // Back taps need faster readings than the shake: read again at the other rate.
      stop()
    }
    val sensors = sensors ?: return
    val sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
    val thread = HandlerThread("AccelerometerGestures").apply { start() }
    val handler = Handler(thread.looper)
    val detector =
      BackTapDetector(
        object : BackTapDetector.Listener {
          override fun onTaps(count: Int) {
            val key = tapKeys.getOrNull(count - 2) ?: return
            mainHandler.post {
              // Taps reported just before the readings stopped run nothing, and the gesture
              // trainer takes back taps while it waits for tries.
              if (sensorListener != null && !GestureTrainerHooks.isWaitingForAttempts()) {
                perform(key)
              }
            }
          }

          override fun onJolt(description: String) {
            LogUtils.d(TAG, "%s", description)
          }
        }
      )
    val shakeDetector =
      ShakeDetector { timeNs ->
        // The jolts of a shake are no back taps.
        detector.dropTaps(timeNs)
        LogUtils.d(TAG, "Shake")
        mainHandler.post {
          // The gesture trainer takes shakes while it waits for tries.
          if (sensorListener != null && shakeUsed && !GestureTrainerHooks.isWaitingForAttempts()) {
            perform(shakeKey)
          }
        }
      }
    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          val values = event.values
          if (values.size >= 3) {
            if (fast) {
              detector.onSample(event.timestamp, values[0], values[1], values[2])
            }
            if (readingShake) {
              shakeDetector.onSample(event.timestamp, values[0], values[1], values[2])
            }
            AccelerometerShare.publish(event.timestamp, values[0], values[1], values[2])
          }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
      }
    configure(detector, shakeDetector)
    val period = if (fast) SAMPLING_PERIOD_US else SHAKE_SAMPLING_PERIOD_US
    if (!sensors.registerListener(listener, sensor, period, MAX_REPORT_LATENCY_US, handler)) {
      LogUtils.w(TAG, "The accelerometer can't be read for back taps or shakes")
      thread.quitSafely()
      return
    }
    this.thread = thread
    this.handler = handler
    this.detector = detector
    this.shakeDetector = shakeDetector
    readingFast = fast
    sensorListener = listener
    AccelerometerShare.setActive(true)
  }

  private fun configure(detector: BackTapDetector, shakeDetector: ShakeDetector?) {
    val threshold = sensitivity.threshold
    val mostTaps = mostTaps.coerceAtLeast(2)
    val shakeThreshold = shakeSensitivity.threshold
    val shake = shakeUsed
    val apply = {
      readingShake = shake
      detector.threshold = threshold
      detector.mostTaps = mostTaps
      shakeDetector?.threshold = shakeThreshold
    }
    // Before the readings start the detectors are used on no thread yet.
    handler?.post(apply) ?: apply()
  }

  private fun stop() {
    if (sensorListener != null) {
      AccelerometerShare.setActive(false)
    }
    sensorListener?.let { sensors?.unregisterListener(it) }
    sensorListener = null
    thread?.quitSafely()
    thread = null
    handler = null
    detector = null
    shakeDetector = null
    readingFast = false
  }

  /** Whether the gesture of [key] runs a command anywhere: in a profile, an app or the set in use. */
  private fun isUsed(prefs: SharedPreferences, key: String, gestureSet: Int): Boolean {
    if (GestureProfiles.usesGesture(key) || AppGestures.usesGesture(key)) {
      return true
    }
    val unassigned = context.getString(R.string.shortcut_value_unassigned)
    val value =
      prefs.getString(
        GestureShortcutMapping.getPrefKeyWithGestureSet(GestureLayout.key(prefs, key), gestureSet),
        GestureLayout.defaultValue(context, prefs, key, unassigned),
      )
    return !value.isNullOrEmpty() && value != unassigned
  }

  private fun gestureSet(prefs: SharedPreferences): Int =
    if (
      GestureShortcutMapping.isGestureSetEnabled(
        context,
        prefs,
        R.string.pref_multiple_gesture_set_key,
        R.bool.pref_multiple_gesture_set_default,
      )
    ) {
      SharedPreferencesUtils.getIntFromStringPref(
        prefs,
        context.resources,
        R.string.pref_gesture_set_key,
        R.string.pref_gesture_set_value_default,
      )
    } else {
      0
    }

  private companion object {
    const val TAG = "AccelerometerGestures"

    /**
     * Readings every 2.5 ms (400 Hz): a tap's jolt lasts about 10 ms. Android gives at most 200 Hz
     * without the HIGH_SAMPLING_RATE_SENSORS permission.
     */
    const val SAMPLING_PERIOD_US = 2_500

    /** Readings every 20 ms (50 Hz) for the shake alone: a shake swings about 3 to 8 times a second. */
    const val SHAKE_SAMPLING_PERIOD_US = 20_000

    /**
     * Readings come in batches up to 100 ms apart where the sensor can hold them, so the device
     * wakes 10 times a second instead of 400. Taps are judged by the readings' timestamps.
     */
    const val MAX_REPORT_LATENCY_US = 100_000
  }
}
