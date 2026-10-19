/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.directtouch

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Rect
import android.graphics.Region
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.DIRECT_TOUCH_REGION
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.LiftToActivateMode
import com.google.android.accessibility.talkback.monitor.RingerModeAndScreenMonitor
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils.WINDOW_ID_NONE
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.Performance
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.input.WindowEventInterpreter
import com.google.android.accessibility.utils.input.WindowEventInterpreter.EventInterpretation
import com.google.android.accessibility.utils.monitor.DisplayMonitor
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_FORCE_FEEDBACK_ALL
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_NO_HISTORY
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_INTERRUPT
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_QUEUE
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH_CAN_IGNORE_INTERRUPTS
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.accessibility.utils.output.ThemeVibrations

/**
 * Gives games direct touch. While an app the user turned on is in front, touches go straight to it.
 * Touch goes back to Backtalk as soon as a dialog, another app, the notification shade or a text
 * field needs the screen reader, or the screen turns off. The keyboard stays with Backtalk without
 * ending direct touch, because its window is cut out of the passthrough region unless direct typing
 * is on for the app. A keyboard that is itself in the list, such as a self-voicing braille keyboard,
 * takes touches directly in every app, whether or not direct touch is on.
 *
 * Everything comes from Backtalk's own window, screen and display monitors, so there is no tree
 * scan. Turning direct touch off takes effect at once. Turning it on waits [DEBOUNCE_MS] so that a
 * burst of window changes settles first. All callbacks arrive on the main thread.
 */
