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

import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.NodeOverrides
import com.google.android.accessibility.utils.output.SpeechControllerImpl
import com.google.android.accessibility.utils.output.SpeechControllerImpl.SpeechFilter

interface ScriptHost {
  fun onAccessibilityEvent(event: AccessibilityEvent)

  fun rewriteFocusSpeech(node: AccessibilityNodeInfoCompat, speech: CharSequence): CharSequence?

  fun rewriteEventSpeech(event: AccessibilityEvent, speech: CharSequence): CharSequence?

  fun filterSpeech(speech: CharSequence): CharSequence?

  fun rule(node: AccessibilityNodeInfoCompat, aspect: RuleAspect): NodeRule?

  fun isHidden(node: AccessibilityNodeInfoCompat): Boolean

  fun isSilenced(node: AccessibilityNodeInfoCompat): Boolean

  fun currentActivity(): String?

  fun currentWindowTitle(): String?

  fun onPausedChanged(paused: Boolean)

  fun onGesture(name: String, fallback: Runnable): Boolean

  fun onKeys(
    modifiers: Int,
    keyCode: Int,
    withBacktalkModifier: Boolean,
    fallback: Runnable,
  ): Boolean

  fun menuItems(): List<ScriptMenuItem>

  fun readingControls(): List<ScriptReadingControl>

  fun itemActions(node: AccessibilityNodeInfoCompat): List<ScriptItemAction>

  /**
   * Turns every script off, or back on, without changing each script's own switch. Returns whether
   * scripts are now on, or null if this device cannot run them.
   */
  fun toggleAll(): Boolean?

  fun shutdown()
}

enum class RuleAspect {
  ANY,
  LABEL,
  SPEAK,
  ROLE,
  STATE,
  HINT,
  HEADING,
  GROUP,
  ORDER,
  ACTIVATE,
  SILENCE,
  LIFT,
}

class NodeRule(
  val label: CharSequence?,
  val speak: CharSequence?,
  val hide: Boolean,
  val hideInside: Boolean,
  val role: Int?,
  val state: CharSequence?,
  val hint: CharSequence?,
  val heading: Boolean?,
  val group: Boolean,
  val order: NodeOverrides.Order?,
  val sources: List<String>,
  /** The item inside this one that a double-tap presses, if a rule chose one. */
  val activate: ((AccessibilityNodeInfoCompat) -> AccessibilityNodeInfoCompat?)? = null,
  val silence: Boolean = false,
  val silenceInside: Boolean = false,
  /** Whether lifting the finger from the item activates it. */
  val liftToActivate: Boolean = false,
  /** How many milliseconds a finger held on the item takes to long-press it, if a rule says. */
  val longPressAfter: Int? = null,
) {
  val hidesDescendants: Boolean
    get() = hideInside || group
}

class ScriptMenuItem(
  val title: String,
  private val action: (AccessibilityNodeInfoCompat?) -> Unit,
) {
  fun run(node: AccessibilityNodeInfoCompat?) = action(node)
}

class ScriptItemAction(val title: String, private val action: () -> Unit) {
  fun run() = action()
}

class ScriptReadingControl(
  val key: String,
  val title: String,
  private val adjuster: (Boolean) -> Unit,
) {
  fun adjust(isNext: Boolean) = adjuster(isNext)
}

object Scripts {
  @Volatile private var host: ScriptHost? = null
  @Volatile private var capture: ((String) -> Unit)? = null

  @JvmStatic
  fun setHost(newHost: ScriptHost?) {
    host = newHost
    NodeOverrides.set(newHost?.let(::RuleOverrides))
    SpeechControllerImpl.setSpeechFilter(newHost?.let { SpeechFilter(it::filterSpeech) })
  }

  @JvmStatic
  fun rewriteSpeech(
    eventType: Int,
    event: AccessibilityEvent?,
    node: AccessibilityNodeInfoCompat?,
    speech: CharSequence,
  ): CharSequence? {
    val current = host ?: return null
    return when (eventType) {
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
        node?.let { current.rewriteFocusSpeech(it, speech) }
      AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED,
      AccessibilityEvent.TYPE_ANNOUNCEMENT -> event?.let { current.rewriteEventSpeech(it, speech) }
      else -> null
    }
  }

  /**
   * Whether a script rule silences {@code node}, so that its changes, selections and announcements
   * are not spoken.
   */
  @JvmStatic
  fun isSilenced(node: AccessibilityNodeInfoCompat?): Boolean =
    node != null && host?.isSilenced(node) == true

