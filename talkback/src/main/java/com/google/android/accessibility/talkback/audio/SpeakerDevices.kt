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

package com.google.android.accessibility.talkback.audio

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R

/**
 * The Bluetooth, wired and USB outputs, such as a Bluetooth speaker, that move Backtalk's audio to
 * the phone speaker while they are connected, and the outputs Backtalk has seen connected, to choose
 * them from. Outputs are known by the names [AudioDeviceRouter.getDeviceDisplayName] gives them.
 */
object SpeakerDevices {
  /** How many outputs Backtalk remembers to offer. */
  private const val MAX_SEEN = 20
  private const val SEPARATOR = "\n"

  /** Returns the names of the outputs chosen to move Backtalk's audio to the phone speaker. */
  @JvmStatic
  fun chosen(prefs: SharedPreferences, context: Context): Set<String> =
    prefs.getStringSet(context.getString(R.string.pref_speaker_devices_key), null) ?: emptySet()

  /** Returns the outputs Backtalk has seen connected, the most recent first. */
  @JvmStatic
  fun seen(prefs: SharedPreferences, context: Context): List<String> =
    prefs
      .getString(context.getString(R.string.pref_audio_outputs_seen_key), null)
      ?.split(SEPARATOR)
      ?.filter { it.isNotEmpty() } ?: emptyList()

  /** Remembers [names] as the outputs seen most recently. */
  @JvmStatic
  fun remember(prefs: SharedPreferences, context: Context, names: Collection<String>) {
    val seen = seen(prefs, context)
    val updated = merged(names, seen).take(MAX_SEEN)
    if (updated != seen) {
      prefs
        .edit()
        .putString(
          context.getString(R.string.pref_audio_outputs_seen_key),
          updated.joinToString(SEPARATOR),
        )
        .apply()
    }
  }

  /** Returns whether any of the outputs [connected] is one of the [chosen] ones. */
  @JvmStatic
  fun anyChosen(connected: Collection<String>, chosen: Set<String>): Boolean =
    connected.any { name -> chosen.any { it.equals(name, ignoreCase = true) } }

  /**
   * Returns the outputs to choose from: those [connected] now, then those [seen] before, then
   * [chosen] ones not seen lately, each once.
   */
  @JvmStatic
  fun choices(
    connected: Collection<String>,
    seen: List<String>,
    chosen: Set<String>,
  ): List<String> = merged(merged(connected, seen), chosen.sorted())

  /** Returns [first] then the names of [then] not in it, each once, ignoring case. */
  private fun merged(first: Collection<String>, then: Collection<String>): List<String> {
    val result = mutableListOf<String>()
    for (name in first + then) {
      if (name.isNotBlank() && result.none { it.equals(name, ignoreCase = true) }) {
        result.add(name)
      }
    }
    return result
  }
}
