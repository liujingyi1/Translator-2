package com.letrans.android.translator.languagemodel;

public class LanguageItem {
    private int iconRes;
    private String languageName;
    private String languageCode;
    private int profilPictureRes;

    private String stt;
    private String tts;

    public LanguageItem(int iconRes, String code, String languageName, int profilPictureRes,
                        String stt, String tts) {
        this.iconRes = iconRes;
        this.languageCode = code;
        this.languageName = languageName;
        this.profilPictureRes = profilPictureRes;
        this.stt = stt;
        this.tts = tts;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    public String getCode() {
        return languageCode;
    }

    public void setCode(String code) {
        this.languageCode = code;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public int getProfilPictureRes() {
        return profilPictureRes;
    }

    public String getStt() {
        return stt;
    }

    public void setStt(String stt) {
        this.stt = stt;
    }

    public String getTts() {
        return tts;
    }

    public void setTts(String tts) {
        this.tts = tts;
    }

    @Override
    public String toString() {
        return languageCode + ":" + languageName;
    }

}
