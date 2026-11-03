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

package com.google.android.accessibility.talkback.menurules;

import static com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.ViewConfiguration;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.contextmenu.ContextMenu;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.utils.TelegramReplyQuote;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Go to quoted message, on a Telegram message that replies to another. The reply quote is read with
 * the message and isn't focused on its own, so this action clicks it, which shows the quoted
 * message, and then focuses the quoted message.
 *
 * <p>Some Telegram forks ignore the accessibility click on the quote. When the chat hasn't moved a
 * moment after the click, the quote is tapped as a finger would, through the pass-through region.
 */
public class QuotedMessageMenu implements NodeMenu {

  private static final String TAG = "QuotedMessageMenu";

  /** How long the chat takes to start moving to the quoted message. */
  private static final long JUMP_WAIT_MS = 700;

  /** How long Android takes to apply the pass-through region. */
  private static final long PASS_THROUGH_WAIT_MS = 350;

  /** How often, and how far apart, the quoted message is looked for after the jump. */
  private static final int FOCUS_ATTEMPTS = 4;

  private static final long FOCUS_RETRY_MS = 500;

  private final Pipeline.FeedbackReturner pipeline;
  private final Handler handler = new Handler(Looper.getMainLooper());

  public QuotedMessageMenu(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return TelegramReplyQuote.isTelegram(node.getPackageName())
        && TelegramReplyQuote.quoteNodeOf(node) != null;
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> items = new ArrayList<>();
    @Nullable AccessibilityNodeInfoCompat quote = TelegramReplyQuote.quoteNodeOf(node);
    if (quote == null) {
      return items;
    }
    ContextMenuItem item =
        ContextMenu.createMenuItem(
            context,
            Menu.NONE,
            R.id.go_to_quoted_message,
            Menu.NONE,
            context.getString(R.string.title_go_to_quoted_message));
    item.setOnMenuItemClickListener(
        menuItem -> {
          // The message list may have been refreshed since the menu opened.
          @Nullable AccessibilityNodeInfoCompat current =
              node.refresh() ? TelegramReplyQuote.quoteNodeOf(node) : null;
          goToQuotedMessage(node, current != null ? current : quote);
          return true;
        });
    items.add(item);
    return items;
  }

  private void goToQuotedMessage(
      AccessibilityNodeInfoCompat message, AccessibilityNodeInfoCompat quote) {
    TelegramReplyQuote.@Nullable Quote quoted = TelegramReplyQuote.parse(quote.getContentDescription());
    @Nullable AccessibilityNodeInfoCompat list = message.getParent();
    Rect messageBefore = new Rect();
    message.getBoundsInScreen(messageBefore);
    Rect quoteBounds = new Rect();
    quote.getBoundsInScreen(quoteBounds);
    boolean clicked = quote.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
    LogUtils.d(TAG, "Clicked the reply quote: %s, quote bounds %s", clicked, quoteBounds);
    handler.postDelayed(
        () -> {
          Rect messageAfter = new Rect();
          boolean moved = !message.refresh();
          if (!moved) {
            message.getBoundsInScreen(messageAfter);
            moved = !messageAfter.equals(messageBefore);
          }
          long wait = 0;
          if (!moved) {
            LogUtils.d(TAG, "The chat didn't move after the click; tapping the reply quote");
            if (tap(quoteBounds.centerX(), quoteBounds.centerY())) {
              wait = JUMP_WAIT_MS;
            }
          }
          if (list != null && quoted != null) {
            handler.postDelayed(
                () -> focusQuotedMessage(list, message, quoted, FOCUS_ATTEMPTS), wait);
          }
        },
        JUMP_WAIT_MS);
  }

  /** Focuses the quoted message once Telegram has scrolled to it, trying [attempts] times. */
  private void focusQuotedMessage(
      AccessibilityNodeInfoCompat list,
      AccessibilityNodeInfoCompat reply,
      TelegramReplyQuote.Quote quoted,
      int attempts) {
    @Nullable AccessibilityNodeInfoCompat target =
        list.refresh() ? TelegramReplyQuote.quotedMessageIn(list, reply, quoted) : null;
    if (target != null) {
      LogUtils.d(TAG, "Focusing the quoted message");
      target.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
      return;
    }
    if (attempts > 1) {
      handler.postDelayed(
          () -> focusQuotedMessage(list, reply, quoted, attempts - 1), FOCUS_RETRY_MS);
    } else {
      LogUtils.d(TAG, "The quoted message isn't on the screen");
    }
  }

  /** Taps the screen at ([x], [y]) through the pass-through region. Returns whether it was sent. */
  private boolean tap(int x, int y) {
    @Nullable AccessibilityService service = TalkBackService.getInstance();
    if (service == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      return false;
    }
    Path path = new Path();
    path.moveTo(x, y);
    GestureDescription gesture =
        new GestureDescription.Builder()
            .addStroke(
                new GestureDescription.StrokeDescription(
                    path, /* startTime= */ 0, ViewConfiguration.getTapTimeout()))
            .build();
    DisplayMetrics metrics = service.getResources().getDisplayMetrics();
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.passThroughMode(
            LOCK_PASS_THROUGH, new Region(0, 0, metrics.widthPixels, metrics.heightPixels)));
    AccessibilityService.GestureResultCallback ended =
        new AccessibilityService.GestureResultCallback() {
          @Override
          public void onCompleted(GestureDescription description) {
            release();
          }

          @Override
          public void onCancelled(GestureDescription description) {
            release();
          }
        };
    handler.postDelayed(
        () -> {
          if (!service.dispatchGesture(gesture, ended, handler)) {
            release();
          }
        },
        PASS_THROUGH_WAIT_MS);
    return true;
  }

  private void release() {
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null));
  }
}
