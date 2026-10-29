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

package com.google.android.accessibility.utils;

/**
 * SandFly's own keyboards, the braille keyboard and handwriting, which take the screen for
 * themselves when they open. While {@link #held} is set, as while the SandFly Pro agent acts, they
 * stay closed when a field gets focus; the keyboard in use doesn't change.
 */
public final class OwnKeyboards {
  private OwnKeyboards() {}

  public static volatile boolean held;
}
