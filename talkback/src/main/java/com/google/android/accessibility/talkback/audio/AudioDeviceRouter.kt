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

package com.google.android.accessibility.talkback.audio

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Manages audio output routing for TalkBack.
 *
 * Allows users to choose where TalkBack audio (sound feedback and speech) routes:
 * 1. System default (follows Android system routing policy)
 * 2. Phone speaker (forces output to the phone's built-in speaker, even when Bluetooth is connected)
 * 3. Specific connected external devices (e.g. "Storm wireless audio", "Bolt wired headphones")
 *
 * Employs a zero-volume keep-alive audio track, communication device change listeners,
 * and broadcast monitors to maintain active audio playback state in Android's AudioDeviceBroker,
 * preventing Android's 6-second idle timeout from dropping the communication route back to Bluetooth.
 */
class AudioDeviceRouter @JvmOverloads constructor(
  private val context: Context,
  private val audioManager: AudioManager? =
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager,
) {

  sealed class AudioTarget {
    object SystemDefault : AudioTarget()
    object PhoneSpeaker : AudioTarget()
    object AnyExternal : AudioTarget()
    data class SpecificDevice(val type: Int, val name: String) : AudioTarget()

    companion object {
      fun parse(value: String?): AudioTarget {
        if (value.isNullOrEmpty() || value == "default") return SystemDefault
        if (value == "speaker") return PhoneSpeaker
        if (value == "bluetooth_or_wired") return AnyExternal
        if (value.startsWith("device:")) {
          val parts = value.split(":", limit = 3)
          if (parts.size >= 3) {
            val type = parts[1].toIntOrNull() ?: 0
            val name = parts[2]
            return SpecificDevice(type, name)
          } else if (parts.size == 2) {
            return SpecificDevice(0, parts[1])
          }
        }
        return SystemDefault
      }
    }
  }

  private var currentTarget: AudioTarget = AudioTarget.SystemDefault
  private var isMonitoring: Boolean = false
  private var audioDeviceCallback: AudioDeviceCallback? = null
  private var communicationDeviceListener: Any? = null
  private var eventReceiver: BroadcastReceiver? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private var lastEnsureRoutingTimeMs: Long = 0L
  @Volatile private var isRoutingActive: Boolean = false
  @Volatile private var isPhoneCallActive: Boolean = false
  // Another app is listening the way a call or a voice assistant does, such as Gemini. It needs the
  // communication device, so holding it on the speaker left Gemini unable to hear the user.
  @Volatile private var isVoiceInputActive: Boolean = false
  private val stepsAside: Boolean
    get() = isPhoneCallActive || isVoiceInputActive
  private var keepAliveTrack: AudioTrack? = null

  private var speakerphoneSetByUs = false
  private var otherPlaybackCount = 0
  private val playbackCallback =
    object : AudioManager.AudioPlaybackCallback() {
      override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
        // Another app (a media player, say) starting or stopping makes Android re-pick routes,
        // which can silently drop ours. Re-assert when the set of other players changes; our own
        // accessibility-usage tracks are ignored so we do not react to ourselves.
        val count =
          configs?.count {
            it.audioAttributes.usage != AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
          } ?: 0
        if (count == otherPlaybackCount) return
        otherPlaybackCount = count
        Log.i(TAG, "Other playback changed (count=$count), re-asserting routing")
        isRoutingActive = false
        scheduleReassertion()
      }
    }

  private val recordingCallback =
    object : AudioManager.AudioRecordingCallback() {
      override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
        val active = isVoiceInput(configs)
        if (active == isVoiceInputActive) return
        isVoiceInputActive = active
        Log.i(TAG, "Voice input ${if (active) "started" else "stopped"} in another app")
        applyRouting()
        if (!active) scheduleReassertion()
      }
    }

  private var reassertionStep = 0
  private val reassertionRunnable =
    object : Runnable {
      override fun run() {
        if (currentTarget == AudioTarget.SystemDefault) return
        ensureRoutingInternal(force = false)
        if (reassertionStep < REASSERTION_DELAYS_MS.size) {
          mainHandler.postDelayed(this, REASSERTION_DELAYS_MS[reassertionStep++])
        }
      }
    }

  private val prefChangeListener =
    SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
      if (key == context.getString(R.string.pref_audio_output_device_key)) {
        val newPref =
          SharedPreferencesUtils.getStringPref(
            prefs,
            context.resources,
            R.string.pref_audio_output_device_key,
            R.string.pref_audio_output_device_default,
          )
        Log.i(TAG, "SharedPreference changed for audio output device: $newPref")
        setPreferredDevice(newPref)
      }
    }

  init {
    instance = this
    try {
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      val savedPref =
        SharedPreferencesUtils.getStringPref(
          prefs,
          context.resources,
          R.string.pref_audio_output_device_key,
          R.string.pref_audio_output_device_default,
        )
      currentTarget = AudioTarget.parse(savedPref)
      // Nothing is routed yet, whatever a previous run left behind.
      setRouteForced(false)
      Log.i(TAG, "AudioDeviceRouter initialized with target: $currentTarget (raw pref: '$savedPref')")
      prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    } catch (e: Exception) {
      Log.e(TAG, "Error initializing AudioDeviceRouter preferences", e)
    }
  }

  /** Sets the preferred audio output target from the preference string value. */
  fun setPreferredDevice(prefValue: String?) {
    val target = AudioTarget.parse(prefValue)
    if (target != currentTarget) {
      Log.i(TAG, "Audio output target changed from $currentTarget to $target")
      LogUtils.d(TAG, "Audio output target changed from %s to %s", currentTarget, target)
      currentTarget = target
      applyRouting()
      scheduleReassertion()
    }
  }

  /** Starts listening for audio device connectivity changes and applies initial routing. */
  fun startMonitoring() {
    if (isMonitoring) return
    val am = audioManager ?: return

    val callback =
      object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
          Log.i(TAG, "Audio devices added, updating routing for target: $currentTarget")
          LogUtils.d(TAG, "Audio devices added, updating routing for target: %s", currentTarget)
          applyRouting()
          scheduleReassertion()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
          Log.i(TAG, "Audio devices removed, updating routing for target: $currentTarget")
          LogUtils.d(TAG, "Audio devices removed, updating routing for target: %s", currentTarget)
          applyRouting()
          scheduleReassertion()
        }
      }
    audioDeviceCallback = callback
    am.registerAudioDeviceCallback(callback, mainHandler)

    registerCommunicationDeviceListener(am)
    registerBroadcastReceiver()
    am.registerAudioPlaybackCallback(playbackCallback, mainHandler)
    isVoiceInputActive = isVoiceInput(am.activeRecordingConfigurations)
    am.registerAudioRecordingCallback(recordingCallback, mainHandler)

    isMonitoring = true
    applyRouting()
    scheduleReassertion()
  }

  /** Re-evaluates and applies audio routing according to the selected target. */
  fun applyRouting() {
    val am = audioManager ?: return
    if (stepsAside) {
      stopKeepAlive()
      clearDeviceRouting(am)
      return
    }

    Log.i(TAG, "Applying audio routing for target: $currentTarget")
    LogUtils.d(TAG, "Applying audio routing for target: %s", currentTarget)
    when (val target = currentTarget) {
      is AudioTarget.SystemDefault -> {
        stopKeepAlive()
        clearDeviceRouting(am)
      }
      is AudioTarget.PhoneSpeaker -> {
        val speaker = speakerToForce(am)
        if (speaker != null) {
          startKeepAlive(speaker)
          setDeviceRouting(am, speaker, forceSpeakerOn = true)
        } else {
          stopKeepAlive()
          clearDeviceRouting(am)
        }
      }
      is AudioTarget.AnyExternal -> {
        val external = findExternalDevice(am)
        if (external != null) {
          startKeepAlive(external)
          setDeviceRouting(am, external, forceSpeakerOn = false)
        } else {
          stopKeepAlive()
          clearDeviceRouting(am)
        }
      }
      is AudioTarget.SpecificDevice -> {
        val specific = findSpecificDevice(am, target) ?: findExternalDevice(am)
        if (specific != null) {
          startKeepAlive(specific)
          setDeviceRouting(am, specific, forceSpeakerOn = false)
        } else {
          stopKeepAlive()
          clearDeviceRouting(am)
        }
      }
    }
  }

  /**
   * Fast check invoked before speech or auditory feedback to ensure routing is
   * active and has not been cleared by Android or Bluetooth connection handshakes.
   */
  fun ensureRouting() {
    if (currentTarget == AudioTarget.SystemDefault) return
    if (stepsAside) return
    val now = SystemClock.uptimeMillis()
    // If routing is actively verified, throttle to avoid redundant queries.
    // If routing was dropped, bypass throttle immediately!
    if (isRoutingActive && (now - lastEnsureRoutingTimeMs < 500)) return
    lastEnsureRoutingTimeMs = now
    ensureRoutingInternal(force = false)
  }

  private fun ensureRoutingInternal(force: Boolean) {
    if (currentTarget == AudioTarget.SystemDefault) return
    if (stepsAside) return
    val am = audioManager ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val targetDevice = getTargetAudioDevice()
      if (targetDevice == null) {
        // The chosen device is not available (yet). Drop our request instead of leaving a stale
        // route behind; the device callback re-applies it when the device shows up.
        if (isRoutingActive) {
          stopKeepAlive()
          clearDeviceRouting(am)
        }
        return
      }
      if (keepAliveTrack == null || keepAliveTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
        startKeepAlive(targetDevice)
      }
      val currentComm = am.communicationDevice
      if (force ||
          currentComm == null ||
          currentComm.id != targetDevice.id ||
          !accessibilityAudioGoesToTarget(am, targetDevice)) {
        Log.i(
          TAG,
          "ensureRoutingInternal: resetting communication device to ${targetDevice.productName} (was: ${currentComm?.productName})",
        )
        setDeviceRouting(am, targetDevice, currentTarget is AudioTarget.PhoneSpeaker)
      } else {
        isRoutingActive = true
      }
    } else {
      if (currentTarget is AudioTarget.PhoneSpeaker) {
        setLegacySpeakerphone(am, true)
      }
    }
  }

  /**
   * On Android 13+, asks the system where audio with TalkBack's own attributes would really play,
   * so a route that looks right on paper but still ends up on the wrong output is caught. Returns
   * true when the answer cannot be determined.
   */
  private fun accessibilityAudioGoesToTarget(am: AudioManager, targetDevice: AudioDeviceInfo): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return try {
      val attrs =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()
      val devices = am.getAudioDevicesForAttributes(attrs)
      if (devices.isEmpty()) return true
      val onSpeaker = devices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
      if (targetDevice.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) onSpeaker else !onSpeaker
    } catch (e: Exception) {
      Log.w(TAG, "getAudioDevicesForAttributes failed", e)
      true
    }
  }

  /**
   * Starts a silent looping audio track in TalkBack's process.
   * This maintains mPlaybackActive=true for TalkBack in Android's AudioDeviceBroker,
   * completely preventing Android's 6-second CHECK_MODE_FOR_UID_PERIOD_MS timeout
   * from resetting the communication route while typing or searching.
   *
   * IMPORTANT: We write pure PCM silence (all zeros 0x00) into the buffer, but we
   * DO NOT call track.setVolume(0.0f). Calling setVolume(0.0f) causes Android's
   * PlaybackActivityMonitor to register the track as muted ("event:muted source:clientVolume"),
   * which makes apc.isActive() return false, causing AudioDeviceBroker's 6-second inactivity check
   * to believe playback is inactive! By keeping volume at default (1.0f) with a buffer of 0x00 PCM samples,
   * the output is 100% physically silent, but the system correctly considers playback ACTIVE.
   */
  private fun startKeepAlive(device: AudioDeviceInfo? = null) {
    if (keepAliveTrack != null) {
      if (device != null) {
        try {
          keepAliveTrack?.preferredDevice = device
        } catch (e: Exception) {
          Log.w(TAG, "Failed to set preferredDevice on existing keepAliveTrack", e)
        }
      }
      return
    }
    try {
      val sampleRate = 16000
      val channelConfig = AudioFormat.CHANNEL_OUT_MONO
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT
      val minBufferSize =
        AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)
      val silentBuffer = ByteArray(minBufferSize) // All zeros = silence

      val audioAttributes =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()

      val format =
        AudioFormat.Builder()
          .setSampleRate(sampleRate)
          .setChannelMask(channelConfig)
          .setEncoding(audioFormat)
          .build()

      val track =
        AudioTrack.Builder()
          .setAudioAttributes(audioAttributes)
          .setAudioFormat(format)
          .setBufferSizeInBytes(minBufferSize)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

      val written = track.write(silentBuffer, 0, silentBuffer.size)
      if (written > 0) {
        val frameCount = silentBuffer.size / 2
        val loopResult = track.setLoopPoints(0, frameCount, -1) // Loop forever
        Log.i(TAG, "track.setLoopPoints(0, $frameCount, -1) returned $loopResult")
      }
      if (device != null) {
        track.preferredDevice = device
      }
      // Do NOT call track.setVolume(0.0f)! The PCM buffer is already 100% zeros (silence).
      track.play()
      keepAliveTrack = track
      setRouteForced(true)
      Log.i(
        TAG,
        "Keep-alive audio track started (playState=${track.playState}, maintaining active playback state in AudioDeviceBroker)",
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start keep-alive audio track", e)
    }
  }

  private fun stopKeepAlive() {
    val track = keepAliveTrack ?: return
    keepAliveTrack = null
    setRouteForced(false)
    try {
      if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
        track.stop()
      }
      track.release()
      Log.i(TAG, "Keep-alive audio track stopped")
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping keep-alive audio track", e)
    }
  }

  /**
   * Tells speech and sounds whether Backtalk routes them, while the keep-alive track plays. Only
   * accessibility audio follows the route, so they use the accessibility stream then, and the
   * user's choice of stream otherwise.
   */
  private fun setRouteForced(forced: Boolean) {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    if (prefs.getBoolean(FailoverTextToSpeech.PREF_ROUTE_FORCED_KEY, false) != forced) {
      prefs.edit().putBoolean(FailoverTextToSpeech.PREF_ROUTE_FORCED_KEY, forced).apply()
    }
  }

  /**
   * Schedules repeated re-assertion passes to ensure routing is kept across Bluetooth
   * profile negotiations (which can take 1-2 seconds) or screen on/off transitions.
   */
  fun scheduleReassertion() {
    if (currentTarget == AudioTarget.SystemDefault) return
    mainHandler.removeCallbacks(reassertionRunnable)
    mainHandler.post(reassertionRunnable.also { reassertionStep = 0 })
  }

  private fun registerCommunicationDeviceListener(am: AudioManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val listener =
        AudioManager.OnCommunicationDeviceChangedListener { device ->
          Log.i(
            TAG,
            "OnCommunicationDeviceChangedListener: ${device?.productName} (type ${device?.type})",
          )
          handleCommunicationDeviceChanged(device)
        }
      communicationDeviceListener = listener
      val executor = ContextCompat.getMainExecutor(context)
      am.addOnCommunicationDeviceChangedListener(executor, listener)
      Log.i(TAG, "Registered OnCommunicationDeviceChangedListener")
    }
  }

  private fun unregisterCommunicationDeviceListener(am: AudioManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      (communicationDeviceListener as? AudioManager.OnCommunicationDeviceChangedListener)?.let {
        am.removeOnCommunicationDeviceChangedListener(it)
      }
      communicationDeviceListener = null
    }
  }

  private fun handleCommunicationDeviceChanged(device: AudioDeviceInfo?) {
    if (currentTarget == AudioTarget.SystemDefault) return
    if (stepsAside) return
    val am = audioManager ?: return
    val targetDevice = getTargetAudioDevice()
    if (targetDevice != null && (device == null || device.id != targetDevice.id)) {
      Log.i(
        TAG,
        "System altered communication device to ${device?.productName}, immediately re-asserting ${targetDevice.productName}",
      )
      isRoutingActive = false
      setDeviceRouting(am, targetDevice, currentTarget is AudioTarget.PhoneSpeaker)
      scheduleReassertion()
    } else {
      isRoutingActive = true
    }
  }

  private fun registerBroadcastReceiver() {
    if (eventReceiver != null) return
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_USER_PRESENT)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
      }
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          val action = intent?.action ?: return
          Log.i(TAG, "Broadcast received: $action for currentTarget: $currentTarget")

          if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            isPhoneCallActive =
              (state == TelephonyManager.EXTRA_STATE_RINGING ||
                state == TelephonyManager.EXTRA_STATE_OFFHOOK)
            Log.i(TAG, "Phone call state changed: $state (isPhoneCallActive: $isPhoneCallActive)")
            if (isPhoneCallActive) {
              stopKeepAlive()
              audioManager?.let { clearDeviceRouting(it) }
              return
            } else {
              applyRouting()
              return
            }
          }

          applyRouting()
          scheduleReassertion()
        }
      }
    eventReceiver = receiver
    try {
      context.registerReceiver(receiver, filter)
      Log.i(TAG, "Registered screen, Bluetooth, and telephony broadcast receiver")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to register broadcast receiver", e)
    }
  }

  private fun unregisterBroadcastReceiver() {
    eventReceiver?.let {
      try {
        context.unregisterReceiver(it)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to unregister broadcast receiver", e)
      }
      eventReceiver = null
    }
  }

  /** Shuts down the router, clearing device overrides and unregistering callbacks. */
  fun shutdown() {
    stopKeepAlive()
    val am = audioManager
    if (am != null) {
      audioDeviceCallback?.let { am.unregisterAudioDeviceCallback(it) }
      unregisterCommunicationDeviceListener(am)
      am.unregisterAudioPlaybackCallback(playbackCallback)
      am.unregisterAudioRecordingCallback(recordingCallback)
      clearDeviceRouting(am)
    }
    unregisterBroadcastReceiver()
    try {
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to unregister prefChangeListener", e)
    }
    mainHandler.removeCallbacksAndMessages(null)
    audioDeviceCallback = null
    isMonitoring = false
    isRoutingActive = false
    if (instance == this) {
      instance = null
    }
  }

  /** Returns the active target [AudioDeviceInfo], or null if using system default. */
  fun getTargetAudioDevice(): AudioDeviceInfo? {
    val am = audioManager ?: return null
    return when (val target = currentTarget) {
      is AudioTarget.SystemDefault -> null
      is AudioTarget.PhoneSpeaker -> speakerToForce(am)
      is AudioTarget.AnyExternal -> findExternalDevice(am)
      is AudioTarget.SpecificDevice -> findSpecificDevice(am, target) ?: findExternalDevice(am)
    }
  }

  private fun setDeviceRouting(am: AudioManager, device: AudioDeviceInfo, forceSpeakerOn: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Do not touch isSpeakerphoneOn here. On Android 12+ setSpeakerphoneOn(false) removes this
      // app's communication-device request, which undoes the call below and lets the sound fall
      // back to the speaker.
      val success =
        try {
          am.setCommunicationDevice(device)
        } catch (e: IllegalArgumentException) {
          // Not a valid communication device (for example a plain A2DP sink).
          Log.w(TAG, "setCommunicationDevice rejected ${device.productName} (type ${device.type})", e)
          false
        }
      isRoutingActive = success
      Log.i(TAG, "setCommunicationDevice(${device.productName} - type ${device.type}): $success")
      LogUtils.d(
        TAG,
        "setCommunicationDevice(%s - type %d): %b",
        device.productName,
        device.type,
        success,
      )
    } else {
      setLegacySpeakerphone(am, forceSpeakerOn)
      isRoutingActive = forceSpeakerOn
    }
  }

  /**
   * Android 11 and older only: speakerphone is a global switch shared with the dialer. Only turn it
   * off again if this router turned it on, and never touch it during a call, so a speaker the user
   * chose in the call screen is left alone.
   */
  @Suppress("DEPRECATION")
  private fun setLegacySpeakerphone(am: AudioManager, on: Boolean) {
    if (isPhoneCallActive) return
    if (on) {
      if (!am.isSpeakerphoneOn) {
        am.isSpeakerphoneOn = true
        speakerphoneSetByUs = true
        Log.i(TAG, "isSpeakerphoneOn set to true")
      }
    } else if (speakerphoneSetByUs) {
      am.isSpeakerphoneOn = false
      speakerphoneSetByUs = false
      Log.i(TAG, "isSpeakerphoneOn set to false")
    }
  }

  private fun clearDeviceRouting(am: AudioManager) {
    isRoutingActive = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      am.clearCommunicationDevice()
      Log.i(TAG, "clearCommunicationDevice() executed")
    } else {
      setLegacySpeakerphone(am, false)
      // If a call is active the user owns the speaker setting now; forget that it was ours.
      speakerphoneSetByUs = false
    }
  }

  @VisibleForTesting
  fun findSpeakerDevice(am: AudioManager): AudioDeviceInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val commDevices = am.availableCommunicationDevices
      val speaker = commDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
      if (speaker != null) return speaker
      val earpiece = commDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
      if (earpiece != null) return earpiece
    }
    val allDevices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    return allDevices.find { it.isSink && it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
      ?: allDevices.find { it.isSink && it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
  }

  /**
   * The speaker to route to, only while headphones or another external output would take the sound
   * otherwise. Routing goes through the communication route, which plays on the speaker with the
   * speakerphone's tuning, noticeably quieter than the speaker as the system default. With nothing
   * external connected, the system default is already the speaker.
   */
  private fun speakerToForce(am: AudioManager): AudioDeviceInfo? =
    if (findExternalDevice(am) != null) findSpeakerDevice(am) else null

  @VisibleForTesting
  fun findExternalDevice(am: AudioManager): AudioDeviceInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val commDevices = am.availableCommunicationDevices
      val ext = commDevices.find { isExternalDevice(it) }
      if (ext != null) return ext
    }
    val allDevices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    return allDevices.find { isExternalDevice(it) }
  }

  @VisibleForTesting
  fun findSpecificDevice(am: AudioManager, target: AudioTarget.SpecificDevice): AudioDeviceInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val commDevices = am.availableCommunicationDevices.filter { isExternalDevice(it) }
      val commMatch = commDevices.find { dev ->
        (target.type == 0 || dev.type == target.type) &&
          getDeviceDisplayName(dev, context).equals(target.name, ignoreCase = true)
      } ?: commDevices.find { dev ->
        getDeviceDisplayName(dev, context).equals(target.name, ignoreCase = true)
      } ?: commDevices.find { dev ->
        dev.productName?.toString()?.equals(target.name, ignoreCase = true) == true
      }
      if (commMatch != null) {
        return commMatch
      }
    }

    val allDevices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { isExternalDevice(it) }
    return allDevices.find { dev ->
      (target.type == 0 || dev.type == target.type) &&
        getDeviceDisplayName(dev, context).equals(target.name, ignoreCase = true)
    } ?: allDevices.find { dev ->
      getDeviceDisplayName(dev, context).equals(target.name, ignoreCase = true)
    } ?: allDevices.find { dev ->
      dev.productName?.toString()?.equals(target.name, ignoreCase = true) == true
    }
  }

  companion object {
    private const val TAG = "AudioDeviceRouter"
    // Gaps between re-assertion passes, covering slow Bluetooth profile switches.
    private val REASSERTION_DELAYS_MS = longArrayOf(150L, 300L, 500L, 800L, 1200L, 2000L)

    @Volatile
    @JvmStatic
    var instance: AudioDeviceRouter? = null
      private set

    /**
     * Whether an app is listening as a call or a voice assistant does. A voice recorder, which uses
     * the plain microphone, worked all along, so it doesn't count.
     */
    @VisibleForTesting
    @JvmStatic
    fun isVoiceInput(configs: List<AudioRecordingConfiguration>?): Boolean =
      configs.orEmpty().any {
        it.clientAudioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION ||
          it.clientAudioSource == MediaRecorder.AudioSource.VOICE_RECOGNITION
      }

    /** Checks whether an [AudioDeviceInfo] represents an external device. */
    fun isExternalDevice(device: AudioDeviceInfo): Boolean {
      if (!device.isSink) return false
      return when (device.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_HEARING_AID -> true
        else -> false
      }
    }

    /** Returns human-readable display name for an [AudioDeviceInfo]. */
    fun getDeviceDisplayName(device: AudioDeviceInfo, context: Context): String {
      val productName = device.productName?.toString()?.trim()
      if (!productName.isNullOrEmpty() &&
          !productName.equals("null", ignoreCase = true) &&
          !productName.equals("unknown", ignoreCase = true)) {
        return productName
      }
      return when (device.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST ->
          context.getString(R.string.value_audio_output_device_bluetooth_default)
        AudioDeviceInfo.TYPE_WIRED_HEADSET ->
          context.getString(R.string.value_audio_output_device_wired_headset)
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES ->
          context.getString(R.string.value_audio_output_device_wired_headphones)
        AudioDeviceInfo.TYPE_USB_HEADSET ->
          context.getString(R.string.value_audio_output_device_usb_headset)
        AudioDeviceInfo.TYPE_USB_DEVICE ->
          context.getString(R.string.value_audio_output_device_usb_device)
        else ->
          context.getString(R.string.value_audio_output_device_bluetooth_or_wired)
      }
    }

    /**
     * Dynamically populates entries and entry values for the audio output device [ListPreference].
     */
    @JvmStatic
    fun updatePreference(preference: ListPreference, context: Context) {
      val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      val connectedDevices =
        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()

      val externalDevices = connectedDevices.filter { isExternalDevice(it) }

      val entries = mutableListOf<CharSequence>()
      val entryValues = mutableListOf<CharSequence>()

      entries.add(context.getString(R.string.value_audio_output_device_default))
      entryValues.add("default")

      entries.add(context.getString(R.string.value_audio_output_device_speaker))
      entryValues.add("speaker")

      val seenNames = mutableSetOf<String>()
      for (dev in externalDevices) {
        val displayName = getDeviceDisplayName(dev, context)
        if (seenNames.add(displayName)) {
          entries.add(displayName)
          entryValues.add("device:${dev.type}:$displayName")
        }
      }

      preference.entries = entries.toTypedArray()
      preference.entryValues = entryValues.toTypedArray()

      val currentValue = preference.value
      val currentTarget = AudioTarget.parse(currentValue)
      val summaryText = when (currentTarget) {
        is AudioTarget.SystemDefault ->
          context.getString(R.string.value_audio_output_device_default)
        is AudioTarget.PhoneSpeaker ->
          context.getString(R.string.value_audio_output_device_speaker)
        is AudioTarget.SpecificDevice -> {
          val matchIndex = entryValues.indexOfFirst {
            it.toString() == currentValue || it.toString().endsWith(":${currentTarget.name}")
          }
          if (matchIndex >= 0) {
            entries[matchIndex].toString()
          } else {
            "${currentTarget.name} (${context.getString(R.string.value_audio_output_device_disconnected)})"
          }
        }
        is AudioTarget.AnyExternal -> {
          if (externalDevices.isNotEmpty()) {
            getDeviceDisplayName(externalDevices.first(), context)
          } else {
            context.getString(R.string.value_audio_output_device_default)
          }
        }
      }
      preference.summary = summaryText
    }
  }
}
