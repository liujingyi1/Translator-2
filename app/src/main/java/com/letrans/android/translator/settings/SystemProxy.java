package com.letrans.android.translator.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.text.format.Formatter;

import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.SystemPropertiesAdapter;
import com.letrans.systemadapter.ISystemAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemProxy {
    private static final String TAG = "RTranslator/SystemProxy";
    private static final String MEM_INFO_PATH = "/proc/meminfo";
    public static final String MEMTOTAL = "MemTotal";
    public static final String MEMFREE = "MemFree";
    private static final long G_BYTES = 1024 * 1024 * 1024L;

    public final static String TABLE_SYSTEM = "system";
    public final static String TABLE_SECURE = "secure";
    public final static String TABLE_GLOBAL = "global";

    public final static int TYPE_BATTERY = 1;
    public final static int TYPE_CHARGING = 2;

    private ISystemAdapter mISystemAdapter;
    private Context mContext;
    private static SystemProxy mInstance;

    private SystemProxy(Context context) {
        Logger.d(TAG, "create SystemProxy.");
        mContext = context;
    }

    public static SystemProxy getInstance() {
        return mInstance;
    }

    public synchronized static void init(Context context) {
        if (mInstance == null) {
            mInstance = new SystemProxy(context);
        }
    }

    public static void destroy() {
        if (mInstance != null) {
            mInstance.onDestroy();
            mInstance = null;
        }
    }

    private void onDestroy() {
        mISystemAdapter = null;
        mContext = null;
    }

    public boolean isActive() {
        return mISystemAdapter != null;
    }

    private ISystemAdapter getISystemAdapter() {
        if (mISystemAdapter != null) {
            return mISystemAdapter;
        }

        try {
            Class clzServiceManager = Class.forName("android.os.ServiceManager");
            Method clzServiceManager$getService = clzServiceManager
                    .getDeclaredMethod("getService", String.class);
            Object oRemoteService = clzServiceManager$getService
                    .invoke(null, "SystemAdapter");
            IBinder iBinder = (IBinder) oRemoteService;
            mISystemAdapter = ISystemAdapter.Stub.asInterface(iBinder);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            mISystemAdapter = null;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            mISystemAdapter = null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            mISystemAdapter = null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            mISystemAdapter = null;
        }
        Logger.d(TAG, "getISystemAdapter: " + mISystemAdapter);
        return mISystemAdapter;
    }

    /**
     * Get total ram memory
     *
     * @return
     */
    public long getTotalMemory() {
        ActivityManager mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        mAm.getMemoryInfo(mi);
        return mi.totalMem;
        //return getMemInfoIype(context, MEMTOTAL);
    }

    /**
     * Get free ram memory
     *
     * @return
     */
    public long getMemoryFree() {
        ActivityManager mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        mAm.getMemoryInfo(mi);
        return mi.availMem;
        //return getMemInfoIype(context, MEMFREE);
    }

    /**
     * MemTotal: 所有可用RAM大小。
     * MemFree: LowFree与HighFree的总和，被系统留着未使用的内存。
     * Buffers: 用来给文件做缓冲大小。
     * Cached: 被高速缓冲存储器（cache memory）用的内存的大小（等于diskcache minus SwapCache）。
     * SwapCached:被高速缓冲存储器（cache memory）用的交换空间的大小。已经被交换出来的内存，仍然被存放在swapfile中，用来在需要的时候很快的被替换而不需要再次打开I/O端口。
     * Active: 在活跃使用中的缓冲或高速缓冲存储器页面文件的大小，除非非常必要，否则不会被移作他用。
     * Inactive: 在不经常使用中的缓冲或高速缓冲存储器页面文件的大小，可能被用于其他途径。
     * SwapTotal: 交换空间的总大小。
     * SwapFree: 未被使用交换空间的大小。
     * Dirty: 等待被写回到磁盘的内存大小。
     * Writeback: 正在被写回到磁盘的内存大小。
     * AnonPages：未映射页的内存大小。
     * Mapped: 设备和文件等映射的大小。
     * Slab: 内核数据结构缓存的大小，可以减少申请和释放内存带来的消耗。
     * SReclaimable:可收回Slab的大小。
     * SUnreclaim：不可收回Slab的大小（SUnreclaim+SReclaimable＝Slab）。
     * PageTables：管理内存分页页面的索引表的大小。
     * NFS_Unstable:不稳定页表的大小。
     * 要获取android手机总内存大小，只需读取”/proc/meminfo”文件的第1行，并进行简单的字符串处理即可。
     * <p>
     * Get ram memory size
     *
     * @param type memory type
     * @return
     */
    public String getMemInfoIype(String type) {
        try {
            FileReader fileReader = new FileReader(MEM_INFO_PATH);
            BufferedReader bufferedReader = new BufferedReader(fileReader, 4 * 1024);
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                if (str.contains(type)) {
                    break;
                }
            }
            bufferedReader.close();
            /* \\s表示   空格,回车,换行等空白符,
            +号表示一个或多个的意思     */
            String[] array = str.split("\\s+");
            // 获得系统总内存，单位是KB，乘以1024转换为Byte
            long length = Integer.valueOf(array[1]).intValue() * 1024;
            length = adjustSize(length);
            return Formatter.formatFileSize(mContext, length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long getInternalTotalStorageMemory() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter != null) {
            try {
                return systemAdapter.getInternalTotalStorageMemory();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        String path = Environment.getDataDirectory().getPath();
        //path = Environment.getExternalStorageDirectory().getPath();
        StatFs statFs = new StatFs(path);
        long blockSize = statFs.getBlockSizeLong();
        long totalBlocks = statFs.getBlockCountLong();
        long availableBlocks = statFs.getAvailableBlocksLong();
        long useBlocks = totalBlocks - availableBlocks;

        long rom_length = totalBlocks * blockSize;

        long systemSpace = getSystemTotalSpace();
        long totalSize = 0;
        if (path.equals("/data")) {
            totalSize = adjustSize(rom_length + systemSpace);
        } else {
            totalSize = rom_length;
        }

        return totalSize;
    }

    public long getInternalFreeStorageMemory() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter != null) {
            try {
                return systemAdapter.getInternalFreeStorageMemory();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public long getExternalTotalStorageMemory() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter != null) {
            try {
                return systemAdapter.getExternalTotalStorageMemory();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public long getExternalFreeStorageMemory() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter != null) {
            try {
                return systemAdapter.getExternalFreeStorageMemory();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public void refreshStorage() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.refreshStorage();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public long[] getExternalStorageMemory() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        long[] usage = new long[]{0, 0, 0};
        if (systemAdapter == null) {
            return usage;
        }
        try {
            usage = systemAdapter.getExternalStorageMemory();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (usage == null) {
            usage = new long[]{0, 0, 0};
        }
        return usage;
    }

    public long[] getInternalStorageMemory() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        long[] usage = new long[]{0, 0, 0};
        if (systemAdapter == null) {
            return usage;
        }
        try {
            usage = systemAdapter.getInternalStorageMemory();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (usage == null) {
            usage = new long[]{0, 0, 0};
        }
        return usage;
    }

    private long adjustSize(long totalSize) {
        long ret = 0;
        if (totalSize <= 1 * G_BYTES) {
            ret = 1 * G_BYTES;
        } else if (totalSize <= 4 * G_BYTES) {
            ret = 4 * G_BYTES;
        } else if (totalSize <= 8 * G_BYTES) {
            ret = 8 * G_BYTES;
        } else if (totalSize <= 16 * G_BYTES) {
            ret = 16 * G_BYTES;
        }
        return ret;
    }

    public long getSystemTotalSpace() {
        long systemSpace = 0L;
        File root = Environment.getRootDirectory();
        StatFs sf = new StatFs(root.getPath());
        long blockSize = sf.getBlockSizeLong();
        long blockCount = sf.getBlockCountLong();
        systemSpace = blockSize * blockCount;
        return systemSpace;
    }

    public String getStringProperties(String key, String def) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return SystemPropertiesAdapter.get(key);
        }
        try {
            return systemAdapter.getStringProperties(key, def);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return def;
    }

    public void eraseDevice() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.eraseDevice();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getWifiMacAddress() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return "";
        }
        String mac = "";
        try {
            mac = systemAdapter.getWifiMacAddress();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return mac;
    }

    //To call SettingsProvider
    public int getInt(String tableName, String name, int def) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return 0;
        }
        int value = 0;
        try {
            value = systemAdapter.getInt(tableName, name, def);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    public boolean putInt(String tableName, String name, int value) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return false;
        }
        boolean ret = false;
        try {
            ret = systemAdapter.putInt(tableName, name, value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public float getFloat(String tableName, String name, float def) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return 0;
        }
        float value = 0;
        try {
            value = systemAdapter.getFloat(tableName, name, def);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    public boolean putFloat(String tableName, String name, float value) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return false;
        }
        boolean ret = false;
        try {
            ret = systemAdapter.putFloat(tableName, name, value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public long getLong(String tableName, String name, long def) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return 0;
        }
        long value = 0;
        try {
            value = systemAdapter.getLong(tableName, name, def);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    public boolean putLong(String tableName, String name, long value) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return false;
        }
        boolean ret = false;
        try {
            ret = systemAdapter.putLong(tableName, name, value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String getString(String tableName, String name) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return "";
        }
        String value = "";
        try {
            value = systemAdapter.getString(tableName, name);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    public boolean putString(String tableName, String name, String value) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return false;
        }
        boolean ret = false;
        try {
            ret = systemAdapter.putString(tableName, name, value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    //To change the system language
    public void updateLocales(String language, String country) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.updateLocales(language, country);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateFontScale(float values) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.updateFontScale(values);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getFontSizeIndex(float[] indices) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return 0;
        }
        int index = 0;
        try {
            index = systemAdapter.getFontSizeIndex(indices);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return index;
    }

    public void setBrightness(int brightness) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.setBrightness(brightness);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void broadcastHourFormat(boolean is24HourFormat) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.broadcastHourFormat(is24HourFormat);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getCSN() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return null;
        }
        String csn = null;
        try {
            csn = systemAdapter.getCSN();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return csn;
    }

    public void setDate(int year, int month, int day) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.setDate(year, month, day);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setTime(int hourOfDay, int minute) {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.setTime(hourOfDay, minute);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setScreenTimeout(long millis, int type) {
        if (type != TYPE_BATTERY && type != TYPE_CHARGING) {
            return;
        }
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.setScreenTimeout(millis, type);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public long getScreenTimeout(int type) {
        long def = 120000l;
        if (type != TYPE_BATTERY && type != TYPE_CHARGING) {
            return def;
        }
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return def;
        }
        try {
            return systemAdapter.getScreenTimeout(type);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return def;
    }

    public void reboot() {
        ISystemAdapter systemAdapter = getISystemAdapter();
        if (systemAdapter == null) {
            return;
        }
        try {
            systemAdapter.reboot();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
