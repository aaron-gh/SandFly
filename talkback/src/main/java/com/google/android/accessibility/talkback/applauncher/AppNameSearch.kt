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
package com.google.android.accessibility.talkback.applauncher

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.overview.RecentTasks
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.text.Normalizer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Finds the apps on the home screen whose name starts with letters typed or written, says them,
 * and opens the one chosen. The surfaces that take the letters, such as handwriting and braille, use it.
 */
class AppNameSearch(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  /** Spoken when there are no letters yet. */
  @StringRes private val promptRes: Int,
  /** Closes the surface before an app opens. */
  private val onOpen: () -> Unit,
) {
  private val handler = Handler(Looper.getMainLooper())

  /** The apps on the home screen, read when the surface shows. */
  private var apps: List<OpenAppActions.App> = emptyList()
  /**
   * Whether [apps] is loaded. Loading reads every launcher app's label, which takes up to seconds
   * and is done off the main thread; letters that come before then wait in [waiting].
   */
  private var loaded = false
  private val waiting = ArrayDeque<() -> Unit>()
  private var loads = 0
  private val loader =
    ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()) {
      Thread(it, "app-name-search").apply { isDaemon = true }
    }

  /** The letters so far, the apps that match them, and the one said last. */
  var letters = ""
    private set
  private var matches: List<OpenAppActions.App> = emptyList()
  private var index = 0

  /** Whether an app is said, which opening would open. */
  val hasApp: Boolean
    get() = matches.getOrNull(index) != null

  /** Forgets the letters and reads the apps again, when the surface shows. */
  fun start() {
    letters = ""
    matches = emptyList()
    index = 0
    val load = ++loads
    loaded = false
    waiting.clear()
    loader.execute {
      val list =
        try {
          OpenAppActions.apps(service)
        } catch (e: Throwable) {
          LogUtils.e(TAG, "Could not list the apps: %s", e)
          emptyList()
        }
      handler.post {
        if (load != loads) return@post
        apps = list
        loaded = true
        while (loaded && waiting.isNotEmpty()) waiting.removeFirst()()
      }
    }
  }

  /** Forgets the apps and letters, when the surface closes. */
  fun stop() {
    loads++
    loaded = false
    waiting.clear()
    apps = emptyList()
    matches = emptyList()
  }

  fun shutdown() {
    stop()
    loader.shutdown()
  }

  /** Runs [action] once the apps are loaded: now, or when they are, in order. */
  fun whenLoaded(action: () -> Unit) {
    if (loaded) action() else if (waiting.size < MAX_WAITING) waiting.addLast(action)
  }

  /**
   * Takes the first of [candidates], whole letters from the start, that some app's name starts
   * with, and says the first app, after [echo] when there is one. Opening is left to the user, even
   * when only one app matches. Returns false, and says "No apps", when no app's name starts with any
   * of them.
   */
  fun find(candidates: List<String>, echo: String? = null): Boolean {
    for (candidate in candidates) {
      val found = matching(apps, candidate)
      if (found.isEmpty()) continue
      letters = candidate
      matches = found
      index = 0
      if (echo != null) speak(echo)
      sayApp(queue = echo != null)
      return true
    }
    speak(service.getString(R.string.app_search_no_apps))
    return false
  }

  /** Sets the letters to fewer, such as after a delete, and the apps that match them. */
  fun shorten(text: String) {
    letters = text
    matches = matching(apps, text)
    index = 0
  }

  fun nextApp(forward: Boolean = true) {
    if (matches.size < 2) {
      speak(service.getString(R.string.handwriting_no_alternatives))
      return
    }
    index = (index + (if (forward) 1 else matches.size - 1)) % matches.size
    sayApp()
  }

  /** Says the app that opening would open, or that there is none yet. */
  fun sayApp(queue: Boolean = false) {
    val app = matches.getOrNull(index)
    val text =
      when {
        app != null -> service.getString(R.string.handwriting_alternative, app.label, index + 1, matches.size)
        normalize(letters).isEmpty() -> service.getString(promptRes)
        else -> service.getString(R.string.app_search_no_apps)
      }
    speak(text, queue)
  }

  fun open() {
    val app = matches.getOrNull(index)
    if (app == null) {
      sayApp()
      return
    }
    onOpen()
    // The app's window says its name when it opens.
    if (!OpenAppActions.open(service, app.value)) {
      speak(service.getString(R.string.handwriting_app_not_opened, app.label))
    }
  }

  /** Opens the app said in split screen beside the most recent other app. */
  fun openInSplit() {
    val app = matches.getOrNull(index)
    if (app == null) {
      sayApp()
      return
    }
    onOpen()
    openInSplit(service, pipeline, app)
  }

  private fun speak(text: String, queue: Boolean = false) {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        speakOptions(if (queue) SpeechController.QUEUE_MODE_QUEUE else SpeechController.QUEUE_MODE_INTERRUPT),
      ),
    )
  }

  companion object {
    private const val TAG = "AppNameSearch"
    private const val MAX_WAITING = 100

    /** Opens [app] in split screen beside the most recent other app, or says why it can't. */
    @JvmStatic
    fun openInSplit(
      service: AccessibilityService,
      pipeline: Pipeline.FeedbackReturner,
      app: OpenAppActions.App,
    ) {
      val say = { text: String ->
        pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(text, speakOptions(SpeechController.QUEUE_MODE_INTERRUPT)),
        )
      }
      if (!RecentTasks.isAvailable(service)) {
        say(service.getString(R.string.split_screen_needs_shizuku))
        return
      }
      RecentTasks.openBeside(service, app) { worked ->
        if (!worked) say(service.getString(R.string.overview_split_failed))
      }
    }

    fun speakOptions(queueMode: Int): SpeakOptions =
      SpeakOptions.create()
        .setQueueMode(queueMode)
        .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)

    /**
     * The apps whose name starts with [letters], and then those with a later word that does, such as
     * Google Maps for "ma". Case, spaces and accents don't count.
     */
    fun matching(apps: List<OpenAppActions.App>, letters: String): List<OpenAppActions.App> {
      val key = normalize(letters)
      if (key.isEmpty()) return emptyList()
      val (starts, others) = apps.partition { normalize(it.label).startsWith(key) }
      val words =
        others.filter { app ->
          app.label.split(' ', '-', '_', '.').drop(1).any { normalize(it).startsWith(key) }
        }
      return starts + words
    }

    /** [text] without case, accents, spaces or punctuation, as names are compared. */
    fun normalize(text: String): String =
      Normalizer.normalize(text, Normalizer.Form.NFD).filter { it.isLetterOrDigit() }.lowercase()
  }
}
