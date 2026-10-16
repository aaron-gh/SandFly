package com.google.android.accessibility.utils.output;

import static com.google.common.primitives.Ints.indexOf;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.VibrationEffect;
import android.os.VibrationEffect.Composition;
import android.os.Vibrator;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.IntPredicate;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.base.Function;
import java.util.function.BiFunction;

/**
 * Handles parsing a resource array defining a haptic pattern.
 *
 * <p>Each array contains up to three definitions: a "fallback" haptic using createWaveform(), an
 * optional "amplitude" haptic using createWaveform() with amplitudes, and a "premium" haptic using
 * VibrationEffect.Composition.
 *
 * <p>The fallback comes first. The amplitude haptic follows a sentinel value of -9998, and the
 * premium follows a sentinel value of -9999. If there is no sentinel, the array is assumed to be a
 * fallback only.
 *
 * <p>The fallback array uses the same array format passed to {@link
 * VibrationEffect#createWaveform(long[], int)}: alternating off and on times in ms, starting with
 * off.
 *
 * <p>The amplitude array is pairs of a time in ms and an amplitude from 0 (off) to 255, for devices
 * that can vary the strength of a vibration but cannot play the premium haptic.
 *
 * <p>The premium array consists of three elements per entry:
 *
 * <p>
 *
 * <ol>
 *   <li>Primitive ID constant See {@link android.os.VibrationEffect.Composition#addPrimitive(int)}
 *   <li>Scale (between 0-255)
 *   <li>delay (in ms)
 * </ol>
 */
public class HapticPatternParser {
  private static final String TAG = "HapticPatternParser";

  private static final int SENTINEL_SEPARATOR = -9999;
  private static final int AMPLITUDE_SEPARATOR = -9998;
  private static final float SCALE_MAX = 255f;

  private final Function<long[], VibrationEffect> waveformCreator;
  private final @Nullable BiFunction<long[], int[], VibrationEffect> amplitudeWaveformCreator;
  private final IntPredicate isPrimitiveSupported;

  /** Creates a new parser, using the given vibrator to determine compatibility. */
  public HapticPatternParser(Vibrator vibrator) {
    this(
        pattern -> VibrationEffect.createWaveform(pattern, /* repeat= */ -1),
        vibrator.hasAmplitudeControl()
            ? (timings, amplitudes) ->
                VibrationEffect.createWaveform(timings, amplitudes, /* repeat= */ -1)
            : null,
        vibrator::areAllPrimitivesSupported);
  }

  @VisibleForTesting
  public HapticPatternParser(
      Function<long[], VibrationEffect> waveformCreator, IntPredicate isPrimitiveSupported) {
    this(waveformCreator, /* amplitudeWaveformCreator= */ null, isPrimitiveSupported);
  }

  /**
   * @param amplitudeWaveformCreator creates a waveform from times and amplitudes, or null if the
   *     device cannot vary the strength of a vibration
   */
  @VisibleForTesting
  public HapticPatternParser(
      Function<long[], VibrationEffect> waveformCreator,
      @Nullable BiFunction<long[], int[], VibrationEffect> amplitudeWaveformCreator,
      IntPredicate isPrimitiveSupported) {
    this.waveformCreator = waveformCreator;
    this.amplitudeWaveformCreator = amplitudeWaveformCreator;
    this.isPrimitiveSupported = isPrimitiveSupported;
  }

  /**
   * Parses the given pattern definition and returns an appropriate effect depending on the device
   * capabilities.
   *
   * <p>See the {@link HapticPatternParser} class docs for the definition spec.
   */
  public VibrationEffect parse(int[] pattern) {
    int splitIndex = indexOf(pattern, SENTINEL_SEPARATOR);
    if (splitIndex >= 0 && BuildVersionUtils.isAtLeastR()) {
      @Nullable VibrationEffect effect = parseComposition(pattern, splitIndex + 1);
      if (effect == null) {
        LogUtils.v(TAG, "Haptic primitive unsupported on this device");
      } else {
        return effect;
      }
    }

    int fallbackEnd = splitIndex >= 0 ? splitIndex : pattern.length;
    int amplitudeIndex = indexOf(pattern, AMPLITUDE_SEPARATOR);
    if (amplitudeIndex >= 0 && amplitudeIndex < fallbackEnd) {
      if (amplitudeWaveformCreator != null) {
        return parseAmplitudes(pattern, amplitudeIndex + 1, fallbackEnd);
      }
      fallbackEnd = amplitudeIndex;
    }

    return parseFallback(pattern, fallbackEnd);
  }

  /**
   * Convert the pattern into a VibrationEffect or return null if this device is unable to handle
   * any of the primitives found in the pattern.
   */
  @TargetApi(VERSION_CODES.R)
  @Nullable
  private VibrationEffect parseComposition(int[] pattern, int splitIndex) {
    Composition effect = VibrationEffect.startComposition();
    for (int i = splitIndex; i < pattern.length; i += 3) {
      int primitive = pattern[i];
      if (!isPrimitiveSupported.test(primitive)) {
        return null;
      }

      effect.addPrimitive(primitive, (float) pattern[i + 1] / SCALE_MAX, pattern[i + 2]);
    }

    return effect.compose();
  }

  private VibrationEffect parseAmplitudes(int[] pattern, int start, int end) {
    int count = (end - start) / 2;
    long[] timings = new long[count];
    int[] amplitudes = new int[count];
    for (int i = 0; i < count; i++) {
      timings[i] = pattern[start + 2 * i];
      amplitudes[i] = pattern[start + 2 * i + 1];
    }
    return amplitudeWaveformCreator.apply(timings, amplitudes);
  }

  private VibrationEffect parseFallback(int[] patternDefinition, int patternLength) {
    final long[] pattern = new long[patternLength];
    for (int i = 0; i < patternLength; i++) {
      pattern[i] = patternDefinition[i];
    }

    return waveformCreator.apply(pattern);
  }
}
