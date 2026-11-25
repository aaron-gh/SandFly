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

package com.google.android.accessibility.braille.common

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleDisplayKeyPreferencesTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun overridesStayWithTheirDisplayProfile() {
    val prefs = BrailleUserPreferences.getSharedPreferences(
      context, BrailleUserPreferences.BRAILLE_SHARED_PREFS_FILENAME
    )
    val profileKey = context.getString(R.string.pref_bd_command_profile)
    val sandFlyOverride = "v1;s1=c3"
    val iosOverride = "v1;c213=-"
    prefs.edit().putString("pref_bd_key_overrides", sandFlyOverride).commit()

    prefs.edit().putString(profileKey, "ios").commit()
    assertNull(BrailleUserPreferences.readDisplayKeyOverridesText(context))
    BrailleUserPreferences.writeDisplayKeyOverrides(context, DisplayKeyOverrides.parse(iosOverride))
    assertEquals(iosOverride, BrailleUserPreferences.readDisplayKeyOverridesText(context))

    prefs.edit().putString(profileKey, "sandfly").commit()
    assertEquals(sandFlyOverride, BrailleUserPreferences.readDisplayKeyOverridesText(context))
    BrailleUserPreferences.writeDisplayKeyOverrides(context, DisplayKeyOverrides.parse(null))
    assertNull(BrailleUserPreferences.readDisplayKeyOverridesText(context))

    prefs.edit().putString(profileKey, "ios").commit()
    assertEquals(iosOverride, BrailleUserPreferences.readDisplayKeyOverridesText(context))
  }
}
