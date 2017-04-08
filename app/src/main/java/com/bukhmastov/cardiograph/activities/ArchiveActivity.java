package com.bukhmastov.cardiograph.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ListView;

import com.bukhmastov.cardiograph.R;
import com.bukhmastov.cardiograph.adapters.ArchiveListView;
import com.bukhmastov.cardiograph.utils.Storage;

import java.util.ArrayList;
import java.util.Collections;

public class ArchiveActivity extends AppCompatActivity {

    private static final String TAG = "ArchiveActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        setSupportActionBar((Toolbar) findViewById(R.id.archive_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        final ArrayList<String> list = Storage.file.getArrayListOfFiles(this, "archive");
        Collections.sort(list);
        ListView archive_listview = (ListView) findViewById(R.id.archive_listview);
        if (archive_listview != null) {
            archive_listview.setAdapter(new ArchiveListView(this, list));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

}
