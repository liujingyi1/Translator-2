package com.letrans.android.translator.translate;

public interface ITranslate {
    void setTranslateFinishedListener(ITranslateFinishedListener listener);

    void doTranslate(String content, String fromLanguage, String targetLanguage, long token);

    void release();
}
