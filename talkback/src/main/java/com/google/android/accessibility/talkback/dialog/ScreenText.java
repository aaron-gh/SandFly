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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Collects the text on the screen for Copy text from screen. The largest list or page of the active
 * window that scrolls up and down is read from its start to its end: it is scrolled to its start, a
 * page at a time to its end, and back until the item that was at its top, or the focused item, is
 * on screen again. These scrolls say nothing. Runs off the main thread; the scrolls are sent from
 * the main thread.
 */
final class ScreenText {

  private static final String TAG = "ScreenText";

  /** The most pages scrolled back to the start of the list. */
  @VisibleForTesting static final int MAX_SCROLLS_BACK = 20;

  /** The most pages scrolled forward past the starting position. */
  @VisibleForTesting static final int MAX_SCROLLS_FORWARD = 20;

  /** The longest the scrolling runs before the list is scrolled back. */
  private static final long MAX_DURATION_MS = 30_000;

  /** How often the list is read while a scroll moves it. */
  private static final long POLL_MS = 100;

  /** How long a scroll may take to change the list before the list counts as at its end. */
  private static final long SCROLL_WAIT_MS = 1500;

  /** How long the main thread may take to send a scroll. */
  private static final long SEND_WAIT_MS = 1000;

  /** Progress tones play this long at most. */
  private static final long TONES_MS = 2 * 60_000L;

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

  /** Collects the text of every window on screen, with all of the active window's list. */
  static WordSelectionDialog.Collected collect(
      TalkBackService service, @Nullable FeedbackReturner pipeline) {
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

    List<String> listTexts = new ArrayList<>();
    if (list != null && pipeline != null && walk.listIndex >= 0) {
      int room = WordSelectionDialog.MAX_TEXT_CHARS - chars(walk.texts);
      listTexts = new ListReader(service, pipeline, list).read(room);
    }

    WordSelectionDialog.Collected collected = new WordSelectionDialog.Collected();
    int listIndex = walk.listIndex < 0 ? walk.texts.size() : walk.listIndex;
    for (int i = 0; i < walk.texts.size(); i++) {
      if (i == listIndex && !addAll(collected, listTexts)) {
        return collected;
      }
      if (!collected.add(walk.texts.get(i))) {
        return collected;
      }
    }
    if (listIndex >= walk.texts.size()) {
      addAll(collected, listTexts);
    }
    return collected;
  }

  private static boolean addAll(WordSelectionDialog.Collected collected, List<String> texts) {
    for (String text : texts) {
      if (!collected.add(text)) {
        return false;
      }
    }
    return true;
  }

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
   * texts} is left out, as the items a scroll keeps on screen. Returns the characters added.
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
    int added = 0;
    for (String text : page.subList(overlap, page.size())) {
      texts.add(text);
      added += text.length();
    }
    return added;
  }

  /** Scrolls one list and reads it, from the worker thread. */
  private static final class ListReader {
    private final FeedbackReturner pipeline;
    private final AccessibilityNodeInfoCompat list;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ProgressTonePlayer tones;
    private final long deadline = SystemClock.uptimeMillis() + MAX_DURATION_MS;

    ListReader(TalkBackService service, FeedbackReturner pipeline, AccessibilityNodeInfoCompat list) {
      this.pipeline = pipeline;
      this.list = list;
      tones =
          new ProgressTonePlayer(
              new DefaultProgressToneProvider(),
              tone -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, tone.tone),
              () -> TONES_MS,
              () -> {});
    }

    /**
     * Returns the list's texts from its start, up to about {@code room} characters, and scrolls
     * it back to where it was.
     */
    List<String> read(int room) {
      Walk start = page();
      List<String> texts = new ArrayList<>(start.texts);
      mainHandler.post(() -> tones.play(/* playTone= */ true, /* delayPlayTone= */ true));
      try {
        // Back to the start.
        int back = 0;
        Walk current = start;
        while (back < MAX_SCROLLS_BACK && !timedOut()) {
          @Nullable Walk next = scroll(/* forward= */ false, current);
          if (next == null) {
            break;
          }
          back++;
          current = next;
        }

        // Forward to the end, joining each page on.
        texts = new ArrayList<>(current.texts);
        int chars = chars(texts);
        int forward = 0;
        while (forward < back + MAX_SCROLLS_FORWARD && chars < room && !timedOut()) {
          @Nullable Walk next = scroll(/* forward= */ true, current);
          if (next == null) {
            break;
          }
          forward++;
          chars += merge(texts, next.texts);
          current = next;
        }
        LogUtils.d(
            TAG, "Scrolled %d pages back, %d forward, %d texts", back, forward, texts.size());

        restore(start, texts, current, forward - back);
      } catch (RuntimeException e) {
        LogUtils.w(TAG, "Can't scroll the list: %s", e);
      } finally {
        mainHandler.post(tones::stop);
      }
      return texts;
    }

    /**
     * Scrolls back the {@code pages} the list moved, or forward when negative, stopping once the
     * focused item, or the first item that was on screen, is on screen again.
     */
    private void restore(Walk start, List<String> texts, Walk current, int pages) {
      if (pages == 0) {
        return;
      }
      @Nullable String anchor = anchor(start, texts);
      boolean forward = pages < 0;
      int steps = Math.abs(pages);
      // One page more than the count, as the last page of a list can be a part of a page.
      int most = (anchor == null) ? steps : steps + 1;
      for (int i = 0; i < most; i++) {
        if (anchor != null && current.texts.contains(anchor)) {
          return;
        }
        @Nullable Walk next = scroll(forward, current);
        if (next == null) {
          return;
        }
        current = next;
      }
    }

    /**
     * The text to find on screen after scrolling back: that of the focused item, or else the first
     * one on screen at the start; null when neither is found only once in {@code texts}.
     */
    @Nullable
    private static String anchor(Walk start, List<String> texts) {
      Map<String, Integer> counts = new HashMap<>();
      for (String text : texts) {
        counts.merge(text, 1, Integer::sum);
      }
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

    private boolean timedOut() {
      return SystemClock.uptimeMillis() > deadline;
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
    private Walk scroll(boolean forward, Walk before) {
      if (!list.refresh()) {
        return null;
      }
      int action = forward ? forwardAction(list) : backAction(list);
      if (action == 0 || !send(action)) {
        return null;
      }
      long end = SystemClock.uptimeMillis() + SCROLL_WAIT_MS;
      @Nullable Walk previous = null;
      while (SystemClock.uptimeMillis() < end) {
        SystemClock.sleep(POLL_MS);
        Walk now = page();
        if (!now.texts.equals(before.texts)) {
          if (previous != null && now.texts.equals(previous.texts)) {
            return now;
          }
          previous = now;
        }
      }
      return previous;
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
}
