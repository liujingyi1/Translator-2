package com.letrans.android.translator.settings.ota.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.letrans.android.translator.utils.Logger;

public class AppInstalledReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        Logger.d("RTranslator/AppInstalledReceiver","action : " + action);
        if (action != null && (action.equals(Intent.ACTION_PACKAGE_ADDED)
                || action.equals(Intent.ACTION_PACKAGE_REPLACED)
                || action.equals(Intent.ACTION_MY_PACKAGE_REPLACED))) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (packageName.equals("com.letrans.android.translator")) {
                android.os.Process.killProcess(android.os.Process.myPid());
                startHomeActivity(context);
            }

        }
    }

    private void startHomeActivity(Context context) {
        Intent intent = new Intent();
        intent.setClassName("com.letrans.android.translator","com.letrans.android.translator.MainActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
