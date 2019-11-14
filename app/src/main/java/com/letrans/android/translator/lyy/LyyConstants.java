package com.letrans.android.translator.lyy;

public class LyyConstants {

    public static String PROCESS_CODE_TOKEN = "0001";
    public static String PROCESS_CODE_TT = "0002";
    public static String PROCESS_CODE_ST = "0004";
    public static String PROCESS_CODE_GETNOTICE = "0019";

    public static String DEVICE_TYPE_TABLET = "01";
    public static String DEVICE_TYPE_PHONE = "02";
    public static String DEVICE_TYPE_OTHER = "03";

    public static String OPFLAG_TT = "01";
    public static String OPFLAG_TS = "02";
    public static String OPFLAG_ST = "03";
    public static String OPFLAG_SS = "04";
    public static String OPFLAG_STS = "05";

    public static String TOKEN_TYPE_TT = "01";
    public static String TOKEN_TYPE_TS = "02";
    public static String TOKEN_TYPE_ST = "03";
    public static String TOKEN_TYPE_SS = "04";
    public static String TOKEN_TYPE_STS = "05";
    public static String TOKEN_TYPE_NOTICE = "18";

    public static String OPFLAG_NOTICE_ALL = "01";
    public static String OPFLAG_NOTICE_PIC = "02";
    public static String OPFLAG_NOTICE_TEXT = "03";
    public static String OPFLAG_NOTICE_OTHER = "04";

    public static String ST_OPFLAG_MICROSOFT = "00";
    public static String ST_OPFLAG_GOOGLE = "01";
    public static String ST_OPFLAG_BAIDU = "02";
    public static String ST_OPFLAG_YOUDAO = "03";
    public static String ST_OPFLAG_XUNFEI = "04";

    public static String RESULT_CODE_SUCCESS = "0";
    public static String RESULT_CODE_FAIL = "1";
    public static String RESULT_CODE_UNKNOWN = "-1";

    public static String getCountryCode(String code, String internalflag){

        switch (code) {
            case "in-ID":
                return "id";
            case "zh-CN":
                return "zh-Hans";
            case "zh-HK":
                return "yue";
        }
        return code;
    }
}
