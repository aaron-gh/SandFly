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

package com.google.android.accessibility.talkback.preference.base

import android.content.Intent
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.ItemOrder
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ItemListFragmentTest {
  private val context = RuntimeEnvironment.getApplication()
  private lateinit var controller: ActivityController<TalkBackPreferencesActivity.TalkBackSubSettings>
  private lateinit var fragment: ItemListFragment
  private val order = ItemOrder.READING_CONTROLS_KEY
  private val textEntities by lazy { context.getString(R.string.pref_selector_text_entities_key) }
  private val option by lazy { context.getString(R.string.pref_text_entities_key) }

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    open(SelectorMenuFragment::class.java)
  }

  private fun open(fragmentClass: Class<out ItemListFragment>) {
    val intent =
      Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java)
        .putExtra(BasePreferencesActivity.FRAGMENT_NAME, fragmentClass.name)
    controller = Robolectric.buildActivity(TalkBackPreferencesActivity.TalkBackSubSettings::class.java, intent).setup()
    fragment = controller.get().supportFragmentManager.findFragmentById(com.google.android.accessibility.utils.R.id.preference_root) as ItemListFragment
  }

  @After
  fun tearDown() {
    controller.pause().stop().destroy()
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
  }

  private fun find(key: String): Preference? = fragment.findPreference(key)

  /** The keys of the rows of Current, in the order they are shown. */
  private fun currentRows(): List<String> {
    val current = find("${order}_current") as PreferenceGroup
    return (0 until current.preferenceCount).map { current.getPreference(it) }.sortedBy { it.order }.map { it.key }
  }

  @Test
  fun anOptionFollowsItsItemAndGoesWithIt() {
    // Addresses and numbers isn't in the list by default.
    assertNull(find(option))
    find("${order}_available_$textEntities")!!.performClick()
    shadowOf(Looper.getMainLooper()).idle()
    val rows = currentRows()
    val row = "${order}_row_$textEntities"
    assertEquals(rows.indexOf(row) + 1, rows.indexOf(option))

    find(row)!!.performClick()
    val dialog = ShadowDialog.getLatestDialog() as AlertDialog
    val list = dialog.listView
    val remove = (0 until list.adapter.count).first { list.adapter.getItem(it) == context.getString(R.string.quick_settings_remove) }
    shadowOf(list).performItemClick(remove)
    shadowOf(Looper.getMainLooper()).idle()
    assertNull(find(option))

    find("${order}_available_$textEntities")!!.performClick()
    shadowOf(Looper.getMainLooper()).idle()
    val added = currentRows()
    assertEquals(added.indexOf(row) + 1, added.indexOf(option))
    assertEquals(row, added[added.size - 2])
  }

  @Test
  fun anAddedItemsOptionIsEnabled() {
    controller.pause().stop().destroy()
    open(ContextMenuFragment::class.java)
    val navigation = context.getString(R.string.pref_show_context_menu_granularity_setting_key)
    val navigationOptions = context.getString(R.string.pref_show_context_menu_granularity_detail_setting_key)
    assertNull(find(navigationOptions))

    find("${ItemOrder.SANDFLY_MENU_KEY}_available_$navigation")!!.performClick()
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(find(navigationOptions)!!.isEnabled)
  }
}
