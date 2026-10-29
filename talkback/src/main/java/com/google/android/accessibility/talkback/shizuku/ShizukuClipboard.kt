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

package com.google.android.accessibility.talkback.shizuku

import android.content.Context
import android.content.res.Resources
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.utils.ClipboardHistory
import java.util.Locale

/**
 * Adds every text copied in any app to the clipboard history, while Shizuku runs and allows
 * SandFly: Shizuku's service, as the shell user, may read the clipboard in the background, which
 * SandFly itself may not. Without Shizuku, the history has only what SandFly copied and what it
 * found while its own screens had focus.
 */
object ShizukuClipboard {

  // How long after a copy is read Android's toast about the read may come.
  private const val READ_TOAST_MILLIS = 3_000L

  private var context: Context? = null
  private var watching = false
  @Volatile private var lastRead = 0L

  private var readToast: Pair<Locale, Regex?>? = null

  private val watcher =
    object : IClipboardWatcher.Stub() {
      override fun onClipText(text: String?) {
        lastRead = SystemClock.uptimeMillis()
        val context = context ?: return
        ClipboardHistory.record(context, text)
      }
    }

  private val onConnected: (IShizukuGrants) -> Unit = { it.watchClipboard(watcher) }

  /** Starts watching the clipboard, if Shizuku runs and allows SandFly already. */
  @JvmStatic
  fun start(context: Context) {
    if (watching || !ShizukuShell.isAllowed()) return
    this.context = context.applicationContext
    watching = true
    ShizukuShell.addConnectListener(onConnected)
    ShizukuShell.hold(context)
  }

  /** Whether every copy goes into the history as it is made. */
  @JvmStatic fun isWatching(): Boolean = watching

  /** Stops watching, such as when Shizuku stops, which ends its service. */
  @JvmStatic
  fun stop() {
    if (!watching) return
    watching = false
    ShizukuShell.removeConnectListener(onConnected)
    ShizukuShell.release()
  }

  /**
   * Whether [event] is Android's toast saying that the shell pasted from the clipboard, which
   * Android shows each time a copy is read for the history.
   */
  @JvmStatic
  fun isReadToast(event: AccessibilityEvent): Boolean {
    if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return false
    if (SystemClock.uptimeMillis() - lastRead > READ_TOAST_MILLIS) return false
    val text = event.text.joinToString(" ")
    return readToastPattern()?.matches(text) == true
  }

  /** Android's "%1$s pasted from your clipboard", in the system's language, with any app name. */
  private fun readToastPattern(): Regex? {
    val locale = Resources.getSystem().configuration.locales[0]
    readToast?.let { (cached, pattern) -> if (cached == locale) return pattern }
    val pattern =
      try {
        val resources = Resources.getSystem()
        val id = resources.getIdentifier("pasted_from_clipboard", "string", "android")
        val format = if (id == 0) null else resources.getString(id)
        val parts = format?.split("%1\$s", "%s")?.takeIf { it.size == 2 }
        parts?.let { Regex(Regex.escape(it[0]) + ".+" + Regex.escape(it[1])) }
      } catch (e: RuntimeException) {
        null
      }
    readToast = locale to pattern
    return pattern
  }
}
