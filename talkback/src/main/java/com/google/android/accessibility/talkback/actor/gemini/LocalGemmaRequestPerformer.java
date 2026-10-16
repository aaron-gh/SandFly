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

package com.google.android.accessibility.talkback.actor.gemini;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager;
import com.google.android.accessibility.talkback.actor.gemini.local.OnDeviceAiSettings;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONObject;

/**
 * Sends Gemini requests to the cloud, or to the model on the phone when the user chose on-device
 * AI. The choice is read for each request, so changing it needs no restart.
 */
public class LocalGemmaRequestPerformer extends GeminiRestRequestPerformer {

  private final Context context;
  private final SharedPreferences prefs;
  private final LocalGemmaRunner runner;

  /**
   * @param announce speaks why the model on the phone could not answer, such as low memory
   */
  public LocalGemmaRequestPerformer(Context context, Consumer<String> announce) {
    super(context);
    this.context = context.getApplicationContext();
    this.prefs = SharedPreferencesUtils.getSharedPreferences(context);
    ExecutorService worker =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "on-device-ai");
              thread.setDaemon(true);
              return thread;
            });
    ExecutorService canceller =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "on-device-ai-cancel");
              thread.setDaemon(true);
              return thread;
            });
    Handler mainHandler = new Handler(Looper.getMainLooper());
    this.runner =
        new LocalGemmaRunner(
            () -> LocalModelManager.Companion.get(this.context).llm(),
            () -> LocalModelManager.Companion.get(this.context).currentLlm(),
            worker,
            canceller,
            mainHandler::post,
            announce);
  }

  private boolean useOnDevice() {
    return OnDeviceAiSettings.INSTANCE.isOnDevice(prefs);
  }

  @Override
  public boolean isKeylessInitialized() {
    return useOnDevice() && LocalModelManager.Companion.get(context).isReady();
  }

  @Override
  public void performRequest(
      String url, String apiKey, JSONObject postData, GeminiRestResponseCallback callback) {
    if (useOnDevice()) {
      runner.run(postData, loggingFailures(callback));
    } else {
      super.performRequest(url, apiKey, postData, callback);
    }
  }

  @Override
  public void cancelExistingRequestIfNeeded() {
    runner.cancel();
    super.cancelExistingRequestIfNeeded();
  }

  @Override
  public boolean hasPendingTransaction() {
    return runner.getHasPending() || super.hasPendingTransaction();
  }
}
