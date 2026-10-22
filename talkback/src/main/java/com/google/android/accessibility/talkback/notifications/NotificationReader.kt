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

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.LocaleSpan
import android.util.LruCache
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.compositor.rule.EventTypeNotificationStateChangedFeedbackRule
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.talkback.scripting.Scripts
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.accessibility.utils.output.VoiceProfiles
import com.google.android.accessibility.utils.SettingsUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import java.util.Date
import java.util.Locale

/**
 * Reads notifications as the notification settings say: which ones, how, when, and what stops
 * them. Every notification that would be spoken passes through [handle], which reads it, sets it
 * aside for a delay, or leaves it unread, and records it in [NotificationHistory].
 *
 * Calls are not handled here, so the caller is always said.
 */
object NotificationReader {
  private const val TAG = "NotificationReader"
  private const val LANGUAGE_WAIT_MS = 600L
  private const val MAX_REPEATING = 10
  private const val VOLUME_KEY_PAIR_MS = 400L
  /** The most notifications set aside for a delay; older ones are dropped in a burst. */
  private const val MAX_WAITING = 20
  /** How often, and for how long at most, another voice waits for SandFly to stop speaking. */
  private const val QUIET_CHECK_MS = 200L
  private const val QUIET_WAIT_MS = 10_000L
  /** How long after both volume keys go down their ups are taken. */
  private const val VOLUME_UP_WAIT_MS = 2_000L
  /** Marks the callbacks of readouts that wait for SandFly to be quiet, to drop them on stop. */
  private val QUIET_TOKEN = Any()
  /** Marks the callbacks of notifications waiting for the listener's Do Not Disturb ranking. */
  private val DND_TOKEN = Any()
  private const val DND_CHECK_MS = 200L
  private const val DND_CHECKS = 5
  /** How long the notification listener waits for a notification's accessibility event. */
  private const val EVENT_WAIT_MS = 1_500L
  /** How long a notification's accessibility event is remembered, and how many. */
  private const val EVENT_MEMORY_MS = 10_000L
  private const val MAX_RECENT_EVENTS = 50
  /** How long after a touch ends the notifications that arrived during it are read. */
  private const val AFTER_TOUCH_MS = 500L
  /**
   * The longest the first notification held for a touch waits for it to end, in case the end is
   * missed. Later ones don't extend it, so a touch that seems never to end doesn't gather them.
   */
  private const val TOUCH_WAIT_MS = 10_000L

  private val handler = Handler(Looper.getMainLooper())
  private var context: Context? = null
  private var feedback: Pipeline.FeedbackReturner? = null
  private var stopSpeech: Runnable? = null
  private var stateReader: DeviceStateReader? = null
  private var sensors: StopSensors? = null
  private var languageIdentifier: LanguageIdentifier? = null
  private var schedules: SpokenSchedules? = null
  private var voice: NotificationVoice? = null
  private var isSandFlySpeaking: () -> Boolean = { false }
  private var isSilenced: (Boolean, Boolean) -> Boolean = { _, _ -> false }
  private var cachedSettings: NotificationSettings? = null
  private val decider = NotificationDecider()
  private val appNames = LruCache<String, String>(100)

  /** The notification being read now. */
  private var reading: Readout? = null
  /** Notifications handed to speech that haven't started. */
  private val queued = mutableListOf<Readout>()
  /** Notifications set aside for a delay. */
  private val waiting = mutableListOf<Readout>()
  /** Notifications that arrived while the screen was touched, read once the touch ends. */
  private val heldForTouch = mutableListOf<Readout>()
  private val releaseHeld = Runnable { releaseTouchHeld() }
  /** Notifications read while the screen was off, said again until it turns on. */
  private val repeating = mutableListOf<Readout>()
  private var focusRequest: AudioFocusRequest? = null
  private var volumeUpDownAt = 0L
  private var volumeDownDownAt = 0L
  /** The volume keys whose ups are taken, after both went down to stop speech, and until when. */
  private val swallowVolumeUps = mutableSetOf<Int>()
  private var swallowVolumeUpsUntil = 0L

