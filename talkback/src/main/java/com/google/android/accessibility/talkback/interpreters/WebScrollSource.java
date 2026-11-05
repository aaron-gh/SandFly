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

package com.google.android.accessibility.talkback.interpreters;

import androidx.annotation.IntDef;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tells the scroll of web content that the user makes with the fingers from the scrolls that the
 * page makes in response during the same touch, such as a strip of category tabs that the page
 * scrolls sideways to follow the menu that the user scrolls down.
 *
 * <p>The first web content that scrolls after a touch starts is the user's scroll. A later scroll
 * that is not sideways replaces a sideways one, since a strip can still be moving from the last
 * scroll when the next starts. Asks the app nothing.
 */
public final class WebScrollSource {

  /** What a scroll of web content is. */
  @IntDef({USER_START, USER, PAGE_SIDEWAYS, PAGE_OTHER})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Kind {}

  /** The first scroll event of the user's scroll in this touch. */
  public static final int USER_START = 0;

  /** A later scroll event of the user's scroll. */
  public static final int USER = 1;

  /** A scroll of other content, that scrolls only sideways, that the page made. */
  public static final int PAGE_SIDEWAYS = 2;

  /** Another scroll of other content that the page made. */
  public static final int PAGE_OTHER = 3;

  private long touchStartTime = -1;
  private @Nullable AccessibilityNodeInfoCompat userScrolled;
  private boolean userScrolledSideways;

  /** Whether a scroll from {@code node} can be one of web content. Asks the app nothing. */
  public static boolean isWebContent(@Nullable AccessibilityNodeInfoCompat node) {
    return WebScrollFocus.isWebContent(node);
  }

  /**
   * Returns what the scroll of {@code scrolled}, web content, is. Called for each scroll event of
   * web content in turn.
   *
   * @param touchStartTime when the last touch interaction started, or -1
   */
  public @Kind int onScroll(AccessibilityNodeInfoCompat scrolled, long touchStartTime) {
    boolean sideways = WebScrollFocus.scrollsOnlySideways(scrolled);
    if (touchStartTime != this.touchStartTime
        || userScrolled == null
        || (userScrolledSideways && !sideways && !scrolled.equals(userScrolled))) {
      this.touchStartTime = touchStartTime;
      userScrolled = scrolled;
      userScrolledSideways = sideways;
      return USER_START;
    }
    if (scrolled.equals(userScrolled)) {
      return USER;
    }
    return sideways ? PAGE_SIDEWAYS : PAGE_OTHER;
  }

  /** Whether the user's scroll scrolls only sideways. */
  public boolean userScrollIsSideways() {
    return userScrolledSideways;
  }
}
