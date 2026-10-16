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

package com.google.android.accessibility.talkback.contextmenu.radial

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Region
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.contextmenu.ContextMenu
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Shows the Backtalk menu as a circle in an accessibility overlay. The user touches the screen,
 * slides to an item, and lifts to select it. Lifting in the middle of the circle or outside it
 * closes the menu.
 *
 * While the menu is open, touches on the screen go straight to the menu instead of through touch
 * exploration, so that sliding does not also perform gestures. Where that is not available, the menu
 * follows touch exploration instead.
 */
class RadialMenuController(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val actorState: ActorState,
  private val listener: Listener,
) : RadialMenuView.Listener {

  interface Listener {
    /** The user lifted over [item]. The menu stays open until [dismiss] or [show] is called. */
    fun onItemSelected(item: ContextMenuItem)

    /** The user closed the menu without selecting an item. The menu is already closed. */
    fun onCancelled()
  }

  private val handler = Handler(Looper.getMainLooper())
  private val windowManager = service.getSystemService(WindowManager::class.java)
  private var view: RadialMenuView? = null
  private var items: List<ContextMenuItem> = emptyList()
  private var passthroughRegionSet = false
  private val hintRunnable = Runnable { speakHint() }

  val isShowing: Boolean
    get() = view != null

  /** Shows the visible items of [menu], replacing the items of a menu that is already showing. */
  fun show(title: CharSequence?, menu: ContextMenu) {
    items = (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible }
    if (items.isEmpty()) {
      dismiss()
      return
    }
    val menuView = view ?: addView() ?: return
    menuView.setItems(
      items.map { RadialMenuView.Item(it.title?.toString().orEmpty(), it.hasSubMenu()) }
    )

    // Announce the menu, with a scale that has as many notes as the menu has items.
    val scale = SCALES[(items.size - 1).coerceAtMost(SCALES.size - 1)]
    val feedback =
      if (title.isNullOrEmpty()) {
        Feedback.sound(scale)
      } else {
        Feedback.speech(title, speakOptions(SpeechController.QUEUE_MODE_FLUSH_ALL)).sound(scale)
      }
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, feedback)
    handler.removeCallbacks(hintRunnable)
    handler.postDelayed(hintRunnable, HINT_DELAY_MS)
  }

  /** Closes the menu without any feedback. */
  fun dismiss() {
    handler.removeCallbacks(hintRunnable)
    val menuView = view ?: return
    view = null
    items = emptyList()
    clearPassthroughRegion()
    try {
      windowManager.removeViewImmediate(menuView)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Circle menu was not attached: %s", e)
    }
  }

  /** Moves the focus with fingers that were already down when the menu opened. */
  fun followHeldTouch(x: Float, y: Float, lifted: Boolean) {
    view?.followHeldTouch(x, y, lifted)
  }

  /**
   * Whether a window titled [title] is the menu's own. The menu appearing changes the windows too,
   * and that should not close it.
   */
  fun isOwnWindow(title: CharSequence?): Boolean =
    title?.toString() == service.getString(R.string.title_pref_radial_menu)

  /** Closes the menu as if the user had lifted in the middle of it. */
  fun cancel() {
    if (!isShowing) {
      return
    }
    dismiss()
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
          service.getString(R.string.radial_menu_closed),
          speakOptions(SpeechController.QUEUE_MODE_INTERRUPT),
        )
        .vibration(R.array.view_clicked_pattern)
        .sound(R.raw.tick),
    )
    listener.onCancelled()
  }

  override fun onTouchStarted() {
    handler.removeCallbacks(hintRunnable)
  }

  override fun onItemFocused(index: Int) {
    val item = items.getOrNull(index)
    val text =
      when {
        item == null -> service.getString(R.string.radial_menu_cancel)
        item.hasSubMenu() -> service.getString(R.string.template_radial_menu_submenu, item.title)
        else -> item.title ?: ""
      }
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(text, speakOptions(SpeechController.QUEUE_MODE_INTERRUPT))
        .vibration(R.array.view_actionable_pattern)
        .sound(R.raw.focus_actionable),
    )
  }

  override fun onItemSelected(index: Int) {
    handler.removeCallbacks(hintRunnable)
    val item = items.getOrNull(index)
    if (item == null) {
      cancel()
      return
    }
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.vibration(R.array.view_clicked_pattern).sound(R.raw.tick),
    )
    listener.onItemSelected(item)
  }

  private fun addView(): RadialMenuView? {
    val menuView = RadialMenuView(service, this)
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
        title = service.getString(R.string.title_pref_radial_menu)
      }
    try {
      windowManager.addView(menuView, params)
    } catch (e: RuntimeException) {
      LogUtils.e(TAG, "Could not show the circle menu: %s", e)
      return null
    }
    view = menuView
    setPassthroughRegion()
    return menuView
  }

  /** Sends touches on the whole screen straight to the menu, instead of through touch exploration. */
  private fun setPassthroughRegion() {
    // Pass-through mode and the braille keyboard use the same region, so leave it alone for them.
    if (
      !FeatureSupport.supportPassthrough() ||
        actorState.passThroughModeState.isPassThroughModeActive
    ) {
      return
    }
    val display =
      service.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        ?: return
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION") display.getRealMetrics(metrics)
    service.setTouchExplorationPassthroughRegion(
      Display.DEFAULT_DISPLAY,
      Region(0, 0, metrics.widthPixels, metrics.heightPixels),
    )
    passthroughRegionSet = true
  }

  private fun clearPassthroughRegion() {
    if (passthroughRegionSet) {
      passthroughRegionSet = false
      service.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, Region())
    }
  }

  private fun speakHint() {
    if (!isShowing) {
      return
    }
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        service.getString(R.string.hint_radial_menu),
        speakOptions(SpeechController.QUEUE_MODE_QUEUE),
      ),
    )
  }

  private fun speakOptions(queueMode: Int): SpeakOptions =
    SpeakOptions.create()
      .setQueueMode(queueMode)
      .setFlags(
        FeedbackItem.FLAG_NO_HISTORY or
          FeedbackItem.FLAG_FORCE_FEEDBACK_ALL
      )

  companion object {
    private const val TAG = "RadialMenuController"

    /** How long after the menu opens to speak the usage hint, if the user has not touched it. */
    private const val HINT_DELAY_MS = 2000L

    /**
     * How long to wait after the menu closes before running an item's deferred action, so that the
     * action runs on the screen under the menu.
     */
    const val DEFERRED_ACTION_DELAY_MS = 300L

    /** Scales with one to eight notes, from TalkBack 8.1. */
    private val SCALES =
      intArrayOf(
        R.raw.radial_menu_1,
        R.raw.radial_menu_2,
        R.raw.radial_menu_3,
        R.raw.radial_menu_4,
        R.raw.radial_menu_5,
        R.raw.radial_menu_6,
        R.raw.radial_menu_7,
        R.raw.radial_menu_8,
      )

    /** Whether the user chose to show the Backtalk menu as a circle. */
    @JvmStatic
    fun isEnabled(context: Context): Boolean {
      // A watch's round screen suits a circle; it is scaled to fit.
      if (FormFactorUtils.isAndroidTv()) {
        return false
      }
      return SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.resources,
        R.string.pref_radial_menu_key,
        R.bool.pref_radial_menu_default,
      )
    }
  }
}
