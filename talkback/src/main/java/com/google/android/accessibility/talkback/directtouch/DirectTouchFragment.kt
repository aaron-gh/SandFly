/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.directtouch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.concurrent.Executors

/**
 * Settings for direct touch: the master switch, what Backtalk reports when it changes, the apps
 * that get raw touch, and backup and restore. Each app has an action that turns direct typing on
 * or off for it, which screen reader users reach from the actions menu.
 */
class DirectTouchFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences
  private var appsCategory: PreferenceCategory? = null

  private val createBackup =
    registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        writeBackup(uri)
      }
    }

  private val openBackup =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        readBackup(uri)
      }
    }

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_direct_touch)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    prefs = SharedPreferencesUtils.getSharedPreferences(requireContext())
    buildScreen()
  }

  private fun buildScreen() {
    val context = requireContext()
    val screen = preferenceManager.createPreferenceScreen(context)
    screen.addPreference(
      switchPreference(
        context,
        DirectTouchSettings.PREF_MASTER,
        R.string.pref_direct_touch_master,
        R.string.pref_direct_touch_master_summary,
        DirectTouchSettings.isMasterEnabled(prefs),
      )
    )
    screen.addPreference(
      switchPreference(
        context,
        DirectTouchSettings.PREF_SPEECH,
        R.string.pref_direct_touch_speech,
        null,
        DirectTouchSettings.isSpeechEnabled(prefs),
      )
    )
    screen.addPreference(
      switchPreference(
        context,
        DirectTouchSettings.PREF_HAPTICS,
        R.string.pref_direct_touch_haptics,
        null,
        DirectTouchSettings.isHapticsEnabled(prefs),
      )
    )
    screen.addPreference(
      actionPreference(context, R.string.pref_direct_touch_backup) {
        createBackup.launch(BACKUP_FILE_NAME)
      }
    )
    screen.addPreference(
      actionPreference(context, R.string.pref_direct_touch_restore) {
        openBackup.launch(arrayOf("application/json", "text/plain"))
      }
    )
    val category =
      PreferenceCategory(context).apply {
        setTitle(R.string.direct_touch_apps_category)
        isIconSpaceReserved = false
      }
    screen.addPreference(category)
    appsCategory = category
    preferenceScreen = screen
    loadApps(context)
  }

  private fun switchPreference(
    context: Context,
    key: String,
    title: Int,
    summary: Int?,
    checked: Boolean,
  ) =
    CheckBoxPreference(context).apply {
      this.key = key
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      setTitle(title)
      summary?.let { setSummary(it) }
      isChecked = checked
      setOnPreferenceChangeListener { _, newValue ->
        prefs.edit().putBoolean(key, newValue as Boolean).apply()
        true
      }
    }

  private fun actionPreference(context: Context, title: Int, onClick: () -> Unit) =
    Preference(context).apply {
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      setTitle(title)
      setOnPreferenceClickListener {
        onClick()
        true
      }
    }

  /** Lists the apps on a background thread, because loading every label is slow. */
  private fun loadApps(context: Context) {
    val appContext = context.applicationContext
    executor.execute {
      val pm = appContext.packageManager
      val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      // Keyboards too, which often have no launcher icon, for those that speak for themselves.
      val keyboards =
        appContext.getSystemService(InputMethodManager::class.java)?.enabledInputMethodList.orEmpty()
      val entries =
        (pm.queryIntentActivities(launcher, 0).map {
            it.activityInfo.packageName to it.loadLabel(pm).toString()
          } + keyboards.map { it.packageName to it.loadLabel(pm).toString() })
          .distinctBy { it.first }
          .filter { (pkg, _) -> pkg != appContext.packageName }
          .map { (pkg, label) -> AppEntry(pkg, label, DirectTouchSettings.isAppEnabled(prefs, pkg)) }
      val sorted = DirectTouchApps.sort(entries)
      view?.post {
        val category = appsCategory ?: return@post
        if (!isAdded) {
          return@post
        }
        category.removeAll()
        sorted.forEachIndexed { index, entry ->
          category.addPreference(AppPreference(requireContext(), entry).apply { order = index })
        }
      }
    }
  }

  private fun writeBackup(uri: Uri) {
    val context = requireContext()
    val written =
      try {
        context.contentResolver.openOutputStream(uri)?.use {
          it.write(DirectTouchSettings.exportJson(prefs).toByteArray())
        } != null
      } catch (_: java.io.IOException) {
        false
      }
    toast(if (written) R.string.direct_touch_backup_done else R.string.direct_touch_backup_failed)
  }

  private fun readBackup(uri: Uri) {
    val context = requireContext()
    val json =
      try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
      } catch (_: java.io.IOException) {
        null
      }
    if (json != null && DirectTouchSettings.importJson(prefs, json)) {
      toast(R.string.direct_touch_restore_done)
      buildScreen()
    } else {
      toast(R.string.direct_touch_restore_failed)
    }
  }

  private fun toast(message: Int) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
  }

  private inner class AppPreference(context: Context, private val entry: AppEntry) :
    CheckBoxPreference(context) {
    init {
      key = entry.packageName
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      title = entry.label
      isChecked = entry.enabled
      updateSummary()
      setOnPreferenceChangeListener { _, newValue ->
        DirectTouchSettings.setAppEnabled(prefs, entry.packageName, newValue as Boolean)
        true
      }
    }

    private fun updateSummary() {
      summary =
        if (DirectTouchSettings.isDirectTyping(prefs, entry.packageName)) {
          context.getString(R.string.direct_touch_typing_summary_on)
        } else {
          null
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other apps, so every bind replaces the action.
      val typing = DirectTouchSettings.isDirectTyping(prefs, entry.packageName)
      val label =
        context.getString(
          if (typing) R.string.direct_touch_action_typing_off
          else R.string.direct_touch_action_typing_on
        )
      ViewCompat.replaceAccessibilityAction(
        holder.itemView,
        AccessibilityActionCompat(ACTION_TYPING, label),
        label,
        AccessibilityViewCommand { _, _ -> toggleTyping() },
      )
    }

    private fun toggleTyping(): Boolean {
      val typing = DirectTouchSettings.isDirectTyping(prefs, entry.packageName)
      DirectTouchSettings.setDirectTyping(prefs, entry.packageName, !typing)
      updateSummary()
      notifyChanged()
      return true
    }
  }

  private companion object {
    const val BACKUP_FILE_NAME = "backtalk-direct-touch.json"
    val ACTION_TYPING = R.id.accessibility_custom_action_0
    val executor = Executors.newSingleThreadExecutor()
  }
}
