package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService
import android.content.res.Resources
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.braille.SendButtonFinder

/**
 * Sends the text in the edit box with input focus for the Submit text command: presses the app's
 * send button beside it, as chat apps need, or else performs the keyboard's action on it.
 */
object SubmitText {

  private const val EXTRACT_EDIT_TEXT_ID_SUFFIX = ":id/extract_edit_text"
  private const val ACTION_KEY_ID_SUFFIX = ":id/key_pos_ime_action"
  private const val ACTION_KEY_ID_PART = "ime_action"

  /** Nodes searched per keyboard window when the action key has no known ID. */
  private const val MAX_SEARCHED_NODES = 500

  /** Returns whether the text was sent. */
  @JvmStatic
  fun submit(service: AccessibilityService): Boolean {
    val field =
      service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { AccessibilityNodeInfoCompat.wrap(it) }
        ?: return false
    if (!field.isEditable) return false
    field.refresh()
    // A keyboard's full-screen editor, as on Wear OS, returns true for ACTION_IME_ENTER without
    // performing the action. The keyboard's action key performs it.
    if (isKeyboardEditor(field)) {
      actionKey(service)?.let {
        return it.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
      }
    }
    SendButtonFinder.find(field, sendWords())?.let {
      return it.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
    }
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
      field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
  }

  /** Returns whether [field] is an input method's extracted editor rather than the app's field. */
  private fun isKeyboardEditor(field: AccessibilityNodeInfoCompat): Boolean {
    if (field.viewIdResourceName?.endsWith(EXTRACT_EDIT_TEXT_ID_SUFFIX) == true) return true
    return field.unwrap().window?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
  }

  /** Returns the visible action key in an input method window, or null. */
  private fun actionKey(service: AccessibilityService): AccessibilityNodeInfoCompat? {
    val windows = runCatching { service.windows }.getOrNull() ?: return null
    for (window in windows) {
      if (window.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue
      val root = window.root ?: continue
      val keyboardPackage = root.packageName?.toString()
      if (keyboardPackage != null) {
        root
          .findAccessibilityNodeInfosByViewId(keyboardPackage + ACTION_KEY_ID_SUFFIX)
          .firstOrNull { it.isVisibleToUser }
          ?.let { return AccessibilityNodeInfoCompat.wrap(it) }
      }
      findByIdPart(root)?.let { return AccessibilityNodeInfoCompat.wrap(it) }
    }
    return null
  }

  /** Returns the first visible, clickable node under [root] whose view ID contains "ime_action". */
  private fun findByIdPart(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.addLast(root)
    var searched = 0
    while (queue.isNotEmpty() && searched < MAX_SEARCHED_NODES) {
      val node = queue.removeFirst()
      searched++
      if (node.viewIdResourceName?.contains(ACTION_KEY_ID_PART) == true &&
        node.isVisibleToUser &&
        (node.isClickable ||
          node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK))
      ) {
        return node
      }
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let { queue.addLast(it) }
      }
    }
    return null
  }

  /** The words a send button is labeled with: Android's own "Send", in the user's language. */
  private fun sendWords(): List<String> {
    val system = Resources.getSystem()
    val id = system.getIdentifier("ime_action_send", "string", "android")
    val send = if (id != 0) system.getString(id) else null
    return listOfNotNull(send, "Send").distinct()
  }
}
