/*
 * A memory budget for a Lua state: an allocator that wraps the state's own one and refuses to
 * grow past the budget, so that Lua raises "not enough memory" inside the script.
 *
 * Lua's functions come from LuaJava's liblua54.so, which is loaded before a state exists.
 *
 * The budget is only enforced while Lua code runs (see set_enforced). A refused allocation raises
 * a Lua error, which unwinds with longjmp; while Java pushes values on the stack, or inside a Java
 * function that Lua called, that would jump over Java frames.
 */
#include <dlfcn.h>
#include <jni.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>

/* LuaJava's library; another name for tests on a computer. */
#ifndef LUA_LIBRARY
#define LUA_LIBRARY "liblua54.so"
#endif

#define LUA_GCCOUNT 3
#define LUA_GCCOUNTB 4

typedef void *(*lua_Alloc)(void *ud, void *ptr, size_t osize, size_t nsize);
typedef lua_Alloc (*getallocf_fn)(void *L, void **ud);
typedef void (*setallocf_fn)(void *L, lua_Alloc f, void *ud);
typedef int (*gc_fn)(void *L, int what, ...);

typedef struct {
  lua_Alloc alloc;
  void *ud;
  size_t used;
  size_t limit;
  int enforced;
} Budget;

static void *budgeted_alloc(void *ud, void *ptr, size_t osize, size_t nsize) {
  Budget *budget = (Budget *)ud;
  /* Without a block, osize is the kind of object, not a size. */
  size_t old = ptr != NULL ? osize : 0;
  if (budget->enforced && nsize > old &&
      (budget->used > budget->limit || nsize - old > budget->limit - budget->used)) {
    /* Lua collects garbage and tries once more before it raises the error. */
    return NULL;
  }
  void *block = budget->alloc(budget->ud, ptr, osize, nsize);
  if (block != NULL || nsize == 0) {
    budget->used = (budget->used > old ? budget->used - old : 0) + (block != NULL ? nsize : 0);
  }
  return block;
}

static void *lua_function(const char *name) {
  void *library = dlopen(LUA_LIBRARY, RTLD_NOW | RTLD_NOLOAD);
  if (library == NULL) return NULL;
  void *function = dlsym(library, name);
  dlclose(library);
  return function;
}

/* Puts a budget of limit bytes on the state L. Returns its handle, or 0 if it couldn't. */
JNIEXPORT jlong JNICALL Java_com_google_android_accessibility_luamemory_LuaMemory_nativeAttach(
    JNIEnv *env, jclass clazz, jlong state, jlong limit) {
  getallocf_fn getallocf = (getallocf_fn)lua_function("lua_getallocf");
  setallocf_fn setallocf = (setallocf_fn)lua_function("lua_setallocf");
  gc_fn gc = (gc_fn)lua_function("lua_gc");
  if (getallocf == NULL || setallocf == NULL || gc == NULL || state == 0 || limit <= 0) return 0;
  Budget *budget = (Budget *)calloc(1, sizeof(Budget));
  if (budget == NULL) return 0;
  void *L = (void *)(intptr_t)state;
  budget->alloc = getallocf(L, &budget->ud);
  budget->used = (size_t)gc(L, LUA_GCCOUNT) * 1024 + (size_t)gc(L, LUA_GCCOUNTB);
  budget->limit = (size_t)limit;
  setallocf(L, budgeted_alloc, budget);
  return (jlong)(intptr_t)budget;
}

/* Turns enforcing on or off and returns whether it was on. */
JNIEXPORT jboolean JNICALL Java_com_google_android_accessibility_luamemory_LuaMemory_nativeSetEnforced(
    JNIEnv *env, jclass clazz, jlong handle, jboolean enforced) {
  Budget *budget = (Budget *)(intptr_t)handle;
  if (budget == NULL) return JNI_FALSE;
  jboolean was = budget->enforced ? JNI_TRUE : JNI_FALSE;
  budget->enforced = enforced == JNI_TRUE;
  return was;
}

/* Frees the budget, after the state was closed, which frees its memory through it. */
JNIEXPORT void JNICALL Java_com_google_android_accessibility_luamemory_LuaMemory_nativeFree(
    JNIEnv *env, jclass clazz, jlong handle) {
  free((Budget *)(intptr_t)handle);
}
