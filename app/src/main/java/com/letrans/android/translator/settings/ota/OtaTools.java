package com.letrans.android.translator.settings.ota;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.utils.AppUtils;
import com.letrans.android.translator.utils.FileUtils;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.ToastUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;

public class OtaTools {

    private static final String TAG = "RTranslator/OtaTools";

    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    public static String getPhoneModel() {
        String res = Build.MODEL;
        if (TextUtils.isEmpty(res)) {
            return "Translator";
        }
        return "Translator";
    }

    public static int getVersion(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);
            String version = info.versionName;
            int versioncode = info.versionCode;
            return versioncode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getVersionName(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);
            String version = info.versionName;
            int versioncode = info.versionCode;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getSimSerialNumber(Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return "";
            }
        }
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String simNum = tm.getSimSerialNumber();
        if (TextUtils.isEmpty(simNum)) {
            return "";
        } else {
            return simNum;
        }
    }

    public static String getDpi(Context context) {
        return "" + context.getResources().getDisplayMetrics().density;
    }

    public static String getApiLevel() {
        return "" + Build.VERSION.SDK_INT;
    }

    public static File getDownloadDir() {
        File file = new File(OtaConstants.DOWNLOAD_OTA_NAME);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static boolean isNewVersion(Context context, int versionCode) {
        final int oldVersion = getVersion(context);
        return versionCode > oldVersion;
    }

    public static String getNetType(Context context) {
        String netType = "";
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            netType = getProvidersName(context);
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = "WIFI";
        }
        return netType;
    }

    private static String getProvidersName(Context context) {
        String ProvidersName = "";
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = conMan.getActiveNetworkInfo();
        if (info.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS) {
            ProvidersName = "gprs";
        } else if (info.getSubtype() == TelephonyManager.NETWORK_TYPE_CDMA) {
            ProvidersName = "cdma";
        } else if (info.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE) {
            ProvidersName = "edge";
        } else if (info.getSubtype() == TelephonyManager.NETWORK_TYPE_EVDO_0 //
                || info.getSubtype() == TelephonyManager.NETWORK_TYPE_EVDO_A //
                || info.getSubtype() == TelephonyManager.NETWORK_TYPE_EVDO_B) {
            ProvidersName = "evdo";
        } else if (info.getSubtype() == TelephonyManager.NETWORK_TYPE_HSDPA //
                || info.getSubtype() == TelephonyManager.NETWORK_TYPE_HSPA) {
            ProvidersName = "hsdpa";
        } else if (info.getSubtype() == TelephonyManager.NETWORK_TYPE_UMTS) {
            ProvidersName = "umts";
        }
        return ProvidersName;
    }

    public static void updateApk(Context context, String f) {
        File file = new File(f);
        if (Build.VERSION.SDK_INT > 23) {
            Uri uri = FileProvider.getUriForFile(context, "com.letrans.android.translator.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static File bspatchApk(Context context, String path) {

        final File destApk = FileUtils.createFile("ota", path, ".apk");
        final File patch = new File(Environment.getExternalStorageDirectory(), path + "/diff.patch");
        String sourceApk = AppUtils.getSourceApkPath(context, AppUtils.getPackageName(context));
        if (TextUtils.isEmpty(sourceApk) || !patch.exists()) return null;
        final File oldApk = new File(sourceApk);
        int diff = BsPatchUtils.bspatch(oldApk.getAbsolutePath(), destApk.getAbsolutePath(),
                patch.getAbsolutePath());
        Logger.e("RTranslator/OtaTools", "bspatchApk diff = " + diff);
        if (destApk.exists() && diff == 0) {
            FileUtils.deleteFiles(patch);
            return destApk;
        }
        return null;
    }

    public static File clearApk(String apkName) {
        File apkFile = new File(Environment.getExternalStorageDirectory() + "/ota/", apkName + ".apk");
        if (apkFile.exists()) {
            apkFile.delete();
        }
        return apkFile;
    }

    public static File clearFile(File file) {
        if (file.exists()) {
            file.delete();
        }
        return file;
    }

    public static void write2File(InputStream inputStream, File file) {
        if (hasSdcard()) {
            file = clearFile(file);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
        } else {
            ToastUtils.showLong(R.string.ota_sd_error);
            return;
        }

        try {
            if (!file.createNewFile()) {}
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                fos.flush();
            }
            fos.close();
            bis.close();
            inputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
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

    public static boolean checkMd5(File file, String md5) {
        if (TextUtils.isEmpty(md5)) {
            throw new RuntimeException("md5 cannot be empty");
        }
        String fileMd5 = getMd5ByFile(file);

        if (md5.equals(fileMd5)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkMd5(String filePath, String md5) {
        return checkMd5(new File(filePath), md5);
    }

    public static String getMd5ByFile(File file) {
        String value = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);

            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] bytes = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(bytes)) > 0) {
                digester.update(bytes, 0, byteCount);
            }
            value = bytes2Hex(digester.digest());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    private static String bytes2Hex(byte[] src) {
        char[] res = new char[src.length * 2];
        final char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (int i = 0, j = 0; i < src.length; i++) {
            res[j++] = hexDigits[src[i] >>> 4 & 0x0f];
            res[j++] = hexDigits[src[i] & 0x0f];
        }

        return new String(res);
    }


    @SuppressLint("WrongConstant")
    public static int installSilentApk(Context context, String file) {
        PackageManager pm = context.getPackageManager();
        String packageName = AppUtils.getPackageName(context);
        ApplicationInfo ai;
        int uid = 0;
        try {
            ai = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
            uid = ai.uid;
            Logger.d(TAG, " uid " + ai.uid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int userId = 0;
        try {
            final Class systemPropertiesClass = Class.forName("android.os.UserHandle");
            if (systemPropertiesClass != null) {
                Method sSystemPropertiesGetMethod = systemPropertiesClass.getMethod("getUserId", int.class);
                try {
                    userId = (Integer) sSystemPropertiesGetMethod.invoke(null, uid);
                    Logger.d(TAG,  "userId " + userId);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int installResult = backgroundInstall(context, file, userId, packageName);
        if (installResult == 0) {
            SystemProxy.getInstance().reboot();//reboot
        }
        return installResult;
    }


    private static int backgroundInstall(Context context, String filePath, int userId, String packageName) {
        Logger.d(TAG, "backgroundInstall start " + filePath);
        File file = new File(filePath);
        if (TextUtils.isEmpty(filePath) || !file.exists() || !file.isFile()) {
            return OtaConstants.INSTALL_FAILED_INVALID_URI;
        }
        if (filePath.endsWith(".patch")) {
            File diffFile = OtaTools.bspatchApk(context, "mydownfile");
            if (diffFile == null || !diffFile.exists()) {
                return OtaConstants.INSTALL_FAILED;
            } else {
                filePath = diffFile.getAbsolutePath();
            }
        }
        String[] args = {"pm", "install", "-r", "-i", packageName, "--user", "" + userId, filePath};
        ProcessBuilder processBuilder = new ProcessBuilder(args);

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            process = processBuilder.start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;

            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
        } catch (IOException e) {
            Logger.d(TAG, "ioerror " + e);
            e.printStackTrace();
        } catch (Exception e) {
            Logger.d(TAG, "e " + e);
            e.printStackTrace();
        } finally {
            FileUtils.deleteFiles(new File(filePath));
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
        if (successMsg != null && (successMsg.toString().contains("Success")
                || successMsg.toString().contains("success"))) {
            return OtaConstants.INSTALL_SUCCEEDED;
        }
        String errorString = errorMsg.toString();
        Logger.e(TAG,"install failed " + errorString);
        return OtaConstants.INSTALL_FAILED;
    }

    public static String formatData(long total) {
        String format = "%1d KB/%2d KB";
        if (total > 1024 * 1024) {
            format = "%1d GB/%2d GB";
        } else if (total > 1024) {
            format = "%1d MB/%2d MB";
        }
        return format;
    }
}
