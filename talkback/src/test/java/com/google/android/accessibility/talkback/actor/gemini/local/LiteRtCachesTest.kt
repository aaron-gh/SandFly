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

package com.google.android.accessibility.talkback.actor.gemini.local

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiteRtCachesTest {
  private lateinit var dir: File

  @Before
  fun setUp() {
    dir = Files.createTempDirectory("cache").toFile()
  }

  @After
  fun tearDown() {
    dir.deleteRecursively()
  }

  private fun file(name: String, bytes: Int = 10) = File(dir, name).apply { writeBytes(ByteArray(bytes)) }

  @Test
  fun cachesAreMatchedToTheirModelWithOrWithoutTheExtension() {
    assertTrue(LiteRtCaches.belongsTo("gemma-4-E4B-it.litertlm.xnnpack_cache", LocalModel.E4B))
    assertTrue(LiteRtCaches.belongsTo("gemma-4-E4B-it.xnnpack_cache", LocalModel.E4B))
    assertTrue(LiteRtCaches.belongsTo("gemma-4-E4B-it_mldrift_weight_cache.bin", LocalModel.E4B))
    assertFalse(LiteRtCaches.belongsTo("gemma-4-E4B-it.xnnpack_cache", LocalModel.E2B))
  }

  @Test
  fun noModelOwnsAnotherModelsCaches() {
    for (model in LocalModel.entries) {
      for (other in LocalModel.entries - model) {
        assertFalse(
          "${model.id} owns caches of ${other.id}",
          LiteRtCaches.belongsTo(other.localFileName + ".xnnpack_cache", model),
        )
      }
    }
  }

  @Test
  fun deleteUnusedKeepsCachesOfInstalledModelsAndOtherFiles() {
    val kept = file("gemma-4-E2B-it.litertlm.xnnpack_cache")
    val stale = file("gemma-4-E4B-it.litertlm.xnnpack_cache", 100)
    val staleGpu = file("gemma-4-E4B-it.litertlm_mldrift_program_cache.bin", 5)
    val other = file("update")

    assertEquals(105L, LiteRtCaches.deleteUnused(dir, listOf(LocalModel.E2B)))

    assertTrue(kept.exists())
    assertFalse(stale.exists())
    assertFalse(staleGpu.exists())
    assertTrue(other.exists())
  }

  @Test
  fun deleteAllLeavesFilesThatAreNotCaches() {
    file("gemma-4-E2B-it.litertlm.xnnpack_cache")
    file("e2b_mldrift_weight_cache.bin")
    val other = file("on-device-ai-1.jpg")

    assertEquals(20L, LiteRtCaches.deleteAll(dir))

    assertEquals(listOf(other), dir.listFiles()!!.toList())
  }
}
