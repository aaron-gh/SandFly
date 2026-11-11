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
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FailoverTextToSpeechPieceIdTest {
  @Test
  public void nextPiece_followsThePiece() {
    assertEquals("talkback_860#chunk6", FailoverTextToSpeech.nextPieceId("talkback_860#chunk5"));
    assertEquals("talkback_860#chunk10", FailoverTextToSpeech.nextPieceId("talkback_860#chunk9"));
  }

  @Test
  public void nextPiece_ofWholeUtterance_isNull() {
    assertNull(FailoverTextToSpeech.nextPieceId("talkback_860"));
  }

  @Test
  public void nextPiece_withoutIndex_isNull() {
    assertNull(FailoverTextToSpeech.nextPieceId("talkback_860#chunk"));
    assertNull(FailoverTextToSpeech.nextPieceId("talkback_860#chunkx"));
  }
}
