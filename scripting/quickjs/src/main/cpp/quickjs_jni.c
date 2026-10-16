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

#include <jni.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>

#include "quickjs.h"

typedef struct {
  JSValue promise;
  JSValue reason;
} Rejection;

typedef struct {
  JSRuntime *rt;
  JSContext *ctx;
  JNIEnv *env;
  jobject host;
  int64_t deadline_ns;
  bool interrupted;
  Rejection *rejections;
  int rejection_count;
  int rejection_capacity;
} Engine;

static jclass exception_class;
static jmethodID exception_init;
static jmethodID host_call;
static jmethodID host_load_module;
static jmethodID host_unhandled_error;
static jmethodID throwable_get_message;

static int64_t now_ns(void) {
  struct timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  return (int64_t)ts.tv_sec * 1000000000LL + ts.tv_nsec;
}

static void drop(JNIEnv *env, jobject ref) {
  if (ref) {
    (*env)->DeleteLocalRef(env, ref);
  }
}

static jstring to_jstring(JNIEnv *env, JSContext *ctx, JSValueConst value) {
  size_t len;
  const uint16_t *chars = JS_ToCStringLenUTF16(ctx, &len, value);
  if (!chars) {
    JS_FreeValue(ctx, JS_GetException(ctx));
    return NULL;
  }
  jstring result = (*env)->NewString(env, (const jchar *)chars, (jsize)len);
  JS_FreeCStringUTF16(ctx, chars);
  return result;
}

static JSValue to_jsvalue(JNIEnv *env, JSContext *ctx, jstring string) {
  if (!string) {
    return JS_NULL;
  }
  jsize len = (*env)->GetStringLength(env, string);
  const jchar *chars = (*env)->GetStringChars(env, string, NULL);
  JSValue value = JS_NewStringUTF16(ctx, (const uint16_t *)chars, (size_t)len);
  (*env)->ReleaseStringChars(env, string, chars);
  return value;
}

static jstring stringify(JNIEnv *env, JSContext *ctx, JSValueConst value) {
  if (JS_IsUndefined(value) || JS_IsNull(value)) {
    return NULL;
  }
  if (JS_IsString(value)) {
    return to_jstring(env, ctx, value);
  }
  JSValue json = JS_JSONStringify(ctx, value, JS_UNDEFINED, JS_UNDEFINED);
  jstring result = JS_IsString(json) ? to_jstring(env, ctx, json) : NULL;
  if (JS_IsException(json)) {
    JS_FreeValue(ctx, JS_GetException(ctx));
  }
  JS_FreeValue(ctx, json);
  return result;
}

static char *to_source(JNIEnv *env, jbyteArray bytes, size_t *len) {
  jsize size = (*env)->GetArrayLength(env, bytes);
  char *buf = malloc((size_t)size + 1);
  if (buf) {
    (*env)->GetByteArrayRegion(env, bytes, 0, size, (jbyte *)buf);
    buf[size] = '\0';
    *len = (size_t)size;
  }
  return buf;
}

static jthrowable new_exception(JNIEnv *env, jstring message, jstring stack, bool interrupted) {
  return (*env)->NewObject(env, exception_class, exception_init, message, stack,
                           (jboolean)interrupted);
}

static jthrowable message_exception(JNIEnv *env, const char *message, bool interrupted) {
  return new_exception(env, (*env)->NewStringUTF(env, message), NULL, interrupted);
}

static jthrowable js_exception(JNIEnv *env, JSContext *ctx, JSValueConst value) {
  jstring stack = NULL;
  if (JS_IsObject(value)) {
    JSValue stack_value = JS_GetPropertyStr(ctx, value, "stack");
    if (JS_IsString(stack_value)) {
      stack = to_jstring(env, ctx, stack_value);
    }
    JS_FreeValue(ctx, stack_value);
  }
  return new_exception(env, to_jstring(env, ctx, value), stack, false);
}

static void throw_pending(Engine *e) {
  JSValue exception = JS_GetException(e->ctx);
  jthrowable error =
      e->interrupted
          ? message_exception(e->env, "The script took too long and was stopped", true)
          : js_exception(e->env, e->ctx, exception);
  JS_FreeValue(e->ctx, exception);
  if (error) {
    (*e->env)->Throw(e->env, error);
  }
}

