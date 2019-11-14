package com.letrans.android.translator.composemessage;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.database.DbConstants.MessageType;
import com.letrans.android.translator.database.beans.MemberBean;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.database.beans.ThreadsBean;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.languagemodel.LanguageItem;
import com.letrans.android.translator.media.MediaManager;
import com.letrans.android.translator.microsoft.translator.MSTranslate;
import com.letrans.android.translator.mpush.HttpClientListener;
import com.letrans.android.translator.mpush.HttpProxyCallback;
import com.letrans.android.translator.mpush.ReceiveMessageBean;
import com.letrans.android.translator.mpush.TMPushService;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.mpush.domain.ServerResponse;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.stt.ISTT;
import com.letrans.android.translator.stt.ISTTFinishedListener;
import com.letrans.android.translator.stt.ISTTVoiceLevelListener;
import com.letrans.android.translator.stt.STTFactory;
import com.letrans.android.translator.translate.ITranslate;
import com.letrans.android.translator.translate.ITranslateFinishedListener;
import com.letrans.android.translator.tts.ITTS;
import com.letrans.android.translator.tts.ITTSListener;
import com.letrans.android.translator.tts.TTSManager;
import com.letrans.android.translator.utils.FileUtils;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;
import com.mpush.api.Constants;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

public class ComposeMessagePresenterImpl implements ComposeMessageContact.IComposeMessagePresenter {

    private static final String TAG = "RTranslator/ComposeMessagePresenterImpl";

    private static final int HANGUP_SELF = 0;
    private static final int HANGUP_OTHER = 1;

    private ComposeMessageContact.IComposeMessageView mComposeMessageView;

    private HashMap<String, StringBuilder> sttString = new HashMap<>();

    private UserBean mUserInfo;

    private ISTT mSTT;

    private ITTS mTTS;

    private TMPushService mTMPushService;

    private String mLanguage;
    private String mSecondLanguage;

    private ITranslate mTranslate;

    private TTSManager mTTSManager;

    private boolean isAuto;

    private MessageBean mSendMessage;
    private MessageBean mReceiveMessage;
    private long mThreadId;

    private HttpClientListener httpClientListener;

    private int mThreadType;

    private String mServerThreadId = AppContext.LOCAL_SERVER_THREAD_ID;

    private String mStorageFolderName;

