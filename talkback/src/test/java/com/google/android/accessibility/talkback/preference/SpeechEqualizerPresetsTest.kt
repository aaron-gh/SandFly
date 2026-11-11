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

package com.google.android.accessibility.talkback.preference

import com.google.android.accessibility.utils.output.SpeechEqualizer
import com.google.android.accessibility.utils.output.SpeechEqualizer.SavedPreset
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saved equalizer presets in [SpeechEqualizer]: reading, checking and matching them. */
class SpeechEqualizerPresetsTest {
  private val gains = intArrayOf(1, 2, 3, 4, 5, 6, -1, -2, -3, -4, -5, -6)

  private fun preset(name: Any?, gains: List<Any?>): JSONObject =
    JSONObject().put("name", name).put("gains", JSONArray(gains))

  @Test
  fun damagedValuesHaveNoPresets() {
    assertTrue(SpeechEqualizer.parsePresets(null).isEmpty())
    assertTrue(SpeechEqualizer.parsePresets("").isEmpty())
    assertTrue(SpeechEqualizer.parsePresets("not json").isEmpty())
    assertTrue(SpeechEqualizer.parsePresets("{\"name\":\"A\"}").isEmpty())
  }

  @Test
  fun formatParsesBack() {
    val presets = listOf(SavedPreset("Podcast", gains), SavedPreset("Car", IntArray(12)))
    val parsed = SpeechEqualizer.parsePresets(SpeechEqualizer.formatPresets(presets))
    assertEquals(listOf("Podcast", "Car"), parsed.map { it.name })
    assertArrayEquals(gains, parsed[0].gains())
    assertArrayEquals(IntArray(12), parsed[1].gains())
  }

  @Test
  fun badEntriesAreDropped() {
    val twelve = List<Any?>(12) { 0 }
    val array =
      JSONArray()
        .put(preset("Kept", twelve))
        .put(preset("", twelve))
        .put(preset("   ", twelve))
        .put(preset(5, twelve))
        .put(preset("Short", List<Any?>(11) { 0 }))
        .put(preset("Long", List<Any?>(13) { 0 }))
        .put(preset("Text", twelve.toMutableList().also { it[3] = "x" }))
        .put(preset("Fraction", twelve.toMutableList().also { it[3] = 1.5 }))
        .put(JSONObject().put("name", "No gains"))
        .put("not an object")
        .put(JSONObject.NULL)
    val parsed = SpeechEqualizer.parsePresets(array.toString())
    assertEquals(listOf("Kept"), parsed.map { it.name })
  }

  @Test
  fun gainsAreKeptInRange() {
    val values = MutableList<Any?>(12) { 0 }
    values[0] = 99
    values[1] = -99
    values[2] = 10_000_000_000L
    values[11] = 7
    val parsed = SpeechEqualizer.parsePresets(JSONArray().put(preset("A", values)).toString())
    val parsedGains = parsed.single().gains()
    assertEquals(SpeechEqualizer.MAX_GAIN_DB, parsedGains[0])
    assertEquals(-SpeechEqualizer.MAX_GAIN_DB, parsedGains[1])
    assertEquals(SpeechEqualizer.MAX_GAIN_DB, parsedGains[2])
    assertEquals(7, parsedGains[11])
  }

  @Test
  fun namesAreTrimmedAndCut() {
    val twelve = List<Any?>(12) { 0 }
    val long = "x".repeat(150)
    val array = JSONArray().put(preset("  Podcast  ", twelve)).put(preset(long, twelve))
    val parsed = SpeechEqualizer.parsePresets(array.toString())
    assertEquals("Podcast", parsed[0].name)
    assertEquals(SpeechEqualizer.MAX_PRESET_NAME_LENGTH, parsed[1].name.length)
    // An emoji across the limit isn't cut in half.
    val emoji = "x".repeat(SpeechEqualizer.MAX_PRESET_NAME_LENGTH - 1) + "😀"
    assertEquals(
      SpeechEqualizer.MAX_PRESET_NAME_LENGTH - 1,
      SpeechEqualizer.cleanPresetName(emoji).length,
    )
  }

