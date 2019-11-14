package com.letrans.android.translator.settings.ota.request;

import android.content.Context;

import com.letrans.android.translator.settings.ota.OtaTools;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.AppUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class OtaApiManager {

    private static OtaApiManager instance = null;
    private OtaApi otaApi;
    private static final String baseUri = "http://myui2.qingcheng.com/api/";
    private static final String baseUri_test = "http://test.apiv1.kindui.com/api/";

    private OtaApiManager(Context context) {
        OkHttpClient okHttpClient = getNewClient(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUri)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();
        otaApi = retrofit.create(OtaApi.class);
    }

    private static OkHttpClient getNewClient(Context context) {

        final String packageName = context.getPackageName();
        final String finalVersionCode = String.valueOf(AppUtils.getVersion(context));
        final String imei = TStorageManager.getInstance().getDeviceId();
        final String mobileName = OtaTools.getPhoneModel();
        final String mobileRom = AppUtils.getVersionName(context);
        final String sim = OtaTools.getSimSerialNumber(context);
        final String dpi = OtaTools.getDpi(context);
        final String apiLevel = OtaTools.getApiLevel();
        Interceptor headersInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request.Builder builder = chain.request().newBuilder();
                builder.addHeader("Charset", "UTF-8");
                builder.addHeader("Content-Type", "application/x-www-form-urlencoded");
                builder.addHeader("packageName", packageName);
                builder.addHeader("versionCode", finalVersionCode);
                builder.addHeader("channelId", "GOAppMarket");
                builder.addHeader("mobileName", mobileName);
                builder.addHeader("imei", imei);
                builder.addHeader("mobileRom", mobileRom);
                builder.addHeader("dpi", dpi);
                builder.addHeader("apilevel", apiLevel);
                builder.addHeader("sim", sim);
                return chain.proceed(builder.build());
            }
        };

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(headersInterceptor)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5,TimeUnit.SECONDS)
                .writeTimeout(5,TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static OtaApiManager getInstance(Context appContext) {
        if (instance == null) {
            synchronized (OtaApiManager.class) {
                if (instance == null) {
                    instance = new OtaApiManager(appContext);
                }
            }
        }
        return instance;
    }

    public OtaApi API() {
        return otaApi;
    }

    public static Map<String, String> getHeaders(Context context) {
        Map<String, String> headers = new HashMap<String, String>();
        String city = "";
        try {
            city = URLEncoder.encode("Shanghai", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        headers.put("city", city);
        headers.put("netType", OtaTools.getNetType(context));
        headers.put("resolution", "1280_800");
        return headers;
    }


}
