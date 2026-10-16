/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.braille.brailledisplay.platform.external;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserManager;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.SupportedCommand;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.BrailleKeyBindingUtils;
import com.google.android.accessibility.braille.brailledisplay.platform.Controller;
import com.google.android.accessibility.braille.brailledisplay.platform.DisplayerInterface;
import com.google.android.accessibility.braille.brailledisplay.platform.external.ExternalDisplayApps.State;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import fyi.quin.backtalk.braille.IBrailleDisplay;
import fyi.quin.backtalk.braille.IBrailleDisplayHost;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Braille displays that other apps provide. Backtalk binds the display service of each app that the
 * user has allowed in Backtalk, and asks about apps it hasn't been told about. One display at a time
 * drives the braille controller. A display connected through BRLTTY comes first: while one is, the
 * others wait, and one that is in use steps aside when it connects. See
 * docs/braille-display-api.md.
 */
public final class ExternalBrailleDisplays {
  private static final String TAG = "ExternalBrailleDisplays";

  /** A display app sends this to Backtalk to have it look for displays again. */
  public static final String ACTION_BRAILLE_DISPLAYS_CHANGED =
      "fyi.quin.backtalk.action.BRAILLE_DISPLAYS_CHANGED";

  private static final long UNLOCK_CHECK_MS = 1000;

  private final Context context;
  private final Controller controller;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final Map<ComponentName, Connection> connections = new HashMap<>();
  private Connection active;
  private boolean started;
  // A display connected through BRLTTY is in use, so no display from an app can be.
  private boolean brlttyStarted;
  private final SharedPreferences approvals;
  // Whatever the user allows or stops allowing takes effect at once.
  private final OnSharedPreferenceChangeListener approvalsChanged =
      (prefs, key) -> {
        if (ExternalDisplayApps.PREF_ALLOWED.equals(key)
            || ExternalDisplayApps.PREF_DENIED.equals(key)) {
          refresh();
        }
      };
  // Looks again as soon as the user is unlocked; see refresh().
  private final Runnable refreshWhenUnlocked = this::refresh;

