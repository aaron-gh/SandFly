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

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.google.android.accessibility.talkback.Pipeline
import java.io.IOException
import org.json.JSONException

object ScriptHostFactory {
  @JvmStatic
  fun create(
    service: AccessibilityService,
    feedback: Pipeline.FeedbackReturner,
    resume: Runnable,
    services: ScriptServices,
  ): ScriptHost? = LazyScriptHost(service, feedback, resume, services)

  /** Returns the commands of installed scripts, without those of turned off scripts unless [all]. */
  @JvmStatic
  fun commands(context: Context, all: Boolean): List<ScriptCommandEntry> =
    ScriptStore.get(context).all().filter { all || it.enabled }.flatMap { script ->
      script.manifest.commands.map {
        ScriptCommandEntry(
          ScriptCommands.valueOf(script.id, it.id),
          "${script.manifest.name}: ${it.title}",
        )
      }
    }

  /** The files of each installed script, by script ID and by path, for Export settings. */
  @JvmStatic
  @Throws(IOException::class)
  fun scriptFiles(context: Context): Map<String, Map<String, ByteArray>> =
    ScriptStore.get(context).codeFiles()

  /**
   * Whether [files] are the code of a script with the ID [id]: a main.lua whose manifest has that
   * ID, within the limits of a script file.
   */
  @JvmStatic
  fun isScript(id: String, files: Map<String, ByteArray>): Boolean {
    if (files.size > ScriptPackage.MAX_FILES) return false
    if (files.values.sumOf { it.size.toLong() } > ScriptPackage.MAX_BYTES) return false
    val main = files[ScriptPackage.MAIN] ?: return false
    if (main.size > ScriptPackage.MAX_LUA_BYTES) return false
    return try {
      ScriptManifest.parse(ManifestReader.read(main.decodeToString()), strict = false).id == id
    } catch (e: ManifestException) {
      false
    } catch (e: JSONException) {
      false
    }
  }

  /**
   * Writes the code of each of [scripts], by script ID and by path, replacing the code of installed
   * scripts with those IDs. The list of scripts comes from the settings; [reloadScripts] reads it.
   */
  @JvmStatic
  @Throws(IOException::class)
  fun restoreScripts(context: Context, scripts: Map<String, Map<String, ByteArray>>) {
    val store = ScriptStore.get(context)
    for ((id, files) in scripts) store.restoreCode(id, files)
  }

  /** Reads the list of scripts again, after Import settings replaced it. */
  @JvmStatic fun reloadScripts(context: Context) = ScriptStore.get(context).reload()
}
