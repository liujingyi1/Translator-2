package com.letrans.android.translator.lyy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.google.protobuf.ByteString;
import com.letrans.android.translator.R;
import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.translate.ITranslate;
import com.letrans.android.translator.translate.ITranslateFinishedListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.ToastUtils;
import com.lyy.android.lyymsgbyte.GreeterGrpc;
import com.lyy.android.lyymsgbyte.LyyMsgByteReply;
import com.lyy.android.lyymsgbyte.LyyMsgByteRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public class LyyTranslate implements ITranslate {
    private static final String TAG = "RTranslator/LyyTranslate";

    private static final String ADDRES = "222.186.36.150";
    private static final int PORT = 50051;

    private String LYY_SP_FILE = "lyy.config";
    private String LYY_KEY_TOKEN = "token";
    private String LYY_KEY_TIME = "time";

    private static long TOKEN_INVALID_TIME = 1000 * 60 * 10 - 100;

    private long tokenTime = 0;
    boolean isRelease = false;
    String resultCode = LyyConstants.RESULT_CODE_SUCCESS;

    Context context;

    private ITranslateFinishedListener mTranslateFinishedListener;
    ExecutorService cachedThreadPool;

    public LyyTranslate() {
        this.context = TranslatorApp.getAppContext();
        cachedThreadPool = Executors.newCachedThreadPool();
    }

    public void release() {
        isRelease = true;
        cachedThreadPool.shutdown();
        TranslatePool.clearObject();
    }


    private String getToken() {
        SharedPreferences sp = context.getSharedPreferences(LYY_SP_FILE, Context.MODE_PRIVATE);
        String token = sp.getString(LYY_KEY_TOKEN, "");
        tokenTime = sp.getLong(LYY_KEY_TIME, 0);
        return token;
    }

    private void saveToken(String token, long time) {
        SharedPreferences sp = context.getSharedPreferences(LYY_SP_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(LYY_KEY_TIME, time);
        editor.putString(LYY_KEY_TOKEN, token);
        editor.commit();
    }

    @Override
    public void setTranslateFinishedListener(ITranslateFinishedListener listener) {
        mTranslateFinishedListener = listener;
    }

    @Override
    public void doTranslate(String content, String fromLanguage, String targetLanguage, final long token) {
        Logger.i(TAG, "doTranslator");
        if (isRelease) {
            return;
        }
        Logger.i(TAG, "doTranslator... content=" + content + " fromLanguage=" + fromLanguage + "" +
                " targetLanguage=" + targetLanguage + " toke=" + token);
        resultCode = LyyConstants.RESULT_CODE_SUCCESS;

        WorkCallBack<TranslateWork> workCallBack = new WorkCallBack<TranslateWork>() {
            @Override
            public void callback(TranslateWork translator1) {
                String result = translator1.greet(content, fromLanguage, targetLanguage);
                if (!isRelease && mTranslateFinishedListener != null) {
                    Logger.i(TAG, "doTranslator...call back token=" + token);
                    mTranslateFinishedListener.onTranslateFinish(result, token);
                }
            }
        };
        cachedThreadPool.execute(TranslatePool.execute(workCallBack));

//        new Thread(TranslatePool.execute(workCallBack)).start();

//        new GrpcTask(token, 1).executeOnExecutor(Executors.newCachedThreadPool(),content, fromLanguage, targetLanguage);
    }

    private class GrpcTask extends AsyncTask<String, Void, String> {
        private long messageId;
        private ManagedChannel taskChannel;
        private GreeterGrpc.GreeterBlockingStub taskStub;
        private String content;
        private String langfrom;
        private String langto;
        private int retryTime;

        private GrpcTask(long messageId, int retryTime) {
            this.messageId = messageId;
            this.retryTime = retryTime;
        }

        @Override
        protected String doInBackground(String... params) {
            content = params[0];
            langfrom = params[1];
            langto = params[2];
            Logger.i(TAG, "GrpcTask doTranslator... content=" + content + " langfrom=" + langfrom + " langto=" + langto);
            try {
                taskChannel = ManagedChannelBuilder.forAddress(ADDRES, PORT).usePlaintext().build();
                taskStub = GreeterGrpc.newBlockingStub(taskChannel);
                Logger.i(TAG, "GrpcTask stub=" + taskStub.toString());
                //得到message，去构造json与压缩
                long current = System.currentTimeMillis();
                String lyyToken = getToken();
                Logger.i(TAG, "lyytoken=" + lyyToken);
                if (((current - tokenTime) > TOKEN_INVALID_TIME) || TextUtils.isEmpty(lyyToken)) {
                    lyyToken = getServerToken();
                    Logger.i(TAG, "new Token=" + lyyToken);
                    saveToken(lyyToken, System.currentTimeMillis());
                }

                if (!TextUtils.isEmpty(lyyToken)) {
                    String str_json = "";
                    str_json = make_tt_message_json(content, langfrom, langto, lyyToken);
                    Logger.i(TAG, "tt_message str_json=" + str_json);

                    byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
                    LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
                    LyyMsgByteReply reply = taskStub.sayLyyMsgByte(request);
                    String reply_string = "";
                    String replay_result = "";
                    reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
                    Logger.i(TAG, "GrpcTask... doInBackground reply_string=" + reply_string);
                    replay_result = analysis_tt_reply_json(reply_string);
                    return replay_result;
                } else {
                    return "";
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                Logger.i(TAG, "doInBackground exception=" + e.getMessage());
                Logger.i(TAG, String.format("Failed... : %n%s", sw));
                resultCode = LyyConstants.RESULT_CODE_UNKNOWN;
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                taskChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Logger.i(TAG, "result=" + result);
            Logger.i(TAG, "onPostExecute resultCode=" + resultCode);
            boolean isfail = false;
            if (resultCode.equals(LyyConstants.RESULT_CODE_FAIL)) {
                ToastUtils.showShort(R.string.translat_error_toast);
            } else if (resultCode.equals(LyyConstants.RESULT_CODE_UNKNOWN)) {
                if (retryTime > 0) {
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_CONTENT, content);
                    bundle.putString(KEY_LANGFROM, langfrom);
                    bundle.putString(KEY_LANGTO, langto);
                    bundle.putLong(KEY_MESSAGEID, messageId);
                    Message message = new Message();
                    message.setData(bundle);
                    message.what = RETRY_TRAN;
                    H.sendMessage(message);
                    isfail = true;
                } else {
                    ToastUtils.showShort(R.string.unknown_error_toast);
                }
            }
            if (!isfail) {
                if ((mTranslateFinishedListener != null)
                        && !isRelease) {
                    mTranslateFinishedListener.onTranslateFinish(result, messageId);
                }
            }
        }

        private String getServerToken() {
            Logger.i(TAG, "getServerToken...");
            // GreeterGrpc.GreeterBlockingStub stub = getStub();
            Logger.i(TAG, "getServerToken... taskStub=" + taskStub);
            String str_json = "";
            str_json = make_token_message_json();
            byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
            LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
            LyyMsgByteReply reply = taskStub.sayLyyMsgByte(request);
            String reply_string = "";
            String replay_result = "";
            reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
            Logger.i(TAG, "getServerToken... reply_string=" + reply_string);
            replay_result = analysis_tt_reply_json(reply_string);
            return replay_result;
        }

        public String make_tt_message_json(String content, String langfrom, String langto, String token) {

            String result = "";
            String internalflag = "0";
            try {
                if ((langfrom.equals("en-US")) && (langto.equals("zh-CN"))
                        || (langfrom.equals("zh-CN")) && (langto.equals("en-US"))) {
                    langfrom = langfrom.substring(0, 2);
                    langto = langto.substring(0, 2);
                    internalflag = "1";
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

        public String make_token_message_json() {
            String result = "";
            try {
                JSONObject json_root = new JSONObject();
                json_root.put("processCode", LyyConstants.PROCESS_CODE_TOKEN);
                json_root.put("opflag", LyyConstants.OPFLAG_TT);
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

    private static String KEY_CONTENT = "content";
    private static String KEY_LANGFROM = "langfrom";
    private static String KEY_LANGTO = "langto";
    private static String KEY_MESSAGEID = "messageid";

    private final static int RETRY_TRAN = 110;
    Handler H = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RETRY_TRAN: {
                    Bundle bundle = msg.getData();
                    String content = bundle.getString(KEY_CONTENT);
                    String langfrom = bundle.getString(KEY_LANGFROM);
                    String langto = bundle.getString(KEY_LANGTO);
                    long messageid = bundle.getLong(KEY_MESSAGEID);

                    if (isRelease) {
                        return;
                    }
                    Logger.i(TAG, "translator error retry...");
                    resultCode = LyyConstants.RESULT_CODE_SUCCESS;
                    new GrpcTask(messageid, 0).execute(content, langfrom, langto);
                    break;
                }
            }
        }
    };
}
