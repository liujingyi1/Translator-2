package com.letrans.android.translator.baidu.stt;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.baidu.speech.asr.SpeechConstant;
import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.baidu.stt.online.OnlineRecogParams;
import com.letrans.android.translator.stt.ISTT;
import com.letrans.android.translator.stt.ISTTFinishedListener;
import com.letrans.android.translator.stt.ISTTVoiceLevelListener;
import com.letrans.android.translator.utils.FileUtils;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.PcmToWav;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class BDSTT implements ISTT, IStatus {

    private static final String TAG = "RTranslator/BDSTT";

    private Activity mActivity;

    private String mLocal = "zh-cn";

    // 识别控制器，使用MyRecognizer控制识别的流程
    private BaiduRecognizer mRecognizer;

    // 控制UI按钮的状态
    protected int status;

    // Api的参数类，仅仅用于生成调用START的json字符串，本身与SDK的调用无关
    protected CommonRecogParams apiParams;

    // 本Activity中是否需要调用离线命令词功能。根据此参数，判断是否需要调用SDK的ASR_KWS_LOAD_ENGINE事件
    // protected boolean enableOffline = false;
    // 调用start函数就是离线的

    private ISTTFinishedListener mSTTFinishedListener;
    private ISTTVoiceLevelListener mSTTVoiceLevelListener;

    private String mWavFilePath;

    private String mToken;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    public BDSTT(Activity activity) {
        this.mActivity = activity;
        mRecognizer = BaiduRecognizer.getInstance(activity, mStatusRecogListener);
        apiParams = getApiParams();
        status = STATUS_NONE;
        /*
        if (enableOffline) {
            mRecognizer.loadOfflineEngine(OfflineRecogParams.fetchOfflineParams());
        }
        */
    }

    public CommonRecogParams getApiParams() {
        return new OnlineRecogParams(mActivity);
    }

    @Override
    public void start(String wavFilePath) {
        mRecognizer.start(getParams(true, wavFilePath));
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
        mToken = token;
        mRecognizer.start(getParams(false, wavFilePath));
    }

    private Map<String, Object> getParams(boolean enableOffline, String wavFilePath) {

        if (FileUtils.makeDirWithFile(wavFilePath)) {
            mWavFilePath = wavFilePath;
        } else {
            mWavFilePath = AppContext.SOUND_ABSOLUTE_FILE_DIR + "/Temp/temp.wav";
        }

        Map<String, Object> params = new LinkedHashMap<String, Object>();

        if (enableOffline) {
            params.put(SpeechConstant.DECODER, 2);
            params.put(SpeechConstant.DISABLE_PUNCTUATION, true);
        } else {
            params.put(SpeechConstant.DECODER, 0);
            params.put(SpeechConstant.DISABLE_PUNCTUATION, false);
        }

        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 8000);
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, true);
        params.put(SpeechConstant.ACCEPT_AUDIO_DATA, true);
        params.put(SpeechConstant.OUT_FILE, mWavFilePath.replace(".wav", ".pcm"));
        params.put(SpeechConstant.PID, getPID());

        (new AutoCheck(mActivity, new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                        Logger.w(TAG, "AutoCheckMessage" + message);
                    }
                }
            }
        }, enableOffline)).checkAsr(params);

        String json = null;
        json = new JSONObject(params).toString();

        Logger.i(TAG, "getParams:" + json);

        return params;
    }

    @Override
    public void stopWithMicrophone() {
        mRecognizer.stop();
    }

    @Override
    public void setLanguageCode(String languageCode) {
        mLocal = languageCode;
    }

    @Override
    public void setSecondLanguageCode(String languageCode) {

    }

    private int getPID() {
        if (!TextUtils.isEmpty(mLocal)) {
            switch (mLocal) {
                case "zh-CN":
                    return 1537;
                case "zh-HK":
                    return 1637;
                case "en-US":
                    return 1737;
            }
        }
        return 1537;
    }

    @Override
    public void onDestroy() {
        mRecognizer.release();
        mActivity = null;
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    // 开始录音后，手动停止录音。SDK会识别在此过程中的录音。点击“停止”按钮后调用。
    private void stop() {
        mRecognizer.stop();
    }

    // 开始录音后，取消这次录音。SDK会取消本次识别，回到原始状态。点击“取消”按钮后调用。
    private void cancel() {
        mRecognizer.cancel();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static void bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        Logger.i(TAG, "byte:" + new String(hexChars));
    }

    private IRecogListener mStatusRecogListener = new IRecogListener() {
        @Override
        public void onAsrReady() {
            status = STATUS_READY;
        }

        @Override
        public void onAsrBegin() {
            status = STATUS_SPEAKING;
        }

        @Override
        public void onAsrEnd() {
            status = STATUS_RECOGNITION;
        }

        @Override
        public void onAsrPartialResult(String[] results, RecogResult recogResult) {
            String message = "临时识别结果，结果是“" + results[0] + "”；原始json：" + recogResult.getOrigalJson();
            // 临时的我们就不做处理了，因为业务不需要！
        }

        @Override
        public void onAsrFinalResult(String[] results, RecogResult recogResult) {
            status = STATUS_FINISHED;

            if (mSTTFinishedListener != null) {
                mSTTFinishedListener.onSTTFinish(FinalResponseStatus.OK, results[0], mToken);
                mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Finished, "", mToken);
            }

            Logger.i(TAG, "mWavFilePath:" + mWavFilePath);

            PcmToWav.copyWaveFile(mWavFilePath.replace(".wav", ".pcm"), mWavFilePath);
        }

        @Override
        public void onAsrFinish(RecogResult recogResult) {
            status = STATUS_FINISHED;
            // 识别一段话结束。如果是长语音的情况会继续识别下段话。
        }


        @Override
        public void onAsrFinishError(int errorCode, int subErrorCode, String errorMessage, String descMessage,
                                     RecogResult recogResult) {
            status = STATUS_FINISHED;

            String message = "识别错误, 错误码：" + errorCode + " ," + subErrorCode + " ; " + descMessage;

            Logger.i(TAG, "onAsrFinishError:" + message);

            if (mSTTFinishedListener != null) {
                mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Error, message, mToken);
            }
        }

        /**
         * 长语音识别结束
         */
        @Override
        public void onAsrLongFinish() {
            status = STATUS_FINISHED;
        }

        @Override
        public void onAsrVolume(int volumePercent, int volume) {
//            Logger.v(TAG, "onAsrVolume, volumePercent=" + volumePercent + ", volume=" + volume);
            mSTTVoiceLevelListener.updateVoiceLevel(volume / 500 + 1);
        }

        @Override
        public void onAsrAudio(byte[] data, int offset, int length) {
            if (offset != 0 || data.length != length) {
                byte[] actualData = new byte[length];
                System.arraycopy(data, 0, actualData, 0, length);
                data = actualData;
            }
        }

        @Override
        public void onAsrExit() {
            status = STATUS_NONE;
        }

        @Override
        public void onAsrOnlineNluResult(String nluResult) {
            status = STATUS_FINISHED;
        }

        @Override
        public void onOfflineLoaded() {

        }

        @Override
        public void onOfflineUnLoaded() {

        }
    };
}
