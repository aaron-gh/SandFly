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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs [model] in [LocalModelService]'s process, so that Backtalk keeps running when Android stops
 * the model for lack of memory. Then [generate] throws [LocalLlmMemoryException] with a message to
 * speak, as it does when there is not enough free memory to load the model.
 *
 * Stays bound to the service while answering, and unbinds after it has been idle as long as the
 * model stays loaded, so the process and its memory go away.
 */
class RemoteLocalLlm(
  context: Context,
  private val model: LocalModel,
  private val modelFile: File,
  private val processor: OnDeviceAiSettings.Processor,
) : LocalLlm {
  private class Reply(val what: Int, val text: String?)

  private val appContext = context.applicationContext
  private val prefs = SharedPreferencesUtils.getSharedPreferences(appContext)
  private val main = Handler(Looper.getMainLooper())
  private val replies =
    Messenger(
      Handler(Looper.getMainLooper()) {
        pending.complete(it.arg1, Reply(it.what, it.data.getString(LocalModelService.KEY_TEXT)))
        true
      }
    )
  private val pending = PendingRequests<Reply>()
  private val lock = Any()
  // Guarded by lock.
  private var service: Messenger? = null
  private var bound = false
  // Counts the times this has bound, so that a request is failed only when its own process stops.
  private var binding = 0
  private var connected = CountDownLatch(1)
  @Volatile private var currentId = 0
  @Volatile private var cancelRequested = false
  private val unbindIdle = Runnable { unbind() }

  private val connection =
    object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        synchronized(lock) {
          service = Messenger(binder)
          connected.countDown()
        }
      }

      override fun onServiceDisconnected(name: ComponentName) {
        // The process stopped, most likely because Android ran out of memory. Drop the binding,
        // so it is not started again until the next request.
        Log.w(TAG, "The on-device AI process stopped")
        stopAll()
      }

      override fun onBindingDied(name: ComponentName) {
        stopAll()
      }
    }

  override fun generate(prompt: String, jpeg: ByteArray?): String {
    cancelRequested = false
    main.removeCallbacks(unbindIdle)
    val id = NEXT_ID.incrementAndGet()
    val future = pending.add(id)
    currentId = id
    var imageFile: File? = null
    try {
      val (messenger, sentOn) = connect()
      imageFile = jpeg?.let { File(appContext.cacheDir, "on-device-ai-$id.jpg").apply { writeBytes(it) } }
      val request = Message.obtain(null, LocalModelService.MSG_GENERATE)
      request.arg1 = id
      request.replyTo = replies
      request.data.apply {
        putString(LocalModelService.KEY_MODEL_PATH, modelFile.path)
        putString(LocalModelService.KEY_PROCESSOR, processor.id)
        putInt(
          LocalModelService.KEY_MAX_TOKENS,
          model.maxTokens ?: LocalModelService.NO_MAX_TOKENS,
        )
        putBoolean(LocalModelService.KEY_CHECK_MEMORY, !OnDeviceAiSettings.ignoreMemoryLimits(prefs))
        putString(LocalModelService.KEY_PROMPT, prompt)
        imageFile?.let { putString(LocalModelService.KEY_IMAGE_PATH, it.path) }
      }
      // When that process already stopped, no answer will come.
      if (!pending.sent(id, sentOn)) throw stopped()
      messenger.send(request)
      if (cancelRequested) cancel()
      val reply = future.get()
      return when (reply.what) {
        LocalModelService.MSG_ANSWER -> reply.text.orEmpty()
        LocalModelService.MSG_CANCELLED -> throw LocalLlmCancelledException()
        LocalModelService.MSG_LOW_MEMORY -> throw notEnoughMemory()
        MSG_STOPPED -> throw stopped()
        else -> throw LocalLlmException(reply.text ?: "The on-device model failed")
      }
    } catch (_: RemoteException) {
      throw stopped()
    } catch (e: IOException) {
      throw LocalLlmException("Could not pass the image to the model", e)
    } catch (e: ExecutionException) {
      throw LocalLlmException(e.cause?.message ?: "The on-device model failed", e)
    } finally {
      pending.remove(id)
      currentId = 0
      imageFile?.delete()
      main.postDelayed(unbindIdle, IDLE_UNBIND_MS)
    }
  }

  override fun cancel() {
    cancelRequested = true
    val id = currentId
    if (id == 0) return
    // Let Backtalk go on at once. The service drops the answer when it comes.
    pending.complete(id, Reply(LocalModelService.MSG_CANCELLED, null))
    val messenger = synchronized(lock) { service } ?: return
    try {
      messenger.send(Message.obtain(null, LocalModelService.MSG_CANCEL).apply { arg1 = id })
    } catch (e: RemoteException) {
      Log.w(TAG, "Could not cancel", e)
    }
  }

  /** Stops the model and lets its process go. */
  fun close() {
    cancel()
    main.removeCallbacks(unbindIdle)
    unbind()
  }

  /** Binds to the service if needed, waits for it, and returns it with its binding number. */
  private fun connect(): Pair<Messenger, Int> {
    val latch =
      synchronized(lock) {
        service?.let {
          return it to binding
        }
        if (!bound) {
          binding++
          connected = CountDownLatch(1)
          // Not perceptible: when memory runs out, Android stops the model before Backtalk.
          val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              Context.BIND_AUTO_CREATE or Context.BIND_NOT_PERCEPTIBLE
            } else {
              Context.BIND_AUTO_CREATE
            }
          bound =
            appContext.bindService(
              Intent(appContext, LocalModelService::class.java),
              connection,
              flags,
            )
          if (!bound) throw LocalLlmException("Could not start on-device AI")
        }
        connected
      }
    if (!latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      throw LocalLlmException("On-device AI did not start")
    }
    return synchronized(lock) { service?.let { it to binding } } ?: throw stopped()
  }

  private fun unbind() {
    synchronized(lock) {
      if (bound) {
        try {
          appContext.unbindService(connection)
        } catch (e: IllegalArgumentException) {
          Log.w(TAG, "Was not bound", e)
        }
      }
      bound = false
      service = null
      connected.countDown()
    }
  }

  /**
   * Drops the binding and ends the requests sent through it, because the process that was answering
   * them is gone. A request sent after this goes to a new process, and is left alone.
   */
  private fun stopAll() {
    val died =
      synchronized(lock) {
        unbind()
        binding
      }
    pending.died(died, Reply(MSG_STOPPED, null))
  }

  private fun notEnoughMemory() =
    LocalLlmMemoryException(
      appContext.getString(R.string.on_device_ai_not_enough_memory, model.name)
    )

  private fun stopped() =
    LocalLlmMemoryException(appContext.getString(R.string.on_device_ai_stopped, model.name))

  companion object {
    private const val TAG = "RemoteLocalLlm"
    private const val CONNECT_TIMEOUT_SECONDS = 10L

    /** A reply that never comes from the service: its process stopped. */
    private const val MSG_STOPPED = -1

    /** A little longer than the service keeps an idle model loaded. */
    private const val IDLE_UNBIND_MS = LiteRtLmLocalLlm.DEFAULT_IDLE_UNLOAD_MS + 10_000L

    private val NEXT_ID = AtomicInteger(0)
  }
}

