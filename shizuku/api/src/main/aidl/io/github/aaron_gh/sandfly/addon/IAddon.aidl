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

package io.github.aaron_gh.sandfly.addon;

import io.github.aaron_gh.sandfly.addon.IAddonListener;
import io.github.aaron_gh.sandfly.addon.IShellConnection;

/**
 * The SandFly add-on's service, which SandFly binds to. Only apps signed with SandFly's key may
 * bind to it. SandFly and the add-on are updated separately, so add new methods only at the end,
 * with new transaction codes, and raise AddonContract.INTERFACE_VERSION.
 */
interface IAddon {
    /** The version of this interface that the add-on has. */
    int interfaceVersion() = 1;

    /** Whether Shizuku is running and new enough. */
    boolean isShizukuReady() = 2;

    /** Whether Shizuku allows the add-on. */
    boolean hasShizukuPermission() = 3;

    /** Whether the user chose "Deny and don't ask again" for the add-on in Shizuku. */
    boolean isShizukuPermissionDenied() = 4;

    /** Shows Shizuku's request to allow the add-on. The answer goes to the listener. */
    oneway void requestShizukuPermission() = 5;

    /** Sets the listener for changes to Shizuku, replacing the one before. Null removes it. */
    void setListener(IAddonListener listener) = 6;

    /**
     * Starts the add-on's shell service, an IShizukuGrants, in a process named after [tag], and
     * gives its binder to [connection].
     */
    oneway void bindShell(String tag, IShellConnection connection) = 7;

    /** Stops the shell service that [connection] was given, and its process. */
    oneway void unbindShell(IShellConnection connection) = 8;
}
