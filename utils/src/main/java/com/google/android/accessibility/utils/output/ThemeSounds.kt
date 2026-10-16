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

package com.google.android.accessibility.utils.output

import android.media.AudioAttributes
import android.media.SoundPool

/**
 * The sounds of the sound theme in use, by name, for the sounds that are played outside
 * [FeedbackController], such as the braille keyboard's typing sounds. They are loaded as soon as
 * the theme is set, so that the first key typed plays at once.
 */
object ThemeSounds {
  private const val MAX_STREAMS = 4

  private val lock = Any()
  private var paths: Map<String, String> = emptyMap()
  private var pool: SoundPool? = null
  // How the pool plays, so that it is made again when sounds change volume.
  private var poolAttributes: AudioAttributes? = null
  // SoundPool sound IDs, by file path.
  private val soundIds = HashMap<String, Int>()

  /** Sets the theme's sound files, by name, and loads them. */
  @JvmStatic
  fun set(pathsByName: Map<String, String>) {
    synchronized(lock) {
      val attributes = FeedbackController.feedbackAttributes()
      if (pathsByName == paths && attributes == poolAttributes) return
      paths = HashMap(pathsByName)
      poolAttributes = attributes
      pool?.release()
      pool = null
      soundIds.clear()
      if (paths.isEmpty()) return
      val newPool =
        SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attributes).build()
      for (path in paths.values.toSet()) {
        soundIds[path] = newPool.load(path, 1)
      }
      pool = newPool
    }
  }

  /**
   * Plays the theme's sound for [name] at [volume], from 0 to 1, and returns true, or returns
   * false if the theme leaves it as it is.
   */
  @JvmStatic
  fun play(name: String, volume: Float): Boolean {
    synchronized(lock) {
      val path = paths[name] ?: return false
      val soundId = soundIds[path] ?: return false
      // A sound still loading is skipped rather than played late.
      pool?.play(soundId, volume, volume, 1, 0, 1f)
      return true
    }
  }
}
