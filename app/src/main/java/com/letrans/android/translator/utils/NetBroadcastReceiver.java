package com.letrans.android.translator.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.permission.RequestPermissionsActivity;
import com.letrans.android.translator.roobo.RooboManager;
import com.letrans.android.translator.settings.ota.OtaTools;
import com.letrans.android.translator.settings.ota.download.OtaDownloadManager;
import com.letrans.android.translator.settings.ota.request.CheckTranslatorOta;
import com.letrans.android.translator.settings.ota.response.AppData;
import com.letrans.android.translator.storage.TStorageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.disposables.Disposable;

public class NetBroadcastReceiver extends BroadcastReceiver implements CheckTranslatorOta.OnCheckListener {

    private static final String TAG = "RTranslator/NetBroadcastReceiver";
    public static Map<Object, NetEvent> eventMap = new HashMap();
    private Context mContext;
    private static final String NORMAL = "2";
    private static final String SILENT = "1";
    List<Disposable> disposableList = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;

        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Logger.i(TAG, "BroadcastReceiver action=" + intent.getAction());
            boolean hasNetwork = NetUtil.isNetworkConnected(TranslatorApp.getAppContext());
            Set<Map.Entry<Object, NetEvent>> set = eventMap.entrySet();
            for (Map.Entry<Object, NetEvent> me : set) {
                NetEvent netEvent = me.getValue();
                Logger.i(TAG, "onNetChange netEvent=" + netEvent);
                if (netEvent != null) {
                    netEvent.onNetChange(hasNetwork);
                }
            }

            if (hasNetwork) {
                if (ServerApi.getInstance().getIsRegisted()) {
                    RooboManager.getInstance().initRoobo(null);
                }
                if (RequestPermissionsActivity.hasBasicPermissions(mContext)) {
                    new CheckTranslatorOta(context, this).startCheckOta();
                    LocationsManager.getInstance(context).getLocation();
                }
            } else {
                removeDispose();
            }
        } else if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            OtaDownloadManager.installSilent(context);
        }
    }


    @Override
    public void onSuccess(AppData client) {
        if (client == null || client.getVersionCode() == null) return;
        int version = OtaTools.getVersion(mContext);
        int versionCode = Integer.parseInt(client.getVersionCode());
        String downloadType = client.getInstallType();
        boolean diffUpdate = client.isDiffUpdate();
        if (version < versionCode) {
            if (downloadType.equals(NORMAL) && RequestPermissionsActivity.hasBasicPermissions(mContext)) {
                long newTime = System.currentTimeMillis();
                long oldTime = TStorageManager.getInstance().getOtaCheckTime();
                boolean isAuto = TStorageManager.getInstance().isOtaAutoCheck();
                if (isAuto && (newTime - oldTime > 4 * 60 * 60 * 1000)) {
                    TStorageManager.getInstance().setOtaCheckTime(newTime);
                    Intent intent = new Intent();
                    intent.putExtra("versionName", client.getVersionName());
                    intent.setClassName("com.letrans.android.translator",
                            "com.letrans.android.translator.settings.ota.dialog.OtaDialogActivity");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                }
            } else {
                if (downloadType.equals(SILENT) && RequestPermissionsActivity.hasBasicPermissions(mContext)) {
                    if (diffUpdate) {
                        OtaDownloadManager.download(mContext, client.getDiffDownloadUrl(), "diff.patch", true);
                    } else {
                        OtaDownloadManager.download(mContext, client.getDownloadUrl(), "Ota.apk", false);
                    }
                }
            }
        }
        removeDispose();
    }

    @Override
    public void onFailed() {
        removeDispose();
    }

    @Override
    public void onAddDisposable(Disposable d) {
        if (disposableList != null) {
            disposableList.add(d);
        }
    }


    private void removeDispose() {
        if (disposableList != null && disposableList.size() > 0) {
            for (Disposable d : disposableList) {
                if (d != null && !d.isDisposed()) {
                    d.dispose();
                }
            }
        }
    }

    public interface NetEvent {
        public void onNetChange(boolean hasNetwork);
    }

    public static void registEvent(Object object, NetEvent netEvent) {
        eventMap.put(object, netEvent);
    }

    public static void unRegistEvent(Object object) {
        eventMap.remove(object);
    }

}
