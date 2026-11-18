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

package com.google.android.accessibility.talkback.speechdictionary

import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.Replacement
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import org.junit.After
import org.junit.Assert.assertEquals
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DictionaryRowActionsTest {
  private val context = RuntimeEnvironment.getApplication()
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(context) }
  private lateinit var controller: ActivityController<TalkBackPreferencesActivity.TalkBackSubSettings>
  private lateinit var fragment: PreferenceFragmentCompat

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    prefs.edit().clear().commit()
  }

  @After
  fun tearDown() {
    if (::controller.isInitialized) controller.pause().stop().destroy()
    prefs.edit().clear().commit()
  }

  private fun open(fragmentClass: Class<out DictionaryFragment>) {
    val intent =
      Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java)
        .putExtra(BasePreferencesActivity.FRAGMENT_NAME, fragmentClass.name)
    controller =
      Robolectric.buildActivity(TalkBackPreferencesActivity.TalkBackSubSettings::class.java, intent).setup()
    fragment =
      controller.get().supportFragmentManager.findFragmentById(
        com.google.android.accessibility.utils.R.id.preference_root
      ) as PreferenceFragmentCompat
    layOut()
  }

  private fun layOut() {
    shadowOf(Looper.getMainLooper()).idle()
    val list = fragment.listView
    list.measure(
      View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(4000, View.MeasureSpec.EXACTLY),
    )
    list.layout(0, 0, 1080, 4000)
    shadowOf(Looper.getMainLooper()).idle()
  }

  /** The shown row whose text includes [text]. */
  private fun row(text: String): View {
    val list = fragment.listView
    return (0 until list.childCount)
      .map { list.getChildAt(it) }
      .first { view ->
        val texts = mutableListOf<CharSequence?>()
        fun collect(v: View) {
          if (v is android.widget.TextView) texts += v.text
          if (v is android.view.ViewGroup) (0 until v.childCount).forEach { collect(v.getChildAt(it)) }
        }
        collect(view)
        texts.any { it?.contains(text) == true }
      }
  }

  private fun actions(view: View): Map<String, Int> {
    val info = AccessibilityNodeInfo.obtain()
    view.onInitializeAccessibilityNodeInfo(info)
    return info.actionList.filter { it.label != null }.associate { it.label.toString() to it.id }
  }

  private fun entry(find: String, replace: String) =
    DictionaryEntry(Replacement(find, replace, regex = false, wholeWord = false))

  @Test
  fun symbolRowsOfferEditChooseAppAndRemove() {
    SymbolDictionary.write(prefs, listOf(entry("#", "hash")))
    open(SymbolDictionaryFragment::class.java)
    val labels = actions(row("hash")).keys
    assertTrue(labels.toString(), context.getString(R.string.notification_edit) in labels)
    assertTrue(labels.toString(), context.getString(R.string.notification_choose_app) in labels)
    assertTrue(labels.toString(), context.getString(R.string.notification_remove) in labels)
  }

  @Test
  fun removeActsOnItsOwnRowAfterTheListChanged() {
    SpeechDictionary.write(prefs, listOf(entry("one", "first"), entry("two", "second"), entry("three", "third")))
    open(SpeechDictionaryFragment::class.java)
    val remove = context.getString(R.string.notification_remove)

    val second = row("second")
    assertTrue(second.performAccessibilityAction(actions(second).getValue(remove), null))
    layOut()
    assertEquals(listOf("one", "three"), SpeechDictionary.read(prefs).map { it.replacement.find })

    // The third entry is now second; its action removes it, not the entry that was second before.
    val third = row("third")
    assertTrue(third.performAccessibilityAction(actions(third).getValue(remove), null))
    layOut()
    assertEquals(listOf("one"), SpeechDictionary.read(prefs).map { it.replacement.find })
  }
}
