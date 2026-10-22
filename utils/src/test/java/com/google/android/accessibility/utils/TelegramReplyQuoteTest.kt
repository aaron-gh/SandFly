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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TelegramReplyQuoteTest {
  private val format = "replying to %1\$s: %2\$s"

  @Test
  fun parsesTheSenderAndText() {
    val quote = TelegramReplyQuote.parse("Reply, Ronan, What other features you love? To me, honestly…")
    assertEquals("Ronan", quote?.name)
    assertEquals("What other features you love? To me, honestly…", quote?.text)
    assertNull(TelegramReplyQuote.parse("Forwarded from\nRonan"))
    assertNull(TelegramReplyQuote.parse("Reply, Ronan"))
  }

  @Test
  fun theQuoteComesLast() {
    val text = "G Melconian\nthat has been on the pixel dialer\nReceived at 00:53"
    val quote = TelegramReplyQuote.Quote("Ronan", "What other features you love?")
    assertEquals(
      "G Melconian\nthat has been on the pixel dialer\nReceived at 00:53, replying to Ronan: What other features you love?",
      TelegramReplyQuote.withQuote(text, quote, format, ", ").toString(),
    )
  }

  @Test
  fun theWholeQuotedTextComesFromTheQuotedMessage() {
    val quoted = "Ronan\nWhat other features you love? To me to be honest I can't live\nwithout the edge gestures.\nReceived at 21:32"
    assertEquals(
      "What other features you love? To me to be honest I can't live without the edge gestures.",
      TelegramReplyQuote.fullText(quoted, "What other features you love? To me to be honest I ca"),
    )
    assertNull(TelegramReplyQuote.fullText(quoted, "Something else entirely"))
  }

  @Test
  fun onlyTelegramApps() {
    assertTrue(TelegramReplyQuote.isTelegram("org.telegram.messenger"))
    assertTrue(TelegramReplyQuote.isTelegram("tw.nekomimi.nekogram"))
    assertFalse(TelegramReplyQuote.isTelegram("com.whatsapp"))
  }
}
