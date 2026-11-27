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

package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.dialog.BaseDialog
import com.google.android.accessibility.talkback.shizuku.ShellAccess
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions

/**
 * Restart phone and Power off phone: after a dialog that confirms them, they restart or turn off
 * the device through Shizuku or root, as `svc power` does, the same on every phone, with no power
 * menu to use. The system power menu opens in their place on the lock screen, where the phone may
 * ask to unlock before it turns off; without Shizuku or root, saying why; and when the command
 * fails.
 */
object PowerCommands {
  private enum class Power(
    @StringRes val value: Int,
    @StringRes val title: Int,
    @StringRes val button: Int,
    val command: ShizukuCommand,
  ) {
    RESTART(
      R.string.shortcut_value_restart_phone,
      R.string.restart_phone_confirm_title,
      R.string.restart_phone_confirm_button,
      ShizukuCommand.REBOOT,
    ),
    POWER_OFF(
      R.string.shortcut_value_power_off_phone,
      R.string.power_off_phone_confirm_title,
      R.string.power_off_phone_confirm_button,
      ShizukuCommand.POWER_OFF,
    ),
  }

  /**
   * How long a failed command waits for Android's shutdown broadcast before it is reported: the
   * device going down stops the shell, and its result may come back before the broadcast.
   */
  private const val SHUTDOWN_WAIT_MS = 1_500L

  /** Set by Android's shutdown broadcast, which comes before the device restarts or turns off. */
  @Volatile private var shuttingDown = false

  private val handler = Handler(Looper.getMainLooper())

  /** Notes that the device is restarting or turning off. */
  @JvmStatic
  fun onShutDown() {
    shuttingDown = true
  }

  /** Whether [action] is Restart phone or Power off phone. */
  @JvmStatic
  fun isAction(context: Context, action: String): Boolean = powerOf(context, action) != null

  /** Runs [action], Restart phone or Power off phone. */
  @JvmStatic
  fun run(service: AccessibilityService, pipeline: FeedbackReturner?, action: String) {
    val power = powerOf(service, action) ?: return
    if (service.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != false) {
      openPowerMenu(service)
      return
    }
    if (!ShizukuShell.isAvailable(service)) {
      openPowerMenu(service)
      return
    }
    unavailable()?.let {
      fallBack(service, pipeline, it)
      return
    }
    ConfirmDialog(service, pipeline, power) {
      // Shizuku may have stopped while the dialog was shown.
      unavailable()?.let {
        fallBack(service, pipeline, it)
        return@ConfirmDialog
      }
      ShizukuShell.run(service, power.command, argument = "") { worked ->
        if (!worked && !shuttingDown) {
          handler.postDelayed(
            { if (!shuttingDown) fallBack(service, pipeline, failure()) },
            SHUTDOWN_WAIT_MS,
          )
        }
      }
    }
      .showDialog()
  }

  private fun powerOf(context: Context, action: String): Power? =
    Power.entries.firstOrNull { context.getString(it.value) == action }

  /**
   * Why the command can't run, or null when it may: Shizuku isn't running, or the user chose "Deny
   * and don't ask again" for SandFly in Shizuku. Root is asked for when the command runs.
   */
  @StringRes
  private fun unavailable(): Int? {
    if (!ShizukuGrants.isReady()) return R.string.overview_shizuku_not_running
    val denied =
      try {
        !ShizukuShell.isAllowed() && ShellAccess.isPermissionDenied()
      } catch (e: RuntimeException) {
        false
      }
    return if (denied) R.string.overview_shizuku_not_allowed else null
  }

  /** Why the command didn't run: Shizuku stopped or doesn't allow SandFly, no root, or it failed. */
  @StringRes
  private fun failure(): Int =
    when {
      !ShizukuGrants.isReady() -> R.string.overview_shizuku_not_running
      !ShizukuShell.isAllowed() ->
        if (ShizukuShell.usesRoot()) R.string.no_root_access else R.string.overview_shizuku_not_allowed
      else -> R.string.power_command_failed
    }

  /** Says [why], and opens the system power menu, which is spoken after it, not over it. */
  private fun fallBack(
    service: AccessibilityService,
    pipeline: FeedbackReturner?,
    @StringRes why: Int,
  ) {
    pipeline?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        service.getString(why),
        SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH)
          .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
    openPowerMenu(service)
  }

  private fun openPowerMenu(service: AccessibilityService) {
    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
  }

  /** Asks whether to restart or turn off the phone, which closes every app. */
  private class ConfirmDialog(
    context: Context,
    pipeline: FeedbackReturner?,
    power: Power,
    private val onConfirm: () -> Unit,
  ) : BaseDialog(context, power.title, pipeline) {

    init {
      setPositiveButtonStringRes(power.button)
    }

    override fun handleDialogClick(buttonClicked: Int) {
      if (buttonClicked == DialogInterface.BUTTON_POSITIVE) onConfirm()
    }

    override fun handleDialogDismiss() {}

    override fun getMessageString(): String? = null

    override fun getCustomizedView(inflater: LayoutInflater): View? = null
  }
}
