package com.letrans.android.translator.settings.ota.download;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.letrans.android.translator.settings.ota.BackgroundInstallTask;
import com.letrans.android.translator.settings.ota.OtaTools;
import com.letrans.android.translator.utils.Logger;

import java.util.concurrent.Executors;

public class OtaDownloadManager {


    private static final String TAG = "RTranslator/OtaDownloadManager";
    private static final String DLID = "downloadId";
    private static final String SP_FILE_NAME = "translator.cfg";

    public static void download(Context context, String url, String appName, boolean diffUpdate) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        long downloadId = sharedPreferences.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        if (downloadId != -1L) {
            OtaDownload downloadManager = OtaDownload.getInstance(context);
            int status = downloadManager.getDownloadStatus(downloadId);
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Uri uri = Uri.parse(downloadManager.getDownloadPath(downloadId));
                Logger.d(TAG,"downuri : " + uri);
                if (uri != null) {
                    if (OtaTools.compareApk(OtaTools.getApkInfo(uri.getPath(), context), context)
                            || (diffUpdate && uri.getPath().endsWith(".diff"))) {
                        OtaDownloadManager.installSilent(context);
                        sharedPreferences.edit().putLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L).apply();
                        return;
                    } else {
                        downloadManager.getDownloadManager().remove(downloadId);
                    }
                }
                startDownload(context, url, appName);
            } else if (status == DownloadManager.STATUS_FAILED || status == -1) {
                downloadManager.getDownloadManager().remove(downloadId);
                startDownload(context, url, appName);
            }
        } else {
            startDownload(context, url, appName);
        }
    }

    private static void startDownload(Context context, String url, String appName) {
        OtaDownload otaDownload = OtaDownload.getInstance(context);
        long downloadId = otaDownload.downloadApk(url, appName);
        Logger.d(TAG,"startDownload downloadId : " + downloadId);
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putLong(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId).apply();
    }

    public static void installSilent(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        long downloadId = sharedPreferences.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        Uri uri = Uri.parse(OtaDownload.getInstance(context).getDownloadPath(downloadId));
        Logger.d(TAG,"installSilent uri : " + uri);
        //new BackgroundInstallTask(context).execute(uri.getPath());
        new BackgroundInstallTask(context).executeOnExecutor(Executors.newCachedThreadPool(), uri.getPath());
    }
}
