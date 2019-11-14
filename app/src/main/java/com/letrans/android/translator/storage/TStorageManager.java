package com.letrans.android.translator.storage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.database.DbConstants;
import com.letrans.android.translator.database.TranslatorProvider;
import com.letrans.android.translator.database.beans.MemberBean;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.database.beans.ThreadsBean;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.languagemodel.ILanguageModel;
import com.letrans.android.translator.languagemodel.LanguageItem;
import com.letrans.android.translator.languagemodel.LanguageModelImpl;
import com.letrans.android.translator.settings.wifi.WifiCheckPortalService;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.utils.ToastUtils;
import com.letrans.android.translator.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TStorageManager {
    private static final String TAG = "RTranslator/TStorageManager";

    public static final int STORAGE_TYPE_NOT_SAVE = 1;
    public static final int STORAGE_TYPE_SAVE_TEXT = 2;
    public static final int STORAGE_TYPE_SAVE_SOUND = 3;
    public static final int STORAGE_TYPE_SAVE_TEXT_SOUND = 4;

    private static final String SP_FILE_NAME = "translator.cfg";
    private static final String SP_KEY_DEVICE_ID = "deviceId";
    public static final String KEY_AUTO = "auto";
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String KEY_PAIR_DEVICE_ID = "PAIR_DEVICE_ID";
    public static final String KEY_STORAGE_TYPE = "key_storage_type";
    public static final String KEY_DEFAULT_PAGE_ADMIN = "default_page_ADMIN";
    public static final String KEY_DEFAULT_PAGE_CUSTOM = "default_page_CUSTOM";
    public static final String KEY_NOTICE_TEXT = "notice_text";
    public static final String KEY_NOTICE_PIC = "notice_pic";
    private static final String OTA_AUTO = "ota_auto";
    private static final String KEY_STYLE_TYPE = "style_type";
    private static final String KEY_COUNTRY = "location_country";
    private static final String KEY_OTA_TIME = "ota_check_time";
    public static final String KEY_EN_US_STT_TYPE = "stt_type";
    public static final String KEY_TTS_KEY = "tts_key";
    private Context mContext;
    //key: serverThreadId
    private HashMap<String, ThreadsBean> mThreads;

    private TranslatorProvider mTranslatorStorage;
    private ILanguageModel mLanguageMode;

    private static TStorageManager instance;


    private TStorageManager(Context context) {
        mContext = context;
        mTranslatorStorage = TranslatorProvider.getInstance();
        mThreads = mTranslatorStorage.getAllGroupThreads();
        mLanguageMode = new LanguageModelImpl(context);
        if (NetUtil.hasConnect(context)) {
            context.startService(new Intent(context, WifiCheckPortalService.class));
        }
    }

    public static void init(Context context) {
        if (instance == null) {
            TranslatorProvider.init(context);
            instance = new TStorageManager(context);
        }
    }

    public MessageBean getMessageBeanById(long id) {
        return mTranslatorStorage.getMessageById(id);
    }

    public static synchronized TStorageManager getInstance() {
        return instance;
    }

    public List<LanguageItem> getLanguageItemList() {
        return mLanguageMode.getLanguageList();
    }

    public LanguageItem getLanguageItem(String code) {
        return mLanguageMode.getLanguageItem(code);
    }

    public ThreadsBean getThreadBean(String serverThreadId) {
        return mThreads.get(serverThreadId);
    }

    public String getStorageFolderName(String serverThreadId) {
        if (getThreadBean(serverThreadId) == null) {
            return AppContext.SOUND_ABSOLUTE_FILE_DIR + "/Temp";
        } else {
            return AppContext.SOUND_ABSOLUTE_FILE_DIR + "/"
                    + getThreadBean(serverThreadId).getStorageFolderName();
        }
    }

    public ArrayList<ThreadsBean> getThreadsByDate(String date) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("strftime('%Y-%m-%d',");
        stringBuffer.append(DbConstants.ThreadsColumns.DATE);
        stringBuffer.append(")=?");
        return mTranslatorStorage.getThreadsWithSelection(stringBuffer.toString(), new String[]{date});
    }

    public ThreadsBean createThread(String serverThreadId, String storageFolderName, int threadType) {
        ThreadsBean threadsBean = new ThreadsBean.Builder().setServerThreadId(serverThreadId)
                .setStorageFolderName(storageFolderName).setThreadType(threadType).setDate(Utils
                        .getCurrentTime("yyyy-MM-dd HH-mm")).build();
        mThreads.put(serverThreadId, threadsBean);
        return threadsBean;
    }

    public void deleteThread(String serverThreadId) {
        Logger.v(TAG, "deleteThread- " + serverThreadId);
        mTranslatorStorage.delete(mThreads.get(serverThreadId));
        mThreads.remove(serverThreadId);
    }

    public void deleteThread(ThreadsBean thread) {
        Logger.v(TAG, "deleteThread: " + thread.getServerThreadId());
        mTranslatorStorage.delete(thread);
        mThreads.remove(thread.getServerThreadId());
    }

    public void closeThread(String serverThreadId) {
        Logger.v(TAG, "closeThread- " + serverThreadId);
        mThreads.remove(serverThreadId);
    }

    public void clean() {
        mTranslatorStorage.clean();
        mThreads.clear();
    }

    public MemberBean getMember(String deviceId) {
        return mTranslatorStorage.getMemberByDeviceId(deviceId);
    }

    public MemberBean createMember(String deviceId, String language) {
        return MemberBean.create(deviceId, "", "", 0, 0, language, "", 0);
    }

    public MemberBean createMember(String deviceId, String name, String nickName,
                                   int sex, int photoId, String language, String description,
                                   int favorite) {
        return MemberBean.create(deviceId, name, nickName, sex, photoId, language, description, favorite);
    }

    public UserBean getUser() {
        return mTranslatorStorage.getUser();
    }

    public boolean isGuest() {
        return getUser().isGuest();
    }

    public void updateUser(UserBean userBean) {
        mTranslatorStorage.update(userBean);
    }

    public boolean isAuto() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_AUTO, true);
    }

    public void setAuto(boolean b) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_AUTO, b).commit();
    }

    public int getTextSize() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_TEXT_SIZE, 1);
    }

    public void setTextSize(int f) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_TEXT_SIZE, f).commit();
    }

    public String getPairedId() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        String deviceId = sp.getString(KEY_PAIR_DEVICE_ID, "");
        return deviceId;
    }

    public void setPairedId(String id) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_PAIR_DEVICE_ID, id).commit();
    }

    /**
     * @return storage type is {@link #STORAGE_TYPE_NOT_SAVE}
     * or {@link #STORAGE_TYPE_SAVE_TEXT}
     * or {@link #STORAGE_TYPE_SAVE_SOUND}
     * or {@link #STORAGE_TYPE_SAVE_TEXT_SOUND}
     */
    public int getStorageType() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        int type = sp.getInt(KEY_STORAGE_TYPE, STORAGE_TYPE_SAVE_TEXT_SOUND);
        Logger.v(TAG, "getStorageType- " + type);
        return type;
    }

    public boolean saveText() {
        return (getStorageType() == STORAGE_TYPE_SAVE_TEXT || getStorageType() == STORAGE_TYPE_SAVE_TEXT_SOUND);
    }

    public boolean saveSound() {
        return (getStorageType() == STORAGE_TYPE_SAVE_SOUND || getStorageType() == STORAGE_TYPE_SAVE_TEXT_SOUND);
    }

    /**
     * @param type is {@link #STORAGE_TYPE_NOT_SAVE}
     *             or {@link #STORAGE_TYPE_SAVE_TEXT}
     *             or {@link #STORAGE_TYPE_SAVE_SOUND}
     *             or {@link #STORAGE_TYPE_SAVE_TEXT_SOUND}
     */
    public void setStorageType(int type) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_STORAGE_TYPE, type).commit();
    }

    //1: 2:
    public void setAdminDefaultPage(int page) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_DEFAULT_PAGE_ADMIN, page).commit();
    }

    public int getAdminDefaultPage() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        int page = sp.getInt(KEY_DEFAULT_PAGE_ADMIN, 1);
        return page;
    }

    //1: 2:
    public void setCustomDefaultPage(int page) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_DEFAULT_PAGE_CUSTOM, page).commit();
    }

    public int getCustomDefaultPage() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        int page = sp.getInt(KEY_DEFAULT_PAGE_CUSTOM, 1);
        return page;
    }

    public void setNoticeText(String noticeId) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_NOTICE_TEXT, noticeId).commit();
    }

    public String getNoticeText() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_NOTICE_TEXT, "");
    }

    public void setNoticePic(String noticeId) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_NOTICE_PIC, noticeId).commit();
    }

    public String getNoticePic() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_NOTICE_PIC, "");
    }

    public String getDeviceId() {
        String deviceId = Build.SERIAL;
        if (TextUtils.isEmpty(deviceId) || "unknown".equalsIgnoreCase(deviceId)) {

            SharedPreferences sp = mContext.getSharedPreferences("translator.cfg", Context.MODE_PRIVATE);
            deviceId = sp.getString("deviceid", "");

            if (TextUtils.isEmpty(deviceId)) {
                String time = Long.toString(System.currentTimeMillis());
                deviceId = time.substring(time.length() - 12);
                sp.edit().putString("deviceid", "").apply();
            }
        }
        return deviceId;
    }

    public void setOtaAutoCheck(boolean auto) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(OTA_AUTO, auto).apply();
    }

    public boolean isOtaAutoCheck() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(OTA_AUTO, false);
    }

    public int getStyleType() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_STYLE_TYPE, AppContext.STYLE_TYPE_DRAK);
    }

    public void setStyleType(int type) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_STYLE_TYPE, type).commit();
    }

    public String getLocationCountry() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        String deviceId = sp.getString(KEY_COUNTRY, "");
        return deviceId;
    }

    public void setLocation(String country) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_COUNTRY, country).commit();
    }

    public void setSTTType(String type) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_EN_US_STT_TYPE, type).commit();
        ToastUtils.showLong("Use " + type);
    }

    public String getSTTType() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        String type = sp.getString(KEY_EN_US_STT_TYPE, AppContext.BAIDU);
        return type;
    }

    public void setTTSKey(String key) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TTS_KEY, key).commit();
        ToastUtils.showLong("Use " + key);
    }

    public String getTTSKey() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        String key = sp.getString(KEY_TTS_KEY, "");
        return key;
    }

    public long getOtaCheckTime() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        long time = sp.getLong(KEY_OTA_TIME, 0L);
        return time;
    }

    public void setOtaCheckTime(long time) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putLong(KEY_OTA_TIME, time).commit();
    }

    private List<IDataCleanListener> mDataCleanListener = new ArrayList<>();

    public void registerDataCleanListener(IDataCleanListener listener) {
        mDataCleanListener.add(listener);
    }

    public void unregisterDataCleanListener(IDataCleanListener listener) {
        mDataCleanListener.remove(listener);
    }

    public void notifyDataCleaned() {
        for (IDataCleanListener listener : mDataCleanListener) {
            listener.onDataCleaned();
        }
    }

    private ArrayList<WifiCaptivePortalListener> mWifiCaptivePortalListeners = new ArrayList<>();
    private Handler mHandler = new Handler();
    private boolean mIsPortal = false;

    public boolean isPortal() {
        return mIsPortal;
    }

    public void saveCaptivePortal(boolean isPortal) {
        mIsPortal = isPortal;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (WifiCaptivePortalListener listener : mWifiCaptivePortalListeners) {
                    listener.onCaptivePortalChanged(mIsPortal);
                }
            }
        });
    }

    public void registerWifiCaptivePortalListener(WifiCaptivePortalListener listener) {
        mWifiCaptivePortalListeners.add(listener);
    }

    public void unregisterWifiCaptivePortalListener(WifiCaptivePortalListener listener) {
        mWifiCaptivePortalListeners.remove(listener);
    }

    public interface WifiCaptivePortalListener {
        void onCaptivePortalChanged(boolean isPortal);
    }
}
