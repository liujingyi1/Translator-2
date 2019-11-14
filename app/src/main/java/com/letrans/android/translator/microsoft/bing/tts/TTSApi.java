package com.letrans.android.translator.microsoft.bing.tts;


import io.reactivex.Observable;
import retrofit2.Call;

import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface TTSApi {

    @Headers("Content-Type:application/json; charset=utf-8")
    @POST("issueToken")
    Call<String> requestToken(@Header("Ocp-Apim-Subscription-Key") String apikey);
    
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    @POST("issueToken")
    Call<String> requestAreaToken(@Header("Ocp-Apim-Subscription-Key") String apikey, @Header("Host") String host);

    @Headers("Content-Type:application/json; charset=utf-8")
    @POST("issueToken")
    Observable<String> requestTokens(@Header("Ocp-Apim-Subscription-Key") String apikey);

    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    @POST("issueToken")
    Observable<String> requestAreaTokens(@Header("Ocp-Apim-Subscription-Key") String apikey, @Header("Host") String host);

}
