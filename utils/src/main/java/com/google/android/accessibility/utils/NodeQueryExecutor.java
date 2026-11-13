/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded, restartable I/O workers. A rejected read is left to its caller's normal path. */
public final class NodeQueryExecutor implements Executor {
  private final String name;
  private final int threads;
  private final int capacity;
  private final Set<Runnable> tasks = new HashSet<>();
  private final AtomicInteger number = new AtomicInteger();
  private ThreadPoolExecutor executor;
  private boolean accepting = true;

  public NodeQueryExecutor(String name, int threads, int capacity) {
    if (threads <= 0 || capacity <= 0) {
      throw new IllegalArgumentException("Positive thread and queue limits are required");
    }
    this.name = name;
    this.threads = threads;
    this.capacity = capacity;
  }

  private ThreadPoolExecutor createExecutor() {
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
        threads, threads, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(capacity), work -> {
          Thread thread = new Thread(work, name + "-" + number.incrementAndGet());
          thread.setDaemon(true);
          return thread;
        }) {
      @Override
      protected void afterExecute(Runnable work, Throwable failure) {
        synchronized (NodeQueryExecutor.this) {
          tasks.remove(work);
        }
      }
    };
    pool.allowCoreThreadTimeOut(true);
    return pool;
  }

  @Override
  public synchronized void execute(Runnable work) {
    if (!accepting) {
      throw new RejectedExecutionException("Node queries stopped");
    }
    if (executor == null) {
      executor = createExecutor();
    }
    tasks.add(work);
    try {
      executor.execute(work);
    } catch (RejectedExecutionException failure) {
      tasks.remove(work);
      // Do not cancel work here: the caller may run this same read synchronously.
      throw failure;
    }
  }

  public <T> FutureTask<T> submit(Callable<T> read) {
    FutureTask<T> task = new FutureTask<>(read);
    execute(task);
    return task;
  }

  /** Cancels a result and removes a read that has not started from the bounded queue. */
  public synchronized void cancel(Future<?> future) {
    future.cancel(false);
    if (future instanceof Runnable work && executor != null && executor.remove(work)) {
      tasks.remove(work);
    }
  }

  /** A new service instance may start workers after the previous instance was stopped. */
  public synchronized void start() {
    accepting = true;
  }

  /** Does not join app IPC: a blocked old read must not hold up service shutdown. */
  public synchronized void shutdown() {
    accepting = false;
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
    for (Runnable work : tasks) {
      if (work instanceof Future<?> future) {
        future.cancel(false);
      }
    }
    tasks.clear();
  }

  synchronized int queuedTaskCount() {
    return executor == null ? 0 : executor.getQueue().size();
  }

  synchronized int trackedTaskCount() {
    return tasks.size();
  }

  synchronized boolean coreThreadsTimeOut() {
    return executor == null || executor.allowsCoreThreadTimeOut();
  }
}
