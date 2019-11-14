package com.letrans.android.translator.settings.common;

import android.app.Fragment;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.SystemProxy;

import java.util.HashMap;

public class ScreenTimeout extends Fragment {
    /**
     * If there is no setting in the provider, use this.
     */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 120000;

    private SystemProxy mSystemProxy;

    private RadioGroup mRadioGroup;

    private HashMap<Integer, Long> mIdToValue = new HashMap<>();
    private HashMap<Long, Integer> mValueToId = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSystemProxy = SystemProxy.getInstance();
        mIdToValue.put(R.id.screen_time_2_mins_btn, 120000l);
        mIdToValue.put(R.id.screen_time_5_mins_btn, 300000l);
        mIdToValue.put(R.id.screen_time_10_mins_btn, 600000l);
        mIdToValue.put(R.id.screen_time_30_mins_btn, 1800000l);
        mValueToId.put(120000l, R.id.screen_time_2_mins_btn);
        mValueToId.put(300000l, R.id.screen_time_5_mins_btn);
        mValueToId.put(600000l, R.id.screen_time_10_mins_btn);
        mValueToId.put(1800000l, R.id.screen_time_30_mins_btn);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setting_screen_time, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mRadioGroup = (RadioGroup) view.findViewById(R.id.screen_time_container);
        long value = mSystemProxy.getLong(SystemProxy.TABLE_SYSTEM,
                Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        if (value < FALLBACK_SCREEN_TIMEOUT_VALUE) {
            value = FALLBACK_SCREEN_TIMEOUT_VALUE;
        }
        mRadioGroup.check(mValueToId.get(value));
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.screen_time_2_mins_btn:
                        mSystemProxy.putLong(SystemProxy.TABLE_SYSTEM,
                                Settings.System.SCREEN_OFF_TIMEOUT, mIdToValue.get(R.id.screen_time_2_mins_btn));
                        break;
                    case R.id.screen_time_5_mins_btn:
                        mSystemProxy.putLong(SystemProxy.TABLE_SYSTEM,
                                Settings.System.SCREEN_OFF_TIMEOUT, mIdToValue.get(R.id.screen_time_5_mins_btn));
                        break;
                    case R.id.screen_time_10_mins_btn:
                        mSystemProxy.putLong(SystemProxy.TABLE_SYSTEM,
                                Settings.System.SCREEN_OFF_TIMEOUT, mIdToValue.get(R.id.screen_time_10_mins_btn));
                        break;
                    case R.id.screen_time_30_mins_btn:
                        mSystemProxy.putLong(SystemProxy.TABLE_SYSTEM,
                                Settings.System.SCREEN_OFF_TIMEOUT, mIdToValue.get(R.id.screen_time_30_mins_btn));
                        break;
                }
            }
        });
    }
}
