package com.bukhmastov.cardiograph;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;

import java.util.Objects;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        SharedPreferences prefs;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("frame_rate"));
            bindPreferenceSummaryToValue(findPreference("pixel_per_frame"));
            bindPreferenceSummaryToValue(findPreference("max_point_abs"));
        }

        @Override
        public void onResume() {
            super.onResume();
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            String value = sharedPreferences.getString(key, "0");
            int int_value;
            String value_verified = value;
            String message = "";
            switch(key){
                case "frame_rate":
                    int_value = Integer.parseInt(value);
                    if(int_value < 1 || int_value > 60){
                        if(int_value < 1) int_value = 1;
                        if(int_value > 60) int_value = 60;
                        value_verified = String.valueOf(int_value);
                        message = "Необходим диапазон: 1 - 60";
                    }
                    break;
                case "pixel_per_frame":
                    int_value = Integer.parseInt(value);
                    if(int_value < 1 || int_value > 10){
                        if(int_value < 1) int_value = 1;
                        if(int_value > 10) int_value = 10;
                        value_verified = String.valueOf(int_value);
                        message = "Необходим диапазон: 1 - 10";
                    }
                    break;
                case "max_point_abs":
                    int_value = Integer.parseInt(value);
                    if(int_value < 1 || int_value > 512){
                        if(int_value < 1) int_value = 1;
                        if(int_value > 512) int_value = 512;
                        value_verified = String.valueOf(int_value);
                        message = "Необходим диапазон: 1 - 512";
                    }
                    break;
            }
            if(!Objects.equals(value, value_verified)){
                Snackbar.make(getActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(key, value_verified);
                editor.apply();
                bindPreferenceSummaryToValue(findPreference(key));
            }
        }
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
}
