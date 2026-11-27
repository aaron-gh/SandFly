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

package com.google.android.accessibility.talkback.trainer

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.BackTapDetector
import com.google.android.accessibility.talkback.gesture.GestureLayout
import com.google.android.accessibility.talkback.gesture.ShakeDetector
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.talkback.gesture.CornerTapTracker.Corner
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.Edge
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
import com.google.android.accessibility.talkback.gesture.GestureLog
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity.TalkBackSubSettings
import com.google.android.accessibility.talkback.preference.base.TalkBackGestureShortcutPreferenceFragment
import com.google.android.accessibility.talkback.preference.search.SettingsSearch
import com.google.android.accessibility.talkback.tutorial.TutorialHooks
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.gestures.GestureConfiguration
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.preference.BasePreferencesActivity.FRAGMENT_ARGS
import com.google.android.accessibility.utils.preference.BasePreferencesActivity.FRAGMENT_NAME

/**
 * Teaches gestures: the user picks a group, and for each gesture the trainer explains how to make
 * it, takes [TRIES] tries, says after each whether SandFly recognized it, and then shows what the
 * fingers did against the limits SandFly allows, with what to change and settings that can help.
 *
 * While tries are taken, every touch on the screen is a try, and the gestures made do nothing else.
 */
class GestureTrainerActivity : AppCompatActivity() {
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var content: LinearLayout
  private lateinit var scroll: ScrollView

  private var group: TrainerGroup? = null

  /** The screens of the trainer, besides the tries. */
  private enum class Screen {
    GROUPS,
    EXERCISE,
    RESULTS,
    CONTENTS,
    GROUP_DONE,
    TRIES,
  }

  private var screen = Screen.GROUPS

  /** Counts the screens shown, so that a callback can tell whether its screen is still shown. */
  private var screenCount = 0

  /** Whether a settings screen was opened from here, whose changes the trainer must show. */
  private var settingsOpened = false

  private fun enterScreen(next: Screen) {
    screen = next
    screenCount++
  }
  private var exercises: List<Exercise> = emptyList()
  private var leftOut: List<String> = emptyList()
  private var index = 0
  private val reports = ArrayList<AttemptReport>()
  private lateinit var limits: TrainerLimits
  private lateinit var analyzer: AttemptAnalyzer

  private var takingTries = false
  private var recorder = TouchRecorder()
  private val recognized = ArrayList<Int>()
  private var lastRecognizedMs = -1L
  private var menuChoice: Int? = null
  private val endTry = Runnable { endTry() }

  // A back-tap try: the jolts the accelerometer felt, the taps of the run that ended, and whether
  // the screen was touched, which fails the try.
  private val jolts = ArrayList<BackTapDetector.Jolt>()
  private var backTapRun: Int? = null
  private var screenTouched = false
  private val backTaps =
    BackTapPractice(
      this,
      object : BackTapPractice.Listener {
        override fun onJolt(jolt: BackTapDetector.Jolt) {
          // Ringing after a run belongs to that run, not to the next try.
          if (!takingTries || jolt.rejection == BackTapDetector.Rejection.JUST_AFTER_RUN) return
          jolts += jolt
          // A run ends by itself once its taps stop; jolts that are no taps end the try here.
          handler.removeCallbacks(endTry)
          handler.postDelayed(endTry, BACK_TAP_QUIET_MS)
        }

        override fun onRunEnded(count: Int) {
          if (!takingTries) return
          backTapRun = count
          handler.removeCallbacks(endTry)
          handler.postDelayed(endTry, SETTLE_MS)
        }
      },
    )

  private val practisesBackTaps: Boolean
    get() = exercise?.shape is Shape.BackTaps

  // A shake try: the strongest movement, the most swings that counted toward a shake at once, and
  // whether the shake was recognized.
  private var shakeStrongest = 0f
  private var shakeMostSwings = 0
  private var shaken = false
  private val shakes =
    ShakePractice(
      this,
      object : ShakePractice.Listener {
        override fun onMovement(strongest: Float) {
          if (!takingTries || shaken) return
          shakeStrongest = maxOf(shakeStrongest, strongest)
          handler.removeCallbacks(endTry)
          handler.postDelayed(endTry, SHAKE_QUIET_MS)
        }

        override fun onSwing(swings: Int) {
          if (!takingTries || shaken) return
          shakeMostSwings = maxOf(shakeMostSwings, swings)
        }

        override fun onShake() {
          if (!takingTries) return
          shaken = true
          shakeMostSwings = maxOf(shakeMostSwings, ShakeDetector.SWINGS)
          handler.removeCallbacks(endTry)
          handler.postDelayed(endTry, SETTLE_MS)
        }
      },
    )

  private val practisesShake: Boolean
    get() = exercise?.shape == Shape.Shake

  /** How many tries of each exercise of the group were recognized, by exercise, last time. */
  private val groupResults = HashMap<Int, Int>()

  private val exercise: Exercise?
    get() = exercises.getOrNull(index)

  private val hooks =
    object : GestureTrainerHooks.Listener {
      override val practisedGestureId: Int
        get() = exercise?.gestureId ?: 0

      override fun onTouch(event: MotionEvent) {
        if (event.downTime < triesStartMs) return
        if (practisesBackTaps) {
          onScreenTouchedInBackTaps(event)
          return
        }
        // A shake is felt, not touched: touches meanwhile do nothing.
        if (practisesShake) return
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
          handler.removeCallbacks(endTry)
          // A gesture already recognized ends the try: this touch starts the next one. Taps that
          // come too slowly stay one try, so that it can say they were too slow.
          val ended = (recognized.isNotEmpty() || tryHasEnded()) && !waitsForRestOfGesture()
          if (ended || menuChoice != null) endTry()
        }
        if (!takingTries) return
        recorder.onMotionEvent(event)
        if (!recorder.fingersDown) {
          handler.postDelayed(endTry, if (lastTouchIsRecognized()) settleMs() else waitForGestureMs())
        }
      }

      override fun onTouchInteractionEnded(timeMs: Long, explored: Boolean) {
        if (practisesBackTaps || practisesShake || !recorder.fingersDown) return
        recorder.endUnseenTouch(timeMs, explored)
        handler.removeCallbacks(endTry)
        val known = recognized.isNotEmpty() || menuChoice != null
        handler.postDelayed(endTry, if (known) SETTLE_MS else waitForGestureMs())
      }

      override fun onGesture(gestureId: Int): Boolean {
        // After the tries, a double tap activates the trainer's buttons, while the listener stays
        // for gestures of the last try that are recognized late.
        if (!takingTries && isActivation(gestureId)) return false
        // A gesture recognized late, after its try ended, belongs to no try: it is still taken.
        if (recorder.touches.isEmpty()) return true
        recognized += gestureId
        lastRecognizedMs = android.os.SystemClock.uptimeMillis()
        if (!recorder.fingersDown) {
          handler.removeCallbacks(endTry)
          handler.postDelayed(endTry, settleMs())
        }
        return true
      }

      override fun practiceMenuItems(edge: MenuEdge): List<String>? {
        val shape = exercise?.shape as? Shape.EdgeMenu ?: return null
        return if (shape.sample && shape.edge == edge) shape.names else null
      }

