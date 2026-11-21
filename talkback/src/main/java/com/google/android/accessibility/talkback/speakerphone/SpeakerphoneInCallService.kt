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

package com.google.android.accessibility.talkback.speakerphone

import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.pause.PauseController
import com.google.android.accessibility.talkback.speakerphone.SpeakerphonePolicy.Route
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Switches a call to the speaker when the phone is away from the user's ear, and back to the
 * earpiece when it is held up again, like VoiceOver on iPhone.
 *
 * Android binds this service during calls only when Backtalk holds MANAGE_ONGOING_CALLS, which the
 * user grants with adb or Shizuku. It has no call screen of its own. It watches the proximity
 * sensor only while a call can be heard and the setting is on, and only switches the call while
 * Backtalk is on and not paused, so that turning Backtalk off or on, or pausing it, takes effect
 * during a call. While bound, it also plays the keypad tones of letter keys pressed on the call
 * screen ([KeypadKeys][com.google.android.accessibility.talkback.keyboard.KeypadKeys]).
 */
@RequiresApi(Build.VERSION_CODES.S)
class SpeakerphoneInCallService : InCallService() {
  private val main = Handler(Looper.getMainLooper())
  private val policy = SpeakerphonePolicy()
  private lateinit var sensorManager: SensorManager
  private var sensor: Sensor? = null
  private var farValue = 0f
  private var listening = false
  private var pendingNearEar: Boolean? = null

  private val prefsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == SpeakerphoneSettings.PREF_ENABLED) update()
    }

  private val callCallback =
    object : Call.Callback() {
      override fun onStateChanged(call: Call, state: Int) {
        update()
      }
    }

  private val sensorListener =
    object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        val nearEar = event.values[0] < farValue
        if (nearEar == pendingNearEar) return
        // Wait until the reading holds, so a hand passing over the sensor does not switch.
        pendingNearEar = nearEar
        main.removeCallbacks(applyReading)
        main.postDelayed(applyReading, SETTLE_MS)
      }

      override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

  private val applyReading = Runnable {
    val nearEar = pendingNearEar ?: return@Runnable
    if (!TalkBackService.isServiceActive() || PauseController.isPaused()) {
      // Forget the reading, so the next one switches once Backtalk is back.
      pendingNearEar = null
      return@Runnable
    }
    val target = policy.onProximity(nearEar, currentRoute()) ?: return@Runnable
    Log.i(TAG, "Phone ${if (nearEar) "at" else "away from"} the ear, switching to $target")
    setAudioRoute(
      if (target == Route.SPEAKER) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
    )
  }

  override fun onCreate() {
    super.onCreate()
    current = this
    sensorManager = getSystemService(SensorManager::class.java)
    sensor =
      sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY, /* wakeUp= */ true)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    farValue = sensor?.let { minOf(it.maximumRange, NEAR_CM) } ?: 0f
    SharedPreferencesUtils.getSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(prefsListener)
  }

  override fun onDestroy() {
    if (current === this) {
      current = null
      toneCall = null
    }
    SharedPreferencesUtils.getSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(prefsListener)
    stopListening()
    super.onDestroy()
  }

  override fun onCallAdded(call: Call) {
    call.registerCallback(callCallback, main)
    update()
  }

  override fun onCallRemoved(call: Call) {
    call.unregisterCallback(callCallback)
    update()
  }

  override fun onCallAudioStateChanged(audioState: CallAudioState) {
    policy.onRouteChanged(currentRoute())
  }

  /** Starts or stops watching the sensor, as calls and the setting change. */
  private fun update() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(this)
    val active =
      calls.any { it.details.state in AUDIBLE_STATES } && SpeakerphoneSettings.isEnabled(prefs)
    if (active) startListening() else stopListening()
    // Remember a switch to the speaker while a call is on hold or calls are swapped, so that the
    // call still goes back to the earpiece when the phone is held up.
    if (calls.isEmpty()) policy.reset()
  }

  private fun startListening() {
    val sensor = sensor ?: return
    if (listening) return
    listening = true
    // The sensor reports its current state as soon as it is registered.
    sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, main)
  }

  private fun stopListening() {
    if (!listening) return
    listening = false
    sensorManager.unregisterListener(sensorListener)
    main.removeCallbacks(applyReading)
    pendingNearEar = null
  }

  private fun currentRoute(): Route =
    when (callAudioState?.route) {
      CallAudioState.ROUTE_EARPIECE -> Route.EARPIECE
      CallAudioState.ROUTE_SPEAKER -> Route.SPEAKER
      else -> Route.OTHER
    }

  companion object {
    private const val TAG = "SpeakerphoneInCall"

    /** The service while Android has it bound, during calls. */
    @Volatile private var current: SpeakerphoneInCallService? = null

    /** The call whose keypad tone is playing. */
    private var toneCall: Call? = null

    /**
     * Plays the keypad tone of [character], a digit, * or #, in the active call, as the call
     * screen's keypad does, until [stopDtmfTone]. Returns false where no call is active or Android
     * hasn't bound the service.
     */
    @JvmStatic
    fun playDtmfTone(character: Char): Boolean {
      val service = current ?: return false
      val call = service.calls.firstOrNull { it.details.state == Call.STATE_ACTIVE } ?: return false
      toneCall?.stopDtmfTone()
      call.playDtmfTone(character)
      toneCall = call
      return true
    }

    /** Stops the keypad tone [playDtmfTone] started. */
    @JvmStatic
    fun stopDtmfTone() {
      toneCall?.stopDtmfTone()
      toneCall = null
    }

    /** How long a reading must hold before the call switches. */
    private const val SETTLE_MS = 300L

    /** Closer than this, in centimetres, counts as at the ear. */
    private const val NEAR_CM = 5f

    /** Call states where the user can hear the call: dialing, connecting and talking. */
    private val AUDIBLE_STATES =
      setOf(
        Call.STATE_DIALING,
        Call.STATE_CONNECTING,
        Call.STATE_ACTIVE,
        Call.STATE_PULLING_CALL,
      )
  }
}
