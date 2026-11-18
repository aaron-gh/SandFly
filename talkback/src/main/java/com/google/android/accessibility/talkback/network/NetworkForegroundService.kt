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

package com.google.android.accessibility.talkback.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.accessibility.talkback.R
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Keeps SandFly in the foreground while [NetworkForeground] runs network requests that Android
 * would block otherwise. Started with startService, not startForegroundService, so that stopping
 * it before it is in the foreground, or Android refusing the foreground, doesn't crash SandFly.
 *
 * Its notification is on a channel of minimal importance: no sound, no status bar icon and no
 * accessibility event, so SandFly doesn't read it.
 */
class NetworkForegroundService : Service() {
  private var lastStartId = 0

  override fun onCreate() {
    super.onCreate()
    instance = this
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    lastStartId = startId
    val leases = NetworkForeground.leases
    if (!leases.onServiceStarted()) {
      stopSelfResult(startId)
      return START_NOT_STICKY
    }
    val foreground =
      try {
        createChannel()
        ServiceCompat.startForeground(
          this,
          NOTIFICATION_ID,
          notification(),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        true
      } catch (e: Exception) {
        // ForegroundServiceStartNotAllowedException when no exemption from the background start
        // limits applies, or when Android 15's daily time for data sync services is used up.
        LogUtils.w(TAG, "Not in the foreground: %s", e)
        false
      }
    if (leases.onForeground(foreground)) {
      leave()
    } else if (!foreground) {
      stopSelfResult(startId)
    }
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  /**
   * Android 15 and later give data sync services 6 hours a day, then call this. The service must
   * leave the foreground within seconds or Android stops SandFly. Requests still running go on as
   * they are.
   */
  override fun onTimeout(startId: Int, fgsType: Int) {
    LogUtils.w(TAG, "Foreground time used up")
    NetworkForeground.leases.onServiceGone()
    leave()
  }

  override fun onDestroy() {
    NetworkForeground.leases.onServiceGone()
    if (instance === this) instance = null
    super.onDestroy()
  }

  private fun leave() {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    // Only when no start came in since, which then runs its own onStartCommand.
    stopSelfResult(lastStartId)
  }

  private fun createChannel() {
    getSystemService(NotificationManager::class.java)
      ?.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.network_notification_channel),
          NotificationManager.IMPORTANCE_MIN,
        )
      )
  }

  private fun notification(): Notification =
    NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.icon)
      .setContentTitle(getString(R.string.network_notification_title))
      .setPriority(NotificationCompat.PRIORITY_MIN)
      .setSilent(true)
      .setOngoing(true)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
      .build()

  companion object {
    private const val TAG = "NetworkForegroundService"
    private const val CHANNEL_ID = "network_requests"
    private const val NOTIFICATION_ID = 0x0E71

    private var instance: NetworkForegroundService? = null

    /** Stops the service when the last request has ended. */
    @MainThread
    fun stopIfIdle() {
      val service = instance ?: return
      if (NetworkForeground.leases.stopIfIdle()) service.leave()
    }
  }
}
