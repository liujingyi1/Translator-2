package com.letrans.android.translator.microsoft.bing.tts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class TTSApiManager {

    private static final String baseUri = "https://api.cognitive.microsoft.com/sts/v1.0/";
    private static TTSApiManager mTTSApiManager;
    private static TTSApi ttsMicApi;
    private static TTSApi ttsApi;

    private TTSApiManager() {

        Gson gson = new GsonBuilder().setLenient().create();
        OkHttpClient okHttpClient = getNewClient();
        Retrofit retrofitMic = new Retrofit.Builder()
                .baseUrl(MSDataModel.getTtsBean().getBaseTtsUri())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUri)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();
        ttsMicApi = retrofitMic.create(TTSApi.class);
        ttsApi = retrofit.create(TTSApi.class);
    }

    private static OkHttpClient getNewClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10,TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static TTSApiManager getInstance() {
        if (mTTSApiManager == null) {
            synchronized (TTSApiManager.class) {
                if (mTTSApiManager == null)
                    mTTSApiManager = new TTSApiManager();
            }
        }
        return mTTSApiManager;
    }

    public TTSApi MicAPI() {
        //location problem
        Gson gson = new GsonBuilder().setLenient().create();
        OkHttpClient okHttpClient = getNewClient();
        Retrofit retrofitMic = new Retrofit.Builder()
                .baseUrl(MSDataModel.getTtsBean().getBaseTtsUri())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();
        ttsMicApi = retrofitMic.create(TTSApi.class);
        return ttsMicApi;
    }

    public TTSApi API() {
        return ttsApi;
    }
}
