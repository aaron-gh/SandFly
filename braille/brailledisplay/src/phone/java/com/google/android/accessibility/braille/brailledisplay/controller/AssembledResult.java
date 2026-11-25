/*
 * Copyright (C) 2023 Google Inc.
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

package com.google.android.accessibility.braille.brailledisplay.controller;

import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.PASSWORD_BULLET;
import static com.google.android.accessibility.braille.common.translate.EditBufferUtils.NO_CURSOR;
import static com.google.android.accessibility.braille.interfaces.BrailleCharacter.EMPTY_CELL;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.text.TextUtils;
import android.util.Range;

import com.google.android.accessibility.braille.interfaces.BrailleDisplayForBrailleIme.ResultForDisplay;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.interfaces.SelectionRange;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import com.google.android.accessibility.braille.translate.TranslationResult;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.List;

/** Assembles the field hint, text, and pending braille for the display. */
@AutoValue
abstract class AssembledResult {
  /** The index of the selected text in the byte array. */
  abstract SelectionRange textByteSelection();
  /** The range of clickable index of text in the text field in the byte array. */
  abstract ImmutableList<Range<Integer>> textFieldTextClickableByteRange();
  /** The range of clickable index of holdings in the byte array. */
  abstract Range<Integer> holdingsClickableByteRange();

  /** TranslationResult of overlayText. */
  abstract TranslationResult overlayTranslationResult();
  /** Builder class for {@link AssembledResult}. */
  static class Builder {
    private static final String SPACE = " ";
    private static final String COLON = ":";
    private final StringBuilder textOnOverlay = new StringBuilder();
    private final BrailleWord brailleWord = new BrailleWord();
    private final List<Range<Integer>> textFieldTextClickableByteRange = new ArrayList<>();
    private final List<Integer> textToBraillePositions = new ArrayList<>();
    private final List<Integer> brailleToTextPositions = new ArrayList<>();
    private final ResultForDisplay resultForDisplay;
    private final BrailleTranslator translator;
    private final String textFieldText;
    private final Range<Integer> textSelectionRange;
    private final int holdingsPosition;
    private final BrailleWord holdingsWord;
    private final BrailleWord textFieldWord;
    private final ImmutableList<Integer> textFieldTextToBraillePositions;
    private final ImmutableList<Integer> textFieldTextBrailleToTextPositions;
    private final boolean textMasked;
    private Range<Integer> holdingsClickableByteRange;
    private int textDisplacement;
    private boolean appendHint;

    public Builder(BrailleTranslator translator, ResultForDisplay resultForDisplay) {
      this.translator = translator;
      this.resultForDisplay = resultForDisplay;
      this.textMasked = resultForDisplay.textMasked();
      BrailleWord holdingsWord =
          new BrailleWord(resultForDisplay.holdingsInfo().holdings().array());
      int holdingsPosition = resultForDisplay.holdingsInfo().position();
      String textFieldText = String.valueOf(resultForDisplay.onScreenText());
      textSelectionRange =
          new Range<>(
              resultForDisplay.textSelection().getLower(),
              resultForDisplay.textSelection().getUpper());
      if (textMasked) {
        BrailleWord bullet = translateIfPossible(PASSWORD_BULLET, NO_CURSOR).cells();
        this.holdingsWord = new BrailleWord(bullet, holdingsWord.size());
        this.holdingsPosition = bullet.size() * holdingsPosition;
        this.textFieldText = Strings.repeat(PASSWORD_BULLET, textFieldText.length());
      } else {
        this.holdingsWord = holdingsWord;
        this.holdingsPosition = holdingsPosition;
        this.textFieldText = textFieldText;
      }
      TranslationResult translationResult =
          translateIfPossible(this.textFieldText, this.textSelectionRange.getLower());
      textFieldWord = new BrailleWord(translationResult.cells());
      textFieldTextToBraillePositions = translationResult.textToBraillePositions();
      textFieldTextBrailleToTextPositions = translationResult.brailleToTextPositions();
      holdingsClickableByteRange = new Range<>(NO_CURSOR, NO_CURSOR);
    }

    /** Whether to append hint. */
    @CanIgnoreReturnValue
    public Builder appendHint(boolean append) {
      appendHint = append;
      return this;
    }

    private void doAppendHint() {
      if (TextUtils.isEmpty(resultForDisplay.hint())) {
        return;
      }
      StringBuilder hint =
          new StringBuilder().append(resultForDisplay.hint()).append(COLON).append(SPACE);
      TranslationResult result = translateIfPossible(hint, NO_CURSOR);
      textOnOverlay.append(hint);
      brailleWord.append(new BrailleWord(result.cells()));
      appendPositionsToList(
          textToBraillePositions,
          /* base= */ 0,
          /* startIndexToAppend= */ 0,
          /* endIndexToAppend= */ result.textToBraillePositions().size(),
          /* positionsToCopy= */ result.textToBraillePositions());
      appendPositionsToList(
          brailleToTextPositions,
          /* base= */ 0,
          /* startIndexToAppend= */ 0,
          /* endIndexToAppend= */ result.brailleToTextPositions().size(),
          /* positionsToCopy= */ result.brailleToTextPositions());
      // user input is put after hint.
      textDisplacement += hint.length();
    }

