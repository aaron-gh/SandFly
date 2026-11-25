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

package com.google.android.accessibility.braille.translate;

import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import java.util.Objects;

/**
 * Translates braille to print with one translator and print to braille with another, for a
 * typing-only table such as Unicode braille, whose text a braille display shows in the reading
 * table.
 */
public class SplitTranslator implements BrailleTranslator {
  private final BrailleTranslator toPrint;
  private final BrailleTranslator toBraille;

  public SplitTranslator(BrailleTranslator toPrint, BrailleTranslator toBraille) {
    this.toPrint = toPrint;
    this.toBraille = toBraille;
  }

  @Override
  public String translateToPrint(BrailleWord brailleWord) {
    return toPrint.translateToPrint(brailleWord);
  }

  @Override
  public String translateToPrint(BrailleWord brailleWord, String before, String after) {
    return toPrint.translateToPrint(brailleWord, before, after);
  }

  @Override
  public String translateToPrintPartial(BrailleWord brailleWord) {
    return toPrint.translateToPrintPartial(brailleWord);
  }

  @Override
  public TranslationResult translate(CharSequence text, int cursorPosition) {
    return toBraille.translate(text, cursorPosition);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SplitTranslator other)) {
      return false;
    }
    return toPrint.equals(other.toPrint) && toBraille.equals(other.toBraille);
  }

  @Override
  public int hashCode() {
    return Objects.hash(toPrint, toBraille);
  }
}
