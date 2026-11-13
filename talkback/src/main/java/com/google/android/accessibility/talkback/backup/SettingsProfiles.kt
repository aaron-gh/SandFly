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
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Settings profiles: named sets of settings that replace the settings while a profile is used. A
 * profile is used when it is chosen to be used everywhere, or in its apps and contexts: web content,
 * continuous reading and text editing. When profiles in use set the same setting, the chosen one
 * wins, then the one higher in the list. Profiles with triggers aren't used in SandFly's own app,
 * so that its settings show the settings without them.
 *
 * A setting changed while a profile sets it is saved to that profile. Other settings changed are
 * the settings without profiles, except while a profile's settings are being changed
 * ([startEditing]), when every setting changed goes to that profile.
 *
 * API keys, the SandFly Pro licence and the state of this phone are not in profiles; see
 * [SettingsBackup.profileValues].
 */
object SettingsProfiles {
  private const val TAG = "SettingsProfiles"
  private const val DIRECTORY = "settings_profiles"

  @Volatile private var store: SettingsProfileStore? = null

  private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

  /** Reads and writes the files and settings one change at a time, away from the main thread. */
  private val executor =
    ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

  /** Held while the settings and the state of the profiles applied are read or changed. */
  private val lock = Any()

  @Volatile private var appContext: Context? = null

  // Where the user is, set on the main thread.
  @Volatile private var app = ""
  @Volatile private var inWeb = false
  @Volatile private var inEditing = false
  @Volatile private var inReading = false

  // Which contexts a profile is used in, so that the others aren't followed.
  @Volatile private var watchesApps = false
  @Volatile private var watchesFocus = false
  @Volatile private var watchesReading = false

  /** The profile whose settings are being changed, or null. */
  @Volatile private var editingId: String? = null

  /** The profiles applied last, first wins, or null before the first time. */
  @Volatile private var appliedIds: List<String>? = null

  private val applyPending = AtomicBoolean(false)

  /** Whether settings are being written for profiles, which the service reloads once afterwards. */
  @Volatile private var writing = false

  /**
   * The store of profiles, in device-protected storage like the settings. The first call reads the
   * index; [preload] makes that call away from the main thread.
   */
  @JvmStatic
  fun store(context: Context): SettingsProfileStore =
    store
      ?: synchronized(this) {
        store
          ?: run {
            val app = context.applicationContext
            SettingsProfileStore(
                File(app.createDeviceProtectedStorageContext().filesDir, DIRECTORY)
              ) { number ->
                app.getString(R.string.settings_profile_new_name, number)
              }
              .also {
                updateWatches(it)
                store = it
              }
          }
      }

  /** The store of profiles if it has been read, or null. */
  @JvmStatic fun loadedStore(): SettingsProfileStore? = store

  /**
   * Reads the store of profiles in the background, finishes changing the settings of a profile
   * left from before, and applies the profiles in use.
   */
  @JvmStatic
  fun preload(context: Context) {
    val app = context.applicationContext
    appContext = app
    executor.execute {
      finishEditingNow(app)
      applyNow(app, force = true)
    }
  }

  /** Whether settings are being written for profiles. */
  @JvmStatic fun isWriting(): Boolean = writing

  /** Runs [work] in the background, then [then] with its result on the main thread. */
  @JvmStatic
  fun <T> inBackground(context: Context, work: (Context) -> T, then: Consumer<T>) {
    val app = context.applicationContext
    executor.execute {
      val result = work(app)
      mainHandler.post { then.accept(result) }
    }
  }

  /** The store, with profiles of version 1 made profiles of this version. */
  @WorkerThread
  fun ready(context: Context): SettingsProfileStore {
    val store = store(context)
    synchronized(lock) {
      if (store.isLegacy()) convertLegacy(context, store)
    }
    return store
  }

  /**
   * Makes each profile of version 1, which holds every setting, a profile that sets the settings in
   * which it differs from the profile in use. The profile in use, whose settings are the settings
   * now, is deleted.
   */
  private fun convertLegacy(context: Context, store: SettingsProfileStore) {
    val current = SettingsBackup.profileValues(context)
    val active = store.legacyActive()
    val converted = mutableMapOf<String, Map<String, Any?>>()
    for (profile in store.profiles()) {
      if (profile.id == active) continue
      val values =
        store.legacySettings(profile.id)?.let { SettingsBackup.profileValuesOf(context, it) }
      converted[profile.id] = values?.let { difference(it, current) } ?: emptyMap()
    }
    if (!store.finishLegacy(converted)) LogUtils.e(TAG, "Cannot convert the settings profiles")
    updateWatches(store)
  }

