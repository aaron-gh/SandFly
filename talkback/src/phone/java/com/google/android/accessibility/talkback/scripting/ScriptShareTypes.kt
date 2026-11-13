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

import java.net.URI
import java.net.URISyntaxException

/**
 * Which files and links [ScriptImportActivity] takes. Messaging apps and file managers often give
 * a .lua file a general type, such as text/plain or application/octet-stream, so the file's name
 * decides.
 */
object ScriptShareTypes {
  private val SCRIPT_TYPES =
    setOf(
      "text/x-lua",
      "application/x-lua",
      "text/x-script.lua",
      "application/zip",
      "application/x-zip-compressed",
    )

  /** Whether [name] is a .lua or .zip file name. */
  fun isScriptName(name: String?): Boolean =
    name != null &&
      (name.trim().endsWith(".lua", ignoreCase = true) ||
        name.trim().endsWith(".zip", ignoreCase = true))

  /**
   * Whether to read a file named [name], of type [mimeType]. A file with a name must be a .lua or
   * .zip file. A file without one is taken only if its type is a script's or a ZIP file's.
   */
  fun acceptsFile(name: String?, mimeType: String?): Boolean =
    if (!name.isNullOrBlank()) isScriptName(name)
    else mimeType?.lowercase()?.substringBefore(';')?.trim() in SCRIPT_TYPES

  /** Whether [url] is a link to a .lua or .zip file, by its path. */
  fun isScriptLink(url: String): Boolean =
    try {
      isScriptName(URI(url).path)
    } catch (e: URISyntaxException) {
      false
    }
}
