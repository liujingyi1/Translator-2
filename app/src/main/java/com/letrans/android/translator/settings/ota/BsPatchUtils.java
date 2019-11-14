package com.letrans.android.translator.settings.ota;


public class BsPatchUtils {

    static {
        System.loadLibrary("bsdiff");
    }

    /**
     * native方法 使用路径为oldApk的apk与路径为patch的补丁包，合成新的apk，并存储于newApk
     *
     * 返回：0，说明操作成功
     *
     * @param oldApk 示例:/sdcard/old.apk
     * @param newApk 示例:/sdcard/new.apk
     * @param patch  示例:/sdcard/xx.patch
     * @return
     */
    public static native int bspatch(String oldApk, String newApk, String patch);

}
