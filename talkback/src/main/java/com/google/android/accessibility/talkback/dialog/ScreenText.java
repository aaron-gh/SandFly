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

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.gemini.progress.DefaultProgressToneProvider;
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressTonePlayer;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollTimeout;
import com.google.android.accessibility.utils.output.ScrollActionRecord;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Collects the text on the screen for Copy text from screen, and scrolls the largest list or page
 * of the active window that scrolls up and down a page at a time for Scroll up and Scroll down. The
 * list is scrolled back when the window closes. These scrolls say nothing.
 */
final class ScreenText {

  private static final String TAG = "ScreenText";

  /** How often the list is read while a scroll moves it. */
  private static final long POLL_MS = 100;

  /** How long a scroll may take to change the list before the list counts as at its end. */
  private static final long SCROLL_WAIT_MS = 1500;

  /** How long the main thread may take to send a scroll. */
  private static final long SEND_WAIT_MS = 1000;

  /** Progress tones play this long at most. */
  private static final long TONES_MS = 60_000L;

  private static final int MAX_NODES = 1500;
  private static final int MAX_DEPTH = 50;

  private ScreenText() {}

  /** The texts of one tree walk, in order, and where the list's texts go among them. */
  private static final class Walk {
    final List<String> texts = new ArrayList<>();
    final Set<AccessibilityNodeInfoCompat> visited = new HashSet<>();
    @Nullable final AccessibilityNodeInfoCompat list;
    /** The index in {@link #texts} where the list's texts go, or -1 before the walk reaches it. */
    int listIndex = -1;
    /** The text of the item with accessibility focus, if the walk passed it. */
    @Nullable String focused;

    Walk(@Nullable AccessibilityNodeInfoCompat list) {
      this.list = list;
    }
  }

  /**
   * Collects the text of every window on screen. Runs off the main thread, as does every method of
   * the returned session.
   */
  static Session collect(TalkBackService service, @Nullable FeedbackReturner pipeline) {
    @Nullable AccessibilityNodeInfoCompat list = pipeline == null ? null : findList(service);
    Walk walk = new Walk(list);

    List<AccessibilityWindowInfo> windows = AccessibilityServiceCompatUtils.getWindows(service);
    if (windows != null) {
      for (AccessibilityWindowInfo window : windows) {
        if (walk.visited.size() >= MAX_NODES) {
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
            collectTexts(AccessibilityNodeInfoCompat.wrap(root), walk, /* depth= */ 0);
          }
        }
      }
    }
    if (walk.texts.isEmpty() && walk.listIndex < 0) {
      AccessibilityNodeInfo activeRoot = service.getRootInActiveWindow();
      if (activeRoot != null) {
        collectTexts(AccessibilityNodeInfoCompat.wrap(activeRoot), walk, /* depth= */ 0);
      }
    }

