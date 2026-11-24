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

package com.google.android.accessibility.talkback.actor.voicecommands

import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceTargetsTest {
  private val gmail = OpenAppActions.App("Gmail", "app:com.google.android.gm/.Main")
  private val notes = OpenAppActions.App("Notes", "app:com.example.notes/.Main")

  // The app category comes first, so the order of the categories doesn't decide the names.
  private val categories =
    listOf(
      Commands.Category(
        R.string.shortcut_title_open_app,
        listOf(
          Commands.Command(gmail.value, "Open Gmail", keyboardAction = null),
          Commands.Command(notes.value, "Open Notes", keyboardAction = null),
        ),
      ),
      Commands.Category(
        R.string.shortcut_title_basic_navigation,
        listOf(Commands.Command("notes_command", "Notes", keyboardAction = null)),
      ),
    )

  private val names = VoiceTargets.commandNames(categories, listOf(gmail, notes), Locale.ENGLISH)

  @Test
  fun appsGoByTheirBareNames() {
    assertEquals(gmail.value, names["gmail"]?.value)
  }

  @Test
  fun commandWinsOverAppOfTheSameName() {
    assertEquals("notes_command", names["notes"]?.value)
  }

  @Test
  fun listLabelsOfAppsAreLeftOut() {
    assertNull(names["open gmail"])
  }
}
