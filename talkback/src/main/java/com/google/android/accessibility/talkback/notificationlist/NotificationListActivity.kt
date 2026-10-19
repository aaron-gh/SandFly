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
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService

/**
 * Backtalk's own list of notifications, opened by the Notifications action instead of the
 * system's shade. The list is Backtalk's, so moving through it does not wait for the system's
 * notification shade, which on some phones redraws all the time.
 */
class NotificationListActivity : AppCompatActivity(), NotificationList.Listener {
  private lateinit var column: LinearLayout
  private lateinit var message: TextView
  private lateinit var grantAccess: Button
  private lateinit var quickSettings: Button

  /** A row per notification key, kept so that a focused row stays focused when the list changes. */
  private val rows = HashMap<String, TextView>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.notification_list_title)
    column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    message = textRow()
    grantAccess =
      Button(this).apply {
        setText(R.string.notification_list_grant_access)
        setOnClickListener { openAccessSettings() }
      }
    quickSettings =
      Button(this).apply {
        setText(R.string.notification_list_quick_settings)
        setOnClickListener {
          TalkBackService.getInstance()
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
          finish()
        }
      }
    val scroll = ScrollView(this).apply { addView(column) }
    ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
      val bars =
        insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      insets
    }
    setContentView(scroll)
  }

  override fun onStart() {
    super.onStart()
    NotificationList.addListener(this)
    if (NotificationList.isAccessGranted(this) && !NotificationList.isConnected) {
      NotificationListenerService.requestRebind(ComponentName(this, NotificationListService::class.java))
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
    if (!NotificationList.isAccessGranted(this)) {
      message.setText(R.string.notification_list_no_access)
      views.add(message)
      views.add(grantAccess)
    } else {
      val entries = NotificationList.entries(this)
      if (entries.isEmpty()) {
        message.setText(R.string.notification_list_empty)
        views.add(message)
      }
      for (entry in entries) {
        val row = rows.getOrPut(entry.key) { textRow() }
        row.text = listOfNotNull(entry.title, entry.text, "${entry.appName} · ${entry.time}").joinToString("\n")
        row.contentDescription = entry.description
        views.add(row)
      }
      rows.keys.retainAll(entries.map { it.key }.toSet())
    }
    views.add(quickSettings)
    setChildren(views)
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

  private fun dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
      .toInt()

  private fun openAccessSettings() {
    val component = ComponentName(this, NotificationListService::class.java)
    val intent =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
          .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
      } else {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
      }
    runCatching { startActivity(intent) }
      .onFailure { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
  }

  companion object {
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
