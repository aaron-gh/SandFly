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

package com.google.android.accessibility.talkback.directtouch

import android.content.res.XmlResourceParser
import android.graphics.Rect
import org.xmlpull.v1.XmlPullParser

/** Where an app in front gets direct touch. */
sealed interface DirectTouchTarget {
  /** No direct touch. */
  data object None : DirectTouchTarget

  /** The whole screen, except system windows and the keyboard. */
  data object Screen : DirectTouchTarget

  /**
   * Parts of the app's window: [screenAreas] in screen pixels, [windowAreas] measured from the
   * window's top left corner, and the views with the resource IDs in [viewIds], wherever they are.
   */
  data class Areas(
    val screenAreas: List<Rect> = emptyList(),
    val windowAreas: List<WindowArea> = emptyList(),
    val viewIds: List<String> = emptyList(),
  ) : DirectTouchTarget
}

/**
 * One element of a declaration: direct touch on the whole screen, or on an area, in [activity]
 * (any when null) and [state] (any when null). An area is the view [viewId], or else [windowArea].
 */
data class DirectTouchRule(
  val activity: String?,
  val state: String?,
  val screen: Boolean,
  val viewId: String? = null,
  val windowArea: WindowArea? = null,
)

/**
 * What an app declares in the XML resource that its manifest names with
 * [DirectTouchDeclaration.META_DATA_KEY]:
 * ```
 * <direct-touch>
 *     <screen activity=".GameActivity" />
 *     <area activity=".MainActivity" state="playing" view="@id/board" />
 *     <area activity=".MainActivity" state="playing" top="50%" />
 *     <area state="playing" bottom="100%" height="200dp" />
 * </direct-touch>
 * ```
 */
class DirectTouchDeclaration(val rules: List<DirectTouchRule>) {

  /** Whether any rule names an activity, so that SandFly has to follow the app's activities. */
  val namesActivities: Boolean = rules.any { it.activity != null }

  /** What the rules give in [activity] and [state]. A matching screen rule wins over areas. */
  fun target(activity: String?, state: String?): DirectTouchTarget {
    val matching =
      rules.filter {
        (it.activity == null || it.activity == activity) && (it.state == null || it.state == state)
      }
    return when {
      matching.isEmpty() -> DirectTouchTarget.None
      matching.any { it.screen } -> DirectTouchTarget.Screen
      else ->
        DirectTouchTarget.Areas(
          windowAreas = matching.mapNotNull { it.windowArea },
          viewIds = matching.mapNotNull { it.viewId },
        )
    }
  }

  companion object {
    /** Application meta-data that names the declaration's XML resource. */
    const val META_DATA_KEY = "io.github.aaron_gh.sandfly.direct_touch"

    /** The deprecated key, read when an app has no [META_DATA_KEY]; see DeprecatedApiNames. */
    const val LEGACY_META_DATA_KEY = "fyi.quin.backtalk.direct_touch"

    /** Rules after this many are ignored. */
    const val MAX_RULES = 64

    private const val MAX_NAME_LENGTH = 256

    private val AREA_LENGTHS = listOf("left", "top", "right", "bottom", "width", "height")

    /**
     * Reads a declaration of [packageName] from [parser]. [resourceName] names a resource ID of the
     * app, such as "com.example.game:id/board". A malformed file gives the rules read before the
     * error, and an element SandFly doesn't know is skipped.
     */
    fun parse(
      parser: XmlPullParser,
      packageName: String,
      resourceName: (Int) -> String?,
    ): DirectTouchDeclaration {
      val rules = mutableListOf<DirectTouchRule>()
      try {
        var type = parser.next()
        while (type != XmlPullParser.END_DOCUMENT && rules.size < MAX_RULES) {
          if (type == XmlPullParser.START_TAG && parser.depth == 2) {
            readRule(parser, packageName, resourceName)?.let { rules += it }
          }
          type = parser.next()
        }
      } catch (_: Exception) {
        // Keep the rules read so far.
      }
      return DirectTouchDeclaration(rules)
    }

    private fun readRule(
      parser: XmlPullParser,
      packageName: String,
      resourceName: (Int) -> String?,
    ): DirectTouchRule? {
      // "*" is any activity, as is no activity attribute.
      val activity =
        attribute(parser, "activity")?.takeUnless { it == "*" }?.let { activityName(packageName, it) }
      val state = attribute(parser, "state")
      return when (parser.name) {
        "screen" -> DirectTouchRule(activity, state, screen = true)
        "area" -> {
          val view = viewId(parser, packageName, resourceName)
          if (view != null) {
            return DirectTouchRule(activity, state, screen = false, viewId = view)
          }
          // A length that is there but isn't one skips the rule; one left out is the window's own.
          val lengths =
            AREA_LENGTHS.associateWith { name ->
              attribute(parser, name)?.let { AreaLength.parse(it) ?: return null }
            }
          val area =
            WindowArea(
              left = lengths["left"],
              top = lengths["top"],
              right = lengths["right"],
              bottom = lengths["bottom"],
              width = lengths["width"],
              height = lengths["height"],
            )
          if (area.isEmpty) null else DirectTouchRule(activity, state, screen = false, windowArea = area)
        }
        else -> null
      }
    }

    private fun attribute(parser: XmlPullParser, name: String): String? =
      parser
        .getAttributeValue(null, name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_LENGTH }

    /** A class name as the manifest writes it: ".Game" and "Game" are in [packageName]. */
    fun activityName(packageName: String, name: String): String =
      when {
        name.startsWith(".") -> packageName + name
        '.' !in name -> "$packageName.$name"
        else -> name
      }

    /**
     * The view attribute as a full resource ID name. The file can hold a reference, "@id/board",
     * which the build turns into a number, or a name: "board", or "com.example.game:id/board".
     */
    private fun viewId(
      parser: XmlPullParser,
      packageName: String,
      resourceName: (Int) -> String?,
    ): String? {
      if (parser is XmlResourceParser) {
        val id = parser.getAttributeResourceValue(null, "view", 0)
        if (id != 0) {
          return resourceName(id)
        }
      }
      return attribute(parser, "view")?.let { fullViewId(packageName, it) }
    }

    /** [id] as Android names a view's resource ID, "package:id/name". */
    fun fullViewId(packageName: String, id: String): String =
      when {
        ":id/" in id -> id
        id.startsWith("@+id/") -> "$packageName:id/${id.removePrefix("@+id/")}"
        id.startsWith("@id/") -> "$packageName:id/${id.removePrefix("@id/")}"
        else -> "$packageName:id/$id"
      }
  }
}
