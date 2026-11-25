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
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.calls.CallNotifications.Sent
import com.google.android.accessibility.talkback.permission.PermissionUtils
import java.util.function.Consumer

/**
 * The Answer call command: answers a ringing phone call through Telecom, or an app's call through
 * its call notification.
 */
object AnswerCall {
  private const val TAG = "AnswerCall"

  /** The result of the Answer call and End call commands. */
  enum class Result {
    DONE,
    /** No call rings, or for End call, no call is in progress either. */
    NO_CALL,
    /** SandFly lacks ANSWER_PHONE_CALLS, and has asked for it. */
    PERMISSION_REQUESTED,
    /** Telecom is missing or refused, or the app cancelled its call notification's intent. */
    FAILED,
  }

  /**
   * Answers the ringing call and passes the result to [done], at once or once the notification
   * listener has connected. [callState] is the TelephonyManager call state SandFly follows, which stays
   * idle without READ_PHONE_STATE. A ringing phone call is answered through Telecom; else an app's
   * ringing call through its notification; else, when the audio mode is the ringtone's, through
   * Telecom.
   */
  @JvmStatic
  @MainThread
  fun answer(context: Context, callState: Int, done: Consumer<Result>) {
    if (callState == TelephonyManager.CALL_STATE_RINGING) {
      done.accept(answerByTelecom(context))
      return
    }
    CallNotifications.answer(context) { sent ->
      done.accept(
        when (sent) {
          Sent.SENT -> Result.DONE
          Sent.FAILED -> Result.FAILED
          Sent.NONE ->
            if (isRinging(callState, audioMode(context))) answerByTelecom(context)
            else Result.NO_CALL
        }
      )
    }
  }

  // acceptRingingCall is deprecated for wearable companion apps, which have InCallService.
  @Suppress("DEPRECATION")
  private fun answerByTelecom(context: Context): Result {
    if (
      context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      PermissionUtils.requestPermissions(context, Manifest.permission.ANSWER_PHONE_CALLS)
      return Result.PERMISSION_REQUESTED
    }
    val telecom = context.getSystemService(TelecomManager::class.java) ?: return Result.FAILED
    return try {
      telecom.acceptRingingCall()
      Result.DONE
    } catch (e: SecurityException) {
      Log.w(TAG, "Telecom refused to answer the call", e)
      Result.FAILED
    }
  }

  @JvmStatic
  fun audioMode(context: Context): Int =
    context.getSystemService(AudioManager::class.java)?.mode ?: AudioManager.MODE_NORMAL

  /** Whether a call rings, from the [callState] of TelephonyManager or the [audioMode]. */
  @JvmStatic
  fun isRinging(callState: Int, audioMode: Int): Boolean =
    callState == TelephonyManager.CALL_STATE_RINGING || audioMode == AudioManager.MODE_RINGTONE
}
