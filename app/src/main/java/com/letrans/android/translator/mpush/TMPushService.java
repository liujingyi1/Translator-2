package com.letrans.android.translator.mpush;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.R;
import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.mpush.bean.ServerPairInfoBean;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.mpush.sdk.MPush;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetBroadcastReceiver;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.utils.ToastUtils;
import com.letrans.android.translator.utils.Utils;
import com.mpush.api.Constants;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.functions.Consumer;

public class TMPushService extends Service implements IMPushApi, NetBroadcastReceiver.NetEvent {

    private static final String TAG = "RTranslator/TMPushService";

    private IMPushApi mPushApi;

    private HttpClientListener httpClientListener;

    private TCallback mTCallback;
    private static final int MSG_ON_NETCHANGE = 2;
    private boolean mHasNetwork;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Logger.i(TAG, "onBind");

        if (mPushApi == null) {
            mPushApi = MPushApi.get(TranslatorApp.getAppContext());
            mPushApi.startPush(TStorageManager.getInstance().getDeviceId());
        }

        return new TMPushBinder();
    }

    public class TMPushBinder extends Binder {
        public TMPushService getService() {
            return TMPushService.this;
        }
    }

    @Override
    public void onCreate() {

        httpClientListener = new HttpClientListener() {
            @Override
            public void onReceivePush(ReceiveMessageBean content) {
                Logger.v(TAG, "mPushApi - onReceivePush ServerTheradId:" + content.channel);
                Logger.v(TAG, "message language:" + content.fromLanguage);

                String mServerThreadId = !TextUtils.isEmpty(content.channel) ? content.channel : AppContext.LOCAL_SERVER_THREAD_ID;

                if (TStorageManager.getInstance().getThreadBean(mServerThreadId) != null) {
                    Logger.v(TAG, "has thread bean:" + mServerThreadId);
                    long mThreadId = TStorageManager.getInstance().getThreadBean(mServerThreadId).getId();
                    MessageBean tmpMessageBean = new MessageBean.Builder(mThreadId, TStorageManager
                            .getInstance()
                            .getMember(content.from).getId
                                    ()).setDate(Utils.getCurrentTime("yyyy-MM-dd HH:mm:ss")).setLanguage(content
                            .fromLanguage).setText(new String(content.content, Constants.UTF_8)).build();

                    if (mTCallback != null) {
                        Logger.v(TAG, "updateUIByMessageBean:" + mServerThreadId);
                        mTCallback.updateUIByMessageBean(tmpMessageBean, content.type, content.content);
                    }
                }
            }
        };

        timer.schedule(timerTask, 1000 * 10, 1000 * 10);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.i(TAG, "onStartCommand and init push API");

        if (mPushApi == null) {
            mPushApi = MPushApi.get(TranslatorApp.getAppContext());
            mPushApi.startPush(TStorageManager.getInstance().getDeviceId());
            String deviceId = TStorageManager.getInstance().getPairedId();
            mPushApi.setPairUser(deviceId);
            addHttpClientListener(httpClientListener);

            NetBroadcastReceiver.registEvent(this, this);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        Logger.i(TAG, "onDestroy");

        if (httpClientListener != null) {
            removeHttpClientListener(httpClientListener);
            httpClientListener = null;
        }

        mPushApi = null;
        NetBroadcastReceiver.unRegistEvent(this);
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {

        Logger.i(TAG, "onUnbind");

        return super.onUnbind(intent);
    }

    // 给一些需要反馈的界面设置个回调接口
    public interface TCallback {
        void updateUIByMessageBean(MessageBean messageBean, int type, byte[] content);
    }

    public void setTCallback(TCallback callback) {
        this.mTCallback = callback;
    }

    // 这个服务必须实现IMPushApi所有方法才能完美衔接，虽然很多接口目前还用不上
    @Override
    public void bindUser(String userId) {
        mPushApi.bindUser(userId);
    }

    @Override
    public void startPush(String userId) {
        mPushApi.startPush(userId);
    }

    @Override
    public void sendPush(MessageBean text) {
        mPushApi.sendPush(text);
    }

    @Override
    public void stopPush() {
        mPushApi.stopPush();
    }

    @Override
    public void pausePush() {
        mPushApi.pausePush();
    }

    @Override
    public void resumePush() {
        mPushApi.resumePush();
    }

    @Override
    public void unbindUser() {
        mPushApi.unbindUser();
    }

    @Override
    public void setPairUser(String id) {
        mPushApi.setPairUser(id);
    }

    @Override
    public String getPairUser() {
        return mPushApi.getPairUser();
    }

    @Override
    public void setHttpCallBack(HttpProxyCallback httpCallback) {
        mPushApi.setHttpCallBack(httpCallback);
    }

    @Override
    public void addHttpClientListener(HttpClientListener httpClientListener) {
        Logger.i(TAG, "addHttpClientListener:" + httpClientListener);
        mPushApi.addHttpClientListener(httpClientListener);
    }

    @Override
    public void removeHttpClientListener(HttpClientListener httpClientListener) {
        mPushApi.removeHttpClientListener(httpClientListener);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_NETCHANGE: {
                    doNetChangeWork();
                    break;
                }

                case CHECK_PUSH_HELTH: {
                    if (NetUtil.isNetworkConnected(getApplicationContext())) {
                        Logger.i(TAG, "CHECK_PUSH_HELTH hasStarted()=" + MPush.I.hasStarted()
                                + " hasRunning=" + MPush.I.hasRunning()
                                +" retryTimes="+retryTimes);
                        if (MPush.I.hasStarted()) {
                            if (!MPush.I.hasRunning()) {
                                retryTimes++;
                                if (retryTimes > 2) {
                                    ToastUtils.showLong(R.string.connect_server_toast);
                                }
                                if (retryTimes > 5) {
                                    mPushApi.stopPush();
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mPushApi.startPush(TStorageManager.getInstance().getDeviceId());
                                    retryTimes = 1;
                                }
                            } else {
                                retryTimes = 0;
                            }
                        } else {
                            startPush(TStorageManager.getInstance().getDeviceId());
                        }
                    }
                    break;
                }
            }
        }
    };

    private void doNetChangeWork() {
        if (mHasNetwork) {

            if (!ServerApi.getInstance().getIsRegisted()) {
                ServerApi.getInstance()
                        .registClient(null);
            }

            if (ServerApi.getInstance().isNeedUpdateRole()) {
                ServerApi.getInstance()
                        .changeRole(TStorageManager.getInstance().getDeviceId()
                                , UserBean.getUser().getRole()
                                , UserBean.getUser().getLanguage(), null);
            }

            ServerApi.getInstance()
                    .getPairInfo(TStorageManager.getInstance().getDeviceId())
                    .subscribe(new Consumer<ServerPairInfoBean>() {
                        @Override
                        public void accept(ServerPairInfoBean serverPairInfoBean) throws Exception {
                            if (serverPairInfoBean != null && serverPairInfoBean.code == 1) {

                                if (serverPairInfoBean.result.size() <= 0) {
                                    TStorageManager.getInstance().setPairedId("");
                                    mPushApi.setPairUser("");
                                } else {
                                    String device = TStorageManager.getInstance().getDeviceId();
                                    String pairDeviceId;
                                    if (device.compareTo(serverPairInfoBean.result.get(0).deviceIdW) == 0) {
                                        pairDeviceId = serverPairInfoBean.result.get(0).deviceIdH;
                                    } else {
                                        pairDeviceId = serverPairInfoBean.result.get(0).deviceIdW;
                                    }
                                    TStorageManager.getInstance().setPairedId(pairDeviceId);
                                    if (TStorageManager.getInstance().getMember(pairDeviceId) == null) {
                                        TStorageManager.getInstance().createMember(pairDeviceId, "");
                                    }
                                    mPushApi.setPairUser(pairDeviceId);
                                }
                            }

                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {

                        }
                    });

            ServerApi.getInstance().clearInvalidCode();
        }
    }

    @Override
    public void onNetChange(boolean hasNetwork) {
        this.mHasNetwork = hasNetwork;
        handler.sendEmptyMessageDelayed(MSG_ON_NETCHANGE, 300);
    }

    private static final int CHECK_PUSH_HELTH = 110;
    private int retryTimes = 0;
    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            handler.sendEmptyMessage(CHECK_PUSH_HELTH);
        }
    };
}
