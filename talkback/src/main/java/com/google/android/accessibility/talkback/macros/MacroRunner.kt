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

package com.google.android.accessibility.talkback.macros

import android.app.KeyguardManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Runs macros, one at a time: starting a macro stops the one running.
 *
 * Steps run on the main thread, one after another through a handler, so gestures and the screen
 * reader go on between them. Looking for an item to tap and checking conditions read the windows on
 * a thread of the run's own, since each node they read asks the app. A step that fails says so and
 * stops the macro.
 *
 * Called on the main thread.
 */
object MacroRunner {
  private const val TAG = "MacroRunner"

  /** The time after a step that changes the screen, such as a command, for the change to start. */
  private const val STEP_GAP_MS = 300L

  private const val POLL_MS = 250L

  /** The time between two runs of a repeat's steps, so that a repeat of quick steps doesn't spin. */
  private const val REPEAT_GAP_MS = 100L

  /** The most items in the focused item whose text a test reads. */
  private const val MAX_FOCUS_NODES = 50

  /** How long a Type text step waits for an edit field to have input focus. */
  private const val FIELD_TIMEOUT_MS = 3_000L

  private const val MAX_NODES = 5_000
  private const val MAX_CLICK_PARENTS = 6

  private val mainHandler = Handler(Looper.getMainLooper())

  private var current: Run? = null

  /** Returns the ID of the macro running, or null. */
  @JvmStatic fun runningId(): String? = current?.macro?.id

  /**
   * Runs the command [value]: the macro whose command it is, or [Macros.STOP_COMMAND]. Returns
   * false when there is no such macro or SandFly isn't running.
   */
  @JvmStatic
  fun runCommand(value: String): Boolean {
    if (value == Macros.STOP_COMMAND) {
      stop()
      return true
    }
    val id = Macros.idOf(value) ?: return false
    return run(id)
  }

  /**
   * Runs the macro [id], stopping the one running, when its condition is met. [sayNotMet] says so
   * when it isn't. Returns false when it can't run.
   */
  @JvmStatic
  @JvmOverloads
  fun run(id: String, sayNotMet: Boolean = false): Boolean {
    val service = TalkBackService.getInstance() ?: return false
    val macro = Macros.find(SharedPreferencesUtils.getSharedPreferences(service), id) ?: return false
    stop()
    val run = Run(service, macro)
    current = run
    run.later(0) { run.start(sayNotMet) }
    return true
  }

  /** Stops the macro running, if one is. */
  @JvmStatic
  fun stop() {
    current?.finish()
    current = null
  }

  /** One run of [macro]. Its callbacks on the main handler are posted with the run as token. */
  private class Run(val service: TalkBackService, val macro: Macros.Macro) {
    @Volatile var finished = false

    private val flow = Macros.analyze(macro.steps)

    // How many times each repeat has run its steps since the macro reached it.
    private val repeats = IntArray(macro.steps.size)

    // The thread that looks for items and checks conditions, made when the run first needs it.
    private var searchThread: HandlerThread? = null
    @Volatile private var searchHandler: Handler? = null

    private val active: Boolean
      get() = !finished && current === this && TalkBackService.getInstance() === service

    fun later(delayMs: Long, block: () -> Unit) {
      mainHandler.postAtTime({ ifActive(block) }, this, SystemClock.uptimeMillis() + delayMs)
    }

    /** Runs [block] while the run is active, or else ends it, as when SandFly has been turned off. */
    private fun ifActive(block: () -> Unit) {
      if (active) block() else done()
    }

    /** Runs the steps when the macro's condition is met and its blocks match. */
    fun start(sayNotMet: Boolean) {
      flow.problem?.let {
        Log.w(TAG, "Macro not run: blocks don't match at step ${it.index + 1}")
        say(
          service.getString(
            R.string.macro_stopped_problem,
            it.index + 1,
            service.getString(it.message),
          )
        )
        done()
        return
      }
      evaluate(macro.condition) { met ->
        if (met) {
          runStep(0)
        } else {
          if (sayNotMet) say(service.getString(R.string.macro_not_met))
          done()
        }
      }
    }

