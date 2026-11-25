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

package com.google.android.accessibility.talkback.soundthemes

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Plays one sound file of another app's folder at a time. The files are opened and prepared on a
 * thread of its own, since another app's provider can be slow, and every player is used and
 * released only on that thread.
 */
class FolderSoundPlayer(context: Context) {
  private val context = context.applicationContext
  private val thread: ExecutorService = Executors.newSingleThreadExecutor()
  private val attributes =
    AudioAttributes.Builder()
      .setUsage(FailoverTextToSpeech.speechUsage(this.context))
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

  /** The URI being played, or the last one asked for. Read and written on the main thread. */
  var playing: Uri? = null
    private set

  // Only used on [thread].
  private var player: MediaPlayer? = null

  // Counts requests, so that a file that finishes preparing after another request doesn't play.
  @Volatile private var request = 0

  /** Stops what is playing and plays [uri]. */
  fun play(uri: Uri) {
    playing = uri
    val current = ++request
    onThread {
      releasePlayer()
      if (current != request) return@onThread
      val created = MediaPlayer()
      try {
        created.setAudioAttributes(attributes)
        created.setDataSource(context, uri)
        created.prepare()
      } catch (e: IOException) {
        created.release()
        return@onThread
      } catch (e: RuntimeException) {
        // IllegalStateException, SecurityException or anything else another app's provider throws.
        created.release()
        return@onThread
      }
      if (current != request) {
        created.release()
        return@onThread
      }
      created.setOnCompletionListener { finished -> onThread { if (player === finished) releasePlayer() } }
      player = created
      try {
        created.start()
      } catch (e: IllegalStateException) {
        releasePlayer()
      }
    }
  }

  /** Stops what is playing. */
  fun stop() {
    playing = null
    request++
    onThread { releasePlayer() }
  }

  /** Stops what is playing and ends the thread. The player can't be used again. */
  fun shutdown() {
    stop()
    thread.shutdown()
  }

  private fun releasePlayer() {
    player?.release()
    player = null
  }

  private fun onThread(task: () -> Unit) {
    try {
      thread.execute(task)
    } catch (e: RejectedExecutionException) {
      // Shut down; nothing is playing any more.
    }
  }
}
