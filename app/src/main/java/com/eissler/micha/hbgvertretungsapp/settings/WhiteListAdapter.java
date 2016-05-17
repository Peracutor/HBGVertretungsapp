package com.eissler.micha.hbgvertretungsapp.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.ForItemViewType;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.SelectionAdapter;
import com.eissler.micha.hbgvertretungsapp.ViewHolderResource;

import java.util.ArrayList;

import butterknife.BindView;


public class WhiteListAdapter extends SelectionAdapter {

    private ArrayList<String> whiteListSubjects;

    public WhiteListAdapter(ArrayList<String> whiteListSubjects, Context context) {
        super(context, ViewHolder.class);
        this.whiteListSubjects = whiteListSubjects;
    }

    @Override
    public int getCount() {
        return whiteListSubjects.size();
    }

    @Override
    public Object getItem(int position) {
        return whiteListSubjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(final int position, @NonNull View convertView, final ViewGroup parent, @NonNull Object viewHolder, boolean isSelected) {
        ViewHolder holder = (ViewHolder) viewHolder;
        final CheckBox checkBox = holder.checkBox;

        holder.textView.setText(whiteListSubjects.get(position));
        if (getCount() == 1 && whiteListSubjects.get(position).contains("Keine Fächer gespeichert")) {
            checkBox.setVisibility(View.GONE);
            convertView.setEnabled(false);
            return convertView;
        }

        checkBox.setChecked(isSelected);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBox.setChecked(!checkBox.isChecked());
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    select(position, false);

                } else {
                    deselect(position, false);
                }

                if (WhitelistSubjects.mActionMode != null) {
                    WhitelistSubjects.mActionMode.setTitle(String.valueOf(getNumberOfSelectedItems()));
                }
            }
        });
        return convertView;
    }

    @ViewHolderResource(R.layout.row_layout_whitelist)
    static class ViewHolder {
        @BindView(R.id.textView) TextView textView;
        @BindView(R.id.checkbox) CheckBox checkBox;
    }
}
