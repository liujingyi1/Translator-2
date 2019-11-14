package com.letrans.android.translator.lyy;

public class LyyResponse {
    public static int RESP_CODE_SUCESS = 0;
    public static int RESP_CODE_FAULT = 1;

    String result;
    String responseTime;
    int respCode;
    String respDesc;

    @Override
    public String toString() {
        return "LyyResponse{" +
                "result='" + result + '\'' +
                ", responseTime='" + responseTime + '\'' +
                ", respCode=" + respCode +
                ", respDesc='" + respDesc + '\'' +
                '}';
    }
}
