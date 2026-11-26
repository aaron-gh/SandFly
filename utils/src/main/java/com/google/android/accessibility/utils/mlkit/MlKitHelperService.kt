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

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.google.android.accessibility.utils.mlkit.MlKitProtocol.DEFAULT_THRESHOLD
import com.google.android.accessibility.utils.ocr.CyrillicOcr
import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer.Script
import com.google.android.accessibility.utils.ocr.TesseractRecognizer
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executor

/**
 * Runs the ML Kit APIs that load their code from Google Play services, language identification and
 * text recognition, in their own process, `:mlkit`. Tesseract, which reads Cyrillic text, runs here
 * too, so that a crash in its native code stops only this process. Android stops every process that has code of
 * an app loaded when that app is updated; in SandFly's process, an update of Google Play services
 * then stopped the screen reader, and Android left it off as a crashed accessibility service. Here
 * it stops only this process, which starts again with the next request.
 *
 * SandFly talks to it through [MlKitHelper], with the messages in [MlKitProtocol].
 */
class MlKitHelperService : Service() {
  private lateinit var thread: HandlerThread
  private lateinit var handler: Handler
  private lateinit var messenger: Messenger
  private lateinit var onThread: Executor

  // Used on [thread] only.
  private val identifiers = HashMap<Float, LanguageIdentifier>()
  private val recognizers = HashMap<Script, TextRecognizer>()
  private var tesseract: TesseractRecognizer? = null

  override fun onCreate() {
    super.onCreate()
    // ML Kit's content provider initializes it in the main process only.
    MlKitContext.initializeIfNeeded(applicationContext)
    thread = HandlerThread("mlkit-helper").apply { start() }
    handler = Handler(thread.looper)
    onThread = Executor { handler.post(it) }
    messenger =
      Messenger(
        Handler(thread.looper) {
          handle(it)
          true
        }
      )
    // Answers that an earlier process left behind.
    File(cacheDir, MlKitProtocol.TEXT_FILE_DIR).listFiles()?.forEach { it.delete() }
  }

  override fun onBind(intent: Intent): IBinder = messenger.binder

  override fun onDestroy() {
    handler.post {
      identifiers.values.forEach { closeQuietly(it::close) }
      identifiers.clear()
      recognizers.values.forEach { closeQuietly(it::close) }
      recognizers.clear()
      tesseract?.close()
      tesseract = null
    }
    thread.quitSafely()
    super.onDestroy()
  }

  private fun handle(message: Message) {
    val id = message.arg1
    val replyTo = message.replyTo ?: return
    val data = Bundle(message.data)
    // Anything that escapes would stop this process and every request in it.
    try {
      when (message.what) {
        MlKitProtocol.MSG_IDENTIFY_LANGUAGE ->
          identifier(data)
            .identifyLanguage(data.getString(MlKitProtocol.KEY_TEXT).orEmpty())
            .answer(replyTo, id) { putString(MlKitProtocol.KEY_LANGUAGE, it) }
        MlKitProtocol.MSG_IDENTIFY_POSSIBLE_LANGUAGES ->
          identifier(data)
            .identifyPossibleLanguages(data.getString(MlKitProtocol.KEY_TEXT).orEmpty())
            .answer(replyTo, id) { MlKitProtocol.putLanguages(this, it) }
        MlKitProtocol.MSG_RECOGNIZE_TEXT -> recognize(data, replyTo, id)
        MlKitProtocol.MSG_INSTALL_TEXT_RECOGNIZER -> {
          // Tesseract's models are downloaded in SandFly's settings.
          if (script(data) == Script.CYRILLIC) {
            throw MlKitException("No Cyrillic module", MlKitException.INVALID_ARGUMENT)
          }
          ModuleInstall.getClient(this)
            .installModules(
              ModuleInstallRequest.newBuilder().addApi(recognizer(script(data))).build()
            )
            .answer(replyTo, id) {}
        }
        else -> Log.w(TAG, "Unknown request ${message.what}")
      }
    } catch (e: Throwable) {
      fail(replyTo, id, e)
    }
  }

  private fun recognize(data: Bundle, replyTo: Messenger, id: Int) {
    @Suppress("DEPRECATION") // getParcelable(String, Class) is API 33.
    val bitmap =
      data.getParcelable<Bitmap>(MlKitProtocol.KEY_IMAGE)
        ?: throw MlKitException("No image", MlKitException.INVALID_ARGUMENT)
    val script = script(data)
    val recognition =
      if (script == Script.CYRILLIC) {
        recognizeWithTesseract(bitmap, data)
      } else {
        val image = InputImage.fromBitmap(bitmap, data.getInt(MlKitProtocol.KEY_ROTATION))
        recognizer(script).process(image)
      }
    recognition.addOnCompleteListener(onThread) { task ->
      // Tesseract recycles its bitmaps itself, once it is done with them.
      if (script != Script.CYRILLIC) bitmap.recycle()
      if (!task.isSuccessful) {
        fail(replyTo, id, task.exception)
        return@addOnCompleteListener
      }
      val reply =
        try {
          val bytes = MlKitProtocol.encodeText(task.result)
          result(id).apply {
            if (bytes.size <= MlKitProtocol.MAX_INLINE_TEXT_BYTES) {
              this.data.putByteArray(MlKitProtocol.KEY_TEXT_DATA, bytes)
            } else {
              val dir = File(cacheDir, MlKitProtocol.TEXT_FILE_DIR).apply { mkdirs() }
              val file = File(dir, "text-$id")
              file.writeBytes(bytes)
              this.data.putString(MlKitProtocol.KEY_TEXT_FILE, file.path)
            }
          }
        } catch (e: Exception) {
          fail(replyTo, id, e)
          return@addOnCompleteListener
        }
      send(replyTo, reply)
    }
  }

