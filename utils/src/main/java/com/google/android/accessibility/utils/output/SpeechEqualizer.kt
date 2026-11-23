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
 * Voice effects for speech, set for SandFly default and for each voice profile: an 11-band graphic
 * equalizer and compression of the bass, voice and treble ranges. They filter speech that
 * [LowLatencyAudio] plays, and [SessionEqualizer] applies them to speech the engine plays itself.
 *
 * Each setting is the gain of every band in dB, separated by commas, then, if any effect is on, a
 * semicolon and the level of every effect, separated by commas. A missing or damaged setting is
 * flat with no effects. Presets that the user saves are shared by SandFly default and every voice
 * profile, in [PREF_PRESETS].
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
  val FREQUENCIES = intArrayOf(64, 125, 250, 500, 1000, 2000, 3000, 4000, 6000, 8000, 16000)

  @JvmField val BANDS = FREQUENCIES.size

  /** The number of bands saved by versions up to 0.6.0. Their first band, 32 Hz, is dropped when read. */
  private const val LEGACY_BANDS = 12

  /** The most a band can boost or cut, in dB. */
  const val MAX_GAIN_DB = 12

  /** The number of effects: compression of the bass, voice and treble ranges. */
  const val EFFECTS = 3

  const val EFFECT_BASS_COMPRESSION = 0
  const val EFFECT_VOICE_COMPRESSION = 1
  const val EFFECT_TREBLE_COMPRESSION = 2

  /** The highest level of an effect: off, low, medium, then high. */
  const val MAX_EFFECT_LEVEL = 3

  /** Where the bass range ends and the voice range starts, in Hz. */
  const val BASS_CROSSOVER_HZ = 200.0

  /** Where the voice range ends and the treble range starts, in Hz. */
  const val TREBLE_CROSSOVER_HZ = 4000.0

  // Compression at each effect level: the ratio above the threshold, and the gain after it.
  private val COMPRESSION_RATIO = floatArrayOf(1f, 2f, 3f, 4f)
  private val COMPRESSION_THRESHOLD_DB = floatArrayOf(0f, -20f, -15f, -20f)
  private val COMPRESSION_GAIN_DB = floatArrayOf(0f, 3f, 6f, 9f)
  internal const val COMPRESSION_ATTACK_MS = 10f
  internal const val COMPRESSION_RELEASE_MS = 50f

  internal fun compressionRatio(level: Int): Float = COMPRESSION_RATIO[level]

  internal fun compressionThresholdDb(level: Int): Float = COMPRESSION_THRESHOLD_DB[level]

  internal fun compressionGainDb(level: Int): Float = COMPRESSION_GAIN_DB[level]

  /** The gains of the bands and the levels of the effects of one equalizer. */
  class Values(gains: IntArray, effects: IntArray) {
    private val gains =
      IntArray(BANDS) { gains.getOrElse(it) { 0 }.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) }
    private val effects =
      IntArray(EFFECTS) { effects.getOrElse(it) { 0 }.coerceIn(0, MAX_EFFECT_LEVEL) }

    /** A copy of the gains. */
    fun gains(): IntArray = gains.copyOf()

    /** A copy of the effect levels. */
    fun effects(): IntArray = effects.copyOf()

    /** Whether these values leave speech as it is. */
    fun isNeutral(): Boolean = isFlat(gains) && hasNoEffects(effects)

    override fun equals(other: Any?): Boolean =
      other is Values && gains.contentEquals(other.gains) && effects.contentEquals(other.effects)

    override fun hashCode(): Int = 31 * gains.contentHashCode() + effects.contentHashCode()
  }

  /** Gains, in dB, and effect levels of each preset. */
  enum class Preset(private val gains: IntArray, private val effects: IntArray = IntArray(EFFECTS)) {
    FLAT(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    CLARITY(intArrayOf(-3, -2, -1, 0, 1, 3, 4, 4, 3, 2, 0)),
    BASS_BOOST(intArrayOf(6, 5, 3, 1, 0, 0, 0, 0, 0, 0, 0)),
    BASS_CUT(intArrayOf(-10, -8, -5, -2, 0, 0, 0, 0, 0, 0, 0)),
    TREBLE_BOOST(intArrayOf(0, 0, 0, 0, 0, 1, 2, 3, 5, 6, 6)),
    TREBLE_CUT(intArrayOf(0, 0, 0, 0, 0, -1, -2, -4, -6, -8, -10)),
    WARM(intArrayOf(3, 3, 2, 1, 0, -1, -1, -2, -3, -3, -3)),
    REDUCE_HARSHNESS(intArrayOf(0, 0, 0, 0, 0, -2, -4, -5, -5, -3, -2)),
    REDUCE_SIBILANCE(intArrayOf(0, 0, 0, 0, 0, 0, 0, -1, -4, -6, -3)),
    SMALL_SPEAKER(intArrayOf(-12, -8, -3, 0, 1, 2, 2, 1, 0, 0, 0)),
    DEEP_BASS(intArrayOf(9, 7, 0, 0, -3, -6, -4, -4, -6, 0, 0), intArrayOf(2, 0, 0));

    /** A copy of this preset's gains. */
    fun gains(): IntArray = gains.copyOf()

    /** A copy of this preset's effect levels. */
    fun effects(): IntArray = effects.copyOf()

    /** This preset's gains and effect levels. */
    fun equalizerValues(): Values = Values(gains, effects)

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
  fun read(prefs: SharedPreferences, profileId: String): IntArray = parse(stored(prefs, profileId))

  /** The effect levels of profile [profileId], or of SandFly default if empty. */
  @JvmStatic
  fun readEffects(prefs: SharedPreferences, profileId: String): IntArray =
    parseEffects(stored(prefs, profileId))

  /** The gains and effect levels of profile [profileId], or of SandFly default if empty. */
  @JvmStatic
  fun readValues(prefs: SharedPreferences, profileId: String): Values =
    parseValues(stored(prefs, profileId))

  private fun stored(prefs: SharedPreferences, profileId: String): String? =
    try {
      prefs.getString(key(profileId), null)
    } catch (e: ClassCastException) {
      null
    }

  /** Saves [gains] for profile [profileId], or for SandFly default if empty, keeping its effects. */
  @JvmStatic
  fun save(prefs: SharedPreferences, profileId: String, gains: IntArray) {
    save(prefs, profileId, Values(gains, readEffects(prefs, profileId)))
  }

  /** Saves [effects] for profile [profileId], or for SandFly default if empty, keeping its gains. */
  @JvmStatic
  fun saveEffects(prefs: SharedPreferences, profileId: String, effects: IntArray) {
    save(prefs, profileId, Values(read(prefs, profileId), effects))
  }

  /** Saves [values] for profile [profileId], or for SandFly default if empty. */
  @JvmStatic
  fun save(prefs: SharedPreferences, profileId: String, values: Values) {
    prefs.edit { putString(key(profileId), format(values)) }
  }

  /**
   * The values of the voice in use, the active profile's or SandFly default's, or null if its
   * equalizer is off, or flat with no effects, and speech needs no filtering.
   */
  @JvmStatic
  fun inUse(prefs: SharedPreferences): Values? = inUse(prefs, VoiceProfiles.activeId(prefs))

  /**
   * The values of profile [profileId], or of SandFly default if empty, or null if its equalizer is
   * off, or flat with no effects.
   */
  @JvmStatic
  fun inUse(prefs: SharedPreferences, profileId: String): Values? {
    if (!isOn(prefs, profileId)) return null
    val values = readValues(prefs, profileId)
    return if (values.isNeutral()) null else values
  }

  /**
   * Gains from a saved [value], each kept within [MAX_GAIN_DB]; flat if it can't be read. A value of
   * [LEGACY_BANDS] bands loses its first.
   */
  @JvmStatic
  fun parse(value: String?): IntArray {
    val gains = IntArray(BANDS)
    if (value.isNullOrEmpty()) return gains
    val parts = value.substringBefore(EFFECTS_SEPARATOR).split(",")
    val skip = legacySkip(parts.size) ?: return gains
    for (i in 0 until BANDS) {
      val gain = parts[i + skip].trim().toIntOrNull() ?: return IntArray(BANDS)
      gains[i] = gain.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
    }
    return gains
  }

  /** How many leading bands a saved list of [size] bands has that are no longer used, or null. */
  private fun legacySkip(size: Int): Int? =
    when (size) {
      BANDS -> 0
      LEGACY_BANDS -> LEGACY_BANDS - BANDS
      else -> null
    }

  /** Effect levels from a saved [value], each kept within [MAX_EFFECT_LEVEL]; none if it has none. */
  @JvmStatic
  fun parseEffects(value: String?): IntArray {
    val effects = IntArray(EFFECTS)
    if (value == null || !value.contains(EFFECTS_SEPARATOR)) return effects
    val parts = value.substringAfter(EFFECTS_SEPARATOR).split(",")
    if (parts.size != EFFECTS) return effects
    for (i in 0 until EFFECTS) {
      val level = parts[i].trim().toIntOrNull() ?: return IntArray(EFFECTS)
      effects[i] = level.coerceIn(0, MAX_EFFECT_LEVEL)
    }
    return effects
  }

  /** The gains and effect levels from a saved [value]. */
  @JvmStatic fun parseValues(value: String?): Values = Values(parse(value), parseEffects(value))

  @JvmStatic fun format(gains: IntArray): String = gains.joinToString(",")

  /** The saved value of [values]. Effects are left out while none is on. */
  @JvmStatic
  fun format(values: Values): String {
    val effects = values.effects()
    val gains = format(values.gains())
    return if (hasNoEffects(effects)) gains
    else gains + EFFECTS_SEPARATOR + effects.joinToString(",")
  }

  @JvmStatic fun isFlat(gains: IntArray): Boolean = gains.all { it == 0 }

  /** Whether every effect in [effects] is off. */
  @JvmStatic fun hasNoEffects(effects: IntArray): Boolean = effects.all { it == 0 }

  /** The preset with [values], or null for values of no preset. */
  @JvmStatic
  fun presetOf(values: Values): Preset? = Preset.entries.firstOrNull { it.equalizerValues() == values }

  /** The preset saved as [value], or null. */
  @JvmStatic fun presetFor(value: String?): Preset? = Preset.entries.firstOrNull { it.value == value }

  /** A preset that the user saved: its name, the gain of each band and the level of each effect. */
  class SavedPreset(val name: String, val values: Values) {
    constructor(name: String, gains: IntArray) : this(name, Values(gains, IntArray(EFFECTS)))

    /** A copy of this preset's gains. */
    fun gains(): IntArray = values.gains()

    /** A copy of this preset's effect levels. */
    fun effects(): IntArray = values.effects()

    /** Whether this preset's gains and effect levels are [values]. */
    fun matches(values: Values): Boolean = this.values == values
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
   * without a whole number for each band and, if it has effects, each effect, is left out, and so
   * is any after [MAX_SAVED_PRESETS]. A preset of [LEGACY_BANDS] bands loses its first.
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
    val skip = legacySkip(array.length()) ?: return null
    val gains = IntArray(BANDS)
    for (i in 0 until BANDS) {
      gains[i] = wholeNumber(array.opt(i + skip), MAX_GAIN_DB) ?: return null
    }
    val effects = IntArray(EFFECTS)
    if (json.has("effects")) {
      val levels = json.opt("effects") as? JSONArray ?: return null
      if (levels.length() != EFFECTS) return null
      for (i in 0 until EFFECTS) {
        effects[i] = (wholeNumber(levels.opt(i), MAX_EFFECT_LEVEL) ?: return null).coerceAtLeast(0)
      }
    }
    return SavedPreset(name, Values(gains, effects))
  }

  /** [item] kept within -[limit] and [limit] if it is a whole number, or null. */
  private fun wholeNumber(item: Any?, limit: Int): Int? {
    val number =
      when (item) {
        is Int -> item.toLong()
        is Long -> item
        else -> return null
      }
    return number.coerceIn(-limit.toLong(), limit.toLong()).toInt()
  }

  /** The value saved for [presets]. */
  @JvmStatic
  fun formatPresets(presets: List<SavedPreset>): String {
    val array = JSONArray()
    for (preset in presets) {
      val gains = JSONArray()
      for (gain in preset.gains()) gains.put(gain)
      val json = JSONObject().put("name", preset.name).put("gains", gains)
      val effects = preset.effects()
      if (!hasNoEffects(effects)) {
        val levels = JSONArray()
        for (level in effects) levels.put(level)
        json.put("effects", levels)
      }
      array.put(json)
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

  /** The first of [presets] with [values], or null. */
  @JvmStatic
  fun savedPresetOf(presets: List<SavedPreset>, values: Values): SavedPreset? =
    presets.firstOrNull { it.matches(values) }

  /** The preset in [presets] called [name], or null. */
  @JvmStatic
  fun savedPresetNamed(presets: List<SavedPreset>, name: String?): SavedPreset? =
    presets.firstOrNull { it.name == name }

  /**
   * [presets] with a preset called [name] with [values] added at the end, or null if the name is
   * empty or taken, or [MAX_SAVED_PRESETS] are saved.
   */
  @JvmStatic
  fun withPresetAdded(
    presets: List<SavedPreset>,
    name: String,
    values: Values,
  ): List<SavedPreset>? {
    val cleaned = cleanPresetName(name)
    if (
      cleaned.isEmpty() ||
        presets.size >= MAX_SAVED_PRESETS ||
        isPresetNameTaken(presets, cleaned, except = null)
    ) {
      return null
    }
    return presets + SavedPreset(cleaned, values)
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
    return presets.map { if (it.name == name) SavedPreset(cleaned, it.values) else it }
  }

  /** [presets] without the preset called [name]. */
  @JvmStatic
  fun withPresetDeleted(presets: List<SavedPreset>, name: String): List<SavedPreset> =
    presets.filter { it.name != name }

  /** Saves [values] as a preset called [name]; false if [withPresetAdded] can't. */
  @JvmStatic
  fun addPreset(prefs: SharedPreferences, name: String, values: Values): Boolean {
    val presets = withPresetAdded(readPresets(prefs), name, values) ?: return false
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

  /** Deletes the preset called [name]. Equalizers set to its values keep them. */
  @JvmStatic
  fun deletePreset(prefs: SharedPreferences, name: String) {
    savePresets(prefs, withPresetDeleted(readPresets(prefs), name))
  }

  /**
   * Filters interleaved audio of [channels] channels at [rate] Hz with a band filter per non-zero
   * gain, then compresses each range whose effect in [effects] is on, and then limits it if any
   * band boosts or effect is on, so that boosted speech doesn't clip.
   */
  class Filter(
    gains: IntArray,
    rate: Int,
    private val channels: Int,
    effects: IntArray = IntArray(EFFECTS),
  ) {
    private val bands: Array<Biquad>
    private val limit = gains.any { it > 0 } || effects.any { it > 0 }
    private val release = exp(-1.0 / (LIMITER_RELEASE_S * rate)).toFloat()
    private var limiterGain = 1f
    private val compressors =
      Array(EFFECTS) { Compressor(effects.getOrElse(it) { 0 }.coerceIn(0, MAX_EFFECT_LEVEL), rate) }
    private val compress = compressors.any { it.on }
    // Linkwitz-Riley crossovers, each two Butterworth filters, between the three ranges. The
    // all-pass filter keeps the bass range in phase with the others, so that they add up to the
    // same level.
    private val trebleHz = minOf(TREBLE_CROSSOVER_HZ, rate * CROSSOVER_MAX_OF_RATE)
    private val bassLow = crossover(::lowPass, BASS_CROSSOVER_HZ, rate)
    private val bassHigh = crossover(::highPass, BASS_CROSSOVER_HZ, rate)
    private val voiceLow = crossover(::lowPass, trebleHz, rate)
    private val voiceHigh = crossover(::highPass, trebleHz, rate)
    private val bassAllPass = Biquad(allPass(trebleHz, BUTTERWORTH_Q, rate.toDouble()), channels)
    private val crossovers = arrayOf(*bassLow, *bassHigh, *voiceLow, *voiceHigh, bassAllPass)
    // Each range's samples of the frame being compressed, per channel.
    private val ranges = FloatArray(EFFECTS * channels)

    init {
      val coefficients = ArrayList<DoubleArray>()
      for (i in 0 until BANDS) {
        val gain = gains.getOrElse(i) { 0 }
        val frequency = FREQUENCIES[i].toDouble()
        // A band at or near half the sample rate has no audio to change.
        if (gain == 0 || frequency >= rate * MAX_FREQUENCY_OF_RATE) continue
        coefficients += peaking(frequency, gain.toDouble(), q(i), rate.toDouble())
      }
      bands = Array(coefficients.size) { Biquad(coefficients[it], channels) }
    }

    private fun crossover(
      design: (Double, Double, Double) -> DoubleArray,
      frequency: Double,
      rate: Int,
    ): Array<Biquad> =
      Array(2) { Biquad(design(frequency, BUTTERWORTH_Q, rate.toDouble()), channels) }

    /** Filters [samples] in place, and returns them. */
    fun process(samples: FloatArray): FloatArray {
      if (bands.isEmpty() && !compress) return samples
      val frames = samples.size / channels
      for (frame in 0 until frames) {
        val start = frame * channels
        for (channel in 0 until channels) {
          var sample = samples[start + channel]
          for (band in bands) sample = band.process(sample, channel)
          samples[start + channel] = sample
        }
        if (compress) compressFrame(samples, start)
        if (limit) {
          var peak = 0f
          for (channel in 0 until channels) peak = maxOf(peak, abs(samples[start + channel]))
          // Instant attack, smooth release.
          limiterGain = 1f - (1f - limiterGain) * release
          if (peak * limiterGain > LIMITER_CEILING) limiterGain = LIMITER_CEILING / peak
          for (channel in 0 until channels) samples[start + channel] *= limiterGain
        }
      }
      // Very small numbers left in the state after speech ends are slow on some processors.
      for (band in bands) band.flush()
      if (compress) for (filter in crossovers) filter.flush()
      return samples
    }

    /** Splits the frame at [start] into the three ranges, compresses each, and adds them up. */
    private fun compressFrame(samples: FloatArray, start: Int) {
      var bassPeak = 0f
      var voicePeak = 0f
      var treblePeak = 0f
      for (channel in 0 until channels) {
        val sample = samples[start + channel]
        val bass =
          bassAllPass.process(
            bassLow[1].process(bassLow[0].process(sample, channel), channel),
            channel,
          )
        val rest = bassHigh[1].process(bassHigh[0].process(sample, channel), channel)
        val voice = voiceLow[1].process(voiceLow[0].process(rest, channel), channel)
        val treble = voiceHigh[1].process(voiceHigh[0].process(rest, channel), channel)
        val index = channel * EFFECTS
        ranges[index + EFFECT_BASS_COMPRESSION] = bass
        ranges[index + EFFECT_VOICE_COMPRESSION] = voice
        ranges[index + EFFECT_TREBLE_COMPRESSION] = treble
        bassPeak = maxOf(bassPeak, abs(bass))
        voicePeak = maxOf(voicePeak, abs(voice))
        treblePeak = maxOf(treblePeak, abs(treble))
      }
      // The channels share each range's gain, so that the sound doesn't move between them.
      val bassGain = compressors[EFFECT_BASS_COMPRESSION].gainFor(bassPeak)
      val voiceGain = compressors[EFFECT_VOICE_COMPRESSION].gainFor(voicePeak)
      val trebleGain = compressors[EFFECT_TREBLE_COMPRESSION].gainFor(treblePeak)
      for (channel in 0 until channels) {
        val index = channel * EFFECTS
        samples[start + channel] =
          ranges[index + EFFECT_BASS_COMPRESSION] * bassGain +
            ranges[index + EFFECT_VOICE_COMPRESSION] * voiceGain +
            ranges[index + EFFECT_TREBLE_COMPRESSION] * trebleGain
      }
    }
  }

  /** A biquad filter with its last two inputs and outputs per channel. */
  private class Biquad(c: DoubleArray, channels: Int) {
    private val b0 = c[0].toFloat()
    private val b1 = c[1].toFloat()
    private val b2 = c[2].toFloat()
    private val a1 = c[3].toFloat()
    private val a2 = c[4].toFloat()
    private val x1 = FloatArray(channels)
    private val x2 = FloatArray(channels)
    private val y1 = FloatArray(channels)
    private val y2 = FloatArray(channels)

    fun process(sample: Float, channel: Int): Float {
      val out =
        b0 * sample + b1 * x1[channel] + b2 * x2[channel] - a1 * y1[channel] - a2 * y2[channel]
      x2[channel] = x1[channel]
      x1[channel] = sample
      y2[channel] = y1[channel]
      y1[channel] = out
      return out
    }

    fun flush() {
      for (i in y1.indices) {
        if (abs(y1[i]) < DENORMAL) y1[i] = 0f
        if (abs(y2[i]) < DENORMAL) y2[i] = 0f
      }
    }
  }

  /** The compression of one range at effect [level], off at 0, for audio at [rate] Hz. */
  private class Compressor(level: Int, rate: Int) {
    val on = level > 0
    private val threshold = 10.0.pow(compressionThresholdDb(level) / 20.0).toFloat()
    private val exponent = 1f - 1f / compressionRatio(level)
    private val gain = 10.0.pow(compressionGainDb(level) / 20.0).toFloat()
    private val attack = exp(-1000.0 / (COMPRESSION_ATTACK_MS * rate)).toFloat()
    private val release = exp(-1000.0 / (COMPRESSION_RELEASE_MS * rate)).toFloat()
    // The range's level: it rises with each peak at once and falls over the release time.
    private var level = 0f
    private var reduction = 1f

    /** The gain for a frame whose peak in this range is [peak]. */
    fun gainFor(peak: Float): Float {
      if (!on) return 1f
      level = maxOf(peak, level * release)
      val target = if (level > threshold) (threshold / level).pow(exponent) else 1f
      // Reduction comes in over the attack time, and goes as the level falls.
      reduction = if (target < reduction) target + (reduction - target) * attack else target
      return reduction * gain
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

  /** A low-pass filter's normalized b0, b1, b2, a1, a2, from the Audio EQ Cookbook. */
  private fun lowPass(frequency: Double, q: Double, rate: Double): DoubleArray {
    val w0 = 2 * PI * frequency / rate
    val alpha = sin(w0) / (2 * q)
    val cosW0 = cos(w0)
    val a0 = 1 + alpha
    return doubleArrayOf(
      (1 - cosW0) / 2 / a0,
      (1 - cosW0) / a0,
      (1 - cosW0) / 2 / a0,
      -2 * cosW0 / a0,
      (1 - alpha) / a0,
    )
  }

  /** A high-pass filter's normalized b0, b1, b2, a1, a2, from the Audio EQ Cookbook. */
  private fun highPass(frequency: Double, q: Double, rate: Double): DoubleArray {
    val w0 = 2 * PI * frequency / rate
    val alpha = sin(w0) / (2 * q)
    val cosW0 = cos(w0)
    val a0 = 1 + alpha
    return doubleArrayOf(
      (1 + cosW0) / 2 / a0,
      -(1 + cosW0) / a0,
      (1 + cosW0) / 2 / a0,
      -2 * cosW0 / a0,
      (1 - alpha) / a0,
    )
  }

  /** An all-pass filter's normalized b0, b1, b2, a1, a2, from the Audio EQ Cookbook. */
  private fun allPass(frequency: Double, q: Double, rate: Double): DoubleArray {
    val w0 = 2 * PI * frequency / rate
    val alpha = sin(w0) / (2 * q)
    val cosW0 = cos(w0)
    val a0 = 1 + alpha
    return doubleArrayOf(
      (1 - alpha) / a0,
      -2 * cosW0 / a0,
      (1 + alpha) / a0,
      -2 * cosW0 / a0,
      (1 - alpha) / a0,
    )
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

  private const val EFFECTS_SEPARATOR = ';'
  private const val MAX_FREQUENCY_OF_RATE = 0.45
  // The highest crossover, as a share of the sample rate.
  private const val CROSSOVER_MAX_OF_RATE = 0.4
  private val BUTTERWORTH_Q = 1 / sqrt(2.0)
  private const val LIMITER_CEILING = 0.98f
  private const val LIMITER_RELEASE_S = 0.08
  private const val DENORMAL = 1e-20f
}
