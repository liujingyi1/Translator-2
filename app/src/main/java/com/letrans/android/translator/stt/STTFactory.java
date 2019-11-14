package com.letrans.android.translator.stt;

import android.app.Activity;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.baidu.stt.BDSTT;
import com.letrans.android.translator.google.stt.GGSTT;
import com.letrans.android.translator.iflytek.stt.FTSTT;
import com.letrans.android.translator.microsoft.bing.stt.MSBSTT;
import com.letrans.android.translator.microsoft.speech.stt.MSSSTT;
import com.letrans.android.translator.microsoft.speech.translate.MSSTSTT;
import com.letrans.android.translator.microsoft.translate.MSTSTT;
import com.letrans.android.translator.roobo.stt.RBSTT;

public class STTFactory {

    private static final String TAG = "RTranslator/STTFactory";

    public static ISTT getSTT(Activity activity, String type) {
        ISTT mSTT = null;

        if (AppContext.MICROSOFT_SPEECH.equals(type)) {
            mSTT = new MSSSTT(activity);
        } else if (AppContext.BAIDU.equals(type)) {
            mSTT = new BDSTT(activity);
        } else if (AppContext.MICROSOFT_SPEECH_TRANSLATE.equals(type)) {
            mSTT = new MSSTSTT(activity);
        } else if (AppContext.MICROSOFT_TRANSLATE.equals(type)) {
            mSTT = new MSTSTT(activity);
        } else if (AppContext.ROOBO.equals(type)) {
            mSTT = new RBSTT();
        } else if (AppContext.MICROSOFT_BING.equals(type)) {
            mSTT = new MSBSTT(activity);
        } else if (AppContext.GOOGLE.equals(type)) {
            mSTT = new GGSTT(activity);
        } else if (AppContext.IFLYTEK.equals(type)) {
            mSTT = new FTSTT(activity);
        }

        return mSTT;
    }

    /* 下面的是用于 STTManager 的，但是我们的业务比较适合Factory

    private static STTFactory mInstance;

    private static String[] types = new String[]{AppContext.IFLYTEK, AppContext.MICROSOFT_SPEECH,
            AppContext.BAIDU,
            AppContext.ROOBO,
            AppContext.MICROSOFT_BING};

    private static HashMap<String, ISTT> mSTT = new HashMap<>();

    private STTFactory(Activity activity) {
        for (String type : types) {
            ISTT stt = null;
            if (AppContext.MICROSOFT_SPEECH.equals(type)) {
                stt = new MSSTT(activity);
            } else if (AppContext.BAIDU.equals(type)) {
                stt = new BDSTT(activity);
            } else if (AppContext.ROOBO.equals(type)) {
                stt = new FTSTT(activity); // TODO change to roobo
            } else if (AppContext.MICROSOFT_BING.equals(type)) {
                stt = new FTSTT(activity); // TODO change to roobo
            } else if (AppContext.IFLYTEK.equals(type)) {
                stt = new FTSTT(activity);
            }
            mSTT.put(type, stt);
        }
    }

    public static STTFactory getInstance(Activity activity) {
        if (mInstance == null) {
            synchronized (STTFactory.class) {
                if (mInstance == null) {
                    mInstance = new STTFactory(activity);
                }
            }
        }
        return mInstance;
    }

    public ISTT getSTT(String type) {
        return mSTT.get(type);
    }
    */
}
