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

import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.microsoft.bing.tts.AbstractSynthesizer;
import com.letrans.android.translator.microsoft.bing.tts.AudioOutputFormat;
import com.letrans.android.translator.microsoft.bing.tts.ISynListener;
import com.letrans.android.translator.microsoft.bing.tts.Voice;
import com.letrans.android.translator.microsoft.bing.tts.XmlDom;
import com.letrans.android.translator.tts.ITTSListener;
import com.letrans.android.translator.utils.Logger;


public class BingSynthesizer extends AbstractSynthesizer {

    private static final String TAG = "RTranslator/BingSynthesizer";
    private Voice serviceVoice;
    private Voice localVoice;
    public String audioOutputFormat = AudioOutputFormat.Raw16Khz16BitMonoPcm;
    ITTSListener listener;
    private BingTtsServiceClient mBingTtsServiceClient;
    private ServiceStrategy serviceStrategy;


    public BingSynthesizer(String apiKey) {
        serviceVoice = new Voice("en-US");
        localVoice = null;
        serviceStrategy = ServiceStrategy.AlwaysService;
        mBingTtsServiceClient = new BingTtsServiceClient(apiKey);
    }

    public void setTTSEventListener(ITTSListener mListener) {
        this.listener = mListener;
    }

    public enum ServiceStrategy {
        AlwaysService//, WiFiOnly, WiFi3G4GOnly, NoService
    }


    public void setVoice(Voice serviceVoice, Voice localVoice) {
        isReleased = false;
        this.serviceVoice = serviceVoice;
        this.localVoice = localVoice;
        new Thread(new Runnable() {
            @Override
            public void run() {
                firstSpeakVoice(serviceVoice);
            }
        }).start();
    }

    public void setServiceStrategy(ServiceStrategy eServiceStrategy) {
        serviceStrategy = eServiceStrategy;
    }

    public byte[] getSpeakBytes(String text) {
        return null;
    }

    public void speakToAudio(MessageBean messageBean, String name, String path, boolean isAuto, boolean isClicked) {
        Logger.d(TAG, "speakToAudio name : " + name + " isAuto : " + isAuto + " text : " + messageBean.getText());
        String ssml = XmlDom.createDom(serviceVoice.lang, String.valueOf(serviceVoice.gender), serviceVoice.voiceName, "+100.00%", messageBean.getText());
        if (serviceStrategy == ServiceStrategy.AlwaysService) {
            if (isClicked) {
                speakClickedAudio(messageBean, name, path);
            } else {
                speak(ssml, messageBean, name, path, isAuto);
            }
        }
    }

    public void speakText(MessageBean messageBean, String ssml, String name, String path) {
        mBingTtsServiceClient.speakToAudio(ssml, name, path, new ISynListener() {
            @Override
            public void onResponse(byte[] data, String name, String path) {
                Logger.d(TAG, "speakToAudio onResponse name : " + name);
                onTTSResponse(messageBean, data, name, path);
            }
        });
    }

    public void speakErrorTexts(MessageBean messageBean, String name, String path) {
        String ssml = XmlDom.createDom(serviceVoice.lang, String.valueOf(serviceVoice.gender), serviceVoice.voiceName, "+100.00%", messageBean.getText());
        speakErrorTextClick(ssml, messageBean, name, path);
    }

    public String synthesizeToUri(String text, String fileName, String filePath) {
//        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filePath + "/" + fileName + ".wav";
//        String ssml = XmlDom.createDom(serviceVoice.lang, String.valueOf(serviceVoice.gender), serviceVoice.voiceName, text);
//        if (serviceStrategy == ServiceStrategy.AlwaysService) {
//            mBingTtsServiceClient.speakToAudio(ssml, fileName, filePath, new ISynListener() {
//                @Override
//                public void onResponse(byte[] data, String name, String path) {
//                    if (isReleased) return;
//                    String file = FileUtils.writeBytesToFile(data, name, path);
//                    if (TextUtils.isEmpty(file)) {
//                        if (listener != null) {
//                            listener.onTTSEvent(MSConstants.STATE_SYN_ERROR, "write file error");
//                        }
//                    } else if (listener != null) {
//                        listener.onTTSEvent(MSConstants.STATE_SYN_SUCCESS, file);
//                    }
//                }
//            });
//        }
//        return path;
        return null;
    }

    private void firstSpeakVoice(Voice serviceVoice) {
        Logger.d(TAG, "firstSpeakVoice");
        String ssml = XmlDom.createDom(serviceVoice.lang, String.valueOf(serviceVoice.gender), serviceVoice.voiceName, "+0.00%", "speed speak");
        if (mBingTtsServiceClient != null) {
            mBingTtsServiceClient.speakToAudio(ssml, null, null, null);
            Logger.d(TAG, "firstSpeakVoice end: ");
        }
    }

    public void release() {
        isReleased = true;
        if (mBingTtsServiceClient != null) {
            mBingTtsServiceClient.release();
            mBingTtsServiceClient = null;
        }
        if (listener != null) {
            listener = null;
        }
        releaseAll();
    }

}
