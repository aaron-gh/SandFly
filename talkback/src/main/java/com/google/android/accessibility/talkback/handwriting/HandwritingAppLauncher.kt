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
package com.google.android.accessibility.talkback.handwriting

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.handwriting.HandwritingRecognizer.Result
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Opens an app by the start of its name, written on the screen. Each piece of writing adds to the
 * letters, and SandFly says the first app whose name, or a word of it, starts with them. When only
 * one app does, it opens. Two-finger gestures work as on the handwriting keyboard: swipe up for the
 * next app, down to open it, left to delete the last letter, and tap to hear the app again. A tap
 * with one finger closes the surface.
 */
class HandwritingAppLauncher(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  actorState: ActorState,
) {
  private val handler = Handler(Looper.getMainLooper())
  private val overlay =
    HandwritingOverlay(
      service,
      pipeline,
      actorState,
      service.getString(R.string.shortcut_write_app_name),
      ::onTouch,
    )
  private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
  private val swipeDistance by lazy { service.resources.displayMetrics.ydpi / CM_PER_INCH }
  private var recognizer: HandwritingRecognizer? = null
  private val strokes = mutableListOf<MutableList<InkPoint>>()
  private val recognizeNow = Runnable { recognize() }
  private val closeIdle = Runnable { cancel() }

  // The touch now: how many fingers it had at most, and where and when its first finger went down.
  private var maxPointers = 0
  private var downX = 0f
  private var downY = 0f
  private var downTime = 0L
  private var lastX = 0f
  private var lastY = 0f

  /** The apps on the home screen, read when the surface shows. */
  private var apps: List<OpenAppActions.App> = emptyList()
  /**
   * Whether [apps] is loaded. Loading reads every launcher app's label, which takes up to seconds
   * and is done off the main thread; letters written before then wait in [waitingLetters].
   */
  private var appsLoaded = false
  private var waitingLetters: List<String>? = null
  private var loads = 0
  private val loader =
    ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()) {
      Thread(it, "handwriting-apps").apply { isDaemon = true }
    }

  /** The letters written so far, the apps that match them, and the one SandFly said last. */
  private var letters = ""
  private var matches: List<OpenAppActions.App> = emptyList()
  private var index = 0

  val isShowing: Boolean
    get() = overlay.isShowing

  /** Shows the writing surface, or says why it can't. */
  fun show() {
    if (isShowing) return
    // Strokes reach the surface only where touches can pass touch exploration by, from Android 11.
    val recognizer =
      if (!FeatureSupport.supportPassthrough()) null
      else
        recognizer
          ?: HandwritingRecognizers.create(service) { HandwritingDownload.onDone(service, it) }
            ?.also { recognizer = it }
    if (recognizer == null) {
      speak(service.getString(R.string.handwriting_not_available))
      return
    }
    if (!overlay.show()) return
    strokes.clear()
    maxPointers = 0
    letters = ""
    matches = emptyList()
    index = 0
    loadApps()
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
          service.getString(R.string.handwriting_app_prompt),
          speakOptions(SpeechController.QUEUE_MODE_FLUSH_ALL),
        )
        .sound(R.raw.chime_up),
    )
    restartIdleTimer()
    recognizer.prepare { result ->
      if (isShowing && result != null) notReady(result)
    }
  }

  /** Closes the writing surface without opening an app. */
  fun cancel() {
    if (!isShowing) return
    dismiss()
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
          service.getString(R.string.handwriting_closed),
          speakOptions(SpeechController.QUEUE_MODE_INTERRUPT),
        )
        .sound(R.raw.chime_down),
    )
  }

  /** Closes the writing surface without any feedback, such as when SandFly stops. */
  fun dismiss() {
    handler.removeCallbacks(recognizeNow)
    handler.removeCallbacks(closeIdle)
    strokes.clear()
    loads++
    appsLoaded = false
    waitingLetters = null
    apps = emptyList()
    matches = emptyList()
    overlay.remove()
  }

  fun shutdown() {
    dismiss()
    recognizer?.close()
    recognizer = null
    loader.shutdown()
  }

  private fun loadApps() {
    val load = ++loads
    appsLoaded = false
    loader.execute {
      val list =
        try {
          OpenAppActions.apps(service)
        } catch (e: Throwable) {
          LogUtils.e(TAG, "Could not list the apps: %s", e)
          emptyList()
        }
      handler.post {
        if (load != loads) return@post
        apps = list
        appsLoaded = true
        waitingLetters?.let {
          waitingLetters = null
          addLetters(it)
        }
      }
    }
  }

  private fun onTouch(event: MotionEvent) {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        handler.removeCallbacks(recognizeNow)
        handler.removeCallbacks(closeIdle)
        maxPointers = 1
        downX = event.x
        downY = event.y
        lastX = event.x
        lastY = event.y
        downTime = event.eventTime
        if (strokes.size >= MAX_STROKES) strokes.clear()
        strokes += mutableListOf(InkPoint(event.x, event.y, event.eventTime))
      }
      MotionEvent.ACTION_POINTER_DOWN -> {
        if (maxPointers == 1) {
          // A gesture, not writing: the first finger's line so far isn't part of a letter.
          strokes.removeLastOrNull()
        }
        maxPointers = maxOf(maxPointers, event.pointerCount)
      }
      MotionEvent.ACTION_MOVE -> {
        lastX = event.getX(0)
        lastY = event.getY(0)
        if (maxPointers > 1) return
        val stroke = strokes.lastOrNull() ?: return
        for (i in 0 until event.historySize) {
          addPoint(stroke, event.getHistoricalX(i), event.getHistoricalY(i), event.getHistoricalEventTime(i))
        }
        addPoint(stroke, event.x, event.y, event.eventTime)
      }
      MotionEvent.ACTION_UP -> {
        restartIdleTimer()
        if (maxPointers > 1) {
          onGesture(maxPointers, lastX - downX, lastY - downY, event.eventTime - downTime)
          return
        }
        // A tap closes the surface, unless it is a dot of writing that waits, such as on an i.
        if (strokes.size == 1 && hypot(lastX - downX, lastY - downY) <= touchSlop) {
          cancel()
          return
        }
        // A letter can have more strokes, so wait a moment for the next one.
        handler.postDelayed(recognizeNow, HandwritingSettings.waitMs(service))
      }
      MotionEvent.ACTION_CANCEL -> {
        strokes.clear()
        maxPointers = 0
      }
    }
  }

  private fun addPoint(stroke: MutableList<InkPoint>, x: Float, y: Float, time: Long) {
    if (stroke.size < MAX_POINTS) stroke += InkPoint(x, y, time)
  }

  private fun isTap(dx: Float, dy: Float): Boolean = hypot(dx, dy) < swipeDistance / 2

  /** A gesture with [fingers] fingers that moved [dx], [dy] pixels in [duration] milliseconds. */
  private fun onGesture(fingers: Int, dx: Float, dy: Float, duration: Long) {
    // Writing that waits to be recognized goes first, so that the gesture acts on its letters.
    if (strokes.isNotEmpty()) {
      handler.removeCallbacks(recognizeNow)
      recognize(then = { onGesture(fingers, dx, dy, duration) })
      return
    }
    if (fingers != 2 || !isShowing) return
    if (isTap(dx, dy)) {
      if (duration < TAP_MS) sayApp()
      return
    }
    when {
      abs(dx) > abs(dy) -> if (dx < 0) deleteLetter()
      dy > 0 -> openApp()
      else -> nextApp()
    }
  }

  private fun recognize(then: (() -> Unit)? = null) {
    val recognizer = recognizer ?: return
    if (!isShowing) return
    val written = strokes.map { it.toList() }
    strokes.clear()
    if (written.isEmpty()) {
      then?.invoke()
      return
    }
    recognizer.recognize(written, overlay.width, overlay.height, letters) { result ->
      if (!isShowing) return@recognize
      when (result) {
        is Result.Candidates -> addLetters(result.texts)
        Result.NotDownloaded,
        Result.Downloading,
        Result.UnsupportedLanguage -> notReady(result)
        Result.Failed -> speak(service.getString(R.string.handwriting_not_recognized))
      }
      if (isShowing) then?.invoke()
    }
  }

  /**
   * Adds the most likely of [candidates] that some app's name starts with to the letters. When only
   * one app's does, opens it.
   */
  private fun addLetters(candidates: List<String>) {
    if (!appsLoaded) {
      waitingLetters = candidates
      return
    }
    // Only letters and digits count, so a stray dot or space doesn't add to the letters.
    val readings = candidates.map { it.filter(Char::isLetterOrDigit) }.filter { it.isNotEmpty() }.distinct()
    if (readings.isEmpty()) {
      speak(service.getString(R.string.handwriting_not_recognized))
      return
    }
    for (reading in readings) {
      val found = matching(apps, letters + reading)
      if (found.isEmpty()) continue
      LogUtils.d(TAG, "Wrote %s, candidates %s, %d apps", reading, candidates, found.size)
      letters += reading
      matches = found
      index = 0
      if (found.size == 1) openApp() else sayApp()
      return
    }
    speak(service.getString(R.string.handwriting_no_app, (letters + readings.first()).uppercase()))
  }

  private fun nextApp() {
    if (matches.size < 2) {
      speak(service.getString(R.string.handwriting_no_alternatives))
      return
    }
    index = (index + 1) % matches.size
    sayApp()
  }

  private fun deleteLetter() {
    if (letters.isEmpty()) {
      speak(service.getString(R.string.handwriting_empty))
      return
    }
    val deleted = letters.last().toString()
    letters = letters.dropLast(1)
    matches = if (letters.isEmpty()) emptyList() else matching(apps, letters)
    index = 0
    speak(service.getString(R.string.template_text_removed, deleted.uppercase()))
  }

  /** Says the app that a swipe down opens, or that there is none yet. */
  private fun sayApp() {
    val app = matches.getOrNull(index)
    when {
      app != null -> speak(service.getString(R.string.handwriting_alternative, app.label, index + 1, matches.size))
      letters.isEmpty() -> speak(service.getString(R.string.handwriting_app_prompt))
      else -> speak(service.getString(R.string.handwriting_no_app, letters.uppercase()))
    }
  }

  private fun openApp() {
    val app = matches.getOrNull(index)
    if (app == null) {
      sayApp()
      return
    }
    dismiss()
    // The app's window says its name when it opens.
    if (!OpenAppActions.open(service, app.value)) {
      speak(service.getString(R.string.handwriting_app_not_opened, app.label))
    }
  }

  /**
   * Closes the surface when handwriting can't be recognized yet, and asks to download the model
   * if it isn't on the phone.
   */
  private fun notReady(result: Result) {
    dismiss()
    val recognizer = recognizer ?: return
    when (result) {
      Result.NotDownloaded -> HandwritingDownload.ask(service, pipeline, recognizer)
      Result.Downloading -> speak(service.getString(R.string.handwriting_downloading))
      else -> speak(service.getString(R.string.handwriting_unsupported_language))
    }
  }

  private fun restartIdleTimer() {
    handler.removeCallbacks(closeIdle)
    handler.postDelayed(closeIdle, IDLE_CLOSE_MS)
  }

  private fun speak(text: String) {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(text, speakOptions(SpeechController.QUEUE_MODE_INTERRUPT)),
    )
  }

  private fun speakOptions(queueMode: Int): SpeakOptions =
    SpeakOptions.create()
      .setQueueMode(queueMode)
      .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)

  internal companion object {
    private const val TAG = "HandwritingAppLauncher"
    /** The surface closes after this long without a touch. */
    private const val IDLE_CLOSE_MS = 10_000L
    private const val CM_PER_INCH = 2.54f
    private const val TAP_MS = 300L
    private const val MAX_STROKES = 40
    private const val MAX_POINTS = 2_000

    /**
     * The apps whose name starts with [letters], and then those with a later word that does, such as
     * Google Maps for "ma". Case, spaces and accents don't count.
     */
    fun matching(apps: List<OpenAppActions.App>, letters: String): List<OpenAppActions.App> {
      val key = normalize(letters)
      if (key.isEmpty()) return emptyList()
      val (starts, others) = apps.partition { normalize(it.label).startsWith(key) }
      val words =
        others.filter { app ->
          app.label.split(' ', '-', '_', '.').drop(1).any { normalize(it).startsWith(key) }
        }
      return starts + words
    }

    private fun normalize(text: String): String =
      java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        .filter { it.isLetterOrDigit() }
        .lowercase()
  }
}
