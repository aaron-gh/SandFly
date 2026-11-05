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

package com.google.android.accessibility.talkback.imagecaption;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.libraries.accessibility.utils.bitmap.BitmapUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Prepares a screenshot for captioning: finds the views that overlap the captioned node, blacks
 * them out and crops the node. The node search is bounded, so that a large tree, such as a web
 * page, cannot make it take seconds. Runs on any thread.
 */
public final class ScreenshotPreparation {

  private static final String TAG = "ScreenshotPreparation";

  /** The most nodes the search for overlapping views reads. */
  public static final int MAX_VISITED_NODES = 1500;

  /** The most overlapping views that are blacked out. */
  public static final int MAX_OVERLAPS = 100;

  /** The most ancestors of the node that are read. */
  private static final int MAX_ANCESTORS = 100;

  /** Reads a tree of nodes. */
  public interface NodeTree<N> {
    int getChildCount(N node);

    @Nullable
    N getChild(N node, int index);

    void getBoundsInScreen(N node, Rect outBounds);

    boolean isFocusable(N node);
  }

  /** Reads an accessibility node tree. */
  public static final NodeTree<AccessibilityNodeInfoCompat> ACCESSIBILITY_TREE =
      new NodeTree<>() {
        @Override
        public int getChildCount(AccessibilityNodeInfoCompat node) {
          return node.getChildCount();
        }

        @Override
        public @Nullable AccessibilityNodeInfoCompat getChild(
            AccessibilityNodeInfoCompat node, int index) {
          return node.getChild(index);
        }

        @Override
        public void getBoundsInScreen(AccessibilityNodeInfoCompat node, Rect outBounds) {
          node.getBoundsInScreen(outBounds);
        }

        @Override
        public boolean isFocusable(AccessibilityNodeInfoCompat node) {
          return AccessibilityNodeInfoUtils.isFocusable(node);
        }
      };

  private ScreenshotPreparation() {}

  /**
   * Adds to {@code overlaps} the bounds of the focusable views inside {@code target} that are
   * clearly smaller than it, other than {@code excluded}. Such a view is blacked out, so that the
   * caption is about the target's own content.
   *
   * <p>Only branches whose bounds intersect {@code target} are searched, and the children of a view
   * that is blacked out are not searched, because they are inside it. The search stops after
   * {@code maxVisitedNodes} nodes, after {@code maxOverlaps} overlaps, or when {@code stop} returns
   * true.
   *
   * @return true if the search read every branch it had to; false if a limit stopped it
   */
  public static <N> boolean findOverlaps(
      @Nullable N root,
      Rect target,
      Set<N> excluded,
      NodeTree<N> tree,
      int maxVisitedNodes,
      int maxOverlaps,
      BooleanSupplier stop,
      List<Rect> overlaps) {
    if (root == null || target.isEmpty()) {
      return true;
    }
    Set<N> visited = new HashSet<>();
    ArrayDeque<N> pending = new ArrayDeque<>();
    pending.push(root);
    Rect bounds = new Rect();
    while (!pending.isEmpty()) {
      if (visited.size() >= maxVisitedNodes
          || overlaps.size() >= maxOverlaps
          || stop.getAsBoolean()) {
        return false;
      }
      N node = pending.pop();
      if (!visited.add(node)) {
        continue;
      }
      tree.getBoundsInScreen(node, bounds);
      if (!bounds.isEmpty() && !Rect.intersects(bounds, target)) {
        // Children are inside their parent's bounds. A node without bounds, such as a web element
        // that only groups others, is searched.
        continue;
      }
      if (!excluded.contains(node) && isOverlap(target, bounds) && tree.isFocusable(node)) {
        overlaps.add(new Rect(bounds));
        continue;
      }
      int childCount = tree.getChildCount(node);
      // Pushed in reverse, so that children are searched in order.
      for (int i = childCount - 1; i >= 0; i--) {
        N child = tree.getChild(node, i);
        if (child != null) {
          pending.push(child);
        }
      }
    }
    return true;
  }

  /**
   * Returns whether a view with {@code bounds} is inside {@code target} and leaves at least 10% of
   * its width and height. b/348281762: a view that covers nearly all of the target is not blacked
   * out, so that some of the target stays visible.
   */
  static boolean isOverlap(Rect target, Rect bounds) {
    return target.contains(bounds)
        && (target.width() - bounds.width()) > target.width() / 10
        && (target.height() - bounds.height()) > target.height() / 10;
  }

  /**
   * Finds the overlapping views of {@code node} in its window with {@link #findOverlaps}.
   *
   * @return true if the search was complete
   */
  public static boolean findOverlapsOfNode(
      AccessibilityNodeInfoCompat node,
      Rect nodeBounds,
      BooleanSupplier stop,
      List<Rect> overlaps) {
    Set<AccessibilityNodeInfoCompat> selfAndAncestors = new HashSet<>();
    selfAndAncestors.add(node);
    AccessibilityNodeInfoCompat parent = node.getParent();
    while (parent != null
        && selfAndAncestors.size() < MAX_ANCESTORS
        && selfAndAncestors.add(parent)) {
      parent = parent.getParent();
    }
    if (stop.getAsBoolean()) {
      return false;
    }
    @Nullable AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(node);
    return findOverlaps(
        root,
        nodeBounds,
        selfAndAncestors,
        ACCESSIBILITY_TREE,
        MAX_VISITED_NODES,
        MAX_OVERLAPS,
        stop,
        overlaps);
  }

  /**
   * Returns a copy of {@code screenshot} with {@code blocks} black, or null if there is nothing to
   * black out or the copy can't be made. {@code screenshot} is unchanged.
   */
  public static @Nullable Bitmap blackOut(Bitmap screenshot, List<Rect> blocks) {
    Rect image = new Rect(0, 0, screenshot.getWidth(), screenshot.getHeight());
    boolean inImage = false;
    for (Rect block : blocks) {
      if (Rect.intersects(image, block)) {
        inImage = true;
        break;
      }
    }
    if (!inImage) {
      return null;
    }
    Bitmap blocked = screenshot.copy(Config.ARGB_8888, /* isMutable= */ true);
    if (blocked == null) {
      LogUtils.w(TAG, "The screenshot could not be copied to black out views.");
      return null;
    }
    Canvas canvas = new Canvas(blocked);
    Paint black = new Paint();
    black.setColor(Color.BLACK);
    black.setStyle(Paint.Style.FILL);
    for (Rect block : blocks) {
      canvas.drawRect(block, black);
    }
    return blocked;
  }

  /**
   * Returns the part of {@code image} inside {@code bounds}, or a copy of the whole image if {@code
   * bounds} is null or outside it. The result may be {@code image} itself, when {@code bounds}
   * covers all of it.
   */
  public static Bitmap crop(Bitmap image, @Nullable Rect bounds) {
    Bitmap cropped = null;
    if (bounds != null
        && (bounds.height() <= image.getHeight() || bounds.width() <= image.getWidth())) {
      try {
        cropped = BitmapUtils.cropBitmap(image, bounds);
      } catch (IllegalArgumentException e) {
        LogUtils.w(TAG, e.getMessage() == null ? "Fail to crop screenshot." : e.getMessage());
      }
    }
    if (cropped == null) {
      // The caller recycles the original image, so this is a copy.
      cropped = image.copy(Config.ARGB_8888, /* isMutable= */ image.isMutable());
    }
    return cropped;
  }
}
