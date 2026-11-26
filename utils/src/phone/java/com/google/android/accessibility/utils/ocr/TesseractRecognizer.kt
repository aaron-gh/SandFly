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

package com.google.android.accessibility.utils.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.text.Text
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Recognizes text with Tesseract, for the scripts ML Kit has no recognizer for. Runs in the `:mlkit`
 * process, through [com.google.android.accessibility.utils.mlkit.MlKitHelperService], so a crash in
 * Tesseract's native code stops only that process.
 *
 * Images are recognized one at a time on one thread, which ends when idle. The loaded models stay
 * until [close].
 */
class TesseractRecognizer(context: Context) {
  private val dataPath = CyrillicOcr.dataDir(context).path

  private val executor =
    ThreadPoolExecutor(
        1,
        1,
        IDLE_THREAD_SECONDS,
        TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>(MAX_QUEUED),
      ) {
        Thread(it, "tesseract")
      }
      .apply { allowCoreThreadTimeOut(true) }

  // Used on the executor's thread only.
  private var api: TessBaseAPI? = null
  private var apiLanguages: String? = null

  /**
   * Recognizes [bitmap], turned by [rotation] degrees, with the models in [languages], such as
   * "rus+eng", and recycles it. Fails with [MlKitException.UNAVAILABLE] when the models can't be
   * loaded.
   */
  fun recognize(bitmap: Bitmap, rotation: Int, languages: String, language: String): Task<Text> {
    val source = TaskCompletionSource<Text>()
    try {
      executor.execute {
        try {
          source.setResult(run(bitmap, rotation, languages, language))
        } catch (e: MlKitException) {
          source.setException(e)
        } catch (e: Throwable) {
          // Such as OutOfMemoryError: this process must keep answering.
          Log.w(TAG, "Recognition failed: $e")
          source.setException(MlKitException("Tesseract: $e", MlKitException.INTERNAL))
        } finally {
          bitmap.recycle()
        }
      }
    } catch (e: RejectedExecutionException) {
      bitmap.recycle()
      source.setException(MlKitException("Too many images waiting", MlKitException.RESOURCE_EXHAUSTED))
    }
    return source.task
  }

  /** Drops the images waiting, and frees the models once the image being read is done. */
  fun close() {
    executor.queue.clear()
    try {
      executor.execute { releaseApi() }
    } catch (e: RejectedExecutionException) {
      // Already closed.
    }
    executor.shutdown()
  }

  private fun run(bitmap: Bitmap, rotation: Int, languages: String, language: String): Text {
    val start = SystemClock.elapsedRealtime()
    val (image, scale) = prepare(bitmap, rotation)
    try {
      val tess = api(languages)
      tess.setImage(image)
      // Recognizes the image.
      tess.getUTF8Text()
      val text = TesseractText(language, scale)
      val iterator = tess.resultIterator
      if (iterator != null) {
        try {
          iterator.begin()
          do {
            val word = iterator.getUTF8Text(PageIteratorLevel.RIL_WORD) ?: continue
            val box = iterator.getBoundingRect(PageIteratorLevel.RIL_WORD) ?: continue
            text.add(
              TesseractText.Word(
                word,
                box,
                iterator.confidence(PageIteratorLevel.RIL_WORD),
                startsBlock = iterator.isAtBeginningOf(PageIteratorLevel.RIL_BLOCK),
                startsLine = iterator.isAtBeginningOf(PageIteratorLevel.RIL_TEXTLINE),
              )
            )
          } while (iterator.next(PageIteratorLevel.RIL_WORD))
        } finally {
          iterator.delete()
        }
      }
      tess.clear()
      Log.d(TAG, "Recognized ${image.width}x${image.height} in ${SystemClock.elapsedRealtime() - start} ms")
      return text.build()
    } finally {
      if (image !== bitmap) image.recycle()
    }
  }

  /** The API with [languages] loaded. */
  private fun api(languages: String): TessBaseAPI {
    api?.let {
      if (apiLanguages == languages) return it
    }
    releaseApi()
    val tess = TessBaseAPI()
    val loaded =
      try {
        tess.init(dataPath, languages, TessBaseAPI.OEM_LSTM_ONLY)
      } catch (e: IllegalArgumentException) {
        // No tessdata directory.
        false
      }
    if (!loaded) {
      tess.recycle()
      throw MlKitException("Tesseract models $languages not loaded", MlKitException.UNAVAILABLE)
    }
    // Screens and photos have text in several places, not one column.
    tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
    api = tess
    apiLanguages = languages
    return tess
  }

  private fun releaseApi() {
    api?.recycle()
    api = null
    apiLanguages = null
  }

  /**
   * [bitmap] upright, as Tesseract's ARGB_8888, and no larger than [MAX_PIXELS], with the factor
   * that turns its coordinates back into [bitmap]'s turned ones.
   */
  private fun prepare(bitmap: Bitmap, rotation: Int): Pair<Bitmap, Float> {
    val pixels = bitmap.width.toLong() * bitmap.height
    val shrink = if (pixels > MAX_PIXELS) sqrt(MAX_PIXELS.toDouble() / pixels).toFloat() else 1f
    var image = bitmap
    if (rotation % 360 != 0 || shrink < 1f) {
      val matrix = Matrix()
      if (rotation % 360 != 0) matrix.postRotate(rotation.toFloat())
      if (shrink < 1f) matrix.postScale(shrink, shrink)
      image = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    if (image.config != Bitmap.Config.ARGB_8888) {
      val copy =
        image.copy(Bitmap.Config.ARGB_8888, false)
          ?: throw MlKitException("Image not copied", MlKitException.INTERNAL)
      if (image !== bitmap) image.recycle()
      image = copy
    }
    return image to 1f / shrink
  }

  companion object {
    private const val TAG = "TesseractRecognizer"

    /** Whether this build has Tesseract. Watch builds don't. */
    const val IS_AVAILABLE = true

    /** Larger images are made smaller first: a 12 MP photo takes Tesseract half a minute or more. */
    private const val MAX_PIXELS = 8_000_000L

    /** Images waiting beyond the one being read; more are refused. */
    private const val MAX_QUEUED = 4

    private const val IDLE_THREAD_SECONDS = 30L
  }
}
