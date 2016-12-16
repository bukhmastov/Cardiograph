package com.bukhmastov.cardiograph;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class DisclaimerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);
        Button disclaimer_button = (Button) findViewById(R.id.disclaimer_button);
        disclaimer_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE).edit();
                editor.putBoolean("storage_disclaimer", false);
                editor.apply();
                finish();
            }
        });
    }
}
