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
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
 * while its window is closed: Edit, a dialog that changes the text of an entry, Limits, a dialog
 * that sets how many texts the history keeps and for how many days, and Export to file,
 * which saves the whole history, or the texts [ClipboardHistoryOverlay.exportedEntries], as a UTF-8
 * text file in the format [ClipboardHistory.exportText] describes. The window shows again when the
 * dialog closes or the file picker returns. Leaving a dialog for another screen discards the
 * change, and the window doesn't show again.
 */
class ClipboardHistoryActivity : AppCompatActivity() {

  private val createFile =
    registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
      // Taken before the window shows again, which forgets them.
      val exported = ClipboardHistoryOverlay.exportedEntries
      if (uri != null) {
        // The whole history is read when the file is written, off the main thread. It holds at
        // most MAX_TOTAL_LENGTH characters.
        ItemFileLauncher.exportText(this, uri) {
          ClipboardHistory.exportText(exported ?: ClipboardHistory.entries(it))
        }
      }
      end(focusText = exported?.singleOrNull())
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
      MODE_LIMITS -> showLimits()
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

  /**
   * Shows the number fields for the most texts the history keeps, from 1 to
   * [ClipboardHistory.MAX_ENTRIES], and the days it keeps them, empty for no limit. Larger numbers
   * are saved as the largest.
   */
  private fun showLimits() {
    val maxField =
      numberField(ClipboardHistory.MAX_ENTRIES.toString().length).apply {
        setText(ClipboardHistory.maxEntries(this@ClipboardHistoryActivity).toString())
      }
    val daysField =
      numberField(DAYS_DIGITS).apply {
        hint = getString(R.string.clipboard_history_days_unlimited)
        ClipboardHistory.days(this@ClipboardHistoryActivity).takeIf { it > 0 }?.let {
          setText(it.toString())
        }
      }
    val padding = (resources.displayMetrics.density * PADDING_DP).toInt()
    val fields =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, padding / 2, padding, 0)
        addLabelled(
          getString(R.string.clipboard_history_max_entries, ClipboardHistory.MAX_ENTRIES),
          maxField,
        )
        addLabelled(getString(R.string.clipboard_history_days), daysField)
      }
    val limits =
      AlertDialog.Builder(this)
        .setTitle(R.string.clipboard_history_limits)
        .setView(fields)
        .setPositiveButton(R.string.granular_editing_save) { _, _ ->
          val max = number(maxField) ?: return@setPositiveButton
          ClipboardHistory.setLimits(this, max, number(daysField) ?: 0)
          end(focusText = null)
        }
        .setNegativeButton(android.R.string.cancel) { _, _ -> end(focusText = null) }
        .setOnCancelListener { end(focusText = null) }
        .setOnDismissListener { if (!ended && !isChangingConfigurations) end(focusText = null) }
        .show()
    limits.keepButtonCase()
    limits.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    // The most texts must be a number above 0.
    val save = limits.getButton(AlertDialog.BUTTON_POSITIVE)
    save?.isEnabled = (number(maxField) ?: 0) > 0
    maxField.addTextChangedListener(
      object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
          save?.isEnabled = (number(maxField) ?: 0) > 0
        }
      }
    )
    maxField.requestFocus()
    maxField.setSelection(maxField.length())
    dialog = limits
  }

  private fun numberField(digits: Int): EditText =
    KeyboardSafeEditText(this).apply {
      id = View.generateViewId()
      inputType = InputType.TYPE_CLASS_NUMBER
      isSingleLine = true
      filters = arrayOf(InputFilter.LengthFilter(digits))
    }

  private fun LinearLayout.addLabelled(label: String, field: EditText) {
    addView(
      TextView(context).apply {
        text = label
        labelFor = field.id
      }
    )
    addView(field)
  }

  /** The field's number; null when it is empty. */
  private fun number(field: EditText): Int? = field.text?.toString()?.trim()?.toIntOrNull()

  override fun onStop() {
    super.onStop()
    // A dialog is left for another screen. Export goes on while the file picker is in front.
    if (
      !ended &&
        !isChangingConfigurations &&
        intent.getStringExtra(EXTRA_MODE) != MODE_EXPORT
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
    const val MODE_LIMITS = "limits"
    private const val MAX_LINES = 12
    /** The most digits of Days kept: up to 99,999 days. */
    private const val DAYS_DIGITS = 5
    private const val PADDING_DP = 24
  }
}
