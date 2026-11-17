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

package com.google.android.accessibility.talkback.focusmanagement

import android.graphics.Rect
import android.graphics.Region
import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The picture-in-picture window and the system windows over it, such as its menu, are one item. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PictureInPictureItemTest {
  private val home = window(1, AccessibilityWindowInfo.TYPE_APPLICATION, Rect(0, 0, 1080, 2400))
  private val pip =
    window(2, AccessibilityWindowInfo.TYPE_APPLICATION, Rect(600, 1600, 1040, 1850), pip = true)
  private val menu = window(3, AccessibilityWindowInfo.TYPE_SYSTEM, Rect(600, 1600, 1040, 1850))
  private val statusBar = window(4, AccessibilityWindowInfo.TYPE_SYSTEM, Rect(0, 0, 1080, 80))
  private val navBar = window(5, AccessibilityWindowInfo.TYPE_SYSTEM, Rect(0, 2280, 1080, 2400))
  private val windows = listOf(menu, pip, home, statusBar, navBar)

  @Test
  fun menuOverThePictureInPicture_belongsToIt() {
    assertTrue(PictureInPictureItem.liesOver(menu, pip))
    assertSame(pip, PictureInPictureItem.pipWindowFor(windows, menu.id))
    assertSame(pip, PictureInPictureItem.pipWindowFor(windows, pip.id))
    assertEquals(setOf(menu.id), PictureInPictureItem.overlayWindowIds(windows))
  }

  @Test
  fun otherWindows_areTheirOwn() {
    assertFalse(PictureInPictureItem.liesOver(home, pip))
    assertFalse(PictureInPictureItem.liesOver(navBar, pip))
    assertFalse(PictureInPictureItem.liesOver(statusBar, pip))
    assertNull(PictureInPictureItem.pipWindowFor(windows, home.id))
    assertNull(PictureInPictureItem.pipWindowFor(windows, navBar.id))
  }

  @Test
  fun withoutPictureInPicture_nothingBelongsToIt() {
    val noPip = listOf(menu, home)
    assertNull(PictureInPictureItem.pipWindowFor(noPip, menu.id))
    assertTrue(PictureInPictureItem.overlayWindowIds(noPip).isEmpty())
  }

  private fun window(
    id: Int,
    type: Int,
    bounds: Rect,
    pip: Boolean = false,
  ): AccessibilityWindowInfo {
    val window = AccessibilityWindowInfo.obtain()
    val c = AccessibilityWindowInfo::class.java
    c.getDeclaredMethod("setId", Int::class.javaPrimitiveType).invoke(window, id)
    c.getDeclaredMethod("setType", Int::class.javaPrimitiveType).invoke(window, type)
    c.getDeclaredMethod("setRegionInScreen", Region::class.java).invoke(window, Region(bounds))
    c.getDeclaredMethod("setPictureInPicture", Boolean::class.javaPrimitiveType)
      .invoke(window, pip)
    return window
  }
}
