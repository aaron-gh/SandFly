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

import android.content.Context
import com.google.android.accessibility.talkback.R

/**
 * The install dialog of a script: its title, message and button, the permissions the script has
 * once installed, and the text shown after installing. Imports and the code editor ask the same.
 */
class ScriptInstallQuestion(
  val title: String,
  val message: String,
  val button: Int,
  val granted: Set<ScriptPermission>,
  val done: String,
) {
  companion object {
    fun of(
      context: Context,
      store: ScriptStore,
      pkg: ScriptPackage,
      from: ScriptFrom,
    ): ScriptInstallQuestion {
      val translations =
        ScriptTranslations.load(ScriptTranslations.languageOf(context)) { pkg.files[it] }
      val manifest = pkg.manifest.translated(translations::text)
      val warning = manifest.compatibilityWarning(context)
      val existing = store.find(manifest.id)
      val added = addedPermissions(existing, manifest.permissions)
      val isNew = existing == null
      val done =
        if (isNew) context.getString(R.string.script_installed, manifest.name)
        else context.getString(R.string.script_updated, manifest.name, manifest.version)
      val title =
        if (isNew) context.getString(R.string.script_install_title, manifest.name)
        else context.getString(R.string.script_update_title, manifest.name, manifest.version)
      val button =
        when {
          isNew && warning == null -> R.string.script_install_button
          isNew -> R.string.script_install_anyway
          warning == null -> R.string.script_update_button
          else -> R.string.script_update_anyway
        }
      val message =
        listOf(
            fromText(context, pkg.signature, from),
            if (existing == null) installMessage(context, manifest, warning)
            else updateMessage(context, existing.manifest, manifest, added, warning),
          )
          .joinToString("\n\n")
      return ScriptInstallQuestion(
        title,
        message,
        button,
        grantedAfter(existing, manifest.permissions),
        done,
      )
    }

    /** The permissions in [permissions] that the installed script [existing] doesn't ask for. */
    fun addedPermissions(
      existing: InstalledScript?,
      permissions: Set<ScriptPermission>,
    ): Set<ScriptPermission> = permissions - existing?.manifest?.permissions.orEmpty()

    /**
     * The permissions a script asking for [permissions] has once it replaces [existing]: the ones
     * the user allowed that it still asks for, and new ones, which the install dialog lists.
     */
    fun grantedAfter(
      existing: InstalledScript?,
      permissions: Set<ScriptPermission>,
    ): Set<ScriptPermission> =
      (existing?.granted.orEmpty() intersect permissions) + addedPermissions(existing, permissions)

    /**
     * Whether SandFly signed the script, and where it came from. A script's author is whatever its
     * manifest says, so only the signature shows that a script is SandFly's own.
     */
    private fun fromText(
      context: Context,
      signature: ScriptSignature.Status,
      from: ScriptFrom,
    ): String {
      val where =
        when (from) {
          ScriptFrom.File -> context.getString(R.string.script_from_file)
          is ScriptFrom.Link -> context.getString(R.string.script_from_link, from.url)
          is ScriptFrom.Source -> context.getString(R.string.script_from_source, from.title)
          ScriptFrom.Code -> context.getString(R.string.script_from_code)
          ScriptFrom.BuiltIn -> return context.getString(R.string.script_from_sandfly)
        }
      val signed =
        when (signature) {
          ScriptSignature.Status.SIGNED -> R.string.script_signed
          ScriptSignature.Status.CHANGED -> R.string.script_signature_changed
          ScriptSignature.Status.UNSIGNED -> R.string.script_unsigned
        }
      return context.getString(signed) + " " + where
    }

    private fun installMessage(
      context: Context,
      manifest: ScriptManifest,
      warning: String?,
    ): String {
      val permissions =
        if (manifest.permissions.isEmpty()) {
          context.getString(R.string.script_install_no_permissions)
        } else {
          listOf(
              context.getString(R.string.script_install_can),
              context.permissionsText(manifest.permissions),
              context.getString(R.string.script_install_permissions_note),
            )
            .joinToString("\n")
        }
      val commands =
        manifest.commands
          .takeIf { it.isNotEmpty() }
          ?.let {
            context.getString(R.string.script_install_adds) + "\n" + context.commandsText(manifest)
          }
      return listOfNotNull(warning, context.scriptAbout(manifest), permissions, commands)
        .joinToString("\n\n")
    }

    // An update always asks first. A file only has to carry an installed script's id to replace
    // it, whoever wrote it, and it then has that script's permissions, data and settings.
    private fun updateMessage(
      context: Context,
      installed: ScriptManifest,
      manifest: ScriptManifest,
      added: Set<ScriptPermission>,
      warning: String?,
    ): String =
      listOfNotNull(
          warning,
          context.getString(R.string.script_update_replaces, installed.version, installed.author),
          context
            .getString(R.string.script_update_other_author, manifest.author)
            .takeIf { manifest.author != installed.author },
          added
            .takeIf { it.isNotEmpty() }
            ?.let {
              context.getString(R.string.script_update_new_permissions) +
                "\n" +
                context.permissionsText(it)
            },
        )
        .joinToString("\n\n")
  }
}
