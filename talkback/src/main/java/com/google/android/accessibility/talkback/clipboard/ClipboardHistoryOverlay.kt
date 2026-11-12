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

package com.google.android.accessibility.talkback.clipboard

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.HandlerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.SystemActionPerformer
import com.google.android.accessibility.talkback.shizuku.ShizukuClipboard
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.ClipboardHistory
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.monitor.ScreenMonitor
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * The texts copied, newest first, in an accessibility overlay over the screen the history was
 * opened from, which stays open under it. Each text can be pasted into the edit box the history was
 * opened from, copied again, added to the end of the clipboard's text, or removed. The list shows
 * long texts shortened, and the actions use the whole text.
 *
 * Opened for an edit box, the window doesn't take input focus, so that the edit box's window keeps
 * it and its app can read the clipboard for Paste. Opened without one, the window takes input
 * focus, as SandFly can read the clipboard only then.
 *
 * Back closes the actions of a text, then the window. Main thread only.
 */
object ClipboardHistoryOverlay {
  private const val TAG = "ClipboardHistoryOverlay"
  private const val TITLE_LENGTH = 500
  private const val MAX_LINES = 3
  private const val PADDING_DP = 16
  private const val MIN_ROW_HEIGHT_DP = 48

  // Accessibility focus moves into a new window once it is in the window list, and onto a new page
  // once it is laid out.
  private const val WINDOW_FOCUS_DELAY_MS = 300L
  private const val PAGE_FOCUS_DELAY_MS = 100L

  /** Configuration changes that rebuild the window. */
  private const val REBUILD_CHANGES =
    ActivityInfo.CONFIG_UI_MODE or
      ActivityInfo.CONFIG_LOCALE or
      ActivityInfo.CONFIG_LAYOUT_DIRECTION or
      ActivityInfo.CONFIG_FONT_SCALE or
      ActivityInfo.CONFIG_DENSITY or
      ActivityInfo.CONFIG_ORIENTATION or
      ActivityInfo.CONFIG_SCREEN_SIZE or
      ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
      ActivityInfo.CONFIG_SCREEN_LAYOUT

  /** Global actions that show another screen, which close the window. */
  private val LEAVING_ACTIONS =
    setOf(
      AccessibilityService.GLOBAL_ACTION_HOME,
      AccessibilityService.GLOBAL_ACTION_RECENTS,
      AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS,
      AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS,
      AccessibilityService.GLOBAL_ACTION_POWER_DIALOG,
      AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN,
      AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS,
      SystemActionPerformer.GLOBAL_ACTION_SYSTEM_NOTIFICATIONS,
      SystemActionPerformer.GLOBAL_ACTION_SYSTEM_QUICK_SETTINGS,
      SystemActionPerformer.GLOBAL_ACTION_SYSTEM_OVERVIEW,
    )

  private val handler = Handler(Looper.getMainLooper())
  private val focusToken = Any()

  private var service: AccessibilityService? = null
  private var root: LinearLayout? = null
  private var shownConfig: Configuration? = null

  /** The edit box the history was opened from, which Paste pastes into. */
  private var pasteTarget: AccessibilityNodeInfo? = null

  /** The item with accessibility focus when the history opened, focused again when it closes. */
  private var focusBefore: AccessibilityNodeInfo? = null

  /** The text whose actions are shown, or null while the list is shown. */
  private var chosen: String? = null

  // The list's parts, while it is shown.
  private var entries: List<String> = emptyList()
  private var listAdapter: ArrayAdapter<String>? = null
  private var clearButton: View? = null

  // Whether a Back or Escape press went down while the window showed: its release is the window's.
  private var backKeyDown = false

  private val onChange: () -> Unit = { if (root != null && chosen == null) refreshList() }

  // The clipboard can be read once the window has input focus, which it doesn't have yet when it
  // is added. While Shizuku adds each copy, the history has the clipboard's text already.
  private val onWindowFocus =
    ViewTreeObserver.OnWindowFocusChangeListener { hasFocus -> if (hasFocus) recordClipboard() }

  @JvmStatic
  val isShowing: Boolean
    get() = root != null

