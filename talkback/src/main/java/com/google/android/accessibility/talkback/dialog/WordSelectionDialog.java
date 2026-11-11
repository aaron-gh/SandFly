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
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

  private static final String TAG = "WordSelectionDialog";

  @VisibleForTesting static final int MAX_TOKENS = 1000;
  /** The most text collected from the screen or the last utterance. */
  @VisibleForTesting static final int MAX_TEXT_CHARS = 20_000;
  /** The longest item; longer paragraphs and words are split into several items. */
  @VisibleForTesting static final int MAX_ITEM_CHARS = 300;
  private static final int MAX_NODES = 1500;
  private static final int MAX_DEPTH = 50;

  // One thread, which ends when it has been idle for a while.
  private static final ThreadPoolExecutor EXECUTOR = createExecutor();

  private static ThreadPoolExecutor createExecutor() {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            1,
            1,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
              Thread thread = new Thread(r, "WordSelection");
              thread.setDaemon(true);
              return thread;
            });
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  /** One item of the list. */
  @VisibleForTesting
  static final class Token {
    final String text;
    /**
     * What joins this item to the one before it when both are copied, because both come from one
     * paragraph or word that was too long for one item; null when the granularity's delimiter does.
     */
    @Nullable final String glue;

    Token(String text, @Nullable String glue) {
      this.text = text;
      this.glue = glue;
    }
  }

  /** The items of one granularity, and whether text was left out. */
  @VisibleForTesting
  static final class Tokens {
    final List<Token> items = new ArrayList<>();
    boolean truncated;

    List<String> texts() {
      List<String> texts = new ArrayList<>();
      for (Token token : items) {
        texts.add(token.text);
      }
      return texts;
    }

    boolean isFull() {
      return items.size() >= MAX_TOKENS;
    }
  }

  /** The collected text, and whether the {@link #MAX_TEXT_CHARS} limit cut it off. */
  @VisibleForTesting
  static final class Collected {
    final List<String> paragraphs = new ArrayList<>();
    int chars;
    boolean truncated;

    /** Adds {@code text} within the limit; returns false once the limit is reached. */
    boolean add(String text) {
      int room = MAX_TEXT_CHARS - chars;
      if (room <= 0) {
        truncated = true;
        return false;
      }
      if (text.length() > room) {
        text = text.substring(0, safeEnd(text, room));
        truncated = true;
      }
      paragraphs.add(text);
      chars += text.length();
      return !truncated;
    }
  }

  private final List<String> paragraphs;
  private final boolean textTruncated;
  private final List<Token> tokens = new ArrayList<>();
  private boolean listTruncated;
  @Nullable private TextView truncatedNotice;
  private final BitSet checked = new BitSet();
  @Nullable private final FeedbackReturner pipeline;
  private Granularity currentGranularity = Granularity.WORDS;
  @Nullable private RecyclerView wordsRecyclerView;
  @Nullable private TokenAdapter adapter;
  private final boolean showGranularityOptions;

  public WordSelectionDialog(
      Context context,
      int titleResId,
      Collected collected,
      @Nullable FeedbackReturner pipeline,
      boolean showGranularityOptions) {
    super(context, titleResId, pipeline);
    this.paragraphs = collected.paragraphs;
    this.textTruncated = collected.truncated;
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
    truncatedNotice = null;
  }

  @Override
  public @Nullable String getMessageString() {
    return null;
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    View view = inflater.inflate(R.layout.word_selection_dialog, null, false);
    TextView granularityLabel = view.findViewById(R.id.granularity_label);
    truncatedNotice = view.findViewById(R.id.truncated_notice);
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
    announceTruncated(SpeechController.QUEUE_MODE_INTERRUPT);
  }

  private void populateItems(Granularity granularity) {
    Tokens extracted = extractTokens(paragraphs, granularity);
    tokens.clear();
    tokens.addAll(extracted.items);
    listTruncated = textTruncated || extracted.truncated;
    checked.clear();
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
    if (truncatedNotice != null) {
      truncatedNotice.setText(truncatedText());
      truncatedNotice.setVisibility(listTruncated ? View.VISIBLE : View.GONE);
    }
  }

  private String truncatedText() {
    return context.getString(R.string.word_selection_list_cut_off, tokens.size());
  }

  private void announceTruncated(int queueMode) {
    if (listTruncated && pipeline != null) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(
              truncatedText(), SpeechController.SpeakOptions.create().setQueueMode(queueMode)));
    }
  }

  @VisibleForTesting
  static Tokens extractTokens(List<String> paragraphs, Granularity granularity) {
    Tokens tokens = new Tokens();
    switch (granularity) {
      case PARAGRAPHS:
        for (String p : paragraphs) {
          addPieces(tokens, p.trim(), /* glue= */ " ");
          if (tokens.truncated) {
            break;
          }
        }
        break;
      case CHARACTERS:
        String fullText = String.join("\n\n", paragraphs);
        BreakIterator boundary = BreakIterator.getCharacterInstance();
        boundary.setText(fullText);
        int start = boundary.first();
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
          if (tokens.isFull()) {
            tokens.truncated = true;
            break;
          }
          tokens.items.add(new Token(fullText.substring(start, end), /* glue= */ null));
        }
        break;
      case WORDS:
      default:
        for (String p : paragraphs) {
          int length = p.length();
          int i = 0;
          while (i < length) {
            while (i < length && Character.isWhitespace(p.charAt(i))) {
              i++;
            }
            int wordStart = i;
            while (i < length && !Character.isWhitespace(p.charAt(i))) {
              i++;
            }
            if (i > wordStart) {
              addPieces(tokens, p.substring(wordStart, i), /* glue= */ "");
              if (tokens.truncated) {
                break;
              }
            }
          }
          if (tokens.truncated) {
            break;
          }
        }
        break;
    }
    return tokens;
  }

  /**
   * Adds {@code text} as one item, or as several of at most {@link #MAX_ITEM_CHARS} when it is
   * longer, split at whitespace where there is some. Sets {@link Tokens#truncated} when the list is
   * full before the end of {@code text}.
   */
  private static void addPieces(Tokens tokens, String text, String glue) {
    if (text.isEmpty()) {
      return;
    }
    if (tokens.isFull()) {
      tokens.truncated = true;
      return;
    }
    int start = 0;
    boolean first = true;
    while (start < text.length()) {
      if (tokens.isFull()) {
        tokens.truncated = true;
        return;
      }
      int end = text.length();
      if (end - start > MAX_ITEM_CHARS) {
        end = safeEnd(text, start + MAX_ITEM_CHARS);
        int space = end;
        while (space > start && !Character.isWhitespace(text.charAt(space - 1))) {
          space--;
        }
        if (space > start) {
          end = space;
        }
      }
      String piece = text.substring(start, end).trim();
      if (!piece.isEmpty()) {
        tokens.items.add(new Token(piece, first ? null : glue));
        first = false;
      }
      start = end;
    }
  }

  /** Returns {@code end}, moved back one so that it does not split a surrogate pair. */
  private static int safeEnd(String text, int end) {
    if (end > 0 && end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
      return end - 1;
    }
    return end;
  }

  @Override
  public A11yAlertDialogWrapper showDialog() {
    A11yAlertDialogWrapper dialog = super.showDialog();
    if (dialog != null) {
      announceTruncated(SpeechController.QUEUE_MODE_QUEUE);
      Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
      if (neutralButton != null) {
        neutralButton.setOnClickListener(v -> toggleSelectAll());
      }

      Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      if (positiveButton != null) {
        positiveButton.setOnClickListener(
            v -> {
              if (checked.isEmpty()) {
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

              int count = checked.cardinality();
              ClipboardHistory.copy(context, joinSelected(tokens, checked, currentGranularity));
              if (pipeline != null) {
                pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.speech(
                        context
                            .getResources()
                            .getQuantityString(R.plurals.word_selection_copied, count, count)));
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

  /**
   * Joins the checked items with the granularity's delimiter, or with their own glue where an item
   * continues the one before it and both are checked.
   */
  @VisibleForTesting
  static String joinSelected(List<Token> tokens, BitSet checked, Granularity granularity) {
    String delimiter =
        switch (granularity) {
          case PARAGRAPHS -> "\n\n";
          case WORDS -> " ";
          case CHARACTERS -> "";
        };
    StringBuilder joined = new StringBuilder();
    int previous = -1;
    for (int i = checked.nextSetBit(0); i >= 0 && i < tokens.size(); i = checked.nextSetBit(i + 1)) {
      Token token = tokens.get(i);
      if (previous >= 0) {
        joined.append(token.glue != null && previous == i - 1 ? token.glue : delimiter);
      }
      joined.append(token.text);
      previous = i;
    }
    return joined.toString();
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

    if (text.isEmpty()) {
      if (pipeline != null) {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(service.getString(R.string.no_phrase_to_select)));
      }
      return;
    }

    Collected collected = new Collected();
    collected.add(text);

    WordSelectionDialog dialog =
        new WordSelectionDialog(
            service,
            R.string.title_select_last_spoken_phrase_word_by_word,
            collected,
            pipeline,
            /* showGranularityOptions= */ false);
    if (fromMenu) {
      dialog.setRestoreFocus(true);
    }
    dialog.showDialog();
  }

  /** Collects the text on the screen off the main thread, then shows the dialog. */
  public static void showForScreen(TalkBackService service, @Nullable FeedbackReturner pipeline) {
    EXECUTOR.execute(
        () -> {
          Collected collected;
          try {
            collected = extractScreenParagraphs(service);
          } catch (Throwable t) {
            LogUtils.w(TAG, "Can't collect the text on the screen: %s", t);
            collected = new Collected();
          }
          Collected result = collected;
          new Handler(service.getMainLooper())
              .post(
                  () -> {
                    if (!TalkBackService.isServiceActive()) {
                      return;
                    }
                    if (result.paragraphs.isEmpty()) {
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
                            result,
                            pipeline,
                            /* showGranularityOptions= */ true);
                    dialog.showDialog();
                  });
        });
  }

  private static Collected extractScreenParagraphs(TalkBackService service) {
    Collected paragraphs = new Collected();
    Set<AccessibilityNodeInfoCompat> visited = new HashSet<>();

    List<AccessibilityWindowInfo> windows = AccessibilityServiceCompatUtils.getWindows(service);
    if (windows != null && !windows.isEmpty()) {
      for (AccessibilityWindowInfo window : windows) {
        if (paragraphs.truncated) {
          break;
        }
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

    if (paragraphs.paragraphs.isEmpty()) {
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
      Collected paragraphs,
      Set<AccessibilityNodeInfoCompat> visited,
      int depth) {
    if (node == null
        || depth > MAX_DEPTH
        || visited.size() >= MAX_NODES
        || paragraphs.truncated) {
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
      holder.checkBox.setText(tokens.get(position).text);
      holder.checkBox.setChecked(checked.get(position));
      holder.checkBox.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            int at = holder.getBindingAdapterPosition();
            if (at != RecyclerView.NO_POSITION) {
              checked.set(at, isChecked);
            }
          });
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
