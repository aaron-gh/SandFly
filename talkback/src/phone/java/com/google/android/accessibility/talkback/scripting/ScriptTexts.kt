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

import android.content.Context
import com.google.android.accessibility.talkback.R

fun Context.scriptSummary(script: InstalledScript): String =
  listOfNotNull(
      getString(if (script.enabled) R.string.script_state_on else R.string.script_state_off),
      getString(R.string.script_state_incompatible)
        .takeIf { script.manifest.compatibility != ApiCompatibility.COMPATIBLE },
      runsIn(script.manifest),
      getString(R.string.script_version, script.manifest.version),
    )
    .joinToString(". ")

fun Context.scriptAbout(manifest: ScriptManifest): String =
  listOfNotNull(
      manifest.description,
      runsIn(manifest),
      getString(R.string.script_version, manifest.version),
      getString(R.string.script_author, manifest.author),
      getString(R.string.script_api_range, manifest.minApiVersion, manifest.testedApiVersion),
    )
    .joinToString(". ")

fun Context.permissionText(permission: ScriptPermission): String =
  getString(permission.text).let {
    if (permission.sensitive) getString(R.string.script_permission_sensitive, it) else it
  }

fun Context.permissionsText(permissions: Collection<ScriptPermission>): String =
  ScriptPermission.entries.filter { it in permissions }.joinToString("\n") { permissionText(it) }

fun Context.commandsText(manifest: ScriptManifest): String =
  manifest.commands.joinToString("\n") { command ->
    getString(
      R.string.script_install_command,
      command.title,
      command.bindings.joinToString(", ") { bindingText(it) },
    )
  }

fun Context.bindingText(binding: CommandBinding): String =
  when (binding) {
    is CommandBinding.Gesture ->
      getString(R.string.script_binding_gesture, ScriptInput.gestureTitle(this, binding.name))
    is CommandBinding.KeyCombo ->
      getString(R.string.script_binding_keys, ScriptInput.keysTitle(this, binding.keys))
    CommandBinding.MenuItem -> getString(R.string.script_binding_menu)
    CommandBinding.ReadingControl -> getString(R.string.script_binding_control)
  }

private fun Context.runsIn(manifest: ScriptManifest): String =
  if (manifest.isGlobal) {
    getString(R.string.script_runs_everywhere)
  } else {
    getString(
      R.string.script_runs_in,
      manifest.apps.map { it.packageName }.distinct().joinToString(", "),
    )
  }
