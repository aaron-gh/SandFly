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

package com.google.android.accessibility.talkback.dictation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/**
 * The language packs of Google's on-device speech recognition: which are downloaded, and asking
 * for the phone's language to be downloaded. Android 13 and later can ask; earlier versions can't,
 * so there the phone's language counts as downloaded.
 */
object GoogleSpeechLanguages {
  private const val TAG = "GoogleSpeechLanguages"
  private const val QUERY_TIMEOUT_MS = 5_000L
  private const val ACTION_DOWNLOAD_LANGUAGE = "com.google.recognition.action.DOWNLOAD_LANGUAGE"

  /** The downloaded languages as last found, or null when not known. */
  @Volatile private var installed: List<String>? = null

  /** Whether the phone's language is downloaded, or null when that isn't known. */
  fun hasPhoneLanguage(): Boolean? = installed?.let { matches(it, Locale.getDefault()) }

  /** Whether [languages], language tags such as en-US, include [locale]'s language. */
  fun matches(languages: List<String>, locale: Locale): Boolean =
    languages.any { Locale.forLanguageTag(it).language == locale.language }

  /** Finds the downloaded languages again, then runs [onDone] on the main thread. */
  fun refresh(context: Context, onDone: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !DictationSettings.googleInstalled(context)) {
      onDone()
      return
    }
    query(context.applicationContext, onDone)
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun query(context: Context, onDone: () -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    val recognizer =
      try {
        SpeechRecognizer.createSpeechRecognizer(context, DictationSettings.GOOGLE_SERVICE)
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "No recognizer: %s", e)
        onDone()
        return
      }
    var finished = false
    val finish = {
      if (!finished) {
        finished = true
        handler.removeCallbacksAndMessages(null)
        recognizer.destroy()
        onDone()
      }
    }
    handler.postDelayed(finish, QUERY_TIMEOUT_MS)
    recognizer.checkRecognitionSupport(
      Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true),
      context.mainExecutor,
      object : RecognitionSupportCallback {
        override fun onSupportResult(support: RecognitionSupport) {
          installed = support.installedOnDeviceLanguages
          LogUtils.i(TAG, "Downloaded languages: %s", installed)
          finish()
        }

        // Speech Recognition & Synthesis can report an error and then the result, so the
        // timeout ends a query that gets only the error.
        override fun onError(error: Int) {
          LogUtils.w(TAG, "Language query error: %d", error)
        }
      },
    )
  }

  /**
   * Opens Speech Recognition & Synthesis's screen that downloads the phone's language. Android
   * blocks that screen when the app opens it itself, from the background, so SandFly opens it.
   */
  fun download(context: Context) {
    val screen =
      Intent(ACTION_DOWNLOAD_LANGUAGE)
        .setPackage(DictationSettings.GOOGLE_SERVICE.packageName)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
    try {
      context.startActivity(screen)
      return
    } catch (e: ActivityNotFoundException) {
      LogUtils.w(TAG, "No download screen: %s", e)
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    try {
      val recognizer = SpeechRecognizer.createSpeechRecognizer(context, DictationSettings.GOOGLE_SERVICE)
      recognizer.triggerModelDownload(intent())
      Handler(Looper.getMainLooper()).postDelayed({ recognizer.destroy() }, QUERY_TIMEOUT_MS)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Download not started: %s", e)
    }
  }

  private fun intent() =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
      .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
}
