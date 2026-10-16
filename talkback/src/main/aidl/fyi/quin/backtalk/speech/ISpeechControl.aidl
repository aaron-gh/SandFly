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

package fyi.quin.backtalk.speech;

/**
 * Lets an app the user allowed speak through Backtalk. See docs/speech-api.md.
 */
oneway interface ISpeechControl {
    /**
     * Says text in the user's Backtalk voice, after a short sound. Rate and pitch are multiples
     * of the user's own, from 0.25 to 4, and volume is from 0 to 1. With interrupt, it replaces
     * what other apps were saying, but never Backtalk's own speech.
     */
    void speak(String text, boolean interrupt, float rate, float pitch, float volume);

    /** Stops what other apps are saying. */
    void stop();
}
