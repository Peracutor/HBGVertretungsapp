package com.eissler.micha.hbgvertretungsapp.settings;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.List;

/**
 * Created by Micha.
 * 23.12.2016
 */
public abstract class CheckBoxItem<Item extends CheckBoxItem<?, ?>, VH extends RecyclerView.ViewHolder> extends AbstractItem<Item, VH> {
    private FastAdapter fastAdapter;

    protected CheckBoxItem(FastAdapter fastAdapter) {
        this.fastAdapter = fastAdapter;
    }


    protected abstract CheckBox getCheckBox(VH holder);

    @Override
    public void bindView(VH holder, List payloads) {
        super.bindView(holder, payloads);
        getCheckBox(holder).setVisibility(fastAdapter.getSelectedItems().size() != 0 ? View.VISIBLE : View.GONE);
        getCheckBox(holder).setChecked(isSelected());
    }
}
