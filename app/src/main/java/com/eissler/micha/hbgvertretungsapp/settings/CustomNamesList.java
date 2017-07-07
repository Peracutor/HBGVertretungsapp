package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.FilterDialog;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.util.CheckBoxItem;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;
import com.mikepenz.fastadapter.FastAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;


public class CustomNamesList extends SubjectListActivity<CustomNamesList.CustomNameItem> {

    private CustomNames customNames;

    @Override
    protected void initialize() {
        customNames = CustomNames.get(getApplicationContext());
    }

    @Override
    protected List<CustomNameItem> getItems() {
        customNames = CustomNames.get(this);
        System.out.println("CustomNamesList.getItems");
        ArrayList<CustomNameItem> items = new ArrayList<>();
        items.add(new CustomNameItem(Html.fromHtml("<b>Fachkürzel</b>"), Html.fromHtml("<b>Anzeigename</b>"), fastAdapter).withSelectable(false));
        for (Map.Entry<String, String> entry : customNames.entrySet()) {
            CustomNameItem customNameItem = new CustomNameItem(entry.getKey(), entry.getValue(), fastAdapter);
            items.add(customNameItem);
        }
        if (items.size() == 1) {
            items.clear();
        }
        return items;
    }

    @Override
    protected void addToData(String subject) {/*never gets called, as actionAdd is overridden*/}

    @Override
    protected void removeFromData(Set<CustomNameItem> selectedItems) {
        for (CustomNameItem item : selectedItems) {
            customNames.remove(item.getOriginalSubject());
        }
        customNames.save();
    }

    @Override
    protected SimpleTextItem getNoItemsItem() {
        return new SimpleTextItem("Keine Anzeigenamen gespeichert", fastAdapter).withSelectable(false);
    }

    @Override
    protected int getLabelResource() {
        return R.string.label_custom_names_list;
    }

    @Override
    protected FastAdapter.OnClickListener<CustomNameItem> getOnClickListener() {
        return (v, adapter, item, position) -> {
            new FilterDialog(item.getOriginalSubject(), item.getCustomSubject(), customNames, CustomNamesList.this, CustomNamesList.this::updateList).show();
            return true;
        };
    }

    @Override
    protected void actionAdd() {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_custom_name, null);
        final EditText originalDisplayname = (EditText) dialogView.findViewById(R.id.kursName);
        final EditText newDisplayName = (EditText) dialogView.findViewById(R.id.new_subject_name);

        final AlertDialog dialog = new AlertDialog.Builder(CustomNamesList.this)
                .setView(dialogView)
                .setTitle("Fach hinzufügen")
                .setPositiveButton("Hinzufügen", (dialog1, which) -> {

                    String originalName = originalDisplayname.getText().toString();
                    String newName = newDisplayName.getText().toString();

                    customNames.put(originalName, newName);
                    customNames.save();
                    updateList();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

        InputValidator.DisablerValidatorGroup validatorGroup = new InputValidator.DisablerValidatorGroup(dialog.getButton(DialogInterface.BUTTON_POSITIVE));

        InputValidator.NotEmptyValidator originalValidator = new InputValidator.NotEmptyValidator(originalDisplayname);
        InputValidator.NotEmptyValidator newValidator = new InputValidator.NotEmptyValidator(newDisplayName);
        validatorGroup.add(originalValidator);
        validatorGroup.add(newValidator);

        originalDisplayname.addTextChangedListener(originalValidator);

        newDisplayName.addTextChangedListener(newValidator);
    }

    public static class CustomNameItem extends CheckBoxItem<CustomNameItem, CustomNameItem.ViewHolder> {
        private CharSequence originalSubject;
        private CharSequence customSubject;

        protected CustomNameItem(CharSequence originalSubject, CharSequence customSubject, FastAdapter fastAdapter) {
            super(fastAdapter);
            this.originalSubject = originalSubject;
            this.customSubject = customSubject;
        }

        @Override
        public int getType() {
            return R.id.custom_name_item;
        }

        @Override
        public int getLayoutRes() {
            return R.layout.row_layout_custom_names;
        }

        @Override
        public void bindView(ViewHolder holder, List<Object> payloads) {
            super.bindView(holder, payloads);
            holder.originalSubject.setText(originalSubject);
            holder.customSubject.setText(customSubject);
        }

        public String getOriginalSubject() {
            return originalSubject.toString();
        }

        public String getCustomSubject() {
            return customSubject.toString();
        }

        @Override
        public ViewHolder getViewHolder(View v) {
            return new ViewHolder(v);
        }

        public class ViewHolder extends CheckBoxItem.ViewHolder {

            @BindView(R.id.original_subject)
            TextView originalSubject;

            @BindView(R.id.custom_subject)
            TextView customSubject;

            public ViewHolder(View itemView) {
                super(itemView);
            }
        }

    }

}