static void report_unhandled(Engine *e, JSValueConst reason) {
  JNIEnv *env = e->env;
  jthrowable error = js_exception(env, e->ctx, reason);
  if (error) {
    (*env)->CallVoidMethod(env, e->host, host_unhandled_error, error);
  }
  drop(env, error);
  (*env)->ExceptionClear(env);
}

static void free_rejection(Engine *e, int i) {
  JS_FreeValue(e->ctx, e->rejections[i].promise);
  JS_FreeValue(e->ctx, e->rejections[i].reason);
}

static void flush_rejections(Engine *e) {
  for (int i = 0; i < e->rejection_count; i++) {
    report_unhandled(e, e->rejections[i].reason);
    free_rejection(e, i);
  }
  e->rejection_count = 0;
}

static void forget_rejection(Engine *e, JSValueConst promise) {
  for (int i = 0; i < e->rejection_count; i++) {
    if (JS_VALUE_GET_PTR(e->rejections[i].promise) == JS_VALUE_GET_PTR(promise)) {
      free_rejection(e, i);
      e->rejections[i] = e->rejections[--e->rejection_count];
      return;
    }
  }
}

static void rejection_tracker(JSContext *ctx, JSValueConst promise, JSValueConst reason,
                              bool is_handled, void *opaque) {
  Engine *e = opaque;
  if (is_handled) {
    forget_rejection(e, promise);
    return;
  }
  if (e->rejection_count == e->rejection_capacity) {
    int capacity = e->rejection_capacity ? e->rejection_capacity * 2 : 4;
    Rejection *grown = realloc(e->rejections, sizeof(Rejection) * (size_t)capacity);
    if (!grown) {
      return;
    }
    e->rejections = grown;
    e->rejection_capacity = capacity;
  }
  e->rejections[e->rejection_count++] =
      (Rejection){JS_DupValue(ctx, promise), JS_DupValue(ctx, reason)};
}

static int interrupt_handler(JSRuntime *rt, void *opaque) {
  Engine *e = opaque;
  e->interrupted = e->deadline_ns != 0 && now_ns() > e->deadline_ns;
  return e->interrupted;
}

static JSValue compile(JSContext *ctx, JNIEnv *env, jbyteArray bytes, const char *name,
                       int flags) {
  size_t len;
  char *source = to_source(env, bytes, &len);
  if (!source) {
    return JS_ThrowOutOfMemory(ctx);
  }
  JSValue compiled = JS_Eval(ctx, source, len, name, flags);
  free(source);
  return compiled;
}

static JSModuleDef *module_loader(JSContext *ctx, const char *module_name, void *opaque) {
  Engine *e = opaque;
  JNIEnv *env = e->env;
  jstring name = (*env)->NewStringUTF(env, module_name);
  jbyteArray bytes = (jbyteArray)(*env)->CallObjectMethod(env, e->host, host_load_module, name);
  drop(env, name);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
    bytes = NULL;
  }
  if (!bytes) {
    JS_ThrowReferenceError(ctx, "could not load module '%s'", module_name);
    return NULL;
  }
  JSValue compiled =
      compile(ctx, env, bytes, module_name, JS_EVAL_TYPE_MODULE | JS_EVAL_FLAG_COMPILE_ONLY);
  drop(env, bytes);
  if (JS_IsException(compiled)) {
    return NULL;
  }
  JSModuleDef *module = JS_VALUE_GET_PTR(compiled);
  JS_FreeValue(ctx, compiled);
  return module;
}

static JSValue host_error(JNIEnv *env, JSContext *ctx) {
  jthrowable error = (*env)->ExceptionOccurred(env);
  (*env)->ExceptionClear(env);
  jstring message = (jstring)(*env)->CallObjectMethod(env, error, throwable_get_message);
  (*env)->ExceptionClear(env);
  drop(env, error);
  JSValue thrown = JS_NewError(ctx);
  JS_SetPropertyStr(ctx, thrown, "message",
                    message ? to_jsvalue(env, ctx, message) : JS_NewString(ctx, "host call failed"));
  drop(env, message);
  return JS_Throw(ctx, thrown);
}