  /** Reads Cyrillic text with Tesseract, with the models SandFly asked for. Recycles [bitmap]. */
  private fun recognizeWithTesseract(bitmap: Bitmap, data: Bundle): Task<Text> {
    val models = data.getString(MlKitProtocol.KEY_TESSERACT_MODELS)
    if (models == null || !CyrillicOcr.isValidLanguages(models)) {
      bitmap.recycle()
      throw MlKitException("No Tesseract models: $models", MlKitException.INVALID_ARGUMENT)
    }
    val recognizer = tesseract ?: TesseractRecognizer(this).also { tesseract = it }
    return recognizer.recognize(
      bitmap,
      data.getInt(MlKitProtocol.KEY_ROTATION),
      models,
      CyrillicOcr.languageOf(models.substringBefore('+')),
    )
  }

  private fun identifier(data: Bundle): LanguageIdentifier {
    val threshold = data.getFloat(MlKitProtocol.KEY_THRESHOLD, DEFAULT_THRESHOLD)
    identifiers[threshold]?.let {
      return it
    }
    // Callers use one or two thresholds; any more are not kept loaded.
    if (identifiers.size >= MAX_IDENTIFIERS) {
      identifiers.values.forEach { closeQuietly(it::close) }
      identifiers.clear()
    }
    val client =
      if (threshold == DEFAULT_THRESHOLD) {
        LanguageIdentification.getClient()
      } else {
        LanguageIdentification.getClient(
          LanguageIdentificationOptions.Builder().setConfidenceThreshold(threshold).build()
        )
      }
    return client.also { identifiers[threshold] = it }
  }

  private fun script(data: Bundle): Script =
    Script.entries.firstOrNull { it.name == data.getString(MlKitProtocol.KEY_SCRIPT) }
      ?: Script.LATIN

  private fun recognizer(script: Script): TextRecognizer {
    recognizers[script]?.let {
      return it
    }
    // Another script's model stays loaded only as long as the language that wanted it is in use.
    if (script != Script.LATIN) {
      for (other in recognizers.keys.filter { it != Script.LATIN }) {
        recognizers.remove(other)?.let { closeQuietly(it::close) }
      }
    }
    return TextRecognition.getClient(optionsFor(script)).also { recognizers[script] = it }
  }

  private fun <T> Task<T>.answer(replyTo: Messenger, id: Int, put: Bundle.(T) -> Unit) {
    addOnCompleteListener(onThread) { task ->
      if (task.isSuccessful) {
        send(replyTo, result(id).apply { data.put(task.result) })
      } else {
        fail(replyTo, id, task.exception)
      }
    }
  }

  private fun result(id: Int): Message = Message.obtain(null, MlKitProtocol.MSG_RESULT).apply { arg1 = id }

  private fun fail(replyTo: Messenger, id: Int, e: Throwable?) {
    Log.w(TAG, "Request failed: $e")
    val message =
      Message.obtain(null, MlKitProtocol.MSG_ERROR).apply {
        arg1 = id
        arg2 = (e as? MlKitException)?.errorCode ?: MlKitException.UNKNOWN
        data.putString(MlKitProtocol.KEY_ERROR, e?.message ?: e?.toString() ?: "Failed")
      }
    send(replyTo, message)
  }

  private fun send(replyTo: Messenger, message: Message) {
    try {
      replyTo.send(message)
    } catch (e: RemoteException) {
      Log.w(TAG, "SandFly is gone", e)
    }
  }

  private fun closeQuietly(close: () -> Unit) {
    try {
      close()
    } catch (e: RuntimeException) {
      Log.w(TAG, "Not closed: $e")
    }
  }

  private companion object {
    const val TAG = "MlKitHelperService"
    const val MAX_IDENTIFIERS = 4

    fun optionsFor(script: Script): TextRecognizerOptionsInterface =
      when (script) {
        Script.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
        Script.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
        Script.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
        Script.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
        Script.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
        // Read by Tesseract; never asked for.
        Script.CYRILLIC -> throw MlKitException("No ML Kit Cyrillic recognizer", MlKitException.INVALID_ARGUMENT)
      }
  }
}
