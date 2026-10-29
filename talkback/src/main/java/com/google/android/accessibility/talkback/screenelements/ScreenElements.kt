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

package com.google.android.accessibility.talkback.screenelements

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * An element that an app draws without exposing it to accessibility services, found by other
 * means, such as SandFly Pro's screen recognition.
 *
 * @param bounds where the element is on screen, in pixels.
 */
data class ScreenElement(
  val label: String,
  val role: Role,
  val bounds: Rect,
  val state: String? = null,
) {
  enum class Role(val className: String) {
    BUTTON("android.widget.Button"),
    TEXT("android.widget.TextView"),
    HEADING("android.widget.TextView"),
    IMAGE("android.widget.ImageView"),
    TEXT_FIELD("android.widget.EditText"),
    CHECK_BOX("android.widget.CheckBox"),
    SWITCH("android.widget.Switch"),
    SLIDER("android.widget.SeekBar"),
    TAB("android.widget.TabWidget"),
    LINK("android.widget.Button"),
  }

  val isActionable: Boolean
    get() = role != Role.TEXT && role != Role.HEADING && role != Role.IMAGE
}

/**
 * Shows [ScreenElement]s as accessibility nodes: each in a transparent accessibility overlay
 * window at the element's bounds. Android reports only touchable windows to accessibility services,
 * so the windows are touchable, which also lets touch exploration find them. A click on one taps
 * the screen at its centre, with the windows made untouchable for the tap so that it reaches the
 * app.
 */
class ScreenElementWindows(
  private val service: AccessibilityService,
  private val pipeline: () -> Pipeline.FeedbackReturner?,
) {
  private val windowManager = service.getSystemService(WindowManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val shown = ArrayList<Pair<ElementView, WindowManager.LayoutParams>>()

  /** The elements on screen, in reading order. */
  val elements: List<ScreenElement>
    get() = shown.map { it.first.element }

  fun show(elements: List<ScreenElement>) {
    clear()
    for (element in elements) {
      if (element.bounds.isEmpty) continue
      val view = ElementView(service, element)
      val params = layoutParams(element.bounds)
      try {
        windowManager.addView(view, params)
        shown.add(view to params)
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "Could not show element %s: %s", element.label, e)
      }
    }
  }

  fun clear() {
    handler.removeCallbacksAndMessages(null)
    for ((view, _) in shown) {
      try {
        windowManager.removeViewImmediate(view)
      } catch (e: IllegalArgumentException) {
        LogUtils.w(TAG, "Element window was not attached: %s", e)
      }
    }
    shown.clear()
  }

  fun isShowing(): Boolean = shown.isNotEmpty()

  private fun layoutParams(bounds: Rect): WindowManager.LayoutParams =
    WindowManager.LayoutParams().apply {
      type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
      format = PixelFormat.TRANSLUCENT
      flags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
      gravity = Gravity.TOP or Gravity.START
      x = bounds.left
      y = bounds.top
      width = bounds.width()
      height = bounds.height()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
      }
      title = WINDOW_TITLE
    }

  /** Makes every element window untouchable, or touchable again. */
  private fun setTouchable(touchable: Boolean) {
    for ((view, params) in shown) {
      params.flags =
        if (touchable) {
          params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
          params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
      try {
        windowManager.updateViewLayout(view, params)
      } catch (e: IllegalArgumentException) {
        LogUtils.w(TAG, "Element window was not attached: %s", e)
      }
    }
  }

  /** Taps the screen at the centre of [element], or holds there when [long] is set. */
  private fun tap(view: ElementView, long: Boolean): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    val bounds = view.element.bounds
    val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
    val duration =
      if (long) ViewConfiguration.getLongPressTimeout() * 2L else ViewConfiguration.getTapTimeout().toLong()
    val gesture =
      GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        .build()
    setTouchable(false)
    val metrics = service.resources.displayMetrics
    pipeline()?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, metrics.widthPixels, metrics.heightPixels)),
    )
    val ended =
      object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) = afterTap(view)

        override fun onCancelled(description: GestureDescription) = afterTap(view)
      }
    // Android applies the pass-through region and the window flags a moment later.
    handler.postDelayed(
      { if (!service.dispatchGesture(gesture, ended, handler)) afterTap(view) },
      PASS_THROUGH_WAIT_MS,
    )
    return true
  }

  private fun afterTap(view: ElementView) {
    pipeline()?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
    // The app gets time to respond. If the screen changed, whoever showed the elements replaces
    // them; until then, focus returns to the element that was clicked.
    handler.postDelayed(
      {
        if (shown.any { it.first === view }) {
          setTouchable(true)
          handler.postDelayed(
            { view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null) },
            REFOCUS_DELAY_MS,
          )
        }
      },
      SETTLE_MS,
    )
  }

  private inner class ElementView(context: Context, val element: ScreenElement) : View(context) {
    init {
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
      contentDescription = element.label
      isFocusable = true
      if (element.isActionable) {
        isClickable = true
        isLongClickable = true
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && element.role == ScreenElement.Role.HEADING) {
        isAccessibilityHeading = true
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && element.state != null) {
        stateDescription = element.state
      }
    }

    override fun getAccessibilityClassName(): CharSequence = element.role.className

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean =
      when (action) {
        AccessibilityNodeInfo.ACTION_CLICK -> tap(this, long = false)
        AccessibilityNodeInfo.ACTION_LONG_CLICK -> tap(this, long = true)
        else -> super.performAccessibilityAction(action, arguments)
      }
  }

  companion object {
    private const val TAG = "ScreenElements"

    /** The title of every element window. */
    const val WINDOW_TITLE = "SandFly screen elements"

    private const val PASS_THROUGH_WAIT_MS = 350L
    private const val SETTLE_MS = 600L
    private const val REFOCUS_DELAY_MS = 150L
  }
}
