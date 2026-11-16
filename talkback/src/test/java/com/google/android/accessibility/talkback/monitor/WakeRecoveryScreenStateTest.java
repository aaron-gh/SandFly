/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.ServiceStateListener;
import com.google.android.accessibility.utils.monitor.DisplayMonitor;
import com.google.android.accessibility.utils.monitor.RecentUserInput;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.UtteranceCompleteRunnable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;

/** The display callback and the SCREEN_ON broadcast need not reach the reader together. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@LooperMode(LooperMode.Mode.PAUSED)
public final class WakeRecoveryScreenStateTest {
  private TalkBackService service;
  private TalkBackService previousService;
  private FormFactorUtils previousFormFactor;
  private boolean previousWatchFeature;
  private PowerManager power;
  private RingerModeAndScreenMonitor monitor;
  private final List<Feedback> announcements = new ArrayList<>();

  @Before
  public void setUp() {
    previousService = TalkBackService.getInstance();
    previousFormFactor = ReflectionHelpers.getStaticField(FormFactorUtils.class, "instance");
    service = Robolectric.buildService(TalkBackService.class).get();
    previousWatchFeature = service.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
    shadowOf(service.getPackageManager()).setSystemFeature(PackageManager.FEATURE_WATCH, true);
    FormFactorUtils.initialize(service);
    ReflectionHelpers.setStaticField(TalkBackService.class, "instance", service);
    ReflectionHelpers.setField(service, "serviceState", ServiceStateListener.SERVICE_STATE_ACTIVE);
    Settings.Secure.putInt(service.getContentResolver(), Settings.Secure.DEVICE_PROVISIONED, 1);
    power = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
    shadowOf(power).setIsInteractive(false);
    FeedbackReturner pipeline =
        feedback -> {
          announcements.add(feedback);
          return true;
        };
    ListMenuManager menus =
        new ListMenuManager(service, pipeline, new ActorState(null), null, null, null);
    monitor =
        new RingerModeAndScreenMonitor(
            menus, pipeline, new CallStateMonitor(service), new DisplayMonitor(service), null, service);
    // Monitoring is already running at the captured wake; do not deliver a broadcast in setUp.
    ReflectionHelpers.setField(monitor, "monitoring", true);
  }

  @After
  public void tearDown() {
    if (monitor != null) {
      ExecutorService executor = ReflectionHelpers.getField(monitor, "executor");
      executor.shutdownNow();
    }
    ReflectionHelpers.setStaticField(TalkBackService.class, "instance", previousService);
    ReflectionHelpers.setStaticField(FormFactorUtils.class, "instance", previousFormFactor);
    shadowOf(service.getPackageManager())
        .setSystemFeature(PackageManager.FEATURE_WATCH, previousWatchFeature);
  }

  private UtteranceCompleteRunnable wakeBeforeScreenOnBroadcast() {
    shadowOf(power).setIsInteractive(true);
    monitor.onDisplayStateChanged(true);
    assertEquals(1, announcements.size());
    UtteranceCompleteRunnable completed =
        announcements.get(0).failovers().get(0).speech().options().mCompletedAction;
    assertNotNull(completed);
    return completed;
  }

  @Test
  public void retriesWhenDisplayIsOnBeforeScreenOnBroadcastArrives() {
    UtteranceCompleteRunnable completed = wakeBeforeScreenOnBroadcast();
    monitor.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_ERROR));
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(2, announcements.size());
  }

  @Test
  public void deviceAlreadyAsleepDoesNotReplayWithOldInteractiveState() {
    UtteranceCompleteRunnable completed = wakeBeforeScreenOnBroadcast();
    ReflectionHelpers.setField(monitor, "isInteractive", true);
    shadowOf(power).setIsInteractive(false);
    monitor.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_ERROR));
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, announcements.size());
  }

  @Test
  public void userInputStillPreventsReplayBeforeScreenOnBroadcastArrives() {
    UtteranceCompleteRunnable completed = wakeBeforeScreenOnBroadcast();
    RecentUserInput.mark();
    monitor.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_ERROR));
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, announcements.size());
  }
}
