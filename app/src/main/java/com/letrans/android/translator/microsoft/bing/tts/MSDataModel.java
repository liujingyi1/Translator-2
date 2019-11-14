package com.letrans.android.translator.microsoft.bing.tts;

import android.content.Context;
import android.text.TextUtils;

import com.letrans.android.translator.R;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.tts.TTSUtils;
import com.letrans.android.translator.utils.Utils;

import java.util.HashMap;

public class MSDataModel {

    private HashMap<String, String> speakerMap = new HashMap<>();
    private static HashMap<String, TtsBean> map = new HashMap<>();

    private static final String uri_westus = "https://westus.tts.speech.microsoft.com/cognitiveservices/v1";
    private static final String uri_asia = "https://eastasia.tts.speech.microsoft.com/cognitiveservices/v1";
    private static final String uri_eu = "https://northeurope.tts.speech.microsoft.com/cognitiveservices/v1";
    private static final String tkhost_westus = "westus.api.cognitive.microsoft.com";
    private static final String tkhost_asia = "eastasia.api.cognitive.microsoft.com";
    private static final String tkhost_eu = "northeurope.api.cognitive.microsoft.com";
    private static final String ttshost_westus = "westus.tts.speech.microsoft.com";
    private static final String ttshost_asia = "eastasia.tts.speech.microsoft.com";
    private static final String ttshost_eu = "northeurope.tts.speech.microsoft.com";
    private static final String baseuri_asia = "https://eastasia.api.cognitive.microsoft.com/sts/v1.0/";
    private static final String baseuri_westus = "https://westus.api.cognitive.microsoft.com/sts/v1.0/";
    private static final String baseuri_eu = "https://northeurope.api.cognitive.microsoft.com/sts/v1.0/";
    public MSDataModel(Context context) {
        initData();
        initTTSData(context);
    }

