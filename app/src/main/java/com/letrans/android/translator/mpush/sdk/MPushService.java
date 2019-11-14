/*
 * (C) Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     ohun@live.cn (夜色)
 */


package com.letrans.android.translator.mpush.sdk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.letrans.android.translator.BuildConfig;
import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.mpush.HttpClientListener;
import com.letrans.android.translator.mpush.MPushApi;
import com.letrans.android.translator.mpush.ReceiveMessageBean;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.mpush.api.Client;
import com.mpush.api.ClientListener;
import com.mpush.api.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by yxx on 2016/2/13.
 *
 * @author ohun@live.cn
 */
public final class MPushService extends Service implements ClientListener {
    private static final String TAG = "RTranslator/MPushService";

    public static final String ACTION_MESSAGE_RECEIVED = "com.mpush.MESSAGE_RECEIVED";
    public static final String ACTION_NOTIFICATION_OPENED = "com.mpush.NOTIFICATION_OPENED";
    public static final String ACTION_KICK_USER = "com.mpush.KICK_USER";
    public static final String ACTION_CONNECTIVITY_CHANGE = "com.mpush.CONNECTIVITY_CHANGE";
    public static final String ACTION_HANDSHAKE_OK = "com.mpush.HANDSHAKE_OK";
    public static final String ACTION_BIND_USER = "com.mpush.BIND_USER";
    public static final String ACTION_UNBIND_USER = "com.mpush.UNBIND_USER";
    public static final String EXTRA_PUSH_MESSAGE = "push_message";
    public static final String EXTRA_PUSH_MESSAGE_ID = "push_message_id";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_DEVICE_ID = "device_id";
    public static final String EXTRA_BIND_RET = "bind_ret";
    public static final String EXTRA_CONNECT_STATE = "connect_state";
    public static final String EXTRA_HEARTBEAT = "heartbeat";
    private int SERVICE_START_DELAYED = 1;

    private List<HttpClientListener> customClientListeners = new ArrayList<HttpClientListener>();
    PushServiceBinder mBinder = new PushServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.i(TAG, "PushService onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i(TAG, "PushService onCreate");
        cancelAutoStartService(this);
    }

    //add
    public class PushServiceBinder extends Binder {
        public MPushService getService() {
            Logger.i(TAG, "PushService getService");
            return MPushService.this;
        }
    }

    public void addHttpClientListener(HttpClientListener listener) {
        if (listener != null) {
            Logger.i(TAG, "addHttpClientListener start add");
            if (!customClientListeners.contains(listener)) {
                Logger.i(TAG, "addHttpClientListener added");
                customClientListeners.add(listener);
            }
        }
    }

    public void removeHttpClientListener(HttpClientListener listener) {
        if (listener != null) {
            Logger.i(TAG, "removeHttpClientListener listener=" + listener);
            if (customClientListeners.contains(listener)) {
                Logger.i(TAG, "remove listener=" + listener);
                customClientListeners.remove(listener);
            }
        }
    }

