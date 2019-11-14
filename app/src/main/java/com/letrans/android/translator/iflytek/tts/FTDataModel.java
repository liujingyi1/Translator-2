package com.letrans.android.translator.iflytek.tts;

import java.util.HashMap;

public class FTDataModel {

    private HashMap<String, String> speakerMap = new HashMap<>();

    public FTDataModel() {
        initData();
    }


    public String getVoiceName(String language, boolean male) {
        String voiceName = FTConstants.VOICE_NAME_EN;
        if (speakerMap != null) {
            voiceName = speakerMap.get(language);
        }
        return voiceName;
    }

    private void initData() {
        if (speakerMap != null) {
            speakerMap.clear();
            speakerMap.put("fr-FR", FTConstants.VOICE_NAME_FR);
            speakerMap.put("ru-RU", FTConstants.VOICE_NAME_RU);
            speakerMap.put("en-US", FTConstants.VOICE_NAME_EN);
            speakerMap.put("en-AU", FTConstants.VOICE_NAME_EN);
            speakerMap.put("en-GB", FTConstants.VOICE_NAME_EN);
            speakerMap.put("en-NZ", FTConstants.VOICE_NAME_EN);
            speakerMap.put("en-IN", FTConstants.VOICE_NAME_EN);
            speakerMap.put("zh-HK", FTConstants.VOICE_NAME_HK);
            speakerMap.put("zh-TW", FTConstants.VOICE_NAME_TW);
            speakerMap.put("es-ES", FTConstants.VOICE_NAME_ES);
            speakerMap.put("hi-IN", FTConstants.VOICE_NAME_IN);
            speakerMap.put("vi-VN", FTConstants.VOICE_NAME_VI);
            speakerMap.put("zh-CN", FTConstants.VOICE_NAME_CN);
            speakerMap.put("zh-CN_SC", FTConstants.VOICE_NAME_CN_SC);
            speakerMap.put("zh-CN_DB", FTConstants.VOICE_NAME_CN_DB);
            speakerMap.put("zh-CN_HEN", FTConstants.VOICE_NAME_CN_HEN);
            speakerMap.put("zh-CN_HUN", FTConstants.VOICE_NAME_CN_HUN);
            speakerMap.put("zh-CN_SX", FTConstants.VOICE_NAME_CN_SX);
            speakerMap.put("zh-CN_PT", FTConstants.VOICE_NAME_CN_PT);
        }
    }
}
