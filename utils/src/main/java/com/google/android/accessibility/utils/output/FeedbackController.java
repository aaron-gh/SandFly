/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.google.android.accessibility.utils.output;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.os.Vibrator;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.R;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A feedback controller that caches sounds for quicker playback. */
public class FeedbackController {

  //////////////////////////////////////////////////////////////////////////////////////////
  // Constants

  private static final String TAG = "FeedbackController";

  /** Maximum number of concurrent audio streams. */
  private static final int MAX_STREAMS = 10;

  public static final long NO_SEPARATION = 0;

  /** Positioned sounds are panned between the left and right speakers. */
  public static final int SPATIAL_STEREO = 0;

  /** Positioned sounds are played in 3D, for headphones. */
  public static final int SPATIAL_3D = 1;

  /** Positioned sounds are played in 3D when headphones are connected, and panned otherwise. */
  public static final int SPATIAL_3D_WITH_HEADPHONES = 2;

  /**
   * How Backtalk's sounds play: as speech, so that they follow the audio output device and the
   * volume chosen for speech. Set by {@link #setUseAccessibilityStream}.
   */
  private static volatile AudioAttributes sFeedbackAttributes =
      feedbackAttributes(/* useAccessibilityStream= */ true);

  //////////////////////////////////////////////////////////////////////////////////////////
  // Member data

  /** The parent context. */
  private final Context mContext;

  /** The resources for this context. */
  private final Resources mResources;

  /** The SoundPool instance for loading sounds and playing previously loaded sounds. */
  private SoundPool mSoundPool;

  /** Whether sounds play through {@link LowLatencyAudio}. */
  private volatile boolean mLowLatencyAudio;

  /** The most decoded samples kept for {@link LowLatencyAudio}, about 8 MB. */
  private static final long MAX_CLIP_SAMPLES = 2_000_000L;

  /** The most sounds remembered for {@link LowLatencyAudio}, decoded or not. */
  private static final int MAX_CLIPS = 256;

  /**
   * Sounds decoded for {@link LowLatencyAudio}, by resource or file, or null while being decoded or
   * if they cannot be, the least recently played first. Guarded by itself.
   */
  private final LinkedHashMap<String, LowLatencyAudio.@Nullable Clip> mClips =
      new LinkedHashMap<>(16, 0.75f, /* accessOrder= */ true);

  /** The samples held by {@link #mClips}. Guarded by {@link #mClips}. */
  private long mClipSamples;

  /** Decodes sounds for {@link LowLatencyAudio}, on a thread that ends when it has nothing to do. */
  private final ThreadPoolExecutor mDecoder = createDecoder();

  /** The vibration service used to play vibration patterns. */
  private final Vibrator mVibrator;

  /** Map from the resource IDs of loaded sounds to SoundPool sound IDs. */
  private final SparseIntArray mSoundIds = new SparseIntArray();

  /** The file each sound in {@link #mSoundIds} was loaded from, if it was a custom sound. */
  private final SparseArray<String> mLoadedPaths = new SparseArray<>();

  /** Whether each sound played so far has a sound resource of its own. */
  private final SparseBooleanArray mHasOwnSound = new SparseBooleanArray();

  /** Sound files the user chose to play in place of the app's own, by sound resource name. */
  private Map<String, String> mCustomSoundPaths = Collections.emptyMap();

  /**
   * Vibration patterns that play in place of the usual ones, by the resource names of the sounds
   * they go with. An empty pattern plays no vibration.
   */
  private Map<String, int[]> mThemeVibrations = Collections.emptyMap();

  private final HapticPatternParser parser;

  /** The volume adjustment for sound feedback. */
  private float mVolumeAdjustment = 1.0f;

  private boolean mAuditoryEnabled;
  private boolean mHapticEnabled;

  /** Resource names of the sounds the user turned off one by one. */
  private Set<String> mMutedAuditoryNames = Collections.emptySet();

  /** Resource names of the vibration patterns the user turned off one by one. */
  private Set<String> mMutedHapticNames = Collections.emptySet();

  /** Cache of resource names, so muting does not look one up on every sound. */
  private final SparseArray<String> mResourceNames = new SparseArray<>();

