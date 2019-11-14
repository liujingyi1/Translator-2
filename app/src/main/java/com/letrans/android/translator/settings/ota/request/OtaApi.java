package com.letrans.android.translator.settings.ota.request;

import com.letrans.android.translator.settings.ota.response.CheckClientVersionResponse;

import java.util.Map;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface OtaApi {

    @POST("market/client/lastest")
    Observable<CheckClientVersionResponse> checkClientVersion(@HeaderMap Map<String, String> headers);

    @Streaming
    @GET
    Observable<ResponseBody> downloadApk(@Url String url);
}
