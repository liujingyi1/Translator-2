package com.letrans.android.translator.iflytek.stt;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.sunflower.FlowerCollector;
import com.letrans.android.translator.stt.ISTT;
import com.letrans.android.translator.stt.ISTTFinishedListener;
import com.letrans.android.translator.R;
import com.letrans.android.translator.stt.ISTTVoiceLevelListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class FTSTT implements ISTT {
    private static final String TAG = "RTranslator/FTSTT";

    private Activity mActivity;

    private SpeechRecognizer mIat;

    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private boolean mTranslateEnable = false;
    private String lag = "mandarin";
    private String iat_vadbos_preference = "4000";
    private String iat_vadeos_preference = "1000";
    private String iat_punc_preference = "1";

    private FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;
    private ISTTFinishedListener mSTTFinishedListener;

    private ISTTVoiceLevelListener mSTTVoiceLevelListener;

    private int ret = 0;

    private String mLocal = "zh-cn";

    private String mToken;

    public FTSTT(Activity activity) {
        this.mActivity = activity;

//        SpeechUtility.createUtility(activity, "appid=" + activity.getString(R.string.iflytek_key));
        SpeechUtility.createUtility(activity, "appid=" + Utils.getEncryptString(mActivity, R.string.iflytek_key));

        mIat = SpeechRecognizer.createRecognizer(activity, mInitListener);
    }

    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Logger.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Logger.d(TAG, "SpeechRecognizer init error!");
            }
        }
    };

    @Override
    public void start(String wavFilePath) {
        mIatResults.clear();

        setParam(wavFilePath);

        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        // 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
        // mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
        // mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
        ret = mIat.startListening(mRecognizerListener);

        Logger.d(TAG, "mResult：" + ret);

        if (ret != ErrorCode.SUCCESS) {
            Logger.d(TAG, "start error：" + ret);
        } else {
            byte[] audioData = FucUtil.readAudioRecordFile(mActivity, Environment
                    .getExternalStorageDirectory() + "/rtranslator/iat.wav");

            if (null != audioData) {
                Logger.d(TAG, "Begin recognizer!");
                // 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），
                // 位长16bit，单声道的wav或者pcm
                // 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
                // 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别。
                // 音频切分方法：FucUtil.splitBuffer(byte[] buffer,int length,int spsize);
                mIat.writeAudio(audioData, 0, audioData.length);
                mIat.stopListening();
            } else {
                mIat.cancel();
                Logger.d(TAG, "Read wav failed!");
            }
        }
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
        FlowerCollector.onEvent(mActivity, "iat_recognize");

        mToken = token;

        mIatResults.clear();

        setParam(wavFilePath);

        ret = mIat.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Logger.d(TAG, "startWithMicrophone error：" + ret);
        } else {
            Logger.d(TAG, "startWithMicrophone");
        }
    }

    @Override
    public void stopWithMicrophone() {
        mIat.stopListening();
    }

    private String getDefaultLocale() {
        return mLocal;
    }

    private String getDefaultLocaleForIFlyTek() {
        return mLocal != null ? mLocal.replace("-", "_") : "zh_cn";
    }

    private String getLag() {

        if (mLocal == null) {
            return lag;
        }

        String language = mLocal.substring(mLocal.indexOf("-") + 1, mLocal.length());

        if ("hk".equalsIgnoreCase(language)) {
            lag = "cantonese";
        }

        return lag;
    }

    @Override
    public void setLanguageCode(String languageCode) {
        mLocal = languageCode;
    }

    @Override
    public void setSecondLanguageCode(String languageCode) {

    }

    @Override
    public void onDestroy() {
        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    public void onResume() {
        FlowerCollector.onResume(mActivity);
        FlowerCollector.onPageStart(TAG);
    }

    @Override
    public void onPause() {
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(mActivity);
    }

    public void setParam(String wavFilePath) {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        if (mTranslateEnable) {
            Logger.i(TAG, "translate enable");
            mIat.setParameter(SpeechConstant.ASR_SCH, "1");
            mIat.setParameter(SpeechConstant.ADD_CAP, "translate");
            mIat.setParameter(SpeechConstant.TRS_SRC, "its");
        }

        if (("en_us").equalsIgnoreCase(getDefaultLocaleForIFlyTek())) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, getDefaultLocaleForIFlyTek());
            mIat.setParameter(SpeechConstant.ACCENT, null);

            if (mTranslateEnable) {
                mIat.setParameter(SpeechConstant.ORI_LANG, "en");
                mIat.setParameter(SpeechConstant.TRANS_LANG, "cn");
            }
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, getDefaultLocaleForIFlyTek());
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, getLag());

            if (mTranslateEnable) {
                mIat.setParameter(SpeechConstant.ORI_LANG, "cn");
                mIat.setParameter(SpeechConstant.TRANS_LANG, "en");
            }
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, iat_vadbos_preference);

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, iat_vadeos_preference);

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, iat_punc_preference);

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, wavFilePath);
    }

    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Logger.d(TAG, "onBeginOfSpeech");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if (mTranslateEnable && error.getErrorCode() == 14002) {
                Logger.d(TAG, error.getPlainDescription(true) + "\n confirm open record permission " +
                        "please");
            } else {
                Logger.d(TAG, error.getPlainDescription(true));
            }

            if (mSTTFinishedListener != null) {
                mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Error, "", mToken);
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Logger.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            if (mTranslateEnable) {
                printTransResult(results);
            } else {
                printResult(results);
            }

            if (isLast) {
                if (mSTTFinishedListener != null) {
                    mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Finished, "", mToken);
                }
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            if (mSTTVoiceLevelListener != null) {
                mSTTVoiceLevelListener.updateVoiceLevel(volume);
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Logger.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        if (mSTTFinishedListener != null) {
            mSTTFinishedListener.onSTTFinish(FinalResponseStatus.OK, text, mToken);
        }

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        // Logger.d(TAG, "printResult is:" + resultBuffer.toString());
    }

    private void printTransResult(RecognizerResult results) {
        String trans = JsonParser.parseTransResult(results.getResultString(), "dst");
        String oris = JsonParser.parseTransResult(results.getResultString(), "src");

        if (TextUtils.isEmpty(trans) || TextUtils.isEmpty(oris)) {
            Logger.d(TAG, "Parse error! confirm open translator permission please!");
        } else {
            Logger.d(TAG, "printTransResult is:" + "原始语言:\n" + oris + "\n目标语言:\n" + trans);
        }
    }
}
