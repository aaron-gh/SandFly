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

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.accessibility.talkback.R

/**
 * Asks for the text of a reply to a notification and sends it. It runs in a process of its own: a
 * keyboard asks the text field about its text as it types, and a field in Backtalk's main process
 * answers on the main thread, which also asks the keyboard about its keys. Each waited for the other
 * until both timed out, so typing stalled.
 */
class NotificationReplyActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    @Suppress("DEPRECATION") // getParcelableExtra(String, Class) needs Android 13.
    val action: Notification.Action? = intent.getParcelableExtra(EXTRA_ACTION)
    val input = action?.remoteInputs?.firstOrNull { it.allowFreeFormInput }
    if (action == null || input == null) {
      finish()
      return
    }
    val field =
      EditText(this).apply {
        hint = input.label ?: action.title
        inputType =
          InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
      }
    val dialog =
      AlertDialog.Builder(this)
        .setTitle(action.title)
        .setView(field)
        .setPositiveButton(R.string.notification_list_send) { _, _ ->
          if (field.text.isNotBlank()) {
            reply(this, action, input, field.text.toString())
          }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .setOnDismissListener { finish() }
        .show()
    dialog.keepButtonCase()
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    field.requestFocus()
  }

  companion object {
    private const val TAG = "NotificationReply"
    private const val EXTRA_ACTION = "action"

    /** Returns the intent that asks for the text of a reply to [action] and sends it. */
    @JvmStatic
    fun intent(context: Context, action: Notification.Action): Intent =
      Intent(context, NotificationReplyActivity::class.java).putExtra(EXTRA_ACTION, action)

    /** Sends [text] as the answer to [input] of [action], as the system's shade does. */
    @JvmStatic
    fun reply(context: Context, action: Notification.Action, input: RemoteInput, text: CharSequence) {
      val results = Bundle().apply { putCharSequence(input.resultKey, text) }
      val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
      RemoteInput.addResultsToIntent(action.remoteInputs, intent, results)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        RemoteInput.setResultsSource(
          intent,
          if (input.allowFreeFormInput) RemoteInput.SOURCE_FREE_FORM_INPUT
          else RemoteInput.SOURCE_CHOICE,
        )
      }
      send(context, action.actionIntent, intent)
    }

    /** Sends [pendingIntent], allowed to open an activity as the system's shade is. */
    @JvmStatic
    fun send(context: Context, pendingIntent: PendingIntent?, fillIn: Intent?) {
      try {
        pendingIntent?.send(context, 0, fillIn, null, null, null, startOptions())
      } catch (e: PendingIntent.CanceledException) {
        Log.w(TAG, "Notification action can't be performed", e)
      }
    }

    /** Lets a notification's action open an activity from the background on Android 14 and up. */
    @JvmStatic
    fun startOptions(): Bundle? {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return null
      }
      return ActivityOptions.makeBasic()
        .setPendingIntentBackgroundActivityStartMode(
          ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        )
        .toBundle()
    }

    /** Keeps the dialog's button text as written. */
    @JvmStatic
    fun AlertDialog.keepButtonCase() {
      listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE).forEach {
        getButton(it)?.isAllCaps = false
      }
    }
  }
}
