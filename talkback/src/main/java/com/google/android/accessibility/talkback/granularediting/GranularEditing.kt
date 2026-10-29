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

package com.google.android.accessibility.talkback.granularediting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.ClipboardHistory
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Role

/**
 * Granular editing: the text of the focused item opens in a screen of its own, split into pieces
 * that can be edited one at a time. Saving writes the text back to the text field it came from.
 */
object GranularEditing {
  /** The text field the text came from, or null when the text isn't editable. */
  private var target: AccessibilityNodeInfoCompat? = null

  /** The text when the screen opened. */
  var text: String = ""
    private set

  /** Whether saving can write the text back. */
  val isEditable: Boolean
    get() = target != null

  private val handler = Handler(Looper.getMainLooper())

  /**
   * Opens the screen for [node]'s text. Returns false, after saying why, when it has none or is a
   * password field.
   */
  @JvmStatic
  fun open(context: Context, node: AccessibilityNodeInfoCompat?): Boolean {
    if (node == null) return refuse(context, R.string.granular_editing_no_text)
    if (node.isPassword) return refuse(context, R.string.granular_editing_password)
    val editable = node.isEditable || Role.getRole(node) == Role.ROLE_EDIT_TEXT
    val nodeText =
      when {
        editable && node.isShowingHintText -> ""
        editable -> fieldText(node)
        else -> AccessibilityNodeInfoUtils.getText(node)?.toString().orEmpty()
      }
    if (!editable && nodeText.isBlank()) return refuse(context, R.string.granular_editing_no_text)
    handler.removeCallbacksAndMessages(null)
    target = if (editable) AccessibilityNodeInfoCompat.obtain(node) else null
    text = nodeText
    context.startActivity(
      Intent(context, GranularEditingActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
    return true
  }

  /**
   * Writes [newText] to the text field once the screen has closed and the field's window is back,
   * trying for a few seconds. When the field is gone, the text goes to the clipboard instead, so
   * that it isn't lost.
   */
  fun save(context: Context, newText: String) {
    val appContext = context.applicationContext
    val field = target ?: return
    target = null
    var tries = 0
    val attempt =
      object : Runnable {
        override fun run() {
          // The field is spoken with its new text once it has focus again.
          if (write(field, newText)) return
          if (++tries < SAVE_TRIES) {
            handler.postDelayed(this, SAVE_INTERVAL_MS)
            return
          }
          copy(appContext, newText)
          Toast.makeText(appContext, R.string.granular_editing_not_saved, Toast.LENGTH_LONG).show()
        }
      }
    handler.postDelayed(attempt, SAVE_INTERVAL_MS)
  }

  /** Forgets the text field, when the screen closes without saving. */
  fun close() {
    target = null
  }

  fun copy(context: Context, copied: String) {
    ClipboardHistory.copy(context, copied)
  }

  private fun write(field: AccessibilityNodeInfoCompat, newText: String): Boolean {
    if (!field.refresh()) return false
    val arguments = Bundle()
    arguments.putCharSequence(
      AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
      newText,
    )
    if (!field.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, arguments)) return false
    // The cursor goes to the end, as after typing.
    val selection = Bundle()
    selection.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, newText.length)
    selection.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, newText.length)
    field.performAction(AccessibilityNodeInfoCompat.ACTION_SET_SELECTION, selection)
    return true
  }

  /**
   * The text typed in [field]. Compose text fields report their label after the text, as "text,
   * label", in both the text and the content description, with no hint of their own. The real
   * text is as long as the furthest the cursor can go, which this finds by moving the cursor, then
   * puts it back.
   */
  private fun fieldText(field: AccessibilityNodeInfoCompat): String {
    val reported = field.text?.toString().orEmpty()
    val mayHaveLabel =
      reported.contains(", ") && field.contentDescription?.toString() == reported
    if (!mayHaveLabel) return reported
    val start = field.textSelectionStart
    val end = field.textSelectionEnd
    if (canPlaceCursor(field, reported.length)) {
      setSelection(field, start, end)
      return reported
    }
    // The furthest the cursor can be placed.
    var low = 0
    var high = reported.length - 1
    while (low < high) {
      val middle = (low + high + 1) / 2
      if (canPlaceCursor(field, middle)) low = middle else high = middle - 1
    }
    setSelection(field, start, end)
    return reported.substring(0, low)
  }

  private fun canPlaceCursor(field: AccessibilityNodeInfoCompat, at: Int): Boolean =
    setSelection(field, at, at)

  private fun setSelection(field: AccessibilityNodeInfoCompat, start: Int, end: Int): Boolean {
    val arguments = Bundle()
    arguments.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, start)
    arguments.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, end)
    return field.performAction(AccessibilityNodeInfoCompat.ACTION_SET_SELECTION, arguments)
  }

  private fun refuse(context: Context, message: Int): Boolean {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    return false
  }

  private const val SAVE_TRIES = 15
  private const val SAVE_INTERVAL_MS = 200L
}
