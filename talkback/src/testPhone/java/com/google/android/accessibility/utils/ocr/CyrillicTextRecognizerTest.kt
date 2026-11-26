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

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.google.android.accessibility.utils.mlkit.MlKitHelper
import com.google.android.accessibility.utils.mlkit.MlKitHelperService
import com.google.android.accessibility.utils.mlkit.MlKitProtocol
import com.google.android.accessibility.utils.mlkit.MlKitProtocolTest
import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer.Script
import com.google.mlkit.common.MlKitException
import java.util.Locale
import java.util.concurrent.Executor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Cyrillic text recognition with Tesseract, which phone builds have. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CyrillicTextRecognizerTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val main = Looper.getMainLooper()
  private val name = ComponentName(context, MlKitHelperService::class.java)

  private val requests = mutableListOf<Message>()
  /** Answers each request; by default with some text. */
  private var answer: (Message) -> Message = { textReply() }

  private val service =
    Messenger(
      Handler(main) { request ->
        val copy = Message.obtain(request)
        requests += copy
        val reply = answer(copy)
        reply.arg1 = copy.arg1
        copy.replyTo.send(reply)
        true
      }
    )

  private val connector =
    object : MlKitHelper.Connector {
      override fun bind(connection: ServiceConnection, executor: Executor): Boolean {
        executor.execute { connection.onServiceConnected(name, service.binder) }
        return true
      }

      override fun unbind(connection: ServiceConnection) {}
    }

  private val helper = MlKitHelper(context, main, connector)
  private val installed = mutableSetOf<String>()
  private val missing = mutableListOf<Locale>()

  private fun recognizer(vararg tags: String) =
    LanguageTextRecognizer(
      context,
      { tags.map(Locale::forLanguageTag) },
      { 0L },
      helper,
      { it in installed },
    )

  private fun textReply(): Message =
    Message.obtain(null, MlKitProtocol.MSG_RESULT).apply {
      data.putByteArray(MlKitProtocol.KEY_TEXT_DATA, MlKitProtocol.encodeText(MlKitProtocolTest.sample()))
    }

  private fun bitmap() = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

  private fun idle() = shadowOf(main).idle()

  private fun asked() =
    requests.map {
      Triple(
        it.what,
        it.data.getString(MlKitProtocol.KEY_SCRIPT),
        it.data.getString(MlKitProtocol.KEY_TESSERACT_MODELS),
      )
    }

  @After
  fun tearDown() {
    LanguageTextRecognizer.setMissingModelListener(null)
  }

  @Test
  fun downloadedModel_readsCyrillicWithTesseractAndLatinLetters() {
    installed += setOf("rus", "eng")
    val recognizer = recognizer("ru-RU")

    val task = recognizer.process(bitmap(), 0)
    idle()

    assertEquals(Script.CYRILLIC, recognizer.script())
    assertEquals(listOf(Triple(MlKitProtocol.MSG_RECOGNIZE_TEXT, "CYRILLIC", "rus+eng")), asked())
    assertEquals(2, task.result.textBlocks.size)
  }

  @Test
  fun firstCyrillicLanguageInUseChoosesTheModel() {
    installed += setOf("ukr", "rus")
    val recognizer = recognizer("en-US", "uk-UA", "ru-RU")

    recognizer.process(bitmap(), 0)
    idle()

    assertEquals("ukr", asked().single().third)
  }

  @Test
  fun missingModel_readsLatinAndIsReportedOnce() {
    LanguageTextRecognizer.setMissingModelListener { missing += it }
    val recognizer = recognizer("bg-BG")

    val first = recognizer.process(bitmap(), 0)
    recognizer.process(bitmap(), 0)
    idle()

    assertEquals(Script.LATIN, recognizer.script())
    assertEquals(
      listOf(
        Triple(MlKitProtocol.MSG_RECOGNIZE_TEXT, "LATIN", null),
        Triple(MlKitProtocol.MSG_RECOGNIZE_TEXT, "LATIN", null),
      ),
      asked(),
    )
    assertEquals(listOf(Locale.forLanguageTag("bg-BG")), missing)
    assertTrue(first.isSuccessful)
  }

  @Test
  fun tesseractUnavailable_readsLatinWithoutAskingGooglePlayServices() {
    installed += "srp"
    answer = { request ->
      if (request.data.getString(MlKitProtocol.KEY_SCRIPT) == "CYRILLIC") {
        Message.obtain(null, MlKitProtocol.MSG_ERROR).apply {
          arg2 = MlKitException.UNAVAILABLE
          data.putString(MlKitProtocol.KEY_ERROR, "Not loaded")
        }
      } else {
        textReply()
      }
    }
    val recognizer = recognizer("sr-RS")

    val task = recognizer.process(bitmap(), 0)
    idle()

    assertEquals(
      listOf(
        Triple(MlKitProtocol.MSG_RECOGNIZE_TEXT, "CYRILLIC", "srp"),
        Triple(MlKitProtocol.MSG_RECOGNIZE_TEXT, "LATIN", null),
      ),
      asked(),
    )
    assertTrue(task.isSuccessful)
    assertEquals(Script.LATIN, recognizer.script())
  }

  @Test
  fun download_asksGooglePlayServicesForNothingForCyrillic() {
    installed += "rus"
    val recognizer = recognizer("ru")

    val task = recognizer.download()
    idle()

    assertTrue(task.isSuccessful)
    assertTrue(requests.isEmpty())
  }
}
