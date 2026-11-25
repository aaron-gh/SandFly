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

package com.google.android.accessibility.talkback.commands

import com.google.android.accessibility.talkback.preference.search.SettingsSearch

/** Finds commands by name, for the search field of the Commands screen. */
object CommandSearch {

  /** A command found, and the category it is listed in. */
  class Found(val category: Commands.Category, val command: Commands.Command)

  /**
   * Returns the commands of [categories] whose names have every word of [query], also inside a
   * word, ignoring case and accents: first those whose names start with the query, then the rest,
   * each in the order of [categories]. A command in several categories is listed once, with the
   * first. An empty query finds nothing.
   */
  fun find(categories: List<Commands.Category>, query: CharSequence?): List<Found> {
    val words = SettingsSearch.words(query)
    if (words.isEmpty()) {
      return emptyList()
    }
    val start = words.joinToString(" ")
    val starting = mutableListOf<Found>()
    val other = mutableListOf<Found>()
    val seen = HashSet<String>()
    for (category in categories) {
      for (command in category.commands) {
        if (command.value in seen || !SettingsSearch.matches(words, listOf(command.label))) {
          continue
        }
        seen += command.value
        val found = Found(category, command)
        if (SettingsSearch.normalize(command.label).startsWith(start)) {
          starting += found
        } else {
          other += found
        }
      }
    }
    return starting + other
  }
}
