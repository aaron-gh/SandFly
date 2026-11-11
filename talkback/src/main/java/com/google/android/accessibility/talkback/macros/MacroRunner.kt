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

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
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
 * reader go on between them. Looking for an item to tap reads the windows on a thread of the run's
 * own, since each node it reads asks the app. A step that fails says so and stops the macro.
 *
 * Called on the main thread.
 */
object MacroRunner {
  private const val TAG = "MacroRunner"

  /** The time after a step that changes the screen, such as a command, for the change to start. */
  private const val STEP_GAP_MS = 300L

  private const val POLL_MS = 250L

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

  /** Runs the macro [id], stopping the one running. Returns false when it can't run. */
  @JvmStatic
  fun run(id: String): Boolean {
    val service = TalkBackService.getInstance() ?: return false
    val macro = Macros.find(SharedPreferencesUtils.getSharedPreferences(service), id) ?: return false
    stop()
    val run = Run(service, macro)
    current = run
    run.later(0) { run.runStep(0) }
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

    // The thread that looks for items to tap, made for the first Tap step.
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

    fun runStep(index: Int) {
      if (!active) return
      val step = macro.steps.getOrNull(index)
      if (step == null) {
        done()
        return
      }
      try {
        when (step) {
          is Macros.RunCommand -> {
            if (step.value == Macros.STOP_COMMAND) {
              done()
              return
            }
            // A macro started from a macro would stop it, and could start itself again and again.
            if (Macros.isCommand(step.value) || !service.performCommand(step.value)) {
              fail(step)
              return
            }
            later(STEP_GAP_MS) { runStep(index + 1) }
          }
          is Macros.Wait -> later(step.millis) { runStep(index + 1) }
          is Macros.OpenApp -> {
            if (!OpenAppActions.open(service, step.value)) {
              fail(step)
              return
            }
            later(STEP_GAP_MS) { runStep(index + 1) }
          }
          is Macros.TypeText -> typeText(index, step, SystemClock.uptimeMillis() + FIELD_TIMEOUT_MS)
          is Macros.Tap -> tap(index, step)
        }
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Macro step failed: %s", e)
        fail(step)
      }
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
          fail(step)
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
        fail(step)
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
      var handler = searchHandler
      if (handler == null) {
        val thread = HandlerThread(TAG)
        thread.start()
        searchThread = thread
        handler = Handler(thread.looper)
        searchHandler = handler
      }
      val deadline = SystemClock.uptimeMillis() + step.timeoutMillis
      handler.post { search(index, step, deadline) }
    }

    /** Looks for the item of [step] on the search thread until [deadline], and clicks it. */
    private fun search(index: Int, step: Macros.Tap, deadline: Long) {
      if (finished) return
      val clicked =
        try {
          val node = findItem(service, step.label)
          // The run can be stopped while the windows are read.
          if (finished) return
          node?.let { click(it) }
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot read the windows: %s", e)
          null
        }
      when {
        clicked == true -> mainHandler.post { ifActive { later(STEP_GAP_MS) { runStep(index + 1) } } }
        clicked == false -> mainHandler.post { ifActive { fail(step, found = true) } }
        SystemClock.uptimeMillis() + POLL_MS <= deadline ->
          searchHandler?.postDelayed({ search(index, step, deadline) }, POLL_MS)
        else -> mainHandler.post { ifActive { fail(step) } }
      }
    }

    /** Says that [step] failed, or for a Tap step not [found], that its item isn't there, and stops. */
    private fun fail(step: Macros.Step, found: Boolean = false) {
      val text =
        if (step is Macros.Tap && !found) {
          service.getString(R.string.macro_item_not_found, step.label)
        } else {
          service.getString(R.string.macro_step_failed, Macros.describe(service, step))
        }
      service.feedbackReturner?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.requestedSpeech(text))
      done()
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
   * The item whose text or description is [label], ignoring case, or else the first one whose text
   * or description contains it, in the active window first, then from the top window down.
   * SandFly's own overlays are left out.
   */
  private fun findItem(service: TalkBackService, label: String): AccessibilityNodeInfo? {
    val target = label.trim()
    if (target.isEmpty()) return null
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
        if (node.isVisibleToUser) {
          for (text in listOfNotNull(node.text, node.contentDescription)) {
            val trimmed = text.toString().trim()
            if (trimmed.equals(target, ignoreCase = true)) return node
            if (partial == null && trimmed.contains(target, ignoreCase = true)) partial = node
          }
        }
        for (i in 0 until node.childCount) {
          node.getChild(i)?.let { queue.add(it) }
        }
      }
    }
    return partial
  }

  /** Clicks [node], or the nearest item above it that can be clicked. Returns whether it did. */
  private fun click(node: AccessibilityNodeInfo): Boolean {
    var candidate: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val item = candidate ?: return@repeat
      if (item.isClickable && item.isEnabled) {
        return item.performAction(AccessibilityNodeInfo.ACTION_CLICK)
      }
      candidate = item.parent
    }
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
  }
}
