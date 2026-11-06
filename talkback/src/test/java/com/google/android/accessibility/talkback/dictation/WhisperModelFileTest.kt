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

package com.google.android.accessibility.talkback.dictation

import android.net.Uri
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowStatFs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhisperModelFileTest {
  private val context = RuntimeEnvironment.getApplication()
  private val models = WhisperModels.get(context)

  @Before
  fun setUp() {
    // The models are kept once per process, in the directory of the first test.
    val directory = models.customFile().parentFile!!.apply { mkdirs() }
    ShadowStatFs.registerStats(directory, 1_000_000, 1_000_000, 1_000_000)
  }

  private fun file(bytes: ByteArray): Uri =
    Uri.fromFile(File.createTempFile("model", ".bin", context.cacheDir).apply { writeBytes(bytes) })

  @Test
  fun aWhisperCppFileIsCopiedIn() {
    val bytes = byteArrayOf(0x6c, 0x6d, 0x67, 0x67) + ByteArray(1000) { it.toByte() }
    assertNull(models.copyCustom(context.contentResolver, file(bytes), bytes.size.toLong()))
    assertTrue(models.hasCustom())
    assertEquals(bytes.toList(), models.customFile().readBytes().toList())
  }

  @Test
  fun anotherFileIsRefusedAndNothingIsLeft() {
    models.customFile().delete()
    val failure = models.copyCustom(context.contentResolver, file("not a model".toByteArray()), 11)
    assertEquals(WhisperModels.ImportFailure.NOT_A_MODEL, failure)
    assertFalse(models.hasCustom())
  }
}
