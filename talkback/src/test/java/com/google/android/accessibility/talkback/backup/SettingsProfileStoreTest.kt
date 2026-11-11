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

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsProfileStoreTest {
  @get:Rule val folder = TemporaryFolder()

  private val directory: File by lazy { File(folder.root, "settings_profiles") }

  private fun store() = SettingsProfileStore(directory)

  @Test
  fun startsWithoutProfiles() {
    val store = store()
    assertTrue(store.isEmpty())
    assertNull(store.active())
    assertNull(store.next())
  }

  @Test
  fun theFirstProfileIsInUse() {
    val store = store()
    val first = store.add("Default", "a")!!
    val second = store.add("Demo", "b")!!
    assertEquals(first, store.active())
    assertEquals(listOf(first, second), store.profiles())
    assertEquals("a", store.settings(first.id))
    assertEquals("b", store.settings(second.id))
  }

  @Test
  fun profilesAndTheOneInUseAreReadBack() {
    val store = store()
    store.add("Default", "a")
    val demo = store.add("Demo", "b")!!
    assertTrue(store.setActive(demo.id))

    val read = store()
    assertEquals(store.profiles(), read.profiles())
    assertEquals(demo, read.active())
    assertEquals("b", read.settings(demo.id))
  }

  @Test
  fun namesAreTrimmedAndUniqueIgnoringCase() {
    val store = store()
    assertEquals("Daily", store.add("  Daily ", "a")?.name)
    assertNull(store.add("daily", "b"))
    assertNull(store.add("   ", "b"))
    assertNull(store.add("x".repeat(SettingsProfileStore.MAX_NAME_LENGTH + 1), "b"))
    assertTrue(store.isNameTaken("DAILY"))
    assertEquals(1, store.profiles().size)
  }

  @Test
  fun renameKeepsNamesUnique() {
    val store = store()
    val daily = store.add("Daily", "a")!!
    val demo = store.add("Demo", "b")!!
    assertFalse(store.rename(demo.id, "daily"))
    assertTrue(store.rename(demo.id, "Testing"))
    // Its own name in other letters.
    assertTrue(store.rename(daily.id, "DAILY"))
    assertEquals(listOf("DAILY", "Testing"), store().profiles().map { it.name })
  }

  @Test
  fun theProfileInUseCannotBeDeleted() {
    val store = store()
    val daily = store.add("Daily", "a")!!
    val demo = store.add("Demo", "b")!!
    assertFalse(store.delete(daily.id))
    assertTrue(store.delete(demo.id))
    assertNull(store.settings(demo.id))
    assertFalse(File(directory, "${demo.id}.json").exists())
    assertEquals(listOf(daily), store().profiles())
  }

  @Test
  fun nextWrapsAround() {
    val store = store()
    val a = store.add("A", "a")!!
    val b = store.add("B", "b")!!
    val c = store.add("C", "c")!!
    assertEquals(b, store.next())
    store.setActive(c.id)
    assertEquals(a, store.next())
  }

  @Test
  fun aSingleProfileIsItsOwnNext() {
    val store = store()
    val only = store.add("Default", "a")!!
    assertEquals(only, store.next())
  }

  @Test
  fun savedSettingsReplaceTheOld() {
    val store = store()
    val daily = store.add("Daily", "a")!!
    assertTrue(store.saveSettings(daily.id, "changed"))
    assertEquals("changed", store().settings(daily.id))
    assertFalse(store.saveSettings("00000000-0000-0000-0000-000000000000", "x"))
    assertFalse(store.setActive("00000000-0000-0000-0000-000000000000"))
  }

  @Test
  fun aBrokenIndexStartsWithoutProfiles() {
    directory.mkdirs()
    File(directory, "profiles.json").writeText("{not json")
    assertTrue(store().isEmpty())
  }

  @Test
  fun anIndexWithoutAValidProfileInUseUsesTheFirst() {
    directory.mkdirs()
    File(directory, "profiles.json")
      .writeText(
        """{"profiles": [{"id": "abc-1", "name": "Daily"}, {"id": "abc-2", "name": "Demo"}],
           "active": "abc-3"}"""
      )
    assertEquals("abc-1", store().active()?.id)
  }

  @Test
  fun idsThatCouldLeaveTheDirectoryAreIgnored() {
    directory.mkdirs()
    File(directory, "profiles.json")
      .writeText(
        """{"profiles": [{"id": "../outside", "name": "Bad"}, {"id": "abc-123", "name": "Good"}],
           "active": "../outside"}"""
      )
    val read = store()
    assertEquals(listOf("Good"), read.profiles().map { it.name })
    assertEquals("abc-123", read.active()?.id)
  }
}
