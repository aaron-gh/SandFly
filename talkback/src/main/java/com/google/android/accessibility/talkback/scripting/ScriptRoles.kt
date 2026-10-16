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

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.Role

object ScriptRoles {
  private const val MAX_ROLE = 100

  private val byName: Map<String, Int> =
    (0..MAX_ROLE)
      .associateBy { nameOf(it) }
      .filterKeys { !it.startsWith("(") }

  val names: Set<String>
    get() = byName.keys

  fun of(name: String): Int? = byName[name]

  fun nameOf(role: Int): String = Role.roleToString(role).removePrefix("ROLE_").lowercase()

  fun appRole(node: AccessibilityNodeInfoCompat): String = nameOf(Role.getAppRole(node))

  fun role(node: AccessibilityNodeInfoCompat): String = nameOf(Role.getRole(node))
}
