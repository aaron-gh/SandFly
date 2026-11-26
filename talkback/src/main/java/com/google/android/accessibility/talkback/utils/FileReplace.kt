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

package com.google.android.accessibility.talkback.utils

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Moves a finished file over another one.
 *
 * [File.renameTo] replaces an existing target on Android and Linux but not on Windows, where the
 * unit tests also run, so a second save there fails. This replaces the target on every platform.
 */
object FileReplace {

  /** Moves [source] to [target], replacing it, and returns whether it worked, like [File.renameTo]. */
  @JvmStatic
  fun replace(source: File, target: File): Boolean =
    try {
      move(source, target)
      true
    } catch (e: IOException) {
      false
    } catch (e: SecurityException) {
      false
    }

  /** Moves [source] to [target] in one step where the file system can, replacing [target]. */
  @Throws(IOException::class)
  private fun move(source: File, target: File) {
    try {
      Files.move(
        source.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE,
      )
    } catch (e: AtomicMoveNotSupportedException) {
      Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
  }
}
