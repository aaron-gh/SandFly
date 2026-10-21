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

    /**
     * Whether the switch of the command with the ordinal [command] in ShizukuCommand is on: 1 for
     * on, 0 for off, and -1 if it can't be read.
     */
    int state(int command) = 3;

    /**
     * Installs the APKs read from [apks], of the given [sizes], as an update to [packageName],
     * recording [installer] as the package that installed it. The shell can record any installer
     * at install time, which an app itself can't. Returns null if it worked, or what went wrong.
     */
    String install(
        in ParcelFileDescriptor[] apks,
        in long[] sizes,
        String packageName,
        String installer) = 4;
}
