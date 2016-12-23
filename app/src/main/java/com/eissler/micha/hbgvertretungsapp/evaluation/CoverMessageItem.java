package com.eissler.micha.hbgvertretungsapp.evaluation;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.R;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.ReplacedCoverMessage;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Micha.
 * 24.10.2016
 */

public class CoverMessageItem extends AbstractItem<CoverMessageItem, CoverMessageItem.ViewHolder> {

    private ReplacedCoverMessage coverMessage;
    private ViewHolder holder;

    public CoverMessageItem(ReplacedCoverMessage coverMessage) {
        this.coverMessage = coverMessage;
    }



    @Override
    public int getType() {
        return R.id.coverText;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.row_layout;
    }

    @Override
    public void bindView(ViewHolder holder, List payloads) {
        super.bindView(holder, payloads);
        if (holder != this.holder) {
            this.holder = holder;
        }

        setItemBackground(isSelected());

        final String message = coverMessage.get(CoverMessage.KIND);

        String newSubject = coverMessage.get(CoverMessage.NEW_SUBJECT);
        newSubject = newSubject.equals("") ? "" : ": " + newSubject;
        final String room = coverMessage.get(CoverMessage.ROOM);
        final String coverText = coverMessage.get(CoverMessage.COVER_TEXT);
        final String hour = coverMessage.get(CoverMessage.HOUR);

        holder.hourView.setText(hour);

        String subject = coverMessage.get(CoverMessage.SUBJECT);

        if (subject.equals("")) {
            holder.messageView.setText(message + newSubject);
        } else if (!subject.equals(coverMessage.get(CoverMessage.NEW_SUBJECT))) {
            holder.messageView.setText(subject + " " + message + newSubject);
        } else {
            holder.messageView.setText(subject + " " + message);
        }

        Context context = holder.messageView.getContext();
        if (coverText.equals("")) {
            holder.coverView.setVisibility(View.GONE);
            setMargins(pxFromDp(context, 8));
            holder.messageView.post(() -> {
                int lineCount = holder.messageView.getLineCount();
                if (lineCount > 1) {
                    setMargins(0);
                }
            });
        } else {
            setMargins(0);
            holder.coverView.setVisibility(View.VISIBLE);
        }

        holder.coverView.setText(coverText);

        if (room.equals("")) {
            holder.roomText.setText("");
            holder.roomView.setText("");
        } else {
            holder.roomText.setText("Raum");
            holder.roomView.setText(room.substring(1));
        }
    }

    private void setMargins(int additionalMargin) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageView.getLayoutParams();
        int defaultMargin = pxFromDp(holder.messageView.getContext(), 6);
        params.setMargins(0, defaultMargin + additionalMargin, 0, defaultMargin);
    }

    public static int pxFromDp(final Context context, final int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }



    @Override
    public CoverMessageItem withSetSelected(boolean selected) {
        setItemBackground(selected);
        return super.withSetSelected(selected);
    }

    public void setItemBackground(boolean selected) {
        if (holder == null) {
            return;
        }
        int color = holder.frameLayout.getContext().getResources().getColor(selected ? R.color.selected : android.R.color.transparent);
        holder.frameLayout.setBackgroundColor(color);
    }

//    private void println(String s) {
//        System.out.println("Item " + getIdentifier() + ": " + s);
//    }

    @Override
    public ViewHolder getViewHolder(View view) {
        return new ViewHolder(view);
    }

    public ReplacedCoverMessage getCoverMessage() {
        return coverMessage;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.frameLayout) FrameLayout frameLayout;
        @BindView(R.id.message) TextView messageView;
        @BindView(R.id.hour) TextView hourView;
        @BindView(R.id.coverText)  TextView coverView;
        @BindView(R.id.roomText) TextView roomText;
        @BindView(R.id.roomNumber) TextView roomView;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
