/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.scripting

import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.soundthemes.SoundThemeInstallActivity

/** The repositories of sound themes that the user has added, as Script sources are for scripts. */
class SoundThemeSourcesFragment : ScriptSourcesFragment() {
  override val kind = SourceKind.SOUND_THEMES
  override val sourceScreen: Class<out ScriptSourceFragment> = SoundThemeSourceFragment::class.java

  public override fun getTitle(): CharSequence = getText(R.string.sound_theme_sources)
}

/**
 * The theme files in one source. Choosing one downloads it and asks before installing, as From a
 * link does.
 */
class SoundThemeSourceFragment : ScriptSourceFragment() {
  override val kind = SourceKind.SOUND_THEMES
  override val failedText = R.string.sound_theme_source_failed
  override val emptyText = R.string.sound_theme_source_empty
  override val removeMessage = R.string.sound_theme_source_remove_message

  override fun install(script: SourceScript) {
    startActivity(SoundThemeInstallActivity.linkIntent(requireContext(), script.url))
  }
}
