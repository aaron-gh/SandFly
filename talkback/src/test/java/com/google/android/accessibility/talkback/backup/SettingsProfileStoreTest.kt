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

  private val rate = "default/rate"
  private val code = "braille_keyboard/code"

  @Test
  fun startsWithoutProfiles() {
    val store = store()
    assertTrue(store.isEmpty())
    assertNull(store.manual())
    assertNull(store.nextManual())
    assertNull(store.previousManual())
    assertTrue(store.applied().isEmpty())
    assertNull(store.editing())
  }

  @Test
  fun profilesTheirTriggersAndTheOneChosenAreReadBack() {
    val store = store()
    val daily = store.add("Daily")!!
    val messaging = store.add("Messaging", mapOf(rate to 80))!!
    val triggers = SettingsProfileStore.Triggers(setOf("org.telegram.messenger"), editing = true)
    assertTrue(store.setTriggers(messaging.id, triggers))
    assertTrue(store.setManual(daily.id))

    val read = store()
    assertEquals(listOf("Daily", "Messaging"), read.profiles().map { it.name })
    assertEquals(triggers, read.profile(messaging.id)!!.triggers)
    assertTrue(read.hasTriggers())
    assertEquals(daily.id, read.manual()?.id)
    assertEquals(mapOf(rate to 80), read.settings(messaging.id))
    assertEquals(emptyMap<String, Any?>(), read.settings(daily.id))
  }

  @Test
  fun settingsKeepTheirTypesAndUnsetOnes() {
    val store = store()
    val profile = store.add("Daily")!!
    val settings =
      mapOf(
        rate to 3,
        "default/switch" to true,
        "default/scale" to 1.5f,
        "default/time" to 5L,
        "default/set" to setOf("a", "b"),
        code to "EN_UEB",
        "default/cleared" to null,
      )
    assertTrue(store.saveSettings(profile.id, settings))
    assertEquals(settings, store().settings(profile.id))
    assertFalse(store.saveSettings("00000000-0000-0000-0000-000000000000", settings))
  }

  @Test
  fun namesAreTrimmedAndUniqueIgnoringCase() {
    val store = store()
    assertEquals("Daily", store.add("  Daily ")?.name)
    assertNull(store.add("daily"))
    assertNull(store.add("   "))
    assertNull(store.add("x".repeat(SettingsProfileStore.MAX_NAME_LENGTH + 1)))
    assertTrue(store.isNameTaken("DAILY"))
    assertEquals(1, store.profiles().size)
  }

  @Test
  fun renameKeepsNamesUnique() {
    val store = store()
    val daily = store.add("Daily")!!
    val demo = store.add("Demo")!!
    assertFalse(store.rename(demo.id, "daily"))
    assertTrue(store.rename(demo.id, "Testing"))
    // Its own name in other letters.
    assertTrue(store.rename(daily.id, "DAILY"))
    assertEquals(listOf("DAILY", "Testing"), store().profiles().map { it.name })
  }

  @Test
  fun deletingTheChosenProfileChoosesNone() {
    val store = store()
    val daily = store.add("Daily")!!
    val demo = store.add("Demo")!!
    store.setManual(daily.id)
    assertTrue(store.delete(daily.id))
    assertNull(store.manual())
    assertNull(store.settings(daily.id))
    assertFalse(File(directory, "${daily.id}.json").exists())
    assertEquals(listOf(demo), store().profiles())
  }

  @Test
  fun profilesMoveWithinTheList() {
    val store = store()
    val a = store.add("A")!!
    val b = store.add("B")!!
    val c = store.add("C")!!
    assertTrue(store.move(c.id, -1))
    assertEquals(listOf(a, c, b), store().profiles())
    assertFalse(store.move(a.id, -1))
    assertFalse(store.move(b.id, 1))
  }

  @Test
  fun nextAndPreviousGoThroughNone() {
    val store = store()
    val a = store.add("A")!!
    val b = store.add("B")!!
    assertEquals(a, store.nextManual())
    assertEquals(b, store.previousManual())
    store.setManual(b.id)
    assertNull(store.nextManual())
    assertEquals(a, store.previousManual())
    store.setManual(a.id)
    assertNull(store.previousManual())
  }

  @Test
  fun theAppliedStateIsReadBack() {
    val store = store()
    val profile = store.add("Daily")!!
    val applied =
      SettingsProfileStore.Applied(
        base = mapOf(rate to 50, code to null),
        written = mapOf(rate to 80, code to "EN_UEB"),
        from = mapOf(rate to profile.id, code to profile.id),
      )
    assertTrue(store.saveApplied(applied))
    assertEquals(applied, store().applied())
    assertTrue(store.saveApplied(SettingsProfileStore.Applied()))
    assertTrue(store().applied().isEmpty())
  }

  @Test
  fun editingNamesAnExistingProfile() {
    val store = store()
    val profile = store.add("Daily")!!
    assertTrue(store.saveEditing(profile.id, mapOf(rate to 50)))
    assertEquals(profile.id to mapOf<String, Any?>(rate to 50), store().editing())
    store.delete(profile.id)
    assertNull(store().editing())
    assertTrue(store.saveEditing(null))
  }

  @Test
  fun profilesOfVersion1AreConvertedOnce() {
    directory.mkdirs()
    File(directory, "profiles.json")
      .writeText(
        """{"version": 1, "profiles": [{"id": "abc-1", "name": "Default"},
           {"id": "abc-2", "name": "Demo"}], "active": "abc-1"}"""
      )
    File(directory, "abc-2.json").writeText("""{"format": "sandfly-settings", "files": {}}""")
    val store = store()
    assertTrue(store.isLegacy())
    assertEquals("abc-1", store.legacyActive())
    assertNull(store.settings("abc-2"))
    assertTrue(store.legacySettings("abc-2")!!.contains("sandfly-settings"))

    assertTrue(store.finishLegacy(mapOf("abc-2" to mapOf(rate to 80))))
    assertFalse(store.isLegacy())
    val read = store()
    assertFalse(read.isLegacy())
    assertEquals(listOf("Demo"), read.profiles().map { it.name })
    assertEquals(mapOf(rate to 80), read.settings("abc-2"))
    assertFalse(File(directory, "abc-1.json").exists())
  }

  @Test
  fun aBrokenIndexStartsWithoutProfiles() {
    directory.mkdirs()
    File(directory, "profiles.json").writeText("{not json")
    assertTrue(store().isEmpty())
    val aside = directory.listFiles()!!.single { it.name.startsWith("profiles.json.corrupt-") }
    assertEquals("{not json", aside.readText())
  }

  @Test
  fun aBrokenIndexKeepsTheProfilesOfItsFiles() {
    val first = store().add("Daily", mapOf(rate to 1))!!
    val second = store().add("Demo")!!
    File(directory, "${first.id}.json").setLastModified(1_000_000L)
    File(directory, "${second.id}.json").setLastModified(2_000_000L)
    File(directory, "profiles.json").writeText("{not json")
    val read = store()
    assertEquals(listOf(first.id, second.id), read.profiles().map { it.id })
    assertEquals(listOf("Profile 1", "Profile 2"), read.profiles().map { it.name })
    assertNull(read.manual())
    assertEquals(mapOf(rate to 1), read.settings(first.id))
    // The new index is read back.
    assertEquals(read.profiles(), store().profiles())
  }

  @Test
  fun idsThatCouldLeaveTheDirectoryAreIgnored() {
    directory.mkdirs()
    File(directory, "profiles.json")
      .writeText(
        """{"version": 2, "profiles": [{"id": "../outside", "name": "Bad"},
           {"id": "abc-123", "name": "Good"}], "manual": "../outside"}"""
      )
    val read = store()
    assertEquals(listOf("Good"), read.profiles().map { it.name })
    assertNull(read.manual())
  }
}
