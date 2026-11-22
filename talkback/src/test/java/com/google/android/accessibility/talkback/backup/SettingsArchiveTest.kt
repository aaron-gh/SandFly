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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsArchiveTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private lateinit var root: File
  private lateinit var directory: File

  @Before
  fun setUp() {
    root = File(context.cacheDir, "archive-test").apply { deleteRecursively(); mkdirs() }
    directory = File(root, "read")
  }

  @After
  fun tearDown() {
    root.deleteRecursively()
  }

  @Test
  fun everyPartComesBack() {
    val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 1, 2, 3)
    val wallpaper = File(root, "wallpaper.png").apply { writeBytes(png) }
    val theme = byteArrayOf(9, 8, 7)
    val contents =
      SettingsArchive.Contents(
        settings = "{\"format\":\"sandfly-settings\"}",
        labels = "{\"labels_array\":[]}",
        profiles = "{\"format\":\"sandfly-items\"}",
        wallpaper = wallpaper,
        scripts = mapOf("my-script" to mapOf("main.lua" to "-- main".toByteArray(), "lib/util.lua" to byteArrayOf(1))),
        themes = mapOf("rain" to { out -> out.write(theme) }, "backtalk" to { out -> out.write(byteArrayOf(5)) }),
      )
    val bytes = ByteArrayOutputStream().also { SettingsArchive.write(it, contents) }.toByteArray()
    assertTrue(SettingsArchive.isZip(bytes))

    val read = SettingsArchive.read(ByteArrayInputStream(bytes), directory)

    assertEquals(contents.settings, read.settings)
    assertEquals(contents.labels, read.labels)
    assertEquals(contents.profiles, read.profiles)
    assertArrayEquals(png, read.wallpaper!!.readBytes())
    assertEquals(setOf("my-script"), read.scripts.keys)
    assertEquals(setOf("main.lua", "lib/util.lua"), read.scripts.getValue("my-script").keys)
    assertArrayEquals("-- main".toByteArray(), read.scripts.getValue("my-script").getValue("main.lua"))
    assertEquals(setOf("rain", "backtalk"), read.themes.keys)
    assertArrayEquals(theme, read.themes.getValue("rain").readBytes())
    assertTrue(read.themes.values.all { it.parentFile == directory })
  }

  @Test
  fun missingPartsAreNull() {
    val bytes = ByteArrayOutputStream().also { SettingsArchive.write(it, SettingsArchive.Contents("{}")) }.toByteArray()
    val read = SettingsArchive.read(ByteArrayInputStream(bytes), directory)
    assertEquals("{}", read.settings)
    assertNull(read.labels)
    assertNull(read.wallpaper)
    assertTrue(read.scripts.isEmpty() && read.themes.isEmpty())
  }

  @Test
  fun aNameOutsideTheFileIsRefused() {
    for (name in listOf("../evil.txt", "scripts/a/../../evil.txt", "/abs/evil.txt", "scripts\\a\\..\\evil", "sound-themes/../x.zip")) {
      val bytes = zipOf(SettingsArchive.SETTINGS to "{}".toByteArray(), name to "x".toByteArray())
      try {
        SettingsArchive.read(ByteArrayInputStream(bytes), directory)
        fail("Read $name")
      } catch (e: SettingsArchive.ArchiveException) {
        // Expected.
      }
      assertFalse(File(root, "evil.txt").exists())
      assertFalse(File(context.cacheDir, "evil.txt").exists())
    }
  }

  @Test
  fun unknownNamesAreSkipped() {
    val bytes = zipOf(SettingsArchive.SETTINGS to "{}".toByteArray(), "later/thing.bin" to byteArrayOf(1))
    val read = SettingsArchive.read(ByteArrayInputStream(bytes), directory)
    assertEquals("{}", read.settings)
    assertFalse(File(directory, "later").exists())
  }

  @Test
  fun aFileWithoutSettingsOrTooLargeIsRefused() {
    assertRefused(zipOf(SettingsArchive.LABELS to "{}".toByteArray()))
    val large = ByteArray((SettingsArchive.MAX_SETTINGS_BYTES + 1).toInt())
    assertRefused(zipOf(SettingsArchive.SETTINGS to large))
    assertRefused(zipOf(SettingsArchive.SETTINGS to "{}".toByteArray(), SettingsArchive.WALLPAPER to "not a png".toByteArray()))
    assertRefused("not a zip".toByteArray())
  }

  @Test
  fun settingsComeBackFromTheZip() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putString("a_string", "zip").putInt("an_int", 4).commit()
    val bytes =
      ByteArrayOutputStream().also { SettingsArchive.write(it, SettingsArchive.Contents(SettingsBackup.export(context))) }
        .toByteArray()
    prefs.edit().clear().putString("a_string", "changed").commit()

    assertTrue(SettingsBackup.importFile(context, ByteArrayInputStream(bytes)))

    assertEquals("zip", prefs.getString("a_string", null))
    assertEquals(4, prefs.getInt("an_int", 0))
  }

  @Test
  fun aBrokenZipChangesNoSettings() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putString("a_string", "kept").commit()
    val settings = SettingsBackup.export(context).replace("kept", "replaced")
    val bytes = zipOf(SettingsArchive.SETTINGS to settings.toByteArray(), "../evil.txt" to byteArrayOf(1))

    assertFalse(SettingsBackup.importFile(context, ByteArrayInputStream(bytes)))

    assertEquals("kept", prefs.getString("a_string", null))
  }

  @Test
  fun aJsonFileIsStillRead() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putString("a_string", "json").commit()
    val json = SettingsBackup.export(context)
    prefs.edit().putString("a_string", "changed").commit()

    assertTrue(SettingsBackup.importFile(context, ByteArrayInputStream(json.toByteArray())))

    assertEquals("json", prefs.getString("a_string", null))
  }

  private fun assertRefused(bytes: ByteArray) {
    try {
      SettingsArchive.read(ByteArrayInputStream(bytes), directory)
      fail("Read the file")
    } catch (e: SettingsArchive.ArchiveException) {
      // Expected.
    }
  }

  private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
      for ((name, bytes) in entries) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
      }
    }
    return out.toByteArray()
  }
}
