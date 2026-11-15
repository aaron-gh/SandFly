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

package com.google.android.accessibility.talkback.granularediting

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CharacterCountTest {
  private val context = RuntimeEnvironment.getApplication()

  @Suppress("DEPRECATION") // obtain(): a node to give text to.
  private fun editBox(text: String?, password: Boolean = false, hint: Boolean = false) =
    AccessibilityNodeInfoCompat.obtain().apply {
      className = "android.widget.EditText"
      isEditable = true
      this.text = text
      isPassword = password
      isShowingHintText = hint
    }

  @Test
  fun charactersCountWhatIsSeen() {
    assertEquals(0, CharacterCount.characters(""))
    assertEquals(5, CharacterCount.characters("ab cd"))
    assertEquals(1, CharacterCount.characters("e\u0301"))
    assertEquals(2, CharacterCount.characters("a\uD83D\uDE00"))
  }

  @Test
  fun wordsLeaveOutSpacesAndPunctuation() {
    assertEquals(0, CharacterCount.words(""))
    assertEquals(0, CharacterCount.words("  ... !"))
    assertEquals(4, CharacterCount.words("Hello, world! It's 42."))
  }

  @Test
  fun saysCharactersAndWords() {
    assertEquals("11 characters, 2 words", CharacterCount.describe(context, editBox("Hello world")))
    assertEquals("1 character, 1 word", CharacterCount.describe(context, editBox("a")))
    assertEquals("0 characters, 0 words", CharacterCount.describe(context, editBox(null)))
  }

  @Test
  fun hintCountsAsNoText() {
    assertEquals(
      "0 characters, 0 words",
      CharacterCount.describe(context, editBox("Message", hint = true)),
    )
  }

  @Test
  fun passwordSaysOnlyCharacters() {
    assertEquals("3 characters", CharacterCount.describe(context, editBox("abc", password = true)))
  }
}
