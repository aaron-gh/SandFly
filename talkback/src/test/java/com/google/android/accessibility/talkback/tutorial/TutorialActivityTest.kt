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

package com.google.android.accessibility.talkback.tutorial

import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.content.Context
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialActivityTest {
  private val context: Context
    get() = RuntimeEnvironment.getApplication()

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
  }

  @After
  fun tearDown() {
    TutorialHooks.listener = null
  }

  private fun open(lesson: String) =
    Robolectric.buildActivity(TutorialActivity::class.java, TutorialActivity.intent(context, lesson))
      .setup()

  private fun openKeyboard(lesson: String) =
    Robolectric.buildActivity(
        TutorialActivity::class.java,
        TutorialActivity.keyboardIntent(context, lesson),
      )
      .setup()

  private fun useEnhancedKeymap() {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(
        context.getString(R.string.pref_select_keymap_key),
        context.getString(R.string.new_keymap_entry_value),
      )
      .commit()
  }

  /** Gives the activity's window focus, which Robolectric doesn't. */
  private fun focusWindow(activity: TutorialActivity) {
    val info =
      View::class.java.getDeclaredField("mAttachInfo").apply { isAccessible = true }.get(activity.window.decorView)
    info.javaClass.getDeclaredField("mHasWindowFocus").apply { isAccessible = true }.setBoolean(info, true)
  }

  private fun texts(view: View): List<TextView> =
    when (view) {
      is TextView -> listOf(view)
      is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
      else -> emptyList()
    }

  private fun TutorialActivity.find(text: CharSequence): TextView? =
    texts(window.decorView).firstOrNull { it.text.toString() == text.toString() }

  private fun TutorialActivity.instruction(): String =
    texts(window.decorView).first { it.text.startsWith(getString(R.string.sandfly_tutorial_step).take(4)) }
      .text
      .toString()

  @Test
  fun everyLessonOpensWithItsInstruction() {
    for (lesson in TutorialLessons.all) {
      val activity = open(lesson.id).get()
      val heading = activity.getString(lesson.title)
      assertTrue(lesson.id, texts(activity.window.decorView).any { it.text.startsWith(heading) })
      for (view in texts(activity.window.decorView)) {
        assertFalse(lesson.id, view.text.contains("%"))
      }
    }
  }

  @Test
  fun swipingToEggsGoesToTheNextStep() {
    val activity = open("move").get()
    val before = activity.instruction()
    val listener = TutorialHooks.listener
    assertNotNull(listener)
    listener!!.onGesture(GESTURE_SWIPE_RIGHT, activity.getString(R.string.shortcut_value_next))
    activity
      .find(activity.getString(R.string.sandfly_tutorial_item_eggs))!!
      .performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    shadowOf(Looper.getMainLooper()).idle()
    val after = activity.instruction()
    assertTrue(before.contains(activity.getString(R.string.sandfly_tutorial_item_eggs)))
    assertTrue(after.contains(activity.getString(R.string.sandfly_tutorial_item_bread)))
  }

  @Test
  fun aFinishedLessonShowsTheNextLessonButton() {
    val activity = open("activate").get()
    activity.find(activity.getString(R.string.sandfly_tutorial_skip_step))!!.apply {
      repeat(3) { performClick() }
    }
    shadowOf(Looper.getMainLooper()).idle()
    val next =
      activity.getString(
        R.string.sandfly_tutorial_next_lesson,
        activity.getString(R.string.sandfly_tutorial_pause_title),
      )
    assertNotNull(activity.find(next))
  }

  @Test
  fun contentsListsEveryLesson() {
    val activity = open("welcome").get()
    activity.find(activity.getString(R.string.sandfly_tutorial_contents))!!.performClick()
    for (lesson in TutorialLessons.all) {
      assertNotNull(lesson.id, activity.find(activity.getString(lesson.title)))
    }
  }

  @Test
  fun everyKeyboardLessonOpensWithItsKeysInBothKeymaps() {
    for (enhanced in listOf(false, true)) {
      if (enhanced) useEnhancedKeymap()
      for (lesson in TutorialLessons.keyboard) {
        val activity = openKeyboard(lesson.id).get()
        val heading = activity.getString(lesson.title)
        assertTrue(lesson.id, texts(activity.window.decorView).any { it.text.startsWith(heading) })
        for (view in texts(activity.window.decorView)) {
          assertFalse(lesson.id, view.text.contains("%"))
        }
      }
    }
  }

  @Test
  fun enhancedKeymapNamesEveryKeyboardLessonsKeys() {
    useEnhancedKeymap()
    val missing = context.getString(R.string.sandfly_keyboard_tutorial_no_key).substringAfter("%1\$s")
    for (lesson in TutorialLessons.keyboard) {
      val activity = openKeyboard(lesson.id).get()
      activity.find(activity.getString(R.string.sandfly_tutorial_skip_step))!!.apply {
        for (step in lesson.steps) {
          assertFalse(
            lesson.id,
            texts(activity.window.decorView).any { it.text.contains(missing.take(20)) },
          )
          performClick()
        }
      }
    }
  }

  @Test
  fun welcomeNamesTheActionKeyInTheEnhancedKeymap() {
    useEnhancedKeymap()
    val activity = openKeyboard("welcome").get()
    assertNotNull(
      texts(activity.window.decorView).firstOrNull {
        it.text.contains(activity.getString(R.string.sandfly_keyboard_tutorial_action_key_location))
      }
    )
  }

  @Test
  fun keysToEggsGoToTheNextStep() {
    val activity = openKeyboard("move").get()
    val listener = TutorialHooks.listener!!
    assertFalse(listener.onKeys("Alt + Right arrow", activity.getString(R.string.shortcut_value_next)))
    activity
      .find(activity.getString(R.string.sandfly_tutorial_item_eggs))!!
      .performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(activity.instruction().contains(activity.getString(R.string.sandfly_tutorial_item_bread)))
  }

  @Test
  fun systemKeysAreTakenAndCountInTheirStep() {
    val activity = openKeyboard("system").get()
    val listener = TutorialHooks.listener!!
    assertFalse(listener.onSystemKeys(KeyEvent.KEYCODE_ESCAPE))
    focusWindow(activity)
    assertTrue(listener.onSystemKeys(KeyEvent.KEYCODE_ESCAPE))
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(activity.instruction().startsWith(activity.getString(R.string.sandfly_tutorial_step).take(4)))
    assertTrue(activity.instruction().contains("H"))
  }

  @Test
  fun keyboardAndTouchTutorialsKeepTheirOwnLesson() {
    openKeyboard("typing")
    val touch = open("move").get()
    assertNotNull(touch.find(touch.getString(R.string.sandfly_tutorial_item_eggs)))
    val keyboard =
      Robolectric.buildActivity(TutorialActivity::class.java, TutorialActivity.keyboardIntent(context))
        .setup()
        .get()
    assertNotNull(keyboard.find(keyboard.getString(R.string.sandfly_tutorial_name)))
  }

  @Test
  fun backStaysInALessonAndClosesFromContents() {
    val activity = open("move").get()
    activity.onBackPressedDispatcher.onBackPressed()
    assertFalse(activity.isFinishing)
    assertNotNull(activity.find(activity.getString(R.string.sandfly_tutorial_item_eggs)))
    activity.find(activity.getString(R.string.sandfly_tutorial_contents))!!.performClick()
    activity.onBackPressedDispatcher.onBackPressed()
    assertTrue(activity.isFinishing)
  }
}
