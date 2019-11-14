//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Speech-TTS
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.letrans.android.translator.microsoft.bing.tts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.letrans.android.translator.R;
import com.letrans.android.translator.tts.ITTSListener;
import com.letrans.android.translator.utils.AppUtils;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;

public class Authentication {
    private static final String LOG_TAG = "RTranslator/Authentication";

    private String apiKey;
    private static SharedPreferences sp;
    private static final String BING_TTS_TOKEN = "bing_tts_token";
    private static final String MIC_TTS_TOKEN = "mic_tts_token";
    private static final String SP_FILE_NAME = "translator.cfg";
    private static final String BING_TTS_TOKEN_TIME = "bing_tts_token_time";
    private static final String MIC_TTS_TOKEN_TIME = "mic_tts_token_time";
    private static final int MSG_SUCCESS_TOKEN = 0;
    private static final int MSG_ERROR_TOKEN = 1;
    private static final int MSG_SUCCESS_BING_TOKEN = 2;
    private final int RefreshTokenDuration = 9 * 60 * 1000;
    private List<Disposable> disposableList = new ArrayList<>();

    public Authentication() {
        apiKey = Utils.getEncryptString(AppUtils.getApp(), R.string.primaryKey);
        initBingTTSToken(apiKey);
        initMicTTSToken(MSDataModel.getTtsBean().getApiKey(), MSDataModel.getTtsBean().getHost());
    }

    @SuppressLint("CheckResult")
    public String getMicTTSToken(String apiKey, String host) {
        Call<String> call = TTSApiManager.getInstance().MicAPI().requestAreaToken(apiKey, host);
        try {
            String token = call.execute().body();
            saveToken(MIC_TTS_TOKEN, token);
            saveTokenTime(MIC_TTS_TOKEN_TIME, System.currentTimeMillis());
            sendMessage(MSG_SUCCESS_TOKEN);
            return token;
        } catch (IOException e) {
            if (handler != null) {
                handler.sendEmptyMessage(MSG_ERROR_TOKEN);
            }
            e.printStackTrace();
        }

//        call.enqueue(new Callback<String>() {
//            @Override
//            public void onResponse(Call<String> call, Response<String> response) {
//                if (response.body() != null) {
//                    String token = response.body().toString();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<String> call, Throwable t) {
//            }
//        });
        return null;
    }

    @SuppressLint("CheckResult")
    public String getBingTTSToken(String apiKey) {
        Call<String> call = TTSApiManager.getInstance().API().requestToken(apiKey);
        try {
            String token = call.execute().body();
            saveToken(BING_TTS_TOKEN, token);
            saveTokenTime(BING_TTS_TOKEN_TIME, System.currentTimeMillis());
            sendMessage(MSG_SUCCESS_BING_TOKEN);
            return token;
        } catch (IOException e) {
            if (handler != null) {
                handler.sendEmptyMessage(MSG_ERROR_TOKEN);
            }
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("CheckResult")
    private void initBingTTSToken(final String apiKey) {
        String accessToken = getToken(BING_TTS_TOKEN);
        if (!TextUtils.isEmpty(accessToken) && (System.currentTimeMillis()
                - getTokenTime(BING_TTS_TOKEN_TIME) < 9 * 60 * 1000)) {
            return;
        }
        TTSApiManager.getInstance().API()
                .requestTokens(apiKey)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        if (disposableList != null) {
                            disposableList.add(disposable);
                        }
                    }
                })
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String ttsResponse) throws Exception {
                        saveToken(BING_TTS_TOKEN, ttsResponse);
                        saveTokenTime(BING_TTS_TOKEN_TIME, System.currentTimeMillis());
                        sendMessage(MSG_SUCCESS_BING_TOKEN);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Logger.e(LOG_TAG, "get token wrong : " + throwable);
                        saveToken(BING_TTS_TOKEN,"");
                        handler.sendEmptyMessage(MSG_ERROR_TOKEN);
                    }
                });

    }

    @SuppressLint("CheckResult")
    private void initMicTTSToken(final String apiKey, final String host) {
        String accessToken = getToken(MIC_TTS_TOKEN);
        if (!TextUtils.isEmpty(accessToken) && (System.currentTimeMillis()
                - getTokenTime(MIC_TTS_TOKEN_TIME) < 9 * 60 * 1000)) {
            return;
        }
        TTSApiManager.getInstance().MicAPI()
                .requestAreaTokens(apiKey, host)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        if (disposableList != null) {
                            disposableList.add(disposable);
                        }
                    }
                })
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String ttsResponse) throws Exception {
                        saveToken(MIC_TTS_TOKEN, ttsResponse);
                        saveTokenTime(MIC_TTS_TOKEN_TIME, System.currentTimeMillis());
                        sendMessage(MSG_SUCCESS_TOKEN);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Logger.e(LOG_TAG, "get mic token wrong : " + throwable);
                        saveToken(MIC_TTS_TOKEN,"");
                        handler.sendEmptyMessage(MSG_ERROR_TOKEN);
                    }
                });

    }

    public void saveToken(String key, String value) {
        if (sp == null) {
            sp = AppUtils.getApp().getSharedPreferences(SP_FILE_NAME, 0);
        }
        sp.edit().putString(key, value).apply();
    }

    public String getToken(String key) {
        if (sp == null) {
            sp = AppUtils.getApp().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        }
        return sp.getString(key, "");

    }

    public void saveTokenTime(String key, long time) {
        if (sp == null) {
            sp = AppUtils.getApp().getSharedPreferences(SP_FILE_NAME, 0);
        }
        sp.edit().putLong(key, time).apply();
    }

    public long getTokenTime(String key) {
        if (sp == null) {
            sp = AppUtils.getApp().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        }
        return sp.getLong(key, 0);
    }


    Handler handler = new Handler() {
          @Override
          public void handleMessage(Message msg) {
              super.handleMessage(msg);
              switch (msg.what) {
                  case MSG_SUCCESS_TOKEN: {
                      initMicTTSToken(MSDataModel.getTtsBean().getApiKey(), MSDataModel.getTtsBean().getHost());
                      break;
                  }
                  case MSG_SUCCESS_BING_TOKEN: {
                      initBingTTSToken(apiKey);
                      break;
                  }
                  case MSG_ERROR_TOKEN: {
                      break;
                  }
                  default:
                      break;
              }
          }
      };

    public void release() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (disposableList != null && disposableList.size() > 0) {
            for (Disposable d : disposableList) {
                if (d != null && !d.isDisposed()) {
                    d.dispose();
                }
            }
        }
    }

    private void sendMessage(int msg) {
        if (handler != null) {
            handler.removeMessages(msg);
            handler.sendEmptyMessageDelayed(msg, RefreshTokenDuration);
        }
    }
}
