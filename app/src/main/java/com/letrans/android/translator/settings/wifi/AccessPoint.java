
package com.letrans.android.translator.settings.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.View;
import android.widget.ImageView;

import com.letrans.android.translator.R;
import com.letrans.android.translator.storage.TStorageManager;

class AccessPoint implements Comparable<AccessPoint> {
    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    private static final int[] STATE_NONE = {};

    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;

    static final int INVALID_NETWORK_ID = -1;

    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    Context context;
    String ssid;
    String bssid;
    int security;
    int networkId;
    String summary;
    boolean wpsAvailable = false;
    boolean connectedNoInternet;

    PskType pskType = PskType.UNKNOWN;

    private WifiConfiguration mConfig;
    private int mRssi;
    private WifiInfo mInfo;
    private DetailedState mState;

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public String getSecurityString() {
        switch (security) {
            case SECURITY_EAP:
                return context.getString(R.string.wifi_security_eap);
            case SECURITY_PSK:
                switch (pskType) {
                    case WPA:
                        return context.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        return context.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        return context.getString(R.string.wifi_security_wpa_wpa2);
                    case UNKNOWN:
                    default:
                        return context.getString(R.string.wifi_security_wpa_wpa2);
                }
            case SECURITY_WEP:
                return context.getString(R.string.wifi_security_wep);
            case SECURITY_NONE:
                return "no password";
            default:
                return "";
        }
    }