  /** The settings of [target] that differ from [from], with null for those [target] doesn't set. */
  @VisibleForTesting
  internal fun difference(target: Map<String, Any?>, from: Map<String, Any?>): Map<String, Any?> =
    (target.keys + from.keys).filter { target[it] != from[it] }.associateWith { target[it] }

  /** The profiles and the ID of the one used everywhere, or an empty ID for none. */
  @JvmStatic
  fun choices(context: Context): Pair<List<SettingsProfileStore.Profile>, String> {
    val store = store(context)
    return store.profiles() to (store.manual()?.id ?: "")
  }

  /** The IDs of the profiles in use. */
  @JvmStatic fun inUse(): List<String> = appliedIds.orEmpty()

  /** The ID of the profile whose settings are being changed, or null. */
  @JvmStatic fun editing(): String? = editingId

  private fun updateWatches(store: SettingsProfileStore) {
    val profiles = store.profiles()
    watchesApps = profiles.any { it.triggers.apps.isNotEmpty() }
    watchesFocus = profiles.any { it.triggers.web || it.triggers.editing }
    watchesReading = profiles.any { it.triggers.reading }
  }

  /** Notes the app whose window is shown. */
  @JvmStatic
  @MainThread
  fun onAppShown(packageName: CharSequence?) {
    val shown = packageName?.toString().orEmpty()
    if (shown == app) return
    app = shown
    inWeb = false
    inEditing = false
    val context = appContext ?: return
    // Leaving SandFly's settings ends changing a profile's settings.
    if (editingId != null && shown != context.packageName) {
      executor.execute { finishEditingNow(context) }
    }
    if (watchesApps || watchesFocus) scheduleApply(context)
  }

  /** Whether focus changes are needed, to know whether focus is in web content or an edit field. */
  @JvmStatic fun watchesFocus(): Boolean = watchesFocus

  /** Notes the item that got accessibility focus. */
  @JvmStatic
  @MainThread
  fun onFocused(node: AccessibilityNodeInfoCompat?) {
    node ?: return
    val editable = node.isEditable
    val web = WebInterfaceUtils.supportsWebActions(node)
    // The keys of the keyboard that types in an edit field keep it.
    if (
      !editable &&
        !web &&
        (inEditing || inWeb) &&
        AccessibilityNodeInfoUtils.getWindowType(node) ==
          AccessibilityWindowInfoCompat.TYPE_INPUT_METHOD
    ) {
      return
    }
    if (editable == inEditing && web == inWeb) return
    inEditing = editable
    inWeb = web
    appContext?.let { scheduleApply(it) }
  }

  /** Notes that continuous reading started or stopped. */
  @JvmStatic
  fun onContinuousReading(active: Boolean) {
    if (active == inReading) return
    inReading = active
    if (watchesReading) appContext?.let { scheduleApply(it) }
  }

  /** Forgets where the user was, once SandFly stops, so that only the chosen profile applies. */
  @JvmStatic
  fun onServiceStopped(context: Context) {
    app = ""
    inWeb = false
    inEditing = false
    inReading = false
    scheduleApply(context.applicationContext)
  }

  private fun scheduleApply(context: Context) {
    if (!applyPending.compareAndSet(false, true)) return
    executor.execute {
      applyPending.set(false)
      applyNow(context)
    }
  }

  /** The IDs of the profiles to use now, first wins. */
  private fun wanted(context: Context, store: SettingsProfileStore): List<String> {
    editingId?.let { id ->
      return if (store.profile(id) != null) listOf(id) else emptyList()
    }
    val ids = mutableListOf<String>()
    store.manual()?.let { ids += it.id }
    if (app != context.packageName) {
      for (profile in store.profiles()) {
        if (profile.id !in ids && isTriggered(profile.triggers)) ids += profile.id
      }
    }
    return ids
  }

  private fun isTriggered(triggers: SettingsProfileStore.Triggers): Boolean =
    app in triggers.apps ||
      (triggers.web && inWeb) ||
      (triggers.reading && inReading) ||
      (triggers.editing && inEditing)

