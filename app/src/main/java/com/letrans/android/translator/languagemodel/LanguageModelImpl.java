package com.letrans.android.translator.languagemodel;

import android.content.Context;
import android.content.res.TypedArray;

import com.letrans.android.translator.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LanguageModelImpl implements ILanguageModel {
    private static final String TAG = "RTranslator/LanguageModelImpl";

    private Context mContext;
    private List<LanguageItem> mLanguageItems = new ArrayList<>();
    private HashMap<String, LanguageItem> mCodeMap = new HashMap<>();


    public LanguageModelImpl(Context context) {
        mContext = context;
        initLanguageItems();
    }

    @Override
    public List<LanguageItem> getLanguageList() {
        return mLanguageItems;
    }

    @Override
    public LanguageItem getLanguageItem(String code) {
        return mCodeMap.get(code);
    }

    private void initLanguageItems() {
        mLanguageItems.clear();
        TypedArray iconArray = mContext.getResources().obtainTypedArray(R.array.language_icon_array);
        String[] codeArray = mContext.getResources().getStringArray(R.array.language_code_array);
        String[] languageArray = mContext.getResources().getStringArray(R.array.language_name_array);
        TypedArray profileArray = mContext.getResources().obtainTypedArray(R.array.language_profile_picture_array);
        String[] sttArray = mContext.getResources().getStringArray(R.array.stt_array);
        String[] ttsArray = mContext.getResources().getStringArray(R.array.tts_array);

        for (int i = 0; i < codeArray.length; i++) {
            LanguageItem item = new LanguageItem(iconArray.getResourceId(i, 0), codeArray[i],
                    languageArray[i], profileArray.getResourceId(i, 0), sttArray[i], ttsArray[i]);
            if ("vi".equals(codeArray[i]) || "ta".equals(codeArray[i])
                    || "bg-BG".equals(codeArray[i]) || "hr-HR".equals(codeArray[i]) || "sl-SI".equals(codeArray[i])) {
                continue;
            }
            mLanguageItems.add(item);
            mCodeMap.put(item.getCode(), item);
        }
        iconArray.recycle();
        profileArray.recycle();
    }
}
