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

import kotlin.math.max

/**
 * The actions [MacroRecorder] records, and the macro steps made from them. Besides one step per
 * action, the steps hold what the actions imply:
 * - scrolls in a list followed by a press on an item in it become a Repeat while that scrolls
 *   until the item is on screen, then the press;
 * - an action, or a run of actions, done several times in a row becomes a Repeat, a Repeat while
 *   the first item pressed is on screen when that item was gone after the last time;
 * - a press that turns a switch or check box on or off runs only when it is off or on;
 * - a press in the app launcher that opened an app becomes Open app;
 * - when the first action is in the app that was in front at the start, that app is opened first
 *   when it isn't in front.
 */
object MacroRecording {
  /** One thing the user did. [time] is in uptime milliseconds; [pkg] is the app it was done in. */
  sealed class Action {
    abstract val time: Long
    abstract val pkg: String?
  }

  /**
   * A press on an item, found by [label] in [field], a regular expression for [regex], or at [x]
   * and [y] when [label] is empty.
   * [long] for a long press; [held] when it was a held touch, which some apps act on only.
   * [checkedBefore] is whether a switch or check box the press turned on or off was on before, or
   * null for other presses. [inScrolled] when the item is in the list scrolled just before.
   * [shownAfter] is the labels of the presses so far, as [labelKey] gives them, that were on screen
   * after the press, or null when that wasn't checked.
   */
  data class Press(
    override val time: Long,
    override val pkg: String?,
    val label: String,
    val field: Macros.Field? = null,
    val regex: Boolean = false,
    val long: Boolean = false,
    val held: Boolean = false,
    val x: Int = 0,
    val y: Int = 0,
    val checkedBefore: Boolean? = null,
    val inScrolled: Boolean = false,
    val inLauncher: Boolean = false,
    val editable: Boolean = false,
    val shownAfter: Set<String>? = null,
  ) : Action()

  /**
   * [count] scrolls of the list found by [list] in [field], or of the largest list on screen when
   * [list] is empty.
   */
  data class Scroll(
    override val time: Long,
    override val pkg: String?,
    val list: String,
    val field: Macros.Field?,
    val forward: Boolean,
    val count: Int = 1,
    val regex: Boolean = false,
  ) : Action()

  /**
   * [text] typed in the edit field found by [label] in [field]. [afterPress] when the press before
   * it was on that field, which gave it input focus.
   */
  data class Type(
    override val time: Long,
    override val pkg: String?,
    val label: String,
    val field: Macros.Field?,
    val text: String,
    val afterPress: Boolean,
    val regex: Boolean = false,
  ) : Action()

  /** A SandFly command, such as Back. */
  data class Command(override val time: Long, override val pkg: String?, val value: String) : Action()

  /** An app opened, by its [OpenAppActions][com.google.android.accessibility.talkback.gesture.OpenAppActions] value. */
  data class AppOpened(override val time: Long, override val pkg: String?, val value: String) : Action()

  /** The key of a label and the field it is looked for in, in [Press.shownAfter]. */
  @JvmStatic fun labelKey(label: String, field: Macros.Field?): String = (field?.key ?: "") + "\u0000" + label

  /**
   * [text] as a regular expression that matches only a whole text that is [text], or for [prefix],
   * one that starts with it: so that "System" isn't found in "Android System notification".
   */
  @JvmStatic
  fun wholeText(text: String, prefix: Boolean = false): String {
    val escaped = StringBuilder("^")
    for (c in text) {
      if (c in REGEX_SPECIAL) escaped.append('\\')
      escaped.append(c)
    }
    if (!prefix) escaped.append('$')
    return escaped.toString()
  }

  private const val REGEX_SPECIAL = "\\^$.|?*+()[]{}"

  /** The app SandFly reads the notification shade and quick settings of. */
  const val SYSTEM_UI = "com.android.systemui"

  /** The most actions a run that repeats can have. */
  private const val MAX_GROUP = 10

