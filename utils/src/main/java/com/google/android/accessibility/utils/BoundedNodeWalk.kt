/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import java.util.ArrayDeque
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Only reads run on workers; filters and traversal decisions belong to the caller. */
internal class BoundedNodeWalk<T : Any>(
  private val executor: NodeQueryExecutor,
  private val remainingMs: () -> Long,
  private val childCount: (T) -> Int,
  private val copy: (T) -> T,
  private val readChild: (T, Int) -> T?,
  private val accepts: (T) -> Boolean,
  private val descend: (T) -> Boolean,
  private val stopAfter: Int,
  private val slots: Int = 8,
  private val maxReads: Int = 5000,
) {
  data class Result<T>(val found: T?, val matches: Int, val complete: Boolean)
  private data class Frame<T>(val parent: T, val count: Int, var next: Int = 0)
  private data class Answer<T>(val child: T?, val failed: Boolean)

  fun run(root: T): Result<T> {
    val frames = ArrayDeque<Frame<T>>()
    frames.add(Frame(copy(root), childCount(root)))
    val seen = hashSetOf(root)
    val answers = ArrayBlockingQueue<Answer<T>>(slots)
    val tasks = mutableListOf<Future<*>>()
    val stopped = AtomicBoolean()
    var pending = 0
    var reads = 0
    var matches = 0
    var found: T? = null
    var failed = false
    var complete = false
    try {
      while (remainingMs() > 0 && executor.isAccepting) {
        while (frames.isNotEmpty() && frames.first.next >= frames.first.count) frames.removeFirst()
        while (pending < slots && frames.isNotEmpty() && reads < maxReads && remainingMs() > 0) {
          val frame = frames.first
          val index = frame.next++
          // Each request owns its node even if this walk times out while app IPC is running.
          val parent = copy(frame.parent)
          val read = {
            var child: T? = null
            var failure = false
            try {
              if (!stopped.get() && !Thread.currentThread().isInterrupted && executor.isAccepting && remainingMs() > 0) child = readChild(parent, index)
              else failure = true
            } catch (_: RuntimeException) { failure = true }
            if (!stopped.get()) answers.offer(Answer(child, failure))
          }
          pending++
          reads++
          try { tasks.add(executor.submit { read() }) }
          catch (_: RejectedExecutionException) {
            // The original read is still available on saturation, within this same deadline.
            if (executor.isAccepting) read() else break
          }
          while (frames.isNotEmpty() && frames.first.next >= frames.first.count) frames.removeFirst()
        }
        if (pending == 0) {
          complete = frames.isEmpty() && !failed
          break
        }
        val answer = answers.poll(remainingMs().coerceAtLeast(0), TimeUnit.MILLISECONDS) ?: break
        pending--
        tasks.removeAll { it.isDone }
        failed = failed || answer.failed
        val child = answer.child ?: continue
        if (!seen.add(child)) continue
        if (accepts(child)) {
          found = child
          matches++
          if (matches >= stopAfter) break
        } else if (descend(child) && remainingMs() > 0) {
          frames.addLast(Frame(copy(child), childCount(child)))
        }
      }
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
    } finally {
      stopped.set(true)
      tasks.forEach(executor::cancel)
    }
    return Result(found, matches, complete)
  }
}
