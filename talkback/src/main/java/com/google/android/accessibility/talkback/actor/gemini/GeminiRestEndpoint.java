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

import java.util.ArrayList;
import java.util.List;
import static com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.FINISH_REASON_STOP;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.Action.UNKNOWN;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Base64;
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.GeminiResponse;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.GeminiEndpoint;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.GeminiResponseListener;
import com.google.android.accessibility.talkback.actor.gemini.GeminiCommand.CommonRequest;
import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback;
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModel;
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager;
import com.google.android.accessibility.talkback.actor.gemini.local.OnDeviceAiSettings;
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse;
import com.google.android.accessibility.utils.NetworkUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Util class to communicate with Gemini APIs */
public class GeminiRestEndpoint implements GeminiEndpoint {

  private static final String TAG = "GeminiEndpoint";
  private static final String GEMINI_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/";
  private static final String GEMINI_URL_NO_PARAM = ":generateContent";

  private final Context context;
  private final SharedPreferences prefs;
  private final String builtInApiKey;
  private final GeminiRestRequestPerformer requestPerformer;
  private final String safetyThresholdHarassment;
  private final String safetyThresholdHateSpeech;
  private final String safetyThresholdSexuallyExplicit;
  private final String safetyThresholdDangerousContent;
  private final String prefixPrompt;
  private final ScreenOverviewRequester screenOverviewRequester;

  /**
   * @param builtInApiKey the key built into the app, used only while the user has not entered one
   *     in settings
   */
  public GeminiRestEndpoint(
      Context context, String builtInApiKey, GeminiRestRequestPerformer requestPerformer) {
    this.context = context;
    this.builtInApiKey = builtInApiKey;
    this.requestPerformer = requestPerformer;
    safetyThresholdHarassment = GeminiConfiguration.getSafetyThresholdHarassment(context);
    safetyThresholdHateSpeech = GeminiConfiguration.getSafetyThresholdHateSpeech(context);
    safetyThresholdSexuallyExplicit =
        GeminiConfiguration.getSafetyThresholdSexuallyExplicit(context);
    safetyThresholdDangerousContent =
        GeminiConfiguration.getSafetyThresholdDangerousContent(context);
    prefixPrompt = GeminiConfiguration.getPrefixPrompt(context);
    screenOverviewRequester =
        new ScreenOverviewRequester(
            () -> {
              SharedPreferences settings = SharedPreferencesUtils.getSharedPreferences(context);
              LocalModelManager localModels = LocalModelManager.Companion.get(context);
              if (!localModels.answersRequests()) {
                return PromptStyle.CLOUD;
              }
              LocalModel model = localModels.activeModel();
              if (model != null && !model.getWritesJson()) {
                return PromptStyle.ON_DEVICE_PLAIN;
              }
              return OnDeviceAiSettings.INSTANCE.shortPrompts(settings)
                  ? PromptStyle.ON_DEVICE_SHORT
                  : PromptStyle.ON_DEVICE;
            },
            (postData, callback) -> {
              requestPerformer.performRequest(urls(), apiKey(), postData, callback);
              return kotlin.Unit.INSTANCE;
            },
            failure -> failure.userMessage(context),
            packageName -> appName(context, packageName));
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
  }