  @Test
  fun namesAreUniqueIgnoringCase() {
    val array =
      JSONArray()
        .put(preset("Podcast", gains.toList()))
        .put(preset("podcast", List<Any?>(12) { 0 }))
        .put(preset(" PODCAST ", List<Any?>(12) { 0 }))
    val parsed = SpeechEqualizer.parsePresets(array.toString())
    assertEquals(1, parsed.size)
    assertArrayEquals(gains, parsed[0].gains())
  }

  @Test
  fun atMostFiftyPresetsAreRead() {
    val array = JSONArray()
    for (i in 1..60) array.put(preset("P$i", List<Any?>(12) { 0 }))
    val parsed = SpeechEqualizer.parsePresets(array.toString())
    assertEquals(SpeechEqualizer.MAX_SAVED_PRESETS, parsed.size)
    assertEquals("P50", parsed.last().name)
  }

  @Test
  fun matchingFindsTheFirstPresetWithTheGains() {
    val presets =
      listOf(SavedPreset("A", IntArray(12)), SavedPreset("B", gains), SavedPreset("C", gains))
    assertEquals("B", SpeechEqualizer.savedPresetOf(presets, gains.copyOf())?.name)
    assertNull(SpeechEqualizer.savedPresetOf(presets, IntArray(12) { 1 }))
    assertEquals("C", SpeechEqualizer.savedPresetNamed(presets, "C")?.name)
    assertNull(SpeechEqualizer.savedPresetNamed(presets, "c"))
  }

  @Test
  fun savedGainsDontChangeWithTheArray() {
    val array = gains.copyOf()
    val preset = SavedPreset("A", array)
    array[0] = 0
    preset.gains()[1] = 0
    assertArrayEquals(gains, preset.gains())
  }

  @Test
  fun addingChecksTheName() {
    val presets = listOf(SavedPreset("Podcast", gains))
    assertNull(SpeechEqualizer.withPresetAdded(presets, "  ", gains))
    assertNull(SpeechEqualizer.withPresetAdded(presets, " PODCAST", gains))
    val added = SpeechEqualizer.withPresetAdded(presets, " Car ", IntArray(12) { 20 })!!
    assertEquals(listOf("Podcast", "Car"), added.map { it.name })
    assertArrayEquals(IntArray(12) { SpeechEqualizer.MAX_GAIN_DB }, added[1].gains())
    val full = List(SpeechEqualizer.MAX_SAVED_PRESETS) { SavedPreset("P$it", gains) }
    assertNull(SpeechEqualizer.withPresetAdded(full, "Another", gains))
  }

  @Test
  fun renamingChecksTheName() {
    val presets = listOf(SavedPreset("Podcast", gains), SavedPreset("Car", IntArray(12)))
    assertNull(SpeechEqualizer.withPresetRenamed(presets, "Podcast", "car"))
    assertNull(SpeechEqualizer.withPresetRenamed(presets, "Podcast", " "))
    assertNull(SpeechEqualizer.withPresetRenamed(presets, "Missing", "Other"))
    // A preset can change the case of its own name.
    val renamed = SpeechEqualizer.withPresetRenamed(presets, "Podcast", " PODCAST ")!!
    assertEquals(listOf("PODCAST", "Car"), renamed.map { it.name })
    assertArrayEquals(gains, renamed[0].gains())
    assertFalse(SpeechEqualizer.isPresetNameTaken(presets, "podcast", except = "Podcast"))
    assertTrue(SpeechEqualizer.isPresetNameTaken(presets, "podcast", except = "Car"))
  }

  @Test
  fun deletingKeepsTheOthers() {
    val presets = listOf(SavedPreset("Podcast", gains), SavedPreset("Car", IntArray(12)))
    val deleted = SpeechEqualizer.withPresetDeleted(presets, "Podcast")
    assertEquals(listOf("Car"), deleted.map { it.name })
    assertEquals(2, SpeechEqualizer.withPresetDeleted(presets, "Other").size)
  }

  @Test
  fun presetsArentAnEqualizerKey() {
    assertFalse(SpeechEqualizer.isKey(SpeechEqualizer.PREF_PRESETS))
  }
}
