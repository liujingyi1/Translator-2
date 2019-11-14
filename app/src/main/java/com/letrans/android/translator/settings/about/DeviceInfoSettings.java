package com.letrans.android.translator.settings.about;

import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.InfoAdapter;
import com.letrans.android.translator.settings.InfoObject;
import com.letrans.android.translator.settings.ItemDivider;
import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class DeviceInfoSettings
        implements InfoAdapter.OnItemClickListener {
    private Context mContext;
    private RecyclerView mRecyclerView;
    private InfoAdapter mInfoAdapter;

    static final int TAPS_TO_SECRET_CODE = 5;
    long[] mHits = new long[3];
    private static long mLastClickTime = 0;
    int mDevHitCountdown;
    Toast mDevHitToast;
    boolean mStartClick = false;
    private SecretCodeDialog mSecretDialog;
    private FragmentManager mFragmentManager;

    private boolean initialized;

    private IntentFilter mConnectivityIntentFilter;
    private static final String[] CONNECTIVITY_INTENTS = {
            BluetoothAdapter.ACTION_STATE_CHANGED,
            ConnectivityManager.CONNECTIVITY_ACTION,
            "android.net.wifi.LINK_CONFIGURATION_CHANGED",
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
    };
    private static final int EVENT_UPDATE_CONNECTIVITY = 600;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_CONNECTIVITY:
                    updateWifiStatus();
                    break;
            }
        }
    };

    private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /*if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(EVENT_UPDATE_CONNECTIVITY);
            }*/
            if (contains(CONNECTIVITY_INTENTS, action)) {
                mHandler.sendEmptyMessage(EVENT_UPDATE_CONNECTIVITY);
            }
        }
    };

    public DeviceInfoSettings(Context context) {
        mContext = context;
        initialized = false;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.mFragmentManager = fragmentManager;
    }

    public void onCreateView(View v) {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.recycle_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext,
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        ItemDivider itemDivider = new ItemDivider(mContext);
        mRecyclerView.addItemDecoration(itemDivider);
        mInfoAdapter = new InfoAdapter(mContext);
        mInfoAdapter.setItemClickListener(this);
        mRecyclerView.setAdapter(mInfoAdapter);
    }

    private ArrayList<InfoObject> getInfoObjects() {
        Resources res = mContext.getResources();
        ArrayList<InfoObject> list = new ArrayList<>();
        list.add(getDeviceName(res));
        list.add(getDeviceModel(res));
        list.add(getSerialNumber(res));
        list.add(getMacAddress(res));
        list.add(getDeviceVersion(res));
        return list;
    }

    public void onStart() {

    }

    public void onResume() {
        mConnectivityIntentFilter = new IntentFilter();
        for (String action : CONNECTIVITY_INTENTS) {
            mConnectivityIntentFilter.addAction(action);
        }
        mContext.registerReceiver(mConnectivityReceiver, mConnectivityIntentFilter,
                android.Manifest.permission.CHANGE_NETWORK_STATE, null);

        mDevHitCountdown = TAPS_TO_SECRET_CODE;
        mDevHitToast = null;

        Logger.d("DeviceInfoSettings", "onResume: initialized=" + initialized);
        if (!initialized) {
            // new DeviceInfoLoader().execute();
            mInfoAdapter.addInfoObjects(getInfoObjects());
            initialized = true;
        }
    }

    public void onPause() {
        mContext.unregisterReceiver(mConnectivityReceiver);
        if (mSecretDialog != null) {
            mSecretDialog.dismiss();
        }
    }

    public void onStop() {

    }

    private void updateWifiStatus() {
        if (mInfoAdapter == null || mInfoAdapter.getDataList().isEmpty()) {
            return;
        }
        InfoObject infoObject = mInfoAdapter.getDataList().get(3);
        infoObject.value = SystemProxy.getInstance().getWifiMacAddress();
        mInfoAdapter.notifyItemChanged(3);
    }

    private InfoObject getDeviceName(Resources res) {
        return new InfoObject(res.getString(R.string.device_name), res.getString(R.string.device_name_value));
    }

    private InfoObject getDeviceModel(Resources res) {
        // return new InfoObject(res.getString(R.string.device_model), Build.MODEL);
        return new InfoObject(res.getString(R.string.device_model), "LE-TP001");
    }

    private InfoObject getSerialNumber(Resources res) {
        return new InfoObject(res.getString(R.string.serial_number), TStorageManager.getInstance().getDeviceId());
    }

    private InfoObject getMacAddress(Resources res) {
        String macAddress = SystemProxy.getInstance().getWifiMacAddress();
        return new InfoObject(res.getString(R.string.mac_address), macAddress);
    }

    private InfoObject getDeviceVersion(Resources res) {
        return new InfoObject(res.getString(R.string.device_version),
                SystemProxy.getInstance().getStringProperties(
                        "ro.mediatek.version.release", ""));
    }

    class DeviceInfoLoader extends AsyncTask<Void, Void, ArrayList<InfoObject>> {

        @Override
        protected ArrayList<InfoObject> doInBackground(Void... objects) {
            return getInfoObjects();
        }

        @Override
        protected void onPostExecute(ArrayList<InfoObject> result) {
            mInfoAdapter.addInfoObjects(result);
        }
    }

    @Override
    public void onItemClick(int position) {
        switch (position) {
            case 1: {
                if (mStartClick) {
                    if (mDevHitCountdown > 0) {
                        mDevHitCountdown--;
                        if (mDevHitCountdown == 0) {
                            mStartClick = false;
                            mDevHitCountdown = TAPS_TO_SECRET_CODE;
                            showSecretDialog();
                        } else if (mDevHitCountdown > 0
                                && mDevHitCountdown <= TAPS_TO_SECRET_CODE - 2) {
                            showToast(mDevHitCountdown);
                        }
                    }
                } else {
                    if (isTripleClick(SystemClock.uptimeMillis())) {
                        mStartClick = true;
                        showToast(mDevHitCountdown);
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private void showSecretDialog() {
        if (mSecretDialog != null && mSecretDialog.isVisible()) {
            mSecretDialog.dismiss();
            mSecretDialog = null;
        }
        if (mFragmentManager != null) {
            mSecretDialog = new SecretCodeDialog();
            mSecretDialog.setCancelable(false);
            mSecretDialog.show(mFragmentManager, "secret_code_dialog");
        }
    }

    private boolean isTripleClick(long time) {
        if ((time - mLastClickTime) < 600) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
            mHits[mHits.length - 1] = time;
            mLastClickTime = time;
            if ((mHits[mHits.length - 1] - mHits[0]) < 1200) {
                return true;
            }
        } else {
            Arrays.fill(mHits, 0);
            mHits[mHits.length - 1] = SystemClock.uptimeMillis();
            mLastClickTime = time;
        }
        return false;
    }

    private void showToast(int mDevHitCountdown) {
        if (mDevHitToast != null) {
            mDevHitToast.cancel();
        }
        mDevHitToast = Toast.makeText(mContext, mContext.getString(
                R.string.show_dev_countdown, mDevHitCountdown),
                Toast.LENGTH_SHORT);
        mDevHitToast.show();
    }

    public <T> boolean contains(T[] array, T value) {
        return indexOf(array, value) != -1;
    }

    public <T> int indexOf(T[] array, T value) {
        if (array == null) return -1;
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], value)) return i;
        }
        return -1;
    }
}
