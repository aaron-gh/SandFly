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

package com.google.android.accessibility.talkback.scripting

import android.os.Handler
import android.os.SystemClock
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import org.json.JSONObject

class ScriptItemActions(private val handler: Handler, private val feedback: ScriptFeedback) {
  private class Offered(
    val node: AccessibilityNodeInfoCompat,
    val time: Long,
    val actions: List<ScriptItemAction>,
  )

  @Volatile private var scripts: List<ScriptRuntime> = emptyList()
  @Volatile private var offered: Offered? = null

  fun update(runtimes: List<ScriptRuntime>) {
    scripts =
      runtimes.filter {
        HOOK in it.hooks && it.allowed(ScriptPermission.INPUT, ScriptRuntime.handlerName(HOOK))
      }
    offered = null
  }

  fun clear() {
    scripts = emptyList()
    offered = null
  }

  fun of(node: AccessibilityNodeInfoCompat, rules: ScriptRules): List<ScriptItemAction> {
    val now = SystemClock.uptimeMillis()
    offered?.takeIf { it.node == node && now - it.time < REUSE_MS }?.let { return it.actions }
    return (fromRules(node, rules) + fromScripts(node)).also { offered = Offered(node, now, it) }
  }

  private fun fromRules(
    node: AccessibilityNodeInfoCompat,
    rules: ScriptRules,
  ): List<ScriptItemAction> =
    rules
      .actions(node)
      .filter { (runtime, action) ->
        action.permission?.let { runtime.allowed(it, "The action ${action.title}") } != false
      }
      .map { (runtime, action) ->
        @Suppress("DEPRECATION") val copy = AccessibilityNodeInfoCompat.obtain(node)
        // Finding the action's target can visit every item on screen.
        ScriptItemAction(action.title) {
          handler.post { ScriptThread.busy { run(runtime, action, copy) } }
        }
      }

  private fun fromScripts(node: AccessibilityNodeInfoCompat): List<ScriptItemAction> {
    val chain = scripts.ifEmpty { return emptyList() }
    if (ScriptThread.heldUpFor(WAIT_MS)) return emptyList()
    @Suppress("DEPRECATION") val copy = AccessibilityNodeInfoCompat.obtain(node)
    return handler
      .await(WAIT_MS, HOOK) { chain.filter { it.isLoaded }.flatMap { offer(it, copy) } }
      .orEmpty()
  }

  private fun offer(
    runtime: ScriptRuntime,
    node: AccessibilityNodeInfoCompat,
  ): List<ScriptItemAction> {
    val data = jsonObject("node" to runtime.snapshotIfAllowed(node))
    val result = runtime.dispatch(HOOK, data, LIMIT_MS)?.let(::JSONObject) ?: return emptyList()
    val offer = result.optInt("offer")
    return result.optJSONArray("titles").strings().map { title ->
      val chosen = jsonObject("offer" to offer, "title" to title)
      ScriptItemAction(title) { handler.post { runtime.dispatch("runAction", chosen) } }
    }
  }

  private fun run(
    runtime: ScriptRuntime,
    action: RuleItemAction,
    node: AccessibilityNodeInfoCompat,
  ) {
    val target = action.target?.let { find(node, it) ?: return missing(runtime, action) }
    when (action.verb) {
      RuleItemAction.Verb.SPEAK -> {
        val text = action.text ?: target?.let { runtime.textOf(it) ?: it.contentDescription }
        feedback.speak(text?.toString().orEmpty(), interrupt = true)
      }
      RuleItemAction.Verb.FOCUS -> target?.let { feedback.focus(it) {} }
      else -> target?.performAction(NodeBridge.ACTIONS.getValue(action.verb.key))
    }
  }

  private fun missing(runtime: ScriptRuntime, action: RuleItemAction) {
    ScriptLog.add(runtime.id, ScriptLog.Level.WARN, "${action.title}: found no ${action.target}")
    feedback.playSound(R.raw.complete)
  }

  private fun find(node: AccessibilityNodeInfoCompat, query: NodeQuery): AccessibilityNodeInfoCompat? {
    val deadline = SystemClock.uptimeMillis() + FIND_LIMIT_MS
    return query.firstIn(node, deadline)
      ?: AccessibilityNodeInfoUtils.getRoot(node)?.let { query.firstIn(it, deadline) }
  }

  private companion object {
    const val HOOK = "actions"
    const val WAIT_MS = 50L
    const val LIMIT_MS = 250L
    const val REUSE_MS = 1000L
    const val FIND_LIMIT_MS = 1000L
  }
}
