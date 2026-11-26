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

package com.google.android.accessibility.talkback.textrecognition

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.actor.gemini.local.DownloadProgress
import com.google.android.accessibility.talkback.actor.gemini.local.ModelDownloader
import com.google.android.accessibility.talkback.actor.gemini.local.ModelFiles
import com.google.android.accessibility.utils.ocr.CyrillicOcr
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tesseract's tessdata_fast models for Cyrillic text recognition (Apache 2.0, from
 * github.com/tesseract-ocr/tessdata_fast at [COMMIT]): which are downloaded, and the download in
 * progress. With the first one, the model of Latin letters ([LATIN]) is downloaded too, so text
 * in both is read; it is deleted with the last one. One instance lives for the whole process, so a
 * download carries on while settings are closed.
 */
class CyrillicTextModels
@VisibleForTesting
internal constructor(context: Context, private val downloader: ModelDownloader = ModelDownloader()) {
  /** A model file: its Tesseract code, size and SHA-256. */
  class ModelFile(val code: String, val sizeBytes: Long, val sha256: String) {
    val url: String
      get() = "$BASE_URL$code${CyrillicOcr.MODEL_SUFFIX}"
  }

  /** A language's model, which [CyrillicOcr.modelFor] gives for [languageTag]. */
  enum class Model(val languageTag: String, val file: ModelFile) {
    RUSSIAN(
      "ru",
      ModelFile("rus", 3_861_738, "e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225"),
    ),
    UKRAINIAN(
      "uk",
      ModelFile("ukr", 3_825_102, "d59e53e2bded32f4445f124b4b00240fcac7e8044c003ab822ccb94f0b3db59b"),
    ),
    BELARUSIAN(
      "be",
      ModelFile("bel", 3_692_098, "9c6668a0b202f3dcfe074b64620d108e1902ca7498a40b5a11b4a3da6112d58f"),
    ),
    BULGARIAN(
      "bg",
      ModelFile("bul", 1_675_212, "aebc9b0fcc8cfaf8a9f38a02bb7b85052bd850744696a2c11cf0081820e5b21e"),
    ),
    SERBIAN(
      "sr",
      ModelFile("srp", 2_149_931, "aa41ae3d9cc705e60d398ab38a5c3cc8b772c0d420c7d4f0859beb13d0e321b6"),
    ),
    MACEDONIAN(
      "mk",
      ModelFile("mkd", 1_600_188, "58622bf154830fa62103359938564aeb8112b929759e22a48224f3ecfaac34c6"),
    ),
    KAZAKH(
      "kk",
      ModelFile("kaz", 4_734_644, "fcc01eed3815a42b9c6321c4c9d3606f39b166cbf95ade98b7d8d12063eae53d"),
    ),
    KYRGYZ(
      "ky",
      ModelFile("kir", 9_928_497, "9777956300900b528d26932cf80693f95e75143433fb851d567194bcc38a31ae"),
    ),
    TAJIK(
      "tg",
      ModelFile("tgk", 2_602_685, "7b32ed1374649b9b44b9a20ed07f1e86c1e1004c3bd11ce182496a47a18cb321"),
    ),
    MONGOLIAN(
      "mn",
      ModelFile("mon", 2_137_042, "a151a3806d61ac43619cd383896d551ba5c3b07388ffec6fc83c8c604d677570"),
    ),
    TATAR(
      "tt",
      ModelFile("tat", 1_072_896, "7ad946d753f4b9688e4e4bcf525a931f486da4aaa9020e32b6a7262718bdf093"),
    ),
    UZBEK_CYRILLIC(
      "uz-Cyrl",
      ModelFile("uzb_cyrl", 1_563_501, "a2fe0b3f0eb4a5e46252470160ad51cc5f3c4c0f390ba67d1911beb0dbe80cc8"),
    ),
    AZERBAIJANI_CYRILLIC(
      "az-Cyrl",
      ModelFile("aze_cyrl", 1_943_672, "2e96ab67817819e14a3a2d6250261130457c8e78dde3077841ab391fe003551c"),
    );

    val locale: Locale
      get() = Locale.forLanguageTag(languageTag)
  }

  sealed interface State {
    data object Idle : State

    data class Downloading(val model: Model, val bytes: Long, val total: Long) : State

    data class Failed(val model: Model, val failure: ModelFiles.Failure) : State
  }

  private val appContext = context.applicationContext ?: context
  private val main = Handler(Looper.getMainLooper())
  // Its thread ends when idle.
  private val worker =
    ThreadPoolExecutor(1, 1, IDLE_THREAD_SECONDS, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()) {
        Thread(it, "cyrillic-text-model").apply { isDaemon = true }
      }
      .apply { allowCoreThreadTimeOut(true) }
  private val listeners = CopyOnWriteArraySet<() -> Unit>()
  private val cancelled = AtomicBoolean(false)

  @Volatile
  var state: State = State.Idle
    private set

  fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }

  fun removeListener(listener: () -> Unit) {
    listeners.remove(listener)
  }

  private fun target(file: ModelFile): java.io.File = CyrillicOcr.modelFile(appContext, file.code)

  fun isInstalled(model: Model): Boolean = target(model.file).isFile

  private fun latinInstalled(): Boolean = target(LATIN).isFile

  /** The files a download of [model] fetches: its own, and the Latin one while it is missing. */
  @VisibleForTesting
  internal fun filesFor(model: Model): List<ModelFile> =
    if (latinInstalled()) listOf(model.file) else listOf(model.file, LATIN)

  /** How many bytes a download of [model] fetches. */
  fun downloadBytes(model: Model): Long = filesFor(model).sumOf { it.sizeBytes }

  /** How much storage [model] frees when deleted, with the Latin model when it is the last one. */
  fun deleteBytes(model: Model): Long =
    model.file.sizeBytes + if (isLastInstalled(model) && latinInstalled()) LATIN.sizeBytes else 0L

  private fun isLastInstalled(model: Model): Boolean =
    Model.entries.none { it != model && isInstalled(it) }

  fun startDownload(model: Model) {
    if (state is State.Downloading) return
    val files = filesFor(model)
    val dir = CyrillicOcr.tessdataDir(appContext)
    dir.mkdirs()
    val total = files.sumOf { it.sizeBytes }
    val have = files.sumOf { ModelFiles.partFile(target(it)).length() }
    val needed = total - have
    val available =
      try {
        StatFs(dir.path).availableBytes
      } catch (e: IllegalArgumentException) {
        0L
      }
    if (available < needed + needed / 10) {
      setState(State.Failed(model, ModelFiles.Failure.LOW_SPACE))
      return
    }
    cancelled.set(false)
    setState(State.Downloading(model, have, total))
    worker.execute {
      val result =
        try {
          download(model, files, total)
        } catch (e: Throwable) {
          // Such as a SecurityException without network access. The worker must stay usable.
          LogUtils.w(TAG, "Download failed: %s", e)
          ModelDownloader.Result.Failed(ModelFiles.Failure.STORAGE)
        }
      if (result == ModelDownloader.Result.Cancelled) {
        files.forEach { ModelFiles.partFile(target(it)).delete() }
      }
      setState(
        when (result) {
          is ModelDownloader.Result.Failed -> State.Failed(model, result.failure)
          else -> State.Idle
        }
      )
    }
  }

  /** Downloads [files] one after another, with the progress of all of them. */
  private fun download(model: Model, files: List<ModelFile>, total: Long): ModelDownloader.Result {
    var done = 0L
    var lastReport = SystemClock.elapsedRealtime()
    for (file in files) {
      if (target(file).isFile) {
        done += file.sizeBytes
        continue
      }
      val before = done
      val result =
        downloader.download(
          file.url,
          target(file),
          file.sizeBytes,
          file.sha256,
          onProgress = { bytes ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastReport >= PROGRESS_INTERVAL_MS) {
              lastReport = now
              setState(State.Downloading(model, before + bytes, total))
            }
          },
          isCancelled = { cancelled.get() },
        )
      if (result != ModelDownloader.Result.Done) return result
      done += file.sizeBytes
    }
    return ModelDownloader.Result.Done
  }

  fun cancelDownload() {
    cancelled.set(true)
  }

  /** Deletes [model], and the Latin model with the last one. */
  fun delete(model: Model) {
    worker.execute {
      val last = isLastInstalled(model)
      deleteFiles(model.file)
      if (last) deleteFiles(LATIN)
      setState(State.Idle)
    }
  }

  private fun deleteFiles(file: ModelFile) {
    target(file).delete()
    ModelFiles.partFile(target(file)).delete()
  }

  /** How far the download has got, in percent. */
  fun percent(state: State.Downloading): Int = DownloadProgress.percent(state.bytes, state.total)

  private fun setState(newState: State) {
    state = newState
    main.post { listeners.forEach { it() } }
  }

  companion object {
    private const val TAG = "CyrillicTextModels"
    private const val PROGRESS_INTERVAL_MS = 500L
    private const val IDLE_THREAD_SECONDS = 30L

    /** The tessdata_fast commit the sizes and hashes are of. */
    const val COMMIT = "87416418657359cb625c412a48b6e1d6d41c29bd"
    private const val BASE_URL = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/$COMMIT/"

    /** The model of Latin letters, read with each Cyrillic one. */
    @JvmField
    val LATIN =
      ModelFile(
        CyrillicOcr.LATIN_MODEL,
        4_113_088,
        "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2",
      )

    @Volatile private var instance: CyrillicTextModels? = null

    @JvmStatic
    fun get(context: Context): CyrillicTextModels =
      instance
        ?: synchronized(this) { instance ?: CyrillicTextModels(context).also { instance = it } }
  }
}
