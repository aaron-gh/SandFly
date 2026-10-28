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

package com.google.android.accessibility.talkback.translation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllLanguageDownloadsTest {
  /** Downloads that wait for [finish], in the order they were asked for. */
  private class FakeModels : OnDeviceTranslation {
    val asked = mutableListOf<String>()
    private val waiting = mutableListOf<(Boolean) -> Unit>()
    var wifiOnly: Boolean? = null

    override val languages = setOf("de", "fr", "es")

    override fun detect(text: String, done: (language: String?, unavailable: Boolean) -> Unit) = done(null, false)

    override fun downloaded(done: (Set<String>) -> Unit) = done(emptySet())

    override fun download(language: String, wifiOnly: Boolean, done: (Boolean) -> Unit) {
      asked += language
      this.wifiOnly = wifiOnly
      waiting += done
    }

    fun finish(ok: Boolean) = waiting.removeAt(0).invoke(ok)

    override fun isDownloading(language: String) = false

    override fun delete(language: String, done: (Boolean) -> Unit) = done(true)

    override fun translate(text: String, from: String, to: String, done: (String?) -> Unit) = done(null)

    override fun close() {}
  }

  private val models = FakeModels()
  private val finished = mutableListOf<AllLanguageDownloads.Progress>()

  @After
  fun tearDown() = AllLanguageDownloads.stop()

  @Test
  fun languages_downloadOneAtATime() {
    AllLanguageDownloads.start(models, listOf("de", "fr", "es"), wifiOnly = true) { finished += it }
    assertEquals(listOf("de"), models.asked)
    assertEquals(true, models.wifiOnly)
    models.finish(ok = true)
    assertEquals(listOf("de", "fr"), models.asked)
    assertEquals(AllLanguageDownloads.Progress(3, 1, 0), AllLanguageDownloads.progress)
    models.finish(ok = false)
    models.finish(ok = true)
    assertEquals(listOf(AllLanguageDownloads.Progress(3, 3, 1)), finished)
    assertFalse(AllLanguageDownloads.isRunning)
  }

  @Test
  fun stop_startsNoMoreDownloads_andNothingFinishes() {
    AllLanguageDownloads.start(models, listOf("de", "fr"), wifiOnly = false) { finished += it }
    AllLanguageDownloads.stop()
    assertNull(AllLanguageDownloads.progress)
    models.finish(ok = true)
    assertEquals(listOf("de"), models.asked)
    assertTrue(finished.isEmpty())
  }

  @Test
  fun aStoppedRunsDownload_doesNotCountInTheNext() {
    AllLanguageDownloads.start(models, listOf("de"), wifiOnly = false) { finished += it }
    AllLanguageDownloads.stop()
    AllLanguageDownloads.start(models, listOf("fr", "es"), wifiOnly = false) { finished += it }
    // The first run's download ends.
    models.finish(ok = true)
    assertEquals(AllLanguageDownloads.Progress(2, 0, 0), AllLanguageDownloads.progress)
  }
}
