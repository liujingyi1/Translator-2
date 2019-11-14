package com.letrans.android.translator.roobo;

import android.content.Context;

import com.letrans.android.translator.utils.AppUtils;
import com.letrans.android.translator.utils.Logger;
import com.roobo.toolkit.RError;
import com.roobo.vui.api.InitListener;
import com.roobo.vui.api.VUIApi;
import com.roobo.vui.api.tts.RTTSPlayer;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

public class RooboManager {

    private static final String TAG = "RTranslator/RooboManager";
    private static RooboManager mInstance;
    private static final int STATE_INITED = 0;
    private static final int STATE_INIT_IDLE = 1;
    private static final int STATE_INIT = 2;
    private int state = STATE_INIT_IDLE;
    private final CopyOnWriteArrayList<RooboInitListener> mCallbacks = new CopyOnWriteArrayList<>();

    private RooboManager() {

    }

    public static RooboManager getInstance() {
        if (mInstance == null) {
            synchronized (RooboManager.class) {
                if (mInstance == null) {
                    mInstance = new RooboManager();
                }
            }
        }
        return mInstance;
    }

    public void initRoobo(RooboInitListener listener) {
        Logger.d(TAG, "initRoobo state : " + state);
        if (state == STATE_INITED) {
            if (listener != null) {
                listener.onSuccess();
            }
        } else if (state == STATE_INIT_IDLE) {
            state = STATE_INIT;
            mCallbacks.add(listener);
            init(AppUtils.getApp());
        } else if (state == STATE_INIT) {
            mCallbacks.add(listener);
        }
    }

    private void init(Context context) {
        VUIApi.InitParam.InitParamBuilder builder = new VUIApi.InitParam.InitParamBuilder();
        builder.setLanguage(RConstants.VOICE_EN_US)
                .addOfflineFileName("rtranslator_roobo")
                .setTTSType(RTTSPlayer.TTSType.TYPE_OFFLINE)
                .setVUIType(VUIApi.VUIType.MANUAL)
                .setMicModel(VUIApi.SourceType.ANDROID_STANDARD)
                .setAudioGenerator(new CustomAndroidAudioGenerator());

        VUIApi.getInstance().init(context, builder.build(),
                new InitListener() {
                    @Override
                    public void onSuccess() {
                        Logger.d(TAG, "onSuccess: called " + mCallbacks.size());
                        state = STATE_INITED;
                        for (int i = 0; i < mCallbacks.size(); i++) {
                            RooboInitListener cb = mCallbacks.get(i);
                            if (cb != null) {
                                cb.onSuccess();
                            }
                        }
                        mCallbacks.clear();
                    }

                    @Override
                    public void onFail(RError rError) {
                        Logger.e(TAG, "onFail: " + rError.getFailDetail());
                        state = STATE_INIT_IDLE;
                        for (int i = 0; i < mCallbacks.size(); i++) {
                            RooboInitListener cb = mCallbacks.get(i);
                            if (cb != null) {
                                cb.onFail();
                            }
                        }
                        mCallbacks.clear();
                    }
                });
    }

    public void removeCallback(RooboInitListener callback) {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            if (mCallbacks.get(i) == callback) {
                mCallbacks.remove(i);
            }
        }
    }

    public void release() {
        VUIApi.getInstance().release();
        mCallbacks.clear();
        state = STATE_INIT_IDLE;
    }
}
