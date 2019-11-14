package com.letrans.android.translator.mpush.domain;

public class ServerResponse {
    public int code;
    public String result;
    public String message;

    @Override
    public String toString() {
        return "ServerResponse{" +
                "code=" + code +
                ", result='" + result + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
