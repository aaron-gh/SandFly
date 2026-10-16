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

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.TraversalTreeCache
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Runs the script engine only while there is something for it to do. The native library, the
 * script thread and the work on every window change cost nothing for people who use no scripts,
 * and scripts do not run, keep timers or use the network while the screen is off or Backtalk is
 * paused.
 */
class LazyScriptHost(
  private val service: AccessibilityService,
  private val feedback: Pipeline.FeedbackReturner,
  private val resume: Runnable,
) : ScriptHost, ScriptStore.Listener {
  private val store = ScriptStore.get(service)
  // Outlives the engine, so that a restart still knows which activity is in front.
  private val activation = ScriptActivation(service)
  @Volatile private var manager: ScriptManager? = null
  private var paused = false
  private var screenOn = service.getSystemService(PowerManager::class.java)?.isInteractive != false
  private var unavailable = false
  private var closed = false

  private val screenReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        screenOn = intent.action != Intent.ACTION_SCREEN_OFF
        update()
      }
    }

  init {
    store.addListener(this)
    val screenChanges =
      IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
      }
    ContextCompat.registerReceiver(
      service,
      screenReceiver,
      screenChanges,
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    update()
  }

  override fun onScriptsChanged() = update()

  override fun onPausedChanged(paused: Boolean) {
    this.paused = paused
    update()
  }

  override fun toggleAll(): Boolean? {
    if (unavailable) return null
    store.allOff = !store.allOff
    update()
    return !store.allOff
  }

  override fun shutdown() {
    closed = true
    store.removeListener(this)
    service.unregisterReceiver(screenReceiver)
    update()
  }

  private fun update() {
    val wanted =
      !closed && !paused && screenOn && !unavailable && !store.allOff && store.anyEnabled()
    val running = manager
    if (wanted && running == null) {
      start()
    } else if (!wanted && running != null) {
      manager = null
      running.shutdown()
      TraversalTreeCache.clear("scripts stopped")
    }
  }

  private fun start() {
    try {
      System.loadLibrary("backtalkquickjs")
      manager = ScriptManager(service, ScriptFeedback(service, feedback, resume), activation)
    } catch (e: UnsatisfiedLinkError) {
      LogUtils.e(TAG, "Scripts are unavailable: %s", e)
      unavailable = true
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent) {
    val running = manager
    if (running != null) {
      running.onAccessibilityEvent(event)
    } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      activation.onWindowStateChanged(event)
    }
  }

  override fun rewriteFocusSpeech(
    node: AccessibilityNodeInfoCompat,
    speech: CharSequence,
  ): CharSequence? = manager?.rewriteFocusSpeech(node, speech)

  override fun rewriteEventSpeech(event: AccessibilityEvent, speech: CharSequence): CharSequence? =
    manager?.rewriteEventSpeech(event, speech)

  override fun filterSpeech(speech: CharSequence): CharSequence? = manager?.filterSpeech(speech)

  override fun rule(node: AccessibilityNodeInfoCompat, aspect: RuleAspect): NodeRule? =
    manager?.rule(node, aspect)

  override fun isHidden(node: AccessibilityNodeInfoCompat): Boolean =
    manager?.isHidden(node) == true

  override fun currentActivity(): String? = manager?.currentActivity()

  override fun currentWindowTitle(): String? = manager?.currentWindowTitle()

  override fun onGesture(name: String, fallback: Runnable): Boolean =
    manager?.onGesture(name, fallback) == true

  override fun onKeys(
    modifiers: Int,
    keyCode: Int,
    withBacktalkModifier: Boolean,
    fallback: Runnable,
  ): Boolean = manager?.onKeys(modifiers, keyCode, withBacktalkModifier, fallback) == true

  override fun menuItems(): List<ScriptMenuItem> = manager?.menuItems().orEmpty()

  override fun readingControls(): List<ScriptReadingControl> =
    manager?.readingControls().orEmpty()

  override fun itemActions(node: AccessibilityNodeInfoCompat): List<ScriptItemAction> =
    manager?.itemActions(node).orEmpty()

  private companion object {
    const val TAG = "LazyScriptHost"
  }
}
