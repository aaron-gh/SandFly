/*
 * Copyright (C) 2021 Google Inc.
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

package com.google.android.accessibility.talkback.icondetection;

import android.content.Context;
import com.google.android.accessibility.utils.screenunderstanding.IconAnnotationsDetector;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Factory class for {@link IconAnnotationsDetector}. */
public final class IconAnnotationsDetectorFactory {

  /**
   * Returns whether {@link #create} can return a detector. The open-source TalkBack code has no
   * icon detection model or download for it, so SandFly has none.
   */
  public static boolean isAvailable() {
    return false;
  }

  public static @Nullable IconAnnotationsDetector create(Context context) {
    return null;
  }

  private IconAnnotationsDetectorFactory() {}
}
