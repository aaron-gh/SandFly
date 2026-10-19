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

package com.google.android.accessibility.utils.input;

import static org.junit.Assert.assertEquals;

import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Which text changes that come straight after another one are still sent on. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventFilterDropTest {
  private static final long START = 10_000;

  private final List<TextEventInterpretation> sent = new ArrayList<>();
  private TextEventFilter filter;

  @Before
  public void setUp() {
    filter =
        new TextEventFilter(
            RuntimeEnvironment.getApplication(),
            /* textCursorTracker= */ null,
            new TextEventHistory());
    filter.addListener(interpretation -> sent.add(interpretation.interpretation));
  }

  private void change(long time, String initialWord) {
    AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
    event.setEventTime(time);
    event.setPackageName("app");
    TextEventInterpretation interpretation =
        new TextEventInterpretation(TextEventInterpretation.TEXT_ADD);
    interpretation.setInitialWord(initialWord);
    filter.filterAndSendInterpretation(event, /* eventId= */ null, interpretation);
  }

  @Test
  public void changeSoonAfterAnotherIsDropped() {
    change(START, "");
    change(START + 50, "");
    assertEquals(1, sent.size());
  }

  @Test
  public void wordEndingChangeAfterACorrectionIsKept() {
    change(START, "");
    change(START + 50, "non-normal ");
    assertEquals(2, sent.size());
    assertEquals("non-normal ", sent.get(1).getInitialWord().toString());
  }

  @Test
  public void secondWordEndingChangeIsDropped() {
    change(START, "hello ");
    change(START + 50, "hello ");
    assertEquals(1, sent.size());
  }
}
