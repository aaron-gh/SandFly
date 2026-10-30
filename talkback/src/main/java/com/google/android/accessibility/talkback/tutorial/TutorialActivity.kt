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

package com.google.android.accessibility.talkback.tutorial

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.commands.CommandBindings
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.keyboard.ArrowKeyNav
import com.google.android.accessibility.talkback.keyboard.KeyCombo
import com.google.android.accessibility.talkback.keyboard.KeyComboModel
import com.google.android.accessibility.talkback.keyboard.SandFlyKey
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut
import com.google.android.accessibility.talkback.selector.SelectorController
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText

/**
 * SandFly's tutorial: lessons in chapters, each with an instruction and practice items. Each step
 * waits until the user has done what it says, with their own gestures, then says the next one.
 * Gestures that would leave the tutorial are taken in the steps that teach them, and said instead.
 * The keyboard tutorial does the same with the user's keyboard shortcuts and its own lessons. The
 * tutorial remembers the lesson it was on and the lessons that are done.
 */
class TutorialActivity : AppCompatActivity() {
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var mapping: GestureShortcutMapping
  private lateinit var content: LinearLayout
  private lateinit var scroll: ScrollView
  private lateinit var bottomBar: LinearLayout
  private lateinit var nextButton: Button
  private var instruction: TextView? = null
  private var instructionFocused = false
  /** Said after the lesson's heading when it first has focus. */
  private var lessonText: String? = null
  private var heading: View? = null

  /** Whether this is the keyboard tutorial. */
  private var keyboard = false

  private var lessonIndex = -1
  private var stepIndex = 0
  private val hits = ArrayList<String>()
  private var lessonDone = false

  private var lastAction: String? = null
  private var lastActionTime = 0L
  private var lostWindowFocus = false
  private var shown = false
  private var speechDelay = SPEECH_DELAY_MS
  private var repeats = 0
  private var reading = false
  /** Said when continuous reading stops, so that it doesn't interrupt the reading. */
  private var afterReading: (() -> Unit)? = null

  private val lessons: List<TutorialLesson>
    get() = if (keyboard) TutorialLessons.keyboard else TutorialLessons.all

  private val lesson: TutorialLesson?
    get() = lessons.getOrNull(lessonIndex)

  private val step: TutorialStep?
    get() = if (lessonDone) null else lesson?.steps?.getOrNull(stepIndex)

  private val stepContext =
    object : StepContext {
      override fun isAction(action: String, key: Int) = action == commandValue(key)

      override fun recentAction(key: Int) =
        lastAction == commandValue(key) &&
          SystemClock.uptimeMillis() - lastActionTime < RECENT_ACTION_MS

      override val readingControl: SelectorController.Setting
        get() = SelectorController.getCurrentSetting(this@TutorialActivity)

      override fun string(id: Int) = getString(id)
    }

  private val hooks =
    object : TutorialHooks.Listener {
      override fun onGesture(gestureId: Int, action: String): Boolean =
        onCommand(action) {
          GestureShortcutMapping.getGestureString(this@TutorialActivity, gestureId)
        }

      override fun onKeys(keys: String?, command: String): Boolean = onCommand(command) { keys }

      override fun onSystemKeys(keyCode: Int): Boolean {
        if (!hasWindowFocus() || lessonIndex < 0) return false
        userActed()
        val name = systemKeyLabel(keyCode)
        when {
          lesson?.id == TutorialLessons.PRACTICE_LESSON && !lessonDone ->
            speakNow(
              getString(R.string.sandfly_tutorial_practice_gesture, systemKeyName(keyCode), name)
            )
          step?.systemKey == keyCode -> {
            speakNow(name)
            handler.post { onInput(TutorialInput.SystemKey(keyCode)) }
          }
          keyCode == KeyEvent.KEYCODE_ESCAPE ->
            speakNow(getString(R.string.sandfly_tutorial_back_in_lesson))
          else -> speakNow(getString(R.string.sandfly_tutorial_not_in_lesson, name))
        }
        return true
      }

      override fun onBrowseMode(enabled: Boolean) {
        handler.post { onInput(TutorialInput.BrowseMode(enabled)) }
      }

      /** A gesture or keys ran [action]: [inputName] names them, for practice. */
      private fun onCommand(action: String, inputName: () -> String?): Boolean {
        userActed()
        lastAction = action
        lastActionTime = SystemClock.uptimeMillis()
        val focused = hasWindowFocus()
        val taken =
          focused &&
            if (lesson?.id == TutorialLessons.PRACTICE_LESSON && !lessonDone) {
              practice(inputName(), action)
            } else {
              step?.taken?.any { commandValue(it) == action } == true
            }
        if (taken && lesson?.id != TutorialLessons.PRACTICE_LESSON) {
          speakNow(actionName(action))
        }
        handler.post { onInput(TutorialInput.Command(action)) }
        if (!taken && focused && action == getString(R.string.shortcut_value_back) && keepsBack()) {
          speakNow(getString(R.string.sandfly_tutorial_back_in_lesson))
          return true
        }
        if (!taken && focused && lessonIndex >= 0) {
          if (leavesLesson(action)) {
            speakNow(getString(R.string.sandfly_tutorial_not_in_lesson, actionName(action)))
            return true
          }
          if (TutorialLessons.SILENCING_ACTIONS.any { commandValue(it) == action }) {
            speakNow(getString(R.string.sandfly_tutorial_not_in_lesson_short, actionName(action)))
            return true
          }
        }
        return taken
      }

