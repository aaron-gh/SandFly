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
package com.google.android.accessibility.talkback.applauncher

import android.accessibilityservice.AccessibilityService
import android.content.ComponentCallbacks
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Size
import com.google.android.accessibility.braille.common.BrailleUserPreferences
import com.google.android.accessibility.braille.common.BrailleUtils
import com.google.android.accessibility.braille.common.DisplayKeyOverrides
import com.google.android.accessibility.braille.common.TouchDots
import com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils
import com.google.android.accessibility.braille.interfaces.BrailleCharacter
import com.google.android.accessibility.braille.interfaces.BrailleWord
import com.google.android.accessibility.braille.translate.BrailleTranslator
import com.google.android.accessibility.brailleime.BrailleImeTypingSounds
import com.google.android.accessibility.brailleime.BrailleInputOptions
import com.google.android.accessibility.brailleime.HeldOrientationTracker
import com.google.android.accessibility.brailleime.LayoutOrientator
import com.google.android.accessibility.brailleime.OrientationMonitor
import com.google.android.accessibility.brailleime.Utils
import com.google.android.accessibility.brailleime.input.BrailleInputView
import com.google.android.accessibility.brailleime.input.DotHoldSwipe
import com.google.android.accessibility.brailleime.input.DotsOrientation
import com.google.android.accessibility.brailleime.input.Swipe
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.CommandAssignments
import com.google.android.accessibility.talkback.handwriting.HandwritingOverlay
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Opens an app by the start of its name, typed in braille on the whole screen, as on the braille
 * keyboard. The cells are read as uncontracted braille of the braille keyboard's current language,
 * and SandFly says the first app whose name, or a word of it, starts with them, and how many apps
 * do. Swipes work as on the braille keyboard: up and down with one finger for the
 * previous and next app, left to delete, up with two fingers to open the app and down with two
 * fingers to close.
 */
