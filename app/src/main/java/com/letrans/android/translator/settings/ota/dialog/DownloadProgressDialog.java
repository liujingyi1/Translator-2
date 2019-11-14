package com.letrans.android.translator.settings.ota.dialog;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.letrans.android.translator.R;

import java.text.NumberFormat;

public class DownloadProgressDialog extends AlertDialog {
    private ProgressBar mProgress;
    private TextView mProgressNumber;
    private TextView mProgressPercent;
    private TextView mProgressMessage;
    private Handler mViewUpdateHandler;
    private int mMax;
    private CharSequence mMessage;
    private boolean mHasStarted;
    private int mProgressVal;
    private String mProgressNumberFormat;
    private NumberFormat mProgressPercentFormat;
    private boolean mIndeterminate;

    public DownloadProgressDialog(Context context) {
        super(context, R.style.DialogStyle);
        // TODO Auto-generated constructor stub
        initFormats();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_progress_dialog);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mProgressNumber = (TextView) findViewById(R.id.progress_number);
        mProgressPercent = (TextView) findViewById(R.id.progress_percent);
        mProgressMessage = (TextView) findViewById(R.id.progress_message);
        mViewUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
                int progress = mProgress.getProgress();
                int max = mProgress.getMax();
                if (mProgressNumberFormat != null) {
                    String format = mProgressNumberFormat;
                    if (progress == 0) {
                        mProgressNumber.setVisibility(View.INVISIBLE);
                    } else {
                        mProgressNumber.setVisibility(View.VISIBLE);
                        mProgressNumber.setText(String.format(format, progress,
                                max));
                    }
                } else {
                    mProgressNumber.setText("");
                }
                if (mProgressPercentFormat != null) {
                    double percent = (double) progress / (double) max;
                    SpannableString tmp = new SpannableString(
                            mProgressPercentFormat.format(percent));
                    tmp.setSpan(new StyleSpan(Typeface.NORMAL),
                            0, tmp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mProgressPercent.setText(tmp);
                } else {
                    mProgressPercent.setText("");
                }
            }
        };

        if (mMessage != null) {
            setMessage(mMessage);
        }
        if (mMax > 0) {
            setMax(mMax);
        }
        if (mProgressVal > 0) {
            setProgress(mProgressVal);
        }
        setIndeterminate(mIndeterminate);
        onProgressChanged();
    }

    private void initFormats() {
        mProgressNumberFormat = "%1d/%2d";
        mProgressPercentFormat = NumberFormat.getPercentInstance();
        mProgressPercentFormat.setMaximumFractionDigits(0);
    }

    private void onProgressChanged() {
        if (mViewUpdateHandler != null && !mViewUpdateHandler.hasMessages(0)) {
            mViewUpdateHandler.sendEmptyMessage(0);
        }
    }

    public void setProgressStyle(int style) {
        // mProgressStyle = style;
    }

    public int getMax() {
        if (mProgress != null) {
            return mProgress.getMax();
        }
        return mMax;
    }

    public void setMax(int max) {
        if (mProgress != null) {
            mProgress.setMax(max);
            onProgressChanged();
        } else {
            mMax = max;
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mProgress != null) {
            mProgress.setIndeterminate(indeterminate);
        } else {
            mIndeterminate = indeterminate;
        }
    }

    public void setProgress(int value) {
        if (mHasStarted) {
            mProgress.setProgress(value);
            onProgressChanged();
        } else {
            mProgressVal = value;
        }
    }

    @Override
    public void setMessage(CharSequence message) {
        // TODO Auto-generated method stub
        // super.setMessage(message);
        if (mProgressMessage != null) {
            mProgressMessage.setText(message);
        } else {
            mMessage = message;
        }
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        mHasStarted = true;
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        mHasStarted = false;
    }

    public void setProgressNumberFormat(String format) {
        mProgressNumberFormat = format;
        if (TextUtils.isEmpty(format)) {
            mProgressPercentFormat = null;
        }
        onProgressChanged();
    }

}
