/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.keyboard

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlreadyEnabledBrowseModeTest {
  private val context get() = RuntimeEnvironment.getApplication()

  @Before fun initialize() { FormFactorUtils.initialize(context) }

  private fun node(web: Boolean): AccessibilityNodeInfo =
    AccessibilityNodeInfo.obtain(View(context)).apply {
      className = if (web) "android.webkit.WebView" else "android.widget.Button"
    }

  private fun send(manager: KeyComboManager, web: Boolean) {
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
    shadowOf(event).setSourceNode(node(web))
    val method = KeyComboManager::class.java.declaredMethods.single {
      it.name == "handleViewAccessibilityFocusedEvent"
    }
    method.isAccessible = true
    method.invoke(manager, event, null)
  }

  private fun enabled(manager: KeyComboManager): Boolean =
    ReflectionHelpers.getField(manager, "browseModeEnabled")

  @Test fun alreadyEnabledWebBrowseModeDoesNotQueryInputFocus() {
    val manager = KeyComboManager.create(context)
    ReflectionHelpers.setField(manager, "browseModeEnabled", true)
    ReflectionHelpers.setField(manager, "accessibilityFocusMonitor",
      object : AccessibilityFocusMonitor(null, null, null) {
        override fun getInputFocus(): AccessibilityNodeInfoCompat? =
          error("Input focus is irrelevant when the handler can only keep browse mode on")
      })
    send(manager, web = true)
    assertTrue(enabled(manager))
  }

  @Test fun leavingWebContentStillClearsTheLocalManualOverride() {
    val manager = KeyComboManager.create(context)
    ReflectionHelpers.setField(manager, "browseModeEnabled", true)
    ReflectionHelpers.setField(manager, "browseModeTurnedOffByUser", true)
    send(manager, web = false)
    assertFalse(ReflectionHelpers.getField(manager, "browseModeTurnedOffByUser"))
    assertTrue(enabled(manager))
  }

  @Test fun manuallyDisabledBrowseModeStaysOff() {
    val manager = KeyComboManager.create(context)
    ReflectionHelpers.setField(manager, "browseModeTurnedOffByUser", true)
    send(manager, web = true)
    assertFalse(enabled(manager))
  }

  @Test fun editableInputStillPreventsAutomaticEnabling() {
    val manager = KeyComboManager.create(context)
    val editable = AccessibilityNodeInfoCompat.wrap(node(web = false).apply {
      isFocused = true
      isEditable = true
    })
    ReflectionHelpers.setField(manager, "accessibilityFocusMonitor",
      object : AccessibilityFocusMonitor(null, null, null) {
        override fun getInputFocus(): AccessibilityNodeInfoCompat = editable
      })
    send(manager, web = true)
    assertFalse(enabled(manager))
  }
}
