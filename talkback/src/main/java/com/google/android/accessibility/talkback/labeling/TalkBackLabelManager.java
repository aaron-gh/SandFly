/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.labeling;

import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.labeling.Label;
import com.google.android.accessibility.utils.labeling.LabelManager;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract controller for detecting unlabeled views and managing custom labels for them.
 *
 * <p>The former requires to keep track of the most recent accessibility focus event.
 */
public abstract class TalkBackLabelManager implements LabelManager, AccessibilityEventListener {
  private boolean hasFocusedEventText = false;

  @Override
  public int getEventTypes() {
    return AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    hasFocusedEventText =
        !TextUtils.isEmpty(AccessibilityEventUtils.getEventTextOrDescription(event));
  }

  /**
   * Returns whether the given node is missing a label.
   *
   * <p>To be called from the read-only state reader interface.
   */
  protected boolean needsLabel(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }

    // Spoken words can come from accessibility event text or content description, like the focus is
    // on ViewGroup that includes several non-focusable views.
    if (hasFocusedEventText
        && (Role.getRole(node) == Role.ROLE_VIEW_GROUP
            || Role.getRole(node) == Role.ROLE_SCROLL_VIEW
            || Role.getRole(node) == Role.ROLE_HORIZONTAL_SCROLL_VIEW)) {
      return false;
    }

    // REFERTO. ImageView without content description, which isn't focusable and in a
    // actionable ViewGroup, can't be labelled because it is not in the accessibility node tree but
    // in the hierarchy viewer. The actionable ViewGroup should be labelled.
    return node.isEnabled()
        && (AccessibilityNodeInfoUtils.isClickable(node)
            || AccessibilityNodeInfoUtils.isLongClickable(node)
            || (AccessibilityNodeInfoUtils.isFocusable(node) && canAddLabel(node)))
        && node.getChildCount() == 0
        && TextUtils.isEmpty(AccessibilityNodeInfoUtils.getNodeText(node));
  }

  /**
   * Returns whether a custom label can replace the text and description of the given node, as its
   * resource ID, by which labels are kept, names only it: it isn't an edit field, whose text is what
   * is typed; its resource ID isn't one of Android's, which every app uses; it isn't an item of a
   * list or grid, or in one, whose items share resource IDs; and no other item in its window has its
   * resource ID. Other nodes use a custom label only when they have no text.
   *
   * <p>Asks the app for the items with the resource ID, so check for a label first.
   */
  public static boolean canReplaceText(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null || node.isEditable()) {
      return false;
    }
    @Nullable String resourceName = node.getViewIdResourceName();
    if (resourceName == null || resourceName.startsWith("android:")) {
      return false;
    }
    if (node.getCollectionItemInfo() != null
        || AccessibilityNodeInfoUtils.hasMatchingAncestor(node, IN_COLLECTION_ITEM)) {
      return false;
    }
    @Nullable AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(node);
    if (root == null) {
      return false;
    }
    try {
      return root.findAccessibilityNodeInfosByViewId(resourceName).size() <= 1;
    } catch (IllegalStateException e) {
      // A node that didn't come from an app can't be asked.
      return false;
    }
  }

  private static final Filter<AccessibilityNodeInfoCompat> IN_COLLECTION_ITEM =
      Filter.node(node -> node.getCollectionItemInfo() != null);

  /** Returns the custom label of the node if it replaces its text, as {@link #canReplaceText}. */
  public static @Nullable String getReplacingLabel(
      AccessibilityNodeInfoCompat node, @Nullable LabelManager labelManager) {
    if (labelManager == null || node.isEditable()) {
      return null;
    }
    @Nullable Label label = labelManager.getLabelForViewIdFromCache(node.getViewIdResourceName());
    return label == null || TextUtils.isEmpty(label.getText()) || !canReplaceText(node)
        ? null
        : label.getText();
  }

  /**
   * Gets the text of a <code>node</code>: its custom label if it replaces the text ({@link
   * #canReplaceText}), then the content description (if available) or the text, then its custom
   * label.
   *
   * @param node The node.
   * @param labelManager The label manager.
   * @return The node text.
   */
  public static @Nullable CharSequence getNodeText(
      AccessibilityNodeInfoCompat node, TalkBackLabelManager labelManager) {
    @Nullable Label label =
        (labelManager != null && labelManager.isInitialized())
            ? labelManager.getLabelForViewIdFromCache(node.getViewIdResourceName())
            : null;
    if (label != null
        && !TextUtils.isEmpty(label.getText())
        && canReplaceText(node)) {
      return label.getText();
    }
    CharSequence text = AccessibilityNodeInfoUtils.getNodeText(node);
    if (!TextUtils.isEmpty(text)) {
      return text;
    }
    return label == null ? null : label.getText();
  }

  /** Whether this manager is properly initialized and ready to store and retrieve labels. */
  public boolean isInitialized() {
    // When this non-static method is called, this object is already initialized by default.
    return true;
  }

  /** Gets called in TalkBack's {@code onResumeInfrastracture()}. */
  public void onResume(Context context) {
    // Do nothing by default.
  }

  /** Gets called in TalkBack's {@code suspendInfrastructure()}. */
  public void onSuspend(Context context) {
    // Do nothing by default.
  }

  /** Gets called in TalkBack's {@code onUnlockedBootCompleted()}. */
  public void onUnlockedBoot() {
    // Do nothing by default.
  }

  /** Shuts down this manager and releases resources. */
  public void shutdown() {
    // do nothing by default
  }

  /** Tries to overwrite the label for a node and returns whether it was successful. */
  public abstract boolean setLabel(
      @Nullable AccessibilityNodeInfoCompat node, @Nullable String userLabel);

  /** Fetches labels from database and calls the given {@code callback} when done. */
  public abstract void getLabelsFromDatabase(LabelsFetchRequest.OnLabelsFetchedListener callback);

  @Override
  public abstract @Nullable Label getLabelForViewIdFromCache(String resourceName);

  /** Returns whether the manager can store a label for the given node. */
  public abstract boolean canAddLabel(@Nullable AccessibilityNodeInfoCompat node);
}
