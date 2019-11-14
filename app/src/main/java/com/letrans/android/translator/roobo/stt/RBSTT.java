package com.letrans.android.translator.roobo.stt;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.letrans.android.translator.R;
import com.letrans.android.translator.roobo.RooboDataModel;
import com.letrans.android.translator.roobo.RooboInitListener;
import com.letrans.android.translator.roobo.RooboManager;
import com.letrans.android.translator.stt.ISTT;
import com.letrans.android.translator.stt.ISTTFinishedListener;
import com.letrans.android.translator.stt.ISTTVoiceLevelListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.ToastUtils;
import com.roobo.toolkit.RError;
import com.roobo.vui.api.VUIApi;
import com.roobo.vui.api.asr.RASRListener;
import com.roobo.vui.common.recognizer.ASRResult;
import com.roobo.vui.common.recognizer.EventType;

import java.util.Random;

public class RBSTT implements ISTT {

    private static final String TAG = "RTranslator/RBSTT";

    private static final int MSG_RESET_VOICE_LEVEL = 1001;
    private static final int MSG_SET_RANDOM_VOICE_LEVEL = 1002;

    private ISTTFinishedListener mSTTFinishedListener;
    private ISTTVoiceLevelListener mSTTVoiceLevelListener;

    private String mToken;

    private String mLanguage = null;

    private boolean isInited = false;

    public RBSTT() {
        RooboManager.getInstance().initRoobo(new RooboInitListener() {
            @Override
            public void onSuccess() {
                setListener();

                if (!TextUtils.isEmpty(mLanguage)) {
                    Logger.i(TAG, "setLanguage after init success:" + mLanguage);
                    VUIApi.getInstance().setCloudRecognizeLang(mLanguage);
                }

                isInited = true;
            }

            @Override
            public void onFail() {
                isInited = false;
                Logger.w(TAG, "init failed");

                ToastUtils.showLong(R.string.roobo_init_failed);
            }
        });
    }

    @Override
    public void start(String wavFilePath) {

    }

    @Override
    public void setSTTFinishedListener(ISTTFinishedListener listener) {
        mSTTFinishedListener = listener;
    }

    @Override
    public void setSTTVoiceLevelListener(ISTTVoiceLevelListener listener) {
        mSTTVoiceLevelListener = listener;
    }

    @Override
    public void startWithMicrophone(String wavFilePath, String token) {

        if (!isInited) {
            ToastUtils.showLong(R.string.roobo_init_failed);
            return;
        }

        if (checkRecordStateSuccess()) {
            mToken = token;
            VUIApi.getInstance().startRecognize();

            if (mSTTVoiceLevelListener != null) {
                H.removeMessages(MSG_RESET_VOICE_LEVEL);
                H.removeMessages(MSG_SET_RANDOM_VOICE_LEVEL);
                H.sendEmptyMessageDelayed(MSG_SET_RANDOM_VOICE_LEVEL, 800);
            }
        }
    }

    @Override
    public void stopWithMicrophone() {
        if (isInited) {
            VUIApi.getInstance().stopRecognize();
        }
    }

    @Override
    public void setLanguageCode(String languageCode) {
        String language = RooboDataModel.getVoiceCode(languageCode, false, true);
        Logger.i(TAG, "setLanguageCode:" + language + "|isInited:" + isInited);
        if (isInited) {
            VUIApi.getInstance().setCloudRecognizeLang(language);
        }
        mLanguage = language;
    }

    @Override
    public void setSecondLanguageCode(String languageCode) {

    }

    private void setListener() {

        VUIApi.getInstance().setASRListener(new RASRListener() {

            @Override
            public void onASRResult(ASRResult asrResult) {
                Logger.d(TAG, "onASRResult" + asrResult.getResultText());
                if (mSTTFinishedListener != null) {
                    mSTTFinishedListener.onSTTFinish(FinalResponseStatus.OK, asrResult
                            .getResultText(), mToken);
                    mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Finished, "", mToken);
                }

                if (mSTTVoiceLevelListener != null) {
                    H.removeMessages(MSG_SET_RANDOM_VOICE_LEVEL);
                    H.sendEmptyMessage(MSG_RESET_VOICE_LEVEL);
                }
            }

            @Override
            public void onFail(RError rError) {
                Logger.d(TAG, "onWakeUp" + rError.getFailDetail());
                if (mSTTFinishedListener != null) {
                    mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Error, rError
                            .getFailDetail(), mToken);
                }
            }

            @Override
            public void onWakeUp(String s) {
                Logger.d(TAG, "onWakeUp" + s);
                VUIApi.getInstance().stopSpeak();
            }

            @Override
            public void onEvent(EventType eventType) {
                Logger.d(TAG, "onEvent:" + eventType.toString());
            }
        });
    }

    private Handler H = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RESET_VOICE_LEVEL:
                    if (mSTTVoiceLevelListener != null) {
                        mSTTVoiceLevelListener.updateVoiceLevel(0);
                    }
                    break;
                case MSG_SET_RANDOM_VOICE_LEVEL:
                    if (mSTTVoiceLevelListener != null) {
                        Random rand = new Random();
                        int level = rand.nextInt(4) + 4;
                        mSTTVoiceLevelListener.updateVoiceLevel(level);
                        H.sendEmptyMessageDelayed(MSG_SET_RANDOM_VOICE_LEVEL, 200);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        if (isInited) {
            VUIApi.getInstance().setASRListener(null);
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {
        if (isInited) {
            VUIApi.getInstance().stopSpeak();
        }
    }

    public static boolean checkRecordStateSuccess() {
        int minBuffer = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, (minBuffer * 2));
        short[] point = new short[minBuffer];
        int readSize = 0;
        try {
            audioRecord.startRecording();//检测是否可以进入初始化状态
        } catch (Exception e) {
            if (audioRecord != null) {
                audioRecord.release();
                Logger.d(TAG, "无法进入录音初始状态");
            }
            return false;
        }
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            //6.0以下机型都会返回此状态，故使用时需要判断bulid版本
            //检测是否在录音中
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                Logger.d(TAG, "录音机被占用");
            }
            return false;
        } else {
            //检测是否可以获取录音结果
            readSize = audioRecord.read(point, 0, point.length);
            if (readSize <= 0) {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                }
                Logger.d(TAG, "录音的结果为空");
                return false;

            } else {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                }
                return true;
            }
        }
    }
}
