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

import android.content.SharedPreferences;
import android.os.Bundle;

import de.spiritcroc.akg_vertretungsplan.R;

public class SettingsActionBarFragment extends CustomPreferenceFragment {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_action_bar);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        setListPreferenceSummary(Keys.ACTION_BAR_NORMAL_BG_COLOR);
        setListPreferenceSummary(Keys.ACTION_BAR_FILTERED_BG_COLOR);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Keys.ACTION_BAR_NORMAL_BG_COLOR:
            case Keys.ACTION_BAR_FILTERED_BG_COLOR:
                setListPreferenceSummary(key);
                break;
        }
    }

}
