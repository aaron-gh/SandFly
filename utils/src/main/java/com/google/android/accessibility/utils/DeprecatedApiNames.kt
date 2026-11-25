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

package com.google.android.accessibility.utils

import android.util.Log
import java.util.Collections

/**
 * Warnings about the deprecated names of SandFly's APIs for other apps. The APIs are named under
 * io.github.aaron_gh.sandfly. The speech, braille display and direct touch APIs also keep their
 * earlier names under fyi.quin.backtalk, from Backtalk, for apps built against them, and SandFly logs
 * a warning with the tag [TAG] when an app uses one. New APIs and names get no fyi.quin.backtalk
 * name.
 */
object DeprecatedApiNames {
  /** The log tag of the warnings, documented for app developers: `adb logcat -s SandFlyApi`. */
  const val TAG = "SandFlyApi"

  /** At most this many warnings while SandFly runs, whatever apps send. */
  private const val MAX_WARNINGS = 256

  private val warned = Collections.synchronizedSet(HashSet<String>())

  /** Told about each warning that names its app, on the thread of the call that used the name. */
  fun interface Listener {
    fun onDeprecatedName(app: String, legacyName: String, name: String)
  }

  /** Set once when SandFly starts. */
  @JvmStatic @Volatile var listener: Listener? = null

  /**
   * Logs once per app and name, while SandFly runs, that [app] used [legacyName] in place of
   * [name]. [app] is null when SandFly can't tell which app it was.
   */
  @JvmStatic
  fun warn(app: String?, legacyName: String, name: String) {
    val key = "${app.orEmpty()}/$legacyName"
    synchronized(warned) {
      if (warned.size >= MAX_WARNINGS || !warned.add(key)) {
        return
      }
    }
    Log.w(
      TAG,
      "${app ?: "An app"} uses $legacyName, which is deprecated and will be removed. Use $name.",
    )
    if (app != null) {
      listener?.onDeprecatedName(app, legacyName, name)
    }
  }
}
