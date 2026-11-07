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
import android.net.Uri
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

private const val HUGGING_FACE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
private const val FUTO = "https://voiceinput.futo.org/VoiceInput/"

/**
 * The Whisper models that Dictate can use on the phone: which are downloaded, the one in use, and
 * the download in progress. One instance lives for the whole process, so a download carries on
 * while Dictation settings are closed.
 */
class WhisperModels private constructor(context: Context) {
  /**
   * A Whisper model in whisper.cpp's format: OpenAI's from Hugging Face, which know 99 languages,
   * and FUTO's, fine-tuned to encode only the spoken audio rather than 30 seconds ([shortContext]),
   * which is much faster for short dictation. The English ones know only English.
   */
  enum class Model(
    val id: String,
    val label: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    private val source: String = HUGGING_FACE,
    val shortContext: Boolean = false,
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
    ),
    FUTO_TINY_EN(
      "futo-tiny-en",
      "FUTO Tiny (English)",
      "tiny_en_acft_q8_0.bin",
      43_550_795,
      "4b5480aa1b14a7efc5b578ef176510970a898049671c3cd237285b3e3f6bfbfc",
      FUTO,
      shortContext = true,
    ),
    FUTO_BASE_EN(
      "futo-base-en",
      "FUTO Base (English)",
      "base_en_acft_q8_0.bin",
      81_781_811,
      "e9b4b7b81b8a28769e8aa9962aa39bb9f21b622cf6a63982e93f065ed5caf1c8",
      FUTO,
      shortContext = true,
    ),
    FUTO_SMALL_EN(
      "futo-small-en",
      "FUTO Small (English)",
      "small_en_acft_q8_0.bin",
      264_477_561,
      "58fbe949992dafed917590d58bc12ca577b08b9957f0b3e0d7ee71b64bed3aa8",
      FUTO,
      shortContext = true,
    ),
    FUTO_TINY(
      "futo-tiny",
      "FUTO Tiny",
      "tiny_acft_q8_0.bin",
      43_537_450,
      "07aa4d514144deacf5ffec5cacb36c93dee272fda9e64ac33a801f8cd5cbd953",
      FUTO,
      shortContext = true,
    ),
    FUTO_BASE(
      "futo-base",
      "FUTO Base",
      "base_acft_q8_0.bin",
      81_768_602,
      "e44f352c9aa2c3609dece20c733c4ad4a75c28cd9ab07d005383df55fa96efc4",
      FUTO,
      shortContext = true,
    ),
    FUTO_SMALL(
      "futo-small",
      "FUTO Small",
      "small_acft_q8_0.bin",
      264_464_624,
      "15ef255465a6dc582ecf1ec651a4618c7ee2c18c05570bbe46493d248d465ac4",
      FUTO,
      shortContext = true,
    );

    val url: String
      get() = source + fileName

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

  /**
   * The listed model Dictate uses: the chosen one if it is downloaded, else the first downloaded
   * one. Null while the user's own model file is in use; see [activeWhisper].
   */
  fun activeModel(): Model? {
    if (!WhisperEngines.isAvailable()) return null
    val chosenId = chosenId()
    if (chosenId == CUSTOM_ID && hasCustom()) return null
    val chosen = Model.of(chosenId)
    return chosen?.takeIf { isInstalled(it) } ?: Model.entries.firstOrNull { isInstalled(it) }
  }

  /** The ID of the chosen model, or of the user's own model file. */
  @androidx.annotation.VisibleForTesting
  internal fun chosenId(): String? = prefs.getString(DictationSettings.PREF_WHISPER_MODEL, null)

  /** A model file that Dictate can use, and whether it encodes only the spoken audio. */
  data class Active(val file: File, val shortContext: Boolean)

  /** The model file Dictate uses: a listed model, or the user's own file, or null for none. */
  fun activeWhisper(): Active? {
    if (!WhisperEngines.isAvailable()) return null
    activeModel()?.let {
      return Active(file(it), it.shortContext)
    }
    return if (hasCustom()) Active(customFile(), shortContext = false) else null
  }

  /** The user's own model file, in whisper.cpp's format. */
  fun customFile() = File(directory, CUSTOM_FILE)

  fun hasCustom() = customFile().isFile

  /** The name of the user's own model file as they chose it, or null for none. */
  fun customName(): String? =
    if (hasCustom()) prefs.getString(PREF_CUSTOM_NAME, null) ?: CUSTOM_FILE else null

  fun isCustomActive() = hasCustom() && activeModel() == null

