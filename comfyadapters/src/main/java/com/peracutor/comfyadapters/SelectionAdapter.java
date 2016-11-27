package com.peracutor.comfyadapters;


import android.content.Context;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;

public abstract class SelectionAdapter extends ComfyBaseAdapter {

    private ArrayList<Integer> selectedItems;
    private AdapterView.OnItemSelectedListener mOnItemSelectedListener;
    private boolean isSelectionMode = false;

    @SafeVarargs
    public SelectionAdapter(Context context, Class<? extends ViewHolder>... viewHolderClasses) {
        this(0, context, viewHolderClasses);
    }

    @SafeVarargs
    public SelectionAdapter(int selectionCapacity, Context context, Class<? extends ViewHolder>... viewHolderClasses) {
        super(context, viewHolderClasses);
//        for (Class c :
//                viewHolderClasses) {
//            boolean implemented = false;
//            for (Class interfaceClass :
//                    c.getInterfaces()) {
//                if (interfaceClass.equals(SelectableViewHolder.class)) {
//                    implemented = true;
//                    break;
//                }
//            }
//            if (!implemented) {
//                throw new IllegalArgumentException("All ViewHolders must implement SelectableViewHolder interface.");
//            }
//        }
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
     * Selects/Deselects the item at the given position. Does nothing if the item is already selected
     *
     * @param position The position of the item to be selected.
     * @param notifyDataSetChanged Pass true for this if you want to call notifyDataSetChanged() after selecting.
     */
    public void select(boolean select, int position, boolean notifyDataSetChanged) {
        if (select) select(position, notifyDataSetChanged);
        else deselect(position, notifyDataSetChanged);
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

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
    }

//    @Override
//    public View getView(int position, @NonNull View convertView, ViewGroup parent, @NonNull Object viewHolder) {
//        return getView(position, convertView, parent, viewHolder, isSelected(position));
//    }

    public abstract class SelectableViewHolder extends ViewHolder {

        @Override
        final protected void recycleViewHolder(int position, View convertView) {
            if (isSelected(position)) {
                onItemSelected(position);
            } else {
                onItemDeselected(position);
            }

            if (isSelectionMode()) {
                onSelectionModeEnabled(position);
            } else {
                onSelectionModeDisabled(position);
            }

            recycleSelectableViewHolder(position, convertView);
        }

        protected abstract void recycleSelectableViewHolder(int position, View convertView);

        protected abstract void onItemSelected(int position);

        protected abstract void onItemDeselected(int position);

        protected abstract void onSelectionModeEnabled(int position);

        protected abstract void onSelectionModeDisabled(int position);
    }
}
