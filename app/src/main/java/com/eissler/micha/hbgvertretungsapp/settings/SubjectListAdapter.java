package com.eissler.micha.hbgvertretungsapp.settings;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.R;
import com.peracutor.comfyadapters.SelectionAdapter;
import com.peracutor.comfyadapters.ViewHolderResource;

import java.util.ArrayList;

import butterknife.BindView;


public abstract class SubjectListAdapter extends SelectionAdapter {

    private ArrayList<String> data;
    private boolean noData;

    public SubjectListAdapter(ArrayList<String> data, Activity activity) {
        super(activity, ViewHolder.class);

        setData(data);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return !(data.get(position).equals(getNoItemsString()));
    }

    protected View.OnLongClickListener getOnLongClickListener(final CheckBox checkBox) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                checkBox.setChecked(!checkBox.isChecked());
                return true;
            }
        };
    }

    protected View.OnClickListener getOnClickListener(final CheckBox checkBox) {
        if (isSelectionMode()) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.setChecked(!checkBox.isChecked());
                }
            };
        } else return getNonSelectiveListener();

    }

    protected View.OnClickListener getNonSelectiveListener() {
        return null;
    }

    public void setData(ArrayList<String> data) {
        if (data.isEmpty()) {
            data = getNoItemsSelectedList();
        }

        this.data = data;
        notifyDataSetChanged();
    }

    private ArrayList<String> getNoItemsSelectedList() {
        ArrayList<String> whitelistArrayList;
        whitelistArrayList = new ArrayList<>(1);
        whitelistArrayList.add(getNoItemsString());
        return whitelistArrayList;
    }

    protected abstract String getNoItemsString();

    @Override
    protected com.peracutor.comfyadapters.ViewHolder instantiate(Class<? extends com.peracutor.comfyadapters.ViewHolder> viewHolderClass) {
        return new ViewHolder();
    }

    @ViewHolderResource(R.layout.row_layout_whitelist)
    public class ViewHolder extends SelectableViewHolder {
        @BindView(R.id.textView) TextView textView;
        @BindView(R.id.checkbox) CheckBox checkBox;

        @Override
        public void recycleSelectableViewHolder(final int position, View convertView) {
            textView.setText(data.get(position));

            if (isEnabled(position)) {
                convertView.setOnClickListener(getOnClickListener(checkBox));
                convertView.setOnLongClickListener(getOnLongClickListener(checkBox));
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        select(isChecked, position, false);

                        if (SubjectListActivity.mActionMode != null) {
                            SubjectListActivity.mActionMode.setTitle(String.valueOf(getNumberOfSelectedItems()));
                        }
                    }
                });
            } else {
                convertView.setOnLongClickListener(null);
                convertView.setOnClickListener(null);
                checkBox.setOnCheckedChangeListener(null);
            }
        }

        @Override
        public void onItemSelected(int position) {
        }

        @Override
        public void onItemDeselected(int position) {
        }

        @Override
        public void onSelectionModeEnabled(int position) {
            if (isEnabled(position)) checkBox.setVisibility(View.VISIBLE);
        }

        @Override
        public void onSelectionModeDisabled(int position) {
            checkBox.setVisibility(View.GONE);
        }
    }
}
