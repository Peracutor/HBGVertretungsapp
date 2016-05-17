package com.eissler.micha.hbgvertretungsapp;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;

public abstract class NoBoilerPlateAdapter extends BaseAdapter implements NoBoilerInterface {

    private LayoutInflater inflater;
    private Map<Integer, ViewHolderInfo> viewHolders;

    public NoBoilerPlateAdapter(Context context, Class<?>... viewHolderClasses) {
        viewHolders = new HashMap<>(viewHolderClasses.length);
        for (Class<?> viewHolderClass : viewHolderClasses) {

            int layoutResource = viewHolderClass.getAnnotation(ViewHolderResource.class).value();

            ViewHolderInfo viewHolderInfo;
            try {
                viewHolderInfo = new ViewHolderInfo(viewHolderClass, layoutResource);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                return;
            }

            ForItemViewType annotation = viewHolderClass.getAnnotation(ForItemViewType.class);
            if (annotation == null) {
                viewHolders.put(0, viewHolderInfo);
                continue;
            }
            int itemViewType = annotation.value();
            viewHolders.put(itemViewType, viewHolderInfo);
        }

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    final public View getView(int position, View convertView, ViewGroup parent) {
        int itemViewType = getItemViewType(position);

        ViewHolderInfo viewHolderInfo = viewHolders.get(itemViewType);
        Object viewHolder = null;

        boolean convertViewNotUsable = (convertView == null || convertView.getTag() == null || !convertView.getTag().getClass().equals(viewHolderInfo.getViewHolderClass()));

        if (convertViewNotUsable) {
            try {
                viewHolder = viewHolderInfo.getViewHolderClass().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }

            int layoutResource = viewHolderInfo.getLayoutResource();

            convertView = inflater.inflate(layoutResource, parent, false);

            ButterKnife.bind(viewHolder, convertView);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = convertView.getTag();
        }

        return getView(position, convertView, parent, viewHolder);
    }

    private static class ViewHolderInfo {

        private final int layoutResource;
        private final Class<?> viewHolderClass;

        public ViewHolderInfo(Class<?> viewHolderClass, int layoutResource) {
            this.viewHolderClass = viewHolderClass;
            this.layoutResource = layoutResource;
        }

        public int getLayoutResource() {
            return layoutResource;
        }

        public Class<?> getViewHolderClass() {
            return viewHolderClass;
        }
    }

}
