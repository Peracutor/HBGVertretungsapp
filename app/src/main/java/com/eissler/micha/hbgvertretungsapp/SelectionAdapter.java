package com.eissler.micha.hbgvertretungsapp;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.ArrayList;

public abstract class SelectionAdapter extends NoBoilerPlateAdapter implements SelectionInterface {

    private ArrayList<Integer> selectedItems;
    private AdapterView.OnItemSelectedListener mOnItemSelectedListener;

    public SelectionAdapter(Context context, Class<?>... viewHolderClasses) {
        this(0, context, viewHolderClasses);
    }

    public SelectionAdapter(int selectionCapacity, Context context, Class<?>... viewHolderClasses) {
        super(context, viewHolderClasses);
        selectedItems = new ArrayList<>(selectionCapacity);
    }

    /**
     * Returns {@code true} if the item at the given position is selected.
     * @param position The position of the item.
     * @return {@code true} if the item at the given position is selected.
     */
    public boolean isSelected(int position) {
        return indexOfPositionInArray(position) != -1;
    }

    /**
     * Selects the item at the given position. Does nothing if the item is already selected
     * @param position The position of the item to be selected.
     * @param notifyDataSetChanged Pass true for this if you want to call notifyDataSetChanged() after selecting.
     */
    public void select(int position, boolean notifyDataSetChanged) {
        if (!isSelected(position)) {
            selectedItems.add(position);
            if (notifyDataSetChanged) {
                notifyDataSetChanged();
            }

            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(null, null, position, getItemId(position));
            }

        }
    }

    /**
     * Deselect the item at the given position, if it is selected.
     * @param position The position of the item to be unselected.
     * @param notifyDataSetChanged Pass true for this if you want to call notifyDataSetChanged() after deselecting.
     * @return {@code true} if the item was deselected, {@code false} if it was not selected and could therefore not be deselected.
     */
    public boolean deselect(int position, boolean notifyDataSetChanged) {
        int index = indexOfPositionInArray(position);
        if (index != -1) {
            selectedItems.remove(index);

            if (mOnItemSelectedListener != null && !itemsSelected()) {
                mOnItemSelectedListener.onNothingSelected(null);
            }

            if (notifyDataSetChanged) {
                notifyDataSetChanged();
            }
            return true;
        }
        return false;

    }

    /**
     * @return {@code true} if at least one item is selected.
     */
    public boolean itemsSelected() {
        return selectedItems.size() > 0;
    }

    /**
     * @return The number of selected items.
     */
    public int getNumberOfSelectedItems() {
        return selectedItems.size();
    }

    /**
     * Deselects all items.
     * @param notifyDataSetChanged Pass true for this if you want to call notifyDataSetChanged() after clearing.
     */
    public void clearSelection(boolean notifyDataSetChanged) {
        selectedItems.clear();

        if (mOnItemSelectedListener != null) {
            mOnItemSelectedListener.onNothingSelected(null);
        }

        if (notifyDataSetChanged) {
            notifyDataSetChanged();
        }
    }

    /**
     * @param onItemSelectedListener The onItemSelectedListener to be registered.
     */
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
//        if (!(onItemSelectedListener instanceof SelectionListView)) {
//            throw new IllegalStateException("Do not set the OnItemClickListener on the adapter but on the SelectionListView");
//        }
        mOnItemSelectedListener = onItemSelectedListener;
    }

    /**
     * @return An ArrayList containing the selected positions.
     */
    public ArrayList<Integer> getSelectedItems() {
        return selectedItems;
    }

    private int indexOfPositionInArray(int position) {
        for (int i = 0; i < selectedItems.size(); i++) {
            int selectedListItem = selectedItems.get(i);
            if (position == selectedListItem) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public View getView(int position, @NonNull View convertView, ViewGroup parent, @NonNull Object viewHolder) {
        return getView(position, convertView, parent, viewHolder, isSelected(position));
    }
}
