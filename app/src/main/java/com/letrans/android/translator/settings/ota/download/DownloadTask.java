package com.letrans.android.translator.settings.ota.download;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.ota.OtaConstants;
import com.letrans.android.translator.settings.ota.OtaTools;
import com.letrans.android.translator.utils.AppUtils;
import com.letrans.android.translator.utils.FileUtils;
import com.letrans.android.translator.utils.ToastUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends AsyncTask<String, Long, String> {

    private String otaFile;
    private DownloadListener listener;

    public interface DownloadListener {
        void onPreDownload();
        void onUpdate(int total, int bytes, boolean isDone);
        void onCompleted(String result, String file);
    }

    public DownloadTask(Context context, DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        File file = null;
        try {
            URL url = new URL(sUrl[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP "
                        + connection.getResponseCode() + " "
                        + connection.getResponseMessage();
            }
            long fileLength = connection.getContentLength();
            boolean isDiffUpdate = !TextUtils.isEmpty(sUrl[2]) && "1".equals(sUrl[2]);
            if (OtaTools.hasSdcard()) {
                if (!isDiffUpdate) {
                    file = FileUtils.createFile(sUrl[1], "ota", ".apk");
                } else {
                    file = FileUtils.createFile("diff", "ota", ".patch");
                }
            } else {
                ToastUtils.showLong(R.string.ota_sd_error);
                return "sdcard error";
            }
            if (!file.createNewFile()) {
                return "create file error";
            }
            otaFile = file.getAbsolutePath();
            input = connection.getInputStream();
            output = new FileOutputStream(file);
            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                if (fileLength > 0) {
                    publishProgress(total, fileLength, 0L);
                    total = total + count;
                }
                output.write(data, 0, count);
            }

            boolean normalUpdate = true;//only for normal update
            if (normalUpdate) {
                publishProgress(total, fileLength, 1L);
                if (isDiffUpdate) {
                    File diffFile = OtaTools.bspatchApk(AppUtils.getApp(), "ota");
                    if (diffFile == null || !diffFile.exists()) {
                        return "create diffFile error";
                    } else {
                        otaFile = diffFile.getAbsolutePath();
                    }
                }
                int result = OtaTools.installSilentApk(AppUtils.getApp(), otaFile);
                if (result != OtaConstants.INSTALL_SUCCEEDED) {
                    return "silent install failed";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Server connect error";
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
            }
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (listener != null) {
            listener.onPreDownload();
        }
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        super.onProgressUpdate(progress);
        if (listener != null) {
            listener.onUpdate((int) (progress[1] / 1024), (int) (progress[0] / 1024), progress[2].equals(1L));
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (listener != null) {
            listener.onCompleted(result, otaFile);
        }
    }
}
