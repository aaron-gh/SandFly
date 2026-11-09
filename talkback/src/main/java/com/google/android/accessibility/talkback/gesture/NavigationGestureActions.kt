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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.input.CursorGranularity
import com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD

/**
 * Gesture actions that move to the next or previous item of one kind, such as a heading, link or
 * word, without changing the reading control. They take the same paths as the keyboard shortcuts
 * for these moves.
 */
object NavigationGestureActions {

  /** What an action moves to. */
  sealed interface Target {
    /** A character, word, line or paragraph, in apps and on web pages. */
    data class Text(val granularity: CursorGranularity) : Target

    /** An element such as a heading or a button: the HTML element on the web, nodes in apps. */
    data class NativeOrWeb(val granularity: CursorGranularity, val htmlTarget: Int) : Target
  }

  /**
   * One assignable action. [value] is saved in the gesture preference, so it must never change
   * once released.
   */
  class Action(
    val value: String,
    @StringRes val labelRes: Int,
    val forward: Boolean,
    val target: Target,
  )

  /** A titled group of actions in the gesture action list. */
  class Category(@StringRes val titleRes: Int, val actions: List<Action>)

  @JvmField
  val CATEGORIES: List<Category> =
    listOf(
      Category(
        R.string.shortcut_title_navigate_by_text,
        pair(
          "CHARACTER",
          R.string.shortcut_previous_character,
          R.string.shortcut_next_character,
          Target.Text(CursorGranularity.CHARACTER),
        ) +
          pair(
            "WORD",
            R.string.shortcut_previous_word,
            R.string.shortcut_next_word,
            Target.Text(CursorGranularity.WORD),
          ) +
          pair(
            "LINE",
            R.string.shortcut_previous_line,
            R.string.shortcut_next_line,
            Target.Text(CursorGranularity.LINE),
          ) +
          pair(
            "PARAGRAPH",
            R.string.shortcut_previous_paragraph,
            R.string.shortcut_next_paragraph,
            Target.Text(CursorGranularity.PARAGRAPH),
          ),
      ),
      Category(
        R.string.shortcut_title_navigate_by_element,
        pair(
          "HEADING",
          R.string.shortcut_previous_heading,
          R.string.shortcut_next_heading,
          Target.NativeOrWeb(CursorGranularity.HEADING, NavigationTarget.TARGET_HEADING),
        ) +
          pair(
            "LINK",
            R.string.shortcut_previous_link,
            R.string.shortcut_next_link,
            Target.NativeOrWeb(CursorGranularity.LINK, NavigationTarget.TARGET_LINK),
          ) +
          pair(
            "CONTROL",
            R.string.shortcut_previous_control,
            R.string.shortcut_next_control,
            Target.NativeOrWeb(CursorGranularity.CONTROL, NavigationTarget.TARGET_CONTROL),
          ) +
          web(
            "LANDMARK",
            R.string.shortcut_previous_landmark,
            R.string.shortcut_next_landmark,
            CursorGranularity.WEB_LANDMARK,
            NavigationTarget.TARGET_HTML_ELEMENT_ARIA_LANDMARK,
          ) +
          web(
            "BUTTON",
            R.string.shortcut_previous_button,
            R.string.shortcut_next_button,
            CursorGranularity.WEB_BUTTON,
            NavigationTarget.TARGET_HTML_ELEMENT_BUTTON,
          ) +
          web(
            "CHECKBOX",
            R.string.shortcut_previous_checkbox,
            R.string.shortcut_next_checkbox,
            CursorGranularity.WEB_CHECKBOX,
            NavigationTarget.TARGET_HTML_ELEMENT_CHECKBOX,
          ) +
          web(
            "RADIO",
            R.string.shortcut_previous_radio,
            R.string.shortcut_next_radio,
            CursorGranularity.WEB_RADIO,
            NavigationTarget.TARGET_HTML_ELEMENT_RADIO,
          ) +
          web(
            "EDIT_FIELD",
            R.string.shortcut_previous_edit_field,
            R.string.shortcut_next_edit_field,
            CursorGranularity.WEB_EDITFIELD,
            NavigationTarget.TARGET_HTML_ELEMENT_EDIT_FIELD,
          ) +
          web(
            "COMBOBOX",
            R.string.shortcut_previous_combobox,
            R.string.shortcut_next_combobox,
            CursorGranularity.WEB_COMBOBOX,
            NavigationTarget.TARGET_HTML_ELEMENT_COMBOBOX,
          ) +
          web(
            "FOCUSABLE_ITEM",
            R.string.shortcut_previous_focusable_item,
            R.string.shortcut_next_focusable_item,
            CursorGranularity.WEB_FOCUSABLE,
            NavigationTarget.TARGET_HTML_ELEMENT_FOCUSABLE_ITEM,
          ) +
          web(
            "GRAPHIC",
            R.string.shortcut_previous_graphic,
            R.string.shortcut_next_graphic,
            CursorGranularity.WEB_GRAPHIC,
            NavigationTarget.TARGET_HTML_ELEMENT_GRAPHIC,
          ) +
          web(
            "LIST",
            R.string.shortcut_previous_list,
            R.string.shortcut_next_list,
            CursorGranularity.WEB_LIST,
            NavigationTarget.TARGET_HTML_ELEMENT_LIST,
          ) +
          web(
            "LIST_ITEM",
            R.string.shortcut_previous_list_item,
            R.string.shortcut_next_list_item,
            CursorGranularity.WEB_LISTITEM,
            NavigationTarget.TARGET_HTML_ELEMENT_LIST_ITEM,
          ) +
          web(
            "TABLE",
            R.string.shortcut_previous_table,
            R.string.shortcut_next_table,
            CursorGranularity.WEB_TABLE,
            NavigationTarget.TARGET_HTML_ELEMENT_TABLE,
          ) +
          web(
            "VISITED_LINK",
            R.string.shortcut_previous_visited_link,
            R.string.shortcut_next_visited_link,
            CursorGranularity.WEB_VISITED_LINK,
            NavigationTarget.TARGET_HTML_ELEMENT_VISITED_LINK,
          ) +
          web(
            "UNVISITED_LINK",
            R.string.shortcut_previous_unvisited_link,
            R.string.shortcut_next_unvisited_link,
            CursorGranularity.WEB_UNVISITED_LINK,
            NavigationTarget.TARGET_HTML_ELEMENT_UNVISITED_LINK,
          ),
      ),
      Category(
        R.string.shortcut_title_navigate_by_heading_level,
        web(
          "HEADING_1",
          R.string.shortcut_previous_heading_1,
          R.string.shortcut_next_heading_1,
          CursorGranularity.WEB_H1,
          NavigationTarget.TARGET_HTML_ELEMENT_HEADING_1,
        ) +
          web(
            "HEADING_2",
            R.string.shortcut_previous_heading_2,
            R.string.shortcut_next_heading_2,
            CursorGranularity.WEB_H2,
            NavigationTarget.TARGET_HTML_ELEMENT_HEADING_2,
          ) +
          web(
            "HEADING_3",
            R.string.shortcut_previous_heading_3,
            R.string.shortcut_next_heading_3,
            CursorGranularity.WEB_H3,
            NavigationTarget.TARGET_HTML_ELEMENT_HEADING_3,
          ) +
          web(
            "HEADING_4",
            R.string.shortcut_previous_heading_4,
            R.string.shortcut_next_heading_4,
            CursorGranularity.WEB_H4,
            NavigationTarget.TARGET_HTML_ELEMENT_HEADING_4,
          ) +
          web(
            "HEADING_5",
            R.string.shortcut_previous_heading_5,
            R.string.shortcut_next_heading_5,
            CursorGranularity.WEB_H5,
            NavigationTarget.TARGET_HTML_ELEMENT_HEADING_5,
          ) +
          web(
            "HEADING_6",
            R.string.shortcut_previous_heading_6,
            R.string.shortcut_next_heading_6,
            CursorGranularity.WEB_H6,
            NavigationTarget.TARGET_HTML_ELEMENT_HEADING_6,
          ),
      ),
    )

