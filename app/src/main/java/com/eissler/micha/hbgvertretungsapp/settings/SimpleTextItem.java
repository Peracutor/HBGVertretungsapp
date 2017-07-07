package com.eissler.micha.hbgvertretungsapp.settings;

import android.view.View;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.util.CheckBoxItem;
import com.mikepenz.fastadapter.FastAdapter;

import java.util.List;

import butterknife.BindView;

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
        return R.layout.row_layout_simple_text;
    }

    @Override
    public void bindView(ViewHolder holder, List<Object> payloads) {
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

    class ViewHolder extends CheckBoxItem.ViewHolder {
        @BindView(R.id.textView)
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
//            ButterKnife.bind(this, itemView);
        }
    }
}