  /** Whether a script rule has lifting the finger from {@code node} activate it. */
  @JvmStatic
  fun liftsToActivate(node: AccessibilityNodeInfoCompat?): Boolean =
    node != null && rule(node, RuleAspect.LIFT)?.liftToActivate == true

  /**
   * How many milliseconds a finger held on {@code node} takes to long-press it, or 0 if no script
   * rule has the node lift to activate and long-press.
   */
  @JvmStatic
  fun longPressAfterMs(node: AccessibilityNodeInfoCompat?): Int =
    node?.let { rule(it, RuleAspect.LIFT) }?.takeIf { it.liftToActivate }?.longPressAfter ?: 0

  @JvmStatic fun currentActivity(): String? = host?.currentActivity()

  @JvmStatic fun currentWindowTitle(): String? = host?.currentWindowTitle()

  fun rule(node: AccessibilityNodeInfoCompat, aspect: RuleAspect = RuleAspect.ANY): NodeRule? =
    host?.rule(node, aspect)

  @JvmStatic
  fun ruleLabel(node: AccessibilityNodeInfoCompat): CharSequence? =
    rule(node, RuleAspect.LABEL)?.label

  @JvmStatic
  fun ruleHint(node: AccessibilityNodeInfoCompat): CharSequence? = rule(node, RuleAspect.HINT)?.hint

  @JvmStatic
  fun groupItems(node: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat>? =
    group(node)?.let { if (it.label != null) emptyList() else ScriptGroups.items(node, ::rule) }

  @JvmStatic
  fun actionTarget(
    node: AccessibilityNodeInfoCompat,
    longClick: Boolean,
  ): AccessibilityNodeInfoCompat {
    if (!longClick) {
      rule(node, RuleAspect.ACTIVATE)?.activate?.invoke(node)?.let {
        return it
      }
    }
    return if (group(node) != null) ScriptGroups.actionTarget(node, longClick) else node
  }

  @JvmStatic
  fun onGesture(gestureId: Int, fallback: Runnable): Boolean =
    offerGesture(ScriptInput.gestureName(gestureId), fallback)

  @JvmStatic
  fun onFingerprintGesture(gestureId: Int, fallback: Runnable): Boolean =
    offerGesture(ScriptInput.fingerprintGestureName(gestureId), fallback)

  @JvmStatic
  fun onKeys(
    modifiers: Int,
    keyCode: Int,
    withBacktalkModifier: Boolean,
    fallback: Runnable,
  ): Boolean =
    (withBacktalkModifier &&
      !KeyEvent.isModifierKey(keyCode) &&
      captured(ScriptInput.keysBinding(ScriptInput.Keys(modifiers, keyCode)))) ||
      host?.onKeys(modifiers, keyCode, withBacktalkModifier, fallback) == true

  fun captureNext(listener: ((String) -> Unit)?) {
    capture = listener
  }

  @JvmStatic fun menuItems(): List<ScriptMenuItem> = host?.menuItems().orEmpty()

  @JvmStatic
  fun readingControls(): List<ScriptReadingControl> = host?.readingControls().orEmpty()

  @JvmStatic
  fun itemActions(node: AccessibilityNodeInfoCompat): List<ScriptItemAction> =
    host?.itemActions(node).orEmpty()

  @JvmStatic fun toggleAll(): Boolean? = host?.toggleAll()

  private fun group(node: AccessibilityNodeInfoCompat): NodeRule? =
    rule(node, RuleAspect.GROUP)?.takeIf { it.group }

  private fun offerGesture(name: String?, fallback: Runnable): Boolean =
    name != null &&
      (captured(ScriptInput.gestureBinding(name)) || host?.onGesture(name, fallback) == true)

  private fun captured(binding: String): Boolean {
    val listener = capture ?: return false
    capture = null
    listener(binding)
    return true
  }
}

private class RuleOverrides(private val host: ScriptHost) : NodeOverrides.Source {
  override fun role(node: AccessibilityNodeInfoCompat): Int? =
    host.rule(node, RuleAspect.ROLE)?.role

  override fun state(node: AccessibilityNodeInfoCompat): CharSequence? =
    host.rule(node, RuleAspect.STATE)?.state

  override fun hidden(node: AccessibilityNodeInfoCompat): Boolean = host.isHidden(node)

  override fun heading(node: AccessibilityNodeInfoCompat): Boolean? =
    host.rule(node, RuleAspect.HEADING)?.heading

  override fun grouped(node: AccessibilityNodeInfoCompat): Boolean =
    host.rule(node, RuleAspect.GROUP)?.group == true

  override fun order(node: AccessibilityNodeInfoCompat): NodeOverrides.Order? =
    host.rule(node, RuleAspect.ORDER)?.order
}
