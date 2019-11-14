package com.letrans.android.translator.mpush.bean;

public class ServerRegistBean {
    public int code;
    public String message;
    public RegistBean result;
    public long timestamp;

    static public class RegistBean{
        int id;
        public String deviceId;
        public String role;
        public String roleLanguage;
        public String language;
        public String profilePic;
        public String pairingCode;
        public int state;
        public String createdDate;
    }
}
