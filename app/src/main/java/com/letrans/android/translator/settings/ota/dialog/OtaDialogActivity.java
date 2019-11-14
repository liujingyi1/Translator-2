package com.letrans.android.translator.settings.ota.dialog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.R;

public class OtaDialogActivity extends AppCompatActivity implements View.OnClickListener {

    TextView cancel, ok;
    TextView message_title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_dialog);
        setTitle(null);
        ok = (TextView) findViewById(R.id.ota_ok);
        cancel = (TextView) findViewById(R.id.ota_cancel);
        message_title = (TextView) findViewById(R.id.message_title);
        String versionName = getIntent().getStringExtra("versionName");
        if (!TextUtils.isEmpty(versionName)) {
            message_title.setText(getString(R.string.ota_discover_new_version) + "v"+ versionName);
        }
        ok.setOnClickListener(this);
        cancel.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ota_ok: {
                startOtaFragment();
                finish();
                break;
            }
            case R.id.ota_cancel: {
                finish();
                break;
            }
            default:
                break;
        }
    }

    private void startOtaFragment() {
        startActivity(new Intent(AppContext.ACTION_OTA_SETTING));
    }
}