    private TranslationResult translateIfPossible(CharSequence text, int cursorPosition) {
      TranslationResult result = translator.translate(text, cursorPosition);
      if (result == null) {
        result = TranslationResult.createUnknown(text, cursorPosition);
      }
      return result;
    }

    public AssembledResult build() {
      if (appendHint) {
        doAppendHint();
      }
      appendTextFieldTextBeforeSelection();
      appendTextFieldTextBetweenSelection();
      appendHoldings();
      appendTextFieldTextAfterSelection();
      // Keep a cell available for the cursor at the end of the field.
      appendSpace();
      int lowerByteIndex;
      if (textSelectionRange.getLower() == NO_CURSOR) {
        lowerByteIndex = NO_CURSOR;
      } else {
        int lowerIndex = textSelectionRange.getLower() + textDisplacement;
        lowerByteIndex =
            lowerIndex == textOnOverlay.length()
                ? brailleWord.size()
                : textToBraillePositions.get(lowerIndex);
      }
      int upperByteIndex;
      if (textSelectionRange.getUpper() == NO_CURSOR) {
        upperByteIndex = NO_CURSOR;
      } else {
        int upperIndex = textSelectionRange.getUpper() + textDisplacement;
        upperByteIndex =
            upperIndex == textOnOverlay.length()
                ? brailleWord.size()
                : textToBraillePositions.get(upperIndex);
      }
      TranslationResult allTranslationResult =
          TranslationResult.builder()
              .setText(textOnOverlay.toString())
              .setCells(brailleWord)
              .setTextToBraillePositions(textToBraillePositions)
              .setBrailleToTextPositions(brailleToTextPositions)
              .setCursorBytePosition(lowerByteIndex)
              .build();
      return new AutoValue_AssembledResult(
          new SelectionRange(lowerByteIndex, upperByteIndex),
          ImmutableList.copyOf(textFieldTextClickableByteRange),
          holdingsClickableByteRange,
          allTranslationResult);
    }

    private void appendTextFieldTextBeforeSelection() {
      if (TextUtils.isEmpty(textFieldText) || textSelectionRange.getLower() <= 0) {
        return;
      }
      int textFieldByteBeforeCursorLength = textFieldWord.size();
      boolean cursorAtEnd = textSelectionRange.getLower() == NO_CURSOR || cursorInTheEnd();
      int textIndexBeforeCursor =
          textSelectionRange.getLower() == NO_CURSOR
              ? textFieldText.length() - 1
              : textSelectionRange.getLower();
      if (!cursorAtEnd) {
        textFieldByteBeforeCursorLength =
            textFieldTextToBraillePositions.get(textIndexBeforeCursor);
      }
      String textFieldTextBeforeCursor = textFieldText.substring(0, textIndexBeforeCursor);
      BrailleWord textFieldSubwordBeforeCursor =
          textFieldWord.subword(0, textFieldByteBeforeCursorLength);
      textFieldTextClickableByteRange.add(
          createValidatedRange(
              brailleWord.size(),
              brailleWord.size()
                  + (cursorAtEnd
                      ? textFieldSubwordBeforeCursor.size()
                      : textFieldSubwordBeforeCursor.size() - 1)));
      appendPositionsToList(
          textToBraillePositions,
          /* base= */ brailleWord.size(),
          /* startIndexToAppend= */ 0,
          /* endIndexToAppend= */ textSelectionRange.getLower(),
          /* positionsToCopy= */ textFieldTextToBraillePositions);
      appendPositionsToList(
          brailleToTextPositions,
          /* base= */ textOnOverlay.length(),
          /* startIndexToAppend= */ 0,
          /* endIndexToAppend= */ textFieldByteBeforeCursorLength,
          /* positionsToCopy= */ textFieldTextBrailleToTextPositions);
      brailleWord.append(textFieldSubwordBeforeCursor);
      textOnOverlay.append(textFieldTextBeforeCursor);
    }

