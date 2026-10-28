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

package com.google.android.accessibility.talkback.braille

import com.google.android.accessibility.talkback.braille.SendButtonFinder.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendButtonFinderTest {
  private val sendWords = listOf("Send", "Envoyer")

  @Test
  fun viewIdsOfChatAppSendButtons_areSendButtons() {
    assertTrue(SendButtonFinder.isSendViewId("com.whatsapp:id/send"))
    assertTrue(
      SendButtonFinder.isSendViewId("com.google.android.apps.messaging:id/send_message_button_icon")
    )
    assertTrue(SendButtonFinder.isSendViewId("Compose:Draft:Send"))
    assertTrue(SendButtonFinder.isSendViewId("org.example:id/sendButton"))
  }

  @Test
  fun viewIdsThatOnlyContainSend_areNotSendButtons() {
    assertFalse(SendButtonFinder.isSendViewId(null))
    assertFalse(SendButtonFinder.isSendViewId("com.whatsapp:id/sender_name"))
    assertFalse(SendButtonFinder.isSendViewId("com.whatsapp:id/voice_note_btn"))
    assertFalse(SendButtonFinder.isSendViewId("org.example:id/resend_code"))
  }

  @Test
  fun labelThatIsASendWord_isStrong() {
    assertEquals(Strength.STRONG, SendButtonFinder.labelStrength("Send", sendWords))
    assertEquals(Strength.STRONG, SendButtonFinder.labelStrength(" send ", sendWords))
    assertEquals(Strength.STRONG, SendButtonFinder.labelStrength("Envoyer", sendWords))
  }

  @Test
  fun labelThatStartsWithASendWord_isWeak() {
    assertEquals(Strength.WEAK, SendButtonFinder.labelStrength("Send SMS", sendWords))
    assertEquals(
      Strength.WEAK,
      SendButtonFinder.labelStrength("Send end-to-end encrypted message", sendWords),
    )
  }

  @Test
  fun otherLabels_areNotSendButtons() {
    assertEquals(Strength.NONE, SendButtonFinder.labelStrength(null, sendWords))
    assertEquals(Strength.NONE, SendButtonFinder.labelStrength("Sender", sendWords))
    assertEquals(Strength.NONE, SendButtonFinder.labelStrength("Voice message", sendWords))
    assertEquals(
      Strength.NONE,
      SendButtonFinder.labelStrength(
        "Send me the photos from the weekend when you get a chance please",
        sendWords,
      ),
    )
  }
}
