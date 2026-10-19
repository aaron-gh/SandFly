/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.utils.input;

import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_ADDED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_PIP;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_REMOVED;
import static com.google.android.accessibility.utils.AccessibilityWindowInfoUtils.WINDOW_ID_NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Rect;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.Nullable;
import com.google.android.accessibility.utils.input.WindowEventInterpreter.Announcement;
import com.google.android.accessibility.utils.input.WindowEventInterpreter.EventInterpretation;
import com.google.android.accessibility.utils.input.WindowEventInterpreter.WindowInterpretation;
import com.google.android.accessibility.utils.monitor.DisplayMonitor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityRecord;
import org.robolectric.shadows.ShadowAccessibilityService;
import org.robolectric.shadows.ShadowAccessibilityWindowInfo;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Feeds the window interpreter the window events of common screen changes, with the windows the
 * system would list at each moment, and records every interpretation that reaches its listeners.
 * Each test pins down all that a listener can read, so that changes to how the interpreter looks up
 * windows can show that what it tells the rest of Backtalk stays the same.
 *
 * <p>The main looper is paused, so time passes only when a test waits, and the 10 ms checks of a
 * window change run one by one.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 34,
    shadows = {
      WindowEventInterpreterTest.WindowsShadow.class,
      WindowEventInterpreterTest.RootsShadow.class
    })
public class WindowEventInterpreterTest {

