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
package com.google.android.accessibility.talkback.applauncher

import android.accessibilityservice.AccessibilityService
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.SpeechController

/** Watches have no braille keyboard, so typing an app's name in braille isn't available. */
@Suppress("UNUSED_PARAMETER")
class BrailleAppLauncher(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  actorState: ActorState,
) {
  fun show() {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        service.getString(R.string.braille_app_not_available),
        AppNameSearch.speakOptions(SpeechController.QUEUE_MODE_INTERRUPT),
      ),
    )
  }

  fun shutdown() {}
}
