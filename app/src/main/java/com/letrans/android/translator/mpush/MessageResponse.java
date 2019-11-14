package com.letrans.android.translator.mpush;

import com.letrans.android.translator.mpush.sdk.MPushMessage;

import org.json.JSONObject;

public class MessageResponse implements MPushMessage {
    private String msgId;
    private String title;
    private String content;
    private Integer nid; //主要用于聚合通知，非必填
    private Byte flags; //特性字段。 0x01:声音   0x02:震动 0x03:闪灯
    private String largeIcon; // 大图标
    private String ticker; //和title一样
    private Integer number;
    private JSONObject extras;

    @Override
    public Integer getNid() {
        return nid;
    }

    @Override
    public String getMsgId() {
        return msgId;
    }

    @Override
    public String getTicker() {
        return ticker;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public Integer getNumber() {
        return number;
    }

    @Override
    public Byte getFlags() {
        return flags;
    }

    @Override
    public String getLargeIcon() {
        return largeIcon;
    }

    public MessageResponse setMsgId(String msgId) {
        this.msgId = msgId;
        return this;
    }

    public MessageResponse setTitle(String title) {
        this.title = title;
        return this;
    }

    public MessageResponse setContent(String content) {
        this.content = content;
        return this;
    }

    public MessageResponse setNid(Integer nid) {
        this.nid = nid;
        return this;
    }

    public MessageResponse setFlags(Byte flags) {
        this.flags = flags;
        return this;
    }

    public MessageResponse setLargeIcon(String largeIcon) {
        this.largeIcon = largeIcon;
        return this;
    }

    public MessageResponse setTicker(String ticker) {
        this.ticker = ticker;
        return this;
    }

    public MessageResponse setNumber(Integer number) {
        this.number = number;
        return this;
    }

    public JSONObject getExtras() {
        return extras;
    }

    public MessageResponse setExtras(JSONObject extras) {
        this.extras = extras;
        return this;
    }

    @Override
    public String toString() {
        return "MessageResponse{" +
                "msgId='" + msgId + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", nid=" + nid +
                ", flags=" + flags +
                ", largeIcon='" + largeIcon + '\'' +
                ", ticker='" + ticker + '\'' +
                ", number=" + number +
                ", extras=" + extras +
                '}';
    }
}
