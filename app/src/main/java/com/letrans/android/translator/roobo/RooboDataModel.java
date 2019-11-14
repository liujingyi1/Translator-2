package com.letrans.android.translator.roobo;

import java.util.HashMap;

public class RooboDataModel {

    private static HashMap<String, String> speakerMap = new HashMap<>();

    static {
        //online
        speakerMap.put("ar-EG", RConstants.VOICE_AR_EG);
        speakerMap.put("ar-SA", RConstants.VOICE_AR_SA);
        speakerMap.put("in-ID", RConstants.VOICE_IN_ID);
        speakerMap.put("zh-HK", RConstants.VOICE_ZH_HK);
        speakerMap.put("ca-ES", RConstants.VOICE_CA_ES);
        speakerMap.put("cs-CZ", RConstants.VOICE_CS_CZ);
        speakerMap.put("da-DK", RConstants.VOICE_DA_DK);
        speakerMap.put("nl-NL", RConstants.VOICE_NL_NL);
        speakerMap.put("de-DE", RConstants.VOICE_DE_DE);
        speakerMap.put("el-GR", RConstants.VOICE_EL_GR);
        speakerMap.put("en-AU", RConstants.VOICE_EN_AU);
        speakerMap.put("en-GB", RConstants.VOICE_EN_GB);
        speakerMap.put("en-IN", RConstants.VOICE_EN_IN);
        speakerMap.put("en-US", RConstants.VOICE_EN_US);
        speakerMap.put("fi-FI", RConstants.VOICE_FI_FI);
        speakerMap.put("es-ES", RConstants.VOICE_ES_ES);
        speakerMap.put("es-MX", RConstants.VOICE_ES_MX);
        speakerMap.put("fr-CA", RConstants.VOICE_FR_CA);
        speakerMap.put("fr-FR", RConstants.VOICE_FR_FR);
        speakerMap.put("he-IL", RConstants.VOICE_HE_IL);
        speakerMap.put("hi-IN", RConstants.VOICE_HI_IN);
        speakerMap.put("hu-HU", RConstants.VOICE_HU_HU);
        speakerMap.put("it-IT", RConstants.VOICE_IT_IT);
        speakerMap.put("ja-JP", RConstants.VOICE_JA_JP);
        speakerMap.put("ko-KR", RConstants.VOICE_KO_KR);
        speakerMap.put("ms-MY", RConstants.VOICE_MS_MY);
        speakerMap.put("nb-NO", RConstants.VOICE_NB_NO);
        speakerMap.put("pl-PL", RConstants.VOICE_PL_PL);
        speakerMap.put("pt-BR", RConstants.VOICE_PT_BR);
        speakerMap.put("pt-PT", RConstants.VOICE_PT_PT);
        speakerMap.put("ro-RO", RConstants.VOICE_RO_RO);
        speakerMap.put("ru-RU", RConstants.VOICE_RU_RU);
        speakerMap.put("sk-SK", RConstants.VOICE_SK_SK);
        speakerMap.put("sv-SE", RConstants.VOICE_SV_SE);
        speakerMap.put("th-TH", RConstants.VOICE_TH_TH);
        speakerMap.put("tr-TR", RConstants.VOICE_TR_TR);
        speakerMap.put("zh-CN", RConstants.VOICE_ZH_CN);
        speakerMap.put("zh-TW", RConstants.VOICE_ZH_TW);
        speakerMap.put("vi", RConstants.VOICE_VI);
        speakerMap.put("en-CA", RConstants.VOICE_EN_US); // 英语 加拿大
        speakerMap.put("en-NZ", RConstants.VOICE_EN_US); // 英语 新西兰
        speakerMap.put("en-IE", RConstants.VOICE_EN_US); // 英语 爱尔兰

        //offline
        speakerMap.put("ar-EG_OFF", RConstants.NAME_AR_EG);
        speakerMap.put("ar-SA_OFF", RConstants.NAME_AR_SA);
        speakerMap.put("in-ID_OFF", RConstants.NAME_IN_ID);
        speakerMap.put("zh-HK_OFF", RConstants.NAME_ZH_HK);
        speakerMap.put("ca-ES_OFF", RConstants.NAME_CA_ES);
        speakerMap.put("hr-HR_OFF", RConstants.NAME_HR_HR);
        speakerMap.put("cs-CZ_OFF", RConstants.NAME_CS_CZ);
        speakerMap.put("da-DK_OFF", RConstants.NAME_DA_DK);
        speakerMap.put("nl-NL_OFF", RConstants.NAME_NL_NL);
        speakerMap.put("de-DE_OFF", RConstants.NAME_DE_DE);
        speakerMap.put("el-GR_OFF", RConstants.NAME_EL_GR);
        speakerMap.put("en-AU_OFF", RConstants.NAME_EN_AU);
        speakerMap.put("en-GB_OFF", RConstants.NAME_EN_GB);
        speakerMap.put("en-IN_OFF", RConstants.NAME_EN_IN);
        speakerMap.put("en-NZ_OFF", RConstants.NAME_EN_NZ);
        speakerMap.put("en-IE_OFF", RConstants.NAME_EN_IE);
        speakerMap.put("en-US_OFF", RConstants.NAME_EN_US);
        speakerMap.put("fi-FI_OFF", RConstants.NAME_FI_FI);
        speakerMap.put("es-ES_OFF", RConstants.NAME_ES_ES);
        speakerMap.put("es-MX_OFF", RConstants.NAME_ES_MX);
        speakerMap.put("fr-CA_OFF", RConstants.NAME_FR_CA);
        speakerMap.put("fr-FR_OFF", RConstants.NAME_FR_FR);
        speakerMap.put("he-IL_OFF", RConstants.NAME_HE_IL);
        speakerMap.put("hi-IN_OFF", RConstants.NAME_HI_IN);
        speakerMap.put("hu-HU_OFF", RConstants.NAME_HU_HU);
        speakerMap.put("it-IT_OFF", RConstants.NAME_IT_IT);
        speakerMap.put("ja-JP_OFF", RConstants.NAME_JA_JP);
        speakerMap.put("ko-KR_OFF", RConstants.NAME_KO_KR);
        speakerMap.put("ms-MY_OFF", RConstants.NAME_MS_MY);
        speakerMap.put("nb-NO_OFF", RConstants.NAME_NB_NO);
        speakerMap.put("pl-PL_OFF", RConstants.NAME_PL_PL);
        speakerMap.put("pt-BR_OFF", RConstants.NAME_PT_BR);
        speakerMap.put("pt-PT_OFF", RConstants.NAME_PT_PT);
        speakerMap.put("ro-RO_OFF", RConstants.NAME_RO_RO);
        speakerMap.put("ru-RU_OFF", RConstants.NAME_RU_RU);
        speakerMap.put("sk-SK_OFF", RConstants.NAME_SK_SK);
        speakerMap.put("sv-SE_OFF", RConstants.NAME_SV_SE);
        speakerMap.put("th-TH_OFF", RConstants.NAME_TH_TH);
        speakerMap.put("tr-TR_OFF", RConstants.NAME_TR_TR);
        speakerMap.put("zh-CN_OFF", RConstants.NAME_ZH_CN);
        speakerMap.put("zh-TW_OFF", RConstants.NAME_ZH_TW);
    }
    
    public static String getVoiceCode(String language, boolean male, boolean isCloud) {
        String voiceCode = isCloud ? RConstants.VOICE_EN_US : RConstants.NAME_EN_US;
        language = isCloud ? language : language + "_OFF";
        if (speakerMap != null) {
            voiceCode = speakerMap.get(language);
        }
        return voiceCode;
    }
}
