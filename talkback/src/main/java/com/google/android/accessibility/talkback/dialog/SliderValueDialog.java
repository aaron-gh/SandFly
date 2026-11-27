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

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.RESTORE_ON_NEXT_WINDOW;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.SliderSteps;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The window of Enter value, in the actions of a slider: a number field for a value from the
 * minimum to the maximum of the slider's range, shown above it. OK, which is on only while the
 * field holds such a value, sets the slider to it with ACTION_SET_PROGRESS.
 */
public class SliderValueDialog extends BaseDialog {
  private static final String TAG = "SliderValueDialog";

  private final Pipeline.FeedbackReturner pipeline;
  private final AccessibilityNodeInfoCompat slider;
  private final int type;
  private final float min;
  private final float max;
  private final float current;
  private @Nullable EditText valueField;

  /** {@code slider} has a valid range: see {@link SliderSteps#isValidRange}. */
  public SliderValueDialog(
      Context context, Pipeline.FeedbackReturner pipeline, AccessibilityNodeInfoCompat slider) {
    super(context, R.string.title_slider_enter_value, pipeline);
    this.pipeline = pipeline;
    this.slider = slider;
    AccessibilityNodeInfoCompat.@Nullable RangeInfoCompat range = slider.getRangeInfo();
    type = range == null ? AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT : range.getType();
    min = range == null ? 0 : range.getMin();
    max = range == null ? 0 : range.getMax();
    current = range == null ? 0 : range.getCurrent();
    setSoftInputMode(true);
    setRestoreFocus(true);
  }

  /** Shows the window, with OK on while the field holds a value in the range. */
  public void show() {
    showDialog();
    updateOk();
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    if (buttonClicked == DialogInterface.BUTTON_POSITIVE) {
      setValue();
    }
  }

  @Override
  public void handleDialogDismiss() {
    valueField = null;
  }

  @Override
  public String getMessageString() {
    return null;
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    Context context = inflater.getContext();
    TextView range = new TextView(context);
    range.setText(
        context.getString(
            R.string.slider_value_range,
            SliderSteps.format(min, type),
            SliderSteps.format(max, type)));

    EditText field = new EditText(context);
    field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
    field.setContentDescription(context.getString(R.string.title_slider_enter_value));
    int inputType = InputType.TYPE_CLASS_NUMBER;
    if (type != AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT) {
      inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
    }
    if (min < 0) {
      inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
    }
    field.setInputType(inputType);
    field.setImeOptions(EditorInfo.IME_ACTION_DONE);
    field.setFilters(new InputFilter[] {new InputFilter.LengthFilter(SliderSteps.MAX_TYPED_LENGTH)});
    String shown = SliderSteps.format(Math.max(min, Math.min(max, current)), type);
    field.setText(shown);
    field.setSelection(0, shown.length());
    field.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void afterTextChanged(Editable text) {
            updateOk();
          }

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    field.setOnEditorActionListener(
        (view, actionId, event) -> {
          if (actionId != EditorInfo.IME_ACTION_DONE) {
            return false;
          }
          if (typedValue() != null) {
            setValue();
            // As OK does.
            pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(RESTORE_ON_NEXT_WINDOW));
            dismissDialog();
          }
          return true;
        });
    valueField = field;
    field.requestFocus();

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    int padding = context.getResources().getDimensionPixelSize(R.dimen.label_dialog_padding);
    layout.setPadding(padding, padding / 2, padding, 0);
    layout.addView(range);
    layout.addView(field);
    return layout;
  }

  private @Nullable Float typedValue() {
    EditText field = valueField;
    return field == null ? null : SliderSteps.parseTyped(field.getText(), type, min, max);
  }

  private void updateOk() {
    setButtonEnabled(DialogInterface.BUTTON_POSITIVE, typedValue() != null);
  }

  private void setValue() {
    @Nullable Float value = typedValue();
    if (value == null) {
      return;
    }
    valueField = null;
    if (!slider.refresh()) {
      LogUtils.w(TAG, "The slider is no longer on the screen");
      return;
    }
    Bundle args = new Bundle();
    args.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value);
    boolean set =
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED, Feedback.nodeAction(slider, ACTION_SET_PROGRESS.getId(), args));
    LogUtils.d(TAG, "Set the slider to %s: %s", value, set);
  }
}
