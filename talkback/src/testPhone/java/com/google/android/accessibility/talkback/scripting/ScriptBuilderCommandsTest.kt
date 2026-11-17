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

package com.google.android.accessibility.talkback.scripting

import android.content.Context
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Commands, triggers and code of scripts made in the script builder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptBuilderCommandsTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val store by lazy { ScriptStore.get(context) }
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(context) }

  private fun macroJson(vararg steps: Macros.Step): JSONObject =
    JSONObject(BuiltScriptMacros.macroJson(Macros.Macro("m", "", steps.toList())))

  private fun draftWithCommandAndTriggers(): Pair<String, JSONObject> {
    val id = ScriptBuilder.create(context, "Chat")
    val draft = ScriptBuilder.draft(store, id)!!
    draft.put(
      "commands",
      JSONArray()
        .put(JSONObject().put("id", "c1").put("title", "Reply").put("macro", macroJson(Macros.SpeakText("Replying"), Macros.RunCommand("NEXT"))))
        // No steps yet: listed, but runs nothing.
        .put(JSONObject().put("id", "c2").put("title", "Empty")),
    )
    draft.put(
      "triggers",
      JSONArray()
        .put(
          JSONObject()
            .put("id", "t1")
            .put("when", ScriptBuilder.WHEN_FOCUS)
            .put("match", JSONObject().put("contains", "new post").put("field", "contentDescription"))
            .put("macro", macroJson(Macros.SpeakText("Compose")))
        )
        .put(JSONObject().put("id", "t2").put("when", ScriptBuilder.WHEN_ANNOUNCEMENT).put("text", "Sent").put("macro", macroJson(Macros.OpenApp("OPEN_APP:com.example/.Main"))))
        // Not finished: no item.
        .put(JSONObject().put("id", "t3").put("when", ScriptBuilder.WHEN_FOCUS).put("match", JSONObject()).put("macro", macroJson(Macros.SpeakText("x")))),
    )
    return id to draft
  }

  @Test
  fun commandsAndTriggers_becomeManifestCommandsCodeAndMacroFiles() {
    val (id, draft) = draftWithCommandAndTriggers()

    ScriptBuilder.save(context, draft)

    val installed = store.find(id)!!
    assertEquals(listOf("c1", "c2"), installed.manifest.commands.map { it.id })
    assertEquals(listOf("Reply", "Empty"), installed.manifest.commands.map { it.title })
    // Opening an app needs system, from the announcement trigger.
    assertEquals(
      setOf(ScriptPermission.INPUT, ScriptPermission.ACTIONS, ScriptPermission.SCREEN, ScriptPermission.SYSTEM),
      installed.manifest.permissions,
    )
    val main = store.readFile(id, ScriptPackage.MAIN)!!.decodeToString()
    assertTrue(main, main.contains("""sf.onCommand("c1", function() sf.macros.run("macros/c1.json") end)"""))
    assertFalse(main, main.contains("sf.onCommand(\"c2\""))
    assertTrue(main, main.contains("sf.onFocus(function(item) if item:find(query) then sf.macros.run(\"macros/t1.json\") end end)"))
    assertTrue(main, main.contains("string.find(string.lower(a.text), \"sent\", 1, true)"))
    assertFalse(main, main.contains("t3"))
    val file = Macros.macroFromJson(store.readFile(id, "macros/c1.json")!!.decodeToString())!!
    assertEquals(listOf(Macros.SpeakText("Replying"), Macros.RunCommand("NEXT")), file.steps)
    assertNull(store.readFile(id, "macros/t3.json"))
    assertNotNull(ScriptBuilder.triggerProblem(draft.getJSONArray("triggers").getJSONObject(2)))
  }

  @Test
  fun macroEditor_editsBuiltCommandsThroughMacros() {
    val (id, draft) = draftWithCommandAndTriggers()
    ScriptBuilder.save(context, draft)
    val macroId = BuiltScriptMacros.idOf(id, BuiltScriptMacros.COMMANDS, "c1")

    val macro = Macros.find(prefs, macroId)!!
    assertEquals("Reply", macro.name)
    Macros.update(prefs, macro.copy(name = "Answer", steps = listOf(Macros.SpeakText("Answering"))))

    val saved = store.find(id)!!
    assertEquals("Answer", saved.manifest.commands.first { it.id == "c1" }.title)
    assertEquals(
      listOf(Macros.SpeakText("Answering")),
      Macros.macroFromJson(store.readFile(id, "macros/c1.json")!!.decodeToString())!!.steps,
    )
    // The user's own macros are left alone.
    assertTrue(Macros.read(prefs).isEmpty())

    Macros.delete(context, prefs, macroId)
    assertEquals(listOf("c2"), store.find(id)!!.manifest.commands.map { it.id })
  }

  @Test
  fun codeAndExtraPermissions_areAdded_andTurnedOffPermissionsStayOff() {
    val id = ScriptBuilder.create(context, "Java")
    val draft = ScriptBuilder.draft(store, id)!!
    draft.put("code", "local Build = java.import('android.os.Build')\nsf.speak(Build.MODEL)")
    draft.put("permissions", JSONArray().put("java").put("speech"))
    ScriptBuilder.save(context, draft)

    var installed = store.find(id)!!
    assertTrue(store.readFile(id, ScriptPackage.MAIN)!!.decodeToString().endsWith("sf.speak(Build.MODEL)\n"))
    assertEquals(setOf(ScriptPermission.JAVA, ScriptPermission.SPEECH), installed.granted)

    store.setGranted(id, ScriptPermission.JAVA, false)
    ScriptBuilder.save(context, ScriptBuilder.draft(store, id)!!.put("name", "Java 2"))
    installed = store.find(id)!!
    assertEquals(setOf(ScriptPermission.SPEECH), installed.granted)
  }

  @Test
  fun export_writesAZipThatInstallsAsTheSameBuiltScript() {
    val (id, draft) = draftWithCommandAndTriggers()
    ScriptBuilder.save(context, draft)
    val zip = File(context.cacheDir, "export.zip")
    zip.outputStream().use { ScriptBuilder.exportZip(store, id, it) }

    val pkg = ScriptPackage.read(zip.readBytes())

    assertEquals(id, pkg.manifest.id)
    assertTrue(ScriptBuilder.DRAFT_FILE in pkg.files)
    assertTrue("macros/c1.json" in pkg.files)
  }

  @Test
  fun notificationTrigger_needsTheNotificationsPermission_andCommandsHaveAnAssignScreen() {
    val id = ScriptBuilder.create(context, "Mail")
    val draft = ScriptBuilder.draft(store, id)!!
    draft.put(
      "triggers",
      JSONArray().put(JSONObject().put("id", "t1").put("when", ScriptBuilder.WHEN_NOTIFICATION).put("text", "Invoice").put("macro", macroJson(Macros.SpeakText("Invoice")))),
    )
    draft.put("commands", JSONArray().put(JSONObject().put("id", "c1").put("title", "Read")))
    ScriptBuilder.save(context, draft)

    assertTrue(ScriptPermission.NOTIFICATIONS in store.find(id)!!.manifest.permissions)
    assertTrue(store.readFile(id, ScriptPackage.MAIN)!!.decodeToString().contains("sf.onNotification("))
    val (screen, args) = Macros.assignScreen(BuiltScriptMacros.idOf(id, BuiltScriptMacros.COMMANDS, "c1"))!!
    assertEquals(ScriptCommandFragment::class.java.name, screen)
    assertEquals("c1", args.getString(ScriptCommandFragment.ARG_COMMAND))
    assertNull(Macros.assignScreen(BuiltScriptMacros.idOf(id, BuiltScriptMacros.TRIGGERS, "t1")))
  }
}
