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
package com.google.android.accessibility.talkback.actor.gemini.progress

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressToneProvider.InfiniteIterator
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressToneProvider.Tone
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressTonePlayerTest {
  private val played = mutableListOf<String>()
  private var timeouts = 0

  /** Plays "load" every 2 seconds while the model loads, and "work" every 3 seconds otherwise. */
  private val provider =
    object : ProgressToneProvider {
      override fun getNextToneGroup(): InfiniteIterator<Tone> = tones("work", 3000)

      override fun getNextToneGroup(modelLoading: Boolean): InfiniteIterator<Tone> =
        if (modelLoading) tones("load", 2000) else getNextToneGroup()
    }

  private fun tones(name: String, delayMs: Int) = InfiniteIterator {
    Tone(Feedback.speech(name), delayMs)
  }

  private val player =
    ProgressTonePlayer(
      provider,
      { tone -> played.add(tone.tone.build().speech()!!.text().toString()) },
      { 60_000L },
      { timeouts++ },
      Handler(Looper.getMainLooper()),
      { SystemClock.elapsedRealtime() },
    )

  private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

  @Test
  fun modelLoadingTonesPlayUntilTheModelIsLoaded() {
    player.play(true, false, true)
    advance(4100)
    assertEquals(listOf("load", "load", "load"), played)
    assertTrue(player.isModelLoading)

    player.setModelLoading(false)
    assertEquals("work", played.last())
    assertFalse(player.isModelLoading)
    played.clear()
    advance(6100)
    assertEquals(listOf("work", "work"), played)
  }

  @Test
  fun switchingToTheSameTonesPlaysNothingNew() {
    player.play(true, false, false)
    player.setModelLoading(false)
    assertEquals(listOf("work"), played)
  }

  @Test
  fun processingCanGoBackToLoading() {
    player.play(true, false)
    player.setModelLoading(true)
    assertEquals(listOf("work", "load"), played)
  }

  @Test
  fun switchingWhenNothingPlaysDoesNothing() {
    player.setModelLoading(true)
    advance(5000)
    assertTrue(played.isEmpty())

    player.play(true, false, true)
    player.stop()
    player.setModelLoading(false)
    advance(5000)
    assertEquals(listOf("load"), played)
  }

  @Test
  fun aSwitchDuringTheDelayChangesTheFirstTone() {
    player.play(true, true, true)
    player.setModelLoading(false)
    assertTrue(played.isEmpty())
    advance(2600)
    assertEquals(listOf("work"), played)
  }

  @Test
  fun stopEndsTheTonesAfterASwitch() {
    player.play(true, false, true)
    player.setModelLoading(false)
    player.stop()
    advance(10_000)
    assertEquals(listOf("load", "work"), played)
    assertEquals(0, timeouts)
  }

  @Test
  fun theTimeoutStaysAfterASwitch() {
    val short =
      ProgressTonePlayer(
        provider,
        { tone -> played.add(tone.tone.build().speech()!!.text().toString()) },
        { 5_000L },
        { timeouts++ },
        Handler(Looper.getMainLooper()),
        { SystemClock.elapsedRealtime() },
      )
    short.play(true, false, true)
    advance(3000)
    short.setModelLoading(false)
    advance(10_000)
    assertEquals(1, timeouts)
    assertEquals(listOf("load", "load", "work"), played)
  }

  @Test
  fun theDefaultTonesDifferWhileTheModelLoads() {
    val tones = DefaultProgressToneProvider()
    val loading = tones.getNextToneGroup(true).next()
    val working = tones.getNextToneGroup(false).next()
    assertEquals(R.raw.model_loading, loading.tone.build().sound()!!.resourceId())
    assertEquals(R.raw.loading, working.tone.build().sound()!!.resourceId())
    assertEquals(2000, loading.delayMs)
    assertEquals(3000, working.delayMs)
  }
}
