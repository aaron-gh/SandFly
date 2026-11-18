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

package com.google.android.accessibility.utils.mlkit

import com.google.android.gms.common.Feature
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentifier

/**
 * ML Kit language identification, run in the `:mlkit` process by [MlKitHelperService], in place of
 * `LanguageIdentification.getClient`, which loads Google Play services code into SandFly's process.
 * [threshold] is the confidence threshold, or null for ML Kit's default.
 *
 * Throws [IllegalStateException] when [MlKitHelper.initialize] was not called in this process.
 */
class RemoteLanguageIdentifier
@JvmOverloads
constructor(private val threshold: Float? = null) : LanguageIdentifier {
  private val helper = MlKitHelper.get()
  @Volatile private var closed = false

  override fun identifyLanguage(text: String): Task<String> =
    if (closed) closedTask() else helper.identifyLanguage(text, threshold)

  override fun identifyPossibleLanguages(text: String): Task<List<IdentifiedLanguage>> =
    if (closed) closedTask() else helper.identifyPossibleLanguages(text, threshold)

  /** The identifier in the other process stays for other clients until that process unbinds. */
  override fun close() {
    closed = true
  }

  override fun getOptionalFeatures(): Array<Feature> = emptyArray()

  private fun <T> closedTask(): Task<T> =
    Tasks.forException(MlKitException("Language identifier closed", MlKitException.FAILED_PRECONDITION))
}
