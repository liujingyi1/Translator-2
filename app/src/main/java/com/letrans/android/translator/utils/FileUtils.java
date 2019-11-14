package com.letrans.android.translator.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FileUtils {

    private static final String TAG = "RTranslator/FileUtils";

    public static boolean deleteFiles(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File files[] = file.listFiles();
            for (File f : files) {
                deleteFiles(f);
            }
            file.delete();
        } else {
            file.delete();
        }
        return true;
    }

    public static void addTxtToFileBuffered(final File file, final String content) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedWriter out = null;
                try {
                    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
                    out.newLine();
                    out.write(content);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    public static boolean makeDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            return file.mkdirs();
        } else {
            return true;
        }
    }

    public static File createFile(String name, String path) {
        File sampleDir = Environment.getExternalStorageDirectory();
        sampleDir = new File(sampleDir.getAbsolutePath() + "/" + path);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        File file = new File(sampleDir.getAbsolutePath() + "/" + name + ".wav");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static File createFile(String name, String path, String suffix) {
        File sampleDir = Environment.getExternalStorageDirectory();
        sampleDir = new File(sampleDir.getAbsolutePath() + "/" + path);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        File file = new File(sampleDir.getAbsolutePath() + "/" + name + suffix);
        if (file.exists()) {
            file.delete();
        }
        return file;
    }

    @SuppressLint("Assert")
    public static String writeBytesToFile(byte[] bytes, String name, String path) {
        Logger.d(TAG, "writeBytesToFile");

        File sampleDir = Environment.getExternalStorageDirectory();
        sampleDir = new File(sampleDir.getAbsolutePath() + "/" + path);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        File file = new File(sampleDir.getAbsolutePath() + "/" + name + ".wav");
        if (file.exists()) {
            file.delete();
        }
        try {
            OutputStream out = new FileOutputStream(file);
            InputStream is = new ByteArrayInputStream(bytes);

            WaveHeader header = new WaveHeader();
            header.fileLength = bytes.length + (44 - 8);
            header.FmtHdrLeth = 16;
            header.BitsPerSample = 16;
            header.Channels = 1;
            header.FormatTag = 0x0001;
            header.SamplesPerSec = 16000;
            header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
            header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
            header.DataHdrLeth = bytes.length;
            byte[] h = header.getHeader();
            assert h.length == 44;
            out.write(h, 0, h.length);

            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = is.read(buff)) != -1) {
                out.write(buff, 0, len);
            }
            is.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file.getAbsolutePath();
    }

    public static boolean compareApk(PackageInfo packageInfo, Context context) {
        if (packageInfo == null) {
            return false;
        }
        String localName = context.getPackageName();
        if (localName.equals(packageInfo.packageName)) {
            try {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo localInfo = packageManager.getPackageInfo(localName, 0);
                if (localInfo.versionCode < packageInfo.versionCode) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static PackageInfo getApkInfo(String path, Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if (packageInfo != null) {
            return packageInfo;
        }
        return null;
    }

    public static boolean makeDirWithFile(String dirPath) {
        if (TextUtils.isEmpty(dirPath) || dirPath.lastIndexOf("/") < 0) {
            return false;
        }
        File file = new File(dirPath.substring(0, dirPath.lastIndexOf("/")));
        if (!file.exists()) {
            return file.mkdirs();
        } else {
            return true;
        }
    }
}
