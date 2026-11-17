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

package com.google.android.accessibility.talkback.preference

import android.content.Intent
import android.os.Looper
import com.google.android.accessibility.talkback.preference.base.SelectorMenuFragment
import com.google.android.accessibility.talkback.preference.base.TalkBackPreferenceFragment
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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

/** Checks that opening the main settings screen again leaves nothing behind it for Back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainScreenIntentTest {
  private val context = RuntimeEnvironment.getApplication()
  private lateinit var controller: ActivityController<TalkBackPreferencesActivity.TalkBackSubSettings>

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
  }

  @After
  fun tearDown() {
    controller.pause().stop().destroy()
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
  }

  private fun intent(fragmentName: String? = null): Intent =
    Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java).apply {
      fragmentName?.let { putExtra(BasePreferencesActivity.FRAGMENT_NAME, it) }
    }

  private fun shown() =
    controller.get().supportFragmentManager.findFragmentById(R.id.preference_root)

  private fun backStack() = controller.get().supportFragmentManager.backStackEntryCount

  @Test
  fun appIconOnTheMainScreenKeepsIt() {
    controller =
      Robolectric.buildActivity(TalkBackPreferencesActivity.TalkBackSubSettings::class.java, intent())
        .setup()
    val main = shown()
    assertTrue(main is TalkBackPreferenceFragment)

    controller.newIntent(intent())
    shadowOf(Looper.getMainLooper()).idle()

    assertSame(main, shown())
    assertEquals(0, backStack())
  }

  @Test
  fun appIconOnAnotherScreenShowsTheMainScreenWithNothingBehindIt() {
    controller =
      Robolectric.buildActivity(
          TalkBackPreferencesActivity.TalkBackSubSettings::class.java,
          intent(SelectorMenuFragment::class.java.name),
        )
        .setup()

    controller.newIntent(intent())
    shadowOf(Looper.getMainLooper()).idle()

    assertTrue(shown() is TalkBackPreferenceFragment)
    assertEquals(0, backStack())
  }

  @Test
  fun anotherScreenOpensOverTheOneShown() {
    controller =
      Robolectric.buildActivity(TalkBackPreferencesActivity.TalkBackSubSettings::class.java, intent())
        .setup()

    controller.newIntent(intent(SelectorMenuFragment::class.java.name))
    shadowOf(Looper.getMainLooper()).idle()

    assertTrue(shown() is SelectorMenuFragment)
    assertEquals(1, backStack())
  }
}
