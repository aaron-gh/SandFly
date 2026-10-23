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

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import androidx.core.content.edit
import com.google.android.accessibility.talkback.actor.gemini.local.DownloadProgress
import com.google.android.accessibility.talkback.actor.gemini.local.ModelDownloader
import com.google.android.accessibility.talkback.actor.gemini.local.ModelFiles
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The Whisper models that Dictate can use on the phone: which are downloaded, the one in use, and
 * the download in progress. One instance lives for the whole process, so a download carries on
 * while Dictation settings are closed.
 */
class WhisperModels private constructor(context: Context) {
  /** A Whisper model in whisper.cpp's format, from Hugging Face. All of them know 99 languages. */
  enum class Model(
    val id: String,
    val label: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
  ) {
    TINY(
      "tiny",
      "Whisper Tiny",
      "ggml-tiny-q5_1.bin",
      32_152_673,
      "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7",
    ),
    BASE(
      "base",
      "Whisper Base",
      "ggml-base-q5_1.bin",
      59_707_625,
      "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
    ),
    SMALL(
      "small",
      "Whisper Small",
      "ggml-small-q5_1.bin",
      190_085_487,
      "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
    ),
    LARGE_TURBO(
      "large-v3-turbo",
      "Whisper Large v3 Turbo",
      "ggml-large-v3-turbo-q5_0.bin",
      574_041_195,
      "394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2",
    );

    val url: String
      get() = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$fileName"

    companion object {
      fun of(id: String?): Model? = entries.firstOrNull { it.id == id }
    }
  }

  sealed interface State {
    data object Idle : State

    data class Downloading(val model: Model, val bytes: Long) : State

    data class Failed(val model: Model, val failure: ModelFiles.Failure) : State
  }

  private val prefs = SharedPreferencesUtils.getSharedPreferences(context.applicationContext)
  private val directory = File(context.applicationContext.filesDir, "whisper")
  private val main = Handler(Looper.getMainLooper())
  private val worker =
    Executors.newSingleThreadExecutor { Thread(it, "whisper-model").apply { isDaemon = true } }
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

  fun file(model: Model) = File(directory, model.fileName)

  fun isInstalled(model: Model) = file(model).isFile

  /** The model Dictate uses: the chosen one if it is downloaded, else the first downloaded one. */
  fun activeModel(): Model? {
    if (!WhisperEngines.isAvailable()) return null
    val chosen = Model.of(prefs.getString(DictationSettings.PREF_WHISPER_MODEL, null))
    return chosen?.takeIf { isInstalled(it) } ?: Model.entries.firstOrNull { isInstalled(it) }
  }

  fun setActive(model: Model) {
    prefs.edit { putString(DictationSettings.PREF_WHISPER_MODEL, model.id) }
    notifyListeners()
  }

  fun startDownload(model: Model) {
    if (state is State.Downloading) return
    directory.mkdirs()
    val needed = model.sizeBytes - ModelFiles.partFile(file(model)).length()
    if (StatFs(directory.path).availableBytes < needed + needed / 10) {
      setState(State.Failed(model, ModelFiles.Failure.LOW_SPACE))
      return
    }
    cancelled.set(false)
    setState(State.Downloading(model, ModelFiles.partFile(file(model)).length()))
    worker.execute {
      var lastReport = SystemClock.elapsedRealtime()
      val result =
        ModelDownloader()
          .download(
            model.url,
            file(model),
            model.sizeBytes,
            model.sha256,
            onProgress = { bytes ->
              val now = SystemClock.elapsedRealtime()
              if (now - lastReport >= PROGRESS_INTERVAL_MS) {
                lastReport = now
                setState(State.Downloading(model, bytes))
              }
            },
            isCancelled = { cancelled.get() },
          )
      if (result == ModelDownloader.Result.Done) {
        // A downloaded model is used unless another chosen one is downloaded.
        val chosen = Model.of(prefs.getString(DictationSettings.PREF_WHISPER_MODEL, null))
        if (chosen == null || !isInstalled(chosen)) {
          prefs.edit { putString(DictationSettings.PREF_WHISPER_MODEL, model.id) }
        }
      }
      setState(
        when (result) {
          is ModelDownloader.Result.Failed -> State.Failed(model, result.failure)
          else -> State.Idle
        }
      )
    }
  }

  fun cancelDownload() {
    cancelled.set(true)
  }

  fun delete(model: Model) {
    worker.execute {
      file(model).delete()
      ModelFiles.partFile(file(model)).delete()
      setState(State.Idle)
    }
  }

  /** How far the download of [model] has got, in percent. */
  fun percent(state: State.Downloading) = DownloadProgress.percent(state.bytes, state.model.sizeBytes)

  private fun setState(newState: State) {
    state = newState
    notifyListeners()
  }

  private fun notifyListeners() {
    main.post { listeners.forEach { it() } }
  }

  companion object {
    private const val PROGRESS_INTERVAL_MS = 500L

    @Volatile private var instance: WhisperModels? = null

    fun get(context: Context): WhisperModels =
      instance
        ?: synchronized(this) { instance ?: WhisperModels(context).also { instance = it } }
  }
}
