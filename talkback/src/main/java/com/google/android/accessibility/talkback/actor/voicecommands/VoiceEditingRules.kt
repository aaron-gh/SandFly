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

package com.google.android.accessibility.talkback.actor.voicecommands

import android.speech.SpeechRecognizer
import com.google.android.accessibility.talkback.commands.Commands
import java.util.Locale

/** The rules of voice editing, apart from Android: limits, retries, stop phrases and commands. */
object VoiceEditingRules {
  /** The choices of Voice editing time limit, in minutes. */
  @JvmField val TIME_LIMIT_MINUTES = listOf(5, 10, 20, 30)

  const val DEFAULT_TIME_LIMIT_MINUTES = 10

  /** Recognition failures in a row after which voice editing ends. */
  const val MAX_FAILURES = 3

  /** The longest wait before recording again after a failure. */
  const val MAX_RETRY_DELAY_MS = 8_000L

  /**
   * A recording in which nothing was said that ends sooner than this counts as a failure, so that a
   * recognizer that ends at once isn't started again and again.
   */
  const val MIN_RECORDING_MS = 1_500L

  /** The time limit in minutes that the setting [value] holds, or the default. */
  @JvmStatic
  fun timeLimitMinutes(value: String?): Int =
    value?.trim()?.toIntOrNull()?.takeIf { it in TIME_LIMIT_MINUTES } ?: DEFAULT_TIME_LIMIT_MINUTES

  /** Whether a session that started at [startedAt] has run [limitMs] by [now]. */
  @JvmStatic
  fun expired(now: Long, startedAt: Long, limitMs: Long): Boolean = now - startedAt >= limitMs

  /** What voice editing does after a recording that gave no text. */
  enum class AfterNoText {
    /** Nothing was said: record again. */
    LISTEN,
    /** Recognition failed: record again after [retryDelayMs]. */
    RETRY,
    /** Recognition can't go on: end voice editing. */
    STOP,
  }

  /**
   * What follows a recording of [recordedMs] that gave no text and ended with [error], a
   * SpeechRecognizer error or 0, when [failures] recordings in a row failed before it.
   */
  @JvmStatic
  fun afterNoText(error: Int, failures: Int, recordedMs: Long): AfterNoText {
    val nothingSaid =
      error == 0 ||
        error == SpeechRecognizer.ERROR_NO_MATCH ||
        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
    return when {
      error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> AfterNoText.STOP
      nothingSaid && recordedMs >= MIN_RECORDING_MS -> AfterNoText.LISTEN
      failures + 1 >= MAX_FAILURES -> AfterNoText.STOP
      else -> AfterNoText.RETRY
    }
  }

  /** The wait before recording again after the [failures]th failure in a row: 1, 2, 4, 8 seconds. */
  @JvmStatic
  fun retryDelayMs(failures: Int): Long {
    val doublings = (failures - 1).coerceIn(0, 3)
    return minOf(1_000L shl doublings, MAX_RETRY_DELAY_MS)
  }

  /**
   * Whether recording waits longer for SandFly's speech: while it speaks, up to [maxMs], and for up
   * to [graceMs] for speech to start when none has. [elapsed] counts from the start of the wait.
   */
  @JvmStatic
  fun keepWaiting(speaking: Boolean, started: Boolean, elapsed: Long, graceMs: Long, maxMs: Long): Boolean =
    when {
      speaking -> elapsed < maxMs
      !started -> elapsed < graceMs
      else -> false
    }

  /**
   * Whether accessibility focus and input focus are still in the field being edited. Accessibility
   * focus can be on the keyboard, or nowhere, while input focus stays in the field.
   */
  @JvmStatic
  fun stillInField(
    accessibilityFocusInField: Boolean,
    accessibilityFocusNone: Boolean,
    accessibilityFocusOnKeyboard: Boolean,
    inputFocusInField: Boolean,
  ): Boolean =
    when {
      accessibilityFocusInField -> true
      accessibilityFocusNone || accessibilityFocusOnKeyboard -> inputFocusInField
      else -> false
    }

