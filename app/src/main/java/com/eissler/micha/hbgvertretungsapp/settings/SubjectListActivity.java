package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.util.FastAdapterHelper;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Micha.
 * 17.09.2016
 */
public abstract class SubjectListActivity<T extends IItem> extends AppCompatActivity implements ActionMode.Callback {

    protected FastItemAdapter<T> fastAdapter;
    private T noItemsItem;


    protected abstract void initialize();

    protected abstract List<T> getItems(); // TODO: 23.12.2016 when returning multiple items, list is completely screwed

    protected abstract void addToData(String subject);

    protected abstract void removeFromData(Set<T> indices);

    protected abstract T getNoItemsItem();


    @StringRes protected abstract int getLabelResource();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_list_layout);

        ((TextView) findViewById(R.id.label)).setText(getLabelResource());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        fastAdapter = new FastItemAdapter<>();
        fastAdapter.setHasStableIds(true);

        initialize();

        fastAdapter = FastAdapterHelper.setupMultiSelectAdapter(fastAdapter, this, this, null, getOnClickListener());

        recyclerView.setAdapter(this.fastAdapter);

        noItemsItem = getNoItemsItem();
        noItemsItem.withSelectable(false);

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
        List<T> items = getItems();
        if (items.isEmpty()) {
            items = Collections.singletonList(noItemsItem);
        }
        fastAdapter.set(items);
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.item_delete) {
            removeFromData(fastAdapter.getSelectedItems());

            updateList();
            mode.finish();
            return true;
        }
        return false;
    }


    @Override
    public void onDestroyActionMode(ActionMode mode) {
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
                .setPositiveButton("Hinzufügen", (dialog1, which) -> {
                    String subject = editText.getText().toString();

                    if (subject.equals("")) {
                        return;
                    }

                    addToData(subject);
                    updateList();


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
                })
                .setNegativeButton("Abbrechen", null)
                .show();

        addTextWatcher(editText, dialog);
        //                inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    protected FastAdapter.OnClickListener<T> getOnClickListener() {
        return null;
    }
}

