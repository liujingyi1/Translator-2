package com.letrans.android.translator;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Process;
import android.text.TextUtils;

import com.letrans.android.translator.mpush.TMPushService;
import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetBroadcastReceiver;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class TranslatorApp extends Application {

    private static final String TAG = "RTranslator/TranslatorApp";

    public TStorageManager mStorageManager;
    private static TranslatorApp mInstance;

    @Override
    public void onCreate() {
        Logger.i(TAG, "onCreate()");
        super.onCreate();
        String processName = this.getProcessName();
        if (!TextUtils.isEmpty(processName) && processName.equals(this.getPackageName())) {
            if (null == getResources()) {
                System.out.println("-killProcess-");
                Process.killProcess(Process.myPid());
            }
            bindSystemAdapterService();
            CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
            strategy.setUploadProcess(processName == null || processName.equals(this.getPackageName()));
            // 初始化Bugly
            CrashReport.initCrashReport(this, "0bb37e8c8e", false, strategy);
            TStorageManager.init(this);
            mStorageManager = TStorageManager.getInstance();

            mInstance = this;
            Intent startIntent = new Intent(this, TMPushService.class);
            startService(startIntent);

            NetBroadcastReceiver netWorkChangReceiver = new NetBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getApplicationContext().registerReceiver(netWorkChangReceiver, filter);
        }
    }

    public static TranslatorApp getInstance() {
        return mInstance;
    }

    public static Context getAppContext() {
        return mInstance.getApplicationContext();
    }

    public static String getProcessName() {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void bindSystemAdapterService() {
        SystemProxy.init(getApplicationContext());
    }
}
