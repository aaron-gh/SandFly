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

package com.google.android.accessibility.talkback.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.os.IBinder
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.system.exitProcess

/**
 * Clears the output device that Android keeps for accessibility audio, as the shell user: in
 * SandFly's or the SandFly add-on's Shizuku service, or from adb, which runs [main] from the APK.
 *
 * Android has no call that clears the device it chose itself, but clearing an app's choice clears
 * the strategy's device in the audio policy, whoever chose it. So this chooses the speaker for
 * accessibility audio, and at once clears that choice. Android chooses again at the next call.
 */
object AccessibilityAudioOutput {
  /** AudioDeviceAttributes.ROLE_OUTPUT. */
  private const val ROLE_OUTPUT = 2
  private const val SUCCESS = 0

  /** Resets the output, as the shell user. Returns null if it worked, or what went wrong. */
  fun reset(): String? =
    try {
      val audio = audioService()
      val strategy = accessibilityStrategy() ?: return "No audio strategy for accessibility"
      val chosen = method(audio, "getPreferredDevicesForStrategy", 1).invoke(audio, strategy)
      if (chosen is List<*> && chosen.isNotEmpty()) {
        // An app chose it on purpose. Clearing would undo that.
        return "An app chose the accessibility output: $chosen"
      }
      val speaker =
        Class.forName("android.media.AudioDeviceAttributes")
          .getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
          .newInstance(ROLE_OUTPUT, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "")
      val set = method(audio, "setPreferredDevicesForStrategy", 2).invoke(audio, strategy, listOf(speaker))
      val cleared = method(audio, "removePreferredDevicesForStrategy", 1).invoke(audio, strategy)
      when {
        set != SUCCESS -> "Choosing the speaker failed with $set"
        cleared != SUCCESS -> "Clearing the output failed with $cleared"
        else -> null
      }
    } catch (e: InvocationTargetException) {
      e.targetException.toString()
    } catch (e: Exception) {
      e.toString()
    }

  /** Runs from adb, through app_process. */
  @JvmStatic
  fun main(args: Array<String>) {
    val error = reset()
    println(error ?: "Accessibility audio output reset")
    exitProcess(if (error == null) 0 else 1)
  }

  private fun audioService(): Any {
    val binder =
      Class.forName("android.os.ServiceManager")
        .getMethod("getService", String::class.java)
        .invoke(null, Context.AUDIO_SERVICE) as IBinder
    return Class.forName("android.media.IAudioService\$Stub")
      .getMethod("asInterface", IBinder::class.java)
      .invoke(null, binder)!!
  }

  /** The ID of the audio policy's strategy for accessibility audio. */
  private fun accessibilityStrategy(): Int? {
    val strategyClass = Class.forName("android.media.audiopolicy.AudioProductStrategy")
    val strategies = strategyClass.getMethod("getAudioProductStrategies").invoke(null) as List<*>
    val supports = strategyClass.getMethod("supportsAudioAttributes", AudioAttributes::class.java)
    val getId = strategyClass.getMethod("getId")
    val accessibility =
      AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).build()
    return strategies.firstOrNull { supports.invoke(it, accessibility) == true }?.let { getId.invoke(it) as Int }
  }

  private fun method(target: Any, name: String, parameters: Int): Method =
    target.javaClass.methods.first { it.name == name && it.parameterCount == parameters }
}
