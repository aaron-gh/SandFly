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
package com.google.android.accessibility.talkback.recognition

import com.google.android.accessibility.talkback.actor.gemini.local.ModelStage
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressTonePlayer

/**
 * Plays the progress tones for Live recognition's requests to the on-device model or the AI
 * service in use, as Describe image does. Frames are described one after another, so after the
 * first answer the processing tones stop; the model loading tones still play whenever the model
 * loads. Main thread only.
 *
 * @param tones plays and stops the tones
 * @param modelLoaded whether the on-device model is most likely loaded; always true for a service
 */
internal class RecognitionTones(
  private val tones: Tones,
  private val modelLoaded: () -> Boolean,
) {
  /** Plays the progress tones. */
  interface Tones {
    fun play(modelLoading: Boolean)

    fun setModelLoading(modelLoading: Boolean)

    fun stop()
  }

  private var playing = false
  private var answered = false
  private var closed = false

  /** A frame is sent to be described. */
  fun onRequest() {
    if (closed) return
    val loading = !modelLoaded()
    if (loading || !answered) start(loading) else stopTones()
  }

  /** The on-device model starts loading, or starts on the frame. */
  fun onStage(stage: ModelStage) {
    if (closed) return
    when {
      stage == ModelStage.LOADING -> start(modelLoading = true)
      answered -> stopTones()
      else -> start(modelLoading = false)
    }
  }

  /** The request for a frame ended, with an answer or without. */
  fun onRequestEnd(gotAnswer: Boolean) {
    stopTones()
    if (gotAnswer) answered = true
  }

  /** Stops the tones for good. */
  fun close() {
    closed = true
    stopTones()
  }

  private fun start(modelLoading: Boolean) {
    if (playing) {
      tones.setModelLoading(modelLoading)
    } else {
      playing = true
      tones.play(modelLoading)
    }
  }

  private fun stopTones() {
    if (!playing) return
    playing = false
    tones.stop()
  }

  /** Plays [Tones] with a [ProgressTonePlayer]. */
  class PlayerTones(private val player: ProgressTonePlayer) : Tones {
    override fun play(modelLoading: Boolean) =
      player.play(/* playTone= */ true, /* delayPlayTone= */ false, modelLoading)

    override fun setModelLoading(modelLoading: Boolean) = player.setModelLoading(modelLoading)

    override fun stop() = player.stop()
  }
}