      override fun onEdgeMenuChoice(edge: MenuEdge, item: Int) {
        menuChoice = item
        if (!recorder.fingersDown) {
          handler.removeCallbacks(endTry)
          handler.postDelayed(endTry, SETTLE_MS)
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTitle(R.string.gesture_trainer_title)
    content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    scroll = ScrollView(this).apply { addView(content) }
    ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      WindowInsetsCompat.CONSUMED
    }
    setContentView(scroll)
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          when {
            takingTries -> showExercise()
            group != null -> showGroups()
            else -> finish()
          }
        }
      },
    )
    readLimits()
    showGroups()
  }

  private fun isActivation(gestureId: Int): Boolean =
    gestureId == AccessibilityService.GESTURE_DOUBLE_TAP ||
      gestureId == AccessibilityService.GESTURE_DOUBLE_TAP_AND_HOLD

  /** Whether the trainer is in the foreground, between onResume and onPause. */
  private var resumed = false

  /**
   * Tries are taken only while the trainer's window has focus: the notification shade, a call or a
   * system dialog over it leaves it resumed, and gestures there must work.
   */
  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      if (takingTries) listen()
    } else if (takingTries) {
      stopListening()
      // Touches of a try left open never end it: the next try starts afresh.
      newTry()
    }
  }

  override fun onResume() {
    super.onResume()
    resumed = true
    readLimits()
    if (takingTries) {
      listen()
    } else if (settingsOpened) {
      settingsOpened = false
      showWithChangedSettings()
    }
  }

  override fun onPause() {
    super.onPause()
    resumed = false
    stopListening()
    // Touches of a try left open never end it: the next try starts afresh.
    if (takingTries) newTry()
  }

  /**
   * Shows the screen again after a settings screen opened from it, as its texts, and the group's
   * exercises, may have changed: turning on two-stroke swipes brings their exercises.
   */
  private fun showWithChangedSettings() {
    val group = group
    if (group != null) {
      val list = TrainerExercises.of(this, group)
      leftOut = list.leftOut
      // Edge menu items are drawn anew each time: the same exercises keep the items drawn before.
      if (list.exercises.map { it.gestureId } != exercises.map { it.gestureId }) {
        exercises = list.exercises
        groupResults.clear()
        if (index >= exercises.size) index = 0
        if (screen == Screen.RESULTS) return showExercise()
      }
    }
    when (screen) {
      Screen.GROUPS -> showGroups()
      Screen.EXERCISE,
      Screen.GROUP_DONE,
      Screen.TRIES -> showExercise()
      Screen.RESULTS -> showResults()
      Screen.CONTENTS -> showContents()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    stopListening()
    handler.removeCallbacksAndMessages(null)
  }

  /**
   * Folding, unfolding or rotating changes the screen size, which the limits of halves, corners,
   * edges and edge menus depend on. The exercise's text gives sizes in mm, so it is shown again.
   */
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    val before = limits
    readLimits()
    val changed =
      before.screenWidth != limits.screenWidth ||
        before.screenHeight != limits.screenHeight ||
        before.pxPerCm != limits.pxPerCm
    if (changed && screen == Screen.EXERCISE) showExercise()
  }

  /** Reads the limits again, as settings may have changed in a settings screen opened from here. */
  private fun readLimits() {
    val size = Point()
    @Suppress("DEPRECATION") val shownDisplay = windowManager.defaultDisplay
    shownDisplay.getRealSize(size)
    limits = TrainerLimits.read(this, size.x, size.y, shownDisplay.rotation)
    analyzer = AttemptAnalyzer(limits)
  }

  // Groups

  private fun showGroups() {
    stopTries()
    enterScreen(Screen.GROUPS)
    group = null
    content.removeAllViews()
    val title = heading(getString(R.string.gesture_trainer_title))
    content.addView(title)
    if (!FeatureSupport.supportGestureDetection()) {
      content.addView(text(getString(R.string.gesture_trainer_no_detection)))
    } else {
      content.addView(text(getString(R.string.gesture_trainer_intro, TRIES)))
      for (group in TrainerGroup.entries) {
        val list = TrainerExercises.of(this, group)
        content.addView(
          button(getString(R.string.gesture_trainer_group_count, getString(group.title), list.exercises.size)) {
            openGroup(group)
          }
        )
      }
    }
    content.addView(button(getString(R.string.gesture_trainer_close)) { finish() })
    showTop(title)
  }

  private fun openGroup(group: TrainerGroup) {
    val list = TrainerExercises.of(this, group)
    this.group = group
    exercises = list.exercises
    leftOut = list.leftOut
    index = 0
    groupResults.clear()
    showExercise()
  }

  // Exercise

  /** Shows how to make the exercise's gesture, with a button that starts the tries. */
  private fun showExercise() {
    stopTries()
    val exercise = exercise ?: return showGroupDone()
    enterScreen(Screen.EXERCISE)
    content.removeAllViews()
    val title =
      heading(
        getString(R.string.gesture_trainer_exercise_heading, index + 1, exercises.size, exerciseName(exercise))
      )
    content.addView(title)
    if (index == 0) leftOut.forEach { content.addView(text(it)) }
    content.addView(text(howTo(exercise)))
    if (exercise.shape is Shape.CornerTaps || exercise.shape is Shape.EdgeSlide) {
      content.addView(text(sizesText()))
      if (exercise.shape is Shape.CornerTaps) {
        content.addView(text(getString(R.string.gesture_trainer_corner_warning)))
      }
    }
    if (exercise.shape is Shape.BackTaps) {
      val key = R.string.pref_back_tap_sensitivity_key
      content.addView(text(getString(R.string.gesture_trainer_back_tap_info, settingTitle(key))))
      content.addView(
        button(getString(R.string.gesture_trainer_open_setting, settingTitle(key))) { openSetting(key) }
      )
    }
    if (exercise.shape == Shape.Shake) {
      val key = R.string.pref_shake_sensitivity_key
      content.addView(text(getString(R.string.gesture_trainer_shake_info, settingTitle(key))))
      content.addView(
        button(getString(R.string.gesture_trainer_open_setting, settingTitle(key))) { openSetting(key) }
      )
    }
    val menu = exercise.shape as? Shape.EdgeMenu
    if (menu != null && menu.sample) {
      content.addView(text(getString(R.string.gesture_trainer_menu_sample, menu.names.size)))
      val key = edgeMenuKey(menu.edge)
      content.addView(
        button(getString(R.string.gesture_trainer_open_setting, settingTitle(key))) { openSetting(key) }
      )
    }
    content.addView(button(getString(R.string.gesture_trainer_start)) { startTries() })
    content.addView(button(getString(R.string.gesture_trainer_skip)) { nextExercise() })
    content.addView(button(getString(R.string.gesture_trainer_contents)) { showContents() })
    content.addView(button(getString(R.string.gesture_trainer_back_to_groups)) { showGroups() })
    showTop(title)
  }

  private fun startTries() {
    reports.clear()
    enterScreen(Screen.TRIES)
    content.removeAllViews()
    val exercise = exercise ?: return
    val title = heading(exerciseName(exercise))
    content.addView(title)
    val go =
      when (exercise.shape) {
        is Shape.BackTaps -> R.string.gesture_trainer_go_back_tap
        Shape.Shake -> R.string.gesture_trainer_go_shake
        // These say where on the screen, so not the "anywhere on the screen" of the others.
        is Shape.EdgeMenu, is Shape.EdgeSlide, is Shape.CornerTaps -> R.string.gesture_trainer_go_placed
        else -> R.string.gesture_trainer_go
      }
    val instructions = text(exerciseName(exercise) + ". " + getString(go, TRIES))
    content.addView(instructions)
    takingTries = true
    newTry()
    // Spoken by moving focus to it, not by speech of its own: the Start button just left the
    // screen, and the focus SandFly moves then would cut such speech short.
    showTop(instructions)
    // At once, so that a try started right away is seen whole. The double tap on Start is no try:
    // touches that began before now are left out.
    triesStartMs = android.os.SystemClock.uptimeMillis()
    listen()
  }

  /** When the tries started, in uptime: touches that began before belong to the Start button. */
  private var triesStartMs = 0L

  /**
   * Takes every gesture as a try, so only while the trainer is on the screen with focus: a trainer
   * that took gestures from the background would leave the user with none that work.
   */
  private fun listen() {
    if (!resumed || !hasWindowFocus() || isFinishing) return
    GestureTrainerHooks.listener = hooks
    if ((practisesBackTaps && !backTaps.start()) || (practisesShake && !shakes.start())) {
      stopTries()
      content.addView(text(getString(R.string.gesture_trainer_no_accelerometer)))
      speakNow(getString(R.string.gesture_trainer_no_accelerometer))
    }
  }

  private fun stopListening() {
    handler.removeCallbacks(stopListeningAfterTries)
    if (GestureTrainerHooks.listener === hooks) GestureTrainerHooks.listener = null
    backTaps.stop()
    shakes.stop()
    handler.removeCallbacks(endTry)
  }

  /**
   * A touch on the screen while back taps are practised: the jolts it makes are no taps, and it
   * fails the try, so that a try can always be made.
   */
  private fun onScreenTouchedInBackTaps(event: MotionEvent) {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        backTaps.onScreenTouch(start = true, event.eventTime)
        handler.removeCallbacks(endTry)
        screenTouched = true
      }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        backTaps.onScreenTouch(start = false, event.eventTime)
        handler.postDelayed(endTry, BACK_TAP_QUIET_MS)
      }
    }
  }

  private fun stopTries() {
    takingTries = false
    // A gesture still being recognized after the last try must not act outside the trainer. Only a
    // two-stroke swipe waits longer than a multi-tap for its rest; meanwhile gestures do nothing.
    val waitMs =
      if (exercise?.shape is Shape.TwoStroke) waitForGestureMs()
      else GestureConfiguration.getMultiTapCompletionTimeoutMs().toLong()
    handler.removeCallbacks(stopListeningAfterTries)
    handler.postDelayed(stopListeningAfterTries, waitMs)
    newTry()
  }

  private val stopListeningAfterTries = Runnable { if (!takingTries) stopListening() }

  private fun newTry() {
    recorder = TouchRecorder()
    recognized.clear()
    lastRecognizedMs = -1L
    menuChoice = null
    jolts.clear()
    backTapRun = null
    screenTouched = false
    shakeStrongest = 0f
    shakeMostSwings = 0
    shaken = false
  }

  /**
   * Whether the try waits for the rest of the practised gesture although a shorter gesture was
   * recognized or it came slowly: more taps of the practised taps or corner taps, or the swipe
   * after the taps of a tap and swipe. The next touch within [SLOW_TAPS_GRACE_MS] belongs to the
   * try, so that it can say the gesture was too slow.
   */
  private fun waitsForRestOfGesture(): Boolean {
    val exercise = exercise ?: return false
    val touchesInGesture =
      when (val shape = exercise.shape) {
        is Shape.Taps -> if (shape.taps < 2) return false else shape.taps
        is Shape.CornerTaps -> if (shape.taps < 2) return false else shape.taps
        is Shape.TapSwipe -> shape.taps + 1
        is Shape.TwoStroke -> 2
        else -> return false
      }
    if (exercise.gestureId in recognized || recorder.touches.size >= touchesInGesture) return false
    val last = recorder.touches.lastOrNull() ?: return false
    return android.os.SystemClock.uptimeMillis() - last.upMs < restOfGestureGraceMs()
  }

  /**
   * How long after a touch the next one still belongs to the try when the gesture isn't complete:
   * for a two-stroke swipe, the time its second swipe may take, and more.
   */
  private fun restOfGestureGraceMs(): Long =
    if (exercise?.shape is Shape.TwoStroke) limits.twoStrokeMs + SLOW_TAPS_GRACE_MS
    else SLOW_TAPS_GRACE_MS

  /**
   * Whether a gesture or menu item was recognized after the last touch began. A gesture recognized
   * for an earlier slow tap does not count: the last tap can still be recognized too.
   */
  private fun lastTouchIsRecognized(): Boolean {
    if (menuChoice != null) return true
    val last = recorder.touches.lastOrNull() ?: return false
    return recognized.isNotEmpty() && lastRecognizedMs >= last.downMs
  }

  /** How long after a recognized gesture the try ends, if no other touch comes. */
  private fun settleMs(): Long = if (waitsForRestOfGesture()) restOfGestureGraceMs() else SETTLE_MS

  /** Whether the last touch of the try lifted too long ago for another tap of the same try. */
  private fun tryHasEnded(): Boolean {
    val last = recorder.touches.lastOrNull() ?: return false
    return android.os.SystemClock.uptimeMillis() - last.upMs > waitForGestureMs()
  }

  /** How long after the fingers lift a gesture can still be recognized, such as a double tap. */
  private fun waitForGestureMs(): Long =
    maxOf(
      GestureConfiguration.getMultiTapCompletionTimeoutMs().toLong(),
      GestureConfiguration.getMultiFingerTapCompletionTimeoutMs().toLong(),
      // The first swipe of a two-stroke swipe waits that long for the second.
      if (exercise?.shape is Shape.TwoStroke) restOfGestureGraceMs() else 0L,
    ) + GESTURE_MARGIN_MS

  private fun endTry() {
    handler.removeCallbacks(endTry)
    val exercise = exercise
    if (!takingTries || exercise == null) return
    val report =
      if (exercise.shape is Shape.BackTaps) {
        if (jolts.isEmpty() && backTapRun == null && !screenTouched) return
        analyzer.analyzeBackTaps(exercise, jolts.toList(), backTapRun, screenTouched)
      } else if (exercise.shape == Shape.Shake) {
        if (shakeStrongest == 0f && !shaken) return
        analyzer.analyzeShake(exercise, shakeStrongest, shakes.threshold, shakeMostSwings, shaken)
      } else {
        if (recorder.touches.isEmpty() || recorder.fingersDown) return
        analyzer.analyze(exercise, recorder.touches, recognized.toList(), menuChoice)
      }
    if (GestureLog.enabled) logTry(report)
    reports += report
    newTry()
    val said = tryText(reports.size, report, short = true)
    if (reports.size < TRIES) {
      speakNow(said)
      return
    }
    stopTries()
    var shown = false
    // Only while the tries' screen is still shown: the user may have gone to another one.
    val triesScreen = screenCount
    val showOnce = {
      if (!shown && !isFinishing && screenCount == triesScreen) {
        shown = true
        showResults()
      }
    }
    speakNow(said, showOnce)
    // In case the speech is dropped and never completes.
    handler.postDelayed(showOnce, RESULTS_FALLBACK_MS)
  }

  private fun logTry(report: AttemptReport) {
    val exercise = exercise
    val touches =
      recorder.touches.joinToString(" ") { touch ->
        touch.fingers.joinToString(",", "[", "]") { f ->
          "(%.0f,%.0f)->(%.0f,%.0f)@%dms".format(f.start.x, f.start.y, f.end.x, f.end.y, f.upMs - f.downMs)
        }
      }
    Log.d(
      LOG_TAG,
      "try ${reports.size + 1} of ${exercise?.gestureId} ${exercise?.shape} " +
        "screen=${limits.screenWidth}x${limits.screenHeight} pxPerCm=${limits.pxPerCm}: " +
        "recognized=${report.recognized} menu=${report.menuChoice} " +
        "rating=${report.rating} " +
        report.findings.joinToString { "${it.kind}/${it.severity} ${it.value} of ${it.limit}" } +
        " | " +
        report.measures.joinToString { "${it.kind}=${it.value}" } +
        " | touches $touches",
    )
  }

  // Results

  private fun showResults() {
    val exercise = exercise ?: return
    enterScreen(Screen.RESULTS)
    content.removeAllViews()
    val hits = reports.count { it.success }
    groupResults[index] = hits
    val title =
      heading(getString(R.string.gesture_trainer_results_heading, exerciseName(exercise), hits, reports.size))
    content.addView(title)

    content.addView(subheading(getString(R.string.gesture_trainer_results_tries)))
    reports.forEachIndexed { i, report ->
      val measures = report.measures.distinctBy { it.kind }.joinToString(", ") { measureText(it) }
          .replaceFirstChar { it.titlecase() }
      content.addView(text(tryText(i + 1, report, short = false) + if (measures.isEmpty()) "" else "\n$measures"))
    }

    val problems = reports.flatMap { it.problems }
    val close = reports.flatMap { it.findings.filter { f -> f.severity == Severity.CLOSE } }
    if (problems.isNotEmpty()) {
      content.addView(subheading(getString(R.string.gesture_trainer_results_change)))
      // The most frequent problems first, each once.
      problems
        .groupBy { it.kind }
        .entries
        .sortedByDescending { it.value.size }
        .take(MAX_TIPS)
        .forEach { (kind, findings) -> content.addView(text(findingText(findings.first()) + " " + tipText(kind))) }
    } else if (close.isEmpty()) {
      content.addView(text(getString(R.string.gesture_trainer_results_all_good)))
    }
    if (close.isNotEmpty()) {
      content.addView(subheading(getString(R.string.gesture_trainer_results_close)))
      close
        .groupBy { it.kind }
        .values
        .take(MAX_TIPS)
        .forEach { findings ->
          content.addView(
            text(getString(R.string.gesture_trainer_close_to_limit, findingText(findings.first())) + " " + tipText(findings.first().kind))
          )
        }
    }
    addSettingsAdvice(exercise, problems + close)
    if (exercise.shape == Shape.Scrub) addTwoFingerSwipesState()

    content.addView(button(getString(R.string.gesture_trainer_next)) { nextExercise() })
    content.addView(button(getString(R.string.gesture_trainer_repeat)) { startTries() })
    content.addView(button(getString(R.string.gesture_trainer_contents)) { showContents() })
    content.addView(button(getString(R.string.gesture_trainer_finish)) { showGroups() })
    showTop(title)
  }

  /** Adds advice and a button for each setting that would help with the [findings]. */
  private fun addSettingsAdvice(exercise: Exercise, findings: List<Finding>) {
    val kinds = findings.map { it.kind }.toSet()
    val advice = ArrayList<Pair<Int, Int>>()
    fun add(kind: FindingKind, text: Int, setting: Int) {
      if (kind in kinds && (text to setting) !in advice) advice += text to setting
    }
    if (exercise.shape is Shape.Swipe && exercise.shape.fingers == 1) {
      add(FindingKind.SLOW_START, R.string.gesture_trainer_advice_start_time, R.string.pref_swipe_start_time_key)
      add(FindingKind.BECAME_EXPLORATION, R.string.gesture_trainer_advice_start_time, R.string.pref_swipe_start_time_key)
    }
    if (exercise.shape is Shape.Taps && exercise.shape.fingers == 1) {
      add(FindingKind.TAPS_TOO_SLOW, R.string.gesture_trainer_advice_tap_time, R.string.pref_multi_tap_timeout_key)
    }
    add(FindingKind.SWIPE_HOLD_TOO_SHORT, R.string.gesture_trainer_advice_swipe_hold, R.string.pref_swipe_hold_time_key)
    add(
      FindingKind.BACK_TAP_TOO_LIGHT,
      R.string.gesture_trainer_advice_back_tap_sensitivity,
      R.string.pref_back_tap_sensitivity_key,
    )
    add(
      FindingKind.SHAKE_TOO_LIGHT,
      R.string.gesture_trainer_advice_shake_sensitivity,
      R.string.pref_shake_sensitivity_key,
    )
    add(
      FindingKind.SECOND_STROKE_LATE,
      R.string.gesture_trainer_advice_two_stroke_time,
      R.string.pref_two_stroke_swipe_time_key,
    )
    add(FindingKind.EDGE_START_TOO_FAR, R.string.gesture_trainer_advice_edge_width, R.string.pref_edge_zone_mm_key)
    add(FindingKind.CORNER_MISSED, R.string.gesture_trainer_advice_corner_size, R.string.pref_corner_size_mm_key)
    add(FindingKind.CORNER_TAP_SLOW, R.string.gesture_trainer_advice_corner_time, R.string.pref_corner_tap_time_ms_key)
    if (advice.isEmpty()) return
    content.addView(subheading(getString(R.string.gesture_trainer_results_settings)))
    for ((text, setting) in advice) {
      val more =
        if (setting == R.string.pref_corner_size_mm_key) "\n" + getString(R.string.gesture_trainer_corner_warning) else ""
      content.addView(text(getString(text, settingTitle(setting)) + more))
      content.addView(
        button(getString(R.string.gesture_trainer_open_setting, settingTitle(setting))) { openSetting(setting) }
      )
    }
  }

  private fun settingTitle(key: Int): String =
    getString(
      when (key) {
        R.string.pref_swipe_start_time_key -> R.string.title_pref_swipe_start_time
        R.string.pref_multi_tap_timeout_key -> R.string.title_pref_multi_tap_timeout
        R.string.pref_edge_zone_mm_key -> R.string.title_pref_edge_zone_mm
        R.string.pref_corner_size_mm_key -> R.string.title_pref_corner_size_mm
        R.string.pref_screen_edge_menu_left_key -> R.string.title_pref_edge_menu_left
        R.string.pref_screen_edge_menu_right_key -> R.string.title_pref_edge_menu_right
        R.string.pref_swipe_hold_time_key -> R.string.title_pref_swipe_hold_time
        R.string.pref_back_tap_sensitivity_key -> R.string.title_pref_back_tap_sensitivity
        R.string.pref_shake_sensitivity_key -> R.string.title_pref_shake_sensitivity
        R.string.pref_two_stroke_swipe_time_key -> R.string.title_pref_two_stroke_swipe_time
        else -> R.string.title_pref_corner_tap_time_ms
      }
    )

  /**
   * Says whether SandFly handles two-finger swipes, which the scrub needs: while it doesn't, they
   * scroll the app and a scrub mostly does too. Offers the setting that turns them on.
   */
  private fun addTwoFingerSwipesState() {
    val on = GestureLayout.handlesTwoFingerSwipes(SharedPreferencesUtils.getSharedPreferences(this))
    val title = getString(R.string.title_pref_handle_two_finger_swipes)
    content.addView(
      text(
        getString(
          if (on) R.string.gesture_trainer_scrub_two_finger_on else R.string.gesture_trainer_scrub_two_finger_off,
          title,
        )
      )
    )
    if (!on) {
      content.addView(
        button(getString(R.string.gesture_trainer_open_setting, title)) {
          openSetting(GestureLayout.TWO_FINGER_SWIPES_KEY)
        }
      )
    }
  }

  /** Opens the Gestures screen at the setting with preference key [key]. */
  private fun openSetting(key: Int) = openSetting(getString(key))

  /** Opens the Gestures screen at the setting with preference key [key]. */
  private fun openSetting(key: String) {
    settingsOpened = true
    startActivity(
      Intent(this, TalkBackSubSettings::class.java)
        .putExtra(FRAGMENT_NAME, TalkBackGestureShortcutPreferenceFragment::class.java.name)
        .putExtra(
          FRAGMENT_ARGS,
          Bundle().apply { putString(SettingsSearch.ARG_FOUND_KEY, key) },
        )
    )
  }

  private fun edgeMenuKey(edge: MenuEdge): Int =
    if (edge == MenuEdge.LEFT) R.string.pref_screen_edge_menu_left_key
    else R.string.pref_screen_edge_menu_right_key

  private fun nextExercise() {
    index++
    showExercise()
  }

  /** Lists the group's exercises, with how the ones done went, to open any of them. */
  private fun showContents() {
    stopTries()
    val group = group ?: return showGroups()
    enterScreen(Screen.CONTENTS)
    content.removeAllViews()
    val title = heading(getString(R.string.gesture_trainer_contents_heading, getString(group.title)))
    content.addView(title)
    exercises.forEachIndexed { i, exercise ->
      val name = getString(R.string.gesture_trainer_contents_item, i + 1, exerciseName(exercise))
      val item =
        button(name) {
          index = i
          showExercise()
        }
      groupResults[i]?.let { hits ->
        ViewCompat.setStateDescription(item, getString(R.string.gesture_trainer_contents_done, hits, TRIES))
      }
      content.addView(item)
    }
    content.addView(button(getString(R.string.gesture_trainer_back_to_groups)) { showGroups() })
    showTop(title)
  }

  private fun showGroupDone() {
    enterScreen(Screen.GROUP_DONE)
    content.removeAllViews()
    val title = heading(getString(R.string.gesture_trainer_group_done, getString(group?.title ?: R.string.gesture_trainer_title)))
    content.addView(title)
    if (exercises.isEmpty()) {
      // A group whose gestures are all left out says why, and offers the setting that brings them.
      leftOut.forEach { content.addView(text(it)) }
      if (group == TrainerGroup.TWO_STROKE_SWIPES) {
        val key = R.string.pref_two_stroke_swipe_time_key
        content.addView(
          button(getString(R.string.gesture_trainer_open_setting, settingTitle(key))) { openSetting(key) }
        )
      }
    }
    content.addView(button(getString(R.string.gesture_trainer_back_to_groups)) { showGroups() })
    content.addView(button(getString(R.string.gesture_trainer_close)) { finish() })
    showTop(title)
  }

  // Texts

  private fun exerciseName(exercise: Exercise): String {
    val menu = exercise.shape as? Shape.EdgeMenu ?: return gestureName(exercise.gestureId)
    return getString(
      R.string.gesture_trainer_menu_title,
      settingTitle(edgeMenuKey(menu.edge)),
      menu.names[menu.item],
      menu.item + 1,
    )
  }

  private fun gestureName(gestureId: Int): String =
    when (gestureId) {
      TrainerExercises.BACK_DOUBLE_TAP_ID -> getString(R.string.title_pref_shortcut_back_tap_double)
      TrainerExercises.BACK_TRIPLE_TAP_ID -> getString(R.string.title_pref_shortcut_back_tap_triple)
      TrainerExercises.BACK_QUADRUPLE_TAP_ID -> getString(R.string.title_pref_shortcut_back_tap_quadruple)
      TrainerExercises.SHAKE_ID -> getString(R.string.title_pref_shortcut_shake)
      else -> GestureShortcutMapping.getGestureString(this, gestureId) ?: gestureId.toString()
    }

  private fun tryText(number: Int, report: AttemptReport, short: Boolean): String {
    val head =
      when {
        report.success ->
          getString(
            R.string.gesture_trainer_try_recognized,
            number,
            getString(
              when (report.rating) {
                Rating.PRECISE -> R.string.gesture_trainer_rating_precise
                Rating.GOOD -> R.string.gesture_trainer_rating_good
                else -> R.string.gesture_trainer_rating_borderline
              }
            ),
          )
        report.menuChoice == AttemptAnalyzer.NO_MENU_ITEM ->
          getString(R.string.gesture_trainer_try_menu_cancelled, number)
        report.menuChoice != null -> {
          val names = (exercise?.shape as? Shape.EdgeMenu)?.names
          getString(R.string.gesture_trainer_try_menu_other, number, names?.getOrNull(report.menuChoice) ?: "")
        }
        report.recognized.isNotEmpty() ->
          getString(R.string.gesture_trainer_try_other, number, gestureName(report.recognized.last()))
        else -> getString(R.string.gesture_trainer_try_nothing, number)
      }
    if (report.success) {
      val close = report.findings.firstOrNull()
      return if (!short && close != null) head + " " + findingText(close) else head
    }
    val problems = report.problems
    val why =
      when {
        problems.isEmpty() && report.recognized.isNotEmpty() ->
          getString(R.string.gesture_trainer_finding_other_gesture)
        problems.isEmpty() -> getString(R.string.gesture_trainer_finding_none)
        short -> findingText(problems.first())
        // Each tap of a try can give the same finding; it is said once.
        else -> problems.map { findingText(it) }.distinct().joinToString(" ")
      }
    return "$head $why"
  }

  /** How many fingers the practised gesture swipes with, or null if it has no swipe. */
  private fun practisedSwipeFingers(): Int? =
    when (val shape = exercise?.shape) {
      is Shape.Swipe -> shape.fingers
      is Shape.TapSwipe -> shape.swipeFingers
      is Shape.TwoStroke -> 1
      else -> null
    }

  private fun findingText(finding: Finding): String {
    val ms = finding.value.toInt()
    val limitMs = finding.limit.toInt()
    val v = finding.value
    val l = finding.limit
    return when (finding.kind) {
      FindingKind.WRONG_FINGER_COUNT -> getString(R.string.gesture_trainer_finding_wrong_finger_count, ms, limitMs)
      FindingKind.FINGERS_LANDED_APART -> getString(R.string.gesture_trainer_finding_fingers_landed_apart, ms, limitMs)
      FindingKind.FINGER_LIFTED_EARLY -> getString(R.string.gesture_trainer_finding_finger_lifted_early)
      FindingKind.FINGERS_LIFTED_APART -> getString(R.string.gesture_trainer_finding_fingers_lifted_apart, ms, limitMs)
      FindingKind.SLOW_START -> getString(R.string.gesture_trainer_finding_slow_start, ms, limitMs)
      FindingKind.BECAME_EXPLORATION -> getString(R.string.gesture_trainer_finding_became_exploration)
      FindingKind.PAUSED -> getString(R.string.gesture_trainer_finding_paused, ms, limitMs)
      FindingKind.TOO_SHORT -> getString(R.string.gesture_trainer_finding_too_short, v, l)
      FindingKind.WRONG_DIRECTION ->
        getString(
          if (practisedSwipeFingers() == 1) R.string.gesture_trainer_finding_wrong_direction_one_finger
          else R.string.gesture_trainer_finding_wrong_direction,
          directionName(finding.direction),
        )
      FindingKind.OFF_LINE -> getString(R.string.gesture_trainer_finding_off_line, v, l)
      FindingKind.CURVED -> getString(R.string.gesture_trainer_finding_curved, ms, limitMs)
      FindingKind.TURN_MISSING -> getString(R.string.gesture_trainer_finding_turn_missing)
      FindingKind.SECOND_PART_WRONG -> getString(R.string.gesture_trainer_finding_second_part_wrong, directionName(finding.direction))
      FindingKind.TAP_MOVED -> getString(R.string.gesture_trainer_finding_tap_moved, v, l)
      FindingKind.TAP_HELD_LONG -> getString(R.string.gesture_trainer_finding_tap_held_long, ms, limitMs)
      FindingKind.HOLD_TOO_SHORT -> getString(R.string.gesture_trainer_finding_hold_too_short, ms, limitMs)
      FindingKind.TOO_FEW_TAPS -> getString(R.string.gesture_trainer_finding_too_few_taps, ms, limitMs)
      FindingKind.TOO_MANY_TAPS -> getString(R.string.gesture_trainer_finding_too_many_taps, ms, limitMs)
      FindingKind.TAPS_TOO_SLOW -> getString(R.string.gesture_trainer_finding_taps_too_slow, ms, limitMs)
      FindingKind.TAPS_APART -> getString(R.string.gesture_trainer_finding_taps_apart, v, l)
      FindingKind.PINCH_TOO_SMALL -> getString(R.string.gesture_trainer_finding_pinch_too_small, v, l)
      FindingKind.PINCH_WRONG_WAY -> getString(R.string.gesture_trainer_finding_pinch_wrong_way)
      FindingKind.MOVED_TOGETHER -> getString(R.string.gesture_trainer_finding_moved_together, v)
      FindingKind.ROTATE_TOO_SMALL -> getString(R.string.gesture_trainer_finding_rotate_too_small, v, l)
      FindingKind.ROTATE_WRONG_WAY -> getString(R.string.gesture_trainer_finding_rotate_wrong_way)
      FindingKind.SCRUB_TOO_SHORT -> getString(R.string.gesture_trainer_finding_scrub_too_short, v, l)
      FindingKind.SCRUB_NO_RETURN -> getString(R.string.gesture_trainer_finding_scrub_no_return, v, l)
      FindingKind.SCRUB_TOO_SLOW -> getString(R.string.gesture_trainer_finding_scrub_too_slow, ms, limitMs)
      FindingKind.EDGE_START_TOO_FAR -> getString(R.string.gesture_trainer_finding_edge_start_too_far, v, l)
      FindingKind.EDGE_WRONG_HALF -> getString(R.string.gesture_trainer_finding_edge_wrong_half)
      FindingKind.EDGE_TOO_SHORT -> getString(R.string.gesture_trainer_finding_edge_too_short, v, l)
      FindingKind.EDGE_TOO_LONG -> getString(R.string.gesture_trainer_finding_edge_too_long, v, l)
      FindingKind.EDGE_TOO_SLOW -> getString(R.string.gesture_trainer_finding_edge_too_slow, ms, limitMs)
      FindingKind.EDGE_SIDEWAYS -> getString(R.string.gesture_trainer_finding_edge_sideways, v, l)
      FindingKind.ALONG_OFF_EDGE -> getString(R.string.gesture_trainer_finding_along_off_edge)
      FindingKind.CORNER_MISSED -> getString(R.string.gesture_trainer_finding_corner_missed, v, l)
      FindingKind.CORNER_TAP_SLOW -> getString(R.string.gesture_trainer_finding_corner_tap_slow, ms, limitMs)
      FindingKind.MENU_IN_CORNER -> getString(R.string.gesture_trainer_finding_menu_in_corner)
      FindingKind.MENU_LEFT_EDGE -> getString(R.string.gesture_trainer_finding_menu_left_edge, v, l)
      FindingKind.MENU_LEFT_LINE -> getString(R.string.gesture_trainer_finding_menu_left_line, v, l)
      FindingKind.MENU_WRONG_ITEM -> getString(R.string.gesture_trainer_finding_menu_wrong_item, ms, limitMs)
      FindingKind.SWIPE_HOLD_TOO_SHORT -> getString(R.string.gesture_trainer_finding_swipe_hold_too_short, ms, limitMs)
      FindingKind.TAP_WRONG_HALF -> getString(R.string.gesture_trainer_finding_tap_wrong_half)
      FindingKind.SWIPE_MISSING -> getString(R.string.gesture_trainer_finding_swipe_missing)
      FindingKind.SWIPE_LATE -> getString(R.string.gesture_trainer_finding_swipe_late, ms, limitMs)
      FindingKind.BACK_TAP_TOO_LIGHT ->
        // One whole number: the strength and the strength needed as two decimals were hard to follow.
        getString(R.string.gesture_trainer_finding_back_tap_too_light, (100 * v / l.coerceAtLeast(0.01f)).toInt())
      FindingKind.BACK_TAP_SIDEWAYS -> getString(R.string.gesture_trainer_finding_back_tap_sideways)
      FindingKind.BACK_TAP_MOVING -> getString(R.string.gesture_trainer_finding_back_tap_moving)
      FindingKind.BACK_TAP_SCREEN_TOUCHED -> getString(R.string.gesture_trainer_finding_back_tap_screen_touched)
      FindingKind.BACK_TAPS_TOO_SLOW -> getString(R.string.gesture_trainer_finding_back_taps_too_slow, ms, limitMs)
      FindingKind.TURNED_IN_PINCH -> getString(R.string.gesture_trainer_finding_turned_in_pinch, v, l)
      FindingKind.WRONG_EDGE ->
        getString(R.string.gesture_trainer_finding_wrong_edge, sideName(finding.side), sideName(finding.expectedSide))
      FindingKind.MENU_OPENED_INSTEAD -> getString(R.string.gesture_trainer_finding_menu_opened_instead)
      FindingKind.TAP_NOT_TOP_AND_BOTTOM -> getString(R.string.gesture_trainer_finding_tap_not_top_and_bottom)
      FindingKind.SECOND_STROKE_MISSING -> getString(R.string.gesture_trainer_finding_second_stroke_missing)
      FindingKind.SECOND_STROKE_LATE -> getString(R.string.gesture_trainer_finding_second_stroke_late, ms, limitMs)
      FindingKind.STROKE_WRONG_DIRECTION ->
        getString(
          if (v >= 2f) R.string.gesture_trainer_finding_second_stroke_wrong_direction
          else R.string.gesture_trainer_finding_first_stroke_wrong_direction,
          directionName(finding.direction),
        )
      FindingKind.SHAKE_TOO_LIGHT ->
        getString(R.string.gesture_trainer_finding_shake_too_light, (100 * v / l.coerceAtLeast(0.01f)).toInt())
      FindingKind.SHAKE_TOO_FEW_SWINGS -> getString(R.string.gesture_trainer_finding_shake_too_few_swings, ms, limitMs)
    }
  }

  private fun tipText(kind: FindingKind): String =
    getString(
      when (kind) {
        FindingKind.WRONG_FINGER_COUNT -> R.string.gesture_trainer_tip_wrong_finger_count
        FindingKind.FINGERS_LANDED_APART -> R.string.gesture_trainer_tip_fingers_landed_apart
        FindingKind.FINGER_LIFTED_EARLY -> R.string.gesture_trainer_tip_finger_lifted_early
        FindingKind.FINGERS_LIFTED_APART -> R.string.gesture_trainer_tip_fingers_lifted_apart
        FindingKind.SLOW_START,
        FindingKind.BECAME_EXPLORATION -> R.string.gesture_trainer_tip_slow_start
        FindingKind.PAUSED -> R.string.gesture_trainer_tip_paused
        FindingKind.TOO_SHORT -> R.string.gesture_trainer_tip_too_short
        FindingKind.WRONG_DIRECTION -> R.string.gesture_trainer_tip_wrong_direction
        FindingKind.OFF_LINE -> R.string.gesture_trainer_tip_off_line
        FindingKind.CURVED -> R.string.gesture_trainer_tip_curved
        FindingKind.TURN_MISSING -> R.string.gesture_trainer_tip_turn_missing
        FindingKind.SECOND_PART_WRONG -> R.string.gesture_trainer_tip_second_part_wrong
        FindingKind.TAP_MOVED -> R.string.gesture_trainer_tip_tap_moved
        FindingKind.TAP_HELD_LONG -> R.string.gesture_trainer_tip_tap_held_long
        FindingKind.HOLD_TOO_SHORT -> R.string.gesture_trainer_tip_hold_too_short
        FindingKind.TOO_FEW_TAPS -> R.string.gesture_trainer_tip_too_few_taps
        FindingKind.TOO_MANY_TAPS -> R.string.gesture_trainer_tip_too_many_taps
        FindingKind.TAPS_TOO_SLOW -> R.string.gesture_trainer_tip_taps_too_slow
        FindingKind.TAPS_APART -> R.string.gesture_trainer_tip_taps_apart
        FindingKind.PINCH_TOO_SMALL -> R.string.gesture_trainer_tip_pinch_too_small
        FindingKind.PINCH_WRONG_WAY -> R.string.gesture_trainer_tip_pinch_wrong_way
        FindingKind.MOVED_TOGETHER -> R.string.gesture_trainer_tip_moved_together
        FindingKind.ROTATE_TOO_SMALL -> R.string.gesture_trainer_tip_rotate_too_small
        FindingKind.ROTATE_WRONG_WAY -> R.string.gesture_trainer_tip_rotate_wrong_way
        FindingKind.SCRUB_TOO_SHORT -> R.string.gesture_trainer_tip_scrub_too_short
        FindingKind.SCRUB_NO_RETURN -> R.string.gesture_trainer_tip_scrub_no_return
        FindingKind.SCRUB_TOO_SLOW -> R.string.gesture_trainer_tip_scrub_too_slow
        FindingKind.EDGE_START_TOO_FAR -> R.string.gesture_trainer_tip_edge_start_too_far
        FindingKind.EDGE_WRONG_HALF -> R.string.gesture_trainer_tip_edge_wrong_half
        FindingKind.EDGE_TOO_SHORT -> R.string.gesture_trainer_tip_edge_too_short
        FindingKind.EDGE_TOO_LONG -> R.string.gesture_trainer_tip_edge_too_long
        FindingKind.EDGE_TOO_SLOW -> R.string.gesture_trainer_tip_edge_too_slow
        FindingKind.EDGE_SIDEWAYS -> R.string.gesture_trainer_tip_edge_sideways
        FindingKind.ALONG_OFF_EDGE -> R.string.gesture_trainer_tip_along_off_edge
        FindingKind.CORNER_MISSED -> R.string.gesture_trainer_tip_corner_missed
        FindingKind.CORNER_TAP_SLOW -> R.string.gesture_trainer_tip_corner_tap_slow
        FindingKind.MENU_IN_CORNER -> R.string.gesture_trainer_tip_menu_in_corner
        FindingKind.MENU_LEFT_EDGE -> R.string.gesture_trainer_tip_menu_left_edge
        FindingKind.MENU_LEFT_LINE -> R.string.gesture_trainer_tip_menu_left_line
        FindingKind.MENU_WRONG_ITEM ->
          if ((exercise?.shape as? Shape.EdgeMenu)?.horizontal == true) {
            R.string.gesture_trainer_tip_menu_wrong_item_across
          } else {
            R.string.gesture_trainer_tip_menu_wrong_item
          }
        FindingKind.SWIPE_HOLD_TOO_SHORT -> R.string.gesture_trainer_tip_swipe_hold_too_short
        FindingKind.TAP_WRONG_HALF -> R.string.gesture_trainer_tip_tap_wrong_half
        FindingKind.SWIPE_MISSING -> R.string.gesture_trainer_tip_swipe_missing
        FindingKind.SWIPE_LATE -> R.string.gesture_trainer_tip_swipe_late
        FindingKind.BACK_TAP_TOO_LIGHT -> R.string.gesture_trainer_tip_back_tap_too_light
        FindingKind.BACK_TAP_SIDEWAYS -> R.string.gesture_trainer_tip_back_tap_sideways
        FindingKind.BACK_TAP_MOVING -> R.string.gesture_trainer_tip_back_tap_moving
        FindingKind.BACK_TAP_SCREEN_TOUCHED -> R.string.gesture_trainer_tip_back_tap_screen_touched
        FindingKind.BACK_TAPS_TOO_SLOW -> R.string.gesture_trainer_tip_back_taps_too_slow
        FindingKind.TURNED_IN_PINCH -> R.string.gesture_trainer_tip_turned_in_pinch
        FindingKind.WRONG_EDGE -> R.string.gesture_trainer_tip_wrong_edge
        FindingKind.MENU_OPENED_INSTEAD -> R.string.gesture_trainer_tip_menu_opened_instead
        FindingKind.TAP_NOT_TOP_AND_BOTTOM -> R.string.gesture_trainer_tip_tap_not_top_and_bottom
        FindingKind.SECOND_STROKE_MISSING -> R.string.gesture_trainer_tip_second_stroke_missing
        FindingKind.SECOND_STROKE_LATE -> R.string.gesture_trainer_tip_second_stroke_late
        FindingKind.STROKE_WRONG_DIRECTION -> R.string.gesture_trainer_tip_wrong_direction
        FindingKind.SHAKE_TOO_LIGHT -> R.string.gesture_trainer_tip_shake_too_light
        FindingKind.SHAKE_TOO_FEW_SWINGS -> R.string.gesture_trainer_tip_shake_too_few_swings
      }
    )

  private fun measureText(measure: Measure): String {
    val v = measure.value
    return when (measure.kind) {
      MeasureKind.DURATION_MS -> getString(R.string.gesture_trainer_measure_duration, v.toInt())
      MeasureKind.LENGTH_MM -> getString(R.string.gesture_trainer_measure_length, v / 10f)
      MeasureKind.SPEED_CM_PER_S -> getString(R.string.gesture_trainer_measure_speed, v)
      MeasureKind.ANGLE_OFF_LINE_DEGREES -> getString(R.string.gesture_trainer_measure_angle, v)
      MeasureKind.START_MS -> getString(R.string.gesture_trainer_measure_start, v.toInt())
      MeasureKind.LANDING_SPREAD_MS -> getString(R.string.gesture_trainer_measure_landing, v.toInt())
      MeasureKind.TAP_MS -> getString(R.string.gesture_trainer_measure_tap, v.toInt())
      MeasureKind.GAP_MS -> getString(R.string.gesture_trainer_measure_gap, v.toInt())
      MeasureKind.SPREAD_CHANGE_MM -> getString(R.string.gesture_trainer_measure_spread, kotlin.math.abs(v))
      MeasureKind.ROTATION_DEGREES -> getString(R.string.gesture_trainer_measure_rotation, v)
      MeasureKind.CORNER_DISTANCE_MM -> getString(R.string.gesture_trainer_measure_corner, v)
      MeasureKind.EDGE_DISTANCE_MM -> getString(R.string.gesture_trainer_measure_edge, v)
      MeasureKind.MENU_ITEM -> getString(R.string.gesture_trainer_measure_menu_item, v.toInt())
      MeasureKind.HOLD_MS -> getString(R.string.gesture_trainer_measure_hold, v.toInt())
      MeasureKind.BACK_TAP_STRENGTH -> getString(R.string.gesture_trainer_measure_back_tap_strength, v)
      MeasureKind.SHAKE_STRENGTH -> getString(R.string.gesture_trainer_measure_shake_strength, v)
      MeasureKind.FIRST_STROKE_MS -> getString(R.string.gesture_trainer_measure_first_stroke, v.toInt())
      MeasureKind.STROKE_GAP_MS -> getString(R.string.gesture_trainer_measure_stroke_gap, v.toInt())
      MeasureKind.SECOND_STROKE_MS -> getString(R.string.gesture_trainer_measure_second_stroke, v.toInt())
      MeasureKind.WHOLE_GESTURE_MS -> getString(R.string.gesture_trainer_measure_whole_gesture, v.toInt())
    }
  }

  private fun sideName(side: ScreenSide?): String =
    getString(
      when (side) {
        ScreenSide.LEFT -> R.string.gesture_trainer_side_left
        ScreenSide.RIGHT -> R.string.gesture_trainer_side_right
        ScreenSide.TOP -> R.string.gesture_trainer_side_top
        else -> R.string.gesture_trainer_side_bottom
      }
    )

  private fun directionName(direction: Direction?): String =
    getString(
      when (direction) {
        Direction.UP -> R.string.gesture_trainer_direction_up
        Direction.DOWN -> R.string.gesture_trainer_direction_down
        Direction.LEFT -> R.string.gesture_trainer_direction_left
        else -> R.string.gesture_trainer_direction_right
      }
    )

  private fun fingersName(fingers: Int): String =
    getString(
      when (fingers) {
        1 -> R.string.gesture_trainer_fingers_1
        2 -> R.string.gesture_trainer_fingers_2
        3 -> R.string.gesture_trainer_fingers_3
        else -> R.string.gesture_trainer_fingers_4
      }
    )

  private fun tapsName(taps: Int): String =
    getString(
      when (taps) {
        1 -> R.string.gesture_trainer_taps_1
        2 -> R.string.gesture_trainer_taps_2
        3 -> R.string.gesture_trainer_taps_3
        else -> R.string.gesture_trainer_taps_4
      }
    )

  /** How to choose the item of a menu across the screen: where each item is, from the edge. */
  private fun howToMenuAcross(shape: Shape.EdgeMenu): String {
    val items = shape.names.size
    val width = limits.screenWidth.toFloat()
    fun startOf(item: Int) =
      EdgeSwipeTracker.horizontalMenuItemStart(item, items, width, limits.edgeFirstStagePx)
    return getString(
      R.string.gesture_trainer_how_menu_horizontal,
      getString(
        if (shape.edge == MenuEdge.LEFT) R.string.gesture_trainer_edge_left
        else R.string.gesture_trainer_edge_right
      ),
      mm(limits.edgeFirstStagePx),
      items,
      shape.item + 1,
      shape.names[shape.item],
      cm(startOf(1) - startOf(0)),
      cm(startOf(shape.item)),
      cm(startOf(shape.item + 1)),
    )
  }

  private fun mm(px: Float): Int = Math.round(limits.toMm(px))

  /** [px] in centimetres, with one decimal, as the language writes it. */
  private fun cm(px: Float): String = "%.1f".format(limits.toMm(px) / 10f)

  private fun howTo(exercise: Exercise): String =
    when (val shape = exercise.shape) {
      is Shape.Swipe -> howToSwipe(shape)
      is Shape.Taps -> {
        val gap = if (shape.fingers == 1) limits.multiTapGapMs else limits.multiFingerTapGapMs
        val main =
          when {
            shape.hold && shape.taps == 1 ->
              getString(R.string.gesture_trainer_how_hold, fingersName(shape.fingers), limits.holdMs(shape).toInt())
            shape.hold ->
              getString(
                R.string.gesture_trainer_how_taps_hold,
                tapsName(shape.taps),
                fingersName(shape.fingers),
                limits.longPressMs.toInt(),
              )
            else -> getString(R.string.gesture_trainer_how_taps, tapsName(shape.taps), fingersName(shape.fingers))
          }
        val quick = if (shape.taps > 1) " " + getString(R.string.gesture_trainer_how_taps_quick, gap.toInt()) else ""
        val half =
          when (shape.half) {
            null -> ""
            ScreenHalf.TOP_AND_BOTTOM -> " " + getString(R.string.gesture_trainer_how_taps_top_and_bottom)
            else ->
              " " +
                getString(
                  R.string.gesture_trainer_how_taps_half,
                  getString(
                    if (shape.half == ScreenHalf.TOP) R.string.gesture_trainer_half_top
                    else R.string.gesture_trainer_half_bottom
                  ),
                )
          }
        val turnsOff =
          if (limits.holdMs(shape) > limits.longPressMs) " " + getString(R.string.gesture_trainer_how_long_hold_turns_off)
          else ""
        main + quick + half + turnsOff
      }
      is Shape.TapSwipe ->
        getString(
          R.string.gesture_trainer_how_tap_swipe,
          tapsName(shape.taps),
          fingersName(shape.fingers),
          limits.multiFingerTapGapMs.toInt(),
          if (shape.swipeFingers == shape.fingers) getString(R.string.gesture_trainer_same_fingers)
          else fingersName(shape.swipeFingers),
          directionName(shape.direction),
        )
      Shape.Shake -> getString(R.string.gesture_trainer_how_shake)
      is Shape.TwoStroke ->
        getString(
          R.string.gesture_trainer_how_two_stroke,
          directionName(shape.first),
          directionName(shape.second),
          limits.twoStrokeMs.toInt(),
        )
      is Shape.BackTaps ->
        getString(
          R.string.gesture_trainer_how_back_tap,
          tapsName(shape.taps),
          (BackTapDetector.MAX_TAP_INTERVAL_NS / 1_000_000L).toInt(),
        )
      is Shape.Pinch -> {
        val how =
          getString(
            if (shape.spread) R.string.gesture_trainer_how_spread else R.string.gesture_trainer_how_pinch,
            shape.fingers,
          )
        val hand =
          when {
            shape.fingers == 3 -> R.string.gesture_trainer_how_three_finger_hand
            shape.fingers == 4 && !shape.spread -> R.string.gesture_trainer_how_four_finger_pinch_hand
            else -> null
          }
        if (hand == null) how else how + " " + getString(hand)
      }
      is Shape.Rotate ->
        getString(
          R.string.gesture_trainer_how_rotate,
          getString(if (shape.clockwise) R.string.gesture_trainer_clockwise else R.string.gesture_trainer_counterclockwise),
        )
      Shape.Scrub -> getString(R.string.gesture_trainer_how_scrub)
      is Shape.EdgeSlide -> howToEdge(shape)
      is Shape.EdgeMenu ->
        if (shape.horizontal) {
          howToMenuAcross(shape)
        } else {
          getString(
            R.string.gesture_trainer_how_menu,
            getString(
              if (shape.edge == MenuEdge.LEFT) R.string.gesture_trainer_edge_left
              else R.string.gesture_trainer_edge_right
            ),
            mm(limits.edgeFirstStagePx),
            shape.names.size,
            shape.item + 1,
            shape.names[shape.item],
            cm(limits.screenHeight.toFloat() / shape.names.size),
            cm(limits.screenHeight.toFloat() * shape.item / shape.names.size),
            cm(limits.screenHeight.toFloat() * (shape.item + 1) / shape.names.size),
          )
        }
      is Shape.CornerTaps ->
        getString(
          R.string.gesture_trainer_how_corner,
          tapsName(shape.taps),
          getString(
            when (shape.corner) {
              Corner.TOP_LEFT -> R.string.gesture_trainer_corner_top_left
              Corner.TOP_RIGHT -> R.string.gesture_trainer_corner_top_right
              Corner.BOTTOM_LEFT -> R.string.gesture_trainer_corner_bottom_left
              Corner.BOTTOM_RIGHT -> R.string.gesture_trainer_corner_bottom_right
            }
          ),
          mm(limits.cornerSizePx),
          limits.cornerTapMs.toInt(),
        ) +
          (if (shape.taps > 1) " " + getString(R.string.gesture_trainer_how_taps_quick, limits.multiTapGapMs.toInt()) else "") +
          (if (shape.hold) " " + getString(R.string.gesture_trainer_how_corner_hold, limits.longPressMs.toInt()) else "")
    }

  private fun howToSwipe(shape: Shape.Swipe): String {
    val swipe = howToSwipeMovement(shape)
    return if (shape.hold) swipe + " " + getString(R.string.gesture_trainer_how_swipe_hold, limits.swipeHoldMs.toInt())
    else swipe
  }

  private fun howToSwipeMovement(shape: Shape.Swipe): String {
    val first = directionName(shape.directions[0])
    if (shape.directions.size == 1) {
      val main = getString(R.string.gesture_trainer_how_swipe, fingersName(shape.fingers), first)
      return if (shape.fingers > 1) main + " " + getString(R.string.gesture_trainer_how_swipe_multi, first) else main
    }
    val second = directionName(shape.directions[1])
    if (shape.directions.size == 3) {
      return getString(R.string.gesture_trainer_how_three_part, first, second, directionName(shape.directions[2]))
    }
    val backAndForth =
      shape.directions[0].dx == -shape.directions[1].dx && shape.directions[0].dy == -shape.directions[1].dy
    return if (backAndForth) {
      getString(R.string.gesture_trainer_how_back_and_forth, first, second)
    } else {
      getString(R.string.gesture_trainer_how_angle, fingersName(shape.fingers), first, second)
    }
  }

  private fun howToEdge(shape: Shape.EdgeSlide): String {
    val edge = shape.edge
    val place =
      getString(
        when (edge) {
          Edge.BOTTOM -> R.string.gesture_trainer_edge_bottom
          Edge.TOP_LEFT -> R.string.gesture_trainer_edge_top_left
          Edge.TOP_RIGHT -> R.string.gesture_trainer_edge_top_right
          Edge.LEFT_TOP -> R.string.gesture_trainer_edge_left_top
          Edge.LEFT_BOTTOM -> R.string.gesture_trainer_edge_left_bottom
          Edge.RIGHT_TOP -> R.string.gesture_trainer_edge_right_top
          Edge.RIGHT_BOTTOM -> R.string.gesture_trainer_edge_right_bottom
          Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN -> R.string.gesture_trainer_edge_left
          Edge.ALONG_RIGHT_UP, Edge.ALONG_RIGHT_DOWN -> R.string.gesture_trainer_edge_right
          Edge.ALONG_TOP_LEFT, Edge.ALONG_TOP_RIGHT -> R.string.gesture_trainer_edge_top
          Edge.ALONG_BOTTOM_LEFT, Edge.ALONG_BOTTOM_RIGHT -> R.string.gesture_trainer_edge_bottom
        }
      )
    val direction =
      directionName(
        when (edge) {
          Edge.BOTTOM, Edge.ALONG_LEFT_UP, Edge.ALONG_RIGHT_UP -> Direction.UP
          Edge.TOP_LEFT, Edge.TOP_RIGHT, Edge.ALONG_LEFT_DOWN, Edge.ALONG_RIGHT_DOWN -> Direction.DOWN
          Edge.LEFT_TOP, Edge.LEFT_BOTTOM, Edge.ALONG_TOP_RIGHT, Edge.ALONG_BOTTOM_RIGHT -> Direction.RIGHT
          else -> Direction.LEFT
        }
      )
    return when {
      edge.along ->
        getString(
          R.string.gesture_trainer_how_along,
          TrainerExercises.alongEdgeName(this, edge.side),
          direction,
          mm(limits.edgeFirstStagePx),
        )
      shape.long ->
        getString(R.string.gesture_trainer_how_edge_long, place, mm(limits.edgeZonePx), direction, mm(limits.edgeSecondStagePx))
      else -> getString(R.string.gesture_trainer_how_edge, place, mm(limits.edgeZonePx), direction, mm(limits.edgeFirstStagePx))
    }
  }

  private fun sizesText(): String =
    getString(
      R.string.gesture_trainer_sizes,
      settingTitle(R.string.pref_edge_zone_mm_key),
      mm(limits.edgeZonePx),
      settingTitle(R.string.pref_corner_size_mm_key),
      mm(limits.cornerSizePx),
    )

  // Speech

  private fun speakNow(text: CharSequence, then: (() -> Unit)? = null) {
    val feedback = TutorialHooks.feedback
    if (feedback == null) {
      @Suppress("DEPRECATION") window.decorView.announceForAccessibility(text)
      then?.let { handler.postDelayed(it, NO_SPEECH_DELAY_MS) }
      return
    }
    // Forced, as speech the user asks for is: SandFly drops other speech while audio plays.
    val options =
      SpeechController.SpeakOptions.create()
        .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
        .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)
    if (then != null) {
      options.setCompletedAction { handler.post { if (!isDestroyed) then() } }
    }
    feedback.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, options))
  }

  // Views

  private fun heading(text: CharSequence) =
    TextView(this).apply {
      this.text = text
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
      setPadding(dp(16), dp(12), dp(16), dp(4))
      ViewCompat.setAccessibilityHeading(this, true)
    }

  private fun subheading(text: CharSequence) =
    TextView(this).apply {
      this.text = text
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
      setPadding(dp(16), dp(12), dp(16), dp(4))
      ViewCompat.setAccessibilityHeading(this, true)
    }

  private fun text(text: CharSequence) =
    TextView(this).apply {
      this.text = text
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
      setPadding(dp(16), dp(8), dp(16), dp(8))
    }

  private fun button(label: CharSequence, onClick: () -> Unit) =
    Button(this).apply {
      text = label
      isAllCaps = false
      setOnClickListener { onClick() }
      layoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

  /** Scrolls to the top and puts SandFly's focus on [view], which SandFly then reads. */
  private fun showTop(view: View) {
    scroll.scrollTo(0, 0)
    view.post { focus(view) }
  }

  // The trainer moves focus as a screen reader does: it is part of SandFly.
  @SuppressLint("AccessibilityFocus")
  private fun focus(view: View) {
    view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
  }

  private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

  companion object {
    const val TRIES = 3
    private const val LOG_TAG = "GestureTrainer"
    private const val SETTLE_MS = 120L
    private const val SLOW_TAPS_GRACE_MS = 1000L
    private const val GESTURE_MARGIN_MS = 250L
    private const val NO_SPEECH_DELAY_MS = 1500L
    private const val RESULTS_FALLBACK_MS = 12000L
    private const val MAX_TIPS = 3
    /** How long after a jolt that is no tap, or a touch on the screen, a back-tap try ends. */
    private const val BACK_TAP_QUIET_MS = 1200L
    /** How long after the device stops moving a shake try ends. */
    private const val SHAKE_QUIET_MS = 1000L

    @JvmStatic
    fun intent(context: Context): Intent = Intent(context, GestureTrainerActivity::class.java)
  }
}
