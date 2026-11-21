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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyPresses
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils
import com.google.android.accessibility.utils.KeyboardUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.ValueSteps
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Leaves unsaid what apps and keyboards that speak by themselves cause SandFly to say:
 * - Speak only focus moved by the user: in all apps or in the chosen apps, focus that the app moved,
 *   announcements, live regions and window titles are not spoken. Focus that the user moved by
 *   touch, gestures, keys, a braille display or commands is.
 * - Speak only focus moved by the user while an assistant or call plays audio: the same in all
 *   apps, while another app plays audio with the assistant or voice communication usage.
 * - Keyboards with their own speech: while one of them is shown, its keys, typed and deleted text
 *   and cursor movement are not spoken.
 *
 * Only speech is left out: focus moves, and sounds and vibrations play, as before. The settings are
 * read by [load]; while they are at their defaults nothing is watched and [isActive] is false.
 */
object AppSpeechFilter {
  private const val TAG = "AppSpeechFilter"

  /** Values of Speak only focus moved by the user. */
  const val MODE_OFF = "off"
  const val MODE_ALL_APPS = "all"
  const val MODE_CHOSEN_APPS = "chosen"

  @JvmField val MODES: List<String> = listOf(MODE_OFF, MODE_ALL_APPS, MODE_CHOSEN_APPS)

  /** What caused speech, as far as the filter tells sources apart. */
  enum class Source {
    /** Focus the user moved: touch exploration, gestures, keys, braille display, commands. */
    USER_FOCUS,
    /** Focus an app moved, or that SandFly moved after a window or input focus change. */
    APP_FOCUS,
    /** An announcement, a live region change, a window title or a window change. */
    APP_SPEECH,
    /** Typed and deleted text and cursor movement in an edit field. */
    TEXT_EDIT,
  }

  /** What the filter applies now. */
  data class State(
    val mode: String = MODE_OFF,
    val apps: Set<String> = emptySet(),
    val assistantPlaying: Boolean = false,
    /** The package of the keyboard with its own speech that is shown, or null. */
    val keyboardPackage: String? = null,
    /** SandFly's own package, whose screens and overlays are always spoken. */
    val ownPackage: String? = null,
  ) {
    /** Whether anything can be left unsaid. */
    val active: Boolean
      get() =
        mode == MODE_ALL_APPS ||
          (mode == MODE_CHOSEN_APPS && apps.isNotEmpty()) ||
          assistantPlaying ||
          keyboardPackage != null
  }

  @Volatile private var state = State()
  @Volatile private var active = false
  // Normalized input method IDs of the keyboards with their own speech.
  @Volatile private var keyboards: Set<String> = emptySet()
  private var assistantAudio: AssistantAudio? = null

  /** Whether any speech can be left unsaid now; false while the settings are at their defaults. */
  @JvmStatic fun isActive(): Boolean = active

  /** Whether speech from [source] of an item or event of [packageName] is left unsaid now. */
  @JvmStatic
  fun mutes(source: Source, packageName: CharSequence?): Boolean =
    active && mutes(state, source, packageName)

  /** Whether speech from [source] of [packageName] is left unsaid in [state]. */
  @JvmStatic
  @VisibleForTesting
  fun mutes(state: State, source: Source, packageName: CharSequence?): Boolean {
    val pkg = packageName?.toString()
    if (state.keyboardPackage != null) {
      if (source == Source.TEXT_EDIT) return true
      if ((source == Source.USER_FOCUS || source == Source.APP_FOCUS) && pkg == state.keyboardPackage) {
        return true
      }
    }
    if (source != Source.APP_FOCUS && source != Source.APP_SPEECH) return false
    if (pkg != null && pkg == state.ownPackage) return false
    if (state.assistantPlaying) return true
    return when (state.mode) {
      MODE_ALL_APPS -> true
      MODE_CHOSEN_APPS -> pkg != null && pkg in state.apps
      else -> false
    }
  }

  /**
   * Whether focus set by an action of [sourceAction], one of [FocusActionInfo]'s source actions,
   * was moved by the user. Focus SandFly moves after a window change, a scroll or input focus is
   * counted as moved by the app, as the app may have caused it.
   */
  @JvmStatic
  fun isMovedByUser(sourceAction: Int): Boolean =
    sourceAction == FocusActionInfo.TOUCH_EXPLORATION ||
      sourceAction == FocusActionInfo.LOGICAL_NAVIGATION ||
      sourceAction == FocusActionInfo.KEYBOARD_SHORTCUT_REFOCUS

