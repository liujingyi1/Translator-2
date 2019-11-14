package com.letrans.android.translator.mpush;

import com.letrans.android.translator.database.beans.MessageBean;

public interface IMPushApi {
    public void bindUser(String userId);

    public void startPush(String userId);

    public void sendPush(MessageBean text);

    public void stopPush();

    public void pausePush();

    public void resumePush();

    public void unbindUser();

    public void setPairUser(String id);

    public String getPairUser();

    public void setHttpCallBack(HttpProxyCallback httpCallback);

    public void addHttpClientListener(HttpClientListener httpClientListener);

    public void removeHttpClientListener(HttpClientListener httpClientListener);
}
