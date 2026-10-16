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

package com.google.android.accessibility.talkback.actor

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.PrimesController
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenState
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory.WindowIdentifier
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.monitor.InputMethodMonitor
import com.google.android.accessibility.utils.FocusFinder
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.input.WindowsDelegate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAccessibilityWindowInfo

/**
 * Restoring the last focus when the screen changes, and what it costs: fetching a window's root
 * waits for the window's app, which is busy drawing the new screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [FocusActorForScreenStateChangeTest.RootsShadow::class])
class FocusActorForScreenStateChangeTest {

  class TestService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}
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

  /** Names each window by its id, as the window interpreter would. */
  private val windows =
    object : WindowsDelegate {
      override fun getWindowTitle(windowId: Int): CharSequence = "Window $windowId"

      override fun getAccessibilityPaneTitle(windowId: Int): CharSequence = ""

      override fun isSplitScreenMode(displayId: Int): Boolean = false

      override fun hasAccessibilityPane(windowId: Int): Boolean = false
    }

  private lateinit var service: TestService
  private lateinit var history: AccessibilityFocusActionHistory
  private lateinit var actor: FocusActorForScreenStateChange
  private val focused = mutableListOf<AccessibilityNodeInfoCompat?>()

  @Before
  fun setUp() {
    service = Robolectric.setupService(TestService::class.java)
    FormFactorUtils.initialize(service)
    history = AccessibilityFocusActionHistory(service)
    actor =
      FocusActorForScreenStateChange(
        service,
        InputMethodMonitor(service),
        FocusFinder(service),
        PrimesController(),
      )
    actor.setActorState(
      ActorState(
        ActorStateWritable(
          null, null, null, null, history.reader, null, null, null, null, null, null, null, null,
          null,
        )
      )
    )
    actor.setPipeline(
      Pipeline.FeedbackReturner { feedback ->
        focused += feedback.failovers().first().focus()?.target()
        true
      }
    )
  }

  @Test
  fun newScreenWithoutAFocusToRestoreLeavesItsRootAlone() {
    val app = window(APP, AccessibilityWindowInfo.TYPE_APPLICATION, root())
    shadowOf(service).setWindows(listOf(app))

    assertFalse(restore(app))
    assertEquals(0, RootsShadow.rootRequests)
  }

  @Test
  fun systemWindowGetsNoFocusLastPutInAnApp() {
    // The history has a focus for the shade's window id, whose node reports an application
    // window, and the node is still there. Only the rule that keeps a focus from an app out of
    // System UI stops it from being restored.
    val root = root()
    val app = window(SHADE, AccessibilityWindowInfo.TYPE_APPLICATION, root)
    history.onAccessibilityFocusAction(button(app, root), focusInfo(), 1, screen(app))
    val shade = window(SHADE, AccessibilityWindowInfo.TYPE_SYSTEM, root)
    shadowOf(service).setWindows(listOf(shade))
    assertNotNull(
      history.reader.getLastFocusActionRecordInWindow(WindowIdentifier.create(SHADE, screen(shade)))
    )

    assertFalse(restore(shade))
    assertEquals(0, RootsShadow.rootRequests)
  }

  @Test
  fun focusLastPutInTheWindowIsRestored() {
    val root = root()
    val app = window(APP, AccessibilityWindowInfo.TYPE_APPLICATION, root)
    history.onAccessibilityFocusAction(button(app, root), focusInfo(), 1, screen(app))
    shadowOf(service).setWindows(listOf(app))

    assertTrue(restore(app))
    assertEquals(listOf("Play"), focused.map { it?.text?.toString() })
    // One root serves to find the node and to check that the node is still in the window.
    assertEquals(1, RootsShadow.rootRequests)
  }

  /** Restores the last focus into [active], counting only the roots that fetches. */
  private fun restore(active: AccessibilityWindowInfo): Boolean {
    RootsShadow.rootRequests = 0
    return actor.restoreLastFocusedNode(screen(active), EVENT_ID_UNTRACKED)
  }

  private fun screen(active: AccessibilityWindowInfo) =
    ScreenState(windows, active, SystemClock.uptimeMillis())

  private fun root() = AccessibilityNodeInfo().apply { packageName = APP_PACKAGE }

  private fun window(id: Int, type: Int, root: AccessibilityNodeInfo): AccessibilityWindowInfo {
    val window = AccessibilityWindowInfo()
    val shadow = shadowOf(window)
    shadow.setId(id)
    shadow.setType(type)
    shadow.setActive(true)
    shadow.setFocused(true)
    shadow.setBoundsInScreen(Rect(0, 0, 1080, 2340))
    shadow.setRoot(root)
    return window
  }

  /** A button in the window, which can still be found there when its focus is restored. */
  private fun button(
    window: AccessibilityWindowInfo,
    root: AccessibilityNodeInfo,
  ): AccessibilityNodeInfoCompat {
    val node = AccessibilityNodeInfo()
    node.packageName = APP_PACKAGE
    node.className = "android.widget.Button"
    node.text = "Play"
    node.isVisibleToUser = true
    node.isEnabled = true
    node.isClickable = true
    node.isFocusable = true
    node.setBoundsInScreen(Rect(100, 1000, 980, 1150))
    shadowOf(node).setAccessibilityWindowInfo(window)
    shadowOf(node).setRefreshReturnValue(true)
    shadowOf(root).addChild(node)
    return AccessibilityNodeInfoCompat.wrap(node)
  }

  private fun focusInfo() =
    FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build()

  private companion object {
    const val APP = 10
    const val SHADE = 20
    const val APP_PACKAGE = "com.example.app"
  }
}
