package com.letrans.android.translator.mpush;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import com.letrans.android.translator.TranslatorApp;
import com.mpush.api.http.HttpCallback;
import com.mpush.api.http.HttpResponse;
import com.mpush.client.ClientConfig;
import com.letrans.android.translator.BuildConfig;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.mpush.domain.RxSchedulers;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.mpush.domain.ServerApiService;
import com.letrans.android.translator.mpush.domain.ServerResponse;
import com.letrans.android.translator.mpush.sdk.MPush;
import com.letrans.android.translator.mpush.sdk.MPushLog;
import com.letrans.android.translator.mpush.sdk.MPushReceiver;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class MPushApiProxy implements IMPushApi {
    private static final String TAG = "RTranslator/MPushApiProxy";

    private static String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCghPCWCobG8nTD24juwSVataW7iViRxcTkey/B792VZEhuHjQvA3cAJgx2Lv8GnX8NIoShZtoCg3Cx6ecs+VEPD2fBcg2L4JK7xldGpOJ3ONEAyVsLOttXZtNXvyDZRijiErQALMTorcgi79M5uVX9/jMv2Ggb2XAeZhlLD28fHwIDAQAB";
    public static String SERVER_IP = "http://116.62.168.13:9999";
    private static String DEVAULT_SEND_TYPE = "2";
    private static String DEVAULT_SEND_TYPE_NOTRANSLATED = "20";
    private static String DEVAULT_SEND_TYPE_TRANSLATED = "21";

    private static String SEND_MESSAGE_KEY_TYPE = "type";
    private static String SEND_MESSAGE_KEY_CONTENT = "content";
    private static String SEND_MESSAGE_KEY_FROM = "from";
    private static String SEND_MESSAGE_KEY_FROMDEVICEID = "fromDeviceId";
    private static String SEND_MESSAGE_KEY_TODEVICEID = "toDeviceId";
    private static String SEND_MESSAGE_KEY_BROADCAST = "broadcast";

    private String mUserId;
    private String mPairUserId;

    private HttpProxyCallback httpProxyCallback;

    ServerApi mServerApi;

    private ServerApi.ServerApiListener serverApiListener = new ServerApi.ServerApiListener() {
        @Override
        public void onError(int errorCode) {

        }
    };

    public MPushApiProxy(Context context) {
        mServerApi = ServerApi.getInstance();
        mServerApi.setServerApiListener(serverApiListener);

        MPushReceiver netWorkChangReceiver = new MPushReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(netWorkChangReceiver, filter);
    }

    public void setPairUser(String id) {
        mPairUserId = id;
    }

    public String getPairUser() {
        return mPairUserId;
    }

    @Override
    public void bindUser(String userId) {
        if (!TextUtils.isEmpty(userId)) {
            mUserId = userId;
            MPush.I.bindAccount(mUserId, mUserId);//"mpush:" + (int) (Math.random() * 10)
        }
    }

    private void initPush(String allocServer, String userId) {
        //公钥有服务端提供和私钥对应

        MPushLog pushLog = new MPushLog();
        pushLog.enable(true);

        ClientConfig cc = ClientConfig.build()
                .setPublicKey(PUBLIC_KEY)
                .setAllotServer(allocServer)
                .setDeviceId(TStorageManager.getInstance().getDeviceId())
                .setClientVersion(BuildConfig.VERSION_NAME)
                .setLogger(pushLog)
                .setLogEnabled(BuildConfig.DEBUG)
                .setEnableHttpProxy(true)
                .setUserId(TStorageManager.getInstance().getDeviceId())
                .setTags(TStorageManager.getInstance().getDeviceId())
                .setMaxHeartbeat(90000)
                .setMinHeartbeat(90000);
        MPush.I.checkInit(TranslatorApp.getAppContext()).setClientConfig(cc);
    }

    @Override
    public void startPush(final String userId) {
        mUserId = userId;
        initPush(SERVER_IP, mUserId);

        MPush.I.checkInit(TranslatorApp.getAppContext()).startPush();

        if (!mServerApi.getIsRegisted()) {
            mServerApi.registClient(null);
        }
    }

    @Override
    public void sendPush(final MessageBean messageBean) {
        if (messageBean == null) return;

            final HashMap<String, String> sendParams = new HashMap();
            if (messageBean.getIsTranslated()) {
                sendParams.put(SEND_MESSAGE_KEY_TYPE, DEVAULT_SEND_TYPE_TRANSLATED);
            } else {
                sendParams.put(SEND_MESSAGE_KEY_TYPE, DEVAULT_SEND_TYPE_NOTRANSLATED);
            }

            sendParams.put(SEND_MESSAGE_KEY_CONTENT, messageBean.getText());
            sendParams.put(SEND_MESSAGE_KEY_FROM, messageBean.getLanguage());
            sendParams.put(SEND_MESSAGE_KEY_FROMDEVICEID, mUserId);
            sendParams.put(SEND_MESSAGE_KEY_TODEVICEID, mPairUserId);
            sendParams.put(SEND_MESSAGE_KEY_BROADCAST, "false");

            SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss:SSS");
            String date = format.format(new Date(System.currentTimeMillis()));
            Logger.i(TAG, "sendMessage date=" + date);
            Logger.i(TAG, "sendMessage mUserId=" + mUserId);

            ServerApi.apiService(ServerApiService.class)
                    .sendMessage(mServerApi.getToken(), sendParams)
                    .observeOn(Schedulers.io())
                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                        @Override
                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                            if (serverResponse.code == ServerApi.SR_TOKEN_INVALID_CODE) {
                                return ServerApi.apiService(ServerApiService.class)
                                        .getToken(TStorageManager.getInstance().getDeviceId())
                                        .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                            @Override
                                            public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                                if (serverResponse.code == 1) {
                                                    mServerApi.setToken(serverResponse.result);

                                                    return ServerApi.apiService(ServerApiService.class)
                                                            .sendMessage(serverResponse.result, sendParams);
                                                }
                                                return Observable.just(serverResponse);
                                            }
                                        });
                            }
                            return Observable.just(serverResponse);
                        }
                    })
                    .compose(RxSchedulers.<ServerResponse>io_main())
                    .subscribe(new Consumer<ServerResponse>() {
                        @Override
                        public void accept(ServerResponse serverResponse) throws Exception {
                            if (serverResponse.code != 1) {
                                processError(serverResponse.code);
                            }
                            if (httpProxyCallback != null) {
                                httpProxyCallback.onResponse(serverResponse);
                            }
                            Logger.i(TAG, "serverResponse =" + serverResponse.toString());

                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Logger.i(TAG, "Send Message error throwable =" + throwable.getMessage());
                            if (httpProxyCallback != null) {
                                httpProxyCallback.onCancelled();
                            }

                        }
                    });
    }

    private void processError(int code) {

    }

    HttpCallback mHttpCallback = new HttpCallback() {
        @Override
        public void onResponse(HttpResponse httpResponse) {
            Logger.i(TAG, "httpResponse=" + httpResponse.toString());
        }

        @Override
        public void onCancelled() {
            if (httpProxyCallback != null) {
                httpProxyCallback.onCancelled();
            }
        }
    };

    @Override
    public void stopPush() {
        MPush.I.stopPush();
    }

    @Override
    public void pausePush() {
        MPush.I.pausePush();
    }

    @Override
    public void resumePush() {
        MPush.I.resumePush();
    }

    @Override
    public void unbindUser() {
        MPush.I.unbindAccount();
    }

    @Override
    public void setHttpCallBack(HttpProxyCallback httpCallBack) {
        this.httpProxyCallback = httpCallBack;
    }

    @Override
    public void addHttpClientListener(HttpClientListener httpClientListener) {
        MPush.I.setHttpClientListener(httpClientListener);
    }

    @Override
    public void removeHttpClientListener(HttpClientListener httpClientListener) {
        MPush.I.removeHttpClientListener(httpClientListener);
    }
}
