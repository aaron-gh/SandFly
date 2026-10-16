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

import android.util.Log
import com.google.android.libraries.accessibility.utils.log.LogUtils

object ScriptLog {
  const val TAG = "BacktalkScript"
  private const val MAX_ENTRIES = 300

  enum class Level(val priority: Int) {
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR),
  }

  data class Entry(val timeMillis: Long, val level: Level, val message: String)

  private val entries = HashMap<String, ArrayDeque<Entry>>()

  fun add(id: String, level: Level, message: String) {
    // Scripts can log what is on screen or in notifications, so it only goes to logcat at the log
    // level chosen in developer settings. The script's own log screen always has it.
    LogUtils.log(TAG, level.priority, "[%s] %s", id, message)
    synchronized(entries) {
      entries.getOrPut(id) { ArrayDeque() }.apply {
        addLast(Entry(System.currentTimeMillis(), level, message))
        if (size > MAX_ENTRIES) {
          removeFirst()
        }
      }
    }
  }

  fun entries(id: String): List<Entry> = synchronized(entries) { entries[id]?.reversed().orEmpty() }

  fun clear(id: String) {
    synchronized(entries) { entries.remove(id) }
  }
}
