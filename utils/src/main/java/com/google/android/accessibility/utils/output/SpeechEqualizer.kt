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

package com.google.android.accessibility.utils.output

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A 12-band graphic equalizer for speech, set for SandFly default and for each voice profile. It
 * filters speech that [LowLatencyAudio] plays, and [SessionEqualizer] applies it to speech the
 * engine plays itself.
 *
 * Each setting is the gain of every band in dB, separated by commas. A missing or damaged setting
 * is flat. Presets that the user saves are shared by SandFly default and every voice profile, in
 * [PREF_PRESETS].
 */
object SpeechEqualizer {

  /** SandFly default's equalizer. */
  const val PREF_DEFAULT = "pref_speech_equalizer"

  /** Whether SandFly default's equalizer is on. Off keeps its gains. */
  const val PREF_DEFAULT_ON = "pref_speech_equalizer_on"

  /**
   * The presets that the user saved, as a JSON array of objects with a "name" and the "gains" of
   * every band.
   */
  const val PREF_PRESETS = "pref_speech_equalizer_presets"

  /** The most presets that can be saved. */
  const val MAX_SAVED_PRESETS = 50

  /** The longest name of a saved preset, in characters. */
  const val MAX_PRESET_NAME_LENGTH = 100

  /** The centre frequency of each band, in Hz. */
  @JvmField
  val FREQUENCIES = intArrayOf(32, 64, 125, 250, 500, 1000, 2000, 3000, 4000, 6000, 8000, 16000)

  @JvmField val BANDS = FREQUENCIES.size

  /** The most a band can boost or cut, in dB. */
  const val MAX_GAIN_DB = 12

  /** Gains, in dB, of each preset. */
  enum class Preset(private val gains: IntArray) {
    FLAT(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    CLARITY(intArrayOf(-4, -3, -2, -1, 0, 1, 3, 4, 4, 3, 2, 0)),
    BASS_BOOST(intArrayOf(6, 6, 5, 3, 1, 0, 0, 0, 0, 0, 0, 0)),
    BASS_CUT(intArrayOf(-12, -10, -8, -5, -2, 0, 0, 0, 0, 0, 0, 0)),
    TREBLE_BOOST(intArrayOf(0, 0, 0, 0, 0, 0, 1, 2, 3, 5, 6, 6)),
    TREBLE_CUT(intArrayOf(0, 0, 0, 0, 0, 0, -1, -2, -4, -6, -8, -10)),
    WARM(intArrayOf(2, 3, 3, 2, 1, 0, -1, -1, -2, -3, -3, -3)),
    REDUCE_HARSHNESS(intArrayOf(0, 0, 0, 0, 0, 0, -2, -4, -5, -5, -3, -2)),
    REDUCE_SIBILANCE(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, -1, -4, -6, -3)),
    SMALL_SPEAKER(intArrayOf(-12, -12, -8, -3, 0, 1, 2, 2, 1, 0, 0, 0));

    /** A copy of this preset's gains. */
    fun gains(): IntArray = gains.copyOf()

    /** The value saved for this preset. */
    val value: String
      get() = name.lowercase()
  }

  /** The setting that holds the equalizer of profile [profileId], or of SandFly default if empty. */
  @JvmStatic
  fun key(profileId: String): String =
    if (profileId.isEmpty()) PREF_DEFAULT else VoiceProfiles.key(profileId, VoiceProfiles.EQUALIZER)

  /** The setting that says whether the equalizer of [profileId], or SandFly default's, is on. */
  @JvmStatic
  fun onKey(profileId: String): String =
    if (profileId.isEmpty()) PREF_DEFAULT_ON
    else VoiceProfiles.key(profileId, VoiceProfiles.EQUALIZER_ON)

