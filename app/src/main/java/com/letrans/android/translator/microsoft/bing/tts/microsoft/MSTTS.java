package com.letrans.android.translator.microsoft.bing.tts.microsoft;

import android.content.Context;

import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.microsoft.bing.tts.MSDataModel;
import com.letrans.android.translator.microsoft.bing.tts.Voice;
import com.letrans.android.translator.tts.ITTS;
import com.letrans.android.translator.tts.ITTSListener;
import com.letrans.android.translator.utils.Logger;

public class MSTTS implements ITTS {

    private static final String TAG = "RTranslator/MTTTS";
    private Synthesizer syn;
    private ITTSListener mListener;
    private MSDataModel model;

    public MSTTS(Context context) {
        if (syn == null) {
            model = new MSDataModel(context);
            syn = new Synthesizer(context);
            syn.setServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
        }
    }


    @Override
    public void setVoice(String language, boolean male, boolean isServiceVoice) {
        if (model == null) return;
        String voiceName =  model.getVoiceName(language, male);
        Logger.d(TAG," setVoice language : " + language + " voiceName : " + voiceName);
        Voice v = new Voice(language, voiceName, male ? Voice.Gender.Male : Voice.Gender.Female, isServiceVoice);
        if (syn != null) {
            syn.setVoice(v, null);
        }
    }

    @Override
    public void speak(MessageBean messageBean, String name, String path, boolean isAuto, boolean isClicked) {
        if (syn != null) {
            syn.speakToAudio(messageBean, name, path, isAuto, isClicked);
        }
    }

    @Override
    public void pause() {
        if (syn != null) {
            syn.pause();
        }
    }

    @Override
    public void resume() {
        if (syn != null) {
            syn.resume();
        }
    }

    @Override
    public void stop() {
        if (syn != null) {
            syn.stopSound();
        }
    }

    @Override
    public void release() {
        if (syn != null) {
            syn.release();
        }
        if (syn != null) {
            syn.stopSound();
        }
        syn = null;
    }

    @Override
    public String synthesizeToUri(String text, String fileName, String filePath) {
        if (syn != null) {
            return syn.synthesizeToUri(text, fileName, filePath);
        }
        return null;
    }

    @Override
    public byte[] getSpeak(String text) {
        if (syn != null) {
            return syn.getSpeakBytes(text);
        }
        return null;
    }

    @Override
    public void setTTSEventListener(ITTSListener ittsListener) {
        this.mListener = ittsListener;
        if (syn != null) {
            syn.setTTSEventListener(mListener);
        }
    }

}
