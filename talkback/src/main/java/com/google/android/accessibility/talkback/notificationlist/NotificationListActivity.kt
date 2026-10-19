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

package com.google.android.accessibility.talkback.notificationlist

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

/**
 * Backtalk's own list of notifications, opened by the Notifications action instead of the
 * system's shade. The list is Backtalk's, so moving through it does not wait for the system's
 * notification shade, which on some phones redraws all the time.
 *
 * Double tapping a notification opens it. Dismissing one is an accessibility action.
 */
class NotificationListActivity : AppCompatActivity(), NotificationList.Listener {
  private lateinit var column: LinearLayout
  private lateinit var message: TextView
  private lateinit var grantAccess: Button
  private lateinit var clearAll: Button
  private lateinit var quickSettings: Button

  /** A row per notification key, kept so that a focused row stays focused when the list changes. */
  private val rows = HashMap<String, TextView>()

  /** The view in the list that has accessibility focus, if one has. */
  private var focusedView: View? = null

  /** Whether the list opened before the listener had reported the notifications. */
  private var waitingForList = false

  /** Whether Clear all was used, so focus goes to the first notification that is left. */
  private var focusFirstAfterClear = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.notification_list_title)
    column =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        accessibilityDelegate = FocusTracker()
      }
    message = textRow()
    grantAccess = button(R.string.notification_list_grant_access) { openAccessSettings() }
    clearAll =
      button(R.string.notification_list_clear_all) {
        val remaining = NotificationList.entries(this).filter { !it.notification.isClearable }
        NotificationListService.instance?.cancelAllNotifications()
        // Like the system's shade, the list closes when nothing is left in it.
        if (remaining.isEmpty()) {
          finish()
        } else {
          focusFirstAfterClear = true
        }
      }
    quickSettings =
      button(R.string.notification_list_quick_settings) {
        TalkBackService.getInstance()
          ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
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
    NotificationList.addListener(this)
    waitingForList = !NotificationList.isConnected
    if (NotificationList.isAccessGranted(this) && !NotificationList.isConnected) {
      NotificationListenerService.requestRebind(
        ComponentName(this, NotificationListService::class.java)
      )
    }
    show()
  }

  override fun onStop() {
    NotificationList.removeListener(this)
    super.onStop()
    // Like the system's shade, the list closes when the user leaves it.
    if (!isChangingConfigurations) {
      finish()
    }
  }

  override fun onNotificationsChanged() {
    show()
  }

  private fun show() {
    val views = ArrayList<View>()
    var focusAfter: View? = null
    if (!NotificationList.isAccessGranted(this)) {
      message.setText(R.string.notification_list_no_access)
      views.add(message)
      views.add(grantAccess)
    } else {
      val entries = NotificationList.entries(this)
      val keys = entries.map { it.key }.toSet()
      if (entries.isEmpty()) {
        message.setText(R.string.notification_list_empty)
        views.add(message)
      }
      for (entry in entries) {
        val row = rows.getOrPut(entry.key) { notificationRow() }
        bind(row, entry)
        views.add(row)
      }
      rows.keys.retainAll(keys)
      if (entries.any { it.notification.isClearable }) {
        views.add(clearAll)
      }
      if (waitingForList && NotificationList.isConnected) {
        // Opened before the notifications were known, so focus went elsewhere.
        waitingForList = false
        focusAfter = views.first()
      }
    }
    views.add(quickSettings)
    if (focusFirstAfterClear && views.first() !== clearAll) {
      focusFirstAfterClear = false
      focusedView = null
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

  private fun bind(row: TextView, entry: NotificationEntry) {
    row.text =
      listOfNotNull(entry.title, entry.text, "${entry.appName} · ${entry.time}").joinToString("\n")
    row.contentDescription = entry.description
    row.setOnClickListener { open(entry) }
    (row.accessibilityDelegate as RowDelegate).entry = entry
  }

  /** Notes which view in the list has accessibility focus. */
  private inner class FocusTracker : View.AccessibilityDelegate() {
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

  /** Gives a row its notification's actions. */
  private inner class RowDelegate : View.AccessibilityDelegate() {
    var entry: NotificationEntry? = null

    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      if (entry?.notification?.isClearable == true) {
        info.addAction(
          AccessibilityNodeInfo.AccessibilityAction(
            R.id.notification_list_dismiss_action,
            getString(R.string.notification_list_dismiss),
          )
        )
      }
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
      val entry = entry
      if (entry != null && action == R.id.notification_list_dismiss_action) {
        NotificationListService.instance?.cancelNotification(entry.key)
        return true
      }
      return super.performAccessibilityAction(host, action, args)
    }
  }

  /** Opens what the notification leads to, and removes it if it asks to be removed when opened. */
  private fun open(entry: NotificationEntry) {
    val notification = entry.notification.notification
    val intent = notification.contentIntent ?: return
    try {
      intent.send(this, 0, null, null, null, null, startOptions())
    } catch (e: PendingIntent.CanceledException) {
      Log.w(TAG, "Notification can't be opened", e)
      return
    }
    if (notification.flags and Notification.FLAG_AUTO_CANCEL != 0) {
      NotificationListService.instance?.cancelNotification(entry.key)
    }
    finish()
  }

  /** Lets the notification start its app from this list, as it could from the system's shade. */
  @Suppress("DEPRECATION") // MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS needs Android 16.
  private fun startOptions(): Bundle? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      return null
    }
    return ActivityOptions.makeBasic()
      .setPendingIntentBackgroundActivityStartMode(
        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
      )
      .toBundle()
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

  private fun notificationRow(): TextView =
    textRow().apply { accessibilityDelegate = RowDelegate() }

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
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics,
      )
      .toInt()

  private fun openAccessSettings() {
    val component = ComponentName(this, NotificationListService::class.java)
    val intent =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
          .putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            component.flattenToString(),
          )
      } else {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
      }
    runCatching { startActivity(intent) }
      .onFailure { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
  }

  companion object {
    private const val TAG = "NotificationList"

    /**
     * Opens the list in place of the system's shade, if the user has chosen it. The system's shade
     * is kept on the lock screen, where it hides what the user chose to hide there.
     */
    @JvmStatic
    fun openInsteadOfShade(context: Context): Boolean {
      if (!NotificationList.isEnabled(context)) {
        return false
      }
      if (context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
        return false
      }
      context.startActivity(
        Intent(context, NotificationListActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      )
      return true
    }
  }
}
