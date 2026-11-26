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

package com.google.android.accessibility.talkback.textrecognition

import android.content.Context
import com.google.android.accessibility.talkback.textrecognition.CyrillicTextModels.Model
import com.google.android.accessibility.utils.ocr.CyrillicOcr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CyrillicTextModelsTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun eachListedLanguageUsesItsOwnModel() {
    for (model in Model.entries) {
      assertEquals(model.name, model.file.code, CyrillicOcr.modelFor(model.locale))
      assertTrue(model.name, CyrillicOcr.isValidLanguages(model.file.code + "+" + CyrillicOcr.LATIN_MODEL))
    }
  }

  @Test
  fun everyModelHasASizeAndHash() {
    for (file in Model.entries.map { it.file } + CyrillicTextModels.LATIN) {
      assertTrue(file.code, file.sizeBytes > 0)
      assertTrue(file.code, Regex("[0-9a-f]{64}").matches(file.sha256))
      assertTrue(file.url, file.url.startsWith("https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/"))
      assertTrue(file.url, file.url.endsWith("/${file.code}.traineddata"))
    }
  }

  @Test
  fun firstDownloadAlsoFetchesLatinLetters() {
    CyrillicOcr.tessdataDir(context).deleteRecursively()
    val models = CyrillicTextModels(context)

    assertEquals(listOf("rus", "eng"), models.filesFor(Model.RUSSIAN).map { it.code })
    assertEquals(
      Model.RUSSIAN.file.sizeBytes + CyrillicTextModels.LATIN.sizeBytes,
      models.downloadBytes(Model.RUSSIAN),
    )

    CyrillicOcr.tessdataDir(context).mkdirs()
    CyrillicOcr.modelFile(context, "eng").writeText("x")
    assertEquals(listOf("ukr"), models.filesFor(Model.UKRAINIAN).map { it.code })
  }

  @Test
  fun lastModelIsDeletedWithLatinLetters() {
    CyrillicOcr.tessdataDir(context).deleteRecursively()
    CyrillicOcr.tessdataDir(context).mkdirs()
    CyrillicOcr.modelFile(context, "eng").writeText("x")
    CyrillicOcr.modelFile(context, "rus").writeText("x")
    val models = CyrillicTextModels(context)

    assertEquals(
      Model.RUSSIAN.file.sizeBytes + CyrillicTextModels.LATIN.sizeBytes,
      models.deleteBytes(Model.RUSSIAN),
    )

    CyrillicOcr.modelFile(context, "bul").writeText("x")
    assertEquals(Model.RUSSIAN.file.sizeBytes, models.deleteBytes(Model.RUSSIAN))
  }
}
