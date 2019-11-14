package com.letrans.android.translator.languagemodel;

import com.letrans.android.translator.mvpbase.BasePresenter;

import java.util.List;

public interface ILanguagePresenter extends BasePresenter {
    List<LanguageItem> getLanguageItemList();
    LanguageItem getLanguageItem(String code);
    void setPrimaryLanguage(String language);
    void setSecondaryLanguage(String language);
}
