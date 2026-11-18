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
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer
import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer.Script
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.time.Duration
import java.util.Locale
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MlKitHelperTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val main = Looper.getMainLooper()
  private val name = ComponentName(context, MlKitHelperService::class.java)

  /** Stands in for the service: records requests, and answers with [answer]. */
  private inner class FakeService {
    val requests = mutableListOf<Message>()
    var answer: ((Message) -> Message?)? = null
    val messenger =
      Messenger(
        Handler(main) { request ->
          val copy = Message.obtain(request)
          requests += copy
          answer?.invoke(copy)?.let { reply ->
            reply.arg1 = copy.arg1
            copy.replyTo.send(reply)
          }
          true
        }
      )
  }

  private inner class FakeConnector : MlKitHelper.Connector {
    var binds = 0
    var unbinds = 0
    var bindWorks = true
    var connection: ServiceConnection? = null
    var executor: Executor? = null

    override fun bind(connection: ServiceConnection, executor: Executor): Boolean {
      binds++
      this.connection = connection
      this.executor = executor
      return bindWorks
    }

    override fun unbind(connection: ServiceConnection) {
      unbinds++
    }

    fun connect(service: FakeService) =
      executor!!.execute { connection!!.onServiceConnected(name, service.messenger.binder) }

    fun die() = executor!!.execute { connection!!.onServiceDisconnected(name) }
  }

  private val connector = FakeConnector()
  private val helper = MlKitHelper(context, main, connector)

  @Before
  fun setUp() {
    // InputImage.fromBitmap needs it, as on a phone, where ML Kit sets it up as SandFly starts.
    MlKitContext.initializeIfNeeded(context)
  }

  private fun idle() = shadowOf(main).idle()

  private fun result(build: Bundle.() -> Unit = {}): Message =
    Message.obtain(null, MlKitProtocol.MSG_RESULT).apply { data.build() }

  private fun error(code: Int, text: String): Message =
    Message.obtain(null, MlKitProtocol.MSG_ERROR).apply {
      arg2 = code
      data.putString(MlKitProtocol.KEY_ERROR, text)
    }

  private fun <T> Task<T>.mlKitError(): MlKitException {
    assertTrue("task should have failed", isComplete && !isSuccessful)
    return exception as MlKitException
  }

  @Test
  fun firstRequest_bindsAndIsSentOnceConnected() {
    val service = FakeService()
    service.answer = { result { putString(MlKitProtocol.KEY_LANGUAGE, "de") } }

    val task = helper.identifyLanguage("Guten Morgen", 0.5f)
    idle()
    assertEquals(1, connector.binds)
    assertTrue(service.requests.isEmpty())
    assertFalse(task.isComplete)

    connector.connect(service)
    idle()

    val request = service.requests.single()
    assertEquals(MlKitProtocol.MSG_IDENTIFY_LANGUAGE, request.what)
    assertEquals("Guten Morgen", request.data.getString(MlKitProtocol.KEY_TEXT))
    assertEquals(0.5f, request.data.getFloat(MlKitProtocol.KEY_THRESHOLD))
    assertEquals("de", task.result)
    assertEquals(0, helper.pendingCount)
  }

  @Test
  fun laterRequests_useTheSameBinding() {
    val service = FakeService()
    service.answer = { result { putString(MlKitProtocol.KEY_LANGUAGE, "fr") } }
    helper.identifyLanguage("a", null)
    idle()
    connector.connect(service)
    idle()

    val second = helper.identifyLanguage("b", null)
    idle()

    assertEquals(1, connector.binds)
    assertEquals(MlKitProtocol.DEFAULT_THRESHOLD, service.requests[1].data.getFloat(MlKitProtocol.KEY_THRESHOLD))
    assertEquals("fr", second.result)
  }

  @Test
  fun possibleLanguages_areReadFromTheAnswer() {
    val service = FakeService()
    val languages = listOf(IdentifiedLanguage("nl", 0.6f), IdentifiedLanguage("af", 0.3f))
    service.answer = { result { MlKitProtocol.putLanguages(this, languages) } }

    val task = helper.identifyPossibleLanguages("Goedemorgen", null)
    idle()
    connector.connect(service)
    idle()

    assertEquals(languages, task.result)
  }

  @Test
  fun longText_isShortenedForTheMessage() {
    val service = FakeService()
    helper.identifyLanguage("a".repeat(MlKitProtocol.MAX_IDENTIFY_CHARS + 10), null)
    idle()
    connector.connect(service)
    idle()

    assertEquals(
      MlKitProtocol.MAX_IDENTIFY_CHARS,
      service.requests.single().data.getString(MlKitProtocol.KEY_TEXT)!!.length,
    )
  }

  @Test
  fun processStopsMidRequest_requestFails_nextRequestStartsItAgain() {
    val service = FakeService()
    val first = helper.identifyLanguage("a", null)
    val queued = helper.identifyPossibleLanguages("b", null)
    idle()
    connector.connect(service)
    idle()
    assertFalse(first.isComplete)

    connector.die()
    idle()

    assertEquals(MlKitException.ABORTED, first.mlKitError().errorCode)
    assertEquals(MlKitException.ABORTED, queued.mlKitError().errorCode)
    assertEquals(1, connector.unbinds)
    assertFalse(helper.isBound)

    val again = FakeService()
    again.answer = { result { putString(MlKitProtocol.KEY_LANGUAGE, "en") } }
    val next = helper.identifyLanguage("c", null)
    idle()
    assertEquals(2, connector.binds)
    connector.connect(again)
    idle()
    assertEquals("en", next.result)
  }

  @Test
  fun failureInTheProcess_keepsItsErrorCode() {
    val service = FakeService()
    service.answer = { error(MlKitException.UNAVAILABLE, "Waiting for the module") }

    val task = helper.identifyLanguage("a", null)
    idle()
    connector.connect(service)
    idle()

    val e = task.mlKitError()
    assertEquals(MlKitException.UNAVAILABLE, e.errorCode)
    assertEquals("Waiting for the module", e.message)
    assertTrue(LanguageTextRecognizer.isUnavailable(e))
  }

  @Test
  fun bindFails_requestFails() {
    connector.bindWorks = false

    val task = helper.identifyLanguage("a", null)
    idle()

    assertEquals(MlKitException.INTERNAL, task.mlKitError().errorCode)
    assertFalse(helper.isBound)
    assertEquals(0, helper.pendingCount)
  }

  @Test
  fun processDoesNotStart_requestFailsAfterTimeout() {
    val task = helper.identifyLanguage("a", null)
    idle()

    shadowOf(main).idleFor(Duration.ofMillis(MlKitHelper.CONNECT_TIMEOUT_MS))

    assertEquals(MlKitException.ABORTED, task.mlKitError().errorCode)
    assertEquals(1, connector.unbinds)
  }

  @Test
  fun noAnswer_requestFailsAfterTimeout_lateAnswerIgnored() {
    val service = FakeService()
    val task = helper.identifyLanguage("a", null)
    idle()
    connector.connect(service)
    idle()

    shadowOf(main).idleFor(Duration.ofMillis(MlKitHelper.REQUEST_TIMEOUT_MS))
    assertEquals(MlKitException.DEADLINE_EXCEEDED, task.mlKitError().errorCode)

    val late = result { putString(MlKitProtocol.KEY_LANGUAGE, "de") }
    late.arg1 = service.requests.single().arg1
    service.requests.single().replyTo.send(late)
    idle()
    assertEquals(MlKitException.DEADLINE_EXCEEDED, task.mlKitError().errorCode)
  }

  @Test
  fun idle_unbindsAfterTheLastAnswer() {
    val service = FakeService()
    service.answer = { result { putString(MlKitProtocol.KEY_LANGUAGE, "de") } }
    helper.identifyLanguage("a", null)
    idle()
    connector.connect(service)
    idle()
    assertTrue(helper.isBound)

    shadowOf(main).idleFor(Duration.ofMillis(MlKitHelper.IDLE_UNBIND_MS - 1000))
    assertTrue(helper.isBound)
    shadowOf(main).idleFor(Duration.ofMillis(1000))

    assertFalse(helper.isBound)
    assertEquals(1, connector.unbinds)
  }

  @Test
  fun recognizedText_inTheMessage() {
    val service = FakeService()
    service.answer = {
      result { putByteArray(MlKitProtocol.KEY_TEXT_DATA, MlKitProtocol.encodeText(MlKitProtocolTest.sample())) }
    }
    val bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)

    val task = helper.recognizeText(bitmap, 90, Script.JAPANESE)
    idle()
    connector.connect(service)
    idle()

    val request = service.requests.single()
    assertEquals(MlKitProtocol.MSG_RECOGNIZE_TEXT, request.what)
    assertEquals(90, request.data.getInt(MlKitProtocol.KEY_ROTATION))
    assertEquals("JAPANESE", request.data.getString(MlKitProtocol.KEY_SCRIPT))
    assertEquals("Hello world\nÜber 你好", task.result.text)
    assertEquals(2, task.result.textBlocks.size)
  }

  @Test
  fun recognizedText_inAFile_isReadAndDeleted() {
    val dir = File(context.cacheDir, MlKitProtocol.TEXT_FILE_DIR).apply { mkdirs() }
    val file = File(dir, "text-1").apply { writeBytes(MlKitProtocol.encodeText(MlKitProtocolTest.sample())) }
    val service = FakeService()
    service.answer = { result { putString(MlKitProtocol.KEY_TEXT_FILE, file.path) } }

    val task = helper.recognizeText(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), 0, Script.LATIN)
    idle()
    connector.connect(service)
    idle()

    assertEquals(2, task.result.textBlocks.size)
    assertFalse(file.exists())
  }

  @Test
  fun answerFileOutsideItsDirectory_isRejected() {
    val outside = File(context.cacheDir, "other").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    val service = FakeService()
    service.answer = { result { putString(MlKitProtocol.KEY_TEXT_FILE, outside.path) } }

    val task = helper.recognizeText(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), 0, Script.LATIN)
    idle()
    connector.connect(service)
    idle()

    assertEquals(MlKitException.DATA_LOSS, task.mlKitError().errorCode)
    assertTrue(outside.exists())
  }

  @Test
  fun damagedAnswer_failsTheRequest() {
    val service = FakeService()
    service.answer = { result { putByteArray(MlKitProtocol.KEY_TEXT_DATA, byteArrayOf(9, 9)) } }

    val task = helper.recognizeText(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), 0, Script.LATIN)
    idle()
    connector.connect(service)
    idle()

    assertEquals(MlKitException.DATA_LOSS, task.mlKitError().errorCode)
  }

  @Test
  fun textRecognizer_withoutTheScriptsModel_usesLatinAndAsksForTheDownload() {
    val service = FakeService()
    service.answer = { request ->
      when {
        request.what == MlKitProtocol.MSG_INSTALL_TEXT_RECOGNIZER -> result()
        request.data.getString(MlKitProtocol.KEY_SCRIPT) == "JAPANESE" ->
          error(MlKitException.UNAVAILABLE, "Downloading")
        else ->
          result {
            putByteArray(MlKitProtocol.KEY_TEXT_DATA, MlKitProtocol.encodeText(MlKitProtocolTest.sample()))
          }
      }
    }
    val recognizer =
      LanguageTextRecognizer(context, { listOf(Locale.JAPAN) }, { 0L }, helper)

    val task = recognizer.process(InputImage.fromBitmap(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), 0))
    idle()
    connector.connect(service)
    idle()

    assertEquals(2, task.result.textBlocks.size)
    val asked = service.requests.map { it.what to it.data.getString(MlKitProtocol.KEY_SCRIPT) }
    assertEquals(
      listOf(
        MlKitProtocol.MSG_RECOGNIZE_TEXT to "JAPANESE",
        MlKitProtocol.MSG_INSTALL_TEXT_RECOGNIZER to "JAPANESE",
        MlKitProtocol.MSG_RECOGNIZE_TEXT to "LATIN",
      ),
      asked,
    )
    assertEquals(Script.LATIN, recognizer.script())
  }

  @Test
  fun closedTextRecognizer_failsWithoutARequest() {
    val recognizer = LanguageTextRecognizer(context, { listOf(Locale.US) }, { 0L }, helper)
    recognizer.close()

    val task = recognizer.process(InputImage.fromBitmap(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), 0))
    idle()

    assertTrue(task.isComplete && !task.isSuccessful)
    assertEquals(0, connector.binds)
  }

  @Test
  fun closedLanguageIdentifier_failsWithoutARequest() {
    MlKitHelper.initialize(context)
    val identifier = RemoteLanguageIdentifier(0.5f)
    identifier.close()

    val task = identifier.identifyLanguage("a")

    assertTrue(task.isComplete && !task.isSuccessful)
    assertNull(task.exception?.cause)
  }
}
