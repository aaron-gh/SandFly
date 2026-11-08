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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell

/**
 * SandFly's own overview, opened by the Overview action instead of the launcher's. It lists the
 * recent tasks, most recent first, as a column of rows that the screen reader moves through in
 * order: the first is the app the overview was opened from. A double tap switches to a task; Close,
 * Split screen and App info are actions of its row.
 */
class OverviewActivity : AppCompatActivity() {
  private lateinit var column: LinearLayout
  private lateinit var message: TextView
  private lateinit var closeAll: Button
  private lateinit var systemOverview: Button

  /** A row per task, kept so that a focused row stays focused when the list changes. */
  private val rows = HashMap<Int, TextView>()
  private var tasks: List<RecentTask> = emptyList()

  /** The view in the list that has accessibility focus, if one has. */
  private var focusedView: View? = null

  /** Whether the list has just opened, so focus goes to its first item. */
  private var opening = false
  private var loaded = false
  /** Whether the tasks couldn't be read. */
  private var failed = false
  private var loading = false
  private var loadAgain = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.overview_title)
    column =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        accessibilityDelegate = ColumnDelegate()
      }
    message = textRow()
    closeAll = button(R.string.overview_close_all) { closeAll() }
    systemOverview =
      button(R.string.overview_system) {
        TalkBackService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        finish()
      }
    val scroll = ScrollView(this).apply { addView(column) }
    ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      insets
    }
    setContentView(scroll)
  }

  override fun onStart() {
    super.onStart()
    opening = true
    loaded = false
    ShizukuShell.hold(this)
    load()
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
    tasks.forEachIndexed { index, task -> views += row(task).also { bind(it, task, index) } }
    if (tasks.isNotEmpty()) {
      views += closeAll
    }
    views += systemOverview
    var focusAfter: View? = null
    if (opening && loaded) {
      // Otherwise focus can start on whatever the screen reader focused last in this window.
      opening = false
      focusAfter = views.first()
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
    focusAfter?.let { view ->
      view.post { view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null) }
    }
  }

  private fun row(task: RecentTask): TextView =
    rows.getOrPut(task.id) {
      textRow().apply {
        val delegate = RowDelegate()
        accessibilityDelegate = delegate
        setTag(R.id.overview_row_delegate, delegate)
        setOnKeyListener { _, keyCode, event ->
          val close = keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL
          if (close && event.action == KeyEvent.ACTION_UP) {
            (getTag(R.id.overview_row_delegate) as RowDelegate).task?.let { close(it) }
          }
          close
        }
      }
    }

  private fun bind(row: TextView, task: RecentTask, index: Int) {
    val name = task.name(this)
    row.text = name
    row.contentDescription = name
    row.setOnClickListener { switchTo(task) }
    (row.getTag(R.id.overview_row_delegate) as RowDelegate).set(task, index)
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

  private fun openAppInfo(task: RecentTask) {
    runCatching {
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
        info.collectionInfo = AccessibilityNodeInfo.CollectionInfo.obtain(tasks.size, 1, false)
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
    private var index = 0

    fun set(task: RecentTask, index: Int) {
      this.task = task
      this.index = index
    }

    @Suppress("DEPRECATION") // CollectionItemInfo's constructor needs Android 13.
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      task ?: return
      info.collectionItemInfo = AccessibilityNodeInfo.CollectionItemInfo.obtain(index, 1, 0, 1, false)
      info.addAction(AccessibilityAction(R.id.overview_close_action, getString(R.string.overview_close)))
      // The first task is the one a split goes beside.
      if (index > 0) {
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
      layoutParams =
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    }

  private fun button(@StringRes text: Int, onClick: () -> Unit): Button =
    Button(this).apply {
      setText(text)
      isAllCaps = false
      setOnClickListener { onClick() }
    }

  private fun dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
      .toInt()

  companion object {
    /**
     * Opens the overview in place of the system's, if the user has chosen it and Shizuku runs,
     * asking Shizuku to allow SandFly first if it doesn't. The system's overview is kept on the
     * lock screen, and opens when Shizuku doesn't allow SandFly.
     */
    @JvmStatic
    fun openInsteadOfSystem(context: Context): Boolean {
      if (!RecentTasks.isEnabled(context) || !ShizukuShell.isAvailable(context) || !ShizukuGrants.isReady()) {
        return false
      }
      if (context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
        return false
      }
      val appContext = context.applicationContext
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
