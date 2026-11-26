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

package com.google.android.accessibility.talkback.recognition

import com.google.android.accessibility.talkback.recognition.ColorClassifier.BLACK
import com.google.android.accessibility.talkback.recognition.ColorClassifier.BLUE
import com.google.android.accessibility.talkback.recognition.ColorClassifier.BROWN
import com.google.android.accessibility.talkback.recognition.ColorClassifier.DARK
import com.google.android.accessibility.talkback.recognition.ColorClassifier.GRAY
import com.google.android.accessibility.talkback.recognition.ColorClassifier.GREEN
import com.google.android.accessibility.talkback.recognition.ColorClassifier.LIGHT
import com.google.android.accessibility.talkback.recognition.ColorClassifier.ORANGE
import com.google.android.accessibility.talkback.recognition.ColorClassifier.PINK
import com.google.android.accessibility.talkback.recognition.ColorClassifier.PLAIN
import com.google.android.accessibility.talkback.recognition.ColorClassifier.PURPLE
import com.google.android.accessibility.talkback.recognition.ColorClassifier.RED
import com.google.android.accessibility.talkback.recognition.ColorClassifier.TURQUOISE
import com.google.android.accessibility.talkback.recognition.ColorClassifier.WHITE
import com.google.android.accessibility.talkback.recognition.ColorClassifier.YELLOW
import com.google.android.accessibility.talkback.recognition.ColorClassifier.color
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveColorsTest {
  private fun classify(r: Int, g: Int, b: Int) = ColorClassifier.classify(r, g, b)

  @Test fun grays() {
    assertEquals(color(BLACK, PLAIN), classify(10, 10, 12))
    assertEquals(color(GRAY, DARK), classify(70, 70, 70))
    assertEquals(color(GRAY, PLAIN), classify(140, 138, 142))
    assertEquals(color(GRAY, LIGHT), classify(200, 200, 205))
    assertEquals(color(WHITE, PLAIN), classify(245, 245, 240))
  }

  @Test fun hues() {
    assertEquals(color(RED, PLAIN), classify(220, 30, 30))
    assertEquals(color(ORANGE, PLAIN), classify(250, 140, 20))
    assertEquals(color(YELLOW, PLAIN), classify(240, 220, 30))
    assertEquals(color(GREEN, PLAIN), classify(40, 200, 60))
    assertEquals(color(TURQUOISE, PLAIN), classify(30, 200, 200))
    assertEquals(color(BLUE, PLAIN), classify(30, 80, 220))
    assertEquals(color(PURPLE, PLAIN), classify(130, 40, 200))
    assertEquals(color(PINK, PLAIN), classify(230, 40, 160))
  }

  @Test fun shades() {
    assertEquals(color(BLUE, DARK), classify(10, 20, 80))
    assertEquals(color(BLUE, LIGHT), classify(150, 190, 240))
    assertEquals(color(GREEN, DARK), classify(20, 80, 30))
    // Light red is pink, and darker orange is brown.
    assertEquals(color(PINK, PLAIN), classify(250, 170, 170))
    assertEquals(color(BROWN, PLAIN), classify(140, 80, 30))
    assertEquals(color(BROWN, DARK), classify(60, 35, 15))
  }

  private fun counts(vararg pairs: Pair<Int, Int>) =
    IntArray(ColorClassifier.COLORS).also { for ((color, count) in pairs) it[color] = count }

  private fun summarize(counts: IntArray) =
    ColorClassifier.summarize(counts, IntArray(ColorClassifier.FAMILIES))

  @Test fun oneColorCoveringMostOfTheCentreIsNamedAlone() {
    val result = summarize(counts(color(BLUE, DARK) to 300, color(BLUE, PLAIN) to 100, color(WHITE, PLAIN) to 50))
    assertEquals(color(BLUE, DARK), ColorClassifier.first(result))
    assertEquals(-1, ColorClassifier.second(result))
  }

  @Test fun twoColorsAreNamedMostFirst() {
    val result = summarize(counts(color(WHITE, PLAIN) to 200, color(RED, PLAIN) to 300, color(GREEN, PLAIN) to 20))
    assertEquals(color(RED, PLAIN), ColorClassifier.first(result))
    assertEquals(color(WHITE, PLAIN), ColorClassifier.second(result))
  }

  @Test fun noColorCoveringEnoughIsMixed() {
    val result =
      summarize(
        counts(
          color(RED, PLAIN) to 100,
          color(BLUE, PLAIN) to 100,
          color(GREEN, PLAIN) to 100,
          color(YELLOW, PLAIN) to 100,
        )
      )
    assertEquals(ColorClassifier.MIXED, result)
    assertEquals(-1, ColorClassifier.first(result))
  }

  @Test fun tooFewPixelsIsNone() {
    assertEquals(ColorClassifier.NONE, summarize(counts(color(RED, PLAIN) to 5)))
  }

  @Test fun theBlackFamilyCanBeNamed() {
    // Black is family 0, which the search for the top family starts from.
    val result = summarize(counts(color(BLACK, PLAIN) to 400, color(RED, PLAIN) to 10))
    assertEquals(color(BLACK, PLAIN), ColorClassifier.first(result))
    assertEquals(-1, ColorClassifier.second(result))
  }

  /** A frame of one color, with interleaved chroma (pixel stride 2) as most cameras give. */
  private fun measure(luma: Int, cb: Int, cr: Int, width: Int = 64, height: Int = 48): Int {
    val y = ByteBuffer.wrap(ByteArray(width * height) { luma.toByte() })
    val chroma = ByteArray(width * height / 2)
    for (i in chroma.indices) chroma[i] = (if (i % 2 == 0) cb else cr).toByte()
    val u = ByteBuffer.wrap(chroma)
    // V starts one byte after U in the same memory, and is one byte shorter.
    val v = ByteBuffer.wrap(chroma, 1, chroma.size - 1).slice()
    return ColorSampler().measure(y, width, 1, u, v, width, 2, width, height)
  }

  @Test fun samplerReadsTheColorOfTheFrame() {
    // Full-range BT.601 for RGB (30, 80, 220).
    val luma = (0.299 * 30 + 0.587 * 80 + 0.114 * 220).toInt()
    val cb = (128 - 0.168736 * 30 - 0.331264 * 80 + 0.5 * 220).toInt()
    val cr = (128 + 0.5 * 30 - 0.418688 * 80 - 0.081312 * 220).toInt()
    val result = measure(luma, cb, cr)
    assertEquals(color(BLUE, PLAIN), ColorClassifier.first(result))
    assertEquals(color(GRAY, PLAIN), ColorClassifier.first(measure(128, 128, 128)))
  }

  @Test fun samplerSkipsTinyFrames() {
    assertEquals(ColorClassifier.NONE, measure(128, 128, 128, width = 8, height = 8))
  }

  @Test fun announcerSpeaksStableChangedResultsOnly() {
    val announcer = ColorAnnouncer(stableCount = 3, minIntervalMs = 1_500)
    val blue = ColorClassifier.result(color(BLUE, PLAIN), -1)
    val red = ColorClassifier.result(color(RED, PLAIN), -1)
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(blue, 0))
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(blue, 200))
    assertEquals(blue, announcer.update(blue, 400))
    // The same result again is not spoken.
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(blue, 600))
    // A flicker is not spoken.
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(red, 800))
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(blue, 1_000))
    // A change waits for the interval.
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(red, 1_200))
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(red, 1_400))
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(red, 1_600))
    assertEquals(red, announcer.update(red, 1_900))
  }

  @Test fun announcerNeverSpeaksNone() {
    val announcer = ColorAnnouncer(stableCount = 1, minIntervalMs = 0)
    assertEquals(ColorAnnouncer.NOTHING, announcer.update(ColorClassifier.NONE, 0))
    assertEquals(ColorClassifier.MIXED, announcer.update(ColorClassifier.MIXED, 10))
  }
}
