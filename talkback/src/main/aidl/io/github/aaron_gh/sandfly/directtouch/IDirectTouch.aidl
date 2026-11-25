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

package io.github.aaron_gh.sandfly.directtouch;

import android.graphics.Rect;

/**
 * Lets an app choose where it gets direct touch while it is in front. See docs/direct-touch-api.md.
 *
 * Every call passes client, a Binder the app keeps for as long as its process runs. SandFly drops
 * the app's requests and state when that process ends. Each request replaces the previous one.
 */
oneway interface IDirectTouch {
    /** Direct touch on the whole screen. */
    void requestScreen(IBinder client);

    /**
     * Direct touch in areas, in screen pixels, and on the views with these resource IDs, such as
     * "com.example.game:id/board". Either array can be empty.
     */
    void requestAreas(IBinder client, in Rect[] areas, in String[] viewIds);

    /** No direct touch, whatever the manifest declares or the user chose, until the next request. */
    void stop(IBinder client);

    /** Drops the request, so that the manifest declaration applies again. */
    void reset(IBinder client);

    /** Sets the state that the manifest declaration's rules match. Null clears it. */
    void setState(IBinder client, String state);
}
