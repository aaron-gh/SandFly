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

package com.google.android.accessibility.talkback.actor.gemini.local

import java.io.File
import java.io.IOException
import java.io.InputStream

/** Where the model files live, and how a file gets in by import from storage. */
class LocalModelStore(private val dir: File) {

  fun file(model: LocalModel) = File(dir, model.localFileName)

  fun isInstalled(model: LocalModel): Boolean =
    file(model).let { it.isFile && it.length() == model.sizeBytes }

  /** The model to run: [preferred] if it is installed, otherwise any installed model. */
  fun installed(preferred: LocalModel?): LocalModel? =
    // A model built for an NPU stands in only for itself, since it runs nowhere else.
    listOfNotNull(preferred).plus(LocalModel.choices).firstOrNull { isInstalled(it) }

  fun delete(model: LocalModel) {
    file(model).delete()
    ModelFiles.partFile(file(model)).delete()
  }

  /** The models that are on the phone and complete. */
  fun installedModels(): List<LocalModel> = LocalModel.entries.filter { isInstalled(it) }

  /** Models with part of a download on the phone but no complete file, and the bytes so far. */
  fun unfinishedDownloads(): List<Pair<LocalModel, Long>> =
    LocalModel.entries.mapNotNull { model ->
      partialBytes(model).takeIf { it > 0 && !isInstalled(model) }?.let { model to it }
    }

  /** All the storage that models use, including unfinished downloads. */
  fun usedBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

  /** The bytes of an unfinished download of [model], or 0. */
  fun partialBytes(model: LocalModel): Long =
    ModelFiles.partFile(file(model)).let { if (it.isFile) it.length() else 0L }

  /** Copies [input] in and checks it against [model]. Returns null on success, or why it failed. */
  fun import(model: LocalModel, input: InputStream): ModelFiles.Failure? {
    val target = file(model)
    val part = ModelFiles.partFile(target)
    try {
      dir.mkdirs()
      part.outputStream().use { input.copyTo(it) }
    } catch (_: IOException) {
      part.delete()
      return ModelFiles.Failure.STORAGE
    }
    return ModelFiles.verifyAndInstall(part, target, model.sizeBytes, model.sha256)
  }

  /**
   * Copies a picked file in once, works out which model it is by its size and hash, and installs it
   * under that model's name. Returns null, leaving nothing behind, when it is not a known model.
   */
  fun importUnknown(source: () -> InputStream): LocalModel? {
    val part = File(dir, "import" + ModelFiles.PART_SUFFIX)
    try {
      dir.mkdirs()
      source().use { input -> part.outputStream().use { input.copyTo(it) } }
    } catch (_: IOException) {
      part.delete()
      return null
    }
    val size = part.length()
    val candidates = LocalModel.entries.filter { it.sizeBytes == size }
    val sha = if (candidates.isEmpty()) null else part.inputStream().use { ModelFiles.sha256(it) }
    val model = candidates.firstOrNull { it.sha256.equals(sha, ignoreCase = true) }
    if (model == null || ModelFiles.moveIntoPlace(part, file(model)) != null) {
      part.delete()
      return null
    }
    return model
  }
}
