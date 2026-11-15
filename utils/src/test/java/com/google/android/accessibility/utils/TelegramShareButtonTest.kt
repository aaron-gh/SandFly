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

package com.google.android.accessibility.utils

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TelegramShareButtonTest {
  private val telegram = "org.telegram.messenger"
  private val imageButton = "android.widget.ImageButton"

  @Test
  fun aClickableImageButtonWithALabelIsTheShareButton() {
    assertTrue(TelegramShareButton.looksLikeShareButton(telegram, imageButton, "Share", true))
    assertTrue(
      TelegramShareButton.looksLikeShareButton("tw.nekomimi.nekogram", imageButton, "Teilen", true)
    )
  }

  @Test
  fun otherPartsOfAMessageAreNot() {
    // The reply quote has no class name.
    assertFalse(TelegramShareButton.looksLikeShareButton(telegram, null, "Reply, Ronan, Hi", true))
    assertFalse(
      TelegramShareButton.looksLikeShareButton(telegram, "android.widget.Button", "Comments", true)
    )
    assertFalse(TelegramShareButton.looksLikeShareButton(telegram, imageButton, "Share", false))
    assertFalse(TelegramShareButton.looksLikeShareButton(telegram, imageButton, " ", true))
    assertFalse(TelegramShareButton.looksLikeShareButton(telegram, imageButton, null, true))
  }

  @Test
  fun onlyTelegramApps() {
    assertFalse(TelegramShareButton.looksLikeShareButton("com.whatsapp", imageButton, "Share", true))
    assertFalse(TelegramShareButton.looksLikeShareButton(null, imageButton, "Share", true))
  }

  private fun child(className: String?, description: String?, clickable: Boolean = true) =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo()).apply {
      packageName = telegram
      this.className = className
      contentDescription = description
      isClickable = clickable
    }

  @Test
  fun everyShareButtonOfAMessageIsOffered() {
    val children =
      listOf(
        child(null, "Reply, Ronan, Hi"),
        child(imageButton, "Share"),
        null,
        child("android.widget.Button", "Comments"),
        child(imageButton, "Open chat"),
      )
    assertEquals(
      listOf("Share", "Open chat"),
      TelegramShareButton.shareButtonsAmong(children).map { it.contentDescription.toString() },
    )
  }

  @Test
  fun aMessageWithoutOneOffersNone() {
    assertTrue(TelegramShareButton.shareButtonsAmong(listOf(child(null, "Reply, Ronan, Hi"))).isEmpty())
  }
}
