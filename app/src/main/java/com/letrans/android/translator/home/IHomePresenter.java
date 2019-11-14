package com.letrans.android.translator.home;

import android.content.Context;

import com.letrans.android.translator.languagemodel.ILanguagePresenter;

public interface IHomePresenter extends ILanguagePresenter {
    String getPairedId();

    boolean isNetworkConnected(Context context);

    void isBothWorker(int index);

    void gotoComposemessage(int index);
}
