package com.letrans.android.translator.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.letrans.android.translator.R;

public class BaseDialog extends DialogFragment implements View.OnClickListener {
    private TextView mTitleView;
    private View mTopLineView;
    private View mBottomLineView;
    protected ViewGroup mContentView;
    protected TextView mButton1;
    protected TextView mButton2;
    protected TextView mButton3;
    private View mButtonContainer;
    private View buttonDivider1;
    private View buttonDivider2;

    public static final int BUTTON_POSITIVE = -1;
    public static final int BUTTON_NEUTRAL = -2;
    public static final int BUTTON_NEGATIVE = -3;

    private DialogParams mDialogParams;
    protected Context mContext;

    private DialogInterface.OnDismissListener mOnDismissListener;

    public BaseDialog() {
        mDialogParams = new DialogParams();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.DialogStyle);
        View view = createView();
        bindView();
        updateButtonBackground();
        builder.setView(view);
        return builder.create();
    }

    @NonNull
    protected View createView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_layout, null);
        mTitleView = (TextView) view.findViewById(R.id.title);
        mTopLineView = view.findViewById(R.id.top_divider);
        mBottomLineView = view.findViewById(R.id.bottom_divider);
        mContentView = (ViewGroup) view.findViewById(R.id.content_view);
        mButton1 = (TextView) view.findViewById(R.id.button1);
        mButton2 = (TextView) view.findViewById(R.id.button2);
        mButton3 = (TextView) view.findViewById(R.id.button3);
        mButtonContainer = view.findViewById(R.id.button_container);
        buttonDivider1 = view.findViewById(R.id.button_divider_1);
        buttonDivider2 = view.findViewById(R.id.button_divider_2);
        return view;
    }

    protected void bindView() {
        if (mDialogParams == null) {
            return;
        }
        if (!TextUtils.isEmpty(mDialogParams.title)) {
            mTitleView.setVisibility(View.VISIBLE);
            mTitleView.setText(mDialogParams.title);
            mTopLineView.setVisibility(mDialogParams.topLineVisible ? View.VISIBLE : View.GONE);
        } else {
            mTitleView.setVisibility(View.GONE);
            mTopLineView.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(mDialogParams.button1)
                && TextUtils.isEmpty(mDialogParams.button2)
                && TextUtils.isEmpty(mDialogParams.button3)) {
            mBottomLineView.setVisibility(View.GONE);
            // mButtonContainer.setVisibility(View.GONE);
        } else {
            mBottomLineView.setVisibility(
                    mDialogParams.bottomLineVisible ? View.VISIBLE : View.GONE);
            // mButtonContainer.setVisibility(View.VISIBLE);
        }
        if (mDialogParams.customContentView != null) {
            mContentView.addView(mDialogParams.customContentView);
        } else if (!TextUtils.isEmpty(mDialogParams.contentMsg)) {
            TextView msgView = (TextView) LayoutInflater.from(mContext).inflate(
                    R.layout.dialog_msg_view, null);
            msgView.setText(mDialogParams.contentMsg);
            mContentView.addView(msgView);
        } else {
            mContentView.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mDialogParams.button1)) {
            mButton1.setVisibility(View.VISIBLE);
            mButton1.setText(mDialogParams.button1);
            mButton1.setOnClickListener(this);
        } else {
            mButton1.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mDialogParams.button2)) {
            mButton2.setVisibility(View.VISIBLE);
            mButton2.setText(mDialogParams.button2);
            mButton2.setOnClickListener(this);
        } else {
            mButton2.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mDialogParams.button3)) {
            mButton3.setVisibility(View.VISIBLE);
            mButton3.setText(mDialogParams.button3);
            mButton3.setOnClickListener(this);
        } else {
            mButton3.setVisibility(View.GONE);
        }
    }

    private void updateButtonBackground() {
        boolean button1Visible = mButton1.getVisibility() == View.VISIBLE;
        boolean button2Visible = mButton2.getVisibility() == View.VISIBLE;
        boolean button3Visible = mButton3.getVisibility() == View.VISIBLE;
        if (button1Visible && button2Visible && button3Visible) {
            buttonDivider1.setVisibility(View.VISIBLE);
            buttonDivider2.setVisibility(View.VISIBLE);
            mButton1.setBackgroundResource(R.drawable.dialog_right_button_background_material);
            mButton2.setBackgroundResource(R.drawable.dialog_middle_button_background_material);
            mButton3.setBackgroundResource(R.drawable.dialog_left_button_background_material);
        } else if (button1Visible && button2Visible) {
            buttonDivider1.setVisibility(View.VISIBLE);
            mButton1.setBackgroundResource(R.drawable.dialog_right_button_background_material);
            mButton2.setBackgroundResource(R.drawable.dialog_left_button_background_material);
        } else if (button1Visible && button3Visible) {
            buttonDivider1.setVisibility(View.VISIBLE);
            mButton1.setBackgroundResource(R.drawable.dialog_right_button_background_material);
            mButton3.setBackgroundResource(R.drawable.dialog_left_button_background_material);
        } else if (button2Visible && button3Visible) {
            buttonDivider2.setVisibility(View.VISIBLE);
            mButton2.setBackgroundResource(R.drawable.dialog_right_button_background_material);
            mButton3.setBackgroundResource(R.drawable.dialog_left_button_background_material);
        } else if (button1Visible) {
            mButton1.setBackgroundResource(R.drawable.dialog_one_button_background_material);
        } else if (button2Visible) {
            mButton2.setBackgroundResource(R.drawable.dialog_one_button_background_material);
        } else if (button3Visible) {
            mButton3.setBackgroundResource(R.drawable.dialog_one_button_background_material);
        } else {
            mButtonContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (mDialogParams == null) {
            return;
        }
        if (mDialogParams.listener == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.button1:
                mDialogParams.listener.onClick(BUTTON_POSITIVE);
                break;
            case R.id.button2:
                mDialogParams.listener.onClick(BUTTON_NEUTRAL);
                break;
            case R.id.button3:
                mDialogParams.listener.onClick(BUTTON_NEGATIVE);
                break;
        }
        dismiss();
    }

    /**
     * @param buttonId Please use {@link #BUTTON_POSITIVE}
     *                 or {@link #BUTTON_POSITIVE}
     *                 or {@link #BUTTON_POSITIVE}
     * @return view
     */
    public View getButton(int buttonId) {
        View view = null;
        switch (buttonId) {
            case BUTTON_POSITIVE:
                view = mButton1;
                break;
            case BUTTON_NEUTRAL:
                view = mButton2;
                break;
            case BUTTON_NEGATIVE:
                view = mButton3;
                break;
        }
        return view;
    }

    private void setDialogParams(DialogParams params) {
        mDialogParams.copyFrom(params);
    }

    public void setTitle(int res) {
        mDialogParams.title = mContext.getResources().getString(res);
    }

    public void setTitle(String title) {
        mDialogParams.title = title;
    }

    public void setTopLineVisible(boolean visible) {
        mDialogParams.topLineVisible = visible;
    }

    public void setBottomLineVisible(boolean visible) {
        mDialogParams.bottomLineVisible = visible;
    }

    public void setMessage(int res) {
        mDialogParams.contentMsg = mContext.getResources().getString(res);
    }

    public void setMessage(String msg) {
        mDialogParams.contentMsg = msg;
    }

    public void setContentView(int res) {
        mDialogParams.customContentView = LayoutInflater.from(mContext).inflate(res, null);
    }

    public void setContentView(View view) {
        mDialogParams.customContentView = view;
    }

    public void setPositiveButton(int res) {
        mDialogParams.button1 = mContext.getResources().getString(res);
    }

    public void setPositiveButton(String positiveLabel) {
        mDialogParams.button1 = positiveLabel;
    }

    public void setNegativeButton(int res) {
        mDialogParams.button3 = mContext.getResources().getString(res);
    }

    public void setNegativeButton(String negativeLabel) {
        mDialogParams.button3 = negativeLabel;
    }

    public void setNeutralButton(int res) {
        mDialogParams.button2 = mContext.getResources().getString(res);
    }

    public void setNeutralButton(String neutralLabel) {
        mDialogParams.button2 = neutralLabel;
    }

    public void setButtonListener(OnButtonClickListener listener) {
        mDialogParams.listener = listener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss(dialog);
        }
    }

    public void setDialogDismissListener(DialogInterface.OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    public static class Builder {
        private DialogParams params;
        private Context mContext;

        public Builder(Context context) {
            mContext = context;
            params = new DialogParams();
        }

        public Builder setTitle(int res) {
            params.title = mContext.getResources().getString(res);
            return this;
        }

        public Builder setTitle(String title) {
            params.title = title;
            return this;
        }

        public Builder setTopLineVisible(boolean visible) {
            params.topLineVisible = visible;
            return this;
        }

        public Builder setBottomLineVisible(boolean visible) {
            params.bottomLineVisible = visible;
            return this;
        }

        public Builder setMessage(int res) {
            params.contentMsg = mContext.getResources().getString(res);
            return this;
        }

        public Builder setMessage(String msg) {
            params.contentMsg = msg;
            return this;
        }

        public Builder setContentView(int res) {
            params.customContentView = LayoutInflater.from(mContext).inflate(res, null);
            return this;
        }

        public Builder setContentView(View view) {
            params.customContentView = view;
            return this;
        }

        public Builder setPositiveButton(int res) {
            params.button1 = mContext.getResources().getString(res);
            return this;
        }

        public Builder setPositiveButton(String positiveLabel) {
            params.button1 = positiveLabel;
            return this;
        }

        public Builder setNegativeButton(int res) {
            params.button3 = mContext.getResources().getString(res);
            return this;
        }

        public Builder setNegativeButton(String negativeLabel) {
            params.button3 = negativeLabel;
            return this;
        }

        public Builder setNeutralButton(int res) {
            params.button2 = mContext.getResources().getString(res);
            return this;
        }

        public Builder setNeutralButton(String neutralLabel) {
            params.button2 = neutralLabel;
            return this;
        }

        public Builder setButtonListener(OnButtonClickListener listener) {
            params.listener = listener;
            return this;
        }

        public BaseDialog build() {
            BaseDialog baseDialog = new BaseDialog();
            baseDialog.setDialogParams(params);
            return baseDialog;
        }
    }

    static class DialogParams {
        String title;
        boolean topLineVisible;
        boolean bottomLineVisible;
        String contentMsg;
        View customContentView;
        String button1;
        String button2;
        String button3;

        OnButtonClickListener listener;

        void copyFrom(DialogParams params) {
            title = params.title;
            topLineVisible = params.topLineVisible;
            bottomLineVisible = params.bottomLineVisible;
            contentMsg = params.contentMsg;
            customContentView = params.customContentView;
            button1 = params.button1;
            button2 = params.button2;
            button3 = params.button3;
            listener = params.listener;
        }
    }

    public interface OnButtonClickListener {
        void onClick(int which);
    }
}