  /** The choice after [mode] in [MODES], or before it, as [ValueSteps] goes. */
  @JvmStatic
  fun step(mode: String, next: Boolean): String =
    MODES[ValueSteps.step(MODES.indexOf(mode).coerceAtLeast(0), MODES.size, next)]

  /**
   * The choice the command turns [mode] into: Off from either other choice, and from Off In chosen
   * apps when there are chosen apps, otherwise In all apps.
   */
  @JvmStatic
  fun toggle(mode: String, hasApps: Boolean): String =
    when {
      mode != MODE_OFF -> MODE_OFF
      hasApps -> MODE_CHOSEN_APPS
      else -> MODE_ALL_APPS
    }

  /** The value of Speak only focus moved by the user, one of [MODES]. */
  @JvmStatic
  fun mode(context: Context, prefs: SharedPreferences): String {
    val value =
      SharedPreferencesUtils.getStringPref(
        prefs,
        context.resources,
        R.string.pref_speak_user_focus_key,
        R.string.pref_speak_user_focus_default,
      )
    return if (value in MODES) value else MODE_OFF
  }

  @JvmStatic
  fun setMode(context: Context, prefs: SharedPreferences, mode: String) {
    prefs.edit { putString(context.getString(R.string.pref_speak_user_focus_key), mode) }
  }

  /** The name of [mode] as the setting lists it. */
  @JvmStatic
  fun modeName(context: Context, mode: String): String =
    context.resources.getStringArray(R.array.pref_speak_user_focus_entries)[
        MODES.indexOf(mode).coerceAtLeast(0)]

  /** The chosen apps' package names. */
  @JvmStatic
  fun apps(context: Context, prefs: SharedPreferences): Set<String> =
    prefs.getStringSet(context.getString(R.string.pref_speak_user_focus_apps_key), null).orEmpty()

  @JvmStatic
  fun setApps(context: Context, prefs: SharedPreferences, apps: Set<String>) {
    prefs.edit { putStringSet(context.getString(R.string.pref_speak_user_focus_apps_key), apps) }
  }

  /** The input method IDs of the keyboards with their own speech. */
  @JvmStatic
  fun keyboards(context: Context, prefs: SharedPreferences): Set<String> =
    prefs.getStringSet(context.getString(R.string.pref_keyboards_with_own_speech_key), null).orEmpty()

  @JvmStatic
  fun setKeyboards(context: Context, prefs: SharedPreferences, keyboards: Set<String>) {
    prefs.edit { putStringSet(context.getString(R.string.pref_keyboards_with_own_speech_key), keyboards) }
  }

  /** [id] as [android.view.inputmethod.InputMethodInfo.getId] gives it, or itself. */
  @VisibleForTesting
  @JvmStatic
  fun normalizeInputMethod(id: String): String =
    ComponentName.unflattenFromString(id)?.flattenToShortString() ?: id

  /**
   * The package of [inputMethod], the keyboard shown, if it is one of [keyboards], which are
   * normalized input method IDs; otherwise null.
   */
  @VisibleForTesting
  @JvmStatic
  fun keyboardPackageOf(inputMethod: String?, keyboards: Set<String>): String? {
    if (inputMethod.isNullOrEmpty()) return null
    val id = normalizeInputMethod(inputMethod)
    if (id !in keyboards) return null
    return ComponentName.unflattenFromString(id)?.packageName ?: id.substringBefore('/')
  }

  /**
   * Whether another app plays assistant or call audio, from the [usages] of the audio playing,
   * SandFly's own speech usage [ownUsage] and the audio [mode]. SandFly's speech never has the
   * assistant usage. When it has the voice communication usage, its players can't be told apart
   * from an app's, and the audio mode of an app call counts instead.
   */
  @VisibleForTesting
  @JvmStatic
  fun othersPlayAssistantOrCall(usages: Collection<Int>, ownUsage: Int, mode: Int): Boolean =
    (AudioAttributes.USAGE_ASSISTANT in usages && ownUsage != AudioAttributes.USAGE_ASSISTANT) ||
      VolumeKeyPresses.isAppCall(usages.toList(), ownUsage, mode)

