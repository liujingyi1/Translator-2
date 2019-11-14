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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.letrans.android.translator.utils.Logger;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

/**
 * Created by yxx on 2016/2/14.
 *
 * @author ohun@live.cn
 */
public final class MPushReceiver extends BroadcastReceiver {
    private static final String TAG = "RTranslator/MPushReceiver";

    public static final String ACTION_HEALTH_CHECK = "com.mpush.HEALTH_CHECK";
    public static final String ACTION_NOTIFY_CANCEL = "com.mpush.NOTIFY_CANCEL";
    // public static int delay = Constants.DEF_HEARTBEAT;
    public static int delay = 100000; //180000
    public static State STATE = State.UNKNOWN;
    private final int TRY_CHECK = 233;
    private Context mContext;
    private int TRY_COUNT_LIMIT = 6;
    private int tryMaxCount = TRY_COUNT_LIMIT;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TRY_CHECK: {
                    if (MPush.I.client.healthCheck()) {
                        startAlarm(mContext, delay);
                        tryMaxCount = TRY_COUNT_LIMIT;
                    } else {
                        if (tryMaxCount > 0) {
                            handler.sendEmptyMessageDelayed(TRY_CHECK, 500);
                            tryMaxCount--;
                        } else {
                            MPush.I.checkInit(mContext).startPush();
                            if (MPush.I.hasStarted()) {
                                if (hasNetwork(mContext)) {
                                    MPush.I.client.start();
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        if (ACTION_HEALTH_CHECK.equals(action)) {//处理心跳
            Logger.i(TAG, "ACTION_HEALTH_CHECK MPush.hasStarted=" + MPush.I.hasStarted());
            if (MPush.I.hasStarted()) {
                Logger.i(TAG, "ACTION_HEALTH_CHECK MPush.isRunning=" + MPush.I.client.isRunning());
                if (MPush.I.client.isRunning()) {
                    if (MPush.I.client.healthCheck()) {
                        Logger.i(TAG, "ACTION_HEALTH_CHECK MPush.healthCheck success");
                        tryMaxCount = TRY_COUNT_LIMIT;
                        startAlarm(context, delay);
                    } else {
                        handler.sendEmptyMessageDelayed(TRY_CHECK, 500);
                    }
                } else {
                    MPush.I.client.start();
                }
            } else {
//                MPush.I.checkInit(context).startPush();
//                if (MPush.I.hasStarted()) {
//                    if (MPushReceiver.hasNetwork(context)) {
//                        MPush.I.client.start();
//                    }
//                }
            }
        } else if (CONNECTIVITY_ACTION.equals(action)) {//处理网络变化
            if (hasNetwork(context)) {
                if (STATE != State.CONNECTED) {
                    STATE = State.CONNECTED;
                    Logger.i(TAG, "MPush.I.hasStarted()=" + MPush.I.hasStarted());
                    if (MPush.I.hasStarted()) {
                        MPush.I.onNetStateChange(true);

                        MPush.I.resumePush();
                    } else {
                        MPush.I.checkInit(context).startPush();
                    }
                }
            } else {
                if (STATE != State.DISCONNECTED) {
                    STATE = State.DISCONNECTED;
                    MPush.I.onNetStateChange(false);

                    MPush.I.pausePush();
                    cancelAlarm(context);//防止特殊场景下alarm没被取消
                }
            }
        } else if (ACTION_NOTIFY_CANCEL.equals(action)) {//处理通知取消
//            Notifications.I.clean(intent);
        }
    }

    static void startAlarm(Context context, int delay) {
        Intent it = new Intent(MPushReceiver.ACTION_HEALTH_CHECK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, it, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, pi);
        MPushReceiver.delay = delay;
    }

    static void cancelAlarm(Context context) {
        Intent it = new Intent(MPushReceiver.ACTION_HEALTH_CHECK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, it, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    public static boolean hasNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }
}
