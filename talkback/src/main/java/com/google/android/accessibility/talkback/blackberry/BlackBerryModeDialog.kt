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

package com.google.android.accessibility.talkback.blackberry

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.dialog.BaseDialog
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner

/**
 * The dialog that Turn BlackBerry mode on or off shows before it turns BlackBerry mode on, from the
 * service: BlackBerry's gestures, keys and sounds are unfamiliar to most people.
 */
class BlackBerryModeDialog(
  context: Context,
  pipeline: FeedbackReturner?,
  private val onTurnOn: Runnable,
) : BaseDialog(context, R.string.blackberry_mode_confirm_title, pipeline) {

  private val message = context.getString(R.string.blackberry_mode_confirm_message)

  init {
    setPositiveButtonStringRes(R.string.blackberry_mode_confirm_button)
  }

  override fun handleDialogClick(buttonClicked: Int) {
    if (buttonClicked == DialogInterface.BUTTON_POSITIVE) onTurnOn.run()
  }

  override fun handleDialogDismiss() {}

  override fun getMessageString(): String = message

  override fun getCustomizedView(inflater: LayoutInflater): View? = null
}
