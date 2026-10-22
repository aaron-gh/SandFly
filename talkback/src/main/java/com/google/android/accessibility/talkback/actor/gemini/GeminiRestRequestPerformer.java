/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.accessibility.talkback.actor.gemini;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.GeminiResponse;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONObject;

/** Performs Gemini REST requests. */
public class GeminiRestRequestPerformer {
  private static final String TAG = "GeminiRequest";
  private static final String API_KEY_HEADER = "x-goog-api-key";
  private static final int TIMEOUT_MS = 30_000;

  /**
   * How long a model that has another after it is given before the next is asked instead. Models
   * slow down when they are in high demand, and the lighter model behind usually answers in seconds.
   */
  private static final int TIMEOUT_BEFORE_NEXT_MODEL_MS = 15_000;

  /** The last model gets what is left of this, so the answer comes before the user gives up. */
  private static final int TOTAL_TIMEOUT_MS = 32_000;

  /** The least a model that is asked is given, in milliseconds. */
  private static final int MIN_TIMEOUT_MS = 8_000;

  /** Models that rejected the thinking setting, and are asked without one from then on. */
  private static final Set<String> modelsWithoutThinkingSetting = ConcurrentHashMap.newKeySet();
  // Retrying a request that timed out sends and pays for the same slow request again.
  private static final int MAX_RETRIES = 0;

  /**
   * How long to wait before asking every model again when each answered that it is busy (HTTP 500
   * and above, such as 503 when a model is overloaded), which often passes within seconds.
   */
  private static final long[] BUSY_RETRY_DELAYS_MS = {1_000, 2_000};

  /** No retry starts later than this after the first request, so the answer comes in time. */
  private static final long RETRY_WINDOW_MS = 20_000;

  interface GeminiRestResponseCallback {
    void onResponse(GeminiResponse response);

    void onFailure(GeminiFailure failure);

    void onCancelled();
  }

  private final RequestQueue requestQueue;
  private final Handler handler = new Handler(Looper.getMainLooper());
  /** Marks the retries waiting to be sent, so that cancelling drops them. */
  private final Object retryToken = new Object();

  public GeminiRestRequestPerformer(Context context) {
    requestQueue = Volley.newRequestQueue(context);
  }

  public boolean isKeylessInitialized() {
    return false;
  }

  /**
   * Performs an HTTP POST request to the given URL with the specified message body. The API key, if
   * not empty, goes in a header rather than the URL, because Volley and the system log URLs.
   */
  public void performRequest(
      String url,
      String apiKey,
      JSONObject postData,
      GeminiRestResponseCallback unloggedCallback) {
    performRequest(ImmutableList.of(url), apiKey, postData, unloggedCallback);
  }

  /**
   * Asks the first of [urls], one per model. When a model is busy or its usage limit is reached,
   * the next is asked at once; when every model is busy, all are asked again after a pause.
   */
  public void performRequest(
      List<String> urls,
      String apiKey,
      JSONObject postData,
      GeminiRestResponseCallback unloggedCallback) {
    send(
        urls, apiKey, postData, loggingFailures(unloggedCallback), 0, 0, SystemClock.uptimeMillis());
  }

  private void send(
      List<String> urls,
      String apiKey,
      JSONObject postData,
      GeminiRestResponseCallback callback,
      int model,
      int round,
      long firstAt) {
    String url = urls.get(model);
    String modelName = modelOf(url);
    boolean thinkingSetting = !modelsWithoutThinkingSetting.contains(modelName);
    JSONObject body = postData;
    if (thinkingSetting) {
      try {
        body = withLeastThinking(postData, modelName);
      } catch (JSONException e) {
        thinkingSetting = false;
      }
    }
    boolean sentThinkingSetting = thinkingSetting && body != postData;
    JsonObjectRequest stringRequest =
        new JsonObjectRequest(
            Request.Method.POST,
            url,
            body,
            response -> {
              try {
                GeminiResponse result = DataFieldUtils.parseGeminiResponse(response);
                callback.onResponse(result);
              } catch (JSONException e) {
                callback.onFailure(GeminiFailure.other("Could not read the answer: " + e));
              }
            },
            error -> {
              GeminiFailure failure = GeminiFailure.fromVolleyError(error);
              long now = SystemClock.uptimeMillis();
              GeminiFailure.Kind kind = failure.getKind();
              if (sentThinkingSetting && rejectedThinkingSetting(failure)) {
                LogUtils.w(
                    TAG,
                    "%s, asking again without the thinking setting: %s",
                    modelName,
                    failure.getLogMessage());
                modelsWithoutThinkingSetting.add(modelName);
                send(urls, apiKey, postData, callback, model, round, firstAt);
                return;
              }
              boolean busy = kind == GeminiFailure.Kind.BUSY;
              boolean another =
                  busy
                      || kind == GeminiFailure.Kind.QUOTA
                      || kind == GeminiFailure.Kind.TIMEOUT;
              if (another && model + 1 < urls.size() && now - firstAt < RETRY_WINDOW_MS) {
                LogUtils.w(
                    TAG, "%s, asking the next model: %s", modelName, failure.getLogMessage());
                send(urls, apiKey, postData, callback, model + 1, round, firstAt);
                return;
              }
              if (busy && round < BUSY_RETRY_DELAYS_MS.length && now - firstAt < RETRY_WINDOW_MS) {
                LogUtils.w(TAG, "Gemini is busy, asking again: %s", failure.getLogMessage());
                handler.postAtTime(
                    () -> send(urls, apiKey, postData, callback, 0, round + 1, firstAt),
                    retryToken,
                    now + BUSY_RETRY_DELAYS_MS[round]);
                return;
              }
              callback.onFailure(failure);
            }) {
          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            if (TextUtils.isEmpty(apiKey)) {
              return super.getHeaders();
            }
            Map<String, String> headers = new HashMap<>(super.getHeaders());
            headers.put(API_KEY_HEADER, apiKey);
            return headers;
          }
        };

