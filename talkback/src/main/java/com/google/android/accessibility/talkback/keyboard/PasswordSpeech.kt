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
package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.text.SpannableStringBuilder
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.VerbosityPreferences
import com.google.android.accessibility.utils.StringBuilderUtils
import com.google.android.accessibility.utils.output.SpeechCleanupUtils
import com.google.android.accessibility.utils.output.SpeechControllerImpl

/**
 * The characters of a password field, which the SandFly keyboard reads through its input
 * connection; accessibility events and nodes give only bullets for them. They are spoken through
 * two paths only: with Speak password characters on, a character typed, deleted or moved over
 * ([Echo]), and the Spell the password field command ([spell]). They are spoken with no speech
 * history and never logged.
 */
object PasswordSpeech {
  /** The text of a field and its selection, read together. */
  class Field(val text: String, val selectionStart: Int, val selectionEnd: Int) {
    val hasCursor: Boolean
      get() = selectionStart == selectionEnd
  }

  /** What one key did in a password field: one character typed, deleted, replaced or passed. */
  sealed class Change {
    class Typed(val character: Char) : Change()

    class Deleted(val character: Char) : Change()

    class Replaced(val character: Char, val old: Char) : Change()

    class Passed(val character: Char) : Change()
  }

  /** What is spoken for a change, at a pitch relative to the speech pitch. */
  class Said(val text: CharSequence, val pitch: Float)

  /**
   * The change from [before] to [after] if it is one character typed, deleted or replaced, or a
   * cursor move over one character, as one key makes. Null for any other change, such as pasted
   * text, a cleared field or a selection.
   */
  @JvmStatic
  fun change(before: Field, after: Field): Change? {
    val old = before.text
    val new = after.text
    if (old == new) {
      if (!before.hasCursor || !after.hasCursor) return null
      val from = minOf(before.selectionStart, after.selectionStart)
      val to = maxOf(before.selectionStart, after.selectionStart)
      return if (to - from == 1 && to <= new.length && isOne(new[from])) Change.Passed(new[from])
      else null
    }
    var prefix = 0
    val shorter = minOf(old.length, new.length)
    while (prefix < shorter && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (
      suffix < shorter - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
    ) {
      suffix++
    }
    val removed = old.substring(prefix, old.length - suffix)
    val added = new.substring(prefix, new.length - suffix)
    if (!added.all(::isOne) || !removed.all(::isOne)) return null
    return when {
      added.length == 1 && removed.isEmpty() -> Change.Typed(added[0])
      removed.length == 1 && added.isEmpty() -> Change.Deleted(removed[0])
      added.length == 1 && removed.length == 1 -> Change.Replaced(added[0], removed[0])
      else -> null
    }
  }

  /** Whether [c] is a whole character, not half of an emoji or another surrogate pair. */
  private fun isOne(c: Char): Boolean = !c.isSurrogate()

  /**
   * The name of [character] as SandFly says a character typed or moved over: symbols by their
   * names, such as "at", and capitals as "capital P" when [sayCapital], the Capital letters
   * setting's Say "capital". Otherwise a capital is the letter, for the speech to raise its pitch
   * or play its sound.
   */
  @JvmStatic
  fun name(context: Context, character: Char, sayCapital: Boolean): String =
    if (sayCapital && Character.isUpperCase(character)) {
      context.getString(R.string.template_capital_letter, character.toString())
    } else {
      SpeechCleanupUtils.getCleanValueFor(context, character)
    }

  /**
   * What SandFly says for [change], with the texts of typing: the character, "a deleted" at a
   * higher pitch, or "b replaced a".
   */
  @JvmStatic
  fun describe(context: Context, change: Change, sayCapital: Boolean): Said =
    when (change) {
      is Change.Typed -> Said(name(context, change.character, sayCapital), 1f)
      is Change.Passed -> Said(name(context, change.character, sayCapital), 1f)
      is Change.Deleted ->
        Said(
          context.getString(
            R.string.template_text_removed,
            name(context, change.character, sayCapital),
          ),
          CHANGED_PITCH,
        )
      is Change.Replaced ->
        Said(
          context.getString(
            R.string.template_text_replaced,
            name(context, change.character, sayCapital),
            name(context, change.old, sayCapital),
          ),
          CHANGED_PITCH,
        )
    }

  /**
   * [text] spelled with the place of each character, such as "1 capital P, 2 a, 3 at". Capitals are
   * always said as "capital P", since a long phrase has no pitch for a single letter. An emoji or
   * other character outside the Basic Multilingual Plane is one place.
   */
  @JvmStatic
  fun spell(context: Context, text: CharSequence): CharSequence {
    val builder = SpannableStringBuilder()
    var place = 1
    var i = 0
    while (i < text.length) {
      val end = i + Character.charCount(Character.codePointAt(text, i))
      val character =
        if (end - i == 1) name(context, text[i], sayCapital = true)
        else text.subSequence(i, end).toString()
      // A comma after every character, an emoji too, so that each place is a phrase of its own.
      if (builder.isNotEmpty()) builder.append(StringBuilderUtils.DEFAULT_BREAKING_SEPARATOR)
      builder.append(
        context.getString(R.string.sandfly_keyboard_password_character, place, character)
      )
      place++
      i = end
    }
    return builder
  }

  /** Whether Speak password characters is on. */
  @JvmStatic
  fun isOn(context: Context, prefs: SharedPreferences): Boolean =
    prefs.getBoolean(
      context.getString(R.string.pref_speak_password_characters_key),
      context.resources.getBoolean(R.bool.pref_speak_password_characters_default),
    )

  /** Whether the Capital letters setting is Say "capital". */
  @JvmStatic
  fun saysCapital(context: Context, prefs: SharedPreferences): Boolean {
    val res = context.resources
    val value =
      VerbosityPreferences.getPreferenceValueString(
        prefs,
        res,
        res.getString(R.string.pref_capital_letters_key),
        res.getString(R.string.pref_capital_letters_default),
      )
    return value.toIntOrNull() == SpeechControllerImpl.CAPITAL_LETTERS_TYPE_SPEAK_CAP
  }

  /**
   * Follows the password field being edited, from one reading of it to the next, and says what one
   * key changed. It follows a field only while Speak password characters is on, the field is a
   * password field and its text can be read.
   */
  class Echo(private val context: Context) {
    private var last: Field? = null

    /** Whether it follows a field, so that it says its characters. */
    val isFollowing: Boolean
      get() = last != null

    /** Stops following the field, as when input ends or the setting is turned off. */
    fun stop() {
      last = null
    }

    /**
     * Takes [field] as just read, or null where it couldn't be read, in a password field when
     * [password] and with Speak password characters on when [on]. Returns what to say for the
     * change since the last reading, or null.
     */
    fun follow(field: Field?, on: Boolean, password: Boolean, sayCapital: Boolean): Said? {
      if (!on || !password || field == null) {
        last = null
        return null
      }
      val before = last
      last = field
      val change = before?.let { change(it, field) } ?: return null
      return describe(context, change, sayCapital)
    }
  }

  /** The pitch of deleted and replaced characters, as for text deleted in other fields. */
  private const val CHANGED_PITCH = 1.2f
}
