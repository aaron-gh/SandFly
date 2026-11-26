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

package com.google.android.accessibility.talkback.training

import android.content.Context
import com.google.android.accessibility.talkback.tutorial.TutorialActivity
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks which tutorial opens the first time SandFly starts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialInitiatorTest {

  private val context = RuntimeEnvironment.getApplication()

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
  }

  /** Input devices as a phone reports them. */
  private class Devices(
    context: Context,
    private val keyboard: Boolean,
    private val touchScreen: Boolean,
  ) : InputDeviceMonitor(context) {
    override fun hasPhysicalKeyboard() = keyboard

    override fun hasTouchScreen() = touchScreen
  }

  private fun firstRun(keyboard: Boolean, touchScreen: Boolean) =
    TutorialInitiator.createFirstRunTutorialIntent(context, Devices(context, keyboard, touchScreen))

  private fun isKeyboardTutorial(keyboard: Boolean, touchScreen: Boolean) =
    firstRun(keyboard, touchScreen).getBooleanExtra(TutorialActivity.EXTRA_KEYBOARD, false)

  @Test
  fun touchScreen_opensGesturesTutorial() {
    val intent = firstRun(keyboard = false, touchScreen = true)
    assertEquals(TutorialActivity::class.java.name, intent.component?.className)
    assertFalse(isKeyboardTutorial(keyboard = false, touchScreen = true))
  }

  /**
   * After the user chooses SandFly's or VoiceOver's gestures on a touch screen, the tutorial that
   * opens teaches gestures, even when the phone reports a keyboard (some phones report their side
   * buttons as one).
   */
  @Test
  fun touchScreenWithKeyboard_opensGesturesTutorial() {
    val intent = firstRun(keyboard = true, touchScreen = true)
    assertEquals(TutorialActivity::class.java.name, intent.component?.className)
    assertFalse(isKeyboardTutorial(keyboard = true, touchScreen = true))
  }

  @Test
  fun keyboardWithoutTouchScreen_opensKeyboardTutorial() {
    assertTrue(isKeyboardTutorial(keyboard = true, touchScreen = false))
  }
}
