package com.letrans.android.translator.settings.wifi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.CaptivePortal;
import android.net.ConnectivityManager;
import android.net.ICaptivePortal;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.Status;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.BaseDialog;
import com.letrans.android.translator.settings.ItemDivider;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import static com.letrans.android.translator.settings.wifi.WifiCheckPortalService.ACTION_WIFI_CHECK_SERVICE;
import static com.letrans.android.translator.settings.wifi.WifiCheckPortalService.EXTRA_WIFI_PORTAL;

public class WifiSettingFragment extends Fragment implements
        BaseDialog.OnButtonClickListener, WifiAdapter.OnItemClickListener,
        DialogInterface.OnDismissListener {
    private Activity mActivity;
    // Combo scans can take 5-6s to complete - set to 10s.
    private static final int WIFI_RESCAN_INTERVAL_MS = 10 * 1000;
    private static final int MSG_SCAN_WIFI = 1;
    public static final int APP_RETURN_WANTED_AS_IS = 2;

    private View switchContainer;
    private Switch wifiSwitch;
    private CurrentWifiItem mCurrentWifiItem;
    private TextView mTvWifiState;
    private RecyclerView mLvWifi;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayoutManager mLinearLayoutManager;
    private WifiAdapter mWifiAdapter;
    private View emptyView;
    private TextView mSelectView;
    private ProgressBar mProgressBar;

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private Scanner mScanner;

    private WifiManager mWifiManager;
    private DetailedState mLastState;
    private WifiInfo mLastInfo;
    private int mLastPriority;
    private AccessPoint mSelected;
    private boolean mResetNetworks = false;
    private boolean mStateChangeWifi = false;
    private boolean needLoginWifiPortal = false;
    private boolean mConnected = false;

    private WifiDialog mDialog;
    private BaseDialog mToastDialog;
    private boolean mFirstToast;

    private FragmentManager mFragmentManager;

    private Handler mHandler;

    public WifiSettingFragment() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mFilter.addAction(ACTION_WIFI_CHECK_SERVICE);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(intent);
            }
        };

        mScanner = new Scanner();

        mHandler = new Handler();

        mFirstToast = true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mFragmentManager = getFragmentManager();
        mWifiManager = mActivity.getApplicationContext().getSystemService(WifiManager.class);
        mActivity.registerReceiver(mReceiver, mFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mResetNetworks) {
            enableNetworks();
        }
        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (mToastDialog != null) {
            mToastDialog.dismiss();
            mToastDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScanner.pause();
        mActivity.unregisterReceiver(mReceiver);
        if (getActivity().isDestroyed()) {
            return;
        }
        int count = mFragmentManager.getBackStackEntryCount();
        for (int i = 0; i < count; i++) {
            mFragmentManager.popBackStack();
        }
    }

    private void initViews(View view) {
        switchContainer = view.findViewById(R.id.ll_wifi_switch);
        wifiSwitch = (Switch) view.findViewById(R.id.switch_status);
        mCurrentWifiItem = new CurrentWifiItem(view.findViewById(R.id.wifi_connected_item));
        mSwipeRefreshLayout = (SwipeRefreshLayout) view
                .findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mScanner.resume();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 1000);
            }
        });
        mLvWifi = (RecyclerView) view.findViewById(R.id.lv_wifi);
        mTvWifiState = (TextView) view.findViewById(R.id.tv_wifi_state);
        emptyView = view.findViewById(R.id.wifi_empty_view);
        mSelectView = (TextView) view.findViewById(R.id.id_wifi_select);
        mProgressBar = (ProgressBar) view.findViewById(R.id.wifi_progressBar);
        mLinearLayoutManager = new LinearLayoutManager(mActivity,
                LinearLayoutManager.VERTICAL, false);
        mLvWifi.setLayoutManager(mLinearLayoutManager);
        ItemDivider itemDivider = new ItemDivider(mActivity, Utils.dpToPx(58));
        mLvWifi.addItemDecoration(itemDivider);
        mWifiAdapter = new WifiAdapter(mActivity);
        mWifiAdapter.setOnItemClickListener(this);
        mLvWifi.setAdapter(mWifiAdapter);
        mWifiAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mWifiAdapter.getItemCount() > 0) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });

        switchContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiSwitch.setChecked(!wifiSwitch.isChecked());
            }
        });
        wifiSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) {
                    if (!mWifiManager.isWifiEnabled()) {
                        mWifiManager.setWifiEnabled(true);
                    }
                    mSwipeRefreshLayout.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    mSelectView.setVisibility(View.VISIBLE);
                    mWifiManager.startScan();
                } else {
                    if (mWifiManager.isWifiEnabled()) {
                        mWifiManager.setWifiEnabled(false);
                    }
                    mSwipeRefreshLayout.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    mSelectView.setVisibility(View.GONE);
                    mCurrentWifiItem.setVisible(false);
                }
            }
        });
        setWiFiSwitch(false);
    }

    private void showDialog(AccessPoint accessPoint) {
        if (mDialog != null && mDialog.isVisible()) {
            mDialog.dismiss();
            mDialog = null;
        }
        mDialog = new WifiDialog();
        mDialog.setAccessPoint(accessPoint);
        mDialog.setButtonListener(this);
        mDialog.show(mFragmentManager, "wifi_dialog");
    }

    private void forget(int networkId) {
        mWifiManager.removeNetwork(networkId);
        saveNetworks();
    }

    private void connect(int networkId) {
        if (networkId == -1) {
            return;
        }

        // Reset the priority of each network if it goes too high.
        if (mLastPriority > 1000000) {
            final ArrayList<AccessPoint> accessPointList = mWifiAdapter.getAccessPointList();
            if (null == accessPointList) {
                return;
            }

            for (int i = accessPointList.size() - 1; i >= 0; --i) {
                AccessPoint accessPoint = accessPointList.get(i);
                if (accessPoint.networkId != -1) {
                    WifiConfiguration config = new WifiConfiguration();
                    config.networkId = accessPoint.networkId;
                    config.priority = 0;
                    mWifiManager.updateNetwork(config);
                }
            }
            mLastPriority = 0;
        }

        // Set to the highest priority and save the configuration.
        WifiConfiguration config = new WifiConfiguration();
        config.networkId = networkId;
        config.priority = ++mLastPriority;
        mWifiManager.updateNetwork(config);
        saveNetworks();

        // Connect to network by disabling others.
        mWifiManager.enableNetwork(networkId, true);
        mWifiManager.reconnect();
        mResetNetworks = true;
    }

    private void connectTo(int networkId) {
        Logger.d("sqm", "connectTo networkId: " + networkId);
        try {
            Class clzActionListener = Class.forName("android.net.wifi.WifiManager$ActionListener");
            Method methodConnect = WifiManager.class.getMethod("connect",
                    int.class, clzActionListener);
            methodConnect.invoke(mWifiManager, networkId, null);
        } catch (Exception e) {
            Logger.d("sqm", "connectTo exception: " + e.getMessage());
        }
    }

    private void connectTo(WifiConfiguration config) {
        Logger.d("sqm", "connectTo config.networkId: " + config.networkId);
        try {
            Class clzActionListener = Class.forName("android.net.wifi.WifiManager$ActionListener");
            Method methodConnect = WifiManager.class.getMethod("connect",
                    WifiConfiguration.class, clzActionListener);
            methodConnect.invoke(mWifiManager, config, null);
        } catch (Exception e) {
            Logger.d("sqm", "connectTo exception: " + e.getMessage());
        }
    }

    private void forgetFrom(int networkId) {
        Logger.d("sqm", "forgetFrom networkId: " + networkId);
        try {
            Class clzActionListener = Class.forName("android.net.wifi.WifiManager$ActionListener");
            Method methodConnect = WifiManager.class.getMethod("forget",
                    int.class, clzActionListener);
            methodConnect.invoke(mWifiManager, networkId, null);
        } catch (Exception e) {
            Logger.d("sqm", "forgetFrom exception: " + e.getMessage());
        }
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();
    }

    private void enableNetworks() {
        final ArrayList<AccessPoint> accessPointList = mWifiAdapter.getAccessPointList();
        if (null == accessPointList) {
            return;
        }

        for (int i = accessPointList.size() - 1; i >= 0; --i) {
            WifiConfiguration config = accessPointList.get(i).getConfig();
            if (config != null && config.status != Status.ENABLED) {
                mWifiManager.enableNetwork(config.networkId, false);
            }
        }
        mResetNetworks = false;
    }

    private void saveNetworks() {
        // Always save the configuration with all networks enabled.
        enableNetworks();
        mWifiManager.saveConfiguration();
        updateAccessPoints();
    }

    private void handleEvent(Intent intent) {
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(action)) {
            if (mSelected != null && mSelected.networkId != -1) {
                mSelected = null;
            }
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            if (!mConnected) {
                updateConnectionState(WifiInfo.getDetailedStateOf((SupplicantState)
                        intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            mConnected = networkInfo.isConnected();
            updateConnectionState(networkInfo.getDetailedState());
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);
        } else if ((ConnectivityManager.CONNECTIVITY_ACTION).equals(action)) {
            /*
            Intent serviceIntent = new Intent(mActivity, WifiCheckPortalService.class);
            mActivity.startService(serviceIntent);
            */
        } else if (ACTION_WIFI_CHECK_SERVICE.equals(action)) {
            needLoginWifiPortal = intent.getIntExtra(EXTRA_WIFI_PORTAL, 0) == 1;
            if (needLoginWifiPortal) {
                showDialog();
            }
        }
    }

    private void updateConnectionState(DetailedState state) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }

        if (state == DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }

        mLastInfo = mWifiManager.getConnectionInfo();
        if (state != null) {
            mLastState = state;
        }

        final ArrayList<AccessPoint> accessPointList = mWifiAdapter.getAccessPointList();
        if (null == accessPointList) {
            return;
        }

        WifiConfiguration connectionConfig = null;
        if (mLastInfo != null) {
            connectionConfig = getWifiConfigurationForNetworkId(mLastInfo.getNetworkId());
        }
        boolean update = false;
        for (int i = accessPointList.size() - 1; i >= 0; --i) {
            AccessPoint accessPoint = accessPointList.get(i);
            if (accessPoint.update(connectionConfig, mLastInfo, mLastState)) {
                update = true;
                mCurrentWifiItem.bindView(accessPoint);
                mWifiAdapter.remove(i);
            }
        }
        if (!update) {
            AccessPoint accessPoint = mCurrentWifiItem.getAccessPoint();
            if (accessPoint != null && accessPoint.update(connectionConfig, mLastInfo, mLastState)) {
                update = true;
                mCurrentWifiItem.bindView(accessPoint);
            }
        }
        if (mLastInfo != null && mLastState != null) {
            mCurrentWifiItem.setVisible(update);
        }
        update &= state != null;
        if (update) {
            // mWifiAdapter.sort();
            // mLinearLayoutManager.scrollToPosition(0);
            changeWiFiConnectInfo();
        }
    }

    private WifiConfiguration getWifiConfigurationForNetworkId(int networkId) {
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (mLastInfo != null && networkId == config.networkId) {
                    return config;
                }
            }
        }
        return null;
    }

    private void updateWifiState(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                mProgressBar.setVisibility(View.VISIBLE);
                showWiFiState(R.string.wifi_starting);
                wifiSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                setWiFiSwitch(true);
                wifiSwitch.setEnabled(true);
                mScanner.resume();
                updateAccessPoints();
                return;
            case WifiManager.WIFI_STATE_DISABLING:
                showWiFiState(R.string.wifi_stopping);
                wifiSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                setWiFiSwitch(false);
                showWiFiState(R.string.wifi_closed);
                wifiSwitch.setEnabled(true);
                mScanner.pause();
                mWifiAdapter.setAccessPointList(null);
                break;
            default:
                setWiFiSwitch(false);
                wifiSwitch.setEnabled(true);
        }

        mLastInfo = null;
        mLastState = null;
    }

    private void setWiFiSwitch(boolean switched) {
        if (wifiSwitch.isChecked() == switched) {
            if (switched) {
                mSwipeRefreshLayout.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                mSelectView.setVisibility(View.VISIBLE);
            } else {
                mSwipeRefreshLayout.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                mSelectView.setVisibility(View.GONE);
                mCurrentWifiItem.setVisible(false);
            }
            return;
        }
        mStateChangeWifi = true;
        wifiSwitch.setChecked(switched);
        mStateChangeWifi = false;
    }

    private void showWiFiState(int resid) {
        mTvWifiState.setText(resid);
    }

    private void updateAccessPoints() {
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();

        WifiConfiguration connectionConfig = null;
        if (mLastInfo != null) {
            connectionConfig = getWifiConfigurationForNetworkId(mLastInfo.getNetworkId());
        }
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            mLastPriority = 0;
            boolean visible = false;
            for (WifiConfiguration config : configs) {
                if (config.priority > mLastPriority) {
                    mLastPriority = config.priority;
                }

                // Shift the status to make enableNetworks() more efficient.
                if (config.status == Status.CURRENT) {
                    config.status = Status.ENABLED;
                } else if (mResetNetworks && config.status == Status.DISABLED) {
                    config.status = Status.CURRENT;
                }

                AccessPoint accessPoint = new AccessPoint(mActivity, config);
                boolean changed = false;
                if (mLastInfo != null && mLastState != null) {
                    if (config.isPasspoint() == false) {
                        changed = accessPoint.update(connectionConfig, mLastInfo, mLastState);
                    }
                }
                if (changed) {
                    visible = true;
                    mCurrentWifiItem.bindView(accessPoint);
                } else {
                    accessPoints.add(accessPoint);
                }
            }
            mCurrentWifiItem.setVisible(visible);
        }

        List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }

                boolean found = false;
                for (AccessPoint accessPoint : accessPoints) {
                    if (accessPoint.update(result)) {
                        found = true;
                    } else if (mCurrentWifiItem.getAccessPoint() != null
                            && mCurrentWifiItem.getAccessPoint().update(result)) {
                        found = true;
                    }
                }
                if (!found) {
                    accessPoints.add(new AccessPoint(mActivity, result));
                }
            }
        }

        mWifiAdapter.setAccessPointList(accessPoints);
        changeWiFiConnectInfo();
    }

    @Override
    public void onItemClick(int position) {
        onItemClick((AccessPoint) mWifiAdapter.getItem(position));
    }

    public void onItemClick(AccessPoint accessPoint) {
        mSelected = accessPoint;
        // AccessPoint accessPoint = mAccessPointList.get(position);
        // DetailedState state = accessPoint.getState();
        showDialog(mSelected);
    }

    @Override
    public void onItemLongClick(int position) {

    }

    @Override
    public void onDetailsClick(int position) {
        onDetailsClick();
    }

    private void onDetailsClick() {
        if (mScanner != null) {
            mScanner.pause();
        }

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        WifiInfoFragment wifiInfoFragment = new WifiInfoFragment();
        wifiInfoFragment.setParentFragment(this);
        wifiInfoFragment.setWifiPortal(needLoginWifiPortal);
        AccessPoint accessPoint = mCurrentWifiItem.getAccessPoint();
        wifiInfoFragment.setHasNetwork(!accessPoint.connectedNoInternet);
        fragmentTransaction.add(R.id.wifi_info_container, wifiInfoFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void refresh() {
        Logger.d("sqm", "WifiSettingFragment.refresh");
        mWifiAdapter.setAccessPointList(null);
        if (mWifiManager.isWifiEnabled() && mScanner != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            mScanner.resume();
            updateAccessPoints();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mToastDialog = null;
    }

    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(MSG_SCAN_WIFI)) {
                sendEmptyMessage(MSG_SCAN_WIFI);
            }
        }

        void pause() {
            mRetry = 0;
            removeMessages(MSG_SCAN_WIFI);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                Toast.makeText(mActivity, R.string.wifi_fail_to_scan,
                        Toast.LENGTH_LONG).show();
                return;
            }
            sendEmptyMessageDelayed(MSG_SCAN_WIFI, WIFI_RESCAN_INTERVAL_MS);
        }
    }

    @Override
    public void onClick(int button) {
        if (button == WifiDialog.BUTTON_FORGET && mSelected != null) {
            forgetFrom(mSelected.networkId);
        } else if (button == WifiDialog.BUTTON_SUBMIT && mDialog != null) {
            WifiConfiguration config = mDialog.getConfig();
            if (config == null) {
                if (mSelected != null && mSelected.networkId != -1) {
                    connectTo(mSelected.getConfig());
                }
            } else {
                connectTo(config);
            }
        }
    }

    private void changeWiFiConnectInfo() {
        final ArrayList<AccessPoint> accessPointList = mWifiAdapter.getAccessPointList();
        if (null == accessPointList || accessPointList.size() < 1) {
            return;
        }

        boolean change = false;
        AccessPoint accessPoint;
        for (int i = accessPointList.size() - 1; i >= 0; --i) {
            accessPoint = accessPointList.get(i);
            if (accessPoint.getInfo() != null) {
                change = true;
            }
        }

        if (!change) {
        }
    }

    private void showDialog() {
        if (mToastDialog != null && !mFirstToast) {
            return;
        }
        mFirstToast = false;
        BaseDialog.Builder builder = new BaseDialog.Builder(mActivity);
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
                                // startCaptivePortalApp();
                                break;
                            case BaseDialog.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                });
        mToastDialog = builder.build();
        mToastDialog.setDialogDismissListener(this);
        mToastDialog.show(mFragmentManager, "wifi_login_dialog");
    }

    private void startCaptivePortalApp() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final Intent intent = new Intent(ConnectivityManager.ACTION_CAPTIVE_PORTAL_SIGN_IN);
        Network network = connectivityManager.getActiveNetwork();
        intent.putExtra(ConnectivityManager.EXTRA_NETWORK, network);
        ICaptivePortal.Stub stub = new ICaptivePortal.Stub() {
            @Override
            public void appResponse(int response) {
                if (response == APP_RETURN_WANTED_AS_IS) {
                    getActivity().enforceCallingPermission(
                            "android.permission.CONNECTIVITY_INTERNAL",
                            "CaptivePortal");
                }
            }
        };
        Class cls = CaptivePortal.class;
        Class[] params = {IBinder.class};
        Object object = null;
        try {
            Constructor constructor = cls.getConstructor(params);
            object = constructor.newInstance(stub);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        intent.putExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL, (Parcelable) object);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    class CurrentWifiItem {
        private View itemView;
        private TextView mTvTitle;
        private ImageView mIvSignal;
        private ImageView mWifiConnectedStatus;
        private ImageView mWifiDetailsArrow;
        private ImageView mLockStateView;
        private TextView mSummaryView;

        private AccessPoint mAccessPoint;

        public CurrentWifiItem(View itemView) {
            this.itemView = itemView;
            mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            mIvSignal = (ImageView) itemView.findViewById(R.id.iv_signal);
            mWifiConnectedStatus = (ImageView) itemView.findViewById(R.id.wifi_connected_status);
            mWifiDetailsArrow = (ImageView) itemView.findViewById(R.id.wifi_details_arrow);
            mLockStateView = (ImageView) itemView.findViewById(R.id.wifi_lock_state);
            mSummaryView = (TextView) itemView.findViewById(R.id.wifi_summary);
        }

        public void bindView(AccessPoint accessPoint) {
            mAccessPoint = accessPoint;
            mTvTitle.setText(accessPoint.ssid);
            NetworkInfo.DetailedState state = accessPoint.getState();
            if (state != null) {
                if ("CONNECTED".equals(state.toString())) {
                    setDetailClickListener();
                    mWifiConnectedStatus.setVisibility(View.VISIBLE);
                    if (accessPoint.connectedNoInternet) {
                        mSummaryView.setText(accessPoint.summary);
                        mSummaryView.setVisibility(View.VISIBLE);
                    } else {
                        mSummaryView.setVisibility(View.GONE);
                    }
                } else {
                    setClickListener(accessPoint);
                    mWifiConnectedStatus.setVisibility(View.INVISIBLE);
                    mSummaryView.setText(accessPoint.summary);
                    mSummaryView.setVisibility(View.VISIBLE);
                }
            } else {
                setClickListener(accessPoint);
                mWifiConnectedStatus.setVisibility(View.INVISIBLE);
                mSummaryView.setVisibility(View.GONE);
            }
            accessPoint.updateSecurityView(mLockStateView);
            accessPoint.setImageSignal(mIvSignal);
        }

        public void setVisible(boolean visible) {
            itemView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        public AccessPoint getAccessPoint() {
            return mAccessPoint;
        }

        private void setDetailClickListener() {
            itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onDetailsClick();
                }
            });
        }

        private void setClickListener(final AccessPoint accessPoint) {
            itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onItemClick(accessPoint);
                }
            });
        }
    }
}
