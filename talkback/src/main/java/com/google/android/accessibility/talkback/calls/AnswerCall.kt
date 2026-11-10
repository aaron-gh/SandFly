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
import com.google.android.accessibility.talkback.permission.PermissionUtils

/** The Answer call command: answers the ringing call through Telecom. */
object AnswerCall {
  private const val TAG = "AnswerCall"

  enum class Result {
    ANSWERED,
    /** No call is ringing. */
    NO_CALL,
    /** SandFly lacks ANSWER_PHONE_CALLS, and has asked for it. */
    PERMISSION_REQUESTED,
    /** Telecom is missing or refused. */
    FAILED,
  }

  /**
   * Answers the ringing call. [callState] is the TelephonyManager call state SandFly follows, which
   * stays idle without READ_PHONE_STATE; the audio mode covers that case and app calls.
   */
  // acceptRingingCall is deprecated for wearable companion apps, which have InCallService.
  @Suppress("DEPRECATION")
  @JvmStatic
  fun answer(context: Context, callState: Int): Result {
    if (
      context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      PermissionUtils.requestPermissions(context, Manifest.permission.ANSWER_PHONE_CALLS)
      return Result.PERMISSION_REQUESTED
    }
    val telecom = context.getSystemService(TelecomManager::class.java) ?: return Result.FAILED
    val audioMode =
      context.getSystemService(AudioManager::class.java)?.mode ?: AudioManager.MODE_NORMAL
    if (!isRinging(callState, audioMode)) {
      return Result.NO_CALL
    }
    return try {
      telecom.acceptRingingCall()
      Result.ANSWERED
    } catch (e: SecurityException) {
      Log.w(TAG, "Telecom refused to answer the call", e)
      Result.FAILED
    }
  }

  /** Whether a call rings, from the [callState] of TelephonyManager or the [audioMode]. */
  @JvmStatic
  fun isRinging(callState: Int, audioMode: Int): Boolean =
    callState == TelephonyManager.CALL_STATE_RINGING || audioMode == AudioManager.MODE_RINGTONE
}
