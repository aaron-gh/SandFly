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

package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.speech.VoiceProfileNames;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech;
import com.google.android.accessibility.utils.output.VoiceProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The voice profiles: the one in use, then Backtalk default and each profile in the order the
 * reading control goes through them, then a way to add one. A profile opens its settings when
 * tapped. It can be moved, renamed and deleted with actions from a screen reader, or renamed and
 * deleted from the menu that a long press shows. Holding a profile and dragging it moves it.
 * Nothing moves above Backtalk default, which uses the text-to-speech settings.
 *
 * <p>Watches draw settings rows with Compose, so there the rows are ordinary preferences, without
 * dragging or actions, and a profile moves up and down from its own settings.
 */
public class VoiceProfilesFragment extends TalkbackBaseFragment {

  private SharedPreferences prefs;
  /** Each profile's name by ID, read again whenever the rows are. */
  private Map<String, String> names = Map.of();
  private AccessibilitySuiteListPreference inUse;

  /** Shows the profile in use when a gesture, keyboard shortcut or the Backtalk menu switches it. */
  private final SharedPreferences.OnSharedPreferenceChangeListener activeListener =
      (sharedPrefs, key) -> {
        if (VoiceProfiles.PREF_ACTIVE.equals(key) && inUse != null) {
          inUse.setValue(VoiceProfiles.activeId(prefs));
        }
      };
  private ProfilesAdapter profiles = new ProfilesAdapter();
  private final boolean wear = FormFactorUtils.isAndroidWear();

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_voice_profiles);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    Context context = requireContext();
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    // Backtalk's settings are in device protected storage. Without this, the profile in use would
    // be saved to a second settings file, which the service moves over the real one when it
    // starts, losing every other setting.
    getPreferenceManager().setStorageDeviceProtected();
    PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
    inUse = new AccessibilitySuiteListPreference(context);
    inUse.setKey(VoiceProfiles.PREF_ACTIVE);
    inUse.setTitle(R.string.title_pref_voice_profile_in_use);
    inUse.setDialogTitle(R.string.title_pref_voice_profile_in_use);
    inUse.setDefaultValue("");
    inUse.setSummary("%s");
    inUse.setIconSpaceReserved(false);
    screen.addPreference(inUse);
    setPreferenceScreen(screen);
    names = VoiceProfileNames.names(context, prefs);
    updateInUse();
  }

  /**
   * The profile rows aren't preferences: the preference list redraws itself a moment after any
   * change, which would end a drag part way through. Each list gets its own row adapter, since an
   * adapter keeps every list it was joined to, and the list is made again each time this screen
   * comes back from a profile's settings.
   */
  @Override
  protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
    if (wear) {
      return super.onCreateAdapter(preferenceScreen);
    }
    profiles = new ProfilesAdapter();
    profiles.reload();
    return new ConcatAdapter(super.onCreateAdapter(preferenceScreen), profiles);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (!wear) {
      new ItemTouchHelper(new DragCallback()).attachToRecyclerView(getListView());
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    // A profile may have been renamed, removed or switched to meanwhile.
    VoiceProfileNames.saveNames(requireContext(), prefs);
    reloadRows();
    updateInUse();
    prefs.registerOnSharedPreferenceChangeListener(activeListener);
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(activeListener);
  }

  private void reloadRows() {
    names = VoiceProfileNames.names(requireContext(), prefs);
    if (wear) {
      addWatchRows();
    } else {
      profiles.reload();
    }
  }

  /** Lists Backtalk default, the profiles and the add row as preferences, on a watch. */
  private void addWatchRows() {
    Context context = requireContext();
    PreferenceScreen screen = getPreferenceScreen();
    for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
      Preference preference = screen.getPreference(i);
      if (preference != inUse) {
        screen.removePreference(preference);
      }
    }
    // Backtalk default's settings are the text-to-speech settings this screen came from.
    screen.addPreference(
        watchRow(
            context,
            getString(R.string.voice_profile_default),
            null,
            () -> requireActivity().getOnBackPressedDispatcher().onBackPressed()));
    for (String id : VoiceProfiles.ids(prefs)) {
      screen.addPreference(
          watchRow(
              context,
              nameOf(id),
              FailoverTextToSpeech.getEngineDisplayName(
                  context, VoiceProfiles.read(prefs, id).engine()),
              () -> open(id)));
    }
    screen.addPreference(
        watchRow(
            context, getString(R.string.title_pref_add_voice_profile), null, this::askForNewName));
  }

  private static Preference watchRow(
      Context context, CharSequence title, @Nullable CharSequence summary, Runnable onClick) {
    Preference row = new AccessibilitySuitePreference(context);
    row.setTitle(title);
    row.setSummary(summary);
    row.setPersistent(false);
    row.setIconSpaceReserved(false);
    row.setOnPreferenceClickListener(
        preference -> {
          onClick.run();
          return true;
        });
    return row;
  }

  /** Lists Backtalk default and the profiles, in order, as the profiles to choose from. */
  private void updateInUse() {
    List<String> ids = VoiceProfiles.ids(prefs);
    CharSequence[] entries = new CharSequence[ids.size() + 1];
    CharSequence[] values = new CharSequence[ids.size() + 1];
    entries[0] = getString(R.string.voice_profile_default);
    values[0] = "";
    for (int i = 0; i < ids.size(); i++) {
      entries[i + 1] = nameOf(ids.get(i));
      values[i + 1] = ids.get(i);
    }
    inUse.setEntries(entries);
    inUse.setEntryValues(values);
    // A removed profile's ID may still be saved as the one in use.
    inUse.setValue(VoiceProfiles.activeId(prefs));
  }

  /** Returns the name of profile {@code id}, or Backtalk default's for an empty ID. */
  private String nameOf(String id) {
    if (id.isEmpty()) {
      return getString(R.string.voice_profile_default);
    }
    String name = names.get(id);
    return (name == null) ? VoiceProfileNames.nameOf(requireContext(), prefs, id) : name;
  }

  private void open(String id) {
    Preference preference = new Preference(requireContext());
    preference.setFragment(VoiceProfileFragment.class.getName());
    preference.getExtras().putString(VoiceProfileFragment.ARG_PROFILE_ID, id);
    onPreferenceTreeClick(preference);
  }

  private void askForNewName() {
    Context context = requireContext();
    askForName(
        context,
        prefs,
        R.string.title_pref_add_voice_profile,
        /* id= */ "",
        VoiceProfileNames.unusedName(context, prefs),
        this::add);
  }

  private void askForRename(String id) {
    askForName(
        requireContext(),
        prefs,
        R.string.voice_profile_rename,
        id,
        nameOf(id),
        name -> {
          if (VoiceProfiles.rename(prefs, id, name, getString(R.string.voice_profile_default))) {
            reloadRows();
            updateInUse();
          }
        });
  }

  /** Receives a name from {@link #askForName}, never blank and never another profile's. */
  interface NameListener {
    void onName(String name);
  }

  /**
   * Asks for the name of profile {@code id}, or of a new profile if {@code id} is empty, starting
   * with {@code name}. OK can't be pressed while the name is blank or another profile's, and the
   * field says when it is another profile's, since every profile needs a name of its own.
   */
  static void askForName(
      Context context,
      SharedPreferences prefs,
      int titleResId,
      String id,
      String name,
      NameListener listener) {
    String defaultName = context.getString(R.string.voice_profile_default);
    EditText field = new EditText(context);
    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    field.setSingleLine(true);
    field.setFilters(
        new InputFilter[] {new InputFilter.LengthFilter(VoiceProfiles.MAX_NAME_LENGTH)});
    field.setText(name);
    field.selectAll();
    AlertDialog dialog =
        new AlertDialog.Builder(context)
            .setTitle(titleResId)
            .setView(padded(context, field))
            .setPositiveButton(
                android.R.string.ok,
                (shown, which) -> {
                  String entered = field.getText().toString().trim();
                  if (!entered.isEmpty()
                      && !VoiceProfiles.isNameTaken(prefs, id, entered, defaultName)) {
                    listener.onName(entered);
                  }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    Runnable check =
        () -> {
          String entered = field.getText().toString().trim();
          boolean taken =
              !entered.isEmpty() && VoiceProfiles.isNameTaken(prefs, id, entered, defaultName);
          ok.setEnabled(!entered.isEmpty() && !taken);
          field.setError(
              taken ? context.getString(R.string.voice_profile_name_taken, entered) : null);
        };
    check.run();
    field.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence text, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable text) {
            check.run();
          }
        });
    field.requestFocus();
  }

  /** Adds a profile that sounds like Backtalk's default to begin with, and opens its settings. */
  private void add(String name) {
    Context context = requireContext();
    String engine = FailoverTextToSpeech.getSelectedEngine(context);
    List<String> installed =
        FailoverTextToSpeech.getInstalledTtsEngines(context.getPackageManager());
    if ((engine == null || !installed.contains(engine)) && !installed.isEmpty()) {
      engine = installed.get(0);
    }
    String id =
        VoiceProfiles.create(
            prefs,
            name,
            engine == null ? "" : engine,
            prefs.getString(
                getString(R.string.pref_speech_volume_key),
                getString(R.string.pref_speech_volume_default)),
            prefs.getString(
                getString(R.string.pref_speech_rate_key),
                getString(R.string.pref_speech_rate_default)),
            prefs.getString(
                getString(R.string.pref_speech_pitch_key),
                getString(R.string.pref_speech_pitch_default)),
            prefs.getBoolean(
                getString(R.string.pref_speak_in_phrases_key),
                getResources().getBoolean(R.bool.pref_speak_in_phrases_default)));
    reloadRows();
    updateInUse();
    open(id);
  }

  private void confirmDelete(String id) {
    String name = nameOf(id);
    new AlertDialog.Builder(requireContext())
        .setMessage(getString(R.string.voice_profile_delete_confirm, name))
        .setPositiveButton(
            R.string.voice_profile_delete,
            (dialog, which) -> {
              VoiceProfiles.delete(prefs, id);
              if (wear) {
                addWatchRows();
              } else {
                profiles.remove(id);
              }
              updateInUse();
              getListView()
                  .announceForAccessibility(getString(R.string.voice_profile_deleted, name));
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** The menu a long press shows, for those who don't use a screen reader's actions. */
  private void showMenu(View anchor, String id) {
    PopupMenu menu = new PopupMenu(requireContext(), anchor);
    menu.getMenu().add(Menu.NONE, R.string.voice_profile_rename, 0, R.string.voice_profile_rename);
    menu.getMenu().add(Menu.NONE, R.string.voice_profile_delete, 1, R.string.voice_profile_delete);
    menu.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == R.string.voice_profile_rename) {
            askForRename(id);
          } else {
            confirmDelete(id);
          }
          return true;
        });
    menu.show();
  }

  /** Lines a dialog's text field up with its title. */
  static FrameLayout padded(Context context, EditText field) {
    int padding = (int) (24 * context.getResources().getDisplayMetrics().density);
    FrameLayout container = new FrameLayout(context);
    container.setPadding(padding, 0, padding, 0);
    container.addView(field);
    return container;
  }

  /** A row that looks like any other setting. */
  private static final class RowHolder extends RecyclerView.ViewHolder {
    final TextView title;
    final TextView summary;
    final List<Integer> actionIds = new ArrayList<>();
    @Nullable String id;
    @Nullable Drawable background;

    RowHolder(View itemView) {
      super(itemView);
      title = itemView.findViewById(android.R.id.title);
      summary = itemView.findViewById(android.R.id.summary);
      hide(itemView.findViewById(androidx.preference.R.id.icon_frame));
      hide(itemView.findViewById(android.R.id.icon_frame));
      hide(itemView.findViewById(android.R.id.widget_frame));
    }

    private static void hide(@Nullable View view) {
      if (view != null) {
        view.setVisibility(View.GONE);
      }
    }

    /** Whether this row is a profile, rather than Backtalk default or the add row. */
    boolean isProfile() {
      return id != null && !id.isEmpty();
    }
  }

  /** Backtalk default, then the profiles, then the add row. */
  private final class ProfilesAdapter extends RecyclerView.Adapter<RowHolder> {
    /** Backtalk default's ID, which is empty, then the profiles' IDs. */
    private final List<String> rows = new ArrayList<>();

    void reload() {
      rows.clear();
      if (prefs != null) {
        rows.add("");
        rows.addAll(VoiceProfiles.ids(prefs));
      }
      notifyDataSetChanged();
    }

    void remove(String id) {
      int position = rows.indexOf(id);
      if (position >= 0) {
        rows.remove(position);
        notifyItemRemoved(position);
        updateActions();
      }
    }

    /** Moves the profile at {@code from} to {@code to}, and says where it went. */
    void move(int from, int to, View announcer) {
      String id = rows.remove(from);
      rows.add(to, id);
      notifyItemMoved(from, to);
      String neighbour = (to < from) ? rows.get(to + 1) : rows.get(to - 1);
      announcer.announceForAccessibility(
          getString(
              (to < from) ? R.string.voice_profile_moved_above : R.string.voice_profile_moved_below,
              nameOf(neighbour)));
    }

    void saveOrder() {
      VoiceProfiles.setOrder(prefs, rows.subList(1, rows.size()));
      updateInUse();
    }

    boolean isProfileRow(int position) {
      return position > 0 && position < rows.size();
    }

    @Override
    public int getItemCount() {
      return rows.isEmpty() ? 0 : rows.size() + 1;
    }

    @Override
    public RowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      Context context = parent.getContext();
      int layout = new AccessibilitySuitePreference(context).getLayoutResource();
      RowHolder holder =
          new RowHolder(
              LayoutInflater.from(context).inflate(layout, parent, /* attachToRoot= */ false));
      holder.itemView.setOnClickListener(
          view -> {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
              return;
            }
            if (position == rows.size()) {
              askForNewName();
            } else if (position == 0) {
              // Backtalk default's settings are the text-to-speech settings this screen came from.
              requireActivity().getOnBackPressedDispatcher().onBackPressed();
            } else {
              open(rows.get(position));
            }
          });
      return holder;
    }

    @Override
    public void onBindViewHolder(RowHolder holder, int position) {
      holder.id = (position < rows.size()) ? rows.get(position) : null;
      CharSequence summary = null;
      if (holder.id == null) {
        holder.title.setText(R.string.title_pref_add_voice_profile);
      } else if (holder.id.isEmpty()) {
        holder.title.setText(R.string.voice_profile_default);
      } else {
        String engine = VoiceProfiles.read(prefs, holder.id).engine();
        holder.title.setText(nameOf(holder.id));
        summary = FailoverTextToSpeech.getEngineDisplayName(holder.itemView.getContext(), engine);
      }
      if (holder.summary != null) {
        holder.summary.setText(summary);
        holder.summary.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
      }
      bindActions(holder, position);
    }

    /** Gives a profile's row its actions, which depend on its {@code position} in the list. */
    void bindActions(RowHolder holder, int position) {
      View view = holder.itemView;
      for (int actionId : holder.actionIds) {
        ViewCompat.removeAccessibilityAction(view, actionId);
      }
      holder.actionIds.clear();
      if (!holder.isProfile() || position == RecyclerView.NO_POSITION) {
        return;
      }
      // Nothing moves above Backtalk default.
      if (position > 1) {
        holder.actionIds.add(
            ViewCompat.addAccessibilityAction(
                view,
                getString(R.string.voice_profile_move_up),
                (host, arguments) -> moveByAction(holder, -1)));
      }
      if (position < rows.size() - 1) {
        holder.actionIds.add(
            ViewCompat.addAccessibilityAction(
                view,
                getString(R.string.voice_profile_move_down),
                (host, arguments) -> moveByAction(holder, 1)));
      }
      holder.actionIds.add(
          ViewCompat.addAccessibilityAction(
              view,
              getString(R.string.voice_profile_rename),
              (host, arguments) -> {
                if (holder.isProfile()) {
                  askForRename(holder.id);
                }
                return true;
              }));
      holder.actionIds.add(
          ViewCompat.addAccessibilityAction(
              view,
              getString(R.string.voice_profile_delete),
              (host, arguments) -> {
                if (holder.isProfile()) {
                  confirmDelete(holder.id);
                }
                return true;
              }));
    }

    private boolean moveByAction(RowHolder holder, int step) {
      int from = holder.getBindingAdapterPosition();
      int to = from + step;
      if (!isProfileRow(from) || !isProfileRow(to)) {
        return false;
      }
      move(from, to, holder.itemView);
      saveOrder();
      updateActions();
      return true;
    }

    /** Updates the actions of the rows on screen after the order changes. */
    void updateActions() {
      RecyclerView list = getListView();
      if (list == null) {
        return;
      }
      for (int i = 0; i < list.getChildCount(); i++) {
        RecyclerView.ViewHolder holder = list.getChildViewHolder(list.getChildAt(i));
        if (holder instanceof RowHolder row && holder.getBindingAdapter() == this) {
          bindActions(row, row.getBindingAdapterPosition());
        }
      }
    }
  }

  /**
   * Moves a profile when it is held and dragged. Holding one and letting go without moving it shows
   * its menu instead.
   */
  private final class DragCallback extends ItemTouchHelper.Callback {
    private boolean moved;

    private boolean isProfile(RecyclerView.ViewHolder holder) {
      return holder instanceof RowHolder row
          && holder.getBindingAdapter() == profiles
          && row.isProfile();
    }

    @Override
    public int getMovementFlags(
        @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder holder) {
      return isProfile(holder)
          ? makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, /* swipeFlags= */ 0)
          : 0;
    }

    @Override
    public boolean isLongPressDragEnabled() {
      return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
      return false;
    }

    @Override
    public boolean canDropOver(
        @NonNull RecyclerView recyclerView,
        @NonNull RecyclerView.ViewHolder current,
        @NonNull RecyclerView.ViewHolder target) {
      return isProfile(target);
    }

    @Override
    public boolean onMove(
        @NonNull RecyclerView recyclerView,
        @NonNull RecyclerView.ViewHolder holder,
        @NonNull RecyclerView.ViewHolder target) {
      int from = holder.getBindingAdapterPosition();
      int to = target.getBindingAdapterPosition();
      if (!profiles.isProfileRow(from) || !profiles.isProfileRow(to) || from == to) {
        return false;
      }
      profiles.move(from, to, recyclerView);
      moved = true;
      return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {}

    @Override
    public void onSelectedChanged(RecyclerView.@Nullable ViewHolder holder, int actionState) {
      super.onSelectedChanged(holder, actionState);
      if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && holder instanceof RowHolder row) {
        moved = false;
        View view = row.itemView;
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        // The row is drawn over the others while it moves, so it needs a background of its own.
        row.background = view.getBackground();
        TypedArray colors =
            view.getContext().obtainStyledAttributes(new int[] {android.R.attr.colorBackground});
        view.setBackgroundColor(colors.getColor(0, 0));
        colors.recycle();
      }
    }

    @Override
    public void clearView(
        @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder holder) {
      super.clearView(recyclerView, holder);
      if (!(holder instanceof RowHolder row)) {
        return;
      }
      row.itemView.setBackground(row.background);
      row.background = null;
      if (moved) {
        moved = false;
        profiles.saveOrder();
        profiles.updateActions();
      } else if (row.isProfile() && isAdded()) {
        showMenu(row.itemView, row.id);
      }
    }
  }
}
