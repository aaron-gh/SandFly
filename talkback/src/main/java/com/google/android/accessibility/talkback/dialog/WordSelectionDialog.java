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

package com.google.android.accessibility.talkback.dialog;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.ClipboardHistory;
import com.google.android.accessibility.utils.EmojiSpeech;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.accessibility.utils.output.FeedbackItem;
import java.util.ArrayList;
import java.util.List;

/** Dialog that allows selecting and copying individual words from the last spoken phrase. */
public class WordSelectionDialog extends BaseDialog {

  private final List<String> words;
  private final List<CheckBox> checkBoxes = new ArrayList<>();
  @Nullable private final FeedbackReturner pipeline;

  public WordSelectionDialog(
      Context context,
      List<String> words,
      @Nullable FeedbackReturner pipeline) {
    super(context, R.string.title_select_last_spoken_phrase_word_by_word, pipeline);
    this.words = words;
    this.pipeline = pipeline;
    setPositiveButtonStringRes(android.R.string.copy);
    setNegativeButtonStringRes(android.R.string.cancel);
    setNeutralButtonStringRes(R.string.button_select_all_words);
    setIncludePositiveButton(true);
    setIncludeNegativeButton(true);
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    // Handled by custom button listeners in showDialog().
  }

  @Override
  public void handleDialogDismiss() {
    checkBoxes.clear();
  }

  @Override
  public @Nullable String getMessageString() {
    return null;
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    View view = inflater.inflate(R.layout.word_selection_dialog, null, false);
    LinearLayout container = view.findViewById(R.id.words_container);
    checkBoxes.clear();

    int minHeight = (int) (48 * context.getResources().getDisplayMetrics().density);

    for (String word : words) {
      CheckBox checkBox = new CheckBox(context);
      checkBox.setLayoutParams(
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
      checkBox.setMinHeight(minHeight);
      checkBox.setText(word);
      checkBox.setTextAppearance(R.style.A11yAlertDialogContentTextStyle);
      checkBoxes.add(checkBox);
      container.addView(checkBox);
    }

    return view;
  }

  @Override
  public A11yAlertDialogWrapper showDialog() {
    A11yAlertDialogWrapper dialog = super.showDialog();
    if (dialog != null) {
      Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
      if (neutralButton != null) {
        neutralButton.setOnClickListener(v -> toggleSelectAll());
      }

      Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      if (positiveButton != null) {
        positiveButton.setOnClickListener(
            v -> {
              List<String> selectedWords = getSelectedWords();
              if (selectedWords.isEmpty()) {
                if (pipeline != null) {
                  pipeline.returnFeedback(
                      EVENT_ID_UNTRACKED,
                      Feedback.speech(context.getString(R.string.no_words_selected)));
                }
                return;
              }

              String joinedText = TextUtils.join(" ", selectedWords);
              ClipboardHistory.copy(context, joinedText);
              if (pipeline != null) {
                pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.speech(context.getString(R.string.template_text_copied, joinedText)));
              }
              dialog.dismiss();
            });
      }
    }
    return dialog;
  }

  private void toggleSelectAll() {
    boolean anyUnchecked = false;
    for (CheckBox cb : checkBoxes) {
      if (!cb.isChecked()) {
        anyUnchecked = true;
        break;
      }
    }
    for (CheckBox cb : checkBoxes) {
      cb.setChecked(anyUnchecked);
    }
  }

  private List<String> getSelectedWords() {
    List<String> selected = new ArrayList<>();
    for (CheckBox cb : checkBoxes) {
      if (cb.isChecked()) {
        selected.add(cb.getText().toString());
      }
    }
    return selected;
  }

  /**
   * Shows the word selection dialog for the given TalkBackService.
   *
   * @param service The TalkBackService instance.
   * @param pipeline The feedback returner for speech/audio.
   * @param fromMenu Whether the action was triggered from the context menu (uses saved utterance).
   */
  public static void showForService(
      TalkBackService service, @Nullable FeedbackReturner pipeline, boolean fromMenu) {
    FeedbackItem item = null;
    if (fromMenu && service.getSpeechController() != null) {
      item = service.getSpeechController().getSavedUtterance();
    }
    if (item == null && service.getSpeechController() != null) {
      item = service.getSpeechController().getLastUtterance();
    }

    CharSequence copyableText =
        (item != null)
            ? EmojiSpeech.restoreOriginalText(SpannableUtils.getCopyableText(item.getAggregateText()))
            : null;
    String text = (copyableText != null) ? copyableText.toString().trim() : "";

    if (TextUtils.isEmpty(text)) {
      if (pipeline != null) {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(service.getString(R.string.no_phrase_to_select)));
      }
      return;
    }

    String[] tokens = text.split("\\s+");
    List<String> words = new ArrayList<>();
    for (String token : tokens) {
      String trimmed = token.trim();
      if (!trimmed.isEmpty()) {
        words.add(trimmed);
      }
    }

    if (words.isEmpty()) {
      if (pipeline != null) {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(service.getString(R.string.no_phrase_to_select)));
      }
      return;
    }

    WordSelectionDialog dialog = new WordSelectionDialog(service, words, pipeline);
    dialog.showDialog();
  }
}
