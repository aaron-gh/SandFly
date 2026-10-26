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

import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.selector.SelectorController.Setting

/** Something the user did that a tutorial step can wait for. */
internal sealed interface TutorialInput {
  /** A gesture and the action assigned to it, after the action has run or been taken. */
  data class Gesture(val gestureId: Int, val action: String) : TutorialInput

  data class Focused(val target: String) : TutorialInput

  data class Clicked(val target: String) : TutorialInput

  data class LongClicked(val target: String) : TutorialInput

  data class Checked(val target: String, val checked: Boolean) : TutorialInput

  data class TextChanged(val target: String, val text: String) : TutorialInput

  data class ValueChanged(val target: String, val value: Int, val max: Int) : TutorialInput

  data class CustomAction(val target: String, @StringRes val label: Int) : TutorialInput

  data object ReadingStarted : TutorialInput

  /** The tutorial's window has focus again, as after a menu closed. */
  data object WindowFocusReturned : TutorialInput
}

/** What a step check can ask about SandFly's state. */
internal interface StepContext {
  /** Whether [action] is the action key in [key], such as R.string.shortcut_value_next. */
  fun isAction(action: String, @StringRes key: Int): Boolean

  /** Whether the last gesture, made in the last few seconds, had the action key in [key]. */
  fun recentAction(@StringRes key: Int): Boolean

  /** The reading control that is selected. */
  val readingControl: Setting

  fun string(@StringRes id: Int): String
}

/**
 * Returns a key for what [TutorialInput] did toward the step, or null if it did nothing. A step
 * that waits for several things to be found counts the different keys.
 */
internal typealias StepCheck = (TutorialInput, StepContext) -> String?

/** The practice items that a lesson shows below its instruction. */
internal enum class Practice {
  NONE,
  TILES,
  SHOPPING_LIST,
  CONTROLS,
  STORY,
  SENTENCE,
  FORECAST,
  RECIPE,
  LONG_LIST,
  SLIDER,
  MESSAGE,
  TEXT_FIELD,
  END_PRACTICE,
}

/**
 * One instruction of a lesson. [text] has a %n$s placeholder for the gesture of each action in
 * [actions], followed by one for each string in [names] and for list item [listItem]. When one of
 * the actions has no gesture, [altText] with [altActions] is used, if every one of those has a
 * gesture.
 */
internal class TutorialStep(
  @StringRes val text: Int,
  val actions: List<Int> = emptyList(),
  val names: List<Int> = emptyList(),
  val listItem: Int = 0,
  @StringRes val altText: Int = 0,
  val altActions: List<Int> = emptyList(),
  /** Actions that the tutorial takes during this step: it says what they do instead. */
  val taken: List<Int> = emptyList(),
  /** How many times the check must succeed, with different keys when [distinct]. */
  val count: Int = 1,
  val distinct: Boolean = false,
  /** Says nothing when the step is done, because speech is paused or reading continues. */
  val silent: Boolean = false,
  val check: StepCheck,
)

internal class TutorialLesson(
  val id: String,
  @StringRes val chapter: Int,
  @StringRes val title: Int,
  @StringRes val intro: Int,
  val practice: Practice,
  val steps: List<TutorialStep>,
)

/** Practice item keys. */
internal object Targets {
  const val HEADING = "heading"
  const val INSTRUCTION = "instruction"
  const val SUN = "sun"
  const val MOON = "moon"
  const val STAR = "star"
  const val CLOUD = "cloud"
  const val APPLES = "apples"
  const val BREAD = "bread"
  const val CHEESE = "cheese"
  const val DATES = "dates"
  const val EGGS = "eggs"
  const val BELL = "bell"
  const val EXTRA_CHEESE = "extra_cheese"
  const val DETAILS = "details"
  const val STORY = "story"
  const val SENTENCE = "sentence"
  const val TODAY = "today"
  const val TOMORROW = "tomorrow"
  const val WEEKEND = "weekend"
  const val RECIPE_FIRST = "recipe_1"
  const val LIST = "list"
  const val SLIDER = "slider"
  const val MESSAGE = "message"
  const val NAME = "name"
  const val END_PRACTICE = "end_practice"

  /** The list item with [number], counting from 1. */
  fun listItem(number: Int) = "item_$number"
}

