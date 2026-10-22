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

package com.google.android.accessibility.talkback.handwriting

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.dialog.BaseDialog

/**
 * Asks before the handwriting model is downloaded, the first time handwriting is used, and says
 * when the download ends.
 */
object HandwritingDownload {
  /**
   * Asks whether to download the model, in a dialog over the screen when [context] is SandFly's
   * service, and downloads it with [recognizer] if the user agrees.
   */
  @JvmStatic
  fun ask(context: Context, feedback: Pipeline.FeedbackReturner?, recognizer: HandwritingRecognizer) {
    Dialog(context, feedback) { recognizer.download() }.showDialog()
  }

  /** Shows a toast that the download ended, [success] or not. */
  @JvmStatic
  fun onDone(context: Context, success: Boolean) {
    Toast.makeText(
        context.applicationContext,
        if (success) R.string.handwriting_downloaded else R.string.handwriting_download_failed,
        Toast.LENGTH_LONG,
      )
      .show()
  }

  private class Dialog(
    context: Context,
    feedback: Pipeline.FeedbackReturner?,
    private val onDownload: () -> Unit,
  ) : BaseDialog(context, R.string.handwriting_download_title, feedback) {
    init {
      setPositiveButtonStringRes(R.string.handwriting_download_button)
    }

    override fun handleDialogClick(buttonClicked: Int) {
      if (buttonClicked == DialogInterface.BUTTON_POSITIVE) onDownload()
    }

    override fun handleDialogDismiss() {}

    override fun getMessageString(): String = context.getString(R.string.handwriting_download_message)

    override fun getCustomizedView(inflater: LayoutInflater): View? = null
  }
}
