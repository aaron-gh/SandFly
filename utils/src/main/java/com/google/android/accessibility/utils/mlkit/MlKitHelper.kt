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

package com.google.android.accessibility.utils.mlkit

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer.Script
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.common.MlKitException
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.vision.text.Text
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor

/**
 * Passes ML Kit language identification and text recognition requests to [MlKitHelperService],
 * which runs them in the `:mlkit` process, and returns their results as tasks, as ML Kit does.
 * Listeners added without an executor run on the main thread; the others run on this client's
 * thread.
 *
 * The service is bound with the first request and unbound after [IDLE_UNBIND_MS] without one. When
 * its process stops, such as when Google Play services is updated, the requests it had fail with
 * [MlKitException.ABORTED], and the next request starts it again.
 */
class MlKitHelper
@VisibleForTesting
constructor(
  private val context: Context,
  looper: Looper,
  private val connector: Connector = BindConnector(context),
) {
  /** Binds and unbinds [MlKitHelperService]; replaced in tests. */
  interface Connector {
    /** Binds, with the connection's callbacks run by [executor]. Returns false when it can't. */
    fun bind(connection: ServiceConnection, executor: Executor): Boolean

    fun unbind(connection: ServiceConnection)
  }

  private class BindConnector(private val context: Context) : Connector {
    override fun bind(connection: ServiceConnection, executor: Executor): Boolean {
      val intent = Intent(context, MlKitHelperService::class.java)
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.bindService(intent, Context.BIND_AUTO_CREATE, executor, connection)
      } else {
        // Run on the main thread, which passes them on.
        context.bindService(
          intent,
          object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder) =
              executor.execute { connection.onServiceConnected(name, service) }

            override fun onServiceDisconnected(name: ComponentName?) =
              executor.execute { connection.onServiceDisconnected(name) }

            override fun onBindingDied(name: ComponentName?) =
              executor.execute { connection.onBindingDied(name) }

            // Android calls it from API 28.
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onNullBinding(name: ComponentName?) =
              executor.execute { connection.onNullBinding(name) }
          }.also { legacyConnections[connection] = it },
          Context.BIND_AUTO_CREATE,
        )
      }
    }

    override fun unbind(connection: ServiceConnection) {
      val bound = legacyConnections.remove(connection) ?: connection
      try {
        context.unbindService(bound)
      } catch (e: IllegalArgumentException) {
        LogUtils.w(TAG, "Was not bound: %s", e)
      }
    }

    private val legacyConnections = HashMap<ServiceConnection, ServiceConnection>()
  }

  private class Pending(
    val source: TaskCompletionSource<Any?>,
    val parse: (Bundle) -> Any?,
    /** The request message, until it is sent. */
    var message: Message?,
    val timeout: Runnable,
  )

  private val handler = Handler(looper)
  private val onThread = Executor { handler.post(it) }
  private val replies =
    Messenger(
      Handler(looper) {
        onReply(it)
        true
      }
    )

  // Used on [handler]'s thread only.
  private val pending = LinkedHashMap<Int, Pending>()
  private var service: Messenger? = null
  private var bound = false
  private var nextId = 0
  private val unbindIdle = Runnable { if (pending.isEmpty()) unbind() }
  private val connectTimeout = Runnable {
    LogUtils.w(TAG, "The ML Kit process did not start")
    stopAll("The ML Kit process did not start")
  }

  private val connection =
    object : ServiceConnection {
      // Each ignores what comes after an unbind, such as a connection that came too late. Names
      // are nullable: Robolectric's unbind calls onServiceDisconnected with none.
      override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        if (!bound) return
        handler.removeCallbacks(connectTimeout)
        val messenger = Messenger(binder)
        service = messenger
        for ((id, request) in pending.entries.toList()) {
          request.message?.let { send(id, it) }
        }
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        if (!bound) return
        LogUtils.w(TAG, "The ML Kit process stopped")
        stopAll("The ML Kit process stopped")
      }

      override fun onBindingDied(name: ComponentName?) {
        if (bound) stopAll("The ML Kit process stopped")
      }

      override fun onNullBinding(name: ComponentName?) {
        if (bound) stopAll("The ML Kit process did not start")
      }
    }

  /** As [com.google.mlkit.nl.languageid.LanguageIdentifier.identifyLanguage]. */
  fun identifyLanguage(text: String, threshold: Float?): Task<String> =
    request(MlKitProtocol.MSG_IDENTIFY_LANGUAGE, identifyData(text, threshold)) {
      it.getString(MlKitProtocol.KEY_LANGUAGE) ?: UNDETERMINED
    }

  /** As [com.google.mlkit.nl.languageid.LanguageIdentifier.identifyPossibleLanguages]. */
  fun identifyPossibleLanguages(text: String, threshold: Float?): Task<List<IdentifiedLanguage>> =
    request(MlKitProtocol.MSG_IDENTIFY_POSSIBLE_LANGUAGES, identifyData(text, threshold)) {
      MlKitProtocol.languages(it)
    }

  /**
   * Recognizes the text in [bitmap], turned by [rotation] degrees, with the recognizer for
   * [script]. Cyrillic is read by Tesseract with [models], such as "rus+eng". The bitmap must stay
   * as it is until the task completes.
   */
  fun recognizeText(bitmap: Bitmap, rotation: Int, script: Script, models: String? = null): Task<Text> {
    val data = Bundle()
    data.putInt(MlKitProtocol.KEY_ROTATION, rotation)
    data.putString(MlKitProtocol.KEY_SCRIPT, script.name)
    if (models != null) data.putString(MlKitProtocol.KEY_TESSERACT_MODELS, models)
    // Hardware bitmaps, such as screenshots, can't always be passed to another process.
    val image =
      if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
          ?: return Tasks.forException(MlKitException("Image not copied", MlKitException.INTERNAL))
      } else {
        bitmap
      }
    // Passed in shared memory, not in the message's 1 MB.
    data.putParcelable(MlKitProtocol.KEY_IMAGE, image)
    return request(MlKitProtocol.MSG_RECOGNIZE_TEXT, data, ::readText)
  }

  /** Asks Google Play services to download the text recognizer for [script]. */
  fun installTextRecognizer(script: Script): Task<Void?> =
    request(MlKitProtocol.MSG_INSTALL_TEXT_RECOGNIZER, Bundle().apply {
      putString(MlKitProtocol.KEY_SCRIPT, script.name)
    }) {
      null
    }

  private fun identifyData(text: String, threshold: Float?) =
    Bundle().apply {
      putString(MlKitProtocol.KEY_TEXT, text.take(MlKitProtocol.MAX_IDENTIFY_CHARS))
      putFloat(MlKitProtocol.KEY_THRESHOLD, threshold ?: MlKitProtocol.DEFAULT_THRESHOLD)
    }

  @Suppress("UNCHECKED_CAST")
  private fun <T> request(what: Int, data: Bundle, parse: (Bundle) -> T): Task<T> {
    val source = TaskCompletionSource<Any?>()
    handler.post {
      val id = ++nextId
      val message = Message.obtain(null, what).apply {
        arg1 = id
        replyTo = replies
        this.data = data
      }
      val timeout = Runnable {
        fail(id, MlKitException("No answer from the ML Kit process", MlKitException.DEADLINE_EXCEEDED))
      }
      pending[id] = Pending(source, parse, message, timeout)
      handler.removeCallbacks(unbindIdle)
      handler.postDelayed(timeout, REQUEST_TIMEOUT_MS)
      val messenger = service
      if (messenger != null) {
        send(id, message)
      } else if (!bound) {
        bound =
          try {
            connector.bind(connection, onThread)
          } catch (e: RuntimeException) {
            LogUtils.w(TAG, "Cannot bind the ML Kit process: %s", e)
            false
          }
        if (bound) {
          handler.postDelayed(connectTimeout, CONNECT_TIMEOUT_MS)
        } else {
          // Android asks for an unbind even after a bind that failed.
          connector.unbind(connection)
          fail(id, MlKitException("Cannot start the ML Kit process", MlKitException.INTERNAL))
        }
      }
    }
    return source.task as Task<T>
  }

  private fun send(id: Int, message: Message) {
    val messenger = service ?: return
    pending[id]?.message = null
    try {
      messenger.send(message)
    } catch (e: RemoteException) {
      // The process is gone; onServiceDisconnected follows.
      fail(id, aborted("The ML Kit process stopped"))
    } catch (e: RuntimeException) {
      // Such as a recycled bitmap, or a message too large.
      fail(id, MlKitException("Request not sent: $e", MlKitException.INVALID_ARGUMENT))
    }
  }

  private fun onReply(message: Message) {
    val id = message.arg1
    val request = pending[id] ?: return discard(message)
    when (message.what) {
      MlKitProtocol.MSG_RESULT -> {
        val result =
          try {
            request.parse(message.data)
          } catch (e: Exception) {
            fail(id, MlKitException("Answer not read: $e", MlKitException.DATA_LOSS))
            return
          }
        finish(id)
        request.source.trySetResult(result)
      }
      MlKitProtocol.MSG_ERROR -> {
        val text = message.data.getString(MlKitProtocol.KEY_ERROR).orEmpty()
        fail(id, MlKitException(text, message.arg2))
      }
    }
  }

  /** Deletes the file of an answer that no request waits for any more. */
  private fun discard(message: Message) {
    message.data.getString(MlKitProtocol.KEY_TEXT_FILE)?.let { deleteTextFile(it) }
  }

  private fun fail(id: Int, e: Exception) {
    val request = pending[id] ?: return
    finish(id)
    request.source.trySetException(e)
  }

  private fun finish(id: Int) {
    pending.remove(id)?.let { handler.removeCallbacks(it.timeout) }
    if (pending.isEmpty()) {
      handler.removeCallbacks(unbindIdle)
      handler.postDelayed(unbindIdle, IDLE_UNBIND_MS)
    }
  }

  /** Fails every request and unbinds, so the process starts again only with the next request. */
  private fun stopAll(reason: String) {
    unbind()
    for (id in pending.keys.toList()) fail(id, aborted(reason))
  }

  private fun unbind() {
    handler.removeCallbacks(connectTimeout)
    if (bound) connector.unbind(connection)
    bound = false
    service = null
  }

  private fun readText(data: Bundle): Text {
    data.getByteArray(MlKitProtocol.KEY_TEXT_DATA)?.let {
      return MlKitProtocol.decodeText(it)
    }
    val path = data.getString(MlKitProtocol.KEY_TEXT_FILE) ?: throw IOException("No text")
    try {
      return MlKitProtocol.decodeText(textFile(path).readBytes())
    } finally {
      deleteTextFile(path)
    }
  }

  /** The answer file at [path], only if it is in the answers' directory. */
  private fun textFile(path: String): File {
    val dir = File(context.cacheDir, MlKitProtocol.TEXT_FILE_DIR).canonicalFile
    val file = File(path).canonicalFile
    if (file.parentFile != dir) throw IOException("Not an answer file: $path")
    return file
  }

  private fun deleteTextFile(path: String) {
    try {
      textFile(path).delete()
    } catch (e: IOException) {
      LogUtils.w(TAG, "Answer file not deleted: %s", e)
    }
  }

  @VisibleForTesting val pendingCount: Int
    get() = pending.size

  @VisibleForTesting val isBound: Boolean
    get() = bound

  companion object {
    private const val TAG = "MlKitHelper"
    private const val UNDETERMINED = "und"

    /** How long the process stays bound after the last answer. */
    @VisibleForTesting const val IDLE_UNBIND_MS = 60_000L

    @VisibleForTesting const val CONNECT_TIMEOUT_MS = 10_000L

    /** Longer than any recognition; only stops requests that would otherwise wait forever. */
    @VisibleForTesting const val REQUEST_TIMEOUT_MS = 60_000L

    @Volatile private var appContext: Context? = null
    @Volatile private var instance: MlKitHelper? = null

    /** Called when SandFly's process starts, for the users that have no context. */
    @JvmStatic
    fun initialize(context: Context) {
      appContext = context.applicationContext ?: context
    }

    /** The client for this process. */
    @JvmStatic
    fun get(context: Context): MlKitHelper {
      if (appContext == null) initialize(context)
      return get()
    }

    /** The client for this process. Throws [IllegalStateException] before [initialize]. */
    @JvmStatic
    fun get(): MlKitHelper =
      instance
        ?: synchronized(this) {
          instance
            ?: run {
              val context = appContext ?: throw IllegalStateException("MlKitHelper not initialized")
              val thread = HandlerThread("mlkit-helper-client").apply { start() }
              MlKitHelper(context, thread.looper)
            }.also { instance = it }
        }

    private fun aborted(reason: String) = MlKitException(reason, MlKitException.ABORTED)
  }
}
