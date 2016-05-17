package com.eissler.micha.hbgvertretungsapp;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Micha.
 * 28.04.2016
 */
public interface NoBoilerInterface {

    View getView(int position, @NonNull View convertView, ViewGroup parent, @NonNull Object viewHolder);
}
