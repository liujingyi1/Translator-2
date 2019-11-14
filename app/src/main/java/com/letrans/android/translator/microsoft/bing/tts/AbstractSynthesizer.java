package com.letrans.android.translator.microsoft.bing.tts;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.text.TextUtils;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.media.MediaManager;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.tts.ITTSListener;
import com.letrans.android.translator.utils.FileUtils;
import com.letrans.android.translator.utils.Logger;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public abstract class AbstractSynthesizer {

    private static final String TAG = "RTranslator/SynthesizerAbstract";

    private ExecutorService executor = Executors.newCachedThreadPool();
    private final ConcurrentLinkedQueue<TBean> list = new ConcurrentLinkedQueue<>();
    private static final int STATE_SPEAKING = 0;
    private static final int STATE_SPEAK = 1;
    private static final int STATE_SYNING = 2;
    private static final int STATE_SYNED = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PAUSING = 5;
    private FutureTask playTask;
    public boolean isReleased;
    private AudioTrack audioTrack;
    private int bufferSizeInBytes;
    public ITTSListener listener;
    private boolean isPaused;
    private MessageBean playBean;


    public AbstractSynthesizer() {
        list.clear();
    }

    public void speakClickedAudio(MessageBean messageBean, String name, String path) {
        if (!list.isEmpty()) {//click the text included in list
            TBean mBean = getListBeanByName(name);
            Logger.d(TAG, "isClicked mBean : " + mBean + " messageBean : " + messageBean.getText());
            if (mBean != null) {
                Logger.d(TAG, "isClicked mBean : " + mBean.getState());
                if (mBean.getState() == STATE_SYNED || mBean.getState() == STATE_SPEAKING
                        || mBean.getState() == STATE_PAUSING) {
                    mBean.getTask().cancel(true);//remove task
                    playSoundByUri(messageBean, name, path);
                } else {
                    TBean bean = getSpeakBean();
                    if (bean != null) {
                        if (bean.getState() == STATE_SPEAKING) {
                            bean.getTask().cancel(true);//stop speaking task
                            bean.setState(STATE_SYNED);
                            //list.poll();//do not poll the speaking voice
                        } else if (bean.getState() == STATE_SPEAK) {// top speak task to syn task
                            bean.setState(STATE_SYNING);
                        }
                    }
                    mBean.setState(STATE_SPEAK);
                }
            } else {
                playSoundByUri(messageBean, name, path);
            }
        } else {
            playSoundByUri(messageBean, name, path);
        }
    }

    public void speak(String ssml, MessageBean messageBean, String name, String path, boolean isAuto) {
        Logger.d(TAG, "speakname : " + name + " list : " + list.size() + " path : " + path
                + " isAuto : " + isAuto + " isPaused : " + isPaused);
        synchronized (list) {
            FutureTask mFutureTask = new FutureTask<>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    speakText(messageBean, ssml, name, path);
                    return null;
                }
            });
            if (isAuto) {
                if (isPaused) {
                    if (list.isEmpty() || getPausedBean() == null) {
                        Logger.d(TAG, "speak  isEmpty() ");
                        list.add(new TBean(messageBean, name, path, STATE_PAUSED, mFutureTask));//set paused state
                    } else {
                        list.add(new TBean(messageBean, name, path, STATE_SYNING, mFutureTask));//already have speaking wav
                    }
                } else {
                    if (getSpeakBean() != null) {//error
                        Logger.d(TAG, "speak  isHeadSpeakState() : " + isHeadSpeakState());
                        list.add(new TBean(messageBean, name, path, STATE_SYNING, mFutureTask));
                    } else if (getPausedBean() != null) {
                        list.add(new TBean(messageBean, name, path, STATE_SYNING, mFutureTask));
                        TBean pausedBean = getPausedBean();
                        Logger.d(TAG, "speak  pausedBean : " + pausedBean.messageBean.getText()
                            + " state : " + pausedBean.getState());
                        if (pausedBean.getState() == STATE_PAUSED) {
                            //pausedBean.setState(STATE_SPEAK);
                        } else {
                            playSoundByUri(pausedBean.getMessageBean(), pausedBean.url, pausedBean.path);
                        }
                    } else {
                        Logger.d(TAG, "speak STATE_SYNING name : " + name);
                        list.add(new TBean(messageBean, name, path, STATE_SPEAK, mFutureTask));//empty list, add to first speaking
                    }
                }
            } else {
                list.add(new TBean(messageBean, name, path, STATE_SYNING, mFutureTask));
            }
            executor.submit(mFutureTask);
        }
    }

    public void onTTSResponse(MessageBean messageBean, byte[] data, String name, String path) {
        Logger.e(TAG, "onTTSResponse name : " + name);
        if (isSpeakState(name)) {
            Logger.e(TAG, "onResponse111");
            playSound(messageBean, data, name, path);
        } else if (isPausedState(name)) {
            Logger.e(TAG, "onResponse2222");
            FileUtils.writeBytesToFile(data, name, path);
            setListState(name, STATE_PAUSING);
        } else {
            Logger.e(TAG, "onResponse3333");
            writeTofile(data, name, path);
        }
    }

    public void speakErrorTextClick(String ssml, MessageBean messageBean, String name, String path) {
        if (!list.isEmpty()) {//click the text included in list
            TBean mBean = getListBeanByName(name);
            Logger.d(TAG, "speakErrorTextClick mBean : " + mBean + " messageBean : " + messageBean.getText());
            TBean bean = getSpeakBean();
            if (bean != null) {
                if (bean.getState() == STATE_SPEAKING) {
                    bean.getTask().cancel(true);//stop speaking task
                    bean.setState(STATE_SYNED);
                } else if (bean.getState() == STATE_SPEAK) {// top speak task to syn task
                    bean.setState(STATE_SYNING);
                }
            }
            if (mBean != null) {
                mBean.setState(STATE_SPEAK);
            } else {
                speak(ssml, messageBean, name, path, true);
            }
        } else {
            speak(ssml, messageBean, name, path, true);
        }
    }

    public abstract void speakText(MessageBean messageBean, String ssml, String name, String path);

    public abstract void speakErrorTexts(MessageBean messageBean, String name, String path);

    private boolean isHeadSpeakState() {
        TBean bean = list.peek();
        return bean != null && (bean.state == STATE_SPEAKING || bean.state == STATE_SPEAK);
    }


    private TBean getSpeakBean() {
        for (TBean bean : list) {
            if (bean.getState() == STATE_SPEAK || bean.getState() == STATE_SPEAKING) {
                return bean;
            }
        }
        return null;
    }

    private TBean getPausedBean() {
        for (TBean bean : list) {
            if (bean.getState() == STATE_PAUSED || bean.getState() == STATE_PAUSING) {
                return bean;
            }
        }
        return null;
    }

    private TBean getListBeanByName(String name) {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getUrl().equals(name)) {// change to speaking state
                    return bean;
                }
            }
        }
        return null;
    }

    private boolean isSpeakState(String name) {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getUrl().equals(name) && bean.getState() == STATE_SPEAK) {// change to speaking state
                    bean.setState(STATE_SPEAKING);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPausedState(String name) {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getUrl().equals(name) && bean.getState() == STATE_PAUSED) {// change to speaking state
                    return true;
                }
            }
        }
        return false;
    }

    private void setListState(String name, int state) {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getUrl().equals(name)) {// change to speaking state
                    bean.setState(state);
                    return;
                }
            }
        }
    }

    private void removeBeanByName(String name) {
        if (!list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getUrl().equals(name)) {//remove the all of same wavs
                    Logger.d(TAG, "removeBeanByName name : " + name);
                    list.remove(bean);
                    //return;
                }
            }
        }
    }

    private void playSound(final MessageBean messageBean, final byte[] sound, final String name, final String path) {
        Logger.d(TAG, "playSound name : " + name + " isReleased : " + isReleased + " sound : " + sound);
        if (isReleased) return;
        if (sound == null || sound.length == 0) {
            Logger.e("RTranslator/Synthesizer11","playSound STATE_SPEAK_ERROR " + messageBean.getText()
                    + " positon : " + messageBean.getPositionInList());
            listener.onTTSEvent(MSConstants.STATE_SPEAK_ERROR, messageBean);
            playNext(name);
            return;
        }
        String file = FileUtils.writeBytesToFile(sound, name, path);
        Logger.e("RTranslator/Synthesizer11","playSound " + messageBean.getText() + " positon : " + messageBean.getPositionInList());
        listener.onTTSEvent(MSConstants.STATE_SPEAK_START, messageBean);
        final int SAMPLE_RATE = 16000;
        bufferSizeInBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.play();
            audioTrack.write(sound, 0, sound.length);
            audioTrack.stop();
            audioTrack.release();
        }
        if (listener != null) {
            listener.onTTSEvent(MSConstants.STATE_SPEAK_SUCCESS, messageBean);
        }
        playNext(name);
    }

    private void playNext(String name) {
        Logger.d(TAG, "playNext name : " + name + " isAuto : " + TStorageManager.getInstance().isAuto());
        for (TBean b : list) {
            Logger.d(TAG, "list : " + b.url + " state : " + b.state);
        }
        playBean = null;
        if (TextUtils.isEmpty(name)) {// play paused voice
            if (TStorageManager.getInstance().isAuto() && !isPaused) {
                TBean pausedBean = getPausedBean();
                if (pausedBean != null) {
                    if (pausedBean.getState() == STATE_PAUSING) {
                        pausedBean.setState(STATE_SPEAKING);
                        playSoundByUri(pausedBean.getMessageBean(), pausedBean.url, pausedBean.path);
                    } else {
                        pausedBean.setState(STATE_SPEAK);
                    }
                }
            }
            return;
        }
        if (isPaused) {
            return;
        }
        removeBeanByName(name);
        if (!TStorageManager.getInstance().isAuto()) {
            return;
        }
        //TBean mBean = list.peekFirst();
        TBean mBean = list.peek();
        Logger.d(TAG, "playNext mBean : " + mBean);
        if (mBean != null) {
            Logger.d(TAG, "playNext mBean : " + mBean.url  + " state : " + mBean.state);
            if (mBean.getState() == STATE_SYNED || mBean.getState() == STATE_SPEAKING) {
                mBean.setState(STATE_SPEAKING);
                playSoundByUri(mBean.getMessageBean(), mBean.url, mBean.path);
            } else {
                mBean.setState(STATE_SPEAK);
            }
        }
    }

    private void playSoundByUri(MessageBean messageBean, String name, String path) {
        if (playTask != null && !playTask.isDone()) {
            playTask.cancel(true);
        }
        int index = path.lastIndexOf("/");
        if (index > 0) {
            path = path.substring(index + 1);
        }
        String uri = AppContext.SOUND_ABSOLUTE_FILE_DIR + "/" + path + "/" + name + ".wav";
        Logger.e("RTranslator/Synthesizer11","playSoundByUri " + messageBean.getText() + " positon : " + messageBean.getPositionInList());
        File file = new File(uri);
        if (!file.exists()) {//TTS request failed, try again
            speakErrorTexts(messageBean, name, path);
            return;
        }
        listener.onTTSEvent(MSConstants.STATE_SPEAK_START, messageBean);
        playBean = messageBean;
        Logger.d(TAG, "playSoundByUri uri : " + uri);
        playTask = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                MediaManager.playSound(uri, new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (listener != null) {
                            listener.onTTSEvent(MSConstants.STATE_SPEAK_SUCCESS, messageBean);
                        }
                        playNext(name);
                    }
                }, new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Logger.e(TAG, "playSoundByUri STATE_SPEAK_ERROR : ");
                        if (listener != null) {
                            listener.onTTSEvent(MSConstants.STATE_SPEAK_ERROR, messageBean);
                        }
                        return false;
                    }
                });
                return null;
            }
        });

        Thread mThread = new Thread(playTask);
        mThread.start();
        Logger.d(TAG, "playSoundByUri mThread start: ");
    }

    private void writeTofile(byte[] data, String name, String path) {
        Logger.d(TAG, "writeTofile name : " + name);
        FileUtils.writeBytesToFile(data, name, path);
        setListState(name, STATE_SYNED);
    }


    private void stop() {
        Logger.d(TAG, "stop");
        if (list != null && !list.isEmpty()) {
            for (TBean bean : list) {
                if (bean.getState() == STATE_SPEAKING) {
                    bean.getTask().cancel(true);
                    list.remove(bean);
                } else if (bean.getState() == STATE_SPEAK) {
                    bean.setState(STATE_SYNING);
                }
            }
        }
    }

    public void releaseAll() {
        isReleased = true;
        stop();
        stopSound();
        if (listener != null) {
            listener = null;
        }
        if (executor != null) {
            executor.shutdownNow();
        }

        if (playTask != null && !playTask.isDone()) {
            playTask.cancel(true);
        }
        if (list != null) {
            list.clear();
        }
    }

    public void resume() {
        isPaused = false;
        Logger.d(TAG, "resume");
        playNext(null);
    }

    public void pause() {
        Logger.d(TAG, "pause isEmpty : " + list.isEmpty());
        isPaused = true;
        TBean bean = getSpeakBean();
        if (bean != null) {
            if (bean.getState() == STATE_SPEAKING) {
                if (playTask != null) {
                    bean.getTask().cancel(true);
                }
                bean.setState(STATE_PAUSING);
                //list.remove(bean);
                if (listener != null) {
                    listener.onTTSEvent(MSConstants.STATE_SPEAK_SUCCESS, bean.getMessageBean());
                }
            } else {
                bean.setState(STATE_PAUSED);
            }
        } else {
            if (playBean != null) {
                MediaManager.pause();
                MediaManager.release();
                if (listener != null) {
                    listener.onTTSEvent(MSConstants.STATE_SPEAK_SUCCESS, playBean);
                }
                playBean = null;
            }
        }
        stopSound();
    }

    public void stopSound() {
        //stop();
        if (playTask != null) {
            playTask.cancel(true);
        }
        try {
            if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.pause();
                audioTrack.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class TBean {
        String url;
        String path;
        int state;
        FutureTask task;
        MessageBean messageBean;

        TBean(MessageBean messageBean, String url, String path, int state, FutureTask task) {
            this.state = state;
            this.url = url;
            this.task = task;
            this.path = path;
            this.messageBean = messageBean;
        }

        public int getState() {
            return state;
        }

        public String getUrl() {
            return url;
        }

        public FutureTask getTask() {
            return task;
        }

        public String getPath() {
            return path;
        }

        public MessageBean getMessageBean() {
            return messageBean;
        }

        public void setState(int state) {
            this.state = state;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setTask(FutureTask task) {
            this.task = task;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setMessageBean(MessageBean messageBean) {
            this.messageBean = messageBean;
        }
    }
}
