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

package com.google.android.accessibility.talkback.handwriting

import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.talkback.handwriting.HandwritingRecognizer.Result
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Recognizes handwriting with ML Kit Digital Ink Recognition, on the device, in the language of the
 * device. The language's model, about 20 MB, is downloaded from Google the first time.
 */
class MlKitHandwritingRecognizer(private val onDownloadDone: (Boolean) -> Unit) :
  HandwritingRecognizer {
  private val models = RemoteModelManager.getInstance()
  private var model: DigitalInkRecognitionModel? = null
  private var modelLocale: Locale? = null
  private var recognizer: DigitalInkRecognizer? = null
  private var downloading = false
  private val handler = Handler(Looper.getMainLooper())

  override fun recognize(
    strokes: List<List<InkPoint>>,
    width: Float,
    height: Float,
    preContext: String,
    onResult: (Result) -> Unit,
  ) = withModel { model ->
    if (model == null) return@withModel onResult(Result.UnsupportedLanguage)
    if (downloading) return@withModel onResult(Result.Downloading)
    models
      .isModelDownloaded(model)
      .addOnSuccessListener { downloaded ->
        if (!downloaded) {
          onResult(Result.NotDownloaded)
          return@addOnSuccessListener
        }
        val client =
          recognizer
            ?: DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
              .also { recognizer = it }
        val ink = Ink.builder()
        for (stroke in strokes) {
          val builder = Ink.Stroke.builder()
          for (point in stroke) builder.addPoint(Ink.Point.create(point.x, point.y, point.time))
          ink.addStroke(builder.build())
        }
        val context =
          RecognitionContext.builder().setPreContext(preContext).setWritingArea(WritingArea(width, height)).build()
        client
          .recognize(ink.build(), context)
          .addOnSuccessListener { result -> onResult(Result.Candidates(result.candidates.map { it.text })) }
          .addOnFailureListener { e ->
            // Recognition only fails when the model can't be used, such as when its files are
            // damaged or gone, so download it again.
            LogUtils.w(TAG, "Handwriting not recognized, downloading the model again: %s", e)
            close()
            downloading = true
            models
              .deleteDownloadedModel(model)
              .addOnCompleteListener { download(model) }
            onResult(Result.Downloading)
          }
      }
      .addOnFailureListener { e ->
        LogUtils.w(TAG, "Handwriting model state unknown: %s", e)
        onResult(Result.Failed)
      }
  }

  override fun prepare(onChecked: (Result?) -> Unit) = withModel { model ->
    if (model == null) return@withModel onChecked(Result.UnsupportedLanguage)
    if (downloading) return@withModel onChecked(Result.Downloading)
    models
      .isModelDownloaded(model)
      .addOnSuccessListener { downloaded ->
        LogUtils.d(TAG, "Handwriting model for %s downloaded: %s", modelLocale, downloaded)
        onChecked(if (downloaded) null else Result.NotDownloaded)
      }
      .addOnFailureListener { e ->
        // Recognition says what went wrong, if it does.
        LogUtils.w(TAG, "Handwriting model state unknown: %s", e)
        onChecked(null)
      }
  }

  /**
   * Finds the model for the device's language on another thread, since ML Kit reads its list of
   * languages the first time, and calls [then] with it, or null if there is none, on the main
   * thread.
   */
  private fun withModel(then: (DigitalInkRecognitionModel?) -> Unit) {
    val locale = Locale.getDefault()
    if (locale == modelLocale) return then(model)
    EXECUTOR.execute {
      val found = modelFor(locale)
      handler.post {
        if (locale != modelLocale) {
          close()
          modelLocale = locale
          model = found
        }
        then(model)
      }
    }
  }

  override fun download() = withModel { model ->
    if (model != null && !downloading) download(model)
  }

  override fun close() {
    recognizer?.close()
    recognizer = null
  }

  private fun download(model: DigitalInkRecognitionModel) {
    downloading = true
    LogUtils.i(TAG, "Downloading the handwriting model for %s", modelLocale)
    models
      .download(model, DownloadConditions.Builder().build())
      .addOnSuccessListener {
        downloading = false
        onDownloadDone(true)
      }
      .addOnFailureListener { e ->
        downloading = false
        LogUtils.w(TAG, "Handwriting model not downloaded: %s", e)
        onDownloadDone(false)
      }
  }

  /** The model for [locale], its region's if there is one, or null if there is none. */
  private fun modelFor(locale: Locale): DigitalInkRecognitionModel? {
    val identifier =
      listOf(locale.toLanguageTag(), locale.language).firstNotNullOfOrNull { tag ->
        try {
          DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag)
        } catch (e: Exception) {
          null
        }
      }
    return identifier?.let { DigitalInkRecognitionModel.builder(it).build() }
  }

  private companion object {
    const val TAG = "Handwriting"
    val EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
  }
}
