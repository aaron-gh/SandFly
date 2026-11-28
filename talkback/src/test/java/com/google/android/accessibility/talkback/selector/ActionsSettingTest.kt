/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.menurules.NodeMenuRuleCreator
import com.google.android.accessibility.talkback.menurules.NodeMenuRuleCreator.MenuRules
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Whether an item switches the reading control to Actions, without listing its whole menu. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActionsSettingTest {
  private val context: Context =
    RuntimeEnvironment.getApplication().also { FormFactorUtils.initialize(it) }
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val focusMonitor = AccessibilityFocusMonitor(null, null, null)
  private val menus = NodeMenuRuleCreator(Pipeline.FeedbackReturner { true }, null, focusMonitor, null)
  private val setting = ActionsSetting(menus, focusMonitor)

  @Before
  fun setUp() {
    prefs
      .edit()
      .clear()
      .putBoolean(context.getString(SelectorController.Setting.ACTIONS.prefKeyResId), true)
      .commit()
  }

  @Suppress("DEPRECATION")
  private fun node(): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.obtain().apply {
      packageName = PACKAGE
      isVisibleToUser = true
    }

  private fun child(parent: AccessibilityNodeInfoCompat, child: AccessibilityNodeInfoCompat) {
    shadowOf(parent.unwrap()).addChild(child.unwrap())
  }

  private fun linkText(): AccessibilityNodeInfoCompat =
    node().apply {
      text =
        SpannableString("Read the terms").apply {
          setSpan(URLSpan("https://example.com/terms"), 9, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

  /** The full menu lists something exactly where the check finds an item. */
  private fun assertMatchesFullMenu(node: AccessibilityNodeInfoCompat) {
    val listed =
      menus.getNodeMenuByRule(MenuRules.RULE_CUSTOM_ACTION, context, node, /* includeAncestors= */ true)
    assertEquals(listed.isNotEmpty(), setting.isNodeSupportSetting(context, node))
  }

  @Test
  fun anItemWithoutActionsKeepsTheReadingControl() {
    val node = node().apply { text = "Plain text" }
    assertFalse(setting.shouldActivateSetting(context, node))
    assertFalse(setting.isNodeSupportSetting(context, node))
    assertMatchesFullMenu(node)
  }

  @Test
  fun textInARowWithAppActionsSwitchesToActions() {
    val row = node()
    row.addAction(AccessibilityActionCompat(0x7f0a0001, "Archive"))
    val text = node().apply {
      text = "Message"
      addAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)
    }
    child(row, text)
    assertTrue(setting.shouldActivateSetting(context, text))
    assertTrue(setting.isNodeSupportSetting(context, text))
    assertMatchesFullMenu(text)
  }

  @Test
  fun anItemWithOnlyMacrosKeepsTheReadingControlButListsThem() {
    Macros.write(prefs, listOf(Macros.Macro("a", "Reply", emptyList(), app = PACKAGE, inActions = true)))
    val node = node().apply { text = "Message" }
    assertFalse(setting.shouldActivateSetting(context, node))
    assertTrue(setting.isNodeSupportSetting(context, node))
    assertTrue(setting.hasOnlyMacros(context, node))
    assertMatchesFullMenu(node)
  }

  @Test
  fun aLinkInTextInsideTheItemSwitchesToActions() {
    val item = node()
    child(item, node().apply { text = "Terms" })
    child(item, linkText())
    assertTrue(setting.shouldActivateSetting(context, item))
    assertMatchesFullMenu(item)
  }

  @Test
  fun aLinkBelowAnItemWithAContentDescriptionIsNotCounted() {
    val item = node().apply { contentDescription = "Terms" }
    child(item, linkText())
    assertFalse(setting.shouldActivateSetting(context, item))
    assertMatchesFullMenu(item)
  }

  @Test
  fun aLinkInAFocusableChildIsNotCounted() {
    val item = node()
    child(item, linkText().apply { isClickable = true })
    assertFalse(setting.shouldActivateSetting(context, item))
    assertMatchesFullMenu(item)
  }

  private companion object {
    const val PACKAGE = "com.example.app"
  }
}
