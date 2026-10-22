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

/** Why a notification was not read, or that it was. Shown in the notification history. */
enum class Outcome {
  READ,
  STOPPED,
  SPEECH_OFF,
  DO_NOT_DISTURB,
  TOUCHING,
  APP_IGNORED,
  ONGOING,
  FOREGROUND_SERVICE,
  LOW_PRIORITY,
  SYSTEM,
  MEDIA,
  GROUP_SUMMARY,
  PROGRESS,
  REQUIRED_WORDS_MISSING,
  BLOCKED_WORD,
  EMPTY,
  REPEAT,
  COOLDOWN,
  QUIET_HOURS,
  SCREEN_ON,
  SCREEN_OFF,
  LOCKED,
  HEADPHONES_CONNECTED,
  HEADPHONES_NOT_CONNECTED,
  RINGER_SILENT,
  RINGER_VIBRATE,
  MEDIA_PLAYING,
  CAR,
  RULE,
  BUSY,
  /** An incoming call, announced as calls always are. */
  CALL,
}

/** What to do with a notification. */
data class Decision(
  val outcome: Outcome,
  /** What to say, when [outcome] is [Outcome.READ]. */
  val text: String = "",
  val private: Boolean = false,
  /** It interrupts a notification being read. */
  val priority: Boolean = false,
  /** Stop reading it after this many seconds, or 0 to read it to the end. */
  val maxSeconds: Int = 0,
  val rules: List<String> = emptyList(),
  /** [text] before a start shared with the app's last notification was left out. */
  val fullText: String = text,
  /**
   * The ID of the voice profile to read it with, empty for SandFly's voice, or null for the voice
   * chosen for every notification.
   */
  val voiceProfile: String? = null,
) {
  val read: Boolean
    get() = outcome == Outcome.READ
}

/**
 * Remembers the notifications read lately, for the repeat, cooldown and repeated-start settings.
 * Holds at most [MAX] apps and texts.
 */
class RepeatMemory {
  private val lastText = LinkedHashMap<String, Long>(16, 0.75f, true)
  private val lastRead = LinkedHashMap<String, Long>(16, 0.75f, true)
  private val lastSpoken = LinkedHashMap<String, Pair<String, Long>>(16, 0.75f, true)
  /** When each title and text was last let through, and each text with whether a summary had it. */
  private val lastPosted = LinkedHashMap<String, Long>(16, 0.75f, true)
  private val lastBody = LinkedHashMap<String, Pair<Long, Boolean>>(16, 0.75f, true)

  fun postedAt(key: String): Long? = lastPosted[key]

  fun body(key: String): Pair<Long, Boolean>? = lastBody[key]

  fun notePosted(key: String, bodyKey: String, summary: Boolean, now: Long) {
    lastPosted[key] = now
    val before = lastBody[bodyKey]
    lastBody[bodyKey] = now to (summary || (before != null && before.second && now - before.first < DUPLICATE_MS))
    trim(lastPosted)
    trim(lastBody)
  }

  fun seenAt(key: String): Long? = lastText[key]

  fun readAt(packageName: String): Long? = lastRead[packageName]

  fun spoken(packageName: String): Pair<String, Long>? = lastSpoken[packageName]

  fun noteSeen(key: String, now: Long) {
    lastText[key] = now
    trim(lastText)
  }

  fun noteRead(packageName: String, spoken: String, now: Long) {
    lastRead[packageName] = now
    lastSpoken[packageName] = spoken to now
    trim(lastRead)
    trim(lastSpoken)
  }

  fun clear() {
    lastText.clear()
    lastRead.clear()
    lastSpoken.clear()
    lastPosted.clear()
    lastBody.clear()
  }

  private fun <V> trim(map: LinkedHashMap<String, V>) {
    while (map.size > MAX) map.remove(map.keys.first())
  }

  companion object {
    const val MAX = 200
    /**
     * How long the same notification posted again counts as one: apps such as Telegram post a
     * message and a group summary with the same text, or post a message twice.
     */
    const val DUPLICATE_MS = 5_000L
  }
}

/** Decides whether and how to read notifications. Free of Android, so it can be unit tested. */
class NotificationDecider(private val memory: RepeatMemory = RepeatMemory()) {