class BrailleAppLauncher(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  actorState: ActorState,
) {
  private val handler = Handler(Looper.getMainLooper())
  private val overlay =
    HandwritingOverlay(service, pipeline, actorState, service.getString(R.string.shortcut_braille_app_name))
  private val search = AppNameSearch(service, pipeline, R.string.braille_app_prompt, ::dismiss)
  private val closeIdle = Runnable { cancel() }
  private var orientator: LayoutOrientator? = null
  private var inputView: BrailleInputView? = null
  private var translator: BrailleTranslator? = null
  private var orientation = Configuration.ORIENTATION_UNDEFINED
  private var screenSize: Size? = null
  /** Whether this turned the orientation monitor on, which the dots read when touched. */
  private var enabledOrientationMonitor = false

  /** Lays the dots out again when the screen turns, or a foldable folds or unfolds. */
  private val configurationCallbacks =
    object : ComponentCallbacks {
      override fun onConfigurationChanged(newConfig: Configuration) {
        val view = inputView ?: return
        val size = Utils.getDisplaySizeInPixels(service)
        if (newConfig.orientation == orientation && size == screenSize) return
        orientation = newConfig.orientation
        screenSize = size
        view.onOrientationChanged(orientation, size)
      }

      @Deprecated("Deprecated in Java") override fun onLowMemory() {}
    }

  /** The cells typed so far. */
  private val cells = BrailleWord()

  private val prefs = SharedPreferencesUtils.getSharedPreferences(service)
  /** Where the braille display keys assigned to commands are saved. */
  private val braillePrefs =
    BrailleUserPreferences.getSharedPreferences(
      service,
      BrailleUserPreferences.BRAILLE_SHARED_PREFS_FILENAME,
    )
  private val assignedCheck = Runnable { updateAssigned() }
  /** Kept in a field: SharedPreferences holds its listeners weakly. */
  private val assignmentListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
      // A change of several settings at once is read once.
      handler.removeCallbacks(assignedCheck)
      handler.post(assignedCheck)
    }

  init {
    prefs.registerOnSharedPreferenceChangeListener(assignmentListener)
    braillePrefs.registerOnSharedPreferenceChangeListener(assignmentListener)
    updateAssigned()
  }

  val isShowing: Boolean
    get() = overlay.isShowing

  /** Shows the braille surface, or says why it can't. */
  fun show() {
    if (isShowing) return
    // Touches reach the surface only where they can pass touch exploration by, from Android 11.
    if (!FeatureSupport.supportPassthrough()) {
      speak(service.getString(R.string.braille_app_not_available))
      return
    }
    val code = BrailleUserPreferences.readCurrentActiveInputCodeAndCorrect(service)
    translator =
      try {
        BrailleUserPreferences.readTranslatorFactory(service)
          .create(service, code.name, /* contractedMode= */ false)
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "No braille translator for %s: %s", code, e)
        null
      }
    if (translator == null) {
      speak(service.getString(R.string.braille_app_not_available))
      return
    }
    val orientator = LayoutOrientator(service, orientatorCallback).also { orientator = it }
    orientator.startIfNeeded()
    val size = Utils.getDisplaySizeInPixels(service)
    val view =
      BrailleInputView(
        service,
        inputCallback,
        size,
        BrailleInputOptions.builder()
          .setReverseDots(BrailleUserPreferences.readReverseDotsMode(service))
          .setFlipDotsVertically(BrailleUserPreferences.readFlipDotsVertically(service))
          .setBrailleType(BrailleUserPreferences.getCurrentTypingLanguageType(service))
          .setTutorialMode(false)
          .build(),
        isTabletop(orientator),
      )
    view.setAccumulationMode(BrailleUserPreferences.readAccumulateMode(service))
    if (!overlay.show(view)) {
      orientator.stop()
      this.orientator = null
      translator = null
      return
    }
    inputView = view
    OrientationMonitor.init(service)
    OrientationMonitor.getInstance().let {
      if (!it.isEnabled) {
        it.enable()
        enabledOrientationMonitor = true
      }
    }
    orientation = service.resources.configuration.orientation
    screenSize = size
    service.registerComponentCallbacks(configurationCallbacks)
    cells.clear()
    search.start()
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
          service.getString(R.string.braille_app_prompt),
          AppNameSearch.speakOptions(SpeechController.QUEUE_MODE_FLUSH_ALL),
        )
        .sound(R.raw.chime_up),
    )
    restartIdleTimer()
  }

  /** Closes the braille surface without opening an app. */
  fun cancel() {
    if (!isShowing) return
    dismiss()
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
          service.getString(R.string.braille_app_closed),
          AppNameSearch.speakOptions(SpeechController.QUEUE_MODE_INTERRUPT),
        )
        .sound(R.raw.chime_down),
    )
  }

  /** Closes the braille surface without any feedback, such as when SandFly stops. */
  fun dismiss() {
    handler.removeCallbacks(closeIdle)
    orientator?.stop()
    orientator = null
    if (enabledOrientationMonitor) {
      enabledOrientationMonitor = false
      // The braille keyboard registers a callback while it is shown, and keeps the monitor on.
      OrientationMonitor.getInstance().let { if (!it.hasCallback()) it.disable() }
    }
    if (inputView != null) service.unregisterComponentCallbacks(configurationCallbacks)
    inputView = null
    translator = null
    cells.clear()
    search.stop()
    overlay.remove()
  }

  fun shutdown() {
    dismiss()
    search.shutdown()
    prefs.unregisterOnSharedPreferenceChangeListener(assignmentListener)
    braillePrefs.unregisterOnSharedPreferenceChangeListener(assignmentListener)
    handler.removeCallbacks(assignedCheck)
    HeldOrientationTracker.setLauncherAssigned(false)
  }

  /**
   * Whether the dots are laid out for a device lying flat, as the braille keyboard decides it. Until
   * the sensors tell, landscape is taken as lying flat.
   */
  private fun isTabletop(orientator: LayoutOrientator): Boolean =
    when (BrailleUserPreferences.readLayoutMode(service)) {
      TouchDots.TABLETOP -> true
      TouchDots.AUTO_DETECT ->
        orientator.detectedLayout
          .map { it == TouchDots.TABLETOP }
          .orElseGet {
            service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
          }
      else -> false
    }

  private val orientatorCallback =
    object : LayoutOrientator.LayoutOrientatorCallback {
      override fun useSensorsToDetectLayout(): Boolean =
        BrailleUserPreferences.readLayoutMode(service) == TouchDots.AUTO_DETECT

      override fun onDetectionChanged(isTabletop: Boolean, firstChangedEvent: Boolean) {
        inputView?.setTabletopMode(isTabletop)
      }

      override fun uprightFacesUser(): Boolean {
        val facesAway = BrailleUserPreferences.readTabletHeldUpFacesAway(service)
        return !facesAway &&
          DotsOrientation.heldFacingUser(
            HeldOrientationTracker.getLastHeld(),
            BrailleUtils.isPhoneSizedDevice(service.resources),
            facesAway,
          )
      }
    }

  private val inputCallback =
    object : BrailleInputView.Callback {
      override fun wasHeldUprightInScreenAway(): Boolean =
        orientatorCallback.useSensorsToDetectLayout().not() ||
          orientator?.wasHeldUprightInScreenAway() != false

      override fun onBrailleProduced(brailleCharacter: BrailleCharacter): String {
        restartIdleTimer()
        search.whenLoaded { addCell(brailleCharacter) }
        return ""
      }

      override fun onSwipeProduced(swipe: Swipe): Boolean {
        restartIdleTimer()
        val action: () -> Unit =
          when (swipe.touchCount to swipe.direction) {
            1 to Swipe.Direction.UP -> { -> search.nextApp(forward = false) }
            1 to Swipe.Direction.DOWN -> { -> search.nextApp() }
            1 to Swipe.Direction.LEFT -> ::deleteCell
            2 to Swipe.Direction.UP -> search::open
            2 to Swipe.Direction.DOWN -> ::cancel
            2 to Swipe.Direction.LEFT -> ::clear
            else -> return false
          }
        // Cells typed before the apps loaded go first.
        search.whenLoaded(action)
        return true
      }

      override fun onDotHoldAndDotSwipe(dotHoldSwipe: DotHoldSwipe): Boolean = false

      override fun onHoldProduced(pointersHeldCount: Int): Boolean = false
    }

  /** The print text of the cells typed, read as uncontracted braille. */
  private fun print(word: BrailleWord): String =
    if (word.isEmpty) "" else translator?.translateToPrintPartial(word).orEmpty()

  /**
   * Adds [cell] to the cells and says the first app whose name starts with them. A cell that adds no
   * letter, such as a capital or number sign, is kept and said; one that leaves no app is dropped.
   */
  private fun addCell(cell: BrailleCharacter) {
    if (!isShowing || cells.size() >= MAX_CELLS) return
    val before = print(cells)
    cells.append(cell)
    val after = print(cells)
    BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.CHARACTER)
    val added = if (after.startsWith(before)) after.substring(before.length) else after
    val echo = added.ifBlank { BrailleTranslateUtils.getDotsText(service.resources, cell) }
    if (AppNameSearch.normalize(after) == AppNameSearch.normalize(before)) {
      speak(echo)
      return
    }
    if (!search.find(listOf(after), echo)) cells.remove(cells.size() - 1)
  }

  private fun deleteCell() {
    if (cells.isEmpty) {
      speak(service.getString(R.string.handwriting_empty))
      return
    }
    val before = print(cells)
    val cell = cells.remove(cells.size() - 1)
    val after = print(cells)
    BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.DELETE)
    val deleted =
      (if (before.startsWith(after)) before.substring(after.length) else before).ifBlank {
        BrailleTranslateUtils.getDotsText(service.resources, cell)
      }
    search.shorten(after)
    speak(service.getString(R.string.template_text_removed, deleted))
  }

  private fun clear() {
    if (cells.isEmpty) {
      speak(service.getString(R.string.handwriting_empty))
      return
    }
    val text = print(cells)
    cells.clear()
    search.shorten("")
    BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.DELETE)
    speak(
      if (text.isBlank()) service.getString(R.string.handwriting_empty)
      else service.getString(R.string.template_text_removed, text)
    )
  }

  /**
   * Tells the held orientation tracker whether this command is assigned to a gesture, key, menu or
   * braille display key, so the dots are laid out for how the phone was held before they opened.
   */
  private fun updateAssigned() {
    val value = service.getString(R.string.shortcut_value_braille_app_name)
    HeldOrientationTracker.setLauncherAssigned(
      CommandAssignments.isAssigned(prefs.all, value) ||
        BrailleUserPreferences.readDisplayKeyOverrides(service)
          .keysFor(DisplayKeyOverrides.screenReaderTarget(value))
          .isNotEmpty()
    )
  }

  private fun restartIdleTimer() {
    handler.removeCallbacks(closeIdle)
    handler.postDelayed(closeIdle, IDLE_CLOSE_MS)
  }

  private fun speak(text: String) {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(text, AppNameSearch.speakOptions(SpeechController.QUEUE_MODE_INTERRUPT)),
    )
  }

  private companion object {
    const val TAG = "BrailleAppLauncher"
    /** The surface closes after this long without a touch. */
    const val IDLE_CLOSE_MS = 15_000L
    const val MAX_CELLS = 100
  }
}
