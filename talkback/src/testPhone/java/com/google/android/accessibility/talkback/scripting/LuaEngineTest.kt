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

import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Runs the prelude and scripts in real Lua 5.4, with a host that records the calls. */
class LuaEngineTest {
  private val prelude = File("src/phone/assets/scripting/prelude.lua").readText()

  private class Host : LuaEngine.Host {
    val calls = mutableListOf<Pair<String, JSONObject?>>()
    val answers = mutableMapOf<String, (JSONObject?) -> String?>()
    val modules = mutableMapOf<String, String>()
    val errors = mutableListOf<String>()
    var deadline = Long.MAX_VALUE

    override fun call(method: String, json: String?): String? {
      val args = json?.let { JSONObject(it) }
      if (method == "error") {
        errors += args!!.optString("message")
        return null
      }
      calls += method to args
      answers[method]?.let { return it(args) }
      return when (method) {
        "apiVersion" -> "1"
        "manifest" -> """{"commands":[]}"""
        else -> null
      }
    }

    override fun loadModule(name: String): ByteArray? = modules[name]?.toByteArray()

    override fun isOvertime(): Boolean = System.nanoTime() / 1_000_000 > deadline

    fun spoken(): List<String> =
      calls.filter { it.first == "speak" }.map { it.second!!.getString("text") }
  }

  private val host = Host()
  private var engine: LuaEngine? = null

  private fun start(java: Boolean = false, memoryBytes: Long = 32L * 1024 * 1024): LuaEngine =
    LuaEngine(host, java).also {
      engine = it
      it.start(prelude, memoryBytes)
    }

  private fun run(source: String, java: Boolean = false, memoryBytes: Long = 32L * 1024 * 1024) {
    start(java, memoryBytes).runMain(source, "main.lua")
  }

  private fun withTimeLimit(ms: Long, block: () -> Unit) {
    host.deadline = System.nanoTime() / 1_000_000 + ms
    try {
      block()
    } finally {
      host.deadline = Long.MAX_VALUE
    }
  }

  private fun dispatch(type: String, data: Any?): String? =
    engine!!.dispatch(JSONObject().put("type", type).put("data", data ?: JSONObject.NULL).toString())

  @After
  fun close() {
    engine?.close()
  }

  @Test
  fun aScriptSeesOnlyTheSandbox() {
    run(
      """
      local seen = {}
      for _, name in ipairs({ 'java', 'debug', 'io', 'package', 'dofile', 'loadfile',
          '__sf_dispatch', '__sf_main', '__jthrowable__' }) do
        if _G[name] ~= nil or _ENV[name] ~= nil then seen[#seen + 1] = name end
      end
      seen[#seen + 1] = tostring(os.execute == nil)
      seen[#seen + 1] = tostring(string.dump == nil)
      seen[#seen + 1] = tostring(getmetatable(''))
      sf.speak(table.concat(seen, ','))
      """
    )
    assertEquals(listOf("true,true,false"), host.spoken())
  }

  @Test
  fun compiledLuaIsRefused() {
    run(
      """
      local ok, err = load('\27Lua garbage')
      sf.speak(tostring(ok) .. ' ' .. tostring(err ~= nil))
      """
    )
    assertEquals(listOf("nil true"), host.spoken())
  }

  @Test
  fun aLoopEndsWhenTheTimeIsUp() {
    assertInterrupted { withTimeLimit(100) { run("while true do end") } }
  }

  @Test
  fun pcallCannotCatchTheTimeLimit() {
    assertInterrupted {
      withTimeLimit(100) {
        run("while true do pcall(function() while true do end end) end")
      }
    }
  }

  @Test
  fun aCoroutineHasTheTimeLimitToo() {
    assertInterrupted {
      withTimeLimit(100) {
        run(
          """
          local co = coroutine.create(function() while true do end end)
          while true do coroutine.resume(co) end
          """
        )
      }
    }
  }

  @Test
  fun memoryIsLimited() {
    assertInterrupted {
      run(
        "local t = {} for i = 1, 1e9 do t[i] = tostring(i) .. 'xxxxxxxxxxxxxxxx' end",
        memoryBytes = 8L * 1024 * 1024,
      )
    }
  }

  @Test
  fun stringRepIsLimited() {
    run(
      """
      local ok, err = pcall(string.rep, 'x', 1e9)
      sf.speak(tostring(ok))
      local ok2 = pcall(function() return ('x'):rep(1e9) end)
      sf.speak(tostring(ok2))
      """
    )
    assertEquals(listOf("false", "false"), host.spoken())
  }

  @Test
  fun textOutsideTheBasicPlaneSurvives() {
    run("sf.speak('Smile 😀, café')")
    assertEquals(listOf("Smile 😀, café"), host.spoken())
  }

  @Test
  fun aScriptWaitsForAnAnswer() {
    host.answers["ui.confirm"] = { """{"promise":7}""" }
    run("local yes = sf.ui.confirm('Go?') sf.speak('answer ' .. tostring(yes))")
    assertEquals(emptyList<String>(), host.spoken())
    dispatch("settle", JSONObject().put("id", 7).put("ok", true).put("value", true))
    assertEquals(listOf("answer true"), host.spoken())
  }