internal object TutorialLessons {
  private fun gesture(@StringRes vararg actions: Int): StepCheck = { input, c ->
    if (input is TutorialInput.Gesture && actions.any { c.isAction(input.action, it) }) "" else null
  }

  private fun focused(target: String): StepCheck = { input, _ ->
    if (input is TutorialInput.Focused && input.target == target) target else null
  }

  /** Focus moved to [target] right after a gesture with one of [actions]. */
  private fun focusedBy(target: String, @StringRes vararg actions: Int): StepCheck = { input, c ->
    if (
      input is TutorialInput.Focused &&
        input.target == target &&
        actions.any { c.recentAction(it) }
    )
      target
    else null
  }

  private fun clicked(target: String): StepCheck = { input, _ ->
    if (input is TutorialInput.Clicked && input.target == target) target else null
  }

  /** A gesture with one of [actions] left [setting] selected. */
  private fun readingControl(setting: Setting, @StringRes vararg actions: Int): StepCheck =
    { input, c ->
      if (
        input is TutorialInput.Gesture &&
          actions.any { c.isAction(input.action, it) } &&
          c.readingControl == setting
      )
        ""
      else null
    }

  private val readingControlActions =
    listOf(R.string.shortcut_value_select_next_setting, R.string.shortcut_value_select_previous_setting)

  private val adjustActions =
    listOf(
      R.string.shortcut_value_selected_setting_previous_action,
      R.string.shortcut_value_selected_setting_next_action,
    )

