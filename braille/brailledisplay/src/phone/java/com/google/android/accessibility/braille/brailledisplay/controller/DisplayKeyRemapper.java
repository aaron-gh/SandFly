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
import android.view.ViewConfiguration;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.DisplayCommandProfile;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.DisplayKeys;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.DisplayKeyOverrides;
import java.util.Objects;

/** Finds what a braille display key runs when the user assigned it to another command. */
final class DisplayKeyRemapper {
  private static final String IOS_HOME_CHORD = DisplayCommandProfile.spaceChordKey("125");

  private final Context context;
  private long lastIosHome = -1;
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
    int command = event.getCommand();
    int argument = event.getArgument();
    String key = DisplayKeys.keyOf(command, argument);
    String boundChord = DisplayKeys.boundSpaceChordKey(argument);
    boolean ios = BrailleUserPreferences.usesIosDisplayCommands(context);
    DisplayKeyOverrides bindings = current();
    String physicalKey = boundChord != null ? boundChord : key;
    String target = physicalKey == null ? null : bindings.get(physicalKey);
    if (target == null && key != null) {
      target = bindings.get(key);
    }

    if (boundChord == null && editing && EditorConsumer.typesDots(context, command)) {
      lastIosHome = -1;
      return null;
    }

    boolean iosHome =
        ios && target == null && IOS_HOME_CHORD.equals(physicalKey)
            && bindings.defaultFor(physicalKey)
                .equals(DisplayKeyOverrides.commandTarget(BrailleInputEvent.CMD_GLOBAL_HOME));
    if (iosHome) {
      long time = event.getEventTime();
      boolean doublePress =
          lastIosHome >= 0 && time >= lastIosHome
              && time - lastIosHome <= ViewConfiguration.getDoubleTapTimeout();
      lastIosHome = doublePress ? -1 : time;
      return DisplayKeyOverrides.commandTarget(
          doublePress ? BrailleInputEvent.CMD_GLOBAL_RECENTS : BrailleInputEvent.CMD_GLOBAL_HOME);
    }
    lastIosHome = -1;
    if (target != null) {
      return target;
    }
    if (editing && command == BrailleInputEvent.CMD_NAV_ITEM_PREVIOUS_SEPARATE_KEY) {
      return DisplayKeyOverrides.commandTarget(BrailleInputEvent.CMD_NAV_PAN_UP);
    }
    if (editing && command == BrailleInputEvent.CMD_NAV_ITEM_NEXT_SEPARATE_KEY) {
      return DisplayKeyOverrides.commandTarget(BrailleInputEvent.CMD_NAV_PAN_DOWN);
    }
    if (boundChord != null) {
      String chordTarget = bindings.defaultFor(boundChord);
      if (!DisplayKeyOverrides.NONE.equals(chordTarget)) {
        return chordTarget;
      }
    }
    if (key == null) {
      return DisplayKeys.isUnboundChord(command, argument) ? DisplayKeyOverrides.NONE : null;
    }
    // Preserve the editor's Enter key even when the profile moves the activation command.
    if (editing && command == BrailleInputEvent.CMD_KEY_ENTER && boundChord == null) {
      return null;
    }
    target = bindings.defaultFor(key);
    return target.equals(DisplayKeyOverrides.commandTarget(command)) ? null : target;
  }

  private DisplayKeyOverrides current() {
    String text = BrailleUserPreferences.readDisplayKeyOverridesText(context);
    if (!Objects.equals(text, savedText)) {
      savedText = text;
      overrides = DisplayKeyOverrides.parse(text);
    }
    return DisplayCommandProfile.applyDefaults(context, overrides);
  }
}
