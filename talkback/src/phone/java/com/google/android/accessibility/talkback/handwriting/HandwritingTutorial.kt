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
package com.google.android.accessibility.talkback.handwriting

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.talkback.R

/**
 * The handwriting keyboard's tutorial: it says each step, waits for the user to do it, and goes on.
 * Nothing is typed in the field meanwhile; the tutorial keeps its own text.
 */
class HandwritingTutorial(
  private val res: Resources,
  private val speak: (String) -> Unit,
  private val onFinished: () -> Unit,
) {
  enum class Gesture {
    SPACE,
    DELETE,
    NEW_LINE,
    NEXT_READING,
    READ_BACK,
  }

  private enum class Step(val instruction: Int) {
    WRITE_LETTER(R.string.handwriting_tutorial_write_letter),
    WRITE_WORD(R.string.handwriting_tutorial_write_word),
    SPACE(R.string.handwriting_tutorial_space),
    DELETE(R.string.handwriting_tutorial_delete),
    NEXT_READING(R.string.handwriting_tutorial_next_reading),
    READ_BACK(R.string.handwriting_tutorial_read_back),
    NEW_LINE(R.string.handwriting_tutorial_new_line),
  }

  private val handler = Handler(Looper.getMainLooper())
  private var step: Step? = null
  private val text = StringBuilder()
  private var readings: List<String> = emptyList()
  private var reading = 0
  private val repeat = Runnable { step?.let { say(res.getString(it.instruction)) } }

  val isActive: Boolean
    get() = step != null

  fun start() {
    text.setLength(0)
    readings = emptyList()
    go(Step.WRITE_LETTER, res.getString(R.string.handwriting_tutorial_intro))
  }

  /** Ends the tutorial, as a three-finger swipe down does. */
  fun finish() {
    if (step == null) return
    step = null
    handler.removeCallbacks(repeat)
    say(res.getString(R.string.handwriting_tutorial_finished))
    onFinished()
  }

  fun cancel() {
    step = null
    handler.removeCallbacks(repeat)
  }

  /** Writing was recognized as [candidates], most likely first. */
  fun onWriting(candidates: List<String>) {
    val current = step ?: return
    val first = candidates.firstOrNull() ?: return
    text.append(first)
    readings = candidates
    reading = 0
    when (current) {
      Step.WRITE_LETTER ->
        if (candidates.any { it.equals("a", ignoreCase = true) }) {
          go(Step.WRITE_WORD, res.getString(R.string.handwriting_tutorial_typed, first))
        } else {
          again(res.getString(R.string.handwriting_tutorial_typed, first))
        }
      Step.WRITE_WORD ->
        if (candidates.any { it.equals("hi", ignoreCase = true) }) {
          go(Step.SPACE, res.getString(R.string.handwriting_tutorial_typed, first))
        } else {
          again(res.getString(R.string.handwriting_tutorial_typed, first))
        }
      else -> again(res.getString(R.string.handwriting_tutorial_typed, first))
    }
  }

  fun onGesture(gesture: Gesture) {
    val current = step ?: return
    val wanted =
      when (current) {
        Step.SPACE -> Gesture.SPACE
        Step.DELETE -> Gesture.DELETE
        Step.NEXT_READING -> Gesture.NEXT_READING
        Step.READ_BACK -> Gesture.READ_BACK
        Step.NEW_LINE -> Gesture.NEW_LINE
        else -> null
      }
    if (gesture != wanted) {
      again(res.getString(R.string.handwriting_tutorial_not_that))
      return
    }
    when (gesture) {
      Gesture.SPACE -> text.append(' ')
      Gesture.DELETE -> if (text.isNotEmpty()) text.setLength(text.length - 1)
      Gesture.NEW_LINE -> text.append('\n')
      Gesture.NEXT_READING,
      Gesture.READ_BACK -> {}
    }
    when (current) {
      Step.SPACE -> go(Step.DELETE, res.getString(R.string.handwriting_space))
      Step.DELETE -> go(Step.NEXT_READING, res.getString(R.string.handwriting_tutorial_deleted))
      Step.NEXT_READING -> {
        val shown =
          if (readings.size < 2) res.getString(R.string.handwriting_no_alternatives)
          else {
            reading = (reading + 1) % readings.size
            res.getString(R.string.handwriting_alternative, readings[reading], reading + 1, readings.size)
          }
        go(Step.READ_BACK, shown)
      }
      Step.READ_BACK ->
        go(Step.NEW_LINE, text.toString().ifBlank { res.getString(R.string.handwriting_empty) })
      Step.NEW_LINE -> {
        step = null
        handler.removeCallbacks(repeat)
        say(res.getString(R.string.handwriting_tutorial_done))
        onFinished()
      }
      else -> {}
    }
  }

  /** Says [result] and the instruction of [next]. */
  private fun go(next: Step, result: String) {
    step = next
    say(sentence(result) + " " + res.getString(next.instruction))
  }

  /** Says [result] and the instruction of the step again. */
  private fun again(result: String) {
    val current = step ?: return
    say(sentence(result) + " " + res.getString(current.instruction))
  }

  /** [text] with a full stop, so that the instruction after it is said as a sentence of its own. */
  private fun sentence(text: String): String =
    if (text.trimEnd().lastOrNull()?.let { it in ".!?" } == true) text else "$text."

  private fun say(text: String) {
    speak(text)
    handler.removeCallbacks(repeat)
    if (step != null) handler.postDelayed(repeat, REPEAT_MS)
  }

  private companion object {
    /** The instruction is said again after this long without the step done. */
    const val REPEAT_MS = 20_000L
  }
}
