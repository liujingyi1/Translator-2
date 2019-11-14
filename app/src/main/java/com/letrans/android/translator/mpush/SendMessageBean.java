package com.letrans.android.translator.mpush;

import java.io.Serializable;

/*
int	type	消息类型1：语音文件2：字符串
File	file	type=1，文件流
string	content	type=2，字符串内容
String	from	源语种，必须
String	to	目标语种，必须
String	fromDeviceId	源设备识别码
string	toDeviceId	目标设备识别码，broadcast=false
boolean 	broadcast	是否群发（频道内）
string	channelNum	频道号，broadcast=true

 int type; 消息类型
 1：语音 2：文字  3：配对   4：解除配对  5：公告   6：挂断

 string msgId;消息ID
 long from;源DeviceId
 long channel;频道，为空说明是P2P
 byte[] content;消息内容

result: {"code":1,"result":null,message:"success"}
 */


public class SendMessageBean implements Serializable {
    int type;
    String content;
    String from;
    String fromDeviceId;
    String toDeviceId;
    boolean broadcast;
    String channelNum;

    public SendMessageBean setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
        return this;
    }

    public SendMessageBean setChannelNum(String channelNum) {
        this.channelNum = channelNum;
        return this;
    }

    public SendMessageBean setContent(String content) {
        this.content = content;
        return this;
    }

    public SendMessageBean setFrom(String from) {
        this.from = from;
        return this;
    }

    public SendMessageBean setFromDeviceId(String fromDeviceId) {
        this.fromDeviceId = fromDeviceId;
        return this;
    }

    public SendMessageBean setToDeviceId(String toDeviceId) {
        this.toDeviceId = toDeviceId;
        return this;
    }

    public SendMessageBean setType(int type) {
        this.type = type;
        return this;
    }
}
