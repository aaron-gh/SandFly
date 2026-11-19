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

package com.google.android.accessibility.brailleime

import android.os.SystemClock
import java.io.PrintWriter

/**
 * The braille keyboard's recent orientation decisions, for `adb shell dumpsys activity service
 * <package>/com.google.android.accessibility.brailleime.BrailleIme`. Kept in memory only.
 */
object OrientationTrace {
  private const val MAX_ENTRIES = 300

  private val entries = ArrayDeque<String>()

  @JvmStatic
  fun add(entry: String) {
    val line = "${SystemClock.uptimeMillis()} $entry"
    synchronized(entries) {
      if (entries.size >= MAX_ENTRIES) {
        entries.removeFirst()
      }
      entries.addLast(line)
    }
  }

  @JvmStatic
  fun dump(writer: PrintWriter) {
    writer.println("Orientation trace (uptime ms ${SystemClock.uptimeMillis()}):")
    synchronized(entries) {
      for (entry in entries) {
        writer.println("  $entry")
      }
    }
  }
}
