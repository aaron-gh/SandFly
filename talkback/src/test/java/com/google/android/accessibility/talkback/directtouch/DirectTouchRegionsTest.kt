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

package com.google.android.accessibility.talkback.directtouch

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectTouchRegionsTest {
  @Test
  fun windowEventsReassertTheRegion() {
    // Other services clear the shared region on these, like Narwhal does when a volume slider opens.
    assertTrue(DirectTouchRegions.reassertsRegion(AccessibilityEvent.TYPE_WINDOWS_CHANGED))
    assertTrue(DirectTouchRegions.reassertsRegion(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
  }

  @Test
  fun otherEventsDoNot() {
    assertFalse(DirectTouchRegions.reassertsRegion(AccessibilityEvent.TYPE_VIEW_FOCUSED))
    assertFalse(DirectTouchRegions.reassertsRegion(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
    assertFalse(DirectTouchRegions.reassertsRegion(AccessibilityEvent.TYPE_TOUCH_INTERACTION_START))
  }

  @Test
  fun aThinStripAlongTheBottomIsANavigationBar() {
    assertTrue(DirectTouchRegions.isNavigationBar(0, 2300, 1080, 2400, 1080, 2400))
  }

  @Test
  fun aNavigationBarThatIsOnlyTheButtonClusterIsStillOne() {
    // The window Android reports on a real phone with three-button navigation.
    assertTrue(DirectTouchRegions.isNavigationBar(165, 2245, 895, 2377, 1080, 2377))
  }

  @Test
  fun aThinStripAlongASideInLandscapeIsANavigationBar() {
    assertTrue(DirectTouchRegions.isNavigationBar(2250, 0, 2400, 1080, 2400, 1080))
    assertTrue(DirectTouchRegions.isNavigationBar(0, 0, 150, 1080, 2400, 1080))
  }

  @Test
  fun theStatusBarIsNotANavigationBar() {
    assertFalse(DirectTouchRegions.isNavigationBar(0, 0, 1080, 100, 1080, 2400))
  }

  @Test
  fun theNotificationShadeIsNotANavigationBar() {
    assertFalse(DirectTouchRegions.isNavigationBar(0, 0, 1080, 2400, 1080, 2400))
    assertFalse(DirectTouchRegions.isNavigationBar(0, 1200, 1080, 2400, 1080, 2400))
  }

  @Test
  fun aSmallPopupAtTheBottomIsNotANavigationBar() {
    assertFalse(DirectTouchRegions.isNavigationBar(300, 2300, 700, 2400, 1080, 2400))
  }

  @Test
  fun anEmptyWindowIsNotANavigationBar() {
    assertFalse(DirectTouchRegions.isNavigationBar(0, 2400, 1080, 2400, 1080, 2400))
  }
}
