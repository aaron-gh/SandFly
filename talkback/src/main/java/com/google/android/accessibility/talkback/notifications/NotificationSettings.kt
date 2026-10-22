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

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** How an app's notifications are read. */
enum class AppMode {
  /** As the settings for other apps say. */
  DEFAULT,
  READ,
  /** Only the app's name is said, never what the notification says. */
  PRIVATE,
  /** Read, and they interrupt a notification being read. */
  PRIORITY,
  IGNORE;

  companion object {
    fun of(name: String?, fallback: AppMode = DEFAULT): AppMode =
      entries.firstOrNull { it.name == name } ?: fallback
  }
}

/** What happens to a notification that arrives while another is being read. */
enum class QueueBehavior {
  QUEUE,
  INTERRUPT,
  SKIP;

  companion object {
    fun of(name: String?): QueueBehavior = entries.firstOrNull { it.name == name } ?: QUEUE
  }
}

/** How links are read. */
enum class UrlHandling {
  FULL,
  DOMAIN,
  REPLACE,
  OMIT;

  companion object {
    fun of(name: String?): UrlHandling = entries.firstOrNull { it.name == name } ?: FULL
  }
}

/** When headphones matter. */
enum class HeadphoneMode {
  ALWAYS,
  ONLY_WITH,
  ONLY_WITHOUT;

  companion object {
    fun of(name: String?): HeadphoneMode = entries.firstOrNull { it.name == name } ?: ALWAYS
  }
}

/** What happens while media plays. */
enum class MediaBehavior {
  READ,
  PAUSE,
  SKIP;

  companion object {
    fun of(name: String?): MediaBehavior = entries.firstOrNull { it.name == name } ?: READ
  }
}

/** Which notifications are read while Do Not Disturb is on. */
enum class DndMode {
  NONE,
  /** Those Do Not Disturb lets through, such as from starred contacts. Needs notification access. */
  ALLOWED,
  ALL;

  companion object {
    fun of(name: String?): DndMode = entries.firstOrNull { it.name == name } ?: ALLOWED
  }
}

/** What happens to a notification that arrives while the screen is touched. */
enum class TouchBehavior {
  READ,
  AFTER_TOUCH,
  SKIP;

  companion object {
    fun of(name: String?): TouchBehavior = entries.firstOrNull { it.name == name } ?: AFTER_TOUCH
  }
}

/** The settings of one app. Null values follow the settings for every app. */
data class AppSettings(
  val mode: AppMode = AppMode.DEFAULT,
  /** What the app is called when its notifications are read, or empty for its own name. */
  val spokenName: String = "",
  /** Seconds between two of the app's notifications being read, or null for the general value. */
  val cooldownSeconds: Int? = null,
  /** Speech format for the app, or empty for the general one. */
  val format: String = "",
  /**
   * The ID of the voice profile the app's notifications are read with, empty for SandFly's voice,
   * or null for the general voice.
   */
  val voiceProfile: String? = null,
) {
  val isEmpty: Boolean
    get() =
      mode == AppMode.DEFAULT && spokenName.isEmpty() && cooldownSeconds == null && format.isEmpty() &&
        voiceProfile == null

  fun toJson(): JSONObject =
    JSONObject().apply {
      if (mode != AppMode.DEFAULT) put("mode", mode.name)
      if (spokenName.isNotEmpty()) put("name", spokenName)
      cooldownSeconds?.let { put("cooldown", it) }
      if (format.isNotEmpty()) put("format", format)
      voiceProfile?.let { put("voice", it) }
    }

  companion object {
    fun fromJson(json: JSONObject): AppSettings =
      AppSettings(
        mode = AppMode.of(json.optString("mode")),
        spokenName = json.optString("name"),
        cooldownSeconds = if (json.has("cooldown")) json.optInt("cooldown") else null,
        format = json.optString("format"),
        voiceProfile = if (json.has("voice")) json.optString("voice") else null,
      )
  }
}