static JSValue js_host(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
  Engine *e = JS_GetContextOpaque(ctx);
  JNIEnv *env = e->env;
  if (argc < 1) {
    return JS_ThrowTypeError(ctx, "__host needs a method");
  }
  jstring method = to_jstring(env, ctx, argv[0]);
  jstring json = (argc > 1 && !JS_IsUndefined(argv[1])) ? to_jstring(env, ctx, argv[1]) : NULL;
  jstring result = (jstring)(*env)->CallObjectMethod(env, e->host, host_call, method, json);
  drop(env, method);
  drop(env, json);
  if ((*env)->ExceptionCheck(env)) {
    return host_error(env, ctx);
  }
  if (!result) {
    return JS_UNDEFINED;
  }
  JSValue value = to_jsvalue(env, ctx, result);
  drop(env, result);
  return value;
}

static Engine *enter(jlong ptr, JNIEnv *env, jlong time_limit_ms) {
  Engine *e = (Engine *)(intptr_t)ptr;
  e->env = env;
  JS_UpdateStackTop(e->rt);
  e->interrupted = false;
  e->deadline_ns = time_limit_ms > 0 ? now_ns() + (int64_t)time_limit_ms * 1000000LL : 0;
  return e;
}

static jstring done(Engine *e, jstring result) {
  e->deadline_ns = 0;
  return result;
}

static jstring fail(Engine *e) {
  throw_pending(e);
  return done(e, NULL);
}

static bool run_jobs(Engine *e) {
  JSContext *job_ctx;
  int result;
  while ((result = JS_ExecutePendingJob(e->rt, &job_ctx)) != 0) {
    if (result < 0) {
      if (e->interrupted) {
        return false;
      }
      JSValue exception = JS_GetException(job_ctx);
      report_unhandled(e, exception);
      JS_FreeValue(job_ctx, exception);
    }
  }
  flush_rejections(e);
  return true;
}

static jmethodID method(JNIEnv *env, jclass clazz, const char *name, const char *signature) {
  return clazz ? (*env)->GetMethodID(env, clazz, name, signature) : NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv *env;
  if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }
  jclass exception = (*env)->FindClass(
      env, "com/google/android/accessibility/scripting/quickjs/QuickJsException");
  jclass host =
      (*env)->FindClass(env, "com/google/android/accessibility/scripting/quickjs/QuickJs$Host");
  jclass throwable = (*env)->FindClass(env, "java/lang/Throwable");
  exception_class = exception ? (*env)->NewGlobalRef(env, exception) : NULL;
  exception_init =
      method(env, exception, "<init>", "(Ljava/lang/String;Ljava/lang/String;Z)V");
  host_call = method(env, host, "call",
                     "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
  host_load_module = method(env, host, "loadModule", "(Ljava/lang/String;)[B");
  host_unhandled_error =
      method(env, host, "onUnhandledError",
             "(Lcom/google/android/accessibility/scripting/quickjs/QuickJsException;)V");
  throwable_get_message = method(env, throwable, "getMessage", "()Ljava/lang/String;");
  bool ready = exception_init && host_call && host_load_module && host_unhandled_error &&
               throwable_get_message;
  return ready ? JNI_VERSION_1_6 : JNI_ERR;
}

JNIEXPORT jlong JNICALL
Java_com_google_android_accessibility_scripting_quickjs_QuickJs_nativeCreate(
    JNIEnv *env, jclass clazz, jobject host, jlong memory_limit, jlong stack_size) {
  Engine *e = calloc(1, sizeof(Engine));
  if (!e) {
    return 0;
  }
  e->rt = JS_NewRuntime();
  e->ctx = e->rt ? JS_NewContext(e->rt) : NULL;
  if (!e->ctx) {
    if (e->rt) {
      JS_FreeRuntime(e->rt);
    }
    free(e);
    return 0;
  }
  e->env = env;
  e->host = (*env)->NewGlobalRef(env, host);
  JS_SetMemoryLimit(e->rt, (size_t)memory_limit);
  JS_SetMaxStackSize(e->rt, (size_t)stack_size);
  JS_SetInterruptHandler(e->rt, interrupt_handler, e);
  JS_SetHostPromiseRejectionTracker(e->rt, rejection_tracker, e);
  JS_SetModuleLoaderFunc(e->rt, NULL, module_loader, e);
  JS_SetContextOpaque(e->ctx, e);
  JSValue global = JS_GetGlobalObject(e->ctx);
  JS_SetPropertyStr(e->ctx, global, "__host", JS_NewCFunction(e->ctx, js_host, "__host", 2));
  JS_FreeValue(e->ctx, global);
  return (jlong)(intptr_t)e;
}

