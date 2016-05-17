package com.eissler.micha.hbgvertretungsapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


public class CustomNamesList extends AppCompatActivity {

    private ListView listView;
    private CustomNames customNames;
    private Tracker mTracker;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTracker = ((HbgApplication) getApplication()).getDefaultTracker();

        Log.d(HbgApplication.HBG_APP, "onCreate CustomNamesList");

        setContentView(R.layout.activity_custom_names_list);
        listView = (ListView) findViewById(R.id.list);
        FloatingActionButton addFAB = (FloatingActionButton) findViewById(R.id.add_fab);


        try {
            customNames = new CustomNames(getApplicationContext());
        } catch (Exception e) {
            App.reportUnexpectedException(e);
            e.printStackTrace();
            return;
        }

        if (customNames.size() == 0) {
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(new String[]{"Keine Anzeigenamen gespeichert."}));
            listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList));
            return;
        }

        assert addFAB != null;
        addFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View dialogView;
                new AlertDialog.Builder(CustomNamesList.this).setView(dialogView = getLayoutInflater().inflate(R.layout.dialog_add_custom_name, null)).setTitle("Fach hinzufügen").setPositiveButton("Hinzufügen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final EditText originalDisplayname = (EditText) dialogView.findViewById(R.id.kursName);
                        final EditText newDisplayName = (EditText) dialogView.findViewById(R.id.new_subject_name);

                        String originalName = originalDisplayname.getText().toString();
                        String newName = newDisplayName.getText().toString();

                        if (originalName.trim().equals("") || newName.trim().equals("")) {
                            return;
                        }

                        customNames.put(originalName, newName);
                        setAdapter();

                        customNames.save();

                        mTracker.send(new HitBuilders.EventBuilder()
                                .setCategory("Action")
                                .setAction("Add Subject")
                                .build());
                    }
                }).show();
            }
        });

        setAdapter();

    }

    private void setAdapter() {
        ArrayList<String> arrayList = new ArrayList<>(12);

        boolean listItem = false;

        for (Map.Entry<String, String> entry : customNames.entrySet()) {

            if (!entry.getValue().equals("Nicht anzeigen")) {
                arrayList.add(entry.getKey() + ": " + entry.getValue());
                listItem = true;
            }

        }

        if (!listItem) {
            arrayList = new ArrayList<>(Arrays.asList(new String[]{"Keine Anzeigenamen gespeichert."}));
            listView.setOnItemClickListener(null);
        } else {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String[] split = parent.getItemAtPosition(position).toString().split(":");
                    new FilterDialog(split[1].trim(), split[0], customNames,
                            new FilterDialog.PostExecuteInterface() {
                                @Override
                                public void onPostExecute() {
                                    setAdapter();
                                }
                            },
                            CustomNamesList.this).show();
                }
            });
        }

        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList));


    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("CustomNamesList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
