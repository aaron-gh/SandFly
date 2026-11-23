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

package com.google.android.accessibility.talkback.actor

import java.io.File
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImageSharerTest {
  @get:Rule val folder = TemporaryFolder()

  private val utc = TimeZone.getTimeZone("UTC")

  @Test
  fun fileName_namesTheTimeToTheMillisecond() {
    // 2026-11-23 14:05:09.042 UTC.
    assertEquals(
      "SandFly-20261123-140509-042.png",
      ImageSharer.fileName(1795442709042L, utc),
    )
  }

  @Test
  fun fileName_sortsByTime() {
    val earlier = ImageSharer.fileName(1795442709042L, utc)
    val later = ImageSharer.fileName(1795442709042L + 1, utc)
    val nextDay = ImageSharer.fileName(1795442709042L + 86_400_000L, utc)
    assertTrue(earlier < later)
    assertTrue(later < nextDay)
  }

  @Test
  fun oldFiles_keepsTheLatestFiles() {
    val names = (0 until 6).map { ImageSharer.fileName(1795442709042L + it * 1000L, utc) }
    // Made out of order, so that the order comes from the names.
    val files = names.reversed().map { folder.newFile(it) }
    val old = ImageSharer.oldFiles(folder.root.listFiles()).map { it.name }.sorted()
    assertEquals(names.take(names.size - ImageSharer.KEPT_FILES), old)
    assertEquals(6, files.size)
  }

  @Test
  fun oldFiles_withFewFiles_removesNone() {
    folder.newFile(ImageSharer.fileName(1795442709042L, utc))
    folder.newFile(ImageSharer.fileName(1795442710042L, utc))
    assertEquals(emptyList<File>(), ImageSharer.oldFiles(folder.root.listFiles()))
  }

  @Test
  fun oldFiles_leavesOtherFilesAndDirectories() {
    for (i in 0 until 5) {
      folder.newFile(ImageSharer.fileName(1795442709042L + i * 1000L, utc))
    }
    folder.newFile("other.png")
    folder.newFile("SandFly-notes.txt")
    folder.newFolder("SandFly-20000101-000000-000.png")
    val old = ImageSharer.oldFiles(folder.root.listFiles()).map { it.name }
    assertEquals(2, old.size)
    assertTrue(old.all { it.startsWith("SandFly-2026") && it.endsWith(".png") })
  }

  @Test
  fun oldFiles_withNoListing_removesNone() {
    assertEquals(emptyList<File>(), ImageSharer.oldFiles(null))
  }
}
