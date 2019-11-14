package com.letrans.android.translator.mpush.domain;

import com.letrans.android.translator.mpush.bean.ServerListResponse;
import com.letrans.android.translator.mpush.bean.ServerPairInfoBean;
import com.letrans.android.translator.mpush.bean.ServerRegistBean;

import java.util.HashMap;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface ServerApiService {

    @Headers("Content-Type:application/json; charset=utf-8")
    @POST("mpush/push/")
    Observable<ServerResponse> sendMessage(
            @Header("token") String token,
            @QueryMap HashMap<String, String> sendParams
    );

    @Multipart
    @POST("mpush/push/")
    Observable<ServerResponse> sendMessage2(
            @Header("token") String token,
            @QueryMap HashMap<String, Object> sendParams,
            @Part("content")RequestBody content
            );

    @GET("client/token/" + "{deviceId}")
    Observable<ServerResponse> getToken(
            @Path("deviceId") String deviceId
    );

    @GET("client/registe/" + "{deviceId}")
    Observable<ServerRegistBean> registClient(
            @Header("token") String token,
            @Path("deviceId") String deviceId
    );

    @GET("client/pairingCode/get/" + "{deviceId}")
    Observable<ServerResponse> getPairCode(
            @Header("token") String token,
            @Path("deviceId") String deviceId
    );

    @GET("client/pairingCode/invalid/" + "{deviceId}"+"/"+"{pairingCode}")
    Observable<ServerResponse> invalidPairCode(
            @Header("token") String token,
            @Path("deviceId") String deviceId,
            @Path("pairingCode") String pairingCode
    );

    @GET("client/paired/" + "{fromDeviceId}"+"/"+"{pairingCode}")
    Observable<ServerResponse> pairClient(
            @Header("token") String token,
            @Path("fromDeviceId") String fromDeviceId,
            @Path("pairingCode") String pairingCode
    );

    @GET("client/relieve/" + "{fromDeviceId}"+"/"+"{toDeviceId}")
    Observable<ServerResponse> relievePair(
            @Header("token") String token,
            @Path("fromDeviceId") String fromDeviceId,
            @Path("toDeviceId") String toDeviceId
    );

    @GET("channel/getNum/")
    Observable<ServerResponse> getChannelNum(
            @Header("token") String token
    );

    @GET("channel/join/" + "{deviceId}"+"/"+"{number}")
    Observable<ServerResponse> joinChannel(
            @Header("token") String token,
            @Path("deviceId") String deviceId,
            @Path("number") long number
    );

    @GET("channel/exit/" + "{deviceId}"+"/"+"{number}")
    Observable<ServerResponse> exitChannel(
            @Header("token") String token,
            @Path("deviceId") String deviceId,
            @Path("number") long number
    );

    @GET("mpush/serverList/")
    Observable<ServerListResponse> getServerList(
            @Header("token") String token
    );

    @GET("mpush/kick/"+"{deviceId}")
    Observable<ServerResponse> kickUser(
            @Header("token") String token,
            @Path("deviceId") String deviceId
    );

    @GET("client/paireInfo/"+"{deviceId}")
    Observable<ServerPairInfoBean> getPairInfo(
            @Header("token") String token,
            @Path("deviceId") String deviceId
    );

    @GET("client/hangUp/"+"{fromDeviceId}"+"/"+"{toDeviceId}"+"/"+"{type}")
    Observable<ServerResponse> hangUp(
            @Header("token") String token,
            @Path("fromDeviceId") String fromDeviceId,
            @Path("toDeviceId") String toDeviceId,
            @Path("type") int type
    );

    @GET("client/language/"+"{fromDeviceId}"+"/"+"{language}")
    Observable<ServerResponse> enterCompose(
            @Header("token") String token,
            @Path("fromDeviceId") String fromDeviceId,
            @Path("language") String language
    );

    @GET("client/role/"+"{deviceId}"+"/"+"{role}"+"/"+"{language}")
    Observable<ServerResponse> changeRole(
            @Header("token") String token,
            @Path("deviceId") String deviceId,
            @Path("role") int role,
            @Path("language") String language
    );

    @GET("client/role/language/"+"{deviceId}")
    Observable<RoleInfoResponse> getRoleLanguage(
            @Header("token") String token,
            @Path("deviceId") String deviceId
    );

    @GET("client/state/"+"{deviceId}")
    Observable<ServerResponse> getComposeState(
            @Header("token") String token,
            @Path("deviceId") String deviceId
    );

    @GET("client/language/get/"+"{deviceId}")
    Observable<ServerResponse> getLanguage(
            @Header("token") String token,
            @Path("deviceId") String deviceId
    );

    @GET("http://116.62.168.13:8081/translator/notice/get/"+"{model}"+"/"+"{noticeId}")
    Observable<NoticeResponse> getNotice(
            @Path("model") String model,
            @Path("noticeId") String noticeId
    );
}
