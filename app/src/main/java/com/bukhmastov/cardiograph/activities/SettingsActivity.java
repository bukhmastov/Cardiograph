package com.bukhmastov.cardiograph.activities;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.bukhmastov.cardiograph.R;
import com.bukhmastov.cardiograph.utils.Storage;

import java.util.List;
import java.util.Objects;

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Toolbar bar;
        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);
        root.addView(bar, 0);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_settings));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        try {
            String value = sharedPreferences.getString(key, "0");
            int int_value;
            String value_verified = value;
            String message = "";
            int min_range = Integer.MIN_VALUE;
            int max_range = Integer.MAX_VALUE;
            switch (key) {
                case "frame_rate":
                    min_range = 1;
                    max_range = 166;
                    break;
                case "pixel_per_frame":
                    min_range = 1;
                    max_range = 10;
                    break;
                case "max_point_abs":
                case "graph_height":
                    min_range = 1;
                    max_range = 512;
                    break;
                case "pref_arduino_average_tolerance":
                case "pref_arduino_max_tolerance":
                    min_range = 1;
                    max_range = 512;
                    break;
            }
            int_value = Integer.parseInt(value);
            if (int_value < min_range || int_value > max_range) {
                if (int_value < min_range) int_value = min_range;
                if (int_value > max_range) int_value = max_range;
                value_verified = String.valueOf(int_value);
                message = "Необходим диапазон: " + min_range + " - " + max_range;
            }
            if (!Objects.equals(value, value_verified)) {
                Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(key, value_verified);
                editor.apply();
                bindPreferenceSummaryToValue(findPreference(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return (this.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    public static abstract class TemplatePreferenceFragment extends PreferenceFragment {
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class GeneralPreferenceFragment extends TemplatePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("frame_rate"));
            bindPreferenceSummaryToValue(findPreference("pixel_per_frame"));
            bindPreferenceSummaryToValue(findPreference("max_point_abs"));
            bindPreferenceSummaryToValue(findPreference("graph_height"));
        }
    }

    public static class ArduinoPreferenceFragment extends TemplatePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_arduino);
            bindPreferenceSummaryToValue(findPreference("pref_arduino_average_tolerance"));
            bindPreferenceSummaryToValue(findPreference("pref_arduino_max_tolerance"));
        }
    }

    public static class ArchivePreferenceFragment extends TemplatePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_archive);
            Preference pref_clear_archive = findPreference("pref_clear_archive");
            if (pref_clear_archive != null) {
                pref_clear_archive.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.pref_clear_archive_title))
                                .setMessage(R.string.pref_clear_archive_warning)
                                .setIcon(R.drawable.ic_warning)
                                .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        boolean success = Storage.file.clear(getActivity(), "archive");
                                        Snackbar.make(getActivity().findViewById(android.R.id.content), success ? R.string.archive_cleared : R.string.something_went_wrong, Snackbar.LENGTH_LONG).show();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create().show();
                        return false;
                    }
                });
            }
        }
    }

    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getActivity().finish();
            startActivity(new Intent(getActivity(), AboutActivity.class));
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || ArduinoPreferenceFragment.class.getName().equals(fragmentName)
                || ArchivePreferenceFragment.class.getName().equals(fragmentName)
                || AboutPreferenceFragment.class.getName().equals(fragmentName);
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
