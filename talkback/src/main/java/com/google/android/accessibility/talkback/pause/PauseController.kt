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

package com.google.android.accessibility.talkback.pause

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.focusmanagement.TraversalTreeCache
import com.google.android.accessibility.talkback.keyboard.KeyCombo
import com.google.android.accessibility.talkback.keyboard.KeyComboManager
import com.google.android.accessibility.talkback.keyboard.KeyComboModel
import com.google.android.accessibility.utils.Performance
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_INTERRUPT
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Pauses Backtalk, like TalkBack's suspend before Android 8. While paused, explore by touch is off,
 * so the screen works as if no screen reader is on, and Backtalk drops accessibility events,
 * gestures and all its feedback. The service stays bound, so resuming is instant.
 *
 * Backtalk resumes from the ongoing notification, the pause keyboard shortcut, three quick presses
 * of volume down, or, depending on a setting, when the screen turns on or the lock screen shows.
 *
 * The pause lives only in memory. If the service restarts while paused, Backtalk starts unpaused,
 * because a screen reader stuck paused after a crash would leave the user with no way back.
 *
 * Everything runs on the main thread.
 */
class PauseController(
  private val service: AccessibilityService,
  private val feedback: Pipeline.FeedbackReturner,
  private val keyComboManager: KeyComboManager,
  private val host: Host,
) {

  /** What the service does when Backtalk pauses or resumes. */
  interface Host {
    /** Stops reading, menus and queued feedback, before Backtalk says it is pausing. */
    fun stopForPause()

    /** Turns explore by touch and the parts that depend on it off, or back on. */
    fun onPausedChanged(paused: Boolean)
  }

  private val prefs = SharedPreferencesUtils.getSharedPreferences(service)
  private val notificationManager = service.getSystemService(NotificationManager::class.java)
  private val keyguardManager = service.getSystemService(KeyguardManager::class.java)
  private val volumeDownPresses = TriplePressDetector()
  private var dialog: PauseDialog? = null
  private var receiverRegistered = false

  // A key-up goes where its key-down went, even when the pause changes in between. Keys held when
  // Backtalk paused get their key-ups sent to the key combo manager, which saw their key-downs.
  // Keys held when Backtalk resumed pass to the app, which saw theirs.
  private val heldKeys = mutableSetOf<Int>()
  private val keyUpsForKeyCombos = mutableSetOf<Int>()
  private val keyUpsToPass = mutableSetOf<Int>()
  private var keyUpToConsume = KeyEvent.KEYCODE_UNKNOWN

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
          ACTION_RESUME -> resume()
          Intent.ACTION_SCREEN_ON -> if (shouldResumeOnScreenOn()) resume()
        }
      }
    }

  init {
    // A notification left over from a service that ended while paused.
    pausedNow = false
    notificationManager?.cancel(R.id.notification_backtalk_paused)
  }

  /** Pauses, after asking first if the user has not turned the question off. */
  fun requestPause() {
    if (pausedNow) {
      return
    }
    val pauseDialog = dialog ?: PauseDialog(service) { pause() }.also { dialog = it }
    if (pauseDialog.shouldShowDialogPref) {
      pauseDialog.show(howToResume())
    } else {
      pause()
    }
  }

  /** Resumes when paused, otherwise asks to pause. For the keyboard shortcut. */
  fun toggle() {
    if (pausedNow) resume() else requestPause()
  }

  fun pause() {
    if (pausedNow || !TalkBackService.isServiceActive()) {
      return
    }
    LogUtils.i(TAG, "Pausing Backtalk")
    host.stopForPause()
    // The focus highlight would stay on screen while nothing can move it.
    service
      .findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
      ?.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
    // Said before the pause, which drops all later feedback. Speech already handed to the speech
    // engine finishes.
    announce(R.raw.chime_down, R.string.backtalk_paused)
    pausedNow = true
    keyUpsForKeyCombos.clear()
    keyUpsForKeyCombos.addAll(heldKeys)
    keyUpsToPass.clear()
    volumeDownPresses.reset()
    host.onPausedChanged(true)
    registerReceiver()
    showNotification()
  }

  fun resume() {
    if (!pausedNow) {
      return
    }
    LogUtils.i(TAG, "Resuming Backtalk")
    pausedNow = false
    keyUpsForKeyCombos.clear()
    keyUpsToPass.clear()
    keyUpsToPass.addAll(heldKeys)
    keyUpsToPass.remove(keyUpToConsume)
    unregisterReceiver()
    notificationManager?.cancel(R.id.notification_backtalk_paused)
    // The screen changed without Backtalk seeing it.
    TraversalTreeCache.clear("resume")
    host.onPausedChanged(false)
    announce(R.raw.chime_up, R.string.backtalk_resumed)
  }

  /** Ends the pause without feedback when the service shuts down. */
  fun shutdown() {
    dialog?.dismissDialog()
    pausedNow = false
    unregisterReceiver()
    notificationManager?.cancel(R.id.notification_backtalk_paused)
  }

  /**
   * Sees every key event before the rest of Backtalk. Returns true to consume the event, false to
   * pass it to the app, or null to let Backtalk handle it as usual.
   *
   * While paused, keys pass to the app, so volume keys still change the volume. Three quick
   * presses of volume down, or the pause keyboard shortcut, resume Backtalk.
   */
  fun onKeyEvent(event: KeyEvent): Boolean? {
    val keyCode = event.keyCode
    when (event.action) {
      KeyEvent.ACTION_DOWN -> {
        if (event.repeatCount == 0) {
          heldKeys.add(keyCode)
        } else if (keyCode == keyUpToConsume) {
          return true
        } else if (keyCode in keyUpsToPass) {
          return false
        }
      }
      KeyEvent.ACTION_UP -> {
        heldKeys.remove(keyCode)
        if (keyCode == keyUpToConsume) {
          keyUpToConsume = KeyEvent.KEYCODE_UNKNOWN
          return true
        }
        if (keyUpsToPass.remove(keyCode)) {
          return false
        }
        if (pausedNow && keyUpsForKeyCombos.remove(keyCode)) {
          return keyComboManager.onKeyEvent(event, null)
        }
      }
    }
    if (!pausedNow) {
      return null
    }
    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
      if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && volumeDownPresses.onPress(event.eventTime)) {
        resume()
        return false
      }
      if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
        volumeDownPresses.reset()
      }
      if (isPauseKeyCombo(event)) {
        keyUpToConsume = keyCode
        resume()
        return true
      }
    }
    return false
  }

  /** Whether the key-down is the pause keyboard shortcut, matched the way the key combo manager does. */
  private fun isPauseKeyCombo(event: KeyEvent): Boolean {
    val model = keyComboManager.keyComboModel ?: return false
    val target = pauseKeyCombo() ?: return false
    val triggerModifier = model.triggerModifier
    val hasTriggerModifier = triggerModifier != KeyComboModel.NO_MODIFIER
    val pressedTriggerModifier =
      hasTriggerModifier && (event.modifiers and triggerModifier) == triggerModifier
    if (hasTriggerModifier && !pressedTriggerModifier) {
      return false
    }
    val combo =
      KeyCombo(
        (event.modifiers and KeyCombo.KEY_EVENT_MODIFIER_MASK) and triggerModifier.inv(),
        KeyComboManager.getConvertedKeyCode(event),
        pressedTriggerModifier,
      )
    return combo.matchWith(triggerModifier, target) == KeyCombo.EXACT_MATCH
  }

  /** The key combo assigned to pausing, or null when none is. */
  private fun pauseKeyCombo(): KeyCombo? {
    val model = keyComboManager.keyComboModel ?: return null
    val combo =
      model.getKeyComboForKey(service.getString(R.string.keycombo_shortcut_global_pause_backtalk))
    return combo.takeIf {
      it.keyComboCode != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()
    }
  }

  private fun resumeMode(): String =
    SharedPreferencesUtils.getStringPref(
      prefs,
      service.resources,
      R.string.pref_resume_backtalk_key,
      R.string.pref_resume_backtalk_default,
    ) ?: service.getString(R.string.resume_screen_keyguard)

  private fun shouldResumeOnScreenOn(): Boolean =
    when (resumeMode()) {
      service.getString(R.string.resume_screen_on) -> true
      service.getString(R.string.resume_screen_manual) -> false
      else -> keyguardManager?.isKeyguardLocked == true
    }

  private fun howToResume(): String {
    val message =
      service.getString(
        when (resumeMode()) {
          service.getString(R.string.resume_screen_on) -> R.string.message_resume_screen_on
          service.getString(R.string.resume_screen_manual) -> R.string.message_resume_manual
          else -> R.string.message_resume_keyguard
        }
      )
    val keyCombo = pauseKeyCombo() ?: return message
    return message +
      " " +
      service.getString(
        R.string.message_resume_key_combo,
        keyComboManager.getKeyComboStringRepresentation(keyCombo),
      )
  }

  private fun announce(@RawRes sound: Int, @StringRes text: Int) {
    val options =
      SpeakOptions.create()
        .setQueueMode(QUEUE_MODE_INTERRUPT)
        .setFlags(
          FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE or
            FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE or
            FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
        )
    feedback.returnFeedback(
      Performance.EVENT_ID_UNTRACKED,
      Feedback.sound(sound).speech(service.getString(text), options),
    )
  }

  private fun registerReceiver() {
    if (receiverRegistered) {
      return
    }
    val filter =
      IntentFilter().apply {
        addAction(ACTION_RESUME)
        addAction(Intent.ACTION_SCREEN_ON)
      }
    // System broadcasts still arrive at a receiver that is not exported.
    ContextCompat.registerReceiver(service, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    receiverRegistered = true
  }

  private fun unregisterReceiver() {
    if (!receiverRegistered) {
      return
    }
    try {
      service.unregisterReceiver(receiver)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Pause receiver was not registered: %s", e)
    }
    receiverRegistered = false
  }

  private fun showNotification() {
    val intent = Intent(ACTION_RESUME).setPackage(service.packageName)
    val pendingIntent =
      PendingIntent.getBroadcast(
        service,
        /* requestCode= */ 0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    // A channel of its own, of low importance, puts it in the Silent section, apart from Backtalk's
    // other notifications. Android groups notifications from one app in a section, and a tap on
    // this one in a collapsed group does nothing, while paused Backtalk can't help find the group.
    notificationManager?.createNotificationChannel(
      NotificationChannel(
        NOTIFICATION_CHANNEL,
        service.getString(R.string.notification_channel_backtalk_paused),
        NotificationManager.IMPORTANCE_LOW,
      )
    )
    val notification =
      NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
        .setSmallIcon(R.drawable.quantum_gm_ic_accessibility_new_vd_theme_24)
        .setContentTitle(service.getString(R.string.notification_title_backtalk_paused))
        .setContentText(service.getString(R.string.notification_message_backtalk_paused))
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setSilent(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()
    try {
      notificationManager?.notify(R.id.notification_backtalk_paused, notification)
    } catch (e: SecurityException) {
      // Without permission to post notifications, the other ways to resume still work.
      LogUtils.w(TAG, "Cannot show the paused notification: %s", e)
    }
  }

  companion object {
    private const val TAG = "PauseController"
    private const val ACTION_RESUME = "com.google.android.accessibility.talkback.RESUME_BACKTALK"
    private const val NOTIFICATION_CHANNEL = "backtalk_paused"

    @Volatile private var pausedNow = false

    /** Whether Backtalk is paused. Backtalk drops events, gestures and feedback while it is. */
    @JvmStatic fun isPaused(): Boolean = pausedNow
  }
}
