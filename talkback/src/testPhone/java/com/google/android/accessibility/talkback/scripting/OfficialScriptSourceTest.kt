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

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfficialScriptSourceTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val official = ScriptSource.parse(ScriptSources.OFFICIAL_SCRIPTS)!!

  private fun addresses(kind: SourceKind = SourceKind.SCRIPTS) =
    ScriptSources.all(context, kind).map { it.address }

  @Test
  fun theOfficialSourceIsListedAtFirst() {
    assertEquals(listOf(official.address), addresses())
  }

  @Test
  fun theOfficialSourceGoesBeforeSourcesAddedEarlier() {
    context
      .getSharedPreferences(SourceKind.SCRIPTS.prefs, Context.MODE_PRIVATE)
      .edit()
      .putString("sources", """["https://github.com/a/b"]""")
      .commit()
    assertEquals(
      listOf(official.address, ScriptSource.parse("github.com/a/b")!!.address),
      addresses(),
    )
  }

  @Test
  fun aRemovedOfficialSourceStaysRemoved() {
    ScriptSources.remove(context, ScriptSources.all(context).first())
    assertEquals(emptyList<String>(), addresses())
  }

  @Test
  fun soundThemeSourcesHaveNoOfficialSource() {
    assertEquals(emptyList<String>(), addresses(SourceKind.SOUND_THEMES))
  }
}
