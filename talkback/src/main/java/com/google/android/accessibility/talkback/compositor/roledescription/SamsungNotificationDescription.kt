/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.compositor.roledescription

import android.content.Context
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.compositor.AccessibilityNodeFeedbackUtils
import com.google.android.accessibility.talkback.compositor.GlobalVariables
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.FormFactorUtils

/** Supplements Samsung's labelled notification card with the message its label omits. */
object SamsungNotificationDescription {
  internal const val PACKAGE = "com.samsung.android.wearable.sysui"
  internal const val CARD_ID = "$PACKAGE:id/chunk_view"
  internal const val BODY_ID = "$PACKAGE:id/single_body_text"
  internal const val TIME_ID = "$PACKAGE:id/timestamp"

  @JvmStatic
  fun isNotificationCard(node: AccessibilityNodeInfoCompat): Boolean =
    isNotificationCard(node.packageName?.toString(), node.viewIdResourceName,
      FormFactorUtils.isAndroidWear())

  internal fun isNotificationCard(packageName: String?, resourceId: String?, isWear: Boolean) =
    isWear && packageName == PACKAGE && resourceId == CARD_ID

  /** A lazy tree view so extraction can be tested without Android accessibility objects. */
  internal interface Node {
    val packageName: String?
    val resourceId: String?
    val text: CharSequence?
    val visible: Boolean
    val focusable: Boolean
    val children: Sequence<Node>
    fun speech(): CharSequence
  }

  @JvmStatic
  fun describeCard(
    node: AccessibilityNodeInfoCompat,
    announcement: CharSequence,
    shouldIterateChildren: Boolean,
    context: Context,
    globalVariables: GlobalVariables,
  ): List<CharSequence>? {
    // Avoid accessing child nodes at all on phones and unrelated watch screens.
    if (!shouldIterateChildren || !isNotificationCard(node)) return null

    class AndroidNode(private val value: AccessibilityNodeInfoCompat) : Node {
      override val packageName get() = value.packageName?.toString()
      override val resourceId get() = value.viewIdResourceName
      override val text get() = value.text
      override val visible get() = AccessibilityNodeInfoUtils.isVisible(value)
      override val focusable get() = AccessibilityNodeInfoUtils.isAccessibilityFocusable(value)
      override val children get() = (0 until value.childCount).asSequence()
        .mapNotNull { value.getChild(it) }.map { AndroidNode(it) }
      override fun speech() =
        AccessibilityNodeFeedbackUtils.getNodeTextDescription(value, context, globalVariables)
    }

    return describeCard(AndroidNode(node), announcement, true, true)
  }

  internal fun describeCard(
    card: Node,
    announcement: CharSequence,
    isWear: Boolean,
    shouldIterateChildren: Boolean,
  ): List<CharSequence>? {
    if (!isNotificationCard(card.packageName, card.resourceId, isWear) ||
      !shouldIterateChildren || normalize(announcement).isEmpty()
    ) return null

    val bodies = collectBodies(card, announcement, isWear, shouldIterateChildren)
    val timestamp = readableDescendants(card).firstOrNull {
      it.resourceId == TIME_ID && normalize(it.text ?: "").isNotEmpty()
    }
    if (timestamp == null) return listOf(announcement) + bodies

    // Match the actual timestamp node, including localized time and Unicode spacing.
    // Only remove a trailing time: a matching time mentioned in the title/message stays intact.
    val rawTime = normalize(timestamp.text ?: "")
    val timePattern = rawTime.split(' ').joinToString("[\\s\\p{Z}]+") { Regex.escape(it) }
    val suffix = Regex("(?:[,;][\\s\\p{Z}]*|[\\s\\p{Z}]+)$timePattern[\\s\\p{Z}]*$")
      .find(announcement)
    if (suffix == null && normalize(announcement).contains(rawTime)) {
      return listOf(announcement) + bodies
    }
    val header = if (suffix == null) announcement else
      announcement.subSequence(0, suffix.range.first).trimEnd()
    val timeSpeech = timestamp.speech()
    if (normalize(timeSpeech).isEmpty()) return listOf(announcement) + bodies
    return listOf(header) + bodies + timeSpeech
  }

  internal fun collectBodies(
    card: Node,
    announcement: CharSequence,
    isWear: Boolean,
    shouldIterateChildren: Boolean,
  ): List<CharSequence> {
    if (!isNotificationCard(card.packageName, card.resourceId, isWear) ||
      !shouldIterateChildren || normalize(announcement).isEmpty()
    ) return emptyList()

    val bodies = mutableListOf<CharSequence>()
    val spoken = mutableListOf(normalize(announcement))
    readableDescendants(card).forEach { node ->
      if (node.resourceId == BODY_ID) {
        val body = normalize(node.text ?: "")
        if (body.isNotEmpty() && spoken.none { it.contains(body) }) {
          val speech = node.speech()
          if (normalize(speech).isNotEmpty()) {
            bodies.add(speech)
            spoken.add(body)
          }
        }
      }
    }
    return bodies
  }

  private fun readableDescendants(card: Node): Sequence<Node> {
    fun visit(node: Node, depth: Int): Sequence<Node> = sequence {
      // Never cross into another card, hidden subtree, or independently navigable control.
      if (depth > 32 || node.packageName != PACKAGE || node.resourceId == CARD_ID ||
        !node.visible || node.focusable
      ) return@sequence
      yield(node)
      if (node.resourceId != BODY_ID && node.resourceId != TIME_ID) {
        node.children.forEach { yieldAll(visit(it, depth + 1)) }
      }
    }
    return card.children.flatMap { visit(it, 0) }
  }

  private fun normalize(text: CharSequence): String =
    text.toString().replace(Regex("[\\s\\p{Z}]+"), " ").trim()
}
