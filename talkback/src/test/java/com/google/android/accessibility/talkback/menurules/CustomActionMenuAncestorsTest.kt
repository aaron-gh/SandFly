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
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.scripting.NodeRule
import com.google.android.accessibility.talkback.scripting.RuleAspect
import com.google.android.accessibility.talkback.scripting.ScriptHost
import com.google.android.accessibility.talkback.scripting.ScriptItemAction
import com.google.android.accessibility.talkback.scripting.ScriptMenuItem
import com.google.android.accessibility.talkback.scripting.ScriptReadingControl
import com.google.android.accessibility.talkback.scripting.Scripts
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The search for an app's actions up the ancestors of the focused item. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomActionMenuAncestorsTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val menu = CustomActionMenu(null, null)
  private var scriptActions: List<ScriptItemAction> = emptyList()

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    Scripts.setHost(FakeHost())
  }

  @After
  fun tearDown() {
    Scripts.setHost(null)
  }

  /** A row with the app's actions, such as a list item, and a piece of text inside it. */
  private fun textInRow(): AccessibilityNodeInfoCompat {
    @Suppress("DEPRECATION") val row = AccessibilityNodeInfoCompat.obtain()
    row.packageName = PACKAGE
    row.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
    row.addAction(AccessibilityActionCompat(0x7f0a0001, "Archive"))
    row.addAction(AccessibilityActionCompat(0x7f0a0002, "Mark as read"))
    @Suppress("DEPRECATION") val text = AccessibilityNodeInfoCompat.obtain()
    text.packageName = PACKAGE
    text.addAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)
    shadowOf(row.unwrap()).addChild(text.unwrap())
    return text
  }

  private fun titles(node: AccessibilityNodeInfoCompat): List<String> =
    menu.getMenuItemsForNode(context, node, /* includeAncestors= */ true).map { it.title.toString() }

  @Test
  fun anItemWithoutActionsOfItsOwnListsThoseOfTheItemAroundIt() {
    assertEquals(listOf("Archive", "Mark as read"), titles(textInRow()))
  }

  @Test
  fun listsNamedAppActionsUsingAndroidResourceIds() {
    @Suppress("DEPRECATION") val row = AccessibilityNodeInfoCompat.obtain()
    row.packageName = PACKAGE
    row.addAction(AccessibilityActionCompat(android.R.id.background, "View threads"))
    row.addAction(AccessibilityActionCompat(android.R.id.checkbox, "Group members"))
    row.addAction(AccessibilityActionCompat(android.R.id.content, null))
    row.addAction(AccessibilityActionCompat(AccessibilityActionCompat.ACTION_SCROLL_UP.id, "Scroll"))
    row.addAction(AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CLICK, "Open"))
    row.addAction(AccessibilityActionCompat(0x7f0a0001, "Archive"))

    val entries = menu.getMenuItemsForNode(context, row, /* includeAncestors= */ true)

    assertEquals(
      listOf(
        android.R.id.background to "View threads",
        android.R.id.checkbox to "Group members",
        0x7f0a0001 to "Archive",
      ),
      entries.map { it.itemId to it.title.toString() },
    )
  }

  @Test
  fun aScriptActionPlacedFirstDoesNotHideTheActionsOfTheItemAroundIt() {
    scriptActions = listOf(ScriptItemAction("Script first", first = true) {})
    assertEquals(listOf("Script first", "Archive", "Mark as read"), titles(textInRow()))
  }

  @Test
  fun aScriptActionPlacedLastComesAfterTheActionsOfTheItemAroundIt() {
    scriptActions = listOf(ScriptItemAction("Script last") {})
    assertEquals(listOf("Archive", "Mark as read", "Script last"), titles(textInRow()))
  }

  private inner class FakeHost : ScriptHost {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun rewriteFocusSpeech(
      node: AccessibilityNodeInfoCompat,
      speech: CharSequence,
    ): CharSequence? = null

    override fun rewriteEventSpeech(event: AccessibilityEvent, speech: CharSequence): CharSequence? =
      null

    override fun filterSpeech(speech: CharSequence): CharSequence? = null

    override fun rule(node: AccessibilityNodeInfoCompat, aspect: RuleAspect): NodeRule? = null

    override fun isHidden(node: AccessibilityNodeInfoCompat): Boolean = false

    override fun isSilenced(node: AccessibilityNodeInfoCompat): Boolean = false

    override fun currentActivity(): String? = null

    override fun currentWindowTitle(): String? = null

    override fun onPausedChanged(paused: Boolean) {}

    override fun onGesture(name: String, fallback: Runnable): Boolean = false

    override fun onKeys(
      modifiers: Int,
      keyCode: Int,
      withBacktalkModifier: Boolean,
      repeated: Boolean,
      fallback: Runnable,
    ): Boolean = false

    override fun menuItems(): List<ScriptMenuItem> = emptyList()

    override fun readingControls(): List<ScriptReadingControl> = emptyList()

    override fun itemActions(node: AccessibilityNodeInfoCompat): List<ScriptItemAction> =
      scriptActions

    override fun toggleAll(): Boolean? = null

    override fun shutdown() {}
  }

  private companion object {
    const val PACKAGE = "com.google.android.gm"
  }
}
