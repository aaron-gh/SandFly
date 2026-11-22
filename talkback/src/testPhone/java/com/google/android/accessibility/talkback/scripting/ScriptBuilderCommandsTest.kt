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
      setOf(ScriptPermission.INPUT, ScriptPermission.SPEECH, ScriptPermission.SCREEN, ScriptPermission.SYSTEM),
      installed.manifest.permissions,
    )
    val main = store.readFile(id, ScriptPackage.MAIN)!!.decodeToString()
    assertTrue(main, main.contains("""sf.onCommand("c1", function(e) __sf_brun("macros/c1.json","""))
    assertFalse(main, main.contains("sf.onCommand(\"c2\""))
    assertTrue(main, main.contains("sf.onFocus(function(item) if item:find(query) then __sf_brun(\"macros/t1.json\""))
    assertTrue(main, main.contains("__sf_bhas(a.text, \"sent\")"))
    // The script ID is random and can contain "t3" itself.
    assertFalse(main, main.contains("macros/t3.json"))
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

  @Test
  fun settingsCommandOptionsAboutAndSpeechChanges_reachTheManifest() {
    val id = ScriptBuilder.create(context, "Full")
    val draft = ScriptBuilder.draft(store, id)!!
    draft
      .put("description", "Does things")
      .put("version", "1.2")
      .put("homepage", "https://example.com")
      .put("keepLoaded", true)
      .put(
        "settings",
        JSONArray()
          .put(JSONObject().put("key", "s1").put("type", "switch").put("title", "Loud").put("default", true))
          .put(JSONObject().put("key", "s2").put("type", "button").put("title", "Reset")),
      )
      .put(
        "commands",
        JSONArray().put(
          JSONObject().put("id", "c1").put("title", "Next").put("menu", true).put("control", true).put("gesture", "swipeUpThenRight").put("keys", "sf+n")
            .put("macro", macroJson(Macros.SpeakText("{direction}")))
        ),
      )
      .put("speech", JSONArray().put(JSONObject().put("when", "focus").put("match", JSONObject().put("id", "row")).put("change", "before").put("text", "Row")))
      .put(
        "rules",
        JSONArray().put(
          JSONObject()
            .put("match", JSONObject().put("text", "OK").put("parentId", "bar").put("checked", false).put("inside", JSONArray().put(JSONObject().put("id", "a")).put(JSONObject().put("id", "b"))))
            .put("label", "Confirm")
        ),
      )
    ScriptBuilder.save(context, draft)

    val manifest = store.find(id)!!.manifest
    assertEquals("Does things", manifest.description)
    assertEquals("1.2", manifest.version)
    assertTrue(manifest.keepLoaded)
    assertEquals(listOf("s1", "s2"), manifest.settings.map { it.key })
    val command = manifest.commands.single()
    assertTrue(command.menu)
    assertTrue(command.control)
    assertEquals(1, manifest.rules.size)
    assertTrue(ScriptPermission.SPEECH in manifest.permissions)
    assertTrue(ScriptPermission.SCREEN in manifest.permissions)
    assertTrue(store.readFile(id, ScriptPackage.MAIN)!!.decodeToString().contains("sf.onFocus(function(item, said)"))
  }

  @Test
  fun copyToBuilder_keepsManifestCodeAndFiles_andDuplicateGetsANewId() {
    val source =
      """local manifest = { id = 'plain', name = 'Plain', author = 'a', minApiVersion = 1, testedApiVersion = 1,
        |  permissions = { 'speech', 'input' }, rules = { { id = 'send', label = 'Send' } }, commands = { { id = 'go', title = 'Go' } } }
        |sf.onCommand('go', function() sf.speak(sf.readFile('data/word.txt')) end)
        |""".trimMargin()
    val pkg = ScriptPackage(mapOf("main.lua" to source.encodeToByteArray(), "data/word.txt" to "hi".encodeToByteArray()), ScriptManifest.parse(ManifestReader.read(source)))
    store.install(pkg, setOf(ScriptPermission.SPEECH, ScriptPermission.INPUT))

    val id = ScriptBuilder.copyToBuilder(context, store.find("plain")!!)

    val copy = store.find(id)!!
    assertEquals("Plain copy", copy.manifest.name)
    assertEquals(listOf("go"), copy.manifest.commands.map { it.id })
    assertEquals("Send", copy.manifest.rules.single().label)
    assertEquals("hi", store.readFile(id, "data/word.txt")!!.decodeToString())
    val main = store.readFile(id, ScriptPackage.MAIN)!!.decodeToString()
    assertTrue(main, main.contains("sf.onCommand('go'"))

    val again = ScriptBuilder.createFrom(context, ScriptBuilder.draft(store, id)!!, id)
    assertTrue(again != id)
    assertEquals("hi", store.readFile(again, "data/word.txt")!!.decodeToString())
  }

  @Test
  fun luaSyntaxErrors_areFound() {
    assertNull(LuaEngine.syntaxError("local x = 1\nsf.speak(x)"))
    assertTrue(LuaEngine.syntaxError("if then end")!!.isNotEmpty())
  }

  @Test
  fun taskCommands_needOnlyWhatTheirStepsUse() {
    val id = ScriptBuilder.create(context, "Torch")
    val draft = ScriptBuilder.draft(store, id)!!
    draft.put(
      "commands",
      JSONArray().put(
        JSONObject().put("id", "c1").put("title", "Torch").put("macro", macroJson(Macros.Do(Macros.DoKind.DEVICE_CONTROL, "toggle", "flashlight")))
      ),
    )
    ScriptBuilder.save(context, draft)

    // No input: the user assigns the command; system: it changes a device setting.
    assertEquals(setOf(ScriptPermission.SYSTEM), store.find(id)!!.manifest.permissions)

    draft.getJSONArray("commands").getJSONObject(0).put("gesture", "swipeUpThenRight")
    ScriptBuilder.save(context, draft)
    assertEquals(setOf(ScriptPermission.SYSTEM, ScriptPermission.INPUT), store.find(id)!!.manifest.permissions)
  }

  @Test
  fun macroPermissions_followTheSteps() {
    fun perms(vararg steps: Macros.Step) = ScriptRuntime.macroPermissions(Macros.Macro("m", "", steps.toList()))
    assertEquals(emptySet<ScriptPermission>(), perms(Macros.Do(Macros.DoKind.VIBRATE, number = 100)))
    assertEquals(setOf(ScriptPermission.SPEECH), perms(Macros.SpeakText("{battery} percent")))
    assertEquals(setOf(ScriptPermission.SPEECH, ScriptPermission.SCREEN), perms(Macros.SpeakText("{item}")))
    assertEquals(setOf(ScriptPermission.SCREEN, ScriptPermission.ACTIONS), perms(Macros.Tap("Send")))
    assertEquals(setOf(ScriptPermission.INPUT), perms(Macros.RunCommand("NEXT")))
    assertEquals(setOf(ScriptPermission.SYSTEM), perms(Macros.Do(Macros.DoKind.MEDIA, "playPause"), Macros.Do(Macros.DoKind.SET_TIMER, "Tea", number = 180)))
    assertEquals(
      setOf(ScriptPermission.SCREEN),
      perms(Macros.If(Macros.Condition(listOf(Macros.Clause(Macros.KeyboardShown)))), Macros.EndIf),
    )
  }
}
