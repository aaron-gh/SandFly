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

package com.google.android.accessibility.braille.common.translate;

import android.content.Context;
import com.google.android.accessibility.braille.common.ImeConnection;
import com.google.android.accessibility.braille.common.TalkBackSpeaker;
import com.google.android.accessibility.braille.translate.BrailleTranslator;

/**
 * The {@link EditBuffer} of the Unicode braille tables: as {@link EditBufferUnContracted}, but a
 * space is the blank braille cell U+2800 rather than U+0020.
 */
public class EditBufferUnicodeBraille extends EditBufferUnContracted {

  /** U+2800 BRAILLE PATTERN BLANK. */
  static final String BLANK_CELL = "⠀";

  public EditBufferUnicodeBraille(
      Context context, BrailleTranslator translator, TalkBackSpeaker talkBack) {
    super(context, translator, talkBack);
  }

  @Override
  public void appendSpace(ImeConnection imeConnection) {
    commit(imeConnection);
    imeConnection.inputConnection.commitText(BLANK_CELL, 1);
  }
}
