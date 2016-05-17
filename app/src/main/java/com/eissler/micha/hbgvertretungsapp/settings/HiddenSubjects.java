package com.eissler.micha.hbgvertretungsapp.settings;

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

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.CustomNames;
import com.eissler.micha.hbgvertretungsapp.FilterDialog;
import com.eissler.micha.hbgvertretungsapp.HbgApplication;
import com.eissler.micha.hbgvertretungsapp.R;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


public class HiddenSubjects extends AppCompatActivity {

    ListView listView;
    CustomNames customNames;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_subjects);

        mTracker = ((HbgApplication) getApplication()).getDefaultTracker();


        Log.d(HbgApplication.HBG_APP, "onCreate HiddenSubjects");

        listView = (ListView) findViewById(R.id.list);

        final FloatingActionButton addFAB = (FloatingActionButton) findViewById(R.id.add_fab);


        try {
            customNames = new CustomNames(getApplicationContext());
        } catch (Exception e) {
            System.err.println("Error in HiddenSubjects.");
            App.reportUnexpectedException(e);
            e.printStackTrace();
            return;
        }

        assert addFAB != null;
        addFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View dialogView;
                new AlertDialog.Builder(HiddenSubjects.this).setView(dialogView = getLayoutInflater().inflate(R.layout.edit_text_dialog, null)).setTitle("Fach hinzufügen").setPositiveButton("Hinzufügen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final EditText editText = (EditText) dialogView.findViewById(R.id.kursName);
                        String text = editText.getText().toString();

                        if (text.equals("")) {
                            return;
                        }

                        customNames.put(text, "Nicht anzeigen");
                        customNames.save();

                        mTracker.send(new HitBuilders.EventBuilder()
                                .setCategory("Action")
                                .setAction("Hide Subject")
                                .build());

                        setAdapter();
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
            if (entry.getValue().equals("Nicht anzeigen")) {
                arrayList.add(entry.getKey());
                listItem = true;
            }

        }

        if (!listItem) {
            arrayList = new ArrayList<>(Arrays.asList(new String[]{"Es werden alle Kurse angezeigt"}));
        }

        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList));

        if (!listItem) {
            listView.setOnItemClickListener(null);
        } else {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String originalSubject = parent.getItemAtPosition(position).toString();
                    System.out.println("originalSubject = " + originalSubject);
                    new FilterDialog("Nicht anzeigen", originalSubject, customNames,
                            new FilterDialog.PostExecuteInterface() {
                                @Override
                                public void onPostExecute() {
                                    HiddenSubjects.this.setAdapter();
                                }
                            },
                            HiddenSubjects.this).show();

                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("HiddenSubjects");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
