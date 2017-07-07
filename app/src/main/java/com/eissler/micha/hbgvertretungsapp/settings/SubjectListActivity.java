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

import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.util.CheckBoxItem;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;
import com.eissler.micha.hbgvertretungsapp.util.MultiSelectHelper;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.HeaderAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Micha.
 * 17.09.2016
 */
public abstract class SubjectListActivity<T extends IItem> extends AppCompatActivity implements ActionMode.Callback {

    protected FastItemAdapter<T> fastAdapter;
    private HeaderAdapter<SimpleTextItem> noItemsAdapter;
    private List<SimpleTextItem> noItemsList;


    protected abstract void initialize();

    protected abstract List<T> getItems();

    protected abstract void addToData(String subject);

    protected abstract void removeFromData(Set<T> indices);

    protected abstract SimpleTextItem getNoItemsItem();

    @StringRes
    protected abstract int getLabelResource();

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
        fastAdapter.withSelectWithItemUpdate(true);


        fastAdapter = new MultiSelectHelper<>(fastAdapter).setupMultiSelectAdapter(this, R.menu.menu_cab_subject_list, this, null, getOnClickListener(), viewHolder -> {
            if (viewHolder instanceof CheckBoxItem.ViewHolder) {
                return ((CheckBoxItem.ViewHolder) viewHolder).checkBox;
            }
            return null;
        });

        noItemsAdapter = new HeaderAdapter<>();

        recyclerView.setAdapter(noItemsAdapter.wrap(fastAdapter));

        SimpleTextItem noItemsItem = getNoItemsItem();
        noItemsItem.withSelectable(false);
        noItemsList = Collections.singletonList(noItemsItem);

        initialize();
        updateList();


//        final InputMethodManager inputManager = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));

    }

    protected void addTextWatcher(final EditText editText, final AlertDialog dialog) {

        editText.addTextChangedListener(new InputValidator.DisablerEditTextValidator(editText, dialog.getButton(DialogInterface.BUTTON_POSITIVE)) {
            int selectedClass = App.getSelectedClass(SubjectListActivity.this);
            boolean oberstufe = selectedClass == 21 || selectedClass == 22;

            @Override
            public CharSequence validate(CharSequence input) {
                String text = input.toString().trim();
                if (text.equals("")) {
                    return "Feld darf nicht leer sein";
                } else if (text.contains(" ")) {
                    return "Darf keine Leerzeichen enthalten";
                } else if (oberstufe && !(text.matches("[GL][A-Za-zÄÖÜäöüß]{2}[NAÜ]\\d?") || text.matches("S[A-Za-zÄÖÜäöüß]{3}\\d?")) || !oberstufe && !text.matches("[A-Za-zÄÖÜäöüß]{1,4}")) {
                    return "Kürzel wie am Vertretungsplan angeben (z.B.\"" + (oberstufe ? "GDeN1" : "Bi") + "\")"; // TODO: 02.06.2017 make sure this is not blocking correct things
                } else {
                    return null;
                }
            }
        });

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }

    protected void updateList() {
        List<T> items = getItems();
        if (items.isEmpty()) {
//            items = noItemsList;
            noItemsAdapter.set(noItemsList);
            fastAdapter.set(items);
            return;
        }
        noItemsAdapter.set(new ArrayList<>(0));
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            item.withIdentifier(i);
        }
        fastAdapter.set(items);
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        fastAdapter.notifyAdapterDataSetChanged();
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
        fastAdapter.notifyAdapterDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(getMenuRes(), menu);
        return true;
    }

    protected int getMenuRes() {
        return R.menu.menu_subject_list;
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
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_subject, null);
        final EditText editText = (EditText) dialogView.findViewById(R.id.kursName);


        final AlertDialog dialog = new AlertDialog.Builder(SubjectListActivity.this).setView(dialogView)
                .setTitle("Fach hinzufügen")
                .setPositiveButton("Hinzufügen", (dialog1, which) -> {
                    String subject = editText.getText().toString().trim();

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

