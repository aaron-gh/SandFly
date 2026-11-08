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

package com.google.android.accessibility.talkback.notificationlist

import android.app.Notification
import android.os.Process
import android.service.notification.StatusBarNotification
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationListTest {
  private val context = RuntimeEnvironment.getApplication()

  private fun posted(packageName: String, id: Int, notification: Notification) =
    @Suppress("DEPRECATION")
    StatusBarNotification(packageName, packageName, id, null, 0, 0, 0, notification, Process.myUserHandle(), 0L)

  private fun mediaActivity(): Notification =
    Notification.Builder(context, "MediaOngoingActivity").setContentTitle("MediaOngoingActivity").build().also {
      it.extras.putInt("android.ongoingActivityNoti.style", 1)
    }

  @After
  fun disconnect() {
    NotificationList.onDisconnected()
  }

  @Test
  fun oneUiMediaActivityIsLeftOut() {
    val player = posted("com.example.player", 2, Notification.Builder(context, "playing").setContentTitle("Episode").build())
    NotificationList.onConnected(arrayOf(posted("com.android.systemui", 1, mediaActivity()), player), null)

    val keys = NotificationList.items(context).flatMap { it.entries }.map { it.key }

    assertEquals(listOf(player.key), keys)
  }
}
