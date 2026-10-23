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
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.google.android.accessibility.talkback.acceleration.Accelerators
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs the on-device model in its own process, `:ondeviceai`. A model takes gigabytes of memory,
 * and when the phone runs out, Android stops the process that holds it. In Backtalk's process that
 * stopped the screen reader. Here it only stops the model, and Backtalk says what happened.
 *
 * Backtalk talks to it through [RemoteLocalLlm], with the messages in the companion object. Before
 * loading a model, it checks that the phone has enough free memory for it.
 */
class LocalModelService : Service() {
  private data class Config(
    val path: String,
    val lastModified: Long,
    val processor: OnDeviceAiSettings.Processor,
    val maxTokens: Int?,
  )

  private lateinit var ipcThread: HandlerThread
  private lateinit var messenger: Messenger
  // The requests that came through this instance. The model and its thread are in the companion.
  private val requests = RequestCancellation()

  override fun onCreate() {
    super.onCreate()
    instances.incrementAndGet()
    ipcThread = HandlerThread("on-device-ai-ipc").apply { start() }
    messenger =
      Messenger(
        Handler(ipcThread.looper) {
          handle(it)
          true
        }
      )
  }

  override fun onBind(intent: Intent): IBinder = messenger.binder

  override fun onDestroy() {
    val id = requests.runningId
    if (id != 0 && requests.cancel(id)) llm?.cancel()
    instances.decrementAndGet()
    worker.execute {
      // A new instance may already be using the model. Then it unloads when idle.
      if (instances.get() == 0) {
        llm?.close()
        llm = null
        llmConfig = null
      }
    }
    ipcThread.quitSafely()
    super.onDestroy()
  }

  private fun handle(message: Message) {
    when (message.what) {
      MSG_GENERATE -> {
        val id = message.arg1
        val data = Bundle(message.data)
        val replyTo = message.replyTo
        worker.execute { generate(id, data, replyTo) }
      }
      MSG_CANCEL -> {
        if (requests.cancel(message.arg1)) llm?.cancel()
      }
    }
  }

  private fun generate(id: Int, data: Bundle, replyTo: Messenger) {
    val reply =
      try {
        if (!requests.start(id)) throw LocalLlmCancelledException()
        checkMemory = data.getBoolean(KEY_CHECK_MEMORY, true)
        val jpeg = data.getString(KEY_IMAGE_PATH)?.let { File(it).readBytes() }
        if (requests.isCancelled(id)) throw LocalLlmCancelledException()
        // Replacing the model can take a while, and a cancel meanwhile reaches no model.
        val model = llmFor(data)
        if (requests.isCancelled(id)) throw LocalLlmCancelledException()
        val answer =
          model.generate(data.getString(KEY_PROMPT).orEmpty(), jpeg) { requests.isCancelled(id) }
        if (requests.isCancelled(id)) throw LocalLlmCancelledException()
        Message.obtain(null, MSG_ANSWER).apply { this.data.putString(KEY_TEXT, answer) }
      } catch (_: LocalLlmCancelledException) {
        Message.obtain(null, MSG_CANCELLED)
      } catch (_: LocalLlmMemoryException) {
        Message.obtain(null, MSG_LOW_MEMORY)
      } catch (e: Exception) {
        Message.obtain(null, MSG_FAILED).apply {
          this.data.putString(KEY_TEXT, e.message ?: e.toString())
        }
      } finally {
        requests.finish()
      }
    reply.arg1 = id
    try {
      replyTo.send(reply)
    } catch (e: RemoteException) {
      Log.w(TAG, "Backtalk is gone", e)
    }
  }

