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

package com.google.android.accessibility.talkback.notifications

import android.app.Notification
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OwnNotificationSoundTest {
  private fun builder() =
    Notification.Builder(RuntimeEnvironment.getApplication(), "TalkBackChannel")
      .setContentTitle("Update available")

  private fun sounds(
    notification: Notification = builder().build(),
    importance: Int = IMPORTANCE_HIGH,
    channelHasSound: Boolean = false,
  ) = NotificationReader.ownNotificationSounds(notification, importance, channelHasSound)

  @Test
  fun aNotificationOnTheSilentChannelSounds() {
    assertTrue(sounds())
    assertTrue(sounds(importance = IMPORTANCE_DEFAULT))
  }

  @Test
  fun aChannelWithItsOwnSoundIsLeftToAndroid() {
    assertFalse(sounds(channelHasSound = true))
  }

  @Test
  fun aLowImportanceChannelIsSilent() {
    assertFalse(sounds(importance = IMPORTANCE_LOW))
  }

  @Test
  fun ongoingAndProgressNotificationsAreSilent() {
    assertFalse(sounds(builder().setOngoing(true).build()))
    assertFalse(sounds(builder().setProgress(100, 40, false).build()))
    assertFalse(sounds(builder().setProgress(0, 0, true).build()))
    assertFalse(sounds(builder().setCategory(Notification.CATEGORY_PROGRESS).build()))
  }
}
