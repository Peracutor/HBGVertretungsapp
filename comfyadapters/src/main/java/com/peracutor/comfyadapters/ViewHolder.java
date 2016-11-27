package com.peracutor.comfyadapters;

import android.view.View;

/**
 * Created by Micha.
 * 24.10.2016
 */
public abstract class ViewHolder {
    protected abstract void recycleViewHolder(int position, View convertView);
}
