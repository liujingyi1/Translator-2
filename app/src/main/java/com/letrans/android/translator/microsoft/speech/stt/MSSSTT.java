package com.letrans.android.translator.microsoft.speech.stt;

import android.app.Activity;
import android.text.TextUtils;

import com.letrans.android.translator.R;
import com.letrans.android.translator.stt.ISTT;
import com.letrans.android.translator.stt.ISTTFinishedListener;
import com.letrans.android.translator.stt.ISTTVoiceLevelListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;
import com.microsoft.cognitiveservices.speech.SpeechFactory;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MSSSTT implements ISTT {
    public final static String TAG = "RTranslator/MSSSTT";
    private Activity mActivity;

    SpeechFactory speechFactory;

    private String languageCode = "en-US";
    private boolean continuousListeningStarted = false;
    private boolean isFinalReceived = false;
    private SpeechRecognizer reco = null;
    private ArrayList<String> content = new ArrayList<>();

    private ISTTFinishedListener mSTTFinishedListener;
    private ISTTVoiceLevelListener mSTTVoiceLevelListener;

    private MicrophoneStream microphoneStream;

    private String mToken;

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }

        microphoneStream = new MicrophoneStream();
        if (mSTTVoiceLevelListener != null) {
            microphoneStream.setSTTVoiceLevelListener(mSTTVoiceLevelListener);
        }
        return microphoneStream;
    }

    public MSSSTT(Activity activity) {
        Logger.v(TAG, "MSSSTT NEW");
        mActivity = activity;
        try {
            // Note: required once after app start.
            SpeechFactory.configureNativePlatformBindingWithDefaultCertificate(mActivity.getCacheDir().getAbsolutePath());
            speechFactory = SpeechFactory.fromSubscription(getSpeechSubscriptionKey(), getServiceRegion());

        } catch (Exception ex) {
            Logger.e(TAG, "unexpected " + ex.getMessage());
        }
    }

    public String getSpeechSubscriptionKey() {
//        return mActivity.getString(R.string.speechSubscriptionKey_dongya);
        return Utils.getEncryptString(mActivity, R.string.speechSubscriptionKey_dongya);
    }

    public String getServiceRegion() {
//        mActivity.getString(R.string.serviceRegion_dongya);
        return Utils.getEncryptString(mActivity, R.string.serviceRegion_dongya);
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
        if (microphoneStream != null) {
            microphoneStream.setSTTVoiceLevelListener(mSTTVoiceLevelListener);
        }
    }

    @Override
    public void startWithMicrophone(String wavFilePath, String token) {
        Logger.d(TAG, "startWithMicrophone:" + wavFilePath);

        mToken = token;
        try {
            content.clear();
            reco = speechFactory.createSpeechRecognizerWithStream(createMicrophoneStream(), languageCode);
            reco.IntermediateResultReceived.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                Logger.i(TAG, "Intermediate result received: " + s);
                content.add(s);
                Logger.i(TAG, TextUtils.join(" ", content));
                content.remove(content.size() - 1);
            });

            reco.FinalResultReceived.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                Logger.i(TAG, "Final result received: " + s);
                isFinalReceived = true;
                content.add(s);
                if (mSTTFinishedListener != null) {
                    mSTTFinishedListener.onSTTFinish(FinalResponseStatus.OK, s, mToken);
                    if (!continuousListeningStarted) {
                        mActivity.runOnUiThread(() -> {
                            mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Finished, "", mToken);
                        });
                    }
                }
                Logger.i(TAG, TextUtils.join(" ", content));
            });

            final Future<Void> task = reco.startContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = true;
                isFinalReceived = false;
                Logger.i(TAG, "Started.");
            });
        } catch (Exception ex) {
            Logger.i(TAG, ex.getMessage());
        }
    }

    @Override
    public void stopWithMicrophone() {
        if (continuousListeningStarted) {
            if (reco != null) {
                final Future<Void> task = reco.stopContinuousRecognitionAsync();
                setOnTaskCompletedListener(task, result -> {
                    Logger.i(TAG, "Continuous recognition stopped.");
                    if (isFinalReceived && mSTTFinishedListener != null) {
                        mActivity.runOnUiThread(() -> {
                            mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Finished, "", mToken);
                        });
                    }
                    continuousListeningStarted = false;
                });
            } else {
                continuousListeningStarted = false;
            }

            return;
        }
    }

    @Override
    public void setLanguageCode(String languageCode) {
        Logger.v(TAG, "setLanguageCode:" + languageCode);
        this.languageCode = languageCode;
    }

    @Override
    public void setSecondLanguageCode(String languageCode) {

    }

    @Override
    public void onDestroy() {
//        speechFactory.close();
        microphoneStream.close();
        microphoneStream = null;
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;

    static {
        s_executorService = Executors.newCachedThreadPool();
    }
}
