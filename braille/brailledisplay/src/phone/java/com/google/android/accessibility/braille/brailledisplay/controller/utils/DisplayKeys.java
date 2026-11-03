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

package com.google.android.accessibility.braille.brailledisplay.controller.utils;

import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.getDotsDescription;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.SupportedCommand;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.brltty.BrailleKeyBinding;
import com.google.android.accessibility.braille.common.DisplayKeyOverrides;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.interfaces.ScreenReaderCommands;
import java.util.ArrayList;
import java.util.List;

/** Braille display keys that can be assigned to commands, and their descriptions. */
public final class DisplayKeys {
  private static final int DOTS_MASK = 0xFF;

  private DisplayKeys() {}

  /**
   * Returns the {@link DisplayKeyOverrides} key of an input event, or null when the keys that
   * produced it can't be assigned: typing, panning and routing keys.
   */
  @Nullable
  public static String keyOf(int command, int argument) {
    if (command == BrailleInputEvent.CMD_BRAILLE_KEY) {
      int dots = argument & DOTS_MASK;
      return (argument & BrailleInputEvent.DOTS_WITH_SPACE) != 0 && dots != 0
          ? DisplayKeyOverrides.chordKey(dots)
          : null;
    }
    if (command < 0 || passesCapture(command)) {
      return null;
    }
    return DisplayKeyOverrides.commandKey(command);
  }

  /** Returns whether an unbound Space chord produced the input event. */
  public static boolean isUnboundChord(int command, int argument) {
    return command == BrailleInputEvent.CMD_BRAILLE_KEY
        && (argument & BrailleInputEvent.DOTS_WITH_SPACE) != 0;
  }

  /**
   * Returns whether panning or routing keys produced the command. They keep working while keys are
   * captured.
   */
  public static boolean passesCapture(int command) {
    return switch (command) {
      case BrailleInputEvent.CMD_NAV_PAN_UP,
          BrailleInputEvent.CMD_NAV_PAN_DOWN,
          BrailleInputEvent.CMD_ROUTE,
          BrailleInputEvent.CMD_LONG_PRESS_ROUTE,
          BrailleInputEvent.CMD_ACTIVATE_CURRENT ->
          true;
      default -> false;
    };
  }

  /** Returns whether keys can be assigned to the display command. */
  public static boolean isAssignable(int command) {
    return switch (command) {
      case BrailleInputEvent.CMD_NAV_PAN_UP,
          BrailleInputEvent.CMD_NAV_PAN_DOWN,
          BrailleInputEvent.CMD_ROUTE,
          BrailleInputEvent.CMD_LONG_PRESS_ROUTE,
          BrailleInputEvent.CMD_ACTIVATE_CURRENT,
          BrailleInputEvent.CMD_BRAILLE_KEY,
          BrailleInputEvent.CMD_SELECTION_START,
          BrailleInputEvent.CMD_SELECTION_END,
          BrailleInputEvent.CMD_INCREASE_AUTO_SCROLL_DURATION,
          BrailleInputEvent.CMD_DECREASE_AUTO_SCROLL_DURATION ->
          false;
      default -> command >= 0;
    };
  }

  /** Returns the name of a {@link DisplayKeyOverrides} target. */
  public static String targetLabel(Context context, String target) {
    int command = DisplayKeyOverrides.commandOf(target);
    if (command >= 0) {
      for (SupportedCommand supportedCommand : SupportedCommand.getAllCommands(context)) {
        if (supportedCommand.getCommand() == command) {
          return supportedCommand.getCommandDescription(context.getResources());
        }
      }
      return target;
    }
    String value = DisplayKeyOverrides.screenReaderValueOf(target);
    if (value != null) {
      ScreenReaderCommands commands = ScreenReaderCommands.Provider.get();
      String label = commands == null ? null : commands.label(context, value);
      return label == null ? value : label;
    }
    return "";
  }

  /** Describes keys for the connected display, or for any display when none is connected. */
  public static class Describer {
    private final Context context;
    @Nullable private final BrailleDisplayProperties props;
    @Nullable private final ArrayList<BrailleKeyBinding> sortedBindings;

    public Describer(Context context, @Nullable BrailleDisplayProperties props) {
      this.context = context;
      this.props = props;
      sortedBindings =
          props == null ? null : BrailleKeyBindingUtils.getSortedBindingsForDisplay(props);
    }

    /** Returns how to press {@code key}, or an empty string when the display has no such keys. */
    public String describe(String key) {
      Resources resources = context.getResources();
      int dots = DisplayKeyOverrides.chordDotsOf(key);
      if (dots > 0) {
        String keys =
            resources.getString(
                R.string.bd_commands_delimiter,
                resources.getString(R.string.bd_key_space),
                getDotsDescription(resources, new BrailleCharacter((byte) dots)));
        return resources.getString(R.string.bd_commands_press_template, keys);
      }
      int command = DisplayKeyOverrides.commandOf(key);
      if (command < 0) {
        return "";
      }
      if (props != null && sortedBindings != null) {
        BrailleKeyBinding binding =
            BrailleKeyBindingUtils.getBrailleKeyBindingForCommand(command, sortedBindings);
        return binding == null
            ? ""
            : BrailleKeyBindingUtils.getFriendlyKeyNamesForCommand(
                binding, props.getFriendlyKeyNames(), context);
      }
      for (SupportedCommand supportedCommand : SupportedCommand.getAllCommands(context)) {
        if (supportedCommand.getCommand() == command) {
          return supportedCommand.getKeyDescription(resources);
        }
      }
      return "";
    }

    /** Returns how to press the keys that run {@code target}, or an empty string for none. */
    public String describeKeysFor(DisplayKeyOverrides overrides, String target) {
      List<String> descriptions = new ArrayList<>();
      for (String key : overrides.keysFor(target)) {
        String description = describe(key);
        if (!description.isEmpty()) {
          descriptions.add(description);
        }
      }
      String result = "";
      for (String description : descriptions) {
        result =
            result.isEmpty()
                ? description
                : context.getString(R.string.bd_commands_delimiter, result, description);
      }
      return result;
    }
  }
}
