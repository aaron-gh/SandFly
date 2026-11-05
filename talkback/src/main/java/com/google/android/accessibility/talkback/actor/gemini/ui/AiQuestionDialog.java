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

package com.google.android.accessibility.talkback.actor.gemini.ui;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.dialog.BaseDialog;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The window of Ask about screen and Ask about image when the question can't be dictated: a text
 * field for a question about a screenshot taken before it opened. Ask sends the question with the
 * screenshot, and the answer is spoken.
 */
public class AiQuestionDialog extends BaseDialog {
  private final Pipeline.FeedbackReturner pipeline;
  private final boolean wholeScreen;
  private byte[] imageBytes;
  private @Nullable EditText questionField;

  public AiQuestionDialog(
      Context context,
      Pipeline.FeedbackReturner pipeline,
      boolean wholeScreen,
      byte[] imageBytes) {
    super(
        context,
        wholeScreen ? R.string.title_ask_about_screen : R.string.title_ask_about_image,
        pipeline);
    this.pipeline = pipeline;
    this.wholeScreen = wholeScreen;
    this.imageBytes = imageBytes;
    setPositiveButtonStringRes(R.string.ai_question_ask);
    setSoftInputMode(true);
    setRestoreFocus(true);
  }

  /** Shows the window with Ask turned off until there is a question. */
  public void show() {
    showDialog();
    setButtonEnabled(DialogInterface.BUTTON_POSITIVE, false);
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    if (buttonClicked == DialogInterface.BUTTON_POSITIVE) {
      ask();
    }
  }

  @Override
  public void handleDialogDismiss() {
    questionField = null;
    imageBytes = null;
  }

  @Override
  public String getMessageString() {
    return null;
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    Context context = inflater.getContext();
    EditText field = new EditText(context);
    field.setHint(R.string.ai_question_hint);
    field.setInputType(
        InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    field.setImeOptions(EditorInfo.IME_ACTION_SEND);
    field.setFilters(
        new InputFilter[] {
          new InputFilter.LengthFilter(GeminiConfiguration.imageQnaTypingSizeLimit(context))
        });
    field.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void afterTextChanged(Editable text) {
            setButtonEnabled(
                DialogInterface.BUTTON_POSITIVE, !TextUtils.isEmpty(text.toString().trim()));
          }

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    field.setOnEditorActionListener(
        (view, actionId, event) -> {
          if (actionId != EditorInfo.IME_ACTION_SEND) {
            return false;
          }
          if (ask()) {
            dismissDialog();
          }
          return true;
        });
    questionField = field;
    field.requestFocus();

    FrameLayout frame = new FrameLayout(context);
    int padding = context.getResources().getDimensionPixelSize(R.dimen.label_dialog_padding);
    frame.setPadding(padding, padding / 2, padding, 0);
    frame.addView(field);
    return frame;
  }

  /** Sends the question, if there is one. Returns whether it was sent. */
  private boolean ask() {
    EditText field = questionField;
    byte[] image = imageBytes;
    if (field == null || image == null) {
      return false;
    }
    String question = field.getText().toString().trim();
    if (question.isEmpty()) {
      return false;
    }
    imageBytes = null;
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        wholeScreen
            ? Feedback.geminiRequestScreenQna(question, image)
            : Feedback.geminiRequestImageQna(question, image));
    return true;
  }
}
