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

import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.vision.text.Text
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MlKitProtocolTest {

  @Test
  fun text_roundTrip_keepsBlocksLinesAndElements() {
    val decoded = MlKitProtocol.decodeText(MlKitProtocol.encodeText(sample()))

    assertEquals("Hello world\nÜber 你好", decoded.text)
    assertEquals(2, decoded.textBlocks.size)
    val block = decoded.textBlocks[0]
    assertEquals("Hello world", block.text)
    assertEquals(Rect(10, 20, 200, 40), block.boundingBox)
    assertArrayEquals(corners(10, 20, 200, 40).toTypedArray(), block.cornerPoints)
    assertEquals("en", block.recognizedLanguage)
    val line = block.lines.single()
    assertEquals(0.9f, line.confidence)
    assertEquals(1.5f, line.angle)
    assertEquals(listOf("Hello", "world"), line.elements.map { it.text })
    val world = line.elements[1]
    assertEquals(Rect(110, 20, 200, 40), world.boundingBox)
    assertEquals(0.8f, world.confidence)
    assertEquals(-2f, world.angle)
    assertEquals("Über 你好", decoded.textBlocks[1].lines.single().elements.joinToString(" ") { it.text })
    assertEquals("", decoded.textBlocks[1].recognizedLanguage)
  }

  @Test
  fun text_withNothingRecognized_roundTrips() {
    val decoded = MlKitProtocol.decodeText(MlKitProtocol.encodeText(Text("", emptyList<Text.TextBlock>())))

    assertEquals("", decoded.text)
    assertTrue(decoded.textBlocks.isEmpty())
  }

  @Test
  fun text_longerThanWriteUtfTakes_roundTrips() {
    val long = "x".repeat(70_000)
    val text = Text(long, listOf(block(long, Rect(0, 0, 1, 1), "en", listOf<Text.Element>())))

    assertEquals(long, MlKitProtocol.decodeText(MlKitProtocol.encodeText(text)).textBlocks[0].text)
  }

  @Test
  fun damagedText_throwsIOException() {
    val bytes = MlKitProtocol.encodeText(sample())

    assertThrows(IOException::class.java) { MlKitProtocol.decodeText(bytes.copyOf(bytes.size / 2)) }
    assertThrows(IOException::class.java) {
      // A string length far beyond the data.
      MlKitProtocol.decodeText(byteArrayOf(0x7f, 0, 0, 0))
    }
    assertThrows(IOException::class.java) {
      // An empty text with a negative block count.
      MlKitProtocol.decodeText(byteArrayOf(0, 0, 0, 0, -1, -1, -1, -1))
    }
  }

  @Test
  fun languages_roundTrip() {
    val data = Bundle()
    val languages = listOf(IdentifiedLanguage("de", 0.7f), IdentifiedLanguage("nl", 0.2f))

    MlKitProtocol.putLanguages(data, languages)

    assertEquals(languages, MlKitProtocol.languages(data))
  }

  @Test
  fun languages_missingOrUneven_giveWhatMatches() {
    assertTrue(MlKitProtocol.languages(Bundle()).isEmpty())
    val uneven =
      Bundle().apply {
        putStringArray(MlKitProtocol.KEY_LANGUAGES, arrayOf("de", "nl"))
        putFloatArray(MlKitProtocol.KEY_CONFIDENCES, floatArrayOf(0.5f))
      }
    assertEquals(listOf(IdentifiedLanguage("de", 0.5f)), MlKitProtocol.languages(uneven))
  }

  companion object {
    fun corners(left: Int, top: Int, right: Int, bottom: Int) =
      listOf(Point(left, top), Point(right, top), Point(right, bottom), Point(left, bottom))

    fun element(text: String, box: Rect, confidence: Float, angle: Float) =
      Text.Element(
        text,
        box,
        corners(box.left, box.top, box.right, box.bottom),
        "en",
        null,
        confidence,
        angle,
        emptyList<Text.Symbol>(),
      )

    fun block(text: String, box: Rect, language: String, elements: List<Text.Element>) =
      Text.TextBlock(
        text,
        box,
        corners(box.left, box.top, box.right, box.bottom),
        language,
        null,
        listOf(
          Text.Line(
            text,
            box,
            corners(box.left, box.top, box.right, box.bottom),
            language,
            null,
            elements,
            0.9f,
            1.5f,
          )
        ),
      )

    fun sample() =
      Text(
        "Hello world\nÜber 你好",
        listOf(
          block(
            "Hello world",
            Rect(10, 20, 200, 40),
            "en",
            listOf(
              element("Hello", Rect(10, 20, 100, 40), 0.95f, 1f),
              element("world", Rect(110, 20, 200, 40), 0.8f, -2f),
            ),
          ),
          block(
            "Über 你好",
            Rect(10, 50, 200, 70),
            "",
            listOf(
              element("Über", Rect(10, 50, 90, 70), 0.7f, 0f),
              element("你好", Rect(100, 50, 200, 70), 0.6f, 0f),
            ),
          ),
        ),
      )
  }
}
