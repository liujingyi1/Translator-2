package com.letrans.android.translator.settings.wifi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.BaseDialog;

public class WifiLoginDialog extends Activity implements DialogInterface.OnDismissListener {
    private BaseDialog mToastDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mToastDialog != null) {
            mToastDialog.dismiss();
            mToastDialog = null;
        }
    }

    private void showDialog() {
        if (mToastDialog != null) {
            return;
        }
        BaseDialog.Builder builder = new BaseDialog.Builder(this);
        builder.setTitle(R.string.wifi_login_toast)
                .setMessage(R.string.wifi_login_message)
                .setPositiveButton(R.string.wifi_login_ok)
                .setNegativeButton(R.string.wifi_login_cancel)
                .setButtonListener(new BaseDialog.OnButtonClickListener() {
                    @Override
                    public void onClick(int which) {
                        switch (which) {
                            case BaseDialog.BUTTON_POSITIVE:
                                Intent intent = new Intent();
                                intent.setAction("android.intent.action.VIEW");
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Uri content_url = Uri.parse("http://captive.apple.com");
                                intent.setData(content_url);
                                startActivity(intent);
                                break;
                            case BaseDialog.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                });
        mToastDialog = builder.build();
        mToastDialog.setDialogDismissListener(this);
        mToastDialog.show(getFragmentManager(), "wifi_login_dialog");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mToastDialog = null;
        finish();
    }
}