  /** Whether [key] is an equalizer's gains or switch, of SandFly default or of a profile. */
  @JvmStatic
  fun isKey(key: String?): Boolean =
    key != null &&
      (key == PREF_DEFAULT ||
        key == PREF_DEFAULT_ON ||
        (key.startsWith(VoiceProfiles.PREF_PREFIX + "_") &&
          (key.endsWith("_" + VoiceProfiles.EQUALIZER) ||
            key.endsWith("_" + VoiceProfiles.EQUALIZER_ON)) &&
          VoiceProfiles.isProfileKey(key)))

  /** Whether the equalizer of [profileId], or SandFly default's if empty, is on. */
  @JvmStatic
  fun isOn(prefs: SharedPreferences, profileId: String): Boolean =
    try {
      prefs.getBoolean(onKey(profileId), true)
    } catch (e: ClassCastException) {
      true
    }

  /** Turns the equalizer of [profileId], or SandFly default's if empty, on or off. */
  @JvmStatic
  fun setOn(prefs: SharedPreferences, profileId: String, on: Boolean) {
    prefs.edit { putBoolean(onKey(profileId), on) }
  }

  /**
   * Changes band [band] of the equalizer of [profileId], or SandFly default's if empty, by [step] dB
   * within [MAX_GAIN_DB], and returns its new gain.
   */
  @JvmStatic
  fun changeGain(prefs: SharedPreferences, profileId: String, band: Int, step: Int): Int {
    val gains = read(prefs, profileId)
    gains[band] = (gains[band] + step).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
    save(prefs, profileId, gains)
    return gains[band]
  }

  /** The gains of profile [profileId], or of SandFly default if empty. */
  @JvmStatic
  fun read(prefs: SharedPreferences, profileId: String): IntArray =
    parse(
      try {
        prefs.getString(key(profileId), null)
      } catch (e: ClassCastException) {
        null
      }
    )

  /** Saves [gains] for profile [profileId], or for SandFly default if empty. */
  @JvmStatic
  fun save(prefs: SharedPreferences, profileId: String, gains: IntArray) {
    prefs.edit { putString(key(profileId), format(gains)) }
  }

  /**
   * The gains of the voice in use, the active profile's or SandFly default's, or null if its
   * equalizer is off or flat and speech needs no filtering.
   */
  @JvmStatic
  fun inUse(prefs: SharedPreferences): IntArray? {
    val id = VoiceProfiles.activeId(prefs)
    if (!isOn(prefs, id)) return null
    val gains = read(prefs, id)
    return if (isFlat(gains)) null else gains
  }

  /** Gains from a saved [value], each kept within [MAX_GAIN_DB]; flat if it can't be read. */
  @JvmStatic
  fun parse(value: String?): IntArray {
    val gains = IntArray(BANDS)
    if (value.isNullOrEmpty()) return gains
    val parts = value.split(",")
    if (parts.size != BANDS) return gains
    for (i in 0 until BANDS) {
      val gain = parts[i].trim().toIntOrNull() ?: return IntArray(BANDS)
      gains[i] = gain.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
    }
    return gains
  }

  @JvmStatic fun format(gains: IntArray): String = gains.joinToString(",")

  @JvmStatic fun isFlat(gains: IntArray): Boolean = gains.all { it == 0 }

  /** The preset with [gains], or null for gains of no preset. */
  @JvmStatic
  fun presetOf(gains: IntArray): Preset? = Preset.entries.firstOrNull { it.gains().contentEquals(gains) }

  /** The preset saved as [value], or null. */
  @JvmStatic fun presetFor(value: String?): Preset? = Preset.entries.firstOrNull { it.value == value }

  /** A preset that the user saved: its name and the gain of each band. */
  class SavedPreset(val name: String, gains: IntArray) {
    private val gains = gains.copyOf()

    /** A copy of this preset's gains. */
    fun gains(): IntArray = gains.copyOf()

    /** Whether this preset's gains are [gains]. */
    fun matches(gains: IntArray): Boolean = this.gains.contentEquals(gains)
  }

