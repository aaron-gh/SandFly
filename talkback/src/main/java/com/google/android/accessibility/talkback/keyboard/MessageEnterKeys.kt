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
package com.google.android.accessibility.talkback.keyboard

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.macros.PassThroughTouch
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale
import java.util.function.BooleanSupplier
import java.util.function.Supplier

/**
 * Enter sends messages: in a messaging app's message field with text in it, Enter presses the app's
 * Send button. Shift + Enter and Alt + Enter go to the app, which types a new line.
 */
class MessageEnterKeys(private val context: Context) {
  /** Keys whose press was taken, so that their release is taken too. */
  private val takenKeys = mutableSetOf<Int>()

  /**
   * Handles [event] when it is Enter or the Send key in a message field. [inputFocus] gives the item with input
   * focus, where the user is typing when [typing] says so; both are asked only for Enter.
   */
  fun onKeyEvent(
    event: KeyEvent,
    inputFocus: Supplier<AccessibilityNodeInfoCompat?>,
    typing: BooleanSupplier,
  ): Boolean {
    val keyCode = event.keyCode
    if (event.action != KeyEvent.ACTION_DOWN) {
      return if (event.action == KeyEvent.ACTION_UP) takenKeys.remove(keyCode) else takenKeys.contains(keyCode)
    }
    if (takenKeys.contains(keyCode)) {
      return true
    }
    if (!sendsMessage(keyCode, isOn(context)) || event.repeatCount > 0 || !event.hasNoModifiers()) {
      return false
    }
    val typingNow = typing.asBoolean
    // Outside the message field, the Send key still presses a Send button on screen, such as the
    // one a messaging app shows for a voice message being recorded.
    if (!typingNow && keyCode != KeyEvent.KEYCODE_CALL) {
      return false
    }
    val sent =
      try {
        val field = if (typingNow) inputFocus.get() else null
        if (field != null) send(field) else pressSendOnScreen()
      } catch (e: RuntimeException) {
        // The app's window may go away while it is read.
        LogUtils.w(TAG, "Message not sent: %s", e)
        false
      }
    if (sent) {
      takenKeys.add(keyCode)
    }
    return sent
  }

  /** Presses the Send button of the messaging app in front, if it shows one. */
  private fun pressSendOnScreen(): Boolean {
    val service = context as? AccessibilityService ?: return false
    val root = service.rootInActiveWindow ?: return false
    val packageName = root.packageName?.toString() ?: return false
    if (!isMessagingApp(packageName)) return false
    val button = findSendButton(root) ?: findSendLabel(root) ?: return false
    if (button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
      LogUtils.e(TAG, "Send key clicked the Send button on screen in %s", packageName)
      return true
    }
    // A Send button drawn by the app, such as blindGram's for a voice message being recorded, may
    // not take the accessibility click: it is tapped instead.
    val bounds = Rect().also(button::getBoundsInScreen)
    val tapped = service is TalkBackService && PassThroughTouch.tap(service, bounds)
    LogUtils.e(TAG, "Send key tapped the Send button on screen in %s: %s", packageName, tapped)
    return tapped
  }

  /**
   * Finds a visible item labelled Send that isn't marked clickable, as an app draws for its own
   * buttons, among a few hundred items.
   */
  private fun findSendLabel(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.addLast(root)
    var seen = 0
    while (queue.isNotEmpty() && seen < MAX_NODES) {
      val node = queue.removeFirst()
      seen++
      if (node.isVisibleToUser && !node.isEditable && isSendLabel(node.contentDescription ?: node.text)) {
        return node
      }
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let(queue::addLast)
      }
    }
    return null
  }

  fun reset() {
    takenKeys.clear()
  }

  private fun send(field: AccessibilityNodeInfoCompat): Boolean {
    if (!field.isEditable || !field.isFocused || field.isPassword || field.isShowingHintText) {
      return false
    }
    val packageName = field.packageName?.toString() ?: return false
    if (!isMessagingApp(packageName) || field.text.isNullOrBlank()) {
      return false
    }
    val root = field.window?.root?.unwrap() ?: return false
    val button = findSendButton(root) ?: return false
    return button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
  }

  /** Finds the Send button among a few hundred items: by its ID, else by its label. */
  private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.addLast(root)
    var seen = 0
    var byLabel: AccessibilityNodeInfo? = null
    while (queue.isNotEmpty() && seen < MAX_NODES) {
      val node = queue.removeFirst()
      seen++
      if (node.isClickable && node.isEnabled && node.isVisibleToUser && !node.isEditable) {
        if (isSendId(node.viewIdResourceName)) {
          return node
        }
        if (byLabel == null && isSendLabel(node.contentDescription ?: node.text)) {
          byLabel = node
        }
      }
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let(queue::addLast)
      }
    }
    return byLabel
  }

  companion object {
    private const val TAG = "MessageEnterKeys"
    private const val MAX_NODES = 400

    /** Messaging apps, whose message fields have a Send button. */
    private val MESSAGING_APPS =
      setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.messaging",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.plus",
        "org.thunderdog.challegram",
        // Telegram apps built on Telegram's code.
        "me.blindgram.messenger",
        "org.telegram.messenger.beta",
        "tw.nekomimi.nekogram",
        "com.exteragram.messenger",
        "uz.unnarsx.cherrygram",
        "org.forkgram.messenger",
        "it.octogram.android",
        "org.thoughtcrime.securesms",
        "im.vector.app",
        "io.element.android.x",
        "com.discord",
        "com.facebook.orca",
        "com.Slack",
        "com.microsoft.teams",
        "com.viber.voip",
        "jp.naver.line.android",
        "ch.threema.app",
        "com.google.android.apps.dynamite",
        "com.beeper.android",
        "chat.simplex.app",
        "com.wire",
        "com.mattermost.rn",
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.google.android.apps.bard",
        "ai.perplexity.app.android",
      )

    /** Labels of Send buttons, lower case. */
    private val SEND_LABELS =
      setOf(
        "send",
        "send message",
        "send button",
        "send sms",
        "send mms",
        "submit",
        "enviar",
        "senden",
        "envoyer",
        "invia",
        "verzenden",
        "versturen",
        "wyślij",
        "отправить",
        "skicka",
        "send now",
      )

    @JvmStatic
    fun isOn(context: Context): Boolean =
      SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.resources,
        R.string.pref_enter_sends_messages_key,
        R.bool.pref_enter_sends_messages_default,
      )

    internal fun isMessagingApp(packageName: String): Boolean = packageName in MESSAGING_APPS

    /**
     * Whether a key sends the message in a message field: the Send key of a keyboard phone, such as
     * the Zinwa Q25's, always does, as it would otherwise open the phone app; Enter does when
     * [enterSendsMessages] is on.
     */
    internal fun sendsMessage(keyCode: Int, enterSendsMessages: Boolean): Boolean =
      keyCode == KeyEvent.KEYCODE_CALL ||
        (enterSendsMessages &&
          (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER))

    /** Whether a view ID, such as com.whatsapp:id/send or Compose:Draft:Send, names a Send button. */
    internal fun isSendId(viewId: String?): Boolean {
      if (viewId == null) {
        return false
      }
      val name = viewId.substringAfter(":id/").lowercase(Locale.ROOT)
      return name.contains("send") && !name.contains("sender")
    }

    internal fun isSendLabel(label: CharSequence?): Boolean =
      label != null && label.toString().trim().lowercase(Locale.ROOT) in SEND_LABELS
  }
}