  /**
   * Applies the profiles to use now, unless they are the ones applied, or [force]. First saves each
   * setting changed since the last time to the profile it came from.
   */
  @VisibleForTesting
  @WorkerThread
  internal fun applyNow(context: Context, force: Boolean = false) {
    synchronized(lock) {
      val store = ready(context)
      val ids = wanted(context, store)
      if (!force && ids == appliedIds) return
      val applied = store.applied()
      val current = SettingsBackup.profileValues(context)

      // Settings changed since they were written belong to the profile they came from.
      val written = applied.written.toMutableMap()
      val changes = mutableMapOf<String, MutableMap<String, Any?>>()
      for ((key, value) in applied.written) {
        val now = current[key]
        if (now == value) continue
        written[key] = now
        val from = applied.from[key] ?: continue
        changes.getOrPut(from) { mutableMapOf() }[key] = now
      }
      for ((id, changed) in changes) {
        val settings = store.settings(id) ?: continue
        if (!store.saveSettings(id, settings + changed)) {
          LogUtils.e(TAG, "Cannot save the settings changed in a profile")
        }
      }

      val effective = LinkedHashMap<String, Pair<Any?, String>>()
      for (id in ids) {
        val settings = store.settings(id) ?: continue
        for ((key, value) in settings) {
          if (key in effective || !SettingsBackup.canSetProfileValue(context, key, value)) continue
          effective[key] = value to id
        }
      }

      val base = applied.base.toMutableMap()
      val writes = mutableMapOf<String, Any?>()
      for ((key, value) in written) {
        if (key in effective) continue
        // Back to the setting without profiles.
        val restored = base.remove(key)
        if (restored != value) writes[key] = restored
      }
      for ((key, entry) in effective) {
        val value = entry.first
        if (key in written) {
          if (written[key] != value) writes[key] = value
        } else {
          base[key] = current[key]
          if (current[key] != value) writes[key] = value
        }
      }
      val next =
        SettingsProfileStore.Applied(
          base.filterKeys { it in effective },
          effective.mapValues { it.value.first },
          effective.mapValues { it.value.second },
        )
      // Saved first, so that the settings written are always known to be a profile's.
      if (!store.saveApplied(next)) {
        LogUtils.e(TAG, "Cannot save the profiles applied")
        return
      }
      appliedIds = ids
      if (writes.isEmpty()) return
      writing = true
      try {
        SettingsBackup.writeProfileValues(context, writes)
      } finally {
        // The preference listeners run on the main thread before this.
        val keyboard = writes.keys.any { it.contains("keycombo") || it.contains("keymap") }
        mainHandler.post {
          writing = false
          TalkBackService.getInstance()?.onSettingsProfilesApplied(keyboard)
        }
      }
    }
  }

  /** The settings without profiles of the settings that profiles replace, by profile key. */
  @JvmStatic
  fun baseValues(context: Context): Map<String, Any?> =
    synchronized(lock) { store(context).applied().base }

  /**
   * Runs [work], which replaces the settings, such as importing a settings file. Its settings are
   * the settings without profiles, which are applied to them again.
   */
  @JvmStatic
  fun replacingSettings(context: Context, work: () -> Boolean): Boolean {
    val app = context.applicationContext
    val done =
      synchronized(lock) {
        val store = ready(app)
        val replaced = work()
        if (replaced) {
          store.saveApplied(SettingsProfileStore.Applied())
          store.saveEditing(null)
          editingId = null
        }
        replaced
      }
    if (done) executor.execute { applyNow(app, force = true) }
    return done
  }

  /**
   * Makes the profile [id], or none for an empty ID, the one used everywhere, and calls [onDone] on
   * the main thread with what to say.
   */
  @JvmStatic
  fun switchTo(context: Context, id: String, onDone: Consumer<String>) {
    inBackground(context, { setManualNow(it, id.ifEmpty { null }) }, onDone)
  }

  /** Uses the profile after the one used everywhere, as [switchTo] does. */
  @JvmStatic
  fun switchToNext(context: Context, onDone: Consumer<String>) {
    inBackground(context, { setManualNow(it, ready(it).nextManual()?.id) }, onDone)
  }

  /** Uses the profile before the one used everywhere, as [switchTo] does. */
  @JvmStatic
  fun switchToPrevious(context: Context, onDone: Consumer<String>) {
    inBackground(context, { setManualNow(it, ready(it).previousManual()?.id) }, onDone)
  }

  @VisibleForTesting
  @WorkerThread
  internal fun setManualNow(context: Context, id: String?): String {
    synchronized(lock) {
      val store = ready(context)
      val profile = id?.let { store.profile(it) }
      if ((id != null && profile == null) || !store.setManual(id)) {
        return context.getString(R.string.settings_profile_not_changed)
      }
      applyNow(context, force = true)
      return profile?.let {
        context.getString(R.string.settings_profile_in_use_announcement, it.name)
      } ?: context.getString(R.string.settings_profile_none)
    }
  }

  /** Adds a profile called [name] that sets nothing, and returns it, or null. */
  @WorkerThread
  fun add(context: Context, name: String): SettingsProfileStore.Profile? = ready(context).add(name)

  /** Deletes the profile [id], and stops using it. */
  @WorkerThread
  fun delete(context: Context, id: String): Boolean =
    synchronized(lock) {
      val store = ready(context)
      // Settings changed while it was applied are saved before it goes.
      applyNow(context, force = true)
      store.delete(id).also {
        updateWatches(store)
        applyNow(context, force = true)
      }
    }