    private void initData() {
        if (speakerMap != null) {
            speakerMap.clear();
            speakerMap.put("ar-EG", MSConstants.VOICE_AR_EG_FEMALE);
            speakerMap.put("ar-SA_M", MSConstants.VOICE_AR_SA_MALE);
            speakerMap.put("bg-BG_M", MSConstants.VOICE_BG_BG_MALE);
            speakerMap.put("ca-ES", MSConstants.VOICE_CA_ES_FEMALE);
            speakerMap.put("cs-CZ_M", MSConstants.VOICE_CS_CZ_Male);
            speakerMap.put("da-DK", MSConstants.VOICE_DA_DK_FEMALE);
            speakerMap.put("de-AT_M", MSConstants.VOICE_DE_AT_MALE);
            speakerMap.put("de-CH_M", MSConstants.VOICE_DE_CH_MALE);
            speakerMap.put("de-DE", MSConstants.VOICE_DE_DE_FEMALE);
            //speakerMap.put("de-DE1", TTSConstants.VOICE_DE_DE_FEMALE1);
            speakerMap.put("de-DE_M", MSConstants.VOICE_DE_DE_MALE);
            speakerMap.put("el-GR_M", MSConstants.VOICE_EL_GR_MALE);
            speakerMap.put("en-AU", MSConstants.VOICE_EN_AU_FEMALE);
            //speakerMap.put("en-AU1", TTSConstants.VOICE_EN_AU_FEMALE1);
            speakerMap.put("en-CA", MSConstants.VOICE_EN_CA_FEMALE);
            //speakerMap.put("en-CA1", TTSConstants.VOICE_EN_CA_FEMALE1);
            speakerMap.put("en-GB", MSConstants.VOICE_EN_GB_FEMALE);
            speakerMap.put("en-GB1", MSConstants.VOICE_EN_GB_FEMALE1);
            speakerMap.put("en-GB_M", MSConstants.VOICE_EN_GB_MALE);
            speakerMap.put("en-IE_M", MSConstants.VOICE_EN_IE_MALE);
            speakerMap.put("en-IN_M", MSConstants.VOICE_EN_IN_MALE);
            speakerMap.put("en-IN", MSConstants.VOICE_EN_IN_FEMALE);
            //speakerMap.put("en-IN2", TTSConstants.VOICE_EN_IN_FEMALE1);
            speakerMap.put("en-US", MSConstants.VOICE_EN_US_FEMALE);
            //speakerMap.put("en-US1", TTSConstants.VOICE_EN_US_FEMALE1);
            //speakerMap.put("en-US2", TTSConstants.VOICE_EN_US_FEMALE2);
            speakerMap.put("en-US_M", MSConstants.VOICE_EN_US_MALE);
            //speakerMap.put("en-US_M1", TTSConstants.VOICE_EN_US_MALE1);
            speakerMap.put("es-ES_M", MSConstants.VOICE_ES_ES_MALE);
            speakerMap.put("es-ES", MSConstants.VOICE_ES_ES_FEMALE);
            //speakerMap.put("es-ES1", TTSConstants.VOICE_ES_ES_FEMALE1);
            speakerMap.put("es-MX", MSConstants.VOICE_ES_MX_FEMALE);
            speakerMap.put("es-MX_M", MSConstants.VOICE_ES_MX_MALE);
            speakerMap.put("fi-FI", MSConstants.VOICE_FI_FI_FEMALE);
            speakerMap.put("fr-CA", MSConstants.VOICE_FR_CA_FEMALE);
            speakerMap.put("fr-CA1", MSConstants.VOICE_FR_CA_FEMALE1);
            speakerMap.put("fr-CH_M", MSConstants.VOICE_FR_CH_MALE);
            speakerMap.put("fr-FR_M", MSConstants.VOICE_FR_FR_MALE);
            speakerMap.put("fr-FR", MSConstants.VOICE_FR_FR_FEMALE);
            speakerMap.put("he-IL_M", MSConstants.VOICE_HE_IL_MALE);
            speakerMap.put("hi-IN_M", MSConstants.VOICE_HI_IN_MALE);
            speakerMap.put("hi-IN", MSConstants.VOICE_HI_IN_FEMALE);
            //speakerMap.put("hi-IN1", TTSConstants.VOICE_HI_IN_FEMALE1);
            speakerMap.put("hr-HR_M", MSConstants.VOICE_HR_HR_MALE);
            speakerMap.put("hu-HU_M", MSConstants.VOICE_HU_HU_MALE);
            speakerMap.put("id-ID_M", MSConstants.VOICE_ID_ID_MALE);
            speakerMap.put("it-IT_M", MSConstants.VOICE_IT_IT_MALE);
            speakerMap.put("it-IT", MSConstants.VOICE_IT_IT_FEMALE);
            speakerMap.put("ja-JP", MSConstants.VOICE_JA_JP_FEMALE);
            //speakerMap.put("ja-JP1", TTSConstants.VOICE_JA_JP_FEMALE1);
            speakerMap.put("ja-JP_M", MSConstants.VOICE_JA_JP_MALE);
            speakerMap.put("ko-KR", MSConstants.VOICE_KO_KR_FEMALE);
            speakerMap.put("ms-MY_M", MSConstants.VOICE_MS_MY_MALE);
            speakerMap.put("nb-NO", MSConstants.VOICE_NB_NO_FEMALE);
            speakerMap.put("nl-NL", MSConstants.VOICE_NL_NL_FEMALE);
            speakerMap.put("pl-PL", MSConstants.VOICE_PL_PL_FEMALE);
            speakerMap.put("pt-BR", MSConstants.VOICE_PT_BR_FEMALE);
            speakerMap.put("pt-BR_M", MSConstants.VOICE_PT_BR_MALE);
            speakerMap.put("pt-PT", MSConstants.VOICE_PT_PT_FEMALE);
            speakerMap.put("ro-RO_M", MSConstants.VOICE_RO_RO_MALE);
            speakerMap.put("ru-RU_M", MSConstants.VOICE_RU_RU_MALE);
            speakerMap.put("ru-RU", MSConstants.VOICE_RU_RU_FEMALE);
            //speakerMap.put("ru-RU1", TTSConstants.VOICE_RU_RU_FEMALE1);
            speakerMap.put("sk-SK", MSConstants.VOICE_SK_SK_MALE);
            speakerMap.put("sl-SI_M", MSConstants.VOICE_SL_SI_MALE);
            speakerMap.put("sv-SE", MSConstants.VOICE_SV_SE_FEMALE);
            speakerMap.put("ta-IN_M", MSConstants.VOICE_TA_IN_MALE);
            speakerMap.put("ta_M", MSConstants.VOICE_TA_IN_MALE);
            speakerMap.put("th-TH_M", MSConstants.VOICE_TH_TH_MALE);
            speakerMap.put("tr-TR", MSConstants.VOICE_TR_TR_FEMALE);
            speakerMap.put("vi-VN", MSConstants.VOICE_VI_VN_MALE);
            speakerMap.put("vi", MSConstants.VOICE_VI_VN_MALE);
            speakerMap.put("zh-CN_M", MSConstants.VOICE_ZH_CN_MALE);
            speakerMap.put("zh-CN", MSConstants.VOICE_ZH_CN_FEMALE);
            //speakerMap.put("zh-CN1", TTSConstants.VOICE_ZH_CN_FEMALE1);
            speakerMap.put("zh-HK", MSConstants.VOICE_ZH_HK_FEMALE1);
            //speakerMap.put("zh-HK1", TTSConstants.VOICE_ZH_HK_FEMALE);
            speakerMap.put("zh-HK_M", MSConstants.VOICE_ZH_HK_MALE);
            speakerMap.put("zh-TW_M", MSConstants.VOICE_ZH_TW_MALE);
            speakerMap.put("zh-TW", MSConstants.VOICE_ZH_TW_FEMALE);
            //speakerMap.put("zh-TW1", TTSConstants.VOICE_ZH_TW_FEMALE1);
        }
    }

