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

package com.google.android.accessibility.talkback.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.calls.AnswerCall.Result
import com.google.android.accessibility.talkback.calls.CallNotifications.Sent
import com.google.android.accessibility.talkback.permission.PermissionUtils
import java.util.function.Consumer

/**
 * The End call command: ends a phone call through Telecom, or declines or hangs up an app's call
 * through its call notification.
 */
object EndCall {
  private const val TAG = "EndCall"

  /**
   * Ends the ringing call or the call in progress and passes the result to [done], at once or once
   * the notification listener has connected. [callState] is the TelephonyManager call state SandFly
   * follows, which stays idle without READ_PHONE_STATE. A phone call is ended through Telecom;
   * else an app's call through its notification; else, when the audio mode is the ringtone's or a
   * phone call's, through Telecom.
   */
  @JvmStatic
  @MainThread
  fun end(context: Context, callState: Int, done: Consumer<Result>) {
    if (
      callState == TelephonyManager.CALL_STATE_RINGING ||
        callState == TelephonyManager.CALL_STATE_OFFHOOK
    ) {
      done.accept(endByTelecom(context, ifNone = Result.FAILED))
      return
    }
    CallNotifications.end(context) { sent ->
      done.accept(
        when (sent) {
          Sent.SENT -> Result.DONE
          Sent.FAILED -> Result.FAILED
          Sent.NONE ->
            if (isPhoneCallAudio(AnswerCall.audioMode(context))) {
              endByTelecom(context, ifNone = Result.NO_CALL)
            } else Result.NO_CALL
        }
      )
    }
  }

  /** Whether the [audioMode] is that of a ringing phone call or one in progress. */
  @JvmStatic
  fun isPhoneCallAudio(audioMode: Int): Boolean =
    audioMode == AudioManager.MODE_RINGTONE || audioMode == AudioManager.MODE_IN_CALL

  /** Ends the call through Telecom, with [ifNone] when Telecom ends none. */
  // endCall is deprecated for wearable companion apps, which have InCallService.
  @Suppress("DEPRECATION")
  private fun endByTelecom(context: Context, ifNone: Result): Result {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return Result.FAILED
    if (
      context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      PermissionUtils.requestPermissions(context, Manifest.permission.ANSWER_PHONE_CALLS)
      return Result.PERMISSION_REQUESTED
    }
    val telecom = context.getSystemService(TelecomManager::class.java) ?: return Result.FAILED
    return try {
      if (telecom.endCall()) Result.DONE else ifNone
    } catch (e: SecurityException) {
      Log.w(TAG, "Telecom refused to end the call", e)
      Result.FAILED
    }
  }
}
