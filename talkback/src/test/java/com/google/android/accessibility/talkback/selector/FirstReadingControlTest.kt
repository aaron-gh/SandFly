/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.selector

import android.content.Context
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.preference.ItemOrder
import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.monitor.CollectionState
import com.google.common.collect.ImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** First reading control selects the first entry of the list next and previous go through. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FirstReadingControlTest {
  private val context = RuntimeEnvironment.getApplication().also { FormFactorUtils.initialize(it) }
  private val prefs =
    context.getSharedPreferences("first-reading-control", Context.MODE_PRIVATE).also {
      it.edit().clear().commit()
    }
  private val controller =
    (SelectorController::class.java.constructors.single { it.parameterCount == 11 }
      .newInstance(
        context,
        Pipeline.FeedbackReturner { true },
        null,
        object : AccessibilityFocusMonitor(null, null, null) {
          override fun getAccessibilityFocus(useInputFocusIfEmpty: Boolean) =
            null as AccessibilityNodeInfoCompat?
        },
        null,
        null,
        null,
        null,
        null,
        null,
        CollectionState(),
      ) as SelectorController)
      .apply {
        ReflectionHelpers.setField(this, "prefs", prefs)
        ReflectionHelpers.setField(this, "contextualSettings", ImmutableList.of<Any>())
        ReflectionHelpers.setField(this, "hiddenSettings", ImmutableList.of<Setting>())
      }

  private fun key(setting: Setting) = context.getString(setting.prefKeyResId)

  /** Leaves only [settings] on in the main list. */
  private fun mainList(vararg settings: Setting) {
    val editor = prefs.edit()
    for (setting in Setting.values()) {
      editor.putBoolean(key(setting), setting in settings)
    }
    editor.commit()
  }

  @Test
  fun mainList_firstIsTheFirstReadingControlInTheUsersOrder() {
    mainList(Setting.GRANULARITY_CHARACTERS, Setting.GRANULARITY_WORDS, Setting.GRANULARITY_HEADINGS)
    assertEquals(Setting.GRANULARITY_CHARACTERS, controller.firstRotorEntry())

    ItemOrder.save(
      prefs,
      ItemOrder.READING_CONTROLS_KEY,
      listOf(key(Setting.GRANULARITY_HEADINGS), key(Setting.GRANULARITY_CHARACTERS)),
    )
    assertEquals(Setting.GRANULARITY_HEADINGS, controller.firstRotorEntry())
  }

  @Test
  fun readingControlListInUse_firstIsTheFirstOfThatList() {
    mainList(Setting.GRANULARITY_CHARACTERS, Setting.GRANULARITY_WORDS)
    ReadingControlLists.write(
      prefs,
      listOf(
        ReadingControlLists.ControlList(
          "speech",
          "Speech",
          listOf(key(Setting.GRANULARITY_WORDS), key(Setting.GRANULARITY_HEADINGS)),
        )
      ),
    )
    ReadingControlLists.setInUse(prefs, "speech")
    assertEquals(Setting.GRANULARITY_WORDS, controller.firstRotorEntry())

    ReadingControlLists.setInUse(prefs, "")
    assertEquals(Setting.GRANULARITY_CHARACTERS, controller.firstRotorEntry())
  }

  @Test
  fun noReadingControls_noFirst() {
    mainList()
    assertNull(controller.firstRotorEntry())
  }
}
