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
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.utils.output.LanguageGuesser
import com.google.android.accessibility.utils.output.LanguageVoices
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import java.util.concurrent.Executor

/**
 * Tells languages written in the same script apart by their words, with ML Kit language
 * identification on the phone, for [LanguageVoices.PREF_IDENTIFY_FROM_WORDS].
 *
 * Speech is made on the main thread, which doesn't wait for identification: text not identified
 * yet is identified in the background, and its result is used the next time it is said. After a
 * failure, such as while Google Play services downloads language identification, nothing is
 * identified for [RETRY_AFTER_FAILURE_MS].
 */
class WordLanguageGuesser
@VisibleForTesting
internal constructor(private val identifier: Identifier) : LanguageGuesser {

  /** Starts identifying the languages of some text. */
  internal fun interface Identifier {
    /**
     * Starts identifying [text], then calls [done] on any thread with its languages and how sure
     * each is, or null if it failed. Returns false if identification is not available.
     */
    fun identify(text: String, done: (List<Pair<String, Float>>?) -> Unit): Boolean
  }

  /** Until when nothing is identified, after identification failed, in uptime ms. */
  @Volatile private var failedUntil = 0L

  /** Each text's languages, with how sure each is, most recently used last. */
  private val results =
    object : LinkedHashMap<String, List<Pair<String, Float>>>(64, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Pair<String, Float>>>) =
        size > MAX_RESULTS
    }

  /** The texts being identified, guarded by [results]. */
  private val identifying = HashSet<String>()

  override fun guess(text: String, candidates: List<String>): String? {
    synchronized(results) { results[text] }?.let { languages ->
      return pick(languages, candidates).also {
        LogUtils.d(TAG, "Words identified as %s of %s: %s", it, candidates, languages)
      }
    }
    identifyLater(text)
    return null
  }

  /** Identifies [text] in the background for the next time it is said. */
  private fun identifyLater(text: String) {
    if (SystemClock.uptimeMillis() < failedUntil) return
    synchronized(results) {
      // Fast navigation says many texts; the ones that wait are dropped, not queued.
      if (text in identifying || identifying.size >= MAX_IDENTIFYING) return
      identifying += text
    }
    val started =
      try {
        identifier.identify(text) { languages ->
          if (languages == null) {
            // Such as while Google Play services downloads language identification. Not kept, so
            // the text is tried again.
            failedUntil = SystemClock.uptimeMillis() + RETRY_AFTER_FAILURE_MS
          }
          synchronized(results) {
            identifying -= text
            if (languages != null) results[text] = languages
          }
        }
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Language not identified: %s", e)
        false
      }
    if (!started) {
      synchronized(results) { identifying -= text }
    }
  }

  /** Identifies languages with ML Kit. */
  private class MlKitIdentifier : Identifier {
    private var client: LanguageIdentifier? = null

    override fun identify(text: String, done: (List<Pair<String, Float>>?) -> Unit): Boolean {
      val client = client() ?: return false
      client.identifyPossibleLanguages(text).addOnCompleteListener(DIRECT) { task ->
        if (task.isSuccessful) {
          done(task.result.orEmpty().map { it.languageTag to it.confidence })
        } else {
          LogUtils.w(TAG, "Language not identified: %s", task.exception)
          done(null)
        }
      }
      return true
    }

    private fun client(): LanguageIdentifier? =
      client
        ?: try {
          LanguageIdentification.getClient().also { client = it }
        } catch (e: IllegalStateException) {
          // ML Kit isn't set up until the device is first unlocked after it starts.
          LogUtils.w(TAG, "No language identification: %s", e)
          null
        }

    fun close() {
      client?.close()
      client = null
    }
  }

  private fun close() {
    (identifier as? MlKitIdentifier)?.close()
  }

  companion object {
    private const val TAG = "WordLanguageGuesser"
    /** How many texts are identified at once at most. */
    private const val MAX_IDENTIFYING = 8
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
          installed = WordLanguageGuesser(MlKitIdentifier()).also { LanguageVoices.guesser = it }
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
