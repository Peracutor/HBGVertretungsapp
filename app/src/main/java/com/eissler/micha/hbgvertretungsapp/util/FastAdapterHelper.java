package com.eissler.micha.hbgvertretungsapp.util;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;

import com.eissler.micha.hbgvertretungsapp.R;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;


/**
 * Created by Micha.
 * 23.12.2016
 */

public class FastAdapterHelper {

//    public static <T extends IItem> FastAdapter<T> setupMultiSelectAdapter()

    public static <T extends IItem, F extends FastAdapter<T>> F setupMultiSelectAdapter(@NonNull F fastAdapter, @NonNull AppCompatActivity activity, @NonNull ActionMode.Callback callback,
                                                                                        @Nullable BiConsumer<FastAdapter<T>, ActionMode> updateActionMode, @Nullable FastAdapter.OnClickListener<T> defaultOnClick) {
        fastAdapter.withSelectable(true);
        fastAdapter.withSelectOnLongClick(true);
        fastAdapter.withMultiSelect(true);

        if (updateActionMode == null) {
            updateActionMode = (fastAdapter1, actionMode) -> actionMode.setTitle(String.valueOf(fastAdapter1.getSelectedItems().size()));
        }

        final ActionModeHelper actionModeHelper = new ActionModeHelper(fastAdapter, R.menu.menu_cab_subject_list, callback);

        fastAdapter.withOnPreClickListener((v, adapter1, item, position) -> {
            if (actionModeHelper.getActionMode() != null) {
                Boolean finishedActionMode = actionModeHelper.onClick(item);

                if (finishedActionMode != null && !finishedActionMode) {
                    //last item deselected, finishing action mode
                    return true;
                }
            }
            return !item.isSelectable();
        });

        final BiConsumer<FastAdapter<T>, ActionMode> finalUpdateActionMode = updateActionMode;
        fastAdapter.withOnClickListener((v, adapter2, item, position) -> {
            if (actionModeHelper.getActionMode() != null) {
                finalUpdateActionMode.accept(fastAdapter, actionModeHelper.getActionMode());
                return true;
            }
            return defaultOnClick != null && defaultOnClick.onClick(v, adapter2, item, position);
        });
        fastAdapter.withOnPreLongClickListener((v, adapter13, item, position) -> actionModeHelper.onLongClick(activity, position) == null);
        fastAdapter.withOnLongClickListener((v, adapter12, item, position) -> {
            if (actionModeHelper.getActionMode() != null) finalUpdateActionMode.accept(fastAdapter, actionModeHelper.getActionMode());
            return true;
        });
        return fastAdapter;
    }


    @FunctionalInterface
    public interface BiConsumer<T, U> {
        void accept(T var1, U var2);
    }

//    private void updateActionMode.accept(FastItemAdapter<> fastAdapter, ActionMode actionMode) {
//        actionMode.setTitle(String.valueOf(fastAdapter.getSelectedItems().size()));
//    }
}
