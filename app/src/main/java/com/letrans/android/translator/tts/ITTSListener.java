package com.letrans.android.translator.tts;

import com.letrans.android.translator.database.beans.MessageBean;

public interface ITTSListener {
    void onTTSEvent(int status, MessageBean messageBean);
}
