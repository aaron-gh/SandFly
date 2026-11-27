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

import android.os.Parcel
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.scripting.ScriptRuntime
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks the list of commands that a script's command setting offers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommandSettingListTest {

  private val context = RuntimeEnvironment.getApplication()

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
  }

  @Test
  fun theListStartsWithTheScriptsChoicesThenUnassigned() {
    val items =
      GestureListPreference.buildCommandSettingItems(
        context,
        arrayOf("Vibrate the time"),
        arrayOf("vibrate"),
        ScriptRuntime.runnableCommands(context),
      )

    assertEquals("Vibrate the time", items[0].text)
    assertEquals("vibrate", items[0].value)
    assertEquals(context.getString(R.string.shortcut_unassigned), items[1].text)
    assertEquals("", items[1].value)
    assertEquals(GestureListPreference.TYPE_TITLE, items[2].viewType)
  }

  @Test
  fun theListHasOnlyTheCommandsThatScriptsMayRun() {
    val categories = ScriptRuntime.runnableCommands(context)
    val values = categories.flatMap { it.commands }.map { it.value }

    assertTrue(values.size > 100)
    assertFalse("TOGGLE_SCRIPTS" in values)
    assertTrue(categories.all { it.commands.isNotEmpty() })
  }

  @Test
  fun theArgumentsLeaveOutTheCommandList() {
    val args =
      GesturePreferenceFragmentCompat.commandSettingArguments(
        "When unlocked",
        "TOGGLE_TOUCH_LOCK",
        "result",
        "onUnlock",
        arrayOf("Vibrate the time"),
        arrayOf("vibrate"),
      )
    val parcel = Parcel.obtain()
    try {
      parcel.writeBundle(args)
      assertTrue(parcel.dataSize() < 4_096)
    } finally {
      parcel.recycle()
    }
  }
}
