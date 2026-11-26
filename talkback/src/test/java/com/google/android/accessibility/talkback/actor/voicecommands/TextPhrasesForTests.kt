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

package com.google.android.accessibility.talkback.actor.voicecommands

/**
 * The text editing phrases from [array], which gives the items of the string-array named
 * voice_commands_ and the name it is given, as [VoiceTargets.textPhrases] reads them.
 */
internal fun textPhrasesForTests(array: (String) -> List<String>): VoiceGrammar.TextPhrases =
  VoiceGrammar.TextPhrases(
    move = array("text_move"),
    select = array("text_select"),
    copy = array("text_copy"),
    cut = array("text_cut"),
    delete = array("text_delete"),
    next = array("text_next"),
    previous = array("text_previous"),
    start = array("text_start"),
    end = array("text_end"),
    units =
      mapOf(
        TextUnit.CHARACTER to array("text_characters"),
        TextUnit.WORD to array("text_words"),
        TextUnit.SENTENCE to array("text_sentences"),
        TextUnit.LINE to array("text_lines"),
        TextUnit.PARAGRAPH to array("text_paragraphs"),
        TextUnit.TEXT to array("text_whole"),
      ),
    numbers = array("text_numbers"),
  )
