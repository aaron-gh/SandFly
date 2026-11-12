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

import com.google.android.accessibility.utils.SpannableUtils
import java.util.function.BooleanSupplier

/**
 * The texts SandFly spoke lately, for the speech history. Kept in memory only, never written to
 * storage or logs, and cleared when SandFly stops. Holds at most [MAX] texts of at most
 * [MAX_LENGTH] characters each. Speech is recorded only while SandFly runs, and not while the
 * privacy check says so, such as while a password field has focus.
 */
object SpeechHistory {
  const val MAX = 100
  const val MAX_LENGTH = 2_000

  private val texts = ArrayDeque<String>()

  /** Set while SandFly runs: true when what is spoken now must be left out. */
  @Volatile private var privacyCheck: BooleanSupplier? = null

  /** True while the speech history is on screen, so that reading it doesn't fill it. */
  @Volatile private var paused = false

  /**
   * Starts recording speech, leaving out what is spoken while [check] is true, or stops when
   * [check] is null and clears the history.
   */
  @JvmStatic
  fun setPrivacyCheck(check: BooleanSupplier?) {
    privacyCheck = check
    if (check == null) clear()
  }

  @JvmStatic
  fun setPaused(paused: Boolean) {
    this.paused = paused
  }

  /** Records the text of [item], as Copy last spoken phrase copies it. */
  @JvmStatic
  fun record(item: FeedbackItem) {
    if (paused || privacyCheck == null) return
    record(EmojiSpeech.restoreOriginalText(SpannableUtils.getCopyableText(item.aggregateText)))
  }

  /** Records [text]. */
  @JvmStatic
  fun record(text: CharSequence?) {
    if (!text.isNullOrBlank() && isRecording()) add(text)
  }

  private fun isRecording(): Boolean {
    if (paused) return false
    val check = privacyCheck ?: return false
    return !check.asBoolean
  }

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
