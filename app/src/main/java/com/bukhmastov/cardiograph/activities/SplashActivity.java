package com.bukhmastov.cardiograph.activities;

import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;

import com.bukhmastov.cardiograph.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_arduino, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_archive, false);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}