    fun runStep(index: Int) {
      if (!active) return
      val step = macro.steps.getOrNull(index)
      if (step == null) {
        done()
        return
      }
      try {
        when (step) {
          is Macros.If -> evaluate(step.condition) { if (it) runStep(index + 1) else branch(flow.next[index]) }
          // The branch before it has run.
          is Macros.ElseIf, Macros.Else -> runStep(flow.end[index] + 1)
          Macros.EndIf -> runStep(index + 1)
          is Macros.RepeatTimes, is Macros.RepeatWhile -> {
            repeats[index] = 0
            repeatAgain(index)
          }
          Macros.EndRepeat -> later(REPEAT_GAP_MS) { repeatAgain(flow.start[index]) }
          Macros.ExitLoop -> runStep(flow.end[flow.start[index]] + 1)
          Macros.StopMacro -> done()
          is Macros.WaitUntil -> waitUntil(index, step, SystemClock.uptimeMillis() + step.timeoutMillis)
          is Macros.RunCommand -> {
            if (step.value == Macros.STOP_COMMAND) {
              done()
              return
            }
            // A macro started from a macro would stop it, and could start itself again and again.
            if (Macros.isCommand(step.value)) {
              fail(index, step, "macro run from a macro")
              return
            }
            if (!service.performCommand(step.value)) {
              fail(index, step, "command not run")
              return
            }
            later(STEP_GAP_MS) { runStep(index + 1) }
          }
          is Macros.Wait -> later(step.millis) { runStep(index + 1) }
          is Macros.OpenApp -> {
            if (!OpenAppActions.open(service, step.value)) {
              fail(index, step, "app not opened")
              return
            }
            later(STEP_GAP_MS) { runStep(index + 1) }
          }
          is Macros.TypeText -> typeText(index, step, SystemClock.uptimeMillis() + FIELD_TIMEOUT_MS)
          is Macros.Tap -> tap(index, step)
        }
      } catch (e: RuntimeException) {
        fail(index, step, e.toString())
      }
    }

    /** Goes on at [index], an Else if, Else or End if reached when the branches before it didn't run. */
    private fun branch(index: Int) {
      when (val step = macro.steps[index]) {
        is Macros.ElseIf -> evaluate(step.condition) { if (it) runStep(index + 1) else branch(flow.next[index]) }
        else -> runStep(index + 1)
      }
    }

    /** Runs the steps of the repeat at [index] once more, or goes on after it. */
    private fun repeatAgain(index: Int) {
      val step = macro.steps[index]
      val again: (Boolean) -> Unit = { again ->
        when {
          !again -> runStep(flow.end[index] + 1)
          repeats[index] >= Macros.MAX_REPEATS -> {
            Log.w(TAG, "Macro stopped at step ${index + 1}: ${Macros.MAX_REPEATS} repeats")
            say(
              service.getString(
                R.string.macro_stopped_repeats,
                Macros.MAX_REPEATS,
                Macros.describe(service, step),
              )
            )
            done()
          }
          else -> {
            repeats[index]++
            runStep(index + 1)
          }
        }
      }
      when (step) {
        is Macros.RepeatTimes -> again(repeats[index] < step.count)
        is Macros.RepeatWhile -> evaluate(step.condition, again)
        else -> runStep(index + 1)
      }
    }

    private fun waitUntil(index: Int, step: Macros.WaitUntil, deadline: Long) {
      evaluate(step.condition) { met ->
        when {
          met -> runStep(index + 1)
          SystemClock.uptimeMillis() + POLL_MS <= deadline -> later(POLL_MS) { waitUntil(index, step, deadline) }
          else -> {
            Log.w(TAG, "Macro stopped at step ${index + 1}: Wait until timed out")
            say(service.getString(R.string.macro_stopped_timeout, Macros.describe(service, step)))
            done()
          }
        }
      }
    }

    /**
     * Checks [condition] on the search thread, and calls [then] on the main thread with whether it
     * is met, while the run is active. A test that can't read the windows isn't met.
     */
    private fun evaluate(condition: Macros.Condition, then: (Boolean) -> Unit) {
      if (condition.clauses.isEmpty()) {
        then(true)
        return
      }
      searchHandler().post {
        if (finished) return@post
        val met =
          try {
            condition.isMet { check(service, it) }
          } catch (e: RuntimeException) {
            LogUtils.w(TAG, "Cannot check a macro condition: %s", e)
            false
          }
        mainHandler.post { ifActive { then(met) } }
      }
    }

    private fun searchHandler(): Handler =
      searchHandler
        ?: Handler(HandlerThread(TAG).also { it.start(); searchThread = it }.looper).also {
          searchHandler = it
        }

    private fun typeText(index: Int, step: Macros.TypeText, deadline: Long) {
      val field =
        try {
          service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot find the edit field: %s", e)
          null
        }
      if (field == null) {
        if (SystemClock.uptimeMillis() < deadline) {
          later(POLL_MS) { typeText(index, step, deadline) }
        } else {
          fail(index, step, "no edit field with input focus")
        }
        return
      }
      val typed =
        if (step.replace) {
          replaceText(field, step.text)
        } else {
          service.feedbackReturner?.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.edit(
                AccessibilityNodeInfoCompat.wrap(field),
                Feedback.EditText.Action.INSERT,
              )
              .setText(step.text),
          ) == true
        }
      if (!typed) {
        fail(index, step, "text not set")
        return
      }
      later(STEP_GAP_MS) { runStep(index + 1) }
    }