  @Test
  fun aFailedAnswerIsAnError() {
    host.answers["ui.confirm"] = { """{"promise":3}""" }
    run(
      """
      local ok, err = pcall(sf.ui.confirm, 'Go?')
      sf.speak(tostring(ok) .. ': ' .. tostring(err))
      """
    )
    dispatch("settle", JSONObject().put("id", 3).put("ok", false).put("error", "No dialog"))
    assertEquals(1, host.spoken().size)
    assertTrue(host.spoken()[0], host.spoken()[0].startsWith("false: ") && "No dialog" in host.spoken()[0])
  }

  @Test
  fun waitingWorksInTheScriptsOwnCoroutines() {
    host.answers["ui.prompt"] = { """{"promise":1}""" }
    run(
      """
      local co = coroutine.wrap(function() return sf.ui.prompt('Name') end)
      sf.speak('got ' .. co())
      """
    )
    dispatch("settle", JSONObject().put("id", 1).put("ok", true).put("value", "Sam"))
    assertEquals(listOf("got Sam"), host.spoken())
  }

  @Test
  fun aSpeechHookCannotWait() {
    host.answers["ui.confirm"] = { """{"promise":1}""" }
    run("sf.onSpeech(function(text) sf.ui.confirm('x') return 'never' end)")
    val answer = dispatch("speech", JSONObject().put("text", "hello"))
    assertNull(answer)
    assertTrue(host.errors.single(), "waits for an answer" in host.errors.single())
  }

  @Test
  fun aFocusHookChangesSpeech() {
    run("sf.onFocus(function(item, text) return { before = item.text .. ':' } end)")
    val answer =
      dispatch(
        "focus",
        JSONObject().put("text", "hello").put("arg", JSONObject().put("handle", 1).put("text", "Row")),
      )
    assertEquals("Row: hello", JSONObject(answer!!).getString("text"))
    val hooks = host.calls.last { it.first == "hooks" }.second!!
    assertEquals("focus", hooks.getJSONArray("names").getString(0))
  }

  @Test
  fun sleepGoesOnAfterTheTimer() {
    run("sf.speak('before') sf.sleep(50) sf.speak('after')")
    val timer = host.calls.single { it.first == "timer.set" }.second!!
    assertEquals(listOf("before"), host.spoken())
    dispatch("timer", JSONObject().put("id", timer.getInt("id")))
    assertEquals(listOf("before", "after"), host.spoken())
  }

  @Test
  fun aCommandCanFallBack() {
    host.answers["manifest"] = { """{"commands":[{"id":"go"}]}""" }
    run("sf.onCommand('go', function(event) return event.via ~= 'keys' end)")
    assertNull(dispatch("command", JSONObject().put("id", "go").put("via", "gesture")))
    assertEquals(
      true,
      JSONObject(dispatch("command", JSONObject().put("id", "go").put("via", "keys"))!!)
        .getBoolean("fallback"),
    )
  }

  @Test
  fun requireLoadsAFileFromThePackage() {
    host.modules["lib/words.lua"] = "return { hello = function() return 'hi' end }"
    run("sf.speak(require('lib.words').hello()) sf.speak(tostring(require('lib.words') == require('lib.words')))")
    assertEquals(listOf("hi", "true"), host.spoken())
  }

  @Test
  fun hostErrorsBecomeLuaErrors() {
    host.answers["storage.set"] = { throw IllegalArgumentException("Storage is full") }
    run("local ok, err = pcall(sf.storage.set, 'k', 'v') sf.speak(tostring(ok) .. ' ' .. err)")
    assertTrue(host.spoken().single(), host.spoken().single().endsWith("Storage is full"))
  }

  @Test
  fun jsonRoundTrips() {
    host.answers["storage.get"] = { """{"value":{"list":[1,2.5,"três"],"flag":false}}""" }
    run(
      """
      local v = sf.storage.get('k')
      sf.speak(v.list[3] .. ' ' .. math.type(v.list[1]) .. ' ' .. tostring(v.flag))
      sf.storage.set('k', { a = { 1, 2 }, b = 'x' })
      """
    )
    assertEquals(listOf("três integer false"), host.spoken())
    val stored = host.calls.last { it.first == "storage.set" }.second!!.getJSONObject("value")
    assertEquals("[1,2]", stored.getJSONArray("a").toString())
  }

  @Test
  fun withoutTheJavaPermissionThereIsNoJava() {
    run("sf.speak(tostring(java))")
    assertEquals(listOf("nil"), host.spoken())
  }

  @Test
  fun withTheJavaPermissionTheScriptHasJava() {
    run("sf.speak(java.import('java.lang.Integer'):toString(42))", java = true)
    assertEquals(listOf("42"), host.spoken())
  }

  @Test
  fun aBrokenScriptFailsToLoad() {
    try {
      run("this is not lua")
      fail()
    } catch (e: LuaEngine.ScriptError) {
      assertFalse(e.interrupted)
    }
  }

  @Test
  fun theExampleScriptReadsAndRuns() {
    val source = File("src/phone/assets/scripting/example-plugin.lua").readText()
    val manifest = ScriptManifest.parse(ManifestReader.read(source))
    assertEquals("example-plugin", manifest.id)
    host.answers["settings.get"] = { "true" }
    host.answers["storage.get"] = { null }
    run(source)
    dispatch("appEnter", JSONObject().put("package", "com.example.mail"))
    assertEquals(listOf("mail, opened 1 time"), host.spoken())
  }

  private fun assertInterrupted(block: () -> Unit) {
    try {
      block()
      fail("The script was not stopped")
    } catch (e: LuaEngine.ScriptError) {
      assertTrue(e.message, e.interrupted)
    }
  }
}
