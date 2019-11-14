
package com.letrans.android.translator.settings.wifi;

import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.BaseDialog;

public class WifiDialog extends BaseDialog implements View.OnClickListener,
        TextWatcher {
    static final int BUTTON_SUBMIT = BaseDialog.BUTTON_POSITIVE;
    static final int BUTTON_FORGET = BaseDialog.BUTTON_NEUTRAL;

    private AccessPoint mAccessPoint;

    private View mView;
    private int mSecurity;
    private TextView mPassword;
    private CheckBox mShowPassword;

    public WifiDialog() {
        super();
    }

    public void setAccessPoint(AccessPoint accessPoint) {
        mAccessPoint = accessPoint;
        mSecurity = (accessPoint == null) ? AccessPoint.SECURITY_NONE : accessPoint.security;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        init();
    }

    @Override
    protected void bindView() {
        super.bindView();
        if (mAccessPoint != null && mAccessPoint.networkId == -1) {
            mView = LayoutInflater.from(mContext).inflate(R.layout.wifi_dialog, null);
            mContentView.setVisibility(View.VISIBLE);
            mContentView.addView(mView);
            showSecurityFields();
        }

        if (getButton(BUTTON_SUBMIT).getVisibility() == View.VISIBLE) {
            validate();
        }
    }

    private void init() {
        if (mAccessPoint == null) {
            return;
        } else {
            setTitle(mAccessPoint.ssid);

            DetailedState state = mAccessPoint.getState();
            int level = mAccessPoint.getLevel();
            if (state == null && level != -1) {
                setPositiveButton(R.string.wifi_connect);
            }

            if (mAccessPoint.networkId != -1) {
                setNeutralButton(R.string.wifi_forget);
            }
        }
        setNegativeButton(R.string.wifi_cancel);
    }

    WifiConfiguration getConfig() {
        if (mAccessPoint != null && mAccessPoint.networkId != -1) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();

        if (mAccessPoint == null) {
            return null;
        } else if (mAccessPoint.networkId == -1) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mAccessPoint.ssid);
        } else {
            config.networkId = mAccessPoint.networkId;
        }

        switch (mSecurity) {
            case AccessPoint.SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;

            case AccessPoint.SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (mPassword.length() != 0) {
                    int length = mPassword.length();
                    String password = mPassword.getText().toString();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                return config;

            case AccessPoint.SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if (mPassword.length() != 0) {
                    String password = mPassword.getText().toString();
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                return config;
        }
        return null;
    }

    private void validate() {
        // TODO: make sure this is complete.
        if ((mAccessPoint.networkId == -1 &&
                ((mSecurity == AccessPoint.SECURITY_WEP && mPassword.length() == 0) ||
                        (mSecurity == AccessPoint.SECURITY_PSK && mPassword.length() < 8)))) {
            getButton(BUTTON_SUBMIT).setEnabled(false);
        } else {
            getButton(BUTTON_SUBMIT).setEnabled(true);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        validate();
    }

    private void showSecurityFields() {
        if (mSecurity == AccessPoint.SECURITY_NONE) {
            mView.findViewById(R.id.fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.fields).setVisibility(View.VISIBLE);

        if (mPassword == null) {
            mPassword = (TextView) mView.findViewById(R.id.password);
            mPassword.addTextChangedListener(this);
            mShowPassword = (CheckBox) mView.findViewById(R.id.show_password);
            mShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mPassword.setInputType(
                            InputType.TYPE_CLASS_TEXT | (isChecked ?
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD));
                }
            });

            if (mAccessPoint != null && mAccessPoint.networkId != -1) {
                mPassword.setHint(R.string.wifi_unchanged);
            }
        }
    }

}