JNIEXPORT void JNICALL
Java_com_google_android_accessibility_scripting_quickjs_QuickJs_nativeDestroy(
    JNIEnv *env, jclass clazz, jlong ptr) {
  Engine *e = (Engine *)(intptr_t)ptr;
  if (!e) {
    return;
  }
  e->env = env;
  for (int i = 0; i < e->rejection_count; i++) {
    free_rejection(e, i);
  }
  free(e->rejections);
  JS_FreeContext(e->ctx);
  JS_FreeRuntime(e->rt);
  (*env)->DeleteGlobalRef(env, e->host);
  free(e);
}

JNIEXPORT jstring JNICALL
Java_com_google_android_accessibility_scripting_quickjs_QuickJs_nativeEval(
    JNIEnv *env, jclass clazz, jlong ptr, jbyteArray source, jstring file_name,
    jboolean is_module, jlong time_limit_ms) {
  Engine *e = enter(ptr, env, time_limit_ms);
  JSContext *ctx = e->ctx;
  const char *name = (*env)->GetStringUTFChars(env, file_name, NULL);
  JSValue compiled = compile(ctx, env, source, name,
                             is_module ? JS_EVAL_TYPE_MODULE | JS_EVAL_FLAG_COMPILE_ONLY
                                       : JS_EVAL_TYPE_GLOBAL);
  (*env)->ReleaseStringUTFChars(env, file_name, name);
  if (JS_IsException(compiled)) {
    return fail(e);
  }
  if (!is_module) {
    JS_FreeValue(ctx, compiled);
    return run_jobs(e) ? done(e, NULL) : fail(e);
  }
  JSModuleDef *module = JS_VALUE_GET_PTR(compiled);
  JSValue evaluation = JS_EvalFunction(ctx, compiled);
  if (JS_IsException(evaluation)) {
    return fail(e);
  }
  forget_rejection(e, evaluation);
  bool ran = run_jobs(e);
  if (ran && JS_PromiseState(ctx, evaluation) == JS_PROMISE_REJECTED) {
    JS_Throw(ctx, JS_PromiseResult(ctx, evaluation));
    ran = false;
  }
  JS_FreeValue(ctx, evaluation);
  if (!ran) {
    return fail(e);
  }
  JSValue namespace = JS_GetModuleNamespace(ctx, module);
  if (JS_IsException(namespace)) {
    return fail(e);
  }
  JSValue manifest = JS_GetPropertyStr(ctx, namespace, "manifest");
  JS_FreeValue(ctx, namespace);
  if (JS_IsException(manifest)) {
    return fail(e);
  }
  jstring json = stringify(env, ctx, manifest);
  JS_FreeValue(ctx, manifest);
  return done(e, json);
}

JNIEXPORT jstring JNICALL
Java_com_google_android_accessibility_scripting_quickjs_QuickJs_nativeCall(
    JNIEnv *env, jclass clazz, jlong ptr, jstring function, jstring argument,
    jlong time_limit_ms) {
  Engine *e = enter(ptr, env, time_limit_ms);
  JSContext *ctx = e->ctx;
  JSValue global = JS_GetGlobalObject(ctx);
  const char *name = (*env)->GetStringUTFChars(env, function, NULL);
  JSValue fn = JS_GetPropertyStr(ctx, global, name);
  (*env)->ReleaseStringUTFChars(env, function, name);
  JSValue arg = to_jsvalue(env, ctx, argument);
  JSValue result = JS_IsFunction(ctx, fn)
                       ? JS_Call(ctx, fn, global, 1, (JSValueConst *)&arg)
                       : JS_ThrowTypeError(ctx, "No such function");
  JS_FreeValue(ctx, arg);
  JS_FreeValue(ctx, fn);
  JS_FreeValue(ctx, global);
  if (JS_IsException(result)) {
    return fail(e);
  }
  jstring output = stringify(env, ctx, result);
  JS_FreeValue(ctx, result);
  if (!run_jobs(e)) {
    drop(env, output);
    return fail(e);
  }
  return done(e, output);
}
