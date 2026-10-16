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

package com.google.android.accessibility.talkback.individualfeedback

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.google.android.accessibility.talkback.soundthemes.SoundThemes
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import java.io.IOException

/** Plays one sound at a time on settings screens, the sound of the theme in use if it has one. */
class SoundPreview {
  private var player: MediaPlayer? = null

  fun play(context: Context, prefs: SharedPreferences, item: FeedbackItem) {
    stop()
    val attributes =
      AudioAttributes.Builder()
        .setUsage(
          if (FailoverTextToSpeech.shouldUseAccessibilityStream(context)) {
            AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
          } else {
            AudioAttributes.USAGE_MEDIA
          }
        )
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val custom = SoundThemes.soundFile(context, prefs, item)
    if (custom == null) {
      // The braille keyboard's typing sounds are Android's keyboard sounds unless a theme has them.
      SoundThemes.BRAILLE_TYPING_EFFECTS[item.key]?.let { effect ->
        context.getSystemService(AudioManager::class.java)?.playSoundEffect(effect, PREVIEW_VOLUME)
        return
      }
    }
    player =
      if (custom != null) {
        try {
          MediaPlayer().apply {
            setAudioAttributes(attributes)
            setDataSource(custom.path)
            prepare()
          }
        } catch (e: IOException) {
          null
        } catch (e: RuntimeException) {
          null
        }
      } else {
        // By name, because the braille sounds are in a module whose R class talkback cannot see.
        val resId =
          context.resources.getIdentifier(item.resourceNames.first(), "raw", context.packageName)
        if (resId == 0) null else MediaPlayer.create(context, resId, attributes, 0)
      }
    player?.apply {
      setOnCompletionListener {
        if (player === it) {
          player = null
        }
        it.release()
      }
      start()
    }
  }

  private companion object {
    // As loud as the braille keyboard plays them.
    const val PREVIEW_VOLUME = 0.5f
  }

  fun stop() {
    player?.release()
    player = null
  }
}