  /**
   * Returns the steps for [actions], recorded from [startTime], at most [maxSteps] whole blocks of
   * them. [startPkg] and [startApp] are the app in front when recording started and its Open app
   * value, or null. Presses in the system UI or launcher and the [trailing] commands at the end are
   * left out: they are how the recording was ended.
   */
  @JvmStatic
  fun steps(
    actions: List<Action>,
    startTime: Long,
    startPkg: String? = null,
    startApp: String? = null,
    trailing: Set<String> = emptySet(),
    maxSteps: Int = Macros.MAX_STEPS,
  ): List<Macros.Step> {
    var end = actions.size
    while (end > 0) {
      val last = actions[end - 1]
      val ending =
        (last is Press && (last.pkg == SYSTEM_UI || last.inLauncher)) || (last is Command && last.value in trailing)
      if (!ending) break
      end--
    }
    val kept = actions.subList(0, end)
    val blocks = repeated(blocks(kept, startTime))
    val out = ArrayList<Macros.Step>()
    val first = kept.firstOrNull()
    if (startPkg != null && startApp != null && first != null && first !is AppOpened && first.pkg == startPkg) {
      out += Macros.If(condition(Macros.AppInFront(startPkg), not = true))
      out += Macros.OpenApp(startApp)
      out += Macros.EndIf
    }
    for (block in blocks) {
      if (out.size + block.size > maxSteps) break
      out += block
    }
    return out
  }

  private data class Label(val text: String, val regex: Boolean, val field: Macros.Field?)

  /** Steps made from one or more actions, with what [repeated] compares and checks. */
  private class Block(
    val steps: List<Macros.Step>,
    /** The first item pressed, or null. */
    val label: Label? = null,
    /** The labels on screen after the block's last press, or null when not known. */
    val shownAfter: Set<String>? = null,
  ) {
    /** The steps without their timeouts, which follow how long the user took. */
    val key: List<Macros.Step> = steps.map(::withoutTimeout)
  }

  private fun blocks(actions: List<Action>, startTime: Long): List<Block> {
    val blocks = ArrayList<Block>()
    var previous = startTime
    var i = 0
    while (i < actions.size) {
      val action = actions[i]
      val gap = action.time - previous
      previous = action.time
      when (action) {
        is Scroll -> {
          // The scrolls of this list in a row, and what follows them.
          var j = i + 1
          while (j < actions.size && (actions[j] as? Scroll)?.let { it.list == action.list && it.field == action.field && it.regex == action.regex } == true) j++
          val next = actions.getOrNull(j)
          if (next is Press && next.inScrolled && next.label.isNotEmpty()) {
            val press = pressSteps(next, next.time - actions[j - 1].time)
            if (press != null) {
              val loop =
                listOf(
                  Macros.RepeatWhile(condition(Macros.ItemOnScreen(next.label, regex = next.regex, field = next.field), not = true)),
                  Macros.ActOnItem(action.list, scrollAct(action.forward), 1, regex = action.regex, field = action.field),
                  Macros.EndRepeat,
                )
              blocks += Block(loop + press, Label(next.label, next.regex, next.field), next.shownAfter)
              previous = next.time
              i = j + 1
              continue
            }
          }
          for (k in i until j) {
            val scroll = actions[k] as Scroll
            blocks += Block(listOf(Macros.ActOnItem(scroll.list, scrollAct(scroll.forward), scroll.count, regex = scroll.regex, field = scroll.field)))
          }
          previous = actions[j - 1].time
          i = j
          continue
        }
        is Press ->
          pressSteps(action, gap)?.let {
            val label = action.label.takeIf { l -> l.isNotEmpty() }?.let { l -> Label(l, action.regex, action.field) }
            blocks += Block(it, label, action.shownAfter)
          }
        is Type -> {
          val steps = ArrayList<Macros.Step>()
          if (!action.afterPress && action.label.isNotEmpty()) steps += Macros.Tap(action.label, timeout(gap), action.regex, action.field)
          steps += Macros.TypeText(action.text, replace = true)
          blocks += Block(steps)
        }
        is Command -> blocks += Block(listOf(Macros.RunCommand(action.value)))
        is AppOpened -> blocks += Block(listOf(Macros.OpenApp(action.value)))
      }
      i++
    }
    return blocks
  }

  /** The steps of [press], made [gap] after the action before it, or null when it can't be found again. */
  private fun pressSteps(press: Press, gap: Long): List<Macros.Step>? {
    val timeout = timeout(gap)
    val step =
      when {
        press.label.isEmpty() -> if (press.long) return null else Macros.TapPosition(press.x, press.y)
        press.long && press.held -> Macros.TouchHold(press.label, timeoutMillis = timeout, regex = press.regex, field = press.field)
        press.long -> Macros.ActOnItem(press.label, Macros.ItemAct.LONG_PRESS, 1, timeout, press.regex, press.field)
        else -> Macros.Tap(press.label, timeout, press.regex, press.field)
      }
    val before = press.checkedBefore
    if (before == null || press.label.isEmpty() || press.long) return listOf(step)
    // Turned on when it was off, or off when it was on: only pressed when it is that way again.
    val test = Macros.ItemOnScreen(press.label, Macros.Kind.CHECKED, press.regex, press.field)
    return listOf(Macros.If(condition(test, not = !before)), step, Macros.EndIf)
  }

