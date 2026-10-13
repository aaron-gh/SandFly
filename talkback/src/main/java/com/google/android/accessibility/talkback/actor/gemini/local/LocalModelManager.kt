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

package com.google.android.accessibility.talkback.actor.gemini.local

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Owns the on-device model: its files, the download and import, and the running model. One
 * instance lives for the whole process, so a download carries on while the settings screen is
 * closed.
 */
class LocalModelManager private constructor(context: Context) {
  /** What the manager is doing now. */
  sealed interface State {
    data object Idle : State

    data class Downloading(val model: LocalModel, val bytes: Long, val bytesPerSecond: Long = 0) :
      State

    data object Importing : State

    data class Failed(val failure: ModelFiles.Failure) : State
  }

  private val appContext = context.applicationContext
  private val prefs = SharedPreferencesUtils.getSharedPreferences(appContext)
  private val main = Handler(Looper.getMainLooper())
  private val worker =
    Executors.newSingleThreadExecutor { Thread(it, "on-device-ai-model").apply { isDaemon = true } }
  private val listeners = CopyOnWriteArraySet<() -> Unit>()
  // How the download ends once the worker sees it: Idle when the user stopped it, Failed when
  // Android did. Null while it carries on.
  private val stopDownload = AtomicReference<State?>(null)
  // Closing a model waits for any answer in progress, so it happens on its own thread. Nothing that
  // runs on the main thread may take llmLock or wait for a model.
  private val closer =
    Executors.newSingleThreadExecutor { Thread(it, "on-device-ai-close").apply { isDaemon = true } }
  private val llmLock = Any()
  @Volatile private var llm: RemoteLocalLlm? = null
  private var llmKey: Triple<LocalModel, Boolean, Long>? = null

  val store = LocalModelStore(File(appContext.filesDir, "models"))

  @Volatile
  var state: State = State.Idle
    private set

  fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }

  fun removeListener(listener: () -> Unit) {
    listeners.remove(listener)
  }

  /** The installed model that will answer, or null when none is installed. */
  fun activeModel(): LocalModel? = store.installed(preferredModel())

  /** The model the user chose, or the best one for this phone when they have not chosen. */
  fun preferredModel(): LocalModel = OnDeviceAiSettings.preferredModel(prefs, totalRamBytes())

  /** The models to offer on this phone, or all of them when memory limits are ignored. */
  fun availableModels(): List<LocalModel> =
    if (OnDeviceAiSettings.ignoreMemoryLimits(prefs)) {
      LocalModel.entries
    } else {
      OnDeviceAiSettings.modelsFor(totalRamBytes()) { store.isInstalled(it) }
    }

  fun isReady(): Boolean = activeModel() != null

  fun support(model: LocalModel): OnDeviceAiSettings.Support =
    OnDeviceAiSettings.support(model, totalRamBytes(), Build.SUPPORTED_ABIS.toList())

  private fun totalRamBytes(): Long {
    val memory = ActivityManager.MemoryInfo()
    (appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
    return memory.totalMem
  }

  /**
   * The model that answers requests, created on first use. Call this from the worker thread, not
   * the main thread. A model that no longer matches the settings is retired in the background.
   */
  fun llm(): LocalLlm? {
    val model = activeModel() ?: return null
    val gpu = OnDeviceAiSettings.useGpu(prefs)
    val file = store.file(model)
    val key = Triple(model, gpu, file.lastModified())
    synchronized(llmLock) {
      val current = llm
      if (current == null || llmKey != key) {
        current?.let { retire(it) }
        llm = RemoteLocalLlm(appContext, model, file, gpu)
        llmKey = key
      }
      return llm
    }
  }

  /** The model that is loaded now, if any, without creating one. Never blocks. */
  fun currentLlm(): LocalLlm? = llm

  private fun releaseLlm() {
    synchronized(llmLock) {
      llm?.let { retire(it) }
      llm = null
      llmKey = null
    }
  }

  private fun retire(old: RemoteLocalLlm) {
    old.cancel()
    closer.execute { old.close() }
  }

  fun startDownload(model: LocalModel) {
    if (state is State.Downloading || state == State.Importing) return
    val needed = model.sizeBytes - store.partialBytes(model)
    store.file(model).parentFile?.mkdirs()
    val free = StatFs(store.file(model).parentFile!!.path).availableBytes
    if (free < needed + needed / 10) {
      setState(State.Failed(ModelFiles.Failure.LOW_SPACE))
      return
    }
    stopDownload.set(null)
    val resumedFrom = store.partialBytes(model)
    setState(State.Downloading(model, resumedFrom))
    ModelDownloadService.start(appContext)
    worker.execute {
      var lastReportTime = SystemClock.elapsedRealtime()
      var lastReportBytes = resumedFrom
      var speed = 0L
      val result =
        ModelDownloader()
          .download(
            model.url,
            store.file(model),
            model.sizeBytes,
            model.sha256,
            onProgress = { bytes ->
              val now = SystemClock.elapsedRealtime()
              if (now - lastReportTime >= PROGRESS_INTERVAL_MS) {
                speed =
                  DownloadProgress.smoothedSpeed(
                    speed,
                    bytes - lastReportBytes,
                    now - lastReportTime,
                  )
                lastReportTime = now
                lastReportBytes = bytes
                setState(State.Downloading(model, bytes, speed))
              }
            },
            isCancelled = { stopDownload.get() != null },
          )
      setState(
        when (result) {
          ModelDownloader.Result.Done -> State.Idle
          ModelDownloader.Result.Cancelled -> stopDownload.get() ?: State.Idle
          is ModelDownloader.Result.Failed -> State.Failed(result.failure)
        }
      )
    }
  }

  fun cancelDownload() {
    stopDownload.set(State.Idle)
  }

  /**
   * Stops the download because Android ended its time, unless the user already stopped it. The
   * part on the phone stays, so the next start carries on from there.
   */
  fun pauseDownload() {
    stopDownload.compareAndSet(null, State.Failed(ModelFiles.Failure.TIME_LIMIT))
  }

  fun delete(model: LocalModel) {
    worker.execute {
      if (activeModel() == model) releaseLlm()
      store.delete(model)
      // With no model left there is nothing to answer, so go back to the cloud.
      if (!isReady()) OnDeviceAiSettings.setOnDevice(prefs, false)
      setState(State.Idle)
    }
  }

  /** Installs a model file the user picked, and tells [onDone] which model it was, or null. */
  fun importFrom(uri: Uri, onDone: (LocalModel?) -> Unit) {
    if (state is State.Downloading || state == State.Importing) {
      main.post { onDone(null) }
      return
    }
    setState(State.Importing)
    worker.execute {
      val model =
        try {
          store.importUnknown {
            appContext.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open")
          }
        } catch (_: IOException) {
          null
        }
      setState(if (model == null) State.Failed(ModelFiles.Failure.HASH_MISMATCH) else State.Idle)
      main.post { onDone(model) }
    }
  }

  private fun setState(newState: State) {
    state = newState
    main.post { listeners.forEach { it() } }
  }

  companion object {
    private const val PROGRESS_INTERVAL_MS = 500L

    @Volatile private var instance: LocalModelManager? = null

    fun get(context: Context): LocalModelManager =
      instance
        ?: synchronized(this) {
          instance ?: LocalModelManager(context).also { instance = it }
        }
  }
}
