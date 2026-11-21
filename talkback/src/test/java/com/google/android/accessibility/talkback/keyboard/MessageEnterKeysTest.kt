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

package com.google.android.accessibility.talkback.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** How Enter finds a messaging app's Send button. */
class MessageEnterKeysTest {
  @Test
  fun sendButtonsByViewId() {
    assertTrue(MessageEnterKeys.isSendId("com.whatsapp:id/send"))
    assertTrue(MessageEnterKeys.isSendId("org.thoughtcrime.securesms:id/send_button"))
    assertTrue(MessageEnterKeys.isSendId("Compose:Draft:Send"))
    assertFalse(MessageEnterKeys.isSendId("com.whatsapp:id/sender_name"))
    assertFalse(MessageEnterKeys.isSendId(null))
  }

  @Test
  fun sendButtonsByLabel() {
    assertTrue(MessageEnterKeys.isSendLabel("Send"))
    assertTrue(MessageEnterKeys.isSendLabel(" Send message "))
    assertFalse(MessageEnterKeys.isSendLabel("Sent"))
    assertFalse(MessageEnterKeys.isSendLabel(null))
  }

  @Test
  fun onlyMessagingApps() {
    assertTrue(MessageEnterKeys.isMessagingApp("com.whatsapp"))
    assertFalse(MessageEnterKeys.isMessagingApp("com.google.android.gm"))
  }
}