  fun useCustom() {
    prefs.edit { putString(DictationSettings.PREF_WHISPER_MODEL, CUSTOM_ID) }
    notifyListeners()
  }

  /** Why the user's model file couldn't be added. */
  enum class ImportFailure {
    NOT_A_MODEL,
    LOW_SPACE,
    STORAGE,
  }

  /**
   * Copies the model file at [uri], named [name], into SandFly and uses it, and calls [onDone] on
   * the main thread with null, or why it failed. A file that doesn't start as whisper.cpp's models
   * do is refused, and so is one larger than [MAX_CUSTOM_BYTES].
   */
  fun importCustom(context: Context, uri: Uri, name: String, size: Long, onDone: (ImportFailure?) -> Unit) {
    val resolver = context.applicationContext.contentResolver
    worker.execute {
      val failure = copyCustom(resolver, uri, size)
      if (failure == null) {
        prefs.edit {
          putString(PREF_CUSTOM_NAME, name)
          putString(DictationSettings.PREF_WHISPER_MODEL, CUSTOM_ID)
        }
      }
      main.post { onDone(failure) }
      notifyListeners()
    }
  }

  @androidx.annotation.VisibleForTesting
  internal fun copyCustom(
    resolver: android.content.ContentResolver,
    uri: Uri,
    size: Long,
  ): ImportFailure? {
    directory.mkdirs()
    if (size > MAX_CUSTOM_BYTES) return ImportFailure.NOT_A_MODEL
    if (size > 0 && StatFs(directory.path).availableBytes < size + size / 10) {
      return ImportFailure.LOW_SPACE
    }
    val part = File(directory, "$CUSTOM_FILE.part")
    try {
      val input = resolver.openInputStream(uri) ?: return ImportFailure.STORAGE
      input.use { stream ->
        val magic = ByteArray(GGML_MAGIC.size)
        if (stream.readNBytesCompat(magic) != magic.size || !magic.contentEquals(GGML_MAGIC)) {
          return ImportFailure.NOT_A_MODEL
        }
        part.outputStream().use { out ->
          out.write(magic)
          val buffer = ByteArray(1 shl 16)
          var total = magic.size.toLong()
          while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_CUSTOM_BYTES) return ImportFailure.NOT_A_MODEL
            out.write(buffer, 0, read)
          }
        }
      }
      if (!part.renameTo(customFile())) return ImportFailure.STORAGE
      return null
    } catch (e: java.io.IOException) {
      return ImportFailure.STORAGE
    } catch (e: SecurityException) {
      return ImportFailure.STORAGE
    } finally {
      part.delete()
    }
  }

  private fun java.io.InputStream.readNBytesCompat(buffer: ByteArray): Int {
    var read = 0
    while (read < buffer.size) {
      val count = read(buffer, read, buffer.size - read)
      if (count < 0) break
      read += count
    }
    return read
  }

  fun deleteCustom() {
    worker.execute {
      customFile().delete()
      prefs.edit { remove(PREF_CUSTOM_NAME) }
      setState(State.Idle)
    }
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
      if (result == ModelDownloader.Result.Done) chooseDownloaded(model)
      setState(
        when (result) {
          is ModelDownloader.Result.Failed -> State.Failed(model, result.failure)
          else -> State.Idle
        }
      )
    }
  }

  /**
   * A downloaded model is used unless another chosen one is downloaded, or the user's own model
   * file is chosen.
   */
  @androidx.annotation.VisibleForTesting
  internal fun chooseDownloaded(model: Model) {
    val chosenId = chosenId()
    if (chosenId == CUSTOM_ID && hasCustom()) return
    val chosen = Model.of(chosenId)
    if (chosen == null || !isInstalled(chosen)) {
      prefs.edit { putString(DictationSettings.PREF_WHISPER_MODEL, model.id) }
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
    /** The value of the chosen model while it is the user's own file. */
    private const val CUSTOM_ID = "custom"
    private const val CUSTOM_FILE = "custom-model.bin"
    private const val PREF_CUSTOM_NAME = "pref_whisper_custom_model_name"
    /** Larger than Whisper Large v3 unquantized, about 3.1 GB. */
    const val MAX_CUSTOM_BYTES = 4L * 1024 * 1024 * 1024
    /** How whisper.cpp's model files start: "ggml" as a little-endian number. */
    private val GGML_MAGIC = byteArrayOf(0x6c, 0x6d, 0x67, 0x67)

    @Volatile private var instance: WhisperModels? = null

    fun get(context: Context): WhisperModels =
      instance
        ?: synchronized(this) { instance ?: WhisperModels(context).also { instance = it } }
  }
}
