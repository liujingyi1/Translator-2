package com.letrans.android.translator.mpush;

import com.mpush.api.Client;

public class HttpClientListener {
    public void onConnected(Client client) {

    }

    public void onDisConnected(Client client) {

    }

    public void onHandshakeOk(Client client, int i) {

    }

    public void onReceivePush(ReceiveMessageBean messageBean) {

    }

    public void onKickUser(String s, String s1) {

    }

    public void onBind(boolean b, String s) {

    }

    public void onUnbind(boolean b, String s) {

    }

    public void onEnter(String language) {

    }

    public void onPaired(String fromDeviceId) {

    }

    public void onUnPaired() {

    }

    //type: 0：对方挂断  1：被远程挂断
    public void onHangUp(int type) {

    }

    //type: 0：对方挂断  1：被远程挂断
    public void onReceiveNotice(int type) {

    }

    public void chenlongjiekou(String content, int num1, int num2) {

    }

    public void chenlongjiekou2(byte[] content, int num1, int num2) {

    }
}
