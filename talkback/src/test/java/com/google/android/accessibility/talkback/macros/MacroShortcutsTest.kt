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
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import com.google.android.accessibility.talkback.macros.Macros.Macro
import com.google.android.accessibility.talkback.macros.Macros.Wait
import com.google.android.accessibility.utils.FormFactorUtils
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroShortcutsTest {
  private lateinit var context: Context
  private lateinit var prefs: SharedPreferences

  private val token = "0123456789abcdef0123456789abcdef"

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    prefs = context.getSharedPreferences("macro_shortcuts_test", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  @Test
  fun newTokensAreRandomTokens() {
    val a = MacroShortcuts.newToken()
    val b = MacroShortcuts.newToken()
    assertTrue(MacroShortcuts.isToken(a))
    assertTrue(MacroShortcuts.isToken(b))
    assertNotEquals(a, b)
    assertFalse(MacroShortcuts.isToken(""))
    assertFalse(MacroShortcuts.isToken(null))
    assertFalse(MacroShortcuts.isToken("0123"))
    assertFalse(MacroShortcuts.isToken(token.uppercase()))
  }

  @Test
  fun verifiedNeedsTheMacrosToken() {
    val macro = Macro("a", "Lights", listOf(Wait(1_000)), shortcutToken = token)
    val other = Macro("b", "No shortcut", emptyList())
    val macros = listOf(macro, other)
    assertEquals(macro, MacroShortcuts.verified(macros, "a", token))
    assertNull(MacroShortcuts.verified(macros, "a", MacroShortcuts.newToken()))
    assertNull(MacroShortcuts.verified(macros, "a", null))
    assertNull(MacroShortcuts.verified(macros, "a", ""))
    assertNull(MacroShortcuts.verified(macros, null, token))
    assertNull(MacroShortcuts.verified(macros, "c", token))
    assertNull(MacroShortcuts.verified(macros, "b", token))
    assertNull(MacroShortcuts.verified(listOf(macro.copy(id = "script:x:y")), "script:x:y", token))
  }

  @Test
  fun intentRunsTheMacroInSandFly() {
    val intent = MacroShortcuts.intent(context, "a", token)
    assertEquals(MacroShortcuts.ACTION_RUN, intent.action)
    assertEquals(context.packageName, intent.component?.packageName)
    assertEquals(MacroShortcutActivity::class.java.name, intent.component?.className)
    assertEquals("a", intent.getStringExtra(MacroShortcuts.EXTRA_MACRO))
    assertEquals(token, intent.getStringExtra(MacroShortcuts.EXTRA_TOKEN))
  }

  @Test
  fun tokenIsSavedButNotInFiles() {
    val macro = Macro("a", "Lights", listOf(Wait(1_000)), shortcutToken = token)
    assertEquals(listOf(macro), Macros.fromJson(Macros.toJson(listOf(macro))))
    val file = Macros.toJson(listOf(macro), shortcuts = false)
    assertFalse(JSONArray(file).getJSONObject(0).has("shortcut"))
    assertNull(Macros.macroFromJson(Macros.macroToJson(macro))!!.shortcutToken)
    val bad = JSONArray(Macros.toJson(listOf(macro))).apply { getJSONObject(0).put("shortcut", "x") }
    assertNull(Macros.fromJson(bad.toString()).single().shortcutToken)
  }

  @Test
  fun requestGivesTheMacroATokenAndPinsItsShortcut() {
    val shortcuts = shadowOf(context.getSystemService(ShortcutManager::class.java))
    shortcuts.setIsRequestPinShortcutSupported(true)
    val macro = Macros.add(prefs, "Lights")
    assertEquals(MacroShortcuts.Result.REQUESTED, MacroShortcuts.request(context, prefs, macro.id))
    val saved = Macros.find(prefs, macro.id)!!.shortcutToken
    assertNotNull(saved)
    val pinned = context.getSystemService(ShortcutManager::class.java).pinnedShortcuts.single()
    assertEquals(MacroShortcuts.shortcutId(macro.id), pinned.id)
    assertEquals("Lights", pinned.shortLabel)
    assertEquals(saved, pinned.intent!!.getStringExtra(MacroShortcuts.EXTRA_TOKEN))

    // A second request keeps the token, so the shortcut added first still runs the macro.
    assertEquals(MacroShortcuts.Result.ALREADY_ADDED, MacroShortcuts.request(context, prefs, macro.id))
    assertEquals(saved, Macros.find(prefs, macro.id)!!.shortcutToken)
  }

  @Test
  fun requestSaysWhenTheLauncherCantPin() {
    shadowOf(context.getSystemService(ShortcutManager::class.java)).setIsRequestPinShortcutSupported(false)
    val macro = Macros.add(prefs, "Lights")
    assertEquals(MacroShortcuts.Result.UNSUPPORTED, MacroShortcuts.request(context, prefs, macro.id))
    assertNull(Macros.find(prefs, macro.id)!!.shortcutToken)
  }

  @Test
  fun deletingTheMacroDisablesItsShortcut() {
    val manager = context.getSystemService(ShortcutManager::class.java)
    shadowOf(manager).setIsRequestPinShortcutSupported(true)
    val macro = Macros.add(prefs, "Lights")
    MacroShortcuts.request(context, prefs, macro.id)
    val pinned = manager.pinnedShortcuts.single()
    manager.createShortcutResultIntent(pinned)
    Macros.delete(context, prefs, macro.id)
    // Robolectric keeps disabled shortcuts apart and refuses result intents for them.
    assertThrows(IllegalArgumentException::class.java) { manager.createShortcutResultIntent(pinned) }
  }
}
