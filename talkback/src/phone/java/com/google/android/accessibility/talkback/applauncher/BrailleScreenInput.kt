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
import android.os.PowerManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.google.android.accessibility.braille.common.BrailleImeAction
import com.google.android.accessibility.braille.common.BrailleUserPreferences
import com.google.android.accessibility.braille.common.BrailleUtils
import com.google.android.accessibility.braille.common.DisplayKeyOverrides
import com.google.android.accessibility.braille.common.TouchDots
import com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils
import com.google.android.accessibility.braille.interfaces.BrailleCharacter
import com.google.android.accessibility.braille.interfaces.BrailleWord
import com.google.android.accessibility.braille.translate.BrailleTranslator
import com.google.android.accessibility.brailleime.BrailleImeGestureAction
import com.google.android.accessibility.brailleime.BrailleImeTypingSounds
import com.google.android.accessibility.brailleime.BrailleInputOptions
import com.google.android.accessibility.brailleime.HeldOrientationTracker
import com.google.android.accessibility.brailleime.LayoutOrientator
import com.google.android.accessibility.brailleime.OrientationMonitor
import com.google.android.accessibility.brailleime.Utils
import com.google.android.accessibility.brailleime.input.BrailleInputView
import com.google.android.accessibility.brailleime.input.DotHoldSwipe
import com.google.android.accessibility.brailleime.input.DotsOrientation
import com.google.android.accessibility.brailleime.input.Gesture
import com.google.android.accessibility.brailleime.input.Swipe
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.braillecommands.BrailleCommands
import com.google.android.accessibility.talkback.commands.CommandAssignments
import com.google.android.accessibility.talkback.gesture.AppGestures
import com.google.android.accessibility.talkback.handwriting.HandwritingOverlay
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Braille typed on the whole screen, as on the braille keyboard, in one of these modes:
 * - Braille commands: each cell runs the command that Braille commands gives it.
 * - App search: the cells are read as uncontracted braille of the braille keyboard's current
 *   language, and SandFly says the first app whose name, or a word of it, starts with them. Swipes
 *   work as on the braille keyboard: up and down with one finger for the previous and next app,
 *   left to delete, up with two fingers to open the app.
 * - Find: the cells are read as uncontracted braille, as in app search, and form the text that Find
 *   text, Find next text and Find previous text look for on the screen. Right with one finger
 *   types a space, left deletes, up or right with two fingers finds.
 * - Recording one cell for the Braille commands screen.
 *
 * The braille keyboard's Next braille mode gesture goes from braille commands to app search, then
 * to typing on the braille keyboard when a text field is being edited. Down with two fingers
 * closes.
 */
