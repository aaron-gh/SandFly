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
}
