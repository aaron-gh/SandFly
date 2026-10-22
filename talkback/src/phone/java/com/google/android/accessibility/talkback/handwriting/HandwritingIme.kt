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

import android.annotation.SuppressLint
import android.graphics.Region
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.handwriting.HandwritingRecognizer.Result
import com.google.android.accessibility.talkback.keyboardecho.KeyboardEchoSettings
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.input.TextEventFilter
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import kotlin.math.abs
import kotlin.math.hypot

/**
 * A keyboard that types what is written on the screen with one finger, recognized on the device.
 * It fills the screen, and while SandFly is on, touches on it go straight to it instead of through
 * touch exploration. Two- and three-finger gestures type a space, delete, start a new line, choose
 * another reading of the last writing, read the text, and close or switch the keyboard.
 */
class HandwritingIme : InputMethodService() {
  private val handler = Handler(Looper.getMainLooper())
  private var recognizer: HandwritingRecognizer? = null
  private var surface: View? = null
  private val strokes = mutableListOf<MutableList<InkPoint>>()
  private val recognizeNow = Runnable { recognize() }

  // The touch now: how many fingers it had at most, and where and when its first finger went down.
  private var maxPointers = 0
  private var downX = 0f
  private var downY = 0f
  private var downTime = 0L
  private var lastX = 0f
  private var lastY = 0f

  /** Where the cursor is, from the app's selection updates. */
  private var cursor = -1

  /** The word typed so far, for word echo. */
  private val word = StringBuilder()

  /** The last writing typed, with the other readings of it, while the cursor is still after it. */
  private var lastWriting: Writing? = null

  private class Writing(val candidates: List<String>, var index: Int, var cursor: Int)