  /** Reads the settings and starts or stops watching what they need. */
  @JvmStatic
  @MainThread
  fun load(service: AccessibilityService, prefs: SharedPreferences) {
    val mode = mode(service, prefs)
    val apps = apps(service, prefs)
    val watchAudio =
      SharedPreferencesUtils.getBooleanPref(
        prefs,
        service.resources,
        R.string.pref_speak_user_focus_assistant_audio_key,
        R.bool.pref_speak_user_focus_assistant_audio_default,
      )
    val newKeyboards = keyboards(service, prefs).map(::normalizeInputMethod).toSet()
    val keyboardsChanged = newKeyboards != keyboards
    keyboards = newKeyboards
    var keyboardPackage = state.keyboardPackage
    if (newKeyboards.isEmpty()) {
      keyboardPackage = null
    } else if (keyboardsChanged) {
      keyboardPackage = keyboardPackageOf(shownInputMethod(service), newKeyboards)
    }
    if (watchAudio && assistantAudio == null) {
      val appContext = service.applicationContext
      assistantAudio =
        AssistantAudio(
          service.getSystemService(AudioManager::class.java),
          ownUsage = { FailoverTextToSpeech.speechUsage(appContext) },
        ) { playing ->
          update { it.copy(assistantPlaying = playing) }
        }
    } else if (!watchAudio) {
      assistantAudio?.stop()
      assistantAudio = null
    }
    update {
      it.copy(
        mode = mode,
        apps = apps,
        assistantPlaying = watchAudio && it.assistantPlaying,
        keyboardPackage = keyboardPackage,
        ownPackage = service.packageName,
      )
    }
    // Started after the state is set, as it reports what plays now at once.
    assistantAudio?.start()
  }

  /** Stops watching, as SandFly stops. */
  @JvmStatic
  @MainThread
  fun stop() {
    assistantAudio?.stop()
    assistantAudio = null
    keyboards = emptySet()
    update { State() }
  }

  /** Whether the keyboard shown is needed. */
  @JvmStatic fun watchesKeyboards(): Boolean = keyboards.isNotEmpty()

  /**
   * Notes the keyboard shown, by its input method ID, such as "com.example/.Keyboard", or null when
   * none is.
   */
  @JvmStatic
  @MainThread
  fun onKeyboardShown(inputMethod: String?) {
    val keyboardPackage = keyboardPackageOf(inputMethod, keyboards)
    if (keyboardPackage != state.keyboardPackage) {
      update { it.copy(keyboardPackage = keyboardPackage) }
    }
  }

  private fun shownInputMethod(service: AccessibilityService): String? =
    if (AccessibilityServiceCompatUtils.isInputWindowOnScreen(service)) {
      KeyboardUtils.getCurrentInputMethod(service)
    } else {
      null
    }

  private inline fun update(change: (State) -> State) {
    val updated = change(state)
    if (updated == state) return
    LogUtils.d(TAG, "State: %s", updated)
    state = updated
    active = updated.active
  }

  /**
   * Watches the audio that apps play with an [AudioManager.AudioPlaybackCallback], while
   * registered, and tells [onChange] whether another app plays assistant or call audio.
   */
  private class AssistantAudio(
    private val audioManager: AudioManager?,
    private val ownUsage: () -> Int,
    private val onChange: (Boolean) -> Unit,
  ) {
    private var registered = false
    private val callback =
      object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
          if (registered) update(configs)
        }
      }

    fun start() {
      val manager = audioManager ?: return
      if (registered) return
      registered = true
      // The callback runs on the main thread.
      manager.registerAudioPlaybackCallback(callback, null)
      update(manager.activePlaybackConfigurations)
    }

    fun stop() {
      if (!registered) return
      registered = false
      audioManager?.unregisterAudioPlaybackCallback(callback)
    }

    private fun update(configs: List<AudioPlaybackConfiguration>) {
      val manager = audioManager ?: return
      val usages = configs.map { it.audioAttributes.usage }
      onChange(othersPlayAssistantOrCall(usages, ownUsage(), manager.mode))
    }
  }
}
