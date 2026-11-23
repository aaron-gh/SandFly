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

package com.google.android.accessibility.talkback.doubletaptaps

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.directtouch.AppEntry
import com.google.android.accessibility.talkback.directtouch.DirectTouchApps
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.concurrent.Executors

/** The apps with a check box each, for Double tap taps the screen. */
class DoubleTapTapsFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_double_tap_taps)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    preferenceScreen = preferenceManager.createPreferenceScreen(context)
    loadApps(context)
  }

  /** Lists the apps on a background thread, because loading every label is slow. */
  private fun loadApps(context: Context) {
    val appContext = context.applicationContext
    executor.execute {
      val pm = appContext.packageManager
      val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      val entries =
        pm.queryIntentActivities(launcher, 0)
          .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
          .distinctBy { it.first }
          .filter { (pkg, _) -> pkg != appContext.packageName }
          .map { (pkg, label) -> AppEntry(pkg, label, DoubleTapTapsApps.isOn(prefs, pkg)) }
      val sorted = DirectTouchApps.sort(entries)
      view?.post {
        if (!isAdded) {
          return@post
        }
        val screen = preferenceScreen ?: return@post
        screen.removeAll()
        sorted.forEachIndexed { index, entry ->
          screen.addPreference(appPreference(requireContext(), entry).apply { order = index })
        }
      }
    }
  }

  private fun appPreference(context: Context, entry: AppEntry) =
    CheckBoxPreference(context).apply {
      key = entry.packageName
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      title = entry.label
      isChecked = entry.enabled
      setOnPreferenceChangeListener { _, newValue ->
        DoubleTapTapsApps.setOn(prefs, entry.packageName, newValue as Boolean)
        true
      }
    }

  private companion object {
    val executor = Executors.newSingleThreadExecutor()
  }
}
