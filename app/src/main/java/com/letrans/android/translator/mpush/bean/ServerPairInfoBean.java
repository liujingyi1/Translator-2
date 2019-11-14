package com.letrans.android.translator.mpush.bean;

import java.util.List;

public class ServerPairInfoBean {
    public int code;
    public String message;
    public List<PairBean> result;

    static public class PairBean{
        int id;
        public String deviceIdH;
        public String deviceIdW;

    }

    @Override
    public String toString() {
        return "ServerPairInfoBean{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", result=" + result +
                '}';
    }
}
