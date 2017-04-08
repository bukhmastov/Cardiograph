package com.bukhmastov.cardiograph.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bukhmastov.cardiograph.R;
import com.bukhmastov.cardiograph.activities.ReplayActivity;
import com.bukhmastov.cardiograph.utils.Storage;

import java.io.File;
import java.util.ArrayList;

public class ArchiveListView extends ArrayAdapter<String> {

    private Context context;
    private ArrayList<String> list;

    public ArchiveListView(Context context, ArrayList<String> list) {
        super(context, R.layout.listview_archive, list);
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.listview_archive, parent, false);
        }
        final String item = list.get(position);
        ((TextView) view.findViewById(R.id.title)).setText(item);
        view.findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), ReplayActivity.class);
                intent.putExtra("file", item);
                getContext().startActivity(intent);
            }
        });
        view.findViewById(R.id.more).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PopupMenu popup = new PopupMenu(context, view);
                    Menu menu = popup.getMenu();
                    popup.getMenuInflater().inflate(R.menu.archive_menu_item, menu);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.download:
                                    File file = new File(Storage.file.getFileLocation(context, "archive#" + item, false));
                                    if (file.exists()) {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.setType("application/octet-stream");
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)));
                                    }
                                    break;
                                case R.id.delete:
                                    if (Storage.file.clear(context, "archive#" + item)) {
                                        Toast.makeText(context, R.string.file_deleted, Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                            }
                            return false;
                        }
                    });
                    popup.show();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

}
