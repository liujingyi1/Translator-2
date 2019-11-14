package com.letrans.android.translator.mpush.domain;

import android.content.Intent;

public class NoticeResponse {
    public static int NOTICE_TYPE_TEXT = 2;
    public static int NOTICE_TYPE_PIC = 1;

    public Integer code;
    public NoticeResult result;
    public String message;

    public class NoticeResult {
        String id;
        String title;
        Integer type;
        String content;
        String[] picsUrl;
        Device[] devices;
        String state;
        String createdDate;
        String createdBy;
        String modifiedDate;
        String modifiedBy;
    }

    public class Device {
        String id;
        String company;
        String deviceId;
        String deviceAlias;
        String model;
        String createdDate;
        String createdBy;
        String modifiedDate;
        String modifiedBy;
    }

    @Override
    public String toString() {
        return "NoticeResponse{" +
                "code=" + code +
                ", result=" + result +
                ", message='" + message + '\'' +
                '}';
    }
}