    private void initTTSData(Context context) {
//        String eastasia = context.getString(R.string.ttsKey_eastasia);
        String eastasia = Utils.getEncryptString(context, R.string.ttsKey_eastasia);
        String northeurope = Utils.getEncryptString(context, R.string.ttsKey_northeurope);
        String westus = Utils.getEncryptString(context, R.string.ttsKey_westus);
        TtsBean bean_asia = new TtsBean(eastasia, uri_asia, tkhost_asia, ttshost_asia, baseuri_asia);
        TtsBean bean_us = new TtsBean(westus, uri_westus, tkhost_westus, ttshost_westus, baseuri_westus);
        TtsBean bean_eu = new TtsBean(northeurope, uri_eu, tkhost_eu, ttshost_eu, baseuri_eu);
        map.clear();
        map.put("asia", bean_asia);
        map.put("eu", bean_eu);
        map.put("us", bean_us);
    }

    public String getVoiceName(String language, boolean male) {
        String voiceName = speakerMap.get(language + (male ? "_M" : ""));
        if (voiceName == null) {
            voiceName = speakerMap.get(language + (male ? "" : "_M"));
        }
        if (voiceName == null) {
            return male ? MSConstants.VOICE_EN_US_MALE : MSConstants.VOICE_EN_US_FEMALE;
        }
        return voiceName;
    };

    public static TtsBean getTtsBean() {
        TtsBean bean = map.get("us");

        String key = TStorageManager.getInstance().getTTSKey();
        if (TextUtils.isEmpty(key)) {
            String country = TStorageManager.getInstance().getLocationCountry();
            if (TextUtils.isEmpty(country)) return bean;
            String code = null;
            String[] info = country.split("_");
            if (info.length > 1) {
                code = info[1];
            }
            if (TextUtils.isEmpty(code)) {
                bean = map.get("us");
            } else {
                if (TTSUtils.isAsia(code)) {
                    bean = map.get("asia");
                } else if (TTSUtils.isEU(code)) {
                    bean = map.get("eu");
                } else if (TTSUtils.isUS(code)) {
                    bean = map.get("us");
                }
            }
        } else {
            bean = map.get(key);
        }

        return bean;
    }
}
