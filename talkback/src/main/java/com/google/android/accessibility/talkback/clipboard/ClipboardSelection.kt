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

package com.google.android.accessibility.talkback.clipboard

/** The texts checked in the clipboard history's Select page, by their whole text. */
internal class ClipboardSelection {
  private val checked = HashSet<String>()

  val count: Int
    get() = checked.size

  fun isChecked(text: String): Boolean = text in checked

  /** Checks [text], or unchecks it when it is checked. Returns whether it is checked now. */
  fun toggle(text: String): Boolean =
    if (checked.remove(text)) {
      false
    } else {
      checked.add(text)
      true
    }

  /** Unchecks the texts no longer among [entries]. */
  fun retain(entries: List<String>) {
    checked.retainAll(entries.toHashSet())
  }

  /** The checked texts among [entries], in the order of [entries]. */
  fun checkedIn(entries: List<String>): List<String> = entries.filter { it in checked }

  fun clear() {
    checked.clear()
  }
}