  /** Cache of whether resources are vibration patterns rather than sounds, by resource ID. */
  private final SparseBooleanArray mIsPattern = new SparseBooleanArray();

  /** The vibration pattern that plays with each sound, by the sound's resource entry name. */
  private Map<String, Integer> mSoundHaptics = Collections.emptyMap();

  /** Skips an event's own vibration right after its sound vibrated. */
  private final SoundHapticCover mSoundHapticCover = new SoundHapticCover();

  private final Set<HapticFeedbackListener> mHapticFeedbackListeners = new HashSet<>();

  /** Starts and stops vibrations in order, off the main thread. Theme vibrations use it too. */
  static final Executor VIBRATION_EXECUTOR =
      Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "BacktalkVibration"));

  private final @NonNull HashMap<Integer, Long> resIdToLastPlayUptimeMillisec = new HashMap<>();

  /** How sounds with a position on the screen are played, one of the {@code SPATIAL_} values. */
  private int mSpatialMode = SPATIAL_3D_WITH_HEADPHONES;

  /** Created the first time a sound is played in 3D. */
  private @Nullable SpatialSoundPlayer mSpatialSoundPlayer;

  /** Follows headphones connecting, from the first time it matters. */
  private @Nullable AudioDeviceCallback mAudioDeviceCallback;

  private volatile boolean mHeadphonesConnected;

  /** When {@link #mHeadphonesConnected} was worked out, or 0 to work it out again. */
  private volatile long mHeadphonesCheckedAt;

  /** How long whether sounds go to headphones is kept before asking again. */
  private static final long HEADPHONES_CHECK_MS = 1000;

  //////////////////////////////////////////////////////////////////////////////////////////
  // Construction

  public FeedbackController(Context context) {
    this(context, createSoundPool(), (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
  }

  public FeedbackController(Context context, SoundPool soundPool, Vibrator vibrator) {
    mContext = context;
    mResources = context.getResources();
    mSoundPool = soundPool;
    mVibrator = vibrator;
    parser = new HapticPatternParser(mVibrator);
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  // Methods

  /**
   * Plays the vibration pattern associated with the given resource ID, unless a sound for the same
   * event just played its own vibration, or would have if the user had not turned it off.
   *
   * <p>Given a sound's resource ID, it plays that sound's vibration without the sound, such as for
   * a control whose sound is off or that the theme gives only a vibration. That stands in for the
   * sound's own vibration, which always plays, so it is never skipped: a swipe's focus is spoken
   * under the swipe's event, whose gesture sound has just vibrated.
   *
   * @param resId The vibration pattern's resource identifier, or a sound's.
   * @return {@code true} if successful.
   */
  public boolean playHaptic(int resId, @Nullable EventId eventId) {
    if (isPattern(resId) && mSoundHapticCover.covers(eventId, SystemClock.uptimeMillis())) {
      LogUtils.v(TAG, "playHaptic() resId=%d skipped, the sound vibrated", resId);
      return false;
    }
    return vibrate(resId, eventId);
  }

  /**
   * Plays the vibration that goes with a sound, whether or not sound feedback is on, so that every
   * sound can be felt as well as heard.
   */
  private void playSoundHaptic(int soundResId, @Nullable EventId eventId) {
    if (!mHapticEnabled || (mSoundHaptics.isEmpty() && mThemeVibrations.isEmpty())) {
      return;
    }
    @Nullable String name = resourceName(soundResId);
    @Nullable Integer patternResId = name == null ? null : mSoundHaptics.get(name);
    int @Nullable [] themePattern = name == null ? null : mThemeVibrations.get(name);
    if (themePattern != null) {
      // Even an empty pattern, of a vibration that is off, keeps the event's own vibration quiet.
      vibratePattern(themePattern, eventId);
      mSoundHapticCover.soundVibrated(eventId, SystemClock.uptimeMillis());
    } else if (patternResId != null) {
      // Even if the user turned this vibration off, the event's own vibration stays quiet.
      vibrate(patternResId, eventId);
      mSoundHapticCover.soundVibrated(eventId, SystemClock.uptimeMillis());
    }
  }

  private boolean vibrate(int resId, @Nullable EventId eventId) {
    // A sound's vibration from the theme, played on its own, such as for a control with a
    // vibration and no sound in the theme.
    @Nullable String name = mThemeVibrations.isEmpty() ? null : resourceName(resId);
    int @Nullable [] themePattern = name == null ? null : mThemeVibrations.get(name);
    if (themePattern != null) {
      return vibratePattern(themePattern, eventId);
    }
    if (!mHapticEnabled || resId == 0 || isMuted(mMutedHapticNames, resId)) {
      return false;
    }
    LogUtils.v(TAG, "playHaptic() resId=%d eventId=%s", resId, eventId);

    final int[] patternArray;
    try {
      patternArray = mResources.getIntArray(resId);
    } catch (NotFoundException e) {
      LogUtils.e(TAG, "Failed to load pattern %d", resId);
      return false;
    }

    return vibratePattern(patternArray, eventId);
  }

  /** Plays a vibration pattern in the format {@link HapticPatternParser} reads. */
  private boolean vibratePattern(int[] patternArray, @Nullable EventId eventId) {
    if (!mHapticEnabled || patternArray.length == 0) {
      return false;
    }
    final VibrationEffect effect;
    try {
      effect = parser.parse(patternArray);
    } catch (RuntimeException e) {
      // A theme's pattern that the device refuses.
      LogUtils.e(TAG, "Cannot play vibration pattern: %s", e);
      return false;
    }

    long nanoTime = System.nanoTime();
    for (HapticFeedbackListener listener : mHapticFeedbackListeners) {
      listener.onHapticFeedbackStarting(nanoTime);
    }

    // Starting a vibration waits for the system, so it is done off the main thread, where it would
    // hold up the speech for the same gesture or focus change.
    VIBRATION_EXECUTOR.execute(() -> mVibrator.vibrate(effect));

    return true;
  }

  /**
   * Adds a listener to be called when haptic feedback begins.
   *
   * @param listener The listener to add.
   */
  public void addHapticFeedbackListener(FeedbackController.HapticFeedbackListener listener) {
    mHapticFeedbackListeners.add(listener);
  }

  /**
   * Removes a HapticFeedbackListener.
   *
   * @param listener The listener to remove.
   */
  public void removeHapticFeedbackListener(FeedbackController.HapticFeedbackListener listener) {
    mHapticFeedbackListeners.remove(listener);
  }

  /**
   * Plays the auditory feedback associated with the given resource ID using the default rate,
   * volume, and panning.
   *
   * @param resId The auditory feedback's resource identifier.
   */
  public void playAuditory(int resId, @Nullable EventId eventId) {
    playAuditory(resId, 1.0f /* rate */, 1.0f /* volume */, eventId);
  }

  /** Plays audio-resource only if it has not been played in the last separationMillisec. */
  public void playAuditory(
      int resId,
      final float rate,
      float volume,
      boolean ignoreVolumeAdjustment,
      @Nullable EventId eventId,
      long separationMillisec) {
    if (separationMillisec != NO_SEPARATION) {
      @Nullable Long lastPlayUptimeMillisec = resIdToLastPlayUptimeMillisec.get(resId);
      long nowUptimeMillisec = SystemClock.uptimeMillis();
      // If time to play... update last-play-time... else... skip playing.
      if ((lastPlayUptimeMillisec == null)
          || (separationMillisec < nowUptimeMillisec - lastPlayUptimeMillisec)) {
        resIdToLastPlayUptimeMillisec.put(resId, nowUptimeMillisec);
      } else {
        return;
      }
    }

    playAuditory(resId, rate, volume, ignoreVolumeAdjustment, eventId);
  }

  /**
   * Plays the auditory feedback associated with the given resource ID using the specified rate,
   * volume, and panning.
   *
   * @param resId The auditory feedback's resource identifier.
   * @param rate The playback rate adjustment, from 0.5 (half speed) to 2.0 (double speed).
   * @param volume The volume adjustment, from 0.0 (mute) to 1.0 (original volume).
   */
  public void playAuditory(int resId, final float rate, float volume, @Nullable EventId eventId) {
    playAuditory(resId, rate, volume, /* ignoreVolumeAdjustment= */ false, eventId);
  }

  /**
   * Plays the auditory feedback associated with the given resource ID using the specified rate,
   * volume, and panning.
   *
   * @param resId The auditory feedback's resource identifier.
   * @param rate The playback rate adjustment, from 0.5 (half speed) to 2.0 (double speed).
   * @param volume The volume adjustment, from 0.0 (mute) to 1.0 (original volume).
   * @param ignoreVolumeAdjustment Ignore the volume adjustment from {@link
   *     #setVolumeAdjustment(float)} or not.
   */
  public void playAuditory(
      int resId,
      final float rate,
      float volume,
      boolean ignoreVolumeAdjustment,
      @Nullable EventId eventId) {
    if (resId != 0) {
      playSoundHaptic(resId, eventId);
    }
    playSound(resId, rate, volume, ignoreVolumeAdjustment, eventId);
  }

  /**
   * Plays a sound without the vibration that goes with it, for sounds that should not be felt,
   * such as repeating progress tones, or whose vibration is someone else's, such as braille.
   */
  public void playAuditoryWithoutHaptic(int resId, @Nullable EventId eventId) {
    playAuditoryWithoutHaptic(resId, 1.0f /* rate */, 1.0f /* volume */, eventId);
  }

  /** Plays a sound with the given rate and volume, without its vibration. */
  public void playAuditoryWithoutHaptic(
      int resId, float rate, float volume, @Nullable EventId eventId) {
    playSound(resId, rate, volume, /* ignoreVolumeAdjustment= */ false, eventId);
  }

  private void playSound(
      int resId,
      final float rate,
      float volume,
      boolean ignoreVolumeAdjustment,
      @Nullable EventId eventId) {
    if (!mAuditoryEnabled
        || resId == 0
        || isMuted(mMutedAuditoryNames, resId)
        || !hasSound(resId)) {
      return;
    }
    LogUtils.v(TAG, "playAuditory() resId=%d eventId=%s", resId, eventId);

    final float adjustedVolume = ignoreVolumeAdjustment ? volume : volume * mVolumeAdjustment;
    playFromPool(resId, rate, adjustedVolume, adjustedVolume);
  }

  /**
   * Plays a sound as if it came from a place on the screen, in 3D or panned between the speakers
   * according to {@link #setSpatialMode}. In 3D mode the sound is played in 3D on the phone's
   * speaker too.
   *
   * @param x The place from the left edge of the screen, from 0 to 1, or negative for no place.
   * @param y The place from the top edge of the screen, from 0 to 1, or negative for no place.
   */
  public void playAuditory(
      int resId, float rate, float volume, float x, float y, @Nullable EventId eventId) {
    if (x < 0 || y < 0) {
      playAuditory(resId, rate, volume, eventId);
      return;
    }
    if (resId != 0) {
      playSoundHaptic(resId, eventId);
    }
    playPlacedSound(resId, rate, volume, x, y, eventId);
  }

  /** Plays a sound from a place on the screen, like {@link #playAuditory}, without its vibration. */
  public void playAuditoryWithoutHaptic(
      int resId, float rate, float volume, float x, float y, @Nullable EventId eventId) {
    if (x < 0 || y < 0) {
      playAuditoryWithoutHaptic(resId, rate, volume, eventId);
      return;
    }
    playPlacedSound(resId, rate, volume, x, y, eventId);
  }

  private void playPlacedSound(
      int resId, float rate, float volume, float x, float y, @Nullable EventId eventId) {
    if (!mAuditoryEnabled
        || resId == 0
        || isMuted(mMutedAuditoryNames, resId)
        || !hasSound(resId)) {
      return;
    }
    LogUtils.v(TAG, "playAuditory() resId=%d x=%.2f y=%.2f eventId=%s", resId, x, y, eventId);

    float adjustedVolume = volume * mVolumeAdjustment;
    if (shouldPlayIn3d()) {
      if (mSpatialSoundPlayer == null) {
        mSpatialSoundPlayer = new SpatialSoundPlayer(mContext);
      }
      mSpatialSoundPlayer.play(resId, customSoundPath(resId), x, y, adjustedVolume);
    } else {
      // Full volume in the middle, fading out of the far speaker towards either edge.
      float pan = Math.max(-1f, Math.min(1f, (x - 0.5f) * 2));
      playFromPool(
          resId,
          rate,
          adjustedVolume * Math.min(1f, 1 - pan),
          adjustedVolume * Math.min(1f, 1 + pan));
    }
  }

  private void playFromPool(int resId, float rate, float leftVolume, float rightVolume) {
    @Nullable String path = customSoundPath(resId);
    if (mLowLatencyAudio
        && !LowLatencyAudio.isMicrophoneInUse()
        && playLowLatency(resId, path, rate, leftVolume, rightVolume)) {
      return;
    }
    int soundId = mSoundIds.get(resId);
    if (soundId != 0 && !TextUtils.equals(path, mLoadedPaths.get(resId))) {
      // The user chose another sound since this one was loaded.
      mSoundPool.unload(soundId);
      mSoundIds.delete(resId);
      soundId = 0;
    }

    if (soundId != 0) {
      new EarconsPlayTask(mSoundPool, soundId, leftVolume, rightVolume, rate).execute();
    } else {
      // The sound could not be played from the cache. Start loading the sound into the
      // SoundPool for future use, and use a listener to play the sound ASAP.
      mSoundPool.setOnLoadCompleteListener(
          (soundPool, sampleId, status) -> {
            if (mAuditoryEnabled && sampleId != 0) {
              new EarconsPlayTask(mSoundPool, sampleId, leftVolume, rightVolume, rate).execute();
            }
          });
      mSoundIds.put(
          resId, path != null ? mSoundPool.load(path, 1) : mSoundPool.load(mContext, resId, 1));
      mLoadedPaths.put(resId, path);
    }
  }

  /**
   * Returns whether {@code resId} has a sound to play: a sound of its own, or one the user chose. A
   * control sound with only a vibration in the theme has neither.
   */
  private boolean hasSound(int resId) {
    if (customSoundPath(resId) != null) {
      return true;
    }
    int index = mHasOwnSound.indexOfKey(resId);
    if (index >= 0) {
      return mHasOwnSound.valueAt(index);
    }
    boolean hasOwn;
    try {
      hasOwn = "raw".equals(mResources.getResourceTypeName(resId));
    } catch (NotFoundException e) {
      hasOwn = false;
    }
    mHasOwnSound.put(resId, hasOwn);
    return hasOwn;
  }

  /** Returns whether {@code resId} is a vibration pattern, not a sound whose vibration plays. */
  private boolean isPattern(int resId) {
    int index = mIsPattern.indexOfKey(resId);
    if (index >= 0) {
      return mIsPattern.valueAt(index);
    }
    boolean isPattern;
    try {
      isPattern = "array".equals(mResources.getResourceTypeName(resId));
    } catch (NotFoundException e) {
      isPattern = true;
    }
    mIsPattern.put(resId, isPattern);
    return isPattern;
  }

  /** Returns the file the user chose to play in place of {@code resId}, or null for its own. */
  private @Nullable String customSoundPath(int resId) {
    if (mCustomSoundPaths.isEmpty()) {
      return null;
    }
    @Nullable String name = resourceName(resId);
    return name == null ? null : mCustomSoundPaths.get(name);
  }

  private boolean shouldPlayIn3d() {
    switch (mSpatialMode) {
      case SPATIAL_3D:
        return true;
      case SPATIAL_3D_WITH_HEADPHONES:
        return isHeadphoneOutput();
      default:
        return false;
    }
  }

  /**
   * Returns whether Backtalk's sounds go to headphones: a Bluetooth headset or headphones, wired or
   * USB headphones, or hearing aids. On Android 13 and later it asks where audio with Backtalk's
   * attributes goes, so that sounds stay flat on the phone speaker, and on a Bluetooth speaker or
   * car that is connected but not playing them. Before that, it can only tell what is connected.
   */
  private boolean isHeadphoneOutput() {
    AudioManager audioManager = mContext.getSystemService(AudioManager.class);
    if (audioManager == null) {
      return false;
    }
    if (mAudioDeviceCallback == null) {
      mAudioDeviceCallback =
          new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
              mHeadphonesCheckedAt = 0;
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
              mHeadphonesCheckedAt = 0;
            }
          };
      audioManager.registerAudioDeviceCallback(mAudioDeviceCallback, null);
    }
    // Asking goes to the audio service, so the answer is kept for a moment, and until a device
    // comes or goes.
    long now = SystemClock.uptimeMillis();
    if (mHeadphonesCheckedAt == 0 || now - mHeadphonesCheckedAt > HEADPHONES_CHECK_MS) {
      mHeadphonesConnected = headphonesPlaySounds(audioManager);
      mHeadphonesCheckedAt = now;
    }
    return mHeadphonesConnected;
  }

  private static boolean headphonesPlaySounds(AudioManager audioManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      for (AudioDeviceInfo device :
          audioManager.getAudioDevicesForAttributes(sFeedbackAttributes)) {
        if (isHeadphone(device)) {
          return true;
        }
      }
      return false;
    }
    for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
      if (isHeadphone(device)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isHeadphone(AudioDeviceInfo device) {
    switch (device.getType()) {
      case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
      case AudioDeviceInfo.TYPE_WIRED_HEADSET:
      case AudioDeviceInfo.TYPE_USB_HEADSET:
      case AudioDeviceInfo.TYPE_HEARING_AID:
      // A2DP is also used by Bluetooth speakers and cars, which can't be told apart from
      // headphones, so it counts. Bluetooth LE says when a device is a speaker, and SCO carries
      // calls.
      case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
      case AudioDeviceInfo.TYPE_BLE_HEADSET:
        return true;
      default:
        return false;
    }
  }

  /** Interrupts all ongoing feedback. */
  public void interrupt() {
    // TODO: Stop all sounds.
    VIBRATION_EXECUTOR.execute(mVibrator::cancel);
  }

  /**
   * Releases all resources held by the feedback controller and clears the shared instance. No calls
   * should be made to this instance after calling this method.
   */
  public void shutdown() {
    mHapticFeedbackListeners.clear();
    mLowLatencyAudio = false;
    mDecoder.shutdownNow();
    clearClips();
    LowLatencyAudio.shutdownAll();
    mSoundPool.release();
    if (mSpatialSoundPlayer != null) {
      mSpatialSoundPlayer.shutdown();
      mSpatialSoundPlayer = null;
    }
    if (mAudioDeviceCallback != null) {
      AudioManager audioManager = mContext.getSystemService(AudioManager.class);
      if (audioManager != null) {
        audioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
      }
      mAudioDeviceCallback = null;
    }
    VIBRATION_EXECUTOR.execute(mVibrator::cancel);
    mAuditoryEnabled = false;
    mHapticEnabled = false;
  }

  /**
   * Sets whether to enable or disable the haptic feedback.
   *
   * @param enabled Whether haptic feedback should be enabled.
   */
  public void setHapticEnabled(boolean enabled) {
    mHapticEnabled = enabled;
  }

  /**
   * Sets whether to enable or disable the auditory feedback.
   *
   * @param enabled Whether auditory feedback should be enabled.
   */
  public void setAuditoryEnabled(boolean enabled) {
    mAuditoryEnabled = enabled;
  }

  /**
   * Sets the sounds to skip while auditory feedback is on.
   *
   * @param resourceNames Resource entry names, such as {@code "focus"} for {@code R.raw.focus}.
   */
  public void setMutedAuditory(Set<String> resourceNames) {
    mMutedAuditoryNames = new HashSet<>(resourceNames);
  }

  /**
   * Sets the vibration patterns to skip while haptic feedback is on.
   *
   * @param resourceNames Resource entry names, such as {@code "view_clicked_pattern"}.
   */
  public void setMutedHaptic(Set<String> resourceNames) {
    mMutedHapticNames = new HashSet<>(resourceNames);
  }

  /**
   * Sets the vibration pattern that plays with each sound. Each sound's vibration plays even when
   * sound feedback is off.
   *
   * @param patternsBySoundName Pattern resource IDs, by sound resource entry names such as {@code
   *     "focus"} for {@code R.raw.focus}.
   */
  public void setSoundHaptics(Map<String, Integer> patternsBySoundName) {
    mSoundHaptics = new HashMap<>(patternsBySoundName);
  }

  private boolean isMuted(Set<String> mutedNames, int resId) {
    if (mutedNames.isEmpty()) {
      return false;
    }
    @Nullable String name = resourceName(resId);
    return name != null && mutedNames.contains(name);
  }

  private @Nullable String resourceName(int resId) {
    String name = mResourceNames.get(resId);
    if (name == null) {
      try {
        name = mResources.getResourceEntryName(resId);
      } catch (NotFoundException e) {
        return null;
      }
      mResourceNames.put(resId, name);
    }
    return name;
  }

  /**
   * Sets the current volume adjustment for auditory feedback.
   *
   * @param adjustment The amount by which to adjust the volume of auditory feedback. 0.0 mutes the
   *     feedback while 1.0 plays it at its original volume.
   */
  public void setVolumeAdjustment(float adjustment) {
    mVolumeAdjustment = adjustment;
  }

  /**
   * Sets the sound files to play in place of the app's own sounds, by the resource names of the
   * sounds they replace. Sounds not in the map play as usual.
   */
  public void setCustomSounds(Map<String, String> pathsByResourceName) {
    if (mCustomSoundPaths.equals(pathsByResourceName)) {
      return;
    }
    mCustomSoundPaths = new HashMap<>(pathsByResourceName);
    if (mSpatialSoundPlayer != null) {
      mSpatialSoundPlayer.forgetSounds();
    }
  }

  /**
   * Sets the vibrations to play with sounds in place of the usual ones, by the resource names of
   * the sounds, in the format {@link HapticPatternParser} reads. An empty pattern plays none, so
   * a vibration the user turned off is passed as an empty pattern.
   */
  public void setThemeVibrations(Map<String, int[]> patternsBySoundName) {
    mThemeVibrations = new HashMap<>(patternsBySoundName);
  }

  /**
   * Sets how sounds with a place on the screen are played.
   *
   * @param mode {@link #SPATIAL_STEREO}, {@link #SPATIAL_3D} or {@link
   *     #SPATIAL_3D_WITH_HEADPHONES}.
   */
  public void setSpatialMode(int mode) {
    mSpatialMode = mode;
  }

  /**
   * Provides vibration and sound feedback to acknowledge the completion of an action (e.g. item
   * selection in Switch Access, gesture completion in TalkBack, etc.).
   */
  public void playActionCompletionFeedback() {
    // The sound first, so that its own vibration stands in for the one below.
    playAuditory(R.raw.window_state, EVENT_ID_UNTRACKED);
    playHaptic(R.array.window_state_pattern, EVENT_ID_UNTRACKED);
  }

  private static SoundPool createSoundPool() {
    return new SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(sFeedbackAttributes)
        .build();
  }

  /** How Backtalk's sounds play, with the volume speech uses. */
  public static AudioAttributes feedbackAttributes() {
    return sFeedbackAttributes;
  }

  /** How sounds play with the accessibility volume, or else the media volume. */
  public static AudioAttributes feedbackAttributes(boolean useAccessibilityStream) {
    return new AudioAttributes.Builder()
        .setUsage(
            useAccessibilityStream
                ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                : AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build();
  }

  /**
   * Sets whether sounds use the accessibility volume or the media volume, the same as speech. The
   * sounds loaded so far are loaded again, with the new volume.
   */
  public void setUseAccessibilityStream(boolean useAccessibilityStream) {
    AudioAttributes attributes = feedbackAttributes(useAccessibilityStream);
    if (attributes.getUsage() == sFeedbackAttributes.getUsage()) {
      return;
    }
    sFeedbackAttributes = attributes;
    mSoundPool.release();
    mSoundPool = createSoundPool();
    mSoundIds.clear();
    mLoadedPaths.clear();
    // Whether sounds reach headphones is asked again, for the new kind of audio.
    mHeadphonesCheckedAt = 0;
  }

  /**
   * Plays the sound, or the file at {@code path} in its place, through the low-latency player, and
   * returns whether it did. A sound plays the usual way the first time, while it is decoded for
   * next time, and whenever it cannot be decoded.
   */
  private boolean playLowLatency(
      int resId, @Nullable String path, float rate, float leftVolume, float rightVolume) {
    @Nullable LowLatencyAudio player = LowLatencyAudio.get(mContext, sFeedbackAttributes);
    if (player == null) {
      return false;
    }
    // By file for a file, so that a replaced file is decoded again.
    String key = path != null ? path : "res:" + resId;
    LowLatencyAudio.@Nullable Clip clip;
    synchronized (mClips) {
      clip = mClips.get(key);
      if (clip == null) {
        if (!mClips.containsKey(key)) {
          // Null marks a sound being decoded, or one that cannot be.
          mClips.put(key, null);
          trimClips(key);
          mDecoder.execute(() -> decodeClip(player, key, resId, path));
        }
        return false;
      }
    }
    player.play(clip, leftVolume, rightVolume, rate);
    return true;
  }

  /** Decodes a sound for {@link #mClips}, on {@link #mDecoder}. */
  private void decodeClip(LowLatencyAudio player, String key, int resId, @Nullable String path) {
    LowLatencyAudio.@Nullable Clip prepared = null;
    try {
      AudioDecoder.@Nullable Decoded decoded =
          path != null ? AudioDecoder.decode(path) : AudioDecoder.decode(mContext, resId);
      if (decoded != null) {
        prepared = player.prepare(decoded);
      }
    } catch (Throwable e) {
      // A broken sound or running out of memory must never take the screen reader down, and an
      // OutOfMemoryError is not an Exception. The sound stays marked as one that cannot be decoded.
      LogUtils.e(TAG, "Cannot prepare sound %s: %s", key, e);
    }
    if (prepared == null) {
      return;
    }
    synchronized (mClips) {
      // Not if the sounds were dropped while it decoded.
      if (mClips.containsKey(key)) {
        mClips.put(key, prepared);
        mClipSamples += prepared.getSize();
        trimClips(key);
      }
    }
  }

  /**
   * Forgets the least recently played sounds beyond the limits, but never {@code keep}. Called with
   * {@link #mClips} held.
   */
  private void trimClips(String keep) {
    Iterator<Map.Entry<String, LowLatencyAudio.@Nullable Clip>> oldest =
        mClips.entrySet().iterator();
    while ((mClipSamples > MAX_CLIP_SAMPLES || mClips.size() > MAX_CLIPS) && oldest.hasNext()) {
      Map.Entry<String, LowLatencyAudio.@Nullable Clip> entry = oldest.next();
      if (entry.getKey().equals(keep)) {
        continue;
      }
      LowLatencyAudio.@Nullable Clip clip = entry.getValue();
      if (clip != null) {
        mClipSamples -= clip.getSize();
      }
      oldest.remove();
    }
  }

  private void clearClips() {
    synchronized (mClips) {
      mClips.clear();
      mClipSamples = 0;
    }
  }

  private static ThreadPoolExecutor createDecoder() {
    ThreadPoolExecutor decoder =
        new ThreadPoolExecutor(
            1,
            1,
            5,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> new Thread(runnable, "BacktalkSoundDecoder"));
    decoder.allowCoreThreadTimeOut(true);
    return decoder;
  }

  /**
   * Sets whether sounds play through the low-latency player, which reaches the speaker sooner than
   * the usual way. Turning it off frees the player and the decoded sounds.
   */
  public void setLowLatencyAudio(boolean enabled) {
    boolean wasEnabled = mLowLatencyAudio;
    mLowLatencyAudio = enabled;
    if (wasEnabled && !enabled) {
      clearClips();
      LowLatencyAudio.shutdownAll();
    }
  }

  /**
   * Some features, such as the tap detector, may be affected by haptic feedback and want to know
   * when we initiate it.
   */
  public interface HapticFeedbackListener {

    /**
     * Alerts the listener that haptic feedback is about to start.
     *
     * @param currentNanoTime The current system time.
     */
    void onHapticFeedbackStarting(long currentNanoTime);
  }
}
