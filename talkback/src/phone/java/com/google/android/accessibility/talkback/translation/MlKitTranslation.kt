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

import android.app.DownloadManager
import android.os.SystemClock
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * ML Kit translation, on the phone. Each language's model, about 30 MB, is downloaded from Google
 * first; translation between two languages other than English goes through English, so its model
 * is needed too.
 */
class MlKitTranslation : OnDeviceTranslation {
  private val models = RemoteModelManager.getInstance()
  /** ML Kit's code for each of [languages]. */
  private val codes: Map<String, String> =
    TranslateLanguage.getAllLanguages()
      .mapNotNull { code -> TranslationLanguages.normalize(code)?.let { it to code } }
      .toMap()
  // When each language's download started.
  private val downloading = mutableMapOf<String, Long>()
  private var identifier: LanguageIdentifier? = null
  /** A client for each language pair in use, the least recently used closed beyond [MAX_CLIENTS]. */
  private val translators =
    object : LinkedHashMap<Pair<String, String>, Translator>(MAX_CLIENTS + 1, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<String, String>, Translator>): Boolean {
        if (size <= MAX_CLIENTS) return false
        eldest.value.close()
        return true
      }
    }

  override val languages: Set<String> = codes.keys

  override fun detect(text: String, done: (language: String?, unavailable: Boolean) -> Unit) {
    val client =
      identifier
        ?: LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder().setConfidenceThreshold(MIN_CONFIDENCE).build()
          )
          .also { identifier = it }
    client
      .identifyLanguage(text)
      .addOnSuccessListener { code ->
        // Romanized text, such as "ja-Latn", is not in its language's script, which models expect.
        val language = if (code.endsWith("-Latn")) null else TranslationLanguages.normalize(code)
        done(language?.takeIf { it in languages }, false)
      }
      .addOnFailureListener { e ->
        LogUtils.w(TAG, "Language not identified: %s", e)
        // Google Play services downloads language identification when it is first asked for.
        done(null, (e as? MlKitException)?.errorCode == MlKitException.UNAVAILABLE)
      }
  }

  override fun downloaded(done: (Set<String>) -> Unit) {
    models
      .getDownloadedModels(TranslateRemoteModel::class.java)
      .addOnSuccessListener { downloaded ->
        done(downloaded.mapNotNull { TranslationLanguages.normalize(it.language) }.toSet())
      }
      .addOnFailureListener { e ->
        LogUtils.w(TAG, "Downloaded models not listed: %s", e)
        done(emptySet())
      }
  }

  override fun download(language: String, wifiOnly: Boolean, done: (Boolean) -> Unit) {
    val code = codes[language] ?: return done(false)
    val now = SystemClock.elapsedRealtime()
    val previous = downloading[language]
    if (previous != null) {
      if (now - previous < STALLED_AFTER_MS) return done(false)
      removeUnfinishedDownloads()
    }
    downloading[language] = now
    val conditions = DownloadConditions.Builder().apply { if (wifiOnly) requireWifi() }.build()
    models
      .download(TranslateRemoteModel.Builder(code).build(), conditions)
      .addOnCompleteListener { task ->
        // A stalled download whose entry was removed for a retry finishes here too.
        if (downloading[language] != now) return@addOnCompleteListener
        downloading.remove(language)
        if (!task.isSuccessful) LogUtils.w(TAG, "%s not downloaded: %s", language, task.exception)
        done(task.isSuccessful)
      }
  }

  override fun isDownloading(language: String): Boolean =
    downloading[language]?.let { SystemClock.elapsedRealtime() - it < STALLED_AFTER_MS } == true

  /**
   * Removes SandFly's unfinished entries from Android's download manager, which only ML Kit uses.
   * ML Kit waits on an existing entry for a model instead of adding a new one, so a stalled entry
   * would stall every retry.
   */
  private fun removeUnfinishedDownloads() {
    try {
      val manager =
        MlKitContext.getInstance().applicationContext.getSystemService(DownloadManager::class.java)
          ?: return
      val query =
        DownloadManager.Query()
          .setFilterByStatus(
            DownloadManager.STATUS_PENDING or
              DownloadManager.STATUS_RUNNING or
              DownloadManager.STATUS_PAUSED
          )
      val ids =
        manager.query(query)?.use { cursor ->
          val column = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
          buildList { while (cursor.moveToNext()) add(cursor.getLong(column)) }
        }
      if (!ids.isNullOrEmpty()) {
        LogUtils.w(TAG, "Removing %d stalled downloads", ids.size)
        manager.remove(*ids.toLongArray())
      }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Stalled downloads not removed: %s", e)
    }
  }

  override fun delete(language: String, done: (Boolean) -> Unit) {
    val code = codes[language] ?: return done(false)
    translators.entries.removeAll { (pair, client) ->
      (language in pair.toList()).also { if (it) client.close() }
    }
    models
      .deleteDownloadedModel(TranslateRemoteModel.Builder(code).build())
      .addOnCompleteListener { task -> done(task.isSuccessful) }
  }

  override fun translate(text: String, from: String, to: String, done: (String?) -> Unit) {
    val source = codes[from]
    val target = codes[to]
    if (source == null || target == null) return done(null)
    val client =
      translators.getOrPut(from to to) {
        com.google.mlkit.nl.translate.Translation.getClient(
          TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
        )
      }
    client
      .translate(text)
      .addOnSuccessListener { done(it) }
      .addOnFailureListener { e ->
        LogUtils.w(TAG, "Not translated: %s", e)
        done(null)
      }
  }

  override fun close() {
    translators.values.forEach { it.close() }
    translators.clear()
    identifier?.close()
    identifier = null
  }

  private companion object {
    const val TAG = "MlKitTranslation"
    const val MIN_CONFIDENCE = 0.5f
    const val MAX_CLIENTS = 3
    // A model is about 30 MB. After this long, the language can be chosen for download again.
    const val STALLED_AFTER_MS = 10 * 60 * 1000L
  }
}
