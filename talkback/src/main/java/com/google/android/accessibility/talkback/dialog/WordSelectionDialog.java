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
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.ClipboardHistory;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.accessibility.utils.output.EmojiSpeech;
import com.google.android.accessibility.utils.output.FeedbackItem;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dialog that allows selecting and copying text from the entire screen
 * with granularity options (characters, words, and paragraphs), or selecting
 * words individually from the last spoken phrase.
 */
public class WordSelectionDialog extends BaseDialog {

  public enum Granularity {
    CHARACTERS,
    WORDS,
    PARAGRAPHS
  }

  @VisibleForTesting static final int MAX_TOKENS = 1000;
  private static final int MAX_NODES = 1500;
  private static final int MAX_DEPTH = 50;

  private static final ExecutorService EXECUTOR =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "WordSelection"));

  private final List<String> paragraphs;
  private final List<String> tokens = new ArrayList<>();
  private final BitSet checked = new BitSet();
  @Nullable private final FeedbackReturner pipeline;
  private Granularity currentGranularity = Granularity.WORDS;
  @Nullable private RecyclerView wordsRecyclerView;
  @Nullable private TokenAdapter adapter;
  private final boolean showGranularityOptions;

  public WordSelectionDialog(
      Context context,
      int titleResId,
      List<String> paragraphs,
      @Nullable FeedbackReturner pipeline,
      boolean showGranularityOptions) {
    super(context, titleResId, pipeline);
    this.paragraphs = paragraphs;
    this.pipeline = pipeline;
    this.showGranularityOptions = showGranularityOptions;
    setPositiveButtonStringRes(android.R.string.copy);
    setNegativeButtonStringRes(android.R.string.cancel);
    setNeutralButtonStringRes(R.string.button_select_all);
    setIncludePositiveButton(true);
    setIncludeNegativeButton(true);
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    // Handled by custom button listeners in showDialog().
  }

  @Override
  public void handleDialogDismiss() {
    tokens.clear();
    checked.clear();
    adapter = null;
    wordsRecyclerView = null;
  }

  @Override
  public @Nullable String getMessageString() {
    return null;
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    View view = inflater.inflate(R.layout.word_selection_dialog, null, false);
    TextView granularityLabel = view.findViewById(R.id.granularity_label);
    RadioGroup radioGroup = view.findViewById(R.id.granularity_group);

    if (!showGranularityOptions) {
      if (granularityLabel != null) {
        granularityLabel.setVisibility(View.GONE);
      }
      if (radioGroup != null) {
        radioGroup.setVisibility(View.GONE);
      }
    } else if (radioGroup != null) {
      radioGroup.setOnCheckedChangeListener(
          (group, checkedId) -> {
            if (checkedId == R.id.radio_characters) {
              setGranularity(Granularity.CHARACTERS);
            } else if (checkedId == R.id.radio_paragraphs) {
              setGranularity(Granularity.PARAGRAPHS);
            } else {
              setGranularity(Granularity.WORDS);
            }
          });
    }

    wordsRecyclerView = view.findViewById(R.id.words_recycler_view);
    if (wordsRecyclerView != null) {
      wordsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
      adapter = new TokenAdapter();
      wordsRecyclerView.setAdapter(adapter);
    }

    populateItems(currentGranularity);
    return view;
  }

  private void setGranularity(Granularity granularity) {
    if (currentGranularity == granularity) {
      return;
    }
    currentGranularity = granularity;
    populateItems(granularity);
  }

  private void populateItems(Granularity granularity) {
    tokens.clear();
    tokens.addAll(extractTokens(paragraphs, granularity));
    checked.clear();
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
  }

  public static List<String> extractTokens(List<String> paragraphs, Granularity granularity) {
    List<String> tokens = new ArrayList<>();
    switch (granularity) {
      case PARAGRAPHS:
        for (String p : paragraphs) {
          String trimmed = p.trim();
          if (!trimmed.isEmpty()) {
            tokens.add(trimmed);
            if (tokens.size() >= MAX_TOKENS) {
              return tokens;
            }
          }
        }
        break;
      case CHARACTERS:
        String fullText = String.join("\n\n", paragraphs);
        BreakIterator boundary = BreakIterator.getCharacterInstance();
        boundary.setText(fullText);
        int start = boundary.first();
        for (int end = boundary.next();
            end != BreakIterator.DONE && tokens.size() < MAX_TOKENS;
            start = end, end = boundary.next()) {
          tokens.add(fullText.substring(start, end));
        }
        break;
      case WORDS:
      default:
        for (String p : paragraphs) {
          String[] words = p.split("\\s+");
          for (String word : words) {
            String trimmed = word.trim();
            if (!trimmed.isEmpty()) {
              tokens.add(trimmed);
              if (tokens.size() >= MAX_TOKENS) {
                return tokens;
              }
            }
          }
        }
        break;
    }
    return tokens;
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
              List<String> selected = getSelectedItems();
              if (selected.isEmpty()) {
                if (pipeline != null) {
                  int emptyFeedbackRes =
                      showGranularityOptions
                          ? R.string.no_text_selected
                          : R.string.no_words_selected;
                  pipeline.returnFeedback(
                      EVENT_ID_UNTRACKED,
                      Feedback.speech(context.getString(emptyFeedbackRes)));
                }
                return;
              }

              String delimiter;
              if (currentGranularity == Granularity.PARAGRAPHS) {
                delimiter = "\n\n";
              } else if (currentGranularity == Granularity.WORDS) {
                delimiter = " ";
              } else {
                delimiter = "";
              }

              String joinedText = String.join(delimiter, selected);
              ClipboardHistory.copy(context, joinedText);
              if (pipeline != null) {
                pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.speech(
                        context.getString(R.string.script_tree_copied, selected.size())));
              }
              dialog.dismiss();
            });
      }
    }
    return dialog;
  }

  private void toggleSelectAll() {
    boolean anyUnchecked = checked.cardinality() < tokens.size();
    if (anyUnchecked) {
      checked.set(0, tokens.size());
    } else {
      checked.clear();
    }
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
    if (pipeline != null) {
      String feedback =
          context.getString(anyUnchecked ? R.string.value_selected : R.string.value_not_selected);
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(feedback));
    }
  }

  private List<String> getSelectedItems() {
    List<String> selected = new ArrayList<>();
    for (int i = checked.nextSetBit(0); i >= 0 && i < tokens.size(); i = checked.nextSetBit(i + 1)) {
      selected.add(tokens.get(i));
    }
    return selected;
  }

  /**
   * Shows the word selection dialog for the last spoken utterance.
   *
   * @param service The TalkBackService instance.
   * @param pipeline The feedback returner for speech/audio.
   * @param fromMenu Whether the action was triggered from the context menu (uses saved utterance).
   */
  public static void showForLastUtterance(
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

    List<String> paragraphs = new ArrayList<>();
    paragraphs.add(text);

    WordSelectionDialog dialog =
        new WordSelectionDialog(
            service,
            R.string.title_select_last_spoken_phrase_word_by_word,
            paragraphs,
            pipeline,
            /* showGranularityOptions= */ false);
    if (fromMenu) {
      dialog.setRestoreFocus(true);
    }
    dialog.showDialog();
  }

  /**
   * Shows the screen text copy dialog for the given TalkBackService off the main thread.
   *
   * @param service The TalkBackService instance.
   * @param pipeline The feedback returner for speech/audio.
   * @param fromMenu Whether the action was triggered from the context menu.
   */
  public static void showForScreen(
      TalkBackService service, @Nullable FeedbackReturner pipeline, boolean fromMenu) {
    EXECUTOR.execute(
        () -> {
          List<String> paragraphs = extractScreenParagraphs(service);
          new Handler(service.getMainLooper())
              .post(
                  () -> {
                    if (paragraphs.isEmpty()) {
                      if (pipeline != null) {
                        pipeline.returnFeedback(
                            EVENT_ID_UNTRACKED,
                            Feedback.speech(service.getString(R.string.no_text_on_screen)));
                      }
                      return;
                    }

                    WordSelectionDialog dialog =
                        new WordSelectionDialog(
                            service,
                            R.string.title_copy_text_from_screen,
                            paragraphs,
                            pipeline,
                            /* showGranularityOptions= */ true);
                    if (fromMenu) {
                      dialog.setRestoreFocus(true);
                    }
                    dialog.showDialog();
                  });
        });
  }

  public static List<String> extractScreenParagraphs(TalkBackService service) {
    List<String> paragraphs = new ArrayList<>();
    Set<AccessibilityNodeInfoCompat> visited = new HashSet<>();

    List<AccessibilityWindowInfo> windows = AccessibilityServiceCompatUtils.getWindows(service);
    if (windows != null && !windows.isEmpty()) {
      for (AccessibilityWindowInfo window : windows) {
        if (window == null || WindowUtils.isSystemBar(service, window)) {
          continue;
        }
        int type = window.getType();
        if (type == AccessibilityWindowInfo.TYPE_APPLICATION
            || type == AccessibilityWindowInfo.TYPE_SYSTEM) {
          AccessibilityNodeInfo root = window.getRoot();
          if (root != null) {
            collectTexts(
                AccessibilityNodeInfoCompat.wrap(root), paragraphs, visited, /* depth= */ 0);
          }
        }
      }
    }

    if (paragraphs.isEmpty()) {
      AccessibilityNodeInfo activeRoot = service.getRootInActiveWindow();
      if (activeRoot != null) {
        collectTexts(
            AccessibilityNodeInfoCompat.wrap(activeRoot), paragraphs, visited, /* depth= */ 0);
      }
    }

    return paragraphs;
  }

  private static boolean collectTexts(
      @Nullable AccessibilityNodeInfoCompat node,
      List<String> paragraphs,
      Set<AccessibilityNodeInfoCompat> visited,
      int depth) {
    if (node == null || depth > MAX_DEPTH || visited.size() >= MAX_NODES) {
      return false;
    }
    if (!visited.add(node)) {
      return false;
    }
    if (!node.isVisibleToUser() || node.isPassword()) {
      return false;
    }

    boolean hasText = false;
    CharSequence text = AccessibilityNodeInfoUtils.getText(node);
    if (!TextUtils.isEmpty(text)) {
      String s = text.toString().trim();
      if (!s.isEmpty()) {
        paragraphs.add(s);
        hasText = true;
      }
    }

    boolean descendantHasText = false;
    int childCount = node.getChildCount();
    for (int i = 0; i < childCount; i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child != null) {
        if (collectTexts(child, paragraphs, visited, depth + 1)) {
          descendantHasText = true;
        }
      }
    }

    if (!hasText && !descendantHasText) {
      CharSequence contentDesc = node.getContentDescription();
      if (!TextUtils.isEmpty(contentDesc)) {
        String s = contentDesc.toString().trim();
        if (!s.isEmpty()) {
          paragraphs.add(s);
          return true;
        }
      }
    }

    return hasText || descendantHasText;
  }

  private class TokenAdapter extends RecyclerView.Adapter<TokenViewHolder> {
    private final int minHeight =
        (int) (48 * context.getResources().getDisplayMetrics().density);

    @Override
    public TokenViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      CheckBox checkBox = new CheckBox(context);
      checkBox.setLayoutParams(
          new RecyclerView.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      checkBox.setMinHeight(minHeight);
      checkBox.setTextAppearance(R.style.A11yAlertDialogContentTextStyle);
      return new TokenViewHolder(checkBox);
    }

    @Override
    public void onBindViewHolder(TokenViewHolder holder, int position) {
      holder.checkBox.setOnCheckedChangeListener(null);
      holder.checkBox.setText(tokens.get(position));
      holder.checkBox.setChecked(checked.get(position));
      holder.checkBox.setOnCheckedChangeListener(
          (buttonView, isChecked) -> checked.set(position, isChecked));
    }

    @Override
    public int getItemCount() {
      return tokens.size();
    }
  }

  private static class TokenViewHolder extends RecyclerView.ViewHolder {
    final CheckBox checkBox;

    TokenViewHolder(CheckBox itemView) {
      super(itemView);
      this.checkBox = itemView;
    }
  }
}
