package com.letrans.android.translator.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.letrans.android.translator.R;
import com.letrans.android.translator.utils.Utils;

public class AudioRecorderButton extends android.support.v7.widget.AppCompatButton {

    private static final int DISTANCE_Y_CANCEL_DP = 50;
    private static final int MAX_VOCIE_LEVEL = 7;

    private static final int STATE_NORMAL = 1;
    private static final int STATE_RECORDING = 2;
    private static final int STATE_WANT_TO_CANCEL = 3;

    private int mCurState = STATE_NORMAL;
    private boolean isRecording = false;
    private int DISTANCE_Y_CANCEL;
    private float mTime;
    //是否开始弹框开始录制
    private boolean mReady;

    private RecorderDialogManager mRecorderDialogManager;


    public AudioRecorderButton(Context context) {
        this(context, null);
    }

    public AudioRecorderButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        DISTANCE_Y_CANCEL = Utils.dp2px(context, DISTANCE_Y_CANCEL_DP);
//        mRecorderDialogManager = new RecorderDialogManager(context);
    }

    public void setRecorderDialogManager(RecorderDialogManager recorderDialogManager) {
        mRecorderDialogManager = recorderDialogManager;
    }

    /**
     * 录音完成后的回调
     */
    public interface onAudioRecorderStateListener {
        void onFinish(boolean isToShort, int stats);

        void onStart();
    }

    private onAudioRecorderStateListener mListener;

    public void setAudioRecorderStateListener(onAudioRecorderStateListener listener) {
        mListener = listener;
    }


    private static final int MSG_AUDIO_PREPARED = 1001;
    private static final int MSG_DIALOG_DISMISS = 1002;

    private Handler H = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AUDIO_PREPARED: {
                    mRecorderDialogManager.showRecordingDialog();
                    mRecorderDialogManager.recording();
                    isRecording = true;
                    break;
                }

                case MSG_DIALOG_DISMISS: {
                    mRecorderDialogManager.dismissDialog();
                    ;
                    reset();
                    break;
                }
            }
        }
    };

    public void updateVoiceLevel(int level) {
        mRecorderDialogManager.updateVoiceLevel(level);
    }

    public void dismissRecordingDialog() {
        H.sendEmptyMessage(MSG_DIALOG_DISMISS);
    }

    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mTime = 0f;
                changeState(STATE_RECORDING);

                if (mListener != null) {
                    mListener.onStart();
                }
                H.sendEmptyMessage(MSG_AUDIO_PREPARED);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isRecording) {
                    if (wantToCancel(x, y)) {
                        changeState(STATE_WANT_TO_CANCEL);
                    } else {
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (mListener != null) {
                    mListener.onFinish(!isRecording || mTime < 0.6f, mCurState);
                }

                break;
            }
        }

        return super.onTouchEvent(event);
    }

    private void reset() {
        changeState(STATE_NORMAL);
        isRecording = false;
        mTime = 0f;
    }

    private boolean wantToCancel(int x, int y) {
        if (x < 0 || x > getWidth()) {
            return true;
        }

        if (y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL) {
            return true;
        }
        return false;
    }

    private void changeState(int state) {
        if (mCurState != state) {
            mCurState = state;
            switch (state) {
                case STATE_NORMAL: {
                    setBackgroundResource(R.drawable.btn_recorder_normal);
                    setText(R.string.recoder_normal);
                    break;
                }

                case STATE_RECORDING: {
                    setBackgroundResource(R.drawable.btn_recorder_recording);
                    setText(R.string.recoder_recording);
                    if (isRecording) {
                        mRecorderDialogManager.recording();
                    }
                    break;
                }

                case STATE_WANT_TO_CANCEL: {
                    setBackgroundResource(R.drawable.btn_recorder_recording);
                    setText(R.string.recoder_want_cancel);
                    mRecorderDialogManager.wantToCancel();
                    break;
                }
            }
        }
    }
}
