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

import com.google.android.accessibility.talkback.R
import java.io.IOException
import java.util.concurrent.Executors

/** Where a script to install came from, as the question before installing says. */
sealed class ScriptFrom {
  object File : ScriptFrom()

  data class Link(val url: String) : ScriptFrom()

  data class Source(val title: String) : ScriptFrom()

  /** The example script that comes with SandFly. */
  object BuiltIn : ScriptFrom()
}

/** A screen that installs scripts, after listing what each one can do. */
abstract class ScriptInstallFragment : ScriptScreenFragment() {

  /**
   * Reads a script and asks before installing it. [from] is where it came from, and [origin] the
   * source and file kept with it for updates, if from a source.
   */
  protected fun read(from: ScriptFrom, origin: String? = null, bytes: () -> ByteArray) {
    executor.execute {
      val result = runCatching { ScriptPackage.read(bytes()) }
      view?.post {
        if (isAdded) result.fold({ confirmInstall(it, from, origin) }) { importFailed(it.message) }
      }
    }
  }

  private fun confirmInstall(pkg: ScriptPackage, from: ScriptFrom, origin: String?) {
    val context = requireContext()
    val translations =
      ScriptTranslations.load(ScriptTranslations.languageOf(context)) { pkg.files[it] }
    val manifest = pkg.manifest.translated(translations::text)
    val warning = manifest.compatibilityWarning(context)
    val existing = store.find(manifest.id)
    val added = manifest.permissions - existing?.manifest?.permissions.orEmpty()
    val granted = (existing?.granted.orEmpty() intersect manifest.permissions) + added
    val isNew = existing == null
    val done =
      if (isNew) getString(R.string.script_installed, manifest.name)
      else getString(R.string.script_updated, manifest.name, manifest.version)
    val title =
      if (isNew) getString(R.string.script_install_title, manifest.name)
      else getString(R.string.script_update_title, manifest.name, manifest.version)
    val button =
      when {
        isNew && warning == null -> R.string.script_install_button
        isNew -> R.string.script_install_anyway
        warning == null -> R.string.script_update_button
        else -> R.string.script_update_anyway
      }
    val message =
      listOf(
          fromText(pkg.signature, from),
          if (existing == null) installMessage(manifest, warning)
          else updateMessage(existing.manifest, manifest, added, warning),
        )
        .joinToString("\n\n")
    confirm(title, message, button) { install(pkg, granted, done, origin) }
  }

  /**
   * Whether SandFly signed the script, and where it came from. A script's author is whatever its
   * manifest says, so only the signature shows that a script is SandFly's own.
   */
  private fun fromText(signature: ScriptSignature.Status, from: ScriptFrom): String {
    val where =
      when (from) {
        ScriptFrom.File -> getString(R.string.script_from_file)
        is ScriptFrom.Link -> getString(R.string.script_from_link, from.url)
        is ScriptFrom.Source -> getString(R.string.script_from_source, from.title)
        ScriptFrom.BuiltIn -> return getString(R.string.script_from_sandfly)
      }
    val signed =
      when (signature) {
        ScriptSignature.Status.SIGNED -> R.string.script_signed
        ScriptSignature.Status.CHANGED -> R.string.script_signature_changed
        ScriptSignature.Status.UNSIGNED -> R.string.script_unsigned
      }
    return getString(signed) + " " + where
  }

  private fun installMessage(manifest: ScriptManifest, warning: String?): String {
    val context = requireContext()
    val permissions =
      if (manifest.permissions.isEmpty()) {
        getString(R.string.script_install_no_permissions)
      } else {
        listOf(
            getString(R.string.script_install_can),
            context.permissionsText(manifest.permissions),
            getString(R.string.script_install_permissions_note),
          )
          .joinToString("\n")
      }
    val commands =
      manifest.commands
        .takeIf { it.isNotEmpty() }
        ?.let { getString(R.string.script_install_adds) + "\n" + context.commandsText(manifest) }
    return listOfNotNull(warning, context.scriptAbout(manifest), permissions, commands)
      .joinToString("\n\n")
  }

  // An update always asks first. A file only has to carry an installed script's id to replace it,
  // whoever wrote it, and it then has that script's permissions, data and settings.
  private fun updateMessage(
    installed: ScriptManifest,
    manifest: ScriptManifest,
    added: Set<ScriptPermission>,
    warning: String?,
  ): String =
    listOfNotNull(
        warning,
        getString(R.string.script_update_replaces, installed.version, installed.author),
        getString(R.string.script_update_other_author, manifest.author)
          .takeIf { manifest.author != installed.author },
        added
          .takeIf { it.isNotEmpty() }
          ?.let {
            getString(R.string.script_update_new_permissions) +
              "\n" +
              requireContext().permissionsText(it)
          },
      )
      .joinToString("\n\n")

  private fun install(pkg: ScriptPackage, granted: Set<ScriptPermission>, done: String, origin: String?) =
    try {
      store.install(pkg, granted, origin = origin)
      toast(done)
    } catch (e: IOException) {
      importFailed(e.message)
    }

  private fun importFailed(reason: String?) =
    toast(getString(R.string.script_import_failed, reason.orEmpty()))

  private companion object {
    val executor = Executors.newSingleThreadExecutor()
  }
}
