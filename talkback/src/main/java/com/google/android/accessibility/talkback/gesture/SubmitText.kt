package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService
import android.content.res.Resources
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.braille.SendButtonFinder

/**
 * Sends the text in the edit box with input focus for the Submit text command: presses the app's
 * send button beside it, as chat apps need, or else performs the keyboard's action on it.
 */
object SubmitText {

  /** Returns whether the text was sent. */
  @JvmStatic
  fun submit(service: AccessibilityService): Boolean {
    val field =
      service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { AccessibilityNodeInfoCompat.wrap(it) }
        ?: return false
    if (!field.isEditable) return false
    field.refresh()
    SendButtonFinder.find(field, sendWords())?.let {
      return it.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
    }
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
      field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
  }

  /** The words a send button is labeled with: Android's own "Send", in the user's language. */
  private fun sendWords(): List<String> {
    val system = Resources.getSystem()
    val id = system.getIdentifier("ime_action_send", "string", "android")
    val send = if (id != 0) system.getString(id) else null
    return listOfNotNull(send, "Send").distinct()
  }
}
