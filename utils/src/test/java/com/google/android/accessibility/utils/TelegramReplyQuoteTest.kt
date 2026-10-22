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

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
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

  private fun message(text: CharSequence): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.obtain().apply {
      className = "android.view.ViewGroup"
      packageName = "tw.nekomimi.nekogram"
      isFocusable = true
      this.text = text
    }

  private fun named(name: String, rest: String): CharSequence =
    SpannableStringBuilder(name).apply {
      setSpan(
        object : ClickableSpan() {
          override fun onClick(widget: View) {}
        },
        0,
        length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
      )
      append('\n').append(rest)
    }

  @Test
  fun parsesTheSenderAndText() {
    val quote = TelegramReplyQuote.parse("Reply, Ronan, What other features you love? To me, honestly…")
    assertEquals("Ronan", quote?.name)
    assertEquals("What other features you love? To me, honestly…", quote?.text)
    assertNull(TelegramReplyQuote.parse("Forwarded from\nRonan"))
    assertNull(TelegramReplyQuote.parse("Reply, Ronan"))
  }

  @Test
  fun theQuoteFollowsTheSendersName() {
    val text = named("G Melconian", "that has been on the pixel dialer\nReceived at 00:53")
    val quote = TelegramReplyQuote.Quote("Ronan", "What other features you love?")
    val said = TelegramReplyQuote.withQuote(message(text), text.toString(), quote, format, ", ")
    assertEquals(
      "G Melconian, replying to Ronan: What other features you love?, that has been on the pixel dialer\nReceived at 00:53",
      said.toString(),
    )
  }

  @Test
  fun withoutANameTheQuoteComesFirst() {
    val text = "sure, tasker is more powerful\nReceived at 21:30"
    val quote = TelegramReplyQuote.Quote("Ann", "Which app?")
    val said = TelegramReplyQuote.withQuote(message(text), text, quote, format, ", ")
    assertEquals("replying to Ann: Which app?, sure, tasker is more powerful\nReceived at 21:30", said.toString())
  }

  @Test
  fun onlyTelegramApps() {
    assertTrue(TelegramReplyQuote.isTelegram("org.telegram.messenger"))
    assertTrue(TelegramReplyQuote.isTelegram("tw.nekomimi.nekogram"))
    assertFalse(TelegramReplyQuote.isTelegram("com.whatsapp"))
  }
}
