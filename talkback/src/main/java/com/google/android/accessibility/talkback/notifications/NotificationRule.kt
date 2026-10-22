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

package com.google.android.accessibility.talkback.notifications

import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** What the device is doing when a notification arrives, for the rules and settings to check. */
data class DeviceState(
  val screenOn: Boolean = true,
  val locked: Boolean = false,
  /** Addresses of the Bluetooth audio devices connected. */
  val bluetoothDevices: Set<String> = emptySet(),
  val wiredHeadphones: Boolean = false,
  val batteryPercent: Int = 100,
  val charging: Boolean = false,
  val landscape: Boolean = false,
  /** Minutes since midnight. */
  val minuteOfDay: Int = 12 * 60,
  /** 1 for Monday to 7 for Sunday. */
  val dayOfWeek: Int = 1,
  /** The name of the Wi-Fi network, or empty when it isn't connected or can't be read. */
  val wifiName: String = "",
  val foregroundApp: String = "",
  val ringerSilent: Boolean = false,
  val ringerVibrate: Boolean = false,
  val mediaPlaying: Boolean = false,
  val carMode: Boolean = false,
) {
  val headphones: Boolean
    get() = wiredHeadphones || bluetoothDevices.isNotEmpty()
}

/** Whether [minute] is between [start] and [end], which can cross midnight. */
fun isBetween(minute: Int, start: Int, end: Int): Boolean =
  when {
    start == end -> false
    start < end -> minute in start until end
    else -> minute >= start || minute < end
  }

/** A condition of a rule. */
data class RuleCondition(
  val type: Type,
  /** The condition is met when it is not true, such as a device not connected. */
  val negate: Boolean = false,
  /** What the condition looks for: an app, a word, a device's address or a network's name. */
  val value: String = "",
  /** A second value: the name of a device or app to show, or a percentage. */
  val label: String = "",
  val number: Int = 0,
  val startMinutes: Int = 0,
  val endMinutes: Int = 0,
  /** Days of the week the time applies to, 1 for Monday to 7 for Sunday; empty for every day. */
  val days: Set<Int> = emptySet(),
) {
  enum class Type {
    BLUETOOTH_DEVICE,
    WIRED_HEADPHONES,
    BATTERY_BELOW,
    CHARGING,
    LOCKED,
    SCREEN_ON,
    LANDSCAPE,
    TIME,
    WIFI_NETWORK,
    FOREGROUND_APP,
    FROM_APP,
    CONTAINS,
    RINGER_SILENT,
    MEDIA_PLAYING,
  }

  private val textMatch by lazy { TextMatch(value, regex = number == 1) }

  fun isMet(state: DeviceState, info: NotificationInfo): Boolean {
    val met =
      when (type) {
        Type.BLUETOOTH_DEVICE ->
          if (value.isEmpty()) state.bluetoothDevices.isNotEmpty()
          else state.bluetoothDevices.any { it.equals(value, ignoreCase = true) }
        Type.WIRED_HEADPHONES -> state.wiredHeadphones
        Type.BATTERY_BELOW -> state.batteryPercent < number
        Type.CHARGING -> state.charging
        Type.LOCKED -> state.locked
        Type.SCREEN_ON -> state.screenOn
        Type.LANDSCAPE -> state.landscape
        Type.TIME ->
          (days.isEmpty() || state.dayOfWeek in days) &&
            isBetween(state.minuteOfDay, startMinutes, endMinutes)
        Type.WIFI_NETWORK ->
          if (value.isEmpty()) state.wifiName.isNotEmpty()
          else state.wifiName.equals(value, ignoreCase = true)
        Type.FOREGROUND_APP -> state.foregroundApp == value
        Type.FROM_APP -> info.packageName == value
        Type.CONTAINS -> textMatch.matches(info.allText)
        Type.RINGER_SILENT -> state.ringerSilent || state.ringerVibrate
        Type.MEDIA_PLAYING -> state.mediaPlaying
      }
    return met != negate
  }

  fun toJson(): JSONObject =
    JSONObject().apply {
      put("type", type.name)
      if (negate) put("negate", true)
      if (value.isNotEmpty()) put("value", value)
      if (label.isNotEmpty()) put("label", label)
      if (number != 0) put("number", number)
      if (type == Type.TIME) {
        put("start", startMinutes)
        put("end", endMinutes)
        if (days.isNotEmpty()) put("days", JSONArray(days.sorted()))
      }
    }

  companion object {
    fun fromJson(json: JSONObject): RuleCondition? {
      val type = Type.entries.firstOrNull { it.name == json.optString("type") } ?: return null
      val days = json.optJSONArray("days")?.let { a -> (0 until a.length()).map { a.optInt(it) } }
      return RuleCondition(
        type = type,
        negate = json.optBoolean("negate"),
        value = json.optString("value"),
        label = json.optString("label"),
        number = json.optInt("number"),
        startMinutes = json.optInt("start"),
        endMinutes = json.optInt("end"),
        days = days?.filter { it in 1..7 }?.toSet().orEmpty(),
      )
    }
  }
}

