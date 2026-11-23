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