/**
 * A word or phrase that a notification is matched against. A [regex] entry is a regular
 * expression; otherwise the text is found anywhere, ignoring case.
 */
data class TextMatch(val text: String, val regex: Boolean = false) {
  private val pattern: Regex? by lazy {
    if (regex) {
      try {
        Regex(text)
      } catch (e: IllegalArgumentException) {
        null
      }
    } else {
      null
    }
  }

  fun matches(subject: String): Boolean =
    // A long text is cut for expressions, which can take long on it.
    if (regex) pattern?.containsMatchIn(subject.take(MAX_REGEX_SUBJECT)) == true
    else text.isNotEmpty() && subject.contains(text, ignoreCase = true)

  fun toJson(): JSONObject = JSONObject().put("text", text).put("regex", regex)

  companion object {
    private const val MAX_REGEX_SUBJECT = 2000

    fun fromJson(json: JSONObject) = TextMatch(json.optString("text"), json.optBoolean("regex"))

    /** Whether [text] is a regular expression that can be used. */
    fun isValidRegex(text: String): Boolean =
      try {
        Regex(text)
        true
      } catch (e: IllegalArgumentException) {
        false
      }
  }
}

/** Text that is said differently: [find] is replaced with [replace]. */
data class Replacement(
  val find: String,
  val replace: String,
  val regex: Boolean = false,
  val wholeWord: Boolean = true,
) {
  private val pattern: Regex? by lazy {
    try {
      when {
        regex -> Regex(find)
        wholeWord -> Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(find) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        else -> Regex(Regex.escape(find), RegexOption.IGNORE_CASE)
      }
    } catch (e: IllegalArgumentException) {
      null
    }
  }

  fun apply(subject: String): String {
    if (find.isEmpty()) return subject
    val pattern = pattern ?: return subject
    return try {
      if (regex) pattern.replace(subject, replace) else pattern.replace(subject, Regex.escapeReplacement(replace))
    } catch (e: RuntimeException) {
      // A replacement that names a group the expression doesn't have.
      subject
    }
  }

  fun toJson(): JSONObject =
    JSONObject().put("find", find).put("replace", replace).put("regex", regex).put("word", wholeWord)

  companion object {
    fun fromJson(json: JSONObject) =
      Replacement(
        json.optString("find"),
        json.optString("replace"),
        json.optBoolean("regex"),
        json.optBoolean("word", true),
      )
  }
}

/**
 * Every setting for reading notifications, read once for each notification. Defaults keep
 * SandFly reading notifications as it always has: the type, title and text, of every app.
 */
