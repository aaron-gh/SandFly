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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Settings profiles: named sets of SandFly's settings, one of them in use. Settings changed while a
 * profile is in use are saved to it before another profile is applied, when the Settings profiles
 * screen opens and when SandFly stops. API keys, the SandFly Pro licence and the state of this phone
 * are not in profiles and stay as they are; see [SettingsBackup.exportProfile].
 *
 * The first time profiles are used, the current settings become the profile Default.
 */
object SettingsProfiles {
  private const val TAG = "SettingsProfiles"
  private const val DIRECTORY = "settings_profiles"

  @Volatile private var store: SettingsProfileStore? = null

  private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

  /** Reads and writes the files and settings one change at a time, away from the main thread. */
  private val executor =
    ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

  /** The store of profiles, in device-protected storage like the settings. */
  @JvmStatic
  fun store(context: Context): SettingsProfileStore =
    store
      ?: synchronized(this) {
        store
          ?: SettingsProfileStore(
              File(context.applicationContext.createDeviceProtectedStorageContext().filesDir, DIRECTORY)
            )
            .also { store = it }
      }

  /** The name of the profile made from the settings the first time profiles are used. */
  @JvmStatic
  fun defaultName(context: Context): String = context.getString(R.string.settings_profile_default)

  /**
   * The ID and name of each profile, in order, and the ID of the one in use. Before profiles are
   * used, Default, with an empty ID, is the only one.
   */
  @JvmStatic
  fun choices(context: Context): Pair<List<SettingsProfileStore.Profile>, String> {
    val store = store(context)
    val active = store.active() ?: return listOf(SettingsProfileStore.Profile("", defaultName(context))) to ""
    return store.profiles() to active.id
  }

  /** Runs [work] in the background, then [then] with its result on the main thread. */
  @JvmStatic
  fun <T> inBackground(context: Context, work: (Context) -> T, then: Consumer<T>) {
    val app = context.applicationContext
    executor.execute {
      val result = work(app)
      mainHandler.post { then.accept(result) }
    }
  }

  /** Makes the current settings the profile Default if there are no profiles, and returns the store. */
  @WorkerThread
  fun ready(context: Context): SettingsProfileStore {
    val store = store(context)
    synchronized(store) {
      if (store.isEmpty() && store.add(defaultName(context), SettingsBackup.exportProfile(context)) == null) {
        LogUtils.e(TAG, "Cannot save the settings as a profile")
      }
    }
    return store
  }

  /** Saves the settings to the profile in use, once there are profiles. */
  @JvmStatic
  fun saveActive(context: Context) {
    val app = context.applicationContext
    executor.execute { saveActiveNow(app) }
  }

  @WorkerThread
  private fun saveActiveNow(context: Context): Boolean {
    val store = store(context)
    val active = store.active() ?: return true
    return store.saveSettings(active.id, SettingsBackup.exportProfile(context))
  }

  /** Adds a profile called [name] with the current settings, and returns it, or null. */
  @WorkerThread
  fun add(context: Context, name: String): SettingsProfileStore.Profile? {
    val store = ready(context)
    saveActiveNow(context)
    return store.add(name, SettingsBackup.exportProfile(context))
  }

  /** Replaces the settings of the profile [id] with the current settings. */
  @WorkerThread
  fun saveTo(context: Context, id: String): Boolean =
    ready(context).saveSettings(id, SettingsBackup.exportProfile(context))

  /**
   * Saves the settings to the profile in use, then applies the profile [id], or the one in use for
   * an empty ID, and calls [onDone] on the main thread with what to say.
   */
  @JvmStatic
  fun switchTo(context: Context, id: String, onDone: Consumer<String>) {
    inBackground(context, { switchNow(it, id) }, onDone)
  }

  /** Changes to the profile after the one in use, as [switchTo] does. */
  @JvmStatic
  fun switchToNext(context: Context, onDone: Consumer<String>) {
    inBackground(context, { switchNow(it, ready(it).next()?.id.orEmpty()) }, onDone)
  }

  /** Forgets the store, so that the next use reads it again. */
  @VisibleForTesting
  internal fun resetStore() {
    store = null
  }

  @VisibleForTesting
  @WorkerThread
  internal fun switchNow(context: Context, id: String): String {
    val store = ready(context)
    val target = if (id.isEmpty()) store.active() else store.profile(id)
    if (target == null || !apply(context, store, target)) {
      return context.getString(R.string.settings_profile_not_changed)
    }
    return context.getString(R.string.settings_profile_in_use_announcement, target.name)
  }

  /** Saves the settings to the profile in use, and applies [target]. */
  @WorkerThread
  private fun apply(
    context: Context,
    store: SettingsProfileStore,
    target: SettingsProfileStore.Profile,
  ): Boolean {
    if (target.id == store.active()?.id) return true
    // Without the settings saved, the changes made in the profile in use would be lost.
    if (!saveActiveNow(context)) return false
    val settings = store.settings(target.id) ?: return false
    if (!SettingsBackup.importProfile(context, settings)) return false
    if (!store.setActive(target.id)) {
      LogUtils.e(TAG, "Applied a profile but cannot save which is in use")
    }
    // The keyboard shortcuts are read once, when the key combo model is built.
    mainHandler.post { TalkBackService.getInstance()?.keyComboManager?.refreshKeyComboModel() }
    return true
  }
}
