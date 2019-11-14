package com.letrans.android.translator.mpush.domain;

public class RoleInfoResponse {
    public int code;
    public RoleInfo result;
    public String message;

    public class RoleInfo{
        int id;
        String deviceId;
        int role;
        String roleLanguage;
        String language;
        String profilePic;
        String pairingCode;
        String state;
        String createdDate;
    }
}
