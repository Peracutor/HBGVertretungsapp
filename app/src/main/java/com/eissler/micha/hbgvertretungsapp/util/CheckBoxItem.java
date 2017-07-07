package com.eissler.micha.hbgvertretungsapp.util;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;

import com.eissler.micha.hbgvertretungsapp.R;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Micha.
 * 23.12.2016
 */
public abstract class CheckBoxItem<Item extends CheckBoxItem<?, ?>, VH extends CheckBoxItem.ViewHolder> extends AbstractItem<Item, VH> {
        private FastAdapter fastAdapter;

    protected CheckBoxItem(FastAdapter fastAdapter) {
        this.fastAdapter = fastAdapter;
    }

    @Override
    public void bindView(VH holder, List<Object> payloads) {
        super.bindView(holder, payloads);
        holder.checkBox.setVisibility(fastAdapter.getSelectedItems().size() != 0 && isSelectable() ? View.VISIBLE : View.INVISIBLE);
        holder.checkBox.setChecked(isSelected());
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.checkbox)
        public
        CheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
