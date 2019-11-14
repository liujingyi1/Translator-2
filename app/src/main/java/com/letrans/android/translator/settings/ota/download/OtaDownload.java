package com.letrans.android.translator.settings.ota.download;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.letrans.android.translator.utils.Logger;

import java.io.File;

public class OtaDownload {

    private static final String TAG = "RTranslator/OtaDownload";
    private DownloadManager downloadManager;
    private static volatile OtaDownload otaDownload;

    private OtaDownload(Context context) {
        downloadManager = (DownloadManager) context.getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public static OtaDownload getInstance(Context context) {
        if (otaDownload == null) {
            synchronized (OtaDownload.class) {
                if (otaDownload == null) {
                    otaDownload = new OtaDownload(context);
                }
            }
        }
        return otaDownload;
    }


    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public long downloadApk(String url, String appName) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mydownfile/");
        if (!file.exists()) {
            file.mkdirs();
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType("application/vnd.android.package-archive");
        request.setVisibleInDownloadsUi(false);
        request.setDestinationInExternalPublicDir("/mydownfile/", appName);
        return downloadManager.enqueue(request);
    }

    public String getDownloadPath(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                }
            } catch (Exception e) {
                Logger.e(TAG, e.getMessage());
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public int getDownloadStatus(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                }
            } catch (Exception e) {
                Logger.e(TAG, e.getMessage());
            } finally {
                cursor.close();
            }
        }
        return -1;
    }
}
