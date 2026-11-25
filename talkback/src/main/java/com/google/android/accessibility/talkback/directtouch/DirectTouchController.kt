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
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.Region
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.DIRECT_TOUCH_REGION
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.KEYBOARD_REGION
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
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
 * takes touches directly in every app, whether or not direct touch is on. A keyboard whose app has an
 * accessibility service of its own and draws in an accessibility overlay, such as Advanced Braille
 * Keyboard, takes touches directly over the overlay. While such a keyboard is shown in an input
 * method window alone, the region its service sets stays.
 *
 * An app can also choose where it gets direct touch: in its manifest, by activity and by a state
 * it sets, and while it runs through [DirectTouchService]. See docs/direct-touch-api.md. Areas that
 * follow views are looked up again when the app's content changes.
 *
 * Everything else comes from Backtalk's own window, screen and display monitors, so there is no
 * tree scan. Turning direct touch off takes effect at once. Turning it on waits [DEBOUNCE_MS] so
 * that a burst of window changes settles first. All callbacks arrive on the main thread.
 */
class DirectTouchController
@JvmOverloads
constructor(
  private val service: AccessibilityService,
  private val prefs: SharedPreferences,
  private val feedback: Pipeline.FeedbackReturner,
  /** Keeps the package of each window's root, which every look at the screen needs. */
  private val windowEventInterpreter: WindowEventInterpreter,
  /** Reads what an app's manifest says about direct touch. */
  private val readApp: (String) -> DirectTouchApp = { pkg ->
    DirectTouchCapability.read(service.packageManager, pkg, permissionName(service))
  },
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
  // The accessibility overlays of a keyboard with its own accessibility service, last sent.
  private var keyboardOverlay = Region()
  private var paused = false
  // The value of takesTouchesDirectly() last reported to onTouchesDirectChanged.
  private var reportedTouchesDirect = false
  // False while the direct touch command changes the settings, as the command speaks the result.
  private var speakChanges = true

  // What each app's manifest says, read once per app until it is updated.
  private val apps = HashMap<String, DirectTouchApp>()
  // The activity in front of each app with a declaration that names activities.
  private val activityByPackage = HashMap<String, String>()
  // Whether "package/class" is an activity of the package, as the package manager says.
  private val knownActivities = HashMap<String, Boolean>()
  // The app whose areas follow views, so that its content changes move them.
  private var viewsPackage: String? = null
  private var viewRecheckPosted = false
  private val viewRecheck = Runnable {
    viewRecheckPosted = false
    evaluate()
  }
  private val requestsChanged = Runnable { evaluate() }

  private val packageReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return
        apps.remove(pkg)
        activityByPackage.remove(pkg)
        knownActivities.keys.removeAll { it.startsWith("$pkg/") }
        scheduleEvaluate()
      }
    }

  /** Runs on the main thread when [takesTouchesDirectly] changes. */
  var onTouchesDirectChanged: Runnable? = null

  // The system holds preference listeners weakly, so this must stay a field.
  private val prefsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (
        key != null &&
          (key.startsWith("pref_direct_touch") || key == DirectTouchSettings.PREF_NAV_BAR_TAP)
      ) {
        evaluate()
      }
    }

  // The last navigation bar button spoken, so that the same tap reported twice is said once.
  private var lastNavBarButton: CharSequence? = null
  private var lastNavBarButtonTime = 0L

  init {
    DirectTouchSettings.migrateNavBarSetting(prefs)
    prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    DirectTouchRequests.onChanged = requestsChanged
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addDataScheme("package")
      }
    ContextCompat.registerReceiver(
      service,
      packageReceiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
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
    when (event.eventType) {
      AccessibilityEvent.TYPE_VIEW_CLICKED -> speakNavBarButton(event)
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> noteActivity(event)
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
      AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
        if (viewsPackage != null && event.packageName?.toString() == viewsPackage) {
          scheduleViewRecheck()
        }
      }
    }
    if (DirectTouchRegions.reassertsRegion(event.eventType)) {
      // Look again once things settle, like the original did after every window change. The active
      // window moves without Backtalk's window interpreter reporting it, for instance back to the
      // game when a finger lifts off the navigation bar, and nothing else would turn direct touch
      // back on. While on, this also sends the region again after another service clears it.
      handler.removeCallbacks(recheck)
      handler.postDelayed(recheck, DEBOUNCE_MS)
    }
    // Also while a text field holds direct touch off, to see it lose focus. Then only focus
    // changes count: typing in it moves the cursor with every key, and each look asks the app.
    if (!active && !textFieldFocused) {
      return
    }
    if (textFieldFocused && event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
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
      reportTouchesDirect()
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

  /**
   * Turns direct touch on or off for the app in front, for the Turn direct touch on or off for this
   * app command, and applies it at once. Turning it on also turns on the Direct touch switch.
   * Returns whether direct touch is now on for the app, or null when no app is in front, such as
   * when SandFly or System UI is.
   */
  fun toggleForAppInFront(): Boolean? {
    if (!FeatureSupport.supportPassthrough()) {
      return null
    }
    val pkg = appInFront(service.windows.orEmpty()) ?: return null
    // Marks the app seen without turning it on, so that its manifest can't undo the user's choice.
    DirectTouchSettings.onFirstSight(prefs, pkg, declaresCapability = false)
    val on =
      !(DirectTouchSettings.isMasterEnabled(prefs) && DirectTouchSettings.isAppEnabled(prefs, pkg))
    speakChanges = false
    try {
      if (on) {
        DirectTouchSettings.setMasterEnabled(prefs, true)
      }
      DirectTouchSettings.setAppEnabled(prefs, pkg, on)
      evaluate()
    } finally {
      speakChanges = true
    }
    return on
  }

  /**
   * The package of the app in front, for commands that change a setting for it. Null when no app
   * is in front, such as when SandFly or System UI is.
   */
  fun appInFront(): String? = appInFront(service.windows.orEmpty())

  /**
   * The package of the app in front: the main window's, or else the active application window's.
   * Null for SandFly, System UI and the other packages direct touch never works in.
   */
  private fun appInFront(windows: List<AccessibilityWindowInfo>): String? {
    val window =
      windows.firstOrNull {
        it.id == mainWindowId && it.type == AccessibilityWindowInfo.TYPE_APPLICATION
      }
        ?: windows.firstOrNull {
          it.isActive && it.type == AccessibilityWindowInfo.TYPE_APPLICATION
        }
        ?: return null
    return rootPackage(window)?.takeUnless { it in IGNORED_PACKAGES || it == service.packageName }
  }

  /**
   * Whether a passthrough region is sent, for an app, the navigation bar or a keyboard, so that
   * touches starting in it go to the windows under them without touch exploration.
   */
  fun takesTouchesDirectly(): Boolean = active || partRegionSent

  private fun reportTouchesDirect() {
    val now = takesTouchesDirectly()
    if (now != reportedTouchesDirect) {
      reportedTouchesDirect = now
      onTouchesDirectChanged?.run()
    }
  }

  fun shutdown() {
    onTouchesDirectChanged = null
    prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    if (DirectTouchRequests.onChanged === requestsChanged) {
      DirectTouchRequests.onChanged = null
    }
    try {
      service.unregisterReceiver(packageReceiver)
    } catch (_: IllegalArgumentException) {}
    handler.removeCallbacks(reevaluate)
    handler.removeCallbacks(recheck)
    handler.removeCallbacks(viewRecheck)
    if (active || partRegionSent) {
      active = false
      partRegionSent = false
      clearRegion()
    }
    sendKeyboardOverlay(Region())
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
    // Leaving a text field doesn't always send a focus event, such as when the keyboard closes.
    if (textFieldFocused) {
      textFieldFocused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.isEditable == true
    }
    val windows = service.windows.orEmpty()
    val mainWindow = windows.firstOrNull { it.id == mainWindowId }
    val mainPackage = mainWindow?.let { rootPackage(it) }
    val directTyping = mainPackage != null && DirectTouchSettings.isDirectTyping(prefs, mainPackage)
    val target =
      if (mainWindow != null && mainPackage != null) target(windows, mainPackage)
      else DirectTouchTarget.None
    viewsPackage =
      mainPackage.takeIf { target is DirectTouchTarget.Areas && target.viewIds.isNotEmpty() }
    val navBarDirect = isNavBarDirect()
    val keyboards = directKeyboards(windows)
    val ownKeyboard = keyboardWithOwnService(windows, keyboards)
    // The overlays go with every region sent, so they also come back after any region replaced
    // the one the keyboard's service set.
    sendKeyboardOverlay(ownKeyboard?.overlay ?: Region())
    // A keyboard in an input method window alone, whose region SandFly doesn't know.
    val keepsKeyboardRegion = ownKeyboard != null && ownKeyboard.overlay.isEmpty
    val region =
      when (target) {
        DirectTouchTarget.None -> null
        DirectTouchTarget.Screen -> screenRegion(windows, directTyping, navBarDirect, keyboards)
        is DirectTouchTarget.Areas ->
          areasRegion(windows, mainWindow!!, target, directTyping, navBarDirect, keyboards)
      }
    val shouldBeActive = region != null
    if (region != null) {
      partRegionSent = false
      // The keyboard's own accessibility service may have set the region for the keyboard, and the
      // app's would replace it. The region already sent stays until the keyboard closes.
      if (!keepsKeyboardRegion) {
        sendRegion(region)
      }
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
      updatePartRegion(windows, navBarDirect, keyboards, keepsKeyboardRegion)
    }
    reportTouchesDirect()
  }

  /**
   * Whether the navigation bar buttons take touches directly, so that a single tap presses them
   * and holding Home holds it.
   */
  private fun isNavBarDirect(): Boolean = DirectTouchSettings.isNavBarTapEnabled(prefs)

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
   * While direct touch is off, keeps the navigation bar in the passthrough region if tap to
   * activate navigation bar buttons is on, and a keyboard in the direct touch list while it shows,
   * and takes them out again once they aren't. The region is sent again on every look, because
   * other services can clear the shared one, except with [keepsKeyboardRegion] and tap to activate
   * navigation bar buttons on: then nothing is sent, and the region already sent stays.
   */
  private fun updatePartRegion(
    windows: List<AccessibilityWindowInfo>,
    navBarDirect: Boolean,
    keyboards: List<AccessibilityWindowInfo>,
    keepsKeyboardRegion: Boolean,
  ) {
    if (navBarDirect && keepsKeyboardRegion) {
      // The system keeps one passthrough region for all services. The keyboard's own accessibility
      // service may have set one so that the keyboard gets touches directly, as some braille
      // keyboards do, and the navigation bar's would replace it. The region already sent stays.
      return
    }
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

  /** A keyboard whose app has an accessibility service of its own, and its overlays' bounds. */
  private class KeyboardWithService(val overlay: Region)

  /**
   * The keyboard shown that isn't in [keyboards], the direct touch list's, and whose app has an
   * enabled accessibility service of its own, which may set the passthrough region for it. Null when
   * there is none. The keyboard is the default input method's
   * [AccessibilityWindowInfo.TYPE_INPUT_METHOD] window, or an
   * [AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY] window of the default input method's
   * package, as some full-screen braille keyboards draw. The overlay holds those overlay windows'
   * bounds, and is empty for a keyboard in an input method window alone.
   */
  private fun keyboardWithOwnService(
    windows: List<AccessibilityWindowInfo>,
    keyboards: List<AccessibilityWindowInfo>,
  ): KeyboardWithService? {
    val inputMethodShown =
      windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD && it !in keyboards }
    val overlays = windows.filter { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
    if (!inputMethodShown && overlays.isEmpty()) {
      return null
    }
    val keyboard =
      Settings.Secure.getString(service.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        ?.let { ComponentName.unflattenFromString(it)?.packageName }
        ?: return null
    if (keyboard == service.packageName) {
      return null
    }
    val keyboardOverlays = overlays.filter { rootPackage(it) == keyboard }
    if (!inputMethodShown && keyboardOverlays.isEmpty()) {
      return null
    }
    val hasService =
      service
        .getSystemService(AccessibilityManager::class.java)
        ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .orEmpty()
        .any { it.resolveInfo?.serviceInfo?.packageName == keyboard }
    if (!hasService) {
      return null
    }
    val overlay = Region()
    keyboardOverlays.forEach { overlay.union(boundsOf(it)) }
    return KeyboardWithService(overlay)
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

  /**
   * Where the app in front gets direct touch: nowhere while the policy says no, else where the app
   * last asked for it, else where its manifest declares it in this activity and state. An app that
   * manages its own direct touch gets none until it says where, and any other app gets the whole
   * screen.
   */
  private fun target(windows: List<AccessibilityWindowInfo>, mainPackage: String): DirectTouchTarget {
    // Off means off: not even the package manager lookups for apps seen for the first time.
    if (mainPackage in IGNORED_PACKAGES || !DirectTouchSettings.isMasterEnabled(prefs)) {
      return DirectTouchTarget.None
    }
    // The manifest is read the first time an app is seen, and then only while the app is turned on.
    if (!DirectTouchSettings.isSeen(prefs, mainPackage)) {
      DirectTouchSettings.onFirstSight(prefs, mainPackage, app(mainPackage).asksForDirectTouch)
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
    if (!DirectTouchPolicy.shouldBeActive(input)) {
      return DirectTouchTarget.None
    }
    val app = app(mainPackage)
    return DirectTouchRequests.request(mainPackage)
      ?: app.declaration?.target(
        activityByPackage[mainPackage],
        DirectTouchRequests.state(mainPackage),
      )
      ?: if (app.managesDirectTouch) DirectTouchTarget.None else DirectTouchTarget.Screen
  }

  private fun app(pkg: String): DirectTouchApp = apps.getOrPut(pkg) { readApp(pkg) }

  /**
   * Keeps the activity in front of an app whose declaration names activities, from the window
   * state change it sends as the activity opens. Dialogs, menus and other views send them too,
   * with their own class, so only the app's activities count. Apps not seen yet are looked at too,
   * as they may be turned on by their manifest a moment later.
   */
  private fun noteActivity(event: AccessibilityEvent) {
    val pkg = event.packageName?.toString() ?: return
    val className = event.className?.toString() ?: return
    if (
      className.startsWith("android.") ||
        pkg in IGNORED_PACKAGES ||
        pkg == service.packageName ||
        !DirectTouchSettings.isMasterEnabled(prefs) ||
        (DirectTouchSettings.isSeen(prefs, pkg) && !DirectTouchSettings.isAppEnabled(prefs, pkg))
    ) {
      return
    }
    if (app(pkg).declaration?.namesActivities == true && isActivity(pkg, className)) {
      activityByPackage[pkg] = className
    }
  }

  private fun isActivity(pkg: String, className: String): Boolean =
    knownActivities.getOrPut("$pkg/$className") {
      if (knownActivities.size >= MAX_KNOWN_ACTIVITIES) {
        knownActivities.clear()
      }
      try {
        @Suppress("DEPRECATION")
        service.packageManager.getActivityInfo(
          ComponentName(pkg, className),
          PackageManager.MATCH_DISABLED_COMPONENTS,
        )
        true
      } catch (_: PackageManager.NameNotFoundException) {
        false
      }
    }

  /** Looks again shortly after the app's content changes, at most every [VIEW_RECHECK_MS]. */
  private fun scheduleViewRecheck() {
    if (!viewRecheckPosted) {
      viewRecheckPosted = true
      handler.postDelayed(viewRecheck, VIEW_RECHECK_MS)
    }
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

  private fun screenRegion(
    windows: List<AccessibilityWindowInfo>,
    directTyping: Boolean,
    navBarDirect: Boolean,
    keyboards: List<AccessibilityWindowInfo>,
  ): Region =
    DirectTouchRegions.passthroughRegion(
      displayBounds(),
      excludedBounds(windows, directTyping, navBarDirect, keyboards),
    )

  /**
   * The app's areas within its window, without the windows that stay with SandFly, or null when
   * none of them is on screen. The navigation bar and keyboards that take touches directly are
   * added, as they are for the whole screen.
   */
  private fun areasRegion(
    windows: List<AccessibilityWindowInfo>,
    mainWindow: AccessibilityWindowInfo,
    target: DirectTouchTarget.Areas,
    directTyping: Boolean,
    navBarDirect: Boolean,
    keyboards: List<AccessibilityWindowInfo>,
  ): Region? {
    val window = boundsOf(mainWindow)
    val areas =
      target.screenAreas +
        target.windowAreas.mapNotNull {
          it.resolve(window, service.resources.displayMetrics.density)
        } +
        viewBounds(mainWindow, target.viewIds)
    val excluded = excludedBounds(windows, directTyping, navBarDirect, keyboards)
    val region = DirectTouchRegions.areasRegion(window, areas, excluded) ?: return null
    if (navBarDirect) {
      navigationBarBounds(windows).forEach { region.op(it, Region.Op.UNION) }
    }
    windows.forEach {
      if (
        it in keyboards || (directTyping && it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD)
      ) {
        region.op(boundsOf(it), Region.Op.UNION)
      }
    }
    return region
  }

  /** Where the views with [viewIds] are on screen, for those shown in [window]. */
  private fun viewBounds(window: AccessibilityWindowInfo, viewIds: List<String>): List<Rect> {
    if (viewIds.isEmpty()) {
      return emptyList()
    }
    val root = window.root ?: return emptyList()
    return viewIds.flatMap { id ->
      root.findAccessibilityNodeInfosByViewId(id).orEmpty().mapNotNull { node ->
        Rect().takeIf { node.isVisibleToUser }?.also(node::getBoundsInScreen)
      }
    }
  }

  /** The windows that keep touches when the app has direct touch. */
  private fun excludedBounds(
    windows: List<AccessibilityWindowInfo>,
    directTyping: Boolean,
    navBarDirect: Boolean,
    keyboards: List<AccessibilityWindowInfo>,
  ): List<Rect> {
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
    return excluded
  }

  private fun clearRegion() = sendRegion(Region())

  /** Sends the keyboard's overlays when they change; every region sent afterwards includes them. */
  private fun sendKeyboardOverlay(overlay: Region) {
    if (overlay == keyboardOverlay) {
      return
    }
    keyboardOverlay = Region(overlay)
    feedback.returnFeedback(
      Performance.EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(KEYBOARD_REGION, overlay),
    )
  }

  private fun sendRegion(region: Region) {
    feedback.returnFeedback(
      Performance.EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(DIRECT_TOUCH_REGION, region),
    )
  }

  private fun displayBounds(): Rect =
    Rect(service.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds)

  private fun announce(on: Boolean) {
    if (speakChanges && DirectTouchSettings.isSpeechEnabled(prefs)) {
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

  companion object {
    /** The permission an app holds to use [DirectTouchService]. */
    @JvmStatic
    fun permissionName(context: Context): String = "${context.packageName}.permission.DIRECT_TOUCH"

    private const val DEBOUNCE_MS = 150L
    // How often areas that follow views are looked up again while the app's content changes.
    private const val VIEW_RECHECK_MS = 200L
    private const val MAX_KNOWN_ACTIVITIES = 500
    // Longer than the gap between the two reports of one press, shorter than a second tap.
    private const val REPEAT_MS = 250L
    private const val SYSTEM_UI = "com.android.systemui"
    // The names a sound theme replaces the on and off vibrations by.
    private const val THEME_ON = "direct_touch_on"
    private const val THEME_OFF = "direct_touch_off"
    private val ON_PATTERN = longArrayOf(0, 15, 100, 15)
    private val OFF_PATTERN = longArrayOf(0, 40)
    private val VIBRATION_ATTRIBUTES: AudioAttributes =
      AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .build()
    private val IGNORED_PACKAGES = setOf(SYSTEM_UI, "android", "com.google.android.gms")
  }
}
