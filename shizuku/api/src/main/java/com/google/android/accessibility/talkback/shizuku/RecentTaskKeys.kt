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

/**
 * Keys of the arrays in the Bundle that IShizukuGrants.recentTasks returns. Each array has an entry
 * per task, most recent first.
 */
object RecentTaskKeys {
  /** IntArray: task IDs. */
  const val IDS = "ids"
  /** IntArray: the user each task runs as. */
  const val USERS = "users"
  /** Array of String: the package of each task's first activity. */
  const val PACKAGES = "packages"
  /** Array of String: flattened components of each task's first activity, or "". */
  const val BASE_ACTIVITIES = "baseActivities"
  /** Array of String: flattened components of each task's top activity, or "". */
  const val TOP_ACTIVITIES = "topActivities"
  /** Array of String: each task's app name. */
  const val APP_LABELS = "appLabels"
  /** Array of String: the label an app gave its task, or "". */
  const val TITLES = "titles"
}
