package com.letrans.android.translator.settings.wifi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.InfoAdapter;
import com.letrans.android.translator.settings.InfoObject;
import com.letrans.android.translator.settings.ItemDivider;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;

public class WifiInfoFragment extends Fragment implements OnClickListener,
        TStorageManager.WifiCaptivePortalListener {
    private final String TAG = "RTranslator/WifiInfoFragment";
    private Context mContext;

    private TextView mButtonForgerWifi;
    private TextView mButtonSign;
    private RecyclerView mRecyclerView;
    private InfoAdapter mInfoAdapter;

    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    private DhcpInfo dhcpInfo;

    private WifiSettingFragment mParentFragment;

    private boolean mNeedLogin;
    private boolean mHasNetwork;

    public WifiInfoFragment() {
        TStorageManager.getInstance().registerWifiCaptivePortalListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wifi_connected_info, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.wifi_info_list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext,
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.addItemDecoration(new ItemDivider(mContext));
        mInfoAdapter = new InfoAdapter(mContext);
        mRecyclerView.setAdapter(mInfoAdapter);

        mButtonForgerWifi = (TextView) view.findViewById(R.id.button_forget);
        mButtonSign = (TextView) view.findViewById(R.id.button_wifi_sign);
        if (mNeedLogin) {
            mButtonSign.setVisibility(View.VISIBLE);
        } else {
            mButtonSign.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        dhcpInfo = mWifiManager.getDhcpInfo();
        mWifiInfo = mWifiManager.getConnectionInfo();
        setWifiPortal(TStorageManager.getInstance().isPortal() || !mHasNetwork);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mWifiInfo && null != mWifiInfo.getSSID()) {
            mInfoAdapter.addInfoObjects(getInfoObjects());
            mButtonForgerWifi.setOnClickListener(this);
            mButtonSign.setOnClickListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TStorageManager.getInstance().unregisterWifiCaptivePortalListener(this);
    }

    private ArrayList<InfoObject> getInfoObjects() {
        ArrayList<InfoObject> list = new ArrayList<>();
        list.add(new InfoObject(
                getString(R.string.wifi_name), mWifiInfo.getSSID()));
        list.add(new InfoObject(
                getString(R.string.wifi_status), getString(R.string.wifi_connected)));
        list.add(new InfoObject(
                getString(R.string.wifi_signal), mWifiInfo.getRssi() + "dbm"));
        list.add(new InfoObject(
                getString(R.string.wifi_ip_address), intToIp(dhcpInfo.ipAddress)));
        list.add(new InfoObject(
                getString(R.string.wifi_netmark), intToIp(dhcpInfo.netmask)));
        list.add(new InfoObject(
                getString(R.string.wifi_gateway), intToIp(dhcpInfo.gateway)));
        return list;
    }

    private String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }

    private void removeWifiBySsid(WifiManager wifiManager, String targetSsid) {
        List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfig : wifiConfigs) {
            String ssid = wifiConfig.SSID;
            if (ssid.equals(targetSsid)) {
                wifiManager.removeNetwork(wifiConfig.networkId);
                wifiManager.saveConfiguration();
            }
        }
    }

    private void forgetFrom(int networkId) {
        Logger.d("sqm", "WifiInfoFragment.forgetFrom networkId: " + networkId);
        try {
            Class clzActionListener = Class.forName("android.net.wifi.WifiManager$ActionListener");
            Method methodConnect = WifiManager.class.getMethod("forget",
                    int.class, clzActionListener);
            methodConnect.invoke(mWifiManager, networkId, null);
        } catch (Exception e) {
            Logger.d("sqm", "forgetFrom exception: " + e.getMessage());
        }
    }

    public void setWifiPortal(boolean needLogin) {
        mNeedLogin = needLogin;
    }

    public void setParentFragment(WifiSettingFragment parentFragment) {
        mParentFragment = parentFragment;
    }

    public void setHasNetwork(boolean hasNetwork) {
        mHasNetwork = hasNetwork;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_forget:
                forgetFrom(mWifiInfo.getNetworkId());
                mParentFragment.refresh();
                getActivity().onBackPressed();
                break;

            case R.id.button_wifi_sign:
                if (mNeedLogin) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri content_url = Uri.parse("http://captive.apple.com");
                    intent.setData(content_url);
                    startActivity(intent);
                }
        }
    }

    @Override
    public void onCaptivePortalChanged(boolean isPortal) {
        setWifiPortal(isPortal);
        if (mButtonSign == null) {
            return;
        }
        if (isPortal) {
            mButtonSign.setVisibility(View.VISIBLE);
        } else {
            mButtonSign.setVisibility(View.GONE);
        }
    }
}
