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
package com.google.android.accessibility.talkback.review

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut as Key
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechCleanupUtils
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.vision.common.InputImage

/**
 * Reviews the text on screen by line, word and character, as it is laid out, without moving SandFly
 * focus: the text of the window's items, or text recognized in a screenshot. Clicks at the review
 * cursor click the item there, or tap the screen there.
 *
 * Review commands start where SandFly focus is, unless the review cursor is on. While it is on, and
 * while recognized text is shown, arrow keys, Home, End, Page Up, Page Down and Enter move and click
 * the review cursor instead of reaching the app.
 */
class ScreenReview(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val focusMonitor: AccessibilityFocusMonitor,
) {
  private val handler = Handler(Looper.getMainLooper())
  private var recognizer: LanguageTextRecognizer? = null

  private var cursor: ReviewCursor? = null

  /** Whether [cursor] reviews recognized text rather than the window's items. */
  private var recognized = false

  /** The app whose window was active when its text was recognized. */
  private var recognizedPackage: CharSequence? = null

  /** Whether the window changed since [cursor]'s text was read from it. */
  private var stale = true

  /** When [cursor]'s text was read from the window, in uptime milliseconds. */
  private var readAt = 0L

  /**
   * Whether the next review command starts where SandFly focus is: after focus moves while the
   * review cursor is off, and when it's turned on.
   */
  private var startAtFocus = true

  /** Whether arrow keys move the review cursor. */
  var isCursorOn = false
    private set

  /** Keys whose down event the review cursor took, so their up event is taken too. */
  private val takenKeys = HashSet<Int>()

  private var tapping = false

  fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
        if (!tapping && !isCursorOn) startAtFocus = true
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
        stale = true
        // Recognized text belongs to the app it was recognized in, which a keyboard or a dialog
        // of its own doesn't leave.
        if (
          recognized &&
            event.packageName != service.packageName &&
            service.rootInActiveWindow?.packageName?.let { it != recognizedPackage } == true
        ) {
          closeRecognizedText(announce = true)
        }
      }
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
      AccessibilityEvent.TYPE_WINDOWS_CHANGED,
      AccessibilityEvent.TYPE_VIEW_SCROLLED -> stale = true
    }
  }

  /** Runs a screen review keyboard action. Returns false for actions it doesn't have. */
  fun perform(action: TalkBackPhysicalKeyboardShortcut): Boolean {
    when (action) {
      Key.REVIEW_PREVIOUS_LINE -> move(Spoken.LINE, forward = false) { it.moveLine(-1) }
      Key.REVIEW_CURRENT_LINE -> speakCurrent(Spoken.LINE)
      Key.REVIEW_NEXT_LINE -> move(Spoken.LINE, forward = true) { it.moveLine(1) }
      Key.REVIEW_PREVIOUS_WORD -> move(Spoken.WORD, forward = false) { it.moveWord(forward = false) }
      Key.REVIEW_CURRENT_WORD -> speakCurrent(Spoken.WORD)
      Key.REVIEW_NEXT_WORD -> move(Spoken.WORD, forward = true) { it.moveWord(forward = true) }
      Key.REVIEW_PREVIOUS_CHARACTER ->
        move(Spoken.CHARACTER, forward = false) { it.moveCharacter(forward = false) }
      Key.REVIEW_CURRENT_CHARACTER -> speakCurrent(Spoken.CHARACTER)
      Key.REVIEW_NEXT_CHARACTER ->
        move(Spoken.CHARACTER, forward = true) { it.moveCharacter(forward = true) }
      Key.REVIEW_TOP -> move(Spoken.LINE, forward = false) { it.moveToTop() }
      Key.REVIEW_BOTTOM -> move(Spoken.LINE, forward = true) { it.moveToBottom() }
      Key.REVIEW_LINE_START ->
        move(Spoken.CHARACTER, forward = false) {
          it.moveToLineStart()
          true
        }
      Key.REVIEW_LINE_END ->
        move(Spoken.CHARACTER, forward = true) {
          it.moveToLineEnd()
          true
        }
      Key.REVIEW_CLICK -> click(long = false)
      Key.REVIEW_LONG_CLICK -> click(long = true)
      Key.REVIEW_MOVE_TO_FOCUS -> {
        if (recognized) closeRecognizedText(announce = false)
        startAtFocus = true
        speakCurrent(Spoken.LINE)
      }
      Key.FOCUS_REVIEW_CURSOR -> focusReviewCursor()
      Key.REVIEW_CURSOR_ON -> setCursorOn(true)
      Key.REVIEW_CURSOR_OFF -> setCursorOn(false)
      Key.RECOGNIZE_SCREEN_TEXT -> recognizeScreenText()
      else -> return false
    }
    return true
  }

  /**
   * Moves or clicks the review cursor for a key pressed while it is on. Returns true when it took
   * the key.
   */
  fun onKeyEvent(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_UP) {
      return takenKeys.remove(event.keyCode)
    }
    if (event.action != KeyEvent.ACTION_DOWN || !isCursorOn) {
      return false
    }
    val modifiers =
      event.metaState and
        (KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_META_ON)
    val action =
      when (modifiers) {
        0 ->
          when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> Key.REVIEW_PREVIOUS_LINE
            KeyEvent.KEYCODE_DPAD_DOWN -> Key.REVIEW_NEXT_LINE
            KeyEvent.KEYCODE_DPAD_LEFT -> Key.REVIEW_PREVIOUS_CHARACTER
            KeyEvent.KEYCODE_DPAD_RIGHT -> Key.REVIEW_NEXT_CHARACTER
            KeyEvent.KEYCODE_MOVE_HOME -> Key.REVIEW_LINE_START
            KeyEvent.KEYCODE_MOVE_END -> Key.REVIEW_LINE_END
            KeyEvent.KEYCODE_PAGE_UP -> Key.REVIEW_TOP
            KeyEvent.KEYCODE_PAGE_DOWN -> Key.REVIEW_BOTTOM
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> Key.REVIEW_CLICK
            KeyEvent.KEYCODE_ESCAPE -> if (recognized) Key.REVIEW_CURSOR_OFF else null
            else -> null
          }
        KeyEvent.META_CTRL_ON ->
          when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> Key.REVIEW_PREVIOUS_WORD
            KeyEvent.KEYCODE_DPAD_RIGHT -> Key.REVIEW_NEXT_WORD
            KeyEvent.KEYCODE_MOVE_HOME -> Key.REVIEW_TOP
            KeyEvent.KEYCODE_MOVE_END -> Key.REVIEW_BOTTOM
            else -> null
          }
        KeyEvent.META_SHIFT_ON ->
          when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> Key.REVIEW_LONG_CLICK
            else -> null
          }
        else -> null
      } ?: return false
    takenKeys.add(event.keyCode)
    perform(action)
    return true
  }

  /** Forgets keys whose up event may not come, such as when the keyboard is disconnected. */
  fun resetKeys() {
    takenKeys.clear()
  }

  fun shutdown() {
    handler.removeCallbacksAndMessages(null)
    if (tapping) {
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
      tapping = false
    }
    recognizer?.close()
    recognizer = null
  }

  private enum class Spoken {
    LINE,
    WORD,
    CHARACTER,
  }

  /** Runs [movement] on the current cursor and speaks what it lands on, or Top or Bottom. */
  private fun move(spoken: Spoken, forward: Boolean, movement: (ReviewCursor) -> Boolean) {
    val cursor = currentCursor() ?: return
    if (!movement(cursor)) {
      speak(service.getString(if (forward) R.string.review_bottom else R.string.review_top))
      return
    }
    speak(cursor, spoken)
  }

  private fun speakCurrent(spoken: Spoken) {
    val cursor = currentCursor() ?: return
    speak(cursor, spoken)
  }

  private fun speak(cursor: ReviewCursor, spoken: Spoken) {
    val text =
      when (spoken) {
        Spoken.LINE -> cursor.currentLine?.text
        Spoken.WORD -> cursor.currentWord?.text
        Spoken.CHARACTER ->
          cursor.currentCharacter?.let { SpeechCleanupUtils.getCleanValueFor(service, it) }
      }
    speak(if (text.isNullOrBlank()) service.getString(R.string.review_blank) else text)
  }

  private fun speak(text: CharSequence) {
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, SPEAK_OPTIONS))
  }

  /**
   * The cursor to review with: on recognized text as left, otherwise on the window's text as it is
   * now, at SandFly focus when it starts there, or near where the cursor was. Says so and returns
   * null when there is no text.
   */
  private fun currentCursor(): ReviewCursor? {
    val old = cursor
    if (recognized && old != null) {
      return old
    }
    // A window that keeps changing, such as one with a timer, is read again at most every
    // REREAD_MS, so that held arrow keys don't read it on every repeat.
    if (
      old != null &&
        !startAtFocus &&
        (!stale || SystemClock.uptimeMillis() - readAt < REREAD_MS)
    ) {
      return old
    }
    val text = ReviewText(screenItems())
    if (text.isEmpty) {
      cursor = null
      speak(service.getString(R.string.review_no_text))
      return null
    }
    val fresh = ReviewCursor(text)
    val word = old?.currentWord
    if (startAtFocus || old == null || word == null) {
      placeAtFocus(fresh)
    } else {
      fresh.moveNear(word.centerX, word.centerY)
      // Stay on the same character of the word.
      fresh.currentWord?.let { landed ->
        if (landed.text == word.text) {
          repeat(old.offset - word.start) { fresh.moveCharacter(forward = true) }
        }
      }
    }
    cursor = fresh
    stale = false
    readAt = SystemClock.uptimeMillis()
    startAtFocus = false
    return fresh
  }

  private fun placeAtFocus(cursor: ReviewCursor) {
    val focus = focus() ?: return
    // Words of one item share its node, so each node's ancestors are looked up once.
    val inFocus = HashMap<AccessibilityNodeInfoCompat, Boolean>()
    if (
      cursor.moveToSource {
        it is AccessibilityNodeInfoCompat && inFocus.getOrPut(it) { it == focus || hasAncestor(it, focus) }
      }
    ) {
      return
    }
    val bounds = Rect().also { focus.getBoundsInScreen(it) }
    cursor.moveNear(bounds.centerX(), bounds.centerY())
  }

  private fun hasAncestor(node: AccessibilityNodeInfoCompat, ancestor: AccessibilityNodeInfoCompat) =
    AccessibilityNodeInfoUtils.hasAncestor(node, ancestor)

  /** SandFly focus, or input focus when there is none. */
  private fun focus(): AccessibilityNodeInfoCompat? =
    focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false) ?: focusMonitor.inputFocus

  /** The window to review: the one with SandFly focus, or the active one. */
  private fun reviewRoot(): AccessibilityNodeInfoCompat? =
    AccessibilityNodeInfoUtils.getRoot(focus())
      ?: service.rootInActiveWindow?.let { AccessibilityNodeInfoCompat.wrap(it) }

  /** Text of the window's items that show on screen. */
  private fun screenItems(): List<ReviewItem> {
    val root = reviewRoot() ?: return emptyList()
    val windowBounds = Rect().also { root.getBoundsInScreen(it) }
    val items = mutableListOf<ReviewItem>()
    val stack = ArrayDeque<Pair<AccessibilityNodeInfoCompat, Int>>()
    stack.addLast(root to 0)
    var visited = 0
    while (stack.isNotEmpty() && visited < MAX_NODES) {
      val (node, depth) = stack.removeLast()
      visited++
      if (!node.isVisibleToUser) continue
      val bounds = Rect().also { node.getBoundsInScreen(it) }
      if (!bounds.intersect(windowBounds) || bounds.isEmpty) continue
      val text = node.text?.toString()?.takeIf { it.isNotBlank() }
      val label =
        text
          ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
          ?: node.hintText?.toString()?.takeIf { it.isNotBlank() && node.isEditable }
      if (label != null) {
        items.add(ReviewItem(label, bounds.left, bounds.top, bounds.right, bounds.bottom, node))
        // A described item is read as its description, as SandFly speaks it, not its contents.
        if (text == null && !node.contentDescription.isNullOrBlank()) continue
      }
      if (depth >= MAX_DEPTH) continue
      // Children go on the stack last first, so they come off in order.
      for (index in node.childCount - 1 downTo 0) {
        node.getChild(index)?.let { stack.addLast(it to depth + 1) }
      }
    }
    return items
  }

  /** Puts SandFly focus on the item at the review cursor. */
  private fun focusReviewCursor() {
    val cursor = currentCursor() ?: return
    val word = cursor.currentWord ?: return
    val source = word.item.source as? AccessibilityNodeInfoCompat ?: nodeAt(word.centerX, word.centerY)
    val target =
      source?.let {
        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(it, AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS)
      } ?: source
    if (target == null) {
      speak(service.getString(R.string.review_no_item))
      return
    }
    if (recognized) closeRecognizedText(announce = false)
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.focus(
          target,
          FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build(),
        )
        .setForceRefocus(true),
    )
  }

  /** The smallest item that should take focus at the point ([x], [y]) of the review window. */
  private fun nodeAt(x: Int, y: Int): AccessibilityNodeInfoCompat? {
    val root = reviewRoot() ?: return null
    var best: AccessibilityNodeInfoCompat? = null
    var bestArea = Long.MAX_VALUE
    val stack = ArrayDeque<AccessibilityNodeInfoCompat>().apply { addLast(root) }
    var visited = 0
    val bounds = Rect()
    while (stack.isNotEmpty() && visited < MAX_NODES) {
      val node = stack.removeLast()
      visited++
      node.getBoundsInScreen(bounds)
      if (!node.isVisibleToUser || !bounds.contains(x, y)) continue
      val area = bounds.width().toLong() * bounds.height()
      if (area < bestArea && AccessibilityNodeInfoUtils.shouldFocusNode(node)) {
        best = node
        bestArea = area
      }
      for (index in 0 until node.childCount) node.getChild(index)?.let { stack.addLast(it) }
    }
    return best
  }

  /** Clicks the item at the review cursor, or taps the screen there. */
  private fun click(long: Boolean) {
    val cursor = currentCursor() ?: return
    val word = cursor.currentWord ?: return
    val node = word.item.source as? AccessibilityNodeInfoCompat
    val action =
      if (long) AccessibilityNodeInfo.ACTION_LONG_CLICK else AccessibilityNodeInfo.ACTION_CLICK
    val target =
      node?.let {
        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
          it,
          object : Filter<AccessibilityNodeInfoCompat>() {
            override fun accept(candidate: AccessibilityNodeInfoCompat?): Boolean =
              candidate != null &&
                candidate.isEnabled &&
                (if (long) candidate.isLongClickable else candidate.isClickable)
          },
        )
      }
    stale = true
    if (recognized) closeRecognizedText(announce = false)
    if (target != null && target.performAction(action)) {
      return
    }
    tap(word.centerX, word.centerY, long)
  }

  /** Taps or holds the screen at ([x], [y]), passing the touch through to the app. */
  private fun tap(x: Int, y: Int, long: Boolean) {
    // Touches pass through to the app only from Android 11.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      speak(service.getString(R.string.review_cannot_click))
      return
    }
    val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
    val duration =
      if (long) ViewConfiguration.getLongPressTimeout() * 2L
      else ViewConfiguration.getTapTimeout().toLong()
    val gesture =
      GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        .build()
    val metrics = service.resources.displayMetrics
    tapping = true
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, metrics.widthPixels, metrics.heightPixels)),
    )
    val ended =
      object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) = afterTap()

        override fun onCancelled(description: GestureDescription) = afterTap()
      }
    // Android applies the pass-through region a moment later.
    handler.postDelayed(
      { if (!service.dispatchGesture(gesture, ended, handler)) afterTap() },
      PASS_THROUGH_WAIT_MS,
    )
  }

  private fun afterTap() {
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
    handler.postDelayed({ tapping = false }, PASS_THROUGH_WAIT_MS)
  }

  private fun setCursorOn(on: Boolean, announce: Boolean = true) {
    if (!on && recognized) {
      closeRecognizedText(announce)
      return
    }
    if (isCursorOn == on) {
      if (announce) {
        speak(service.getString(if (on) R.string.review_cursor_on else R.string.review_cursor_off))
      }
      return
    }
    isCursorOn = on
    if (on) {
      startAtFocus = true
    }
    if (announce) {
      speak(service.getString(if (on) R.string.review_cursor_on else R.string.review_cursor_off))
    }
  }

  private fun closeRecognizedText(announce: Boolean) {
    if (!recognized) return
    recognized = false
    cursor = null
    stale = true
    isCursorOn = false
    if (announce) {
      speak(service.getString(R.string.recognized_text_closed))
    }
  }

  /**
   * Recognizes the text in a screenshot and reviews it with the review cursor, from its first line.
   */
  private fun recognizeScreenText() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      speak(service.getString(R.string.recognize_screen_text_failed))
      return
    }
    speak(service.getString(R.string.recognize_screen_text_started))
    capture(retries = 2)
  }

  private fun capture(retries: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    service.takeScreenshot(
      Display.DEFAULT_DISPLAY,
      service.mainExecutor,
      object : AccessibilityService.TakeScreenshotCallback {
        override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
          val buffer = result.hardwareBuffer
          val bitmap =
            try {
              Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)?.let { hardware ->
                try {
                  hardware.copy(Bitmap.Config.ARGB_8888, false)
                } finally {
                  hardware.recycle()
                }
              }
            } catch (e: RuntimeException) {
              LogUtils.w(TAG, "Could not copy the screenshot: %s", e)
              null
            } catch (e: OutOfMemoryError) {
              LogUtils.w(TAG, "No memory to copy the screenshot")
              null
            } finally {
              buffer.close()
            }
          if (bitmap == null) {
            speak(service.getString(R.string.recognize_screen_text_failed))
            return
          }
          recognize(bitmap)
        }

        override fun onFailure(errorCode: Int) {
          // Android allows only a few screenshots a second.
          if (errorCode == AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT && retries > 0) {
            handler.postDelayed({ capture(retries - 1) }, SCREENSHOT_RETRY_MS)
          } else {
            LogUtils.w(TAG, "Screenshot failed: %d", errorCode)
            speak(service.getString(R.string.recognize_screen_text_failed))
          }
        }
      },
    )
  }

  private fun recognize(bitmap: Bitmap) {
    val recognizer = recognizer ?: LanguageTextRecognizer(service).also { recognizer = it }
    recognizer
      .process(InputImage.fromBitmap(bitmap, 0))
      .addOnSuccessListener { result ->
        val items =
          result.textBlocks.flatMap { block ->
            block.lines.flatMap { line ->
              line.elements.mapNotNull { element ->
                val box = element.boundingBox ?: return@mapNotNull null
                ReviewItem(element.text, box.left, box.top, box.right, box.bottom)
              }
            }
          }
        val text = ReviewText(items)
        if (text.isEmpty) {
          speak(service.getString(R.string.recognize_screen_text_none))
          return@addOnSuccessListener
        }
        cursor = ReviewCursor(text)
        recognized = true
        recognizedPackage = service.rootInActiveWindow?.packageName
        isCursorOn = true
        speak(cursor!!, Spoken.LINE)
      }
      .addOnFailureListener { e ->
        LogUtils.w(TAG, "Text recognition failed: %s", e)
        // Google Play services installs the recognizer when an app first needs it.
        if (LanguageTextRecognizer.isUnavailable(e)) {
          recognizer
            .download()
            .addOnSuccessListener { speak(service.getString(R.string.recognize_screen_text_downloading)) }
            .addOnFailureListener { speak(service.getString(R.string.recognize_screen_text_failed)) }
        } else {
          speak(service.getString(R.string.recognize_screen_text_failed))
        }
      }
      .addOnCompleteListener { bitmap.recycle() }
  }

  private companion object {
    const val TAG = "ScreenReview"
    const val MAX_NODES = 4000
    const val MAX_DEPTH = 60
    const val PASS_THROUGH_WAIT_MS = 350L
    const val SCREENSHOT_RETRY_MS = 400L
    const val REREAD_MS = 500L

    // Kept in speech history, so that the last line read can be copied.
    val SPEAK_OPTIONS: SpeakOptions =
      SpeakOptions.create()
        .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
        .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)
  }
}
