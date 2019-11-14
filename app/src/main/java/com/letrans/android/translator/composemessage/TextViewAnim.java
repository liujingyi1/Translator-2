package com.letrans.android.translator.composemessage;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.widget.TextView;

import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.view.VerticalImageSpan;

import java.util.List;

public class TextViewAnim {
    private static final String TAG = "RTranslator/TextViewAnim";

    private static final int MSG_PLAY = 1;
    private static final int MSG_STOP = 2;

    private static TextViewAnim instance = null;

    private static Context mContext;
    private TextView textView;
    private String text;
    private SpannableString spannableString;
    private Status status = Status.IDLE;

    private List<List<Integer>> animSrcList;
    private int idleImgInex;
    private int animSrcListSize;
    private int mResIndex = 0;

    private Thread animThread;
    private TextViewAnim() {}

    public synchronized static TextViewAnim getInstance(Context context) {
        mContext = context;
        if (instance == null) {
            instance = new TextViewAnim();
        }

        return instance;
    }

    Handler H = new Handler() {
        int i = 0;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PLAY: {
                    if (i >= animSrcListSize) {
                        i = 0;
                    }
                    // Logger.v(TAG, "MSG_PLAY-"+textView);
                    VerticalImageSpan imageSpan = new VerticalImageSpan(mContext, animSrcList.get(mResIndex).get(i).intValue());
                    int l = text.length();
                    spannableString.setSpan(imageSpan, l, l + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textView.setText(spannableString);

                    i++;
                    H.sendEmptyMessageDelayed(MSG_PLAY, 200);
                    break;
                }

                case MSG_STOP: {
                    // Logger.v(TAG, "MSG_STOP");
                    VerticalImageSpan imageSpan = new VerticalImageSpan(mContext, animSrcList.get(mResIndex).get(idleImgInex).intValue());
                    int l = text.length();
                    spannableString.setSpan(imageSpan, l, l + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textView.setText(spannableString);
                    break;
                }
            }
        }
    };

    public void setAnimSrcList(List<List<Integer>> animSrcList) {
        this.animSrcList = animSrcList;
    }

    public void setResIndex(int index) {
        if (status == Status.PLAYING) {
            stopPlayAnim();
        }
        mResIndex = index;
        this.animSrcListSize = animSrcList.get(mResIndex).size();
    }

    public void startPlayAnim(TextView textView, String text, int idleImgInex) {
        // Logger.v(TAG, "startPlayAnim");
        if (status == Status.PLAYING) {
            stopPlayAnim();
        }

        status = Status.PLAYING;
        this.textView = textView;
        this.text = text;
        this.animSrcList = animSrcList;
        this.idleImgInex = idleImgInex;
        this.spannableString  = new SpannableString(text + " ");
        this.animSrcListSize = animSrcList.get(mResIndex).size();

        H.sendEmptyMessageDelayed(MSG_PLAY, 200);
    }

    public void stopPlayAnim() {
        // Logger.v(TAG, "stopPlayAnim");
        if(status == Status.PLAYING) {
            H.removeMessages(MSG_PLAY);
            VerticalImageSpan imageSpan = new VerticalImageSpan(mContext, animSrcList.get(mResIndex).get(idleImgInex).intValue());
            int l = text.length();
            spannableString.setSpan(imageSpan, l, l + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannableString);
            textView = null;
        }
        status = Status.IDLE;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    enum Status {
        IDLE,
        PLAYING
    }
}
