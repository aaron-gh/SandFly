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

package com.google.android.accessibility.talkback.gesture

import android.content.Intent
import org.xmlpull.v1.XmlPullParser

/**
 * Reads the static shortcuts of an app from the XML file that its home screen activity names in
 * the [META_DATA] meta-data, as a launcher does for the shortcuts a long press on the app's icon
 * shows.
 */
object AppShortcutXml {

  /** The meta-data of a home screen activity that names its shortcuts XML file. */
  const val META_DATA = "android.app.shortcuts"

  private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

  /** A static shortcut: its ID in the app, its name, and the intents that it starts, in order. */
  data class Shortcut(val id: String, val label: String, val intents: List<Intent>)

  /**
   * Returns the enabled shortcuts in the shortcuts XML [parser] reads, with an ID, a name and at
   * least one intent, in the file's order. [text] gives the text of an attribute value, which is a
   * resource reference such as "@2131886080" in a compiled file, or null. [readIntent] reads the
   * intent element at the parser's start tag, leaving the parser at its end tag, or returns null
   * when it can't.
   */
  @JvmStatic
  fun parse(
    parser: XmlPullParser,
    text: (String?) -> String?,
    readIntent: (XmlPullParser) -> Intent?,
  ): List<Shortcut> {
    val shortcuts = mutableListOf<Shortcut>()
    var type = parser.eventType
    while (type != XmlPullParser.END_DOCUMENT) {
      if (type == XmlPullParser.START_TAG && parser.name == "shortcut") {
        readShortcut(parser, text, readIntent)?.let { shortcuts += it }
      }
      type = parser.next()
    }
    return shortcuts.distinctBy { it.id }
  }

  /** Reads the shortcut element at the parser's start tag, leaving the parser at its end tag. */
  private fun readShortcut(
    parser: XmlPullParser,
    text: (String?) -> String?,
    readIntent: (XmlPullParser) -> Intent?,
  ): Shortcut? {
    val id = text(attribute(parser, "shortcutId"))
    val enabled = attribute(parser, "enabled")?.equals("false", ignoreCase = true) != true
    val label =
      text(attribute(parser, "shortcutShortLabel"))?.takeIf { it.isNotBlank() }
        ?: text(attribute(parser, "shortcutLongLabel"))
    val intents = mutableListOf<Intent>()
    val depth = parser.depth
    var type = parser.next()
    while (type != XmlPullParser.END_DOCUMENT &&
      !(type == XmlPullParser.END_TAG && parser.depth <= depth)) {
      if (type == XmlPullParser.START_TAG && parser.depth == depth + 1) {
        if (parser.name == "intent") {
          readIntent(parser)?.let { intents += it }
        } else {
          skip(parser)
        }
      }
      type = parser.next()
    }
    if (!enabled || id.isNullOrEmpty() || label.isNullOrBlank() || intents.isEmpty()) {
      return null
    }
    return Shortcut(id, label.trim(), intents)
  }

  /** Moves the parser from a start tag to its end tag. */
  @JvmStatic
  fun skip(parser: XmlPullParser) {
    val depth = parser.depth
    var type = parser.next()
    while (type != XmlPullParser.END_DOCUMENT &&
      !(type == XmlPullParser.END_TAG && parser.depth <= depth)) {
      type = parser.next()
    }
  }

  private fun attribute(parser: XmlPullParser, name: String): String? =
    parser.getAttributeValue(ANDROID_NAMESPACE, name)

  /**
   * Returns the resource ID of a resource reference as a compiled XML file gives it, such as
   * "@2131886080", or null for other values.
   */
  @JvmStatic
  fun resourceId(value: String?): Int? =
    if (value != null && value.startsWith("@")) value.substring(1).toIntOrNull()?.takeIf { it != 0 }
    else null
}
