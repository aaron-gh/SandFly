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

package com.google.android.accessibility.talkback.selector

import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import com.google.android.accessibility.utils.input.CursorGranularity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Read from focused item by reading control moves by the reading control's kind of item, and reads
 * every item for any other reading control.
 */
class ReadingGranularityTest {
  @Test
  fun itemKinds_moveByThatKind() {
    assertEquals(
      CursorGranularity.HEADING,
      SelectorController.readingGranularityOf(Setting.GRANULARITY_HEADINGS),
    )
    assertEquals(
      CursorGranularity.LINK,
      SelectorController.readingGranularityOf(Setting.GRANULARITY_LINKS),
    )
    assertEquals(
      CursorGranularity.CONTROL,
      SelectorController.readingGranularityOf(Setting.GRANULARITY_CONTROLS),
    )
    assertEquals(
      CursorGranularity.WEB_BUTTON,
      SelectorController.readingGranularityOf(Setting.GRANULARITY_WEB_BUTTONS),
    )
  }

  @Test
  fun otherReadingControls_readEveryItem() {
    assertEquals(
      CursorGranularity.DEFAULT,
      SelectorController.readingGranularityOf(Setting.GRANULARITY_WORDS),
    )
    assertEquals(
      CursorGranularity.DEFAULT,
      SelectorController.readingGranularityOf(Setting.GRANULARITY_CONTAINERS),
    )
    assertEquals(CursorGranularity.DEFAULT, SelectorController.readingGranularityOf(Setting.SPEECH_RATE))
  }
}
