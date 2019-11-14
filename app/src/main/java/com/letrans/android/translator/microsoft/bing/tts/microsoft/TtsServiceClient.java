//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Speech-TTS
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.letrans.android.translator.microsoft.bing.tts.microsoft;

import android.content.Context;
import android.text.TextUtils;
import com.letrans.android.translator.microsoft.bing.tts.Authentication;
import com.letrans.android.translator.microsoft.bing.tts.ISynListener;
import com.letrans.android.translator.microsoft.bing.tts.MSDataModel;
import com.letrans.android.translator.microsoft.bing.tts.TtsBean;
import com.letrans.android.translator.utils.Logger;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;



import javax.net.ssl.HttpsURLConnection;

public class TtsServiceClient {
    private static final String LOG_TAG = "RTranslator/TtsServiceClient";
    private static String contentType = "application/ssml+xml";
    private static final String MIC_TTS_TOKEN = "mic_tts_token";
    private static final String MIC_TTS_TOKEN_TIME = "mic_tts_token_time";
    private String outputFormat;
    private Authentication auth;
    TtsBean bean;

    TtsServiceClient(Context context) {
        outputFormat = "raw-16khz-16bit-mono-pcm";
        auth = new Authentication();
        bean = MSDataModel.getTtsBean();
    }

    public void release() {
        if (auth != null) {
            auth.release();
        }
    }

    public void speakToAudio(String ssml, String name, String path, ISynListener listener) {
        speakToAudio(ssml, name, path, listener, 0);
    }

    private void speakToAudio(String ssml, String name, String path, ISynListener listener, int retryCount) {
        Logger.d(LOG_TAG, "dowork start : " + name);
        boolean isSuccess = false;
        byte[] result = null;
        int code;
        HttpsURLConnection urlConnection = null;
        //synchronized(auth) {
            String accessToken = getToken(auth);
            try {
                URL url = new URL(bean.getBaseUri());
                urlConnection = (HttpsURLConnection)url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(15000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", contentType);
                urlConnection.setRequestProperty("X-MICROSOFT-OutputFormat", outputFormat);
                urlConnection.setRequestProperty("Authorization", accessToken);
                urlConnection.setRequestProperty("Host", bean.getUri_host());
                urlConnection.setRequestProperty("User-Agent", "TTSAndroid");
                urlConnection.setRequestProperty("Accept", "*/*");
                urlConnection.setRequestProperty("Connection", "Keep-Alive");
                byte[] ssmlBytes = ssml.getBytes();
                urlConnection.setRequestProperty("content-length", String.valueOf(ssmlBytes.length));
                urlConnection.connect();
                urlConnection.getOutputStream().write(ssmlBytes);
                code = urlConnection.getResponseCode();
                if (code == 200) {
                    InputStream in = urlConnection.getInputStream();
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    byte[] bytes = new byte[1024];
                    int ret = in.read(bytes);
                    while (ret > 0) {
                        bout.write(bytes, 0, ret);
                        ret = in.read(bytes);
                    }
                    result = bout.toByteArray();
                    isSuccess = true;
                } else {
                    if (code == 401) {
                        auth.saveToken(MIC_TTS_TOKEN, "");
                    }
                    Logger.e(LOG_TAG, "code " + code);
                }
                Logger.d(LOG_TAG, "doWork end retryCount : " + retryCount + " isSuccess : " + isSuccess);
            } catch (Exception e) {
                Logger.e(LOG_TAG, "Exception error " + e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (retryCount == 0 && !isSuccess) {// retry
                    Logger.d(LOG_TAG, "doWork retry");
                    speakToAudio(ssml, name, path, listener, 1);
                } else {
                    if (listener != null) {
                        listener.onResponse(result, name, path);
                    }
                }
            }
        //}
    }

    private String getToken(Authentication auth) {
        String accessToken = auth.getToken(MIC_TTS_TOKEN);
        if (TextUtils.isEmpty(accessToken) || (System.currentTimeMillis()
                - auth.getTokenTime(MIC_TTS_TOKEN_TIME) > 9 * 60 * 1000)) {
            accessToken = auth.getMicTTSToken(bean.getApiKey(), bean.getHost());
        }
        return accessToken;
    }
}