  /**
   * How long a step waits for its item: the default, or longer when the user waited longer, as
   * for a page to load.
   */
  private fun timeout(gap: Long): Long {
    if (gap <= Macros.DEFAULT_TAP_TIMEOUT_MS) return Macros.DEFAULT_TAP_TIMEOUT_MS
    val seconds = (gap * 3 / 2 + 999) / 1000
    return (seconds * 1000).coerceIn(Macros.DEFAULT_TAP_TIMEOUT_MS, Macros.MAX_TAP_TIMEOUT_MS)
  }

  private fun scrollAct(forward: Boolean): Macros.ItemAct =
    if (forward) Macros.ItemAct.SCROLL_FORWARD else Macros.ItemAct.SCROLL_BACKWARD

  private fun condition(test: Macros.Test, not: Boolean = false): Macros.Condition =
    Macros.Condition(listOf(Macros.Clause(test, not = not)))

  private fun withoutTimeout(step: Macros.Step): Macros.Step =
    when (step) {
      is Macros.Tap -> step.copy(timeoutMillis = 0)
      is Macros.TouchHold -> step.copy(timeoutMillis = 0)
      is Macros.ActOnItem -> step.copy(timeoutMillis = 0)
      else -> step
    }

  /**
   * Puts runs of blocks done several times in a row in a Repeat: the longest stretch first, and of
   * those, the shortest run.
   */
  private fun repeated(blocks: List<Block>): List<List<Macros.Step>> {
    val out = ArrayList<List<Macros.Step>>()
    var i = 0
    while (i < blocks.size) {
      var bestLength = 1
      var bestCount = 1
      for (length in 1..minOf(MAX_GROUP, (blocks.size - i) / 2)) {
        var count = 1
        while (i + (count + 1) * length <= blocks.size && (0 until length).all { blocks[i + count * length + it].key == blocks[i + it].key }) {
          count++
        }
        if (count >= 2 && length * count > bestLength * bestCount) {
          bestLength = length
          bestCount = count
        }
      }
      if (bestCount < 2) {
        out += blocks[i].steps
        i++
        continue
      }
      val group = blocks.subList(i, i + bestLength)
      // What was on screen after each time the run was done.
      val after = (0 until bestCount).map { blocks[i + it * bestLength + bestLength - 1].shownAfter }
      val label = group.first().label
      val key = label?.let { labelKey(it.text, it.field) }
      val untilGone =
        key != null &&
          after.dropLast(1).all { it?.contains(key) == true } &&
          after.last()?.let { key !in it } == true
      val steps = ArrayList<Macros.Step>()
      steps +=
        if (untilGone) Macros.RepeatWhile(condition(Macros.ItemOnScreen(label.text, regex = label.regex, field = label.field)))
        else Macros.RepeatTimes(bestCount)
      // Each step waits as long as it did the longest time.
      val longest = group.indices.map { g -> (0 until bestCount).map { blocks[i + it * bestLength + g] } }
      for ((g, block) in group.withIndex()) {
        steps += block.steps.mapIndexed { s, step -> longestTimeout(step, longest[g].map { it.steps[s] }) }
      }
      steps += Macros.EndRepeat
      out += steps
      i += bestLength * bestCount
    }
    return out
  }

  private fun longestTimeout(step: Macros.Step, all: List<Macros.Step>): Macros.Step {
    fun Macros.Step.timeout(): Long =
      when (this) {
        is Macros.Tap -> timeoutMillis
        is Macros.TouchHold -> timeoutMillis
        is Macros.ActOnItem -> timeoutMillis
        else -> 0
      }
    val longest = all.fold(0L) { most, it -> max(most, it.timeout()) }
    return when (step) {
      is Macros.Tap -> step.copy(timeoutMillis = longest)
      is Macros.TouchHold -> step.copy(timeoutMillis = longest)
      is Macros.ActOnItem -> step.copy(timeoutMillis = longest)
      else -> step
    }
  }
}
