package com.letrans.android.translator.google.stt;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;

import com.google.protobuf.ByteString;
import com.letrans.android.translator.R;
import com.letrans.android.translator.lyy.LyyConstants;
import com.letrans.android.translator.lyy.LyyUtils;
import com.letrans.android.translator.recorder.AudioRecordFunc;
import com.letrans.android.translator.recorder.IRecorderListener;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.stt.ISTT;
import com.letrans.android.translator.stt.ISTTFinishedListener;
import com.letrans.android.translator.stt.ISTTVoiceLevelListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.ToastUtils;
import com.letrans.android.translator.utils.Utils;
import com.lyy.android.lyymsgbyte.GreeterGrpc;
import com.lyy.android.lyymsgbyte.LyyMsgByteReply;
import com.lyy.android.lyymsgbyte.LyyMsgByteRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GGSTT implements ISTT {

    private static final String TAG = "RTranslator/GGSTT";

    private static final String ADDRES = "18.224.42.95";
    private static final int PORT = 50052;

    private String LYY_SP_FILE = "lyy.stt.config";
    private String LYY_KEY_TOKEN = "token";
    private String LYY_KEY_TIME = "time";

    private static long TOKEN_INVALID_TIME = 1000 * 60 * 3 - 10000;

    private long tokenTime = 0;
//    ManagedChannel channel;

    Context mContext;
    private String mLanguageCode = "";
    String resultCode;

    private AudioRecordFunc mAudioRecord;
    private IRecorderListener mRecorderListener = new IRecorderListener() {
        @Override
        public void onRecordFinished() {
            Logger.d(TAG, "onRecordFinished:" + mAudioRecord.getSoundPath());
            File file = new File(mAudioRecord.getSoundPath());
            if (file.exists() && !file.isDirectory()) {
                try {
                    byte[] content = new byte[(int) (file.length())];
                    FileInputStream inputStream = new FileInputStream(file);
                    inputStream.read(content);
                    byte[] tmpt = Base64.encode(content, Base64.DEFAULT);
                    Logger.d(TAG, "GrpcSTTask:" + mLanguageCode);
                    resultCode = LyyConstants.RESULT_CODE_SUCCESS;
                    new GrpcSTTask(mToken).executeOnExecutor(Executors.newCachedThreadPool(), new String(tmpt), mLanguageCode);
                } catch (Exception e) {

                }
            }
        }

        @Override
        public void onSoundLevelChanged(int level) {
            level = Utils.convertLevel(level);
            if (mSTTVoiceLevelListener != null) {
                mSTTVoiceLevelListener.updateVoiceLevel(level);
            }
        }
    };

    private ISTTVoiceLevelListener mSTTVoiceLevelListener;
    private ISTTFinishedListener mSTTFinishedListener;

    private String mToken;

    public GGSTT(Activity activity) {
        mContext = activity;
        mAudioRecord = AudioRecordFunc.getInstance();
        mAudioRecord.setOnRecorderFinishedListener(mRecorderListener);
    }

    @Override
    public void start(String wavFilePath) {

    }

    @Override
    public void setSTTFinishedListener(ISTTFinishedListener listener) {
        mSTTFinishedListener = listener;
    }

    @Override
    public void setSTTVoiceLevelListener(ISTTVoiceLevelListener listener) {
        mSTTVoiceLevelListener = listener;
    }

    @Override
    public void startWithMicrophone(String wavFilePath, String token) {
        Logger.d(TAG, "startWithMicrophone:" + wavFilePath);
        mToken = token;
        String ss[] = wavFilePath.split("/");
        StringBuffer basepath = new StringBuffer();
        for (int i = 0; i < ss.length - 1; i++) {
            if (i == 0) {
                basepath.append(ss[i]);
            } else {
                basepath.append("/").append(ss[i]);
            }
        }

        Logger.d(TAG, "startWithMicrophone:" + basepath.toString());

        String sss[] = ss[ss.length - 1].split("\\.");
        Logger.d(TAG, "startWithMicrophone:" + sss[0]);
        mAudioRecord.prepare(basepath.toString(), sss[0]);
        mAudioRecord.startRecord();
    }

    @Override
    public void stopWithMicrophone() {
        mAudioRecord.stopRecord();
    }

    @Override
    public void setLanguageCode(String languageCode) {
        mLanguageCode = languageCode;
    }

    @Override
    public void setSecondLanguageCode(String languageCode) {

    }

    @Override
    public void onDestroy() {
        mAudioRecord.onDestroy();
//        if (channel != null) {
//            try {
//                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//            }
//        }
        mSTTFinishedListener = null;
        mSTTVoiceLevelListener = null;
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    private class GrpcSTTask extends AsyncTask<String, Void, String> {
        private ManagedChannel taskChannel;
        private GreeterGrpc.GreeterBlockingStub taskStub;
        private String token;

        private GrpcSTTask(String token) {
            this.token = token;
        }

        @Override
        protected String doInBackground(String... params) {
            String content = params[0];
            String language = params[1];
            try {
                taskChannel = ManagedChannelBuilder.forAddress(ADDRES, PORT).usePlaintext().build();
                taskStub = GreeterGrpc.newBlockingStub(taskChannel);

                String lyyToken = getToken();
                long current = System.currentTimeMillis();
                Logger.i(TAG, "lyytoken=" + lyyToken);
                if (((current - tokenTime) > TOKEN_INVALID_TIME) || TextUtils.isEmpty(lyyToken)) {
                    lyyToken = getServerToken();
                    Logger.i(TAG, "new Token=" + lyyToken);
                    saveToken(lyyToken, System.currentTimeMillis());
                }

                if (!TextUtils.isEmpty(lyyToken)) {
                    String str_json = "";
                    str_json = make_st_message_json(content, language, lyyToken);
                    //Logger.i(TAG, "request str_json=" + str_json);

                    byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
                    LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
                    LyyMsgByteReply reply = taskStub.sayLyyMsgByte(request);
                    String reply_string = "";
                    String replay_result = "";
                    reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
                    Logger.i(TAG, "reply_string=" + reply_string);
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
            Logger.i(TAG, "onPostExecute resultCode="+resultCode);
            if (resultCode.equals(LyyConstants.RESULT_CODE_FAIL)) {
                ToastUtils.showShort(R.string.translat_error_toast);
            } else if (resultCode.equals(LyyConstants.RESULT_CODE_UNKNOWN)) {
                ToastUtils.showShort(R.string.unknown_error_toast);
            }
            if (mSTTFinishedListener != null) {
                mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Finished, result, token);
            }
        }

        private String getServerToken() {
            String str_json = "";
            str_json = make_token_message_json();
            Logger.i(TAG, "getServerToken str_json=" + str_json);
            byte[] byte_message = LyyUtils.compressStringToGZip(str_json);
            LyyMsgByteRequest request = LyyMsgByteRequest.newBuilder().setQuerykey(ByteString.copyFrom(byte_message)).build();
            LyyMsgByteReply reply = taskStub.sayLyyMsgByte(request);
            String reply_string = "";
            String replay_result = "";
            reply_string = LyyUtils.decompressGZipToString(reply.getReplymessage().toByteArray());
            replay_result = analysis_tt_reply_json(reply_string);
            return replay_result;
        }

        public String make_st_message_json(String content, String language, String token) {
            String result = "";
            String internalflag = "1";
            Logger.i(TAG, "make_st_message_json language=" + language+" token="+token);
            try {
                JSONObject json_root = new JSONObject();
                json_root.put("processCode", LyyConstants.PROCESS_CODE_ST);
                JSONArray jsonarray_items = new JSONArray();
                JSONObject json_item = new JSONObject();
                json_item.put("token", token);
                json_item.put("internalflag", internalflag);
                json_item.put("langfrom", language);
                json_item.put("langto", language);
                json_item.put("inparam", content);
                json_item.put("ispflag", LyyConstants.ST_OPFLAG_GOOGLE);
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
                json_root.put("opflag", LyyConstants.TOKEN_TYPE_ST);
                json_root.put("deviceType", LyyConstants.DEVICE_TYPE_TABLET);
                json_root.put("deviceID", TStorageManager.getInstance().getDeviceId());
                result = json_root.toString();
            } catch (Exception e) {
                resultCode = LyyConstants.RESULT_CODE_UNKNOWN;
            }
            return result;
        }

        public String analysis_tt_reply_json(String strIn) {
            Logger.i(TAG, "st reply strIn=" + strIn);
            String result = "";
            if (strIn.length() > 0) {
                try {
                    JSONObject jsonObject = new JSONObject(strIn);
                    String flag = "";
                    flag = jsonObject.getString("respCode");
                    if (flag.equals("0")) {
                        result = jsonObject.getString("result");
                        resultCode = LyyConstants.RESULT_CODE_SUCCESS;
                    } else {
                        result = "";
                        resultCode = LyyConstants.RESULT_CODE_FAIL;
                    }
                } catch (Exception e) {
                    resultCode = LyyConstants.RESULT_CODE_UNKNOWN;
                }
            }
            return result;
        }
    }

    private String getToken() {
        SharedPreferences sp = mContext.getSharedPreferences(LYY_SP_FILE, Context.MODE_PRIVATE);
        String token = sp.getString(LYY_KEY_TOKEN, "");
        tokenTime = sp.getLong(LYY_KEY_TIME, 0);
        return token;
    }

    private void saveToken(String token, long time) {
        SharedPreferences sp = mContext.getSharedPreferences(LYY_SP_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(LYY_KEY_TIME, time);
        editor.putString(LYY_KEY_TOKEN, token);
        editor.commit();
    }

//    private GreeterGrpc.GreeterBlockingStub getStub() {
//        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
//            channel = ManagedChannelBuilder.forAddress(ADDRES, PORT).usePlaintext().build();
//        }
//
//        return GreeterGrpc.newBlockingStub(channel);
//    }
}
