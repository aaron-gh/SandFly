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

package com.google.android.accessibility.talkback.handwriting

/** A point of a handwritten stroke, in screen pixels, at [time] in milliseconds. */
class InkPoint(val x: Float, val y: Float, val time: Long)

/** Recognizes handwriting on the device. */
interface HandwritingRecognizer {
  sealed interface Result {
    /** What the strokes may be, most likely first. */
    class Candidates(val texts: List<String>) : Result

    /** The model for the language isn't on the device. It is downloading now. */
    object Downloading : Result

    /** There is no handwriting model for the language. */
    object UnsupportedLanguage : Result

    object Failed : Result
  }

  /**
   * Recognizes [strokes], written in an area of [width] by [height] pixels, and calls [onResult]
   * on the main thread.
   */
  fun recognize(strokes: List<List<InkPoint>>, width: Float, height: Float, onResult: (Result) -> Unit)

  /**
   * Checks that the model for the language is on the device, and starts downloading it if not.
   * Calls [onNotReady] on the main thread with [Result.Downloading] or [Result.UnsupportedLanguage]
   * when handwriting can't be recognized yet, and nothing otherwise.
   */
  fun prepare(onNotReady: (Result) -> Unit)

  fun close()
}