  private val byValue: Map<String, Action> =
    CATEGORIES.flatMap { it.actions }.associateBy { it.value }

  /** Returns the action saved as [value], or null when [value] is not one of these actions. */
  @JvmStatic fun find(value: String?): Action? = value?.let { byValue[it] }

  /** Returns whether [value] is one of these actions. */
  @JvmStatic fun isAction(value: String?): Boolean = find(value) != null

  /** Returns the name of the action saved as [value], or null when it is not one of these. */
  @JvmStatic
  fun label(context: Context, value: String?): String? =
    find(value)?.let { context.getString(it.labelRes) }

  /**
   * Moves to the next or previous item for the action saved as [value]. Returns false when nothing
   * moved, so that the caller can play the failure sound.
   */
  @JvmStatic
  fun perform(
    value: String,
    pipeline: Pipeline.FeedbackReturner,
    hasWebContent: Boolean,
    eventId: EventId?,
  ): Boolean {
    val action = find(value) ?: return false
    val direction =
      Feedback.focusDirection(if (action.forward) SEARCH_FOCUS_FORWARD else SEARCH_FOCUS_BACKWARD)
        .setInputMode(INPUT_MODE_TOUCH)
    return when (val target = action.target) {
      is Target.Text -> pipeline.returnFeedback(eventId, direction.setGranularity(target.granularity))
      is Target.NativeOrWeb ->
        if (hasWebContent) {
          pipeline.returnFeedback(eventId, direction.setHtmlTargetType(target.htmlTarget))
        } else {
          pipeline.returnFeedback(
            eventId,
            direction.setGranularity(target.granularity).setWrap(true),
          )
        }
    }
  }

  private fun pair(
    name: String,
    @StringRes previousLabel: Int,
    @StringRes nextLabel: Int,
    target: Target,
  ): List<Action> =
    listOf(
      Action("PREVIOUS_$name", previousLabel, forward = false, target),
      Action("NEXT_$name", nextLabel, forward = true, target),
    )

  private fun web(
    name: String,
    @StringRes previousLabel: Int,
    @StringRes nextLabel: Int,
    granularity: CursorGranularity,
    htmlTarget: Int,
  ): List<Action> = pair(name, previousLabel, nextLabel, Target.NativeOrWeb(granularity, htmlTarget))
}
