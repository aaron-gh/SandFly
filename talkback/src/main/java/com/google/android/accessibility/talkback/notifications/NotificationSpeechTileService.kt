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

package com.google.android.accessibility.talkback.notifications

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** A quick settings tile that turns notification speech on and off. */
class NotificationSpeechTileService : TileService() {
  override fun onStartListening() {
    update()
  }

  override fun onClick() {
    NotificationReader.setSpeechOn(this, null, announce = false)
    update()
  }

  private fun update() {
    val tile = qsTile ?: return
    val on =
      SharedPreferencesUtils.getSharedPreferences(this)
        .getBoolean(NotificationSettings.Companion.Keys.ENABLED, true)
    tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.updateTile()
  }
}
