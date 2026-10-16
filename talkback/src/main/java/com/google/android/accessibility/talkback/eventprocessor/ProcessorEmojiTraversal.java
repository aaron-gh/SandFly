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

package com.google.android.accessibility.talkback.eventprocessor;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.output.EmojiSpeech;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Moves by character over a whole emoji when Backtalk speaks emoji, in apps that move through its
 * parts one at a time. Chrome moves by code point, so a skin tone, or each person in a family and
 * the joiners between them, took a move each. The first move into the emoji names it (see
 * TextEventInterpreter); this asks the app to keep moving, silently, until the cursor is past it.
 */
public class ProcessorEmojiTraversal implements AccessibilityEventListener {

  /** Most moves asked for in a row, more than the parts of any emoji. */
  private static final int MAX_MOVES = 32;

  private final Context context;
  private final Pipeline.FeedbackReturner pipeline;

  /** Moves asked for since the last one that reached the edge of an emoji. */
  private int moves;

  public ProcessorEmojiTraversal(Context context, Pipeline.FeedbackReturner pipeline) {
    this.context = context;
    this.pipeline = pipeline;
  }

  @Override
  public int getEventTypes() {
    return AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    if (EmojiSpeech.engineSpeaksEmoji()
        || !AccessibilityEventUtils.isCharacterTraversalEvent(event)) {
      return;
    }
    CharSequence text = AccessibilityEventUtils.getEventTextOrDescription(event);
    int from = Math.min(event.getFromIndex(), event.getToIndex());
    int to = Math.max(event.getFromIndex(), event.getToIndex());
    if (TextUtils.isEmpty(text) || from < 0 || to > text.length() || from >= to) {
      return;
    }
    boolean forward =
        event.getAction() != AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY;
    int @Nullable [] emoji = EmojiSpeech.emojiAround(context, text, from);
    boolean inside = emoji != null && (forward ? to < emoji[1] : from > emoji[0]);
    if (!inside || moves >= MAX_MOVES) {
      moves = 0;
      return;
    }
    @Nullable AccessibilityNodeInfoCompat source = AccessibilityEventUtils.sourceCompat(event);
    if (source == null) {
      moves = 0;
      return;
    }
    moves++;
    Bundle args = new Bundle();
    args.putInt(
        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER);
    pipeline.returnFeedback(
        eventId,
        Feedback.nodeAction(
            source,
            forward
                ? AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                : AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
            args));
  }
}
