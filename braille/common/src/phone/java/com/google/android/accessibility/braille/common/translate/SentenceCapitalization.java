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
import android.text.InputType;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.ImeConnection;

/**
 * Capitalizes the first letter typed at the start of a sentence, for the "Capitalize sentences"
 * braille keyboard setting.
 */
public final class SentenceCapitalization {

  /** Number of characters before the cursor read to find the end of the previous sentence. */
  static final int CONTEXT_LENGTH = 50;

  /**
   * Returns whether text typed now starts a sentence: the setting is on, the field takes
   * capitalized text, and the text before the cursor ends a sentence.
   */
  public static boolean shouldCapitalize(Context context, ImeConnection imeConnection) {
    if (imeConnection.editorInfo == null
        || !BrailleUserPreferences.readCapitalizeSentences(context)) {
      return false;
    }
    int inputType = imeConnection.editorInfo.inputType;
    if (!fieldAllowsCapitalization(inputType)) {
      return false;
    }
    return isSentenceStart(
        imeConnection.inputConnection.getTextBeforeCursor(CONTEXT_LENGTH, /* flags= */ 0));
  }

  /** Returns the text with its first letter capitalized if {@code capitalize} is set. */
  public static String apply(boolean capitalize, String text) {
    return capitalize ? capitalizeFirstLetter(text) : text;
  }

  /**
   * Returns whether a field with this input type takes sentence capitals: plain text fields only,
   * not passwords, email addresses, web addresses, numbers, phone numbers or dates.
   */
  static boolean fieldAllowsCapitalization(int inputType) {
    if ((inputType & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) {
      return false;
    }
    switch (inputType & InputType.TYPE_MASK_VARIATION) {
      case InputType.TYPE_TEXT_VARIATION_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
      case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
      case InputType.TYPE_TEXT_VARIATION_URI:
      case InputType.TYPE_TEXT_VARIATION_FILTER:
        return false;
      default:
        return true;
    }
  }

  /**
   * Returns whether text typed after {@code textBeforeCursor} starts a sentence: the field is empty
   * before the cursor, or the text ends with a line break, or with sentence-ending punctuation
   * (optionally followed by closing quotes or brackets) and then whitespace.
   */
  static boolean isSentenceStart(@Nullable CharSequence textBeforeCursor) {
    if (textBeforeCursor == null) {
      return false;
    }
    int index = textBeforeCursor.length();
    boolean sawSpace = false;
    while (index > 0) {
      char c = textBeforeCursor.charAt(index - 1);
      if (isLineBreak(c)) {
        return true;
      }
      if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
        break;
      }
      sawSpace = true;
      index--;
    }
    if (index == 0) {
      return true;
    }
    if (!sawSpace) {
      return false;
    }
    while (index > 0 && isClosingPunctuation(textBeforeCursor.charAt(index - 1))) {
      index--;
    }
    return index > 0 && isSentenceEnd(textBeforeCursor.charAt(index - 1));
  }

  /**
   * Returns {@code text} with its first letter changed to a capital, skipping opening quotes and
   * brackets. Text that starts with anything else than a lowercase letter is returned unchanged.
   */
  static String capitalizeFirstLetter(String text) {
    int index = 0;
    while (index < text.length() && isOpeningPunctuation(text.charAt(index))) {
      index++;
    }
    if (index >= text.length()) {
      return text;
    }
    int codePoint = text.codePointAt(index);
    if (!Character.isLowerCase(codePoint)) {
      return text;
    }
    int capital = Character.toTitleCase(codePoint);
    if (capital == codePoint || Character.charCount(capital) != Character.charCount(codePoint)) {
      return text;
    }
    return new StringBuilder(text.length())
        .append(text, 0, index)
        .appendCodePoint(capital)
        .append(text, index + Character.charCount(codePoint), text.length())
        .toString();
  }

  private static boolean isLineBreak(char c) {
    return c == '\n' || c == '\r' || c == ' ' || c == ' ';
  }

  private static boolean isSentenceEnd(char c) {
    switch (c) {
      case '.':
      case '!':
      case '?':
      case '…': // Horizontal ellipsis.
      case '‼': // Double exclamation mark.
      case '‽': // Interrobang.
      case '⁇': // Double question mark.
      case '⁈': // Question exclamation mark.
      case '⁉': // Exclamation question mark.
      case ';': // Greek question mark.
      case '։': // Armenian full stop.
      case '؟': // Arabic question mark.
      case '۔': // Arabic full stop.
      case '।': // Devanagari danda.
      case '॥': // Devanagari double danda.
      case '。': // Ideographic full stop.
      case '．': // Full-width full stop.
      case '！': // Full-width exclamation mark.
      case '？': // Full-width question mark.
      case '｡': // Half-width ideographic full stop.
        return true;
      default:
        return false;
    }
  }

  private static boolean isClosingPunctuation(char c) {
    if (c == '"' || c == '\'') {
      return true;
    }
    int type = Character.getType(c);
    return type == Character.END_PUNCTUATION || type == Character.FINAL_QUOTE_PUNCTUATION;
  }

  private static boolean isOpeningPunctuation(char c) {
    if (c == '"' || c == '\'' || c == '¿' || c == '¡') {
      return true;
    }
    int type = Character.getType(c);
    return type == Character.START_PUNCTUATION || type == Character.INITIAL_QUOTE_PUNCTUATION;
  }

  private SentenceCapitalization() {}
}
