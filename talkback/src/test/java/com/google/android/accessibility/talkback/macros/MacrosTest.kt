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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.CommandMenus
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.AppGestures
import com.google.android.accessibility.talkback.gesture.GestureProfiles
import com.google.android.accessibility.talkback.macros.Macros.Macro
import com.google.android.accessibility.talkback.macros.Macros.OpenApp
import com.google.android.accessibility.talkback.macros.Macros.RunCommand
import com.google.android.accessibility.talkback.macros.Macros.Tap
import com.google.android.accessibility.talkback.macros.Macros.TypeText
import com.google.android.accessibility.talkback.macros.Macros.Wait
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacrosTest {
  private lateinit var context: Context
  private lateinit var prefs: SharedPreferences

  private val gmail = "OPEN_APP:com.google.android.gm/.ConversationListActivityGmail"

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    prefs = context.getSharedPreferences("macros_test", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  private fun back() = context.getString(R.string.shortcut_value_back)

  private fun unassigned() = context.getString(R.string.shortcut_value_unassigned)

  @Test
  fun everyStepSurvivesJson() {
    val macro =
      Macro(
        "a",
        "Send message",
        listOf(
          RunCommand(back()),
          Wait(1_500),
          Tap("Send", 5_000),
          OpenApp(gmail),
          TypeText("On my way", replace = true),
          TypeText("Thanks"),
        ),
      )
    assertEquals(listOf(macro), Macros.fromJson(Macros.toJson(listOf(macro))))
  }

  @Test
  fun unreadableJsonGivesNoMacros() {
    assertEquals(emptyList<Macro>(), Macros.fromJson(null))
    assertEquals(emptyList<Macro>(), Macros.fromJson(""))
    assertEquals(emptyList<Macro>(), Macros.fromJson("{not json"))
    assertEquals(emptyList<Macro>(), Macros.fromJson("{}"))
  }

  @Test
  fun stepsOutOfRangeAreKeptWithinLimitsOrLeftOut() {
    val steps =
      JSONArray()
        .put(JSONObject().put("type", "wait").put("ms", 99_999_999L))
        .put(JSONObject().put("type", "wait").put("ms", -5))
        .put(JSONObject().put("type", "tap").put("label", "OK").put("timeout", 1))
        .put(JSONObject().put("type", "tap").put("label", "  "))
        .put(JSONObject().put("type", "open_app").put("app", "com.example"))
        .put(JSONObject().put("type", "type_text").put("text", ""))
        .put(JSONObject().put("type", "swipe"))
        .put(JSONObject().put("type", "command").put("command", ""))
        .put("not an object")
    val json =
      JSONArray().put(JSONObject().put("id", "a").put("name", "A").put("steps", steps)).toString()
    assertEquals(
      listOf(Wait(Macros.MAX_WAIT_MS), Wait(0), Tap("OK", Macros.MIN_TAP_TIMEOUT_MS)),
      Macros.fromJson(json).single().steps,
    )
  }

  @Test
  fun macrosWithoutIdOrNameOrWithRepeatedIdAreLeftOut() {
    val json =
      JSONArray()
        .put(JSONObject().put("id", "a").put("name", "First"))
        .put(JSONObject().put("id", "a").put("name", "Second"))
        .put(JSONObject().put("name", "No ID"))
        .put(JSONObject().put("id", "b"))
        .put(JSONObject().put("id", "").put("name", "Empty ID"))
        .toString()
    assertEquals(listOf(Macro("a", "First", emptyList())), Macros.fromJson(json))
  }

  @Test
  fun stepsBeyondTheLimitAreLeftOut() {
    val steps = JSONArray()
    repeat(Macros.MAX_STEPS + 10) { steps.put(JSONObject().put("type", "wait").put("ms", it)) }
    val json =
      JSONArray().put(JSONObject().put("id", "a").put("name", "A").put("steps", steps)).toString()
    assertEquals(Macros.MAX_STEPS, Macros.fromJson(json).single().steps.size)
  }

  @Test
  fun commandValues() {
    val value = Macros.commandOf("abc")
    assertTrue(Macros.isCommand(value))
    assertEquals("abc", Macros.idOf(value))
    assertTrue(Macros.isCommand(Macros.STOP_COMMAND))
    assertNull(Macros.idOf(Macros.STOP_COMMAND))
    assertFalse(Macros.isCommand(back()))
    assertFalse(Macros.isCommand(null))
    assertNull(Macros.idOf(back()))
  }

  @Test
  fun addUpdateAndLabel() {
    val macro = Macros.add(prefs, "Reply")
    assertEquals(listOf(macro), Macros.read(prefs))
    assertEquals("Reply", Macros.label(context, prefs, Macros.commandOf(macro.id)))
    Macros.update(prefs, macro.copy(name = "Answer", steps = listOf(Wait(100))))
    assertEquals(listOf(macro.copy(name = "Answer", steps = listOf(Wait(100)))), Macros.read(prefs))
    assertEquals("Answer", Macros.label(context, prefs, Macros.commandOf(macro.id)))
    assertNull(Macros.label(context, prefs, Macros.commandOf("gone")))
    assertNull(Macros.label(context, prefs, back()))
    assertEquals(
      context.getString(R.string.shortcut_stop_macro),
      Macros.label(context, prefs, Macros.STOP_COMMAND),
    )
  }

  @Test
  fun movedStep() {
    val steps = listOf(Wait(1), Wait(2), Wait(3))
    assertEquals(listOf(Wait(2), Wait(1), Wait(3)), Macros.moved(steps, 1, -1))
    assertEquals(listOf(Wait(1), Wait(3), Wait(2)), Macros.moved(steps, 1, 1))
    assertNull(Macros.moved(steps, 0, -1))
    assertNull(Macros.moved(steps, 2, 1))
    assertNull(Macros.moved(steps, 5, -1))
  }

  @Test
  fun parseSeconds() {
    assertEquals(1_000L, Macros.parseSeconds("1"))
    assertEquals(2_000L, Macros.parseSeconds(" 2 "))
    assertEquals(500L, Macros.parseSeconds("0.5"))
    assertEquals(250L, Macros.parseSeconds("0,25"))
    assertEquals(0L, Macros.parseSeconds("0"))
    assertEquals(Macros.MAX_WAIT_MS, Macros.parseSeconds("600"))
    assertNull(Macros.parseSeconds("601"))
    assertNull(Macros.parseSeconds("-1"))
    assertNull(Macros.parseSeconds(""))
    assertNull(Macros.parseSeconds("soon"))
    assertNull(Macros.parseSeconds("NaN"))
  }

  @Test
  fun secondsText() {
    assertEquals("1", Macros.secondsText(1_000))
    assertEquals("1.5", Macros.secondsText(1_500))
    assertEquals("0.25", Macros.secondsText(250))
    assertEquals("0", Macros.secondsText(0))
  }

  @Test
  fun describeSteps() {
    assertEquals("Wait 1 second", Macros.describe(context, Wait(1_000)))
    assertEquals("Wait 2 seconds", Macros.describe(context, Wait(2_000)))
    assertEquals("Wait 500 milliseconds", Macros.describe(context, Wait(500)))
    assertEquals("Tap Send", Macros.describe(context, Tap("Send")))
    assertEquals("Type Hello", Macros.describe(context, TypeText("Hello")))
    assertEquals("Replace text with Hello", Macros.describe(context, TypeText("Hello", replace = true)))
  }

  @Test
  fun macrosAreCommandsWithStopMacro() {
    val shared = SharedPreferencesUtils.getSharedPreferences(context)
    val macro = Macros.add(shared, "Reply")
    try {
      val category = Commands.categories(context).single { it.titleRes == R.string.title_pref_macros }
      assertEquals(
        listOf(Macros.commandOf(macro.id), Macros.STOP_COMMAND),
        category.commands.map { it.value },
      )
      assertEquals("Reply", Commands.label(context, Macros.commandOf(macro.id)))
      assertEquals(
        listOf("Reply", context.getString(R.string.shortcut_stop_macro)),
        Commands.labels(context, listOf(Macros.commandOf(macro.id), Macros.STOP_COMMAND)),
      )
    } finally {
      shared.edit().remove(Macros.KEY).commit()
    }
    assertTrue(Commands.categories(context).none { it.titleRes == R.string.title_pref_macros })
  }

  @Test
  fun commandLeavesSubmenusToo() {
    val value = Macros.commandOf("a")
    val nodes =
      listOf(
        CommandMenus.Command(value),
        CommandMenus.Submenu("s", "Sub", listOf(CommandMenus.Command(back()), CommandMenus.Command(value))),
      )
    assertEquals(
      listOf(CommandMenus.Submenu("s", "Sub", listOf(CommandMenus.Command(back())))),
      CommandMenus.withoutCommand(nodes, value),
    )
  }

  @Test
  fun deleteUnassignsTheMacro() {
    val macro = Macros.add(prefs, "Reply")
    val other = Macros.add(prefs, "Other")
    val value = Macros.commandOf(macro.id)
    val otherValue = Macros.commandOf(other.id)
    val shortcutCode = "classic|" + Commands.shortcutKey(value) + "|key_combo_code"
    val otherShortcutCode = "classic|" + Commands.shortcutKey(otherValue) + "|key_combo_code"
    prefs
      .edit()
      .putString("pref_shortcut_down_and_right_key", value)
      .putString("pref_shortcut_up_and_left_key", otherValue)
      .putLong(shortcutCode, 123L)
      .putBoolean("classic|" + Commands.shortcutKey(value) + "|trigger_modifier_used", true)
      .putLong(otherShortcutCode, 456L)
      .commit()
    AppGestures.write(
      prefs,
      mapOf("com.example" to AppGestures.App("Example", mapOf("g1" to value, "g2" to back()), mapOf("k" to value))),
    )
    GestureProfiles.write(prefs, listOf(GestureProfiles.Profile("p", "Profile", mapOf("g1" to value, "g2" to otherValue))))
    val menu = CommandMenus.Menu("m", "Menu", listOf(CommandMenus.Command(value), CommandMenus.Command(back())))
    CommandMenus.writeCustom(prefs, listOf(menu))

    Macros.delete(context, prefs, macro.id)

    assertEquals(listOf(other), Macros.read(prefs))
    assertEquals(unassigned(), prefs.getString("pref_shortcut_down_and_right_key", null))
    assertEquals(otherValue, prefs.getString("pref_shortcut_up_and_left_key", null))
    assertFalse(prefs.contains(shortcutCode))
    assertFalse(prefs.contains("classic|" + Commands.shortcutKey(value) + "|trigger_modifier_used"))
    assertEquals(456L, prefs.getLong(otherShortcutCode, 0L))
    assertEquals(
      AppGestures.App("Example", mapOf("g2" to back()), emptyMap()),
      AppGestures.read(prefs)["com.example"],
    )
    assertEquals(mapOf("g2" to otherValue), GestureProfiles.read(prefs).single().gestures)
    assertEquals(listOf(CommandMenus.Command(back())), CommandMenus.custom(prefs).single().items)
  }
}
