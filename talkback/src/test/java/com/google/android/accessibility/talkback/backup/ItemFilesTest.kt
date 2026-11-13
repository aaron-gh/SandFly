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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.voicecommands.CustomVoiceCommands
import com.google.android.accessibility.talkback.gesture.AppGestures
import com.google.android.accessibility.talkback.gesture.GestureProfiles
import com.google.android.accessibility.talkback.keyboard.CustomKeymaps
import com.google.android.accessibility.talkback.keyboard.KeyCombo
import com.google.android.accessibility.talkback.keyboard.KeyComboPersister
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.speechdictionary.SpeechDictionary
import com.google.android.accessibility.talkback.speechdictionary.SymbolDictionary
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ItemFilesTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    SettingsProfiles.reset()
    prefs.edit().clear().commit()
  }

  @After
  fun tearDown() {
    SettingsProfiles.reset()
  }

  private fun imported(text: String): ItemFiles.Result.Imported =
    ItemFiles.import(context, text) as ItemFiles.Result.Imported

  @Test
  fun importsMacrosAndProfilesThatNameThemByName() {
    val result =
      imported(
        """
        Here is your file:
        ```json
        {
          "format": "sandfly-items",
          "version": 1,
          // A gesture profile that runs a macro of the file.
          "gestureProfiles": [
            { "name": "Reading", "gestures": {
              "pref_shortcut_up": "MACRO:Read all",
              "pref_shortcut_down": "NO_SUCH_COMMAND",
              "pref_no_such_gesture": "NEXT"
            } }
          ],
          "macros": [
            { "name": "Read all", "steps": [ { "type": "command", "command": "READ_FROM_TOP" } ] }
          ]
        }
        ```
        """
      )
    assertEquals(1, result.counts[ItemFiles.Kind.MACROS])
    assertEquals(1, result.counts[ItemFiles.Kind.GESTURE_PROFILES])
    assertEquals(2, result.problems.size)
    assertTrue(result.problems.any { "NO_SUCH_COMMAND" in it })
    assertTrue(result.problems.any { "pref_no_such_gesture" in it })

    val macro = Macros.read(prefs).single()
    val profile = GestureProfiles.read(prefs).single()
    assertEquals("Reading", profile.name)
    assertEquals(mapOf("pref_shortcut_up" to Macros.commandOf(macro.id)), profile.gestures)
    assertEquals(listOf(Macros.RunCommand("READ_FROM_TOP")), macro.steps)
  }

  @Test
  fun importReplacesItemsOfTheSameNameAndKeepsTheirIds() {
    imported("""{"macros":[{"name":"Mail","steps":[{"type":"wait","ms":100}]}]}""")
    val id = Macros.read(prefs).single().id
    imported("""{"macros":[{"name":"mail","steps":[{"type":"wait","ms":200}]},{"name":"Other","steps":[]}]}""")
    val macros = Macros.read(prefs)
    assertEquals(2, macros.size)
    assertEquals(id, macros[0].id)
    assertEquals("mail", macros[0].name)
    assertEquals(listOf(Macros.Wait(200)), macros[0].steps)
  }

  @Test
  fun exportedProfileBringsItsMacrosAndImportsAsItWas() {
    imported(
      """{"macros":[{"id":"m","name":"Read all","steps":[{"type":"command","command":"READ_FROM_TOP"}]}],
         "gestureProfiles":[{"name":"Reading","swipesBeforeLift":true,"gestures":{"pref_shortcut_up":"MACRO:m"}}]}"""
    )
    val profile = GestureProfiles.read(prefs).single()
    val file = ItemFiles.exportGestureProfiles(context, setOf(profile.id))
    assertTrue(JSONObject(file).has("macros"))

    prefs.edit().clear().commit()
    val result = imported(file)
    assertTrue(result.problems.isEmpty())
    val macro = Macros.read(prefs).single()
    val again = GestureProfiles.read(prefs).single()
    assertEquals(mapOf("pref_shortcut_up" to Macros.commandOf(macro.id)), again.gestures)
    assertEquals(true, again.swipesBeforeLift)
  }

  @Test
  fun importsAppsWithKeys() {
    val result =
      imported(
        """{"apps":[{"package":"org.telegram.messenger","name":"Telegram",
            "gestures":{"pref_shortcut_up":"READ_FROM_TOP"},
            "keys":{"Ctrl+Enter":"UNASSIGNED","SandFly+M":"NEXT","SandFly+Nope":"NEXT"}},
           {"package":"not a package"}]}"""
      )
    assertEquals(1, result.counts[ItemFiles.Kind.APPS])
    assertEquals(2, result.problems.size)
    val app = AppGestures.read(prefs).getValue("org.telegram.messenger")
    assertEquals(mapOf("pref_shortcut_up" to "READ_FROM_TOP"), app.gestures)
    assertEquals(
      mapOf(
        AppGestures.keyName(KeyCombo(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_ENTER, false)) to "UNASSIGNED",
        AppGestures.keyName(KeyCombo(0, KeyEvent.KEYCODE_M, true)) to "NEXT",
      ),
      app.keys,
    )
    // And back.
    val exported = JSONObject(ItemFiles.exportApps(context)).getJSONArray("apps").getJSONObject(0)
    assertEquals("NEXT", exported.getJSONObject("keys").getString("SandFly+M"))
  }

  @Test
  fun importsKeymapAsCustomKeymapAndReplacesIt() {
    val file =
      """{"keymaps":[{"name":"Mine","sandFlyKey":"insert","shortcuts":{"NEXT":"SandFly+Right","PREVIOUS":"SandFly+Right"}}]}"""
    val result = imported(file)
    assertEquals(1, result.counts[ItemFiles.Kind.KEYMAPS])
    assertEquals(1, result.problems.size)
    val keymap = CustomKeymaps.all(context).single()
    assertEquals("Mine", keymap.name)
    val saved = KeyComboPersister(context, keymap.persisterPrefix)
    assertEquals(1, saved.keysStartingWith("").size)

    imported("""{"keymaps":[{"name":"mine","shortcuts":{"PREVIOUS":"SandFly+Left"}}]}""")
    assertEquals(1, CustomKeymaps.all(context).size)
    val replaced = KeyComboPersister(context, keymap.persisterPrefix)
    val keys = replaced.keysStartingWith("")
    assertEquals(1, keys.size)
    assertEquals(KeyCombo(0, KeyEvent.KEYCODE_DPAD_LEFT, true), replaced.getKeyCombo(keys.single()))
  }

  @Test
  fun dictionaryEntriesReplaceTheSameFindAndRefuseSlowExpressions() {
    SpeechDictionary.write(prefs, SpeechDictionary.fromJson("""[{"find":"btw","replace":"by the way"}]"""))
    val result =
      imported(
        """{"speechDictionary":[{"find":"btw","replace":"BTW"},{"find":"(a+)+","replace":"","regex":true},
            {"find":"colou?r","replace":"color","regex":true}],
           "symbolDictionary":[{"find":"→","replace":"arrow","regex":true}]}"""
      )
    assertEquals(2, result.counts[ItemFiles.Kind.SPEECH_DICTIONARY])
    assertEquals(1, result.problems.size)
    val entries = SpeechDictionary.read(prefs)
    assertEquals(listOf("BTW", "color"), entries.map { it.replacement.replace })
    assertTrue(entries[1].replacement.regex)
    // The symbol dictionary has no regular expressions.
    assertFalse(SymbolDictionary.read(prefs).single().replacement.regex)
  }

  @Test
  fun importsVoiceCommands() {
    val result =
      imported("""{"voiceCommands":[{"phrase":"read it all","command":"READ_FROM_TOP"},{"phrase":"x","command":"NOPE"}]}""")
    assertEquals(1, result.counts[ItemFiles.Kind.VOICE_COMMANDS])
    assertEquals(listOf("read it all" to "READ_FROM_TOP"), CustomVoiceCommands.read(prefs).map { it.phrase to it.value })
  }

  @Test
  fun importsSettingsProfilesWithValuesOfTheSettingsTypes() {
    prefs.edit().putString("a_choice", "1.0").putInt("a_number", 3).putBoolean("a_switch", true).commit()
    val result =
      imported(
        """{"settingsProfiles":[{"name":"Reading","apps":["com.amazon.kindle"],"reading":true,
            "settings":{"a_choice":1.5,"a_number":7,"a_switch":false,"pref_vibration":"no",
                        "pref_gemini_api_key":"x","a_new_one":true}}]}"""
      )
    assertEquals(1, result.counts[ItemFiles.Kind.SETTINGS_PROFILES])
    // A text for a switch, and an API key, are left out.
    assertEquals(2, result.problems.size)
    val store = SettingsProfiles.ready(context)
    val profile = store.profiles().single()
    assertEquals(setOf("com.amazon.kindle"), profile.triggers.apps)
    assertTrue(profile.triggers.reading)
    assertEquals(
      mapOf("default/a_choice" to "1.5", "default/a_number" to 7, "default/a_switch" to false, "default/a_new_one" to true),
      store.settings(profile.id),
    )
    val exported = JSONObject(ItemFiles.exportSettingsProfiles(context)).getJSONArray("settingsProfiles").getJSONObject(0)
    assertEquals("1.5", exported.getJSONObject("settings").getString("a_choice"))
  }

  @Test
  fun recognizesOtherFiles() {
    assertEquals(ItemFiles.Result.NotRead, ItemFiles.import(context, "not json"))
    assertEquals(ItemFiles.Result.NotRead, ItemFiles.import(context, """{"format":"other","macros":[]}"""))
    assertEquals(ItemFiles.Result.NotRead, ItemFiles.import(context, """{"something":1}"""))
    assertEquals(ItemFiles.Result.SettingsFile, ItemFiles.import(context, """{"format":"sandfly-settings","version":1}"""))
    assertEquals(ItemFiles.Result.Newer, ItemFiles.import(context, """{"format":"sandfly-items","version":9,"macros":[]}"""))
  }

  @Test
  fun macroIdsInTheFileAreNotKept() {
    imported("""{"macros":[{"id":"fixed","name":"A","steps":[]}]}""")
    assertNotEquals("fixed", Macros.read(prefs).single().id)
  }

  @Test
  fun referenceStartsWithTheFormatAndListsThisPhone() {
    prefs.edit().putBoolean("pref_vibration", false).commit()
    val reference = ItemFileReference.build(ContextThemeWrapper(context, R.style.TalkBackSettings))
    assertTrue(reference.startsWith("# SandFly item files"))
    assertTrue("| `pref_shortcut_up` |" in reference)
    assertTrue("| `READ_FROM_TOP` |" in reference)
    assertTrue("| `pref_vibration` |" in reference)
  }

  @Test
  fun gesturesAreReadFromTheGesturesScreen() {
    val gestures = ItemFiles.gestures(context)
    assertTrue(gestures.size > 50)
    assertTrue("pref_shortcut_up" in gestures)
  }
}
