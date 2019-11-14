package com.letrans.android.translator.settings.ota;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import com.letrans.android.translator.utils.AppUtils;
import com.letrans.android.translator.utils.Logger;

public class BackgroundInstallTask extends AsyncTask<String, Void, Void> {

    private static final String TAG = "RTranslator/BackgroundInstallTask";
    private Context context;
    private String packageName;

    public BackgroundInstallTask(Context context) {
        this.context = context;
        packageName = AppUtils.getPackageName(context);
    }


    @SuppressLint("WrongConstant")
    @Override
    protected Void doInBackground(String... params) {
        int result = OtaTools.installSilentApk(context, params[0]);
        Logger.d(TAG,"result : " + result);
        return null;
    }
}

