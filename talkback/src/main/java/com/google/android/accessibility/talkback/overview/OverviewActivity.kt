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

package com.google.android.accessibility.talkback.overview

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.pm.LauncherApps
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/**
 * SandFly's own overview, opened by the Overview action instead of the launcher's. It lists the
 * recent tasks as a column of rows that the screen reader moves through in order, most recent first,
 * or last with Most recent app at the bottom: the most recent is the app the overview was opened
 * from. A double tap switches to a task; Close, Split screen and App info are actions of its row.
 * Close all and System overview are floating buttons at the bottom left and bottom right. With
 * Columns above 1, the rows are a grid, filled a row at a time. The Overview action, performed
 * again while this is on screen, switches to the previous app.
 */
class OverviewActivity : AppCompatActivity() {
  private lateinit var scroll: ScrollView
  /** Holds [column], at the top of the window or at the bottom with [newestLast]. */
  private lateinit var content: LinearLayout
  /** The message and the tasks' rows, in [columns] columns. */
  private lateinit var column: GridLayout
  private lateinit var message: TextView
  private lateinit var closeAll: ExtendedFloatingActionButton
  private lateinit var systemOverview: ExtendedFloatingActionButton

  /** Whether the most recent task is listed last, with the list opening scrolled to its end. */
  private var newestLast = false

  /** The number of columns: 1 is a list. */
  private var columns = 1

  /** A row per task, kept so that a focused row stays focused when the list changes. */
  private val rows = HashMap<Int, TextView>()
  private var tasks: List<RecentTask> = emptyList()

  /** The view in the list that has accessibility focus, if one has. */
  private var focusedView: View? = null

  /** Whether the list has just opened, so focus goes to the most recent task. */
  private var opening = false

  /**
   * The place in the list of an app that was just closed, where focus goes next. SandFly's actions
   * menu took focus from its row, so the row being read can't be followed.
   */
  private var focusIndex: Int? = null

