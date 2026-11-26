/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.google.android.accessibility.utils.preference;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.R;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Preference activity used. This base class is separate from PreferencesActivity to allow partner
 * code (for open-source) to inherit from AppCompatActivity.
 */
public abstract class BasePreferencesActivity extends AppCompatActivity {

  // This variable are used as arguments of Intent to identify the fragment which should be created.
  public static final String FRAGMENT_NAME = "FragmentName";
  public static final String FRAGMENT_ARGS = "FragmentArgs";

  private static final int DEFAULT_CONTAINER_ID = android.R.id.content;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (SettingsHelp.getProvider() == null) {
      return;
    }
    addMenuProvider(new HelpMenu());
    // The action follows the screen shown.
    getSupportFragmentManager()
        .registerFragmentLifecycleCallbacks(
            new FragmentManager.FragmentLifecycleCallbacks() {
              @Override
              public void onFragmentResumed(@NonNull FragmentManager manager, @NonNull Fragment fragment) {
                invalidateOptionsMenu();
              }
            },
            /* recursive= */ false);
  }

  /** The Help action of the screen shown, when it has help. */
  private final class HelpMenu implements MenuProvider {
    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
      SettingsHelp.Provider provider = SettingsHelp.getProvider();
      if (provider == null || helpIntent(provider) == null) {
        return;
      }
      menu.add(Menu.NONE, R.id.settings_help, Menu.NONE, provider.title(BasePreferencesActivity.this))
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
      if (menuItem.getItemId() != R.id.settings_help) {
        return false;
      }
      SettingsHelp.Provider provider = SettingsHelp.getProvider();
      Intent intent = provider == null ? null : helpIntent(provider);
      if (intent != null) {
        try {
          startActivity(intent);
        } catch (ActivityNotFoundException e) {
          // Nothing to open.
        }
      }
      return true;
    }

    private @Nullable Intent helpIntent(SettingsHelp.Provider provider) {
      Fragment screen = getSupportFragmentManager().findFragmentById(getContainerId());
      return provider.helpIntent(BasePreferencesActivity.this, screen);
    }
  }

  /**
   * If action-bar back key button is pressed, end this sub-activity when there is no fragment in
   * the stack. Otherwise, it will go to last fragment.
   */
  @Override
  public void onBackPressed() {
    // Closes the activity if there is no fragment inside the stack. Otherwise the activity will has
    // a blank screen since there is no any fragment. onBackPressed() in Activity.java only handles
    // popBackStackImmediate(). This will close activity to avoid a blank screen.
    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
      finishAfterTransition();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    readActionBarFirst();
  }

  /**
   * Keeps the action bar, with Navigate up and the screen's title, first in the reading order.
   * Android 15 and later draw the screen edge to edge, so the content starts at the top of the
   * screen, under the action bar, rather than below it. Both then start at the same place, and
   * Android puts the taller content first, so the action bar came last. Android only passes on the
   * order between views it gives to accessibility services, so both containers are given, though
   * they have nothing to focus themselves.
   */
  private void readActionBarFirst() {
    View actionBar = findViewById(androidx.appcompat.R.id.action_bar_container);
    View content = findViewById(android.R.id.content);
    if (actionBar == null || content == null) {
      return;
    }
    actionBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    content.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    actionBar.setAccessibilityTraversalBefore(android.R.id.content);
  }

  /**
   * Prepares action bar
   *
   * @param icon Icon shows on action bar.
   */
  protected void prepareActionBar(@Nullable Drawable icon) {
    ActionBar actionBar = getSupportActionBar();
    if (FeatureSupport.isWatch(getApplicationContext())) {
      if (actionBar != null) {
        actionBar.hide();
      }
      return;
    }

    if (actionBar != null) {
      if (icon != null) {
        actionBar.setIcon(icon);
      }
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    if (FeatureSupport.isTv(getApplicationContext())) {
      hideBackButton();
    }
  }

  /** Hides the back button (or home) inside the action bar */
  private void hideBackButton() {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(false);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Gets Identifier of the container whose fragment(s) at the activity should use.
   *
   * @return The fragments container in the activity.
   */
  protected int getContainerId() {
    return DEFAULT_CONTAINER_ID;
  }

  /** Return {@code true} if the ancestor already handles fragment transaction. */
  protected boolean isDefaultFragmentTransactionHandled() {
    return false;
  }
}
