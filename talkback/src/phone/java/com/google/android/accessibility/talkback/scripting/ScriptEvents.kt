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

import android.app.Notification
import android.os.Build
import android.os.Handler
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject

class ScriptEventDelivery(
  private val handler: Handler,
  private val loaded: () -> Collection<ScriptRuntime>,
) {
  @Volatile private var hooks: Set<String> = emptySet()
  private val queued = AtomicInteger()
  private val pendingContent = LinkedHashMap<String, AccessibilityEvent>()
  private val flushContent = Runnable { flushContentChanges() }
  private var flushScheduled = false
  private var pendingScroll: AccessibilityEvent? = null
  private val flushScroll = Runnable { flushScrolls() }

  fun update(runtimes: List<ScriptRuntime>) {
    hooks = runtimes.flatMap { it.hooks }.filter { it in HOOKS }.toSet()
  }

  fun clear() {
    hooks = emptySet()
    handler.removeCallbacks(flushContent)
    handler.removeCallbacks(flushScroll)
    pendingContent.clear()
    pendingScroll = null
    flushScheduled = false
  }

  fun deliver(event: AccessibilityEvent) {
    val hooks = hooks
    if ("event" in hooks) {
      if (queued.incrementAndGet() <= MAX_QUEUED) {
        val data = event.rawJson()
        val password = event.isPassword
        handler.post {
          queued.decrementAndGet()
          deliverRaw(data, password)
        }
      } else {
        queued.decrementAndGet()
      }
    }
    when (event.eventType) {
      AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED if "textChange" in hooks -> {
        val copy = copyOf(event)
        handler.post { deliverText(copy) }
      }
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED if "contentChange" in hooks -> {
        val copy = copyOf(event)
        handler.post { queueContent(copy) }
      }
      AccessibilityEvent.TYPE_VIEW_SCROLLED if "scroll" in hooks -> {
        val copy = copyOf(event)
        handler.post { queueScroll(copy) }
      }
      AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED if "selectionChange" in hooks -> {
        val copy = copyOf(event)
        handler.post { deliverSelection(copy) }
      }
      AccessibilityEvent.TYPE_VIEW_CLICKED if "click" in hooks -> deliverPlain("click", event)
      AccessibilityEvent.TYPE_VIEW_LONG_CLICKED if "longClick" in hooks ->
        deliverPlain("longClick", event)
      AccessibilityEvent.TYPE_VIEW_FOCUSED if "inputFocus" in hooks ->
        deliverPlain("inputFocus", event)
    }
  }

  private fun deliverRaw(data: JSONObject, password: Boolean) {
    val listeners =
      loaded().filter { "event" in it.hooks && it.allowed(ScriptPermission.EVENTS, "onEvent") }
    for (runtime in listeners) {
      val forScript =
        if (runtime.hidesPassword(password)) {
          JSONObject(data.toString()).apply {
            put("text", JSONObject.NULL)
            put("beforeText", JSONObject.NULL)
          }
        } else {
          data
        }
      runtime.dispatch("event", forScript)
    }
  }

  private fun deliverText(event: AccessibilityEvent) =
    deliverToListeners("textChange", event) { runtime, node ->
      val hidden = runtime.hidesPassword(event.isPassword)
      jsonObject(
        "package" to event.packageName?.toString(),
        "node" to runtime.snapshotIfAllowed(node),
        "text" to event.scriptText().takeUnless { hidden },
        "before" to event.beforeText?.toString().takeUnless { hidden },
        "from" to event.fromIndex,
        "added" to event.addedCount,
        "removed" to event.removedCount,
        "password" to event.isPassword,
      )
    }

  private fun deliverPlain(hook: String, event: AccessibilityEvent) {
    val copy = copyOf(event)
    handler.post {
      deliverToListeners(hook, copy) { runtime, node ->
        jsonObject(
          "package" to copy.packageName?.toString(),
          "node" to runtime.snapshotIfAllowed(node),
        )
      }
    }
  }

  private fun deliverSelection(event: AccessibilityEvent) =
    deliverToListeners("selectionChange", event) { runtime, node ->
      val hidden = runtime.hidesPassword(event.isPassword)
      jsonObject(
        "package" to event.packageName?.toString(),
        "node" to runtime.snapshotIfAllowed(node),
        "text" to event.scriptText().takeUnless { hidden },
        "start" to event.fromIndex,
        "end" to event.toIndex,
        "length" to event.itemCount,
        "password" to event.isPassword,
      )
    }

  // A fling sends many scroll events, so each script hears of the last one in a short while.
  private fun queueScroll(event: AccessibilityEvent) {
    val pending = pendingScroll
    pendingScroll = event
    if (pending == null) {
      handler.postDelayed(flushScroll, SCROLL_DELAY_MS)
    }
  }

  private fun flushScrolls() {
    val event = pendingScroll ?: return
    pendingScroll = null
    deliverToListeners("scroll", event) { runtime, node ->
      jsonObject(
        "package" to event.packageName?.toString(),
        "node" to runtime.snapshotIfAllowed(node),
        "from" to event.fromIndex.takeIf { it >= 0 }?.plus(1),
        "to" to event.toIndex.takeIf { it >= 0 }?.plus(1),
        "count" to event.itemCount.takeIf { it >= 0 },
        "scrollX" to event.scrollX,
        "scrollY" to event.scrollY,
        "deltaX" to if (DELTAS) event.scrollDeltaX else null,
        "deltaY" to if (DELTAS) event.scrollDeltaY else null,
      )
    }
  }

  private fun queueContent(event: AccessibilityEvent) {
    val packageName = event.packageName?.toString() ?: return
    pendingContent.remove(packageName)
    pendingContent[packageName] = event
    if (!flushScheduled) {
      flushScheduled = true
      handler.postDelayed(flushContent, CONTENT_DELAY_MS)
    }
  }

  private fun flushContentChanges() {
    flushScheduled = false
    val events = pendingContent.values.toList().also { pendingContent.clear() }
    for (event in events) {
      deliverToListeners("contentChange", event) { runtime, node ->
        jsonObject(
          "package" to event.packageName?.toString(),
          "node" to runtime.snapshotIfAllowed(node),
          "changeTypes" to event.contentChangeTypes,
        )
      }
    }
  }

  private fun deliverToListeners(
    hook: String,
    event: AccessibilityEvent,
    data: (ScriptRuntime, AccessibilityNodeInfoCompat?) -> JSONObject,
  ) {
    val packageName = event.packageName?.toString()
    val listeners =
      loaded().filter {
        hook in it.hooks &&
          it.appliesTo(packageName) &&
          it.allowed(ScriptPermission.SCREEN, ScriptRuntime.handlerName(hook))
      }
    if (listeners.isEmpty()) {
      return
    }
    val node = event.source?.let(AccessibilityNodeInfoCompat::wrap)
    listeners.forEach { it.dispatch(hook, data(it, node)) }
  }

  private companion object {
    const val CONTENT_DELAY_MS = 200L
    const val SCROLL_DELAY_MS = 150L
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val DELTAS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    const val MAX_QUEUED = 100
    val HOOKS =
      setOf(
        "event",
        "textChange",
        "contentChange",
        "scroll",
        "selectionChange",
        "click",
        "longClick",
        "inputFocus",
      )
  }
}

