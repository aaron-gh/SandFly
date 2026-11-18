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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomActionMenuMacrosTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val menu = CustomActionMenu(null, null)

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    prefs.edit().clear().commit()
    Macros.write(
      prefs,
      listOf(
        Macros.Macro("a", "Reply", emptyList(), app = "com.whatsapp", inActions = true),
        Macros.Macro("b", "Not in Actions", emptyList(), app = "com.whatsapp"),
        Macros.Macro("c", "Everywhere", emptyList(), inActions = true),
        Macros.Macro("d", "YouTube", emptyList(), app = "com.google.android.youtube", inActions = true),
      ),
    )
  }

  private fun node(packageName: String): AccessibilityNodeInfoCompat =
    @Suppress("DEPRECATION") AccessibilityNodeInfoCompat.obtain().apply { this.packageName = packageName }

  @Test
  fun macrosOfTheAppAndOfAllAppsComeAfterTheAppsActions() {
    val node = node("com.whatsapp")
    node.addAction(AccessibilityActionCompat(0x7f0a0001, "Star"))
    assertTrue(menu.accept(context, node))
    val items = menu.getMenuItemsForNode(context, node, /* includeAncestors= */ false)
    assertEquals(listOf("Star", "Reply", "Everywhere"), items.map { it.title.toString() })
    assertEquals(listOf(false, true, true), items.map { CustomActionMenu.isMacroItem(it) })
  }

  @Test
  fun anItemWithoutActionsListsTheMacros() {
    val node = node("com.google.android.youtube")
    assertTrue(menu.accept(context, node))
    assertEquals(
      listOf("Everywhere", "YouTube"),
      menu.getMenuItemsForNode(context, node, false).map { it.title.toString() },
    )
  }

  @Test
  fun withoutMacrosInActionsNothingIsListed() {
    Macros.write(prefs, listOf(Macros.Macro("b", "Not in Actions", emptyList(), app = "com.whatsapp")))
    val node = node("com.whatsapp")
    assertFalse(menu.accept(context, node))
    assertTrue(menu.getMenuItemsForNode(context, node, false).isEmpty())
  }
}
