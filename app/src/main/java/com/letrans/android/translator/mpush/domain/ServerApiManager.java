package com.letrans.android.translator.mpush.domain;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApiManager {

    private static final String BASE_URL = "http://116.62.168.13:8080/translator/";

    private HashMap<Class, Retrofit> mRetrofitServiceHashMap = new HashMap<>();

    private Retrofit mApiRetrofit;
    private ConcurrentHashMap<Class, Object> cachedApis = new ConcurrentHashMap<>();

    public ServerApiManager() {

        // init okhttp 3 logger
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        // init cache
        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logInterceptor)
                .build();

        mApiRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(StringConverterFactory.create())
                .build();

        mRetrofitServiceHashMap.put(ServerApiService.class, mApiRetrofit);
    }

    public <T> void addService(Class<T> clz) {
        mRetrofitServiceHashMap.put(clz, mApiRetrofit);
    }

    public <T> T getService(Class<T> clz) {
        Object obj = cachedApis.get(clz);
        if (obj != null) {
            return (T) obj;
        } else {
            Retrofit retrofit = mRetrofitServiceHashMap.get(clz);
            if (retrofit != null) {
                T service = retrofit.create(clz);
                cachedApis.put(clz, service);
                return service;
            } else {
                return null;
            }
        }
    }
}
