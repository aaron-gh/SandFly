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

  /** Entered in New script from code. */
  object Code : ScriptFrom()
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
    val question = ScriptInstallQuestion.of(requireContext(), store, pkg, from)
    confirm(question.title, question.message, question.button) {
      install(pkg, question.granted, question.done, origin)
    }
  }

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
