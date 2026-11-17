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

import com.google.android.accessibility.talkback.macros.Macros
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the code the script builder writes in real Lua 5.4. */
class ScriptBuilderLuaTest {
  private val prelude = File("src/phone/assets/scripting/prelude.lua").readText()

  private class Host : LuaEngine.Host {
    val calls = mutableListOf<Pair<String, JSONObject?>>()
    val errors = mutableListOf<String>()

    override fun call(method: String, json: String?): String? {
      val args = json?.let { JSONObject(it) }
      if (method == "error") {
        errors += args!!.optString("message")
        return null
      }
      calls += method to args
      return when (method) {
        "apiVersion" -> "6"
        "manifest" -> """{"commands":[{"id":"c1"}]}"""
        "macros.run" -> "true"
        else -> null
      }
    }

    override fun loadModule(name: String): ByteArray? = null

    override fun isOvertime(): Boolean = false
  }

  @Test
  fun generatedCodeRuns_andRunsTheMacroFile() {
    val macro = JSONObject(BuiltScriptMacros.macroJson(Macros.Macro("m", "", listOf(Macros.SpeakText("Hi")))))
    val draft =
      JSONObject()
        .put("commands", JSONArray().put(JSONObject().put("id", "c1").put("title", "Hi").put("macro", macro)))
        .put(
          "triggers",
          JSONArray()
            .put(JSONObject().put("id", "t1").put("when", "focus").put("match", JSONObject().put("contains", "a \"quoted\" text")).put("macro", macro))
            .put(JSONObject().put("id", "t2").put("when", "app").put("macro", macro))
            .put(JSONObject().put("id", "t3").put("when", "announcement").put("text", "Sent").put("macro", macro))
            .put(JSONObject().put("id", "t4").put("when", "notification").put("text", "New message").put("macro", macro)),
        )
        .put("code", "sf.macros.run('macros/c1.json')")
    val host = Host()
    val engine = LuaEngine(host, false)
    engine.start(prelude, 32L * 1024 * 1024)

    engine.runMain("local manifest = {}\n" + ScriptBuilder.code(draft), "main.lua")

    assertEquals(emptyList<String>(), host.errors)
    val runs = host.calls.filter { it.first == "macros.run" }.map { it.second!!.getString("file") }
    assertEquals(listOf("macros/c1.json"), runs)
    assertTrue(host.calls.map { it.first }.toString(), host.calls.any { it.first.startsWith("hook") || it.first.contains("on") })
    engine.close()
  }

  @Test
  fun everyTriggerAndSpeechChangeRuns_andSpeechChangesChangeSpeech() {
    val macro = JSONObject(BuiltScriptMacros.macroJson(Macros.Macro("m", "", listOf(Macros.SpeakText("Hi")))))
    val triggers = JSONArray()
    val kinds =
      listOf("app", "appLeave", "window", "textChange", "contentChange", "scroll", "selection", "click", "longClick", "inputFocus")
    kinds.forEachIndexed { i, kind -> triggers.put(JSONObject().put("id", "t$i").put("when", kind).put("macro", macro)) }
    triggers.put(JSONObject().put("id", "e").put("when", "every").put("minutes", 5).put("macro", macro))
    triggers.put(JSONObject().put("id", "d").put("when", "daily").put("time", "08:30").put("macro", macro))
    triggers.put(JSONObject().put("id", "b").put("when", "button").put("setting", "s1").put("macro", macro))
    triggers.put(JSONObject().put("id", "c").put("when", "click").put("match", JSONObject().put("contains", "Send")).put("macro", macro))
    val draft =
      JSONObject()
        .put("triggers", triggers)
        .put(
          "speech",
          JSONArray()
            .put(JSONObject().put("when", "speech").put("change", "replacePart").put("find", "Wi-Fi").put("text", "wireless"))
            .put(JSONObject().put("when", "speech").put("has", "battery").put("change", "after").put("text", "check {text}"))
            .put(JSONObject().put("when", "notification").put("app", "com.example.mail").put("change", "silence")),
        )
    val host = Host()
    val engine = LuaEngine(host, false)
    engine.start(prelude, 32L * 1024 * 1024)

    engine.runMain("local manifest = {}\n" + ScriptBuilder.code(draft), "main.lua")

    assertEquals(emptyList<String>(), host.errors)
    val hooks = host.calls.last { it.first == "hooks" }.second!!.getJSONArray("names")
    val names = (0 until hooks.length()).map { hooks.getString(it) }.toSet()
    for (hook in listOf("appEnter", "appLeave", "windowChange", "textChange", "contentChange", "scroll", "selectionChange", "click", "longClick", "inputFocus", "button", "speech", "notification")) {
      assertTrue("$hook in $names", hook in names)
    }
    fun speech(text: String) =
      JSONObject(engine.dispatch(JSONObject().put("type", "speech").put("data", JSONObject().put("text", text)).toString())!!)
    assertEquals("wireless on", speech("Wi-Fi on").getString("text"))
    val battery = speech("Battery low")
    assertEquals(battery.toString(), "Battery low check Battery low", battery.optString("text"))
    engine.close()
  }

  @Test
  fun afterAlone_addsToTheSpeech() {
    val host = Host()
    val engine = LuaEngine(host, false)
    engine.start(prelude, 32L * 1024 * 1024)
    engine.runMain("local manifest = {}\nsf.onSpeech(function(said) return { after = 'x' } end)", "main.lua")
    val answer = engine.dispatch(JSONObject().put("type", "speech").put("data", JSONObject().put("text", "Battery low")).toString())
    engine.close()
    assertEquals("Battery low x", JSONObject(answer!!).getString("text"))
  }

  @Test
  fun deviceAndSystemCalls_reachSandFly() {
    val host = Host()
    val engine = LuaEngine(host, false)
    engine.start(prelude, 32L * 1024 * 1024)

    engine.runMain(
      "local manifest = {}\n" +
        "sf.device.get('wifi')\nsf.device.media('playPause')\nsf.system.setTimer(60, 'Tea')\n" +
        "sf.system.setAlarm(7, 30)\nsf.system.call('123')\nsf.system.message('123', 'Hi')\nsf.system.camera(true)",
      "main.lua",
    )

    assertEquals(emptyList<String>(), host.errors)
    val calls = host.calls.map { it.first }
    for (name in listOf("device.get", "device.media", "system.setTimer", "system.setAlarm", "system.call", "system.message", "system.camera")) {
      assertTrue("$name in $calls", name in calls)
    }
    assertEquals(60, host.calls.first { it.first == "system.setTimer" }.second!!.getInt("seconds"))
    engine.close()
  }
}
