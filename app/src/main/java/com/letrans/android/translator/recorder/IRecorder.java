package com.letrans.android.translator.recorder;

public interface IRecorder {

    // ERROE code
    public final static int SUCCESS = 1000;
    public final static int E_NOSDCARD = 1001;
    public final static int E_STATE_RECODING = 1002;
    public final static int E_NOT_RREPARE = 1003;
    // private final static int E_UNKOWN = 1004;

    public void prepare(String basePath, String fileName);
    public int startRecord();
    public void stopRecord();
    public void cancel();
    public String getSoundPath();
    public void setOnRecorderFinishedListener(IRecorderListener listener);
    void onDestroy();
}
