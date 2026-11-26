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

import android.graphics.Rect
import com.google.android.accessibility.utils.mlkit.MlKitProtocol
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TesseractTextTest {

  private fun word(
    text: String,
    left: Int,
    top: Int,
    confidence: Float = 90f,
    block: Boolean = false,
    line: Boolean = false,
  ) = TesseractText.Word(text, Rect(left, top, left + 40, top + 20), confidence, block, line)

  @Test
  fun wordsAreGroupedIntoBlocksAndLinesAsMlKitGivesThem() {
    val text = TesseractText("ru")
    text.add(word("Привет", 10, 10, block = true, line = true))
    text.add(word("мир", 60, 10))
    text.add(word("Второй", 10, 40, line = true))
    text.add(word("Ниже", 10, 200, block = true, line = true))

    val result = text.build()

    assertEquals("Привет мир\nВторой\nНиже", result.text)
    assertEquals(2, result.textBlocks.size)
    val first = result.textBlocks[0]
    assertEquals("Привет мир\nВторой", first.text)
    assertEquals(Rect(10, 10, 100, 60), first.boundingBox)
    assertEquals(listOf("Привет мир", "Второй"), first.lines.map { it.text })
    assertEquals(Rect(10, 10, 100, 30), first.lines[0].boundingBox)
    assertEquals("ru", first.lines[0].elements[0].recognizedLanguage)
    assertEquals(0.9f, first.lines[0].elements[0].confidence, 0.001f)
    assertEquals(4, first.cornerPoints!!.size)
  }

  @Test
  fun unsureWordsAndMarksAreLeftOut() {
    val text = TesseractText("ru")
    text.add(word("|", 0, 0, block = true, line = true))
    text.add(word("шум", 50, 0, confidence = 10f))
    text.add(word("Текст", 100, 0))
    text.add(word("—", 0, 100, block = true, line = true))

    val result = text.build()

    assertEquals("Текст", result.text)
    assertEquals(1, result.textBlocks.size)
  }

  @Test
  fun boxesOfAReducedImageAreScaledBack() {
    val text = TesseractText("uk", scale = 2f)
    text.add(word("Слово", 10, 10, block = true, line = true))

    val element = text.build().textBlocks.single().lines.single().elements.single()

    assertEquals(Rect(20, 20, 100, 60), element.boundingBox)
  }

  @Test
  fun noWordsGiveNoText() {
    val result = TesseractText("ru").build()

    assertEquals("", result.text)
    assertEquals(0, result.textBlocks.size)
  }

  @Test
  fun resultIsSentToSandFlyLikeMlKitText() {
    val text = TesseractText("bg")
    text.add(word("Здравей", 0, 0, block = true, line = true))

    val decoded = MlKitProtocol.decodeText(MlKitProtocol.encodeText(text.build()))

    assertEquals("Здравей", decoded.textBlocks.single().lines.single().elements.single().text)
    assertEquals("bg", decoded.textBlocks.single().recognizedLanguage)
  }
}
