/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static android.speech.tts.TextToSpeech.LANG_AVAILABLE;
import static android.speech.tts.TextToSpeech.LANG_COUNTRY_AVAILABLE;
import static android.speech.tts.TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static androidx.core.content.ContextCompat.RECEIVER_EXPORTED;
import static com.google.android.accessibility.utils.output.SpeechCacheManager.CACHE_UTTERANCE_ID_PREFIX;
import static java.util.Locale.forLanguageTag;

import android.content.ComponentCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.ChangeLocaleAction;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.SpannableUtils.IdentifierSpan;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.accessibility.utils.broadcast.SameThreadBroadcastReceiver;
import com.google.android.accessibility.utils.compat.provider.SettingsCompatUtils.SecureCompatUtils;
import com.google.android.accessibility.utils.compat.speech.tts.TextToSpeechCompatUtils;
import com.google.android.accessibility.utils.output.SpeechCacheManager.LoadSpeechResultNotifier;
import com.google.android.accessibility.utils.output.SpeechCachePlayer.PlaybackStateListener;
import com.google.android.accessibility.utils.output.SpeechCachePlayer.SpeechInfo;
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.auto.value.AutoValue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wrapper for {@link TextToSpeech} that handles fail-over when a specific engine does not work.
 *
 * <p>Does <strong>NOT</strong> implement queuing! Every call to {@link #speak} flushes the global
 * speech queue.
 *
 * <p>This wrapper handles the following:
 *
 * <ul>
 *   <li>Fail-over from a failing TTS to a working one
 *   <li>Splitting utterances into &lt;4k character chunks
 *   <li>Switching to the system TTS when media is unmounted
 *   <li>Utterance-specific pitch and rate changes
 *   <li>Pitch and rate changes relative to the user preference
 * </ul>
 */
@SuppressWarnings("deprecation")
public class FailoverTextToSpeech {
  private static final String TAG = "FailoverTextToSpeech";

  /** The package name for the Google TTS engine. */
  private static final String PACKAGE_GOOGLE_TTS = "com.google.android.tts";

  /** Number of times a TTS engine can fail before switching. */
  private static final int MAX_TTS_FAILURES = 3;

  /** Maximum number of TTS error messages to print to the log. */
  private static final int MAX_LOG_MESSAGES = 10;

  public static final String AGGRESSIVE_CHUNK = "AggressiveChunk";

  public static final int VALUE_ON = 1;

  // Some apps have their own rate setting and send the absolute rate
  // Others have rate increase/decrease controls and just send the multiplier
  public static final String RATE_PARAMETER_TYPE = "RateParameterType";
  public static final int MULTIPLIER = 0; // This is the default
  public static final int ABSOLUTE = 1;

  /**
   * Ensure the consecutive manipulation is thread safe. For example, to synthesize file with give
   * locale, we need to invoke {@link TextToSpeech#setLanguage(Locale)} and {@link
   * TextToSpeech#synthesizeToFile(CharSequence, Bundle, File, String)}.
   */
  private final Object ttsLock = new Object();

  /** Removes the speech from the speech cache with the given {@link SpeechInfo}. */
  void removeSpeech(SpeechInfo speechInfo) {
    if (speechCacheManager != null) {
      speechCacheManager.removeSpeech(speechInfo);
    }
  }

  /** Clears the entire in-memory speech cache. */
  void cleanCache() {
    if (speechCacheManager != null) {
      speechCacheManager.cleanCache();
    }
  }

  /**
   * Sets the voice for the TTS engine.
   *
   * @param voice The voice to set.
   */
  public void setVoice(Voice voice) {
    if (isReady()) {
      synchronized (ttsLock) {
        tts.setVoice(voice);
      }
    }
  }

  public long getCacheSizeMb() {
    if (speechCacheManager != null) {
      return speechCacheManager.getCacheSizeMb();
    }
    return -1;
  }

  /** Class defining constants used for describing speech parameters. */
  public static final class SpeechParam {
    /** Float parameter for controlling speech volume. Range is {0 ... 2}. */
    public static final String VOLUME = Engine.KEY_PARAM_VOLUME;

    /** Float parameter for controlling speech rate. Range is {0 ... 2}. */
    public static final String RATE = "rate";

    /** Float parameter for controlling speech pitch. Range is {0 ... 2}. */
    public static final String PITCH = "pitch";

    public static final String FALLBACK_LOCALE =
        "com.google.android.tts:EnableEnUsVoiceSelectionFallback";

    private SpeechParam() {}
  }

  /**
   * Constant to flush speech globally. The constant corresponds to the non-public API {@link
   * TextToSpeech#QUEUE_DESTROY}. To avoid a bug, we always need to use {@link
   * TextToSpeech#QUEUE_FLUSH} before using {@link #SPEECH_FLUSH_ALL} -- on Android version M only.
   */
  static final int SPEECH_FLUSH_ALL = 2;

  /**
   * What fraction of the volume seekbar corresponds to a doubling of audio volume.
   *
   * <p>During a phone call, TalkBack speech is redirected from STREAM_MUSIC to STREAM_VOICE_CALL,
   * causing an unexpected change in TalkBack speech volume. During a phone call, we reduce the
   * TalkBack speech volume based on the volume difference between STREAM_MUSIC and
   * STREAM_VOICE_CALL. VOLUME_FRAC_PER_DOUBLING controls the amount of volume reduction per
   * difference of STREAM_MUSIC vs STREAM_VOICE_CALL.
   *
   * <p>On nexus 6, volume doubles every 11% volume seekbar step. On samsung s5, volume doubles
   * every 27% volume step. Setting adjustment too aggressively (too low) causes effective volume to
   * go down when call volume is higher -- the call volume seekbar would work in reverse for
   * TalkBack speech. Setting this adjustment too conservatively (too high) causes the original
   * volume jump to continue, though in lesser degree.
   */
  private static final float VOLUME_FRAC_PER_DOUBLING = 0.25f;

  /**
   * {@link BroadcastReceiver} for determining changes in the media state used for switching the TTS
   * engine.
   */
  private final MediaMountStateMonitor mediaStateMonitor = new MediaMountStateMonitor();

  /** A list of installed TTS engines. */
  private final LinkedList<String> installedTtsEngines = new LinkedList<>();

  private final Context context;
  private final ContentResolver resolver;

  /** The TTS engine. */
  private TextToSpeech tts;

  /** The engine loaded into the current TTS. */
  private String ttsEngine;

  public static final String PREF_TTS_ENGINE_KEY = "pref_tts_engine";
  public static final String PREF_USE_ACCESSIBILITY_STREAM_KEY = "pref_use_accessibility_stream";

  /**
   * Set while Backtalk routes its audio to a chosen device, which only accessibility audio follows.
   * Not a user setting.
   */
  public static final String PREF_ROUTE_FORCED_KEY = "pref_audio_route_forced";
  private static final boolean USE_ACCESSIBILITY_STREAM_DEFAULT = true;
  public static final String PREF_SPEAK_IN_PHRASES_KEY = "pref_speak_in_phrases";
  private static final boolean SPEAK_IN_PHRASES_DEFAULT = false;
  private volatile boolean speakInPhrases = SPEAK_IN_PHRASES_DEFAULT;

  /** Plays speech through {@link LowLatencyAudio}, which reaches the speaker sooner. */
  public static final String PREF_LOW_LATENCY_AUDIO_KEY = "pref_low_latency_audio";

  private volatile boolean lowLatencyAudio = false;

  /**
   * Returns whether low-latency audio is on when the user hasn't chosen. It's off on watches,
   * whose settings don't have the switch.
   */
  public static boolean lowLatencyAudioDefault(Context context) {
    return !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
  }

  /** Engines that gave no audio when synthesizing to a file, which speak the usual way. */
  private final Set<String> enginesWithoutFileAudio = ConcurrentHashMap.newKeySet();

  /**
   * Utterances sent to the engine to synthesize for {@link LowLatencyAudio}, by ID, until the
   * engine finishes with them. The engine's callbacks for these go to their streams, and the streams
   * report the utterance's progress as it plays.
   */
  private final Map<String, LowLatencyAudio.SpeechStream> lowLatencyStreams =
      new ConcurrentHashMap<>();

  /**
   * The utterance whose low-latency speech is held, mid-word, since a touch interrupted it, or null.
   * If the pause gesture follows, it stays held until speech resumes, and carries on from exactly
   * where it stopped. Otherwise it is dropped after {@link #HELD_SPEECH_TIMEOUT_MS}.
   */
  private volatile @Nullable String heldUtteranceId;

  /**
   * The player holding {@link #heldUtteranceId}'s speech. Kept, rather than got again, because the
   * player for speech changes with the speech volume setting.
   */
  private volatile @Nullable LowLatencyAudio heldPlayer;

  /** When speech was held, so that the pause is not counted as speaking time. */
  private volatile long heldSinceMs;

  /** How long held speech waits for the pause gesture, as long as saved speech waits for it. */
  private static final long HELD_SPEECH_TIMEOUT_MS = 800;

  private final Runnable dropHeldSpeech = this::dropHeldSpeech;

  /** The text of recent utterances, by ID, to match a resumed utterance with its held speech. */
  private final Map<String, CharSequence> utteranceTexts = recentMap();

  /**
   * How far into its text a resumed utterance's held speech carried on, by utterance ID, for recent
   * utterances. Backtalk resumes with the rest of the text, so word positions in the held speech
   * are reported from there.
   */
  private final Map<String, Integer> resumeOffsets = recentMap();

  /** A map that keeps only the last few entries put in it, since only recent speech can resume. */
  private static <V> Map<String, V> recentMap() {
    return Collections.synchronizedMap(
        new java.util.LinkedHashMap<String, V>(16, 0.75f, false) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
            return size() > 8;
          }
        });
  }

  /**
   * How long an engine with nothing else to do may say nothing at all about new speech before it
   * counts as hung, with low-latency audio on. A hung engine takes speech and never makes it, for
   * every app, until its process is restarted, so Backtalk moves to another engine rather than
   * going silent. This is shorter than {@link LowLatencyAudio#STALL_MS}, so a hung engine is not
   * taken for a low-latency failure. With the setting off, the engine plays its own speech, and
   * Backtalk leaves it be.
   */
  private static final long ENGINE_HANG_MS = 3000;

  /** How long an engine that says it is speaking, perhaps for another app, may say nothing. */
  private static final long ENGINE_BUSY_HANG_MS = 20_000;

  /** When speech went to an engine with nothing else to do, or 0 once the engine responded. */
  private volatile long engineQuietSince;

  /** The utterance that {@link #engineQuietSince} waits on. */
  private volatile @Nullable String engineQuietUtteranceId;

  /** Utterances the engine has taken but not yet reported finishing, stopping or failing. */
  private final Set<String> unfinishedUtterances = ConcurrentHashMap.newKeySet();

  private final Runnable checkEngineHang = this::checkEngineHang;

  /** What each low-latency utterance said, to say it again the usual way if it gives no audio. */
  private final Map<String, Pair<CharSequence, Bundle>> lowLatencyRequests =
      new ConcurrentHashMap<>();

  /**
   * Low-latency utterances being said again the usual way, by ID. The engine can still report on
   * the silent first try, which must not reach Backtalk as the utterance's progress, or Backtalk
   * would take it as done before it is heard. So the second try has its own ID, {@link
   * #RESPOKEN_ID_PREFIX} and the utterance's, and reports on the first try are dropped.
   */
  private final Set<String> respokenUtterances = ConcurrentHashMap.newKeySet();

  private static final String RESPOKEN_ID_PREFIX = "respoken:";
  public static final String PREF_SWITCH_LANGUAGES_KEY = "pref_switch_languages";
  public static final String PREF_SWITCH_DIALECTS_KEY = "pref_switch_dialects";
  private static final boolean SWITCH_LANGUAGES_DEFAULT = true;
  private static final boolean SWITCH_DIALECTS_DEFAULT = true;
  private @Nullable String preferredTtsEngine;

  private final OnSharedPreferenceChangeListener preferenceChangeListener =
      (sharedPrefs, key) -> {
        if (PREF_TTS_ENGINE_KEY.equals(key)) {
          preferredTtsEngine = readPreferredEngine(sharedPrefs);
          updateDefaultEngine();
        } else if (PREF_USE_ACCESSIBILITY_STREAM_KEY.equals(key)
            || PREF_ROUTE_FORCED_KEY.equals(key)) {
          applyAudioAttributes();
        } else if (PREF_SPEAK_IN_PHRASES_KEY.equals(key)) {
          speakInPhrases = sharedPrefs.getBoolean(key, SPEAK_IN_PHRASES_DEFAULT);
        } else if (PREF_LOW_LATENCY_AUDIO_KEY.equals(key)) {
          boolean wasOn = lowLatencyAudio;
          lowLatencyAudio =
              sharedPrefs.getBoolean(
                  key, lowLatencyAudioDefault(FailoverTextToSpeech.this.context));
          if (wasOn && !lowLatencyAudio) {
            turnOffLowLatencyAudio();
          }
        } else if (PREF_SWITCH_LANGUAGES_KEY.equals(key)
            || PREF_SWITCH_DIALECTS_KEY.equals(key)) {
          readLanguageSwitches(sharedPrefs);
        } else if (VoiceProfiles.isProfileKey(key)) {
          readVoiceProfile(sharedPrefs);
        }
      };

  /** The number of time the current TTS has failed consecutively. */
  private int ttsFailures;

  /** The package name of the preferred TTS engine. */
  private String defaultTtsEngine;

  /** The package name of the system TTS engine. */
  private String systemTtsEngine;

  /** A temporary TTS used for switching engines. */
  private @Nullable TextToSpeech tempTts;

  /** The engine loading into the temporary TTS. */
  private @Nullable String tempTtsEngine;

  private int tempTtsGeneration;

  /** The rate adjustment specified in {@link Settings}. */
  private float defaultRate;

  /** The pitch adjustment specified in {@link Settings}. */
  private float defaultPitch;

  private final List<FailoverTtsListener> listeners = new ArrayList<>();

  /** Wake lock for keeping the device unlocked while reading */
  private WakeLock wakeLock;

  private final AudioManager audioManager;
  private final TelephonyManager telephonyManager;

  private boolean shouldHandleTtsCallbackInHandlerThread = true;

  /**
   * A buffer of N most recent utterance ids, used to ensure that a recent utterance's completion
   * handler does not unlock a WakeLock used by the currently speaking utterance.
   */
  private final Deque<String> recentUtteranceIds =
      new ConcurrentLinkedDeque<>(); // may contain nulls

  private final @Nullable SpeechCacheManager speechCacheManager;

  public FailoverTextToSpeech(Context context) {
    this(context, /* enableSpeechCache= */ false);
  }

  /**
   * Constructs a {@link FailoverTextToSpeech} instance.
   *
   * @param context the context to use
   * @param enableSpeechCache whether to enable local speech cache
   */
  public FailoverTextToSpeech(Context context, boolean enableSpeechCache) {
    this.context = context;
    ContextCompat.registerReceiver(
        context, mediaStateMonitor, mediaStateMonitor.getFilter(), RECEIVER_EXPORTED);
    if (enableSpeechCache) {
      HandlerThread handlerThread =
          new HandlerThread("SpeechCacheManager", Process.THREAD_PRIORITY_AUDIO);
      handlerThread.start();
      Looper looper = handlerThread.getLooper();
      speechCacheManager = new SpeechCacheManager(context, looper);
    } else {
      speechCacheManager = null;
    }

    final Uri defaultSynth = Secure.getUriFor(Secure.TTS_DEFAULT_SYNTH);
    final Uri defaultPitch = Secure.getUriFor(Secure.TTS_DEFAULT_PITCH);
    final Uri defaultRate = Secure.getUriFor(Secure.TTS_DEFAULT_RATE);

    resolver = context.getContentResolver();
    resolver.registerContentObserver(defaultSynth, false, mSynthObserver);
    resolver.registerContentObserver(defaultPitch, false, mPitchObserver);
    resolver.registerContentObserver(defaultRate, false, mRateObserver);

    registerGoogleTtsFixCallbacks();

    updateDefaultPitch();
    updateDefaultRate();

    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    preferredTtsEngine = readPreferredEngine(prefs);
    speakInPhrases = prefs.getBoolean(PREF_SPEAK_IN_PHRASES_KEY, SPEAK_IN_PHRASES_DEFAULT);
    lowLatencyAudio = prefs.getBoolean(PREF_LOW_LATENCY_AUDIO_KEY, lowLatencyAudioDefault(context));
    readLanguageSwitches(prefs);
    VoiceProfiles.setActive(VoiceProfiles.readActive(prefs));
    prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

    // Updating the default engine reloads the list of installed engines and
    // the system engine. This also loads the default engine.
    updateDefaultEngine();

    // connect to system services
    initWakeLock(context);
    audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    telephonyManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  protected @Nullable Looper getSpeechCacheManagerLooper() {
    return speechCacheManager == null ? null : speechCacheManager.getLooper();
  }

  /** Separate function for overriding in unit tests, because WakeLock cannot be mocked. */
  protected void initWakeLock(Context context) {
    wakeLock =
        ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
            .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
  }

  /**
   * Adds a new listener for changes in speaking state.
   *
   * @param listener The listener to add.
   */
  public void addListener(FailoverTtsListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes the given listener.
   *
   * @param listener The listener to remove.
   */
  public void removeListener(FailoverTtsListener listener) {
    listeners.remove(listener);
  }

  /**
   * Whether the text-to-speech engine is ready to speak.
   *
   * @return {@code true} if calling {@link #speak} is expected to succeed.
   */
  public boolean isReady() {
    return (tts != null);
  }

  /**
   * Returns the label for the current text-to-speech engine.
   *
   * @return The localized name of the current engine.
   */
  public @Nullable CharSequence getEngineLabel() {
    return TextToSpeechUtils.getLabelForEngine(context, ttsEngine);
  }

  public static List<String> getInstalledTtsEngines(PackageManager pm) {
    List<String> engines = new ArrayList<>();
    TextToSpeechUtils.reloadInstalledTtsEngines(pm, engines);
    return engines;
  }

  public static String getEngineDisplayName(Context context, String enginePackage) {
    CharSequence label = TextToSpeechUtils.getLabelForEngine(context, enginePackage);
    return (label == null) ? enginePackage : label.toString();
  }

  public static boolean shouldUseAccessibilityStream(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    // Only accessibility audio follows a device that Backtalk routes to. While it routes nowhere,
    // as with the phone speaker chosen and nothing else connected, the user's choice stands.
    if (prefs.getBoolean(PREF_ROUTE_FORCED_KEY, false)) {
      return true;
    }
    return prefs.getBoolean(PREF_USE_ACCESSIBILITY_STREAM_KEY, USE_ACCESSIBILITY_STREAM_DEFAULT);
  }

  public static int getSpeechAudioStream(Context context) {
    return shouldUseAccessibilityStream(context)
        ? AudioManager.STREAM_ACCESSIBILITY
        : AudioManager.STREAM_MUSIC;
  }

  public static @Nullable String getSelectedEngine(Context context) {
    String preferred = readPreferredEngine(SharedPreferencesUtils.getSharedPreferences(context));
    return (preferred != null)
        ? preferred
        : Secure.getString(context.getContentResolver(), Secure.TTS_DEFAULT_SYNTH);
  }

  private static @Nullable String readPreferredEngine(SharedPreferences prefs) {
    String engine = prefs.getString(PREF_TTS_ENGINE_KEY, "");
    return TextUtils.isEmpty(engine) ? null : engine;
  }

  /**
   * Returns the {@link TextToSpeech} instance that is currently being used as the engine.
   *
   * @return The engine instance.
   */
  @SuppressWarnings("UnusedDeclaration") // Used by analytics
  public TextToSpeech getEngineInstance() {
    return tts;
  }

  /**
   * Sets whether to handle TTS callback in handler thread. If {@code false}, the callback will be
   * handled in binder thread.
   */
  public void setHandleTtsCallbackInHandlerThread(boolean shouldHandleTtsCallbackInHandlerThread) {
    this.shouldHandleTtsCallbackInHandlerThread = shouldHandleTtsCallbackInHandlerThread;
  }

  /**
   * Speak the specified text.
   *
   * @param text The text to speak.
   * @param locale Language of the text.
   * @param pitch The pitch adjustment, in the range [0 ... 1].
   * @param rate The rate adjustment, in the range [0 ... 1].
   * @param params The parameters to pass to the text-to-speech engine.
   * @param customFlags to adapt information such as performance features.
   * @param flushGlobalTtsQueue Whether to flush the global TTS queue.
   * @param eventId {@link EventTypeId}
   */
  public void speak(
      CharSequence text,
      @Nullable Locale locale,
      float pitch,
      float rate,
      Map<String, String> params,
      Map<String, Integer> customFlags,
      int stream,
      float volume,
      boolean preventDeviceSleep,
      boolean flushGlobalTtsQueue,
      EventId eventId) {
    String utteranceId = params.get(Engine.KEY_PARAM_UTTERANCE_ID);
    addRecentUtteranceId(utteranceId);

    // Handle empty text immediately.
    if (TextUtils.isEmpty(text)) {
      mHandler.onUtteranceCompleted(params.get(Engine.KEY_PARAM_UTTERANCE_ID), /* success= */ true);
      return;
    }

    int result;

    volume *= calculateVolumeAdjustment();

    if (preventDeviceSleep && wakeLock != null && !wakeLock.isHeld()) {
      wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
    }

    Exception failureException = null;
    try {
      result =
          trySpeak(
              text,
              locale,
              pitch,
              rate,
              params,
              customFlags,
              stream,
              volume,
              flushGlobalTtsQueue,
              eventId);
    } catch (Exception e) {
      failureException = e;
      result = TextToSpeech.ERROR;
      allowDeviceSleep();
    }

    if (result == TextToSpeech.ERROR) {
      attemptTtsFailover(ttsEngine);
    }

    if ((result != TextToSpeech.SUCCESS) && params.containsKey(Engine.KEY_PARAM_UTTERANCE_ID)) {
      if (failureException != null) {
        LogUtils.w(TAG, "Failed to speak %s due to an exception", text);
        failureException.printStackTrace();
      } else {
        LogUtils.w(TAG, "Failed to speak %s", text);
      }

      mHandler.onUtteranceCompleted(params.get(Engine.KEY_PARAM_UTTERANCE_ID), /* success= */ true);
    }
  }

  /** Adjust volume if we are in a phone call and speaking with phone audio stream */
  private float calculateVolumeAdjustment() {
    float multiple = 1.0f;

    // Accessibility services will eventually have their own audio stream, making this
    // adjustment unnecessary.
    if (!BuildVersionUtils.isAtLeastN()) {

      // If we are in a phone call...
      // (Phone call state is often reported late, missing the first utterance.)
      if (telephonyManager != null) {
        int callState = telephonyManager.getCallState();
        if (callState != TelephonyManager.CALL_STATE_IDLE) {
          // find audio stream volumes
          if (audioManager != null) {
            int volumeMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volumeMusic <= 0) {
              return 0.0f;
            }
            int volumeVoice = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            int maxVolMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int maxVolVoice = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            float volumeMusicFrac =
                (maxVolMusic <= 0) ? -1.0f : (float) volumeMusic / (float) maxVolMusic;
            float volumeVoiceCallFrac =
                (maxVolVoice <= 0) ? -1.0f : (float) volumeVoice / (float) maxVolVoice;
            // If phone volume is higher than talkback/media volume...
            if (0.0f <= volumeMusicFrac && volumeMusicFrac < volumeVoiceCallFrac) {
              // Reduce effective volume closer to media volume.
              // The UI volume seekbars have an exponential effect on volume,
              // but text-to-speech volume multiple has a linear effect.
              // So take the Nth root of the volume difference to reduce speech
              // volume multiplier exponentially, to match the volume seekbar effect.
              float diff = volumeVoiceCallFrac - volumeMusicFrac;
              float numberDoublingSteps = diff / VOLUME_FRAC_PER_DOUBLING;
              multiple = (float) Math.pow(2.0f, -numberDoublingSteps);
            }
          }
        }
      }
    }
    return multiple;
  }

  /** Releases the {@link WakeLock} */
  private void allowDeviceSleep() {
    allowDeviceSleep(null);
  }

  private void allowDeviceSleep(@Nullable String completedUtteranceId) {
    if (wakeLock != null && wakeLock.isHeld()) {
      boolean isRecent = recentUtteranceIds.contains(completedUtteranceId);
      boolean isLast = Objects.equals(recentUtteranceIds.peekLast(), completedUtteranceId);
      if (completedUtteranceId == null || isLast || !isRecent) {
        try {
          wakeLock.release();
        } catch (RuntimeException unused) {
          // Ignore: already released by timeout.
          // TODO: Log this exception from GoogleLogger.
        }
      }
    }
  }

  private void addRecentUtteranceId(String utteranceId) {
    recentUtteranceIds.add(utteranceId);
    while (recentUtteranceIds.size() > 10) {
      recentUtteranceIds.poll();
    }
  }

  @VisibleForTesting
  public List<String> getRecentUtteranceIds() {
    return Collections.unmodifiableList(new ArrayList<>(recentUtteranceIds));
  }

  /** Stops speech from all applications. No utterance callbacks will be sent. */
  public void stopAll() {
    dropHeldSpeech();
    stopLowLatencySpeech();
    try {
      allowDeviceSleep();
      ensureQueueFlush();
      tts.speak("", SPEECH_FLUSH_ALL, null);
      unfinishedUtterances.clear();
    } catch (Exception e) {
      // Don't care, we're not speaking.
    }
  }

  /** Stops all speech that originated from TalkBack. No utterance callbacks will be sent. */
  public void stopFromTalkBack() {
    if (holdLowLatencySpeech()) {
      // The engine keeps making the held speech, so it can carry on if speech is paused.
      allowDeviceSleep();
      return;
    }
    stopLowLatencySpeech();
    try {
      allowDeviceSleep();
      tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
      unfinishedUtterances.clear();
    } catch (Exception e) {
      // Don't care, we're not speaking.
    }
  }

  /**
   * Unregisters receivers, observers, and shuts down the text-to-speech engine. No calls should be
   * made to this object after calling this method.
   */
  public void shutdown() {
    // The player outlives this engine, so speech it holds or waits for must not block the next.
    releaseLowLatencyAudio();
    allowDeviceSleep();
    context.unregisterReceiver(mediaStateMonitor);
    unregisterGoogleTtsFixCallbacks();
    mHandler.removeCallbacksAndMessages(null);

    resolver.unregisterContentObserver(mSynthObserver);
    resolver.unregisterContentObserver(mPitchObserver);
    resolver.unregisterContentObserver(mRateObserver);

    SharedPreferencesUtils.getSharedPreferences(context)
        .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);

    TextToSpeechUtils.attemptTtsShutdown(tts);
    tts = null;

    TextToSpeechUtils.attemptTtsShutdown(tempTts);
    tempTts = null;
    if (speechCacheManager != null) {
      speechCacheManager.shutDown();
    }
  }

  /**
   * Primes the TTS engine for the selected voice. This is recommended to minimize latency on first
   * synthesis.
   */
  public void primeTtsEngine() {
    if (tts == null) {
      addListener(
          new FailoverTtsListener() {
            @Override
            public void onTtsInitialized(boolean wasSwitchingEngines, String enginePackageName) {
              primeTtsEngineInternal();
            }

            @Override
            public void onUtteranceRangeStarted(String utteranceId, int start, int end) {}

            @Override
            public void onUtteranceCompleted(String utteranceId, boolean success) {}
          });
    } else {
      primeTtsEngineInternal();
    }
  }

  private void primeTtsEngineInternal() {
    if (tts == null) {
      return;
    }
    try {
      File tempFile = File.createTempFile("tmpsynthesize", null, context.getCacheDir());
      tts.synthesizeToFile("1 2 3", null, tempFile, "tmpsynthesize");
      tempFile.deleteOnExit();
    } catch (IOException e) {
      LogUtils.w(TAG, "Exception during TTS init:", e);
    }
  }

  /**
   * Attempts to speak the specified text.
   *
   * @param text to speak, must be under 3999 chars.
   * @param locale language to speak with. Use default language if it's null.
   * @param pitch to speak text in.
   * @param rate to speak text in.
   * @param params to the TTS.
   * @param customFlags to adapt information such as performance features.
   * @param flushGlobalTtsQueue If the global TTS queue should be flushed.
   * @param eventId event id
   * @return The result of speaking the specified text.
   */
  private int trySpeak(
      CharSequence text,
      @Nullable Locale locale,
      float pitch,
      float rate,
      Map<String, String> params,
      Map<String, Integer> customFlags,
      int stream,
      float volume,
      boolean flushGlobalTtsQueue,
      EventId eventId) {
    if (tts == null) {
      return TextToSpeech.ERROR;
    }

    float effectivePitch = (pitch * defaultPitch);
    float effectiveRate;
    if (customFlags.containsKey(RATE_PARAMETER_TYPE)
        && customFlags.get(RATE_PARAMETER_TYPE) == ABSOLUTE) {
      effectiveRate = rate;
    } else {
      effectiveRate = rate * defaultRate;
    }

    requestRate = effectiveRate;
    synchronized (ttsLock) {
      locale = localeToSpeak(locale);
      String utteranceId = params.get(Engine.KEY_PARAM_UTTERANCE_ID);
      boolean isLocaleAttached = locale != null;
      Locale previousLocale = getUsedLocale();
      boolean changeLocale = false;
      int queueMode = flushGlobalTtsQueue ? SPEECH_FLUSH_ALL : QUEUE_ADD;
      SpeechCacheManager speechCacheManager = usableSpeechCache();
      if (isLocaleAttached
          && speechCacheManager != null
          && speechCacheManager.hasCache(
              utteranceId, text, locale, queueMode, effectivePitch, effectiveRate)) {
        LogUtils.d(TAG, "cached speech available, don't need to change locale");
      } else if (isLocaleAttached && !locale.equals(mLastUtteranceLocale)) {
        localeInUse = attemptSetLanguage(locale);
        if (localeInUse != null) {
          mLastUtteranceLocale = locale;
        }
        changeLocale = true;
      } else if (!isLocaleAttached && (mLastUtteranceLocale != null)) {
        localeInUse = ensureSupportedLocale();
        mLastUtteranceLocale = null;
        changeLocale = true;
      }
      int changeLocaleAction = Performance.CHANGE_LOCALE_NONE;
      if (changeLocale) {
        changeLocaleAction = getChangeLocaleAction(previousLocale);
      }
      UtteranceInfoCombo.Builder utteranceInfoCombo =
          UtteranceInfoCombo.builder(text, localeInUse, isLocaleAttached, changeLocaleAction)
              .setFlushGlobalTtsQueue(flushGlobalTtsQueue);

      if ((text instanceof Spannable spannable)) {
        IdentifierSpan[] identifierSpans =
            spannable.getSpans(0, text.length(), IdentifierSpan.class);
        if (identifierSpans.length > 0) {
          utteranceInfoCombo.setIsSeparatorInUtterance(true);
          for (IdentifierSpan identifierSpan : identifierSpans) {
            spannable.removeSpan(identifierSpan);
          }
        }
      }

      if (customFlags.get(AGGRESSIVE_CHUNK) != null
          && customFlags.get(AGGRESSIVE_CHUNK) == VALUE_ON) {
        utteranceInfoCombo.setIsAggressiveChunking(true);
      }
      UtteranceInfoCombo utteranceInfo = utteranceInfoCombo.build();
      for (FailoverTtsListener mListener : listeners) {
        mListener.onBeforeUtteranceRequested(utteranceId, utteranceInfo);
      }
      int result =
          speak(
              text,
              locale,
              params,
              utteranceId,
              effectivePitch,
              effectiveRate,
              stream,
              volume,
              utteranceInfo,
              eventId);

      if (result != TextToSpeech.SUCCESS) {
        localeInUse = ensureSupportedLocale();
      }

      LogUtils.d(TAG, "Speak call for %s returned %d", utteranceId, result);
      return result;
    }
  }

  @ChangeLocaleAction
  private int getChangeLocaleAction(Locale previousLocale) {
    if (Objects.equals(localeInUse, previousLocale)) {
      return Performance.CHANGE_LOCALE_SAME;
    }
    if (localeInUse == null) {
      return Performance.CHANGE_LOCALE_UNDEFINED;
    }
    if (previousLocale != null
        && Objects.equals(previousLocale.getLanguage(), localeInUse.getLanguage())) {
      return Performance.CHANGE_LOCALE_SAME_LANGUAGE;
    }
    return Performance.CHANGE_LOCALE_DIFFERENT;
  }

  private int speak(
      CharSequence text,
      Locale locale,
      Map<String, String> params,
      String utteranceId,
      float pitch,
      float rate,
      int stream,
      float volume,
      UtteranceInfoCombo utteranceInfoCombo,
      EventId eventId) {
    Bundle bundle = new Bundle();

    if (params != null) {
      for (String key : params.keySet()) {
        bundle.putString(key, params.get(key));
      }
    }

    bundle.putInt(SpeechParam.PITCH, (int) (pitch * 100));
    bundle.putInt(SpeechParam.RATE, (int) (rate * 100));
    bundle.putInt(Engine.KEY_PARAM_STREAM, stream);
    bundle.putFloat(SpeechParam.VOLUME, volume);
    // When the language in use in not available, TTS will use en-us as the fallback locale.
    bundle.putString(SpeechParam.FALLBACK_LOCALE, "true");

    ensureQueueFlush();

    int queueMode =
        utteranceInfoCombo.flushGlobalTtsQueue() ? SPEECH_FLUSH_ALL : TextToSpeech.QUEUE_ADD;

    // Track latency from event received to feedback queued.
    if (eventId != null && utteranceId != null) {
      Performance.getInstance().onFeedbackQueued(eventId, utteranceId, utteranceInfoCombo);
    }

    if (locale == null) {
      locale = getUsedLocale();
    }

    return speakWithCacheOrTts(utteranceId, text, queueMode, locale, bundle);
  }

  private static void readLanguageSwitches(SharedPreferences prefs) {
    LanguageSwitch.setSwitches(
        prefs.getBoolean(PREF_SWITCH_LANGUAGES_KEY, SWITCH_LANGUAGES_DEFAULT),
        prefs.getBoolean(PREF_SWITCH_DIALECTS_KEY, SWITCH_DIALECTS_DEFAULT));
  }

  /** Returns the language to speak text marked as {@code locale} in, by the language switches. */
  private static @Nullable Locale localeToSpeak(@Nullable Locale locale) {
    Locale toSpeak = LanguageSwitch.localeToSpeak(locale);
    if (!Objects.equals(toSpeak, locale)) {
      LogUtils.v(TAG, "Speaking text marked as %s in %s", locale, toSpeak);
    }
    return toSpeak;
  }

  /** Tells LanguageSwitch the language attemptRestorePreferredLocale speaks unmarked text in. */
  private void updateVoiceLanguage() {
    LanguageSwitch.setVoiceLanguage(mDefaultLocale != null ? mDefaultLocale : mSystemLocale);
  }

  /** The current engine's voices, for {@link #findVoice}, or null until they're needed. */
  private @Nullable Set<Voice> engineVoices;

  /**
   * A voice {@link #findVoice} didn't find even after listing the voices again, so that it isn't
   * listed again for every utterance. Cleared with {@link #engineVoices}, when the engine changes.
   */
  private @Nullable String missingVoice;

  /** Whether long text is sent a sentence at a time, by the voice profile in use if any. */
  private boolean speaksInPhrases() {
    VoiceProfile profile = VoiceProfiles.active();
    return profile != null ? profile.phrases() : speakInPhrases;
  }

  /**
   * Returns the speech cache, or null if there is none or a voice profile is in use. The cache keeps
   * speech by language, not by voice, so it could play speech made in another voice.
   */
  private @Nullable SpeechCacheManager usableSpeechCache() {
    return VoiceProfiles.isProfileActive() ? null : speechCacheManager;
  }

  /**
   * Reads the voice profile in use, and moves to its engine and voice if they changed. Changes to
   * its volume, rate and pitch are applied by the speech controller.
   */
  private void readVoiceProfile(SharedPreferences prefs) {
    VoiceProfile previous = VoiceProfiles.active();
    VoiceProfile profile = VoiceProfiles.readActive(prefs);
    VoiceProfiles.setActive(profile);
    if (profile == null ? previous == null : profile.sameVoiceAs(previous)) {
      return;
    }
    LogUtils.i(TAG, "Voice profile: %s", profile == null ? "default" : profile.name());
    if (profile != null && speechCacheManager != null) {
      // Speech from the cache was made in the voice in use until now, and speech queued behind it
      // would set its language over the profile's voice.
      notifyInterruptedForSuspendQueue();
      speechCacheManager.stopSpeaking();
    }
    cancelEngineSwitchAwayFrom(getTargetEngine());
    // A new engine sets its voice once it starts.
    updateDefaultEngine();
    if (tts != null && tempTts == null) {
      synchronized (ttsLock) {
        cachedTtsLocale = null;
        mLastUtteranceLocale = null;
        missingVoice = null;
        if (profile == null) {
          // Backtalk default sets only a language, which can fail, so start from the engine's own
          // voice rather than keep the profile's.
          try {
            Voice engineDefault = tts.getDefaultVoice();
            if (engineDefault != null) {
              tts.setVoice(engineDefault);
            }
          } catch (RuntimeException e) {
            // The engine may have stopped.
            LogUtils.e(TAG, "Failed to reset the voice: %s", e.toString());
          }
        }
      }
      localeInUse = ensureSupportedLocale();
    }
  }

  /**
   * Sets the voice of a voice profile: its voice, else its language's default voice, else the
   * engine's default voice.
   *
   * @return the voice's language, or null if no voice could be set
   */
  private @Nullable Locale attemptSetProfileVoice(VoiceProfile profile) {
    if (tts == null) {
      return null;
    }
    synchronized (ttsLock) {
      mLastUtteranceLocale = null;
      cachedTtsLocale = null;
      try {
        // Each step is tried in turn, since a voice the engine lists can still fail to be set,
        // and the engine would otherwise keep the voice it had, such as another profile's.
        Voice voice = findVoice(profile.voice());
        if (voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS) {
          LogUtils.i(TAG, "Voice profile voice: %s", voice.getName());
          cachedTtsLocale = voice.getLocale();
          return cachedTtsLocale;
        }
        if (!profile.language().isEmpty()) {
          Locale language = Locale.forLanguageTag(profile.language());
          if (!isNotAvailableStatus(tts.setLanguage(language))) {
            LogUtils.i(TAG, "Voice profile language: %s", language);
            cachedTtsLocale = language;
            return language;
          }
          LogUtils.w(TAG, "Voice profile language %s is not available", language);
        }
        Voice engineDefault = tts.getDefaultVoice();
        if (engineDefault != null && tts.setVoice(engineDefault) == TextToSpeech.SUCCESS) {
          LogUtils.i(TAG, "Voice profile voice: engine default %s", engineDefault.getName());
          cachedTtsLocale = engineDefault.getLocale();
          return cachedTtsLocale;
        }
      } catch (RuntimeException e) {
        // The engine may have stopped.
        LogUtils.e(TAG, "Failed to set the voice profile's voice: %s", e.toString());
      }
    }
    LogUtils.e(TAG, "No voice for voice profile %s", profile.name());
    return null;
  }

  /**
   * Returns the current engine's installed voice called {@code name}, or null if it has none. The
   * engine's voices are kept from the last time they were needed, as listing them is slow, and
   * listed again only if the voice isn't there, as it may have been installed since. A voice still
   * missing after that isn't looked for again until the engine changes. Call with {@link
   * #ttsLock}.
   */
  private @Nullable Voice findVoice(String name) {
    if (name.isEmpty() || name.equals(missingVoice)) {
      return null;
    }
    boolean listed = false;
    if (engineVoices == null) {
      engineVoices = tts.getVoices();
      listed = true;
    }
    Voice voice = findVoice(engineVoices, name);
    if (voice == null && !listed) {
      engineVoices = tts.getVoices();
      voice = findVoice(engineVoices, name);
    }
    if (voice == null) {
      LogUtils.w(TAG, "Voice %s is not installed", name);
      missingVoice = name;
    }
    return voice;
  }

  /** Returns the voice called {@code name} in {@code voices}, unless it needs downloading. */
  private static @Nullable Voice findVoice(@Nullable Set<Voice> voices, String name) {
    if (voices != null) {
      for (Voice voice : voices) {
        if (voice != null && name.equals(voice.getName())) {
          Set<String> features = voice.getFeatures();
          boolean installed =
              features == null
                  || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
          return installed ? voice : null;
        }
      }
    }
    return null;
  }

  private Locale getUsedLocale() {
    synchronized (ttsLock) {
      return cachedTtsLocale;
    }
  }

  private final PlaybackStateListener playbackStateListener =
      new PlaybackStateListener() {
        @Override
        public void onStateChanged(@Nullable String utteranceId, int state) {
          if ((state == SpeechCachePlayer.STATE_START)) {
            utteranceProgressCallback.onStart(utteranceId);
          } else if (state == SpeechCachePlayer.STATE_READY) {
            utteranceProgressCallback.onAudioCacheAvailable(utteranceId);
          } else if (state == SpeechCachePlayer.STATE_COMPLETED) {
            // Consume the speeches with queue_MODE_ADD.
            List<SpeakRequest> suspendQueueCopy = new ArrayList<>(suspendQueue);
            suspendQueue.clear();
            for (SpeakRequest speakRequest : suspendQueueCopy) {
              LogUtils.d(TAG, "consume suspend queue speakRequest= %s: ", speakRequest);
              mHandler.post(
                  () -> {
                    if (isReady()) {
                      attemptSetLanguage(speakRequest.locale);
                      ttsSpeak(
                          speakRequest.text,
                          speakRequest.queueMode,
                          speakRequest.bundle,
                          utteranceId);
                    }
                  });
            }
            mHandler.onUtteranceCompleted(utteranceId, /* success= */ true);
          } else if (state == SpeechCachePlayer.STATE_STOPPED) {
            mHandler.onUtteranceCompleted(utteranceId, /* success= */ false);
          }
        }
      };

  private final List<SpeakRequest> suspendQueue = Collections.synchronizedList(new ArrayList<>());

  /**
   * Speaks the text with cache if possible, otherwiase fallback to {@link
   * TextToSpeech#speak(CharSequence, int, Bundle, String)}} and drop the suspend queue if needed.
   * If the queue mode is {@link TextToSpeech#QUEUE_ADD} and the cache is speaking, the speech will
   * be added to the {@link #suspendQueue} and speaks until the cache is spoken completed.
   *
   * @param utteranceId the id to know status of the utterance when the callback is called.
   * @param text the text to speak
   * @param queueMode The queue mode to use.
   * @param locale the locale to use
   * @param bundle The bundle to use.
   * @return The result of speaking the text.
   */
  private int speakWithCacheOrTts(
      String utteranceId, CharSequence text, int queueMode, Locale locale, Bundle bundle) {
    if (heldUtteranceId != null) {
      if (resumeHeldSpeech(utteranceId, text)) {
        return TextToSpeech.SUCCESS;
      }
      // New speech ends a pause.
      dropHeldSpeech();
    }
    if (utteranceId != null) {
      utteranceTexts.put(utteranceId, text);
    }
    SpeechCacheManager speechCacheManager = usableSpeechCache();
    if (speechCacheManager == null) {
      return speakInChunks(text, queueMode, bundle, utteranceId, locale);
    }

    if (speechCacheManager.isSpeaking()) {
      if (queueMode == TextToSpeech.QUEUE_ADD) {
        LogUtils.d(TAG, "speakWithCacheOrTts -- speaking cache, play %s later", utteranceId);
        SpeakRequest speakRequest = new SpeakRequest(locale, text, queueMode, bundle, utteranceId);
        suspendQueue.add(speakRequest);
        return TextToSpeech.SUCCESS;
      } else {
        LogUtils.d(
            TAG,
            "speaking cache, speak %s now and clean queue size %s: ",
            utteranceId,
            suspendQueue.size());
        notifyInterruptedForSuspendQueue();
        // speak next speech immediately
        if (speechCacheManager.speakWithSpeechCachePlayer(
            utteranceId, queueMode, text, bundle, playbackStateListener, locale)) {
          tts.speak("", SPEECH_FLUSH_ALL, null);
          return TextToSpeech.SUCCESS;
        }
        LogUtils.d(TAG, "tts.speak, utteranceId =" + utteranceId);
        return speakInChunks(text, queueMode, bundle, utteranceId, locale);
      }
    } else {
      if (speechCacheManager.speakWithSpeechCachePlayer(
          utteranceId, queueMode, text, bundle, playbackStateListener, locale)) {
        tts.speak("", SPEECH_FLUSH_ALL, null);
        return TextToSpeech.SUCCESS;
      }
      LogUtils.d(TAG, "tts.speak, utteranceId =" + utteranceId);
      return speakInChunks(text, queueMode, bundle, utteranceId, locale);
    }
  }

  /** Marks the utterance ID of one piece of a split utterance. */
  private static final String CHUNK_ID_SEPARATOR = "#chunk";

  /** One queued piece of a split utterance, by its own utterance ID. */
  private record SpeechChunk(String utteranceId, int offset, boolean first, boolean last) {}

  private final Map<String, SpeechChunk> speechChunks = new ConcurrentHashMap<>();

  /** Estimates progress through speech from engines that report no word positions. */
  private final SpeechProgressEstimator progressEstimator = new SpeechProgressEstimator();

  /** The rate of the speech being sent, for {@link #progressEstimator}. */
  private volatile float requestRate = 1f;

  /**
   * Reports an estimate of how far the speaking utterance has got, as a word range, so that paused
   * speech resumes near where it stopped. Does nothing when the engine reports word positions
   * itself, or the speech was sent in pieces, which report their own progress. Call this on the
   * thread that handles utterance callbacks, just before saving the speech to resume.
   */
  public void reportEstimatedProgress() {
    String utteranceId = progressEstimator.currentUtteranceId();
    int offset = progressEstimator.estimateResumeOffset(SystemClock.uptimeMillis());
    // Held speech that resumed is reported from where it resumed, not the start of its text.
    @Nullable Integer resumedAt = utteranceId == null ? null : resumeOffsets.get(utteranceId);
    if (resumedAt != null) {
      offset -= resumedAt;
    }
    if (utteranceId != null && offset > 0) {
      handleUtteranceRangeStarted(utteranceId, offset, offset);
    }
  }

  /**
   * Speaks long text as pieces of a sentence or phrase, so a later utterance can interrupt it
   * quickly. See {@link SpeechChunker}. The pieces report progress as the one original utterance:
   * see {@link #toOriginal}. The {@link #PREF_SPEAK_IN_PHRASES_KEY} setting turns this off.
   */
  private int speakInChunks(
      CharSequence text, int queueMode, Bundle bundle, String utteranceId, Locale locale) {
    List<Integer> starts =
        utteranceId == null || !speaksInPhrases()
            ? Collections.singletonList(0)
            : SpeechChunker.chunkStarts(text, locale == null ? Locale.getDefault() : locale);
    if (starts.size() <= 1) {
      if (utteranceId != null) {
        progressEstimator.onQueued(utteranceId, text, requestRate, ttsEngine);
      }
      return ttsSpeak(text, queueMode, bundle, utteranceId);
    }
    for (int i = 0; i < starts.size(); i++) {
      int start = starts.get(i);
      boolean last = i == starts.size() - 1;
      int end = last ? text.length() : starts.get(i + 1);
      String chunkId = utteranceId + CHUNK_ID_SEPARATOR + i;
      speechChunks.put(chunkId, new SpeechChunk(utteranceId, start, i == 0, last));
      int result =
          ttsSpeak(text.subSequence(start, end), i == 0 ? queueMode : QUEUE_ADD, bundle, chunkId);
      if (result != TextToSpeech.SUCCESS) {
        speechChunks.remove(chunkId);
        if (i == 0) {
          return result;
        }
        // Finish the utterance with the pieces that were queued.
        String previousId = utteranceId + CHUNK_ID_SEPARATOR + (i - 1);
        SpeechChunk previous = speechChunks.get(previousId);
        if (previous != null) {
          speechChunks.put(
              previousId, new SpeechChunk(utteranceId, previous.offset(), previous.first(), true));
        }
        break;
      }
    }
    LogUtils.d(TAG, "speakInChunks: %s in %d pieces", utteranceId, starts.size());
    return TextToSpeech.SUCCESS;
  }

  /**
   * Speaks text with the engine, through {@link LowLatencyAudio} if the setting is on and it can.
   * Then the engine synthesizes the speech and Backtalk plays it, so that it reaches the speaker
   * sooner than the engine's own playback.
   */
  private int ttsSpeak(
      CharSequence text, int queueMode, Bundle bundle, @Nullable String utteranceId) {
    @Nullable LowLatencyAudio player = lowLatencyPlayer();
    if (player == null || utteranceId == null || TextUtils.isEmpty(text)) {
      return tts.speak(text, queueMode, bundle, utteranceId);
    }
    watchForEngineHang(queueMode, utteranceId);
    if (queueMode != QUEUE_ADD) {
      // The engine flushes its own queue for this queue mode. Every player, since the player for
      // speech changes with the speech volume setting.
      LowLatencyAudio.stopAllStreams();
    }
    LowLatencyAudio.SpeechStream stream =
        player.openStream(
            utteranceId,
            floatParam(bundle, SpeechParam.VOLUME, 1f),
            floatParam(bundle, Engine.KEY_PARAM_PAN, 0f),
            lowLatencyListener);
    lowLatencyStreams.put(utteranceId, stream);
    lowLatencyRequests.put(utteranceId, Pair.create(text, bundle));
    // The engine speaks as usual but at no volume, and Backtalk plays the copy of its audio that
    // Android gives it. As the engine is still playing audio, Android and phone makers' battery
    // savers keep it running, which they do not do for an engine only making audio for an app.
    Bundle silent = new Bundle(bundle);
    silent.putFloat(SpeechParam.VOLUME, 0f);
    int result = tts.speak(text, queueMode, silent, utteranceId);
    if (result != TextToSpeech.SUCCESS) {
      stream.discard();
      lowLatencyStreams.remove(utteranceId);
      lowLatencyRequests.remove(utteranceId);
      return tts.speak(text, queueMode, bundle, utteranceId);
    }
    return result;
  }

  /** Whether a phone or voice call has the audio mode. */
  private boolean inCall() {
    AudioManager audioManager = context.getSystemService(AudioManager.class);
    if (audioManager == null) {
      return false;
    }
    int mode = audioManager.getMode();
    return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION;
  }

  /** The player for speech, or null if speech should play the usual way. */
  private @Nullable LowLatencyAudio lowLatencyPlayer() {
    if (!lowLatencyAudio
        || LowLatencyAudio.isMicrophoneInUse()
        || inCall()
        || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R
        || (ttsEngine != null && enginesWithoutFileAudio.contains(ttsEngine))) {
      return null;
    }
    return LowLatencyAudio.get(context, speechAttributes());
  }

  private AudioAttributes speechAttributes() {
    return new AudioAttributes.Builder()
        .setUsage(
            shouldUseAccessibilityStream(context)
                ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                : AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build();
  }

  /** Reads a number parameter, which can be a float or a string. */
  private static float floatParam(Bundle bundle, String key, float defaultValue) {
    Object value = bundle.get(key);
    if (value instanceof Number number) {
      return number.floatValue();
    }
    if (value instanceof String string) {
      try {
        return Float.parseFloat(string);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * Holds the low-latency speech playing now, and returns whether there was any. Speech that is
   * held already stays held.
   */
  private boolean holdLowLatencySpeech() {
    if (heldUtteranceId != null) {
      return true;
    }
    if (!lowLatencyAudio || (lowLatencyStreams.isEmpty() && lowLatencyRequests.isEmpty())) {
      return false;
    }
    // Whichever player has the speech, since the player for speech changes with the speech volume
    // setting. Got without making a player.
    @Nullable LowLatencyAudio player = null;
    @Nullable String head = null;
    for (LowLatencyAudio candidate : LowLatencyAudio.players()) {
      head = candidate.hold();
      if (head != null) {
        player = candidate;
        break;
      }
    }
    if (player == null || head == null) {
      return false;
    }
    heldPlayer = player;
    heldSinceMs = SystemClock.uptimeMillis();
    heldUtteranceId = parentUtteranceId(head);
    mHandler.removeCallbacks(dropHeldSpeech);
    mHandler.postDelayed(dropHeldSpeech, HELD_SPEECH_TIMEOUT_MS);
    LogUtils.d(TAG, "Holding speech of %s", heldUtteranceId);
    return true;
  }

  /**
   * Keeps held speech held until speech resumes, for the pause gesture, and returns whether there is
   * any.
   */
  public boolean keepHeldSpeech() {
    mHandler.removeCallbacks(dropHeldSpeech);
    return heldUtteranceId != null;
  }

  /**
   * Carries on held speech from exactly where it stopped, if {@code utteranceId} is the held
   * utterance and {@code text} is the rest of its text, as Backtalk resumes it. Returns whether it
   * did.
   */
  private boolean resumeHeldSpeech(@Nullable String utteranceId, CharSequence text) {
    @Nullable String held = heldUtteranceId;
    @Nullable CharSequence fullText = held == null ? null : utteranceTexts.get(held);
    if (held == null
        || !held.equals(utteranceId)
        || fullText == null
        || !fullText.toString().endsWith(text.toString())) {
      return false;
    }
    int offset = fullText.length() - text.length();
    // Word positions are reported from the start of the rest of the text, which Backtalk now holds.
    // Pieces are already moved by the offset of any earlier resume.
    @Nullable Integer earlier = resumeOffsets.put(held, offset);
    int moved = offset - (earlier == null ? 0 : earlier);
    for (Map.Entry<String, SpeechChunk> entry : speechChunks.entrySet()) {
      SpeechChunk chunk = entry.getValue();
      if (held.equals(chunk.utteranceId())) {
        entry.setValue(
            new SpeechChunk(held, chunk.offset() - moved, chunk.first(), chunk.last()));
      }
    }
    // The pause is not speaking time, for the speed learned from this utterance.
    progressEstimator.onPaused(held, SystemClock.uptimeMillis() - heldSinceMs);
    mHandler.removeCallbacks(dropHeldSpeech);
    heldUtteranceId = null;
    @Nullable LowLatencyAudio player = heldPlayer;
    heldPlayer = null;
    if (player != null) {
      player.resume();
    }
    LogUtils.d(TAG, "Resuming held speech of %s from %d", held, offset);
    return true;
  }

  /** Drops held speech, and stops the engine making the rest of it. */
  private void dropHeldSpeech() {
    mHandler.removeCallbacks(dropHeldSpeech);
    if (heldUtteranceId == null) {
      return;
    }
    LogUtils.d(TAG, "Dropping held speech of %s", heldUtteranceId);
    heldUtteranceId = null;
    @Nullable LowLatencyAudio player = heldPlayer;
    heldPlayer = null;
    if (player != null) {
      // Stopping its streams ends the hold, even if the player for speech has changed since.
      player.stopStreams();
    }
    stopLowLatencySpeech();
    try {
      // Stopping rather than speaking nothing, which would queue ahead of new speech.
      tts.stop();
      unfinishedUtterances.clear();
    } catch (Exception e) {
      // Not speaking.
    }
  }

  private static String parentUtteranceId(String utteranceId) {
    int separator = utteranceId.indexOf(CHUNK_ID_SEPARATOR);
    return separator < 0 ? utteranceId : utteranceId.substring(0, separator);
  }

  /**
   * Speaks the usual way from now on, rather than through {@link LowLatencyAudio}, until the
   * setting is read again.
   */
  public void speakWithoutLowLatencyAudio() {
    if (lowLatencyAudio) {
      lowLatencyAudio = false;
      turnOffLowLatencyAudio();
    }
  }

  /** Stops the speech playing through {@link LowLatencyAudio}. */
  private void stopLowLatencySpeech() {
    // Speech the engine has finished making may still be playing, until it reports finishing.
    // Every player, since the player for speech changes with the speech volume setting.
    if (!lowLatencyStreams.isEmpty() || !lowLatencyRequests.isEmpty()) {
      LowLatencyAudio.stopAllStreams();
    }
  }

  /**
   * Stops low-latency speech and frees the players, for when the setting is turned off. Speech
   * playing reports that it stopped. Its streams stay in {@link #lowLatencyStreams}, finished, so
   * that the engine's own callbacks for it are ignored rather than reported a second time.
   */
  private void turnOffLowLatencyAudio() {
    dropHeldSpeech();
    mHandler.removeCallbacks(checkEngineHang);
    engineQuietSince = 0;
    unfinishedUtterances.clear();
    boolean speaking = !lowLatencyStreams.isEmpty();
    LowLatencyAudio.shutdownAll();
    if (speaking && tts != null) {
      try {
        // The engine is still making that speech, at no volume.
        tts.stop();
      } catch (Exception e) {
        // Not speaking.
      }
    }
  }

  /**
   * Stops all low-latency speech and frees the players, for when the engine goes away. Speech that
   * was waiting on the engine must not block the next.
   */
  private void releaseLowLatencyAudio() {
    mHandler.removeCallbacks(dropHeldSpeech);
    heldUtteranceId = null;
    heldPlayer = null;
    lowLatencyStreams.clear();
    lowLatencyRequests.clear();
    respokenUtterances.clear();
    mHandler.removeCallbacks(checkEngineHang);
    engineQuietSince = 0;
    unfinishedUtterances.clear();
    LowLatencyAudio.shutdownAll();
  }

  /** How many utterances in a row have ended without the engine giving them audio. */
  private int noAudioInARow = 0;

  /** How many utterances in a row without audio mean that the engine plays its own audio. */
  private static final int NO_AUDIO_IN_A_ROW_LIMIT = 3;

  /** Reports the progress of low-latency speech as it plays, as the engine would. */
  private final LowLatencyAudio.StreamListener lowLatencyListener =
      new LowLatencyAudio.StreamListener() {
        @Override
        public void onStarted(String id) {
          noAudioInARow = 0;
          utteranceProgressCallback.onStart(id);
        }

        @Override
        public void onRange(String id, int start, int end) {
          // Pieces of a split utterance have their starts moved instead.
          @Nullable Integer offset = id.contains(CHUNK_ID_SEPARATOR) ? null : resumeOffsets.get(id);
          if (offset != null) {
            start -= offset;
            end -= offset;
            if (end < 0) {
              return;
            }
          }
          utteranceProgressCallback.onRangeStart(id, start, end, /* frame= */ 0);
        }

        @Override
        public void onFinished(String id, boolean completed) {
          lowLatencyRequests.remove(id);
          if (completed) {
            utteranceProgressCallback.onDone(id);
          } else {
            utteranceProgressCallback.onStop(id, /* interrupted= */ true);
          }
        }

        @Override
        public void onSilent(String id) {
          // The engine applied the zero volume to its audio itself. Say it the usual way, and
          // speak the usual way with this engine from now on.
          mHandler.post(
              () -> {
                LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.get(id);
                speakAgainWithoutLowLatency(id, stream, /* engineAtFault= */ true);
              });
        }

        @Override
        public void onHoldFull(String id) {
          // The pause went on too long to keep all the speech. Resuming says it the usual way.
          mHandler.post(FailoverTextToSpeech.this::dropHeldSpeech);
        }

        @Override
        public void onStalled(String id) {
          // Say it the usual way, and speak the usual way with this engine from now on.
          mHandler.post(
              () -> speakAgainWithoutLowLatency(id, /* stream= */ null, /* engineAtFault= */ true));
        }

        @Override
        public void onPlayerFailed(String id) {
          // Say it the usual way. The player is not used again until it is made again, but the
          // engine is not at fault, so a new player may use low latency with it.
          mHandler.post(
              () ->
                  speakAgainWithoutLowLatency(id, /* stream= */ null, /* engineAtFault= */ false));
        }

        @Override
        public void onNoAudio(String id) {
          // The engine may play its own audio rather than give it to Backtalk, so it was heard
          // already. But an utterance with nothing to say, such as a pause, has no audio either, so
          // only several in a row mean that the engine plays its own. Then speak the usual way
          // with this engine from now on.
          lowLatencyRequests.remove(id);
          noAudioInARow++;
          if (noAudioInARow >= NO_AUDIO_IN_A_ROW_LIMIT) {
            stopUsingLowLatencyAudio("no audio " + noAudioInARow + " times in a row");
          }
          utteranceProgressCallback.onDone(id);
        }
      };

  /**
   * Starts waiting for the engine to respond to {@code utteranceId}, if the engine has nothing else
   * to do first, so it should respond at once.
   */
  private void watchForEngineHang(int queueMode, @Nullable String utteranceId) {
    if (utteranceId == null || !lowLatencyAudio) {
      return;
    }
    boolean idle = queueMode != QUEUE_ADD || unfinishedUtterances.isEmpty();
    if (queueMode != QUEUE_ADD) {
      // The engine may never report on the speech this flushes.
      unfinishedUtterances.clear();
    }
    unfinishedUtterances.add(utteranceId);
    if (idle && engineQuietSince == 0) {
      engineQuietUtteranceId = utteranceId;
      engineQuietSince = SystemClock.uptimeMillis();
      mHandler.removeCallbacks(checkEngineHang);
      mHandler.postDelayed(checkEngineHang, ENGINE_HANG_MS);
    }
  }

  /** Whether the engine says it is speaking, for any app. A hung engine says it is not. */
  private boolean isEngineSpeaking() {
    try {
      return tts.isSpeaking();
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** Notes that the engine said something about {@code utteranceId}, so it is not hung. */
  private void engineResponded(String utteranceId, boolean finished) {
    engineQuietSince = 0;
    if (finished) {
      unfinishedUtterances.remove(utteranceId);
    }
  }

  /** Moves to another engine if the engine has said nothing about new speech in time. */
  private void checkEngineHang() {
    long since = engineQuietSince;
    if (since == 0 || tts == null || !lowLatencyAudio) {
      engineQuietSince = 0;
      return;
    }
    long quiet = SystemClock.uptimeMillis() - since;
    if (quiet < ENGINE_HANG_MS) {
      mHandler.postDelayed(checkEngineHang, ENGINE_HANG_MS - quiet);
      return;
    }
    if (quiet < ENGINE_BUSY_HANG_MS && isEngineSpeaking()) {
      // Another app's speech with the same engine comes first.
      mHandler.postDelayed(checkEngineHang, ENGINE_HANG_MS);
      return;
    }
    @Nullable String hungUtteranceId = engineQuietUtteranceId;
    LogUtils.e(TAG, "TTS engine %s said nothing for %d ms, so it is hung", ttsEngine, quiet);
    engineQuietSince = 0;
    unfinishedUtterances.clear();
    boolean lowLatency = hungUtteranceId != null && lowLatencyRequests.containsKey(hungUtteranceId);
    // Speech waiting on the hung engine stops, so that it holds up nothing.
    mHandler.removeCallbacks(dropHeldSpeech);
    heldUtteranceId = null;
    heldPlayer = null;
    lowLatencyStreams.clear();
    LowLatencyAudio.stopAllStreams();
    lowLatencyRequests.clear();
    respokenUtterances.clear();
    // Restarting the same engine does not help, since its process stays hung.
    ttsFailures = Math.max(ttsFailures, MAX_TTS_FAILURES - 1);
    attemptTtsFailover(ttsEngine);
    if (hungUtteranceId != null && !lowLatency) {
      utteranceProgressCallback.onError(hungUtteranceId);
    }
  }

  /** Speaks the usual way with the current engine from now on. */
  private void stopUsingLowLatencyAudio(String reason) {
    if (ttsEngine != null) {
      enginesWithoutFileAudio.add(ttsEngine);
    }
    // Logged at error level, so that it shows at the default log level: speech slows down from
    // here on, and the reason is needed to tell why.
    LogUtils.e(TAG, "Low-latency playback off for %s: %s", ttsEngine, reason);
  }

  /**
   * Says a low-latency utterance that failed again the usual way. If {@code engineAtFault}, the
   * engine speaks the usual way from now on.
   */
  private void speakAgainWithoutLowLatency(
      String utteranceId, LowLatencyAudio.@Nullable SpeechStream stream, boolean engineAtFault) {
    // A stream that stalled has left its player already.
    if (stream != null) {
      stream.discard();
    }
    lowLatencyStreams.remove(utteranceId);
    if (engineAtFault) {
      stopUsingLowLatencyAudio(stream == null ? "no audio in time" : "engine error");
    } else {
      LogUtils.e(TAG, "Low-latency player failed, so %s is said the usual way", utteranceId);
    }
    @Nullable Pair<CharSequence, Bundle> request = lowLatencyRequests.remove(utteranceId);
    if (request == null || tts == null) {
      utteranceProgressCallback.onError(utteranceId);
      return;
    }
    respokenUtterances.add(utteranceId);
    mHandler.post(
        () -> {
          if (tts == null) {
            respokenUtterances.remove(utteranceId);
            utteranceProgressCallback.onError(utteranceId);
            return;
          }
          // The silent first try may still be in the engine, which would hold up the second try
          // for as long as it takes. Stop it, unless other speech waits in the engine too, which
          // stopping would lose.
          boolean onlyThis =
              unfinishedUtterances.isEmpty()
                  || (unfinishedUtterances.size() == 1
                      && unfinishedUtterances.contains(utteranceId));
          if (onlyThis) {
            tts.stop();
          }
          tts.speak(request.first, QUEUE_ADD, request.second, RESPOKEN_ID_PREFIX + utteranceId);
        });
  }

  /** Whether {@code utteranceId} is the silent first try of an utterance said again. */
  private boolean isRespokenFirstTry(@Nullable String utteranceId) {
    return utteranceId != null && respokenUtterances.contains(utteranceId);
  }

  /**
   * Returns the ID to report the engine's callback for {@code utteranceId} under: the utterance's
   * own ID for its second try, said the usual way. See {@link #respokenUtterances}.
   */
  private @Nullable String reportedId(@Nullable String utteranceId, boolean finished) {
    if (utteranceId == null || !utteranceId.startsWith(RESPOKEN_ID_PREFIX)) {
      return utteranceId;
    }
    String original = utteranceId.substring(RESPOKEN_ID_PREFIX.length());
    if (finished) {
      respokenUtterances.remove(original);
    }
    return original;
  }

  /**
   * Sends the engine's callbacks for low-latency utterances to their streams, and the rest to
   * {@link #utteranceProgressCallback}.
   */
  private final UtteranceProgressListener lowLatencyRouter =
      new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
          engineResponded(utteranceId, /* finished= */ false);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ false);
          if (!lowLatencyStreams.containsKey(utteranceId)) {
            utteranceProgressCallback.onStart(reported);
          }
        }

        @Override
        public void onBeginSynthesis(
            String utteranceId, int sampleRateInHz, int audioFormat, int channelCount) {
          engineResponded(utteranceId, /* finished= */ false);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ false);
          LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.get(utteranceId);
          if (stream != null) {
            stream.begin(sampleRateInHz, audioFormat, channelCount);
          } else {
            utteranceProgressCallback.onBeginSynthesis(
                reported, sampleRateInHz, audioFormat, channelCount);
          }
        }

        @Override
        public void onAudioAvailable(String utteranceId, byte[] audio) {
          engineResponded(utteranceId, /* finished= */ false);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ false);
          LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.get(utteranceId);
          if (stream != null) {
            stream.write(audio);
          }
          utteranceProgressCallback.onAudioAvailable(reported, audio);
        }

        @Override
        public void onRangeStart(String utteranceId, int start, int end, int frame) {
          engineResponded(utteranceId, /* finished= */ false);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ false);
          LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.get(utteranceId);
          if (stream != null) {
            stream.range(start, end, frame);
          } else {
            utteranceProgressCallback.onRangeStart(reported, start, end, frame);
          }
        }

        @Override
        public void onDone(String utteranceId) {
          engineResponded(utteranceId, /* finished= */ true);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ true);
          LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.remove(utteranceId);
          if (stream != null) {
            stream.end();
          } else {
            utteranceProgressCallback.onDone(reported);
          }
        }

        @Override
        public void onStop(String utteranceId, boolean interrupted) {
          engineResponded(utteranceId, /* finished= */ true);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ true);
          LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.remove(utteranceId);
          if (stream != null) {
            stream.stop();
          } else {
            utteranceProgressCallback.onStop(reported, interrupted);
          }
        }

        @Override
        public void onError(String utteranceId) {
          engineResponded(utteranceId, /* finished= */ true);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ true);
          LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.remove(utteranceId);
          if (stream != null) {
            // Not for speech that was stopped already, and reported so.
            if (!stream.isFinished()) {
              speakAgainWithoutLowLatency(utteranceId, stream, /* engineAtFault= */ true);
            }
          } else {
            utteranceProgressCallback.onError(reported);
          }
        }

        @Override
        public void onError(String utteranceId, int errorCode) {
          engineResponded(utteranceId, /* finished= */ true);
          if (isRespokenFirstTry(utteranceId)) {
            return;
          }
          String reported = reportedId(utteranceId, /* finished= */ true);
          LowLatencyAudio.@Nullable SpeechStream stream = lowLatencyStreams.remove(utteranceId);
          if (stream != null) {
            // Not for speech that was stopped already, and reported so.
            if (!stream.isFinished()) {
              speakAgainWithoutLowLatency(utteranceId, stream, /* engineAtFault= */ true);
            }
          } else {
            utteranceProgressCallback.onError(reported, errorCode);
          }
        }
      };

  /**
   * Returns the piece for a split utterance's progress callback, or {@code null} if {@code
   * utteranceId} is not a piece. A piece of an utterance that already ended, by a stop or an error
   * on another piece, returns a piece with a {@code null} utterance ID, and its callback is dropped.
   */
  private @Nullable SpeechChunk toOriginal(@Nullable String utteranceId) {
    if (utteranceId == null || !utteranceId.contains(CHUNK_ID_SEPARATOR)) {
      return null;
    }
    SpeechChunk chunk = speechChunks.get(utteranceId);
    return chunk != null ? chunk : new SpeechChunk(null, 0, false, false);
  }

  /** Forgets every piece of {@code utteranceId}, once it has ended. */
  private void forgetChunks(String utteranceId) {
    String prefix = utteranceId + CHUNK_ID_SEPARATOR;
    speechChunks.keySet().removeIf(id -> id.startsWith(prefix));
  }

  private void notifyInterruptedForSuspendQueue() {
    List<SpeakRequest> suspendQueueCopy = new ArrayList<>(suspendQueue);
    suspendQueue.clear();
    LogUtils.d(TAG, "notifyInterruptedForSuspendQueue suspendQueueCopy = %s", suspendQueueCopy);
    suspendQueueCopy.forEach(
        speakRequest -> utteranceProgressCallback.onStop(speakRequest.utteranceId, true));
  }

  /**
   * Flushes the TextToSpeech queue for fast speech queueing, needed only on Android M.
   * REFERTO
   */
  private void ensureQueueFlush() {
    if (BuildVersionUtils.isM()) {
      tts.speak("", TextToSpeech.QUEUE_FLUSH, null, null);
    }
  }

  /**
   * Stops starting another engine if {@code engine} is the one in use, such as when switching to a
   * profile with another engine and back before it has started. Otherwise that engine would take
   * over once it started.
   */
  private void cancelEngineSwitchAwayFrom(@Nullable String engine) {
    if (tempTts == null || engine == null || !engine.equals(ttsEngine)
        || engine.equals(tempTtsEngine)) {
      return;
    }
    LogUtils.i(TAG, "Not switching to TTS engine %s, staying with %s", tempTtsEngine, engine);
    TextToSpeechUtils.attemptTtsShutdown(tempTts);
    tempTts = null;
    tempTtsEngine = null;
    // Its start-up callback is ignored.
    tempTtsGeneration++;
  }

  /**
   * Try to switch the TTS engine.
   *
   * @param engine The package name of the desired TTS engine
   */
  private void setTtsEngine(String engine, boolean resetFailures) {
    if (resetFailures) {
      ttsFailures = 0;
    }

    TextToSpeechUtils.attemptTtsShutdown(tempTts);

    LogUtils.logWithLimit(
        TAG, Log.INFO, ttsFailures, MAX_LOG_MESSAGES, "Switching to TTS engine: %s", engine);

    final int generation = ++tempTtsGeneration;
    tempTtsEngine = engine;
    tempTts =
        new TextToSpeech(context, status -> mHandler.onTtsInitialized(status, generation), engine);
  }

  /**
   * Assumes the current engine has failed and attempts to start the next available engine.
   *
   * @param failedEngine The package name of the engine to switch from.
   */
  private void attemptTtsFailover(String failedEngine) {
    LogUtils.logWithLimit(
        TAG,
        Log.ERROR,
        ttsFailures,
        MAX_LOG_MESSAGES,
        "Attempting TTS failover from %s",
        failedEngine);

    ttsFailures++;

    // If there is only one installed engine, or if the current engine
    // hasn't failed enough times, just restart the current engine.
    if ((installedTtsEngines.size() <= 1) || (ttsFailures < MAX_TTS_FAILURES)) {
      setTtsEngine(failedEngine, false);
      return;
    }

    // Move the engine to the back of the list.
    if (failedEngine != null) {
      installedTtsEngines.remove(failedEngine);
      installedTtsEngines.addLast(failedEngine);
    }

    // Try to use the first available TTS engine.
    final String nextEngine = installedTtsEngines.getFirst();

    setTtsEngine(nextEngine, true);
  }

  /**
   * Handles TTS engine initialization.
   *
   * @param status The status returned by the TTS engine.
   */
  @SuppressWarnings("deprecation")
  private void handleTtsInitialized(int status, int generation) {
    if (generation != tempTtsGeneration) {
      LogUtils.v(TAG, "Ignoring init of an abandoned TTS engine");
      return;
    }
    if (tempTts == null) {
      LogUtils.e(TAG, "Attempted to initialize TTS more than once!");
      return;
    }

    final TextToSpeech tempTts = this.tempTts;
    final String tempTtsEngine = this.tempTtsEngine;

    this.tempTts = null;
    this.tempTtsEngine = null;

    synchronized (ttsLock) {
      cachedTtsLocale = null;
      engineVoices = null;
      missingVoice = null;
    }

    if (status != TextToSpeech.SUCCESS) {
      attemptTtsFailover(tempTtsEngine);
      return;
    }

    final boolean isSwitchingEngines = (tts != null);

    if (isSwitchingEngines) {
      TextToSpeechUtils.attemptTtsShutdown(tts);
    }
    // Speech from an engine that is gone never finishes, so it must not hold up the new engine's.
    mHandler.removeCallbacks(dropHeldSpeech);
    heldUtteranceId = null;
    heldPlayer = null;
    lowLatencyStreams.clear();
    lowLatencyRequests.clear();
    respokenUtterances.clear();
    LowLatencyAudio.stopAllStreams();
    mHandler.removeCallbacks(checkEngineHang);
    engineQuietSince = 0;
    unfinishedUtterances.clear();

    tts = tempTts;
    tts.setOnUtteranceProgressListener(lowLatencyRouter);

    if (tempTtsEngine == null) {
      ttsEngine = TextToSpeechCompatUtils.getCurrentEngine(tts);
    } else {
      ttsEngine = tempTtsEngine;
    }

    updateDefaultLocale();

    applyAudioAttributes();

    LogUtils.i(TAG, "Switched to TTS engine: %s", tempTtsEngine);

    for (FailoverTtsListener mListener : listeners) {
      mListener.onTtsInitialized(isSwitchingEngines, tempTtsEngine);
    }
  }

  /**
   * Adds a speech to the speech cache.
   *
   * @param text the text to be spoken
   * @param pitch the pitch of the utterance
   * @param rate the speech rate of the utterance
   * @param resultNotifier the callback to notify whether the speech cache is loaded or failed.
   * @param eventId the event id triggering this action, used for logging
   */
  void addSpeech(
      CharSequence text,
      LoadSpeechResultNotifier resultNotifier,
      float pitch,
      float rate,
      @Nullable EventId eventId) {
    if (!isReady()) {
      LogUtils.e(TAG, "addSpeech TTS is not ready: text=" + text);
      resultNotifier.onFinished(
          new SpeechInfo(text, /* utteranceId= */ null, /* locale= */ null, pitch, rate), false);
      return;
    }
    if (speechCacheManager == null) {
      return;
    }
    if (usableSpeechCache() == null) {
      // Nothing is cached while a voice profile is in use. Reported as a failure, as when the
      // engine isn't ready, so that it's cached again once Backtalk default is back.
      resultNotifier.onFinished(
          new SpeechInfo(text, /* utteranceId= */ null, /* locale= */ null, pitch, rate), false);
      return;
    }
    Locale locale = null;
    if (text instanceof Spannable spannable) {
      TtsSpan[] ttsSpans = spannable.getSpans(0, text.length(), TtsSpan.class);
      if (ttsSpans.length > 0) {
        resultNotifier.onFinished(null, false);
        return;
      }
      LocaleSpan[] spans = spannable.getSpans(0, text.length(), LocaleSpan.class);
      if (spans.length > 1) {
        return;
      }
      if (spans.length == 1) {
        locale = localeToSpeak(spans[0].getLocale());
      }
    }
    if (locale == null) {
      locale = getUsedLocale();
    }

    Bundle bundle = new Bundle();
    float effectiveRate = rate * defaultRate;
    float effectivePitch = pitch * defaultPitch;
    bundle.putInt(FailoverTextToSpeech.SpeechParam.PITCH, (int) (effectivePitch * 100));
    bundle.putInt(FailoverTextToSpeech.SpeechParam.RATE, (int) (effectiveRate * 100));
    bundle.putFloat(FailoverTextToSpeech.SpeechParam.VOLUME, 1.0f);
    speechCacheManager.addSpeechCache(this, text, locale, resultNotifier, bundle, ttsEngine);
  }

  /**
   * Method that's called by TTS whenever an utterance starts.
   *
   * @param utteranceId The utteranceId from the onUtteranceStarted callback - we expect this to
   *     consist of UTTERANCE_ID_PREFIX followed by the utterance index.
   * @param delay The time in milliseconds elapsed between {@link
   *     UtteranceProgressListener#onStart(String)} invoked and the callback dispatched by {@link
   *     SpeechHandler}.
   */
  private void handleUtteranceStarted(String utteranceId, long delay) {
    for (FailoverTtsListener mListener : listeners) {
      mListener.onUtteranceStarted(utteranceId, delay);
    }
  }

  /**
   * Method that's called by TTS to update the range of utterance being spoken.
   *
   * @param utteranceId The utteranceId from the onUtteranceStarted callback - we expect this to
   *     consist of UTTERANCE_ID_PREFIX followed by the utterance index.
   * @param start The start index of the range in the utterance text.
   * @param end The end index of the range in the utterance text.
   */
  private void handleUtteranceRangeStarted(String utteranceId, int start, int end) {
    for (FailoverTtsListener mListener : listeners) {
      mListener.onUtteranceRangeStarted(utteranceId, start, end);
    }
  }

  /**
   * Method that's called by TTS whenever an utterance is completed. Do common tasks and execute any
   * UtteranceCompleteActions associate with this utterance index (or an earlier index, in case one
   * was accidentally dropped).
   *
   * @param utteranceId The utteranceId from the onUtteranceCompleted callback - we expect this to
   *     consist of UTTERANCE_ID_PREFIX followed by the utterance index.
   * @param success {@code true} if the utterance was spoken successfully.
   */
  private void handleUtteranceCompleted(String utteranceId, boolean success) {
    if (success) {
      ttsFailures = 0;
    }
    allowDeviceSleep(utteranceId);
    for (FailoverTtsListener mListener : listeners) {
      mListener.onUtteranceCompleted(utteranceId, success);
    }
  }

  /**
   * Handles media state changes.
   *
   * @param action The current media state.
   */
  private void handleMediaStateChanged(String action) {
    if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
      if (!TextUtils.equals(systemTtsEngine, ttsEngine)) {
        // Temporarily switch to the system TTS engine.
        LogUtils.v(TAG, "Saw media unmount");
        setTtsEngine(systemTtsEngine, true);
      }
    }

    if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
      final String targetEngine = getTargetEngine();
      if (!TextUtils.equals(targetEngine, ttsEngine)) {
        // Try to switch back to the preferred or default engine.
        LogUtils.v(TAG, "Saw media mount");
        setTtsEngine(targetEngine, true);
      }
    }
  }

  private void applyAudioAttributes() {
    if (tts == null) {
      return;
    }
    int usage =
        shouldUseAccessibilityStream(context)
            ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
            : AudioAttributes.USAGE_MEDIA;
    tts.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build());
  }

  private @Nullable String getTargetEngine() {
    VoiceProfile profile = VoiceProfiles.active();
    String preferred = profile != null ? profile.engine() : preferredTtsEngine;
    return (preferred != null && installedTtsEngines.contains(preferred))
        ? preferred
        : defaultTtsEngine;
  }

  public void updateDefaultEngine() {
    final ContentResolver resolver = context.getContentResolver();

    // Always refresh the list of available engines, since the user may have
    // installed a new TTS and then switched to it.
    installedTtsEngines.clear();
    systemTtsEngine =
        TextToSpeechUtils.reloadInstalledTtsEngines(
            context.getPackageManager(), installedTtsEngines);

    // This may be null if the user hasn't specified an engine.
    defaultTtsEngine = Secure.getString(resolver, Secure.TTS_DEFAULT_SYNTH);

    // Switch engines when the target engine changes and it's not the current engine.
    final String targetEngine = getTargetEngine();
    if (targetEngine != null && targetEngine.equals(tempTtsEngine)) {
      return;
    }
    if (ttsEngine == null || !ttsEngine.equals(targetEngine)) {
      if (installedTtsEngines.contains(targetEngine)) {
        setTtsEngine(targetEngine, true);
      } else if (!installedTtsEngines.isEmpty()) {
        // We'll take whatever TTS we can get.
        setTtsEngine(installedTtsEngines.getFirst(), true);
      }
    }
  }

  /**
   * Loads the default pitch adjustment from {@link Secure#TTS_DEFAULT_PITCH}. This will take effect
   * during the next call to {@link #trySpeak}.
   */
  private void updateDefaultPitch() {
    defaultPitch = (Secure.getInt(resolver, Secure.TTS_DEFAULT_PITCH, 100) / 100.0f);
    for (FailoverTtsListener listener : listeners) {
      listener.onDefaultPitchChanged(defaultPitch);
    }
  }

  /**
   * Loads the default rate adjustment from {@link Secure#TTS_DEFAULT_RATE}. This will take effect
   * during the next call to {@link #trySpeak}.
   */
  private void updateDefaultRate() {
    defaultRate = (Secure.getInt(resolver, Secure.TTS_DEFAULT_RATE, 100) / 100.0f);
    for (FailoverTtsListener listener : listeners) {
      listener.onDefaultRateChanged(defaultRate);
    }
  }

  /** Preferred locale for fallback language. */
  private static final Locale PREFERRED_FALLBACK_LOCALE = Locale.US;

  /** The system's default locale. */
  private Locale mSystemLocale = Locale.getDefault();

  /**
   * The current engine's default locale. This will be {@code null} if the user never specified a
   * preference.
   */
  private @Nullable Locale mDefaultLocale = null;

  /**
   * The locale specified by the last utterance with {@link #speak(CharSequence, Locale, float,
   * float, HashMap, int, float, boolean, boolean)}.
   */
  private @Nullable Locale mLastUtteranceLocale = null;

  // TODO: Replace it with cachedTtsLocale.
  /**
   * Keep the recently in used locale by TTS. Querying current TTS' language is time-consuming, we
   * use this cache variable to save time.
   */
  private @Nullable Locale localeInUse = null;

  /**
   * Caches the locale previously set using {@link TextToSpeech#setLanguage(Locale)}. Since setting
   * the language on a `TextToSpeech` instance can be a time-consuming operation, this cache helps
   * to avoid redundant calls and improve performance.
   */
  private @Nullable Locale cachedTtsLocale;

  /**
   * Helper method that ensures the text-to-speech engine works even when the user is using the
   * Google TTS and has the system set to a non-embedded language.
   *
   * <p>This method should be called whenever the TTS engine is loaded, the system locale changes,
   * or the default TTS locale changes.
   */
  private @Nullable Locale ensureSupportedLocale() {
    VoiceProfile profile = VoiceProfiles.active();
    if (profile != null) {
      return attemptSetProfileVoice(profile);
    }
    if (needsFallbackLocale()) {
      return attemptSetFallbackLanguage();
    } else {
      // We might need to restore the system locale. Or, if we've ever
      // explicitly set the locale, we'll need to work around a bug where
      // there's no way to tell the TTS engine to use whatever it thinks
      // the default language should be.
      return attemptRestorePreferredLocale();
    }
  }

  /** Returns whether we need to attempt to use a fallback language. */
  private boolean needsFallbackLocale() {
    // If the user isn't using Google TTS, or if they set a preferred
    // locale, we do not need to check locale support.
    if (!Objects.equals(ttsEngine, PACKAGE_GOOGLE_TTS) || (mDefaultLocale != null)) {
      return false;
    }

    if (tts == null) {
      return false;
    }

    // Otherwise, the TTS engine will attempt to use the system locale which
    // may not be supported. If the locale is embedded or advertised as
    // available, we're fine.
    final Set<String> features = tts.getFeatures(mSystemLocale);
    return !(((features != null) && features.contains(Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS))
        || !isNotAvailableStatus(tts.isLanguageAvailable(mSystemLocale)));
  }

  /** Attempts to obtain and set a fallback TTS locale. */
  private @Nullable Locale attemptSetFallbackLanguage() {
    final Locale fallbackLocale = getBestAvailableLocale();
    if (fallbackLocale == null) {
      LogUtils.e(TAG, "Failed to find fallback locale");
      return null;
    }
    LogUtils.v(TAG, "Attempt setting fallback TTS locale.");
    LogUtils.v(TAG, "attemptSetFallbackLanguage fallback tts locale.");
    return attemptSetLanguage(fallbackLocale);
  }

  /**
   * Attempts to set a TTS locale.
   *
   * @param locale TTS locale to set.
   * @return {@code true} if successfully set the TTS locale.
   */
  private @Nullable Locale attemptSetLanguage(@Nullable Locale locale) {
    if (locale == null) {
      LogUtils.w(TAG, "Cannot set null locale.");
      return null;
    }
    if (tts == null) {
      LogUtils.e(TAG, "mTts null when setting locale.");
      return null;
    }

    final int status = setTtsLocale(locale);
    if (isNotAvailableStatus(status)) {
      LogUtils.e(TAG, "Failed to set locale to %s", locale);
      return null;
    }

    LogUtils.v(TAG, "attemptSetLanguage- Set locale to %s", locale);
    return locale;
  }

  /**
   * Synthesizes the speech to file with the given locale. If the locale is not set, then it will be
   * {@link #cachedTtsLocale}, the current locale used in {@link TextToSpeech}.
   */
  @WorkerThread
  protected boolean synthesizeToFile(
      final CharSequence text,
      @Nullable Locale textLocale,
      final Bundle params,
      final File file,
      final String utteranceId) {
    if (!isReady()) {
      return false;
    }
    synchronized (ttsLock) {
      if (textLocale != null) {
        Locale actualLocale = attemptSetLanguage(textLocale);
        if (actualLocale == null) {
          return false;
        }
        mLastUtteranceLocale = actualLocale;
      }
      // Use getter to override it in testing.
      if (getEngineInstance().synthesizeToFile(text, params, file, utteranceId)
          == TextToSpeech.SUCCESS) {
        return true;
      }
      return false;
    }
  }

  private int setTtsLocale(Locale locale) {
    synchronized (ttsLock) {
      if (cachedTtsLocale != null && cachedTtsLocale.equals(locale)) {
        return LANG_AVAILABLE;
      }

      int status = tts.setLanguage(locale);
      LogUtils.d(TAG, "set ttsLocale: %s with status %s", locale, status);
      if (!isNotAvailableStatus(status)) {
        cachedTtsLocale = locale;
      }
      return status;
    }
  }

  /** Dumps the important variables for debugging. */
  public void dump(Logger dumpLogger) {
    dumpLogger.log(" cachedTtsLocale=%s", getUsedLocale());
    dumpLogger.log(" mDefaultLocale=%s", toLanguageTag(mDefaultLocale));
    dumpLogger.log(" mSystemLocale=%s", toLanguageTag(mSystemLocale));
    dumpLogger.log(" mLastUtteranceLocale=%s", toLanguageTag(mLastUtteranceLocale));
    dumpLogger.log(" getVoice=%s", getTtsVoice());
    if (speechCacheManager != null) {
      speechCacheManager.dumpCachedSpeechInfo(dumpLogger);
    }
  }

  private String toLanguageTag(@Nullable Locale locale) {
    return locale != null ? locale.toLanguageTag() : "null";
  }

  private @Nullable Voice getTtsVoice() {
    return tts != null ? tts.getVoice() : null;
  }

  /**
   * Attempts to obtain a supported TTS locale with preference given to {@link
   * #PREFERRED_FALLBACK_LOCALE}. The resulting locale may not be optimal for the user, but it will
   * likely be enough to understand what's on the screen.
   */
  private @Nullable Locale getBestAvailableLocale() {
    if (tts == null) {
      return null;
    }

    // Always attempt to use the preferred locale first.
    if (tts.isLanguageAvailable(PREFERRED_FALLBACK_LOCALE) >= 0) {
      return PREFERRED_FALLBACK_LOCALE;
    }

    // Since there's no way to query available languages from an engine,
    // we'll need to check every locale supported by the device.
    Locale bestLocale = null;
    int bestScore = -1;

    final Locale[] locales = Locale.getAvailableLocales();
    for (Locale locale : locales) {
      final int status = tts.isLanguageAvailable(locale);
      if (isNotAvailableStatus(status)) {
        continue;
      }

      final int score = compareLocales(mSystemLocale, locale);
      if (score > bestScore) {
        bestLocale = locale;
        bestScore = score;
      }
    }

    return bestLocale;
  }

  /**
   * Attempts to restore the user's preferred TTS locale, if set. Otherwise attempts to restore the
   * system locale.
   */
  private @Nullable Locale attemptRestorePreferredLocale() {
    if (tts == null) {
      return null;
    }
    mLastUtteranceLocale = null;
    final Locale preferredLocale = (mDefaultLocale != null ? mDefaultLocale : mSystemLocale);
    try {
      final int status = setTtsLocale(preferredLocale);
      if (!isNotAvailableStatus(status)) {
        LogUtils.i(TAG, "Restored TTS locale to %s", preferredLocale);
        return preferredLocale;
      }
    } catch (Exception e) {
      LogUtils.e(TAG, "Failed to setLanguage(): %s", e.toString());
    }

    LogUtils.e(TAG, "Failed to restore TTS locale to %s", preferredLocale);
    return null;
  }

  /** Handles updating the default locale. */
  private void updateDefaultLocale() {
    final String defaultLocale = TextToSpeechUtils.getDefaultLocaleForEngine(resolver, ttsEngine);
    mDefaultLocale = !TextUtils.isEmpty(defaultLocale) ? forLanguageTag(defaultLocale) : null;
    updateVoiceLanguage();

    // The default locale changed, which may mean we can restore the user's
    // preferred locale.
    localeInUse = ensureSupportedLocale();
  }

  /** Handles updating the system locale. */
  private void onConfigurationChanged(Configuration newConfig) {
    final Locale newLocale = newConfig.locale;
    if (newLocale.equals(mSystemLocale)) {
      return;
    }

    mSystemLocale = newLocale;
    updateVoiceLanguage();

    // The system locale changed, which may mean we need to override the
    // current TTS locale.
    localeInUse = ensureSupportedLocale();
  }

  /** Registers the configuration change callback. */
  private void registerGoogleTtsFixCallbacks() {
    final Uri defaultLocaleUri = Secure.getUriFor(SecureCompatUtils.TTS_DEFAULT_LOCALE);
    resolver.registerContentObserver(defaultLocaleUri, false, mLocaleObserver);
    context.registerComponentCallbacks(mComponentCallbacks);
  }

  /** Unregisters the configuration change callback. */
  private void unregisterGoogleTtsFixCallbacks() {
    resolver.unregisterContentObserver(mLocaleObserver);
    context.unregisterComponentCallbacks(mComponentCallbacks);
  }

  /**
   * Compares a locale against a primary locale. Returns higher values for closer matches. A return
   * value of 3 indicates that the locale is an exact match for the primary locale's language,
   * country, and variant.
   *
   * @param primary The primary locale for comparison.
   * @param other The other locale to compare against the primary locale.
   * @return A value indicating how well the other locale matches the primary locale. Higher is
   *     better.
   */
  private static int compareLocales(Locale primary, Locale other) {
    final String lang = primary.getLanguage();
    if ((lang == null) || !lang.equals(other.getLanguage())) {
      return 0;
    }

    final String country = primary.getCountry();
    if ((country == null) || !country.equals(other.getCountry())) {
      return 1;
    }

    final String variant = primary.getVariant();
    if ((variant == null) || !variant.equals(other.getVariant())) {
      return 2;
    }

    return 3;
  }

  /**
   * Returns {@code true} if the specified status indicates that the language is available.
   *
   * @param status A language availability code, as returned from {@link
   *     TextToSpeech#isLanguageAvailable}.
   * @return {@code true} if the status indicates that the language is available.
   */
  private static boolean isNotAvailableStatus(int status) {
    return (status != LANG_AVAILABLE)
        && (status != LANG_COUNTRY_AVAILABLE)
        && (status != LANG_COUNTRY_VAR_AVAILABLE);
  }

  private final SpeechHandler mHandler = new SpeechHandler(this);

  /** Handles changes to the default TTS engine. */
  private final ContentObserver mSynthObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultEngine();
        }
      };

  private final ContentObserver mPitchObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultPitch();
        }
      };

  private final ContentObserver mRateObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultRate();
        }
      };

  /** Callbacks used to observe changes to the TTS locale. */
  private final ContentObserver mLocaleObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultLocale();
        }
      };

  /**
   * A callback for speech progress of {@link TextToSpeech} and {@link SpeechCacheManager}.
   *
   * <p><strong>Note: </strong> By default, the callback is invoked in TTS thread and we hand over
   * the message to handler thread for processing. In some special cases when we want to handle the
   * callback in TTS thread, call {@link #setHandleTtsCallbackInHandlerThread(boolean)}.
   */
  private class UtteranceProgressCallback extends UtteranceProgressListener {
    private @Nullable String lastUpdatedUtteranceId = null;

    private void updatePerformanceMetrics(String utteranceId, boolean localCache) {
      // Update performance for this utterance, only if we did not recently update
      // for the same utterance.
      if (utteranceId != null && !utteranceId.equals(lastUpdatedUtteranceId)) {
        Performance.getInstance().onFeedbackOutput(utteranceId, localCache);
      }
      lastUpdatedUtteranceId = utteranceId;
    }

    @Override
    public void onStart(String utteranceId) {
      progressEstimator.onStarted(utteranceId, SystemClock.uptimeMillis());
      if (utteranceId.startsWith(CACHE_UTTERANCE_ID_PREFIX)) {
        return;
      }
      SpeechChunk chunk = toOriginal(utteranceId);
      if (chunk != null) {
        if (chunk.utteranceId() == null) {
          return;
        }
        if (!chunk.first()) {
          // A later piece starting to play is progress through the utterance. Report it as a
          // range, so pausing resumes from this piece with engines that report no word ranges.
          if (shouldHandleTtsCallbackInHandlerThread) {
            mHandler.onUtteranceRangeStarted(chunk.utteranceId(), chunk.offset(), chunk.offset());
          } else {
            FailoverTextToSpeech.this.handleUtteranceRangeStarted(
                chunk.utteranceId(), chunk.offset(), chunk.offset());
          }
          return;
        }
        utteranceId = chunk.utteranceId();
      }
      Performance.getInstance().onFeedbackReady(utteranceId);
      if (shouldHandleTtsCallbackInHandlerThread) {
        mHandler.onUtteranceStarted(utteranceId);
      } else {
        FailoverTextToSpeech.this.handleUtteranceStarted(utteranceId, /* delay= */ 0);
      }
    }

    @Override
    public void onAudioAvailable(String utteranceId, byte[] audio) {
      SpeechChunk chunk = toOriginal(utteranceId);
      if (chunk != null) {
        if (chunk.utteranceId() == null) {
          return;
        }
        utteranceId = chunk.utteranceId();
      }
      // onAudioAvailable() is usually called many times per utterance,
      // once for each audio chunk.
      updatePerformanceMetrics(utteranceId, /* localCache= */ false);
    }

    void onAudioCacheAvailable(String utteranceId) {
      // onAudioAvailable() is usually called many times per utterance,
      // once for each audio chunk.
      updatePerformanceMetrics(utteranceId, /* localCache= */ true);
    }

    @Override
    public void onRangeStart(String utteranceId, int start, int end, int frame) {
      progressEstimator.onRange(utteranceId);
      SpeechChunk chunk = toOriginal(utteranceId);
      if (chunk != null) {
        if (chunk.utteranceId() == null) {
          return;
        }
        utteranceId = chunk.utteranceId();
        start += chunk.offset();
        end += chunk.offset();
      }
      Performance.getInstance().onFeedbackRangeStarted(utteranceId);
      if (shouldHandleTtsCallbackInHandlerThread) {
        mHandler.onUtteranceRangeStarted(utteranceId, start, end);
      } else {
        FailoverTextToSpeech.this.handleUtteranceRangeStarted(utteranceId, start, end);
      }
    }

    @Override
    public void onStop(String utteranceId, boolean interrupted) {
      progressEstimator.onFinished(utteranceId, SystemClock.uptimeMillis(), /* completed= */ false);
      SpeechChunk chunk = toOriginal(utteranceId);
      if (chunk != null) {
        if (chunk.utteranceId() == null) {
          return;
        }
        utteranceId = chunk.utteranceId();
        forgetChunks(utteranceId);
      }
      if (speechCacheManager != null && speechCacheManager.handleOnStop(utteranceId, interrupted)) {
        return;
      }
      reportUtteranceCompleted(utteranceId, /* success= */ !interrupted);
    }

    @Override
    public void onError(String utteranceId) {
      progressEstimator.onFinished(utteranceId, SystemClock.uptimeMillis(), /* completed= */ false);
      SpeechChunk chunk = toOriginal(utteranceId);
      if (chunk != null) {
        if (chunk.utteranceId() == null) {
          return;
        }
        utteranceId = chunk.utteranceId();
        forgetChunks(utteranceId);
      }
      if (speechCacheManager != null && speechCacheManager.handleOnError(utteranceId)) {
        return;
      }
      reportUtteranceCompleted(utteranceId, /* success= */ false);
    }

    @Override
    public void onDone(String utteranceId) {
      progressEstimator.onFinished(utteranceId, SystemClock.uptimeMillis(), /* completed= */ true);
      SpeechChunk chunk = toOriginal(utteranceId);
      if (chunk != null) {
        speechChunks.remove(utteranceId);
        if (chunk.utteranceId() == null || !chunk.last()) {
          return;
        }
        utteranceId = chunk.utteranceId();
        forgetChunks(utteranceId);
      }
      if (speechCacheManager != null && speechCacheManager.handleOnDone(utteranceId)) {
        return;
      }
      reportUtteranceCompleted(utteranceId, /* success= */ true);
    }

    /**
     * Hands the end of an utterance over to the handler thread, as its start is, since the speech
     * controller starts the next utterance from it and is not safe to use from two threads.
     */
    private void reportUtteranceCompleted(String utteranceId, boolean success) {
      if (shouldHandleTtsCallbackInHandlerThread) {
        mHandler.onUtteranceCompleted(utteranceId, success);
      } else {
        handleUtteranceCompleted(utteranceId, success);
      }
    }
  }

  private final UtteranceProgressCallback utteranceProgressCallback =
      new UtteranceProgressCallback();

  /** Callbacks used to observe configuration changes. */
  private final ComponentCallbacks mComponentCallbacks =
      new ComponentCallbacks() {
        @Override
        public void onLowMemory() {
          // Do nothing.
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
          FailoverTextToSpeech.this.onConfigurationChanged(newConfig);
        }
      };

  /** {@link BroadcastReceiver} for detecting media mount and unmount. */
  private class MediaMountStateMonitor extends SameThreadBroadcastReceiver {
    private final IntentFilter mMediaIntentFilter;

    public MediaMountStateMonitor() {
      mMediaIntentFilter = new IntentFilter();
      mMediaIntentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
      mMediaIntentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
      mMediaIntentFilter.addDataScheme("file");
    }

    public IntentFilter getFilter() {
      return mMediaIntentFilter;
    }

    @Override
    public void onReceiveIntent(Intent intent) {
      final String action = intent.getAction();

      mHandler.onMediaStateChanged(action);
    }
  }

  /** Handler used to return to the main thread from the TTS thread. */
  private static class SpeechHandler extends WeakReferenceHandler<FailoverTextToSpeech> {
    /** Hand-off engine initialized. */
    private static final int MSG_INITIALIZED = 1;

    /** Hand-off utterance started. */
    private static final int MSG_UTTERANCE_STARTED = 2;

    /** Hand-off utterance completed. */
    private static final int MSG_UTTERANCE_COMPLETED = 3;

    /** Hand-off media state changes. */
    private static final int MSG_MEDIA_STATE_CHANGED = 4;

    /** Hand-off a range of utterance started. */
    private static final int MSG_UTTERANCE_RANGE_STARTED = 5;

    public SpeechHandler(FailoverTextToSpeech parent) {
      super(parent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleMessage(Message msg, FailoverTextToSpeech parent) {
      switch (msg.what) {
        case MSG_INITIALIZED -> parent.handleTtsInitialized(msg.arg1, msg.arg2);
        case MSG_UTTERANCE_STARTED -> {
          long talkbackDelay = SystemClock.uptimeMillis() - msg.getWhen();
          parent.handleUtteranceStarted((String) msg.obj, talkbackDelay);
        }
        case MSG_UTTERANCE_COMPLETED -> {
          Pair<String, Boolean> data = (Pair<String, Boolean>) msg.obj;
          parent.handleUtteranceCompleted(
              /* utteranceId= */ data.first, /* success= */ data.second);
        }
        case MSG_MEDIA_STATE_CHANGED -> parent.handleMediaStateChanged((String) msg.obj);
        case MSG_UTTERANCE_RANGE_STARTED ->
            parent.handleUtteranceRangeStarted((String) msg.obj, msg.arg1, msg.arg2);
        default -> {}
      }
    }

    public void onTtsInitialized(int status, int generation) {
      obtainMessage(MSG_INITIALIZED, status, generation).sendToTarget();
    }

    public void onUtteranceStarted(String utteranceId) {
      obtainMessage(MSG_UTTERANCE_STARTED, utteranceId).sendToTarget();
    }

    public void onUtteranceRangeStarted(String utteranceId, int start, int end) {
      obtainMessage(MSG_UTTERANCE_RANGE_STARTED, start, end, utteranceId).sendToTarget();
    }

    public void onUtteranceCompleted(String utteranceId, boolean success) {
      obtainMessage(MSG_UTTERANCE_COMPLETED, Pair.create(utteranceId, success)).sendToTarget();
    }

    public void onMediaStateChanged(String action) {
      obtainMessage(MSG_MEDIA_STATE_CHANGED, action).sendToTarget();
    }
  }

  /** Listener for TTS events. */
  public interface FailoverTtsListener {
    /** Called after the class has initialized with a tts engine. */
    default void onTtsInitialized(boolean wasSwitchingEngines, String enginePackageName) {}

    /** Called before an utterance is sent to the TTS engine. */
    default void onBeforeUtteranceRequested(
        String utteranceId, UtteranceInfoCombo utteranceInfoCombo) {}

    /*
     * Called before an utterance starts speaking.
     */
    default void onUtteranceStarted(String utteranceId) {}

    /**
     * Called before an utterance starts speaking.
     *
     * @param delay The time (in milliseconds) elapsed between {@link
     *     UtteranceProgressListener#onStart(String)} invoked and the callback dispatched by {@link
     *     SpeechHandler}.
     */
    default void onUtteranceStarted(String utteranceId, long delay) {
      onUtteranceStarted(utteranceId);
    }

    /*
     * Called before speaking the range of an utterance.
     */
    void onUtteranceRangeStarted(String utteranceId, int start, int end);

    /*
     * Called after an utterance has completed speaking.
     */
    void onUtteranceCompleted(String utteranceId, boolean success);

    /** Called when the default pitch changes. */
    default void onDefaultPitchChanged(float pitch) {}

    /** Called when the default rate changes. */
    default void onDefaultRateChanged(float rate) {}
  }

  /** Details of the utterance that is sent to the TTS engine. */
  @AutoValue
  public abstract static class UtteranceInfoCombo {
    public abstract CharSequence text();

    public abstract @Nullable Locale locale();

    public abstract boolean isLocaleAttached();

    public abstract boolean isSeparatorInUtterance();

    public abstract boolean isAggressiveChunking();

    public abstract boolean flushGlobalTtsQueue();

    @ChangeLocaleAction
    public abstract int changeLocaleAction();

    public static Builder builder(
        CharSequence text, @Nullable Locale locale, boolean isLocaleAttached) {
      return builder(text, locale, isLocaleAttached, Performance.CHANGE_LOCALE_NONE);
    }

    public static Builder builder(
        CharSequence text,
        @Nullable Locale locale,
        boolean isLocaleAttached,
        @ChangeLocaleAction int changeLocaleAction) {
      return new AutoValue_FailoverTextToSpeech_UtteranceInfoCombo.Builder()
          .setText(text)
          .setLocale(locale)
          .setIsLocaleAttached(isLocaleAttached)
          .setIsSeparatorInUtterance(false)
          .setIsAggressiveChunking(false)
          .setFlushGlobalTtsQueue(true)
          .setChangeLocaleAction(changeLocaleAction);
    }

    /** Builders of the utterance info combo. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setText(CharSequence text);

      public abstract Builder setLocale(@Nullable Locale locale);

      public abstract Builder setIsLocaleAttached(boolean value);

      public abstract Builder setIsSeparatorInUtterance(boolean value);

      public abstract Builder setIsAggressiveChunking(boolean value);

      public abstract Builder setFlushGlobalTtsQueue(boolean value);

      public abstract Builder setChangeLocaleAction(@ChangeLocaleAction int changeLocaleAction);

      public abstract UtteranceInfoCombo build();
    }
  }

  /** Details of the utterance that is sent to the TTS engine. */
  public record SpeakRequest(
      Locale locale, CharSequence text, int queueMode, Bundle bundle, String utteranceId) {}

}
