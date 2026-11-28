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

package com.google.android.accessibility.talkback.menurules

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.actor.DirectionNavigationActor
import com.google.android.accessibility.talkback.actor.TextEditActor
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.input.TextCursorTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Verifies RuleAction.hasMenuItemsForNode matches getMenuItemsForNode across node types. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RuleActionEditBoxTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private lateinit var ruleAction: RuleAction

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    val focusMonitor = AccessibilityFocusMonitor(null, null, null)
    val directionNavigation =
      DirectionNavigationActor(null, null, null, null, null, null, focusMonitor, null, null, null)
    val textEditActor = TextEditActor(null, null, TextCursorTracker(), null, null, null)
    val actorState =
      ActorState(
        ActorStateWritable(
          null,
          null,
          null,
          null,
          null,
          directionNavigation.state,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          textEditActor.state,
        )
      )
    ruleAction = RuleAction(null, actorState, focusMonitor, null)
  }

  private fun assertHasItemsMatchesGetItems(
    node: AccessibilityNodeInfoCompat,
    includeAncestors: Boolean,
  ): Boolean {
    val hasItems = ruleAction.hasMenuItemsForNode(context, node, includeAncestors)
    val gotItems = ruleAction.getMenuItemsForNode(context, node, includeAncestors).isNotEmpty()
    assertEquals(gotItems, hasItems)
    return hasItems
  }

  @Test
  fun editBoxMatchesGetMenuItemsForNode() {
    @Suppress("DEPRECATION") val editBox = AccessibilityNodeInfoCompat.obtain()
    editBox.className = EditText::class.java.name
    editBox.isEditable = true
    editBox.text = "helo wrld"

    assertTrue(assertHasItemsMatchesGetItems(editBox, includeAncestors = true))
    assertTrue(assertHasItemsMatchesGetItems(editBox, includeAncestors = false))
  }

  @Test
  fun selectableTextMatchesGetMenuItemsForNode() {
    @Suppress("DEPRECATION") val textView = AccessibilityNodeInfoCompat.obtain()
    textView.className = TextView::class.java.name
    textView.isEditable = false
    textView.unwrap().isTextSelectable = true
    textView.text = "Selectable text"
    textView.addAction(AccessibilityNodeInfoCompat.ACTION_COPY)

    assertTrue(assertHasItemsMatchesGetItems(textView, includeAncestors = true))
    assertTrue(assertHasItemsMatchesGetItems(textView, includeAncestors = false))
  }

  @Test
  fun customActionNodeAndAncestorMatchGetMenuItemsForNode() {
    @Suppress("DEPRECATION") val row = AccessibilityNodeInfoCompat.obtain()
    row.addAction(AccessibilityActionCompat(0x7f0a0001, "Archive"))
    @Suppress("DEPRECATION") val textView = AccessibilityNodeInfoCompat.obtain()
    textView.className = TextView::class.java.name
    textView.addAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)
    shadowOf(row.unwrap()).addChild(textView.unwrap())

    assertTrue(assertHasItemsMatchesGetItems(row, includeAncestors = true))
    assertTrue(assertHasItemsMatchesGetItems(row, includeAncestors = false))
    assertTrue(assertHasItemsMatchesGetItems(textView, includeAncestors = true))
    assertFalse(assertHasItemsMatchesGetItems(textView, includeAncestors = false))
  }

  @Test
  fun plainTextMatchesGetMenuItemsForNode() {
    @Suppress("DEPRECATION") val textView = AccessibilityNodeInfoCompat.obtain()
    textView.className = TextView::class.java.name
    textView.text = "Hello"

    assertFalse(assertHasItemsMatchesGetItems(textView, includeAncestors = true))
    assertFalse(assertHasItemsMatchesGetItems(textView, includeAncestors = false))
  }
}
