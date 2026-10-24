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

package com.google.android.accessibility.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramChatCellTest {
  private val telegram = "org.telegram.messenger"
  private val viewGroup = "android.view.ViewGroup"

  @Test
  fun aChatRowIsDescribedOnFocus() {
    assertTrue(TelegramChatCell.isDescribedOnFocus(telegram, viewGroup, null, 0))
    assertTrue(TelegramChatCell.isDescribedOnFocus("tw.nekomimi.nekogram", viewGroup, "", 0))
    assertTrue(
      TelegramChatCell.isDescribedOnFocus(telegram, "android.widget.CheckBox", null, 0)
    )
  }

  @Test
  fun aMessageOrOtherViewIsNot() {
    // A message in a chat has its text.
    assertFalse(TelegramChatCell.isDescribedOnFocus(telegram, viewGroup, "Ronan\nHi", 0))
    assertFalse(TelegramChatCell.isDescribedOnFocus(telegram, viewGroup, null, 2))
    assertFalse(TelegramChatCell.isDescribedOnFocus(telegram, "android.widget.TextView", null, 0))
  }

  @Test
  fun onlyTelegramApps() {
    assertFalse(TelegramChatCell.isDescribedOnFocus("com.whatsapp", viewGroup, null, 0))
    assertFalse(TelegramChatCell.isDescribedOnFocus(null, viewGroup, null, 0))
  }
}