  val all: List<TutorialLesson> =
    listOf(
      TutorialLesson(
        id = "welcome",
        chapter = R.string.sandfly_tutorial_chapter_basics,
        title = R.string.sandfly_tutorial_welcome_title,
        intro = R.string.sandfly_tutorial_welcome_intro,
        practice = Practice.NONE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_welcome_start,
              actions = listOf(R.string.shortcut_value_action_setting_activate_current_action),
            ) { input, c ->
              gesture(R.string.shortcut_value_action_setting_activate_current_action)(input, c)
                ?: clicked(Targets.INSTRUCTION)(input, c)
            }
          ),
      ),
      TutorialLesson(
        id = "explore",
        chapter = R.string.sandfly_tutorial_chapter_basics,
        title = R.string.sandfly_tutorial_explore_title,
        intro = R.string.sandfly_tutorial_explore_intro,
        practice = Practice.TILES,
        steps =
          listOf(
            TutorialStep(R.string.sandfly_tutorial_explore_find, count = 4, distinct = true) {
              input,
              c ->
              // Found by touch, not by swiping.
              if (
                input is TutorialInput.Focused &&
                  input.target in listOf(Targets.SUN, Targets.MOON, Targets.STAR, Targets.CLOUD) &&
                  !c.recentAction(R.string.shortcut_value_next) &&
                  !c.recentAction(R.string.shortcut_value_previous)
              )
                input.target
              else null
            },
            TutorialStep(
              R.string.sandfly_tutorial_explore_activate,
              actions = listOf(R.string.shortcut_value_action_setting_activate_current_action),
              check = clicked(Targets.STAR),
            ),
          ),
      ),
      TutorialLesson(
        id = "move",
        chapter = R.string.sandfly_tutorial_chapter_basics,
        title = R.string.sandfly_tutorial_move_title,
        intro = R.string.sandfly_tutorial_move_intro,
        practice = Practice.SHOPPING_LIST,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_move_next,
              actions = listOf(R.string.shortcut_value_next),
              names = listOf(R.string.sandfly_tutorial_item_eggs),
              check = focusedBy(Targets.EGGS, R.string.shortcut_value_next),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_move_previous,
              actions = listOf(R.string.shortcut_value_previous),
              names = listOf(R.string.sandfly_tutorial_item_bread),
              check = focusedBy(Targets.BREAD, R.string.shortcut_value_previous),
            ),
          ),
      ),
      TutorialLesson(
        id = "activate",
        chapter = R.string.sandfly_tutorial_chapter_basics,
        title = R.string.sandfly_tutorial_activate_title,
        intro = R.string.sandfly_tutorial_activate_intro,
        practice = Practice.CONTROLS,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_activate_button,
              actions = listOf(R.string.shortcut_value_action_setting_activate_current_action),
              names = listOf(R.string.sandfly_tutorial_bell),
              check = clicked(Targets.BELL),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_activate_check_box,
              names = listOf(R.string.sandfly_tutorial_extra_cheese),
            ) { input, _ ->
              if (
                input is TutorialInput.Checked &&
                  input.target == Targets.EXTRA_CHEESE &&
                  input.checked
              )
                ""
              else null
            },
            TutorialStep(
              R.string.sandfly_tutorial_activate_hold,
              actions = listOf(R.string.shortcut_value_perform_long_click_action),
              names = listOf(R.string.sandfly_tutorial_details),
            ) { input, _ ->
              if (input is TutorialInput.LongClicked && input.target == Targets.DETAILS) "" else null
            },
          ),
      ),
      TutorialLesson(
        id = "pause",
        chapter = R.string.sandfly_tutorial_chapter_basics,
        title = R.string.sandfly_tutorial_pause_title,
        intro = R.string.sandfly_tutorial_pause_intro,
        practice = Practice.STORY,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_pause_pause,
              actions = listOf(R.string.shortcut_value_pause_or_resume_feedback),
              silent = true,
              check = gesture(R.string.shortcut_value_pause_or_resume_feedback),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_pause_resume,
              actions = listOf(R.string.shortcut_value_pause_or_resume_feedback),
              check = gesture(R.string.shortcut_value_pause_or_resume_feedback),
            ),
          ),
      ),
      TutorialLesson(
        id = "reading_controls",
        chapter = R.string.sandfly_tutorial_chapter_reading,
        title = R.string.sandfly_tutorial_reading_controls_title,
        intro = R.string.sandfly_tutorial_reading_controls_intro,
        practice = Practice.SENTENCE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_reading_controls_move,
              check = focused(Targets.SENTENCE),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_choose_reading_control,
              actions = readingControlActions,
              names = listOf(R.string.granularity_word),
              check = readingControl(Setting.GRANULARITY_WORDS, *readingControlActions.toIntArray()),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_reading_controls_next_word,
              actions = listOf(R.string.shortcut_value_selected_setting_next_action),
              count = 3,
              check =
                readingControl(
                  Setting.GRANULARITY_WORDS,
                  R.string.shortcut_value_selected_setting_next_action,
                ),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_reading_controls_characters,
              actions = listOf(R.string.shortcut_value_selected_setting_next_action),
              names = listOf(R.string.granularity_character),
              check =
                readingControl(
                  Setting.GRANULARITY_CHARACTERS,
                  R.string.shortcut_value_selected_setting_next_action,
                ),
            ),
          ),
      ),
      TutorialLesson(
        id = "headings",
        chapter = R.string.sandfly_tutorial_chapter_reading,
        title = R.string.sandfly_tutorial_headings_title,
        intro = R.string.sandfly_tutorial_headings_intro,
        practice = Practice.FORECAST,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_choose_reading_control,
              actions = readingControlActions,
              names = listOf(R.string.granularity_native_heading),
              check =
                readingControl(Setting.GRANULARITY_HEADINGS, *readingControlActions.toIntArray()),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_headings_jump,
              actions = listOf(R.string.shortcut_value_selected_setting_next_action),
              names = listOf(R.string.sandfly_tutorial_forecast_weekend),
            ) { input, c ->
              if (
                input is TutorialInput.Focused &&
                  input.target == Targets.WEEKEND &&
                  c.readingControl == Setting.GRANULARITY_HEADINGS
              )
                ""
              else null
            },
          ),
      ),
      TutorialLesson(
        id = "continuous_reading",
        chapter = R.string.sandfly_tutorial_chapter_reading,
        title = R.string.sandfly_tutorial_continuous_title,
        intro = R.string.sandfly_tutorial_continuous_intro,
        practice = Practice.RECIPE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_continuous_move,
              check = focused(Targets.RECIPE_FIRST),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_continuous_read,
              actions = listOf(R.string.shortcut_value_read_from_current),
              names = listOf(R.string.shortcut_read_from_current),
              altText = R.string.sandfly_tutorial_continuous_read_from_menu,
              altActions = listOf(R.string.shortcut_value_talkback_breakout),
            ) { input, _ ->
              if (input == TutorialInput.ReadingStarted) "" else null
            },
          ),
      ),
      TutorialLesson(
        id = "scrolling",
        chapter = R.string.sandfly_tutorial_chapter_controls,
        title = R.string.sandfly_tutorial_scrolling_title,
        intro = R.string.sandfly_tutorial_scrolling_intro,
        practice = Practice.LONG_LIST,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_scrolling_swipe,
              actions = listOf(R.string.shortcut_value_next),
              listItem = SCROLL_TARGET,
              check = focused(Targets.listItem(SCROLL_TARGET)),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_scrolling_page,
              actions = listOf(R.string.shortcut_value_scroll_forward),
              check = gesture(R.string.shortcut_value_scroll_forward),
            ),
          ),
      ),
      TutorialLesson(
        id = "sliders",
        chapter = R.string.sandfly_tutorial_chapter_controls,
        title = R.string.sandfly_tutorial_sliders_title,
        intro = R.string.sandfly_tutorial_sliders_intro,
        practice = Practice.SLIDER,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_sliders_change,
              actions = adjustActions,
              names = listOf(R.string.sandfly_tutorial_brightness),
            ) {
              input,
              _ ->
              if (input is TutorialInput.ValueChanged && input.target == Targets.SLIDER) "" else null
            },
            TutorialStep(
              R.string.sandfly_tutorial_sliders_highest,
              names = listOf(R.string.sandfly_tutorial_brightness),
            ) {
              input,
              _ ->
              if (
                input is TutorialInput.ValueChanged &&
                  input.target == Targets.SLIDER &&
                  input.value == input.max
              )
                ""
              else null
            },
          ),
      ),
      TutorialLesson(
        id = "actions",
        chapter = R.string.sandfly_tutorial_chapter_controls,
        title = R.string.sandfly_tutorial_actions_title,
        intro = R.string.sandfly_tutorial_actions_intro,
        practice = Practice.MESSAGE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_actions_archive,
              actions = adjustActions + R.string.shortcut_value_action_setting_activate_current_action,
              names = listOf(R.string.sandfly_tutorial_message_archive),
            ) { input, _ ->
              if (
                input is TutorialInput.CustomAction &&
                  input.target == Targets.MESSAGE &&
                  input.label == R.string.sandfly_tutorial_message_archive
              )
                ""
              else null
            }
          ),
      ),
      TutorialLesson(
        id = "typing",
        chapter = R.string.sandfly_tutorial_chapter_controls,
        title = R.string.sandfly_tutorial_typing_title,
        intro = R.string.sandfly_tutorial_typing_intro,
        practice = Practice.TEXT_FIELD,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_typing_open,
              actions = listOf(R.string.shortcut_value_action_setting_activate_current_action),
              names = listOf(R.string.sandfly_tutorial_name),
              check = clicked(Targets.NAME),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_typing_type,
              names = listOf(R.string.sandfly_tutorial_typing_word),
            ) { input, c ->
              if (
                input is TutorialInput.TextChanged &&
                  input.target == Targets.NAME &&
                  input.text
                    .trim()
                    .equals(c.string(R.string.sandfly_tutorial_typing_word), ignoreCase = true)
              )
                ""
              else null
            },
            TutorialStep(R.string.sandfly_tutorial_typing_delete) { input, c ->
              if (
                input is TutorialInput.TextChanged &&
                  input.target == Targets.NAME &&
                  input.text
                    .trim()
                    .equals(
                      c.string(R.string.sandfly_tutorial_typing_word).dropLast(1),
                      ignoreCase = true,
                    )
              )
                ""
              else null
            },
          ),
      ),
      TutorialLesson(
        id = "system",
        chapter = R.string.sandfly_tutorial_chapter_getting_around,
        title = R.string.sandfly_tutorial_system_title,
        intro = R.string.sandfly_tutorial_system_intro,
        practice = Practice.NONE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_system_back,
              actions = listOf(R.string.shortcut_value_back),
              taken = listOf(R.string.shortcut_value_back),
              check = gesture(R.string.shortcut_value_back),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_system_home,
              actions = listOf(R.string.shortcut_value_home),
              taken = listOf(R.string.shortcut_value_home),
              check = gesture(R.string.shortcut_value_home),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_system_overview,
              actions = listOf(R.string.shortcut_value_overview),
              taken = listOf(R.string.shortcut_value_overview),
              check = gesture(R.string.shortcut_value_overview),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_system_notifications,
              actions = listOf(R.string.shortcut_value_notifications),
              taken = listOf(R.string.shortcut_value_notifications),
              check = gesture(R.string.shortcut_value_notifications),
            ),
          ),
      ),
      TutorialLesson(
        id = "menu",
        chapter = R.string.sandfly_tutorial_chapter_getting_around,
        title = R.string.sandfly_tutorial_menu_title,
        intro = R.string.sandfly_tutorial_menu_intro,
        practice = Practice.NONE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_menu_open,
              actions =
                listOf(R.string.shortcut_value_talkback_breakout, R.string.shortcut_value_back),
              silent = true,
              check = gesture(R.string.shortcut_value_talkback_breakout),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_menu_close,
              actions = listOf(R.string.shortcut_value_back),
            ) { input, c ->
              if (input == TutorialInput.WindowFocusReturned) ""
              else gesture(R.string.shortcut_value_back)(input, c)
            },
          ),
      ),
      TutorialLesson(
        id = "commands",
        chapter = R.string.sandfly_tutorial_chapter_getting_around,
        title = R.string.sandfly_tutorial_commands_title,
        intro = R.string.sandfly_tutorial_commands_intro,
        practice = Practice.NONE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_commands_status,
              actions = listOf(R.string.shortcut_value_announce_status),
              check = gesture(R.string.shortcut_value_announce_status),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_commands_copy,
              actions = listOf(R.string.shortcut_value_copy_last_spoken_phrase),
              check = gesture(R.string.shortcut_value_copy_last_spoken_phrase),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_commands_search,
              actions = listOf(R.string.shortcut_value_screen_search),
              taken = listOf(R.string.shortcut_value_screen_search),
              check = gesture(R.string.shortcut_value_screen_search),
            ),
            TutorialStep(
              R.string.sandfly_tutorial_commands_speech_off,
              actions = listOf(R.string.shortcut_value_toggle_voice_feedback),
              taken = listOf(R.string.shortcut_value_toggle_voice_feedback),
              check = gesture(R.string.shortcut_value_toggle_voice_feedback),
            ),
          ),
      ),
      TutorialLesson(
        id = "practice",
        chapter = R.string.sandfly_tutorial_chapter_practice,
        title = R.string.sandfly_tutorial_practice_title,
        intro = R.string.sandfly_tutorial_practice_intro,
        practice = Practice.END_PRACTICE,
        steps =
          listOf(
            TutorialStep(
              R.string.sandfly_tutorial_practice_step,
              names = listOf(R.string.sandfly_tutorial_end_practice),
              check = clicked(Targets.END_PRACTICE),
            )
          ),
      ),
    )

  /** The lesson whose gestures are said instead of done, apart from moving and activating. */
  const val PRACTICE_LESSON = "practice"

  /** The actions that gesture practice still performs, so that End practice can be reached. */
  val PRACTICE_PERFORMED =
    listOf(
      R.string.shortcut_value_next,
      R.string.shortcut_value_previous,
      R.string.shortcut_value_action_setting_activate_current_action,
    )

  /**
   * Actions that a lesson takes and doesn't do, unless a step teaches them, as they leave the
   * tutorial. Back is taken too, apart from closing the keyboard.
   */
  val LEAVING_ACTIONS =
    listOf(
      R.string.shortcut_value_home,
      R.string.shortcut_value_overview,
      R.string.shortcut_value_notifications,
      R.string.shortcut_value_system_notifications,
      R.string.shortcut_value_quick_settings,
      R.string.shortcut_value_system_quick_settings,
      R.string.shortcut_value_all_apps,
      R.string.shortcut_value_lock_screen,
      R.string.shortcut_value_power_menu,
      R.string.shortcut_value_a11y_button,
      R.string.shortcut_value_a11y_button_long_press,
      R.string.shortcut_value_tutorial,
      R.string.shortcut_value_practice_gestures,
    )

  /** Actions that a lesson takes in the same way, as they turn speech or SandFly off. */
  val SILENCING_ACTIONS =
    listOf(R.string.shortcut_value_toggle_voice_feedback, R.string.shortcut_value_pause_backtalk)

  /** The list item that the scrolling lesson moves to. */
  const val SCROLL_TARGET = 15

  const val LONG_LIST_SIZE = 40

  fun indexOf(id: String?): Int = all.indexOfFirst { it.id == id }
}
