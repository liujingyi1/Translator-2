package com.letrans.android.translator.recorder;

public interface IRecorderListener {
    void onRecordFinished();
    void onSoundLevelChanged(int level);
}
