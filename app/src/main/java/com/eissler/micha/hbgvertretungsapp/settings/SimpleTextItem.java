package com.eissler.micha.hbgvertretungsapp.settings;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.R;
import com.mikepenz.fastadapter.FastAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Micha.
 * 23.12.2016
 */
public class SimpleTextItem extends CheckBoxItem<SimpleTextItem, SimpleTextItem.ViewHolder> {

    private String text;

    public SimpleTextItem(String text, FastAdapter fastAdapter) {
        super(fastAdapter);
        this.text = text;
    }


    @Override
    public int getType() {
        return R.id.simple_text_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.row_layout_whitelist;
    }

    @Override
    public void bindView(ViewHolder holder, List payloads) {
        super.bindView(holder, payloads);
        holder.textView.setText(text);

    }

    @Override
    public ViewHolder getViewHolder(View view) {
        return new ViewHolder(view);
    }

    public String getText() {
        return text;
    }

    @Override
    protected CheckBox getCheckBox(ViewHolder holder) {
        return holder.checkBox;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.textView)
        TextView textView;
        @BindView(R.id.checkbox)
        CheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