  /** Sets when the profile [id] is used. */
  @WorkerThread
  fun setTriggers(context: Context, id: String, triggers: SettingsProfileStore.Triggers): Boolean =
    synchronized(lock) {
      val store = ready(context)
      store.setTriggers(id, triggers).also {
        updateWatches(store)
        applyNow(context, force = true)
      }
    }

  /** Moves the profile [id] up or down [by] places. */
  @WorkerThread
  fun move(context: Context, id: String, by: Int): Boolean =
    synchronized(lock) { ready(context).move(id, by).also { applyNow(context, force = true) } }

  /** The settings that the profile [id] sets, with those changed while it is applied. */
  @WorkerThread
  fun settingsOf(context: Context, id: String): Map<String, Any?>? =
    synchronized(lock) {
      applyNow(context, force = true)
      ready(context).settings(id)
    }

  /** Stops the profile [id] from setting [key]. */
  @WorkerThread
  fun removeSetting(context: Context, id: String, key: String): Boolean =
    synchronized(lock) {
      val store = ready(context)
      applyNow(context, force = true)
      val settings = store.settings(id) ?: return@synchronized false
      store.saveSettings(id, settings - key).also { applyNow(context, force = true) }
    }

  /**
   * Applies the profile [id] alone, and saves every setting changed until [finishEditing] to it.
   * Calls [onDone] on the main thread with what to say, or null when the profile is gone.
   */
  @JvmStatic
  fun startEditing(context: Context, id: String, onDone: Consumer<String?>) {
    inBackground(context, { startEditingNow(it, id) }, onDone)
  }

  @VisibleForTesting
  @WorkerThread
  internal fun startEditingNow(context: Context, id: String): String? {
    synchronized(lock) {
      finishEditingNow(context)
      val store = ready(context)
      val profile = store.profile(id) ?: return null
      editingId = id
      applyNow(context, force = true)
      // The settings with the profile applied: what changes from here goes to it.
      if (!store.saveEditing(id, SettingsBackup.profileValues(context))) {
        editingId = null
        applyNow(context, force = true)
        return context.getString(R.string.settings_profile_not_saved)
      }
      return context.getString(R.string.settings_profile_editing, profile.name)
    }
  }

  /**
   * Saves the settings changed since [startEditing] to the profile, and uses the profiles as before.
   * Calls [onDone] on the main thread with what to say, or null when no profile's settings were
   * being changed.
   */
  @JvmStatic
  fun finishEditing(context: Context, onDone: Consumer<String?>) {
    // Also when SandFly's process stopped while the settings were being changed.
    inBackground(context, { finishEditingNow(it) }, onDone)
  }

  @VisibleForTesting
  @WorkerThread
  internal fun finishEditingNow(context: Context): String? {
    synchronized(lock) {
      val store = ready(context)
      val (id, start) =
        store.editing()
          ?: run {
            if (editingId != null) {
              editingId = null
              applyNow(context, force = true)
            }
            return null
          }
      editingId = id
      val profile = store.profile(id)
      // Changes to the profile's own settings are saved to it as for any profile applied.
      applyNow(context, force = true)
      val applied = store.applied()
      val current = SettingsBackup.profileValues(context)
      val changed =
        (start.keys + current.keys).filter {
          it !in applied.written &&
            start[it] != current[it] &&
            SettingsBackup.isProfileKey(context, it)
        }
      val settings = store.settings(id)
      if (changed.isNotEmpty() && settings != null) {
        val values = changed.associateWith { current[it] }
        if (store.saveSettings(id, settings + values)) {
          // Without the profile, these settings are as they were at the start.
          store.saveApplied(
            SettingsProfileStore.Applied(
              applied.base + changed.associateWith { start[it] },
              applied.written + values,
              applied.from + changed.associateWith { id },
            )
          )
        } else {
          LogUtils.e(TAG, "Cannot save the settings changed to a profile")
        }
      }
      store.saveEditing(null)
      editingId = null
      applyNow(context, force = true)
      return profile?.let { context.getString(R.string.settings_profile_edit_saved, it.name) }
    }
  }

  /** Forgets the store and where the user is, so that the next use reads them again. */
  @VisibleForTesting
  internal fun reset() {
    store = null
    appliedIds = null
    editingId = null
    app = ""
    inWeb = false
    inEditing = false
    inReading = false
  }

  /** Sets where the user is, as the service would. */
  @VisibleForTesting
  internal fun setContext(
    app: String,
    web: Boolean = false,
    editing: Boolean = false,
    reading: Boolean = false,
  ) {
    this.app = app
    inWeb = web
    inEditing = editing
    inReading = reading
  }
}
