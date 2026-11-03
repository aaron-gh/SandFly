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

package io.github.aaron_gh.sandfly.addon

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Shizuku's state for the add-on. */
object AddonShizuku {
  const val REQUEST_CODE = 0x5F17

  /** Whether Shizuku is running and new enough. */
  fun isReady(): Boolean =
    try {
      Shizuku.pingBinder() && !Shizuku.isPreV11()
    } catch (e: RuntimeException) {
      false
    }

  /** Whether Shizuku allows the add-on. */
  fun hasPermission(): Boolean =
    try {
      isReady() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: RuntimeException) {
      false
    }

  /** Whether the user chose "Deny and don't ask again" for the add-on in Shizuku. */
  fun isPermissionDenied(): Boolean =
    try {
      isReady() && !hasPermission() && Shizuku.shouldShowRequestPermissionRationale()
    } catch (e: RuntimeException) {
      false
    }
}
