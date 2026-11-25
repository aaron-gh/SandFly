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
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.os.Binder
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.SparseArray
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_ADDED
import android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_REMOVED
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.input.WindowEventInterpreter
import com.google.android.accessibility.utils.monitor.DisplayMonitor
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.shadows.ShadowAccessibilityWindowInfo
import org.robolectric.shadows.ShadowApplicationPackageManager
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/**
 * Feeds window changes through the window interpreter to direct touch, as the service does, and
 * records what direct touch says and which passthrough regions it sends. It also counts what each
 * look at the screen costs: fetching a window's root waits for the window's app, and reading an
 * app's manifest asks the package manager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [34],
  qualifiers = "w360dp-h780dp-xxhdpi",
  shadows =
    [
      DirectTouchControllerTest.WindowsShadow::class,
      DirectTouchControllerTest.RootsShadow::class,
      DirectTouchControllerTest.ManifestsShadow::class,
    ],
)
class DirectTouchControllerTest {

  class TestService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}
  }

  /** Lists the windows a test puts on screen, in a new list each time as Android does. */
  @Implements(AccessibilityService::class)
  class WindowsShadow : ShadowAccessibilityService() {
    var onScreen: List<AccessibilityWindowInfo> = emptyList()

    @Implementation
    override fun getWindows(): List<AccessibilityWindowInfo> = ArrayList(onScreen)

    @Implementation
    override fun getWindowsOnAllDisplays(): SparseArray<List<AccessibilityWindowInfo>> =
      SparseArray<List<AccessibilityWindowInfo>>().apply {
        put(Display.DEFAULT_DISPLAY, ArrayList(onScreen))
      }
  }

  /** Counts how often a window's root is fetched. */
  @Implements(AccessibilityWindowInfo::class)
  class RootsShadow : ShadowAccessibilityWindowInfo() {
    @Implementation
    override fun getRoot(): AccessibilityNodeInfo? {
      rootRequests++
      return super.getRoot()
    }

    companion object {
      var rootRequests = 0
    }
  }

  /** Counts how often the package manager is asked for an app's manifest, with its meta-data. */
  @Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
  class ManifestsShadow : ShadowApplicationPackageManager() {
    @Implementation
    override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo {
      if (flags and PackageManager.GET_META_DATA != 0) {
        manifestRequests++
      }
      return super.getApplicationInfo(packageName, flags)
    }

    @Implementation
    override fun getPackageInfo(packageName: String, flags: Int): PackageInfo? {
      if (flags and PackageManager.GET_META_DATA != 0) {
        manifestRequests++
      }
      return super.getPackageInfo(packageName, flags)
    }

    companion object {
      var manifestRequests = 0
    }
  }

  private lateinit var service: TestService
  private lateinit var serviceWindows: WindowsShadow
  private lateinit var interpreter: WindowEventInterpreter
  private lateinit var controller: DirectTouchController
  private val prefs = FakeSharedPreferences()
  private val transcript = mutableListOf<String>()
  // Manifests that tests give apps, in place of reading them.
  private val declared = HashMap<String, DirectTouchApp>()

  @Before
  fun setUp() {
    service = Robolectric.setupService(TestService::class.java)
    serviceWindows = Shadow.extract(service)
    interpreter = WindowEventInterpreter(service, DisplayMonitor(service))
    controller =
      DirectTouchController(
        service,
        prefs,
        Pipeline.FeedbackReturner { feedback ->
          transcript += describe(feedback)
          true
        },
        interpreter,
      ) { pkg ->
        declared[pkg]
          ?: DirectTouchCapability.read(
            service.packageManager,
            pkg,
            DirectTouchController.permissionName(service),
          )
      }
    interpreter.addListener(controller)
    DirectTouchRequests.clear()
    installApp(GAME_PACKAGE, asksForDirectTouch = false)
    installApp(GAME2_PACKAGE, asksForDirectTouch = true)
    RootsShadow.rootRequests = 0
    ManifestsShadow.manifestRequests = 0
  }

  @Test
  fun gameNotTurnedOnStaysOff() {
    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    // A toast comes and goes over the game.
    windowsChange(TOAST, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game(), toast())
    windowsChange(TOAST, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game())

    assertTranscript()
    // Direct touch looks at the screen several times for each window change. The game's root is
    // fetched once, and its manifest is read once, the first time the game is seen.
    assertEquals(1, RootsShadow.rootRequests)
    assertEquals(1, ManifestsShadow.manifestRequests)
  }

  @Test
  fun gameTurnedOnHandsTouchBackWhileTheShadeIsOpen() {
    DirectTouchSettings.setAppEnabled(prefs, GAME_PACKAGE, true)

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    val underShade = game(inactive = true)
    windowsChange(SHADE, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), underShade, shade())
    windowsChange(SHADE, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game())

    assertTranscript(
      "region [0,80][1080,2220] complex=false",
      "say \"Direct touch on\"",
      "region [0,80][1080,2220] complex=false",
      "region [0,80][1080,2220] complex=false",
      "region none",
      "say \"Direct touch off\"",
      "region [0,80][1080,2220] complex=false",
      "say \"Direct touch on\"",
      "region [0,80][1080,2220] complex=false",
      "region [0,80][1080,2220] complex=false",
    )
    // Only the game's root and the shade's are fetched, once each. The bars are too small to be the
    // shade, so they are never asked whether they belong to System UI.
    assertEquals(2, RootsShadow.rootRequests)
    assertEquals(1, ManifestsShadow.manifestRequests)
  }

  @Test
  fun reportsWhenTouchesStartAndStopGoingDirectly() {
    val reported = mutableListOf<Boolean>()
    controller.onTouchesDirectChanged = Runnable { reported += controller.takesTouchesDirectly() }
    DirectTouchSettings.setAppEnabled(prefs, GAME_PACKAGE, true)

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    val underShade = game(inactive = true)
    windowsChange(SHADE, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), underShade, shade())
    windowsChange(SHADE, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game())

    assertEquals(listOf(true, false, true), reported)
  }

  @Test
  fun gameThatAsksForDirectTouchGetsItTheFirstTimeItIsSeen() {
    windowsChange(GAME2, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game2())
    windowsChange(TOAST, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game2(), toast())
    windowsChange(TOAST, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game2())

    assertTranscript(
      "region [0,80][1080,2220] complex=false",
      "say \"Direct touch on\"",
      "region [0,80][1080,2220] complex=false",
      "region [0,80][1080,2220] complex=false",
      "region [0,80][1080,2220] complex=false",
      "region [0,80][1080,2220] complex=false",
      "region [0,80][1080,2220] complex=false",
      "region [0,80][1080,2220] complex=false",
    )
    assertEquals(1, RootsShadow.rootRequests)
    assertEquals(1, ManifestsShadow.manifestRequests)
  }

  @Test
  fun appThatManagesDirectTouchGetsOnlyTheAreasItAsksFor() {
    declared[GAME_PACKAGE] = DirectTouchApp(true, null, managesDirectTouch = true)

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    // Turned on by its manifest, but it hasn't said where yet.
    assertTranscript()

    request(DirectTouchTarget.Areas(screenAreas = listOf(Rect(0, 0, 1080, 1170))))
    // The status bar stays with SandFly.
    assertTranscript("region [0,80][1080,1170] complex=false", "say \"Direct touch on\"")

    transcript.clear()
    request(DirectTouchTarget.Screen)
    request(DirectTouchTarget.None)
    assertTranscript(
      "region [0,80][1080,2220] complex=false",
      "region none",
      "say \"Direct touch off\"",
    )
  }

  @Test
  fun stopOverridesTheUsersChoiceUntilTheAppAsksAgain() {
    DirectTouchSettings.setAppEnabled(prefs, GAME_PACKAGE, true)
    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    transcript.clear()

    request(DirectTouchTarget.None)
    assertTranscript("region none", "say \"Direct touch off\"")

    transcript.clear()
    request(null)
    assertTranscript("region [0,80][1080,2220] complex=false", "say \"Direct touch on\"")
  }

  @Test
  fun requestsCountOnlyForAppsTurnedOn() {
    declared[GAME_PACKAGE] = DirectTouchApp(false, null, managesDirectTouch = true)
    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())

    request(DirectTouchTarget.Screen)

    assertTranscript()
  }

  @Test
  fun declarationFollowsTheActivityAndTheState() {
    installApp(GAME_PACKAGE, asksForDirectTouch = false, activities = listOf(GAME_ACTIVITY))
    declared[GAME_PACKAGE] =
      DirectTouchApp(
        true,
        DirectTouchDeclaration(
          listOf(
            DirectTouchRule(
              GAME_ACTIVITY,
              null,
              screen = false,
              windowArea = WindowArea(top = AreaLength(0.5f, AreaLength.Unit.FRACTION)),
            ),
            DirectTouchRule(null, "playing", screen = true),
          )
        ),
        managesDirectTouch = true,
      )

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    // A dialog's window state change names a class that isn't an activity.
    windowStateChange(GAME_PACKAGE, "android.app.AlertDialog")
    windowStateChange(GAME_PACKAGE, "com.example.game.Board")
    assertTranscript()

    windowStateChange(GAME_PACKAGE, GAME_ACTIVITY)
    // The bottom half of the window, without the navigation bar.
    assertTranscript("region [0,1170][1080,2220] complex=false", "say \"Direct touch on\"")

    transcript.clear()
    setState("playing")
    setState(null)
    assertTranscript(
      "region [0,80][1080,2220] complex=false",
      "region [0,1170][1080,2220] complex=false",
    )
  }

  @Test
  fun keyboardWithItsOwnServiceKeepsTheRegionThatServiceSets() {
    prefs.edit().putBoolean(DirectTouchSettings.PREF_NAV_BAR_TAP, true).apply()
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf(BRAILLE_KEYBOARD_PACKAGE)

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    assertEquals(listOf(NAVIGATION_BAR_REGION), transcript.distinct())

    // The keyboard's service sets the region for it, which the navigation bar's would replace.
    transcript.clear()
    windowsChange(
      KEYBOARD,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      keyboard(BRAILLE_KEYBOARD_PACKAGE),
    )
    assertTranscript()

    windowsChange(KEYBOARD, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game())
    assertEquals(listOf(NAVIGATION_BAR_REGION), transcript.distinct())
  }

  @Test
  fun keyboardWithoutAServiceKeepsTheNavigationBarRegion() {
    prefs.edit().putBoolean(DirectTouchSettings.PREF_NAV_BAR_TAP, true).apply()
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf("com.example.other")

    windowsChange(
      KEYBOARD,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      keyboard(BRAILLE_KEYBOARD_PACKAGE),
    )

    assertEquals(listOf(NAVIGATION_BAR_REGION), transcript.distinct())
  }

  @Test
  fun keyboardOverlayWithItsOwnServiceTakesTouchesDirectlyWithTheNavigationBar() {
    prefs.edit().putBoolean(DirectTouchSettings.PREF_NAV_BAR_TAP, true).apply()
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf(BRAILLE_KEYBOARD_PACKAGE)

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    assertEquals(listOf(NAVIGATION_BAR_REGION), transcript.distinct())

    // The keyboard draws itself in an accessibility overlay instead of an input method window.
    // The overlay goes with every region sent, so a region sent before SandFly saw the overlay
    // can't keep the keyboard's touches.
    transcript.clear()
    windowsChange(
      OVERLAY,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      overlay(BRAILLE_KEYBOARD_PACKAGE),
    )
    assertEquals(listOf(SCREEN_KEYBOARD_REGION, NAVIGATION_BAR_REGION), transcript.distinct())

    transcript.clear()
    windowsChange(OVERLAY, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game())
    assertEquals(listOf("keyboard region none", NAVIGATION_BAR_REGION), transcript.distinct())
  }

  @Test
  fun keyboardOverlayBesideItsInputMethodWindowTakesTouchesDirectly() {
    // As Advanced Braille Keyboard draws: a full-screen accessibility overlay, and a strip at the
    // bottom in an input method window.
    prefs.edit().putBoolean(DirectTouchSettings.PREF_NAV_BAR_TAP, true).apply()
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf(BRAILLE_KEYBOARD_PACKAGE)

    windowsChange(
      KEYBOARD,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      keyboard(BRAILLE_KEYBOARD_PACKAGE, Rect(0, 2220, 1080, 2340)),
      overlay(BRAILLE_KEYBOARD_PACKAGE),
    )

    assertEquals(listOf(SCREEN_KEYBOARD_REGION, NAVIGATION_BAR_REGION), transcript.distinct())
  }

  @Test
  fun keyboardOverlayTakesTouchesDirectlyWithTheNavigationBarOff() {
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf(BRAILLE_KEYBOARD_PACKAGE)

    windowsChange(
      OVERLAY,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      overlay(BRAILLE_KEYBOARD_PACKAGE),
    )
    assertTranscript(SCREEN_KEYBOARD_REGION)

    transcript.clear()
    windowsChange(OVERLAY, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game())
    assertTranscript("keyboard region none")
  }

  @Test
  fun keyboardOverlayWithoutAServiceTakesNoRegion() {
    prefs.edit().putBoolean(DirectTouchSettings.PREF_NAV_BAR_TAP, true).apply()
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf("com.example.other")

    windowsChange(
      OVERLAY,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      overlay(BRAILLE_KEYBOARD_PACKAGE),
    )

    assertEquals(listOf(NAVIGATION_BAR_REGION), transcript.distinct())
  }

  @Test
  fun overlayOfAnotherServiceKeepsTheNavigationBarRegion() {
    prefs.edit().putBoolean(DirectTouchSettings.PREF_NAV_BAR_TAP, true).apply()
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf(BRAILLE_KEYBOARD_PACKAGE)

    windowsChange(
      OVERLAY,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      overlay(MAGNIFIER_PACKAGE),
    )

    assertEquals(listOf(NAVIGATION_BAR_REGION), transcript.distinct())
  }

  @Test
  fun keyboardWithItsOwnServiceKeepsItsRegionOverAGameWithDirectTouch() {
    DirectTouchSettings.setAppEnabled(prefs, GAME_PACKAGE, true)
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf(BRAILLE_KEYBOARD_PACKAGE)

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    assertEquals(listOf(GAME_REGION, "say \"Direct touch on\""), transcript.distinct())

    transcript.clear()
    windowsChange(
      KEYBOARD,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      keyboard(BRAILLE_KEYBOARD_PACKAGE),
    )
    assertTranscript()

    // A keyboard in an accessibility overlay takes touches directly over it. The game's region
    // leaves out the windows above the game, here all of it.
    windowsChange(
      OVERLAY,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      overlay(BRAILLE_KEYBOARD_PACKAGE),
    )
    assertEquals(listOf(SCREEN_KEYBOARD_REGION, "region none"), transcript.distinct())

    transcript.clear()
    windowsChange(OVERLAY, WINDOWS_CHANGE_REMOVED, statusBar(), navigationBar(), game())
    assertEquals(listOf("keyboard region none", GAME_REGION), transcript.distinct())
  }

  @Test
  fun keyboardWithoutAServiceTakesNoRegionFromAGameWithDirectTouch() {
    DirectTouchSettings.setAppEnabled(prefs, GAME_PACKAGE, true)
    useKeyboard(BRAILLE_KEYBOARD_PACKAGE)
    enableServiceOf("com.example.other")

    windowsChange(GAME, WINDOWS_CHANGE_ADDED, statusBar(), navigationBar(), game())
    transcript.clear()
    windowsChange(
      KEYBOARD,
      WINDOWS_CHANGE_ADDED,
      statusBar(),
      navigationBar(),
      game(),
      keyboard(BRAILLE_KEYBOARD_PACKAGE, Rect(0, 1500, 1080, 2220)),
    )

    // The keyboard stays with SandFly.
    assertEquals(listOf("region [0,80][1080,1500] complex=false"), transcript.distinct())
  }

  // Screens

  private fun statusBar() =
    window(STATUS_BAR, SYSTEM, "Status bar", Rect(0, 0, 1080, 80), SYSTEM_UI)

  private fun navigationBar() =
    window(NAVIGATION_BAR, SYSTEM, "Navigation bar", Rect(0, 2220, 1080, 2340), SYSTEM_UI)

  private fun game(inactive: Boolean = false) =
    window(GAME, APPLICATION, "Game", SCREEN, GAME_PACKAGE, active = !inactive)

  private fun game2() = window(GAME2, APPLICATION, "Game 2", SCREEN, GAME2_PACKAGE, active = true)

  private fun toast() = window(TOAST, SYSTEM, "Saved", Rect(200, 1900, 880, 2050), SYSTEM_UI)

  private fun keyboard(rootPackage: String, bounds: Rect = Rect(0, 80, 1080, 2220)) =
    window(KEYBOARD, INPUT_METHOD, "Keyboard", bounds, rootPackage, layer = 3)

  private fun overlay(rootPackage: String) =
    window(OVERLAY, ACCESSIBILITY_OVERLAY, "Overlay", SCREEN, rootPackage, layer = 10)

  private fun shade() =
    window(SHADE, SYSTEM, "Notification shade", SCREEN, SYSTEM_UI, active = true, layer = 9)

  /** A window as Android lists it, with a title, so that the interpreter needn't name it. */
  private fun window(
    id: Int,
    type: Int,
    title: String,
    bounds: Rect,
    rootPackage: String,
    active: Boolean = false,
    layer: Int = if (type == SYSTEM) 5 else 1,
  ): AccessibilityWindowInfo {
    val window = AccessibilityWindowInfo()
    val shadow = shadowOf(window)
    shadow.setId(id)
    shadow.setType(type)
    shadow.setTitle(title)
    shadow.setBoundsInScreen(bounds)
    shadow.setLayer(layer)
    shadow.setActive(active)
    shadow.setFocused(active)
    shadow.setDisplayId(Display.DEFAULT_DISPLAY)
    shadow.setRoot(AccessibilityNodeInfo().apply { packageName = rootPackage })
    return window
  }

  private fun installApp(
    packageName: String,
    asksForDirectTouch: Boolean,
    activities: List<String> = emptyList(),
  ) {
    val info = PackageInfo()
    info.packageName = packageName
    info.activities =
      activities
        .map {
          ActivityInfo().apply {
            name = it
            this.packageName = packageName
          }
        }
        .toTypedArray()
    info.applicationInfo =
      ApplicationInfo().apply {
        this.packageName = packageName
        if (asksForDirectTouch) {
          metaData = Bundle().apply { putBoolean(DirectTouchSettings.CAPABILITY_KEY, true) }
        }
      }
    shadowOf(service.packageManager).installPackage(info)
  }

  // Keyboards and services

  private fun useKeyboard(packageName: String) {
    Settings.Secure.putString(
      service.contentResolver,
      Settings.Secure.DEFAULT_INPUT_METHOD,
      "$packageName/.Keyboard",
    )
  }

  /** Lists an enabled accessibility service of [packageName]. */
  private fun enableServiceOf(packageName: String) {
    val info = AccessibilityServiceInfo()
    ReflectionHelpers.setField(
      info,
      "mResolveInfo",
      ResolveInfo().apply {
        serviceInfo =
          ServiceInfo().apply {
            this.packageName = packageName
            name = "$packageName.Service"
          }
      },
    )
    shadowOf(service.getSystemService(AccessibilityManager::class.java))
      .setEnabledAccessibilityServiceList(listOf(info))
  }

  // Requests

  private val client = Binder()

  private fun request(target: DirectTouchTarget?) {
    DirectTouchRequests.setRequest(GAME_UID, listOf(GAME_PACKAGE), client, target)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
  }

  private fun setState(state: String?) {
    DirectTouchRequests.setState(GAME_UID, listOf(GAME_PACKAGE), client, state)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
  }

  // Events

  /** An app reports that [className] came to the front, and things settle. */
  private fun windowStateChange(packageName: String, className: String) {
    val event = AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    event.packageName = packageName
    event.className = className
    controller.onAccessibilityEvent(event)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
  }

  /** The windows change to [windows], Android reports it, and things settle for a second. */
  private fun windowsChange(windowId: Int, changes: Int, vararg windows: AccessibilityWindowInfo) {
    serviceWindows.onScreen = windows.toList()
    val event = AccessibilityEvent(AccessibilityEvent.TYPE_WINDOWS_CHANGED)
    ReflectionHelpers.callInstanceMethod<Any>(
      event,
      "setWindowId",
      ClassParameter.from(Int::class.javaPrimitiveType, windowId),
    )
    ReflectionHelpers.callInstanceMethod<Any>(
      event,
      "setWindowChanges",
      ClassParameter.from(Int::class.javaPrimitiveType, changes),
    )
    event.eventTime = SystemClock.uptimeMillis()
    interpreter.interpret(event, null)
    controller.onAccessibilityEvent(event)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
  }

  // Transcript

  private fun assertTranscript(vararg expected: String) {
    assertEquals(expected.joinToString("\n"), transcript.joinToString("\n"))
  }

  private fun describe(feedback: Feedback): String {
    val part = feedback.failovers().first()
    part.speech()?.let {
      return "say \"${it.text()}\""
    }
    part.passThroughMode()?.let {
      val region = it.region()
      val name =
        if (it.action() == Feedback.PassThroughMode.Action.KEYBOARD_REGION) "keyboard region"
        else "region"
      return if (region.isEmpty) {
        "$name none"
      } else {
        "$name ${region.bounds.toShortString()} complex=${region.isComplex}"
      }
    }
    return part.toString()
  }

  private companion object {
    const val STATUS_BAR = 1
    const val NAVIGATION_BAR = 2
    const val SHADE = 3
    const val TOAST = 4
    const val KEYBOARD = 5
    const val OVERLAY = 6
    const val GAME = 10
    const val GAME2 = 11
    const val GAME_PACKAGE = "com.example.game"
    const val GAME_ACTIVITY = "com.example.game.GameActivity"
    const val GAME_UID = 10123
    const val GAME2_PACKAGE = "com.example.game2"
    const val SYSTEM_UI = "com.android.systemui"
    const val SYSTEM = AccessibilityWindowInfo.TYPE_SYSTEM
    const val APPLICATION = AccessibilityWindowInfo.TYPE_APPLICATION
    const val INPUT_METHOD = AccessibilityWindowInfo.TYPE_INPUT_METHOD
    const val ACCESSIBILITY_OVERLAY = AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
    const val BRAILLE_KEYBOARD_PACKAGE = "com.example.braillekeyboard"
    const val MAGNIFIER_PACKAGE = "com.example.magnifier"
    const val NAVIGATION_BAR_REGION = "region [0,2220][1080,2340] complex=false"
    const val GAME_REGION = "region [0,80][1080,2220] complex=false"
    const val SCREEN_KEYBOARD_REGION = "keyboard region [0,0][1080,2340] complex=false"
    val SCREEN = Rect(0, 0, 1080, 2340)
  }
}
