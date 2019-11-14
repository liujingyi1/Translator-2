package com.letrans.android.translator.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import com.letrans.android.translator.settings.SystemProxy;

import java.io.File;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Utils {
    public static int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static int pxToDp(float px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * 随机生成文件名称
     *
     * @return
     */
    public static String generateAmrFileName() {
        return UUID.randomUUID().toString() + ".amr";
    }

    public static String generateFileName() {
        return UUID.randomUUID().toString();
    }

    public static String getPcmAbsolutePath(String dir, String fileName) {
        File file = new File(dir, fileName + ".pcm");
        return file.getAbsolutePath();
    }

    public static String getWavAbsolutePath(String dir, String fileName) {
        File file = new File(dir, fileName + ".wav");
        return file.getAbsolutePath();
    }

    public static String getCurrentTime() {
        return getCurrentTime("yyyy-MM-dd-HH-mm-ss-SSS");
    }

    public static String getCurrentTime(String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date());
    }

    public static int getSoundLevel(long readsize, byte[] audiodata) {
        short cAmplitude = 0;
        for (int i=0; i<readsize/2; i++) {
            short curSample = (short)(audiodata[i*2] | (audiodata[i*2+1] << 8));
            if (curSample > cAmplitude) {
                cAmplitude = curSample;
            }
        }
        return cAmplitude;
    }

    public static int convertLevel(int level) {
        return level / 2500 + 1;
    }

    public static String getEncryptString(Context context, int id) {
        if (id > 0) {
            String s = context.getString(id);
            try {
                String decryStr = AES.decrypt(s);
                if (!TextUtils.isEmpty(decryStr)) {
                    return decryStr;
                } else {
                    return s;
                }
            } catch (Exception e) {
                return s;
            }
        }
        return null;
    }

    public static void setData(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        Log.i("jingyi", "year="+year+" month="+month+" day="+day+" hour="+hour+" minute="+minute+" second="+second);

        SystemProxy.getInstance().setDate(year, month, day);
        SystemProxy.getInstance().setTime(hour, minute);
    }
}
