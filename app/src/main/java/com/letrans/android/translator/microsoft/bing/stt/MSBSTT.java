package com.letrans.android.translator.microsoft.bing.stt;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.letrans.android.translator.R;
import com.letrans.android.translator.stt.ISTT;
import com.letrans.android.translator.stt.ISTTFinishedListener;
import com.letrans.android.translator.stt.ISTTVoiceLevelListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MSBSTT implements ISTT, ISpeechRecognitionServerEvents {
    private static final String TAG = "RTranslator/MSBSTT";

    private static final int MSG_RESET_VOICE_LEVEL = 1001;
    private static final int MSG_SET_RANDOM_VOICE_LEVEL = 1002;

    private Activity mActivity;

    private int m_waitSeconds = 0;
    private boolean useMicrophone = false;
    private boolean isLongMode = true;

    private DataRecognitionClient dataClient = null;
    private MicrophoneRecognitionClient micClient = null;
    private FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;

    private ISTTFinishedListener mSTTFinishedListener;

    private ISTTVoiceLevelListener mSTTVoiceLevelListener;

    public MSBSTT(Activity activity) {
        mActivity = activity;
    }

    private String mLocal = "zh-cn";

    private String mToken;

    /**
     * Gets the primary subscription key
     */
    public String getPrimaryKey() {
//        return mActivity.getString(R.string.primaryKey);
        return Utils.getEncryptString(mActivity, R.string.primaryKey);
    }

    /**
     * Gets the LUIS application identifier.
     *
     * @return The LUIS application identifier.
     */
    private String getLuisAppId() {
//        mActivity.getString(R.string.luisAppID);
        return Utils.getEncryptString(mActivity, R.string.luisAppID);
    }

    /**
     * Gets the LUIS subscription identifier.
     *
     * @return The LUIS subscription identifier.
     */
    private String getLuisSubscriptionID() {
//        return mActivity.getString(R.string.luisSubscriptionID);
        return Utils.getEncryptString(mActivity, R.string.luisSubscriptionID);
    }

    /**
     * Gets a value indicating whether or not to use the microphone.
     *
     * @return true if [use microphone]; otherwise, false.
     */
    private Boolean getUseMicrophone() {
        return useMicrophone;
    }

    private void setUseMicrophone(boolean useMicrophone) {
        this.useMicrophone = useMicrophone;
    }

    /**
     * Gets the current speech recognition mode.
     *
     * @return The speech recognition mode.
     */
    private SpeechRecognitionMode getMode() {
        if (isLongMode) {
            return SpeechRecognitionMode.LongDictation;
        } else {
            return SpeechRecognitionMode.ShortPhrase;
        }
    }

    /**
     * Gets a value indicating whether LUIS results are desired.
     *
     * @return true if LUIS results are to be returned otherwise, false.
     */
    private Boolean getWantIntent() {
        //TODO
        return false;
    }

    /**
     * Gets the default locale.
     *
     * @return The default locale.
     */
    private String getDefaultLocale() {
        return mLocal;
    }

    /**
     * Gets the short wave file path.
     *
     * @return The short wave file.
     */
    private String getShortWaveFile() {
        return "";
    }

    /**
     * Gets the long wave file path.
     *
     * @return The long wave file.
     */
    private String getLongWaveFile() {
        return "";
    }

    /**
     * Gets the Cognitive Service Authentication Uri.
     *
     * @return The Cognitive Service Authentication Uri.  Empty if the global default is to be used.
     */
    private String getAuthenticationUri() {
        return mActivity.getString(R.string.authenticationUri);
    }

    @Override
    public void start(String wavFilePath) {
        setUseMicrophone(false);
        if (dataClient == null) {
            dataClient = SpeechRecognitionServiceFactory.createDataClient(
                    mActivity,
                    getMode(),
                    getDefaultLocale(),
                    this,
                    getPrimaryKey());

            dataClient.setAuthenticationUri(this.getAuthenticationUri());
        }
        sendAudioByFilePath(wavFilePath);
    }

    @Override
    public void startWithMicrophone(String wavFilePath, String token) {
        Logger.v(TAG, "startWithMicrophone");

        mToken = token;

        setUseMicrophone(true);
        if (micClient == null) {
            micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                    mActivity,
                    getMode(),
                    this.getDefaultLocale(),
                    this,
                    getPrimaryKey());
            micClient.setAuthenticationUri(getAuthenticationUri());
        }
        micClient.startMicAndRecognition();

        if (mSTTVoiceLevelListener != null) {
            H.removeMessages(MSG_RESET_VOICE_LEVEL);
            H.removeMessages(MSG_SET_RANDOM_VOICE_LEVEL);
            H.sendEmptyMessageDelayed(MSG_SET_RANDOM_VOICE_LEVEL, 800);
        }
    }

    @Override
    public void stopWithMicrophone() {
        Logger.v(TAG, "stopWithMicrophone");
        if (micClient != null) {
            micClient.endMicAndRecognition();
        }
    }

    @Override
    public void setLanguageCode(String languageCode) {
        mLocal = languageCode;
    }

    @Override
    public void setSecondLanguageCode(String languageCode) {

    }

    @Override
    public void setSTTFinishedListener(ISTTFinishedListener listener) {
        mSTTFinishedListener = listener;
    }

    @Override
    public void setSTTVoiceLevelListener(ISTTVoiceLevelListener listener) {
        mSTTVoiceLevelListener = listener;
    }

    private void start() {
        Logger.i(TAG, "start");
        m_waitSeconds = getMode() == SpeechRecognitionMode.ShortPhrase ? 20 : 200;

        if (getUseMicrophone()) {
            if (micClient == null) {
                if (getWantIntent()) {
                    micClient =
                            SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                                    mActivity,
                                    getDefaultLocale(),
                                    this,
                                    getPrimaryKey(),
                                    getLuisAppId(),
                                    getLuisSubscriptionID());
                } else {
                    micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                            mActivity,
                            getMode(),
                            this.getDefaultLocale(),
                            this,
                            getPrimaryKey());
                }

                micClient.setAuthenticationUri(getAuthenticationUri());
            }

            micClient.startMicAndRecognition();
        } else {
            if (null == dataClient) {
                if (getWantIntent()) {
                    dataClient =
                            SpeechRecognitionServiceFactory.createDataClientWithIntent(
                                    mActivity,
                                    getDefaultLocale(),
                                    this,
                                    getPrimaryKey(),
                                    getLuisAppId(),
                                    getLuisSubscriptionID());
                } else {
                    dataClient = SpeechRecognitionServiceFactory.createDataClient(
                            mActivity,
                            getMode(),
                            getDefaultLocale(),
                            this,
                            getPrimaryKey());
                }

                dataClient.setAuthenticationUri(this.getAuthenticationUri());
            }

            SendAudioHelper((this.getMode() == SpeechRecognitionMode.ShortPhrase) ? getShortWaveFile() : getLongWaveFile());
        }
    }

    @Override
    public void onPartialResponseReceived(String response) {
        Logger.i(TAG, "onPartialResponseReceived:" + response);
        if (mSTTVoiceLevelListener != null) {
            H.removeMessages(MSG_SET_RANDOM_VOICE_LEVEL);
            H.sendEmptyMessage(MSG_SET_RANDOM_VOICE_LEVEL);
        }
    }

    private Handler H = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RESET_VOICE_LEVEL:
                    if (mSTTVoiceLevelListener != null) {
                        mSTTVoiceLevelListener.updateVoiceLevel(0);
                    }
                    break;
                case MSG_SET_RANDOM_VOICE_LEVEL:
                    if (mSTTVoiceLevelListener != null) {
                        Random rand = new Random();
                        int level = rand.nextInt(4) + 4;
                        mSTTVoiceLevelListener.updateVoiceLevel(level);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void SendAudioHelper(String filename) {
        Logger.i(TAG, "SendAudioHelper-" + filename);
        RecognitionTask doDataReco = new RecognitionTask(dataClient, getMode(), filename, null);
        try {
            doDataReco.execute().get(m_waitSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            doDataReco.cancel(true);
            isReceivedResponse = FinalResponseStatus.Timeout;
        }
    }

    private void sendAudioByFilePath(String path) {
        Logger.i(TAG, "sendAudioByFilePath-" + path);
        RecognitionTask doDataReco = new RecognitionTask(dataClient, getMode(), null, path);
        try {
            doDataReco.execute().get(m_waitSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            Logger.e(TAG, "sendAudioByFilePath-" + e);
            doDataReco.cancel(true);
            isReceivedResponse = FinalResponseStatus.Timeout;
            if (mSTTFinishedListener != null) {
                mSTTFinishedListener.onSTTFinish(isReceivedResponse, "", mToken);
            }
        }
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult response) {

        Logger.i(TAG, "onFinalResponseReceived");

        boolean isFinalDicationMessage = getMode() == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);
        if (null != micClient && getUseMicrophone() && ((getMode() == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage)) {
            // we got the final result, so it we can end the mic reco.  No need to do this
            // for dataReco, since we already called endAudio() on it as soon as we were done
            // sending all the data.
            Logger.i(TAG, "endMicAndRecognition");
            micClient.endMicAndRecognition();
            if (mSTTFinishedListener != null) {
                mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Finished, "", mToken);
            }
        }

        if (isFinalDicationMessage) {
            isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDicationMessage) {
            Logger.i(TAG, "********* Final n-BEST Results *********");
            for (int i = 0; i < response.Results.length; i++) {
                Logger.i(TAG, "[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
                        " Text=\"" + response.Results[i].DisplayText + "\"");
            }
            if (mSTTFinishedListener != null) {
                if (response.Results.length > 0) {
                    mSTTFinishedListener.onSTTFinish(isReceivedResponse, response.Results[0]
                            .DisplayText, mToken);
                } else {
                    mSTTFinishedListener.onSTTFinish(isReceivedResponse, "", mToken);
                }
            }
        }

        if (mSTTVoiceLevelListener != null) {
            H.removeMessages(MSG_SET_RANDOM_VOICE_LEVEL);
            H.sendEmptyMessage(MSG_RESET_VOICE_LEVEL);
        }
    }

    @Override
    public void onIntentReceived(String payload) {
        Logger.i(TAG, "onIntentReceived:" + payload);
    }

    @Override
    public void onError(int errorCode, String response) {
        Logger.i(TAG, "onError: errorCode-" + errorCode + "-" + response);
        if (micClient != null) {
            micClient = null;
        }

        if (dataClient != null) {
            dataClient = null;
        }

        if (mSTTFinishedListener != null) {
            mSTTFinishedListener.onSTTFinish(FinalResponseStatus.Error, "", mToken);
        }
    }

    @Override
    public void onAudioEvent(boolean recording) {
        if (recording) {
            Logger.i(TAG, "Please start speaking.");
        }

        if (!recording) {
            Logger.i(TAG, "Recording end - endMicAndRecognition");
            micClient.endMicAndRecognition();
            //this._startButton.setEnabled(true);
        }
    }

    private class RecognitionTask extends AsyncTask<Void, Void, Void> {
        DataRecognitionClient dataClient;
        SpeechRecognitionMode recoMode;
        String filename;
        String filePath;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename, String filePath) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filename = filename;
            this.filePath = filePath;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Logger.v(TAG, "RecognitionTask - doInBackground");
            try {
                // Note for wave files, we can just send data from the file right to the server.
                // In the case you are not an audio file in wave format, and instead you have just
                // raw data (for example audio coming over bluetooth), then before sending up any
                // audio data, you must first send up an SpeechAudioFormat descriptor to describe
                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
                InputStream fileStream = null;
                if (null != filename) {
                    fileStream = mActivity.getAssets().open(filename);
                } else {
                    fileStream = new FileInputStream(filePath);
                }
                int bytesRead = 0;
                byte[] buffer = new byte[4096];

                do {
                    // Get  Audio data to send into byte buffer.
                    bytesRead = fileStream.read(buffer);

                    if (bytesRead > -1) {
                        // Send of audio data to service.
                        //Logger.v(TAG, "sendAudio - bytesRead:"+bytesRead);
                        dataClient.sendAudio(buffer, bytesRead);
                    }
                } while (bytesRead > 0);

            } catch (Throwable throwable) {
                Logger.e(TAG, throwable.toString());
                throwable.printStackTrace();
            } finally {
                dataClient.endAudio();
            }

            return null;
        }
    }

    @Override
    public void onDestroy() {
        if (micClient != null) {
            micClient = null;
        }

        if (dataClient != null) {
            dataClient = null;
        }

        if (mActivity != null) {
            mActivity = null;
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }
}