  /** The name of the app with {@code packageName}, or null when it isn't installed. */
  private static @Nullable String appName(Context context, CharSequence packageName) {
    PackageManager packageManager = context.getPackageManager();
    try {
      return packageManager
          .getApplicationLabel(packageManager.getApplicationInfo(packageName.toString(), 0))
          .toString();
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }

  /**
   * Returns the API key, or an empty string when there is none. The key is read each time, so that
   * a key entered in settings works at once.
   */
  private String apiKey() {
    // Gemini is asked only while it is the service in use; with none, nothing goes to the cloud.
    if (AiProvider.active(prefs) == AiProvider.NONE) {
      return "";
    }
    return GeminiApiKey.effectiveKey(prefs, builtInApiKey);
  }

  /** The address of each model to ask, in order, read each time like the key. */
  private List<String> urls() {
    List<String> urls = new ArrayList<>();
    for (String model : GeminiModel.chain(prefs)) {
      urls.add(GEMINI_URL + model + GEMINI_URL_NO_PARAM);
    }
    return urls;
  }

  private boolean isSupported() {
    return !TextUtils.isEmpty(apiKey()) || requestPerformer.isKeylessInitialized();
  }

  @Override
  public boolean createRequestGeminiCommand(
      String command,
      Bitmap screenshotCapture,
      boolean manualTrigger,
      GeminiResponseListener geminiResponseListener) {
    return createRequestGeminiCommand(
        new CommonRequest(
            UNKNOWN,
            command,
            DataFieldUtils.encodeImageToByteArray(screenshotCapture),
            manualTrigger,
            geminiResponseListener));
  }

  @Override
  public boolean createRequestGeminiCommand(GeminiCommand command) {
    LogUtils.v(TAG, "createRequestGeminiCommand - %s", GeminiModel.chain(prefs));
    String text;
    byte[] imageByteArray;
    GeminiResponseListener geminiResponseListener;
    if (command instanceof GeminiCommand.CommonRequest commonRequest) {
      text = commonRequest.getText();
      imageByteArray = commonRequest.getScreenshot();
      geminiResponseListener = commonRequest.getListener();
    } else if (command instanceof GeminiCommand.ScreenOverview
        || command instanceof GeminiCommand.ScreenQuery) {
      return requestScreenOverviewOrQuery(command);
    } else {
      LogUtils.v(TAG, "Not a common request - Return.");
      return false;
    }

    if (!checkGeminiAvailability(text, imageByteArray, geminiResponseListener)) {
      return false;
    }

    try {
      // Set safety settings
      JSONArray safetySettings =
          DataFieldUtils.createSafetySettingsJson(
              safetyThresholdHarassment,
              safetyThresholdHateSpeech,
              safetyThresholdSexuallyExplicit,
              safetyThresholdDangerousContent);

      // Encode screenshot in base64
      String encodedImage = Base64.encodeToString(imageByteArray, Base64.NO_WRAP);
      if (TextUtils.isEmpty(encodedImage)) {
        LogUtils.e(TAG, "Bitmap compression failed!");
        geminiResponseListener.onError(ErrorReason.BITMAP_COMPRESSION_FAIL);
        return false;
      }

      JSONObject postData =
          DataFieldUtils.createPostDataJson(prefixPrompt + text, encodedImage, safetySettings);

      requestPerformer.performRequest(
          urls(),
          apiKey(),
          postData,
          new GeminiRestResponseCallback() {
            @Override
            public void onResponse(GeminiResponse response) {
              if (response.finishReason() != null
                  && response.finishReason().equals(FINISH_REASON_STOP)) { // Positive acknowledge
                geminiResponseListener.onResponse(FinishReason.STOP, response.text());
                LogUtils.v(TAG, "Gemini succeeds");
              } else { // Redefine the hint of these kinds when the use cases are understood.
                geminiResponseListener.onResponse(FinishReason.ERROR_BLOCKED, /* response= */ null);
                LogUtils.e(
                    TAG,
                    "Gemini gave no answer, finish or block reason: %s",
                    (response.blockReason() != null)
                        ? response.blockReason()
                        : response.finishReason());
              }
            }

            @Override
            public void onFailure(GeminiFailure failure) {
              // The request performer has logged the failure. The listener speaks the text.
              geminiResponseListener.onResponse(
                  FinishReason.ERROR_RESPONSE, failure.userMessage(context));
            }

            @Override
            public void onCancelled() {
              LogUtils.v(TAG, "Cancelled a pending task.");
              geminiResponseListener.onError(ErrorReason.JOB_CANCELLED);
            }
          });
    } catch (JSONException e) {
      LogUtils.e(TAG, "Error processing Gemini request: " + e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public boolean requestText(String prompt, GeminiResponseListener listener) {
    if (!isSupported()) {
      listener.onError(ErrorReason.UNSUPPORTED);
      return false;
    }
    if (!LocalModelManager.Companion.get(context).answersRequests()
        && !NetworkUtils.isNetworkConnected(context)) {
      listener.onError(ErrorReason.NETWORK_ERROR);
      return false;
    }
    JSONObject postData;
    try {
      postData =
          DataFieldUtils.createTextPostDataJson(
              prompt,
              DataFieldUtils.createSafetySettingsJson(
                  safetyThresholdHarassment,
                  safetyThresholdHateSpeech,
                  safetyThresholdSexuallyExplicit,
                  safetyThresholdDangerousContent));
    } catch (JSONException e) {
      LogUtils.e(TAG, "Error making a text request: " + e.getMessage());
      listener.onError(ErrorReason.UNSUPPORTED);
      return false;
    }
    requestPerformer.performRequest(
        urls(),
        apiKey(),
        postData,
        new GeminiRestResponseCallback() {
          @Override
          public void onResponse(GeminiResponse response) {
            if (FINISH_REASON_STOP.equals(response.finishReason())
                && !TextUtils.isEmpty(response.text())) {
              listener.onResponse(FinishReason.STOP, response.text());
            } else {
              LogUtils.e(
                  TAG,
                  "No answer to a text request, finish or block reason: %s",
                  response.blockReason() != null
                      ? response.blockReason()
                      : response.finishReason());
              listener.onResponse(FinishReason.ERROR_BLOCKED, /* response= */ null);
            }
          }

          @Override
          public void onFailure(GeminiFailure failure) {
            listener.onResponse(FinishReason.ERROR_RESPONSE, failure.userMessage(context));
          }

          @Override
          public void onCancelled() {
            listener.onError(ErrorReason.JOB_CANCELLED);
          }
        });
    return true;
  }

  private boolean requestScreenOverviewOrQuery(GeminiCommand command) {
    GeminiResponseCallback<OverviewResponse> listener =
        command instanceof GeminiCommand.ScreenOverview overview
            ? overview.getListener()
            : ((GeminiCommand.ScreenQuery) command).getListener();
    if (!isSupported()) {
      listener.onError(ErrorReason.UNSUPPORTED);
      return false;
    }
    if (!NetworkUtils.isNetworkConnected(context)) {
      listener.onError(ErrorReason.NETWORK_ERROR);
      return false;
    }
    try {
      JSONArray safetySettings =
          DataFieldUtils.createSafetySettingsJson(
              safetyThresholdHarassment,
              safetyThresholdHateSpeech,
              safetyThresholdSexuallyExplicit,
              safetyThresholdDangerousContent);
      if (command instanceof GeminiCommand.ScreenOverview overview) {
        screenOverviewRequester.requestOverview(overview, safetySettings);
      } else {
        screenOverviewRequester.requestQuery((GeminiCommand.ScreenQuery) command, safetySettings);
      }
    } catch (JSONException e) {
      LogUtils.e(TAG, "Error processing screen overview request: " + e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public void cancelCommand() {
    requestPerformer.cancelExistingRequestIfNeeded();
  }

  @Override
  public boolean hasPendingTransaction() {
    return requestPerformer.hasPendingTransaction();
  }

  private boolean checkGeminiAvailability(
      String command, byte[] imageByteArray, GeminiResponseListener geminiResponseListener) {
    if (!isSupported()) {
      LogUtils.d(TAG, "Gemini API is not supported");
      geminiResponseListener.onError(ErrorReason.UNSUPPORTED);
      return false;
    }

    if (!NetworkUtils.isNetworkConnected(context)) {
      LogUtils.d(TAG, "Internet is not connected");
      geminiResponseListener.onError(ErrorReason.NETWORK_ERROR);
      return false;
    }

    if (imageByteArray == null || imageByteArray.length == 0) {
      LogUtils.d(TAG, "screenshot is not provided.");
      geminiResponseListener.onError(ErrorReason.NO_IMAGE);
      return false;
    }
    if (TextUtils.isEmpty(command)) {
      LogUtils.d(TAG, "command part is empty.");
      geminiResponseListener.onError(ErrorReason.EMPTY_PROMPT);
      return false;
    }
    return true;
  }
}
