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

package com.google.android.accessibility.talkback.calls

import android.app.Notification
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.compositor.CompositorUtils
import com.google.android.accessibility.talkback.compositor.rule.EventTypeNotificationStateChangedFeedbackRule
import com.google.android.accessibility.talkback.monitor.CallStateMonitor
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.function.BooleanSupplier
import java.util.function.IntSupplier

/**
 * The caller of an incoming call: Announce caller, which says the call notification once, repeats
 * it while the call rings, or leaves it unsaid, and the Say caller command. The caller is said as
 * the call notification's announcement says it.
 *
 * Repeats run only while a call rings: from the first announcement until neither the call state, the
 * audio mode nor an incoming call notification in the shade shows a ringing call, the call state
 * changes, the setting changes, [MAX_REPEAT_MS] has passed, or SandFly stops. Without notification
 * access, an app's call is repeated only while the audio mode is the ringtone's.
 */
class CallerAnnouncer
@JvmOverloads
constructor(
  private val context: Context,
  private val speak: (CharSequence) -> Unit,
  private val callState: IntSupplier,
  private val isSpeaking: BooleanSupplier,
  private val audioMode: IntSupplier = IntSupplier { AnswerCall.audioMode(context) },
  /** The caller of the newest incoming call in the shade, or null; null without notification access. */
  private val callerInShade: () -> CharSequence? = {
    CallNotifications.incomingCallInShade(context)?.let { callerText(context, it) }
  },
  private val listenerConnected: BooleanSupplier = BooleanSupplier {
    NotificationListService.instance != null
  },
  private val clock: () -> Long = SystemClock::uptimeMillis,
  private val handler: Handler = Handler(Looper.getMainLooper()),
) : CallStateMonitor.CallStateChangedListener {

  /** Announce caller choices. */
  enum class Mode {
    ONCE,
    REPEAT,
    OFF,
  }

  /** The caller of the last incoming call notification, until the call state changes. */
  private var lastCaller: CharSequence? = null
  private var lastCallerAt = 0L
  /** When repeats started, or null while none run. */
  private var repeatsStartedAt: Long? = null

  private val repeat =
    object : Runnable {
      override fun run() {
        val started = repeatsStartedAt ?: return
        if (mode(context) != Mode.REPEAT || clock() - started >= MAX_REPEAT_MS) {
          stopRepeats()
          return
        }
        val inShade = callerInShade()
        if (inShade == null && !AnswerCall.isRinging(callState.asInt, audioMode.asInt)) {
          // Answered, declined or ended, or an app's call without notification access.
          stopRepeats()
          return
        }
        if (isSpeaking.asBoolean) {
          handler.postDelayed(this, BUSY_RETRY_MS)
          return
        }
        val caller = inShade ?: lastCaller
        if (caller == null) {
          stopRepeats()
          return
        }
        speak(caller)
        handler.postDelayed(this, REPEAT_MS)
      }
    }

  /** Notes the notification of an incoming call, from an accessibility event. */
  @MainThread
  fun onIncomingCall(notification: Notification) {
    val caller = callerText(context, notification)
    if (caller.isBlank()) return
    lastCaller = caller
    lastCallerAt = clock()
  }

  /** Whether the notification of an incoming call is announced as it arrives. */
  fun announcesIncomingCalls(): Boolean = mode(context) != Mode.OFF

  /**
   * Called when the notification of an incoming call is announced, to repeat it while the call
   * rings with Announce caller set to repeat.
   */
  @MainThread
  fun onIncomingCallAnnounced() {
    if (mode(context) != Mode.REPEAT) return
    handler.removeCallbacks(repeat)
    if (repeatsStartedAt == null) {
      LogUtils.d(TAG, "Repeating the caller while the call rings")
      repeatsStartedAt = clock()
    }
    handler.postDelayed(repeat, REPEAT_MS)
  }

  /** Whether [notification] of [packageName], in the call category, is an incoming call. */
  fun isIncomingCall(packageName: CharSequence?, notification: Notification): Boolean =
    CallNotifications.isIncomingCall(packageName, notification) ||
      AnswerCall.isRinging(callState.asInt, audioMode.asInt)

  /** What Say caller says: the caller of the ringing call, or that no call rings. */
  @MainThread
  fun sayCallerText(): CharSequence {
    val state = callState.asInt
    val mode = audioMode.asInt
    val recent = lastCaller?.takeIf { clock() - lastCallerAt < RECENT_MS }
    return choose(
      inShade = callerInShade(),
      recent = recent,
      ringing = AnswerCall.isRinging(state, mode),
      inCall = isInCall(state, mode),
      listenerConnected = listenerConnected.asBoolean,
    )
      ?: context.getString(
        if (AnswerCall.isRinging(state, mode)) R.string.say_caller_unknown
        else R.string.answer_call_no_call
      )
  }

  override fun onCallStateChanged(oldState: Int, newState: Int) {
    if (newState == TelephonyManager.CALL_STATE_RINGING) return
    // Answered, declined or ended.
    stopRepeats()
    lastCaller = null
  }

  /** Called when SandFly stops. */
  @MainThread
  fun shutdown() {
    stopRepeats()
    lastCaller = null
  }

  private fun stopRepeats() {
    handler.removeCallbacks(repeat)
    if (repeatsStartedAt != null) LogUtils.d(TAG, "Caller repeats stopped")
    repeatsStartedAt = null
  }

  companion object {
    private const val TAG = "CallerAnnouncer"

    /** How long after an announcement the caller is said again. */
    const val REPEAT_MS = 5_000L
    /** How long a repeat waits for SandFly to stop speaking before it checks again. */
    const val BUSY_RETRY_MS = 1_000L
    /** How long repeats run at most, in case a ringing state is never cleared. */
    const val MAX_REPEAT_MS = 180_000L
    /** How long the caller of a call notification is kept for Say caller without the shade. */
    const val RECENT_MS = 60_000L

    private const val MODE_REPEAT = "repeat"
    private const val MODE_OFF = "off"

    /** The Announce caller setting. */
    @JvmStatic
    fun mode(context: Context): Mode =
      parse(
        SharedPreferencesUtils.getStringPref(
          SharedPreferencesUtils.getSharedPreferences(context),
          context.resources,
          R.string.pref_announce_caller_key,
          R.string.pref_announce_caller_default,
        )
      )

    @JvmStatic
    fun parse(value: String?): Mode =
      when (value) {
        MODE_REPEAT -> Mode.REPEAT
        MODE_OFF -> Mode.OFF
        else -> Mode.ONCE
      }

    /**
     * The caller Say caller says, or null when no call rings: the incoming call in the shade, else
     * while a call rings the caller of the [recent] call notification, else without the shade and
     * outside a call, the [recent] one, which may be an app's call that rings.
     */
    @JvmStatic
    fun choose(
      inShade: CharSequence?,
      recent: CharSequence?,
      ringing: Boolean,
      inCall: Boolean,
      listenerConnected: Boolean,
    ): CharSequence? =
      when {
        inShade != null -> inShade
        ringing -> recent
        listenerConnected || inCall -> null
        else -> recent
      }

    /** Whether a call is in progress, from the call state or the audio mode. */
    @JvmStatic
    fun isInCall(callState: Int, audioMode: Int): Boolean =
      callState == TelephonyManager.CALL_STATE_OFFHOOK ||
        audioMode == AudioManager.MODE_IN_CALL ||
        audioMode == AudioManager.MODE_IN_COMMUNICATION

    /** The text of a call notification, as its announcement says it. */
    @JvmStatic
    fun callerText(context: Context, notification: Notification): CharSequence =
      try {
        CompositorUtils.joinCharSequences(
          EventTypeNotificationStateChangedFeedbackRule.getNotificationCategoryStateText(
            context,
            notification,
          ),
          EventTypeNotificationStateChangedFeedbackRule.getNotificationDetailsStateText(
            notification
          ),
        )
      } catch (e: RuntimeException) {
        // BadParcelableException from an app's extras.
        LogUtils.w(TAG, "Can't read a call notification: %s", e)
        ""
      }

    /** Creates one that speaks through [pipeline], also during a call and over media. */
    @JvmStatic
    fun create(
      context: Context,
      pipeline: Pipeline.FeedbackReturner,
      callState: IntSupplier,
      isSpeaking: BooleanSupplier,
    ): CallerAnnouncer =
      CallerAnnouncer(
        context,
        { text ->
          pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(
              text,
              SpeechController.SpeakOptions.create()
                .setQueueMode(SpeechController.QUEUE_MODE_QUEUE)
                .setFlags(
                  FeedbackItem.FLAG_NO_HISTORY or
                    FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE or
                    FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE or
                    FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
                ),
            ),
          )
        },
        callState,
        isSpeaking,
      )
  }
}
