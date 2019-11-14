package com.letrans.android.translator.tts;

import com.letrans.android.translator.database.beans.MessageBean;

public interface ITTS {
    void speak(MessageBean messageBean, String fileName, String filePath, boolean isAuto, boolean isClicked);
    void pause();
    void resume();
    void stop();
    void release();
    String synthesizeToUri(String text, String fileName, String filePath);
    void setVoice(String language, boolean male, boolean isCloudServiceVoice);
    byte[] getSpeak(String text);
    void setTTSEventListener(ITTSListener ittsListener);
}
