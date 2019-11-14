package com.letrans.android.translator.lyy;

import java.util.List;

public class LyyRequest {
    public static String PROCESS_CODE_TOKEN = "0001";
    public static String PROCESS_CODE_TT = "0002";

    public static String DEVICE_TYPE_TABLET = "01";
    public static String DEVICE_TYPE_PHONE = "02";
    public static String DEVICE_TYPE_OTHER = "03";

    public static String OPFLAG_TT = "01";
    public static String OPFLAG_TS = "02";
    public static String OPFLAG_ST = "03";
    public static String OPFLAG_SS = "04";
    public static String OPFLAG_STS = "05";

    String processCode;
    OrderContent orderContent;

    static class OrderContent {
        List<Detail> DetailList;
    }

    static class Detail {
        String token;
        String internalflag;
        String langfrom;
        String langto;
        String inparam;

        @Override
        public String toString() {
            return "Detail{" +
                    "token='" + token + '\'' +
                    ", internalflag='" + internalflag + '\'' +
                    ", langfrom='" + langfrom + '\'' +
                    ", langto='" + langto + '\'' +
                    ", inparam='" + inparam + '\'' +
                    '}';
        }
    }

}
