/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.scripting

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptShareTypesTest {
  @Test
  fun luaAndZipNamesAreTakenWhateverTheType() {
    assertTrue(ScriptShareTypes.acceptsFile("clock.lua", "text/plain"))
    assertTrue(ScriptShareTypes.acceptsFile("Clock.LUA", "application/octet-stream"))
    assertTrue(ScriptShareTypes.acceptsFile("clock.zip", null))
  }

  @Test
  fun otherNamesAreRejectedEvenWithAScriptType() {
    assertFalse(ScriptShareTypes.acceptsFile("notes.txt", "text/plain"))
    assertFalse(ScriptShareTypes.acceptsFile("photo.jpg", "application/octet-stream"))
    assertFalse(ScriptShareTypes.acceptsFile("clock.lua.txt", "text/x-lua"))
  }

  @Test
  fun filesWithoutANameNeedAScriptType() {
    assertTrue(ScriptShareTypes.acceptsFile(null, "text/x-lua"))
    assertTrue(ScriptShareTypes.acceptsFile("", "application/zip"))
    assertTrue(ScriptShareTypes.acceptsFile(null, "Application/X-Lua; charset=utf-8"))
    assertFalse(ScriptShareTypes.acceptsFile(null, "text/plain"))
    assertFalse(ScriptShareTypes.acceptsFile(null, "application/octet-stream"))
    assertFalse(ScriptShareTypes.acceptsFile(null, null))
  }

  @Test
  fun linksMustBeToALuaOrZipFile() {
    assertTrue(
      ScriptShareTypes.isScriptLink("https://raw.githubusercontent.com/a/b/HEAD/clock.lua")
    )
    assertTrue(ScriptShareTypes.isScriptLink("https://example.com/clock.zip?download=1"))
    assertFalse(ScriptShareTypes.isScriptLink("https://example.com/news/article"))
    assertFalse(ScriptShareTypes.isScriptLink("https://example.com/page?file=clock.lua"))
    assertFalse(ScriptShareTypes.isScriptLink("https://example.com/a b.lua"))
  }
}
