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

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R
import java.io.File
import java.io.IOException

class ScriptsFragment : ScriptInstallFragment() {
  private val openScript =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let(::importScript)
    }

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_scripts)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val path = arguments?.getString(ARG_IMPORT_FILE) ?: return
    val link = arguments?.getString(ARG_IMPORT_LINK)
    // Asked about once, not again when the screen is recreated.
    arguments?.remove(ARG_IMPORT_FILE)
    arguments?.remove(ARG_IMPORT_LINK)
    if (savedInstanceState != null) return
    // Only ScriptImportActivity's copy, whatever path the arguments name.
    val file = File(requireContext().cacheDir, ScriptImportActivity.SHARED_FILE)
    if (File(path) != file) return
    read(link?.let { ScriptFrom.Link(it) } ?: ScriptFrom.File) {
      try {
        file.readBytes()
      } finally {
        file.delete()
      }
    }
  }

  private var checking = false

  override fun state(): Any =
    listOf(store.all(), store.allOff, checking, ScriptUpdater.available(requireContext()))

  override fun build(screen: PreferenceScreen) {
    screen.switch(
      getString(R.string.scripts_all_off),
      getString(R.string.scripts_all_off_summary),
      store.allOff,
    ) {
      store.allOff = it
    }
    screen.action(getString(R.string.script_builder_new)) { newScript() }
    screen.action(getString(R.string.script_code_new)) {
      startActivity(ScriptCodeActivity.newScriptIntent(requireContext()))
    }
    com.google.android.accessibility.talkback.sandflyai.SandflyAi.features?.let { pro ->
      screen.action(getString(R.string.script_ai_build)) { pro.buildScript(requireContext(), null) }
    }
    screen.action(getString(R.string.script_import), getString(R.string.script_import_summary)) {
      openScript.launch(arrayOf("*/*"))
    }
    screen.item(Preference(requireContext()), getString(R.string.script_sources)) {
      fragment = ScriptSourcesFragment::class.java.name
    }
    screen.switch(
      getString(R.string.scripts_auto_update),
      null,
      ScriptUpdater.isAutomatic(requireContext()),
    ) {
      ScriptUpdater.setAutomatic(requireContext(), it)
    }
    screen.action(
      getString(if (checking) R.string.scripts_checking_updates else R.string.scripts_check_updates)
    ) {
      checkForUpdates()
    }
    val available = ScriptUpdater.available(requireContext())
    if (available.isNotEmpty()) {
      val category = screen.category(getString(R.string.scripts_updates_category))
      available.forEach { update ->
        category.action(getString(R.string.script_update_available, update.name, update.version)) {
          toast(getString(R.string.script_source_downloading, update.name))
          val context = requireContext().applicationContext
          read(ScriptFrom.Source(ScriptUpdater.sourceTitle(context, update)), update.origin) {
            ScriptUpdater.download(context, update)
          }
        }
      }
    }
    if (store.find(ScriptStore.EXAMPLE_ID) == null) {
      screen.action(
        getString(R.string.script_add_example),
        getString(R.string.script_add_example_summary),
      ) {
        read(ScriptFrom.BuiltIn) { store.exampleBytes() }
      }
    }
    val (global, app) = store.all().partition { it.manifest.isGlobal }
    if (global.isEmpty() && app.isEmpty()) screen.info(getString(R.string.scripts_none))
    for ((title, scripts) in
      listOf(R.string.scripts_category_app to app, R.string.scripts_category_global to global)) {
      if (scripts.isEmpty()) continue
      val category = screen.category(getString(title))
      scripts.forEach {
        category.link(
          it.manifest.name,
          requireContext().scriptSummary(it),
          ScriptDetailFragment::class.java,
          it.id,
        )
      }
    }
  }

  private fun checkForUpdates() {
    if (checking) return
    checking = true
    rebuild()
    val context = requireContext()
    val started =
      ScriptUpdater.check(context, install = ScriptUpdater.isAutomatic(context)) { result ->
        checking = false
        if (!isAdded) return@check
        rebuild()
        toast(
          when {
            result.updated.isNotEmpty() ->
              resources.getQuantityString(
                R.plurals.scripts_updated,
                result.updated.size,
                result.updated.size,
                result.updated.joinToString(", "),
              )
            result.available.isNotEmpty() -> getString(R.string.scripts_updates_to_approve)
            result.failed > 0 -> getString(R.string.scripts_update_check_failed)
            else -> getString(R.string.scripts_up_to_date)
          }
        )
      }
    // The daily check is running and calls only its own callback.
    if (!started) {
      checking = false
      rebuild()
      toast(getString(R.string.scripts_checking_updates))
    }
  }

  private fun newScript() {
    val context = requireContext()
    val field =
      android.widget.EditText(context).apply {
        hint = getString(R.string.script_builder_name)
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    val padding = (20 * resources.displayMetrics.density).toInt()
    val layout = android.widget.FrameLayout(context).apply { setPadding(padding, padding / 2, padding, 0); addView(field) }
    dialog(getString(R.string.script_builder_new)) {
      setView(layout)
      setPositiveButton(android.R.string.ok) { _, _ ->
        val name = field.text.toString().trim()
        if (name.isEmpty()) return@setPositiveButton
        val id =
          try {
            ScriptBuilder.create(context, name)
          } catch (e: IOException) {
            toast(getString(R.string.script_builder_save_failed, e.message))
            return@setPositiveButton
          }
        onPreferenceTreeClick(
          Preference(context).apply {
            fragment = ScriptBuilderFragment::class.java.name
            extras.putString(ARG_ID, id)
          }
        )
      }
    }
    field.requestFocus()
  }

  private fun importScript(uri: Uri) {
    val resolver = requireContext().contentResolver
    read(ScriptFrom.File) {
      resolver.openInputStream(uri)?.use { it.readAtMost(ScriptPackage.MAX_BYTES + 1) }
        ?: throw IOException("The file could not be opened")
    }
  }

  companion object {
    /** A script to ask about installing as the screen opens, from [ScriptImportActivity]. */
    const val ARG_IMPORT_FILE = "import_file"
    /** The link [ARG_IMPORT_FILE] was downloaded from, if it wasn't a file. */
    const val ARG_IMPORT_LINK = "import_link"
  }
}
