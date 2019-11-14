package com.letrans.android.translator.settings.about;

import android.os.Handler;

import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FeedBackManager {

    private static final String TAG = "RTranslator/FeedBackManager";
    public static final String HTTP_HEAD_TEST = "http://test.kindui.com/api";
    private static final String HTTP_HEAD = "http://api.kindui.com/api";
    private static final String API_FEEDBACK_URL = "/push/addUserFeedbackMsg";
    private static final String KEY_feedbackPushPicture = "feedbackPushPicture";
    private static final int FEEDBACK_SUCCEEDED = 0;
    private static final int FEEDBACK_FAILED = 1;
    private static final int FEEDBACK_TIMEOUT = 2;
    private String baseUrl;
    private OkHttpClient client;

    public FeedBackManager() {
        baseUrl = HTTP_HEAD + API_FEEDBACK_URL;
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
    }

    public void commitUserFeedbackToService(Handler handler, String label, String description, String phone, File file) {
        String imei = TStorageManager.getInstance().getDeviceId();
        RequestBody body;
        Logger.d(TAG, "commitUserFeedbackToService "+file);
        if(file == null ) {
            body = new FormBody.Builder()
                    .add("feedbackMsgType", "string")
                    .add("deviceId", imei)
                    .add("feedbackPushType", label)
                    .add("feedbackPushDescription", description)
                    .add("feedbackPushEmail", phone)
                    .add("versionCode", "PUSH_G2_4.3.13")
                    .add("productModel", "Translator")
                    .build();
        } else {
            RequestBody fileBody = MultipartBody.create(MediaType.parse("audio/wav"), file);
            body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("feedbackMsgType", "voice")
                    .addFormDataPart("deviceId", imei)
                    .addFormDataPart("feedbackPushType", label)
                    .addFormDataPart("feedbackPushDescription", description)
                    .addFormDataPart("feedbackPushEmail", phone)
                    .addFormDataPart("versionCode", "PUSH_G2_4.3.13")
                    .addFormDataPart("productModel", "Translator")
                    .addFormDataPart(KEY_feedbackPushPicture, file.getName(), fileBody)
                    .build();
        }
        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                if (handler != null) {
                    handler.sendEmptyMessage(FEEDBACK_SUCCEEDED);
                }
            } else {
                if (handler != null) {
                    handler.sendEmptyMessage(FEEDBACK_FAILED);
                }
            }
        } catch (IOException e) {
            if (handler != null) {
                handler.sendEmptyMessage(FEEDBACK_TIMEOUT);
            }
            e.printStackTrace();
        }
    }
}
