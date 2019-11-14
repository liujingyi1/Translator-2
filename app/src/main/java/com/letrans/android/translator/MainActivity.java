package com.letrans.android.translator;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import com.letrans.android.translator.composemessage.ComposeMessageActivity;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.home.HomeActivity;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.settings.SettingsActivity;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.ui.GuideActivity;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.utils.ToastUtils;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RTranslator/MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (TStorageManager.getInstance().getUser().getRole() == UserBean.ROLE_TYPE_GUEST) {
            if (TStorageManager.getInstance().getCustomDefaultPage() == 1) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(MainActivity.this, GuideActivity.class);
                startActivity(intent);
                finish();
            }
        } else if(TStorageManager.getInstance().getUser().getRole() == UserBean.ROLE_TYPE_OWNER){
            if (TStorageManager.getInstance().getAdminDefaultPage() == 1) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (!NetUtil.isNetworkConnected(getApplicationContext())
                        || (nc != null && !nc.hasCapability(nc.NET_CAPABILITY_VALIDATED))
                        || TStorageManager.getInstance().isPortal()) {
                    Logger.i(TAG, "No network !");
                    startActivity(new Intent(AppContext.ACTION_NETWORK_SETTING));
                    finish();
                } else if (TextUtils.isEmpty(TStorageManager.getInstance().getPairedId())) {
                    Logger.i(TAG, "No paired !");
                    startActivity(new Intent(AppContext.ACTION_PAIR_SETTING));
                    finish();
                } else {
                     gotoComposemessage();
                }

            }
        }
    }

    private void gotoComposemessage () {
        final int userRole = UserBean.getUser().getRole();
        final String language = TStorageManager.getInstance().getUser().getLanguage();
        final String deviceId = TStorageManager.getInstance().getDeviceId();
        ServerApi.getInstance()
                .getRole(TStorageManager.getInstance().getPairedId(), new ServerApi.ServerCallback() {
                    @Override
                    public void call(String result, int code) {
                        if (code == ServerApi.SR_REQUEST_SUCCESS_CODE) {
                            int pairRole = Integer.parseInt(result);
                            if (userRole == UserBean.ROLE_TYPE_OWNER && pairRole == UserBean.ROLE_TYPE_OWNER) {
                                ToastUtils.showLong(R.string.only_one_worker_accept);
                                startActivity(new Intent(AppContext.ACTION_ROLE_SETTING));
                                finish();
                            } else{
                                ServerApi.getInstance().enterCompose(deviceId,language, new ServerApi.ServerCallback() {
                                            @Override
                                            public void call(String result, int code) {
                                                if (code == ServerApi.SR_SEND_TIMEOUT_CODE) {
                                                    ServerApi.getInstance().enterCompose(deviceId,language, new ServerApi.ServerCallback() {
                                                                @Override
                                                                public void call(String result, int code) {
                                                                    Intent composeActivityIntent = new Intent(MainActivity.this, ComposeMessageActivity.class);
                                                                    startActivity(composeActivityIntent);
                                                                    finish();
                                                                }
                                                            });
                                                } else {
                                                    Intent composeActivityIntent = new Intent(MainActivity.this, ComposeMessageActivity.class);
                                                    startActivity(composeActivityIntent);
                                                    finish();
                                                }
                                            }
                                        });
                            }
                        } else {
                            gotoSettings();
                        }
                    }
                });
    }

    private void gotoSettings() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        finish();
    }
}
