package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.evaluation.CoverMessage;
import com.eissler.micha.hbgvertretungsapp.evaluation.HBGMessage;
import com.eissler.micha.hbgvertretungsapp.evaluation.HeaderMessage;

import java.util.ArrayList;

import butterknife.BindView;

public class HbgListAdapter extends SelectionAdapter {

    public static HbgListAdapter sLoadingAdapter;

    private static final int MESSAGE_ITEM = 0;
    private static final int DATE_HEADER_ITEM = 1;

    private static final int VIEW_TYPE_COUNT = 2;

    private final Context context;
    private final ArrayList<HBGMessage> mMessagesList;

    public HbgListAdapter(Context context, ArrayList<HBGMessage> messageslist) {
        super(0, context, ViewHolder.class, DateViewHolder.class);
        this.context = context;
        this.mMessagesList = messageslist;
    }

    @Override
    public int getItemViewType(int position) {
        return mMessagesList.get(position).getMessageType() == HBGMessage.Type.HEADER_MESSAGE ? DATE_HEADER_ITEM : MESSAGE_ITEM;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != DATE_HEADER_ITEM && (!getFieldOfList(position, CoverMessage.SUBJECT).equals("") || !getFieldOfList(position, CoverMessage.NEW_SUBJECT).equals(""));
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

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

    @Override
    public View getView(int position, @NonNull View convertView, ViewGroup parent, @NonNull Object viewHolder, boolean isSelected) {
        HBGMessage hbgMessage = mMessagesList.get(position);

        if (getItemViewType(position) == DATE_HEADER_ITEM) {
            TextView dateView = ((DateViewHolder) viewHolder).dateView;
            dateView.setText(((HeaderMessage) hbgMessage).getHeaderString());

        } else {
            ViewHolder holder = (ViewHolder) convertView.getTag();

            CoverMessage coverMessage = (CoverMessage) hbgMessage;
            final String message = coverMessage.getField(CoverMessage.KIND);

            String newSubject = coverMessage.getField(CoverMessage.NEW_SUBJECT);
            newSubject = newSubject.equals("") ? "" : ": " + newSubject;
            final String room = coverMessage.getField(CoverMessage.ROOM);
            final String coverText = coverMessage.getField(CoverMessage.COVER_TEXT);
            final String hour = coverMessage.getField(CoverMessage.HOUR);

            holder.hourView.setText(hour);

            String subject = coverMessage.getField(CoverMessage.SUBJECT);

            if (subject.equals("")) {
                holder.messageView.setText(message + newSubject);
            } else if (!subject.equals(coverMessage.getField(CoverMessage.NEW_SUBJECT))) {
                holder.messageView.setText(subject + " " + message + newSubject);
            } else {
                holder.messageView.setText(subject + " " + message);
            }

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageView.getLayoutParams();
            if (!coverText.equals("")) {
                params.addRule(RelativeLayout.ABOVE, R.id.coverText);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            } else {
                params.addRule(RelativeLayout.ABOVE, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            }


//            holder.messageView.post(new Runnable() {
//                @Override
//                public void run() {
//                    ViewGroup.LayoutParams params = holder.mainLayout.getLayoutParams();
//                    if (holder.messageView.getLineCount() > 1 && !coverText.equals("")) {
//                        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, context.getResources().getDisplayMetrics());
//                    } else {
//                        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
//                    }
//                    holder.mainLayout.setLayoutParams(params);
//                }
//            });

            if (isSelected) {
//                holder.mainLayout.setBackgroundResource(R.drawable.row_selected);
                holder.frameLayout.setBackgroundColor(context.getResources().getColor(R.color.selected));
            } else {
//                holder.mainLayout.setBackgroundResource(R.drawable.row_not_selected);
                holder.frameLayout.setBackgroundColor(0x00ffffff);
            }

            if (!(coverText.equals("") || coverText.equals("---"))) {
                holder.coverView.setText(coverText);
            } else {
                holder.coverView.setText("");
            }

            if (room.equals("") || room.equals("---")) {
                holder.roomText.setText("");
                holder.roomView.setText("");
            } else {
                holder.roomText.setText("Raum");
                holder.roomView.setText(room.substring(1));
            }
        }

        return convertView;
    }

    @ViewHolderResource(R.layout.row_layout)
    @ForItemViewType(MESSAGE_ITEM)
    static class ViewHolder {
        @BindView(R.id.frameLayout) FrameLayout frameLayout;
        @BindView(R.id.message) TextView messageView;
        @BindView(R.id.hour) TextView hourView;
        @BindView(R.id.coverText)  TextView coverView;
        @BindView(R.id.roomText) TextView roomText;
        @BindView(R.id.roomNumber) TextView roomView;
    }

    @ViewHolderResource(R.layout.row_layout_date)
    @ForItemViewType(DATE_HEADER_ITEM)
    static class DateViewHolder {
        @BindView(R.id.date) TextView dateView;
    }

    public String getFieldOfList(int position, int field) {
        return ((CoverMessage) mMessagesList.get(position)).getField(field);
    }

    public static HbgListAdapter getNoDataAdapter(Context context) {
        ArrayList<HBGMessage> listData = new ArrayList<>(1);
        listData.add(new HeaderMessage("Es liegen keine Vertretungsdaten vor.")); //this is not the only place where this message is set, see SortedCoverMessages
        return new HbgListAdapter(context, listData);
    }

    static HbgListAdapter getLoadingAdapter(Context context) {
        App.logTrace("getLoadingAdapter");

        if (sLoadingAdapter == null) {
            ArrayList<HBGMessage> arrayList = new ArrayList<>(1);
            arrayList.add(new HeaderMessage("Lade Vertretungsdaten..."));
            sLoadingAdapter = new HbgListAdapter(context, arrayList);
        }
        return sLoadingAdapter;
    }


}
