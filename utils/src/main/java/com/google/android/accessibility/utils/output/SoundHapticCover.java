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

import com.google.android.accessibility.utils.Performance.EventId;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Decides when an event's own vibration is skipped because the event's sound already played a
 * vibration of its own, so that an event that asks for a sound and a vibration vibrates once.
 */
final class SoundHapticCover {
  /** How long after a sound's vibration a vibration for the same event is skipped. */
  static final long COVER_MILLIS = 50;

  /** When the last sound with a vibration played, or -1 if none has. */
  private long lastSoundHapticMillis = -1;

  /** The event of the last sound with a vibration. */
  private @Nullable EventId lastSoundHapticEventId;

  /**
   * Records that a sound for {@code eventId} had a vibration at {@code nowMillis}, even if the user
   * turned that vibration off, so the event's own vibration stays quiet either way.
   */
  void soundVibrated(@Nullable EventId eventId, long nowMillis) {
    lastSoundHapticMillis = nowMillis;
    lastSoundHapticEventId = eventId;
  }

  /** Returns whether a vibration for {@code eventId} at {@code nowMillis} should be skipped. */
  boolean covers(@Nullable EventId eventId, long nowMillis) {
    return lastSoundHapticMillis >= 0
        && nowMillis - lastSoundHapticMillis < COVER_MILLIS
        && Objects.equals(eventId, lastSoundHapticEventId);
  }
}
