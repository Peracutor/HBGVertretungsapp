package com.peracutor.comfyadapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;

public abstract class ComfyBaseAdapter extends BaseAdapter {

    private final Map<Class<? extends ViewHolder>, ViewHolderInfo> viewHolders;
    private final int viewTypeCount;
    private final LayoutInflater inflater;
    private final Class<? extends ViewHolder> defaultViewHolderType;


    @SafeVarargs
    public ComfyBaseAdapter(Context context, Class<? extends ViewHolder>... viewHolderClasses) {
        if (viewHolderClasses == null || viewHolderClasses.length == 0) {
            throw new IllegalArgumentException("You have to pass at least one ViewHolder-Class");
        }
        viewTypeCount = viewHolderClasses.length;
        viewHolders = new HashMap<>(viewHolderClasses.length);


        for (int i = 0, viewHolderClassesLength = viewHolderClasses.length; i < viewHolderClassesLength; i++) {
            Class<? extends ViewHolder> viewHolderClass = viewHolderClasses[i];
            int layoutResource = viewHolderClass.getAnnotation(ViewHolderResource.class).value();

            ViewHolderInfo viewHolderInfo;
            viewHolderInfo = new ViewHolderInfo(layoutResource, i);

            viewHolders.put(viewHolderClass, viewHolderInfo);
        }

        if (viewHolderClasses.length == 1) {
            defaultViewHolderType = viewHolderClasses[0];
        } else {
            defaultViewHolderType = null;
        }

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    protected abstract ViewHolder instantiate(Class<? extends ViewHolder> viewHolderClass);

    protected Class<? extends ViewHolder> getViewHolderType(int position) {
        if (defaultViewHolderType == null) {
            throw new IllegalArgumentException("getViewHolderType must be overwritten when using multiple ViewHolders");
        }
        return defaultViewHolderType;
    }

    @Override
    final public View getView(int position, View convertView, ViewGroup parent) {

        Class<? extends ViewHolder> type = getViewHolderType(position);
        // // TODO: 24.10.2016 throw Exception if wrong classes returned by getViewHolderType
        ViewHolderInfo viewHolderInfo = viewHolders.get(type);
        ViewHolder viewHolder;

        boolean convertViewNotUsable = (convertView == null || convertView.getTag() == null || !convertView.getTag().getClass().equals(type));

        if (convertViewNotUsable) {
            viewHolder = instantiate(type);

            int layoutResource = viewHolderInfo.getLayoutResource();

            convertView = inflater.inflate(layoutResource, parent, false);

            ButterKnife.bind(viewHolder, convertView);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.recycleViewHolder(position, convertView);

        return convertView;
    }

    @Override
    final public int getViewTypeCount() {
        return viewTypeCount;
    }

    @Override
    final public int getItemViewType(int position) {
        return viewHolders.get(getViewHolderType(position)).getItemViewType();
    }

    private static class ViewHolderInfo {

        private final int layoutResource;
        private int itemViewType;

        public ViewHolderInfo(int layoutResource, int itemViewType) {
            this.layoutResource = layoutResource;
            this.itemViewType = itemViewType;
        }

        public int getLayoutResource() {
            return layoutResource;
        }

        public int getItemViewType() {
            return itemViewType;
        }
    }


}
