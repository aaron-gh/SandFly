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

import java.util.Locale

/**
 * The macros shown in the Actions menu of the last item asked for, kept for [reuseMillis]. One focus
 * change asks for the Actions of the focused item several times (reading control, usage hints), and
 * an Item on screen test searches every window, so the Show in Actions only when conditions are
 * checked once for all of them.
 */
internal class ShownInActions(private val reuseMillis: Long) {
  private var item: Any? = null
  private var macros: List<Macros.Macro>? = null
  private var shown: List<Macros.Macro> = emptyList()
  private var time = 0L

  /**
   * Returns the [macros] listed for [item] whose Actions condition is met, checked with [check], or
   * the result of the same call less than [reuseMillis] before [now].
   */
  @Synchronized
  fun get(
    item: Any?,
    macros: List<Macros.Macro>,
    now: Long,
    check: (Macros.Test) -> Boolean,
  ): List<Macros.Macro> {
    if (macros.none { it.actionsCondition.clauses.isNotEmpty() }) return macros
    if (item != null && item == this.item && macros == this.macros && now >= time && now - time < reuseMillis) {
      return shown
    }
    val result = shownBy(macros, check)
    this.item = item
    this.macros = macros
    shown = result
    time = now
    return result
  }

  companion object {
    /**
     * The [macros] whose Actions condition is met, checking each distinct test with [check] at most
     * once, as several macros can have the same test.
     */
    internal fun shownBy(macros: List<Macros.Macro>, check: (Macros.Test) -> Boolean): List<Macros.Macro> {
      val results = HashMap<Macros.Test, Boolean>()
      return macros.filter { macro ->
        macro.actionsCondition.isMet { test -> results.getOrPut(test) { check(test) } }
      }
    }
  }
}

/**
 * How long the tests of Show in Actions only when conditions took for one item, by kind of test,
 * for problem reports.
 */
internal class ConditionTiming(private val clock: () -> Long = System::nanoTime) {
  private val counts = LinkedHashMap<String, Int>()
  private val nanos = LinkedHashMap<String, Long>()

  /** Runs [check], the check of [test], and adds the time it took to its kind of test. */
  fun <T> time(test: Macros.Test, check: () -> T): T {
    val start = clock()
    try {
      return check()
    } finally {
      val kind = test::class.java.simpleName
      counts[kind] = (counts[kind] ?: 0) + 1
      nanos[kind] = (nanos[kind] ?: 0L) + (clock() - start)
    }
  }

  /** Whether no test was checked, as when the result for the item was reused. */
  val isEmpty: Boolean
    get() = counts.isEmpty()

  /** Such as "3 tests in 45.0 ms: ItemOnScreen 2 in 40.0 ms, FocusedItem 1 in 5.0 ms". */
  fun describe(): String {
    val total = counts.values.sum()
    return "$total ${if (total == 1) "test" else "tests"} in ${millis(nanos.values.sum())}: " +
      counts.keys.joinToString(", ") { "$it ${counts[it]} in ${millis(nanos[it] ?: 0L)}" }
  }

  private fun millis(nanos: Long): String = String.format(Locale.ROOT, "%.1f ms", nanos / 1_000_000.0)
}
