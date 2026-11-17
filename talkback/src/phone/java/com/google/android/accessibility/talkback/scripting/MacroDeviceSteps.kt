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

package com.google.android.accessibility.talkback.scripting

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.talkback.translation.Translator
import org.json.JSONObject

/**
 * The macro steps that use the code scripts use: Android's settings pages, volume and ringer mode,
 * keys through Shizuku, messages, fetching and translating. Called on the main thread.
 */
object MacroDeviceSteps : Macros.DeviceSteps {
  private val mainHandler = Handler(Looper.getMainLooper())
  private var dialogs: Pair<AccessibilityService, ScriptDialogs>? = null
  private var translator: Pair<AccessibilityService, Translator>? = null

  override fun run(service: AccessibilityService, step: Macros.Do, text: String, done: (Boolean, String?) -> Unit) {
    try {
      when (step.kind) {
        Macros.DoKind.OPEN_SETTINGS ->
          done(ScriptDevice(service).openSettings(JSONObject().put("page", step.text)), null)
        Macros.DoKind.SET_VOLUME -> {
          val device = ScriptDevice(service)
          val stream = JSONObject().put("stream", step.name.ifEmpty { "media" })
          val range = device.volume(stream)
          val min = range.optInt("min")
          val level = min + (range.optInt("max") - min) * step.number.coerceIn(0, 100) / 100
          device.setVolume(stream.put("level", level))
          done(true, null)
        }
        Macros.DoKind.SET_RINGER -> {
          ScriptDevice(service).setRingerMode(JSONObject().put("mode", step.text))
          done(true, null)
        }
        Macros.DoKind.PRESS_KEYS -> {
          if (!ShizukuShell.isAllowed()) return done(false, "Shizuku isn't running or doesn't allow SandFly")
          val codes = text.split(' ', ',').filter { it.isNotBlank() }.map { name ->
            val upper = name.trim().uppercase()
            val full = if (upper.startsWith("KEYCODE_")) upper else "KEYCODE_" + (ScriptTouch.KEY_ALIASES[upper] ?: upper)
            if (KeyEvent.keyCodeFromString(full) == KeyEvent.KEYCODE_UNKNOWN) return done(false, "unknown key $name")
            full
          }
          if (codes.isEmpty()) return done(false, "no keys")
          ShizukuShell.run(service, ShizukuCommand.INPUT_KEYEVENT, codes.joinToString(" ")) { worked ->
            mainHandler.post { done(worked, null) }
          }
        }
        Macros.DoKind.SHOW_MESSAGE -> {
          val shown = dialogs?.takeIf { it.first === service }?.second ?: ScriptDialogs(service).also { dialogs = service to it }
          shown.alert(OWNER, "", text) { mainHandler.post { done(true, null) } }
        }
        Macros.DoKind.FETCH -> {
          val request = ScriptFetch.request(JSONObject().put("url", text.trim()))
          ScriptFetch.start(request) { worked, result ->
            val body = (result as? JSONObject)?.optString("body")
            mainHandler.post {
              if (worked && body != null) done(true, body.take(Macros.MAX_VALUE_CHARS))
              else done(false, result?.toString() ?: "fetch failed")
            }
          }
        }
        Macros.DoKind.TRANSLATE -> {
          val translating =
            translator?.takeIf { it.first === service }?.second ?: Translator(service).also { translator = service to it }
          translating.translateTo(text, null, null) { result ->
            mainHandler.post {
              when (result) {
                is Translator.Result.Done -> done(true, result.translation.text)
                is Translator.Result.Failed -> done(false, "translation failed: ${result.failure.kind}")
                is Translator.Result.Downloading -> {}
              }
            }
          }
        }
        else -> done(false, "not a device step")
      }
    } catch (e: ScriptApiException) {
      done(false, e.message)
    } catch (e: RuntimeException) {
      done(false, e.toString())
    }
  }

  /** The owner of the messages of macros among SandFly's dialogs, which scripts own by their ids. */
  private const val OWNER = "macro"
}
