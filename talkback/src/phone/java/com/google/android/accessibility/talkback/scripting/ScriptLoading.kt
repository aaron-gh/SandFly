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

/**
 * Which scripts are loaded. While the screen is off, only scripts whose manifest has
 * `keepLoaded = true` and whose Keep running with the screen off switch the user turned on stay
 * loaded. A script can't turn the switch on itself.
 */
object ScriptLoading {
  /** Whether [script] stays loaded while the screen is off. */
  fun keptWithScreenOff(script: InstalledScript): Boolean =
    script.enabled &&
      script.keepRunning &&
      script.manifest.keepLoaded &&
      script.manifest.isGlobal &&
      !script.manifest.onDemand

  /**
   * The scripts to load, of [scripts]. [demanded] are the onDemand scripts running a command, and
   * [wants] says whether a script for the app in front is wanted.
   */
  fun wanted(
    scripts: List<InstalledScript>,
    screenOn: Boolean,
    demanded: Set<String>,
    wants: (InstalledScript) -> Boolean,
  ): List<InstalledScript> =
    scripts.filter {
      when {
        !screenOn -> keptWithScreenOff(it)
        it.manifest.onDemand -> it.enabled && it.id in demanded
        else -> wants(it)
      }
    }

  /**
   * Whether the script engine runs. With the screen on, it runs for any script that is always
   * loaded or an onDemand script that is loaded; with it off, only for scripts kept loaded.
   */
  fun engineNeeded(
    screenOn: Boolean,
    anyAlwaysLoaded: Boolean,
    onDemandLoaded: Boolean,
    anyKeptWithScreenOff: Boolean,
  ): Boolean = if (screenOn) anyAlwaysLoaded || onDemandLoaded else anyKeptWithScreenOff
}
