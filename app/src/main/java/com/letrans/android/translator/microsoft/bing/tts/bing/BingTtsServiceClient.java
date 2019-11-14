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
package com.letrans.android.translator.microsoft.bing.tts.bing;

import android.text.TextUtils;

import com.letrans.android.translator.microsoft.bing.tts.Authentication;
import com.letrans.android.translator.microsoft.bing.tts.ISynListener;
import com.letrans.android.translator.utils.Logger;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class BingTtsServiceClient {
    private static final String LOG_TAG = "RTranslator/BingTtsServiceClient";
    private static final String BING_TTS_TOKEN = "bing_tts_token";
    private static final String BING_TTS_TOKEN_TIME = "bing_tts_token_time";
    private static final String serviceUri = "https://speech.platform.bing.com/synthesize";
    private String outputFormat;
    private Authentication auth;
    private byte[] result;
    private String apiKey;

    public BingTtsServiceClient(String apiKey) {
        outputFormat = "raw-16khz-16bit-mono-pcm";
        auth = new Authentication();
        this.apiKey = apiKey;
    }

    public void speakToAudio(String ssml, String name, String path, ISynListener listener) {
        speakToAudio(ssml, name, path, listener, 0);
    }

    private void speakToAudio(String ssml, String name, String path, ISynListener listener, int retryCount) {
        int code;
        //synchronized(auth) {
            String accessToken = getToken(auth);
            try {
                URL url = new URL(serviceUri);
                HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(15000);
                urlConnection.setRequestMethod("POST");

                urlConnection.setRequestProperty("Content-Type", "application/ssml+xml");
                urlConnection.setRequestProperty("X-MICROSOFT-OutputFormat", outputFormat);
                urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                urlConnection.setRequestProperty("X-Search-AppId", "07D3234E49CE426DAA29772419F436CA");
                urlConnection.setRequestProperty("X-Search-ClientID", "1ECFAE91408841A480F00935DC390960");
                urlConnection.setRequestProperty("User-Agent", "TTSAndroid");
                urlConnection.setRequestProperty("Accept", "*/*");
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
                } else {
                    Logger.d(LOG_TAG, "code " + code);
                    if (code == 401) {
                        auth.saveToken(BING_TTS_TOKEN, "");
                    }
                    if (retryCount == 0) {// retry
                        urlConnection.disconnect();
                        speakToAudio(ssml, name, path, listener, 1);
                        return;
                    }
                }
                Logger.d(LOG_TAG, "doWork end retryCount : " + retryCount);
                if (listener != null) {
                    listener.onResponse(result, name, path);
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                Logger.e(LOG_TAG, "Exception error" + e);
            }
        //}
    }

    public void release() {
        if (auth != null) {
            auth.release();
        }
    }

    private String getToken(Authentication auth) {
        String accessToken = auth.getToken(BING_TTS_TOKEN);
        if (TextUtils.isEmpty(accessToken) || (System.currentTimeMillis()
                - auth.getTokenTime(BING_TTS_TOKEN_TIME) > 9 * 60 * 1000)) {
            accessToken = auth.getBingTTSToken(apiKey);
        }
        return accessToken;
    }
}
