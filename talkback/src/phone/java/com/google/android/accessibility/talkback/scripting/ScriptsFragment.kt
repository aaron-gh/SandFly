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
    // Asked about once, not again when the screen is recreated.
    arguments?.remove(ARG_IMPORT_FILE)
    if (savedInstanceState != null) return
    // Only ScriptImportActivity's copy, whatever path the arguments name.
    val file = File(requireContext().cacheDir, ScriptImportActivity.SHARED_FILE)
    if (File(path) != file) return
    read {
      try {
        file.readBytes()
      } finally {
        file.delete()
      }
    }
  }

  override fun state(): Any = store.all() to store.allOff

  override fun build(screen: PreferenceScreen) {
    screen.switch(
      getString(R.string.scripts_all_off),
      getString(R.string.scripts_all_off_summary),
      store.allOff,
    ) {
      store.allOff = it
    }
    screen.action(getString(R.string.script_import), getString(R.string.script_import_summary)) {
      openScript.launch(arrayOf("*/*"))
    }
    screen.item(Preference(requireContext()), getString(R.string.script_sources)) {
      fragment = ScriptSourcesFragment::class.java.name
    }
    if (store.find(ScriptStore.EXAMPLE_ID) == null) {
      screen.action(
        getString(R.string.script_add_example),
        getString(R.string.script_add_example_summary),
      ) {
        read { store.exampleBytes() }
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

  private fun importScript(uri: Uri) {
    val resolver = requireContext().contentResolver
    read {
      resolver.openInputStream(uri)?.use { it.readAtMost(ScriptPackage.MAX_BYTES + 1) }
        ?: throw IOException("The file could not be opened")
    }
  }

  companion object {
    /** A script to ask about installing as the screen opens, from [ScriptImportActivity]. */
    const val ARG_IMPORT_FILE = "import_file"
  }
}