class BrailleScreenInput(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  actorState: ActorState,
  private val host: BrailleScreenInputHost,
) {
  private enum class Mode {
    COMMANDS,
    APPS,
    FIND,
    RECORD,
  }

  private val handler = Handler(Looper.getMainLooper())
  private val title = service.getString(R.string.title_braille_commands)
  private val overlay = HandwritingOverlay(service, pipeline, actorState, title)
  private val search = AppNameSearch(service, pipeline, R.string.braille_app_prompt, ::dismiss)
  private val closeIdle = Runnable { if (mode == Mode.FIND) cancelFind() else cancel() }
  private var orientator: LayoutOrientator? = null
  private var inputView: BrailleInputView? = null
  private var translator: BrailleTranslator? = null
  private var orientation = Configuration.ORIENTATION_UNDEFINED
  private var screenSize: Size? = null
  /** Whether this turned the orientation monitor on, which the dots read when touched. */
  private var enabledOrientationMonitor = false

  private var mode = Mode.COMMANDS
  /** Gets the cell recorded, or null when recording ends without one. */
  private var onRecorded: ((String?) -> Unit)? = null
  /** The app in front when the surface showed, whose braille commands apply. */
  private var appShown: String? = null

  /**
   * Whether braille commands show again when the braille keyboard closes: the surface closed for the
   * keyboard, and wasn't shown over it.
   */
  private var returnAfterKeyboard = false
  /** Whether the braille keyboard was shown when the surface showed. */
  private var shownOverKeyboard = false
  private val showAfterKeyboard = Runnable {
    if (!returnAfterKeyboard || isShowing) return@Runnable
    returnAfterKeyboard = false
    // Not over the lock screen, or while the screen is off.
    val power = service.getSystemService(PowerManager::class.java)
    val keyguard = service.getSystemService(KeyguardManager::class.java)
    if (power?.isInteractive == false || keyguard?.isKeyguardLocked == true) return@Runnable
    showCommands()
  }
  private val reclaimPassthrough = Runnable { overlay.claimPassthrough() }

  /** The text found last since the surface showed, which Find next and previous text look for. */
  private var findText: String? = null
  /** Which way the text being typed is looked for, and whether past the bottom from the top. */
  private var findForward = true
  private var findWrap = false
  /** Whether the surface showed only for the text to find, and closes once it is found. */
  private var closeAfterFind = false
  /** The words of the text to find typed before the one in [cells]. */
  private val findWords = mutableListOf<BrailleWord>()

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

  /** Closes the surface when the screen turns off, so that it doesn't cover the lock screen. */
  private val screenOffReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (isShowing) cancel()
      }
    }
  private var screenOffRegistered = false

  /** SandFly's windows shown before the last command ran, which don't count as covering. */
  private var overlaysBefore: Set<Int> = emptySet()

  /**
   * Closes braille commands when a command showed a SandFly window over them, such as a menu, or
   * leaves them to the braille keyboard when the command opened it.
   */
  private val coveredCheck = Runnable {
    if (!isShowing || mode != Mode.COMMANDS || (otherOverlays() - overlaysBefore).isEmpty()) {
      return@Runnable
    }
    if (host.isBrailleKeyboardShown()) {
      onBrailleKeyboardShown()
    } else {
      dismiss()
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down))
    }
  }

  /** The cells typed so far in app search. */
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

  /** Shows braille commands, or changes to them. */
  fun showCommands() {
    show(Mode.COMMANDS)
  }

  /** Shows app search, or changes to it. */
  fun showApps() {
    show(Mode.APPS)
  }

  /**
   * Shows the surface to record one cell, and gives its dot numbers to [onCell], or null when the
   * surface closes first. Returns false when the surface can't show.
   */
  fun recordCell(onCell: (String?) -> Unit): Boolean {
    if (isShowing) dismiss()
    if (!show(Mode.RECORD)) return false
    onRecorded = onCell
    return true
  }

  /**
   * Focuses the next item on the screen that contains the text to find, or the previous one when
   * [forward] is false; with [wrap], from the top when none is after the focused item. With
   * [newText], or before any text was found since the surface showed, asks for the text first.
   */
  fun find(forward: Boolean, wrap: Boolean, newText: Boolean) {
    val text = findText
    if (!newText && text != null && isShowing && mode != Mode.RECORD) {
      if (mode != Mode.COMMANDS) enter(Mode.COMMANDS, quiet = true)
      search(text, forward, wrap)
      return
    }
    if (isShowing && mode == Mode.RECORD) return
    findForward = forward
    findWrap = wrap
    // Asked again while typing the text, the surface still closes after the search if it would have.
    if (!isShowing) closeAfterFind = true else if (mode != Mode.FIND) closeAfterFind = false
    if (!show(Mode.FIND)) closeAfterFind = false
  }

  /**
   * Called when the braille keyboard shows. A keyboard that opens over the surface, such as for a
   * text field activated with a braille command, takes the touches of the whole screen, so the
   * surface closes and shows again when the keyboard closes.
   */
  fun onBrailleKeyboardShown() {
    handler.removeCallbacks(showAfterKeyboard)
    handler.removeCallbacks(reclaimPassthrough)
    if (!isShowing || mode == Mode.RECORD || shownOverKeyboard) return
    dismiss(releasePassthrough = false)
    returnAfterKeyboard = true
  }

  /**
   * Called when the braille keyboard closes: braille commands show again if they closed for it, and
   * a surface shown over the keyboard takes back the touches the keyboard let go of.
   */
  fun onBrailleKeyboardHidden() {
    shownOverKeyboard = false
    if (isShowing) {
      // After the keyboard has let go of the region.
      handler.post(reclaimPassthrough)
    } else if (returnAfterKeyboard) {
      // A keyboard that closes and opens again, as when focus moves between text fields, leaves the
      // surface closed.
      handler.removeCallbacks(showAfterKeyboard)
      handler.postDelayed(showAfterKeyboard, RETURN_AFTER_KEYBOARD_MS)
    }
  }

  /**
   * Leaves typing to the braille keyboard while the surface shows, as Switch to braille keyboard
   * does. Returns false, and says so, when no text field is being edited.
   */
  fun switchToBrailleKeyboard(): Boolean {
    if (!typeOnBrailleKeyboard()) {
      speak(service.getString(R.string.braille_mode_no_text_field))
      return false
    }
    return true
  }

  /**
   * Closes the surface and leaves typing to the braille keyboard, or returns false when no text
   * field is being edited.
   */
  private fun typeOnBrailleKeyboard(): Boolean {
    if (!host.canType()) return false
    val shown = host.isBrailleKeyboardShown()
    val returnToCommands = !shownOverKeyboard
    // Closed first: a keyboard that opens takes touches of its own, which closing would undo.
    dismiss(releasePassthrough = !shown)
    returnAfterKeyboard = returnToCommands
    if (shown) {
      speak(service.getString(R.string.title_pref_brailleime))
    } else {
      // The keyboard says its name when it opens.
      if (!host.switchToBrailleKeyboard()) returnAfterKeyboard = false
    }
    return true
  }

  /** Shows the surface in [newMode], or changes to it, or says why it can't show. */
  private fun show(newMode: Mode): Boolean {
    if (isShowing) {
      if (mode != Mode.RECORD) enter(newMode)
      return true
    }
    // Touches reach the surface only where they can pass touch exploration by, from Android 11.
    if (!FeatureSupport.supportPassthrough()) {
      speak(service.getString(R.string.braille_app_not_available))
      return false
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
      return false
    }
    // Taken before the surface shows, which may count as a window of SandFly's.
    appShown = AppGestures.appForGestures().takeIf { it.isNotEmpty() }
    shownOverKeyboard = host.isBrailleKeyboardShown()
    returnAfterKeyboard = false
    handler.removeCallbacks(showAfterKeyboard)
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
      appShown = null
      return false
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
    ContextCompat.registerReceiver(
      service,
      screenOffReceiver,
      IntentFilter(Intent.ACTION_SCREEN_OFF),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    screenOffRegistered = true
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_up))
    enter(newMode)
    return true
  }

  /** Changes to [newMode] and says it, unless [quiet]. */
  private fun enter(newMode: Mode, quiet: Boolean = false) {
    val previous = mode
    mode = newMode
    cells.clear()
    findWords.clear()
    handler.removeCallbacks(closeIdle)
    handler.removeCallbacks(coveredCheck)
    if (previous == Mode.APPS && newMode != Mode.APPS) search.stop()
    when (newMode) {
      Mode.APPS -> {
        search.start()
        speak(service.getString(R.string.braille_app_prompt))
        restartIdleTimer()
      }
      Mode.COMMANDS -> {
        if (quiet) return
        val profile = BrailleCommands.activeName(prefs)
        speak(
          if (profile == null) title
          else service.getString(R.string.braille_commands_with_profile, profile)
        )
      }
      Mode.FIND -> {
        speak(service.getString(R.string.braille_find_prompt))
        restartIdleTimer()
      }
      Mode.RECORD -> {
        speak(service.getString(R.string.braille_command_record_prompt))
        restartIdleTimer()
      }
    }
  }

  /** Changes to the next mode: app search after braille commands, then the braille keyboard. */
  private fun nextMode() {
    when (mode) {
      Mode.COMMANDS -> enter(Mode.APPS)
      Mode.APPS -> if (!typeOnBrailleKeyboard()) enter(Mode.COMMANDS)
      Mode.FIND -> cancelFind()
      Mode.RECORD -> {}
    }
  }

  /** Closes the surface without opening an app or running a command. */
  private fun cancel() {
    if (!isShowing) return
    val closed = mode
    dismiss()
    val text =
      when (closed) {
        Mode.APPS -> service.getString(R.string.braille_app_closed)
        Mode.COMMANDS -> service.getString(R.string.braille_commands_closed)
        Mode.FIND,
        Mode.RECORD -> null
      }
    if (text != null) speak(text)
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down))
  }

  /**
   * Closes the surface without any feedback, such as when SandFly stops. With [releasePassthrough]
   * false, the region touches pass through is left to the braille keyboard that holds it.
   */
  private fun dismiss(releasePassthrough: Boolean = true) {
    handler.removeCallbacks(closeIdle)
    handler.removeCallbacks(coveredCheck)
    handler.removeCallbacks(reclaimPassthrough)
    returnAfterKeyboard = false
    orientator?.stop()
    orientator = null
    if (enabledOrientationMonitor) {
      enabledOrientationMonitor = false
      // The braille keyboard registers a callback while it is shown, and keeps the monitor on.
      OrientationMonitor.getInstance().let { if (!it.hasCallback()) it.disable() }
    }
    if (inputView != null) service.unregisterComponentCallbacks(configurationCallbacks)
    if (screenOffRegistered) {
      screenOffRegistered = false
      service.unregisterReceiver(screenOffReceiver)
    }
    inputView = null
    translator = null
    appShown = null
    findText = null
    closeAfterFind = false
    findWords.clear()
    cells.clear()
    search.stop()
    overlay.remove(releasePassthrough)
    mode = Mode.COMMANDS
    onRecorded?.let {
      onRecorded = null
      it(null)
    }
  }

  fun shutdown() {
    dismiss()
    handler.removeCallbacks(showAfterKeyboard)
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

  /** Whether [gesture] is the braille keyboard's Next braille mode gesture. */
  private fun isNextModeGesture(gesture: Gesture): Boolean =
    BrailleImeGestureAction.getAction(service, gesture).contains(BrailleImeAction.NEXT_BRAILLE_MODE)

  private val inputCallback =
    object : BrailleInputView.Callback {
      override fun wasHeldUprightInScreenAway(): Boolean =
        orientatorCallback.useSensorsToDetectLayout().not() ||
          orientator?.wasHeldUprightInScreenAway() != false

      override fun onBrailleProduced(brailleCharacter: BrailleCharacter): String {
        when (mode) {
          Mode.APPS -> {
            restartIdleTimer()
            search.whenLoaded { addCell(brailleCharacter) }
          }
          Mode.COMMANDS -> runCell(brailleCharacter)
          Mode.FIND -> {
            restartIdleTimer()
            addFindCell(brailleCharacter)
          }
          Mode.RECORD -> {
            val record = onRecorded
            onRecorded = null
            BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.CHARACTER)
            dismiss()
            record?.invoke(brailleCharacter.toString())
          }
        }
        return ""
      }

      override fun onSwipeProduced(swipe: Swipe): Boolean {
        if (mode == Mode.FIND) return onFindSwipe(swipe)
        if (swipe.touchCount == 2 && swipe.direction == Swipe.Direction.DOWN) {
          cancel()
          return true
        }
        if (mode != Mode.RECORD && isNextModeGesture(swipe)) {
          nextMode()
          return true
        }
        if (mode != Mode.APPS) return false
        restartIdleTimer()
        val action: () -> Unit =
          when (swipe.touchCount to swipe.direction) {
            1 to Swipe.Direction.UP -> { -> search.nextApp(forward = false) }
            1 to Swipe.Direction.DOWN -> { -> search.nextApp() }
            1 to Swipe.Direction.LEFT -> ::deleteCell
            2 to Swipe.Direction.UP -> search::open
            3 to Swipe.Direction.UP -> search::openInSplit
            2 to Swipe.Direction.LEFT -> ::clear
            else -> return false
          }
        // Cells typed before the apps loaded go first.
        search.whenLoaded(action)
        return true
      }

      override fun onDotHoldAndDotSwipe(dotHoldSwipe: DotHoldSwipe): Boolean {
        // Find goes back to braille commands, as Next braille mode goes on from them.
        if (mode == Mode.RECORD || !isNextModeGesture(dotHoldSwipe)) return false
        nextMode()
        return true
      }

      override fun onHoldProduced(pointersHeldCount: Int): Boolean = false
    }

  /** Types, deletes or finds the text to find, or leaves Find. */
  private fun onFindSwipe(swipe: Swipe): Boolean {
    restartIdleTimer()
    when (swipe.touchCount to swipe.direction) {
      2 to Swipe.Direction.DOWN -> cancelFind()
      1 to Swipe.Direction.RIGHT -> addFindSpace()
      1 to Swipe.Direction.LEFT -> deleteFindCell()
      2 to Swipe.Direction.LEFT -> clearFind()
      2 to Swipe.Direction.UP,
      2 to Swipe.Direction.RIGHT -> confirmFind()
      else -> return isNextModeGesture(swipe).also { if (it) cancelFind() }
    }
    return true
  }

  /** The text to find typed so far. */
  private fun typedFindText(): String =
    (findWords + cells).map { print(it) }.filter { it.isNotBlank() }.joinToString(" ")

  private fun findCellCount(): Int = findWords.sumOf { it.size() } + cells.size()

  private fun addFindCell(cell: BrailleCharacter) {
    if (findCellCount() >= MAX_CELLS) return
    val before = print(cells)
    cells.append(cell)
    val after = print(cells)
    BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.CHARACTER)
    val added = if (after.startsWith(before)) after.substring(before.length) else after
    speak(added.ifBlank { BrailleTranslateUtils.getDotsText(service.resources, cell) })
  }

  private fun addFindSpace() {
    if (cells.isEmpty || findCellCount() >= MAX_CELLS) return
    findWords.add(BrailleWord(cells))
    cells.clear()
    BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.SPACE)
    speak(service.getString(R.string.handwriting_space))
  }

  /** Deletes the last cell, or the space before the word being typed when it has none. */
  private fun deleteFindCell() {
    if (cells.isEmpty) {
      if (findWords.isEmpty()) {
        speak(service.getString(R.string.handwriting_empty))
        return
      }
      cells.append(findWords.removeAt(findWords.size - 1))
      BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.DELETE)
      speak(
        service.getString(
          R.string.template_text_removed,
          service.getString(R.string.handwriting_space),
        )
      )
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
    speak(service.getString(R.string.template_text_removed, deleted))
  }

  private fun clearFind() {
    val text = typedFindText()
    findWords.clear()
    cells.clear()
    if (text.isBlank()) {
      speak(service.getString(R.string.handwriting_empty))
      return
    }
    BrailleImeTypingSounds.play(service, BrailleImeTypingSounds.Key.DELETE)
    speak(service.getString(R.string.template_text_removed, text))
  }

  /** Looks for the text typed, or asks for it again when none is typed. */
  private fun confirmFind() {
    val text = typedFindText()
    if (text.isBlank()) {
      speak(service.getString(R.string.braille_find_prompt))
      return
    }
    val forward = findForward
    val wrap = findWrap
    leaveFind()
    findText = text
    search(text, forward, wrap)
  }

  /** Leaves Find without looking for anything. */
  private fun cancelFind() {
    if (closeAfterFind) {
      dismiss()
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down))
    } else {
      enter(Mode.COMMANDS)
    }
  }

  /** Goes back to braille commands, or closes the surface when it showed only for Find. */
  private fun leaveFind() {
    if (closeAfterFind) dismiss() else enter(Mode.COMMANDS, quiet = true)
  }

  /** Focuses the next or previous item that contains [text], or says that none does. */
  private fun search(text: String, forward: Boolean, wrap: Boolean) {
    val found =
      host.findText(text, forward, wrap) {
        // Said before the item found, which the focus change says next.
        pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(
            service.getString(R.string.braille_find_wrapped),
            AppNameSearch.speakOptions(
              SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH
            ),
          ),
        )
      }
    if (!found) speak(service.getString(R.string.granular_editing_not_found))
  }

  /** Runs the command that braille commands give [cell], or says that it has none. */
  private fun runCell(cell: BrailleCharacter) {
    val app = AppGestures.appForGestures().takeIf { it.isNotEmpty() && it != service.packageName }
    val value = BrailleCommands.action(service, prefs, cell.toString(), app ?: appShown)
    if (value == null || value == service.getString(R.string.shortcut_value_unassigned)) {
      speak(
        service.getString(
          R.string.braille_command_none,
          BrailleTranslateUtils.getDotsText(service.resources, cell),
        )
      )
      return
    }
    overlaysBefore = otherOverlays()
    host.runCommand(value)
    // A command that shows a menu or dialog over the surface leaves it to touch.
    if (isShowing && mode == Mode.COMMANDS) {
      handler.removeCallbacks(coveredCheck)
      handler.postDelayed(coveredCheck, COVERED_CHECK_MS)
    }
  }

  /**
   * The IDs of SandFly's windows other than this surface with something in them, such as the
   * SandFly menu or the braille keyboard.
   */
  private fun otherOverlays(): Set<Int> =
    service.windows
      .filter { window ->
        window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
          window.title?.toString() != title &&
          window.root?.let { it.childCount > 0 && it.packageName == service.packageName } == true
      }
      .map { it.id }
      .toSet()

  /** The print text of the cells typed, read as uncontracted braille. */
  private fun print(word: BrailleWord): String =
    if (word.isEmpty) "" else translator?.translateToPrintPartial(word).orEmpty()

  /**
   * Adds [cell] to the cells and says the first app whose name starts with them. A cell that adds no
   * letter, such as a capital or number sign, is kept and said; one that leaves no app is dropped.
   */
  private fun addCell(cell: BrailleCharacter) {
    if (!isShowing || mode != Mode.APPS || cells.size() >= MAX_CELLS) return
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
   * Tells the held orientation tracker whether a command that shows this surface is assigned to a
   * gesture, key, menu or braille display key, so the dots are laid out for how the phone was held
   * before they opened.
   */
  private fun updateAssigned() {
    val overrides = BrailleUserPreferences.readDisplayKeyOverrides(service)
    HeldOrientationTracker.setLauncherAssigned(
      listOf(R.string.shortcut_value_braille_app_name, R.string.shortcut_value_braille_commands)
        .map { service.getString(it) }
        .any { value ->
          CommandAssignments.isAssigned(prefs.all, value) ||
            overrides.keysFor(DisplayKeyOverrides.screenReaderTarget(value)).isNotEmpty()
        }
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
    const val TAG = "BrailleScreenInput"
    /** App search and recording close after this long without a touch. */
    const val IDLE_CLOSE_MS = 15_000L
    /** How long braille commands wait to show after the braille keyboard closes. */
    const val RETURN_AFTER_KEYBOARD_MS = 300L
    /** How long after a command the surface looks for a window the command showed over it. */
    const val COVERED_CHECK_MS = 700L
    const val MAX_CELLS = 100
  }
}
