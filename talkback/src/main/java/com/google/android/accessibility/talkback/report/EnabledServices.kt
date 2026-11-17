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

package com.google.android.accessibility.talkback.report

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.annotation.VisibleForTesting

/**
 * The accessibility services turned on, with the flags, capabilities and event types each has now,
 * for problem reports: another service that requests touch exploration, key events or gestures, or
 * SandFly itself without its key event flag, changes how touches and keys reach SandFly.
 */
object EnabledServices {
  /** One line for each enabled service, or null if there are none or they can't be read. */
  @JvmStatic
  fun describe(context: Context): String? {
    val services =
      runCatching {
          context
            .getSystemService(AccessibilityManager::class.java)
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        }
        .getOrNull()
    if (services.isNullOrEmpty()) return null
    return services.joinToString("\n") { info ->
      val id = runCatching { info.id }.getOrNull() ?: "unknown"
      val events = runCatching { AccessibilityEvent.eventTypeToString(info.eventTypes) }.getOrNull()
      "$id: flags ${flagNames(info.flags)}; " +
        "capabilities ${capabilityNames(info.capabilities)}; " +
        "events $events"
    }
  }

  /** The names of the set bits of [flags], as AccessibilityServiceInfo names them. */
  @VisibleForTesting
  internal fun flagNames(flags: Int): String =
    bitNames(flags) { bit -> AccessibilityServiceInfo.flagToString(bit) }

  private fun capabilityNames(capabilities: Int): String =
    bitNames(capabilities) { bit -> AccessibilityServiceInfo.capabilityToString(bit) }

  /** The [name] of each set bit of [bits], or its hexadecimal value when it has none. */
  @VisibleForTesting
  internal fun bitNames(bits: Int, name: (Int) -> String?): String {
    if (bits == 0) return "none"
    val names = mutableListOf<String>()
    for (shift in 0 until Int.SIZE_BITS) {
      val bit = 1 shl shift
      if ((bits and bit) == 0) continue
      val known = runCatching { name(bit) }.getOrNull()
      names +=
        if (known.isNullOrEmpty() || known == "UNKNOWN") "0x" + Integer.toHexString(bit) else known
    }
    return names.joinToString(" ")
  }
}
