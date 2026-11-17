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

package com.google.android.accessibility.talkback.macros

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/**
 * Whether the flashlight is on, which Android reports only to torch callbacks, so that macros and
 * scripts can toggle it. Followed from the first time it is asked for.
 */
object Flashlight {
  @Volatile private var on = false
  @Volatile private var following = false

  private fun follow(context: Context) {
    if (following) return
    val manager = context.applicationContext.getSystemService(CameraManager::class.java) ?: return
    following = true
    manager.registerTorchCallback(
      object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
          on = enabled
        }
      },
      Handler(Looper.getMainLooper()),
    )
  }

  fun isOn(context: Context): Boolean {
    follow(context)
    return on
  }

  /** Records a change SandFly made, before Android's callback arrives. */
  fun saw(enabled: Boolean) {
    on = enabled
  }
}
