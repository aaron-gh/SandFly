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
package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import androidx.core.os.UserManagerCompat
import com.google.android.accessibility.talkback.keyboard.words.LearnedWords
import com.google.android.accessibility.talkback.keyboard.words.WordLanguage
import com.google.android.accessibility.talkback.keyboard.words.WordList
import com.google.android.accessibility.talkback.keyboard.words.WordSuggester
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.util.concurrent.Executors

/**
 * The word list and learned words of the SandFly keyboard's auto-correct and word suggestions, for
 * one language at a time. A list is read on a thread of its own the first time a text field needs
 * it, and kept until another language is needed; until then there are no suggestions. Learned words
 * are saved in the app's files, `sandfly_keyboard_words_<language>.txt`, on a thread of their own.
 */
internal object WordData {
  private const val TAG = "SandFlyKeyboardWords"
  private const val LIST_DIRECTORY = "word_lists"

  private val main = Handler(Looper.getMainLooper())
  private val saver = Executors.newSingleThreadExecutor { Thread(it, "SandFlyKeyboardWordsSave") }

  private var loaded: WordSuggester? = null
  private var loading: WordLanguage? = null

  /** The suggester for [language], or null while its list is read; starts reading it. Main thread. */
  fun suggester(context: Context, language: WordLanguage): WordSuggester? {
    loaded?.let { if (it.language == language) return it }
    if (loading == language) return null
    loading = language
    val app = context.applicationContext
    Thread(
        {
          val suggester =
            try {
              val start = System.currentTimeMillis()
              val list =
                app.assets.open("$LIST_DIRECTORY/${language.fileName}").bufferedReader().use {
                  WordList.read(it, language)
                }
              val result = WordSuggester(language, list, readLearned(app, language))
              LogUtils.i(
                TAG,
                "Word list %s read: %d nodes in %d ms",
                language.code,
                list.nodeCount,
                System.currentTimeMillis() - start,
              )
              result
            } catch (e: Throwable) {
              // An out of memory or a missing asset leaves the keyboard without suggestions.
              LogUtils.e(TAG, "Can't read word list %s: %s", language.code, e)
              null
            }
          main.post {
            if (loading == language) {
              loading = null
              if (suggester != null) {
                loaded = suggester
                SandFlyKeyboardHost.learnedWordsDeleter = ::deleteLearned
              }
            }
          }
        },
        "SandFlyKeyboardWords",
      )
      .apply { isDaemon = true }
      .start()
    return null
  }

  /** The suggester for [language] if its list is read, without starting to read it. */
  fun loaded(language: WordLanguage): WordSuggester? = loaded?.takeIf { it.language == language }

  /** Saves the learned words of [suggester] if they changed. Main thread. */
  fun save(context: Context, suggester: WordSuggester) {
    val learned = suggester.learned
    if (!learned.changed || !UserManagerCompat.isUserUnlocked(context)) return
    val text = learned.text()
    val file = learnedFile(context, suggester.language)
    saver.execute {
      val atomic = AtomicFile(file)
      try {
        val out = atomic.startWrite()
        try {
          out.write(text.toByteArray())
          atomic.finishWrite(out)
        } catch (e: Throwable) {
          atomic.failWrite(out)
          throw e
        }
      } catch (e: Throwable) {
        LogUtils.e(TAG, "Can't save learned words: %s", e)
      }
    }
  }

  /** Deletes the learned words of every language, and forgets those of the list in use. */
  fun deleteLearned(context: Context) {
    loaded?.learned?.clear()
    loaded?.let { save(context, it) }
    // The words added to the list go too: it is read again when next needed.
    loaded = null
    val files = context.filesDir.listFiles { _, name -> name.startsWith(LEARNED_PREFIX) }
    saver.execute { files?.forEach { AtomicFile(it).delete() } }
  }

  private const val LEARNED_PREFIX = SandFlyKeyboardHost.LEARNED_WORDS_PREFIX

  /** Saved learned words are far smaller; a bigger file isn't read. */
  private const val MAX_LEARNED_BYTES = 1L shl 20

  private fun learnedFile(context: Context, language: WordLanguage) =
    File(context.filesDir, "$LEARNED_PREFIX${language.code}.txt")

  private fun readLearned(context: Context, language: WordLanguage): LearnedWords {
    if (!UserManagerCompat.isUserUnlocked(context)) return LearnedWords()
    val file = learnedFile(context, language)
    if (!file.exists() || file.length() > MAX_LEARNED_BYTES) return LearnedWords()
    return try {
      LearnedWords.parse(String(AtomicFile(file).readFully()))
    } catch (e: Throwable) {
      LogUtils.e(TAG, "Can't read learned words: %s", e)
      LearnedWords()
    }
  }
}
