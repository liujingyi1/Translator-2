package com.letrans.android.translator.composemessage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import com.letrans.android.translator.R;
import com.letrans.android.translator.database.DbConstants;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.view.VerticalImageSpan;

public class ComposeListAdapter extends RecyclerView.Adapter<ComposeListAdapter.ComposeListViewHolder> {

    private static final String TAG = "RTranslator/ComposeListAdapter";

    private static final int VIEW_TYPE_SEND = 1;
    private static final int VIEW_TYPE_RECEIVE = 2;

    private Context mContext;
    private List<MessageBean> mDatas;
    private ComposeMessageContact.IComposeMessagePresenter mPresenter;

    private float mTextSize = 14f;
    private int mReceivedPlayDrawableRes = R.drawable.compose_play_recordor_wave_receive_v3;
    private String timeShowing = "";

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public ComposeListAdapter(Context context, List<MessageBean> data, ComposeMessageContact.IComposeMessagePresenter presenter) {
        mContext = context;
        mDatas = data;
        mPresenter = presenter;
    }

    public void setReceivedPlayDrawableRes(int res) {
        mReceivedPlayDrawableRes = res;
    }

    public void addItem(MessageBean item) {
        mDatas.add(item);
        notifyItemInserted(mDatas.size() - 1);
    }

    public void updateItemText(int index, String text) {
        mDatas.get(index).setText(text);
        notifyDataSetChanged();
    }

    public void updateTextSize(float textSize) {
        mTextSize = textSize;
    }

    @NonNull
    @Override
    public ComposeListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Logger.v(TAG, "onCreateViewHolder");
        View view;
        switch (viewType) {
            case VIEW_TYPE_RECEIVE: {
                view = LayoutInflater.from(mContext).inflate(R.layout.item_recorder_receive, parent, false);
                break;
            }

            case VIEW_TYPE_SEND: {
                view = LayoutInflater.from(mContext).inflate(R.layout.item_recorder_send, parent, false);
                break;
            }

            default: {
                view = LayoutInflater.from(mContext).inflate(R.layout.item_recorder_send, parent, false);
                break;
            }
        }

        ComposeListViewHolder holder = new ComposeListViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ComposeListViewHolder holder, final int position) {
        Logger.v(TAG, "onBindViewHolder");
        holder.icon.setImageResource(mPresenter.getLanguageItem(mDatas.get(position).getLanguage()).getProfilPictureRes());
        if (isNeedshowTime(position)) {
            timeShowing = mDatas.get(position).getDate();
            holder.time.setText(mDatas.get(position).getDate());
        } else {
            holder.time.setText(" ");
        }

        if (mDatas.get(position).isSend()) {
            holder.txt.setText(mDatas.get(position).getText());
        } else {
            if (mDatas.get(position).getType() == DbConstants.MessageType.TYPE_TEXT) {
                holder.txt.setText(mDatas.get(position).getText());
            } else {
                SpannableString spannableString = new SpannableString(mDatas.get(position).getText() + " ");
                VerticalImageSpan imageSpan = new VerticalImageSpan(mContext, mReceivedPlayDrawableRes);
                int l = mDatas.get(position).getText().length();
                spannableString.setSpan(imageSpan, l, l + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.txt.setText(spannableString);
            }
        }
        holder.txt.setTextSize(mTextSize);
        holder.txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDatas.get(position).isSend() ? VIEW_TYPE_SEND : VIEW_TYPE_RECEIVE;
    }

    private boolean isNeedshowTime(int position) {
        if (position == 0) {
            return true;
        } else {
            if (mDatas.get(position).getShowTimeType() == MessageBean.SHOW_TIME_TYPE_SHOW) {
                return true;
            } else if (mDatas.get(position).getShowTimeType() == MessageBean.SHOW_TIME_TYPE_NOT_SHOW) {
                return false;
            } else {
                String newTime = mDatas.get(position).getDate();
                String s0[] = newTime.split(" ");
                String s1[] = timeShowing.split(" ");
                if (!s0[0].equals(s1[0])) {
                    mDatas.get(position).setShowTimeType(MessageBean.SHOW_TIME_TYPE_SHOW);
                    return true;
                } else {
                    String ss0[] = s0[1].split(":");
                    String ss1[] = s1[1].split(":");

                    int i = Integer.parseInt(ss0[0]) * 60 + Integer.parseInt(ss0[1]);
                    int j = Integer.parseInt(ss1[0]) * 60 + Integer.parseInt(ss1[1]);
                    if (i - j > 5) {
                        mDatas.get(position).setShowTimeType(MessageBean.SHOW_TIME_TYPE_SHOW);
                        return true;
                    } else {
                        mDatas.get(position).setShowTimeType(MessageBean.SHOW_TIME_TYPE_NOT_SHOW);
                        return false;
                    }
                }
            }
        }
    }

    class ComposeListViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView time;
        TextView txt;

        public ComposeListViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.id_icon);
            time = (TextView) itemView.findViewById(R.id.id_msg_time);
            txt = (TextView) itemView.findViewById(R.id.id_msg_txt);
        }
    }
}
