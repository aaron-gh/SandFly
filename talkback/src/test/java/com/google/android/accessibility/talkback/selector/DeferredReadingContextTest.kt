/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Interpretation
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.input.CursorGranularity
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.monitor.InputModeTracker
import com.google.android.accessibility.utils.monitor.CollectionState
import com.google.common.collect.ImmutableList
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** Default item moves defer only the unused reading-control match, never the focus move. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeferredReadingContextTest {
  private val context = RuntimeEnvironment.getApplication().also { FormFactorUtils.initialize(it) }
  private val matches = mutableListOf<String>()
  private val feedback = Pipeline.FeedbackReturner { true }
  private val focusMonitor = AccessibilityFocusMonitor(null, null, null)
  // Navigation actors are unused here; exercise the real handler, pending state and context match.
  private val controller = (SelectorController::class.java.constructors.single { it.parameterCount == 11 }
    .newInstance(context, feedback, null, focusMonitor, null, null, null, null, null, null, CollectionState())
    as SelectorController).apply {
    val prefs = context.getSharedPreferences("deferred-controls", Context.MODE_PRIVATE)
    prefs.edit().putBoolean(context.getString(R.string.pref_selector_auto_switch_key), true)
      .putString(context.getString(R.string.pref_current_selector_setting_key),
        context.getString(SelectorController.Setting.GRANULARITY_CHARACTERS.prefValueResId)).commit()
    val setting = object : SelectorController.ContextualSetting {
      override fun getSetting() = SelectorController.Setting.ADJUSTABLE_WIDGET
      override fun isNodeSupportSetting(context: Context, node: AccessibilityNodeInfoCompat): Boolean {
        matches.add(node.text.toString())
        return false
      }
    }
    ReflectionHelpers.setField(this, "prefs", prefs)
    ReflectionHelpers.setField(this, "contextualSettings", ImmutableList.of(setting))
    ReflectionHelpers.setField(this, "hiddenSettings", ImmutableList.of<SelectorController.Setting>())
  }

  private fun focus(label: String, mode: Int = InputModeTracker.INPUT_MODE_TOUCH, web: Boolean = true) {
    val node = AccessibilityNodeInfo.obtain(View(context)).apply {
      text = label
      if (web) addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
    }
    val navigation = NavigationAction.Builder().setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setInputMode(mode).setOriginalNavigationGranularity(CursorGranularity.DEFAULT).build()
    val info = FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION)
      .setNavigationAction(navigation).build()
    controller.newItemFocused(node, Interpretation.AccessibilityFocused.create(info, false))
  }

  private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

  @Test fun successiveTouchItemMovesMatchOnlyTheCurrentItemAfterTheIdleInterval() {
    focus("first")
    assertTrue(matches.isEmpty())
    idle(150)
    focus("second")
    idle(100)
    assertTrue(matches.isEmpty())
    idle(100)
    assertEquals(listOf("second"), matches)
  }

  @Test fun usingTheReadingControlResolvesItImmediatelyAndCancelsTheIdleMatch() {
    focus("current item")
    controller.applyPendingSwitch()
    assertEquals(listOf("current item"), matches)
    idle(1000)
    assertEquals(listOf("current item"), matches)
  }

  @Test fun keyboardItemMovesKeepTheirImmediateReadingControlMatch() {
    focus("keyboard item", InputModeTracker.INPUT_MODE_KEYBOARD)
    assertEquals(listOf("keyboard item"), matches)
    idle(1000)
    assertEquals(listOf("keyboard item"), matches)
  }

  @Test fun nativeTouchItemMovesKeepTheirImmediateReadingControlMatch() {
    focus("native item", web = false)
    assertEquals(listOf("native item"), matches)
    idle(1000)
    assertEquals(listOf("native item"), matches)
  }

  @Test fun aNativePendingTouchIsResolvedBeforeTheNextDefaultItemCommand() {
    ReflectionHelpers.setField(controller, "touchActive", true)
    focus("native pending", web = false)
    assertTrue(matches.isEmpty())
    controller.applyPendingSwitchForDefaultNavigation()
    assertEquals(listOf("native pending"), matches)
  }

  private fun enterTable() {
    ReflectionHelpers.setField(controller, "rowColumnGranularity",
      object : RowColumnGranularity(null, null) {
        override fun shouldActivateSetting(context: Context, node: AccessibilityNodeInfoCompat) = true
      })
  }

  @Test fun enteringATableKeepsItsImmediateGestureContextAfterTouchEnds() {
    enterTable()
    focus("table cell")
    assertEquals(listOf("table cell"), matches)
    idle(1000)
    assertEquals(listOf("table cell"), matches)
  }

  @Test fun tableContextPendingDuringTouchIsResolvedBeforeTheNextItemCommand() {
    enterTable()
    ReflectionHelpers.setField(controller, "touchActive", true)
    focus("table cell")
    assertTrue(matches.isEmpty())
    controller.applyPendingSwitchForDefaultNavigation()
    assertEquals(listOf("table cell"), matches)
  }
}
