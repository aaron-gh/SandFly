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

package com.google.android.accessibility.talkback.interpreters

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tells the user's scroll of web content from the scrolls that the page makes in response. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebScrollSourceTest {
  private class Node(sideways: Boolean) :
    AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()) {
    init {
      addAction(ACTION_NEXT_HTML_ELEMENT)
      addAction(ACTION_PREVIOUS_HTML_ELEMENT)
      isScrollable = true
      if (sideways) {
        addAction(AccessibilityActionCompat.ACTION_SCROLL_LEFT)
        addAction(AccessibilityActionCompat.ACTION_SCROLL_RIGHT)
      } else {
        addAction(AccessibilityActionCompat.ACTION_SCROLL_DOWN)
        addAction(AccessibilityActionCompat.ACTION_SCROLL_UP)
      }
    }

    override fun equals(other: Any?) = this === other

    override fun hashCode() = System.identityHashCode(this)
  }

  private val menu = Node(sideways = false)
  private val strip = Node(sideways = true)

  @Test
  fun aStripThatThePageScrollsWhileTheUserScrollsTheMenuIsThePages() {
    val source = WebScrollSource()

    assertEquals(WebScrollSource.USER_START, source.onScroll(menu, 1000))
    assertEquals(WebScrollSource.PAGE_SIDEWAYS, source.onScroll(strip, 1000))
    assertEquals(WebScrollSource.USER, source.onScroll(menu, 1000))
    assertFalse(source.userScrollIsSideways())
  }

  @Test
  fun theMenuReplacesAStripStillMovingWhenTheTouchStarted() {
    val source = WebScrollSource()

    assertEquals(WebScrollSource.USER_START, source.onScroll(strip, 1000))
    assertTrue(source.userScrollIsSideways())
    assertEquals(WebScrollSource.USER_START, source.onScroll(menu, 1000))
    assertEquals(WebScrollSource.PAGE_SIDEWAYS, source.onScroll(strip, 1000))
  }

  @Test
  fun aNewTouchStartsANewUserScroll() {
    val source = WebScrollSource()
    source.onScroll(menu, 1000)

    assertEquals(WebScrollSource.USER_START, source.onScroll(strip, 2000))
    assertTrue(source.userScrollIsSideways())
  }

  @Test
  fun anotherListThatThePageScrollsIsNotSideways() {
    val source = WebScrollSource()
    source.onScroll(menu, 1000)

    assertEquals(WebScrollSource.PAGE_OTHER, source.onScroll(Node(sideways = false), 1000))
  }
}
