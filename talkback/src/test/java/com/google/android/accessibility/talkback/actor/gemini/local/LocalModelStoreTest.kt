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
import java.io.RandomAccessFile
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalModelStoreTest {
  private lateinit var dir: File
  private lateinit var store: LocalModelStore

  @Before
  fun setUp() {
    dir = Files.createTempDirectory("store").toFile()
    store = LocalModelStore(dir)
  }

  @After
  fun tearDown() {
    dir.deleteRecursively()
  }

  @Test
  fun everyModelIsPinnedToACommit() {
    val commit = Regex("[0-9a-f]{40}")
    LocalModel.entries.forEach {
      assertTrue(it.id, commit.matches(it.revision))
      assertTrue(it.id, it.url.contains("/resolve/${it.revision}/"))
    }
  }

  @Test
  fun aFileOfThePreviousSizeStaysInstalled() {
    val model = LocalModel.entries.first { it.previousSizeBytes != null }
    RandomAccessFile(store.file(model), "rw").use { it.setLength(model.previousSizeBytes!!) }
    assertTrue(store.isInstalled(model))
    RandomAccessFile(store.file(model), "rw").use { it.setLength(model.previousSizeBytes!! - 1) }
    assertFalse(store.isInstalled(model))
  }

  @Test
  fun nothingIsInstalledAtFirst() {
    assertFalse(store.isInstalled(LocalModel.E2B))
    assertNull(store.installed(LocalModel.E2B))
  }

  @Test
  fun aFileOfTheWrongSizeIsNotInstalled() {
    File(dir, LocalModel.E2B.fileName).writeBytes(ByteArray(10))
    assertFalse(store.isInstalled(LocalModel.E2B))
  }

  @Test
  fun importRejectsABadFileAndLeavesNothing() {
    val failure = store.import(LocalModel.E2B, ByteArray(10).inputStream())
    assertEquals(ModelFiles.Failure.SIZE_MISMATCH, failure)
    assertEquals(0, dir.listFiles()!!.size)
  }

  @Test
  fun importUnknownReturnsNullForAFileThatIsNoKnownModel() {
    assertNull(store.importUnknown { ByteArray(10).inputStream() })
  }

  @Test
  fun installedFallsBackToTheOtherModel() {
    RandomAccessFile(File(dir, LocalModel.E4B.fileName), "rw").use {
      it.setLength(LocalModel.E4B.sizeBytes)
    }
    assertTrue(store.isInstalled(LocalModel.E4B))
    assertEquals(LocalModel.E4B, store.installed(LocalModel.E2B))
  }

  @Test
  fun deleteRemovesTheFileAndItsPart() {
    val file = File(dir, LocalModel.E2B.fileName)
    file.writeBytes(byteArrayOf(1))
    ModelFiles.partFile(file).writeBytes(byteArrayOf(1))
    store.delete(LocalModel.E2B)
    assertEquals(0, dir.listFiles()!!.size)
  }

  @Test
  fun catalogueHashesAreSha256Hex() {
    LocalModel.entries.forEach { assertTrue(it.sha256.matches(Regex("[0-9a-f]{64}"))) }
    assertEquals(LocalModel.E4B, LocalModel.fromId("e4b"))
    assertNull(LocalModel.fromId("nope"))
  }

  @Test
  fun everyModelHasItsOwnIdAndFileOnThePhone() {
    val models = LocalModel.entries
    assertEquals(models.size, models.map { it.id }.toSet().size)
    // Several repos name their file "model.litertlm", so the phone must not use that name.
    assertEquals(models.size, models.map { it.localFileName }.toSet().size)
    models.forEach {
      assertTrue(it.localFileName.endsWith(".litertlm"))
      assertTrue(it.url.startsWith("https://huggingface.co/litert-community/"))
      assertTrue(it.sizeBytes > 100_000_000L)
      assertTrue(it.minTotalRamGib in 3..16)
    }
  }

  @Test
  fun theModelsDownloadedBeforeTheCatalogGrewKeepTheirFileNames() {
    assertEquals("gemma-4-E2B-it.litertlm", LocalModel.E2B.localFileName)
    assertEquals("gemma-4-E4B-it.litertlm", LocalModel.E4B.localFileName)
  }

  @Test
  fun onlyTheGemma4ModelsThatWereTriedAreNotMarkedExperimental() {
    assertEquals(listOf("e2b", "e4b"), LocalModel.entries.filter { !it.experimental }.map { it.id })
  }

  @Test
  fun managingListsCompleteModelsAndUnfinishedDownloadsSeparately() {
    val small = LocalModel.fromId("smolvlm2-500m")!!
    val other = LocalModel.fromId("internvl3-1b")!!
    RandomAccessFile(store.file(small), "rw").use { it.setLength(small.sizeBytes) }
    ModelFiles.partFile(store.file(other)).writeBytes(ByteArray(1234))

    assertEquals(listOf(small), store.installedModels())
    assertEquals(listOf(other to 1234L), store.unfinishedDownloads())
    assertEquals(small.sizeBytes + 1234L, store.usedBytes())
  }

  @Test
  fun aModelWithAPartAndAFullFileIsNotListedAsUnfinished() {
    val small = LocalModel.fromId("smolvlm2-500m")!!
    RandomAccessFile(store.file(small), "rw").use { it.setLength(small.sizeBytes) }
    ModelFiles.partFile(store.file(small)).writeBytes(ByteArray(10))
    assertTrue(store.unfinishedDownloads().isEmpty())
  }

  @Test
  fun deletingTheOnlyModelFreesAllTheStorage() {
    val small = LocalModel.fromId("smolvlm2-500m")!!
    RandomAccessFile(store.file(small), "rw").use { it.setLength(small.sizeBytes) }
    store.delete(small)
    assertTrue(store.installedModels().isEmpty())
    assertEquals(0L, store.usedBytes())
  }
}
