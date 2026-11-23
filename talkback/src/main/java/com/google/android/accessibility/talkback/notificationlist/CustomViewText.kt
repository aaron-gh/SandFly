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

package com.google.android.accessibility.talkback.notificationlist

import android.content.Context
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * The text of a notification's custom layout, for notifications such as Samsung Weather's that put
 * their text only in the layout and none in the notification's text fields (#583).
 */
object CustomViewText {
  private const val TAG = "CustomViewText"

  /** Text views read from one layout. */
  private const val MAX_TEXTS = 12

  /** Views visited in one layout. */
  private const val MAX_VIEWS = 200

  /** Notifications whose layout text is kept, by key and post time. */
  private const val MAX_CACHED = 32

  private val cache =
    object : LinkedHashMap<String, String>(MAX_CACHED, 0.75f, /* accessOrder= */ true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
        size > MAX_CACHED
    }

  /**
   * Returns the text shown by the custom layout of [notification], one text view per line, leaving
   * out [skip] such as the title, or null when it has no custom layout or no other text.
   */
  fun of(context: Context, notification: StatusBarNotification, skip: Collection<CharSequence?>): CharSequence? {
    val posted = notification.notification ?: return null
    @Suppress("DEPRECATION")
    val views = posted.bigContentView ?: posted.contentView ?: posted.headsUpContentView ?: return null
    val cacheKey = "${notification.key}@${notification.postTime}"
    val texts =
      cache[cacheKey]
        ?: try {
            val parent = FrameLayout(context)
            val root = views.apply(context, parent)
            val found = ArrayList<String>()
            collect(root, found, intArrayOf(0))
            found.joinToString("\n")
          } catch (e: Throwable) {
            // Another app's layout: missing resources, unsupported views or running out of memory.
            LogUtils.w(TAG, "Custom layout of %s not read: %s", notification.packageName, e)
            ""
          }
          .also { cache[cacheKey] = it }
    val skipped = skip.mapNotNull { it?.toString()?.trim() }.toSet()
    val lines = texts.split('\n').filter { it.isNotEmpty() && it !in skipped }.distinct()
    return if (lines.isEmpty()) null else lines.joinToString("\n")
  }

  private fun collect(view: View, found: MutableList<String>, visited: IntArray) {
    if (found.size >= MAX_TEXTS || ++visited[0] > MAX_VIEWS || view.visibility != View.VISIBLE) {
      return
    }
    if (view is TextView) {
      val text = view.text?.toString()?.trim()
      if (!text.isNullOrEmpty()) {
        found.add(text.take(MAX_LINE_LENGTH))
      }
      return
    }
    if (view is ViewGroup) {
      for (i in 0 until view.childCount) {
        collect(view.getChildAt(i), found, visited)
      }
    }
  }

  private const val MAX_LINE_LENGTH = 500
}
