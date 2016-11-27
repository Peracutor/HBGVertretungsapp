package com.eissler.micha.hbgvertretungsapp.evaluation;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.R;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.peracutor.hbgserverapi.HeaderMessage;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Micha.
 * 24.10.2016
 */
public class DateHeaderItem extends AbstractItem<DateHeaderItem, DateHeaderItem.ViewHolder> {
    private HeaderMessage headerMessage;

    public DateHeaderItem(HeaderMessage headerMessage) {
        this.headerMessage = headerMessage;
    }

    @Override
    public int getType() {
        return R.id.date;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.row_layout_date;
    }

    @Override
    public void bindView(ViewHolder holder, List payloads) {
        super.bindView(holder, payloads);
        holder.dateView.setText(headerMessage.getHeaderString());
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.date) TextView dateView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
