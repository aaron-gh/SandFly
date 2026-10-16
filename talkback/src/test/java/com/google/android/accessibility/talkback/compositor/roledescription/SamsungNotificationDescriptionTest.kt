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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungNotificationDescriptionTest {
  private data class FakeNode(
    override val packageName: String? = SamsungNotificationDescription.PACKAGE,
    override val resourceId: String? = null,
    override val text: CharSequence? = null,
    override val visible: Boolean = true,
    override val focusable: Boolean = false,
    val nodes: List<FakeNode> = emptyList(),
    val processedText: CharSequence? = text,
  ) : SamsungNotificationDescription.Node {
    override val children get() = nodes.asSequence()
    override fun speech() = processedText ?: ""
  }

  private fun body(text: String) =
    FakeNode(resourceId = SamsungNotificationDescription.BODY_ID, text = text)

  private fun card(vararg nodes: FakeNode) =
    FakeNode(resourceId = SamsungNotificationDescription.CARD_ID, focusable = true,
      nodes = nodes.toList())

  private fun collect(
    node: FakeNode,
    announcement: String = "WhatsApp, Group, 9:11 p.m.",
    wear: Boolean = true,
    iterate: Boolean = true,
  ) = SamsungNotificationDescription.collectBodies(node, announcement, wear, iterate)
    .map { it.toString() }

  @Test fun addsOnlyBodyAndPreservesProcessing() {
    val message = body("Hello 😁").copy(processedText = "Hello beaming face")
    val title = FakeNode(resourceId = "${SamsungNotificationDescription.PACKAGE}:id/title",
      text = "Group")
    val time = FakeNode(resourceId = "${SamsungNotificationDescription.PACKAGE}:id/timestamp",
      text = "9:11 p.m.")
    val action = FakeNode(text = "Reply", focusable = true)
    assertEquals(listOf("Hello beaming face"), collect(card(title, time, message, action)))
  }

  @Test fun skipsBodyAlreadyInAnnouncementWithUnicodeWhitespace() {
    assertTrue(collect(card(body("Hello\u00a0  world")), "Group, Hello world").isEmpty())
  }

  @Test fun skipsRepeatedBodiesWithoutChangingTheirWording() {
    assertEquals(listOf(" Hello\u00a0world "),
      collect(card(body(" Hello\u00a0world "), body("Hello world"))))
  }

  @Test fun skipsEmptyMissingAndUnavailableBody() {
    assertTrue(collect(card(body(" \n\u00a0"),
      FakeNode(resourceId = SamsungNotificationDescription.BODY_ID))).isEmpty())
    assertTrue(collect(card()).isEmpty())
  }

  @Test fun readsNestedBodiesInTreeOrder() {
    assertEquals(listOf("First", "Second"),
      collect(card(FakeNode(nodes = listOf(body("First"))), body("Second"))))
  }

  @Test fun skipsHiddenBodyAndHiddenAncestor() {
    assertTrue(collect(card(body("Hidden").copy(visible = false),
      FakeNode(visible = false, nodes = listOf(body("Also hidden"))))).isEmpty())
  }

  @Test fun skipsFocusableBodyAndFocusableAncestor() {
    assertTrue(collect(card(body("Separate").copy(focusable = true),
      FakeNode(focusable = true, nodes = listOf(body("Separate child"))))).isEmpty())
  }

  @Test fun neverReadsNeighboringOrNestedCards() {
    val first = card(body("Current"), card(body("Nested other card")))
    val second = card(body("Neighbor"))
    assertEquals(listOf("Current"), collect(first))
    assertTrue(collect(FakeNode(nodes = listOf(first, second))).isEmpty())
  }

  @Test fun rejectsOtherPackagesAndCardIds() {
    assertTrue(collect(card(body("Message")).copy(packageName = "another.app")).isEmpty())
    assertTrue(collect(card(body("Message")).copy(resourceId = "android:id/content")).isEmpty())
    assertTrue(collect(card(body("Message").copy(packageName = "another.app"))).isEmpty())
  }

  @Test fun phoneAndDisabledChildIterationDoNotReadChildren() {
    val unreadable = object : SamsungNotificationDescription.Node {
      override val packageName = SamsungNotificationDescription.PACKAGE
      override val resourceId = SamsungNotificationDescription.CARD_ID
      override val text = ""
      override val visible = true
      override val focusable = true
      override val children: Sequence<SamsungNotificationDescription.Node>
        get() = error("Must not access children")
      override fun speech() = ""
    }
    assertTrue(SamsungNotificationDescription.collectBodies(unreadable, "Group", false, true)
      .isEmpty())
    assertTrue(SamsungNotificationDescription.collectBodies(unreadable, "Group", true, false)
      .isEmpty())
  }

  @Test fun unlabelledCardsRetainNormalAggregation() {
    assertTrue(collect(card(body("Message")), "").isEmpty())
  }

  private fun time(text: String) =
    FakeNode(resourceId = SamsungNotificationDescription.TIME_ID, text = text)

  private fun describe(node: FakeNode, announcement: String = "WhatsApp, Group, 9:11 p.m.") =
    SamsungNotificationDescription.describeCard(node, announcement, true, true)
      ?.map { it.toString() }

  @Test fun readsHeaderThenBodyThenTimeOnce() {
    val timestamp = time("9:11\u202fp.m.").copy(processedText = "9:11 p.m.")
    assertEquals(listOf("WhatsApp, Group", "Message", "9:11 p.m."),
      describe(card(timestamp, body("Message")), "WhatsApp, Group , 9:11\u202fp.m."))
  }

  @Test fun handlesLocalizedTimeAndKeepsTimesInsideTitleAndBody() {
    assertEquals(listOf("WhatsApp, Meet at 21:11", "I arrive at 21:11", "21:11"),
      describe(card(time("21:11"), body("I arrive at 21:11")),
        "WhatsApp, Meet at 21:11, 21:11"))
  }

  @Test fun appendsTimeAfterBodyWhenLabelDoesNotContainIt() {
    assertEquals(listOf("WhatsApp, Group", "Message", "Yesterday"),
      describe(card(time("Yesterday"), body("Message")), "WhatsApp, Group"))
  }

  @Test fun missingOrHiddenTimeKeepsLabelAndBody() {
    assertEquals(listOf("WhatsApp, Group, 9:11 p.m.", "Message"),
      describe(card(body("Message"))))
    assertEquals(listOf("WhatsApp, Group", "Message"),
      describe(card(time("9:11 p.m.").copy(visible = false), body("Message")),
        "WhatsApp, Group"))
  }

  @Test fun doesNotDuplicateBodyAlreadyIncludedBeforeTime() {
    assertEquals(listOf("WhatsApp, Group, Message", "9:11 p.m."),
      describe(card(time("9:11 p.m."), body("Message")),
        "WhatsApp, Group, Message, 9:11 p.m."))
  }

  @Test fun missingBodyStillReadsTimeOnce() {
    assertEquals(listOf("WhatsApp, Group", "9:11 p.m."),
      describe(card(time("9:11 p.m."))))
  }

  @Test fun reorderedDescriptionIsOnlyForSupportedWearCards() {
    val node = card(time("9:11 p.m."), body("Message"))
    assertEquals(null, SamsungNotificationDescription.describeCard(node, "Group", false, true))
    assertEquals(null, SamsungNotificationDescription.describeCard(node, "Group", true, false))
    assertEquals(null, describe(node.copy(packageName = "other.app")))
    assertEquals(null, describe(node.copy(resourceId = "android:id/content")))
  }

  @Test fun fontAnnouncementExemptionIsOnlyForSamsungWearNotificationCard() {
    assertTrue(SamsungNotificationDescription.isNotificationCard(
      SamsungNotificationDescription.PACKAGE, SamsungNotificationDescription.CARD_ID, true))
    assertEquals(false, SamsungNotificationDescription.isNotificationCard(
      SamsungNotificationDescription.PACKAGE, SamsungNotificationDescription.CARD_ID, false))
    assertEquals(false, SamsungNotificationDescription.isNotificationCard(
      SamsungNotificationDescription.PACKAGE, SamsungNotificationDescription.BODY_ID, true))
    assertEquals(false, SamsungNotificationDescription.isNotificationCard(
      "other.app", SamsungNotificationDescription.CARD_ID, true))
  }
}
