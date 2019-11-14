package com.letrans.android.translator.roobo.tts;

import com.letrans.android.translator.R;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.roobo.RooboDataModel;
import com.letrans.android.translator.roobo.RooboInitListener;
import com.letrans.android.translator.roobo.RooboManager;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.tts.ITTS;
import com.letrans.android.translator.tts.ITTSListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.ToastUtils;
import com.roobo.vui.api.VUIApi;
import com.roobo.vui.api.tts.RTTSListener;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RooboTTS implements ITTS {

    private static final String TAG = "RTranslator/RooboTTS";
    private ITTSListener listener;
    private static final int STATE_SPEAK_SUCCESS = 0;
    private static final int STATE_SPEAK_ERROR = 1;
    private static final int STATE_SPEAK_START = 5;

    private static final int STATE_SPEAK = 0;
    private static final int STATE_IDLE = 1;
    private static final int STATE_PAUSED = 2;
    private volatile ConcurrentLinkedQueue<TBean> list = new ConcurrentLinkedQueue<>();
    private boolean isInited;
    private boolean isPaused;

    public RooboTTS() {
        RooboManager.getInstance().initRoobo(new RooboInitListener() {
            @Override
            public void onSuccess() {
                Logger.w(TAG, "init success");
                isInited = true;
                if (!setSpeaker)
                VUIApi.getInstance().setSpeaker(code);
            }

            @Override
            public void onFail() {
                isInited = false;
                Logger.w(TAG, "init failed");
                ToastUtils.showLong(R.string.roobo_init_failed);
            }
        });
        list.clear();
    }

    @Override
    public void speak(MessageBean messageBean, String fileName, String filePath, boolean isAuto, boolean isClicked) {
        Logger.w(TAG, "speakTEXTVOICE : " + isClicked + " getText : " + messageBean.getText()
                 + " isInited : " + isInited);
        if (!isInited) return;
        if (isClicked) {
            VUIApi.getInstance().stopSpeak();
            speakClickText(messageBean);
        } else {
            speak(messageBean, isAuto);
        }
    }

    private void speak(MessageBean messageBean, boolean isAuto) {
        Logger.w(TAG, "speakAuto : " + isAuto + " isEmpty : " + list.isEmpty()
                + " getText : " + messageBean.getText() + " isPaused : " + isPaused);
        if (isAuto) {
            if (list.isEmpty()) {
                if (isPaused) {
                    list.offer(new TBean(messageBean, STATE_PAUSED));
                } else {
                    list.offer(new TBean(messageBean, STATE_SPEAK));
                    speakText(messageBean);
                }
            } else {
                TBean pausedBean = getPausedStateBean();
                Logger.d(TAG, "pausedBean : " + pausedBean + " isPaused : " + isPaused);
                if (!isPaused) {
                    if (pausedBean != null) {
                        Logger.d(TAG, "pausedBean : " + pausedBean.getMessageBean().getText() + " isPaused : " + isPaused);
                        list.offer(new TBean(messageBean, STATE_IDLE));
                        pausedBean.setState(STATE_SPEAK);
                        speakText(pausedBean.getMessageBean());
                    } else {
                        if (getSpeakStateBean() == null) {
                            list.offer(new TBean(messageBean, STATE_SPEAK));
                            speakText(messageBean);
                        } else {
                            list.offer(new TBean(messageBean, STATE_IDLE));
                        }
                    }
                } else {
                    if (getSpeakStateBean() == null && pausedBean == null) {
                        list.offer(new TBean(messageBean, STATE_PAUSED));
                    } else {
                        list.offer(new TBean(messageBean, STATE_IDLE));
                    }
                }
            }
        } else {
            list.offer(new TBean(messageBean, STATE_IDLE));
        }
    }

    private void speakClickText(MessageBean messageBean) {
        Logger.w(TAG, "speakClickText isEmpty : " + list.isEmpty() + " getText : " + messageBean.getText());
        if (!list.isEmpty()) {
            boolean isSpeaking = false;
            for (TBean bean : list) {
                if (bean.getState() == STATE_SPEAK) {
                    bean.setState(STATE_IDLE);
                    isSpeaking = true;
                    break;
                }
            }
            TBean clickBean = getBeanByName(messageBean);
            if (clickBean != null) {
                clickBean.setState(STATE_SPEAK);
            } else {
                list.offer(new TBean(messageBean, STATE_SPEAK));
            }
            if (!isSpeaking) {
                speakText(messageBean);
            }
        } else {
            list.offer(new TBean(messageBean, STATE_SPEAK));
            speakText(messageBean);
        }
    }


    private void speakText(MessageBean messageBean) {
        Logger.d(TAG, "speakText1 : " + messageBean.getText());
        VUIApi.getInstance().speak(messageBean.getText(), new RTTSListener() {
            @Override
            public void onSpeakBegin() {
                Logger.d(TAG, "onSpeakBegin : " + messageBean.getText());
                if (listener != null) {
                    listener.onTTSEvent(STATE_SPEAK_START, messageBean);
                }
            }

            @Override
            public void onCompleted() {
                Logger.d(TAG, "onCompleted : " + messageBean.getText());
                if (listener != null) {
                    listener.onTTSEvent(STATE_SPEAK_SUCCESS, messageBean);
                }
                playNext(messageBean);
            }

            @Override
            public void onError(int code) {
                Logger.d(TAG, "onError : " + messageBean.getText());
                if (listener != null) {
                    listener.onTTSEvent(STATE_SPEAK_ERROR, messageBean);
                }
                playNext(messageBean);
            }
        });
    }

    private void playNext(MessageBean messageBean) {

        if (messageBean != null) {// remove text
            Logger.d(TAG, "playNextremovebean : " + messageBean.getText() + " isAuto : " + TStorageManager.getInstance().isAuto());
            if (isPaused) {
                TBean bean = getBeanByName(messageBean);
                if (bean != null) {
                    bean.setState(STATE_PAUSED);
                }
            } else {
                list.remove(getBeanByName(messageBean));
            }
        } else {
            TBean bean = getPausedStateBean();
            if (bean != null) {
                speakText(bean.getMessageBean());
            }
            return;
        }

        if (isPaused) {
            return;
        }

        for (TBean bean : list) {
            Logger.w(TAG, " playNextfor : " + bean.getMessageBean().getText() + " state : " + bean.getState());
        }
        TBean mBean = getSpeakStateBean();

        if (!TStorageManager.getInstance().isAuto()) {
            if (mBean != null) {
                Logger.d(TAG, "playNextspeakText : " + mBean.getMessageBean().getText());
                speakText(mBean.getMessageBean());
            }
        } else {
            if (mBean != null) {
                speakText(mBean.getMessageBean());
            } else {
                TBean bean = list.peek();
                if (bean != null) {
                    bean.setState(STATE_SPEAK);
                    speakText(bean.getMessageBean());
                }
            }
        }
    }

    private TBean getSpeakStateBean() {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getState() == STATE_SPEAK) {
                    return bean;
                }
            }
        }
        return null;
    }

    private TBean getPausedStateBean() {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getState() == STATE_PAUSED) {
                    return bean;
                }
            }
        }
        return null;
    }

    private TBean getBeanByName(MessageBean messageBean) {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getMessageBean().equals(messageBean)) {
                    return bean;
                }
            }
        }
        return null;
    }

    @Override
    public void pause() {
        if (!isInited) return;
        isPaused = true;
        Logger.d(TAG, "pause ");
        VUIApi.getInstance().stopSpeak();
    }

    @Override
    public void resume() {
        if (!isInited) return;
        isPaused = false;
        Logger.d(TAG, "resume ");
        playNext(null);
    }

    @Override
    public void stop() {
        if (!isInited) return;
        VUIApi.getInstance().stopSpeak();
    }

    @Override
    public void release() {
        //VUIApi.getInstance().release();
        listener = null;
        if (isInited) {
            VUIApi.getInstance().stopSpeak();
        }
        list.clear();
    }

    @Override
    public String synthesizeToUri(String text, String fileName, String filePath) {
//        VUIApi.getInstance().getTTSAudioData(text, new RTTSAudioDataListener() {
//            @Override
//            public void onSpeakAudio(byte[] data, boolean isFinish) {
//                //data是音频数据, isFinish true 是结束,最后一块数据data是null
//            }
//        });
        return null;
    }

    private String code = "Allison";
    private boolean setSpeaker = true;

    @Override
    public void setVoice(String language, boolean male, boolean isCloud) {
        isCloud = false;//only offline support
        code = RooboDataModel.getVoiceCode(language, male, isCloud);

        Logger.d(TAG, "setSpeaker code : " + code);
        if (!isInited) {
            setSpeaker = false;
            return;
        }

        VUIApi.getInstance().setSpeaker(code);
    }

    @Override
    public byte[] getSpeak(String text) {
        return null;
    }

    @Override
    public void setTTSEventListener(ITTSListener ittsListener) {
        this.listener = ittsListener;
    }


    private class TBean {
        MessageBean messageBean;
        int state;

        TBean(MessageBean messageBean, int state) {
            this.messageBean = messageBean;
            this.state = state;
        }

        public int getState() {
            return state;
        }

        public MessageBean getMessageBean() {
            return messageBean;
        }

        public void setState(int state) {
            this.state = state;
        }

        public void setMessageBean(MessageBean messageBean) {
            this.messageBean = messageBean;
        }
    }
}
