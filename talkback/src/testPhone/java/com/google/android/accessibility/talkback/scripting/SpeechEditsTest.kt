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

package com.google.android.accessibility.talkback.scripting

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechEditsTest {
  private fun edit(op: String, find: String? = null, with: String? = null, regex: Boolean = false, text: String? = null, count: Int = 0) =
    JSONObject().put("op", op).apply {
      find?.let { put("find", it) }
      with?.let { put("with", it) }
      text?.let { put("text", it) }
      if (regex) put("regex", true)
      if (count > 0) put("count", count)
    }

  @Test
  fun facebookPost_losesItsClutter() {
    val post =
      "Penrith Leisure Centre profile picture, Penrith Leisure Centre•Follow, Mon at 19:01•Shared with: Public, " +
        "Introducing our new Poolpod at Penrith Leisure Centre!… more, Photo 1 of 3, May be an image of segway and text, " +
        "Photo 2 of 3, May be an image of text, Photo 3 of 3, May be an image of swimming and pool, " +
        "How does Poolpod operation work?, Which venues offer Poolpod access?"
    val edits =
      JSONArray()
        .put(edit(SpeechEdits.REMOVE, " profile picture"))
        .put(edit(SpeechEdits.REMOVE, "•Follow"))
        .put(edit(SpeechEdits.REMOVE, "•Shared with: Public"))
        .put(edit(SpeechEdits.REMOVE, "… more"))
        .put(edit(SpeechEdits.REMOVE_PARTS, "^Photo \\d+ of \\d+$", regex = true))
        .put(edit(SpeechEdits.REMOVE_PARTS, "May be an image"))
        .put(edit(SpeechEdits.REMOVE_REPEATS))

    assertEquals(
      SpeechEdits.Result.Speak(
        "Penrith Leisure Centre, Mon at 19:01, Introducing our new Poolpod at Penrith Leisure Centre!, " +
          "How does Poolpod operation work?, Which venues offer Poolpod access?"
      ),
      SpeechEdits.apply(post, edits),
    )
  }

  @Test
  fun personRow_saysTheNameOnce() {
    val row = "Zoe Murrow, Zoe Murrow's profile picture, Add friend, Remove friend suggestion, Add friend, Remove, Zoe Murrow"
    val edits =
      JSONArray()
        .put(edit(SpeechEdits.REMOVE_PARTS, "profile picture"))
        .put(edit(SpeechEdits.REMOVE_PARTS, "^(Add friend|Remove friend suggestion|Remove)$", regex = true))
        .put(edit(SpeechEdits.REMOVE_REPEATS))
        .put(edit(SpeechEdits.AFTER, text = "suggested friend"))

    assertEquals(SpeechEdits.Result.Speak("Zoe Murrow, suggested friend"), SpeechEdits.apply(row, edits))
  }

  @Test
  fun replaceWithGroups_keepFirstParts_sayAndSilence() {
    assertEquals(
      SpeechEdits.Result.Speak("19:01 by Ana"),
      SpeechEdits.apply("Ana posted at 19:01", JSONArray().put(edit(SpeechEdits.REPLACE, "^(\\w+) posted at (.+)$", "$2 by $1", regex = true))),
    )
    assertEquals(SpeechEdits.Result.Speak("a, b"), SpeechEdits.apply("a, b, c, d", JSONArray().put(edit(SpeechEdits.KEEP_PARTS, count = 2))))
    assertEquals(SpeechEdits.Result.Speak("Post: x"), SpeechEdits.apply("x", JSONArray().put(edit(SpeechEdits.SAY, text = "Post: {text}"))))
    assertEquals(SpeechEdits.Result.Silent, SpeechEdits.apply("x", JSONArray().put(edit(SpeechEdits.SILENCE))))
    assertEquals(SpeechEdits.Result.Unchanged, SpeechEdits.apply("x", JSONArray().put(edit(SpeechEdits.REMOVE, "y"))))
  }
}
