package com.letrans.android.translator.mpush;

import java.util.Arrays;

public class ReceiveMessageBean {

    public static int TYPE_SOUND = 1; //声音
    public static int TYPE_TEXT = 2;  //文字
    public static int TYPE_TEXT_NOTRANSLATE = 20;  //文字未翻译
    public static int TYPE_TEXT_TRANSLATED = 21;  //文字已翻译
    public static int TYPE_PAIR = 3; //配对
    public static int TYPE_RELIEVE_PAIR =4; //取消配对
    public static int TYPE_NOTICE = 5; // 公告
    public static int TYPE_HUNGUP = 6; // 挂断
    public static int TYPE_ENTER = 7; //进入会话

    public byte[] content;//  String content1 = new String(content, Constants.UTF_8);
    public int type; //type
    public String fromLanguage; //源语种
    public String msgId;
    public String from; //对方DeviceId
    public String channel; // ServerTheradId
    public boolean isTranslated; // 是否已经翻译

    @Override
    public String toString() {
        return "ReceiveMessageBean{" +
                "content=" + Arrays.toString(content) +
                ", type=" + type +
                ", fromLanguage='" + fromLanguage + '\'' +
                ", msgId='" + msgId + '\'' +
                ", from='" + from + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }
}
