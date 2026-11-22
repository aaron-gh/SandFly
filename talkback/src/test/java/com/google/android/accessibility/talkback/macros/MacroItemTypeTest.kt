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

package com.google.android.accessibility.talkback.macros

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.macros.Macros.ActOnItem
import com.google.android.accessibility.talkback.macros.Macros.FocusItem
import com.google.android.accessibility.talkback.macros.Macros.Kind
import com.google.android.accessibility.talkback.macros.Macros.Macro
import com.google.android.accessibility.talkback.macros.Macros.SpeakItem
import com.google.android.accessibility.talkback.macros.Macros.Tap
import com.google.android.accessibility.talkback.macros.Macros.TouchHold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The item type of macro steps that find an item: saved, described and matched. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroItemTypeTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
  }

  @Test
  fun itemTypeSurvivesJson() {
    val macro =
      Macro(
        "a",
        "Types",
        listOf(
          Tap("Send", kind = Kind.CLICKABLE),
          Tap("^Re", 5_000, regex = true, field = Macros.Field.TEXT, kind = Kind.HEADING),
          TouchHold("Record", kind = Kind.LONG_CLICKABLE),
          FocusItem("Inbox", kind = Kind.HEADING),
          SpeakItem("Volume", kind = Kind.ADJUSTABLE),
          ActOnItem("Messages", Macros.ItemAct.SCROLL_FORWARD, kind = Kind.SCROLLABLE),
          Tap("Any"),
        ),
      )
    assertEquals(listOf(macro), Macros.fromJson(Macros.toJson(listOf(macro))))
  }

  @Test
  fun anyTypeIsNotSaved() {
    val steps = Macros.toJson(listOf(Macro("a", "Any", listOf(Tap("Send")))))
    assertFalse(steps, steps.contains("\"kind\""))
  }

  @Test
  fun unknownTypeReadsAsAny() {
    val json =
      """[{"id":"a","name":"Old","steps":[{"type":"tap","label":"Send","kind":"button"}]}]"""
    assertEquals(listOf(Tap("Send")), Macros.fromJson(json).single().steps)
  }

  @Test
  fun scrollOfLargestListHasNoType() {
    val json =
      """[{"id":"a","name":"Scroll","steps":[{"type":"act_item","act":"scroll_forward","label":"","kind":"heading"}]}]"""
    assertEquals(listOf(ActOnItem("", Macros.ItemAct.SCROLL_FORWARD)), Macros.fromJson(json).single().steps)
  }

  @Test
  fun describeItemType() {
    assertEquals("Tap Send, Clickable", Macros.describe(context, Tap("Send", kind = Kind.CLICKABLE)))
    assertEquals(
      "Focus send (Resource ID), Heading",
      Macros.describe(context, FocusItem("send", field = Macros.Field.RESOURCE_ID, kind = Kind.HEADING)),
    )
    assertEquals("Tap Send", Macros.describe(context, Tap("Send")))
  }

  @Test
  fun itemTypeChoosesBetweenItemsWithTheSameText() {
    val heading = node("Send").also { AccessibilityNodeInfoCompat.wrap(it).isHeading = true }
    val button = node("Send").also { it.isClickable = true }
    // Items as roots of their own: Robolectric's copies of child nodes don't keep their parents.
    val roots = listOf(heading, button)

    // Without a type, the first item with the text, as before.
    assertSame(heading, MacroRunner.stepItemIn(roots, "Send"))
    assertSame(button, MacroRunner.stepItemIn(roots, "Send", kind = Kind.CLICKABLE))
    assertSame(heading, MacroRunner.stepItemIn(roots, "Send", kind = Kind.HEADING))
    assertNull(MacroRunner.stepItemIn(roots, "Send", kind = Kind.EDITABLE))
  }

  @Test
  fun itemTypeKeepsWholeTextFirst() {
    val partialButton = node("Send message").also { it.isClickable = true }
    val heading = node("Send").also { AccessibilityNodeInfoCompat.wrap(it).isHeading = true }
    val button = node("Send").also { it.isClickable = true }
    val roots = listOf(partialButton, heading, button)

    assertSame(button, MacroRunner.stepItemIn(roots, "Send", kind = Kind.CLICKABLE))
    assertSame(partialButton, MacroRunner.stepItemIn(roots, "Send mess", kind = Kind.CLICKABLE))
    assertNull(MacroRunner.stepItemIn(roots, "Send mess", kind = Kind.HEADING))
  }

  private fun node(text: String?): AccessibilityNodeInfo =
    AccessibilityNodeInfo.obtain().apply {
      this.text = text
      isVisibleToUser = true
    }
}
