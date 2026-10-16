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

package com.google.android.accessibility.talkback.soundthemes

import com.google.android.accessibility.talkback.soundthemes.ThemeLayout.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeLayoutTest {
  @Test
  fun aThemeAtTheTop() {
    val layout = ThemeLayout.of(listOf("theme.json", "focus.wav", "COPYING.txt", "art/cover.png"))
    assertEquals("theme.json", layout.manifest)
    assertEquals(mapOf("focus.wav" to Role.SOUND, "COPYING.txt" to Role.DOCUMENT), layout.files)
  }

  @Test
  fun aThemeInARepositoryFolder() {
    // As GitHub puts a repository in a ZIP file.
    val layout =
      ThemeLayout.of(
        listOf(
          "my-theme-main/README.md",
          "my-theme-main/theme.json",
          "my-theme-main/tick.ogg",
          "my-theme-main/source/tick.wav",
        )
      )
    assertEquals("my-theme-main/theme.json", layout.manifest)
    assertEquals(
      mapOf("my-theme-main/README.md" to Role.DOCUMENT, "my-theme-main/tick.ogg" to Role.SOUND),
      layout.files,
    )
  }

  @Test
  fun soundsAloneFromAnyFolder() {
    val layout = ThemeLayout.of(listOf("sounds/focus.wav", "tick.mp3", "__MACOSX/._tick.mp3"))
    assertNull(layout.manifest)
    assertEquals(mapOf("sounds/focus.wav" to Role.SOUND, "tick.mp3" to Role.SOUND), layout.files)
  }
}