    public void removeAllHttpClientListener() {
        int count = customClientListeners.size();
        for (int i = count; i > 0; i--) {
            customClientListeners.remove(i - 1);
        }
    }
    //@

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i(TAG, "PushService onStartCommand");
        if (!MPush.I.hasStarted()) {
            MPush.I.checkInit(this).create(this);
        }
        if (MPush.I.hasStarted()) {
            if (MPushReceiver.hasNetwork(this)) {
                MPush.I.client.start();
            }
            MPushFakeService.startForeground(this);
            flags = START_STICKY;
            SERVICE_START_DELAYED = 1;
            return super.onStartCommand(intent, flags, startId);
        } else {
            int ret = super.onStartCommand(intent, flags, startId);
            stopSelf();
            SERVICE_START_DELAYED += SERVICE_START_DELAYED;
            return ret;
        }
    }

    /**
     * service停掉后自动启动应用
     *
     * @param context
     * @param delayed 延后启动的时间，单位为秒
     */
    private static void startServiceAfterClosed(Context context, int delayed) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayed * 1000, getOperation(context));
    }

    public static void cancelAutoStartService(Context context) {
        AlarmManager alarm = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(getOperation(context));
    }

    private static PendingIntent getOperation(Context context) {
        Intent intent = new Intent(context, MPushService.class);
        PendingIntent operation = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        return operation;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MPushReceiver.cancelAlarm(this);
        MPush.I.destroy();
        Logger.i(TAG, "PushService onDestroy");
        startServiceAfterClosed(this, SERVICE_START_DELAYED);//1s后重启
    }

    @Override
    public void onReceivePush(Client client, byte[] content, int messageId) {
        String message = new String(content, Constants.UTF_8);
        if (messageId > 0) MPush.I.ack(messageId);

        Logger.i(TAG, "onReceivePush=" + customClientListeners.size());

        if (customClientListeners.size() > 0) {
            try {
                ReceiveMessageBean receiveMessage = JSON.parseObject(message, ReceiveMessageBean.class);
                String messageContent = new String(receiveMessage.content, Constants.UTF_8);

                Logger.i(TAG, "onReceivePush receiveMessage=" + receiveMessage.toString()
                        + "\n messageContent=" + messageContent);

                if (receiveMessage.type == 999) {
                    for (HttpClientListener clientListener : customClientListeners) {
                        if (clientListener != null) {
                            String num = receiveMessage.fromLanguage;
                            String[] splitStr = num.split(",");
                            Integer num1 = Integer.parseInt(splitStr[0]);
                            Integer num2 = Integer.parseInt(splitStr[1]);
                            clientListener.chenlongjiekou2(receiveMessage.content, num1, num2);
                        }
                    }
                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_ENTER) {

                    String fromDeviceId = receiveMessage.from;
                    TStorageManager.getInstance().getMember(fromDeviceId).setLanguage(receiveMessage.fromLanguage);

                    for (HttpClientListener clientListener : customClientListeners) {
                        if (clientListener != null) {
                            clientListener.onEnter(messageContent);
                        }
                    }
                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_TEXT) {

                    SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss:SSS");
                    String date = format.format(new Date(System.currentTimeMillis()));
                    Logger.i(TAG, "date=" + date + "\ncontent=" + messageContent);

                    receiveMessage.isTranslated = false;

                    for (HttpClientListener clientListener : customClientListeners) {
                        Logger.i(TAG, "TYPE_TEXT clientListener=" + clientListener);
                        if (clientListener != null) {
                            Logger.i(TAG, "TYPE_TEXT clientListener=" + clientListener);
                            clientListener.onReceivePush(receiveMessage);
                        }
                    }
                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_TEXT_NOTRANSLATE) {

                    SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss:SSS");
                    String date = format.format(new Date(System.currentTimeMillis()));
                    Logger.i(TAG, "date=" + date + "\ncontent=" + messageContent);

                    receiveMessage.isTranslated = false;
                    receiveMessage.type = ReceiveMessageBean.TYPE_TEXT;

                    for (HttpClientListener clientListener : customClientListeners) {
                        Logger.i(TAG, "TYPE_TEXT_NOTRANSLATE clientListener=" + clientListener);
                        if (clientListener != null) {
                            Logger.i(TAG, "TYPE_TEXT_NOTRANSLATE clientListener=" + clientListener);
                            clientListener.onReceivePush(receiveMessage);
                        }
                    }

                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_TEXT_TRANSLATED) {

                    SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss:SSS");
                    String date = format.format(new Date(System.currentTimeMillis()));
                    Logger.i(TAG, "date=" + date + "\ncontent=" + messageContent);

                    receiveMessage.isTranslated = true;
                    receiveMessage.type = ReceiveMessageBean.TYPE_TEXT;

                    for (HttpClientListener clientListener : customClientListeners) {
                        Logger.i(TAG, "TYPE_TEXT_TRANSLATED clientListener=" + clientListener);
                        if (clientListener != null) {
                            Logger.i(TAG, "TYPE_TEXT_TRANSLATED clientListener=" + clientListener);
                            clientListener.onReceivePush(receiveMessage);
                        }
                    }

                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_PAIR) {
                    for (HttpClientListener clientListener : customClientListeners) {
                        if (clientListener != null) {
                            clientListener.onPaired(receiveMessage.from);
                        }
                    }
                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_RELIEVE_PAIR) {

                    TStorageManager.getInstance().setPairedId("");
                    MPushApi.get(TranslatorApp.getAppContext()).setPairUser("");

                    for (HttpClientListener clientListener : customClientListeners) {
                        if (clientListener != null) {
                            clientListener.onUnPaired();
                        }
                    }
                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_HUNGUP) {
                    for (HttpClientListener clientListener : customClientListeners) {
                        if (clientListener != null) {
                            Integer type = Integer.parseInt(messageContent);
                            clientListener.onHangUp(type);
                        }
                    }
                } else if (receiveMessage.type == ReceiveMessageBean.TYPE_NOTICE) {
                    ServerApi.getInstance().getNotice("letrans", messageContent);

                } else if (receiveMessage.type == 999) {
                    for (HttpClientListener clientListener : customClientListeners) {
                        if (clientListener != null) {
                            String num = receiveMessage.fromLanguage;
                            String[] splitStr = num.split(",");
                            Integer num1 = Integer.parseInt(splitStr[0]);
                            Integer num2 = Integer.parseInt(splitStr[1]);
                            clientListener.chenlongjiekou(messageContent, num1, num2);
                        }
                    }

                }
            } catch (Exception e) {
                Logger.i(TAG, "e=" + e.getMessage());
            }

        } else {
            sendBroadcast(new Intent(ACTION_MESSAGE_RECEIVED)
                    .addCategory(BuildConfig.APPLICATION_ID)
                    .putExtra(EXTRA_PUSH_MESSAGE, content)
                    .putExtra(EXTRA_PUSH_MESSAGE_ID, messageId)
            );
        }
    }

    @Override
    public void onKickUser(String deviceId, String userId) {
        Logger.i(TAG, "onKickUser");
        //可能会被踢掉，踢掉也不unbind.
        //MPush.I.unbindAccount();
        MPush.I.client.stop();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MPush.I.client.start();
        MPush.I.bindAccount(TStorageManager.getInstance().getDeviceId(), TStorageManager.getInstance().getDeviceId());
        // MPush.I.restartClient();

        if (customClientListeners.size() > 0) {
            for (HttpClientListener clientListener : customClientListeners) {
                if (clientListener != null) {
                    clientListener.onKickUser(deviceId, userId);
                }
            }
        } else {
            sendBroadcast(new Intent(ACTION_KICK_USER)
                    .addCategory(BuildConfig.APPLICATION_ID)
                    .putExtra(EXTRA_DEVICE_ID, deviceId)
                    .putExtra(EXTRA_USER_ID, userId)
            );
        }
    }

    @Override
    public void onBind(boolean success, String userId) {
        Logger.i(TAG, "onBind success=" + success + " userId=" + userId);
        MPush.I.bindAccount(TStorageManager.getInstance().getDeviceId(), TStorageManager.getInstance().getDeviceId());

        if (customClientListeners.size() > 0) {
            for (HttpClientListener clientListener : customClientListeners) {
                if (clientListener != null) {
                    clientListener.onBind(success, userId);
                }
            }
        } else {
            sendBroadcast(new Intent(ACTION_BIND_USER)
                    .addCategory(BuildConfig.APPLICATION_ID)
                    .putExtra(EXTRA_BIND_RET, success)
                    .putExtra(EXTRA_USER_ID, userId)
            );
        }
    }

    @Override
    public void onUnbind(boolean success, String userId) {
        Logger.i(TAG, "onUnbind");

        if (customClientListeners.size() > 0) {
            for (HttpClientListener clientListener : customClientListeners) {
                if (clientListener != null) {
                    clientListener.onUnbind(success, userId);
                }
            }
        } else {
            sendBroadcast(new Intent(ACTION_UNBIND_USER)
                    .addCategory(BuildConfig.APPLICATION_ID)
                    .putExtra(EXTRA_BIND_RET, success)
                    .putExtra(EXTRA_USER_ID, userId)
            );
        }
    }

    @Override
    public void onConnected(Client client) {
        Logger.i(TAG, "onConnected client=" + client);
        client.bindUser(TStorageManager.getInstance().getDeviceId(), TStorageManager.getInstance().getDeviceId());

        if (customClientListeners.size() > 0) {
            for (HttpClientListener clientListener : customClientListeners) {
                if (clientListener != null) {
                    clientListener.onConnected(client);
                }
            }
        } else {
            sendBroadcast(new Intent(ACTION_CONNECTIVITY_CHANGE)
                    .addCategory(BuildConfig.APPLICATION_ID)
                    .putExtra(EXTRA_CONNECT_STATE, true)
            );
        }
    }

    @Override
    public void onDisConnected(Client client) {
        Logger.i(TAG, "onDisConnected");
        MPushReceiver.cancelAlarm(this);

        if (customClientListeners.size() > 0) {
            for (HttpClientListener clientListener : customClientListeners) {
                if (clientListener != null) {
                    clientListener.onDisConnected(client);
                }
            }
        } else {
            sendBroadcast(new Intent(ACTION_CONNECTIVITY_CHANGE)
                    .addCategory(BuildConfig.APPLICATION_ID)
                    .putExtra(EXTRA_CONNECT_STATE, false)
            );
        }
    }

    @Override
    public void onHandshakeOk(Client client, int heartbeat) {
        Logger.i(TAG, "onHandshakeOk heartbeat=" + heartbeat);
        MPushReceiver.startAlarm(this, heartbeat / 2);

        if (customClientListeners.size() > 0) {
            for (HttpClientListener clientListener : customClientListeners) {
                if (clientListener != null) {
                    clientListener.onHandshakeOk(client, heartbeat);
                }
            }
        } else {
            sendBroadcast(new Intent(ACTION_HANDSHAKE_OK)
                    .addCategory(BuildConfig.APPLICATION_ID)
                    .putExtra(EXTRA_HEARTBEAT, heartbeat)
            );
        }
    }
}
