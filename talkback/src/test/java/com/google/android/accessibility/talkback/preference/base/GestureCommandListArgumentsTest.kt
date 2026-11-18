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
import com.google.android.accessibility.talkback.gesture.GestureProfiles
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Checks that the screen of a gesture's commands gets only a few values in its arguments, which go
 * into the activity's saved state, and builds the command list itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GestureCommandListArgumentsTest {

  private val context = RuntimeEnvironment.getApplication()

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
  }

  @Test
  fun argumentsLeaveOutTheCommandList() {
    val row = GestureListPreference(context, "pref_shortcut_up", "Swipe up", "UNASSIGNED")
    val args = GesturePreferenceFragmentCompat.createBundleForFragmentArguments(row)
    val parcel = Parcel.obtain()
    try {
      parcel.writeBundle(args)
      // The whole list was over 400 KB.
      assertTrue(parcel.dataSize() < 4_096)
    } finally {
      parcel.recycle()
    }
    assertTrue(GestureListPreference.buildActionItems(context, null).size > 100)
  }

  @Test
  fun listOfAGestureStartsWithUnassigned() {
    val first = GestureListPreference.buildActionItems(context, null).first()
    assertEquals(context.getString(R.string.shortcut_unassigned), first.text)
  }

  @Test
  fun listOfAProfileRowStartsWithItsDefaultRow() {
    val row =
      GestureListPreference(
        context,
        GestureProfiles.rowKey("b", "pref_shortcut_up"),
        "Swipe up",
        GestureProfiles.DEFAULT_VALUE,
      )
    val defaultText = row.defaultRowText
    assertEquals(context.getString(R.string.gesture_profile_default), defaultText)
    val items = GestureListPreference.buildActionItems(context, defaultText)
    assertEquals(defaultText, items[0].text)
    assertEquals(GestureProfiles.DEFAULT_VALUE, items[0].value)
    assertEquals(context.getString(R.string.shortcut_unassigned), items[1].text)
  }
}
