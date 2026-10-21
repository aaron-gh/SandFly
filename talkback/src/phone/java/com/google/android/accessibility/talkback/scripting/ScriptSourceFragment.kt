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

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R
import java.util.concurrent.Executors

/**
 * The scripts in one source. Choosing a script downloads it and asks before installing, as Import
 * script does, listing the script's description, author, API versions and what it can do.
 */
open class ScriptSourceFragment : ScriptInstallFragment() {
  protected open val kind = SourceKind.SCRIPTS
  private val source: ScriptSource? by lazy { ScriptSource.parse(scriptId) }
  private var scripts: List<SourceScript>? = null
  private var error: String? = null

  public override fun getTitle(): CharSequence = source?.title ?: getText(R.string.script_sources)

  protected open val failedText: Int = R.string.script_source_failed
  protected open val emptyText: Int = R.string.script_source_empty
  protected open val removeMessage: Int = R.string.script_source_remove_message

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (scripts == null) load()
  }

  override fun state(): Any? = scripts to error

  override fun build(screen: PreferenceScreen) {
    val source = orClose(source) ?: return
    when {
      error != null -> {
        screen.info(getString(failedText, error))
        screen.action(getString(R.string.script_source_retry)) { load() }
      }
      scripts == null -> screen.info(getString(R.string.script_source_loading))
      scripts.isNullOrEmpty() -> screen.info(getString(emptyText))
      else -> scripts?.forEach { script -> screen.action(script.name) { install(script) } }
    }
    screen.category(getString(R.string.script_manage_category)).action(
      getString(R.string.script_source_remove)
    ) {
      confirm(
        getString(R.string.script_source_remove_title, source.title),
        getString(removeMessage),
        R.string.script_source_remove,
      ) {
        ScriptSources.remove(requireContext(), source, kind)
        parentFragmentManager.popBackStack()
      }
    }
  }

  private fun load() {
    val source = source ?: return
    scripts = null
    error = null
    rebuild()
    executor.execute {
      val result = runCatching { ScriptSources.list(source, kind) }
      view?.post {
        if (!isAdded) return@post
        result.fold({ scripts = it }, { error = it.message ?: it.javaClass.simpleName })
        rebuild()
      }
    }
  }

  protected open fun install(script: SourceScript) {
    toast(getString(R.string.script_source_downloading, script.name))
    read { ScriptSources.download(script) }
  }

  private companion object {
    val executor = Executors.newSingleThreadExecutor()
  }
}
