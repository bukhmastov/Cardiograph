package com.bukhmastov.cardiograph;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DisclaimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);
        Button disclaimer_button = (Button) findViewById(R.id.disclaimer_button);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int frame_rate = Integer.parseInt(sharedPreferences.getString("frame_rate", "0"));
        int pixel_per_frame = Integer.parseInt(sharedPreferences.getString("pixel_per_frame", "0"));
        int max_point_abs = Integer.parseInt(sharedPreferences.getString("max_point_abs", "0"));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(frame_rate == 0) editor.putString("frame_rate", "40");
        if(pixel_per_frame == 0) editor.putString("pixel_per_frame", "2");
        if(max_point_abs == 0) editor.putString("max_point_abs", "250");
        editor.apply();
        disclaimer_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                editor.putBoolean("storage_disclaimer", false);
                editor.apply();
                finish();
            }
        });
    }
}
