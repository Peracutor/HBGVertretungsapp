package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.peracutor.comfyadapters.SelectionAdapter;
import com.peracutor.comfyadapters.ViewHolderResource;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HeaderMessage;

import java.util.ArrayList;

import butterknife.BindView;

public class HbgListAdapter extends SelectionAdapter { // TODO: 27.10.2016 unused

//    public static final int MESSAGE_ITEM = 0;
//    public static final int DATE_HEADER_ITEM = 1;

//    private static final int VIEW_TYPE_COUNT = 2;

    private final Context context;
    private final ArrayList<HBGMessage> mMessagesList;

    public HbgListAdapter(Context context, ArrayList<HBGMessage> messageslist) {
        super(0, context, ViewHolder.class, DateViewHolder.class);
        this.context = context;
        this.mMessagesList = messageslist;
    }

    public ArrayList<HBGMessage> getData() {
        return mMessagesList;
    }

//    @Override
//    public int getItemViewType(int position) {
//        return mMessagesList.get(position) instanceof CoverMessage ? MESSAGE_ITEM : DATE_HEADER_ITEM;
//    }


    @Override
    protected com.peracutor.comfyadapters.ViewHolder instantiate(Class<? extends com.peracutor.comfyadapters.ViewHolder> viewHolderClass) {
        return viewHolderClass.equals(ViewHolder.class) ? new ViewHolder() : new DateViewHolder();
    }

    @Override
    protected Class<? extends com.peracutor.comfyadapters.ViewHolder> getViewHolderType(int position) {
        return mMessagesList.get(position) instanceof CoverMessage ? ViewHolder.class : DateViewHolder.class;
    }

    @Override
    public boolean isEnabled(int position) {
        return (getViewHolderType(position) == ViewHolder.class) && (!getFieldOfList(position, CoverMessage.SUBJECT).equals("") || !getFieldOfList(position, CoverMessage.NEW_SUBJECT).equals(""));
    }

//    @Override
//    public int getViewTypeCount() {
//        return VIEW_TYPE_COUNT;
//    }

    @Override
    public int getCount() {
        return mMessagesList.size();
    }

    @Override
    public Object getItem(int position) {
        return mMessagesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //    @Override
//    public View getView(int position, @NonNull View convertView, ViewGroup parent, @NonNull Object viewHolder, boolean isSelected) {
//        HBGMessage hbgMessage = getData().get(position);
//
//        if (getItemViewType(position) == DATE_HEADER_ITEM) {
//
//
//        } else {
//            ViewHolder holder = (ViewHolder) convertView.getTag();
//
//
//
//
////            holder.messageView.post(new Runnable() {
////                @Override
////                public void run() {
////                    ViewGroup.LayoutParams params = holder.mainLayout.getLayoutParams();
////                    if (holder.messageView.getLineCount() > 1 && !coverText.equals("")) {
////                        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, context.getResources().getDisplayMetrics());
////                    } else {
////                        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
////                    }
////                    holder.mainLayout.setLayoutParams(params);
////                }
////            });
//
//            if (isSelected) {
////                holder.mainLayout.setBackgroundResource(R.drawable.row_selected);
//                holder.frameLayout.setBackgroundColor(context.getResources().getColor(R.color.selected));
//            } else {
////                holder.mainLayout.setBackgroundResource(R.drawable.row_not_selected);
//                holder.frameLayout.setBackgroundColor(0x00ffffff);
//            }
//
//
//        }
//
//        return convertView;
//    }

    @ViewHolderResource(R.layout.row_layout)
//    @ForItemViewType(MESSAGE_ITEM)
    public class ViewHolder extends SelectableViewHolder {
        @BindView(R.id.frameLayout) FrameLayout frameLayout;
        @BindView(R.id.message) TextView messageView;
        @BindView(R.id.hour) TextView hourView;
        @BindView(R.id.coverText)  TextView coverView;
        @BindView(R.id.roomText) TextView roomText;
        @BindView(R.id.roomNumber) TextView roomView;

        @Override
        public void recycleSelectableViewHolder(int position, View convertView) {
            CoverMessage coverMessage = (CoverMessage) getData().get(position);
            final String message = coverMessage.get(CoverMessage.KIND);

            String newSubject = coverMessage.get(CoverMessage.NEW_SUBJECT);
            newSubject = newSubject.equals("") ? "" : ": " + newSubject;
            final String room = coverMessage.get(CoverMessage.ROOM);
            final String coverText = coverMessage.get(CoverMessage.COVER_TEXT);
            final String hour = coverMessage.get(CoverMessage.HOUR);

            hourView.setText(hour);

            String subject = coverMessage.get(CoverMessage.SUBJECT);

            if (subject.equals("")) {
                messageView.setText(message + newSubject);
            } else if (!subject.equals(coverMessage.get(CoverMessage.NEW_SUBJECT))) {
                messageView.setText(subject + " " + message + newSubject);
            } else {
                messageView.setText(subject + " " + message);
            }

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageView.getLayoutParams();
            if (!coverText.equals("")) {
                params.addRule(RelativeLayout.ABOVE, R.id.coverText);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            } else {
                params.addRule(RelativeLayout.ABOVE, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            }

            if (!(coverText.equals("") || coverText.equals("---"))) {
                coverView.setText(coverText);
            } else {
                coverView.setText("");
            }

            if (room.equals("") || room.equals("---")) {
                roomText.setText("");
                roomView.setText("");
            } else {
                roomText.setText("Raum");
                roomView.setText(room.substring(1));
            }
        }

        @Override
        public void onItemSelected(int position) {
            frameLayout.setBackgroundColor(context.getResources().getColor(R.color.selected));
        }

        @Override
        public void onItemDeselected(int position) {
            frameLayout.setBackgroundColor(0x00ffffff);
        }

        @Override
        protected void onSelectionModeEnabled(int position) {

        }

        @Override
        protected void onSelectionModeDisabled(int position) {

        }
    }

    @ViewHolderResource(R.layout.row_layout_date)
//    @ForItemViewType(DATE_HEADER_ITEM)
    public class DateViewHolder extends com.peracutor.comfyadapters.ViewHolder {
        @BindView(R.id.date) TextView dateView;

        @Override
        public void recycleViewHolder(int position, View convertView) {
//            System.out.println("getFieldOfList(position, CoverMessage.COVER_TEXT) = " + getFieldOfList(position, CoverMessage.COVER_TEXT));
            HeaderMessage headerMessage = (HeaderMessage) getData().get(position);
            dateView.setText(headerMessage.getHeaderString());
        }
    }

    public String getFieldOfList(int position, int field) {
        return ((CoverMessage) mMessagesList.get(position)).get(field);
    }

    public static HbgListAdapter getNoDataAdapter(Context context) {
        ArrayList<HBGMessage> listData = new ArrayList<>(1);
        listData.add(new HeaderMessage("Es liegen keine Vertretungsdaten vor.")); //this is not the only place where this message is set, see SortedCoverMessages
        return new HbgListAdapter(context, listData);
    }

    public static HbgListAdapter getLoadingAdapter(Context context) {
        App.logTrace("getLoadingAdapter");

        ArrayList<HBGMessage> arrayList = new ArrayList<>(1);
        arrayList.add(new HeaderMessage("Lade Vertretungsdaten..."));
        return new HbgListAdapter(context, arrayList);
    }


}
