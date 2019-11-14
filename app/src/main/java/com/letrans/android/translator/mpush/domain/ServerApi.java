package com.letrans.android.translator.mpush.domain;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.database.DbConstants;
import com.letrans.android.translator.database.TranslatorProvider;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.mpush.bean.ServerListResponse;
import com.letrans.android.translator.mpush.bean.ServerPairInfoBean;
import com.letrans.android.translator.mpush.bean.ServerRegistBean;
import com.letrans.android.translator.roobo.RooboManager;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ServerApi {
    private static String TAG = "RTranslator/ServerApi";

    private static ServerApi mInstance = null;
    private ServerApiManager mApiManager = null;

    private static final String SP_PUSH_FILE_NAME = "pushapi.config";
    private static final String SP_KEY_TOKEN = "token1";
    private static final String SP_KEY_SERVER_IP = "serverIp";
    private static final String SP_KEY_IS_REGISTED = "registed1";
    private static final String SP_KEY_NEED_UPDATE_ROLE = "needUpdateRole";
    private static final String SP_KEY_AS = "allotServer";
    private static final String SP_KEY_AT = "account";
    private static final String SP_KEY_TG = "tags";

    public static final int SR_REQUEST_SUCCESS_CODE = 1; //成功
    public static final int SR_ERROR_CODE_UNKNOWN = 0; //未知错误
    public static final int SR_SHORT_PRAGRAM_CODE = 1001; //缺少参数
    public static final int SR_SEND_MESSAGE_ERROR_CODE = 1002; //发送消息失败
    public static final int SR_CLIENT_NOT_ONLINE_CODE = 1003; // 对方未在线
    public static final int SR_SEND_TIMEOUT_CODE = 1004; //发送消息超时
    public static final int SR_CHANNEL_COUNT_LIMMIT_CODE = 1005; //达到频道数上限
    public static final int SR_ALREADY_IN_CHANNEL_CODE = 1006; //已经加入频道
    public static final int SR_QUITE_CHANNEL_ERROR_CODE = 1007; //退出频道失败
    public static final int SR_DEVICE_ALREADY_REGIST_CODE = 1008; //该设备已经注册
    public static final int SR_REGIST_ERROR_CODE = 1009; //注册失败
    public static final int SR_PAIR_CODE_INVALID_CODE = 1010; //配对码已经失效
    public static final int SR_PAIR_ERROR_CODE = 1011; //配对失败
    public static final int SR_ALREADY_PAIRED_CODE = 1012; //已经配对
    public static final int SR_GET_PAIRCODE_FAIL = 1013; //获取配对码失败
    public static final int SR_CHANGE_LANGUAGE_ERROR_CODE = 1020; //更新语言失败
    public static final int SR_CHANGE_ROLE_ERROR_CODE = 1021; //更改角色失败
    public static final int SR_NO_PAIR_INFO_CODE = 1022; //没有配对信息
    public static final int SR_HUNG_UP_ERROR_CODE = 1023; //挂断失败
    public static final int SR_TOKEN_INVALID_CODE = 1040; //TOKEN失效

    private static final int SUCCESS_CODE = 1;
    public static final int CODE_INVALID_SECOND = 200;
    String  mToken = null;
    String  mServerIp = null;

    ServerApiListener mServerApiListener;
    public void setServerApiListener(ServerApiListener serverApiListener) {
        this.mServerApiListener = serverApiListener;
    }

    public interface GetTokenCallback{
        public void call(int code);
    }

    public interface ServerApiListener {
        public void onError(int errorCode);
    }

    public interface ServerCallback {
        public void call(String result, int code);
    }

    private ServerApi() {
        mApiManager = new ServerApiManager();
    }

    public static <T> T apiService(Class<T> clz) {
        return getInstance().mApiManager.getService(clz);
    }

    public static ServerApi getInstance() {
            if (mInstance == null) {
                mInstance = new ServerApi();
            }
            return mInstance;
    }

    public <T> void addApiService(Class<T> clz) {
        getInstance().mApiManager.addService(clz);
    }

//    public String getToken(final OnGetTokenListener tokenListener, boolean needRefresh) {
//        if (TextUtils.isEmpty(mToken) && !needRefresh) {
    public String getToken() {
        if (TextUtils.isEmpty(mToken)) {
            SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
            mToken = sp.getString(SP_KEY_TOKEN, "");
        }
        return mToken;
    }

    public void setToken(String token) {
        mToken = token;
        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SP_KEY_TOKEN, mToken);
        editor.commit();
    }

    public String getServerIp() {
        if (TextUtils.isEmpty(mServerIp)) {
            SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
            mServerIp = sp.getString(SP_KEY_SERVER_IP, "");
        }
        return mServerIp;
    }

    public void setServerIp(String ip) {
        mServerIp = ip;

        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SP_KEY_SERVER_IP, mServerIp);
        editor.commit();
    }

    public boolean getIsRegisted() {
        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
        boolean isRegist = sp.getBoolean(SP_KEY_IS_REGISTED, false);

        String deviceId = TStorageManager.getInstance().getDeviceId();
        String dbDeviceId = TStorageManager.getInstance().getUser().getDeviceId();

        return (isRegist && deviceId.equals(dbDeviceId));
    }

    public void setIsRegisted(Boolean is) {
        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(SP_KEY_IS_REGISTED, is);
        editor.commit();
    }

    public boolean isNeedUpdateRole() {
        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(SP_KEY_NEED_UPDATE_ROLE, true);
    }

    public void setNeedUpdateRole(Boolean is) {
        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(SP_KEY_NEED_UPDATE_ROLE, is).commit();
    }

    private void getTokenObservable(final GetTokenCallback callback) {
        
        ServerApi.apiService(ServerApiService.class)
                .getToken(TStorageManager.getInstance().getDeviceId())
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        mToken = serverResponse.result;
                        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(SP_KEY_TOKEN, mToken);
                        editor.commit();

                        callback.call(serverResponse.code);
                        return null;
                    }
                });
    }

    private Observable<ServerListResponse> getTokenObservable(final Observable<ServerListResponse> observable, int a) {
        return ServerApi.apiService(ServerApiService.class)
                .getToken(TStorageManager.getInstance().getDeviceId())
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerListResponse>>() {
                    @Override
                    public ObservableSource<ServerListResponse> apply(ServerResponse serverResponse) throws Exception {
                        mToken = serverResponse.result;
                        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(SP_KEY_TOKEN, mToken);
                        editor.commit();
                        return observable;
                    }
                });
    }

    private Observable<ServerListResponse> getServerIpObservable() {
        return ServerApi.apiService(ServerApiService.class)
                .getServerList(getToken())
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerListResponse, ObservableSource<ServerListResponse>>() {
                    @Override
                    public ObservableSource<ServerListResponse> apply(ServerListResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerListResponse>>() {
                                        @Override
                                        public ObservableSource<ServerListResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .getServerList(mToken);
                                            } else {
                                                return null;
                                            }
                                        }
                                    });
                        } else if (serverResponse.code == 1) {
                            int count = serverResponse.result.size();
                            Random random = new Random();
                            int randomIndex = random.nextInt(count);
                            ServerListResponse.ServerIpBean serverIpBean = serverResponse.result.get(randomIndex);
                            mServerIp = serverIpBean.attrs.public_ip+":"+serverIpBean.port;

                            SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(SP_PUSH_FILE_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString(SP_KEY_SERVER_IP, mServerIp);
                            editor.commit();
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<ServerListResponse>io_main());
    }

    public void registClient(final ServerCallback callback) {
        ServerApi.apiService(ServerApiService.class)
                .registClient(getToken(), TStorageManager.getInstance().getDeviceId())
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerRegistBean, ObservableSource<ServerRegistBean>>() {
                    @Override
                    public ObservableSource<ServerRegistBean> apply(ServerRegistBean serverResponse) throws Exception {
                        Log.i(TAG, "RegistClient flatMap serverResponse="+serverResponse.code);
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .subscribeOn(Schedulers.io())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerRegistBean>>() {
                                        @Override
                                        public ObservableSource<ServerRegistBean> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .registClient(mToken, TStorageManager.getInstance().getDeviceId());
                                            } else {
                                                return null;
                                            }
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<ServerRegistBean>io_main())
                .subscribe(new Consumer<ServerRegistBean>() {
                    @Override
                    public void accept(ServerRegistBean serverResponse) throws Exception {
                        if (serverResponse != null
                                && (serverResponse.code == 1
                                || serverResponse.code == 1008)) {
                            setIsRegisted(true);
                            String deviceId = TStorageManager.getInstance().getDeviceId();
                            UserBean userBean = TStorageManager.getInstance().getUser();
                            userBean.setDeviceId(deviceId);
                            userBean.update();

                            ServerApi.getInstance()
                                    .changeRole(TStorageManager.getInstance().getDeviceId()
                                            , UserBean.getUser().getRole()
                                            , UserBean.getUser().getLanguage(), null);

                            Utils.setData(serverResponse.timestamp);

                            if (callback != null) {
                                callback.call("", serverResponse.code);
                            }
                            RooboManager.getInstance().initRoobo(null);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(TAG, "RegistClient throwable="+throwable.getMessage());
                    }
                });
    }

    public Observable<ServerResponse> pairClient(final String fromDeviceId, final String pairCode) {
        return ServerApi.apiService(ServerApiService.class)
                .pairClient(getToken(), fromDeviceId, pairCode)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {

                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .pairClient(getToken(), fromDeviceId, pairCode);
                                            }
                                            return  Observable.just(serverResponse);
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<ServerResponse>io_main());
    }

    public Observable<ServerResponse> getPairCode(final String fromDeviceId) {
        return  ServerApi.apiService(ServerApiService.class)
                .getPairCode(getToken(), fromDeviceId)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {

                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .getPairCode(getToken(), fromDeviceId);
                                            }
                                            return  Observable.just(serverResponse);
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse != null && serverResponse.code==1) {
                            String code = serverResponse.result;
                            if (!TextUtils.isEmpty(code)) {
                                SQLiteDatabase sqLiteDatabase = TranslatorProvider.getInstance().getDatabaseHelper().getWritableDatabase();
                                ContentValues values = new ContentValues();
                                values.put(DbConstants.CodeColumns.PAIR_CODE, code);
                                values.put(DbConstants.CodeColumns.TIME, System.currentTimeMillis());
                                sqLiteDatabase.insert(DbConstants.Tables.TABLE_PAIR_CODE,
                                DbConstants.CodeColumns.PAIR_CODE, values);
                            }
                        }
                        return  Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<ServerResponse>io_main());
    }

    public Observable<ServerResponse> invalidPairCode(final String fromDeviceId, final String code) {
        return ServerApi.apiService(ServerApiService.class)
                .invalidPairCode(getToken(), fromDeviceId, code)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {

                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .invalidPairCode(getToken(), fromDeviceId, code);
                                            }
                                            return  Observable.just(serverResponse);
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse != null && serverResponse.code==1) {
                            if (!TextUtils.isEmpty(code)) {
                                SQLiteDatabase sqLiteDatabase = TranslatorProvider.getInstance().getDatabaseHelper().getWritableDatabase();
                                sqLiteDatabase.delete(DbConstants.Tables.TABLE_PAIR_CODE,
                                        DbConstants. CodeColumns.PAIR_CODE+ "="+code,null);
                            }
                        }
                        return  Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<ServerResponse>io_main());
    }

    public void clearInvalidCode() {
        final SQLiteDatabase sqLiteDatabase = TranslatorProvider.getInstance().getDatabaseHelper().getWritableDatabase();

        String sql = "SELECT * FROM "+DbConstants.Tables.TABLE_PAIR_CODE;
        Cursor cursor = sqLiteDatabase.rawQuery(sql, null);

        try {
            if (cursor != null && cursor.getCount() > 0) {
                long current = System.currentTimeMillis();
                while (cursor.moveToNext()) {
                    long time = cursor.getLong(cursor.getColumnIndex(DbConstants.CodeColumns.TIME));
                    final int id = cursor.getInt(cursor.getColumnIndex(DbConstants.CodeColumns.ID));
                    String pairCode = cursor.getString(cursor.getColumnIndex(DbConstants.CodeColumns.PAIR_CODE));

                    if ((current - time) > CODE_INVALID_SECOND * 1000) {
                        invalidPairCode(TStorageManager.getInstance().getDeviceId(), pairCode)
                                .subscribe(new Consumer<ServerResponse>() {
                                    @Override
                                    public void accept(ServerResponse serverResponse) throws Exception {
                                        if (serverResponse.code == SR_REQUEST_SUCCESS_CODE) {
                                            sqLiteDatabase.delete(DbConstants.Tables.TABLE_PAIR_CODE, DbConstants.CodeColumns.ID + "=" + id, null);
                                        }
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {

                                    }
                                });
                    }
                }
            }
        }  finally {
            cursor.close();
        }
    }

    public Observable<ServerResponse> relievePair(final String fromDeviceId, final String toDeviceId) {
        return ServerApi.apiService(ServerApiService.class)
                .relievePair(getToken(), fromDeviceId, toDeviceId)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {

                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .relievePair(getToken(), fromDeviceId, toDeviceId);
                                            }
                                            return  Observable.just(serverResponse);
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<ServerResponse>io_main());
    }

    public Observable<ServerPairInfoBean> getPairInfo(final String deviceId) {
        return ServerApi.apiService(ServerApiService.class)
                .getPairInfo(getToken(), deviceId)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerPairInfoBean, ObservableSource<ServerPairInfoBean>>() {
                    @Override
                    public ObservableSource<ServerPairInfoBean> apply(ServerPairInfoBean serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {

                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerPairInfoBean>>() {
                                        @Override
                                        public ObservableSource<ServerPairInfoBean> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .getPairInfo(getToken(), deviceId);
                                            }
                                            return null;
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<ServerPairInfoBean>io_main());
    }

    public void hangUp(final String fromeDeviceInfo, final String toDeviceId, final int type, final ServerCallback callback) {
         ServerApi.apiService(ServerApiService.class)
                .hangUp(getToken(),fromeDeviceInfo,toDeviceId, type)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {

                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .hangUp(getToken(),fromeDeviceInfo,toDeviceId, type);
                                            }
                                            return  Observable.just(serverResponse);
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
                         if (serverResponse != null) {
                             if (callback != null) {
                                 callback.call(serverResponse.result, serverResponse.code);
                             }
                         } else {
                             if (callback != null) {
                                 callback.call("", -1);
                             }
                         }
                     }
                 }, new Consumer<Throwable>() {
                     @Override
                     public void accept(Throwable throwable) throws Exception {
                         Log.i(TAG, "hangUp exception throwable="+throwable.getMessage());
                         if (callback != null) {
                             callback.call("", -1);
                         }
                     }
                 });
    }

    public void enterCompose(final String fromeDeviceId, final String language, final ServerCallback serverCallback) {
        ServerApi.apiService(ServerApiService.class)
                .enterCompose(getToken(), fromeDeviceId, language)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .enterCompose(getToken(), fromeDeviceId, language);
                                            }
                                            return  Observable.just(serverResponse);
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
                        if (serverResponse != null) {
                            if (serverCallback != null) {
                                serverCallback.call(serverResponse.result, serverResponse.code);
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (serverCallback != null) {
                            serverCallback.call("", -1);
                        }
                    }
                });
    }

    //设备角色：1：客服   2：访客
    public void changeRole(final String deviceId, final int role, final String language, final ServerCallback callback) {
        ServerApi.apiService(ServerApiService.class)
                .changeRole(getToken(), deviceId, role, language)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .changeRole(getToken(), deviceId, role, language);
                                            }
                                            return  Observable.just(serverResponse);
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
                        if (serverResponse != null) {
                            if (callback != null) {
                                callback.call(serverResponse.result, serverResponse.code);
                            }
                            if (serverResponse.code == SR_REQUEST_SUCCESS_CODE) {
                                setNeedUpdateRole(false);
                                return;
                            }
                        }
                        setNeedUpdateRole(true);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        setNeedUpdateRole(true);
                    }
                });
    }

    public void getRoleLanguage(final String deviceId, final ServerCallback callback) {
        ServerApi.apiService(ServerApiService.class)
                .getRoleLanguage(getToken(), deviceId)
                .observeOn(Schedulers.io())
                .flatMap(new Function<RoleInfoResponse, ObservableSource<RoleInfoResponse>>() {
                    @Override
                    public ObservableSource<RoleInfoResponse> apply(RoleInfoResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<RoleInfoResponse>>() {
                                        @Override
                                        public ObservableSource<RoleInfoResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .getRoleLanguage(getToken(), deviceId);
                                            }
                                            return  null;
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<RoleInfoResponse>io_main())
                .subscribe(new Consumer<RoleInfoResponse>() {
                    @Override
                    public void accept(RoleInfoResponse serverResponse) throws Exception {
                        if (serverResponse != null) {
                            if (callback != null) {
                                callback.call(serverResponse.result.roleLanguage, serverResponse.code);
                            }
                        } else {
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });
    }

    public void getRole(final String deviceId, final ServerCallback callback) {
        ServerApi.apiService(ServerApiService.class)
                .getRoleLanguage(getToken(), deviceId)
                .observeOn(Schedulers.io())
                .flatMap(new Function<RoleInfoResponse, ObservableSource<RoleInfoResponse>>() {
                    @Override
                    public ObservableSource<RoleInfoResponse> apply(RoleInfoResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<RoleInfoResponse>>() {
                                        @Override
                                        public ObservableSource<RoleInfoResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .getRoleLanguage(getToken(), deviceId);
                                            }
                                            return  null;
                                        }
                                    });
                        }
                        return Observable.just(serverResponse);
                    }
                })
                .compose(RxSchedulers.<RoleInfoResponse>io_main())
                .subscribe(new Consumer<RoleInfoResponse>() {
                    @Override
                    public void accept(RoleInfoResponse serverResponse) throws Exception {
                        if (serverResponse != null) {
                            if (callback != null) {
                                callback.call(String.valueOf(serverResponse.result.role), serverResponse.code);
                            }
                        } else {
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Logger.i(TAG, "getRole throwable="+throwable.getMessage());
                        if (callback != null) {
                            callback.call("", -1);
                        }
                    }
                });
    }

    public void getComposeState(final String deviceId, final ServerCallback callback) {
        ServerApi.apiService(ServerApiService.class)
                .getComposeState(getToken(), deviceId)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .getComposeState(getToken(), deviceId);
                                            }
                                            return  Observable.just(serverResponse);
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
                        if (serverResponse != null) {
                            if (callback != null) {
                                callback.call(serverResponse.result, serverResponse.code);
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });
    }

    public void getLanguage(final String deviceId, final ServerCallback callback) {
        ServerApi.apiService(ServerApiService.class)
                .getLanguage(getToken(), deviceId)
                .observeOn(Schedulers.io())
                .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                    @Override
                    public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                        if (serverResponse.code == SR_TOKEN_INVALID_CODE) {
                            return ServerApi.apiService(ServerApiService.class)
                                    .getToken(TStorageManager.getInstance().getDeviceId())
                                    .flatMap(new Function<ServerResponse, ObservableSource<ServerResponse>>() {
                                        @Override
                                        public ObservableSource<ServerResponse> apply(ServerResponse serverResponse) throws Exception {
                                            if (serverResponse.code == 1) {
                                                mToken = serverResponse.result;
                                                setToken(serverResponse.result);

                                                return ServerApi.apiService(ServerApiService.class)
                                                        .getLanguage(getToken(), deviceId);
                                            }
                                            return  Observable.just(serverResponse);
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
                        if (serverResponse != null) {
                            if (callback != null) {
                                callback.call(serverResponse.result, serverResponse.code);
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });
    }

    public Observable<NoticeResponse> getNotice(final String model, final String noticeId) {
        ServerApi.apiService(ServerApiService.class)
                .getNotice(model, noticeId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<NoticeResponse>() {
                    @Override
                    public void accept(NoticeResponse noticeResponse) throws Exception {
                        Log.i(TAG, "noticeResponse="+noticeResponse.toString());

                        if (noticeResponse.result.type == NoticeResponse.NOTICE_TYPE_TEXT) {
                            TStorageManager.getInstance().setNoticeText(noticeResponse.result.content);
                            Intent intent = new Intent("com.translator.UPDATE_NOTICE_TEXT");
                            TranslatorApp.getAppContext().sendBroadcast(intent);
                        } else if (noticeResponse.result.type == NoticeResponse.NOTICE_TYPE_PIC) {
                            TStorageManager.getInstance().setNoticePic(noticeResponse.result.content);
                            Intent intent = new Intent("com.translator.UPDATE_NOTICE_PICS");
                            TranslatorApp.getAppContext().sendBroadcast(intent);
                        }

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
        return null;
    }
}
