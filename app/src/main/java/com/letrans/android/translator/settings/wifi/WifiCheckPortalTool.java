package com.letrans.android.translator.settings.wifi;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WifiCheckPortalTool {
    private static final String TAG = "WifiCheckPortalTool";
    private static final int SUCCESS_CODE = 204;

    /**
     * 可以参考Android系统framework的实现，类NetworkMonitor，方法isCaptivePortal，
     * 该方法主要用来得到CaptivePortalProbeResult实例，赋值给变量probeResult。
     *
     * isCaptivePortal方法的主要逻辑，这里着重介绍下。
     * 实际打印log，走的逻辑是第3个：
     * if (pacUrl != null) {
     * result = sendHttpProbe(pacUrl, ValidationProbeEvent.PROBE_PAC);
     * } else if (mUseHttps) {
     * result = sendParallelHttpProbes(httpsUrl, httpUrl);
     * } else {
     * result = sendHttpProbe(httpUrl, ValidationProbeEvent.PROBE_HTTP); //----------执行这个逻辑
     * }
     * httpUrl的来源：httpUrl = new URL(getCaptivePortalServerUrl(mContext, false));
     * 方法getCaptivePortalServerUrl：
     * private static String getCaptivePortalServerUrl(Context context, boolean isHttps) {
     * String server = Settings.Global.getString(context.getContentResolver(),
     * Settings.Global.CAPTIVE_PORTAL_SERVER);
     * if (server == null) server = DEFAULT_SERVER_SECONDARY;
     * return (isHttps ? "https" : "http") + "://" + server + "/generate_204";
     * }
     * 从settings.db数据库查询的server地址为null，所以server的值为DEFAULT_SERVER_SECONDARY
     * DEFAULT_SERVER_SECONDARY：captive.apple.com
     * 所以最后得到了URL：http://captive.apple.com/generate_204
     *
     * 然后方法sendHttpProbe和下面这个方法的代码是差不多了，主要是上面的逻辑得到正确的URL很重要吧。
     * 最后还会得到一个转接URL，通过调用：redirectUrl = urlConnection.getHeaderField("location");
     * 值为:https://1.1.1.1/login.html?redirect=captive.apple.com/generate_204
     *
     * 上面的逻辑是用来判断是否需要弹出一个通知，供用户进行登录认证连接网络。
     * 系统的通知发出的代码在方法：ConnectivityService.setProvNotificationVisibleIntent
     * 代码大致流程：
     * NetworkMonitor.EvaluatingState.processMessage：else if (probeResult.isPortal()) ->transitionTo(mCaptivePortalState);
     *   --NetworkMonitor.CaptivePortalState.enter() :发送消息EVENT_PROVISIONING_NOTIFICATION
     *     --ConnectivityService$NetworkStateTrackerHandler.handleMessage(EVENT_PROVISIONING_NOTIFICATION)
     *       --ConnectivityService$NetworkStateTrackerHandler.maybeHandleNetworkMonitorMessage
     *         --ConnectivityService.-wrap28
     *           --ConnectivityService.setProvNotificationVisibleIntent
     * 这个执行流程可以看到，关键的判断就是：else if (probeResult.isPortal())，这个判断的值就是由上面介绍的逻辑决定的。
     *
     * @return
     */
    public static boolean isWifiSetPortal() {
        final String mWalledGardenUrl = "http://g.cn/generate_204";
        final int WALLED_GARDEN_SOCKET_TIMEOUT_MS = 10000;

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(mWalledGardenUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setConnectTimeout(WALLED_GARDEN_SOCKET_TIMEOUT_MS);
            urlConnection.setReadTimeout(WALLED_GARDEN_SOCKET_TIMEOUT_MS);
            urlConnection.setUseCaches(false);
            urlConnection.getInputStream();
            int httpResponseCode = urlConnection.getResponseCode();
            Log.d(TAG, "isWifiSetPortal(): httpResponseCode=" + httpResponseCode);
            if (httpResponseCode == 200) {
                Log.d(TAG, "isWifiSetPortal(): Empty 200 response interpreted as 204 response.");
                if (urlConnection.getContentLength() == 0) {
                    httpResponseCode = SUCCESS_CODE;
                } else if (urlConnection.getContentLength() == -1) {
                    if (urlConnection.getInputStream().read() == -1) {
                        httpResponseCode = SUCCESS_CODE;
                    }
                }
            }
            String contentType = urlConnection.getContentType();
            if ("text/html".equals(contentType)) {
                InputStreamReader in = new InputStreamReader(
                        (InputStream) urlConnection.getContent());
                BufferedReader buff = new BufferedReader(in);
                String line = buff.readLine();
                Log.d(TAG, "isWifiSetPortal(): urlConnection.getContent() = " + line);
                if (httpResponseCode == 200 && line == null) {
                    httpResponseCode = SUCCESS_CODE;
                    Log.d(TAG, "isWifiSetPortal(): Internet detected!--1");
                } else if (httpResponseCode == 200 && line.contains("Success")) {
                    httpResponseCode = SUCCESS_CODE;
                    Log.d(TAG, "isWifiSetPortal(): Internet detected!--2");
                }
                in.close();
                buff.close();
            }

            return isPortal(httpResponseCode);
        } catch (IOException e) {
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private static boolean isPortal(int httpResponseCode) {
        return httpResponseCode != SUCCESS_CODE &&
                (httpResponseCode >= 200) && (httpResponseCode <= 399);
    }
}
