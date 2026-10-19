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

package com.google.android.accessibility.talkback.actor.gemini.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager.State
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.preference.BasePreferencesActivity

/**
 * Keeps the process in the foreground while a model downloads, and shows the progress in a
 * notification with a button to stop. It follows [LocalModelManager], which does the download.
 */
class ModelDownloadService : Service() {
  private lateinit var manager: LocalModelManager
  private var model: LocalModel? = null
  // Set when Android ended the time in the foreground. The service then waits for the download to
  // stop without a progress notification, and says how it ended.
  private var timedOut = false
  private val listener: () -> Unit = { onStateChanged() }

  override fun onCreate() {
    super.onCreate()
    manager = LocalModelManager.get(this)
    createChannel()
    val state = manager.state as? State.Downloading
    model = state?.model
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      progressNotification(state ?: State.Downloading(LocalModel.E2B, 0)),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
    manager.addListener(listener)
    onStateChanged()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_CANCEL) manager.cancelDownload()
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    manager.removeListener(listener)
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  /**
   * Android 15 and later give a dataSync service 6 hours from when the user last had Backtalk open,
   * then call this. The service must leave the foreground within seconds, or Android crashes the
   * whole process, screen reader included. The download can take longer to stop, at its next read
   * or after checking the file, so the service does not wait for it.
   */
  override fun onTimeout(startId: Int, fgsType: Int) {
    timedOut = true
    manager.pauseDownload()
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
  }

  private fun onStateChanged() {
    val state = manager.state
    if (state is State.Downloading) {
      model = state.model
      if (!timedOut) notificationManager().notify(NOTIFICATION_ID, progressNotification(state))
      return
    }
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    finalNotification(state)?.let { notificationManager().notify(RESULT_NOTIFICATION_ID, it) }
    stopSelf()
  }

  private fun progressNotification(state: State.Downloading): Notification {
    val total = state.model.sizeBytes
    val percent = DownloadProgress.percent(state.bytes, total)
    val sizes =
      getString(
        R.string.on_device_ai_progress_sizes,
        Formatter.formatShortFileSize(this, state.bytes),
        Formatter.formatShortFileSize(this, total),
      )
    val seconds = DownloadProgress.secondsLeft(state.bytes, total, state.bytesPerSecond)
    val text =
      if (seconds == null) sizes
      else
        getString(R.string.on_device_ai_progress_with_time, sizes, DateUtils.formatElapsedTime(seconds))
    val cancel =
      PendingIntent.getService(
        this,
        0,
        Intent(this, ModelDownloadService::class.java).setAction(ACTION_CANCEL),
        PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.icon)
      .setContentTitle(getString(R.string.on_device_ai_notification_downloading, percent))
      .setContentText(text)
      .setProgress(100, percent, false)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .addAction(0, getString(R.string.on_device_ai_cancel), cancel)
      .build()
  }

  /**
   * Says whether the download worked, failed or was paused by Android, and opens the on-device AI
   * settings when tapped. Nothing is shown when the user stopped it.
   */
  private fun finalNotification(state: State): Notification? {
    val done = model?.let { manager.store.isInstalled(it) } == true
    val text =
      when {
        state == State.Failed(ModelFiles.Failure.TIME_LIMIT) ->
          getString(R.string.on_device_ai_notification_paused)
        state is State.Failed -> getString(R.string.on_device_ai_notification_failed)
        done -> getString(R.string.on_device_ai_notification_done)
        else -> return null
      }
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.icon)
      .setContentTitle(getString(R.string.title_pref_on_device_ai))
      .setContentText(text)
      .setContentIntent(settingsIntent())
      .setAutoCancel(true)
      .build()
  }

  // Opening the settings also gives a paused download a new 6 hours.
  private fun settingsIntent(): PendingIntent =
    PendingIntent.getActivity(
      this,
      0,
      Intent(this, TalkBackPreferencesActivity::class.java)
        .putExtra(BasePreferencesActivity.FRAGMENT_NAME, OnDeviceAiFragment::class.java.name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  private fun createChannel() {
    notificationManager()
      .createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.on_device_ai_notification_channel),
          NotificationManager.IMPORTANCE_LOW,
        )
      )
  }

  private fun notificationManager() = getSystemService(NotificationManager::class.java)

  companion object {
    private const val CHANNEL_ID = "on_device_ai_download"
    private const val NOTIFICATION_ID = 0x0DA1
    private const val RESULT_NOTIFICATION_ID = 0x0DA2
    private const val ACTION_CANCEL = "com.google.android.accessibility.talkback.CANCEL_MODEL_DOWNLOAD"

    // Call this only from Backtalk's own screens. Once Android has ended the download's time, it
    // lets the service into the foreground again only after the user has had Backtalk open. Started
    // from anywhere else, like a button in a notification, startForeground crashes the process.
    fun start(context: Context) {
      ContextCompat.startForegroundService(context, Intent(context, ModelDownloadService::class.java))
    }
  }
}