    stringRequest.setRetryPolicy(
        new DefaultRetryPolicy(
            timeoutMs(model + 1 < urls.size(), SystemClock.uptimeMillis() - firstAt),
            MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    requestQueue.add(stringRequest);
  }

  /**
   * How long to wait for a model, [elapsedMs] after the first request: a model with another after
   * it gets [TIMEOUT_BEFORE_NEXT_MODEL_MS], and the last gets what is left of [TOTAL_TIMEOUT_MS].
   */
  static int timeoutMs(boolean anotherModelAfter, long elapsedMs) {
    if (anotherModelAfter) {
      return TIMEOUT_BEFORE_NEXT_MODEL_MS;
    }
    if (elapsedMs <= 0) {
      return TIMEOUT_MS;
    }
    return (int) Math.max(MIN_TIMEOUT_MS, Math.min(TIMEOUT_MS, TOTAL_TIMEOUT_MS - elapsedMs));
  }

  /**
   * Returns a copy of [postData] asking [model] to think as little as it can, which is most of the
   * time a thinking model takes, or [postData] itself for a model that doesn't think. Gemini 2.5
   * takes a thinking budget, which 0 turns off; Gemini 3 and later, and the -latest names, take a
   * thinking level, whose least is minimal.
   */
  static JSONObject withLeastThinking(JSONObject postData, String model) throws JSONException {
    JSONObject thinkingConfig;
    if (model.startsWith("gemini-2.5")) {
      thinkingConfig = new JSONObject().put("thinkingBudget", 0);
    } else if (model.startsWith("gemini-1") || model.startsWith("gemini-2.0")) {
      return postData;
    } else {
      thinkingConfig = new JSONObject().put("thinkingLevel", "minimal");
    }
    JSONObject copy = new JSONObject(postData.toString());
    JSONObject generationConfig = copy.optJSONObject("generationConfig");
    if (generationConfig == null) {
      generationConfig = new JSONObject();
      copy.put("generationConfig", generationConfig);
    }
    generationConfig.put("thinkingConfig", thinkingConfig);
    return copy;
  }

  /** Whether the model refused the request because of its thinking setting. */
  static boolean rejectedThinkingSetting(GeminiFailure failure) {
    return failure.getStatusCode() == 400
        && failure.getMessage().toLowerCase(Locale.ROOT).contains("thinking");
  }

  /**
   * Wraps the callback so that each failure is logged at error level, which is logged at every log
   * output level.
   */
  static GeminiRestResponseCallback loggingFailures(GeminiRestResponseCallback callback) {
    return new GeminiRestResponseCallback() {
      @Override
      public void onResponse(GeminiResponse response) {
        callback.onResponse(response);
      }

      @Override
      public void onFailure(GeminiFailure failure) {
        LogUtils.e(TAG, "Gemini request failed: %s", failure.getLogMessage());
        callback.onFailure(failure);
      }

      @Override
      public void onCancelled() {
        callback.onCancelled();
      }
    };
  }

  /** The model in a request [url], for the log. */
  private static String modelOf(String url) {
    int start = url.lastIndexOf('/') + 1;
    int end = url.indexOf(':', start);
    return end > start ? url.substring(start, end) : url;
  }

  /** Drops the retries waiting to be sent, when the request is given up. */
  public void cancelExistingRequestIfNeeded() {
    handler.removeCallbacksAndMessages(retryToken);
  }

  public boolean hasPendingTransaction() {
    return false;
  }
}