  /** The view that gets accessibility focus when the overview's window has input focus. */
  private var pendingFocus: View? = null
  private var loaded = false
  /** Whether the tasks couldn't be read. */
  private var failed = false
  private var loading = false
  private var loadAgain = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.overview_title)
    column = GridLayout(this).apply { accessibilityDelegate = ColumnDelegate() }
    content =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(
          column,
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
          ),
        )
      }
    message = textRow()
    closeAll =
      button(R.string.overview_close_all, Gravity.BOTTOM or Gravity.START) { closeAll() }.apply {
        visibility = View.GONE
      }
    systemOverview =
      button(R.string.overview_system, Gravity.BOTTOM or Gravity.END) {
        TalkBackService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        finish()
      }
    scroll =
      ScrollView(this).apply {
        isFillViewport = true
        clipToPadding = false
        addView(content)
      }
    // The list scrolls clear of the buttons, which cover its end.
    systemOverview.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
      val bottom = view.height + 2 * dp(BUTTON_MARGIN_DP)
      if (scroll.paddingBottom != bottom) {
        scroll.setPadding(0, 0, 0, bottom)
      }
    }
    val root =
      FrameLayout(this).apply {
        addView(scroll, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        addView(closeAll)
        addView(systemOverview)
      }
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      insets
    }
    setContentView(root)
  }

  override fun onStart() {
    super.onStart()
    newestLast = RecentTasks.isNewestLast(this)
    // A short list sits at the bottom, beside the buttons.
    content.gravity = if (newestLast) Gravity.BOTTOM else Gravity.TOP
    columns = RecentTasks.columns(this)
    column.columnCount = columns
    // The message spans the whole width.
    message.layoutParams = cellParams(span = columns)
    opening = true
    loaded = false
    ShizukuShell.hold(this)
    load()
  }

  override fun onResume() {
    super.onResume()
    resumed++
  }

  override fun onPause() {
    resumed--
    super.onPause()
  }

  override fun onStop() {
    ShizukuShell.release()
    super.onStop()
    // Like the system's overview, this one closes when the user leaves it.
    if (!isChangingConfigurations) {
      finish()
    }
  }

  private fun load() {
    if (loading) {
      loadAgain = true
      return
    }
    loading = true
    RecentTasks.load(this) { result ->
      loading = false
      if (isFinishing || isDestroyed) return@load
      if (loadAgain) {
        loadAgain = false
        load()
        return@load
      }
      loaded = true
      failed = result == null
      tasks = result.orEmpty()
      show()
    }
  }

  private fun show() {
    val views = ArrayList<View>()
    when {
      !loaded -> R.string.overview_loading
      failed -> R.string.overview_unavailable
      tasks.isEmpty() -> R.string.overview_empty
      else -> null
    }?.let {
      message.setText(it)
      views += message
    }
    rows.keys.retainAll(tasks.map { it.id }.toSet())
    shownTasks().forEachIndexed { position, task ->
      views += row(task).also { bind(it, task, position) }
    }
    closeAll.visibility = if (tasks.isNotEmpty()) View.VISIBLE else View.GONE
    var focusAfter: View? = null
    var scrollToEnd = false
    if (opening && loaded) {
      // Otherwise focus can start on whatever the screen reader focused last in this window.
      opening = false
      focusAfter = if (newestLast) views.last() else views.first()
      scrollToEnd = newestLast
    }
    focusIndex?.let { index ->
      focusIndex = null
      focusAfter = views[minOf(index, views.size - 1)]
    }
    // The view being read was removed: focus moves to the one that takes its place.
    focusedView
      ?.takeIf { it !in views }
      ?.let { column.indexOfChild(it) }
      ?.takeIf { it >= 0 }
      ?.let { index ->
        focusedView = null
        focusAfter = views[minOf(index, views.size - 1)]
      }
    setChildren(views)
    if (scrollToEnd) {
      scroll.doOnPreDraw { scroll.scrollTo(0, content.height) }
    }
    focusAfter?.let { focus(it) }
  }

  /** The tasks in the order they are listed. */
  private fun shownTasks(): List<RecentTask> = if (newestLast) tasks.asReversed() else tasks

  /**
   * Moves accessibility focus to [view] once the overview's window has input focus. While SandFly's
   * actions menu is open, it has the focus, and the screen reader says the overview's title when it
   * closes: focus moved before then isn't said.
   */
  private fun focus(view: View) {
    pendingFocus = view
    if (hasWindowFocus()) {
      view.post { focusPending() }
    }
  }

  private fun focusPending() {
    val view = pendingFocus ?: return
    pendingFocus = null
    if (view.parent === column) {
      view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus && pendingFocus != null) {
      column.postDelayed({ focusPending() }, REFOCUS_DELAY_MS)
    }
  }

  private fun row(task: RecentTask): TextView =
    rows.getOrPut(task.id) {
      textRow().apply {
        val delegate = RowDelegate()
        accessibilityDelegate = delegate
        setTag(R.id.overview_row_delegate, delegate)
      }
    }

  /**
   * Delete closes the app whose row the screen reader is on. Input focus can be on another row,
   * since the screen reader moves only its own focus.
   */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode == KeyEvent.KEYCODE_DEL || event.keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
      val task = (focusedView?.getTag(R.id.overview_row_delegate) as? RowDelegate)?.task
      if (task != null) {
        if (event.action == KeyEvent.ACTION_UP) {
          close(task)
        }
        return true
      }
    }
    return super.dispatchKeyEvent(event)
  }

  private fun bind(row: TextView, task: RecentTask, position: Int) {
    val name = task.name(this)
    row.text = name
    row.contentDescription = name
    row.setOnClickListener { switchTo(task) }
    (row.getTag(R.id.overview_row_delegate) as RowDelegate).set(task, position)
  }

  private fun switchTo(task: RecentTask) {
    RecentTasks.open(this, task) { worked ->
      if (!worked) {
        announce(column, getString(R.string.overview_failed))
      }
      // On success, the task covers the overview, which closes when it stops.
    }
  }

  private fun close(task: RecentTask) {
    RecentTasks.close(this, task) { worked ->
      if (!worked) {
        announce(column, getString(R.string.overview_failed))
        return@close
      }
      announce(column, getString(R.string.overview_closed, task.appLabel))
      shownTasks().indexOfFirst { it.id == task.id }.takeIf { it >= 0 }?.let { focusIndex = it }
      tasks = tasks.filter { it.id != task.id }
      show()
      load()
    }
  }

  private fun closeAll() {
    val all = tasks
    var left = all.size
    var failed = false
    for (task in all) {
      RecentTasks.close(this, task) { worked ->
        failed = failed || !worked
        if (--left == 0) {
          if (failed) {
            announce(column, getString(R.string.overview_failed))
            load()
          } else {
            // Like the system's overview, Close all leaves for the home screen.
            TalkBackService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            finish()
          }
        }
      }
    }
  }

  /** Puts [task] in split screen beside the app the overview was opened from. */
  private fun split(task: RecentTask) {
    val front = tasks.firstOrNull() ?: return
    RecentTasks.split(this, front, task) { worked ->
      if (!worked) {
        announce(column, getString(R.string.overview_split_failed))
      }
    }
  }

  /**
   * Opens the app's App info page in Settings, in the task's profile: the work profile's page for a
   * work app, which Settings' own intent can't open from the personal profile.
   */
  private fun openAppInfo(task: RecentTask) {
    runCatching {
      getSystemService(LauncherApps::class.java)
        .startAppDetailsActivity(
          task.component ?: ComponentName(task.packageName, ""),
          task.user,
          null,
          null,
        )
    }
      .recoverCatching {
        startActivity(
          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", task.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
      }
  }

  /** Tells the screen reader the list's size, and notes which of its views has focus. */
  private inner class ColumnDelegate : View.AccessibilityDelegate() {
    @Suppress("DEPRECATION") // CollectionInfo's constructor needs Android 13.
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      if (tasks.isNotEmpty()) {
        info.collectionInfo =
          AccessibilityNodeInfo.CollectionInfo.obtain(
            (tasks.size + columns - 1) / columns,
            columns,
            false,
          )
      }
    }

    override fun onRequestSendAccessibilityEvent(
      host: ViewGroup,
      child: View,
      event: AccessibilityEvent,
    ): Boolean {
      when (event.eventType) {
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> focusedView = child
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED ->
          if (focusedView === child) {
            focusedView = null
          }
      }
      return super.onRequestSendAccessibilityEvent(host, child, event)
    }
  }

  /** Gives a task's row its place in the list and the actions Close, Split screen and App info. */
  private inner class RowDelegate : View.AccessibilityDelegate() {
    var task: RecentTask? = null
      private set
    /** The row's place in the list. */
    private var position = 0

    fun set(task: RecentTask, position: Int) {
      this.task = task
      this.position = position
    }

    @Suppress("DEPRECATION") // CollectionItemInfo's constructor needs Android 13.
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      val task = task ?: return
      info.collectionItemInfo =
        AccessibilityNodeInfo.CollectionItemInfo.obtain(
          position / columns,
          1,
          position % columns,
          1,
          false,
        )
      info.addAction(AccessibilityAction(R.id.overview_close_action, getString(R.string.overview_close)))
      // The most recent task is the one a split goes beside.
      if (task.id != tasks.firstOrNull()?.id) {
        info.addAction(AccessibilityAction(R.id.overview_split_action, getString(R.string.overview_split)))
      }
      info.addAction(AccessibilityAction(R.id.overview_app_info_action, getString(R.string.overview_app_info)))
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
      val task = task ?: return super.performAccessibilityAction(host, action, args)
      when (action) {
        R.id.overview_close_action -> close(task)
        R.id.overview_split_action -> split(task)
        R.id.overview_app_info_action -> openAppInfo(task)
        else -> return super.performAccessibilityAction(host, action, args)
      }
      return true
    }
  }

  /**
   * Puts [views] in the column in order, moving only the views that are out of place. A view taken
   * out of the window loses accessibility focus, so the others stay where they are.
   */
  private fun setChildren(views: List<View>) {
    val wanted = views.toSet()
    for (i in column.childCount - 1 downTo 0) {
      if (column.getChildAt(i) !in wanted) {
        column.removeViewAt(i)
      }
    }
    views.forEachIndexed { index, view ->
      if (column.getChildAt(index) !== view) {
        (view.parent as? ViewGroup)?.removeView(view)
        column.addView(view, index)
      }
    }
  }

  private fun textRow(): TextView =
    TextView(this).apply {
      val padding = dp(16)
      setPadding(padding, padding, padding, padding)
      minHeight = dp(48)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      isFocusable = true
      layoutParams = cellParams()
    }

  /** A cell of the grid [span] columns wide, the columns sharing the width equally. */
  private fun cellParams(span: Int = 1): GridLayout.LayoutParams =
    GridLayout.LayoutParams(
        GridLayout.spec(GridLayout.UNDEFINED),
        GridLayout.spec(GridLayout.UNDEFINED, span, 1f),
      )
      .apply {
        width = 0
        setGravity(Gravity.FILL)
      }

  /** A floating button at [gravity] in the window. */
  private fun button(
    @StringRes text: Int,
    gravity: Int,
    onClick: () -> Unit,
  ): ExtendedFloatingActionButton =
    ExtendedFloatingActionButton(this).apply {
      setText(text)
      isAllCaps = false
      setOnClickListener { onClick() }
      layoutParams =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            gravity,
          )
          .apply { dp(BUTTON_MARGIN_DP).let { setMargins(it, it, it, it) } }
    }

  private fun dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
      .toInt()

  companion object {
    /** Longer than the screen reader takes to say the window's title when its menu closes. */
    private const val REFOCUS_DELAY_MS = 300L

    /** Space around the floating buttons. */
    private const val BUTTON_MARGIN_DP = 16

    /**
     * The number of resumed overviews. onStop can come long after the user has left, while onPause
     * comes at once. SandFly's menus are overlays, which don't pause the overview.
     */
    private var resumed = 0

    /** The app windows on screen when the overview was last opened, as [RecentTasks.frontPackages]. */
    private var openedFromPackages: List<String> = emptyList()

    /** Whether the overview is on screen. */
    @JvmStatic fun isShown(): Boolean = resumed > 0

    /**
     * The app windows on screen when the overview on screen was opened, or null when the overview
     * isn't on screen.
     */
    fun openedFrom(): List<String>? = if (isShown()) openedFromPackages else null

    /**
     * Opens the overview in place of the system's, if the user has chosen it and Shizuku runs,
     * asking Shizuku to allow SandFly first if it doesn't. The system's overview is kept on the
     * lock screen, and opens when Shizuku doesn't allow SandFly.
     */
    @JvmStatic
    fun openInsteadOfSystem(service: AccessibilityService): Boolean {
      if (!RecentTasks.isEnabled(service) || !ShizukuShell.isAvailable(service) || !ShizukuGrants.isReady()) {
        return false
      }
      if (service.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
        return false
      }
      openedFromPackages = RecentTasks.frontPackages(service)
      val appContext = service.applicationContext
      ShizukuGrants.withShizukuPermission { allowed ->
        if (allowed) {
          appContext.startActivity(
            Intent(appContext, OverviewActivity::class.java)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          )
        } else {
          TalkBackService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }
      }
      return true
    }
  }
}
