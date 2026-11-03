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

package com.google.android.accessibility.talkback.focusmanagement

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.compositor.Compositor
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.input.CursorGranularity
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_LEFT
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * A swipe from an item of web content asks the page for the next item before reading the window's
 * reading order, which it needs only to leave the page.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebNavigationFirstTest {
  private lateinit var service: TalkBackService
  private lateinit var processor: FocusProcessorForLogicalNavigation
  /** What the navigation asked of the app, in order. */
  private val requests = ArrayList<String>()
  private var pageMoves = true

  private class WindowRead : RuntimeException()

  private inner class Pivot(web: Boolean = true, webView: Boolean = false) :
    AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()) {
    var exists = true

    init {
      className = if (webView) "android.webkit.WebView" else "android.view.View"
      packageName = "example.web"
      isVisibleToUser = true
      if (web) {
        addAction(ACTION_NEXT_HTML_ELEMENT)
        addAction(ACTION_PREVIOUS_HTML_ELEMENT)
      }
    }

    override fun refresh(): Boolean {
      requests.add("refresh")
      return exists
    }

    // Reading the window's root starts building its reading order.
    override fun getWindow(): AccessibilityWindowInfoCompat? {
      requests.add("window")
      throw WindowRead()
    }
  }

  @Before
  fun setUp() {
    service = Robolectric.buildService(TalkBackService::class.java).get()
    FormFactorUtils.initialize(service)
    TraversalTreeCache.clear("test")
    processor = processor(monitor = null)
  }

  @After
  fun tearDown() {
    TraversalTreeCache.clear("test")
    // Ends any wait for a web focus event.
    ColumnWrap.takeWebFocus(
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
    )
  }

  private fun processor(monitor: AccessibilityFocusMonitor?) =
    FocusProcessorForLogicalNavigation(
        service,
        null,
        monitor,
        null,
        null,
        Compositor.TextComposer { _, _, _ -> null },
        null,
      )
      .apply {
        setPipeline(
          Pipeline.FeedbackReturner {
            requests.add("html")
            pageMoves
          }
        )
      }

  /** A focus monitor that returns [pivot] as the accessibility or input focus. */
  private fun monitorReturning(pivot: AccessibilityNodeInfoCompat) =
    object : AccessibilityFocusMonitor(service, null, null) {
      override fun getAccessibilityFocus(
        useInputFocusIfEmpty: Boolean,
        requireEditable: Boolean,
        isCurrent: Filter<AccessibilityNodeInfoCompat>?,
      ): AccessibilityNodeInfoCompat = pivot
    }

  private fun action(
    direction: Int,
    target: Int = NavigationTarget.TARGET_DEFAULT,
    granularity: CursorGranularity? = null,
  ) =
    NavigationAction.Builder()
      .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setDirection(direction)
      .setTarget(target)
      .setOriginalNavigationGranularity(granularity)
      .build()

  private fun navigate(
    pivot: AccessibilityNodeInfoCompat,
    action: NavigationAction,
    ignoreDescendantsOfPivot: Boolean = false,
  ) =
    processor.onDirectionalNavigationAction(
      pivot,
      ignoreDescendantsOfPivot,
      action,
      EVENT_ID_UNTRACKED,
    )

  private fun assertReadsWindow(
    pivot: AccessibilityNodeInfoCompat,
    action: NavigationAction,
    ignoreDescendantsOfPivot: Boolean = false,
  ) {
    assertThrows(WindowRead::class.java) { navigate(pivot, action, ignoreDescendantsOfPivot) }
  }

  @Test
  fun nextAndPreviousInWebContentAskOnlyThePage() {
    assertTrue(navigate(Pivot(), action(SEARCH_FOCUS_FORWARD)))
    assertTrue(navigate(Pivot(), action(SEARCH_FOCUS_BACKWARD)))
    assertEquals(listOf("html", "html"), requests)
  }

  @Test
  fun headingsInWebContentAskOnlyThePage() {
    assertTrue(navigate(Pivot(), action(SEARCH_FOCUS_FORWARD, NavigationTarget.TARGET_HEADING)))
    assertEquals(listOf("html"), requests)
  }

  @Test
  fun leavingCharacterGranularityInWebContentAsksOnlyThePage() {
    val action = action(SEARCH_FOCUS_FORWARD, granularity = CursorGranularity.CHARACTER)
    assertTrue(navigate(Pivot(), action))
    assertEquals(listOf("html"), requests)
  }

  @Test
  fun anEdgeReachedEarlierIsClearedWhenThePageMoves() {
    ReflectionHelpers.setField(processor, "reachEdge", true)
    assertTrue(navigate(Pivot(), action(SEARCH_FOCUS_FORWARD)))
    assertFalse(ReflectionHelpers.getField<Boolean>(processor, "reachEdge"))
  }

  @Test
  fun whenThePageHasNoNextItemTheWindowIsReadWithoutAskingThePageAgain() {
    pageMoves = false
    assertReadsWindow(Pivot(), action(SEARCH_FOCUS_FORWARD))
    assertEquals(listOf("html", "window"), requests)
  }

  @Test
  fun backFromTheWebViewItselfUsesTheWindowOrder() {
    assertReadsWindow(Pivot(webView = true), action(SEARCH_FOCUS_BACKWARD))
    assertEquals(listOf("window"), requests)
  }

  @Test
  fun nativeItemsUseTheWindowOrder() {
    assertReadsWindow(Pivot(web = false), action(SEARCH_FOCUS_FORWARD))
    assertEquals(listOf("window"), requests)
  }

  @Test
  fun spatialDirectionsUseTheWindowOrder() {
    assertReadsWindow(Pivot(), action(SEARCH_FOCUS_LEFT))
    assertEquals(listOf("window"), requests)
  }

  @Test
  fun skippingThePivotsDescendantsUsesTheWindowOrder() {
    assertReadsWindow(Pivot(), action(SEARCH_FOCUS_FORWARD), ignoreDescendantsOfPivot = true)
    assertEquals(listOf("window"), requests)
  }

  @Test
  fun aFocusEventOnTheNextRowPlaysTheColumnWrapSound() {
    val pivot = Pivot().apply { setBoundsInScreen(Rect(0, 0, 100, 50)) }
    assertTrue(navigate(pivot, action(SEARCH_FOCUS_FORWARD)))

    val next = AccessibilityNodeInfo.obtain().apply { setBoundsInScreen(Rect(0, 100, 100, 150)) }
    val focus =
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED).also {
        shadowOf(it).setSourceNode(next)
      }
    assertEquals(ColumnWrap.FORWARD, ColumnWrap.takeWebFocus(focus))
  }

  @Test
  fun accessibilityFocusIsNotCheckedWithTheAppAgain() {
    val pivot = Pivot().apply { isAccessibilityFocused = true }
    processor = processor(monitorReturning(pivot))
    assertTrue(processor.onNavigationAction(action(SEARCH_FOCUS_FORWARD), EVENT_ID_UNTRACKED))
    assertEquals(listOf("html"), requests)
  }

  @Test
  fun inputFocusIsCheckedWithTheApp() {
    val pivot = Pivot()
    processor = processor(monitorReturning(pivot))
    assertTrue(processor.onNavigationAction(action(SEARCH_FOCUS_FORWARD), EVENT_ID_UNTRACKED))
    assertEquals(listOf("refresh", "html"), requests)
  }
}
