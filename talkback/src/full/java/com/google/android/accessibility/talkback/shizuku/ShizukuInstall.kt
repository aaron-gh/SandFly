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

package com.google.android.accessibility.talkback.shizuku

import android.content.Context
import android.os.ParcelFileDescriptor
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Installs [apks], of the given [sizes], as an update to [packageName], recording [installer] as
 * the installer. [onDone] gets whether it worked, and false at once without Shizuku. Closes the
 * file descriptors afterwards.
 */
fun ShizukuShell.install(
  context: Context,
  apks: List<ParcelFileDescriptor>,
  sizes: List<Long>,
  packageName: String,
  installer: String,
  onDone: (Boolean) -> Unit,
) {
  if (!ShizukuGrants.isReady()) {
    apks.forEach { runCatching { it.close() } }
    onDone(false)
    return
  }
  val appContext = context.applicationContext
  ShizukuGrants.withShizukuPermission { allowed ->
    if (!allowed) {
      apks.forEach { runCatching { it.close() } }
      onDone(false)
      return@withShizukuPermission
    }
    call(
      appContext,
      { connected ->
        val error = connected.install(apks.toTypedArray(), sizes.toLongArray(), packageName, installer)
        if (error != null) {
          LogUtils.w("ShizukuShell", "Install as %s failed: %s", installer, error)
        }
        error == null
      },
    ) { worked ->
      apks.forEach { runCatching { it.close() } }
      onDone(worked == true)
    }
  }
}