    private fun replaceText(field: AccessibilityNodeInfo, text: String): Boolean {
      val arguments =
        Bundle().apply {
          putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
      if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) return false
      val cursor =
        Bundle().apply {
          putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, text.length)
          putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
        }
      // The text is set even when the field can't move its cursor.
      field.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursor)
      return true
    }

    private fun tap(index: Int, step: Macros.Tap) {
      val label = LabelMatch(step.label, step.regex)
      if (!label.isUsable) {
        fail(index, step, "regular expression not usable", found = true)
        return
      }
      val deadline = SystemClock.uptimeMillis() + step.timeoutMillis
      searchHandler().post { search(index, step, label, deadline) }
    }

    /** Looks for the item of [step] by [label] on the search thread until [deadline], and clicks it. */
    private fun search(index: Int, step: Macros.Tap, label: LabelMatch, deadline: Long) {
      if (finished) return
      val clicked =
        try {
          val node = findItem(service, label)
          // The run can be stopped while the windows are read.
          if (finished) return
          node?.let { click(it) }
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot read the windows: %s", e)
          null
        }
      when {
        clicked == true -> mainHandler.post { ifActive { later(STEP_GAP_MS) { runStep(index + 1) } } }
        clicked == false -> mainHandler.post { ifActive { fail(index, step, "click not performed", found = true) } }
        SystemClock.uptimeMillis() + POLL_MS <= deadline ->
          searchHandler?.postDelayed({ search(index, step, label, deadline) }, POLL_MS)
        else -> mainHandler.post { ifActive { fail(index, step, "item not found") } }
      }
    }

    /**
     * Says that [step], at [index], failed, or for a Tap step not [found], that its item isn't
     * there, and stops. Logs [reason] at warning level, which problem reports include by default.
     */
    private fun fail(index: Int, step: Macros.Step, reason: String, found: Boolean = false) {
      Log.w(TAG, "Macro stopped at step ${index + 1} of ${macro.steps.size}: ${step::class.simpleName}, $reason")
      val text =
        if (step is Macros.Tap && !found) {
          service.getString(R.string.macro_item_not_found, step.label)
        } else {
          service.getString(R.string.macro_step_failed, Macros.describe(service, step))
        }
      say(text)
      done()
    }

    private fun say(text: String) {
      service.feedbackReturner?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.requestedSpeech(text))
    }

    private fun done() {
      finish()
      if (current === this) current = null
    }

    fun finish() {
      finished = true
      mainHandler.removeCallbacksAndMessages(this)
      searchThread?.quitSafely()
      searchThread = null
      searchHandler = null
    }
  }

  /**
   * Looks for the text of a macro step or test in the texts of items: as a text they contain,
   * ignoring case, or for [regex] as a regular expression found in them. A regular expression that
   * [Macros.compilePattern] refuses matches nothing.
   */
  private class LabelMatch(label: String, private val regex: Boolean) {
    private val target = label.trim()
    private val pattern = if (regex) Macros.compilePattern(target) else null

    val isEmpty: Boolean = target.isEmpty()

    /** Whether there is something to look for. */
    val isUsable: Boolean = !isEmpty && (!regex || pattern != null)

    /** Whether [text] is the text looked for. A regular expression is only ever found in a text. */
    fun isExactly(text: String): Boolean = !regex && text.equals(target, ignoreCase = true)

    fun isIn(text: CharSequence): Boolean =
      if (regex) pattern?.matcher(text.take(Macros.MAX_PATTERN_INPUT))?.find() == true
      else text.contains(target, ignoreCase = true)
  }

  /**
   * The item whose text or description is [label], ignoring case, or else the first one whose text
   * or description contains it or, for a regular expression, matches it, in the active window
   * first, then from the top window down. SandFly's own overlays are left out.
   */
  private fun findItem(
    service: TalkBackService,
    label: LabelMatch,
    kind: Macros.Kind? = null,
    anyMatch: Boolean = false,
  ): AccessibilityNodeInfo? {
    if (!label.isUsable) return null
    val roots =
      service.windows
        .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
        .sortedWith(compareByDescending<AccessibilityWindowInfo> { it.isActive }.thenByDescending { it.layer })
        .mapNotNull { it.root }
    var partial: AccessibilityNodeInfo? = null
    var count = 0
    for (root in roots) {
      val queue = ArrayDeque<AccessibilityNodeInfo>()
      queue.add(root)
      while (queue.isNotEmpty() && count < MAX_NODES) {
        val node = queue.removeFirst()
        count++
        if (node.isVisibleToUser && (kind == null || isKind(node, kind))) {
          val texts =
            if (kind == null && !anyMatch) listOfNotNull(node.text, node.contentDescription)
            else texts(node)
          for (text in texts) {
            val trimmed = text.toString().trim()
            if (label.isExactly(trimmed)) return node
            if (partial == null && label.isIn(trimmed)) {
              if (anyMatch) return node
              partial = node
            }
          }
        }
        for (i in 0 until node.childCount) {
          node.getChild(i)?.let { queue.add(it) }
        }
      }
    }
    return partial
  }

  /** Returns whether the macro condition test [test] is met. Called on the search thread. */
  private fun check(service: TalkBackService, test: Macros.Test): Boolean =
    when (test) {
      is Macros.ItemOnScreen ->
        findItem(service, LabelMatch(test.label, test.regex), test.kind, anyMatch = true) != null
      is Macros.FocusedItem -> {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        val label = LabelMatch(test.label, test.regex)
        focused != null &&
          (test.kind == null || isKind(focused, test.kind)) &&
          (label.isEmpty || containsText(focused, label))
      }
      is Macros.AppInFront -> activePackage(service) == test.packageName
      Macros.KeyboardShown -> service.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
      Macros.ScreenLocked -> service.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
    }

  /** The package of the app in the active window, or of the top app window when none is active. */
  private fun activePackage(service: TalkBackService): CharSequence? {
    val windows = service.windows
    val window =
      windows.firstOrNull { it.isActive && it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        ?: windows
          .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
          .maxByOrNull { it.layer }
    return window?.root?.packageName
  }

  private fun texts(node: AccessibilityNodeInfo): List<CharSequence> =
    listOfNotNull(
      node.text,
      node.contentDescription,
      node.hintText,
      AccessibilityNodeInfoCompat.wrap(node).stateDescription,
    )

  /** Returns whether the text of [node] or of the items in it has [label] in it. */
  private fun containsText(node: AccessibilityNodeInfo, label: LabelMatch): Boolean {
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(node)
    var count = 0
    while (queue.isNotEmpty() && count < MAX_FOCUS_NODES) {
      val item = queue.removeFirst()
      count++
      if (texts(item).any { label.isIn(it) }) return true
      for (i in 0 until item.childCount) {
        item.getChild(i)?.let { queue.add(it) }
      }
    }
    return false
  }

  /**
   * Returns whether [node] is of [kind]. For Clickable and Long-clickable, an item above it within
   * a few levels counts too, as the text of a button is often an item in it.
   */
  private fun isKind(node: AccessibilityNodeInfo, kind: Macros.Kind): Boolean =
    when (kind) {
      Macros.Kind.CLICKABLE -> nodeOrParent(node) { it.isClickable }
      Macros.Kind.LONG_CLICKABLE -> nodeOrParent(node) { it.isLongClickable }
      Macros.Kind.EDITABLE -> node.isEditable
      Macros.Kind.SCROLLABLE ->
        node.isScrollable ||
          node.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
              it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
          }
      Macros.Kind.ADJUSTABLE ->
        node.rangeInfo != null ||
          node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id }
      Macros.Kind.CHECKABLE -> node.isCheckable
      @Suppress("DEPRECATION") Macros.Kind.CHECKED -> node.isChecked
      Macros.Kind.SELECTED -> node.isSelected
      Macros.Kind.HEADING -> AccessibilityNodeInfoCompat.wrap(node).isHeading
      Macros.Kind.ENABLED -> node.isEnabled
    }

  private fun nodeOrParent(node: AccessibilityNodeInfo, test: (AccessibilityNodeInfo) -> Boolean): Boolean {
    var candidate: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val item = candidate ?: return false
      if (test(item)) return true
      candidate = item.parent
    }
    return false
  }

  /** Clicks [node], or the nearest item above it that can be clicked. Returns whether it did. */
  private fun click(node: AccessibilityNodeInfo): Boolean {
    var candidate: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val item = candidate ?: return@repeat
      if (item.isClickable && item.isEnabled) {
        return item.performAction(AccessibilityNodeInfo.ACTION_CLICK).also {
          if (!it) Log.w(TAG, "Click returned false: ${item.className} in ${item.packageName}")
        }
      }
      candidate = item.parent
    }
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK).also {
      if (!it) Log.w(TAG, "No clickable item: ${node.className} in ${node.packageName}")
    }
  }
}
