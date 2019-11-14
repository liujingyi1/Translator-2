package com.letrans.android.translator.settings.wifi;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.letrans.android.translator.storage.TStorageManager;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class WifiCheckPortalService extends IntentService {
    static final String ACTION_WIFI_CHECK_SERVICE =
            "com.letrans.android.translator.settings.wifi.WifiCheckPortalService";
    public static final String EXTRA_WIFI_PORTAL = "isWifiSetPortal";

    public WifiCheckPortalService() {
        super("WifiCheckPortalService");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        boolean isPortal = WifiCheckPortalTool.isWifiSetPortal();
        TStorageManager.getInstance().saveCaptivePortal(isPortal);
        if (isPortal) {
            Intent loginIntent = new Intent(getApplicationContext(), WifiLoginDialog.class);
            loginIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*new Thread() {
            @Override
            public void run() {
                int isWifiSetPortal = WifiCheckPortalTool.isWifiSetPortal() ? 1 : 0;
                Intent intent = new Intent();
                intent.putExtra(EXTRA_WIFI_PORTAL, isWifiSetPortal);
                intent.setAction(ACTION_WIFI_CHECK_SERVICE);
                sendBroadcast(intent);
            }
        }.start();*/
        return super.onStartCommand(intent, flags, startId);
    }
}