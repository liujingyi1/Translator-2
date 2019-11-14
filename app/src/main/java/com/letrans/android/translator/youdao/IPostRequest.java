package com.letrans.android.translator.youdao;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface IPostRequest {
    @POST("translate?doctype=json&jsonversion=&keyfrom=&model=&mid=&imei=&vendor" +
            "=&screen" +
            "=&ssid=&network=&abtest=")
    @FormUrlEncoded
    Call<PostTranslation> getCall(@Field("i") String targetSentence, @Query("type") String xtx);
}