    @Nullable ListScroller scroller =
        (list != null && pipeline != null && walk.listIndex >= 0)
            ? new ListScroller(pipeline, list)
            : null;
    return new Session(walk.texts, walk.listIndex, scroller);
  }

  /**
   * The text on screen, with what Scroll up and Scroll down added from the list. All methods run
   * off the main thread, one at a time.
   */
  static final class Session {
    private final List<String> outside;
    private final int listIndex;
    @Nullable private final ListScroller scroller;
    /** The list's texts read so far, from the highest page read to the lowest. */
    private final List<String> listTexts;

    Session(List<String> outside, int listIndex, @Nullable ListScroller scroller) {
      this.outside = outside;
      this.scroller = scroller;
      this.listIndex = (scroller == null || listIndex < 0) ? outside.size() : listIndex;
      this.listTexts =
          (scroller == null) ? new ArrayList<>() : new ArrayList<>(scroller.start.texts);
    }

    /** Whether the screen has a list that Scroll up and Scroll down scroll. */
    boolean canScroll() {
      return scroller != null;
    }

    /** The texts in screen order, the list's in its place, within the dialog's limits. */
    WordSelectionDialog.Collected collected() {
      WordSelectionDialog.Collected collected = new WordSelectionDialog.Collected();
      for (int i = 0; i <= outside.size(); i++) {
        if (i == listIndex) {
          for (String text : listTexts) {
            if (!collected.add(text)) {
              return collected;
            }
          }
        }
        if (i < outside.size() && !collected.add(outside.get(i))) {
          return collected;
        }
      }
      return collected;
    }

    /**
     * Scrolls the list a page past the text read so far and adds that page's text. Returns the
     * number of texts added, 0 when the list is at its end or start, {@link #NOT_SCROLLED} when it
     * can't scroll, or {@link #FULL} when the text is at its limit.
     */
    int scroll(boolean forward) {
      if (scroller == null) {
        return NOT_SCROLLED;
      }
      if (chars(outside) + chars(listTexts) >= WordSelectionDialog.MAX_TEXT_CHARS) {
        return FULL;
      }
      return scroller.readPage(listTexts, forward);
    }

    /** Scrolls the list back to where it was when the text was collected. */
    void restore() {
      if (scroller != null) {
        scroller.restore(listTexts);
      }
    }
  }

  /** {@link Session#scroll} result when the list can't scroll. */
  static final int NOT_SCROLLED = -1;

  /** {@link Session#scroll} result when the text is at its limit. */
  static final int FULL = -2;

  private static int chars(List<String> texts) {
    int chars = 0;
    for (String text : texts) {
      chars += text.length();
    }
    return chars;
  }

  /**
   * Adds the texts of {@code node} and the items inside it that are on screen: each item's text,
   * or its content description when nothing inside it has text. Stops at {@link Walk#list}, whose
   * texts the scrolling collects. Returns whether anything was added.
   */
  private static boolean collectTexts(
      @Nullable AccessibilityNodeInfoCompat node, Walk walk, int depth) {
    if (node == null || depth > MAX_DEPTH || walk.visited.size() >= MAX_NODES) {
      return false;
    }
    if (!walk.visited.add(node)) {
      return false;
    }
    if (!node.isVisibleToUser() || node.isPassword()) {
      return false;
    }
    if (walk.list != null && walk.listIndex < 0 && node.equals(walk.list)) {
      walk.listIndex = walk.texts.size();
      return true;
    }

    int start = walk.texts.size();
    boolean hasText = false;
    CharSequence text = AccessibilityNodeInfoUtils.getText(node);
    if (!TextUtils.isEmpty(text)) {
      String s = text.toString().trim();
      if (!s.isEmpty()) {
        walk.texts.add(s);
        hasText = true;
      }
    }

    boolean descendantHasText = false;
    int childCount = node.getChildCount();
    for (int i = 0; i < childCount; i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child != null && collectTexts(child, walk, depth + 1)) {
        descendantHasText = true;
      }
    }

    if (!hasText && !descendantHasText) {
      CharSequence contentDesc = node.getContentDescription();
      if (!TextUtils.isEmpty(contentDesc)) {
        String s = contentDesc.toString().trim();
        if (!s.isEmpty()) {
          walk.texts.add(s);
          hasText = true;
        }
      }
    }

    if (node.isAccessibilityFocused() && walk.texts.size() > start) {
      walk.focused = String.join(" ", walk.texts.subList(start, walk.texts.size()));
    }
    return hasText || descendantHasText;
  }

  /**
   * The largest list or page on screen in the active window that scrolls up and down, or null when
   * it has none.
   */
  @Nullable
  private static AccessibilityNodeInfoCompat findList(TalkBackService service) {
    AccessibilityNodeInfo activeRoot = service.getRootInActiveWindow();
    if (activeRoot == null) {
      return null;
    }
    AccessibilityNodeInfoCompat root = AccessibilityNodeInfoCompat.wrap(activeRoot);
    if (AccessibilityNodeInfoUtils.getWindowType(root) != AccessibilityWindowInfo.TYPE_APPLICATION) {
      return null;
    }
    List<AccessibilityNodeInfoCompat> queue = new ArrayList<>();
    Set<AccessibilityNodeInfoCompat> seen = new HashSet<>();
    queue.add(root);
    @Nullable AccessibilityNodeInfoCompat best = null;
    long bestArea = 0;
    Rect bounds = new Rect();
    for (int head = 0; head < queue.size() && seen.size() < MAX_NODES; head++) {
      AccessibilityNodeInfoCompat node = queue.get(head);
      if (!seen.add(node) || !node.isVisibleToUser()) {
        continue;
      }
      if (scrollsUpAndDown(node)) {
        node.getBoundsInScreen(bounds);
        long area = (long) bounds.width() * bounds.height();
        if (area > bestArea) {
          best = node;
          bestArea = area;
        }
      }
      int childCount = node.getChildCount();
      for (int i = 0; i < childCount; i++) {
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child != null) {
          queue.add(child);
        }
      }
    }
    return best;
  }

  private static boolean scrollsUpAndDown(AccessibilityNodeInfoCompat node) {
    if (!AccessibilityNodeInfoUtils.isScrollable(node)) {
      return false;
    }
    int role = Role.getRole(node);
    if (role == Role.ROLE_SEEK_CONTROL
        || role == Role.ROLE_PAGER
        || role == Role.ROLE_HORIZONTAL_SCROLL_VIEW
        || AccessibilityNodeInfoUtils.scrollsOnlySideways(node)
        || AccessibilityNodeInfoUtils.isPlainViewScrollingWithoutAxis(node)) {
      return false;
    }
    return backAction(node) != 0 || forwardAction(node) != 0;
  }

  /** The action that scrolls {@code node} back a page, or 0 when it has none. */
  private static int backAction(AccessibilityNodeInfoCompat node) {
    if (AccessibilityNodeInfoUtils.supportsAction(
        node, AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)) {
      return AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;
    }
    int up = AccessibilityActionCompat.ACTION_SCROLL_UP.getId();
    return AccessibilityNodeInfoUtils.supportsAction(node, up) ? up : 0;
  }

  /** The action that scrolls {@code node} forward a page, or 0 when it has none. */
  private static int forwardAction(AccessibilityNodeInfoCompat node) {
    if (AccessibilityNodeInfoUtils.supportsAction(
        node, AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)) {
      return AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
    }
    int down = AccessibilityActionCompat.ACTION_SCROLL_DOWN.getId();
    return AccessibilityNodeInfoUtils.supportsAction(node, down) ? down : 0;
  }

  /**
   * Joins {@code page} onto {@code texts}: the start of {@code page} that repeats the end of {@code
   * texts} is left out, as the items a scroll keeps on screen. Returns the number of texts added.
   */
  @VisibleForTesting
  static int merge(List<String> texts, List<String> page) {
    int overlap = 0;
    int most = Math.min(texts.size(), page.size());
    for (int k = most; k > 0; k--) {
      if (texts.subList(texts.size() - k, texts.size()).equals(page.subList(0, k))) {
        overlap = k;
        break;
      }
    }
    if (overlap == 0 && !texts.isEmpty()) {
      // No exact overlap, such as when an item at the edge changed as it came on screen: leave out
      // the start of the page whose texts are all among the last texts.
      Set<String> recent =
          new HashSet<>(texts.subList(Math.max(0, texts.size() - page.size()), texts.size()));
      while (overlap < page.size() && recent.contains(page.get(overlap))) {
        overlap++;
      }
    }
    List<String> rest = new ArrayList<>(page.subList(overlap, page.size()));
    texts.addAll(rest);
    return rest.size();
  }

  /**
   * Joins {@code page}, from higher in the list, before {@code texts}: the end of {@code page}
   * that repeats the start of {@code texts} is left out. Returns the number of texts added.
   */
  @VisibleForTesting
  static int mergeBefore(List<String> texts, List<String> page) {
    List<String> reversed = new ArrayList<>(texts);
    Collections.reverse(reversed);
    List<String> reversedPage = new ArrayList<>(page);
    Collections.reverse(reversedPage);
    int added = merge(reversed, reversedPage);
    Collections.reverse(reversed);
    texts.clear();
    texts.addAll(reversed);
    return added;
  }

  /** Scrolls one list and reads its pages, from the worker thread. */
  private static final class ListScroller {
    private final FeedbackReturner pipeline;
    private final AccessibilityNodeInfoCompat list;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ProgressTonePlayer tones;
    /** The list's page on screen when the text was collected. */
    final Walk start;
    /** The page on screen now. */
    private Walk current;
    /** Pages scrolled forward from the start, less pages scrolled back. */
    private int position;
    /** The positions of the highest and the lowest page read. */
    private int highest;
    private int lowest;

    ListScroller(FeedbackReturner pipeline, AccessibilityNodeInfoCompat list) {
      this.pipeline = pipeline;
      this.list = list;
      start = page();
      current = start;
      tones =
          new ProgressTonePlayer(
              new DefaultProgressToneProvider(),
              tone -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, tone.tone),
              () -> TONES_MS,
              () -> {});
    }

    /**
     * Scrolls back to the edge of {@code texts} that {@code forward} points to, then a page past
     * it, and joins that page onto {@code texts}. Returns the number of texts added.
     */
    int readPage(List<String> texts, boolean forward) {
      mainHandler.post(() -> tones.play(/* playTone= */ true, /* delayPlayTone= */ true));
      try {
        if (texts.isEmpty()) {
          @Nullable Walk next = step(forward);
          if (next == null) {
            return NOT_SCROLLED;
          }
          texts.addAll(next.texts);
          return next.texts.size();
        }
        // Up and Down can be pressed in turn: go back to the edge read so far.
        @Nullable String edge = forward ? lastUnique(texts) : firstUnique(texts);
        int most = lowest - highest + 2;
        for (int i = 0; edge != null && !current.texts.contains(edge) && i < most; i++) {
          if (step(forward) == null) {
            return 0;
          }
        }
        int added = join(texts, current.texts, forward);
        if (added == 0) {
          @Nullable Walk next = step(forward);
          if (next == null) {
            return 0;
          }
          added = join(texts, next.texts, forward);
        }
        return added;
      } catch (RuntimeException e) {
        LogUtils.w(TAG, "Can't scroll the list: %s", e);
        return NOT_SCROLLED;
      } finally {
        mainHandler.post(tones::stop);
      }
    }

    private static int join(List<String> texts, List<String> page, boolean forward) {
      return forward ? merge(texts, page) : mergeBefore(texts, page);
    }

    /**
     * Scrolls back the pages the list moved, stopping once the focused item, or the first item
     * that was on screen, is on screen again.
     */
    void restore(List<String> texts) {
      if (position == 0) {
        return;
      }
      try {
        @Nullable String anchor = anchor(start, texts);
        boolean forward = position < 0;
        // One page more than the count, as the last page of a list can be a part of a page.
        int most = Math.abs(position) + (anchor == null ? 0 : 1);
        for (int i = 0; i < most; i++) {
          if (anchor != null && current.texts.contains(anchor)) {
            return;
          }
          if (step(forward) == null) {
            return;
          }
        }
      } catch (RuntimeException e) {
        LogUtils.w(TAG, "Can't scroll the list back: %s", e);
      }
    }

    /**
     * The text to find on screen after scrolling back: that of the focused item, or else the first
     * one on screen at the start; null when neither is found only once in {@code texts}.
     */
    @Nullable
    private static String anchor(Walk start, List<String> texts) {
      Map<String, Integer> counts = counts(texts);
      if (start.focused != null && counts.getOrDefault(start.focused, 0) == 1) {
        return start.focused;
      }
      for (String text : start.texts) {
        if (counts.getOrDefault(text, 0) == 1) {
          return text;
        }
      }
      return null;
    }

    /** The texts of the list's items on screen. */
    private Walk page() {
      Walk walk = new Walk(/* list= */ null);
      collectTexts(list, walk, /* depth= */ 0);
      return walk;
    }

    /**
     * Scrolls the list a page and returns its texts once it stops moving, or null when it can't
     * scroll that way or nothing on it changed.
     */
    @Nullable
    private Walk step(boolean forward) {
      if (!list.refresh()) {
        return null;
      }
      int action = forward ? forwardAction(list) : backAction(list);
      if (action == 0 || !send(action)) {
        return null;
      }
      long end = SystemClock.uptimeMillis() + SCROLL_WAIT_MS;
      @Nullable Walk previous = null;
      @Nullable Walk moved = null;
      while (SystemClock.uptimeMillis() < end) {
        SystemClock.sleep(POLL_MS);
        Walk now = page();
        if (!now.texts.equals(current.texts)) {
          if (previous != null && now.texts.equals(previous.texts)) {
            moved = now;
            break;
          }
          previous = now;
        }
      }
      if (moved == null) {
        moved = previous;
      }
      if (moved != null) {
        current = moved;
        position += forward ? 1 : -1;
        highest = Math.min(highest, position);
        lowest = Math.max(lowest, position);
      }
      return moved;
    }

    /** Sends the scroll from the main thread, recorded as SandFly's own, which says nothing. */
    private boolean send(int action) {
      FutureTask<Boolean> task =
          new FutureTask<>(
              () ->
                  pipeline.returnFeedback(
                      EVENT_ID_UNTRACKED,
                      Feedback.scroll(
                          list,
                          ScrollActionRecord.ACTION_AUTO_SCROLL,
                          action,
                          ScrollActionRecord.COLLECT_TEXT,
                          ScrollTimeout.SCROLL_TIMEOUT_SHORT,
                          /* autoScrollAttempt= */ 0)));
      mainHandler.post(task);
      try {
        return task.get(SEND_WAIT_MS, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        task.cancel(/* mayInterruptIfRunning= */ false);
        return false;
      } catch (Exception e) {
        task.cancel(/* mayInterruptIfRunning= */ false);
        return false;
      }
    }
  }

  private static Map<String, Integer> counts(List<String> texts) {
    Map<String, Integer> counts = new HashMap<>();
    for (String text : texts) {
      counts.merge(text, 1, Integer::sum);
    }
    return counts;
  }

  /** The last of {@code texts} found only once in them, or null. */
  @Nullable
  private static String lastUnique(List<String> texts) {
    Map<String, Integer> counts = counts(texts);
    for (int i = texts.size() - 1; i >= 0; i--) {
      if (counts.get(texts.get(i)) == 1) {
        return texts.get(i);
      }
    }
    return null;
  }

  /** The first of {@code texts} found only once in them, or null. */
  @Nullable
  private static String firstUnique(List<String> texts) {
    Map<String, Integer> counts = counts(texts);
    for (String text : texts) {
      if (counts.get(text) == 1) {
        return text;
      }
    }
    return null;
  }
}