    public ComposeMessagePresenterImpl(ComposeMessageContact.IComposeMessageView
                                               view, Activity activity, int threadType) {
        mComposeMessageView = view;

        mUserInfo = TStorageManager.getInstance().getUser();
        isAuto = TStorageManager.getInstance().isAuto();
        mLanguage = mUserInfo.getLanguage();

        mThreadType = threadType;

        initSTT(activity);
        initTTS(activity);
        initTranslate();
        Intent bindIntent = new Intent(TranslatorApp.getAppContext(), TMPushService.class);
        TranslatorApp.getAppContext().bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTMPushService = ((TMPushService.TMPushBinder) service).getService();

            mTMPushService.resumePush();
            mTMPushService.bindUser(TStorageManager.getInstance().getDeviceId());
            Logger.i(TAG, "onServiceConnected:" + mTMPushService);

            initMPush();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.i(TAG, "onServiceDisconnected:" + name);
        }
    };

    @Override
    public void initSTT(Activity activity) {
        if ("en-US".equals(mUserInfo.getLanguage())) {
            mSTT = STTFactory.getSTT(activity, TStorageManager.getInstance().getSTTType());
        } else {
            mSTT = STTFactory.getSTT(activity, getLanguageItem().getStt());
        }
        mSTT.setLanguageCode(mUserInfo.getLanguage());
        mSTT.setSTTFinishedListener(mSTTFinishedListener);
        mSTT.setSTTVoiceLevelListener(mSTTVoiceLevelListener);
    }

    @Override
    public void initTTS(Activity activity) {
        mTTSManager = TTSManager.getInstance(activity);
        mTTS = mTTSManager.getTTS(activity, mLanguage, true, getLanguageItem().getTts());
        mTTS.setVoice(mLanguage, false, true);
        mTTS.setTTSEventListener(mTTSEventListener);
    }

    @Override
    public void playSound(MessageBean messageBean) {
        mTTS.stop();

        String filePath = messageBean.getUrl();
        Logger.e(TAG, "filepath : " + filePath);
        if (filePath != null) {
            int index = filePath.lastIndexOf("/");
            String name = filePath.substring(index + 1, filePath.length() - 4);
            String path = filePath.substring(0, index);
            String file = path.substring(path.lastIndexOf("/") + 1);
            if (!TStorageManager.getInstance().saveSound()) {
                file = "Temp";
            }
            mTTS.speak(messageBean, name, file, true, true);
        }

    }

    @Override
    public void playSoundByText(MessageBean messageBean) {
        mTTS.speak(messageBean, null, null, false, true);
    }

    @Override
    public void hangUpSelf() {
        //不需要返回把callback设置为null
        ServerApi.getInstance()
                .hangUp(mUserInfo.getDeviceId(), mTMPushService.getPairUser(), HANGUP_SELF, new ServerApi.ServerCallback() {
                    @Override
                    public void call(String result, int code) {
                        //ServerApi.SR_REQUEST_SUCCESS_CODE 请求成功
                        //SServerApi.SR_HUNG_UP_ERROR_C ODE 挂断失败
                        // -1 服务器错误
                        Logger.i(TAG, "hangUpSelf:" + result);
                    }
                });
    }

    @Override
    public void hangUpOther() {
        //不需要返回把callback设置为null
        ServerApi.getInstance()
                .hangUp(mUserInfo.getDeviceId(), mTMPushService.getPairUser(), HANGUP_OTHER, new ServerApi.ServerCallback() {
                    @Override
                    public void call(String result, int code) {
                        //ServerApi.SR_REQUEST_SUCCESS_CODE 请求成功
                        //SServerApi.SR_HUNG_UP_ERROR_CODE 挂断失败
                        // -1 服务器错误
                        if (code == ServerApi.SR_REQUEST_SUCCESS_CODE) {
                            MemberBean tempMemberBean = TStorageManager.getInstance().getMember(TStorageManager.getInstance().getPairedId());
                            tempMemberBean.setLanguage("");
                            tempMemberBean.update();

                            mComposeMessageView.updateOtherIcon("");

                            closeThread();
                        }
                    }
                });
    }

    @Override
    public void initMPush() {

        mTMPushService.setTCallback(new TMPushService.TCallback() {
            @Override
            public void updateUIByMessageBean(MessageBean messageBean, int type, byte[] content) {
                mReceiveMessage = messageBean;
                if (type == ReceiveMessageBean.TYPE_TEXT) {
                    Logger.v(TAG, "updateUIByMessageBean:" + mServerThreadId + "|token:" + mReceiveMessage.getId());
                    mTranslate.doTranslate(new String(content, Constants.UTF_8), mReceiveMessage
                            .getLanguage(), mUserInfo
                            .getLanguage(), mReceiveMessage.getId());

                    if (!mReceiveMessage.getLanguage().equals(mSecondLanguage)) {
                        if (mComposeMessageView != null) {
                            mComposeMessageView.updateOtherIcon(mReceiveMessage.getLanguage());
                        }
                    }
                }
            }
        });

        mTMPushService.setHttpCallBack(new HttpProxyCallback() {
            @Override
            public void onResponse(ServerResponse httpResponse) {
                Logger.i(TAG, "MPushApi - onResponse");
            }

            @Override
            public void onCancelled() {
                Logger.i(TAG, "MPushApi - onCancelled");
            }
        });
        httpClientListener = new HttpClientListener() {
            @Override
            public void onEnter(String language) {

                if (TStorageManager.getInstance().getThreadBean(mServerThreadId) == null) {
                    createThread(mThreadType);
                    addMemberToThread(TStorageManager.getInstance().getPairedId(), mServerThreadId);
                    mThreadId = TStorageManager.getInstance().getThreadBean(mServerThreadId).getId();
                    mStorageFolderName = TStorageManager.getInstance().getThreadBean(mServerThreadId)
                            .getStorageFolderName();
                }

                MemberBean tempMemberBean = TStorageManager.getInstance().getMember(TStorageManager.getInstance().getPairedId());
                tempMemberBean.setLanguage(language);
                tempMemberBean.update();

                if (mComposeMessageView != null) {
                    mComposeMessageView.updateOtherIcon(language);
                }
            }

            @Override
            public void onUnPaired() {
                mComposeMessageView.closeView();
            }

            @Override
            public void onHangUp(int type) {
                switch (type) {
                    case HANGUP_SELF:
                        // 对方要是挂断了，我们这里也把Thread结束了，方便后续查看
                        closeThread();
                        mComposeMessageView.updateOtherIcon(null);
                        break;
                    case HANGUP_OTHER:
                        closeThread();
                        mComposeMessageView.finishByHungup();
                        break;
                }
            }
        };
        mTMPushService.addHttpClientListener(httpClientListener);
    }

    @Override
    public ThreadsBean createThread(int threadType) {
        Logger.i(TAG, "createThread at:" + Utils.getCurrentTime());
        return TStorageManager.getInstance().createThread(mServerThreadId, Utils.getCurrentTime(), threadType);
    }

    private void closeThread() {
        Logger.i(TAG, "closeThread!");
        if (TStorageManager.getInstance() != null) {
            if (getMessageBeanListSize(mServerThreadId) == 0) {
                TStorageManager.getInstance().deleteThread(mServerThreadId);
            } else {
                TStorageManager.getInstance().closeThread(mServerThreadId);
            }
        }

        if (sttString != null) {
            sttString.clear();
        }
    }

    public int getMessageBeanListSize(String serverThreadId) {
        if (TStorageManager.getInstance().getThreadBean(serverThreadId) == null) {
            return 0;
        }

        if (TStorageManager.getInstance().getThreadBean(serverThreadId).getMessageBeans() == null) {
            return 0;
        }

        return TStorageManager.getInstance().getThreadBean(serverThreadId).getMessageBeans().size();
    }

    private void addMemberToThread(String deviceId, String threadId) {
        if (TStorageManager.getInstance().getMember(deviceId) != null) {
            TStorageManager.getInstance().getThreadBean(threadId).addMember(TStorageManager.getInstance().getMember(deviceId));
        } else {
            TStorageManager.getInstance().getThreadBean(threadId).addMember(TStorageManager.getInstance().createMember(deviceId, ""));
        }
    }

    @Override
    public void initTranslate() {
        //mTranslate = new YDTranslate();
//        mTranslate = new LyyTranslate();
        mTranslate = new MSTranslate();
        mTranslate.setTranslateFinishedListener(mTranslateFinishedListener);
    }

    @Override
    public List<MessageBean> getMessageBeanList(String serverThreadId) {

        if (mServerThreadId != serverThreadId) {
            mServerThreadId = serverThreadId;
        }

        if (TStorageManager.getInstance().getThreadBean(serverThreadId) == null) {
            createThread(mThreadType);
            addMemberToThread(TStorageManager.getInstance().getPairedId(), serverThreadId);
            mThreadId = TStorageManager.getInstance().getThreadBean(serverThreadId).getId();
            mStorageFolderName = TStorageManager.getInstance().getThreadBean(mServerThreadId)
                    .getStorageFolderName();
        }

        return TStorageManager.getInstance().getThreadBean(serverThreadId).getMessageBeans();
    }

    @Override
    public boolean isGuest() {
        return TStorageManager.getInstance().isGuest();
    }

    @Override
    public void stopPlaySound() {
        if (mTTS != null) {
            mTTS.pause();
        }
    }

    @Override
    public void resumePlaySound() {
        if (mTTS != null) {
            mTTS.resume();
        }
    }

    @Override
    public void setAuto() {
        isAuto = !isAuto;
        TStorageManager.getInstance().setAuto(isAuto);
    }

    @Override
    public boolean getAuto() {
        return isAuto;
    }

    @Override
    public void setTextSize(int size) {
        TStorageManager.getInstance().setTextSize(size);
    }

    @Override
    public int getTextSize() {
        return TStorageManager.getInstance().getTextSize();
    }


    @Override
    public HashMap<String, MemberBean> getMembers(String threadId) {
        return TStorageManager.getInstance().getThreadBean(threadId).getMembers();
    }

    @Override
    public void recordStart() {
        String fileName = String.valueOf(Utils.getCurrentTime());
        sttString.put(fileName, new StringBuilder());
        Logger.v(TAG, "recordStart, token=" + fileName);
        String wavFilePath = TStorageManager.getInstance().saveSound() ? TStorageManager.getInstance().getStorageFolderName(mServerThreadId) + "/" + fileName + ".wav" : AppContext.SOUND_ABSOLUTE_FILE_DIR + "/Temp/" + fileName + ".wav";
        mSTT.startWithMicrophone(wavFilePath, fileName);
    }

    @Override
    public void recordFinish() {
        mSTT.stopWithMicrophone();
    }

    @Override
    public void onResume() {
        mSTT.onResume();
        MediaManager.resume();
        if (mTMPushService != null) {
            mTMPushService.resumePush();
        }
    }

    @Override
    public void onPause() {
        mSTT.onPause();
        MediaManager.pause();
    }

    @Override
    public void searchOtherLanguage() {
        ServerApi.getInstance().getLanguage(TStorageManager.getInstance().getPairedId(), new ServerApi.ServerCallback() {
            @Override
            public void call(String result, int code) {
                Logger.v(TAG, "searchOtherLanguage, result=" + result + ", code=" + code);

                mComposeMessageView.updateOtherIcon(result);

                MemberBean tempMemberBean = TStorageManager.getInstance().getMember(TStorageManager.getInstance().getPairedId());
                tempMemberBean.setLanguage(result);
                tempMemberBean.update();
            }
        });
    }

    @Override
    public LanguageItem getLanguageItem() {
        return getLanguageItem(TStorageManager.getInstance().getUser().getLanguage());
    }

    @Override
    public LanguageItem getPairedLanguageItem() {
        if (TStorageManager.getInstance().getPairedId().equals(TStorageManager.getInstance().getDeviceId())) {
            return getLanguageItem(TStorageManager.getInstance().getUser().getLanguage());
        } else {
            return getLanguageItem(TStorageManager.getInstance().getMember(TStorageManager.getInstance().getPairedId())
                    .getLanguage());
        }
    }

    @Override
    public void setSecondLanguage(String language) {
        mSTT.setSecondLanguageCode(language);
        mSecondLanguage = language;
    }

    @Override
    public List<LanguageItem> getLanguageItemList() {
        return TStorageManager.getInstance().getLanguageItemList();
    }

    @Override
    public LanguageItem getLanguageItem(String code) {
        Logger.v(TAG, "getLanguageItem:" + code);
        return TStorageManager.getInstance().getLanguageItem(code);
    }

    @Override
    public void setPrimaryLanguage(String code) {
        UserBean user = TStorageManager.getInstance().getUser();
        user.setLanguage(code);
    }

    @Override
    public void setSecondaryLanguage(String language) {
        // TODO for Handheld device
    }

    @Override
    public void onDestroy() {

        if (mTranslate != null) {
            mTranslate.setTranslateFinishedListener(null);
            mTranslate.release();
        }

        if (!TStorageManager.getInstance().saveSound()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    FileUtils.deleteFiles(new File(AppContext.SOUND_ABSOLUTE_FILE_DIR + "/Temp"));
                }
            });
            thread.start();
        }

        if (mTMPushService != null) {
            mTMPushService.removeHttpClientListener(httpClientListener);
            mTMPushService.setHttpCallBack(null);
            mTMPushService.setTCallback(null);
        }
        TranslatorApp.getAppContext().unbindService(mServiceConnection);

        httpClientListener = null;
        mTMPushService = null;

        closeThread();

        MediaManager.release();

        if (mSTT != null) {
            mSTT.setSTTFinishedListener(null);
            mSTT.onDestroy();
        }

        mTTS.release();
        if (mTTSManager != null) {
            mTTSManager.releaseAll();
        }

        if (mComposeMessageView != null) {
            mComposeMessageView = null;
        }

        if (sttString != null) {
            sttString = null;
        }

        System.gc();
    }

    ISTTFinishedListener mSTTFinishedListener = new ISTTFinishedListener() {
        @Override
        public void onSTTFinish(ISTT.FinalResponseStatus status, String text, String token) {
            Logger.v(TAG, "onSTTFinish, status=" + status + ", text=" + text + ", token=" + token);

            try {

                if (sttString.get(token) == null) {
                    sttString.put(token, new StringBuilder());
                }

                if (ISTT.FinalResponseStatus.OK == status) {
                    sttString.get(token).append(text);
                } else if (ISTT.FinalResponseStatus.NotReceived == status) {
                    sttString.get(token).append(text);
                } else if (ISTT.FinalResponseStatus.Timeout == status) {
                    sttString.get(token).append("");
                } else if (ISTT.FinalResponseStatus.Finished == status) {
                    sttString.get(token).append(text);
                    if (TextUtils.isEmpty(sttString.get(token).toString())) {
                        if (mComposeMessageView != null) {
                            mComposeMessageView.callbackSTT(null);
                        }
                    } else {
                        if (sttString.get(token).toString().endsWith("，")) {
                            sttString.get(token).replace(sttString.get(token).length() - 1, sttString.get(token).length(), "。");
                        }
                        mSendMessage = new MessageBean.Builder(mThreadId, -1).setDate(Utils.getCurrentTime("yyyy-MM-dd HH:mm:ss"))
                                .setType(MessageType.TYPE_TEXT).setLanguage(mUserInfo.getLanguage
                                        ()).setText(sttString.get(token).toString())
                                .build();
                        mTMPushService.sendPush(mSendMessage);
                        if (mComposeMessageView != null) {
                            mComposeMessageView.callbackSTT(mSendMessage);
                        }
                        if (TStorageManager.getInstance().saveText()) {
                            StringBuffer stringBuffer = new StringBuffer();
                            stringBuffer.append("Send at ");
                            stringBuffer.append(mSendMessage.getDate());
                            stringBuffer.append(":");
                            stringBuffer.append(sttString.get(token).toString());
                            FileUtils.addTxtToFileBuffered(getMessagesFile(), stringBuffer.toString());
                        }
                    }
                    sttString.remove(token);
                } else if (ISTT.FinalResponseStatus.Error == status) {
                    if (mComposeMessageView != null) {
                        mComposeMessageView.callbackSTT(null);
                    }

                    sttString.remove(token);
                }
            } catch (NullPointerException e) {
                Logger.e(TAG, "mSTTFinishedListener error:" + e.getMessage());
            }
        }
    };

    ISTTVoiceLevelListener mSTTVoiceLevelListener = new ISTTVoiceLevelListener() {
        @Override
        public void updateVoiceLevel(int level) {
            if (mComposeMessageView != null) {
                mComposeMessageView.updateVoiceLevel(level);
            }
        }
    };

    ITTSListener mTTSEventListener = new ITTSListener() {
        @Override
        public void onTTSEvent(int status, MessageBean messageBean) {
            Logger.v(TAG, "onTTSEvent-" + status);
            if (mComposeMessageView != null) {
                Logger.v(TAG, "onTTSEvent-" + messageBean.getPositionInList());
                mComposeMessageView.callbackTTS(status, messageBean);
            }
        }
    };

    ITranslateFinishedListener mTranslateFinishedListener = new ITranslateFinishedListener() {
        @Override
        public void onTranslateFinish(String result, long token) {
            Logger.d(TAG, "onTranslateFinish mTranslateFinishedListener message language: " +
                    mReceiveMessage.getLanguage());

            try {

                if (mReceiveMessage.getId() != token) {
                    Logger.d(TAG, "mTranslateFinishedListener message change from " +
                            mReceiveMessage.getId() + " to " + token);
                    mReceiveMessage = TStorageManager.getInstance().getMessageBeanById(token);
                    Logger.d(TAG, "new to " + mReceiveMessage.getId());
                }
                Logger.d(TAG, "mTranslateFinishedListener message language: " + mReceiveMessage.getLanguage());

                mReceiveMessage.setText(result);
                String fileName = String.valueOf(Utils.getCurrentTime());

                String filePath = TStorageManager.getInstance().saveSound() ? AppContext.SOUND_FILE_DIR + "/"
                        + mStorageFolderName :
                        AppContext.SOUND_FILE_DIR + "/Temp";

                Logger.v(TAG, "mTranslateFinishedListener save sound filePath-" + filePath);
                mTTS.speak(mReceiveMessage, fileName, filePath, isAuto, false);
                mReceiveMessage.setType(MessageType.TYPE_SOUND_TEXT);
                if (TStorageManager.getInstance().saveSound()) {
                    mReceiveMessage.setUrl(TStorageManager.getInstance().getStorageFolderName(mServerThreadId) + "/" + fileName + ".wav");
                } else {
                    mReceiveMessage.setUrl(AppContext.SOUND_ABSOLUTE_FILE_DIR + "/Temp/" + fileName + ".wav");
                }

                if (mComposeMessageView != null) {
                    mComposeMessageView.receiveMessage(mReceiveMessage);
                }

                mReceiveMessage.update();

                if (TStorageManager.getInstance().saveText()) {
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("Receive at ");
                    stringBuffer.append(mReceiveMessage.getDate());
                    stringBuffer.append(":");
                    stringBuffer.append(result);
                    FileUtils.addTxtToFileBuffered(getMessagesFile(), stringBuffer.toString());
                }
            } catch (NullPointerException e) {
                Logger.e(TAG, "mTranslateFinishedListener error:" + e.getMessage());
            }
        }
    };

    private File getMessagesFile() {
        FileUtils.makeDir(TStorageManager.getInstance().getStorageFolderName(mServerThreadId));
        return new File(TStorageManager.getInstance().getStorageFolderName(mServerThreadId) +
                "/Messages" +
                ".txt");
    }
}
