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

package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** Volume keys: what the volume keys do while SandFly is on. */
public class VolumeKeysFragment extends AdvancedSettingFragment {
  public VolumeKeysFragment() {
    super(R.xml.volume_keys_preferences, R.string.title_pref_volume_keys);
  }
}
