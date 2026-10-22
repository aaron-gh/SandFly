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
package com.google.android.accessibility.talkback.handwriting

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Region
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.handwriting.HandwritingRecognizer.Result
import com.google.android.accessibility.talkback.selector.SelectorController
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Chooses a reading control by a letter written on the screen: the next reading control whose name
 * starts with the letter. While it waits for the letter, the whole screen takes the strokes, as the
 * circle menu takes touches. A tap closes it.
 */
class HandwritingCommand(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val actorState: ActorState,
  private val selector: SelectorController,
) {
  private val handler = Handler(Looper.getMainLooper())
  private val windowManager = service.getSystemService(WindowManager::class.java)
  private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
  private var recognizer: HandwritingRecognizer? = null
  private var view: View? = null
  private var passthroughRegionSet = false
  private val strokes = mutableListOf<MutableList<InkPoint>>()
  /** Whether the strokes so far move further than a tap. */
  private var moved = false
  private var downX = 0f
  private var downY = 0f
  private val recognizeNow = Runnable { recognize() }
  private val closeIdle = Runnable { cancel() }

  val isShowing: Boolean
    get() = view != null

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
    if (addView() == null) return
    strokes.clear()
    moved = false
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(service.getString(R.string.handwriting_prompt), speakOptions(SpeechController.QUEUE_MODE_FLUSH_ALL))
        .sound(R.raw.chime_up),
    )
    restartIdleTimer()
    recognizer.prepare { result ->
      if (isShowing && result != null) notReady(result)
    }
  }

  /** Closes the writing surface without choosing a reading control. */
  fun cancel() {
    if (!isShowing) return
    dismiss()
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(service.getString(R.string.handwriting_closed), speakOptions(SpeechController.QUEUE_MODE_INTERRUPT))
        .sound(R.raw.chime_down),
    )
  }

  /** Closes the writing surface without any feedback, such as when SandFly stops. */
  fun dismiss() {
    handler.removeCallbacks(recognizeNow)
    handler.removeCallbacks(closeIdle)
    strokes.clear()
    val current = view ?: return
    view = null
    clearPassthroughRegion()
    try {
      windowManager.removeViewImmediate(current)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Handwriting surface was not attached: %s", e)
    }
  }

  fun shutdown() {
    dismiss()
    recognizer?.close()
    recognizer = null
  }

  private fun onTouch(event: MotionEvent) {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        handler.removeCallbacks(recognizeNow)
        handler.removeCallbacks(closeIdle)
        if (strokes.size >= MAX_STROKES) strokes.clear()
        downX = event.x
        downY = event.y
        strokes += mutableListOf(InkPoint(event.x, event.y, event.eventTime))
      }
      MotionEvent.ACTION_MOVE -> {
        val stroke = strokes.lastOrNull() ?: return
        for (i in 0 until event.historySize) {
          addPoint(stroke, event.getHistoricalX(i), event.getHistoricalY(i), event.getHistoricalEventTime(i))
        }
        addPoint(stroke, event.x, event.y, event.eventTime)
        if (Math.hypot((event.x - downX).toDouble(), (event.y - downY).toDouble()) > touchSlop) {
          moved = true
        }
      }
      MotionEvent.ACTION_UP -> {
        if (!moved) {
          // A tap, rather than writing.
          cancel()
          return
        }
        // A letter can have more strokes, so wait a moment for the next one.
        handler.postDelayed(recognizeNow, HandwritingSettings.waitMs(service))
      }
      MotionEvent.ACTION_CANCEL -> cancel()
    }
  }

  private fun addPoint(stroke: MutableList<InkPoint>, x: Float, y: Float, time: Long) {
    if (stroke.size < MAX_POINTS) stroke += InkPoint(x, y, time)
  }

  private fun recognize() {
    val recognizer = recognizer ?: return
    val current = view ?: return
    val written = strokes.map { it.toList() }
    strokes.clear()
    moved = false
    recognizer.recognize(written, current.width.toFloat(), current.height.toFloat()) { result ->
      if (!isShowing) return@recognize
      when (result) {
        is Result.Candidates -> choose(result.texts)
        Result.NotDownloaded,
        Result.Downloading,
        Result.UnsupportedLanguage -> notReady(result)
        Result.Failed -> retry(service.getString(R.string.handwriting_not_recognized))
      }
    }
  }

  /** Chooses the reading control for the most likely letter that one starts with. */
  private fun choose(candidates: List<String>) {
    val letters =
      candidates.mapNotNull { text -> text.trim().firstOrNull { it.isLetterOrDigit() }?.toString() }.distinct()
    if (letters.isEmpty()) {
      retry(service.getString(R.string.handwriting_not_recognized))
      return
    }
    for (letter in letters) {
      if (selector.selectSettingStartingWith(EVENT_ID_UNTRACKED, letter) != null) {
        LogUtils.d(TAG, "Wrote %s, candidates %s", letter, candidates)
        dismiss()
        return
      }
    }
    retry(service.getString(R.string.handwriting_no_match, letters.first().uppercase()))
  }

  /** Says [message] and waits for another letter. */
  private fun retry(message: String) {
    speak(message)
    restartIdleTimer()
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

  @SuppressLint("ClickableViewAccessibility")
  private fun addView(): View? {
    val surface =
      object : View(service as Context) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
          onTouch(event)
          return true
        }
      }
    surface.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    val params =
      WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags =
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        title = service.getString(R.string.shortcut_write_reading_control)
      }
    try {
      windowManager.addView(surface, params)
    } catch (e: RuntimeException) {
      LogUtils.e(TAG, "Could not show the handwriting surface: %s", e)
      return null
    }
    view = surface
    setPassthroughRegion()
    return surface
  }

  /** Sends touches on the whole screen straight to the surface, instead of through touch exploration. */
  private fun setPassthroughRegion() {
    // Pass-through mode and the braille keyboard use the same region, so leave it alone for them.
    if (
      Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
        actorState.passThroughModeState.isPassThroughModeActive
    ) {
      return
    }
    val display =
      service.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION") display.getRealMetrics(metrics)
    service.setTouchExplorationPassthroughRegion(
      Display.DEFAULT_DISPLAY,
      Region(0, 0, metrics.widthPixels, metrics.heightPixels),
    )
    passthroughRegionSet = true
  }

  private fun clearPassthroughRegion() {
    if (passthroughRegionSet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      passthroughRegionSet = false
      service.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, Region())
    }
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

  private companion object {
    const val TAG = "HandwritingCommand"
    /** The surface closes after this long without writing. */
    const val IDLE_CLOSE_MS = 10_000L
    const val MAX_STROKES = 10
    const val MAX_POINTS = 2_000
  }
}
