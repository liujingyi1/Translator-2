package com.letrans.android.translator.stt;

public interface ISTTFinishedListener {
    void onSTTFinish(ISTT.FinalResponseStatus status, String text, String token);
}
