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

package com.google.android.accessibility.talkback.gesture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.IntentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * The dialog shown the first time SandFly starts, which chooses the gesture layout: SandFly's
 * gestures or VoiceOver's. Closing it chooses SandFly's. Then it opens the next screen, the
 * tutorial, which teaches the gestures chosen.
 */
class GestureLayoutActivity : Activity() {

  private var chosen = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.gesture_layout_chooser_dialog)
    setFinishOnTouchOutside(false)
    findViewById<View>(R.id.sandfly_gestures_button).setOnClickListener {
      choose(GestureLayout.SANDFLY)
    }
    findViewById<View>(R.id.voiceover_gestures_button).setOnClickListener {
      choose(GestureLayout.VOICEOVER)
    }
  }

  override fun onDestroy() {
    // Closed with Back or by the system, without a choice.
    if (isFinishing && !chosen) {
      choose(GestureLayout.SANDFLY)
    }
    super.onDestroy()
  }

  private fun choose(layout: String) {
    if (chosen) {
      return
    }
    chosen = true
    GestureLayout.select(SharedPreferencesUtils.getSharedPreferences(this), layout)
    IntentCompat.getParcelableExtra(intent, EXTRA_NEXT, Intent::class.java)?.let {
      startActivity(it)
    }
    if (!isFinishing) {
      finish()
    }
  }

  companion object {
    private const val EXTRA_NEXT = "next"

    /** Returns the intent of the dialog, which opens [next] once a layout is chosen. */
    @JvmStatic
    fun intent(context: Context, next: Intent?): Intent =
      Intent(context, GestureLayoutActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .apply { if (next != null) putExtra(EXTRA_NEXT, next) }
  }
}