    private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            return PskType.UNKNOWN;
        }
    }

    AccessPoint(Context context, WifiConfiguration config) {
        this.context = context;
        loadConfig(config);
        updateSummary();
    }

    AccessPoint(Context context, ScanResult result) {
        this.context = context;
        loadResult(result);
        updateSummary();
    }

    void loadConfig(WifiConfiguration config) {
        ssid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
        bssid = config.BSSID;
        security = getSecurity(config);
        networkId = config.networkId;
        mConfig = config;
        mRssi = Integer.MAX_VALUE;
    }

    void loadResult(ScanResult result) {
        ssid = result.SSID;
        bssid = result.BSSID;
        security = getSecurity(result);
        networkId = INVALID_NETWORK_ID;
        mRssi = result.level;
        wpsAvailable = security != SECURITY_EAP && result.capabilities.contains("WPS");
        if (security == SECURITY_PSK) {
            pskType = getPskType(result);
        }
    }

    @Override
    public int compareTo(AccessPoint other) {
        // Active one goes first.
        if (mInfo != null && other.mInfo == null) return -1;
        if (mInfo == null && other.mInfo != null) return 1;

        // Reachable one goes before unreachable one.
        if (mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) return -1;
        if (mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) return 1;

        // Configured one goes before unconfigured one.
        if (networkId != INVALID_NETWORK_ID
                && other.networkId == INVALID_NETWORK_ID) return -1;
        if (networkId == INVALID_NETWORK_ID
                && other.networkId != INVALID_NETWORK_ID) return 1;

        // Sort by signal strength.
        int difference = WifiManager.compareSignalLevel(other.mRssi, mRssi);
        if (difference != 0) {
            return difference;
        }
        // Sort by ssid.
        return ssid.compareToIgnoreCase(other.ssid);
    }

    boolean update(ScanResult result) {
        // We do not call refresh() since this is called before onBindView().
        if (ssid.equals(result.SSID) && security == getSecurity(result)) {
            mRssi = result.level;
            // This flag only comes from scans, is not easily saved in config
            if (security == SECURITY_PSK) {
                pskType = getPskType(result);
            }
            updateSummary();
            return true;
        }
        return false;
    }

    boolean update(WifiConfiguration config, WifiInfo info, DetailedState state) {
        boolean reorder = false;
        if (info != null && isInfoForThisAccessPoint(config, info)) {
            reorder = true;
            if (config != null && mConfig != config) {
                loadConfig(config);
            }
            mRssi = info.getRssi();
            mInfo = info;
            mState = state;
            updateSummary();
        } else if (mInfo != null) {
            reorder = true;
            mInfo = null;
            mState = null;
            updateSummary();
        }
        return reorder;
    }

    private boolean isInfoForThisAccessPoint(WifiConfiguration config, WifiInfo info) {
        if (isPasspoint() == false && networkId != -1) {
            return networkId == info.getNetworkId();
        } else if (config != null) {
            return matches(config);
        } else {
            // Might be an ephemeral connection with no WifiConfiguration. Try matching on SSID.
            // (Note that we only do this if the WifiConfiguration explicitly equals INVALID).
            // TODO: Handle hex string SSIDs.
            return ssid.equals(removeDoubleQuotes(info.getSSID()));
        }
    }

    public boolean matches(WifiConfiguration config) {
        if (config.isPasspoint() && mConfig != null && mConfig.isPasspoint())
            return config.FQDN.equals(mConfig.providerFriendlyName);
        else
            return ssid.equals(removeDoubleQuotes(config.SSID)) && security == getSecurity(config);
    }

    public boolean isPasspoint() {
        return mConfig != null && mConfig.isPasspoint();
    }

    void setImageSignal(ImageView signal) {
        if (null == signal) {
            return;
        }
        if (mRssi == Integer.MAX_VALUE) {
            signal.setImageDrawable(null);
        } else {
            signal.setImageResource(R.drawable.wifi_signal_open);
			/*signal.setImageState((security != SECURITY_NONE) ? STATE_SECURED
					: STATE_NONE, true);*/
            int level = getLevel();
            signal.setImageLevel(level == 0 ? 1 : level);
        }
    }

    void updateSecurityView(ImageView imageView) {
        if (security != SECURITY_NONE) {
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    int getLevel() {
        if (mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(mRssi, 4);
    }

    int getRssi() {
        return mRssi;
    }


    WifiConfiguration getConfig() {
        return mConfig;
    }

    WifiInfo getInfo() {
        return mInfo;
    }

    DetailedState getState() {
        return mState;
    }

    static String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    private void updateSummary() {
        if (mState != null) {
            summary = WifiSummary.get(context, mState);
            if (mState == DetailedState.CONNECTED) {
                ConnectivityManager cm = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (nc != null && !nc.hasCapability(nc.NET_CAPABILITY_VALIDATED)
                        || TStorageManager.getInstance().isPortal()) {
                    connectedNoInternet = true;
                    summary = context.getString(R.string.wifi_connected_no_internet);
                } else {
                    connectedNoInternet = false;
                }
            }
        } else if (mRssi == Integer.MAX_VALUE) {
            summary = context.getString(R.string.wifi_not_in_range);
        } else {
            StringBuilder status = new StringBuilder();
            if (mConfig != null) {
                status.append(context.getString((mConfig.status == WifiConfiguration.Status.DISABLED) ?
                        R.string.wifi_disabled : R.string.wifi_remembered));
            }

            if (security != SECURITY_NONE) {
                String securityStrFormat;
                if (status.length() == 0) {
                    securityStrFormat = context.getString(R.string.wifi_secured_first_item);
                } else {
                    securityStrFormat = context.getString(R.string.wifi_secured_second_item);
                }
                status.append(String.format(securityStrFormat, getSecurityString()));
            }

//            if (mConfig == null && wpsAvailable) { // Only list WPS available for unsaved networks
//                if (status.length() == 0) {
//                	status.append(context.getString(R.string.wifi_wps_available_first_item));
//                } else {
//                	status.append(context.getString(R.string.wifi_wps_available_second_item));
//                }
//            }
            summary = status.toString();
        }
    }

    @Override
    public String toString() {
        return getClass().getName()
                + "{ssid=" + ssid
                + ",bssid=" + bssid
                + ",security=" + security
                + ",networkId=" + networkId
                + ",summary=" + summary
                + ",wpsAvailable=" + wpsAvailable
                + ",pskType=" + pskType
                + ",mRssi=" + mRssi
                + "}";
    }
}
