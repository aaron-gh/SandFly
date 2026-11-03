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

package com.google.android.accessibility.braille.interfaces;

import android.content.Context;
import androidx.annotation.Nullable;
import java.util.List;

/**
 * The screen reader commands that gestures run, for the braille display settings to assign to
 * display keys.
 */
public interface ScreenReaderCommands {

  /** A command: the value saved for it and its name. */
  final class Command {
    public final String value;
    public final String label;

    public Command(String value, String label) {
      this.value = value;
      this.label = label;
    }
  }

  /** A titled group of commands. */
  final class Category {
    public final String title;
    public final List<Command> commands;

    public Category(String title, List<Command> commands) {
      this.title = title;
      this.commands = commands;
    }
  }

  /** Returns the commands available on this device, by category. */
  List<Category> categories(Context context);

  /** Returns the name of the command saved as {@code value}, or null when it isn't one. */
  @Nullable
  String label(Context context, String value);

  /** Holds the screen reader's implementation. */
  final class Provider {
    @Nullable private static volatile ScreenReaderCommands instance;

    private Provider() {}

    /** Sets the implementation. */
    public static void set(ScreenReaderCommands commands) {
      instance = commands;
    }

    /** Returns the implementation, or null when none is set. */
    @Nullable
    public static ScreenReaderCommands get() {
      return instance;
    }
  }
}
