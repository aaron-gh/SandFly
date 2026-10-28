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
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notificationlist.NotificationReplyActivity.Companion.keepButtonCase
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.quicksettings.QuickSettingsActivity
import com.google.android.accessibility.utils.SharedPreferencesUtils

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

  /**
   * A row per notification or group key, kept so that a focused row stays focused when the list
   * changes.
   */
  private val rows = HashMap<String, TextView>()

  /** Controllers of the media sessions of media notifications, by session. */
  private val mediaControllers = HashMap<MediaSession.Token, MediaController>()

  /** Says Playing or Paused as soon as a session changes, before its app updates the notification. */
  private val playbackListener =
    object : MediaController.Callback() {
      override fun onPlaybackStateChanged(state: PlaybackState?) {
        show()
      }
    }

  /** The parts of a notification to speak, read when the list opens. */
  private var parts: List<NotificationPart> = NotificationPart.entries

  /** The groups the user has expanded while the list is open. */
  private val expandedGroups = HashSet<String>()
  /** Notifications whose long text the user chose to show in full. */
  private val expandedTexts = HashSet<String>()

  /** The view in the list that has accessibility focus, if one has. */
  private var focusedView: View? = null

  /** Whether the list has just opened, so focus goes to its first item. */
  private var opening = false

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
        val remaining =
          NotificationList.items(this).flatMap { it.entries }.filter { !it.notification.isClearable }
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
        if (!QuickSettingsActivity.openInsteadOfSystem(this)) {
          TalkBackService.getInstance()
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
        }
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
    opening = true
    parts = NotificationParts.enabledParts(SharedPreferencesUtils.getSharedPreferences(this))
    waitingForList = !NotificationList.isConnected
    if (NotificationList.isAccessGranted(this) && !NotificationList.isConnected) {
      NotificationListService.rebind(this)
    }
    show()
  }

  override fun onStop() {
    NotificationList.removeListener(this)
    mediaControllers.values.forEach { it.unregisterCallback(playbackListener) }
    mediaControllers.clear()
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
      val items = NotificationList.items(this)
      val keys = HashSet<String>()
      if (items.isEmpty()) {
        message.setText(R.string.notification_list_empty)
        views.add(message)
      }
      for (item in items) {
        when (item) {
          is NotificationItem.Single -> {
            keys.add(item.entry.key)
            views.add(row(item.entry.key).also { bind(it, item.entry, indented = false) })
          }
          is NotificationItem.Group -> {
            val groupKey = GROUP_PREFIX + item.key
            keys.add(groupKey)
            views.add(row(groupKey).also { bindGroup(it, item) })
            if (item.key in expandedGroups) {
              for (entry in item.entries) {
                keys.add(entry.key)
                views.add(row(entry.key).also { bind(it, entry, indented = true) })
              }
            }
          }
        }
      }
      rows.keys.retainAll(keys)
      expandedGroups.retainAll(items.filterIsInstance<NotificationItem.Group>().map { it.key }.toSet())
      expandedTexts.retainAll(keys)
      if (items.any { item -> item.entries.any { it.notification.isClearable } }) {
        views.add(clearAll)
      }
      if (waitingForList && NotificationList.isConnected) {
        // Opened before the notifications were known, so focus went elsewhere.
        waitingForList = false
        focusAfter = views.first()
      }
    }
    views.add(quickSettings)
    if (opening) {
      // Otherwise focus can start on whatever the screen reader focused last in this window.
      opening = false
      focusAfter = views.first()
    }
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

  private fun row(key: String): TextView = rows.getOrPut(key) { notificationRow() }

  private fun bind(row: TextView, entry: NotificationEntry, indented: Boolean) {
    val details = details(entry, playbackState(entry), full = entry.key in expandedTexts)
    row.text = details.joinToString("\n")
    row.contentDescription = details.joinToString(", ")
    row.setPaddingRelative(dp(if (indented) 40 else 16), row.paddingTop, row.paddingEnd, row.paddingBottom)
    row.setOnClickListener { open(entry) }
    (row.getTag(R.id.notification_list_row_delegate) as RowDelegate).set(entry, null)
  }

  /**
   * The details of the notification that the user chose, in their order, both spoken and shown.
   * The title, or the app if there is none, when every detail is turned off, so that the row still
   * says something. A long text is shortened unless [full].
   */
  private fun details(
    entry: NotificationEntry,
    playbackState: String?,
    full: Boolean,
  ): List<String> {
    val details =
      parts
        .mapNotNull { part ->
          when (part) {
            NotificationPart.TITLE -> entry.title
            NotificationPart.TEXT -> entry.text?.let { if (full) it else shortened(it) }
            NotificationPart.APP -> entry.appName
            NotificationPart.TIME -> entry.time
            NotificationPart.PLAYBACK -> playbackState
          }
        }
        .map { it.toString().trim() }
        .filter { it.isNotEmpty() }
        .distinct()
    return details.ifEmpty { listOf((entry.title ?: entry.appName).toString()) }
  }

  /** The start of a long [text], cut at a space where it can be, so that a row stays short. */
  private fun shortened(text: CharSequence): CharSequence {
    val trimmed = text.toString().trim()
    if (trimmed.length <= SHORT_TEXT_LENGTH) return trimmed
    val cut = trimmed.lastIndexOf(' ', SHORT_TEXT_LENGTH).takeIf { it > SHORT_TEXT_LENGTH / 2 }
    return trimmed.substring(0, cut ?: SHORT_TEXT_LENGTH).trimEnd() + "…"
  }

  private fun isLong(entry: NotificationEntry): Boolean =
    NotificationPart.TEXT in parts && (entry.text?.trim()?.length ?: 0) > SHORT_TEXT_LENGTH

  /** A group's row names the app, the number of notifications and the newest one. */
  private fun bindGroup(row: TextView, group: NotificationItem.Group) {
    val newest = group.entries.maxBy { it.timeMillis }
    val count =
      resources.getQuantityString(
        R.plurals.notification_list_group_count,
        group.entries.size,
        group.entries.size,
      )
    val details =
      listOfNotNull(group.appName, count, newest.title, newest.text?.let { shortened(it) })
        .map { it.toString().trim() }
        .filter { it.isNotEmpty() }
    row.text = details.joinToString("\n")
    row.contentDescription = details.joinToString(", ")
    row.setPaddingRelative(dp(16), row.paddingTop, row.paddingEnd, row.paddingBottom)
    row.setOnClickListener { toggle(group) }
    (row.getTag(R.id.notification_list_row_delegate) as RowDelegate).set(null, group)
  }

  private fun toggle(group: NotificationItem.Group) {
    if (!expandedGroups.add(group.key)) {
      expandedGroups.remove(group.key)
    }
    show()
  }

  /** "Playing" or "Paused" for a media notification whose session reports either. */
  private fun playbackState(entry: NotificationEntry): String? {
    val controller = mediaController(entry) ?: return null
    val state = runCatching { controller.playbackState?.state }.getOrNull()
    return when (state) {
      PlaybackState.STATE_PLAYING -> getString(R.string.notification_list_playing)
      PlaybackState.STATE_PAUSED,
      PlaybackState.STATE_STOPPED -> getString(R.string.notification_list_paused)
      else -> null
    }
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

  /**
   * Gives a notification's row its own actions, such as Reply, and Dismiss, and a group's row
   * Dismiss for all of the group. A group shows or hides its notifications on a double tap only: an
   * Expand action would make the screen reader say "Collapsed" on every group.
   */
  private inner class RowDelegate : View.AccessibilityDelegate() {
    private var entry: NotificationEntry? = null
    private var group: NotificationItem.Group? = null

    fun set(entry: NotificationEntry?, group: NotificationItem.Group?) {
      this.entry = entry
      this.group = group
    }

    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      group?.let { group ->
        if (group.entries.any { it.notification.isClearable }) {
          info.addAction(dismissAction())
        }
      }
      val entry = entry ?: return
      actions(entry).take(ACTION_IDS.size).forEachIndexed { index, action ->
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(ACTION_IDS[index], action.label))
      }
      if (entry.notification.isClearable) {
        info.addAction(dismissAction())
      }
      if (isLong(entry)) {
        info.addAction(
          AccessibilityNodeInfo.AccessibilityAction(
            R.id.notification_list_full_text_action,
            getString(
              if (entry.key in expandedTexts) R.string.notification_list_show_less
              else R.string.notification_list_show_all
            ),
          )
        )
      }
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
      group?.let { group ->
        when (action) {
          R.id.notification_list_dismiss_action -> {
            val listener = NotificationListService.instance
            (group.entries + listOfNotNull(group.summary))
              .filter { it.notification.isClearable }
              .forEach { listener?.cancelNotification(it.key) }
            return true
          }
        }
      }
      val entry = entry ?: return super.performAccessibilityAction(host, action, args)
      if (action == R.id.notification_list_dismiss_action) {
        NotificationListService.instance?.cancelNotification(entry.key)
        return true
      }
      if (action == R.id.notification_list_full_text_action) {
        if (!expandedTexts.add(entry.key)) {
          expandedTexts.remove(entry.key)
        }
        show()
        return true
      }
      val index = ACTION_IDS.indexOf(action)
      val rowAction = if (index >= 0) actions(entry).getOrNull(index) else null
      if (rowAction != null) {
        rowAction.run()
        return true
      }
      return super.performAccessibilityAction(host, action, args)
    }

    private fun dismissAction() =
      AccessibilityNodeInfo.AccessibilityAction(
        R.id.notification_list_dismiss_action,
        getString(R.string.notification_list_dismiss),
      )
  }

  /** An action on a notification's row. */
  private class RowAction(val label: CharSequence, val run: () -> Unit)

  /**
   * A media notification's actions control its media session, as the system's media player does.
   * Its own buttons are left out: they ask the app to start a service or send a broadcast, which
   * Android allows only when the system's shade sends them.
   */
  private fun actions(entry: NotificationEntry): List<RowAction> {
    val controller = mediaController(entry)
    if (controller != null) {
      return mediaActions(controller)
    }
    return entry.notification.notification.actions
      .orEmpty()
      .filter { !it.title.isNullOrBlank() }
      .map { action -> RowAction(action.title) { perform(action) } }
  }

  private fun mediaActions(controller: MediaController): List<RowAction> {
    val state = controller.playbackState
    val supported = state?.actions ?: 0L
    val controls = controller.transportControls
    val result = ArrayList<RowAction>()
    fun supports(action: Long) = supported and action != 0L
    if (supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
      result.add(RowAction(getString(R.string.notification_list_previous)) { controls.skipToPrevious() })
    }
    if (state?.state == PlaybackState.STATE_PLAYING) {
      if (supports(PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)) {
        result.add(RowAction(getString(R.string.notification_list_pause)) { controls.pause() })
      }
    } else if (supports(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PLAY_PAUSE)) {
      result.add(RowAction(getString(R.string.notification_list_play)) { controls.play() })
    }
    if (supports(PlaybackState.ACTION_SKIP_TO_NEXT)) {
      result.add(RowAction(getString(R.string.notification_list_next)) { controls.skipToNext() })
    }
    state?.customActions?.forEach { custom ->
      result.add(RowAction(custom.name) { controls.sendCustomAction(custom, custom.extras) })
    }
    return result
  }

  /** The controller of a media notification's session, kept while the list is open. */
  @Suppress("DEPRECATION") // getParcelable(String, Class) needs Android 13.
  private fun mediaController(entry: NotificationEntry): MediaController? {
    val token =
      entry.notification.notification.extras?.getParcelable<MediaSession.Token>(
        Notification.EXTRA_MEDIA_SESSION
      ) ?: return null
    mediaControllers[token]?.let {
      return it
    }
    // Android asks rows for their actions until the window is gone, after the list has stopped.
    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
      return null
    }
    val controller =
      runCatching { MediaController(this, token) }
        .onFailure { Log.w(TAG, "No media session", it) }
        .getOrNull() ?: return null
    controller.registerCallback(playbackListener)
    mediaControllers[token] = controller
    return controller
  }

  /** Performs one of a notification's own actions, asking for the text of a reply first. */
  private fun perform(action: Notification.Action) {
    val input = action.remoteInputs?.firstOrNull { it.allowFreeFormInput || !it.choices.isNullOrEmpty() }
    if (input == null) {
      NotificationReplyActivity.send(this, action.actionIntent, null)
      return
    }
    if (!input.allowFreeFormInput) {
      val choices = input.choices
      AlertDialog.Builder(this)
        .setTitle(input.label ?: action.title)
        .setItems(choices) { _, which ->
          NotificationReplyActivity.reply(this, action, input, choices[which])
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
        .keepButtonCase()
      return
    }
    // The text field is in its own process, so that the keyboard and Backtalk don't wait for
    // each other.
    startActivity(NotificationReplyActivity.intent(this, action))
  }

  /** Opens what the notification leads to, and removes it if it asks to be removed when opened. */
  private fun open(entry: NotificationEntry) {
    val notification = entry.notification.notification
    val intent = notification.contentIntent ?: return
    try {
      intent.send(this, 0, null, null, null, null, NotificationReplyActivity.startOptions())
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
    textRow().apply {
      val delegate = RowDelegate()
      accessibilityDelegate = delegate
      // Reading the delegate back from the view needs Android 10.
      setTag(R.id.notification_list_row_delegate, delegate)
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
      .recoverCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
      .onFailure { Log.w(TAG, "No notification access settings", it) }
  }

  companion object {
    private const val TAG = "NotificationList"

    /** How many characters of a notification's text a row shows until the user shows all. */
    private const val SHORT_TEXT_LENGTH = 160

    /** Keeps group keys apart from notification keys in the row map. */
    private const val GROUP_PREFIX = "group:"

    /** The accessibility actions that stand for a notification's own actions, in order. */
    private val ACTION_IDS =
      intArrayOf(
        R.id.notification_list_action_1,
        R.id.notification_list_action_2,
        R.id.notification_list_action_3,
        R.id.notification_list_action_4,
        R.id.notification_list_action_5,
        R.id.notification_list_action_6,
        R.id.notification_list_action_7,
        R.id.notification_list_action_8,
      )

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
