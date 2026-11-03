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
 * A permission that the shell service grants an app, which Android only gives through adb.
 *
 * The service gets a grant by its ordinal, and the SandFly add-on may run an older or newer service
 * than SandFly, so add new grants only at the end.
 */
enum class ShellGrant {
  DUMP,
  MANAGE_ONGOING_CALLS,
  WRITE_SETTINGS,
  USAGE_STATS,
  WRITE_SECURE_SETTINGS;

  /** The command that grants it to [packageName], as adb would run it after `adb shell`. */
  fun shellCommand(packageName: String): List<String> =
    when (this) {
      DUMP -> listOf("pm", "grant", packageName, "android.permission.DUMP")
      MANAGE_ONGOING_CALLS -> listOf("appops", "set", packageName, "MANAGE_ONGOING_CALLS", "allow")
      WRITE_SETTINGS -> listOf("appops", "set", packageName, "WRITE_SETTINGS", "allow")
      USAGE_STATS -> listOf("pm", "grant", packageName, "android.permission.PACKAGE_USAGE_STATS")
      WRITE_SECURE_SETTINGS ->
        listOf("pm", "grant", packageName, "android.permission.WRITE_SECURE_SETTINGS")
    }
}