class DirectTouchController(
  private val service: AccessibilityService,
  private val prefs: SharedPreferences,
  private val feedback: Pipeline.FeedbackReturner,
  /** Keeps the package of each window's root, which every look at the screen needs. */
  private val windowEventInterpreter: WindowEventInterpreter,
) :
  WindowEventInterpreter.WindowEventHandler,
  RingerModeAndScreenMonitor.ScreenChangedListener,
  DisplayMonitor.DisplayStateChangedListener {

  private val handler = Handler(Looper.getMainLooper())
  private val reevaluate = Runnable { evaluate() }
  private val recheck = Runnable { evaluate() }

  private var mainWindowId = WINDOW_ID_NONE
  private var textFieldFocused = false
  private var screenInteractive = true
  private var displayOn = true
  private var active = false
  // Whether the passthrough region now holds just the navigation bar or a keyboard in the list,
  // while direct touch is off.
  private var partRegionSent = false
  private var paused = false

  // The system holds preference listeners weakly, so this must stay a field.
  private val prefsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key != null && (key.startsWith("pref_direct_touch") || key == liftToActivateKey)) {
        evaluate()
      }
    }

  private val liftToActivateKey = service.getString(R.string.pref_lift_to_activate_key)

  // The last navigation bar button spoken, so that the same tap reported twice is said once.
  private var lastNavBarButton: CharSequence? = null
  private var lastNavBarButtonTime = 0L

  init {
    DirectTouchSettings.migrateNavBarSetting(prefs, liftToActivateKey)
    prefs.registerOnSharedPreferenceChangeListener(prefsListener)
  }

  override fun handle(interpretation: EventInterpretation, eventId: EventId?) {
    if (!FeatureSupport.supportPassthrough()) {
      return
    }
    mainWindowId = interpretation.windowA.id
    if (interpretation.mainWindowsChanged) {
      textFieldFocused = false
    }
    scheduleEvaluate()
  }

  /**
   * Raw events, before Backtalk drops any. Watches for window changes, on or off, and for a text
   * field taking focus.
   */
  fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (!FeatureSupport.supportPassthrough()) {
      return
    }
    if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
      speakNavBarButton(event)
    }
    if (DirectTouchRegions.reassertsRegion(event.eventType)) {
      // Look again once things settle, like the original did after every window change. The active
      // window moves without Backtalk's window interpreter reporting it, for instance back to the
      // game when a finger lifts off the navigation bar, and nothing else would turn direct touch
      // back on. While on, this also sends the region again after another service clears it.
      handler.removeCallbacks(recheck)
      handler.postDelayed(recheck, DEBOUNCE_MS)
    }
    if (!active) {
      return
    }
    when (event.eventType) {
      AccessibilityEvent.TYPE_VIEW_FOCUSED,
      AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.isEditable == true
        if (focused != textFieldFocused) {
          textFieldFocused = focused
          scheduleEvaluate()
        }
      }
    }
  }

  override fun onScreenChanged(isInteractive: Boolean, eventId: EventId?) {
    screenInteractive = isInteractive
    onScreenStateChanged()
  }

  override fun onDisplayStateChanged(displayOn: Boolean) {
    this.displayOn = displayOn
    onScreenStateChanged()
  }

  /**
   * Looks again after the screen turns on or off, without asking apps for their windows' contents
   * while the screen is changing. A watch's screen changes all the time, as the wrist turns, and
   * that work blocked the main thread for hundreds of milliseconds. The app in front is the same as
   * before, so with the screen off, direct touch only has to end, and with it on, there is nothing
   * to do unless direct touch can turn on, and then it waits until the screen has settled.
   */
  private fun onScreenStateChanged() {
    if (!screenInteractive || !displayOn) {
      handler.removeCallbacks(reevaluate)
      if (active) {
        active = false
        clearRegion()
        partRegionSent = false
        announce(false)
      }
      return
    }
    if (isNavBarDirect() || DirectTouchSettings.hasEnabledApps(prefs)) {
      scheduleEvaluate()
    }
  }

  /**
   * Stops sending the region while Backtalk is paused, when explore by touch is off and the region
   * does nothing. On resume, sends it again for the current screen.
   */
  fun setPaused(paused: Boolean) {
    this.paused = paused
    handler.removeCallbacks(reevaluate)
    handler.removeCallbacks(recheck)
    if (!paused) {
      evaluate()
    }
  }

  fun shutdown() {
    prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    handler.removeCallbacks(reevaluate)
    handler.removeCallbacks(recheck)
    if (active || partRegionSent) {
      active = false
      partRegionSent = false
      clearRegion()
    }
  }

  private fun scheduleEvaluate() {
    handler.removeCallbacks(reevaluate)
    if (active) {
      // Handing back must not wait, and neither must a region that changed under us.
      evaluate()
    } else {
      handler.postDelayed(reevaluate, DEBOUNCE_MS)
    }
  }

  private fun evaluate() {
    handler.removeCallbacks(reevaluate)
    if (paused || !FeatureSupport.supportPassthrough()) {
      return
    }
    val windows = service.windows.orEmpty()
    val mainPackage = windows.firstOrNull { it.id == mainWindowId }?.let { rootPackage(it) }
    val directTyping = mainPackage != null && DirectTouchSettings.isDirectTyping(prefs, mainPackage)
    val shouldBeActive = mainPackage != null && shouldBeActive(windows, mainPackage)
    val navBarDirect = isNavBarDirect()
    val keyboards = directKeyboards(windows)
    if (shouldBeActive) {
      partRegionSent = false
      applyRegion(windows, directTyping, navBarDirect, keyboards)
    }
    if (shouldBeActive != active) {
      active = shouldBeActive
      if (!active) {
        clearRegion()
        partRegionSent = false
      }
      announce(active)
    }
    if (!active) {
      updatePartRegion(windows, navBarDirect, keyboards)
    }
  }

  /**
   * Whether the navigation bar buttons take touches directly, so that a single tap presses them
   * and holding Home holds it, as with lift to activate on the navigation bar. Lifting to activate
   * a button Backtalk focused does not work on every phone, but touches that go straight to the
   * navigation bar do.
   */
  private fun isNavBarDirect(): Boolean =
    LiftToActivateMode.fromPrefValue(prefs.getString(liftToActivateKey, null)) ==
      LiftToActivateMode.NAVIGATION_BAR

  /**
   * Says the navigation bar button that a tap pressed, such as Back or Home, when the navigation
   * bar takes touches directly. Backtalk never sees those touches, so without this a tap on the
   * bar would act without a word. The app that draws the bar reports the press, sometimes twice.
   */
  private fun speakNavBarButton(event: AccessibilityEvent) {
    if (!isNavBarDirect() || !DirectTouchSettings.isNavBarSpeechEnabled(prefs)) {
      return
    }
    val label =
      event.contentDescription?.takeIf { it.isNotEmpty() }
        ?: event.text.joinToString(" ").takeIf { it.isNotEmpty() }
        ?: return
    val window = service.windows.orEmpty().firstOrNull { it.id == event.windowId } ?: return
    if (window.type != AccessibilityWindowInfo.TYPE_SYSTEM) {
      return
    }
    val bounds = Rect()
    window.getBoundsInScreen(bounds)
    val display = displayBounds()
    if (
      !DirectTouchRegions.isNavigationBar(
        bounds.left,
        bounds.top,
        bounds.right,
        bounds.bottom,
        display.width(),
        display.height(),
      )
    ) {
      return
    }
    val now = SystemClock.uptimeMillis()
    if (label.toString() == lastNavBarButton?.toString() && now - lastNavBarButtonTime < REPEAT_MS) {
      return
    }
    lastNavBarButton = label
    lastNavBarButtonTime = now
    // The press opens another screen at once, and that screen's announcement would cut the button
    // off, so the button can't be interrupted, and the announcement follows it. It is also said
    // over media, as a press always is. No named queue mode both interrupts and ignores interrupts.
    @SuppressLint("WrongConstant")
    val options =
      SpeakOptions.create()
        .setQueueMode(
          QUEUE_MODE_INTERRUPT or QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH_CAN_IGNORE_INTERRUPTS
        )
        .setFlags(FLAG_FORCE_FEEDBACK_ALL)
    feedback.returnFeedback(Performance.EVENT_ID_UNTRACKED, Feedback.speech(label, options))
  }

  /**
   * While direct touch is off, keeps the navigation bar in the passthrough region if lift to
   * activate is on for it, and a keyboard in the direct touch list while it shows, and takes them
   * out again once they aren't. The region is sent again on every look, because other services can
   * clear the shared one.
   */
  private fun updatePartRegion(
    windows: List<AccessibilityWindowInfo>,
    navBarDirect: Boolean,
    keyboards: List<AccessibilityWindowInfo>,
  ) {
    val parts = (if (navBarDirect) navigationBarBounds(windows) else emptyList()) + keyboards.map(::boundsOf)
    if (parts.isNotEmpty()) {
      val region = Region()
      parts.forEach { region.union(it) }
      sendRegion(region)
      partRegionSent = true
    } else if (partRegionSent) {
      clearRegion()
      partRegionSent = false
    }
  }

  /**
   * The keyboards on screen that are in the direct touch list, such as a self-voicing braille
   * keyboard. They take touches directly in every app, since a keyboard is never the app in front.
   * Their apps are asked for only while some app is in the list.
   */
  private fun directKeyboards(windows: List<AccessibilityWindowInfo>): List<AccessibilityWindowInfo> {
    if (!DirectTouchSettings.hasEnabledApps(prefs)) {
      return emptyList()
    }
    return windows.filter { window ->
      window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
        rootPackage(window)?.let { DirectTouchSettings.isAppEnabled(prefs, it) } == true
    }
  }

  private fun boundsOf(window: AccessibilityWindowInfo): Rect =
    Rect().also { window.getBoundsInScreen(it) }

  /**
   * The navigation bars on screen, by shape: a system window that is a thin strip along an edge.
   * They are told apart by shape rather than by app, because not every phone draws them in System
   * UI. From Android 17, Pixel phones draw the navigation bar in the Pixel Launcher.
   */
  private fun navigationBarBounds(windows: List<AccessibilityWindowInfo>): List<Rect> {
    val display = displayBounds()
    return windows.mapNotNull { window ->
      if (window.type != AccessibilityWindowInfo.TYPE_SYSTEM) {
        return@mapNotNull null
      }
      val bounds = Rect()
      window.getBoundsInScreen(bounds)
      bounds.takeIf {
        DirectTouchRegions.isNavigationBar(
          it.left,
          it.top,
          it.right,
          it.bottom,
          display.width(),
          display.height(),
        )
      }
    }
  }

  /** The package of the window's root, which the window's app is asked for once per window. */
  private fun rootPackage(window: AccessibilityWindowInfo): String? =
    windowEventInterpreter.getRootPackageName(window)?.toString()

  private fun isSystemUiWindow(window: AccessibilityWindowInfo): Boolean =
    window.type == AccessibilityWindowInfo.TYPE_SYSTEM && rootPackage(window) == SYSTEM_UI

  private fun shouldBeActive(
    windows: List<AccessibilityWindowInfo>,
    mainPackage: String,
  ): Boolean {
    if (mainPackage in IGNORED_PACKAGES) {
      return false
    }
    // Only the first sight of an app uses its manifest, which the package manager has to read.
    if (!DirectTouchSettings.isSeen(prefs, mainPackage)) {
      val capable = DirectTouchCapability.declaresDirectTouch(service.packageManager, mainPackage)
      DirectTouchSettings.onFirstSight(prefs, mainPackage, capable)
    }
    // A dialog is a window of its own, so any other application window is either a dialog from the
    // same app or another app.
    val otherApplicationWindows =
      windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.id != mainWindowId }
    val input =
      DirectTouchInput(
        masterEnabled = DirectTouchSettings.isMasterEnabled(prefs),
        appEnabled = DirectTouchSettings.isAppEnabled(prefs, mainPackage),
        screenInteractive = screenInteractive && displayOn,
        dialogShowing = otherApplicationWindows.isNotEmpty(),
        textFieldFocused = textFieldFocused,
        foreignAppWindow = { otherApplicationWindows.any { rootPackage(it) != mainPackage } },
        systemUiCoversHalf = { systemUiCoversHalf(windows) },
      )
    return DirectTouchPolicy.shouldBeActive(input)
  }

  private fun systemUiCoversHalf(windows: List<AccessibilityWindowInfo>): Boolean {
    val displayHeight = displayBounds().height()
    val bounds = Rect()
    return windows.any { window ->
      window.getBoundsInScreen(bounds)
      // Telling System UI apart asks the window's app for its root, so the shape comes first.
      DirectTouchPolicy.coversHalfScreen(window.isActive, bounds.height(), displayHeight) &&
        isSystemUiWindow(window)
    }
  }

  private fun applyRegion(
    windows: List<AccessibilityWindowInfo>,
    directTyping: Boolean,
    navBarDirect: Boolean,
    keyboards: List<AccessibilityWindowInfo>,
  ) {
    val display = displayBounds()
    val excluded = mutableListOf<Rect>()
    windows.forEach { window ->
      if (
        DirectTouchRegions.shouldExcludeWindow(window.type, directTyping) && window !in keyboards
      ) {
        val bounds = Rect()
        window.getBoundsInScreen(bounds)
        val isNavBar =
          window.type == AccessibilityWindowInfo.TYPE_SYSTEM &&
            DirectTouchRegions.isNavigationBar(
              bounds.left,
              bounds.top,
              bounds.right,
              bounds.bottom,
              display.width(),
              display.height(),
            )
        if (!(navBarDirect && isNavBar)) {
          excluded += bounds
        }
      }
    }
    sendRegion(DirectTouchRegions.passthroughRegion(display, excluded))
  }

  private fun clearRegion() = sendRegion(Region())

  private fun sendRegion(region: Region) {
    feedback.returnFeedback(
      Performance.EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(DIRECT_TOUCH_REGION, region),
    )
  }

  private fun displayBounds(): Rect =
    Rect(service.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds)

  private fun announce(on: Boolean) {
    if (DirectTouchSettings.isSpeechEnabled(prefs)) {
      val text = service.getString(if (on) R.string.direct_touch_on else R.string.direct_touch_off)
      // A game is always playing audio, and speech is dropped while audio plays unless it is forced.
      val options =
        SpeakOptions.create()
          .setQueueMode(QUEUE_MODE_QUEUE)
          .setFlags(
            FLAG_NO_HISTORY or FLAG_FORCE_FEEDBACK_ALL
          )
      feedback.returnFeedback(Performance.EVENT_ID_UNTRACKED, Feedback.speech(text, options))
    }
    if (DirectTouchSettings.isHapticsEnabled(prefs)) {
      val vibrator = service.getSystemService(Vibrator::class.java)
      val themeName = if (on) THEME_ON else THEME_OFF
      if (vibrator == null || !ThemeVibrations.play(vibrator, themeName, VIBRATION_ATTRIBUTES)) {
        vibrate(if (on) ON_PATTERN else OFF_PATTERN)
      }
    }
  }

  /**
   * Vibrates directly. The switch in the direct touch settings is an explicit choice, so it does not
   * depend on Backtalk's vibration feedback setting, which drops every Feedback.vibration when off.
   */
  private fun vibrate(pattern: LongArray) {
    val vibrator = service.getSystemService(Vibrator::class.java)
    if (vibrator == null || !vibrator.hasVibrator()) {
      return
    }
    @Suppress("DEPRECATION") // The attributes overload is the one that reaches API 26.
    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), VIBRATION_ATTRIBUTES)
  }

  private companion object {
    const val DEBOUNCE_MS = 150L
    // Longer than the gap between the two reports of one press, shorter than a second tap.
    const val REPEAT_MS = 250L
    const val SYSTEM_UI = "com.android.systemui"
    // The names a sound theme replaces the on and off vibrations by.
    const val THEME_ON = "direct_touch_on"
    const val THEME_OFF = "direct_touch_off"
    val ON_PATTERN = longArrayOf(0, 15, 100, 15)
    val OFF_PATTERN = longArrayOf(0, 40)
    val VIBRATION_ATTRIBUTES: AudioAttributes =
      AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .build()
    val IGNORED_PACKAGES = setOf(SYSTEM_UI, "android", "com.google.android.gms")
  }
}
