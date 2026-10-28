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

package com.google.android.accessibility.talkback.translation

object OnDeviceTranslations {
  private var instance: OnDeviceTranslation? = null

  /** ML Kit's translation, one for the process, since its models are shared. Main thread only. */
  @JvmStatic
  fun get(): OnDeviceTranslation? = instance ?: MlKitTranslation().also { instance = it }
}
