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

package com.google.android.accessibility.utils.output

import android.view.accessibility.AccessibilityEvent
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.utils.SpannableUtils

/**
 * The texts SandFly spoke lately, for the speech history. Kept in memory only, never written to
 * storage or logs, and cleared when SandFly stops. Holds at most [MAX] texts of at most
 * [MAX_LENGTH] characters each. Speech is recorded only while SandFly runs, and not while a
 * password field has accessibility focus or input focus.
 *
 * Which focus is on a password field is kept from the focus events and from the nodes SandFly
 * speaks before their focus events arrive, so that recording asks the app nothing. Where it is not
 * known, such as after a focus action that failed, speech is not recorded until focus moves.
 */
object SpeechHistory {
  const val MAX = 100
  const val MAX_LENGTH = 2_000

  private val texts = ArrayDeque<String>()

  /** Set while SandFly runs. */
  @Volatile private var running = false

  /** Whether accessibility focus is, or may be, on a password field. */
  @Volatile private var accessibilityFocusPassword = false

  /** Whether input focus is, or may be, on a password field. */
  @Volatile private var inputFocusPassword = false

  /**
   * When SandFly last set accessibility focus, in uptime. Focus events sent before it are of an
   * earlier focus, and don't say the focus left a password field.
   */
  @Volatile private var earlyFocusTime = 0L

  /** True while the speech history is on screen, so that reading it doesn't fill it. */
  @Volatile private var paused = false

  /** Starts recording speech, when SandFly starts. */
  @JvmStatic
  fun start() {
    resetFocus()
    running = true
  }

  /** Stops recording speech and clears the history, when SandFly stops. */
  @JvmStatic
  fun stop() {
    running = false
    resetFocus()
    clear()
  }

  private fun resetFocus() {
    accessibilityFocusPassword = false
    inputFocusPassword = false
    earlyFocusTime = 0L
  }

  /** Follows accessibility focus and input focus from [event]. Asks the app nothing. */
  @JvmStatic
  fun onAccessibilityEvent(event: AccessibilityEvent) =
    onEvent(event.eventType, event.isPassword, event.eventTime)

  /**
   * Follows the focus from an event of [type], about a password field when [password], sent at
   * [eventTime] in uptime, or 0 if unknown.
   */
  @VisibleForTesting
  @JvmStatic
  fun onEvent(type: Int, password: Boolean, eventTime: Long) {
    when (type) {
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
        if (password) {
          accessibilityFocusPassword = true
        } else if (eventTime <= 0 || eventTime >= earlyFocusTime) {
          accessibilityFocusPassword = false
        }
      AccessibilityEvent.TYPE_VIEW_FOCUSED -> inputFocusPassword = password
      // Typing or moving in a field means it has input focus. Other fields don't send these.
      AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
      AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
      AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY ->
        if (password) inputFocusPassword = true
    }
  }

  /**
   * Notes that SandFly sets accessibility focus on a node, a password field when [password], which
   * may be spoken before the app's focus event for it arrives. [time] is when the focus action
   * started, or now when the speech comes before the action, in uptime.
   */
  @JvmStatic
  fun onAccessibilityFocusSet(password: Boolean, time: Long) {
    accessibilityFocusPassword = password
    earlyFocusTime = maxOf(earlyFocusTime, time)
  }

  /**
   * Notes that accessibility focus is not known, such as when a focus action spoken before it was
   * performed failed. Nothing is recorded until focus moves.
   */
  @JvmStatic
  fun onFocusUnknown() {
    accessibilityFocusPassword = true
  }

  /** Whether accessibility focus or input focus is, or may be, on a password field. */
  @JvmStatic fun isPasswordFocused(): Boolean = accessibilityFocusPassword || inputFocusPassword

  /**
   * Sets which focus is on a password field, as read from the app again, such as after a window
   * change, when the field may have gone with its window without a focus event.
   */
  @JvmStatic
  fun onFocusRead(accessibilityFocusPassword: Boolean, inputFocusPassword: Boolean) {
    this.accessibilityFocusPassword = accessibilityFocusPassword
    this.inputFocusPassword = inputFocusPassword
  }

  @JvmStatic
  fun setPaused(paused: Boolean) {
    this.paused = paused
  }

  /** Records the text of [item], as Copy last spoken phrase copies it. */
  @JvmStatic
  fun record(item: FeedbackItem) {
    if (!isRecording()) return
    record(EmojiSpeech.restoreOriginalText(SpannableUtils.getCopyableText(item.aggregateText)))
  }

  /** Records [text]. */
  @JvmStatic
  fun record(text: CharSequence?) {
    if (!text.isNullOrBlank() && isRecording()) add(text)
  }

  private fun isRecording(): Boolean = running && !paused && !isPasswordFocused()

  private fun add(text: CharSequence) {
    var entry = text.toString().trim()
    if (entry.length > MAX_LENGTH) {
      // Not between the two halves of a surrogate pair.
      val end = if (Character.isHighSurrogate(entry[MAX_LENGTH - 1])) MAX_LENGTH - 1 else MAX_LENGTH
      entry = entry.substring(0, end)
    }
    synchronized(texts) {
      // The same text again, such as from Repeat last spoken phrase, is listed once.
      if (texts.lastOrNull() == entry) return
      texts.addLast(entry)
      while (texts.size > MAX) texts.removeFirst()
    }
  }

  /** The texts, newest first. */
  @JvmStatic fun entries(): List<String> = synchronized(texts) { texts.reversed() }

  @JvmStatic
  fun clear() {
    synchronized(texts) { texts.clear() }
  }
}