  /** Opens the clipboard history from the service, for the edit box with focus. */
  @JvmStatic
  fun open(context: Context): Boolean =
    open(context, (context as? AccessibilityService)?.let(::editBoxWithFocus))

  /**
   * Opens the clipboard history from the service, for [editBox], found before a SandFly window
   * took the focus. Opened again while open, it keeps the edit box it was opened for.
   */
  @JvmStatic
  fun open(context: Context, editBox: AccessibilityNodeInfo?): Boolean {
    val service = context as? AccessibilityService ?: return false
    // The history can hold private text.
    if (ScreenMonitor.isDeviceLocked(service)) return false
    // A paste from a history closed before stops once the history opens again.
    PasteAttempts.cancel()
    val wasShowing = isShowing
    val target = editBox?.takeIf { it.isEditable } ?: if (wasShowing) pasteTarget else null
    val before =
      if (wasShowing) focusBefore else service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
    close(restoreFocus = false)
    return show(service, target, before)
  }

  /** Removes the window, such as when the screen turns off or SandFly stops. */
  @JvmStatic
  fun close() {
    close(restoreFocus = false)
  }

  /** Shows the list again from the actions of a text, or else closes the window. */
  @JvmStatic
  fun back(): Boolean {
    if (!isShowing) return false
    val text = chosen
    if (text != null) showList(focusText = text) else close(restoreFocus = true)
    return true
  }

  /**
   * Called before SandFly performs the global [action]: Back goes back in the window, and actions
   * that show another screen close it. Returns whether the action was used up.
   */
  @JvmStatic
  fun onGlobalAction(action: Int): Boolean {
    if (!isShowing) return false
    if (action == AccessibilityService.GLOBAL_ACTION_BACK) return back()
    if (action in LEAVING_ACTIONS) close()
    return false
  }

