// Copyright 2026 SandFly contributors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License. You may obtain a copy of
// the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations under
// the License.

// JNI for WhisperModel: loads a whisper.cpp model and transcribes 16 kHz mono audio with it. Built
// twice: for the CPU, as WhisperNative, and with the OpenCL GPU backend, as WhisperOpenClNative.

#include <android/log.h>
#include <jni.h>

#include <atomic>
#include <string>

#include "whisper.h"

#ifndef WHISPER_JNI_CLASS
#define WHISPER_JNI_CLASS WhisperNative
#endif
#define JNI_NAME3(cls, fn) Java_com_google_android_accessibility_whisper_##cls##_##fn
#define JNI_NAME2(cls, fn) JNI_NAME3(cls, fn)
#define JNI_NAME(fn) JNI_NAME2(WHISPER_JNI_CLASS, fn)

namespace {

constexpr const char* kTag = "SandFlyWhisper";

struct Model {
  whisper_context* context;
  std::atomic<bool> cancelled{false};
};

void Log(ggml_log_level level, const char* text, void*) {
  if (level == GGML_LOG_LEVEL_ERROR) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "%s", text);
  } else if (level == GGML_LOG_LEVEL_WARN) {
    __android_log_print(ANDROID_LOG_WARN, kTag, "%s", text);
  }
}

bool IsCancelled(void* data) {
  return static_cast<Model*>(data)->cancelled.load();
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
JNI_NAME(nativeLoad)(
    JNIEnv* env, jobject, jstring path, jboolean gpu) {
  whisper_log_set(Log, nullptr);
  const char* file = env->GetStringUTFChars(path, nullptr);
  whisper_context_params params = whisper_context_default_params();
  params.use_gpu = gpu;
  whisper_context* context = whisper_init_from_file_with_params(file, params);
  env->ReleaseStringUTFChars(path, file);
  if (context == nullptr) return 0;
  return reinterpret_cast<jlong>(new Model{context});
}

JNIEXPORT jint JNICALL
JNI_NAME(nativeLanguageId)(
    JNIEnv* env, jobject, jstring code) {
  const char* lang = env->GetStringUTFChars(code, nullptr);
  int id = whisper_lang_id(lang);
  env->ReleaseStringUTFChars(code, lang);
  return id;
}

JNIEXPORT void JNICALL
JNI_NAME(nativeFree)(
    JNIEnv*, jobject, jlong handle) {
  auto* model = reinterpret_cast<Model*>(handle);
  if (model == nullptr) return;
  whisper_free(model->context);
  delete model;
}

JNIEXPORT void JNICALL
JNI_NAME(nativeCancel)(
    JNIEnv*, jobject, jlong handle) {
  auto* model = reinterpret_cast<Model*>(handle);
  if (model != nullptr) model->cancelled.store(true);
}

// Returns the text as UTF-8, since JNI's own strings can't hold every character, or null when
// transcription failed or was cancelled.
JNIEXPORT jbyteArray JNICALL
JNI_NAME(nativeTranscribe)(
    JNIEnv* env, jobject, jlong handle, jfloatArray samples, jstring language, jint threads) {
  auto* model = reinterpret_cast<Model*>(handle);
  if (model == nullptr) return nullptr;
  model->cancelled.store(false);

  const char* lang = env->GetStringUTFChars(language, nullptr);
  whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  params.n_threads = threads;
  params.language = lang;
  params.translate = false;
  params.no_timestamps = true;
  params.print_special = false;
  params.print_progress = false;
  params.print_realtime = false;
  params.print_timestamps = false;
  params.suppress_nst = true;
  params.abort_callback = IsCancelled;
  params.abort_callback_user_data = model;

  jsize count = env->GetArrayLength(samples);
  jfloat* audio = env->GetFloatArrayElements(samples, nullptr);
  int result = whisper_full(model->context, params, audio, count);
  env->ReleaseFloatArrayElements(samples, audio, JNI_ABORT);
  env->ReleaseStringUTFChars(language, lang);
  if (result != 0 || model->cancelled.load()) return nullptr;

  std::string text;
  int segments = whisper_full_n_segments(model->context);
  for (int i = 0; i < segments; i++) {
    text += whisper_full_get_segment_text(model->context, i);
  }
  jbyteArray bytes = env->NewByteArray(static_cast<jsize>(text.size()));
  if (bytes == nullptr) return nullptr;
  env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(text.size()),
                          reinterpret_cast<const jbyte*>(text.data()));
  return bytes;
}

}  // extern "C"
