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

import fyi.quin.backtalk.braille.IBrailleDisplayHost;

/**
 * A braille display that another app provides. Backtalk binds the app's service and calls these.
 * See docs/braille-display-api.md.
 */
oneway interface IBrailleDisplay {
    /** Backtalk is ready. The display uses host to start, to send its keys and to stop. */
    void onHostConnected(IBrailleDisplayHost host);

    /** Shows these cells, one byte each, with bit 0 for dot 1 up to bit 7 for dot 8. */
    void showCells(in byte[] cells);
}
