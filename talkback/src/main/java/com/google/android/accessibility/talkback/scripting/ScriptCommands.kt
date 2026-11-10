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

package com.google.android.accessibility.talkback.scripting

/** A script's command as listed in Commands. */
class ScriptCommandEntry(val value: String, val label: String)

/** The values under which script commands are saved in gestures, shortcuts and menus. */
object ScriptCommands {
  private const val PREFIX = "SCRIPT:"

  @JvmStatic fun isCommand(value: String?): Boolean = value?.startsWith(PREFIX) == true

  /** Returns the saved value of a command. Script ids can't contain a slash. */
  fun valueOf(scriptId: String, commandId: String): String = "$PREFIX$scriptId/$commandId"
}
