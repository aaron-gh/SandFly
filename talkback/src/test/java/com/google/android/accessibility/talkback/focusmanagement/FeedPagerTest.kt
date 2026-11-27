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
import android.view.Display
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Only a feed of full-screen pages that turn up and down, such as TikTok's, turns without reading
 * the new page. Onboarding screens, galleries and other pagers read it as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedPagerTest {
  private lateinit var window: AccessibilityWindowInfo

  @Before
  fun setUp() {
    window = AccessibilityWindowInfo()
    shadowOf(window).apply {
      setId(APP_ID)
      setType(AccessibilityWindowInfo.TYPE_APPLICATION)
      setBoundsInScreen(SCREEN)
      setDisplayId(Display.DEFAULT_DISPLAY)
    }
  }

  private fun pager(bounds: Rect, vararg actions: AccessibilityAction) =
    AccessibilityNodeInfoCompat.wrap(
      AccessibilityNodeInfo().apply {
        setSource(View(RuntimeEnvironment.getApplication()))
        className = "androidx.viewpager.widget.ViewPager"
        setBoundsInScreen(bounds)
        isVisibleToUser = true
        isScrollable = true
        actions.forEach { addAction(it) }
        ReflectionHelpers.setField(this, "mWindowId", APP_ID)
        shadowOf(this).setAccessibilityWindowInfo(this@FeedPagerTest.window)
      }
    )

  private fun scrolled(dx: Int, dy: Int) =
    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      scrollDeltaX = dx
      scrollDeltaY = dy
    }

  @Test
  fun fullScreenPagerTurnedUp_isAFeed() {
    assertTrue(FocusFeedbackMapper.isFeedPager(pager(SCREEN), scrolled(0, 30)))
  }

  @Test
  fun fullScreenPagerTurnedSideways_isNoFeed() {
    // An onboarding screen or a full-screen gallery.
    assertFalse(FocusFeedbackMapper.isFeedPager(pager(SCREEN), scrolled(40, 0)))
  }

  @Test
  fun smallPagerTurnedUp_isNoFeed() {
    // A carousel or banner in a page.
    assertFalse(FocusFeedbackMapper.isFeedPager(pager(Rect(0, 400, 1080, 1000)), scrolled(0, 30)))
  }

  @Test
  fun withoutScrollDeltas_theActionsTellTheDirection() {
    val upDown =
      pager(SCREEN, AccessibilityAction.ACTION_SCROLL_UP, AccessibilityAction.ACTION_SCROLL_DOWN)
    assertTrue(FocusFeedbackMapper.isFeedPager(upDown, scrolled(-1, -1)))
    val sideways =
      pager(SCREEN, AccessibilityAction.ACTION_PAGE_LEFT, AccessibilityAction.ACTION_PAGE_RIGHT)
    assertFalse(FocusFeedbackMapper.isFeedPager(sideways, scrolled(-1, -1)))
    assertFalse(FocusFeedbackMapper.isFeedPager(pager(SCREEN), null))
  }

  private companion object {
    const val APP_ID = 3
    val SCREEN = Rect(0, 0, 1080, 2400)
  }
}
