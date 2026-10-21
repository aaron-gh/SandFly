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

package com.google.android.accessibility.talkback.shizuku;

/**
 * Runs as the shell user through Shizuku. Grants SandFly one of a few fixed permissions, and runs
 * the fixed commands of SandFly's quick settings.
 */
interface IShizukuGrants {
    /** Shizuku calls this when it stops the service. */
    void destroy() = 16777114;

    /**
     * Grants the permission with the ordinal [grant] in ShizukuGrants.Grant to [packageName].
     * Returns null if it was granted, or what went wrong.
     */
    String grant(int grant, String packageName) = 1;

    /**
     * Runs the command with the ordinal [command] in ShizukuCommand, with [argument]. Returns null
     * if it worked, or what went wrong.
     */
    String run(int command, String argument) = 2;
}
