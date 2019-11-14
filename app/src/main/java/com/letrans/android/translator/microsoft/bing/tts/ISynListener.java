package com.letrans.android.translator.microsoft.bing.tts;

public interface ISynListener {

    void onResponse(byte[] data, String name, String path);
}
