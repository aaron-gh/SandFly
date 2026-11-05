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

package com.google.android.accessibility.talkback.imagecaption

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Which web nodes automatic captions are for. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageCaptionUtilsWebContainerTest {

  private val context: Context
    get() = RuntimeEnvironment.getApplication()

  /** A web node without a name that reports an image, as Chrome and WebView do. */
  private fun webNode(
    className: String,
    bounds: Rect,
    children: Int = 0,
  ): AccessibilityNodeInfoCompat {
    val info = AccessibilityNodeInfo.obtain()
    info.className = className
    info.setBoundsInScreen(bounds)
    info.isVisibleToUser = true
    info.extras.putString("AccessibilityNodeInfo.hasImage", "true")
    repeat(children) {
      val child = AccessibilityNodeInfo.obtain()
      child.setBoundsInScreen(bounds)
      shadowOf(info).addChild(child)
    }
    return AccessibilityNodeInfoCompat.wrap(info)
  }

  @Test
  fun aWholeWebView_isNotCaptioned() {
    val webView = webNode("android.webkit.WebView", Rect(0, 200, 1080, 2400), children = 3)

    assertFalse(ImageCaptionUtils.isCaptionable(context, webView))
    assertFalse(ImageCaptionUtils.needImageCaptionForUnlabelledView(context, webView))
    assertFalse(
      ImageCaptionUtils.needImageCaption(
        context,
        webView,
        /* enableAutomaticImageCaptionForAllImages= */ true,
        /* needSizeCheck= */ false,
      )
    )
  }

  @Test
  fun aWebViewWithoutChildren_isNotCaptioned() {
    val webView = webNode("android.webkit.WebView", Rect(0, 0, 100, 100))

    assertFalse(ImageCaptionUtils.needImageCaptionForUnlabelledView(context, webView))
  }

  @Test
  fun aScrollingWebContainerWithAnImage_isNotCaptioned() {
    val list = webNode("android.view.View", Rect(0, 200, 1080, 1200), children = 5)
    list.isScrollable = true

    assertFalse(ImageCaptionUtils.needImageCaptionForUnlabelledView(context, list))
  }

  @Test
  fun anUnlabelledWebImage_isCaptioned() {
    val image = webNode("android.widget.Image", Rect(0, 0, 200, 200))

    assertTrue(ImageCaptionUtils.needImageCaptionForUnlabelledView(context, image))
  }

  @Test
  fun anUnlabelledWebLinkThatContainsAnImage_isCaptioned() {
    val link = webNode("android.view.View", Rect(0, 0, 400, 300), children = 1)
    link.isClickable = true

    assertTrue(ImageCaptionUtils.needImageCaptionForUnlabelledView(context, link))
  }
}
