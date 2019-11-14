package com.letrans.android.translator.settings.common;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.utils.Logger;


public class CommonSettingFragment extends Fragment implements View.OnClickListener {

    private SeekBar mBrightnessLevel;
    private BrightnessController mBrightnessController;

    private View mBatteryPercentageContainer;
    private Switch mBatteryPercentage;
    private FragmentManager mFragmentManager;
    private TextView mSystemLanguages;
    private TextView mTextSize;
    private TextView mDateTime;

    private FragmentDateTime mFragmentDateTime;
    private FragmentFontSize mFragmentFontSize;
    private FragmentSystemLanguages mFragmentSystemLanguages;
    private ScreenTimeout mScreenTimeout;
    private boolean mIsTracking;
    private long[] times = {120000l, 300000l, 600000l, 1800000l, -1};
    private int mCurrentScreenTimeoutProgress;
    private int mCurrentScreenTimeoutChargingProgress;
    private boolean mChanged;

    private static final String TAG = "CommonSettingFragment";
    private static final String BATTERY_PERCENTAGE_ENABLE = "battery_percentage_enable";

    private ScreenTimeoutSeekBar.OnSeekBarChangeListener mSeekBarListener
            = new ScreenTimeoutSeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(ScreenTimeoutSeekBar seekBar, int progress) {
            mChanged = true;
            switch (seekBar.getId()) {
                case R.id.seek_bar_screen_timeout:
                    mCurrentScreenTimeoutProgress = progress;
                    break;
                case R.id.seek_bar_screen_timeout_charging:
                    mCurrentScreenTimeoutChargingProgress = progress;
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(ScreenTimeoutSeekBar seekBar) {
            mChanged = false;
        }

        @Override
        public void onStopTrackingTouch(ScreenTimeoutSeekBar seekBar) {
            if (!mChanged) {
                return;
            }
            switch (seekBar.getId()) {
                case R.id.seek_bar_screen_timeout:
                    SystemProxy.getInstance().setScreenTimeout(
                            times[mCurrentScreenTimeoutProgress],
                            SystemProxy.TYPE_BATTERY);
                    break;
                case R.id.seek_bar_screen_timeout_charging:
                    SystemProxy.getInstance().setScreenTimeout(
                            times[mCurrentScreenTimeoutChargingProgress],
                            SystemProxy.TYPE_CHARGING);
                    break;
            }
        }
    };

    /*@Override
    public void onAttachFragment(Fragment childFragment) {
        if (childFragment instanceof FragmentDateTime) {
            mFragmentDateTime = (FragmentDateTime) childFragment;
        } else if (childFragment instanceof FragmentFontSize) {
            mFragmentFontSize = (FragmentFontSize) childFragment;
        } else if (childFragment instanceof FragmentSystemLanguages) {
            mFragmentSystemLanguages = (FragmentSystemLanguages) childFragment;
        }
    }*/

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getFragmentManager();
        if (savedInstanceState != null) {
            Fragment fragment = mFragmentManager.findFragmentById(R.id.new_container);
            if (fragment != null) {
                if (fragment instanceof FragmentDateTime) {
                    mFragmentDateTime = (FragmentDateTime) fragment;
                } else if (fragment instanceof FragmentFontSize) {
                    mFragmentFontSize = (FragmentFontSize) fragment;
                } else if (fragment instanceof FragmentSystemLanguages) {
                    mFragmentSystemLanguages = (FragmentSystemLanguages) fragment;
                } else if (fragment instanceof ScreenTimeout) {
                    mScreenTimeout = (ScreenTimeout) fragment;
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.setting_common, container, false);
        mBrightnessLevel = (SeekBar) view.findViewById(R.id.seek_bar_brightness_level);
        mSystemLanguages = (TextView) view.findViewById(R.id.id_languages);
        mTextSize = (TextView) view.findViewById(R.id.id_font_size);
        mDateTime = (TextView) view.findViewById(R.id.id_date_time);
        mBatteryPercentageContainer = view.findViewById(R.id.sw_battery_percentage_container);
        mBatteryPercentage = (Switch) view.findViewById(R.id.sw_battery_percentage);
        initSeekBar(view);
        mTextSize.setOnClickListener(this);
        mDateTime.setOnClickListener(this);
        mSystemLanguages.setOnClickListener(this);
        configBrightnessLevel();
        configBatteryPercentage();

        return view;
    }

    private void initSeekBar(View view) {
        ScreenTimeoutSeekBar screenTimeoutSeekBar = (ScreenTimeoutSeekBar) view.findViewById(R.id.seek_bar_screen_timeout);
        ScreenTimeoutSeekBar screenTimeoutChargingSeekBar = (ScreenTimeoutSeekBar) view.findViewById(R.id.seek_bar_screen_timeout_charging);
        screenTimeoutSeekBar.setOnSeekBarChangeListener(mSeekBarListener);
        screenTimeoutChargingSeekBar.setOnSeekBarChangeListener(mSeekBarListener);
        long millis = SystemProxy.getInstance().getScreenTimeout(SystemProxy.TYPE_BATTERY);
        screenTimeoutSeekBar.setMax(4);
        screenTimeoutSeekBar.setProgress(findProgress(millis));
        millis = SystemProxy.getInstance().getScreenTimeout(SystemProxy.TYPE_CHARGING);
        screenTimeoutChargingSeekBar.setMax(4);
        screenTimeoutChargingSeekBar.setProgress(findProgress(millis));
    }

    private int findProgress(long millis) {
        for (int i = 0; i < times.length; i++) {
            if (times[i] == millis) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        mBrightnessController.updateSlider();
    }

    private void configBatteryPercentage() {
        int t = SystemProxy.getInstance().getInt(SystemProxy.TABLE_SYSTEM, BATTERY_PERCENTAGE_ENABLE, 0);
        Logger.d(TAG, "configBatteryPercentage: t " + t);
        mBatteryPercentage.setChecked(t > 0 ? true : false);
        mBatteryPercentageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBatteryPercentage.setChecked(!mBatteryPercentage.isChecked());
            }
        });
        mBatteryPercentage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SystemProxy.getInstance().putInt(SystemProxy.TABLE_SYSTEM,
                        BATTERY_PERCENTAGE_ENABLE, isChecked ? 1 : 0);
            }
        });
    }

    private void configBrightnessLevel() {
        mBrightnessController = new BrightnessController(getContext(), mBrightnessLevel);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.id_date_time:
                showDateTimeFragment();
                break;
            case R.id.id_font_size:
                showFontSizeFragment();
                break;
            case R.id.id_languages:
                showSystemLanguagesFragment();
                break;
        }
    }

    private void showDateTimeFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentDateTime = new FragmentDateTime();
        fragmentTransaction.add(R.id.common_new_container, mFragmentDateTime);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showFontSizeFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentFontSize = new FragmentFontSize();
        fragmentTransaction.add(R.id.common_new_container, mFragmentFontSize);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showSystemLanguagesFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentSystemLanguages = new FragmentSystemLanguages();
        fragmentTransaction.add(R.id.common_new_container, mFragmentSystemLanguages);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showScreenTimeoutFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        mScreenTimeout = new ScreenTimeout();
        fragmentTransaction.add(R.id.common_new_container, mScreenTimeout);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isDestroyed()) {
            return;
        }
        int count = mFragmentManager.getBackStackEntryCount();
        for (int i = 0; i < count; i++) {
            mFragmentManager.popBackStack();
        }
    }
}