      override fun onContinuousReading(active: Boolean) {
        reading = active
        if (active) {
          onInput(TutorialInput.ReadingStarted)
        } else {
          afterReading?.let {
            afterReading = null
            handler.postDelayed(it, SPEECH_DELAY_MS)
          }
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    keyboard = intent.getBooleanExtra(EXTRA_KEYBOARD, false)
    setTitle(titleRes())
    mapping = GestureShortcutMapping(this)

    content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    scroll = ScrollView(this).apply { addView(content) }
    nextButton = button(R.string.sandfly_tutorial_skip_step) { next() }
    bottomBar =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(button(R.string.sandfly_tutorial_contents) { showContents() }, weight())
        addView(nextButton, weight())
      }
    val root =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(bottomBar)
      }
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      WindowInsetsCompat.CONSUMED
    }
    setContentView(root)
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (keepsBack()) speakNow(getString(R.string.sandfly_tutorial_back_in_lesson))
          else finish()
        }
      },
    )

    openStartLesson(intent)
  }

  /** Opens the lesson that [intent] asks for, or the one the tutorial was on. */
  private fun openStartLesson(intent: Intent) {
    val requested = indexOf(intent.getStringExtra(EXTRA_LESSON))
    val saved = indexOf(prefs().getString(prefLesson(), null))
    val done = prefs().getStringSet(prefDone(), emptySet()) ?: emptySet()
    when {
      requested >= 0 -> openLesson(requested)
      saved < 0 -> openLesson(0)
      // A lesson that was finished goes on to the next.
      lessons[saved].id in done && saved + 1 < lessons.size -> openLesson(saved + 1)
      else -> openLesson(saved)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    // What the tutorial shows now, for when the activity is made again.
    setIntent(intent)
    // Brought back from the background, where Android restores focus to the item that last had
    // it: the heading gets focus when the window does, as when the tutorial first opens.
    if (!hasWindowFocus()) shown = false
    val keyboard = intent.getBooleanExtra(EXTRA_KEYBOARD, false)
    if (keyboard != this.keyboard) {
      // The other tutorial, which has lessons of its own.
      this.keyboard = keyboard
      setTitle(titleRes())
      openStartLesson(intent)
      return
    }
    // Opened again, so that the lesson is read again.
    val requested = indexOf(intent.getStringExtra(EXTRA_LESSON))
    when {
      requested >= 0 -> openLesson(requested)
      lessonIndex >= 0 -> openLesson(lessonIndex)
      else -> showContents()
    }
  }

  override fun onResume() {
    super.onResume()
    TutorialHooks.listener = hooks
    // Continuous reading that started or stopped while the tutorial was in the background.
    val continuous = TutorialHooks.continuousReading
    if (reading != continuous) hooks.onContinuousReading(continuous)
    userActed()
  }

  override fun onPause() {
    super.onPause()
    handler.removeCallbacks(repeatInstruction)
    if (TutorialHooks.listener === hooks) TutorialHooks.listener = null
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacksAndMessages(null)
    mapping.onUnbind()
    TalkBackService.getInstance()?.onTutorialClosed()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (!hasFocus) {
      lostWindowFocus = true
    } else if (!shown) {
      shown = true
      // In case SandFly restored focus to another item.
      handler.postDelayed({ if (lessonText != null) heading?.let { focus(it) } }, FIRST_FOCUS_MS)
    } else if (lostWindowFocus) {
      lostWindowFocus = false
      // Lets the window's focus settle before the next instruction is said.
      handler.postDelayed({ onInput(TutorialInput.WindowFocusReturned) }, SPEECH_DELAY_MS)
    }
  }

  // Lessons

  private fun openLesson(index: Int) {
    val lesson = lessons[index]
    lessonIndex = index
    stepIndex = 0
    hits.clear()
    lessonDone = false
    afterReading = null
    prefs().edit { putString(prefLesson(), lesson.id) }

    content.removeAllViews()
    val heading =
      heading(
          getString(
            R.string.sandfly_tutorial_lesson_heading,
            getString(lesson.title),
            index + 1,
            lessons.size,
          )
        )
        .apply { accessibilityDelegate = Watch(Targets.HEADING, heading = true) }
    content.addView(heading)
    val introText = introOf(lesson)
    val intro = text(introText)
    val instruction = watched(text(""), Targets.INSTRUCTION)
    if (index == 0) {
      // Before Android 13, Android recognizes the double tap itself and activates the focus.
      for (view in listOf(heading, intro, instruction)) {
        view.setOnClickListener { onInput(TutorialInput.Clicked(Targets.INSTRUCTION)) }
      }
    }
    this.instruction = instruction
    content.addView(intro)
    content.addView(instruction)
    addPractice(lesson.practice)
    bottomBar.visibility = View.VISIBLE
    nextButton.setText(R.string.sandfly_tutorial_skip_step)
    lessonText = introText + "\n" + showInstruction()
    scroll.scrollTo(0, 0)
    // When the window opens, SandFly focuses its first item, the heading, itself.
    if (hasWindowFocus()) heading.post { focus(heading) }
    this.heading = heading
  }

  /** Shows the instruction of the current step, and returns it. */
  private fun showInstruction(): String {
    val lesson = lesson ?: return ""
    val step = step
    val text =
      when {
        step == null -> getString(R.string.sandfly_tutorial_lesson_complete)
        lesson.steps.size == 1 -> instructionOf(step)
        else ->
          getString(
            R.string.sandfly_tutorial_step,
            stepIndex + 1,
            lesson.steps.size,
            instructionOf(step),
          )
      }
    instruction?.text = text
    return text
  }

  private fun onInput(input: TutorialInput) {
    userActed()
    speechDelay = SPEECH_DELAY_MS
    val step = step ?: return
    val key = step.check(input, stepContext)
    if (key == null) {
      if (input is TutorialInput.CustomAction) speakLater(getString(input.label))
      return
    }
    if (step.distinct && key in hits) return
    hits += key
    if (hits.size < step.count) {
      speakLater(getString(R.string.sandfly_tutorial_progress, hits.size, step.count))
      return
    }
    // A gesture's action, such as a scroll, can take a while to be said.
    speechDelay = if (input is TutorialInput.Command) GESTURE_SPEECH_DELAY_MS else SPEECH_DELAY_MS
    val done = {
      if (!step.silent) sound()
      nextStep(say = !step.silent)
    }
    // Continuous reading would read the screen as it changes.
    if (reading) afterReading = done else done()
  }

  private fun nextStep(say: Boolean) {
    val lesson = lesson ?: return
    stepIndex++
    hits.clear()
    if (stepIndex >= lesson.steps.size) {
      completeLesson(lesson)
      return
    }
    // SandFly reads the instruction again itself when it has focus and changes.
    val focused = instructionFocused
    val text = showInstruction()
    if (say && !focused) speakLater(text)
  }

  private fun completeLesson(lesson: TutorialLesson) {
    lessonDone = true
    val done = HashSet(prefs().getStringSet(prefDone(), emptySet()) ?: emptySet())
    done += lesson.id
    prefs().edit { putStringSet(prefDone(), done) }
    val next = lessons.getOrNull(lessonIndex + 1)
    nextButton.text =
      if (next == null) getString(R.string.sandfly_tutorial_finish)
      else getString(R.string.sandfly_tutorial_next_lesson, getString(next.title))
    val since = lastActionTime
    speakLater(completionText()) {
      // Unless the user went on with gestures meanwhile.
      if (lastActionTime == since && lessonDone) focus(nextButton)
      // Changed after focus has moved, so that SandFly doesn't read it again.
      if (lessonDone) showInstruction()
    }
  }

  /** Said when the lesson is done: how to go on, as focus moves to the button that does. */
  private fun completionText(): String {
    val activate =
      inputName(
        if (keyboard) R.string.shortcut_value_perform_click_action
        else R.string.shortcut_value_action_setting_activate_current_action
      ) ?: return getString(R.string.sandfly_tutorial_lesson_complete)
    val last = lessonIndex + 1 >= lessons.size
    return getString(
      when {
        keyboard && last -> R.string.sandfly_keyboard_tutorial_lesson_complete_last
        keyboard -> R.string.sandfly_keyboard_tutorial_lesson_complete_next
        last -> R.string.sandfly_tutorial_lesson_complete_last
        else -> R.string.sandfly_tutorial_lesson_complete_next
      },
      activate,
    )
  }

  /** Skips the step, or goes to the next lesson once this one is done. */
  private fun next() {
    if (!lessonDone) {
      nextStep(say = true)
      return
    }
    if (lessonIndex + 1 < lessons.size) {
      openLesson(lessonIndex + 1)
    } else {
      prefs().edit { remove(prefLesson()) }
      finish()
    }
  }

  /**
   * The instruction of [step] with the user's gestures or keys, or what to do when one has none.
   */
  private fun instructionOf(step: TutorialStep): String {
    val names = step.names.map { getString(it) }.toMutableList()
    if (step.listItem > 0) names += getString(R.string.sandfly_tutorial_list_item, step.listItem)
    if (step.systemKey != 0) {
      return getString(step.text, systemKeyName(step.systemKey), *names.toTypedArray())
    }
    inputNames(step.actions)?.let {
      return getString(step.text, *(it + names).toTypedArray())
    }
    if (step.altText != 0) {
      inputNames(step.altActions)?.let {
        return getString(step.altText, *(it + names).toTypedArray())
      }
    }
    val missing = step.actions.first { inputName(it) == null }
    return getString(
      if (keyboard) R.string.sandfly_keyboard_tutorial_no_key
      else R.string.sandfly_tutorial_no_gesture,
      actionName(commandValue(missing)),
    )
  }

  private fun inputNames(actions: List<Int>): List<String>? =
    actions.map { inputName(it) ?: return null }

  /** The keys or the gesture that run [action], as the user has them. */
  private fun inputName(@StringRes action: Int): String? =
    if (keyboard) keyName(action) else gestureName(action)

  /**
   * The value of the command in [res]: a command value, or a keyboard action's key, which stands
   * for the command that the keyboard action runs.
   */
  private fun commandValue(@StringRes res: Int): String {
    val key = getString(res)
    val action = TalkBackPhysicalKeyboardShortcut.getActionFromKey(resources, key)
    return if (action == TalkBackPhysicalKeyboardShortcut.ACTION_UNKNOWN) key
    else Commands.valueOfKeyboardAction(this, action)
  }

  /**
   * The keys that run the command in [action]: its keyboard shortcut, or else the arrow keys that
   * run it while arrow key navigation is on.
   */
  private fun keyName(@StringRes action: Int): String? {
    val manager = CommandBindings.keyComboManager(this)
    val model = manager.keyComboModel ?: return null
    val value = commandValue(action)
    if (value == getString(R.string.shortcut_value_toggle_browse_mode)) {
      // Browse mode turns on and off with the SandFly key pressed twice, or with its own shortcut
      // where the keymap has no double press.
      if (!manager.isBrowseModeSupported) return null
      val twice = model.getKeyComboForKey(getString(R.string.keycombo_shortcut_other_toggle_browse_mode))
      if (twice.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()) {
        return CommandBindings.keyboardShortcutText(
          manager,
          getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra),
        )
      }
      return manager.getKeyComboStringRepresentation(twice)
    }
    val key =
      Commands.keyboardActionFor(this, value)?.getKey(resources)?.takeIf {
        model.keyComboMap.containsKey(it)
      } ?: Commands.shortcutKey(value)
    CommandBindings.keyboardShortcutText(manager, key)?.let {
      return it
    }
    if (!ArrowKeyNav.isOn(this)) return null
    return CommandBindings.arrowKeys(this)
      .firstOrNull { it.value(this, prefs()) == value }
      ?.name
      ?.toString()
  }

  /** Android's own shortcut with the Meta key and [keyCode], such as "Action + H". */
  private fun systemKeyName(keyCode: Int): String =
    CommandBindings.keyComboManager(this)
      .getArrowKeysStringRepresentation(
        KeyCombo.computeKeyComboCode(KeyEvent.META_META_ON, keyCode)
      )

  /** What Android's own shortcut with the Meta key and [keyCode] does. */
  private fun systemKeyLabel(keyCode: Int): String =
    actionName(
      getString(
        when (keyCode) {
          KeyEvent.KEYCODE_ESCAPE -> R.string.shortcut_value_back
          KeyEvent.KEYCODE_H -> R.string.shortcut_value_home
          KeyEvent.KEYCODE_TAB -> R.string.shortcut_value_overview
          else -> R.string.shortcut_value_notifications
        }
      )
    )

  /** The introduction of [lesson], with the key that keyboard shortcuts use where it names it. */
  private fun introOf(lesson: TutorialLesson): String {
    if (!lesson.introNamesKey) return getString(lesson.intro)
    val key = CommandBindings.keyComboManager(this).sandFlyKey ?: SandFlyKey.META
    return getString(
      lesson.intro,
      getString(key.nameRes),
      getString(
        when (key) {
          SandFlyKey.META -> R.string.sandfly_keyboard_tutorial_action_key_location
          SandFlyKey.ALT -> R.string.sandfly_keyboard_tutorial_alt_key_location
          SandFlyKey.CTRL_ALT -> R.string.sandfly_keyboard_tutorial_ctrl_alt_key_location
          SandFlyKey.CTRL_META -> R.string.sandfly_keyboard_tutorial_ctrl_meta_key_location
          SandFlyKey.INSERT -> R.string.sandfly_keyboard_tutorial_insert_key_location
          SandFlyKey.NUMPAD_INSERT -> R.string.sandfly_keyboard_tutorial_numpad_insert_key_location
        }
      ),
    )
  }

  /**
   * The name of a gesture assigned to [action]. One-finger gestures come first, as the easiest to
   * learn, then the others by number of fingers, then fingerprint gestures.
   */
  private fun gestureName(@StringRes action: Int): String? {
    val key = getString(action)
    return mapping
      .getGestureActions()
      .filter { (id, assigned) -> assigned == key && mapping.isSupportedGesture(id) }
      .keys
      .sortedWith(compareBy({ it !in ONE_FINGER_GESTURES }, { it }))
      .firstNotNullOfOrNull { GestureShortcutMapping.getGestureString(this, it) }
      ?: mapping.getGestureFromActionKey(key)
      ?: when (action) {
        // Android recognizes these itself before Android 13, so they have no gesture to name.
        R.string.shortcut_value_action_setting_activate_current_action ->
          getString(R.string.gesture_name_double_tap)
        R.string.shortcut_value_perform_long_click_action ->
          getString(R.string.gesture_name_double_tap_and_hold)
        else -> null
      }
  }

  /** The name of the command saved as [action]. */
  private fun actionName(action: String): String =
    when {
      // Double tap's own action has no name in the list of actions that gestures can have.
      action == getString(R.string.shortcut_value_action_setting_activate_current_action) ->
        getString(R.string.shortcut_perform_click_action)
      keyboard ->
        Commands.label(this, action) ?: GestureShortcutMapping.getActionString(this, action)
      else -> GestureShortcutMapping.getActionString(this, action)
    }

  /**
   * In practice, says what the gesture or keys named [inputName] do. Returns true if it isn't
   * done.
   */
  private fun practice(inputName: String?, action: String): Boolean {
    val actionName = actionName(action)
    val text =
      if (inputName == null) actionName
      else getString(R.string.sandfly_tutorial_practice_gesture, inputName, actionName)
    val performed =
      (if (keyboard) TutorialLessons.KEYBOARD_PRACTICE_PERFORMED
        else TutorialLessons.PRACTICE_PERFORMED)
        .any { commandValue(it) == action }
    if (performed) speakLater(text) else speakNow(text)
    return !performed
  }

  /**
   * Whether Back stays in the lesson, so that a gesture made while learning doesn't leave it.
   * Back still closes the keyboard, and closes the tutorial from Contents.
   */
  private fun keepsBack(): Boolean {
    if (lessonIndex < 0) return false
    val insets = ViewCompat.getRootWindowInsets(window.decorView)
    return insets?.isVisible(WindowInsetsCompat.Type.ime()) != true
  }

  /** Whether [action] leaves the tutorial. */
  private fun leavesLesson(action: String): Boolean =
    OpenAppActions.isAction(action) ||
      TutorialLessons.LEAVING_ACTIONS.any { commandValue(it) == action }

  /** Starts the wait before the instruction is said again, as the user has done something. */
  private fun userActed() {
    repeats = 0
    handler.removeCallbacks(repeatInstruction)
    if (lessonIndex >= 0 && lesson?.id != TutorialLessons.PRACTICE_LESSON) {
      handler.postDelayed(repeatInstruction, REPEAT_MS)
    }
  }

  /** Says the instruction again when nothing has happened for a while, a few times at most. */
  private val repeatInstruction =
    object : Runnable {
      override fun run() {
        if (lessonIndex < 0) return
        if (hasWindowFocus() && !reading) {
          val text = if (lessonDone) completionText() else instruction?.text?.toString()
          if (text != null) speak(text, SpeechController.QUEUE_MODE_QUEUE, null)
          repeats++
        }
        if (repeats < MAX_REPEATS) handler.postDelayed(this, REPEAT_MS)
      }
    }

  // Contents

  private fun showContents() {
    handler.removeCallbacks(repeatInstruction)
    lessonIndex = -1
    instruction = null
    lessonText = null
    afterReading = null
    content.removeAllViews()
    val title = heading(getString(titleRes()))
    content.addView(title)
    content.addView(text(getString(R.string.sandfly_tutorial_contents_intro)))
    val done = prefs().getStringSet(prefDone(), emptySet()) ?: emptySet()
    var chapter = 0
    lessons.forEachIndexed { index, lesson ->
      if (lesson.chapter != chapter) {
        chapter = lesson.chapter
        content.addView(heading(getString(chapter)))
      }
      val button = button(lesson.title) { openLesson(index) }
      if (lesson.id in done) {
        val state = getString(R.string.sandfly_tutorial_done)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          button.stateDescription = state
        } else {
          button.contentDescription = getString(lesson.title) + ", " + state
        }
      }
      content.addView(button)
    }
    content.addView(button(R.string.sandfly_tutorial_close) { finish() })
    bottomBar.visibility = View.GONE
    scroll.scrollTo(0, 0)
    heading = null
    title.post { focus(title) }
  }

  // Practice items

  private fun addPractice(practice: Practice) {
    when (practice) {
      Practice.NONE -> {}
      Practice.TILES -> {
        content.addView(
          row(
            tile(R.string.sandfly_tutorial_tile_sun, Targets.SUN, 0xFFF9A825.toInt()),
            tile(R.string.sandfly_tutorial_tile_moon, Targets.MOON, 0xFF5C6BC0.toInt()),
          )
        )
        content.addView(
          row(
            tile(R.string.sandfly_tutorial_tile_star, Targets.STAR, 0xFF8E24AA.toInt()),
            tile(R.string.sandfly_tutorial_tile_cloud, Targets.CLOUD, 0xFF78909C.toInt()),
          )
        )
      }
      Practice.SHOPPING_LIST -> {
        listOf(
            R.string.sandfly_tutorial_item_apples to Targets.APPLES,
            R.string.sandfly_tutorial_item_bread to Targets.BREAD,
            R.string.sandfly_tutorial_item_cheese to Targets.CHEESE,
            R.string.sandfly_tutorial_item_dates to Targets.DATES,
            R.string.sandfly_tutorial_item_eggs to Targets.EGGS,
          )
          .forEach { (label, target) -> content.addView(watched(text(getString(label)), target)) }
      }
      Practice.CONTROLS -> {
        val rung = text(getString(R.string.sandfly_tutorial_bell_rung, 0))
        var times = 0
        content.addView(
          watched(
            button(R.string.sandfly_tutorial_bell) {
              times++
              rung.text = getString(R.string.sandfly_tutorial_bell_rung, times)
              onInput(TutorialInput.Clicked(Targets.BELL))
            },
            Targets.BELL,
          )
        )
        content.addView(rung)
        content.addView(
          watched(
            CheckBox(this).apply {
              setText(R.string.sandfly_tutorial_extra_cheese)
              setOnCheckedChangeListener { _, checked ->
                onInput(TutorialInput.Checked(Targets.EXTRA_CHEESE, checked))
              }
            },
            Targets.EXTRA_CHEESE,
          )
        )
        val details = text("")
        content.addView(
          watched(
            button(R.string.sandfly_tutorial_details) {}.apply {
              setOnLongClickListener {
                details.setText(R.string.sandfly_tutorial_details_shown)
                onInput(TutorialInput.LongClicked(Targets.DETAILS))
                true
              }
            },
            Targets.DETAILS,
          )
        )
        content.addView(details)
      }
      Practice.STORY ->
        content.addView(watched(text(getString(R.string.sandfly_tutorial_story)), Targets.STORY))
      Practice.SENTENCE ->
        content.addView(
          watched(text(getString(R.string.sandfly_tutorial_sentence)), Targets.SENTENCE)
        )
      Practice.FORECAST -> {
        listOf(
            Triple(
              R.string.sandfly_tutorial_forecast_today,
              R.string.sandfly_tutorial_forecast_today_text,
              Targets.TODAY,
            ),
            Triple(
              R.string.sandfly_tutorial_forecast_tomorrow,
              R.string.sandfly_tutorial_forecast_tomorrow_text,
              Targets.TOMORROW,
            ),
            Triple(
              R.string.sandfly_tutorial_forecast_weekend,
              R.string.sandfly_tutorial_forecast_weekend_text,
              Targets.WEEKEND,
            ),
          )
          .forEach { (title, body, target) ->
            content.addView(
              text(getString(title)).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                accessibilityDelegate = Watch(target, heading = true)
              }
            )
            content.addView(text(getString(body)))
          }
      }
      Practice.RECIPE -> {
        listOf(
            R.string.sandfly_tutorial_recipe_1,
            R.string.sandfly_tutorial_recipe_2,
            R.string.sandfly_tutorial_recipe_3,
            R.string.sandfly_tutorial_recipe_4,
            R.string.sandfly_tutorial_recipe_5,
          )
          .forEachIndexed { index, line ->
            val view = text(getString(line))
            if (index == 0) watched(view, Targets.RECIPE_FIRST)
            content.addView(view)
          }
      }
      Practice.LONG_LIST ->
        content.addView(
          RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@TutorialActivity)
            adapter = LongListAdapter()
          },
          LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(LIST_HEIGHT_DP)),
        )
      Practice.SLIDER -> {
        val label = text(getString(R.string.sandfly_tutorial_brightness))
        content.addView(label)
        content.addView(
          watched(
            SeekBar(this).apply {
              id = View.generateViewId()
              max = SLIDER_MAX
              progress = SLIDER_START
              contentDescription = getString(R.string.sandfly_tutorial_brightness)
              setPadding(dp(16), dp(16), dp(16), dp(16))
              setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                  override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    onInput(TutorialInput.ValueChanged(Targets.SLIDER, value, bar.max))
                  }

                  override fun onStartTrackingTouch(bar: SeekBar) {}

                  override fun onStopTrackingTouch(bar: SeekBar) {}
                }
              )
            },
            Targets.SLIDER,
          )
        )
        label.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      }
      Practice.MESSAGE ->
        content.addView(
          text(getString(R.string.sandfly_tutorial_message)).apply {
            accessibilityDelegate =
              Watch(
                Targets.MESSAGE,
                actions =
                  listOf(
                    R.string.sandfly_tutorial_message_reply,
                    R.string.sandfly_tutorial_message_archive,
                    R.string.sandfly_tutorial_message_delete,
                  ),
              )
          }
        )
      Practice.TEXT_FIELD -> {
        val field =
          KeyboardSafeEditText(this).apply {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setHint(R.string.sandfly_tutorial_name)
            setOnClickListener { onInput(TutorialInput.Clicked(Targets.NAME)) }
            setOnFocusChangeListener { _, hasFocus ->
              if (hasFocus) onInput(TutorialInput.Clicked(Targets.NAME))
            }
            addTextChangedListener(
              object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, a: Int, b: Int, c: Int) {}

                override fun onTextChanged(s: CharSequence, a: Int, b: Int, c: Int) {}

                override fun afterTextChanged(s: Editable) {
                  onInput(TutorialInput.TextChanged(Targets.NAME, s.toString()))
                }
              }
            )
          }
        content.addView(
          text(getString(R.string.sandfly_tutorial_name)).apply {
            labelFor = field.id
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
          }
        )
        content.addView(watched(field, Targets.NAME))
      }
      Practice.END_PRACTICE ->
        content.addView(
          button(R.string.sandfly_tutorial_end_practice) {
            onInput(TutorialInput.Clicked(Targets.END_PRACTICE))
          }
        )
    }
  }

  /** Reports focus on a practice item, and adds its custom actions. */
  private inner class Watch(
    private val target: String,
    private val heading: Boolean = false,
    private val actions: List<Int> = emptyList(),
  ) : View.AccessibilityDelegate() {
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
      super.onInitializeAccessibilityNodeInfo(host, info)
      if (heading) AccessibilityNodeInfoCompat.wrap(info).isHeading = true
      for (label in actions) info.addAction(AccessibilityAction(actionId(label), getString(label)))
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
      if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
        userActed()
        instructionFocused = target == Targets.INSTRUCTION
        if (target == Targets.HEADING) {
          lessonText?.let { speakLater(it) }
          lessonText = null
        }
        if (target.isNotEmpty()) handler.post { onInput(TutorialInput.Focused(target)) }
      } else if (
        action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS &&
          target == Targets.INSTRUCTION
      ) {
        instructionFocused = false
      }
      actions
        .firstOrNull { actionId(it) == action }
        ?.let {
          handler.post { onInput(TutorialInput.CustomAction(target, it)) }
          return true
        }
      return super.performAccessibilityAction(host, action, args)
    }
  }

  private fun actionId(@StringRes label: Int): Int =
    when (label) {
      R.string.sandfly_tutorial_message_reply -> R.id.sandfly_tutorial_action_reply
      R.string.sandfly_tutorial_message_archive -> R.id.sandfly_tutorial_action_archive
      else -> R.id.sandfly_tutorial_action_delete
    }

  /** A list item that reports focus, since RecyclerView gives its items delegates of its own. */
  private class ListItemView(context: Context, private val onFocused: (String) -> Unit) :
    AppCompatTextView(context) {
    var target = ""

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
      if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) onFocused(target)
      return super.performAccessibilityAction(action, arguments)
    }
  }

  private class ItemHolder(val view: ListItemView) : RecyclerView.ViewHolder(view)

  private inner class LongListAdapter : RecyclerView.Adapter<ItemHolder>() {
    override fun getItemCount() = TutorialLessons.LONG_LIST_SIZE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
      ItemHolder(
        ListItemView(parent.context) { target ->
            instructionFocused = false
            handler.post { onInput(TutorialInput.Focused(target)) }
          }
          .apply {
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            layoutParams =
              RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
              )
          }
      )

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
      holder.view.text = getString(R.string.sandfly_tutorial_list_item, position + 1)
      holder.view.target = Targets.listItem(position + 1)
    }
  }

  // Speech

  /** Says [text] after what SandFly says about the gesture or focus that led to it. */
  private fun speakLater(text: CharSequence, then: (() -> Unit)? = null) {
    val say = { speak(text, SpeechController.QUEUE_MODE_QUEUE, then) }
    if (reading) afterReading = say else handler.postDelayed(say, speechDelay)
  }

  private fun speakNow(text: CharSequence) {
    speak(text, SpeechController.QUEUE_MODE_INTERRUPT, null)
  }

  private fun speak(text: CharSequence, queueMode: Int, then: (() -> Unit)?) {
    val feedback = TutorialHooks.feedback
    if (feedback == null) {
      @Suppress("DEPRECATION") window.decorView.announceForAccessibility(text)
      return
    }
    // Forced, as speech the user asks for is: SandFly drops other speech while audio plays.
    val options =
      SpeechController.SpeakOptions.create()
        .setQueueMode(queueMode)
        .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)
    if (then != null) {
      options.setCompletedAction { handler.post { if (!isDestroyed) then() } }
    }
    feedback.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, options))
  }

  private fun sound() {
    TutorialHooks.feedback?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.complete))
  }

  // Views

  private fun <T : View> watched(view: T, target: String): T =
    view.apply { accessibilityDelegate = Watch(target) }

  // Every view has a Watch, so that the tutorial knows when focus leaves the instruction.

  private fun heading(text: CharSequence) =
    TextView(this).apply {
      this.text = text
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
      setPadding(dp(16), dp(12), dp(16), dp(4))
      accessibilityDelegate = Watch("", heading = true)
    }

  private fun text(text: CharSequence) =
    TextView(this).apply {
      this.text = text
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
      setPadding(dp(16), dp(8), dp(16), dp(8))
      accessibilityDelegate = Watch("")
    }

  private fun tile(@StringRes label: Int, target: String, color: Int) =
    watched(
      TextView(this).apply {
        setText(label)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        setTextColor(Color.WHITE)
        setBackgroundColor(color)
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(0, dp(TILE_HEIGHT_DP), 1f)
        setOnClickListener { onInput(TutorialInput.Clicked(target)) }
      },
      target,
    )

  private fun row(vararg views: View) =
    LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      views.forEach { addView(it) }
    }

  private fun button(@StringRes label: Int, onClick: () -> Unit) =
    Button(this).apply {
      setText(label)
      isAllCaps = false
      setOnClickListener { onClick() }
      accessibilityDelegate = Watch("")
    }

  // The tutorial moves focus as a screen reader does: it is part of SandFly.
  @SuppressLint("AccessibilityFocus")
  private fun focus(view: View) {
    view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
  }

  private fun weight() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

  private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

  private fun prefs() = SharedPreferencesUtils.getSharedPreferences(this)

  private fun prefLesson() = if (keyboard) PREF_KEYBOARD_LESSON else PREF_LESSON

  private fun prefDone() = if (keyboard) PREF_KEYBOARD_DONE else PREF_DONE

  private fun titleRes() =
    if (keyboard) R.string.sandfly_keyboard_tutorial_title else R.string.sandfly_tutorial_title

  private fun indexOf(id: String?): Int = lessons.indexOfFirst { it.id == id }

  companion object {
    /** The ID of the lesson to open, instead of the one the tutorial was last on. */
    const val EXTRA_LESSON = "lesson"

    /** Opens the keyboard tutorial. */
    const val EXTRA_KEYBOARD = "keyboard"

    private const val PREF_LESSON = "pref_sandfly_tutorial_lesson"
    private const val PREF_DONE = "pref_sandfly_tutorial_done"
    private const val PREF_KEYBOARD_LESSON = "pref_sandfly_keyboard_tutorial_lesson"
    private const val PREF_KEYBOARD_DONE = "pref_sandfly_keyboard_tutorial_done"

    /** How long a gesture counts as the one that moved focus. */
    private const val RECENT_ACTION_MS = 3000L

    /** Lets SandFly start saying the result of a gesture before the tutorial adds to it. */
    private const val SPEECH_DELAY_MS = 300L
    private const val GESTURE_SPEECH_DELAY_MS = 1000L

    /** How long without any gesture, touch or input before the instruction is said again. */
    private const val REPEAT_MS = 25_000L
    private const val MAX_REPEATS = 3

    /** How long SandFly has to focus the first item of the window when it opens. */
    private const val FIRST_FOCUS_MS = 2500L

    /** One-finger swipes, double tap, and double tap and hold. */
    private val ONE_FINGER_GESTURES =
      AccessibilityService.GESTURE_SWIPE_UP..AccessibilityService.GESTURE_DOUBLE_TAP_AND_HOLD

    private const val SLIDER_MAX = 10
    private const val SLIDER_START = 7
    private const val LIST_HEIGHT_DP = 320
    private const val TILE_HEIGHT_DP = 150

    @JvmStatic
    @JvmOverloads
    fun intent(context: Context, lesson: String? = null): Intent =
      Intent(context, TutorialActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .apply { if (lesson != null) putExtra(EXTRA_LESSON, lesson) }

    /** Opens the keyboard tutorial, at [lesson] if given. */
    @JvmStatic
    @JvmOverloads
    fun keyboardIntent(context: Context, lesson: String? = null): Intent =
      intent(context, lesson).putExtra(EXTRA_KEYBOARD, true)
  }
}