  /** [name] without spaces around it, and cut to [MAX_PRESET_NAME_LENGTH]; empty if none. */
  @JvmStatic
  fun cleanPresetName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.length <= MAX_PRESET_NAME_LENGTH) return trimmed
    var end = MAX_PRESET_NAME_LENGTH
    // Don't cut a character outside the Basic Multilingual Plane, such as an emoji, in half.
    if (Character.isHighSurrogate(trimmed[end - 1])) end--
    return trimmed.substring(0, end).trim()
  }

  /**
   * The saved presets in [value]. A preset without a name, with a name used earlier in the list, or
   * without a whole number for each band is left out, and so is any after [MAX_SAVED_PRESETS].
   */
  @JvmStatic
  fun parsePresets(value: String?): List<SavedPreset> {
    if (value.isNullOrEmpty()) return emptyList()
    val array =
      try {
        JSONArray(value)
      } catch (e: JSONException) {
        return emptyList()
      }
    val presets = ArrayList<SavedPreset>()
    for (i in 0 until array.length()) {
      if (presets.size >= MAX_SAVED_PRESETS) break
      val preset = parsePreset(array.opt(i) as? JSONObject) ?: continue
      if (isPresetNameTaken(presets, preset.name, except = null)) continue
      presets += preset
    }
    return presets
  }

  private fun parsePreset(json: JSONObject?): SavedPreset? {
    json ?: return null
    val name = cleanPresetName(json.opt("name") as? String)
    if (name.isEmpty()) return null
    val array = json.opt("gains") as? JSONArray ?: return null
    if (array.length() != BANDS) return null
    val gains = IntArray(BANDS)
    for (i in 0 until BANDS) {
      val gain =
        when (val item = array.opt(i)) {
          is Int -> item.toLong()
          is Long -> item
          else -> return null
        }
      gains[i] = gain.coerceIn(-MAX_GAIN_DB.toLong(), MAX_GAIN_DB.toLong()).toInt()
    }
    return SavedPreset(name, gains)
  }

  /** The value saved for [presets]. */
  @JvmStatic
  fun formatPresets(presets: List<SavedPreset>): String {
    val array = JSONArray()
    for (preset in presets) {
      val gains = JSONArray()
      for (gain in preset.gains()) gains.put(gain)
      array.put(JSONObject().put("name", preset.name).put("gains", gains))
    }
    return array.toString()
  }

  /** The saved presets, in the order they were saved. */
  @JvmStatic
  fun readPresets(prefs: SharedPreferences): List<SavedPreset> =
    parsePresets(
      try {
        prefs.getString(PREF_PRESETS, null)
      } catch (e: ClassCastException) {
        null
      }
    )

  private fun savePresets(prefs: SharedPreferences, presets: List<SavedPreset>) {
    prefs.edit { putString(PREF_PRESETS, formatPresets(presets)) }
  }

  /**
   * Whether a preset in [presets] other than the one named [except] is called [name], ignoring
   * case.
   */
  @JvmStatic
  fun isPresetNameTaken(presets: List<SavedPreset>, name: String, except: String?): Boolean =
    presets.any {
      it.name.equals(name, ignoreCase = true) && (except == null || it.name != except)
    }

  /** The first of [presets] with [gains], or null. */
  @JvmStatic
  fun savedPresetOf(presets: List<SavedPreset>, gains: IntArray): SavedPreset? =
    presets.firstOrNull { it.matches(gains) }

  /** The preset in [presets] called [name], or null. */
  @JvmStatic
  fun savedPresetNamed(presets: List<SavedPreset>, name: String?): SavedPreset? =
    presets.firstOrNull { it.name == name }

  /**
   * [presets] with a preset called [name] with [gains] added at the end, or null if the name is
   * empty or taken, or [MAX_SAVED_PRESETS] are saved.
   */
  @JvmStatic
  fun withPresetAdded(
    presets: List<SavedPreset>,
    name: String,
    gains: IntArray,
  ): List<SavedPreset>? {
    val cleaned = cleanPresetName(name)
    if (
      cleaned.isEmpty() ||
        presets.size >= MAX_SAVED_PRESETS ||
        isPresetNameTaken(presets, cleaned, except = null)
    ) {
      return null
    }
    val kept = IntArray(BANDS) { gains.getOrElse(it) { 0 }.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) }
    return presets + SavedPreset(cleaned, kept)
  }

  /**
   * [presets] with the preset called [name] renamed to [newName], or null if there is none or the
   * new name is empty or another preset's.
   */
  @JvmStatic
  fun withPresetRenamed(
    presets: List<SavedPreset>,
    name: String,
    newName: String,
  ): List<SavedPreset>? {
    val cleaned = cleanPresetName(newName)
    if (
      cleaned.isEmpty() ||
        savedPresetNamed(presets, name) == null ||
        isPresetNameTaken(presets, cleaned, except = name)
    ) {
      return null
    }
    return presets.map { if (it.name == name) SavedPreset(cleaned, it.gains()) else it }
  }

  /** [presets] without the preset called [name]. */
  @JvmStatic
  fun withPresetDeleted(presets: List<SavedPreset>, name: String): List<SavedPreset> =
    presets.filter { it.name != name }

  /** Saves [gains] as a preset called [name]; false if [withPresetAdded] can't. */
  @JvmStatic
  fun addPreset(prefs: SharedPreferences, name: String, gains: IntArray): Boolean {
    val presets = withPresetAdded(readPresets(prefs), name, gains) ?: return false
    savePresets(prefs, presets)
    return true
  }

  /** Renames the preset called [name] to [newName]; false if [withPresetRenamed] can't. */
  @JvmStatic
  fun renamePreset(prefs: SharedPreferences, name: String, newName: String): Boolean {
    val presets = withPresetRenamed(readPresets(prefs), name, newName) ?: return false
    savePresets(prefs, presets)
    return true
  }

  /** Deletes the preset called [name]. Equalizers set to its gains keep them. */
  @JvmStatic
  fun deletePreset(prefs: SharedPreferences, name: String) {
    savePresets(prefs, withPresetDeleted(readPresets(prefs), name))
  }

  /**
   * Filters interleaved audio of [channels] channels at [rate] Hz with a band filter per non-zero
   * gain, and then a limiter if any band boosts, so that boosted speech doesn't clip.
   */
  class Filter(gains: IntArray, rate: Int, private val channels: Int) {
    private val b0: FloatArray
    private val b1: FloatArray
    private val b2: FloatArray
    private val a1: FloatArray
    private val a2: FloatArray
    // Each band's last two inputs and outputs, per channel.
    private val x1: FloatArray
    private val x2: FloatArray
    private val y1: FloatArray
    private val y2: FloatArray
    private val stages: Int
    private val limit = gains.any { it > 0 }
    private val release = exp(-1.0 / (LIMITER_RELEASE_S * rate)).toFloat()
    private var limiterGain = 1f

    init {
      val coefficients = ArrayList<DoubleArray>()
      for (i in 0 until BANDS) {
        val gain = gains.getOrElse(i) { 0 }
        val frequency = FREQUENCIES[i].toDouble()
        // A band at or near half the sample rate has no audio to change.
        if (gain == 0 || frequency >= rate * MAX_FREQUENCY_OF_RATE) continue
        coefficients += peaking(frequency, gain.toDouble(), q(i), rate.toDouble())
      }
      stages = coefficients.size
      b0 = FloatArray(stages) { coefficients[it][0].toFloat() }
      b1 = FloatArray(stages) { coefficients[it][1].toFloat() }
      b2 = FloatArray(stages) { coefficients[it][2].toFloat() }
      a1 = FloatArray(stages) { coefficients[it][3].toFloat() }
      a2 = FloatArray(stages) { coefficients[it][4].toFloat() }
      x1 = FloatArray(stages * channels)
      x2 = FloatArray(stages * channels)
      y1 = FloatArray(stages * channels)
      y2 = FloatArray(stages * channels)
    }

    /** Filters [samples] in place, and returns them. */
    fun process(samples: FloatArray): FloatArray {
      if (stages == 0) return samples
      val frames = samples.size / channels
      for (frame in 0 until frames) {
        var peak = 0f
        for (channel in 0 until channels) {
          val index = frame * channels + channel
          var sample = samples[index]
          for (stage in 0 until stages) {
            val state = stage * channels + channel
            val out =
              b0[stage] * sample + b1[stage] * x1[state] + b2[stage] * x2[state] -
                a1[stage] * y1[state] - a2[stage] * y2[state]
            x2[state] = x1[state]
            x1[state] = sample
            y2[state] = y1[state]
            y1[state] = out
            sample = out
          }
          samples[index] = sample
          peak = maxOf(peak, abs(sample))
        }
        if (limit) {
          // Instant attack, smooth release.
          limiterGain = 1f - (1f - limiterGain) * release
          if (peak * limiterGain > LIMITER_CEILING) limiterGain = LIMITER_CEILING / peak
          for (channel in 0 until channels) samples[frame * channels + channel] *= limiterGain
        }
      }
      // Very small numbers left in the state after speech ends are slow on some processors.
      for (i in y1.indices) {
        if (abs(y1[i]) < DENORMAL) y1[i] = 0f
        if (abs(y2[i]) < DENORMAL) y2[i] = 0f
      }
      return samples
    }
  }

  /** How much [Filter] for [gains] at [rate] Hz changes audio at [frequency] Hz, in dB. */
  @JvmStatic
  fun responseDb(gains: IntArray, frequency: Double, rate: Int): Double {
    val w = 2 * PI * frequency / rate
    var db = 0.0
    for (i in 0 until BANDS) {
      val gain = gains.getOrElse(i) { 0 }
      val centre = FREQUENCIES[i].toDouble()
      if (gain == 0 || centre >= rate * MAX_FREQUENCY_OF_RATE) continue
      val c = peaking(centre, gain.toDouble(), q(i), rate.toDouble())
      val numRe = c[0] + c[1] * cos(w) + c[2] * cos(2 * w)
      val numIm = -(c[1] * sin(w) + c[2] * sin(2 * w))
      val denRe = 1 + c[3] * cos(w) + c[4] * cos(2 * w)
      val denIm = -(c[3] * sin(w) + c[4] * sin(2 * w))
      db += 10 * log10((numRe * numRe + numIm * numIm) / (denRe * denRe + denIm * denIm))
    }
    return db
  }

  /** Band [index]'s Q, from its distance to the bands either side, in octaves. */
  private fun q(index: Int): Double {
    val log = { i: Int -> ln(FREQUENCIES[i].toDouble()) / ln(2.0) }
    val octaves =
      when (index) {
        0 -> log(1) - log(0)
        BANDS - 1 -> log(BANDS - 1) - log(BANDS - 2)
        else -> (log(index + 1) - log(index - 1)) / 2
      }
    val ratio = 2.0.pow(octaves)
    return sqrt(ratio) / (ratio - 1)
  }

  /** A peaking filter's normalized b0, b1, b2, a1, a2, from the Audio EQ Cookbook. */
  private fun peaking(frequency: Double, gainDb: Double, q: Double, rate: Double): DoubleArray {
    val a = 10.0.pow(gainDb / 40)
    val w0 = 2 * PI * frequency / rate
    val alpha = sin(w0) / (2 * q)
    val cosW0 = cos(w0)
    val a0 = 1 + alpha / a
    return doubleArrayOf(
      (1 + alpha * a) / a0,
      -2 * cosW0 / a0,
      (1 - alpha * a) / a0,
      -2 * cosW0 / a0,
      (1 - alpha / a) / a0,
    )
  }

  private const val MAX_FREQUENCY_OF_RATE = 0.45
  private const val LIMITER_CEILING = 0.98f
  private const val LIMITER_RELEASE_S = 0.08
  private const val DENORMAL = 1e-20f
}
