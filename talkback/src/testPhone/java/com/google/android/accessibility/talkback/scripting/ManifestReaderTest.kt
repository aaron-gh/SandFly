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

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestReaderTest {
  private fun read(source: String) = JSONObject(ManifestReader.read(source))

  @Test
  fun readsTheTable() {
    val manifest =
      read(
        """
        -- manifest = { id = 'not this one' }
        --[[ manifest = { id = 'nor this' } ]]
        local manifest = {
          id = 'hello',
          name = "Hello \"there\"\n",
          minApiVersion = 5,
          ratio = 0.5,
          offset = -3,
          apps = { 'com.example.a', { package = 'com.example.b', activity = '.Main' } },
          permissions = { 'speech'; 'screen' },
          description = [[
        Two
        lines]],
          ['odd key'] = true,
          empty = {},
        }

        sf.speak('hi')
        """
      )
    assertEquals("hello", manifest.getString("id"))
    assertEquals("Hello \"there\"\n", manifest.getString("name"))
    assertEquals(5, manifest.getInt("minApiVersion"))
    assertEquals(0.5, manifest.getDouble("ratio"), 0.0)
    assertEquals(-3, manifest.getInt("offset"))
    assertEquals("com.example.a", manifest.getJSONArray("apps").getString(0))
    assertEquals(".Main", manifest.getJSONArray("apps").getJSONObject(1).getString("activity"))
    assertEquals(2, manifest.getJSONArray("permissions").length())
    assertEquals("        Two\n        lines", manifest.getString("description"))
    assertEquals(true, manifest.getBoolean("odd key"))
    assertEquals(0, manifest.getJSONArray("empty").length())
  }

  @Test
  fun aGlobalManifestWorksToo() {
    assertEquals("x", read("manifest = { id = 'x' }").getString("id"))
  }

  @Test
  fun unicodeEscapes() {
    assertEquals("é😀", read("""local manifest = { name = "\u{E9}\u{1F600}" }""").getString("name"))
  }

  @Test(expected = ManifestException::class)
  fun codeIsRefused() {
    read("local manifest = { name = string.upper('x') }")
  }

  @Test(expected = ManifestException::class)
  fun concatenationIsRefused() {
    read("local manifest = { name = 'a' .. 'b' }")
  }

  @Test(expected = ManifestException::class)
  fun aMixedTableIsRefused() {
    read("local manifest = { apps = { 'a', x = 1 } }")
  }

  @Test(expected = ManifestException::class)
  fun aFieldOfAnotherTableIsNotTheManifest() {
    read("config.manifest = { id = 'x' }")
  }

  @Test(expected = ManifestException::class)
  fun aScriptWithoutAManifestIsRefused() {
    read("sf.speak('hi')")
  }

  @Test(expected = ManifestException::class)
  fun anUnclosedTableIsRefused() {
    read("local manifest = { id = 'x'")
  }
}