/** What a rule does to a notification when its conditions are met. */
data class RuleAction(val type: Type, val value: String = "", val number: Int = 0) {
  enum class Type {
    /** The notification is not read. */
    SKIP,
    /** Only the app's name is said. */
    PRIVATE,
    /** The whole notification is read, even when its app or words would make it private. */
    NOT_PRIVATE,
    /** It is read with [value] as its speech format. */
    FORMAT,
    /** It is read up to [number] words. */
    MAX_WORDS,
    /** It is read in full, without the length limits. */
    NO_LIMIT,
    KEEP_EMOJI,
    KEEP_NUMBERS,
    /** It interrupts a notification being read. */
    PRIORITY,
    /** It is read even while notifications would be quiet, as in quiet hours. */
    ALWAYS_READ,
    /** It is read with the voice profile whose ID is [value], or SandFly's voice when empty. */
    VOICE,
  }

  fun toJson(): JSONObject =
    JSONObject().apply {
      put("type", type.name)
      if (value.isNotEmpty()) put("value", value)
      if (number != 0) put("number", number)
    }

  companion object {
    fun fromJson(json: JSONObject): RuleAction? {
      val type = Type.entries.firstOrNull { it.name == json.optString("type") } ?: return null
      return RuleAction(type, json.optString("value"), json.optInt("number"))
    }
  }
}

/**
 * A rule: when its conditions are met, all of them or any one, its actions apply to the
 * notification. A rule with no conditions applies to every notification.
 */
data class NotificationRule(
  val id: String = UUID.randomUUID().toString(),
  val name: String = "",
  val enabled: Boolean = true,
  val matchAll: Boolean = true,
  val conditions: List<RuleCondition> = emptyList(),
  val actions: List<RuleAction> = emptyList(),
) {
  fun applies(state: DeviceState, info: NotificationInfo): Boolean {
    if (!enabled || actions.isEmpty()) return false
    if (conditions.isEmpty()) return true
    return if (matchAll) conditions.all { it.isMet(state, info) }
    else conditions.any { it.isMet(state, info) }
  }

  fun toJson(): JSONObject =
    JSONObject()
      .put("id", id)
      .put("name", name)
      .put("enabled", enabled)
      .put("all", matchAll)
      .put("conditions", JSONArray(conditions.map { it.toJson() }))
      .put("actions", JSONArray(actions.map { it.toJson() }))

  companion object {
    fun fromJson(json: JSONObject): NotificationRule {
      fun <T> list(key: String, read: (JSONObject) -> T?): List<T> {
        val array = json.optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(read) }
      }
      return NotificationRule(
        id = json.optString("id").ifEmpty { UUID.randomUUID().toString() },
        name = json.optString("name"),
        enabled = json.optBoolean("enabled", true),
        matchAll = json.optBoolean("all", true),
        conditions = list("conditions") { RuleCondition.fromJson(it) },
        actions = list("actions") { RuleAction.fromJson(it) },
      )
    }

    fun listFromJson(json: String?): List<NotificationRule> {
      json ?: return emptyList()
      return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::fromJson) }
      } catch (e: JSONException) {
        emptyList()
      }
    }

    fun listToJson(rules: List<NotificationRule>): String =
      JSONArray(rules.map { it.toJson() }).toString()
  }
}

/** The actions of every rule that applies to a notification, merged. */
data class RuleEffects(
  val skip: Boolean = false,
  val private: Boolean? = null,
  val format: String = "",
  val maxWords: Int = 0,
  val noLimit: Boolean = false,
  val keepEmoji: Boolean = false,
  val keepNumbers: Boolean = false,
  val priority: Boolean = false,
  val alwaysRead: Boolean = false,
  /** The voice profile to read with, as in [AppSettings.voiceProfile], or null for no change. */
  val voiceProfile: String? = null,
  /** The names of the rules that applied, for the history. */
  val ruleNames: List<String> = emptyList(),
) {
  companion object {
    /** Merges the actions of the [rules] that apply, in order; a later rule wins a conflict. */
    fun of(rules: List<NotificationRule>, state: DeviceState, info: NotificationInfo): RuleEffects {
      var effects = RuleEffects()
      for (rule in rules) {
        if (!rule.applies(state, info)) continue
        for (action in rule.actions) {
          effects =
            when (action.type) {
              RuleAction.Type.SKIP -> effects.copy(skip = true)
              RuleAction.Type.PRIVATE -> effects.copy(private = true)
              RuleAction.Type.NOT_PRIVATE -> effects.copy(private = false)
              RuleAction.Type.FORMAT -> effects.copy(format = action.value)
              RuleAction.Type.MAX_WORDS -> effects.copy(maxWords = action.number)
              RuleAction.Type.NO_LIMIT -> effects.copy(noLimit = true)
              RuleAction.Type.KEEP_EMOJI -> effects.copy(keepEmoji = true)
              RuleAction.Type.KEEP_NUMBERS -> effects.copy(keepNumbers = true)
              RuleAction.Type.PRIORITY -> effects.copy(priority = true)
              RuleAction.Type.ALWAYS_READ -> effects.copy(alwaysRead = true)
              RuleAction.Type.VOICE -> effects.copy(voiceProfile = action.value)
            }
        }
        effects = effects.copy(ruleNames = effects.ruleNames + rule.name)
      }
      return effects
    }
  }
}
