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

package com.google.android.accessibility.talkback.macros

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.android.accessibility.talkback.R
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Home screen shortcuts that run a macro, through [MacroShortcutActivity].
 *
 * The activity isn't exported: the launcher starts a pinned shortcut through
 * LauncherApps.startShortcut, which starts its intent as SandFly. Each shortcut also carries a
 * random token saved with its macro, which the activity checks.
 */
object MacroShortcuts {
  private const val TAG = "MacroShortcuts"

  const val ACTION_RUN = "com.google.android.accessibility.talkback.macros.RUN_MACRO"
  const val EXTRA_MACRO = "macro"
  const val EXTRA_TOKEN = "token"

  private const val ID_PREFIX = "macro:"
  private const val TOKEN_BYTES = 16
  private val TOKEN = Regex("^[0-9a-f]{${TOKEN_BYTES * 2}}$")

  /** The longest macro ID a shortcut's intent is read with. */
  private const val MAX_ID = 200

  /** The result of [request]. */
  enum class Result {
    REQUESTED,
    /** The shortcut is on the home screen already. */
    ALREADY_ADDED,
    /** The launcher can't add shortcuts. */
    UNSUPPORTED,
    FAILED,
  }

  /** The ID of the shortcut of the macro [macroId]. */
  fun shortcutId(macroId: String): String = ID_PREFIX + macroId

  /** A new random token. */
  fun newToken(): String {
    val bytes = ByteArray(TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
    return bytes.joinToString("") { "%02x".format(it) }
  }

  /** Whether [text] has the form of a token from [newToken]. */
  fun isToken(text: String?): Boolean = text != null && TOKEN.matches(text)

  /** The intent of the shortcut that runs the macro [macroId] with [token]. */
  fun intent(context: Context, macroId: String, token: String): Intent =
    Intent(ACTION_RUN)
      .setClass(context, MacroShortcutActivity::class.java)
      .putExtra(EXTRA_MACRO, macroId)
      .putExtra(EXTRA_TOKEN, token)

  /**
   * Returns the macro of [macros] that a shortcut with [macroId] and [token] runs, or null when
   * there is no such macro or its token is another.
   */
  fun verified(macros: List<Macros.Macro>, macroId: String?, token: String?): Macros.Macro? {
    if (macroId.isNullOrEmpty() || macroId.length > MAX_ID || Macros.isExternal(macroId)) return null
    if (!isToken(token)) return null
    val macro = macros.firstOrNull { it.id == macroId } ?: return null
    val saved = macro.shortcutToken ?: return null
    return macro.takeIf { MessageDigest.isEqual(saved.toByteArray(), token!!.toByteArray()) }
  }

  /** Whether [macroId] could be the ID of a macro's shortcut, for disabling it. */
  fun isMacroId(macroId: String?): Boolean =
    !macroId.isNullOrEmpty() && macroId.length <= MAX_ID && !Macros.isExternal(macroId)

  /**
   * Asks the launcher to add a shortcut to the home screen that runs the macro [macroId], giving
   * the macro a token when it has none.
   */
  fun request(context: Context, prefs: SharedPreferences, macroId: String): Result {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return Result.UNSUPPORTED
    var macro = Macros.find(prefs, macroId)?.takeIf { !Macros.isExternal(it.id) } ?: return Result.FAILED
    if (macro.shortcutToken == null) {
      macro = macro.copy(shortcutToken = newToken())
      Macros.update(prefs, macro)
    }
    return try {
      val info = info(context, macro)
      // A shortcut added before, or disabled when a macro with this ID was deleted, gets the
      // current name and token.
      ShortcutManagerCompat.enableShortcuts(context, listOf(info))
      ShortcutManagerCompat.updateShortcuts(context, listOf(info))
      // Android adds a pinned shortcut again without asking the launcher.
      val pinned = context.getSystemService(ShortcutManager::class.java)?.pinnedShortcuts.orEmpty()
      if (pinned.any { it.id == info.id && it.isEnabled }) return Result.ALREADY_ADDED
      if (ShortcutManagerCompat.requestPinShortcut(context, info, null)) Result.REQUESTED else Result.FAILED
    } catch (e: RuntimeException) {
      // IllegalStateException in the background, IllegalArgumentException for a bad shortcut.
      Log.w(TAG, "Shortcut not requested", e)
      Result.FAILED
    }
  }

  /** Gives the shortcut of [macro] its name, when it has one. */
  fun update(context: Context, macro: Macros.Macro) {
    if (macro.shortcutToken == null || Macros.isExternal(macro.id)) return
    try {
      ShortcutManagerCompat.updateShortcuts(context, listOf(info(context, macro)))
    } catch (e: RuntimeException) {
      Log.w(TAG, "Shortcut not updated", e)
    }
  }

  /** Disables the shortcut of the macro [macroId], which no longer runs. */
  fun disable(context: Context, macroId: String) {
    try {
      ShortcutManagerCompat.disableShortcuts(
        context,
        listOf(shortcutId(macroId)),
        context.getString(R.string.macro_shortcut_deleted),
      )
    } catch (e: RuntimeException) {
      Log.w(TAG, "Shortcut not disabled", e)
    }
  }

  private fun info(context: Context, macro: Macros.Macro): ShortcutInfoCompat {
    val label = macro.name.trim().ifEmpty { context.getString(R.string.title_pref_macros) }
    return ShortcutInfoCompat.Builder(context, shortcutId(macro.id))
      .setShortLabel(label)
      .setLongLabel(label)
      .setIcon(IconCompat.createWithResource(context, R.drawable.icon))
      .setIntent(intent(context, macro.id, macro.shortcutToken!!))
      .build()
  }
}
