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
package com.google.android.accessibility.utils.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The Help action in the app bar of settings screens. The app sets the provider, which names the
 * help of each screen; {@link BasePreferencesActivity} shows the action on screens that have help.
 */
public final class SettingsHelp {

  /** Names the help of settings screens. */
  public interface Provider {
    /**
     * Returns the intent that opens the help of the screen, or null when it has none. {@code
     * screen} is the fragment shown in {@code activity}, or null for an activity without one.
     */
    @Nullable Intent helpIntent(Activity activity, @Nullable Fragment screen);

    /** Returns the title of the action. */
    CharSequence title(Context context);
  }

  private static volatile @Nullable Provider provider;

  private SettingsHelp() {}

  /** Sets the provider. */
  public static void setProvider(@Nullable Provider newProvider) {
    provider = newProvider;
  }

  /** Returns the provider, or null when none is set. */
  public static @Nullable Provider getProvider() {
    return provider;
  }
}
