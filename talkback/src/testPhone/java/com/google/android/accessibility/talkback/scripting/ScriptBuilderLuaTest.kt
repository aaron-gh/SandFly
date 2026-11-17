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
}
