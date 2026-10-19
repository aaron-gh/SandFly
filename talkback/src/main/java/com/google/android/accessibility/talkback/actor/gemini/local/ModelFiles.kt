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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Hashing and installing model files. A file only reaches its final name after it checks out. */
object ModelFiles {
  const val PART_SUFFIX = ".part"
  private const val BUFFER_SIZE = 64 * 1024

  enum class Failure {
    NETWORK,
    HTTP,
    SIZE_MISMATCH,
    HASH_MISMATCH,
    STORAGE,
    LOW_SPACE,
    /** Android ended the download's time in the background. The part stays, to carry on later. */
    TIME_LIMIT,
  }

  fun sha256(input: InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER_SIZE)
    while (true) {
      val read = input.read(buffer)
      if (read < 0) break
      digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  fun partFile(target: File) = File(target.path + PART_SUFFIX)

  /**
   * Checks the size and hash of [part] and, if they match, moves it to [target]. Deletes [part]
   * when it does not match, so a bad file is never used or resumed.
   */
  fun verifyAndInstall(
    part: File,
    target: File,
    expectedSize: Long,
    expectedSha256: String,
  ): Failure? {
    if (part.length() != expectedSize) {
      part.delete()
      return Failure.SIZE_MISMATCH
    }
    val actual = part.inputStream().use { sha256(it) }
    if (!actual.equals(expectedSha256, ignoreCase = true)) {
      part.delete()
      return Failure.HASH_MISMATCH
    }
    return moveIntoPlace(part, target)
  }

  /** Moves a checked [part] to [target] in one step, so [target] is never half written. */
  fun moveIntoPlace(part: File, target: File): Failure? {
    try {
      Files.move(
        part.toPath(),
        target.toPath(),
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING,
      )
    } catch (_: IOException) {
      return Failure.STORAGE
    }
    return null
  }
}