  /**
   * Decides for [info] with [settings] in [state], at [nowMillis]. [categoryName] is how the
   * notification's category is said, such as "Message", and [time] the time it is said as.
   * Call [markSpoken] once it is read, for the cooldown and repeated-start settings.
   */
  fun decide(
    info: NotificationInfo,
    settings: NotificationSettings,
    state: DeviceState,
    nowMillis: Long,
    categoryName: String = "",
    time: String = "",
  ): Decision {
    val effects = RuleEffects.of(settings.rules, state, info)
    fun skip(outcome: Outcome) = Decision(outcome, rules = effects.ruleNames)

    if (effects.skip) return skip(Outcome.RULE)
    val app = settings.app(info.packageName)
    val mode = settings.modeOf(info.packageName)
    if (mode == AppMode.IGNORE) return skip(Outcome.APP_IGNORED)

    kindOutcome(info, settings)?.let { return skip(it) }
    if (!effects.alwaysRead) stateOutcome(settings, state)?.let { return skip(it) }

    val allText = info.allText
    if (settings.requiredWords.isNotEmpty() && settings.requiredWords.none { it.matches(allText) }) {
      return skip(Outcome.REQUIRED_WORDS_MISSING)
    }
    if (settings.blockedWords.any { it.matches(allText) }) return skip(Outcome.BLOCKED_WORD)

    val private =
      effects.private
        ?: (mode == AppMode.PRIVATE || settings.privateWords.any { it.matches(allText) })
    val spokenName = app.spokenName.ifEmpty { info.appName }
    val format =
      when {
        private -> settings.privateFormat
        effects.format.isNotEmpty() -> effects.format
        app.format.isNotEmpty() -> app.format
        else -> settings.format
      }

    var text = NotificationText.render(format, info, spokenName, categoryName, time)
    if (!private) text = clean(text, settings, effects)
    if (text.isBlank() || (settings.skipEmpty && !private && isEmptyBesidesName(info))) {
      return skip(Outcome.EMPTY)
    }

    if (isDuplicate(info, nowMillis)) return skip(Outcome.REPEAT)

    // Repeats: the same text from the same app within the time set.
    val repeatKey =
      info.packageName + "\u0000" + if (settings.numbersAlike) NotificationText.withoutNumbers(text) else text
    val seen = memory.seenAt(repeatKey)
    memory.noteSeen(repeatKey, nowMillis)
    if (seen != null && settings.repeatSeconds != 0) {
      val within = settings.repeatSeconds < 0 || nowMillis - seen < settings.repeatSeconds * 1000L
      if (within) return skip(Outcome.REPEAT)
    }

    val cooldown = app.cooldownSeconds ?: settings.cooldownSeconds
    val lastRead = memory.readAt(info.packageName)
    if (cooldown > 0 && lastRead != null && nowMillis - lastRead < cooldown * 1000L) {
      return skip(Outcome.COOLDOWN)
    }

    var said = text
    if (settings.skipRepeatedPrefix && !private) {
      memory.spoken(info.packageName)?.let { (previous, at) ->
        if (nowMillis - at < settings.prefixMemorySeconds * 1000L) {
          said = NotificationText.withoutSharedPrefix(text, previous)
        }
      }
    }
    return Decision(
      Outcome.READ,
      text = said,
      private = private,
      priority = effects.priority || mode == AppMode.PRIORITY,
      maxSeconds = if (effects.noLimit) 0 else settings.maxSeconds,
      rules = effects.ruleNames,
      fullText = text,
      voiceProfile = effects.voiceProfile ?: app.voiceProfile,
    )
  }

  /** Remembers that [decision], for [packageName]'s notification, was read at [nowMillis]. */
  fun markSpoken(packageName: String, decision: Decision, nowMillis: Long) =
    memory.noteRead(packageName, decision.fullText, nowMillis)

  /**
   * How [info] is said in a summary of the notifications, or null when the settings leave it
   * out: its app is not read, its kind is skipped, its words block it, or a rule skips it. The
   * time and device state settings don't apply, since the user asked for the summary.
   */
  fun describe(
    info: NotificationInfo,
    settings: NotificationSettings,
    format: String,
    state: DeviceState = DeviceState(),
    categoryName: String = "",
    time: String = "",
  ): String? {
    val effects = RuleEffects.of(settings.rules, state, info)
    val mode = settings.modeOf(info.packageName)
    if (effects.skip || mode == AppMode.IGNORE || kindOutcome(info, settings) != null) return null
    val allText = info.allText
    if (settings.requiredWords.isNotEmpty() && settings.requiredWords.none { it.matches(allText) }) {
      return null
    }
    if (settings.blockedWords.any { it.matches(allText) }) return null
    val private =
      effects.private ?: (mode == AppMode.PRIVATE || settings.privateWords.any { it.matches(allText) })
    val name = settings.app(info.packageName).spokenName.ifEmpty { info.appName }
    val chosen = if (private) settings.privateFormat else effects.format.ifEmpty { format }
    val text = NotificationText.render(chosen, info, name, categoryName, time)
    return (if (private) text else clean(text, settings, effects)).takeIf { it.isNotBlank() }
  }