    private void appendTextFieldTextBetweenSelection() {
      if (TextUtils.isEmpty(textFieldText) || noSelection()) {
        return;
      }
      int textIndexBeforeCursor =
          textSelectionRange.getUpper() == NO_CURSOR
              ? textFieldText.length() - 1
              : textSelectionRange.getLower();
      int textIndexAfterCursor = textSelectionRange.getUpper();
      String textFieldTextBetweenCursor =
          textFieldText.substring(textIndexBeforeCursor, textIndexAfterCursor);
      int textFieldWordEnd =
          textIndexAfterCursor != textFieldText.length()
              ? textFieldTextToBraillePositions.get(textIndexAfterCursor)
              : textFieldWord.size();
      BrailleWord textFieldSubwordBetweenCursor =
          textFieldWord.subword(
              textFieldTextToBraillePositions.get(textIndexBeforeCursor), textFieldWordEnd);
      appendPositionsToList(
          textToBraillePositions,
          /* base= */ brailleWord.size()
              - textFieldTextToBraillePositions.get(textIndexBeforeCursor),
          /* startIndexToAppend= */ textIndexBeforeCursor,
          /* endIndexToAppend= */ textIndexAfterCursor,
          /* positionsToCopy= */ textFieldTextToBraillePositions);
      appendPositionsToList(
          brailleToTextPositions,
          /* base= */ textOnOverlay.length() - textIndexBeforeCursor,
          /* startIndexToAppend= */ textFieldTextToBraillePositions.get(textIndexBeforeCursor),
          /* endIndexToAppend= */ textFieldWordEnd,
          /* positionsToCopy= */ textFieldTextBrailleToTextPositions);
      textFieldTextClickableByteRange.add(
          createValidatedRange(
              brailleWord.size(), brailleWord.size() + textFieldSubwordBetweenCursor.size()));
      textOnOverlay.append(textFieldTextBetweenCursor);
      brailleWord.append(textFieldSubwordBetweenCursor);
    }

    private void appendHoldings() {
      if (holdingsWord.isEmpty()) {
        return;
      }
      boolean extendForCursor =
          holdingsWord.size() == holdingsPosition
              && textFieldText.length() == textSelectionRange.getLower();
      holdingsClickableByteRange =
          createValidatedRange(brailleWord.size(), brailleWord.size() + holdingsWord.size());
      for (int i = 0; i < holdingsWord.size(); i++) {
        textToBraillePositions.add(brailleWord.size());
        brailleToTextPositions.add(textOnOverlay.length());
        // Translate holdings to space.
        textOnOverlay.append(SPACE);
        brailleWord.append(holdingsWord.get(i));
      }
      if (extendForCursor) {
        appendSpace();
      }
      textDisplacement += holdingsPosition;
    }

    private void appendTextFieldTextAfterSelection() {
      if (TextUtils.isEmpty(textFieldText)
          || textSelectionRange.getUpper().intValue() == textFieldText.length()) {
        return;
      }
      int textAfterCursor =
          textSelectionRange.getUpper() == NO_CURSOR ? 0 : textSelectionRange.getUpper();
      String textFieldTextAfterCursor =
          textSelectionRange.getUpper() == NO_CURSOR
              ? textFieldText
              : textFieldText.substring(textAfterCursor);
      int textFieldWordStart = textFieldTextToBraillePositions.get(textAfterCursor);
      BrailleWord textFieldSubwordAfterCursor =
          textFieldWord.subword(textFieldWordStart, textFieldWord.size());
      appendPositionsToList(
          textToBraillePositions,
          /* base= */ brailleWord.size() - textFieldWordStart,
          /* startIndexToAppend= */ textAfterCursor,
          /* endIndexToAppend= */ textFieldText.length(),
          /* positionsToCopy= */ textFieldTextToBraillePositions);
      appendPositionsToList(
          brailleToTextPositions,
          /* base= */ textOnOverlay.length() - textAfterCursor,
          /* startIndexToAppend= */ textFieldWordStart,
          /* endIndexToAppend= */ textFieldWord.size(),
          /* positionsToCopy= */ textFieldTextBrailleToTextPositions);
      textFieldTextClickableByteRange.add(
          createValidatedRange(
              brailleWord.size(), brailleWord.size() + textFieldSubwordAfterCursor.size()));
      textOnOverlay.append(textFieldTextAfterCursor);
      brailleWord.append(textFieldSubwordAfterCursor);
    }

    private boolean cursorInTheEnd() {
      return textSelectionRange.getLower() == textFieldText.length();
    }

    private boolean noSelection() {
      return textSelectionRange.getLower().intValue() == textSelectionRange.getUpper().intValue();
    }

    private void appendSpace() {
      textToBraillePositions.add(brailleWord.size());
      brailleToTextPositions.add(textOnOverlay.length());
      brailleWord.append(EMPTY_CELL);
      textOnOverlay.append(SPACE);
    }

    private Range<Integer> createValidatedRange(int value1, int value2) {
      Integer lower = min(value1, value2);
      Integer upper = max(value1, value2);
      return new Range<>(lower, upper);
    }

    private static void appendPositionsToList(
        List<Integer> list,
        int base,
        int startIndexToAppend,
        int endIndexToAppend,
        List<Integer> positionsToCopy) {
      for (int i = startIndexToAppend; i < endIndexToAppend; i++) {
        list.add(base + positionsToCopy.get(i));
      }
    }
  }
}
