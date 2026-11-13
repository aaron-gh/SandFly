package com.google.android.accessibility.talkback.actor.gemini.progress;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressToneProvider.InfiniteIterator;
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressToneProvider.Tone;
import com.google.android.accessibility.utils.Consumer;
import com.google.android.accessibility.utils.Supplier;

/** Schedules and sequences playing a sequence of earcons to indicate loading progress */
public class ProgressTonePlayer {

  private static final int DELAY_PLAY_TONE_MS = 2500;
  private final Handler handler;
  private final ProgressToneProvider toneProvider;
  private final Consumer<Tone> tonePlayer;
  private final Runnable onTimeoutListener;
  private final Supplier<Long> currentTimeMsSupplier;
  private final Supplier<Long> timeoutDelayMsSupplier;

  @Nullable private InfiniteIterator<Tone> tones;
  private long endTimeMs;
  // Whether the tones for the on-device model loading play, rather than the others.
  private boolean modelLoading;
  // Whether the first tone has played, so that later ones are scheduled.
  private boolean tonesStarted;
  private final Runnable nextTone = this::playNextTone;

  /**
   * @param toneProvider provides the set of tones to play for each invocation of {@link
   *     #play(boolean)}
   * @param tonePlayer given a Tone, actually plays the sound via the feedback {@link Pipeline}
   * @param timeoutDelayMsSupplier how long to play tones before timing out, read on each play
   * @param onTimeoutListener called after timeoutDelayMs is reached after calling {@link
   *     #play(boolean)}
   */
  public ProgressTonePlayer(
      ProgressToneProvider toneProvider,
      Consumer<Tone> tonePlayer,
      Supplier<Long> timeoutDelayMsSupplier,
      Runnable onTimeoutListener) {
    this(
        toneProvider,
        tonePlayer,
        timeoutDelayMsSupplier,
        onTimeoutListener,
        new Handler(Looper.getMainLooper()),
        SystemClock::elapsedRealtime);
  }

  /**
   * @param toneProvider provides the set of tones to play for each invocation of {@link
   *     #play(boolean)}
   * @param tonePlayer given a Tone, actually plays the sound via the feedback {@link Pipeline}
   * @param timeoutDelayMsSupplier how long to play tones before timing out, read on each play
   * @param onTimeoutListener called after timeoutDelayMs is reached
   * @param handler handler to use for testing
   * @param currentTimeMsSupplier returns the current elapsed system realtime in milliseconds
   */
  @VisibleForTesting
  public ProgressTonePlayer(
      ProgressToneProvider toneProvider,
      Consumer<Tone> tonePlayer,
      Supplier<Long> timeoutDelayMsSupplier,
      Runnable onTimeoutListener,
      Handler handler,
      Supplier<Long> currentTimeMsSupplier) {
    this.handler = handler;
    this.toneProvider = toneProvider;
    this.tonePlayer = tonePlayer;
    this.onTimeoutListener = onTimeoutListener;
    this.currentTimeMsSupplier = currentTimeMsSupplier;
    this.timeoutDelayMsSupplier = timeoutDelayMsSupplier;
  }

  /**
   * Starts playback of the tones.
   *
   * @param playTone if true, plays the tones. If false, simply waits for the timeout without
   *     playing any tones.
   * @param delayPlayTone if true, delay the tone play.
   */
  public void play(boolean playTone, boolean delayPlayTone) {
    play(playTone, delayPlayTone, /* modelLoading= */ false);
  }

  /**
   * Starts playback of the tones, with the tones for the on-device model loading when {@code
   * modelLoading} is true.
   */
  public void play(boolean playTone, boolean delayPlayTone, boolean modelLoading) {
    stop();
    this.modelLoading = modelLoading;
    long timeoutDelayMs = timeoutDelayMsSupplier.get();
    long timeoutMs = currentTimeMsSupplier.get() + timeoutDelayMs;
    if (!playTone) {
      tones = null;
      handler.postDelayed(onTimeoutListener, timeoutDelayMs);
      return;
    }

    tones = toneProvider.getNextToneGroup(modelLoading);
    if (delayPlayTone) {
      handler.postDelayed(() -> playTonesInternal(timeoutMs), DELAY_PLAY_TONE_MS);
      return;
    }

    playTonesInternal(timeoutMs);
  }

  /** Stops playback and cancels pending tones. Does not call onTimeoutListener. */
  public void stop() {
    stopTones();
    tonesStarted = false;
    handler.removeCallbacksAndMessages(null);
  }

  /**
   * Switches between the tones for the on-device model loading and the others. When tones have
   * started, the new tone plays at once. Does nothing when no tones play. The timeout stays.
   */
  public void setModelLoading(boolean modelLoading) {
    if (tones == null || this.modelLoading == modelLoading) {
      return;
    }
    this.modelLoading = modelLoading;
    tones = toneProvider.getNextToneGroup(modelLoading);
    if (tonesStarted) {
      handler.removeCallbacks(nextTone);
      playNextTone();
    }
  }

  /** Whether the tones for the on-device model loading play. */
  public boolean isModelLoading() {
    return tones != null && modelLoading;
  }

  public void stopTones() {
    tones = null;
  }

  private void playTonesInternal(long endTimeMs) {
    this.endTimeMs = endTimeMs;
    tonesStarted = true;
    playNextTone();
  }

  private void playNextTone() {
    if (currentTimeMsSupplier.get() >= endTimeMs) {
      onTimeoutListener.run();
      return;
    }

    if (tones == null) {
      long timeoutMs = endTimeMs - currentTimeMsSupplier.get();
      if (timeoutMs > 0) {
        handler.postDelayed(onTimeoutListener, timeoutMs);
      } else {
        onTimeoutListener.run();
      }
      return;
    }

    Tone tone = tones.next();
    tonePlayer.accept(tone);
    handler.postDelayed(nextTone, tone.delayMs);
  }
}