  private val swipeDistance by lazy { resources.displayMetrics.ydpi / CM_PER_INCH }
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(this) }
  private val tutorial by lazy {
    HandwritingTutorial(resources, ::speak) { prefs.edit().putBoolean(PREF_TUTORIAL_DONE, true).apply() }
  }

  override fun onCreate() {
    super.onCreate()
    recognizer = HandwritingRecognizers.create(this) { HandwritingDownload.onDone(this, it) }
  }

  override fun onDestroy() {
    HandwritingKeyboardHost.isTyping = false
    tutorial.cancel()
    handler.removeCallbacksAndMessages(null)
    recognizer?.close()
    recognizer = null
    super.onDestroy()
  }

  // The input view fills the screen, as the braille keyboard's does, so there's no extract area.
  override fun onEvaluateFullscreenMode(): Boolean = false

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreateInputView(): View {
    val view =
      object : View(this) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
          onTouch(event)
          return true
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
          val metrics = resources.displayMetrics
          setMeasuredDimension(metrics.widthPixels, metrics.heightPixels)
        }
      }
    view.contentDescription = getString(R.string.handwriting_keyboard_name)
    surface = view
    return view
  }

  override fun onComputeInsets(outInsets: Insets) {
    super.onComputeInsets(outInsets)
    // Leaves the app its whole height to lay out in, as the braille keyboard does.
    outInsets.contentTopInsets = resources.displayMetrics.heightPixels
  }

  override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
    super.onStartInputView(info, restarting)
    strokes.clear()
    lastWriting = null
    word.setLength(0)
    cursor = if (info.initialSelStart == info.initialSelEnd) info.initialSelEnd else -1
    val metrics = resources.displayMetrics
    HandwritingKeyboardHost.feedback?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, metrics.widthPixels, metrics.heightPixels)),
    )
    HandwritingKeyboardHost.isTyping = true
    // Speaks only once the model is known to be there, since without it the keyboard hides and
    // asks to download it.
    recognizer?.prepare { result ->
      when {
        result != null -> notReady(result)
        restarting -> {}
        !prefs.getBoolean(PREF_TUTORIAL_DONE, false) -> tutorial.start()
        else -> speak(getString(R.string.handwriting_keyboard_name))
      }
    }
  }

  override fun onFinishInputView(finishingInput: Boolean) {
    HandwritingKeyboardHost.isTyping = false
    tutorial.cancel()
    handler.removeCallbacks(recognizeNow)
    strokes.clear()
    lastWriting = null
    HandwritingKeyboardHost.feedback?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, /* region= */ null),
    )
    super.onFinishInputView(finishingInput)
  }

  override fun onUpdateSelection(
    oldSelStart: Int,
    oldSelEnd: Int,
    newSelStart: Int,
    newSelEnd: Int,
    candidatesStart: Int,
    candidatesEnd: Int,
  ) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    cursor = if (newSelStart == newSelEnd) newSelEnd else -1
    // Another reading can only replace the last writing while the cursor is right after it.
    val writing = lastWriting ?: return
    if (cursor < 0 || cursor != writing.cursor) lastWriting = null
  }

  private fun onTouch(event: MotionEvent) {
    if (event.actionMasked != MotionEvent.ACTION_MOVE) {
      LogUtils.v(TAG, "Touch %s, %d pointers", MotionEvent.actionToString(event.actionMasked), event.pointerCount)
    }
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        handler.removeCallbacks(recognizeNow)
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
        if (maxPointers > 1) {
          onGesture(maxPointers, lastX - downX, lastY - downY, event.eventTime - downTime)
          return
        }
        // A tap is a stroke too: a dot, such as on an i, or a full stop.
        if (strokes.isNotEmpty()) handler.postDelayed(recognizeNow, HandwritingSettings.waitMs(this))
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

  /** A gesture with [fingers] fingers that moved [dx], [dy] pixels in [duration] milliseconds. */
  private fun onGesture(fingers: Int, dx: Float, dy: Float, duration: Long) {
    // Writing that waits to be recognized goes first, so that a space comes after it.
    if (strokes.isNotEmpty()) {
      handler.removeCallbacks(recognizeNow)
      recognize(then = { onGesture(fingers, dx, dy, duration) })
      return
    }
    val distance = hypot(dx, dy)
    val tap = distance < swipeDistance / 2
    if (tap && duration >= TAP_MS) return
    if (tap && fingers == 3) {
      tutorial.start()
      return
    }
    val horizontal = abs(dx) > abs(dy)
    if (tutorial.isActive) {
      when {
        fingers == 3 && !horizontal && dy > 0 -> tutorial.finish()
        fingers != 2 -> {}
        tap -> tutorial.onGesture(HandwritingTutorial.Gesture.READ_BACK)
        horizontal && dx > 0 -> tutorial.onGesture(HandwritingTutorial.Gesture.SPACE)
        horizontal -> tutorial.onGesture(HandwritingTutorial.Gesture.DELETE)
        dy > 0 -> tutorial.onGesture(HandwritingTutorial.Gesture.NEW_LINE)
        else -> tutorial.onGesture(HandwritingTutorial.Gesture.NEXT_READING)
      }
      return
    }
    if (tap) {
      if (fingers == 2) readBack()
      return
    }
    when (fingers) {
      2 ->
        when {
          horizontal && dx > 0 -> typeSpace()
          horizontal -> deleteBack()
          dy > 0 -> newLine()
          else -> nextReading()
        }
      3 ->
        when {
          // SandFly says that the keyboard is hidden.
          !horizontal && dy > 0 -> requestHideSelf(0)
          // Before Android 9, the system's keyboard switcher does it.
          Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> {}
          horizontal && dx > 0 -> switchToNextInputMethod(/* onlyCurrentIme= */ false)
          horizontal -> switchToPreviousInputMethod()
        }
    }
  }

  private fun recognize(then: (() -> Unit)? = null) {
    val recognizer = recognizer ?: return
    val view = surface ?: return
    val written = strokes.map { it.toList() }
    strokes.clear()
    if (written.isEmpty()) {
      then?.invoke()
      return
    }
    recognizer.recognize(written, view.width.toFloat(), view.height.toFloat(), textBefore(PRE_CONTEXT_LENGTH)) { result ->
      when (result) {
        is Result.Candidates -> {
          val candidates = result.candidates()
          when {
            candidates.isEmpty() -> speak(getString(R.string.handwriting_not_recognized))
            tutorial.isActive -> tutorial.onWriting(candidates)
            else -> {
              val before = cursor
              commit(candidates[0])
              word.append(candidates[0])
              lastWriting = if (before < 0) null else Writing(candidates, 0, before + candidates[0].length)
              if (KeyboardEchoSettings.echoesCharacters(echo())) speak(candidates[0])
            }
          }
        }
        Result.NotDownloaded,
        Result.Downloading,
        Result.UnsupportedLanguage -> notReady(result)
        Result.Failed -> speak(getString(R.string.handwriting_not_recognized))
      }
      then?.invoke()
    }
  }

  /**
   * The readings to offer, most likely first: those as long as the most likely one, so that a
   * letter is offered other letters, not words that start with it.
   */
  private fun Result.Candidates.candidates(): List<String> {
    val texts = texts.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val length = texts.firstOrNull()?.let { it.codePointCount(0, it.length) } ?: return emptyList()
    return texts.filter { it.codePointCount(0, it.length) == length }.take(MAX_CANDIDATES)
  }

  /** Replaces the last writing with its next reading, if the cursor is still after it. */
  private fun nextReading() {
    val writing = lastWriting
    if (writing == null || writing.candidates.size < 2 || cursor != writing.cursor) {
      speak(getString(R.string.handwriting_no_alternatives))
      return
    }
    val connection = currentInputConnection ?: return
    val old = writing.candidates[writing.index]
    writing.index = (writing.index + 1) % writing.candidates.size
    val new = writing.candidates[writing.index]
    connection.beginBatchEdit()
    connection.deleteSurroundingText(old.length, 0)
    connection.commitText(new, 1)
    connection.endBatchEdit()
    if (word.endsWith(old)) word.setLength(word.length - old.length)
    word.append(new)
    writing.cursor = writing.cursor - old.length + new.length
    speak(getString(R.string.handwriting_alternative, new, writing.index + 1, writing.candidates.size))
  }

  private fun commit(text: String) {
    currentInputConnection?.commitText(text, 1)
  }

  private fun typeSpace() {
    lastWriting = null
    commit(" ")
    endWord(getString(R.string.handwriting_space))
  }

  /**
   * Echoes the end of a word, as the keyboard echo for this keyboard says: the word, or what ended
   * it, [separator]. The app's field is hidden behind the keyboard, so SandFly can't echo it.
   */
  private fun endWord(separator: String) {
    val echo = echo()
    val ended = word.toString()
    word.setLength(0)
    val echoesWords =
      echo == TextEventFilter.PREF_ECHO_WORDS || echo == TextEventFilter.PREF_ECHO_CHARACTERS_AND_WORDS
    when {
      echoesWords && ended.isNotBlank() -> speak(ended)
      KeyboardEchoSettings.echoesCharacters(echo) -> speak(separator)
    }
  }

  private fun echo(): Int =
    KeyboardEchoSettings.read(prefs, resources, TextEventFilter.TypingInput.HANDWRITING_KEYBOARD)

  /** Deletes the character before the cursor. SandFly says what was deleted, as for any keyboard. */
  private fun deleteBack() {
    lastWriting = null
    val connection = currentInputConnection ?: return
    val selected = if (isOwnField()) null else connection.getSelectedText(0)
    val deleted =
      if (!selected.isNullOrEmpty()) {
        connection.commitText("", 1)
        selected.toString()
      } else {
        val before = textBefore(2)
        connection.deleteSurroundingTextInCodePoints(1, 0)
        before.takeLast(if (before.length == 2 && Character.isSurrogatePair(before[0], before[1])) 2 else 1)
      }
    if (word.isNotEmpty()) word.setLength(word.length - 1)
    // The app's field is hidden behind the keyboard, so SandFly can't say what was deleted.
    speak(
      when {
        deleted.isEmpty() -> getString(R.string.handwriting_tutorial_deleted)
        deleted == " " -> getString(R.string.template_text_removed, getString(R.string.handwriting_space))
        deleted == "\n" -> getString(R.string.template_text_removed, getString(R.string.handwriting_new_line))
        else -> getString(R.string.template_text_removed, deleted)
      }
    )
  }

  /** Types a new line, or performs the field's action, such as Send or Search, if it has one. */
  private fun newLine() {
    lastWriting = null
    val info = currentInputEditorInfo
    val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
    val multiLine = (info?.inputType ?: 0) and android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
    val noEnterAction = (info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
    if (!multiLine && !noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
      currentInputConnection?.performEditorAction(action)
      return
    }
    commit("\n")
    endWord(getString(R.string.handwriting_new_line))
  }

  /** Says the text before the cursor, up to a sentence or so. */
  private fun readBack() {
    val text = textBefore(READ_BACK_LENGTH)
    speak(text.ifBlank { getString(R.string.handwriting_empty) })
  }

  /**
   * Up to [length] characters before the cursor. SandFly's own fields answer on SandFly's main
   * thread, which this keyboard runs on, so it doesn't ask them.
   */
  private fun textBefore(length: Int): String {
    if (isOwnField()) return ""
    return currentInputConnection?.getTextBeforeCursor(length, 0)?.toString().orEmpty()
  }

  /**
   * Hides the keyboard when handwriting can't be recognized yet, and asks to download the model if
   * it isn't on the phone. The question shows over the screen once the keyboard is hidden, so
   * that SandFly's touch exploration reaches it.
   */
  private fun notReady(result: Result) {
    tutorial.cancel()
    requestHideSelf(0)
    val recognizer = recognizer ?: return
    when (result) {
      Result.NotDownloaded -> {
        val service = TalkBackService.getInstance()
        if (service != null) {
          handler.post { HandwritingDownload.ask(service, HandwritingKeyboardHost.feedback, recognizer) }
        } else {
          recognizer.download()
        }
      }
      Result.Downloading -> speak(getString(R.string.handwriting_downloading))
      else -> speak(getString(R.string.handwriting_unsupported_language))
    }
  }

  private fun isOwnField(): Boolean = currentInputEditorInfo?.packageName == packageName

  private fun speak(text: CharSequence) {
    HandwritingKeyboardHost.feedback?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
          .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
  }

  private companion object {
    const val TAG = "HandwritingIme"
    const val PREF_TUTORIAL_DONE = "pref_handwriting_tutorial_done"
    const val CM_PER_INCH = 2.54f
    const val TAP_MS = 300L
    const val MAX_STROKES = 40
    const val MAX_POINTS = 2_000
    const val MAX_CANDIDATES = 8
    const val PRE_CONTEXT_LENGTH = 20
    const val READ_BACK_LENGTH = 200
  }
}
