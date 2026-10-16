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

package com.google.android.accessibility.talkback.trainingcommon.content

import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.GestureHints
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Which tutorial texts show as written, for the user's gestures and keyboard shortcuts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialGestureUsesTest {
  private val context: Context
    get() = RuntimeEnvironment.getApplication()

  /** Receives the gestures that a fresh mapping has, as the service sends them. */
  private fun receiveDefaultGestures(data: ServiceData) {
    data.shortcuts.clear()
    val mapping = GestureShortcutMapping(context)
    mapping.gestureActions.forEach { (gestureId, action) ->
      data.shortcuts.putGestureAction(gestureId, action)
    }
    mapping.onUnbind()
  }

  private fun dataWithDefaultGestures() = ServiceData(context).also { receiveDefaultGestures(it) }

  @Test
  fun leftAndRightSwipesSwapOnlyInRtl() {
    assertEquals(GESTURE_SWIPE_RIGHT, GestureHints.asLaidOut(GESTURE_SWIPE_RIGHT, false))
    assertEquals(GESTURE_SWIPE_LEFT, GestureHints.asLaidOut(GESTURE_SWIPE_RIGHT, true))
    assertEquals(GESTURE_SWIPE_RIGHT, GestureHints.asLaidOut(GESTURE_SWIPE_LEFT, true))
  }

  @Test
  fun defaultGesturesKeepTheText() {
    val data = dataWithDefaultGestures()
    assertNull(
        TutorialGestureUses.gestureLinesIfChanged(context, R.string.welcome_to_talkback_text, data))
  }

  @Test
  @Config(qualifiers = "ar-ldrtl")
  fun defaultGesturesKeepTheTextInRtl() {
    // In right-to-left layouts, swiping left moves to the next item, and the texts say so.
    val data = dataWithDefaultGestures()
    assertEquals(
        context.getString(R.string.shortcut_value_previous),
        GestureShortcutMapping(context).getActionKeyFromGestureId(GESTURE_SWIPE_RIGHT))
    assertNull(
        TutorialGestureUses.gestureLinesIfChanged(context, R.string.welcome_to_talkback_text, data))
    assertNull(
        TutorialGestureUses.gestureLinesIfChanged(
            context, R.string.wear_training_welcome_paragraph, data))
  }

  @Test
  fun changedGestureReplacesTheText() {
    val data = dataWithDefaultGestures()
    data.shortcuts.putGestureAction(
        GESTURE_SWIPE_RIGHT, context.getString(R.string.shortcut_value_unassigned))
    val lines =
        TutorialGestureUses.gestureLinesIfChanged(context, R.string.welcome_to_talkback_text, data)
    assertNotNull(lines)
    assertTrue(lines!!.contains(GestureHints.gestureSettingsPath(context)))
  }

  @Test
  fun textShownBeforeTheGesturesArriveIsRedrawnOnceIfChanged() {
    val data = ServiceData(context)
    // Before the service sends its gestures, the text shows as written.
    assertNull(
        TutorialGestureUses.gestureLinesIfChanged(context, R.string.welcome_to_talkback_text, data))
    data.shortcuts.clear()
    data.shortcuts.putGestureAction(
        GESTURE_SWIPE_RIGHT, context.getString(R.string.shortcut_value_unassigned))
    assertTrue(TutorialGestureUses.anyEarlyTextReplaced(context, data))
    assertFalse(TutorialGestureUses.anyEarlyTextReplaced(context, data))
  }

  @Test
  fun textShownBeforeTheGesturesArriveIsNotRedrawnIfUnchanged() {
    val data = ServiceData(context)
    TutorialGestureUses.gestureLinesIfChanged(context, R.string.welcome_to_talkback_text, data)
    receiveDefaultGestures(data)
    assertFalse(TutorialGestureUses.anyEarlyTextReplaced(context, data))
  }

  @Test
  fun everyMappedTextIsOnAPage() {
    // The maps are keyed on string IDs, so a page that changes to a reworded string would show it
    // as written whatever the user's settings.
    val mappingFiles = setOf("TutorialGestureUses.java", "KeyboardTutorialKeys.java")
    val source =
        File("src")
            .walk()
            .filter { it.isFile && it.extension == "java" && it.name !in mappingFiles }
            .filterNot { it.path.startsWith("src/test") }
            .joinToString("\n") { it.readText() }
    val unused =
        TutorialGestureUses.textsNamingShortcuts()
            .map { context.resources.getResourceEntryName(it) }
            .filterNot { Regex("""R\.string\.$it\b""").containsMatchIn(source) }
    assertEquals(emptyList<String>(), unused)
  }
}
