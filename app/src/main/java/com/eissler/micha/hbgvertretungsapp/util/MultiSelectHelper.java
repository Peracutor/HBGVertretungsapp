package com.eissler.micha.hbgvertretungsapp.util;


import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;


/**
 * Created by Micha.
 * 23.12.2016
 */

public class MultiSelectHelper<T extends IItem, F extends FastAdapter<T>> {
    private F fastAdapter;
    private FastAdapter.OnClickListener<T> onClickListener;
    private FastAdapter.OnClickListener<T> onPreClickListener;
    private ActionModeHelper actionModeHelper;
//    private FastAdapter.OnClickListener<T> onClickListenerExternal;

    public MultiSelectHelper(F fastAdapter) {
        this.fastAdapter = fastAdapter;
    }

//    public static <T extends IItem> FastAdapter<T> setupMultiSelectAdapter()

    public F setupMultiSelectAdapter(@NonNull AppCompatActivity activity, @MenuRes int menuId, @NonNull ActionMode.Callback callback, @Nullable BiConsumer<FastAdapter<T>, ActionMode> updateActionMode,
                                     @Nullable FastAdapter.OnClickListener<T> defaultOnClick, @Nullable OnClickView... onClickViews) {
        fastAdapter.withSelectable(true);
        fastAdapter.withSelectOnLongClick(true);
        fastAdapter.withMultiSelect(true);

        if (updateActionMode == null) {
            updateActionMode = (fastAdapter1, actionMode) -> actionMode.setTitle(String.valueOf(fastAdapter1.getSelectedItems().size()));
        }

        actionModeHelper = new ActionModeHelper(fastAdapter, menuId, callback);

        fastAdapter.withOnPreClickListener(onPreClickListener = (v, adapter1, item, position) -> {
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
        fastAdapter.withOnClickListener(onClickListener = (v, adapter2, item, position) -> {
            if (actionModeHelper.getActionMode() != null) {
                finalUpdateActionMode.accept(fastAdapter, actionModeHelper.getActionMode());
                return true;
            }
            return defaultOnClick != null && defaultOnClick.onClick(v, adapter2, item, position);
        });
        fastAdapter.withOnPreLongClickListener((v, adapter3, item, position) -> actionModeHelper.onLongClick(activity, position) == null);
        fastAdapter.withOnLongClickListener((v, adapter4, item, position) -> {
            if (actionModeHelper.getActionMode() != null) finalUpdateActionMode.accept(fastAdapter, actionModeHelper.getActionMode());
            return true;
        });

        if (onClickViews != null) {
            for (OnClickView onClickView : onClickViews) {
                fastAdapter.withItemEvent(new CheckBoxClickEvent(onClickView));
            }
        }
        return fastAdapter;
    }

    public ActionMode getActionMode() {
        return actionModeHelper.getActionMode();
    }


    @FunctionalInterface
    public interface BiConsumer<T, U> {
        void accept(T var1, U var2);
    }

    private class CheckBoxClickEvent extends ClickEventHook<T> {
        private OnClickView onClickView;

        private CheckBoxClickEvent(OnClickView onClickView) {
            this.onClickView = onClickView;
        }

        @Override
        public View onBind(@NonNull RecyclerView.ViewHolder viewHolder) {
            return onClickView.getOnClickView(viewHolder);
        }

        @Override
        public void onClick(View v, int position, FastAdapter<T> fastAdapter, T item) {
            boolean consumed = onPreClickListener.onClick(v, fastAdapter.getAdapter(position), item, position);
            if (consumed) {
                return;
            }
            fastAdapter.toggleSelection(position);
            onClickListener.onClick(v, fastAdapter.getAdapter(position), item, position);
        }
    }

    public interface OnClickView {
        View getOnClickView(RecyclerView.ViewHolder viewHolder);
    }
}
