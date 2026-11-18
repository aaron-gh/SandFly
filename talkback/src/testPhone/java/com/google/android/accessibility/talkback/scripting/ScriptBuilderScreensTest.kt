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

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** Makes a rule in the script builder's screens, as a user would. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScriptBuilderScreensTest {
  private val context = RuntimeEnvironment.getApplication()
  private lateinit var id: String

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    id = ScriptBuilder.create(context, "Social")
  }

  private fun idle() = shadowOf(Looper.getMainLooper()).idle()

  private val activities = mutableListOf<android.app.Activity>()

  private fun open(intent: Intent): PreferenceFragmentCompat {
    val activity =
      Robolectric.buildActivity(TalkBackPreferencesActivity.TalkBackSubSettings::class.java, intent).setup().get()
    activities += activity
    idle()
    return activity.supportFragmentManager.findFragmentById(com.google.android.accessibility.utils.R.id.preference_root)
      as PreferenceFragmentCompat
  }

  private fun open(fragment: Class<*>, args: Bundle): PreferenceFragmentCompat =
    open(
      Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java)
        .putExtra(BasePreferencesActivity.FRAGMENT_NAME, fragment.name)
        .putExtra(BasePreferencesActivity.FRAGMENT_ARGS, args)
    )

  /** The screen shown now, after a click opened one or Back closed one. */
  private fun current(): PreferenceFragmentCompat {
    val activity = activities.last() as androidx.fragment.app.FragmentActivity
    idle()
    return activity.supportFragmentManager.findFragmentById(com.google.android.accessibility.utils.R.id.preference_root)
      as PreferenceFragmentCompat
  }

  private fun back(): PreferenceFragmentCompat {
    (activities.last() as androidx.fragment.app.FragmentActivity).supportFragmentManager.popBackStackImmediate()
    return current()
  }

  private fun all(group: PreferenceGroup): List<Preference> =
    (0 until group.preferenceCount).flatMap {
      val preference = group.getPreference(it)
      listOf(preference) + if (preference is PreferenceGroup) all(preference) else emptyList()
    }

  private fun PreferenceFragmentCompat.row(title: String): Preference =
    all(preferenceScreen).firstOrNull { it.title?.toString() == title }
      ?: throw AssertionError("No row $title in ${all(preferenceScreen).map { it.title }}")

  private fun PreferenceFragmentCompat.click(title: String) {
    row(title).performClick()
    idle()
  }

  private fun enterText(text: String) {
    val dialog = ShadowDialog.getLatestDialog() as AlertDialog
    fun edit(view: View): EditText? =
      view as? EditText ?: (view as? ViewGroup)?.let { group -> (0 until group.childCount).firstNotNullOfOrNull { edit(group.getChildAt(it)) } }
    edit(dialog.window!!.decorView)!!.setText(text)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
    idle()
  }

  private fun chooseRow(title: String) {
    val dialog = ShadowDialog.getLatestDialog() as AlertDialog
    val list = dialog.listView
    val index = (0 until list.adapter.count).first { list.adapter.getItem(it).toString() == title }
    list.performItemClick(list.adapter.getView(index, null, list), index, index.toLong())
    idle()
  }

  @Test
  fun ruleMadeFromTheInspectedItemIsInstalled() {
    val button =
      AccessibilityNodeInfo.obtain().apply {
        contentDescription = "New post"
        isClickable = true
        isVisibleToUser = true
      }
    ItemInspector.report(context, AccessibilityNodeInfoCompat.wrap(button))

    val page = open(ScriptBuilderFragment::class.java, Bundle().apply { putString(ScriptScreenFragment.ARG_ID, id) })
    page.click("Add rule")
    assertEquals(1, ScriptBuilder.draft(ScriptStore.get(context), id)!!.getJSONArray("rules").length())
    val rule = current()
    assertTrue(rule is ScriptRuleFragment)
    rule.click("Item")
    val item = current()
    assertTrue(item is ScriptQueryFragment)
    item.click("Use the inspected item")

    back().click("Label")
    enterText("Compose")

    val installed = ScriptStore.get(context).find(id)!!.manifest
    assertEquals(1, installed.rules.size)
    assertEquals("Compose", installed.rules[0].label)
    assertNotNull(installed.rules[0].match)
    assertEquals("New post", installed.rules[0].match!!.contains)
    assertEquals(setOf(ScriptPermission.SPEECH), installed.permissions)
  }

  @Test
  fun addRuleForInspectedItem_makesAScriptForItsApp() {
    val button =
      AccessibilityNodeInfo.obtain().apply {
        contentDescription = "New post"
        isClickable = true
        isVisibleToUser = true
        packageName = "com.example.social"
      }
    ItemInspector.report(context, AccessibilityNodeInfoCompat.wrap(button))

    val pick = open(ScriptRulePickFragment::class.java, Bundle())
    pick.click("New script for com.example.social")
    assertTrue(current() is ScriptRuleFragment)
    current().click("Label")
    enterText("Compose")

    val script = ScriptStore.get(context).all().first { it.manifest.name == "com.example.social" }
    assertEquals(listOf("com.example.social"), script.manifest.apps.map { it.packageName })
    assertEquals("Compose", script.manifest.rules.single().label)
    assertEquals("New post", script.manifest.rules.single().match!!.contains)
  }

  @Test
  fun unfinishedRule_saysWhatItNeeds() {
    val page = open(ScriptBuilderFragment::class.java, Bundle().apply { putString(ScriptScreenFragment.ARG_ID, id) })
    page.click("Add rule")
    assertTrue(all(current().preferenceScreen).any { it.title?.toString() == "Not used: set the item" })
  }

  @Test
  fun everyBuilderScreenShowsAFullDraft() {
    val store = ScriptStore.get(context)
    val draft = ScriptBuilder.draft(store, id)!!
    val macro = org.json.JSONObject(BuiltScriptMacros.macroJson(com.google.android.accessibility.talkback.macros.Macros.Macro("m", "", emptyList())))
    draft
      .put("settings", org.json.JSONArray().put(org.json.JSONObject().put("key", "s1").put("type", "list").put("title", "Mode").put("options", org.json.JSONArray().put("a").put("b"))))
      .put("commands", org.json.JSONArray().put(org.json.JSONObject().put("id", "c1").put("title", "Go").put("gesture", "swipeUpThenRight").put("macro", macro)))
      .put(
        "triggers",
        org.json.JSONArray()
          .put(org.json.JSONObject().put("id", "t1").put("when", "daily").put("time", "08:30").put("macro", macro))
          .put(org.json.JSONObject().put("id", "t2").put("when", "click").put("macro", macro)),
      )
      .put("speech", org.json.JSONArray().put(org.json.JSONObject().put("when", "notification").put("change", "replacePart").put("find", "a").put("text", "b")))
      .put(
        "rules",
        org.json.JSONArray().put(
          org.json.JSONObject().put("match", org.json.JSONObject().put("text", "OK").put("checked", true).put("inside", org.json.JSONArray().put(org.json.JSONObject().put("id", "x")))).put("label", "L")
        ),
      )
      .put("description", "D")
    ScriptBuilder.save(context, draft)
    fun args(block: Bundle.() -> Unit) = Bundle().apply { putString(ScriptScreenFragment.ARG_ID, id); block() }

    val page = open(ScriptBuilderFragment::class.java, args {})
    for (title in listOf("Every day at 08:30", "When an item is clicked", "Mode", "Go", "Description", "Lua code", "Duplicate script")) page.row(title)
    open(ScriptBuiltCommandFragment::class.java, args { putInt(ScriptBuiltCommandFragment.ARG_INDEX, 0) }).row("Gesture")
    open(ScriptSpeechChangeFragment::class.java, args { putInt(ScriptSpeechChangeFragment.ARG_INDEX, 0) }).row("Replace a with b")
    val query = open(ScriptQueryFragment::class.java, args { putString(ScriptQueryFragment.ARG_PATH, Paths.encode(listOf("rules", 0, "match"))) })
    query.row("Exact text")
    query.row("States")
    query.row("Add item it is inside")
    open(ScriptRuleFragment::class.java, args { putInt(ScriptRuleFragment.ARG_INDEX, 0) }).row("Duplicate rule")
  }

  @Test
  fun conditionsFromTheInspectedItemCanBeUnset() {
    val button =
      AccessibilityNodeInfo.obtain().apply {
        contentDescription = "New post"
        isClickable = true
        isVisibleToUser = true
      }
    ItemInspector.report(context, AccessibilityNodeInfoCompat.wrap(button))
    val page = open(ScriptBuilderFragment::class.java, Bundle().apply { putString(ScriptScreenFragment.ARG_ID, id) })
    page.click("Add rule")
    current().click("Item")
    current().click("Use the inspected item")
    fun match() = ScriptBuilder.draft(ScriptStore.get(context), id)!!.getJSONArray("rules").getJSONObject(0).getJSONObject("match")
    assertEquals("contentDescription", match().optString("field"))
    assertEquals("clickable", match().optString("kind"))

    current().click("Item type")
    chooseRow("Remove")
    assertTrue(match().toString(), !match().has("kind"))

    current().click("Text")
    chooseRow("Remove")
    assertTrue(match().toString(), !match().has("contains") && !match().has("field"))

    // An unset condition opens its dialog at once.
    current().click("Text")
    enterText("Post")
    assertEquals("Post", match().optString("contains"))
    current().click("Text")
    chooseRow("Edit")
    enterText("")
    assertTrue(match().toString(), !match().has("contains"))
  }

  @Test
  fun inspectedRows_makeOneItemWithActionsAndChooseWhatIsRead() {
    com.google.android.accessibility.utils.SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString("item_inspector_last_sample", ItemPatternTest.friends.toString())
      .commit()
    id = ScriptBuilder.create(context, "Friends tab")
    val pick = open(ScriptRulePickFragment::class.java, Bundle())
    assertTrue(all(pick.preferenceScreen).any { it.summary?.toString()?.contains("Matches the inspected item and 2 items like it") == true })
    pick.click("Friends tab")
    val rule = current()
    assertTrue(rule is ScriptRuleFragment)
    rule.click("Read as one item, with its buttons as actions")

    val draft = ScriptBuilder.draft(ScriptStore.get(context), id)!!
    val saved = draft.getJSONArray("rules").getJSONObject(0)
    assertEquals(true, saved.getBoolean("group"))
    assertEquals(listOf("Add Friend", "Remove"), saved.getJSONArray("actions").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("title") } })
    assertEquals("Natalie Anderson, 3 mutual friends", current().row("Choose what is read").summary.toString())

    current().click("Choose what is read")
    val dialog = ShadowDialog.getLatestDialog() as AlertDialog
    dialog.listView.performItemClick(dialog.listView, 1, 1)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
    idle()
    assertEquals("Natalie Anderson", current().row("Choose what is read").summary.toString())

    val installed = ScriptStore.get(context).find(id)!!.manifest
    assertEquals(1, installed.rules.size)

    // The speech change follows the rule's item when it changes.
    current().click("Item")
    current().click("Class name")
    chooseRow("Remove")
    val changed = ScriptBuilder.draft(ScriptStore.get(context), id)!!
    val match = changed.getJSONArray("rules").getJSONObject(0).getJSONObject("match")
    assertTrue(!match.has("className"))
    assertEquals(match.toString(), changed.getJSONArray("speech").getJSONObject(0).getJSONObject("match").toString())
  }

  @Test
  fun scriptWrittenBySandFlyPro_isReviewedInTheBuilder() {
    val source = "local manifest = { name = 'Episodes', version = '1', permissions = { 'speech', 'network' }, rules = { { match = { id = 'row' }, label = 'Episode' } } }\n"
    val saved = ScriptAi.save(context, null, source)
    val scriptId = saved.id!!
    ScriptAi.setReview(context, scriptId, "Reads each episode row as one item.")
    val page = open(ScriptBuilderFragment::class.java, Bundle().apply { putString(ScriptScreenFragment.ARG_ID, scriptId) })
    page.row("Reads each episode row as one item.")
    page.row("Not allowed until accepted: network")
    page.click("Accept")
    assertEquals(null, ScriptAi.review(context, scriptId))
    assertTrue(ScriptPermission.NETWORK in ScriptStore.get(context).find(scriptId)!!.granted)
    assertTrue(all(current().preferenceScreen).none { it.title?.toString() == "Accept" })
  }

  @Test
  fun changingARule_keepsItsRowsInPlace() {
    val page = open(ScriptBuilderFragment::class.java, Bundle().apply { putString(ScriptScreenFragment.ARG_ID, id) })
    page.click("Add rule")
    val rule = current()
    val group = rule.row("Read as one item") as androidx.preference.CheckBoxPreference
    val label = rule.row("Label")
    group.performClick()
    idle()
    // The same rows, updated, so that accessibility focus stays on the checkbox.
    assertTrue(rule.row("Read as one item") === group)
    assertTrue(group.isChecked)
    assertTrue(rule.row("Label") === label)
    assertTrue(label.isEnabled)
    assertEquals(true, ScriptBuilder.draft(ScriptStore.get(context), id)!!.getJSONArray("rules").getJSONObject(0).getBoolean("group"))
    // A row that only some states have comes and goes.
    assertTrue(all(rule.preferenceScreen).none { it.title?.toString() == "Not used: set the item" } ||
      all(rule.preferenceScreen).count { it.title?.toString()?.startsWith("Not used") == true } == 1)
  }
}
