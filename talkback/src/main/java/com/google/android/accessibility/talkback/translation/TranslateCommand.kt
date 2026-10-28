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

package com.google.android.accessibility.talkback.translation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.icu.text.ListFormatter
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LocaleSpan
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.translation.TranslationFailure.Kind
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.SpannableUtils
import com.google.android.accessibility.utils.output.EmojiSpeech
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import java.util.Locale

/**
 * The Translate last spoken phrase gesture action: translates what SandFly said last, as Translation
 * settings say, and speaks the translation. Done again right after, it translates the same text
 * again rather than the translation, so that a change of language in between takes effect.
 */
class TranslateCommand(private val service: TalkBackService) {
  private val prefs = SharedPreferencesUtils.getSharedPreferences(service)
  private var translator: Translator? = null

  /** The text last translated, and what was spoken for its translation. */
  private var original: String? = null
  private var spoken: String? = null
  /** The last translation, and the settings and text it was made with. */
  private var cached: Translation? = null
  private var cachedFor: String? = null

  fun translateLast() {
    val last = service.speechController.lastUtterance
    // What was on screen, such as emoji, not the words speech used for it, as Copy does.
    val text =
      last?.let { EmojiSpeech.restoreOriginalText(SpannableUtils.getCopyableText(it.aggregateText)) }
        ?.toString()
        ?.trim()
        .orEmpty()
    val again = text.isNotEmpty() && (text == spoken || text == cached?.text)
    val source = if (again) original ?: text else text
    if (source.isEmpty()) return status(service.getString(R.string.translation_nothing))
    val translator = translator ?: Translator(service).also { translator = it }
    val key = settingsKey(translator.service()) + "\n" + source
    cached?.let { if (key == cachedFor) return speak(source, it) }
    translator.translate(source) { result ->
      when (result) {
        is Translator.Result.Done -> {
          cached = result.translation
          cachedFor = key
          speak(source, result.translation)
        }
        is Translator.Result.Downloading ->
          status(service.getString(R.string.translation_downloading, names(result.languages)))
        is Translator.Result.Failed -> status(failureText(result.failure, result.service))
      }
    }
  }

  fun shutdown() {
    translator?.shutdown()
    translator = null
  }

  private fun speak(source: String, translation: Translation) {
    if (TranslationLanguages.same(translation.from, translation.to) && translation.text == source) {
      return status(service.getString(R.string.translation_already_in, TranslationLanguages.name(translation.to)))
    }
    val text = SpannableString(translation.text)
    if (TranslationSettings.targetVoice(prefs)) {
      text.setSpan(LocaleSpan(Locale.forLanguageTag(translation.to)), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    val speech = SpannableStringBuilder()
    val from = translation.from
    if (TranslationSettings.saySource(prefs) && from != null) {
      speech.append(service.getString(R.string.translation_from, TranslationLanguages.name(from))).append(' ')
    }
    speech.append(text)
    original = source
    spoken = speech.toString()
    if (TranslationSettings.copy(prefs)) {
      (service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText(null, translation.text))
    }
    // Kept in the speech history, so that Repeat, Spell and Copy last spoken phrase have it.
    service.speechController.speak(speech, EVENT_ID_UNTRACKED, options(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL))
  }

  /** Speaks a message about translating, which stays out of the history so it isn't translated next. */
  private fun status(text: String) {
    service.speechController.speak(
      text,
      EVENT_ID_UNTRACKED,
      options(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL or FeedbackItem.FLAG_NO_HISTORY),
    )
  }

  private fun options(flags: Int): SpeechController.SpeakOptions =
    SpeechController.SpeakOptions.create().setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT).setFlags(flags)

  private fun settingsKey(translationService: TranslationService): String =
    listOf(
        translationService.id,
        TranslationSettings.source(prefs),
        TranslationSettings.target(prefs),
        TranslationSettings.secondTarget(prefs),
      )
      .joinToString("|")

  private fun names(languages: List<String>): String =
    ListFormatter.getInstance().format(languages.map { TranslationLanguages.name(it) })

  private fun failureText(failure: TranslationFailure, translationService: TranslationService): String {
    val name = service.getString(translationService.label)
    return when (failure.kind) {
      Kind.NOT_SET_UP ->
        when {
          translationService == TranslationService.AI -> service.getString(R.string.ai_service_not_set_up)
          translationService.needsKey -> service.getString(R.string.translation_error_no_key, name)
          else -> service.getString(R.string.translation_error_no_address, name)
        }
      Kind.CONNECTION ->
        if (translationService == TranslationService.ON_DEVICE) service.getString(R.string.translation_error_download)
        else service.getString(R.string.translation_error_connection, name)
      Kind.API_KEY -> service.getString(R.string.translation_error_api_key, name)
      Kind.QUOTA -> service.getString(R.string.translation_error_quota, name)
      Kind.LANGUAGE ->
        service.getString(R.string.translation_error_language, name, TranslationLanguages.name(failure.detail))
      Kind.NOT_DETECTED -> service.getString(R.string.translation_error_not_detected)
      Kind.DOWNLOADING -> service.getString(R.string.translation_error_downloading)
      Kind.DETECTION_DOWNLOADING -> service.getString(R.string.translation_error_detection_downloading)
      Kind.OTHER ->
        when {
          // The AI service's failures come in words already, as Describe image speaks them.
          translationService == TranslationService.AI && failure.detail.isNotEmpty() -> failure.detail
          failure.detail.isEmpty() -> service.getString(R.string.translation_error, name)
          else -> service.getString(R.string.translation_error_details, name, failure.detail)
        }
    }
  }
}
