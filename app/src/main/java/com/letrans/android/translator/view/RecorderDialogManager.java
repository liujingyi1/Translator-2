package com.letrans.android.translator.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.letrans.android.translator.R;
import com.letrans.android.translator.utils.Logger;

public class RecorderDialogManager {
    private static final String TAG = "RTranslator/RecorderDialogManager";
    private Dialog mDialog;

    private ImageView mIcon;
    private ImageView mVoice;
    private TextView mLabel;

    private Context mContext;
    private String mLabelText;

    public RecorderDialogManager(Context context) {
        mContext = context;
    }

    public void setLabelText(String str){
        mLabelText = str;
        if(mLabel != null){
            mLabel.setText(str);
        }
    }

    public void showRecordingDialog() {
        Logger.v(TAG, "showRecordingDialog");
        mDialog = new Dialog(mContext, R.style.Theme_RecorderAudioDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_recorder, null);
        mDialog.setContentView(view);

        mIcon = (ImageView) mDialog.findViewById(R.id.id_recorder_dialog_icon);
        mVoice = (ImageView) mDialog.findViewById(R.id.id_recorder_dialog_voice);
        mLabel = (TextView) mDialog.findViewById(R.id.id_recorder_dialog_label);

        mDialog.show();
    }

    public void recording() {
        Logger.v(TAG, "recording ");
        if (mDialog != null && mDialog.isShowing()) {
            Logger.v(TAG, "recording-isShowing");
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.VISIBLE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.drawable.ic_recorder_recording);
            if(mLabelText == null){
                mLabel.setText(R.string.recoder_recording);
            }

        }
    }

    public void wantToCancel() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.drawable.ic_recorder_want_cancel);
            if(mLabelText == null) {
                mLabel.setText(R.string.recoder_want_cancel);
            }
        }
    }

    public void tooShort() {
        Logger.v(TAG, "tooShort");
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.drawable.ic_recorder_too_short);
            if(mLabelText == null) {
                mLabel.setText(R.string.recoder_too_short);
            }
        }
    }

    public void dismissDialog() {
        Logger.v(TAG, "dismissDialog");
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    public void updateVoiceLevel(int level) {
//        Logger.v(TAG, "updateVoiceLevel: level="+level);
        if (mDialog != null && mDialog.isShowing()) {
            if (level < 1) {
                level = 1;
            } else if (level > 7) {
                level = 7;
            }

            int resId = mContext.getResources().getIdentifier("ic_recorder_v" + level, "drawable", mContext.getPackageName());
            mVoice.setImageResource(resId);
        }
    }
}
