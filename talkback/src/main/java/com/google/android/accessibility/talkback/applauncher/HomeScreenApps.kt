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

import android.content.Context
import android.os.SystemClock
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The apps a launcher's icons stand for, by name. Launchers don't expose the package of an icon, and
 * reading every label takes up to seconds, so the names are read off the main thread when a launcher
 * window shows.
 */
object HomeScreenApps {
  private const val TAG = "HomeScreenApps"
  private const val REFRESH_MS = 120_000L
  private const val NEVER = -REFRESH_MS

  @Volatile private var byName: Map<String, OpenAppActions.App> = emptyMap()
  @Volatile private var readAt = NEVER
  private val reading = AtomicBoolean(false)

  /** Reads the names again off the main thread, unless they were read in the last two minutes. */
  @JvmStatic
  fun refresh(context: Context) {
    if (SystemClock.uptimeMillis() - readAt < REFRESH_MS || !reading.compareAndSet(false, true)) {
      return
    }
    val appContext = context.applicationContext
    Thread(
        {
          try {
            byName = index(OpenAppActions.apps(appContext))
            readAt = SystemClock.uptimeMillis()
          } catch (e: Throwable) {
            LogUtils.e(TAG, "Could not list the apps: %s", e)
          } finally {
            reading.set(false)
          }
        },
        "home-screen-apps",
      )
      .apply { isDaemon = true }
      .start()
  }

  /** The app an icon named [text] or described as [description] stands for, if any. */
  @JvmStatic
  fun find(text: CharSequence?, description: CharSequence?): OpenAppActions.App? =
    find(byName, text, description)

  internal fun index(apps: List<OpenAppActions.App>): Map<String, OpenAppActions.App> {
    val found = LinkedHashMap<String, OpenAppActions.App>()
    for (app in apps) found.putIfAbsent(AppNameSearch.normalize(app.label), app)
    return found
  }

  /** Icons with a notification dot add text after a comma, and predicted apps a label before a colon. */
  internal fun find(
    apps: Map<String, OpenAppActions.App>,
    text: CharSequence?,
    description: CharSequence?,
  ): OpenAppActions.App? =
    listOfNotNull(
        text,
        description,
        description?.split(',')?.firstOrNull(),
        description?.toString()?.substringAfterLast(": "),
      )
      .firstNotNullOfOrNull { apps[AppNameSearch.normalize(it.toString())] }
}
