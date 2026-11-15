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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.os.SystemClock
import com.google.android.accessibility.talkback.feedbackpolicy.ScreenFeedbackManager.NOTIFICATION_PANE_MS
import com.google.android.accessibility.talkback.feedbackpolicy.ScreenFeedbackManager.followsNotification
import com.google.android.accessibility.utils.monitor.RecentUserInput
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationPaneTest {
  @Test
  fun aPaneSoonAfterANotification_followsIt() {
    assertTrue(followsNotification(10_000, -1, 10_400))
    assertTrue(followsNotification(10_000, 9_000, 10_000 + NOTIFICATION_PANE_MS))
  }

  @Test
  fun aPaneAfterATouchOrKey_doesNotFollowTheNotification() {
    assertFalse(followsNotification(10_000, 10_200, 10_400))
    assertFalse(followsNotification(10_000, 10_000, 10_400))
  }

  @Test
  fun inputRecordedAfterTheNotification_isUserInput() {
    SystemClock.sleep(10)
    val notificationTime = SystemClock.uptimeMillis()
    SystemClock.sleep(10)
    RecentUserInput.mark()
    assertFalse(
      followsNotification(notificationTime, RecentUserInput.lastInputUptimeMs(), notificationTime + 100)
    )
  }

  @Test
  fun aPaneLongAfterANotification_doesNotFollowIt() {
    assertFalse(followsNotification(10_000, -1, 10_001 + NOTIFICATION_PANE_MS))
  }

  @Test
  fun withoutANotification_noPaneFollowsOne() {
    assertFalse(followsNotification(-1, -1, 10_000))
  }
}
