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

package com.google.android.accessibility.talkback.scripting

import org.json.JSONArray
import org.json.JSONObject

fun jsonOf(value: Any?): String =
  JSONArray().put(value ?: JSONObject.NULL).toString().let { it.substring(1, it.length - 1) }

fun parseJson(json: String?): Any =
  if (json == null) JSONObject.NULL else JSONArray("[$json]").get(0)

fun jsonObject(vararg fields: Pair<String, Any?>): JSONObject =
  JSONObject().apply { fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) } }

fun JSONArray?.items(): List<Any> = if (this == null) emptyList() else List(length()) { get(it) }

fun JSONArray?.strings(): List<String> = items().map { it.toString() }

fun JSONArray?.options(): List<Pair<String, String>> =
  items().map { option ->
    if (option is JSONObject) {
      option.optString("value").let { it to option.optString("label", it) }
    } else {
      option.toString() to option.toString()
    }
  }