  /** The service the interpreter belongs to. */
  public static class TestService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
  }

  /** Lists the windows a test puts on screen, and counts how often the service is asked. */
  @Implements(AccessibilityService.class)
  public static class WindowsShadow extends ShadowAccessibilityService {
    private SparseArray<List<AccessibilityWindowInfo>> windows = new SparseArray<>();
    int windowListRequests;

    void setWindows(SparseArray<List<AccessibilityWindowInfo>> windows) {
      this.windows = windows;
    }

    // Like Android, hands out a new list each time, so that a list kept by the interpreter doesn't
    // change when the test puts other windows on screen.
    @Implementation
    @Override
    protected SparseArray<List<AccessibilityWindowInfo>> getWindowsOnAllDisplays() {
      windowListRequests++;
      SparseArray<List<AccessibilityWindowInfo>> copy = new SparseArray<>();
      for (int i = 0; i < windows.size(); i++) {
        copy.put(windows.keyAt(i), new ArrayList<>(windows.valueAt(i)));
      }
      return copy;
    }

    @Implementation
    @Override
    protected List<AccessibilityWindowInfo> getWindows() {
      windowListRequests++;
      return new ArrayList<>(windows.get(Display.DEFAULT_DISPLAY, new ArrayList<>()));
    }
  }

  /** Counts how often a window's root is fetched, which waits for the window's app. */
  @Implements(AccessibilityWindowInfo.class)
  public static class RootsShadow extends ShadowAccessibilityWindowInfo {
    static int rootRequests;

    @Implementation
    @Override
    protected AccessibilityNodeInfo getRoot() {
      rootRequests++;
      return super.getRoot();
    }
  }

  private static final int STATUS_BAR = 1;
  private static final int NAVIGATION_BAR = 2;
  private static final int SETTINGS = 10;
  private static final int WIFI = 11;
  private static final int TIP = 12;
  private static final int KEYBOARD = 20;
  private static final int DIVIDER = 30;
  private static final int CALCULATOR = 31;
  private static final int VIDEO = 40;
  private static final int NOTES = 50;
  private static final int VOLUME = 60;
  private static final int DOWNLOADS = 70;

  private static final String SETTINGS_PACKAGE = "com.android.settings";
  private static final String NOTES_PACKAGE = "com.example.notes";
  private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
  private static final String VOLUME_DIALOG_CLASS =
      "com.android.systemui.volume.VolumeDialogImpl$CustomDialog";

  private static final Rect SCREEN = new Rect(0, 0, 1080, 2400);
  private static final Rect TOP_HALF = new Rect(0, 0, 1080, 1180);
  private static final Rect BOTTOM_HALF = new Rect(0, 1220, 1080, 2400);

  private TestService service;
  private WindowsShadow serviceWindows;
  private WindowEventInterpreter interpreter;
  private final List<String> transcript = new ArrayList<>();
  private long startTime;

  @Before
  public void setUp() {
    service = Robolectric.setupService(TestService.class);
    serviceWindows = Shadow.extract(service);
    RootsShadow.rootRequests = 0;
    interpreter = new WindowEventInterpreter(service, new DisplayMonitor(service));
    interpreter.addListener((interpretation, eventId) -> transcript.add(describe(interpretation)));
  }

  @After
  public void tearDown() {
    WindowEventInterpreter.setReduceWindowDelay(false);
  }

  @Test
  public void screenNamedByItsStateChangeSettlesAtTheFirstCheck() {
    openSettings();

    setWindows(statusBar(), navigationBar(), app(WIFI, "Wi-Fi").active().build());
    interpret(stateChanged(WIFI, "Wi-Fi", SETTINGS_PACKAGE));
    waitMs(5);
    interpret(windowsChanged(WIFI, WINDOWS_CHANGE_ADDED));
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_REMOVED));
    waitMs(600);

    assertTranscript(
        "0ms state from=11 original start=0 delay=10/550 main settled | src 11\"Wi-Fi\" settled"
            + " was 10\"Settings\" | A 11\"Wi-Fi\" settled was 10\"Settings\"",
        "5ms windows from=11 changes=0x1 original start=0 delay=10/550 main settled | src"
            + " 11\"Wi-Fi\" settled was 10\"Settings\" | A 11\"Wi-Fi\" settled was 10\"Settings\"",
        "5ms windows from=10 changes=0x2 original start=0 delay=10/550 main settled | src 10"
            + " feedback \"untitled\" was 10\"Settings\" | A 11\"Wi-Fi\" settled was"
            + " 10\"Settings\"",
        "15ms windows from=10 changes=0x2 stable start=0 delay=10/550 main settled | src 10"
            + " feedback \"untitled\" was 10\"Settings\" | A 11\"Wi-Fi\" settled was"
            + " 10\"Settings\"");
  }

  @Test
  public void screenWithoutAStateChangeTitleWaitsForTheLongestDelay() {
    openSettings();

    setWindows(statusBar(), navigationBar(), app(WIFI, "Wi-Fi").active().build());
    interpret(windowsChanged(WIFI, WINDOWS_CHANGE_ADDED));
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_REMOVED));
    waitMs(600);

    assertTranscript(
        "0ms windows from=11 changes=0x1 original start=0 delay=10/550 main | src 11\"Wi-Fi\" was"
            + " 10\"Settings\" | A 11\"Wi-Fi\" was 10\"Settings\"",
        "0ms windows from=10 changes=0x2 original start=0 delay=10/550 main | src 10 feedback"
            + " \"untitled\" was 10\"Settings\" | A 11\"Wi-Fi\" was 10\"Settings\"",
        "550ms windows from=10 changes=0x2 stable start=0 delay=550/550 main | src 10 feedback"
            + " \"untitled\" was 10\"Settings\" | A 11\"Wi-Fi\" was 10\"Settings\"");
    // Each event and each of the 55 checks asks for the windows once, though each looks up the
    // Settings window, which has closed.
    assertEquals(2 + 55, serviceWindows.windowListRequests);
  }

  @Test
  public void reducedDelayEndsTheWaitSooner() {
    WindowEventInterpreter.setReduceWindowDelay(true);
    openSettings();

    setWindows(statusBar(), navigationBar(), app(WIFI, "Wi-Fi").active().build());
    interpret(windowsChanged(WIFI, WINDOWS_CHANGE_ADDED));
    waitMs(600);

    assertTranscript(
        "0ms windows from=11 changes=0x1 original start=0 delay=10/200 main | src 11\"Wi-Fi\" was"
            + " 10\"Settings\" | A 11\"Wi-Fi\" was 10\"Settings\"",
        "200ms windows from=11 changes=0x1 stable start=0 delay=200/200 main | src 11\"Wi-Fi\""
            + " was 10\"Settings\" | A 11\"Wi-Fi\" was 10\"Settings\"");
    // The event and each of the 20 checks ask for the windows once, for all their lookups.
    assertEquals(1 + 20, serviceWindows.windowListRequests);
  }

  @Test
  public void windowsThatChangeWithoutAnEventAreSeenWhileWaiting() {
    openSettings();

    // A window opens over Settings, but closes again before the system reports that it closed.
    setWindows(
        statusBar(),
        navigationBar(),
        app(SETTINGS, "Settings").inactive().build(),
        app(TIP, "Tip").layer(2).bounds(new Rect(100, 800, 980, 1600)).active().build());
    interpret(windowsChanged(TIP, WINDOWS_CHANGE_ADDED));
    waitMs(35);
    setWindows(statusBar(), navigationBar(), app(SETTINGS, "Settings").active().build());
    waitMs(20);
    interpret(windowsChanged(TIP, WINDOWS_CHANGE_REMOVED));
    waitMs(600);

    assertTranscript(
        "0ms windows from=12 changes=0x1 original start=0 delay=10/550 main | src 12\"Tip\" was"
            + " 10\"Settings\" | A 12\"Tip\" was 10\"Settings\"",
        "40ms windows from=12 changes=0x1 stable start=0 delay=40/550 settled | src 12\"Tip\" was"
            + " 10\"Settings\" | A 10\"Settings\"",
        "55ms windows from=12 changes=0x2 original stable start=55 delay=0/0 settled | src 12"
            + " feedback \"untitled\" was 12\"Tip\" | A 10\"Settings\"");
  }

  @Test
  public void splitScreenNamesBothApps() {
    openSettings();

    setWindows(
        statusBar(),
        navigationBar(),
        app(SETTINGS, "Settings").bounds(TOP_HALF).active().build(),
        window(DIVIDER, AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER, "Divider")
            .layer(3)
            .bounds(new Rect(0, 1180, 1080, 1220))
            .build(),
        app(CALCULATOR, "Calculator").bounds(BOTTOM_HALF).inactive().build());
    interpret(windowsChanged(CALCULATOR, WINDOWS_CHANGE_ADDED));
    waitMs(600);

    assertTranscript(
        "0ms windows from=31 changes=0x1 original start=0 delay=10/550 main | src 31\"Calculator\""
            + " was 10\"Settings\" | A 10\"Settings\" | B 31\"Calculator\" was -",
        "550ms windows from=31 changes=0x1 stable start=0 delay=550/550 main | src"
            + " 31\"Calculator\" was 10\"Settings\" | A 10\"Settings\" | B 31\"Calculator\" was -");
    assertTrue(interpreter.isSplitScreenMode(Display.DEFAULT_DISPLAY));
  }

  @Test
  public void keyboardIsReportedWhenShownAndHidden() {
    openSettings();

    setWindows(
        statusBar(),
        navigationBar(),
        app(SETTINGS, "Settings").active().build(),
        window(KEYBOARD, AccessibilityWindowInfo.TYPE_INPUT_METHOD, "Gboard")
            .layer(3)
            .bounds(new Rect(0, 1500, 1080, 2280))
            .build());
    interpret(windowsChanged(KEYBOARD, WINDOWS_CHANGE_ADDED));
    waitMs(600);
    setWindows(statusBar(), navigationBar(), app(SETTINGS, "Settings").active().build());
    interpret(windowsChanged(KEYBOARD, WINDOWS_CHANGE_REMOVED));
    waitMs(600);

    assertTranscript(
        "0ms windows from=20 changes=0x1 original start=0 delay=10/550 imeChanged announceIme |"
            + " src 20\"Gboard\" was 10\"Settings\" | A 10\"Settings\" | ime 20\"Gboard\" was -",
        "550ms windows from=20 changes=0x1 stable start=0 delay=550/550 imeChanged announceIme |"
            + " src 20\"Gboard\" was 10\"Settings\" | A 10\"Settings\" | ime 20\"Gboard\" was -",
        "600ms windows from=20 changes=0x2 original start=600 delay=10/550 imeChanged announceIme"
            + " | src 20 feedback \"untitled\" was 20\"Gboard\" | A 10\"Settings\" | ime -"
            + " feedback \"untitled\" was 20\"Gboard\"",
        "1150ms windows from=20 changes=0x2 stable start=600 delay=550/550 imeChanged announceIme"
            + " | src 20 feedback \"untitled\" was 20\"Gboard\" | A 10\"Settings\" | ime -"
            + " feedback \"untitled\" was 20\"Gboard\"");
  }

  @Test
  public void pictureInPictureWindowIsReported() {
    openSettings();

    setWindows(
        statusBar(),
        navigationBar(),
        app(SETTINGS, "Settings").active().build(),
        app(VIDEO, "Video")
            .layer(4)
            .bounds(new Rect(600, 1600, 1000, 1900))
            .pictureInPicture()
            .build());
    interpret(windowsChanged(VIDEO, WINDOWS_CHANGE_ADDED | WINDOWS_CHANGE_PIP));
    waitMs(600);
    setWindows(statusBar(), navigationBar(), app(SETTINGS, "Settings").active().build());
    interpret(windowsChanged(VIDEO, WINDOWS_CHANGE_REMOVED | WINDOWS_CHANGE_PIP));
    waitMs(600);

    assertTranscript(
        "0ms windows from=40 changes=0x401 original stable start=0 delay=0/0 pipChanged | src"
            + " 40\"Video\" was 10\"Settings\" | A 10\"Settings\" | pip 40\"Video\" was -",
        "600ms windows from=40 changes=0x402 original stable start=600 delay=0/0 pipChanged | src"
            + " 40 feedback \"untitled\" was 40\"Video\" | A 10\"Settings\" | pip - feedback"
            + " \"untitled\" was 40\"Video\"");
  }

  @Test
  public void windowWithoutATitleIsNamedAfterItsApp() {
    installApp(NOTES_PACKAGE, "Notes");
    openSettings();

    setWindows(
        statusBar(),
        navigationBar(),
        app(NOTES, null).rootPackage(NOTES_PACKAGE).active().build());
    interpret(windowsChanged(NOTES, WINDOWS_CHANGE_ADDED));
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_REMOVED));
    waitMs(600);
    assertEquals("Notes", interpreter.getWindowTitle(NOTES).toString());
    // Notes closes, and is no longer named after its app.
    setWindows(statusBar(), navigationBar(), app(SETTINGS, "Settings").active().build());
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_ADDED));
    interpret(windowsChanged(NOTES, WINDOWS_CHANGE_REMOVED));
    waitMs(600);

    assertTranscript(
        "0ms windows from=50 changes=0x1 original start=0 delay=10/550 main | src 50 feedback"
            + " \"Notes\" was 10\"Settings\" | A 50 feedback \"Notes\" was 10\"Settings\"",
        "0ms windows from=10 changes=0x2 original start=0 delay=10/550 main | src 10 feedback"
            + " \"untitled\" was 10\"Settings\" | A 50 feedback \"Notes\" was 10\"Settings\"",
        "550ms windows from=10 changes=0x2 stable start=0 delay=550/550 main | src 10 feedback"
            + " \"untitled\" was 10\"Settings\" | A 50 feedback \"Notes\" was 10\"Settings\"",
        "600ms windows from=10 changes=0x1 original start=600 delay=10/550 main | src"
            + " 10\"Settings\" was 10 | A 10\"Settings\" was 50",
        "600ms windows from=50 changes=0x2 original start=600 delay=10/550 main | src 50 feedback"
            + " \"untitled\" was 10 | A 10\"Settings\" was 50",
        "1150ms windows from=50 changes=0x2 stable start=600 delay=550/550 main | src 50 feedback"
            + " \"untitled\" was 10 | A 10\"Settings\" was 50");
    // The app is asked for the window's root once, though the window is named at every check.
    assertEquals(1, RootsShadow.rootRequests);
  }

  @Test
  public void appOfAWindowWithoutATitleIsAskedForAgainUntilItAnswers() {
    installApp(NOTES_PACKAGE, "Notes");
    openSettings();

    // At first the window's root can't be fetched, as before its app has drawn it.
    setWindows(statusBar(), navigationBar(), app(NOTES, null).active().build());
    interpret(windowsChanged(NOTES, WINDOWS_CHANGE_ADDED));
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_REMOVED));
    waitMs(100);
    setWindows(
        statusBar(),
        navigationBar(),
        app(NOTES, null).rootPackage(NOTES_PACKAGE).active().build());
    int rootRequestsBefore = RootsShadow.rootRequests;
    waitMs(500);

    assertTranscript(
        "0ms windows from=50 changes=0x1 original start=0 delay=10/550 main | src 50 feedback"
            + " \"untitled\" was 10\"Settings\" | A 50 feedback \"untitled\" was 10\"Settings\"",
        "0ms windows from=10 changes=0x2 original start=0 delay=10/550 main | src 10 feedback"
            + " \"untitled\" was 10\"Settings\" | A 50 feedback \"untitled\" was 10\"Settings\"",
        "550ms windows from=10 changes=0x2 stable start=0 delay=550/550 main | src 10 feedback"
            + " \"untitled\" was 10\"Settings\" | A 50 feedback \"Notes\" was 10\"Settings\"");
    // Once the root can be fetched, it is fetched once, and its package is kept.
    assertEquals(1, RootsShadow.rootRequests - rootRequestsBefore);
  }

  @Test
  public void rootPackageIsKeptForOthersToo() {
    AccessibilityWindowInfo notes = app(NOTES, null).rootPackage(NOTES_PACKAGE).active().build();
    setWindows(statusBar(), navigationBar(), notes);

    assertEquals(NOTES_PACKAGE, interpreter.getRootPackageName(notes).toString());
    assertEquals(NOTES_PACKAGE, interpreter.getRootPackageName(notes).toString());
    assertEquals(1, RootsShadow.rootRequests);
  }

  @Test
  public void paneTitleNamesTheWindowUntilThePaneCloses() {
    openSettings();

    interpret(pane(SETTINGS, CONTENT_CHANGE_TYPE_PANE_APPEARED, "Filters"));
    waitMs(600);
    assertEquals("Filters", interpreter.getAccessibilityPaneTitle(SETTINGS).toString());
    interpret(pane(SETTINGS, CONTENT_CHANGE_TYPE_PANE_DISAPPEARED, "Filters"));
    waitMs(600);

    assertTranscript(
        "0ms state from=10 content=0x10 original source start=0 delay=10/550 main settled"
            + " bounds=[0,0][0,0] | src 10\"Filters\" settled was 10\"Settings\" | A 10\"Filters\""
            + " settled was 10\"Settings\"",
        "10ms state from=10 content=0x10 stable source start=0 delay=10/550 main settled"
            + " bounds=[0,0][0,0] | src 10\"Filters\" settled was 10\"Settings\" | A 10\"Filters\""
            + " settled was 10\"Settings\"",
        "600ms state from=10 content=0x20 original source start=600 delay=10/550 main settled"
            + " bounds=[0,0][0,0] | src 10\"Settings\" settled was 10\"Filters\" | A"
            + " 10\"Settings\" settled was 10\"Filters\"",
        "610ms state from=10 content=0x20 stable source start=600 delay=10/550 main settled"
            + " bounds=[0,0][0,0] | src 10\"Settings\" settled was 10\"Filters\" | A"
            + " 10\"Settings\" settled was 10\"Filters\"");
    assertFalse(interpreter.hasAccessibilityPane(SETTINGS));
  }

  @Test
  public void screenOffForgetsTheWindowsAndTheFirstChangeAfterwardIsMarked() {
    openSettings();

    interpreter.onDisplayStateChanged(false);
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_ADDED));
    waitMs(600);
    interpreter.onDisplayStateChanged(true);
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_ADDED));
    waitMs(600);

    assertTranscript(
        "600ms windows from=10 changes=0x1 original wakeUp start=600 delay=10/550 main | src"
            + " 10\"Settings\" was - | A 10\"Settings\" was -",
        "1150ms windows from=10 changes=0x1 stable wakeUp start=600 delay=550/550 main | src"
            + " 10\"Settings\" was - | A 10\"Settings\" was -");
  }

  @Test
  public void volumePanelIsAnnounced() {
    openSettings();

    setWindows(
        statusBar(),
        navigationBar(),
        app(SETTINGS, "Settings").active().build(),
        window(VOLUME, AccessibilityWindowInfo.TYPE_SYSTEM, "Volume")
            .layer(6)
            .bounds(new Rect(900, 600, 1080, 1600))
            .build());
    AccessibilityEvent volume = stateChanged(VOLUME, "Volume", SYSTEM_UI_PACKAGE);
    volume.setClassName(VOLUME_DIALOG_CLASS);
    interpret(volume);
    waitMs(600);

    assertTranscript(
        "0ms state from=60 original start=0 delay=10/550 settled announcement=\"Volume\" from"
            + " com.android.systemui volume | src 60\"Volume\" was 10\"Settings\" | A"
            + " 10\"Settings\"",
        "10ms state from=60 stable start=0 delay=10/550 settled announcement=\"Volume\" from"
            + " com.android.systemui volume | src 60\"Volume\" was 10\"Settings\" | A"
            + " 10\"Settings\"");
  }

  @Test
  public void titleOfAWindowListedBeforeItsEventIsFound() {
    openSettings();
    assertEquals("Settings", interpreter.getWindowTitle(SETTINGS).toString());

    setWindows(
        statusBar(),
        navigationBar(),
        app(SETTINGS, "Settings").inactive().build(),
        app(DOWNLOADS, "Downloads").layer(2).active().build());

    assertEquals("Downloads", interpreter.getWindowTitle(DOWNLOADS).toString());
  }

  // Screens

  /** Puts Settings on screen, lets the interpreter take it in, and starts the transcript there. */
  private void openSettings() {
    setWindows(statusBar(), navigationBar(), app(SETTINGS, "Settings").active().build());
    interpret(windowsChanged(SETTINGS, WINDOWS_CHANGE_ADDED));
    waitMs(600);
    transcript.clear();
    serviceWindows.windowListRequests = 0;
    RootsShadow.rootRequests = 0;
    startTime = SystemClock.uptimeMillis();
  }

  private static AccessibilityWindowInfo statusBar() {
    return window(STATUS_BAR, AccessibilityWindowInfo.TYPE_SYSTEM, "Status bar")
        .layer(5)
        .bounds(new Rect(0, 0, 1080, 80))
        .build();
  }

  private static AccessibilityWindowInfo navigationBar() {
    return window(NAVIGATION_BAR, AccessibilityWindowInfo.TYPE_SYSTEM, "Navigation bar")
        .layer(5)
        .bounds(new Rect(0, 2280, 1080, 2400))
        .build();
  }

  private static WindowBuilder app(int id, @Nullable String title) {
    return window(id, AccessibilityWindowInfo.TYPE_APPLICATION, title);
  }

  private static WindowBuilder window(int id, int type, @Nullable String title) {
    return new WindowBuilder(id, type, title);
  }

  /** Makes a new window each time, as the windows Android lists are copies. */
  private static class WindowBuilder {
    private final int id;
    private final int type;
    private final @Nullable String title;
    private int layer = 1;
    private Rect bounds = SCREEN;
    private boolean active;
    private boolean pictureInPicture;
    private @Nullable String rootPackage;

    WindowBuilder(int id, int type, @Nullable String title) {
      this.id = id;
      this.type = type;
      this.title = title;
    }

    WindowBuilder layer(int layer) {
      this.layer = layer;
      return this;
    }

    WindowBuilder bounds(Rect bounds) {
      this.bounds = bounds;
      return this;
    }

    /** Active and focused, as the window the user is in is. */
    WindowBuilder active() {
      active = true;
      return this;
    }

    WindowBuilder inactive() {
      active = false;
      return this;
    }

    WindowBuilder pictureInPicture() {
      pictureInPicture = true;
      return this;
    }

    WindowBuilder rootPackage(String packageName) {
      rootPackage = packageName;
      return this;
    }

    AccessibilityWindowInfo build() {
      AccessibilityWindowInfo window = new AccessibilityWindowInfo();
      var shadow = shadowOf(window);
      shadow.setId(id);
      shadow.setType(type);
      shadow.setTitle(title);
      shadow.setLayer(layer);
      shadow.setBoundsInScreen(bounds);
      shadow.setActive(active);
      shadow.setFocused(active);
      shadow.setPictureInPicture(pictureInPicture);
      shadow.setDisplayId(Display.DEFAULT_DISPLAY);
      if (rootPackage != null) {
        AccessibilityNodeInfo root = new AccessibilityNodeInfo();
        root.setPackageName(rootPackage);
        shadow.setRoot(root);
      }
      return window;
    }
  }

  private void setWindows(AccessibilityWindowInfo... windows) {
    SparseArray<List<AccessibilityWindowInfo>> onAllDisplays = new SparseArray<>();
    onAllDisplays.put(Display.DEFAULT_DISPLAY, new ArrayList<>(Arrays.asList(windows)));
    serviceWindows.setWindows(onAllDisplays);
  }

  private void installApp(String packageName, String label) {
    PackageInfo packageInfo = new PackageInfo();
    packageInfo.packageName = packageName;
    packageInfo.applicationInfo = new ApplicationInfo();
    packageInfo.applicationInfo.packageName = packageName;
    packageInfo.applicationInfo.nonLocalizedLabel = label;
    shadowOf(service.getPackageManager()).installPackage(packageInfo);
  }

  // Events

  private void interpret(AccessibilityEvent event) {
    interpreter.interpret(event, /* eventId= */ null);
  }

  private static void waitMs(long ms) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));
  }

  private static AccessibilityEvent windowsChanged(int windowId, int changes) {
    AccessibilityEvent event = event(TYPE_WINDOWS_CHANGED, windowId);
    ReflectionHelpers.callInstanceMethod(
        event, "setWindowChanges", ClassParameter.from(int.class, changes));
    return event;
  }

  private static AccessibilityEvent stateChanged(
      int windowId, CharSequence text, String packageName) {
    AccessibilityEvent event = event(TYPE_WINDOW_STATE_CHANGED, windowId);
    event.getText().add(text);
    event.setPackageName(packageName);
    return event;
  }

  /** A pane opening or closing in the window, as Android reports it with the pane as source. */
  private static AccessibilityEvent pane(int windowId, int contentChangeType, String paneTitle) {
    AccessibilityEvent event = event(TYPE_WINDOW_STATE_CHANGED, windowId);
    event.setContentChangeTypes(contentChangeType);
    event.setPackageName(SETTINGS_PACKAGE);
    AccessibilityNodeInfo source = new AccessibilityNodeInfo();
    source.setPaneTitle(paneTitle);
    source.setPackageName(SETTINGS_PACKAGE);
    ShadowAccessibilityRecord shadow = Shadow.extract(event);
    shadow.setSourceNode(source);
    return event;
  }

  private static AccessibilityEvent event(int type, int windowId) {
    AccessibilityEvent event = new AccessibilityEvent(type);
    ReflectionHelpers.callInstanceMethod(
        event, "setWindowId", ClassParameter.from(int.class, windowId));
    event.setEventTime(SystemClock.uptimeMillis());
    return event;
  }

  // Transcript

  private void assertTranscript(String... expected) {
    assertEquals(String.join("\n", expected), String.join("\n", transcript));
  }

  /** Everything a listener can read from the interpretation, leaving out what is unset. */
  private String describe(EventInterpretation in) {
    StringBuilder line = new StringBuilder();
    line.append(SystemClock.uptimeMillis() - startTime).append("ms ");
    line.append(in.getEventType() == TYPE_WINDOWS_CHANGED ? "windows" : "state");
    line.append(" from=").append(windowId(in.getWindowIdFromEvent()));
    if (in.getDisplayId() != Display.DEFAULT_DISPLAY) {
      line.append(" display=").append(in.getDisplayId());
    }
    if (in.getWindowChangeTypes() != 0) {
      line.append(" changes=0x").append(Integer.toHexString(in.getWindowChangeTypes()));
    }
    if (in.getContentChangeTypes() != 0) {
      line.append(" content=0x").append(Integer.toHexString(in.getContentChangeTypes()));
    }
    flag(line, in.isOriginalEvent(), "original");
    flag(line, in.areWindowsStable(), "stable");
    flag(line, !in.isAllowAnnounce(), "silent");
    flag(line, in.isInterpretFirstTimeWhenWakeUp(), "wakeUp");
    flag(line, in.isWebDialogOpenedEvent(), "webDialog");
    flag(line, in.getSourceNode() != null, "source");
    long start = in.getEventStartTime();
    line.append(" start=").append((start == 0) ? 0 : start - startTime);
    line.append(" delay=").append(in.getTotalDelayMs()).append('/').append(in.getMaxDelayMs());
    flag(line, in.getMainWindowsChanged(), "main");
    flag(line, in.getPicInPicChanged(), "pipChanged");
    flag(line, in.getInputMethodChanged(), "imeChanged");
    flag(line, in.getShouldAnnounceInputMethodChange(), "announceIme");
    flag(line, in.getHorizontalPlacement(), "sideBySide");
    flag(line, in.hasTitlesFromStateChange(), "settled");
    @Nullable Announcement announcement = in.getAnnouncement();
    if (announcement != null) {
      line.append(" announcement=\"").append(announcement.text()).append("\" from ");
      line.append(announcement.packageName());
      flag(line, announcement.isFromVolumeControlPanel(), "volume");
      flag(line, announcement.isFromInputMethodEditor(), "keyboard");
    }
    if (in.getAnchorNodeRole() != 0) {
      line.append(" anchor=").append(in.getAnchorNodeRole());
    }
    if (in.getSourceBoundsInScreen() != null) {
      line.append(" bounds=").append(in.getSourceBoundsInScreen().toShortString());
    }
    window(line, "src", in.getEventSourceWindow());
    window(line, "A", in.getWindowA());
    window(line, "B", in.getWindowB());
    window(line, "other", in.getOtherActiveWindow());
    window(line, "pip", in.getPicInPic());
    window(line, "ime", in.getInputMethod());
    return line.toString();
  }

  /**
   * Describes the window in one role. A role with no window, which a listener sees as untitled, is
   * left out unless it just lost its window.
   */
  private void window(StringBuilder line, String role, WindowInterpretation window) {
    boolean changed = window.idOrTitleChanged();
    if (window.getId() == WINDOW_ID_NONE
        && window.getTitle() == null
        && !changed
        && TextUtils.equals(window.getTitleForFeedback(), untitled())) {
      return;
    }
    line.append(" | ").append(role).append(' ').append(windowId(window.getId()));
    title(line, window.getTitle());
    if (!TextUtils.equals(window.getTitleForFeedback(), window.getTitle())) {
      line.append(" feedback ");
      title(line, window.getTitleForFeedback());
    }
    if (changed) {
      flag(line, window.hasTitleFromStateChange(), "settled");
      line.append(" was ").append(windowId(window.getOldId()));
      title(line, window.getOldTitle());
    }
  }

  private String untitled() {
    return service.getString(com.google.android.accessibility.utils.R.string.untitled_window);
  }

  private static void title(StringBuilder line, @Nullable CharSequence title) {
    if (title != null) {
      line.append('"').append(title).append('"');
    }
  }

  private static String windowId(int id) {
    return (id == WINDOW_ID_NONE) ? "-" : String.valueOf(id);
  }

  private static void flag(StringBuilder line, boolean set, String name) {
    if (set) {
      line.append(' ').append(name);
    }
  }
}
