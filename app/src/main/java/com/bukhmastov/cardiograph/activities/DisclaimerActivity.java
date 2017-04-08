package com.bukhmastov.cardiograph.activities;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.bukhmastov.cardiograph.R;

public class DisclaimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);
        Button disclaimer_button = (Button) findViewById(R.id.disclaimer_button);
        if (disclaimer_button != null) {
            disclaimer_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean("storage_disclaimer", false).apply();
                    finish();
                }
            });
        }
    }
}
