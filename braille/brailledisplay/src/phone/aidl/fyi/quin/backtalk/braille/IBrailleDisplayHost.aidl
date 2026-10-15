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

package fyi.quin.backtalk.braille;

/**
 * Backtalk's side of a braille display that another app provides. See docs/braille-display-api.md.
 */
oneway interface IBrailleDisplayHost {
    /**
     * The display is ready to use, with this name and number of text cells, which can be 0 for a
     * display with keys only. Call it again if either changes.
     */
    void start(String name, int cellCount);

    /** The display can't be used any more, for example because its hardware went away. */
    void stop();

    /**
     * Braille keys pressed together, with bit 0 for dot 1 up to bit 7 for dot 8, and whether the
     * space bar was one of them. They run a command or type, as on any other braille display.
     */
    void brailleKeys(int dots, boolean space);

    /**
     * Runs a braille display command, named as in Backtalk's BrailleInputEvent without the CMD_
     * prefix, such as NAV_ITEM_NEXT, or ROUTE with the cell as the argument.
     */
    void command(String name, int argument);
}
