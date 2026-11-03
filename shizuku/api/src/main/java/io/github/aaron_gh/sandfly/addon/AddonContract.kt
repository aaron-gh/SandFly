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

package io.github.aaron_gh.sandfly.addon

/** What SandFly and the SandFly add-on agree on to connect. */
object AddonContract {
  /** The add-on's package. */
  const val PACKAGE = "io.github.aaron_gh.sandfly.addon"

  /**
   * The permission that binding to the add-on's service needs. Both apps define it with signature
   * protection, so only apps signed with SandFly's key have it, whichever is installed first.
   */
  const val PERMISSION = "io.github.aaron_gh.sandfly.permission.ADDON"

  /** The action of the add-on's [IAddon] service. */
  const val ACTION_BIND = "io.github.aaron_gh.sandfly.addon.BIND"

  /** The version of [IAddon] in this build. */
  const val INTERFACE_VERSION = 1
}
