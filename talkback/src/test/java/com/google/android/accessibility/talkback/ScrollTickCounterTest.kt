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

package com.google.android.accessibility.talkback

import com.google.android.accessibility.talkback.ScrollTickCounter.Companion.MAX_TICKS
import com.google.android.accessibility.talkback.ScrollTickCounter.Companion.UNKNOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollTickCounterTest {
  private val counter = ScrollTickCounter(itemPx = 100f)

  private fun scrollBy(
    time: Long,
    deltaY: Int,
    window: Int = 1,
    className: String = "List",
    source: Any? = "list",
  ) = counter.onScroll(time, window, className, source, UNKNOWN, 0, UNKNOWN, 0, deltaY)

  private fun scrollTo(time: Long, fromIndex: Int, source: Any? = "list") =
    counter.onScroll(time, 1, "List", source, fromIndex, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN)

  @Test
  fun theFirstEventOfAScrollTicksOnce() {
    assertEquals(1, scrollBy(time = 0, deltaY = 500))
  }

  @Test
  fun ticksOncePerItemOfDistance_carryingTheRest() {
    scrollBy(time = 0, deltaY = 10)
    assertEquals(1, scrollBy(time = 100, deltaY = 150))
    assertEquals(0, scrollBy(time = 200, deltaY = 40))
    // 50 left over plus 60.
    assertEquals(1, scrollBy(time = 300, deltaY = 60))
  }

  @Test
  fun scrollingBackTicksToo() {
    scrollBy(time = 0, deltaY = 10)
    assertEquals(2, scrollBy(time = 100, deltaY = -250))
  }

  @Test
  fun aFlingTicksAtMostMaxTicks() {
    scrollBy(time = 0, deltaY = 10)
    assertEquals(MAX_TICKS, scrollBy(time = 100, deltaY = 2000))
  }

  @Test
  fun aPauseStartsANewScroll() {
    scrollBy(time = 0, deltaY = 10)
    assertEquals(1, scrollBy(time = 400, deltaY = 300))
    assertEquals(3, scrollBy(time = 500, deltaY = 300))
  }

  @Test
  fun anotherListStartsANewScroll() {
    scrollBy(time = 0, deltaY = 10)
    assertEquals(1, scrollBy(time = 100, deltaY = 300, className = "Other"))
    assertEquals(1, scrollBy(time = 200, deltaY = 300, className = "Other", window = 2))
  }

  @Test
  fun listsWithIndexesTickPerItem() {
    scrollTo(time = 0, fromIndex = 5)
    assertEquals(0, scrollTo(time = 100, fromIndex = 5))
    assertEquals(2, scrollTo(time = 200, fromIndex = 7))
    assertEquals(1, scrollTo(time = 300, fromIndex = 6))
  }

  @Test
  fun withoutDeltasTheScrollPositionIsUsed() {
    counter.onScroll(0, 1, "List", null, UNKNOWN, 0, 0, UNKNOWN, UNKNOWN)
    assertEquals(2, counter.onScroll(100, 1, "List", null, UNKNOWN, 0, 200, UNKNOWN, UNKNOWN))
  }

  @Test
  fun anotherListOfTheSameClassInTheSameWindowStartsANewScroll() {
    scrollTo(time = 0, fromIndex = 0, source = "left pane")
    scrollTo(time = 100, fromIndex = 40, source = "left pane")
    // Without telling the lists apart, this would count 38 items between them.
    assertEquals(1, scrollTo(time = 200, fromIndex = 2, source = "right pane"))
    assertEquals(1, scrollTo(time = 300, fromIndex = 3, source = "right pane"))
  }

  @Test
  fun aRateLimiterLetsFeedbackThroughAtMostOncePerInterval() {
    val limiter = FeedbackRateLimiter(minIntervalMs = 250)
    assertTrue(limiter.tryAcquire(1000))
    assertFalse(limiter.tryAcquire(1100))
    assertFalse(limiter.tryAcquire(1249))
    assertTrue(limiter.tryAcquire(1250))
    assertFalse(limiter.tryAcquire(1400))
  }
}
