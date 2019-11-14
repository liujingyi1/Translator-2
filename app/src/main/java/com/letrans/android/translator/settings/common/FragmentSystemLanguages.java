package com.letrans.android.translator.settings.common;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.ItemDivider;
import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentSystemLanguages extends Fragment implements SystemLanguageAdapter.OnItemClickListener {
    private RecyclerView mListView;
    private List<LanguageItemInfo> mList = new ArrayList<>();
    private String[] strEntryValues;

    private SystemLanguageAdapter mSystemLanguageAdapter;

    private static final String TAG = "FragmentSystemLanguages";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setting_system_languages, container, false);
        mListView = (RecyclerView) view.findViewById(R.id.id_system_languages_list);
        initData();
        configSystemLanguages(view);
        return view;
    }


    private void configSystemLanguages(View view) {
        mListView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new ItemDivider(getContext(), Utils.dpToPx(58)));
        mSystemLanguageAdapter = new SystemLanguageAdapter(getContext(), mList);
        mSystemLanguageAdapter.setOnItemClickListener(this);
        mListView.setAdapter(mSystemLanguageAdapter);
        Log.d(TAG, "configSystemLanguages: mList = " + mList.size());
    }

    @Override
    public void onItemClick(int position) {
        Logger.d(TAG, "onItemClick: position = " + position + ", strEntryValues = " + strEntryValues[position]);
        if (mList.size() == 0) return;
        for (int i = 0; i < mList.size(); i++) {
            if (i == position) {
                Logger.d(TAG, "saveTypeData position : " + position);
                mList.get(i).setChecked(true);
            } else {
                mList.get(i).setChecked(false);
            }
        }
        mSystemLanguageAdapter.setData(mList);
        mSystemLanguageAdapter.notifyItemRangeChanged(0, mList.size());

        String[] selectLocale = strEntryValues[position].split("_");
        updateLocales(selectLocale[0], selectLocale[1]);
    }

    public static void updateLocales(String language, String country) {
        SystemProxy.getInstance().updateLocales(language, country);
    }

    public void initData() {
        strEntryValues = getActivity().getResources().getStringArray(R.array.entries_values_system_languages);
        String[] localEntryValues = new String[]{"CN", "US"};
        String[] localEntryLabel = getContext().getResources().getStringArray(R.array.entries_system_languages);
        mList.clear();
        for (int i = 0; i < localEntryValues.length; i++) {
            if (localEntryValues[i].equalsIgnoreCase(Locale.getDefault().getCountry())) {
                mList.add(new LanguageItemInfo(localEntryLabel[i], localEntryValues[i], true));
            } else {
                mList.add(new LanguageItemInfo(localEntryLabel[i], localEntryValues[i], false));
            }
        }
    }

    public class LanguageItemInfo {
        private String label;
        private String value;
        private boolean checked;

        LanguageItemInfo(String label, String value, boolean checked) {
            this.checked = checked;
            this.value = value;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }

        public boolean isChecked() {
            return this.checked;
        }
    }
}
