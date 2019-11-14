package com.letrans.android.translator.tts;

import android.content.Context;
import com.letrans.android.translator.microsoft.bing.tts.bing.BTTS;
import com.letrans.android.translator.microsoft.bing.tts.microsoft.MSTTS;
import com.letrans.android.translator.roobo.tts.RooboTTS;
import com.letrans.android.translator.utils.Logger;


public class TTSManager {

    private static final String TAG = "RTranslator/TTSManager";
    private static TTSManager mInstance;
    private static int TTS_IFLYTEK = 0;
    private static int TTS_MICROSOFT = 1;
    private static int TTS_ROOBO = 2;
    private static int TTS_BING = 3;
    private static int TTS_COUNT = 4;
    private static String IFLYTEK = "iflytek";
    private static String MICROSOFT = "microsoft";
    private static String ROOBO = "roobo";
    private static String BING = "bing";
    private static String GOOGLE = "google";
    String[] types = new String[]{IFLYTEK,MICROSOFT,ROOBO,BING};
    ITTS tts;


    private TTSManager(Context context) {
        Logger.d(TAG,"TTSManager init ");
    }

    public static TTSManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (TTSManager.class) {
                if (mInstance == null) {
                    mInstance = new TTSManager(context);
                }
            }
        }
        return mInstance;
    }

    public ITTS getTTS(Context context, String language, boolean male , String type) {
        Logger.d(TAG,"lang : " + language + " type : " + type);
        if (ROOBO.equals(type)) {
            tts = new RooboTTS();
        } else if (MICROSOFT.equals(type)) {
            tts = new MSTTS(context);
        } else if (BING.equals(type)) {
            tts = new BTTS(context);
        } else if (GOOGLE.equals(type)) {
            tts = new BTTS(context);
        } else {
            tts = new MSTTS(context);
        }
        return tts;
    }


    public void releaseAll() {
        if (tts != null) {
            tts.release();
            tts = null;
        }
    }

}