  private final BroadcastReceiver displaysChanged =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String packageName =
              intent.getData() == null ? null : intent.getData().getSchemeSpecificPart();
          // Apps change all the time, so look again only when one that can be a display changed.
          if (packageName == null || isDisplayPackage(packageName)) {
            refresh();
          }
        }
      };

  public ExternalBrailleDisplays(Context context, Controller controller) {
    this.context = context;
    this.controller = controller;
    this.approvals = ExternalDisplayApps.prefs(context);
  }

  /**
   * The controller that BRLTTY's displays are to drive in place of the braille controller itself, so
   * that the two kinds of display take turns: a display from an app steps aside while a BRLTTY one is
   * in use, and isn't stopped by BRLTTY when it isn't.
   */
  public Controller brlttyController() {
    return new Controller() {
      @Override
      public void onStart(DisplayerInterface displayer) {
        brlttyStarted = true;
        if (active != null) {
          active.waiting = true;
          stopActive();
        }
        controller.onStart(displayer);
      }

      @Override
      public void onStop() {
        if (brlttyStarted) {
          brlttyStarted = false;
          controller.onStop();
          activateWaiting();
        } else if (active == null) {
          // Nothing is in use, so this changes nothing a display from an app uses.
          controller.onStop();
        }
      }

      @Override
      public void onDestroy() {
        controller.onDestroy();
      }

      @Override
      public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        controller.onAccessibilityEvent(accessibilityEvent);
      }

      @Override
      public void onBrailleInputEvent(BrailleInputEvent brailleInputEvent) {
        controller.onBrailleInputEvent(brailleInputEvent);
      }

      @Override
      public void onReadingControlSettingsChanged(CharSequence readingControlDescription) {
        controller.onReadingControlSettingsChanged(readingControlDescription);
      }

      @Override
      public void onReadingControlValueChanged() {
        controller.onReadingControlValueChanged();
      }
    };
  }

  /** Binds the displays of the apps that the user has allowed. */
  public void start() {
    if (started) {
      return;
    }
    started = true;
    IntentFilter packages = new IntentFilter();
    packages.addAction(Intent.ACTION_PACKAGE_ADDED);
    packages.addAction(Intent.ACTION_PACKAGE_CHANGED);
    packages.addAction(Intent.ACTION_PACKAGE_REPLACED);
    packages.addAction(Intent.ACTION_PACKAGE_REMOVED);
    packages.addDataScheme("package");
    ContextCompat.registerReceiver(
        context, displaysChanged, packages, ContextCompat.RECEIVER_NOT_EXPORTED);
    // Apps that can't run until the user is unlocked aren't found before then; see refresh().
    ContextCompat.registerReceiver(
        context,
        displaysChanged,
        new IntentFilter(Intent.ACTION_USER_UNLOCKED),
        ContextCompat.RECEIVER_NOT_EXPORTED);
    approvals.registerOnSharedPreferenceChangeListener(approvalsChanged);
    // Only an app that holds the permission can send this.
    ContextCompat.registerReceiver(
        context,
        displaysChanged,
        new IntentFilter(ACTION_BRAILLE_DISPLAYS_CHANGED),
        ExternalDisplayApps.permissionBrailleDisplay(context),
        /* scheduler= */ null,
        ContextCompat.RECEIVER_EXPORTED);
    refresh();
  }

  /** Unbinds every display. */
  public void stop() {
    if (!started) {
      return;
    }
    started = false;
    mainHandler.removeCallbacks(refreshWhenUnlocked);
    context.unregisterReceiver(displaysChanged);
    approvals.unregisterOnSharedPreferenceChangeListener(approvalsChanged);
    for (Connection connection : connections.values()) {
      connection.unbind();
    }
    connections.clear();
    stopActive();
  }

  /**
   * Passes an event on to the controller, so that a display with cells shows what changed. A
   * display connected through BRLTTY gets events already, and while one is, none from an app is in
   * use.
   */
  public void onAccessibilityEvent(AccessibilityEvent event) {
    if (active != null && active.cellCount > 0) {
      controller.onAccessibilityEvent(event);
    }
  }

  /**
   * Binds newly allowed displays, unbinds those that are no longer allowed or installed, and asks
   * about apps the user hasn't answered for.
   */
  private void refresh() {
    if (!started) {
      return;
    }
    mainHandler.removeCallbacks(refreshWhenUnlocked);
    if (!context.getSystemService(UserManager.class).isUserUnlocked()) {
      // After a reboot, Backtalk starts before the user is unlocked, and a display app that can't
      // run until then isn't found. The broadcast that says the user is unlocked can come many
      // seconds later, behind the rest of the boot, so if such a display is waiting, check every
      // second.
      if (hasAllowedDisplays(
          PackageManager.MATCH_DIRECT_BOOT_AWARE | PackageManager.MATCH_DIRECT_BOOT_UNAWARE)) {
        mainHandler.postDelayed(refreshWhenUnlocked, UNLOCK_CHECK_MS);
      }
    }
    Set<ComponentName> allowed = allowedDisplays(0, /* ask= */ true);
    Iterator<Map.Entry<ComponentName, Connection>> it = connections.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<ComponentName, Connection> entry = it.next();
      if (!allowed.contains(entry.getKey())) {
        stopDisplay(entry.getValue());
        entry.getValue().unbind();
        it.remove();
      }
    }
    for (ComponentName component : allowed) {
      if (!connections.containsKey(component)) {
        Connection connection = new Connection(component);
        if (connection.bind()) {
          connections.put(component, connection);
        }
      }
    }
  }

  private boolean isDisplayPackage(String packageName) {
    for (ComponentName component : connections.keySet()) {
      if (component.getPackageName().equals(packageName)) {
        return true;
      }
    }
    return ExternalDisplayApps.holdsPermission(context, packageName);
  }

  private boolean hasAllowedDisplays(int flags) {
    return !allowedDisplays(flags, /* ask= */ false).isEmpty();
  }

  /** The display services of the apps the user allowed; {@code ask} asks about the others. */
  private Set<ComponentName> allowedDisplays(int flags, boolean ask) {
    Set<ComponentName> allowed = new HashSet<>();
    for (Map.Entry<String, ComponentName> service :
        ExternalDisplayApps.displayServices(context, flags).entrySet()) {
      State state = ExternalDisplayApps.state(approvals, service.getKey());
      if (state == State.ALLOWED) {
        allowed.add(service.getValue());
      } else if (state == State.UNKNOWN && ask) {
        ExternalDisplayApproval.ask(context, service.getKey());
      }
    }
    return allowed;
  }

  private boolean isAllowed(String app) {
    return ExternalDisplayApps.holdsPermission(context, app)
        && ExternalDisplayApps.state(approvals, app) == State.ALLOWED;
  }

  private void startDisplay(Connection connection, String name, int cellCount) {
    connection.name = name;
    connection.cellCount = Math.max(cellCount, 0);
    connection.waiting = true;
    if (active == connection) {
      stopActive();
    }
    if (active == null) {
      activateWaiting();
    }
  }

  private void stopDisplay(Connection connection) {
    connection.waiting = false;
    if (active == connection) {
      stopActive();
      activateWaiting();
    }
  }

  /** Uses a display that has started and waits, unless one is in use, from BRLTTY or an app. */
  private void activateWaiting() {
    if (active != null || brlttyStarted || !started) {
      return;
    }
    for (Connection waiting : connections.values()) {
      if (waiting.waiting && waiting.display != null) {
        active = waiting;
        ExternalDisplayApproval.showInUse(context, waiting.component.getPackageName());
        controller.onStart(new ExternalDisplayer(waiting.name, waiting.cellCount, waiting.display));
        return;
      }
    }
  }

  private void stopActive() {
    if (active != null) {
      active = null;
      ExternalDisplayApproval.hideInUse(context);
      controller.onStop();
    }
  }

  private void input(Connection connection, BrailleInputEvent event) {
    if (active == connection && event != null) {
      controller.onBrailleInputEvent(event);
    }
  }

  /** Braille keys run the command they are bound to, as on any display, or else type. */
  private BrailleInputEvent brailleKeysEvent(int dots, boolean space) {
    long time = SystemClock.uptimeMillis();
    if (dots == 0) {
      return space ? new BrailleInputEvent(BrailleInputEvent.CMD_BRAILLE_KEY, 0, time) : null;
    }
    SupportedCommand command = BrailleKeyBindingUtils.convertToCommand(context, space, dots);
    if (command != null) {
      return new BrailleInputEvent(command.getCommand(), 0, time);
    }
    return space ? null : new BrailleInputEvent(BrailleInputEvent.CMD_BRAILLE_KEY, dots, time);
  }

  private static BrailleInputEvent commandEvent(String name, int argument) {
    Integer command = Commands.CODES.get(name);
    if (command == null) {
      BrailleDisplayLog.w(TAG, "unknown command " + name);
      return null;
    }
    return new BrailleInputEvent(command, argument, SystemClock.uptimeMillis());
  }

  /** The command codes of {@link BrailleInputEvent}, by name without the CMD_ prefix. */
  private static final class Commands {
    static final Map<String, Integer> CODES = new HashMap<>();

    static {
      for (Field field : BrailleInputEvent.class.getFields()) {
        int modifiers = field.getModifiers();
        if (field.getName().startsWith("CMD_")
            && field.getType() == int.class
            && Modifier.isStatic(modifiers)) {
          try {
            CODES.put(field.getName().substring(4), field.getInt(null));
          } catch (IllegalAccessException e) {
            // Public constants are always readable.
          }
        }
      }
    }
  }

  /** The binding to one display app's service. */
  private final class Connection implements ServiceConnection {
    final ComponentName component;
    IBrailleDisplay display;
    String name;
    int cellCount;
    // Started by its app, whether or not it is the display in use.
    boolean waiting;
    private boolean bound;

    Connection(ComponentName component) {
      this.component = component;
    }

    boolean bind() {
      Intent intent = new Intent(ExternalDisplayApps.ACTION_BRAILLE_DISPLAY).setComponent(component);
      bound = context.bindService(intent, this, Context.BIND_AUTO_CREATE);
      if (!bound) {
        BrailleDisplayLog.w(TAG, "couldn't bind " + component.flattenToShortString());
      }
      return bound;
    }

    void unbind() {
      if (bound) {
        bound = false;
        context.unbindService(this);
      }
      display = null;
    }

    /**
     * Whether this is still the app's binding. Android can still deliver calls it queued for one
     * that has been replaced, such as while the app is being updated.
     */
    private boolean isCurrent() {
      return connections.get(component) == this;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      if (!isCurrent()) {
        unbind();
        return;
      }
      // The app may have stopped being allowed while Android started its service again for this
      // binding, so check again.
      if (!isAllowed(component.getPackageName())) {
        unbind();
        connections.remove(component);
        return;
      }
      display = IBrailleDisplay.Stub.asInterface(binder);
      try {
        display.onHostConnected(new Host(this));
      } catch (RemoteException e) {
        BrailleDisplayLog.w(TAG, "couldn't connect " + name.flattenToShortString(), e);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      if (!isCurrent()) {
        return;
      }
      // The app stopped; Android binds it again when it can.
      display = null;
      stopDisplay(this);
    }

    @Override
    public void onBindingDied(ComponentName name) {
      if (!isCurrent()) {
        unbind();
        return;
      }
      // The app was updated or force stopped, and this binding won't come back.
      stopDisplay(this);
      unbind();
      connections.remove(component);
      refresh();
    }
  }

  /** What a display app calls. Calls from a display that has since been unbound are ignored. */
  private final class Host extends IBrailleDisplayHost.Stub {
    private final Connection connection;

    Host(Connection connection) {
      this.connection = connection;
    }

    private void post(Runnable runnable) {
      mainHandler.post(
          () -> {
            if (connection.bound && connections.get(connection.component) == connection) {
              runnable.run();
            }
          });
    }

    @Override
    public void start(String name, int cellCount) {
      post(() -> startDisplay(connection, name == null ? "" : name, cellCount));
    }

    @Override
    public void stop() {
      post(() -> stopDisplay(connection));
    }

    @Override
    public void brailleKeys(int dots, boolean space) {
      post(() -> input(connection, brailleKeysEvent(dots & 0xFF, space)));
    }

    @Override
    public void command(String name, int argument) {
      post(() -> input(connection, commandEvent(name, argument)));
    }
  }
}
