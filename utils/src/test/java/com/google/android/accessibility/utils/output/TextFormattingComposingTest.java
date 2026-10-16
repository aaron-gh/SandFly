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

package com.google.android.accessibility.utils.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.UnderlineSpan;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextFormattingComposingTest {
  private final Context context = RuntimeEnvironment.getApplication();

  private static SpannableString underlined(int flags) {
    SpannableString text = new SpannableString("hello world");
    text.setSpan(new UnderlineSpan(), 6, 11, flags);
    return text;
  }

  private int infoCount(SpannableString text) {
    CharacterStyle[] spans = text.getSpans(0, text.length(), CharacterStyle.class);
    return new TextFormattingInfo(
            context, Arrays.asList(spans), text, TextFormattingUtils.OPTION_UNDERLINE)
        .getSelfInfos()
        .size();
  }

  @Test
  public void composingUnderline_isNotFormatting() {
    SpannableString text =
        underlined(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
    assertTrue(
        TextFormattingUtils.isComposingSpan(text, text.getSpans(0, 11, UnderlineSpan.class)[0]));
    assertEquals(0, infoCount(text));
  }

  @Test
  public void realUnderline_isFormatting() {
    SpannableString text = underlined(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    assertFalse(
        TextFormattingUtils.isComposingSpan(text, text.getSpans(0, 11, UnderlineSpan.class)[0]));
    assertEquals(1, infoCount(text));
  }
}
