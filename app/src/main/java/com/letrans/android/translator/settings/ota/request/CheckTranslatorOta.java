package com.letrans.android.translator.settings.ota.request;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;

import com.letrans.android.translator.settings.ota.OtaConstants;
import com.letrans.android.translator.settings.ota.response.AppData;
import com.letrans.android.translator.settings.ota.response.CheckClientVersionResponse;
import com.letrans.android.translator.utils.AppUtils;
import com.letrans.android.translator.utils.Logger;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CheckTranslatorOta {

    private static final String TAG = "RTranslator/CheckTranslatorOta";
    private Context context;
    OnCheckListener listener;

    public CheckTranslatorOta(Context context, OnCheckListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void startCheckOta() {

        Observable<CheckClientVersionResponse> observable = OtaApiManager.getInstance(context).API()
                .checkClientVersion(OtaApiManager.getHeaders(context));
        BaseObserver<CheckClientVersionResponse> baseObserver = new BaseObserver<CheckClientVersionResponse>(H, CheckClientVersionResponse.class) {
            @Override
            public void setDisposable(Disposable d) {
                if (listener != null) {
                    listener.onAddDisposable(d);
                }
            }
        };

        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onTerminateDetach()
                .subscribe(baseObserver);
    }

    private Handler H = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case OtaConstants.MESSAGE_JSON_FORMAT_ERROR: {
                    handleErrorMsg("json format error", msg);
                    break;
                }
                case OtaConstants.MESSAGE_NETWORK_ERROR: {
                    handleErrorMsg("network error", msg);
                    break;
                }
                case OtaConstants.MESSAGE_SYSTEM_ERROR: {
                    handleErrorMsg("system error", msg);
                    break;
                }
                case OtaConstants.MESSAGE_SUCCESS: {
                    handleSuccess(msg);
                    break;
                }
                default:
                    break;
            }
        }
    };

    private void handleErrorMsg(String error, Message msg) {
        Logger.d(TAG,"handleErrorMsg : " + error);
        if (listener != null) {
            listener.onFailed();
        }
    }

    private void handleSuccess(Message msg) {
        if (msg.obj instanceof CheckClientVersionResponse) {
            Logger.d(TAG,"handleSuccess : ");
            CheckClientVersionResponse res = (CheckClientVersionResponse) msg.obj;
            if (res.getData() == null || TextUtils.isEmpty(res.getData().getVersionCode())) {
                if (listener != null) {
                    listener.onFailed();
                }
                return;
            }
            final AppData client = res.getData();
            String versionCode = client.getVersionCode();
            String versionName = client.getVersionName();

            String downloadType = client.getInstallType();
            ArrayList<AppData.DiffChild> diffChild = client.getDiffChildList();
            String diffVersionName = client.getDiffOldVersionName();
            String oldVersionName = AppUtils.getVersionName(context);
            Logger.d(TAG," versionCode : " + versionCode + " versionName : " + versionName
                    + " downloadType : " + downloadType
                    + " diffVersionName : " + diffVersionName + " oldVersionName : " + oldVersionName
                    + " size : " + diffChild.size());

            String message = getHTMLWithString(client.getDescribe());
            message = String.valueOf(Html.fromHtml(message));
            client.setDescribe(message);

            if (diffChild != null && diffChild.size() != 0) {
                for (AppData.DiffChild child : diffChild) {
                    diffVersionName = child.getOldVersionName();
                    Logger.d(TAG," diffVersionName : " + diffVersionName);
                    if (!TextUtils.isEmpty(diffVersionName) && oldVersionName.equals(diffVersionName)) {
                        client.setDiffUpdate(true);
                        client.setDiffDownloadUrl(child.getDownloadUrl());
                        client.setDiffOldVersionName(child.getOldVersionName());
                        client.setDiffSize(child.getSize());
                        client.setDiffChild(child);
                        if (listener != null) {
                            listener.onSuccess(client);
                        }
                        return;
                    }
                }
                client.setDiffUpdate(false);
            } else {
                client.setDiffUpdate(false);
            }
            if (listener != null) {
                listener.onSuccess(client);
            }
        }
    }

    private String getHTMLWithString(String message) {
        String content = message;
        if (content == null) {
            return "";
        }
        content = content.replaceAll("&amp;", "&");
        content = content.replaceAll("&lt;", "<");
        content = content.replaceAll("&gt;", ">");
        content = content.replaceAll("&quot;", "\"");
        content = content.replaceAll("\r&#10;", "\n");
        content = content.replaceAll("&#10;", "\n");
        content = content.replaceAll("&#032;", " ");
        content = content.replaceAll("&#039;", "'");
        content = content.replaceAll("&#033;", "!");
        return content;
    }

    public interface OnCheckListener {
        void onSuccess(AppData client);
        void onFailed();
        void onAddDisposable(Disposable d);
    }
}
