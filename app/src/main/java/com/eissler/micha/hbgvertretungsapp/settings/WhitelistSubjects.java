package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.HbgApplication;
import com.eissler.micha.hbgvertretungsapp.MainActivity;
import com.eissler.micha.hbgvertretungsapp.R;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.acra.ACRA;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class WhitelistSubjects extends AppCompatActivity implements ActionMode.Callback {

    private static final String WHITELIST_ITEMS = "WhitelistItems";
    final static String WHITELIST_SWITCH = "whitelist_switch";
    private ListView listView;
    private ArrayList<String> whitelistArrayList;
    private WhiteListAdapter whiteListAdapter;

    static ActionMode mActionMode = null;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist_subjects);

        mTracker = ((HbgApplication) getApplication()).getDefaultTracker();


        listView = (ListView) findViewById(R.id.list);

        whitelistArrayList = getWhiteListArray(this);
        setListView();



        final FloatingActionButton addFAB = (FloatingActionButton) findViewById(R.id.add_fab);

        assert addFAB != null;
        addFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View dialogView;
                new AlertDialog.Builder(WhitelistSubjects.this).setView(dialogView = getLayoutInflater().inflate(R.layout.edit_text_dialog, null)).setTitle("Fach hinzufügen").setPositiveButton("Hinzufügen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final EditText editText = (EditText) dialogView.findViewById(R.id.kursName);
                        String text = editText.getText().toString();

                        if (text.equals("")) {
                            return;
                        }

                        if (whitelistArrayList == null) {
                            whitelistArrayList = new ArrayList<>(1);
                        }
                        whitelistArrayList.add(text);
                        setListView();

                        saveWhiteListArray(whitelistArrayList, WhitelistSubjects.this);
                        mTracker.send(new HitBuilders.EventBuilder()
                                .setCategory("Action")
                                .setAction("Add Whitelist-Subject")
                                .build());
                    }
                }).show();
            }
        });


    }

    public static boolean saveWhiteListArray(ArrayList<String> whitelistArrayList, Context context) { // TODO: 26.03.2016 forward exceptions
        try {
            App.writeObject(whitelistArrayList, WHITELIST_ITEMS, context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
        return false;
    }

    public static ArrayList<String> getWhiteListArray(Context context) { // TODO: 26.03.2016 forward exceptions
        ArrayList<String> whitelistArrayList;
        try {
            whitelistArrayList = App.retrieveObject(WHITELIST_ITEMS, context);
            return whitelistArrayList;
        } catch (ClassCastException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            return getNoItemsSelectedList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setListView() {
        final WhiteListAdapter whitelistAdapter_final = whiteListAdapter = new WhiteListAdapter(whitelistArrayList.size() == 0 ? getNoItemsSelectedList() : whitelistArrayList, WhitelistSubjects.this);
        whitelistAdapter_final.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (whitelistAdapter_final.getNumberOfSelectedItems() == 1) {
                    mActionMode = WhitelistSubjects.this.startActionMode(WhitelistSubjects.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }
        });

        listView.setAdapter(whitelistAdapter_final);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_cam_whitelist, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.item_delete) {
            for (int selectedPosition : whiteListAdapter.getSelectedItems()) {
                whitelistArrayList.set(selectedPosition, null);
            }

            for (int i = 0; i < whiteListAdapter.getNumberOfSelectedItems(); i++) {
                whitelistArrayList.remove(null);
            }

            saveWhiteListArray(whitelistArrayList, WhitelistSubjects.this);

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("Remove Whitelist-Subjects")
                    .setValue(whiteListAdapter.getNumberOfSelectedItems())
                    .build());
            setListView();
            mActionMode.finish();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        whiteListAdapter.clearSelection(true);
    }

    private static ArrayList<String> getNoItemsSelectedList() {
        ArrayList<String> whitelistArrayList;
        whitelistArrayList = new ArrayList<>(1);
        whitelistArrayList.add("Keine Fächer gespeichert - Es wird dir keine einzige Meldung angezeigt werden!");
        return whitelistArrayList;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("WhitelistSubjects");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public static boolean isWhitelistModeActive() {
        return MainActivity.defaultPrefs.getBoolean(WHITELIST_SWITCH, false);
    }
}
