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

class BoundCommand(val runtime: ScriptRuntime, val command: ScriptCommand)

class BoundNavigation(val runtime: ScriptRuntime, val index: Int, val navigation: ScriptNavigation)

class ScriptBindings
private constructor(
  val gestures: Map<String, BoundCommand>,
  val keys: Map<ScriptInput.Keys, BoundCommand>,
  val menu: List<BoundCommand>,
  val controls: List<BoundCommand>,
  val navigation: List<BoundNavigation>,
) {
  val layers: Set<ScriptInput.Keys> =
    keys.keys.filter { it.next != null }.mapTo(HashSet()) { it.first }

  companion object {
    val EMPTY = ScriptBindings(emptyMap(), emptyMap(), emptyList(), emptyList(), emptyList())

    fun of(runtimes: List<ScriptRuntime>, store: ScriptStore): ScriptBindings {
      val gestures = HashMap<String, BoundCommand>()
      val keys = HashMap<ScriptInput.Keys, BoundCommand>()
      val menu = mutableListOf<BoundCommand>()
      val controls = mutableListOf<BoundCommand>()
      for (runtime in runtimes) {
        val commands = runtime.script.manifest.commands.filter { it.id in runtime.commands }
        if (commands.isEmpty() || !runtime.allowed(ScriptPermission.INPUT, "Commands")) continue
        for (command in commands) {
          val bound = BoundCommand(runtime, command)
          for (binding in store.bindingsOf(runtime.id, command)) {
            if (!store.isBindingOn(runtime.id, command.id, binding)) continue
            when (binding) {
              is CommandBinding.Gesture -> gestures.putIfAbsent(binding.name, bound)
              is CommandBinding.KeyCombo -> keys.putIfAbsent(binding.keys, bound)
              CommandBinding.MenuItem -> menu += bound
              CommandBinding.ReadingControl -> controls += bound
            }
          }
        }
      }
      val navigation =
        runtimes
          .filter {
            it.script.manifest.navigation.isNotEmpty() &&
              it.allowed(ScriptPermission.INPUT, "Navigation")
          }
          .flatMap { runtime ->
            runtime.script.manifest.navigation.mapIndexed { index, item ->
              BoundNavigation(runtime, index, item)
            }
          }
      return ScriptBindings(gestures, keys, menu, controls, navigation)
    }
  }
}
