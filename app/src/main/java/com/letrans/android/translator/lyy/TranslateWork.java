package com.letrans.android.translator.lyy;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.protobuf.ByteString;
import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.lyy.android.lyymsgbyte.GreeterGrpc;
import com.lyy.android.lyymsgbyte.LyyMsgByteReply;
import com.lyy.android.lyymsgbyte.LyyMsgByteRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class TranslateWork {
    private static final String TAG = "RTranslator/TranslateWork";

    private final ManagedChannel channel;
    private GreeterGrpc.GreeterBlockingStub stub;

    private String LYY_SP_FILE = "lyy.config";
    private String LYY_KEY_TOKEN = "token";
    private String LYY_KEY_TIME = "time";
    private long tokenTime = 0;
    private static long TOKEN_INVALID_TIME = 1000 * 60 * 3 - 10000;


    String resultCode = LyyConstants.RESULT_CODE_SUCCESS;

    public TranslateWork(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        channel.notifyWhenStateChanged(channel.getState(true), new Runnable() {
            @Override
            public void run() {
                Logger.i(TAG, "channel state change");
                Logger.i(TAG, "channel state=" + channel.getState(true));

            }
        });
    }

    public void shutdown() throws InterruptedException {
        Logger.i(TAG, "shutdown...");
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void checkChannelState() {
        ConnectivityState state = channel.getState(true);
        Logger.i(TAG, "checkChanneState=" + state);
//        if (state == ConnectivityState.TRANSIENT_FAILURE
//                || state == ConnectivityState.IDLE) {
//            resetConnect();
//        }
        resetConnect();
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void resetConnect() {
        Logger.i(TAG, "resetConnect...");
        channel.resetConnectBackoff();
    }

    //客户端方法
    public String greet(String content, String langfrom, String langto) {
        //需要用到存根时创建,不可复用
        stub = GreeterGrpc.newBlockingStub(channel);

        Logger.i(TAG, "GrpcTask stub=" + stub.toString());
        //得到message，去构造json与压缩
        long current = System.currentTimeMillis();
        String lyyToken = getToken(LyyConstants.TOKEN_TYPE_TT);
        Logger.i(TAG, "lyytoken=" + lyyToken);
        String replay_result = "";
        try {
            Logger.i(TAG, "current - tokenTime=" + (current - tokenTime));
            if (((current - tokenTime) > TOKEN_INVALID_TIME) || TextUtils.isEmpty(lyyToken)) {

                String str_json = "";
                str_json = make_token_message_json(LyyConstants.TOKEN_TYPE_TT);
                byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
                LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
                LyyMsgByteReply reply = stub.sayLyyMsgByte(request);
                String reply_string = "";
                reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
                Logger.i(TAG, "getServerToken... reply_string=" + reply_string);
                lyyToken = analysis_tt_reply_json(reply_string);

                Logger.i(TAG, "new Token=" + lyyToken);
                saveToken(lyyToken, LyyConstants.TOKEN_TYPE_TT, System.currentTimeMillis());
            }

            if (!TextUtils.isEmpty(lyyToken)) {
                String str_json = "";
                str_json = make_tt_message_json(content, langfrom, langto, lyyToken);
                Logger.i(TAG, "tt_message str_json=" + str_json);

                byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
                LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
                LyyMsgByteReply reply = stub.sayLyyMsgByte(request);
                String reply_string = "";

                reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
                Logger.i(TAG, "GrpcTask... doInBackground reply_string=" + reply_string);
                replay_result = analysis_tt_reply_json(reply_string);
                return replay_result;
            } else {
                return "";
            }
        } catch (StatusRuntimeException e) {
            Logger.i(TAG, "RPC调用失败:" + e.getMessage());
        }
        Logger.i(TAG, "服务器返回信息:" + replay_result);
        return "";
    }

    //客户端方法
    public String getNotice(String opflag) {
        stub = GreeterGrpc.newBlockingStub(channel);

        Logger.i(TAG, "GrpcTask stub=" + stub.toString());
        //得到message，去构造json与压缩
        long current = System.currentTimeMillis();
        String lyyToken = getToken(LyyConstants.TOKEN_TYPE_NOTICE);
        Logger.i(TAG, "lyytoken=" + lyyToken);
        String replay_result = "";
        try {
            Logger.i(TAG, "current - tokenTime=" + (current - tokenTime));
            if (((current - tokenTime) > TOKEN_INVALID_TIME) || TextUtils.isEmpty(lyyToken)) {

                String str_json = "";
                str_json = make_token_message_json(LyyConstants.TOKEN_TYPE_NOTICE);
                byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
                LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
                LyyMsgByteReply reply = stub.sayLyyMsgByte(request);
                String reply_string = "";
                reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
                Logger.i(TAG, "getServerToken... reply_string=" + reply_string);
                lyyToken = analysis_tt_reply_json(reply_string);

                Logger.i(TAG, "new Token=" + lyyToken);
                saveToken(lyyToken, LyyConstants.TOKEN_TYPE_NOTICE, System.currentTimeMillis());
            }

            if (!TextUtils.isEmpty(lyyToken)) {
                String str_json = "";
                str_json = getNoticeRequest(opflag, lyyToken);
                Logger.i(TAG, "notice_message str_json=" + str_json);

                byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
                LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
                LyyMsgByteReply reply = stub.sayLyyMsgByte(request);
                String reply_string = "";

                reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
                Logger.i(TAG, "GrpcTask... doInBackground reply_string=" + reply_string);
                replay_result = analysis_tt_reply_json(reply_string);
                return replay_result;
            } else {
                return "";
            }
        } catch (StatusRuntimeException e) {
            Logger.i(TAG, "RPC调用失败:" + e.getMessage());
        }
        Logger.i(TAG, "服务器返回信息:" + replay_result);
        return "";
    }

    private String getNoticeRequest(String opflag, String token) {
        String result = "";
        try {

            JSONObject json_root = new JSONObject();
            json_root.put("processCode", LyyConstants.PROCESS_CODE_GETNOTICE);
            json_root.put("token", token);
            json_root.put("deviceID", TStorageManager.getInstance().getDeviceId());
            json_root.put("deviceType", LyyConstants.DEVICE_TYPE_TABLET);
            json_root.put("opflag",opflag);
            result = json_root.toString();
        } catch (Exception e) {
            resultCode = LyyConstants.RESULT_CODE_UNKNOWN;
        }
        return result;
    }


    private String getToken(String type) {
        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(LYY_SP_FILE, Context.MODE_PRIVATE);
        String token = sp.getString(LYY_KEY_TOKEN+type, "");
        tokenTime = sp.getLong(LYY_KEY_TIME+type, 0);
        return token;
    }

    private void saveToken(String token, String type, long time) {
        SharedPreferences sp = TranslatorApp.getAppContext().getSharedPreferences(LYY_SP_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(LYY_KEY_TIME+type, time);
        editor.putString(LYY_KEY_TOKEN+type, token);
        editor.commit();
    }

    public String make_tt_message_json(String content, String langfrom, String langto, String token) {

        String result = "";
        String internalflag = "0";
        String ispflag = "00";
        try {
            if ((langfrom.equals("en-US")) && (langto.equals("zh-CN"))
                    || (langfrom.equals("zh-CN")) && (langto.equals("en-US"))) {
                langfrom = langfrom.substring(0, 2);
                langto = langto.substring(0, 2);
                internalflag = "1";
                ispflag = "02";
            } else {
                internalflag = "0";
                langfrom = LyyConstants.getCountryCode(langfrom, internalflag);
                langto = LyyConstants.getCountryCode(langto, internalflag);
            }

            JSONObject json_root = new JSONObject();
            json_root.put("processCode", LyyConstants.PROCESS_CODE_TT);
            JSONArray jsonarray_items = new JSONArray();
            JSONObject json_item = new JSONObject();
            json_item.put("token", token);
            json_item.put("internalflag", internalflag);
            json_item.put("langfrom", langfrom);
            json_item.put("langto", langto);
            json_item.put("inparam", content);
            json_item.put("ispflag", ispflag);
            jsonarray_items.put(json_item);
            JSONObject json_detaillist = new JSONObject();
            json_detaillist.put("DetailList", jsonarray_items);
            json_root.put("orderContent", json_detaillist);
            result = json_root.toString();
        } catch (Exception e) {
            resultCode = LyyConstants.RESULT_CODE_UNKNOWN;
        }
        return result;
    }

    public String make_token_message_json(String type) {
        String result = "";
        try {
            JSONObject json_root = new JSONObject();
            json_root.put("processCode", LyyConstants.PROCESS_CODE_TOKEN);
            json_root.put("opflag", type);
            json_root.put("deviceType", LyyConstants.DEVICE_TYPE_TABLET);
            json_root.put("deviceID", TStorageManager.getInstance().getDeviceId());
            result = json_root.toString();
        } catch (Exception e) {
            resultCode = LyyConstants.RESULT_CODE_UNKNOWN;
        }
        return result;
    }

    public String analysis_tt_reply_json(String strIn) {
        String result = "";
        if (strIn.length() > 0) {
            try {
                JSONObject jsonObject = new JSONObject(strIn);
                String flag = "";
                flag = jsonObject.getString("respCode");
                Logger.i(TAG, "analysis_tt_reply_json... flag=" + flag);
                if (flag.equals("0")) {
                    result = jsonObject.getString("result");
                    Logger.i(TAG, "analysis_tt_reply_json... result=" + result);
                    resultCode = LyyConstants.RESULT_CODE_SUCCESS;
                } else {
                    result = "";
                    resultCode = LyyConstants.RESULT_CODE_FAIL;
                }
            } catch (Exception e) {
                resultCode = LyyConstants.RESULT_CODE_UNKNOWN;
            }
        } else {
            Logger.i(TAG, "analysis_tt_reply_json... strIn=" + strIn);
        }
        return result;
    }
}
