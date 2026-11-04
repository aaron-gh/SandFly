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

package com.google.android.accessibility.utils

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Passes on the accelerometer readings of a part of SandFly that reads the accelerometer fast, to
 * parts that need only a few readings, so that they don't register listeners of their own. Android
 * sends every accelerometer listener in a process readings at the fastest rate any of them asked
 * for.
 */
object AccelerometerShare {
  /** Takes readings: [timeNs] in the clock of sensor timestamps, acceleration in m/s². */
  fun interface Reader {
    fun onReading(timeNs: Long, x: Float, y: Float, z: Float)
  }

  private val readers = CopyOnWriteArrayList<Reader>()
  private val activeListeners = CopyOnWriteArrayList<Runnable>()

  /** Whether readings are being passed on. */
  @Volatile
  @JvmStatic
  var isActive = false
    private set

  /**
   * Sets whether the source reads the accelerometer and passes its readings on. Runs the listeners
   * of [addActiveListener] on the calling thread when it changes.
   */
  @JvmStatic
  fun setActive(active: Boolean) {
    if (isActive == active) {
      return
    }
    isActive = active
    for (listener in activeListeners) {
      listener.run()
    }
  }

  /** Passes a reading to the readers, on the source's thread. */
  @JvmStatic
  fun publish(timeNs: Long, x: Float, y: Float, z: Float) {
    for (reader in readers) {
      reader.onReading(timeNs, x, y, z)
    }
  }

  @JvmStatic
  fun addReader(reader: Reader) {
    readers.addIfAbsent(reader)
  }

  @JvmStatic
  fun removeReader(reader: Reader) {
    readers.remove(reader)
  }

  /** Runs [listener] whenever [isActive] changes. */
  @JvmStatic
  fun addActiveListener(listener: Runnable) {
    activeListeners.addIfAbsent(listener)
  }

  @JvmStatic
  fun removeActiveListener(listener: Runnable) {
    activeListeners.remove(listener)
  }
}
