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

package com.google.android.accessibility.talkback.clipboard

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.backup.ItemFileLauncher
import com.google.android.accessibility.talkback.notificationlist.NotificationReplyActivity.Companion.keepButtonCase
import com.google.android.accessibility.utils.ClipboardHistory
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText

/**
 * The parts of the clipboard history that need an activity, opened from [ClipboardHistoryOverlay]
 * while its window is closed: Edit, a dialog that changes the text of an entry, and Export to file,
 * which saves the whole history as a UTF-8 text file in the format [ClipboardHistory.exportText]
 * describes. The window shows again when the dialog closes or the file picker returns. Leaving the
 * Edit dialog for another screen discards the change, and the window doesn't show again.
 */
class ClipboardHistoryActivity : AppCompatActivity() {

  private val createFile =
    registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
      if (uri != null) {
        // Read when the file is written, off the main thread; the history holds at most
        // MAX_ENTRIES texts of MAX_LENGTH characters.
        ItemFileLauncher.exportText(this, uri) { ClipboardHistory.exportText(ClipboardHistory.entries(it)) }
      }
      end(focusText = null)
    }

  private var dialog: AlertDialog? = null

  // Whether the activity ended as the user chose, rather than by leaving for another screen.
  private var ended = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    when (intent.getStringExtra(EXTRA_MODE)) {
      MODE_EDIT -> {
        val entry = ClipboardHistoryOverlay.editedEntry
        if (entry == null) finishWithoutWindow() else showEditor(entry)
      }
      MODE_EXPORT -> {
        // After a configuration change the file picker is already open, and its result comes to
        // this activity.
        if (savedInstanceState == null) {
          try {
            createFile.launch(ClipboardHistory.EXPORT_FILE_NAME)
          } catch (e: RuntimeException) {
            // No app handles creating a document.
            end(focusText = null)
          }
        }
      }
      else -> finishWithoutWindow()
    }
  }

  private fun showEditor(entry: String) {
    val field =
      KeyboardSafeEditText(this).apply {
        setText(entry)
        inputType =
          InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        // Long texts scroll in the field, so that the buttons stay on the screen.
        maxLines = MAX_LINES
        setSelection(entry.length)
      }
    val editor =
      AlertDialog.Builder(this)
        .setTitle(R.string.notification_edit)
        .setView(field)
        .setPositiveButton(R.string.granular_editing_save) { _, _ ->
          val edited = field.text?.toString().orEmpty()
          ClipboardHistory.replace(this, entry, edited)
          end(focusText = edited.take(ClipboardHistory.MAX_LENGTH))
        }
        .setNegativeButton(android.R.string.cancel) { _, _ -> end(focusText = entry) }
        .setOnCancelListener { end(focusText = entry) }
        .setOnDismissListener { if (!ended && !isChangingConfigurations) end(focusText = entry) }
        .show()
    editor.keepButtonCase()
    editor.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    // An entry can't be blank.
    val save = editor.getButton(AlertDialog.BUTTON_POSITIVE)
    save?.isEnabled = entry.isNotBlank()
    field.addTextChangedListener(
      object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
          save?.isEnabled = !s.isNullOrBlank()
        }
      }
    )
    field.requestFocus()
    dialog = editor
  }

  override fun onStop() {
    super.onStop()
    // Edit is left for another screen. Export goes on while the file picker is in front.
    if (
      !ended &&
        !isChangingConfigurations &&
        intent.getStringExtra(EXTRA_MODE) == MODE_EDIT
    ) {
      ended = true
      ClipboardHistoryOverlay.forgetSuspended()
      dialog?.dismiss()
      finish()
    }
  }

  override fun onDestroy() {
    dialog?.setOnDismissListener(null)
    dialog?.dismiss()
    dialog = null
    super.onDestroy()
  }

  /** Closes the activity and shows the clipboard history again, with focus on [focusText]. */
  private fun end(focusText: String?) {
    if (ended) return
    ended = true
    finish()
    ClipboardHistoryOverlay.resume(focusText)
  }

  private fun finishWithoutWindow() {
    ended = true
    ClipboardHistoryOverlay.forgetSuspended()
    finish()
  }

  companion object {
    const val EXTRA_MODE = "mode"
    const val MODE_EDIT = "edit"
    const val MODE_EXPORT = "export"
    private const val MAX_LINES = 12
  }
}
