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

import android.content.SharedPreferences
import android.os.SystemClock
import com.google.android.accessibility.utils.output.LanguageGuesser
import com.google.android.accessibility.utils.output.LanguageVoices
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Tells languages written in the same script apart by their words, with ML Kit language
 * identification on the phone, for [LanguageVoices.PREF_IDENTIFY_FROM_WORDS].
 *
 * Speech is made on the main thread, so a piece of text waits at most [WAIT_MS] for its result.
 * Results are kept, so text identified too late is identified the next time it is said. After a
 * failure, such as while Google Play services downloads language identification, nothing is
 * identified for [RETRY_AFTER_FAILURE_MS].
 */
class WordLanguageGuesser private constructor() : LanguageGuesser {

  private var identifier: LanguageIdentifier? = null

  /** Until when nothing is identified, after identification failed, in uptime ms. */
  @Volatile private var failedUntil = 0L

  /** Each text's languages, with how sure each is, most recently used last. */
  private val results =
    object : LinkedHashMap<String, List<Pair<String, Float>>>(64, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Pair<String, Float>>>) =
        size > MAX_RESULTS
    }

  override fun guess(text: String, candidates: List<String>): String? {
    synchronized(results) { results[text] }?.let {
      return pick(it, candidates)
    }
    if (SystemClock.uptimeMillis() < failedUntil) return null
    val client = client() ?: return null
    val found = AtomicReference<List<Pair<String, Float>>>()
    val done = CountDownLatch(1)
    try {
      client
        .identifyPossibleLanguages(text)
        .addOnCompleteListener(DIRECT) { task ->
          val languages =
            if (task.isSuccessful) {
              task.result.orEmpty().map { it.languageTag to it.confidence }
            } else {
              // Such as while Google Play services downloads language identification. Not kept, so
              // the text is tried again.
              LogUtils.w(TAG, "Language not identified: %s", task.exception)
              failedUntil = SystemClock.uptimeMillis() + RETRY_AFTER_FAILURE_MS
              null
            }
          if (languages != null) {
            synchronized(results) { results[text] = languages }
            found.set(languages)
          }
          done.countDown()
        }
      done.await(WAIT_MS, TimeUnit.MILLISECONDS)
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Language not identified: %s", e)
    }
    val languages = found.get()
    if (languages == null) {
      LogUtils.d(TAG, "Words not identified")
      return null
    }
    return pick(languages, candidates).also {
      LogUtils.d(TAG, "Words identified as %s of %s: %s", it, candidates, languages)
    }
  }

  private fun client(): LanguageIdentifier? =
    identifier
      ?: try {
        LanguageIdentification.getClient().also { identifier = it }
      } catch (e: IllegalStateException) {
        // ML Kit isn't set up until the device is first unlocked after it starts.
        LogUtils.w(TAG, "No language identification: %s", e)
        null
      }

  private fun close() {
    identifier?.close()
    identifier = null
  }

  companion object {
    private const val TAG = "WordLanguageGuesser"
    private const val WAIT_MS = 50L
    private const val RETRY_AFTER_FAILURE_MS = 60_000L
    private const val MIN_CONFIDENCE = 0.5f
    private const val MAX_RESULTS = 300
    private val DIRECT = Executor { it.run() }

    private var installed: WordLanguageGuesser? = null

    /** The most likely of [candidates] in [languages], if likely enough. */
    internal fun pick(languages: List<Pair<String, Float>>, candidates: List<String>): String? =
      languages
        // Romanized text, such as "ja-Latn", is not in its language's script.
        .filter { (tag, _) -> !tag.endsWith("-Latn") }
        .map { (tag, confidence) -> tag.substringBefore('-') to confidence }
        .filter { (language, confidence) -> language in candidates && confidence >= MIN_CONFIDENCE }
        .maxByOrNull { it.second }
        ?.first

    /** Installs the guesser while [LanguageVoices.PREF_IDENTIFY_FROM_WORDS] is on. */
    @JvmStatic
    fun update(prefs: SharedPreferences) {
      if (prefs.getBoolean(LanguageVoices.PREF_IDENTIFY_FROM_WORDS, false)) {
        if (installed == null) {
          installed = WordLanguageGuesser().also { LanguageVoices.guesser = it }
        }
      } else {
        stop()
      }
    }

    @JvmStatic
    fun stop() {
      LanguageVoices.guesser = null
      installed?.close()
      installed = null
    }
  }
}
