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

package com.google.android.accessibility.talkback.soundthemes

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A sound theme's theme.json: who made it, its settings, and its own vibrations. See themes.md at
 * the top of the repository for the format. Settings left out of the file are null, and the theme
 * then uses its defaults.
 */
data class SoundThemeManifest(
  val name: String,
  val author: String? = null,
  val description: String? = null,
  val license: String? = null,
  val website: String? = null,
  /** Whether control sounds are on with this theme. */
  val controlSounds: Boolean? = null,
  /** When control sounds play in 3D: one of [AUDIO_3D_VALUES]. */
  val audio3d: String? = null,
  /**
   * The theme's vibrations, by the name of the sound they play with, as JSON objects or "none", as
   * written in theme.json, so that they are saved again as the author wrote them.
   */
  val vibrations: Map<String, Any> = emptyMap(),
  /** Problems with the file that were skipped over, for the person installing the theme. */
  val warnings: List<String> = emptyList(),
) {
  /** The vibrations as patterns for the feedback controller, with an empty pattern for none. */
  fun vibrationPatterns(): Map<String, IntArray> =
    vibrations.mapNotNull { (key, value) -> toPattern(value)?.let { key to it } }.toMap()

  fun toJson(): String {
    val json = JSONObject()
    json.put(FORMAT, FORMAT_VERSION)
    json.put(NAME, name)
    author?.let { json.put(AUTHOR, it) }
    description?.let { json.put(DESCRIPTION, it) }
    license?.let { json.put(LICENSE, it) }
    website?.let { json.put(WEBSITE, it) }
    if (controlSounds != null || audio3d != null) {
      val settings = JSONObject()
      controlSounds?.let { settings.put(CONTROL_SOUNDS, it) }
      audio3d?.let { settings.put(AUDIO_3D, it) }
      json.put(SETTINGS, settings)
    }
    if (vibrations.isNotEmpty()) {
      val all = JSONObject()
      for ((key, value) in vibrations) all.put(key, value)
      json.put(VIBRATIONS, all)
    }
    return json.toString(2)
  }

  companion object {
    const val FILE_NAME = "theme.json"
    const val FORMAT_VERSION = 1
    const val NONE = "none"

    val AUDIO_3D_VALUES = setOf("headphones", "always", "never")

    private const val FORMAT = "format"
    private const val NAME = "name"
    private const val AUTHOR = "author"
    private const val DESCRIPTION = "description"
    private const val LICENSE = "license"
    private const val WEBSITE = "website"
    private const val SETTINGS = "settings"
    private const val CONTROL_SOUNDS = "control_sounds"
    private const val AUDIO_3D = "3d_audio"
    private const val VIBRATIONS = "vibrations"
    private const val PATTERN = "pattern"
    private const val STRENGTH = "strength"
    private const val EFFECTS = "effects"

    private const val MAX_NAME_LENGTH = 100
    private const val MAX_TEXT_LENGTH = 2000
    private const val MAX_STEPS = 64
    private const val MAX_MS = 5000
    private const val MAX_LEVEL = 255

    /** The haptic primitives of VibrationEffect.Composition, by the names themes use. */
    val EFFECT_IDS =
      linkedMapOf(
        "click" to 1,
        "thud" to 2,
        "spin" to 3,
        "quick_rise" to 4,
        "slow_rise" to 5,
        "quick_fall" to 6,
        "tick" to 7,
        "low_tick" to 8,
      )

    // Separators of the pattern format HapticPatternParser reads.
    private const val AMPLITUDE_SEPARATOR = -9998
    private const val SENTINEL_SEPARATOR = -9999

    /**
     * Reads a theme.json. Vibrations not in [vibrationNames], and ones that are not well formed,
     * are left out with a warning. Throws [JSONException] if it is not a JSON object.
     *
     * @param fallbackName the name to use if the file has none, such as the name of the ZIP file
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun parse(text: String, vibrationNames: Set<String>, fallbackName: String): SoundThemeManifest {
      val json = JSONObject(text)
      val warnings = ArrayList<String>()
      if (json.optInt(FORMAT, FORMAT_VERSION) > FORMAT_VERSION) {
        warnings += "made for a newer Backtalk, so some of it may be left out"
      }
      val name = text(json, NAME, MAX_NAME_LENGTH) ?: fallbackName
      val settings = json.optJSONObject(SETTINGS)
      val controlSounds =
        settings?.opt(CONTROL_SOUNDS)?.let {
          it as? Boolean ?: null.also { warnings += "settings.$CONTROL_SOUNDS must be true or false" }
        }
      val audio3d =
        settings?.opt(AUDIO_3D)?.let {
          (it as? String)?.takeIf { value -> value in AUDIO_3D_VALUES }
            ?: null.also {
              warnings += "settings.$AUDIO_3D must be one of ${AUDIO_3D_VALUES.joinToString()}"
            }
        }
      val vibrations = LinkedHashMap<String, Any>()
      json.optJSONObject(VIBRATIONS)?.let { all ->
        for (key in all.keys()) {
          val value = all.get(key)
          if (key !in vibrationNames) {
            warnings += "vibrations.$key: there is no vibration called $key"
            continue
          }
          try {
            toPatternOrThrow(value)
            vibrations[key] = value
          } catch (e: IllegalArgumentException) {
            warnings += "vibrations.$key: ${e.message}"
          } catch (e: JSONException) {
            warnings += "vibrations.$key: ${e.message}"
          }
        }
      }
      return SoundThemeManifest(
        name = name,
        author = text(json, AUTHOR, MAX_NAME_LENGTH),
        description = text(json, DESCRIPTION, MAX_TEXT_LENGTH),
        license = text(json, LICENSE, MAX_NAME_LENGTH),
        website = text(json, WEBSITE, MAX_TEXT_LENGTH),
        controlSounds = controlSounds,
        audio3d = audio3d,
        vibrations = vibrations,
        warnings = warnings,
      )
    }

    private fun text(json: JSONObject, key: String, maxLength: Int): String? =
      json.optString(key, "").trim().take(maxLength).ifEmpty { null }

    private fun toPattern(value: Any): IntArray? =
      try {
        toPatternOrThrow(value)
      } catch (e: IllegalArgumentException) {
        null
      } catch (e: JSONException) {
        null
      }

    /**
     * Turns a theme vibration into the pattern HapticPatternParser reads: the on and off times,
     * then the strengths after -9998, then the effects after -9999. "none" is an empty pattern.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun toPatternOrThrow(value: Any): IntArray {
      if (value == NONE) return IntArray(0)
      require(value is JSONObject) { "must be an object or \"none\"" }
      val pattern = ArrayList<Int>()
      val strength = value.optJSONArray(STRENGTH)
      val times = value.optJSONArray(PATTERN)
      if (times != null) {
        require(times.length() in 1..MAX_STEPS) { "$PATTERN must have 1 to $MAX_STEPS times" }
        for (i in 0 until times.length()) pattern += number(times, i, MAX_MS, PATTERN)
      } else {
        // Without a pattern, a vibrator that cannot vary strength plays every step that has some.
        requireNotNull(strength) { "needs a $PATTERN or a $STRENGTH" }
        var on = false
        pattern += 0
        for (i in 0 until strength.length()) {
          val step = strength.getJSONArray(i)
          val ms = number(step, 0, MAX_MS, STRENGTH)
          val stepOn = number(step, 1, MAX_LEVEL, STRENGTH) > 0
          if (stepOn == on) pattern[pattern.size - 1] += ms
          else pattern += ms
          on = stepOn
        }
      }
      require(pattern.sum() in 1..MAX_MS) { "$PATTERN must last 1 to $MAX_MS ms" }
      if (strength != null) {
        require(strength.length() in 1..MAX_STEPS) { "$STRENGTH must have 1 to $MAX_STEPS steps" }
        pattern += AMPLITUDE_SEPARATOR
        var strengthMs = 0
        for (i in 0 until strength.length()) {
          val step = strength.getJSONArray(i)
          require(step.length() == 2) { "each $STRENGTH step is [ms, strength]" }
          val ms = number(step, 0, MAX_MS, STRENGTH)
          strengthMs += ms
          pattern += ms
          pattern += number(step, 1, MAX_LEVEL, STRENGTH)
        }
        // Each form is played on its own, so each must keep to the limit, not just the pattern.
        require(strengthMs in 1..MAX_MS) { "$STRENGTH must last 1 to $MAX_MS ms" }
      }
      value.optJSONArray(EFFECTS)?.let { effects ->
        require(effects.length() in 1..MAX_STEPS) { "$EFFECTS must have 1 to $MAX_STEPS effects" }
        pattern += SENTINEL_SEPARATOR
        var delayMs = 0
        for (i in 0 until effects.length()) {
          val effect = effects.getJSONArray(i)
          require(effect.length() == 3) { "each effect is [name, strength, delay in ms]" }
          val name = effect.getString(0)
          pattern +=
            requireNotNull(EFFECT_IDS[name]) {
              "$name is not an effect; use one of ${EFFECT_IDS.keys.joinToString()}"
            }
          pattern += number(effect, 1, MAX_LEVEL, EFFECTS)
          val delay = number(effect, 2, MAX_MS, EFFECTS)
          delayMs += delay
          pattern += delay
        }
        require(delayMs <= MAX_MS) { "$EFFECTS delays must add up to at most $MAX_MS ms" }
      }
      return pattern.toIntArray()
    }

    private fun number(array: JSONArray, index: Int, max: Int, field: String): Int {
      val value = array.get(index)
      require(value is Number && value.toDouble() == value.toInt().toDouble()) {
        "$field must hold whole numbers"
      }
      val number = value.toInt()
      require(number in 0..max) { "$field numbers must be from 0 to $max" }
      return number
    }
  }
}
