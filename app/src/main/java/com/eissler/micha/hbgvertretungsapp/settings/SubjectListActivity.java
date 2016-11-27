package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.InputValidator;
import com.eissler.micha.hbgvertretungsapp.R;

import java.util.ArrayList;

/**
 * Created by Micha.
 * 17.09.2016
 */
public abstract class SubjectListActivity extends AppCompatActivity implements ActionMode.Callback {

    private SubjectListAdapter adapter;

    static ActionMode mActionMode = null;


    protected abstract void initialize();

    protected abstract SubjectListAdapter getSubjectListAdapter();

    protected abstract ArrayList<String> getData();

    protected abstract void addToData(String subject);

    protected abstract void removeFromData(ArrayList<Integer> indices);

    protected abstract void saveData();

    @StringRes protected abstract int getLabelResource();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_list_layout);

        ((TextView) findViewById(R.id.label)).setText(getLabelResource());

        ListView listView = (ListView) findViewById(R.id.list);

        initialize();

        adapter = getSubjectListAdapter();

        adapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (adapter.getNumberOfSelectedItems() == 1) {
                    mActionMode = SubjectListActivity.this.startActionMode(SubjectListActivity.this);
                    adapter.setSelectionMode(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }
        });

        //noinspection ConstantConditions
        listView.setAdapter(adapter);

        updateList();


//        final InputMethodManager inputManager = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));

    }

    protected void addTextWatcher(final EditText editText, final AlertDialog dialog) {
        editText.addTextChangedListener(new InputValidator() {
            @Override
            protected boolean validate(String text) {
                if (text.equals("")) {
                    editText.setError("Feld darf nicht leer sein");
                    return false;
                } else {
                    editText.setError(null);
                    return true;
                }
            }

            @Override
            protected void onValidated(boolean valid) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(valid);
            }
        });

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }

    protected void updateList() {
        adapter.setData(getData());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_cab_whitelist, menu);
        if (!showCheckBoxesConstantly()) adapter.notifyDataSetChanged();
        return true;
    }

    protected boolean showCheckBoxesConstantly() {
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.item_delete) {
            removeFromData(adapter.getSelectedItems());
            saveData();

            updateList();
            mActionMode.finish();
            return true;
        }
        return false;
    }


    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        adapter.clearSelection(true);
        adapter.setSelectionMode(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_subject_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_add:
                System.out.println("action_add");
                actionAdd();
                return true;
        }
        return false;
    }

    protected void actionAdd() {
        final View dialogView = getLayoutInflater().inflate(R.layout.edit_text_dialog, null);
        final EditText editText = (EditText) dialogView.findViewById(R.id.kursName);


        final AlertDialog dialog = new AlertDialog.Builder(SubjectListActivity.this).setView(dialogView)
                .setTitle("Fach hinzufügen")
                .setPositiveButton("Hinzufügen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, int which) {
                        String subject = editText.getText().toString();

                        if (subject.equals("")) {
                            return;
                        }

                        addToData(subject);
                        updateList();
                        saveData();


                        //                        if (getCurrentFocus() == null) { // 17.09.2016 hiding did not work, maybe retry implement
                        //                            System.out.println("No focus");
                        //                            return;
                        //                        }
                        //                        if (getCurrentFocus().getWindowToken() == null) {
                        //                            System.out.println("No WindowToken");
                        //                            return;
                        //                        }
                        //                        System.out.println("Hide Keyboard");
                        //                        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();

        addTextWatcher(editText, dialog);
        //                inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}

