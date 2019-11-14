package com.letrans.android.translator.settings.common;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.SeekBar;

import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.utils.Logger;

import java.lang.reflect.Method;

public class BrightnessController {
    private static final String TAG = "BrightnessController";
    private SeekBar mSeekBar;
    private Context mContext;
    private final int mMinimumBacklight = 10;
    private final int mMaximumBacklight = 255;
    public static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";

    private SystemProxy mSystemProxy;

    public BrightnessController(Context context, SeekBar seekBar) {
        mContext = context;
        mSeekBar = seekBar;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        //mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        //mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();
        mSystemProxy = SystemProxy.getInstance();
        mSystemProxy.putInt(SystemProxy.TABLE_SYSTEM, SCREEN_BRIGHTNESS_MODE, 0);
        updateSlider();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Logger.d(TAG, "onProgressChanged: progress = " + progress);
                final int val = progress + mMinimumBacklight;
                setBrightness(val);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Logger.d(TAG, "onProgressChanged: seekBar.getProgress() = " + seekBar.getProgress());
                final int value = seekBar.getProgress();
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        mSystemProxy.putInt(SystemProxy.TABLE_SYSTEM,
                                Settings.System.SCREEN_BRIGHTNESS, mMinimumBacklight + value);
                    }
                });
            }
        });
    }


    private void setBrightness(int brightness) {
        mSystemProxy.setBrightness(brightness);
    }

    private void setBrightnessAdj(float adj) {
        try {
            Class clzIPowerMag = Class.forName("android.os.IPowerManager");
            Method clzIPowerMag$setTemporaryScreenAutoBrightnessAdjustmentSettingOverride = clzIPowerMag.getDeclaredMethod("setTemporaryScreenAutoBrightnessAdjustmentSettingOverride", float.class);
            clzIPowerMag$setTemporaryScreenAutoBrightnessAdjustmentSettingOverride.invoke(clzIPowerMag, adj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateSlider() {
        int value = mSystemProxy.getInt(SystemProxy.TABLE_SYSTEM,
                Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight);
        Logger.d(TAG, "value = " + value);
        mSeekBar.setMax(mMaximumBacklight - mMinimumBacklight);
        mSeekBar.setProgress(value - mMinimumBacklight);
    }
}
