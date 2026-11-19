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

package com.google.android.accessibility.talkback.scripting

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.PerformActionUtils
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import org.json.JSONObject

/**
 * Records an action of a built script's rule as the user does it: each item pressed or
 * long-pressed, with SandFly or by touch, becomes a step. The first press on an item the rule names
 * makes it the action's item; later presses in it are found in the item, and others, such as menu
 * entries, on screen. Steps are kept as queries that find the same item in any item like it.
 */
object ScriptActionRecorder {
  /** What is being recorded: the action [title] of rule [ruleIndex] of the script [scriptId]. */
  class Recording(val scriptId: String, val ruleIndex: Int, val title: String, val query: NodeQuery) {
    var item: AccessibilityNodeInfoCompat? = null
    var row: JSONObject? = null
    val steps = mutableListOf<JSONObject>()
    var lastNode: AccessibilityNodeInfoCompat? = null
    var lastTime = 0L
  }

  @Volatile private var recording: Recording? = null

  private val listener =
    PerformActionUtils.ActionListener { node, action -> record(node, action, fromSandFly = true) }

  /** Starts recording, ending any recording before it, and shows the notification that ends it. */
  fun start(context: Context, scriptId: String, ruleIndex: Int, title: String, match: JSONObject) {
    val query = NodeQuery.of(match)
    recording = Recording(scriptId, ruleIndex, title, query)
    PerformActionUtils.addActionListener(listener)
    notify(context, scriptId, ruleIndex, title)
  }

  /** The recording for rule [ruleIndex] of [scriptId], or null when that isn't being recorded. */
  fun recordingFor(scriptId: String, ruleIndex: Int): Recording? =
    recording?.takeIf { it.scriptId == scriptId && it.ruleIndex == ruleIndex }

  /** Ends the recording, and gives its steps. */
  fun stop(context: Context): List<JSONObject> {
    val ended = recording
    recording = null
    PerformActionUtils.removeActionListener(listener)
    context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    return ended?.let { synchronized(it) { it.steps.toList() } }.orEmpty()
  }

  /** A press made by touch, which the app reports. Presses SandFly made are recorded already. */
  fun onEvent(event: AccessibilityEvent) {
    if (recording == null) return
    val action =
      when (event.eventType) {
        AccessibilityEvent.TYPE_VIEW_CLICKED -> AccessibilityNodeInfo.ACTION_CLICK
        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> AccessibilityNodeInfo.ACTION_LONG_CLICK
        else -> return
      }
    val source = event.source ?: return
    record(AccessibilityNodeInfoCompat.wrap(source), action, fromSandFly = false)
  }

  private fun record(node: AccessibilityNodeInfoCompat, action: Int, fromSandFly: Boolean) {
    if (action != AccessibilityNodeInfo.ACTION_CLICK && action != AccessibilityNodeInfo.ACTION_LONG_CLICK) return
    val current = recording ?: return
    synchronized(current) {
      val now = SystemClock.uptimeMillis()
      // The app reports a press SandFly made too, on the same item or one around it.
      if (!fromSandFly && now - current.lastTime < SAME_PRESS_MS) return
      if (node == current.lastNode && now - current.lastTime < SAME_PRESS_MS) return
      var item = current.item
      if (item == null) {
        item = (sequenceOf(node) + node.ancestors().take(MAX_ITEM_LEVELS)).firstOrNull { current.query.matches(it) } ?: return
        current.item = item
        current.row = ItemInspector.snapshot(item)
      } else if (node.packageName != item.packageName) {
        return
      }
      val inItem = node == item || node.ancestors().take(MAX_LEVELS).any { it == item }
      val target =
        if (node == item) JSONObject()
        else ItemPattern.target(current.row, ItemInspector.snapshot(node, TARGET_DEPTH)) ?: return
      // A long press by touch, or SandFly's double-tap and hold, which passes a held touch to the
      // app, is kept as a held touch: some apps act only on that.
      val verb =
        when {
          action != AccessibilityNodeInfo.ACTION_LONG_CLICK -> "click"
          fromSandFly -> "longClick"
          else -> "touchHold"
        }
      current.steps += JSONObject().put(verb, target).put("where", if (inItem) "item" else "screen")
      current.lastNode = node
      current.lastTime = now
    }
  }

  private fun notify(context: Context, scriptId: String, ruleIndex: Int, title: String) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val open =
      Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(BasePreferencesActivity.FRAGMENT_NAME, ScriptRuleFragment::class.java.name)
        .putExtra(
          BasePreferencesActivity.FRAGMENT_ARGS,
          Bundle().apply {
            putString(ScriptScreenFragment.ARG_ID, scriptId)
            putInt(ScriptRuleFragment.ARG_INDEX, ruleIndex)
          },
        )
    val pending =
      PendingIntent.getActivity(context, NOTIFICATION_ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val heading = context.getString(R.string.script_builder_recording_notification, title)
    val text = context.getString(R.string.script_builder_recording_notification_text)
    try {
      manager.notify(NOTIFICATION_ID, NotificationUtils.createNotification(context, heading, heading, text, pending))
    } catch (e: SecurityException) {
      // Notifications aren't allowed; the builder ends the recording when it is opened again.
    }
  }

  private const val NOTIFICATION_ID = 0x5f_ac70
  private const val SAME_PRESS_MS = 600L
  /** How far up from a pressed item the item the rule names is looked for. */
  private const val MAX_ITEM_LEVELS = 8
  private const val MAX_LEVELS = 30
  private const val TARGET_DEPTH = 2
}