  /** Takes Back and Escape presses while the window shows. Returns whether [event] is used up. */
  @JvmStatic
  fun onKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode != KeyEvent.KEYCODE_BACK && event.keyCode != KeyEvent.KEYCODE_ESCAPE) {
      return false
    }
    return when (event.action) {
      KeyEvent.ACTION_DOWN -> {
        backKeyDown = isShowing
        backKeyDown
      }
      KeyEvent.ACTION_UP -> {
        if (!backKeyDown) return false
        backKeyDown = false
        back()
        true
      }
      else -> false
    }
  }

  /** Builds the window again for [newConfig], such as a new theme, language or screen size. */
  @JvmStatic
  fun onConfigurationChanged(newConfig: Configuration) {
    val service = service ?: return
    val shown = shownConfig ?: return
    if ((shown.diff(newConfig) and REBUILD_CHANGES) == 0) return
    val target = pasteTarget
    val before = focusBefore
    close(restoreFocus = false)
    show(service, target, before)
  }

  /** Whether [window] is the clipboard history's window. */
  @JvmStatic
  fun isOwnWindow(context: Context, window: AccessibilityWindowInfo): Boolean =
    isShowing &&
      window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
      TextUtils.equals(window.title, context.getString(R.string.title_clipboard_history))

  /** The edit box with accessibility focus, or else with input focus. */
  @JvmStatic
  fun editBoxWithFocus(service: AccessibilityService): AccessibilityNodeInfo? =
    listOf(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY, AccessibilityNodeInfo.FOCUS_INPUT)
      .firstNotNullOfOrNull { focus -> service.findFocus(focus)?.takeIf { it.isEditable } }

  private fun show(
    service: AccessibilityService,
    target: AccessibilityNodeInfo?,
    before: AccessibilityNodeInfo?,
  ): Boolean {
    val context = ContextThemeWrapper(service, R.style.TalkBackSettings)
    val padding = dp(context, PADDING_DP)
    val content =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, padding, padding, padding)
        setBackgroundColor(themeColor(context, android.R.attr.colorBackground))
      }
    // Clear of the status and navigation bars and the display cutout, where the window covers them.
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
      view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
      insets
    }
    val params =
      WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSLUCENT
        // Without input focus, the edit box's window keeps it, and its app can paste.
        flags = if (target != null) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        title = service.getString(R.string.title_clipboard_history)
      }
    try {
      service.getSystemService(WindowManager::class.java).addView(content, params)
    } catch (e: RuntimeException) {
      LogUtils.e(TAG, "Could not show the clipboard history: %s", e)
      return false
    }
    this.service = service
    root = content
    pasteTarget = target
    focusBefore = before
    shownConfig = Configuration(service.resources.configuration)
    ClipboardHistory.addListener(onChange)
    content.viewTreeObserver.addOnWindowFocusChangeListener(onWindowFocus)
    recordClipboard()
    showList(focusText = null, focusDelayMs = WINDOW_FOCUS_DELAY_MS)
    return true
  }

  private fun close(restoreFocus: Boolean) {
    val view = root ?: return
    val service = service
    val target = pasteTarget
    val before = focusBefore
    handler.removeCallbacksAndMessages(focusToken)
    ClipboardHistory.removeListener(onChange)
    view.viewTreeObserver.removeOnWindowFocusChangeListener(onWindowFocus)
    root = null
    this.service = null
    shownConfig = null
    pasteTarget = null
    focusBefore = null
    chosen = null
    entries = emptyList()
    listAdapter = null
    clearButton = null
    try {
      service?.getSystemService(WindowManager::class.java)?.removeViewImmediate(view)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Clipboard history window was not attached: %s", e)
    }
    if (restoreFocus) {
      (target ?: before)?.let {
        if (it.refresh()) it.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
      }
    }
  }

  /** Adds the clipboard's text to the history, if SandFly may read it now. */
  private fun recordClipboard() {
    val service = service ?: return
    if (!ShizukuClipboard.isWatching()) ClipboardHistory.recordClipboard(service)
  }

  /**
   * Shows the list of texts, and puts accessibility focus on [focusText]'s row, or else on the
   * title, after [focusDelayMs].
   */
  private fun showList(focusText: String?, focusDelayMs: Long = PAGE_FOCUS_DELAY_MS) {
    val page = root ?: return
    val context = page.context
    chosen = null
    page.removeAllViews()
    val title = heading(page, context.getString(R.string.title_clipboard_history))
    if (ShizukuShell.isAllowed()) {
      page.addView(
        SwitchCompat(context).apply {
          setText(R.string.title_clipboard_history_other_apps)
          isChecked = ShizukuClipboard.isWanted(context)
          minHeight = dp(context, MIN_ROW_HEIGHT_DP)
          setOnCheckedChangeListener { _, checked -> ShizukuClipboard.setWanted(context, checked) }
        },
        wrapHeight(),
      )
    }
    val adapter = rows(context, emptyList())
    val list = ListView(context)
    list.adapter = adapter
    list.setOnItemClickListener { _, _, position, _ ->
      entries.getOrNull(position)?.let(::showActions)
    }
    val empty =
      TextView(context).apply {
        setText(R.string.clipboard_history_empty)
        setTextAppearance(android.R.style.TextAppearance_Material_Body1)
      }
    page.addView(list, fillHeight())
    page.addView(empty, fillHeight())
    list.emptyView = empty
    listAdapter = adapter
    clearButton =
      button(page, R.string.clipboard_history_clear) {
        ClipboardHistory.clear(context)
        // The button goes with the texts.
        refreshList()
        focusLater(PAGE_FOCUS_DELAY_MS) { empty }
      }
    button(page, R.string.close_string) { close(restoreFocus = true) }
    refreshList()
    val index = focusText?.let { entries.indexOf(it) } ?: -1
    if (index >= 0) {
      list.setSelection(index)
      focusLater(focusDelayMs) { list.getChildAt(index - list.firstVisiblePosition) }
    } else {
      focusLater(focusDelayMs) { title }
    }
  }

  private fun refreshList() {
    val service = service ?: return
    val adapter = listAdapter ?: return
    entries = ClipboardHistory.entries(service)
    adapter.setNotifyOnChange(false)
    adapter.clear()
    adapter.addAll(entries.map(::shortened))
    adapter.notifyDataSetChanged()
    clearButton?.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
  }

  /** Shows what can be done with [text], in place of the list. */
  private fun showActions(text: String) {
    val page = root ?: return
    val context = page.context
    chosen = text
    listAdapter = null
    clearButton = null
    page.removeAllViews()
    val title = heading(page, shortened(text)).apply { maxLines = MAX_LINES }
    val actions: List<Pair<Int, () -> Unit>> =
      listOfNotNull(
        pasteTarget?.let { android.R.string.paste to { paste(text) } },
        android.R.string.copy to { done(text) { ClipboardHistory.copy(context, text) } },
        // As the Add to clipboard command does, with the whole text rather than the shortened one.
        R.string.shortcut_add_to_clipboard to { done(text) { ClipboardHistory.append(context, text) } },
        R.string.notification_remove to { done(text) { ClipboardHistory.remove(context, text) } },
      )
    val list = ListView(context)
    list.adapter = rows(context, actions.map { context.getString(it.first) })
    list.setOnItemClickListener { _, _, position, _ -> actions.getOrNull(position)?.second?.invoke() }
    page.addView(list, fillHeight())
    button(page, android.R.string.cancel) { showList(focusText = text) }
    focusLater(PAGE_FOCUS_DELAY_MS) { title }
  }

  /** Runs [action] on [text], then shows the list with focus on the text, if it is still there. */
  private fun done(text: String, action: () -> Unit) {
    action()
    showList(focusText = text)
  }

  /** Copies [text], closes the history and pastes it into the edit box it was opened from. */
  private fun paste(text: String) {
    val service = service ?: return
    val target = pasteTarget ?: return
    if (!ClipboardHistory.copy(service, text)) return
    close(restoreFocus = true)
    // The edit box's window kept input focus, so the paste normally works at once; it is tried
    // again while the edit box's app is busy.
    PasteAttempts.start(
      0L,
      { TalkBackService.getInstance() === service },
      { pasteInto(target) },
      {
        (service as? TalkBackService)
          ?.feedbackReturner
          ?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.complete))
      },
    )
  }

  /**
   * Pastes into [target], giving it input focus first: an edit box pastes only with input focus,
   * and it may have had only accessibility focus.
   */
  private fun pasteInto(target: AccessibilityNodeInfo): Boolean {
    if (!target.refresh()) return false
    if (!target.isFocused) target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    return target.performAction(AccessibilityNodeInfo.ACTION_PASTE)
  }

  private fun focusLater(delayMs: Long, view: () -> View?) {
    handler.removeCallbacksAndMessages(focusToken)
    HandlerCompat.postDelayed(
      handler,
      { view()?.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null) },
      focusToken,
      delayMs,
    )
  }

  private fun heading(page: LinearLayout, text: CharSequence): TextView =
    TextView(page.context).also {
      it.text = text
      it.ellipsize = TextUtils.TruncateAt.END
      it.setTextAppearance(android.R.style.TextAppearance_Material_Title)
      ViewCompat.setAccessibilityHeading(it, true)
      page.addView(it, wrapHeight())
    }

  private fun button(page: LinearLayout, @StringRes text: Int, onClick: () -> Unit): Button =
    Button(page.context).also {
      it.setText(text)
      it.setOnClickListener { onClick() }
      page.addView(it, wrapHeight())
    }

  private fun wrapHeight() =
    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

  private fun fillHeight() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)

  /** Rows of [items], each shown in up to [MAX_LINES] lines and read whole. */
  private fun rows(context: Context, items: List<String>): ArrayAdapter<String> =
    object :
      ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items.toMutableList()) {
      override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        super.getView(position, convertView, parent).also {
          (it as? TextView)?.apply {
            maxLines = MAX_LINES
            ellipsize = TextUtils.TruncateAt.END
          }
        }
    }

  private fun shortened(text: String): String =
    if (text.length > TITLE_LENGTH) text.take(TITLE_LENGTH) + "…" else text

  private fun themeColor(context: Context, attr: Int): Int {
    val value = TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return value.data
  }

  private fun dp(context: Context, value: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        context.resources.displayMetrics,
      )
      .toInt()
}