  /** The model for [data]'s settings, replacing the loaded one when they changed. */
  private fun llmFor(data: Bundle): LiteRtLmLocalLlm {
    val file = File(data.getString(KEY_MODEL_PATH).orEmpty())
    val maxTokens = data.getInt(KEY_MAX_TOKENS, NO_MAX_TOKENS).takeIf { it != NO_MAX_TOKENS }
    val processor =
      OnDeviceAiSettings.Processor.entries.firstOrNull { it.id == data.getString(KEY_PROCESSOR) }
        ?: OnDeviceAiSettings.Processor.CPU
    val config = Config(file.path, file.lastModified(), processor, maxTokens)
    llm?.let {
      if (config == llmConfig) return it
      // Forget it before closing it, so that a cancel meanwhile does not go to it.
      llm = null
      llmConfig = null
      it.close()
    }
    val appContext = applicationContext
    return LiteRtLmLocalLlm(
        appContext,
        file,
        config.processor,
        // Downloaded the first time the NPU is used.
        if (processor == OnDeviceAiSettings.Processor.NPU) {
          val chip = Accelerators.npuChip ?: throw LocalLlmException("No NPU on this phone")
          NpuRuntime.ensure(appContext, chip).path
        } else {
          null
        },
        maxTokens,
        beforeLoad = { if (checkMemory) checkFreeMemory(appContext, file.length()) },
      )
      .also {
        llm = it
        llmConfig = config
      }
  }

  companion object {
    private const val TAG = "LocalModelService"
    private const val MIB = 1024L * 1024L

    // One thread and one model for the whole process, not for each instance of the service. When
    // Backtalk unbinds while a model loads, the load goes on, and the next bind makes a new
    // instance. Its requests then wait for that load, instead of loading a second model beside it.
    private val worker =
      Executors.newSingleThreadExecutor { Thread(it, "on-device-ai").apply { isDaemon = true } }
    // Only the worker thread changes these.
    @Volatile private var llm: LiteRtLmLocalLlm? = null
    private var llmConfig: Config? = null
    @Volatile private var checkMemory = true
    // How many instances of the service exist, so that the model closes only when none is left.
    private val instances = AtomicInteger(0)

    private fun checkFreeMemory(context: Context, modelBytes: Long) {
      val memory = ActivityManager.MemoryInfo()
      context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
      Log.i(TAG, "${memory.availMem / MIB} MB free for a ${modelBytes / MIB} MB model")
      if (!OnDeviceAiSettings.hasFreeMemoryFor(modelBytes, memory.availMem, memory.lowMemory)) {
        throw LocalLlmMemoryException("Not enough free memory")
      }
    }

    /** Backtalk asks for an answer. arg1 is the request ID, replyTo gets the answer. */
    const val MSG_GENERATE = 1

    /** Backtalk cancels the request whose ID is in arg1, and any before it. */
    const val MSG_CANCEL = 2

    /** The answer, in [KEY_TEXT]. */
    const val MSG_ANSWER = 3

    const val MSG_CANCELLED = 4

    /** There was not enough free memory to load the model. */
    const val MSG_LOW_MEMORY = 5

    /** Any other failure, described in [KEY_TEXT]. */
    const val MSG_FAILED = 6

    const val KEY_MODEL_PATH = "model_path"
    const val KEY_PROCESSOR = "processor"
    const val KEY_MAX_TOKENS = "max_tokens"
    const val KEY_PROMPT = "prompt"

    /** False to load without checking for free memory, to test running out. */
    const val KEY_CHECK_MEMORY = "check_memory"

    /** A JPEG file to look at. Images go through a file because a message can only hold 1 MB. */
    const val KEY_IMAGE_PATH = "image_path"
    const val KEY_TEXT = "text"
    const val NO_MAX_TOKENS = -1
  }
}

/**
 * Which requests are cancelled, and which one is running, for one instance of
 * [LocalModelService]. Request IDs only grow, and a cancel covers its request and any before it.
 * Safe to use from any thread.
 */
internal class RequestCancellation {
  private val cancelledUpTo = AtomicInteger(0)

  /** The request the model is working on, or 0. */
  @Volatile
  var runningId = 0
    private set

  /**
   * Cancels [id] and any request before it. Returns true when one of them is running, so the model
   * should stop. Otherwise the request stops when it checks [isCancelled].
   */
  fun cancel(id: Int): Boolean {
    cancelledUpTo.accumulateAndGet(id, ::maxOf)
    val running = runningId
    return running != 0 && running <= id
  }

  fun isCancelled(id: Int): Boolean = id <= cancelledUpTo.get()

  /**
   * Marks [id] as running, and returns false when it is already cancelled. Checks after marking it,
   * so a cancel at the same time either sees it running or is seen here.
   */
  fun start(id: Int): Boolean {
    runningId = id
    if (isCancelled(id)) {
      runningId = 0
      return false
    }
    return true
  }

  fun finish() {
    runningId = 0
  }
}