  /** Whether [said] is all one of [phrases], such as "stop editing", once both are normalized. */
  @JvmStatic
  fun isStopPhrase(said: String, phrases: List<String>, locale: Locale): Boolean {
    val normalized = VoiceGrammar.normalize(said, locale)
    if (normalized.isEmpty()) return false
    return phrases.any { VoiceGrammar.normalize(it, locale) == normalized }
  }

  /**
   * The commands of [categories] that voice editing runs: those of the categories titled [titles],
   * Text editing and Navigate by text, without the commands [excluded].
   */
  @JvmStatic
  fun editingCommands(
    categories: List<Commands.Category>,
    titles: Set<Int>,
    excluded: Set<String>,
  ): List<Commands.Command> =
    categories.filter { it.titleRes in titles }.flatMap { it.commands }.filter { it.value !in excluded }

  /**
   * Commands by their normalized names, and by their [aliases], other names by command value; the
   * first command with a name keeps it.
   */
  @JvmStatic
  @JvmOverloads
  fun commandNames(
    commands: List<Commands.Command>,
    locale: Locale,
    aliases: Map<String, List<String>> = emptyMap(),
  ): Map<String, Commands.Command> {
    val names = HashMap<String, Commands.Command>()
    for (command in commands) names.putIfAbsent(VoiceGrammar.normalize(command.label, locale), command)
    addAliases(names, commands, aliases, locale)
    return names
  }

  /** Adds the [aliases] of [commands] to [names], after the names the commands have. */
  @JvmStatic
  fun addAliases(
    names: MutableMap<String, Commands.Command>,
    commands: List<Commands.Command>,
    aliases: Map<String, List<String>>,
    locale: Locale,
  ) {
    if (aliases.isEmpty()) return
    val byValue = commands.associateBy { it.value }
    for ((value, phrases) in aliases) {
      val command = byValue[value] ?: continue
      for (phrase in phrases) {
        val name = VoiceGrammar.normalize(phrase, locale)
        if (name.isNotEmpty()) names.putIfAbsent(name, command)
      }
    }
  }

  /**
   * The phrases of voice editing: joining words, fillers, typing and text editing, without the other
   * patterns. Text editing phrases act on the text only, without activating items.
   */
  @JvmStatic
  @JvmOverloads
  fun phrases(
    chain: List<String>,
    and: List<String>,
    fillersStart: List<String>,
    fillersEnd: List<String>,
    cancel: List<String>,
    type: List<String>,
    run: List<String>,
    text: VoiceGrammar.TextPhrases = VoiceGrammar.TextPhrases(),
  ): VoiceGrammar.Phrases =
    VoiceGrammar.Phrases(
      chain = chain,
      and = and,
      fillersStart = fillersStart,
      fillersEnd = fillersEnd,
      cancel = cancel,
      sendImageDescription = emptyList(),
      sendScreenDescription = emptyList(),
      sendPrevious = emptyList(),
      message = emptyList(),
      call = emptyList(),
      describeScreen = emptyList(),
      describeImage = emptyList(),
      copy = emptyList(),
      wait = emptyList(),
      type = type,
      run = run,
      open = emptyList(),
      tap = emptyList(),
      text = text,
      textFallback = false,
    )

  /**
   * What voice editing looks up: the editing commands by [names], and the custom voice commands
   * [custom] whose command [allowed] lets run. No apps.
   */
  class Resolver(
    private val names: Map<String, Commands.Command>,
    private val custom: List<CustomVoiceCommands.Entry>,
    private val allowed: (String) -> Boolean,
    private val locale: Locale,
  ) : VoiceGrammar.Resolver {
    override fun custom(normalized: String): VoiceStep.Command? {
      val entry =
        custom.firstOrNull { allowed(it.value) && VoiceGrammar.normalize(it.phrase, locale) == normalized }
          ?: return null
      val label = names.values.firstOrNull { it.value == entry.value }?.label ?: entry.phrase
      return VoiceStep.Command(entry.value, label)
    }

    override fun command(normalized: String): VoiceStep.Command? =
      names[normalized]?.let { VoiceStep.Command(it.value, it.label) }

    override fun app(name: String): VoiceStep.OpenApp? = null
  }
}
