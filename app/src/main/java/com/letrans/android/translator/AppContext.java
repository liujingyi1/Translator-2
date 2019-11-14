package com.letrans.android.translator;

import android.os.Environment;

public class AppContext {
    public static final String SOUND_FILE_DIR = "rtranslator";
    public static final String SOUND_ABSOLUTE_FILE_DIR = Environment.getExternalStorageDirectory() + "/" + SOUND_FILE_DIR;

    public static final String ACTION_NETWORK_SETTING = "com.translator.setting.network";
    public static final String ACTION_PAIR_SETTING = "com.translator.setting.pair";
    public static final String ACTION_ROLE_SETTING = "com.translator.setting.role";
    public static final String ACTION_COMMON_SETTING = "com.translator.setting.common";
    public static final String ACTION_STORAGE_SETTING = "com.translator.setting.storage";
    public static final String ACTION_OTA_SETTING = "com.translator.setting.ota";
    public static final String ACTION_ABOUT_SETTING = "com.translator.setting.about";
    public static final String ACTION_MAINSCREEN_SETTING = "com.translator.setting.mainscreen";

    public static final String LOCAL_SERVER_THREAD_ID = "RGK";

    public static final boolean USE_TRANSITION_ANIM = false;

    public static String IFLYTEK = "iflytek";
    public static String MICROSOFT_SPEECH = "microsoft_speech";
    public static String MICROSOFT_SPEECH_TRANSLATE = "microsoft_speech_translate";
    public static String MICROSOFT_TRANSLATE = "microsoft_translate";
    public static String MICROSOFT_BING = "microsoft_bing";
    public static String BAIDU = "baidu";
    public static String ROOBO = "roobo";
    public static String GOOGLE = "google";
    public static String BING = "bing";
    public static String MICROSOFT = "microsoft";

    public static final int STYLE_TYPE_LIGHT = 1;
    public static final int STYLE_TYPE_DRAK = 2;
}
