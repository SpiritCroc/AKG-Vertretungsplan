/*
 * Copyright (C) 2015-2016 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.akg_vertretungsplan.settings;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import de.spiritcroc.akg_vertretungsplan.LessonPlanShortcutActivity;
import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsLessonPlanFragment extends CustomPreferenceFragment {

    private static final String KEY_ADD_LAUNCHER_SHORTCUT =
            "pref_lesson_plan_add_launcher_shortcut";

    private Preference autoSelectDayTimePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_lesson_plan);

        autoSelectDayTimePref = findPreference(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_preferences_lesson_plan, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_reset:
                promptResetColors();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void init() {
        setAutoSelectDayTimeSummary();
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_TIME);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_LESSON);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_FREE_TIME);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_ROOM);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION);
        setListPreferenceSummary(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION);
        setListPreferenceSummary(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME:
                setAutoSelectDayTimeSummary();
                break;
            case Keys.LESSON_PLAN_COLOR_TIME:
            case Keys.LESSON_PLAN_COLOR_LESSON:
            case Keys.LESSON_PLAN_COLOR_FREE_TIME:
            case Keys.LESSON_PLAN_COLOR_ROOM:
            case Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION:
            case Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION:
            case Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON:
                setListPreferenceSummary(key);
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        String key = preference.getKey();
        if (KEY_ADD_LAUNCHER_SHORTCUT.equals(key)) {
            addLauncherShortcut();
        } else if (Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME.equals(key)) {
            editAutoSelectDayTime();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    private void setAutoSelectDayTimeSummary() {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        int hour = correctInteger(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME,
                sp.getString(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME, ""), 17);
        int minute = sp.getInt(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, 0);
        String m = minute > 9 ? ("" + minute) : ("0" + minute);
        String summary = getString(R.string.pref_lesson_plan_auto_select_day_time_summary, hour, m);
        autoSelectDayTimePref.setSummary(summary);
    }

    private void editAutoSelectDayTime() {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        int hour = Integer.parseInt(sp.getString(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME, "17"));
        int minute = sp.getInt(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, 0);
        new TimePickerDialog(getActivity(), autoSelectDayTimeSetListener, hour, minute, true)
                .show();
    }

    private void saveAutoSelectDayTime(int hourOfDay, int minute) {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        sp.edit().putString(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME, "" + hourOfDay)
                .putInt(Keys.LESSON_PLAN_AUTO_SELECT_DAY_TIME_MINUTES, minute)
                .apply();
        setAutoSelectDayTimeSummary();
    }

    private TimePickerDialog.OnTimeSetListener autoSelectDayTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    saveAutoSelectDayTime(hourOfDay, minute);
                }
            };

    private void addLauncherShortcut() {
        Intent intent = LessonPlanShortcutActivity.getShortcut(getActivity());
        getActivity().sendBroadcast(intent);
        Toast.makeText(getActivity(), R.string.launcher_added_successfully, Toast.LENGTH_SHORT)
                .show();
    }

    private void promptResetColors() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.pref_restore_default_colors)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetColors();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .show();
    }

    private void resetColors() {
        getPreferenceManager().getSharedPreferences().edit()
                .remove(Keys.LESSON_PLAN_COLOR_TIME)
                .remove(Keys.LESSON_PLAN_COLOR_LESSON)
                .remove(Keys.LESSON_PLAN_COLOR_FREE_TIME)
                .remove(Keys.LESSON_PLAN_COLOR_ROOM)
                .remove(Keys.LESSON_PLAN_COLOR_RELEVANT_INFORMATION)
                .remove(Keys.LESSON_PLAN_COLOR_GENERAL_INFORMATION)
                .remove(Keys.LESSON_PLAN_BG_COLOR_CURRENT_LESSON)
                .apply();
        SettingsUserInterfaceFragment.applyThemeToCustomColors(getActivity(),
                SettingsUserInterfaceFragment.APPLY_THEME_LESSONS, true);
        // Restart to immediately show changes
        getActivity().recreate();
    }

}
