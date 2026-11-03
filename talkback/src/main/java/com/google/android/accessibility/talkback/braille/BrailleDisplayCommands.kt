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

package com.google.android.accessibility.talkback.braille

import android.content.Context
import com.google.android.accessibility.braille.interfaces.ScreenReaderCommands
import com.google.android.accessibility.talkback.commands.Commands

/** The commands of the Commands screen, for braille display keys to run. */
object BrailleDisplayCommands : ScreenReaderCommands {
  override fun categories(context: Context): List<ScreenReaderCommands.Category> =
    Commands.categories(context).map { category ->
      ScreenReaderCommands.Category(
        context.getString(category.titleRes),
        category.commands.map { ScreenReaderCommands.Command(it.value, it.label) },
      )
    }

  override fun label(context: Context, value: String): String? = Commands.label(context, value)
}