  private class Readout(
    val info: NotificationInfo,
    val decision: Decision,
    var text: CharSequence,
    val settings: NotificationSettings,
  ) {
    var started = false
    var cancelled = false
    val speak = Runnable { NotificationReader.speak(this) }

    fun copy() = Readout(info, decision, text, settings)
  }

  private val stopAfterLimit = Runnable { stop() }
  private val repeat =
    object : Runnable {
      override fun run() {
        val context = context ?: return
        if (repeating.isEmpty() || ScreenState.isOn(context) || !settings(context).enabled) {
          repeating.clear()
          return
        }
        repeating.forEach { speak(it.copy(), repeat = true) }
        val minutes = repeating.first().settings.repeatMinutes
        if (minutes > 0) handler.postDelayed(this, minutes * 60_000L)
      }
    }

  private val screenOnReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        repeating.clear()
        handler.removeCallbacks(repeat)
      }
    }

  private val settingsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
      if (key == null || key.startsWith("pref_notification") || key == NotificationSettings.Companion.Keys.ENABLED) {
        cachedSettings = null
      }
      if (key != null && key.startsWith("pref_notification")) decider.forget()
      if (key == NotificationSettings.Companion.Keys.VOICE_PROFILE || VoiceProfiles.isProfileKey(key)) {
        updateVoice(prefs)
      }
    }

  /**
   * Called when SandFly starts, with how to speak and how to stop notification speech, whether
   * SandFly is speaking, and whether its speech is silenced now, such as during a call. The
   * silence check counts audio playback only when its first value is false, and calls only
   * when its second value is false.
   */
  @JvmStatic
  @MainThread
  fun attach(
    context: Context,
    feedback: Pipeline.FeedbackReturner,
    stopSpeech: Runnable,
    isSandFlySpeaking: () -> Boolean,
    isSilenced: (overMedia: Boolean, duringCalls: Boolean) -> Boolean,
  ) {
    if (this.context != null) detach()
    val app = context.applicationContext
    this.context = app
    this.feedback = feedback
    this.stopSpeech = stopSpeech
    this.isSandFlySpeaking = isSandFlySpeaking
    this.isSilenced = isSilenced
    cachedSettings = null
    stateReader = DeviceStateReader(app)
    sensors = StopSensors(app) { stop() }
    ContextCompat.registerReceiver(
      app,
      screenOnReceiver,
      IntentFilter(Intent.ACTION_SCREEN_ON),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    SharedPreferencesUtils.getSharedPreferences(app).registerOnSharedPreferenceChangeListener(settingsListener)
    schedules = SpokenSchedules(app) { text, interrupt -> announce(text, interrupt) }.also { it.start() }
    updateVoice(SharedPreferencesUtils.getSharedPreferences(app))
  }

  /** Called when SandFly stops. */
  @JvmStatic
  @MainThread
  fun detach() {
    val app = context ?: return
    cancelAll()
    schedules?.stop()
    schedules = null
    voice?.shutdown()
    voice = null
    isSandFlySpeaking = { false }
    isSilenced = { _, _ -> false }
    cachedSettings = null
    runCatching { app.unregisterReceiver(screenOnReceiver) }
    SharedPreferencesUtils.getSharedPreferences(app).unregisterOnSharedPreferenceChangeListener(settingsListener)
    languageIdentifier?.close()
    languageIdentifier = null
    sensors?.stop()
    sensors = null
    stateReader = null
    feedback = null
    stopSpeech = null
    context = null
  }

  /** Tells which app is in front, for the rules. */
  @JvmStatic
  fun onActiveApp(packageName: CharSequence?) {
    if (packageName.isNullOrEmpty()) return
    stateReader?.foregroundApp = packageName.toString()
  }

  /** Whether a notification is being read now. */
  @JvmStatic val isReading: Boolean
    get() = reading?.started == true

  /**
   * Reads [notification], posted by [event]'s app, as the settings say, including what happens
   * while the screen is [touching] and while Do Not Disturb, [dnd], is on. Returns
   * false when it is left to the screen reader's usual speech: for calls, and when SandFly isn't
   * running.
   */
  @JvmStatic
  @MainThread
  fun handle(event: AccessibilityEvent, notification: Notification, touching: Boolean, dnd: Boolean): Boolean {
    val packageName = event.packageName?.toString() ?: return false
    noteEvent(packageName, notification)
    return handle(packageName, event, notification, touching, dnd)
  }

  /**
   * Handles a notification of [packageName], from its accessibility [event], or from the
   * notification listener when Android sent no event for it.
   */
  private fun handle(
    packageName: String,
    event: AccessibilityEvent?,
    notification: Notification,
    touching: Boolean,
    dnd: Boolean,
  ): Boolean {
    val context = context ?: return false
    if (stateReader == null) return false
    if (notification.category == Notification.CATEGORY_CALL) return false
    val touchBehavior = settings(context).touch
    if (touching && touchBehavior == TouchBehavior.SKIP) {
      noteUnread(packageName, notification, Outcome.TOUCHING)
      return true
    }
    // Read right away, or held until the touch ends.
    val hold = touching && touchBehavior == TouchBehavior.AFTER_TOUCH
    if (dnd) {
      when (settings(context).dnd) {
        DndMode.ALL -> {}
        DndMode.NONE -> {
          noteUnread(packageName, notification, Outcome.DO_NOT_DISTURB)
          return true
        }
        DndMode.ALLOWED -> {
          // The event is used after it is handled.
          @Suppress("DEPRECATION") val copy = event?.let { AccessibilityEvent.obtain(it) }
          whenDndDecided(packageName, notification, 1) { allowed ->
            if (allowed) read(packageName, copy, notification, hold)
            else noteUnread(packageName, notification, Outcome.DO_NOT_DISTURB)
          }
          return true
        }
      }
    }
    read(packageName, event, notification, hold)
    return true
  }

  /** Notifications that came as accessibility events lately, as [eventKey]s, and when. */
  private val recentEvents = ArrayDeque<Pair<String, Long>>()

  private fun eventKey(packageName: String, notification: Notification): String =
    "$packageName|${notification.`when`}|${notification.extras?.getCharSequence(Notification.EXTRA_TITLE)}"

  private fun noteEvent(packageName: String, notification: Notification) {
    val now = SystemClock.uptimeMillis()
    while (recentEvents.isNotEmpty() && now - recentEvents.first().second > EVENT_MEMORY_MS) {
      recentEvents.removeFirst()
    }
    while (recentEvents.size >= MAX_RECENT_EVENTS) recentEvents.removeFirst()
    recentEvents.addLast(eventKey(packageName, notification) to now)
  }

  /**
   * A new notification, or one whose text changed, that the notification listener received.
   * Android sends screen readers no accessibility event for a notification that Do Not Disturb
   * holds back, or for an update posted to alert only once, such as a camera app's latest motion
   * alert replacing the one before. If no event comes for it within [EVENT_WAIT_MS], it is handled
   * as an event would be. Progress and ongoing notifications are left out, as their text changes
   * as they run. [importance] is its channel's.
   */
  @JvmStatic
  @MainThread
  fun onListenerPosted(packageName: String, notification: Notification, importance: Int) {
    val context = context ?: return
    val flags = notification.flags
    if (packageName == context.packageName ||
      notification.category == Notification.CATEGORY_CALL ||
      notification.category == Notification.CATEGORY_PROGRESS ||
      (notification.extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0) > 0 ||
      flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0 ||
      flags and Notification.FLAG_GROUP_SUMMARY != 0 ||
      importance < NotificationManager.IMPORTANCE_LOW ||
      !settings(context).enabled
    ) {
      return
    }
    val key = eventKey(packageName, notification)
    handler.postDelayed(
      {
        if (recentEvents.none { it.first == key }) {
          LogUtils.d(TAG, "No accessibility event for a notification of %s; handling it", packageName)
          val dnd = SettingsUtils.getDoNotDisturbState(context) > 0
          handle(packageName, null, notification, touching = false, dnd = dnd)
        }
      },
      EVENT_WAIT_MS,
    )
  }

  /** Whether a call is announced while Do Not Disturb is on. */
  @JvmStatic
  @MainThread
  fun callAllowedDuringDnd(event: AccessibilityEvent, notification: Notification): Boolean {
    val context = context ?: return false
    val packageName = event.packageName?.toString() ?: return false
    return when (settings(context).dnd) {
      DndMode.ALL -> true
      DndMode.NONE -> false
      DndMode.ALLOWED ->
        NotificationListService.instance?.matchesInterruptionFilter(packageName, notification) == true
    }
  }

  /**
   * Tells [then] whether Do Not Disturb lets [notification] through. The listener can have the
   * notification's ranking a moment after the accessibility event, so it is asked again a few
   * times. Without notification access, nothing is let through.
   */
  private fun whenDndDecided(packageName: String, notification: Notification, attempt: Int, then: (Boolean) -> Unit) {
    val listener = NotificationListService.instance
    val allowed = listener?.matchesInterruptionFilter(packageName, notification)
    if (listener == null || allowed != null || attempt >= DND_CHECKS) {
      LogUtils.d(TAG, "Do Not Disturb lets a notification of %s through: %s", packageName, allowed)
      then(allowed == true)
      return
    }
    handler.postAtTime(
      { whenDndDecided(packageName, notification, attempt + 1, then) },
      DND_TOKEN,
      SystemClock.uptimeMillis() + DND_CHECK_MS,
    )
  }

  /** Called when a touch of the screen ends, to read the notifications that arrived during it. */
  @JvmStatic
  @MainThread
  fun onTouchEnded() {
    if (heldForTouch.isEmpty()) return
    handler.removeCallbacks(releaseHeld)
    handler.postDelayed(releaseHeld, AFTER_TOUCH_MS)
  }

  private fun releaseTouchHeld() {
    handler.removeCallbacks(releaseHeld)
    val held = heldForTouch.toList()
    heldForTouch.clear()
    LogUtils.d(TAG, "Reading %d notifications held while the screen was touched", held.size)
    held.forEach { schedule(it) }
  }

  private fun read(
    packageName: String,
    event: AccessibilityEvent?,
    notification: Notification,
    holdForTouch: Boolean,
  ) {
    val context = context ?: return
    val stateReader = stateReader ?: return
    val settings = settings(context)
    val info = NotificationInfo.from(packageName, appName(context, packageName), notification)
    val categoryName =
      EventTypeNotificationStateChangedFeedbackRule.getNotificationCategoryStateText(context, notification)
        .toString()
    val time = DateFormat.getTimeFormat(context).format(Date(info.postedAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis()))
    val decision =
      decider.decide(info, settings, stateReader.read(settings), System.currentTimeMillis(), categoryName, time)
    var text: CharSequence = decision.text
    if (decision.read) {
      // Scripts that change spoken notifications see the text the settings made.
      Scripts.rewriteSpeech(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED, event, null, text)
        ?.let { text = it }
    }
    if (settings.history) {
      NotificationHistory.add(
        NotificationHistory.Entry(info, System.currentTimeMillis(), decision.outcome, text.toString(), decision.rules)
      )
    }
    if (!decision.read || text.isBlank()) {
      LogUtils.d(TAG, "Not reading a notification of %s: %s", packageName, decision.outcome)
      return
    }
    val readout = Readout(info, decision, text, settings)
    val next = { if (holdForTouch) holdForTouch(readout) else schedule(readout) }
    if (settings.detectLanguage && !decision.private) {
      detectLanguage(context, readout) { next() }
    } else {
      next()
    }
  }

  private fun holdForTouch(readout: Readout) {
    while (heldForTouch.size >= MAX_WAITING) heldForTouch.removeAt(0)
    if (heldForTouch.isEmpty()) handler.postDelayed(releaseHeld, TOUCH_WAIT_MS)
    heldForTouch += readout
    LogUtils.d(TAG, "Holding a notification of %s until the touch ends", readout.info.packageName)
  }

  /**
   * Records in the history a notification that SandFly left unread before the settings were
   * applied, such as one that arrived while Do Not Disturb was on.
   */
  @JvmStatic
  @MainThread
  fun noteUnread(event: AccessibilityEvent, notification: Notification, outcome: Outcome) {
    val packageName = event.packageName?.toString() ?: return
    noteUnread(packageName, notification, outcome)
  }

  private fun noteUnread(packageName: String, notification: Notification, outcome: Outcome) {
    val context = context ?: return
    if (notification.category == Notification.CATEGORY_CALL || !settings(context).history) return
    val info = NotificationInfo.from(packageName, appName(context, packageName), notification)
    NotificationHistory.add(NotificationHistory.Entry(info, System.currentTimeMillis(), outcome, info.content, emptyList()))
  }

  /**
   * Records in the history an incoming call, which is announced as calls always are, whatever the
   * notification settings say.
   */
  @JvmStatic
  @MainThread
  fun noteCall(event: AccessibilityEvent, notification: Notification) {
    val context = context ?: return
    if (!settings(context).history) return
    val packageName = event.packageName?.toString() ?: return
    val info = NotificationInfo.from(packageName, appName(context, packageName), notification)
    NotificationHistory.add(
      NotificationHistory.Entry(info, System.currentTimeMillis(), Outcome.CALL, info.content, emptyList())
    )
  }

  /** Reads every notification in the shade. Returns false when SandFly isn't running. */
  @JvmStatic
  @MainThread
  fun readSummary(): Boolean {
    val context = context ?: return false
    SpokenSchedules.readSummary(context) { text, interrupt -> announce(text, interrupt) }
    return true
  }

  /**
   * Turns notification speech on or off, or the other way when [on] is null, and says which.
   * Returns the new state.
   */
  @JvmStatic
  @MainThread
  fun setSpeechOn(context: Context, on: Boolean?, announce: Boolean): Boolean {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val key = NotificationSettings.Companion.Keys.ENABLED
    val now = on ?: !prefs.getBoolean(key, true)
    prefs.edit().putBoolean(key, now).apply()
    if (!now) stop()
    if (announce) {
      feedback?.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
          context.getString(if (now) R.string.notification_speech_now_on else R.string.notification_speech_now_off),
          SpeakOptions.create()
            .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
            .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
        ),
      )
    }
    return now
  }

  /** Says again the last notification that was read. Returns false when there is none. */
  @JvmStatic
  @MainThread
  fun readLastAgain(): Boolean {
    val entry = NotificationHistory.lastRead() ?: return false
    val context = context ?: return false
    val settings = settings(context)
    speak(Readout(entry.info, Decision(Outcome.READ, entry.spoken), entry.spoken, settings), repeat = true)
    return true
  }

  /** Stops the notification being read, and those waiting to be. */
  @JvmStatic
  @MainThread
  fun stop() {
    val current = reading
    LogUtils.d(TAG, "Stopping notification speech")
    cancelAll()
    if (current != null) NotificationHistory.markStopped(current.info)
    SpokenSchedules.cancelSummary()
    stopSpeech?.run()
    voice?.stop()
  }

  /**
   * Called when a notification goes away: a notification being read that the user dismissed, or
   * that its app took back, stops when the settings say so.
   */
  @JvmStatic
  @MainThread
  fun onNotificationRemoved(packageName: String, postedAtMillis: Long, title: String, reason: Int) {
    val context = context ?: return
    val settings = settings(context)
    val byUser =
      reason == NotificationListenerService.REASON_CANCEL ||
        reason == NotificationListenerService.REASON_CLICK ||
        reason == NotificationListenerService.REASON_CANCEL_ALL ||
        (settings.watchDismissStops && reason == NotificationListenerService.REASON_LISTENER_CANCEL)
    val byApp = reason == NotificationListenerService.REASON_APP_CANCEL
    val stops = (byUser && settings.dismissStops) || (byApp && settings.appCancelStops)
    if (!stops) return
    fun matches(readout: Readout) =
      readout.info.packageName == packageName &&
        readout.info.postedAtMillis == postedAtMillis &&
        (title.isEmpty() || readout.info.title == title)
    waiting.filter(::matches).forEach { cancel(it) }
    queued.filter(::matches).forEach { it.cancelled = true }
    repeating.removeAll(::matches)
    if (reading?.let(::matches) == true) stop()
  }

  /**
   * Stops reading when both volume keys are pressed together, if the settings say so. Returns
   * whether [event] was used.
   */
  @JvmStatic
  @MainThread
  fun onKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
    if (event.action == KeyEvent.ACTION_UP) {
      if (SystemClock.uptimeMillis() > swallowVolumeUpsUntil) swallowVolumeUps.clear()
      return swallowVolumeUps.remove(keyCode)
    }
    if (event.repeatCount > 0 || !isReading) return false
    val context = context ?: return false
    if (!SharedPreferencesUtils.getSharedPreferences(context)
        .getBoolean(NotificationSettings.Companion.Keys.VOLUME_KEYS, false)) {
      return false
    }
    val now = SystemClock.uptimeMillis()
    val other: Long
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      volumeUpDownAt = now
      other = volumeDownDownAt
    } else {
      volumeDownDownAt = now
      other = volumeUpDownAt
    }
    if (now - other < VOLUME_KEY_PAIR_MS) {
      volumeUpDownAt = 0
      volumeDownDownAt = 0
      // Both keys' ups follow.
      swallowVolumeUps.clear()
      swallowVolumeUps += listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
      swallowVolumeUpsUntil = now + VOLUME_UP_WAIT_MS
      stop()
      return true
    }
    return false
  }

  private fun schedule(readout: Readout) {
    val delay = readout.settings.delaySeconds * 1000L
    if (delay <= 0) {
      speak(readout)
      return
    }
    while (waiting.size >= MAX_WAITING) cancel(waiting.first())
    waiting += readout
    handler.postDelayed(readout.speak, delay)
  }

  private fun speak(readout: Readout, repeat: Boolean = false) {
    waiting.remove(readout)
    if (readout.cancelled) return
    val feedback = feedback ?: return
    val settings = readout.settings
    val busy = reading != null
    var flags = 0
    var interrupt = false
    if (busy && !repeat) {
      interrupt = readout.decision.priority || settings.queueBehavior == QueueBehavior.INTERRUPT
      if (!interrupt && settings.queueBehavior == QueueBehavior.SKIP) {
        LogUtils.d(TAG, "Not reading a notification while another is read")
        if (settings.history) NotificationHistory.mark(readout.info, Outcome.BUSY)
        return
      }
      if (interrupt) {
        flags =
          FeedbackItem.FLAG_CLEAR_QUEUED_UTTERANCES_WITH_SAME_UTTERANCE_GROUP or
            FeedbackItem.FLAG_INTERRUPT_CURRENT_UTTERANCE_WITH_SAME_UTTERANCE_GROUP
      }
    }
    // SandFly's speech is silenced while audio plays, unless it is told otherwise.
    val overMedia = settings.media != MediaBehavior.SKIP
    if (overMedia) flags = flags or FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
    val duringCalls = settings.duringCalls
    if (duringCalls) flags = flags or FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
    queued += readout
    val voice = voiceFor(settings)
    // Silenced speech, such as during a call, goes to SandFly's speech, which drops it.
    if (voice != null && !isSilenced(overMedia, duringCalls)) {
      if (settings.soundBefore) feedback.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down))
      whenSandFlyIsQuiet(SystemClock.uptimeMillis()) {
        if (readout.cancelled) return@whenSandFlyIsQuiet
        if (isSilenced(overMedia, duringCalls)) {
          onDone(readout, SpeechController.STATUS_INTERRUPTED)
          return@whenSandFlyIsQuiet
        }
        voice.speak(
          readout.text,
          flush = interrupt,
          locale = localeOf(readout.text),
          listener =
            object : NotificationVoice.Listener {
              override fun onStart() = onStarted(readout)

              override fun onDone(spoken: Boolean) =
                onDone(readout, if (spoken) SpeechController.STATUS_SPOKEN else SpeechController.STATUS_INTERRUPTED)
            },
        )
      }
      return
    }
    val options =
      SpeakOptions.create()
        .setQueueMode(SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
        .setFlags(flags)
        .setUtteranceGroup(SpeechController.UTTERANCE_GROUP_NOTIFICATIONS)
        .setStartingAction { onStarted(readout) }
        .setCompletedAction { status -> onDone(readout, status) }
    val speech = Feedback.speech(readout.text, options)
    if (settings.soundBefore) speech.sound(R.raw.chime_down)
    feedback.returnFeedback(EVENT_ID_UNTRACKED, speech)
  }

  private fun onStarted(readout: Readout) {
    queued.remove(readout)
    LogUtils.d(TAG, "Reading a notification of %s", readout.info.packageName)
    reading = readout
    readout.started = true
    decider.markSpoken(readout.info.packageName, readout.decision, System.currentTimeMillis())
    val settings = readout.settings
    sensors?.start(settings)
    handler.removeCallbacks(stopAfterLimit)
    if (readout.decision.maxSeconds > 0) {
      handler.postDelayed(stopAfterLimit, readout.decision.maxSeconds * 1000L)
    }
    if (settings.media == MediaBehavior.PAUSE) requestFocus()
  }

  private fun onDone(readout: Readout, status: Int) {
    queued.remove(readout)
    if (reading === readout) {
      reading = null
      sensors?.stop()
      handler.removeCallbacks(stopAfterLimit)
      abandonFocus()
    }
    val context = context ?: return
    val minutes = readout.settings.repeatMinutes
    if (status == SpeechController.STATUS_SPOKEN && readout.started && !readout.cancelled && minutes > 0 &&
      !ScreenState.isOn(context) && repeating.none { it.info === readout.info }) {
      repeating += readout
      while (repeating.size > MAX_REPEATING) repeating.removeAt(0)
      handler.removeCallbacks(repeat)
      handler.postDelayed(repeat, minutes * 60_000L)
    }
  }

  /**
   * Says [text] in the notification voice, or SandFly's when there is none: summaries and time
   * announcements.
   */
  private fun announce(text: CharSequence, interrupt: Boolean) {
    val context = context ?: return
    val settings = settings(context)
    val overMedia = settings.media != MediaBehavior.SKIP
    val duringCalls = settings.duringCalls
    val voice = voiceFor(settings)
    if (voice != null && !isSilenced(overMedia, duringCalls)) {
      whenSandFlyIsQuiet(SystemClock.uptimeMillis()) {
        if (isSilenced(overMedia, duringCalls)) return@whenSandFlyIsQuiet
        voice.speak(text, interrupt, null, null)
      }
      return
    }
    val options =
      SpeakOptions.create()
        .setQueueMode(if (interrupt) SpeechController.QUEUE_MODE_INTERRUPT else SpeechController.QUEUE_MODE_QUEUE)
        .setUtteranceGroup(SpeechController.UTTERANCE_GROUP_NOTIFICATIONS)
        .setFlags(
          (if (overMedia) FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE else 0) or
            (if (duringCalls) FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE else 0)
        )
    feedback?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, options))
  }

  /**
   * The voice notifications are read with, or null for SandFly's: when none is chosen, its profile
   * is gone, or its engine didn't start.
   */
  private fun voiceFor(settings: NotificationSettings): NotificationVoice? {
    val voice = voice ?: return null
    if (settings.voiceProfile.isEmpty() || voice.failed || voice.currentProfile == null) return null
    return voice
  }

  /** Starts, stops or changes the notification voice as the settings say. */
  private fun updateVoice(prefs: SharedPreferences) {
    val context = context ?: return
    val id = prefs.getString(NotificationSettings.Companion.Keys.VOICE_PROFILE, null).orEmpty()
    if (id.isEmpty() || id !in VoiceProfiles.ids(prefs)) {
      voice?.shutdown()
      voice = null
      return
    }
    val profile = VoiceProfiles.read(prefs, id)
    val current = voice ?: NotificationVoice(context).also { voice = it }
    if (current.currentProfile != profile || current.failed) current.use(profile)
  }

  /**
   * Runs [then] once SandFly isn't speaking, so another voice doesn't talk over it, or after
   * [QUIET_WAIT_MS] at most.
   */
  private fun whenSandFlyIsQuiet(since: Long, then: () -> Unit) {
    if (!isSandFlySpeaking() || SystemClock.uptimeMillis() - since >= QUIET_WAIT_MS) {
      then()
      return
    }
    handler.postAtTime({ whenSandFlyIsQuiet(since, then) }, QUIET_TOKEN, SystemClock.uptimeMillis() + QUIET_CHECK_MS)
  }

  /** The notification settings, read again only after they change. */
  private fun settings(context: Context): NotificationSettings =
    cachedSettings
      ?: NotificationSettings.read(SharedPreferencesUtils.getSharedPreferences(context)).also { cachedSettings = it }

  /** The language a detected language marked [text] with, or null. */
  private fun localeOf(text: CharSequence): Locale? =
    (text as? Spanned)?.getSpans(0, text.length, LocaleSpan::class.java)?.firstOrNull()?.locale

  private fun cancel(readout: Readout) {
    readout.cancelled = true
    handler.removeCallbacks(readout.speak)
    waiting.remove(readout)
  }

  private fun cancelAll() {
    waiting.toList().forEach { cancel(it) }
    queued.forEach { it.cancelled = true }
    queued.clear()
    handler.removeCallbacksAndMessages(QUIET_TOKEN)
    handler.removeCallbacksAndMessages(DND_TOKEN)
    heldForTouch.clear()
    handler.removeCallbacks(releaseHeld)
    reading?.cancelled = true
    reading = null
    repeating.clear()
    handler.removeCallbacks(repeat)
    sensors?.stop()
    handler.removeCallbacks(stopAfterLimit)
    abandonFocus()
  }

  /** Marks the text with the language it is written in, so it is said in that language. */
  private fun detectLanguage(context: Context, readout: Readout, then: () -> Unit) {
    val identifier =
      languageIdentifier
        ?: try {
          LanguageIdentification.getClient(
              LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.6f).build()
            )
            .also { languageIdentifier = it }
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "No language identification: %s", e)
          null
        }
    if (identifier == null) {
      then()
      return
    }
    var done = false
    val finish = Runnable {
      if (!done) {
        done = true
        then()
      }
    }
    handler.postDelayed(finish, LANGUAGE_WAIT_MS)
    identifier
      .identifyLanguage(readout.text.toString())
      .addOnSuccessListener { tag ->
        if (!done && tag != "und") {
          val locale = Locale.forLanguageTag(tag)
          val current = context.resources.configuration.locales[0]
          if (locale.language != current.language) {
            readout.text =
              SpannableString(readout.text).apply {
                setSpan(LocaleSpan(locale), 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
              }
          }
        }
        handler.removeCallbacks(finish)
        finish.run()
      }
      .addOnFailureListener {
        handler.removeCallbacks(finish)
        finish.run()
      }
  }

  private fun requestFocus() {
    val context = context ?: return
    val audio = context.getSystemService(AudioManager::class.java) ?: return
    val request =
      AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .build()
    if (audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      focusRequest = request
    }
  }

  private fun abandonFocus() {
    val request = focusRequest ?: return
    focusRequest = null
    context?.getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request)
  }

  /** The name of [packageName]'s app, or the package name when it has none. */
  fun appName(context: Context, packageName: String): String {
    appNames.get(packageName)?.let { return it }
    val name =
      try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
      } catch (e: PackageManager.NameNotFoundException) {
        packageName
      }
    appNames.put(packageName, name)
    return name
  }

  /** Whether the screen is on. */
  private object ScreenState {
    fun isOn(context: Context): Boolean =
      context.getSystemService(android.os.PowerManager::class.java)?.isInteractive != false
  }
}