  /**
   * Whether [info] is the same as one let through moments ago: the same title and text, or the
   * same text where one of the two is a group summary. Records it when it isn't.
   */
  private fun isDuplicate(info: NotificationInfo, nowMillis: Long): Boolean {
    val body = NotificationText.textOf(info).trim().lowercase()
    if (body.isEmpty()) return false
    val bodyKey = info.packageName + "\u0000" + body
    val key = bodyKey + "\u0000" + info.title.trim().lowercase()
    fun recent(at: Long?) = at != null && nowMillis - at in 0 until RepeatMemory.DUPLICATE_MS
    val sameBody = memory.body(bodyKey)
    val duplicate =
      recent(memory.postedAt(key)) ||
        (sameBody != null && recent(sameBody.first) && (info.groupSummary || sameBody.second))
    if (!duplicate) memory.notePosted(key, bodyKey, info.groupSummary, nowMillis)
    return duplicate
  }

  /** Forgets every notification read, such as when the settings change. */
  fun forget() = memory.clear()

  private fun clean(text: String, settings: NotificationSettings, effects: RuleEffects): String {
    var result = NotificationText.replace(text, settings.replacements)
    result = NotificationText.links(result, settings.urls, settings.urlText)
    if (settings.removeEmoji && !effects.keepEmoji) result = NotificationText.removeEmoji(result)
    if (settings.separateDigits && !effects.keepNumbers) {
      result = NotificationText.separateDigits(result, settings.minDigits, settings.digitSeparator)
    }
    if (settings.lowercase) result = result.lowercase()
    if (!effects.noLimit) {
      val maxWords = if (effects.maxWords > 0) effects.maxWords else settings.maxWords
      result = NotificationText.cap(result, maxWords, settings.maxSentences, settings.maxCharacters)
    }
    return result
  }

  private fun isEmptyBesidesName(info: NotificationInfo): Boolean {
    val content = listOf(info.text, info.bigText, info.ticker).plus(info.lines)
    val text = content.joinToString(" ").let { NotificationText.removeEmoji(it) }
    return text.isBlank()
  }

  private fun kindOutcome(info: NotificationInfo, settings: NotificationSettings): Outcome? =
    when {
      settings.skipMedia && info.media -> Outcome.MEDIA
      settings.skipForegroundService && info.foregroundService -> Outcome.FOREGROUND_SERVICE
      settings.skipOngoing && info.ongoing -> Outcome.ONGOING
      settings.skipGroupSummaries && info.groupSummary -> Outcome.GROUP_SUMMARY
      settings.skipProgress && info.progress -> Outcome.PROGRESS
      settings.skipLowPriority && info.lowPriority -> Outcome.LOW_PRIORITY
      settings.skipSystem && info.system -> Outcome.SYSTEM
      else -> null
    }

  private fun stateOutcome(settings: NotificationSettings, state: DeviceState): Outcome? =
    when {
      settings.quietHours &&
        isBetween(state.minuteOfDay, settings.quietStartMinutes, settings.quietEndMinutes) ->
        Outcome.QUIET_HOURS
      !settings.whenScreenOn && state.screenOn -> Outcome.SCREEN_ON
      !settings.whenScreenOff && !state.screenOn -> Outcome.SCREEN_OFF
      !settings.whenLocked && state.locked -> Outcome.LOCKED
      settings.headphones == HeadphoneMode.ONLY_WITH && !state.headphones ->
        Outcome.HEADPHONES_NOT_CONNECTED
      settings.headphones == HeadphoneMode.ONLY_WITHOUT && state.headphones ->
        Outcome.HEADPHONES_CONNECTED
      settings.honourSilent && state.ringerSilent -> Outcome.RINGER_SILENT
      settings.honourVibrate && state.ringerVibrate -> Outcome.RINGER_VIBRATE
      settings.media == MediaBehavior.SKIP && state.mediaPlaying -> Outcome.MEDIA_PLAYING
      settings.skipInCar && state.carMode -> Outcome.CAR
      else -> null
    }
}
