package com.letrans.android.translator.settings.wifi;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;

import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetUtil;

import java.lang.reflect.Field;
import java.util.List;

public class WifiCheckPortalReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiCheckPortalReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isTranslator(context)) {
            Logger.d(TAG, "WifiCheckPortalReceiver.onReceive not Translator app----------return.");
            return;
        }
        if (!NetUtil.hasConnect(context)) {
            Logger.d(TAG, "WifiCheckPortalReceiver.onReceive: network not connect, return.");
            return;
        }
        String action = intent.getAction();
        Logger.d(TAG, "WifiCheckPortalReceiver.  action: " + action);
        switch (action) {
            case ConnectivityManager.CONNECTIVITY_ACTION:
                context.startService(new Intent(context, WifiCheckPortalService.class));
                break;
        }
    }

    private boolean isTranslator(Context context) {
        String packageName = context.getApplicationInfo().packageName;
        return packageName.equals(getTopAppInfoPackageName(context));
    }

    /**
     * This method return the top process's package name.
     *
     * @param context
     * @return return package name or return "".
     */
    public static String getTopAppInfoPackageName(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = "";
        if (Build.VERSION.SDK_INT < 21) { // if: version < 22
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            if (tasks != null && tasks.size() > 0) {
                ActivityManager.RunningTaskInfo info = tasks.get(0);
                packageName = info.topActivity.getPackageName();
                Logger.d(TAG, "getTopAppInfoPackageName: Version < 21 --" + packageName);
            }
        } else { // if: version >= 22
            final int PROCESS_STATE_TOP = 2;
            try {
                Field processStateField = ActivityManager.RunningAppProcessInfo.class
                        .getDeclaredField("processState");
                List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && process.importanceReasonCode == 0) {
                        int state = processStateField.getInt(process);
                        if (state == PROCESS_STATE_TOP) {
                            packageName = process.pkgList[0];
                            Logger.d(TAG, "getTopAppInfoPackageName: Version >= 21 -----" + packageName);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        Logger.d(TAG, "getTopAppInfoPackageName: return-----" + packageName);

        return packageName;
    }
}
