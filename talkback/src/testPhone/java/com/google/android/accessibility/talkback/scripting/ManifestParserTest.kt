/*
 * Copyright 2026 Backtalk contributors
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

import android.view.KeyEvent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManifestParserTest {
  private fun manifest(extra: String = ""): String =
    """{"id":"my-script","name":"My script","author":"Me","minApiVersion":1,"testedApiVersion":1$extra}"""

  private fun parse(extra: String = "", strict: Boolean = true): ScriptManifest =
    ScriptManifest.parse(manifest(extra), strict)

  private fun rejects(extra: String): String =
    assertThrows(ManifestException::class.java) { parse(extra) }.message.orEmpty()

  private fun rule(json: String): ScriptRule = ManifestParser(strict = true).rule(JSONObject(json))

  private fun rejectsRule(json: String): String =
    assertThrows(ManifestException::class.java) { rule(json) }.message.orEmpty()

  @Test
  fun theSmallestManifestIsAScriptForAllApps() {
    val parsed = parse()

    assertEquals("my-script", parsed.id)
    assertEquals("My script", parsed.name)
    assertEquals("1", parsed.version)
    assertEquals(1, parsed.testedApiVersion)
    assertTrue(parsed.isGlobal)
    assertTrue(parsed.permissions.isEmpty())
  }

  @Test
  fun anIdIsLowercaseWithoutSpaces() {
    val bad = manifest().replace("my-script", "My Script")

    val error = assertThrows(ManifestException::class.java) { ScriptManifest.parse(bad) }

    assertTrue("The id must be" in error.message.orEmpty())
  }

  @Test
  fun anImportedManifestNeedsAnAuthorAndTheApiVersions() {
    for (needed in listOf("author", "minApiVersion", "testedApiVersion")) {
      val without = JSONObject(manifest()).apply { remove(needed) }.toString()

      val error = assertThrows(ManifestException::class.java) { ScriptManifest.parse(without) }

      assertTrue(needed in error.message.orEmpty())
    }
  }

  @Test
  fun aStoredManifestIsReadEvenWithoutAnAuthorOrAnApiVersion() {
    val stored = ScriptManifest.parse("""{"id":"my-script","name":"My script"}""", strict = false)

    assertEquals("?", stored.author)
    assertEquals(1, stored.minApiVersion)
  }

  @Test
  fun testedApiVersionCannotBeLowerThanMinApiVersion() {
    val bad = """{"id":"s","name":"n","author":"a","minApiVersion":2,"testedApiVersion":1}"""

    val error = assertThrows(ManifestException::class.java) { ScriptManifest.parse(bad) }

    assertTrue("testedApiVersion cannot be lower" in error.message.orEmpty())
  }

  @Test
  fun permissionsAreReadByKey() {
    val parsed = parse(""","permissions":["screen","dialogs"]""")

    assertEquals(setOf(ScriptPermission.SCREEN, ScriptPermission.DIALOGS), parsed.permissions)
  }

  @Test
  fun anUnknownPermissionIsRejected() {
    assertTrue("camera" in rejects(""","permissions":["camera"]"""))
  }

  @Test
  fun appsAreAPackageNameOrAnObjectNamingAScreen() {
    val parsed =
      parse(""","apps":["com.example.one",{"package":"com.example.two","activity":".Main"}]""")

    assertFalse(parsed.isGlobal)
    assertEquals(listOf("com.example.one", "com.example.two"), parsed.apps.map { it.packageName })
    assertEquals(".Main", parsed.apps[1].activity)
    assertTrue(parsed.runsIn("com.example.two"))
    assertFalse(parsed.runsIn("com.example.three"))
  }

  @Test
  fun aHomepageIsAWebAddress() {
    assertEquals("https://example.com/s", parse(""","homepage":"https://example.com/s"""").homepage)
    assertTrue("homepage" in rejects(""","homepage":"file:///sdcard/s""""))
  }

  @Test
  fun aRuleNamesItsItemByIdOrMatch() {
    assertEquals("more", rule("""{"id":"more","hide":true}""").target)
    assertEquals(
      NodeQuery(className = "ImageView", clickable = true),
      rule("""{"match":{"className":"ImageView","clickable":true},"label":"More"}""").match,
    )
    assertTrue("id or match" in rejectsRule("""{"label":"More"}"""))
  }

  @Test
  fun aMatchCanNameAnItemTheItemIsInside() {
    assertEquals(
      NodeQuery(className = "Button", inside = NodeQuery(textContains = "play video")),
      rule("""{"match":{"className":"Button","inside":{"textContains":"play video"}},"hide":true}""")
        .match,
    )
    assertTrue(
      "inside is an object" in rejectsRule("""{"match":{"inside":"row"},"hide":true}""")
    )
    assertTrue("colour" in rejectsRule("""{"match":{"inside":{"colour":"red"}},"hide":true}"""))
    assertTrue(
      "another inside" in
        rejectsRule("""{"match":{"inside":{"id":"a","inside":{"id":"b"}}},"hide":true}""")
    )
  }

  @Test
  fun aRuleHasToChangeSomething() {
    assertTrue("needs label" in rejectsRule("""{"id":"more"}"""))
  }

  @Test
  fun hideIsTrueFalseOrAll() {
    assertEquals(Hide.SELF, rule("""{"id":"a","hide":true}""").hide)
    assertEquals(Hide.ALL, rule("""{"id":"a","hide":"all"}""").hide)
    assertEquals(Hide.NONE, rule("""{"id":"a","hide":false,"label":"A"}""").hide)
    assertTrue("hide is" in rejectsRule("""{"id":"a","hide":"some"}"""))
  }

  @Test
  fun unknownRuleFieldsAreRejected() {
    assertTrue("colour" in rejectsRule("""{"id":"a","label":"A","colour":"red"}"""))
  }

  @Test
  fun aRuleCannotBeReadBothBeforeAndAfter() {
    assertTrue("both" in rejectsRule("""{"id":"a","readBefore":"b","readAfter":"c"}"""))
  }

  @Test
  fun anUnknownRoleIsRejected() {
    assertEquals("button", rule("""{"id":"a","role":"button"}""").role)
    assertTrue("Unknown role gizmo" in rejectsRule("""{"id":"a","role":"gizmo"}"""))
  }

  @Test
  fun aRuleActionClicksAnItemNamedByIdOrMatch() {
    val actions =
      rule(
          """{"id":"title","actions":[
            {"title":"More","click":"menu"},
            {"title":"Share","longClick":{"contentDescription":"Share"}}]}"""
        )
        .actions

    assertEquals(RuleItemAction.Verb.CLICK, actions[0].verb)
    assertEquals(NodeQuery(id = "menu"), actions[0].target)
    assertEquals(ScriptPermission.ACTIONS, actions[0].permission)
    assertEquals(NodeQuery(contentDescription = "Share"), actions[1].target)
  }

  @Test
  fun aSpeakActionSaysTextOrAnItemsText() {
    val actions =
      rule(
          """{"id":"row","actions":[
            {"title":"Say hello","speak":"Hello"},
            {"title":"Say price","speak":{"textOf":"price"}}]}"""
        )
        .actions

    assertEquals("Hello", actions[0].text)
    assertNull(actions[0].permission)
    assertEquals(NodeQuery(id = "price"), actions[1].target)
    assertEquals(ScriptPermission.SCREEN, actions[1].permission)
  }

  @Test
  fun anActionNeedsATitleAndOneVerb() {
    assertTrue("needs a title" in rejectsRule("""{"id":"a","actions":[{"click":"b"}]}"""))
    assertTrue("needs one of" in rejectsRule("""{"id":"a","actions":[{"title":"T"}]}"""))
    assertTrue(
      "needs one of" in
        rejectsRule("""{"id":"a","actions":[{"title":"T","click":"b","focus":"c"}]}""")
    )
  }

  @Test
  fun commandsNeedTheInputPermission() {
    val command = """"commands":[{"id":"go","title":"Go","gesture":"swipeUp"}]"""

    assertTrue("input" in rejects(""",$command"""))
    assertEquals(listOf("swipeUp"), parse(""","permissions":["input"],$command""").commands[0].gestures)
  }

  @Test
  fun aCommandNeedsAWayToRunIt() {
    assertTrue(
      "needs gesture, keys, menu or control" in
        rejects(""","permissions":["input"],"commands":[{"id":"go","title":"Go"}]""")
    )
  }

  @Test
  fun aCommandsGestureAndKeysAreChecked() {
    val permission = """"permissions":["input"]"""
    assertTrue(
      "Unknown gesture wiggle" in
        rejects(""",$permission,"commands":[{"id":"go","title":"Go","gesture":"wiggle"}]""")
    )
    assertTrue(
      "must start with backtalk" in
        rejects(""",$permission,"commands":[{"id":"go","title":"Go","keys":"shift+n"}]""")
    )

    val keys =
      parse(""",$permission,"commands":[{"id":"go","title":"Go","keys":"backtalk+shift+n"}]""")
        .commands[0]
        .keys

    assertEquals(listOf(ScriptInput.Keys(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_N)), keys)
  }

  @Test
  fun commandIdsAreUnique() {
    val twice =
      """"commands":[{"id":"go","title":"Go","menu":true},{"id":"go","title":"Again","menu":true}]"""

    assertTrue("must be unique" in rejects(""","permissions":["input"],$twice"""))
  }

  @Test
  fun aListSettingNeedsOptionsAndDefaultsToTheFirst() {
    assertTrue(
      "needs options" in rejects(""","settings":[{"key":"voice","type":"list","title":"Voice"}]""")
    )

    val setting =
      parse(""","settings":[{"key":"voice","type":"list","title":"Voice","options":["a","b"]}]""")
        .settings[0]

    assertEquals("\"a\"", setting.defaultJson)
  }

  @Test
  fun aSwitchSettingDefaultsToOff() {
    val setting = parse(""","settings":[{"key":"loud","title":"Loud"}]""").settings[0]

    assertEquals(ScriptSetting.Type.SWITCH, setting.type)
    assertEquals("false", setting.defaultJson)
  }
}
