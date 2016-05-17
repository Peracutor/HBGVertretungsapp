package com.eissler.micha.hbgvertretungsapp;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Micha.
 * 14.04.2016
 */
public interface SelectionInterface {
    /**
     *
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position The position of the item within the adapter's data set of the item whose view
     *        we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     *        Heterogeneous lists can specify their number of view types, so that this View is
     *        always of the right type (see {@code #getViewTypeCount()} and
     *        {@code #getItemViewType(int)}).
     * @param parent The {@code SelectionListView} that this view will eventually be attached to
     * @param viewHolder The ViewHolder-Object set as tag of convertView.
     * @param isSelected true if the item at this position is selected
     * @return A View corresponding to the data at the specified position.
     */
    View getView(int position,@NonNull View convertView, ViewGroup parent, @NonNull Object viewHolder, boolean isSelected);
}
