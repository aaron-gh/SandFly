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

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R

class ScriptDetailFragment : ScriptScreenFragment() {

  public override fun getTitle(): CharSequence =
    store.find(scriptId)?.manifest?.name ?: getText(R.string.title_pref_scripts)

  override fun state(): Any? = store.find(scriptId)?.let { it.manifest to it.revision }

  override fun build(screen: PreferenceScreen) {
    val script = orClose(store.find(scriptId)) ?: return
    val context = requireContext()
    val manifest = script.manifest
    screen.switch(getString(R.string.script_enabled), null, script.enabled) {
      store.setEnabled(scriptId, it)
    }
    screen.info(manifest.name, context.scriptAbout(manifest))
    manifest.compatibilityWarning(context)?.let {
      screen.info(getString(R.string.script_compatibility_title), it)
    }
    manifest.homepage?.let { address ->
      screen.action(getString(R.string.script_homepage)) {
        val title = getString(R.string.script_homepage_confirm_title)
        confirm(title, address, R.string.script_homepage_open) { open(address) }
      }
    }
    if (manifest.settings.isNotEmpty()) {
      screen.link(
        getString(R.string.script_settings_category),
        null,
        ScriptSettingsFragment::class.java,
        scriptId,
      )
    }
    addPermissions(screen.category(getString(R.string.script_permissions_title)), script)
    if (manifest.commands.isNotEmpty()) {
      addCommands(screen.category(getString(R.string.script_commands_category)), manifest)
    }
    addManagement(screen.category(getString(R.string.script_manage_category)), manifest)
  }

  private fun addPermissions(category: PreferenceGroup, script: InstalledScript) {
    val declared = ScriptPermission.entries.filter { it in script.manifest.permissions }
    if (declared.isEmpty()) {
      category.info(getString(R.string.script_permissions_none))
      return
    }
    category.info(getString(R.string.script_permissions_summary))
    for (permission in declared) {
      category.switch(requireContext().permissionText(permission), null, script.has(permission)) {
        store.setGranted(scriptId, permission, it)
      }
    }
  }

  private fun addCommands(category: PreferenceGroup, manifest: ScriptManifest) {
    for (command in manifest.commands) {
      val summary =
        store
          .bindingsOf(scriptId, command)
          .filter { store.isBindingOn(scriptId, command.id, it) }
          .joinToString(", ") { requireContext().bindingText(it) }
          .ifEmpty { getString(R.string.script_binding_none) }
      category.link(command.title, summary, ScriptCommandFragment::class.java, scriptId) {
        putString(ScriptCommandFragment.ARG_COMMAND, command.id)
      }
    }
  }

  private fun addManagement(category: PreferenceGroup, manifest: ScriptManifest) {
    category.action(getString(R.string.script_move_up), getString(R.string.script_order_summary)) {
      store.move(scriptId, -1)
    }
    category.action(getString(R.string.script_move_down)) { store.move(scriptId, 1) }
    category.link(
      getString(R.string.script_log),
      getString(R.string.script_log_summary),
      ScriptLogFragment::class.java,
      scriptId,
    )
    category.action(getString(R.string.script_clear_storage)) {
      store.storageClear(scriptId)
      toast(R.string.script_storage_cleared)
    }
    category.action(getString(R.string.script_remove)) {
      confirm(
        getString(R.string.script_remove_title, manifest.name),
        getString(R.string.script_remove_message),
        R.string.script_remove,
      ) {
        store.remove(scriptId)
        ScriptLog.clear(scriptId)
        toast(getString(R.string.script_removed, manifest.name))
      }
    }
  }

  private fun open(address: String) =
    try {
      startActivity(Intent(Intent.ACTION_VIEW, address.toUri()))
    } catch (e: ActivityNotFoundException) {
      toast(getString(R.string.script_homepage_failed, address))
    }
}