data class NotificationSettings(
  val enabled: Boolean = true,
  // Reading.
  val format: String = DEFAULT_FORMAT,
  val privateFormat: String = DEFAULT_PRIVATE_FORMAT,
  val detectLanguage: Boolean = false,
  /** The voice profile notifications are read with, or empty for SandFly's own voice. */
  val voiceProfile: String = "",
  val soundBefore: Boolean = false,
  val delaySeconds: Int = 0,
  val queueBehavior: QueueBehavior = QueueBehavior.QUEUE,
  val repeatMinutes: Int = 0,
  // Apps.
  val otherApps: AppMode = AppMode.READ,
  val apps: Map<String, AppSettings> = emptyMap(),
  // Words.
  val requiredWords: List<TextMatch> = emptyList(),
  val blockedWords: List<TextMatch> = emptyList(),
  val privateWords: List<TextMatch> = emptyList(),
  val replacements: List<Replacement> = emptyList(),
  // Text.
  val removeEmoji: Boolean = false,
  val lowercase: Boolean = false,
  val separateDigits: Boolean = false,
  val minDigits: Int = 5,
  val digitSeparator: String = " ",
  val urls: UrlHandling = UrlHandling.FULL,
  val urlText: String = "",
  val maxWords: Int = 0,
  val maxSentences: Int = 0,
  val maxCharacters: Int = 0,
  val maxSeconds: Int = 0,
  val skipEmpty: Boolean = false,
  // Kinds of notifications.
  val skipOngoing: Boolean = false,
  val skipForegroundService: Boolean = false,
  val skipLowPriority: Boolean = false,
  val skipSystem: Boolean = false,
  val skipMedia: Boolean = true,
  val skipGroupSummaries: Boolean = false,
  val skipProgress: Boolean = false,
  // Repeats.
  val repeatSeconds: Int = 0,
  val numbersAlike: Boolean = false,
  val cooldownSeconds: Int = 0,
  val skipRepeatedPrefix: Boolean = false,
  val prefixMemorySeconds: Int = 60,
  // When to read.
  val whenScreenOn: Boolean = true,
  val whenScreenOff: Boolean = false,
  val whenLocked: Boolean = true,
  val headphones: HeadphoneMode = HeadphoneMode.ALWAYS,
  val honourSilent: Boolean = false,
  val honourVibrate: Boolean = false,
  val media: MediaBehavior = MediaBehavior.READ,
  val dnd: DndMode = DndMode.ALLOWED,
  val touch: TouchBehavior = TouchBehavior.AFTER_TOUCH,
  val duringCalls: Boolean = false,
  val skipInCar: Boolean = false,
  val quietHours: Boolean = false,
  val quietStartMinutes: Int = 22 * 60,
  val quietEndMinutes: Int = 7 * 60,
  // Stop reading.
  val shakeToStop: Boolean = false,
  val shakeStrength: Int = 2,
  val waveToStop: Boolean = false,
  val pocketMode: Boolean = true,
  val volumeKeysStop: Boolean = false,
  val dismissStops: Boolean = false,
  val watchDismissStops: Boolean = true,
  val appCancelStops: Boolean = false,
  // History.
  val history: Boolean = true,
  val rules: List<NotificationRule> = emptyList(),
) {
  /** The settings of [packageName], which are empty when the user set none. */
  fun app(packageName: String): AppSettings = apps[packageName] ?: AppSettings()

  /** How [packageName]'s notifications are read, with the setting for other apps applied. */
  fun modeOf(packageName: String): AppMode =
    app(packageName).mode.takeIf { it != AppMode.DEFAULT } ?: otherApps

  /** Whether the notification listener is needed, which tells when a notification goes away. */
  val needsListener: Boolean
    get() = dismissStops || appCancelStops

  companion object {
    const val DEFAULT_FORMAT = "{category}, {title}, {text}"
    const val DEFAULT_PRIVATE_FORMAT = "{app}"

    /** The keys of the settings, all in SandFly's default preferences. */
    object Keys {
      const val ENABLED = "pref_speak_notifications"
      const val FORMAT = "pref_notification_format"
      const val PRIVATE_FORMAT = "pref_notification_private_format"
      const val DETECT_LANGUAGE = "pref_notification_detect_language"
      const val VOICE_PROFILE = "pref_notification_voice_profile"
      const val SOUND_BEFORE = "pref_notification_sound_before"
      const val DELAY = "pref_notification_delay"
      const val QUEUE = "pref_notification_queue"
      const val REPEAT_MINUTES = "pref_notification_repeat_minutes"
      const val OTHER_APPS = "pref_notification_other_apps"
      const val APPS = "pref_notification_apps"
      const val REQUIRED = "pref_notification_required_words"
      const val BLOCKED = "pref_notification_blocked_words"
      const val PRIVATE = "pref_notification_private_words"
      const val REPLACEMENTS = "pref_notification_replacements"
      const val REMOVE_EMOJI = "pref_notification_remove_emoji"
      const val LOWERCASE = "pref_notification_lowercase"
      const val SEPARATE_DIGITS = "pref_notification_separate_digits"
      const val MIN_DIGITS = "pref_notification_min_digits"
      const val DIGIT_SEPARATOR = "pref_notification_digit_separator"
      const val URLS = "pref_notification_urls"
      const val URL_TEXT = "pref_notification_url_text"
      const val MAX_WORDS = "pref_notification_max_words"
      const val MAX_SENTENCES = "pref_notification_max_sentences"
      const val MAX_CHARACTERS = "pref_notification_max_characters"
      const val MAX_SECONDS = "pref_notification_max_seconds"
      const val SKIP_EMPTY = "pref_notification_skip_empty"
      const val SKIP_ONGOING = "pref_notification_skip_ongoing"
      const val SKIP_FOREGROUND = "pref_notification_skip_foreground"
      const val SKIP_LOW_PRIORITY = "pref_notification_skip_low_priority"
      const val SKIP_SYSTEM = "pref_notification_skip_system"
      const val SKIP_MEDIA = "pref_notification_skip_media"
      const val SKIP_GROUP_SUMMARIES = "pref_notification_skip_group_summaries"
      const val SKIP_PROGRESS = "pref_notification_skip_progress"
      const val REPEAT_SECONDS = "pref_notification_repeat_seconds"
      const val NUMBERS_ALIKE = "pref_notification_numbers_alike"
      const val COOLDOWN = "pref_notification_cooldown"
      const val SKIP_PREFIX = "pref_notification_skip_prefix"
      const val PREFIX_MEMORY = "pref_notification_prefix_memory"
      const val SCREEN_ON = "pref_notification_screen_on"
      const val SCREEN_OFF = "pref_notification_screen_off"
      const val LOCKED = "pref_notification_locked"
      const val HEADPHONES = "pref_notification_headphones"
      const val HONOUR_SILENT = "pref_notification_honour_silent"
      const val HONOUR_VIBRATE = "pref_notification_honour_vibrate"
      const val MEDIA = "pref_notification_media"
      const val DND = "pref_notification_dnd"
      const val TOUCH = "pref_notification_touch"
      const val DURING_CALLS = "pref_notification_during_calls"
      const val SKIP_IN_CAR = "pref_notification_skip_in_car"
      const val QUIET_HOURS = "pref_notification_quiet_hours"
      const val QUIET_START = "pref_notification_quiet_start"
      const val QUIET_END = "pref_notification_quiet_end"
      const val SHAKE = "pref_notification_shake_to_stop"
      const val SHAKE_STRENGTH = "pref_notification_shake_strength"
      const val WAVE = "pref_notification_wave_to_stop"
      const val POCKET = "pref_notification_pocket_mode"
      const val VOLUME_KEYS = "pref_notification_volume_keys_stop"
      const val DISMISS = "pref_notification_dismiss_stops"
      const val WATCH_DISMISS = "pref_notification_watch_dismiss_stops"
      const val APP_CANCEL = "pref_notification_app_cancel_stops"
      const val HISTORY = "pref_notification_history"
      const val RULES = "pref_notification_rules"
    }

    /** Reads the settings from [prefs]. */
    @JvmStatic
    fun read(prefs: SharedPreferences): NotificationSettings {
      val d = NotificationSettings()
      fun b(key: String, default: Boolean) = prefs.getBoolean(key, default)
      fun i(key: String, default: Int) = int(prefs, key, default)
      fun s(key: String, default: String) = prefs.getString(key, null) ?: default
      return NotificationSettings(
        enabled = b(Keys.ENABLED, d.enabled),
        format = s(Keys.FORMAT, d.format).ifBlank { d.format },
        privateFormat = s(Keys.PRIVATE_FORMAT, d.privateFormat).ifBlank { d.privateFormat },
        detectLanguage = b(Keys.DETECT_LANGUAGE, d.detectLanguage),
        voiceProfile = s(Keys.VOICE_PROFILE, ""),
        soundBefore = b(Keys.SOUND_BEFORE, d.soundBefore),
        delaySeconds = i(Keys.DELAY, d.delaySeconds).coerceIn(0, 60),
        queueBehavior = QueueBehavior.of(prefs.getString(Keys.QUEUE, null)),
        repeatMinutes = i(Keys.REPEAT_MINUTES, d.repeatMinutes).coerceAtLeast(0),
        otherApps = AppMode.of(prefs.getString(Keys.OTHER_APPS, null), AppMode.READ)
          .takeIf { it != AppMode.DEFAULT } ?: AppMode.READ,
        apps = apps(prefs.getString(Keys.APPS, null)),
        requiredWords = matches(prefs.getString(Keys.REQUIRED, null)),
        blockedWords = matches(prefs.getString(Keys.BLOCKED, null)),
        privateWords = matches(prefs.getString(Keys.PRIVATE, null)),
        replacements = replacements(prefs.getString(Keys.REPLACEMENTS, null)),
        removeEmoji = b(Keys.REMOVE_EMOJI, d.removeEmoji),
        lowercase = b(Keys.LOWERCASE, d.lowercase),
        separateDigits = b(Keys.SEPARATE_DIGITS, d.separateDigits),
        minDigits = i(Keys.MIN_DIGITS, d.minDigits).coerceIn(2, 20),
        digitSeparator = s(Keys.DIGIT_SEPARATOR, d.digitSeparator).ifEmpty { " " },
        urls = UrlHandling.of(prefs.getString(Keys.URLS, null)),
        urlText = s(Keys.URL_TEXT, d.urlText),
        maxWords = i(Keys.MAX_WORDS, 0).coerceAtLeast(0),
        maxSentences = i(Keys.MAX_SENTENCES, 0).coerceAtLeast(0),
        maxCharacters = i(Keys.MAX_CHARACTERS, 0).coerceAtLeast(0),
        maxSeconds = i(Keys.MAX_SECONDS, 0).coerceAtLeast(0),
        skipEmpty = b(Keys.SKIP_EMPTY, d.skipEmpty),
        skipOngoing = b(Keys.SKIP_ONGOING, d.skipOngoing),
        skipForegroundService = b(Keys.SKIP_FOREGROUND, d.skipForegroundService),
        skipLowPriority = b(Keys.SKIP_LOW_PRIORITY, d.skipLowPriority),
        skipSystem = b(Keys.SKIP_SYSTEM, d.skipSystem),
        skipMedia = b(Keys.SKIP_MEDIA, d.skipMedia),
        skipGroupSummaries = b(Keys.SKIP_GROUP_SUMMARIES, d.skipGroupSummaries),
        skipProgress = b(Keys.SKIP_PROGRESS, d.skipProgress),
        repeatSeconds = i(Keys.REPEAT_SECONDS, 0),
        numbersAlike = b(Keys.NUMBERS_ALIKE, d.numbersAlike),
        cooldownSeconds = i(Keys.COOLDOWN, 0).coerceAtLeast(0),
        skipRepeatedPrefix = b(Keys.SKIP_PREFIX, d.skipRepeatedPrefix),
        prefixMemorySeconds = i(Keys.PREFIX_MEMORY, d.prefixMemorySeconds).coerceAtLeast(1),
        whenScreenOn = b(Keys.SCREEN_ON, d.whenScreenOn),
        whenScreenOff = b(Keys.SCREEN_OFF, d.whenScreenOff),
        whenLocked = b(Keys.LOCKED, d.whenLocked),
        headphones = HeadphoneMode.of(prefs.getString(Keys.HEADPHONES, null)),
        honourSilent = b(Keys.HONOUR_SILENT, d.honourSilent),
        honourVibrate = b(Keys.HONOUR_VIBRATE, d.honourVibrate),
        media = MediaBehavior.of(prefs.getString(Keys.MEDIA, null)),
        dnd = DndMode.of(prefs.getString(Keys.DND, null)),
        touch = TouchBehavior.of(prefs.getString(Keys.TOUCH, null)),
        duringCalls = b(Keys.DURING_CALLS, d.duringCalls),
        skipInCar = b(Keys.SKIP_IN_CAR, d.skipInCar),
        quietHours = b(Keys.QUIET_HOURS, d.quietHours),
        quietStartMinutes = i(Keys.QUIET_START, d.quietStartMinutes).coerceIn(0, 24 * 60 - 1),
        quietEndMinutes = i(Keys.QUIET_END, d.quietEndMinutes).coerceIn(0, 24 * 60 - 1),
        shakeToStop = b(Keys.SHAKE, d.shakeToStop),
        shakeStrength = i(Keys.SHAKE_STRENGTH, d.shakeStrength).coerceIn(1, 3),
        waveToStop = b(Keys.WAVE, d.waveToStop),
        pocketMode = b(Keys.POCKET, d.pocketMode),
        volumeKeysStop = b(Keys.VOLUME_KEYS, d.volumeKeysStop),
        dismissStops = b(Keys.DISMISS, d.dismissStops),
        watchDismissStops = b(Keys.WATCH_DISMISS, d.watchDismissStops),
        appCancelStops = b(Keys.APP_CANCEL, d.appCancelStops),
        history = b(Keys.HISTORY, d.history),
        rules = NotificationRule.listFromJson(prefs.getString(Keys.RULES, null)),
      )
    }

    /**
     * An int setting, which list and text preferences save as a string. A value that can't be
     * read gives [default].
     */
    /**
     * Whether notifications are read while the screen is off. SandFly drops every other event
     * then, and these too when this is off.
     */
    @JvmStatic
    fun readsWhenScreenOff(prefs: SharedPreferences): Boolean = prefs.getBoolean(Keys.SCREEN_OFF, false)

    /** Sets Read when screen off from Verbosity's old switch, [old], when it hasn't been set. */
    @JvmStatic
    fun migrateScreenOff(prefs: SharedPreferences, old: Boolean) {
      if (!prefs.contains(Keys.SCREEN_OFF)) prefs.edit { putBoolean(Keys.SCREEN_OFF, old) }
    }

    fun int(prefs: SharedPreferences, key: String, default: Int): Int {
      if (!prefs.contains(key)) return default
      return try {
        prefs.getString(key, null)?.trim()?.toIntOrNull() ?: default
      } catch (e: ClassCastException) {
        try {
          prefs.getInt(key, default)
        } catch (e: ClassCastException) {
          runCatching { prefs.getLong(key, default.toLong()).toInt() }.getOrDefault(default)
        }
      }
    }

    fun apps(json: String?): Map<String, AppSettings> {
      json ?: return emptyMap()
      return try {
        val obj = JSONObject(json)
        obj.keys().asSequence().associateWith { AppSettings.fromJson(obj.getJSONObject(it)) }
      } catch (e: JSONException) {
        emptyMap()
      }
    }

    fun appsToJson(apps: Map<String, AppSettings>): String =
      JSONObject().apply { apps.filterValues { !it.isEmpty }.forEach { (k, v) -> put(k, v.toJson()) } }
        .toString()

    fun matches(json: String?): List<TextMatch> = list(json) { TextMatch.fromJson(it) }
      .filter { it.text.isNotEmpty() }

    fun matchesToJson(list: List<TextMatch>): String = JSONArray(list.map { it.toJson() }).toString()

    fun replacements(json: String?): List<Replacement> = list(json) { Replacement.fromJson(it) }
      .filter { it.find.isNotEmpty() }

    fun replacementsToJson(list: List<Replacement>): String =
      JSONArray(list.map { it.toJson() }).toString()

    private fun <T> list(json: String?, read: (JSONObject) -> T): List<T> {
      json ?: return emptyList()
      return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(read) }
      } catch (e: JSONException) {
        emptyList()
      }
    }
  }
}
