/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils;

import android.text.TextUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/** This class manages efficient loading of classes. */
public class ClassLoadingCache {

  private static final String TAG = "ClassLoadingCache";

  // TODO: Use a LRU map instead?
  private static final HashMap<String, @Nullable Class<?>> mCachedClasses = new HashMap<>();

  /**
   * Returns a class by given <code>className</code>. It tries to load from the current class loader
   * and caches them.
   *
   * @param className The name of the class to load.
   * @return The class if loaded successfully, null otherwise.
   */
  public static @Nullable Class<?> loadOrGetCachedClass(String className) {
    if (TextUtils.isEmpty(className)) {
      LogUtils.d(TAG, "Missing class name. Failed to load class.");
      return null;
    }

    if (mCachedClasses.containsKey(className)) {
      return mCachedClasses.get(className);
    }

    Class<?> insideClazz = null;
    if (!canBeLoaded(className)) {
      mCachedClasses.put(className, null);
      return null;
    }
    try {
      ClassLoader classLoader = ClassLoadingCache.class.getClassLoader();
      if (classLoader != null) {
        insideClazz = classLoader.loadClass(className);
      }
      if (insideClazz == null) {
        LogUtils.d(TAG, "Failed to load class: %s", className);
      }
    } catch (ClassNotFoundException e) {
      LogUtils.d(TAG, "Failed to load class: %s", className);
    }

    mCachedClasses.put(className, insideClazz);
    return insideClazz;
  }

  /**
   * Returns whether the class loader can find a class, going by its package. A class that belongs
   * to another app is never found, but looking it up throws an exception with a message that lists
   * every dex file and library path, which takes long enough to stall the main thread when a screen
   * has many custom views.
   */
  private static boolean canBeLoaded(String className) {
    for (String prefix : LOADABLE_PACKAGE_PREFIXES) {
      if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private static final String[] LOADABLE_PACKAGE_PREFIXES = {
    "android.", "androidx.", "java.", "javax.", "kotlin.", "kotlinx.", "dalvik.", "libcore.",
    "com.android.", "com.google.", "org.",
  };

  /** Returns whether a target class is an instance of a reference class. */
  public static boolean checkInstanceOf(
      CharSequence targetClassName, CharSequence referenceClassName) {
    if ((targetClassName == null) || (referenceClassName == null)) return false;
    if (TextUtils.equals(targetClassName, referenceClassName)) return true;

    String reference = referenceClassName.toString();
    synchronized (ClassLoadingCache.class) {
      HashMap<Object, Boolean> results = instanceOfResultsFor(targetClassName.toString());
      @Nullable Boolean result = results.get(reference);
      if (result == null) {
        final Class<?> referenceClass = loadOrGetCachedClass(reference);
        final Class<?> targetClass = loadOrGetCachedClass(targetClassName.toString());
        result =
            referenceClass != null
                && targetClass != null
                && referenceClass.isAssignableFrom(targetClass);
        results.put(reference, result);
      }
      return result;
    }
  }

  /** Returns whether a target class is an instance of a reference class. */
  public static boolean checkInstanceOf(CharSequence targetClassName, Class<?> referenceClass) {
    if ((targetClassName == null) || (referenceClass == null)) return false;
    if (TextUtils.equals(targetClassName, referenceClass.getName())) return true;

    synchronized (ClassLoadingCache.class) {
      HashMap<Object, Boolean> results = instanceOfResultsFor(targetClassName.toString());
      @Nullable Boolean result = results.get(referenceClass);
      if (result == null) {
        final Class<?> targetClass = loadOrGetCachedClass(targetClassName.toString());
        result = targetClass != null && referenceClass.isAssignableFrom(targetClass);
        results.put(referenceClass, result);
      }
      return result;
    }
  }

  // Results of checkInstanceOf, by target class name and then by reference class or class name.
  // Role checks a node's class against dozens of classes, many times for each focus change, so
  // looking each pair up again was a large part of the time to answer a swipe. The answers never
  // change, but apps have many class names, so only the most recently used are kept.
  private static final int MAX_TARGET_CLASSES = 512;

  private static final LinkedHashMap<String, HashMap<Object, Boolean>> instanceOfResults =
      new LinkedHashMap<String, HashMap<Object, Boolean>>(64, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, HashMap<Object, Boolean>> eldest) {
          return size() > MAX_TARGET_CLASSES;
        }
      };
  // The target of the last check, since most checks come in runs for the same node's class name.
  private static @Nullable String lastTargetClassName;
  private static HashMap<Object, Boolean> lastTargetResults = new HashMap<>();

  @SuppressWarnings({"StringEquality", "ReferenceEquality"})
  private static HashMap<Object, Boolean> instanceOfResultsFor(String targetClassName) {
    // The same string object as last time needs no lookup, nor comparing its characters.
    if (targetClassName == lastTargetClassName) {
      return lastTargetResults;
    }
    HashMap<Object, Boolean> results = instanceOfResults.get(targetClassName);
    if (results == null) {
      results = new HashMap<>();
      instanceOfResults.put(targetClassName, results);
    }
    lastTargetClassName = targetClassName;
    lastTargetResults = results;
    return results;
  }
}
