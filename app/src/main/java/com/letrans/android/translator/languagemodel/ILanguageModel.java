package com.letrans.android.translator.languagemodel;

import java.util.List;

public interface ILanguageModel {
    List<LanguageItem> getLanguageList();

    LanguageItem getLanguageItem(String code);
}
