package com.letrans.android.translator.settings.ota.request;

import android.net.ParseException;
import android.os.Handler;
import android.os.Message;
import com.google.gson.JsonParseException;
import org.json.JSONException;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import com.letrans.android.translator.settings.ota.OtaConstants;
import com.letrans.android.translator.settings.ota.response.BaseResponse;
import com.letrans.android.translator.utils.Logger;

public abstract class BaseObserver<T> implements Observer<T> {

    private static final String TAG = "RTranslator/BaseObserver";
    private Handler handler;
    private String responseName;

    public BaseObserver(Handler handler, Class<?> classzz) {
        super();
        this.handler = handler;
        responseName = classzz.getSimpleName();
    }

    @Override
    public void onSubscribe(Disposable d) {
        setDisposable(d);
    }

    public abstract void setDisposable(Disposable d);
    
    @Override
    public void onNext(T t) {
        BaseResponse res = (BaseResponse) t;
        if (handler != null) {
            Logger.d(TAG," BaseObserver getStatus : " + res.getStatus());
            Message msg = new Message();
            if ("1".equals(res.getStatus())) {
                msg.what = OtaConstants.MESSAGE_SUCCESS;
                msg.obj = res;
                handler.sendMessage(msg);
            } else {
                msg.what = OtaConstants.MESSAGE_SYSTEM_ERROR;
                msg.obj = responseName;
                handler.sendMessage(msg);
            }
        }

    }

    @Override
    public void onError(Throwable e) {
        Logger.e(TAG, "onError: " + e.toString());
        if (e instanceof Exception) {
            if (e instanceof JsonParseException
                    || e instanceof JSONException
                    || e instanceof ParseException) {
                if (handler != null) {
                    Message message = new Message();
                    message.what = OtaConstants.MESSAGE_JSON_FORMAT_ERROR;
                    message.obj = responseName;
                    handler.sendMessage(message);
                }
            } else {
                if (handler != null) {
                    Message message = new Message();
                    message.what = OtaConstants.MESSAGE_NETWORK_ERROR;
                    message.obj = responseName;
                    handler.sendMessage(message);
                }
            }
        } else {
            if (handler != null) {
                Message message = new Message();
                message.what = OtaConstants.MESSAGE_NETWORK_ERROR;
                message.obj = responseName;
                handler.sendMessage(message);
            }
        }
    }

    @Override
    public void onComplete() {

    }
}
