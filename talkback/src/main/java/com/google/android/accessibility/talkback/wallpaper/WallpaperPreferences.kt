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

package com.google.android.accessibility.talkback.wallpaper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R

/** Opens Android's screen for setting SandFly as the wallpaper, and says whether it is. */
class SandFlyWallpaperPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
  AccessibilitySuitePreference(context, attrs) {

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    // Written to the view, not set as the summary, which would change the list while it binds.
    val summaryView = holder.findViewById(android.R.id.summary) as? TextView ?: return
    if (SandFlyWallpaperService.isCurrent(context)) {
      summaryView.text = context.getString(R.string.summary_pref_sandfly_wallpaper_set)
      summaryView.visibility = View.VISIBLE
    } else {
      summaryView.visibility = View.GONE
    }
  }

  override fun onClick() {
    try {
      context.startActivity(
        SandFlyWallpaperService.intentToSet(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } catch (e: ActivityNotFoundException) {
      // Some phones have no live wallpapers.
    }
  }
}

/** Opens the image picker for the image that SandFly's wallpaper shows. */
class WallpaperImagePreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
  AccessibilitySuitePreference(context, attrs) {

  override fun onClick() {
    context.startActivity(Intent(context, WallpaperImageActivity::class.java))
  }
}
