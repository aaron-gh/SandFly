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

package com.google.android.accessibility.talkback.actor.voicecommands

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/** Finds the phone numbers of contacts by the name said. Called off the main thread. */
object VoiceContacts {
  private const val TAG = "VoiceContacts"

  /** The most phone numbers read. */
  private const val MAX_ROWS = 10_000

  /** A phone number of the contact [name]. */
  data class Number(
    val contactId: Long,
    val name: String,
    val number: String,
    val mobile: Boolean,
    val primary: Boolean,
  )

  /**
   * What looking for a contact found: the contact's [number], or the [names] of the contacts that
   * match equally well, or nothing.
   */
  class Found(val number: Number?, val names: List<String>)

  /** Whether contacts can be read. */
  @JvmStatic
  fun canRead(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

  /** Whether [who] is a phone number rather than a name. */
  @JvmStatic
  fun isNumber(who: String): Boolean = Regex("^\\+?[\\d\\s().-]{3,}$").matches(who.trim())

  /** Reads every phone number in the contacts. */
  @JvmStatic
  fun numbers(context: Context): List<Number> {
    if (!canRead(context)) return emptyList()
    val numbers = mutableListOf<Number>()
    try {
      context.contentResolver
        .query(
          Phone.CONTENT_URI,
          arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER, Phone.TYPE, Phone.IS_SUPER_PRIMARY),
          null,
          null,
          null,
        )
        ?.use { cursor ->
          while (cursor.moveToNext() && numbers.size < MAX_ROWS) {
            val name = cursor.getString(1)?.trim().orEmpty()
            val number = cursor.getString(2)?.trim().orEmpty()
            if (name.isEmpty() || number.isEmpty()) continue
            numbers.add(
              Number(
                contactId = cursor.getLong(0),
                name = name,
                number = number,
                mobile = cursor.getInt(3) == Phone.TYPE_MOBILE,
                primary = cursor.getInt(4) != 0,
              )
            )
          }
        }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot read contacts: %s", e)
    }
    return numbers
  }

  /** Finds the contact named [who] in [numbers]. */
  @JvmStatic
  fun find(numbers: List<Number>, who: String, locale: Locale): Found {
    val query = VoiceGrammar.normalize(who, locale)
    if (query.isEmpty()) return Found(null, emptyList())
    val scored =
      numbers.groupBy { it.contactId }.values.mapNotNull { contact ->
        val score = score(VoiceGrammar.normalize(contact.first().name, locale), query)
        if (score > 0) score to contact else null
      }
    val best = scored.maxOfOrNull { it.first } ?: return Found(null, emptyList())
    val contacts = scored.filter { it.first == best }.map { it.second }
    // Two contacts of one name, as when an account and the phone each keep one, are one person.
    val distinct = contacts.distinctBy { VoiceGrammar.normalize(it.first().name, locale) }
    if (distinct.size > 1) {
      return Found(null, distinct.map { it.first().name }.sorted())
    }
    return Found(preferred(contacts.flatten()), emptyList())
  }

  /**
   * Finds the contact whose name starts [text], the longest name first. Returns the contact found
   * and the rest of [text], or null when no contact's name starts it.
   */
  @JvmStatic
  fun findAtStart(numbers: List<Number>, text: String, locale: Locale): Pair<Found, String>? {
    val words = text.trim().split(Regex("\\s+"))
    for (count in minOf(words.size, MAX_NAME_WORDS) downTo 1) {
      val name = words.take(count).joinToString(" ")
      val query = VoiceGrammar.normalize(name, locale)
      val exact =
        numbers.filter { VoiceGrammar.normalize(it.name, locale) == query }.takeIf { it.isNotEmpty() }
      val found =
        if (exact != null) Found(preferred(exact), emptyList())
        else find(numbers, name, locale).takeIf { it.number != null || it.names.isNotEmpty() }
      if (found != null) {
        return found to words.drop(count).joinToString(" ")
      }
    }
    return null
  }

  private const val MAX_NAME_WORDS = 5

  /** The number to use: the default one, else a mobile one, else the first. */
  private fun preferred(numbers: List<Number>): Number? =
    numbers.firstOrNull { it.primary } ?: numbers.firstOrNull { it.mobile } ?: numbers.firstOrNull()

  /**
   * How well [name] matches [query], both normalized: 3 for the whole name, 2 when the name starts
   * with it, 1 when a word of the name is it, 0 otherwise.
   */
  internal fun score(name: String, query: String): Int =
    when {
      name == query -> 3
      name.startsWith("$query ") -> 2
      (" $name ").contains(" $query ") -> 1
      else -> 0
    }
}
