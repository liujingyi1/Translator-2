package com.letrans.android.translator.tts;

public class TTSUtils {

    public static int getTTSCount() {
        return 4;
    }

    public static boolean isAsia(String code) {
        if ("zh-CN".contains(code) ||"zh-HK".contains(code) ||"zh-TW".contains(code)
                || "en-AU".contains(code) || "en-IN".contains(code) || "ar-SA".contains(code)
                || "ar-EG".contains(code) || "vi-VN".contains(code) || "ta-IN".contains(code)
                || "hi-IN".contains(code)) {
            return true;
        }
        return false;
    }

    public static boolean isEU(String code) {
        if ("en-GB".contains(code) || "en-IE".contains(code) ||"hr-HR".contains(code)
                || "sl-SI".contains(code)) {
            return true;
        }
        return false;
    }

    public static boolean isUS(String code) {
        if ("en-US".contains(code) || "en-CA".contains(code) || "es-MX".contains(code)
                || "fr-CA".contains(code) || "pt-BR".contains(code)) {
            return true;
        }
        return false;
    }

}

