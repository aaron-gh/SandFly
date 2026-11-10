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

package com.google.android.accessibility.braille.brailledisplay.controller;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.DisplayKeys;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.DisplayKeyOverrides;
import java.util.Objects;

/** Finds what a braille display key runs when the user assigned it to another command. */
final class DisplayKeyRemapper {
  private final Context context;
  @Nullable private String savedText;
  private DisplayKeyOverrides overrides = DisplayKeyOverrides.parse(null);

  DisplayKeyRemapper(Context context) {
    this.context = context;
  }

  /**
   * Returns the {@link DisplayKeyOverrides} target that the event runs instead of its own command,
   * {@link DisplayKeyOverrides#NONE} to ignore it, or null to run it unchanged.
   *
   * @param editing whether a text field takes the display's input
   */
  @Nullable
  String targetFor(BrailleInputEvent event, boolean editing) {
    String key = DisplayKeys.keyOf(event.getCommand(), event.getArgument());
    if (key == null) {
      return DisplayKeys.isUnboundChord(event.getCommand(), event.getArgument())
          ? DisplayKeyOverrides.NONE
          : null;
    }
    String target = current().get(key);
    if (target == null) {
      // Unassigned, a chord runs nothing, a separate key runs the command it stands in for, and
      // other keys run their own command.
      int command = DisplayKeyOverrides.commandOf(key);
      if (command < 0) {
        return DisplayKeyOverrides.NONE;
      }
      int defaultCommand = DisplayKeyOverrides.defaultCommandOf(command);
      return defaultCommand == command ? null : DisplayKeyOverrides.commandTarget(defaultCommand);
    }
    if (editing) {
      // Keys of commands without Space type their dots in text fields.
      if (EditorConsumer.typesDots(context, DisplayKeyOverrides.commandOf(key))) {
        return null;
      }
      int command = DisplayKeyOverrides.commandOf(target);
      if (command >= 0 && EditorConsumer.typesDots(context, command)) {
        return DisplayKeyOverrides.NONE;
      }
    }
    return target;
  }

  private DisplayKeyOverrides current() {
    String text = BrailleUserPreferences.readDisplayKeyOverridesText(context);
    if (!Objects.equals(text, savedText)) {
      savedText = text;
      overrides = DisplayKeyOverrides.parse(text);
    }
    return overrides;
  }
}
