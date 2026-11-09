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

package com.google.android.accessibility.talkback.monitor

/**
 * Keeps the time of the first screen-on signal after the screen turned off. The display state
 * change can arrive more than 100 ms before the screen-on broadcast, with lock screen events
 * between them.
 */
class ScreenOnRecorder {
  /** When the screen last turned on, in uptime milliseconds, or 0. */
  var screenOnUptime = 0L
    private set

  private var recorded = false

  fun onScreenOnSignal(uptime: Long) {
    if (!recorded) {
      recorded = true
      screenOnUptime = uptime
    }
  }

  fun onScreenOff() {
    recorded = false
  }
}
