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

package com.google.android.accessibility.talkback.updatetasks

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CacheCleanupTest {
  private lateinit var dir: File

  @Before
  fun setUp() {
    dir = Files.createTempDirectory("cache").toFile()
  }

  @After
  fun tearDown() {
    dir.deleteRecursively()
  }

  @Test
  fun deletesModelCachesAndSpeechEnginePrimeFiles() {
    File(dir, "gemma-4-E2B-it.litertlm.xnnpack_cache").writeBytes(ByteArray(100))
    File(dir, "tmpsynthesize123.tmp").writeBytes(ByteArray(3))
    File(dir, "tmpsynthesize456.tmp").writeBytes(ByteArray(4))
    File(dir, "update").mkdirs()
    File(dir, "sound_theme.zip").writeBytes(ByteArray(1))

    assertEquals(107L, CacheCleanup.clean(dir))

    assertEquals(setOf("update", "sound_theme.zip"), dir.list()!!.toSet())
  }
}