fun AccessibilityEvent.scriptText(): String? =
  text.filterNotNull().joinToString(" ").ifEmpty { null }

fun AccessibilityEvent.notificationJson(): JSONObject {
  val notification = parcelableData as? Notification
  val extras = notification?.extras
  return jsonObject(
    "package" to packageName?.toString(),
    "toast" to (notification == null),
    "title" to extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
    "text" to (extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: scriptText()),
    "category" to notification?.category,
  )
}

private fun AccessibilityEvent.rawJson(): JSONObject =
  jsonObject(
      "type" to typeName(eventType),
      "package" to packageName?.toString(),
      "className" to className?.toString(),
      "text" to scriptText(),
      "contentDescription" to contentDescription?.toString(),
      "beforeText" to beforeText?.toString(),
      "password" to isPassword,
      "windowId" to windowId,
      "time" to eventTime,
    )
    .apply {
      when (eventType) {
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> put("changeTypes", contentChangeTypes)
        AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
          put("notification", notificationJson())
      }
    }

private fun typeName(type: Int): String =
  AccessibilityEvent.eventTypeToString(type)
    .removePrefix("TYPE_")
    .lowercase()
    .split('_')
    .let { parts ->
      parts.first() + parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercase) }
    }

@Suppress("DEPRECATION")
private fun copyOf(event: AccessibilityEvent): AccessibilityEvent = AccessibilityEvent.obtain(event)
