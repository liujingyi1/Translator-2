package com.letrans.android.translator.baidu.stt.online;

import android.app.Activity;

import com.baidu.speech.asr.SpeechConstant;
import com.letrans.android.translator.baidu.stt.CommonRecogParams;

import java.util.Arrays;

public class OnlineRecogParams extends CommonRecogParams {


    private static final String TAG = "RTranslator/OnlineRecogParams";

    public OnlineRecogParams(Activity context) {
        super(context);

        stringParams.addAll(Arrays.asList(
                "_language", // 用于生成PID参数
                "_model" // 用于生成PID参数
        ));

        intParams.addAll(Arrays.asList(SpeechConstant.PROP));

        boolParams.addAll(Arrays.asList(SpeechConstant.DISABLE_PUNCTUATION));
    }
}
