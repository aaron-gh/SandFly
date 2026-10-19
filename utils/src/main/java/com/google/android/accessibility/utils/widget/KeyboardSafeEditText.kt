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

package com.google.android.accessibility.utils.widget

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.accessibility.utils.input.KeyboardCalls

/**
 * A text field in one of the screen reader's own windows. A keyboard in another app has its calls
 * answered as [KeyboardCalls] describes, so typing doesn't stall the screen reader or the keyboard.
 *
 * The screen reader's own braille keyboard runs on this app's main thread, where Android answers
 * its calls at once, so it gets the field's own connection. Through [KeyboardCalls], each of its
 * questions would wait for the main thread it runs on, and get no answer: typing was slow, and
 * deleting a word did nothing.
 */
open class KeyboardSafeEditText
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

  override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
    val connection = super.onCreateInputConnection(outAttrs)
    return if (isOwnKeyboard(context)) connection else KeyboardCalls.wrap(connection)
  }

  companion object {
    /** Whether the keyboard in use belongs to this app, and so runs in its process. */
    @VisibleForTesting
    fun isOwnKeyboard(context: Context): Boolean = keyboardPackage(context) == context.packageName

    private fun keyboardPackage(context: Context): String? {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(InputMethodManager::class.java)?.currentInputMethodInfo?.let {
          return it.packageName
        }
      }
      return try {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
          ?.let { ComponentName.unflattenFromString(it)?.packageName }
      } catch (e: SecurityException) {
        null
      }
    }
  }
}
