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

package com.google.android.accessibility.talkback.recognition

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.google.android.accessibility.talkback.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveRecognitionActivityTest {
  @Test fun openingTheScreenDoesNotEnableAnyDetectorOrRequestPermission() {
    val controller = Robolectric.buildActivity(LiveRecognitionActivity::class.java).setup()
    val activity = controller.get()
    assertTrue(RecognitionMode.entries.none { activity.isEnabled(it) })
    assertNull(shadowOf(activity).lastRequestedPermission)
    val checkboxes = descendants(activity.window.decorView).filterIsInstance<CheckBox>().toList()
    RecognitionMode.entries.forEach { mode ->
      assertTrue(checkboxes.any { it.text == activity.getString(mode.label) && !it.isChecked })
    }
    controller.pause().stop().destroy()
  }

  @Test fun permissionRequestDoesNotTurnTheModeOnPrematurely() {
    val controller = Robolectric.buildActivity(LiveRecognitionActivity::class.java).setup()
    val activity = controller.get()
    activity.toggle(RecognitionMode.TEXT)
    assertArrayEquals(arrayOf(Manifest.permission.CAMERA), shadowOf(activity).lastRequestedPermission.requestedPermissions)
    assertFalse(activity.isEnabled(RecognitionMode.TEXT))
    controller.pause().stop().destroy()
  }

  @Test fun recreationDoesNotReactivateTheModeFromTheOriginalRotorIntent() {
    val controller = Robolectric.buildActivity(LiveRecognitionActivity::class.java)
    controller.get().intent.putExtra(LiveRecognitionActivity.EXTRA_MODE, RecognitionMode.TEXT.name)
    controller.create(Bundle()).start().resume()
    assertNull(shadowOf(controller.get()).lastRequestedPermission)
    assertFalse(controller.get().isEnabled(RecognitionMode.TEXT))
    controller.pause().stop().destroy()
  }

  private fun descendants(view: View): Sequence<View> = sequence {
    yield(view)
    if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
  }
}