/**
 * The requests waiting for an answer from [LocalModelService], with the binding each was sent
 * through. When a binding's process stops, only the requests sent through it are ended, not one
 * that was just sent to the process that replaced it. Safe to use from any thread.
 */
internal class PendingRequests<R> {
  private class Request<R> {
    val reply = CompletableFuture<R>()
    // The binding it was sent through, or 0 before it is sent.
    @Volatile var binding = 0
  }

  private val requests = ConcurrentHashMap<Int, Request<R>>()
  // The last binding whose process stopped.
  @Volatile private var lastDied = 0

  /** Adds request [id], and returns where its reply goes. */
  fun add(id: Int): CompletableFuture<R> = Request<R>().also { requests[id] = it }.reply

  /** Answers request [id], if it is still waiting and has no answer yet. */
  fun complete(id: Int, reply: R) {
    requests[id]?.reply?.complete(reply)
  }

  fun remove(id: Int) {
    requests.remove(id)
  }

  /**
   * Records that request [id] is about to go through [binding]. Returns false when that binding's
   * process already stopped, so no answer would come.
   */
  fun sent(id: Int, binding: Int): Boolean {
    requests[id]?.binding = binding
    return binding > lastDied
  }

  /** Ends with [reply] the requests sent through [binding], whose process stopped, or before it. */
  fun died(binding: Int, reply: R) {
    lastDied = maxOf(lastDied, binding)
    requests.values.forEach { if (it.binding in 1..binding) it.reply.complete(reply) }
  }
}
